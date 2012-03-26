<%@page import="java.net.FileNameMap"%>
<%@page import="java.net.URLConnection"%>

<%@page import="org.apache.jasper.JasperException"%>
<%@page import="javax.script.ScriptException"%>
<%@page import="javax.script.ScriptEngineManager"%>
<%@page import="javax.script.ScriptEngine"%>
<%@page import="org.apache.commons.fileupload.util.Streams"%>
<%@page import="org.apache.commons.fileupload.FileItemStream"%>
<%@page import="org.apache.commons.fileupload.FileItemIterator"%>
<%@page import="org.apache.commons.fileupload.disk.DiskFileItemFactory"%>
<%@page import="org.apache.commons.fileupload.servlet.ServletFileUpload"%>
<%@ page contentType="text/html; charset=utf-8" language="java" import="java.io.*, java.util.*,java.net.*,com.google.gson.Gson, org.apache.commons.io.*,sun.misc.BASE64Encoder,java.security.*;" errorPage="" %>
<%!
	static String API_ROOT = null;
	static String SHARE_ROOT = null;
	static Boolean showAll = false;
	static Boolean localCookie = false;
	static String user = null;
	static CookieManager cm = new CookieManager();

	static class keepAlive
	{
		static class ka
		{
			String action;
			Boolean success;
			String message;
			int time;
		
		}
		public ka response;
		
	}
	static class getShare
	{
		static class shareResponse
		{
			String action;
			Boolean success;
			String message;
			int time;
		
		}
		static class shareOwner
		{
			String _id;
			String email;
			String displayName;
		}
		static class shareCommunity
		{
			String _id;
			String name;
			String comment;
		}
		static class shareData
		{
			String _id;
			String created;
			String modified;
			shareOwner owner;
			String type;
			String title;
			String description;
			String mediaType;
			shareCommunity[] communities;
			String binaryID;
		
		}
		public shareResponse response;
		public shareData[] data;
		
	}
	static class getModules
	{
		static class widRes
		{
			String action;
			Boolean success;
			String message;
			int time;
		
		}
		public widRes response;
		
	}
	static class logIn
	{
		static class loginData
		{
			public String action;
			public Boolean success;
			public int time;
			
		}
		public loginData response;
	}
	static class modResponse
	{
		static class moduleResponse
		{
			public String action;
			public Boolean success;
			public String message;
			public int time;
			
		}
		public moduleResponse response;
		public String data;
	}
	
	static class widgetToDBResponse
	{
		static class wtdbResponse
		{
			public String action;
			public Boolean success;
			public String message;
			public int time;
			
		}
		static class wtdbData
		{
			public String _id;
			public Boolean approved;
		}
		public wtdbResponse response;
		public wtdbData data;
	}
	
	public static void setBrowserInfiniteCookie(HttpServletResponse response, String value)
	{
		//System.out.println("Set Browser Cookie to " + value);
		Cookie cookie = new Cookie ("infinitecookie",value);
		cookie.setPath("/");
		response.addCookie(cookie);
	}
	
	public static String getBrowserInfiniteCookie(HttpServletRequest request)
	{
		Cookie[] cookieJar = request.getCookies();
		if ( cookieJar != null)
		{
			for( Cookie cookie : cookieJar)
			{
				if (cookie.getName().equals("infinitecookie"))
				{
					//System.out.println("Got Browser Cookie Line 109: " + cookie.getValue());
					return cookie.getValue() + ";";
				}
			}
		}
		return null;
	}
	
	public static String getConnectionInfiniteCookie(URLConnection urlConnection)
	{
		Map<String, List<String>> headers = urlConnection.getHeaderFields();
    	Set<Map.Entry<String, List<String>>> entrySet = headers.entrySet();
    	
    	for (Map.Entry<String, List<String>> entry : entrySet) 
    	{
            String headerName = entry.getKey();
			if ( headerName != null && headerName.equals("Set-Cookie"))
			{
				List<String> headerValues = entry.getValue();
	            for (String value : headerValues) 
	            {
	            	if (value.contains("infinitecookie"))
	            	{
	            		int equalsLoc = value.indexOf("=");
	            		int semicolonLoc = value.indexOf(";");
	            		//System.out.println("Got Connection Cookie Line 133: " + value.substring(equalsLoc+1,semicolonLoc));
	            		return value.substring(equalsLoc+1,semicolonLoc);
	            	}
	            }
			}  
		}
    	return null;
	}
	
	public static String stringOfUrl(String addr, HttpServletRequest request, HttpServletResponse response)
	{
		//if(localCookie)
			CookieHandler.setDefault(cm);
        try
        {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
        	URL url = new URL(addr);
        	URLConnection urlConnection = url.openConnection();

        	String cookieVal = getBrowserInfiniteCookie(request);
        	if (cookieVal != null)
        	{
        		urlConnection.addRequestProperty("Cookie","infinitecookie=" + cookieVal);
        		urlConnection.setDoInput(true);
        		urlConnection.setDoOutput(true);
        		urlConnection.setRequestProperty("Accept-Charset","UTF-8");
        	}
        	else
        		System.out.println("NULLLLLLLLL");
        	IOUtils.copy(url.openStream(), output);
        	String newCookie = getConnectionInfiniteCookie(urlConnection);
        	if (newCookie != null && response != null)
        	{
        		setBrowserInfiniteCookie(response, newCookie);
        	}
			
        	
        	return output.toString();
        }
        catch(IOException e)
        {
        	return null;
        }
    }
	
	private static String encrypt(String password) throws NoSuchAlgorithmException, UnsupportedEncodingException 
	{	
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		md.update(password.getBytes("UTF-8"));			
		return URLEncoder.encode((new BASE64Encoder()).encode(md.digest()));	
	}
	private Boolean logMeIn(String username, String pword, HttpServletRequest request, HttpServletResponse response ) throws IOException, NoSuchAlgorithmException, UnsupportedEncodingException, URISyntaxException 
    {
		String json = stringOfUrl(API_ROOT + "auth/login/"+username+"/"+encrypt(pword), request, response);
		logIn login = new Gson().fromJson(json, logIn.class);
		if (login == null)
			return false;
		user = username;
		return login.response.success;
    }
	
	private void logOut(HttpServletRequest request, HttpServletResponse response ) throws IOException, NoSuchAlgorithmException, UnsupportedEncodingException, URISyntaxException 
    {
		String json = stringOfUrl(API_ROOT + "auth/logout", request, response);
		
    }
	
	
	public Boolean isLoggedIn(HttpServletRequest request, HttpServletResponse response)
	{
		String json = stringOfUrl(API_ROOT + "auth/keepalive", request, response);
		if (json != null)
		{
			keepAlive keepA = new Gson().fromJson(json, keepAlive.class);
			return keepA.response.success;
		}
		else
		{
			return null;
		}
	}
	
	public String removeFromShare(String shareId, HttpServletRequest request, HttpServletResponse response)
	{
		if (shareId != null)
		{
			String json = stringOfUrl(API_ROOT + "share/remove/" + shareId + "/", request, response);
			if (json != null)
			{
				keepAlive keepA = new Gson().fromJson(json, keepAlive.class);
				return keepA.response.message;
			}
		}
		return null;
	}
	
	
	private String AddToShare(byte[] bytes, String mimeType, String title, String description, HttpServletRequest request, HttpServletResponse response)
	{
		return UpdateToShare(bytes, mimeType, title, description, null, request, response);
	}
	//uploads a new widget's bytes and returns it's shareID if successful. If a share
	//ID is provided, then it updates the widget containing that shareID
	private String UpdateToShare(byte[] bytes, String mimeType, String title, String description, String prevId, HttpServletRequest request, HttpServletResponse response)
	{
		String charset = "UTF-8";
		String url = "";
		
		try{
			if (prevId == null)
				url = API_ROOT + "share/add/binary/" + URLEncoder.encode(title,charset) + "/" + URLEncoder.encode(description,charset) + "/";
			else
				url = API_ROOT + "share/update/binary/" + prevId + "/" + URLEncoder.encode(title,charset) + "/" + URLEncoder.encode(description,charset) + "/";
			
			//if(localCookie)
			CookieHandler.setDefault(cm);
			URLConnection connection = new URL(url).openConnection();
			connection.setDoOutput(true);
	        connection.setRequestProperty("Accept-Charset",charset);
	        String cookieVal = getBrowserInfiniteCookie(request);
        	if (cookieVal != null)
        	{
        		connection.addRequestProperty("Cookie","infinitecookie=" + cookieVal);
        		connection.setDoInput(true);
        		connection.setDoOutput(true);
        		connection.setRequestProperty("Accept-Charset","UTF-8");
        	}
	        if (mimeType != null && mimeType.length() > 0)
	        	connection.setRequestProperty("Content-Type", mimeType + ";charset=" + charset);
	        DataOutputStream output = new DataOutputStream(connection.getOutputStream());
            output.write(bytes);
            DataInputStream responseStream = new DataInputStream(connection.getInputStream());
            
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[16384];
            while ((nRead = responseStream.read(data,0,data.length)) != -1)
            {
                buffer.write(data,0,nRead);
            }
            
            String json = buffer.toString();
            String newCookie = getConnectionInfiniteCookie(connection);
        	if (newCookie != null && response != null)
        	{
        		setBrowserInfiniteCookie(response, newCookie);
        	}
            buffer.flush();
            buffer.close();
            output.close();
            responseStream.close();
            
            modResponse mr = new Gson().fromJson(json, modResponse.class);
    		if (mr == null)
    		{
    			return "Failed: " + json;
    		}
    		if (mr.response.success == true)
    		{
    			if (prevId != null && mr.data == null)
    			{
    				System.out.println("Added Share to community: " + addShareToCommunity(prevId, request, response));
    				return prevId;
    			}
    			System.out.println("Added Share to community: " + addShareToCommunity(prevId, request, response));
    			return mr.data; //When a new upload, mr.data contains the ShareID for the upload
    		}
    		else
    		{
    			return "Upload Failed: " + mr.response.message;
    		}
		}catch(IOException e)
		{
			e.printStackTrace();
			return "Upload Failed: " + e.getMessage();
		}
	}
	
	private String UpdateToShareKeepFile(String title, String description, String prevId, HttpServletRequest request, HttpServletResponse response)
	{
		System.out.println("Accessed UpdateToShareKeepFile");
		try{
			String charset = "UTF-8";
			
			//wfot specific
			String communityId = "4e9c77ef17ef3523b657a890";
			String comment = "Added for file access to all users (Added by fileUploader.jsp)";
			
			String json = stringOfUrl(API_ROOT + "social/share/update/json/" + URLEncoder.encode(prevId,charset) + "/binary/" + URLEncoder.encode(title,charset) + "/" + URLEncoder.encode(description,charset) + "/?json=<>", request, response);
			getModules gm = new Gson().fromJson(json, getModules.class);
			if (gm == null)
				return "Update Failed: returned object was null: " + json;
			System.out.println("Added Share to community: " + addShareToCommunity(prevId, request, response));
			return prevId;
		}catch(IOException e)
		{
			e.printStackTrace();
			return "Update Failed:" + e.getMessage();
		}
	}
	
	private String addShareToCommunity( String shareId, HttpServletRequest request, HttpServletResponse response)
	{
		try{
			String charset = "UTF-8";
			
			//wfot specific
			String communityId = "4e9c77ef17ef3523b657a890";
			String comment = "Added by fileUploader";
			
			///share/add/community/{shareid}/{comment}/{communityid}
			String json = stringOfUrl(API_ROOT + "share/add/community/" + URLEncoder.encode(shareId,charset) + "/" + URLEncoder.encode(comment,charset) + "/" + URLEncoder.encode(communityId,charset) + "/", request, response);
			getModules gm = new Gson().fromJson(json, getModules.class);
			if (gm == null)
				//return false;
				return "Json was null: " + json + "\n " + API_ROOT + "share/add/community/" + URLEncoder.encode(shareId,charset) + "/" + URLEncoder.encode(communityId,charset) + "/" + URLEncoder.encode(comment,charset) + "/";
			return gm.response.message + API_ROOT + "share/add/community/" + URLEncoder.encode(shareId,charset) + "/" + URLEncoder.encode(communityId,charset) + "/" + URLEncoder.encode(comment,charset) + "/";
		}catch(IOException e)
		{
			e.printStackTrace();
			return e.getMessage();
		}
		
	}

	private String populatePreviousUploads(HttpServletRequest request, HttpServletResponse response)
	{
		String toReturn = "";
		String delim = "$$$";
		String json = stringOfUrl(API_ROOT + "share/search/?type=binary&searchby=type", request, response);
		 if (json != null)
		{
			getShare gs = new Gson().fromJson(json, getShare.class);
			for ( getShare.shareData info : gs.data)
			{
				if (request.getParameter("ext") == null)
				{
					String value = info._id+delim+info.created+delim+info.title+delim+info.description+delim+SHARE_ROOT+info._id;
					toReturn += "<option value=\""+value+"\" > <b>Edit:</b> " + info.title + "</option>";
				}
				else
				{
					String ext = request.getParameter("ext").toString();
					
					if (ext.contains("jar") || ext.contains("JAR"))
					{
						if ( info.mediaType.equalsIgnoreCase("application/java-archive") || info.mediaType.equalsIgnoreCase("application/x-java-archive") || info.mediaType.equalsIgnoreCase("application/octet-stream"))
						{
							System.out.println("Outputting Uploaded Jar files");
							String value = info._id+delim+info.created+delim+info.title+delim+info.description+delim+SHARE_ROOT+info._id;
							toReturn += "<option value=\""+value+"\" > <b>Edit:</b> " + info.title + "</option>";
						}
					}
					else if (info.mediaType.contains(ext))
					{
						System.out.println("Extension '" + ext + "' Triggered Mime Type : " + info.mediaType.toString());
						String value = info._id+delim+info.created+delim+info.title+delim+info.description+delim+SHARE_ROOT+info._id;
						toReturn += "<option value=\""+value+"\" > <b>Edit:</b> " + info.title + "</option>";
						
					}
				}
				
			}
		}
		return toReturn; 
	}
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
<title>Infinit.e File Upload Tool</title>
<style media="screen" type="text/css">

