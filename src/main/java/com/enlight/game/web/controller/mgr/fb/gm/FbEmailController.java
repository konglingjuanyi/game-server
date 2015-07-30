package com.enlight.game.web.controller.mgr.fb.gm;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletRequest;

import net.sf.json.JSONObject;

import org.apache.shiro.SecurityUtils;
import org.codehaus.jackson.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springside.modules.web.Servlets;

import com.enlight.game.entity.Server;
import com.enlight.game.entity.ServerZone;
import com.enlight.game.entity.Stores;
import com.enlight.game.entity.User;
import com.enlight.game.service.account.AccountService;
import com.enlight.game.service.account.ShiroDbRealm.ShiroUser;
import com.enlight.game.service.enumCategory.EnumCategoryService;
import com.enlight.game.service.enumFunction.EnumFunctionService;
import com.enlight.game.service.platForm.PlatFormService;
import com.enlight.game.service.server.ServerService;
import com.enlight.game.service.serverZone.ServerZoneService;
import com.enlight.game.service.store.StoreService;
import com.enlight.game.util.HttpClientUts;
import com.enlight.game.util.JsonBinder;
import com.enlight.game.web.controller.mgr.BaseController;
import com.enlight.game.entity.fb.gm.Annex;
import com.enlight.game.entity.fb.gm.Category;
import com.enlight.game.entity.fb.gm.Email;
import com.google.common.collect.Maps;

@Controller("fbEmailController")
@RequestMapping("/manage/gm/fb/email")
public class FbEmailController extends BaseController{

	private static final String PAGE_SIZE = "10";

	private static final Logger logger = LoggerFactory.getLogger(FbEmailController.class);
	
	private static Map<String, String> sortTypes = Maps.newLinkedHashMap();
	
	private static JsonBinder binder = JsonBinder.buildNonDefaultBinder();
	
	static {
		sortTypes.put("auto", "自动");
	}

	public static Map<String, String> getSortTypes() {
		return sortTypes;
	}

	public static void setSortTypes(Map<String, String> sortTypes) {
		FbEmailController.sortTypes = sortTypes;
	}
	
	@Autowired
	private AccountService accountService;
	
	@Autowired
	private EnumFunctionService enumFunctionService;
	
	@Autowired
	private StoreService storeService;
	
	@Autowired
	private ServerZoneService serverZoneService;
	
	@Autowired
	private EnumCategoryService enumCategoryService;
	
	@Autowired
	private PlatFormService platFormService;
	
	@Autowired
	private ServerService serverService;
	
