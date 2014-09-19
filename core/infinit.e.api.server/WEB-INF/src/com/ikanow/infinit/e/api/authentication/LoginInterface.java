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
package com.ikanow.infinit.e.api.authentication;

import java.util.Date;
import java.util.Map;

import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.CookieSetting;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;
import org.restlet.util.Series;

import com.ikanow.infinit.e.api.Parameters;
import com.ikanow.infinit.e.api.utils.PropertiesManager;
import com.ikanow.infinit.e.api.utils.RESTTools;
import com.ikanow.infinit.e.data_model.api.ResponsePojo;
import com.ikanow.infinit.e.data_model.api.ResponsePojo.ResponseObject;
import com.ikanow.infinit.e.data_model.index.ElasticSearchManager;
import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.MongoDbManager;
import com.ikanow.infinit.e.data_model.store.social.authentication.AuthenticationPojo;
import com.ikanow.infinit.e.data_model.store.social.cookies.CookiePojo;

public class LoginInterface extends ServerResource 
{
	private static final Logger logger = Logger.getLogger(LoginInterface.class);

	private Parameters parameters = null;
	private String user = null;
	private String pass = null;
	private boolean isLogin = false;	
	private String action = "";
	private String ipAddress = null;
	private static final StringBuffer logMsg = new StringBuffer();	
	private String cookieLookup = null;
	private String cookie = null;
	private boolean mustComeFromAuthority = false;
	private String admuser = null;
	private String admpass = null;
	private boolean multi = false;
	private boolean override = true;
	private boolean returnCookieInJson = false;

