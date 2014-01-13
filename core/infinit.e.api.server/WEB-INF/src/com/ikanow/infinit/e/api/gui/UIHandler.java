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
package com.ikanow.infinit.e.api.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.bson.types.ObjectId;

import com.ikanow.infinit.e.api.utils.SocialUtils;
import com.ikanow.infinit.e.api.utils.RESTTools;
import com.ikanow.infinit.e.data_model.api.ApiManager;
import com.ikanow.infinit.e.data_model.api.ResponsePojo;
import com.ikanow.infinit.e.data_model.api.ResponsePojo.ResponseObject;
import com.ikanow.infinit.e.data_model.api.gui.UIModulePojoApiMap;
import com.ikanow.infinit.e.data_model.api.gui.UISetupPojoApiMap;
import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.social.authentication.AuthenticationPojo;
import com.ikanow.infinit.e.data_model.store.social.gui.UIModulePojo;
import com.ikanow.infinit.e.data_model.store.social.gui.UISetupPojo;
import com.ikanow.infinit.e.data_model.store.social.gui.FavoriteUIModulePojo;
import com.ikanow.infinit.e.data_model.store.social.gui.WidgetPojo;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

public class UIHandler 
{
	public ResponsePojo getLastSetup(String profileID)
	{
		ResponsePojo rp = new ResponsePojo();
		try
		{
			UISetupPojo querySetup = new UISetupPojo();
			querySetup.setProfileID(new ObjectId(profileID));
			DBObject dbo = DbManager.getSocial().getUISetup().findOne(querySetup.toDb());	
			if ( dbo != null )
			{
				UISetupPojo up = UISetupPojo.fromDb(dbo, UISetupPojo.class);
				rp.setResponse(new ResponseObject("UISetup",true,"ui returned successfully"));
				rp.setData(up, new UISetupPojoApiMap());
			}
			else
			{
				rp.setResponse(new ResponseObject("UISetup",false,"user had no setup"));
			}
		}
		catch (Exception ex)
		{
			rp.setResponse(new ResponseObject("UISetup",false,"error retrieving ui setup"));
		}
		return rp;
	}
	
	public ResponsePojo updateLastSetup(String profileID, String communityIdStrList, String query, String openModules)
	{
		ResponsePojo rp = new ResponsePojo();
		try
		{
			UISetupPojo up = new UISetupPojo();			
			List<WidgetPojo> modulesList = new ArrayList<WidgetPojo>();
			if ( !openModules.equals("null"))
			{
				String[] modules = openModules.split("]");
				
				for ( String s : modules )
				{
					String[] name = s.split(",");
					WidgetPojo wp = new WidgetPojo();
					wp.setWidgetTitle(name[0]);
					wp.setWidgetUrl(name[1]);
					wp.setWidgetDisplay(name[2]);
					wp.setWidgetImageUrl(name[3]);
					wp.setWidgetX(name[4]);
					wp.setWidgetY(name[5]);
					wp.setWidgetWidth(name[6]);
					wp.setWidgetHeight(name[7]);
					if ( !name[8].equals("null") )
						wp.setWidgetOptions(s.substring(s.indexOf(name[6] + "," + name[7]) + name[6].length() + name[7].length() + 2));
						
					modulesList.add(wp);				
				}
			}
			UISetupPojo querySetup = new UISetupPojo();
			querySetup.setProfileID(new ObjectId(profileID));
			DBObject dbo = DbManager.getSocial().getUISetup().findOne(querySetup.toDb());
			if ( dbo == null )
			{
				//add new entry
				up.setProfileID(querySetup.getProfileID());
				up.addWidgets(modulesList);
				up.setQueryString(query);
				up.setCommunityIds(communityIdStrList);
				DbManager.getSocial().getUISetup().insert(up.toDb());
			}
			else
			{
				//update old entry				
				up = UISetupPojo.fromDb(dbo, UISetupPojo.class);
				up.addWidgets(modulesList);
				up.setQueryString(query);
				up.setCommunityIds(communityIdStrList);
				DbManager.getSocial().getUISetup().update(querySetup.toDb(), up.toDb());
			}
			rp.setResponse(new ResponseObject("Update UISetup",true,"modules updated successfully"));						
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			rp.setResponse(new ResponseObject("Update UISetup",false,"error updating modules"));
		}
		return rp;
	}

