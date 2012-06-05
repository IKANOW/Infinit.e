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

<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"%>
<%@ include file="inc/sharedFunctions.jsp" %>

<%!
	// 
	String action = "";
	String logoutAction = "";

	//
	String shareid = "";
	String sourceJson = "";
	String communityId = "";
	String shareTitle = "";
	String shareDescription = "";
	String shareType = "";
	
	// !----------  ----------!
	String sourceTemplateSelect = "";
	String selectedSourceTemplate = "";
	String communityIdSelect = "";	
	private static String starterSourceString = "{ title: \"Title\", description: \"Description\", url: " +
			"\"http://youraddress.com/news.rss\", isPublic: true, extractType: \"Feed\", mediaType: " +
			"\"Social\", tags: [ \"tag2\", \"tag1\" ], harvestBadSource: false }";
%>

<%
	messageToDisplay = "";
	
	// 
	if (isLoggedIn) 
	{
		// Determine which action to perform on postback/request
		action = "";
		if (request.getParameter("action") != null) action = request.getParameter("action").toLowerCase();
		if (request.getParameter("dispatchAction") != null) action = request.getParameter("dispatchAction").toLowerCase();
		if (request.getParameter("logoutButton") != null) action = request.getParameter("logoutButton").toLowerCase();
		if (request.getParameter("selectTemplate") != null) action = request.getParameter("selectTemplate");
		if (request.getParameter("saveSource") != null) action = "saveSource";
		
		try
		{

			// Read in values from the edit form
			shareid = (request.getParameter("shareid") != null) ? request.getParameter("shareid") : "";
			communityId = (request.getParameter("Community_ID") != null) ? request.getParameter("Community_ID") : "";
			shareTitle = (request.getParameter("shareTitle") != null) ? request.getParameter("shareTitle") : "";
			shareDescription = (request.getParameter("shareDescription") != null) ? request.getParameter("shareDescription") : "";
			sourceJson = (request.getParameter("Source_JSON") != null) ? request.getParameter("Source_JSON") : "";
			selectedSourceTemplate = (request.getParameter("sourceTemplateSelect") != null) ? request.getParameter("sourceTemplateSelect") : "";
			
			if (action.equals("")) 
			{
				sourceJson = new JSONObject(starterSourceString).toString(4);
			}
			else if (action.equals("selectTemplate")) 
			{
				sourceJson = getSourceJSONObjectFromShare(selectedSourceTemplate, request, response).toString(4);
			}
			else if (action.equals("saveSource")) 
			{
				String newId = saveShare(request, response);
				if ( newId.length() > 0 )
				{	
					out.println("<meta http-equiv=\"refresh\" content=\"0;url=sources.jsp?action=edit&shareid="+ newId + "\">");
				}
			}
			else if (action.equals("logout")) 
			{
				logOut(request, response);
				out.println("<meta http-equiv=\"refresh\" content=\"0;url=index.jsp\">");
			}
			
			createCommunityIdSelect(request, response);
			createSourceTemplateSelect(request, response);
		}
		catch (Exception e)
		{
			System.out.println(e.getMessage());
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
	<title>Infinit.e.Manager - Create New Source</title>
</head>
<body>

<%
	// !-- Create JavaScript Popup --
	if (messageToDisplay.length() > 0) { 
%>
	<script language="javascript" type="text/javascript">
		alert("<%=messageToDisplay %>");
	</script>
<% } %>

	<form method="post">
	
<%@ include file="inc/header.jsp" %>

<% if (!isLoggedIn) { %>
		<%@ include file="inc/login_form.jsp" %>
<% } else { %>
	
	<table class="standardTable" cellpadding="5" cellspacing="0" width="100%">
	<tr valign="top">
		<td width="30%" bgcolor="#ffffff">
		
			<table class="standardTable" cellpadding="5" cellspacing="1" width="100%">
			<tr>
				<td class="headerLink">Source Templates</td>
				<td align="right"></td>
			</tr>
			<tr>
				<td colspan="2" bgcolor="white">
					<table class="listTable" cellpadding="3" cellspacing="1" width="100%">
						<tr>
							<td bgcolor="white"><%=sourceTemplateSelect %><button name="selectTemplate" value="selectTemplate">Select</button></td>
						</tr>
						<tr>
							<td bgcolor="white">&nbsp;</td>
						</tr>
					</table>
				</td>
			</tr>
			</table>

		</td>
		
		<td width="70%" bgcolor="#ffffff">
		
			<table class="standardTable" cellpadding="5" cellspacing="1" width="100%">
			<tr>
				<td class="headerLink">New Source</td>
				<td align="right"></td>
			</tr>
			<tr>
				<td colspan="2" bgcolor="white">
					<table class="standardSubTable" cellpadding="3" cellspacing="1" width="100%" >
						<tr>
							<td bgcolor="white" width="30%">Title:</td>
							<td bgcolor="white" width="70%">
								<input type="text" id="shareTitle" name="shareTitle" value="<%=shareTitle%>" size="60" />
							</td>		
						</tr>
						<tr valign="top">
							<td bgcolor="white" width="30%">Description:</td>
							<td bgcolor="white" width="70%">
								<textarea cols="45" rows="3" id="shareDescription" name="shareDescription"><%=shareDescription%></textarea>
							</td>		
						</tr>
						<tr>
							<td bgcolor="white" width="30%">Community:</td>
							<td bgcolor="white" width="70%"><%=communityIdSelect%></td>		
						</tr>
						<tr>
							<td bgcolor="white" width="100%" colspan="2">
								<textarea cols="90" rows="25" id="Source_JSON" name="Source_JSON"><%=sourceJson%></textarea>
							</td>		
						</tr>
						<tr>
							<td bgcolor="white" width="30%">&nbsp;</td>
							<td bgcolor="white" width="70%">
								<button name="saveSource" value="saveSource">Save Source</button>
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
<% } %>

<%@ include file="inc/footer.jsp" %>
</body>
</html>


<%!

//saveShare - 
private String saveShare(HttpServletRequest request, HttpServletResponse response) 
{
	String returnid = "";
	if (validateFormFields() && validateSourceJson())
	{
		try 
		{
			String urlShareTitle = URLEncoder.encode(shareTitle.trim(), "UTF-8");
			String urlShareDescription = URLEncoder.encode(shareDescription.trim(), "UTF-8");
			String apiAddress = "social/share/add/json/source/" + urlShareTitle + "/" + urlShareDescription;
			
			//
			JSONObject source = new JSONObject(sourceJson);
			source.remove("title");
			source.put("title", shareTitle.trim());
			source.remove("description");
			source.put("description", shareDescription.trim());
			
			// CommunityID Array - Delete and replace with id from community id dropdown list
			if (communityId.length() > 0)
			{
				source.remove("communityIds");
				JSONArray communityIds = new JSONArray();
				communityIds.put(communityId);
				source.put("communityIds", communityIds);
				sourceJson = source.toString(4);
			} //TESTED
	
			// Post the update to our rest API and check the results of the post
			JSONObject json_response =  new JSONObject(postToRestfulApi(apiAddress, sourceJson, request, response));
			JSONObject responseObject = json_response.getJSONObject("response");
			JSONObject shareObject = json_response.getJSONObject("data");
			
			if (responseObject.getString("success").equalsIgnoreCase("true")) 
			{
				returnid = shareObject.getString("_id");
				messageToDisplay = "Success: " + responseObject.getString("message");
			}
			else
			{
				messageToDisplay = "Error: " + responseObject.getString("message");
			}
		} 
		catch (Exception e) 
		{
			messageToDisplay = "Error: " + e.getMessage() + " " + e.getStackTrace().toString();
		}
	}
	return returnid;
} // TESTED


// validateFormFields
private boolean validateFormFields()
{
	boolean isValid = true;
	ArrayList<String> al = new ArrayList<String>();
	if (shareTitle.length() < 1) al.add("Title");
	if (shareDescription.length() < 1) al.add("Description");
	if (sourceJson.length() < 1) al.add("Source JSON");
	
	if (al.size() > 0)
	{
		isValid = false;
		messageToDisplay = "Error, the following required fields are missing: " + al.toString();
	}
	return isValid;
}  // TESTED


// validateSourceJson
private boolean validateSourceJson()
{
	boolean isValid = true;
	try
	{
		JSONObject test = new JSONObject(sourceJson);
	}
	catch (Exception e)
	{
		isValid = false;
		messageToDisplay = "Error, the JSON in the source field is not valid. (" + e.getMessage() + ")";
	}
	return isValid;
}


// createCommunityIdSelect -
// Create select control with list of communityids available to user
private void createCommunityIdSelect(HttpServletRequest request, HttpServletResponse response) 
{
	try 
	{
		StringBuffer html = new StringBuffer();
		html.append("<select name=\"Community_ID\" id=\"Community_ID\">");
		
		JSONArray communities = getPersonCommunities(request, response);
		if (communities != null)
		{
			for (int i = 0; i < communities.length(); i++) 
			{
				JSONObject source = communities.getJSONObject(i);
				String name = source.getString("name");
				String id = source.getString("_id");
				String selectedString = (id.equalsIgnoreCase(communityId)) ? " SELECTED" : "";
				html.append("<option value=\"" + id + "\"" + selectedString + ">" + name + "</option>");
			}				
		}
		html.append("</select>");
		communityIdSelect = html.toString();
	} 
	catch (Exception e) 
	{
	}
} // TESTED



// createSourceTemplateSelect -
// Create select control with list of source templates available to user
private void createSourceTemplateSelect(HttpServletRequest request, HttpServletResponse response) 
{
	StringBuffer sources = new StringBuffer();
	sources.append("<select name=\"sourceTemplateSelect\" id=\"sourceTemplateSelect\">");
	sources.append("<option value=\"\">-- Basic RSS Source Template --</option>");
	
	String apiAddress = "social/share/search/?searchby=type&type=source_template";
	try 
	{
		// Call the api and get the result as a string
		String result = callRestfulApi(apiAddress, request, response);
		JSONObject json = new JSONObject(result);
		JSONObject json_response = json.getJSONObject("response");
		if (json_response.getString("success").equalsIgnoreCase("true") && json.has("data")) 
		{
			JSONArray data = json.getJSONArray("data");
			for (int i = 0; i < data.length(); i++) 
			{
				JSONObject source = data.getJSONObject(i);
				String title = source.getString("title");
				String id = source.getString("_id");
				sources.append("<option value=\"" + id + "\">" + title
						+ "</option>");
			}
		}
	} 
	catch (Exception e) 
	{
	}
	
	sources.append("</select>");
	sourceTemplateSelect = sources.toString();
} // TESTED


// getSourceJSONObject
private JSONObject getSourceJSONObjectFromShare(String shareId, HttpServletRequest request, HttpServletResponse response)
{
	try
	{
		// Call the api and get the result as a string
		String result = getShare(shareId, request, response);
		
		// Convert string to JSONObjects
		JSONObject json_response = new JSONObject(result);
		JSONObject data = json_response.getJSONObject("data");
		
		JSONObject source = new JSONObject(data.getString("share"));
		source.remove("_id");
		source.remove("created");
		source.remove("modified");
		source.remove("harvest");
		source.remove("key");
		source.remove("ownerId");
		source.remove("shah256Hash");
		source.remove("communityIds");
		return source;
	}
	catch (Exception e)
	{
		return null;
	}
}

%>
