package com.enlight.game.schedual.kds;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.FilteredQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.cardinality.Cardinality;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.enlight.game.entity.analysis.UserRetained;
import com.enlight.game.util.EsUtil;

@Transactional(readOnly = true)
public class KdsRetainedScheduled {
	
	@Autowired
	public Client client;
	
	//项目名称
	private static final String game = "KDS";
	
	private static final String index = "logstash-kds-user-*";
	
	private static final String type = "kds_user.log";
	
	private static final String bulk_index = "log_kds_user";
	
	private static final String bulk_type_retained = "kds_user_retained";
	
	private static final Integer szsize = 10; //运营大区
	
	private static final Integer pfsize = 300; //渠道
	
	private static final Integer srsize = 300; //服务器
	
	private static final Integer num = 35; //35天以内的留存
	
	EsUtil esUtilTest = new EsUtil();
	
	public Long createCount(String from , String to){
		FilteredQueryBuilder builder2 = QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(),
		        FilterBuilders.andFilter(
				        FilterBuilders.rangeFilter("@timestamp").from(from).to(to),
		        		FilterBuilders.termFilter("日志分类关键字", "create"))
		        );
		SearchResponse sr = client.prepareSearch(index).setTypes(type).setSearchType("count").setQuery(builder2).execute().actionGet();
		Long count = sr.getHits().getTotalHits();
		return count;
	}
	
	public Long createServerZoneCount(String key,String from , String to){
		FilteredQueryBuilder builder2 = QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(),
		        FilterBuilders.andFilter(
				        FilterBuilders.rangeFilter("@timestamp").from(from).to(to),
		        		FilterBuilders.termFilter("日志分类关键字", "create"),
		        		FilterBuilders.termFilter("运营大区ID", key))
		        );
		SearchResponse sr = client.prepareSearch(index).setTypes(type).setSearchType("count").setQuery(builder2).execute().actionGet();
		Long count = sr.getHits().getTotalHits();
		return count;
	}
	
	public Long createPlatFormCount(String key,String from , String to){
		FilteredQueryBuilder builder2 = QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(),
		        FilterBuilders.andFilter(
				        FilterBuilders.rangeFilter("@timestamp").from(from).to(to),
		        		FilterBuilders.termFilter("日志分类关键字", "create"),
		        		FilterBuilders.termFilter("渠道ID", key))
		        );
		SearchResponse sr = client.prepareSearch(index).setTypes(type).setSearchType("count").setQuery(builder2).execute().actionGet();
		Long count = sr.getHits().getTotalHits();
		return count;
	}
	
	public Long createServerCount(String key,String from , String to){
		FilteredQueryBuilder builder2 = QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(),
		        FilterBuilders.andFilter(
				        FilterBuilders.rangeFilter("@timestamp").from(from).to(to),
		        		FilterBuilders.termFilter("日志分类关键字", "create"),
		        		FilterBuilders.termFilter("服务器ID", key))
		        );
		SearchResponse sr = client.prepareSearch(index).setTypes(type).setSearchType("count").setQuery(builder2).execute().actionGet();
		Long count = sr.getHits().getTotalHits();
		return count;
	}


	public void esAll() throws IOException {	
		BulkRequestBuilder bulkRequest = client.prepareBulk();
		SimpleDateFormat sdf =   new SimpleDateFormat("yyyy-MM-dd'T'00:00:00.000'Z'" ); 
		DecimalFormat df = new DecimalFormat("0.00");//格式化小数  
		
		FilteredQueryBuilder builder = QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(),
		        FilterBuilders.andFilter(
				        FilterBuilders.rangeFilter("@timestamp").from(esUtilTest.oneDayAgoFrom()).to(esUtilTest.nowDate()),
		        		FilterBuilders.termFilter("日志分类关键字", "login"))
		        );
		SearchResponse sr = client.prepareSearch(index).setSearchType("count").setTypes(type).setQuery(builder)
			    .addAggregation(
			    		AggregationBuilders.terms("create").field("注册时间").order(Terms.Order.term(false))
			    		.subAggregation(AggregationBuilders.cardinality("agg").field("玩家GUID")).size(num)
			    ).execute().actionGet();
		System.out.println("----------------esAll---------------");
		
	    UserRetained userRetained = new UserRetained();
		Terms genders = sr.getAggregations().get("create");	
		for (Terms.Bucket entry : genders.getBuckets()) {
			String dateBucket = sdf.format(new Date((Long) entry.getKeyAsNumber()));
			if(dateBucket.equals(esUtilTest.twoDayAgoFrom())){
			    Cardinality agg = entry.getAggregations().get("agg");
			    Long aggcount = agg.getValue();
			    Double RetentionTwo ;
			    
			    Long r = createCount(esUtilTest.twoDayAgoFrom(), esUtilTest.twoDayAgoTo());
			    if(r==0){
				    RetentionTwo = (double) 0.00;
			    }else{
				    RetentionTwo = (double)aggcount*100/createCount(esUtilTest.twoDayAgoFrom(), esUtilTest.twoDayAgoTo());
			    }


			    userRetained.setDate(esUtilTest.twoDayAgoFrom());
			    userRetained.setGameId(game);
			    userRetained.setKey(UserRetained.KEY_ALL);
			    userRetained.setCtRetained(UserRetained.CT_NEXTDAY);
			    userRetained.setRetained(df.format(RetentionTwo));
			    userRetained.setTs(entry.getKey());

			}else if(dateBucket.equals(esUtilTest.eightDayAgoFrom())){
			    Cardinality agg = entry.getAggregations().get("agg");
			    Long aggcount = agg.getValue();
			    Double RetentionEight ;

				RetentionEight = (double)aggcount*100/createCount(esUtilTest.eightDayAgoFrom(), esUtilTest.eightDayAgoTo());
			    userRetained.setDate(esUtilTest.eightDayAgoFrom());
			    userRetained.setGameId(game);
			    userRetained.setKey(UserRetained.KEY_ALL);
			    userRetained.setCtRetained(UserRetained.CT_SEVENDAY);
			    userRetained.setRetained(df.format(RetentionEight));
			    userRetained.setTs(entry.getKey());
			    	
			}else if(dateBucket.equals(esUtilTest.thirtyOneDayAgoFrom())){
			    Cardinality agg = entry.getAggregations().get("agg");
			    Long aggcount = agg.getValue();
			    Double RetentionThirty ;

			    RetentionThirty = (double)aggcount*100/createCount(esUtilTest.thirtyOneDayAgoFrom(), esUtilTest.thirtyOneDayAgoTo());
			    userRetained.setDate(esUtilTest.thirtyOneDayAgoFrom());
			    userRetained.setGameId(game);
			    userRetained.setKey(UserRetained.KEY_ALL);
			    userRetained.setCtRetained(UserRetained.CT_THIRYTDAY);
			    userRetained.setRetained(df.format(RetentionThirty));
			    userRetained.setTs(entry.getKey());


			}
		}

		if(bulkRequest.numberOfActions()!=0){
			bulkRequest.add(client.prepareIndex(bulk_index, bulk_type_retained)
			        .setSource(jsonBuilder()
				           	 .startObject()
		                        .field("date", userRetained.getDate().split("T")[0])
		                        .field("gameId", userRetained.getGameId())
		                        .field("ctRetained", userRetained.getCtRetained())
		                        .field("retained", userRetained.getRetained())
		                        .field("key", userRetained.getKey())
		                        .field("@timestamp", new Date())
		                    .endObject()
			                  )
			        );
			bulkRequest.execute().actionGet();	
		}else{
			bulkRequest.add(client.prepareIndex(bulk_index, bulk_type_retained)
			        .setSource(
			                  )
			        );
			bulkRequest.execute().actionGet();	
		}
	}	
	
	public void esServerZone() throws IOException {	
		BulkRequestBuilder bulkRequest = client.prepareBulk();
		//计算时间（当前）2015-04-15 ，统计出2015-04-14到2015-04-15的数据 ，得出2015-04-13的次日留存、得出2015-04-07的7日留存、得出2015-03-15的30日留存
		SimpleDateFormat sdf =   new SimpleDateFormat("yyyy-MM-dd'T'00:00:00.000'Z'" ); 
		DecimalFormat df = new DecimalFormat("0.00");//格式化小数  
		
		FilteredQueryBuilder builder = QueryBuilders.filteredQuery(
		        QueryBuilders.matchAllQuery(),
		        FilterBuilders.andFilter(
				        FilterBuilders.rangeFilter("@timestamp").from(esUtilTest.oneDayAgoFrom()).to(esUtilTest.nowDate()),
		        		FilterBuilders.termFilter("日志分类关键字", "login"))
		        );
		SearchResponse sr = client.prepareSearch(index).setTypes(type).setSearchType("count").setQuery(builder)
			    .addAggregation(
			    		AggregationBuilders.terms("create").field("注册时间").order(Terms.Order.term(false)).size(num)
			    		.subAggregation(
			    				AggregationBuilders.cardinality("agg").field("玩家GUID").precisionThreshold(10000)
			    				)
			    		.subAggregation(
			    				AggregationBuilders.terms("serverZone").field("运营大区ID").size(szsize).subAggregation(
			    						AggregationBuilders.cardinality("agg").field("玩家GUID").precisionThreshold(10000)
			    						)
			    				)
			    ).execute().actionGet();
		System.out.println("-------------esServerZone-------------");

    	UserRetained userRetained = new UserRetained();
		Terms genders = sr.getAggregations().get("create");	
		for (Terms.Bucket entry : genders.getBuckets()) {
			String dateBucket = sdf.format(new Date((Long) entry.getKeyAsNumber()));
			if(dateBucket.equals(esUtilTest.twoDayAgoFrom())){
				Terms serverZone = entry.getAggregations().get("serverZone");
				
				for (Terms.Bucket e : serverZone.getBuckets()) {
				    Cardinality agg = e.getAggregations().get("agg");
				    Long aggcount = agg.getValue();
				    Double RetentionTwo ;

				    Long r = createServerZoneCount(e.getKey(),esUtilTest.twoDayAgoFrom(), esUtilTest.twoDayAgoTo());
				    if(r==0){
					    RetentionTwo = (double) 0.00;
				    }else{
				    	RetentionTwo = (double)aggcount*100/createServerZoneCount(e.getKey(),esUtilTest.twoDayAgoFrom(), esUtilTest.twoDayAgoTo());
				    }
				    

				    	userRetained.setDate(esUtilTest.twoDayAgoFrom());
				    	userRetained.setGameId(game);
				    	userRetained.setKey(UserRetained.KEY_SEVSERZONE);
				    	userRetained.setCtRetained(UserRetained.CT_NEXTDAY);
				    	userRetained.setRetained(df.format(RetentionTwo));
				    	userRetained.setValue(e.getKey());
				    	userRetained.setTs(entry.getKey());
				}
			}else if(dateBucket.equals(esUtilTest.eightDayAgoFrom())){
				Terms serverZone = entry.getAggregations().get("serverZone");
				for (Terms.Bucket e : serverZone.getBuckets()) {
				    Cardinality agg = e.getAggregations().get("agg");
				    Long aggcount = agg.getValue();
				    Double RetentionEight ;
					    RetentionEight = (double)aggcount*100/createServerZoneCount(e.getKey(),esUtilTest.eightDayAgoFrom(), esUtilTest.eightDayAgoTo());
				    	userRetained.setDate(esUtilTest.eightDayAgoFrom());
				    	userRetained.setGameId(game);
				    	userRetained.setKey(UserRetained.KEY_SEVSERZONE);
				    	userRetained.setCtRetained(UserRetained.CT_SEVENDAY);
				    	userRetained.setRetained(df.format(RetentionEight));
				    	userRetained.setValue(e.getKey());
				    	userRetained.setTs(entry.getKey());
				}
			}else if(dateBucket.equals(esUtilTest.thirtyOneDayAgoFrom())){
				Terms serverZone = entry.getAggregations().get("serverZone");
				for (Terms.Bucket e : serverZone.getBuckets()) {
				    Cardinality agg = e.getAggregations().get("agg");
				    Long aggcount = agg.getValue();
				    Double RetentionThirty ;
				    	RetentionThirty = (double)aggcount*100/createServerZoneCount(e.getKey(),esUtilTest.thirtyOneDayAgoFrom(), esUtilTest.thirtyOneDayAgoTo());
				    	userRetained.setDate(esUtilTest.thirtyOneDayAgoFrom());
				    	userRetained.setGameId(game);
				    	userRetained.setKey(UserRetained.KEY_SEVSERZONE);
				    	userRetained.setCtRetained(UserRetained.CT_THIRYTDAY);
				    	userRetained.setRetained(df.format(RetentionThirty));
				    	userRetained.setValue(e.getKey());
				    	userRetained.setTs(entry.getKey());

				}

			}
		}
		if(bulkRequest.numberOfActions()!=0){
			bulkRequest.add(client.prepareIndex(bulk_index, bulk_type_retained)
			        .setSource(jsonBuilder()
				           	 .startObject()
		                        .field("date", userRetained.getDate().split("T")[0])
		                        .field("gameId", userRetained.getGameId())
		                        .field("ctRetained", userRetained.getCtRetained())
		                        .field("retained", userRetained.getRetained())
		                        .field("key", userRetained.getKey())
		                        .field("ts","["+userRetained.getTs()+","+userRetained.getRetained()+"]")
		                        .field("value", userRetained.getValue())
		                        .field("@timestamp", new Date())
		                    .endObject()
			                  )
			        );
			bulkRequest.execute().actionGet();	
		}else{
			bulkRequest.add(client.prepareIndex(bulk_index, bulk_type_retained)
			        .setSource(
			                  )
			        );
			bulkRequest.execute().actionGet();	
		}
	}
	
	
	public void esPlatForm() throws IOException {	
		BulkRequestBuilder bulkRequest = client.prepareBulk();
		//计算时间（当前）2015-04-15 ，统计出2015-04-14到2015-04-15的数据 ，得出2015-04-13的次日留存、得出2015-04-07的7日留存、得出2015-03-15的30日留存
		SimpleDateFormat sdf =   new SimpleDateFormat("yyyy-MM-dd'T'00:00:00.000'Z'" ); 
		DecimalFormat df = new DecimalFormat("0.00");//格式化小数  
		
		FilteredQueryBuilder builder = QueryBuilders.filteredQuery(
		        QueryBuilders.matchAllQuery(),
		        FilterBuilders.andFilter(
				        FilterBuilders.rangeFilter("@timestamp").from(esUtilTest.oneDayAgoFrom()).to(esUtilTest.nowDate()),
		        		FilterBuilders.termFilter("日志分类关键字", "login"))
		        );
		SearchResponse sr = client.prepareSearch(index).setTypes(type).setSearchType("count").setQuery(builder)
			    .addAggregation(
			    		AggregationBuilders.terms("create").field("注册时间").order(Terms.Order.term(false)).size(num)
			    		.subAggregation(AggregationBuilders.cardinality("agg").field("玩家GUID"))
			    		.subAggregation(AggregationBuilders.terms("platForm").field("渠道ID").size(pfsize).subAggregation(AggregationBuilders.cardinality("agg").field("玩家GUID")))
			    ).execute().actionGet();
		System.out.println("------------esPlatForm--------------");

    	UserRetained userRetained = new UserRetained();
		Terms genders = sr.getAggregations().get("create");	
		for (Terms.Bucket entry : genders.getBuckets()) {
			String dateBucket = sdf.format(new Date((Long) entry.getKeyAsNumber()));
			if(dateBucket.equals(esUtilTest.twoDayAgoFrom())){
				Terms serverZone = entry.getAggregations().get("platForm");
				for (Terms.Bucket e : serverZone.getBuckets()) {
				    Cardinality agg = e.getAggregations().get("agg");
				    Long aggcount = agg.getValue();
				    Double RetentionTwo ;
				    
				    Long r = createPlatFormCount(e.getKey(),esUtilTest.twoDayAgoFrom(), esUtilTest.twoDayAgoTo());
				    if(r==0){
					    RetentionTwo = (double) 0.00;
				    }else{
				    	RetentionTwo = (double)aggcount*100/createPlatFormCount(e.getKey(),esUtilTest.twoDayAgoFrom(), esUtilTest.twoDayAgoTo());
				    }
				    

			    	userRetained.setDate(esUtilTest.twoDayAgoFrom());
			    	userRetained.setGameId(game);
			    	userRetained.setKey(UserRetained.KEY_PLATFORM);
			    	userRetained.setCtRetained(UserRetained.CT_NEXTDAY);
			    	userRetained.setRetained(df.format(RetentionTwo));
			    	userRetained.setValue(e.getKey());
			    	userRetained.setTs(entry.getKey());
				}
			}else if(dateBucket.equals(esUtilTest.eightDayAgoFrom())){
				Terms serverZone = entry.getAggregations().get("platForm");
				for (Terms.Bucket e : serverZone.getBuckets()) {
				    Cardinality agg = e.getAggregations().get("agg");
				    Long aggcount = agg.getValue();
				    Double RetentionEight ;
				    
				    RetentionEight = (double)aggcount*100/createPlatFormCount(e.getKey(),esUtilTest.eightDayAgoFrom(), esUtilTest.eightDayAgoTo());
			    	userRetained.setDate(esUtilTest.eightDayAgoFrom());
			    	userRetained.setGameId(game);
			    	userRetained.setKey(UserRetained.KEY_PLATFORM);
			    	userRetained.setCtRetained(UserRetained.CT_SEVENDAY);
			    	userRetained.setRetained(df.format(RetentionEight));
			    	userRetained.setValue(e.getKey());
			    	userRetained.setTs(entry.getKey());
				}
			}else if(dateBucket.equals(esUtilTest.thirtyOneDayAgoFrom())){
				Terms serverZone = entry.getAggregations().get("platForm");
				for (Terms.Bucket e : serverZone.getBuckets()) {
				    Cardinality agg = e.getAggregations().get("agg");
				    Long aggcount = agg.getValue();
				    Double RetentionThirty ;
				    
			    	RetentionThirty = (double)aggcount*100/createPlatFormCount(e.getKey(),esUtilTest.thirtyOneDayAgoFrom(), esUtilTest.thirtyOneDayAgoTo());
			    	userRetained.setDate(esUtilTest.thirtyOneDayAgoFrom());
			    	userRetained.setGameId(game);
			    	userRetained.setKey(UserRetained.KEY_PLATFORM);
			    	userRetained.setCtRetained(UserRetained.CT_THIRYTDAY);
			    	userRetained.setRetained(df.format(RetentionThirty));
			    	userRetained.setValue(e.getKey());
			    	userRetained.setTs(entry.getKey());
				}
			}
		}
		if(bulkRequest.numberOfActions()!=0){
			bulkRequest.add(client.prepareIndex(bulk_index, bulk_type_retained)
			        .setSource(jsonBuilder()
				           	 .startObject()
		                        .field("date", userRetained.getDate().split("T")[0])
		                        .field("gameId", userRetained.getGameId())
		                        .field("ctRetained", userRetained.getCtRetained())
		                        .field("retained", userRetained.getRetained())
		                        .field("key", userRetained.getKey())
		                        .field("value", userRetained.getValue())
		                        .field("@timestamp", new Date())
		                    .endObject()
			                  )
			        );
			bulkRequest.execute().actionGet();	
		}else{
			bulkRequest.add(client.prepareIndex(bulk_index, bulk_type_retained)
			        .setSource(
			                  )
			        );
			bulkRequest.execute().actionGet();	
		}
	}
	
	public void esServer() throws IOException {	
		BulkRequestBuilder bulkRequest = client.prepareBulk();
		//计算时间（当前）2015-04-15 ，统计出2015-04-14到2015-04-15的数据 ，得出2015-04-13的次日留存、得出2015-04-07的7日留存、得出2015-03-15的30日留存
		SimpleDateFormat sdf =   new SimpleDateFormat("yyyy-MM-dd'T'00:00:00.000'Z'" ); 
		DecimalFormat df = new DecimalFormat("0.00");//格式化小数  
		
		FilteredQueryBuilder builder = QueryBuilders.filteredQuery(
		        QueryBuilders.matchAllQuery(),
		        FilterBuilders.andFilter(
				        FilterBuilders.rangeFilter("@timestamp").from(esUtilTest.oneDayAgoFrom()).to(esUtilTest.nowDate()),
		        		FilterBuilders.termFilter("日志分类关键字", "login"))
		        );
		SearchResponse sr = client.prepareSearch(index).setTypes(type).setSearchType("count").setQuery(builder)
			    .addAggregation(
			    		AggregationBuilders.terms("create").field("注册时间").order(Terms.Order.term(false)).size(num)
			    		.subAggregation(AggregationBuilders.cardinality("agg").field("玩家GUID"))
			    		.subAggregation(AggregationBuilders.terms("server").field("服务器ID").size(srsize).subAggregation(AggregationBuilders.cardinality("agg").field("玩家GUID")))
			    ).execute().actionGet();
		System.out.println("-----------esServer---------------");

		UserRetained userRetained = new UserRetained();
		Terms genders = sr.getAggregations().get("create");	
		for (Terms.Bucket entry : genders.getBuckets()) {
			String dateBucket = sdf.format(new Date((Long) entry.getKeyAsNumber()));
			if(dateBucket.equals(esUtilTest.twoDayAgoFrom())){
				Terms serverZone = entry.getAggregations().get("server");
				for (Terms.Bucket e : serverZone.getBuckets()) {
				    Cardinality agg = e.getAggregations().get("agg");
				    Long aggcount = agg.getValue();
				    Double RetentionTwo ;
				    
				    Long r = createServerCount(e.getKey(),esUtilTest.twoDayAgoFrom(), esUtilTest.twoDayAgoTo());
				    if(r==0){
					    RetentionTwo = (double) 0.00;
				    }else{
				    	RetentionTwo = (double)aggcount*100/createServerCount(e.getKey(),esUtilTest.twoDayAgoFrom(), esUtilTest.twoDayAgoTo());
				    }

			    	userRetained.setDate(esUtilTest.twoDayAgoFrom());
			    	userRetained.setGameId(game);
			    	userRetained.setKey(UserRetained.KEY_SEVSER);
			    	userRetained.setCtRetained(UserRetained.CT_NEXTDAY);
			    	userRetained.setRetained(df.format(RetentionTwo));
			    	userRetained.setValue(e.getKey());
			    	userRetained.setTs(entry.getKey());	
				}
			}else if(dateBucket.equals(esUtilTest.eightDayAgoFrom())){
				Terms serverZone = entry.getAggregations().get("server");
				for (Terms.Bucket e : serverZone.getBuckets()) {
				    Cardinality agg = e.getAggregations().get("agg");
				    Long aggcount = agg.getValue();
				    Double RetentionEight ;
				    
				    RetentionEight = (double)aggcount*100/createServerCount(e.getKey(),esUtilTest.eightDayAgoFrom(), esUtilTest.eightDayAgoTo());
			    	userRetained.setDate(esUtilTest.eightDayAgoFrom());
			    	userRetained.setGameId(game);
			    	userRetained.setKey(UserRetained.KEY_SEVSER);
			    	userRetained.setCtRetained(UserRetained.CT_SEVENDAY);
			    	userRetained.setRetained(df.format(RetentionEight));
			    	userRetained.setValue(e.getKey());
			    	userRetained.setTs(entry.getKey());
				}
			}else if(dateBucket.equals(esUtilTest.thirtyOneDayAgoFrom())){
				Terms serverZone = entry.getAggregations().get("server");
				for (Terms.Bucket e : serverZone.getBuckets()) {
				    Cardinality agg = e.getAggregations().get("agg");
				    Long aggcount = agg.getValue();
				    Double RetentionThirty ;
				    
			    	RetentionThirty = (double)aggcount*100/createServerCount(e.getKey(),esUtilTest.thirtyOneDayAgoFrom(), esUtilTest.thirtyOneDayAgoTo());
			    	
			    	userRetained.setDate(esUtilTest.thirtyOneDayAgoFrom());
			    	userRetained.setGameId(game);
			    	userRetained.setKey(UserRetained.KEY_SEVSER);
			    	userRetained.setCtRetained(UserRetained.CT_THIRYTDAY);
			    	userRetained.setRetained(df.format(RetentionThirty));
			    	userRetained.setValue(e.getKey());
			    	userRetained.setTs(entry.getKey());

				}
			}
		}
		if(bulkRequest.numberOfActions()!=0){
			bulkRequest.add(client.prepareIndex(bulk_index, bulk_type_retained)
			        .setSource(jsonBuilder()
				           	 .startObject()
		                        .field("date", userRetained.getDate().split("T")[0])
		                        .field("gameId", userRetained.getGameId())
		                        .field("ctRetained", userRetained.getCtRetained())
		                        .field("retained", userRetained.getRetained())
		                        .field("key", userRetained.getKey())
		                        .field("value", userRetained.getValue())
		                        .field("@timestamp", new Date())
		                    .endObject()
			                  )
			        );
			bulkRequest.execute().actionGet();	
		}else{
			bulkRequest.add(client.prepareIndex(bulk_index, bulk_type_retained)
			        .setSource(
			                  )
			        );
			bulkRequest.execute().actionGet();	
		}
	}
	
	@Transactional(readOnly=false, propagation=Propagation.REQUIRED)
	public void schedual() throws Exception{
		System.out.println("----------------用户留存了 retained 调度开始");
		esAll();
		esServerZone();
		esPlatForm();
		esServer();
		System.out.println("----------------用户留存了 retained 调度结束");
	}
	

}