	public ResponsePojo getModules(String userIdStr) 
	{
		ResponsePojo rp = new ResponsePojo();
		try
		{
			boolean bAdmin = RESTTools.adminLookup(userIdStr);

			// Query too complex to be represented by Pojos
			BasicDBObject query = new BasicDBObject("approved", true);
			HashSet<ObjectId> memberOf = SocialUtils.getUserCommunities(userIdStr);
			if ((null != memberOf) && !bAdmin) {
				BasicDBObject query_communities = new BasicDBObject("communityIds", new BasicDBObject("$in", memberOf));
				BasicDBObject query_nosec =  new BasicDBObject("communityIds", new BasicDBObject("$exists", false));
				query.put("$or", Arrays.asList(query_communities, query_nosec));
			}//TESTED
			
			// Now add community code, requires
			DBCursor dbc = DbManager.getSocial().getUIModules().find(query);
			List<UIModulePojo> mps = UIModulePojo.listFromDb(dbc, UIModulePojo.listType());
			rp.setData(mps, new UIModulePojoApiMap(bAdmin?null:memberOf));
			rp.setResponse(new ResponseObject("Get Modules",true,"modules returned successfully"));
		}
		catch (Exception ex)
		{
			rp.setResponse(new ResponseObject("Get Modules",false,"error returning modules"));
		}
		return rp;
	}//TESTED

	public ResponsePojo getUserModules(String userIdStr) 
	{
		ResponsePojo rp = new ResponsePojo();
		try
		{
			boolean bAdmin = RESTTools.adminLookup(userIdStr);
			
			FavoriteUIModulePojo moduleQuery = new FavoriteUIModulePojo();
			moduleQuery.setProfileId(new ObjectId(userIdStr));
			DBObject dbo = DbManager.getSocial().getUIFavoriteModules().findOne(moduleQuery.toDb());			
			HashSet<ObjectId> memberOf = SocialUtils.getUserCommunities(userIdStr);
			List<UIModulePojo> mods = getFullModule(FavoriteUIModulePojo.fromDb(dbo,FavoriteUIModulePojo.class).getQuickModules(), memberOf, bAdmin);
			rp.setData(mods, new UIModulePojoApiMap(bAdmin?null:memberOf));
			rp.setResponse(new ResponseObject("Get User Modules",true,"users modules returned successfully"));
		}
		catch (Exception ex)
		{
			rp.setResponse(new ResponseObject("Get User Modules",false,"error returning user modules"));
		}
		return rp;
	}

	private List<UIModulePojo> getFullModule(List<ObjectId> moduleids, Set<ObjectId> memberOf, boolean bAdmin) 
	{
		List<UIModulePojo> mods = new ArrayList<UIModulePojo>();		
		try
		{
			for ( int i = 0; i < moduleids.size(); i++ )	//loop through all the ids sent in, and grab the module		
			{
				UIModulePojo moduleQuery = new UIModulePojo();
				moduleQuery.set_id(moduleids.get(i));
				DBObject dbo = DbManager.getSocial().getUIModules().findOne(moduleQuery.toDb());
				if ( dbo != null) {
					UIModulePojo module = UIModulePojo.fromDb(dbo,UIModulePojo.class);
					if (null != module.getCommunityIds()) {
						if (bAdmin) {
							mods.add(module);
						}
						else if (null != memberOf) {
							for (ObjectId communityId: module.getCommunityIds()) {
								if (memberOf.contains(communityId)) {
									mods.add(module);
									break;
								}
							}
						}
					}//(end check user has access to this module)
					else { // (no security on this module)
						mods.add(module);
					}
				}//TESTED
			}
		}
		catch (Exception ex)
		{			
		}
		return mods;
	}//TESTED

	public ResponsePojo searchModules(String userIdStr, String updateItem) 
	{
		ResponsePojo rp = new ResponsePojo();
		List<UIModulePojo> mods = null;		
		try
		{
			boolean bAdmin = RESTTools.adminLookup(userIdStr);
			
			BasicDBObject query = new BasicDBObject();
			if ( !updateItem.equals("*") ) {				
				Pattern p = Pattern.compile(updateItem, Pattern.CASE_INSENSITIVE);
				query.put("searchterms", p);
			}			
			HashSet<ObjectId> memberOf = SocialUtils.getUserCommunities(userIdStr);
			if ((null != memberOf) && !bAdmin)
			{
				BasicDBObject query_communities = new BasicDBObject("communityIds", new BasicDBObject("$in", memberOf));
				BasicDBObject query_nosec =  new BasicDBObject("communityIds", new BasicDBObject("$exists", false));
				query.put("$or", Arrays.asList(query_communities, query_nosec));
			}//TESTED	
			DBCursor dbc = DbManager.getSocial().getUIModules().find(query);
			mods = UIModulePojo.listFromDb(dbc, UIModulePojo.listType());
			rp.setData(mods, new UIModulePojoApiMap(bAdmin?null:memberOf));
			rp.setResponse(new ResponseObject("Search Modules",true,updateItem));
		}
		catch (Exception ex)
		{			
			rp.setResponse(new ResponseObject("Get User Modules",false,"searched modules unsuccessfully for " + updateItem));
		}
		return rp;
	}//TESTED

