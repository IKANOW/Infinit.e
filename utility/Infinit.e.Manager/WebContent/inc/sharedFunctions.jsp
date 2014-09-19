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

<%@ page import="java.io.*" %>
<%@ page import="java.util.*" %>
<%@ page import="com.google.common.collect.TreeMultimap" %>
<%@ page import="java.net.*" %>
<%@ page import="javax.naming.*" %>
<%@ page import="javax.servlet.jsp.*" %>
<%@ page import="javax.script.ScriptException"%>
<%@ page import="javax.script.ScriptEngineManager"%>
<%@ page import="javax.script.ScriptEngine"%>
<%@ page import="org.apache.commons.io.*" %>
<%@ page import="com.google.gson.Gson" %>
<%@ page import="org.json.JSONObject" %>
<%@ page import="org.json.JSONArray" %>
<%@ page import="org.bson.types.ObjectId" %>
<%@ page import="infinit.e.web.util.Utils" %>
<%@ page import="org.apache.commons.httpclient.HttpClient" %>
<%@ page import="org.apache.commons.httpclient.HttpStatus" %>
<%@ page import="org.apache.commons.httpclient.methods.PostMethod" %>
<%@ page import="java.io.BufferedReader" %>
<%@ page import="java.io.InputStreamReader" %>
<%@ page import="org.apache.commons.lang.StringEscapeUtils.*" %>

<%@ page import ="java.security.KeyManagementException" %>
<%@ page import ="java.security.NoSuchAlgorithmException" %>
<%@ page import ="java.security.SecureRandom" %>
<%@ page import ="java.security.cert.CertificateException" %>
<%@ page import ="java.security.cert.X509Certificate" %>
<%@ page import ="javax.net.ssl.HostnameVerifier" %>
<%@ page import ="javax.net.ssl.HttpsURLConnection" %>
<%@ page import ="javax.net.ssl.SSLContext" %>
<%@ page import ="javax.net.ssl.SSLSession" %>
<%@ page import ="javax.net.ssl.TrustManager" %>
<%@ page import ="javax.net.ssl.X509TrustManager" %>


<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="language" value="${not empty param.language ? param.language : not empty language ? language : pageContext.request.locale}" />
<fmt:setLocale value="${language}" />
<fmt:setBundle basename="infinit.e.web.localization.text" />

<%!
	// !----------  ----------!
	String API_ROOT = null;
		
	// !----------  ----------!
	String messageToDisplay = "";
	
%>


<%

	//!---------- Read AppConstants.js to get the API_ROOT value  ----------!
	if (API_ROOT == null)
	{
		URL baseUrl = new URL(request.getScheme(), request.getServerName(), request.getServerPort(), "");
		ScriptEngineManager manager = new ScriptEngineManager();
		ScriptEngine engine = manager.getEngineByName("javascript");		
		String appConstantFile = null;

		InputStream in = null;
		// Use file from local deployment always
		try
		{
			in =  new FileInputStream (application.getRealPath("/") + "AppConstants.js");
			appConstantFile = IOUtils.toString( in );
		}
		catch (Exception e)
		{
			//System.out.println("Exception: " + e.getMessage());
		}
		

		// Eval the file as JavaScript through or JS engine and call getEndPointUrl
		try
		{
			engine.eval(appConstantFile);
			engine.eval("output = getEndPointUrl();");
			API_ROOT = (String) engine.get("output");
		}
		catch (Exception e)
		{
			//System.out.println("Exception: " + e.getMessage());
		}
		if (null == API_ROOT) { 
			// Default to localhost
			API_ROOT = "http://localhost:8080/api/";
			//API_ROOT = "http://localhost:8888/api/";
			//API_ROOT = "http://localhost:8184/";
		}		
	}

	boolean isLoggedIn = false;
	messageToDisplay = "";
	
	// Page request is a post back from the login form
	if (request.getParameter("username") != null && request.getParameter("password") != null) 
	{
		String username = request.getParameter("username");
		String password = request.getParameter("password");
		isLoggedIn = getLogin(username, password, request, response);
		
		// Temp fix, refresh the page to retrieve the new cookie that was set
		out.println("<meta http-equiv=\"refresh\" content=\"0\">");
	}
	// Make sure user is already logged in and retrieve their user id
	else 
	{
		isLoggedIn = isLoggedIn(request, response);
	}

