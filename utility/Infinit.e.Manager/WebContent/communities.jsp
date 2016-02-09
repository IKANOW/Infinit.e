<!--
Copyright 2012 The Infinit.e Open Source Project

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1" session="false" %>
<%@ include file="inc/sharedFunctions.jsp" %>

<%!
	// 
	static class Session {
		int currentPage = 1;
		int itemsToShowPerPage = 10;
		String action = "";
		String lastaction = "";
		String logoutAction = "";
		String listFilter = "";
	
		//
		String editTableTitle = "Add New Community";
		String communityid = "";
		String communityType = "";
		String visibleCommunityId = "";
		String name = "";
		String description = "";
		String parentId = "";
		String tags = "";
		String isSystemCommunity = "";
		String isPersonalCommunity = "";
		String ownerId = "";
		String communityStatus = "";
		String ownerDisplayName = "";
		String ownerEmail = "";
		String numberOfMembers = "";
		String usersCanCreateSubCommunities = "";
		String usersCanCreateSubCommunitiesChecked = "";
		String registrationRequiresApproval = "";
		String registrationRequiresApprovalChecked = "";
		String publishMemberOverride = "";
		String publishMemberOverrideChecked = "";
		String isPublic = "";
		String isPublicChecked = "";
		String usersCanSelfRegister = "";
		String usersCanSelfRegisterChecked = "";
		String isCommAttrDisabled = "disabled=\"true\"";
		String commAttrEditable = "(editable after creation)";
		
		String userAttributesAreReadonly = "disabled=\"disabled\"";
		String parentIdIsReadonly="";
		
		String editMembersLink = "";
		String addMembersLink = "";
	
		String communityTypeDropdown = "<select id='communityType' name='communityType'><option selected='true'>Community</option><option>Data Group (Testing Only)</option><option>User Group (Testing Only)</option></select>";
		
		String messageToDisplay = "";
	}
%>

