package com.enlight.game.web.controller.mgr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
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

import com.enlight.game.entity.EnumCategory;
import com.enlight.game.entity.EnumFunction;
import com.enlight.game.entity.RoleFunction;
import com.enlight.game.entity.Stores;
import com.enlight.game.entity.User;
import com.enlight.game.entity.UserRole;
import com.enlight.game.service.account.ShiroDbRealm.ShiroUser;
import com.enlight.game.service.enumCategory.EnumCategoryService;
import com.enlight.game.service.enumFunction.EnumFunctionService;
import com.enlight.game.service.roleFunction.RoleFunctionService;
import com.enlight.game.service.store.StoreService;
import com.enlight.game.service.user.UserService;
import com.enlight.game.service.userRole.UserRoleService;
import com.google.common.collect.Maps;

/**
 * 游戏功能权限分配管理
 * @author dell
 *
 */
@Controller("roleFunctionController")
@RequestMapping(value="/manage/roleFunction")
public class RoleFunctionController extends BaseController{
	
	private static final String PAGE_SIZE = "10";
	
	private static final Logger logger = LoggerFactory.getLogger(RoleFunctionController.class);
	
	private static Map<String,String> sortTypes = Maps.newLinkedHashMap();
	
	static{
		sortTypes.put("auto","自动");
		sortTypes.put("id", "Id");
	}
	
	public static Map<String, String> getSortTypes() {
		return sortTypes;
	}
	
	public static void setSortTypes(Map<String, String> sortTypes) {
		RoleFunctionController.sortTypes = sortTypes;
	}
	
	@Autowired
	private RoleFunctionService roleFunctionService;
	
	@Autowired
	private StoreService storeService;
	
	@Autowired
	private EnumFunctionService enumFunctionService;
	
	@Autowired
	private UserRoleService userRoleService;
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private EnumCategoryService enumCategoryService;
	
	/**
	 * 游戏功能权限分配管理首页
	 */
	@RequestMapping(value = "index",method=RequestMethod.GET)
	public String index(@RequestParam(value = "page", defaultValue = "1") int pageNumber,
			@RequestParam(value = "page.size", defaultValue = PAGE_SIZE) int pageSize,
			@RequestParam(value = "sortType", defaultValue = "auto")String sortType, Model model,
			ServletRequest request){
		Long userId = getCurrentUserId();
		logger.info("userId"+userId+"游戏功能权限分配管理首页");
		Map<String, Object> searchParams = Servlets.getParametersStartingWith(request, "search_");
		Page<RoleFunction> roleFunctions = roleFunctionService.findRoleFunctionByCondition(userId,searchParams, pageNumber, pageSize, sortType);
		for (RoleFunction roleFunction : roleFunctions) {
		  	EnumFunction enumFunction =  enumFunctionService.findByEnumRole(roleFunction.getFunction());
		  	if(enumFunction!=null){
		  		roleFunction.setFunctionName(enumFunction.getEnumName());
		  	}
		}
/*		Set<EnumCategory> enumCategories = new HashSet<EnumCategory>();
		for (RoleFunction roleFunction : roleFunctions) {
			EnumFunction enumFunction  = enumFunctionService.findByEnumRole(roleFunction.getFunction());
			enumFunction.getCategoryId();
			EnumCategory enumCategory = enumCategoryService.find((long)enumFunction.getCategoryId());
			enumCategories.add(enumCategory);
		}*/
		
		model.addAttribute("roleFunctions", roleFunctions);
		model.addAttribute("sortType", sortType);
		model.addAttribute("sortTypes", sortTypes);
		List<Stores> stores = storeService.findList();
		model.addAttribute("stores", stores);
		// 将搜索条件编码成字符串，用于排序，分页的URL
		model.addAttribute("searchParams", Servlets.encodeParameterStringWithPrefix(searchParams, "search_"));
		return "/roleFunction/index";
	}
	
