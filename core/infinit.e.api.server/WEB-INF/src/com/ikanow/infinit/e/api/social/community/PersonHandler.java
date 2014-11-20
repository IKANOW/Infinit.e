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
package com.ikanow.infinit.e.api.social.community;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.bson.types.ObjectId;

import com.ikanow.infinit.e.api.authentication.PasswordEncryption;
import com.ikanow.infinit.e.api.utils.RESTTools;
import com.ikanow.infinit.e.api.utils.SocialUtils;
import com.ikanow.infinit.e.data_model.InfiniteEnums.AccountStatus;
import com.ikanow.infinit.e.data_model.api.ResponsePojo;
import com.ikanow.infinit.e.data_model.api.ResponsePojo.ResponseObject;
import com.ikanow.infinit.e.data_model.api.authentication.AuthenticationPojoApiMap;
import com.ikanow.infinit.e.data_model.api.authentication.WordPressAuthPojo;
import com.ikanow.infinit.e.data_model.api.authentication.WordPressSetupPojo;
import com.ikanow.infinit.e.data_model.api.authentication.WordPressUserPojo;
import com.ikanow.infinit.e.data_model.api.social.person.PersonPojoApiMap;
import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.MongoDbManager;
import com.ikanow.infinit.e.data_model.store.social.authentication.AuthenticationPojo;
import com.ikanow.infinit.e.data_model.store.social.community.CommunityMemberPojo;
import com.ikanow.infinit.e.data_model.store.social.community.CommunityPojo;
import com.ikanow.infinit.e.data_model.store.social.cookies.CookiePojo;
import com.ikanow.infinit.e.data_model.store.social.person.PersonCommunityPojo;
import com.ikanow.infinit.e.data_model.store.social.person.PersonPojo;
import com.ikanow.infinit.e.harvest.utils.PropertiesManager;
import com.ikanow.infinit.e.processing.generic.GenericProcessingController;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

/**
 * @author Craig
 * */
public class PersonHandler 
{
	private static final Logger logger = Logger.getLogger(PersonHandler.class);
	
	//////////////////////////////////////////////////////////////////////////
	////////////////////////   REST handlers  ////////////////////////////////
	//////////////////////////////////////////////////////////////////////////
	/**
	 * getPersonInfo (REST)
	 * @param id
	 * @param isPublicRequest
	 * @return
	 */
	public ResponsePojo getPerson(String id, Boolean isPublicRequest)
	{
		//TODO (INF-502): there is no public vs private currently
		if ( isPublicRequest )
			return new ResponsePojo(new ResponseObject("Person Info",false,"There is no public get person info call currently, coming soon!"));
		
		
		ResponsePojo rp = new ResponsePojo();
		
		try
		{
			// Set up the query
			PersonPojo personQuery = new PersonPojo();
			try {
				personQuery.set_id(new ObjectId(id));
			}
			catch (Exception e) { // Not an id, try email
				personQuery.setEmail(id);
			}
			
			BasicDBObject dbo = (BasicDBObject) DbManager.getSocial().getPerson().findOne(personQuery.toDb());
			PersonPojo person = PersonPojo.fromDb(dbo, PersonPojo.class);
			
			rp.setData(person, new PersonPojoApiMap());
			rp.setResponse(new ResponseObject("Person Info", true, "Person info returned successfully"));	
		} 
		catch (Exception e)
		{
			logger.error("Exception Message: " + e.getMessage(), e);
			rp.setResponse(new ResponseObject("Person Info", false, "Error returning person info: " + e.getMessage()
					+ " - " + e.getStackTrace().toString()));
		}
		return rp;
	}	
	
