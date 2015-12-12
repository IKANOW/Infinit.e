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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.bson.types.ObjectId;

import com.ikanow.infinit.e.api.config.source.SourceHandler;
import com.ikanow.infinit.e.api.custom.mapreduce.CustomHandler;
import com.ikanow.infinit.e.api.utils.SocialUtils;
import com.ikanow.infinit.e.api.utils.PropertiesManager;
import com.ikanow.infinit.e.api.utils.RESTTools;
import com.ikanow.infinit.e.data_model.Globals;
import com.ikanow.infinit.e.data_model.utils.SendMail;
import com.ikanow.infinit.e.data_model.api.ApiManager;
import com.ikanow.infinit.e.data_model.api.BasePojoApiMap;
import com.ikanow.infinit.e.data_model.api.ResponsePojo;
import com.ikanow.infinit.e.data_model.api.ResponsePojo.ResponseObject;
import com.ikanow.infinit.e.data_model.api.social.community.CommunityApprovalPojo;
import com.ikanow.infinit.e.data_model.api.social.community.CommunityPojoApiMap;
import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.MongoDbManager;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.ikanow.infinit.e.data_model.store.custom.mapreduce.CustomMapReduceJobPojo;
import com.ikanow.infinit.e.data_model.store.document.DocCountPojo;
import com.ikanow.infinit.e.data_model.store.social.community.CommunityApprovePojo;
import com.ikanow.infinit.e.data_model.store.social.community.CommunityAttributePojo;
import com.ikanow.infinit.e.data_model.store.social.community.CommunityMemberContactPojo;
import com.ikanow.infinit.e.data_model.store.social.community.CommunityMemberLinkPojo;
import com.ikanow.infinit.e.data_model.store.social.community.CommunityMemberPojo;
import com.ikanow.infinit.e.data_model.store.social.community.CommunityMemberPojo.MemberType;
import com.ikanow.infinit.e.data_model.store.social.community.CommunityMemberUserAttributePojo;
import com.ikanow.infinit.e.data_model.store.social.community.CommunityPojo;
import com.ikanow.infinit.e.data_model.store.social.community.CommunityUserAttributePojo;
import com.ikanow.infinit.e.data_model.store.social.community.CommunityPojo.CommunityType;
import com.ikanow.infinit.e.data_model.store.social.person.PersonCommunityPojo;
import com.ikanow.infinit.e.data_model.store.social.person.PersonContactPojo;
import com.ikanow.infinit.e.data_model.store.social.person.PersonLinkPojo;
import com.ikanow.infinit.e.data_model.store.social.person.PersonPojo;
import com.ikanow.infinit.e.data_model.store.social.sharing.SharePojo;
import com.ikanow.infinit.e.data_model.store.social.sharing.SharePojo.ShareCommunityPojo;
import com.ikanow.infinit.e.data_model.store.social.sharing.SharePojo.ShareOwnerPojo;
import com.ikanow.infinit.e.processing.generic.GenericProcessingController;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

/**
 * This class is for all operations related to the retrieval, addition
 * or update of communities within the system
 */
public class CommunityHandler 
{
	private static final Logger logger = Logger.getLogger(CommunityHandler.class);
	private SourceHandler sourceHandler = new SourceHandler();
	
	//////////////////////////////////////////////////////////////////////////
	////////////////////////   REST handlers  ////////////////////////////////
	//////////////////////////////////////////////////////////////////////////
		
	/**
	 * getCommunities (REST)
	 * Returns information for all communities
	 * @return
	 */
	public ResponsePojo getCommunities(String userIdStr, CommunityPojo.CommunityType communityType, String communityIdStrList) 
	{	
		ResponsePojo rp = new ResponsePojo();

		/////////////////////////////////////////////////////////////////////////////////////////////////
		// Note: Only Sys Admins see all communities
		boolean isSysAdmin = RESTTools.adminLookup(userIdStr);

		try
		{
			if (isSysAdmin)
			{
				BasicDBObject query = new BasicDBObject();
				if (communityIdStrList != null)
				{
					String[] communityIdStrs = SocialUtils.getCommunityIds(userIdStr, communityIdStrList);
					Set<ObjectId> communityIdSet = new TreeSet<ObjectId>();
					for (String s: communityIdStrs) {
						ObjectId communityId = new ObjectId(s); 
						communityIdSet.add(communityId);
					}
					query.put(CommunityPojo._id_, new BasicDBObject(MongoDbManager.in_, communityIdSet));
					//communityIdStr = allowCommunityRegex(userIdStr, communityIdStr);
					//query.put("_id", new ObjectId(communityIdStr));
				}
				addCommunityTypeTerm(query, communityType);
				DBCursor dbc = DbManager.getSocial().getCommunity().find(query);
				
				if ( dbc.hasNext() )
				{
					List<CommunityPojo> communities = CommunityPojo.listFromDb(dbc, CommunityPojo.listType());
					filterCommunityMembers(communities, isSysAdmin, userIdStr);
					rp.setData(communities, new CommunityPojoApiMap());
					rp.setResponse(new ResponseObject("Community Info", true, "Community info returned successfully"));				
				}
				else
				{
					rp.setResponse(new ResponseObject("Community Info", true, "No communities returned"));	
				}
			}
			else // Get all public communities and all private communities to which the user belongs
			{							
				
				// Set up the query
//				BasicDBObject queryTerm1 = new BasicDBObject("communityAttributes.isPublic.value", "true");
//				BasicDBObject queryTerm2 = new BasicDBObject("members._id", new ObjectId(userIdStr));
//				BasicDBObject queryTerm3 = new BasicDBObject("ownerId", new ObjectId(userIdStr));
//				BasicDBObject query = new BasicDBObject(MongoDbManager.or_, Arrays.asList(queryTerm1, queryTerm2, queryTerm3));
				BasicDBObject queryPublic = new BasicDBObject("communityAttributes.isPublic.value", "true");
				
				//THIS GETS ALL OUR PRIVATE COMMUNITIES
				Set<ObjectId> communityIdSet = null;
				if (communityIdStrList == null)
					communityIdStrList = "*";
				String[] communityIdStrs = SocialUtils.getCommunityIds(userIdStr, communityIdStrList);
				communityIdSet = new TreeSet<ObjectId>();
				for (String s: communityIdStrs) {
					ObjectId communityId = new ObjectId(s); 
					communityIdSet.add(communityId);
				}
				BasicDBObject queryPrivate = new BasicDBObject(CommunityPojo._id_, new BasicDBObject(MongoDbManager.in_, communityIdSet));
				//communityIdStr = allowCommunityRegex(userIdStr, communityIdStr);
				//query.put("_id", new ObjectId(communityIdStr));
				BasicDBObject query = new BasicDBObject(MongoDbManager.or_, Arrays.asList(queryPrivate, queryPublic));
				
				addCommunityTypeTerm(query, communityType);

				DBCursor dbc = DbManager.getSocial().getCommunity().find(query);				
				if ( dbc.hasNext() )
				{
					List<CommunityPojo> communities = CommunityPojo.listFromDb(dbc, CommunityPojo.listType());
					filterCommunityMembers(communities, isSysAdmin, userIdStr);
					//add personal community (if not filtered)
					DBObject dbo = DbManager.getSocial().getCommunity().findOne(new BasicDBObject("_id",new ObjectId(userIdStr)));
					if ( dbo != null )
					{
						CommunityPojo personal_community = CommunityPojo.fromDb(dbo, CommunityPojo.class);
						if ( communityIdSet == null || communityIdSet.contains(personal_community.getId()) )
							communities.add(personal_community);
					}
					rp.setData(communities, new CommunityPojoApiMap());
					rp.setResponse(new ResponseObject("Community Info", true, "Community info returned successfully"));				
				}
				else
				{
					rp.setResponse(new ResponseObject("Community Info", true, "No communities returned"));	
				}
			}
		}
		catch (Exception e)
		{
			logger.error("Exception Message: " + e.getMessage(), e);
			rp.setResponse(new ResponseObject("Community Info", false, "error returning community info"));
		}

		return rp;
	}
	

	/**
	 * getCommunities (REST)
	 * Returns information for communities where isPublic = true/false
	 * @param isPublic
	 * @return
	 */
	public ResponsePojo getCommunities(String userIdStr, Boolean isPublic, CommunityPojo.CommunityType communityType) 
	{
		ResponsePojo rp = new ResponsePojo();
		
		/////////////////////////////////////////////////////////////////////////////////////////////////
		// Note: Only Sys Admins see private communities
		boolean isSysAdmin = RESTTools.adminLookup(userIdStr);
		
		try
		{	
			// Set up the query
			BasicDBObject query = new BasicDBObject();
			query.put("communityAttributes.isPublic.value", isPublic.toString());	
			if (!isPublic && !isSysAdmin)
			{
				//Add user ID to query so only get (private) communities of which I'm a member
				query.put("members._id", new ObjectId(userIdStr));
			}
			query.put("communityStatus", new BasicDBObject("$ne", "disabled"));
			addCommunityTypeTerm(query, communityType);
			
			DBCursor dbc = DbManager.getSocial().getCommunity().find(query);
			if ( dbc.hasNext() )
			{
				List<CommunityPojo> communities = CommunityPojo.listFromDb(dbc, CommunityPojo.listType());
				filterCommunityMembers(communities, isSysAdmin, userIdStr);
				rp.setData(communities, new CommunityPojoApiMap());
				rp.setResponse(new ResponseObject("Community Info", true, "Community info returned successfully"));				
			}
			else
			{
				rp.setResponse(new ResponseObject("Community Info", true, "No communities returned"));	
			}
		} 
		catch (Exception e)
		{
			logger.error("Exception Message: " + e.getMessage(), e);
			rp.setResponse(new ResponseObject("Community Info", false, "error returning community info"));
		}
		return rp;
	}
		
	
	/**
	 * getCommunity (REST)
	 * Returns information for a single community
	 * @param communityIdStr
	 * @return
	 */
	public ResponsePojo getCommunity(String userIdStr, String communityIdStr, boolean showDocInfo, CommunityPojo.CommunityType communityType) 
	{	
		ResponsePojo rp = new ResponsePojo();
		
		try
		{
			
			// Set up the query
			BasicDBObject query = new BasicDBObject();
			if (communityIdStr != null)
			{
				communityIdStr = allowCommunityRegex(userIdStr, communityIdStr);
				query.put("_id", new ObjectId(communityIdStr));
			}
			else
			{
				query.put("_id", new ObjectId("4c927585d591d31d7b37097a")); // (hardwired sys community)
			}
			addCommunityTypeTerm(query, communityType);
			
			// Get GsonBuilder object with MongoDb de/serializers registered
			BasicDBObject dbo = (BasicDBObject)DbManager.getSocial().getCommunity().findOne(query);
			
			if (dbo != null)
			{
				CommunityPojo community = CommunityPojo.fromDb(dbo, CommunityPojo.class);
				if (showDocInfo) {
					DocCountPojo dc = (DocCountPojo) DbManager.getDocument().getCounts().findOne(query);
					if (null != dc) {
						dc.set_id(null);
						community.setDocumentInfo(dc);
					}
				}
				community = filterCommunityMembers(community, RESTTools.adminLookup(userIdStr), userIdStr);
				rp.setData(community, new CommunityPojoApiMap());
				rp.setResponse(new ResponseObject("Community Info", true, "Community info returned successfully"));
			}
			else
			{
				rp.setResponse(new ResponseObject("Community Info", false, "Unable to return information for the community specified."));
			}
		} 
		catch (Exception e)
		{
			//logger.error("Exception Message: " + e.getMessage(), e);
			rp.setResponse(new ResponseObject("Community Info", false, "Error returning community info: " + e.getMessage()));
		}
		return rp;
	}

	/**
	 * getSystemCommunity (REST)
	 * @return ResponsePojo
	 */
	public ResponsePojo getSystemCommunity(CommunityPojo.CommunityType communityType) 
	{	
		return getCommunity(null, null, false, communityType);
	}
	
