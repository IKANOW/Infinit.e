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

<!-- Optional localized variables -->

<fmt:message key='source.editor.action.disable_source' var='localized_DisableSource' scope='request' />
<fmt:message key='source.editor.action.enable_source' var='locale_EnableSource' scope='request' />

<fmt:message key='source.list.temp_copy' var='locale_SourceList_TempCopy' scope='request'/>
<fmt:message key='source.list.other_owner' var='locale_SourceList_OtherOwner' scope='request'/>
<fmt:message key='source.list.no_sources' var='locale_SourceList_NoSources' scope='request'/>

<fmt:message key='source.list.action.edit_share' var='locale_SourceList_EditShare' scope='request'/>
<fmt:message key='source.list.action.create_share' var='locale_SourceList_CreateShare' scope='request'/>
<fmt:message key='source.list.action.delete_share' var='locale_SourceList_DeleteShare' scope='request'/>
<fmt:message key='source.list.action.delete_share.confirm' var='locale_SourceList_DeleteShare_Confirm' scope='request'/>
<fmt:message key='source.list.action.delete_source' var='locale_SourceList_DeleteSource' scope='request'/>
<fmt:message key='source.list.action.delete_source.confirm' var='locale_SourceList_DeleteSource_Confirm' scope='request'/>
<fmt:message key='source.list.suspend_selected.confirm' var='locale' />

<fmt:message key='source.result.success' var='locale_SourceResult_Success' scope='request' />
<fmt:message key='source.result.error' var='locale_SourceResult_Error' scope='request' />
<fmt:message key='source.result.source_bulk_deletion' var='locale_SourceResult_SourceBulkDeletion' scope='request' />
<fmt:message key='source.result.mixed_bulk_deletion' var='locale_SourceResult_MixedBulkDeletion' scope='request' />
<fmt:message key='source.result.mixed_bulk_deletion_fail' var='locale_SourceResult_MixedBulkDeletionFail' scope='request' />
<fmt:message key='source.result.source_bulk_suspend' var='locale_SourceResult_SourceBulkSuspend' scope='request' />
<fmt:message key='source.result.source_bulk_resume' var='locale_SourceResult_SourceBulkResume' scope='request' />
<fmt:message key='source.result.test' var='locale_SourceResult_Test' scope='request' />

<fmt:message key='source.editor.mediaType.values' var='local_mediaType_values' scope='request' />
<fmt:message key='source.editor.mediaType.custom' var='local_mediaType_custom' scope='request' />

<!-- JSP logic -->

<%!
	// 
	static class Session {
		int currentPage = 1;
		int itemsToShowPerPage = 18;
		String action = "";
		String logoutAction = "";
		String listFilter = "";
	
		//
		String shareid = "";
		String sourceid = "";
		String sourceShowRss = "style=\"display: none\";";
		String sourceShowLogstash = "style=\"display: none\";";
		String sourceOnyShowLogstash = "style=\"display: none\";";	
		String enableOrDisable = "";
		String formShareId = "";
		String shareJson = "";
		String sourceJson = "";
		String communityId = "";
		String shareCreated = "";
		String shareTitle = "";
		String shareMediaType = "null";
		String shareTags = "";
		String shareDescription = "";
		String shareType = "";
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
		String mediaTypeSelect = "";
		String getFullText = "";
		String getFullTextChecked = "";
		String getTestUpdateLogic = "";
		String getTestUpdateLogicChecked = "";
		String numberOfDocuments = "";
		
		boolean pipelineMode = false; // new source pipeline logic
		boolean enterpriseMode = true; // access to source builder GUI
		boolean flowBuilderMode = true; // access to flow builder GUI
		boolean bucketBuilderMode = true; // access to "Bucket Builder" UI
		
		String messageToDisplay = "";
	}
%>

