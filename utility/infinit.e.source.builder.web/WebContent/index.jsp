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
<%@ page import="java.io.*" %>
<%@ page import="java.util.*" %>
<%@ page import="java.util.List" %>
<%@ page import="java.net.*" %>
<%@ page import="javax.servlet.jsp.*" %>
<%@ page import="org.apache.commons.io.*" %>
<%@ page import="com.google.gson.Gson" %>
<%@ page import="org.json.JSONObject" %>
<%@ page import="org.json.JSONArray" %>
<%@ page import="org.bson.types.ObjectId" %>
<%@ page import="infinit.e.source.builder.web.util.Utils" %>
<%@ page import="org.apache.commons.httpclient.HttpClient" %>
<%@ page import="org.apache.commons.httpclient.HttpStatus" %>
<%@ page import="org.apache.commons.httpclient.methods.PostMethod" %>
<%@ page import="java.io.BufferedReader" %>
<%@ page import="java.io.InputStreamReader" %>

<%@ include file="inc/sharedFunctions.jsp" %>

<%! 
	// !----------  ----------!
	String shareJson = "";
	String sourceJson = "";
	String shareId = "";
	String sourceId = "";
	String communityId = "";
	String shareCreated = "";
	String shareTitle = "";
	String shareDescription = "";
	String shareType = "";
	String shareTypeDisplayVal = "";
	String shareModified = "";
	String shareOwnerName = "";
	String shareOwnerEmail = "";
	
	// !----------  ----------!
	String harvesterOutput = "";
	
	// !----------  ----------!
	String messageToDisplay = "";
	String actionToTake = "";

	// !----------  ----------!
	String sourcePageNo = "0";
	String sourceTemplateSelect = "";
	String selectedSourceTemplate = "";
	String communityIdSelect = "";
	String getFullText = "";
	String getFullTextChecked = "";
	String numberOfDocuments = "";
	
%>

<%
	boolean isLoggedIn = false;
	messageToDisplay = "";

	// Page request is a post back from the login form
	if (request.getParameter("username") != null && request.getParameter("password") != null) 
	{
		String username = request.getParameter("username");
		String password = request.getParameter("password");
		isLoggedIn = getLogin(username, password, request, response);
		
		// Temp fix, refresh the page to retrieve the new cookie that was set
		out.println("<meta http-equiv=\"refresh\" content=\"0\">");
	}
	// Make sure user is already logged in and retrieve their user id
	else 
	{
		isLoggedIn = isLoggedIn(request, response);
	}

	// 
	if (isLoggedIn) 
	{	
		// Determine which action is being called for by the user
		String action = "";
		if (request.getParameter("action") != null) action = request.getParameter("action").toLowerCase();
		if (request.getParameter("dispatchAction") != null) action = request.getParameter("dispatchAction").toLowerCase();
		
		try
		{
			// Update vars with form/query data
			shareId = (request.getParameter("Share_ID") != null) ? request.getParameter("Share_ID") : "";
			if (shareId.length() < 1)
			{
				shareId = (request.getParameter("shareid") != null) ? request.getParameter("shareid") : "";
			}
			sourceId = (request.getParameter("sourceid") != null) ? request.getParameter("sourceid") : "";
			communityId = (request.getParameter("Community_ID") != null) ? request.getParameter("Community_ID") : "";
			shareTitle = (request.getParameter("shareTitle") != null) ? request.getParameter("shareTitle") : "";
			shareDescription = (request.getParameter("shareDescription") != null) ? request.getParameter("shareDescription") : "";
			sourceJson = (request.getParameter("Source_JSON") != null) ? request.getParameter("Source_JSON") : "";
			selectedSourceTemplate = (request.getParameter("sourceTemplateSelect") != null) ? request.getParameter("sourceTemplateSelect") : "";
			numberOfDocuments = (request.getParameter("numOfDocs") != null) ? request.getParameter("numOfDocs") : "10";
			getFullText = (request.getParameter("fullText") != null) ? "true" : "false";
			getFullTextChecked = (getFullText.equalsIgnoreCase("true")) ? "CHECKED" : "";
					
			if (shareId.length() < 1)
			{
				shareType = "";
				shareTypeDisplayVal = "";
				shareOwnerName = "";
				shareOwnerEmail = "";
				shareCreated = "";
				shareModified = "";
			}
			
			if (action.equals("logout")) 
			{
				logOut(request, response);
				out.println("<meta http-equiv=\"refresh\" content=\"0\">");
			}
			else if (action.equals("edit")) 
			{
				populateEditSourceForm(shareId, request, response);
			} 
			else if (action.equals("save-share")) 
			{
				saveShare(request, response);
			}
			else if (action.equals("save-template")) 
			{
				saveShareAsTemplate(request, response);
			}
			else if (action.equals("publish-source")) 
			{
				publishSource(request, response);
				populateEditSourceForm(shareId, request, response);
			}
			else if (action.equals("test")) 
			{
				getShare(shareId, request, response);
				testSource(request, response);
			}
			else if (action.equals("new-share")) 
			{
				createNewShare(selectedSourceTemplate, request, response);
			}
			else if (action.equals("sharefromsource")) 
			{
				createShareFromSource(sourceId, request, response);
			}
			else if (action.equals("delete")) 
			{
				deleteShare(shareId, request, response);
				clearSourceForm();
			}
			else if (action.equals("deletesource")) 
			{
				deleteSourceObject(sourceId, request, response);
				clearSourceForm();
			}
		}
		catch (Exception e)
		{
			//System.out.println(e.getMessage());
		}

		createCommunityIdSelect(request, response);
		createSourceTemplateSelect(request, response);
	}
	
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
	<title>Infinit.e Source.Builder - V0.1</title>
	<script type="text/javascript" src="inc/utilities.js"></script>
