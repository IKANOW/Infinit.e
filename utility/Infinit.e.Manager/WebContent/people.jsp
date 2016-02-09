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
		
		//
		String action = "";
		String lastaction = "";
		String logoutAction = "";
		
		// 
		String listFilter = "";
	
		//
		String editTableTitle = "Register New User Account";
			
		//
		String personid = "";
		String visiblePersonId = "";
		String firstName = "";
		String lastName = "";
		String displayName = "";
		String accountStatus = "";
		String apiKey = "";
		String phone = "";
		String email = "";
		String oldemail = "";
		String accounttype = "";
		String listOfCommunities = "";
		String password = "";
		String passwordConfirmation = "";
		String accountTypeHidden = "style=\"display:none;\"";
		String isRequestAdminVisible = "style=\"display:none;\"";
		String isDemoteAdminVisible = "style=\"display:none;\"";
		String messageToDisplay = "";
	}		
%>

<%
	Session session = new Session();

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
		session.lastaction = session.action;
		if (request.getParameter("dispatchAction") != null) session.action = request.getParameter("dispatchAction").toLowerCase();
		if (request.getParameter("clearForm") != null) session.action = request.getParameter("clearForm").toLowerCase();
		if (request.getParameter("filterList") != null) session.action = request.getParameter("filterList").toLowerCase();
		if (request.getParameter("clearFilter") != null) session.action = request.getParameter("clearFilter").toLowerCase();
		if (request.getParameter("logoutButton") != null) session.action = request.getParameter("logoutButton").toLowerCase();
		if (request.getParameter("deleteSelected") != null) session.action = request.getParameter("deleteSelected").toLowerCase();
		
		// Capture values sent by button clicks, these will override the session.action value as appropriate 
		String saveAccount = "";
		String createAccount = "";
		String updatePassword = "";
		String addto = "";
		String removefrom = "";
		boolean bRequestAdmin = false;
		boolean bDemoteAdmin = false;
		if (request.getParameter("createAccount") != null) createAccount = request.getParameter("createAccount").toLowerCase();
		if (request.getParameter("saveAccount") != null) saveAccount = request.getParameter("saveAccount").toLowerCase();
		if (request.getParameter("updatePassword") != null) updatePassword = request.getParameter("updatePassword").toLowerCase();
		if (request.getParameter("addto") != null) addto = request.getParameter("addto");
		if (request.getParameter("removefrom") != null) removefrom = request.getParameter("removefrom");
		if (request.getParameter("requestAdmin") != null) bRequestAdmin = true;
		if (request.getParameter("demoteAdmin") != null) bDemoteAdmin = true;

		// Capture input for page value if passed to handle the page selected in the left hand list of items
		if (request.getParameter("page") != null) 
		{
			session.currentPage = Integer.parseInt( request.getParameter("page").toLowerCase() );
		}
		else
		{
			session.currentPage = 1;
		}
		
		Boolean bIsAdmin = null;
		
		if (bDemoteAdmin) {
			adminLogOut(request, response);
		}
		bIsAdmin = isLoggedInAsAdmin_GetAdmin(bRequestAdmin, request, response);
		if (null == bIsAdmin) { //inactive admin
			session.accountTypeHidden = "style=\"display:none;\"";
			session.isRequestAdminVisible = "";			
			session.isDemoteAdminVisible = "style=\"display:none;\"";
		}
		else if (bIsAdmin) { // admin active
			session.accountTypeHidden = "";
			session.isRequestAdminVisible = "style=\"display:none;\"";	
			session.isDemoteAdminVisible = "";
		}
		else { // not admin
			session.accountTypeHidden = "style=\"display:none;\"";
			session.isRequestAdminVisible = "style=\"display:none;\"";
			session.isDemoteAdminVisible = "style=\"display:none;\"";
		}
		
		try
		{
			// Always clear the form first so there is no bleed over of values from previous requests
			clearForm(session);

			// Read in values from the edit form
			String visiblePersonId = (request.getParameter("visiblePersonId") != null) ? request.getParameter("visiblePersonId") : "";
			session.personid = (request.getParameter("session.personid") != null) ? request.getParameter("session.personid") : "";
			session.firstName = (request.getParameter("firstName") != null) ? request.getParameter("firstName") : "";
			session.lastName = (request.getParameter("lastName") != null) ? request.getParameter("lastName") : "";
			session.displayName = (request.getParameter("displayName") != null) ? request.getParameter("displayName") : "";
			session.phone = (request.getParameter("phone") != null) ? request.getParameter("phone") : "";
			session.email = (request.getParameter("email") != null) ? request.getParameter("email") : "";
			session.oldemail = (request.getParameter("oldemail") != null) ? request.getParameter("oldemail") : "";
			session.passwordConfirmation = (request.getParameter("passwordConfirmation") != null) ? request.getParameter("passwordConfirmation") : "";
			session.password = (request.getParameter("password") != null) ? request.getParameter("password") : "";
			session.accounttype = (request.getParameter("accounttype") != null) ? request.getParameter("accounttype") : "";
			session.apiKey = (request.getParameter("apiKey") != null) ? request.getParameter("apiKey") : "";
			
			Boolean redirect = false;
			
			// If user has clicked save, create, or update buttons do those actions before handling the session.action param
			if (saveAccount.equals("saveaccount") || saveAccount.equals("save user account")) 
			{
				if ( validateFormFields(session) )
				{
					savePerson(false, request, response, session);
				}
			}
			if (createAccount.equals("createaccount") || createAccount.equals("create user account")) 
			{
				if ( validateFormFields(session) && validatePassword(session) )
				{
					savePerson(true, request, response, session);
					redirect = true;
				}
			}
			if (updatePassword.equals("updatepassword") || updatePassword.equals("update password")) 
			{
				if ( validatePassword(session) )
				{
					updateAccountPassword(request, response, session);
				}
			}
			if (addto.length() > 0)
			{
				addPersonToCommunity(session.personid, addto, request, response, session);
				redirect = true;
			}
			if (removefrom.length() > 0)
			{
				removePersonFromCommunity(session.personid, removefrom, request, response, session);
				redirect = true;
			}
			
			// if redirect == true refresh the page to update the edit form's content to reflect changes
			if (redirect) 
			{
				String urlParams = "";
				if (session.listFilter.length() > 0) urlParams = "&listFilterStr="+ session.listFilter;
				if (session.currentPage > 1) urlParams += "&page=" + session.currentPage;
				out.println("<meta http-equiv=\"refresh\" content=\"0;url=people.jsp?session.action=edit&session.personid=" 
					+ session.personid + urlParams + "\">");
			}
			
			if (session.action.equals("new user")) 
			{
				out.println("<meta http-equiv=\"refresh\" content=\"0;url=people.jsp\">");
			}
			else if (session.action.equals("edit")) 
			{
				populateEditForm(session.personid, request, response, session);
			}
			else if (session.action.equals("delete")) 
			{
				deleteAccount(session.personid, request, response, session);
				out.print("<meta http-equiv=\"refresh\" content=\"0;url=people.jsp?page=" + session.currentPage);
				if (session.listFilter.length() > 0) {
					out.print("&listFilterStr=" + session.listFilter);					
				}
				out.println("\">");
			}
			else if (session.action.equals("deleteselected")) 
			{
				String[] ids= request.getParameterValues("peopleToDelete");
				
				int nDeleted = 0;
				int nFailed = 0;
				for (String id: ids) {
					if (!deleteAccount(id, request, response, session)) {
						nFailed++;
					}
					else nDeleted++;
				}
				session.messageToDisplay = "Bulk person deletion: deleted " + nDeleted + ", failed: " + nFailed; 
				
				out.print("<meta http-equiv=\"refresh\" content=\"0;url=people.jsp?page=" + session.currentPage);
				if (session.listFilter.length() > 0) {
					out.print("&listFilterStr=" + session.listFilter);					
				}
				out.println("\">");
			}
			else if (session.action.equals("filterlist")) 
			{
				session.currentPage = 1; // (don't perpetuate this session.action across page jumps)
				populateEditForm(session.personid, request, response, session);
			}
			else if (session.action.equals("clear")) 
			{
				session.currentPage = 1; // (don't perpetuate this session.action across page jumps)
				session.listFilter = "";
				populateEditForm(session.personid, request, response, session);
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
	<title>Infinit.e.Manager - People</title>
</head>
<body>

<%
	// !-- Create JavaScript Popup --
	if (session.messageToDisplay.length() > 0) { 
%>
	<script language="javascript" type="text/javascript">
		alert("<%=session.messageToDisplay.replace('"', '\'') %>");
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
				<td class="headerLink">People</td>
				<td align="right"><input type="text" id="session.listFilter" 
					onkeydown="if (event.keyCode == 13) { setDipatchAction('filterList'); 
					document.getElementById('filterList').click(); }" 
					name="session.listFilter" size="20" value="<%=session.listFilter %>"/><input name="filterList" type="submit"
					value="Filter"/><input name="clearFilter" value="Clear" type="submit"/></td>
			</tr>
			<tr>
				<td colspan="2" bgcolor="white"><%=listItems(request, response, session) %></td>
			</tr>
			<tr>
				<td colspan="2" >
				<button name="deleteSelected" onclick="return confirm('Do you really wish to delete the selected people?');" name="deleteSelected" value="deleteSelected">Delete selected people</button>
				<input type="checkbox" name="selectall" onchange="var cbs = document.getElementsByName('peopleToDelete'); for(var i=0; i < cbs.length; i++) if(cbs[i].type == 'checkbox') cbs[i].checked=selectall.checked" value=""></input>
				</td>
			</tr>
			<tr <%= session.isRequestAdminVisible %>>
				<td colspan="2" ><button name="requestAdmin" name="requestAdmin" value="requestAdmin">Grab temp admin rights</button></td>
			</tr>
			<tr  <%= session.isDemoteAdminVisible %> >
				<td colspan="2" ><button name="demoteAdmin" name="demoteAdmin" value="demoteAdmin">Relinquish temp admin rights</button></td>
			</tr>			
			</table>

		</td>
		
		<td width="70%" bgcolor="#ffffff">
		
			<table class="standardTable" cellpadding="5" cellspacing="1" width="100%">
			<tr>
				<td class="headerLink"><%=session.editTableTitle %></td>
				<td align="right"><input name="clearForm" id="clearForm" value="New User" type="submit"/></td>
			</tr>
			<tr>
				<td colspan="2" bgcolor="white">
					<table class="standardSubTable" cellpadding="5" cellspacing="1" width="100%">
					<tr>
						<td bgcolor="#ffffff" width="30%">Account Status:</td>
						<td bgcolor="#ffffff" width="70%"><%=session.accountStatus %></td>
					</tr>
					<tr <%=session.accountTypeHidden %>>
						<td bgcolor="#ffffff" width="30%">Account Type (Admin Only):</td>
						<td bgcolor="#ffffff" width="70%">
							<select name="accounttype" id="accounttype">
								<option value="Unknown">Unknown</option>
								<option value="admin" <%if(session.accounttype.equals("admin")) out.print("selected"); %>>Admin</option>
								<option value="admin-enabled" <%if(session.accounttype.equals("admin-enabled")) out.print("selected"); %>>Admin-On-Request</option>
								<option value="user" <%if(session.accounttype.equals("user")) out.print("selected"); %>>User</option>							
							</select>
						</td>							
					</tr>
					<tr>
						<td bgcolor="#ffffff" width="30%">Account Id:</td>
						<td bgcolor="#ffffff" width="70%"><input type="text" readonly id="session.personid" name="session.personid" value="<%=session.visiblePersonId %>" size="50" /></td>
					</tr>
					<tr>
						<td bgcolor="#ffffff" width="30%">First Name:*</td>
						<td bgcolor="#ffffff" width="70%"><input type="text" id="firstName" name="firstName" value="<%=session.firstName%>" size="50" /></td>
					</tr>
					<tr>
						<td bgcolor="#ffffff" width="30%">Last Name:*</td>
						<td bgcolor="#ffffff" width="70%"><input type="text" id="lastName" name="lastName" value="<%=session.lastName%>" size="50" /></td>
					</tr>
					<tr>
						<td bgcolor="#ffffff" width="30%">Display Name:</td>
						<td bgcolor="#ffffff" width="70%"><input type="text" readonly id="displayName" name="displayName" value="<%=session.displayName%>" size="50" /></td>
					</tr>
					<tr>
						<td bgcolor="#ffffff" width="30%">Phone Number:</td>
						<td bgcolor="#ffffff" width="70%"><input type="text" id="phone" name="phone" value="<%=session.phone%>" size="30" /></td>
					</tr>
					<tr>
						<td bgcolor="#ffffff" width="30%">Email Address (User Name):</td>
						<td bgcolor="#ffffff" width="70%"><input type="text" id="email" name="email" value="<%=session.email%>" size="75" /></td>
					</tr>
					<tr>
						<td bgcolor="#ffffff" width="30%">Password:</td>
						<td bgcolor="#ffffff" width="70%"><input type="password" id="password" name="password" autocomplete="off" value="<%=session.password%>" size="20" /></td>
					</tr>
					<tr>
						<td bgcolor="#ffffff" width="30%">Password Confirmation:</td>
						<td bgcolor="#ffffff" width="70%"><input type="password" id="passwordConfirmation" name="passwordConfirmation" value="<%=session.passwordConfirmation%>" size="20" /></td>
					</tr>
					<tr>
						<td bgcolor="#ffffff" width="30%">API key:</td>
						<td bgcolor="#ffffff" width="70%"><input type="password" id="apiKey" name="apiKey" value="" size="20" /></td>
					</tr>
					<tr valign="top">
						<td bgcolor="#ffffff" width="30%">Communities:</td>
						<td bgcolor="#ffffff" width="70%"><%=session.listOfCommunities %></td>
					</tr>
					<tr>
						<td bgcolor="#ffffff" width="30%"></td>
						<td bgcolor="#ffffff" width="70%">
<%
	if (session.personid.length() > 0) {
%>
							<input name="saveAccount" id="saveAccount" value="Save User Account" type="submit"
									onclick="if (confirm('Are you sure you want to change these account details?'))  return true; return false;"
							/>
							
							<input name="updatePassword" value="Update Password" type="submit"
									onclick="if (confirm('Are you sure you want to update this password?'))  return true; return false;"
							/>
<%
	}
	else
	{
%>
							<input name="createAccount" value="Create User Account" type="submit"
									onclick="if (confirm('Are you sure you want to create this user account?'))  return true; return false;"
							/>	
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
	<input type="hidden" name="oldemail" id="oldemail" value="<%=session.email%>"/>
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
	if (session.firstName.length() < 1) al.add("First Name");
	if (session.lastName.length() < 1) al.add("Last Name");
	if (session.email.length() < 1) al.add("Email");
	if (al.size() > 0)
	{
		isValid = false;
		session.messageToDisplay = "Error, the following required fields are missing: " + al.toString();
	}
	return isValid;
}  // TESTED


//validateFormFields
private boolean validatePassword(Session session)
{
	boolean isValid = true;
	ArrayList<String> al = new ArrayList<String>();
	if (session.password.length() < 6) al.add("Password must be greater than 5 characters in length");
	if (!session.password.equals(session.passwordConfirmation)) al.add("The Password and Password Confirmation fields must match");
	if (al.size() > 0)
	{
		isValid = false;
		session.messageToDisplay = "Error: " + al.toString();
		//System.out.print(session.messageToDisplay);
	}
	return isValid;
}  // TESTED



// savePerson -
private boolean savePerson( boolean isNewAccount, HttpServletRequest request, HttpServletResponse response, Session session )
{
	try
	{
		String newPassword = "";
		if (session.password.length() > 0 || isNewAccount)
		{
			if (validatePassword(session))
			{
				newPassword = ", \"password\" : \"" + session.password + "\"";
			}
			else
			{
				return false;
			}
		}
		String apiKeyJson = "";
		if (!session.apiKey.equals("")) {
			apiKeyJson = ", \"apiKey\" : \""+session.apiKey+"\"";
		}
		
		String accountType = "";
		if (!session.accounttype.equalsIgnoreCase("unknown")) {
			accountType = ", \"accountType\" : \""+session.accounttype+"\"";
		}
		else if (isNewAccount) { accountType = ", \"accountType\" : \"user\""; }
		
		if (null == session.phone) {
			session.phone = "";
		}
		String userJson = "" +
		"{" + 
		"    \"user\": " +
		"        {" +
		"            \"firstname\" : \"" + session.firstName + "\", " +
		"            \"lastname\" : \"" + session.lastName + "\", " +
		"            \"displayName\" : \"" + session.displayName + "\", " +
		"            \"phone\" : \"" + session.phone + "\", " +
		"            \"email\" : [ \"" + session.email + "\" ] " +
		"        }," +
		"    \"auth\" : { \"username\" : \"" + session.oldemail + "\" " + accountType + newPassword + apiKeyJson + " } " +
		"}";
		
		JSONObject actionResponse = null;
		if (isNewAccount)
		{
			actionResponse = new JSONObject(postToRestfulApi("social/person/register", userJson, request, response));
			JSONObject dataVal = new JSONObject(actionResponse.getString("data"));
			session.personid = dataVal.getString("_id");
		}
		else
		{
			actionResponse = new JSONObject(postToRestfulApi("social/person/update", userJson, request, response));
		}
		JSONObject responseVal = new JSONObject(actionResponse.getString("response"));
		
		if (responseVal.getString("success").equalsIgnoreCase("true"))
		{
			session.messageToDisplay = "Success: User account information saved."; return true;
		}
		else
		{
			session.messageToDisplay = "Error: Unable to save the user's account information. (" + responseVal.getString("message") + ")"; 
			return false;
		}
	}
	catch (Exception e)
	{
		session.messageToDisplay = "Error: Unable to the save user's account information. (" + e.getMessage() + ")"; return false;
	}
} // TESTED


// updateAccountPassword -
private boolean updateAccountPassword( HttpServletRequest request, HttpServletResponse response, Session session )
{
	try
	{
		JSONObject updateResponse = new JSONObject ( new JSONObject ( updatePassword(session.email, Utils.encrypt(session.password), request, response) ).getString("response") );
		if (updateResponse.getString("success").equalsIgnoreCase("true"))
		{
			session.messageToDisplay = "Success: Password updated."; return true;
		}
		else
		{
			session.messageToDisplay = "Error: Unable to update password."; return false;
		}
	}
	catch (Exception e)
	{
		session.messageToDisplay = "Error: Unable to update password. (" + e.getMessage() + ")"; return false;
	}
}  // TESTED


//deleteAccount -
private boolean deleteAccount( String id, HttpServletRequest request, HttpServletResponse response, Session session )
{
	try
	{
		JSONObject updateResponse = new JSONObject ( new JSONObject ( deletePerson(id, request, response) ).getString("response") );
		if (updateResponse.getString("success").equalsIgnoreCase("true"))
		{
			session.messageToDisplay = "Success: Account Deleted."; return true;
		}
		else
		{
			session.messageToDisplay = "Error: Unable to Delete Account."; return false;
		}
	}
	catch (Exception e)
	{
		session.messageToDisplay = "Error: Unable to Delete Account. (" + e.getMessage() + ")"; return false;
	}
}  // TESTED



// addPersonToCommunity
private boolean addPersonToCommunity(String person, String community, HttpServletRequest request, HttpServletResponse response, Session session)
{
	try
	{
		JSONObject updateResponse = new JSONObject ( new JSONObject ( addToCommunity(community, person, request, response) ).getString("response") );
		if (updateResponse.getString("success").equalsIgnoreCase("true"))
		{
			// Don't output a message, the visual feedback is sufficient
			//session.messageToDisplay = "Success: Person added to community."; 
			return true;
		}
		else
		{
			session.messageToDisplay = "Error: Unable to add person to community."; return false;
		}
	}
	catch (Exception e)
	{
		session.messageToDisplay = "Error: Unable to add person to community. (" + e.getMessage() + ")"; return false;
	}
}  // TESTED


// removePersonFromCommunity
private boolean removePersonFromCommunity(String person, String community, HttpServletRequest request, HttpServletResponse response, Session session)
{
	try
	{
		JSONObject updateResponse = new JSONObject ( new JSONObject ( removeFromCommunity(community, person, request, response) ).getString("response") );
		if (updateResponse.getString("success").equalsIgnoreCase("true"))
		{
			// Don't output a message, the visual feedback is sufficient
			//session.messageToDisplay = "Success: Person removed from community."; 
			return true;
		}
		else
		{
			session.messageToDisplay = "Error: Unable to remove person from community."; return false;
		}
	}
	catch (Exception e)
	{
		session.messageToDisplay = "Error: Unable to remove person from community. (" + e.getMessage() + ")"; return false;
	}
}





// populateEditForm - 
private void populateEditForm(String id, HttpServletRequest request, HttpServletResponse response, Session session) 
{
	clearForm(session);
	if (id != null && id != "") 
	{
		try 
		{
			session.editTableTitle = "Edit User Account";
			
			// Get person from API
			JSONObject personResponse = new JSONObject( getPerson(id, request, response) );
			if (personResponse.has("data")) { // (otherwise trying to get someone else's info but aren't admin...)
				JSONObject person = personResponse.getJSONObject("data");
				String status = person.getString("accountStatus").substring(0,1).toUpperCase() + person.getString("accountStatus").substring(1);
				session.accountStatus =  status;
				session.visiblePersonId = id;
				session.firstName = person.getString("firstName");
				session.lastName = person.getString("lastName");
				session.displayName = person.getString("displayName");
				session.email = person.getString("email");
				session.accounttype = person.getString("accountType").toLowerCase();
				if (person.has("phone")) session.phone = person.getString("phone");
				
				// Output user communities
				JSONArray communities = person.getJSONArray("communities");
				session.listOfCommunities = getListOfCommunities(communities, request, response, session);
			}
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
private String getListOfCommunities(JSONArray memberOf, HttpServletRequest request, HttpServletResponse response, Session session)
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
			if (c.has("name"))
			{
				listOfCommunityNames.add(c.getString("name"));
			}
		}
		Collections.sort( listOfCommunityNames, String.CASE_INSENSITIVE_ORDER );
		
		int column = 1;
		
		String lastName = null;
		for (String communityName : listOfCommunityNames)
		{
			if ((null != lastName) && lastName.equals(communityName)) {
				continue;
			}
			lastName = communityName;
			
			// Iterate over the list of all communities
			for (int i = 0; i < communities.length(); i++)
			{
				JSONObject community = communities.getJSONObject(i);
				if (community.has("name") && community.getString("name").equalsIgnoreCase(communityName.toLowerCase()))
				{
					// Only show the non-system, non-personal communities
					if (community.getString("isPersonalCommunity").equalsIgnoreCase("false") && community.getString("isSystemCommunity").equalsIgnoreCase("false"))
					{
						if (column == 1) { communityList.append("<tr valign=\"middle\">"); }
						communityList.append("<td width=\"5%\">");
						
						String listFilterString = "";
						if (session.listFilter.length() > 0) listFilterString = "&listFilterStr="+ session.listFilter;
						String pageString = "";
						if (session.currentPage > 1) pageString = "&page=" + session.currentPage;
						
						String deleteLink = "<a href=\"people.jsp?session.action=edit&session.personid=" + session.personid
								+ pageString + listFilterString + "&removefrom=" + community.getString("_id") 
								+ "\" title=\"Remove User from Community\" >"
								+ "<img src=\"image/minus_button.png\" border=0></a>";
								
						String addLink = "<a href=\"people.jsp?session.action=edit&session.personid=" + session.personid
								+ pageString + listFilterString + "&addto=" + community.getString("_id") 
								+ "\" title=\"Add User to Community\"><img src=\"image/plus_button.png\" border=0></a>";
						
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
		System.out.println(e.getMessage());
	}
	communityList.append("</table>");
	return communityList.toString();
}



// clearForm
private void clearForm(Session session)
{
	session.editTableTitle = "Add New User Account";
	session.visiblePersonId = "";
	session.accountStatus =  "";
	session.firstName = "";
	session.lastName = "";
	session.displayName = "";
	session.email = "";
	session.oldemail = "";
	session.phone = "";
	session.password = "";
	session.passwordConfirmation = "";
	session.listOfCommunities = "";
	session.accounttype = "";
}  // TESTED


// listItems -
private String listItems(HttpServletRequest request, HttpServletResponse response, Session session)
{
	StringBuffer people = new StringBuffer();
	Map<String, String> listOfPeople = getListOfAllPeople(request, response);
	
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
			if (currentItem >= startItem && currentItem <= endItem)
			{
				String name = key;
				String id = listOfPeople.get(key).toString();
				String editLink = "";
				String deleteLink = "";
	
				String listFilterString = "";
				if (session.listFilter.length() > 0) listFilterString = "&listFilterStr="+ session.listFilter;
				
				editLink = "<a href=\"people.jsp?session.action=edit&session.personid=" + id + "&page=" + session.currentPage 
						+ listFilterString + "\" title=\"Edit User Account\">" + name + "</a>";
				deleteLink = "<a href=\"people.jsp?session.action=delete&session.personid=" + id+ "&page=" + session.currentPage
						+ listFilterString + "\" title=\"Delete User Account\" "
						+ "onclick='return confirm(\"Do you really wish to delete the user account for: "
						+ name + "?\");'><img src=\"image/delete_x_button.png\" border=0></a>";
	
				// Create the HTML table row
				people.append("<tr>");
				people.append("<td bgcolor=\"white\" width=\"100%\">" + editLink + "</td>");
				people.append("<td align=\"center\" bgcolor=\"white\"><input type=\"checkbox\" name=\"peopleToDelete\" value=\"" + id + "\"/></td>");
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
		baseUrl.append("people.jsp?");
		if (session.listFilter.length() > 0) baseUrl.append("listFilterStr=").append(session.listFilter).append('&');
		
		String actionString = (session.lastaction.equals("edit")) ? "session.action=" + session.lastaction : "";
		String personIdString = (session.personid.length() > 0) ? "session.personid=" + session.personid : "";
		
		if (actionString.length() > 0) baseUrl.append(actionString);
		if (actionString.length() > 0 && personIdString.length() > 0) baseUrl.append("&");
		if (personIdString.length() > 0) baseUrl.append(personIdString);
		if (actionString.length() > 0 || personIdString.length() > 0) baseUrl.append("&");
		
		baseUrl.append("page=");
		people.append( createPageString( sortedAndFilteredKeys.size(), session.itemsToShowPerPage, session.currentPage, baseUrl.toString() ));
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
