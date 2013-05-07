package org.apache.jsp;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;
import java.io.*;
import java.util.*;
import java.net.*;
import javax.naming.*;
import javax.servlet.jsp.*;
import javax.script.ScriptException;
import javax.script.ScriptEngineManager;
import javax.script.ScriptEngine;
import org.apache.commons.io.*;
import com.google.gson.Gson;
import org.json.JSONObject;
import org.json.JSONArray;
import org.bson.types.ObjectId;
import infinit.e.web.util.Utils;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public final class people_jsp extends org.apache.jasper.runtime.HttpJspBase
    implements org.apache.jasper.runtime.JspSourceDependent {


	// !----------  ----------!
	String API_ROOT = null;
	Boolean localCookie = false;
		
	// !----------  ----------!
	String messageToDisplay = "";
	

	
	// !---------- Start login/session handling code ----------!
	static CookieManager cm = new CookieManager();
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
		if (localCookie) CookieHandler.setDefault(cm);
		
		try 
		{
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			URL url = new URL(API_ROOT + addr);
			URLConnection urlConnection = url.openConnection();
			String cookieVal = getBrowserInfiniteCookie(request);
        	if (cookieVal != null)
        	{
        		urlConnection.addRequestProperty("Cookie","infinitecookie=" + cookieVal);
        		urlConnection.setDoInput(true);
        		urlConnection.setDoOutput(true);
        		urlConnection.setRequestProperty("Accept-Charset","UTF-8");
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
	private String postToRestfulApi(String addr, String data, HttpServletRequest request, HttpServletResponse response) 
	{
		if(localCookie)
			CookieHandler.setDefault(cm);
		String result = "";
	    try
		{
	    	URLConnection connection = new URL(API_ROOT + addr).openConnection();
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
	private String logOut(HttpServletRequest request, HttpServletResponse response) 
	{
		return callRestfulApi("auth/logout/", request, response);
	} // TESTED
	
	
	// getShare -
	private String getShare(String id, HttpServletRequest request, HttpServletResponse response) 
	{
		return callRestfulApi("social/share/get/" + id + "/", request, response);
	} // TESTED
	
	
	// getShareObject - 
	private JSONObject getShareObject(String id, HttpServletRequest request, HttpServletResponse response) 
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
		return callRestfulApi("config/source/user/", request, response);
	} // TESTED
	
	
	// getSystemCommunity - 
	private String getSystemCommunity(HttpServletRequest request, HttpServletResponse response) 
	{
		return callRestfulApi("social/community/getsystem/", request, response);
	}
	
	// getAllCommunities - 
	private String getAllCommunities(HttpServletRequest request, HttpServletResponse response) 
	{
		return callRestfulApi("social/community/getall/", request, response);
	}
	
	
	// getPublicCommunities - 
	private String getPublicCommunities(HttpServletRequest request, HttpServletResponse response) 
	{
		return callRestfulApi("social/community/getpublic/", request, response);
	}
	
	
	// getCommunity - 
	private String getCommunity(String id, HttpServletRequest request, HttpServletResponse response) 
	{
		return callRestfulApi("social/community/get/" + id, request, response);
	}
	
	// getCommunity - 
	private String removeCommunity(String id, HttpServletRequest request, HttpServletResponse response) 
	{
		return callRestfulApi("social/community/remove/" + id, request, response);
	}
	
	
	// updateCommunityMemberStatus - 
	private String updateCommunityMemberStatus(String communityid, String personid, String status,
			HttpServletRequest request, HttpServletResponse response) 
	{
		return callRestfulApi("social/community/member/update/status/" + communityid + "/" 
			+ personid + "/" + status, request, response);
	}
	
	
	// updateCommunityMemberType - 
	private String updateCommunityMemberType(String communityid, String personid, String type,
			HttpServletRequest request, HttpServletResponse response) 
	{
		return callRestfulApi("social/community/member/update/type/" + communityid + "/" 
			+ personid + "/" + type, request, response);
	}
	
	
	// getSource - 
	private String getSource(String sourceId, HttpServletRequest request, HttpServletResponse response) 
	{
		return callRestfulApi("config/source/get/" + sourceId, request, response);
	}
	
	
	// deleteSource
	private String deleteSource(String sourceId, boolean bDocsOnly, String communityId, HttpServletRequest request, HttpServletResponse response)
	{
		if (bDocsOnly) {
			return callRestfulApi("config/source/delete/docs/" + sourceId + "/" + communityId, request, response);
		}
		else {
			return callRestfulApi("config/source/delete/" + sourceId + "/" + communityId, request, response);
		}
	}
	
	
	
	// getListOfAllShares - 
	private Map<String,String> getListOfAllShares(HttpServletRequest request, HttpServletResponse response)
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
	private Map<String,String> getListOfSharesByType(String type, HttpServletRequest request, HttpServletResponse response)
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
		return callRestfulApi("social/share/search/?type=source,source_published",request, response);
	} // TESTED
	
	
	// getUserSources - 
	private Map<String,String> getUserSourcesAndShares(HttpServletRequest request, HttpServletResponse response)
	{
		Map<String,String> userSources = new HashMap<String,String>();
		String userIdStr = null;
		
		// publishedSources - array of source._ids of published sources
		ArrayList<String> publishedSources = new ArrayList<String>();
		try
		{
			JSONObject personObj = new JSONObject ( getPerson(request, response) );
			if (personObj.has("data"))
			{
				JSONObject person = new JSONObject ( personObj.getString("data") );
				userIdStr = person.getString("_id");
			}
			
			// Get the user's shares from social.share where type = source or source_published
			String tempJson = getSourceShares(request, response);
			
			// Covert to JSONObject
			JSONObject json = new JSONObject(tempJson);
			JSONObject json_response = json.getJSONObject("response");
			if (json_response.getString("success").equalsIgnoreCase("true")) 
			{
				if (json.has("data")) 
				{
					// Iterate over share objects and write to our collection
					JSONArray data = json.getJSONArray("data");
					for (int i = 0; i < data.length(); i++) 
					{
						JSONObject shareObj = data.getJSONObject(i);
						String tempTitle = shareObj.getString("title");
						
						JSONObject sourceObj = new JSONObject( shareObj.getString("share") );
						if (sourceObj.has("_id")) publishedSources.add( sourceObj.getString("_id") );
						if (sourceObj.has("ownerId") && !sourceObj.getString("ownerId").equalsIgnoreCase(userIdStr)) tempTitle += " (+)";
						tempTitle += " (*)";
						
						userSources.put(tempTitle, shareObj.getString("_id"));
					}
				}
			}
			
			// Get sources that the user owns from ingest.source
 			tempJson = getUserSources(request, response);
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
							// Only add the source to our list if it isn't already in our
							if (!publishedSources.contains( sourceObj.getString("_id") ))
							{
								String tempTitle = sourceObj.getString("title");
								if (sourceObj.has("ownerId") && !sourceObj.getString("ownerId").equalsIgnoreCase(userIdStr)) tempTitle += " (+)";
								userSources.put(tempTitle, sourceObj.getString("_id"));
							}
						}
					}
				}
 			}
		}
		catch (Exception e)
		{
			System.out.println(e.getMessage());
		}
		return userSources;
	}
	
	
	// getListOfAllPeople
	private Map<String,String> getListOfAllPeople(HttpServletRequest request, HttpServletResponse response)
	{
		Map<String,String> allPeople = new HashMap<String,String>();
		try
		{
			JSONObject communityObj = new JSONObject ( getSystemCommunity(request, response) );
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
			System.out.println(e.getMessage());
		}
		return allPeople;
	}
	
	
	// getListOfCommunityMembers
	private Map<String,String> getListOfCommunityMembers(String id, HttpServletRequest request, HttpServletResponse response)
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
	private Map<String,String> getListOfAllCommunities(HttpServletRequest request, HttpServletResponse response)
	{
		Map<String,String> allCommunities = new HashMap<String,String>();
		try
		{
			JSONObject communitiesObj = new JSONObject ( getAllCommunities(request, response) );
			if ( communitiesObj.has("data") )
			{
				JSONArray communities = communitiesObj.getJSONArray("data");
				for (int i = 0; i < communities.length(); i++) 
				{
					JSONObject community = communities.getJSONObject(i);
					allCommunities.put( community.getString("name"), community.getString("_id") );
				}
			}
		}
		catch (Exception e)
		{
			//System.out.println(e.getMessage());
		}
		return allCommunities;
	}
	
	
	// getListOfAllCommunities
	private Map<String,String> getListOfAllNonPersonalCommunities(HttpServletRequest request, HttpServletResponse response)
	{
		Map<String,String> allCommunities = new HashMap<String,String>();
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
						allCommunities.put( community.getString("name"), community.getString("_id") );
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
	private String getPerson(HttpServletRequest request, HttpServletResponse response) 
	{
		return callRestfulApi("social/person/get/", request, response);
	} // TESTED
	
	
	// getPerson -
	private String getPerson(String id, HttpServletRequest request, HttpServletResponse response) 
	{
		return callRestfulApi("social/person/get/" + id, request, response);
	}
	
	
	// getPersonCommunities -
	private JSONArray getPersonCommunities(HttpServletRequest request, HttpServletResponse response)
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
	private String deletePerson(String id, HttpServletRequest request, HttpServletResponse response)
	{
		return callRestfulApi("social/person/delete/" + id, request, response);
	} // TESTED 
	
	
	// updatePassword
	private String updatePassword(String id, String password, HttpServletRequest request, HttpServletResponse response)
	{
		return callRestfulApi("social/person/update/password/" + id + "/" + password, request, response);
	} // TESTED 
	
	
	// addToCommunity
	private String addToCommunity(String community, String person, HttpServletRequest request, HttpServletResponse response)
	{
		return callRestfulApi("social/community/member/invite/" + community + "/" + person + "?skipinvitation=true", request, response);
	} // TESTED 
	
	// removeFromCommunity
	private String removeFromCommunity(String community, String person, HttpServletRequest request, HttpServletResponse response)
	{
		return callRestfulApi("social/community/member/update/status/" + community + "/" + person + "/remove", request, response);
	} // TESTED
	
	
	
	
	// !---------- End Get/Post API Handlers ----------!
	
	
	
	// !---------- Page functions ----------!
	
	
	// createPageString - 
	// Create list of pages for search results
	private String createPageString(int numberOfItems, int itemsPerPage, int currentPage, String baseUrl)
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



	// 
	int currentPage = 1;
	int itemsToShowPerPage = 10;
	
	//
	String action = "";
	String logoutAction = "";
	
	// 
	String listFilter = "";

	//
	String editTableTitle = "Register New User Account";
		
	//
	String personid = "";
	String visiblePersonId = "";
	String firstName = "";
	String lastName = "";
	String displayName = "";
	String accountStatus = "";
	String apiKey = "";
	String phone = "";
	String email = "";
	String oldemail = "";
	String accounttype = "";
	String listOfCommunities = "";
	String password = "";
	String passwordConfirmation = "";
	// No way of knowing if I'm admin, so leave as visible for now, later change to:
	//String accountTypeHidden = "style=\"display:none;\"";
	String accountTypeHidden = "";
	
		



// validateFormFields
private boolean validateFormFields()
{
	boolean isValid = true;
	ArrayList<String> al = new ArrayList<String>();
	if (firstName.length() < 1) al.add("First Name");
	if (lastName.length() < 1) al.add("Last Name");
	if (email.length() < 1) al.add("Email");
	if (al.size() > 0)
	{
		isValid = false;
		messageToDisplay = "Error, the following required fields are missing: " + al.toString();
	}
	return isValid;
}  // TESTED


//validateFormFields
private boolean validatePassword()
{
	boolean isValid = true;
	ArrayList<String> al = new ArrayList<String>();
	if (password.length() < 6) al.add("Password must be greater than 5 characters in length");
	if (!password.equals(passwordConfirmation)) al.add("The Password and Password Confirmation fields must match");
	if (al.size() > 0)
	{
		isValid = false;
		messageToDisplay = "Error: " + al.toString();
		//System.out.print(messageToDisplay);
	}
	return isValid;
}  // TESTED



// savePerson -
private boolean savePerson( boolean isNewAccount, HttpServletRequest request, HttpServletResponse response )
{
	try
	{
		String newPassword = "";
		if (password.length() > 0 || isNewAccount)
		{
			if (validatePassword())
			{
				newPassword = ", \"password\" : \"" + password + "\"";
			}
			else
			{
				return false;
			}
		}
		String apiKeyJson = "";
		if (!apiKey.equals("")) {
			apiKeyJson = ", \"apiKey\" : \""+apiKey+"\"";
		}
		
		String accountType = "";
		if (!accounttype.equalsIgnoreCase("unknown")) {
			accountType = ", \"accountType\" : \""+accounttype+"\"";
		}
		else if (isNewAccount) { accountType = ", \"accountType\" : \"user\""; }
		
		if (null == phone) {
			phone = "";
		}
		String userJson = "" +
		"{" + 
		"    \"user\": " +
		"        {" +
		"            \"firstname\" : \"" + firstName + "\", " +
		"            \"lastname\" : \"" + lastName + "\", " +
		"            \"displayName\" : \"" + displayName + "\", " +
		"            \"phone\" : \"" + phone + "\", " +
		"            \"email\" : [ \"" + email + "\" ] " +
		"        }," +
		"    \"auth\" : { \"username\" : \"" + oldemail + "\" " + accountType + newPassword + apiKeyJson + " } " +
		"}";
		
		JSONObject actionResponse = null;
		if (isNewAccount)
		{
			actionResponse = new JSONObject(postToRestfulApi("social/person/register", userJson, request, response));
			JSONObject dataVal = new JSONObject(actionResponse.getString("data"));
			personid = dataVal.getString("_id");
		}
		else
		{
			actionResponse = new JSONObject(postToRestfulApi("social/person/update", userJson, request, response));
		}
		JSONObject responseVal = new JSONObject(actionResponse.getString("response"));
		
		if (responseVal.getString("success").equalsIgnoreCase("true"))
		{
			messageToDisplay = "Success: User account information saved."; return true;
		}
		else
		{
			messageToDisplay = "Error: Unable to save the user's account information.";
			return false;
		}
	}
	catch (Exception e)
	{
		messageToDisplay = "Error: Unable to the save user's account information. (" + e.getMessage() + ")"; return false;
	}
} // TESTED


// updateAccountPassword -
private boolean updateAccountPassword( HttpServletRequest request, HttpServletResponse response )
{
	try
	{
		JSONObject updateResponse = new JSONObject ( new JSONObject ( updatePassword(email, Utils.encrypt(password), request, response) ).getString("response") );
		if (updateResponse.getString("success").equalsIgnoreCase("true"))
		{
			messageToDisplay = "Success: Password updated."; return true;
		}
		else
		{
			messageToDisplay = "Error: Unable to update password."; return false;
		}
	}
	catch (Exception e)
	{
		messageToDisplay = "Error: Unable to update password. (" + e.getMessage() + ")"; return false;
	}
}  // TESTED


//deleteAccount -
private boolean deleteAccount( String id, HttpServletRequest request, HttpServletResponse response )
{
	try
	{
		JSONObject updateResponse = new JSONObject ( new JSONObject ( deletePerson(id, request, response) ).getString("response") );
		if (updateResponse.getString("success").equalsIgnoreCase("true"))
		{
			messageToDisplay = "Success: Account Deleted."; return true;
		}
		else
		{
			messageToDisplay = "Error: Unable to Delete Account."; return false;
		}
	}
	catch (Exception e)
	{
		messageToDisplay = "Error: Unable to Delete Account. (" + e.getMessage() + ")"; return false;
	}
}  // TESTED



// addPersonToCommunity
private boolean addPersonToCommunity(String person, String community, HttpServletRequest request, HttpServletResponse response)
{
	try
	{
		JSONObject updateResponse = new JSONObject ( new JSONObject ( addToCommunity(community, person, request, response) ).getString("response") );
		if (updateResponse.getString("success").equalsIgnoreCase("true"))
		{
			messageToDisplay = "Success: Person added to community."; return true;
		}
		else
		{
			messageToDisplay = "Error: Unable to add person to community."; return false;
		}
	}
	catch (Exception e)
	{
		messageToDisplay = "Error: Unable to add person to community. (" + e.getMessage() + ")"; return false;
	}
}  // TESTED


// removePersonFromCommunity
private boolean removePersonFromCommunity(String person, String community, HttpServletRequest request, HttpServletResponse response)
{
	try
	{
		JSONObject updateResponse = new JSONObject ( new JSONObject ( removeFromCommunity(community, person, request, response) ).getString("response") );
		if (updateResponse.getString("success").equalsIgnoreCase("true"))
		{
			messageToDisplay = "Success: Person removed from community."; return true;
		}
		else
		{
			messageToDisplay = "Error: Unable to remove person from community."; return false;
		}
	}
	catch (Exception e)
	{
		messageToDisplay = "Error: Unable to remove person from community. (" + e.getMessage() + ")"; return false;
	}
}





// populateEditForm - 
private void populateEditForm(String id, HttpServletRequest request, HttpServletResponse response) 
{
	clearForm();
	if (id != null && id != "") 
	{
		try 
		{
			editTableTitle = "Edit User Account";
			
			// Get person from API
			JSONObject personResponse = new JSONObject( getPerson(id, request, response) );
			if (personResponse.has("data")) { // (otherwise trying to get someone else's info but aren't admin...)
				JSONObject person = personResponse.getJSONObject("data");
				String status = person.getString("accountStatus").substring(0,1).toUpperCase() + person.getString("accountStatus").substring(1);
				accountStatus =  status;
				visiblePersonId = id;
				firstName = person.getString("firstName");
				lastName = person.getString("lastName");
				displayName = person.getString("displayName");
				email = person.getString("email");
				if (person.has("phone")) phone = person.getString("phone");
				
				// Output user communities
				JSONArray communities = person.getJSONArray("communities");
				listOfCommunities = getListOfCommunities(communities, request, response);
			}
		} 
		catch (Exception e) 
		{
			System.out.println(e.getMessage());
		}
	}
}  // TESTED


// getListOfCommunities -
// Retrieve a list of all communities in the system and display with the following conditions:
// 1. Do not display the system or personal communities
// 2. If user is a member have option to remove user from the community
// 3. If user is not a member have the option to sign them up (users should not be able to sign them selves up
//	  unless the community supports self registration.)
private String getListOfCommunities(JSONArray memberOf, HttpServletRequest request, HttpServletResponse response)
{	
	StringBuffer communityList = new StringBuffer();
	communityList.append("<table width=\"100%\">");
	try
	{
		// Create an array list of communities the user is a member of
		List<String> memberOfList = new ArrayList<String>();
		for (int i = 0; i < memberOf.length(); i++)
		{
			JSONObject c = memberOf.getJSONObject(i);
			memberOfList.add(c.getString("_id"));
		}
		
		// Get a list of all communities from the api
		JSONObject communitiesObject = new JSONObject( getAllCommunities(request, response) );
		JSONArray communities = communitiesObject.getJSONArray("data");
		
		// Create an array list of all community names so we can sort them correctly for display
		List<String> listOfCommunityNames = new ArrayList<String>();
		for (int i = 0; i < communities.length(); i++)
		{
			JSONObject c = communities.getJSONObject(i);
			if (c.has("name"))
			{
				listOfCommunityNames.add(c.getString("name"));
			}
		}
		Collections.sort( listOfCommunityNames, String.CASE_INSENSITIVE_ORDER );
		
		int column = 1;
		
		for (String communityName : listOfCommunityNames)
		{
			// Iterate over the list of all communities
			for (int i = 0; i < communities.length(); i++)
			{
				JSONObject community = communities.getJSONObject(i);
				if (community.has("name") && community.getString("name").equalsIgnoreCase(communityName.toLowerCase()))
				{
					// Only show the non-system, non-personal communities
					if (community.getString("isPersonalCommunity").equalsIgnoreCase("false") && community.getString("isSystemCommunity").equalsIgnoreCase("false"))
					{
						if (column == 1) { communityList.append("<tr valign=\"middle\">"); }
						communityList.append("<td width=\"5%\">");
						
						String listFilterString = "";
						if (listFilter.length() > 0) listFilterString = "&listFilterStr="+ listFilter;
						String pageString = "";
						if (currentPage > 1) pageString = "&page=" + currentPage;
						
						String deleteLink = "<a href=\"people.jsp?action=edit&personid=" + personid
								+ pageString + listFilterString + "&removefrom=" + community.getString("_id") 
								+ "\" title=\"Remove User from Community\" "
								+ "onclick='return confirm(\"Do you really wish to remove the user account from: "
								+ community.getString("name") + "?\");'><img src=\"image/minus_button.png\" border=0></a>";
								
						String addLink = "<a href=\"people.jsp?action=edit&personid=" + personid
								+ pageString + listFilterString + "&addto=" + community.getString("_id") 
								+ "\" title=\"Add User to Community\"><img src=\"image/plus_button.png\" border=0></a>";
						
						String divOne = "";
						String divTwo = "";
						if (memberOfList.contains(community.getString("_id")))
						{
							communityList.append(deleteLink);
						}
						else
						{
							communityList.append(addLink);
							divOne = "<div class=\"notAMemberOfCommunity\">";
							divTwo = "</div>";
						}
						
						communityList.append("</td>");
						communityList.append("<td width=\"45%\">");
						communityList.append(divOne + community.getString("name") + divTwo);
						communityList.append("</td>");
						if (column == 1) { column = 2; }
						else
						{
							communityList.append("</tr>");	
							column = 1;
						}
					}
				}
				
			}	
		}
	}
	catch (Exception e)
	{
		System.out.println(e.getMessage());
	}
	communityList.append("</table>");
	return communityList.toString();
}



// clearForm
private void clearForm()
{
	editTableTitle = "Add New User Account";
	visiblePersonId = "";
	accountStatus =  "";
	firstName = "";
	lastName = "";
	displayName = "";
	email = "";
	oldemail = "";
	phone = "";
	password = "";
	passwordConfirmation = "";
	listOfCommunities = "";
}  // TESTED


// listItems -
private String listItems(HttpServletRequest request, HttpServletResponse response)
{
	StringBuffer people = new StringBuffer();
	Map<String, String> listOfPeople = getListOfAllPeople(request, response);
	
	if (listOfPeople.size() > 0)
	{
		people.append("<table class=\"listTable\" cellpadding=\"3\" cellspacing=\"1\" width=\"100%\" >");

		// Sort the sources alphabetically
		List<String> sortedKeys = new ArrayList<String>(listOfPeople.keySet());
		Collections.sort( sortedKeys, String.CASE_INSENSITIVE_ORDER );
		
		// Filter the list
		List<String> sortedAndFilteredKeys = new ArrayList<String>();
		for (String key : sortedKeys)
		{
			if ( listFilter.length() > 0 )
			{
				if ( key.toLowerCase().contains( listFilter.toLowerCase() ) ) sortedAndFilteredKeys.add( key );
			}
			else
			{
				sortedAndFilteredKeys.add( key );
			}
		}
		
		// If the user has filtered the list down we might need to adjust our page calculations
		// e.g. 20 total items might = 2 pages but filtered down to 5 items there would only be 1
		// Calculate first item to start with with
		// Page = 1, item = 1
		// Page = X, item = ( ( currentPage - 1 ) * itemsToShowPerPage ) + 1;
		int startItem = 1;
		int endItem = startItem + itemsToShowPerPage - 1;
		if (currentPage > 1)
		{
			startItem = ( ( currentPage - 1 ) * itemsToShowPerPage ) + 1;
			endItem = ( startItem + itemsToShowPerPage ) - 1;
		}

		int currentItem = 1;
		for (String key : sortedAndFilteredKeys)
		{
			if (currentItem >= startItem && currentItem <= endItem)
			{
				String name = key;
				String id = listOfPeople.get(key).toString();
				String editLink = "";
				String deleteLink = "";
	
				String listFilterString = "";
				if (listFilter.length() > 0) listFilterString = "&listFilterStr="+ listFilter;
				
				editLink = "<a href=\"people.jsp?action=edit&personid=" + id + "&page=" + currentPage 
						+ listFilterString + "\" title=\"Edit User Account\">" + name + "</a>";
				deleteLink = "<a href=\"people.jsp?action=delete&personid=" + id
						+ listFilterString + "\" title=\"Delete User Account\" "
						+ "onclick='return confirm(\"Do you really wish to delete the user account for: "
						+ name + "?\");'><img src=\"image/delete_x_button.png\" border=0></a>";
	
				// Create the HTML table row
				people.append("<tr>");
				people.append("<td bgcolor=\"white\" width=\"100%\">" + editLink + "</td>");
				people.append("<td align=\"center\" bgcolor=\"white\">" + deleteLink + "</td>");
				people.append("</tr>");
			}
			currentItem++;
		}
		
		// Calculate number of pages, current page, page links...
		people.append("<tr><td colspan=\"2\" align=\"center\" class=\"subTableFooter\">");
		// --------------------------------------------------------------------------------
		// Create base URL for each page
		StringBuffer baseUrl = new StringBuffer();
		baseUrl.append("people.jsp?");
		String actionString = (action.length() > 0) ? "action=" + action : "";
		String personIdString = (personid.length() > 0) ? "personid=" + personid : "";
		if (actionString.length() > 0) baseUrl.append(actionString);
		if (actionString.length() > 0 && personIdString.length() > 0) baseUrl.append("&");
		if (personIdString.length() > 0) baseUrl.append(personIdString);
		if (actionString.length() > 0 || personIdString.length() > 0) baseUrl.append("&");
		baseUrl.append("page=");
		people.append( createPageString( sortedAndFilteredKeys.size(), itemsToShowPerPage, currentPage, baseUrl.toString() ));
		people.append("</td></tr>");
		// --------------------------------------------------------------------------------
		people.append("</table>");
	}
	else
	{
		people.append("No user accounts were retrieved");
	}

	return people.toString();
}




  private static final JspFactory _jspxFactory = JspFactory.getDefaultFactory();

  private static java.util.List _jspx_dependants;

  static {
    _jspx_dependants = new java.util.ArrayList(4);
    _jspx_dependants.add("/inc/sharedFunctions.jsp");
    _jspx_dependants.add("/inc/header.jsp");
    _jspx_dependants.add("/inc/login_form.jsp");
    _jspx_dependants.add("/inc/footer.jsp");
  }

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
    ServletContext application = null;
    ServletConfig config = null;
    JspWriter out = null;
    Object page = this;
    JspWriter _jspx_out = null;
    PageContext _jspx_page_context = null;


    try {
      response.setContentType("text/html; charset=ISO-8859-1");
      pageContext = _jspxFactory.getPageContext(this, request, response,
      			null, false, 8192, true);
      _jspx_page_context = pageContext;
      application = pageContext.getServletContext();
      config = pageContext.getServletConfig();
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
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write('\n');
      out.write('\n');
      out.write('\n');


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
		}
		
		if (API_ROOT.contains("localhost")) { localCookie=true; }
		else { localCookie=false; }
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


      out.write("\n");
      out.write("\n");
      out.write("\t\n");
      out.write('\n');
      out.write('\n');
      out.write('\n');
      out.write('\n');
      out.write('\n');

	messageToDisplay = "";
	
	// 
	if (isLoggedIn) 
	{	
		// Capture value in the left handed table filter field
		if (request.getParameter("listFilter") != null) 
		{
			listFilter = request.getParameter("listFilter");
		}
		else if (request.getParameter("listFilterStr") != null) 
		{
			listFilter = request.getParameter("listFilterStr");
		}
		else
		{
			listFilter = "";
		}
		
		// Determine which action to perform on postback/request
		action = "";
		if (request.getParameter("action") != null) action = request.getParameter("action").toLowerCase();
		if (request.getParameter("dispatchAction") != null) action = request.getParameter("dispatchAction").toLowerCase();
		if (request.getParameter("clearForm") != null) action = request.getParameter("clearForm").toLowerCase();
		if (request.getParameter("filterList") != null) action = request.getParameter("filterList").toLowerCase();
		if (request.getParameter("clearFilter") != null) action = request.getParameter("clearFilter").toLowerCase();
		if (request.getParameter("logoutButton") != null) action = request.getParameter("logoutButton").toLowerCase();
		
		// Capture values sent by button clicks, these will override the action value as appropriate 
		String saveAccount = "";
		String createAccount = "";
		String updatePassword = "";
		String addto = "";
		String removefrom = "";
		if (request.getParameter("createAccount") != null) createAccount = request.getParameter("createAccount").toLowerCase();
		if (request.getParameter("saveAccount") != null) saveAccount = request.getParameter("saveAccount").toLowerCase();
		if (request.getParameter("updatePassword") != null) updatePassword = request.getParameter("updatePassword").toLowerCase();
		if (request.getParameter("addto") != null) addto = request.getParameter("addto");
		if (request.getParameter("removefrom") != null) removefrom = request.getParameter("removefrom");
		
		// Capture input for page value if passed to handle the page selected in the left hand list of items
		if (request.getParameter("page") != null) 
		{
			currentPage = Integer.parseInt( request.getParameter("page").toLowerCase() );
		}
		else
		{
			currentPage = 1;
		}
		
		try
		{
			// Always clear the form first so there is no bleed over of values from previous requests
			clearForm();

			// Read in values from the edit form
			String visiblePersonId = (request.getParameter("visiblePersonId") != null) ? request.getParameter("visiblePersonId") : "";
			personid = (request.getParameter("personid") != null) ? request.getParameter("personid") : "";
			firstName = (request.getParameter("firstName") != null) ? request.getParameter("firstName") : "";
			lastName = (request.getParameter("lastName") != null) ? request.getParameter("lastName") : "";
			displayName = (request.getParameter("displayName") != null) ? request.getParameter("displayName") : "";
			phone = (request.getParameter("phone") != null) ? request.getParameter("phone") : "";
			email = (request.getParameter("email") != null) ? request.getParameter("email") : "";
			oldemail = (request.getParameter("oldemail") != null) ? request.getParameter("oldemail") : "";
			passwordConfirmation = (request.getParameter("passwordConfirmation") != null) ? request.getParameter("passwordConfirmation") : "";
			password = (request.getParameter("password") != null) ? request.getParameter("password") : "";
			accounttype = (request.getParameter("accounttype") != null) ? request.getParameter("accounttype") : "";
			apiKey = (request.getParameter("apiKey") != null) ? request.getParameter("apiKey") : "";
			
			Boolean redirect = false;
			
			// If user has clicked save, create, or update buttons do those actions before handling the action param
			if (saveAccount.equals("saveaccount") || saveAccount.equals("save user account")) 
			{
				if ( validateFormFields() )
				{
					savePerson(false, request, response);
				}
			}
			if (createAccount.equals("createaccount") || createAccount.equals("create user account")) 
			{
				if ( validateFormFields() && validatePassword() )
				{
					savePerson(true, request, response);
					redirect = true;
				}
			}
			if (updatePassword.equals("updatepassword") || updatePassword.equals("update password")) 
			{
				if ( validatePassword() )
				{
					updateAccountPassword(request, response);
				}
			}
			if (addto.length() > 0)
			{
				addPersonToCommunity(personid, addto, request, response);
				redirect = true;
			}
			if (removefrom.length() > 0)
			{
				removePersonFromCommunity(personid, removefrom, request, response);
				redirect = true;
			}
			
			// if redirect == true refresh the page to update the edit form's content to reflect changes
			if (redirect) 
			{
				String urlParams = "";
				if (listFilter.length() > 0) urlParams = "&listFilterStr="+ listFilter;
				if (currentPage > 1) urlParams += "&page=" + currentPage;
				out.println("<meta http-equiv=\"refresh\" content=\"0;url=people.jsp?action=edit&personid=" 
					+ personid + urlParams + "\">");
			}
			
			if (action.equals("new user")) 
			{
				out.println("<meta http-equiv=\"refresh\" content=\"0;url=people.jsp\">");
			}
			else if (action.equals("edit")) 
			{
				populateEditForm(personid, request, response);
			}
			else if (action.equals("delete")) 
			{
				deleteAccount(personid, request, response);
				out.println("<meta http-equiv=\"refresh\" content=\"0;url=people.jsp\">");
			}
			else if (action.equals("filterlist")) 
			{
				currentPage = 1;
				populateEditForm(personid, request, response);
			}
			else if (action.equals("clear")) 
			{
				listFilter = "";
				populateEditForm(personid, request, response);
			}
			else if (action.equals("logout")) 
			{
				logOut(request, response);
				out.println("<meta http-equiv=\"refresh\" content=\"0;url=index.jsp\">");
			}
		}
		catch (Exception e)
		{
			//System.out.println(e.getMessage());
		}
	}
	

      out.write("\n");
      out.write("\n");
      out.write("<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">\n");
      out.write("<html>\n");
      out.write("<head>\n");
      out.write("\t<meta http-equiv=\"Content-Type\" content=\"text/html; charset=ISO-8859-1\">\n");
      out.write("\t<link rel=\"stylesheet\" type=\"text/css\" href=\"inc/manager.css\" />\n");
      out.write("\t<script type=\"text/javascript\" src=\"inc/utilities.js\"></script>\n");
      out.write("\t<link rel=\"shortcut icon\" href=\"image/favicon.ico\" />\n");
      out.write("\t<title>Infinit.e.Manager - People</title>\n");
      out.write("</head>\n");
      out.write("<body>\n");
      out.write("\n");

	// !-- Create JavaScript Popup --
	if (messageToDisplay.length() > 0) { 

      out.write("\n");
      out.write("\t<script language=\"javascript\" type=\"text/javascript\">\n");
      out.write("\t\talert('");
      out.print(messageToDisplay );
      out.write("');\n");
      out.write("\t</script>\n");
 
	} 

      out.write("\n");
      out.write("\n");
      out.write("\t<form method=\"post\">\n");
      out.write("\t\n");
      out.write("<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" >\n");
      out.write("<tr valign=\"middle\">\n");
      out.write("\t<td width=\"100%\" background=\"image/infinite_logo_bg.png\">\n");
      out.write("\t\t<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" >\n");
      out.write("\t\t\t<tr valign=\"bottom\">\n");
      out.write("\t\t\t\t<td width=\"200\"><a href=\"index.jsp\"><img src=\"image/infinite_logo.png\" border=\"0\"></a></td>\n");
      out.write("\t\t\t\t<td>\n");
      out.write("\t\t\t\t\t<a href=\"people.jsp\" class=\"headerLink\" title=\"Add/Edit Users\">People</a> &nbsp; &nbsp;\n");
      out.write("\t\t\t\t\t<a href=\"communities.jsp\" class=\"headerLink\" title=\"Add/Edit Communities\">Communities</a> &nbsp; &nbsp;\n");
      out.write("\t\t\t\t\t<a href=\"sources.jsp\" class=\"headerLink\" title=\"Add/Edit Sources\">Sources</a> &nbsp; &nbsp;\n");
      out.write("\t\t\t\t\t<!-- <a href=\"widgets.jsp\" class=\"headerLink\" title=\"Add/Edit Widgets\">Widgets</a> &nbsp; &nbsp; -->\n");
      out.write("\t\t\t\t\t<!-- <a href=\"hadoop.jsp\" class=\"headerLink\" title=\"Add/Edit Hadoop Jars\">Hadoop</a> &nbsp; &nbsp; -->\n");
      out.write("\t\t\t\t\t<!-- <a href=\"shares.jsp\" class=\"headerLink\" title=\"Add/Edit Shares\">Shares</a> &nbsp; &nbsp; -->\n");
      out.write("\t\t\t\t\t<a href=\"index.jsp\" class=\"headerLink\" title=\"Home\">Home</a> &nbsp; &nbsp;\n");
      out.write("\t\t\t\t\t<a href=\"?action=logout\" class=\"headerLink\" title=\"Logout\">Logout</a>\n");
      out.write("\t\t\t\t</td>\n");
      out.write("\t\t\t\t<td align=\"right\" width=\"120\" background=\"image/ikanow_logo_smaller_bg.png\"></td>\n");
      out.write("\t\t\t</tr>\n");
      out.write("\t\t</table>\n");
      out.write("\t</td>\n");
      out.write("</tr>\n");
      out.write("<tr>\n");
      out.write("\t<td bgcolor=\"#ffffff\">\n");
      out.write('\n');
      out.write('\n');

	if (!isLoggedIn) 
	{

      out.write('\n');
      out.write('	');
      out.write('	');
      out.write("<!-- Begin login_form.jsp  -->\n");
      out.write("\n");
      out.write("<br />\n");
      out.write("<br />\n");
      out.write("<br />\n");
      out.write("<br />\n");
      out.write("<center>\n");
      out.write("<form method=\"post\" name=\"login_form\">\n");
      out.write("<table class=\"standardTable\" cellpadding=\"5\" cellspacing=\"1\" width=\"35%\" >\n");
      out.write("\t<tr>\n");
      out.write("\t\t<td colspan=\"2\" align=\"center\">\n");
      out.write("\t\t\t<font color=\"white\"><b>Login to Infinit.e.Manager</b></font>\n");
      out.write("\t\t</td>\n");
      out.write("\t</tr>\n");
      out.write("\t<tr>\n");
      out.write("\t\t<td bgcolor=\"white\" width=\"40%\">User Name:</td>\n");
      out.write("\t\t<td bgcolor=\"white\" width=\"60%\"><input type=\"text\" name=\"username\" size=\"40\"></td>\n");
      out.write("\t</tr>\n");
      out.write("\t<tr>\n");
      out.write("\t\t<td bgcolor=\"white\" width=\"40%\">Password:</td>\n");
      out.write("\t\t<td bgcolor=\"white\" width=\"60%\"><input type=\"password\" name=\"password\" size=\"40\"></td>\n");
      out.write("\t</tr>\n");
      out.write("\t<tr>\n");
      out.write("\t\t<td colspan=\"2\" align=\"right\"><input type=\"submit\"></td>\n");
      out.write("\t</tr>\n");
      out.write("</table>\n");
      out.write("</form>\n");
      out.write("</center>\n");
      out.write("<br />\n");
      out.write("<br />\n");
      out.write("<br />\n");
      out.write("<br />\n");
      out.write("<!-- End login_form.jsp  -->");
      out.write('\n');

	}
	else
	{

      out.write("\n");
      out.write("\t\n");
      out.write("\t<table class=\"standardTable\" cellpadding=\"5\" cellspacing=\"0\" width=\"100%\">\n");
      out.write("\t<tr valign=\"top\">\n");
      out.write("\t\t<td width=\"30%\" bgcolor=\"#ffffff\">\n");
      out.write("\t\t\n");
      out.write("\t\t\t<table class=\"standardTable\" cellpadding=\"5\" cellspacing=\"1\" width=\"100%\">\n");
      out.write("\t\t\t<tr>\n");
      out.write("\t\t\t\t<td class=\"headerLink\">People</td>\n");
      out.write("\t\t\t\t<td align=\"right\"><input type=\"text\" id=\"listFilter\" \n");
      out.write("\t\t\t\t\tonkeydown=\"if (event.keyCode == 13) { setDipatchAction('filterList'); \n");
      out.write("\t\t\t\t\tdocument.getElementById('filterList').click(); }\" \n");
      out.write("\t\t\t\t\tname=\"listFilter\" size=\"20\" value=\"");
      out.print(listFilter );
      out.write("\"/><input name=\"filterList\" type=\"submit\"\n");
      out.write("\t\t\t\t\tvalue=\"Filter\"/><input name=\"clearFilter\" value=\"Clear\" type=\"submit\"/></td>\n");
      out.write("\t\t\t</tr>\n");
      out.write("\t\t\t<tr>\n");
      out.write("\t\t\t\t<td colspan=\"2\" bgcolor=\"white\">");
      out.print(listItems(request, response) );
      out.write("</td>\n");
      out.write("\t\t\t</tr>\n");
      out.write("\t\t\t</table>\n");
      out.write("\n");
      out.write("\t\t</td>\n");
      out.write("\t\t\n");
      out.write("\t\t<td width=\"70%\" bgcolor=\"#ffffff\">\n");
      out.write("\t\t\n");
      out.write("\t\t\t<table class=\"standardTable\" cellpadding=\"5\" cellspacing=\"1\" width=\"100%\">\n");
      out.write("\t\t\t<tr>\n");
      out.write("\t\t\t\t<td class=\"headerLink\">");
      out.print(editTableTitle );
      out.write("</td>\n");
      out.write("\t\t\t\t<td align=\"right\"><input name=\"clearForm\" id=\"clearForm\" value=\"New User\" type=\"submit\"/></td>\n");
      out.write("\t\t\t</tr>\n");
      out.write("\t\t\t<tr>\n");
      out.write("\t\t\t\t<td colspan=\"2\" bgcolor=\"white\">\n");
      out.write("\t\t\t\t\t<table class=\"standardSubTable\" cellpadding=\"5\" cellspacing=\"1\" width=\"100%\">\n");
      out.write("\t\t\t\t\t<tr>\n");
      out.write("\t\t\t\t\t\t<td bgcolor=\"#ffffff\" width=\"30%\">Account Status:</td>\n");
      out.write("\t\t\t\t\t\t<td bgcolor=\"#ffffff\" width=\"70%\">");
      out.print(accountStatus );
      out.write("</td>\n");
      out.write("\t\t\t\t\t</tr>\n");
      out.write("\t\t\t\t\t<tr>\n");
      out.write("\t\t\t\t\t\t<td bgcolor=\"#ffffff\" width=\"30%\">Account Type (Admin Only):</td>\n");
      out.write("\t\t\t\t\t\t<td bgcolor=\"#ffffff\" width=\"70%\">\n");
      out.write("\t\t\t\t\t\t\t<select name=\"accounttype\" id=\"accounttype\" ");
      out.print(accountTypeHidden );
      out.write(">\n");
      out.write("\t\t\t\t\t\t\t\t<option value=\"Unknown\">Unknown</option>\n");
      out.write("\t\t\t\t\t\t\t\t<option value=\"admin\">Admin</option>\n");
      out.write("\t\t\t\t\t\t\t\t<option value=\"user\">User</option>\n");
      out.write("\t\t\t\t\t\t\t</select>\n");
      out.write("\t\t\t\t\t\t</td>\t\t\t\t\t\t\t\n");
      out.write("\t\t\t\t\t</tr>\n");
      out.write("\t\t\t\t\t<tr>\n");
      out.write("\t\t\t\t\t\t<td bgcolor=\"#ffffff\" width=\"30%\">Account Id:</td>\n");
      out.write("\t\t\t\t\t\t<td bgcolor=\"#ffffff\" width=\"70%\"><input type=\"text\" readonly id=\"personid\" name=\"personid\" value=\"");
      out.print(visiblePersonId );
      out.write("\" size=\"50\" /></td>\n");
      out.write("\t\t\t\t\t</tr>\n");
      out.write("\t\t\t\t\t<tr>\n");
      out.write("\t\t\t\t\t\t<td bgcolor=\"#ffffff\" width=\"30%\">First Name:*</td>\n");
      out.write("\t\t\t\t\t\t<td bgcolor=\"#ffffff\" width=\"70%\"><input type=\"text\" id=\"firstName\" name=\"firstName\" value=\"");
      out.print(firstName);
      out.write("\" size=\"50\" /></td>\n");
      out.write("\t\t\t\t\t</tr>\n");
      out.write("\t\t\t\t\t<tr>\n");
      out.write("\t\t\t\t\t\t<td bgcolor=\"#ffffff\" width=\"30%\">Last Name:*</td>\n");
      out.write("\t\t\t\t\t\t<td bgcolor=\"#ffffff\" width=\"70%\"><input type=\"text\" id=\"lastName\" name=\"lastName\" value=\"");
      out.print(lastName);
      out.write("\" size=\"50\" /></td>\n");
      out.write("\t\t\t\t\t</tr>\n");
      out.write("\t\t\t\t\t<tr>\n");
      out.write("\t\t\t\t\t\t<td bgcolor=\"#ffffff\" width=\"30%\">Display Name:*</td>\n");
      out.write("\t\t\t\t\t\t<td bgcolor=\"#ffffff\" width=\"70%\"><input type=\"text\" readonly id=\"displayName\" name=\"displayName\" value=\"");
      out.print(displayName);
      out.write("\" size=\"50\" /></td>\n");
      out.write("\t\t\t\t\t</tr>\n");
      out.write("\t\t\t\t\t<tr>\n");
      out.write("\t\t\t\t\t\t<td bgcolor=\"#ffffff\" width=\"30%\">Phone Number:*</td>\n");
      out.write("\t\t\t\t\t\t<td bgcolor=\"#ffffff\" width=\"70%\"><input type=\"text\" id=\"phone\" name=\"phone\" value=\"");
      out.print(phone);
      out.write("\" size=\"30\" /></td>\n");
      out.write("\t\t\t\t\t</tr>\n");
      out.write("\t\t\t\t\t<tr>\n");
      out.write("\t\t\t\t\t\t<td bgcolor=\"#ffffff\" width=\"30%\">Email Address (User Name):</td>\n");
      out.write("\t\t\t\t\t\t<td bgcolor=\"#ffffff\" width=\"70%\"><input type=\"text\" id=\"email\" name=\"email\" value=\"");
      out.print(email);
      out.write("\" size=\"75\" /></td>\n");
      out.write("\t\t\t\t\t</tr>\n");
      out.write("\t\t\t\t\t<tr>\n");
      out.write("\t\t\t\t\t\t<td bgcolor=\"#ffffff\" width=\"30%\">Password:</td>\n");
      out.write("\t\t\t\t\t\t<td bgcolor=\"#ffffff\" width=\"70%\"><input type=\"password\" id=\"password\" name=\"password\" value=\"");
      out.print(password);
      out.write("\" size=\"20\" /></td>\n");
      out.write("\t\t\t\t\t</tr>\n");
      out.write("\t\t\t\t\t<tr>\n");
      out.write("\t\t\t\t\t\t<td bgcolor=\"#ffffff\" width=\"30%\">Password Confirmation:</td>\n");
      out.write("\t\t\t\t\t\t<td bgcolor=\"#ffffff\" width=\"70%\"><input type=\"password\" id=\"passwordConfirmation\" name=\"passwordConfirmation\" value=\"");
      out.print(passwordConfirmation);
      out.write("\" size=\"20\" /></td>\n");
      out.write("\t\t\t\t\t</tr>\n");
      out.write("\t\t\t\t\t<tr>\n");
      out.write("\t\t\t\t\t\t<td bgcolor=\"#ffffff\" width=\"30%\">API key:</td>\n");
      out.write("\t\t\t\t\t\t<td bgcolor=\"#ffffff\" width=\"70%\"><input type=\"password\" id=\"apiKey\" name=\"apiKey\" value=\"\" size=\"20\" /></td>\n");
      out.write("\t\t\t\t\t</tr>\n");
      out.write("\t\t\t\t\t<tr valign=\"top\">\n");
      out.write("\t\t\t\t\t\t<td bgcolor=\"#ffffff\" width=\"30%\">Communities:</td>\n");
      out.write("\t\t\t\t\t\t<td bgcolor=\"#ffffff\" width=\"70%\">");
      out.print(listOfCommunities );
      out.write("</td>\n");
      out.write("\t\t\t\t\t</tr>\n");
      out.write("\t\t\t\t\t<tr>\n");
      out.write("\t\t\t\t\t\t<td bgcolor=\"#ffffff\" width=\"30%\"></td>\n");
      out.write("\t\t\t\t\t\t<td bgcolor=\"#ffffff\" width=\"70%\">\n");

	if (personid.length() > 0) {

      out.write("\n");
      out.write("\t\t\t\t\t\t\t<input name=\"saveAccount\" id=\"saveAccount\" value=\"Save User Account\" type=\"submit\"\n");
      out.write("\t\t\t\t\t\t\t\t\tonclick=\"if (confirm('Are you sure you want to change these account details?'))  return true; return false;\"\n");
      out.write("\t\t\t\t\t\t\t/>\n");
      out.write("\t\t\t\t\t\t\t\n");
      out.write("\t\t\t\t\t\t\t<input name=\"updatePassword\" value=\"Update Password\" type=\"submit\"\n");
      out.write("\t\t\t\t\t\t\t\t\tonclick=\"if (confirm('Are you sure you want to update this password?'))  return true; return false;\"\n");
      out.write("\t\t\t\t\t\t\t/>\n");

	}
	else
	{

      out.write("\n");
      out.write("\t\t\t\t\t\t\t<input name=\"createAccount\" value=\"Create User Account\" type=\"submit\"\n");
      out.write("\t\t\t\t\t\t\t\t\tonclick=\"if (confirm('Are you sure you want to create this user account?'))  return true; return false;\"\n");
      out.write("\t\t\t\t\t\t\t/>\t\n");

	}

      out.write("\n");
      out.write("\t\t\t\t\t\t</td>\n");
      out.write("\t\t\t\t\t</tr>\n");
      out.write("\t\t\t\t\t</table>\n");
      out.write("\t\t\t\t\t\n");
      out.write("\t\t\t\t</td>\n");
      out.write("\t\t\t</tr>\n");
      out.write("\t\t\t</table>\n");
      out.write("\t\t\n");
      out.write("\t\t</td>\n");
      out.write("\t\t\n");
      out.write("\t<tr>\n");
      out.write("\t</table>\n");
      out.write("\t<input type=\"hidden\" name=\"oldemail\" id=\"oldemail\" value=\"");
      out.print(email);
      out.write("\"/>\n");
      out.write("\t</form>\n");

	}

      out.write('\n');
      out.write('\n');
      out.write("\t\n");
      out.write("\t</td>\n");
      out.write("<tr>\n");
      out.write("<tr>\n");
      out.write("\t<td align=\"right\" bgcolor=\"#000000\">\n");
      out.write("\t\t&nbsp;\n");
      out.write("\t\t<!-- <a href=\"http://www.ikanow.com\" title=\"www.ikanow.com\"><img src=\"image/ikanow_logo_small.png\" border=\"0\"></a> -->\n");
      out.write("\t</td>\n");
      out.write("</tr>\n");
      out.write("</table>\n");
      out.write("\n");
      out.write("</body>\n");
      out.write("</html>\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write('\n');
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
