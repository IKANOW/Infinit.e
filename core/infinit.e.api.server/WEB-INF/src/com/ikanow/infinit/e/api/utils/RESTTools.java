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
import java.security.SecureRandom;
import java.util.Date;
import java.util.Map;

import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import org.restlet.data.Form;
import org.restlet.resource.ServerResource;

import com.ikanow.infinit.e.api.authentication.PasswordEncryption;
import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.social.authentication.AuthenticationPojo;
import com.ikanow.infinit.e.data_model.store.social.cookies.CookiePojo;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

public class RESTTools 
{
	private static final Logger logger = Logger.getLogger(RESTTools.class);
	private static final long COOKIE_TIMEOUT = 900000; //1000ms * 60s * 15m
	public static final String AUTH_TOKEN_NAME = "inf_token";
	private static InMemoryCache<String> cache = new InMemoryCache<String>(5000); //5s
	
	public static ObjectId generateRandomId() {
		SecureRandom randomBytes = new SecureRandom();
		byte bytes[] = new byte[12];
		randomBytes.nextBytes(bytes);
		return new ObjectId(bytes); 		
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

	public static String getEncodedRESTParam(String name, Map<String,Object> attributes)
	{
		return (String) attributes.get(name);
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
			ObjectId randomObjectId = generateRandomId();
			
			cp.set_id(randomObjectId); 
			cp.setCookieId(randomObjectId);
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
		
		//check cache first
		String entry = cache.getEntry(cookieIdStr);
		if ( entry != null )
			return entry;
		
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
			//put in cache
			cache.addEntry(cookieIdStr, userid.toString());
			return userid.toString();
		}
		catch (Exception e )
		{
			logger.error("Line: [" + e.getStackTrace()[2].getLineNumber() + "] " + e.getMessage());
			e.printStackTrace();			
		}
		return null;
	}

	// By default, admin must be enabled
	public static boolean adminLookup(String personIdStr) 
	{
		return adminLookup(personIdStr, true);
	}
	public static boolean adminLookup(String personIdStr, boolean mustBeEnabled) 
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
				return adminCheck(ap, mustBeEnabled);
			}
			return false;
		}
		catch (Exception e )
		{
			logger.error("Line: [" + e.getStackTrace()[2].getLineNumber() + "] " + e.getMessage(), e);
			e.printStackTrace();			
		}
		catch (Error e) {
			logger.error("isAdminError", e);
			e.printStackTrace();						
		}
		return false;
	}

	// Utility function for the two types of authentication	
	public static boolean adminCheck(AuthenticationPojo ap, boolean mustBeEnabled) {
		if (null != ap.getAccountType()) {
			if (ap.getAccountType().equalsIgnoreCase("admin")) {
				return true;
			}
			else if (ap.getAccountType().equalsIgnoreCase("admin-enabled")) {
				if (!mustBeEnabled) {
					return true;
				}
				else if (null != ap.getLastSudo()) {
					if ((ap.getLastSudo().getTime() + 10*60*1000) > new Date().getTime()) {
						// (ie admin rights last 10 minutes)
						return true;
					}
				}
			}
		}//TESTED		
		return false;
	}	

	public static void logRequest(ServerResource requestHandle) {
		Form headers = (Form) requestHandle.getResponseAttributes().get("org.restlet.http.headers");
		if (null == headers) {
			headers = new Form();
			requestHandle.getResponseAttributes().put("org.restlet.http.headers", headers);
		}
		headers.add("X-infinit.e.log", "1");		
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
			 String cookieLookup = cookieLookup(cookie); //get cookie to check admin
			 allowedToRegisterUpdate = adminLookup(cookieLookup);
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
				 query.put("password", admpass); 
				 
				 AuthenticationPojo ap = AuthenticationPojo.fromDb(DbManager.getSocial().getAuthentication().findOne(query), AuthenticationPojo.class); 
				 if (null != ap) {
					 allowedToRegisterUpdate = adminCheck(ap, true);
				 }//TESTED (admin, admin-enabled, non-admin)
			 }
			 catch (Exception e) {
				 // Do nothing
			 }
		 }		
		 return allowedToRegisterUpdate;
		 
	}//TESTED (just moved code across from PersonInterface)
	
	
	private final static String ATTRIBUTE_VAR_PREFIX = "$";
	/**
	 * Looks in attributes for attr_name.  If nothing is there returns null.
	 * If result is a String that starts with $var_name will look in queryOptions
	 * for var_name and return that instead, otherwise null.
	 * 
	 * 
	 * @param attr_name
	 * @param attributes
	 * @param queryOptions
	 * @return
	 */
	public static String getUrlAttribute(String attr_name, Map<String,Object> attributes, Map<String, String> queryOptions)
	{
		String attr = RESTTools.getEncodedRESTParam(attr_name, attributes);
		if ( attr != null )
		{
			if ( attr.startsWith(ATTRIBUTE_VAR_PREFIX))
			{
				//special case to grab from queryOptions instead of attribute
				String attr_var_name = attr.substring(ATTRIBUTE_VAR_PREFIX.length());
				return (String)queryOptions.get(attr_var_name); // (ie or null
			}
			else
			{
				//was just in attribute
				return decodeURL(attr);
			}
		}
		return null;
	}
	
}