<%
	final Session session = new Session();

	session.messageToDisplay = "";
	session.enableOrDisable = (String)request.getAttribute("localized_DisableSource");
	
	// Check if source builder is installed:
	String enterpriseBaseDir =  System.getProperty("catalina.base") + "/webapps/infinit.e.source.builder";
	session.enterpriseMode = new File(enterpriseBaseDir).exists(); 	
	String flowBaseDir =  System.getProperty("catalina.base") + "/webapps/infinit.e.flow.builder";
	session.flowBuilderMode = new File(flowBaseDir).exists(); 	
	String bucketBuilderBaseDir = System.getProperty("catalina.base") + "/webapps/aleph2_bucket_builder";
	session.bucketBuilderMode = new File(bucketBuilderBaseDir).exists(); 	
	// 
	if (isLoggedIn) 
	{	
		// Capture value in the left handed table filter field
		if (request.getParameter("listFilter") != null) 
		{
			session.listFilter = request.getParameter("listFilter");
		}
		else if (request.getParameter("listFilterStr") != null) 
		{
			session.listFilter = request.getParameter("listFilterStr");
		}
		else
		{
			session.listFilter = "";
		}
		
		// Determine which action to perform on postback/request
		session.action = "";
		if (request.getParameter("action") != null) session.action = request.getParameter("action").toLowerCase();
		if (request.getParameter("dispatchAction") != null) session.action = request.getParameter("dispatchAction").toLowerCase();
		if (request.getParameter("clearForm") != null) session.action = request.getParameter("clearForm").toLowerCase();
		if (request.getParameter("filterList") != null) session.action = request.getParameter("filterList").toLowerCase();
		if (request.getParameter("clearFilter") != null) session.action = request.getParameter("clearFilter").toLowerCase();
		if (request.getParameter("logoutButton") != null) session.action = request.getParameter("logoutButton").toLowerCase();
		if (request.getParameter("deleteSelected") != null) session.action = request.getParameter("deleteSelected").toLowerCase();
		if (request.getParameter("deleteDocsFromSelected") != null) session.action = request.getParameter("deleteDocsFromSelected").toLowerCase();
		if (request.getParameter("suspendSelected") != null) session.action = request.getParameter("suspendSelected").toLowerCase();
		if (request.getParameter("resumeSelected") != null) session.action = request.getParameter("resumeSelected").toLowerCase();
		
		if (request.getParameter("testSource") != null) session.action = "testSource";
		if (request.getParameter("saveSource") != null) session.action = "saveSource";
		if (request.getParameter("saveSourceAsTemplate") != null) session.action = "saveSourceAsTemplate";
		if (request.getParameter("publishSource") != null) session.action = "publishSource";
		if (request.getParameter("deleteDocs") != null) session.action = "deleteDocs";
		if (request.getParameter("newSource") != null) session.action = "newSource";
		if (request.getParameter("revertSource") != null) session.action = "revertSource";
		
		// Capture input for page value if passed to handle the page selected in the left hand list of items
		if (request.getParameter("page") != null) 
		{
			session.currentPage = Integer.parseInt( request.getParameter("page").toLowerCase() );
		}
		else
		{
			session.currentPage = 1;
		}
		
		try
		{
			// Always clear the form first so there is no bleed over of values from previous requests
			clearForm(session);

			// Read in values from the edit form
			session.shareid = (request.getParameter("shareid") != null) ? request.getParameter("shareid") : "";
			session.formShareId = (request.getParameter("shareId") != null) ? request.getParameter("shareId") : "";
			session.sourceid = (request.getParameter("sourceid") != null) ? request.getParameter("sourceid") : "";
			session.communityId = (request.getParameter("Community_ID") != null) ? request.getParameter("Community_ID") : "";
			session.shareTitle = (request.getParameter("shareTitle") != null) ? request.getParameter("shareTitle") : "";
			session.shareMediaType = (request.getParameter("shareMediaType") != null) ? request.getParameter("shareMediaType") : "";
			session.shareTags = (request.getParameter("shareTags") != null) ? request.getParameter("shareTags") : "";
			session.shareTitle = org.apache.commons.lang.StringEscapeUtils.unescapeHtml(session.shareTitle);
			session.shareDescription = (request.getParameter("shareDescription") != null) ? request.getParameter("shareDescription") : "";
			session.sourceJson = (request.getParameter("Source_JSON") != null) ? request.getParameter("Source_JSON") : "";
			session.selectedSourceTemplate = (request.getParameter("sourceTemplateSelect") != null) ? request.getParameter("sourceTemplateSelect") : "";
			session.numberOfDocuments = (request.getParameter("numOfDocs") != null) ? request.getParameter("numOfDocs") : "10";
			session.getFullText = (request.getParameter("fullText") != null) ? "true" : "false";
			session.getFullTextChecked = (session.getFullText.equalsIgnoreCase("true")) ? "CHECKED" : "";
			session.getTestUpdateLogic = (request.getParameter("testUpdateLogic") != null) ? "true" : "false";
			session.getTestUpdateLogicChecked = (session.getTestUpdateLogic.equalsIgnoreCase("true")) ? "CHECKED" : "";
			
			Boolean redirect = false;
			
			// if redirect == true refresh the page to update the edit form's content to reflect changes
			if (redirect) 
			{
				String urlParams = "";
				if (session.listFilter.length() > 0) urlParams = "&listFilterStr="+ session.listFilter;
				if (session.currentPage > 1) urlParams += "&page=" + session.currentPage;
				out.println("<meta http-equiv=\"refresh\" content=\"0;url=sources.jsp?action=edit&shareid=" 
					+ session.shareid + urlParams + "\">");
			}
			
			if (session.action.equals("clearform")) 
			{
				out.println("<meta http-equiv=\"refresh\" content=\"0;url=sources.jsp?page=" + session.currentPage + "\">");
			}
			else if (session.action.equals("edit")) 
			{
				populateEditForm(session.shareid, request, response, session);
			}
			else if (session.action.equals("sharefromsource"))
			{
				// Create a new share from the source object
				String newshareid = createShareFromSource(session.sourceid, request, response, session);
				// redirect user to edit source page
				String listFilterString = "";
				if (session.listFilter.length() > 0) listFilterString = "&listFilterStr="+ session.listFilter;
				String urlArgs = "action=edit&shareid=" + newshareid + listFilterString + "&page=" + session.currentPage;
				out.println("<meta http-equiv=\"refresh\" content=\"0;url=sources.jsp?" + urlArgs + "\">");
			}
			else if (session.action.equals("revertSource"))
			{				
				// First delete the existing share:
				deleteShare(session.shareid, request, response, session);
				
				// Then Create a new share from the source object
				String newshareid = createShareFromSource(session.sourceid, request, response, session);
				// redirect user to edit source page
				String listFilterString = "";
				if (session.listFilter.length() > 0) listFilterString = "&listFilterStr="+ session.listFilter;
				String urlArgs = "action=edit&shareid=" + newshareid + listFilterString + "&page=" + session.currentPage;
				out.println("<meta http-equiv=\"refresh\" content=\"0;url=sources.jsp?" + urlArgs + "\">");
			}
			else if (session.action.equals("delete")) 
			{
				deleteShare(session.shareid, request, response, session);
				String listFilterString = "";
				if (session.listFilter.length() > 0) listFilterString = "&listFilterStr="+ session.listFilter;
				out.println("<meta http-equiv=\"refresh\" content=\"0;url=sources.jsp" + "?page=" + session.currentPage + listFilterString + "\">");
			}
			else if (session.action.equals("deletesource")) 
			{
				deleteSourceObject(session.sourceid, false, request, response, session);
				String listFilterString = "";
				if (session.listFilter.length() > 0) listFilterString = "&listFilterStr="+ session.listFilter;
				out.println("<meta http-equiv=\"refresh\" content=\"0;url=sources.jsp" + "?page=" + session.currentPage + listFilterString + "\">");
			}
			else if (session.action.equals("deleteselected")) 
			{
				saveShare(request, response, session);
				
				String[] ids= request.getParameterValues("sourcesToProcess");
				
				int nDeletedShares = 0;
				int nDeletedSources = 0;
				int nFailed = 0;
				for (String id: ids) {
					if (id.startsWith("_")) {
						id = id.substring(1);
						if (!deleteSourceObject(id, false, request, response, session)) {
							nFailed++;
						}
						else nDeletedSources++;						
					}
					else {
						if (!deleteShare(id, request, response, session)) {
							nFailed++;
						}
						else nDeletedShares++;						
					}
				}
				session.messageToDisplay = String.format((String)request.getAttribute("locale_SourceResult_SourceBulkDeletion"), 
													(Object)nDeletedSources, (Object)nDeletedShares, (Object)nFailed);
				
				out.print("<meta http-equiv=\"refresh\" content=\"0;url=sources.jsp?page=" + session.currentPage);
				if (session.listFilter.length() > 0) {
					out.print("&listFilterStr=" + session.listFilter);					
				}
				out.println("\">");
			}
			else if (session.action.equals("deletedocsfromselected")) 
			{
				saveShare(request, response, session);
				
				String[] ids= request.getParameterValues("sourcesToProcess");
				
				int nDeletedShares = 0;
				int nDeletedSources = 0;
				int nFailed = 0;
				for (String id: ids) {
					if (id.startsWith("_")) {
						id = id.substring(1);
						if (!deleteSourceObject(id, true, request, response, session)) {
							nFailed++;
						}
						else nDeletedSources++;						
					}
					else {
						nFailed++;
					}
				}
				session.messageToDisplay = String.format((String)request.getAttribute("locale_SourceResult_MixedBulkDeletion"), 
													(Object)nDeletedSources, (Object)nFailed);

				if (nFailed > 0) {
					session.messageToDisplay += " " + (String)request.getAttribute("locale_SourceResult_MixedBulkDeletionFail");
				}				
				out.print("<meta http-equiv=\"refresh\" content=\"0;url=sources.jsp?page=" + session.currentPage);
				if (session.listFilter.length() > 0) {
					out.print("&listFilterStr=" + session.listFilter);					
				}
				out.println("\">");
			}
			else if ( session.action.equals("suspendselected"))
			{
				saveShare(request, response, session);
				
				int num_suspended = 0;
				int num_failed = 0;
				String[] ids= request.getParameterValues("sourcesToProcess");				
				
				if ( ids != null )
				{					
					for (String id: ids) 
					{
						id = getSourceId(id, request, response, session);
						if ( id != null )
						{
							if (!suspendSourceObject(id, true, request, response, session)) 
								num_failed++;
							else 
								num_suspended++;
						}												
					}
				}				
				session.messageToDisplay = String.format((String)request.getAttribute("locale_SourceResult_SourceBulkSuspend"), 
													 (Object)num_suspended, (Object)num_failed);
				
				out.print("<meta http-equiv=\"refresh\" content=\"0;url=sources.jsp?page=" + session.currentPage);
				if (session.listFilter.length() > 0) 
				{
					out.print("&listFilterStr=" + session.listFilter);					
				}
				out.println("\">");
			}
			else if ( session.action.equals("resumeselected"))
			{
				saveShare(request, response, session);
				
				int num_suspended = 0;
				int num_failed = 0;
				String[] ids= request.getParameterValues("sourcesToProcess");				
				
				if ( ids != null )
				{
					for (String id: ids) 
					{
						id = getSourceId(id, request, response, session);
						if ( id != null )
						{
							if (!suspendSourceObject(id, false, request, response, session)) 
								num_failed++;
							else 
								num_suspended++;
						}										
					}
				}				
				session.messageToDisplay = String.format((String)request.getAttribute("locale_SourceResult_SourceBulkResume"), 
													 (Object)num_suspended, (Object)num_failed);
				
				out.print("<meta http-equiv=\"refresh\" content=\"0;url=sources.jsp?page=" + session.currentPage);
				if (session.listFilter.length() > 0) 
				{
					out.print("&listFilterStr=" + session.listFilter);					
				}
				out.println("\">");
			}
			else if (session.action.equals("filterlist")) 
			{
				saveShare(request, response, session);
				session.currentPage = 1;
				populateEditForm(session.shareid, request, response, session);
			}
			else if (session.action.equals("clearfilter")) 
			{
				saveShare(request, response, session);
				session.currentPage = 1;
				session.listFilter = "";
				populateEditForm(session.shareid, request, response, session);
			}
			else if (session.action.equals("saveSource")) 
			{
				saveShare(request, response, session);
				populateEditForm(session.shareid, request, response, session);
			}
			else if (session.action.equals("saveSourceAsTemplate")) 
			{
				saveShare(request, response, session);
				saveShareAsTemplate(request, response, session);
				populateEditForm(session.shareid, request, response, session);
			}
			else if (session.action.equals("publishSource")) 
			{
				saveShare(request, response, session); // (save even though about to delete it...because if the pub fails i can restore)
				boolean published = publishSource(request, response, session);
				String listFilterString = "";
				if (session.listFilter.length() > 0) listFilterString = "&listFilterStr="+ session.listFilter;
				if (published) {
					out.println("<meta http-equiv=\"refresh\" content=\"0;url=sources.jsp" + "?page=" + session.currentPage + listFilterString + "\">");
				}
				else {
					populateEditForm(session.shareid, request, response, session);
				}
			}
			else if (session.action.equals("deleteDocs")) 
			{
				saveShare(request, response, session);
				deleteSourceObject(session.sourceid, true, request, response, session);
				String listFilterString = "";
				if (session.listFilter.length() > 0) listFilterString = "&listFilterStr="+ session.listFilter;
				out.println("<meta http-equiv=\"refresh\" content=\"0;url=sources.jsp?action=edit&shareid=" + session.shareid + "&page=" + session.currentPage + listFilterString + "\">");
			}
			else if (session.action.equals("newSource")) 
			{
				out.println("<meta http-equiv=\"refresh\" content=\"0;url=newsource.jsp\">");
			}
			else if (session.action.equals("testSource")) 
			{
				testSource(request, response, session);
			}
			else if (session.action.equals("logout")) 
			{
				logOut(request, response);
				out.println("<meta http-equiv=\"refresh\" content=\"0;url=index.jsp\">");
			}
			else {
				populateEditForm(session.shareid, request, response, session);				
			}
			
			createCommunityIdSelect(request, response, session);
			session.mediaTypeSelect = createMediaTypeSelect(request, session);
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
	<script type="text/javascript" src="inc/convertToPipeline.js"></script>
	<link rel="shortcut icon" href="image/favicon.ico" />
	
   <script src="lib/jquery.js"></script>
   <script src="lib/jquery.cookie.js"></script>
   
    <script src="lib/splitter.js"></script>
    
   	<script type="text/javascript" src="lib/codemirror.js"></script>
   	<script type="text/javascript" src="lib/languages/javascript.js"></script>
   	<script type="text/javascript" src="lib/languages/ruby.js"></script>
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
	height: 800px;
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
		sizeTop: 290, minTop: 62, maxTop: 290,
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
		sourceJsonEditor_logstash.setSize(newWidth, newHeight);
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
			else if ("none" != sourceJsonEditor_logstash.display.wrapper.style.display) {
				// Not supported:
				alert("<fmt:message key='source.code.action.check_format.logstash'/>\n");
				return true;
			}			
		}
		
		var success = JSHINT(editor.getValue());
		var output = '';
		if (!success) {
			output = "<fmt:message key='source.result.check_format.error'/>\n\n";
			for (var i in JSHINT.errors) {
				var err = JSHINT.errors[i];
				if (null != err) {
					output += err.line + '[' + err.character + ']: ' + err.reason + '\n';
				}
				else {
					output += "<fmt:message key='source.result.check_format.unknown_error'/>\n";
				}
			}
		}
		if (success && (editor == sourceJsonEditor)) {
			var json = eval('('+sourceJsonEditor.getValue()+')');
			if ((null == json.title) || (json.title == "")) {
				output = ("<fmt:message key='source.result.check_format.no_title'/>\n");
				success = false;
			}
			if ((null == json.description) || (json.description == "")) {
				output += ("<fmt:message key='source.result.check_format.no_description'/>\n");
				success = false;
			}
		}
		if (alertOnSuccess || !success) {
			if (output == "") {
				output = "<fmt:message key='source.result.check_format.success'/>\n";
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
			var logstash = sourceJsonEditor_logstash.getValue();

			if ((null != sah) && (sah.trim() != "")) {
				if (null == srcObj.structuredAnalysis) {
					srcObj.structuredAnalysis = {};
				}
				srcObj.structuredAnalysis.script = sah;
				srcObj.structuredAnalysis.scriptEngine = "javascript";
			}

			
			if (null != srcObj.processingPipeline) { // (handle the 4 pipeline cases, logstash, custom, and JS ... and now V2)
				var globals = null;		
				var isCustomJob = false;
				var isV2Job = false;
				for (var x in srcObj.processingPipeline) {
					var pxPipe = srcObj.processingPipeline[x];
					if (pxPipe.logstash) {
						if ((null != logstash) && (logstash.trim() != "")) {
							pxPipe.logstash.config = logstash;
						}			
						else {
							delete pxPipe.logstash.config;
						}
					}
					if (pxPipe.globals) {
						globals = pxPipe.globals;
						break;
					}
					if (pxPipe.scriptingEngine) {
						isCustomJob = true;
						pxPipe.scriptingEngine.globalScript = uah;
					}
					if (pxPipe.data_bucket) {
						isV2Job = true;
						if (!pxPipe.data_bucket.scripting) {
							pxPipe.data_bucket.scripting = { "sub_prefix": "$$SCRIPT_", "sub_suffix": "$$" };
						}
						pxPipe.data_bucket.scripting.js = { "script": uah, "separator_regex": "//ALEPH2_MODULE-.*" };
						break;
						
					}
				}
				if (!isCustomJob && !isV2Job) {
					if ((null != uah) && (uah.trim() != "")) {
						if (null == globals) { // no globals, insert
							globals = {};
							srcObj.processingPipeline.splice(1, 0, { "globals": globals });
						}
						globals.scriptlang = "javascript";
						if (null == globals.scripts) {
							globals.scripts = [];
						}
						globals.scripts[0] = uah;
					}
					else {
						if (null != globals) {
							delete globals.scripts;
						}
						//(else nothing to do)
					}
				}
			}//(end if px pipeline mode)
			
			if ((null != uah) && (uah.trim() != "")) {
				if (null == srcObj.processingPipeline) { // (legacy format)
					if (null == srcObj.unstructuredAnalysis) {
						srcObj.unstructuredAnalysis = {};
					}
					srcObj.unstructuredAnalysis.script = uah;
				}
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
			else if (srcObj.processingPipeline) {
				var isCustomJob = false;
				var isV2Job = false;
				var globals = null;
				sourceJsonEditor_logstash.setValue("");
				for (var x in srcObj.processingPipeline) {
					var pxPipe = srcObj.processingPipeline[x];
					if (pxPipe.logstash) {
						if (null != pxPipe.logstash.config) {
							sourceJsonEditor_logstash.setValue(pxPipe.logstash.config);
						}
					}
					if (pxPipe.data_bucket) {
						if ((null != pxPipe.data_bucket.scripting) && (null != pxPipe.data_bucket.scripting.js) && (null != pxPipe.data_bucket.scripting.js.script))
						{
							isV2Job = true;
							globals = pxPipe.data_bucket.scripting.js.script;
						}
						break;						
					}
					if (pxPipe.globals) {
						globals = pxPipe.globals;
						break;
					}
					if (pxPipe.custom_file || pxPipe.docs_datastoreQuery || pxPipe.docs_documentQuery || pxPipe.records_indexQuery || pxPipe.custom_datastoreQuery || pxPipe.feature_datastoreQuery) {
						isCustomJob = true;
					}
					if (pxPipe.scriptingEngine) {
						globals = pxPipe.scriptingEngine;
					}
				}
				if (!isCustomJob && !isV2Job) {
					if ((null == globals) || (null == globals.scripts) || (0 == globals.scripts.length)) { 
						// no globals, set script to be blank
						sourceJsonEditor_uah.setValue("");					
					}				
					else {
						sourceJsonEditor_uah.setValue(globals.scripts[0]);
					}
				}
				else if (isCustomJob && (the_editor == sourceJsonEditor_uah)){ // bit more restrictive in custom mode, scriptingEngine has to exist
					if (null == globals) {
						alert("Can't use the JS tab until a scriptingEngine elements exists");
						return;
					}
					else {
						sourceJsonEditor_uah.setValue(globals.globalScript);
					}
				}
				else if (isV2Job) {
					sourceJsonEditor_uah.setValue(globals);
				}
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
		sourceJsonEditor_logstash.display.wrapper.style.display = "none";
		the_editor.display.wrapper.style.display = null;
		
		if (the_editor == sourceJsonEditor) {
			$("#toJson").css("font-weight", "bold");
			$("#toJsS").css("font-weight", "normal");
			$("#toJsU").css("font-weight", "normal");
			$("#toJsRss").css("font-weight", "normal");			
			$("#toJsLog").css("font-weight", "normal");			
		}
		if (the_editor == sourceJsonEditor_sah) {
			$("#toJson").css("font-weight", "normal");
			$("#toJsS").css("font-weight", "bold");
			$("#toJsU").css("font-weight", "normal");
			$("#toJsRss").css("font-weight", "normal");			
			$("#toJsLog").css("font-weight", "normal");			
		}
		if (the_editor == sourceJsonEditor_uah) {
			$("#toJson").css("font-weight", "normal");
			$("#toJsS").css("font-weight", "normal");
			$("#toJsU").css("font-weight", "bold");
			$("#toJsRss").css("font-weight", "normal");			
			$("#toJsLog").css("font-weight", "normal");			
		}
		if (the_editor == sourceJsonEditor_rss) {
			$("#toJson").css("font-weight", "normal");
			$("#toJsS").css("font-weight", "normal");
			$("#toJsU").css("font-weight", "normal");
			$("#toJsRss").css("font-weight", "bold");			
			$("#toJsLog").css("font-weight", "normal");			
		}
		if (the_editor == sourceJsonEditor_logstash) {
			$("#toJson").css("font-weight", "normal");
			$("#toJsS").css("font-weight", "normal");
			$("#toJsU").css("font-weight", "normal");
			$("#toJsRss").css("font-weight", "normal");			
			$("#toJsLog").css("font-weight", "bold");			
		}
		sourceJsonEditor.refresh();
		sourceJsonEditor_sah.refresh();
		sourceJsonEditor_uah.refresh();
		sourceJsonEditor_rss.refresh();
		sourceJsonEditor_logstash.refresh();
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
				enableOrDisable.value = "<fmt:message key='source.editor.action.disable_source'/>";
			}
			else if (srcObj.searchCycle_secs == 0) { // => run once only
				srcObj.searchCycle_secs = -1;
				enableOrDisable.value = "<fmt:message key='source.editor.action.enable_source'/>";
			}
			else {
				srcObj.searchCycle_secs = -srcObj.searchCycle_secs;
				if (srcObj.searchCycle_secs > 0) {
					enableOrDisable.value = "<fmt:message key='source.editor.action.disable_source'/>";					
				}
				else {
					enableOrDisable.value = "<fmt:message key='source.editor.action.enable_source'/>";										
				}
			}
		}
		else {
			srcObj.searchCycle_secs = -1;
			enableOrDisable.value = "<fmt:message key='source.editor.action.enable_source'/>";
		}
		sourceJsonEditor.setValue(JSON.stringify(srcObj, null, "    "));
		$( "#publishSource" ).click();
	}
	function convertOldSource() {
		// Check overall JSON format is OK first
		if (!checkFormat(false, true)) {
			return;
		}
		// Convert source JSON text into JSON
		var srcObj = eval('(' + sourceJsonEditor.getValue() + ')');
		var err = convertToPipeline(srcObj);
		if (null == err) {
			sourceJsonEditor.setValue(JSON.stringify(srcObj, null, "    "));
			alert("Completed without errors. Press 'Save Source' to save the new source, and refresh the editor.\n\nWARNING: this process is not always perfect and you should test your source before publishing it.");
		}
		else {
			alert(err);
		}
	}
	
</script>
	<script language=javascript>
	function hideFlowBuilder()
	{
		if (!checkFormat(false)) {
			return;
		}
		try {
			//Grab iframe for flowbuilder
			fbWindow = document. getElementById('flowbuilderInfinitIframe').contentWindow; 
			//flowbuilderJson is the code we have to plug into the srcObj
			var errors = fbWindow.fbDataFlowObj.graphErrors;
			if(errors !== ""){
				if (!window.confirm("The following errors have been found:\n\n"+errors+"\n\n Do you wish to return to editor anyways? Flow source will not be saved.")) { 
					  return
				}
			}else{
				//alert("No flow errors found");
			
			var flowbuilderJson = fbWindow.fbDataFlowObj.plugins.source.getCode();
			var srcObj  = eval('(' + sourceJsonEditor.getValue() + ')');
			var flowObj = eval('(' + flowbuilderJson + ')');
			srcObj.templateProcessingFlow = flowObj;
			var modifiedSrc = fbWindow.fbDataFlowObj.modifiedSource;
			srcObj.processingPipeline = modifiedSrc.processingPipeline; 			
			sourceJsonEditor.setValue(JSON.stringify(srcObj, null, "    "));
			}
		}
		catch (err) {
			if (!confirm('Error reading GUI config - click OK to continue back to the source editor (WILL LOSE YOUR CHANGES)'))
			{
				return;
			}
		}		
		
		
		$('#flowbuilderIframeClose').hide();
		$(flowBuilder).css("width", "0%");
		$(flowBuilder).css("height", "0%");
		$(flowBuilder).hide();
		$(flowBuilder_overlay).hide();
	}
		function hideSourceBuilder()
		{
			if (!checkFormat(false)) {
				return;
			}
			var pxPipelineStr = document.getElementById('InfinitIframe').contentWindow.getSource();
			try {
				if ('%' == pxPipelineStr.charAt(0)) {
					pxPipelineStr = decodeURIComponent(pxPipelineStr);
				}
				var pipelineObj = eval('(' + pxPipelineStr + ')');
				
				var srcObj = eval('(' + sourceJsonEditor.getValue() + ')');
				srcObj.processingPipeline = pipelineObj;
				sourceJsonEditor.setValue(JSON.stringify(srcObj, null, "    "));
			}
			catch (err) {
				if (!confirm('Error reading GUI config - click OK to continue back to the source editor (WILL LOSE YOUR CHANGES)'))
				{
					return;
				}
			}			
			$(sourceBuilder).css("width", "0%");
			$(sourceBuilder).css("height", "0%");
			$(sourceBuilder_overlay).hide();
		}
		function checkSourceCompatibleWithSourceBuilder(srcObj)
		{
			var nForms = 0;
			for (var x in srcObj.processingPipeline) {
				var pipe = srcObj.processingPipeline[x];
				if (pipe.feed) {
					if (pipe.feed.extraUrls) {
						nForms += pipe.feed.extraUrls.length;
					}
				}//TESTED
				if (pipe.web) {
					if (pipe.web.extraUrls) {
						nForms += pipe.web.extraUrls.length;
					}
				}//TESTED
				if (pipe.links) {
					if (pipe.links.extraMeta) {
						nForms += pipe.links.extraMeta.length;
					}
				}//TESTED
				if (pipe.entities) {
					nForms += 2*pipe.entities.length; // (extra complex form)
				}//TESTED
				if (pipe.associations) {
					nForms += 2*pipe.associations.length; // (extra complex form)
				}//TESTED
				if (pipe.text) {
					nForms += pipe.text.length; 
				}//TESTED
				if (pipe.contentMetadata) {
					nForms += pipe.contentMetadata.length; 
				}//TESTED
			}
			if (nForms > 120) { 
				return "This source object is currently too complicated to be viewed from the GUI: " + nForms;
			}
			return null;
		}
		function showSourceBuilder()		
		{
			// Check overall JSON format is OK first
			if (!checkFormat(false)) {
				return;
			}
			// Convert source JSON text into JSON
			var srcObj = eval('(' + sourceJsonEditor.getValue() + ')');
			
			if (srcObj.extractType == "V2DataBucket") {
				alert("V2 sources not currently supported in the source builder UI");
				return;
			}
			
			// CHECK FOR A COMMON USE CASE - ACCIDENTALLY CLICK ON A NON FLOW SOURCE
			if ((null != srcObj.templateProcessingFlow))
			{
				if (!confirm("Warning: this source is flow-based - changes made in this UI will not be reflected in the flow view - the next time the flow UI is accessed it will overwrite these changes. Select 'OK' to continue or 'CANCEL' to abort.")) 
				{
					return;
				}
			}											
			
			var errMsg = checkSourceCompatibleWithSourceBuilder(srcObj);
			if (null != errMsg) {
				alert(errMsg);
				return;
			}
			var pxPipelineStr = JSON.stringify(srcObj.processingPipeline, null, "    ");
			if (null == pxPipelineStr) {
				pxPipelineStr = "";
			}
			document.getElementById('InfinitIframe').contentWindow.setSource(pxPipelineStr);
			$(sourceBuilder_overlay).show();
			$(sourceBuilder).css("z-index", "1000");
			$(sourceBuilder).css("width", "90%");
			$(sourceBuilder).css("height", "90%");
		}
		// bucket builder
		function showBucketBuilder() 
		{
			try {
				// Check overall JSON format is OK first
				if (!checkFormat(false)) {
					return;
				}
				
				// Convert source JSON text into JSON
				var srcObj = eval('(' + sourceJsonEditor.getValue() + ')');
				
				// CHECK FOR A COMMON USE CASE - ACCIDENTALLY CLICK ON A NON FLOW SOURCE
				if ((null == srcObj.templateProcessingFlow) && (null != srcObj.processingPipeline) && (srcObj.processingPipeline.length > 0))
				{
					if (!confirm("This source doesn't not appear to be card-based - continuing will delete your existing source logic. To continue, select 'OK'. Select 'CANCEL' to edit the source from the editor or source builder UI.")) {
						return;
					}
				}								
				
				//only checks if source works as it was before. Does not check the correctness of the 
				//flowbuilder source object.
				var errMsg = checkSourceCompatibleWithSourceBuilder(srcObj);
				if (null != errMsg) {
					alert(errMsg);
					return;
				}
				
				// Set up parent variables:
				window.aleph2_json_builder__root_element = (srcObj.extractType != "V2DataBucket" ? "Source" : "Bucket");
				window.aleph2_json_builder__generated_input_obj = 
					srcObj.extractType != "V2DataBucket" 
					?
					{ "processingPipeline": [] } 
					:
					{ "processingPipeline": [{ "data_bucket": {} }] } 

				window.aleph2_json_builder__get_input_root_fn = 
					srcObj.extractType != "V2DataBucket" 
					?					
					function(input) { return input.processingPipeline; }
					:
					function(input) { return input.processingPipeline[0].data_bucket; }
				
				window.aleph2_json_builder__config_input_obj = srcObj.templateProcessingFlow;
				
				window.aleph2_json_builder__template_url = "/api/social/share/search?type=aleph2-bucket-template";

                window.aleph2_json_builder__template_conversion_fn = function(input) {
                    // (ok this is a bit complicated because of the lack of map in JS)
                    var output = [];
                    if (null != input.data) {
                        for (i in input.data) {
                            var el = input.data[i];
                            if (null != el.share) {
                                try {
                                    var el_obj = JSON.parse(el.share);
                                    if (Array.isArray(el_obj)) {
                                        for (j in el_obj) output.push(el_obj[j]);
                                    }
                                    else if (el_obj.templates) {
                                        for (j in el_obj.templates) output.push(el_obj.templates[j]);
                                    }
                                    else {
                                        output.push(el_obj);
                                    }
                                }
                                catch (e) {
                                    console.log(e.message);
                                }
                            }
                        }
                    }
                    return output;
				}
				
				//DEBUG
//				window.aleph2_json_builder__template_url = "/aleph2_bucket_builder/assets/json/sample_templates.json";
//				window.aleph2_json_builder__template_conversion_fn = function(input) { return input.ret_val; } 

				
				window.aleph2_json_builder__config_output_str  = "{}";
				window.aleph2_json_builder__generated_output_str = "{}";
				
				// Resize

				$(bucketBuilder).css("width", "98%");
				$(bucketBuilder).css("height", "95%");

				// Reload
				
				document.getElementById('bucketbuilderInfinitIframe').contentWindow.location.reload(true)
				
				// Show
				
				$(bucketBuilder).css("z-index", "1000");
				$(bucketBuilder_overlay).show();
				$('#bucketbuilderIframeClose').show();
				$(bucketBuilder).show();
				
			}
			catch (e) {
				alert(e.message);
			}
		}
		function hideBucketBuilder()
		{
			if (!checkFormat(false)) {
				return;
			}
			try {
				//Grab iframe for flowbuilder
				fbWindow = document.getElementById('bucketbuilderInfinitIframe').contentWindow; 

				/* SHOULD PORT THIS OVER TO BB
				var errors = fbWindow.fbDataFlowObj.graphErrors;
				if(errors !== ""){
					if (!window.confirm("The following errors have been found:\n\n"+errors+"\n\n Do you wish to return to editor anyways? Flow source will not be saved.")) { 
						  return
					}
				}else */
				{
					//alert("No flow errors found");
				
					var srcObj  = JSON.parse(fbWindow.aleph2_json_builder__generated_output_str);
					var flowObj = JSON.parse(fbWindow.aleph2_json_builder__config_output_str);
					
					flowObj.start_pos = fbWindow.aleph2_json_builder__output_start_pos;
					srcObj.templateProcessingFlow = flowObj;
					
					// Copy fields into original src
					var oldSrcObj = JSON.parse(sourceJsonEditor.getValue());
					oldSrcObj.processingPipeline = []; //(should get overwritten)
					for (x in srcObj) {
						oldSrcObj[x] = srcObj[x]
					}
					sourceJsonEditor.setValue(JSON.stringify(oldSrcObj, null, "    "));
				}
			}
			catch (err) {
				if (!confirm('Error reading GUI config - click OK to continue back to the source editor (WILL LOSE YOUR CHANGES)'))
				{
					return;
				}
			}		
			
			
			$('#bucketbuilderIframeClose').hide();
			$(bucketBuilder).css("width", "0%");
			$(bucketBuilder).css("height", "0%");
			$(bucketBuilder).hide();
			$(bucketBuilder_overlay).hide();
		}
		
		// flowbuilder
		function showFlowBuilder()		
		{
			try {
				// Check overall JSON format is OK first
				if (!checkFormat(false)) {
					return;
				}
				
				// Convert source JSON text into JSON
				var srcObj = eval('(' + sourceJsonEditor.getValue() + ')');
				
				// CHECK FOR A COMMON USE CASE - ACCIDENTALLY CLICK ON A NON FLOW SOURCE
				if ((null == srcObj.templateProcessingFlow) && (null != srcObj.processingPipeline) && (srcObj.processingPipeline.length > 0))
				{
					if (!confirm("This source doesn't not appear to be flow-based - continuing will delete your existing source logic. To continue, select 'OK'. Select 'CANCEL' to edit the source from the editor or source builder UI.")) {
						return;
					}
				}								
				
				//only checks if source works as it was before. Does not check the correctness of the 
				//flowbuilder source object.
				var errMsg = checkSourceCompatibleWithSourceBuilder(srcObj);
				if (null != errMsg) {
					alert(errMsg);
					return;
				}
				var flPipelineStr = JSON.stringify(srcObj.templateProcessingFlow, null, "    ");
				if (null == flPipelineStr) {
					flPipelineStr = "";
				}
				fbWindow = document. getElementById('flowbuilderInfinitIframe').contentWindow;
				// Spin up app
			    var dataflow = fbWindow.fbDataFlowObj;
				if (null == dataflow) {
					alert("Error accessing flow builder - if the browser is still loading, wait a few moments and then try again.\n\nOtherwise - check you are logged on (this may also fail if the API is in a non-standard place - contact your system administrator if you suspect this to be the case)");
					return;
				}
				dataflow.doConvert=false;
				dataflow.originalSrc = srcObj;
			    var g = dataflow.loadGraph(
			    		(typeof(srcObj.templateProcessingFlow)==="undefined")?{}:srcObj.templateProcessingFlow
			    		);
			    		
			   	dataflow.doConvert=true;
			    g.trigger("change");
				
				$(flowBuilder_overlay).show();
				$('#flowbuilderIframeClose').show();
				$(flowBuilder).show();
				$(flowBuilder).css("z-index", "1000");
				$(flowBuilder).css("width", "98%");
				$(flowBuilder).css("height", "95%");
			}
			catch (e) {
				alert(e.message);
			}
		}
		function checkSourceCompatibleWithFlowBuilder(srcObj){
			return null;
		}
	</script>
<title><fmt:message key='source.title'/></title>
</head>
<body>

<%
	// !-- Create JavaScript Popup --
	if ((session.messageToDisplay.length() > 0) && 
			(session.action.equalsIgnoreCase("deleteDocs") || session.action.equalsIgnoreCase("publishSource") || session.action.equalsIgnoreCase("saveSourceAsTemplate") 
					|| session.action.equalsIgnoreCase("delete") || session.action.equalsIgnoreCase("deletesource")
					|| session.action.equalsIgnoreCase("deleteSelected") || session.action.equalsIgnoreCase("deletedocsfromselected")
					|| session.action.equalsIgnoreCase("suspendSelected") || session.action.equalsIgnoreCase("resumeSelected")
					))
	{ 
%>
	<script language="javascript" type="text/javascript">
		alert('<%=session.messageToDisplay.replace("'", "\\'").replaceAll("[\n\r]", " ") %>');
	</script>
<% } %>


<% 
	//!-- Open new window to show source test results --
	if (session.harvesterOutput.length() > 0) 
	{
		String messageToOutput = URLEncoder.encode(session.messageToDisplay, "UTF-8");
		String output = URLEncoder.encode(session.harvesterOutput, "UTF-8");
		session.harvesterOutput = "";
		session.messageToDisplay = "";
%>
	<script language="javascript" type="text/javascript">
		openTestSourceWindow("<fmt:message key='source.result.test.title'/>", '<%=messageToOutput %>', '<%=output %>', "<fmt:message key='source.editor.popups_blocked'/>");
	</script>
<% } %>

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
	 <div id="lrSplitter">
		 <div id="Left" class="Pane">
			<table class="standardTable" cellpadding="5" cellspacing="1" width="100%">
			<tr>
				<td class="headerLink"><fmt:message key='source.list.name'/></td>
				<td align="right"><input type="text" id="listFilter" 
					onkeydown="if (event.keyCode == 13) { setDipatchAction('filterList'); 
					document.getElementById('filterList').click(); }" 
					name="listFilter" size="20" value="<%=session.listFilter %>"/><button name="filterList" 
					value="filterList"><fmt:message key='source.list.action.filter'/></button><button name="clearFilter" value="clearFilter"><fmt:message key='source.list.action.clear'/></button></td>
			</tr>
			<tr>
				<td colspan="2" bgcolor="white"><%=listItems(request, response, session) %></td>
			</tr>
			<tr>
				<td colspan="2" >
					<button name="deleteSelected" onclick="return confirm('<fmt:message key='source.list.delete_selected.confirm'/>');" name="deleteSelected" value="deleteSelected"><fmt:message key='source.list.delete_selected'/></button>
					<button name="deleteDocsFromSelected" onclick="return confirm('<fmt:message key='source.list.delete_selected_docs.confirm'/>');" name="deleteDocsFromSelected" value="deleteDocsFromSelected"><fmt:message key='source.list.delete_selected_docs'/></button>				
					<button name="suspendSelected" onclick="return confirm('<fmt:message key='source.list.suspend_selected.confirm'/>');" name="suspendSelected" value="suspendSelected"><fmt:message key='source.list.suspend_selected'/></button>
					<button name="resumeSelected" onclick="return confirm('<fmt:message key='source.list.resume_selected.confirm'/>');" name="resumeSelected" value="resumeSelected"><fmt:message key='source.list.resume_selected'/></button>
					<input type="checkbox" name="selectall" onchange="var cbs = document.getElementsByName('sourcesToProcess'); for(var i=0; i < cbs.length; i++) if(cbs[i].type == 'checkbox') cbs[i].checked=selectall.checked" value=""></input>
				</td>
			</tr>
			</table>
		</div><!-- Left -->
		<div id="Right">
			<table class="standardTable" cellpadding="5" cellspacing="1" width="100%">
			<tr>
				<td class="headerLink"><fmt:message key='source.editor.name'/></td>
				<td align="right"><button name="newSource" value="newSource"><fmt:message key='source.editor.action.new_source'/></button></td>
			</tr>
			<tr>
				<td colspan="2" bgcolor="white">
					<div id="tbSplitter">
					<div id="Top" class="Pane">
					<table class="standardSubTable" cellpadding="3" cellspacing="1" width="100%" >
<% if (!session.shareid.equalsIgnoreCase("")) { %>
						<tr>
							<td bgcolor="white" width="30%"><fmt:message key='source.editor.functions.title'/></td>
							<td bgcolor="white" width="70%" colspan="2">

								<input type="button" 
									onclick="switchToEditor(sourceJsonEditor, false); if (checkFormat(false)) invertEnabledOrDisabled();" 
									id="enableOrDisable" value="<%= session.enableOrDisable %>" />
								<button name="testSource" onclick="switchToEditor(sourceJsonEditor, false); return checkFormat(false)" value="testSource"><fmt:message key='source.editor.action.test_source'/></button>
								<button name="saveSource" onclick="switchToEditor(sourceJsonEditor, false); return checkFormat(false)" value="saveSource"><fmt:message key='source.editor.action.save_source'/></button>
								<button name="saveSourceAsTemplate" onclick="switchToEditor(sourceJsonEditor, false); return removeStatusFields()" value="saveSourceAsTemplate"><fmt:message key='source.editor.action.save_template'/></button>
								
								<button name="publishSource" value="publishSource" id="publishSource"  
									onclick="switchToEditor(sourceJsonEditor, false); if (checkFormat(false) && confirm('<fmt:message key='source.editor.action.publish_source.confirm'/>'))  return true; return false;"
-									><fmt:message key='source.editor.action.publish_source'/></button>
									
<% if ((null != session.sourceid) && !session.sourceid.equalsIgnoreCase("")) { %>
								<button name="deleteDocs" value="deleteDocs" 
									onclick="switchToEditor(sourceJsonEditor, false); if (confirm('<fmt:message key='source.editor.action.delete_docs.confirm'/>')) return true; return false;"
									><fmt:message key='source.editor.action.delete_docs'/></button>
<% } %>
							</td>		
						</tr>
<% } %>
						<tr>
							<td bgcolor="white" width="30%"><fmt:message key='source.editor.title.title'/></td>
							<td bgcolor="white" width="70%"  colspan="2">
								<input type="text" id="shareTitle" name="shareTitle" value="<%=org.apache.commons.lang.StringEscapeUtils.escapeHtml(session.shareTitle)%>" size="60" />
							</td>		
						</tr>
						<tr>
							<td bgcolor="white" width="30%"><fmt:message key='source.editor.share_id.title'/></td>
							<td bgcolor="white" width="70%"  colspan="2">
								<input type="text" id="shareId" name="shareId" value="<%=session.shareid%>" size="35" READONLY />
							</td>		
						</tr>
						<tr valign="top">
							<td bgcolor="white" width="30%"><fmt:message key='source.editor.description.title'/></td>
							<td bgcolor="white" width="70%"  colspan="2">
								<textarea cols="45" rows="3" id="shareDescription" name="shareDescription"><%=session.shareDescription%></textarea>
							</td>		
						</tr>
						<tr>
							<td bgcolor="white" width="30%"><fmt:message key='source.editor.tags.title'/></td>
							
							<td bgcolor="white" width="28%">
								<input type="text" id="shareTags" name="shareTags" value="<%=org.apache.commons.lang.StringEscapeUtils.escapeHtml(session.shareTags)%>" size="60" />
							</td>
							
							<td bgcolor="white" width="42%" align="left">
								<fmt:message key='source.editor.mediaType.title'/>
								<select id="shareMediaType" name="shareMediaType">
									<%= session.mediaTypeSelect %>
								</select>
							</td>									
						</tr>
						<tr>
							<td bgcolor="white" width="30%"><fmt:message key='source.editor.owner.title'/></td>
							<td bgcolor="white" width="70%" colspan="2"><%=session.shareOwnerName%> - <%=session.shareOwnerEmail%></td>		
						</tr>
						<tr>
							<td bgcolor="white" width="30%"><fmt:message key='source.editor.community.title'/></td>
							<td bgcolor="white" width="70%" colspan="2"><%=session.communityIdSelect%></td>		
						</tr>
						<tr>
							<td bgcolor="white" width="30%"><fmt:message key='source.editor.test_parameters.title'/></td>
							<td bgcolor="white" width="70%" style="height:21px" colspan="2">
								<fmt:message key='source.editor.params.full_text'/> <input type="checkbox" name="fullText" value="true" <%=session.getFullTextChecked %>/>
								<fmt:message key='source.editor.params.num_docs'/> <input type="text" id="numOfDocs" name="numOfDocs" value="<%=session.numberOfDocuments %>"
									size="3" title="Maximum of 100" />
								<fmt:message key='source.editor.params.update_mode'/> <input type="checkbox" name="testUpdateLogic" value="true" <%=session.getTestUpdateLogicChecked %>/>							
							</td>		
						</tr>
					</table>
					</div>
					<div id="Bottom" class="Pane">					
						<input type="button" title="<fmt:message key='source.code.show_full_source.tooltip'/>" style="font-weight:bold" onclick="switchToEditor(sourceJsonEditor)" id="toJson" value="JSON" />
<%
						// If in pipelineMode 
						if (session.pipelineMode)
						{
%>
						<input type="button" title="<fmt:message key='source.code.show_js.tooltip'/>" onclick="switchToEditor(sourceJsonEditor_uah)" id="toJs" value="JS" <%=session.sourceOnyShowLogstash %> />
						<input type="button" title="<fmt:message key='source.code.show_logstash.tooltip'/>" onclick="switchToEditor(sourceJsonEditor_logstash)" id="toJsLog" value="LS" <%=session.sourceShowLogstash%> />
<%
						// If in pipelineMode 
						if (session.enterpriseMode)
						{
%>
						<input type="button" title="<fmt:message key='source.code.show_ui.tooltip'/>" onclick="showSourceBuilder()" id="toUI" value="SRC UI"/>
<% } // (end enterpriseMode) %>
<% } else { %>
						<input type="button" title="<fmt:message key='source.code.show_uah.tooltip'/>" onclick="switchToEditor(sourceJsonEditor_uah)" id="toJsU" value="JS-U" />
						<input type="button" title="<fmt:message key='source.code.show_sah.tooltip'/>" onclick="switchToEditor(sourceJsonEditor_sah)" id="toJsS" value="JS-S" />
						<input type="button" title="<fmt:message key='source.code.show_rss.tooltip'/>" onclick="switchToEditor(sourceJsonEditor_rss)" id="toJsRss" value="JS-RSS" <%=session.sourceShowRss%> />
<% } // (end pipelineMode) %>
<%
						// If in flowBuilderMode 
						if (session.flowBuilderMode)
						{
%>
						<input type="button" title="<fmt:message key='source.code.show_flowbuilder.tooltip'/>" onclick="showFlowBuilder()" id="flowbuilderUI" value="FLOW UI" />
<% } // (end flowBuilderMode) %>

<%
						if (session.bucketBuilderMode)
						{
%>
						<input type="button" title="<fmt:message key='source.code.show_bucketbuilder.tooltip'/>" onclick="showBucketBuilder()" id="bucketbuilderUI" value="CARD UI" />
<% } // (end bucketBuilderMode) %>
						
						<input type="submit" class="rightButton" name="revertSource" value="<fmt:message key='source.code.action.revert'/>" onclick="return confirm('<fmt:message key='source.code.action.revert.confirm'/>');" value="revertSource"/>				
						<input type="button" onclick="checkFormat(true)" value="<fmt:message key='source.code.action.check_format'/>" class="rightButton" />
						<input type="button" title="<fmt:message key='source.code.action.scrub.tooltip'/>" onclick="removeStatusFields()" value="<fmt:message key='source.code.action.scrub'/>" class="rightButton" />
<%
						// If in pipelineMode 
						if (!session.pipelineMode)
						{
%>
						<input type="button" title="<fmt:message key='source.code.action.convert.tooltip'/>" onclick="if (confirm('<fmt:message key='source.code.action.convert.confirm'/>')) convertOldSource();" value="<fmt:message key='source.code.action.convert'/>" class="rightButton" />
<% } // (end !pipelineMode) %>
						<textarea cols="90" rows="25" id="Source_JSON" name="Source_JSON"><%=session.sourceJson%></textarea>
						<textarea id="Source_JSON_uahScript" name="Source_JSON_uahScript"></textarea>
						<textarea id="Source_JSON_sahScript" name="Source_JSON_sahScript"></textarea>
						<textarea id="Source_JSON_rssScript" name="Source_JSON_rssScript"></textarea>
						<textarea id="Source_JSON_logstash" name="Source_JSON_logstash"></textarea>
					</div>
					</div>
				</td>
			</tr>
			</table>
		</div><!--  Right -->
	</div><!-- lrSplitter -->
	<input type="hidden" name="sourceid" id="sourceid" value="<%=session.sourceid%>"/>
	
	<%@ include file="inc/footer.jsp" %>

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
	
	var sourceJsonEditor_logstash = CodeMirror.fromTextArea(document.getElementById("Source_JSON_logstash"), {
		mode: "ruby",
		lineNumbers: true,
		matchBrackets: true,
		indentUnit: 4,
		extraKeys: { "Tab": "indentAuto", "Ctrl-Q": function(cm){foldFunc(cm, cm.getCursor().line);}}
	});
	sourceJsonEditor_logstash.setSize("100%", "100%");
	sourceJsonEditor_logstash.display.wrapper.style.display = "none";
	sourceJsonEditor_logstash.on("gutterClick", foldFunc);
	
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

<%
	// If in pipelineMode and enterpriseMode
	if (session.pipelineMode && session.enterpriseMode)
	{
%>
	<div id="sourceBuilder_overlay" 
			style="width: 100%; height: 100%; position:absolute; top: 0px; left: 0px; z-index: 999; opacity: .5; background-color: Black; display: none;"
			onclick="hideSourceBuilder()"
			>
	</div>
	<!--  Don't hide and don't make (0,0) in size - because then the flash contents won't load -->
	<div id="sourceBuilder" style="width: 10px; height: 10px; position:absolute; top: 50px; left: 5%; z-index: -1; ">
		<iframe id="InfinitIframe" src="../infinit.e.source.builder/Infinit.html" style="width: 100%; height: 100%;"></iframe>
	</div> 
<% } %>
<%
	// If in flowBuilderMode
	if (session.flowBuilderMode)
	{
%>
	<!-- Location of this Div to be moved in order to facilitate user access restrictions -->
	<div id="flowBuilder_overlay" 
			style="width: 100%; height: 100%; position:absolute; top: 0px; left: 0px; z-index: 999; opacity: .5; background-color: Black; display: none;"
			onclick="hideFlowBuilder()"
			>
	</div>
	<div id="flowBuilder" style="width: 10px; height: 10px; position:absolute; top: 10px; left: 10px; right: 10px; right: 10px; z-index: -1; ">
		<!-- Flowbuilder webapp location to be moved from localhost -->
		<a class="flowbuilderIframeClose" id="flowbuilderIframeClose" onclick="hideFlowBuilder()">Return to Editor</a>
		<iframe id="flowbuilderInfinitIframe" src="/infinit.e.flow.builder/" style="width: 100%; height: 100%;"></iframe> <!-- dev -->
	</div>
<% } %>
<%
	// If in bucketBuilderMode
	if (session.bucketBuilderMode)
	{
%>
	<!-- Location of this Div to be moved in order to facilitate user access restrictions -->
	<div id="bucketBuilder_overlay" 
			style="width: 100%; height: 100%; position:absolute; top: 0px; left: 0px; z-index: 999; opacity: .5; background-color: Black; display: none;"
			onclick="hideBucketBuilder()"
			>
	</div>
	<div id="bucketBuilder" style="width: 10px; height: 10px; position:absolute; top: 10px; left: 10px; right: 10px; right: 10px; z-index: -1; ">
		<!-- Bucketbuilder webapp location to be moved from localhost -->
		<a class="bucketbuilderIframeClose" id="bucketbuilderIframeClose" onclick="hideBucketBuilder()" style="color:white">Return to Editor</a>
		<iframe id="bucketbuilderInfinitIframe" src="/aleph2_bucket_builder/assets/html/embed_index.html#/home" style="width: 100%; height: 100%;"></iframe> <!-- dev -->
	</div>
<% } %>

</body>
</html>


<%!

// saveShare - 
private void saveShare(HttpServletRequest request, HttpServletResponse response, Session session) 
{
	// (exceptions out nicely if called without a valid share, ie from filter when nothing is selected)
	
	try 
	{
		String oldId = session.formShareId;
		
		String apiAddress = "";
		String urlShareTitle = URLEncoder.encode(session.shareTitle.trim(), "UTF-8");
		String urlShareDescription = URLEncoder.encode(session.shareDescription.trim(), "UTF-8");
		
		if (oldId != null)
		{
			apiAddress = "social/share/update/json/" + oldId + "/source/" + urlShareTitle + "/$desc?desc=" + urlShareDescription;
		}
		else
		{
			apiAddress = "social/share/add/json/source/" + urlShareTitle + "/$desc?desc=" + urlShareDescription;
		}
		
		//
		JSONObject source = new JSONObject(session.sourceJson);
		source.remove("title");
		source.put("title", session.shareTitle.trim());
		source.remove("tags");
		String trimmedShareTags = session.shareTags.trim();
		if (!trimmedShareTags.isEmpty())
			source.put("tags", new JSONArray(trimmedShareTags.split("(?:\\s*,\\s*|\\s+)")));
		source.remove("description");
		source.put("description", session.shareDescription.trim());
		if (!session.shareMediaType.equalsIgnoreCase("null")) {
			source.put("mediaType", session.shareMediaType);
		}
		
		// CommunityID Array - Delete and replace with id from community id dropdown list
		if (session.communityId.length() > 0)
		{
			source.remove("communityIds");
			JSONArray communityIds = new JSONArray();
			communityIds.put(session.communityId);
			source.put("communityIds", communityIds);
		} //TESTED
		session.sourceJson = source.toString(4);

		// Post the update to our rest API and check the results of the post
		JSONObject json_response = new JSONObject(postToRestfulApi(apiAddress, session.sourceJson, request, response)).getJSONObject("response");
		
		if (json_response.getString("success").equalsIgnoreCase("true")) 
		{
			session.messageToDisplay = (String)request.getAttribute("locale_SourceResult_Success") + json_response.getString("message");
		}
		else
		{
			session.messageToDisplay = (String)request.getAttribute("locale_SourceResult_Error") + json_response.getString("message");
		}
	} 
	catch (Exception e) 
	{
		session.messageToDisplay = (String)request.getAttribute("locale_SourceResult_Error") + e.getMessage() + " " + e.getStackTrace().toString();
	}
} // TESTED


// publishSource - 
// 1. Add/update ingest.source object
// 2. Delete the share object, shazam
private boolean publishSource(HttpServletRequest request, HttpServletResponse response, Session session) 
{
	try 
	{
		JSONObject source = new JSONObject(session.sourceJson);
		source.remove("title");
		source.put("title", session.shareTitle.trim());
		source.put("tags", session.shareTags.split("(?:\\s*,\\s*|\\s+)"));
		if (!session.shareMediaType.equalsIgnoreCase("null")) {
			source.put("mediaType", session.shareMediaType);
		}
		source.remove("description");
		source.put("description", session.shareDescription.trim());
		// CommunityID Array - Delete and replace with id from community id dropdown list
		if (session.communityId.length() > 0)
		{
			source.remove("communityIds");
			JSONArray communityIds = new JSONArray();
			communityIds.put(session.communityId);
			source.put("communityIds", communityIds);
		} //TESTED
		// Always remove from quarantine:
		source.put("harvestBadSource", false);
		session.sourceJson = source.toString(4);
		
		String sourceApiString = "config/source/save/" + session.communityId;
		
		// Post the update to our rest API and check the results of the post
		JSONObject result = new JSONObject(postToRestfulApi(sourceApiString, session.sourceJson, request, response));
		JSONObject JSONresponse = result.getJSONObject("response");
		
		if (JSONresponse.getString("success").equalsIgnoreCase("true")) 
		{
			session.messageToDisplay = (String)request.getAttribute("locale_SourceResult_Success") + JSONresponse.getString("message");
			// Delete the share object - shareId
			String apiAddress = "social/share/remove/" + session.shareid;

			// Post the update to our rest API and check the results of the post
			JSONObject shareResponse = new JSONObject(callRestfulApi(apiAddress, request, response)).getJSONObject("response");
			if (shareResponse.getString("success").equalsIgnoreCase("true")) 
			{
				session.messageToDisplay += " (" + shareResponse.getString("message") + ")";
			}
			else
			{
				session.messageToDisplay += " (" + shareResponse.getString("message") + ")";
			}
			return true;
		}
		else
		{
			session.messageToDisplay = (String)request.getAttribute("locale_SourceResult_Error") + JSONresponse.getString("message");
			return false;
		}
	} 
	catch (Exception e) 
	{
		session.messageToDisplay = (String)request.getAttribute("locale_SourceResult_Error") + e.getMessage() + " " + e.getStackTrace().toString();
		return false;
	}
} // 



// saveSourceAsTemplate - 
private void saveShareAsTemplate(HttpServletRequest request, HttpServletResponse response, Session session) 
{
	try 
	{
		JSONObject source = new JSONObject(session.sourceJson);
		source.remove("title");
		source.put("title", session.shareTitle.trim());
		source.put("tags", session.shareTags.split("(?:\\s*,\\s*|\\s+)"));
		source.remove("description");
		source.put("description", session.shareDescription.trim());
		if (!session.shareMediaType.equalsIgnoreCase("null")) {
			source.put("mediaType", session.shareMediaType);
		}

		// Remove any non-functional things:
		source.remove("_id");
		source.remove("communityIds");
		source.remove("created");
		source.remove("harvest");
		source.remove("harvestBadSource");
		source.remove("isApproved");
		source.remove("key");
		source.remove("modified");
		source.remove("ownerId");
		source.remove("shah256Hash");
		
		session.sourceJson = source.toString(4);
		
		String urlShareTitle = URLEncoder.encode(session.shareTitle + " - Template", "UTF-8");
		String urlShareDescription = URLEncoder.encode(session.shareDescription, "UTF-8");
		String apiAddress = "social/share/add/json/source_template/" + urlShareTitle + "/$desc?desc=" + urlShareDescription;
		
		JSONObject JSONresponseTop = new JSONObject(postToRestfulApi(apiAddress, session.sourceJson, request, response));
		JSONObject JSONresponseStatus = JSONresponseTop.getJSONObject("response");
		if (JSONresponseStatus.getString("success").equalsIgnoreCase("true")) 
		{
			session.messageToDisplay = (String)request.getAttribute("locale_SourceResult_Success") + JSONresponseStatus.getString("message");
		}
		else
		{
			session.messageToDisplay = (String)request.getAttribute("locale_SourceResult_Error") + JSONresponseStatus.getString("message");
		}
		Object responseData = JSONresponseTop.get("data");
		String shareId = null;
		if (null != responseData) {
			if (responseData instanceof String) {
				shareId = (String) responseData;
			}
			else if (responseData instanceof JSONObject) {
				Object shareIdObj = ((JSONObject)responseData).get("_id");
				if (null != shareIdObj) {
					shareId = shareIdObj.toString();
				}
			}
		}
		
		// Now share the source vs the selected community
		if (null != shareId) {
			apiAddress = "social/share/add/community/" + shareId + "/Created+As+Template/" + session.communityId;
			callRestfulApi(apiAddress, request, response);
		}
		
	} 
	catch (Exception e) 
	{
		session.messageToDisplay = (String)request.getAttribute("locale_SourceResult_Error") + e.getMessage() + " " + e.getStackTrace().toString();
	}
} //



// deleteShare -
private boolean deleteShare(String shareId, HttpServletRequest request, HttpServletResponse response, Session session)
{
	if (shareId != null && shareId != "") 
	{
		JSONObject source = getSourceJSONObjectFromShare(shareId, request, response, session);
		String apiAddress = "social/share/remove/" + shareId + "/";
		try 
		{
			JSONObject JSONresponse = new JSONObject(callRestfulApi(apiAddress, request, response)).getJSONObject("response");
			if (JSONresponse.getString("success").equalsIgnoreCase("true")) 
			{
				session.messageToDisplay = (String)request.getAttribute("locale_SourceResult_Success") + JSONresponse.getString("message");
				return true;
			}
			else
			{
				session.messageToDisplay = (String)request.getAttribute("locale_SourceResult_Error") + JSONresponse.getString("message");
				return false;
			}
		}
		catch (Exception e)
		{
			session.messageToDisplay = (String)request.getAttribute("locale_SourceResult_Error") + e.getMessage() + " " + e.getStackTrace().toString();
			return false;
		}
	}
	return false;
} // TESTED


// getSourceJSONObject
private JSONObject getSourceJSONObjectFromShare(String shareId, HttpServletRequest request, HttpServletResponse response, Session session)
{
	try
	{
		// Call the api and get the result as a string
		String result = getShare(shareId, request, response);
		
		// Convert string to JSONObjects
		JSONObject json_response = new JSONObject(result);
		JSONObject data = json_response.getJSONObject("data");
		
		// Get the share object and make sure it is encoded properly for display
		session.shareJson = data.toString();
		return new JSONObject(data.getString("share"));
	}
	catch (Exception e)
	{
		return null;
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
			// Call the api and get the result as a string
			String result = getShare(id, request, response);
			
			// Convert string to JSONObjects
			JSONObject json_response = new JSONObject(result);
			JSONObject data = json_response.getJSONObject("data");
			
			// Get the share object and make sure it is encoded properly for display
			session.shareJson = data.toString();
			JSONObject source = new JSONObject(data.getString("share"));
			JSONObject owner = data.getJSONObject("owner");
			
			session.pipelineMode = source.has("processingPipeline");
			
			try
			{
				session.communityId = source.getJSONArray("communityIds").getString(0);
			}
			catch (Exception ex) { }
			
			// Copy fields to the edit source form
			session.sourceJson = source.toString(4); // Formatted with indents for display
			if (source.has("_id")) {
				session.sourceid = source.getString("_id");
			}
			if (source.has("title")) {
				session.shareTitle = data.getString("title");
			}
			if ((null == session.shareTitle) || session.shareTitle.isEmpty()) {
				session.shareTitle = source.getString("title");				
			}
			if (source.has("mediaType")) {
				session.shareMediaType = source.getString("mediaType");
			}
			if ((null == session.shareMediaType) || session.shareMediaType.isEmpty()) {
				session.shareMediaType = "null";
			}
			if (source.has("tags")) {
				StringBuilder stags = new StringBuilder();
				JSONArray arrTags = source.getJSONArray("tags");
				for (int i = 0; i < arrTags.length(); ++i) {
					stags.append(arrTags.get(i)).append(' ');
				}
				session.shareTags = stags.toString();
			}
			if (source.has("description")) {
				session.shareDescription = data.getString("description");
			}
			if ((null == session.shareDescription) || session.shareDescription.isEmpty()) {
				session.shareDescription = source.getString("description");				
			}
			if (source.has("searchCycle_secs")) {
				int searchCycle_secs = source.getInt("searchCycle_secs");
				if (searchCycle_secs >= 0) {
					session.enableOrDisable = (String) request.getAttribute("localized_DisableSource");
				}
				else {
					session.enableOrDisable = (String) request.getAttribute("locale_EnableSource");
				}
			} 
			else {
				session.enableOrDisable = (String) request.getAttribute("localized_DisableSource");
			}
					
			String shareType = data.getString("type");
			if (!shareType.equalsIgnoreCase("source_template")) 
			{
				session.shareid = data.getString("_id");
				session.shareOwnerName = owner.getString("displayName");
				session.shareOwnerEmail = owner.getString("email");
				session.shareCreated = data.getString("created");
				session.shareModified = data.getString("modified");
			}
			//
			else
			{
				session.shareid = "";
				session.shareJson = "";
			}
			
			// Finally, decide whether to show JS-RSS tab
			session.sourceShowRss = "style=\"display: none\";";
			session.sourceShowLogstash = "style=\"display: none\";";
			session.sourceOnyShowLogstash = "";
			try {
				String sourceType = source.getString("extractType"); 
				if ((null != sourceType) && sourceType.equalsIgnoreCase("Feed")) {
					JSONObject rss = source.getJSONObject("rss");
					if (null != rss) {
						JSONObject searchConfig = rss.getJSONObject("searchConfig");
						if (null != searchConfig) {
							session.sourceShowRss = "";
						}
					}
				}
				if ((null != sourceType) && sourceType.equalsIgnoreCase("Logstash")) {
					session.sourceShowLogstash = "";
					session.sourceOnyShowLogstash = "style=\"display: none\";";
				}
			}catch (Exception e) {} // do nothing, this block doesn't exist
		} 
		catch (Exception e) 
		{
			session.sourceJson = (String)request.getAttribute("locale_SourceResult_Error") + e.getMessage();
		}
	}
}  // TESTED