	/**
	 * 新增权限页面
	 */
	@RequestMapping(value = "/add" ,method=RequestMethod.GET)
	public String addGameRole(Model model){
		Long uid = getCurrentUserId();
		List<Stores> stores = storeService.findList();
		//List<EnumFunction> enumFunctions = enumFunctionService.findAll();
		List<EnumCategory> cateAndFunctions = enumCategoryService.findAll();
		model.addAttribute("stores", stores);
		model.addAttribute("cateAndFunctions", cateAndFunctions);
		return "/roleFunction/add";
	}
	
	/**
	 * 新增权限
	 */
	@RequestMapping(value = "/save",method=RequestMethod.POST)
	public String saveGameRole(RoleFunction roleFunction,ServletRequest request,RedirectAttributes redirectAttributes,Model model){
		logger.debug("save function 功能号..." + roleFunction.getFunction());
		logger.debug("save role 权限号..." + roleFunction.getRole());
		logger.debug("save gameid 项目号..." + roleFunction.getGameId());
		
		String[] functions = request.getParameterValues("functions");
		boolean flag = roleFunctionService.isOnly(roleFunction.getGameId(), roleFunction.getRole());
		if(flag){
			for (int i = 0; i < functions.length; i++) {
				logger.debug("权限号：function..." + roleFunction.getFunction());
				roleFunction.setFunction(Integer.valueOf(functions[i]));
	        	roleFunction.setStatus(RoleFunction.STATUS_VALIDE);
				roleFunctionService.save(roleFunction);
			}
			redirectAttributes.addFlashAttribute("message", "新增权限成功");
			return "redirect:/manage/roleFunction/index?search_EQ_gameId="+roleFunction.getGameId();
		}else{
			List<Stores> stores = storeService.findList();
			List<EnumFunction> enumFunctions = enumFunctionService.findAll();
			model.addAttribute("stores", stores);
			model.addAttribute("enumFunctions", enumFunctions);
			model.addAttribute("message", "权限组名称重复！");
			return "/roleFunction/add";
		}
	}
	
	/**
	 * 删除权限对应功能
	 * @param id
	 * @param model
	 * @return
	 */
	@RequestMapping(value="del",method=RequestMethod.DELETE)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public Map<String,Object> del(@RequestParam(value="id")long id,Model model){
		RoleFunction function = roleFunctionService.findById(id);
		roleFunctionService.delById(id);
		List<String> functions = roleFunctionService.findByGameIdAndRoleFunctions(function.getGameId(), function.getRole());
		if(functions!=null && functions.size()!=0){
			List<UserRole> userRoles = userRoleService.findByStoreIdAndRole(function.getGameId(), function.getRole());
			for (UserRole userRole : userRoles) {
				userRole.setFunctions(StringUtils.join(functions,","));
				userRoleService.save(userRole);
			}
		}else{
			List<UserRole> userRoles = userRoleService.findByStoreIdAndRole(function.getGameId(), function.getRole());
			for (UserRole userRole : userRoles) {
				User user =userService.findById(userRole.getUserId());
				List<String> stIds = user.getStoreIds();
				List<String> storeIds = new ArrayList<String>(stIds);
				for (String s : stIds) {
					if(Long.parseLong(s) == function.getGameId()){
						storeIds.remove(s);
					}
				}
				user.setStoreId(StringUtils.join(storeIds,","));
				userService.update(user);
			}
			userRoleService.delByStoreIdAndRole(function.getGameId(), function.getRole());
		}


		Map<String,Object> map = new HashMap<String, Object>();
		map.put("success", "true");
		return map;
	}
	