	public ResponsePojo addCommunity(String userIdStr, String json, CommunityType communityType) {
		ResponsePojo rp = new ResponsePojo();
		CommunityPojo newCommunity = null;
		try
		{
			newCommunity = ApiManager.mapFromApi(json, CommunityPojo.class, new CommunityPojoApiMap());
		}
		catch (Exception ex)
		{
			rp.setResponse(new ResponseObject("Add Community",false,"Add json is badly formatted, could not deserialize."));
			return rp;
		}
		
		//verify the necessary params are set
		if (  newCommunity.getName() == null || newCommunity.getName().isEmpty() )
		{
			rp.setResponse(new ResponseObject("Add Community",false,"Name field is required"));
			return rp;
		}
		if (  newCommunity.getDescription() == null || newCommunity.getDescription().isEmpty() )
		{
			rp.setResponse(new ResponseObject("Add Community",false,"Description field is required"));
			return rp;
		}
		
		String parent_id = null;
		if (newCommunity.getParentId() != null ) parent_id = newCommunity.getParentId().toString();
		String tags = null;
		if ( newCommunity.getTags() != null ) tags = newCommunity.getTags().toString();
		//pass them on to the regular create function
		return addCommunity(userIdStr, newCommunity.getName(), newCommunity.getDescription(), parent_id, 
				tags, communityType, newCommunity.getCommunityAttributes(), newCommunity.getCommunityUserAttribute());
	}
	
	/**
	 * addCommunity (REST)
	 * Creates a new community
	 * @param userIdStr
	 * @param name
	 * @param description
	 * @param parentIdStr
	 * @param parentName
	 * @param tags
	 * @param ownerId
	 * @param ownerDisplayName
	 * @return ResponsePojo
	 */
	public ResponsePojo addCommunity(String userIdStr, String name, String description, String parentIdStr, String tags, CommunityPojo.CommunityType communityType)
	{
		return addCommunity(userIdStr, name, description, parentIdStr, tags, communityType, null, null);
	}
	
	public ResponsePojo addCommunity(String userIdStr, String name, String description, String parentIdStr, String tags, CommunityPojo.CommunityType communityType, Map<String, CommunityAttributePojo> community_attributes, Map<String, CommunityUserAttributePojo> user_attributes)
	{
		String userName = null;
		String userEmail = null;
		String parentName = null;
		
		try
		{
			DBObject dboperson = DbManager.getSocial().getPerson().findOne(new BasicDBObject("_id", new ObjectId(userIdStr)));
			
			if ( dboperson != null )
			{
				PersonPojo person =  PersonPojo.fromDb(dboperson, PersonPojo.class);
				userName = person.getDisplayName();
				userEmail = person.getEmail();
			
				// Parent Community is Optional 
				if (parentIdStr != null)
				{
					try {
						DBObject dboparent = DbManager.getSocial().getCommunity().findOne(new BasicDBObject("_id", new ObjectId(parentIdStr)));
						if ( dboparent != null )
						{
							CommunityPojo cp = CommunityPojo.fromDb(dboparent, CommunityPojo.class);
							parentName = cp.getName();
							
							if (cp.getIsPersonalCommunity()) {
								return new ResponsePojo(new ResponseObject("Add Community", false, "Can't create sub-community of personal community"));							
							}//TESTED
							if ((null == cp.getCommunityStatus()) || !cp.getCommunityStatus().equalsIgnoreCase("active")) {
								return new ResponsePojo(new ResponseObject("Add Community", false, "Can't create sub-community of inactive community"));							
							}//TESTED
							// Check attributes
							if (null != cp.getCommunityAttributes()) {
								CommunityAttributePojo attr = cp .getCommunityAttributes().get("usersCanCreateSubCommunities");
								if ((null == attr) || (null== attr.getValue()) || (attr.getValue().equals("false"))) {
									if (!cp.isOwner(person.get_id()) && !SocialUtils.isModerator(userIdStr, cp) && !RESTTools.adminLookup(userIdStr)) {
										return new ResponsePojo(new ResponseObject("Add Community", false, "Can't create sub-community when not permitted by parent"));
									}//TESTED (owner+admin+mod)
								}
							}						
						}//TESTED - different restrictions as above 
						else
						{
							return new ResponsePojo(new ResponseObject("Add Community", false, "Parent community does not exist"));
						}//TESTED
					}
					catch (Exception e) {
						return new ResponsePojo(new ResponseObject("Add Community", false, "Invalid parent community id"));
					}//TESTED
				}
			}
			else
			{
				return new ResponsePojo(new ResponseObject("Add Community", false, "Error: Unable to get person record"));
			}
		}
		catch (Exception ex)
		{
			return new ResponsePojo(new ResponseObject("Add Community", false, "General Error: " + ex.getMessage()));
		}
		return addCommunity(userIdStr, name, description, parentIdStr, parentName, tags, userIdStr, userName, userEmail, communityType, community_attributes, user_attributes);
	}
	
	/**
	 * addCommunity (REST)
	 * Creates a new community by id (alternate)
	 */
	private ResponsePojo addCommunity(String userIdStr, String name, String description, 
			String parentIdStr, String parentName, String tags, String ownerIdStr, 
			String ownerDisplayName, String ownerEmail, CommunityPojo.CommunityType communityType, Map<String, CommunityAttributePojo> community_attributes, Map<String, CommunityUserAttributePojo> user_attributes)
	{
		return addCommunity(userIdStr, null, name, description, parentIdStr, parentName, tags, 
				ownerIdStr, ownerDisplayName, ownerEmail, communityType, community_attributes, user_attributes);	
	}	
	public ResponsePojo addCommunity(String userId, String idStr, String name, String description, 
			String parentIdStr, String parentName, String tags, String ownerIdStr, 
			String ownerDisplayName, String ownerEmail, CommunityPojo.CommunityType communityType, Map<String, CommunityAttributePojo> community_attributes, Map<String, CommunityUserAttributePojo> user_attributes)
	{
		ResponsePojo rp = new ResponsePojo();
		
		try
		{
			// Check to see if a community exists already with the supplied name or ID
			// do not create new one if true - 
			// TODO (INF-1214): Think about need for unique names - proposed:
			//	     Community names unique per parent community
			BasicDBObject query = new BasicDBObject();
			if (idStr == null)
			{
				query.put("name", name);		
			}
			else
			{
				query.put("_id", new ObjectId(idStr));
			}
			DBObject dbo = DbManager.getSocial().getCommunity().findOne(query);			
			
			if (dbo == null)
			{
				ObjectId oId = null;
				if (idStr == null)
				{
					oId = new ObjectId();
				}
				else
				{
					oId = new ObjectId(idStr);
				}
				CommunityPojo c = new CommunityPojo();
				c.setType(communityType);
				c.setId(oId);
				c.setCreated(new Date());
				c.setModified(new Date());
				c.setName(name);
				c.setDescription(description);
				if (parentIdStr != null && parentName != null)
				{
					c.setParentId(new ObjectId(parentIdStr));
					c.setParentName(parentName);
					c = SocialUtils.createParentTreeRecursion(c, false);
				}
				c.setIsPersonalCommunity(false);
				c.setTags(getTagsFromString(tags));
				c.setOwnerId(new ObjectId(ownerIdStr));
				c.setOwnerDisplayName(ownerDisplayName);
				c.setCommunityAttributes(getDefaultCommunityAttributes());
				c.setCommunityUserAttribute(getDefaultCommunityUserAttributes());
				
				if ( community_attributes != null )
					c.getCommunityAttributes().putAll(community_attributes);				
				if ( user_attributes != null )
					c.getCommunityUserAttribute().putAll(user_attributes);				
				
				// Insert new community document in the community collection
				DBObject commObj = c.toDb();

				// Create the index form of the community:
				if (CommunityType.user != c.getType()) {
					//check number of shards param
					int num_shards = GenericProcessingController.DEFAULT_NUM_SHARDS;
					if ( c.getCommunityAttributes().containsKey(CommunityAttributePojo.NUM_SHARDS_ATTRIBUTE))
					{
						try
						{
							num_shards = Integer.parseInt(c.getCommunityAttributes().get(CommunityAttributePojo.NUM_SHARDS_ATTRIBUTE).getValue());
						}
						catch (Exception ex)
						{
							rp.setResponse(new ResponseObject("Add Community", false, "Error adding new community because of num_shards failure: " + ex.getMessage()));
							return rp;
						}
					}
					if ( num_shards == 0 )
						num_shards = GenericProcessingController.DEFAULT_NUM_SHARDS;
					if ( num_shards > GenericProcessingController.MAX_NUM_SHARDS )
					{
						rp.setResponse(new ResponseObject("Add Community", false, "Error adding new community because num_shards is set to too large of a value, was " + num_shards + " max is " + GenericProcessingController.MAX_NUM_SHARDS));
						return rp;
					}
					if ( num_shards > 0 )
					{
						try {
							GenericProcessingController.createCommunityDocIndex(c.getId().toString(), c.getParentId(), c.getIsPersonalCommunity(), c.getIsSystemCommunity(), false, num_shards);
						}
						catch (Exception e) { // Can't create community
							rp.setResponse(new ResponseObject("Add Community", false, "Error adding new community because of index failure: " + e.getMessage()));
							return rp;
						}
					}
				}
				//TESTED
				
				DbManager.getSocial().getCommunity().save(commObj);				
				
				// If a child, update the parent:
				if (null != c.getParentId()) {
					BasicDBObject updateQuery = new BasicDBObject("_id", c.getParentId());
					BasicDBObject updateUpdate = new BasicDBObject(DbManager.addToSet_, new BasicDBObject("children", c.getId()));
					DbManager.getSocial().getCommunity().update(updateQuery, updateUpdate, false, true);
				}
				//TESTED
				
				// Update the new community record to add the owner to the list of members
				rp = addCommunityMember(userId, oId.toString(), name, ownerIdStr, ownerEmail, ownerDisplayName, "owner", "active");
				rp.setResponse(new ResponseObject("Add Community", true, "The " + name + " community has been added."));				
			}
			else
			{
				rp.setResponse(new ResponseObject("Add Community", false, 
						"Error adding new community. A community with the name " + name + " already exists."));
			}
			
		}
		catch (Exception e)
		{
			// If an exception occurs log the error
			logger.error("Exception Message: " + e.getMessage(), e);
			rp.setResponse(new ResponseObject("Add Community", false, 
					"Error adding new community " + e.getMessage()));
		}
		return rp;
	}

