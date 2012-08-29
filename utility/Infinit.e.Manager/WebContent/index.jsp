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

<%
	messageToDisplay = "";

	// 
	if (isLoggedIn) 
	{	
		// Determine which action is being called for by the user
		String action = "";
		if (request.getParameter("action") != null) action = request.getParameter("action").toLowerCase();
		if (request.getParameter("dispatchAction") != null) action = request.getParameter("dispatchAction").toLowerCase();
		
		try
		{
			if (action.equals("logout")) 
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
	<title>Infinit.e.Manager - Home</title>
</head>
<body>

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
	<table class="standardTable" cellpadding="5" cellspacing="1" width="100%" >
	<tr>
		<td width="100%" bgcolor="#ffffff">
			<br />
			<br />
			
			<center>
			<table class="standardTable" cellpadding="5" cellspacing="1" width="50%">
				<tr>
					<td>&nbsp</td>
				</tr>
				<tr>
					<td bgcolor="white">
						<ul>
							<li><b><a href="people.jsp" title="Add/Edit Users">People</a></b> - Add/Edit Users</li>
							<li><b><a href="communities.jsp" title="Add/Edit Users">Communities</a></b> - Add/Edit Communities and Membership</li>
							<li><b><a href="sources.jsp" title="Add/Edit Users">Sources</a></b> - Add/Edit Sources</li>
						</ul>
						<ul>
							<li><b><a href="fileUploader.jsp" title="Add/Edit Users" target="_blank">File Uploader</a></b> - Add/Edit Files or JSON</li>
							<li><b><a href="widgetUploader.jsp" title="Add/Edit Users" target="_blank">Widget Uploader</a></b> - Add/Edit Widgets</li>
							<li><b><a href="pluginManager.jsp" title="Add/Edit Users" target="_blank">Plugin Manager</a></b> - Add/Edit Hadoop Plugins</li>						
						</ul>
						<ul>
							<li><b><a href="chrome.html" title="Install Chrome Source Extension" target="_blank">Infinit.e Chrome Extension</a></b> - Create Sources from Chrome</li>
						</ul>
					</td>
				</tr>
			</table>
			</center>

			<br />
			<br />
			<br />
			<br />
			<br />
			<br />
		</td>
	<tr>
	</table>
<%
	}
%>

<%@ include file="inc/footer.jsp" %>

</body>
</html>