	//public LoginInterface(Context context, Request request, Response response) throws IOException 
	@Override
	public void doInit()
	{
		//super(context, request, response);
		Response response = this.getResponse();
		Request request = this.getRequest();
		String urlStr = request.getResourceRef().toString();
		Map<String,Object> attributes = request.getAttributes();
		parameters = new Parameters(request.getResourceRef().getQueryAsForm());
		ipAddress = request.getClientInfo().getAddress();
		Map<String, String> queryOptions = this.getQuery().getValuesMap();
		admuser = queryOptions.get("admuser");
		admpass = queryOptions.get("admpass");
		String returnCookieInJsonStr =  queryOptions.get("return_tmp_key");
		if ((null != returnCookieInJsonStr) && (returnCookieInJsonStr.equalsIgnoreCase("true") || returnCookieInJsonStr.equalsIgnoreCase("1")))
		{
			returnCookieInJson = true;
		}

		if ( urlStr.contains("auth/login") )
		{	
			action = "login";
			//Allow users to use login/user/pass or login/?username=bob&password=12345
			if ( parameters.getUsername() == null ) {
				user = RESTTools.decodeRESTParam("user",attributes);
			}
			else {
				user = parameters.getUsername();
			}
			if ( parameters.getPassword() == null ) {
				pass = RESTTools.decodeRESTParam("pass",attributes);
			}
			else {
				pass = parameters.getPassword();
			}
			if (null != parameters.getMulti()) { // (For API/REST purposes, allows users to login multiple times)
				multi = parameters.getMulti(); // (note: only allowed for admin, below)
			}
			if (null != parameters.getOverride()) {
				override = parameters.getOverride();
			}

			//Verify login
			if (!user.equals("ping")) { // (this is a reserved username for LB pinging)
				AuthenticationPojo authuser = PasswordEncryption.validateUser(user,pass);

				if ( authuser != null )
				{
					// Since logging-in isn't time critical, we'll ensure that api users have their api cookie at this point...
					if (null != authuser.getApiKey()) {
						CookiePojo cp = new CookiePojo();
						cp.set_id(authuser.getProfileId());
						cp.setCookieId(cp.get_id());
						cp.setApiKey(authuser.getApiKey());
						cp.setStartDate(authuser.getCreated());
						cp.setProfileId(authuser.getProfileId());
						DbManager.getSocial().getCookies().save(cp.toDb());						 
					}//TESTED

					if ((authuser.getAccountType() == null) ||
							!(authuser.getAccountType().equalsIgnoreCase("admin") || authuser.getAccountType().equalsIgnoreCase("admin-enabled")))
					{
						multi = false; // (not allowed except for admin)
					}

					CookieSetting cookieId = createSessionCookie(authuser.getProfileId(), true, response.getServerInfo().getPort());
					if (null != cookieId) {

						Series<CookieSetting> cooks = response.getCookieSettings();				 
						cooks.add(cookieId);
						response.setCookieSettings(cooks);
						isLogin = true;
						cookieLookup = cookieId.getValue();
						boolean bAdmin = false;

						//If this request is checking admin status, check that
						if (urlStr.contains("/admin/"))
						{
							isLogin = false;
							if (authuser.getAccountType().equalsIgnoreCase("admin")) {
								bAdmin = true;
								isLogin = true;
							}
							else if (authuser.getAccountType().equalsIgnoreCase("admin-enabled")) {
								isLogin = true;
								if (!multi) {
									authuser.setLastSudo(new Date());
									MongoDbManager.getSocial().getAuthentication().save(authuser.toDb());
									bAdmin = true;
								}
							}
						}//TESTED

						logMsg.setLength(0);
						logMsg.append("auth/login");
						logMsg.append(" user=").append(user);
						logMsg.append(" userid=").append(authuser.getProfileId().toString());
						if (bAdmin) logMsg.append(" admin=true");
						logMsg.append(" success=").append(isLogin);
						logger.info(logMsg.toString());
					}
				}
			}
			else { // Check if the index instance is running
				// (this will also update the node replication for local-only indexes - handy side effect!)
				boolean bAllGood = true;
				ElasticSearchManager esm = null;
				esm = ElasticSearchManager.getIndex("association_index");
				bAllGood &= esm.pingIndex();
				esm = ElasticSearchManager.getIndex("entity_index");
				bAllGood &= esm.pingIndex();

				bAllGood &= (DbManager.getSocial().getCommunity().count() > 0);
				// (also test connection to the DB)

				if (!bAllGood) 
				{
					throw new RuntimeException("Index not running");
				}
			}//TESTED
			//If a redirect is supplied, send them there after logging in if successful
			if ( parameters.getReturnURL() != null )
			{
				response.redirectTemporary(parameters.getReturnURL() + "?success=" + isLogin);
			}
		}
		else if ( urlStr.contains("auth/forgotpassword"))
		{
			action = "forgot";
			user = parameters.getUsername();				 
			pass = parameters.getPassword();

			mustComeFromAuthority = true;
		}
		else if (urlStr.contains("auth/deactivate"))
		{
			user = parameters.getUsername();
			cookie = request.getCookies().getFirstValue("infinitecookie",true);
			action = "deactivate";

			mustComeFromAuthority = true;
		}
		else if ( urlStr.contains("auth/keepalive/admin"))
		{
			if (null != parameters.getOverride()) {
				override = parameters.getOverride();
			}
			cookie = request.getCookies().getFirstValue("infinitecookie",true);
			action ="admin-keepalive";
		}
		else if ( urlStr.contains("auth/keepalive"))
		{
			cookie = request.getCookies().getFirstValue("infinitecookie",true);
			action ="keepalive";
		}
		else if ( urlStr.contains("auth/logout/admin"))
		{
			cookie = request.getCookies().getFirstValue("infinitecookie",true);
			action ="admin-logout";			 
		}
		else if ( urlStr.contains("auth/logout"))
		{
			cookie = request.getCookies().getFirstValue("infinitecookie",true);

			action ="logout";

			Series<CookieSetting> cooks = response.getCookieSettings();				 
			cooks.add(createSessionCookie(null, false, response.getServerInfo().getPort()));
			response.setCookieSettings(cooks);
		}		 
	}

	private CookieSetting createSessionCookie(ObjectId user, boolean bSet, int nClientPort)
	{
		//Create a new objectId to map this cookie to a userid
		String set = null;
		if (bSet) {
			ObjectId cookieId = RESTTools.createSession(user, multi, override);
			if (null == cookieId) { 
				return null;
			}
			set = cookieId.toString();
		}
		else {
			set = "";
		}
		CookieSetting cs = null;
		//store in mongo (or whatever db we need)
		try
		{
			cs = new CookieSetting("infinitecookie",set);
			cs.setPath("/");
			cs.setAccessRestricted(true);
			if ((443 == nClientPort) || (8443 == nClientPort)) {
				cs.setSecure(true);
			}
		}
		catch (Exception ex)
		{
			logger.error("Line: [" + ex.getStackTrace()[2].getLineNumber() + "] " + ex.getMessage());
		}
		return cs;
	}

