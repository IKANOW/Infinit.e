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

<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1" session="false"%>
<%@ include file="inc/sharedFunctions.jsp" %>

<%!
	// 
	int currentPage = 1;
	int itemsToShowPerPage = 10;
	String action = "";
	String logoutAction = "";
	String listFilter = "";

	//
	String editTableTitle = "Add New Share";
		
	//
	String shareid = "";
	String shareType = "";
	String title = "";
	String description = "";
	
	String ownerId = "";
	String ownerEmail = "";
	String ownerDisplayname = "";

	String listOfCommunities = "";
		
%>

<%
	messageToDisplay = "";
	
	// 
	if (isLoggedIn) 
	{	
		// Capture value in the left handed table filter field
		if (request.getParameter("listFilter") != null) 
		{
			listFilter = request.getParameter("listFilter");
		}
		else if (request.getParameter("listFilterStr") != null) 
		{
			listFilter = request.getParameter("listFilterStr");
		}
		else
		{
			listFilter = "";
		}
		
		// Determine which action to perform on postback/request
		action = "";
		if (request.getParameter("action") != null) action = request.getParameter("action").toLowerCase();
		if (request.getParameter("dispatchAction") != null) action = request.getParameter("dispatchAction").toLowerCase();
		if (request.getParameter("clearForm") != null) action = request.getParameter("clearForm").toLowerCase();
		if (request.getParameter("filterList") != null) action = request.getParameter("filterList").toLowerCase();
		if (request.getParameter("clearFilter") != null) action = request.getParameter("clearFilter").toLowerCase();
		if (request.getParameter("logoutButton") != null) action = request.getParameter("logoutButton").toLowerCase();
		
		// Capture values sent by button clicks, these will override the action value as appropriate 
		String saveAccount = "";
		String createAccount = "";
		String updatePassword = "";
		String addto = "";
		String removefrom = "";
		if (request.getParameter("createAccount") != null) createAccount = request.getParameter("createAccount").toLowerCase();
		if (request.getParameter("saveAccount") != null) saveAccount = request.getParameter("saveAccount").toLowerCase();
		if (request.getParameter("updatePassword") != null) updatePassword = request.getParameter("updatePassword").toLowerCase();
		if (request.getParameter("addto") != null) addto = request.getParameter("addto");
		if (request.getParameter("removefrom") != null) removefrom = request.getParameter("removefrom");
		
		// Capture input for page value if passed to handle the page selected in the left hand list of items
		if (request.getParameter("page") != null) 
		{
			currentPage = Integer.parseInt( request.getParameter("page").toLowerCase() );
		}
		else
		{
			currentPage = 1;
		}
		
		try
		{
			// Always clear the form first so there is no bleed over of values from previous requests
			clearForm();

			// Read in values from the edit form
			shareid = (request.getParameter("shareid") != null) ? request.getParameter("shareid") : "";
			title = (request.getParameter("title") != null) ? request.getParameter("title") : "";
			description = (request.getParameter("description") != null) ? request.getParameter("description") : "";
			
			Boolean redirect = false;
			
			// If user has clicked save, create, or update buttons do those actions before handling the action param
			if (saveAccount.equals("saveaccount")) 
			{
				if ( validateFormFields() )
				{
					savePerson(false, request, response);
				}
			}
			if (createAccount.equals("createaccount")) 
			{
				if ( validateFormFields() )
				{
					savePerson(true, request, response);
					redirect = true;
				}
			}

			if (removefrom.length() > 0)
			{
				//removePersonFromCommunity(personid, removefrom, request, response);
				//redirect = true;
			}
			
			// if redirect == true refresh the page to update the edit form's content to reflect changes
			if (redirect) 
			{
				String urlParams = "";
				if (listFilter.length() > 0) urlParams = "&listFilterStr="+ listFilter;
				if (currentPage > 1) urlParams += "&page=" + currentPage;
				out.println("<meta http-equiv=\"refresh\" content=\"0;url=shares.jsp?action=edit&shareid=" 
					+ shareid + urlParams + "\">");
			}
			
			if (action.equals("clearform")) 
			{
				out.println("<meta http-equiv=\"refresh\" content=\"0;url=people.jsp\">");
			}
			else if (action.equals("edit")) 
			{
				populateEditForm(shareid, request, response);
			}
			else if (action.equals("delete")) 
			{
				//deleteAccount(personid, request, response);
				//out.println("<meta http-equiv=\"refresh\" content=\"0;url=shares.jsp\">");
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
			else if (action.equals("logout")) 
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
	<title>Infinit.e.Manager - Shares</title>
</head>
<body>

<%
	// !-- Create JavaScript Popup --
	if (messageToDisplay.length() > 0) { 
%>
	<script language="javascript" type="text/javascript">
		alert('<%=messageToDisplay %>');
	</script>
<% 
	} 
%>

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
				<td class="headerLink">Shares</td>
				<td align="right"><input type="text" id="listFilter" 
					onkeydown="if (event.keyCode == 13) { setDipatchAction('filterList'); 
					document.getElementById('filterList').click(); }" 
					name="listFilter" size="20" value="<%=listFilter %>"/><button name="filterList" 
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
				<td class="headerLink"><%=editTableTitle %></td>
				<td align="right"><button name="clearForm" value="clearForm">New Share</button></td>
			</tr>
			<tr>
				<td colspan="2" bgcolor="white">
					<table class="standardSubTable" cellpadding="5" cellspacing="1" width="100%">
					<tr>
						<td bgcolor="#ffffff" width="30%">Share Id:</td>
						<td bgcolor="#ffffff" width="70%"><%=shareid %></td>
					</tr>
					<tr>
						<td bgcolor="#ffffff" width="30%">Type</td>
						<td bgcolor="#ffffff" width="70%"><%=shareType %></td>
					</tr>
					<tr>
						<td bgcolor="#ffffff" width="30%">Title:*</td>
						<td bgcolor="#ffffff" width="70%"><input type="text" id="title" name="title" value="<%=title%>" size="50" /></td>
					</tr>
					<tr valign="top">
						<td bgcolor="#ffffff" width="30%">Description:*</td>
						<td bgcolor="#ffffff" width="70%">
							<textarea cols="60" rows="5" id="description" name="description"><%=description %></textarea>
						</td>
					</tr>
					<tr>
						<td bgcolor="#ffffff" width="30%">Owner:</td>
						<td bgcolor="#ffffff" width="70%"><%=ownerDisplayname %> - <%=ownerEmail %></td>
					</tr>

					<tr valign="top">
						<td bgcolor="#ffffff" width="30%">Communities:</td>
						<td bgcolor="#ffffff" width="70%"><%=listOfCommunities %></td>
					</tr>
					<tr>
						<td bgcolor="#ffffff" width="30%"></td>
						<td bgcolor="#ffffff" width="70%">
<%
	if (shareid.length() > 0) {
%>
							<button name="update" value="update">Update Share</button>
<%
	}
	else
	{
%>
							<button name="create" value="create">Create Share</button>	
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
private boolean validateFormFields()
{
	boolean isValid = true;
	ArrayList<String> al = new ArrayList<String>();
	/* if (firstName.length() < 1) al.add("First Name");
	if (lastName.length() < 1) al.add("Last Name");
	if (email.length() < 1) al.add("Email");
	if (phone.length() < 1) al.add("Phone"); */
	if (al.size() > 0)
	{
		isValid = false;
		messageToDisplay = "Error, the following required fields are missing: " + al.toString();
	}
	return isValid;
}  // TESTED




// savePerson -
private boolean savePerson( boolean isNewAccount, HttpServletRequest request, HttpServletResponse response )
{
	try
	{

		JSONObject actionResponse = null;
		if (isNewAccount)
		{
			//actionResponse = new JSONObject(postToRestfulApi("social/person/register", userJson, request, response));
			//JSONObject dataVal = new JSONObject(actionResponse.getString("data"));
			//personid = dataVal.getString("_id");
		}
		else
		{
			//actionResponse = new JSONObject(postToRestfulApi("social/person/update", userJson, request, response));
		}
		JSONObject responseVal = new JSONObject(actionResponse.getString("response"));
		
		if (responseVal.getString("success").equalsIgnoreCase("true"))
		{
			messageToDisplay = "Success: User account information saved."; return true;
		}
		else
		{
			messageToDisplay = "Error: Unable to save the user's account information.";
			return false;
		}
	}
	catch (Exception e)
	{
		messageToDisplay = "Error: Unable to the save user's account information. (" + e.getMessage() + ")"; return false;
	}
} // TESTED

// populateEditForm - 
private void populateEditForm(String id, HttpServletRequest request, HttpServletResponse response) 
{
	clearForm();
	if (id != null && id != "") 
	{
		try 
		{
			editTableTitle = "Edit Share";
			
			// Get person from API
			JSONObject personResponse = new JSONObject( getPerson(id, request, response) );
			JSONObject person = personResponse.getJSONObject("data");
			String status = person.getString("accountStatus").substring(0,1).toUpperCase() + person.getString("accountStatus").substring(1);
			/* accountStatus =  status;
			visiblePersonId = id;
			firstName = person.getString("firstName");
			lastName = person.getString("lastName");
			displayName = person.getString("displayName");
			email = person.getString("email");
			phone = person.getString("phone");
			emailReadOnly = "readOnly"; */
			
			// Output user communities
			JSONArray communities = person.getJSONArray("communities");
			listOfCommunities = getListOfCommunities(communities, request, response);
		} 
		catch (Exception e) 
		{
			System.out.println(e.getMessage());
		}
	}
}  // TESTED


// getListOfCommunities -
// Retrieve a list of all communities in the system and display with the following conditions:
// 1. Do not display the system or personal communities
// 2. If user is a member have option to remove user from the community
// 3. If user is not a member have the option to sign them up (users should not be able to sign them selves up
//	  unless the community supports self registration.)
private String getListOfCommunities(JSONArray memberOf, HttpServletRequest request, HttpServletResponse response)
{	
	StringBuffer communityList = new StringBuffer();
	communityList.append("<table width=\"100%\">");
	try
	{
		// Create an array list of communities the user is a member of
		List<String> memberOfList = new ArrayList<String>();
		for (int i = 0; i < memberOf.length(); i++)
		{
			JSONObject c = memberOf.getJSONObject(i);
			memberOfList.add(c.getString("_id"));
		}
		
		// Get a list of all communities from the api
		JSONObject communitiesObject = new JSONObject( getAllCommunities(request, response) );
		JSONArray communities = communitiesObject.getJSONArray("data");
		
		// Create an array list of all community names so we can sort them correctly for display
		List<String> listOfCommunityNames = new ArrayList<String>();
		for (int i = 0; i < communities.length(); i++)
		{
			JSONObject c = communities.getJSONObject(i);
			listOfCommunityNames.add(c.getString("name"));
		}
		Collections.sort( listOfCommunityNames, String.CASE_INSENSITIVE_ORDER );
		
		int column = 1;
		
		for (String communityName : listOfCommunityNames)
		{
			// Iterate over the list of all communities
			for (int i = 0; i < communities.length(); i++)
			{
				JSONObject community = communities.getJSONObject(i);
				if (community.getString("name").equalsIgnoreCase(communityName.toLowerCase()))
				{
					// Only show the non-system, non-personal communities
					if (community.getString("isPersonalCommunity").equalsIgnoreCase("false") && community.getString("isSystemCommunity").equalsIgnoreCase("false"))
					{
						if (column == 1) { communityList.append("<tr valign=\"middle\">"); }
						communityList.append("<td width=\"5%\">");
						
						String listFilterString = "";
						if (listFilter.length() > 0) listFilterString = "&listFilterStr="+ listFilter;
						String pageString = "";
						if (currentPage > 1) pageString = "&page=" + currentPage;
						
						String deleteLink = "<a href=\"people.jsp?action=edit&personid=" + shareid
								+ pageString + listFilterString + "&removefrom=" + community.getString("_id") 
								+ "\" title=\"Remove User for Community\" "
								+ "onclick='return confirm(\"Do you really wish to remove the user account from: "
								+ community.getString("name") + "?\");'><img src=\"image/delete.png\" border=0></a>";
								
						String addLink = "<a href=\"people.jsp?action=edit&personid=" + shareid
								+ pageString + listFilterString + "&addto=" + community.getString("_id") 
								+ "\" title=\"Add User to Community\"><img src=\"image/test.png\" border=0></a>";
						
						String divOne = "";
						String divTwo = "";
						if (memberOfList.contains(community.getString("_id")))
						{
							communityList.append(deleteLink);
						}
						else
						{
							communityList.append(addLink);
							divOne = "<div class=\"notAMemberOfCommunity\">";
							divTwo = "</div>";
						}
						
						communityList.append("</td>");
						communityList.append("<td width=\"45%\">");
						communityList.append(divOne + community.getString("name") + divTwo);
						communityList.append("</td>");
						if (column == 1) { column = 2; }
						else
						{
							communityList.append("</tr>");	
							column = 1;
						}
					}
				}
				
			}	
		}
	}
	catch (Exception e)
	{
	}
	communityList.append("</table>");
	return communityList.toString();
}



// clearForm
private void clearForm()
{
	editTableTitle = "Add New Share";
	shareid = "";
	title =  "";
	description = "";
	listOfCommunities = "";
}  // TESTED


// listItems -
private String listItems(HttpServletRequest request, HttpServletResponse response)
{
	/* StringBuffer shares = new StringBuffer();
	Map<String, String> listOfShares = getListOfAllShares(request, response);
	
	if (listOfShares.size() > 0)
	{
		shares.append("<table class=\"listTable\" cellpadding=\"3\" cellspacing=\"1\" width=\"100%\" >");

		// Sort the sources alphabetically
		List<String> sortedKeys = new ArrayList<String>(listOfShares.keySet());
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
			if (currentItem >= startItem && currentItem <= endItem)
			{
				String name = key;
				String id = listOfShares.get(key)[0].toString();
				String editLink = "";
				String deleteLink = "";
	
				String listFilterString = "";
				if (listFilter.length() > 0) listFilterString = "&listFilterStr="+ listFilter;
				
				editLink = "<a href=\"shares.jsp?action=edit&shareid=" + id + "&page=" + currentPage 
						+ listFilterString + "\" title=\"Edit Share\">" + name + "</a>";
				deleteLink = "<a href=\"shares.jsp?action=delete&shareid=" + id
						+ listFilterString + "\" title=\"Delete Share\" "
						+ "onclick='return confirm(\"Do you really wish to delete the " + name + " share?\");'>"
						+ "<img src=\"image/delete.png\" border=0></a>";
	
				// Create the HTML table row
				shares.append("<tr>");
				shares.append("<td bgcolor=\"white\" width=\"100%\">" + editLink + "</td>");
				shares.append("<td align=\"center\" bgcolor=\"white\">" + deleteLink + "</td>");
				shares.append("</tr>");
			}
			currentItem++;
		}
		
		// Calculate number of pages, current page, page links...
		shares.append("<tr><td colspan=\"2\" align=\"center\" class=\"subTableFooter\">");
		// --------------------------------------------------------------------------------
		// Create base URL for each page
		StringBuffer baseUrl = new StringBuffer();
		baseUrl.append("shares.jsp?");
		String actionString = (action.length() > 0) ? "action=" + action : "";
		String shareIdString = (shareid.length() > 0) ? "shareid=" + shareid : "";
		if (actionString.length() > 0) baseUrl.append(actionString);
		if (actionString.length() > 0 && shareIdString.length() > 0) baseUrl.append("&");
		if (shareIdString.length() > 0) baseUrl.append(shareIdString);
		if (actionString.length() > 0 || shareIdString.length() > 0) baseUrl.append("&");
		baseUrl.append("page=");
		shares.append( createPageString( sortedAndFilteredKeys.size(), itemsToShowPerPage, currentPage, baseUrl.toString() ));
		shares.append("</td></tr>");
		// --------------------------------------------------------------------------------
		shares.append("</table>");
	}
	else
	{
		shares.append("No shares were retrieved");
	}

	return shares.toString(); */
	
	return null;
}



%>
