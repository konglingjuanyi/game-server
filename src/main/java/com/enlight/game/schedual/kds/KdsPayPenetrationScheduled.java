package com.enlight.game.schedual.kds;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.cardinality.Cardinality;
import org.elasticsearch.search.aggregations.metrics.sum.Sum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.enlight.game.util.EsUtil;

/**
 * 
 * @author apple
 * ARPU(日) ARPU(月) ARPPU(日) ARPPU(月)
 */
@Transactional(readOnly = true)
public class KdsPayPenetrationScheduled {
	
	@Autowired
	private Client client;
	
	//项目名称
	private static final String game = "KDS";
	
	private static final String index_user = "logstash-kds-user-*";
	
	private static final String index_money = "logstash-kds-money-*";
	
	private static final String type_user = "kds_user.log";
	
	private static final String type_money = "kds_money.log";
	
	
	private static final String bulk_index = "log_kds_money";
	
	private static final String bulk_type_money_arpuday = "kds_money_arpu_day";
	
	private static final String bulk_type_money_arpumouth = "kds_money_arpu_mouth";
	
	private static final String bulk_type_money_arppuday = "kds_money_arppu_day";
	
	private static final String bulk_type_money_arppumouth = "kds_money_arppu_mouth";
	
	private static final Integer szsize = 10; //运营大区
	
	private static final Integer pfsize = 300; //渠道
	
	private static final Integer srsize = 300; //服务器
	
	EsUtil esUtilTest = new EsUtil();
	