<%

	final Session session = new Session();
	session.messageToDisplay = "";
	
	// 
	if (isLoggedIn) 
	{	
		// Capture value in the left handed table filter field
		if (request.getParameter("session.listFilter") != null) 
		{
			session.listFilter = request.getParameter("session.listFilter");
		}
		else if (request.getParameter("listFilterStr") != null) 
		{
			session.listFilter = request.getParameter("listFilterStr");
		}
		else
		{
			session.listFilter = "";
		}
		
		// Determine which session.action to perform on postback/request
		session.action = "";
		if (request.getParameter("session.action") != null) session.action = request.getParameter("session.action").toLowerCase();
		session.lastaction = session.action; // (preserve this - normally edit)
		if (request.getParameter("dispatchAction") != null) session.action = request.getParameter("dispatchAction").toLowerCase();
		if (request.getParameter("clearForm") != null) session.action = request.getParameter("clearForm").toLowerCase();
		if (request.getParameter("filterList") != null) session.action = request.getParameter("filterList").toLowerCase();
		if (request.getParameter("clearFilter") != null) session.action = request.getParameter("clearFilter").toLowerCase();
		if (request.getParameter("logoutButton") != null) session.action = request.getParameter("logoutButton").toLowerCase();
		if (request.getParameter("deleteSelected") != null) session.action = request.getParameter("deleteSelected").toLowerCase();
		
		// Capture values sent by button clicks, these will override the session.action value as appropriate 
		String save = "";
		String create = "";
		if (request.getParameter("create") != null) create = request.getParameter("create").toLowerCase();
		if (request.getParameter("save") != null) save = request.getParameter("save").toLowerCase();
		
		// Capture input for page value if passed to handle the page selected in the left hand list of items
		if (request.getParameter("page") != null) 
		{
			session.currentPage = Integer.parseInt( request.getParameter("page").toLowerCase() );
		}
		else
		{
			session.currentPage = 1;
		}
		
		try
		{
			// Always clear the form first so there is no bleed over of values from previous requests
			clearForm(session);

			// Read in values from the edit form
			session.communityid = (request.getParameter("communityid") != null) ? request.getParameter("communityid") : "";
			session.name = (request.getParameter("name") != null) ? request.getParameter("name") : "";
			session.parentId = (request.getParameter("parentId") != null) ? request.getParameter("parentId") : "";
			session.description = (request.getParameter("description") != null) ? request.getParameter("description") : "";
			session.tags = (request.getParameter("tags") != null) ? request.getParameter("tags") : "";
			session.isPublic = (request.getParameter("isPublic") != null) ? request.getParameter("isPublic") : "";
			session.usersCanCreateSubCommunities = (request.getParameter("usersCanCreateSubCommunities") != null) ? request.getParameter("usersCanCreateSubCommunities") : "";
			session.registrationRequiresApproval = (request.getParameter("registrationRequiresApproval") != null) ? request.getParameter("registrationRequiresApproval") : "";
			session.publishMemberOverride = (request.getParameter("publishMemberOverride") != null) ? request.getParameter("publishMemberOverride") : "";
			session.usersCanSelfRegister = (request.getParameter("usersCanSelfRegister") != null) ? request.getParameter("usersCanSelfRegister") : "";
			session.communityType = (request.getParameter("communityType") != null) ? request.getParameter("communityType") : "";

			Boolean redirect = false;
			
			// If user has clicked save, create, or update buttons do those actions before handling the session.action param
			if (save.equals("save")) 
			{
				if ( validateFormFields(session) )
				{
					saveCommunity(false, request, response, session);
				}
			}
			if (create.equals("create")) 
			{
				if ( validateFormFields(session) )
				{
					saveCommunity(true, request, response, session);
					redirect = true;
				}
			}
			
			// if redirect == true refresh the page to update the edit form's content to reflect changes
			if (redirect) 
			{
				String urlParams = "";
				if (session.listFilter.length() > 0) urlParams = "&listFilterStr="+ session.listFilter;
				if (session.currentPage > 1) urlParams += "&page=" + session.currentPage;
				out.println("<meta http-equiv=\"refresh\" content=\"0;url=communities.jsp?session.action=edit&communityid=" 
					+ session.communityid + urlParams + "\">");
			}
			
			if (session.action.equals("clearform")) 
			{
				out.println("<meta http-equiv=\"refresh\" content=\"0;url=communities.jsp\">");
			}
			else if (session.action.equals("edit")) 
			{
				if (session.communityid.length() > 0) {
					session.parentIdIsReadonly="readonly";
				}
				populateEditForm(session.communityid, request, response, session);
			}
			else if (session.action.equals("delete")) 
			{
				deleteCommunity(session.communityid, request, response, session);
				out.print("<meta http-equiv=\"refresh\" content=\"0;url=communities.jsp?page=" + session.currentPage);
				if (session.listFilter.length() > 0) {
					out.print("&listFilterStr=" + session.listFilter);					
				}
				out.println("\">");
			}
			else if (session.action.equals("deleteselected")) 
			{
				String[] ids= request.getParameterValues("docsToDelete");
				
				int nDeleted = 0;
				int nFailed = 0;
				for (String id: ids) {
					if (!deleteCommunity(id, request, response, session)) {
						nFailed++;
					}
					else nDeleted++;
				}
				session.messageToDisplay = "Bulk community deletion: deleted " + nDeleted + ", failed: " + nFailed; 
				
				out.print("<meta http-equiv=\"refresh\" content=\"0;url=communities.jsp?page=" + session.currentPage);
				if (session.listFilter.length() > 0) {
					out.print("&listFilterStr=" + session.listFilter);					
				}
				out.println("\">");
			}
			else if (session.action.equals("filterlist")) 
			{
				session.currentPage = 1; // (don't perpetuate this session.action across page jumps)
				populateEditForm(session.communityid, request, response, session);
			}
			else if (session.action.equals("clearfilter")) 
			{
				session.currentPage = 1; // (don't perpetuate this session.action across page jumps)
				session.listFilter = "";
				populateEditForm(session.communityid, request, response, session);
			}
			else if (session.action.equals("logout")) 
			{
				logOut(request, response);
				out.println("<meta http-equiv=\"refresh\" content=\"0;url=index.jsp\">");
			}
		}
		catch (Exception e)
		{
			//System.out.println(e.getMessage());
		}
	}
	
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
	<link rel="stylesheet" type="text/css" href="inc/manager.css" />
	<script type="text/javascript" src="inc/utilities.js"></script>
	<link rel="shortcut icon" href="image/favicon.ico" />
	<title>Infinit.e.Manager - Communities</title>
	