	public ResponsePojo listPerson(String userId)
	{
		ResponsePojo rp = new ResponsePojo();
		try
		{
			PersonPojo person = SocialUtils.getPerson(userId);	
			boolean isAdmin = RESTTools.adminLookup(userId);
			CommunityPojo system_comm = getSystemCommunity();
			List<ObjectId> communityIds = new ArrayList<ObjectId>();
			for ( PersonCommunityPojo community : person.getCommunities())
			{
				ObjectId comm_id = community.get_id();
				if ( allowedToSeeCommunityMembers(comm_id, isAdmin, system_comm) )
				{
					communityIds.add(comm_id);
				}
			}
			BasicDBObject query = new BasicDBObject();
			query.put("communities._id", new BasicDBObject( MongoDbManager.in_, communityIds ));
			DBCursor dbc = DbManager.getSocial().getPerson().find(query);
			
			
			if (dbc.hasNext())
			{
				rp.setData(PersonPojo.listFromDb(dbc, PersonPojo.listType()), new PersonPojoApiMap());
				rp.setResponse(new ResponseObject("People List", true, "List returned successfully"));				
			}
			else
			{
				rp.setResponse(new ResponseObject("People List", true, "No list to return returned"));	
			}
			
		} 
		catch (Exception e)
		{
			logger.error("Exception Message: " + e.getMessage(), e);
			rp.setResponse(new ResponseObject("Person List", false, "Error returning person list: " + e.getMessage()
					+ " - " + e.getStackTrace().toString()));
		}
		return rp;
	}
	
	private boolean allowedToSeeCommunityMembers(ObjectId communityId, boolean isAdmin, CommunityPojo systemCommunity)
	{
		//admin can see everything, always return true
		if ( isAdmin )
			return true;
		else
		{
			//if this is the system community, check if publicMemberOverride is true
			if ( systemCommunity != null && systemCommunity.getId().equals(communityId))
			{
				if ( systemCommunity.getCommunityAttributes().containsKey("publishMemberOverride") && 
						systemCommunity.getCommunityAttributes().get("publishMemberOverride").getValue().equals("true"))
				{
					return true;
				}
				else
				{
					return false;
				}
			}
			else
			{
				//all other communities can show members for now
				return true;
			}
		}
	}
	
	private CommunityPojo getSystemCommunity()
	{
		BasicDBObject query = new BasicDBObject("isSystemCommunity", true);
		BasicDBObject dbo = (BasicDBObject)DbManager.getSocial().getCommunity().findOne(query);
		if (dbo != null)
		{
			return CommunityPojo.fromDb(dbo, CommunityPojo.class);	
		}
		return null;
	}
	
