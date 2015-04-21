package com.enlight.game.web.controller.mgr.fb;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletRequest;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.elasticsearch.ElasticsearchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import org.springside.modules.web.Servlets;

import com.enlight.game.entity.EnumFunction;
import com.enlight.game.entity.PlatForm;
import com.enlight.game.entity.Server;
import com.enlight.game.entity.ServerZone;
import com.enlight.game.entity.Stores;
import com.enlight.game.service.account.AccountService;
import com.enlight.game.service.account.ShiroDbRealm.ShiroUser;
import com.enlight.game.service.es.EsRetainedServer;
import com.enlight.game.service.platForm.PlatFormService;
import com.enlight.game.service.server.ServerService;
import com.enlight.game.service.serverZone.ServerZoneService;
import com.enlight.game.service.store.StoreService;
import com.enlight.game.web.controller.mgr.BaseController;

@Controller("RetainedController")
@RequestMapping("/manage/retained")
public class RetainedController extends BaseController{
	
	private static final Logger logger = LoggerFactory.getLogger(RetainedController.class);
	
	SimpleDateFormat sdf =   new SimpleDateFormat("yyyy-MM-dd" ); 
	Calendar calendar = new GregorianCalendar(); 
	
	@Autowired
	private ServerZoneService serverZoneService;
	
	@Autowired
	private ServerService serverService;
	
	@Autowired
	private PlatFormService platFormService;
	
	@Autowired
	private EsRetainedServer EsRetainedServer;
	
	@Autowired
	private AccountService accountService;
	
	@Autowired
	private StoreService storeService;
	
	/**
	 * 用户留存
	 * @throws ParseException 
	 * @throws IOException 
	 * @throws ElasticsearchException 
	 */
	@RequiresRoles(value = { "admin", "28" }, logical = Logical.OR)
	@RequestMapping(value = "/fb/userRetained/{storeId}", method = RequestMethod.GET)
	public String userRetained(Model model,ServletRequest request,@PathVariable("storeId") final String storeId) throws ElasticsearchException, IOException, ParseException{
		logger.debug("user Retained...");
		model.addAttribute("user", EnumFunction.ENUM_USER);
		Map<String, Object> searchParams = Servlets.getParametersStartingWith(request, "search_");

		Stores stores = storeService.findById(Long.valueOf(storeId));
		List<ServerZone> serverZones = serverZoneService.findAll();
		String dateFrom = eightDayAgoFrom();
		String dateTo = nowDate();
		
		Map<String, Object> m = new HashMap<String, Object>();
		
		if(searchParams.isEmpty()){
			m = EsRetainedServer.searchAllRetained(dateFrom, dateTo);
			logger.debug("查看所有运营大区。。。all" );
		}else{
			if(searchParams.get("EQ_value")!="" && searchParams.get("EQ_value")!=null){
				if(searchParams.get("EQ_category").toString().equals("platForm")){
					PlatForm platform =  platFormService.findByPfName(searchParams.get("EQ_value").toString());
					m = EsRetainedServer.searchPlatFormRetained(dateFrom, dateTo, platform.getPfId());
					logger.debug("查看渠道。。。"+platform.getPfId());
				}else if(searchParams.get("EQ_category").toString().equals("server")){
					Server server = serverService.findByServerId(searchParams.get("EQ_value").toString());
					m = EsRetainedServer.searchServerRetained(dateFrom, dateTo, server.getServerId());
					logger.debug("查看服务器。。。"+server.getServerId());
				}
			}else{
				if(searchParams.get("EQ_serverZone").toString().equals("all")){
					m = EsRetainedServer.searchAllRetained(dateFrom, dateTo);
					logger.debug("查看所有运营大区。。。all" );
				}else{
					m = EsRetainedServer.searchServerZoneRetained(dateFrom, dateTo, searchParams.get("EQ_serverZone").toString());
					logger.debug("查看运营大区。。。" +searchParams.get("EQ_serverZone").toString());
				}
			}
		}
	
		model.addAttribute("datenext", m.get("datenext"));
		model.addAttribute("dateSeven", m.get("dateSeven"));
		model.addAttribute("datethirty", m.get("datethirty"));
		model.addAttribute("table", m.get("table"));
		
		model.addAttribute("store", stores);
		model.addAttribute("serverZone", serverZones);
		model.addAttribute("dateFrom", dateFrom);
		model.addAttribute("dateTo", dateTo);
		model.addAttribute("searchParams", Servlets.encodeParameterStringWithPrefix(searchParams, "search_"));
		return "/kibana/fb/user/retain";
	}
	
	
	public String nowDate(){
		String nowDate = sdf.format(new Date());
		return nowDate;
	}
	
