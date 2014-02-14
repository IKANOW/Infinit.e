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
	int currentPage = 1;
	int itemsToShowPerPage = 10;
	String action = "";
	String lastaction = "";
	String logoutAction = "";
	String listFilter = "";
		
	//
	String communityid = "";
	String communityName = "";
	String personid = "";
	String displayName = "";
	String userStatus = "";
	String userType = "";
	String userStatusControl = "";
	String userTypeControl = "";
	String userStatusSelect = "";
	String userTypeSelect = "";
	String email = "";
		
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
		lastaction = action;
		if (request.getParameter("dispatchAction") != null) action = request.getParameter("dispatchAction").toLowerCase();
		if (request.getParameter("clearForm") != null) action = request.getParameter("clearForm").toLowerCase();
		if (request.getParameter("filterList") != null) action = request.getParameter("filterList").toLowerCase();
		if (request.getParameter("removeSelected") != null) action = request.getParameter("removeSelected").toLowerCase();
		if (request.getParameter("clearFilter") != null) action = request.getParameter("clearFilter").toLowerCase();
		if (request.getParameter("logoutButton") != null) action = request.getParameter("logoutButton").toLowerCase();
		communityid = (request.getParameter("communityid") != null) ? request.getParameter("communityid") : "";

		// Capture values sent by button clicks, these will override the action value as appropriate 
		String updateMember = "";
		String removefrom = "";
		
		if (request.getParameter("updateMember") != null) updateMember = request.getParameter("updateMember").toLowerCase();
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
			personid = (request.getParameter("personid") != null) ? request.getParameter("personid") : "";
			displayName = (request.getParameter("displayName") != null) ? request.getParameter("displayName") : "";
			email = (request.getParameter("email") != null) ? request.getParameter("email") : "";
			userTypeSelect = (request.getParameter("userTypeSelect") != null) ? request.getParameter("userTypeSelect") : "";
			userStatusSelect = (request.getParameter("userStatusSelect") != null) ? request.getParameter("userStatusSelect") : "";
			
			Boolean redirect = false;
			
			// 
			if (updateMember.equals("updatemember")) 
			{
				updateMember(personid, communityid, userStatusSelect, userTypeSelect, request, response);
			}
			
			// if redirect == true refresh the page to update the edit form's content to reflect changes
			if (redirect) 
			{
				String urlParams = "";
				if (listFilter.length() > 0) urlParams = "&listFilterStr="+ listFilter;
				if (currentPage > 1) urlParams += "&page=" + currentPage;
				out.println("<meta http-equiv=\"refresh\" content=\"0;url=members.jsp?communityid=" + communityid + 
					"&action=edit&personid=" + personid + urlParams + "\">");
			}
			
			if (action.equals("clearform")) 
			{
				out.println("<meta http-equiv=\"refresh\" content=\"0;url=members.jsp?communityid=" + communityid + "\">");
			}
			else if (action.equals("edit")) 
			{
				populateEditForm(personid, request, response);
			}
			else if (action.equals("delete")) 
			{
				removePersonFromCommunity(personid, communityid, request, response);
				out.println("<meta http-equiv=\"refresh\" content=\"0;url=members.jsp?communityid=" + communityid + "\">");
			}
			else if (action.equals("removeselected")) 
			{
				String[] ids= request.getParameterValues("peopleToRemove");
				
				int nRemoved = 0;
				int nFailed = 0;
				for (String id: ids) {
					
					if (!removePersonFromCommunity(id, communityid, request, response)) {
						nFailed++;
					}
					else 
						nRemoved++;
				}
				messageToDisplay = "Bulk community removal: removed " + nRemoved + ", failed: " + nFailed; 
				
				out.print("<meta http-equiv=\"refresh\" content=\"0;url=members.jsp?communityid=" + communityid);
				if (currentPage > 1) {
					out.print("&page=" + currentPage);
				}
				if (listFilter.length() > 0) {
					out.print("&listFilterStr=" + listFilter);					
				}
				out.println("\">");
			}
			else if (action.equals("filterlist")) 
			{
				currentPage = 1; // (don't perpetuate this action across page jumps)
				populateEditForm(personid, request, response);
			}
			else if (action.equals("clearfilter")) 
			{
				currentPage = 1; // (don't perpetuate this action across page jumps)
				listFilter = "";
				populateEditForm(personid, request, response);
			}
			else if (action.equals("logout")) 
			{
				logOut(request, response);
				out.println("<meta http-equiv=\"refresh\" content=\"0;url=index.jsp\">");
			}
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
	<title>Infinit.e.Manager - Community Members</title>
