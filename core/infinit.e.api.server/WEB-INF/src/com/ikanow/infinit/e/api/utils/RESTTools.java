/*******************************************************************************
 * Copyright 2012, The Infinit.e Open Source Project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.ikanow.infinit.e.api.utils;

import java.net.InetAddress;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.bson.types.ObjectId;

import com.ikanow.infinit.e.api.authentication.PasswordEncryption;
import com.ikanow.infinit.e.api.social.community.PersonHandler;
import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.social.authentication.AuthenticationPojo;
import com.ikanow.infinit.e.data_model.store.social.community.CommunityPojo;
import com.ikanow.infinit.e.data_model.store.social.cookies.CookiePojo;
import com.ikanow.infinit.e.data_model.store.social.person.PersonCommunityPojo;
import com.ikanow.infinit.e.data_model.store.social.person.PersonPojo;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

public class RESTTools 
{
	private static final Logger logger = Logger.getLogger(RESTTools.class);
	private static final long COOKIE_TIMEOUT = 900000; //1000ms * 60s * 15m
	
	public static String createUniqueKey()
	{
		return new ObjectId().toString();
	}
	
	public static String decodeRESTPostParam(String name, Map<String,String> attributes)
	{
		String toDecode = (String) attributes.get(name);
		if (null == toDecode) {
			return null;
		}
		else {
			return decodeURL(toDecode);
		}
	}

	public static String decodeRESTParam(String name, Map<String,Object> attributes)
	{
		String toDecode = (String) attributes.get(name);
		if (null == toDecode) {
			return null;
		}
		else {
			return decodeURL(toDecode);
		}
	}
	
	public static String decodeURL(String toDecode)
	{
		String decodedString = "";
		try
		{
			decodedString = URLDecoder.decode( toDecode, "UTF-8");
		}
		catch (Exception e)
		{
			logger.error("Line: [" + e.getStackTrace()[2].getLineNumber() + "] " + e.getMessage());
			e.printStackTrace();
		}
		return decodedString;
	}
	
	/**
	 * Creates a new session for a user, adding
	 * an entry to our cookie table (maps cookieid
	 * to userid) and starts the clock
	 * 
	 * @param username
	 * @param bMulti if true lets you login from many sources
	 * @param bOverride if false will fail if already logged in
	 * @return
	 */
	public static ObjectId createSession( ObjectId userid, boolean bMulti, boolean bOverride )
	{
		
		try
		{
			DBCollection cookieColl = DbManager.getSocial().getCookies();
			
			if (!bMulti) { // Otherwise allow multiple cookies for this user
				//remove any old cookie for this user
				BasicDBObject dbQuery = new BasicDBObject();
				dbQuery.put("profileId", userid);
				dbQuery.put("apiKey", new BasicDBObject(DbManager.exists_, false));
				DBCursor dbc = cookieColl.find(dbQuery);
				if (bOverride) {
					while (dbc.hasNext()) {
						cookieColl.remove(dbc.next());
					}
				}//TESTED
				else if (dbc.length() > 0) {
					return null;
				}//TESTED
			}
			//Find user
			//create a new entry
			CookiePojo cp = new CookiePojo();
			cp.set_id(new ObjectId());
			cp.setCookieId(cp.get_id());
			cp.setLastActivity(new Date());
			cp.setProfileId(userid);
			cp.setStartDate(new Date());
			cookieColl.insert(cp.toDb());
			//return cookieid
			return cp.getCookieId();
		}
		catch (Exception e )
		{
			logger.error("Line: [" + e.getStackTrace()[2].getLineNumber() + "] " + e.getMessage());
			e.printStackTrace();
		}
		
		return null;
	}
	
	/**
	 * Users the cookieid that is saved to the browser to look up a the
	 * currently logged in users id
	 * 
	 * Checks if cookie has been active in last 15 minutes otherwise denies 
	 * request and removes cookie.
	 * 
	 * If cookie is active, updates last access time.
	 * 
	 * @param cookieIdStr
	 * @return Returns userid associated with this cookie
	 */
	public static String cookieLookup(String cookieIdStr)
	{
		if ((null == cookieIdStr) || cookieIdStr.equalsIgnoreCase("null") || cookieIdStr.isEmpty())
			return null;
		try
		{
			//remove any old cookie for this user
			CookiePojo cookieQuery = new CookiePojo();
			if (cookieIdStr.startsWith("api:")) {
				cookieQuery.setApiKey(cookieIdStr.substring(4));
			}
			else {
				cookieQuery.set_id(new ObjectId(cookieIdStr));
			}
			BasicDBObject cookieQueryDbo = (BasicDBObject) cookieQuery.toDb();
			DBObject dbo = DbManager.getSocial().getCookies().findOne(cookieQueryDbo);
			ObjectId userid = null;
			if ( dbo != null )
			{
				CookiePojo cp = CookiePojo.fromDb(dbo, CookiePojo.class);
				if (null != cp.getApiKey()) {
					userid = cp.getProfileId();
					// (no activity/timeout)
				}//TESTED
				else {
					PropertiesManager props = new PropertiesManager();
					Long timeout_s = props.getApiTimeoutSeconds();
					long nTimeout_ms = (null == timeout_s)? COOKIE_TIMEOUT : 1000L*timeout_s;
					if ( (new Date().getTime() - cp.getLastActivity().getTime()) < nTimeout_ms) 
					{
						//less than 15min, update activity time
						cp.setLastActivity(new Date());
						DbManager.getSocial().getCookies().update(cookieQueryDbo, cp.toDb());
						userid = cp.getProfileId();
					}
					else
					{
						//to late, drop cookie
						DbManager.getSocial().getCookies().remove(dbo);
					}
				}//TESTED
			}
			if (null == userid) {
				return null;
			}
			return userid.toString();
		}
		catch (Exception e )
		{
			logger.error("Line: [" + e.getStackTrace()[2].getLineNumber() + "] " + e.getMessage());
			e.printStackTrace();			
		}
		return null;
	}

	public static boolean adminLookup(String personIdStr) 
	{
		if ( null == personIdStr)
			return false;
		try
		{
			AuthenticationPojo authQuery = new AuthenticationPojo();
			authQuery.setProfileId(new ObjectId(personIdStr));
			BasicDBObject dbo = (BasicDBObject) DbManager.getSocial().getAuthentication().findOne(authQuery.toDb());
			if (null != dbo) {
				AuthenticationPojo ap = AuthenticationPojo.fromDb(dbo, AuthenticationPojo.class);			
				return ap.getAccountType().equalsIgnoreCase("admin");
			}
			else { 
				return false;
			}
		}
		catch (Exception e )
		{
			logger.error("Line: [" + e.getStackTrace()[2].getLineNumber() + "] " + e.getMessage());
			e.printStackTrace();			
		}
		return false;
	}

	
	/**
	 * validateCommunityIds
	 * @param userIdStr
	 * @param communityIdStrList
	 * @return
	 */
	public static boolean validateCommunityIds(String userIdStr, String communityIdStrList) 
	{
		// Trivial case:
		if ( (null == communityIdStrList) || (communityIdStrList.isEmpty()) || (communityIdStrList.charAt(0) < 0x30) ) 
		{
			// (<0x30 => is a regex, don't need to validate since check vs person pojos anyway) 
			return true;
		}
		if (null == userIdStr) {
			return false;
		}
		if (RESTTools.adminLookup(userIdStr)) {
			return true;
		}
		
		String[] communities =  communityIdStrList.split(",");
		
		// User's personal community included in list of communities
		if (communityIdStrList.contains(userIdStr)) return true;
		
		ObjectId[] communityObjects = new ObjectId[communities.length];
		for (int i = 0; i < communities.length; i++)
		{
			communityObjects[i] = new ObjectId(communities[i]);
		}
		
		// Get object Id for owner test
		ObjectId userId = null;
		try {
			userId = new ObjectId(userIdStr);
		}
		catch (Exception e) {
			userId = new ObjectId("0"); // (dummy user id)
		}			
		try
		{
			//check in mongo a user is part of these groups		
			BasicDBObject query = new BasicDBObject("_id",new BasicDBObject("$in",communityObjects) ); 
			List<CommunityPojo> retCommunities = CommunityPojo.listFromDb(DbManager.getSocial().getCommunity().find(query), CommunityPojo.listType());
			if ( retCommunities.size() == communities.length ) //make sure it found a group for all the id's
			{
				for (CommunityPojo cp: retCommunities)
				{
					//check to make sure user is a member or is his personal community (communityid and userid will be the same)
					if ( !cp.getId().equals(new ObjectId(userIdStr))) //this is NOT a personal group so check we are a member
					{
						if (!userId.equals(cp.getOwnerId())) { // (if you're owner you can always have it)
							if ( !cp.isMember(new ObjectId(userIdStr)) ) //if user is not a member of this group, return false
								return false;
						}
					}
				}
			}
			else
			{
				//error wrong number of groups returned meaning incorrect community ids were sent (groups dont exist)
				return false;				
			}
		}
		catch (Exception ex)
		{
			return false;
		}		
		return true; //made it thru the gauntlet, return successful
	}
	
	// Get a list of communities from a user and an optional regex
	
	static public HashSet<ObjectId> getUserCommunities(String userIdStr) {
		return getUserCommunities(userIdStr, null);
	}
	
	static public HashSet<ObjectId> getUserCommunities(String userIdStr, Pattern regex) {
		PersonPojo person = PersonHandler.getPerson(userIdStr);
		HashSet<ObjectId> memberOf = new HashSet<ObjectId>();
		if (null != person) {
			if (null != person.getCommunities()) {
				for (PersonCommunityPojo community: person.getCommunities()) {
					if ((null == regex) || regex.matcher(community.getName()).find()) {
						memberOf.add(community.get_id());
					}
				}
			}
		}
		return memberOf;
	}//TESTED
	
	static public boolean mustComeFromAuthority(PropertiesManager properties, String ipAddress, String cookie, String admuser, String admpass) 
	{
		 boolean allowedToRegisterUpdate = false;	
		 if ( properties.isSaasDeployment() ) //if saas, must come from trusted ip
		 {				 		 
			 String[] trustedDnsNames = properties.getSaasTrustedDns().split("\\s*,\\s*");
			 for (String dns: trustedDnsNames) 
			 {
				 InetAddress authIpAddress;
				 try 
				 {
					authIpAddress = InetAddress.getByName(dns);
				 }
				 catch (UnknownHostException e) 
				 {
					return false;
				 }
				 if (ipAddress.equals(authIpAddress.getHostAddress())) 
				 {
					 allowedToRegisterUpdate = true;
					 break;
				 }
			 } // (end loop over allowed DNS)
		 } 
		 if (!allowedToRegisterUpdate && (null != cookie)) //if not saas, must be an admin
		 {
			 String cookieLookup = RESTTools.cookieLookup(cookie); //get cookie to check admin
			 allowedToRegisterUpdate = RESTTools.adminLookup(cookieLookup);
		 }			 
		 
		 if (!allowedToRegisterUpdate && (null != admuser) && (null != admpass)) { 
			 // ie IP address / cookie doesn't match, system is also allowed to use admin user/pass:
			 
			 // 1. Check user/pass is valid:
			 
			 // 2. Check user is admin
			 try {
				 BasicDBObject query = new BasicDBObject();
				 query.put("username", admuser);
				 if (44 != admpass.length()) { // hash if in the clear
					 admpass = PasswordEncryption.encrypt(admpass);
				 }
				 query.put("password", admpass); // Needs to be hashed
				 query.put("accountType", "admin");
				 if (DbManager.getSocial().getAuthentication().find(query).count() > 0) {
					 allowedToRegisterUpdate = true;
				 }
			 }
			 catch (Exception e) {
				 // Do nothing
			 }
		 }		
		 return allowedToRegisterUpdate;
		 
	}//TESTED (just moved code across from PersonInterface)
	
	// Utility function to get communities entered in the following formats:
	// "*" for all user communities
	// "*<regex>" to apply a regex to the community names
	// "<id1>,<id2>,etc"
	
	static public String[] getCommunityIds(String userIdStr, String communityIdStrList) {
		
		if (communityIdStrList.charAt(0) < 0x30) {
			Pattern communityRegex = null;
			if (communityIdStrList.length() > 1) {
				communityRegex = Pattern.compile(communityIdStrList.substring(1), Pattern.CASE_INSENSITIVE);
			}
			HashSet<ObjectId> allCommunities = getUserCommunities(userIdStr, communityRegex);
			String[] communityIds = new String[allCommunities.size()];
			int i = 0; 
			for (ObjectId oid: allCommunities) {
				communityIds[i] = oid.toString();
				++i;
			}
			return communityIds;
		}
		else {
			 String[] communityIdStrs = communityIdStrList.split("\\s*,\\s*");
			 return communityIdStrs;
		}
	} //TESTED
	
}
