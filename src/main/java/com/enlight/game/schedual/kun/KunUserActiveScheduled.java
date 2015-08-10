package com.enlight.game.schedual.kun;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.cardinality.Cardinality;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.enlight.game.util.EsUtil;

/**
 * 
 * @author apple
 * 活跃玩家
 */
@Transactional(readOnly = true)
public class KunUserActiveScheduled {
	
	@Autowired
	public Client client;
	
	//项目名称
	private static final String game = "KUN";
	
	private static final String index = "logstash-kun-user-*";
	
	private static final String type = "kun_user.log";
	
	private static final String bulk_index = "log_kun_user";
	
	private static final String bulk_type_active_day = "kun_user_active_day";
	
	private static final String bulk_type_active_week = "kun_user_active_week";
	
	private static final String bulk_type_active_mouth = "kun_user_active_mouth";
	
	private static final Integer szsize = 10; //运营大区
	
	private static final Integer pfsize = 300; //渠道
	
	private static final Integer srsize = 300; //服务器
	
	EsUtil esUtilTest = new EsUtil();
	
	//all
	public void esAll() throws IOException, ParseException {	
		BulkRequestBuilder bulkRequest = client.prepareBulk();
		System.out.println("###############  all 活跃玩家");
		SearchResponse dayactive = client.prepareSearch(index).setSearchType("count").setTypes(type).setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(),
				FilterBuilders.andFilter(
				        FilterBuilders.rangeFilter("@timestamp").from(esUtilTest.oneDayAgoFrom()).to(esUtilTest.nowDate()),
				        FilterBuilders.termFilter("日志分类关键字", "login")
						))
		        ).addAggregation(
			    		AggregationBuilders.cardinality("agg").field("玩家GUID")
			    ).execute().actionGet();
		
		Cardinality aggDay = dayactive.getAggregations().get("agg");
		long valueDay = aggDay.getValue();
		
		bulkRequest.add(client.prepareIndex(bulk_index, bulk_type_active_day)
		        .setSource(jsonBuilder()
			           	 .startObject()
	                        .field("date", esUtilTest.oneDayAgoFrom().split("T")[0])
	                        .field("gameId", game)
	                        .field("key", "all")
	                        .field("@timestamp", new Date())
	                        .field("ct","day")
	                        .field("cv", valueDay)
	                    .endObject()
		                  )
		        );
		
		SearchResponse weekactive = client.prepareSearch(index).setSearchType("count").setTypes(type).setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(),
				FilterBuilders.andFilter(
						FilterBuilders.rangeFilter("@timestamp").from(esUtilTest.weekFrom()).to(esUtilTest.nowDate()),
				        FilterBuilders.termFilter("日志分类关键字", "login")
						))
		        ).addAggregation(
			    		AggregationBuilders.cardinality("agg").field("玩家GUID")
			    ).execute().actionGet();
		
		Cardinality aggWeek = weekactive.getAggregations().get("agg");
		long valueWeek = aggWeek.getValue();
		
		bulkRequest.add(client.prepareIndex(bulk_index, bulk_type_active_week)
		        .setSource(jsonBuilder()
			           	 .startObject()
	                        .field("date", esUtilTest.oneDayAgoFrom().split("T")[0])
	                        .field("gameId", game)
	                        .field("key", "all")
	                        .field("@timestamp", new Date())
	                        .field("ct","week")
	                        .field("cv", valueWeek)
	                    .endObject()
		                  )
		        );
		
		SearchResponse mouthactive = client.prepareSearch(index).setSearchType("count").setTypes(type).setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(),
				FilterBuilders.andFilter(
						FilterBuilders.rangeFilter("@timestamp").from(esUtilTest.mouthFrom()).to(esUtilTest.nowDate()),
				        FilterBuilders.termFilter("日志分类关键字", "login")
						))
		        ).addAggregation(
			    		AggregationBuilders.cardinality("agg").field("玩家GUID")
			    ).execute().actionGet();
		
		Cardinality aggMouth = mouthactive.getAggregations().get("agg");
		long valueMouth = aggMouth.getValue();
		
		bulkRequest.add(client.prepareIndex(bulk_index, bulk_type_active_mouth)
		        .setSource(jsonBuilder()
			           	 .startObject()
	                        .field("date", esUtilTest.oneDayAgoFrom().split("T")[0])
	                        .field("gameId", game)
	                        .field("key", "all")
	                        .field("@timestamp", new Date())
	                        .field("ct","mouth")
	                        .field("cv", valueMouth)
	                    .endObject()
		                  )
		        );
		if(bulkRequest.numberOfActions()!=0){
			bulkRequest.execute().actionGet();	
		}	
	}	
	
	//serverzone
	public void esServerZone() throws IOException, ParseException {
		BulkRequestBuilder bulkRequest = client.prepareBulk();
		System.out.println("###############  serverzone 活跃玩家");
		SearchResponse dayactive = client.prepareSearch(index).setSearchType("count").setTypes(type).setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(),
				FilterBuilders.andFilter(
				        FilterBuilders.rangeFilter("@timestamp").from(esUtilTest.oneDayAgoFrom()).to(esUtilTest.nowDate()),
				        FilterBuilders.termFilter("日志分类关键字", "login")
						))
		        ).addAggregation(
						AggregationBuilders.terms("serverZone").field("运营大区ID").size(szsize).subAggregation(
								AggregationBuilders.cardinality("agg").field("玩家GUID")
								)
			    ).execute().actionGet();
		Terms daygenders = dayactive.getAggregations().get("serverZone");	
		for (Terms.Bucket e : daygenders.getBuckets()) {
		    Cardinality agg = e.getAggregations().get("agg");
		    Long aggcount = agg.getValue();
		    
			bulkRequest.add(client.prepareIndex(bulk_index, bulk_type_active_day)
			        .setSource(jsonBuilder()
				           	 .startObject()
		                        .field("date", esUtilTest.oneDayAgoFrom().split("T")[0])
		                        .field("gameId", game)
		                        .field("key", "serverZone")
		                        .field("value", e.getKey())
		                        .field("@timestamp", new Date())
		                        .field("ct","day")
		                        .field("cv", aggcount.toString())
		                    .endObject()
			                  )
			        );
		}   
		
		SearchResponse weekactive = client.prepareSearch(index).setSearchType("count").setTypes(type).setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(),
				FilterBuilders.andFilter(
						FilterBuilders.rangeFilter("@timestamp").from(esUtilTest.weekFrom()).to(esUtilTest.nowDate()),
				        FilterBuilders.termFilter("日志分类关键字", "login")
						))
		        ).addAggregation(
						AggregationBuilders.terms("serverZone").field("运营大区ID").size(szsize).subAggregation(
								AggregationBuilders.cardinality("agg").field("玩家GUID")
								)
			    ).execute().actionGet();
		Terms weekgenders = weekactive.getAggregations().get("serverZone");	
		for (Terms.Bucket e : weekgenders.getBuckets()) {
		    Cardinality agg = e.getAggregations().get("agg");
		    Long aggcount = agg.getValue();
		    
			bulkRequest.add(client.prepareIndex(bulk_index, bulk_type_active_week)
			        .setSource(jsonBuilder()
				           	 .startObject()
		                        .field("date", esUtilTest.oneDayAgoFrom().split("T")[0])
		                        .field("gameId", game)
		                        .field("key", "serverZone")
		                        .field("value", e.getKey())
		                        .field("@timestamp", new Date())
		                        .field("ct","week")
		                        .field("cv", aggcount.toString())
		                    .endObject()
			                  )
			        );
		}  
		
		
		SearchResponse mouthactive = client.prepareSearch(index).setSearchType("count").setTypes(type).setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(),
				FilterBuilders.andFilter(
						FilterBuilders.rangeFilter("@timestamp").from(esUtilTest.mouthFrom()).to(esUtilTest.nowDate()),
				        FilterBuilders.termFilter("日志分类关键字", "login")
						))
		        ).addAggregation(
						AggregationBuilders.terms("serverZone").field("运营大区ID").size(szsize).subAggregation(
								AggregationBuilders.cardinality("agg").field("玩家GUID")
								)
			    ).execute().actionGet();
		Terms mouthgenders = mouthactive.getAggregations().get("serverZone");	
		for (Terms.Bucket e : mouthgenders.getBuckets()) {
		    Cardinality agg = e.getAggregations().get("agg");
		    Long aggcount = agg.getValue();
		    
			bulkRequest.add(client.prepareIndex(bulk_index, bulk_type_active_mouth)
			        .setSource(jsonBuilder()
				           	 .startObject()
		                        .field("date", esUtilTest.oneDayAgoFrom().split("T")[0])
		                        .field("gameId", game)
		                        .field("key", "serverZone")
		                        .field("value", e.getKey())
		                        .field("@timestamp", new Date())
		                        .field("ct","mouth")
		                        .field("cv", aggcount.toString())
		                    .endObject()
			                  )
			        );
		}  
		if(bulkRequest.numberOfActions()!=0){
			bulkRequest.execute().actionGet();	
		}	
	}
	
	//platform
	public void esPlatForm() throws IOException, ParseException {
		BulkRequestBuilder bulkRequest = client.prepareBulk();
		System.out.println("###############  platform 活跃玩家");
		SearchResponse dayactive = client.prepareSearch(index).setSearchType("count").setTypes(type).setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(),
				FilterBuilders.andFilter(
				        FilterBuilders.rangeFilter("@timestamp").from(esUtilTest.oneDayAgoFrom()).to(esUtilTest.nowDate()),
				        FilterBuilders.termFilter("日志分类关键字", "login")
						))
		        ).addAggregation(
						AggregationBuilders.terms("platform").field("渠道ID").size(pfsize).subAggregation(
								AggregationBuilders.cardinality("agg").field("玩家GUID")
								)
			    ).execute().actionGet();
		Terms daygenders = dayactive.getAggregations().get("platform");	
		for (Terms.Bucket e : daygenders.getBuckets()) {
		    Cardinality agg = e.getAggregations().get("agg");
		    Long aggcount = agg.getValue();
		    
			bulkRequest.add(client.prepareIndex(bulk_index, bulk_type_active_day)
			        .setSource(jsonBuilder()
				           	 .startObject()
		                        .field("date", esUtilTest.oneDayAgoFrom().split("T")[0])
		                        .field("gameId", game)
		                        .field("key", "platForm")
		                        .field("value", e.getKey())
		                        .field("@timestamp", new Date())
		                        .field("ct","day")
		                        .field("cv", aggcount.toString())
		                    .endObject()
			                  )
			        );
		}   
		
		SearchResponse weekactive = client.prepareSearch(index).setSearchType("count").setTypes(type).setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(),
				FilterBuilders.andFilter(
						FilterBuilders.rangeFilter("@timestamp").from(esUtilTest.weekFrom()).to(esUtilTest.nowDate()),
				        FilterBuilders.termFilter("日志分类关键字", "login")
						))
		        ).addAggregation(
		        		AggregationBuilders.terms("platform").field("渠道ID").size(pfsize).subAggregation(
								AggregationBuilders.cardinality("agg").field("玩家GUID")
								)
			    ).execute().actionGet();
		Terms weekgenders = weekactive.getAggregations().get("platform");	
		for (Terms.Bucket e : weekgenders.getBuckets()) {
		    Cardinality agg = e.getAggregations().get("agg");
		    Long aggcount = agg.getValue();
		    
			bulkRequest.add(client.prepareIndex(bulk_index, bulk_type_active_week)
			        .setSource(jsonBuilder()
				           	 .startObject()
		                        .field("date", esUtilTest.oneDayAgoFrom().split("T")[0])
		                        .field("gameId", game)
		                        .field("key", "platForm")
		                        .field("value", e.getKey())
		                        .field("@timestamp", new Date())
		                        .field("ct","week")
		                        .field("cv", aggcount.toString())
		                    .endObject()
			                  )
			        );
		}  
		
		
		SearchResponse mouthactive = client.prepareSearch(index).setSearchType("count").setTypes(type).setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(),
				FilterBuilders.andFilter(
						FilterBuilders.rangeFilter("@timestamp").from(esUtilTest.mouthFrom()).to(esUtilTest.nowDate()),
				        FilterBuilders.termFilter("日志分类关键字", "login")
						))
		        ).addAggregation(
		        		AggregationBuilders.terms("platform").field("渠道ID").size(pfsize).subAggregation(
								AggregationBuilders.cardinality("agg").field("玩家GUID")
								)
			    ).execute().actionGet();
		Terms mouthgenders = mouthactive.getAggregations().get("platform");	
		for (Terms.Bucket e : mouthgenders.getBuckets()) {
		    Cardinality agg = e.getAggregations().get("agg");
		    Long aggcount = agg.getValue();
		    
			bulkRequest.add(client.prepareIndex(bulk_index, bulk_type_active_mouth)
			        .setSource(jsonBuilder()
				           	 .startObject()
		                        .field("date", esUtilTest.oneDayAgoFrom().split("T")[0])
		                        .field("gameId", game)
		                        .field("key", "platForm")
		                        .field("value", e.getKey())
		                        .field("@timestamp", new Date())
		                        .field("ct","mouth")
		                        .field("cv", aggcount.toString())
		                    .endObject()
			                  )
			        );
		}  
		if(bulkRequest.numberOfActions()!=0){
			bulkRequest.execute().actionGet();	
		}	
	}	
	
	//server
	public void esServer() throws IOException, ParseException {
		BulkRequestBuilder bulkRequest = client.prepareBulk();
		System.out.println("###############  server 活跃玩家");
		SearchResponse dayactive = client.prepareSearch(index).setSearchType("count").setTypes(type).setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(),
				FilterBuilders.andFilter(
				        FilterBuilders.rangeFilter("@timestamp").from(esUtilTest.oneDayAgoFrom()).to(esUtilTest.nowDate()),
				        FilterBuilders.termFilter("日志分类关键字", "login")
						))
		        ).addAggregation(
						AggregationBuilders.terms("server").field("服务器ID").size(srsize).subAggregation(
								AggregationBuilders.cardinality("agg").field("玩家GUID")
								)
			    ).execute().actionGet();
		Terms daygenders = dayactive.getAggregations().get("server");	
		for (Terms.Bucket e : daygenders.getBuckets()) {
		    Cardinality agg = e.getAggregations().get("agg");
		    Long aggcount = agg.getValue();
		    
			bulkRequest.add(client.prepareIndex(bulk_index, bulk_type_active_day)
			        .setSource(jsonBuilder()
				           	 .startObject()
		                        .field("date", esUtilTest.oneDayAgoFrom().split("T")[0])
		                        .field("gameId", game)
		                        .field("key", "server")
		                        .field("value", e.getKey())
		                        .field("@timestamp", new Date())
		                        .field("ct","day")
		                        .field("cv", aggcount.toString())
		                    .endObject()
			                  )
			        );
		}   
		
		SearchResponse weekactive = client.prepareSearch(index).setSearchType("count").setTypes(type).setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(),
				FilterBuilders.andFilter(
						FilterBuilders.rangeFilter("@timestamp").from(esUtilTest.weekFrom()).to(esUtilTest.nowDate()),
				        FilterBuilders.termFilter("日志分类关键字", "login")
						))
		        ).addAggregation(
		        		AggregationBuilders.terms("server").field("服务器ID").size(srsize).subAggregation(
								AggregationBuilders.cardinality("agg").field("玩家GUID")
								)
			    ).execute().actionGet();
		Terms weekgenders = weekactive.getAggregations().get("server");	
		for (Terms.Bucket e : weekgenders.getBuckets()) {
		    Cardinality agg = e.getAggregations().get("agg");
		    Long aggcount = agg.getValue();
		    
			bulkRequest.add(client.prepareIndex(bulk_index, bulk_type_active_week)
			        .setSource(jsonBuilder()
				           	 .startObject()
		                        .field("date", esUtilTest.oneDayAgoFrom().split("T")[0])
		                        .field("gameId", game)
		                        .field("key", "server")
		                        .field("value", e.getKey())
		                        .field("@timestamp", new Date())
		                        .field("ct","week")
		                        .field("cv", aggcount.toString())
		                    .endObject()
			                  )
			        );
		}  
		
		
		SearchResponse mouthactive = client.prepareSearch(index).setSearchType("count").setTypes(type).setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(),
				FilterBuilders.andFilter(
						FilterBuilders.rangeFilter("@timestamp").from(esUtilTest.mouthFrom()).to(esUtilTest.nowDate()),
				        FilterBuilders.termFilter("日志分类关键字", "login")
						))
		        ).addAggregation(
		        		AggregationBuilders.terms("server").field("服务器ID").size(srsize).subAggregation(
								AggregationBuilders.cardinality("agg").field("玩家GUID")
								)
			    ).execute().actionGet();
		Terms mouthgenders = mouthactive.getAggregations().get("server");	
		for (Terms.Bucket e : mouthgenders.getBuckets()) {
		    Cardinality agg = e.getAggregations().get("agg");
		    Long aggcount = agg.getValue();
		    
			bulkRequest.add(client.prepareIndex(bulk_index, bulk_type_active_mouth)
			        .setSource(jsonBuilder()
				           	 .startObject()
		                        .field("date", esUtilTest.oneDayAgoFrom().split("T")[0])
		                        .field("gameId", game)
		                        .field("key", "server")
		                        .field("value", e.getKey())
		                        .field("@timestamp", new Date())
		                        .field("ct","mouth")
		                        .field("cv", aggcount.toString())
		                    .endObject()
			                  )
			        );
		}  
		if(bulkRequest.numberOfActions()!=0){
			bulkRequest.execute().actionGet();	
		}	
	}	

	
	@Transactional(readOnly=false, propagation=Propagation.REQUIRED)
	public void schedual() throws Exception{
		System.out.println("----------------活跃用户 active 调度开始");
		esAll();
		esServerZone();
		esPlatForm();
		esServer();
		System.out.println("----------------活跃用户 active 调度结束");
	}
}
