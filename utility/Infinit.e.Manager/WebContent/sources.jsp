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
	int itemsToShowPerPage = 18;
	String action = "";
	String logoutAction = "";
	String listFilter = "";

	//
	String shareid = "";
	String sourceid = "";
	String sourceShowRss = "style=\"display: none\";";
	String formShareId = "";
	String shareJson = "";
	String sourceJson = "";
	String enableOrDisable = "Disable Source";
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
	String getTestUpdateLogic = "";
	String getTestUpdateLogicChecked = "";
	String numberOfDocuments = "";
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
		
		if (request.getParameter("testSource") != null) action = "testSource";
		if (request.getParameter("saveSource") != null) action = "saveSource";
		if (request.getParameter("saveSourceAsTemplate") != null) action = "saveSourceAsTemplate";
		if (request.getParameter("publishSource") != null) action = "publishSource";
		if (request.getParameter("deleteDocs") != null) action = "deleteDocs";
		if (request.getParameter("newSource") != null) action = "newSource";
		
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
			formShareId = (request.getParameter("shareId") != null) ? request.getParameter("shareId") : "";
			sourceid = (request.getParameter("sourceid") != null) ? request.getParameter("sourceid") : "";
			communityId = (request.getParameter("Community_ID") != null) ? request.getParameter("Community_ID") : "";
			shareTitle = (request.getParameter("shareTitle") != null) ? request.getParameter("shareTitle") : "";
			shareTitle = org.apache.commons.lang.StringEscapeUtils.unescapeHtml(shareTitle);
			shareDescription = (request.getParameter("shareDescription") != null) ? request.getParameter("shareDescription") : "";
			sourceJson = (request.getParameter("Source_JSON") != null) ? request.getParameter("Source_JSON") : "";
			selectedSourceTemplate = (request.getParameter("sourceTemplateSelect") != null) ? request.getParameter("sourceTemplateSelect") : "";
			numberOfDocuments = (request.getParameter("numOfDocs") != null) ? request.getParameter("numOfDocs") : "10";
			getFullText = (request.getParameter("fullText") != null) ? "true" : "false";
			getFullTextChecked = (getFullText.equalsIgnoreCase("true")) ? "CHECKED" : "";
			getTestUpdateLogic = (request.getParameter("testUpdateLogic") != null) ? "true" : "false";
			getTestUpdateLogicChecked = (getTestUpdateLogic.equalsIgnoreCase("true")) ? "CHECKED" : "";
			
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
				String listFilterString = "";
				if (listFilter.length() > 0) listFilterString = "?listFilterStr="+ listFilter;
				out.println("<meta http-equiv=\"refresh\" content=\"0;url=sources.jsp" + listFilterString + "\">");
			}
			else if (action.equals("deletesource")) 
			{
				deleteSourceObject(sourceid, false, request, response);
				String listFilterString = "";
				if (listFilter.length() > 0) listFilterString = "?listFilterStr="+ listFilter;
				out.println("<meta http-equiv=\"refresh\" content=\"0;url=sources.jsp" + listFilterString + "\">");
			}
			else if (action.equals("filterlist")) 
			{
				currentPage = 1;
				populateEditForm(shareid, request, response);
			}
			else if (action.equals("clearfilter")) 
			{
				currentPage = 1;
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
				String listFilterString = "";
				if (listFilter.length() > 0) listFilterString = "?listFilterStr="+ listFilter;
				out.println("<meta http-equiv=\"refresh\" content=\"0;url=sources.jsp" + listFilterString + "\">");
			}
			else if (action.equals("deleteDocs")) 
			{
				deleteSourceObject(sourceid, true, request, response);
				String listFilterString = "";
				if (listFilter.length() > 0) listFilterString = "?listFilterStr="+ listFilter;
				out.println("<meta http-equiv=\"refresh\" content=\"0;url=sources.jsp" + listFilterString + "\">");
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
			else {
				populateEditForm(shareid, request, response);				
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
	
   <script src="lib/jquery.js"></script>
   <script src="lib/jquery.cookie.js"></script>
   
    <script src="lib/splitter.js"></script>
    
   	<script type="text/javascript" src="lib/codemirror.js"></script>
   	<script type="text/javascript" src="lib/languages/javascript.js"></script>
	<link rel="stylesheet" type="text/css" href="lib/codemirror.css" />
    <script src="lib/codemirror_extra/dialog/dialog.js"></script>
    <link rel="stylesheet" href="lib/codemirror_extra/dialog/dialog.css"/>
    <script src="lib/codemirror_extra/search/searchcursor.js"></script>
    <script src="lib/codemirror_extra/search/search.js"></script>
    <script src="lib/codemirror_extra/edit/matchbrackets.js"></script>
    <script src="lib/codemirror_extra/fold/foldcode.js"></script>
    <script src="lib/codemirror_extra/fold/brace-fold.js"></script>
    
    <script src="lib/jshint.js"></script>
	
<style media="screen" type="text/css">

input.rightButton {
    float: right;
}
	
#lrSplitter {
	width: 100%;
	height: 750px;
}
#tbSplitter {
	height: 700px;
}
#lrSplitter .Pane {
	overflow: auto;
}
#Right {
	overflow: hidden;
}
.vsplitbar {
	width: 3px;
	background: #999999 no-repeat center;
	/* No margin, border, or padding allowed */
}
.vsplitbar.active, .vsplitbar:hover {
	background: #e88 no-repeat center;
}
.hsplitbar {
	height: 3px;
	background: #999999 no-repeat center;
	/* No margin, border, or padding allowed */
}
.hsplitbar.active, .hsplitbar:hover {
	background: #e88 no-repeat center;
}
.CodeMirror { border-width:1px; border-style: solid; border-color:#DBDFE6; }
.CodeMirror-foldmarker {
        color: blue;
        text-shadow: #b9f 1px 1px 2px, #b9f -1px -1px 2px, #b9f 1px -1px 2px, #b9f -1px 1px 2px;
        font-family: arial;
        line-height: .3;
        cursor: pointer;
      }
</style>
	
<script type="text/javascript">
$().ready(function() {
	$("#lrSplitter").splitter({
		type: "v",
		sizeLeft: 400, maxLeft: 500,
		outline: true,
		cookie: "lrSplitter"
	});
});
$().ready(function() {
	$("#tbSplitter").splitter({
		type: "h",
		sizeTop: 255, minTop: 62, maxTop: 255,
		outline: true,
		cookie: "tbSplitter"
	});
});
</script>
<script language=javascript>

var currWidth = 0;
var currHeight = 0;

var int=self.setInterval(function(){clock()},50);
function clock()
  {	
	var newHeight = $('#Bottom').height() - 45;
	var newWidth = $('#Right').width() - 25;
	
	if ((currWidth != newWidth) || (currHeight != newHeight)) {
		currWidth = newWidth;
		currHeight = newHeight;
		$("#tbSplitter").css("width", ($('#Right').width() - 20)+"px").trigger("resize");
		$("#Top").css("width", ($('#Right').width() - 20)+"px");
		sourceJsonEditor.setSize(newWidth, newHeight);
		sourceJsonEditor_sah.setSize(newWidth, newHeight);
		sourceJsonEditor_uah.setSize(newWidth, newHeight);
		sourceJsonEditor_rss.setSize(newWidth, newHeight);
	}
  }
  $(window).resize(function(){
  	var leftWidth = $('#Left').width();
  	var winWidth = $(window).width();
  	$("#Right").css("width", (winWidth - leftWidth - 20)+"px");
  });
</script>
<script language=javascript>
	function checkFormat(alertOnSuccess, alertOnFailure)
	{
		alertOnFailure = (typeof alertOnFailure != 'undefined') ? alertOnFailure : true;
		var editor = sourceJsonEditor;
		if (alertOnSuccess) { // this is manual mode, work out which editor to check...
			if ("none" != sourceJsonEditor_sah.display.wrapper.style.display) {
				editor = sourceJsonEditor_sah;
			}
			else if ("none" != sourceJsonEditor_uah.display.wrapper.style.display) {
				editor = sourceJsonEditor_uah;
			}			
			else if ("none" != sourceJsonEditor_rss.display.wrapper.style.display) {
				editor = sourceJsonEditor_rss;
			}			
		}
		
		var success = JSHINT(editor.getValue());
		var output = '';
		if (!success) {
			output = 'Errors:\n\n'
			for (var i in JSHINT.errors) {
				var err = JSHINT.errors[i];
				if (null != err) {
					output += err.line + '[' + err.character + ']: ' + err.reason + '\n';
				}
				else {
					output += 'Unknown catastrophic error\n';
				}
			}
		}
		if (success && (editor == sourceJsonEditor)) {
			var json = eval('('+sourceJsonEditor.getValue()+')');
			if ((null == json.title) || (json.title == "")) {
				output = ("Title must be non-zero length\n");
				success = false;
			}
			if ((null == json.description) || (json.description == "")) {
				output += ("Description must be non-zero length\n");
				success = false;
			}
		}
		if (alertOnSuccess || !success) {
			if (output == "") {
				output = "No errors.\n";
			}
			if (success || alertOnFailure) {
				alert(output);
			}
		}
		return success;
	}//TESTED
	function switchToEditor(the_editor, alertOnFailure)
	{
		alertOnFailure = (typeof alertOnFailure != 'undefined') ? alertOnFailure : true;
		
		// Check overall JSON format is OK first
		if (!checkFormat(false, alertOnFailure)) {
			return;
		}
		// Convert source JSON text into JSON
		var srcObj = eval('(' + sourceJsonEditor.getValue() + ')');

		// Are we leaving the JSON page?
		var old_editor = null;
		if ("none" != sourceJsonEditor.display.wrapper.style.display) {
			old_editor = sourceJsonEditor;
		}//TESTED
		
		// Write results back into JSON editor if we're leaving a JS page
		if (null == old_editor) {
			var sah = sourceJsonEditor_sah.getValue();
			var uah = sourceJsonEditor_uah.getValue();
			var rss = sourceJsonEditor_rss.getValue();

			if ((null != sah) && (sah.trim() != "")) {
				if (null == srcObj.structuredAnalysis) {
					srcObj.structuredAnalysis = {};
				}
				srcObj.structuredAnalysis.script = sah;
				srcObj.structuredAnalysis.scriptEngine = "javascript";
			}

			if ((null != uah) && (uah.trim() != "")) {
				if (null == srcObj.unstructuredAnalysis) {
					srcObj.unstructuredAnalysis = {};
				}
				srcObj.unstructuredAnalysis.script = uah;
			}

			if ((null != rss) && (rss.trim() != "")) {
				if (null == srcObj.rss) {
					srcObj.rss = {};
				}
				if (null == srcObj.rss.searchConfig) {
					srcObj.rss.searchConfig = {};
				}
				srcObj.rss.searchConfig.globals = rss;
			}
			sourceJsonEditor.setValue(JSON.stringify(srcObj, null, "    "));
		}//TESTED
		else { // If we are leaving then set the JS contents from the source
			
			// Get script from source
			if ((null != srcObj.structuredAnalysis) && (null != srcObj.structuredAnalysis.script)) {
				sourceJsonEditor_sah.setValue(srcObj.structuredAnalysis.script);				
			}
			else {
				sourceJsonEditor_sah.setValue("");
			}			
			// Get script from source
			if ((null != srcObj.unstructuredAnalysis) && (null != srcObj.unstructuredAnalysis.script)) {
				sourceJsonEditor_uah.setValue(srcObj.unstructuredAnalysis.script);				
			}
			else {
				sourceJsonEditor_uah.setValue("");
			}			
			// Get script from source
			if ((null != srcObj.rss) && (null != srcObj.rss.searchConfig) && (null != srcObj.rss.searchConfig.globals)) {
				sourceJsonEditor_rss.setValue(srcObj.rss.searchConfig.globals);				
			}
			else {
				sourceJsonEditor_rss.setValue("");
			}			
		}//TESTED
		
		// Set the display:
		sourceJsonEditor.display.wrapper.style.display = "none";
		sourceJsonEditor_sah.display.wrapper.style.display = "none";
		sourceJsonEditor_uah.display.wrapper.style.display = "none";			
		sourceJsonEditor_rss.display.wrapper.style.display = "none";
		the_editor.display.wrapper.style.display = null;
		
		if (the_editor == sourceJsonEditor) {
			$("#toJson").css("font-weight", "bold");
			$("#toJsS").css("font-weight", "normal");
			$("#toJsU").css("font-weight", "normal");
			$("#toJsRss").css("font-weight", "normal");			
		}
		if (the_editor == sourceJsonEditor_sah) {
			$("#toJson").css("font-weight", "normal");
			$("#toJsS").css("font-weight", "bold");
			$("#toJsU").css("font-weight", "normal");
			$("#toJsRss").css("font-weight", "normal");			
		}
		if (the_editor == sourceJsonEditor_uah) {
			$("#toJson").css("font-weight", "normal");
			$("#toJsS").css("font-weight", "normal");
			$("#toJsU").css("font-weight", "bold");
			$("#toJsRss").css("font-weight", "normal");			
		}
		if (the_editor == sourceJsonEditor_rss) {
			$("#toJson").css("font-weight", "normal");
			$("#toJsS").css("font-weight", "normal");
			$("#toJsU").css("font-weight", "normal");
			$("#toJsRss").css("font-weight", "bold");			
		}
		sourceJsonEditor.refresh();
		sourceJsonEditor_sah.refresh();
		sourceJsonEditor_uah.refresh();
		sourceJsonEditor_rss.refresh();
		the_editor.focus();
	}//TESTED
	function removeStatusFields()
	{
		// Check overall JSON format is OK first
		if (!checkFormat(false)) {
			return false;
		}
		// Convert source JSON text into JSON
		var srcObj = eval('(' + sourceJsonEditor.getValue() + ')');
		
		// Remove fields we don't care about for config
		delete srcObj._id;
		delete srcObj.communityIds;
		delete srcObj.created;
		delete srcObj.harvest;
		delete srcObj.harvestBadSource;
		delete srcObj.isApproved;
		delete srcObj.key;
		delete srcObj.modified;
		delete srcObj.ownerId;
		delete srcObj.shah256Hash;		
		
		sourceJsonEditor.setValue(JSON.stringify(srcObj, null, "    "));
		return true;
	}
	function invertEnabledOrDisabled()
	{
		// Check overall JSON format is OK first
		if (!checkFormat(false)) {
			return false;
		}
		// Convert source JSON text into JSON
		var srcObj = eval('(' + sourceJsonEditor.getValue() + ')');

		if (srcObj.hasOwnProperty('searchCycle_secs')) {
			if (srcObj.searchCycle_secs == -1) {
				delete srcObj.searchCycle_secs;
				enableOrDisable.value = "Disable Source";
			}
			else {
				srcObj.searchCycle_secs = -srcObj.searchCycle_secs;
				if (srcObj.searchCycle_secs > 0) {
					enableOrDisable.value = "Disable Source";					
				}
				else {
					enableOrDisable.value = "Enable Source";										
				}
			}
		}
		else {
			srcObj.searchCycle_secs = -1;
			enableOrDisable.value = "Enable Source";
		}
		sourceJsonEditor.setValue(JSON.stringify(srcObj, null, "    "));
	}
	
</script>
<title>Infinit.e.Manager - Sources</title>
</head>
<body>

<%
	// !-- Create JavaScript Popup --
	if ((messageToDisplay.length() > 0) && 
			(action.equals("deleteDocs") || action.equals("publishSource") || action.equals("saveSourceAsTemplate") || action.equals("delete") || action.equals("deletesource")))
	{ 
%>
	<script language="javascript" type="text/javascript">
		alert('<%=messageToDisplay %>');
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
	
	 <div id="lrSplitter">
		 <div id="Left" class="Pane">
			<table class="standardTable" cellpadding="5" cellspacing="1" width="100%">
			<tr>
				<td class="headerLink">Sources</td>
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
		</div><!-- Left -->
		<div id="Right">
			<table class="standardTable" cellpadding="5" cellspacing="1" width="100%">
			<tr>
				<td class="headerLink">Edit Source</td>
				<td align="right"><button name="newSource" value="newSource">New Source</button></td>
			</tr>
			<tr>
				<td colspan="2" bgcolor="white">
					<div id="tbSplitter">
					<div id="Top" class="Pane">
					<table class="standardSubTable" cellpadding="3" cellspacing="1" width="100%" >
<% if (!shareid.equalsIgnoreCase("")) { %>
						<tr>
							<td bgcolor="white" width="30%">Source Functions:</td>
							<td bgcolor="white" width="70%">

								<input type="button" onclick="switchToEditor(sourceJsonEditor, false); if (checkFormat(false)) invertEnabledOrDisabled()" id="enableOrDisable" value="<%= enableOrDisable %>" />
								<button name="testSource" onclick="switchToEditor(sourceJsonEditor, false); return checkFormat(false)" value="testSource">Test Source</button>
								<button name="saveSource" onclick="switchToEditor(sourceJsonEditor, false); return checkFormat(false)" value="saveSource">Save Source</button>
								<button name="saveSourceAsTemplate" onclick="switchToEditor(sourceJsonEditor, false); return removeStatusFields()" value="saveSourceAsTemplate">Save As Template</button>
								
								<button name="publishSource" value="publishSource"
									onclick="switchToEditor(sourceJsonEditor, false); if (checkFormat(false) && confirm('Are you sure you want to publish this source?'))  return true; return false;"
									>Publish Source</button>
									
<% if ((null != sourceid) && !sourceid.equalsIgnoreCase("")) { %>
								<button name="deleteDocs" value="deleteDocs" 
									onclick="switchToEditor(sourceJsonEditor, false); if (confirm('Are you sure you want to delete all documents for this source?')) return true; return false;"
									>Delete docs</button>
<% } %>
							</td>		
						</tr>
<% } %>
						<tr>
							<td bgcolor="white" width="30%">Title:</td>
							<td bgcolor="white" width="70%">
								<input type="text" id="shareTitle" name="shareTitle" value="<%=org.apache.commons.lang.StringEscapeUtils.escapeHtml(shareTitle)%>" size="60" />
							</td>		
						</tr>
						<tr>
							<td bgcolor="white" width="30%">Share ID:</td>
							<td bgcolor="white" width="70%">
								<input type="text" id="shareId" name="shareId" value="<%=shareid%>" size="35" READONLY />
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
									size="3" title="Maximum of 100" />
								Update Test Mode: <input type="checkbox" name="testUpdateLogic" value="true" <%=getTestUpdateLogicChecked %>/>							
							</td>		
						</tr>
					</table>
					</div>
					<div id="Bottom" class="Pane">					
						<input type="button" title="Show full source JSON" style="font-weight:bold" onclick="switchToEditor(sourceJsonEditor)" id="toJson" value="JSON" />
						<input type="button" title="Show unstructuredAnalysis.script" onclick="switchToEditor(sourceJsonEditor_uah)" id="toJsU" value="JS-U" />
						<input type="button" title="Show structuredAnalysis.script" onclick="switchToEditor(sourceJsonEditor_sah)" id="toJsS" value="JS-S" />
						<input type="button" title="Show rss.searchConfig.globals" onclick="switchToEditor(sourceJsonEditor_rss)" id="toJsRss" value="JS-RSS" <%=sourceShowRss%> />
						<input type="button" onclick="checkFormat(true)" value="Check Format" class="rightButton" />
						<input type="button" title="Remove status fields added by server" onclick="removeStatusFields()" value="Scrub" class="rightButton" />
						<textarea cols="90" rows="25" id="Source_JSON" name="Source_JSON"><%=sourceJson%></textarea>
						<textarea id="Source_JSON_uahScript" name="Source_JSON_uahScript"></textarea>
						<textarea id="Source_JSON_sahScript" name="Source_JSON_sahScript"></textarea>
						<textarea id="Source_JSON_rssScript" name="Source_JSON_rssScript"></textarea>
					</div>
					</div>
				</td>
			</tr>
			</table>
		</div><!--  Right -->
	</div><!-- lrSplitter -->
	<input type="hidden" name="sourceid" id="sourceid" value="<%=sourceid%>"/>
	</form>
	
	
<!---------- CodeMirror JavaScripts ---------->
<script>
	var foldFunc = CodeMirror.newFoldFunction(CodeMirror.braceRangeFinder);
	var sourceJsonEditor_uah = CodeMirror.fromTextArea(document.getElementById("Source_JSON_uahScript"), {
		mode: "javascript",
		lineNumbers: true,
		matchBrackets: true,
		indentUnit: 4,
		extraKeys: { "Tab": "indentAuto", "Ctrl-Q": function(cm){foldFunc(cm, cm.getCursor().line);}}
	});
	sourceJsonEditor_uah.setSize("100%", "100%");
	sourceJsonEditor_uah.display.wrapper.style.display = "none";
	sourceJsonEditor_uah.on("gutterClick", foldFunc);

	var sourceJsonEditor_sah = CodeMirror.fromTextArea(document.getElementById("Source_JSON_sahScript"), {
		mode: "javascript",
		lineNumbers: true,
		matchBrackets: true,
		indentUnit: 4,
		extraKeys: { "Tab": "indentAuto", "Ctrl-Q": function(cm){foldFunc(cm, cm.getCursor().line);}}
	});
	sourceJsonEditor_sah.setSize("100%", "100%");
	sourceJsonEditor_sah.display.wrapper.style.display = "none";
	sourceJsonEditor_sah.on("gutterClick", foldFunc);
	
	var sourceJsonEditor_rss = CodeMirror.fromTextArea(document.getElementById("Source_JSON_rssScript"), {
		mode: "javascript",
		lineNumbers: true,
		matchBrackets: true,
		indentUnit: 4,
		extraKeys: { "Tab": "indentAuto", "Ctrl-Q": function(cm){foldFunc(cm, cm.getCursor().line);}}
	});
	sourceJsonEditor_rss.setSize("100%", "100%");
	sourceJsonEditor_rss.display.wrapper.style.display = "none";
	sourceJsonEditor_rss.on("gutterClick", foldFunc);
	
	var sourceJsonEditor = CodeMirror.fromTextArea(document.getElementById("Source_JSON"), {
		mode: "application/json",
		lineNumbers: true,
		matchBrackets: true,
		indentUnit: 4,
		extraKeys: { "Tab": "indentAuto", "Ctrl-Q": function(cm){foldFunc(cm, cm.getCursor().line);}}
	});
	sourceJsonEditor.setSize("100%", "100%");
	sourceJsonEditor.on("gutterClick", foldFunc);
	
	sourceJsonEditor.focus();
</script>
	
	
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
			if (source.has("_id")) {
				sourceid = source.getString("_id");
			}
			if (source.has("title")) {
				shareTitle = data.getString("title");
			}
			if ((null == shareTitle) || shareTitle.isEmpty()) {
				shareTitle = source.getString("title");				
			}
			if (source.has("description")) {
				shareDescription = data.getString("description");
			}
			if ((null == shareDescription) || shareDescription.isEmpty()) {
				shareDescription = source.getString("description");				
			}
			if (source.has("searchCycle_secs")) {
				int searchCycle_secs = source.getInt("searchCycle_secs");
				if (searchCycle_secs >= 0) {
					enableOrDisable = "Disable Source";
				}
				else {
					enableOrDisable = "Enable Source";
				}
			} 
			else {
				enableOrDisable = "Disable Source";
			}
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
			
			// Finally, decide whether to show JS-RSS tab
			sourceShowRss = "style=\"display: none\";";
			try {
				String sourceType = source.getString("extractType"); 
				if ((null != sourceType) && sourceType.equalsIgnoreCase("Feed")) {
					JSONObject rss = source.getJSONObject("rss");
					if (null != rss) {
						JSONObject searchConfig = rss.getJSONObject("searchConfig");
						if (null != searchConfig) {
							sourceShowRss = "";
						}
					}
				}
			}catch (Exception e) {} // do nothing, this block doesn't exist
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
	TreeMultimap<String, String> listOfSources = getUserSourcesAndShares(request, response, listFilter);
	
	if (listOfSources.size() > 0)
	{
		sources.append("<table class=\"listTable\" cellpadding=\"3\" cellspacing=\"1\" width=\"100%\" >");

		// Sort the sources alphabetically
		SortedSet<String> sortedKeys = listOfSources.keySet();
		
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
		for (String key : sortedKeys)
		{
			String name = key;
			SortedSet<String> vals = listOfSources.get(key);
			for (String val: vals) {
				if (currentItem >= startItem && currentItem <= endItem)
				{
					String id = val.toString();
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
								+ name.replace("\"", "\\\"") + "?\");'><img src=\"image/delete_x_button.png\" border=0></a>";
					}
					else
					{
						editLink = "<a href=\"sources.jsp?action=sharefromsource&sourceid=" + id + "&page=" + currentPage 
								+ listFilterString + "\" title=\"Create Share from Source\">" + name + "</a>";
								
						deleteLink = "<a href=\"sources.jsp?action=deletesource&sourceid=" + id + "&page=" + currentPage 
								+ listFilterString + "\" title=\"Delete Source\" "
								+ "onclick='return confirm(\"Do you really wish to delete the source: "
								+ name.replace("\"", "\\\"") + "?\");'><img src=\"image/delete_x_button.png\" border=0></a>";
					}
		
					// Create the HTML table row
					sources.append("<tr valign=\"top\">");
					if (id.equals(shareid)) {
						sources.append("<td bgcolor=\"white\" width=\"100%\"><b>" + editLink + "</b></td>");						
					}
					else {
						sources.append("<td bgcolor=\"white\" width=\"100%\">" + editLink + "</td>");
					}
					sources.append("<td align=\"center\" bgcolor=\"white\">" + deleteLink + "</td>");
					sources.append("</tr>");
				}
				currentItem++;
			}
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
		baseUrl.append("sources.jsp?action=page");
		if (listFilter.length() > 0) baseUrl.append('&').append("listFilterStr=").append(listFilter);
		if (shareid.length() > 0) baseUrl.append('&').append("shareid=").append(shareid);
		baseUrl.append("&page=");
		sources.append( createPageString( sortedKeys.size(), itemsToShowPerPage, currentPage, baseUrl.toString() ));
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
		if (0 == urlShareTitle.length()) {
			urlShareTitle = "Share+title+goes+here";
		}
		if (0 == urlShareDescription.length()) {
			urlShareDescription = "Share+description+goes+here";
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
		if (numDocs < 1 || numDocs > 100) numDocs = 10;
	}
	catch (Exception e)
	{
		numDocs = 10;
	}
	String apiAddress = "config/source/test?returnFullText=" + getFullText + "&numReturn=" + String.valueOf(numDocs) + "&testUpdates=" + getTestUpdateLogic;
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


// deleteSourceObject -
private void deleteSourceObject(String sourceId, boolean bDocsOnly, HttpServletRequest request, HttpServletResponse response)
{
	if (sourceId != null && sourceId != "") 
	{
		try 
		{
			JSONObject sourceResponse = new JSONObject( getSource(sourceId, request, response) );
			JSONObject source = new JSONObject( sourceResponse.getString("data") );
			JSONArray com = source.getJSONArray("communityIds");
			String tempCommunityId = com.getString(0);
					
			JSONObject JSONresponse = new JSONObject(deleteSource(sourceId, bDocsOnly, tempCommunityId, 
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



%>
