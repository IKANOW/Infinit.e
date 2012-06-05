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
<%@page import="java.text.SimpleDateFormat"%>
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
	static Boolean DEBUG_MODE = false;
	static Boolean showAll = false;
	static Boolean localCookie = false;
	static String user = null;
	static CookieManager cm = new CookieManager();


	static class wpuser
	{
		String created;
		String modified;
		String firstname;
		String lastname;
		String phone;
		String[] email;
	}
	
	static class wpauth
	{
		String username;
		String password;
		String accountType;
		String created;
		String modified;
	}
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
	
	static class getPerson
	{
		static class response
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
		public response response;
		public data data;
	}
	
	static class getSystem
	{
		static class response
		{
			String action;
			Boolean success;
			String message;
			int time;
		}
		static class typeVal
		{
			String type;
			String value;
		}
		static class communityAttributes
		{
			static class usersCanCreateSubCommunities
			{
				String type;
				String value;
			}
			static class registrationRequiresApproval
			{
				String type;
				String value;
			}
			static class isPublic
			{
				String type;
				String value;
			}
			static class usersCanSelfRegister
			{
				String type;
				String value;
			}
			public usersCanCreateSubCommunities usersCanCreateSubCommunities;
			public registrationRequiresApproval registrationRequiresApproval;
			public isPublic isPublic;
			public usersCanSelfRegister usersCanSelfRegister;
		}
		static class userAttributes
		{
			static class publicCommentsPublicly
			{
				String type;
				String defaultValue;
				Boolean allowOverride;
			}
			static class publishQueriesToActivityFeed
			{
				String type;
				String defaultValue;
				Boolean allowOverride;
			}
			static class publishLoginToActivityFeed
			{
				String type;
				String defaultValue;
				Boolean allowOverride;
			}
			static class publishSharingToActivityFeed
			{
				String type;
				String defaultValue;
				Boolean allowOverride;
			}
			static class publishCommentsToActivityFeed
			{
				String type;
				String defaultValue;
				Boolean allowOverride;
			}
			
			public publicCommentsPublicly publicCommentsPublicly;
			public publishQueriesToActivityFeed publishQueriesToActivityFeed;
			public publishLoginToActivityFeed publishLoginToActivityFeed;
			public publishSharingToActivityFeed publishSharingToActivityFeed;
			public publishCommentsToActivityFeed publishCommentsToActivityFeed;
		}
		static class member
		{
			static class userAttribute
			{
				String type;
				String value;
			}
		
			String _id;
			String email;
			String displayName;
			String userType;
			String userStatus;
			userAttribute[] userAttributes;
		}
		static class data
		{
			String _id;
			String name;
			String description;
			Boolean isSystemCommunity;
			Boolean isPersonalCommunity;
			String[] tags;
			communityAttributes communityAttributes;
			userAttributes userAttributes;
			String ownerId;
			String communityStatus;
			String ownerDisplayName;
			int numberOfMembers;
			member[] members;
		}
		public response response;
		public data data;
	}
	
	static class personRegister
	{
		static class response
		{
			String action;
			Boolean success;
			String message;
			int time;
		}
		
		static class data
		{
			String _id;
			String profileId;
			String username;
			String password;
			String accountType;
			String accountStatus;
			String created;
			String modified;			
		}
		public response response;
		public data data;
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
	
	static class registerResponse
	{
		static class resp
		{
			String action;
			Boolean success;
			String message;
			int time;
		}
		
		static class data
		{
			String _id;
			String profileId;
			String username;
			String password;
			String acountType;
			String created;
			String modified;
		}
		
		public resp response;
		public data data;
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
        		urlConnection.addRequestProperty("Cookie","infinitecookie=" + cookieVal);
        		urlConnection.setDoInput(true);
        		urlConnection.setDoOutput(true);
        		urlConnection.setRequestProperty("Accept-Charset","UTF-8");
        	}
        	else if (DEBUG_MODE)
        		System.out.println("Infinit.e Cookie Value is Null");
        	IOUtils.copy(urlConnection.getInputStream(), output);
        	String newCookie = getConnectionInfiniteCookie(urlConnection);
        	if (newCookie != null && response != null)
        	{
        		setBrowserInfiniteCookie(response, newCookie);
        	}
			
        	
        	String toReturn = output.toString();
        	output.close();
        	return toReturn;
        }
        catch(IOException e)
        {
        	return null;
        }
    }
    
    // postToRestfulApi - 

