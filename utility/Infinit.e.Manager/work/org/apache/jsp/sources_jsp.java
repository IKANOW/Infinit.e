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

public final class sources_jsp extends org.apache.jasper.runtime.HttpJspBase
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
	int itemsToShowPerPage = 18;
	String action = "";
	String logoutAction = "";
	String listFilter = "";

	//
	String shareid = "";
	String sourceid = "";
	String formShareId = "";
	String shareJson = "";
	String sourceJson = "";
	String communityId = "";
	String shareCreated = "";
	String shareTitle = "";
	String shareDescription = "";
	String shareType = "";
	String shareTypeDisplayVal = "";
	String shareModified = "";
	String shareOwnerName = "";
	String shareOwnerEmail = "";
	
	
	// !----------  ----------!
	String harvesterOutput = "";
	
	// !----------  ----------!
	String actionToTake = "";

	// !----------  ----------!
	String sourcePageNo = "0";
	String sourceTemplateSelect = "";
	String selectedSourceTemplate = "";
	String communityIdSelect = "";
	String getFullText = "";
	String getFullTextChecked = "";
	String getTestUpdateLogic = "";
	String getTestUpdateLogicChecked = "";
	String numberOfDocuments = "";



// validateFormFields
private boolean validateFormFields()
{
	boolean isValid = true;
	ArrayList<String> al = new ArrayList<String>();
	if (shareTitle.length() < 1) al.add("Title");
	if (shareDescription.length() < 1) al.add("Description");
	if (al.size() > 0)
	{
		isValid = false;
		messageToDisplay = "Error, the following required fields are missing: " + al.toString();
	}
	return isValid;
}  // TESTED



// saveShare - 
private void saveShare(HttpServletRequest request, HttpServletResponse response) 
{
	try 
	{
		String oldId = formShareId;
		
		String apiAddress = "";
		String urlShareTitle = URLEncoder.encode(shareTitle.trim(), "UTF-8");
		String urlShareDescription = URLEncoder.encode(shareDescription.trim(), "UTF-8");
		
		if (oldId != null)
		{
			apiAddress = "social/share/update/json/" + oldId + "/source/" + urlShareTitle + "/" + urlShareDescription;
		}
		else
		{
			apiAddress = "social/share/add/json/source/" + urlShareTitle + "/" + urlShareDescription;
		}
		
		//
		JSONObject source = new JSONObject(sourceJson);
		source.remove("title");
		source.put("title", shareTitle.trim());
		source.remove("description");
		source.put("description", shareDescription.trim());
		
		// CommunityID Array - Delete and replace with id from community id dropdown list
		if (communityId.length() > 0)
		{
			source.remove("communityIds");
			JSONArray communityIds = new JSONArray();
			communityIds.put(communityId);
			source.put("communityIds", communityIds);
			sourceJson = source.toString(4);
		} //TESTED

		// Post the update to our rest API and check the results of the post
		JSONObject json_response = new JSONObject(postToRestfulApi(apiAddress, sourceJson, request, response)).getJSONObject("response");
		if (json_response.getString("success").equalsIgnoreCase("true")) 
		{
			messageToDisplay = "Success: " + json_response.getString("message");
		}
		else
		{
			messageToDisplay = "Error: " + json_response.getString("message");
		}
	} 
	catch (Exception e) 
	{
		messageToDisplay = "Error: " + e.getMessage() + " " + e.getStackTrace().toString();
	}
} // TESTED


// publishSource - 
// 1. Add/update ingest.source object
// 2. Delete the share object, shazam
private void publishSource(HttpServletRequest request, HttpServletResponse response) 
{
	try 
	{
		// CommunityID Array - Delete and replace with id from community id dropdown list
		if (communityId.length() > 0)
		{
			JSONObject source = new JSONObject(sourceJson);
			source.remove("communityIds");
			JSONArray communityIds = new JSONArray();
			communityIds.put(communityId);
			source.put("communityIds", communityIds);
			sourceJson = source.toString(4);
		} //TESTED
		
		String sourceApiString = "config/source/save/" + communityId;
		
		// Post the update to our rest API and check the results of the post
		JSONObject result = new JSONObject(postToRestfulApi(sourceApiString, sourceJson, request, response));
		JSONObject JSONresponse = result.getJSONObject("response");
		
		if (JSONresponse.getString("success").equalsIgnoreCase("true")) 
		{
			messageToDisplay = "Success: " + JSONresponse.getString("message");
			// Delete the share object - shareId
			String apiAddress = "social/share/remove/" + shareid;

			// Post the update to our rest API and check the results of the post
			JSONObject shareResponse = new JSONObject(callRestfulApi(apiAddress, request, response)).getJSONObject("response");
			if (shareResponse.getString("success").equalsIgnoreCase("true")) 
			{
				messageToDisplay += " (" + shareResponse.getString("message") + ")";
			}
			else
			{
				messageToDisplay += " (" + shareResponse.getString("message") + ")";
			}
		}
		else
		{
			messageToDisplay = "Error: " + JSONresponse.getString("message");
		}
	} 
	catch (Exception e) 
	{
		messageToDisplay = "Error: " + e.getMessage() + " " + e.getStackTrace().toString();
	}
} // 



