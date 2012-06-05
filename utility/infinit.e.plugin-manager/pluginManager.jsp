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
<%@ page contentType="text/html; charset=utf-8" language="java" import="java.io.*, java.util.*,java.net.*,com.google.gson.Gson, org.apache.commons.io.*,sun.misc.BASE64Encoder,java.security.*,java.util.zip.*;" errorPage="" %>
<%!
	static String API_ROOT = null;
	static String SHARE_ROOT = "$infinite/share/get/";
	static Boolean DEBUG_MODE = false;
	static Boolean showAll = false;
	static Boolean localCookie = false;
	static String user = null;
	static String communityList = null; // (generated from generateCommunityList)
	static String taskList = null; // (generated from populateExistingTasks)
	static String jarList = null; // (generated from populatePreviousJarUploads)
	static String inputCollectionList = null; // (generated from populateExistingTasks)
	static CookieManager cm = new CookieManager();
	static String selectedJson = null; // If want to preserve pages across submit/refresh calls	

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
	static class jobPojo
	{
		String mapper;
		String combiner;
		String reducer;
		String outputKey;
		String outputValue;
		
		String query;
		String firstSchedule;
		Long nextRunTime;
		String lastRunTime;
		String lastCompletionTime;
		String scheduleFreq;
		Long timesRan;

		String inputCollection;
		String outputCollection;
		String jarURL;
		String jobdesc;
		String jobtitle;

		String communityIds[];
		
		String _id;
		String jobidS;
		Long jobidN;
		String submitterID;
	}
	static class getJobs
	{
		static class jobRes
		{
			String action;
			Boolean success;
			String message;
			int time;
		
		}
		public jobRes response;
		public jobPojo[] data;		
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
	
	static class jobActionResponse
	{
		static class jobResponse
		{
			public String action;
			public Boolean success;
			public String message;
			public int time;
			
		}
		public jobResponse response;
		public String data;
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
	public static void setBrowserInfiniteCookie(HttpServletResponse response, String value)
	{
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
        		setBrowserInfiniteCookie(response, newCookie);
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
			String json = stringOfUrl(API_ROOT + "social/share/remove/" + shareId + "/", request, response);
			if (json != null)
			{
				keepAlive keepA = new Gson().fromJson(json, keepAlive.class);
				return keepA.response.message;
			}
		}
		return null;
	}
	
	public String deleteTask(String taskId, HttpServletRequest request, HttpServletResponse response)
	{
		if (taskId != null)
		{
			String json = stringOfUrl(API_ROOT + "custom/mapreduce/removejob/" + taskId, request, response);
			if (json != null)
			{
				jobActionResponse mr = new Gson().fromJson(json, jobActionResponse.class);
				if (mr.response.message != null)
					return mr.response.message;
				else
					return json;
			}
		}
		return "There was an error deleting your task, please try again later (" +taskId.toString() +")";
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
        		setBrowserInfiniteCookie(response, newCookie);
        	}
            buffer.flush();
            buffer.close();
            output.close();
            responseStream.close();
            
            jobActionResponse mr = new Gson().fromJson(json, jobActionResponse.class);
            	//(lazily use jobActionResponse, just need something with a string data)
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
    			return "JAR Upload Failed: " + mr.response.message;
    		}
		}catch(IOException e)
		{
			e.printStackTrace();
			return "JAR Upload Failed: " + e.getMessage();
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
			
			String comment = "Added by pluginManager";
			
			String json = stringOfUrl(API_ROOT + "social/share/add/community/" + URLEncoder.encode(shareId,charset) + "/" + URLEncoder.encode(comment,charset) + "/" + URLEncoder.encode(communityId,charset) + "/", request, response);
			getJobs gm = new Gson().fromJson(json, getJobs.class); // (any getXXX will do, ignore the data)
			if (gm == null)
			{
				if(DEBUG_MODE)
					System.out.println("The JSON for Adding a share to community was null");
				return false;
			}
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
				user = pg.data._id;
				return pg.data.communities;
			}
			return null;
		}catch(Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}

	private String populatePreviousJarUploads(HttpServletRequest request, HttpServletResponse response)
	{
		String toReturn = "";
		String ext = null;
		String searchCriteria = "?type=binary";
		String json = stringOfUrl(API_ROOT + "social/share/search/" + searchCriteria, request, response);
		
		 if (json != null)
		{
			getShare gs = new Gson().fromJson(json, getShare.class);
			if (gs != null && gs.data != null)
			{
				for ( getShare.shareData info : gs.data)
				{
					if (info.mediaType.equalsIgnoreCase("application/java-archive") || info.mediaType.equalsIgnoreCase("application/x-java-archive"))
					{
						toReturn += "<option value=\""+SHARE_ROOT + info._id+"\" >" + info.title + " (" + info._id + ")</option>";
					}
				}
			}
		}
		return toReturn; 
	}
	
	private String populateExistingTasks(HttpServletRequest request, HttpServletResponse response, String taskTitle) throws UnsupportedEncodingException
	{
		String charset = "UTF-8";
		
		String toReturn1 = ""; // (return val, task list)
		String toReturn2 = ""; // (bonus val, input collections)
		String json = stringOfUrl(API_ROOT + "custom/mapreduce/getjobs", request, response);
		selectedJson = null;
				
		if (json != null)
		{			
			getJobs gm = new Gson().fromJson(json, getJobs.class);
			
			for ( jobPojo info : gm.data)
			{
				if (showAll == true || info.submitterID.equalsIgnoreCase(user))
				{						
					String value1 = URLEncoder.encode(new Gson().toJson(info), charset);
					toReturn1 += "<option value=\""+value1+"\" > <b>Edit:</b> " + info.jobtitle + "</option>";
					
					if ((null != taskTitle) && (info.jobtitle.equals(taskTitle)))
						selectedJson = value1;
				}
				toReturn2 += "<option value=\""+info.jobtitle+"\" >"+info.jobtitle+" Output Collection</option>";
			}			
		}
		inputCollectionList = toReturn2;
		return toReturn1;
	}
	
	private String sendTaskToDb(String pluginId,
			String title, String description, 
			String frequency, 
			Long nextruntime, String inputcollection, 
			String currJarUrl, String mapper, String combiner, String reducer,
			String query, String outputKey, String outputValue,
			Set<String> communities, HttpServletRequest request, HttpServletResponse response) throws UnsupportedEncodingException
	{
		//Create Entry for DB
		if (currJarUrl == null || currJarUrl.length() == 0)
			return "ERROR: The JAR Url was unable to be set.";
		
		String s = null;
		if((null == (s = installTask(pluginId, title, description, frequency, nextruntime, inputcollection, currJarUrl, 
						mapper, combiner, reducer, query, outputKey, outputValue,
				communities, request, response))))
		{
			return "Task Added/Updated Successfully!";
		}
		else
			return s;
	}
	
	private String parseIdFromUrl(String url)
	{
		if (url.startsWith(SHARE_ROOT))
		{
			return url.replace(SHARE_ROOT, "");
		}
		return null;
	}
	
	//Places the task information into the Database
	private String installTask(String pluginId,
			String title, String description, 
			String frequency, 
			Long nextruntime, String inputcollection, 
			String currJarUrl, String mapper, String combiner, String reducer,
			String query, String outputKey, String outputValue,
			Set<String> communities, HttpServletRequest request, HttpServletResponse response) throws UnsupportedEncodingException
	{
		String charset = "UTF-8";
		
		if(localCookie)
			CookieHandler.setDefault(cm);
		String url = API_ROOT; 
		
		if ((null != pluginId) && (pluginId.length() > 0))
		{
			url += "custom/mapreduce/updatejob/";
			url += pluginId + "/";
		}
		else
		{
			url += "custom/mapreduce/schedulejob/";			
		}
		url += title + "/";
		url += java.net.URLEncoder.encode(description, charset) + "/";
		int n = 0;
		for (String commId: communities) {
			if (n > 0) {
				url += ',';
			}
			n++;
			url += commId;
		}
		url += "/";
		url += java.net.URLEncoder.encode(currJarUrl, charset) + "/";
		if (nextruntime < 0) // don't set it
		{
			url += "null/null/";						
		}
		else 
		{
			url += nextruntime + "/";
			url += frequency + "/";
		}
		url += java.net.URLEncoder.encode(mapper, charset) + "/" + java.net.URLEncoder.encode(reducer, charset) + "/" + java.net.URLEncoder.encode(combiner, charset) + "/";
		url += java.net.URLEncoder.encode(query, charset) + "/"; 
		url += java.net.URLEncoder.encode(inputcollection, charset) + "/";
		url += outputKey + "/";
		url += outputValue;
		
		//DEBUG
		//System.out.println(url);
		//if (true) return false;		
		
		String json = stringOfUrl(url, request, response);	        
        jobActionResponse resp = new Gson().fromJson(json, jobActionResponse.class);
        
        if (resp.response.success)
        	return null;
        else 
        	return resp.response.message;
	}
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
<title>Infinit.e MapReduce Plugin Manager</title>
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
	
	if (request.getParameter("logout") != null)
	{
		logOut(request, response);
		out.println("<div style=\" text-align: center;\">");
		out.println("<meta http-equiv=\"refresh\" content=\"0\">");
		out.println("</div>");		
	}
	else
	{
		String title = null;
		out.println("<div style=\" text-align: center;\">");
		String contentType = request.getContentType();
		if ((contentType != null) && ( contentType.indexOf("multipart/form-data") >= 0 ))
		{			
			// Create a new file upload handler
	 		ServletFileUpload upload = new ServletFileUpload(new DiskFileItemFactory());
			// Parse the request
	 		FileItemIterator iter = upload.getItemIterator(request);
			byte[] fileBytes = null;
			String fileDS = null;
			Set<String> communities = new HashSet<String>();
			boolean bContinueAfterFileUpload = true;
			HashSet<String> classes = null;
			
	 		while (iter.hasNext()) {
	 		    FileItemStream item = iter.next();
	 		    String name = item.getFieldName();
	 		    InputStream stream = item.openStream();
	 		    
	 		    if (item.isFormField())
	 		    {
	 		    	if (name.equalsIgnoreCase("communities"))
	 		    	{
	 		    		communities.add(Streams.asString(stream));
	 		    	}
	 		    	else
	 		    	{
	 		    		request.setAttribute(name, Streams.asString(stream));
	 		    	}
	 		    } 
	 		    else // not a form field, must be content
	 		    {
	 		    	classes = new HashSet<String>();
	 		    	
	 		    	if (name.equalsIgnoreCase("file"))
	 		    	{
	 		    		fileBytes = IOUtils.toByteArray(stream);
	 		    		
						ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(fileBytes));
						ZipEntry entry;
						while ((entry = zis.getNextEntry()) != null)
						{
							if (entry.getName().endsWith(".class"))
							{
								String test = entry.getName().replace('/', '.').substring(0, entry.getName().length() - 6); // (-6 for the .class)
								classes.add(test);												
							}
						}
	 		    	}
	 		    	
	 		    }//loop over attributes
	 		}//end if posted content

 			if (request.getAttribute("reusejar_check") == null) // (user is uploading own file)
 			{
				if (null != classes)
				{
		    		// Will check this is a zip file (==JAR) and contains the right classes
		    		String mapper = request.getAttribute("mapper").toString();
		    		String combiner = request.getAttribute("combiner").toString();
		    		String reducer = request.getAttribute("reducer").toString();	 		    		
			    		
					if ((classes.contains(mapper)) && (classes.contains(combiner)) && classes.contains(reducer))
					{
						// If we're here then it must be a valid archive	 		    			 		    		
	 		    		fileDS = "application/java-archive";	 		    		
					}
					else 
					{
						out.println("Uploaded 'JAR' file is not a java archive or does not contain a required class file: " + mapper + "/" + combiner + "/" + reducer);	 				
						bContinueAfterFileUpload = false;
					}
			    }//end if file is specified
 			}//end if URL not specified
 				 		
	 		////////////////////////////////////Delete Task ////////////////////////////////
	 		if (request.getAttribute("deleteId") != null)
	 		{
	 			out.println(deleteTask(request.getAttribute("deleteId").toString(), request, response));
	 		}//TESTED
	 		else if (request.getAttribute("refreshId") != null)
	 		{
				title = request.getAttribute("refreshId").toString();	 			
	 		}
	 		else if (bContinueAfterFileUpload)
	 		{
				title = request.getAttribute("title").toString();
		 		
		 	//////////////////////////////////////////////////////////////////////////////////
		 	// Updating/Adding a task, logic:
		 	// If uploading a file:
		 	// - if an existing task, and current file corresponds to the job title:
			//   - update the existing binary
			// - in other cases (new task/current file does not "belong" to the job title)
		 	//   - add the binary and set fileUrl
		 	// If using an existing URL, do nothing (ie leave any previously uploaded JARs out there, need to delete them from the file uploader)
		 	// Then call the "add task" logic, which decides whether to add or update
		 	
		 		///////////////////////////////// JAR Manip  /////////////////////////////////
		 		
		 		String fileUrl = "";
				String fileId = "";
		 		boolean newTask = ( request.getAttribute("DBId").toString().length() == 0 );
		 		
		 		if (request.getAttribute("reusejar_check") == null) //User is uploading own file
				{
			 		if (newTask) // Easy case, just add binary and update jar_url
			 		{
			 			//DEBUG
			 			//System.out.println("NEW TASK, UPLOADED: " + request.getAttribute("title").toString());
			 			
						fileId = AddToShare(fileBytes, fileDS, request.getAttribute("title").toString(),request.getAttribute("description").toString(), request, response);			 			
			 		}// TESTED
			 		else // Tricky case, need to know more about the share...
			 		{
			 			//DEBUG
			 			//System.out.println("OLD TASK, EXISTING URL: " + request.getAttribute("currJarUrl").toString() + " / " + request.getAttribute("currJarTitle").toString());			 			
			 			
			 			boolean thisIsMyShare = request.getAttribute("title").toString().equals(request.getAttribute("currJarTitle").toString());
			 				//TESTED
			 			
			 			if (thisIsMyShare)
			 			{
							fileId = parseIdFromUrl(request.getAttribute("currJarUrl").toString());
							if (fileId == null)
							{								
					 			//DEBUG
					 			//System.out.println("MALFORMED URL, JUST ADD NEW BINARY: " + request.getAttribute("title").toString());
					 			//fileId = "";
					 			
								fileId = AddToShare(fileBytes, fileDS, request.getAttribute("title").toString(),request.getAttribute("description").toString(), request, response);
							}// TESTED
							else
							{
					 			//DEBUG
					 			//System.out.println("UPDATED MY JAR: " + request.getAttribute("title").toString());

								UpdateToShare(fileBytes, fileDS, request.getAttribute("title").toString(),request.getAttribute("description").toString(), fileId, request, response);
								removeShareFromAllUserCommunities(fileId, request, response);
							}// TESTED
			 			}
			 			else 
			 			{
				 			//DEBUG
				 			//System.out.println("UPDATE BUT JAR WASN'T MINE, SO JUST ADD NEW SHARE: " + request.getAttribute("title").toString());

							fileId = AddToShare(fileBytes, fileDS, request.getAttribute("title").toString(),request.getAttribute("description").toString(), request, response);			 						 				
			 			}// TESTED
			 		}
			 		//TESTED
			 		
			 		// Tidy up
			 		
					if ((0 == fileId.length()) || fileId.contains("Failed"))
					{
						out.println("JAR File Upload Failed. Please Try again.");
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
			 		//TESTED
				}
		 		else //User has provided a URL to the JAR File
		 		{
		 			if (request.getAttribute("jar_url") == null)
		 			{
		 				fileUrl = null;
		 				out.println("Error: Not enough information provided for JAR Upload");
		 			}
		 			else
		 			{		 				
		 				fileUrl = request.getAttribute("jar_url").toString();
		 			}		 			
		 		}//TESTED
		 		
				///////////////////////////////// End JAR Manip  /////////////////////////////////
				
				
				//////////////////// MapReduce task DB Submission /////////////////////////////////////
		 		
				// (Mostly validation is done from the submit button)
				if(title != null && fileUrl != null )
				{
					String DBId = "";
					if(request.getAttribute("DBId") == null)
						DBId = null;
					else
						DBId = request.getAttribute("DBId").toString();
					
					String oldTitle = request.getAttribute("currTitle").toString(); 
					if ((null != oldTitle) && title.equals(oldTitle))
						title = "null";
					
					out.println(sendTaskToDb(DBId,
							title, 
							request.getAttribute("description").toString(), 
							request.getAttribute("frequency").toString(), 
							Long.parseLong(request.getAttribute("nextruntime").toString()), 
							request.getAttribute("inputcollection").toString(), 
							fileUrl, 
							request.getAttribute("mapper").toString(), 
							request.getAttribute("combiner").toString(), 
							request.getAttribute("reducer").toString(), 
							request.getAttribute("query").toString(), 
							request.getAttribute("outputkey").toString(), 
							request.getAttribute("outputvalue").toString(), 
							communities, request, response));
					
					if (title.equals("null"))
						title = oldTitle; // (reset this in case of edit where title hasn't changed)
				}
				else // If it fails, and it was a new upload then delete
				{
					if ((fileId.length() > 0) && newTask)
					{
						removeFromShare(fileId, request, response);
					}
					out.println("Upload Failed");
				}	
		 		//TESTED
				
				out.println("</div>");
		 		
	 		}//(end jar/task manipulation)
	 		
		} // (end if not a delete, file is valid/not-specified)
		
		// Call this here, AFTER all the logic (which can modify the DB) BEFORE any of the HTML (which needs the variables)
		
		communityList = generateCommunityList(request, response);
		taskList = populateExistingTasks(request, response, title);
		jarList = populatePreviousJarUploads(request, response);
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
			commListStr = JSON.stringify(commList);
				// (horrible way of doing it, but easiest way of modding the existing code)
			mult_comms = document.getElementById('communities');
			for ( var i = 0, l = mult_comms.options.length, o; i < l; i++ )
			{
			  o = mult_comms.options[i];
			  if(commListStr.indexOf(o.value) == -1)
				o.selected = false;
			  else  
			  	o.selected = true;
			}
		}
		
		function populate(selectedJson)
		{			
			// Get form entries:
			title = document.getElementById('title');
			description = document.getElementById('description');
			nextruntime = document.getElementById('nextruntime');
			frequency = document.getElementById('frequency');
			inputcollection = document.getElementById('inputcollection');
			mapper = document.getElementById('mapper');
			combiner = document.getElementById('combiner');
			reducer = document.getElementById('reducer');
			outputKey = document.getElementById('outputkey');
			outputValue = document.getElementById('outputvalue');
			query = document.getElementById('query');			
			file = document.getElementById('file');
			reusejar_checked = document.getElementById('reusejar_check');
			jar_url = document.getElementById('jar_url');
			var select = document.getElementById( 'communities' );

			// Other misc entries			
			refreshId = document.getElementById('refreshId');
			deleteId = document.getElementById('deleteId');
			deleteButton = document.getElementById('deleteButton');
			dropdown = document.getElementById("upload_info");			
			status_form = document.getElementById("status_form");
			jobstatus = document.getElementById("jobstatus");
			
			// Hidden entries
			DBId = document.getElementById('DBId');
			currTitle = document.getElementById('currTitle');
			currJarUrl = document.getElementById('currJarUrl');
			currJarTitle = document.getElementById('currJarTitle'); // (assign this based on the title)

			// If the page was saved from the last command, start that
			if ((selectedJson != null) && (selectedJson != "null"))
			{
				dropdown.value = selectedJson;				
			}//TESTED
			
			json = decodeURIComponent(dropdown.options[dropdown.selectedIndex].value.replace(/\+/g, " "));
			
			if (json == "new")
			{
				title.value = "(New Plugin)";
				description.value = "(Description Here)";
				frequency.value = "NONE";
				nextruntime.value = new Date().toLocaleString();
				inputcollection.value = "DOC_METADATA";
				
				mapper.value = "package.MainClass$MapperClass";
				combiner.value = "package.MainClass$CombinerClass";
				reducer.value = "package.MainClass$ReducerClass";
				
				outputKey.value = "com.mongodb.hadoop.io.BSONWritable";
				outputValue.value = "com.mongodb.hadoop.io.BSONWritable";
				
				query.value = "{}";
				
				jar_url.value = "";
				reusejar_checked.checked = false;
				
				DBId.value = "";
				deleteId.value = "";
				refreshId.value = "";
				currJarUrl.value = "";
				currTitle.value = "";
				deleteButton.style.visibility = 'hidden';
				status_form.style.display = 'none';
				
				useUrlJar();
				clearCommList();
				return;
			}
			else if (json == "copy")
			{
				title.value = title.value + " (COPY)";
				
				DBId.value = "";
				currJarUrl.value = "";
				currTitle.value = "";
				deleteId.value = "";
				refreshId.value = "";
				deleteButton.style.visibility = 'hidden';				
				status_form.style.display = 'none';
								
				return;				
			}
			var jsonObj = eval('(' + json + ')');
			
			deleteButton.style.visibility = '';			
			status_form.style.display = '';
			
			title.value = jsonObj.jobtitle;
			description.value = jsonObj.jobdesc;
			
			if ((jsonObj.nextRunTime < 0) || (4070908800000 == jsonObj.nextRunTime))
			{
				// (note -1 should never happen, it's only passed around internally to determine if to send null back to the API)				
				frequency.value = "null";
				nextruntime.value = new Date().toLocaleString();
			}//TESTED
			else if (jsonObj.scheduleFreq == "NONE")
			{
				// Two cases, has never run, has already run
				
				if (0 == jsonObj.timesRan) // Hasn't run
				{
					if (jsonObj.firstSchedule.indexOf("1969") >= 0)
					{
						nextruntime.value = "ASAP";
					}//TESTED
					else 
					{
						nextruntime.value = jsonObj.firstSchedule;					
					}//TESTED
					frequency.value = jsonObj.scheduleFreq;
				}//TESTED
				else // Won't run unless changed, so just put to now
				{					
					frequency.value = "null";
					nextruntime.value = new Date().toLocaleString();					
				}//TESTED
			}
			else
			{
				nextruntime.value = new Date(jsonObj.nextRunTime).toLocaleString();
				if (nextruntime.value == "Invalid Date")
				{
					nextruntime.value = jsonObj.firstSchedule;
				}
				frequency.value = jsonObj.scheduleFreq;
			}//TESTED
			//TESTED each of the above 5 clauses
			
			if (jsonObj.inputCollection == "doc_metadata.metadata")
			{
				inputcollection.value = "DOC_METADATA";
			}
			else 
			{
				inputcollection.value = jsonObj.inputCollection;
			}
			mapper.value = jsonObj.mapper;
			combiner.value = jsonObj.combiner;
			reducer.value = jsonObj.reducer;
			outputKey.value = jsonObj.outputKey;
			outputValue.value = jsonObj.outputValue;
			query.value = jsonObj.query;
			
			DBId.value = jsonObj._id;
			deleteId.value = jsonObj._id;
			refreshId.value = jsonObj.jobtitle;
			
			jar_url.value = jsonObj.jarURL;
			currTitle.value = jsonObj.jobtitle;
			currJarUrl.value = jsonObj.jarURL;
			if (jar_url.value == jsonObj.jarURL) // successfully found the share
			{
				reusejar_checked.checked = true;
				
				// Assign title to currJarTitle
				var text = jar_url.options[jar_url.selectedIndex].text;
				var index = text.indexOf(" (");
				if ((null != index) && (index > 0))
				{
					currJarTitle.value = text.substring(0, index);					
				}
				else 
					currJarTitle.value = text;					
			}//TESTED
			else 
			{
				reusejar_checked.checked = false;
			}
			useUrlJar();
			highlightComms(jsonObj.communityIds);
			
			// Status		
			var nRunningDuration_s = 0;
			if ((null != jsonObj.lastRunTime) && (null != jsonObj.jobidS))
			{
				nRunningDuration_s = Math.floor((new Date().getTime() - new Date(jsonObj.lastRunTime).getTime())/1000); // (ms->s)
			}
			if (null == jsonObj.jobidS)			
			{
				if (new Date().getTime() > jsonObj.nextRunTime)
				{
					jobstatus.value = "Pending";					
				}
				else if (0 == jsonObj.timesRan)
				{
					jobstatus.value = "Idle (times run: 0)";					
				}
				else
				{
					jobstatus.value = "Idle (times run: " + jsonObj.timesRan + ", last: " + jsonObj.lastCompletionTime + ")";
				}
			}
			else if (jsonObj.jobidS == "")
			{
				if (nRunningDuration_s > 180) 
				{
					jobstatus.value = "Initializing (error, probably invalid jar)";									
				}
				else if (nRunningDuration_s > 0) 
				{
					jobstatus.value = "Initializing (" + nRunningDuration_s + "s)";					
				}
				else				
					jobstatus.value = "Initializing";
			}
			else 
			{
				if (nRunningDuration_s > 0) 
				{
					jobstatus.value = "Running (job id: " + jsonObj.jobidS + ", for " + nRunningDuration_s + "s)";					
				}
				else				
					jobstatus.value = "Running (job id: " + jsonObj.jobidS + ")";				
				//TODO (INF-1480): Go get complete % from Hadoop REST call in JSP portion
			}
			//TESTED: test all 5 clauses
		}
		function useUrlJar()
		{
			file = document.getElementById('file');
			jar_url = document.getElementById('jar_url');
			
			if (document.getElementById('reusejar_check').checked)
			{
				jar_url.style.display = "";
				file.style.display = "none";
			}
			else
			{
				file.style.display = "";
				jar_url.style.display = "none";
			}
		}
		function validate_fields()
		{			
			title = document.getElementById('title').value;
			description = document.getElementById('description').value;
			nextruntime = document.getElementById('nextruntime').value;
			frequency = document.getElementById('frequency').value;
				// (no need to validate since it comes from a drop down)
			inputcollection = document.getElementById('inputcollection').value;
			mapper = document.getElementById('mapper').value;
			combiner = document.getElementById('combiner').value;
			reducer = document.getElementById('reducer').value;
			outputKey = document.getElementById('outputkey').value;
			outputValue = document.getElementById('outputvalue').value;
			query = document.getElementById('query').value;			
			file = document.getElementById('file').value;
			reusejar_checked = document.getElementById('reusejar_check').checked;
			jar_url = document.getElementById('jar_url').value;
			var select = document.getElementById( 'communities' );
			
			// Hidden entries
			DBId = document.getElementById('DBId').value;
			
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
			else
			{
				var regex = /^[a-zA-Z0-9_]+$/;
				if (!regex.test(title))
				{				
					alert('The title can consist of only of alphanumeric characters and _. ');
					return false;
				}//TESTED
			}
			if (description == "")
			{
				alert('Please provide a description.');
				return false;
			}
						
			if (query != "")
			{
				try {
					eval('(' + query + ')');
				}
				catch (err) {
					alert('Error parsing query: ' + err.toString())
				}
			}
			
			if ((mapper == "") || (combiner == "") || (reducer == ""))
			{
				alert('Please provide a mapper, combiner, and reducer.');
				return false;
			}			
			if ((outputKey == "") || (outputValue == ""))
			{
				alert('Please provide an output key and value.');
				return false;
			}
			
			if (file == "" && reusejar_checked == false)
			{
				alert('Please provide your JAR file.');
				return false;
			}
			if (jar_url == "" && reusejar_checked == true)
			{
				alert('Please provide the URL to your JAR file.');
				return false;
			}
			
			if (numSelected == 0)
			{
				alert('Please select at least 1 community');
				return false;
			}
			// Validate time (do this last since if it works we change the value):
			if ("null" == frequency) // don't want to run again 
			{
				// Two cases: either it's a new task, just set it to run once in the future
				if ((null == DBId) || ("" == DBId)) 
				{					
					document.getElementById('nextruntime').value = 4070908800000;
					document.getElementById('frequency').value = "NONE";
				}//TESTED
				else
				{
					document.getElementById('nextruntime').value = "-1";					
				}//TESTED
			}
			else if ("ASAP" == nextruntime)
			{
				document.getElementById('nextruntime').value = "0";
			}//TESTED
			else
			{
				var d = new Date(nextruntime);
				s = d.getTime() + "";
				if (s == "NaN")
				{
					alert('Please provide a valid next run time.');
					return false;
				}//TESTED
				// And then write the numeric value back into the form...
				document.getElementById('nextruntime').value = s;
				//TESTED
			}
			//TESTED (all 5 clauses above)
			
			return true;
		}
		function refreshStatus()
		{
			return true;
		}
		function confirmDelete()
		{
			var agree=confirm("Are you sure you wish to delete this MapReduce task?");
			if (agree)
				return true ;
			else
				return false ;
		}		
		// -->
		</script>
	</script>
		<div id="uploader_outter_div" name="uploader_outter_div" align="center" style="width:100%" >
	    	<div id="uploader_div" name="uploader_div" style="border-style:solid; border-color:#999999; border-radius: 10px; width:800px; margin:auto">
	        	<h2>MapReduce Plugin Manager</h2>
	        	<form id="delete_form" name="delete_form" method="post" enctype="multipart/form-data" onsubmit="javascript:return confirmDelete()" >
	        		<select id="upload_info" onchange="populate(null)" name="upload_info">
	        			<option value="new">Upload New Plugin</option> 
	        			<option value="copy">Copy Current Plugin</option> 
	        			<% out.print(taskList); %>
	        		</select>
	        		<input type="submit" name="deleteButton" id="deleteButton" style="visibility:hidden;" value="Delete" />
	        		<input type="hidden" name="deleteId" id="deleteId" />
	        	</form>
		        <form id="status_form" name="status_form" method="post" enctype="multipart/form-data" onsubmit="javascript:return refreshStatus();">
	                <table><tr>
		                <td>Run status:</td>
		                <td><input type="text" name="jobstatus" id="jobstatus" readonly="readonly" size="60" /></td>
			        	<td><input type="submit" name="refresh" id = "refresh" value="Refresh" /></td>
	                </tr></table>
		        	<input type="hidden" name="refreshId" id="refreshId" />
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
	                    <td>Next scheduled time:</td>
	                    <td>
	                    <input type="text" name="nextruntime" id="nextruntime" size="35"/>
	                    </td>
	                  </tr>
	                  <tr>
	                    <td>Frequency:</td>
						<td>
							<select name="frequency" id="frequency">
								<option value="NONE">Once Only</option>
								<option value="DAILY">Daily</option>
								<option value="WEEKLY">Weekly</option>
								<option value="MONTHLY">Monthly</option>
								<option value="null">Don't Run</option>
							</select>
						</td>
	                  </tr>
	                  <tr>
	                    <td>Input collection:</td>
						<td>
							<select name="inputcollection" id="inputcollection">
								<option value="DOC_METADATA">Document Metadata Collection</option>
								<% out.print(inputCollectionList); %>
							</select>
						</td>
	                  </tr>
	                  <tr>
	                    <td>Query:</td>
	                    <td><textarea rows="4" cols="60" name="query" id="query" ></textarea></td>
	                  </tr>
	                  <tr>
	                  	<td>Communities:</td>
	                  	<td><% out.print(communityList); %></td>
	                  </tr>
	                  <tr>
	                    <td>Mapper Class:</td>
	                    <td><input type="text" name="mapper" id="mapper" size="60" /></td>
	                  </tr>
	                  <tr>
	                    <td>Combiner Class:</td>
	                    <td><input type="text" name="combiner" id="combiner" size="60" /></td>
	                  </tr>
	                  <tr>
	                    <td>Reducer Class:</td>
	                    <td><input type="text" name="reducer" id="reducer" size="60" /></td>
	                  </tr>
	                  <tr>
	                    <td>Output Key Class:</td>
						<td>
							<select name="outputkey" id="outputkey">
								<option value="org.apache.hadoop.io.Text">org.apache.hadoop.io.Text</option>
								<option value="org.apache.hadoop.io.IntWritable">org.apache.hadoop.io.IntWritable</option>
								<option value="org.apache.hadoop.io.LongWritable">org.apache.hadoop.io.LongWritable</option>
								<option value="org.apache.hadoop.io.DoubleWritable">org.apache.hadoop.io.DoubleWritable</option>
								<option value="com.mongodb.hadoop.io.BSONWritable">com.mongodb.hadoop.io.BSONWritable</option>
							</select>
						</td>
	                  </tr>
	                  <tr>
	                    <td>Output Value Class:</td>
						<td>
							<select name="outputvalue" id="outputvalue">
								<option value="org.apache.hadoop.io.Text">org.apache.hadoop.io.Text</option>
								<option value="org.apache.hadoop.io.IntWritable">org.apache.hadoop.io.IntWritable</option>
								<option value="org.apache.hadoop.io.LongWritable">org.apache.hadoop.io.LongWritable</option>
								<option value="org.apache.hadoop.io.DoubleWritable">org.apache.hadoop.io.DoubleWritable</option>
								<option value="com.mongodb.hadoop.io.BSONWritable">com.mongodb.hadoop.io.BSONWritable</option>
							</select>
						</td>
	                  </tr>
	                  <tr>
	                    <td>JAR file:</td>
	                    <td>
	                    	<input type="file" name="file" id="file" size="60"/>
							<select name="jar_url" id="jar_url"><% out.print(jarList); %></select>
	                    	<input type="checkbox" id="reusejar_check" name="reusejar_check" onchange="useUrlJar()" /> 
	                    	<span id="reusejar_provide" name="reusejar_provide"> Reuse existing JAR </span>
	                    </td>
	                  </tr>
	                  <tr>
	                    <td colspan="2" style="text-align:right"><input type="submit" value="Submit" /></td>
	                  </tr>
	                </table>
					<input type="hidden" name="DBId" id="DBId" />
					<input type="hidden" name="currTitle" id="currTitle" />
					<input type="hidden" name="currJarUrl" id="currJarUrl" />
					<input type="hidden" name="currJarTitle" id="currJarTitle" />
				</form>
	        </div>
	        <form id="logout_form" name="logout_form" method="post">
	        	<input type="submit" name="logout" id = "logout" value="Log Out" />
	        </form>
	    </div>
	    </p>
	    
	    <script>		
		populate(<% out.print("'" + selectedJson + "'"); %>);
	    </script>
	
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