	/**
	 * Represent the user object in the requested format.
	 * 
	 * @param variant
	 * @return
	 * @throws ResourceException
	 */
	//public Representation represent(Variant variant) throws ResourceException
	@Get
	public Representation get() throws ResourceException
	{
		ResponsePojo rp = new ResponsePojo();
		Date startTime = new Date();	 
		if ( action.equals("login"))
		{
			rp = new ResponsePojo(new ResponseObject("Login",isLogin,null));
			if (returnCookieInJson)
				rp.setData((String)cookieLookup, null);
		}
		else if ( action.equals("admin-keepalive"))
		{
			if (null != cookie) {
				cookieLookup = RESTTools.cookieLookup(cookie);
			}
			if (null != cookieLookup) {
				AuthenticationPojo query = new AuthenticationPojo();
				query.setProfileId(new ObjectId(cookieLookup));
				AuthenticationPojo authUser = AuthenticationPojo.fromDb(DbManager.getSocial().getAuthentication().findOne(query.toDb()), AuthenticationPojo.class);
				if ((authUser.getAccountType() != null) && authUser.getAccountType().equalsIgnoreCase("admin")) {
					rp = new LoginHandler().keepAlive(cookieLookup, true);					
				}
				else if ((authUser.getAccountType() != null) && authUser.getAccountType().equalsIgnoreCase("admin-enabled")) { // keepalive - only updates auth pojo if needed
					boolean bUpdateCookie = false;
					if (null == authUser.getLastSudo()) {
						bUpdateCookie = true;
					}
					else if ((authUser.getLastSudo().getTime() + 10*60*1000) < new Date().getTime()) {
						// (ie admin rights last 10 minutes)
						bUpdateCookie = true;
					}
					if (bUpdateCookie && override) {
						authUser.setLastSudo(new Date());
						MongoDbManager.getSocial().getAuthentication().save(authUser.toDb());
					}
					rp = new LoginHandler().keepAlive(cookieLookup, override || !bUpdateCookie);
					// (ie if we're overriding we must be ... also if we're not override but are within the last sudo range then we are)
				}
				else {					
					rp.setResponse(new ResponseObject("Keepalive", false, "Logged in but not admin."));
				}
			}//TESTED
			else {
				rp.setResponse(new ResponseObject("Keepalive", false, "Not logged in."));
			}
		}
		else if ( action.equals("keepalive"))
		{
			if (null != cookie) {
				cookieLookup = RESTTools.cookieLookup(cookie);
			}
			if (null != cookieLookup) {
				rp = new LoginHandler().keepAlive(cookieLookup);
			}
			else {
				rp.setResponse(new ResponseObject("Keepalive", false, "Not logged in."));
			}
		}
		else if ( action.equals("admin-logout"))
		{
			cookieLookup = RESTTools.cookieLookup(cookie);
			if (null != cookieLookup) {
				AuthenticationPojo query = new AuthenticationPojo();
				query.setProfileId(new ObjectId(cookieLookup));
				AuthenticationPojo authUser = AuthenticationPojo.fromDb(DbManager.getSocial().getAuthentication().findOne(query.toDb()), AuthenticationPojo.class);			
				if ((null != authUser) && (null != authUser.getLastSudo())) {
					authUser.setLastSudo(null);
					MongoDbManager.getSocial().getAuthentication().save(authUser.toDb());
				}
			}
			rp.setResponse(new ResponseObject("Logout", true, "No longer admin."));
		}
		else if ( action.equals("logout"))
		{
			cookieLookup = RESTTools.cookieLookup(cookie);
			if (null != cookieLookup) {
				rp = new LoginHandler().removeCookies(cookieLookup);
			}
			else {
				rp.setResponse(new ResponseObject("Logout", false, "Not logged in."));
			}
		}
		else if (this.mustComeFromAuthority) 
		{			 
			boolean bCanProceed = RESTTools.mustComeFromAuthority(new PropertiesManager(), ipAddress, cookie, admuser, admpass);

			if (bCanProceed) 
			{			 
				if (action.equals("forgot"))
				{	 
					rp = new LoginHandler().resetPassword(user, true);
				}
				else if (action.equals("deactivate"))
				{
					rp = new LoginHandler().deactivateAccount(user);
				}
			}
			else if (action.equals("forgot")) 
			{ 
				// This has come from the user, part of 2 stage process
				// No password specified
				if (null == pass) 
				{ 
					rp = new LoginHandler().resetPassword(user, false);
				}
				else { // Validate password, allow reset if valid
					AuthenticationPojo authuser = PasswordEncryption.validateUser(user,pass);
					if (null != authuser) {
						rp = new LoginHandler().resetPassword(user, true);
					}
				}
			}
		}

		Date endTime = new Date();
		rp.getResponse().setTime(endTime.getTime() - startTime.getTime());
		
		if (!rp.getResponse().isSuccess()) {
			if ((!action.equals("keepalive")) && ((null == user) || !user.equals("ping"))) {
				// (not a keepalive) and (user!=ping)
				RESTTools.logRequest(this);
			}
		}//TOTEST (TODO-2194)
		return new StringRepresentation(rp.toApi(), MediaType.APPLICATION_JSON);
	}		
}