	/**
	 * getAllPeople (REST, CURRENTLY UNUSED)
	 * @return
	 */
	public ResponsePojo getAllPeople()
	{
		ResponsePojo rp = new ResponsePojo();
		try
		{
			DBCursor dbc = DbManager.getSocial().getPerson().find();
			
			if (dbc.hasNext())
			{
				rp.setData(PersonPojo.listFromDb(dbc, PersonPojo.listType()), new PersonPojoApiMap());
				rp.setResponse(new ResponseObject("People Info", true, "Info returned successfully"));				
			}
			else
			{
				rp.setResponse(new ResponseObject("People Info", true, "No info returned"));	
			}
			
		} 
		catch (Exception e)
		{
			logger.error("Exception Message: " + e.getMessage(), e);
			rp.setResponse(new ResponseObject("Person Info", false, "Error returning person info: " + e.getMessage()
					+ " - " + e.getStackTrace().toString()));
		}
		return rp;
	}
	
	
	/**
	 * registerWPUser (REST)
	 * Takes the wordpress user and auth objects and creates
	 * a new user from them.  Adds user to own personal community
	 * and to system community.
	 * 
	 * @param wpuser
	 * @param wpauth
	 * @return
	 */
	public ResponsePojo registerWPUser(String cookieLookup, String wpuser, String wpauth, String wpsetup) 
	{		
		ResponsePojo rp = new ResponsePojo();
		
		//Step 0 Read wordpress objects
		WordPressUserPojo wpu = null;
		WordPressAuthPojo wpa = null;
		if (null != wpsetup) {
			WordPressSetupPojo setup = WordPressSetupPojo.fromApi(wpsetup, WordPressSetupPojo.class);
			wpu = setup.getUser();
			wpa = setup.getAuth();
			if ((null == wpu) || (null == wpa)) {
				rp.setResponse(new ResponseObject("WP Register User",false,"Need to specify both user and auth objects"));
				return rp;
			}
		}//TESTED
		else {
			wpu = WordPressUserPojo.fromApi(wpuser,WordPressUserPojo.class); 
			wpa = WordPressAuthPojo.fromApi(wpauth,WordPressAuthPojo.class);
		}
		
		//Step 1 Create the person object
		//NOTE we use to store subscription info (i.e. in the peoplepojo)
		//but it was never used anywhere (validating subscription?)
		//its in the WPUserPojo that comes across
		ObjectId profileId = new ObjectId();
		PersonPojo pp = new PersonPojo();
		pp.set_id(profileId);
		pp.setAccountStatus("active");
		if ((null == wpu.getEmail()) || (0 == wpu.getEmail().size())) {
			rp.setResponse(new ResponseObject("WP Register User",false,"Need to specify email"));
			return rp;
		}//TESTED (2c)
		pp.setEmail(wpu.getEmail().get(0));
		pp.setFirstName(wpu.getFirstname()); // (optional but one of this + last name must be set)
		pp.setLastName(wpu.getLastname()); // (optional but one of this + first name must be set)
		if ((null == wpu.getFirstname()) || wpu.getFirstname().isEmpty()){
			if (null == wpu.getLastname()) {
				rp.setResponse(new ResponseObject("WP Register User",false,"Need to specify one of firstname,lastname"));
				return rp;
			}
			pp.setDisplayName(wpu.getLastname());
		}//TESTED (2d)
		else if ((null == wpu.getLastname()) || wpu.getLastname().isEmpty()) {
			pp.setDisplayName(wpu.getFirstname());			
		}
		else {
			pp.setDisplayName(wpu.getFirstname() + " " + wpu.getLastname());						
		}
		
		// Check if user is already present (+set "primary keys"):
		
		if (null == wpu.getWPUserID()) { // WPUserId is optional, uses email if not present
			wpu.setWPUserID(pp.getEmail());
		}
		else { // Check WPU (+email later)
			PersonPojo personQuery = new PersonPojo();
			personQuery.setWPUserID(wpu.getWPUserID()); // (can be null, that's fine)						
			DBObject dboperson = DbManager.getSocial().getPerson().findOne(personQuery.toDb());
			if (null != dboperson) {
				rp.setResponse(new ResponseObject("WP Register User",false,"User already exists, both WPUserId and first email must be unique"));
				return rp;				
			}//TESTED (2e)
		}		
		pp.setWPUserID(wpu.getWPUserID());
		
		PersonPojo personQuery = new PersonPojo();
		personQuery.setEmail(pp.getEmail());
		DBObject dboperson = DbManager.getSocial().getPerson().findOne(personQuery.toDb());
		if (null != dboperson) {
			rp.setResponse(new ResponseObject("WP Register User",false,"User already exists, both WPUserId and first email must be unique"));
			return rp;				
		}//TESTED (2f)
		
		//(The rest of this code has not significantly changed)
		
		// Optional fields:
		pp.setPhone(wpu.getPhone());
		pp.setSubscriptionEndDate(wpu.getSubscriptionEndDate());
		pp.setSubscriptionID(wpu.getSubscriptionID());
		pp.setSubscriptionStartDate(wpu.getSubscriptionStartDate());
		pp.setSubscriptionTypeID(wpu.getSubscriptionTypeID());
		
		//Step 3 add communities to my list (self and system)
		List<PersonCommunityPojo> communities = new ArrayList<PersonCommunityPojo>();
		pp.setCommunities(communities);
		
		//these fields may need set one day
		pp.setAvatar(null);
		pp.setBiography(null);		
		pp.setContacts(null);
		pp.setLanguages(null);
		pp.setLinks(null);
		pp.setLocation(null);
		pp.setOrganization(null);
		pp.setTags(null);
		pp.setTitle(null);
		//end set of fields i didn't use
				
		//Step 4 Create the new auth object so user can login
		AuthenticationPojo ap = new AuthenticationPojo();
		ap.setId(profileId);
		ap.setProfileId(profileId);
		ap.setUsername(pp.getEmail());
		ap.setAccountStatus(AccountStatus.ACTIVE);
		if (null == wpa.getPassword()) { // Obligatory
			rp.setResponse(new ResponseObject("WP Register User",false,"Need to specify password"));
			return rp;
		}
		try
		{
			if (44 != wpa.getPassword().length()) { // hash if in the clear
				wpa.setPassword(PasswordEncryption.encrypt(wpa.getPassword()));
			}
			ap.setPassword(wpa.getPassword());
			if (null == wpa.getAccountType()) { // (optional, defaults to "user"
				wpa.setAccountType("user");
			}
			ap.setAccountType(wpa.getAccountType());
				// to create an account you must be admin, so this is fine....
			
			ap.setWPUserID(wpa.getWPUserID());		
				
			DateFormat df = new SimpleDateFormat("MMM dd, yyyy kk:mm:ss aa");
			//Handle copying dates from wordpress objects
			// (These are all optional, just use now if not specified)
			if (null == wpu.getCreated()) {
				pp.setCreated(new Date());				
			}
			else {
				pp.setCreated(df.parse(wpu.getCreated()));				
			}
			if (null == wpu.getModified()) {
				pp.setModified(new Date());				
			}
			else {
				pp.setModified(df.parse(wpu.getModified()));				
			}
			if (null == wpa.getCreated()) {
				ap.setCreated(new Date());				
			}
			else {
				ap.setCreated(df.parse(wpa.getCreated()));				
			}
			if (null == wpa.getModified()) {
				ap.setModified(new Date());				
			}
			else {
				ap.setModified(df.parse(wpa.getModified()));				
			}
			ap.setApiKey(wpa.getApiKey());
			
			//Step 5 Save all of these objects to the DB
			DbManager.getSocial().getPerson().insert(pp.toDb());
			DbManager.getSocial().getAuthentication().insert(ap.toDb());
			
			CommunityHandler cc = new CommunityHandler();
			cc.createSelfCommunity(pp); //add user to own community
			
			//try to get system
			BasicDBObject commQueryDbo = new BasicDBObject("isSystemCommunity", true);
				// (annoyingly can't use community pojo for queries because it has default fields)
			DBObject dbo = DbManager.getSocial().getCommunity().findOne(commQueryDbo);
			if (null != dbo) {
				CommunityPojo systemGroup = CommunityPojo.fromDb(dbo, CommunityPojo.class);
				
				//Add user to system community also
				cc.addCommunityMember(cookieLookup, systemGroup.getId().toString(), "Infinit.e System", pp.get_id().toString(), 
						pp.getEmail(), pp.getDisplayName(), "member", "active", true);
			}								
			rp.setResponse(new ResponseObject("WP Register User",true,"User Registered Successfully"));
			rp.setData(ap, new AuthenticationPojoApiMap());
			
			// OK we're all good, finally for API key users create a persistent cookie:
			if (null != ap.getApiKey()) {
				// (if we're here then we're already admin so can always do this - unlike the update)
				CookiePojo cp = new CookiePojo();
				cp.set_id(profileId);
				cp.setCookieId(cp.get_id());
				cp.setApiKey(wpa.getApiKey());
				cp.setStartDate(ap.getCreated());
				cp.setProfileId(profileId);
				DbManager.getSocial().getCookies().save(cp.toDb());
			}//TOTEST
		}
		catch (Exception ex )
		{
			logger.error("Exception Message: " + ex.getMessage(), ex);
			rp.setResponse(new ResponseObject("WP Register User",false,"error while saving wp objects"));
		}
		return rp;
	}//TESTED
	