	public String eightDayAgoFrom(){
	    calendar.setTime(new Date()); 
	    calendar.add(calendar.DATE,-30);
	    Date date=calendar.getTime();
	    String da = sdf.format(date); 
		return da;
	}
	
	@RequestMapping(value = "/findServerZone")
	@ResponseBody
	public List<ServerZone> findServerZone() {
		List<ServerZone> serverZones = serverZoneService.findAll();
		return serverZones;
	}
	
	@RequestMapping(value = "/findPlatForm")
	@ResponseBody
	public List<PlatForm> findPlatForm() {
		List<PlatForm> platForms = platFormService.findAll();
		return platForms;
	}
	
	@RequestMapping(value = "/findPlatFormByServerId")
	@ResponseBody
	public List<PlatForm> findPlatFormByServerId(@RequestParam(value="serverId")String serverId) {
		List<PlatForm> platForms = platFormService.findByServerZoneId(serverId);
		return platForms;
	}
	
	@RequestMapping(value = "/findServerByStoreId")
	@ResponseBody
	public Set<Server> findServerByStoreId(@RequestParam(value="storeId")String storeId){
		Set<Server> servers = serverService.findByStoreId(storeId);
		return servers;
		
	}
	
	@RequestMapping(value = "/findServerByStoreIdAndServerZoneId")
	@ResponseBody
	public Set<Server> findServerByStoreIdAndServerZoneId(@RequestParam(value="storeId")String storeId,@RequestParam(value="serverZoneId")String serverZoneId){
		Set<Server> servers = serverService.findByServerZoneIdAndStoreId(serverZoneId, storeId);
		return servers;
	}
	
	
	/**
	 * 服务器获取时间
	 */
	@RequestMapping(value="/getDate")
	@ResponseBody
	public Map<String, String> getDate(){
		Map<String,String> dateMap = new HashMap<String, String>();
		SimpleDateFormat sdf =   new SimpleDateFormat("yyyy-MM-dd" ); 
		Calendar calendar = new GregorianCalendar(); 
		String nowDate = sdf.format(new Date());
		
	    calendar.setTime(new Date()); 
	    calendar.add(calendar.DATE,-1);
	    String yesterday = sdf.format(calendar.getTime());
	    
	    calendar.setTime(new Date()); 
	    calendar.add(calendar.DATE,-7);
	    String sevenDayAgo = sdf.format(calendar.getTime()); 
	    
	    calendar.setTime(new Date()); 
	    calendar.add(calendar.DATE,-30);
	    String thirtyDayAgo = sdf.format(calendar.getTime()); 
		
	    dateMap.put("nowDate",nowDate);
	    dateMap.put("yesterday",yesterday);
	    dateMap.put("sevenDayAgo",sevenDayAgo);
	    dateMap.put("thirtyDayAgo",thirtyDayAgo);
		return dateMap;
	}
	
	/**
	 * 取出Shiro中的当前用户Id.
	 */
	public ShiroUser getCurrentUser() {
		ShiroUser user = (ShiroUser) SecurityUtils.getSubject().getPrincipal();
		return user;
	}
	
}