</head>
<body>

<%
	// !-- Create JavaScript Popup --
	if (session.messageToDisplay.length() > 0) { 
%>
	<script language="javascript" type="text/javascript">
		alert("<%=session.messageToDisplay %>");
	</script>
<% 
	} 
%>

	<form method="post">
	
<%@ include file="inc/header.jsp.inc" %>

<%
	if (!isLoggedIn) 
	{
%>
		<%@ include file="inc/login_form.jsp" %>
<%
	}
	else
	{
%>
	
	<table class="standardTable" cellpadding="5" cellspacing="0" width="100%">
	<tr valign="top">
		<td width="30%" bgcolor="#ffffff">
		
			<table class="standardTable" cellpadding="5" cellspacing="1" width="100%">
			<tr>
				<td class="headerLink">Communities</td>
				<td align="right">
					<input type="text" id="session.listFilter" 
					onkeydown="if (event.keyCode == 13) { setDipatchAction('filterList'); 
					document.getElementById('filterList').click(); }" 
					name="session.listFilter" size="20" value="<%=session.listFilter %>"/><button name="filterList" 
					value="filterList">Filter</button>
					<button name="clearFilter" value="clearFilter">Clear</button>
					</td>
			</tr>
			<tr>
				<td colspan="2" bgcolor="white"><%=listItems(request, response, session) %></td>
			</tr>
			<tr>
				<td colspan="2" >
				<button name="deleteSelected" onclick="return confirm('Do you really wish to delete the selected communities?');" name="deleteSelected" value="deleteSelected">Delete selected communities</button>
				<input type="checkbox" name="selectall" onchange="var cbs = document.getElementsByName('docsToDelete'); for(var i=0; i < cbs.length; i++) if(cbs[i].type == 'checkbox') cbs[i].checked=selectall.checked" value=""></input>
				</td>
			</tr>
			</table>
		</td>
		
		<td width="70%" bgcolor="#ffffff">
		
			<table class="standardTable" cellpadding="5" cellspacing="1" width="100%">
			<tr>
				<td class="headerLink"><%=session.editTableTitle %></td>
				<td align="right"><button name="clearForm" value="clearForm">New Community</button></td>
			</tr>
			<tr>
				<td colspan="2" bgcolor="white">
					<table class="standardSubTable" cellpadding="5" cellspacing="1" width="100%">
					<tr>
						<td bgcolor="#ffffff" width="30%">Status:</td>
						<td bgcolor="#ffffff" width="70%"><%=session.communityStatus %></td>
					</tr>
					<tr>
						<td bgcolor="#ffffff" width="30%">Community Id:</td>
						<td bgcolor="#ffffff" width="70%"><input type="text" readonly id="communityid" name="communityid" value="<%=session.visibleCommunityId %>" size="50" /></td>
					</tr>
					<tr>
						<td bgcolor="#ffffff" width="30%">Name:*</td>
						<td bgcolor="#ffffff" width="70%"><input type="text" id="name" name="name" value="<%=session.name%>" size="50" /></td>
					</tr>
					<tr valign="top">
						<td bgcolor="#ffffff" width="30%">Description:*</td>
						<td bgcolor="#ffffff" width="70%">
							<textarea cols="60" rows="5" id="description" name="description"><%=session.description %></textarea>
						</td>
					</tr>
					<tr valign="top">
						<td bgcolor="#ffffff" width="30%">Parent Id (optional):</td>
						<td bgcolor="#ffffff" width="70%"><input type="text" <%=session.parentIdIsReadonly%> id="parentId" name="parentId" value="<%=session.parentId%>" size="50" /></td>
					</tr>
					<tr valign="top">
						<td bgcolor="#ffffff" width="30%">Tags:*</td>
						<td bgcolor="#ffffff" width="70%">
							<textarea cols="60" rows="3" id="tags" name="tags"><%=session.tags %></textarea>
						</td>
					</tr>
					<tr>
						<td bgcolor="#ffffff" width="30%">Owner:</td>
						<td bgcolor="#ffffff" width="70%"><%=session.ownerDisplayName %> - <%=session.ownerEmail %></td>
					</tr>
					<tr valign="top" >
						<td bgcolor="#ffffff" width="30%">Community Attributes <%=session.commAttrEditable %>:</td>
						<td bgcolor="#ffffff" width="70%">
							<table cellpadding="5" cellspacing="1" width="100%">
								<tr>
									<td><input type="checkbox" name="isPublic" <%=session.isPublicChecked %> <%=session.isCommAttrDisabled %>
										 /> Is Public</td>
								</tr>
								<tr>
									<td><input type="checkbox" name="usersCanSelfRegister" <%=session.usersCanSelfRegisterChecked %> <%=session.isCommAttrDisabled %>
										 /> Users Can Self Register</td>
								</tr>
								<tr>
									<td><input type="checkbox" name="registrationRequiresApproval" <%=session.registrationRequiresApprovalChecked %> <%=session.isCommAttrDisabled %>
										 /> Registration Requires Approval</td>
								</tr>
								<tr>
									<td><input type="checkbox" name="usersCanCreateSubCommunities" <%=session.usersCanCreateSubCommunitiesChecked %> <%=session.isCommAttrDisabled %>
										 /> Users Can Create Subcommunites</td>
								</tr>
								<tr>
									<td><input type="checkbox" name="publishMemberOverride" <%=session.publishMemberOverrideChecked %> <%=session.isCommAttrDisabled %>
										 /> Members are visible to other members</td>
								</tr>
							</table>
						</td>
					</tr>
					<!-- <tr valign="top">
						<td bgcolor="#ffffff" width="30%" class="disabledText">User Attributes:</td>
						<td bgcolor="#ffffff" width="70%">
							<table cellpadding="5" cellspacing="1" width="100%" class="disabledText">
								<tr>
									<td><input type="checkbox" name="publishQueriesToActivityFeed" <%=session.userAttributesAreReadonly %> /> User Queries are Published to Activity Feed</td>
								</tr>
								<tr>
									<td><input type="checkbox" name="publishLoginToActivityFeed" <%=session.userAttributesAreReadonly %> /> User Login Published to Activity Feed</td>
								</tr>
								<tr>
									<td><input type="checkbox" name="publishSharingToActivityFeed" <%=session.userAttributesAreReadonly %> /> User Shares Published to Activity Feed</td>
								</tr>
								<tr>
									<td><input type="checkbox" name="publishCommentsToActivityFeed" <%=session.userAttributesAreReadonly %> /> User Comments Published to Activity Feed</td>
								</tr>
								<tr>
									<td><input type="checkbox" name="publishCommentsPublicly" <%=session.userAttributesAreReadonly %> /> User Comments are Public</td>
								</tr>
							</table>
						</td>
					</tr>  -->
					<tr>
						<td bgcolor="#ffffff" width="30%">Number of Members:</td>
						<td bgcolor="#ffffff" width="70%"><%=session.numberOfMembers %></td>
					</tr>
					<tr>
						<td bgcolor="#ffffff" width="30%">Members:</td>
						<td bgcolor="#ffffff" width="70%"><%=session.editMembersLink %> <br/> <%=session.addMembersLink %></td>
					</tr>
					<tr>
						<td bgcolor="#ffffff" width="30%"></td>
						<td bgcolor="#ffffff" width="70%">