	/**
	 * updateWPUser (REST)
	 * Takes the wordpress user and auth objects and updates
	 * the associated db entries with the new info.
	 * 
	 * @param wpuser
	 * @param wpauth
	 * @return
	 */
	public ResponsePojo updateWPUser(String wpuser, String wpauth, String wpsetup, String personIdStr) 
	{
		ResponsePojo rp = new ResponsePojo();
				
		boolean bNeedToUpdateCommunities = false;
		
		WordPressUserPojo wpu = null;
		WordPressAuthPojo wpa = null;
		if (null != wpsetup) {
			WordPressSetupPojo setup = WordPressSetupPojo.fromApi(wpsetup, WordPressSetupPojo.class);
			wpu = setup.getUser();
			wpa = setup.getAuth();
			if ((null == wpu) || (null == wpa)) {
				rp.setResponse(new ResponseObject("WP Update User",false,"Need to specify both user and auth objects"));
				return rp;
			}
		}
		else {
			wpu = WordPressUserPojo.fromApi(wpuser,WordPressUserPojo.class); 
			wpa = WordPressAuthPojo.fromApi(wpauth,WordPressAuthPojo.class);
		}
		
		//Save both these objects to the DB
		try
		{					
			PersonPojo personQuery = new PersonPojo();
			if (null != personIdStr) {
				personQuery.set_id(new ObjectId(personIdStr));				
			}
			else {
				if (null == wpu.getWPUserID()) {
					if ((null == wpu.getEmail()) || wpu.getEmail().isEmpty()) {
						rp.setResponse(new ResponseObject("WP Update User",false,"Need to specify WPUserID (or email address if not integrated via WordPress)"));
						return rp;
					}
					else {	// If authentication username is set, use that because it means that we're trying to change 
							// the email (and we're an admin user) 
						
						if (null != wpa.getUsername()) { // I may be changing someone's name
							personQuery.setEmail(wpa.getUsername());
						}
						else { // I'm not changing anybody's name
							personQuery.setEmail(wpu.getEmail().get(0));
						}
					}
				}
				else {
					personQuery.setWPUserID(wpu.getWPUserID());
				}
			}			
			BasicDBObject dboperson = (BasicDBObject) DbManager.getSocial().getPerson().findOne(personQuery.toDb());
			if (null == dboperson) {
				rp.setResponse(new ResponseObject("WP Update User",false,"Can't find user specified by WPUserID"));
				return rp;				
			}
			
			PersonPojo pp = PersonPojo.fromDb(dboperson,PersonPojo.class);
			
			if ((null != wpu.getEmail()) && !wpu.getEmail().isEmpty()) {
				if (!pp.getEmail().equalsIgnoreCase(wpu.getEmail().get(0))) { // Email has changed...
					pp.setEmail(wpu.getEmail().get(0));
				
					// Check this is allowed (ie haven't taken a username already in use):
					personQuery = new PersonPojo();
					personQuery.setEmail(pp.getEmail());
					dboperson = (BasicDBObject) DbManager.getSocial().getPerson().findOne(personQuery.toDb());
					if (null != dboperson) {
						rp.setResponse(new ResponseObject("WP Update User",false,"This primary email address is not unique"));
						return rp;				
					}//TOTEST
					
					bNeedToUpdateCommunities = true;
				}
			}
			if (null != wpu.getFirstname()) {
				if ((null == pp.getFirstName()) || !pp.getFirstName().equals(wpu.getFirstname())) {
					pp.setFirstName(wpu.getFirstname());
					bNeedToUpdateCommunities = true;
				}
			}
			if (null != wpu.getLastname()) {
				if ((null == pp.getLastName()) || !pp.getLastName().equals(wpu.getLastname())) {
					pp.setLastName(wpu.getLastname());
					bNeedToUpdateCommunities = true;
				}
			}
			// Update display name
			StringBuffer displayName = new StringBuffer();
			if ((null != pp.getFirstName()) && !pp.getFirstName().isEmpty()) {
				displayName.append(pp.getFirstName());
			}
			if ((null != pp.getLastName()) && !pp.getLastName().isEmpty()) {
				if (displayName.length() > 0) {
					displayName.append(' ');
				}
				displayName.append(pp.getLastName());
			}//TOTESTx2
			pp.setDisplayName(displayName.toString());
			
			if (null != wpu.getPhone()) {
				pp.setPhone(wpu.getPhone());
			}
			if (null != wpu.getSubscriptionEndDate()) {
				pp.setSubscriptionEndDate(wpu.getSubscriptionEndDate());
			}
			if (null != wpu.getSubscriptionID()) {
				pp.setSubscriptionID(wpu.getSubscriptionID());
			}
			if (null != wpu.getSubscriptionStartDate()) {
				pp.setSubscriptionStartDate(wpu.getSubscriptionStartDate());
			}
			if (null != wpu.getSubscriptionTypeID()) {
				pp.setSubscriptionTypeID(wpu.getSubscriptionTypeID());
			}
			// (can't change WPUserId obv)
			
			AuthenticationPojo authQuery = new AuthenticationPojo();
			if (null != pp.get_id()) {
				authQuery.setProfileId(pp.get_id());
			}
			else {
				rp.setResponse(new ResponseObject("WP Update User",false,"Internal authentication error 1"));
				return rp;				
			}
			DBObject dboauth = DbManager.getSocial().getAuthentication().findOne(authQuery.toDb());
			if (null == dboauth) {
				rp.setResponse(new ResponseObject("WP Update User",false,"Internal authentication error 2"));
				return rp;				
			}			
			AuthenticationPojo ap = AuthenticationPojo.fromDb(dboauth, AuthenticationPojo.class);
			
			if ((null != wpu.getEmail()) && !wpu.getEmail().isEmpty()) {
				ap.setUsername(wpu.getEmail().get(0)); // (ap.username == email address, make life easy when resetting password)
			}
			if (null != wpa.getPassword()) {
				if (44 != wpa.getPassword().length()) { // hash if in the clear
					wpa.setPassword(PasswordEncryption.encrypt(wpa.getPassword()));
				}
				ap.setPassword(wpa.getPassword());
			}
			if (null != wpa.getAccountType()) {
				if (null == personIdStr) { // (this means you're admin and hence can upgrade users to admins)
					ap.setAccountType(wpa.getAccountType());
				}
			}
			// (can't change WPUserId obv)
			
			//Handle dates (just update modified times)
			pp.setModified(new Date());
			ap.setModified(new Date());
			
			if ((null != wpa.getApiKey()) && (0 == wpa.getApiKey().length()) && (null != ap.getApiKey()))			
			{
				// Delete existing API key
				// (We'll allow a user to update their own API key - just not create it, see below)
				CookiePojo removeMe = new CookiePojo();
				removeMe.setApiKey(ap.getApiKey());
				ap.setApiKey(null);				
				DbManager.getSocial().getCookies().remove(removeMe.toDb());
			}
			else if (null != wpa.getApiKey()) {
				// Change or create API key
				// Only admins can do this:
				if (null != personIdStr) { // (this is != null iff user isn't admin)
					// Check security settings
					PropertiesManager pm = new PropertiesManager(); 
					if (pm.getHarvestSecurity()) {
						rp.setResponse(new ResponseObject("WP Update User",false,"You must be admin in secure mode to set an API key"));
						return rp;
					}
				}//TESTED (admin, admin-enabled, non-admin - harvest.secure on and off)
				
				ap.setApiKey(wpa.getApiKey());
				CookiePojo cp = new CookiePojo();
				cp.set_id(ap.getProfileId());
				cp.setCookieId(cp.get_id());
				cp.setApiKey(wpa.getApiKey());
				cp.setStartDate(ap.getCreated());
				cp.setProfileId(ap.getProfileId());
				DbManager.getSocial().getCookies().save(cp.toDb());								
			}//TESTED
			//else if api key is null then leave alone, assume hasn't changed
			
			//update old entries
			DbManager.getSocial().getPerson().update(new BasicDBObject("_id", pp.get_id()), pp.toDb());
			DbManager.getSocial().getAuthentication().update(authQuery.toDb(), ap.toDb());			
			rp.setResponse(new ResponseObject("WP Update User",true,"User Updated Successfully"));
			rp.setData(ap, new AuthenticationPojoApiMap());
			
			//update communities if necessary
			if (bNeedToUpdateCommunities) 
			{
				//set community members name and email, if they match on id
				BasicDBObject query = new BasicDBObject("members._id", pp.get_id());
				BasicDBObject update = new BasicDBObject("members.$.email", pp.getEmail());
				update.put("members.$.displayName", pp.getDisplayName());
				DbManager.getSocial().getCommunity().update(query, new BasicDBObject("$set", update), false, true);
					// (don't upsert, many times)
				
				//INF-1314 if the ownerid == pp_id, set new username
				BasicDBObject query1 = new BasicDBObject("ownerId", pp.get_id());
				BasicDBObject update1 = new BasicDBObject("ownerDisplayName", pp.getDisplayName());
				DbManager.getSocial().getCommunity().update(query1, new BasicDBObject("$set", update1), false, true);
			}//TOTEST
			
			// Just recreate personal community if necessary (means if something goes wrong can always just update user...)
			if (null != pp.get_id()) {
				GenericProcessingController.createCommunityDocIndex(pp.get_id().toString(), null, true, false, false);
			}
			//TESTED
			
			rp.setResponse(new ResponseObject("WP Update User",true,"User Updated Successfully"));
		}
		catch (Exception ex )
		{
			logger.error("Exception Message: " + ex.getMessage(), ex);
			rp.setResponse(new ResponseObject("WP Update User",false,"error while updating wp objects"));
		}
		return rp;
	}//TOTEST
	