// clearForm
private void clearForm(Session session)
{
	session.shareid = "";
	session.sourceid = "";
	session.shareTitle = "";
	session.shareMediaType = "null";
	session.shareTags = "";
	session.shareDescription = "";
	session.shareType = "";
	session.shareOwnerName = "";
	session.shareOwnerEmail = "";
	session.shareCreated = "";
	session.shareModified = "";
	session.shareJson = "";
	session.sourceJson = "";
}  // TESTED



// listItems -
private String listItems(HttpServletRequest request, HttpServletResponse response, Session session)
{
	StringBuffer sources = new StringBuffer();
	TreeMultimap<String, String> listOfSources = getUserSourcesAndShares(request, response, session.listFilter);
	
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
		int endItem = startItem + session.itemsToShowPerPage - 1;
		if (session.currentPage > 1)
		{
			startItem = ( ( session.currentPage - 1 ) * session.itemsToShowPerPage ) + 1;
			endItem = ( startItem + session.itemsToShowPerPage ) - 1;
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
					String prefixedid = id;
					String editLink = "";
					String deleteLink = "";
					String listFilterString = "";
					if (session.listFilter.length() > 0) listFilterString = "&listFilterStr="+ session.listFilter;
					
					if (name.endsWith(" (*)"))
					{
						editLink = "<a href=\"sources.jsp?action=edit&shareid=" + id + "&page=" + session.currentPage 
								+ listFilterString + "\" title=\""+(String)request.getAttribute("locale_SourceList_EditShare")+"\">" + name + "</a>";
	
						deleteLink = "<a href=\"sources.jsp?action=delete&shareid=" + id + "&page=" + session.currentPage 
								+ listFilterString + "\" title=\""+(String)request.getAttribute("locale_SourceList_DeleteShare")+"\" "
								+ "onclick='return confirm(\""+(String)request.getAttribute("locale_SourceList_DeleteShare_Confirm")+" "
								+ name.replace("\"", "\\\"") + "?\");'><img src=\"image/minus_button.png\" border=0></a>";
					}
					else
					{
						prefixedid = "_" + id; // (so we know in the JSP what we're deleting...)
						editLink = "<a href=\"sources.jsp?action=sharefromsource&sourceid=" + id + "&page=" + session.currentPage 
								+ listFilterString + "\" title=\""+(String)request.getAttribute("locale_SourceList_CreateShare")+"\">" + name + "</a>";
								
						deleteLink = "<a href=\"sources.jsp?action=deletesource&sourceid=" + id + "&page=" + session.currentPage 
								+ listFilterString + "\" title=\""+(String)request.getAttribute("locale_SourceList_DeleteSource")+"\" "
								+ "onclick='return confirm(\""+(String)request.getAttribute("locale_SourceList_DeleteSource_Confirm")+" "
								+ name.replace("\"", "\\\"") + "?\");'><img src=\"image/delete_x_button.png\" border=0></a>";
					}
		
					// Create the HTML table row
					sources.append("<tr valign=\"top\">");
					if (id.equals(session.shareid)) {
						sources.append("<td bgcolor=\"white\" width=\"100%\"><b>" + editLink + "</b></td>");						
					}
					else {
						sources.append("<td bgcolor=\"white\" width=\"100%\">" + editLink + "</td>");
					}
					sources.append("<td align=\"center\" bgcolor=\"white\"><input type=\"checkbox\" name=\"sourcesToProcess\" value=\"" + prefixedid + "\"/></td>");
					sources.append("<td align=\"center\" bgcolor=\"white\">" + deleteLink + "</td>");
					sources.append("</tr>");
				}
				currentItem++;
			}
		}
		
		sources.append("<tr valign=\"top\">");
		sources.append("<td bgcolor=\"white\" width=\"100%\" colspan=\"3\">");
		sources.append("(*) " + (String)request.getAttribute("locale_SourceList_TempCopy") + "<br>");
		sources.append("(+) " + (String)request.getAttribute("locale_SourceList_OtherOwner"));
		sources.append("</td>");
		sources.append("</tr>");
		
		// Calculate number of pages, current page, page links...
		sources.append("<tr><td colspan=\"2\" align=\"center\" class=\"subTableFooter\">");
		// --------------------------------------------------------------------------------
		// Create base URL for each page
		StringBuffer baseUrl = new StringBuffer();
		baseUrl.append("sources.jsp?action=page");
		if (session.listFilter.length() > 0) baseUrl.append('&').append("listFilterStr=").append(session.listFilter);
		if (session.shareid.length() > 0) baseUrl.append('&').append("shareid=").append(session.shareid);
		baseUrl.append("&page=");
		sources.append( createPageString( sortedKeys.size(), session.itemsToShowPerPage, session.currentPage, baseUrl.toString() ));
		sources.append("</td></tr>");
		// --------------------------------------------------------------------------------
		sources.append("</table>");
	}
	else
	{
		sources.append(request.getAttribute("locale_SourceList_NoSources"));
	}

	return sources.toString();
}