	@Value("#{envProps.gm_url}")
	private String gm_url;

	
	/**
	 * @param pageNumber 当前	 
	 * @param pageSize   显示条数
	 * @param sortType  排序
	 * @param model   返回对象
	 * @param request  封装的请	
	 */
	@RequestMapping(value = "index", method = RequestMethod.GET)
	public String index(@RequestParam(value = "page", defaultValue = "1") int pageNumber,
			@RequestParam(value = "page.size", defaultValue = PAGE_SIZE) int pageSize,
			@RequestParam(value = "sortType", defaultValue = "auto")String sortType, Model model,
			ServletRequest request){
		ShiroUser user = getCurrentUser();
		User u = accountService.getUser(user.id);
		
		Map<String, Object> searchParams = Servlets.getParametersStartingWith(request, "search_");
		String storeId = request.getParameter("search_LIKE_storeId");
		String serverZoneId =  request.getParameter("search_LIKE_serverZoneId");
		String serverId = request.getParameter("search_LIKE_serverId");
		
		if (!u.getRoles().equals(User.USER_ROLE_ADMIN)) {
			List<Stores> stores = new ArrayList<Stores>();
			Stores sto=  storeService.findById(Long.valueOf(user.getStoreId()));
			stores.add(sto);
			List<ServerZone> serverZones = new ArrayList<ServerZone>();
			List<String> s = u.getServerZoneList();
			for (String str : s) {
				ServerZone server = serverZoneService.findById(Long.valueOf(str));
				serverZones.add(server);
			}
			model.addAttribute("stores", stores);
			model.addAttribute("serverZones", serverZones);
		}else{
			List<Stores> stores =  storeService.findList();
			List<ServerZone> serverZones = serverZoneService.findAll();
			model.addAttribute("stores", stores);
			model.addAttribute("serverZones", serverZones);
		}
		
		try {
	        if(!searchParams.isEmpty() && null != request.getParameter("search_LIKE_serverId")){
				if(!u.getRoles().equals(User.USER_ROLE_ADMIN)){
					storeId = user.getStoreId();
				}
				String gs = HttpClientUts.doGet(gm_url+"/fbserver/email/getAllEmails"+"?serverZoneId="+serverZoneId+"&gameId="+storeId+"&serverId="+URLEncoder.encode(serverId, "utf-8")+"&pageNumber="+pageNumber+"&pageSize="+pageSize, "utf-8");
				String total = HttpClientUts.doGet(gm_url+"/fbserver/getTotalByServerZoneIdAndGameId"+"?serverZoneId="+serverZoneId+"&gameId="+storeId+"&category="+Category.email, "utf-8");
				JSONObject dataJson=JSONObject.fromObject(total);
				PageRequest pageRequest = buildPageRequest(pageNumber, pageSize, sortType);
		        List<Email> beanList = binder.getMapper().readValue(gs, new TypeReference<List<Email>>() {}); 
		        PageImpl<Email> email = new PageImpl<Email>(beanList, pageRequest, Long.valueOf(dataJson.get("num").toString()));
				model.addAttribute("email", email);
				Set<Server> servers = serverService.findByServerZoneIdAndStoreId(serverZoneId,request.getParameter("search_LIKE_storeId"));
				model.addAttribute("servers", servers);
	        }else{
	        	List<Email> beanList = new ArrayList<Email>();
				PageRequest pageRequest = buildPageRequest(pageNumber, pageSize, sortType);
		        PageImpl<Email> email = new PageImpl<Email>(beanList, pageRequest, 0);
				model.addAttribute("email", email);
				
				Set<Server> servers = new HashSet<Server>();
				model.addAttribute("servers", servers);
	        }
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		model.addAttribute("sortType", sortType);
		model.addAttribute("sortTypes", sortTypes);
		// 将搜索条件编码成字符串，用于排序，分页的URL
		model.addAttribute("searchParams", Servlets.encodeParameterStringWithPrefix(searchParams, "search_"));
		return "/gm/fb/email/index";
	}
	
	/**
	 * 新增页面
	 */
	@RequestMapping(value = "/add" ,method=RequestMethod.GET)
	public String add(Model model){
		ShiroUser user = getCurrentUser();
		User u = accountService.getUser(user.id);
		if (!u.getRoles().equals(User.USER_ROLE_ADMIN)) {
			List<Stores> stores = new ArrayList<Stores>();
			Stores sto=  storeService.findById(Long.valueOf(user.getStoreId()));
			stores.add(sto);
			List<ServerZone> serverZones = new ArrayList<ServerZone>();
			List<String> s = u.getServerZoneList();
			for (String str : s) {
				ServerZone server = serverZoneService.findById(Long.valueOf(str));
				serverZones.add(server);
			}
			model.addAttribute("stores", stores);
			model.addAttribute("serverZones", serverZones);
		}else{
			List<Stores> stores =  storeService.findList();
			List<ServerZone> serverZones = serverZoneService.findAll();
			model.addAttribute("stores", stores);
			model.addAttribute("serverZones", serverZones);
		}
		return "/gm/fb/email/add";
	}
	
	/**
	 * 修改
	 */
	@RequestMapping(value = "/update",method=RequestMethod.POST)
	public String update(ServletRequest request,RedirectAttributes redirectAttributes){
		String id = request.getParameter("id");
		String gameId = request.getParameter("search_LIKE_storeId");
		String serverZoneId = request.getParameter("search_LIKE_serverZoneId");
		String serverIds = request.getParameter("search_LIKE_serverId");
		String sender = request.getParameter("sender");
		String title = request.getParameter("title");
		String contents = request.getParameter("contents");
		String[] itemId = request.getParameterValues("itemId");
		String[] itemNum = request.getParameterValues("itemNum");
		List<Annex> annexs  = new ArrayList<Annex>();
		for (int i = 0; i < itemId.length; i++) {
			Annex annex = new Annex();
			annex.setItemId(itemId[i]);
			annex.setItemNum(itemNum[i]);
			annexs.add(annex);
		}
		Email email = new Email();
		email.setId(Integer.valueOf(id));
		email.setSender(sender);
		email.setTitle(title);
		email.setContents(contents);
		email.setAnnex(annexs);

		JSONObject res = HttpClientUts.doPost(gm_url+"/fbserver/email/updateEmail" , JSONObject.fromObject(email));
		redirectAttributes.addFlashAttribute("message", "选择"+res.getString("choose")+"个，成功"+res.getString("success")+"个，失败"+res.getString("fail")+"个，失败的服务器有："+res.getString("objFail"));
		return "redirect:/manage/gm/fb/email/index";
	}

	
	/**
	 * 保存
	 * @return
	 */
	@RequestMapping(value="/save" , method=RequestMethod.POST)
	public String save(Email email,ServletRequest request,RedirectAttributes redirectAttributes,Model model){
		JSONObject res = HttpClientUts.doPost(gm_url+"/fbserver/email/addEmail" , JSONObject.fromObject(email));
		redirectAttributes.addFlashAttribute("message", "选择"+res.getString("choose")+"个，成功"+res.getString("success")+"个，失败"+res.getString("fail")+"个，失败的服务器有："+res.getString("objFail"));
		return "redirect:/manage/gm/fb/email/add";
	}
	
	/**
	 * 删除操作	 
	 * @param oid 用户id
	 * @throws Exception 
	 */
	@RequestMapping(value = "del", method = RequestMethod.DELETE)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public Map<String,Object> del(@RequestParam(value = "id")Long id) throws Exception{
		 Map<String,Object> map = new HashMap<String, Object>();
		 String	account = HttpClientUts.doGet(gm_url+"/fbserver/email/delEmailById"+"?id="+id, "utf-8");
		 JSONObject dataJson=JSONObject.fromObject(account);
		 map.put("success", dataJson.get("message"));
		 return map;
	}

	/**
	 * 获取操作	 
	 * @param oid 用户id
	 * @throws Exception 
	 */
	@RequestMapping(value = "findByEmailId", method = RequestMethod.GET)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public Email findByEmailId(@RequestParam(value = "id")Long id) throws Exception{
		 String	account = HttpClientUts.doGet(gm_url+"/fbserver/email/getEmailById"+"?id="+id, "utf-8");
		 Email email = binder.fromJson(account, Email.class);
		 return email;
	}
	
	/**
	 * 取出Shiro中的当前用户Id.
	 */
	public Long getCurrentUserId() {
		ShiroUser user = (ShiroUser) SecurityUtils.getSubject().getPrincipal();
		return user.id;
	}
	
	public ShiroUser getCurrentUser() {
		ShiroUser user = (ShiroUser) SecurityUtils.getSubject().getPrincipal();
		return user;
	}
	
	private PageRequest buildPageRequest(int pageNumber, int pagzSize, String sortType) {
		Sort sort = null;
		if ("auto".equals(sortType)) {
			sort = new Sort(Direction.DESC, "id");
		} else if ("registerDate".equals(sortType)) {
			sort = new Sort(Direction.DESC, "registerDate");
		}
		return new PageRequest(pageNumber - 1, pagzSize, sort);
	}
	
}