</head>
<body>

<%
	// !-- Create JavaScript Popup --
	if (messageToDisplay.length() > 0) { 
%>
	<script language="javascript" type="text/javascript">
		alert("<%=messageToDisplay.replace("\"", "\\\"") %>");
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
				<td class="headerLink">Members</td>
				<td align="right"><input type="text" id="listFilter" 
					onkeydown="if (event.keyCode == 13) { setDipatchAction('filterList'); 
					document.getElementById('filterList').click(); }" 
					name="listFilter" size="20" value="<%=listFilter %>"/><button name="filterList" 
					value="filterList">Filter</button><button name="clearFilter" value="clearFilter">Clear</button></td>
			</tr>
			<tr>
				<td colspan="2" bgcolor="white"><%=listItems(request, response) %></td>
			</tr>
			<tr>
				<td colspan="2" >
				<button name="removeSelected" onclick="return confirm('Do you really wish to remove the selected people from this community?');" name="removeSelected" value="removeSelected">Remove selected people</button>
				<input type="checkbox" name="selectall" onchange="var cbs = document.getElementsByName('peopleToRemove'); for(var i=0; i < cbs.length; i++) if(cbs[i].type == 'checkbox') cbs[i].checked=selectall.checked" value=""></input>
				<div style="float: right"><a align="right" href='communities.jsp?action=edit&communityid=<%=communityid %>'>Back</a></div>
				</td>
			</tr>
			</table>

		</td>
		
		<td width="70%" bgcolor="#ffffff">
		
			<table class="standardTable" cellpadding="5" cellspacing="1" width="100%">
			<tr>
				<td class="headerLink">Edit Selected Member</td>
				<td align="right">&nbsp;</td>
			</tr>
			<tr>
				<td colspan="2" bgcolor="white">
					<table class="standardSubTable" cellpadding="5" cellspacing="1" width="100%">
					<tr>
						<td bgcolor="#ffffff" width="30%">Community:</td>
						<td bgcolor="#ffffff" width="70%">
							<a href="communities.jsp?action=edit&communityid=<%=communityid %>" title="Return to <%=communityName %>"><%=communityName %></a>
						</td>
					</tr>
					<tr>
						<td bgcolor="#ffffff" width="30%">Member Id:</td>
						<td bgcolor="#ffffff" width="70%"><%=personid %></td>
					</tr>
					<tr>
						<td bgcolor="#ffffff" width="30%">Member Name:</td>
						<td bgcolor="#ffffff" width="70%"><%=displayName%></td>
					</tr>

					<tr>
						<td bgcolor="#ffffff" width="30%">Email:</td>
						<td bgcolor="#ffffff" width="70%"><%=email%></td>
					</tr>
					<tr>
						<td bgcolor="#ffffff" width="30%">Member Type:</td>
						<td bgcolor="#ffffff" width="70%"><%=userTypeControl %></td>
					</tr>
					<tr>
						<td bgcolor="#ffffff" width="30%">Membership Status:</td>
						<td bgcolor="#ffffff" width="70%"><%=userStatusControl %></td>
					</tr>
					<tr valign="top">
						<td bgcolor="#ffffff" width="30%" class="disabledText">User Attributes*:</td>
						<td bgcolor="#ffffff" width="70%">
							<table cellpadding="5" cellspacing="1" width="100%" class="disabledText">
								<tr>
									<td><input type="checkbox" name="publishQueriesToActivityFeed" READONLY /> User Queries are Published to Activity Feed</td>
								</tr>
								<tr>
									<td><input type="checkbox" name="publishLoginToActivityFeed" READONLY /> User Login Published to Activity Feed</td>
								</tr>
								<tr>
									<td><input type="checkbox" name="publishSharingToActivityFeed" READONLY /> User Shares Published to Activity Feed</td>
								</tr>
								<tr>
									<td><input type="checkbox" name="publishCommentsToActivityFeed" READONLY /> User Comments Published to Activity Feed</td>
								</tr>
								<tr>
									<td><input type="checkbox" name="publishCommentsPublicly" READONLY /> User Comments are Public</td>
								</tr>
								<tr>
									<td>* Note: The user attribute functionality is not yet implemented within Infinit.e.</td>
								</tr>
							</table>
						</td>
					</tr>
					<tr>
						<td bgcolor="#ffffff" width="30%"></td>
						<td bgcolor="#ffffff" width="70%">