// createShareFromSource
private String createShareFromSource(String sourceId, HttpServletRequest request, HttpServletResponse response, Session session)
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
		String apiAddress = "social/share/add/json/source/" + urlShareTitle + "/$desc?desc=" + urlShareDescription;
		JSONObject jsonObject = new JSONObject( postToRestfulApi(apiAddress, sourceJson.toString(4), request, response) );
		JSONObject json_response = jsonObject.getJSONObject("response");
		JSONObject json_data = new JSONObject ( jsonObject.getString("data") );
		
		//clearForm();
		//populateEditForm(json_data.getString("_id"), request, response, session);
		
		// Return new shareid to caller
		return json_data.getString("_id");
	}
	catch (Exception e)
	{
		System.out.println(e.getMessage());
		return null;
	}
}

// createMediaTypeSelect
private String createMediaTypeSelect(HttpServletRequest request, Session session)
{
	StringBuffer html = new StringBuffer();
	String baseList = (String)request.getAttribute("local_mediaType_values");
	if (null == baseList) { // (default set if none specified)
		baseList = "News, Social, Report, Record, Blog, Intel, Discussion, Video, Imagery";
	}
	String customElement = (String)request.getAttribute("local_mediaType_custom");
	if (null == customElement) {
		customElement = "Custom (enter into JSON below)";
	}
	String[] baseElements = baseList.split("\\s*,\\s*");
	boolean selected = false;
	for (String baseElement: baseElements) {
		if (!selected && baseElement.equalsIgnoreCase(session.shareMediaType)) {
			html.append("<option selected=\"selected\"'>");
			selected = true;
		}
		else {
			html.append("<option>");
		}
		html.append(baseElement).append("</option>\n");
	}
	if (null != customElement) {
		if (!selected) {
			html.append("<option selected=\"selected\" value=\"null\"'>");
		}
		else {
			html.append("<option value=\"null\">");
		}
		html.append(customElement).append("</option>\n");		
	}
	return html.toString();
}


