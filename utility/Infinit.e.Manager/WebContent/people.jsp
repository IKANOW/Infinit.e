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
	
	//
	String action = "";
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
	// No way of knowing if I'm admin, so leave as visible for now, later change to:
	//String accountTypeHidden = "style=\"display:none;\"";
	String accountTypeHidden = "";
	
		
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
			String visiblePersonId = (request.getParameter("visiblePersonId") != null) ? request.getParameter("visiblePersonId") : "";
			personid = (request.getParameter("personid") != null) ? request.getParameter("personid") : "";
			firstName = (request.getParameter("firstName") != null) ? request.getParameter("firstName") : "";
			lastName = (request.getParameter("lastName") != null) ? request.getParameter("lastName") : "";
			displayName = (request.getParameter("displayName") != null) ? request.getParameter("displayName") : "";
			phone = (request.getParameter("phone") != null) ? request.getParameter("phone") : "";
			email = (request.getParameter("email") != null) ? request.getParameter("email") : "";
			oldemail = (request.getParameter("oldemail") != null) ? request.getParameter("oldemail") : "";
			passwordConfirmation = (request.getParameter("passwordConfirmation") != null) ? request.getParameter("passwordConfirmation") : "";
			password = (request.getParameter("password") != null) ? request.getParameter("password") : "";
			accounttype = (request.getParameter("accounttype") != null) ? request.getParameter("accounttype") : "";
			apiKey = (request.getParameter("apiKey") != null) ? request.getParameter("apiKey") : "";
			
			Boolean redirect = false;
			
			// If user has clicked save, create, or update buttons do those actions before handling the action param
			if (saveAccount.equals("saveaccount") || saveAccount.equals("save user account")) 
			{
				if ( validateFormFields() )
				{
					savePerson(false, request, response);
				}
			}
			if (createAccount.equals("createaccount") || createAccount.equals("create user account")) 
			{
				if ( validateFormFields() && validatePassword() )
				{
					savePerson(true, request, response);
					redirect = true;
				}
			}
			if (updatePassword.equals("updatepassword") || updatePassword.equals("update password")) 
			{
				if ( validatePassword() )
				{
					updateAccountPassword(request, response);
				}
			}
			if (addto.length() > 0)
			{
				addPersonToCommunity(personid, addto, request, response);
				redirect = true;
			}
			if (removefrom.length() > 0)
			{
				removePersonFromCommunity(personid, removefrom, request, response);
				redirect = true;
			}
			
			// if redirect == true refresh the page to update the edit form's content to reflect changes
			if (redirect) 
			{
				String urlParams = "";
				if (listFilter.length() > 0) urlParams = "&listFilterStr="+ listFilter;
				if (currentPage > 1) urlParams += "&page=" + currentPage;
				out.println("<meta http-equiv=\"refresh\" content=\"0;url=people.jsp?action=edit&personid=" 
					+ personid + urlParams + "\">");
			}
			
			if (action.equals("new user")) 
			{
				out.println("<meta http-equiv=\"refresh\" content=\"0;url=people.jsp\">");
			}
			else if (action.equals("edit")) 
			{
				populateEditForm(personid, request, response);
			}
			else if (action.equals("delete")) 
			{
				deleteAccount(personid, request, response);
				out.println("<meta http-equiv=\"refresh\" content=\"0;url=people.jsp\">");
			}
			else if (action.equals("filterlist")) 
			{
				currentPage = 1;
				populateEditForm(personid, request, response);
			}
			else if (action.equals("clear")) 
			{
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
				<td class="headerLink">People</td>
				<td align="right"><input type="text" id="listFilter" 
					onkeydown="if (event.keyCode == 13) { setDipatchAction('filterList'); 
					document.getElementById('filterList').click(); }" 
					name="listFilter" size="20" value="<%=listFilter %>"/><input name="filterList" type="submit"
					value="Filter"/><input name="clearFilter" value="Clear" type="submit"/></td>
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
				<td align="right"><input name="clearForm" id="clearForm" value="New User" type="submit"/></td>
			</tr>
			<tr>
				<td colspan="2" bgcolor="white">
					<table class="standardSubTable" cellpadding="5" cellspacing="1" width="100%">
					<tr>
						<td bgcolor="#ffffff" width="30%">Account Status:</td>
						<td bgcolor="#ffffff" width="70%"><%=accountStatus %></td>
					</tr>
					<tr>
						<td bgcolor="#ffffff" width="30%">Account Type (Admin Only):</td>
						<td bgcolor="#ffffff" width="70%">
							<select name="accounttype" id="accounttype" <%=accountTypeHidden %>>
								<option value="Unknown">Unknown</option>
								<option value="admin">Admin</option>
								<option value="user">User</option>
							</select>
						</td>							
					</tr>
					<tr>
						<td bgcolor="#ffffff" width="30%">Account Id:</td>
						<td bgcolor="#ffffff" width="70%"><input type="text" readonly id="personid" name="personid" value="<%=visiblePersonId %>" size="50" /></td>
					</tr>
					<tr>
						<td bgcolor="#ffffff" width="30%">First Name:*</td>
						<td bgcolor="#ffffff" width="70%"><input type="text" id="firstName" name="firstName" value="<%=firstName%>" size="50" /></td>
					</tr>
					<tr>
						<td bgcolor="#ffffff" width="30%">Last Name:*</td>
						<td bgcolor="#ffffff" width="70%"><input type="text" id="lastName" name="lastName" value="<%=lastName%>" size="50" /></td>
					</tr>
					<tr>
						<td bgcolor="#ffffff" width="30%">Display Name:*</td>
						<td bgcolor="#ffffff" width="70%"><input type="text" readonly id="displayName" name="displayName" value="<%=displayName%>" size="50" /></td>
					</tr>
					<tr>
						<td bgcolor="#ffffff" width="30%">Phone Number:*</td>
						<td bgcolor="#ffffff" width="70%"><input type="text" id="phone" name="phone" value="<%=phone%>" size="30" /></td>
					</tr>
					<tr>
						<td bgcolor="#ffffff" width="30%">Email Address (User Name):</td>
						<td bgcolor="#ffffff" width="70%"><input type="text" id="email" name="email" value="<%=email%>" size="75" /></td>
					</tr>
					<tr>
						<td bgcolor="#ffffff" width="30%">Password:</td>
						<td bgcolor="#ffffff" width="70%"><input type="password" id="password" name="password" value="<%=password%>" size="20" /></td>
					</tr>
					<tr>
						<td bgcolor="#ffffff" width="30%">Password Confirmation:</td>
						<td bgcolor="#ffffff" width="70%"><input type="password" id="passwordConfirmation" name="passwordConfirmation" value="<%=passwordConfirmation%>" size="20" /></td>
					</tr>
					<tr>
						<td bgcolor="#ffffff" width="30%">API key:</td>
						<td bgcolor="#ffffff" width="70%"><input type="password" id="apiKey" name="apiKey" value="" size="20" /></td>
					</tr>
					<tr valign="top">
						<td bgcolor="#ffffff" width="30%">Communities:</td>
						<td bgcolor="#ffffff" width="70%"><%=listOfCommunities %></td>
					</tr>
					<tr>
						<td bgcolor="#ffffff" width="30%"></td>
						<td bgcolor="#ffffff" width="70%">