<%
	if (session.communityid.length() > 0) {
%>
							<button name="save" value="save">Save Community</button>
<%
	}
	else
	{
%>
							<button name="create" value="create">Create Community</button>	
<%
	}
%>
						</td>
					</tr>
					</table>
					
				</td>
			</tr>
			</table>
		
		</td>
		
	<tr>
	</table>
	</form>
<%
	}
%>

<%@ include file="inc/footer.jsp" %>
</body>
</html>




<%!

// validateFormFields
private boolean validateFormFields(Session session)
{
	boolean isValid = true;
	ArrayList<String> al = new ArrayList<String>();
	if (session.name.length() < 1) al.add("Name");
	if (session.description.length() < 1) al.add("Description");
	if (session.tags.length() < 1) al.add("Tags");
	if (al.size() > 0)
	{
		isValid = false;
		session.messageToDisplay = "Error, the following required fields are missing: " + al.toString();
	}
	return isValid;
}  // TESTED



// saveCommunity -
private boolean saveCommunity( boolean isNewCommunity, HttpServletRequest request, HttpServletResponse response, Session session )
{
	try
	{
		JSONObject actionResponse = null;
		JSONObject responseVal = null;
		
		String json = "";
		
		// Existing community, get the community object from the API, update the fields that have changed
		// and send back to the server via the update API
		if (!isNewCommunity)
		{
			JSONObject communityResponse = new JSONObject( getCommunity(session.communityid, request, response) );
			JSONObject community = communityResponse.getJSONObject("data");
			
			// Update the community object values in the form
			community.remove("name");
			community.put("name", session.name);
			community.remove("description");
			community.put("description", session.description);
			
			// Tags
			String[] formTags = session.tags.split(",");
			community.remove("tags");
			List<String> newTags = new ArrayList<String>();
			for (String s : formTags)
			{
				newTags.add(s.trim());
			}
			community.put("tags", newTags);
			
			// Community properties
			String valueStr = "";
			JSONObject communityProperties = new JSONObject();
			// isPublic
			valueStr = (session.isPublic.equalsIgnoreCase("on")) ? "true" : "false";
			JSONObject isPublicObject = new JSONObject();
			isPublicObject.put("type", "Boolean");
			isPublicObject.put("value", valueStr);
			communityProperties.put("isPublic", isPublicObject);
			// registrationRequiresApproval
			valueStr = (session.registrationRequiresApproval.equalsIgnoreCase("on")) ? "true" : "false";
			JSONObject registrationRequiresApprovalObject = new JSONObject();
			registrationRequiresApprovalObject.put("type", "Boolean");
			registrationRequiresApprovalObject.put("value", valueStr);
			communityProperties.put("registrationRequiresApproval", registrationRequiresApprovalObject);
			// usersCanSelfRegister
			valueStr = (session.usersCanSelfRegister.equalsIgnoreCase("on")) ? "true" : "false";
			JSONObject usersCanSelfRegisterObject = new JSONObject();
			usersCanSelfRegisterObject.put("type", "Boolean");
			usersCanSelfRegisterObject.put("value", valueStr);
			communityProperties.put("usersCanSelfRegister", usersCanSelfRegisterObject);
			// usersCanCreateSubCommunities
			valueStr = (session.usersCanCreateSubCommunities.equalsIgnoreCase("on")) ? "true" : "false";
			JSONObject usersCanCreateSubCommunitiesObject = new JSONObject();
			usersCanCreateSubCommunitiesObject.put("type", "Boolean");
			usersCanCreateSubCommunitiesObject.put("value", valueStr);
			communityProperties.put("usersCanCreateSubCommunities", usersCanCreateSubCommunitiesObject);
			// publishMemberOverride
			valueStr = (session.publishMemberOverride.equalsIgnoreCase("on")) ? "true" : "false";
			JSONObject publishMemberOverrideObject = new JSONObject();
			publishMemberOverrideObject.put("type", "Boolean");
			publishMemberOverrideObject.put("value", valueStr);
			communityProperties.put("publishMemberOverride", publishMemberOverrideObject);
			//replace attributes
			community.remove("communityAttributes");
			community.put("communityAttributes", communityProperties);
			
			//System.out.println(community.toString(4));
			
			actionResponse = new JSONObject(postToRestfulApi("social/community/update/" + session.communityid, 
					community.toString(), request, response));
			
			responseVal = new JSONObject(actionResponse.getString("response"));
			if (responseVal.getString("success").equalsIgnoreCase("true"))
			{
				session.messageToDisplay = "Success: Community updated.";
			}
			else
			{
				session.messageToDisplay = "Error: Unable to update the community: " + responseVal.getString("message");
				return false;
			}
		}
		// Create new community
		else
		{
			String nameStr = URLEncoder.encode(session.name, "UTF-8");
			String descriptionStr = URLEncoder.encode(session.description, "UTF-8");
			String tagsStr = URLEncoder.encode(trimTagString(session.tags), "UTF-8");
			String parentIdStr = null;
			String prefix = "social/community";
			if (session.communityType.contains("User Group")) {
				prefix = "social/group/user";
			}
			else if (session.communityType.contains("Data Group")) {
				prefix = "social/group/data";				
			}
			
			if ((null != session.parentId) && (session.parentId.length() > 0)) {
				parentIdStr = URLEncoder.encode(session.parentId, "UTF-8");
				actionResponse = new JSONObject(callRestfulApi(prefix + "/add/" + nameStr + 
						"/$desc/" + tagsStr + "/" + parentIdStr + "?desc=" + descriptionStr, request, response));
			}//TESTED - valid-works, invalid id, valid-but-not-allowed, valid-but-pending-delete, personal-community
			else {
				actionResponse = new JSONObject(callRestfulApi(prefix + "/add/" + nameStr + 
						"/$desc/" + tagsStr + "?desc=" + descriptionStr, request, response));
			}
			
			responseVal = new JSONObject(actionResponse.getString("response"));
			if (responseVal.getString("success").equalsIgnoreCase("true"))
			{
				JSONObject dataVal = new JSONObject(actionResponse.getString("data"));
				session.communityid = dataVal.getString("_id");
				session.messageToDisplay = "Success: Community added."; return true;
			}
			else
			{
				session.messageToDisplay = "Error: Unable to add the community: " + responseVal.getString("message");
				return false;
			}
		}

		return true;
	}
	catch (Exception e)
	{
		session.messageToDisplay = "Error: Unable to save the community. (" + e.getMessage() + ")"; return false;
	}
} // TESTED