	/**
	 * removeCommunity (REST)
	 * Remove communityid only if the personid is the owner
	 * TODO (INF-1214): Remove users from the groups in both personpojo and communitypojo 
	 * 
	 * @param personIdStr
	 * @param communityIdStr
	 * @return
	 */
	public ResponsePojo removeCommunity(String personIdStr, String communityIdStr, CommunityPojo.CommunityType communityType) 
	{
		boolean isSysAdmin = RESTTools.adminLookup(personIdStr);
		ResponsePojo rp = new ResponsePojo();
		try
		{
			ObjectId communityId = new ObjectId(communityIdStr);
			//get the communitypojo
			communityIdStr = allowCommunityRegex(personIdStr, communityIdStr);
			DBObject communitydbo = DbManager.getSocial().getCommunity().findOne(new BasicDBObject("_id",communityId));
			if ( communitydbo != null )
			{
				CommunityPojo cp = CommunityPojo.fromDb(communitydbo, CommunityPojo.class);
				//get the personpojo
				DBObject persondbo = DbManager.getSocial().getPerson().findOne(new BasicDBObject("_id",new ObjectId(personIdStr)));
				if ( persondbo != null )
				{
					//PersonPojo pp = gson.fromJson(persondbo.toString(),PersonPojo.class);
					if ( !cp.getIsPersonalCommunity() )
					{
						if ( cp.isOwner(new ObjectId(personIdStr)) || isSysAdmin )
						{
							if (cp.getCommunityStatus().equals("disabled")) { // Delete for good, this is going to be ugly...
								
								if ((null != cp.getChildren()) && !cp.getChildren().isEmpty()) {
									rp.setResponse(new ResponseObject("Delete community", false, "Undeleted sub-communities exist, please delete them first"));
									return rp;
								}
								//TESTED
								
								// 0] If it's a user group then remove from all communities
								
								if (CommunityType.user == cp.getType()) {									
									BasicDBObject query = new BasicDBObject("members._id", cp.getId()); 
									CommunityMemberPojo cmp = new CommunityMemberPojo();
									cmp.set_id(cp.getId());
									BasicDBObject actions = new BasicDBObject();
									actions.put("$pull", new BasicDBObject("members", new BasicDBObject("_id", cp.getId())));
										// ie for communities for which he's a member...remove...any elements of the list members...with his _id
									
									DbManager.getSocial().getCommunity().update(query, actions, false, true); 
										// (don't upsert, many times)																					
								}//TESTED
								
								// 1] Remove from all shares (delete shares if that leaves them orphaned)
								
								BasicDBObject deleteQuery1 = new BasicDBObject(ShareCommunityPojo.shareQuery_id_, communityId);
								BasicDBObject deleteFields1 = new BasicDBObject(SharePojo.communities_, 1);
								List<SharePojo> shares = SharePojo.listFromDb(DbManager.getSocial().getShare().find(deleteQuery1, deleteFields1), SharePojo.listType());				
								for (SharePojo share: shares) {
									if (1 == share.getCommunities().size()) { // delete this share
										DbManager.getSocial().getShare().remove(new BasicDBObject(SharePojo._id_, share.get_id()));
									}
								}
								BasicDBObject update1 = new BasicDBObject(DbManager.pull_, new BasicDBObject(SharePojo.communities_, 
																							new BasicDBObject(ShareOwnerPojo._id_, communityId)));
								DbManager.getSocial().getShare().update(deleteQuery1, update1, false, true);
								
								//TESTED (both types)
								
								// 2] Remove from all sources (also delete the documents)
								// (In most cases this will leave the source orphaned, so delete it)
								
								BasicDBObject deleteQuery2 = new BasicDBObject(SourcePojo.communityIds_, communityId);
								BasicDBObject deleteFields2 = new BasicDBObject(SourcePojo.communityIds_, 1);
								List<SourcePojo> sources = SourcePojo.listFromDb(DbManager.getIngest().getSource().find(deleteQuery2, deleteFields2), SourcePojo.listType());
								List<SourcePojo> failedSources = new ArrayList<SourcePojo>();
								for (SourcePojo source: sources) 
								{
									ResponsePojo rp1 = null;
									SourceHandler tmpHandler = new SourceHandler();
									if (1 == source.getCommunityIds().size()) { // delete this source
										rp1 = tmpHandler.deleteSource(source.getId().toString(), communityIdStr, personIdStr, false);
											// (deletes all docs and removes from the share)
									}
									else { // Still need to delete docs from this share from this community
										rp1 = tmpHandler.deleteSource(source.getId().toString(), communityIdStr, personIdStr, true);										
									}
									if ( rp1 != null && !rp1.getResponse().isSuccess() )
									{
										failedSources.add(source);
									}
								}
								
								//if we have more than 1 failed source, bail out w/ error
								if (failedSources.size() > 0 )
								{
									StringBuilder sb = new StringBuilder();
									for ( SourcePojo source : failedSources )
										sb.append(source.getId().toString() + " ");
									rp.setResponse(new ResponseObject("Delete community", false, "Could not stop sources (they might be currently running): " + sb.toString()));
									return rp;
								}
								
								BasicDBObject update2 = new BasicDBObject(DbManager.pull_, new BasicDBObject(SourcePojo.communityIds_, communityId));
								DbManager.getSocial().getShare().update(deleteQuery2, update2, false, true);

								//TESTED (both types, check docs deleted)
								
								// 3] Remove from all map reduce jobs (delete any that it is only comm left on)
								String customJobsMessage = removeCommunityFromJobs(personIdStr, communityId);
								if ( customJobsMessage.length() > 0)
								{
									rp.setResponse(new ResponseObject("Delete community", false, "Could not stop all map reduce jobs (they might be currently running): " + customJobsMessage));
									return rp;
								}
								
								// 4] Finally delete the object itself
								
								DbManager.getSocial().getCommunity().remove(new BasicDBObject("_id", communityId));
								
								// Remove from index:
								if (CommunityType.user != cp.getType()) {
									GenericProcessingController.deleteCommunityDocIndex(communityId.toString(), cp.getParentId(), false);
								}
								//TESTED
								
								// 5] Finally finally remove from parent communities
								if (null != cp.getParentId()) {
									BasicDBObject updateQuery = new BasicDBObject("_id", cp.getParentId());
									BasicDBObject updateUpdate = new BasicDBObject(DbManager.pull_, new BasicDBObject("children", cp.getId()));
									DbManager.getSocial().getCommunity().update(updateQuery, updateUpdate, false, true);									
								}
								//TESTED
								
								rp.setResponse(new ResponseObject("Delete community", true, "Community deleted forever. " + customJobsMessage));
							}
							else { // First time, just remove all users and disable
								//at this point, we have verified, community/user exist, not a personal group, user is member and owner
								//set community as inactive (for some reason we don't delete it)
								DbManager.getSocial().getCommunity().update(new BasicDBObject("_id", communityId), 
																			new BasicDBObject(DbManager.set_, new BasicDBObject("communityStatus","disabled")));
								

								//run user.datagroup_reason logic
								CommunityPojo.removeDatagroupFromUserOrUsergroup(null, cp, null);
								
								//remove all members
								for ( CommunityMemberPojo cmp : cp.getMembers())
									removeCommunityMember(personIdStr, communityIdStr, cmp.get_id().toString());
								rp.setResponse(new ResponseObject("Delete community", true, "Community disabled successfully - call delete again to remove for good, including all sources, shares, and documents"));
							}
						}
						else
						{
							rp.setResponse(new ResponseObject("Delete community", false, "You are not the owner of this community"));
						}
					}
					else
					{
						rp.setResponse(new ResponseObject("Delete community", false, "Cannot delete personal community."));
					}					
				}
				else
				{
					rp.setResponse(new ResponseObject("Delete community", false, "Person ID was incorrect, no matching person found"));
				}
			}
			else
			{
				rp.setResponse(new ResponseObject("Delete community", false, "Community ID was incorrect, no matching commmunity found"));
			}

		}
		catch (Exception ex)
		{
			rp.setResponse(new ResponseObject("Delete community", false, "Error returning community info: " + ex.getMessage()));			
		}
		
		return rp;
	}
	
	/**
	 * Removes this community from any map reduce jobs, then deletes any jobs where it was the only community
	 * 
	 * @param communityId
	 * @return 
	 */
	private String removeCommunityFromJobs(String personIdStr, ObjectId communityId)
	{
		StringBuilder sb = new StringBuilder();		
		List<CustomMapReduceJobPojo> failedToRemove = CustomHandler.removeCommunityJobs(communityId);				
		//return a list of job ids
		for ( CustomMapReduceJobPojo cmr : failedToRemove )
		{
			sb.append(cmr.jobtitle + " ");
		}
		if ( sb.length() > 0 )
		{
			sb.insert(0, "These map reduce jobs could not be removed: " );
		}
		return sb.toString();
	}

	// (Note supports personId as either Id or username (email) both are unique indexes)

	/**
	 * updateMemberStatus (REST)
	 */
	
	public ResponsePojo updateMemberStatus(String callerIdStr, String personIdStr, String communityIdStr, String userStatus, CommunityPojo.CommunityType communityType) 
	{
		boolean isSysAdmin = RESTTools.adminLookup(callerIdStr);
		ResponsePojo rp = new ResponsePojo();
		try
		{
			communityIdStr = allowCommunityRegex(callerIdStr, communityIdStr);
			
			//verify user is in this community, then update status
			BasicDBObject query = new BasicDBObject("_id",new ObjectId(communityIdStr));
			DBObject dbo = DbManager.getSocial().getCommunity().findOne(query);
			if ( dbo != null )
			{
				// PersonId can be _id or username/email
				BasicDBObject dboPerson = (BasicDBObject) DbManager.getSocial().getPerson().findOne(new BasicDBObject("email", personIdStr));
				if (null != dboPerson) { // (ie personId isn't an email address... convert to ObjectId and try again)
					personIdStr = dboPerson.getString("_id"); 
				}
				// OK from here on, personId is the object Id...
								
				boolean bAuthorized = isSysAdmin || SocialUtils.isOwnerOrModerator(communityIdStr, callerIdStr) || isRemovingSelf(userStatus, callerIdStr, personIdStr);
				if (bAuthorized) {
					
					CommunityPojo cp = CommunityPojo.fromDb(dbo,CommunityPojo.class);
					ObjectId personId = new ObjectId(personIdStr);
					
					if ( cp.isOwner(personId) && !userStatus.equalsIgnoreCase("active")) {						
						rp.setResponse(new ResponseObject("Update member status",false,"Can't change owner status, remove their ownership first"));
						return rp;
					}//TESTED (tried as owner+admin (failed both times), tested owner works fine if setting/keeping active)
					else if ( cp.isMember(personId) )
					{
						// Remove user:
						if (userStatus.equalsIgnoreCase("remove")) 
						{
							//removeCommunityMember(callerIdStr, communityIdStr, personIdStr);
							rp = removeCommunityMember(callerIdStr, communityIdStr, personIdStr); //.setResponse(new ResponseObject("Update member status",true,"Member removed from community."));
						}
						else
						{
							//verified user, update status
							if ( cp.updateMemberStatus(personIdStr, userStatus) )
							{
								/////////////////////////////////////////////////////////////////////////////////////////////////
								// TODO (INF-1214): Make this code more robust to handle changes to the community that need to
								// Caleb: this means change update to $set
								/////////////////////////////////////////////////////////////////////////////////////////////////
								DbManager.getSocial().getCommunity().update(query, cp.toDb());
								rp.setResponse(new ResponseObject("Update member status",true,"Updated member status successfully"));
							}
							else
							{
								rp.setResponse(new ResponseObject("Update member status",false,"Failed to update status"));
							}
						}
					}
					else
					{
						rp.setResponse(new ResponseObject("Update member status",false,"User was not a member of the community"));
					}
				}
				else
				{
					rp.setResponse(new ResponseObject("Update member status",false,"Caller must be admin, or a community owner or moderator"));
				}//TESTED - tried to update my status as member (failed), as admin (succeeded), as moderator (succeeded), as owner (succeeded)  
			}
			else
			{
				rp.setResponse(new ResponseObject("Update member status",false,"Community does not exist"));
			}			
		}
		catch(Exception ex)
		{
			rp.setResponse(new ResponseObject("Update member status",false, Globals.populateStackTrace(new StringBuffer("General Error, bad params maybe? "), ex).toString()));
		}
		return rp;
	}//TESTED

	/**
	 * Utility function for testing self removal from communities
	 * Returns true if the status is "remove" and callerId == personId
	 * 
	 * @param userStatus
	 * @param callerIdStr
	 * @param personIdStr
	 * @return
	 */
	private boolean isRemovingSelf(String userStatus, String callerIdStr, String personIdStr) 
	{
		return userStatus.toLowerCase().equals("remove") && ( callerIdStr.equals(personIdStr));
	}


	// (Note supports personId as either Id or username (email) both are unique indexes)