<%
	if (personid.length() > 0) {
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
	<input type="hidden" name="oldemail" id="oldemail" value="<%=email%>"/>
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
	if (firstName.length() < 1) al.add("First Name");
	if (lastName.length() < 1) al.add("Last Name");
	if (email.length() < 1) al.add("Email");
	if (al.size() > 0)
	{
		isValid = false;
		messageToDisplay = "Error, the following required fields are missing: " + al.toString();
	}
	return isValid;
}  // TESTED


//validateFormFields
private boolean validatePassword()
{
	boolean isValid = true;
	ArrayList<String> al = new ArrayList<String>();
	if (password.length() < 6) al.add("Password must be greater than 5 characters in length");
	if (!password.equals(passwordConfirmation)) al.add("The Password and Password Confirmation fields must match");
	if (al.size() > 0)
	{
		isValid = false;
		messageToDisplay = "Error: " + al.toString();
		//System.out.print(messageToDisplay);
	}
	return isValid;
}  // TESTED



// savePerson -
private boolean savePerson( boolean isNewAccount, HttpServletRequest request, HttpServletResponse response )
{
	try
	{
		String newPassword = "";
		if (password.length() > 0 || isNewAccount)
		{
			if (validatePassword())
			{
				newPassword = ", \"password\" : \"" + password + "\"";
			}
			else
			{
				return false;
			}
		}
		String apiKeyJson = "";
		if (!apiKey.equals("")) {
			apiKeyJson = ", \"apiKey\" : \""+apiKey+"\"";
		}
		
		String accountType = "";
		if (!accounttype.equalsIgnoreCase("unknown")) {
			accountType = ", \"accountType\" : \""+accounttype+"\"";
		}
		else if (isNewAccount) { accountType = ", \"accountType\" : \"user\""; }
		
		if (null == phone) {
			phone = "";
		}
		String userJson = "" +
		"{" + 
		"    \"user\": " +
		"        {" +
		"            \"firstname\" : \"" + firstName + "\", " +
		"            \"lastname\" : \"" + lastName + "\", " +
		"            \"displayName\" : \"" + displayName + "\", " +
		"            \"phone\" : \"" + phone + "\", " +
		"            \"email\" : [ \"" + email + "\" ] " +
		"        }," +
		"    \"auth\" : { \"username\" : \"" + oldemail + "\" " + accountType + newPassword + apiKeyJson + " } " +
		"}";
		
		JSONObject actionResponse = null;
		if (isNewAccount)
		{
			actionResponse = new JSONObject(postToRestfulApi("social/person/register", userJson, request, response));
			JSONObject dataVal = new JSONObject(actionResponse.getString("data"));
			personid = dataVal.getString("_id");
		}
		else
		{
			actionResponse = new JSONObject(postToRestfulApi("social/person/update", userJson, request, response));
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


// updateAccountPassword -
private boolean updateAccountPassword( HttpServletRequest request, HttpServletResponse response )
{
	try
	{
		JSONObject updateResponse = new JSONObject ( new JSONObject ( updatePassword(email, Utils.encrypt(password), request, response) ).getString("response") );
		if (updateResponse.getString("success").equalsIgnoreCase("true"))
		{
			messageToDisplay = "Success: Password updated."; return true;
		}
		else
		{
			messageToDisplay = "Error: Unable to update password."; return false;
		}
	}
	catch (Exception e)
	{
		messageToDisplay = "Error: Unable to update password. (" + e.getMessage() + ")"; return false;
	}
}  // TESTED


//deleteAccount -
private boolean deleteAccount( String id, HttpServletRequest request, HttpServletResponse response )
{
	try
	{
		JSONObject updateResponse = new JSONObject ( new JSONObject ( deletePerson(id, request, response) ).getString("response") );
		if (updateResponse.getString("success").equalsIgnoreCase("true"))
		{
			messageToDisplay = "Success: Account Deleted."; return true;
		}
		else
		{
			messageToDisplay = "Error: Unable to Delete Account."; return false;
		}
	}
	catch (Exception e)
	{
		messageToDisplay = "Error: Unable to Delete Account. (" + e.getMessage() + ")"; return false;
	}
}  // TESTED



// addPersonToCommunity
private boolean addPersonToCommunity(String person, String community, HttpServletRequest request, HttpServletResponse response)
{
	try
	{
		JSONObject updateResponse = new JSONObject ( new JSONObject ( addToCommunity(community, person, request, response) ).getString("response") );
		if (updateResponse.getString("success").equalsIgnoreCase("true"))
		{
			messageToDisplay = "Success: Person added to community."; return true;
		}
		else
		{
			messageToDisplay = "Error: Unable to add person to community."; return false;
		}
	}
	catch (Exception e)
	{
		messageToDisplay = "Error: Unable to add person to community. (" + e.getMessage() + ")"; return false;
	}
}  // TESTED


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
			editTableTitle = "Edit User Account";
			
			// Get person from API
			JSONObject personResponse = new JSONObject( getPerson(id, request, response) );
			if (personResponse.has("data")) { // (otherwise trying to get someone else's info but aren't admin...)
				JSONObject person = personResponse.getJSONObject("data");
				String status = person.getString("accountStatus").substring(0,1).toUpperCase() + person.getString("accountStatus").substring(1);
				accountStatus =  status;
				visiblePersonId = id;
				firstName = person.getString("firstName");
				lastName = person.getString("lastName");
				displayName = person.getString("displayName");
				email = person.getString("email");
				if (person.has("phone")) phone = person.getString("phone");
				
				// Output user communities
				JSONArray communities = person.getJSONArray("communities");
				listOfCommunities = getListOfCommunities(communities, request, response);
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
			if (c.has("name"))
			{
				listOfCommunityNames.add(c.getString("name"));
			}
		}
		Collections.sort( listOfCommunityNames, String.CASE_INSENSITIVE_ORDER );
		
		int column = 1;
		
		for (String communityName : listOfCommunityNames)
		{
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
						if (listFilter.length() > 0) listFilterString = "&listFilterStr="+ listFilter;
						String pageString = "";
						if (currentPage > 1) pageString = "&page=" + currentPage;
						
						String deleteLink = "<a href=\"people.jsp?action=edit&personid=" + personid
								+ pageString + listFilterString + "&removefrom=" + community.getString("_id") 
								+ "\" title=\"Remove User from Community\" "
								+ "onclick='return confirm(\"Do you really wish to remove the user account from: "
								+ community.getString("name") + "?\");'><img src=\"image/minus_button.png\" border=0></a>";
								
						String addLink = "<a href=\"people.jsp?action=edit&personid=" + personid
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
private void clearForm()
{
	editTableTitle = "Add New User Account";
	visiblePersonId = "";
	accountStatus =  "";
	firstName = "";
	lastName = "";
	displayName = "";
	email = "";
	oldemail = "";
	phone = "";
	password = "";
	passwordConfirmation = "";
	listOfCommunities = "";
}  // TESTED


// listItems -
private String listItems(HttpServletRequest request, HttpServletResponse response)
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
				
				editLink = "<a href=\"people.jsp?action=edit&personid=" + id + "&page=" + currentPage 
						+ listFilterString + "\" title=\"Edit User Account\">" + name + "</a>";
				deleteLink = "<a href=\"people.jsp?action=delete&personid=" + id
						+ listFilterString + "\" title=\"Delete User Account\" "
						+ "onclick='return confirm(\"Do you really wish to delete the user account for: "
						+ name + "?\");'><img src=\"image/delete_x_button.png\" border=0></a>";
	
				// Create the HTML table row
				people.append("<tr>");
				people.append("<td bgcolor=\"white\" width=\"100%\">" + editLink + "</td>");
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
		String actionString = (action.length() > 0) ? "action=" + action : "";
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
