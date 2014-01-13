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
						
						JSONObject shareOrSource = shareObj;
						if ((filterType != FilterType.title) ||  (filterType != FilterType.description)) {
							if (shareObj.has("share"))
								shareOrSource = new JSONObject(shareObj.getString("share"));
						}//TESTED
						if (isSourceFiltered(filterType, shareOrSource, filter)) {
							continue;
						}//TESTED
						
						String tempTitle = shareObj.getString("title");						
						JSONObject sourceObj = new JSONObject( shareObj.getString("share") );
						if ( isSuspended(sourceObj) )
							tempTitle = "[SUSPENDED] " + tempTitle;
						if (sourceObj.has("_id")) 
							publishedSources.add( sourceObj.getString("_id") );
						if (sourceObj.has("ownerId") && !sourceObj.getString("ownerId").equalsIgnoreCase(userIdStr)) 
							tempTitle += " (+)";
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
								if (isSourceFiltered(filterType, sourceObj, filter)) {
									continue;
								}//TESTED
								
								String tempTitle = sourceObj.getString("title");
								if ( isSuspended(sourceObj) )
									tempTitle = "[SUSPENDED] " + tempTitle;
								
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



	// 
	int currentPage = 1;
	int itemsToShowPerPage = 18;
	String action = "";
	String logoutAction = "";
	String listFilter = "";

	//
	String shareid = "";
	String sourceid = "";
	String sourceShowRss = "style=\"display: none\";";
	String enableOrDisable = "";
	String formShareId = "";
	String shareJson = "";
	String sourceJson = "";
	String communityId = "";
	String shareCreated = "";
	String shareTitle = "";
	String shareMediaType = "null";
	String shareTags = "";
	String shareDescription = "";
	String shareType = "";
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
	String mediaTypeSelect = "";
	String getFullText = "";
	String getFullTextChecked = "";
	String getTestUpdateLogic = "";
	String getTestUpdateLogicChecked = "";
	String numberOfDocuments = "";
	
	boolean pipelineMode = false; // new source pipeline logic
	boolean enterpriseMode = true; // access to source builder GUI



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
		source.remove("tags");
		String trimmedShareTags = shareTags.trim();
		if (!trimmedShareTags.isEmpty())
			source.put("tags", new JSONArray(trimmedShareTags.split("(?:\\s*,\\s*|\\s+)")));
		source.remove("description");
		source.put("description", shareDescription.trim());
		if (!shareMediaType.equalsIgnoreCase("null")) {
			source.put("mediaType", shareMediaType);
		}
		
		// CommunityID Array - Delete and replace with id from community id dropdown list
		if (communityId.length() > 0)
		{
			source.remove("communityIds");
			JSONArray communityIds = new JSONArray();
			communityIds.put(communityId);
			source.put("communityIds", communityIds);
		} //TESTED
		sourceJson = source.toString(4);

		// Post the update to our rest API and check the results of the post
		JSONObject json_response = new JSONObject(postToRestfulApi(apiAddress, sourceJson, request, response)).getJSONObject("response");
		if (json_response.getString("success").equalsIgnoreCase("true")) 
		{
			messageToDisplay = (String)request.getAttribute("locale_SourceResult_Success") + json_response.getString("message");
		}
		else
		{
			messageToDisplay = (String)request.getAttribute("locale_SourceResult_Error") + json_response.getString("message");
		}
	} 
	catch (Exception e) 
	{
		messageToDisplay = (String)request.getAttribute("locale_SourceResult_Error") + e.getMessage() + " " + e.getStackTrace().toString();
	}
} // TESTED


// publishSource - 
// 1. Add/update ingest.source object
// 2. Delete the share object, shazam
private void publishSource(HttpServletRequest request, HttpServletResponse response) 
{
	try 
	{
		JSONObject source = new JSONObject(sourceJson);
		source.remove("title");
		source.put("title", shareTitle.trim());
		source.put("tags", shareTags.split("(?:\\s*,\\s*|\\s+)"));
		if (!shareMediaType.equalsIgnoreCase("null")) {
			source.put("mediaType", shareMediaType);
		}
		source.remove("description");
		source.put("description", shareDescription.trim());
		// CommunityID Array - Delete and replace with id from community id dropdown list
		if (communityId.length() > 0)
		{
			source.remove("communityIds");
			JSONArray communityIds = new JSONArray();
			communityIds.put(communityId);
			source.put("communityIds", communityIds);
		} //TESTED
		sourceJson = source.toString(4);
		
		String sourceApiString = "config/source/save/" + communityId;
		
		// Post the update to our rest API and check the results of the post
		JSONObject result = new JSONObject(postToRestfulApi(sourceApiString, sourceJson, request, response));
		JSONObject JSONresponse = result.getJSONObject("response");
		
		if (JSONresponse.getString("success").equalsIgnoreCase("true")) 
		{
			messageToDisplay = (String)request.getAttribute("locale_SourceResult_Success") + JSONresponse.getString("message");
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
			messageToDisplay = (String)request.getAttribute("locale_SourceResult_Error") + JSONresponse.getString("message");
		}
	} 
	catch (Exception e) 
	{
		messageToDisplay = (String)request.getAttribute("locale_SourceResult_Error") + e.getMessage() + " " + e.getStackTrace().toString();
	}
} // 



// saveSourceAsTemplate - 
private void saveShareAsTemplate(HttpServletRequest request, HttpServletResponse response) 
{
	try 
	{
		JSONObject source = new JSONObject(sourceJson);
		source.remove("title");
		source.put("title", shareTitle.trim());
		source.put("tags", shareTags.split("(?:\\s*,\\s*|\\s+)"));
		source.remove("description");
		source.put("description", shareDescription.trim());
		if (!shareMediaType.equalsIgnoreCase("null")) {
			source.put("mediaType", shareMediaType);
		}

		// Remove any non-functional things:
		source.remove("_id");
		source.remove("communityIds");
		source.remove("created");
		source.remove("harvest");
		source.remove("harvestBadSource");
		source.remove("isApproved");
		source.remove("key");
		source.remove("modified");
		source.remove("ownerId");
		source.remove("shah256Hash");
		
		sourceJson = source.toString(4);
		
		String urlShareTitle = URLEncoder.encode(shareTitle + " - Template", "UTF-8");
		String urlShareDescription = URLEncoder.encode(shareDescription, "UTF-8");
		String apiAddress = "social/share/add/json/source_template/" + urlShareTitle + "/" + urlShareDescription;
		
		JSONObject JSONresponse = new JSONObject(postToRestfulApi(apiAddress, sourceJson, request, response)).getJSONObject("response");
		if (JSONresponse.getString("success").equalsIgnoreCase("true")) 
		{
			messageToDisplay = (String)request.getAttribute("locale_SourceResult_Success") + JSONresponse.getString("message");
		}
		else
		{
			messageToDisplay = (String)request.getAttribute("locale_SourceResult_Error") + JSONresponse.getString("message");
		}
	} 
	catch (Exception e) 
	{
		messageToDisplay = (String)request.getAttribute("locale_SourceResult_Error") + e.getMessage() + " " + e.getStackTrace().toString();
	}
} //



// deleteShare -
private boolean deleteShare(String shareId, HttpServletRequest request, HttpServletResponse response)
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
				messageToDisplay = (String)request.getAttribute("locale_SourceResult_Success") + JSONresponse.getString("message");
				return true;
			}
			else
			{
				messageToDisplay = (String)request.getAttribute("locale_SourceResult_Error") + JSONresponse.getString("message");
				return false;
			}
		}
		catch (Exception e)
		{
			messageToDisplay = (String)request.getAttribute("locale_SourceResult_Error") + e.getMessage() + " " + e.getStackTrace().toString();
			return false;
		}
	}
	return false;
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
			
			pipelineMode = source.has("processingPipeline");
			
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
			if (source.has("mediaType")) {
				shareMediaType = source.getString("mediaType");
			}
			if ((null == shareMediaType) || shareMediaType.isEmpty()) {
				shareMediaType = "null";
			}
			if (source.has("tags")) {
				StringBuilder stags = new StringBuilder();
				JSONArray arrTags = source.getJSONArray("tags");
				for (int i = 0; i < arrTags.length(); ++i) {
					stags.append(arrTags.get(i)).append(' ');
				}
				shareTags = stags.toString();
			}
			if (source.has("description")) {
				shareDescription = data.getString("description");
			}
			if ((null == shareDescription) || shareDescription.isEmpty()) {
				shareDescription = source.getString("description");				
			}
			if (source.has("searchCycle_secs")) {
				int searchCycle_secs = source.getInt("searchCycle_secs");
				if (searchCycle_secs >= 0) {
					enableOrDisable = (String) request.getAttribute("localized_DisableSource");
				}
				else {
					enableOrDisable = (String) request.getAttribute("locale_EnableSource");
				}
			} 
			else {
				enableOrDisable = (String) request.getAttribute("localized_DisableSource");
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
			
			// Finally, decide whether to show JS-RSS tab
			sourceShowRss = "style=\"display: none\";";
			try {
				String sourceType = source.getString("extractType"); 
				if ((null != sourceType) && sourceType.equalsIgnoreCase("Feed")) {
					JSONObject rss = source.getJSONObject("rss");
					if (null != rss) {
						JSONObject searchConfig = rss.getJSONObject("searchConfig");
						if (null != searchConfig) {
							sourceShowRss = "";
						}
					}
				}
			}catch (Exception e) {} // do nothing, this block doesn't exist
		} 
		catch (Exception e) 
		{
			sourceJson = (String)request.getAttribute("locale_SourceResult_Error") + e.getMessage();
		}
	}
}  // TESTED



// clearForm
private void clearForm()
{
	shareid = "";
	sourceid = "";
	shareTitle = "";
	shareMediaType = "null";
	shareTags = "";
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
	TreeMultimap<String, String> listOfSources = getUserSourcesAndShares(request, response, listFilter);
	
	if (listOfSources.size() > 0)
	{
		sources.append("<table class=\"listTable\" cellpadding=\"3\" cellspacing=\"1\" width=\"100%\" >");

		// Sort the sources alphabetically
		SortedSet<String> sortedKeys = listOfSources.keySet();
		
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
		for (String key : sortedKeys)
		{
			String name = key;
			SortedSet<String> vals = listOfSources.get(key);
			for (String val: vals) {
				if (currentItem >= startItem && currentItem <= endItem)
				{
					String id = val.toString();
					String prefixedid = id;
					String editLink = "";
					String deleteLink = "";
					String listFilterString = "";
					if (listFilter.length() > 0) listFilterString = "&listFilterStr="+ listFilter;
					
					if (name.endsWith(" (*)"))
					{
						editLink = "<a href=\"sources.jsp?action=edit&shareid=" + id + "&page=" + currentPage 
								+ listFilterString + "\" title=\""+(String)request.getAttribute("locale_SourceList_EditShare")+"\">" + name + "</a>";
	
						deleteLink = "<a href=\"sources.jsp?action=delete&shareid=" + id + "&page=" + currentPage 
								+ listFilterString + "\" title=\""+(String)request.getAttribute("locale_SourceList_DeleteShare")+"\" "
								+ "onclick='return confirm(\""+(String)request.getAttribute("locale_SourceList_DeleteShare_Confirm")+" "
								+ name.replace("\"", "\\\"") + "?\");'><img src=\"image/minus_button.png\" border=0></a>";
					}
					else
					{
						prefixedid = "_" + id; // (so we know in the JSP what we're deleting...)
						editLink = "<a href=\"sources.jsp?action=sharefromsource&sourceid=" + id + "&page=" + currentPage 
								+ listFilterString + "\" title=\""+(String)request.getAttribute("locale_SourceList_CreateShare")+"\">" + name + "</a>";
								
						deleteLink = "<a href=\"sources.jsp?action=deletesource&sourceid=" + id + "&page=" + currentPage 
								+ listFilterString + "\" title=\""+(String)request.getAttribute("locale_SourceList_DeleteSource")+"\" "
								+ "onclick='return confirm(\""+(String)request.getAttribute("locale_SourceList_DeleteSource_Confirm")+" "
								+ name.replace("\"", "\\\"") + "?\");'><img src=\"image/delete_x_button.png\" border=0></a>";
					}
		
					// Create the HTML table row
					sources.append("<tr valign=\"top\">");
					if (id.equals(shareid)) {
						sources.append("<td bgcolor=\"white\" width=\"100%\"><b>" + editLink + "</b></td>");						
					}
					else {
						sources.append("<td bgcolor=\"white\" width=\"100%\">" + editLink + "</td>");
					}
					sources.append("<td align=\"center\" bgcolor=\"white\"><input type=\"checkbox\" name=\"sourcesToProcess\" value=\"" + prefixedid + "\"/></td>");
					sources.append("<td align=\"center\" bgcolor=\"white\">" + deleteLink + "</td>");
					sources.append("</tr>");
				}
				currentItem++;
			}
		}
		
		sources.append("<tr valign=\"top\">");
		sources.append("<td bgcolor=\"white\" width=\"100%\" colspan=\"3\">");
		sources.append("(*) " + (String)request.getAttribute("locale_SourceList_TempCopy") + "<br>");
		sources.append("(+) " + (String)request.getAttribute("locale_SourceList_OtherOwner"));
		sources.append("</td>");
		sources.append("</tr>");
		
		// Calculate number of pages, current page, page links...
		sources.append("<tr><td colspan=\"2\" align=\"center\" class=\"subTableFooter\">");
		// --------------------------------------------------------------------------------
		// Create base URL for each page
		StringBuffer baseUrl = new StringBuffer();
		baseUrl.append("sources.jsp?action=page");
		if (listFilter.length() > 0) baseUrl.append('&').append("listFilterStr=").append(listFilter);
		if (shareid.length() > 0) baseUrl.append('&').append("shareid=").append(shareid);
		baseUrl.append("&page=");
		sources.append( createPageString( sortedKeys.size(), itemsToShowPerPage, currentPage, baseUrl.toString() ));
		sources.append("</td></tr>");
		// --------------------------------------------------------------------------------
		sources.append("</table>");
	}
	else
	{
		sources.append(request.getAttribute("locale_SourceList_NoSources"));
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
		if (0 == urlShareTitle.length()) {
			urlShareTitle = "Share+title+goes+here";
		}
		if (0 == urlShareDescription.length()) {
			urlShareDescription = "Share+description+goes+here";
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

// createMediaTypeSelect
private String createMediaTypeSelect(HttpServletRequest request)
{
	StringBuffer html = new StringBuffer();
	String baseList = (String)request.getAttribute("local_mediaType_values");
	if (null == baseList) { // (default set if none specified)
		baseList = "News, Social, Report, Record, Blog, Intel, Discussion, Video, Imagery";
	}
	String customElement = (String)request.getAttribute("local_mediaType_custom");
	if (null == customElement) {
		customElement = "Custom (enter into JSON below)";
	}
	String[] baseElements = baseList.split("\\s*,\\s*");
	boolean selected = false;
	for (String baseElement: baseElements) {
		if (!selected && baseElement.equalsIgnoreCase(shareMediaType)) {
			html.append("<option selected=\"selected\"'>");
			selected = true;
		}
		else {
			html.append("<option>");
		}
		html.append(baseElement).append("</option>\n");
	}
	if (null != customElement) {
		if (!selected) {
			html.append("<option selected=\"selected\" value=\"null\"'>");
		}
		else {
			html.append("<option value=\"null\">");
		}
		html.append(customElement).append("</option>\n");		
	}
	return html.toString();
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
		if (numDocs < 1 || numDocs > 100) numDocs = 10;
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
		// (just used for )
		JSONObject source = new JSONObject(sourceJson);
		pipelineMode = source.has("processingPipeline");
		
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
			messageToDisplay = (String)request.getAttribute("locale_SourceResult_Test") + JSONresponse.getString("message");
		}
		if (harvesterOutput.length() < 1) harvesterOutput = " ";
	}
	catch (Exception e)
	{
		messageToDisplay = (String)request.getAttribute("locale_SourceResult_Error") + e.getMessage() + " " + e.getStackTrace().toString();
	}
} // TESTED


// deleteSourceObject -
private boolean deleteSourceObject(String sourceId, boolean bDocsOnly, HttpServletRequest request, HttpServletResponse response)
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
				messageToDisplay = (String)request.getAttribute("locale_SourceResult_Success") + JSONresponse.getString("message");
				return true;
			}
			else
			{
				messageToDisplay = (String)request.getAttribute("locale_SourceResult_Error") + JSONresponse.getString("message");
				return false;
			}
		}
		catch (Exception e)
		{
			messageToDisplay = (String)request.getAttribute("locale_SourceResult_Error") + e.getMessage() + " " + e.getStackTrace().toString();
			return false;
		}
	}
	return false;
}