// createCommunityIdSelect -
// Create select control with list of communityids available to user
private void createCommunityIdSelect(HttpServletRequest request, HttpServletResponse response, Session session) 
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
				if (source.has("type") && source.getString("type").equals("user")) {
					continue;
				}
				String selectedString = (id.equalsIgnoreCase(session.communityId)) ? " SELECTED" : "";
				html.append("<option value=\"" + id + "\"" + selectedString + ">" + name + "</option>");
			}				
		}
		html.append("</select>");
		session.communityIdSelect = html.toString();
	} 
	catch (Exception e) 
	{
	}
} // TESTED



// testSource -
private void testSource(HttpServletRequest request, HttpServletResponse response, Session session)
{
	int numDocs = 10;
	try
	{
		numDocs = Integer.parseInt(session.numberOfDocuments);
		if (numDocs < 1 || numDocs > 100) numDocs = 10;
	}
	catch (Exception e)
	{
		numDocs = 10;
	}
	String apiAddress = "config/source/test?returnFullText=" + session.getFullText + "&numReturn=" + String.valueOf(numDocs) + "&testUpdates=" + session.getTestUpdateLogic;
	session.harvesterOutput = "";
	session.messageToDisplay = "";
	
	try 
	{
		// (just used for )
		JSONObject source = new JSONObject(session.sourceJson);
		session.pipelineMode = source.has("processingPipeline");
		String newCommunity = null;
		
		// Overwrite the community id if that is required:
		// (the tabs/type/title etc are still out of date, but that doesn't typically result in problems so we'll live with that)
		if (!source.has("communityIds")) { 
			newCommunity = session.communityId;
		}//TESTED
		else { // check community vs dropdown
			JSONArray com = source.getJSONArray("communityIds");
			if (1 == com.length()) {
				String tempCommunityId = com.getString(0);
				if (!session.communityId.equals(tempCommunityId)) {
					newCommunity = session.communityId;
				}
			}//TESTED
			else {
				newCommunity = session.communityId;
			}//TESTED
		}
		if (null != newCommunity) {
			JSONArray communityIds = new JSONArray();
			communityIds.put(newCommunity);
			source.put("communityIds", communityIds);
			session.sourceJson = source.toString(4);			
		}//TESTED
		
		JSONObject jsonObject = new JSONObject(postToRestfulApi(apiAddress, session.sourceJson, request, response));
		JSONObject JSONresponse = jsonObject.getJSONObject("response");
		
		try
		{
			if (source.has("searchCycle_secs")) {
				int searchCycle_secs = source.getInt("searchCycle_secs");
				if (searchCycle_secs >= 0) {
					session.enableOrDisable = (String) request.getAttribute("localized_DisableSource");
				}
				else {
					session.enableOrDisable = (String) request.getAttribute("locale_EnableSource");
				}
			} 
			else {
				session.enableOrDisable = (String) request.getAttribute("localized_DisableSource");
			}
			
			// Finally, decide whether to show JS-RSS tab
			session.sourceShowRss = "style=\"display: none\";";
			session.sourceShowLogstash = "style=\"display: none\";";
			session.sourceOnyShowLogstash = "";
			try {
				String sourceType = source.getString("extractType"); 
				if ((null != sourceType) && sourceType.equalsIgnoreCase("Feed")) {
					JSONObject rss = source.getJSONObject("rss");
					if (null != rss) {
						JSONObject searchConfig = rss.getJSONObject("searchConfig");
						if (null != searchConfig) {
							session.sourceShowRss = "";
						}
					}
				}
				if ((null != sourceType) && sourceType.equalsIgnoreCase("Logstash")) {
					session.sourceShowLogstash = "";
					session.sourceOnyShowLogstash = "style=\"display: none\";";
				}
			}catch (Exception e) {} // do nothing, this block doesn't exist
			
			session.messageToDisplay = JSONresponse.getString("message");
			
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
				
				session.harvesterOutput = s.toString();
			}
		}
		catch (Exception ex)
		{
			session.messageToDisplay = (String)request.getAttribute("locale_SourceResult_Test") + JSONresponse.getString("message");
		}
		if (session.harvesterOutput.length() < 1) session.harvesterOutput = " ";
	}
	catch (Exception e)
	{
		session.messageToDisplay = (String)request.getAttribute("locale_SourceResult_Error") + e.getMessage() + " " + e.getStackTrace().toString();
	}
} // TESTED


