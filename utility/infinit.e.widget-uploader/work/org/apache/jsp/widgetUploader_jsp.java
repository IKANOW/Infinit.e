package org.apache.jsp;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;
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
import java.security.*;;

public final class widgetUploader_jsp extends org.apache.jasper.runtime.HttpJspBase
    implements org.apache.jasper.runtime.JspSourceDependent {


	static String API_ROOT = null;
	static String SHARE_ROOT = "$infinite/share/get/";
	static Boolean showAll = false;
	static Boolean debugMode = false;
	//static Boolean localCookie = false;
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
	public static void setBrowserInfiniteCookie(HttpServletResponse response, String value)
	{
		if(debugMode)
			System.out.println("Set Browser Cookie to " + value);
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
        		//urlConnection.setRequestProperty("Content-Type", "text/plain;charset=UTF-8");
        	}
        	else if (debugMode)
        		System.out.println("Don't Current Have a Cookie Value");
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
		//System.out.println("isLoggedIn Called");
		String json = stringOfUrl(API_ROOT + "auth/keepalive", request, response);
		//System.out.println(json);
		if (json != null)
		{
			keepAlive keepA = new Gson().fromJson(json, keepAlive.class);
			//System.out.println("IsLoggedIn = " + keepA.response.success);
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
		if(debugMode)
			System.out.println("Removing share from community:");
		try{
			String charset = "UTF-8";

			String json = stringOfUrl(API_ROOT + "social/share/remove/community/" + URLEncoder.encode(shareId,charset) + "/" + URLEncoder.encode(communityId,charset) + "/" , request, response);
			keepAlive ka = new Gson().fromJson(json, keepAlive.class);
			if (ka != null)
			{
				if(debugMode)
					System.out.println("Share: " + shareId + "ComunityId: " + communityId + "Removed:" + ka.response.success + " Message: " + ka.response.message);
				return ka.response.success;
			}
			else
			{
				if(debugMode)
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
		if(debugMode)
			System.out.println("Adding share to community:");
		try{
			String charset = "UTF-8";
			
			String comment = "Added by widgetUploader";
			
			///share/add/community/{shareid}/{comment}/{communityid}
			String json = stringOfUrl(API_ROOT + "share/add/community/" + URLEncoder.encode(shareId,charset) + "/" + URLEncoder.encode(comment,charset) + "/" + URLEncoder.encode(communityId,charset) + "/", request, response);
			getModules gm = new Gson().fromJson(json, getModules.class);
			if (gm == null)
			{
				if(debugMode)
					System.out.println("The JSON for Adding a share to community was null");
				return false;
			}
				//return "Json was null: " + json + "\n " + API_ROOT + "share/add/community/" + URLEncoder.encode(shareId,charset) + "/" + URLEncoder.encode(communityId,charset) + "/" + URLEncoder.encode(comment,charset) + "/";
			if(debugMode)
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
						String value = info._id+delim+info.url+delim+info.title+delim+info.description+delim+info.created+delim+info.modified+delim+info.version+delim+info.imageurl+delim+info.communityIds;
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
		//if(localCookie)
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
        		setBrowserInfiniteCookie(response, newCookie);
        	}
	        
	        widgetToDBResponse resp = new Gson().fromJson(buffer.toString(), widgetToDBResponse.class);
	        return resp.response.success;
        }catch(IOException e){
        	return false;
        }
        
        
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
      out.write("<title>Infinit.e Widget Upload Tool</title>\n");
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
      out.write("\n");
      out.write("</style>\n");
      out.write("<script language=\"javascript\" src=\"AppConstants.js\"> </script>\n");
      out.write("</head>\n");
      out.write("\n");
      out.write("<body>\n");

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
	
	
      out.write("\n");
      out.write("\t\n");
      out.write("\t<script>\n");
      out.write("\t\tfunction clearCommList()\n");
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
      out.write("\t\tfunction populate()\n");
      out.write("\t\t{\n");
      out.write("\t\n");
      out.write("\t\t\ttitle = document.getElementById('title');\n");
      out.write("\t\t\tdescription = document.getElementById('description');\n");
      out.write("\t\t\tfile = document.getElementById('file');\n");
      out.write("\t\t\ticon = document.getElementById('icon');\n");
      out.write("\t\t\tversion = document.getElementById('version');\n");
      out.write("\t\t\tcreated = document.getElementById('created');\n");
      out.write("\t\t\tDBId = document.getElementById('DBId');\n");
      out.write("\t\t\tdeleteId = document.getElementById('deleteId');\n");
      out.write("\t\t\tdeleteFile = document.getElementById('deleteFile');\n");
      out.write("\t\t\tdeleteIcon = document.getElementById('deleteIcon');\n");
      out.write("\t\t\timageUrl = document.getElementById('imageUrl');\n");
      out.write("\t\t\tswfUrl = document.getElementById('swfUrl');\n");
      out.write("\t\t\ticon_check = document.getElementById('icon_check');\n");
      out.write("\t\t\tfile_check = document.getElementById('file_check');\n");
      out.write("\t\t\tfile_url = document.getElementById('file_url');\n");
      out.write("\t\t\ticon_url = document.getElementById('icon_url');\n");
      out.write("\t\t\tdeleteButton = document.getElementById('deleteButton');\n");
      out.write("\t\t\t\n");
      out.write("\t\t\t\n");
      out.write("\t\t\tdropdown = document.getElementById(\"upload_info\");\n");
      out.write("\t\t\tlist = dropdown.options[dropdown.selectedIndex].value;\n");
      out.write("\t\t\t\n");
      out.write("\t\t\tif (list == \"new\")\n");
      out.write("\t\t\t{\n");
      out.write("\t\t\t\ttitle.value = \"\";\n");
      out.write("\t\t\t\tdescription.value = \"\";\n");
      out.write("\t\t\t\tversion.value = \"\";\n");
      out.write("\t\t\t\tcreated.value = \"\";\n");
      out.write("\t\t\t\tDBId.value = \"\";\n");
      out.write("\t\t\t\tdeleteId.value = \"\";\n");
      out.write("\t\t\t\tdeleteFile.value = \"\";\n");
      out.write("\t\t\t\tdeleteIcon.value = \"\";\n");
      out.write("\t\t\t\timageUrl.value = \"\";\n");
      out.write("\t\t\t\tswfUrl.value = \"\";\n");
      out.write("\t\t\t\tfile_url.value = \"\";\n");
      out.write("\t\t\t\ticon_url.value = \"\";\n");
      out.write("\t\t\t\ticon_check.checked = false;\n");
      out.write("\t\t\t\tfile_check.checked = false;\n");
      out.write("\t\t\t\tdeleteButton.style.visibility = 'hidden';\n");
      out.write("\t\t\t\tuseUrlSwf();\n");
      out.write("\t\t\t\tuseUrlIcon();\n");
      out.write("\t\t\t\tclearCommList();\n");
      out.write("\t\t\t\treturn;\n");
      out.write("\t\t\t}\n");
      out.write("\t\t\t//_id,url,title,description,created,modified,version,imageurl,communities\n");
      out.write("\t\t\tsplit = list.split(\"$$$\");\n");
      out.write("\t\t\t\n");
      out.write("\t\t\tres_id = split[0];\n");
      out.write("\t\t\tresUrl = split[1];\n");
      out.write("\t\t\tresTitle = split[2];\n");
      out.write("\t\t\tresDescription = split[3];\n");
      out.write("\t\t\tresCreated = split[4];\n");
      out.write("\t\t\tresModified = split[5];\n");
      out.write("\t\t\tresVersion = split[6];\n");
      out.write("\t\t\tresImageurl = split[7];\n");
      out.write("\t\t\tcommunities = split[8];\n");
      out.write("\t\n");
      out.write("\t\t\ticon_check.checked = true;\n");
      out.write("\t\t\tfile_check.checked = true;\n");
      out.write("\t\t\ttitle.value = resTitle;\n");
      out.write("\t\t\tdescription.value = resDescription;\n");
      out.write("\t\t\tversion.value = resVersion;\n");
      out.write("\t\t\tcreated.value = resCreated;\n");
      out.write("\t\t\tDBId.value = res_id;\n");
      out.write("\t\t\tdeleteId.value = res_id;\n");
      out.write("\t\t\tdeleteFile.value = resUrl;\n");
      out.write("\t\t\tdeleteIcon.value = resImageurl;\n");
      out.write("\t\t\timageUrl.value = resImageurl;\n");
      out.write("\t\t\tswfUrl.value = resUrl;\n");
      out.write("\t\t\tfile_url.value = resUrl;\n");
      out.write("\t\t\ticon_url.value = resImageurl;\n");
      out.write("\t\t\tuseUrlSwf();\n");
      out.write("\t\t\tuseUrlIcon();\n");
      out.write("\t\t\tdeleteButton.style.visibility = '';\n");
      out.write("\t\t\thighlightComms(communities);\n");
      out.write("\t\t}\n");
      out.write("\t\tfunction useUrlSwf()\n");
      out.write("\t\t{\n");
      out.write("\t\t\tfile = document.getElementById('file');\n");
      out.write("\t\t\tfile_url = document.getElementById('file_url');\n");
      out.write("\t\t\t\n");
      out.write("\t\t\tif (document.getElementById('file_check').checked)\n");
      out.write("\t\t\t{\n");
      out.write("\t\t\t\tfile_url.style.display = \"\";\n");
      out.write("\t\t\t\tfile.style.display = \"none\";\n");
      out.write("\t\t\t}\n");
      out.write("\t\t\telse\n");
      out.write("\t\t\t{\n");
      out.write("\t\t\t\tfile.style.display = \"\";\n");
      out.write("\t\t\t\tfile_url.style.display = \"none\";\n");
      out.write("\t\t\t}\n");
      out.write("\t\t}\n");
      out.write("\t\tfunction useUrlIcon()\n");
      out.write("\t\t{\n");
      out.write("\t\t\ticon = document.getElementById('icon');\n");
      out.write("\t\t\ticon_url = document.getElementById('icon_url');\n");
      out.write("\t\t\t\n");
      out.write("\t\t\tif (document.getElementById('icon_check').checked)\n");
      out.write("\t\t\t{\n");
      out.write("\t\t\t\ticon_url.style.display = \"\";\n");
      out.write("\t\t\t\ticon.style.display = \"none\";\n");
      out.write("\t\t\t}\n");
      out.write("\t\t\telse\n");
      out.write("\t\t\t{\n");
      out.write("\t\t\t\ticon.style.display = \"\";\n");
      out.write("\t\t\t\ticon_url.style.display = \"none\";\n");
      out.write("\t\t\t}\n");
      out.write("\t\t}\n");
      out.write("\t\tfunction validate_fields()\n");
      out.write("\t\t{\n");
      out.write("\t\t\ttitle = document.getElementById('title').value;\n");
      out.write("\t\t\tdescription = document.getElementById('description').value;\n");
      out.write("\t\t\tfile = document.getElementById('file').value;\n");
      out.write("\t\t\ticon = document.getElementById('icon').value;\n");
      out.write("\t\t\tversion = document.getElementById('version').value;\n");
      out.write("\t\t\ticon_checked = document.getElementById('icon_check').checked;\n");
      out.write("\t\t\tfile_checked = document.getElementById('file_check').checked;\n");
      out.write("\t\t\tfile_url = document.getElementById('file_url').value;\n");
      out.write("\t\t\ticon_url = document.getElementById('icon_url').value;\n");
      out.write("\n");
      out.write("\t\t\tvar select = document.getElementById( 'communities' );\n");
      out.write("\t\t\tnumSelected = 0;\n");
      out.write("\t\t\t\n");
      out.write("\t\t\tfor ( var i = 0, l = select.options.length, o; i < l; i++ )\n");
      out.write("\t\t\t{\n");
      out.write("\t\t\t  o = select.options[i];\n");
      out.write("\t\t\t  if ( o.selected == true )\n");
      out.write("\t\t\t  {\n");
      out.write("\t\t\t    numSelected++;\n");
      out.write("\t\t\t  }\n");
      out.write("\t\t\t}\n");
      out.write("\n");
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
      out.write("\t\t\tif (file == \"\" && file_checked == false)\n");
      out.write("\t\t\t{\n");
      out.write("\t\t\t\talert('Please provide your file.');\n");
      out.write("\t\t\t\treturn false;\n");
      out.write("\t\t\t}\n");
      out.write("\t\t\tif (file_url == \"\" && file_checked == true)\n");
      out.write("\t\t\t{\n");
      out.write("\t\t\t\talert('Please provide the Url to your Swf file.');\n");
      out.write("\t\t\t\treturn false;\n");
      out.write("\t\t\t}\n");
      out.write("\t\t\tif (version == \"\")\n");
      out.write("\t\t\t{\n");
      out.write("\t\t\t\talert('Please provide a Version Number.');\n");
      out.write("\t\t\t\treturn false;\n");
      out.write("\t\t\t}\n");
      out.write("\t\t\tif (icon == \"\" && icon_checked == false)\n");
      out.write("\t\t\t{\n");
      out.write("\t\t\t\talert('Please provide an icon image for your widget.');\n");
      out.write("\t\t\t\treturn false;\n");
      out.write("\t\t\t}\n");
      out.write("\t\t\tif (icon_url == \"\" && icon_checked == true)\n");
      out.write("\t\t\t{\n");
      out.write("\t\t\t\talert('Please provide a url to your icon image.');\n");
      out.write("\t\t\t\treturn false;\n");
      out.write("\t\t\t}\n");
      out.write("\t\t\tif (numSelected == 0)\n");
      out.write("\t\t\t{\n");
      out.write("\t\t\t\talert('Please select at least 1 community');\n");
      out.write("\t\t\t\treturn false;\n");
      out.write("\t\t\t}\n");
      out.write("\t\t\t\n");
      out.write("\t\t}\n");
      out.write("\t\tfunction confirmDelete()\n");
      out.write("\t\t{\n");
      out.write("\t\t\tvar agree=confirm(\"Are you sure you wish to Delete this Widget?\");\n");
      out.write("\t\t\tif (agree)\n");
      out.write("\t\t\t\treturn true ;\n");
      out.write("\t\t\telse\n");
      out.write("\t\t\t\treturn false ;\n");
      out.write("\t\t}\n");
      out.write("\t\t// -->\n");
      out.write("\t\t</script>\n");
      out.write("\t</script>\n");
      out.write("\t\t<div id=\"uploader_outter_div\" name=\"uploader_outter_div\" align=\"center\" style=\"width:100%\" >\n");
      out.write("\t    \t<div id=\"uploader_div\" name=\"uploader_div\" style=\"border-style:solid; border-color:#999999; border-radius: 10px; width:450px; margin:auto\">\n");
      out.write("\t        \t<h2>Widget Uploader</h2>\n");
      out.write("\t        \t<form id=\"delete_form\" name=\"delete_form\" method=\"post\" enctype=\"multipart/form-data\" onsubmit=\"javascript:return confirmDelete()\" >\n");
      out.write("\t        \t\t<select id=\"upload_info\" onchange=\"populate()\" name=\"upload_info\"><option value=\"new\">Upload New Widget</option> ");
 out.print(populatePreviousUploads(request, response)); 
      out.write("</select>\n");
      out.write("\t        \t\t<input type=\"submit\" name=\"deleteButton\" id=\"deleteButton\" style=\"visibility:hidden;\" value=\"Delete\" />\n");
      out.write("\t        \t\t<input type=\"hidden\" name=\"deleteId\" id=\"deleteId\" />\n");
      out.write("\t        \t\t<input type=\"hidden\" name=\"deleteFile\" id=\"deleteFile\" />\n");
      out.write("\t        \t\t<input type=\"hidden\" name=\"deleteIcon\" id=\"deleteIcon\" />\n");
      out.write("\t        \t</form>\n");
      out.write("\t            <form id=\"upload_form\" name=\"upload_form\" method=\"post\" enctype=\"multipart/form-data\" onsubmit=\"javascript:return validate_fields();\" >\n");
      out.write("\t                <table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"padding-left:10px; padding-right:10px\">\n");
      out.write("\t                  <tr>\n");
      out.write("\t                    <td colspan=\"2\" align=\"center\"></td>\n");
      out.write("\t                  </tr>\n");
      out.write("\t                  <tr>\n");
      out.write("\t                    <td>Title:</td>\n");
      out.write("\t                    <td><input type=\"text\" name=\"title\" id=\"title\" size=\"35\" /></td>\n");
      out.write("\t                  </tr>\n");
      out.write("\t                  <tr>\n");
      out.write("\t                    <td>Description:</td>\n");
      out.write("\t                    <td><textarea rows=\"4\" cols=\"30\" name=\"description\" id=\"description\" ></textarea></td>\n");
      out.write("\t                  </tr>\n");
      out.write("\t                  <tr>\n");
      out.write("\t                  \t<td>Communities:</td>\n");
      out.write("\t                  \t<td>");
 out.print(generateCommunityList(request, response)); 
      out.write("</td>\n");
      out.write("\t                  </tr>\n");
      out.write("\t                  <tr>\n");
      out.write("\t                    <td>Swf File:</td>\n");
      out.write("\t                    <td><input type=\"file\" name=\"file\" id=\"file\" /><input type=\"text\" name=\"file_url\" id=\"file_url\" size=\"32\" style=\"display:none;\" /><input type=\"checkbox\" id=\"file_check\" name=\"file_check\" onchange=\"useUrlSwf()\" /> <span id=\"file_provide\" name=\"file_provide\"> Provide Url </span></td>\n");
      out.write("\t                  </tr>\n");
      out.write("\t                  <tr>\n");
      out.write("\t                    <td>Version Number:</td>\n");
      out.write("\t                    <td><input type=\"text\" name=\"version\" id=\"version\" /></td>\n");
      out.write("\t                  </tr>\n");
      out.write("\t                  <tr id=\"iconRow\">\n");
      out.write("\t                    <td>Icon Image:</td>\n");
      out.write("\t                    <td><input type=\"file\" name=\"icon\" id=\"icon\" /><input type=\"text\" name=\"icon_url\" id=\"icon_url\" size=\"32\" style=\"display:none;\" /><input type=\"checkbox\" id=\"icon_check\" name=\"icon_check\" onchange=\"useUrlIcon()\" /><span id=\"file_provide\" name=\"file_provide\"> Provide Url</span></td>\n");
      out.write("\t                  </tr>\n");
      out.write("\t                  <tr>\n");
      out.write("\t                    <td colspan=\"2\" style=\"text-align:right\"><input type=\"submit\" value=\"Submit\" /></td>\n");
      out.write("\t                  </tr>\n");
      out.write("\t                </table>\n");
      out.write("\t\t\t\t\t<input type=\"hidden\" name=\"created\" id=\"created\" />\n");
      out.write("\t\t\t\t\t<input type=\"hidden\" name=\"DBId\" id=\"DBId\" />\n");
      out.write("\t\t\t\t\t<input type=\"hidden\" name=\"imageUrl\" id=\"imageUrl\" />\n");
      out.write("\t\t\t\t\t<input type=\"hidden\" name=\"swfUrl\" id=\"swfUrl\" />\n");
      out.write("\t\t\t\t</form>\n");
      out.write("\t        </div>\n");
      out.write("\t        <form id=\"logout_form\" name=\"logout_form\" method=\"post\">\n");
      out.write("\t        \t<input type=\"submit\" name=\"logout\" id = \"logout\" value=\"Log Out\" />\n");
      out.write("\t        </form>\n");
      out.write("\t    </div>\n");
      out.write("\t    </p>\n");
      out.write("\t\n");

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
			debugMode = (request.getParameter("debug") != null);
			out.println("<meta http-equiv=\"refresh\" content=\"0\">");
			out.println("Login Success");
		}
		else
		{
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
      out.print(errorMsg );
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