</head>
<body>

<%
	// !-- Create JavaScript Popup --
	if (messageToDisplay.length() > 0 && harvesterOutput.length() == 0) { 
%>
	<script language="javascript" type="text/javascript">
		alert("<%=messageToDisplay %>");
	</script>
<% } %>
	
<% 
	//!-- Open new window to show source test results --
	if (harvesterOutput.length() > 0) 
	{
		String messageToOutput = URLEncoder.encode(messageToDisplay, "UTF-8");
		String output = URLEncoder.encode(harvesterOutput, "UTF-8");
		harvesterOutput = "";
		messageToDisplay = "";
%>
	<script language="javascript" type="text/javascript">
		openTestSourceWindow('<%=messageToOutput %>', '<%=output %>');
	</script>
<% } %>

<%@ include file="inc/header.jsp" %>

<%
	// User already logged in
	if (isLoggedIn) 
	{
%>

	<!-- Begin source editing table and form -->
	<form method="post" name="source_form">
	<input type="hidden" id="Share_ID" name="Share_ID" value="<%=shareId%>">
	<input type="hidden" id="Source_Page" name="Source_Page" value="<%=sourcePageNo%>">
	<input type="hidden" id="dispatchAction" name="dispatchAction" value="" >

	<table bgcolor="gray" cellpadding="2" cellspacing="0" width="100%" >
		<tr valign="top">
		
			<!-- Your Sources/Create New Source Column -->
			<td bgcolor="white" width="33%">
				<table bgcolor="lightgray" cellpadding="5" cellspacing="1" width="100%" >
					<tr>
						<td bgcolor="lightgray" width="100%" align="center">
							<table width="100%">
								<tr>
									<td><b>Create New Source</b></td>
									<td align="right"><button onClick="setDispatchAction('logout')">Logout</button></td>
								</tr>
							</table>
						</td>
					</tr>
					<tr>
						<td bgcolor="white" width="100%" align="center">
							<table bgcolor="silver" cellpadding="3" cellspacing="1" width="100%">
								<tr>
									<td bgcolor="white" width="100%">
										<%=sourceTemplateSelect%>
									</td>
									<td bgcolor="white">
										<input type="image" src="img/right_arrow.png" title="Create New Source" 
											onClick="setDispatchAction('new-share')" />
									</td>
								</tr>
							</table>
						</td>
					</tr>
				
					<tr>
						<td bgcolor="lightgray" width="100%" align="center">
							<table width="100%">
								<tr>
									<td><b>Your Sources</b></td>
								</tr>
							</table>
						</td>
					</tr>
					<tr>
						<td bgcolor="white" width="100%" align="center">
							<%=listSources(request, response)%>
						</td>
					</tr>
			
				</table>
			</td>
			
			<!-- Source Editing Column -->
			<td bgcolor="white" width="66%">
				<table bgcolor="lightgray" cellpadding="5" cellspacing="1" width="100%" >
					<tr>
						<td bgcolor="lightgray" width="100%" align="center">
							<table width="100%">
								<tr><td><b>Edit Source</b></td></tr>
							</table>
						</td>
					</tr>
					<tr>
						<td bgcolor="white" width="100%">
							<table bgcolor="silver" cellpadding="3" cellspacing="1" width="100%" >
								<tr>
									<td bgcolor="white" width="25%">Share ID:</td>
									<td bgcolor="white" width="75%"><%=shareId%></td>		
								</tr>
								<tr>
									<td bgcolor="white" width="25%">Title:</td>
									<td bgcolor="white" width="75%">
										<input type="text" id="shareTitle" name="shareTitle" value="<%=shareTitle%>" size="75" />
									</td>		
								</tr>
								<tr valign="top">
									<td bgcolor="white" width="25%">Description:</td>
									<td bgcolor="white" width="75%">
										<textarea cols="50" rows="3" id="shareDescription" name="shareDescription"><%=shareDescription%></textarea>
									</td>		
								</tr>
								<tr>
									<td bgcolor="white" width="25%">Type:</td>
									<td bgcolor="white" width="75%"><%=shareTypeDisplayVal%></td>		
								</tr>
								<tr>
									<td bgcolor="white" width="25%">Owner:</td>
									<td bgcolor="white" width="75%"><%=shareOwnerName%> - <%=shareOwnerEmail%></td>		
								</tr>
								<tr>
									<td bgcolor="white" width="25%">Community:</td>
									<td bgcolor="white" width="75%"><%=communityIdSelect%></td>		
								</tr>
								<tr>
									<td bgcolor="white" width="25%">Test Parameters:</td>
									<td bgcolor="white" width="75%">
										Full Text: 
											<input type="checkbox" name="fullText" value="true" <%=getFullTextChecked %>/>
										Number of Documents: 
											<input type="text" id="numOfDocs" name="numOfDocs" value="<%=numberOfDocuments %>" size="3" title="Maximum of 10" /> 
									</td>		
								</tr>
								<tr>
									<td bgcolor="white" width="25%" width="100%" colspan="2">
										<table>
											<tr valign="top">
												<td>
													<textarea cols="85" rows="30" id="Source_JSON" name="Source_JSON"><%=sourceJson%></textarea>
												</td>
												<td width="1%" align="center">
<% 
	//!--  --
	if (!shareId.equalsIgnoreCase("")) {
%>
													<input type="image" src="img/test.png" title="Test Source" 
														onClick="setDispatchAction('test')" /><br />
													<input type="image" src="img/save.png" title="Save" 
														onClick="setDispatchAction('save-share')" /><br />
													<input type="image" src="img/save2.png" title="Save Source as Template" 
														onClick="setDispatchAction('save-template')" /><br /> 
													<input type="image" src="img/publish.png" title="Publish Source" 
														onClick='publishSourceAlert();' />
<% } %>
												</td>
												<td>
													&nbsp;
												</td>
											</tr>
										</table>
									</td>		
								</tr>
								<tr>
									<td bgcolor="white" width="25%">Created:</td>
									<td bgcolor="white" width="75%"><%=shareCreated%></td>		
								</tr>
								<tr>
									<td bgcolor="white" width="25%">Modified:</td>
									<td bgcolor="white" width="75%"><%=shareModified%></td>		
								</tr>
								
							</table>
													
						</td>
					</tr>

				</table>
			<!--  -->
			
			</td>
		</tr>
	</table>	
	</form>
<!-- End source editing table  -->

<%
	}
	// Use is not logged in, show login_form
	else 
	{
%>
		<%@ include file="inc/login_form.jsp" %>
<%
	}
