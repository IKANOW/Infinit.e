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
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.List;
import org.apache.log4j.Logger;
import org.bson.types.ObjectId;

import com.ikanow.infinit.e.api.config.source.SourceHandler;
import com.ikanow.infinit.e.api.utils.PropertiesManager;
import com.ikanow.infinit.e.api.utils.RESTTools;
import com.ikanow.infinit.e.api.utils.SendMail;
import com.ikanow.infinit.e.data_model.api.ApiManager;
import com.ikanow.infinit.e.data_model.api.ResponsePojo;
import com.ikanow.infinit.e.data_model.api.ResponsePojo.ResponseObject;
import com.ikanow.infinit.e.data_model.api.social.community.CommunityApprovalPojo;
import com.ikanow.infinit.e.data_model.api.social.community.CommunityPojoApiMap;
import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.MongoDbManager;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.ikanow.infinit.e.data_model.store.social.community.CommunityApprovePojo;
import com.ikanow.infinit.e.data_model.store.social.community.CommunityAttributePojo;
import com.ikanow.infinit.e.data_model.store.social.community.CommunityMemberContactPojo;
import com.ikanow.infinit.e.data_model.store.social.community.CommunityMemberLinkPojo;
import com.ikanow.infinit.e.data_model.store.social.community.CommunityMemberPojo;
import com.ikanow.infinit.e.data_model.store.social.community.CommunityMemberUserAttributePojo;
import com.ikanow.infinit.e.data_model.store.social.community.CommunityPojo;
import com.ikanow.infinit.e.data_model.store.social.community.CommunityUserAttributePojo;
import com.ikanow.infinit.e.data_model.store.social.person.PersonCommunityPojo;
import com.ikanow.infinit.e.data_model.store.social.person.PersonContactPojo;
import com.ikanow.infinit.e.data_model.store.social.person.PersonLinkPojo;
import com.ikanow.infinit.e.data_model.store.social.person.PersonPojo;
import com.ikanow.infinit.e.data_model.store.social.sharing.SharePojo;
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
	public ResponsePojo getCommunities(String userIdStr) 
	{	
		ResponsePojo rp = new ResponsePojo();

		/////////////////////////////////////////////////////////////////////////////////////////////////
		// Note: Only Sys Admins see all communities
		boolean isSysAdmin = RESTTools.adminLookup(userIdStr);

		try
		{
			if (isSysAdmin)
			{
				DBCursor dbc = DbManager.getSocial().getCommunity().find();
				
				if ( dbc.count() > 0 )
				{
					rp.setData(CommunityPojo.listFromDb(dbc, CommunityPojo.listType()), new CommunityPojoApiMap());
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
				BasicDBObject queryTerm1 = new BasicDBObject("communityAttributes.isPublic.value", "true");
				BasicDBObject queryTerm2 = new BasicDBObject("members._id", new ObjectId(userIdStr));
				BasicDBObject queryTerm3 = new BasicDBObject("ownerId", new ObjectId(userIdStr));
				BasicDBObject query = new BasicDBObject(MongoDbManager.or_, Arrays.asList(queryTerm1, queryTerm2, queryTerm3));

				DBCursor dbc = DbManager.getSocial().getCommunity().find(query);				
				if ( dbc.count() > 0 )
				{
					rp.setData(CommunityPojo.listFromDb(dbc, CommunityPojo.listType()), new CommunityPojoApiMap());
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
	public ResponsePojo getCommunities(String userIdStr, Boolean isPublic) 
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
			
			DBCursor dbc = DbManager.getSocial().getCommunity().find(query);
			if ( dbc.count() > 0 )
			{
				rp.setData(CommunityPojo.listFromDb(dbc, CommunityPojo.listType()), new CommunityPojoApiMap());
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
	public ResponsePojo getCommunity(String userIdStr, String communityIdStr) 
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
				query.put("isSystemCommunity", true);
			}
			
			// Get GsonBuilder object with MongoDb de/serializers registered
			BasicDBObject dbo = (BasicDBObject)DbManager.getSocial().getCommunity().findOne(query);
			
			if (dbo != null)
			{
				CommunityPojo community = CommunityPojo.fromDb(dbo, CommunityPojo.class);			
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
			logger.error("Exception Message: " + e.getMessage(), e);
			rp.setResponse(new ResponseObject("Community Info", false, "Error returning community info: " + e.getMessage()
					+ " - " + e.getStackTrace().toString()));
		}
		return rp;
	}

	/**
	 * getSystemCommunity (REST)
	 * @return ResponsePojo
	 */
	public ResponsePojo getSystemCommunity() 
	{	
		return getCommunity(null, null);
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
	public ResponsePojo addCommunity(String userIdStr, String name, String description, String parentIdStr, String tags)
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
					DBObject dboparent = DbManager.getSocial().getCommunity().findOne(new BasicDBObject("_id", new ObjectId(parentIdStr)));
					if ( dboparent != null )
					{
						CommunityPojo cp = CommunityPojo.fromDb(dboparent, CommunityPojo.class);
						parentName = cp.getName();
					}
					else
					{
						return new ResponsePojo(new ResponseObject("Add Community", false, "Parent community does not exist"));
					}
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
		return addCommunity(userIdStr, name, description, parentIdStr, parentName, tags, userIdStr, userName, userEmail);
	}
	
	/**
	 * addCommunity (REST)
	 * Creates a new community by id (alternate)
	 */
	private ResponsePojo addCommunity(String userIdStr, String name, String description, 
			String parentIdStr, String parentName, String tags, String ownerIdStr, 
			String ownerDisplayName, String ownerEmail)
	{
		return addCommunity(userIdStr, null, name, description, parentIdStr, parentName, tags, 
				ownerIdStr, ownerDisplayName, ownerEmail);	
	}	
	public ResponsePojo addCommunity(String userId, String idStr, String name, String description, 
			String parentIdStr, String parentName, String tags, String ownerIdStr, 
			String ownerDisplayName, String ownerEmail)
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
				c.setId(oId);
				c.setCreated(new Date());
				c.setModified(new Date());
				c.setName(name);
				c.setDescription(description);
				if (parentIdStr != null && parentName != null)
				{
					c.setParentId(new ObjectId(parentIdStr));
					c.setParentName(parentName);
				}
				c.setIsPersonalCommunity(false);
				c.setTags(getTagsFromString(tags));
				c.setOwnerId(new ObjectId(ownerIdStr));
				c.setOwnerDisplayName(ownerDisplayName);
				c.setNumberOfMembers(0);
				c.setCommunityAttributes(getDefaultCommunityAttributes());
				c.setCommunityUserAttribute(getDefaultCommunityUserAttributes());
				
				// Insert new community document in the community collection
				DBObject commObj = c.toDb();

				// Create the index form of the community:
				try {
					GenericProcessingController.createCommunityDocIndex(c.getId().toString(), c.getParentId(), c.getIsPersonalCommunity(), c.getIsSystemCommunity(), false);
				}
				catch (Exception e) { // Can't create community
					rp.setResponse(new ResponseObject("Add Community", false, "Error adding new community because of index failure: " + e.getMessage()));
					return rp;
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
				rp = addCommunityMember(userId, oId.toStringMongod(), name, ownerIdStr, ownerEmail, ownerDisplayName, "owner", "active");
				rp.setResponse(new ResponseObject("Add Community", true, "The " + name + " community has " +
					"been added."));				
			}
			else
			{
				rp.setResponse(new ResponseObject("Add Community", false, 
						"Error adding new community. A community with the name " + name + " " +
						"already exists."));
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
	public ResponsePojo removeCommunity(String personIdStr, String communityIdStr) 
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
								
								// 1] Remove from all shares (delete shares if that leaves them orphaned)
								
								BasicDBObject deleteQuery1 = new BasicDBObject(ShareOwnerPojo.communities_id_, communityId);
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
								for (SourcePojo source: sources) {
									SourceHandler tmpHandler = new SourceHandler();
									if (1 == source.getCommunityIds().size()) { // delete this source
										tmpHandler.deleteSource(source.getId().toString(), communityIdStr, personIdStr, false);
											// (deletes all docs and removes from the share)
									}
									else { // Still need to delete docs from this share from this community
										tmpHandler.deleteSource(source.getId().toString(), communityIdStr, personIdStr, true);										
									}
								}
								BasicDBObject update2 = new BasicDBObject(DbManager.pull_, new BasicDBObject(SourcePojo.communityIds_, communityId));
								DbManager.getSocial().getShare().update(deleteQuery2, update2, false, true);

								//TESTED (both types, check docs deleted)
								
								// 4] Finally delete the object itself
								
								DbManager.getSocial().getCommunity().remove(new BasicDBObject("_id", communityId));
								
								// Remove from index:
								GenericProcessingController.deleteCommunityDocIndex(communityId.toString(), cp.getParentId(), false);
								//TESTED
								
								// 5] Finally finally remove from parent communities
								if (null != cp.getParentId()) {
									BasicDBObject updateQuery = new BasicDBObject("_id", cp.getParentId());
									BasicDBObject updateUpdate = new BasicDBObject(DbManager.pull_, new BasicDBObject("children", cp.getId()));
									DbManager.getSocial().getCommunity().update(updateQuery, updateUpdate, false, true);									
								}
								//TESTED
								
								rp.setResponse(new ResponseObject("Delete community", true, "Community deleted forever."));
							}
							else { // First time, just remove all users and disable
								//at this point, we have verified, community/user exist, not a personal group, user is member and owner
								//set community as inactive (for some reason we don't delete it)
								DbManager.getSocial().getCommunity().update(new BasicDBObject("_id", communityId), 
																			new BasicDBObject(DbManager.set_, new BasicDBObject("communityStatus","disabled")));
								
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
			
		}
		
		return rp;
	}

	// (Note supports personId as either Id or username (email) both are unique indexes)

	/**
	 * updateMemberStatus (REST)
	 */
	
	public ResponsePojo updateMemberStatus(String callerIdStr, String personIdStr, String communityIdStr, String userStatus) 
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
				if (null == dboPerson) { // (ie personId isn't an email address... convert to ObjectId and try again)
					dboPerson = (BasicDBObject) DbManager.getSocial().getPerson().findOne(new BasicDBObject("_id", new ObjectId(personIdStr)));
				}
				else {
					personIdStr = dboPerson.getString("_id"); 
				}
				// OK from here on, personId is the object Id...
								
				CommunityPojo cp = CommunityPojo.fromDb(dbo,CommunityPojo.class);
				if ( cp.isOwner(new ObjectId(callerIdStr)) || callerIdStr.equals(personIdStr) || isSysAdmin)
				{
					if ( cp.isMember(new ObjectId(personIdStr)) )
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
					rp.setResponse(new ResponseObject("Update member status",false,"Caller must be owner of community, or member changing"));
				}
			}
			else
			{
				rp.setResponse(new ResponseObject("Update member status",false,"Community does not exist"));
			}			
		}
		catch(Exception ex)
		{
			rp.setResponse(new ResponseObject("Update member status",false,"General Error, bad params maybe? " + ex.getMessage()));
		}
		return rp;
	}

	// (Note supports personId as either Id or username (email) both are unique indexes)

	/**
	 * updateMemberType (REST)
	 */
	public ResponsePojo updateMemberType(String callerIdStr, String personIdStr, String communityIdStr, String userType) 
	{
		boolean isSysAdmin = RESTTools.adminLookup(callerIdStr);
		ResponsePojo rp = new ResponsePojo();
		try
		{
			//verify user is in this community, then update status
			communityIdStr = allowCommunityRegex(callerIdStr, communityIdStr);
			BasicDBObject query = new BasicDBObject("_id",new ObjectId(communityIdStr));
			DBObject dbo = DbManager.getSocial().getCommunity().findOne(query);
			if ( dbo != null )
			{
				// PersonId can be _id or username/email
				BasicDBObject dboPerson = (BasicDBObject) DbManager.getSocial().getPerson().findOne(new BasicDBObject("email", personIdStr));
				if (null == dboPerson) { // (ie personId isn't an email address... convert to ObjectId and try again)
					dboPerson = (BasicDBObject) DbManager.getSocial().getPerson().findOne(new BasicDBObject("_id", new ObjectId(personIdStr)));
				}
				else {
					personIdStr = dboPerson.getString("_id"); 
				}
				// OK from here on, personId is the object Id...

				CommunityPojo cp = CommunityPojo.fromDb(dbo,CommunityPojo.class);
				if ( cp.isOwner(new ObjectId(callerIdStr)) || callerIdStr.equals(personIdStr) || isSysAdmin)
				{
					if ( cp.isMember(new ObjectId(personIdStr)))
					{
						ObjectId userID = new ObjectId(personIdStr);
						//verified user, update status
						for ( CommunityMemberPojo cmp : cp.getMembers())
						{
							if ( cmp.get_id().equals(userID) )
							{
								cmp.setUserType(userType);		
								/////////////////////////////////////////////////////////////////////////////////////////////////
								// TODO (INF-1214): Make this code more robust to handle changes to the community that need to
								// Caleb: this means change update to $set
								/////////////////////////////////////////////////////////////////////////////////////////////////
								DbManager.getSocial().getCommunity().update(query, cp.toDb());
								rp.setResponse(new ResponseObject("Update member type",true,"Updated member type successfully"));
								break;
							}
						}
					}
					else
					{
						rp.setResponse(new ResponseObject("Update member type",false,"User was not a member of the community"));
					}
				}
				else
				{
					rp.setResponse(new ResponseObject("Update member type",false,"Caller must be owner of community, or member changing"));
				}
			}
			else
			{
				rp.setResponse(new ResponseObject("Update member type",false,"Community does not exist"));
			}			
		}
		catch(Exception ex)
		{
			rp.setResponse(new ResponseObject("Update member type",false,"General Error, bad params maybe? " + ex.getMessage()));
		}
		return rp;
	}

	/**
	 * joinCommunity (REST)
	 */
	public ResponsePojo joinCommunity(String personIdStr, String communityIdStr) 
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
					if ( !cp.isMember(new ObjectId(personIdStr)))
					{
						Map<String,CommunityAttributePojo> commatt = cp.getCommunityAttributes();
						if ( commatt.containsKey("usersCanSelfRegister") && commatt.get("usersCanSelfRegister").getValue().equals("true"))
						{		
							boolean requiresApproval = false;
							if ( commatt.containsKey("registrationRequiresApproval") )
								requiresApproval = commatt.get("registrationRequiresApproval").getValue().equals("true");
							//if approval is required, add user to comm, wait for owner to approve
							//otherwise go ahead and add as a member
							if ( requiresApproval )
							{
								DBObject dboPerson = DbManager.getSocial().getPerson().findOne(new BasicDBObject("_id",new ObjectId(personIdStr)));
								PersonPojo pp = PersonPojo.fromDb(dboPerson,PersonPojo.class);
								cp.addMember(pp,true);
								//write both objects back to db now
								/////////////////////////////////////////////////////////////////////////////////////////////////
								// TODO (INF-1214): Make this code more robust to handle changes to the community that need to
								// Caleb: this means change update to $set
								/////////////////////////////////////////////////////////////////////////////////////////////////
								DbManager.getSocial().getCommunity().update(query, cp.toDb());
															
								//send email out to owner for approval
								CommunityApprovePojo cap = cp.createCommunityApprove(personIdStr,communityIdStr,"join",personIdStr);
								DbManager.getSocial().getCommunityApprove().insert(cap.toDb());
								
								// Get to addresses for Owner and Moderators
								String toAddresses = getToAddressesFromCommunity(cp);
								
								PropertiesManager propManager = new PropertiesManager();
								String rootUrl = propManager.getUrlRoot();
								
								String subject = pp.getDisplayName() + " is trying to join infinit.e community: " + cp.getName();
								String body = pp.getDisplayName() + " is trying to join infinit.e community: " + cp.getName() + "<br/>Do you want to accept this request?" +
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
								BasicDBObject queryPerson = new BasicDBObject("_id",new ObjectId(personIdStr));
								DBObject dboPerson = DbManager.getSocial().getPerson().findOne(queryPerson);
								PersonPojo pp = PersonPojo.fromDb(dboPerson,PersonPojo.class);
								cp.addMember(pp);
								pp.addCommunity(cp);
								//write both objects back to db now
								/////////////////////////////////////////////////////////////////////////////////////////////////
								// TODO (INF-1214): Make this code more robust to handle changes to the community that need to
								// Caleb: this means change update to $set
								/////////////////////////////////////////////////////////////////////////////////////////////////
								DbManager.getSocial().getCommunity().update(query, cp.toDb());
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
			rp.setResponse(new ResponseObject("Join Community",false,"General Error, bad params maybe? " + ex.getMessage()));
		}
		return rp;
	}
	
	/**
	 * leaveCommunity (REST)
	 */
	
	public ResponsePojo leaveCommunity(String personIdStr, String communityIdStr) 
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
			rp.setResponse(new ResponseObject("Leave Community",false,"General Error, bad params maybe? " + ex.getMessage()));
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
	public ResponsePojo inviteCommunity(String userIdStr, String personIdStr, String communityIdStr, String skipInvitation) 
	{
		communityIdStr = allowCommunityRegex(userIdStr, communityIdStr);
		
		boolean skipInvite = ((null != skipInvitation) && (skipInvitation.equalsIgnoreCase("true"))) ? true : false;
		
		/////////////////////////////////////////////////////////////////////////////////////////////////
		// Note: Only Sys Admins, Community Owner, and Community Moderators can invite users to
		// private communities however any member can be able to invite someone to a public community
		boolean isOwnerOrModerator = CommunityHandler.isOwnerOrModerator(communityIdStr, userIdStr);
		boolean isSysAdmin = RESTTools.adminLookup(userIdStr);
		boolean canInvite = (isOwnerOrModerator || isSysAdmin) ? true : false;

		ResponsePojo rp = new ResponsePojo();
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
					// Check to see if the user has permissions to invite
					if ( canInvite || cp.getOwnerId().toString().equalsIgnoreCase(userIdStr) )
					{
						BasicDBObject dboPerson = (BasicDBObject) DbManager.getSocial().getPerson().findOne(new BasicDBObject("email", personIdStr));
						if (null == dboPerson) { // (ie personId isn't an email address... convert to ObjectId and try again)
							dboPerson = (BasicDBObject) DbManager.getSocial().getPerson().findOne(new BasicDBObject("_id", new ObjectId(personIdStr)));
						}
						else {
							personIdStr = dboPerson.getString("_id"); 
						}
						// OK from here on, personId is the object Id...
						
						if ( dboPerson != null )
						{
							PersonPojo pp = PersonPojo.fromDb(dboPerson,PersonPojo.class);
							
							if ( !cp.isMember(pp.get_id()))
							{
								if (isSysAdmin && skipInvite) // Can only skip invite if user is Admin
								{
									// Update community with new member
									cp.addMember(pp, false); // Member status set to Active
									cp.setNumberOfMembers(cp.getNumberOfMembers() + 1); // Increment number of members
									/////////////////////////////////////////////////////////////////////////////////////////////////
									// TODO (INF-1214): Make this code more robust to handle changes to the community that need to
									// Caleb: this means change update to $set
									/////////////////////////////////////////////////////////////////////////////////////////////////
									DbManager.getSocial().getCommunity().update(query, cp.toDb());
									
									// Add community to persons object and save to db
									pp.addCommunity(cp);
									/////////////////////////////////////////////////////////////////////////////////////////////////
									// TODO (INF-1214): Make this code more robust to handle changes to the community that need to
									// Caleb: this means change update to $set
									/////////////////////////////////////////////////////////////////////////////////////////////////
									DbManager.getSocial().getPerson().update(new BasicDBObject("_id", pp.get_id()), pp.toDb());
									
									rp.setResponse(new ResponseObject("Invite Community",true,"User added to community successfully."));
								}
								else
								{
									cp.addMember(pp, true); // Member status set to Pending
									/////////////////////////////////////////////////////////////////////////////////////////////////
									// TODO (INF-1214): Make this code more robust to handle changes to the community that need to
									// Caleb: this means change update to $set
									/////////////////////////////////////////////////////////////////////////////////////////////////
									DbManager.getSocial().getCommunity().update(query, cp.toDb());
									
									//send email out inviting user
									CommunityApprovePojo cap = cp.createCommunityApprove(personIdStr,communityIdStr,"invite",userIdStr);
									DbManager.getSocial().getCommunityApprove().insert(cap.toDb());
									
									PropertiesManager propManager = new PropertiesManager();
									String rootUrl = propManager.getUrlRoot();
									
									String subject = "Invite to join infinit.e community: " + cp.getName();
									String body = "You have been invited to join the community " + cp.getName() + 
										"<br/><a href=\"" + rootUrl + "social/community/requestresponse/"+cap.get_id().toString() + "/true\">Accept</a> " +
										"<a href=\"" + rootUrl + "social/community/requestresponse/"+cap.get_id().toString() + "/false\">Deny</a>"; 
									
									SendMail mail = new SendMail(new PropertiesManager().getAdminEmailAddress(), pp.getEmail(), subject, body);
									
									if (mail.send("text/html"))
									{
										if (isSysAdmin) {
											rp.setResponse(new ResponseObject("Invite Community",true,"Invited user to community successfully: " + cap.get_id().toString()));
										}
										else {
											rp.setResponse(new ResponseObject("Invite Community",true,"Invited user to community successfully"));									
										}
									}
									else
									{
										rp.setResponse(new ResponseObject("Invite Community",false,"The system was unable to email the invite for an unknown reason."));
									}
								}
							}
							else
							{
								rp.setResponse(new ResponseObject("Invite Community",false,"The user is already a member of this community."));
							}
						}
						else
						{
							rp.setResponse(new ResponseObject("Invite Community",false,"Person does not exist"));
						}
					}
					else
					{
						rp.setResponse(new ResponseObject("Invite Community",false,"You must be owner to invite other members"));
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
			rp.setResponse(new ResponseObject("Invite Community",false,"General Error, bad params maybe? " + ex.getMessage()));
		}
		return rp;
	}


	/**
	 * requestResponse (REST)
	 * @param requestIdStr
	 * @param resp
	 * @return
	 */
	public ResponsePojo requestResponse(String requestIdStr, String resp) 
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
						
						if ( dboComm != null )
						{
							CommunityPojo cp = CommunityPojo.fromDb(dboComm, CommunityPojo.class);
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
								BasicDBObject queryPerson = new BasicDBObject("_id",new ObjectId(cap.getPersonId()));
								DBObject dboperson = DbManager.getSocial().getPerson().findOne(queryPerson);
								if ( dboperson != null)
								{
									cp.updateMemberStatus(cap.getPersonId(), "active");
									cp.setNumberOfMembers(cp.getNumberOfMembers()+1);
									/////////////////////////////////////////////////////////////////////////////////////////////////
									// TODO (INF-1214): Make this code more robust to handle changes to the community that need to
									// Caleb: this means change update to $set
									/////////////////////////////////////////////////////////////////////////////////////////////////
									DbManager.getSocial().getCommunity().update(query, cp.toDb());
									
									PersonPojo pp = PersonPojo.fromDb(dboperson, PersonPojo.class);
									pp.addCommunity(cp);
									/////////////////////////////////////////////////////////////////////////////////////////////////
									// TODO (INF-1214): Make this code more robust to handle changes to the community that need to
									// Caleb: this means change update to $set
									/////////////////////////////////////////////////////////////////////////////////////////////////
									DbManager.getSocial().getPerson().update(queryPerson, pp.toDb());
								}
								else
								{
									rp.setResponse(new ResponseObject("Request Response",false,"The person does not exist."));
								}
							}
							//remove request object now
							DbManager.getSocial().getCommunityApprove().remove(new BasicDBObject("_id",new ObjectId(requestIdStr)));
							//return successfully
							rp.setResponse(new ResponseObject("Request Response",true,"Request answered successfully!"));
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
			rp.setResponse(new ResponseObject("Request Response",false,"General Error, bad params maybe?"  + ex.getMessage()));
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
	 * @return
	 */
	public ResponsePojo updateCommunity(String userIdStr, String communityIdStr, String json) 
	{
		ResponsePojo rp = new ResponsePojo();
		communityIdStr = allowCommunityRegex(userIdStr, communityIdStr);
		
		/////////////////////////////////////////////////////////////////////////////////////////////////
		// Note: Only Sys Admins, Community Owner, and Community Moderators can add update communities
		boolean isOwnerOrModerator = CommunityHandler.isOwnerOrModerator(communityIdStr, userIdStr);
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
				// Here are the fields you are allowed to change:
				// name:
				if (null != updateCommunity.getName()) 
				{
					originalName = cp.getName();
					cp.setName(updateCommunity.getName());
				}
				if (null != updateCommunity.getDescription()) {
					cp.setDescription(updateCommunity.getDescription());					
				}
				if (null != updateCommunity.getTags()) {
					cp.setTags(updateCommunity.getTags());					
				}
				if ((null != updateCommunity.getCommunityAttributes() && !updateCommunity.getCommunityAttributes().isEmpty()))
				{
					cp.setCommunityAttributes(updateCommunity.getCommunityAttributes());					
				}
				if ((null != updateCommunity.getCommunityUserAttribute() && !updateCommunity.getCommunityUserAttribute().isEmpty()))
				{
					cp.setCommunityUserAttribute(updateCommunity.getCommunityUserAttribute());					
				}
				// Change owner: slighly meatier:
				if ((null != updateCommunity.getOwnerId()) && updateCommunity.getOwnerId().equals(cp.getOwnerId()))
				{
					cp.setOwnerId(null);
					// Must be currently a member:
					for (CommunityMemberPojo member: cp.getMembers()) {
						if (member.get_id().equals(updateCommunity.getOwnerId())) {
							cp.setOwnerId(member.get_id());
							cp.setOwnerDisplayName(member.getDisplayName());
							break;
						}
					}// (end loop over community members)
					if (null == cp.getOwnerId()) {
						rp.setResponse(new ResponseObject("Update Community",false,"Tried to change owner to a non-member"));
						return rp;
					}
				}
								
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
		boolean isOwnerOrModerator = CommunityHandler.isOwnerOrModerator(communityIdStr, userIdStr);
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
							if (c.get_id().toStringMongod().equals(personIdStr)) alreadyMember = true;
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

						// Add new member object to the set
						cmps.add(cmp);

						// Increment number of members by 1
						community.setNumberOfMembers(community.getNumberOfMembers() + 1);

						/////////////////////////////////////////////////////////////////////////////////////////////////
						// TODO (INF-1214): Make this code more robust to handle changes to the community that need to
						// Caleb: this means change update to $set
						/////////////////////////////////////////////////////////////////////////////////////////////////
						DbManager.getSocial().getCommunity().update(query, community.toDb());

						PersonHandler person = new PersonHandler();
						person.addCommunity(personIdStr, communityIdStr, communityName);

						rp.setData(community, new CommunityPojoApiMap());

						rp.setResponse(new ResponseObject("Add member to community", true, "Person has been added as member of community"));	
					}					
					else
					{
						rp.setResponse(new ResponseObject("Add member to community",true,"Person is already a member of the community."));	
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
		boolean isOwnerOrModerator = CommunityHandler.isOwnerOrModerator(communityIdStr, userIdStr);
		boolean isSysAdmin = RESTTools.adminLookup(userIdStr);
		boolean canRemove = (isOwnerOrModerator || isSysAdmin) ? true : false;
		
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
							if (c.get_id().toStringMongod().equals(personIdStr))
							{
								cmp = c;
								isMember = true;
							}
						}
					}

					if (isMember)
					{
						cmps.remove(cmp);

						community.setNumberOfMembers(community.getNumberOfMembers() - 1);

						/////////////////////////////////////////////////////////////////////////////////////////////////
						// TODO (INF-1214): Make this code more robust to handle changes to the community that need to
						// Caleb: this means change update to $set
						/////////////////////////////////////////////////////////////////////////////////////////////////					
						DbManager.getSocial().getCommunity().update(query, community.toDb());

						PersonHandler person = new PersonHandler();
						person.removeCommunity(personIdStr, communityIdStr);

						rp.setData(community, new CommunityPojoApiMap());
						rp.setResponse(new ResponseObject("Remove member from community", true, "Member has been removed from community"));	
					}					
					else
					{
						rp.setResponse(new ResponseObject("Remove member from community",true,"Person is not a member of this community."));	
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
	 * isOwnerOrModerator
	 * Returns true if personid is the owner or a moderator of the community
	 * @param communityIdStr
	 * @param personIdStr
	 * @return
	 */
	public static boolean isOwnerOrModeratorOrContentPublisher(String communityIdStr, String personIdStr) 
	{
		return  isOwnerOrModerator(communityIdStr, personIdStr, true);
	}
	public static boolean isOwnerOrModerator(String communityIdStr, String personIdStr) 
	{
		return  isOwnerOrModerator(communityIdStr, personIdStr, false);
	}
	private static boolean isOwnerOrModerator(String communityIdStr, String personIdStr, boolean bAllowContentPublisher) 
	{	
		boolean isOwnerOrModerator = false;
		
		try
		{

			BasicDBObject query = new BasicDBObject("_id", new ObjectId(communityIdStr));
			BasicDBObject dbo = (BasicDBObject)DbManager.getSocial().getCommunity().findOne(query);
			
			if ((null != dbo) && !dbo.isEmpty())
			{
				CommunityPojo community = CommunityPojo.fromDb(dbo, CommunityPojo.class);
				if (community.getIsPersonalCommunity() && communityIdStr.equals(personIdStr))
				{
					isOwnerOrModerator = true;					
				}
				else if (community.getOwnerId().toString().equalsIgnoreCase(personIdStr))
				{
					isOwnerOrModerator = true;
				}
				else
				{
					Set<CommunityMemberPojo> members = community.getMembers();
					for (CommunityMemberPojo c : members)
					{
						if (c.get_id().toString().equalsIgnoreCase(personIdStr) && c.getUserType().equalsIgnoreCase("moderator"))
							isOwnerOrModerator = true;
						else if (bAllowContentPublisher && c.get_id().toString().equalsIgnoreCase(personIdStr) && c.getUserType().equalsIgnoreCase("content_publisher"))
							isOwnerOrModerator = true;
					}
				}
			}
		} 
		catch (Exception e)
		{
			logger.error("Exception Message: " + e.getMessage(), e);
		}
		return isOwnerOrModerator;
	}

	/**
	 * getCommunities
	 * Accepts a String[] of community IDs and returns an ArrayList of CommunityPojos
	 * @param ids
	 * @return
	 */
	public static ArrayList<CommunityPojo> getCommunities(Collection<ObjectId> ids) 
	{	
		BasicDBObject in = new BasicDBObject();
		in.put("$in", ids);
		BasicDBObject query = new BasicDBObject();
		query.put("_id", in);
		
		ArrayList<CommunityPojo> communities = new ArrayList<CommunityPojo>();
		
		try
		{
			DBCursor dbc = DbManager.getSocial().getCommunity().find(query);
			communities.addAll(CommunityPojo.listFromDb(dbc, CommunityPojo.listType()));
		} 
		catch (Exception e)
		{
			logger.error("Exception Message: " + e.getMessage(), e);
		}
		return communities;
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
			selfCommunity.setNumberOfMembers(0);
			
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
				emailAddresses.append(";" + cm.getEmail());
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
	
	// Utility: make life easier in terms of adding/update/inviting/leaving from the command line
	
	private static String allowCommunityRegex(String userIdStr, String communityIdStr) {
		if (communityIdStr.startsWith("*")) {
			String[] communityIdStrs = RESTTools.getCommunityIds(userIdStr, communityIdStr);	
			if (1 == communityIdStrs.length) {
				communityIdStr = communityIdStrs[0]; 
			}
			else {
				throw new RuntimeException("Invalid community pattern");
			}
		}	
		return communityIdStr;
	}	
}
