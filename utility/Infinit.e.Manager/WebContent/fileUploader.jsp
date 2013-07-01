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
<%@page import="com.google.gson.JsonParser"%>
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
<%@ page contentType="text/html; charset=utf-8" language="java" import="java.io.*,java.util.*,java.net.*,com.google.gson.Gson,org.apache.commons.io.*,sun.misc.BASE64Encoder,java.security.*,java.util.zip.*;" errorPage="" %>
<%!static String API_ROOT = null;
	static String SHARE_ROOT = null;
	static Boolean DEBUG_MODE = false;
	static Boolean showAll = false;
	static Boolean localCookie = false;
	static String user = null;
	static String communityList = null; // (ensures that generateCommunityList is called)
	static CookieManager cm = new CookieManager();

	static class keepAlive {
		static class ka {
			String action;
			Boolean success;
			String message;
			int time;

		}

		public ka response;

	}

	static class personGet {
		static class resp {
			String action;
			Boolean success;
			String message;
			int time;

		}

		static class community {
			String _id;
			String name;
		}

		static class data {
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
	static class shareOwner {
		String _id;
		String email;
		String displayName;
	}

	static class shareCommunity {
		String _id;
		String name;
		String comment;
	}

	static class shareData {
		String _id;
		String created;
		String modified;
		shareOwner owner;
		String type;
		String title;
		String description;
		String mediaType;
		shareCommunity[] communities;
		String binaryId;

	}
	
	static class getShare {
		static class shareResponse {
			String action;
			Boolean success;
			String message;
			int time;

		}
		public shareResponse response;
		public shareData[] data;

	}

	static class getModules {
		static class widRes {
			String action;
			Boolean success;
			String message;
			int time;

		}

		public widRes response;

	}

	static class logIn {
		static class loginData {
			public String action;
			public Boolean success;
			public int time;

		}

		public loginData response;
	}
	
	static class jsonResponse {
		static class moduleResponse {
			public String action;
			public Boolean success;
			public String message;
			public int time;

		}
		
		public moduleResponse response;
		public shareData data;
	}

	static class modResponse {
		static class moduleResponse {
			public String action;
			public Boolean success;
			public String message;
			public int time;

		}

		public moduleResponse response;
		public String data;
	}

	static class widgetToDBResponse {
		static class wtdbResponse {
			public String action;
			public Boolean success;
			public String message;
			public int time;

		}

		static class wtdbData {
			public String _id;
			public Boolean approved;
		}

		public wtdbResponse response;
		public wtdbData data;
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

	public static String getBrowserInfiniteCookie(HttpServletRequest request) {
		Cookie[] cookieJar = request.getCookies();
		if (cookieJar != null) {
			for (Cookie cookie : cookieJar) {
				if (cookie.getName().equals("infinitecookie")) {
					//System.out.println("Got Browser Cookie Line 109: " + cookie.getValue());
					return cookie.getValue() + ";";
				}
			}
		}
		return null;
	}

	public static String getConnectionInfiniteCookie(URLConnection urlConnection) {
		Map<String, List<String>> headers = urlConnection.getHeaderFields();
		Set<Map.Entry<String, List<String>>> entrySet = headers.entrySet();

		for (Map.Entry<String, List<String>> entry : entrySet) {
			String headerName = entry.getKey();
			if (headerName != null && headerName.equals("Set-Cookie")) {
				List<String> headerValues = entry.getValue();
				for (String value : headerValues) {
					if (value.contains("infinitecookie")) {
						int equalsLoc = value.indexOf("=");
						int semicolonLoc = value.indexOf(";");
						//System.out.println("Got Connection Cookie Line 133: " + value.substring(equalsLoc+1,semicolonLoc));
						return value.substring(equalsLoc + 1, semicolonLoc);
					}
				}
			}
		}
		return null;
	}

	public static String stringOfUrl(String addr, HttpServletRequest request,
			HttpServletResponse response) {
		if (localCookie)
			CookieHandler.setDefault(cm);
		try {
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			URL url = new URL(addr);
			URLConnection urlConnection = url.openConnection();

			String cookieVal = getBrowserInfiniteCookie(request);
			if (cookieVal != null) {
				urlConnection.addRequestProperty("Cookie", "infinitecookie="
						+ cookieVal);
				urlConnection.setDoInput(true);
				urlConnection.setDoOutput(true);
				urlConnection.setRequestProperty("Accept-Charset", "UTF-8");
			} else if (DEBUG_MODE)
				System.out.println("Infinit.e Cookie Value is Null");
			IOUtils.copy(urlConnection.getInputStream(), output);
			String newCookie = getConnectionInfiniteCookie(urlConnection);
			if (newCookie != null && response != null) {
				setBrowserInfiniteCookie(response, newCookie, request.getServerPort());
			}

			String toReturn = output.toString();
			output.close();
			return toReturn;
		} catch (IOException e) {
			return null;
		}
	}

	private static String encrypt(String password)
			throws NoSuchAlgorithmException, UnsupportedEncodingException {
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		md.update(password.getBytes("UTF-8"));
		return URLEncoder.encode((new BASE64Encoder()).encode(md.digest()),
				"UTF-8");
	}

	private Boolean logMeIn(String username, String pword,
			HttpServletRequest request, HttpServletResponse response)
			throws IOException, NoSuchAlgorithmException,
			UnsupportedEncodingException, URISyntaxException {
		String json = stringOfUrl(API_ROOT + "auth/login/" + username + "/"
				+ encrypt(pword), request, response);
		logIn login = new Gson().fromJson(json, logIn.class);
		if (login == null)
			return false;
		user = username;
		return login.response.success;
	}

	private void logOut(HttpServletRequest request, HttpServletResponse response)
			throws IOException, NoSuchAlgorithmException,
			UnsupportedEncodingException, URISyntaxException {
		String json = stringOfUrl(API_ROOT + "auth/logout", request, response);

	}

	public Boolean isLoggedIn(HttpServletRequest request,
			HttpServletResponse response) {
		String json = stringOfUrl(API_ROOT + "auth/keepalive", request,
				response);
		if (json != null) {
			keepAlive keepA = new Gson().fromJson(json, keepAlive.class);
			return keepA.response.success;
		} else {
			return null;
		}
	}

	public String removeFromShare(String shareId, HttpServletRequest request,
			HttpServletResponse response) {
		if (shareId != null) {
			String json = stringOfUrl(API_ROOT + "social/share/remove/" + shareId
					+ "/", request, response);
			if (json != null) {
				keepAlive keepA = new Gson().fromJson(json, keepAlive.class);
				return keepA.response.message;
			}
		}
		return null;
	}

	//uploads a new widget's bytes and returns it's shareID if successful. If a share
	//ID is provided, then it updates the widget containing that shareID
	private String UpdateToShare(byte[] bytes, String mimeType, String title,
			String description, String prevId, Set<String> communities, boolean isJson, 
			String type, boolean newShare,
			HttpServletRequest request, HttpServletResponse response) {
		String charset = "UTF-8";
		String url = "";
		try 
		{
			if ( isJson )
			{
				//first check if bytes are actually json
				try
				{					
					new JsonParser().parse(new String(bytes));
				}
				catch (Exception ex)
				{
					return "Failed, file was not valid JSON";
				}
				if (newShare)
					url = API_ROOT + "social/share/add/json/"
							+ URLEncoder.encode(type, charset) + "/"
							+ URLEncoder.encode(title, charset) + "/"
							+ URLEncoder.encode(description, charset) + "/";
				else
					url = API_ROOT + "social/share/update/json/" + prevId + "/"
							+ URLEncoder.encode(type, charset) + "/"
							+ URLEncoder.encode(title, charset) + "/"
							+ URLEncoder.encode(description, charset) + "/";
			}
			else
			{
				if (newShare)
					url = API_ROOT + "social/share/add/binary/"
							+ URLEncoder.encode(title, charset) + "/"
							+ URLEncoder.encode(description, charset) + "/";
				else
					url = API_ROOT + "social/share/update/binary/" + prevId + "/"
							+ URLEncoder.encode(title, charset) + "/"
							+ URLEncoder.encode(description, charset) + "/";
			}

			if (localCookie)
				CookieHandler.setDefault(cm);
			URLConnection connection = new URL(url).openConnection();
			connection.setDoOutput(true);
			connection.setRequestProperty("Accept-Charset", charset);
			String cookieVal = getBrowserInfiniteCookie(request);
			if (cookieVal != null) 
			{
				connection.addRequestProperty("Cookie", "infinitecookie="
						+ cookieVal);
				connection.setDoInput(true);
				connection.setDoOutput(true);
				connection.setRequestProperty("Accept-Charset", "UTF-8");
			}
			if (mimeType != null && mimeType.length() > 0)
				connection.setRequestProperty("Content-Type", mimeType
						+ ";charset=" + charset);
			DataOutputStream output = new DataOutputStream(
					connection.getOutputStream());
			output.write(bytes);
			DataInputStream responseStream = new DataInputStream(
					connection.getInputStream());

			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			int nRead;
			byte[] data = new byte[16384];
			while ((nRead = responseStream.read(data, 0, data.length)) != -1) {
				buffer.write(data, 0, nRead);
			}

			String json = buffer.toString();
			String newCookie = getConnectionInfiniteCookie(connection);
			if (newCookie != null && response != null) {
				setBrowserInfiniteCookie(response, newCookie, request.getServerPort());
			}
			buffer.flush();
			buffer.close();
			output.close();
			responseStream.close();
			
			if ( isJson )
			{
				jsonResponse jr = new Gson().fromJson(json, jsonResponse.class);
				if (jr == null) 
				{
					return "Failed: " + json;
				}
				if (jr.response.success == true) 
				{
					if ( jr.data != null && jr.data._id != null )
					{
						addRemoveCommunities(jr.data._id, communities, request, response);
						return jr.data._id; //When a new upload, mr.data contains the ShareID for the upload
					}
				} 
				return "Upload Failed: " + jr.response.message;				
			}
			else
			{
				modResponse mr = new Gson().fromJson(json, modResponse.class);
				if (mr == null) {
					return "Failed: " + json;
				}
				if (mr.response.success == true) 
				{
					if (prevId != null && mr.data == null) 
					{
						addRemoveCommunities(prevId, communities, request, response);
						return prevId;
					}
					else
					{
						addRemoveCommunities(mr.data, communities, request, response);
						return mr.data; //When a new upload, mr.data contains the ShareID for the upload
					}
				} 
				else 
				{
					return "Upload Failed: " + mr.response.message;
				}
			}			
		} catch (IOException e) {
			e.printStackTrace();
			return "Upload Failed: " + e.getMessage();
		}
	}

	private void addRemoveCommunities(String shareId, Set<String> commsToAdd,
			HttpServletRequest request, HttpServletResponse response) 
	{
		personGet.community[] userCommunities = getUserCommunities(request,
				response);

		for (personGet.community userComm : userCommunities) {
			if (stringInSet(userComm._id, commsToAdd))
				addShareToCommunity(shareId, userComm._id, request, response);
			else
				removeShareFromCommunity(shareId, userComm._id, request,
						response);
		}
	}

	private Boolean stringInSet(String value, Set<String> set) {
		if (set != null) {
			for (String compare : set) {
				if (value.equalsIgnoreCase(compare))
					return true;
			}
		}
		return false;
	}

	private String addShareToCommunity(String shareId, String communityId,
			HttpServletRequest request, HttpServletResponse response) {
		try {
			String charset = "UTF-8";

			String comment = "Added by fileUploader";

			///share/add/community/{shareid}/{comment}/{communityid}
			String json = stringOfUrl(
					API_ROOT + "social/share/add/community/"
							+ URLEncoder.encode(shareId, charset) + "/"
							+ URLEncoder.encode(comment, charset) + "/"
							+ URLEncoder.encode(communityId, charset) + "/",
					request, response);
			getModules gm = new Gson().fromJson(json, getModules.class);
			if (gm == null)
				return "Json was null: " + json + "\n " + API_ROOT
						+ "social/share/add/community/"
						+ URLEncoder.encode(shareId, charset) + "/"
						+ URLEncoder.encode(communityId, charset) + "/"
						+ URLEncoder.encode(comment, charset) + "/";
			return gm.response.message + API_ROOT + "social/share/add/community/"
					+ URLEncoder.encode(shareId, charset) + "/"
					+ URLEncoder.encode(communityId, charset) + "/"
					+ URLEncoder.encode(comment, charset) + "/";
		} catch (IOException e) {
			e.printStackTrace();
			return e.getMessage();
		}

	}

	private String removeShareFromCommunity(String shareId, String communityId,
			HttpServletRequest request, HttpServletResponse response) {
		try {
			String charset = "UTF-8";

			String comment = "Added by fileUploader";

			///social/share/remove/community/{shareid}/{communityid}
			String json = stringOfUrl(
					API_ROOT + "social/share/remove/community/"
							+ URLEncoder.encode(shareId, charset) + "/"
							+ URLEncoder.encode(communityId, charset) + "/",
					request, response);
			if (DEBUG_MODE)
				System.out.println("Removing from Community:" + json);
			getModules gm = new Gson().fromJson(json, getModules.class);
			if (gm == null)
				return "Json was null: " + json + "\n " + API_ROOT
						+ "social/share/remove/community/"
						+ URLEncoder.encode(shareId, charset) + "/"
						+ URLEncoder.encode(comment, charset) + "/";
			return gm.response.message + API_ROOT
					+ "social/share/remove/community/"
					+ URLEncoder.encode(shareId, charset) + "/"
					+ URLEncoder.encode(communityId, charset) + "/";
		} catch (IOException e) {
			e.printStackTrace();
			return e.getMessage();
		}

	}

	private personGet.community[] getUserCommunities(
			HttpServletRequest request, HttpServletResponse response) {
		try {
			String charset = "UTF-8";

			String json = stringOfUrl(API_ROOT + "person/get/", request,
					response);
			personGet pg = new Gson().fromJson(json, personGet.class);
			if (pg != null) {
				user = pg.data.email;
				return pg.data.communities;
			}
			return null;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public String generateCommunityList(HttpServletRequest request,
			HttpServletResponse response) {
		String toReturn = "<select multiple=\"multiple\" name=\"communities\" id=\"communities\">";
		personGet.community[] pgs = getUserCommunities(request, response);
		if (pgs != null) {
			for (personGet.community comm : pgs) {
				toReturn += "<option value=\"" + comm._id + "\">" + comm.name
						+ "</option>";
			}
		}
		return toReturn + "</select>";
	}

	private String populatePreviousUploads(HttpServletRequest request,
			HttpServletResponse response) {
		String toReturn = "";
		String delim = "$$$";
		String ext = null;
		Object o = request.getParameter("ext");
		if (null != o) {
			ext = o.toString();
		}
		String searchCriteria = "";
		if ((null != ext) && ext.startsWith("type:")) {
			searchCriteria = "?type=" + ext.substring(5);
			ext = null;
		} else if ((null != ext) && ext.equalsIgnoreCase("see all")) {
			ext = null;
		}
		String json = stringOfUrl(API_ROOT + "social/share/search/"
				+ searchCriteria, request, response);

		if (json != null) 
		{
			getShare gs = new Gson().fromJson(json, getShare.class);
			if (gs != null && gs.data != null) 
			{
				for (shareData info : gs.data) {
					if ((showAll == false) && (info.owner != null)
							&& (info.owner.email != null)
							&& !user.equalsIgnoreCase(info.owner.email)) {
						continue;
					}

					String owner = "unknown";
					if ((null != info.owner) && (null != info.owner.email)) {
						owner = info.owner.email;
					}
					if (ext == null) {
						String value = info._id + delim + info.created + delim
								+ info.title + delim + info.description + delim
								+ SHARE_ROOT + info._id + delim;
						for (shareCommunity scomm : info.communities) {
							value += scomm._id + ",";
						}
						value += delim + owner + delim + info.binaryId + delim + info.type;
						toReturn += "<option value=\"" + value
								+ "\" > <b>Edit:</b> " + info.title
								+ "</option>";
					} 
					else 
					{
						if (ext.contains("jar") || ext.contains("JAR")) 
						{
							if ((null != info.mediaType)
									&& (info.mediaType
											.equalsIgnoreCase("application/java-archive")
											|| info.mediaType
													.equalsIgnoreCase("application/x-java-archive") || info.mediaType
											.equalsIgnoreCase("application/octet-stream"))) {
								if (DEBUG_MODE)
									System.out
											.println("Outputting Uploaded Jar files");
								String value = info._id + delim + info.created
										+ delim + info.title + delim
										+ info.description + delim + SHARE_ROOT
										+ info._id + delim;
								for (shareCommunity scomm : info.communities) {
									value += scomm._id + ",";
								}
								value += delim + owner + delim + info.binaryId + delim + info.type;
								toReturn += "<option value=\"" + value
										+ "\" > <b>Edit:</b> " + info.title
										+ "</option>";
							}
						} else if ((null != info.mediaType)
								&& info.mediaType.contains(ext)) {
							if (DEBUG_MODE)
								System.out.println("Extension '" + ext
										+ "' Triggered Mime Type : "
										+ info.mediaType.toString());
							String value = info._id + delim + info.created
									+ delim + info.title + delim
									+ info.description + delim + SHARE_ROOT
									+ info._id + delim;
							for (shareCommunity scomm : info.communities) {
								value += scomm._id + ",";
							}
							value += delim + owner + delim + info.binaryId + delim + info.type;
							toReturn += "<option value=\"" + value
									+ "\" > <b>Edit:</b> " + info.title
									+ "</option>";
						}
					}

				}
			}
		}
		return toReturn;
	}

	private String populateMediaTypes(HttpServletRequest request,
			HttpServletResponse response) {
		String toReturn = "<option> See All </option>";
		String delim = "$$$";
		String json = stringOfUrl(API_ROOT + "social/share/search/", request,
				response);
		String ext = null;
		Object o = request.getParameter("ext");
		if (null != o) {
			ext = o.toString();
		}

		if (json != null) {
			getShare gs = new Gson().fromJson(json, getShare.class);
			if (gs != null && gs.data != null) {
				Set<String> set = new TreeSet<String>(
						String.CASE_INSENSITIVE_ORDER);
				for (shareData info : gs.data) {
					if ((showAll == false) && (info.owner != null)
							&& (info.owner.email != null)
							&& !user.equalsIgnoreCase(info.owner.email)) {
						continue;
					}
					if (info.mediaType != null && !set.contains(info.mediaType)) {
						set.add(info.mediaType);
					}
					if (info.type != null && !set.contains(info.type)) {
						set.add("type:" + info.type);
					}
				}

				for (String mt : set) {
					String selected_text = "";
					if ((null != ext) && mt.equalsIgnoreCase(ext)) {
						selected_text = "selected";
					}

					toReturn += "<option " + selected_text + " value=\"" + mt
							+ "\" > " + mt + "</option>";
				}

			}
		}
		return toReturn;
	}%>
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

<body onload="populate()">
<%
	if (API_ROOT == null) 
	{
		ServletContext context = session.getServletContext();
		String realContextPath = context.getRealPath("/");
		ScriptEngineManager manager = new ScriptEngineManager();
		ScriptEngine engine = manager.getEngineByName("javascript");
		try 
		{ // EC2 Machines
			FileReader reader = new FileReader(realContextPath
					+ "/AppConstants.js");
			engine.eval(reader);
			reader.close();
			engine.eval("output = getEndPointUrl();");
			API_ROOT = (String) engine.get("output");
			SHARE_ROOT = API_ROOT + "share/get/";
		} 
		catch (Exception je) 
		{
			try 
			{ ////////////Windows + Tomcat
				FileReader reader = new FileReader(realContextPath
						+ "\\..\\AppConstants.js");
				engine.eval(reader);
				reader.close();
				engine.eval("output = getEndPointUrl();");
				API_ROOT = (String) engine.get("output");
				SHARE_ROOT = API_ROOT + "share/get/";
			} 
			catch (Exception e) 
			{
				System.err.println(e.toString());
			}
		}
		if (null == API_ROOT) { 
			// Default to localhost
			API_ROOT = "http://localhost:8080/api/";
			SHARE_ROOT = "$infinite/share/get/";
		}

		if (API_ROOT.contains("localhost"))
			localCookie = true;
		else
			localCookie = false;
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
		DEBUG_MODE = (request.getParameter("debug") != null);
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
			if ((contentType != null)
					&& (contentType.indexOf("multipart/form-data") >= 0)) 
			{

				//		Create a new file upload handler
				ServletFileUpload upload = new ServletFileUpload(
						new DiskFileItemFactory());
				//		Parse the request
				FileItemIterator iter = upload.getItemIterator(request);
				byte[] fileBytes = null;
				String fileDS = null;
				byte[] iconBytes = null;
				String iconDS = null;
				Set<String> communities = new HashSet<String>();
				boolean isFileSet = false;
				while (iter.hasNext()) 
				{
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
							request.setAttribute(name, Streams.asString(stream));

						//out.println("<b>" + name + ":</b>" + request.getAttribute(name).toString()+"</br>");
					} 
					else 
					{
						if (name.equalsIgnoreCase("file")) 
						{
							if ( !item.getName().equals(""))
								isFileSet = true;
							fileDS = item.getContentType();
							fileBytes = IOUtils.toByteArray(stream);

							// Check if this should be a java-archive (rather than just an octet stream)
							if (fileDS.equals("application/octet-stream"))
							{
								ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(fileBytes));
								ZipEntry entry;
								while ((entry = zis.getNextEntry()) != null)
								{
									if (entry.getName().endsWith(".class"))
									{
										fileDS = "application/java-archive";
										break;
									}
								}
							}
							// Reset stream, and read
						}
					}
				}
				
				////////////////////////////////////Delete Share ////////////////////////////////
				if (request.getAttribute("deleteId") != null) {
					String fileId = request.getAttribute("deleteId")
							.toString();
					if (fileId != null && fileId != "")
						removeFromShare(fileId, request, response)
								.toString();

				}
				////////////////////////////////////Update Community Info////////////////////////////////
				else if (null == fileBytes) 
				{
					String shareId = request.getAttribute("DBId").toString();
					if (shareId != null && shareId != "")
						addRemoveCommunities(shareId, communities, request, response);					
				} 
				else 
				{
					//////////////////////////////////////////////////////////////////////////////////

					Boolean newUpload = (request.getAttribute("DBId").toString().length() == 0);

					///////////////////////////////// SWF Manip  /////////////////////////////////
					String shareId = request.getAttribute("DBId").toString();
					String fileUrl = "";
					String fileId = "";
					String bin = request.getAttribute("binary").toString();		
					if (request.getAttribute("title") != null
							&& request.getAttribute("description") != null
							&& fileBytes != null) 
					{						
						if ( !isFileSet ) //if not a binary file or file was not changed
						{												
							fileId = shareId;
							if (shareId != null && shareId != "")
								addRemoveCommunities(shareId, communities, request, response);							
							out.println("File was not set, just updated communities.");
						}
						else if ( bin.equals("null")) //is a json file, make sure its okay and upload it
						{
							fileId = UpdateToShare(fileBytes, fileDS, request
									.getAttribute("title").toString(),
									request.getAttribute("description")
											.toString(), shareId,
									communities, true, request.getAttribute("type")
									.toString(), newUpload, request, response);
						}
						else //is a binary, do normal
						{
							fileId = UpdateToShare(fileBytes, fileDS, request
									.getAttribute("title").toString(),
									request.getAttribute("description")
											.toString(), shareId,
									communities, false, request.getAttribute("type")
									.toString(), newUpload, request, response);
						}

						if (fileId.contains("Failed")) 
						{
							out.println(fileId);
						} 
						else 
						{
							fileUrl = SHARE_ROOT + fileId;
							if (newUpload)
								out.println("You have successfully added a file to the share, its location is: "
										+ fileUrl);
							else
								out.println("You have successfully updated a file on the share, its location is: "
										+ fileUrl);
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
		var typerow = document.getElementById('typerow');
		var type = document.getElementById('type');
		var title = document.getElementById('title');
		var description = document.getElementById('description');
		var file = document.getElementById('file');
		var created = document.getElementById('created');
		var DBId = document.getElementById('DBId');
		var deleteId = document.getElementById('deleteId');
		var deleteButton = document.getElementById('deleteButton');
		var share_url = document.getElementById('share_url');
		var owner_text = document.getElementById('owner_text');
		var owner = document.getElementById('owner');
		var url_row = document.getElementById('url_row');
		var dropdown = document.getElementById("upload_info");
		var list = dropdown.options[dropdown.selectedIndex].value;
		var binary = document.getElementById("binary");
		
		if (list == "new")
		{
			title.value = "";
			description.value = "";
			type.value = "binary";
			created.value = "";
			DBId.value = "";
			deleteId.value = "";
			share_url.value = "";
			owner.value = "";
			typerow.className = "hide";
			url_row.className = "hide";
			owner.className = "hide";
			owner_text.className = "hide";
			deleteButton.className = "hide";
			clearCommList();
			binary.value = "";
			return;
		}
		
		if ( list == "newJSON")
		{
			title.value = "";
			description.value = "";
			type.value = "";
			created.value = "";
			DBId.value = "";
			deleteId.value = "";
			share_url.value = "";
			owner.value = "";
			typerow.className = "show";
			url_row.className = "hide";
			owner.className = "hide";
			owner_text.className = "hide";
			deleteButton.className = "hide";
			clearCommList();
			binary.value = "null";
			return;
		}
		
		//_id, created, title, description
		split = list.split("$$$");
		
		res_id = split[0];
		res_created = split[1];
		res_title = split[2];
		res_description = split[3];
		res_url = split[4];
		communities = split[5];
		res_owner = split[6];
		res_binary = split[7];		
		res_type = split[8];			
		
		if ( res_binary == "null" )
		{
			typerow.className = "show";
		}
		else
		{
			typerow.className = "hide";
		}
		title.value = res_title;
		description.value = res_description;
		created.value = res_created;
		DBId.value = res_id;
		deleteId.value = res_id;
		share_url.value = res_url;
		owner.value = res_owner;		
		deleteButton.className = "show";
		owner.className = "show";
		owner_text.className = "show";
		url_row.className = "show";
		highlightComms(communities);		
		binary.value = res_binary;
		type.value = res_type;
	}
		function validate_fields()
		{
			title = document.getElementById('title').value;
			description = document.getElementById('description').value;
			file = document.getElementById('file').value;
			binary = document.getElementById("binary").value;
			type = document.getElementById("type").value;
			//share_url = document.getElementById('share_url').value;
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
			if ( binary == "null" && type == "")
			{
				alert('Please provide a type.');
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
		function showResults()
		{
			var title = document.getElementById('DBId').value;
			var url = getEndPointUrl() + "share/get/" + title + "?nometa=true";
			window.open(url, '_blank');
			window.focus();			
		}
		// -->
		</script>
	</script>
		<div id="uploader_outter_div" name="uploader_outter_div" align="center" style="width:100%" >
	    	<div id="uploader_div" name="uploader_div" style="border-style:solid; border-color:#999999; border-radius: 10px; width:475px; margin:auto">
	        	<h2>File Uploader</h2>
	        	<form id="search_form" name="search_form" method="get">
	        		<div align="center"">
	        		<label for="ext">Filter On</label>
					  <select name="ext" id="ext" onchange="this.form.submit();">
					    <%
					    	out.print(populateMediaTypes(request, response));
					    %>
					  </select>
					 </div>
					 <%
					 	if (showAll)
					 				out.print("<input type=\"hidden\" name=\"sudo\" id=\"sudo\" value=\"true\" />");
					 %>	        		
	        	</form>
	        	<form id="delete_form" name="delete_form" method="post" enctype="multipart/form-data" onsubmit="javascript:return confirmDelete()" >
	        		<select id="upload_info" onchange="populate()" name="upload_info"><option value="new">Upload New File</option><option value="newJSON">Upload New JSON</option> <%
 	out.print(populatePreviousUploads(request, response));
 %></select>
	        		<input type="submit" name="deleteButton" id="deleteButton" class="hidden" value="Delete" />
	        		<input type="hidden" name="deleteId" id="deleteId" />
	        		<input type="hidden" name="deleteFile" id="deleteFile" />
					 <%
					 	if (showAll)
					 				out.print("<input type=\"hidden\" name=\"sudo\" id=\"sudo\" value=\"true\" />");
					 %>	        		
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
	                  <tr id="typerow">
	                    <td>Type:</td>
	                    <td><input type="text" name="type" id="type" size="39" /></td>
	                  </tr>
	                  <tr>
	                  	<td>Communities:</td>
	                  	<td><%
	                  		out.print(communityList);
	                  	%></td>
	                  </tr>
	                  <tr>
	                  	<td id="owner_text">Owner:</td>
	                  	<td>
	                    <input type="text" name="owner" id="owner" readonly="readonly" size="25" />
	                  	</td>
	                  </tr>
	                  <tr>
	                    <td>File:</td>
	                    <td><input type="file" name="file" id="file" /></td>
	                  </tr>
	                  <tr id="url_row" class="hide">
	                  	<td>Share URL:</td>
	                  	<td><input type="text" name="share_url" id="share_url" readonly="readonly" size="38"/>
	                  	<input type="button" onclick="showResults()" value="View"/>
	                  	</td>
	                	<td></td>
	                  </tr>
	                  <tr>
	                    <td colspan="2" style="text-align:right"><input type="submit" value="Submit" /></td>
	                  </tr>
	                </table>
					<input type="hidden" name="created" id="created" />
					<input type="hidden" name="DBId" id="DBId" />
					<input type="hidden" name="fileUrl" id="fileUrl" />
					<input type="hidden" name="binary" id="binary" />
					 <%
					 	if (showAll)
					 				out.print("<input type=\"hidden\" name=\"sudo\" id=\"sudo\" value=\"true\" />");
					 %>	        		
				</form>
	        </div>
	        <form id="logout_form" name="logout_form" method="post">
	        	<input type="submit" name="logout" id = "logout" value="Log Out" />
	        </form>
	    </div>
	    </p>
	
<%
		}
		} else if (isLoggedIn == false) {
			//localCookie =(request.getParameter("local") != null);
			//System.out.println("LocalCookie = " + localCookie.toString());
			String errorMsg = "";
			if (request.getParameter("logintext") != null
					|| request.getParameter("passwordtext") != null) {
				if (logMeIn(request.getParameter("logintext"),
						request.getParameter("passwordtext"), request,
						response)) {
					showAll = (request.getParameter("sudo") != null);
					out.println("<meta http-equiv=\"refresh\" content=\"0\">");
					out.println("Login Success");
				} else {
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
	<div style="color: red; text-align: center;"> <%=errorMsg%> </div>
<%
	}
%>
    
    
</body>
</html>