//suspend/resume source
private boolean suspendSourceObject(String sourceId, boolean shouldSuspend, HttpServletRequest request, HttpServletResponse response)
{
	if (sourceId != null && sourceId != "") 
	{
		try 
		{
			JSONObject sourceResponse = new JSONObject( getSource(sourceId, request, response) );
			JSONObject source = new JSONObject( sourceResponse.getString("data") );
			JSONArray com = source.getJSONArray("communityIds");
			String tempCommunityId = com.getString(0);
					
			JSONObject JSONresponse = new JSONObject(suspendSource(sourceId, shouldSuspend, tempCommunityId, 
					request, response)).getJSONObject("response");
			
			if (JSONresponse.getString("success").equalsIgnoreCase("true")) 
			{				
				return true;
			}
			else
			{				
				return false;
			}
		}
		catch (Exception e)
		{			
			return false;
		}
	}
	return false;
}

private String getSourceId(String id, HttpServletRequest request, HttpServletResponse response)
{
	String sourceId = null;
	if (id.startsWith("_")) 
	{
		//is just a source, send this id
		sourceId = id.substring(1);
	}
	else
	{
		try
		{
			//share objects that are connected to sources will have an _id so get
			//the share object and check for that
			JSONObject source = getSourceJSONObjectFromShare(id, request, response);
			if ( source != null && source.has("_id") )
			{
				sourceId = source.getString("_id");
			}
		}
		catch (Exception ex)
		{
			//do nothing, let it be null
		}
	}
	return sourceId;
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
  private org.apache.jasper.runtime.TagHandlerPool _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fscope_005fkey_005fnobody;
  private org.apache.jasper.runtime.TagHandlerPool _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fkey_005fnobody;
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
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fscope_005fkey_005fnobody = org.apache.jasper.runtime.TagHandlerPool.getTagHandlerPool(getServletConfig());
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fkey_005fnobody = org.apache.jasper.runtime.TagHandlerPool.getTagHandlerPool(getServletConfig());
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody = org.apache.jasper.runtime.TagHandlerPool.getTagHandlerPool(getServletConfig());
    _el_expressionfactory = _jspxFactory.getJspApplicationContext(getServletConfig().getServletContext()).getExpressionFactory();
    _jsp_annotationprocessor = (org.apache.AnnotationProcessor) getServletConfig().getServletContext().getAttribute(org.apache.AnnotationProcessor.class.getName());
  }

  public void _jspDestroy() {
    _005fjspx_005ftagPool_005fc_005fset_0026_005fvar_005fvalue_005fnobody.release();
    _005fjspx_005ftagPool_005ffmt_005fsetLocale_0026_005fvalue_005fnobody.release();
    _005fjspx_005ftagPool_005ffmt_005fsetBundle_0026_005fbasename_005fnobody.release();
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fscope_005fkey_005fnobody.release();
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fkey_005fnobody.release();
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
			API_ROOT = "http://localhost:8080/api/";
			//API_ROOT = "http://localhost:8184/";
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
      out.write("<!-- Optional localized variables -->\r\n");
      out.write("\r\n");
      if (_jspx_meth_fmt_005fmessage_005f0(_jspx_page_context))
        return;
      out.write('\r');
      out.write('\n');
      if (_jspx_meth_fmt_005fmessage_005f1(_jspx_page_context))
        return;
      out.write("\r\n");
      out.write("\r\n");
      if (_jspx_meth_fmt_005fmessage_005f2(_jspx_page_context))
        return;
      out.write('\r');
      out.write('\n');
      if (_jspx_meth_fmt_005fmessage_005f3(_jspx_page_context))
        return;
      out.write('\r');
      out.write('\n');
      if (_jspx_meth_fmt_005fmessage_005f4(_jspx_page_context))
        return;
      out.write("\r\n");
      out.write("\r\n");
      if (_jspx_meth_fmt_005fmessage_005f5(_jspx_page_context))
        return;
      out.write('\r');
      out.write('\n');
      if (_jspx_meth_fmt_005fmessage_005f6(_jspx_page_context))
        return;
      out.write('\r');
      out.write('\n');
      if (_jspx_meth_fmt_005fmessage_005f7(_jspx_page_context))
        return;
      out.write('\r');
      out.write('\n');
      if (_jspx_meth_fmt_005fmessage_005f8(_jspx_page_context))
        return;
      out.write('\r');
      out.write('\n');
      if (_jspx_meth_fmt_005fmessage_005f9(_jspx_page_context))
        return;
      out.write('\r');
      out.write('\n');
      if (_jspx_meth_fmt_005fmessage_005f10(_jspx_page_context))
        return;
      out.write('\r');
      out.write('\n');
      if (_jspx_meth_fmt_005fmessage_005f11(_jspx_page_context))
        return;
      out.write("\r\n");
      out.write("\r\n");
      if (_jspx_meth_fmt_005fmessage_005f12(_jspx_page_context))
        return;
      out.write('\r');
      out.write('\n');
      if (_jspx_meth_fmt_005fmessage_005f13(_jspx_page_context))
        return;
      out.write('\r');
      out.write('\n');
      if (_jspx_meth_fmt_005fmessage_005f14(_jspx_page_context))
        return;
      out.write('\r');
      out.write('\n');
      if (_jspx_meth_fmt_005fmessage_005f15(_jspx_page_context))
        return;
      out.write('\r');
      out.write('\n');
      if (_jspx_meth_fmt_005fmessage_005f16(_jspx_page_context))
        return;
      out.write('\r');
      out.write('\n');
      if (_jspx_meth_fmt_005fmessage_005f17(_jspx_page_context))
        return;
      out.write('\r');
      out.write('\n');
      if (_jspx_meth_fmt_005fmessage_005f18(_jspx_page_context))
        return;
      out.write('\r');
      out.write('\n');
      if (_jspx_meth_fmt_005fmessage_005f19(_jspx_page_context))
        return;
      out.write("\r\n");
      out.write("\r\n");
      if (_jspx_meth_fmt_005fmessage_005f20(_jspx_page_context))
        return;
      out.write('\r');
      out.write('\n');
      if (_jspx_meth_fmt_005fmessage_005f21(_jspx_page_context))
        return;
      out.write("\r\n");
      out.write("\r\n");
      out.write("<!-- JSP logic -->\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");

	messageToDisplay = "";
	enableOrDisable = (String)request.getAttribute("localized_DisableSource");
	
	// Check if source builder is installed:
	String baseDir =  System.getProperty("catalina.base") + "/webapps/infinit.e.source.builder";
	enterpriseMode = new File(baseDir).exists(); 	
	
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
		if (request.getParameter("deleteSelected") != null) action = request.getParameter("deleteSelected").toLowerCase();
		if (request.getParameter("deleteDocsFromSelected") != null) action = request.getParameter("deleteDocsFromSelected").toLowerCase();
		if (request.getParameter("suspendSelected") != null) action = request.getParameter("suspendSelected").toLowerCase();
		if (request.getParameter("resumeSelected") != null) action = request.getParameter("resumeSelected").toLowerCase();
		
		if (request.getParameter("testSource") != null) action = "testSource";
		if (request.getParameter("saveSource") != null) action = "saveSource";
		if (request.getParameter("saveSourceAsTemplate") != null) action = "saveSourceAsTemplate";
		if (request.getParameter("publishSource") != null) action = "publishSource";
		if (request.getParameter("deleteDocs") != null) action = "deleteDocs";
		if (request.getParameter("newSource") != null) action = "newSource";
		if (request.getParameter("revertSource") != null) action = "revertSource";
		
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
			shareMediaType = (request.getParameter("shareMediaType") != null) ? request.getParameter("shareMediaType") : "";
			shareTags = (request.getParameter("shareTags") != null) ? request.getParameter("shareTags") : "";
			shareTitle = org.apache.commons.lang.StringEscapeUtils.unescapeHtml(shareTitle);
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
				out.println("<meta http-equiv=\"refresh\" content=\"0;url=sources.jsp?page=" + currentPage + "\">");
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
			else if (action.equals("revertSource"))
			{				
				// First delete the existing share:
				deleteShare(shareid, request, response);
				
				// Then Create a new share from the source object
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
				if (listFilter.length() > 0) listFilterString = "&listFilterStr="+ listFilter;
				out.println("<meta http-equiv=\"refresh\" content=\"0;url=sources.jsp" + "?page=" + currentPage + listFilterString + "\">");
			}
			else if (action.equals("deletesource")) 
			{
				deleteSourceObject(sourceid, false, request, response);
				String listFilterString = "";
				if (listFilter.length() > 0) listFilterString = "&listFilterStr="+ listFilter;
				out.println("<meta http-equiv=\"refresh\" content=\"0;url=sources.jsp" + "?page=" + currentPage + listFilterString + "\">");
			}
			else if (action.equals("deleteselected")) 
			{
				String[] ids= request.getParameterValues("sourcesToProcess");
				
				int nDeletedShares = 0;
				int nDeletedSources = 0;
				int nFailed = 0;
				for (String id: ids) {
					if (id.startsWith("_")) {
						id = id.substring(1);
						if (!deleteSourceObject(id, false, request, response)) {
							nFailed++;
						}
						else nDeletedSources++;						
					}
					else {
						if (!deleteShare(id, request, response)) {
							nFailed++;
						}
						else nDeletedShares++;						
					}
				}
				messageToDisplay = String.format((String)request.getAttribute("locale_SourceResult_SourceBulkDeletion"), 
													(Object)nDeletedSources, (Object)nDeletedShares, (Object)nFailed);
				
				out.print("<meta http-equiv=\"refresh\" content=\"0;url=sources.jsp?page=" + currentPage);
				if (listFilter.length() > 0) {
					out.print("&listFilterStr=" + listFilter);					
				}
				out.println("\">");
			}
			else if (action.equals("deletedocsfromselected")) 
			{
				String[] ids= request.getParameterValues("sourcesToProcess");
				
				int nDeletedShares = 0;
				int nDeletedSources = 0;
				int nFailed = 0;
				for (String id: ids) {
					if (id.startsWith("_")) {
						id = id.substring(1);
						if (!deleteSourceObject(id, true, request, response)) {
							nFailed++;
						}
						else nDeletedSources++;						
					}
					else {
						nFailed++;
					}
				}
				messageToDisplay = String.format((String)request.getAttribute("locale_SourceResult_MixedBulkDeletion"), 
													(Object)nDeletedSources, (Object)nFailed);

				if (nFailed > 0) {
					messageToDisplay += " " + (String)request.getAttribute("locale_SourceResult_MixedBulkDeletionFail");
				}				
				out.print("<meta http-equiv=\"refresh\" content=\"0;url=sources.jsp?page=" + currentPage);
				if (listFilter.length() > 0) {
					out.print("&listFilterStr=" + listFilter);					
				}
				out.println("\">");
			}
			else if ( action.equals("suspendselected"))
			{
				int num_suspended = 0;
				int num_failed = 0;
				String[] ids= request.getParameterValues("sourcesToProcess");				
				
				if ( ids != null )
				{					
					for (String id: ids) 
					{
						id = getSourceId(id, request, response);
						if ( id != null )
						{
							if (!suspendSourceObject(id, true, request, response)) 
								num_failed++;
							else 
								num_suspended++;
						}												
					}
				}				
				messageToDisplay = String.format((String)request.getAttribute("locale_SourceResult_SourceBulkSuspend"), 
													 (Object)num_suspended, (Object)num_failed);
				
				out.print("<meta http-equiv=\"refresh\" content=\"0;url=sources.jsp?page=" + currentPage);
				if (listFilter.length() > 0) 
				{
					out.print("&listFilterStr=" + listFilter);					
				}
				out.println("\">");
			}
			else if ( action.equals("resumeselected"))
			{
				int num_suspended = 0;
				int num_failed = 0;
				String[] ids= request.getParameterValues("sourcesToProcess");				
				
				if ( ids != null )
				{
					for (String id: ids) 
					{
						id = getSourceId(id, request, response);
						if ( id != null )
						{
							if (!suspendSourceObject(id, false, request, response)) 
								num_failed++;
							else 
								num_suspended++;
						}										
					}
				}				
				messageToDisplay = String.format((String)request.getAttribute("locale_SourceResult_SourceBulkResume"), 
													 (Object)num_suspended, (Object)num_failed);
				
				out.print("<meta http-equiv=\"refresh\" content=\"0;url=sources.jsp?page=" + currentPage);
				if (listFilter.length() > 0) 
				{
					out.print("&listFilterStr=" + listFilter);					
				}
				out.println("\">");
			}
			else if (action.equals("filterlist")) 
			{
				currentPage = 1;
				populateEditForm(shareid, request, response);
			}
			else if (action.equals("clearfilter")) 
			{
				currentPage = 1;
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
				if (listFilter.length() > 0) listFilterString = "&listFilterStr="+ listFilter;
				out.println("<meta http-equiv=\"refresh\" content=\"0;url=sources.jsp" + "?page=" + currentPage + listFilterString + "\">");
			}
			else if (action.equals("deleteDocs")) 
			{
				deleteSourceObject(sourceid, true, request, response);
				String listFilterString = "";
				if (listFilter.length() > 0) listFilterString = "&listFilterStr="+ listFilter;
				out.println("<meta http-equiv=\"refresh\" content=\"0;url=sources.jsp?action=edit&shareid=" + shareid + "&page=" + currentPage + listFilterString + "\">");
			}
			else if (action.equals("newSource")) 
			{
				out.println("<meta http-equiv=\"refresh\" content=\"0;url=newsource.jsp\">");
			}
			else if (action.equals("testSource")) 
			{
				testSource(request, response);
			}
			else if (action.equals("logout")) 
			{
				logOut(request, response);
				out.println("<meta http-equiv=\"refresh\" content=\"0;url=index.jsp\">");
			}
			else {
				populateEditForm(shareid, request, response);				
			}
			
			createCommunityIdSelect(request, response);
			mediaTypeSelect = createMediaTypeSelect(request);
		}
		catch (Exception e)
		{
			System.out.println(e.getMessage());
		}
	}
	
	
	

      out.write("\r\n");
      out.write("\r\n");
      out.write("<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">\r\n");
      out.write("<html>\r\n");
      out.write("<head>\r\n");
      out.write("\t<meta http-equiv=\"Content-Type\" content=\"text/html; charset=ISO-8859-1\">\r\n");
      out.write("\t\r\n");
      out.write("\t<link rel=\"stylesheet\" type=\"text/css\" href=\"inc/manager.css\" />\r\n");
      out.write("\t\r\n");
      out.write("\t<script type=\"text/javascript\" src=\"inc/utilities.js\"></script>\r\n");
      out.write("\t<link rel=\"shortcut icon\" href=\"image/favicon.ico\" />\r\n");
      out.write("\t\r\n");
      out.write("   <script src=\"lib/jquery.js\"></script>\r\n");
      out.write("   <script src=\"lib/jquery.cookie.js\"></script>\r\n");
      out.write("   \r\n");
      out.write("    <script src=\"lib/splitter.js\"></script>\r\n");
      out.write("    \r\n");
      out.write("   \t<script type=\"text/javascript\" src=\"lib/codemirror.js\"></script>\r\n");
      out.write("   \t<script type=\"text/javascript\" src=\"lib/languages/javascript.js\"></script>\r\n");
      out.write("\t<link rel=\"stylesheet\" type=\"text/css\" href=\"lib/codemirror.css\" />\r\n");
      out.write("    <script src=\"lib/codemirror_extra/dialog/dialog.js\"></script>\r\n");
      out.write("    <link rel=\"stylesheet\" href=\"lib/codemirror_extra/dialog/dialog.css\"/>\r\n");
      out.write("    <script src=\"lib/codemirror_extra/search/searchcursor.js\"></script>\r\n");
      out.write("    <script src=\"lib/codemirror_extra/search/search.js\"></script>\r\n");
      out.write("    <script src=\"lib/codemirror_extra/edit/matchbrackets.js\"></script>\r\n");
      out.write("    <script src=\"lib/codemirror_extra/fold/foldcode.js\"></script>\r\n");
      out.write("    <script src=\"lib/codemirror_extra/fold/brace-fold.js\"></script>\r\n");
      out.write("    \r\n");
      out.write("    <script src=\"lib/jshint.js\"></script>\r\n");
      out.write("\t\r\n");
      out.write("<style media=\"screen\" type=\"text/css\">\r\n");
      out.write("\r\n");
      out.write("input.rightButton {\r\n");
      out.write("    float: right;\r\n");
      out.write("}\r\n");
      out.write("\t\r\n");
      out.write("#lrSplitter {\r\n");
      out.write("\twidth: 100%;\r\n");
      out.write("\theight: 800px;\r\n");
      out.write("}\r\n");
      out.write("#tbSplitter {\r\n");
      out.write("\theight: 700px;\r\n");
      out.write("}\r\n");
      out.write("#lrSplitter .Pane {\r\n");
      out.write("\toverflow: auto;\r\n");
      out.write("}\r\n");
      out.write("#Right {\r\n");
      out.write("\toverflow: hidden;\r\n");
      out.write("}\r\n");
      out.write(".vsplitbar {\r\n");
      out.write("\twidth: 3px;\r\n");
      out.write("\tbackground: #999999 no-repeat center;\r\n");
      out.write("\t/* No margin, border, or padding allowed */\r\n");
      out.write("}\r\n");
      out.write(".vsplitbar.active, .vsplitbar:hover {\r\n");
      out.write("\tbackground: #e88 no-repeat center;\r\n");
      out.write("}\r\n");
      out.write(".hsplitbar {\r\n");
      out.write("\theight: 3px;\r\n");
      out.write("\tbackground: #999999 no-repeat center;\r\n");
      out.write("\t/* No margin, border, or padding allowed */\r\n");
      out.write("}\r\n");
      out.write(".hsplitbar.active, .hsplitbar:hover {\r\n");
      out.write("\tbackground: #e88 no-repeat center;\r\n");
      out.write("}\r\n");
      out.write(".CodeMirror { border-width:1px; border-style: solid; border-color:#DBDFE6; }\r\n");
      out.write(".CodeMirror-foldmarker {\r\n");
      out.write("        color: blue;\r\n");
      out.write("        text-shadow: #b9f 1px 1px 2px, #b9f -1px -1px 2px, #b9f 1px -1px 2px, #b9f -1px 1px 2px;\r\n");
      out.write("        font-family: arial;\r\n");
      out.write("        line-height: .3;\r\n");
      out.write("        cursor: pointer;\r\n");
      out.write("      }\r\n");
      out.write("</style>\r\n");
      out.write("\t\r\n");
      out.write("<script type=\"text/javascript\">\r\n");
      out.write("$().ready(function() {\r\n");
      out.write("\t$(\"#lrSplitter\").splitter({\r\n");
      out.write("\t\ttype: \"v\",\r\n");
      out.write("\t\tsizeLeft: 400, maxLeft: 500,\r\n");
      out.write("\t\toutline: true,\r\n");
      out.write("\t\tcookie: \"lrSplitter\"\r\n");
      out.write("\t});\r\n");
      out.write("});\r\n");
      out.write("$().ready(function() {\r\n");
      out.write("\t$(\"#tbSplitter\").splitter({\r\n");
      out.write("\t\ttype: \"h\",\r\n");
      out.write("\t\tsizeTop: 290, minTop: 62, maxTop: 290,\r\n");
      out.write("\t\toutline: true,\r\n");
      out.write("\t\tcookie: \"tbSplitter\"\r\n");
      out.write("\t});\r\n");
      out.write("});\r\n");
      out.write("</script>\r\n");
      out.write("<script language=javascript>\r\n");
      out.write("\r\n");
      out.write("var currWidth = 0;\r\n");
      out.write("var currHeight = 0;\r\n");
      out.write("\r\n");
      out.write("var int=self.setInterval(function(){clock()},50);\r\n");
      out.write("function clock()\r\n");
      out.write("  {\t\r\n");
      out.write("\tvar newHeight = $('#Bottom').height() - 45;\r\n");
      out.write("\tvar newWidth = $('#Right').width() - 25;\r\n");
      out.write("\t\r\n");
      out.write("\tif ((currWidth != newWidth) || (currHeight != newHeight)) {\r\n");
      out.write("\t\tcurrWidth = newWidth;\r\n");
      out.write("\t\tcurrHeight = newHeight;\r\n");
      out.write("\t\t$(\"#tbSplitter\").css(\"width\", ($('#Right').width() - 20)+\"px\").trigger(\"resize\");\r\n");
      out.write("\t\t$(\"#Top\").css(\"width\", ($('#Right').width() - 20)+\"px\");\r\n");
      out.write("\t\tsourceJsonEditor.setSize(newWidth, newHeight);\r\n");
      out.write("\t\tsourceJsonEditor_sah.setSize(newWidth, newHeight);\r\n");
      out.write("\t\tsourceJsonEditor_uah.setSize(newWidth, newHeight);\r\n");
      out.write("\t\tsourceJsonEditor_rss.setSize(newWidth, newHeight);\r\n");
      out.write("\t}\r\n");
      out.write("  }\r\n");
      out.write("  $(window).resize(function(){\r\n");
      out.write("  \tvar leftWidth = $('#Left').width();\r\n");
      out.write("  \tvar winWidth = $(window).width();\r\n");
      out.write("  \t$(\"#Right\").css(\"width\", (winWidth - leftWidth - 20)+\"px\");\r\n");
      out.write("  });\r\n");
      out.write("</script>\r\n");
      out.write("<script language=javascript>\r\n");
      out.write("\tfunction checkFormat(alertOnSuccess, alertOnFailure)\r\n");
      out.write("\t{\r\n");
      out.write("\t\talertOnFailure = (typeof alertOnFailure != 'undefined') ? alertOnFailure : true;\r\n");
      out.write("\t\tvar editor = sourceJsonEditor;\r\n");
      out.write("\t\tif (alertOnSuccess) { // this is manual mode, work out which editor to check...\r\n");
      out.write("\t\t\tif (\"none\" != sourceJsonEditor_sah.display.wrapper.style.display) {\r\n");
      out.write("\t\t\t\teditor = sourceJsonEditor_sah;\r\n");
      out.write("\t\t\t}\r\n");
      out.write("\t\t\telse if (\"none\" != sourceJsonEditor_uah.display.wrapper.style.display) {\r\n");
      out.write("\t\t\t\teditor = sourceJsonEditor_uah;\r\n");
      out.write("\t\t\t}\t\t\t\r\n");
      out.write("\t\t\telse if (\"none\" != sourceJsonEditor_rss.display.wrapper.style.display) {\r\n");
      out.write("\t\t\t\teditor = sourceJsonEditor_rss;\r\n");
      out.write("\t\t\t}\t\t\t\r\n");
      out.write("\t\t}\r\n");
      out.write("\t\t\r\n");
      out.write("\t\tvar success = JSHINT(editor.getValue());\r\n");
      out.write("\t\tvar output = '';\r\n");
      out.write("\t\tif (!success) {\r\n");
      out.write("\t\t\toutput = \"");
      if (_jspx_meth_fmt_005fmessage_005f22(_jspx_page_context))
        return;
      out.write("\\n\\n\";\r\n");
      out.write("\t\t\tfor (var i in JSHINT.errors) {\r\n");
      out.write("\t\t\t\tvar err = JSHINT.errors[i];\r\n");
      out.write("\t\t\t\tif (null != err) {\r\n");
      out.write("\t\t\t\t\toutput += err.line + '[' + err.character + ']: ' + err.reason + '\\n';\r\n");
      out.write("\t\t\t\t}\r\n");
      out.write("\t\t\t\telse {\r\n");
      out.write("\t\t\t\t\toutput += \"");
      if (_jspx_meth_fmt_005fmessage_005f23(_jspx_page_context))
        return;
      out.write("\\n\";\r\n");
      out.write("\t\t\t\t}\r\n");
      out.write("\t\t\t}\r\n");
      out.write("\t\t}\r\n");
      out.write("\t\tif (success && (editor == sourceJsonEditor)) {\r\n");
      out.write("\t\t\tvar json = eval('('+sourceJsonEditor.getValue()+')');\r\n");
      out.write("\t\t\tif ((null == json.title) || (json.title == \"\")) {\r\n");
      out.write("\t\t\t\toutput = (\"");
      if (_jspx_meth_fmt_005fmessage_005f24(_jspx_page_context))
        return;
      out.write("\\n\");\r\n");
      out.write("\t\t\t\tsuccess = false;\r\n");
      out.write("\t\t\t}\r\n");
      out.write("\t\t\tif ((null == json.description) || (json.description == \"\")) {\r\n");
      out.write("\t\t\t\toutput += (\"");
      if (_jspx_meth_fmt_005fmessage_005f25(_jspx_page_context))
        return;
      out.write("\\n\");\r\n");
      out.write("\t\t\t\tsuccess = false;\r\n");
      out.write("\t\t\t}\r\n");
      out.write("\t\t}\r\n");
      out.write("\t\tif (alertOnSuccess || !success) {\r\n");
      out.write("\t\t\tif (output == \"\") {\r\n");
      out.write("\t\t\t\toutput = \"");
      if (_jspx_meth_fmt_005fmessage_005f26(_jspx_page_context))
        return;
      out.write("\\n\";\r\n");
      out.write("\t\t\t}\r\n");
      out.write("\t\t\tif (success || alertOnFailure) {\r\n");
      out.write("\t\t\t\talert(output);\r\n");
      out.write("\t\t\t}\r\n");
      out.write("\t\t}\r\n");
      out.write("\t\treturn success;\r\n");
      out.write("\t}//TESTED\r\n");
      out.write("\tfunction switchToEditor(the_editor, alertOnFailure)\r\n");
      out.write("\t{\r\n");
      out.write("\t\talertOnFailure = (typeof alertOnFailure != 'undefined') ? alertOnFailure : true;\r\n");
      out.write("\t\t\r\n");
      out.write("\t\t// Check overall JSON format is OK first\r\n");
      out.write("\t\tif (!checkFormat(false, alertOnFailure)) {\r\n");
      out.write("\t\t\treturn;\r\n");
      out.write("\t\t}\r\n");
      out.write("\t\t// Convert source JSON text into JSON\r\n");
      out.write("\t\tvar srcObj = eval('(' + sourceJsonEditor.getValue() + ')');\r\n");
      out.write("\r\n");
      out.write("\t\t// Are we leaving the JSON page?\r\n");
      out.write("\t\tvar old_editor = null;\r\n");
      out.write("\t\tif (\"none\" != sourceJsonEditor.display.wrapper.style.display) {\r\n");
      out.write("\t\t\told_editor = sourceJsonEditor;\r\n");
      out.write("\t\t}//TESTED\r\n");
      out.write("\t\t\r\n");
      out.write("\t\t// Write results back into JSON editor if we're leaving a JS page\r\n");
      out.write("\t\tif (null == old_editor) {\r\n");
      out.write("\t\t\tvar sah = sourceJsonEditor_sah.getValue();\r\n");
      out.write("\t\t\tvar uah = sourceJsonEditor_uah.getValue();\r\n");
      out.write("\t\t\tvar rss = sourceJsonEditor_rss.getValue();\r\n");
      out.write("\r\n");
      out.write("\t\t\tif ((null != sah) && (sah.trim() != \"\")) {\r\n");
      out.write("\t\t\t\tif (null == srcObj.structuredAnalysis) {\r\n");
      out.write("\t\t\t\t\tsrcObj.structuredAnalysis = {};\r\n");
      out.write("\t\t\t\t}\r\n");
      out.write("\t\t\t\tsrcObj.structuredAnalysis.script = sah;\r\n");
      out.write("\t\t\t\tsrcObj.structuredAnalysis.scriptEngine = \"javascript\";\r\n");
      out.write("\t\t\t}\r\n");
      out.write("\r\n");
      out.write("\t\t\tif ((null != uah) && (uah.trim() != \"\")) {\r\n");
      out.write("\t\t\t\tif (null == srcObj.processingPipeline) { // (legacy format)\r\n");
      out.write("\t\t\t\t\tif (null == srcObj.unstructuredAnalysis) {\r\n");
      out.write("\t\t\t\t\t\tsrcObj.unstructuredAnalysis = {};\r\n");
      out.write("\t\t\t\t\t}\r\n");
      out.write("\t\t\t\t\tsrcObj.unstructuredAnalysis.script = uah;\r\n");
      out.write("\t\t\t\t}\r\n");
      out.write("\t\t\t\telse { // Processing pipeline: find the globals block \r\n");
      out.write("\t\t\t\t\tvar globals = null;\r\n");
      out.write("\t\t\t\t\tfor (var x in srcObj.processingPipeline) {\r\n");
      out.write("\t\t\t\t\t\tvar pxPipe = srcObj.processingPipeline[x];\r\n");
      out.write("\t\t\t\t\t\tif (pxPipe.globals) {\r\n");
      out.write("\t\t\t\t\t\t\tglobals = pxPipe.globals;\r\n");
      out.write("\t\t\t\t\t\t\tbreak;\r\n");
      out.write("\t\t\t\t\t\t}\r\n");
      out.write("\t\t\t\t\t}\r\n");
      out.write("\t\t\t\t\tif (null == globals) { // no globals, insert\r\n");
      out.write("\t\t\t\t\t\tglobals = {};\r\n");
      out.write("\t\t\t\t\t\tsrcObj.processingPipeline.splice(1, 0, { \"globals\": globals });\r\n");
      out.write("\t\t\t\t\t}\r\n");
      out.write("\t\t\t\t\tglobals.scriptlang = \"javascript\";\r\n");
      out.write("\t\t\t\t\tif (null == globals.scripts) {\r\n");
      out.write("\t\t\t\t\t\tglobals.scripts = [];\r\n");
      out.write("\t\t\t\t\t}\r\n");
      out.write("\t\t\t\t\tglobals.scripts[0] = uah;\r\n");
      out.write("\t\t\t\t}\r\n");
      out.write("\t\t\t}\r\n");
      out.write("\r\n");
      out.write("\t\t\tif ((null != rss) && (rss.trim() != \"\")) {\r\n");
      out.write("\t\t\t\tif (null == srcObj.rss) {\r\n");
      out.write("\t\t\t\t\tsrcObj.rss = {};\r\n");
      out.write("\t\t\t\t}\r\n");
      out.write("\t\t\t\tif (null == srcObj.rss.searchConfig) {\r\n");
      out.write("\t\t\t\t\tsrcObj.rss.searchConfig = {};\r\n");
      out.write("\t\t\t\t}\r\n");
      out.write("\t\t\t\tsrcObj.rss.searchConfig.globals = rss;\r\n");
      out.write("\t\t\t}\r\n");
      out.write("\t\t\tsourceJsonEditor.setValue(JSON.stringify(srcObj, null, \"    \"));\r\n");
      out.write("\t\t}//TESTED\r\n");
      out.write("\t\telse { // If we are leaving then set the JS contents from the source\r\n");
      out.write("\t\t\t\r\n");
      out.write("\t\t\t// Get script from source\r\n");
      out.write("\t\t\tif ((null != srcObj.structuredAnalysis) && (null != srcObj.structuredAnalysis.script)) {\r\n");
      out.write("\t\t\t\tsourceJsonEditor_sah.setValue(srcObj.structuredAnalysis.script);\t\t\t\t\r\n");
      out.write("\t\t\t}\r\n");
      out.write("\t\t\telse {\r\n");
      out.write("\t\t\t\tsourceJsonEditor_sah.setValue(\"\");\r\n");
      out.write("\t\t\t}\t\t\t\r\n");
      out.write("\t\t\t// Get script from source\r\n");
      out.write("\t\t\tif ((null != srcObj.unstructuredAnalysis) && (null != srcObj.unstructuredAnalysis.script)) {\r\n");
      out.write("\t\t\t\tsourceJsonEditor_uah.setValue(srcObj.unstructuredAnalysis.script);\t\t\t\t\r\n");
      out.write("\t\t\t}\r\n");
      out.write("\t\t\telse if (srcObj.processingPipeline) {\r\n");
      out.write("\t\t\t\tvar globals = null;\r\n");
      out.write("\t\t\t\tfor (var x in srcObj.processingPipeline) {\r\n");
      out.write("\t\t\t\t\tvar pxPipe = srcObj.processingPipeline[x];\r\n");
      out.write("\t\t\t\t\tif (pxPipe.globals) {\r\n");
      out.write("\t\t\t\t\t\tglobals = pxPipe.globals;\r\n");
      out.write("\t\t\t\t\t\tbreak;\r\n");
      out.write("\t\t\t\t\t}\r\n");
      out.write("\t\t\t\t}\r\n");
      out.write("\t\t\t\tif ((null == globals) || (null == globals.scripts) || (0 == globals.scripts.length)) { \r\n");
      out.write("\t\t\t\t\t// no globals, set script to be blank\r\n");
      out.write("\t\t\t\t\tsourceJsonEditor_uah.setValue(\"\");\t\t\t\t\t\r\n");
      out.write("\t\t\t\t}\t\t\t\t\r\n");
      out.write("\t\t\t\telse {\r\n");
      out.write("\t\t\t\t\tsourceJsonEditor_uah.setValue(globals.scripts[0]);\r\n");
      out.write("\t\t\t\t}\r\n");
      out.write("\t\t\t}\r\n");
      out.write("\t\t\telse {\r\n");
      out.write("\t\t\t\tsourceJsonEditor_uah.setValue(\"\");\r\n");
      out.write("\t\t\t}\t\t\t\r\n");
      out.write("\t\t\t// Get script from source\r\n");
      out.write("\t\t\tif ((null != srcObj.rss) && (null != srcObj.rss.searchConfig) && (null != srcObj.rss.searchConfig.globals)) {\r\n");
      out.write("\t\t\t\tsourceJsonEditor_rss.setValue(srcObj.rss.searchConfig.globals);\t\t\t\t\r\n");
      out.write("\t\t\t}\r\n");
      out.write("\t\t\telse {\r\n");
      out.write("\t\t\t\tsourceJsonEditor_rss.setValue(\"\");\r\n");
      out.write("\t\t\t}\t\t\t\r\n");
      out.write("\t\t}//TESTED\r\n");
      out.write("\t\t\r\n");
      out.write("\t\t// Set the display:\r\n");
      out.write("\t\tsourceJsonEditor.display.wrapper.style.display = \"none\";\r\n");
      out.write("\t\tsourceJsonEditor_sah.display.wrapper.style.display = \"none\";\r\n");
      out.write("\t\tsourceJsonEditor_uah.display.wrapper.style.display = \"none\";\t\t\t\r\n");
      out.write("\t\tsourceJsonEditor_rss.display.wrapper.style.display = \"none\";\r\n");
      out.write("\t\tthe_editor.display.wrapper.style.display = null;\r\n");
      out.write("\t\t\r\n");
      out.write("\t\tif (the_editor == sourceJsonEditor) {\r\n");
      out.write("\t\t\t$(\"#toJson\").css(\"font-weight\", \"bold\");\r\n");
      out.write("\t\t\t$(\"#toJsS\").css(\"font-weight\", \"normal\");\r\n");
      out.write("\t\t\t$(\"#toJsU\").css(\"font-weight\", \"normal\");\r\n");
      out.write("\t\t\t$(\"#toJsRss\").css(\"font-weight\", \"normal\");\t\t\t\r\n");
      out.write("\t\t}\r\n");
      out.write("\t\tif (the_editor == sourceJsonEditor_sah) {\r\n");
      out.write("\t\t\t$(\"#toJson\").css(\"font-weight\", \"normal\");\r\n");
      out.write("\t\t\t$(\"#toJsS\").css(\"font-weight\", \"bold\");\r\n");
      out.write("\t\t\t$(\"#toJsU\").css(\"font-weight\", \"normal\");\r\n");
      out.write("\t\t\t$(\"#toJsRss\").css(\"font-weight\", \"normal\");\t\t\t\r\n");
      out.write("\t\t}\r\n");
      out.write("\t\tif (the_editor == sourceJsonEditor_uah) {\r\n");
      out.write("\t\t\t$(\"#toJson\").css(\"font-weight\", \"normal\");\r\n");
      out.write("\t\t\t$(\"#toJsS\").css(\"font-weight\", \"normal\");\r\n");
      out.write("\t\t\t$(\"#toJsU\").css(\"font-weight\", \"bold\");\r\n");
      out.write("\t\t\t$(\"#toJsRss\").css(\"font-weight\", \"normal\");\t\t\t\r\n");
      out.write("\t\t}\r\n");
      out.write("\t\tif (the_editor == sourceJsonEditor_rss) {\r\n");
      out.write("\t\t\t$(\"#toJson\").css(\"font-weight\", \"normal\");\r\n");
      out.write("\t\t\t$(\"#toJsS\").css(\"font-weight\", \"normal\");\r\n");
      out.write("\t\t\t$(\"#toJsU\").css(\"font-weight\", \"normal\");\r\n");
      out.write("\t\t\t$(\"#toJsRss\").css(\"font-weight\", \"bold\");\t\t\t\r\n");
      out.write("\t\t}\r\n");
      out.write("\t\tsourceJsonEditor.refresh();\r\n");
      out.write("\t\tsourceJsonEditor_sah.refresh();\r\n");
      out.write("\t\tsourceJsonEditor_uah.refresh();\r\n");
      out.write("\t\tsourceJsonEditor_rss.refresh();\r\n");
      out.write("\t\tthe_editor.focus();\r\n");
      out.write("\t}//TESTED\r\n");
      out.write("\tfunction removeStatusFields()\r\n");
      out.write("\t{\r\n");
      out.write("\t\t// Check overall JSON format is OK first\r\n");
      out.write("\t\tif (!checkFormat(false)) {\r\n");
      out.write("\t\t\treturn false;\r\n");
      out.write("\t\t}\r\n");
      out.write("\t\t// Convert source JSON text into JSON\r\n");
      out.write("\t\tvar srcObj = eval('(' + sourceJsonEditor.getValue() + ')');\r\n");
      out.write("\t\t\r\n");
      out.write("\t\t// Remove fields we don't care about for config\r\n");
      out.write("\t\tdelete srcObj._id;\r\n");
      out.write("\t\tdelete srcObj.communityIds;\r\n");
      out.write("\t\tdelete srcObj.created;\r\n");
      out.write("\t\tdelete srcObj.harvest;\r\n");
      out.write("\t\tdelete srcObj.harvestBadSource;\r\n");
      out.write("\t\tdelete srcObj.isApproved;\r\n");
      out.write("\t\tdelete srcObj.key;\r\n");
      out.write("\t\tdelete srcObj.modified;\r\n");
      out.write("\t\tdelete srcObj.ownerId;\r\n");
      out.write("\t\tdelete srcObj.shah256Hash;\t\t\r\n");
      out.write("\t\t\r\n");
      out.write("\t\tsourceJsonEditor.setValue(JSON.stringify(srcObj, null, \"    \"));\r\n");
      out.write("\t\treturn true;\r\n");
      out.write("\t}\r\n");
      out.write("\tfunction invertEnabledOrDisabled()\r\n");
      out.write("\t{\r\n");
      out.write("\t\t// Check overall JSON format is OK first\r\n");
      out.write("\t\tif (!checkFormat(false)) {\r\n");
      out.write("\t\t\treturn false;\r\n");
      out.write("\t\t}\r\n");
      out.write("\t\t// Convert source JSON text into JSON\r\n");
      out.write("\t\tvar srcObj = eval('(' + sourceJsonEditor.getValue() + ')');\r\n");
      out.write("\r\n");
      out.write("\t\tif (srcObj.hasOwnProperty('searchCycle_secs')) {\r\n");
      out.write("\t\t\tif (srcObj.searchCycle_secs == -1) {\r\n");
      out.write("\t\t\t\tdelete srcObj.searchCycle_secs;\r\n");
      out.write("\t\t\t\tenableOrDisable.value = \"");
      if (_jspx_meth_fmt_005fmessage_005f27(_jspx_page_context))
        return;
      out.write("\";\r\n");
      out.write("\t\t\t}\r\n");
      out.write("\t\t\telse {\r\n");
      out.write("\t\t\t\tsrcObj.searchCycle_secs = -srcObj.searchCycle_secs;\r\n");
      out.write("\t\t\t\tif (srcObj.searchCycle_secs > 0) {\r\n");
      out.write("\t\t\t\t\tenableOrDisable.value = \"");
      if (_jspx_meth_fmt_005fmessage_005f28(_jspx_page_context))
        return;
      out.write("\";\t\t\t\t\t\r\n");
      out.write("\t\t\t\t}\r\n");
      out.write("\t\t\t\telse {\r\n");
      out.write("\t\t\t\t\tenableOrDisable.value = \"");
      if (_jspx_meth_fmt_005fmessage_005f29(_jspx_page_context))
        return;
      out.write("\";\t\t\t\t\t\t\t\t\t\t\r\n");
      out.write("\t\t\t\t}\r\n");
      out.write("\t\t\t}\r\n");
      out.write("\t\t}\r\n");
      out.write("\t\telse {\r\n");
      out.write("\t\t\tsrcObj.searchCycle_secs = -1;\r\n");
      out.write("\t\t\tenableOrDisable.value = \"");
      if (_jspx_meth_fmt_005fmessage_005f30(_jspx_page_context))
        return;
      out.write("\";\r\n");
      out.write("\t\t}\r\n");
      out.write("\t\tsourceJsonEditor.setValue(JSON.stringify(srcObj, null, \"    \"));\r\n");
      out.write("\t\t$( \"#publishSource\" ).click();\r\n");
      out.write("\t}\r\n");
      out.write("\t\r\n");
      out.write("</script>\r\n");
      out.write("\t<script language=javascript>\r\n");
      out.write("\t\tfunction hideSourceBuilder()\r\n");
      out.write("\t\t{\r\n");
      out.write("\t\t\tif (!checkFormat(false)) {\r\n");
      out.write("\t\t\t\treturn;\r\n");
      out.write("\t\t\t}\r\n");
      out.write("\t\t\tvar pxPipelineStr = document.getElementById('InfinitIframe').contentWindow.getSource();\r\n");
      out.write("\t\t\ttry {\r\n");
      out.write("\t\t\t\tif ('%' == pxPipelineStr.charAt(0)) {\r\n");
      out.write("\t\t\t\t\tpxPipelineStr = decodeURIComponent(pxPipelineStr);\r\n");
      out.write("\t\t\t\t}\r\n");
      out.write("\t\t\t\tvar pipelineObj = eval('(' + pxPipelineStr + ')');\r\n");
      out.write("\t\t\t\t\r\n");
      out.write("\t\t\t\tvar srcObj = eval('(' + sourceJsonEditor.getValue() + ')');\r\n");
      out.write("\t\t\t\tsrcObj.processingPipeline = pipelineObj;\r\n");
      out.write("\t\t\t\tsourceJsonEditor.setValue(JSON.stringify(srcObj, null, \"    \"));\r\n");
      out.write("\t\t\t}\r\n");
      out.write("\t\t\tcatch (err) {\r\n");
      out.write("\t\t\t\tif (!confirm('Error reading GUI config - click OK to continue back to the source editor (WILL LOSE YOUR CHANGES)'))\r\n");
      out.write("\t\t\t\t{\r\n");
      out.write("\t\t\t\t\treturn;\r\n");
      out.write("\t\t\t\t}\r\n");
      out.write("\t\t\t}\t\t\t\r\n");
      out.write("\t\t\t$(sourceBuilder).css(\"width\", \"0%\");\r\n");
      out.write("\t\t\t$(sourceBuilder).css(\"height\", \"0%\");\r\n");
      out.write("\t\t\t$(sourceBuilder_overlay).hide();\r\n");
      out.write("\t\t}\r\n");
      out.write("\t\tfunction showSourceBuilder()\r\n");
      out.write("\t\t{\r\n");
      out.write("\t\t\t// Check overall JSON format is OK first\r\n");
      out.write("\t\t\tif (!checkFormat(false)) {\r\n");
      out.write("\t\t\t\treturn;\r\n");
      out.write("\t\t\t}\r\n");
      out.write("\t\t\t// Convert source JSON text into JSON\r\n");
      out.write("\t\t\tvar srcObj = eval('(' + sourceJsonEditor.getValue() + ')');\r\n");
      out.write("\t\t\tvar pxPipelineStr = JSON.stringify(srcObj.processingPipeline, null, \"    \");\r\n");
      out.write("\t\t\tif (null == pxPipelineStr) {\r\n");
      out.write("\t\t\t\tpxPipelineStr = \"\";\r\n");
      out.write("\t\t\t}\r\n");
      out.write("\t\t\tdocument.getElementById('InfinitIframe').contentWindow.setSource(pxPipelineStr);\r\n");
      out.write("\t\t\t$(sourceBuilder_overlay).show();\r\n");
      out.write("\t\t\t$(sourceBuilder).css(\"z-index\", \"1000\");\r\n");
      out.write("\t\t\t$(sourceBuilder).css(\"width\", \"90%\");\r\n");
      out.write("\t\t\t$(sourceBuilder).css(\"height\", \"90%\");\r\n");
      out.write("\t\t}\r\n");
      out.write("\t</script>\r\n");
      out.write("<title>");
      if (_jspx_meth_fmt_005fmessage_005f31(_jspx_page_context))
        return;
      out.write("</title>\r\n");
      out.write("</head>\r\n");
      out.write("<body>\r\n");
      out.write("\r\n");

	// !-- Create JavaScript Popup --
	if ((messageToDisplay.length() > 0) && 
			(action.equalsIgnoreCase("deleteDocs") || action.equalsIgnoreCase("publishSource") || action.equalsIgnoreCase("saveSourceAsTemplate") 
					|| action.equalsIgnoreCase("delete") || action.equalsIgnoreCase("deletesource")
					|| action.equalsIgnoreCase("deleteSelected") || action.equalsIgnoreCase("deletedocsfromselected")
					|| action.equalsIgnoreCase("suspendSelected") || action.equalsIgnoreCase("resumeSelected")
					))
	{ 

      out.write("\r\n");
      out.write("\t<script language=\"javascript\" type=\"text/javascript\">\r\n");
      out.write("\t\talert('");
      out.print(messageToDisplay.replace("'", "\\'") );
      out.write("');\r\n");
      out.write("\t</script>\r\n");
 } 
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
 
	//!-- Open new window to show source test results --
	if (harvesterOutput.length() > 0) 
	{
		String messageToOutput = URLEncoder.encode(messageToDisplay, "UTF-8");
		String output = URLEncoder.encode(harvesterOutput, "UTF-8");
		harvesterOutput = "";
		messageToDisplay = "";

      out.write("\r\n");
      out.write("\t<script language=\"javascript\" type=\"text/javascript\">\r\n");
      out.write("\t\topenTestSourceWindow(\"");
      if (_jspx_meth_fmt_005fmessage_005f32(_jspx_page_context))
        return;
      out.write("\", '");
      out.print(messageToOutput );
      out.write("', '");
      out.print(output );
      out.write("');\r\n");
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
      if (_jspx_meth_fmt_005fmessage_005f33(_jspx_page_context))
        return;
      out.write('"');
      out.write('>');
      if (_jspx_meth_fmt_005fmessage_005f34(_jspx_page_context))
        return;
      out.write("</a> &nbsp; &nbsp;\r\n");
      out.write("\t\t\t\t\t<a href=\"communities.jsp\" class=\"headerLink\" title=\"");
      if (_jspx_meth_fmt_005fmessage_005f35(_jspx_page_context))
        return;
      out.write('"');
      out.write('>');
      if (_jspx_meth_fmt_005fmessage_005f36(_jspx_page_context))
        return;
      out.write("</a> &nbsp; &nbsp;\r\n");
      out.write("\t\t\t\t\t<a href=\"sources.jsp\" class=\"headerLink\" title=\"");
      if (_jspx_meth_fmt_005fmessage_005f37(_jspx_page_context))
        return;
      out.write('"');
      out.write('>');
      if (_jspx_meth_fmt_005fmessage_005f38(_jspx_page_context))
        return;
      out.write("</a> &nbsp; &nbsp;\r\n");
      out.write("\t\t\t\t\t<a href=\"index.jsp\" class=\"headerLink\" title=\"");
      if (_jspx_meth_fmt_005fmessage_005f39(_jspx_page_context))
        return;
      out.write('"');
      out.write('>');
      if (_jspx_meth_fmt_005fmessage_005f40(_jspx_page_context))
        return;
      out.write("</a> &nbsp; &nbsp;\r\n");
      out.write("\t\t\t\t\t<a href=\"?action=logout\" class=\"headerLink\" title=\"");
      if (_jspx_meth_fmt_005fmessage_005f41(_jspx_page_context))
        return;
      out.write('"');
      out.write('>');
      if (_jspx_meth_fmt_005fmessage_005f42(_jspx_page_context))
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
      out.write("\t <div id=\"lrSplitter\">\r\n");
      out.write("\t\t <div id=\"Left\" class=\"Pane\">\r\n");
      out.write("\t\t\t<table class=\"standardTable\" cellpadding=\"5\" cellspacing=\"1\" width=\"100%\">\r\n");
      out.write("\t\t\t<tr>\r\n");
      out.write("\t\t\t\t<td class=\"headerLink\">");
      if (_jspx_meth_fmt_005fmessage_005f43(_jspx_page_context))
        return;
      out.write("</td>\r\n");
      out.write("\t\t\t\t<td align=\"right\"><input type=\"text\" id=\"listFilter\" \r\n");
      out.write("\t\t\t\t\tonkeydown=\"if (event.keyCode == 13) { setDipatchAction('filterList'); \r\n");
      out.write("\t\t\t\t\tdocument.getElementById('filterList').click(); }\" \r\n");
      out.write("\t\t\t\t\tname=\"listFilter\" size=\"20\" value=\"");
      out.print(listFilter );
      out.write("\"/><button name=\"filterList\" \r\n");
      out.write("\t\t\t\t\tvalue=\"filterList\">");
      if (_jspx_meth_fmt_005fmessage_005f44(_jspx_page_context))
        return;
      out.write("</button><button name=\"clearFilter\" value=\"clearFilter\">");
      if (_jspx_meth_fmt_005fmessage_005f45(_jspx_page_context))
        return;
      out.write("</button></td>\r\n");
      out.write("\t\t\t</tr>\r\n");
      out.write("\t\t\t<tr>\r\n");
      out.write("\t\t\t\t<td colspan=\"2\" bgcolor=\"white\">");
      out.print(listItems(request, response) );
      out.write("</td>\r\n");
      out.write("\t\t\t</tr>\r\n");
      out.write("\t\t\t<tr>\r\n");
      out.write("\t\t\t\t<td colspan=\"2\" >\r\n");
      out.write("\t\t\t\t\t<button name=\"deleteSelected\" onclick=\"return confirm('");
      if (_jspx_meth_fmt_005fmessage_005f46(_jspx_page_context))
        return;
      out.write("');\" name=\"deleteSelected\" value=\"deleteSelected\">");
      if (_jspx_meth_fmt_005fmessage_005f47(_jspx_page_context))
        return;
      out.write("</button>\r\n");
      out.write("\t\t\t\t\t<button name=\"deleteDocsFromSelected\" onclick=\"return confirm('");
      if (_jspx_meth_fmt_005fmessage_005f48(_jspx_page_context))
        return;
      out.write("');\" name=\"deleteDocsFromSelected\" value=\"deleteDocsFromSelected\">");
      if (_jspx_meth_fmt_005fmessage_005f49(_jspx_page_context))
        return;
      out.write("</button>\t\t\t\t\r\n");
      out.write("\t\t\t\t\t<button name=\"suspendSelected\" onclick=\"return confirm('");
      if (_jspx_meth_fmt_005fmessage_005f50(_jspx_page_context))
        return;
      out.write("');\" name=\"suspendSelected\" value=\"suspendSelected\">");
      if (_jspx_meth_fmt_005fmessage_005f51(_jspx_page_context))
        return;
      out.write("</button>\r\n");
      out.write("\t\t\t\t\t<button name=\"resumeSelected\" onclick=\"return confirm('");
      if (_jspx_meth_fmt_005fmessage_005f52(_jspx_page_context))
        return;
      out.write("');\" name=\"resumeSelected\" value=\"resumeSelected\">");
      if (_jspx_meth_fmt_005fmessage_005f53(_jspx_page_context))
        return;
      out.write("</button>\r\n");
      out.write("\t\t\t\t</td>\r\n");
      out.write("\t\t\t</tr>\r\n");
      out.write("\t\t\t</table>\r\n");
      out.write("\t\t</div><!-- Left -->\r\n");
      out.write("\t\t<div id=\"Right\">\r\n");
      out.write("\t\t\t<table class=\"standardTable\" cellpadding=\"5\" cellspacing=\"1\" width=\"100%\">\r\n");
      out.write("\t\t\t<tr>\r\n");
      out.write("\t\t\t\t<td class=\"headerLink\">");
      if (_jspx_meth_fmt_005fmessage_005f54(_jspx_page_context))
        return;
      out.write("</td>\r\n");
      out.write("\t\t\t\t<td align=\"right\"><button name=\"newSource\" value=\"newSource\">");
      if (_jspx_meth_fmt_005fmessage_005f55(_jspx_page_context))
        return;
      out.write("</button></td>\r\n");
      out.write("\t\t\t</tr>\r\n");
      out.write("\t\t\t<tr>\r\n");
      out.write("\t\t\t\t<td colspan=\"2\" bgcolor=\"white\">\r\n");
      out.write("\t\t\t\t\t<div id=\"tbSplitter\">\r\n");
      out.write("\t\t\t\t\t<div id=\"Top\" class=\"Pane\">\r\n");
      out.write("\t\t\t\t\t<table class=\"standardSubTable\" cellpadding=\"3\" cellspacing=\"1\" width=\"100%\" >\r\n");
 if (!shareid.equalsIgnoreCase("")) { 
      out.write("\r\n");
      out.write("\t\t\t\t\t\t<tr>\r\n");
      out.write("\t\t\t\t\t\t\t<td bgcolor=\"white\" width=\"30%\">");
      if (_jspx_meth_fmt_005fmessage_005f56(_jspx_page_context))
        return;
      out.write("</td>\r\n");
      out.write("\t\t\t\t\t\t\t<td bgcolor=\"white\" width=\"70%\" colspan=\"2\">\r\n");
      out.write("\r\n");
      out.write("\t\t\t\t\t\t\t\t<input type=\"button\" \r\n");
      out.write("\t\t\t\t\t\t\t\t\tonclick=\"switchToEditor(sourceJsonEditor, false); if (checkFormat(false)) invertEnabledOrDisabled();\" \r\n");
      out.write("\t\t\t\t\t\t\t\t\tid=\"enableOrDisable\" value=\"");
      out.print( enableOrDisable );
      out.write("\" />\r\n");
      out.write("\t\t\t\t\t\t\t\t<button name=\"testSource\" onclick=\"switchToEditor(sourceJsonEditor, false); return checkFormat(false)\" value=\"testSource\">");
      if (_jspx_meth_fmt_005fmessage_005f57(_jspx_page_context))
        return;
      out.write("</button>\r\n");
      out.write("\t\t\t\t\t\t\t\t<button name=\"saveSource\" onclick=\"switchToEditor(sourceJsonEditor, false); return checkFormat(false)\" value=\"saveSource\">");
      if (_jspx_meth_fmt_005fmessage_005f58(_jspx_page_context))
        return;
      out.write("</button>\r\n");
      out.write("\t\t\t\t\t\t\t\t<button name=\"saveSourceAsTemplate\" onclick=\"switchToEditor(sourceJsonEditor, false); return removeStatusFields()\" value=\"saveSourceAsTemplate\">");
      if (_jspx_meth_fmt_005fmessage_005f59(_jspx_page_context))
        return;
      out.write("</button>\r\n");
      out.write("\t\t\t\t\t\t\t\t\r\n");
      out.write("\t\t\t\t\t\t\t\t<button name=\"publishSource\" value=\"publishSource\" id=\"publishSource\"  \r\n");
      out.write("\t\t\t\t\t\t\t\t\tonclick=\"switchToEditor(sourceJsonEditor, false); if (checkFormat(false) && confirm('");
      if (_jspx_meth_fmt_005fmessage_005f60(_jspx_page_context))
        return;
      out.write("'))  return true; return false;\"\r\n");
      out.write("\t\t\t\t\t\t\t\t\t>");
      if (_jspx_meth_fmt_005fmessage_005f61(_jspx_page_context))
        return;
      out.write("</button>\r\n");
      out.write("\t\t\t\t\t\t\t\t\t\r\n");
 if ((null != sourceid) && !sourceid.equalsIgnoreCase("")) { 
      out.write("\r\n");
      out.write("\t\t\t\t\t\t\t\t<button name=\"deleteDocs\" value=\"deleteDocs\" \r\n");
      out.write("\t\t\t\t\t\t\t\t\tonclick=\"switchToEditor(sourceJsonEditor, false); if (confirm('");
      if (_jspx_meth_fmt_005fmessage_005f62(_jspx_page_context))
        return;
      out.write("')) return true; return false;\"\r\n");
      out.write("\t\t\t\t\t\t\t\t\t>");
      if (_jspx_meth_fmt_005fmessage_005f63(_jspx_page_context))
        return;
      out.write("</button>\r\n");
 } 
      out.write("\r\n");
      out.write("\t\t\t\t\t\t\t</td>\t\t\r\n");
      out.write("\t\t\t\t\t\t</tr>\r\n");
 } 
      out.write("\r\n");
      out.write("\t\t\t\t\t\t<tr>\r\n");
      out.write("\t\t\t\t\t\t\t<td bgcolor=\"white\" width=\"30%\">");
      if (_jspx_meth_fmt_005fmessage_005f64(_jspx_page_context))
        return;
      out.write("</td>\r\n");
      out.write("\t\t\t\t\t\t\t<td bgcolor=\"white\" width=\"70%\"  colspan=\"2\">\r\n");
      out.write("\t\t\t\t\t\t\t\t<input type=\"text\" id=\"shareTitle\" name=\"shareTitle\" value=\"");
      out.print(org.apache.commons.lang.StringEscapeUtils.escapeHtml(shareTitle));
      out.write("\" size=\"60\" />\r\n");
      out.write("\t\t\t\t\t\t\t</td>\t\t\r\n");
      out.write("\t\t\t\t\t\t</tr>\r\n");
      out.write("\t\t\t\t\t\t<tr>\r\n");
      out.write("\t\t\t\t\t\t\t<td bgcolor=\"white\" width=\"30%\">");
      if (_jspx_meth_fmt_005fmessage_005f65(_jspx_page_context))
        return;
      out.write("</td>\r\n");
      out.write("\t\t\t\t\t\t\t<td bgcolor=\"white\" width=\"70%\"  colspan=\"2\">\r\n");
      out.write("\t\t\t\t\t\t\t\t<input type=\"text\" id=\"shareId\" name=\"shareId\" value=\"");
      out.print(shareid);
      out.write("\" size=\"35\" READONLY />\r\n");
      out.write("\t\t\t\t\t\t\t</td>\t\t\r\n");
      out.write("\t\t\t\t\t\t</tr>\r\n");
      out.write("\t\t\t\t\t\t<tr valign=\"top\">\r\n");
      out.write("\t\t\t\t\t\t\t<td bgcolor=\"white\" width=\"30%\">");
      if (_jspx_meth_fmt_005fmessage_005f66(_jspx_page_context))
        return;
      out.write("</td>\r\n");
      out.write("\t\t\t\t\t\t\t<td bgcolor=\"white\" width=\"70%\"  colspan=\"2\">\r\n");
      out.write("\t\t\t\t\t\t\t\t<textarea cols=\"45\" rows=\"3\" id=\"shareDescription\" name=\"shareDescription\">");
      out.print(shareDescription);
      out.write("</textarea>\r\n");
      out.write("\t\t\t\t\t\t\t</td>\t\t\r\n");
      out.write("\t\t\t\t\t\t</tr>\r\n");
      out.write("\t\t\t\t\t\t<tr>\r\n");
      out.write("\t\t\t\t\t\t\t<td bgcolor=\"white\" width=\"30%\">");
      if (_jspx_meth_fmt_005fmessage_005f67(_jspx_page_context))
        return;
      out.write("</td>\r\n");
      out.write("\t\t\t\t\t\t\t\r\n");
      out.write("\t\t\t\t\t\t\t<td bgcolor=\"white\" width=\"28%\">\r\n");
      out.write("\t\t\t\t\t\t\t\t<input type=\"text\" id=\"shareTags\" name=\"shareTags\" value=\"");
      out.print(org.apache.commons.lang.StringEscapeUtils.escapeHtml(shareTags));
      out.write("\" size=\"60\" />\r\n");
      out.write("\t\t\t\t\t\t\t</td>\r\n");
      out.write("\t\t\t\t\t\t\t\r\n");
      out.write("\t\t\t\t\t\t\t<td bgcolor=\"white\" width=\"42%\" align=\"left\">\r\n");
      out.write("\t\t\t\t\t\t\t\t&nbsp;&nbsp;&nbsp;");
      if (_jspx_meth_fmt_005fmessage_005f68(_jspx_page_context))
        return;
      out.write("&nbsp;&nbsp;&nbsp;\r\n");
      out.write("\t\t\t\t\t\t\t\t<select id=\"shareMediaType\" name=\"shareMediaType\">\r\n");
      out.write("\t\t\t\t\t\t\t\t\t");
      out.print( mediaTypeSelect );
      out.write("\r\n");
      out.write("\t\t\t\t\t\t\t\t</select>\r\n");
      out.write("\t\t\t\t\t\t\t</td>\t\t\t\t\t\t\t\t\t\r\n");
      out.write("\t\t\t\t\t\t</tr>\r\n");
      out.write("\t\t\t\t\t\t<tr>\r\n");
      out.write("\t\t\t\t\t\t\t<td bgcolor=\"white\" width=\"30%\">");
      if (_jspx_meth_fmt_005fmessage_005f69(_jspx_page_context))
        return;
      out.write("</td>\r\n");
      out.write("\t\t\t\t\t\t\t<td bgcolor=\"white\" width=\"70%\" colspan=\"2\">");
      out.print(shareOwnerName);
      out.write(' ');
      out.write('-');
      out.write(' ');
      out.print(shareOwnerEmail);
      out.write("</td>\t\t\r\n");
      out.write("\t\t\t\t\t\t</tr>\r\n");
      out.write("\t\t\t\t\t\t<tr>\r\n");
      out.write("\t\t\t\t\t\t\t<td bgcolor=\"white\" width=\"30%\">");
      if (_jspx_meth_fmt_005fmessage_005f70(_jspx_page_context))
        return;
      out.write("</td>\r\n");
      out.write("\t\t\t\t\t\t\t<td bgcolor=\"white\" width=\"70%\" colspan=\"2\">");
      out.print(communityIdSelect);
      out.write("</td>\t\t\r\n");
      out.write("\t\t\t\t\t\t</tr>\r\n");
      out.write("\t\t\t\t\t\t<tr>\r\n");
      out.write("\t\t\t\t\t\t\t<td bgcolor=\"white\" width=\"30%\">");
      if (_jspx_meth_fmt_005fmessage_005f71(_jspx_page_context))
        return;
      out.write("</td>\r\n");
      out.write("\t\t\t\t\t\t\t<td bgcolor=\"white\" width=\"70%\" style=\"height:21px\" colspan=\"2\">\r\n");
      out.write("\t\t\t\t\t\t\t\t");
      if (_jspx_meth_fmt_005fmessage_005f72(_jspx_page_context))
        return;
      out.write(" <input type=\"checkbox\" name=\"fullText\" value=\"true\" ");
      out.print(getFullTextChecked );
      out.write("/>\r\n");
      out.write("\t\t\t\t\t\t\t\t");
      if (_jspx_meth_fmt_005fmessage_005f73(_jspx_page_context))
        return;
      out.write(" <input type=\"text\" id=\"numOfDocs\" name=\"numOfDocs\" value=\"");
      out.print(numberOfDocuments );
      out.write("\"\r\n");
      out.write("\t\t\t\t\t\t\t\t\tsize=\"3\" title=\"Maximum of 100\" />\r\n");
      out.write("\t\t\t\t\t\t\t\t");
      if (_jspx_meth_fmt_005fmessage_005f74(_jspx_page_context))
        return;
      out.write(" <input type=\"checkbox\" name=\"testUpdateLogic\" value=\"true\" ");
      out.print(getTestUpdateLogicChecked );
      out.write("/>\t\t\t\t\t\t\t\r\n");
      out.write("\t\t\t\t\t\t\t</td>\t\t\r\n");
      out.write("\t\t\t\t\t\t</tr>\r\n");
      out.write("\t\t\t\t\t</table>\r\n");
      out.write("\t\t\t\t\t</div>\r\n");
      out.write("\t\t\t\t\t<div id=\"Bottom\" class=\"Pane\">\t\t\t\t\t\r\n");
      out.write("\t\t\t\t\t\t<input type=\"button\" title=\"");
      if (_jspx_meth_fmt_005fmessage_005f75(_jspx_page_context))
        return;
      out.write("\" style=\"font-weight:bold\" onclick=\"switchToEditor(sourceJsonEditor)\" id=\"toJson\" value=\"JSON\" />\r\n");

						// If in pipelineMode 
						if (pipelineMode)
						{

      out.write("\r\n");
      out.write("\t\t\t\t\t\t<input type=\"button\" title=\"");
      if (_jspx_meth_fmt_005fmessage_005f76(_jspx_page_context))
        return;
      out.write("\" onclick=\"switchToEditor(sourceJsonEditor_uah)\" id=\"toJs\" value=\"JS\" />\r\n");

						// If in pipelineMode 
						if (enterpriseMode)
						{

      out.write("\r\n");
      out.write("\t\t\t\t\t\t<input type=\"button\" title=\"");
      if (_jspx_meth_fmt_005fmessage_005f77(_jspx_page_context))
        return;
      out.write("\" onclick=\"showSourceBuilder()\" id=\"toUI\" value=\"UI\" />\r\n");
 } // (end enterpriseMode) 
      out.write('\r');
      out.write('\n');
 } else { 
      out.write("\r\n");
      out.write("\t\t\t\t\t\t<input type=\"button\" title=\"");
      if (_jspx_meth_fmt_005fmessage_005f78(_jspx_page_context))
        return;
      out.write("\" onclick=\"switchToEditor(sourceJsonEditor_uah)\" id=\"toJsU\" value=\"JS-U\" />\r\n");
      out.write("\t\t\t\t\t\t<input type=\"button\" title=\"");
      if (_jspx_meth_fmt_005fmessage_005f79(_jspx_page_context))
        return;
      out.write("\" onclick=\"switchToEditor(sourceJsonEditor_sah)\" id=\"toJsS\" value=\"JS-S\" />\r\n");
      out.write("\t\t\t\t\t\t<input type=\"button\" title=\"");
      if (_jspx_meth_fmt_005fmessage_005f80(_jspx_page_context))
        return;
      out.write("\" onclick=\"switchToEditor(sourceJsonEditor_rss)\" id=\"toJsRss\" value=\"JS-RSS\" ");
      out.print(sourceShowRss);
      out.write(" />\r\n");
 } // (end pipelineMode) 
      out.write("\r\n");
      out.write("\t\t\t\t\t\t\r\n");
      out.write("\t\t\t\t\t\t<input type=\"submit\" class=\"rightButton\" name=\"revertSource\" value=\"");
      if (_jspx_meth_fmt_005fmessage_005f81(_jspx_page_context))
        return;
      out.write("\" onclick=\"return confirm('");
      if (_jspx_meth_fmt_005fmessage_005f82(_jspx_page_context))
        return;
      out.write("');\" value=\"revertSource\"/>\t\t\t\t\r\n");
      out.write("\t\t\t\t\t\t<input type=\"button\" onclick=\"checkFormat(true)\" value=\"");
      if (_jspx_meth_fmt_005fmessage_005f83(_jspx_page_context))
        return;
      out.write("\" class=\"rightButton\" />\r\n");
      out.write("\t\t\t\t\t\t<input type=\"button\" title=\"");
      if (_jspx_meth_fmt_005fmessage_005f84(_jspx_page_context))
        return;
      out.write("\" onclick=\"removeStatusFields()\" value=\"");
      if (_jspx_meth_fmt_005fmessage_005f85(_jspx_page_context))
        return;
      out.write("\" class=\"rightButton\" />\r\n");

						// If in pipelineMode 
						if (!pipelineMode)
						{

      out.write("\r\n");
      out.write("\t\t\t\t\t\t<input type=\"button\" title=\"");
      if (_jspx_meth_fmt_005fmessage_005f86(_jspx_page_context))
        return;
      out.write("\" onclick=\"alert('Not yet supported - coming soon')\" value=\"");
      if (_jspx_meth_fmt_005fmessage_005f87(_jspx_page_context))
        return;
      out.write("\" class=\"rightButton\" />\r\n");
 } // (end !pipelineMode) 
      out.write("\r\n");
      out.write("\t\t\t\t\t\t<textarea cols=\"90\" rows=\"25\" id=\"Source_JSON\" name=\"Source_JSON\">");
      out.print(sourceJson);
      out.write("</textarea>\r\n");
      out.write("\t\t\t\t\t\t<textarea id=\"Source_JSON_uahScript\" name=\"Source_JSON_uahScript\"></textarea>\r\n");
      out.write("\t\t\t\t\t\t<textarea id=\"Source_JSON_sahScript\" name=\"Source_JSON_sahScript\"></textarea>\r\n");
      out.write("\t\t\t\t\t\t<textarea id=\"Source_JSON_rssScript\" name=\"Source_JSON_rssScript\"></textarea>\r\n");
      out.write("\t\t\t\t\t</div>\r\n");
      out.write("\t\t\t\t\t</div>\r\n");
      out.write("\t\t\t\t</td>\r\n");
      out.write("\t\t\t</tr>\r\n");
      out.write("\t\t\t</table>\r\n");
      out.write("\t\t</div><!--  Right -->\r\n");
      out.write("\t</div><!-- lrSplitter -->\r\n");
      out.write("\t<input type=\"hidden\" name=\"sourceid\" id=\"sourceid\" value=\"");
      out.print(sourceid);
      out.write("\"/>\r\n");
      out.write("\t\r\n");
      out.write("\t");
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
      out.write("\r\n");
      out.write("\t</form>\r\n");
      out.write("\t\r\n");
      out.write("<!---------- CodeMirror JavaScripts ---------->\r\n");
      out.write("<script>\r\n");
      out.write("\tvar foldFunc = CodeMirror.newFoldFunction(CodeMirror.braceRangeFinder);\r\n");
      out.write("\tvar sourceJsonEditor_uah = CodeMirror.fromTextArea(document.getElementById(\"Source_JSON_uahScript\"), {\r\n");
      out.write("\t\tmode: \"javascript\",\r\n");
      out.write("\t\tlineNumbers: true,\r\n");
      out.write("\t\tmatchBrackets: true,\r\n");
      out.write("\t\tindentUnit: 4,\r\n");
      out.write("\t\textraKeys: { \"Tab\": \"indentAuto\", \"Ctrl-Q\": function(cm){foldFunc(cm, cm.getCursor().line);}}\r\n");
      out.write("\t});\r\n");
      out.write("\tsourceJsonEditor_uah.setSize(\"100%\", \"100%\");\r\n");
      out.write("\tsourceJsonEditor_uah.display.wrapper.style.display = \"none\";\r\n");
      out.write("\tsourceJsonEditor_uah.on(\"gutterClick\", foldFunc);\r\n");
      out.write("\r\n");
      out.write("\tvar sourceJsonEditor_sah = CodeMirror.fromTextArea(document.getElementById(\"Source_JSON_sahScript\"), {\r\n");
      out.write("\t\tmode: \"javascript\",\r\n");
      out.write("\t\tlineNumbers: true,\r\n");
      out.write("\t\tmatchBrackets: true,\r\n");
      out.write("\t\tindentUnit: 4,\r\n");
      out.write("\t\textraKeys: { \"Tab\": \"indentAuto\", \"Ctrl-Q\": function(cm){foldFunc(cm, cm.getCursor().line);}}\r\n");
      out.write("\t});\r\n");
      out.write("\tsourceJsonEditor_sah.setSize(\"100%\", \"100%\");\r\n");
      out.write("\tsourceJsonEditor_sah.display.wrapper.style.display = \"none\";\r\n");
      out.write("\tsourceJsonEditor_sah.on(\"gutterClick\", foldFunc);\r\n");
      out.write("\t\r\n");
      out.write("\tvar sourceJsonEditor_rss = CodeMirror.fromTextArea(document.getElementById(\"Source_JSON_rssScript\"), {\r\n");
      out.write("\t\tmode: \"javascript\",\r\n");
      out.write("\t\tlineNumbers: true,\r\n");
      out.write("\t\tmatchBrackets: true,\r\n");
      out.write("\t\tindentUnit: 4,\r\n");
      out.write("\t\textraKeys: { \"Tab\": \"indentAuto\", \"Ctrl-Q\": function(cm){foldFunc(cm, cm.getCursor().line);}}\r\n");
      out.write("\t});\r\n");
      out.write("\tsourceJsonEditor_rss.setSize(\"100%\", \"100%\");\r\n");
      out.write("\tsourceJsonEditor_rss.display.wrapper.style.display = \"none\";\r\n");
      out.write("\tsourceJsonEditor_rss.on(\"gutterClick\", foldFunc);\r\n");
      out.write("\t\r\n");
      out.write("\tvar sourceJsonEditor = CodeMirror.fromTextArea(document.getElementById(\"Source_JSON\"), {\r\n");
      out.write("\t\tmode: \"application/json\",\r\n");
      out.write("\t\tlineNumbers: true,\r\n");
      out.write("\t\tmatchBrackets: true,\r\n");
      out.write("\t\tindentUnit: 4,\r\n");
      out.write("\t\textraKeys: { \"Tab\": \"indentAuto\", \"Ctrl-Q\": function(cm){foldFunc(cm, cm.getCursor().line);}}\r\n");
      out.write("\t});\r\n");
      out.write("\tsourceJsonEditor.setSize(\"100%\", \"100%\");\r\n");
      out.write("\tsourceJsonEditor.on(\"gutterClick\", foldFunc);\r\n");
      out.write("\t\r\n");
      out.write("\tsourceJsonEditor.focus();\r\n");
      out.write("</script>\r\n");
      out.write("\t\r\n");
      out.write("\t\r\n");
 } 
      out.write("\r\n");
      out.write("\r\n");

	// If in pipelineMode and enterpriseMode
	if (pipelineMode && enterpriseMode)
	{

      out.write("\r\n");
      out.write("\t<div id=\"sourceBuilder_overlay\" \r\n");
      out.write("\t\t\tstyle=\"width: 100%; height: 100%; position:absolute; top: 0px; left: 0px; z-index: 999; opacity: .5; background-color: Black; display: none;\"\r\n");
      out.write("\t\t\tonclick=\"hideSourceBuilder()\";\r\n");
      out.write("\t\t\t>\r\n");
      out.write("\t</div>\r\n");
      out.write("\t<!--  Don't hide and don't make (0,0) in size - because then the flash contents won't load -->\r\n");
      out.write("\t<div id=\"sourceBuilder\" style=\"width: 10px; height: 10px; position:absolute; top: 50px; left: 5%; z-index: -1; \">\r\n");
      out.write("\t\t<iframe id=\"InfinitIframe\" src=\"../infinit.e.source.builder/Infinit.html\" style=\"width: 100%; height: 100%;\"></iframe>\r\n");
      out.write("\t</div> \r\n");
 } 
      out.write("\r\n");
      out.write("\r\n");
      out.write("</body>\r\n");
      out.write("</html>\r\n");
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
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f0 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fscope_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f0.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f0.setParent(null);
    // /sources.jsp(22,0) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f0.setKey("source.editor.action.disable_source");
    // /sources.jsp(22,0) name = var type = java.lang.String reqTime = false required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f0.setVar("localized_DisableSource");
    // /sources.jsp(22,0) name = scope type = java.lang.String reqTime = false required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f0.setScope("request");
    int _jspx_eval_fmt_005fmessage_005f0 = _jspx_th_fmt_005fmessage_005f0.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f0.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fscope_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f0);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fscope_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f0);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f1(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f1 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fscope_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f1.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f1.setParent(null);
    // /sources.jsp(23,0) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f1.setKey("source.editor.action.enable_source");
    // /sources.jsp(23,0) name = var type = java.lang.String reqTime = false required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f1.setVar("locale_EnableSource");
    // /sources.jsp(23,0) name = scope type = java.lang.String reqTime = false required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f1.setScope("request");
    int _jspx_eval_fmt_005fmessage_005f1 = _jspx_th_fmt_005fmessage_005f1.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f1.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fscope_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f1);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fscope_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f1);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f2(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f2 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fscope_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f2.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f2.setParent(null);
    // /sources.jsp(25,0) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f2.setKey("source.list.temp_copy");
    // /sources.jsp(25,0) name = var type = java.lang.String reqTime = false required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f2.setVar("locale_SourceList_TempCopy");
    // /sources.jsp(25,0) name = scope type = java.lang.String reqTime = false required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f2.setScope("request");
    int _jspx_eval_fmt_005fmessage_005f2 = _jspx_th_fmt_005fmessage_005f2.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f2.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fscope_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f2);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fscope_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f2);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f3(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f3 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fscope_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f3.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f3.setParent(null);
    // /sources.jsp(26,0) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f3.setKey("source.list.other_owner");
    // /sources.jsp(26,0) name = var type = java.lang.String reqTime = false required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f3.setVar("locale_SourceList_OtherOwner");
    // /sources.jsp(26,0) name = scope type = java.lang.String reqTime = false required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f3.setScope("request");
    int _jspx_eval_fmt_005fmessage_005f3 = _jspx_th_fmt_005fmessage_005f3.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f3.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fscope_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f3);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fscope_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f3);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f4(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f4 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fscope_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f4.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f4.setParent(null);
    // /sources.jsp(27,0) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f4.setKey("source.list.no_sources");
    // /sources.jsp(27,0) name = var type = java.lang.String reqTime = false required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f4.setVar("locale_SourceList_NoSources");
    // /sources.jsp(27,0) name = scope type = java.lang.String reqTime = false required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f4.setScope("request");
    int _jspx_eval_fmt_005fmessage_005f4 = _jspx_th_fmt_005fmessage_005f4.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f4.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fscope_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f4);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fscope_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f4);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f5(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f5 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fscope_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f5.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f5.setParent(null);
    // /sources.jsp(29,0) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f5.setKey("source.list.action.edit_share");
    // /sources.jsp(29,0) name = var type = java.lang.String reqTime = false required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f5.setVar("locale_SourceList_EditShare");
    // /sources.jsp(29,0) name = scope type = java.lang.String reqTime = false required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f5.setScope("request");
    int _jspx_eval_fmt_005fmessage_005f5 = _jspx_th_fmt_005fmessage_005f5.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f5.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fscope_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f5);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fscope_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f5);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f6(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f6 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fscope_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f6.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f6.setParent(null);
    // /sources.jsp(30,0) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f6.setKey("source.list.action.create_share");
    // /sources.jsp(30,0) name = var type = java.lang.String reqTime = false required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f6.setVar("locale_SourceList_CreateShare");
    // /sources.jsp(30,0) name = scope type = java.lang.String reqTime = false required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f6.setScope("request");
    int _jspx_eval_fmt_005fmessage_005f6 = _jspx_th_fmt_005fmessage_005f6.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f6.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fscope_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f6);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fscope_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f6);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f7(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f7 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fscope_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f7.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f7.setParent(null);
    // /sources.jsp(31,0) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f7.setKey("source.list.action.delete_share");
    // /sources.jsp(31,0) name = var type = java.lang.String reqTime = false required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f7.setVar("locale_SourceList_DeleteShare");
    // /sources.jsp(31,0) name = scope type = java.lang.String reqTime = false required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f7.setScope("request");
    int _jspx_eval_fmt_005fmessage_005f7 = _jspx_th_fmt_005fmessage_005f7.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f7.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fscope_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f7);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fscope_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f7);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f8(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f8 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fscope_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f8.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f8.setParent(null);
    // /sources.jsp(32,0) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f8.setKey("source.list.action.delete_share.confirm");
    // /sources.jsp(32,0) name = var type = java.lang.String reqTime = false required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f8.setVar("locale_SourceList_DeleteShare_Confirm");
    // /sources.jsp(32,0) name = scope type = java.lang.String reqTime = false required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f8.setScope("request");
    int _jspx_eval_fmt_005fmessage_005f8 = _jspx_th_fmt_005fmessage_005f8.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f8.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fscope_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f8);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fscope_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f8);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f9(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f9 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fscope_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f9.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f9.setParent(null);
    // /sources.jsp(33,0) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f9.setKey("source.list.action.delete_source");
    // /sources.jsp(33,0) name = var type = java.lang.String reqTime = false required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f9.setVar("locale_SourceList_DeleteSource");
    // /sources.jsp(33,0) name = scope type = java.lang.String reqTime = false required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f9.setScope("request");
    int _jspx_eval_fmt_005fmessage_005f9 = _jspx_th_fmt_005fmessage_005f9.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f9.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fscope_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f9);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fscope_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f9);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f10(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f10 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fscope_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f10.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f10.setParent(null);
    // /sources.jsp(34,0) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f10.setKey("source.list.action.delete_source.confirm");
    // /sources.jsp(34,0) name = var type = java.lang.String reqTime = false required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f10.setVar("locale_SourceList_DeleteSource_Confirm");
    // /sources.jsp(34,0) name = scope type = java.lang.String reqTime = false required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f10.setScope("request");
    int _jspx_eval_fmt_005fmessage_005f10 = _jspx_th_fmt_005fmessage_005f10.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f10.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fscope_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f10);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fscope_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f10);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f11(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f11 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f11.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f11.setParent(null);
    // /sources.jsp(35,0) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f11.setKey("source.list.suspend_selected.confirm");
    // /sources.jsp(35,0) name = var type = java.lang.String reqTime = false required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f11.setVar("locale");
    int _jspx_eval_fmt_005fmessage_005f11 = _jspx_th_fmt_005fmessage_005f11.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f11.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f11);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f11);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f12(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f12 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fscope_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f12.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f12.setParent(null);
    // /sources.jsp(37,0) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f12.setKey("source.result.success");
    // /sources.jsp(37,0) name = var type = java.lang.String reqTime = false required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f12.setVar("locale_SourceResult_Success");
    // /sources.jsp(37,0) name = scope type = java.lang.String reqTime = false required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f12.setScope("request");
    int _jspx_eval_fmt_005fmessage_005f12 = _jspx_th_fmt_005fmessage_005f12.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f12.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fscope_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f12);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fscope_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f12);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f13(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f13 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fscope_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f13.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f13.setParent(null);
    // /sources.jsp(38,0) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f13.setKey("source.result.error");
    // /sources.jsp(38,0) name = var type = java.lang.String reqTime = false required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f13.setVar("locale_SourceResult_Error");
    // /sources.jsp(38,0) name = scope type = java.lang.String reqTime = false required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f13.setScope("request");
    int _jspx_eval_fmt_005fmessage_005f13 = _jspx_th_fmt_005fmessage_005f13.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f13.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fscope_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f13);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fscope_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f13);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f14(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f14 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fscope_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f14.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f14.setParent(null);
    // /sources.jsp(39,0) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f14.setKey("source.result.source_bulk_deletion");
    // /sources.jsp(39,0) name = var type = java.lang.String reqTime = false required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f14.setVar("locale_SourceResult_SourceBulkDeletion");
    // /sources.jsp(39,0) name = scope type = java.lang.String reqTime = false required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f14.setScope("request");
    int _jspx_eval_fmt_005fmessage_005f14 = _jspx_th_fmt_005fmessage_005f14.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f14.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fscope_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f14);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fscope_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f14);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f15(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f15 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fscope_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f15.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f15.setParent(null);
    // /sources.jsp(40,0) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f15.setKey("source.result.mixed_bulk_deletion");
    // /sources.jsp(40,0) name = var type = java.lang.String reqTime = false required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f15.setVar("locale_SourceResult_MixedBulkDeletion");
    // /sources.jsp(40,0) name = scope type = java.lang.String reqTime = false required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f15.setScope("request");
    int _jspx_eval_fmt_005fmessage_005f15 = _jspx_th_fmt_005fmessage_005f15.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f15.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fscope_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f15);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fscope_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f15);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f16(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f16 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fscope_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f16.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f16.setParent(null);
    // /sources.jsp(41,0) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f16.setKey("source.result.mixed_bulk_deletion_fail");
    // /sources.jsp(41,0) name = var type = java.lang.String reqTime = false required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f16.setVar("locale_SourceResult_MixedBulkDeletionFail");
    // /sources.jsp(41,0) name = scope type = java.lang.String reqTime = false required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f16.setScope("request");
    int _jspx_eval_fmt_005fmessage_005f16 = _jspx_th_fmt_005fmessage_005f16.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f16.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fscope_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f16);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fscope_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f16);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f17(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f17 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fscope_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f17.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f17.setParent(null);
    // /sources.jsp(42,0) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f17.setKey("source.result.source_bulk_suspend");
    // /sources.jsp(42,0) name = var type = java.lang.String reqTime = false required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f17.setVar("locale_SourceResult_SourceBulkSuspend");
    // /sources.jsp(42,0) name = scope type = java.lang.String reqTime = false required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f17.setScope("request");
    int _jspx_eval_fmt_005fmessage_005f17 = _jspx_th_fmt_005fmessage_005f17.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f17.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fscope_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f17);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fscope_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f17);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f18(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f18 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fscope_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f18.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f18.setParent(null);
    // /sources.jsp(43,0) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f18.setKey("source.result.source_bulk_resume");
    // /sources.jsp(43,0) name = var type = java.lang.String reqTime = false required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f18.setVar("locale_SourceResult_SourceBulkResume");
    // /sources.jsp(43,0) name = scope type = java.lang.String reqTime = false required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f18.setScope("request");
    int _jspx_eval_fmt_005fmessage_005f18 = _jspx_th_fmt_005fmessage_005f18.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f18.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fscope_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f18);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fscope_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f18);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f19(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f19 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fscope_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f19.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f19.setParent(null);
    // /sources.jsp(44,0) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f19.setKey("source.result.test");
    // /sources.jsp(44,0) name = var type = java.lang.String reqTime = false required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f19.setVar("locale_SourceResult_Test");
    // /sources.jsp(44,0) name = scope type = java.lang.String reqTime = false required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f19.setScope("request");
    int _jspx_eval_fmt_005fmessage_005f19 = _jspx_th_fmt_005fmessage_005f19.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f19.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fscope_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f19);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fscope_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f19);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f20(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f20 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fscope_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f20.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f20.setParent(null);
    // /sources.jsp(46,0) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f20.setKey("source.editor.mediaType.values");
    // /sources.jsp(46,0) name = var type = java.lang.String reqTime = false required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f20.setVar("local_mediaType_values");
    // /sources.jsp(46,0) name = scope type = java.lang.String reqTime = false required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f20.setScope("request");
    int _jspx_eval_fmt_005fmessage_005f20 = _jspx_th_fmt_005fmessage_005f20.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f20.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fscope_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f20);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fscope_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f20);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f21(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f21 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fscope_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f21.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f21.setParent(null);
    // /sources.jsp(47,0) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f21.setKey("source.editor.mediaType.custom");
    // /sources.jsp(47,0) name = var type = java.lang.String reqTime = false required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f21.setVar("local_mediaType_custom");
    // /sources.jsp(47,0) name = scope type = java.lang.String reqTime = false required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f21.setScope("request");
    int _jspx_eval_fmt_005fmessage_005f21 = _jspx_th_fmt_005fmessage_005f21.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f21.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fscope_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f21);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fvar_005fscope_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f21);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f22(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f22 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f22.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f22.setParent(null);
    // /sources.jsp(563,13) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f22.setKey("source.result.check_format.error");
    int _jspx_eval_fmt_005fmessage_005f22 = _jspx_th_fmt_005fmessage_005f22.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f22.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f22);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f22);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f23(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f23 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f23.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f23.setParent(null);
    // /sources.jsp(570,16) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f23.setKey("source.result.check_format.unknown_error");
    int _jspx_eval_fmt_005fmessage_005f23 = _jspx_th_fmt_005fmessage_005f23.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f23.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f23);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f23);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f24(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f24 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f24.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f24.setParent(null);
    // /sources.jsp(577,15) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f24.setKey("source.result.check_format.no_title");
    int _jspx_eval_fmt_005fmessage_005f24 = _jspx_th_fmt_005fmessage_005f24.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f24.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f24);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f24);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f25(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f25 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f25.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f25.setParent(null);
    // /sources.jsp(581,16) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f25.setKey("source.result.check_format.no_description");
    int _jspx_eval_fmt_005fmessage_005f25 = _jspx_th_fmt_005fmessage_005f25.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f25.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f25);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f25);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f26(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f26 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f26.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f26.setParent(null);
    // /sources.jsp(587,14) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f26.setKey("source.result.check_format.success");
    int _jspx_eval_fmt_005fmessage_005f26 = _jspx_th_fmt_005fmessage_005f26.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f26.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f26);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f26);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f27(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f27 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f27.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f27.setParent(null);
    // /sources.jsp(780,29) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f27.setKey("source.editor.action.disable_source");
    int _jspx_eval_fmt_005fmessage_005f27 = _jspx_th_fmt_005fmessage_005f27.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f27.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f27);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f27);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f28(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f28 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f28.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f28.setParent(null);
    // /sources.jsp(785,30) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f28.setKey("source.editor.action.disable_source");
    int _jspx_eval_fmt_005fmessage_005f28 = _jspx_th_fmt_005fmessage_005f28.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f28.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f28);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f28);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f29(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f29 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f29.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f29.setParent(null);
    // /sources.jsp(788,30) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f29.setKey("source.editor.action.enable_source");
    int _jspx_eval_fmt_005fmessage_005f29 = _jspx_th_fmt_005fmessage_005f29.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f29.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f29);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f29);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f30(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f30 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f30.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f30.setParent(null);
    // /sources.jsp(794,28) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f30.setKey("source.editor.action.enable_source");
    int _jspx_eval_fmt_005fmessage_005f30 = _jspx_th_fmt_005fmessage_005f30.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f30.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f30);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f30);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f31(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f31 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f31.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f31.setParent(null);
    // /sources.jsp(847,7) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f31.setKey("source.title");
    int _jspx_eval_fmt_005fmessage_005f31 = _jspx_th_fmt_005fmessage_005f31.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f31.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f31);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f31);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f32(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f32 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f32.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f32.setParent(null);
    // /sources.jsp(877,24) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f32.setKey("source.result.test.title");
    int _jspx_eval_fmt_005fmessage_005f32 = _jspx_th_fmt_005fmessage_005f32.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f32.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f32);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f32);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f33(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f33 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f33.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f33.setParent(null);
    // /inc/header.jsp(8,52) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f33.setKey("header.people.description");
    int _jspx_eval_fmt_005fmessage_005f33 = _jspx_th_fmt_005fmessage_005f33.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f33.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f33);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f33);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f34(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f34 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f34.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f34.setParent(null);
    // /inc/header.jsp(8,101) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f34.setKey("header.people.title");
    int _jspx_eval_fmt_005fmessage_005f34 = _jspx_th_fmt_005fmessage_005f34.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f34.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f34);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f34);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f35(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f35 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f35.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f35.setParent(null);
    // /inc/header.jsp(9,57) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f35.setKey("header.communities.description");
    int _jspx_eval_fmt_005fmessage_005f35 = _jspx_th_fmt_005fmessage_005f35.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f35.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f35);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f35);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f36(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f36 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f36.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f36.setParent(null);
    // /inc/header.jsp(9,111) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f36.setKey("header.communities.title");
    int _jspx_eval_fmt_005fmessage_005f36 = _jspx_th_fmt_005fmessage_005f36.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f36.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f36);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f36);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f37(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f37 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f37.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f37.setParent(null);
    // /inc/header.jsp(10,53) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f37.setKey("header.source_editor.description");
    int _jspx_eval_fmt_005fmessage_005f37 = _jspx_th_fmt_005fmessage_005f37.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f37.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f37);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f37);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f38(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f38 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f38.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f38.setParent(null);
    // /inc/header.jsp(10,109) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f38.setKey("header.source_editor.title");
    int _jspx_eval_fmt_005fmessage_005f38 = _jspx_th_fmt_005fmessage_005f38.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f38.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f38);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f38);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f39(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f39 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f39.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f39.setParent(null);
    // /inc/header.jsp(11,51) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f39.setKey("header.home.description");
    int _jspx_eval_fmt_005fmessage_005f39 = _jspx_th_fmt_005fmessage_005f39.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f39.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f39);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f39);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f40(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f40 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f40.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f40.setParent(null);
    // /inc/header.jsp(11,98) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f40.setKey("header.home.title");
    int _jspx_eval_fmt_005fmessage_005f40 = _jspx_th_fmt_005fmessage_005f40.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f40.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f40);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f40);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f41(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f41 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f41.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f41.setParent(null);
    // /inc/header.jsp(12,56) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f41.setKey("header.logout.description");
    int _jspx_eval_fmt_005fmessage_005f41 = _jspx_th_fmt_005fmessage_005f41.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f41.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f41);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f41);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f42(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f42 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f42.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f42.setParent(null);
    // /inc/header.jsp(12,105) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f42.setKey("header.logout.title");
    int _jspx_eval_fmt_005fmessage_005f42 = _jspx_th_fmt_005fmessage_005f42.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f42.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f42);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f42);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f43(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f43 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f43.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f43.setParent(null);
    // /sources.jsp(899,27) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f43.setKey("source.list.name");
    int _jspx_eval_fmt_005fmessage_005f43 = _jspx_th_fmt_005fmessage_005f43.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f43.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f43);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f43);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f44(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f44 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f44.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f44.setParent(null);
    // /sources.jsp(904,24) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f44.setKey("source.list.action.filter");
    int _jspx_eval_fmt_005fmessage_005f44 = _jspx_th_fmt_005fmessage_005f44.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f44.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f44);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f44);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f45(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f45 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f45.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f45.setParent(null);
    // /sources.jsp(904,126) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f45.setKey("source.list.action.clear");
    int _jspx_eval_fmt_005fmessage_005f45 = _jspx_th_fmt_005fmessage_005f45.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f45.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f45);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f45);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f46(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f46 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f46.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f46.setParent(null);
    // /sources.jsp(911,60) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f46.setKey("source.list.delete_selected.confirm");
    int _jspx_eval_fmt_005fmessage_005f46 = _jspx_th_fmt_005fmessage_005f46.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f46.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f46);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f46);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f47(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f47 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f47.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f47.setParent(null);
    // /sources.jsp(911,166) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f47.setKey("source.list.delete_selected");
    int _jspx_eval_fmt_005fmessage_005f47 = _jspx_th_fmt_005fmessage_005f47.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f47.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f47);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f47);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f48(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f48 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f48.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f48.setParent(null);
    // /sources.jsp(912,68) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f48.setKey("source.list.delete_selected_docs.confirm");
    int _jspx_eval_fmt_005fmessage_005f48 = _jspx_th_fmt_005fmessage_005f48.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f48.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f48);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f48);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f49(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f49 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f49.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f49.setParent(null);
    // /sources.jsp(912,195) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f49.setKey("source.list.delete_selected_docs");
    int _jspx_eval_fmt_005fmessage_005f49 = _jspx_th_fmt_005fmessage_005f49.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f49.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f49);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f49);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f50(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f50 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f50.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f50.setParent(null);
    // /sources.jsp(913,61) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f50.setKey("source.list.suspend_selected.confirm");
    int _jspx_eval_fmt_005fmessage_005f50 = _jspx_th_fmt_005fmessage_005f50.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f50.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f50);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f50);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f51(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f51 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f51.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f51.setParent(null);
    // /sources.jsp(913,170) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f51.setKey("source.list.suspend_selected");
    int _jspx_eval_fmt_005fmessage_005f51 = _jspx_th_fmt_005fmessage_005f51.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f51.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f51);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f51);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f52(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f52 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f52.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f52.setParent(null);
    // /sources.jsp(914,60) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f52.setKey("source.list.resume_selected.confirm");
    int _jspx_eval_fmt_005fmessage_005f52 = _jspx_th_fmt_005fmessage_005f52.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f52.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f52);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f52);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f53(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f53 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f53.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f53.setParent(null);
    // /sources.jsp(914,166) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f53.setKey("source.list.resume_selected");
    int _jspx_eval_fmt_005fmessage_005f53 = _jspx_th_fmt_005fmessage_005f53.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f53.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f53);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f53);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f54(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f54 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f54.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f54.setParent(null);
    // /sources.jsp(922,27) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f54.setKey("source.editor.name");
    int _jspx_eval_fmt_005fmessage_005f54 = _jspx_th_fmt_005fmessage_005f54.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f54.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f54);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f54);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f55(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f55 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f55.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f55.setParent(null);
    // /sources.jsp(923,65) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f55.setKey("source.editor.action.new_source");
    int _jspx_eval_fmt_005fmessage_005f55 = _jspx_th_fmt_005fmessage_005f55.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f55.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f55);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f55);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f56(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f56 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f56.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f56.setParent(null);
    // /sources.jsp(932,39) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f56.setKey("source.editor.functions.title");
    int _jspx_eval_fmt_005fmessage_005f56 = _jspx_th_fmt_005fmessage_005f56.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f56.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f56);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f56);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f57(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f57 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f57.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f57.setParent(null);
    // /sources.jsp(938,130) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f57.setKey("source.editor.action.test_source");
    int _jspx_eval_fmt_005fmessage_005f57 = _jspx_th_fmt_005fmessage_005f57.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f57.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f57);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f57);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f58(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f58 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f58.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f58.setParent(null);
    // /sources.jsp(939,130) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f58.setKey("source.editor.action.save_source");
    int _jspx_eval_fmt_005fmessage_005f58 = _jspx_th_fmt_005fmessage_005f58.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f58.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f58);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f58);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f59(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f59 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f59.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f59.setParent(null);
    // /sources.jsp(940,152) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f59.setKey("source.editor.action.save_template");
    int _jspx_eval_fmt_005fmessage_005f59 = _jspx_th_fmt_005fmessage_005f59.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f59.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f59);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f59);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f60(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f60 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f60.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f60.setParent(null);
    // /sources.jsp(943,94) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f60.setKey("source.editor.action.publish_source.confirm");
    int _jspx_eval_fmt_005fmessage_005f60 = _jspx_th_fmt_005fmessage_005f60.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f60.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f60);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f60);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f61(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f61 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f61.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f61.setParent(null);
    // /sources.jsp(944,10) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f61.setKey("source.editor.action.publish_source");
    int _jspx_eval_fmt_005fmessage_005f61 = _jspx_th_fmt_005fmessage_005f61.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f61.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f61);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f61);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f62(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f62 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f62.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f62.setParent(null);
    // /sources.jsp(948,72) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f62.setKey("source.editor.action.delete_docs.confirm");
    int _jspx_eval_fmt_005fmessage_005f62 = _jspx_th_fmt_005fmessage_005f62.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f62.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f62);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f62);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f63(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f63 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f63.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f63.setParent(null);
    // /sources.jsp(949,10) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f63.setKey("source.editor.action.delete_docs");
    int _jspx_eval_fmt_005fmessage_005f63 = _jspx_th_fmt_005fmessage_005f63.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f63.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f63);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f63);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f64(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f64 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f64.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f64.setParent(null);
    // /sources.jsp(955,39) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f64.setKey("source.editor.title.title");
    int _jspx_eval_fmt_005fmessage_005f64 = _jspx_th_fmt_005fmessage_005f64.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f64.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f64);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f64);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f65(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f65 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f65.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f65.setParent(null);
    // /sources.jsp(961,39) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f65.setKey("source.editor.share_id.title");
    int _jspx_eval_fmt_005fmessage_005f65 = _jspx_th_fmt_005fmessage_005f65.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f65.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f65);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f65);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f66(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f66 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f66.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f66.setParent(null);
    // /sources.jsp(967,39) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f66.setKey("source.editor.description.title");
    int _jspx_eval_fmt_005fmessage_005f66 = _jspx_th_fmt_005fmessage_005f66.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f66.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f66);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f66);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f67(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f67 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f67.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f67.setParent(null);
    // /sources.jsp(973,39) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f67.setKey("source.editor.tags.title");
    int _jspx_eval_fmt_005fmessage_005f67 = _jspx_th_fmt_005fmessage_005f67.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f67.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f67);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f67);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f68(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f68 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f68.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f68.setParent(null);
    // /sources.jsp(980,26) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f68.setKey("source.editor.mediaType.title");
    int _jspx_eval_fmt_005fmessage_005f68 = _jspx_th_fmt_005fmessage_005f68.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f68.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f68);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f68);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f69(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f69 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f69.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f69.setParent(null);
    // /sources.jsp(987,39) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f69.setKey("source.editor.owner.title");
    int _jspx_eval_fmt_005fmessage_005f69 = _jspx_th_fmt_005fmessage_005f69.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f69.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f69);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f69);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f70(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f70 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f70.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f70.setParent(null);
    // /sources.jsp(991,39) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f70.setKey("source.editor.community.title");
    int _jspx_eval_fmt_005fmessage_005f70 = _jspx_th_fmt_005fmessage_005f70.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f70.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f70);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f70);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f71(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f71 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f71.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f71.setParent(null);
    // /sources.jsp(995,39) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f71.setKey("source.editor.test_parameters.title");
    int _jspx_eval_fmt_005fmessage_005f71 = _jspx_th_fmt_005fmessage_005f71.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f71.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f71);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f71);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f72(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f72 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f72.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f72.setParent(null);
    // /sources.jsp(997,8) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f72.setKey("source.editor.params.full_text");
    int _jspx_eval_fmt_005fmessage_005f72 = _jspx_th_fmt_005fmessage_005f72.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f72.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f72);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f72);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f73(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f73 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f73.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f73.setParent(null);
    // /sources.jsp(998,8) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f73.setKey("source.editor.params.num_docs");
    int _jspx_eval_fmt_005fmessage_005f73 = _jspx_th_fmt_005fmessage_005f73.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f73.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f73);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f73);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f74(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f74 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f74.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f74.setParent(null);
    // /sources.jsp(1000,8) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f74.setKey("source.editor.params.update_mode");
    int _jspx_eval_fmt_005fmessage_005f74 = _jspx_th_fmt_005fmessage_005f74.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f74.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f74);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f74);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f75(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f75 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f75.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f75.setParent(null);
    // /sources.jsp(1006,34) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f75.setKey("source.code.show_full_source.tooltip");
    int _jspx_eval_fmt_005fmessage_005f75 = _jspx_th_fmt_005fmessage_005f75.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f75.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f75);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f75);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f76(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f76 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f76.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f76.setParent(null);
    // /sources.jsp(1012,34) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f76.setKey("source.code.show_js.tooltip");
    int _jspx_eval_fmt_005fmessage_005f76 = _jspx_th_fmt_005fmessage_005f76.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f76.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f76);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f76);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f77(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f77 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f77.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f77.setParent(null);
    // /sources.jsp(1018,34) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f77.setKey("source.code.show_ui.tooltip");
    int _jspx_eval_fmt_005fmessage_005f77 = _jspx_th_fmt_005fmessage_005f77.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f77.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f77);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f77);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f78(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f78 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f78.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f78.setParent(null);
    // /sources.jsp(1021,34) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f78.setKey("source.code.show_uah.tooltip");
    int _jspx_eval_fmt_005fmessage_005f78 = _jspx_th_fmt_005fmessage_005f78.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f78.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f78);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f78);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f79(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f79 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f79.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f79.setParent(null);
    // /sources.jsp(1022,34) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f79.setKey("source.code.show_sah.tooltip");
    int _jspx_eval_fmt_005fmessage_005f79 = _jspx_th_fmt_005fmessage_005f79.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f79.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f79);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f79);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f80(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f80 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f80.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f80.setParent(null);
    // /sources.jsp(1023,34) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f80.setKey("source.code.show_rss.tooltip");
    int _jspx_eval_fmt_005fmessage_005f80 = _jspx_th_fmt_005fmessage_005f80.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f80.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f80);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f80);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f81(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f81 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f81.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f81.setParent(null);
    // /sources.jsp(1026,74) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f81.setKey("source.code.action.revert");
    int _jspx_eval_fmt_005fmessage_005f81 = _jspx_th_fmt_005fmessage_005f81.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f81.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f81);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f81);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f82(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f82 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f82.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f82.setParent(null);
    // /sources.jsp(1026,147) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f82.setKey("source.code.action.revert.confirm");
    int _jspx_eval_fmt_005fmessage_005f82 = _jspx_th_fmt_005fmessage_005f82.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f82.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f82);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f82);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f83(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f83 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f83.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f83.setParent(null);
    // /sources.jsp(1027,62) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f83.setKey("source.code.action.check_format");
    int _jspx_eval_fmt_005fmessage_005f83 = _jspx_th_fmt_005fmessage_005f83.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f83.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f83);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f83);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f84(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f84 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f84.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f84.setParent(null);
    // /sources.jsp(1028,34) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f84.setKey("source.code.action.scrub.tooltip");
    int _jspx_eval_fmt_005fmessage_005f84 = _jspx_th_fmt_005fmessage_005f84.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f84.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f84);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f84);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f85(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f85 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f85.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f85.setParent(null);
    // /sources.jsp(1028,127) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f85.setKey("source.code.action.scrub");
    int _jspx_eval_fmt_005fmessage_005f85 = _jspx_th_fmt_005fmessage_005f85.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f85.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f85);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f85);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f86(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f86 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f86.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f86.setParent(null);
    // /sources.jsp(1034,34) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f86.setKey("source.code.action.convert.tooltip");
    int _jspx_eval_fmt_005fmessage_005f86 = _jspx_th_fmt_005fmessage_005f86.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f86.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f86);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f86);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f87(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f87 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f87.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f87.setParent(null);
    // /sources.jsp(1034,149) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f87.setKey("source.code.action.convert");
    int _jspx_eval_fmt_005fmessage_005f87 = _jspx_th_fmt_005fmessage_005f87.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f87.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f87);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f87);
    return false;
  }
}