	/**
	 * deleteWPUser (REST)
	 * Deletes the user, including all his communities
	 * 
	 * @param userIdStrOrEmailOrWP - WPUserId, or email, or 
	 * @return
	 */
	public ResponsePojo deleteWPUser(String userIdStrOrEmailOrWP) 
	{
		ResponsePojo rp = new ResponsePojo();
		
		// Work out which field to delete on
		BasicDBObject personDbo = null;
		try {
			PersonPojo personQuery = new PersonPojo(); 
			personQuery.set_id(new ObjectId(userIdStrOrEmailOrWP));
			personDbo = (BasicDBObject) DbManager.getSocial().getPerson().findOne(personQuery.toDb());
		}//TESTED
		catch (Exception e){ // Not an object id
			if (userIdStrOrEmailOrWP.indexOf('@') < 0) { // not an email
				PersonPojo personQuery = new PersonPojo(); 
				personQuery.setWPUserID(userIdStrOrEmailOrWP);
				personDbo = (BasicDBObject) DbManager.getSocial().getPerson().findOne(personQuery.toDb());				
			}
			else { // can be either an email address or a WPU
				BasicDBObject complexQuery_term1 = new BasicDBObject("WPUserID", userIdStrOrEmailOrWP);
				BasicDBObject complexQuery_term2 = new BasicDBObject("email", userIdStrOrEmailOrWP);
				BasicDBObject complexQuery = new BasicDBObject("$or", Arrays.asList(complexQuery_term1, complexQuery_term2));
				personDbo = (BasicDBObject) DbManager.getSocial().getPerson().findOne(complexQuery);				
			}
		}//TESTED
		if (null == personDbo) {
			rp.setResponse(new ResponseObject("WP Update User",false,"Can't find user to delete via id, WPUserId, or (primary) email"));
			return rp;							
		}//TESTED		
		PersonPojo pp = PersonPojo.fromDb(personDbo, PersonPojo.class);
		
		// Delete personal communities
		try {
			DbManager.getSocial().getCommunity().remove(new BasicDBObject("_id", pp.get_id()));
				// (can't use community pojo because it contains default params unfortunately)
		}
		catch (Exception e) { 
			// We're past the point of no return so just carry on...
		}
		
		// Remove from index:
		GenericProcessingController.deleteCommunityDocIndex(pp.get_id().toString(), null, true);
		//TESTED
		
		// Remove from all (other) communities 
		try {
			BasicDBObject query = new BasicDBObject("members._id", pp.get_id()); 
			CommunityMemberPojo cmp = new CommunityMemberPojo();
			cmp.set_id(pp.get_id());
			BasicDBObject actions = new BasicDBObject();
			actions.put("$pull", new BasicDBObject("members", new BasicDBObject("_id", pp.get_id())));
				// ie for communities for which he's a member...remove...any elements of the list members...with his _id
			actions.put("$inc", new BasicDBObject("numberOfMembers", -1));
				// ie decrement number of members
			
			DbManager.getSocial().getCommunity().update(query, actions, false, true); 
				// (don't upsert, many times)			
		}
		catch (Exception e) { 
			// We're past the point of no return so just carry on...
		}//TESTED
		
		// Remove from respective DBs
		
		PersonPojo personQuery = new PersonPojo();
		personQuery.set_id(pp.get_id());
		DbManager.getSocial().getPerson().remove(personQuery.toDb());
		//TESTED
		
		AuthenticationPojo authQuery = new AuthenticationPojo();
		if (null != authQuery.getWPUserID()) { // (Some older records have this and of course it deletes the entire auth DB...) 
			authQuery.setWPUserID(pp.getWPUserID());
			DbManager.getSocial().getAuthentication().remove(authQuery.toDb());
		}
		else if (null != pp.getEmail()) {
			authQuery.setUsername(pp.getEmail());
			DbManager.getSocial().getAuthentication().remove(authQuery.toDb());
		}
		// (else we'll just have to leave that object in there unfortunately)
		//TESTED
		
		// Delete any cookies the user might have
		CookiePojo cookieQuery = new CookiePojo();
		if (null != pp.get_id()) {
			cookieQuery.setProfileId(pp.get_id());
			DbManager.getSocial().getCookies().remove(cookieQuery.toDb());
		}
		//TOTEST
		
		rp.setResponse(new ResponseObject("WP Delete User",true,"User Deleted Successfully"));
		return rp;
		
	}//TOTEST
	