	public ResponsePojo saveModules(String modules, String profileId) 
	{
		ResponsePojo rp = new ResponsePojo();
		String[] modids;
		if ( modules.equals("null"))
		{
			modids = new String[0];
		}
		else
		{
			modids = modules.split("\\s*,\\s*");
		}
		 	
		try
		{
			//try to get entry for user (if they have one)
			FavoriteUIModulePojo moduleQuery = new FavoriteUIModulePojo();
			moduleQuery.setProfileId(new ObjectId(profileId));
			DBObject dbo = DbManager.getSocial().getUIFavoriteModules().findOne(moduleQuery.toDb());
			if ( dbo != null )
			{
				//found old entry, update it with new modules
				FavoriteUIModulePojo ump = FavoriteUIModulePojo.fromDb(dbo, FavoriteUIModulePojo.class);
				ump.setQuickModules(modids);
				DbManager.getSocial().getUIFavoriteModules().update(moduleQuery.toDb(), ump.toDb());
			}
			else
			{
				//no entry exists for this user, create a new one
				FavoriteUIModulePojo ump = new FavoriteUIModulePojo();
				ump.set_id(new ObjectId());
				ump.setProfileId(moduleQuery.getProfileId());
				ump.setQuickModules(modids);
				ump.setTimestamp(new Date());
				DbManager.getSocial().getUIFavoriteModules().insert(ump.toDb());
			}	
			rp.setResponse(new ResponseObject("Save Modules",true,"modules saved successfully"));
		}
		catch (Exception ex)
		{			
			rp.setResponse(new ResponseObject("Save Modules",false,"modules saved unsuccessfully"));
		}
		return rp;
	}
	public ResponsePojo installModule(String moduleJson, String userIdStr) 
	{
		ResponsePojo rp = new ResponsePojo();
		try
		{
			UIModulePojo module = ApiManager.mapFromApi(moduleJson, UIModulePojo.class, new UIModulePojoApiMap(null));
				// (allow all communities submitted by the user)
			module.setSwf(""); // (no longer needed)
			module.setApproved(true); // (no mechanism for admin authentication / or of communities)
			
			// Check it's all valid (id can be null
			if ((null == module.getDescription())||(null == module.getImageurl())
					||(null == module.getUrl())||(null == module.getTitle())||(null == module.getVersion()))
			{
				rp.setResponse(new ResponseObject("Install Module",false,"Missing one of description, image url, url, title, or version"));
				return rp;
			}//TESTED
			else {
				boolean bAdmin = RESTTools.adminLookup(userIdStr);

				if (!bAdmin) { // need to be owner or moderator to add to a community
					if (null == module.getCommunityIds() || module.getCommunityIds().isEmpty()) {
						module.addToCommunityIds(new ObjectId(userIdStr)); // (ie adds to personal community)
					}
					else {
						HashSet<ObjectId> newSet = new HashSet<ObjectId>();
						for (ObjectId communityId: module.getCommunityIds()) {
							if (!SocialUtils.isOwnerOrModerator(communityId.toString(), userIdStr)) {
								rp.setResponse(new ResponseObject("Install Module",false,"Don't have permission to update one or more communities: " + communityId.toString()));
								return rp;
							}
							newSet.add(communityId);
						}
						if (newSet.isEmpty()) {
							newSet.add(new ObjectId(userIdStr)); // (ie adds to personal community)
						}
						module.setCommunityIds(newSet);
					}
				}//TESTED
				
				// Get username from profile id:
				AuthenticationPojo userQuery = new AuthenticationPojo();
				userQuery.setProfileId(new ObjectId(userIdStr));
				BasicDBObject userDbo = (BasicDBObject) DbManager.getSocial().getAuthentication().findOne(userQuery.toDb());
				String userName = userIdStr;
				if (null != userDbo) {
					AuthenticationPojo user =  AuthenticationPojo.fromDb(userDbo, AuthenticationPojo.class);
					userName = user.getUsername();
				}//TESTED
				if (!bAdmin || (null == module.getAuthor())) {
					module.setAuthor(userName);
						// (if module name set and I'm admin then allow it)
				}
				
				if (null == module.get_id()) { // Either new, or no id specified
					
					// Check if it exists (uniquely defined by url + owner)
					UIModulePojo queryModule = new UIModulePojo();
					queryModule.setUrl(module.getUrl());
					queryModule.setAuthor(userName);
					BasicDBObject oldModuleDbo = (BasicDBObject) DbManager.getSocial().getUIModules().findOne(queryModule.toDb());
					
					if (null == oldModuleDbo) { // New module
						module.set_id(new ObjectId());
						module.setCreated(new Date());
						//DEBUG
						//System.out.println("New module: " + module.getUrl());
					}//TESTED
					else { // Overwrite
						UIModulePojo oldModule = UIModulePojo.fromDb(oldModuleDbo, UIModulePojo.class);
						module.set_id(oldModule.get_id());
						//DEBUG
						//System.out.println("Overwrite module: " + module.get_id());
					}//TESTED
					module.setModified(new Date());
				}//TESTED
				else { // Check if it already exists, owner must match if so

					UIModulePojo moduleQuery = new UIModulePojo(); 
					moduleQuery.set_id(module.get_id());
					BasicDBObject oldModuleDbo = (BasicDBObject) DbManager.getSocial().getUIModules().findOne(moduleQuery.toDb());
					if (null == oldModuleDbo) {
						// Fine, carry on
						//DEBUG
						//System.out.println("Id specified for new module: " + module.get_id());
					}//TESTED
					else {
						UIModulePojo oldModule = UIModulePojo.fromDb(oldModuleDbo, UIModulePojo.class);
						if ((null != oldModule.getAuthor()) && (!userName.equals(oldModule.getAuthor())))
						{ // Owner doesn't match
							if (!bAdmin) {
								rp.setResponse(new ResponseObject("Install Module",false,"Permissions error: must be either owner or root"));
								return rp;							
							}
						}//TESTED						
					}
					
					//Set modified
					module.setModified(new Date());
				}
			}
			// Insert or update:
			DbManager.getSocial().getUIModules().save(module.toDb());
			
			UIModulePojo returnVal = new UIModulePojo();
			returnVal.set_id(module.get_id());
			returnVal.setApproved(true);
			rp.setResponse(new ResponseObject("Install Module",true,"module installed/updated successfully"));
			rp.setData(returnVal, new UIModulePojoApiMap(null));
		}
		catch (Exception ex) {
			//ex.printStackTrace();
			rp.setResponse(new ResponseObject("Install Module",false,"Module install error: " + ex.getMessage()));			
		}//TESTED
		return rp;
	}//TESTED
	
