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
	int currentPage = 1;
	int itemsToShowPerPage = 18;
	String action = "";
	String logoutAction = "";
	String listFilter = "";

	//
	String shareid = "";
	String sourceid = "";
	String formShareId = "";
	String shareJson = "";
	String sourceJson = "";
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
	messageToDisplay = "";
	
	// 
	if (isLoggedIn) 
	{	
		// Capture value in the left handed table filter field
		if (request.getParameter("listFilter") != null) listFilter = request.getParameter("listFilter");
		if (request.getParameter("listFilter") == null)
		{
			if (request.getParameter("listFilterStr") != null) listFilter = request.getParameter("listFilterStr");
		}
		
		// Determine which action to perform on postback/request
		action = "";
		if (request.getParameter("action") != null) action = request.getParameter("action").toLowerCase();
		if (request.getParameter("dispatchAction") != null) action = request.getParameter("dispatchAction").toLowerCase();
		if (request.getParameter("clearForm") != null) action = request.getParameter("clearForm").toLowerCase();
		if (request.getParameter("filterList") != null) action = request.getParameter("filterList").toLowerCase();
		if (request.getParameter("clearFilter") != null) action = request.getParameter("clearFilter").toLowerCase();
		if (request.getParameter("logoutButton") != null) action = request.getParameter("logoutButton").toLowerCase();
		
		if (request.getParameter("testSource") != null) action = "testSource";
		if (request.getParameter("saveSource") != null) action = "saveSource";
		if (request.getParameter("saveSourceAsTemplate") != null) action = "saveSourceAsTemplate";
		if (request.getParameter("publishSource") != null) action = "publishSource";
		if (request.getParameter("newSource") != null) action = "newSource";
		
		// Capture input for page value if passed to handle the page selected in the left hand list of items
		if (request.getParameter("page") != null) currentPage = Integer.parseInt( request.getParameter("page").toLowerCase() );
		else currentPage = 1;
		
		try
		{
			// Always clear the form first so there is no bleed over of values from previous requests
			clearForm();

			// Read in values from the edit form
			shareid = (request.getParameter("shareid") != null) ? request.getParameter("shareid") : "";
			formShareId = (request.getParameter("shareId") != null) ? request.getParameter("shareId") : "";
			sourceid = (request.getParameter("sourceid") != null) ? request.getParameter("sourceid") : "";
			communityId = (request.getParameter("Community_ID") != null) ? request.getParameter("Community_ID") : "";
			shareTitle = (request.getParameter("shareTitle") != null) ? request.getParameter("shareTitle") : "";
			shareDescription = (request.getParameter("shareDescription") != null) ? request.getParameter("shareDescription") : "";
			sourceJson = (request.getParameter("Source_JSON") != null) ? request.getParameter("Source_JSON") : "";
			selectedSourceTemplate = (request.getParameter("sourceTemplateSelect") != null) ? request.getParameter("sourceTemplateSelect") : "";
			numberOfDocuments = (request.getParameter("numOfDocs") != null) ? request.getParameter("numOfDocs") : "10";
			getFullText = (request.getParameter("fullText") != null) ? "true" : "false";
			getFullTextChecked = (getFullText.equalsIgnoreCase("true")) ? "CHECKED" : "";
			
			Boolean redirect = false;
			
			// if redirect == true refresh the page to update the edit form's content to reflect changes
			if (redirect) 
			{
				String urlParams = "";
				if (listFilter.length() > 0) urlParams = "&listFilterStr="+ listFilter;
				if (currentPage > 1) urlParams += "&page=" + currentPage;
				out.println("<meta http-equiv=\"refresh\" content=\"0;url=sources.jsp?action=edit&shareid=" 
					+ shareid + urlParams + "\">");
			}
			
			if (action.equals("clearform")) 
			{
				out.println("<meta http-equiv=\"refresh\" content=\"0;url=sources.jsp\">");
			}
			else if (action.equals("edit")) 
			{
				populateEditForm(shareid, request, response);
			}
			else if (action.equals("sharefromsource"))
			{
				// Create a new share from the source object
				String newshareid = createShareFromSource(sourceid, request, response);
				// redirect user to edit source page
				String listFilterString = "";
				if (listFilter.length() > 0) listFilterString = "&listFilterStr="+ listFilter;
				String urlArgs = "action=edit&shareid=" + newshareid + listFilterString + "&page=" + currentPage;
				out.println("<meta http-equiv=\"refresh\" content=\"0;url=sources.jsp?" + urlArgs + "\">");
			}
			else if (action.equals("delete")) 
			{
				deleteShare(shareid, request, response);
				out.println("<meta http-equiv=\"refresh\" content=\"0;url=sources.jsp\">");
			}
			else if (action.equals("filterlist")) 
			{
				currentPage = 1;
				populateEditForm(shareid, request, response);
			}
			else if (action.equals("clearfilter")) 
			{
				listFilter = "";
				populateEditForm(shareid, request, response);
			}
			else if (action.equals("saveSource")) 
			{
				saveShare(request, response);
				populateEditForm(shareid, request, response);
			}
			else if (action.equals("saveSourceAsTemplate")) 
			{
				saveShareAsTemplate(request, response);
				populateEditForm(shareid, request, response);
			}
			else if (action.equals("publishSource")) 
			{
				publishSource(request, response);
				out.println("<meta http-equiv=\"refresh\" content=\"0;url=sources.jsp\">");
			}
			else if (action.equals("newSource")) 
			{
				out.println("<meta http-equiv=\"refresh\" content=\"0;url=newsource.jsp\">");
			}
			else if (action.equals("testSource")) 
			{
				getShare(shareid, request, response);
				testSource(request, response);
			}
			else if (action.equals("logout")) 
			{
				logOut(request, response);
				out.println("<meta http-equiv=\"refresh\" content=\"0;url=index.jsp\">");
			}
			
			createCommunityIdSelect(request, response);
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
	<title>Infinit.e.Manager - Sources</title>
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


	<form method="post">
	
<%@ include file="inc/header.jsp" %>

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
				<td class="headerLink">Sources</td>
				<td align="right"><input type="text" id="listFilter" 
					onkeydown="if (event.keyCode == 13) { setDipatchAction('filterList'); 
					document.getElementById('filterList').click(); }" 
					name="listFilter" size="30" value="<%=listFilter %>"/><button name="filterList" 
					value="filterList">Filter</button><button name="clearFilter" value="clearFilter">Clear</button></td>
			</tr>
			<tr>
				<td colspan="2" bgcolor="white"><%=listItems(request, response) %></td>
			</tr>
			</table>

		</td>
		
		<td width="70%" bgcolor="#ffffff">
		
			<table class="standardTable" cellpadding="5" cellspacing="1" width="100%">
			<tr>
				<td class="headerLink">Edit Source</td>
				<td align="right"><button name="newSource" value="newSource">New Source</button></td>
			</tr>
			<tr>
				<td colspan="2" bgcolor="white">
					<table class="standardSubTable" cellpadding="3" cellspacing="1" width="100%" >
						<tr>
							<td bgcolor="white" width="30%">Share ID:</td>
							<td bgcolor="white" width="70%">
								<input type="text" id="shareId" name="shareId" value="<%=shareid%>" size="35" READONLY />
							</td>		
						</tr>
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
						<!-- <tr>
							<td bgcolor="white" width="30%">Type:</td>
							<td bgcolor="white" width="70%"><%=shareTypeDisplayVal%></td>		
						</tr> -->
						<tr>
							<td bgcolor="white" width="30%">Owner:</td>
							<td bgcolor="white" width="70%"><%=shareOwnerName%> - <%=shareOwnerEmail%></td>		
						</tr>
						<tr>
							<td bgcolor="white" width="30%">Community:</td>
							<td bgcolor="white" width="70%"><%=communityIdSelect%></td>		
						</tr>
						<tr>
							<td bgcolor="white" width="30%">Test Parameters:</td>
							<td bgcolor="white" width="70%" style="height:21px">
								Full Text: <input type="checkbox" name="fullText" value="true" <%=getFullTextChecked %>/>
								Number of Documents: <input type="text" id="numOfDocs" name="numOfDocs" value="<%=numberOfDocuments %>"
									size="3" title="Maximum of 10" />
							</td>		
						</tr>
						<tr>
							<td bgcolor="white" width="100%" colspan="2">
								<textarea cols="90" rows="25" id="Source_JSON" name="Source_JSON"><%=sourceJson%></textarea>
							</td>		
						</tr>
						<tr>
							<td bgcolor="white" width="30%">Created:</td>
							<td bgcolor="white" width="70%"><%=shareCreated%></td>		
						</tr>
						<tr>
							<td bgcolor="white" width="30%">Modified:</td>
							<td bgcolor="white" width="70%"><%=shareModified%></td>		
						</tr>
						<tr>
							<td bgcolor="white" width="30%">&nbsp;</td>
							<td bgcolor="white" width="70%">
<% if (!shareid.equalsIgnoreCase("")) { %>
								<button name="testSource" value="testSource">Test Source</button>
								<button name="saveSource" value="saveSource">Save Source</button>
								<button name="saveSourceAsTemplate" value="saveSourceAsTemplate">Save Source as Template</button>
								<button name="publishSource" value="publishSource">Publish Source</button>
<% } %>
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

// validateFormFields
private boolean validateFormFields()
{
	boolean isValid = true;
	ArrayList<String> al = new ArrayList<String>();
	if (shareTitle.length() < 1) al.add("Title");
	if (shareDescription.length() < 1) al.add("Description");
	if (al.size() > 0)
	{
		isValid = false;
		messageToDisplay = "Error, the following required fields are missing: " + al.toString();
	}
	return isValid;
}  // TESTED



// saveShare - 
private void saveShare(HttpServletRequest request, HttpServletResponse response) 
{
	try 
	{
		String oldId = formShareId;
		
		String apiAddress = "";
		String urlShareTitle = URLEncoder.encode(shareTitle.trim(), "UTF-8");
		String urlShareDescription = URLEncoder.encode(shareDescription.trim(), "UTF-8");
		
		if (oldId != null)
		{
			apiAddress = "social/share/update/json/" + oldId + "/source/" + urlShareTitle + "/" + urlShareDescription;
		}
		else
		{
			apiAddress = "social/share/add/json/source/" + urlShareTitle + "/" + urlShareDescription;
		}
		
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
			String apiAddress = "social/share/remove/" + shareid;

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



// populateEditForm - 
private void populateEditForm(String id, HttpServletRequest request, HttpServletResponse response) 
{
	clearForm();
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
				shareid = data.getString("_id");
				shareOwnerName = owner.getString("displayName");
				shareOwnerEmail = owner.getString("email");
				shareCreated = data.getString("created");
				shareModified = data.getString("modified");
			}
			//
			else
			{
				shareid = "";
				shareJson = "";
			}
		} 
		catch (Exception e) 
		{
			sourceJson = "Error:" + e.getMessage();
		}
	}
}  // TESTED



