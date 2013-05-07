package org.apache.jsp;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;
import com.google.gson.JsonParser;
import java.net.FileNameMap;
import java.net.URLConnection;
import org.apache.jasper.JasperException;
import javax.script.ScriptException;
import javax.script.ScriptEngineManager;
import javax.script.ScriptEngine;
import org.apache.commons.fileupload.util.Streams;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import java.io.*;
import java.util.*;
import java.net.*;
import com.google.gson.Gson;
import org.apache.commons.io.*;
import sun.misc.BASE64Encoder;
import java.security.*;
import java.util.zip.*;;

public final class fileUploader_jsp extends org.apache.jasper.runtime.HttpJspBase
    implements org.apache.jasper.runtime.JspSourceDependent {

static String API_ROOT = null;
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
	}
  private static final JspFactory _jspxFactory = JspFactory.getDefaultFactory();

  private static java.util.List _jspx_dependants;

  private javax.el.ExpressionFactory _el_expressionfactory;
  private org.apache.AnnotationProcessor _jsp_annotationprocessor;

  public Object getDependants() {
    return _jspx_dependants;
  }

  public void _jspInit() {
    _el_expressionfactory = _jspxFactory.getJspApplicationContext(getServletConfig().getServletContext()).getExpressionFactory();
    _jsp_annotationprocessor = (org.apache.AnnotationProcessor) getServletConfig().getServletContext().getAttribute(org.apache.AnnotationProcessor.class.getName());
  }

  public void _jspDestroy() {
  }

  public void _jspService(HttpServletRequest request, HttpServletResponse response)
        throws java.io.IOException, ServletException {

    PageContext pageContext = null;
    HttpSession session = null;
    ServletContext application = null;
    ServletConfig config = null;
    JspWriter out = null;
    Object page = this;
    JspWriter _jspx_out = null;
    PageContext _jspx_page_context = null;


    try {
      response.setContentType("text/html; charset=utf-8");
      pageContext = _jspxFactory.getPageContext(this, request, response,
      			"", true, 8192, true);
      _jspx_page_context = pageContext;
      application = pageContext.getServletContext();
      config = pageContext.getServletConfig();
      session = pageContext.getSession();
      out = pageContext.getOut();
      _jspx_out = out;

      out.write("<!--\n");
      out.write("Copyright 2012 The Infinit.e Open Source Project\n");
      out.write("\n");
      out.write("Licensed under the Apache License, Version 2.0 (the \"License\");\n");
      out.write("you may not use this file except in compliance with the License.\n");
      out.write("You may obtain a copy of the License at\n");
      out.write("\n");
      out.write("  http://www.apache.org/licenses/LICENSE-2.0\n");
      out.write("\n");
      out.write("Unless required by applicable law or agreed to in writing, software\n");
      out.write("distributed under the License is distributed on an \"AS IS\" BASIS,\n");
      out.write("WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n");
      out.write("See the License for the specific language governing permissions and\n");
      out.write("limitations under the License.\n");
      out.write("-->\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n");
      out.write("<html xmlns=\"http://www.w3.org/1999/xhtml\">\n");
      out.write("<head>\n");
      out.write("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />\n");
      out.write("<title>Infinit.e File Upload Tool</title>\n");
      out.write("<style media=\"screen\" type=\"text/css\">\n");
      out.write("\n");
      out.write("body \n");
      out.write("{\n");
      out.write("\tfont: 14px Arial,sans-serif;\n");
      out.write("}\n");
      out.write("h2\n");
      out.write("{\n");
      out.write("\tfont-family: \"Times New Roman\";\n");
      out.write("\tfont-style: italic;\n");
      out.write("\tfont-variant: normal;\n");
      out.write("\tfont-weight: normal;\n");
      out.write("\tfont-size: 24px;\n");
      out.write("\tline-height: 29px;\n");
      out.write("\tfont-size-adjust: none;\n");
      out.write("\tfont-stretch: normal;\n");
      out.write("\t-x-system-font: none;\n");
      out.write("\tcolor: #d2331f;\n");
      out.write("\tmargin-bottom: 25px;\n");
      out.write("}\n");
      out.write(".show {\n");
      out.write("display: ;\n");
      out.write("visibility: visible;\n");
      out.write("}\n");
      out.write(".hide {\n");
      out.write("display: none;\n");
      out.write("visibility: hidden;\n");
      out.write("}\n");
      out.write("</style>\n");
      out.write("<script language=\"javascript\" src=\"AppConstants.js\"> </script>\n");
      out.write("</head>\n");
      out.write("\n");
      out.write("<body onload=\"populate()\">\n");

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

      out.write("\n");
      out.write("\t\n");
      out.write("\t<script>\n");
      out.write("\tfunction clearCommList()\n");
      out.write("\t\t{\n");
      out.write("\t\t\tmult_comms = document.getElementById('communities');\n");
      out.write("\t\t\tfor ( var i = 0, l = mult_comms.options.length, o; i < l; i++ )\n");
      out.write("\t\t\t{\n");
      out.write("\t\t\t  o = mult_comms.options[i];\n");
      out.write("\t\t\t  o.selected = false;\n");
      out.write("\t\t\t}\n");
      out.write("\t\t}\n");
      out.write("\t\tfunction highlightComms(commList)\n");
      out.write("\t\t{\n");
      out.write("\t\t\tmult_comms = document.getElementById('communities');\n");
      out.write("\t\t\tfor ( var i = 0, l = mult_comms.options.length, o; i < l; i++ )\n");
      out.write("\t\t\t{\n");
      out.write("\t\t\t  o = mult_comms.options[i];\n");
      out.write("\t\t\t  if(commList.indexOf(o.value) == -1)\n");
      out.write("\t\t\t\to.selected = false;\n");
      out.write("\t\t\t  else  \n");
      out.write("\t\t\t  \to.selected = true;\n");
      out.write("\t\t\t}\n");
      out.write("\t\t}\n");
      out.write("\tfunction populate()\n");
      out.write("\t{\n");
      out.write("\t\tvar typerow = document.getElementById('typerow');\n");
      out.write("\t\tvar type = document.getElementById('type');\n");
      out.write("\t\tvar title = document.getElementById('title');\n");
      out.write("\t\tvar description = document.getElementById('description');\n");
      out.write("\t\tvar file = document.getElementById('file');\n");
      out.write("\t\tvar created = document.getElementById('created');\n");
      out.write("\t\tvar DBId = document.getElementById('DBId');\n");
      out.write("\t\tvar deleteId = document.getElementById('deleteId');\n");
      out.write("\t\tvar deleteButton = document.getElementById('deleteButton');\n");
      out.write("\t\tvar share_url = document.getElementById('share_url');\n");
      out.write("\t\tvar owner_text = document.getElementById('owner_text');\n");
      out.write("\t\tvar owner = document.getElementById('owner');\n");
      out.write("\t\tvar url_row = document.getElementById('url_row');\n");
      out.write("\t\tvar dropdown = document.getElementById(\"upload_info\");\n");
      out.write("\t\tvar list = dropdown.options[dropdown.selectedIndex].value;\n");
      out.write("\t\tvar binary = document.getElementById(\"binary\");\n");
      out.write("\t\t\n");
      out.write("\t\tif (list == \"new\")\n");
      out.write("\t\t{\n");
      out.write("\t\t\ttitle.value = \"\";\n");
      out.write("\t\t\tdescription.value = \"\";\n");
      out.write("\t\t\ttype.value = \"binary\";\n");
      out.write("\t\t\tcreated.value = \"\";\n");
      out.write("\t\t\tDBId.value = \"\";\n");
      out.write("\t\t\tdeleteId.value = \"\";\n");
      out.write("\t\t\tshare_url.value = \"\";\n");
      out.write("\t\t\towner.value = \"\";\n");
      out.write("\t\t\ttyperow.className = \"hide\";\n");
      out.write("\t\t\turl_row.className = \"hide\";\n");
      out.write("\t\t\towner.className = \"hide\";\n");
      out.write("\t\t\towner_text.className = \"hide\";\n");
      out.write("\t\t\tdeleteButton.className = \"hide\";\n");
      out.write("\t\t\tclearCommList();\n");
      out.write("\t\t\tbinary.value = \"\";\n");
      out.write("\t\t\treturn;\n");
      out.write("\t\t}\n");
      out.write("\t\t\n");
      out.write("\t\tif ( list == \"newJSON\")\n");
      out.write("\t\t{\n");
      out.write("\t\t\ttitle.value = \"\";\n");
      out.write("\t\t\tdescription.value = \"\";\n");
      out.write("\t\t\ttype.value = \"\";\n");
      out.write("\t\t\tcreated.value = \"\";\n");
      out.write("\t\t\tDBId.value = \"\";\n");
      out.write("\t\t\tdeleteId.value = \"\";\n");
      out.write("\t\t\tshare_url.value = \"\";\n");
      out.write("\t\t\towner.value = \"\";\n");
      out.write("\t\t\ttyperow.className = \"show\";\n");
      out.write("\t\t\turl_row.className = \"hide\";\n");
      out.write("\t\t\towner.className = \"hide\";\n");
      out.write("\t\t\towner_text.className = \"hide\";\n");
      out.write("\t\t\tdeleteButton.className = \"hide\";\n");
      out.write("\t\t\tclearCommList();\n");
      out.write("\t\t\tbinary.value = \"null\";\n");
      out.write("\t\t\treturn;\n");
      out.write("\t\t}\n");
      out.write("\t\t\n");
      out.write("\t\t//_id, created, title, description\n");
      out.write("\t\tsplit = list.split(\"$$$\");\n");
      out.write("\t\t\n");
      out.write("\t\tres_id = split[0];\n");
      out.write("\t\tres_created = split[1];\n");
      out.write("\t\tres_title = split[2];\n");
      out.write("\t\tres_description = split[3];\n");
      out.write("\t\tres_url = split[4];\n");
      out.write("\t\tcommunities = split[5];\n");
      out.write("\t\tres_owner = split[6];\n");
      out.write("\t\tres_binary = split[7];\t\t\n");
      out.write("\t\tres_type = split[8];\t\t\t\n");
      out.write("\t\t\n");
      out.write("\t\tif ( res_binary == \"null\" )\n");
      out.write("\t\t{\n");
      out.write("\t\t\ttyperow.className = \"show\";\n");
      out.write("\t\t}\n");
      out.write("\t\telse\n");
      out.write("\t\t{\n");
      out.write("\t\t\ttyperow.className = \"hide\";\n");
      out.write("\t\t}\n");
      out.write("\t\ttitle.value = res_title;\n");
      out.write("\t\tdescription.value = res_description;\n");
      out.write("\t\tcreated.value = res_created;\n");
      out.write("\t\tDBId.value = res_id;\n");
      out.write("\t\tdeleteId.value = res_id;\n");
      out.write("\t\tshare_url.value = res_url;\n");
      out.write("\t\towner.value = res_owner;\t\t\n");
      out.write("\t\tdeleteButton.className = \"show\";\n");
      out.write("\t\towner.className = \"show\";\n");
      out.write("\t\towner_text.className = \"show\";\n");
      out.write("\t\turl_row.className = \"show\";\n");
      out.write("\t\thighlightComms(communities);\t\t\n");
      out.write("\t\tbinary.value = res_binary;\n");
      out.write("\t\ttype.value = res_type;\n");
      out.write("\t}\n");
      out.write("\t\tfunction validate_fields()\n");
      out.write("\t\t{\n");
      out.write("\t\t\ttitle = document.getElementById('title').value;\n");
      out.write("\t\t\tdescription = document.getElementById('description').value;\n");
      out.write("\t\t\tfile = document.getElementById('file').value;\n");
      out.write("\t\t\tbinary = document.getElementById(\"binary\").value;\n");
      out.write("\t\t\ttype = document.getElementById(\"type\").value;\n");
      out.write("\t\t\t//share_url = document.getElementById('share_url').value;\n");
      out.write("\t\t\t//file_url = document.getElementById('file_url').value;\n");
      out.write("\t\t\t//file_check = document.getElementById('file_check').checked;\n");
      out.write("\t\t\t\n");
      out.write("\t\t\tif (title == \"\")\n");
      out.write("\t\t\t{\n");
      out.write("\t\t\t\talert('Please provide a title.');\n");
      out.write("\t\t\t\treturn false;\n");
      out.write("\t\t\t}\n");
      out.write("\t\t\tif (description == \"\")\n");
      out.write("\t\t\t{\n");
      out.write("\t\t\t\talert('Please provide a description.');\n");
      out.write("\t\t\t\treturn false;\n");
      out.write("\t\t\t}\n");
      out.write("\t\t\tif ( binary == \"null\" && type == \"\")\n");
      out.write("\t\t\t{\n");
      out.write("\t\t\t\talert('Please provide a type.');\n");
      out.write("\t\t\t\treturn false;\n");
      out.write("\t\t\t}\n");
      out.write("\t\t\t\n");
      out.write("\t\t\t\n");
      out.write("\t\t}\n");
      out.write("\t\tfunction confirmDelete()\n");
      out.write("\t\t{\n");
      out.write("\t\t\tvar agree=confirm(\"Are you sure you wish to Delete this file from the File Share?\");\n");
      out.write("\t\t\tif (agree)\n");
      out.write("\t\t\t\treturn true ;\n");
      out.write("\t\t\telse\n");
      out.write("\t\t\t\treturn false ;\n");
      out.write("\t\t}\n");
      out.write("\t\tfunction showResults()\n");
      out.write("\t\t{\n");
      out.write("\t\t\tvar title = document.getElementById('DBId').value;\n");
      out.write("\t\t\tvar url = getEndPointUrl() + \"share/get/\" + title;\n");
      out.write("\t\t\twindow.open(url, '_blank');\n");
      out.write("\t\t\twindow.focus();\t\t\t\n");
      out.write("\t\t}\n");
      out.write("\t\t// -->\n");
      out.write("\t\t</script>\n");
      out.write("\t</script>\n");
      out.write("\t\t<div id=\"uploader_outter_div\" name=\"uploader_outter_div\" align=\"center\" style=\"width:100%\" >\n");
      out.write("\t    \t<div id=\"uploader_div\" name=\"uploader_div\" style=\"border-style:solid; border-color:#999999; border-radius: 10px; width:475px; margin:auto\">\n");
      out.write("\t        \t<h2>File Uploader</h2>\n");
      out.write("\t        \t<form id=\"search_form\" name=\"search_form\" method=\"get\">\n");
      out.write("\t        \t\t<div align=\"center\"\">\n");
      out.write("\t        \t\t<label for=\"ext\">Filter On</label>\n");
      out.write("\t\t\t\t\t  <select name=\"ext\" id=\"ext\" onchange=\"this.form.submit();\">\n");
      out.write("\t\t\t\t\t    ");

					    	out.print(populateMediaTypes(request, response));
					    
      out.write("\n");
      out.write("\t\t\t\t\t  </select>\n");
      out.write("\t\t\t\t\t </div>\n");
      out.write("\t\t\t\t\t ");

					 	if (showAll)
					 				out.print("<input type=\"hidden\" name=\"sudo\" id=\"sudo\" value=\"true\" />");
					 
      out.write("\t        \t\t\n");
      out.write("\t        \t</form>\n");
      out.write("\t        \t<form id=\"delete_form\" name=\"delete_form\" method=\"post\" enctype=\"multipart/form-data\" onsubmit=\"javascript:return confirmDelete()\" >\n");
      out.write("\t        \t\t<select id=\"upload_info\" onchange=\"populate()\" name=\"upload_info\"><option value=\"new\">Upload New File</option><option value=\"newJSON\">Upload New JSON</option> ");

 	out.print(populatePreviousUploads(request, response));
 
      out.write("</select>\n");
      out.write("\t        \t\t<input type=\"submit\" name=\"deleteButton\" id=\"deleteButton\" class=\"hidden\" value=\"Delete\" />\n");
      out.write("\t        \t\t<input type=\"hidden\" name=\"deleteId\" id=\"deleteId\" />\n");
      out.write("\t        \t\t<input type=\"hidden\" name=\"deleteFile\" id=\"deleteFile\" />\n");
      out.write("\t\t\t\t\t ");

					 	if (showAll)
					 				out.print("<input type=\"hidden\" name=\"sudo\" id=\"sudo\" value=\"true\" />");
					 
      out.write("\t        \t\t\n");
      out.write("\t        \t</form>\n");
      out.write("\t            <form id=\"upload_form\" name=\"upload_form\" method=\"post\" enctype=\"multipart/form-data\" onsubmit=\"javascript:return validate_fields();\" >\n");
      out.write("\t                <table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"padding-left:10px; padding-right:10px\">\n");
      out.write("\t                  <tr>\n");
      out.write("\t                    <td colspan=\"2\" align=\"center\"></td>\n");
      out.write("\t                  </tr>\n");
      out.write("\t                  <tr>\n");
      out.write("\t                    <td>Title:</td>\n");
      out.write("\t                    <td><input type=\"text\" name=\"title\" id=\"title\" size=\"39\" /></td>\n");
      out.write("\t                  </tr>\n");
      out.write("\t                  <tr>\n");
      out.write("\t                    <td>Description:</td>\n");
      out.write("\t                    <td><textarea rows=\"4\" cols=\"30\" name=\"description\" id=\"description\" ></textarea></td>\n");
      out.write("\t                  </tr>\n");
      out.write("\t                  <tr id=\"typerow\">\n");
      out.write("\t                    <td>Type:</td>\n");
      out.write("\t                    <td><input type=\"text\" name=\"type\" id=\"type\" size=\"39\" /></td>\n");
      out.write("\t                  </tr>\n");
      out.write("\t                  <tr>\n");
      out.write("\t                  \t<td>Communities:</td>\n");
      out.write("\t                  \t<td>");

	                  		out.print(communityList);
	                  	
      out.write("</td>\n");
      out.write("\t                  </tr>\n");
      out.write("\t                  <tr>\n");
      out.write("\t                  \t<td id=\"owner_text\">Owner:</td>\n");
      out.write("\t                  \t<td>\n");
      out.write("\t                    <input type=\"text\" name=\"owner\" id=\"owner\" readonly=\"readonly\" size=\"25\" />\n");
      out.write("\t                  \t</td>\n");
      out.write("\t                  </tr>\n");
      out.write("\t                  <tr>\n");
      out.write("\t                    <td>File:</td>\n");
      out.write("\t                    <td><input type=\"file\" name=\"file\" id=\"file\" /></td>\n");
      out.write("\t                  </tr>\n");
      out.write("\t                  <tr id=\"url_row\" class=\"hide\">\n");
      out.write("\t                  \t<td>Share URL:</td>\n");
      out.write("\t                  \t<td><input type=\"text\" name=\"share_url\" id=\"share_url\" readonly=\"readonly\" size=\"38\"/>\n");
      out.write("\t                  \t<input type=\"button\" onclick=\"showResults()\" value=\"View\"/>\n");
      out.write("\t                  \t</td>\n");
      out.write("\t                \t<td></td>\n");
      out.write("\t                  </tr>\n");
      out.write("\t                  <tr>\n");
      out.write("\t                    <td colspan=\"2\" style=\"text-align:right\"><input type=\"submit\" value=\"Submit\" /></td>\n");
      out.write("\t                  </tr>\n");
      out.write("\t                </table>\n");
      out.write("\t\t\t\t\t<input type=\"hidden\" name=\"created\" id=\"created\" />\n");
      out.write("\t\t\t\t\t<input type=\"hidden\" name=\"DBId\" id=\"DBId\" />\n");
      out.write("\t\t\t\t\t<input type=\"hidden\" name=\"fileUrl\" id=\"fileUrl\" />\n");
      out.write("\t\t\t\t\t<input type=\"hidden\" name=\"binary\" id=\"binary\" />\n");
      out.write("\t\t\t\t\t ");

					 	if (showAll)
					 				out.print("<input type=\"hidden\" name=\"sudo\" id=\"sudo\" value=\"true\" />");
					 
      out.write("\t        \t\t\n");
      out.write("\t\t\t\t</form>\n");
      out.write("\t        </div>\n");
      out.write("\t        <form id=\"logout_form\" name=\"logout_form\" method=\"post\">\n");
      out.write("\t        \t<input type=\"submit\" name=\"logout\" id = \"logout\" value=\"Log Out\" />\n");
      out.write("\t        </form>\n");
      out.write("\t    </div>\n");
      out.write("\t    </p>\n");
      out.write("\t\n");

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
	
      out.write("\n");
      out.write("\n");
      out.write("<script>\n");
      out.write("\tfunction validate_fields()\n");
      out.write("\t{\n");
      out.write("\t\tuname = document.getElementById('logintext').value;\n");
      out.write("\t\tpword = document.getElementById('passwordtext').value;\n");
      out.write("\t\t\n");
      out.write("\t\tif (uname == \"\")\n");
      out.write("\t\t{\n");
      out.write("\t\t\talert('Please provide your username.');\n");
      out.write("\t\t\treturn false;\n");
      out.write("\t\t}\n");
      out.write("\t\tif (pword == \"\")\n");
      out.write("\t\t{\n");
      out.write("\t\t\talert('Please provide your password.');\n");
      out.write("\t\t\treturn false;\n");
      out.write("\t\t}\n");
      out.write("\t}\n");
      out.write("\n");
      out.write("\n");
      out.write("</script>\n");
      out.write("\t<div id=\"login_outter_div\" name=\"login_outter_div\" align=\"center\" style=\"width:100%\" >\n");
      out.write("    \t<div id=\"login_div\" name=\"login_div\" style=\"border-style:solid; border-color:#999999; border-radius: 10px; width:450px; margin:auto\">\n");
      out.write("        \t<h2>Login</h2>\n");
      out.write("            <form id=\"login_form\" name=\"login_form\" method=\"post\" onsubmit=\"javascript:return validate_fields();\" >\n");
      out.write("                <table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"padding-left:10px\">\n");
      out.write("                  <tr>\n");
      out.write("                    <td>User Name</td>\n");
      out.write("                    <td>&nbsp;</td>\n");
      out.write("                    <td>Password</td>\n");
      out.write("                  </tr>\n");
      out.write("                  <tr>\n");
      out.write("                    <td><input type=\"text\" name=\"logintext\" id=\"logintext\" width=\"190px\" /></td>\n");
      out.write("                    <td>&nbsp;</td>\n");
      out.write("                    <td><input type=\"password\" name=\"passwordtext\" id=\"passwordtext\" width=\"190px\" /></td>\n");
      out.write("                  </tr>\n");
      out.write("                  <tr>\n");
      out.write("                    <td colspan=\"3\" align=\"right\"><input name=\"Login\" type=\"submit\" value=\"Login\" /></td>\n");
      out.write("                  </tr>\n");
      out.write("                </table>\n");
      out.write("\t\t\t</form>\n");
      out.write("        </div>\n");
      out.write("    </div>\n");
      out.write("\t<div style=\"color: red; text-align: center;\"> ");
      out.print(errorMsg);
      out.write(" </div>\n");

	}

      out.write("\n");
      out.write("    \n");
      out.write("    \n");
      out.write("</body>\n");
      out.write("</html>");
    } catch (Throwable t) {
      if (!(t instanceof SkipPageException)){
        out = _jspx_out;
        if (out != null && out.getBufferSize() != 0)
          try { out.clearBuffer(); } catch (java.io.IOException e) {}
        if (_jspx_page_context != null) _jspx_page_context.handlePageException(t);
      }
    } finally {
      _jspxFactory.releasePageContext(_jspx_page_context);
    }
  }
}