private String trimTagString(String tagStr)
{
	String[] tags = tagStr.split(",");
	StringBuilder sb = new StringBuilder();
	
	for (int i = 0; i < tags.length; i++)
	{
		String s = tags[i];
		if ( i > 0 )
			sb.append(",");
		sb.append(s.trim());				
	}
	return sb.toString();
}


//deleteAccount -
private boolean deleteCommunity( String id, HttpServletRequest request, HttpServletResponse response, Session session )
{
	try
	{
		JSONObject updateResponse = new JSONObject ( new JSONObject ( removeCommunity(id, request, response) ).getString("response") );
		if (updateResponse.getString("success").equalsIgnoreCase("true"))
		{
			session.messageToDisplay = "Success: Community Deleted."; return true;
		}
		else
		{
			session.messageToDisplay = "Error: Unable to Delete Community: " + updateResponse.getString("message"); 
			return false;
		}
	}
	catch (Exception e)
	{
		session.messageToDisplay = "Error: Unable to Delete Community. (" + e.getMessage() + ")"; return false;
	}
}  // TESTED



// populateEditForm - 
private void populateEditForm(String id, HttpServletRequest request, HttpServletResponse response, Session session) 
{
	if (id != null && id != "") 
	{
		try 
		{
			session.editTableTitle = "Edit Community";
			session.isCommAttrDisabled = "";
			session.commAttrEditable = "";
			// Get person from API
			JSONObject communityResponse = new JSONObject( getCommunity(id, request, response) );
			JSONObject community = communityResponse.getJSONObject("data");
			String status = community.getString("communityStatus").substring(0,1).toUpperCase() + community.getString("communityStatus").substring(1);
			session.communityStatus =  status;
			session.visibleCommunityId = id;
			session.name = community.getString("name");
			if (community.has("parentId")) {
				session.parentId = community.getString("parentId");
			}
			else {
				session.parentId = "";
			}
			session.description = community.getString("description");
			if (community.has("members")) {
				JSONArray members = community.getJSONArray("members");
				session.numberOfMembers = Integer.toString(members.length());
			}
			else {
				session.numberOfMembers = "0";
			}
			
			session.ownerDisplayName = community.getString("ownerDisplayName");
			
			// Community tags
			if (community.has("tags"))
			{
				String listOfTags = community.getString("tags").replace("[", "");
				listOfTags = listOfTags.replace("]", "");
				listOfTags = listOfTags.replace("\"", "");
				session.tags = listOfTags;
			}
			
			// Community Attributes Check Boxes
			JSONObject communityAttributes = community.getJSONObject("communityAttributes");
			JSONObject isPublic = communityAttributes.getJSONObject("isPublic");
			session.isPublicChecked = (isPublic.getString("value").equalsIgnoreCase("true")) ? "checked=\"checked\"" : "";
			JSONObject usersCanCreateSubCommunities = communityAttributes.getJSONObject("usersCanCreateSubCommunities");
			session.usersCanCreateSubCommunitiesChecked = (usersCanCreateSubCommunities.getString("value").equalsIgnoreCase("true")) ? "checked=\"checked\"" : "";
			JSONObject registrationRequiresApproval = communityAttributes.getJSONObject("registrationRequiresApproval");
			session.registrationRequiresApprovalChecked = (registrationRequiresApproval.getString("value").equalsIgnoreCase("true")) ? "checked=\"checked\"" : "";
			JSONObject usersCanSelfRegister = communityAttributes.getJSONObject("usersCanSelfRegister");
			session.usersCanSelfRegisterChecked = (usersCanSelfRegister.getString("value").equalsIgnoreCase("false")) ? "" : "checked=\"checked\"";
			if ( communityAttributes.has("publishMemberOverride"))
			{
				JSONObject publishMemberOverride = communityAttributes.getJSONObject("publishMemberOverride");
				session.publishMemberOverrideChecked = (publishMemberOverride.getString("value").equalsIgnoreCase("true")) ? "checked=\"checked\"" : "";
			}
			
			// Get an array of members
			JSONArray members = community.getJSONArray("members");
			for (int i = 0; i < members.length(); i++)
			{
				JSONObject member = members.getJSONObject(i);
				if (member.getString("userType").equalsIgnoreCase("owner"))
				{
					session.ownerEmail = member.getString("email");
				}
			}

			session.editMembersLink = "<a href='members.jsp?communityid=" + session.communityid + "' title='Edit Community Members'>Edit Community Members</a>";
			session.addMembersLink = "<a href='addmembers.jsp?communityid=" + session.communityid + "' title='Add Community Members'>Add Community Members</a>";
		} 
		catch (Exception e) 
		{
			System.out.println(e.getMessage());
		}
	}
}  // TESTED