	//ARPU(日) ARPPU(日) 
	public void testdayall() throws IOException, ParseException {	
		BulkRequestBuilder bulkRequest = client.prepareBulk();
		DecimalFormat df = new DecimalFormat("0.00");//格式化小数  
		//收入金额
		SearchResponse paysum = client.prepareSearch(index_money).setSearchType("count").setTypes(type_money).setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(),
		        FilterBuilders.andFilter(
						FilterBuilders.rangeFilter("@timestamp").from(esUtilTest.oneDayAgoFrom()).to(esUtilTest.nowDate()),
				        FilterBuilders.termFilter("日志分类关键字", "money_get")
		        )))
		        .addAggregation(
		        		AggregationBuilders.sum("sum").field("支付金额")
			    ).execute().actionGet();
		Sum pays = paysum.getAggregations().get("sum");
		
		//充值人数
		SearchResponse peoplenum = client.prepareSearch(index_money).setSearchType("count").setTypes(type_money).setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(),
		        FilterBuilders.andFilter(
						FilterBuilders.rangeFilter("@timestamp").from(esUtilTest.oneDayAgoFrom()).to(esUtilTest.nowDate()),
				        FilterBuilders.termFilter("日志分类关键字", "money_get")
		        )))
		        .addAggregation(
		        		AggregationBuilders.cardinality("agg").field("玩家GUID")
			    ).execute().actionGet();
		Cardinality peoplen = peoplenum.getAggregations().get("agg");
		
		//活跃玩家
		SearchResponse active = client.prepareSearch(index_user).setSearchType("count").setTypes(type_user).setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(),
				FilterBuilders.andFilter(
				        FilterBuilders.rangeFilter("@timestamp").from(esUtilTest.oneDayAgoFrom()).to(esUtilTest.nowDate()),
				        FilterBuilders.termFilter("日志分类关键字", "login")
						))
		        ).addAggregation(
			    		AggregationBuilders.cardinality("agg").field("玩家GUID")
			    ).execute().actionGet();
		Cardinality ac = active.getAggregations().get("agg");
		
		double s,t;
		if(peoplen.getValue() == 0 ){
			s = 0.00;
		}else{
			s = pays.getValue()/peoplen.getValue();
		}
		if(ac.getValue() == 0 ){
			t = 0.00;
		}else{
			t = pays.getValue()/ac.getValue();
		}
		
		String time = esUtilTest.oneDayAgoFrom().split("T")[0]; 
		//arpu 日
		bulkRequest.add(client.prepareIndex(bulk_index, bulk_type_money_arpuday)
		        .setSource(jsonBuilder()
			           	 .startObject()
	                        .field("date", time)
	                        .field("gameId", game)
	                        .field("key", "all")
	                        .field("@timestamp", new Date())
	                        .field("ct", "arpuday") 
	                        .field("cv", df.format(t)) 
	                    .endObject()
		                  )
		        );
		//arppu 日
		bulkRequest.add(client.prepareIndex(bulk_index, bulk_type_money_arppuday)
		        .setSource(jsonBuilder()
			           	 .startObject()
	                        .field("date", time)
	                        .field("gameId", game)
	                        .field("key", "all")
	                        .field("@timestamp", new Date())
	                        .field("ct", "arppuday") 
	                        .field("cv", df.format(s)) 
	                    .endObject()
		                  )
		        );
		if(bulkRequest.numberOfActions()!=0){
			bulkRequest.execute().actionGet();	
		}
	}
	
	public void testdayserverzone() throws IOException, ParseException {	
		BulkRequestBuilder bulkRequest = client.prepareBulk();
		DecimalFormat df = new DecimalFormat("0.00");//格式化小数  
		Map<String, Double> mapsum = new HashMap<String, Double>();
		Map<String, Long> mapnum = new HashMap<String, Long>();
		Map<String, Long> mapac = new HashMap<String, Long>();
		//收入金额
		SearchResponse paysum = client.prepareSearch(index_money).setSearchType("count").setTypes(type_money).setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(),
		        FilterBuilders.andFilter(
						FilterBuilders.rangeFilter("@timestamp").from(esUtilTest.oneDayAgoFrom()).to(esUtilTest.nowDate()),
				        FilterBuilders.termFilter("日志分类关键字", "money_get")
		        )))
		        .addAggregation(
					AggregationBuilders.terms("serverZone").field("运营大区ID").size(szsize).subAggregation(
							AggregationBuilders.sum("sum").field("支付金额")
							)
				).execute().actionGet();
		Terms g1 = paysum.getAggregations().get("serverZone");
		for (Terms.Bucket e : g1.getBuckets()) {
			 Sum agg = e.getAggregations().get("sum");
			 mapsum.put(e.getKey(), agg.getValue());
		}

		
		//充值人数
		SearchResponse peoplenum = client.prepareSearch(index_money).setSearchType("count").setTypes(type_money).setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(),
		        FilterBuilders.andFilter(
						FilterBuilders.rangeFilter("@timestamp").from(esUtilTest.oneDayAgoFrom()).to(esUtilTest.nowDate()),
				        FilterBuilders.termFilter("日志分类关键字", "money_get")
		        )))
		        .addAggregation(
						AggregationBuilders.terms("serverZone").field("运营大区ID").size(szsize).subAggregation(
								AggregationBuilders.cardinality("agg").field("玩家GUID")
								)
			    ).execute().actionGet();
		Terms g2 = peoplenum.getAggregations().get("serverZone");
		for (Terms.Bucket e : g2.getBuckets()) {
			Cardinality peoplen = e.getAggregations().get("agg");
			mapnum.put(e.getKey(), peoplen.getValue());
		}

		
		//活跃玩家
		SearchResponse active = client.prepareSearch(index_user).setSearchType("count").setTypes(type_user).setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(),
				FilterBuilders.andFilter(
				        FilterBuilders.rangeFilter("@timestamp").from(esUtilTest.oneDayAgoFrom()).to(esUtilTest.nowDate()),
				        FilterBuilders.termFilter("日志分类关键字", "login")
						))
		        ).addAggregation(
						AggregationBuilders.terms("serverZone").field("运营大区ID").size(szsize).subAggregation(
								AggregationBuilders.cardinality("agg").field("玩家GUID")
								)
			    ).execute().actionGet();
		Terms g3 = active.getAggregations().get("serverZone");
		for (Terms.Bucket e : g3.getBuckets()) {
			Cardinality ac = e.getAggregations().get("agg");
			 mapac.put(e.getKey(), ac.getValue());
		}
		
		String time = esUtilTest.oneDayAgoFrom().split("T")[0]; 
		for(Entry<String,Double> entry : mapsum.entrySet()){
			if(mapac.containsKey(entry.getKey())){
				bulkRequest.add(client.prepareIndex(bulk_index, bulk_type_money_arpuday)
				        .setSource(jsonBuilder()
					           	 .startObject()
			                        .field("date", time)
			                        .field("gameId", game)
			                        .field("key", "serverZone")
			                        .field("value", entry.getKey())
			                        .field("@timestamp", new Date())
			                        .field("ct", "arpuday") 
			                        .field("cv", df.format(entry.getValue()/mapac.get(entry.getKey()))) 
			                    .endObject()
				                  )
				        );
			}
			if(mapnum.containsKey(entry.getKey())){
				bulkRequest.add(client.prepareIndex(bulk_index, bulk_type_money_arppuday)
				        .setSource(jsonBuilder()
					           	 .startObject()
			                        .field("date", time)
			                        .field("gameId", game)
			                        .field("key", "serverZone")
			                        .field("value", entry.getKey())
			                        .field("@timestamp", new Date())
			                        .field("ct", "arppuday") 
			                        .field("cv", df.format(entry.getValue()/mapnum.get(entry.getKey()))) 
			                    .endObject()
				                  )
				        );
			}
		}
		if(bulkRequest.numberOfActions()!=0){
			bulkRequest.execute().actionGet();	
		}
	}
	
	public void testdayplatform() throws IOException, ParseException {	
		BulkRequestBuilder bulkRequest = client.prepareBulk();
		DecimalFormat df = new DecimalFormat("0.00");//格式化小数  
		Map<String, Double> mapsum = new HashMap<String, Double>();
		Map<String, Long> mapnum = new HashMap<String, Long>();
		Map<String, Long> mapac = new HashMap<String, Long>();
		//收入金额
		SearchResponse paysum = client.prepareSearch(index_money).setSearchType("count").setTypes(type_money).setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(),
		        FilterBuilders.andFilter(
						FilterBuilders.rangeFilter("@timestamp").from(esUtilTest.oneDayAgoFrom()).to(esUtilTest.nowDate()),
				        FilterBuilders.termFilter("日志分类关键字", "money_get")
		        )))
		        .addAggregation(
					AggregationBuilders.terms("platForm").field("渠道ID").size(pfsize).subAggregation(
							AggregationBuilders.sum("sum").field("支付金额")
							)
				).execute().actionGet();
		Terms g1 = paysum.getAggregations().get("platForm");
		for (Terms.Bucket e : g1.getBuckets()) {
			 Sum agg = e.getAggregations().get("sum");
			 mapsum.put(e.getKey(), agg.getValue());
		}

		
		//充值人数
		SearchResponse peoplenum = client.prepareSearch(index_money).setSearchType("count").setTypes(type_money).setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(),
		        FilterBuilders.andFilter(
						FilterBuilders.rangeFilter("@timestamp").from(esUtilTest.oneDayAgoFrom()).to(esUtilTest.nowDate()),
				        FilterBuilders.termFilter("日志分类关键字", "money_get")
		        )))
		        .addAggregation(
						AggregationBuilders.terms("platForm").field("渠道ID").size(pfsize).subAggregation(
								AggregationBuilders.cardinality("agg").field("玩家GUID")
								)
			    ).execute().actionGet();
		Terms g2 = peoplenum.getAggregations().get("platForm");
		for (Terms.Bucket e : g2.getBuckets()) {
			Cardinality peoplen = e.getAggregations().get("agg");
			mapnum.put(e.getKey(), peoplen.getValue());
		}

		
		//活跃玩家
		SearchResponse active = client.prepareSearch(index_user).setSearchType("count").setTypes(type_user).setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(),
				FilterBuilders.andFilter(
				        FilterBuilders.rangeFilter("@timestamp").from(esUtilTest.oneDayAgoFrom()).to(esUtilTest.nowDate()),
				        FilterBuilders.termFilter("日志分类关键字", "login")
						))
		        ).addAggregation(
						AggregationBuilders.terms("platForm").field("渠道ID").size(pfsize).subAggregation(
								AggregationBuilders.cardinality("agg").field("玩家GUID")
								)
			    ).execute().actionGet();
		Terms g3 = active.getAggregations().get("platForm");
		for (Terms.Bucket e : g3.getBuckets()) {
			Cardinality ac = e.getAggregations().get("agg");
			 mapac.put(e.getKey(), ac.getValue());
		}
		
		String time = esUtilTest.oneDayAgoFrom().split("T")[0]; 
		for(Entry<String,Double> entry : mapsum.entrySet()){
			if(mapac.containsKey(entry.getKey())){
				bulkRequest.add(client.prepareIndex(bulk_index, bulk_type_money_arpuday)
				        .setSource(jsonBuilder()
					           	 .startObject()
			                        .field("date", time)
			                        .field("gameId", game)
			                        .field("key", "platForm")
			                        .field("value", entry.getKey())
			                        .field("@timestamp", new Date())
			                        .field("ct", "arpuday") 
			                        .field("cv", df.format(entry.getValue()/mapac.get(entry.getKey()))) 
			                    .endObject()
				                  )
				        );
			}
			if(mapnum.containsKey(entry.getKey())){
				bulkRequest.add(client.prepareIndex(bulk_index, bulk_type_money_arppuday)
				        .setSource(jsonBuilder()
					           	 .startObject()
			                        .field("date", time)
			                        .field("gameId", game)
			                        .field("key", "platForm")
			                        .field("value", entry.getKey())
			                        .field("@timestamp", new Date())
			                        .field("ct", "arppuday") 
			                        .field("cv", df.format(entry.getValue()/mapnum.get(entry.getKey()))) 
			                    .endObject()
				                  )
				        );
			}
		}
		if(bulkRequest.numberOfActions()!=0){
			bulkRequest.execute().actionGet();	
		}
	}
	
	public void testdayserver() throws IOException, ParseException {	
		BulkRequestBuilder bulkRequest = client.prepareBulk();
		DecimalFormat df = new DecimalFormat("0.00");//格式化小数  
		Map<String, Double> mapsum = new HashMap<String, Double>();
		Map<String, Long> mapnum = new HashMap<String, Long>();
		Map<String, Long> mapac = new HashMap<String, Long>();
		//收入金额
		SearchResponse paysum = client.prepareSearch(index_money).setSearchType("count").setTypes(type_money).setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(),
		        FilterBuilders.andFilter(
						FilterBuilders.rangeFilter("@timestamp").from(esUtilTest.oneDayAgoFrom()).to(esUtilTest.nowDate()),
				        FilterBuilders.termFilter("日志分类关键字", "money_get")
		        )))
		        .addAggregation(
					AggregationBuilders.terms("server").field("服务器ID").size(srsize).subAggregation(
							AggregationBuilders.sum("sum").field("支付金额")
							)
				).execute().actionGet();
		Terms g1 = paysum.getAggregations().get("server");
		for (Terms.Bucket e : g1.getBuckets()) {
			 Sum agg = e.getAggregations().get("sum");
			 mapsum.put(e.getKey(), agg.getValue());
		}

		
		//充值人数
		SearchResponse peoplenum = client.prepareSearch(index_money).setSearchType("count").setTypes(type_money).setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(),
		        FilterBuilders.andFilter(
						FilterBuilders.rangeFilter("@timestamp").from(esUtilTest.oneDayAgoFrom()).to(esUtilTest.nowDate()),
				        FilterBuilders.termFilter("日志分类关键字", "money_get")
		        )))
		        .addAggregation(
						AggregationBuilders.terms("server").field("服务器ID").size(srsize).subAggregation(
								AggregationBuilders.cardinality("agg").field("玩家GUID")
								)
			    ).execute().actionGet();
		Terms g2 = peoplenum.getAggregations().get("server");
		for (Terms.Bucket e : g2.getBuckets()) {
			Cardinality peoplen = e.getAggregations().get("agg");
			mapnum.put(e.getKey(), peoplen.getValue());
		}

		
		//活跃玩家
		SearchResponse active = client.prepareSearch(index_user).setSearchType("count").setTypes(type_user).setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(),
				FilterBuilders.andFilter(
				        FilterBuilders.rangeFilter("@timestamp").from(esUtilTest.oneDayAgoFrom()).to(esUtilTest.nowDate()),
				        FilterBuilders.termFilter("日志分类关键字", "login")
						))
		        ).addAggregation(
						AggregationBuilders.terms("server").field("服务器ID").size(srsize).subAggregation(
								AggregationBuilders.cardinality("agg").field("玩家GUID")
								)
			    ).execute().actionGet();
		Terms g3 = active.getAggregations().get("server");
		for (Terms.Bucket e : g3.getBuckets()) {
			Cardinality ac = e.getAggregations().get("agg");
			 mapac.put(e.getKey(), ac.getValue());
		}
		
		String time = esUtilTest.oneDayAgoFrom().split("T")[0]; 
		for(Entry<String,Double> entry : mapsum.entrySet()){
			if(mapac.containsKey(entry.getKey())){
				bulkRequest.add(client.prepareIndex(bulk_index, bulk_type_money_arpuday)
				        .setSource(jsonBuilder()
					           	 .startObject()
			                        .field("date", time)
			                        .field("gameId", game)
			                        .field("key", "server")
			                        .field("value", entry.getKey())
			                        .field("@timestamp", new Date())
			                        .field("ct", "arpuday") 
			                        .field("cv", df.format(entry.getValue()/mapac.get(entry.getKey()))) 
			                    .endObject()
				                  )
				        );
			}
			if(mapnum.containsKey(entry.getKey())){
				bulkRequest.add(client.prepareIndex(bulk_index, bulk_type_money_arppuday)
				        .setSource(jsonBuilder()
					           	 .startObject()
			                        .field("date", time)
			                        .field("gameId", game)
			                        .field("key", "server")
			                        .field("value", entry.getKey())
			                        .field("@timestamp", new Date())
			                        .field("ct", "arppuday") 
			                        .field("cv", df.format(entry.getValue()/mapnum.get(entry.getKey()))) 
			                    .endObject()
				                  )
				        );
			}
		}
		if(bulkRequest.numberOfActions()!=0){
			bulkRequest.execute().actionGet();	
		}
	}
	
	

	public void testmouthall() throws IOException, ParseException {	
		BulkRequestBuilder bulkRequest = client.prepareBulk();
		DecimalFormat df = new DecimalFormat("0.00");//格式化小数  
		//收入金额
		SearchResponse paysum = client.prepareSearch(index_money).setSearchType("count").setTypes(type_money).setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(),
		        FilterBuilders.andFilter(
						FilterBuilders.rangeFilter("@timestamp").from(esUtilTest.presentfirstday()).to(esUtilTest.lastmouthfirstday()),
				        FilterBuilders.termFilter("日志分类关键字", "money_get")
		        )))
		        .addAggregation(
		        		AggregationBuilders.sum("sum").field("支付金额")
			    ).execute().actionGet();
		Sum pays = paysum.getAggregations().get("sum");
		
		//充值人数
		SearchResponse peoplenum = client.prepareSearch(index_money).setSearchType("count").setTypes(type_money).setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(),
		        FilterBuilders.andFilter(
						FilterBuilders.rangeFilter("@timestamp").from(esUtilTest.presentfirstday()).to(esUtilTest.lastmouthfirstday()),
				        FilterBuilders.termFilter("日志分类关键字", "money_get")
		        )))
		        .addAggregation(
		        		AggregationBuilders.cardinality("agg").field("玩家GUID")
			    ).execute().actionGet();
		Cardinality peoplen = peoplenum.getAggregations().get("agg");
		
		//活跃玩家
		SearchResponse active = client.prepareSearch(index_user).setSearchType("count").setTypes(type_user).setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(),
				FilterBuilders.andFilter(
				        FilterBuilders.rangeFilter("@timestamp").from(esUtilTest.presentfirstday()).to(esUtilTest.lastmouthfirstday()),
				        FilterBuilders.termFilter("日志分类关键字", "login")
						))
		        ).addAggregation(
			    		AggregationBuilders.cardinality("agg").field("玩家GUID")
			    ).execute().actionGet();
		Cardinality ac = active.getAggregations().get("agg");
		
		double s,t;
		if(peoplen.getValue() == 0 ){
			s = 0.00;
		}else{
			s = pays.getValue()/peoplen.getValue();
		}
		if(ac.getValue() == 0 ){
			t = 0.00;
		}else{
			t = pays.getValue()/ac.getValue();
		}
		
		System.out.println(df.format(s) + "        "  + df.format(t));
		
		//arpu 月
		bulkRequest.add(client.prepareIndex(bulk_index, bulk_type_money_arpumouth,esUtilTest.lastmouth()+"_all")
		        .setSource(jsonBuilder()
			           	 .startObject()
	                        .field("date", esUtilTest.lastmouth())
	                        .field("gameId", game)
	                        .field("key", "all")
	                        .field("@timestamp", new Date())
	                        .field("ct", "arpumouth") 
	                        .field("cv", df.format(t)) 
	                    .endObject()
		                  )
		        );
		//arppu 月
		bulkRequest.add(client.prepareIndex(bulk_index, bulk_type_money_arppumouth,esUtilTest.lastmouth()+"_all")
		        .setSource(jsonBuilder()
			           	 .startObject()
	                        .field("date", esUtilTest.lastmouth())
	                        .field("gameId", game)
	                        .field("key", "all")
	                        .field("@timestamp", new Date())
	                        .field("ct", "arppumouth") 
	                        .field("cv", df.format(s)) 
	                    .endObject()
		                  )
		        );
		if(bulkRequest.numberOfActions()!=0){
			bulkRequest.execute().actionGet();	
		}
	}
	
	public void testmouthserverzone() throws IOException, ParseException {	
		BulkRequestBuilder bulkRequest = client.prepareBulk();
		DecimalFormat df = new DecimalFormat("0.00");//格式化小数  
		Map<String, Double> mapsum = new HashMap<String, Double>();
		Map<String, Long> mapnum = new HashMap<String, Long>();
		Map<String, Long> mapac = new HashMap<String, Long>();
		//收入金额
		SearchResponse paysum = client.prepareSearch(index_money).setSearchType("count").setTypes(type_money).setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(),
		        FilterBuilders.andFilter(
						FilterBuilders.rangeFilter("@timestamp").from(esUtilTest.presentfirstday()).to(esUtilTest.lastmouthfirstday()),
				        FilterBuilders.termFilter("日志分类关键字", "money_get")
		        )))
		        .addAggregation(
					AggregationBuilders.terms("serverZone").field("运营大区ID").size(szsize).subAggregation(
							AggregationBuilders.sum("sum").field("支付金额")
							)
				).execute().actionGet();
		Terms g1 = paysum.getAggregations().get("serverZone");
		for (Terms.Bucket e : g1.getBuckets()) {
			 Sum agg = e.getAggregations().get("sum");
			 mapsum.put(e.getKey(), agg.getValue());
		}

		
		//充值人数
		SearchResponse peoplenum = client.prepareSearch(index_money).setSearchType("count").setTypes(type_money).setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(),
		        FilterBuilders.andFilter(
						FilterBuilders.rangeFilter("@timestamp").from(esUtilTest.presentfirstday()).to(esUtilTest.lastmouthfirstday()),
				        FilterBuilders.termFilter("日志分类关键字", "money_get")
		        )))
		        .addAggregation(
						AggregationBuilders.terms("serverZone").field("运营大区ID").size(szsize).subAggregation(
								AggregationBuilders.cardinality("agg").field("玩家GUID")
								)
			    ).execute().actionGet();
		Terms g2 = peoplenum.getAggregations().get("serverZone");
		for (Terms.Bucket e : g2.getBuckets()) {
			Cardinality peoplen = e.getAggregations().get("agg");
			mapnum.put(e.getKey(), peoplen.getValue());
		}

		
		//活跃玩家
		SearchResponse active = client.prepareSearch(index_user).setSearchType("count").setTypes(type_user).setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(),
				FilterBuilders.andFilter(
				        FilterBuilders.rangeFilter("@timestamp").from(esUtilTest.presentfirstday()).to(esUtilTest.lastmouthfirstday()),
				        FilterBuilders.termFilter("日志分类关键字", "login")
						))
		        ).addAggregation(
						AggregationBuilders.terms("serverZone").field("运营大区ID").size(szsize).subAggregation(
								AggregationBuilders.cardinality("agg").field("玩家GUID")
								)
			    ).execute().actionGet();
		Terms g3 = active.getAggregations().get("serverZone");
		for (Terms.Bucket e : g3.getBuckets()) {
			Cardinality ac = e.getAggregations().get("agg");
			 mapac.put(e.getKey(), ac.getValue());
		}
		
		for(Entry<String,Double> entry : mapsum.entrySet()){
			if(mapac.containsKey(entry.getKey())){
				bulkRequest.add(client.prepareIndex(bulk_index, bulk_type_money_arpumouth,esUtilTest.lastmouth()+"_serverZone_"+entry.getKey())
				        .setSource(jsonBuilder()
					           	 .startObject()
			                        .field("date", esUtilTest.lastmouth())
			                        .field("gameId", game)
			                        .field("key", "serverZone")
			                        .field("value", entry.getKey())
			                        .field("@timestamp", new Date())
			                        .field("ct", "arpuday") 
			                        .field("cv", df.format(entry.getValue()/mapac.get(entry.getKey()))) 
			                    .endObject()
				                  )
				        );
			}
			if(mapnum.containsKey(entry.getKey())){
				bulkRequest.add(client.prepareIndex(bulk_index, bulk_type_money_arppumouth,esUtilTest.lastmouth()+"_serverZone_"+entry.getKey())
				        .setSource(jsonBuilder()
					           	 .startObject()
			                        .field("date", esUtilTest.lastmouth())
			                        .field("gameId", game)
			                        .field("key", "serverZone")
			                        .field("value", entry.getKey())
			                        .field("@timestamp", new Date())
			                        .field("ct", "arppuday") 
			                        .field("cv", df.format(entry.getValue()/mapnum.get(entry.getKey()))) 
			                    .endObject()
				                  )
				        );
			}
		}
		if(!mapsum.isEmpty()){
			if(bulkRequest.numberOfActions()!=0){
			bulkRequest.execute().actionGet();	
		}
		}
		
	}
	
	public void testmouthplatform() throws IOException, ParseException {	
		BulkRequestBuilder bulkRequest = client.prepareBulk();
		DecimalFormat df = new DecimalFormat("0.00");//格式化小数  
		Map<String, Double> mapsum = new HashMap<String, Double>();
		Map<String, Long> mapnum = new HashMap<String, Long>();
		Map<String, Long> mapac = new HashMap<String, Long>();
		//收入金额
		SearchResponse paysum = client.prepareSearch(index_money).setSearchType("count").setTypes(type_money).setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(),
		        FilterBuilders.andFilter(
						FilterBuilders.rangeFilter("@timestamp").from(esUtilTest.presentfirstday()).to(esUtilTest.lastmouthfirstday()),
				        FilterBuilders.termFilter("日志分类关键字", "money_get")
		        )))
		        .addAggregation(
					AggregationBuilders.terms("platForm").field("渠道ID").size(pfsize).subAggregation(
							AggregationBuilders.sum("sum").field("支付金额")
							)
				).execute().actionGet();
		Terms g1 = paysum.getAggregations().get("platForm");
		for (Terms.Bucket e : g1.getBuckets()) {
			 Sum agg = e.getAggregations().get("sum");
			 mapsum.put(e.getKey(), agg.getValue());
		}

		
		//充值人数
		SearchResponse peoplenum = client.prepareSearch(index_money).setSearchType("count").setTypes(type_money).setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(),
		        FilterBuilders.andFilter(
						FilterBuilders.rangeFilter("@timestamp").from(esUtilTest.presentfirstday()).to(esUtilTest.lastmouthfirstday()),
				        FilterBuilders.termFilter("日志分类关键字", "money_get")
		        )))
		        .addAggregation(
						AggregationBuilders.terms("platForm").field("渠道ID").size(pfsize).subAggregation(
								AggregationBuilders.cardinality("agg").field("玩家GUID")
								)
			    ).execute().actionGet();
		Terms g2 = peoplenum.getAggregations().get("platForm");
		for (Terms.Bucket e : g2.getBuckets()) {
			Cardinality peoplen = e.getAggregations().get("agg");
			mapnum.put(e.getKey(), peoplen.getValue());
		}

		
		//活跃玩家
		SearchResponse active = client.prepareSearch(index_user).setSearchType("count").setTypes(type_user).setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(),
				FilterBuilders.andFilter(
				        FilterBuilders.rangeFilter("@timestamp").from(esUtilTest.presentfirstday()).to(esUtilTest.lastmouthfirstday()),
				        FilterBuilders.termFilter("日志分类关键字", "login")
						))
		        ).addAggregation(
						AggregationBuilders.terms("platForm").field("渠道ID").size(pfsize).subAggregation(
								AggregationBuilders.cardinality("agg").field("玩家GUID")
								)
			    ).execute().actionGet();
		Terms g3 = active.getAggregations().get("platForm");
		for (Terms.Bucket e : g3.getBuckets()) {
			Cardinality ac = e.getAggregations().get("agg");
			 mapac.put(e.getKey(), ac.getValue());
		}
		
		for(Entry<String,Double> entry : mapsum.entrySet()){
			if(mapac.containsKey(entry.getKey())){
				bulkRequest.add(client.prepareIndex(bulk_index, bulk_type_money_arpumouth,esUtilTest.lastmouth()+"_platForm_"+entry.getKey())
				        .setSource(jsonBuilder()
					           	 .startObject()
			                        .field("date", esUtilTest.lastmouth())
			                        .field("gameId", game)
			                        .field("key", "platForm")
			                        .field("value", entry.getKey())
			                        .field("@timestamp", new Date())
			                        .field("ct", "arpuday") 
			                        .field("cv", df.format(entry.getValue()/mapac.get(entry.getKey()))) 
			                    .endObject()
				                  )
				        );
			}
			if(mapnum.containsKey(entry.getKey())){
				bulkRequest.add(client.prepareIndex(bulk_index, bulk_type_money_arppumouth,esUtilTest.lastmouth()+"_platForm_"+entry.getKey())
				        .setSource(jsonBuilder()
					           	 .startObject()
			                        .field("date", esUtilTest.lastmouth())
			                        .field("gameId", game)
			                        .field("key", "platForm")
			                        .field("value", entry.getKey())
			                        .field("@timestamp", new Date())
			                        .field("ct", "arppuday") 
			                        .field("cv", df.format(entry.getValue()/mapnum.get(entry.getKey()))) 
			                    .endObject()
				                  )
				        );
			}
		}
		if(!mapsum.isEmpty()){
			if(bulkRequest.numberOfActions()!=0){
			bulkRequest.execute().actionGet();	
		}
		}
	}
	
	public void testmouthserver() throws IOException, ParseException {	
		BulkRequestBuilder bulkRequest = client.prepareBulk();
		DecimalFormat df = new DecimalFormat("0.00");//格式化小数  
		Map<String, Double> mapsum = new HashMap<String, Double>();
		Map<String, Long> mapnum = new HashMap<String, Long>();
		Map<String, Long> mapac = new HashMap<String, Long>();
		//收入金额
		SearchResponse paysum = client.prepareSearch(index_money).setSearchType("count").setTypes(type_money).setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(),
		        FilterBuilders.andFilter(
						FilterBuilders.rangeFilter("@timestamp").from(esUtilTest.presentfirstday()).to(esUtilTest.lastmouthfirstday()),
				        FilterBuilders.termFilter("日志分类关键字", "money_get")
		        )))
		        .addAggregation(
					AggregationBuilders.terms("server").field("服务器ID").size(srsize).subAggregation(
							AggregationBuilders.sum("sum").field("支付金额")
							)
				).execute().actionGet();
		Terms g1 = paysum.getAggregations().get("server");
		for (Terms.Bucket e : g1.getBuckets()) {
			 Sum agg = e.getAggregations().get("sum");
			 mapsum.put(e.getKey(), agg.getValue());
		}

		
		//充值人数
		SearchResponse peoplenum = client.prepareSearch(index_money).setSearchType("count").setTypes(type_money).setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(),
		        FilterBuilders.andFilter(
						FilterBuilders.rangeFilter("@timestamp").from(esUtilTest.presentfirstday()).to(esUtilTest.lastmouthfirstday()),
				        FilterBuilders.termFilter("日志分类关键字", "money_get")
		        )))
		        .addAggregation(
						AggregationBuilders.terms("server").field("服务器ID").size(srsize).subAggregation(
								AggregationBuilders.cardinality("agg").field("玩家GUID")
								)
			    ).execute().actionGet();
		Terms g2 = peoplenum.getAggregations().get("server");
		for (Terms.Bucket e : g2.getBuckets()) {
			Cardinality peoplen = e.getAggregations().get("agg");
			mapnum.put(e.getKey(), peoplen.getValue());
		}

		
		//活跃玩家
		SearchResponse active = client.prepareSearch(index_user).setSearchType("count").setTypes(type_user).setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(),
				FilterBuilders.andFilter(
				        FilterBuilders.rangeFilter("@timestamp").from(esUtilTest.presentfirstday()).to(esUtilTest.lastmouthfirstday()),
				        FilterBuilders.termFilter("日志分类关键字", "login")
						))
		        ).addAggregation(
						AggregationBuilders.terms("server").field("服务器ID").size(srsize).subAggregation(
								AggregationBuilders.cardinality("agg").field("玩家GUID")
								)
			    ).execute().actionGet();
		Terms g3 = active.getAggregations().get("server");
		for (Terms.Bucket e : g3.getBuckets()) {
			Cardinality ac = e.getAggregations().get("agg");
			 mapac.put(e.getKey(), ac.getValue());
		}
		
		for(Entry<String,Double> entry : mapsum.entrySet()){
			if(mapac.containsKey(entry.getKey())){
				bulkRequest.add(client.prepareIndex(bulk_index, bulk_type_money_arpumouth,esUtilTest.lastmouth()+"_platForm_"+entry.getKey())
				        .setSource(jsonBuilder()
					           	 .startObject()
			                        .field("date", esUtilTest.lastmouth())
			                        .field("gameId", game)
			                        .field("key", "server")
			                        .field("value", entry.getKey())
			                        .field("@timestamp", new Date())
			                        .field("ct", "arpuday") 
			                        .field("cv", df.format(entry.getValue()/mapac.get(entry.getKey()))) 
			                    .endObject()
				                  )
				        );
			}
			if(mapnum.containsKey(entry.getKey())){
				bulkRequest.add(client.prepareIndex(bulk_index, bulk_type_money_arppumouth,esUtilTest.lastmouth()+"_platForm_"+entry.getKey())
				        .setSource(jsonBuilder()
					           	 .startObject()
			                        .field("date", esUtilTest.lastmouth())
			                        .field("gameId", game)
			                        .field("key", "server")
			                        .field("value", entry.getKey())
			                        .field("@timestamp", new Date())
			                        .field("ct", "arppuday") 
			                        .field("cv", df.format(entry.getValue()/mapnum.get(entry.getKey()))) 
			                    .endObject()
				                  )
				        );
			}
		}
		if(!mapsum.isEmpty()){
			if(bulkRequest.numberOfActions()!=0){
			bulkRequest.execute().actionGet();	
		}
		}
	}
	
	@Transactional(readOnly=false, propagation=Propagation.REQUIRED)
	public void schedual() throws Exception{
		System.out.println("----------------ARPU(日) ARPU(月) ARPPU(日) ARPPU(月) 开始");
		testdayall();
		testdayserverzone();
		testdayplatform();
		testdayserver();
		testmouthall();
		testmouthserverzone();
		testmouthplatform();
		testmouthserver();
		System.out.println("----------------ARPU(日) ARPU(月) ARPPU(日) ARPPU(月) 调度结束");
	}
	
}
