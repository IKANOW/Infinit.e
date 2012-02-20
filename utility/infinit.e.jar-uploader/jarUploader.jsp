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
	static String user = null;
	//static CookieManager cm = new CookieManager();

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

        	String cookieVal = getBrowserInfiniteCookie(request).replace("this", "that");
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
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
<title>Infinit.e Java Archive Upload Tool</title>
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
	
}
Boolean isLoggedIn = isLoggedIn(request, response);
if (isLoggedIn == null)
{
	out.println("The Infinit.e API cannot be reached:");
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
		
		dropdown = document.getElementById("upload_info");
		list = dropdown.options[dropdown.selectedIndex].value;
		
		if (list == "new")
		{
			title.value = "";
			description.value = "";
			created.value = "";
			DBId.value = "";
			deleteId.value = "";
			deleteButton.style.visibility = 'hidden';
			return;
		}
		//_id, created, title, description
		split = list.split("$$$");
		
		res_id = split[0];
		res_created = split[1];
		res_title = split[2];
		res_description = split[3];

		
		title.value = res_title;
		description.value = res_description;
		created.value = res_created;
		DBId.value = res_id;
		deleteId.value = res_id;
		deleteButton.style.visibility = '';
	}
	function validate_fields()
	{
		title = document.getElementById('title').value;
		description = document.getElementById('description').value;
		file = document.getElementById('file').value;
		
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
		if (file == "")
		{
			alert('Please provide a file.');
			return false;
		}
		
	}
	function confirmDelete()
	{
		var agree=confirm("Are you sure you wish to Delete this File?");
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
        	<h2>Java Archive Uploader</h2>
        	<form id="delete_form" name="delete_form" method="post" enctype="multipart/form-data" onsubmit="javascript:return confirmDelete()" >
        		<select id="upload_info" onchange="populate()" name="upload_info"><option value="new">Upload New JAR</option> <% out.print(populatePreviousUploads(request, response)); %></select>
        		<input type="submit" name="deleteButton" id="deleteButton" style="visibility:hidden;" value="Delete" />
        		<input type="hidden" name="deleteId" id="deleteId" />
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
                    <td>File:</td>
                    <td><input type="file" name="file" id="file" /></td>
                  </tr>
                  <tr>
                    <td colspan="2" style="text-align:right"><input type="submit" value="Submit" /></td>
                  </tr>
                </table>
				<input type="hidden" name="created" id="created" />
				<input type="hidden" name="DBId" id="DBId" />
			</form>
        </div>
    </div>
    </p>
<%	
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