	/**
	 * bulkUpdateOperation (REST)
	 * For ,-separated personIdStr variables, applies the operation to each member in turn, aggregating the results, supporting:
	 * - inviteCommunity
	 * - updateMemberType
	 * - updateMemberStatus
	 */
	public ResponsePojo bulkUpdateOperation(String opName, String callerIdStr, String personIdStr, String userType, String userStatus, String communityIdStr, CommunityPojo.CommunityType communityType, String skipInvitation)
	{
		 String[] personIds = personIdStr.split("\\s*,\\s*");
		 HashMap<String, String> failed = new HashMap<String, String>();
		 int num_failed = 0;
		 LinkedList<String> succeeded = new LinkedList<String>();
		 int num_succeeded = 0;
		 ResponsePojo rp = new ResponsePojo();
		 rp.setResponse(new ResponseObject());
		 rp.getResponse().setSuccess(true);
		 for (String subPerson: personIds) {
			 ResponsePojo tmpRp = null;
			 if (opName.equalsIgnoreCase("inviteCommunity")) {
				 tmpRp = this.inviteCommunity(callerIdStr, subPerson, communityIdStr, skipInvitation, communityType);
			 }
			 else if (opName.equalsIgnoreCase("updateMemberType")) {
				 tmpRp = this.updateMemberType(callerIdStr, subPerson, communityIdStr, userType, communityType);
			 }
			 else if (opName.equalsIgnoreCase("updateMemberStatus")) {
				 tmpRp = this.updateMemberStatus(callerIdStr, subPerson, communityIdStr, userStatus, communityType);
			 }
			 rp.getResponse().setAction(tmpRp.getResponse().getAction());
			 rp.getResponse().setTime(rp.getResponse().getTime() + tmpRp.getResponse().getTime());
			 if (!tmpRp.getResponse().isSuccess()) {
				 rp.getResponse().setSuccess(false);
				 failed.put(subPerson, tmpRp.getResponse().getMessage());
				 num_failed++;
			 }
			 else {
				 succeeded.add(subPerson);							 
				 num_succeeded++;
			 }						 
		 }//(end loop over bulk users)
		 BasicDBObject breakdown = new BasicDBObject();
		 breakdown.put("succeeded", succeeded);
		 breakdown.put("failed", failed);
		 ResponsePojo comm = getCommunity(callerIdStr, communityIdStr, false, communityType);
		 if ( comm.getResponse().isSuccess() )
			 breakdown.put("community", (CommunityPojo)comm.getData());
		 rp.getResponse().setMessage("succeeded=" + num_succeeded + " failed=" + num_failed);
		 rp.setData(breakdown, (BasePojoApiMap<BasicDBObject>)null);
		 
		 return rp;
		
	}//TESTED (by hand)
	
	/**
	 * updateMemberType (REST)
	 */
	public ResponsePojo updateMemberType(String callerIdStr, String personIdStr, String communityIdStr, String userType, CommunityPojo.CommunityType communityType) 
	{
		boolean isSysAdmin = RESTTools.adminLookup(callerIdStr);
		ResponsePojo rp = new ResponsePojo();
		try
		{
			if (!userType.equalsIgnoreCase("owner") && !userType.equalsIgnoreCase("moderator") && !userType.equalsIgnoreCase("member") && !userType.equalsIgnoreCase("content_publisher"))
			{
				rp.setResponse(new ResponseObject("Update member type",false,"Invalid user type: " + userType));
				return rp;
			}//TESTED - tested all the types work, hacked members.jsp to insert invalid type
			
			//verify user is in this community, then update status
			communityIdStr = allowCommunityRegex(callerIdStr, communityIdStr);
			BasicDBObject query = new BasicDBObject("_id",new ObjectId(communityIdStr));
			DBObject dbo = DbManager.getSocial().getCommunity().findOne(query);
			if ( dbo != null )
			{
				// PersonId can be _id or username/email
				BasicDBObject dboPerson = (BasicDBObject) DbManager.getSocial().getPerson().findOne(new BasicDBObject("email", personIdStr));
				if (null != dboPerson) { // (ie personId isn't an email address... convert to ObjectId and try again)
					personIdStr = dboPerson.getString("_id"); 
				}
				// OK from here on, personId is the object Id...

				CommunityPojo cp = CommunityPojo.fromDb(dbo,CommunityPojo.class);
				
				boolean bOwnershipChangeRequested = userType.equalsIgnoreCase("owner");
				boolean bAuthorized = isSysAdmin;
				if (!bAuthorized) {
					if (bOwnershipChangeRequested) { // must be owner or admin				
						bAuthorized = cp.isOwner(new ObjectId(callerIdStr));
					}//TESTED - tried to update myself as moderator to owner (failed), gave up my community (succeeded), changed ownership as admin (FAILED) 
					else { // Can also be moderator
						bAuthorized = SocialUtils.isOwnerOrModerator(communityIdStr, callerIdStr);					
					}//TESTED - tried to update my role as member (failed), as admin->moderator (succeeded), as moderator (succeeded)
				}
				
				if (bAuthorized) // (see above)
				{
					if ( cp.isMember(new ObjectId(personIdStr)))
					{
						boolean bChangedMembership = false;
						boolean bChangedOwnership = !bOwnershipChangeRequested;
						
						ObjectId personId = new ObjectId(personIdStr);
						
						// Check that not trying to downgrade owner...
						if (cp.isOwner(personId) && !bOwnershipChangeRequested) {
							rp.setResponse(new ResponseObject("Update member type",false,"To change ownership, set new owner, will automatically downgrade existing owner to moderator"));
							return rp;
						}//TESTED
						
						String personDisplayName = null;
						//verified user, update status
						for ( CommunityMemberPojo cmp : cp.getMembers())
						{
							if ( cmp.get_id().equals(personId) )
							{
								if ((MemberType.user_group == cmp.getType()) && bOwnershipChangeRequested) {
									// Not allowed to set a user group to be the owner (obv)
									rp.setResponse(new ResponseObject("Update member type",false,"Can't set a user group to be the owner"));
									return rp;									
								}//TESTED
								
								cmp.setUserType(userType);	
								personDisplayName = cmp.getDisplayName();
								bChangedMembership = true;
								
								if (bChangedOwnership) { // (includes case where didn't need to)
									break;
								}
								
							}//TESTED 
							if (bOwnershipChangeRequested && cmp.get_id().equals(cp.getOwnerId())) {
								cmp.setUserType("moderator");
								bChangedOwnership = true;
								
								if (bChangedMembership) {
									break;
								}
								
							}//TESTED
						}
						if (bChangedMembership) {
							if (bOwnershipChangeRequested) {
								cp.setOwnerId(personId);
								cp.setOwnerDisplayName(personDisplayName);
							}//TESTED
							
							/////////////////////////////////////////////////////////////////////////////////////////////////
							// TODO (INF-1214): Make this code more robust to handle changes to the community that need to
							// Caleb: this means change update to $set
							/////////////////////////////////////////////////////////////////////////////////////////////////
							DbManager.getSocial().getCommunity().update(query, cp.toDb());
							rp.setResponse(new ResponseObject("Update member type",true,"Updated member type successfully"));
						}//TESTED									
					}
					else
					{
						rp.setResponse(new ResponseObject("Update member type",false,"User was not a member of the community"));
					}
				}
				else
				{
					rp.setResponse(new ResponseObject("Update member type",false,"Caller must be admin/owner/moderator (unless changing ownership)"));
				}
			}
			else
			{
				rp.setResponse(new ResponseObject("Update member type",false,"Community does not exist"));
			}			
		}
		catch(Exception ex)
		{
			rp.setResponse(new ResponseObject("Update member type",false, Globals.populateStackTrace(new StringBuffer("General Error, bad params maybe? "), ex).toString()));
		}
		return rp;
	}//TESTED (see sub-clauses for details)

	/**
	 * joinCommunity (REST)
	 */
	public ResponsePojo joinCommunity(String personIdStr, String communityIdStr, CommunityPojo.CommunityType communityType) 
	{
		boolean isSysAdmin = RESTTools.adminLookup(personIdStr);
		return joinCommunity(personIdStr, communityIdStr, isSysAdmin, communityType);
	}
	
	public ResponsePojo joinCommunity(String personIdStr, String communityIdStr, boolean isSysAdmin, CommunityPojo.CommunityType communityType) 
	{		
		ResponsePojo rp = new ResponsePojo();
		try
		{			
			communityIdStr = allowCommunityRegex(personIdStr, communityIdStr);
			BasicDBObject query = new BasicDBObject("_id",new ObjectId(communityIdStr));
			DBObject dboComm = DbManager.getSocial().getCommunity().findOne(query);
			if ( dboComm != null )
			{
				CommunityPojo cp = CommunityPojo.fromDb(dboComm, CommunityPojo.class);
				if ( !cp.getIsPersonalCommunity() )
				{
					BasicDBObject queryPerson = new BasicDBObject("_id",new ObjectId(personIdStr));
					DBObject dboPerson = DbManager.getSocial().getPerson().findOne(queryPerson);
					ObjectId personOrUserGroupId = null;
					PersonPojo pp = null;
					CommunityPojo userGroup = null;
					if (null == dboPerson) {							
						userGroup = CommunityPojo.fromDb(DbManager.getSocial().getCommunity().findOne(new BasicDBObject("_id", new ObjectId(personIdStr))), CommunityPojo.class);
						if (null != userGroup)
							personOrUserGroupId = userGroup.getId();
						
						if ((null != userGroup) && (CommunityType.user != userGroup.getType())) {
							rp.setResponse(new ResponseObject("Join member to community", false, "Can't add data groups as members."));
							return rp;								
						}
						if ((null != userGroup) && (CommunityType.user == cp.getType())) {
							rp.setResponse(new ResponseObject("Join member to community", false, "Can't add user groups to user groups."));
							return rp;															
						}
					}
					else {
						pp = PersonPojo.fromDb(dboPerson,PersonPojo.class);
						if (null != pp)
							personOrUserGroupId = pp.get_id();
					}
					if ((null == pp) && (null == userGroup)) {
						rp.setResponse(new ResponseObject("Join community", false, "Can't find user or user group."));
						return rp;														
					}
					
					boolean isPending = isMemberPending(cp, personOrUserGroupId);
					
					if ( !cp.isMember(new ObjectId(personIdStr)) || isPending )
					{
						Map<String,CommunityAttributePojo> commatt = cp.getCommunityAttributes();
						if ( isSysAdmin || (commatt.containsKey("usersCanSelfRegister") && commatt.get("usersCanSelfRegister").getValue().equals("true") ) || isPending )
						{		
							boolean requiresApproval = false;
							if ( !isSysAdmin && !isPending && commatt.containsKey("registrationRequiresApproval") )
								requiresApproval = commatt.get("registrationRequiresApproval").getValue().equals("true");
							//if approval is required, add user to comm, wait for owner to approve
							//otherwise go ahead and add as a member
							if ( requiresApproval )
							{
								cp.addMember(personOrUserGroupId, pp, userGroup, true);
								//write both objects back to db now
								/////////////////////////////////////////////////////////////////////////////////////////////////
								// TODO (INF-1214): Make this code more robust to handle changes to the community that need to
								// Caleb: this means change update to $set
								/////////////////////////////////////////////////////////////////////////////////////////////////
								DbManager.getSocial().getCommunity().update(query, cp.toDb());
															
								//send email out to owner for approval
								CommunityApprovePojo cap = cp.createCommunityApprove(RESTTools.generateRandomId(), personIdStr,communityIdStr,"join",personIdStr);
								DbManager.getSocial().getCommunityApprove().insert(cap.toDb());
								
								// Get to addresses for Owner and Moderators
								String toAddresses = getToAddressesFromCommunity(cp);
								
								PropertiesManager propManager = new PropertiesManager();
								String rootUrl = propManager.getUrlRoot();
								
								String displayName = (null != pp) ? pp.getDisplayName() : userGroup.getName();
								
								String subject = displayName + " is trying to join infinit.e community: " + cp.getName();
								String body = displayName + " is trying to join infinit.e community: " + cp.getName() + "<br/>Do you want to accept this request?" +
								"<br/><a href=\"" + rootUrl + "social/community/requestresponse/"+cap.get_id().toString() + "/true\">Accept</a> " +
								"<a href=\"" + rootUrl + "social/community/requestresponse/"+cap.get_id().toString() + "/false\">Deny</a>"; 
								
								SendMail mail = new SendMail(new PropertiesManager().getAdminEmailAddress(), toAddresses, subject, body);
								
								if (mail.send("text/html"))
								{
									rp.setResponse(new ResponseObject("Join Community",true,"Joined community successfully, awaiting owner approval"));
									rp.setData(new CommunityApprovalPojo(false));
								}
								else
								{
									rp.setResponse(new ResponseObject("Join Community",false,"The system was uable to send an email to the owner"));								
								}
							}
							else
							{
								cp.addMember(personOrUserGroupId, pp, userGroup);
								if (null != pp)
									pp.addCommunity(cp);
								//write both objects back to db now
								/////////////////////////////////////////////////////////////////////////////////////////////////
								// TODO (INF-1214): Make this code more robust to handle changes to the community that need to
								// Caleb: this means change update to $set
								/////////////////////////////////////////////////////////////////////////////////////////////////
								DbManager.getSocial().getCommunity().update(query, cp.toDb());
								if (null != pp)
									DbManager.getSocial().getPerson().update(queryPerson, pp.toDb());
								rp.setResponse(new ResponseObject("Join Community",true,"Joined community successfully"));
								rp.setData(new CommunityApprovalPojo(true));
							}						
						}
						else
						{
							rp.setResponse(new ResponseObject("Join Community",false,"You must be invited to this community"));
						}
					}
					else
					{
						rp.setResponse(new ResponseObject("Join Community",false,"You are already a member of this community"));
					}
				}
				else
				{
					rp.setResponse(new ResponseObject("Join Community",false,"Cannot add members to personal community"));
				}
			}
			else
			{
				rp.setResponse(new ResponseObject("Join Community",false,"Community does not exist"));
			}
		}
		catch(Exception ex)
		{
			
			rp.setResponse(new ResponseObject("Join Community",false, Globals.populateStackTrace(new StringBuffer("General Error, bad params maybe? "), ex).toString()));
		}
		return rp;
	}
	
