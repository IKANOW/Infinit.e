package org.apache.jsp;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;
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

public final class jarUploader_jsp extends org.apache.jasper.runtime.HttpJspBase
    implements org.apache.jasper.runtime.JspSourceDependent {


	static String API_ROOT = null;
	static String SHARE_ROOT = "$infinite/share/get/";
	static String user = null;
	//static CookieManager cm = new CookieManager( null, CookiePolicy.ACCEPT_ALL);

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
	
	public static void setBrowserInfiniteCookie(HttpServletResponse response, String value)
	{
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
					System.out.println("Got Browser Cookie Line 109: " + cookie.getValue());
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
	            		System.out.println("Got Connection Cookie Line 133: " + value.substring(equalsLoc+1,semicolonLoc));
	            		return value.substring(equalsLoc+1,semicolonLoc);
	            	}
	            }
			}  
		}
    	return null;
	}
	
	public static String stringOfUrl(String addr, HttpServletRequest request, HttpServletResponse response)
	{
		//CookieHandler.setDefault(cm);
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
	
	public Boolean isLoggedIn(HttpServletRequest request, HttpServletResponse response)
	{
		System.out.println("isLoggedIn Called");
		String json = stringOfUrl(API_ROOT + "auth/keepalive", request, response);
		System.out.println(json);
		if (json != null)
		{
			keepAlive keepA = new Gson().fromJson(json, keepAlive.class);
			System.out.println("IsLoggedIn = " + keepA.response.success);
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
	
	
	
	private String AddToShare(byte[] bytes, String mimeType, String title, String description)
	{
		return UpdateToShare(bytes, mimeType, title, description, null);
	}
	//uploads a new widget's bytes and returns it's shareID if successful. If a share
	//ID is provided, then it updates the widget containing that shareID
	private String UpdateToShare(byte[] bytes, String mimeType, String title, String description, String prevId)
	{
		String charset = "UTF-8";
		String url = "";
		
		try{
			if (prevId == null)
				url = API_ROOT + "share/add/binary/" + URLEncoder.encode(title,charset) + "/" + URLEncoder.encode(description,charset) + "/";
			else
				url = API_ROOT + "share/update/binary/" + prevId + "/" + URLEncoder.encode(title,charset) + "/" + URLEncoder.encode(description,charset) + "/";
			
			//CookieHandler.setDefault(cm);
			URLConnection connection = new URL(url).openConnection();
			connection.setDoOutput(true);
	        connection.setRequestProperty("Accept-Charset",charset);
	        if (mimeType != null && mimeType.length() > 0)
	        	connection.setRequestProperty("Content-Type", mimeType + ";charset=" + charset);
	        DataOutputStream output = new DataOutputStream(connection.getOutputStream());
            output.write(bytes);
            DataInputStream response = new DataInputStream(connection.getInputStream());
            
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[16384];
            while ((nRead = response.read(data,0,data.length)) != -1)
            {
                buffer.write(data,0,nRead);
            }
            
            String json = buffer.toString();
            buffer.flush();
            buffer.close();
            output.close();
            response.close();
            
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
    			return "Upload Failed: " + mr.response.message;
    		}
		}catch(IOException e)
		{
			e.printStackTrace();
			return "Upload Failed: " + e.getMessage();
		}
	}

	private String populatePreviousUploads(HttpServletRequest request, HttpServletResponse response)
	{
		String toReturn = "";
		String delim = "$$$";
		String json = stringOfUrl(API_ROOT + "share/search/?type=binary", request, response);
		 if (json != null)
		{
			getShare gs = new Gson().fromJson(json, getShare.class);
			for ( getShare.shareData info : gs.data)
			{
				//_id, created, title, description
				if ( info.mediaType.equalsIgnoreCase("application/java-archive") || info.mediaType.equalsIgnoreCase("application/x-java-archive") || info.mediaType.equalsIgnoreCase("application/octet-stream"))
				{
					String value = info._id+delim+info.created+delim+info.title+delim+info.description;
					toReturn += "<option value=\""+value+"\" > <b>Edit:</b> " + info.title + "</option>";
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
      out.write("<title>Infinit.e Java Archive Upload Tool</title>\n");
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
	System.out.println("isLoggedIn == true");
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
 			out.println(removeFromShare(request.getAttribute("deleteId").toString(), request, response).toString());	
 		}
 		else
 		{
 		
	 	//////////////////////////////////////////////////////////////////////////////////
	 		
	 		Boolean newFile =(request.getAttribute("DBId").toString().length() == 0 );
			
	 		///////////////////////////////// File Manip  /////////////////////////////////
			String fileId = "";
			if(request.getAttribute("title") != null && request.getAttribute("description") != null && fileBytes != null)
			{
				if (newFile)
				{
					fileId = AddToShare(fileBytes, fileDS, request.getAttribute("title").toString(),request.getAttribute("description").toString());
				}
				else
				{
					fileId = UpdateToShare(fileBytes, fileDS, request.getAttribute("title").toString(),request.getAttribute("description").toString(), request.getAttribute("DBId").toString());
				}
				if (fileId.contains("Failed"))
				{
					out.println("File Upload Failed. Please Try again.</br> Error Msg =" + fileId);
				}
				else
				{
					if (fileId.length() == 0)
						out.println("File Successfully Updated. </br> File ID: " + request.getAttribute("DBId").toString());
					else
						out.println("File Successfully Uploaded. </br> File ID: " + fileId);
				}
			}
			else
			{
				out.println("Error: Not enough information provided for File Upload");
			}

	 		
			///////////////////////////////// End File Manip  /////////////////////////////////
			
			out.println("</div>");
 		}
	}
	else
	{
	}


      out.write("\n");
      out.write("\n");
      out.write("<script>\n");
      out.write("\tfunction populate()\n");
      out.write("\t{\n");
      out.write("\n");
      out.write("\t\ttitle = document.getElementById('title');\n");
      out.write("\t\tdescription = document.getElementById('description');\n");
      out.write("\t\tfile = document.getElementById('file');\n");
      out.write("\t\tcreated = document.getElementById('created');\n");
      out.write("\t\tDBId = document.getElementById('DBId');\n");
      out.write("\t\tdeleteId = document.getElementById('deleteId');\n");
      out.write("\t\tdeleteButton = document.getElementById('deleteButton');\n");
      out.write("\t\t\n");
      out.write("\t\tdropdown = document.getElementById(\"upload_info\");\n");
      out.write("\t\tlist = dropdown.options[dropdown.selectedIndex].value;\n");
      out.write("\t\t\n");
      out.write("\t\tif (list == \"new\")\n");
      out.write("\t\t{\n");
      out.write("\t\t\ttitle.value = \"\";\n");
      out.write("\t\t\tdescription.value = \"\";\n");
      out.write("\t\t\tcreated.value = \"\";\n");
      out.write("\t\t\tDBId.value = \"\";\n");
      out.write("\t\t\tdeleteId.value = \"\";\n");
      out.write("\t\t\tdeleteButton.style.visibility = 'hidden';\n");
      out.write("\t\t\treturn;\n");
      out.write("\t\t}\n");
      out.write("\t\t//_id, created, title, description\n");
      out.write("\t\tsplit = list.split(\"$$$\");\n");
      out.write("\t\t\n");
      out.write("\t\tres_id = split[0];\n");
      out.write("\t\tres_created = split[1];\n");
      out.write("\t\tres_title = split[2];\n");
      out.write("\t\tres_description = split[3];\n");
      out.write("\n");
      out.write("\t\t\n");
      out.write("\t\ttitle.value = res_title;\n");
      out.write("\t\tdescription.value = res_description;\n");
      out.write("\t\tcreated.value = res_created;\n");
      out.write("\t\tDBId.value = res_id;\n");
      out.write("\t\tdeleteId.value = res_id;\n");
      out.write("\t\tdeleteButton.style.visibility = '';\n");
      out.write("\t}\n");
      out.write("\tfunction validate_fields()\n");
      out.write("\t{\n");
      out.write("\t\ttitle = document.getElementById('title').value;\n");
      out.write("\t\tdescription = document.getElementById('description').value;\n");
      out.write("\t\tfile = document.getElementById('file').value;\n");
      out.write("\t\t\n");
      out.write("\t\tif (title == \"\")\n");
      out.write("\t\t{\n");
      out.write("\t\t\talert('Please provide a title.');\n");
      out.write("\t\t\treturn false;\n");
      out.write("\t\t}\n");
      out.write("\t\tif (description == \"\")\n");
      out.write("\t\t{\n");
      out.write("\t\t\talert('Please provide a description.');\n");
      out.write("\t\t\treturn false;\n");
      out.write("\t\t}\n");
      out.write("\t\tif (file == \"\")\n");
      out.write("\t\t{\n");
      out.write("\t\t\talert('Please provide a file.');\n");
      out.write("\t\t\treturn false;\n");
      out.write("\t\t}\n");
      out.write("\t\t\n");
      out.write("\t}\n");
      out.write("\tfunction confirmDelete()\n");
      out.write("\t{\n");
      out.write("\t\tvar agree=confirm(\"Are you sure you wish to Delete this File?\");\n");
      out.write("\t\tif (agree)\n");
      out.write("\t\t\treturn true ;\n");
      out.write("\t\telse\n");
      out.write("\t\t\treturn false ;\n");
      out.write("\t}\n");
      out.write("\t// -->\n");
      out.write("\t</script>\n");
      out.write("</script>\n");
      out.write("\t<div id=\"uploader_outter_div\" name=\"uploader_outter_div\" align=\"center\" style=\"width:100%\" >\n");
      out.write("    \t<div id=\"uploader_div\" name=\"uploader_div\" style=\"border-style:solid; border-color:#999999; border-radius: 10px; width:450px; margin:auto\">\n");
      out.write("        \t<h2>Java Archive Uploader</h2>\n");
      out.write("        \t<form id=\"delete_form\" name=\"delete_form\" method=\"post\" enctype=\"multipart/form-data\" onsubmit=\"javascript:return confirmDelete()\" >\n");
      out.write("        \t\t<select id=\"upload_info\" onchange=\"populate()\" name=\"upload_info\"><option value=\"new\">Upload New JAR</option> ");
 out.print(populatePreviousUploads(request, response)); 
      out.write("</select>\n");
      out.write("        \t\t<input type=\"submit\" name=\"deleteButton\" id=\"deleteButton\" style=\"visibility:hidden;\" value=\"Delete\" />\n");
      out.write("        \t\t<input type=\"hidden\" name=\"deleteId\" id=\"deleteId\" />\n");
      out.write("        \t</form>\n");
      out.write("            <form id=\"upload_form\" name=\"upload_form\" method=\"post\" enctype=\"multipart/form-data\" onsubmit=\"javascript:return validate_fields();\" >\n");
      out.write("                <table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"padding-left:10px; padding-right:10px\">\n");
      out.write("                  <tr>\n");
      out.write("                    <td colspan=\"2\" align=\"center\"></td>\n");
      out.write("                  </tr>\n");
      out.write("                  <tr>\n");
      out.write("                    <td>Title:</td>\n");
      out.write("                    <td><input type=\"text\" name=\"title\" id=\"title\" size=\"35\" /></td>\n");
      out.write("                  </tr>\n");
      out.write("                  <tr>\n");
      out.write("                    <td>Description:</td>\n");
      out.write("                    <td><textarea rows=\"4\" cols=\"30\" name=\"description\" id=\"description\" ></textarea></td>\n");
      out.write("                  </tr>\n");
      out.write("                  <tr>\n");
      out.write("                    <td>File:</td>\n");
      out.write("                    <td><input type=\"file\" name=\"file\" id=\"file\" /></td>\n");
      out.write("                  </tr>\n");
      out.write("                  <tr>\n");
      out.write("                    <td colspan=\"2\" style=\"text-align:right\"><input type=\"submit\" value=\"Submit\" /></td>\n");
      out.write("                  </tr>\n");
      out.write("                </table>\n");
      out.write("\t\t\t\t<input type=\"hidden\" name=\"created\" id=\"created\" />\n");
      out.write("\t\t\t\t<input type=\"hidden\" name=\"DBId\" id=\"DBId\" />\n");
      out.write("\t\t\t</form>\n");
      out.write("        </div>\n");
      out.write("    </div>\n");
      out.write("    </p>\n");
	
}
else if (isLoggedIn == false)
{
	String errorMsg = "";
	if (request.getParameter("logintext") != null || request.getParameter("passwordtext") != null)
	{
		if(logMeIn(request.getParameter("logintext"),request.getParameter("passwordtext"), request, response))
		{
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
