package com.ikanow.infinit.e.api.social.community;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;
import org.restlet.resource.ResourceException;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;

import com.ikanow.infinit.e.api.Parameters;
import com.ikanow.infinit.e.api.utils.PropertiesManager;
import com.ikanow.infinit.e.api.utils.RESTTools;
import com.ikanow.infinit.e.data_model.api.ResponsePojo;
import com.ikanow.infinit.e.data_model.api.ResponsePojo.ResponseObject;
import com.ikanow.infinit.e.data_model.api.authentication.WordPressAuthPojo;
import com.ikanow.infinit.e.data_model.api.authentication.WordPressUserPojo;

/**
 * @author cvitter
 */
public class PersonInterface extends Resource
{
	private String personId = null;
	
	//private String updateItem = null;
	private PersonHandler person = new PersonHandler();
	private String action = "";
	private String cookieLookup = null;
	private Parameters parameters = null;
	private String wpuser = null;
	private String wpauth = null;
	private String wpsetup = null;
	private String admuser = null;
	private String admpass = null;
	private String cookie = null;
	private boolean needCookie = true;
	private String ipAddress = null;
	
	
	public PersonInterface(Context context, Request request, Response response) 
	{
		 super(context, request, response);
		 
		 Map<String,Object> attributes = request.getAttributes();
		 ipAddress =  request.getClientInfo().getAddress();
		 
		 //Every user must pass in their cookies	
		 cookie = request.getCookies().getFirstValue("infinitecookie", true);
		 
		 parameters = new Parameters(request.getResourceRef().getQueryAsForm());
		 Map<String, String> queryOptions = this.getQuery().getValuesMap();
		 admuser = queryOptions.get("admuser");
		 admpass = queryOptions.get("admpass");
		 
		 String urlStr = request.getResourceRef().toString();
		 
		 //Optional query object (else is a POST)
		 if (Method.POST == request.getMethod()) 
		 {
			 if ( urlStr.contains("/person/register") ) {
				 needCookie = false;
				 action = "register";				 
			 }
			 else if ( urlStr.contains("/person/update") ) {
				 needCookie = false;
				 action = "wpupdate";				 
			 }
		 }
		 else // Get request
		 {			 
			 if (urlStr.contains("/person/get"))
			 {
				 personId = RESTTools.decodeRESTParam("personid", attributes);
				 action = "getPerson";			 
			 }
			 else if ( urlStr.contains("/person/register") || urlStr.contains("/people/register"))
			 {
				 needCookie = false;
				 wpuser = RESTTools.decodeRESTParam("wpuser", attributes);
				 wpauth = RESTTools.decodeRESTParam("wpauth", attributes);
				 if (null == wpuser) {
					 wpuser = parameters.getWpuser();					 
				 }
				 if (null == wpauth) {
					 wpauth = parameters.getWpauth();					 
				 }
				 action = "register";
			 }
			 else if ( urlStr.contains("/person/delete") )
			 {
				 needCookie = false;
				 personId = RESTTools.decodeRESTParam("userid", attributes);
				 action = "delete";
			 }
			 else if ( urlStr.contains("/person/update") || urlStr.contains("/person/wpupdate") || urlStr.contains("/people/wpupdate"))
			 {
				 needCookie = false;
				 wpuser = RESTTools.decodeRESTParam("wpuser", attributes);
				 wpauth = RESTTools.decodeRESTParam("wpauth", attributes);
				 if (null == wpuser) {
					 wpuser = parameters.getWpuser();					 
				 }
				 if (null == wpauth) {
					 wpauth = parameters.getWpauth();					 
				 }
				 if (urlStr.contains("/person/update/password/")) { //special case: update password
					 WordPressUserPojo user = new WordPressUserPojo();
					 WordPressAuthPojo auth = new WordPressAuthPojo();
					 user.setWPUserID(wpuser); // (this is ignored if a normal user is logged in)
					 auth.setPassword(wpauth);
					 wpuser = user.toApi();
					 wpauth = auth.toApi();
				 }
				 else if (urlStr.contains("/person/update/email/")) { //special case: update email
					 WordPressUserPojo user = new WordPressUserPojo();
					 WordPressAuthPojo auth = new WordPressAuthPojo();
					 user.setWPUserID(wpuser); // (this is ignored if a normal user is logged in)
					 user.setEmail(Arrays.asList(wpauth));
					 wpuser = user.toApi();
					 wpauth = auth.toApi();
				 }
				 action = "wpupdate";
			 }
			 getVariants().add(new Variant(MediaType.APPLICATION_JSON));
		 }
		 // All modifications of this resource
		 this.setModifiable(true);		 
	}
	
	//___________________________________________________________________________________
	
	/**
	 * Handles a POST
	 * 
	 * @param entity
	 * @return
	 * @throws ResourceException
	 */

	public void acceptRepresentation(Representation entity) throws ResourceException 
	{

		if (Method.POST == getRequest().getMethod()) 
		{
			try {
				wpsetup = entity.getText();
			} catch (IOException e) {
				// Do nothing, the error should bubble back to the user itself
			}
		}		 
		Representation response = represent(null);
		this.getResponse().setEntity(response);
	}//TESTED
	
	public Representation represent(Variant variant) throws ResourceException 
	{
		 ResponsePojo rp = new ResponsePojo(); 
		 Date startTime = new Date();	

		 if ( !needCookie ) //wordpress calls go here
		 {
			 PropertiesManager properties = new PropertiesManager();
			 boolean allowedToRegisterUpdate = RESTTools.mustComeFromAuthority(properties, ipAddress, cookie, admuser, admpass);	

			 // Assuming one of the above forms of authentication worked...
			 
			 if (allowedToRegisterUpdate ) 
			 {
				 if ( action.equals("register"))
				 {
					 rp = this.person.registerWPUser(cookieLookup, wpuser,wpauth,wpsetup);
				 }
				 else if ( action.equals("wpupdate"))
				 {
					 rp = this.person.updateWPUser(wpuser,wpauth,wpsetup, null);
				 }
				 else if ( action.equals("delete"))
				 {
					 rp = this.person.deleteWPUser(personId);
				 }
			 }
			 else { // Can only update...(must be logged in obviously)
				 
				 if ( action.equals("wpupdate") && (null != (cookieLookup = RESTTools.cookieLookup(cookie))))
				 {
					 rp = this.person.updateWPUser(wpuser,wpauth,wpsetup, cookieLookup);
				 }
				 else {
					 rp.setResponse(new ResponseObject("Cookie Lookup",false,"Insufficient privileges or not logged in"));
				 }
			 }
		 }
		 else //some normal call
		 {
			 cookieLookup = RESTTools.cookieLookup(cookie);
			 if (cookieLookup == null )
			 {
				 rp = new ResponsePojo();
				 rp.setResponse(new ResponseObject("Cookie Lookup",false,"Cookie session expired or never existed, please login first"));
			 }
			 else
			 {
				 if (action.equals("getPerson"))
				 {
					 if (personId == null || personId.length() < 1) personId = cookieLookup;
					 rp = this.person.getPerson(personId, false);
				 }
			 }
		 }
		 
		 Date endTime = new Date();
		 if (null != rp.getResponse()) {
			 rp.getResponse().setTime(endTime.getTime() - startTime.getTime());
		 }
		 return new StringRepresentation(rp.toApi(), MediaType.APPLICATION_JSON);
	}

}