body 
{
	font: 14px Arial,sans-serif;
}
h2
{
	font-family: "Times New Roman";
	font-style: italic;
	font-variant: normal;
	font-weight: normal;
	font-size: 24px;
	line-height: 29px;
	font-size-adjust: none;
	font-stretch: normal;
	-x-system-font: none;
	color: #d2331f;
	margin-bottom: 25px;
}

</style>
<script language="javascript" src="AppConstants.js"> </script>
</head>

<body>
<%
if (API_ROOT == null)
{
	ServletContext context = session.getServletContext();
	String realContextPath = context.getRealPath(request.getContextPath());
	ScriptEngineManager manager = new ScriptEngineManager();
	ScriptEngine engine = manager.getEngineByName("javascript");
	try{ // EC2 Machines
		FileReader reader = new FileReader(realContextPath + "/AppConstants.js");
		engine.eval(reader);
		reader.close();
		engine.eval("output = getEndPointUrl();");
		API_ROOT = (String) engine.get("output");
		SHARE_ROOT = API_ROOT + "share/get/";
	}
	catch (Exception je)
	{
		try { ////////////Windows + Tomcat
			FileReader reader = new FileReader(realContextPath + "\\..\\AppConstants.js");
			engine.eval(reader);
			reader.close();
			engine.eval("output = getEndPointUrl();");
			API_ROOT = (String) engine.get("output");
			SHARE_ROOT = API_ROOT + "share/get/";
		}catch (Exception e)
		{
			System.err.println(e.toString());
		}
	}

	
}
Boolean isLoggedIn = isLoggedIn(request, response);
if (isLoggedIn == null)
{
	out.println("The Infinit.e API cannot be reached.");
	out.println(API_ROOT);
}