// clearForm
private void clearForm()
{
	shareid = "";
	shareTitle = "";
	shareDescription = "";
	shareType = "";
	shareOwnerName = "";
	shareOwnerEmail = "";
	shareCreated = "";
	shareModified = "";
	shareJson = "";
	sourceJson = "";
}  // TESTED



// listItems -
private String listItems(HttpServletRequest request, HttpServletResponse response)
{
	StringBuffer sources = new StringBuffer();
	Map<String, String> listOfSources = getUserSourcesAndShares(request, response);
	
	if (listOfSources.size() > 0)
	{
		sources.append("<table class=\"listTable\" cellpadding=\"3\" cellspacing=\"1\" width=\"100%\" >");

		// Sort the sources alphabetically
		List<String> sortedKeys = new ArrayList<String>(listOfSources.keySet());
		Collections.sort( sortedKeys, String.CASE_INSENSITIVE_ORDER );
		
		// Filter the list
		List<String> sortedAndFilteredKeys = new ArrayList<String>();
		for (String key : sortedKeys)
		{
			if ( listFilter.length() > 0 )
			{
				if ( key.toLowerCase().contains( listFilter.toLowerCase() ) ) sortedAndFilteredKeys.add( key );
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
		int endItem = startItem + itemsToShowPerPage - 1;
		if (currentPage > 1)
		{
			startItem = ( ( currentPage - 1 ) * itemsToShowPerPage ) + 1;
			endItem = ( startItem + itemsToShowPerPage ) - 1;
		}

		int currentItem = 1;
		for (String key : sortedAndFilteredKeys)
		{
			String name = key;
			if (currentItem >= startItem && currentItem <= endItem)
			{
				String id = listOfSources.get(key).toString();
				String editLink = "";
				String deleteLink = "";
				String listFilterString = "";
				if (listFilter.length() > 0) listFilterString = "&listFilterStr="+ listFilter;
				
				if (name.contains("*"))
				{
					editLink = "<a href=\"sources.jsp?action=edit&shareid=" + id + "&page=" + currentPage 
							+ listFilterString + "\" title=\"Edit Share\">" + name + "</a>";

					deleteLink = "<a href=\"sources.jsp?action=delete&shareid=" + id + "&page=" + currentPage 
							+ listFilterString + "\" title=\"Delete Share\" "
							+ "onclick='return confirm(\"Do you really wish to delete the share: "
							+ name + "?\");'><img src=\"image/delete_x_button.png\" border=0></a>";
				}
				else
				{
					editLink = "<a href=\"sources.jsp?action=sharefromsource&sourceid=" + id + "&page=" + currentPage 
							+ listFilterString + "\" title=\"Create Share from Source\">" + name + "</a>";
							
					deleteLink = "<a href=\"sources.jsp?action=deletesource&sourceid=" + id + "&page=" + currentPage 
							+ listFilterString + "\" title=\"Delete Source\" "
							+ "onclick='return confirm(\"Do you really wish to delete the source: "
							+ name + "?\");'><img src=\"image/delete_x_button.png\" border=0></a>";
				}
	
				// Create the HTML table row
				sources.append("<tr valign=\"top\">");
				sources.append("<td bgcolor=\"white\" width=\"100%\">" + editLink + "</td>");
				sources.append("<td align=\"center\" bgcolor=\"white\">" + deleteLink + "</td>");
				sources.append("</tr>");
			}
			currentItem++;
		}
		
		sources.append("<tr valign=\"top\">");
		sources.append("<td bgcolor=\"white\" width=\"100%\" colspan=\"2\">");
		sources.append("(*) Share<br>");
		sources.append("(+) Source owned by someone else");
		sources.append("</td>");
		sources.append("</tr>");
		
		// Calculate number of pages, current page, page links...
		sources.append("<tr><td colspan=\"2\" align=\"center\" class=\"subTableFooter\">");
		// --------------------------------------------------------------------------------
		// Create base URL for each page
		StringBuffer baseUrl = new StringBuffer();
		baseUrl.append("sources.jsp?");
		String actionString = (action.length() > 0) ? "action=" + action : "";
		String shareIdString = (shareid.length() > 0) ? "shareid=" + shareid : "";
		if (actionString.length() > 0) baseUrl.append(actionString);
		if (actionString.length() > 0 && shareIdString.length() > 0) baseUrl.append("&");
		if (shareIdString.length() > 0) baseUrl.append(shareIdString);
		if (actionString.length() > 0 || shareIdString.length() > 0) baseUrl.append("&");
		baseUrl.append("page=");
		sources.append( createPageString( sortedAndFilteredKeys.size(), itemsToShowPerPage, currentPage, baseUrl.toString() ));
		sources.append("</td></tr>");
		// --------------------------------------------------------------------------------
		sources.append("</table>");
	}
	else
	{
		sources.append("No sources were retrieved");
	}

	return sources.toString();
}


// createShareFromSource
private String createShareFromSource(String sourceId, HttpServletRequest request, HttpServletResponse response)
{
	try
	{
		JSONObject sourceResponse = new JSONObject( getSource(sourceId, request, response) );
		JSONObject sourceJson =  new JSONObject( sourceResponse.getString("data") );
		
		String urlShareTitle = URLEncoder.encode(sourceJson.getString("title"), "UTF-8");
		String urlShareDescription = "";
		try
		{
			urlShareDescription = URLEncoder.encode(sourceJson.getString("description"), "UTF-8");
		}
		catch (Exception de)
		{
			urlShareDescription = URLEncoder.encode("Share description goes here", "UTF-8");
		}
		
		String apiAddress = "social/share/add/json/source/" + urlShareTitle + "/" + urlShareDescription;
		JSONObject jsonObject = new JSONObject( postToRestfulApi(apiAddress, sourceJson.toString(4), request, response) );
		JSONObject json_response = jsonObject.getJSONObject("response");
		JSONObject json_data = new JSONObject ( jsonObject.getString("data") );
		
		//clearForm();
		//populateEditForm(json_data.getString("_id"), request, response);
		
		// Return new shareid to caller
		return json_data.getString("_id");
	}
	catch (Exception e)
	{
		System.out.println(e.getMessage());
		return null;
	}
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



%>