// Note: params in the addr field need to be URLEncoded

	private static String postToRestfulApi(String addr, String data, HttpServletRequest request, HttpServletResponse response) 
	{
		
    	if(localCookie)
			CookieHandler.setDefault(cm);
	
		String result = "";
	
	    try
		{
	
	    	URLConnection connection = new URL(addr).openConnection();
	    	
	    	String cookieVal = getBrowserInfiniteCookie(request);
        	if (cookieVal != null)
        	{
        		connection.addRequestProperty("Cookie","infinitecookie=" + cookieVal);
        		connection.setDoInput(true);
        	}
        	else if (DEBUG_MODE)
        		System.out.println("NULLLLLLLLL");
	
	    	connection.setDoOutput(true);
	
			connection.setRequestProperty("Accept-Charset", "UTF-8");
	
			// Post JSON string to URL
	
			OutputStream os = connection.getOutputStream();
	
			byte[] b = data.getBytes("UTF-8");
	
			os.write(b);
	
			// Receive results back from API
	
			InputStream is = connection.getInputStream();
	
			result = IOUtils.toString(is, "UTF-8");
			
			//System.out.println("The result from POST Call: '" + result + "'");
			
			String newCookie = getConnectionInfiniteCookie(connection);
        	if (newCookie != null && response != null)
        	{
        		setBrowserInfiniteCookie(response, newCookie);
        	}
	
		}
	
		catch (Exception e)
		{
			if (DEBUG_MODE)
				System.out.println("Exception: " + e.getMessage());
		}
			return result;
	}
	
	private static String encrypt(String password) throws NoSuchAlgorithmException, UnsupportedEncodingException 
	{	
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		md.update(password.getBytes("UTF-8"));			
		return URLEncoder.encode((new BASE64Encoder()).encode(md.digest()), "UTF-8");	
	}
	private static String encryptWithoutEncode(String password) throws NoSuchAlgorithmException, UnsupportedEncodingException 
	{	
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		md.update(password.getBytes("UTF-8"));			
		return (new BASE64Encoder()).encode(md.digest());	
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
	
	public String generateCommunityList(HttpServletRequest request, HttpServletResponse response)
	{
		String toReturn = "<select multiple=\"multiple\" name=\"communities\" id=\"communities\">";
		String json = stringOfUrl(API_ROOT + "person/get/", request, response);
		String system_id = getSystemCommunityId(request, response);
		if (json !=null)
		{
			personGet pg = new Gson().fromJson(json, personGet.class);
			for(personGet.community comm : pg.data.communities)
			{
				if (comm._id.equals(system_id))
					comm.name = comm.name + " (System Community) ";
				if (!comm.name.contains("Personal"))
					toReturn += "<option value=\""+ comm._id + "\">"+ comm.name +"</option>";
			}	
		}
			 // <option selected value="volvo">Volvo</option>
		
		return toReturn + "</select>";
	}
	
	public String createUser(HttpServletRequest request, HttpServletResponse response,String first_name, String last_name, String phone, String email, String password, String[] communities) throws IOException, NoSuchAlgorithmException, UnsupportedEncodingException, URISyntaxException 
	{
		String charset = "UTF-8";
		Date date = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat("MMM dd, yyyy kk:mm:ss aa");
		String today = formatter.format(date);
		String encrypted_password = encryptWithoutEncode(password);
		//String encrypted_encoded_password = encrypt(password);
		
		wpuser wpuser = new wpuser();
		wpauth wpauth = new wpauth();
		
		wpuser.created = today;
		wpuser.modified = today;
		wpuser.firstname = first_name;
		wpuser.lastname = last_name;
		wpuser.phone = phone;
		wpuser.email = new String[1];
		wpuser.email[0] = email;
		
		wpauth.username = email;
		wpauth.password = encrypted_password;
		wpauth.accountType = "user";
		wpauth.created = today;
		wpauth.modified = today;
		
		String theUrl = API_ROOT + "social/person/register";
		String data = "{ \"user\":" + new Gson().toJson(wpuser) + ", \"auth\":" + new Gson().toJson(wpauth) + "}";

		String json = postToRestfulApi(theUrl, data, request, response);

		if (json !=null)
		{
			personGet pg = new Gson().fromJson(json, personGet.class);
			if (pg.response.success == true && communities != null)
			{
				for(int loopIndex = 0; loopIndex < communities.length; loopIndex++)
		        {
		            //out.println(communities[loopIndex] + "<BR>");
		            addUserToCommunity(request, response, communities[loopIndex], pg.data._id);
		            //add to community code goes here...
		        }
		        return pg.response.message;
			}
			else
				return pg.response.message;
		}
		return "ERROR: Response was null";
	}
	
	
	public String updateUser(HttpServletRequest request, HttpServletResponse response,String first_name, String last_name, String phone, String email, String password, String[] communities) throws IOException, NoSuchAlgorithmException, UnsupportedEncodingException, URISyntaxException 
	{
		String charset = "UTF-8";
		String encrypted_password = null;
		if (password != null)
			encrypted_password = encryptWithoutEncode(password);
		
		wpuser wpuser = new wpuser();
		wpauth wpauth = new wpauth();
		
		wpuser.firstname = first_name;
		wpuser.lastname = last_name;
		wpuser.phone = phone;
		wpuser.email = new String[1];
		wpuser.email[0] = email;
		
		wpauth.username = email;
		wpauth.password = encrypted_password;

		
		String theUrl = API_ROOT + "social/person/update";
		String data = "{ \"user\":" + new Gson().toJson(wpuser) + ", \"auth\":" + new Gson().toJson(wpauth) + "}";

		String json = postToRestfulApi(theUrl, data, request, response);

		if (json !=null)
		{
			personGet pg = new Gson().fromJson(json, personGet.class);
			if (pg.response.success == true)
			{
				personGet.community[] user_comms = getUserCommunities(request,response);
				for(int i = 0; i < user_comms.length; i++)
		        {
		        	if (containedInArray(user_comms[i]._id, communities))
		        	{
		        		addUserToCommunity(request, response, user_comms[i]._id, email);
		        	}
		        	else
		        	{
		        		removeUserFromCommunity(request, response, user_comms[i]._id, email);
		        	}
		        }
			}
				
			return pg.response.message;
		}
		return "ERROR: Response was null";
	}
	
	public boolean containedInArray(String term, String[] array)
	{
		if (null != array)
		{
			for(int i = 0; i < array.length; i++)
			{
				if (array[i].equalsIgnoreCase(term))
					return true;
			}
		}
		return false;
	}
	
	public void addUserToCommunity(HttpServletRequest request, HttpServletResponse response, String communityId, String personId)
	{
		//if they have beena member before, we can just set their status back to active:
		//setUserCommunityStatus(request, response, communityId, personId, "active");
		
		//Otherwise, the invite call
		String url = API_ROOT + "social/community/member/invite/"+communityId+"/"+personId;
		String json = stringOfUrl(url, request, response);
		if (DEBUG_MODE)
			System.out.println("Adding user to community: " + json);
	}
	
	public void setUserCommunityStatus(HttpServletRequest request, HttpServletResponse response, String communityId, String personId, String status)
	{
		String url = API_ROOT + "social/community/member/update/status/"+communityId+"/"+personId+"/" + status;
		String json = stringOfUrl(url, request, response);
		if (DEBUG_MODE)
			System.out.println("Setting User Community Status to " + status + " : " + json);
	}
	
	public void removeUserFromCommunity(HttpServletRequest request, HttpServletResponse response, String communityId, String personId)
	{
		setUserCommunityStatus(request, response, communityId, personId, "remove");
	}
	
	public String deleteUser(HttpServletRequest request, HttpServletResponse response, String username)
	{
		String url = API_ROOT + "social/person/delete/"+username;
		String json = stringOfUrl(url, request, response);
		
		if (json != null)
		{
			keepAlive deleteUser = new Gson().fromJson(json, keepAlive.class);
			return deleteUser.response.message;
		}
		return "ERROR: API Response was NULL";
	}
	
	private String populateUserDropdown(HttpServletRequest request, HttpServletResponse response)
	{
		String toReturn = "";
		String json = stringOfUrl(API_ROOT + "social/community/getsystem", request, response);
		 if (json != null)
		{
			getSystem gs = new Gson().fromJson(json, getSystem.class);
			if (gs != null && gs.data != null && gs.data.members != null)
			{
				for ( getSystem.member member : gs.data.members)
				{
					toReturn += getUserDropdownInfo(request, response, member._id);
				}
			}
			else
			{
				toReturn += getUserDropdownInfo(request, response, null);
			}
		}
		return toReturn; 
	}
	
	private String getSystemCommunityId(HttpServletRequest request, HttpServletResponse response)
	{
		String toReturn = "";
		String json = stringOfUrl(API_ROOT + "social/community/getsystem", request, response);
		 if (json != null)
		{
			getSystem gs = new Gson().fromJson(json, getSystem.class);
			return gs.data._id;
		}
		return ""; 
	}
	
	private String getUserDropdownInfo(HttpServletRequest request, HttpServletResponse response, String userId)
	{
		String delim = "$$$";
		String json = "";
		if (userId == null)
			json = stringOfUrl(API_ROOT + "social/person/get/", request, response);
		else
			json = stringOfUrl(API_ROOT + "social/person/get/"+userId, request, response);
		
		if (json != null)
		{
			getPerson gp = new Gson().fromJson(json, getPerson.class);

			if (null == gp.data) { // user doesn't exist that's fine
				return "";
			}
			
			String _id = gp.data._id;
			if (_id == null)
				_id = "";
			
			String email = gp.data.email;
			if (email == null)
				email = "";
			
			String firstname = gp.data.firstName;
			if (firstname == null)
				firstname = "";
			
			String lastname = gp.data.lastName;
			if (lastname == null)
				lastname = "";
			
			String phone = gp.data.phone;
			if (phone == null)
				phone = "";
			
			String value = _id+delim+email+delim+firstname+delim+lastname+delim+phone+delim;
			if (gp.data.communities != null)
			{
				for ( getPerson.community comm : gp.data.communities)
				{
					value += comm._id+",";
				}
			}
			return "<option value=\""+value+"\" > <b>Edit:</b> " + gp.data.displayName + "</option>";
		}
		return "";
	}

%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
<title>Infinit.e Person Management</title>
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
		if (request.getParameter("deletePerson") != null)
		{
			out.println(deleteUser(request,response, request.getParameter("deletePerson").toString()));
		}
		else if (request.getParameter("updatePerson") != null && request.getParameter("updatePerson").toString().length() > 0)
		{
			String[] communities = request.getParameterValues("communities");

			if(request.getParameter("keepPassword") != null)
			{
				out.println(updateUser(request, response, request.getParameter("first_name").toString(), request.getParameter("last_name").toString(), request.getParameter("phone").toString(), request.getParameter("email").toString(), null, communities));
			}
			else
			{
				out.println(updateUser(request, response, request.getParameter("first_name").toString(), request.getParameter("last_name").toString(), request.getParameter("phone").toString(), request.getParameter("email").toString(), request.getParameter("password").toString(), communities));
			}
		}
		else if (request.getParameter("add_user") != null)
		{
			String[] communities = request.getParameterValues("communities");

			out.println(createUser(request, response, request.getParameter("first_name").toString(), request.getParameter("last_name").toString(), request.getParameter("phone").toString(), request.getParameter("email").toString(), request.getParameter("password").toString(), communities));
			
			
	        
		}
		out.println("</div>");
	
	%>
	
	<script>
		function validate_fields()
		{
			reg = /^([A-Za-z0-9_\-\.])+\@([A-Za-z0-9_\-\.])+\.([A-Za-z]{2,4})$/;
   			email = document.getElementById('email').value;
			first_name = document.getElementById('first_name').value;
			last_name = document.getElementById('last_name').value;
			phone = document.getElementById('phone').value;
			email = document.getElementById('email').value;
			password = document.getElementById('password').value;
			password_match = document.getElementById('password_match').value;
			keep_password = document.getElementById('keepPassword').checked;
			
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
		
			if(reg.test(email) == false)
			{
				alert('Invalid email address');
				return false;
			}
			if (first_name == "")
			{
				alert('Please provide a first name.');
				return false;
			}
			if (last_name == "")
			{
				alert('Please provide a last name.');
				return false;
			}
			if (phone == "" )
			{
				alert('Please provide a phone number.');
				return false;
			}
			if (keep_password == false )
			{
				if (password == "")
				{
					alert('Please provide a password.');
					return false;
				}
				
				if (password != password_match)
				{
					alert('Passwords do not match.');
					return false;
				}
				
				
			}
			/* if (numSelected == 0)
			{
				alert('Please select at least 1 community');
				return false;
			} */
		}
		function confirmDelete()
		{
			var agree=confirm("Are you sure you wish to Delete this file from the File Share?");
			if (agree)
				return true ;
			else
				return false ;
		}
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
	
			first_name = document.getElementById('first_name');
			last_name = document.getElementById('last_name');
			phone = document.getElementById('phone');
			email = document.getElementById('email');
			deleteButton = document.getElementById('deleteButton');
			dropdown = document.getElementById("user_info");
			deletePerson = document.getElementById("deletePerson");
			updatePerson = document.getElementById("updatePerson");
			list = dropdown.options[dropdown.selectedIndex].value;
			keepPassword_checkbox = document.getElementById('keepPassword');
			keepPassword_label = document.getElementById('keepPasswordLabel');
			password_box = document.getElementById('password');
			password_repeat_box = document.getElementById('password_match');
			add_user_button = document.getElementById('add_user');
			
			if (list == "new")
			{
				first_name.value = "";
				last_name.value = "";
				phone.value = "";
				email.value = "";
				deletePerson.value = "";
				updatePerson.value = "";
				deleteButton.style.visibility = 'hidden';
				keepPassword_checkbox.style.display = 'none';
				keepPassword_checkbox.checked = false;
				keepPassword_label.style.display = 'none';
				password_box.style.display = "";
				password_repeat_box.style.display = "";
				add_user_button.value = "Add User";
				clearCommList();
				return;
			}
			//_id  email  firstName   lastName   phone   communities;
			split = list.split("$$$");
			
			deletePerson.value = split[0];
			updatePerson.value = split[0];
			first_name.value = split[2];
			last_name.value = split[3];
			phone.value = split[4];
			email.value = split[1];
			var community_vals = split[5];
	
			highlightComms(community_vals.split(","));
			deleteButton.style.visibility = '';
			keepPassword_checkbox.style.display = '';
			keepPassword_checkbox.checked = true;
			password_box.style.display = "none";
			password_repeat_box.style.display = "none";
			keepPassword_label.style.display = '';
			add_user_button.value = "Edit User";
		}
		
		function confirmDelete()
		{
			var agree=confirm("Are you sure you wish to Delete this User?");
			if (agree)
				return true ;
			else
				return false ;
		}
		
		function keepPasswordToggle()
		{
			password_box = document.getElementById('password');
			password_repeat_box = document.getElementById('password_match');
			keepPasswordLabel = document.getElementById('keepPasswordLabel');
			
			if (document.getElementById('keepPassword').checked)
			{
				password_box.style.display = "none";
				password_repeat_box.style.display = "none";
				//keepPasswordLabel.innerHTML='Set New Password';
			}
			else
			{
				password_box.style.display = "";
				password_repeat_box.style.display = "";
				//keepPasswordLabel.innerHTML='Use Existing Password';
			}
		}	

	</script>
		<div id="uploader_outter_div" name="uploader_outter_div" align="center" style="width:100%" >
	    	<div id="uploader_div" name="uploader_div" style="border-style:solid; border-color:#999999; border-radius: 10px; width:450px; margin:auto">
	        	<h2>User Management</h2>
	        	<form id="delete_form" name="delete_form" method="post" onsubmit="javascript:return confirmDelete()" >
	        		<select id="user_info" onchange="populate()" name="upload_info"><option value="new">Add New User</option> <% out.print(populateUserDropdown(request, response)); %></select>
	                <input type="hidden" name="deletePerson" id="deletePerson" />
	                <input type="submit" name="deleteButton" id="deleteButton" style="visibility:hidden;" value="Delete" />
	        	</form>
	            <form id="upload_form" name="upload_form" method="post" onsubmit="javascript:return validate_fields();" >
	                <table width="100%" border="0" cellspacing="0" cellpadding="0" style="padding-left:10px; padding-right:10px">           
	                  <tr>
	                  	<td>First Name:</td>
	                  	<td><input type="text" name="first_name" id="first_name" /></td>
	                  </tr>
	                  <tr>
	                  	<td>Last Name:</td>
	                  	<td><input type="text" name="last_name" id="last_name" /></td>
	                  </tr>
	                  <tr>
	                  <tr>
	                  	<td>Phone:</td>
	                  	<td><input type="text" name="phone" id="phone" /></td>
	                  </tr>
	                  <tr>
	                  <tr>
	                  	<td>Email/Username:</td>
	                  	<td><input type="text" name="email" id="email" /></td>
	                  </tr>
	                  <tr>
	                  	<td>Password:</td>
	                  	<td><input type="password" name="password" id="password" /><input type="checkbox" id="keepPassword" style="display:none;" name="keepPassword" onchange="keepPasswordToggle()" /> <span id="keepPasswordLabel" name="keepPasswordLabel" style="display:none;"> Use Existing Password </span></td>
	                  </tr>
	                  <tr>
	                  	<td>Repeat Password</td>
	                  	<td><input type="password" name="password_match" id="password_match" /></td>
	                  </tr>
	                  <tr>
	                  	<td>Communities:</td>
	                    <td align="center"><% out.print(generateCommunityList(request, response)); %></td>
	                  </tr>
	                  <tr>
	                  	<td colspan="2" align="right"><input type="submit" name="add_user" id="add_user" value="Add User"/></td>
	                  </tr>
	               </table>
	                  <input type="hidden" name="updatePerson" id="updatePerson" />
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

	String errorMsg = "";
	if (request.getParameter("logintext") != null || request.getParameter("passwordtext") != null)
	{
		if(logMeIn(request.getParameter("logintext"),request.getParameter("passwordtext"), request, response))
		{
			showAll = (request.getParameter("sudo") != null);
			DEBUG_MODE = (request.getParameter("debug") != null);
			out.println("<meta http-equiv=\"refresh\" content=\"0\">");
			out.println("Successfully Logged In. Please Wait.");
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