	/**
	 * 修改页面
	 * @param id
	 * @param model
	 * @return
	 */
	@RequestMapping(value="edit",method=RequestMethod.GET)
	public String edit(@RequestParam(value="id")long id,Model model){
		RoleFunction roleFunction = roleFunctionService.findById(id);
		
		List<RoleFunction> roleFunctions = roleFunctionService.findByGameIdAndRole(roleFunction.getGameId(), roleFunction.getRole());
		List<EnumFunction> enumFunctions = enumFunctionService.findAll();
		
		List<EnumFunction> enumFusNohas = enumFunctionService.findAll();
		List<EnumFunction> enumFusHas = new ArrayList<EnumFunction>();
		for (int i = enumFunctions.size() - 1; i >= 0; i--) {
			for (int j = roleFunctions.size() - 1; j >= 0; j--) {
					if(enumFunctions.get(i).getEnumRole().equals(roleFunctions.get(j).getFunction())){
						enumFusHas.add(enumFunctions.get(i));
						enumFusNohas.remove(enumFunctions.get(i));
					}
			}
		}
		
		List<EnumCategory> cateAndFunctions = enumCategoryService.findAll();
		
		model.addAttribute("cateAndFunctions", cateAndFunctions);
		model.addAttribute("stores", storeService.findList());
		model.addAttribute("enumFusHas", enumFusHas);
		model.addAttribute("roleFunction", roleFunction);
		return "/roleFunction/edit";
	}
	
	/**
	 * 修改权限
	 */
	@RequestMapping(value = "/update",method=RequestMethod.POST)
	public String updateFunction(RoleFunction roleFunction,ServletRequest request,RedirectAttributes redirectAttributes){
		List<String> functions = Arrays.asList(request.getParameterValues("functions"));
		List<RoleFunction> roleFunctions = roleFunctionService.findByGameIdAndRole(roleFunction.getGameId(), roleFunction.getRole());
		//新增的功能
		List<String> funs =  new ArrayList<String>();
		Collections.addAll(funs, request.getParameterValues("functions"));
		//删除的功能
        List<RoleFunction> roleFuncs = roleFunctionService.findByGameIdAndRole(roleFunction.getGameId(), roleFunction.getRole());
		for (RoleFunction role : roleFunctions) {
			for (String str : functions) {
				if(role.getFunction().toString().equals(str)){
					funs.remove(str);
					roleFuncs.remove(role);
				}
			}
		}      
        for (RoleFunction roleFunctionDel : roleFuncs) {
        	roleFunctionService.delById(roleFunctionDel.getId());
    			List<UserRole> userRoles = userRoleService.findByStoreIdAndRole(roleFunctionDel.getGameId(), roleFunctionDel.getRole());
    			for (UserRole userRole : userRoles) {
    				List<String> functs =  userRole.getRoleList();
    				List<String> f =  new ArrayList<String>();
    				for (String str: functs) {
    					f.add(str);
						if(roleFunctionDel.getFunction().toString().equals(str)){
							f.remove(str);
						}
					}
    				userRole.setFunctions(StringUtils.join(f,","));
    				userRoleService.save(userRole);
    			}
		}
        for (String roleFunctionAdd : funs) {
        	roleFunction.setFunction(Integer.valueOf(roleFunctionAdd));
        	roleFunction.setStatus(RoleFunction.STATUS_VALIDE);
        	roleFunctionService.save(roleFunction);
		}
		redirectAttributes.addFlashAttribute("message", "更新权限成功");
		return "redirect:/manage/roleFunction/index?search_EQ_gameId="+roleFunction.getGameId();
	}
	
	/**
	 * 权限详细
	 * @param id 用户id
	 */
	@RequestMapping(value = "detail", method = RequestMethod.GET)
	public String show(@RequestParam(value = "id")long id,Model model){
		RoleFunction roleFunction = roleFunctionService.findById(id);
		List<RoleFunction> roleFuncs = roleFunctionService.findByGameIdAndRole(roleFunction.getGameId(), roleFunction.getRole());
		model.addAttribute("roleFunction", roleFunction);
		model.addAttribute("roleFuncs", roleFuncs);
		return "/roleFunction/info";
	}
	
	@RequestMapping(value = "/checkRoleFunctionName")
	@ResponseBody
	public String checkRoleFunctionName(@RequestParam("role") String role,@RequestParam("storeId") String storeId) {
		if (roleFunctionService.isOnly(Long.parseLong(storeId), role)) {
			return "true";
		} else {
			return "false";
		}
	}
	
	/**
	 * 取出Shiro中的当前用户Id.
	 */
	public Long getCurrentUserId() {
		ShiroUser user = (ShiroUser) SecurityUtils.getSubject().getPrincipal();
		return user.id;
	}
}
