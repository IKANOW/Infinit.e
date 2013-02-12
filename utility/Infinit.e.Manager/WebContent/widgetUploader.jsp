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
<%@page import="javax.xml.ws.Response"%>
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
	static String SHARE_ROOT = "$infinite/share/get/";
	static Boolean DEBUG_MODE = false;
	static Boolean showAll = false;
	static Boolean localCookie = false;
	static String user = null;
	static String communityList = null; // (ensures that generateCommunityList is called)
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
	static class personGet
	{
		static class resp
		{
			String action;
			Boolean success;
			String message;
			int time;
		
		}
		static class community
		{
			String _id;
			String name;
		}
		static class data
		{
			String _id;
			String created;
			String modified;
			String accountStatus;
			String email;
			String firstName;
			String lastName;
			String displayName;
			String phone;
			community[] communities;
		}
		public resp response;
		public data data;
		
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
		public widgetPojo[] data;
		
	}
	static class widgetPojo 
	{
		String _id = null; // (optional, filled in by system if missing)
		String swf = null; // (obsolete field, ignored by the system)
		String url = null; // (mandatory)
		String title = null; // (mandatory)
		String description = null; // (mandatory)
		String created = null; // (user values ignored, filled in by the system)
		String modified = null; // (user values ignored, filled in by the system)
		String version = null; // (mandatory)
		String author = null; // (mandatory)
		String imageurl = null; // (mandatory)
		Boolean approved = null; // (optional, defaults to true)
		String[] searchterms = null; // (optional)
		Boolean debug = null; // (optional - debug objects won't appear in release versions of the UM GUI)
		Set<String> communityIds = null; // (optional - if specified, restricts access to users belonging to one of the spec'd communities)
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
		public shareData data;
		
	}
	public static void setBrowserInfiniteCookie(HttpServletResponse response,
			String value, int nServerPort) {
        String params = null;
        if ((443 == nServerPort) || (8443 == nServerPort)) {
                params="; path=/; HttpOnly; Secure";
        }
        else {
                params="; path=/; HttpOnly";
        }
        response.setHeader("SET-COOKIE", "infinitecookie="+value+params);
        	// (all this is needed in order to support HTTP only cookies)
	} // TESTED
	
	public static String getBrowserInfiniteCookie(HttpServletRequest request)
	{
		Cookie[] cookieJar = request.getCookies();
		if ( cookieJar != null)
		{
			for( Cookie cookie : cookieJar)
			{
				if (cookie.getName().equals("infinitecookie"))
				{
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
	            		return value.substring(equalsLoc+1,semicolonLoc);
	            	}
	            }
			}  
		}
    	return null;
	}
	
	public static String stringOfUrl(String addr, HttpServletRequest request, HttpServletResponse response)
	{
		if(localCookie)
			CookieHandler.setDefault(cm);
        try
        {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
        	URL url = new URL(addr);
        	URLConnection urlConnection = url.openConnection();

        	String cookieVal = getBrowserInfiniteCookie(request);
        	
        	if (cookieVal != null)
        	{
        		urlConnection.addRequestProperty("Cookie","infinitecookie=" + cookieVal.replace(";", ""));
        		urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.9.2.3) Gecko/20100401");
        		urlConnection.setDoInput(true);
        		urlConnection.setDoOutput(true);
        		urlConnection.setRequestProperty("Accept-Charset","UTF-8");
        	}
        	else if (DEBUG_MODE)
        		System.out.println("Don't Current Have a Cookie Value");
        	IOUtils.copy(urlConnection.getInputStream(), output);
        	String newCookie = getConnectionInfiniteCookie(urlConnection);
        	if (newCookie != null && response != null)
        	{
        		setBrowserInfiniteCookie(response, newCookie, request.getServerPort());
        	}
        	
        	if (DEBUG_MODE)
        		System.out.println(output.toString());
        	
        	String toReturn = output.toString();
        	output.close();
        	return toReturn;
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
		return URLEncoder.encode((new BASE64Encoder()).encode(md.digest()), "UTF-8");	
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
	
	public String deleteWidget(String widgetId, HttpServletRequest request, HttpServletResponse response)
	{
		if (widgetId != null)
		{
			String json = stringOfUrl(API_ROOT + "knowledge/uisetup/modules/delete/" + widgetId, request, response);
			if (json != null)
			{
				modResponse mr = new Gson().fromJson(json, modResponse.class);
				if (mr.response.message != null)
					return mr.response.message.replace("module", "Widget");
				else
					return json;
			}
		}
		return "There was an error deleting your widget, please try again later (" +widgetId.toString() +")";
		
		
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
				url = API_ROOT + "social/share/add/binary/" + URLEncoder.encode(title,charset) + "/" + URLEncoder.encode(description,charset) + "/";
			else
				url = API_ROOT + "social/share/update/binary/" + prevId + "/" + URLEncoder.encode(title,charset) + "/" + URLEncoder.encode(description,charset) + "/";
			
			if(localCookie)
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
        		setBrowserInfiniteCookie(response, newCookie, request.getServerPort());
        	}
            buffer.flush();
            buffer.close();
            output.close();
            responseStream.close();
            
            /**/
            System.out.println("JSON! " + json);
            
            modResponse mr = new Gson().fromJson(json, modResponse.class);
    		if (mr == null)
    		{
    			return "Failed: " + json;
    		}
    		if (mr.response.success == true)
    		{
    			if (prevId != null && mr.data == null)
    			{
    				return prevId;
    			}
    			return mr.data; //When a new upload, mr.data contains the ShareID for the upload
    		}
    		else
    		{
    			return "Widget Upload Failed: " + mr.response.message;
    		}
		}catch(IOException e)
		{
			e.printStackTrace();
			return "Widget Upload Failed: " + e.getMessage();
		}
	}
	
	private Boolean removeShareFromCommunity(String shareId, String communityId, HttpServletRequest request, HttpServletResponse response)
	{
		if(DEBUG_MODE)
			System.out.println("Removing share from community:");
		try{
			String charset = "UTF-8";

			String json = stringOfUrl(API_ROOT + "social/share/remove/community/" + URLEncoder.encode(shareId,charset) + "/" + URLEncoder.encode(communityId,charset) + "/" , request, response);
			keepAlive ka = new Gson().fromJson(json, keepAlive.class);
			if (ka != null)
			{
				if(DEBUG_MODE)
					System.out.println("Share: " + shareId + "ComunityId: " + communityId + "Removed:" + ka.response.success + " Message: " + ka.response.message);
				return ka.response.success;
			}
			else
			{
				if(DEBUG_MODE)
					System.out.println("JSON return null:   " + API_ROOT + "social/share/removecommunity/" + URLEncoder.encode(shareId,charset) + "/" + URLEncoder.encode(communityId,charset) + "/");
			}
			return false;
		}catch(IOException e)
		{
			e.printStackTrace();
			return false;
		}
	}
	
	//Removes a share with share id shareId from all communities that the current user is a member of. 
	//(This will be used in conjunction with then adding newly selected communities later)
	private void removeShareFromAllUserCommunities(String shareId, HttpServletRequest request, HttpServletResponse response)
	{
		try{
			String charset = "UTF-8";

			personGet.community[] comms = getUserCommunities(request, response);
			if (comms != null)
			{
				for (personGet.community comm : comms)
				{
					removeShareFromCommunity(shareId, comm._id, request, response);
				}
			}
		}catch(Exception e)
		{
			e.printStackTrace();
			//return e.getMessage();
		}
	}
	
	public String generateCommunityList(HttpServletRequest request, HttpServletResponse response)
	{
		String toReturn = "<select multiple=\"multiple\" name=\"communities\" id=\"communities\">";
		personGet.community[] pgs = getUserCommunities(request, response);
		if (pgs !=null)
		{
			for(personGet.community comm : pgs)
			{
				toReturn += "<option value=\""+ comm._id + "\">"+ comm.name +"</option>";
			}	
		}
			 // <option selected value="volvo">Volvo</option>
		
		return toReturn + "</select>";
	}
	
	private Boolean addShareToCommunity( String shareId, String communityId, HttpServletRequest request, HttpServletResponse response)
	{
		if(DEBUG_MODE)
			System.out.println("Adding share to community:");
		try{
			String charset = "UTF-8";
			
			String comment = "Added by widgetUploader";
			
			//social/share/add/community/{shareid}/{comment}/{communityid}
			String json = stringOfUrl(API_ROOT + "social/share/add/community/" + URLEncoder.encode(shareId,charset) + "/" + URLEncoder.encode(comment,charset) + "/" + URLEncoder.encode(communityId,charset) + "/", request, response);
			getModules gm = new Gson().fromJson(json, getModules.class);
			if (gm == null)
			{
				if(DEBUG_MODE)
					System.out.println("The JSON for Adding a share to community was null");
				return false;
			}
				//return "Json was null: " + json + "\n " + API_ROOT + "social/share/add/community/" + URLEncoder.encode(shareId,charset) + "/" + URLEncoder.encode(communityId,charset) + "/" + URLEncoder.encode(comment,charset) + "/";
			if(DEBUG_MODE)
				System.out.println("Share: " + shareId + "   CommunityId: " + communityId + " Shared:" + gm.response.success + "  Message: " + gm.response.message);
			return gm.response.success;
		}catch(IOException e)
		{
			e.printStackTrace();
			return false;
		}
		
	}
	
	private personGet.community[] getUserCommunities(HttpServletRequest request, HttpServletResponse response)
	{
		try{
			String charset = "UTF-8";

			String json = stringOfUrl(API_ROOT + "person/get/", request, response);
			personGet pg = new Gson().fromJson(json, personGet.class);
			if (pg != null)
			{
				user = pg.data.email;
				return pg.data.communities;
			}
			return null;
		}catch(Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}

	private String populatePreviousUploads(HttpServletRequest request, HttpServletResponse response)
	{
		String toReturn = "";
		String delim = "$$$";
		String json = stringOfUrl(API_ROOT + "knowledge/uisetup/modules/get/", request, response);
		if (json != null)
		{
			getModules gm = new Gson().fromJson(json, getModules.class);
			
				for ( widgetPojo info : gm.data)
				{
					//_id,url,title,description,created,modified,version,imageurl,communities
					if (showAll == true || info.author.equalsIgnoreCase(user))
					{
						String value = info._id+delim+info.url+delim+info.title+delim+info.description+delim+info.created+delim+info.modified+delim+info.version+delim+info.imageurl+delim+info.communityIds+delim+info.author;
						toReturn += "<option value=\""+value+"\" > <b>Edit:</b> " + info.title + "</option>";
					}
				}
			
		}
		return toReturn;
	}
	
	private String sendWidgetToDb(String title, String description, String version, String swfUrl, String iconUrl, String widgetShareId, String created, Set<String> communities, HttpServletRequest request, HttpServletResponse response)
	{
		//Create Entry for DB
		widgetPojo wig = new widgetPojo();

		if (created == null)
 			wig.created = new Date().toString();
		else
			wig.created = created;
		
		if(widgetShareId == null || widgetShareId.length() == 0)
			wig._id = null;
		else
			wig._id = widgetShareId;
		
		if (iconUrl == null || iconUrl.length() == 0)
			return "ERROR: The Icon Url was unable to be set.";
		
		if (swfUrl == null || swfUrl.length() == 0)
			return "ERROR: The Swf Url was unable to be set.";
		
		wig.imageurl = iconUrl;
		wig.title = title; 
		wig.description = description;
		wig.searchterms = (description + " " + title).split(" ");
		wig.url = swfUrl;
		wig.modified = new Date().toString();
		wig.version = version;
		wig.communityIds = communities;
 			
		//convert Widget to Json String
		String stringWig = new Gson().toJson(wig);
		
		if(installWidget(stringWig, request, response))
			return "Widget Uploaded Successfully!";
		else
			return "ERROR: DB Add/Update Failed.";
	}
	
	private String parseIdFromUrl(String url)
	{
		if (url.startsWith(SHARE_ROOT))
		{
			return url.replace(SHARE_ROOT, "");
		}
		return null;
	}
	
	//Places the widget information into the Database
	private Boolean installWidget(String wigJson, HttpServletRequest request, HttpServletResponse response)
	{
		if(localCookie)
			CookieHandler.setDefault(cm);
		String url = API_ROOT + "knowledge/uisetup/modules/install/";
        String charset = "UTF-8";
        OutputStream output = null;
        URLConnection connection = null;
        try{
	        connection = new URL(url).openConnection();
	        connection.setDoOutput(true);
	        connection.setRequestProperty("Content-Type", "text/plain;charset=" + charset);
	        String cookieVal = getBrowserInfiniteCookie(request);
        	if (cookieVal != null)
        	{
        		connection.addRequestProperty("Cookie","infinitecookie=" + cookieVal);
        		connection.setDoInput(true);
        		connection.setDoOutput(true);
        		connection.setRequestProperty("Accept-Charset","UTF-8");
        	}
	        output = connection.getOutputStream();
	        output.write(wigJson.getBytes());
	        
	        InputStream responseStream = connection.getInputStream();
	        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
	        int nRead;
	        byte[] data = new byte[16384];
	        while ((nRead = responseStream.read(data,0,data.length)) != -1)
	        {
	            buffer.write(data,0,nRead);
	        }
	        
	        String newCookie = getConnectionInfiniteCookie(connection);
        	if (newCookie != null && response != null)
        	{
        		setBrowserInfiniteCookie(response, newCookie, request.getServerPort());
        	}
	        
	        widgetToDBResponse resp = new Gson().fromJson(buffer.toString(), widgetToDBResponse.class);
	        return resp.response.success;
        }catch(IOException e){
        	return false;
        }
        
        
	}
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
<title>Infinit.e Widget Upload Tool</title>
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
.show {
display: ;
visibility: visible;
}
.hide {
display: none;
visibility: hidden;
}
</style>
<script language="javascript" src="AppConstants.js"> </script>
</head>

<body>
<%
if (API_ROOT == null)
{
	ServletContext context = session.getServletContext();
	String realContextPath = context.getRealPath("/");
	ScriptEngineManager manager = new ScriptEngineManager();
	ScriptEngine engine = manager.getEngineByName("javascript");
	try{ // EC2 Machines
		FileReader reader = new FileReader(realContextPath + "/AppConstants.js");
		engine.eval(reader);
		reader.close();
		engine.eval("output = getEndPointUrl();");
		API_ROOT = (String) engine.get("output");
	}
	catch (Exception je)
	{
		try { ////////////Windows + Tomcat
			FileReader reader = new FileReader(realContextPath + "\\..\\AppConstants.js");
			engine.eval(reader);
			reader.close();
			engine.eval("output = getEndPointUrl();");
			API_ROOT = (String) engine.get("output");
		}catch (Exception e)
		{
			System.err.println(e.toString());
		}
	}
	if (null == API_ROOT) { 
		// Default to localhost
		API_ROOT = "http://localhost:8080/api/";
	}

	if (API_ROOT.contains("localhost"))
		localCookie=true;
	else
		localCookie=false;
}



Boolean isLoggedIn = isLoggedIn(request, response);
if (isLoggedIn == null)
{
	out.println("The Infinit.e API cannot be reached.");
	out.println(API_ROOT);
}

else if (isLoggedIn == true)
{ 
	showAll = (request.getParameter("sudo") != null);
	communityList = generateCommunityList(request, response);
	
	if (request.getParameter("logout") != null)
	{
		logOut(request, response);
		out.println("<div style=\" text-align: center;\">");
		out.println("<meta http-equiv=\"refresh\" content=\"0\">");
		out.println("</div>");						
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
			Set<String> communities = new HashSet<String>();
	 		while (iter.hasNext()) {
	 		    FileItemStream item = iter.next();
	 		    String name = item.getFieldName();
	 		    InputStream stream = item.openStream();
	 		    if (item.isFormField()) {
	 		    	if (name.equalsIgnoreCase("communities"))
	 		    	{
	 		    		communities.add(Streams.asString(stream));
	 		    	}
	 		    	else
	 		    		request.setAttribute(name, Streams.asString(stream));
	 		    	
	 		    	//out.println("<b>" + name + ":</b>" + request.getAttribute(name).toString()+"</br>");
	 		    } else 
	 		    {
	 		    	if (name.equalsIgnoreCase("file"))
	 		    	{
	 		    		fileDS = item.getContentType();
	 		    		fileBytes = IOUtils.toByteArray(stream);
	 		    	}
	 		    	else if (name.equalsIgnoreCase("icon"))
	 		    	{
	 		    		iconDS = item.getContentType();
	 		    		iconBytes = IOUtils.toByteArray(stream);
	 		    	}
	 		    	
	 		    }
	 		}
	 		
	 		////////////////////////////////////Delete Widget ////////////////////////////////
	 		if (request.getAttribute("deleteId") != null)
	 		{
	 			if (request.getAttribute("deleteIcon") != null)
	 			{
	 				String iconId = parseIdFromUrl(request.getAttribute("deleteIcon").toString());
	 				if (iconId != null)
	 					removeFromShare(iconId, request, response).toString();
	 			}
	 			
	 			if (request.getAttribute("deleteFile") != null)
	 			{
	 				String fileId = parseIdFromUrl(request.getAttribute("deleteFile").toString());
	 				if (fileId != null)
	 					removeFromShare(fileId, request, response).toString();
	 			}
	 			
	 			out.println(deleteWidget(request.getAttribute("deleteId").toString(), request, response));
	 		}
	 		else
	 		{
	 		
		 	//////////////////////////////////////////////////////////////////////////////////
		 		
		 		Boolean newWidget =(request.getAttribute("DBId").toString().length() == 0 );
		 		///////////////////////////////// Icon Image Manip  /////////////////////////////////
		 		String iconUrl = "";
		 		String iconId = "";
		 		if (request.getAttribute("icon_check") == null) //User is uploading own icon file
				{
					
					if(request.getAttribute("title") != null && request.getAttribute("description") != null && iconBytes != null)
					{
						if (newWidget)
						{
							iconId = AddToShare(iconBytes, iconDS, request.getAttribute("title").toString(),request.getAttribute("description").toString(), request, response);
						}
						else
						{
							iconId = parseIdFromUrl(request.getAttribute("imageUrl").toString());
							if (iconId == null)
								iconId = AddToShare(iconBytes,iconDS, request.getAttribute("title").toString() + " icon",request.getAttribute("description").toString(), request, response);
							else
							{
								UpdateToShare(iconBytes, iconDS, request.getAttribute("title").toString() + " icon",request.getAttribute("description").toString(), iconId, request, response);
								removeShareFromAllUserCommunities(iconId, request, response);
							}
								
						}
						if (iconId.contains("Failed"))
						{
							out.println(iconId);
							iconUrl = null;
						}
						else
						{
							//Add the shares to their communities
							for (String comm: communities)
							{
								addShareToCommunity(iconId, comm, request, response);
							}
							iconUrl = SHARE_ROOT + iconId;
						}
					}
					else
					{
						iconUrl = null;
						out.println("Error: Not enough information provided for Icon Upload");
					}
				}
		 		else //User has provided a URL to the Icon File
		 		{
		 			if (request.getAttribute("imageUrl") == null)
		 			{
		 				iconUrl = null;
		 				out.println("Error: Not enough information provided for Icon Upload");
		 			}
		 			else
		 			{
		 				String new_url = request.getAttribute("icon_url").toString();
		 				if (request.getAttribute("imageUrl") != null)
		 				{
			 				String old_url = request.getAttribute("imageUrl").toString();
	
			 				if (!old_url.equals(new_url))
			 				{
			 					removeFromShare(parseIdFromUrl(old_url), request, response);
			 				}
			 				
			 				if(new_url.contains(SHARE_ROOT)) //they are using a link to a share
			 				{
			 					iconId = parseIdFromUrl(new_url);
			 					//remove from all communities that the user is a member of
			 					removeShareFromAllUserCommunities(iconId, request, response);
			 					//Add the share to the communities
			 					for (String comm: communities)
			 					{
			 						addShareToCommunity(iconId, comm, request, response);
			 					}
			 				}
		 				}
		 				
		 				iconUrl = new_url;
		 			}
		 			
		 		}
		 		
				///////////////////////////////// End Icon Image Manip  /////////////////////////////////
				
				
		 		///////////////////////////////// SWF Manip  /////////////////////////////////
		 		String fileUrl = "";
				String fileId = "";
		 		if (request.getAttribute("file_check") == null) //User is uploading own file file
				{
					if(request.getAttribute("title") != null && request.getAttribute("description") != null && fileBytes != null)
					{
						if (newWidget)
						{
							fileId = AddToShare(fileBytes, fileDS, request.getAttribute("title").toString(),request.getAttribute("description").toString(), request, response);
						}
						else
						{
							fileId = parseIdFromUrl(request.getAttribute("swfUrl").toString());
							if (fileId == null)
								fileId = AddToShare(fileBytes, fileDS, request.getAttribute("title").toString(),request.getAttribute("description").toString(), request, response);
							else
							{
								UpdateToShare(fileBytes, fileDS, request.getAttribute("title").toString(),request.getAttribute("description").toString(), fileId, request, response);
								removeShareFromAllUserCommunities(fileId, request, response);
							}
						}
						if (fileId.contains("Failed"))
						{
							removeFromShare(iconId, request, response);
							out.println("SWF File Upload Failed. Please Try again.");
							fileUrl = null;
						}
						else
						{
							//Add the shares to their communities
							for (String comm: communities)
							{
								addShareToCommunity(fileId, comm, request, response);
							}
							fileUrl = SHARE_ROOT + fileId;
						}
					}
					else
					{
						fileUrl = null;
						out.println("Error: Not enough information provided for file Upload");
					}
				}
		 		else //User has provided a URL to the swf File
		 		{
		 			if (request.getAttribute("file_url") == null)
		 			{
		 				fileUrl = null;
		 				out.println("Error: Not enough information provided for SWF Upload");
		 			}
		 			else
		 			{
		 				String new_url = request.getAttribute("file_url").toString();
		 				
		 				if (request.getAttribute("swfUrl") != null)
		 				{
			 				String old_url = request.getAttribute("swfUrl").toString();
			 				
			 				if (!old_url.equals(new_url))
			 				{
			 					removeFromShare(parseIdFromUrl(old_url), request, response);
			 				}
			 				
			 				if(new_url.contains(SHARE_ROOT)) //they are using a link to a share
			 				{
			 					fileId = parseIdFromUrl(new_url);
			 					//remove from all communities that the user is a member of
			 					removeShareFromAllUserCommunities(fileId, request, response);
			 					//Add the share to the communities
			 					for (String comm: communities)
			 					{
			 						addShareToCommunity(fileId, comm, request, response);
			 					}
			 				}
		 				}
		 				
		 				fileUrl = new_url;
		 			}
		 			
		 		}
		 		
				///////////////////////////////// End SWF Manip  /////////////////////////////////
				
				
				//////////////////// Widget DB Submission /////////////////////////////////////
		 		
				if(request.getAttribute("title") != null && request.getAttribute("description") != null && request.getAttribute("version") != null && fileUrl != null && iconUrl != null )
				{
					String created = "";
					if(request.getAttribute("created") == null)
						created = null;
					else
						created = request.getAttribute("created").toString();
					
					String DBId = "";
					if(request.getAttribute("DBId") == null)
						DBId = null;
					else
						DBId = request.getAttribute("DBId").toString();
					
					out.println(sendWidgetToDb(request.getAttribute("title").toString(), request.getAttribute("description").toString(), request.getAttribute("version").toString(), fileUrl, iconUrl, DBId, created,communities, request, response));
					
					
					
				}
				else
				{
					removeFromShare(fileId, request, response);
					removeFromShare(iconId, request, response);
					out.println("Upload Failed");
				}	
				
				out.println("</div>");
	 		}
	 		
	 		
	 		
		}
		else
		{
		}
	
	%>
	
	<script>
		function clearCommList()
		{
			mult_comms = document.getElementById('communities');
			for ( var i = 0, l = mult_comms.options.length, o; i < l; i++ )
			{
			  o = mult_comms.options[i];
			  o.selected = false;
			}
		}
		function highlightComms(commList)
		{
			mult_comms = document.getElementById('communities');
			for ( var i = 0, l = mult_comms.options.length, o; i < l; i++ )
			{
			  o = mult_comms.options[i];
			  if(commList.indexOf(o.value) == -1)
				o.selected = false;
			  else  
			  	o.selected = true;
			}
		}
		function populate()
		{
	
			var title = document.getElementById('title');
			var description = document.getElementById('description');
			var file = document.getElementById('file');
			var icon = document.getElementById('icon');
			var version = document.getElementById('version');
			var created = document.getElementById('created');
			var DBId = document.getElementById('DBId');
			var deleteId = document.getElementById('deleteId');
			var deleteFile = document.getElementById('deleteFile');
			var deleteIcon = document.getElementById('deleteIcon');
			var imageUrl = document.getElementById('imageUrl');
			var swfUrl = document.getElementById('swfUrl');
			var icon_check = document.getElementById('icon_check');
			var file_check = document.getElementById('file_check');
			var file_url = document.getElementById('file_url');
			var icon_url = document.getElementById('icon_url');
			var deleteButton = document.getElementById('deleteButton');
			var owner_text = document.getElementById('owner_text');
			var owner = document.getElementById('owner');
			
			
			dropdown = document.getElementById("upload_info");
			list = dropdown.options[dropdown.selectedIndex].value;
			
			if (list == "new")
			{
				title.value = "";
				description.value = "";
				version.value = "";
				created.value = "";
				DBId.value = "";
				deleteId.value = "";
				deleteFile.value = "";
				deleteIcon.value = "";
				imageUrl.value = "";
				swfUrl.value = "";
				file_url.value = "";
				icon_url.value = "";
				icon_check.checked = false;
				file_check.checked = false;
				owner.className = "hide";
				owner_text.className = "hide";
				deleteButton.className = "hide";
				useUrlSwf();
				useUrlIcon();
				clearCommList();
				return;
			}
			//_id,url,title,description,created,modified,version,imageurl,communities
			split = list.split("$$$");
			
			res_id = split[0];
			resUrl = split[1];
			resTitle = split[2];
			resDescription = split[3];
			resCreated = split[4];
			resModified = split[5];
			resVersion = split[6];
			resImageurl = split[7];
			communities = split[8];
			res_author = split[9];
	
			icon_check.checked = true;
			file_check.checked = true;
			title.value = resTitle;
			description.value = resDescription;
			version.value = resVersion;
			created.value = resCreated;
			DBId.value = res_id;
			deleteId.value = res_id;
			deleteFile.value = resUrl;
			deleteIcon.value = resImageurl;
			imageUrl.value = resImageurl;
			swfUrl.value = resUrl;
			file_url.value = resUrl;
			icon_url.value = resImageurl;
			owner.value = res_author;
			useUrlSwf();
			useUrlIcon();
			deleteButton.className = "show";
			owner.className = "show";
			owner_text.className = "show";
			highlightComms(communities);
		}
		function useUrlSwf()
		{
			file = document.getElementById('file');
			file_url = document.getElementById('file_url');
			
			if (document.getElementById('file_check').checked)
			{
				file_url.className = "show";
				file.className = "hide";
			}
			else
			{
				file.className = "show";
				file_url.className = "hide";
			}
		}
		function useUrlIcon()
		{
			icon = document.getElementById('icon');
			icon_url = document.getElementById('icon_url');
			
			if (document.getElementById('icon_check').checked)
			{
				icon_url.className = "show";
				icon.className = "hide";
			}
			else
			{
				icon.className = "show";
				icon_url.className = "hide";
			}
		}
		function validate_fields()
		{
			title = document.getElementById('title').value;
			description = document.getElementById('description').value;
			file = document.getElementById('file').value;
			icon = document.getElementById('icon').value;
			version = document.getElementById('version').value;
			icon_checked = document.getElementById('icon_check').checked;
			file_checked = document.getElementById('file_check').checked;
			file_url = document.getElementById('file_url').value;
			icon_url = document.getElementById('icon_url').value;

			var select = document.getElementById( 'communities' );
			numSelected = 0;
			
			for ( var i = 0, l = select.options.length, o; i < l; i++ )
			{
			  o = select.options[i];
			  if ( o.selected == true )
			  {
			    numSelected++;
			  }
			}

			
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
			if (file == "" && file_checked == false)
			{
				alert('Please provide your file.');
				return false;
			}
			if (file_url == "" && file_checked == true)
			{
				alert('Please provide the Url to your Swf file.');
				return false;
			}
			if (version == "")
			{
				alert('Please provide a Version Number.');
				return false;
			}
			if (icon == "" && icon_checked == false)
			{
				alert('Please provide an icon image for your widget.');
				return false;
			}
			if (icon_url == "" && icon_checked == true)
			{
				alert('Please provide a url to your icon image.');
				return false;
			}
			if (numSelected == 0)
			{
				alert('Please select at least 1 community');
				return false;
			}
			
		}
		function confirmDelete()
		{
			var agree=confirm("Are you sure you wish to Delete this Widget?");
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
	        	<h2>Widget Uploader</h2>
	        	<form id="delete_form" name="delete_form" method="post" enctype="multipart/form-data" onsubmit="javascript:return confirmDelete()" >
	        		<select id="upload_info" onchange="populate()" name="upload_info"><option value="new">Upload New Widget</option> <% out.print(populatePreviousUploads(request, response)); %></select>
	        		<input type="submit" name="deleteButton" id="deleteButton" class="hidden" value="Delete" />
	        		<input type="hidden" name="deleteId" id="deleteId" />
	        		<input type="hidden" name="deleteFile" id="deleteFile" />
	        		<input type="hidden" name="deleteIcon" id="deleteIcon" />
	        	</form>
	            <form id="upload_form" name="upload_form" method="post" enctype="multipart/form-data" onsubmit="javascript:return validate_fields();" >
	                <table width="100%" border="0" cellspacing="0" cellpadding="0" style="padding-left:10px; padding-right:10px">
	                  <tr>
	                    <td colspan="2" align="center"></td>
	                  </tr>
	                  <tr>
	                    <td>Title:</td>
	                    <td><input type="text" name="title" id="title" size="35" /></td>
	                  </tr>
	                  <tr>
	                    <td>Description:</td>
	                    <td><textarea rows="4" cols="30" name="description" id="description" ></textarea></td>
	                  </tr>
	                  <tr>
	                  	<td>Communities:</td>
	                  	<td><% out.print(communityList); %></td>
	                  </tr>
	                  <tr>
	                  	<td id="owner_text">Owner:</td>
	                  	<td>
	                    <input type="text" name="owner" id="owner" readonly="readonly" size="25" />
	                  	</td>
	                  </tr>
	                  <tr>
	                    <td>Swf File:</td>
	                    <td><input type="file" name="file" id="file" /><input type="text" name="file_url" id="file_url" size="32" class="hide" /><input type="checkbox" id="file_check" name="file_check" onchange="useUrlSwf()" /> <span id="file_provide" name="file_provide"> Provide Url </span></td>
	                  </tr>
	                  <tr>
	                    <td>Version Number:</td>
	                    <td><input type="text" name="version" id="version" /></td>
	                  </tr>
	                  <tr id="iconRow">
	                    <td>Icon Image:</td>
	                    <td><input type="file" name="icon" id="icon" /><input type="text" name="icon_url" id="icon_url" size="32" class="hide" /><input type="checkbox" id="icon_check" name="icon_check" onchange="useUrlIcon()" /><span id="file_provide" name="file_provide"> Provide Url</span></td>
	                  </tr>
	                  <tr>
	                    <td colspan="2" style="text-align:right"><input type="submit" value="Submit" /></td>
	                  </tr>
	                </table>
					<input type="hidden" name="created" id="created" />
					<input type="hidden" name="DBId" id="DBId" />
					<input type="hidden" name="imageUrl" id="imageUrl" />
					<input type="hidden" name="swfUrl" id="swfUrl" />
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
	//localCookie =(request.getParameter("local") != null);
	String errorMsg = "";
	if (request.getParameter("logintext") != null || request.getParameter("passwordtext") != null)
	{
		if(logMeIn(request.getParameter("logintext"),request.getParameter("passwordtext"), request, response))
		{
			showAll = (request.getParameter("sudo") != null);
			DEBUG_MODE = (request.getParameter("debug") != null);
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
