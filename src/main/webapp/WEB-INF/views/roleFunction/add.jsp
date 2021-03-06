<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="tags" tagdir="/WEB-INF/tags"%>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<html>
<head>
	<title>新增游戏功能权限分配</title>
<style type="text/css"> 
.error{ 
color:Red; 
margin-left:10px;  
} 
input[type="radio"], input[type="checkbox"] {
margin: 0px 0 0;
}
</style> 
</head>

<body>
	<div class="page-header">
   		<h2>新增游戏功能权限分配</h2>
 	</div>
 	 <c:if test="${not empty message}">
		<div id="message" class="alert alert-success"><button data-dismiss="alert" class="close">×</button>${message}</div>
	</c:if>
	<form id="inputForm" method="post" Class="form-horizontal" action="${ctx}/manage/roleFunction/save">
		<div class="control-group">
			<label class="control-label" for="gameId">项目名称：</label>
			<div class="controls">
				<select id="gameId" name="gameId">	
					<option value="">请选择项目</option>
					<c:forEach items="${stores}" var="item" >
						<option value="${item.id }"  >
							${item.name }
						</option>
					</c:forEach>
				</select>	
			</div>
		</div>
		<div
			class="control-group">
			<label class="control-label" for="role">权限名称：</label>
			<div class="controls">
				<input type="text" id="role" name="role" class="input-large " value=""  placeholder="如：超级管理员、2级管理员..." />
			</div>
		</div>
		
		<div class="control-group" id="category">
			<c:forEach items="${cateAndFunctions}" var="item" varStatus="i">
				<div>
					<label class="control-label">${item.categoryName}<input type="checkbox" id="${item.id}" onclick="selectAll(${item.id});" class="box" />：</label>
					<div class="controls" id="categoryItem">
						<div>
						       <c:forEach items="${item.enumFunctions}" var="ite" varStatus="j">
									   <input type="checkbox" id="${item.id}" name="functions" value="${ite.enumRole}" class="box" />
							           	<span>${ite.enumName}</span>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
							         <c:if test="${(j.index+1)%5 == 0}">
									<br/>
									<br/>
									</c:if>
								</c:forEach>
						</div>
						</br>
						<div id="category_${item.id}">
						
						</div>			
					</div>
				</div>
				</br>
			</c:forEach>
		</div>
		
<!-- 		<div class="control-group">
				<label for="tag_status" class="control-label">状态:</label>
				<div class="controls">
					<select path="status" name="status">
						<option value="1" selected="selected">有效</option>
						<option value="0">无效</option>
					</select>
				</div>
	   </div> -->
		
	   <div class="form-actions">
  			<button type="submit" class="btn btn-primary" id="submit">保存</button>
			<a href="<%=request.getContextPath()%>/manage/roleFunction/index" class="btn btn-primary">返回</a>
	   </div>
	</form>
	<script type="text/javascript">
	
	function selectAll(id){  
	    if ($("#"+id).prop("checked")) {
	        $("input[id='"+id+"']").prop("checked", true);  
	    } else {  
	    	$("input[id='"+id+"']").prop("checked", false);  
	    }  
	}	

$(function(){
	
/* 	jQuery.validator.addMethod("rules", function(value, element) { 

		var tel = /^([0-9])*$/; 

		return this.optional(element) || (tel.test(value)); 

		}, "数字格式错误"); */

	$("#role").change(function(e){
		var gameId = $("#gameId").val();
		var role = $("#role").val();
		$.ajax({                                               
			url: '<%=request.getContextPath()%>/manage/roleFunction/checkRoleFunctionName?storeId='+gameId+'&role=' + role, 
			type: 'GET',
			contentType: "application/json;charset=UTF-8",		
			dataType: 'text',
			success: function(data){
				if(data=="false"){	
					$("#role").after("<span for='role' class='error'>权限组已存在</span>");
				}
			},error:function(xhr){alert('错误了\n\n'+xhr.responseText)}//回调看看是否有出错
		});
	});
		
	$("#gameId").change(function(e){
			var gameId = $("#gameId").val();
			$.ajax({                                               
				url: '<%=request.getContextPath()%>/manage/roleFunction/changGameId?storeId='+gameId, 
				type: 'GET',
				contentType: "application/json;charset=UTF-8",		
				dataType: 'text',
				success: function(data){	
					if(data!="" && data!=null){
		 				 var parsedJson = $.parseJSON(data); 
		 				 jQuery.each(parsedJson, function(ind, data) {
							 if(data.id=='10000'){// gm
								 $("#category_"+data.id).remove(); 
								 $("#categoryItem").append("<div id='category_"+data.id+"'></div>");
								 jQuery.each(data.enumFunctions, function(index, itemData) {
									 $("#category_"+data.id).append("<input type='checkbox' id='"+data.id+"' name='functions' value='"+itemData.enumRole+"' class='box'/> <span>"+itemData.enumName+"</span>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");			
								 	 if((index+1)%5==0){
									 	$("#category_"+data.id).append("<br/><br/>");
									 }
								 });
							 }else if(data.id=='10001'){ //礼品卡
								 	
							 }else if(data.id=='10002'){ //统计
				 				 $("#divCategory").remove(); 
				 				 $("#category").append("<div id='divCategory'><label class='control-label'>"+data.categoryName+"<input type='checkbox' id='"+data.id+"' onclick='selectAll("+data.id+");' class='box' />：</label><div class='controls' id='functions'></div></div>"); 
								 jQuery.each(data.enumFunctions, function(index, itemData) {
									 $("#functions").append("<input type='checkbox' id='"+data.id+"' name='functions' value='"+itemData.enumRole+"' class='box'/> <span>"+itemData.enumName+"</span>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");			
								 	 if((index+1)%5==0){
								 		$("#functions").append("<br/><br/>");
								 	 }
								 });
							 }
		 				 });
					}else{
		 				 $("#divCategory").remove(); 
					}
				},error:function(xhr){alert('错误了\n\n'+xhr.responseText)}//回调看看是否有出错
			});
	});
	
	$("#inputForm").validate({
		rules:{
			gameId:{
				required:true
			},
			gameName:{
				required:true
			},
			role:{
				required:true
			},
			functions:{
	     		required:true
			}
		},messages:{
			gameId:{
				required:"必须填写",
			},
			gameName:{
				required:"必须填写",
			},
			role:{
				required:"必须填写",
			},
			functions:{
				required:"至少选择一个功能",
			}
		}
	});
})


</script> 
</body>
