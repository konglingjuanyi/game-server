package com.enlight.game.service.es.fb;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.naming.LinkLoopException;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class FbUserTotalServer {
	

	@Autowired
	private Client client;
	
	private static final String index = "log_fb_user";
	
	private static final String type = "fb_user_total";
	
	public Map<String, Object> searchAllUserTotal(String dateFrom,String dateTo) throws IOException, ElasticsearchException, ParseException{

			SearchResponse response = client.prepareSearch(index)
			        .setTypes(type)
			        .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
			        .setPostFilter(
			                FilterBuilders.andFilter(
			        		        FilterBuilders.rangeFilter("date").from(dateFrom).to(dateTo),
			                		FilterBuilders.termFilter("key", "all"))
			        		)
			        .addSort("date", SortOrder.DESC)
			        .setFrom(0).setSize(daysBetween(dateFrom,dateTo)).setExplain(true)
			        .execute()
			        .actionGet();		
			return retained(response);
	}
	
	public Map<String, Object> searchServerZoneUserTotal(String dateFrom,String dateTo,String value) throws IOException, ElasticsearchException, ParseException{

		SearchResponse response = client.prepareSearch(index)
		        .setTypes(type)
		        .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
		        .setPostFilter(
		                FilterBuilders.andFilter(
		        		        FilterBuilders.rangeFilter("date").from(dateFrom).to(dateTo),
		                		FilterBuilders.termFilter("key", "serverZone"),
		                		FilterBuilders.termFilter("value", value))
		        		)
		        .addSort("date", SortOrder.DESC)
		        .setFrom(0).setSize(daysBetween(dateFrom,dateTo)).setExplain(true)
		        .execute()
		        .actionGet();		
		return retained(response);
	}
	public Map<String, Object> searchPlatFormUserTotal(String dateFrom,String dateTo,String value) throws IOException, ElasticsearchException, ParseException{

		SearchResponse response = client.prepareSearch(index)
		        .setTypes(type)
		        .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
		        .setPostFilter(
		                FilterBuilders.andFilter(
		        		        FilterBuilders.rangeFilter("date").from(dateFrom).to(dateTo),
		                		FilterBuilders.termFilter("key", "platForm"),
		                		FilterBuilders.termFilter("value", value))
		        		)
		        .addSort("date", SortOrder.DESC)
		        .setFrom(0).setSize(daysBetween(dateFrom,dateTo)).setExplain(true)
		        .execute()
		        .actionGet();
		
		return retained(response);
	}
	
	public Map<String, Object> searchServerUserTotal(String dateFrom,String dateTo,String value) throws IOException, ElasticsearchException, ParseException{

		SearchResponse response = client.prepareSearch(index)
		        .setTypes(type)
		        .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
		        .setPostFilter(
		                FilterBuilders.andFilter(
		        		        FilterBuilders.rangeFilter("date").from(dateFrom).to(dateTo),
		                		FilterBuilders.termFilter("key", "server"),
		                		FilterBuilders.termFilter("value", value))
		        		)
		        .addSort("date", SortOrder.DESC)
		        .setFrom(0).setSize(daysBetween(dateFrom,dateTo)).setExplain(true)
		        .execute()
		        .actionGet();
		
		return retained(response);
	}
	
	public Map<String, Object> retained(SearchResponse response){
			Map<String, Object> map = new HashMap<String, Object>();
			for (SearchHit hit : response.getHits()) {
				Map<String, Object> source = hit.getSource();
				map.put(source.get("date").toString(), source.get("userTotal").toString());
			}
			return map;
	}
	
		public static int daysBetween(String smdate,String bdate) throws ParseException{  
	        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd");  
	        Calendar cal = Calendar.getInstance();    
	        cal.setTime(sdf.parse(smdate));    
	        long time1 = cal.getTimeInMillis();                 	
	        cal.setTime(sdf.parse(bdate));    
	        long time2 = cal.getTimeInMillis();         
	        long between_days=(time2-time1)/(1000*3600*24);  
	            
	        return Integer.parseInt(String.valueOf(between_days));     
	    }  
	
}