else if (isLoggedIn == true)
{ 
	if (request.getParameter("logout") != null)
	{
		logOut(request, response);
		out.println("<meta http-equiv=\"refresh\" content=\"0\">");
		
	}
	else
	{
	
	
		out.println("<div style=\" text-align: center;\">");
		String contentType = request.getContentType();
		if ((contentType != null) && (contentType.indexOf("multipart/form-data") >= 0 ))
		{
			
	//		Create a new file upload handler
	 		ServletFileUpload upload = new ServletFileUpload(new DiskFileItemFactory());
	//		Parse the request
	 		FileItemIterator iter = upload.getItemIterator(request);
			byte[] fileBytes = null;
			String fileDS = null;
			byte[] iconBytes = null;
			String iconDS = null;
	 		while (iter.hasNext()) {
	 		    FileItemStream item = iter.next();
	 		    String name = item.getFieldName();
	 		    InputStream stream = item.openStream();
	 		    if (item.isFormField()) {
	 		    	request.setAttribute(name, Streams.asString(stream));
	 		    	
	 		    	//out.println("<b>" + name + ":</b>" + request.getAttribute(name).toString()+"</br>");
	 		    } else 
	 		    {
	 		    	if (name.equalsIgnoreCase("file"))
	 		    	{
	 		    		fileDS = item.getContentType();
	 		    		fileBytes = IOUtils.toByteArray(stream);
	 		    	}
	 		    }
	 		}
	 		
	 		////////////////////////////////////Delete Share ////////////////////////////////
	 		if (request.getAttribute("deleteId") != null)
	 		{
	 			String fileId = request.getAttribute("deleteId").toString();
	 			if (fileId != null && fileId != "")
	 				removeFromShare(fileId, request, response).toString();
	 			
	 		}
	 		else
	 		{
	 		
		 	//////////////////////////////////////////////////////////////////////////////////
		 		
		 		Boolean newUpload =(request.getAttribute("DBId").toString().length() == 0 );
		 		//Boolean keepFileSame =(request.getAttribute("file_check").toString().length() != 0 );
				
				
		 		///////////////////////////////// SWF Manip  /////////////////////////////////
		 		String fileUrl = "";
				String fileId = "";

				if(request.getAttribute("title") != null && request.getAttribute("description") != null && fileBytes != null)
				{
					if (newUpload)
					{
						fileId = AddToShare(fileBytes, fileDS, request.getAttribute("title").toString(),request.getAttribute("description").toString(), request, response);
					}
					//else if (keepFileSame) //use previous link (ignores what the user may have changed it to))
					//{
					//	fileId = UpdateToShareKeepFile(request.getAttribute("title").toString(),request.getAttribute("description").toString(), request.getAttribute("DBId").toString(), request, response);
					//}
					else
					{
						fileId = request.getAttribute("DBId").toString();
						UpdateToShare(fileBytes, fileDS, request.getAttribute("title").toString(),request.getAttribute("description").toString(), fileId, request, response);
					}
					
					if (fileId.contains("Failed"))
					{
						out.println(fileId);
					}
					else
					{
						fileUrl = SHARE_ROOT + fileId;
						if (newUpload)
							out.println("You have successfully added a file to the share, it's location is: " + fileUrl);
						//else if (keepFileSame)
						//	out.println("You have successfully modified the title and description of the file. The file's location is still: " + fileUrl);
						else
							out.println("You have successfully updated a file on the share, it's location is: " + fileUrl);
					}
				}
				else
				{
					fileUrl = null;
					out.println("Error: Not enough information provided for file Upload");
				}

		 		
				///////////////////////////////// End File Manip  /////////////////////////////////
				
				out.println("</div>");
	 		}
		}
		else
		{
		}
	
	%>
	
	<script>
	function populate()
	{

		title = document.getElementById('title');
		description = document.getElementById('description');
		file = document.getElementById('file');
		created = document.getElementById('created');
		DBId = document.getElementById('DBId');
		deleteId = document.getElementById('deleteId');
		deleteButton = document.getElementById('deleteButton');
		//file_check = document.getElementById('file_check');
		//file_provide = document.getElementById('file_provide');
		//file_url = document.getElementById('file_url');
		share_url = document.getElementById('share_url');
		url_row = document.getElementById('url_row');
		dropdown = document.getElementById("upload_info");
		list = dropdown.options[dropdown.selectedIndex].value;
		
		if (list == "new")
		{
			title.value = "";
			description.value = "";
			created.value = "";
			DBId.value = "";
			deleteId.value = "";
			share_url.value = "";
			//file_url.value = "";
			//file_check.checked = false;
			//useUrl();
			url_row.style.display = 'none';
			deleteButton.style.visibility = 'hidden';
			//file_check.style.display = 'none';
			//file_provide.style.display = 'none';
			return;
		}
		//_id, created, title, description
		split = list.split("$$$");
		
		res_id = split[0];
		res_created = split[1];
		res_title = split[2];
		res_description = split[3];
		res_url = split[4];

		
		title.value = res_title;
		description.value = res_description;
		created.value = res_created;
		DBId.value = res_id;
		deleteId.value = res_id;
		//file_url.value = res_url;
		share_url.value = res_url;
		deleteButton.style.visibility = '';
		url_row.style.display = '';
		//file_check.style.display = '';
		//file_check.checked = true;
		//useUrl();
		//file_provide.style.display = '';
	}
	/*
		function useUrl()
		{
			file = document.getElementById('file');
			//file_url = document.getElementById('file_url');
			
			if (document.getElementById('file_check').checked)
			{
				file_url.style.display = "";
				file.style.display = "none";
			}
			else
			{
				file.style.display = "";
				file_url.style.display = "none";
			}
		}
	*/
		function validate_fields()
		{
			title = document.getElementById('title').value;
			description = document.getElementById('description').value;
			file = document.getElementById('file').value;
			//file_url = document.getElementById('file_url').value;
			//file_check = document.getElementById('file_check').checked;
			
			if (title == "")
			{
				alert('Please provide a title.');
				return false;
			}
			if (description == "")
			{
				alert('Please provide a description.');
				return false;
			}
			if (file == "" )
			{
				alert('Please provide your file.');
				return false;
			}
			
		}
		function confirmDelete()
		{
			var agree=confirm("Are you sure you wish to Delete this file from the File Share?");
			if (agree)
				return true ;
			else
				return false ;
		}
		// -->
		</script>
	</script>
		<div id="uploader_outter_div" name="uploader_outter_div" align="center" style="width:100%" >
	    	<div id="uploader_div" name="uploader_div" style="border-style:solid; border-color:#999999; border-radius: 10px; width:450px; margin:auto">
	        	<h2>File Uploader</h2>
	        	<form id="delete_form" name="delete_form" method="post" enctype="multipart/form-data" onsubmit="javascript:return confirmDelete()" >
	        		<select id="upload_info" onchange="populate()" name="upload_info"><option value="new">Upload New File</option> <% out.print(populatePreviousUploads(request, response)); %></select>
	        		<input type="submit" name="deleteButton" id="deleteButton" style="visibility:hidden;" value="Delete" />
	        		<input type="hidden" name="deleteId" id="deleteId" />
	        		<input type="hidden" name="deleteFile" id="deleteFile" />
	        	</form>
	            <form id="upload_form" name="upload_form" method="post" enctype="multipart/form-data" onsubmit="javascript:return validate_fields();" >
	                <table width="100%" border="0" cellspacing="0" cellpadding="0" style="padding-left:10px; padding-right:10px">
	                  <tr>
	                    <td colspan="2" align="center"></td>
	                  </tr>
	                  <tr>
	                    <td>Title:</td>
	                    <td><input type="text" name="title" id="title" size="39" /></td>
	                  </tr>
	                  <tr>
	                    <td>Description:</td>
	                    <td><textarea rows="4" cols="30" name="description" id="description" ></textarea></td>
	                  </tr>
	                  <tr>
	                    <td>File:</td>
	                    <td><input type="file" name="file" id="file" /><!--<input type="text" name="file_url" id="file_url" size="32" style="display:none;" /><input type="checkbox" id="file_check" name="file_check" onchange="useUrl()" style="display:none;" /> <span id="file_provide" name="file_provide" style="display:none;" > Use Existing File </span>--></td>
	                  </tr>
	                  <tr id="url_row" style="display:none">
	                  	<td>Share URL:</td>
	                  	<td><input type="text" name="share_url" id="share_url" readonly="readonly" size="55"/>
	                  	</td>
	                  </tr>
	                  <tr>
	                    <td colspan="2" style="text-align:right"><input type="submit" value="Submit" /></td>
	                  </tr>
	                </table>
					<input type="hidden" name="created" id="created" />
					<input type="hidden" name="DBId" id="DBId" />
					<input type="hidden" name="fileUrl" id="fileUrl" />
				</form>
	        </div>
	        <form id="logout_form" name="logout_form" method="post">
	        	<input type="submit" name="logout" id = "logout" value="Log Out" />
	        </form>
	    </div>
	    </p>
	
<%
	}
}
else if (isLoggedIn == false)
{
	localCookie =(request.getParameter("local") != null);
	//System.out.println("LocalCookie = " + localCookie.toString());
	String errorMsg = "";
	if (request.getParameter("logintext") != null || request.getParameter("passwordtext") != null)
	{
		if(logMeIn(request.getParameter("logintext"),request.getParameter("passwordtext"), request, response))
		{
			showAll = (request.getParameter("sudo") != null);
			out.println("<meta http-equiv=\"refresh\" content=\"0\">");
			out.println("Login Success");
		}
		else
		{
			errorMsg = "Log in Failed, Please Try again";
		}
		
	}
	
%>

<script>
	function validate_fields()
	{
		uname = document.getElementById('logintext').value;
		pword = document.getElementById('passwordtext').value;
		
		if (uname == "")
		{
			alert('Please provide your username.');
			return false;
		}
		if (pword == "")
		{
			alert('Please provide your password.');
			return false;
		}
	}


</script>
	<div id="login_outter_div" name="login_outter_div" align="center" style="width:100%" >
    	<div id="login_div" name="login_div" style="border-style:solid; border-color:#999999; border-radius: 10px; width:450px; margin:auto">
        	<h2>Login</h2>
            <form id="login_form" name="login_form" method="post" onsubmit="javascript:return validate_fields();" >
                <table width="100%" border="0" cellspacing="0" cellpadding="0" style="padding-left:10px">
                  <tr>
                    <td>User Name</td>
                    <td>&nbsp;</td>
                    <td>Password</td>
                  </tr>
                  <tr>
                    <td><input type="text" name="logintext" id="logintext" width="190px" /></td>
                    <td>&nbsp;</td>
                    <td><input type="password" name="passwordtext" id="passwordtext" width="190px" /></td>
                  </tr>
                  <tr>
                    <td colspan="3" align="right"><input name="Login" type="submit" value="Login" /></td>
                  </tr>
                </table>
			</form>
        </div>
    </div>
	<div style="color: red; text-align: center;"> <%=errorMsg %> </div>
<%
} %>
    
    
</body>
</html>