// saveSourceAsTemplate - 
private void saveShareAsTemplate(HttpServletRequest request, HttpServletResponse response) 
{
	try 
	{
		String urlShareTitle = URLEncoder.encode(shareTitle + " - Template", "UTF-8");
		String urlShareDescription = URLEncoder.encode(shareDescription, "UTF-8");
		String apiAddress = "social/share/add/json/source_template/" + urlShareTitle + "/" + urlShareDescription;
		
		JSONObject JSONresponse = new JSONObject(postToRestfulApi(apiAddress, sourceJson, request, response)).getJSONObject("response");
		if (JSONresponse.getString("success").equalsIgnoreCase("true")) 
		{
			messageToDisplay = "Success: " + JSONresponse.getString("message");
		}
		else
		{
			messageToDisplay = "Error: " + JSONresponse.getString("message");
		}
	} 
	catch (Exception e) 
	{
		messageToDisplay = "Error: " + e.getMessage() + " " + e.getStackTrace().toString();
	}
} //



// deleteShare -
private void deleteShare(String shareId, HttpServletRequest request, HttpServletResponse response)
{
	if (shareId != null && shareId != "") 
	{
		JSONObject source = getSourceJSONObjectFromShare(shareId, request, response);
		String apiAddress = "social/share/remove/" + shareId + "/";
		try 
		{
			JSONObject JSONresponse = new JSONObject(callRestfulApi(apiAddress, request, response)).getJSONObject("response");
			if (JSONresponse.getString("success").equalsIgnoreCase("true")) 
			{
				messageToDisplay = "Success: " + JSONresponse.getString("message");
			}
			else
			{
				messageToDisplay = "Error: " + JSONresponse.getString("message");
			}
		}
		catch (Exception e)
		{
			messageToDisplay = "Error: " + e.getMessage() + " " + e.getStackTrace().toString();
		}
	}
} // TESTED


// getSourceJSONObject
private JSONObject getSourceJSONObjectFromShare(String shareId, HttpServletRequest request, HttpServletResponse response)
{
	try
	{
		// Call the api and get the result as a string
		String result = getShare(shareId, request, response);
		
		// Convert string to JSONObjects
		JSONObject json_response = new JSONObject(result);
		JSONObject data = json_response.getJSONObject("data");
		
		// Get the share object and make sure it is encoded properly for display
		shareJson = URLDecoder.decode(data.toString(), "UTF-8");
		return new JSONObject(data.getString("share"));
	}
	catch (Exception e)
	{
		return null;
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
			// Call the api and get the result as a string
			String result = getShare(id, request, response);
			
			// Convert string to JSONObjects
			JSONObject json_response = new JSONObject(result);
			JSONObject data = json_response.getJSONObject("data");
			
			// Get the share object and make sure it is encoded properly for display
			shareJson = URLDecoder.decode(data.toString(), "UTF-8");
			JSONObject source = new JSONObject(data.getString("share"));
			JSONObject owner = data.getJSONObject("owner");
			
			try
			{
				communityId = source.getJSONArray("communityIds").getString(0);
			}
			catch (Exception ex) { }
			
			// Copy fields to the edit source form
			sourceJson = source.toString(4); // Formatted with indents for display
			if (source.has("_id")) {
				sourceid = source.getString("_id");
			}
			if (source.has("title")) {
				shareTitle = data.getString("title");
			}
			if ((null == shareTitle) || shareTitle.isEmpty()) {
				shareTitle = source.getString("title");				
			}
			if (source.has("description")) {
				shareDescription = data.getString("description");
			}
			if ((null == shareDescription) || shareDescription.isEmpty()) {
				shareDescription = source.getString("description");				
			}
			shareType = data.getString("type");
			if (shareType.equalsIgnoreCase("source"))
			{
				shareTypeDisplayVal = "Source";
			}
			else if (shareType.equalsIgnoreCase("source_published"))
			{
				shareTypeDisplayVal = "Published Source";
			}
			else if (shareType.equalsIgnoreCase("source_template"))
			{
				shareTypeDisplayVal = "Source Template";
			}
					
			String shareType = data.getString("type");
			if (!shareType.equalsIgnoreCase("source_template")) 
			{
				shareid = data.getString("_id");
				shareOwnerName = owner.getString("displayName");
				shareOwnerEmail = owner.getString("email");
				shareCreated = data.getString("created");
				shareModified = data.getString("modified");
			}
			//
			else
			{
				shareid = "";
				shareJson = "";
			}
		} 
		catch (Exception e) 
		{
			sourceJson = "Error:" + e.getMessage();
		}
	}
}  // TESTED