<%
	if (personid.length() > 0) {
%>
							<button name="updateMember" value="updateMember">Update Member</button>
<%
	}
	else
	{
%>
							&nbsp;	
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

// updateMember
private boolean updateMember(String person, String community, String status, String type, HttpServletRequest request, HttpServletResponse response)
{
	try
	{
		JSONObject updateResponse = new JSONObject ( new JSONObject ( updateCommunityMemberStatus(community, person, 
				status, request, response) ).getString("response") );
		
		if (updateResponse.getString("success").equalsIgnoreCase("true"))
		{
			updateResponse = new JSONObject ( new JSONObject ( updateCommunityMemberType(community, person, 
					type, request, response) ).getString("response") );
			if (updateResponse.getString("success").equalsIgnoreCase("true"))
			{
				messageToDisplay = "Success: Member updated."; return true;
			}
			else
			{
				messageToDisplay = "Error: Unable to update member: " +  updateResponse.getString("message"); return false;
			}
		}
		else
		{
			messageToDisplay = "Error: Unable to update member: " +  updateResponse.getString("message"); return false;
		}
	}
	catch (Exception e)
	{
		messageToDisplay = "Error: Unable to update member. (" + e.getMessage() + ")"; return false;
	}
}


// removePersonFromCommunity
private boolean removePersonFromCommunity(String person, String community, HttpServletRequest request, HttpServletResponse response)
{
	try
	{
		JSONObject updateResponse = new JSONObject ( new JSONObject ( removeFromCommunity(community, person, request, response) ).getString("response") );
		if (updateResponse.getString("success").equalsIgnoreCase("true"))
		{
			messageToDisplay = "Success: Person removed from community."; return true;
		}
		else
		{
			messageToDisplay = "Error: Unable to remove person from community."; return false;
		}
	}
	catch (Exception e)
	{
		messageToDisplay = "Error: Unable to remove person from community. (" + e.getMessage() + ")"; return false;
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
			JSONObject communityResponse = new JSONObject( getCommunity(communityid, request, response) );
			JSONObject community = new JSONObject( communityResponse.getString("data") );
			communityName = community.getString("name");
			communityid = community.getString("_id");
			
			JSONArray members = community.getJSONArray("members");
			for (int i = 0; i < members.length(); i++)
			{
				JSONObject person = members.getJSONObject(i);
				if (person.getString("_id").equalsIgnoreCase(id))
				{
					//System.out.println(person.toString(4));
					
					personid = person.getString("_id");
					displayName = person.getString("displayName");
					email = person.getString("email");
					
					// User Type: Owner, Moderator, User
					StringBuffer type = new StringBuffer();
					String typeStr =  person.getString("userType");
					type.append("<select name='userTypeSelect' id='userTypeSelect'>");
					String[] typeVals = {"Owner","Moderator","Content_Publisher","Member"};
					for (String s : typeVals)
					{
						type.append("<option value='" + s.toLowerCase() + "'");
						if (typeStr.equalsIgnoreCase(s)) type.append(" SELECTED");
						type.append(">" + s + "</option>");
					}
					type.append("</select>");
					userTypeControl = type.toString();
					
					
					// User Status: Active, Pending, Disabled, Removed
					StringBuffer status = new StringBuffer();
					String statusStr =  person.getString("userStatus");
					status.append("<select name='userStatusSelect' id='userStatusSelect'>");
					String[] statusVals = {"Active","Pending","Disabled"};
					for (String s : statusVals)
					{
						status.append("<option value='" + s.toLowerCase() + "'");
						if (statusStr.equalsIgnoreCase(s)) status.append(" SELECTED");
						status.append(">" + s + "</option>");
					}
					status.append("</select>");
					userStatusControl = status.toString();
					
				}
			}

		} 
		catch (Exception e) 
		{
			System.out.println(e.getMessage());
		}
	}
}  // TESTED