	//////////////////////////////////////////////////////////////////////////
	//////////////////////// Helper Functions ////////////////////////////////
	//////////////////////////////////////////////////////////////////////////

	/**
	 * addCommunity (UTILITY FOR COMMUNITY HANDLER)
	 * @param personId
	 * @param communityId
	 * @param communityName
	 * @return ResponsePojo
	 */
	public ResponsePojo addCommunity(String personId, String communityId, String communityName)
	{
		ResponsePojo rp = new ResponsePojo();
		try
		{
			// Find person record to update
			PersonPojo personQuery = new PersonPojo();
			personQuery.set_id(new ObjectId(personId));
			DBObject dbo = DbManager.getSocial().getPerson().findOne(personQuery.toDb());
			
			if (dbo != null) 
			{
				// Get GsonBuilder object with MongoDb de/serializers registered
				PersonPojo person = PersonPojo.fromDb(dbo, PersonPojo.class);

				// Create a new PersonCommunityPojo object
				PersonCommunityPojo community = new PersonCommunityPojo();
				community.set_id(new ObjectId(communityId));
				community.setName(communityName);
				
				// Check to see if person is already a member of the community to be added
				List<PersonCommunityPojo> communities = person.getCommunities();
				Boolean alreadyMember = false;
				for (PersonCommunityPojo c : communities)
				{
					String idToTest = c.get_id().toStringMongod();					
					if (idToTest.equals(communityId))
					{
						alreadyMember = true;
						break;
					}
				}
				
				// Add the community to the list if it does not already exist
				if (!alreadyMember)
				{
					//TODO (INF-1214): (not thread safe)					
					communities.add(community);	
					person.setModified(new Date());					
					DbManager.getSocial().getPerson().update(personQuery.toDb(), person.toDb());
					rp.setData(person, new PersonPojoApiMap());
					rp.setResponse(new ResponseObject("Add community status",true,"Community added successfully."));	
				}					
				else
				{
					rp.setResponse(new ResponseObject("Add community status",true,"Community already exists."));	
				}
			}
			else
			{
				rp.setResponse(new ResponseObject("Add community status", false, "Person not found."));
			}
		}
		catch (Exception e)
		{			
			// If an exception occurs log the error
			logger.error("Exception Message: " + e.getMessage(), e);
			rp.setResponse(new ResponseObject("Add community status", false, 
					"Error adding community to person " + e.getMessage()));
		}	
		return rp;
	}
	
	
	/**
	 * removeCommunity (UTILITY FOR COMMUNITY HANDLER)
	 * @param personId
	 * @param communityId
	 * @return
	 */
	public ResponsePojo removeCommunity(String personId, String communityId)
	{
		ResponsePojo rp = new ResponsePojo();	
		try
		{
			// Find person record to update
			PersonPojo personQuery = new PersonPojo();
			personQuery.set_id(new ObjectId(personId));
			DBObject dbo = DbManager.getSocial().getPerson().findOne(personQuery.toDb());
			
			if (dbo != null) 
			{
				PersonPojo person = PersonPojo.fromDb(dbo, PersonPojo.class);
				
				// Check to see if person is already a member of the community to be added
				List<PersonCommunityPojo> communities = person.getCommunities();
				Boolean alreadyMember = false;
				int communityIndex = 0;
				for (PersonCommunityPojo c : communities)
				{
					String idToTest = c.get_id().toStringMongod();					
					if (idToTest.equals(communityId))
					{
						alreadyMember = true;
						break;
					}
					communityIndex++;
				}				
				
				// Remove the community from the list
				if (alreadyMember)
				{
					//TODO (INF-1214): (not thread safe)					
					communities.remove(communityIndex);	
					person.setModified(new Date());					
					DbManager.getSocial().getPerson().update(personQuery.toDb(), person.toDb());
					rp.setData(person, new PersonPojoApiMap());
					rp.setResponse(new ResponseObject("Remove community status",true,"Community removed successfully."));	
				}					
				else
				{
					rp.setResponse(new ResponseObject("Remove community status",true,"Person is not a member of the specified community."));	
				}
			}
			else
			{
				rp.setResponse(new ResponseObject("Remove community status", false, "Person not found."));
			}
		}
		catch (Exception e)
		{			
			// If an exception occurs log the error
			logger.error("Exception Message: " + e.getMessage(), e);
			rp.setResponse(new ResponseObject("Remove community status", false, 
					"Error removing community record "));
		}	
		return rp;
	}
	
}