// clearForm
private void clearForm(Session session)
{
	session.editTableTitle = "Add New Community";
	session.parentIdIsReadonly="";
	session.communityStatus = session.communityTypeDropdown;
	session.visibleCommunityId = "";
	session.name =  "";
	session.parentId = "";
	session.description = "";
	session.tags = "";
	session.numberOfMembers = "";
	session.isPublicChecked = "";
	session.usersCanCreateSubCommunitiesChecked = "";
	session.registrationRequiresApprovalChecked = "checked=\"checked\"";
	session.usersCanSelfRegisterChecked = "checked=\"checked\"";
	session.publishMemberOverrideChecked = "checked=\"checked\"";
	session.ownerDisplayName = "";
	session.ownerEmail = "";
	session.editMembersLink = "";
	session.addMembersLink = "";
	session.isCommAttrDisabled = "disabled=\"true\"";
	session.commAttrEditable = "(editable after creation)";
}  // TESTED



// listItems -
private String listItems(HttpServletRequest request, HttpServletResponse response, Session session)
{
	StringBuffer communities = new StringBuffer();
	TreeMultimap<String, String> listOfCommunities = getListOfAllNonPersonalCommunities(request, response);
	
	if (listOfCommunities.size() > 0)
	{
		communities.append("<table id=\"listTable\" class=\"listTable\" cellpadding=\"3\" cellspacing=\"1\" width=\"100%\" >");

		// Sort the sources alphabetically
		SortedSet<String> sortedKeys = listOfCommunities.keySet();
		
		// Filter the list
		List<String> sortedAndFilteredKeys = new ArrayList<String>();
		for (String key : sortedKeys)
		{
			if ( session.listFilter.length() > 0 )
			{
				if ( key.toLowerCase().contains( session.listFilter.toLowerCase() ) ) sortedAndFilteredKeys.add( key );
			}
			else
			{
				sortedAndFilteredKeys.add( key );
			}
		}
		
		// If the user has filtered the list down we might need to adjust our page calculations
		// e.g. 20 total items might = 2 pages but filtered down to 5 items there would only be 1
		// Calculate first item to start with with
		// Page = 1, item = 1
		// Page = X, item = ( ( currentPage - 1 ) * itemsToShowPerPage ) + 1;
		int startItem = 1;
		int endItem = startItem + session.itemsToShowPerPage - 1;
		if (session.currentPage > 1)
		{
			startItem = ( ( session.currentPage - 1 ) * session.itemsToShowPerPage ) + 1;
			endItem = ( startItem + session.itemsToShowPerPage ) - 1;
		}

		int currentItem = 1;
		for (String key : sortedAndFilteredKeys)
		{
			String name = key;
			SortedSet<String> vals = listOfCommunities.get(key);
			for (String val: vals) {
				if (currentItem >= startItem && currentItem <= endItem)
				{
					String id = val;
					String editLink = "";
					String deleteLink = "";
					String listFilterString = "";
					if (session.listFilter.length() > 0) listFilterString = "&listFilterStr="+ session.listFilter;
					
					editLink = "<a href=\"communities.jsp?session.action=edit&communityid=" + id + "&page=" + session.currentPage 
							+ listFilterString + "\" title=\"Edit Community\">" + name + "</a>";
					deleteLink = "<a href=\"communities.jsp?session.action=delete&communityid=" + id + "&page=" + session.currentPage
							+ listFilterString + "\" title=\"Delete Community\" "
							+ "onclick='return confirm(\"Do you really wish to delete the following community: "
							+ name + "?\");'><img src=\"image/delete_x_button.png\" border=0></a>";
		
					// Create the HTML table row
					communities.append("<tr>");
					communities.append("<td bgcolor=\"white\" width=\"100%\">" + editLink + "</td>");
					communities.append("<td align=\"center\" bgcolor=\"white\"><input type=\"checkbox\" name=\"docsToDelete\" value=\"" + id + "\"/></td>");
					communities.append("<td align=\"center\" bgcolor=\"white\">" + deleteLink + "</td>");
					communities.append("</tr>");
				}
				currentItem++;
			}
		}
		
		// Calculate number of pages, current page, page links...
		communities.append("<tr><td colspan=\"2\" align=\"center\" class=\"subTableFooter\">");
		// --------------------------------------------------------------------------------
		// Create base URL for each page
		StringBuffer baseUrl = new StringBuffer();
		baseUrl.append("communities.jsp?");
		if (session.listFilter.length() > 0) baseUrl.append("listFilterStr=").append(session.listFilter).append("&");
		
		String actionString = (session.lastaction.equals("edit")) ? "session.action=" + session.lastaction : "";
		String communityIdString = (session.communityid.length() > 0) ? "communityid=" + session.communityid : "";
		
		if (actionString.length() > 0) baseUrl.append(actionString);
		if (actionString.length() > 0 && communityIdString.length() > 0) baseUrl.append("&");
		if (communityIdString.length() > 0) baseUrl.append(communityIdString);
		if (actionString.length() > 0 || communityIdString.length() > 0) baseUrl.append("&");
		
		baseUrl.append("page=");
		communities.append( createPageString( sortedAndFilteredKeys.size(), session.itemsToShowPerPage, session.currentPage, baseUrl.toString() ));
		communities.append("</td></tr>");
		// --------------------------------------------------------------------------------
		communities.append("</table>");
	}
	else
	{
		communities.append("No communities were retrieved");
	}

	return communities.toString();
}

%>