	/**
	 * leaveCommunity (REST)
	 */
	
	/**/
	//TODO: can this be called for user groups?
	
	public ResponsePojo leaveCommunity(String personIdStr, String communityIdStr, CommunityPojo.CommunityType communityType) 
	{
		ResponsePojo rp = new ResponsePojo();
		try
		{
			communityIdStr = allowCommunityRegex(personIdStr, communityIdStr);
			BasicDBObject query = new BasicDBObject("_id",new ObjectId(communityIdStr));
			DBObject dboComm = DbManager.getSocial().getCommunity().findOne(query);
			if ( dboComm != null )
			{
				CommunityPojo cp = CommunityPojo.fromDb(dboComm, CommunityPojo.class);
				if ( !cp.getIsPersonalCommunity())
				{
					BasicDBObject queryPerson = new BasicDBObject("_id",new ObjectId(personIdStr));
					DBObject dboPerson = DbManager.getSocial().getPerson().findOne(queryPerson);
					PersonPojo pp = PersonPojo.fromDb(dboPerson,PersonPojo.class);
					cp.removeMember(new ObjectId(personIdStr));
					pp.removeCommunity(cp);					
					//write both objects back to db now
					/////////////////////////////////////////////////////////////////////////////////////////////////
					// TODO (INF-1214): Make this code more robust to handle changes to the community that need to
					// Caleb: this means change update to $set
					/////////////////////////////////////////////////////////////////////////////////////////////////
					DbManager.getSocial().getCommunity().update(query, cp.toDb());
					DbManager.getSocial().getPerson().update(queryPerson, pp.toDb());
					rp.setResponse(new ResponseObject("Leave Community",true,"Left community successfully"));
				}
				else
				{
					rp.setResponse(new ResponseObject("Leave Community",false,"Cannot leave personal community"));
				}
			}
			else
			{
				rp.setResponse(new ResponseObject("Leave Community",false,"Community does not exist"));
			}
		}
		catch(Exception ex)
		{
			rp.setResponse(new ResponseObject("Leave Community",false, Globals.populateStackTrace(new StringBuffer("General Error, bad params maybe? "), ex).toString()));
		}
		return rp;
	}


	/**
	 * inviteCommunity (REST)
	 * Invite a user to a community, only add them as pending to community
	 * and do not add community into the person object yet
	 * Need to send email out
	// (Note supports personId as either Id or username (email) both are unique indexes)
	 * @param personIdStr
	 * @param userIdStr
	 * @param communityIdStr
	 * @return
	 */
	public ResponsePojo inviteCommunity(String userIdStr, String personIdStr, String communityIdStr, String skipInvitation, CommunityPojo.CommunityType communityType) 
	{
		ResponsePojo rp = new ResponsePojo();
		try {
			communityIdStr = allowCommunityRegex(userIdStr, communityIdStr);
		}
		catch (Exception e) {
			rp.setResponse(new ResponseObject("Invite Community", false, "Error returning community info: " + e.getMessage()));
			return rp;
		}
		
		boolean skipInvite = ((null != skipInvitation) && (skipInvitation.equalsIgnoreCase("true"))) ? true : false;
		
		/////////////////////////////////////////////////////////////////////////////////////////////////
		// Note: Only Sys Admins, Community Owner, and Community Moderators can invite users to
		// private communities however any member can be able to invite someone to a public community
		boolean isOwnerOrModerator = SocialUtils.isOwnerOrModerator(communityIdStr, userIdStr);
		boolean isSysAdmin = RESTTools.adminLookup(userIdStr);
		boolean canInvite = (isOwnerOrModerator || isSysAdmin) ? true : false;

		try
		{
			BasicDBObject query = new BasicDBObject("_id",new ObjectId(communityIdStr));
			DBObject dboComm = DbManager.getSocial().getCommunity().findOne(query);
			
			if ( dboComm != null )
			{
				CommunityPojo cp = CommunityPojo.fromDb(dboComm, CommunityPojo.class);
				
				// Make sure this isn't a personal community
				if ( !cp.getIsPersonalCommunity() )
				{
					// Check to see if the user has permissions to invite or selfregister
					boolean selfRegister = canSelfRegister(cp);
					if ( canInvite || cp.getOwnerId().toString().equalsIgnoreCase(userIdStr) || selfRegister )
					{
						BasicDBObject dboPerson = (BasicDBObject) DbManager.getSocial().getPerson().findOne(new BasicDBObject("email", personIdStr));
						if (null == dboPerson) { // (ie personId isn't an email address... convert to ObjectId and try again)
							dboPerson = (BasicDBObject) DbManager.getSocial().getPerson().findOne(new BasicDBObject("_id", new ObjectId(personIdStr)));
						}
						else {
							personIdStr = dboPerson.getString("_id"); 
						}
						ObjectId personOrUserGroupId = null;
						PersonPojo pp = null;
						CommunityPojo userGroup = null;
						if (null == dboPerson) {							
							userGroup = CommunityPojo.fromDb(DbManager.getSocial().getCommunity().findOne(new BasicDBObject("_id", new ObjectId(personIdStr))), CommunityPojo.class);
							if (null != userGroup)
								personOrUserGroupId = userGroup.getId();
							
							if ((null != userGroup) && (CommunityType.user != userGroup.getType())) {
								rp.setResponse(new ResponseObject("Join member to community", false, "Can't add data groups as members."));
								return rp;								
							}
							if ((null != userGroup) && (CommunityType.user == cp.getType())) {
								rp.setResponse(new ResponseObject("Join member to community", false, "Can't add user groups to user groups."));
								return rp;															
							}
						}
						else {
							pp = PersonPojo.fromDb(dboPerson,PersonPojo.class);
							if (null != pp)
								personOrUserGroupId = pp.get_id();							
						}
						// OK from here on, personIdStr is the object Id...
						
						if ( (pp != null) || (userGroup != null) )
						{
							//need to check for if a person is pending, and skipInvite and isSysAdmin, otherwise
							//they would just get sent an email again, so leave it be
							boolean isPending = false;
							if ( isSysAdmin && skipInvite )
							{
								isPending = isMemberPending(cp, personOrUserGroupId);
							}
							
							if ( selfRegister )
							{
								//If the comm allows for self registering, just call join community
								//instead of invite, it will handle registration
								return this.joinCommunity(personOrUserGroupId.toString(), communityIdStr, isSysAdmin, communityType);
							}
							else if ( !cp.isMember(personOrUserGroupId) || isPending )
							{
								if (isSysAdmin && skipInvite) // Can only skip invite if user is Admin
								{
									// Update community with new member
									cp.addMember(personOrUserGroupId, pp, userGroup, false); // Member status set to Active
									/////////////////////////////////////////////////////////////////////////////////////////////////
									// TODO (INF-1214): Make this code more robust to handle changes to the community that need to
									// Caleb: this means change update to $set
									/////////////////////////////////////////////////////////////////////////////////////////////////
									DbManager.getSocial().getCommunity().update(query, cp.toDb());
									
									/////////////////////////////////////////////////////////////////////////////////////////////////
									// TODO (INF-1214): Make this code more robust to handle changes to the community that need to
									// Caleb: this means change update to $set
									/////////////////////////////////////////////////////////////////////////////////////////////////
									if (null != pp) {
										// Add community to persons object and save to db
										pp.addCommunity(cp);
										
										DbManager.getSocial().getPerson().update(new BasicDBObject("_id", pp.get_id()), pp.toDb());
										rp.setResponse(new ResponseObject("Invite Community",true,"User added to community successfully."));
									}
									else {
										//TODO (INF-2866): might need to do some person manipulation
										rp.setResponse(new ResponseObject("Invite Community",true,"User group added to community successfully."));
									}
								}
								else
								{
									cp.addMember(personOrUserGroupId, pp, userGroup, true); // Member status set to Pending
									/////////////////////////////////////////////////////////////////////////////////////////////////
									// TODO (INF-1214): Make this code more robust to handle changes to the community that need to
									// Caleb: this means change update to $set
									/////////////////////////////////////////////////////////////////////////////////////////////////
									DbManager.getSocial().getCommunity().update(query, cp.toDb());
									
									//send email out inviting user
									CommunityApprovePojo cap = cp.createCommunityApprove(RESTTools.generateRandomId(), personIdStr,communityIdStr,"invite",userIdStr);
									DbManager.getSocial().getCommunityApprove().insert(cap.toDb());
									
									PropertiesManager propManager = new PropertiesManager();
									String rootUrl = propManager.getUrlRoot();
									
									if (null == rootUrl) {
										rp.setResponse(new ResponseObject("Invite Community",false,"The system was unable to email the invite because an invite was required and root.url is not set up."));
										return rp;
									}
									
									String subject = "Invite to join infinit.e community: " + cp.getName();
									String body = "You have been invited to join the community " + cp.getName() + 
										"<br/><a href=\"" + rootUrl + "social/community/requestresponse/"+cap.get_id().toString() + "/true\">Accept</a> " +
										"<a href=\"" + rootUrl + "social/community/requestresponse/"+cap.get_id().toString() + "/false\">Deny</a>"; 
														
									SendMail mail = null;
									if (null != pp) {
										mail = new SendMail(new PropertiesManager().getAdminEmailAddress(), pp.getEmail(), subject, body);
									}
									else {
										mail = new SendMail(new PropertiesManager().getAdminEmailAddress(), getToAddressesFromCommunity(userGroup), subject, body);
									}
									
									if (mail.send("text/html"))
									{
										if (isSysAdmin) {
											if (null == pp) 
												rp.setResponse(new ResponseObject("Invite Community",true,"Invited user to community successfully: " + cap.get_id().toString()));
											else
												rp.setResponse(new ResponseObject("Invite Community",true,"Invited user group to community successfully: " + cap.get_id().toString()));
										}
										else {
											if (null == pp) 
												rp.setResponse(new ResponseObject("Invite Community",true,"Invited user to community successfully"));
											else
												rp.setResponse(new ResponseObject("Invite Community",true,"Invited user group to community successfully"));
										}
									}
									else
									{
										rp.setResponse(new ResponseObject("Invite Community",false,"The system was unable to email the invite for an unknown reason (eg an invite was required and the mail server is not setup)."));
									}
								}
							}
							else
							{								
								//otherwise just return we failed
								rp.setResponse(new ResponseObject("Invite Community",false,"The user is already a member of this community."));
							}
						}
						else
						{
							rp.setResponse(new ResponseObject("Invite Community",false,"Person/User Group does not exist"));
						}
					}
					else
					{
						rp.setResponse(new ResponseObject("Invite Community",false,"You must be owner to invite other members, if you received an invite, you must accept it through that"));
					}
				}
				else
				{
					rp.setResponse(new ResponseObject("Invite Community",false,"Cannot invite members to personal community"));
				}
			}
			else
			{
				rp.setResponse(new ResponseObject("Invite Community",false,"Community does not exist"));
			}
		}
		catch(Exception ex)
		{
			rp.setResponse(new ResponseObject("Invite Community",false, Globals.populateStackTrace(new StringBuffer("General Error, bad params maybe? "), ex).toString()));
		}
		return rp;
	}
	
