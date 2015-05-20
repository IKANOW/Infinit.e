/*******************************************************************************
 * Copyright 2012 The Infinit.e Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.ikanow.infinit.e.data_model.driver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.bson.types.ObjectId;

import com.google.common.collect.HashMultimap;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.ikanow.infinit.e.data_model.InfiniteEnums.HarvestEnum;
import com.ikanow.infinit.e.data_model.api.ApiManager;
import com.ikanow.infinit.e.data_model.api.ResponsePojo;
import com.ikanow.infinit.e.data_model.api.ResponsePojo.ResponseObject;
import com.ikanow.infinit.e.data_model.api.authentication.WordPressAuthPojo;
import com.ikanow.infinit.e.data_model.api.authentication.WordPressSetupPojo;
import com.ikanow.infinit.e.data_model.api.authentication.WordPressUserPojo;
import com.ikanow.infinit.e.data_model.api.config.SourcePojoApiMap;
import com.ikanow.infinit.e.data_model.api.custom.mapreduce.CustomMapReduceResultPojo;
import com.ikanow.infinit.e.data_model.api.custom.mapreduce.CustomMapReduceResultPojoApiMap;
import com.ikanow.infinit.e.data_model.api.knowledge.AdvancedQueryPojo;
import com.ikanow.infinit.e.data_model.api.knowledge.DimensionListPojo;
import com.ikanow.infinit.e.data_model.api.knowledge.DocumentPojoApiMap;
import com.ikanow.infinit.e.data_model.api.knowledge.SearchSuggestPojo;
import com.ikanow.infinit.e.data_model.api.social.community.CommunityPojoApiMap;
import com.ikanow.infinit.e.data_model.api.social.person.PersonPojoApiMap;
import com.ikanow.infinit.e.data_model.api.social.sharing.SharePojoApiMap;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.ikanow.infinit.e.data_model.store.custom.mapreduce.CustomMapReduceJobPojo;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.infinit.e.data_model.store.feature.entity.EntityFeaturePojo;
import com.ikanow.infinit.e.data_model.store.social.community.CommunityPojo;
import com.ikanow.infinit.e.data_model.store.social.community.CommunityPojo.CommunityType;
import com.ikanow.infinit.e.data_model.store.social.person.PersonPojo;
import com.ikanow.infinit.e.data_model.store.social.sharing.SharePojo;
import com.ikanow.infinit.e.data_model.store.social.sharing.SharePojo.ShareCommunityPojo;
import com.ikanow.infinit.e.data_model.utils.TrustManagerManipulator;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;

public class InfiniteDriver 
{
	private static String DEFAULT_API_ROOT = null;
	private static String DEFAULT_USER = null;
	private static String DEFAULT_PASSWORD = null;
	
	private String apiRoot;
	private String user;
	private String password;
	private String apiKey = null;
	
	private String cookie = null;
	private String project_id = null;
	
	public InfiniteDriver()
	{
		apiRoot = DEFAULT_API_ROOT;
		user = DEFAULT_USER;
		password = DEFAULT_PASSWORD;
	}
	
	public InfiniteDriver(String apiRootUrl)
	{
		if (apiRootUrl == null)
			apiRoot = DEFAULT_API_ROOT;
		else
			apiRoot = apiRootUrl;
		
		user = DEFAULT_USER;
		password = DEFAULT_PASSWORD;
	}
	
	public InfiniteDriver(String apiRootUrl, String apiKey)
	{
		if (apiRootUrl == null)
			apiRoot = DEFAULT_API_ROOT;
		else
			apiRoot = apiRootUrl;
		
		this.apiKey = "infinitecookie=api:" + apiKey + ";"; // (use it like a cookie to avoid localhost dev issues)
		
		// (unused)
		user = DEFAULT_USER;
		password = DEFAULT_PASSWORD;
	}
	
	public InfiniteDriver(String apiRootUrl, String username, String unencryptedPassword)
	{
		if (apiRootUrl == null)
			apiRoot = DEFAULT_API_ROOT;
		else
			apiRoot = apiRootUrl;
		
		if (username == null || unencryptedPassword == null)
		{
			user = DEFAULT_USER;
			password = DEFAULT_PASSWORD;
		}
		else
		{
			user = username;
			password = unencryptedPassword;
		}
	}
	
	static public void setDefaultUser(String username)
	{
		DEFAULT_USER = username;	
	}
	
	static public void setDefaultPassword(String unencryptedPassword)
	{
		DEFAULT_PASSWORD = unencryptedPassword;
	}
	
	static public void setDefaultApiRoot(String rootUrl)
	{
		DEFAULT_API_ROOT = rootUrl;
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////
	
	// AUTHENTICATION
	
	// If you already have a valid cookie, then use that:
	
	public void useExistingCookie(String existingCookie) {
		cookie = "infinitecookie=" + existingCookie + "; Path=/; HttpOnly";
	}
	
	/**
	 * Returns the currently set cookie, can be used to
	 * get the cookie after you login.
	 * 
	 * @return
	 */
	public String getCookie()
	{
		return cookie;
	}
	
	/**
	 * Logs the user in specified by setUser() using the password
	 * specified by setPassword().
	 * @return true if the user was logged in successfully.
	 */
	public Boolean login()
	{
		return login(new ResponseObject());
	}
	
	public Boolean adminLogin()
	{
		return adminLogin(new ResponseObject());
	}

	/**
	 * Logs the user in specified by setUser() using the password
	 * specified by setPassword().
	 * @param responseObject this object will be said from the response of the API call
	 * @return true if the user was logged in successfully.
	 */
	public Boolean login(ResponseObject responseObject)
	{
		return login (user, password, responseObject);
	}
	
	public Boolean adminLogin(ResponseObject responseObject)
	{
		return adminLogin (user, password, responseObject);
	}

	/**
	 * Logs the user in with the specified username and password.
	 * This overrides the setUser and setPassword commands
	 * @param username The username to log in
	 * @param password the unencrypted password for the username
	 * @param responseObject this object will be said from the response of the API call
	 * @return true if the user was logged in successfully.
	 */
	public Boolean login(String username, String password, ResponseObject responseObject)
	{
		try
		{
			return doLogin(username, encryptWithoutEncode(password), responseObject);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		return null;
	}
	
	public Boolean login_encrypted(String username, String encrypted_password, ResponseObject responseObject)
	{
		return doLogin(username, encrypted_password, responseObject);
	}
	
	private Boolean doLogin(String username, String password, ResponseObject responseObject)
	{
		if (null != apiKey) { // Have an API key, don't need to login....
			return true;
		}		
		cookie = null;
		try 
		{
			String address = apiRoot + "auth/login/" + username + "/" + URLEncoder.encode(password, "UTF-8");
			String loginResult;
			loginResult = sendRequest(address, null);
			ResponsePojo internal_responsePojo = ResponsePojo.fromApi(loginResult, ResponsePojo.class);
			ResponseObject internal_ro = internal_responsePojo.getResponse();
			responseObject = shallowCopy(responseObject, internal_ro);
			return responseObject.isSuccess();
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}

		return null;
	}
	
	public Boolean adminLogin(String username, String password, ResponseObject responseObject)
	{
		// Allow this even if have an API key because it's currently the only way of knowing if you're an admin
		
		cookie = null;
		try {
			String address = apiRoot + "auth/login/admin/" + username + "/" + encryptEncodePassword(password);
			String loginResult;
			loginResult = sendRequest(address, null);
			ResponsePojo internal_responsePojo = ResponsePojo.fromApi(loginResult, ResponsePojo.class);
			ResponseObject internal_ro = internal_responsePojo.getResponse();
			responseObject = shallowCopy(responseObject, internal_ro);
			return responseObject.isSuccess();
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * Logs the current logged in user out.
	 * @return true if logout was successful.
	 */
	public Boolean logout()
	{
		if (null == cookie) { // not logged in, eg have an API key
			return true;
		}
		try {
			String address = apiRoot + "auth/logout/";
			String logoutResult;
			logoutResult = sendRequest(address, null);
			ResponsePojo response = ResponsePojo.fromApi(logoutResult, ResponsePojo.class);

			//cookie set to null so that new cookie will be grabbed on next login
			cookie = null;

			return response.getResponse().isSuccess();
		}
		catch (Exception e) 
		{
			cookie = null;
			return false;
		}
	}

	/**
	 * Sends a keepalive message
	 * @return true if user is currently logged in
	 */
	public Boolean sendKeepalive() {
		return sendKeepalive(false);
	}
	public Boolean sendKeepalive(boolean bAdminOnly) {
		
		try {			
			String address = apiRoot + "auth/keepalive";
			if (bAdminOnly) {
				address += "/admin";
			}
			String logoutResult;
			logoutResult = sendRequest(address, null);
			ResponsePojo response = ResponsePojo.fromApi(logoutResult, ResponsePojo.class);
			return response.getResponse().isSuccess();
		}
		catch (Exception e) 
		{
			return false;
		}
	}
	
	public Boolean deactivateUser(String username)
	{
		try
		{
			String deactivateAddress = apiRoot + "auth/deactivate?username=" + URLEncoder.encode(username,"UTF-8");
			
			String deactivateResult = sendRequest(deactivateAddress, null);

			ResponsePojo response = ResponsePojo.fromApi(deactivateResult, ResponsePojo.class);
			return response.getResponse().isSuccess();

		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return false;
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////
	
	// SOCIAL - COMMUNITIES
	
	public CommunityPojo createCommunity(String communityName, String communityDesc, String communityTags,String parentid, ResponseObject responseObject, CommunityType communityType )
	{
		StringBuilder url = new StringBuilder(apiRoot).append("social/");
		try
		{
			if ( communityType == null)
				url.append("community/");
			else if ( communityType.equals(CommunityType.data))
				url.append("group/data/");
			else if ( communityType.equals(CommunityType.user))
				url.append("group/user/");
			
			url.append("add/").append(URLEncoder.encode(communityName,"UTF-8")).append("/")
				.append(URLEncoder.encode(communityDesc,"UTF-8")).append("/").append(URLEncoder.encode(communityTags,"UTF-8"));
			//String createCommunityAddress = apiRoot + "social/community/add/" + URLEncoder.encode(communityName,"UTF-8") + "/" +
			//		URLEncoder.encode(communityDesc,"UTF-8") + "/" + URLEncoder.encode(communityTags,"UTF-8");
			if (null != parentid)
				url.append("/").append(URLEncoder.encode(parentid,"UTF-8"));
				//createCommunityAddress += "/" + URLEncoder.encode(parentid,"UTF-8");
			
			String communityresult = sendRequest(url.toString(), null);

			ResponsePojo internal_responsePojo = ResponsePojo.fromApi(communityresult, ResponsePojo.class, CommunityPojo.class, new CommunityPojoApiMap()); 
			ResponseObject internal_ro = internal_responsePojo.getResponse();
			responseObject = shallowCopy(responseObject, internal_ro);
			
			return (CommunityPojo)internal_responsePojo.getData();

		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return null;
	}
	
	/**
	 * createCommunity
	 * @param communityName
	 * @param communityDesc
	 * @param communityTag
	 * @param responsePojo
	 * @return CommunityPojo
	 */
	public CommunityPojo createCommunity(String communityName, String communityDesc, String communityTags,String parentid, ResponseObject responseObject )
	{
		return createCommunity(communityName, communityDesc, communityTags, parentid, responseObject, null);
	}

	public String addToCommunity(String communityId, String personId, ResponseObject responseObject )
	{
		try
		{
			String addToCommunityAddress = apiRoot + "social/community/member/invite/" + URLEncoder.encode(communityId,"UTF-8") + "/" +
					URLEncoder.encode(personId,"UTF-8") + "/";
			String inviteresult = sendRequest(addToCommunityAddress, null);

			ResponsePojo internal_responsePojo = ResponsePojo.fromApi(inviteresult, ResponsePojo.class, CommunityPojo.class, new CommunityPojoApiMap()); 
			ResponseObject internal_ro = internal_responsePojo.getResponse();
			responseObject = shallowCopy(responseObject, internal_ro);

			return responseObject.getMessage();

		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return null;
	}
	
	public String joinCommunity(String communityId, ResponseObject responseObject )
	{
		try
		{
			String addToCommunityAddress = apiRoot + "social/community/member/join/" + URLEncoder.encode(communityId,"UTF-8");
			String inviteresult = sendRequest(addToCommunityAddress, null);

			ResponsePojo internal_responsePojo = ResponsePojo.fromApi(inviteresult, ResponsePojo.class, CommunityPojo.class, new CommunityPojoApiMap()); 
			ResponseObject internal_ro = internal_responsePojo.getResponse();
			responseObject = shallowCopy(responseObject, internal_ro);

			return responseObject.getMessage();

		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return null;
	}
	
	public String forcefullyAddToCommunity(String communityId, String personId, ResponseObject responseObject )
	{
		try
		{
			String addToCommunityAddress = apiRoot + "social/community/member/invite/" + URLEncoder.encode(communityId,"UTF-8") + "/" +
					URLEncoder.encode(personId,"UTF-8") + "/?skipinvitation=true";
			String inviteresult = sendRequest(addToCommunityAddress, null);

			ResponsePojo internal_responsePojo = ResponsePojo.fromApi(inviteresult, ResponsePojo.class, CommunityPojo.class, new CommunityPojoApiMap()); 
			ResponseObject internal_ro = internal_responsePojo.getResponse();
			responseObject = shallowCopy(responseObject, internal_ro);

			return responseObject.getMessage();

		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return null;
	}
	
	/**
	 * Helper function that calls the backwards update community member status function that allows
	 * removing other community members
	 * 
	 * @param personId
	 * @param communityId
	 * @param responseObject
	 */
	public void removeUserFromCommunity(String personId, String communityId, ResponseObject responseObject)
	{
		updateCommunityMemberStatus(communityId, personId, "remove", responseObject);		
	}
	
	public CommunityPojo getCommunity(String communityIdentifier, ResponseObject responseObject )
	{
		try
		{
			String getCommunityAddress = apiRoot + "social/community/get/" + URLEncoder.encode(communityIdentifier,"UTF-8") + "";
			String getResult = sendRequest(getCommunityAddress, null);

			ResponsePojo internal_responsePojo = ResponsePojo.fromApi(getResult, ResponsePojo.class, CommunityPojo.class, new CommunityPojoApiMap()); 
			ResponseObject internal_ro = internal_responsePojo.getResponse();
			responseObject = shallowCopy(responseObject, internal_ro);

			return (CommunityPojo)internal_responsePojo.getData();

		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return null;
	}
	
	public List<CommunityPojo> getPublicCommunities(ResponseObject responseObject)
	{
		try
		{
			String getCommunityAddress = apiRoot + "social/community/getpublic";
			String getResult = sendRequest(getCommunityAddress, null);
			
			ResponsePojo internal_responsePojo = ResponsePojo.fromApi(getResult, ResponsePojo.class); 
			ResponseObject internal_ro = internal_responsePojo.getResponse();
			responseObject = shallowCopy(responseObject, internal_ro);
			
			List<CommunityPojo> communities = null;
			
			communities = ApiManager.mapListFromApi((JsonElement)internal_responsePojo.getData(), 
															CommunityPojo.listType(), null);			
			
			return communities;
		}
		catch (Exception e) 
		{
			responseObject.setSuccess(false);
			responseObject.setMessage(e.getMessage());
		}		
		return null;
	}
	
	public List<CommunityPojo> getAllCommunity(ResponseObject responseObject){
		return getAllCommunity(responseObject, null);
	}
	public List<CommunityPojo> getAllCommunity(ResponseObject responseObject, String communityIdStr)
	{
		try
		{
			String getCommunityAddress = apiRoot + "social/community/getall";
			if ( communityIdStr != null )
				getCommunityAddress += "/" + communityIdStr;
			String getResult = sendRequest(getCommunityAddress, null);
			
			ResponsePojo internal_responsePojo = ResponsePojo.fromApi(getResult, ResponsePojo.class); 
			ResponseObject internal_ro = internal_responsePojo.getResponse();
			responseObject = shallowCopy(responseObject, internal_ro);
			
			List<CommunityPojo> communities = null;
			
			communities = ApiManager.mapListFromApi((JsonElement)internal_responsePojo.getData(), 
															CommunityPojo.listType(), null);			
			
			return communities;
		}
		catch (Exception e) 
		{
			responseObject.setSuccess(false);
			responseObject.setMessage(e.getMessage());
		}		
		return null;
	}

	public Boolean deleteCommunity(String communityId, ResponseObject responseObject)
	{
		try
		{
			String addToCommunityAddress = apiRoot + "social/community/remove/" + URLEncoder.encode(communityId,"UTF-8");
			String inviteresult = sendRequest(addToCommunityAddress, null);

			ResponsePojo internal_responsePojo = ResponsePojo.fromApi(inviteresult, ResponsePojo.class); 
			ResponseObject internal_ro = internal_responsePojo.getResponse();
			responseObject = shallowCopy(responseObject, internal_ro);

			return responseObject.isSuccess();

		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return null;
	}

	public Boolean updateCommunity(String communityId, CommunityPojo communityPojo, ResponseObject responseObject)
	{
		try{
			String addToCommunityAddress = apiRoot + "social/community/update/" + URLEncoder.encode(communityId,"UTF-8");
			String updateResult = sendRequest(addToCommunityAddress, new Gson().toJson(communityPojo));
			ResponsePojo internal_responsePojo = ResponsePojo.fromApi(updateResult, ResponsePojo.class); 
			ResponseObject internal_ro = internal_responsePojo.getResponse();
			responseObject = shallowCopy(responseObject, internal_ro);

			return responseObject.isSuccess();

		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return false;
	}
	
	public Boolean updateCommunityMemberType(String communityId, String personId, String userType, ResponseObject responseObject)
	{
		try
		{
			String updateCommunityMemberUrl = apiRoot + "social/community/member/update/type/" + URLEncoder.encode(communityId,"UTF-8") + 
			"/" + URLEncoder.encode(personId,"UTF-8") + "/" + URLEncoder.encode(userType,"UTF-8");
			String updateResult = sendRequest(updateCommunityMemberUrl, null);
			ResponsePojo internal_responsePojo = ResponsePojo.fromApi(updateResult, ResponsePojo.class); 
			ResponseObject internal_ro = internal_responsePojo.getResponse();
			responseObject = shallowCopy(responseObject, internal_ro);

			return responseObject.isSuccess();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return false;
	}
	
	public Boolean updateCommunityMemberStatus(String communityId, String personId, String userStatus, ResponseObject responseObject)
	{
		try
		{
			String updateCommunityMemberUrl = apiRoot + "social/community/member/update/status/" + URLEncoder.encode(communityId,"UTF-8") + 
			"/" + URLEncoder.encode(personId,"UTF-8") + "/" + URLEncoder.encode(userStatus,"UTF-8");
			String updateResult = sendRequest(updateCommunityMemberUrl, null);
			ResponsePojo internal_responsePojo = ResponsePojo.fromApi(updateResult, ResponsePojo.class); 
			ResponseObject internal_ro = internal_responsePojo.getResponse();
			responseObject = shallowCopy(responseObject, internal_ro);

			return responseObject.isSuccess();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return false;
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////
	
	// SOCIAL - SHARES
	
	public List<SharePojo> searchShares(String searchCriteria, String searchString, String typeFilter, ResponseObject responseObject)
	{
		return searchShares(searchCriteria,searchString,typeFilter,false,responseObject);
	}
	//TESTED
	
	public List<SharePojo> searchShares(String searchCriteria, String searchString, String typeFilter, Boolean searchParent, ResponseObject responseObject)
	{
		try {
			StringBuffer url = new StringBuffer(apiRoot).append("social/share/search/");
			if (null != searchCriteria) {
				url.append("?searchby=").append(searchCriteria).append("&id=").append(URLEncoder.encode(searchString, "UTF-8"));
			}
			if (null != typeFilter) {
				if (null != searchCriteria) {
					url.append("&");
				}
				else {
					url.append("?");				
				}
				url.append("type=").append(typeFilter);
			}
			if ( searchParent )
			{
				if ( null != searchCriteria && null != typeFilter)
					url.append("&");
				else
					url.append("?");
				url.append("searchParent=true");
			}
			String deleteResult = sendRequest(url.toString(), null);
			ResponsePojo internal_responsePojo = ResponsePojo.fromApi(deleteResult, ResponsePojo.class); 
			ResponseObject internal_ro = internal_responsePojo.getResponse();
			responseObject = shallowCopy(responseObject, internal_ro);
			
			List<SharePojo> shares = null;
			
			shares = ApiManager.mapListFromApi((JsonElement)internal_responsePojo.getData(), 
															SharePojo.listType(), null);			
			
			return shares;
		}
		catch (Exception e) {
			responseObject.setSuccess(false);
			responseObject.setMessage(e.getMessage());
		}
		return null;
	}
	
	public SharePojo getShare(String shareId, ResponseObject responseObject)
	{
		try{
			String getShareAddress = apiRoot + "social/share/get/" + URLEncoder.encode(shareId,"UTF-8");
			String getResult = sendRequest(getShareAddress, null);
			ResponsePojo internal_responsePojo = ResponsePojo.fromApi(getResult, ResponsePojo.class, SharePojo.class, new SharePojoApiMap(null)); 
			ResponseObject internal_ro = internal_responsePojo.getResponse();
			responseObject = shallowCopy(responseObject, internal_ro);

			return (SharePojo)internal_responsePojo.getData();

		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return null;
	}
	
	
	public SharePojo addShareJSON(String title, String description, String type, String jsonString , ResponseObject responseObject)
	{
		try
		{
			String addShareAddress = apiRoot + "social/share/add/json/" + URLEncoder.encode(type, "UTF-8") + "/" + URLEncoder.encode(title,"UTF-8") + "/" + URLEncoder.encode(description,"UTF-8");
			String addResult = sendRequest(addShareAddress, jsonString);
			ResponsePojo internal_responsePojo = ResponsePojo.fromApi(addResult, ResponsePojo.class, SharePojo.class, new SharePojoApiMap(null)); 
			ResponseObject internal_ro = internal_responsePojo.getResponse();
			responseObject = shallowCopy(responseObject, internal_ro);

			return (SharePojo)internal_responsePojo.getData();

		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return null;
	}
	
	public Boolean updateShareJSON(String shareId, String title, String description, String type, String jsonString , ResponseObject responseObject)
	{
		try{
			String updateShareAddress = apiRoot + "social/share/update/json/" + shareId + "/" + URLEncoder.encode(type,"UTF-8") + "/" + URLEncoder.encode(title,"UTF-8") + "/" + URLEncoder.encode(description,"UTF-8");
			String updateResult = sendRequest(updateShareAddress, jsonString);
			ResponsePojo internal_responsePojo = ResponsePojo.fromApi(updateResult, ResponsePojo.class); 
			ResponseObject internal_ro = internal_responsePojo.getResponse();
			responseObject = shallowCopy(responseObject, internal_ro);

			return responseObject.isSuccess();

		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return false;
	}
	
	public Boolean removeShare(String shareId, ResponseObject responseObject)
	{
		try{
			String removeShareAddress = apiRoot + "social/share/remove/" + URLEncoder.encode(shareId,"UTF-8");
			String updateResult = sendRequest(removeShareAddress, null);
			ResponsePojo internal_responsePojo = ResponsePojo.fromApi(updateResult, ResponsePojo.class); 
			ResponseObject internal_ro = internal_responsePojo.getResponse();
			responseObject = shallowCopy(responseObject, internal_ro);

			return responseObject.isSuccess();

		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return false;
	}
	
	public Boolean addShareToCommunity(String shareId, String comment, String communityId, ResponseObject responseObject)
	{
		try
		{
			String addCommunityAddress = apiRoot + "social/share/add/community/" + shareId + "/" + URLEncoder.encode(comment,"UTF-8") + "/" + URLEncoder.encode(communityId,"UTF-8");
			String updateResult = sendRequest(addCommunityAddress, null);
			ResponsePojo internal_responsePojo = ResponsePojo.fromApi(updateResult, ResponsePojo.class); 
			ResponseObject internal_ro = internal_responsePojo.getResponse();
			responseObject = shallowCopy(responseObject, internal_ro);

			return responseObject.isSuccess();

		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return false;
	}
	
	public Boolean removeShareFromCommunity(String shareId, String communityId, ResponseObject responseObject)
	{
		try{
			String addCommunityAddress = apiRoot + "social/share/remove/community/" + shareId + "/" + URLEncoder.encode(communityId,"UTF-8");
			String updateResult = sendRequest(addCommunityAddress, null);
			ResponsePojo internal_responsePojo = ResponsePojo.fromApi(updateResult, ResponsePojo.class); 
			ResponseObject internal_ro = internal_responsePojo.getResponse();
			responseObject = shallowCopy(responseObject, internal_ro);

			return responseObject.isSuccess();

		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return false;
	}
	

	//////////////////////////////////////////////////////////////////////////////////////////////
	
	// SOCIAL - PEOPLE
	
	public String deletePerson(String personId, ResponseObject responseObject)
	{
		try
		{
			String deletePersonAddress = apiRoot + "social/person/delete/" + URLEncoder.encode(personId,"UTF-8");
			String deleteResult = sendRequest(deletePersonAddress, null);
			ResponsePojo internal_responsePojo = ResponsePojo.fromApi(deleteResult, ResponsePojo.class); 
			ResponseObject internal_ro = internal_responsePojo.getResponse();
			responseObject = shallowCopy(responseObject, internal_ro);
			
			return responseObject.getMessage();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * getPerson
	 * @param personId  Can be the person id, email address, or Null. If Null, it will return information about current user.
	 * @param responsePojo
	 * @return PersonPojo
	 */
	public PersonPojo getPerson(String personId, ResponseObject responseObject)
	{
		try
		{
			String getPersonAddress = apiRoot + "social/person/get";

			if (personId != null)
				getPersonAddress +=  "/" + URLEncoder.encode(personId,"UTF-8");

			String getResult = sendRequest(getPersonAddress, null);
			ResponsePojo internal_responsePojo = ResponsePojo.fromApi(getResult, ResponsePojo.class, PersonPojo.class, new PersonPojoApiMap());  
			ResponseObject internal_ro = internal_responsePojo.getResponse();
			responseObject = shallowCopy(responseObject, internal_ro);
			return (PersonPojo)internal_responsePojo.getData();

		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * listPerson
	 * 
	 * @param responseObject
	 * @return
	 */
	public List<PersonPojo> listPerson(ResponseObject responseObject)
	{
		try
		{
			String listPersonAddress = apiRoot + "social/person/list";			
			String getResult = sendRequest(listPersonAddress, null);
			ResponsePojo internal_responsePojo = ResponsePojo.fromApi(getResult, ResponsePojo.class); 
			ResponseObject internal_ro = internal_responsePojo.getResponse();			
			responseObject = shallowCopy(responseObject, internal_ro);
			List<PersonPojo> people = null;
			people = ApiManager.mapListFromApi((JsonElement)internal_responsePojo.getData(), PersonPojo.listType(), new PersonPojoApiMap());
			
			return people;									
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return null;
	}

	public String registerPerson(String first_name, String last_name, String phone, String email, String password, String accountType, ResponseObject responseObject) 
	{
		try {
			Date date = new Date();
			SimpleDateFormat formatter = new SimpleDateFormat("MMM dd, yyyy kk:mm:ss aa");
			String today = formatter.format(date);
			String encrypted_password;

			encrypted_password = encryptWithoutEncode(password);

			WordPressUserPojo wpuser = new WordPressUserPojo();
			WordPressAuthPojo wpauth = new WordPressAuthPojo();

			wpuser.setCreated(today);
			wpuser.setModified(today);
			wpuser.setFirstname(first_name);
			wpuser.setLastname(last_name);
			wpuser.setPhone(phone);

			ArrayList<String> emailArray = new ArrayList<String>();
			emailArray.add(email);
			wpuser.setEmail(emailArray);

			//wpauth.setWPUserID(email);
			wpauth.setPassword(encrypted_password);
			wpauth.setAccountType(accountType);
			wpauth.setCreated(today);
			wpauth.setModified(today);
			
			WordPressSetupPojo wpSetup = new WordPressSetupPojo();
			wpSetup.setAuth(wpauth);
			wpSetup.setUser(wpuser);

			return registerPerson(wpSetup, responseObject);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		return null; 
	}
	
	public String registerPerson(WordPressSetupPojo wpSetup, ResponseObject responseObject) 
	{

		String theUrl = apiRoot + "social/person/register";
		String data =  new Gson().toJson(wpSetup);
		String registerResult = sendRequest(theUrl, data);
		ResponsePojo internal_responsePojo = ResponsePojo.fromApi(registerResult, ResponsePojo.class); 
		ResponseObject internal_ro = internal_responsePojo.getResponse();
		responseObject = shallowCopy(responseObject, internal_ro);
		
		return responseObject.getMessage();
	}
	
	public String updatePerson(String first_name, String last_name, String phone, String email, String password, String accountType, ResponseObject responseObject) 
	{
		try 
		{
			Date date = new Date();
			SimpleDateFormat formatter = new SimpleDateFormat("MMM dd, yyyy kk:mm:ss aa");
			String today = formatter.format(date);
			String encrypted_password = null;
			
			if ( password != null )
				encrypted_password = encryptWithoutEncode(password);


			WordPressUserPojo wpuser = new WordPressUserPojo();
			WordPressAuthPojo wpauth = new WordPressAuthPojo();
			
			wpuser.setModified(today);
			wpuser.setFirstname(first_name);
			wpuser.setLastname(last_name);
			wpuser.setPhone(phone);
			ArrayList<String> emailArray = new ArrayList<String>();
			emailArray.add(email);			
			wpuser.setEmail(emailArray);
			wpauth.setWPUserID(email);
			wpauth.setPassword(encrypted_password);
			wpauth.setAccountType(accountType);
			wpauth.setModified(today);

			return updatePerson(wpuser, wpauth, responseObject);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		return null; 
	}
	
	public String updatePerson(WordPressUserPojo wpuser, WordPressAuthPojo wpauth, ResponseObject responseObject) 
	{

		String theUrl = apiRoot + "social/person/update";
		String data = "{ \"user\":" + new Gson().toJson(wpuser) + ", \"auth\":" + new Gson().toJson(wpauth) + "}";
		String updateResult = sendRequest(theUrl, data);
		ResponsePojo internal_responsePojo = ResponsePojo.fromApi(updateResult, ResponsePojo.class); 
		ResponseObject internal_ro = internal_responsePojo.getResponse();
		responseObject = shallowCopy(responseObject, internal_ro);
		
		return responseObject.getMessage();
	}
	
	public String updatePersonPassword(String id, String newPassword, ResponseObject responseObject) 
	{
		try
		{
			String updatePasswordAddress = apiRoot + "social/person/update/password/" + URLEncoder.encode(id,"UTF-8") + "/" + URLEncoder.encode(newPassword,"UTF-8");
			String updateResult = sendRequest(updatePasswordAddress, null);
			ResponsePojo internal_responsePojo = ResponsePojo.fromApi(updateResult, ResponsePojo.class); 
			ResponseObject internal_ro = internal_responsePojo.getResponse();
			responseObject = shallowCopy(responseObject, internal_ro);
		
			return responseObject.getMessage();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return "";
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////
	
	// CONFIG - SOURCE
	
	public SourcePojo getSource(String sourceId, ResponseObject responseObject)
	{
		try{
			String getSourceAddress = apiRoot + "config/source/get/" + URLEncoder.encode(sourceId,"UTF-8");

			String getResult = sendRequest(getSourceAddress, null);
			ResponsePojo internal_responsePojo = ResponsePojo.fromApi(getResult, ResponsePojo.class, SourcePojo.class, new SourcePojoApiMap(null));  
			ResponseObject internal_ro = internal_responsePojo.getResponse();
			responseObject = shallowCopy(responseObject, internal_ro);
			return (SourcePojo)internal_responsePojo.getData();

		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return null;
	}
	
	public SourcePojo saveSource(SourcePojo source, String communityId, ResponseObject responseObject)
	{
		try {
			String address = apiRoot + "config/source/save/" + communityId + "/";
			String saveResult;
			saveResult = sendRequest(address, new Gson().toJson(source));
			ResponsePojo internal_responsePojo = ResponsePojo.fromApi(saveResult, ResponsePojo.class, SourcePojo.class, new SourcePojoApiMap(null));
			ResponseObject internal_ro = internal_responsePojo.getResponse();
			responseObject = shallowCopy(responseObject, internal_ro);
			return (SourcePojo)internal_responsePojo.getData();
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}

		return null;
	}
	
	public String deleteSource(String sourceId, String communityId, ResponseObject responseObject)
	{
		try {
			String address = apiRoot + "config/source/delete/" + sourceId + "/" + communityId;
			String deleteResult = sendRequest(address, null);
			ResponsePojo internal_responsePojo = ResponsePojo.fromApi(deleteResult, ResponsePojo.class); 
			ResponseObject internal_ro = internal_responsePojo.getResponse();
			responseObject = shallowCopy(responseObject, internal_ro);
			
			return responseObject.getMessage();
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}

		return null;
	}

	public List<DocumentPojo> testSource(SourcePojo testSrc, ResponseObject response, int nToReturn, boolean bReturnFullText) {
		
		StringBuffer theUrl = new StringBuffer(apiRoot).append("config/source/test");
		if (nToReturn > 0) {
			theUrl.append("?numReturn=").append(nToReturn);
		}
		if (bReturnFullText) {
			if (nToReturn > 0) {
				theUrl.append('&');
			}
			else {
				theUrl.append('?');
			}
			theUrl.append("returnFullText=true");
		}
		String map = ApiManager.mapToApi(testSrc, null);		
		String testResult = sendRequest(theUrl.toString(), map);
		
		ResponsePojo internal_response = ResponsePojo.fromApi(testResult, ResponsePojo.class);
		shallowCopy(response, internal_response.getResponse());
		List<DocumentPojo> docs = null;
		if (response.isSuccess()) {
			docs = ApiManager.mapListFromApi((JsonElement)internal_response.getData(), 
																	DocumentPojo.listType(), new DocumentPojoApiMap());
		}
		
		return docs;
	}
	
	/**
	 * calls get source good/bad/pending and merges results
	 * @param responseObject
	 * @return
	 */
	public List<SourcePojo> getSourceAll(String communityIdList, ResponseObject responseObject)
	{
		List<SourcePojo> sources = getSourceGood(communityIdList, responseObject);
		sources.addAll(getSourceBad(communityIdList, responseObject));
		sources.addAll(getSourcePending(communityIdList, responseObject));
		return sources;
	}
	public List<SourcePojo> getSourceGood(String communityIdList, ResponseObject responseObject)
	{
		List<SourcePojo> sources = new ArrayList<SourcePojo>();
		try{
			String getSourceAddress = apiRoot + "config/source/good/" + URLEncoder.encode(communityIdList,"UTF-8");

			String getResult = sendRequest(getSourceAddress, null);
			ResponsePojo internal_response = ResponsePojo.fromApi(getResult, ResponsePojo.class);
			shallowCopy(responseObject, internal_response.getResponse());
			//ResponsePojo internal_responsePojo = ResponsePojo.fromApi(getResult, ResponsePojo.class, SourcePojo.class, new SourcePojoApiMap(null));  
			//ResponseObject internal_ro = internal_responsePojo.getResponse();
			//responseObject = shallowCopy(responseObject, internal_ro);
			
			//TODO I'm passing null as the commids currently, I think this will return all commids and not wipe based on permissions
			if ( responseObject.isSuccess() )
				sources = ApiManager.mapListFromApi((JsonElement)internal_response.getData(), SourcePojo.listType(), new SourcePojoApiMap(null));			
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return sources;
	}
	public List<SourcePojo> getSourceBad(String communityIdList, ResponseObject responseObject)
	{
		List<SourcePojo> sources = new ArrayList<SourcePojo>();
		try{
			String getSourceAddress = apiRoot + "config/source/bad/" + URLEncoder.encode(communityIdList,"UTF-8");

			String getResult = sendRequest(getSourceAddress, null);
			ResponsePojo internal_response = ResponsePojo.fromApi(getResult, ResponsePojo.class);
			shallowCopy(responseObject, internal_response.getResponse());
			
			//TODO I'm passing null as the commids currently, I think this will return all commids and not wipe based on permissions
			if ( responseObject.isSuccess() )
				sources = ApiManager.mapListFromApi((JsonElement)internal_response.getData(), SourcePojo.listType(), new SourcePojoApiMap(null));			
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return sources;		
	}
	public List<SourcePojo> getSourcePending(String communityIdList, ResponseObject responseObject)
	{
		List<SourcePojo> sources = new ArrayList<SourcePojo>();
		try{
			String getSourceAddress = apiRoot + "config/source/pending/" + URLEncoder.encode(communityIdList,"UTF-8");

			String getResult = sendRequest(getSourceAddress, null);
			ResponsePojo internal_response = ResponsePojo.fromApi(getResult, ResponsePojo.class);
			shallowCopy(responseObject, internal_response.getResponse());
			
			//TODO I'm passing null as the commids currently, I think this will return all commids and not wipe based on permissions
			if ( responseObject.isSuccess() )
				sources = ApiManager.mapListFromApi((JsonElement)internal_response.getData(), SourcePojo.listType(), new SourcePojoApiMap(null));			
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return sources;		
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////
	
	// KNOWLEDGE - QUERY
	
	// Queries are slightly different - the entire ResponsePojo is used.
	
	
	public ResponsePojo sendQuery(AdvancedQueryPojo query, ObjectId communityId, ResponseObject response) {
		ArrayList<ObjectId> community = new ArrayList<ObjectId>(1);
		community.add(communityId);
		return sendQuery(query, community, response);		
	}
	
	
	public ResponsePojo sendQuery(AdvancedQueryPojo query, Collection<ObjectId> communities, ResponseObject response) {	
		StringBuffer theUrl = new StringBuffer(apiRoot).append("knowledge/document/query/");
		boolean bFirstComm = true;
		for (ObjectId commId: communities) {
			if (!bFirstComm) 
				theUrl.append(',');
			theUrl.append(commId.toString());
			bFirstComm = false;
		}		
		String testResult = sendRequest(theUrl.toString(), query.toApi());
		
		ResponsePojo internal_response = ResponsePojo.fromApi(testResult, ResponsePojo.class);
		shallowCopy(response, internal_response.getResponse());
		
		return internal_response;
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////
	
	// KNOWLEDGE - DOCUMENT - GET
	public DocumentPojo getDocument(String docid, ResponseObject responseObject)
	{
		return getDocument(docid,false,false,responseObject);
	}	
	public DocumentPojo getDocument(String docid, boolean returnFullText, boolean returnRawData, ResponseObject responseObject)
	{
		try
		{
			StringBuffer theUrl = new StringBuffer(apiRoot).append("knowledge/document/get/");
			theUrl.append(docid);
			theUrl.append("?returnFullText=").append(returnFullText);
			theUrl.append("?returnRawData=").append(returnRawData);
			
			String getResult = sendRequest(theUrl.toString(), null);
			ResponsePojo internal_responsePojo = ResponsePojo.fromApi(getResult, ResponsePojo.class, DocumentPojo.class, new DocumentPojoApiMap());  
			ResponseObject internal_ro = internal_responsePojo.getResponse();
			responseObject = shallowCopy(responseObject, internal_ro);
			return (DocumentPojo)internal_responsePojo.getData();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		return null;
	}
	
	public DocumentPojo getDocument(String sourceKey, String url, ResponseObject responseObject)
	{
		return getDocument(sourceKey, url, false, false, responseObject);
	}
	public DocumentPojo getDocument(String sourceKey, String url, boolean returnFullText, boolean returnRawData, ResponseObject responseObject)
	{
		try
		{
			StringBuffer theUrl = new StringBuffer(apiRoot).append("knowledge/document/get/");
			theUrl.append(sourceKey).append("/").append(url);
			theUrl.append("?returnFullText=").append(returnFullText);
			theUrl.append("?returnRawData=").append(returnRawData);
			
			String getResult = sendRequest(theUrl.toString(), null);
			ResponsePojo internal_responsePojo = ResponsePojo.fromApi(getResult, ResponsePojo.class, DocumentPojo.class, new DocumentPojoApiMap());  
			ResponseObject internal_ro = internal_responsePojo.getResponse();
			responseObject = shallowCopy(responseObject, internal_ro);
			return (DocumentPojo)internal_responsePojo.getData();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		return null;
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////
	
	// CUSTOM - CREATE PLUGIN TASK
	
	// The following fields of taskConfig are filled in (string unless otherwise specified):
	// - jobtitle, jobdesc
	// - jarURL: the name of the JAR in the share
	// - inputCollection
	// - firstSchedule (Date) ... when it will be run
	// - scheduleFreq (SCHEDULE_FREQUENCY)
	// - mapper, combiner, reducer
	// - outputKey, outputValue
	// - appendResult, ageOutInDays
	// - jobDependencies ... though using the input param that allows them to be specified as jobtitles is recommended
	
	public ObjectId createCustomPluginTask(CustomMapReduceJobPojo taskConfig, Collection<String> dependencies, ObjectId communityId, ResponseObject response) {
		ArrayList<ObjectId> community = new ArrayList<ObjectId>(1);
		community.add(communityId);
		return createCustomPluginTask(taskConfig, dependencies, community, response);
	}
	public ObjectId createCustomPluginTask(CustomMapReduceJobPojo taskConfig, Collection<String> dependencies, Collection<ObjectId> communities, ResponseObject response) {
		return createCustomPluginTask(taskConfig, dependencies, communities, response, false);
	}
	
	private ObjectId createCustomPluginTask(CustomMapReduceJobPojo taskConfig, Collection<String> dependencies, Collection<ObjectId> communities, ResponseObject response, boolean bUpdate) {
		ObjectId retVal = null;
		try {
			StringBuffer url = new StringBuffer(apiRoot).append("custom/mapreduce/");
			if (bUpdate) {
				if (taskConfig._id != null) {
					url.append("updatejob/").append(taskConfig._id.toString()).append("/");				
				}
				else {
					url.append("updatejob/").append(taskConfig.jobtitle).append("/");
					taskConfig.jobtitle = null;
				}//TESTED
			}
			else {
				url.append("schedulejob/");				
			}			
			if (null != communities) {
				for (ObjectId communityId: communities) {
					url.append(communityId.toString()).append(',');
				}
				url.setLength(url.length() - 1);
			}
			else {
				url.append("null");
			}
			url.append('/');
			
			if ((null != taskConfig.jobDependencies) && !taskConfig.jobDependencies.isEmpty()) {
				for (ObjectId jobId: taskConfig.jobDependencies) {
					url.append(jobId.toString()).append(',');
				}
				url.setLength(url.length() - 1);				
			}
			else if ((null != dependencies) && !dependencies.isEmpty()) {
				for (String jobTitle: dependencies) {
					url.append(jobTitle).append(',');
				}
				url.setLength(url.length() - 1);								
			}
			else {
				url.append("null");				
			}
			url.append("/");

			// "nextRunTime"==first Schedule (date)
			if (null != taskConfig.firstSchedule) {
				taskConfig.nextRunTime = taskConfig.firstSchedule.getTime();
			}
			
			String json = sendRequest(url.toString(), ApiManager.mapToApi(taskConfig, null));
			ResponsePojo internal_responsePojo = ResponsePojo.fromApi(json, ResponsePojo.class); 
			ResponseObject internal_ro = internal_responsePojo.getResponse();
			response = shallowCopy(response, internal_ro);
			
			if (response.isSuccess()) {
				JsonPrimitive retValObj = (JsonPrimitive)internal_responsePojo.getData(); 
				retVal = new ObjectId(retValObj.getAsString());
			}			
		} 
		catch (Exception e) {
			response.setSuccess(false);
			response.setMessage(e.getMessage());
		}
		return retVal;
	}
	//TESTED (one minor clause to test, will leave for now)
	
	///////////////////////////////////////////////////////////////////
	
	// Updates ... as above but jobtitle or _id must be specified
	
	public ObjectId updateCustomPluginTask(CustomMapReduceJobPojo taskConfig, Collection<String> dependencies, ObjectId communityId, ResponseObject response) {
		ArrayList<ObjectId> community = new ArrayList<ObjectId>(1);
		community.add(communityId);
		return updateCustomPluginTask(taskConfig, dependencies, community, response);
	}
	
	public ObjectId updateCustomPluginTask(CustomMapReduceJobPojo taskConfig, Collection<String> dependencies, Collection<ObjectId> communities, ResponseObject response) {
		return createCustomPluginTask(taskConfig, dependencies, communities, response, true);
	}
	//TESTED
		
	//////////////////////////////////////////////////////////////////////////////////////////////
	
	// CUSTOM - CREATE PLUGIN TASK
	
	// The following fields of taskConfig are filled in (string unless otherwise specified):
	// - jobtitle, jobdesc
	// - firstSchedule (Date) ... when it will be run
	// - scheduleFreq (SCHEDULE_FREQUENCY)
	// - appendResult, ageOutInDays
	
	public ObjectId createCustomSavedQueryTask(CustomMapReduceJobPojo taskConfig, AdvancedQueryPojo query, ObjectId communityId, ResponseObject response) {
		ArrayList<ObjectId> community = new ArrayList<ObjectId>(1);
		community.add(communityId);
		return createCustomSavedQueryTask(taskConfig, query, community, response);
	}
	public ObjectId createCustomSavedQueryTask(CustomMapReduceJobPojo taskConfig, ObjectId communityId, ResponseObject response) {
		ArrayList<ObjectId> community = new ArrayList<ObjectId>(1);
		community.add(communityId);
		return createCustomSavedQueryTask(taskConfig, null, community, response);
	}
	public ObjectId createCustomSavedQueryTask(CustomMapReduceJobPojo taskConfig, AdvancedQueryPojo query, Collection<ObjectId> communities, ResponseObject response) {
		if (null != query) {
			taskConfig.query = query.toApi();
		}
		return createCustomPluginTask(taskConfig, null, communities, response);
	}
	//TESTED
	
	///////////////////////////////////////////////////////////////////
	
	// Updates ... as above but jobtitle or _id must be specified
	
	public ObjectId updateCustomSavedQueryTask(CustomMapReduceJobPojo taskConfig, AdvancedQueryPojo query, ObjectId communityId, ResponseObject response) {
		ArrayList<ObjectId> community = new ArrayList<ObjectId>(1);
		community.add(communityId);
		return updateCustomSavedQueryTask(taskConfig, query, community, response);
	}
	public ObjectId updateCustomSavedQueryTask(CustomMapReduceJobPojo taskConfig, ObjectId communityId, ResponseObject response) {
		ArrayList<ObjectId> community = new ArrayList<ObjectId>(1);
		community.add(communityId);
		return updateCustomPluginTask(taskConfig, null, community, response);
	}
	public ObjectId updateCustomSavedQueryTask(CustomMapReduceJobPojo taskConfig, AdvancedQueryPojo query, Collection<ObjectId> communities, ResponseObject response) {
		if (null != query) {
			taskConfig.query = query.toApi();
		}
		return updateCustomPluginTask(taskConfig, null, communities, response);		
	}
	//TESTED
	
	public CustomMapReduceResultPojo getCustomTaskOrQueryResults(String jobtitle, ResponseObject response)
	{
		CustomMapReduceResultPojo retVal = null;
		try
		{
			StringBuilder url = new StringBuilder(apiRoot).append("custom/mapreduce/getresults/");
			if ( null != jobtitle)
			{
				url.append(jobtitle);
			}
				
			String json = sendRequest(url.toString(), null);
			ResponsePojo internal_responsePojo = ResponsePojo.fromApi(json, ResponsePojo.class, CustomMapReduceResultPojo.class, new CustomMapReduceResultPojoApiMap()); 
			
			ResponseObject internal_ro = internal_responsePojo.getResponse();
			response = shallowCopy(response, internal_ro);
			
			if (response.isSuccess()) 
			{
				//JsonElement js = (JsonElement)internal_responsePojo.getData();
				//retVal = ApiManager.mapFromApi(js, CustomMapReduceResultPojo.class, null);
				retVal = (CustomMapReduceResultPojo)internal_responsePojo.getData();
				
			}
		}
		catch (Exception e)
		{
			response.setSuccess(false);
			response.setMessage(e.getMessage());
		}
		return retVal;
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////

	public List<CustomMapReduceJobPojo> getCustomTaskOrQuery(String jobtitle, ResponseObject response) {
		List<CustomMapReduceJobPojo> retVal = null;
		try {
			StringBuilder url = new StringBuilder(apiRoot).append("custom/mapreduce/getjobs");
			if (null != jobtitle) {
				url.append("/").append(jobtitle);
			}
			// (else get all)
			
			String json = sendRequest(url.toString(), null);
			ResponsePojo internal_responsePojo = ResponsePojo.fromApi(json, ResponsePojo.class); 
			ResponseObject internal_ro = internal_responsePojo.getResponse();
			response = shallowCopy(response, internal_ro);
			
			if (response.isSuccess()) {
				retVal = ApiManager.mapListFromApi((JsonElement)internal_responsePojo.getData(), 
													CustomMapReduceJobPojo.listType(), null);
			}
		}
		catch (Exception e) { 
			response.setSuccess(false);
			response.setMessage(e.getMessage());			
		}
		return retVal;
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////
	
	// And deletes, apply to either:	
	
	public boolean deleteCustomTaskOrQuery(String jobtitle, ResponseObject response) {
		try {
			StringBuilder url = new StringBuilder(apiRoot).append("custom/mapreduce/removejob/");
			url.append(jobtitle);
			
			String json = sendRequest(url.toString(), null);
			ResponsePojo internal_responsePojo = ResponsePojo.fromApi(json, ResponsePojo.class); 
			ResponseObject internal_ro = internal_responsePojo.getResponse();
			response = shallowCopy(response, internal_ro);
		}
		catch (Exception e) {
			response.setSuccess(false);
			response.setMessage(e.getMessage());
		}
		return response.isSuccess();
	}
	public boolean deleteCustomTaskOrQuery(ObjectId taskId, ResponseObject response) {
		return deleteCustomTaskOrQuery(taskId.toString(), response);
	}	
	//TESTED
	
	//////////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////////////
	
	// ALIASES
	
	// UPDATE, REMOVE, AND GET
	
	// This is not super-efficient code
	
	public Set<String> updateAliases(Collection<EntityFeaturePojo> aliasesToUpdate, String communityIdStr, boolean bUpsert, ResponseObject response) {
		return updateAliases(aliasesToUpdate, communityIdStr, bUpsert, null, response);
		
	}
	public Set<String> removeAliases(Collection<String> aliasesToRemove, String communityIdStr, ResponseObject response) {
		return removeAliases(aliasesToRemove, communityIdStr, null, response);
		
	}
	// (Only use this version if immediately called after getAliases since none of this is in any way atomic)
	public Set<String> updateAliases(Collection<EntityFeaturePojo> aliasesToUpdate, String communityIdStr, boolean bUpsert, Map<String, List<SharePojo>> aliasMapping, ResponseObject response) {
		if (null == aliasMapping) {
			aliasMapping = new HashMap<String, List<SharePojo>>();
			this.getAliases(communityIdStr, aliasMapping, response);
			if (!response.isSuccess()) {
				return null;
			}
		}//TESTED
		Map<ObjectId, BasicDBObject> shareContentCache = new HashMap<ObjectId, BasicDBObject>();
		List<SharePojo> sharesToUpdate = new LinkedList<SharePojo>();
		// Step through the aliases, update the content
		// Loop 1 update
		SharePojo shareForNewAliases = null;
		Set<String> erroredAliases = new HashSet<String>();
		HashMultimap<ObjectId, String> shareToAliasMapping = HashMultimap.create();
		for (EntityFeaturePojo alias: aliasesToUpdate) {
			List<SharePojo> sharesForThisAlias = aliasMapping.get(alias.getIndex());
			if ((null == sharesForThisAlias) && bUpsert) { // This is a new alias and not ignoring upserts
				if (null == shareForNewAliases) { // Haven't yet assigned such a share
					shareForNewAliases = this.upsertSharePrep(communityIdStr, shareContentCache, aliasMapping);
					if (null == shareForNewAliases) {
						erroredAliases.add(alias.getIndex());
						continue;
					}
					sharesToUpdate.add(shareForNewAliases);
				}
				BasicDBObject shareContent =  shareContentCache.get(shareForNewAliases.get_id()); // (exists by construction)
				shareContent.put(alias.getIndex(), alias.toDb());
				shareToAliasMapping.put(shareForNewAliases.get_id(), alias.getIndex());
			}//TESTED
			else if (null != sharesForThisAlias) {
				for (SharePojo share: sharesForThisAlias) {
					BasicDBObject shareContent =  shareContentCache.get(share.get_id());
					if (null == shareContent) {
						try {
							String json = share.getShare();
							shareContent = (BasicDBObject) JSON.parse(json);
							shareContentCache.put(share.get_id(), shareContent);
							sharesToUpdate.add(share);
						}
						catch (Exception e) {
							erroredAliases.add(alias.getIndex());							
						}
					}//TESTED
					shareContent.put(alias.getIndex(), alias.toDb());
					shareToAliasMapping.put(share.get_id(), alias.getIndex());
				}//TESTED
			}
			else {
				erroredAliases.add(alias.getIndex());	
			}
			// end loop over updating shares
		}//end loop over aliases

		// Loop 2 now update all the shares
		boolean bSucceededUpdatingSomething = false;
		for (SharePojo share: sharesToUpdate) {
			BasicDBObject shareContent =  shareContentCache.get(share.get_id()); // (exists by construction)
			String shareIdStr = share.get_id().toString();
			this.updateShareJSON(shareIdStr, share.getTitle(), share.getDescription(), "infinite-entity-alias", shareContent.toString(), response);
			bSucceededUpdatingSomething |= response.isSuccess();
			if (!response.isSuccess()) {
				Set<String> failedAliases = shareToAliasMapping.get(share.get_id());
				if (null != failedAliases) {
					erroredAliases.addAll(failedAliases);
				}
			}
		}//TESTED		
		
		response.setSuccess(bSucceededUpdatingSomething); 
		return erroredAliases;
	}
	
	// (Only use this version if immediately called after getAliases since none of this is in any way atomic)
	public Set<String> removeAliases(Collection<String> aliasesToRemove, String communityIdStr, Map<String, List<SharePojo>> aliasMapping, ResponseObject response) {
		if (null == aliasMapping) {
			aliasMapping = new HashMap<String, List<SharePojo>>();
			this.getAliases(communityIdStr, aliasMapping, response);
			if (!response.isSuccess()) {
				return null;
			}
		}//TESTED
		Map<ObjectId, BasicDBObject> shareContentCache = new HashMap<ObjectId, BasicDBObject>();
		List<SharePojo> sharesToUpdate = new LinkedList<SharePojo>();
		// Step through the aliases, update the content
		// Loop 1 update
		Set<String> erroredAliases = new HashSet<String>();
		HashMultimap<ObjectId, String> shareToAliasMapping = HashMultimap.create();
		for (String alias: aliasesToRemove) {
			List<SharePojo> sharesForThisAlias = aliasMapping.get(alias);
			if (null != sharesForThisAlias) {
				for (SharePojo share: sharesForThisAlias) {
					BasicDBObject shareContent =  shareContentCache.get(share.get_id());
					if (null == shareContent) {
						try {
							String json = share.getShare();
							shareContent = (BasicDBObject) JSON.parse(json);
							shareContentCache.put(share.get_id(), shareContent);
							sharesToUpdate.add(share);
						}
						catch (Exception e) {
							erroredAliases.add(alias);							
						}
					}//TESTED
					shareContent.remove(alias);
					shareToAliasMapping.put(share.get_id(), alias);
				}//TESTED
			}
			// end loop over updating shares
		}//end loop over aliases

		// Loop 2 now update all the shares
		boolean bSucceededUpdatingSomething = false;
		for (SharePojo share: sharesToUpdate) {
			BasicDBObject shareContent =  shareContentCache.get(share.get_id()); // (exists by construction)
			String shareIdStr = share.get_id().toString();
			if (shareContent.isEmpty()) { // Remove the share
				this.removeShare(shareIdStr, response);
				if (!response.isSuccess()) {
					Set<String> failedAliases = shareToAliasMapping.get(share.get_id());
					if (null != failedAliases) {
						erroredAliases.addAll(failedAliases);
					}
				}
			}//TESTED
			else {
				this.updateShareJSON(shareIdStr, share.getTitle(), share.getDescription(), "infinite-entity-alias", shareContent.toString(), response);
				bSucceededUpdatingSomething |= response.isSuccess();
				if (!response.isSuccess()) {
					Set<String> failedAliases = shareToAliasMapping.get(share.get_id());
					if (null != failedAliases) {
						erroredAliases.addAll(failedAliases);
					}
				}
			}//TESTED
		}//TESTED		
		
		response.setSuccess(bSucceededUpdatingSomething); 
		return erroredAliases;
	}
	// THIS IS BOTH PUBLIC AND A UTILITY FOR THE OTHER CALLS
	
	public Map<String, EntityFeaturePojo> getAliases(String communityIdStr, ResponseObject response) {
		return getAliases(communityIdStr, null, response);
	}
	
	public Map<String, EntityFeaturePojo> getAliases(String communityIdStr, Map<String, List<SharePojo>> aliasMapping, ResponseObject response) {
		
		// Get the shares with the right type and community
		List<SharePojo> shareList = this.searchShares("community", communityIdStr, "infinite-entity-alias", response);
		if (!response.isSuccess()) {
			return null;
		}
		// Generate a list
		HashMap<String, EntityFeaturePojo> masterAliases = new HashMap<String, EntityFeaturePojo>();
		
		if (null != shareList) {
			for (SharePojo share: shareList) {
				populateAliasTableFromShare(share, masterAliases, aliasMapping);
			}
		}
		response.setSuccess(true);
		return masterAliases;
	}//TESTED
	
	// ALIAS UTILITIES:
	
	private SharePojo upsertSharePrep(String communityIdStr, Map<ObjectId, BasicDBObject> shareContentCache, Map<String, List<SharePojo>> aliasMapping) {
		SharePojo shareForNewAliases = null;
		BasicDBObject contentForNewAliases = null;
		for (List<SharePojo> shares: aliasMapping.values()) {
			if (!shares.isEmpty()) {
				shareForNewAliases = shares.iterator().next();
				try {
					String json = shareForNewAliases.getShare();
					contentForNewAliases = (BasicDBObject) JSON.parse(json);
					shareContentCache.put(shareForNewAliases.get_id(), contentForNewAliases);
					break;
				}
				catch (Exception e) {} // Try a different share
			}
		}//TESTED
		if (null == shareForNewAliases) { // Didn't find one, so going to have to create something
			EntityFeaturePojo discard = new EntityFeaturePojo();
			discard.setDisambiguatedName("DISCARD");
			discard.setType("SPECIAL");
			discard.setIndex("DISCARD");
			EntityFeaturePojo docDiscard = new EntityFeaturePojo();
			docDiscard.setDisambiguatedName("DOCUMENT_DISCARD");
			docDiscard.setType("SPECIAL");
			docDiscard.setIndex("DOCUMENT_DISCARD");
			contentForNewAliases = new BasicDBObject("DISCARD", discard.toDb());
			contentForNewAliases.put("DOCUMENT_DISCARD", docDiscard);
			ResponseObject response = new ResponseObject();
			shareForNewAliases = this.addShareJSON("Alias Share: " + communityIdStr, "An alias share for a specific community", "infinite-entity-alias", "{}", response);
			if ((null == shareForNewAliases) || !response.isSuccess()) {
				return null;
			}//TESTED
			
			// Remove share from personal community
			try {
				ShareCommunityPojo currCommunity =  shareForNewAliases.getCommunities().iterator().next();
				String removeCommunityIdStr = currCommunity.get_id().toString();
				if (!communityIdStr.equals(removeCommunityIdStr)) { // (obv not if this is my community somehow, don't think that's possible)
					this.removeShareFromCommunity(shareForNewAliases.get_id().toString(), removeCommunityIdStr, response);
					if (!response.isSuccess()) {
						return null;				
					}
				}
			}
			catch (Exception e) {} // do nothing I guess, not in any communities?
			
			this.addShareToCommunity(shareForNewAliases.get_id().toString(), "aliasComm", communityIdStr, response);
			if (!response.isSuccess()) {
				return null;				
			}
		}//TESTED
		shareContentCache.put(shareForNewAliases.get_id(), contentForNewAliases);
		return shareForNewAliases;
	}//TESTED
	
	// THIS IS A SUBSET OF com.ikanow.infinit.e.api.knowledge.aliases.AliasLookupTable
	private void populateAliasTableFromShare(SharePojo share, HashMap<String, EntityFeaturePojo> masterAliases,  Map<String, List<SharePojo>> aliasMapping) {
		String json = share.getShare();
		if (null != json) {
			try {
				DBObject dbo = (DBObject) JSON.parse(json);
				if (null != dbo) {
					for (Object entryObj: dbo.toMap().entrySet()) {
						@SuppressWarnings("unchecked")
						Map.Entry<String, Object> entry = (Map.Entry<String, Object>)entryObj;
						
						String masterAlias = entry.getKey();
						
						BasicDBObject entityFeatureObj = (BasicDBObject) entry.getValue();
						EntityFeaturePojo aliasInfo = null;
						try {
							aliasInfo = EntityFeaturePojo.fromDb(entityFeatureObj, EntityFeaturePojo.class);
						}
						catch (Exception e) {
							continue;
						}
						if (null == aliasInfo.getIndex()) {
							aliasInfo.setIndex(masterAlias);
						}

						if (null != aliasInfo) { // (allow getAlias to be null/empty)
							//(overwrite duplicates)
							masterAliases.put(aliasInfo.getIndex(), aliasInfo);
							if (null != aliasMapping) {
								List<SharePojo> shareList = aliasMapping.get(aliasInfo.getIndex());
								if (null == shareList) {
									shareList = new LinkedList<SharePojo>();
									aliasMapping.put(aliasInfo.getIndex(), shareList);
								}
								shareList.add(share);
							}
						}
					}
				}
			}
			catch (Exception e) {
				
			} // not Json, just carry on...
		}		
	}//TESTED
	
	public DimensionListPojo getEntitySuggest(String searchterm, String communityIdStr, boolean includeGeo, boolean includeLinkData, ResponseObject response)
	{		
		try 
		{
			StringBuilder url = new StringBuilder(apiRoot).append("knowledge/feature/entitySuggest/");			
			url.append(URLEncoder.encode(searchterm,"UTF-8"));
			url.append("/");			
			url.append(URLEncoder.encode(communityIdStr,"UTF-8"));
			
			String json = sendRequest(url.toString(), null);
			ResponsePojo internal_responsePojo = ResponsePojo.fromApi(json, ResponsePojo.class); 
			ResponseObject internal_ro = internal_responsePojo.getResponse();
			response = shallowCopy(response, internal_ro);
			
			return new Gson().fromJson((JsonElement)internal_responsePojo.getData(), DimensionListPojo.class);		
		}
		catch (Exception e) 
		{
			response.setSuccess(false);
			response.setMessage(e.getMessage());
		}
		return null;
	}
	
	public List<SearchSuggestPojo> getGeoSuggest(Double latitude, Double longitude, String communityIdStr, ResponseObject response)
	{
		List<SearchSuggestPojo> locations = null;
		try 
		{
			String searchterm = latitude + "," + longitude;
			StringBuilder url = new StringBuilder(apiRoot).append("knowledge/feature/geoSuggest/");			
			url.append(URLEncoder.encode(searchterm,"UTF-8"));
			url.append("/");			
			url.append(URLEncoder.encode(communityIdStr,"UTF-8"));
			
			String json = sendRequest(url.toString(), null);
			ResponsePojo internal_responsePojo = ResponsePojo.fromApi(json, ResponsePojo.class); 
			ResponseObject internal_ro = internal_responsePojo.getResponse();
			response = shallowCopy(response, internal_ro);
			
			locations = ApiManager.mapListFromApi((JsonElement)internal_responsePojo.getData(), SearchSuggestPojo.listType(), null);			
		}
		catch (Exception e) 
		{
			response.setSuccess(false);
			response.setMessage(e.getMessage());
		}
		return locations;
	}
	
	
	//////////////////////////////////////////////////////////////////////////////////////////////
	
	// UTILITY	
	
	public boolean hasSourceHarvested(String sourceId)
	{
		ResponseObject ro = new ResponseObject();
		return hasSourceHarvested(getSource(sourceId, ro));
	}
	
	public boolean hasSourceHarvested(SourcePojo source)
	{
		if ( source != null )
		{
			if ( source.getHarvestStatus() != null && source.getHarvestStatus().getHarvested() != null )
			{
				if ( source.getHarvestStatus().getHarvest_status() == HarvestEnum.in_progress && source.getHarvestStatus().getDoccount() != null && source.getHarvestStatus().getDoccount() > 0)
				{
					//if the source is in progress but has a doc count, it has already completed once
					return true;
				}
				else if ( source.getHarvestStatus().getHarvested() != null )
				{
					//if the source is not in progress but has a harvested date, it has completed atleast once
					return true;
				}
			}
		}
		return false;
	}
		
	public boolean isAdmin()
	{
		return this.sendKeepalive(true);
	}
	
	/**
	 * Let's the driver know to send the project_id with every api call.
	 * 
	 * @param project_id
	 */
	public void setProjectFilter(String project_id) {
		this.project_id = project_id;		
	}
	
	/**
	 * Turns off sending the project_id with every api call.
	 * 
	 */
	public void clearProjectFilter() {
		this.project_id = null;
	}
	
	/**
	 * Helper function to add the project_id if it exists to the given
	 * url
	 * 
	 * @param url
	 * @return
	 */
	public String appendProjectIdToUrl(String url){
		if ( project_id != null)
		{
			//TODO hardcode the project_id param
			String param = "project_id=" + this.project_id;
			if ( url.contains("?") )
				url += "&" + param;
			else
				url += "?" + param;
		}
		return url;
	}
	
	///////// Request Calls	
	
	public String sendRequest(String urlAddress, String postData)
	{
		try {
			if (postData == null)
				return sendGetRequest(urlAddress);
			else
				return sendPostRequest(urlAddress, postData);
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}

	private String sendPostRequest(String urlAddress, String data) throws MalformedURLException, IOException
	{
		return sendPostRequest(urlAddress, data, 0);
	}

	private String sendPostRequest(String urlAddress, String data, int redirects) throws MalformedURLException, IOException
	{	
		String result = "";
		urlAddress = appendProjectIdToUrl(urlAddress);
		
		if (urlAddress.startsWith("https:")) {
			TrustManagerManipulator.allowAllSSL();
		}
		URLConnection urlConnection = new URL(urlAddress).openConnection();

		if ( cookie != null )
			urlConnection.setRequestProperty("Cookie", cookie);
		if ( apiKey != null )
			urlConnection.setRequestProperty("Cookie", apiKey);

		urlConnection.setDoOutput(true);
		urlConnection.setRequestProperty("Accept-Charset", "UTF-8");
		((HttpURLConnection)urlConnection).setRequestMethod("POST");

		// Post JSON string to URL

		OutputStream os = urlConnection.getOutputStream();

		byte[] b = data.getBytes("UTF-8");

		os.write(b);

		int status = ((HttpURLConnection)urlConnection).getResponseCode();
		// normally, 3xx is redirect
		if (status != HttpURLConnection.HTTP_OK)
		{
			if (status == HttpURLConnection.HTTP_MOVED_TEMP
				|| status == HttpURLConnection.HTTP_MOVED_PERM
					|| status == HttpURLConnection.HTTP_SEE_OTHER)
			{
				if (redirects <= 5) {
					String newUrlAddress = ((HttpURLConnection)urlConnection).getHeaderField("Location");
					if (null != newUrlAddress) {
						return sendPostRequest(newUrlAddress, data, redirects + 1);
					}
				}
				//(else carry on, will exception out or something below)
			}
		}//TESTED
		
		// Receive results back from API

		InputStream inStream = urlConnection.getInputStream();

		result = IOUtils.toString(inStream, "UTF-8");

		inStream.close();

		//save cookie if cookie is null
		if ( cookie == null )
		{
			String headername;
			for ( int i = 1; (headername = urlConnection.getHeaderFieldKey(i)) != null; i++ )
			{
				if ( headername.equals("Set-Cookie") )
				{
					cookie = urlConnection.getHeaderField(i);
					break;
				}
			}
		}     


		return result;
	}
	
	
	public String sendGetRequest(String urlAddress) throws Exception
	{
		return sendGetRequest(urlAddress, 0);
	}

	public String sendGetRequest(String urlAddress, int redirects) throws Exception
	{
		if (urlAddress.startsWith("https:")) {
			TrustManagerManipulator.allowAllSSL();
		}
		
		urlAddress = appendProjectIdToUrl(urlAddress);
		
		URL url = new URL(urlAddress);		
		URLConnection urlConnection = url.openConnection();
		if ( cookie != null )
			urlConnection.setRequestProperty("Cookie", cookie);
		if ( apiKey != null )
			urlConnection.setRequestProperty("Cookie", apiKey);
			
		((HttpURLConnection)urlConnection).setRequestMethod("GET");

		int status = ((HttpURLConnection)urlConnection).getResponseCode();
		// normally, 3xx is redirect
		if (status != HttpURLConnection.HTTP_OK)
		{
			if (status == HttpURLConnection.HTTP_MOVED_TEMP
				|| status == HttpURLConnection.HTTP_MOVED_PERM
					|| status == HttpURLConnection.HTTP_SEE_OTHER)
			{
				if (redirects <= 5) {
					String newUrlAddress = ((HttpURLConnection)urlConnection).getHeaderField("Location");
					if (null != newUrlAddress) {
						return sendGetRequest(newUrlAddress, redirects + 1);
					}
				}
				//(else carry on, will exception out or something below)
			}
		}//TESTED
		
		//read back result
		BufferedReader inStream = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
		StringBuilder strBuilder = new StringBuilder();
		String buffer;           
		while ( (buffer = inStream.readLine()) != null )
		{
			strBuilder.append(buffer);
		}       
		inStream.close();

		//save cookie if cookie is null
		if ( cookie == null )
		{
			String headername;
			for ( int i = 1; (headername = urlConnection.getHeaderFieldKey(i)) != null; i++ )
			{
				if ( headername.equals("Set-Cookie") )
				{
					cookie = urlConnection.getHeaderField(i);
					break;
				}
			}
		}       
		return strBuilder.toString();
	}  
	
	
	///////////Password Encryption

	public String encryptEncodePassword(String pword) throws UnsupportedEncodingException, NoSuchAlgorithmException
	{
		return URLEncoder.encode(encryptWithoutEncode(pword), "UTF-8");
	}

	public static String encryptWithoutEncode(String password) throws NoSuchAlgorithmException, UnsupportedEncodingException 
	{	
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		md.update(password.getBytes("UTF-8"));

		return new String(Base64.encodeBase64(md.digest()));
	}
	
	
	
	//////////Shallow Copy Response Object
	
	private ResponseObject shallowCopy(ResponseObject toCopy, ResponseObject fromCopy)
	{
		toCopy.setAction(fromCopy.getAction());
		toCopy.setMessage(fromCopy.getMessage());
		toCopy.setSuccess(fromCopy.isSuccess());
		toCopy.setTime(fromCopy.getTime());
		return fromCopy;
	}
	
	///////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////
	
	// Start of some test code
	// Run standalone RESTlet API on 8184 pointing to the right place
	// Invoke with username and password
	
	public static void main(String[] args) {
		if (args.length < 2) {
			System.out.println("*** CALL WITH username password as args");
			System.exit(-1);
		}
		InfiniteDriver.setDefaultApiRoot("http://localhost:8184/");
		InfiniteDriver infDriver = new InfiniteDriver();
		ResponseObject responseObject = new ResponseObject();
		infDriver.login(args[0], args[1], responseObject);
		System.out.println("DRIVER LOGIN = " + responseObject.isSuccess() + " : " + responseObject.getMessage());
		List<PersonPojo> people = infDriver.listPerson(responseObject);
		System.out.println("DRIVER LIST PEOPLE = " + responseObject.isSuccess() + " : " + responseObject.getMessage());
		
		if (null != people) {
			System.out.println("FOUND " + people.size());
			if (people.size() > 0) {
				System.out.println("EXAMPLE " + people.iterator().next().toDb());				
			}
		}
	}

	
}
