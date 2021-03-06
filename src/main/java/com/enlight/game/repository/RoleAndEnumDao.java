package com.enlight.game.repository;


import java.util.List;

import javax.persistence.QueryHint;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.PagingAndSortingRepository;

import com.enlight.game.entity.RoleAndEnum;

public interface RoleAndEnumDao extends PagingAndSortingRepository<RoleAndEnum, Long>,JpaSpecificationExecutor<RoleAndEnum>{
	
	@Modifying
	@Query("from RoleAndEnum roleAndEnum where roleAndEnum.roleRunctionId =?1 And roleAndEnum.status='1' order by roleAndEnum.enumRole asc ")
	@QueryHints({ @QueryHint(name = "org.hibernate.cacheable", value ="true") })  
	List<RoleAndEnum> findByRoleRunctionId(Long roleRunctionId);
	
	@Modifying
	@Query("select roleAndEnum.enumRole from RoleAndEnum roleAndEnum where roleAndEnum.roleRunctionId =?1 And roleAndEnum.status='1'")
	@QueryHints({ @QueryHint(name = "org.hibernate.cacheable", value ="true") })  
	List<String> findByRole(Long roleRunctionId);
	
	@Modifying
	@Query("delete from RoleAndEnum roleAndEnum where roleAndEnum.roleRunctionId =?1")
	void deleteByRoleRunctionId(Long roleRunctionId);
}