// clearForm
private void clearForm()
{
	communityName = "";
	personid = "";
	displayName =  "";
	displayName = "";
	email = "";
	userStatusControl = "";
	userTypeControl = "";
}  // TESTED


// listItems -
private String listItems(HttpServletRequest request, HttpServletResponse response)
{
	StringBuffer people = new StringBuffer();
	Map<String, String> listOfPeople = getListOfCommunityMembers(communityid, request, response);
	
	if (listOfPeople.size() > 0)
	{
		people.append("<table class=\"listTable\" cellpadding=\"3\" cellspacing=\"1\" width=\"100%\" >");

		// Sort the sources alphabetically
		List<String> sortedKeys = new ArrayList<String>(listOfPeople.keySet());
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
				String id = listOfPeople.get(key).toString();
				String editLink = "";
				String deleteLink = "";
	
				String listFilterString = "";
				if (listFilter.length() > 0) listFilterString = "&listFilterStr="+ listFilter;
				
				editLink = "<a href=\"members.jsp?communityid=" + communityid + "&action=edit&personid=" 
						+ id + "&page=" + currentPage 
						+ listFilterString + "\" title=\"Edit Member\">" + name + "</a>";
				deleteLink = "<a href=\"members.jsp?communityid=" + communityid + "&action=delete&personid=" + id
						+ listFilterString + "\" title=\"Remove Member from Community\" "
						+ "onclick='return confirm(\"Do you really wish to remove the member?\");'>" 
						+ "<img src=\"image/delete_x_button.png\" border=0></a>";
	
				// Create the HTML table row
				people.append("<tr>");
				people.append("<td bgcolor=\"white\" width=\"100%\">" + editLink + "</td>");
				people.append("<td align=\"center\" bgcolor=\"white\"><input type=\"checkbox\" name=\"peopleToRemove\" value=\"" + id + "\"/></td>");
				people.append("<td align=\"center\" bgcolor=\"white\">" + deleteLink + "</td>");
				people.append("</tr>");
			}
			currentItem++;
		}
		
		// Calculate number of pages, current page, page links...
		people.append("<tr><td colspan=\"2\" align=\"center\" class=\"subTableFooter\">");
		// --------------------------------------------------------------------------------
		// Create base URL for each page
		StringBuffer baseUrl = new StringBuffer();
		baseUrl.append("members.jsp?");
		if (listFilter.length() > 0) baseUrl.append("listFilterStr=").append(listFilter).append('&');
		
		String actionString = (lastaction.equals("edit")) ? "action=" + lastaction : "";
		String personIdString = (personid.length() > 0) ? "personid=" + personid : "";
		
		if (actionString.length() > 0) baseUrl.append(actionString);
		if (actionString.length() > 0 && personIdString.length() > 0) baseUrl.append("&");
		if (personIdString.length() > 0) baseUrl.append(personIdString);
		if (actionString.length() > 0 || personIdString.length() > 0) baseUrl.append("&");
		
		baseUrl.append("page=");
		people.append( createPageString( sortedAndFilteredKeys.size(), itemsToShowPerPage, currentPage, baseUrl.toString() ));
		people.append("</td></tr>");
		// --------------------------------------------------------------------------------
		people.append("</table>");
	}
	else
	{
		people.append("No user accounts were retrieved");
	}
	return people.toString();
}


%>