	/**
	 * Returns true if users can self register to this community
	 * 
	 * @param cp
	 * @return
	 */
	private boolean canSelfRegister(CommunityPojo cp)
	{
		if ( cp != null )
		{
			if ( cp.getCommunityAttributes().containsKey("usersCanSelfRegister") && 
					cp.getCommunityAttributes().get("usersCanSelfRegister").getValue().equals("true"))
			{
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Returns true if the given member is pending in the given community
	 * 
	 * @return
	 */
	private boolean isMemberPending( CommunityPojo cp, ObjectId personOrUserGroupId)
	{									
		for ( CommunityMemberPojo cmp : cp.getMembers() )
		{
			if ( cmp.get_id().equals(personOrUserGroupId) )
			{
				if ( cmp.getUserStatus().equals("pending") )
				{
					//found the user, and his status is pending
					return true;
				}
				return false;
			}
		}
		return false;
		//TESTED only finds members that are pending while sysadmin
	}


	/**
	 * requestResponse (REST)
	 * @param requestIdStr
	 * @param resp
	 * @return
	 */
	public ResponsePojo requestResponse(String requestIdStr, String resp, CommunityPojo.CommunityType communityType) 
	{
		ResponsePojo rp = new ResponsePojo();
		try
		{
			// Check for valid text response value
			if ( resp.equals("true") || resp.equals("false") )
			{
				// Attempt to retrieve the invite from the social.communityapprove collection
				DBObject dbo = DbManager.getSocial().getCommunityApprove().findOne(new BasicDBObject("_id", new ObjectId(requestIdStr)));
				
				if (dbo != null )
				{
					CommunityApprovePojo cap = CommunityApprovePojo.fromDb(dbo, CommunityApprovePojo.class);
					if ( cap.getType().equals("source"))
					{
						//approving a source
						if ( resp.equals("true"))
						{
							rp = sourceHandler.approveSource(cap.getSourceId(), cap.getCommunityId(), cap.getRequesterId());
						}
						else
						{
							rp = sourceHandler.denySource(cap.getSourceId(), cap.getCommunityId(), cap.getRequesterId());
						}
						if ( rp.getResponse().isSuccess() )
						{
							//remove request object now
							DbManager.getSocial().getCommunityApprove().remove(new BasicDBObject("_id",new ObjectId(requestIdStr)));
						}
					}
					else
					{
						//approving a user joining a community
						BasicDBObject query = new BasicDBObject("_id",new ObjectId(cap.getCommunityId()));
						DBObject dboComm = DbManager.getSocial().getCommunity().findOne(query);
						
						//get user
						BasicDBObject queryPerson = new BasicDBObject("_id",new ObjectId(cap.getPersonId()));
						DBObject dboperson = DbManager.getSocial().getPerson().findOne(queryPerson);
						
						// handle user vs user group:
						ObjectId personOrUserGroupId = null;
						PersonPojo pp = null;
						CommunityPojo userGroup = null;
						if (null == dboperson) {							
							userGroup = CommunityPojo.fromDb(DbManager.getSocial().getCommunity().findOne(new BasicDBObject("_id", new ObjectId(cap.getPersonId()))), CommunityPojo.class);
							if (null != userGroup)
								personOrUserGroupId = userGroup.getId();
							
							//(must be a user group etc etc if we got this far)
						}
						else {
							pp = PersonPojo.fromDb(dboperson,PersonPojo.class);
							if (null != pp)
								personOrUserGroupId = pp.get_id();
						}
						if ((null == pp) && (null == userGroup)) {
							rp.setResponse(new ResponseObject("Request Response",false,"The user or user group does not exist."));
							return rp;
						}						
						else if ( dboComm != null )
						{
							CommunityPojo cp = CommunityPojo.fromDb(dboComm, CommunityPojo.class);
							boolean isStillPending = isMemberPending(cp, personOrUserGroupId);
							//make sure the user is still waiting to join the community, otherwise remove this request and return
							if ( isStillPending )
							{
								if ( resp.equals("false"))
								{
									//if response is false (deny), always just remove user from community							
									cp.removeMember(new ObjectId(cap.getPersonId()));
									/////////////////////////////////////////////////////////////////////////////////////////////////
									// TODO (INF-1214): Make this code more robust to handle changes to the community that need to
									// Caleb: this means change update to $set
									/////////////////////////////////////////////////////////////////////////////////////////////////
									DbManager.getSocial().getCommunity().update(query, cp.toDb());
								}
								else
								{
									//if response is true (allow), always just add community info to user, and change status to active
									
									cp.updateMemberStatus(cap.getPersonId(), "active");
									/////////////////////////////////////////////////////////////////////////////////////////////////
									// TODO (INF-1214): Make this code more robust to handle changes to the community that need to
									// Caleb: this means change update to $set
									/////////////////////////////////////////////////////////////////////////////////////////////////
									DbManager.getSocial().getCommunity().update(query, cp.toDb());
									
									if (null != pp) {
										pp.addCommunity(cp);
										/////////////////////////////////////////////////////////////////////////////////////////////////
										// TODO (INF-1214): Make this code more robust to handle changes to the community that need to
										// Caleb: this means change update to $set
										/////////////////////////////////////////////////////////////////////////////////////////////////
										DbManager.getSocial().getPerson().update(queryPerson, pp.toDb());
									}
									else {
										//TODO (INF-2866): handle user group level updating
									}
								}
								//return successfully
								rp.setResponse(new ResponseObject("Request Response",true,"Request answered successfully!"));
							}
							else
							{
								//return fail
								rp.setResponse(new ResponseObject("Request Response",false,"Request has already been answered!"));
							}
							//remove request object now
							DbManager.getSocial().getCommunityApprove().remove(new BasicDBObject("_id",new ObjectId(requestIdStr)));
							
						}
						else
						{
							rp.setResponse(new ResponseObject("Request Response",false,"The community does not exist."));
						}
					}
				}
				else
				{
					rp.setResponse(new ResponseObject("Request Response",false,"This request does not exist, possibly answered already?"));
				}
			}
			else
			{
				rp.setResponse(new ResponseObject("Request Response",false,"Reponse must be true or false"));
			}
		}
		catch(Exception ex)
		{
			rp.setResponse(new ResponseObject("Request Response",false, Globals.populateStackTrace(new StringBuffer("General Error, bad params maybe? "), ex).toString()));
		}
		return rp;
	}
	

	/**
	 * updateCommunity (REST)
	 * Updates a community found with communityId with fields from updateItem (communitypojo in json form)
	 * Must be owner of communityId to update
	 * @param userIdStr
	 * @param communityIdStr
	 * @param json
	 * @param returnCommunity 
	 * @return
	 */
	public ResponsePojo updateCommunity(String userIdStr, String communityIdStr, String json, CommunityPojo.CommunityType communityType, boolean returnCommunity) 
	{
		ResponsePojo rp = new ResponsePojo();
		try {
			communityIdStr = allowCommunityRegex(userIdStr, communityIdStr);
		}
		catch (Exception e) {
			rp.setResponse(new ResponseObject("Update Community", false, "Error returning community info: " + e.getMessage()));
			return rp;
		}
		
		/////////////////////////////////////////////////////////////////////////////////////////////////
		// Note: Only Sys Admins, Community Owner, and Community Moderators can add update communities
		boolean isOwnerOrModerator = SocialUtils.isOwnerOrModerator(communityIdStr, userIdStr);
		boolean isSysAdmin = RESTTools.adminLookup(userIdStr);
		boolean canUpdate = (isOwnerOrModerator || isSysAdmin) ? true : false;	
		if (!canUpdate)
		{
			rp.setResponse(new ResponseObject("Update Community",false,"User does not have permission to update the community."));
			return rp;
		}
		
		/////////////////////////////////////////////////////////////////////////////////////////////////
		// Attempt to parse the JSON passed in to a CommunityPojo
		CommunityPojo updateCommunity = null;
		try
		{
			updateCommunity = ApiManager.mapFromApi(json, CommunityPojo.class, new CommunityPojoApiMap());
		}
		catch (Exception ex)
		{
			rp.setResponse(new ResponseObject("Update Community",false,"Update json is badly formatted, could not deserialize."));
			return rp;
		}
		
		try
		{
			// Retrieve community we are trying to update from the database
			BasicDBObject query = new BasicDBObject("_id", new ObjectId(communityIdStr));
			DBObject dbo = DbManager.getSocial().getCommunity().findOne(query);
			String originalName = null;
			
			if ( dbo != null )
			{
				
				CommunityPojo cp = CommunityPojo.fromDb(dbo, CommunityPojo.class);
				
				if (null == cp) {
					rp.setResponse(new ResponseObject("Update Community",false,"Community to update does not exist"));
					return rp;
				}
				// (not allowed to change community type, too complex)
				if (cp.getType() != updateCommunity.getType()) {
					rp.setResponse(new ResponseObject("Update Community",false,"Can't update community type, currently: " + cp.getType().toString()));
					return rp;					
				}
				
				// Here are the fields you are allowed to change:
				// name:
				if (null != updateCommunity.getName()) 
				{
					// If you're changing name then ensure it's unique for consistency
					//TODO (INF-1214): see addCommunity, this is currently something of a security hole
					BasicDBObject nameCheck = new BasicDBObject("name", updateCommunity.getName());
					nameCheck.put("_id", new BasicDBObject(MongoDbManager.ne_, cp.getId()));
					if (null != MongoDbManager.getSocial().getCommunity().findOne(nameCheck)) {
						rp.setResponse(new ResponseObject("Update Community",false,"Can't change name to an existing community"));
						return rp;						
					}//TESTED (tested changing names of existing community works...)		
					originalName = cp.getName();
					cp.setName(updateCommunity.getName());
				}
				if (null != updateCommunity.getDescription()) {
					cp.setDescription(updateCommunity.getDescription());					
				}
				if (null != updateCommunity.getTags()) {
					cp.setTags(updateCommunity.getTags());					
				}
				int new_num_shards = -1;
				if ((null != updateCommunity.getCommunityAttributes() && !updateCommunity.getCommunityAttributes().isEmpty()))
				{
					//handle shard logic
					int prev_num_shards = 5;
					if ( cp.getCommunityAttributes().containsKey(CommunityAttributePojo.NUM_SHARDS_ATTRIBUTE) )
					{
						prev_num_shards = Integer.parseInt( cp.getCommunityAttributes().get(CommunityAttributePojo.NUM_SHARDS_ATTRIBUTE).getValue() );
					}
					new_num_shards = 5;
					if ( updateCommunity.getCommunityAttributes().containsKey(CommunityAttributePojo.NUM_SHARDS_ATTRIBUTE) )
					{
						new_num_shards = Integer.parseInt( updateCommunity.getCommunityAttributes().get(CommunityAttributePojo.NUM_SHARDS_ATTRIBUTE).getValue() );
					}
					if ( new_num_shards != prev_num_shards )
					{
						if (prev_num_shards >= 0 )
						{
							rp.setResponse(new ResponseObject("Update Community", false, "Can't change num_shards param from a positive value to anything else (can't update shard count once an index exists)"));
							return rp;
						}
					}
					else
					{
						new_num_shards = -1;
					}
					
					//merge with existing (overwriting any changes) this way we keep the defaults
					cp.getCommunityAttributes().putAll(updateCommunity.getCommunityAttributes());									
				}
				if ((null != updateCommunity.getCommunityUserAttribute() && !updateCommunity.getCommunityUserAttribute().isEmpty()))
				{
					//merge with existing (overwriting any changes) this way we keep the defaults
					cp.getCommunityUserAttribute().putAll(updateCommunity.getCommunityUserAttribute());					
				}
				// Change owner: not allowed here, use community/update/status
				if ((null != updateCommunity.getOwnerId()) && !updateCommunity.getOwnerId().equals(cp.getOwnerId()))
				{
					rp.setResponse(new ResponseObject("Update Community",false,"Use community/update/status to change ownership"));
					return rp;
				}//TESTED
								
				DbManager.getSocial().getCommunity().update(query, cp.toDb());
								
				// Community name has changed, member records need to be updated to reflect the name change
				if (originalName != null)
				{
					DBObject query_person = new BasicDBObject("communities.name", originalName);
					DBObject update_person = new BasicDBObject("$set",new BasicDBObject("communities.$.name", updateCommunity.getName()));					
					DbManager.getSocial().getPerson().update(query_person, update_person, false, true);
					
					//Also need to update share community names to reflect the name change
					DBObject query_share = new BasicDBObject("communities.name", originalName);
					DBObject update_share = new BasicDBObject("$set",new BasicDBObject("communities.$.name", updateCommunity.getName()));					
					DbManager.getSocial().getShare().update(query_share, update_share, false, true);
				}
				
				if ( new_num_shards == 0 )
					new_num_shards = GenericProcessingController.DEFAULT_NUM_SHARDS;
				if ( new_num_shards > 0 )
				{
					try {
						GenericProcessingController.createCommunityDocIndex(cp.getId().toString(), cp.getParentId(), cp.getIsPersonalCommunity(), cp.getIsSystemCommunity(), false, new_num_shards);
					}
					catch (Exception e) { // Can't create community
						rp.setResponse(new ResponseObject("Update Community", false, "Community updated successfully, error creating index: " + e.getMessage()));
						return rp;
					}
				}
				
				
				/////////////////////////////////////////////////////////////////////////////////////////////////
				// TODO (INF-1214): Make this code more robust to handle changes to the community that need to 
				// propagate out to other records like Person
				// caleb note: 1/7 (change this to use $set is what this means, 
				// including above DbManager.getSocial().getCommunity().update(query, cp.toDb()); )
				// and the below unwritten communityuserattri
				/////////////////////////////////////////////////////////////////////////////////////////////////
				/////////////////////////////////////////////////////////////////////////////////////////////////
				// Community.communityUserAttribute
				// If user attributes have changed we might need to update member records...

				
				rp.setResponse(new ResponseObject("Update Community", true, "Community updated successfully."));
				if ( returnCommunity ) {
					rp = getCommunity(userIdStr, communityIdStr, false, communityType);
				}
			}
			else
			{
				rp.setResponse(new ResponseObject("Update Community",false,"Community does not exist"));
			}
		}
		catch (Exception ex)
		{
			rp.setResponse(new ResponseObject("Update Community",false,"Unable to update community. Error:" + ex.getMessage()));
		}
		return rp;
	}
	
	//////////////////////////////////////////////////////////////////////////
	//////////////////////// Helper Functions ////////////////////////////////
	//////////////////////////////////////////////////////////////////////////

	/**
	 * addCommunityMember (only called internally and by PersonHandler)
	 * @param communityIdStr
	 * @param personIdStr
	 * @param email
	 * @param displayName
	 * @param userType
	 * @param userStatus
	 * @return
	 */
	private ResponsePojo addCommunityMember(String userIdStr, String communityIdStr, String communityName,
			String personIdStr, String email, String displayName, String userType, String userStatus)
	{
		return addCommunityMember(userIdStr,communityIdStr,communityName,personIdStr,email,displayName,userType,userStatus,false);
	}
	
	public ResponsePojo addCommunityMember(String userIdStr, String communityIdStr, String communityName,
			String personIdStr, String email, String displayName, String userType, String userStatus, boolean override)
	{
		/////////////////////////////////////////////////////////////////////////////////////////////////
		// Note: Only Sys Admins, Community Owner, and Community Moderators can add users to communities
		boolean isOwnerOrModerator = SocialUtils.isOwnerOrModerator(communityIdStr, userIdStr);
		boolean isSysAdmin = RESTTools.adminLookup(userIdStr);
		boolean canAdd = (isOwnerOrModerator || isSysAdmin || override) ? true : false;
		
		ResponsePojo rp = new ResponsePojo();
		
		if (canAdd)
		{
			try
			{
				// Find person record to update
				BasicDBObject query = new BasicDBObject();
				query.put("_id", new ObjectId(communityIdStr));

				DBObject dbo = DbManager.getSocial().getCommunity().findOne(query);

				if (dbo != null) 
				{
					// Get GsonBuilder object with MongoDb de/serializers registered
					CommunityPojo community = CommunityPojo.fromDb(dbo, CommunityPojo.class);

					// Get the list of existing members, check to see if user is already 
					// a member of the community, make sure CommunityMember obj isn't null/empty
					Boolean alreadyMember = false;				
					Set<CommunityMemberPojo> cmps = null;
					if (community.getMembers() != null)
					{
						cmps = community.getMembers();
						for (CommunityMemberPojo c : cmps)
						{
							if (c.get_id().toString().equals(personIdStr)) alreadyMember = true;
						}
					}
					else
					{ 
						cmps = new HashSet<CommunityMemberPojo>();
						community.setMembers(cmps);
					}

					if (!alreadyMember)
					{
						// Note: This changes community owner
						if (userType.equals("owner"))
						{
							community.setOwnerId(new ObjectId(personIdStr));
							community.setOwnerDisplayName(displayName);
						}

						// Create the new member object
						CommunityMemberPojo cmp = new CommunityMemberPojo();
						cmp.set_id(new ObjectId(personIdStr));
						// (These all come from API - so applies to both users and user groups)
						cmp.setEmail(email);
						cmp.setDisplayName(displayName);
						cmp.setUserStatus(userStatus);
						cmp.setUserType(userType);

						// Set the userAttributes based on default
						Set<CommunityMemberUserAttributePojo> cmua = new HashSet<CommunityMemberUserAttributePojo>();
						Map<String,CommunityUserAttributePojo> cua = community.getCommunityUserAttribute();

						Iterator<Map.Entry<String,CommunityUserAttributePojo>> it = cua.entrySet().iterator();
						while (it.hasNext())
						{
							CommunityMemberUserAttributePojo c = new CommunityMemberUserAttributePojo();
							Map.Entry<String,CommunityUserAttributePojo> pair = it.next();
							c.setType(pair.getKey().toString());
							CommunityUserAttributePojo v = (CommunityUserAttributePojo)pair.getValue();
							c.setValue(v.getDefaultValue());
							cmua.add(c);
						}
						cmp.setUserAttributes(cmua);

						// Get Person data to added to member record
						BasicDBObject query2 = new BasicDBObject();
						query2.put("_id", new ObjectId(personIdStr));
						DBObject dbo2 = DbManager.getSocial().getPerson().findOne(query2);
						PersonPojo p = PersonPojo.fromDb(dbo2, PersonPojo.class);

						if (null == p) {
							// This could be a user group instead ... note can't add user groups to user groups... 
							CommunityPojo userGroup = CommunityPojo.fromDb(DbManager.getSocial().getCommunity().findOne(new BasicDBObject("_id", cmp.get_id())), CommunityPojo.class);
							if ((null != userGroup) && (CommunityType.user != userGroup.getType())) {
								rp.setResponse(new ResponseObject("Add member to community", false, "Can't add data groups as members."));
								return rp;								
							}
							if (null == userGroup) {
								rp.setResponse(new ResponseObject("Add member to community", false, "User/User Group not found."));
								return rp;
							}
							else if (CommunityPojo.CommunityType.user == community.getType()) {
								rp.setResponse(new ResponseObject("Add member to community", false, "Can't add user group to user group."));
								return rp;								
							}
							else {
								//TODO (INF-2866): If doing it that way, then add community to all members 
							}
							cmp.setType(MemberType.user_group);
							
						}//TESTED
						else {
							if (p.getContacts() != null)
							{
								// Set contacts from person record
								Set<CommunityMemberContactPojo> contacts = new HashSet<CommunityMemberContactPojo>();
								Map<String, PersonContactPojo> pcp = p.getContacts();
								Iterator<Map.Entry<String, PersonContactPojo>> it2 = pcp.entrySet().iterator();
								while (it2.hasNext())
								{
									CommunityMemberContactPojo c = new CommunityMemberContactPojo();
									Map.Entry<String, PersonContactPojo> pair = it2.next();
									c.setType(pair.getKey().toString());
									PersonContactPojo v = (PersonContactPojo)pair.getValue();
									c.setValue(v.getValue());
									contacts.add(c);
								}
								cmp.setContacts(contacts);
							}
	
							// Set links from person record
							if (p.getLinks() != null)
							{
								// Set contacts from person record
								Set<CommunityMemberLinkPojo> links = new HashSet<CommunityMemberLinkPojo>();
								Map<String, PersonLinkPojo> plp = p.getLinks();
								Iterator<Map.Entry<String, PersonLinkPojo>> it3 = plp.entrySet().iterator();
								while (it.hasNext())
								{
									CommunityMemberLinkPojo c = new CommunityMemberLinkPojo();
									Map.Entry<String, PersonLinkPojo> pair = it3.next();
									c.setTitle(pair.getKey().toString());
									PersonLinkPojo v = (PersonLinkPojo)pair.getValue();
									c.setUrl(v.getUrl());
									links.add(c);
								}
								cmp.setLinks(links);
							}
							cmp.setType(MemberType.user);
							
						}//(end if adding user _not_ user group to community) 
	
						// Add new member object to the set
						cmps.add(cmp);

						/////////////////////////////////////////////////////////////////////////////////////////////////
						// TODO (INF-1214): Make this code more robust to handle changes to the community that need to
						// Caleb: this means change update to $set
						/////////////////////////////////////////////////////////////////////////////////////////////////
						DbManager.getSocial().getCommunity().update(query, community.toDb());

						rp.setData(community, new CommunityPojoApiMap());

						if (MemberType.user == cmp.getType()) {
							PersonHandler person = new PersonHandler();
							person.addCommunity(personIdStr, communityIdStr, communityName, community.getType());
							
							rp.setResponse(new ResponseObject("Add member to community", true, "Person has been added as member of community"));
						}
						else {
							rp.setResponse(new ResponseObject("Add member to community", true, "User Group has been added to community"));							
						}
					}					
					else
					{
						rp.setResponse(new ResponseObject("Add member to community",true,"Person/Group is already a member of the community."));	
					}
				}
				else
				{
					rp.setResponse(new ResponseObject("Add member to community", false, "Community not found."));
				}
			}
			catch (Exception e)
			{			
				// If an exception occurs log the error
				logger.error("Exception Message: " + e.getMessage(), e);
				rp.setResponse(new ResponseObject("Add member to community", false, 
						"Error adding member to community " + e.getMessage()));
			}	
		}
		else
		{
			rp.setResponse(new ResponseObject("Add member to community", false, 
				"The user does not have permissions to add a user to the community."));
		}
		
		return rp;
	}

	
	/**
	 * removeCommunityMember (only called internally and from PersonHandler)
	 * @param userIdStr
	 * @param communityIdStr
	 * @param personIdStr
	 * @return ResponsePojo
	 */
	public ResponsePojo removeCommunityMember(String userIdStr, String communityIdStr, String personIdStr)
	{
		/////////////////////////////////////////////////////////////////////////////////////////////////
		// Note: Only Sys Admins, Community Owner, and Community Moderators can remove users
		boolean isOwnerOrModerator = SocialUtils.isOwnerOrModerator(communityIdStr, userIdStr);
		boolean isSysAdmin = RESTTools.adminLookup(userIdStr);
		boolean canRemove = (isOwnerOrModerator || isSysAdmin || isRemovingSelf("remove", userIdStr, personIdStr)) ? true : false;
		
		ResponsePojo rp = new ResponsePojo();
		
		if (canRemove)
		{
			try
			{
				// Find person record to update
				BasicDBObject query = new BasicDBObject();
				query.put("_id", new ObjectId(communityIdStr));
				DBObject dbo = DbManager.getSocial().getCommunity().findOne(query);

				if (dbo != null) 
				{
					// Get GsonBuilder object with MongoDb de/serializers registered
					CommunityPojo community = CommunityPojo.fromDb(dbo, CommunityPojo.class);

					Boolean isMember = false;

					Set<CommunityMemberPojo> cmps = null;
					CommunityMemberPojo cmp = null;
					if (community.getMembers() != null)
					{
						cmps = community.getMembers();
						for (CommunityMemberPojo c : cmps)
						{
							if (c.get_id().toString().equals(personIdStr))
							{
								cmp = c;
								isMember = true;
							}
						}
					}

					if (isMember)
					{
						cmps.remove(cmp);

						/////////////////////////////////////////////////////////////////////////////////////////////////
						// TODO (INF-1214): Make this code more robust to handle changes to the community that need to
						// Caleb: this means change update to $set
						/////////////////////////////////////////////////////////////////////////////////////////////////					
						DbManager.getSocial().getCommunity().update(query, community.toDb());

						if (CommunityMemberPojo.MemberType.user == cmp.getType()) {
							PersonHandler person = new PersonHandler();
							person.removeCommunity(personIdStr, communityIdStr);
	
							rp.setData(community, new CommunityPojoApiMap());
							rp.setResponse(new ResponseObject("Remove member from community", true, "Member has been removed from community"));
						}
						else { // user group
							//TODO (INF-2866): remove from community from each user, if that's the way we end up implementing this (?)
							CommunityPojo user_group = CommunityPojo.fromDb(MongoDbManager.getSocial().getCommunity().findOne(new BasicDBObject(CommunityPojo._id_, new ObjectId(personIdStr))), CommunityPojo.class);
							CommunityPojo.removeDatagroupFromUserOrUsergroup(null, user_group, community);
							rp.setData(community, new CommunityPojoApiMap());
							rp.setResponse(new ResponseObject("Remove member from community", true, "User Group has been removed from community"));							
						}
					}					
					else
					{
						rp.setResponse(new ResponseObject("Remove member from community",true,"Person/Group is not a member of this community."));	
					}
				}
				else
				{
					rp.setResponse(new ResponseObject("Remove member from community", false, "Community not found."));
				}
			}
			catch (Exception e)
			{			
				// If an exception occurs log the error
				logger.error("Exception Message: " + e.getMessage(), e);
				rp.setResponse(new ResponseObject("Remove member from community", false, 
						"Error removing member from community " + e.getMessage()));
			}	
		}
		else
		{
			rp.setResponse(new ResponseObject("Remove member from community", false, 
					"The user does not have permissions to remove a user from the community."));
		}
		
		return rp;
	}
	
	/**
	 * createSelfCommunity
	 * Creates a personal group for the given user and adds them to it.
	 * @param person
	 */
	public void createSelfCommunity(PersonPojo person)
	{
		try
		{
			//create community
			CommunityPojo selfCommunity = new CommunityPojo();
			selfCommunity.setId(person.get_id()); //set to same as users
			selfCommunity.setName(person.getDisplayName() + "'s Personal Community");
			selfCommunity.setDescription(person.getDisplayName() + "'s Personal Community");
			selfCommunity.setCreated(new Date());
			selfCommunity.setModified(new Date());
			selfCommunity.setIsPersonalCommunity(true);		
			Map<String,CommunityAttributePojo> commAttributes = new HashMap<String,CommunityAttributePojo>();
			commAttributes.put("isPublic", new CommunityAttributePojo("boolean","false") );
			commAttributes.put("usersCanSelfRegister", new CommunityAttributePojo("boolean","false") );
			commAttributes.put("registrationRequiresApproval", new CommunityAttributePojo("boolean","false") );
			commAttributes.put("usersCanCreateSubCommunities", new CommunityAttributePojo("boolean","false") );
			selfCommunity.setCommunityAttributes(commAttributes);
			Map<String,CommunityUserAttributePojo> commUserAttributes = new HashMap<String,CommunityUserAttributePojo>();
			commUserAttributes.put("publishLoginToActivityFeed", new CommunityUserAttributePojo("boolean","true",true));
			commUserAttributes.put("publishCommentsToActivityFeed", new CommunityUserAttributePojo("boolean","true",true));
			commUserAttributes.put("publishSharingToActivityFeed", new CommunityUserAttributePojo("boolean","true",true));
			commUserAttributes.put("publishQueriesToActivityFeed", new CommunityUserAttributePojo("boolean","true",true));
			commUserAttributes.put("publishCommentsPublicly", new CommunityUserAttributePojo("boolean","false",true));
			selfCommunity.setCommunityUserAttribute(commUserAttributes);
			
			// Create the index form of the community:
			try {
				GenericProcessingController.createCommunityDocIndex(selfCommunity.getId().toString(), null, true, false, false);
				//TESTED
			}
			catch (Exception e) {} // Do nothing, will have to update the user to stop bad things from happening on query though
			
			//write community to db
			DbManager.getSocial().getCommunity().insert(selfCommunity.toDb());
			//update user to be in this community
			PersonCommunityPojo pcpSelf = new PersonCommunityPojo(person.get_id(), selfCommunity.getName());
			person.getCommunities().add(pcpSelf);
			/////////////////////////////////////////////////////////////////////////////////////////////////
			// TODO (INF-1214): Make this code more robust to handle changes to the community that need to
			// Caleb: this means change update to $set
			/////////////////////////////////////////////////////////////////////////////////////////////////
			DbManager.getSocial().getPerson().update(new BasicDBObject("_id",person.get_id()), person.toDb());
		}
		catch (Exception ex)
		{
			
		}
	}
	


	//////////////////////////////////////////////////////////////////////////
	//////////////////////// Helper Functions ////////////////////////////////
	///////// These functions do not get called from the public API //////////
	//////////////////////////////////////////////////////////////////////////
	
	/**
	 * getToAddressesFromCommunity (Internal)
	 * @param cp
	 * @return
	 */
	private static String getToAddressesFromCommunity(CommunityPojo cp)
	{
		StringBuffer emailAddresses = new StringBuffer();
		emailAddresses.append(cp.getOwner().getEmail());
		
		for (CommunityMemberPojo cm : cp.getMembers())
		{
			if (cm.getUserType().equalsIgnoreCase("moderator"))
			{
				if (CommunityMemberPojo.MemberType.user == cm.getType()) {
					emailAddresses.append(";" + cm.getEmail());
				}
				else {// user group
					
					CommunityPojo userGroup = CommunityPojo.fromDb(DbManager.getSocial().getCommunity().findOne(new BasicDBObject("_id", cm.get_id())), CommunityPojo.class);

					if (null != userGroup) {
						emailAddresses.append(";" + userGroup.getOwner().getEmail());
						for (CommunityMemberPojo cm2 : userGroup.getMembers())
						{
							if (CommunityMemberPojo.MemberType.user_group != cm.getType()) {
								emailAddresses.append(";" + cm2.getEmail());
							}
							//(don't support recursion, so just ignore from here)
						}						
					}
				}//TESTED
			}
		}		
		return emailAddresses.toString();
	}
	
	/**
	 * getTagsFromString
	 * @param t
	 * @return List<String>
	 */
	private List<String> getTagsFromString(String t)
	{
		List<String> tags = new ArrayList<String>();
		if (t != null)
		{	
			try
			{
				String[] a = t.split(",");
				for (String v : a)
				{
					tags.add(v);
				}
			}
			catch (Exception e)
			{
			}
		}
		return tags;
	}	
	
	/**
	 * getDefaultCommunityAttributes
	 * @return Map<String, CommunityAttributePojo
	 */
	private Map<String, CommunityAttributePojo> getDefaultCommunityAttributes()
	{
		Map<String, CommunityAttributePojo> c = new HashMap<String, CommunityAttributePojo>();
		CommunityAttributePojo v = new CommunityAttributePojo();	
		v.setType("Boolean");
		v.setValue("false");
		c.put("isPublic", v);
		v = new CommunityAttributePojo();
		v.setType("Boolean");
		v.setValue("true");
		c.put("usersCanSelfRegister", v);
		v = new CommunityAttributePojo();
		v.setType("Boolean");
		v.setValue("true");
		c.put("registrationRequiresApproval", v);
		v = new CommunityAttributePojo();
		v.setType("Boolean");
		v.setValue("false");
		c.put("usersCanCreateSubCommunities", v);
		return c;
	}
	
	/**
	 * getDefaultCommunityUserAttributes
	 * @return Map<String, CommunityUserAttributePojo>
	 */
	private Map<String, CommunityUserAttributePojo> getDefaultCommunityUserAttributes()
	{
		Map<String, CommunityUserAttributePojo> c = new HashMap<String, CommunityUserAttributePojo>();
		CommunityUserAttributePojo v = new CommunityUserAttributePojo();
		v.setType("Boolean");
		v.setDefaultValue("true");
		v.setAllowOverride(false);
		c.put("publishLoginToActivityFeed", v);
		v = new CommunityUserAttributePojo();
		v.setType("Boolean");
		v.setDefaultValue("true");
		v.setAllowOverride(false);
		c.put("publishCommentsToActivityFeed", v);
		v = new CommunityUserAttributePojo();
		v.setType("Boolean");
		v.setDefaultValue("true");
		v.setAllowOverride(false);
		c.put("publishSharingToActivityFeed", v);
		v = new CommunityUserAttributePojo();
		v.setType("Boolean");
		v.setDefaultValue("true");
		v.setAllowOverride(false);
		c.put("publishQueriesToActivityFeed", v);
		v = new CommunityUserAttributePojo();
		v.setType("Boolean");
		v.setDefaultValue("false");
		v.setAllowOverride(false);
		c.put("publishCommentsPublicly", v);
		return c;
	}
	
	/**
	 * Utility function to remove members if users are not suppose to see them
	 * 
	 * @param inputCommunities
	 * @param isAdminModerator
	 * @return
	 */
	private CommunityPojo filterCommunityMembers(CommunityPojo community, boolean isAdmin, String userId)
	{
		//an admin can see everything
		if ( !(isAdmin || SocialUtils.isOwnerOrModerator(community.getId().toString(), userId) ) )
		{
			//if community has publish members turned off, remove the members
			if ( community.getCommunityAttributes().containsKey("publishMemberOverride") && 
					community.getCommunityAttributes().get("publishMemberOverride").getValue().equals("false") )
			{
				community.setMembers(null);
			}				
		}
		return community;
	}
	private List<CommunityPojo> filterCommunityMembers(List<CommunityPojo> inputCommunities, boolean isAdmin, String userId)
	{
		//an admin can see everything
		if ( !isAdmin )
		{
			for ( CommunityPojo community : inputCommunities )
			{
				filterCommunityMembers(community, isAdmin, userId);
			}			
		}
		return inputCommunities;
	}
	
	// Utility: make life easier in terms of adding/update/inviting/leaving from the command line
	
	private static String allowCommunityRegex(String userIdStr, String communityIdStr) {
		if (communityIdStr.startsWith("*")) {
			String[] communityIdStrs = SocialUtils.getCommunityIds(userIdStr, communityIdStr);	
			if (1 == communityIdStrs.length) {
				communityIdStr = communityIdStrs[0]; 
			}
			else if (communityIdStrs.length > 0) {
				throw new RuntimeException("Invalid community pattern (many): " + Arrays.toString(communityIdStrs));				
			}
			else {
				throw new RuntimeException("Invalid community pattern (none)");
			}
		}	
		return communityIdStr;
	}		

	// UTILITY: data/user group handling
	
	private BasicDBObject addCommunityTypeTerm(BasicDBObject query, CommunityType type) {
		if (CommunityType.data == type) { // ie both combined and data (except personal/combined)
			query.put("type", new BasicDBObject(DbManager.ne_, CommunityType.user.toString()));
			query.put("isPersonalCommunity", false);
		}
		else if (CommunityType.user == type) { // (user only)
			query.put("type", CommunityType.user.toString());
		}
		return query;
	}//TESTED (by hand)

}