// clearForm
private void clearForm()
{
	shareid = "";
	shareTitle = "";
	shareDescription = "";
	shareType = "";
	shareOwnerName = "";
	shareOwnerEmail = "";
	shareCreated = "";
	shareModified = "";
	shareJson = "";
	sourceJson = "";
}  // TESTED



// listItems -
private String listItems(HttpServletRequest request, HttpServletResponse response)
{
	StringBuffer sources = new StringBuffer();
	Map<String, String> listOfSources = getUserSourcesAndShares(request, response);
	
	if (listOfSources.size() > 0)
	{
		sources.append("<table class=\"listTable\" cellpadding=\"3\" cellspacing=\"1\" width=\"100%\" >");

		// Sort the sources alphabetically
		List<String> sortedKeys = new ArrayList<String>(listOfSources.keySet());
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
			String name = key;
			if (currentItem >= startItem && currentItem <= endItem)
			{
				String id = listOfSources.get(key).toString();
				String editLink = "";
				String deleteLink = "";
				String listFilterString = "";
				if (listFilter.length() > 0) listFilterString = "&listFilterStr="+ listFilter;
				
				if (name.contains("*"))
				{
					editLink = "<a href=\"sources.jsp?action=edit&shareid=" + id + "&page=" + currentPage 
							+ listFilterString + "\" title=\"Edit Share\">" + name + "</a>";

					deleteLink = "<a href=\"sources.jsp?action=delete&shareid=" + id + "&page=" + currentPage 
							+ listFilterString + "\" title=\"Delete Share\" "
							+ "onclick='return confirm(\"Do you really wish to delete the share: "
							+ name + "?\");'><img src=\"image/delete_x_button.png\" border=0></a>";
				}
				else
				{
					editLink = "<a href=\"sources.jsp?action=sharefromsource&sourceid=" + id + "&page=" + currentPage 
							+ listFilterString + "\" title=\"Create Share from Source\">" + name + "</a>";
							
					deleteLink = "<a href=\"sources.jsp?action=deletesource&sourceid=" + id + "&page=" + currentPage 
							+ listFilterString + "\" title=\"Delete Source\" "
							+ "onclick='return confirm(\"Do you really wish to delete the source: "
							+ name + "?\");'><img src=\"image/delete_x_button.png\" border=0></a>";
				}
	
				// Create the HTML table row
				sources.append("<tr valign=\"top\">");
				sources.append("<td bgcolor=\"white\" width=\"100%\">" + editLink + "</td>");
				sources.append("<td align=\"center\" bgcolor=\"white\">" + deleteLink + "</td>");
				sources.append("</tr>");
			}
			currentItem++;
		}
		
		sources.append("<tr valign=\"top\">");
		sources.append("<td bgcolor=\"white\" width=\"100%\" colspan=\"2\">");
		sources.append("(*) Share<br>");
		sources.append("(+) Source owned by someone else");
		sources.append("</td>");
		sources.append("</tr>");
		
		// Calculate number of pages, current page, page links...
		sources.append("<tr><td colspan=\"2\" align=\"center\" class=\"subTableFooter\">");
		// --------------------------------------------------------------------------------
		// Create base URL for each page
		StringBuffer baseUrl = new StringBuffer();
		baseUrl.append("sources.jsp?");
		String actionString = (action.length() > 0) ? "action=" + action : "";
		String shareIdString = (shareid.length() > 0) ? "shareid=" + shareid : "";
		if (actionString.length() > 0) baseUrl.append(actionString);
		if (actionString.length() > 0 && shareIdString.length() > 0) baseUrl.append("&");
		if (shareIdString.length() > 0) baseUrl.append(shareIdString);
		if (actionString.length() > 0 || shareIdString.length() > 0) baseUrl.append("&");
		baseUrl.append("page=");
		sources.append( createPageString( sortedAndFilteredKeys.size(), itemsToShowPerPage, currentPage, baseUrl.toString() ));
		sources.append("</td></tr>");
		// --------------------------------------------------------------------------------
		sources.append("</table>");
	}
	else
	{
		sources.append("No sources were retrieved");
	}

	return sources.toString();
}