// deleteSourceObject -
private boolean deleteSourceObject(String sourceId, boolean bDocsOnly, HttpServletRequest request, HttpServletResponse response, Session session)
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
				session.messageToDisplay = (String)request.getAttribute("locale_SourceResult_Success") + JSONresponse.getString("message");
				return true;
			}
			else
			{
				session.messageToDisplay = (String)request.getAttribute("locale_SourceResult_Error") + JSONresponse.getString("message");
				return false;
			}
		}
		catch (Exception e)
		{
			session.messageToDisplay = (String)request.getAttribute("locale_SourceResult_Error") + e.getMessage() + " " + e.getStackTrace().toString();
			return false;
		}
	}
	return false;
}

//suspend/resume source
private boolean suspendSourceObject(String sourceId, boolean shouldSuspend, HttpServletRequest request, HttpServletResponse response, Session session)
{
	if (sourceId != null && sourceId != "") 
	{
		try 
		{
			JSONObject sourceResponse = new JSONObject( getSource(sourceId, request, response) );
			JSONObject source = new JSONObject( sourceResponse.getString("data") );
			JSONArray com = source.getJSONArray("communityIds");
			String tempCommunityId = com.getString(0);
					
			JSONObject JSONresponse = new JSONObject(suspendSource(sourceId, shouldSuspend, tempCommunityId, 
					request, response)).getJSONObject("response");
			
			if (JSONresponse.getString("success").equalsIgnoreCase("true")) 
			{				
				return true;
			}
			else
			{				
				return false;
			}
		}
		catch (Exception e)
		{			
			return false;
		}
	}
	return false;
}

private String getSourceId(String id, HttpServletRequest request, HttpServletResponse response, Session session)
{
	String sourceId = null;
	if (id.startsWith("_")) 
	{
		//is just a source, send this id
		sourceId = id.substring(1);
	}
	else
	{
		try
		{
			//share objects that are connected to sources will have an _id so get
			//the share object and check for that
			JSONObject source = getSourceJSONObjectFromShare(id, request, response, session);
			if ( source != null && source.has("_id") )
			{
				sourceId = source.getString("_id");
			}
		}
		catch (Exception ex)
		{
			//do nothing, let it be null
		}
	}
	return sourceId;
}


%>