%>

<%@ include file="inc/footer.jsp" %>
</body>
</html>


<%!

	// listSources - 
	// Retrieves a list of the user's sources and outputs the list as table rows
	private String listSources(HttpServletRequest request, HttpServletResponse response) 
	{
		StringBuffer sources = new StringBuffer();
		Map<String,String> userSources = getUserSourcesAndShares(request, response);
		boolean sourceOnly = false;
		
		if (userSources.size() > 0)
		{
			sources.append("<table bgcolor=\"silver\" cellpadding=\"3\" cellspacing=\"1\" width=\"100%\" >");

			// Sort the sources alphabetically
			List<String> sortedKeys = new ArrayList<String>(userSources.keySet());
			Collections.sort( sortedKeys );
			
			int itemNumber = 1;
			
			for (String key : sortedKeys)
			{
				String title = key;
				String id = userSources.get(key).toString();
				String sourceLink = "";
				String editLink = "";
				String deleteLink = "";
				
				if (title.contains("(*)"))
				{
					sourceLink = "<a href=\"index.jsp?action=edit&shareid=" + id
							+ "\" title=\"Edit Source\">" + title + "</a>";
	
					editLink = "<a href=\"index.jsp?action=edit&shareid=" + id
							+ "\" title=\"Edit Source\"><img src=\"img/edit.png\" border=0></a>";

					deleteLink = "<a href=\"index.jsp?action=delete&shareid=" + id
							+ "\" title=\"Delete Share and Source\" "
							+ "onclick='return confirm(\"Do you really wish to delete the share: "
							+ title + "?\");'><img src=\"img/delete.png\" border=0></a>";
				}
				else
				{
					sourceLink = "<a href=\"index.jsp?action=sharefromsource&sourceid=" + id
							+ "\" title=\"Create Share from Source\">" + title + "</a>";
					editLink = "<a href=\"index.jsp?action=sharefromsource&sourceid=" + id
							+ "\" title=\"Create Share from Source\"><img src=\"img/document.png\" border=0></a>";
					deleteLink = "<a href=\"index.jsp?action=deletesource&sourceid=" + id
							+ "\" title=\"Delete Source\" "
							+ "onclick='return confirm(\"Do you really wish to delete the source: "
							+ title + "?\");'><img src=\"img/delete.png\" border=0></a>";
				}

				// Create the HTML table row
				sources.append("<tr>");
				sources.append("<td bgcolor=\"white\" width=\"100%\">" + sourceLink + "</td>");
				sources.append("<td align=\"center\" bgcolor=\"white\">" + editLink + "</td>");
				sources.append("<td align=\"center\" bgcolor=\"white\">" + deleteLink + "</td>");
				sources.append("</tr>");
				
				itemNumber++;
			}
			
			// Display a note if there are sources that don't have matching shares
			sources.append("<tr>");
			sources.append("<td bgcolor=\"white\" colspan=\"3\">");
			sources.append("(*) - Share (Editable)<br />");
			sources.append("(+) - Document that is not owned by you</td>");
			sources.append("</tr>");
			
			
			// Calculate number of pages, current page, page links...
			sources.append(getPageString(userSources.size(), 1));
			
			sources.append("</table>");
		}
		else
		{
			sources.append("You have no saved sources.");
		}
		return sources.toString();
	} // TESTED
	
	
	// getPageString
	private String getPageString(double numOfSources, int currentPage)
	{
		StringBuffer pageNumbers = new StringBuffer();
		if (numOfSources <= 10)
		{
			return "";	
		}
		else
		{
			pageNumbers.append("<tr>");
			pageNumbers.append("<td bgcolor=\"white\" colspan=\"3\">");
			
			// Number of pages = numOfSources / 10 round up to nearest whole number (4.1 = 5)
			int numOfPages = (int) Math.ceil( numOfSources / 10.0 );
			for (int i = 1; i <= numOfPages; i++)
			{
				String pageHtml = "";
				if (i == currentPage)
				{
					pageHtml = String.valueOf(i);
				}
				else
				{
					
				}
				pageNumbers.append(pageHtml);
				if (i < numOfPages) pageNumbers.append(" ");
			}
			
			pageNumbers.append("</td>");
			pageNumbers.append("</tr>");
		}
		
		return "";
	}
	

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

	
	// createShareFromSource
	private void createShareFromSource(String sourceId, HttpServletRequest request, HttpServletResponse response)
	{
		try
		{
			JSONObject sourceResponse = new JSONObject( getSource(sourceId, request, response) );
			JSONObject sourceJson =  new JSONObject( sourceResponse.getString("data") );
			
			String urlShareTitle = URLEncoder.encode(sourceJson.getString("title"), "UTF-8");
			String urlShareDescription = URLEncoder.encode(sourceJson.getString("description"), "UTF-8");
			
			String apiAddress = "social/share/add/json/source/" + urlShareTitle + "/" + urlShareDescription;
			JSONObject jsonObject = new JSONObject(postToRestfulApi(apiAddress, sourceJson.toString(4), request, response));
			JSONObject json_response = jsonObject.getJSONObject("response");
			clearSourceForm();
		}
		catch (Exception e)
		{
			
		}
	}
	
	
	// createNewShare
	private void createNewShare(String templateId, HttpServletRequest request, HttpServletResponse response)
	{
		Random r = new Random();
		char c = (char)(r.nextInt(26) + 'a');
		
		String title = "1" + c + " - New source";
		String description = "New source";
		String json = "";
		
		try
		{
			// Get JSON from source_template
			String share = "";
			if (templateId.length() > 0)
			{
				JSONObject shareObject = new JSONObject(getShare(templateId, request, response));
				JSONObject data = shareObject.getJSONObject("data");
				json = data.getString("share");
			}
			else
			{
				json = basicNewSource; // String val in sharedFunctions.jsp file
			}
			
			String urlShareTitle = URLEncoder.encode(title, "UTF-8");
			String urlShareDescription = URLEncoder.encode(description, "UTF-8");
			String apiAddress = "social/share/add/json/source/" + urlShareTitle + "/" + urlShareDescription;
			
			JSONObject jsonObject = new JSONObject(postToRestfulApi(apiAddress, json, request, response));
			JSONObject json_response = jsonObject.getJSONObject("response");
		}
		catch (Exception e)
		{	
			System.out.println(e.getMessage());
		}
		clearSourceForm();
	}


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
			
			// Get the share object and make sure it is encoded properly for display
			shareJson = URLDecoder.decode(data.toString(), "UTF-8");
			return new JSONObject(data.getString("share"));
		}
		catch (Exception e)
		{
			return null;
		}
	}
	
	
	// populateEditSourceForm - 
	// Gets specific share from api and populates the fields in the edit source form
	private void populateEditSourceForm(String id, HttpServletRequest request, HttpServletResponse response) 
	{
		clearSourceForm();
		if (id != null && id != "") 
		{
			try 
			{
				// Call the api and get the result as a string
				String result = getShare(id, request, response);
				
				// Convert string to JSONObjects
				JSONObject json_response = new JSONObject(result);
				JSONObject data = json_response.getJSONObject("data");
				
				// Get the share object and make sure it is encoded properly for display
				shareJson = URLDecoder.decode(data.toString(), "UTF-8");
				JSONObject source = new JSONObject(data.getString("share"));
				JSONObject owner = data.getJSONObject("owner");
				
				try
				{
					communityId = source.getJSONArray("communityIds").getString(0);
				}
				catch (Exception ex) { }
				
				// Copy fields to the edit source form
				sourceJson = source.toString(4); // Formatted with indents for display
				shareTitle = data.getString("title");
				shareDescription = data.getString("description");
				shareType = data.getString("type");
				if (shareType.equalsIgnoreCase("source"))
				{
					shareTypeDisplayVal = "Source";
				}
				else if (shareType.equalsIgnoreCase("source_published"))
				{
					shareTypeDisplayVal = "Published Source";
				}
				else if (shareType.equalsIgnoreCase("source_template"))
				{
					shareTypeDisplayVal = "Source Template";
				}
						
				String shareType = data.getString("type");
				if (!shareType.equalsIgnoreCase("source_template")) 
				{
					shareId = data.getString("_id");
					shareOwnerName = owner.getString("displayName");
					shareOwnerEmail = owner.getString("email");
					shareCreated = data.getString("created");
					shareModified = data.getString("modified");
				}
				//
				else
				{
					shareId = "";
					shareJson = "";
				}
			} 
			catch (Exception e) 
			{
				//sourceJson = "Error:" + e.getMessage();
			}
		}
	} // TESTED

	

	// saveShare - 
	private void saveShare(HttpServletRequest request, HttpServletResponse response) 
	{
		try 
		{
			// Retrieve old share into JSONObject
			JSONObject oldShare = null;
			String oldId = null;
			if (shareJson.length() > 0)
			{
				oldShare = new JSONObject(URLDecoder.decode(shareJson, "UTF-8"));
				if (oldShare.has("_id")) oldId = oldShare.getString("_id");
			}
			
			String apiAddress = "";
			String urlShareTitle = URLEncoder.encode(shareTitle, "UTF-8");
			String urlShareDescription = URLEncoder.encode(shareDescription, "UTF-8");
			
			if (oldId != null)
			{
				apiAddress = "social/share/update/json/" + oldShare.getString("_id") + "/source/" +
					urlShareTitle + "/" + urlShareDescription;
			}
			else
			{
				apiAddress = "social/share/add/json/source/" + urlShareTitle + "/" + urlShareDescription;
			}
			
			// CommunityID Array - Delete and replace with id from community id dropdown list
			if (communityId.length() > 0)
			{
				JSONObject source = new JSONObject(sourceJson);
				source.remove("communityIds");
				JSONArray communityIds = new JSONArray();
				communityIds.put(communityId);
				source.put("communityIds", communityIds);
				sourceJson = source.toString(4);
			} //TESTED

			// Post the update to our rest API and check the results of the post
			JSONObject json_response = new JSONObject(postToRestfulApi(apiAddress, sourceJson, request, response)).getJSONObject("response");
			if (json_response.getString("success").equalsIgnoreCase("true")) 
			{
				messageToDisplay = "Success: " + json_response.getString("message");
			}
			else
			{
				messageToDisplay = "Error: " + json_response.getString("message");
			}
		} 
		catch (Exception e) 
		{
			messageToDisplay = "Error: " + e.getMessage() + " " + e.getStackTrace().toString();
		}
	} // TESTED
	
	
	// publishSource - 
	// 1. Add/update ingest.source object
	// 2. Delete the share object, shazam
	private void publishSource(HttpServletRequest request, HttpServletResponse response) 
	{
		try 
		{
			// CommunityID Array - Delete and replace with id from community id dropdown list
			if (communityId.length() > 0)
			{
				JSONObject source = new JSONObject(sourceJson);
				source.remove("communityIds");
				JSONArray communityIds = new JSONArray();
				communityIds.put(communityId);
				source.put("communityIds", communityIds);
				sourceJson = source.toString(4);
			} //TESTED
			
			String sourceApiString = "config/source/save/" + communityId;
			
			// Post the update to our rest API and check the results of the post
			JSONObject result = new JSONObject(postToRestfulApi(sourceApiString, sourceJson, request, response));
			JSONObject JSONresponse = result.getJSONObject("response");
			
			if (JSONresponse.getString("success").equalsIgnoreCase("true")) 
			{
				messageToDisplay = "Success: " + JSONresponse.getString("message");
				// Delete the share object - shareId
				String apiAddress = "social/share/remove/" + shareId;

				// Post the update to our rest API and check the results of the post
				JSONObject shareResponse = new JSONObject(callRestfulApi(apiAddress, request, response)).getJSONObject("response");
				if (shareResponse.getString("success").equalsIgnoreCase("true")) 
				{
					messageToDisplay += " (" + shareResponse.getString("message") + ")";
				}
				else
				{
					messageToDisplay += " (" + shareResponse.getString("message") + ")";
				}
			}
			else
			{
				messageToDisplay = "Error: " + JSONresponse.getString("message");
			}
		} 
		catch (Exception e) 
		{
			messageToDisplay = "Error: " + e.getMessage() + " " + e.getStackTrace().toString();
		}
	} // 
	
	
	
	// saveSourceAsTemplate - 
	private void saveShareAsTemplate(HttpServletRequest request, HttpServletResponse response) 
	{
		try 
		{
			String urlShareTitle = URLEncoder.encode(shareTitle + " - Template", "UTF-8");
			String urlShareDescription = URLEncoder.encode(shareDescription, "UTF-8");
			String apiAddress = "social/share/add/json/source_template/" + urlShareTitle + "/" + urlShareDescription;
			
			JSONObject JSONresponse = new JSONObject(postToRestfulApi(apiAddress, sourceJson, request, response)).getJSONObject("response");
			if (JSONresponse.getString("success").equalsIgnoreCase("true")) 
			{
				messageToDisplay = "Success: " + JSONresponse.getString("message");
			}
			else
			{
				messageToDisplay = "Error: " + JSONresponse.getString("message");
			}
		} 
		catch (Exception e) 
		{
			messageToDisplay = "Error: " + e.getMessage() + " " + e.getStackTrace().toString();
		}
	} //
	
	
	// deleteSourceObject -
	private void deleteSourceObject(String sourceId, HttpServletRequest request, HttpServletResponse response)
	{
		if (sourceId != null && sourceId != "") 
		{
			try 
			{
				JSONObject sourceResponse = new JSONObject( getSource(sourceId, request, response) );
				JSONObject source = new JSONObject( sourceResponse.getString("data") );
				JSONArray com = source.getJSONArray("communityIds");
				String tempCommunityId = com.getString(0);
						
				JSONObject JSONresponse = new JSONObject(deleteSource(sourceId, tempCommunityId, 
						request, response)).getJSONObject("response");
				
				if (JSONresponse.getString("success").equalsIgnoreCase("true")) 
				{
					messageToDisplay = "Success: " + JSONresponse.getString("message");
				}
				else
				{
					messageToDisplay = "Error: " + JSONresponse.getString("message");
				}
			}
			catch (Exception e)
			{
				messageToDisplay = "Error: " + e.getMessage() + " " + e.getStackTrace().toString();
			}
		}
	}
	
	// deleteShare -
	private void deleteShare(String shareId, HttpServletRequest request, HttpServletResponse response)
	{
		if (shareId != null && shareId != "") 
		{
			JSONObject source = getSourceJSONObjectFromShare(shareId, request, response);
			String apiAddress = "social/share/remove/" + shareId + "/";
			try 
			{
				JSONObject JSONresponse = new JSONObject(callRestfulApi(apiAddress, request, response)).getJSONObject("response");
				if (JSONresponse.getString("success").equalsIgnoreCase("true")) 
				{
					messageToDisplay = "Success: " + JSONresponse.getString("message");
				}
				else
				{
					messageToDisplay = "Error: " + JSONresponse.getString("message");
				}
			}
			catch (Exception e)
			{
				messageToDisplay = "Error: " + e.getMessage() + " " + e.getStackTrace().toString();
			}
		}
	} // TESTED
	

	// testSource -
	private void testSource(HttpServletRequest request, HttpServletResponse response)
	{
		int numDocs = 10;
		try
		{
			numDocs = Integer.parseInt(numberOfDocuments);
			if (numDocs < 1 || numDocs > 10) numDocs = 10;
		}
		catch (Exception e)
		{
			numDocs = 10;
		}
		
		String apiAddress = "config/source/test?returnFullText=" + getFullText + "&numReturn=" + String.valueOf(numDocs);
		
		harvesterOutput = "";
		messageToDisplay = "";
		
		try 
		{
			JSONObject jsonObject = new JSONObject(postToRestfulApi(apiAddress, sourceJson, request, response));
			JSONObject JSONresponse = jsonObject.getJSONObject("response");
			
			try
			{
				messageToDisplay = JSONresponse.getString("message");
				
				if (jsonObject.has("data"))
				{
					JSONArray data = jsonObject.getJSONArray("data");
					
					StringBuffer s = new StringBuffer();
					for (int i = 0; i < data.length(); i++ )
					{
						JSONObject jo = data.getJSONObject(i);
						s.append("\n");
						s.append(jo.toString(4));
					}
					
					harvesterOutput = s.toString();
				}
			}
			catch (Exception ex)
			{
				messageToDisplay = "Test Result: " + JSONresponse.getString("message");
			}
			if (harvesterOutput.length() < 1) harvesterOutput = " ";
		}
		catch (Exception e)
		{
			messageToDisplay = "Error: " + e.getMessage() + " " + e.getStackTrace().toString();
		}
	} // TESTED
	
	
	
	// clearSourceForm -
	// Clear edit source form fields
	private void clearSourceForm() 
	{
		shareId = "";
		shareTitle = "";
		shareDescription = "";
		shareType = "";
		shareOwnerName = "";
		shareOwnerEmail = "";
		shareCreated = "";
		shareModified = "";
		shareJson = "";
		sourceJson = "";
	} // TESTED

%>
	