package com.ikanow.infinit.e.api.authentication;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import org.restlet.Context;
import org.restlet.data.CookieSetting;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;
import org.restlet.resource.ResourceException;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;
import org.restlet.util.Series;

import com.ikanow.infinit.e.api.Parameters;
import com.ikanow.infinit.e.api.utils.PropertiesManager;
import com.ikanow.infinit.e.api.utils.RESTTools;
import com.ikanow.infinit.e.data_model.api.ResponsePojo;
import com.ikanow.infinit.e.data_model.api.ResponsePojo.ResponseObject;
import com.ikanow.infinit.e.data_model.index.ElasticSearchManager;
import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.social.authentication.AuthenticationPojo;

public class LoginInterface extends Resource 
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
	
	public LoginInterface(Context context, Request request, Response response) throws IOException 
	{
		 super(context, request, response);
		 String urlStr = request.getResourceRef().toString();
		 Map<String,Object> attributes = request.getAttributes();
		 parameters = new Parameters(request.getResourceRef().getQueryAsForm());
		 ipAddress = request.getClientInfo().getAddress();
		 Map<String, String> queryOptions = this.getQuery().getValuesMap();
		 admuser = queryOptions.get("admuser");
		 admpass = queryOptions.get("admpass");
		 
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
					 if ((authuser.getAccountType() == null) || !authuser.getAccountType().equalsIgnoreCase("admin"))
					 {
						 multi = false; // (not allowed except for admin)
					 }
					 
					 CookieSetting cookieId = createSessionCookie(authuser.getProfileId(), true);
					 if (null != cookieId) {
						 
						 Series<CookieSetting> cooks = response.getCookieSettings();				 
						 cooks.add(cookieId);
						 response.setCookieSettings(cooks);
						 isLogin = true;
	
						 //If this request is checking admin status, check that
						 if (urlStr.contains("/admin/") && (authuser.getAccountType() != null) && authuser.getAccountType().equalsIgnoreCase("admin"))
							 isLogin = true;
						 else if (urlStr.contains("/admin/"))
							 isLogin = false;
						 
						 logMsg.setLength(0);
						 logMsg.append("auth/login");
						 logMsg.append(" user=").append(user);
						 logMsg.append(" pword=").append(pass);
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
				 
				 if (!bAllGood) {
					 throw new IOException("Index not running");
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
			 pass = parameters.getPassword();
			 AuthenticationPojo authuser = PasswordEncryption.validateUser(user,pass);
			 if ( authuser != null )
				 action = "deactivate";
			 
			 mustComeFromAuthority = true;
		 }
		 else if ( urlStr.contains("auth/keepalive"))
		 {
			 cookie = request.getCookies().getFirstValue("infinitecookie",true);
			 action ="keepalive";
		 }
		 else if ( urlStr.contains("auth/logout"))
		 {
			 cookie = request.getCookies().getFirstValue("infinitecookie",true);
			 
			 action ="logout";
			 
			 Series<CookieSetting> cooks = response.getCookieSettings();				 
			 cooks.add(createSessionCookie(null, false));
			 response.setCookieSettings(cooks);
		 }
		 
		 // All modifications of this resource
		 this.setModifiable(true);
		 
		 getVariants().add(new Variant(MediaType.APPLICATION_JSON));
	}
	private CookieSetting createSessionCookie(ObjectId user, boolean bSet)
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
	public Representation represent(Variant variant) throws ResourceException 
	{
		 ResponsePojo rp = new ResponsePojo();
		 Date startTime = new Date();	 
		 if ( action.equals("login"))
		 {
			 rp = new ResponsePojo(new ResponseObject("Login",isLogin,null));
		 }
		 else if ( action.equals("keepalive"))
		 {
			 cookieLookup = RESTTools.cookieLookup(cookie);
			 if (null != cookieLookup) {
				 rp = new LoginHandler().keepAlive(cookieLookup);
			 }
			 else {
				 rp.setResponse(new ResponseObject("Keepalive", false, "Not logged in."));
			 }
		 }
		 else if ( action.equals("logout"))
		 {
			 cookieLookup = RESTTools.cookieLookup(cookie);
			 cookieLookup = RESTTools.cookieLookup(cookie);
			 if (null != cookieLookup) {
				 rp = new LoginHandler().removeCookies(cookieLookup);
			 }
			 else {
				 rp.setResponse(new ResponseObject("Logout", false, "Not logged in."));
			 }
		 }
		 else if (this.mustComeFromAuthority) {
			 
			 boolean bCanProceed = RESTTools.mustComeFromAuthority(new PropertiesManager(), ipAddress, cookie, admuser, admpass);
			 
			 if (bCanProceed) {
			 
				 if (action.equals("forgot"))
				 {	 
					 rp = new LoginHandler().resetPassword(user, true);
				 }
				 else if (action.equals("deactivate"))
				 {
					 rp = new LoginHandler().deactivateAccount(user,pass);
				 }
			 }
			 else if (action.equals("forgot")) { // This has come from the user, part of 2 stage process
				 
				 if (null == pass) { // No password specified
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
		 return new StringRepresentation(rp.toApi(), MediaType.APPLICATION_JSON);
	}		
}