%>

	
<%!	
//!---------- SSL handling -------------!

	/**/
	static class TrustManagerManipulator implements X509TrustManager {
	
		private static TrustManager[] trustManagers;
		private static final X509Certificate[] acceptedIssuers = new X509Certificate[] {};
	
	
		public boolean isClientTrusted(X509Certificate[] chain) {
			return true;
		}
	
		public boolean isServerTrusted(X509Certificate[] chain) {
			return true;
		}
	
	
		public static void allowAllSSL() {
			HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
				public boolean verify(String hostname, SSLSession session) {
					return true;
				}
			});
			SSLContext context = null;
			if (trustManagers == null) {
				trustManagers = new TrustManager[] { new TrustManagerManipulator() };
			}
			try {
				context = SSLContext.getInstance("TLS");
				context.init(null, trustManagers, new SecureRandom());
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			} catch (KeyManagementException e) {
				e.printStackTrace();
			}
			HttpsURLConnection.setDefaultSSLSocketFactory(context
					.getSocketFactory());
		}
	
		public void checkClientTrusted(X509Certificate[] chain, String authType)
				throws CertificateException {
		}
	
		public void checkServerTrusted(X509Certificate[] chain, String authType)
				throws CertificateException {
		}
	
		public X509Certificate[] getAcceptedIssuers() {
			return acceptedIssuers;
		}
	}

	// !---------- Start login/session handling code ----------!
	String shares = null;
	
	// Classes used to handle login and session
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
	
	
	// isLoggedIn - check to see if user is already logged in
	public Boolean isLoggedIn(HttpServletRequest request, HttpServletResponse response) 
	{
		String json = callRestfulApi("auth/keepalive", request, response);
		if (json != null) 
		{
			keepAlive keepA = new Gson().fromJson(json, keepAlive.class);
			return keepA.response.success;
		} 
		else 
		{
			return false;
		}
	} // TESTED
	
	// isLoggedInAsAdmin_GetAdmin - check to see if user is already logged in
	// NOTE tri-state ... null means admin but not enabled, true is admin and active, false is definitely not admin
	public Boolean isLoggedInAsAdmin_GetAdmin(boolean bGetAdmin, HttpServletRequest request, HttpServletResponse response) 
	{
		String json = callRestfulApi("auth/keepalive/admin?override=" + bGetAdmin, request, response);
		if (json != null) 
		{
			keepAlive keepA = new Gson().fromJson(json, keepAlive.class);
			if (keepA.response.success) {
				if (keepA.response.message.contains("Active Admin"))
					return true;
				else
					return null;
			}
			else return false;
		} 
		else 
		{
			return false;
		}
	} // TESTED
	
	// getLogin - attempt to log user in
	private Boolean getLogin(String username, String password, HttpServletRequest request, HttpServletResponse response) 
	{
		String json = callRestfulApi("auth/login/" + username + "/" + Utils.encrypt(password), request, response);
		if (json != null) 
		{
			logIn login = new Gson().fromJson(json, logIn.class);
			if (login == null) 
			{
				return false;
			} 
			else 
			{
				return login.response.success;
			}
		} 
		else 
		{
			return false;
		}
	} // TESTED
	
	// !---------- End login/session handling code ----------!
	
	// !---------- Start Get/Post API Handlers ----------!
	
	// callRestfulApi - Calls restful API and returns results as a string
	public String callRestfulApi(String addr, HttpServletRequest request, HttpServletResponse response) 
	{
		return callRestfulApi(addr, request, response, null);
	}
	public String callRestfulApi(String addr, HttpServletRequest request, HttpServletResponse response, String newUrl) 
	{
		try 
		{
			TrustManagerManipulator.allowAllSSL();
			
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			if (newUrl == null) {
				newUrl = API_ROOT + addr;
			}
			URL url = new URL(newUrl);
			HttpURLConnection urlConnection = (HttpURLConnection )url.openConnection();
    		urlConnection.addRequestProperty("X-Forwarded-For", request.getRemoteAddr());
			String cookieVal = getBrowserInfiniteCookie(request);
        	if (cookieVal != null)
        	{
        		urlConnection.addRequestProperty("Cookie","infinitecookie=" + cookieVal);
        		urlConnection.setDoInput(true);
        		urlConnection.setDoOutput(true);
        		urlConnection.setRequestProperty("Accept-Charset","UTF-8");
        	}
        	int status = urlConnection.getResponseCode();
        	if (status != HttpURLConnection.HTTP_OK) {
        		if (status == HttpURLConnection.HTTP_MOVED_TEMP
        			|| status == HttpURLConnection.HTTP_MOVED_PERM
        				|| status == HttpURLConnection.HTTP_SEE_OTHER)
	        	{
	        		return callRestfulApi(addr, request, response, urlConnection.getHeaderField("Location"));
	        	}
        	}        	
			IOUtils.copy(urlConnection.getInputStream(), output);
			String newCookie = getConnectionInfiniteCookie(urlConnection);
        	if (newCookie != null && response != null)
        	{
        		setBrowserInfiniteCookie(response, newCookie, request.getServerPort());
        	}
			return output.toString();
		} 
		catch (IOException e) 
		{
			System.out.println(e.getMessage());
			return null;
		}
	} // TESTED
	
	
	// postToRestfulApi - 
	// Note: params in the addr field need to be URLEncoded
	public String postToRestfulApi(String addr, String data, HttpServletRequest request, HttpServletResponse response) 
	{
		return postToRestfulApi(addr, data, request, response, null);
	}
	public String postToRestfulApi(String addr, String data, HttpServletRequest request, HttpServletResponse response, String newUrl) 
	{
		String result = "";
	    try
		{
			TrustManagerManipulator.allowAllSSL();
			
			if (newUrl == null) {
				newUrl = API_ROOT + addr;
			}
			URL url = new URL(newUrl);
			HttpURLConnection connection = (HttpURLConnection )url.openConnection();
			
	    	String cookieVal = getBrowserInfiniteCookie(request);
        	if (cookieVal != null)
        	{
        		connection.addRequestProperty("Cookie","infinitecookie=" + cookieVal);
        		connection.setDoInput(true);
        	}
	    	connection.setDoOutput(true);
			connection.setRequestProperty("Accept-Charset", "UTF-8");
			
			// Post JSON string to URL
			OutputStream os = connection.getOutputStream();
			byte[] b = data.getBytes("UTF-8");
			os.write(b);

			// Check for HTTP->HTTPS redirect
        	int status = connection.getResponseCode();
        	if (status != HttpURLConnection.HTTP_OK) {
        		if (status == HttpURLConnection.HTTP_MOVED_TEMP
        			|| status == HttpURLConnection.HTTP_MOVED_PERM
        				|| status == HttpURLConnection.HTTP_SEE_OTHER)
	        	{
	        		return postToRestfulApi(addr, data, request, response, connection.getHeaderField("Location"));
	        	}
        	}        				
			
			// Receive results back from API
			InputStream is = connection.getInputStream();
			result = IOUtils.toString(is, "UTF-8");
			
			String newCookie = getConnectionInfiniteCookie(connection);
        	if (newCookie != null && response != null)
        	{
        		setBrowserInfiniteCookie(response, newCookie, request.getServerPort());
        	}
		}
		catch (Exception e)
		{
			//System.out.println("Exception: " + e.getMessage());
		}
		return result;
	} // TESTED
	
	public String postToRestfulApi(String addr, byte[] data, String mimeType, HttpServletRequest request, HttpServletResponse response) 
	{
		return postToRestfulApi(addr, data, mimeType, request, response, null);
	}
	public String postToRestfulApi(String addr, byte[] data, String mimeType, HttpServletRequest request, HttpServletResponse response, String newUrl) 
	{
		String result = "";
	    try
		{
			TrustManagerManipulator.allowAllSSL();
			
			if (newUrl == null) {
				newUrl = API_ROOT + addr;
			}
			URL url = new URL(newUrl);
			HttpURLConnection connection = (HttpURLConnection )url.openConnection();
			
	    	String cookieVal = getBrowserInfiniteCookie(request);
        	if (cookieVal != null)
        	{
        		connection.addRequestProperty("Cookie","infinitecookie=" + cookieVal);
        		connection.setDoInput(true);
        	}
	    	connection.setDoOutput(true);
			connection.setRequestProperty("Accept-Charset", "UTF-8");
			if (mimeType != null && mimeType.length() > 0)
				connection.setRequestProperty("Content-Type", mimeType);
			
			// Post JSON string to URL
			OutputStream os = connection.getOutputStream();
			os.write(data);

			// Check for HTTP->HTTPS redirect
        	int status = connection.getResponseCode();
        	if (status != HttpURLConnection.HTTP_OK) {
        		if (status == HttpURLConnection.HTTP_MOVED_TEMP
        			|| status == HttpURLConnection.HTTP_MOVED_PERM
        				|| status == HttpURLConnection.HTTP_SEE_OTHER)
	        	{
	        		return postToRestfulApi(addr, data, mimeType, request, response, connection.getHeaderField("Location"));
	        	}
        	}        				
			
			// Receive results back from API
			InputStream is = connection.getInputStream();
			result = IOUtils.toString(is, "UTF-8");
			
			String newCookie = getConnectionInfiniteCookie(connection);
        	if (newCookie != null && response != null)
        	{
        		setBrowserInfiniteCookie(response, newCookie, request.getServerPort());
        	}
		}
		catch (Exception e)
		{
			//System.out.println("Exception: " + e.getMessage());
		}
		return result;
	} // TESTED
	
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
	
	
	// getBrowserInfiniteCookie
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
	} // TESTED
	
	
	// getConnectionInfiniteCookie
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
	} // TESTED
	
	
	//!---------- END Get/Post API Handlers ----------!
	
	
	
	//!---------- Start Infinit.e API Calls ----------!
	
	
	// logOut -
	public String logOut(HttpServletRequest request, HttpServletResponse response) 
	{
		return callRestfulApi("auth/logout/", request, response);
	} // TESTED
	
	// Admin logOut - note doesn't actually log you out, just relinquishes admin
	public String adminLogOut(HttpServletRequest request, HttpServletResponse response) 
	{
		return callRestfulApi("auth/logout/admin/", request, response);
	} // TESTED
	
	// getShare -
	private String getShare(String id, HttpServletRequest request, HttpServletResponse response) 
	{
		return callRestfulApi("social/share/get/" + id + "/", request, response);
	} // TESTED
	
	
	// getShareObject - 
	public JSONObject getShareObject(String id, HttpServletRequest request, HttpServletResponse response) 
	{
		try 
		{
			return new JSONObject(getShare(id, request, response));
		} 
		catch (Exception e) 
		{
			return null;
		}
	} // TESTED


	
	// getUserSources - 
	private String getUserSources(HttpServletRequest request, HttpServletResponse response) 
	{
		return callRestfulApi("config/source/user?stripped=true", request, response);
	} // TESTED
	
	//getPersonList
	private String getPeopleList(HttpServletRequest request, HttpServletResponse response) 
	{
		return callRestfulApi("social/person/list/", request, response);
	}
	
	// getSystemCommunity - 
	public String getSystemCommunity(HttpServletRequest request, HttpServletResponse response) 
	{
		return callRestfulApi("social/community/getsystem/", request, response);
	}
	
	// getAllCommunities - 
	private String getAllCommunities(HttpServletRequest request, HttpServletResponse response) 
	{
		return callRestfulApi("social/community/getall/", request, response);
	}
	
	
	// getPublicCommunities - 
	public String getPublicCommunities(HttpServletRequest request, HttpServletResponse response) 
	{
		return callRestfulApi("social/community/getpublic/", request, response);
	}
	
	
	// getCommunity - 
	private String getCommunity(String id, HttpServletRequest request, HttpServletResponse response) 
	{
		return callRestfulApi("social/community/get/" + id, request, response);
	}
	
	// getCommunity - 
	public String removeCommunity(String id, HttpServletRequest request, HttpServletResponse response) 
	{
		return callRestfulApi("social/community/remove/" + id, request, response);
	}
	
	
	// updateCommunityMemberStatus - 
	public String updateCommunityMemberStatus(String communityid, String personid, String status,
			HttpServletRequest request, HttpServletResponse response) 
	{
		return callRestfulApi("social/community/member/update/status/" + communityid + "/" 
			+ personid + "/" + status, request, response);
	}
	
	
	// updateCommunityMemberType - 
	public String updateCommunityMemberType(String communityid, String personid, String type,
			HttpServletRequest request, HttpServletResponse response) 
	{
		return callRestfulApi("social/community/member/update/type/" + communityid + "/" 
			+ personid + "/" + type, request, response);
	}
	
	
	// getSource - 
	public String getSource(String sourceId, HttpServletRequest request, HttpServletResponse response) 
	{
		return callRestfulApi("config/source/get/" + sourceId, request, response);
	}
	
	
	// deleteSource
	public String deleteSource(String sourceId, boolean bDocsOnly, String communityId, HttpServletRequest request, HttpServletResponse response)
	{
		if (bDocsOnly) {
			return callRestfulApi("config/source/delete/docs/" + sourceId + "/" + communityId, request, response);
		}
		else {
			return callRestfulApi("config/source/delete/" + sourceId + "/" + communityId, request, response);
		}
	}
	
	// suspendSource
	public String suspendSource(String sourceId, boolean shouldSuspend, String communityId, HttpServletRequest request, HttpServletResponse response)
	{
		return callRestfulApi("config/source/suspend/" + sourceId + "/" + communityId + "/" + shouldSuspend, request, response);
	}
	
	
	
	// getListOfAllShares - 
	public Map<String,String> getListOfAllShares(HttpServletRequest request, HttpServletResponse response)
	{
		Map<String,String> allShares = new HashMap<String,String>();
		
		// publishedSources - array of source._ids of published sources
		ArrayList<String> publishedSources = new ArrayList<String>();
		try
		{
			JSONObject sharesObject = new JSONObject( getAllShares(request, response) );
			JSONObject json_response = sharesObject.getJSONObject("response");
			
			if (json_response.getString("success").equalsIgnoreCase("true")) 
			{
				if (sharesObject.has("data")) 
				{
					// Iterate over share objects and write to our collection
					JSONArray shares = sharesObject.getJSONArray("data");
					for (int i = 0; i < shares.length(); i++) 
					{
						JSONObject share = shares.getJSONObject(i);
						allShares.put(share.getString("title"), share.getString("_id"));
					}
				}
			}
		}
		catch (Exception e)
		{
			System.out.println(e.getMessage());
		}
		return allShares;
	}
	
	
	// getAllShares
	private String getAllShares(HttpServletRequest request, HttpServletResponse response)
	{
		return callRestfulApi("social/share/search/", request, response);
	}
	
	
	// getListOfSharesByType -
	public Map<String,String> getListOfSharesByType(String type, HttpServletRequest request, HttpServletResponse response)
	{
		Map<String,String> allShares = new HashMap<String,String>();
		
		// publishedSources - array of source._ids of published sources
		ArrayList<String> publishedSources = new ArrayList<String>();
		try
		{
			JSONObject sharesObject = new JSONObject( searchSharesByType(type, request, response) );
			JSONObject json_response = sharesObject.getJSONObject("response");
			
			if (json_response.getString("success").equalsIgnoreCase("true")) 
			{
				if (sharesObject.has("data")) 
				{
					// Iterate over share objects and write to our collection
					JSONArray shares = sharesObject.getJSONArray("data");
					for (int i = 0; i < shares.length(); i++) 
					{
						JSONObject share = shares.getJSONObject(i);
						allShares.put(share.getString("title"), share.getString("_id"));
					}
				}
			}
		}
		catch (Exception e)
		{
			System.out.println(e.getMessage());
		}
		return allShares;
	}
	
	// searchSharesByType
	private String searchSharesByType(String typeStr, HttpServletRequest request, HttpServletResponse response)
	{
		return callRestfulApi("social/share/search/?type=" + typeStr,request, response);
	}
	
	
	// getSourceShares - 
	private String getSourceShares(HttpServletRequest request, HttpServletResponse response) 
	{

		return callRestfulApi("social/share/search/?type=source,source_published&searchby=community&id=*",request, response);
	} // TESTED
	
	
	enum FilterType { title, description, tags, community, key, url, id, mediaType, extractType, ownerId,
						tempQuarantined, fullQuarantined, suspended };
	
	// getUserSources - 
	public TreeMultimap<String,String> getUserSourcesAndShares(HttpServletRequest request, HttpServletResponse response, String filter)
	{
		TreeMultimap<String,String> userSources = TreeMultimap.create();
		String userIdStr = null;
		
		FilterType filterType = FilterType.title;
		for (FilterType type: FilterType.values()) {
			String prefix = type.toString() + ":";
			if (filter.startsWith(prefix)) {
				filterType = type;
				filter = filter.substring(prefix.length());
				break;
			}
		}//TOTEST
		
		// publishedSources - array of source._ids of published sources
		ArrayList<String> publishedSources = new ArrayList<String>();
		Map<String,JSONObject> srcEnabled = new HashMap<String, JSONObject>();
		try
		{
			JSONObject personObj = new JSONObject ( getPerson(request, response) );
			if (personObj.has("data"))
			{
				JSONObject person = new JSONObject ( personObj.getString("data") );
				userIdStr = person.getString("_id");
			}
			JSONObject json;
			JSONObject json_response;
			
			//STEP 1: Get sources first so we can get enable/disable status
			//fills out a map srcEnabled with <_id, {title, enabled}>
			String tempJson = getUserSources(request, response);
			if (tempJson != null)
 			{
				json = new JSONObject(tempJson);
				json_response = json.getJSONObject("response");
				if (json_response.getString("success").equalsIgnoreCase("true")) 
				{
					if (json.has("data")) 
					{
						// Iterate over source objects and write to our collection
						JSONArray data = json.getJSONArray("data");
						for (int i = 0; i < data.length(); i++) 
						{
							JSONObject sourceObj = data.getJSONObject(i);
							JSONObject srcEnabledObject = new JSONObject();														
							
							if (isSourceFiltered(filterType, sourceObj, filter)) {
								continue;
							}//TESTED
							
							String tempTitle = sourceObj.getString("title");
							if ( isSuspended(sourceObj) )
								tempTitle = "[SUSPENDED] " + tempTitle;
							
							if (sourceObj.has("ownerId") && !sourceObj.getString("ownerId").equalsIgnoreCase(userIdStr)) tempTitle += " (+)";							
							
							srcEnabledObject.put("title", tempTitle);
							srcEnabledObject.put("suspended", isSuspended(sourceObj));
							srcEnabled.put(sourceObj.getString("_id"), srcEnabledObject);
						}
					}
				}
 			}
			
			//STEP 2: get shares, if source exists, use its enable/disable status, otherwise use share
			// Get the user's shares from social.share where type = source or source_published
			tempJson = getSourceShares(request, response);
			
			// Covert to JSONObject
			json = new JSONObject(tempJson);
			json_response = json.getJSONObject("response");
			if (json_response.getString("success").equalsIgnoreCase("true")) 
			{
				if (json.has("data")) 
				{
					// Iterate over share objects and write to our collection
					JSONArray data = json.getJSONArray("data");
					for (int i = 0; i < data.length(); i++) 
					{
						JSONObject shareObj = data.getJSONObject(i);
						
						JSONObject shareOrSource = shareObj;
						if ((filterType != FilterType.title) ||  (filterType != FilterType.description)) {
							if (shareObj.has("share"))
								shareOrSource = new JSONObject(shareObj.getString("share"));
						}//TESTED
						if (isSourceFiltered(filterType, shareOrSource, filter)) {
							continue;
						}//TESTED
						
						//remove item from source array so we dont write its name again below						
						String tempTitle = shareObj.getString("title");						
						JSONObject sourceObj = new JSONObject( shareObj.getString("share") );
						boolean suspended = false;
						if (sourceObj.has("_id")) 
						{
							publishedSources.add( sourceObj.getString("_id") );
							JSONObject actual_source = srcEnabled.remove(sourceObj.getString("_id"));
							if ( actual_source != null )
								suspended = actual_source.getBoolean("suspended");
						}						
						if ( isSuspended(sourceObj) || suspended )
							tempTitle = "[SUSPENDED] " + tempTitle;
						
						if (sourceObj.has("ownerId") && !sourceObj.getString("ownerId").equalsIgnoreCase(userIdStr)) 
							tempTitle += " (+)";
						tempTitle += " (*)";
						
						userSources.put(tempTitle, shareObj.getString("_id"));
					}
				}
			}
			
			//STEP 3: loop over remaining sources, add them to the list
			for (String key : srcEnabled.keySet() )			
			{				
				userSources.put(srcEnabled.get(key).getString("title"), key );
			}			
		}
		catch (Exception e)
		{
			System.out.println(e.getMessage());
		}
		return userSources;
	}
	
	private boolean isSourceFiltered(FilterType filterType, JSONObject sourceOrShare, String filter)
	{
		String value = "";
		try {
			if (filter.length() > 0) {
				if (FilterType.title == filterType) {
					value = sourceOrShare.getString("title");
				}
				if (FilterType.description == filterType) {
					value = sourceOrShare.getString("description");
				}
				else if (FilterType.tags == filterType) {
					JSONArray array = sourceOrShare.getJSONArray("tags");
					value = array.toString();
				}
				else if (FilterType.community == filterType) {
					JSONArray array = sourceOrShare.getJSONArray("communityIds");
					value = array.toString();					
				}
				else if (FilterType.key == filterType) {
					value = sourceOrShare.getString("key");					
				}
				else if (FilterType.url == filterType) { // annoyingly complicated!
					StringBuffer allUrls = new StringBuffer();
					if (sourceOrShare.has("url")) {
						allUrls.append(sourceOrShare.getString("url")).append(" ");
					}//TESTED
					if (sourceOrShare.has("rss")) { // (Note will only work on shares because sources have this field stripped)
						JSONObject rss = sourceOrShare.getJSONObject("rss");
						if (rss.has("extraUrls")) {
							JSONArray urls = rss.getJSONArray("extraUrls");
							allUrls.append(urls.toString()).append(" ");
						}
					}//TESTED
					if (sourceOrShare.has("processingPipeline")) { // (Note will only work on shares because sources have this field stripped)
						JSONArray pxPipeline = sourceOrShare.getJSONArray("processingPipeline");
						if (pxPipeline.length() > 0) {
							JSONObject firstEl = pxPipeline.getJSONObject(0);
							Iterator it = firstEl.keys();
							while (it.hasNext()) {
								String key = (String) it.next();
								if (!key.equalsIgnoreCase("display")) {
									JSONObject obj = firstEl.getJSONObject(key);
									if (null != obj) {
										if (obj.has("url")) {
											String url = obj.getString("url");
											allUrls.append(url).append(" ");
										}//TOTEST
										if (obj.has("extraUrls")) {
											JSONArray urls = obj.getJSONArray("extraUrls");
											allUrls.append(urls.toString()).append(" ");											
										}//TESTED
										break;
									}
								}
							}
						}
					}//TESTED
					value = allUrls.toString();
				}
				else if (FilterType.id == filterType) {
					value = sourceOrShare.getString("_id");															
				}
				else if (FilterType.mediaType == filterType) {
					value = sourceOrShare.getString("mediaType");															
				}
				else if (FilterType.extractType == filterType) {
					value = sourceOrShare.getString("extractType");															
				}
				else if (FilterType.ownerId == filterType) {
					value = sourceOrShare.getString("ownerId");															
				}
				else if (FilterType.tempQuarantined == filterType) {
					value = Boolean.toString(sourceOrShare.has("harvestBadSource") && sourceOrShare.getBoolean("harvestBadSource")); 
				}
				else if (FilterType.fullQuarantined == filterType) {
					value = Boolean.toString(sourceOrShare.has("isApproved") && sourceOrShare.getBoolean("isApproved")); 
				}
				else if (FilterType.suspended == filterType) {
					value = Boolean.toString(isSuspended(sourceOrShare)); 
				}
			}
		}
		catch (Exception e) {
			// field not present, discard source
			/**/
			e.printStackTrace();
		} 
		if ( !value.toLowerCase().contains( filter.toLowerCase() ) )
		{
			return true;
		}
		return false;
	}//TESTED
	
	private boolean isSuspended(JSONObject source)
	{
		try
		{
			if ( source.has("searchCycle_secs") && source.getInt("searchCycle_secs") <= 0)
			{
				return true;
			}
		}
		catch (Exception ex)
		{
			//do nothing, we return false anyways
		}
		return false;
	}
	
	// getListOfAllPeople
	public Map<String,String> getListOfAllPeople(HttpServletRequest request, HttpServletResponse response)
	{
		return getListOfAllPeople(request, response, null);
	}
	private Map<String,String> getListOfAllPeople(HttpServletRequest request, HttpServletResponse response, HashSet<String> personFilter)
	{
		Map<String,String> allPeople = new HashMap<String,String>();
		try
		{
			JSONObject listOfPeopleObj = new JSONObject( getPeopleList(request, response));
			if ( listOfPeopleObj.has("data"))
			{
				JSONArray listOfPeople = new JSONArray ( listOfPeopleObj.getString("data") );
				for ( int i = 0; i < listOfPeople.length(); i++)
				{
					JSONObject person = listOfPeople.getJSONObject(i);
					String id = person.getString("_id");
					if ((null != personFilter) && personFilter.contains(id))
						continue;
					
					if (person.has("displayName"))
					{
						allPeople.put( person.getString("displayName"), id );
					}
					else
					{
						allPeople.put( id, id );
					}
				}
			}
		}
		catch (Exception e)
		{
			System.out.println(e.getMessage());
		}	
		return allPeople;
	}
	
	
	// getListOfCommunityMembers
	public Map<String,String> getListOfCommunityMembers(String id, HttpServletRequest request, HttpServletResponse response)
	{
		Map<String,String> allPeople = new HashMap<String,String>();
		try
		{
			JSONObject communityObj = new JSONObject ( getCommunity(id, request, response) );
			if ( communityObj.has("data") )
			{
				JSONObject community = new JSONObject ( communityObj.getString("data") );
				if ( community.has("members") )
				{
					JSONArray members = community.getJSONArray("members");
					for (int i = 0; i < members.length(); i++) 
					{
						JSONObject member = members.getJSONObject(i);
						if (member.has("displayName"))
						{
							allPeople.put( member.getString("displayName"), member.getString("_id") );
						}
						else
						{
							allPeople.put( member.getString("_id"), member.getString("_id") );
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			//System.out.println(e.getMessage());
		}
		return allPeople;
	}
	
	
	// getListOfAllCommunities
	public TreeMultimap<String,String> getListOfAllNonPersonalCommunities(HttpServletRequest request, HttpServletResponse response)
	{
		TreeMultimap<String,String> allCommunities = TreeMultimap.create();
		try
		{
			JSONObject communitiesObj = new JSONObject ( getAllCommunities(request, response) );
			if ( communitiesObj.has("data") )
			{
				JSONArray communities = communitiesObj.getJSONArray("data");
				for (int i = 0; i < communities.length(); i++) 
				{
					JSONObject community = communities.getJSONObject(i);
					if (community.getString("isPersonalCommunity").equalsIgnoreCase("false") && community.has("name"))
					{
						String name = community.getString("name");
						if (community.has("communityStatus") && (community.getString("communityStatus").equalsIgnoreCase("disabled"))) {
							name += " (Disabled pending deletion)";
						}
						allCommunities.put( name, community.getString("_id") );
					}
				}
			}
		}
		catch (Exception e)
		{
			System.out.println(e.getMessage());
		}
		return allCommunities;
	}
	
	
	// getPerson -
	public String getPerson(HttpServletRequest request, HttpServletResponse response) 
	{
		return callRestfulApi("social/person/get/", request, response);
	} // TESTED
	
	
	// getPerson -
	public String getPerson(String id, HttpServletRequest request, HttpServletResponse response) 
	{
		return callRestfulApi("social/person/get/" + id, request, response);
	}
	
	
	// getPersonCommunities -
	public JSONArray getPersonCommunities(HttpServletRequest request, HttpServletResponse response)
	{
		JSONArray communities = null;
		try
		{
			JSONObject person = new JSONObject( getPerson(request, response) );
			JSONObject json_response = person.getJSONObject("response");
			if (json_response.getString("success").equalsIgnoreCase("true"))
			{
				JSONObject data = person.getJSONObject("data");
				communities = data.getJSONArray("communities");
				return communities;
			}
		}
		catch (Exception e)
		{
		}
		return null;
	} // TESTED 
	
	
	// deletePerson
	public String deletePerson(String id, HttpServletRequest request, HttpServletResponse response)
	{
		return callRestfulApi("social/person/delete/" + id, request, response);
	} // TESTED 
	
	
	// updatePassword
	public String updatePassword(String id, String password, HttpServletRequest request, HttpServletResponse response)
	{
		return callRestfulApi("social/person/update/password/" + id + "/" + password, request, response);
	} // TESTED 
	
	
	// addToCommunity
	public String addToCommunity(String community, String person, HttpServletRequest request, HttpServletResponse response)
	{
		return callRestfulApi("social/community/member/invite/" + community + "/" + person + "?skipinvitation=true", request, response);
	} // TESTED 
	
	// removeFromCommunity
	public String removeFromCommunity(String community, String person, HttpServletRequest request, HttpServletResponse response)
	{
		return callRestfulApi("social/community/member/update/status/" + community + "/" + person + "/remove", request, response);
	} // TESTED
	
	
	
	
	// !---------- End Get/Post API Handlers ----------!
	
	
	
	// !---------- Page functions ----------!
	
	
	// createPageString - 
	// Create list of pages for search results
	public String createPageString(int numberOfItems, int itemsPerPage, int currentPage, String baseUrl)
	{	
		StringBuffer pageString = new StringBuffer();
		
		// Calculate the total number of pages
		int totalPages = 1;
		if (numberOfItems > itemsPerPage)
		{
			double pages = Math.ceil(numberOfItems / (double) itemsPerPage);
			totalPages = (int)Math.ceil( pages );
		}

		//
		if (totalPages > 1)
		{
			for (int i = 1; i <= totalPages; i++)
			{
				if (i == currentPage) 
					{ pageString.append(i); }
				else 
					{ pageString.append("<a href=\"" + baseUrl + i + "\" title=\"" + i + "\">" + i + "</a>"); }
				
				if (i != totalPages) pageString.append(" ");
			}
		}
		else
		{
			pageString.append("1");
		}
		
		return pageString.toString();
	}

	
	
	
	
	// !---------- Misc. Shared Strings ----------!

	public String basicNewSource = "" +	
		"{\n" +
    	"	\"title\": \"Basic RSS Source Template\",\n" +
    	"	\"description\": \"Create a description of your source here.\",\n" +
    	"	\"url\": \"http://blahblahblah.com/blah.rss\",\n" +
    	"	\"communityIds\": [\"4c927585d591d31d7b37097a\"],\n" +
    	"	\"extractType\": \"Feed\",\n" +
    	"	\"harvestBadSource\": false,\n" +
    	"	\"isApproved\": true,\n" +
    	"	\"isPublic\": true,\n" +
    	"	\"mediaType\": \"Social\",\n" +
    	"   \"useExtractor\": \"none\",\n" +
    	"	\"tags\": [\n" +
    	"		\"tag1\",\n" +
    	"		\"tag2\"\n" +
	    "]\n" +
		"}";

%>