// createShareFromSource
private String createShareFromSource(String sourceId, HttpServletRequest request, HttpServletResponse response)
{
	try
	{
		JSONObject sourceResponse = new JSONObject( getSource(sourceId, request, response) );
		JSONObject sourceJson =  new JSONObject( sourceResponse.getString("data") );
		
		String urlShareTitle = URLEncoder.encode(sourceJson.getString("title"), "UTF-8");
		String urlShareDescription = "";
		try
		{
			urlShareDescription = URLEncoder.encode(sourceJson.getString("description"), "UTF-8");
		}
		catch (Exception de)
		{
			urlShareDescription = URLEncoder.encode("Share description goes here", "UTF-8");
		}
		
		String apiAddress = "social/share/add/json/source/" + urlShareTitle + "/" + urlShareDescription;
		JSONObject jsonObject = new JSONObject( postToRestfulApi(apiAddress, sourceJson.toString(4), request, response) );
		JSONObject json_response = jsonObject.getJSONObject("response");
		JSONObject json_data = new JSONObject ( jsonObject.getString("data") );
		
		//clearForm();
		//populateEditForm(json_data.getString("_id"), request, response);
		
		// Return new shareid to caller
		return json_data.getString("_id");
	}
	catch (Exception e)
	{
		System.out.println(e.getMessage());
		return null;
	}
}



// createCommunityIdSelect -
// Create select control with list of communityids available to user
private void createCommunityIdSelect(HttpServletRequest request, HttpServletResponse response) 
{
	try 
	{
		StringBuffer html = new StringBuffer();
		html.append("<select name=\"Community_ID\" id=\"Community_ID\">");
		
		JSONArray communities = getPersonCommunities(request, response);
		if (communities != null)
		{
			for (int i = 0; i < communities.length(); i++) 
			{
				JSONObject source = communities.getJSONObject(i);
				String name = source.getString("name");
				String id = source.getString("_id");
				String selectedString = (id.equalsIgnoreCase(communityId)) ? " SELECTED" : "";
				html.append("<option value=\"" + id + "\"" + selectedString + ">" + name + "</option>");
			}				
		}
		html.append("</select>");
		communityIdSelect = html.toString();
	} 
	catch (Exception e) 
	{
	}
} // TESTED



// testSource -
private void testSource(HttpServletRequest request, HttpServletResponse response)
{
	int numDocs = 10;
	try
	{
		numDocs = Integer.parseInt(numberOfDocuments);
		if (numDocs < 1 || numDocs > 10) numDocs = 10;
	}
	catch (Exception e)
	{
		numDocs = 10;
	}
	String apiAddress = "config/source/test?returnFullText=" + getFullText + "&numReturn=" + String.valueOf(numDocs) + "&testUpdates=" + getTestUpdateLogic;
	harvesterOutput = "";
	messageToDisplay = "";
	
	try 
	{
		JSONObject jsonObject = new JSONObject(postToRestfulApi(apiAddress, sourceJson, request, response));
		JSONObject JSONresponse = jsonObject.getJSONObject("response");
		
		try
		{
			messageToDisplay = JSONresponse.getString("message");
			
			if (jsonObject.has("data"))
			{
				JSONArray data = jsonObject.getJSONArray("data");
				
				StringBuffer s = new StringBuffer();
				for (int i = 0; i < data.length(); i++ )
				{
					JSONObject jo = data.getJSONObject(i);
					s.append("\n");
					s.append(jo.toString(4));
				}
				
				harvesterOutput = s.toString();
			}
		}
		catch (Exception ex)
		{
			messageToDisplay = "Test Result: " + JSONresponse.getString("message");
		}
		if (harvesterOutput.length() < 1) harvesterOutput = " ";
	}
	catch (Exception e)
	{
		messageToDisplay = "Error: " + e.getMessage() + " " + e.getStackTrace().toString();
	}
} // TESTED