	public ResponsePojo deleteModule(String moduleId, String userIdStr)
	{
		ResponsePojo rp = new ResponsePojo();
		try
		{
			UIModulePojo moduleQuery = new UIModulePojo();
			moduleQuery.set_id(new ObjectId(moduleId));
			BasicDBObject oldModuleDbo = (BasicDBObject) DbManager.getSocial().getUIModules().findOne(moduleQuery.toDb());
			if (null == oldModuleDbo) {
				rp.setResponse(new ResponseObject("Delete Module",false,"Module doesn't exist"));
				return rp;
			}//TESTED
			UIModulePojo oldModule = UIModulePojo.fromDb(oldModuleDbo, UIModulePojo.class);
			
			// Get username from profile id:
			AuthenticationPojo userQuery = new AuthenticationPojo();
			userQuery.setProfileId(new ObjectId(userIdStr));
			BasicDBObject userDbo = (BasicDBObject) DbManager.getSocial().getAuthentication().findOne(userQuery.toDb());				
			String userName = userIdStr;
			if (null != userDbo) {
				AuthenticationPojo user =  AuthenticationPojo.fromDb(userDbo, AuthenticationPojo.class);
				userName = user.getUsername();
			}//TOTEST
			
			String moduleOwner = oldModule.getAuthor();
			if ((null == moduleOwner) || (moduleOwner.equals(userName)) || RESTTools.adminLookup(userIdStr)) {
				DbManager.getSocial().getUIModules().remove(moduleQuery.toDb());
				rp.setResponse(new ResponseObject("Delete Module",true,"module deleted successfully"));
			}//TESTED
			else {
				rp.setResponse(new ResponseObject("Delete Module",false,"Don't have permission to delete module"));			
				return rp;
			}//TESTED
		}
		catch (Exception ex) {
			//ex.printStackTrace();
			rp.setResponse(new ResponseObject("Delete Module",false,"Module delete error"));			
		}//TESTED		
	
		return rp;
	}
}
