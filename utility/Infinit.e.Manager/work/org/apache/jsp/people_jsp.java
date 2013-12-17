package org.apache.jsp;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;
import java.io.*;
import java.util.*;
import com.google.common.collect.TreeMultimap;
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
import org.apache.commons.lang.StringEscapeUtils.*;

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
		if (localCookie) CookieHandler.setDefault(cm);
		
		try 
		{
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			URL url = new URL(API_ROOT + addr);
			URLConnection urlConnection = url.openConnection();
    		urlConnection.addRequestProperty("X-Forwarded-For", request.getRemoteAddr());
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
	public String postToRestfulApi(String addr, String data, HttpServletRequest request, HttpServletResponse response) 
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
	
	
	// getUserSources - 
	public TreeMultimap<String,String> getUserSourcesAndShares(HttpServletRequest request, HttpServletResponse response, String filter)
	{
		TreeMultimap<String,String> userSources = TreeMultimap.create();
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
						if ( ( filter.length() > 0 ) && !tempTitle.toLowerCase().contains( filter.toLowerCase() ) )
						{
							continue;
						}
						
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
								if ( ( filter.length() > 0 ) && !tempTitle.toLowerCase().contains( filter.toLowerCase() ) )
								{
									continue;
								}
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



	// 
	int currentPage = 1; 
	int itemsToShowPerPage = 10;
	
	//
	String action = "";
	String lastaction = "";
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
	String accountTypeHidden = "style=\"display:none;\"";
	String isRequestAdminVisible = "style=\"display:none;\"";
	String isDemoteAdminVisible = "style=\"display:none;\"";
		



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
			messageToDisplay = "Error: Unable to save the user's account information. (" + responseVal.getString("message") + ")"; 
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
			// Don't output a message, the visual feedback is sufficient
			//messageToDisplay = "Success: Person added to community."; 
			return true;
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
			// Don't output a message, the visual feedback is sufficient
			//messageToDisplay = "Success: Person removed from community."; 
			return true;
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
		
		String lastName = null;
		for (String communityName : listOfCommunityNames)
		{
			if ((null != lastName) && lastName.equals(communityName)) {
				continue;
			}
			lastName = communityName;
			
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
								+ "\" title=\"Remove User from Community\" >"
								+ "<img src=\"image/minus_button.png\" border=0></a>";
								
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
				deleteLink = "<a href=\"people.jsp?action=delete&personid=" + id+ "&page=" + currentPage
						+ listFilterString + "\" title=\"Delete User Account\" "
						+ "onclick='return confirm(\"Do you really wish to delete the user account for: "
						+ name + "?\");'><img src=\"image/delete_x_button.png\" border=0></a>";
	
				// Create the HTML table row
				people.append("<tr>");
				people.append("<td bgcolor=\"white\" width=\"100%\">" + editLink + "</td>");
				people.append("<td align=\"center\" bgcolor=\"white\"><input type=\"checkbox\" name=\"peopleToDelete\" value=\"" + id + "\"/></td>");
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
		if (listFilter.length() > 0) baseUrl.append("listFilterStr=").append(listFilter).append('&');
		
		String actionString = (lastaction.equals("edit")) ? "action=" + lastaction : "";
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

  private org.apache.jasper.runtime.TagHandlerPool _005fjspx_005ftagPool_005fc_005fset_0026_005fvar_005fvalue_005fnobody;
  private org.apache.jasper.runtime.TagHandlerPool _005fjspx_005ftagPool_005ffmt_005fsetLocale_0026_005fvalue_005fnobody;
  private org.apache.jasper.runtime.TagHandlerPool _005fjspx_005ftagPool_005ffmt_005fsetBundle_0026_005fbasename_005fnobody;
  private org.apache.jasper.runtime.TagHandlerPool _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody;

  private javax.el.ExpressionFactory _el_expressionfactory;
  private org.apache.AnnotationProcessor _jsp_annotationprocessor;

  public Object getDependants() {
    return _jspx_dependants;
  }

  public void _jspInit() {
    _005fjspx_005ftagPool_005fc_005fset_0026_005fvar_005fvalue_005fnobody = org.apache.jasper.runtime.TagHandlerPool.getTagHandlerPool(getServletConfig());
    _005fjspx_005ftagPool_005ffmt_005fsetLocale_0026_005fvalue_005fnobody = org.apache.jasper.runtime.TagHandlerPool.getTagHandlerPool(getServletConfig());
    _005fjspx_005ftagPool_005ffmt_005fsetBundle_0026_005fbasename_005fnobody = org.apache.jasper.runtime.TagHandlerPool.getTagHandlerPool(getServletConfig());
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody = org.apache.jasper.runtime.TagHandlerPool.getTagHandlerPool(getServletConfig());
    _el_expressionfactory = _jspxFactory.getJspApplicationContext(getServletConfig().getServletContext()).getExpressionFactory();
    _jsp_annotationprocessor = (org.apache.AnnotationProcessor) getServletConfig().getServletContext().getAttribute(org.apache.AnnotationProcessor.class.getName());
  }

  public void _jspDestroy() {
    _005fjspx_005ftagPool_005fc_005fset_0026_005fvar_005fvalue_005fnobody.release();
    _005fjspx_005ftagPool_005ffmt_005fsetLocale_0026_005fvalue_005fnobody.release();
    _005fjspx_005ftagPool_005ffmt_005fsetBundle_0026_005fbasename_005fnobody.release();
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.release();
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

      out.write("<!--\r\n");
      out.write("Copyright 2012 The Infinit.e Open Source Project\r\n");
      out.write("\r\n");
      out.write("Licensed under the Apache License, Version 2.0 (the \"License\");\r\n");
      out.write("you may not use this file except in compliance with the License.\r\n");
      out.write("You may obtain a copy of the License at\r\n");
      out.write("\r\n");
      out.write("  http://www.apache.org/licenses/LICENSE-2.0\r\n");
      out.write("\r\n");
      out.write("Unless required by applicable law or agreed to in writing, software\r\n");
      out.write("distributed under the License is distributed on an \"AS IS\" BASIS,\r\n");
      out.write("WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\r\n");
      out.write("See the License for the specific language governing permissions and\r\n");
      out.write("limitations under the License.\r\n");
      out.write("-->\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("<!--\r\n");
      out.write("Copyright 2012 The Infinit.e Open Source Project\r\n");
      out.write("\r\n");
      out.write("Licensed under the Apache License, Version 2.0 (the \"License\");\r\n");
      out.write("you may not use this file except in compliance with the License.\r\n");
      out.write("You may obtain a copy of the License at\r\n");
      out.write("\r\n");
      out.write("  http://www.apache.org/licenses/LICENSE-2.0\r\n");
      out.write("\r\n");
      out.write("Unless required by applicable law or agreed to in writing, software\r\n");
      out.write("distributed under the License is distributed on an \"AS IS\" BASIS,\r\n");
      out.write("WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\r\n");
      out.write("See the License for the specific language governing permissions and\r\n");
      out.write("limitations under the License.\r\n");
      out.write("-->\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      if (_jspx_meth_c_005fset_005f0(_jspx_page_context))
        return;
      out.write('\r');
      out.write('\n');
      if (_jspx_meth_fmt_005fsetLocale_005f0(_jspx_page_context))
        return;
      out.write('\r');
      out.write('\n');
      if (_jspx_meth_fmt_005fsetBundle_005f0(_jspx_page_context))
        return;
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");


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
			//API_ROOT = "http://localhost:8080/api/";
			API_ROOT = "http://localhost:8184/";
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


      out.write("\r\n");
      out.write("\r\n");
      out.write("\t\r\n");
      out.write('\r');
      out.write('\n');
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");

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
		lastaction = action;
		if (request.getParameter("dispatchAction") != null) action = request.getParameter("dispatchAction").toLowerCase();
		if (request.getParameter("clearForm") != null) action = request.getParameter("clearForm").toLowerCase();
		if (request.getParameter("filterList") != null) action = request.getParameter("filterList").toLowerCase();
		if (request.getParameter("clearFilter") != null) action = request.getParameter("clearFilter").toLowerCase();
		if (request.getParameter("logoutButton") != null) action = request.getParameter("logoutButton").toLowerCase();
		if (request.getParameter("deleteSelected") != null) action = request.getParameter("deleteSelected").toLowerCase();
		
		// Capture values sent by button clicks, these will override the action value as appropriate 
		String saveAccount = "";
		String createAccount = "";
		String updatePassword = "";
		String addto = "";
		String removefrom = "";
		boolean bRequestAdmin = false;
		boolean bDemoteAdmin = false;
		if (request.getParameter("createAccount") != null) createAccount = request.getParameter("createAccount").toLowerCase();
		if (request.getParameter("saveAccount") != null) saveAccount = request.getParameter("saveAccount").toLowerCase();
		if (request.getParameter("updatePassword") != null) updatePassword = request.getParameter("updatePassword").toLowerCase();
		if (request.getParameter("addto") != null) addto = request.getParameter("addto");
		if (request.getParameter("removefrom") != null) removefrom = request.getParameter("removefrom");
		if (request.getParameter("requestAdmin") != null) bRequestAdmin = true;
		if (request.getParameter("demoteAdmin") != null) bDemoteAdmin = true;

		// Capture input for page value if passed to handle the page selected in the left hand list of items
		if (request.getParameter("page") != null) 
		{
			currentPage = Integer.parseInt( request.getParameter("page").toLowerCase() );
		}
		else
		{
			currentPage = 1;
		}
		
		Boolean bIsAdmin = null;
		
		if (bDemoteAdmin) {
			adminLogOut(request, response);
		}
		bIsAdmin = isLoggedInAsAdmin_GetAdmin(bRequestAdmin, request, response);
		if (null == bIsAdmin) { //inactive admin
			accountTypeHidden = "style=\"display:none;\"";
			isRequestAdminVisible = "";			
			isDemoteAdminVisible = "style=\"display:none;\"";
		}
		else if (bIsAdmin) { // admin active
			accountTypeHidden = "";
			isRequestAdminVisible = "style=\"display:none;\"";	
			isDemoteAdminVisible = "";
		}
		else { // not admin
			accountTypeHidden = "style=\"display:none;\"";
			isRequestAdminVisible = "style=\"display:none;\"";
			isDemoteAdminVisible = "style=\"display:none;\"";
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
				out.print("<meta http-equiv=\"refresh\" content=\"0;url=people.jsp?page=" + currentPage);
				if (listFilter.length() > 0) {
					out.print("&listFilterStr=" + listFilter);					
				}
				out.println("\">");
			}
			else if (action.equals("deleteselected")) 
			{
				String[] ids= request.getParameterValues("peopleToDelete");
				
				int nDeleted = 0;
				int nFailed = 0;
				for (String id: ids) {
					if (!deleteAccount(id, request, response)) {
						nFailed++;
					}
					else nDeleted++;
				}
				messageToDisplay = "Bulk person deletion: deleted " + nDeleted + ", failed: " + nFailed; 
				
				out.print("<meta http-equiv=\"refresh\" content=\"0;url=people.jsp?page=" + currentPage);
				if (listFilter.length() > 0) {
					out.print("&listFilterStr=" + listFilter);					
				}
				out.println("\">");
			}
			else if (action.equals("filterlist")) 
			{
				currentPage = 1; // (don't perpetuate this action across page jumps)
				populateEditForm(personid, request, response);
			}
			else if (action.equals("clear")) 
			{
				currentPage = 1; // (don't perpetuate this action across page jumps)
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
	

      out.write("\r\n");
      out.write("\r\n");
      out.write("<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">\r\n");
      out.write("<html>\r\n");
      out.write("<head>\r\n");
      out.write("\t<meta http-equiv=\"Content-Type\" content=\"text/html; charset=ISO-8859-1\">\r\n");
      out.write("\t<link rel=\"stylesheet\" type=\"text/css\" href=\"inc/manager.css\" />\r\n");
      out.write("\t<script type=\"text/javascript\" src=\"inc/utilities.js\"></script>\r\n");
      out.write("\t<link rel=\"shortcut icon\" href=\"image/favicon.ico\" />\r\n");
      out.write("\t<title>Infinit.e.Manager - People</title>\r\n");
      out.write("</head>\r\n");
      out.write("<body>\r\n");
      out.write("\r\n");

	// !-- Create JavaScript Popup --
	if (messageToDisplay.length() > 0) { 

      out.write("\r\n");
      out.write("\t<script language=\"javascript\" type=\"text/javascript\">\r\n");
      out.write("\t\talert(\"");
      out.print(messageToDisplay.replace('"', '\'') );
      out.write("\");\r\n");
      out.write("\t</script>\r\n");
 
	} 

      out.write("\r\n");
      out.write("\r\n");
      out.write("\t<form method=\"post\">\r\n");
      out.write("\t\r\n");
      out.write("<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" >\r\n");
      out.write("<tr valign=\"middle\">\r\n");
      out.write("\t<td width=\"100%\" background=\"image/infinite_logo_bg.png\">\r\n");
      out.write("\t\t<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" >\r\n");
      out.write("\t\t\t<tr valign=\"bottom\">\r\n");
      out.write("\t\t\t\t<td width=\"200\"><a href=\"index.jsp\"><img src=\"image/infinite_logo.png\" border=\"0\"></a></td>\r\n");
      out.write("\t\t\t\t<td>\r\n");
      out.write("\t\t\t\t\t<a href=\"people.jsp\" class=\"headerLink\" title=\"");
      if (_jspx_meth_fmt_005fmessage_005f0(_jspx_page_context))
        return;
      out.write('"');
      out.write('>');
      if (_jspx_meth_fmt_005fmessage_005f1(_jspx_page_context))
        return;
      out.write("</a> &nbsp; &nbsp;\r\n");
      out.write("\t\t\t\t\t<a href=\"communities.jsp\" class=\"headerLink\" title=\"");
      if (_jspx_meth_fmt_005fmessage_005f2(_jspx_page_context))
        return;
      out.write('"');
      out.write('>');
      if (_jspx_meth_fmt_005fmessage_005f3(_jspx_page_context))
        return;
      out.write("</a> &nbsp; &nbsp;\r\n");
      out.write("\t\t\t\t\t<a href=\"sources.jsp\" class=\"headerLink\" title=\"");
      if (_jspx_meth_fmt_005fmessage_005f4(_jspx_page_context))
        return;
      out.write('"');
      out.write('>');
      if (_jspx_meth_fmt_005fmessage_005f5(_jspx_page_context))
        return;
      out.write("</a> &nbsp; &nbsp;\r\n");
      out.write("\t\t\t\t\t<a href=\"index.jsp\" class=\"headerLink\" title=\"");
      if (_jspx_meth_fmt_005fmessage_005f6(_jspx_page_context))
        return;
      out.write('"');
      out.write('>');
      if (_jspx_meth_fmt_005fmessage_005f7(_jspx_page_context))
        return;
      out.write("</a> &nbsp; &nbsp;\r\n");
      out.write("\t\t\t\t\t<a href=\"?action=logout\" class=\"headerLink\" title=\"");
      if (_jspx_meth_fmt_005fmessage_005f8(_jspx_page_context))
        return;
      out.write('"');
      out.write('>');
      if (_jspx_meth_fmt_005fmessage_005f9(_jspx_page_context))
        return;
      out.write("</a>\r\n");
      out.write("\t\t\t\t</td>\r\n");
      out.write("\t\t\t\t<td align=\"right\" width=\"120\" background=\"image/ikanow_logo_smaller_bg.png\"></td>\r\n");
      out.write("\t\t\t</tr>\r\n");
      out.write("\t\t</table>\r\n");
      out.write("\t</td>\r\n");
      out.write("</tr>\r\n");
      out.write("<tr>\r\n");
      out.write("\t<td bgcolor=\"#ffffff\">\r\n");
      out.write("\r\n");
      out.write("\r\n");

	if (!isLoggedIn) 
	{

      out.write("\r\n");
      out.write("\t\t");
      out.write("<!-- Begin login_form.jsp  -->\r\n");
      out.write("\r\n");
      out.write("<br />\r\n");
      out.write("<br />\r\n");
      out.write("<br />\r\n");
      out.write("<br />\r\n");
      out.write("<center>\r\n");
      out.write("<form method=\"post\" name=\"login_form\">\r\n");
      out.write("<table class=\"standardTable\" cellpadding=\"5\" cellspacing=\"1\" width=\"35%\" >\r\n");
      out.write("\t<tr>\r\n");
      out.write("\t\t<td colspan=\"2\" align=\"center\">\r\n");
      out.write("\t\t\t<font color=\"white\"><b>Login to Infinit.e.Manager</b></font>\r\n");
      out.write("\t\t</td>\r\n");
      out.write("\t</tr>\r\n");
      out.write("\t<tr>\r\n");
      out.write("\t\t<td bgcolor=\"white\" width=\"40%\">User Name:</td>\r\n");
      out.write("\t\t<td bgcolor=\"white\" width=\"60%\"><input type=\"text\" name=\"username\" size=\"40\"></td>\r\n");
      out.write("\t</tr>\r\n");
      out.write("\t<tr>\r\n");
      out.write("\t\t<td bgcolor=\"white\" width=\"40%\">Password:</td>\r\n");
      out.write("\t\t<td bgcolor=\"white\" width=\"60%\"><input type=\"password\" name=\"password\" size=\"40\"></td>\r\n");
      out.write("\t</tr>\r\n");
      out.write("\t<tr>\r\n");
      out.write("\t\t<td colspan=\"2\" align=\"right\"><input type=\"submit\"></td>\r\n");
      out.write("\t</tr>\r\n");
      out.write("</table>\r\n");
      out.write("</form>\r\n");
      out.write("</center>\r\n");
      out.write("<br />\r\n");
      out.write("<br />\r\n");
      out.write("<br />\r\n");
      out.write("<br />\r\n");
      out.write("<!-- End login_form.jsp  -->");
      out.write('\r');
      out.write('\n');

	}
	else
	{

      out.write("\r\n");
      out.write("\t\r\n");
      out.write("\t<table class=\"standardTable\" cellpadding=\"5\" cellspacing=\"0\" width=\"100%\">\r\n");
      out.write("\t<tr valign=\"top\">\r\n");
      out.write("\t\t<td width=\"30%\" bgcolor=\"#ffffff\">\r\n");
      out.write("\t\t\r\n");
      out.write("\t\t\t<table class=\"standardTable\" cellpadding=\"5\" cellspacing=\"1\" width=\"100%\">\r\n");
      out.write("\t\t\t<tr>\r\n");
      out.write("\t\t\t\t<td class=\"headerLink\">People</td>\r\n");
      out.write("\t\t\t\t<td align=\"right\"><input type=\"text\" id=\"listFilter\" \r\n");
      out.write("\t\t\t\t\tonkeydown=\"if (event.keyCode == 13) { setDipatchAction('filterList'); \r\n");
      out.write("\t\t\t\t\tdocument.getElementById('filterList').click(); }\" \r\n");
      out.write("\t\t\t\t\tname=\"listFilter\" size=\"20\" value=\"");
      out.print(listFilter );
      out.write("\"/><input name=\"filterList\" type=\"submit\"\r\n");
      out.write("\t\t\t\t\tvalue=\"Filter\"/><input name=\"clearFilter\" value=\"Clear\" type=\"submit\"/></td>\r\n");
      out.write("\t\t\t</tr>\r\n");
      out.write("\t\t\t<tr>\r\n");
      out.write("\t\t\t\t<td colspan=\"2\" bgcolor=\"white\">");
      out.print(listItems(request, response) );
      out.write("</td>\r\n");
      out.write("\t\t\t</tr>\r\n");
      out.write("\t\t\t<tr>\r\n");
      out.write("\t\t\t\t<td colspan=\"2\" ><button name=\"deleteSelected\" onclick=\"return confirm('Do you really wish to delete the selected people?');\" name=\"deleteSelected\" value=\"deleteSelected\">Delete selected people</button></td>\r\n");
      out.write("\t\t\t</tr>\r\n");
      out.write("\t\t\t<tr ");
      out.print( isRequestAdminVisible );
      out.write(">\r\n");
      out.write("\t\t\t\t<td colspan=\"2\" ><button name=\"requestAdmin\" name=\"requestAdmin\" value=\"requestAdmin\">Grab temp admin rights</button></td>\r\n");
      out.write("\t\t\t</tr>\r\n");
      out.write("\t\t\t<tr  ");
      out.print( isDemoteAdminVisible );
      out.write(" >\r\n");
      out.write("\t\t\t\t<td colspan=\"2\" ><button name=\"demoteAdmin\" name=\"demoteAdmin\" value=\"demoteAdmin\">Relinquish temp admin rights</button></td>\r\n");
      out.write("\t\t\t</tr>\t\t\t\r\n");
      out.write("\t\t\t</table>\r\n");
      out.write("\r\n");
      out.write("\t\t</td>\r\n");
      out.write("\t\t\r\n");
      out.write("\t\t<td width=\"70%\" bgcolor=\"#ffffff\">\r\n");
      out.write("\t\t\r\n");
      out.write("\t\t\t<table class=\"standardTable\" cellpadding=\"5\" cellspacing=\"1\" width=\"100%\">\r\n");
      out.write("\t\t\t<tr>\r\n");
      out.write("\t\t\t\t<td class=\"headerLink\">");
      out.print(editTableTitle );
      out.write("</td>\r\n");
      out.write("\t\t\t\t<td align=\"right\"><input name=\"clearForm\" id=\"clearForm\" value=\"New User\" type=\"submit\"/></td>\r\n");
      out.write("\t\t\t</tr>\r\n");
      out.write("\t\t\t<tr>\r\n");
      out.write("\t\t\t\t<td colspan=\"2\" bgcolor=\"white\">\r\n");
      out.write("\t\t\t\t\t<table class=\"standardSubTable\" cellpadding=\"5\" cellspacing=\"1\" width=\"100%\">\r\n");
      out.write("\t\t\t\t\t<tr>\r\n");
      out.write("\t\t\t\t\t\t<td bgcolor=\"#ffffff\" width=\"30%\">Account Status:</td>\r\n");
      out.write("\t\t\t\t\t\t<td bgcolor=\"#ffffff\" width=\"70%\">");
      out.print(accountStatus );
      out.write("</td>\r\n");
      out.write("\t\t\t\t\t</tr>\r\n");
      out.write("\t\t\t\t\t<tr ");
      out.print(accountTypeHidden );
      out.write(">\r\n");
      out.write("\t\t\t\t\t\t<td bgcolor=\"#ffffff\" width=\"30%\">Account Type (Admin Only):</td>\r\n");
      out.write("\t\t\t\t\t\t<td bgcolor=\"#ffffff\" width=\"70%\">\r\n");
      out.write("\t\t\t\t\t\t\t<select name=\"accounttype\" id=\"accounttype\">\r\n");
      out.write("\t\t\t\t\t\t\t\t<option value=\"Unknown\">Unknown</option>\r\n");
      out.write("\t\t\t\t\t\t\t\t<option value=\"admin\">Admin</option>\r\n");
      out.write("\t\t\t\t\t\t\t\t<option value=\"admin-enabled\">Admin-On-Request</option>\r\n");
      out.write("\t\t\t\t\t\t\t\t<option value=\"user\">User</option>\r\n");
      out.write("\t\t\t\t\t\t\t</select>\r\n");
      out.write("\t\t\t\t\t\t</td>\t\t\t\t\t\t\t\r\n");
      out.write("\t\t\t\t\t</tr>\r\n");
      out.write("\t\t\t\t\t<tr>\r\n");
      out.write("\t\t\t\t\t\t<td bgcolor=\"#ffffff\" width=\"30%\">Account Id:</td>\r\n");
      out.write("\t\t\t\t\t\t<td bgcolor=\"#ffffff\" width=\"70%\"><input type=\"text\" readonly id=\"personid\" name=\"personid\" value=\"");
      out.print(visiblePersonId );
      out.write("\" size=\"50\" /></td>\r\n");
      out.write("\t\t\t\t\t</tr>\r\n");
      out.write("\t\t\t\t\t<tr>\r\n");
      out.write("\t\t\t\t\t\t<td bgcolor=\"#ffffff\" width=\"30%\">First Name:*</td>\r\n");
      out.write("\t\t\t\t\t\t<td bgcolor=\"#ffffff\" width=\"70%\"><input type=\"text\" id=\"firstName\" name=\"firstName\" value=\"");
      out.print(firstName);
      out.write("\" size=\"50\" /></td>\r\n");
      out.write("\t\t\t\t\t</tr>\r\n");
      out.write("\t\t\t\t\t<tr>\r\n");
      out.write("\t\t\t\t\t\t<td bgcolor=\"#ffffff\" width=\"30%\">Last Name:*</td>\r\n");
      out.write("\t\t\t\t\t\t<td bgcolor=\"#ffffff\" width=\"70%\"><input type=\"text\" id=\"lastName\" name=\"lastName\" value=\"");
      out.print(lastName);
      out.write("\" size=\"50\" /></td>\r\n");
      out.write("\t\t\t\t\t</tr>\r\n");
      out.write("\t\t\t\t\t<tr>\r\n");
      out.write("\t\t\t\t\t\t<td bgcolor=\"#ffffff\" width=\"30%\">Display Name:</td>\r\n");
      out.write("\t\t\t\t\t\t<td bgcolor=\"#ffffff\" width=\"70%\"><input type=\"text\" readonly id=\"displayName\" name=\"displayName\" value=\"");
      out.print(displayName);
      out.write("\" size=\"50\" /></td>\r\n");
      out.write("\t\t\t\t\t</tr>\r\n");
      out.write("\t\t\t\t\t<tr>\r\n");
      out.write("\t\t\t\t\t\t<td bgcolor=\"#ffffff\" width=\"30%\">Phone Number:</td>\r\n");
      out.write("\t\t\t\t\t\t<td bgcolor=\"#ffffff\" width=\"70%\"><input type=\"text\" id=\"phone\" name=\"phone\" value=\"");
      out.print(phone);
      out.write("\" size=\"30\" /></td>\r\n");
      out.write("\t\t\t\t\t</tr>\r\n");
      out.write("\t\t\t\t\t<tr>\r\n");
      out.write("\t\t\t\t\t\t<td bgcolor=\"#ffffff\" width=\"30%\">Email Address (User Name):</td>\r\n");
      out.write("\t\t\t\t\t\t<td bgcolor=\"#ffffff\" width=\"70%\"><input type=\"text\" id=\"email\" name=\"email\" value=\"");
      out.print(email);
      out.write("\" size=\"75\" /></td>\r\n");
      out.write("\t\t\t\t\t</tr>\r\n");
      out.write("\t\t\t\t\t<tr>\r\n");
      out.write("\t\t\t\t\t\t<td bgcolor=\"#ffffff\" width=\"30%\">Password:</td>\r\n");
      out.write("\t\t\t\t\t\t<td bgcolor=\"#ffffff\" width=\"70%\"><input type=\"password\" id=\"password\" name=\"password\" autocomplete=\"off\" value=\"");
      out.print(password);
      out.write("\" size=\"20\" /></td>\r\n");
      out.write("\t\t\t\t\t</tr>\r\n");
      out.write("\t\t\t\t\t<tr>\r\n");
      out.write("\t\t\t\t\t\t<td bgcolor=\"#ffffff\" width=\"30%\">Password Confirmation:</td>\r\n");
      out.write("\t\t\t\t\t\t<td bgcolor=\"#ffffff\" width=\"70%\"><input type=\"password\" id=\"passwordConfirmation\" name=\"passwordConfirmation\" value=\"");
      out.print(passwordConfirmation);
      out.write("\" size=\"20\" /></td>\r\n");
      out.write("\t\t\t\t\t</tr>\r\n");
      out.write("\t\t\t\t\t<tr>\r\n");
      out.write("\t\t\t\t\t\t<td bgcolor=\"#ffffff\" width=\"30%\">API key:</td>\r\n");
      out.write("\t\t\t\t\t\t<td bgcolor=\"#ffffff\" width=\"70%\"><input type=\"password\" id=\"apiKey\" name=\"apiKey\" value=\"\" size=\"20\" /></td>\r\n");
      out.write("\t\t\t\t\t</tr>\r\n");
      out.write("\t\t\t\t\t<tr valign=\"top\">\r\n");
      out.write("\t\t\t\t\t\t<td bgcolor=\"#ffffff\" width=\"30%\">Communities:</td>\r\n");
      out.write("\t\t\t\t\t\t<td bgcolor=\"#ffffff\" width=\"70%\">");
      out.print(listOfCommunities );
      out.write("</td>\r\n");
      out.write("\t\t\t\t\t</tr>\r\n");
      out.write("\t\t\t\t\t<tr>\r\n");
      out.write("\t\t\t\t\t\t<td bgcolor=\"#ffffff\" width=\"30%\"></td>\r\n");
      out.write("\t\t\t\t\t\t<td bgcolor=\"#ffffff\" width=\"70%\">\r\n");

	if (personid.length() > 0) {

      out.write("\r\n");
      out.write("\t\t\t\t\t\t\t<input name=\"saveAccount\" id=\"saveAccount\" value=\"Save User Account\" type=\"submit\"\r\n");
      out.write("\t\t\t\t\t\t\t\t\tonclick=\"if (confirm('Are you sure you want to change these account details?'))  return true; return false;\"\r\n");
      out.write("\t\t\t\t\t\t\t/>\r\n");
      out.write("\t\t\t\t\t\t\t\r\n");
      out.write("\t\t\t\t\t\t\t<input name=\"updatePassword\" value=\"Update Password\" type=\"submit\"\r\n");
      out.write("\t\t\t\t\t\t\t\t\tonclick=\"if (confirm('Are you sure you want to update this password?'))  return true; return false;\"\r\n");
      out.write("\t\t\t\t\t\t\t/>\r\n");

	}
	else
	{

      out.write("\r\n");
      out.write("\t\t\t\t\t\t\t<input name=\"createAccount\" value=\"Create User Account\" type=\"submit\"\r\n");
      out.write("\t\t\t\t\t\t\t\t\tonclick=\"if (confirm('Are you sure you want to create this user account?'))  return true; return false;\"\r\n");
      out.write("\t\t\t\t\t\t\t/>\t\r\n");

	}

      out.write("\r\n");
      out.write("\t\t\t\t\t\t</td>\r\n");
      out.write("\t\t\t\t\t</tr>\r\n");
      out.write("\t\t\t\t\t</table>\r\n");
      out.write("\t\t\t\t\t\r\n");
      out.write("\t\t\t\t</td>\r\n");
      out.write("\t\t\t</tr>\r\n");
      out.write("\t\t\t</table>\r\n");
      out.write("\t\t\r\n");
      out.write("\t\t</td>\r\n");
      out.write("\t\t\r\n");
      out.write("\t<tr>\r\n");
      out.write("\t</table>\r\n");
      out.write("\t<input type=\"hidden\" name=\"oldemail\" id=\"oldemail\" value=\"");
      out.print(email);
      out.write("\"/>\r\n");
      out.write("\t</form>\r\n");

	}

      out.write("\r\n");
      out.write("\r\n");
      out.write("\t\r\n");
      out.write("\t</td>\r\n");
      out.write("<tr>\r\n");
      out.write("<tr>\r\n");
      out.write("\t<td align=\"right\" bgcolor=\"#000000\">\r\n");
      out.write("\t\t&nbsp;\r\n");
      out.write("\t\t<!-- <a href=\"http://www.ikanow.com\" title=\"www.ikanow.com\"><img src=\"image/ikanow_logo_small.png\" border=\"0\"></a> -->\r\n");
      out.write("\t</td>\r\n");
      out.write("</tr>\r\n");
      out.write("</table>\r\n");
      out.write("\r\n");
      out.write("</body>\r\n");
      out.write("</html>\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write('\r');
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

  private boolean _jspx_meth_c_005fset_005f0(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  c:set
    org.apache.taglibs.standard.tag.rt.core.SetTag _jspx_th_c_005fset_005f0 = (org.apache.taglibs.standard.tag.rt.core.SetTag) _005fjspx_005ftagPool_005fc_005fset_0026_005fvar_005fvalue_005fnobody.get(org.apache.taglibs.standard.tag.rt.core.SetTag.class);
    _jspx_th_c_005fset_005f0.setPageContext(_jspx_page_context);
    _jspx_th_c_005fset_005f0.setParent(null);
    // /inc/sharedFunctions.jsp(41,0) name = var type = java.lang.String reqTime = false required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_c_005fset_005f0.setVar("language");
    // /inc/sharedFunctions.jsp(41,0) name = value type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_c_005fset_005f0.setValue((java.lang.Object) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${not empty param.language ? param.language : not empty language ? language : pageContext.request.locale}", java.lang.Object.class, (PageContext)_jspx_page_context, null, false));
    int _jspx_eval_c_005fset_005f0 = _jspx_th_c_005fset_005f0.doStartTag();
    if (_jspx_th_c_005fset_005f0.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005fc_005fset_0026_005fvar_005fvalue_005fnobody.reuse(_jspx_th_c_005fset_005f0);
      return true;
    }
    _005fjspx_005ftagPool_005fc_005fset_0026_005fvar_005fvalue_005fnobody.reuse(_jspx_th_c_005fset_005f0);
    return false;
  }

  private boolean _jspx_meth_fmt_005fsetLocale_005f0(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:setLocale
    org.apache.taglibs.standard.tag.rt.fmt.SetLocaleTag _jspx_th_fmt_005fsetLocale_005f0 = (org.apache.taglibs.standard.tag.rt.fmt.SetLocaleTag) _005fjspx_005ftagPool_005ffmt_005fsetLocale_0026_005fvalue_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.SetLocaleTag.class);
    _jspx_th_fmt_005fsetLocale_005f0.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fsetLocale_005f0.setParent(null);
    // /inc/sharedFunctions.jsp(42,0) name = value type = null reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fsetLocale_005f0.setValue((java.lang.Object) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${language}", java.lang.Object.class, (PageContext)_jspx_page_context, null, false));
    int _jspx_eval_fmt_005fsetLocale_005f0 = _jspx_th_fmt_005fsetLocale_005f0.doStartTag();
    if (_jspx_th_fmt_005fsetLocale_005f0.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fsetLocale_0026_005fvalue_005fnobody.reuse(_jspx_th_fmt_005fsetLocale_005f0);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fsetLocale_0026_005fvalue_005fnobody.reuse(_jspx_th_fmt_005fsetLocale_005f0);
    return false;
  }

  private boolean _jspx_meth_fmt_005fsetBundle_005f0(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:setBundle
    org.apache.taglibs.standard.tag.rt.fmt.SetBundleTag _jspx_th_fmt_005fsetBundle_005f0 = (org.apache.taglibs.standard.tag.rt.fmt.SetBundleTag) _005fjspx_005ftagPool_005ffmt_005fsetBundle_0026_005fbasename_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.SetBundleTag.class);
    _jspx_th_fmt_005fsetBundle_005f0.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fsetBundle_005f0.setParent(null);
    // /inc/sharedFunctions.jsp(43,0) name = basename type = null reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fsetBundle_005f0.setBasename("infinit.e.web.localization.text");
    int _jspx_eval_fmt_005fsetBundle_005f0 = _jspx_th_fmt_005fsetBundle_005f0.doStartTag();
    if (_jspx_th_fmt_005fsetBundle_005f0.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fsetBundle_0026_005fbasename_005fnobody.reuse(_jspx_th_fmt_005fsetBundle_005f0);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fsetBundle_0026_005fbasename_005fnobody.reuse(_jspx_th_fmt_005fsetBundle_005f0);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f0(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f0 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f0.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f0.setParent(null);
    // /inc/header.jsp(8,52) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f0.setKey("header.people.description");
    int _jspx_eval_fmt_005fmessage_005f0 = _jspx_th_fmt_005fmessage_005f0.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f0.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f0);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f0);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f1(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f1 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f1.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f1.setParent(null);
    // /inc/header.jsp(8,101) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f1.setKey("header.people.title");
    int _jspx_eval_fmt_005fmessage_005f1 = _jspx_th_fmt_005fmessage_005f1.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f1.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f1);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f1);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f2(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f2 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f2.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f2.setParent(null);
    // /inc/header.jsp(9,57) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f2.setKey("header.communities.description");
    int _jspx_eval_fmt_005fmessage_005f2 = _jspx_th_fmt_005fmessage_005f2.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f2.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f2);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f2);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f3(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f3 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f3.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f3.setParent(null);
    // /inc/header.jsp(9,111) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f3.setKey("header.communities.title");
    int _jspx_eval_fmt_005fmessage_005f3 = _jspx_th_fmt_005fmessage_005f3.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f3.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f3);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f3);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f4(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f4 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f4.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f4.setParent(null);
    // /inc/header.jsp(10,53) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f4.setKey("header.source_editor.description");
    int _jspx_eval_fmt_005fmessage_005f4 = _jspx_th_fmt_005fmessage_005f4.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f4.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f4);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f4);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f5(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f5 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f5.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f5.setParent(null);
    // /inc/header.jsp(10,109) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f5.setKey("header.source_editor.title");
    int _jspx_eval_fmt_005fmessage_005f5 = _jspx_th_fmt_005fmessage_005f5.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f5.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f5);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f5);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f6(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f6 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f6.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f6.setParent(null);
    // /inc/header.jsp(11,51) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f6.setKey("header.home.description");
    int _jspx_eval_fmt_005fmessage_005f6 = _jspx_th_fmt_005fmessage_005f6.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f6.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f6);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f6);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f7(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f7 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f7.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f7.setParent(null);
    // /inc/header.jsp(11,98) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f7.setKey("header.home.title");
    int _jspx_eval_fmt_005fmessage_005f7 = _jspx_th_fmt_005fmessage_005f7.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f7.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f7);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f7);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f8(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f8 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f8.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f8.setParent(null);
    // /inc/header.jsp(12,56) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f8.setKey("header.logout.description");
    int _jspx_eval_fmt_005fmessage_005f8 = _jspx_th_fmt_005fmessage_005f8.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f8.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f8);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f8);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f9(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f9 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f9.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f9.setParent(null);
    // /inc/header.jsp(12,105) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f9.setKey("header.logout.title");
    int _jspx_eval_fmt_005fmessage_005f9 = _jspx_th_fmt_005fmessage_005f9.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f9.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f9);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f9);
    return false;
  }
}
