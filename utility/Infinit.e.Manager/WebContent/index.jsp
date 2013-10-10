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
<html lang=${language}>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
	<link rel="stylesheet" type="text/css" href="inc/manager.css" />
	<title><fmt:message key='index.title'/></title>
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
							<li><b><a href="people.jsp" title="<fmt:message key='index.people.description'/>"><fmt:message key='index.people.title' /></a></b> - <fmt:message key='index.people.description'/></li>
							<li><b><a href="communities.jsp" title="<fmt:message key='index.communities.description'/>"><fmt:message key='index.communities.title' /></a></b> - <fmt:message key='index.communities.description'/></li>
							<li><b><a href="sources.jsp" title="<fmt:message key='index.source_editor.description'/>"><fmt:message key='index.source_editor.title' /></a></b> - <fmt:message key='index.source_editor.description'/>
								<ul><li><b><a href="sourcemonitor.jsp" title="<fmt:message key='index.source_monitor.description'/>" target="_blank"><fmt:message key='index.source_monitor.title' /></a></b> - <fmt:message key='index.source_monitor.description'/></li></ul>
							</li>
						</ul>
						<ul>
							<li><b><a href="fileUploader.jsp" title="<fmt:message key='index.file_uploader.description'/>" target="_blank"><fmt:message key='index.file_uploader.title'/></a></b> - <fmt:message key='index.file_uploader.description'/></li>
							<li><b><a href="widgetUploader.jsp" title="<fmt:message key='index.widget_uploader.description'/>" target="_blank"><fmt:message key='index.widget_uploader.title'/></a></b> - <fmt:message key='index.widget_uploader.description'/></li>
							<li><b><a href="pluginManager.jsp" title="<fmt:message key='index.plugin_manager.description'/>" target="_blank"><fmt:message key='index.plugin_manager.title'/></a></b> - <fmt:message key='index.plugin_manager.description'/></li>						
						</ul>
						<ul>
							<li><b><a href="chrome.html" title="<fmt:message key='index.chrome_extension.description'/>" target="_blank"><fmt:message key='index.chrome_extension.title'/></a></b> - <fmt:message key='index.chrome_extension.description'/></li>
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