// deleteSourceObject -
private void deleteSourceObject(String sourceId, boolean bDocsOnly, HttpServletRequest request, HttpServletResponse response)
{
	if (sourceId != null && sourceId != "") 
	{
		try 
		{
			JSONObject sourceResponse = new JSONObject( getSource(sourceId, request, response) );
			JSONObject source = new JSONObject( sourceResponse.getString("data") );
			JSONArray com = source.getJSONArray("communityIds");
			String tempCommunityId = com.getString(0);
					
			JSONObject JSONresponse = new JSONObject(deleteSource(sourceId, bDocsOnly, tempCommunityId, 
					request, response)).getJSONObject("response");
			
			if (JSONresponse.getString("success").equalsIgnoreCase("true")) 
			{
				messageToDisplay = "Success: " + JSONresponse.getString("message");
			}
			else
			{
				messageToDisplay = "Error: " + JSONresponse.getString("message");
			}
		}
		catch (Exception e)
		{
			messageToDisplay = "Error: " + e.getMessage() + " " + e.getStackTrace().toString();
		}
	}
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
		
		if (request.getParameter("testSource") != null) action = "testSource";
		if (request.getParameter("saveSource") != null) action = "saveSource";
		if (request.getParameter("saveSourceAsTemplate") != null) action = "saveSourceAsTemplate";
		if (request.getParameter("publishSource") != null) action = "publishSource";
		if (request.getParameter("deleteDocs") != null) action = "deleteDocs";
		if (request.getParameter("newSource") != null) action = "newSource";
		
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
			shareid = (request.getParameter("shareid") != null) ? request.getParameter("shareid") : "";
			formShareId = (request.getParameter("shareId") != null) ? request.getParameter("shareId") : "";
			sourceid = (request.getParameter("sourceid") != null) ? request.getParameter("sourceid") : "";
			communityId = (request.getParameter("Community_ID") != null) ? request.getParameter("Community_ID") : "";
			shareTitle = (request.getParameter("shareTitle") != null) ? request.getParameter("shareTitle") : "";
			shareDescription = (request.getParameter("shareDescription") != null) ? request.getParameter("shareDescription") : "";
			sourceJson = (request.getParameter("Source_JSON") != null) ? request.getParameter("Source_JSON") : "";
			selectedSourceTemplate = (request.getParameter("sourceTemplateSelect") != null) ? request.getParameter("sourceTemplateSelect") : "";
			numberOfDocuments = (request.getParameter("numOfDocs") != null) ? request.getParameter("numOfDocs") : "10";
			getFullText = (request.getParameter("fullText") != null) ? "true" : "false";
			getFullTextChecked = (getFullText.equalsIgnoreCase("true")) ? "CHECKED" : "";
			getTestUpdateLogic = (request.getParameter("testUpdateLogic") != null) ? "true" : "false";
			getTestUpdateLogicChecked = (getTestUpdateLogic.equalsIgnoreCase("true")) ? "CHECKED" : "";
			
			Boolean redirect = false;
			
			// if redirect == true refresh the page to update the edit form's content to reflect changes
			if (redirect) 
			{
				String urlParams = "";
				if (listFilter.length() > 0) urlParams = "&listFilterStr="+ listFilter;
				if (currentPage > 1) urlParams += "&page=" + currentPage;
				out.println("<meta http-equiv=\"refresh\" content=\"0;url=sources.jsp?action=edit&shareid=" 
					+ shareid + urlParams + "\">");
			}
			
			if (action.equals("clearform")) 
			{
				out.println("<meta http-equiv=\"refresh\" content=\"0;url=sources.jsp\">");
			}
			else if (action.equals("edit")) 
			{
				populateEditForm(shareid, request, response);
			}
			else if (action.equals("sharefromsource"))
			{
				// Create a new share from the source object
				String newshareid = createShareFromSource(sourceid, request, response);
				// redirect user to edit source page
				String listFilterString = "";
				if (listFilter.length() > 0) listFilterString = "&listFilterStr="+ listFilter;
				String urlArgs = "action=edit&shareid=" + newshareid + listFilterString + "&page=" + currentPage;
				out.println("<meta http-equiv=\"refresh\" content=\"0;url=sources.jsp?" + urlArgs + "\">");
			}
			else if (action.equals("delete")) 
			{
				deleteShare(shareid, request, response);
				String listFilterString = "";
				if (listFilter.length() > 0) listFilterString = "?listFilterStr="+ listFilter;
				out.println("<meta http-equiv=\"refresh\" content=\"0;url=sources.jsp" + listFilterString + "\">");
			}
			else if (action.equals("deletesource")) 
			{
				deleteSourceObject(sourceid, false, request, response);
				String listFilterString = "";
				if (listFilter.length() > 0) listFilterString = "?listFilterStr="+ listFilter;
				out.println("<meta http-equiv=\"refresh\" content=\"0;url=sources.jsp" + listFilterString + "\">");
			}
			else if (action.equals("filterlist")) 
			{
				currentPage = 1;
				populateEditForm(shareid, request, response);
			}
			else if (action.equals("clearfilter")) 
			{
				listFilter = "";
				populateEditForm(shareid, request, response);
			}
			else if (action.equals("saveSource")) 
			{
				saveShare(request, response);
				populateEditForm(shareid, request, response);
			}
			else if (action.equals("saveSourceAsTemplate")) 
			{
				saveShareAsTemplate(request, response);
				populateEditForm(shareid, request, response);
			}
			else if (action.equals("publishSource")) 
			{
				publishSource(request, response);
				String listFilterString = "";
				if (listFilter.length() > 0) listFilterString = "?listFilterStr="+ listFilter;
				out.println("<meta http-equiv=\"refresh\" content=\"0;url=sources.jsp" + listFilterString + "\">");
			}
			else if (action.equals("deleteDocs")) 
			{
				deleteSourceObject(sourceid, true, request, response);
				String listFilterString = "";
				if (listFilter.length() > 0) listFilterString = "?listFilterStr="+ listFilter;
				out.println("<meta http-equiv=\"refresh\" content=\"0;url=sources.jsp" + listFilterString + "\">");
			}
			else if (action.equals("newSource")) 
			{
				out.println("<meta http-equiv=\"refresh\" content=\"0;url=newsource.jsp\">");
			}
			else if (action.equals("testSource")) 
			{
				getShare(shareid, request, response);
				testSource(request, response);
			}
			else if (action.equals("logout")) 
			{
				logOut(request, response);
				out.println("<meta http-equiv=\"refresh\" content=\"0;url=index.jsp\">");
			}
			
			createCommunityIdSelect(request, response);
		}
		catch (Exception e)
		{
			System.out.println(e.getMessage());
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
      out.write("\t<title>Infinit.e.Manager - Sources</title>\n");
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
      out.write('\n');
      out.write('\n');
      out.write('\n');
 
	//!-- Open new window to show source test results --
	if (harvesterOutput.length() > 0) 
	{
		String messageToOutput = URLEncoder.encode(messageToDisplay, "UTF-8");
		String output = URLEncoder.encode(harvesterOutput, "UTF-8");
		harvesterOutput = "";
		messageToDisplay = "";

      out.write("\n");
      out.write("\t<script language=\"javascript\" type=\"text/javascript\">\n");
      out.write("\t\topenTestSourceWindow('");
      out.print(messageToOutput );
      out.write("', '");
      out.print(output );
      out.write("');\n");
      out.write("\t</script>\n");
 } 
      out.write("\n");
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
      out.write("\t\t\t\t<td class=\"headerLink\">Sources</td>\n");
      out.write("\t\t\t\t<td align=\"right\"><input type=\"text\" id=\"listFilter\" \n");
      out.write("\t\t\t\t\tonkeydown=\"if (event.keyCode == 13) { setDipatchAction('filterList'); \n");
      out.write("\t\t\t\t\tdocument.getElementById('filterList').click(); }\" \n");
      out.write("\t\t\t\t\tname=\"listFilter\" size=\"20\" value=\"");
      out.print(listFilter );
      out.write("\"/><button name=\"filterList\" \n");
      out.write("\t\t\t\t\tvalue=\"filterList\">Filter</button><button name=\"clearFilter\" value=\"clearFilter\">Clear</button></td>\n");
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
      out.write("\t\t\t\t<td class=\"headerLink\">Edit Source</td>\n");
      out.write("\t\t\t\t<td align=\"right\"><button name=\"newSource\" value=\"newSource\">New Source</button></td>\n");
      out.write("\t\t\t</tr>\n");
      out.write("\t\t\t<tr>\n");
      out.write("\t\t\t\t<td colspan=\"2\" bgcolor=\"white\">\n");
      out.write("\t\t\t\t\t<table class=\"standardSubTable\" cellpadding=\"3\" cellspacing=\"1\" width=\"100%\" >\n");
 if (!shareid.equalsIgnoreCase("")) { 
      out.write("\n");
      out.write("\t\t\t\t\t\t<tr>\n");
      out.write("\t\t\t\t\t\t\t<td bgcolor=\"white\" width=\"30%\">Source Functions:</td>\n");
      out.write("\t\t\t\t\t\t\t<td bgcolor=\"white\" width=\"70%\">\n");
      out.write("\n");
      out.write("\t\t\t\t\t\t\t\t<button name=\"testSource\" value=\"testSource\">Test Source</button>\n");
      out.write("\t\t\t\t\t\t\t\t<button name=\"saveSource\" value=\"saveSource\">Save Source</button>\n");
      out.write("\t\t\t\t\t\t\t\t<button name=\"saveSourceAsTemplate\" value=\"saveSourceAsTemplate\">Save Source as Template</button>\n");
      out.write("\t\t\t\t\t\t\t\t\n");
      out.write("\t\t\t\t\t\t\t\t<button name=\"publishSource\" value=\"publishSource\"\n");
      out.write("\t\t\t\t\t\t\t\t\tonclick=\"if (confirm('Are you sure you want to publish this source?'))  return true; return false;\"\n");
      out.write("\t\t\t\t\t\t\t\t\t>Publish Source</button>\n");
      out.write("\t\t\t\t\t\t\t\t\t\n");
 if ((null != sourceid) && !sourceid.equalsIgnoreCase("")) { 
      out.write("\n");
      out.write("\t\t\t\t\t\t\t\t<button name=\"deleteDocs\" value=\"deleteDocs\" \n");
      out.write("\t\t\t\t\t\t\t\t\tonclick=\"if (confirm('Are you sure you want to delete all documents for this source?')) return true; return false;\"\n");
      out.write("\t\t\t\t\t\t\t\t\t>Delete docs</button>\n");
 } 
      out.write("\n");
      out.write("\t\t\t\t\t\t\t</td>\t\t\n");
      out.write("\t\t\t\t\t\t</tr>\n");
 } 
      out.write("\n");
      out.write("\t\t\t\t\t\t<tr>\n");
      out.write("\t\t\t\t\t\t\t<td bgcolor=\"white\" width=\"30%\">Share ID:</td>\n");
      out.write("\t\t\t\t\t\t\t<td bgcolor=\"white\" width=\"70%\">\n");
      out.write("\t\t\t\t\t\t\t\t<input type=\"text\" id=\"shareId\" name=\"shareId\" value=\"");
      out.print(shareid);
      out.write("\" size=\"35\" READONLY />\n");
      out.write("\t\t\t\t\t\t\t</td>\t\t\n");
      out.write("\t\t\t\t\t\t</tr>\n");
      out.write("\t\t\t\t\t\t<tr>\n");
      out.write("\t\t\t\t\t\t\t<td bgcolor=\"white\" width=\"30%\">Title:</td>\n");
      out.write("\t\t\t\t\t\t\t<td bgcolor=\"white\" width=\"70%\">\n");
      out.write("\t\t\t\t\t\t\t\t<input type=\"text\" id=\"shareTitle\" name=\"shareTitle\" value=\"");
      out.print(shareTitle);
      out.write("\" size=\"60\" />\n");
      out.write("\t\t\t\t\t\t\t</td>\t\t\n");
      out.write("\t\t\t\t\t\t</tr>\n");
      out.write("\t\t\t\t\t\t<tr valign=\"top\">\n");
      out.write("\t\t\t\t\t\t\t<td bgcolor=\"white\" width=\"30%\">Description:</td>\n");
      out.write("\t\t\t\t\t\t\t<td bgcolor=\"white\" width=\"70%\">\n");
      out.write("\t\t\t\t\t\t\t\t<textarea cols=\"45\" rows=\"3\" id=\"shareDescription\" name=\"shareDescription\">");
      out.print(shareDescription);
      out.write("</textarea>\n");
      out.write("\t\t\t\t\t\t\t</td>\t\t\n");
      out.write("\t\t\t\t\t\t</tr>\n");
      out.write("\t\t\t\t\t\t<!-- <tr>\n");
      out.write("\t\t\t\t\t\t\t<td bgcolor=\"white\" width=\"30%\">Type:</td>\n");
      out.write("\t\t\t\t\t\t\t<td bgcolor=\"white\" width=\"70%\">");
      out.print(shareTypeDisplayVal);
      out.write("</td>\t\t\n");
      out.write("\t\t\t\t\t\t</tr> -->\n");
      out.write("\t\t\t\t\t\t<tr>\n");
      out.write("\t\t\t\t\t\t\t<td bgcolor=\"white\" width=\"30%\">Owner:</td>\n");
      out.write("\t\t\t\t\t\t\t<td bgcolor=\"white\" width=\"70%\">");
      out.print(shareOwnerName);
      out.write(' ');
      out.write('-');
      out.write(' ');
      out.print(shareOwnerEmail);
      out.write("</td>\t\t\n");
      out.write("\t\t\t\t\t\t</tr>\n");
      out.write("\t\t\t\t\t\t<tr>\n");
      out.write("\t\t\t\t\t\t\t<td bgcolor=\"white\" width=\"30%\">Community:</td>\n");
      out.write("\t\t\t\t\t\t\t<td bgcolor=\"white\" width=\"70%\">");
      out.print(communityIdSelect);
      out.write("</td>\t\t\n");
      out.write("\t\t\t\t\t\t</tr>\n");
      out.write("\t\t\t\t\t\t<tr>\n");
      out.write("\t\t\t\t\t\t\t<td bgcolor=\"white\" width=\"30%\">Test Parameters:</td>\n");
      out.write("\t\t\t\t\t\t\t<td bgcolor=\"white\" width=\"70%\" style=\"height:21px\">\n");
      out.write("\t\t\t\t\t\t\t\tFull Text: <input type=\"checkbox\" name=\"fullText\" value=\"true\" ");
      out.print(getFullTextChecked );
      out.write("/>\n");
      out.write("\t\t\t\t\t\t\t\tNumber of Documents: <input type=\"text\" id=\"numOfDocs\" name=\"numOfDocs\" value=\"");
      out.print(numberOfDocuments );
      out.write("\"\n");
      out.write("\t\t\t\t\t\t\t\t\tsize=\"3\" title=\"Maximum of 10\" />\n");
      out.write("\t\t\t\t\t\t\t\tUpdate Test Mode: <input type=\"checkbox\" name=\"testUpdateLogic\" value=\"true\" ");
      out.print(getTestUpdateLogicChecked );
      out.write("/>\t\t\t\t\t\t\t\n");
      out.write("\t\t\t\t\t\t\t</td>\t\t\n");
      out.write("\t\t\t\t\t\t</tr>\n");
      out.write("\t\t\t\t\t\t<tr>\n");
      out.write("\t\t\t\t\t\t\t<td bgcolor=\"white\" width=\"100%\" colspan=\"2\">\n");
      out.write("\t\t\t\t\t\t\t\t<textarea cols=\"90\" rows=\"25\" id=\"Source_JSON\" name=\"Source_JSON\">");
      out.print(sourceJson);
      out.write("</textarea>\n");
      out.write("\t\t\t\t\t\t\t</td>\t\t\n");
      out.write("\t\t\t\t\t\t</tr>\n");
      out.write("\t\t\t\t\t\t<tr>\n");
      out.write("\t\t\t\t\t\t\t<td bgcolor=\"white\" width=\"30%\">Created:</td>\n");
      out.write("\t\t\t\t\t\t\t<td bgcolor=\"white\" width=\"70%\">");
      out.print(shareCreated);
      out.write("</td>\t\t\n");
      out.write("\t\t\t\t\t\t</tr>\n");
      out.write("\t\t\t\t\t\t<tr>\n");
      out.write("\t\t\t\t\t\t\t<td bgcolor=\"white\" width=\"30%\">Modified:</td>\n");
      out.write("\t\t\t\t\t\t\t<td bgcolor=\"white\" width=\"70%\">");
      out.print(shareModified);
      out.write("</td>\t\t\n");
      out.write("\t\t\t\t\t\t</tr>\n");
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
      out.write("\t<input type=\"hidden\" name=\"sourceid\" id=\"sourceid\" value=\"");
      out.print(sourceid);
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
