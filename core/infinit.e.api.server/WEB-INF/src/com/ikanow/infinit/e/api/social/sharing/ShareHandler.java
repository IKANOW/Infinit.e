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
package com.ikanow.infinit.e.api.social.sharing;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import org.apache.log4j.Logger;
import org.bson.types.ObjectId;

import com.ikanow.infinit.e.api.utils.RESTTools;
import com.ikanow.infinit.e.data_model.api.ResponsePojo;
import com.ikanow.infinit.e.data_model.api.ResponsePojo.ResponseObject;
import com.ikanow.infinit.e.data_model.api.social.sharing.SharePojoApiMap;
import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.social.community.CommunityPojo;
import com.ikanow.infinit.e.data_model.store.social.person.PersonCommunityPojo;
import com.ikanow.infinit.e.data_model.store.social.person.PersonPojo;
import com.ikanow.infinit.e.data_model.store.social.sharing.SharePojo;
import com.ikanow.infinit.e.data_model.store.social.sharing.SharePojo.DocumentLocationPojo;
import com.ikanow.infinit.e.data_model.store.social.sharing.SharePojo.ShareCommunityPojo;
import com.ikanow.infinit.e.data_model.store.social.sharing.SharePojo.ShareOwnerPojo;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;
import com.mongodb.util.JSON;

public class ShareHandler 
{
	private static final Logger logger = Logger.getLogger(ShareHandler.class);
	
	/**
	 * getShare
	 * Retrieve an individual share by id
	 * @param shareIdStr
	 * @return
	 */
	public ResponsePojo getShare(String userIdStr, String shareIdStr, boolean returnContent)
	{
		ResponsePojo rp = new ResponsePojo();
		
		BasicDBObject query = new BasicDBObject("_id", new ObjectId(shareIdStr));
		HashSet<ObjectId> memberOf = null;
		
		if (!RESTTools.adminLookup(userIdStr)) { // (admins can see all shares)			
			memberOf = RESTTools.getUserCommunities(userIdStr);
			if (null != memberOf) {
				query.put("communities._id", new BasicDBObject("$in", memberOf));
			}
			else { // (some error but we'll let this go if the share has no security)
				query.put("communities", new BasicDBObject("$exists", false));
			}
		}
		
		try 
		{
			BasicDBObject dbo = (BasicDBObject)DbManager.getSocial().getShare().findOne(query);
			if (null != dbo) {
				byte[] bytes = (byte[]) dbo.remove("binaryData");
				
				SharePojo share = SharePojo.fromDb(dbo, SharePojo.class);
				share.setBinaryData(bytes);
				
				//new way of handling bytes, if has a binaryid, get bytes from there and set
	
				if ( ( share.getBinaryId() != null ) && returnContent ) {
					share.setBinaryData(getGridFile(share.getBinaryId()));
				}
				else if (!returnContent) {
					share.setShare(null);
				}
				
				rp.setData(share, new SharePojoApiMap(memberOf));				
				
				rp.setResponse(new ResponseObject("Share", true, "Share returned successfully"));
			}
			else {
				rp.setResponse(new ResponseObject("Get Share", false, "Unable to get share, not found or no access permission"));				
			}
		} 
		catch (Exception e) 
		{
			logger.error("Exception Message: " + e.getMessage(), e);
			rp.setResponse(new ResponseObject("Get Share", false, "Unable to get share: " + e.getMessage()));
		}
		return rp;
	}
	
	
	/**
	 * searchShares
	 * @param personIdStr
	 * @param searchby
	 * @param idStrList
	 * @param sharetypes
	 * @param skip
	 * @param limit
	 * @return
	 */
	public ResponsePojo searchShares(String personIdStr, String searchby, String idStrList, String sharetypes, String skip, String limit)
	{
		ResponsePojo rp = new ResponsePojo();
		
		///////////////////////////////////////////////////////////////////////////////////////////////
		// Sample search queries
		// share/search/
		// share/search/?type=binary
		// share/search/?searchby=person&id=admin_infinite@ikanow.com
		// share/search/?searchby=community&id=4d88d0f1f9a624a4b0c8bd71,4d88d0f1f9a624a4b0c8bd72&type=dataset&skip=0&limit=10
		///////////////////////////////////////////////////////////////////////////////////////////////
		
		// Create Query Object
		BasicDBObject query = new BasicDBObject();
		
		HashSet<ObjectId> memberOf = RESTTools.getUserCommunities(personIdStr);
			// (need this to sanitize share communities even if searching explicitly by community) 
		
		// Community search, supports one or more communities._id values
		if ((searchby != null) && (searchby.equalsIgnoreCase("community") || searchby.equalsIgnoreCase("communities")))
		{
			if (idStrList != null && idStrList.length() > 0)
			{
				idStrList = allowCommunityRegex(personIdStr, idStrList); // (allows regex)
				
				// List of communities to search on
				String[] idStrArray = idStrList.split(",");
				List<ObjectId> communityIds = new ArrayList<ObjectId>();
				for (String idStr : idStrArray)
				{
					try {
						ObjectId id = new ObjectId(idStr);
						if ((null != memberOf) && (memberOf.contains(id))) {
							communityIds.add(id); 
						}
					}
					catch (Exception e) {}
				}
				if (communityIds.size() > 0) {
					BasicDBObject communities = new BasicDBObject();
					communities.append("$in", communityIds);
					query.put("communities._id", communities);
				}
				else { // (some error but we'll let this go if the share has no security)
					query.put("communities", new BasicDBObject("$exists", false));						
				}
			}
			else { // (some error but we'll let this go if the share has no security)
				query.put("communities", new BasicDBObject("$exists", false));
			}			
			//TESTED regex and list versions (single and multiple), no allowed commmunity versions
		}
		else if ((searchby != null) && searchby.equalsIgnoreCase("person"))
		{
			if (!RESTTools.adminLookup(personIdStr) || (null == idStrList)) { // not admin or no ids spec'd
				
				query.put("owner._id", new ObjectId(personIdStr));
			}//TESTED
			else { // admin and spec'd - can either be an array of ids or an array of email addresses
				memberOf = null; // (also returns all the communities in the mapper below)
				
				// List of communities to search on
				String[] idStrArray = idStrList.split(",");
				List<ObjectId> peopleIds = new ArrayList<ObjectId>();
				for (String idStr : idStrArray)
				{
					try {
						ObjectId id = new ObjectId(idStr);
						peopleIds.add(id); 
					}
					catch (Exception e) { // Try as people's email addresses
						query.put("owner.email", new BasicDBObject("$in", idStrArray));
						peopleIds.clear();
						break;
					}//TESTED
				}
				if (peopleIds.size() > 0) {
					BasicDBObject communities = new BasicDBObject();
					communities.append("$in", peopleIds);
					query.put("owner._id", communities);
				}//TESTED
			}
			//TESTED: nobody, ids, emails
		}
		else { // Defaults to all communities to which a user belongs (or everything for admins)
			
			if (!RESTTools.adminLookup(personIdStr)) {			
				if (null != memberOf) {
					query.put("communities._id", new BasicDBObject("$in", memberOf));
				}
				else { // (some error but we'll let this go if the share has no security)
					query.put("communities", new BasicDBObject("$exists", false));
				}
			}
			else {
				memberOf = null; // (also returns all the communities in the mapper below)
			}
		}
		
		// Search on share type or types
		if (sharetypes != null && sharetypes.length() > 0)
		{
			if (sharetypes.split(",").length > 1)
			{
				BasicDBObject types = new BasicDBObject();
				String[] typeArray = sharetypes.split(",");
				types.append("$in", typeArray);
				query.put("type", types);
			}
			else
			{
				query.put("type", sharetypes);
			}
		}
		
		//REMOVING BINARY, if you want to return it you can't do deserialize on it
		BasicDBObject removeFields = new BasicDBObject("binaryData",false);
		
		try 
		{
			DBCursor dbc = null;
			
			// Attempt to parse skip and limit into ints if they aren't null
			int numToSkip = 0;
			int limitToNum = 0;
			if (skip != null) try { numToSkip = Integer.parseInt(skip); } catch (Exception e) {}
			if (limit != null) try { limitToNum = Integer.parseInt(limit); } catch (Exception e) {}
			
			// Run find query based on whether or not to skip and limit the number of results to return
			if (skip != null && limit != null)
			{
				dbc = (DBCursor)DbManager.getSocial().getShare().find(query,removeFields).skip(numToSkip).limit(limitToNum);
			}
			else if (skip != null && limit == null)
			{
				dbc = (DBCursor)DbManager.getSocial().getShare().find(query,removeFields).skip(numToSkip);
			}
			else if (skip == null && limit != null)
			{
				dbc = (DBCursor)DbManager.getSocial().getShare().find(query,removeFields).limit(limitToNum);
			}
			else
			{
				dbc = (DBCursor)DbManager.getSocial().getShare().find(query,removeFields);
			}
			
			List<SharePojo> shares = SharePojo.listFromDb(dbc, SharePojo.listType());
			if (!shares.isEmpty()) {
				rp.setData(shares, new SharePojoApiMap(memberOf));
				rp.setResponse(new ResponseObject("Share", true, "Shares returned successfully"));				
			}
			else {
				rp.setResponse(new ResponseObject("Share", true, "No shares matching search critera found."));				
			}
		} 
		catch (Exception e) 
		{
			logger.error("Exception Message: " + e.getMessage(), e);
			rp.setResponse(new ResponseObject("Share", false, "Unable to search for shares: " + e.getMessage()));
		}
		return rp;
	}
	
	
	
	/**
	 * addShare
	 * @param ownerIdStr
	 * @param type
	 * @param title
	 * @param description
	 * @param json
	 * @return
	 */
	public ResponsePojo addShare(String ownerIdStr, String type, String title, String description, String json)
	{
		ResponsePojo rp = new ResponsePojo();
		try
		{
			DBObject o = (DBObject) JSON.parse(json);
			if (null == o)
			{
				rp.setResponse(new ResponseObject("Share", false, "Share object is not a valid JSON object."));
			}
			else
			{
				// Create a new SharePojo object
				SharePojo share = new SharePojo();
				share.setCreated(new Date());
				share.setModified(new Date());
				share.setType(type);
				share.setTitle(title);
				share.setDescription(description);
				share.setShare(json);

				// Get ShareOwnerPojo object and personal community
				PersonPojo owner = getPerson(new ObjectId(ownerIdStr));
				share.setOwner(getOwner(owner));
				share.setCommunities(getPersonalCommunity(owner));

				// Serialize the ID and Dates in the object to MongoDB format
				// Save the document to the share collection
				DbManager.getSocial().getShare().save(share.toDb());
				rp.setResponse(new ResponseObject("Share", true, "New share added successfully."));
			}
		}
		catch (Exception e)
		{
			logger.error("Exception Message: " + e.getMessage(), e);
			rp.setResponse(new ResponseObject("Share", false, "Unable to add share: " + e.getMessage()));
		}
		return rp;
	}
	
	
	/**
	 * addBinary
	 * @param ownerIdStr
	 * @param type
	 * @param title
	 * @param description
	 * @param mediatype
	 * @param bytes
	 * @return
	 */
	public ResponsePojo addBinary(String ownerIdStr, String type, String title, String description, String mediatype, byte[] bytes)
	{
		ResponsePojo rp = new ResponsePojo();		
		try
		{			
			// Create a new SharePojo object
			SharePojo share = new SharePojo();
			share.setCreated(new Date());
			share.setModified(new Date());
			share.setType(type);
			share.setTitle(title);
			share.setDescription(description);			
			//share.setBinaryData(bytes);
			share.setMediaType(mediatype);
			ObjectId id = new ObjectId();
			share.set_id(id);
			
			// Get ShareOwnerPojo object and personal community
			PersonPojo owner = getPerson(new ObjectId(ownerIdStr));
			share.setOwner(getOwner(owner));
			share.setCommunities(getPersonalCommunity(owner));

			// Serialize the ID and Dates in the object to MongoDB format
			//Save the binary objects into separate db
			ObjectId gridid = saveGridFile(bytes);
			share.setBinaryId(gridid);
			
			// Save the document to the share collection
			DbManager.getSocial().getShare().save(share.toDb());
			rp.setResponse(new ResponseObject("Share", true, "New binary share added successfully. ID in data field"));
			rp.setData(id.toString(), null);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			logger.error("Exception Message: " + e.getMessage(), e);
			rp.setResponse(new ResponseObject("Share", false, "Unable to add share: " + e.getMessage()));
		}
		return rp;
	}

	/**
	 * updateBinary
	 * @param ownerIdStr
	 * @param shareIdStr
	 * @param type
	 * @param title
	 * @param description
	 * @param mediatype
	 * @param bytes
	 * @return
	 */
	public ResponsePojo updateBinary(String ownerIdStr, String shareIdStr, String type, String title, String description, String mediatype, byte[] bytes)
	{
		ResponsePojo rp = new ResponsePojo();
		try
		{			
			//get old share
			BasicDBObject query = new BasicDBObject("_id",new ObjectId(shareIdStr));
			DBObject dboshare = DbManager.getSocial().getShare().findOne(query);
			if ( dboshare != null )
			{
				//write everything but binary
				dboshare.removeField("binaryData");
				SharePojo share = SharePojo.fromDb(dboshare, SharePojo.class);
				// Check ... am I the owner?
				ObjectId ownerId = new ObjectId(ownerIdStr);
				if (!share.getOwner().get_id().equals(ownerId)) { // Then I have to be admin
					if (!RESTTools.adminLookup(ownerIdStr)) {
						rp.setResponse(new ResponseObject("Update Share",false,"Shareid does not exist or you are not owner or admin"));
						return rp;
					}					
				}
				
				share.setModified(new Date());
				share.setType(type);
				share.setTitle(title);
				share.setDescription(description);
				share.setMediaType(mediatype);
				share.setBinaryData(null);
				share.setBinaryId(updateGridFile(share.getBinaryId(), bytes));
				
				DbManager.getSocial().getShare().update(query, share.toDb());
				
				rp.setResponse(new ResponseObject("Update Share", true, "Binary share updated successfully"));
			}
			else
			{
				rp.setResponse(new ResponseObject("Update Share",false,"Shareid does not exist or you are not owner or admin"));
			}			
		}
		catch(Exception e)
		{
			logger.error("Exception Message: " + e.getMessage(), e);
			rp.setResponse(new ResponseObject("Update Share", false, "Unable to update share: " + e.getMessage()));
		}
		return rp;
	}

	
	/**
	 * saveJson
	 * @param shareIdStr
	 * @param type
	 * @param title
	 * @param description
	 * @param json
	 * @return
	 */
	public ResponsePojo saveJson(String ownerIdStr, String shareIdStr, String type, String title, String description, String json)
	{
		ResponsePojo rp = new ResponsePojo();
		try
		{
			SharePojo share = null;
			BasicDBObject dbo = null;
			BasicDBObject query = null;

			// Retrieve the share to update (if it exists)
			if (shareIdStr != null && ObjectId.isValid(shareIdStr))
			{
				query = new BasicDBObject();
				query.put("_id", new ObjectId(shareIdStr));
				dbo = (BasicDBObject)DbManager.getSocial().getShare().findOne(query);
			}
			else
			{
				shareIdStr = new ObjectId().toString();
			}
			
			// Update existing share
			if (dbo != null)
			{
				share = SharePojo.fromDb(dbo, SharePojo.class);
				// Check ... am I the owner?
				ObjectId ownerId = new ObjectId(ownerIdStr);
				if (!share.getOwner().get_id().equals(ownerId)) { // Then I have to be admin
					if (!RESTTools.adminLookup(ownerIdStr)) {
						rp.setResponse(new ResponseObject("Update Share",false,"Unable to update share: you are not owner or admin"));
						return rp;
					}					
				}
				
				share.setModified(new Date());
				share.setType(type);
				share.setTitle(title);
				share.setDescription(description);
				share.setShare(json);
				
				// Save the document to the share collection
				DbManager.getSocial().getShare().update(query, share.toDb());
				rp.setResponse(new ResponseObject("Share", true, "Share updated successfully."));
			}
			// Create new share
			else
			{
				// Create a new SharePojo object
				share = new SharePojo();
				share.set_id(new ObjectId(shareIdStr));
				share.setCreated(new Date());
				share.setModified(new Date());
				share.setType(type);
				share.setTitle(title);
				share.setDescription(description);
				share.setShare(json);
				
				// Get ShareOwnerPojo object and personal community
				PersonPojo owner = getPerson(new ObjectId(ownerIdStr));
				share.setOwner(getOwner(owner));
				share.setCommunities(getPersonalCommunity(owner));

				// Serialize the ID and Dates in the object to MongoDB format
				// Save the document to the share collection
				DbManager.getSocial().getShare().save(share.toDb());
				rp.setResponse(new ResponseObject("Share", true, "New share added successfully."));
			}
		}
		catch (Exception e)
		{
			logger.error("Exception Message: " + e.getMessage(), e);
			rp.setResponse(new ResponseObject("Share", false, "Unable to update share: " + e.getMessage()));
		}
		return rp;
	}
	
	
	
	/**
	 * addRef
	 * @param ownerIdStr
	 * @param type
	 * @param idStr
	 * @param description
	 * @return
	 */
	public ResponsePojo addRef(String ownerIdStr, String type, String idStr, String title, String description)
	{
		ResponsePojo rp = new ResponsePojo();
		try
		{
			ObjectId shareId = new ObjectId();

			// Create a new SharePojo object
			SharePojo share = new SharePojo();
			share.set_id(shareId);
			share.setType(type);
			share.setTitle(title);
			share.setDescription(description);
			share.setCreated(new Date());
			share.setModified(new Date());
			
			// Create DocumentLocationPojo and add to the share
			DocumentLocationPojo documentLocation = new DocumentLocationPojo();
			documentLocation.set_id(new ObjectId(idStr));
			documentLocation.setCollection(type.toLowerCase());
			
			// This should be a type from the published data model
			documentLocation.setDatabase(type.toLowerCase());
			//////////////////////////////////////////////////////////////////////////
			// TODO (INF-1299): Implement code to validate that document referenced exists
//			if (isValidRef(documentLocation))
//			{
//				share.setDocumentLocation(documentLocation);
//			}
//			else
//			{
//				rp.setResponse(new ResponseObject("Share", false, "Unable to add new share: the reference specified "+
//						" (Type: " + type + ", ID: " + id + ") does not exist in the database"));
//				return rp;
//			}
			
			share.setDocumentLocation(documentLocation);
			
			// Get ShareOwnerPojo object
			PersonPojo owner = getPerson(new ObjectId(ownerIdStr));
			share.setOwner(getOwner(owner));

			// Set Personal Community
			share.setCommunities(getPersonalCommunity(owner));

			// Serialize the ID and Dates in the object to MongoDB format
			// Save the document to the share collection
			DbManager.getSocial().getShare().save(share.toDb());
			rp.setResponse(new ResponseObject("Share", true, "New share added successfully."));
		}
		catch (Exception e)
		{
			logger.error("Exception Message: " + e.getMessage(), e);
			rp.setResponse(new ResponseObject("Share", false, "Unable to add new share: " + e.getMessage()));
		}
		return rp;
	}
	
	
	public ResponsePojo updateRef(String ownerIdStr, String shareIdStr, String type, String idStr, String title, String description)
	{
		ResponsePojo rp = new ResponsePojo();
		try
		{
			// Get Share Object from Collection
			BasicDBObject query = new BasicDBObject();
			query.put("_id", new ObjectId(shareIdStr));

			BasicDBObject dbo = (BasicDBObject)DbManager.getSocial().getShare().findOne(query);
			if (dbo != null)
			{
				SharePojo share = SharePojo.fromDb(dbo, SharePojo.class);
				// Check ... am I the owner?
				ObjectId ownerId = new ObjectId(ownerIdStr);
				if (!share.getOwner().get_id().equals(ownerId)) { // Then I have to be admin
					if (!RESTTools.adminLookup(ownerIdStr)) {
						rp.setResponse(new ResponseObject("Share", false, "Unable to update share: only the owner of the share or admin can update it."));
						return rp;
					}					
				}

				share.setType(type);
				share.setTitle(title);
				share.setDescription(description);
				share.setModified(new Date());

				// Create DocumentLocationPojo and add to the share
				DocumentLocationPojo documentLocation = new DocumentLocationPojo();
				documentLocation.set_id(new ObjectId(idStr));
				documentLocation.setCollection(type.toLowerCase());

				// This should be a type from the published data model
				documentLocation.setDatabase(type.toLowerCase());
				//////////////////////////////////////////////////////////////////////////
				// TODO (INF-1299): Implement code to validate that document referenced exists
				//			if (isValidRef(documentLocation))
				//			{
				//				share.setDocumentLocation(documentLocation);
				//			}
				//			else
				//			{
				//				rp.setResponse(new ResponseObject("Share", false, "Unable to add new share: the reference specified "+
				//						" (Type: " + type + ", ID: " + id + ") does not exist in the database"));
				//				return rp;
				//			}

				share.setDocumentLocation(documentLocation);

				// Get ShareOwnerPojo object
				PersonPojo owner = getPerson(new ObjectId(ownerIdStr));
				share.setOwner(getOwner(owner));

				// Set Personal Community
				share.setCommunities(getPersonalCommunity(owner));

				// Serialize the ID and Dates in the object to MongoDB format
				// Save the document to the share collection
				DbManager.getSocial().getShare().update(query, share.toDb());
				rp.setResponse(new ResponseObject("Share", true, "Share updated successfully."));
			}
			else
			{
				rp.setResponse(new ResponseObject("Share", false, "Unable to update share: only the owner of the share or admin can update it."));
				return rp;
			}
		}
		catch (Exception e)
		{
			logger.error("Exception Message: " + e.getMessage(), e);
			rp.setResponse(new ResponseObject("Share", false, "Unable to update share: " + e.getMessage()));
		}
		return rp;
	}
	

	
	/**
	 * removeShare
	 * Remove an individual share from the share collection, shares can only be removed by the
	 * owner via the rest API (id retrieved via cookielookup).
	 * @param shareIdStr
	 * @return
	 */
	public ResponsePojo removeShare(String ownerIdStr, String shareIdStr)
	{
		ResponsePojo rp = new ResponsePojo();
		// Query = share._id and share.shareOwner._id (unless admin)
		BasicDBObject query = new BasicDBObject();
		query.put("_id", new ObjectId(shareIdStr));
		mustBeOwnerOrAdmin(ownerIdStr, query);
		
		try 
		{
			//first we must get the share to see if we need to remove the binary portion
			SharePojo sp = SharePojo.fromDb(DbManager.getSocial().getShare().findOne(query),SharePojo.class);			
			WriteResult wr = DbManager.getSocial().getShare().remove(query);
			if (wr.getN() ==  1)
			{
				//if remvoe was successful, attempt to remove the gridfs entry
				if ( sp.getBinaryId() != null )
				{
					//remove gridfs
					DbManager.getSocial().getShareBinary().remove(sp.getBinaryId());
				}
				rp.setResponse(new ResponseObject("Share", true, "Share removed from database successfully"));
			}
			else
			{
				rp.setResponse(new ResponseObject("Share", false, "Unable to remove share from database successfully (reason: share does not exist or user does not own the share)."));
			}
		} 
		catch (Exception e) 
		{
			logger.error("Exception Message: " + e.getMessage(), e);
			rp.setResponse(new ResponseObject("Share", false, "Unable to remove share: " + e.getMessage()));
		}
		return rp;
	}
	
	
	
	/**
	 * addCommunity
	 * @param shareIdStr
	 * @param communityIdStr
	 * @param comment
	 * @return
	 */
	public ResponsePojo addCommunity(String ownerIdStr, String shareIdStr, String communityIdStr, String comment)
	{
		// First get the share document from the database (only works for the share owner)
		ResponsePojo rp = new ResponsePojo();
		
		BasicDBObject query = new BasicDBObject();
		query.put("_id", new ObjectId(shareIdStr));
		mustBeOwnerOrAdmin(ownerIdStr, query);
		
		try 
		{
			communityIdStr = allowCommunityRegex(ownerIdStr, communityIdStr);
			
			BasicDBObject dbo = (BasicDBObject)DbManager.getSocial().getShare().findOne(query);
			if (dbo != null)
			{
				SharePojo share = SharePojo.fromDb(dbo, SharePojo.class);	

				List<ShareCommunityPojo> communities = share.getCommunities();
				if (null == communities) {
					communities = new ArrayList<ShareCommunityPojo>();
				}

				// Check to see if the community is already in share.communities
				Boolean addCommunity = true;
				for (ShareCommunityPojo scp : communities)
				{
					if (scp.get_id().toString().equalsIgnoreCase(communityIdStr)) addCommunity = false;
				}

				// Add new community to communities
				if (addCommunity)
				{
					ShareCommunityPojo cp = new ShareCommunityPojo();
					cp.set_id(new ObjectId(communityIdStr));
					cp.setName(getCommunity(new ObjectId(communityIdStr)).getName());
					cp.setComment(comment);
					communities.add(cp);

					share.setModified(new Date());

					DbManager.getSocial().getShare().update(query, share.toDb());
					rp.setResponse(new ResponseObject("Share", false, "Community successfully added to the share"));
				}
				// Community already in share.communities
				else
				{
					rp.setResponse(new ResponseObject("Share", false, "Community has already been added to the share"));
				}
			}
			else
			{
				rp.setResponse(new ResponseObject("Share", false, "Unable to add community to share."));
			}
		} 
		catch (Exception e) 
		{
			logger.error("Exception Message: " + e.getMessage(), e);
			rp.setResponse(new ResponseObject("Share", false, "Unable to add community to share: " + e.getMessage()));
		}
		return rp;
	}
	
	
	
	/**
	 * removeCommunity
	 * @param ownerIdStr
	 * @param shareIdStr
	 * @param communityIdStr
	 * @return
	 */
	public ResponsePojo removeCommunity(String ownerIdStr, String shareIdStr, String communityIdStr)
	{
		// First get the share document from the database (only works for the share owner)
		ResponsePojo rp = new ResponsePojo();
		
		BasicDBObject query = new BasicDBObject();
		query.put("_id", new ObjectId(shareIdStr));
		mustBeOwnerOrAdmin(ownerIdStr, query);
		
		try 
		{
			communityIdStr = allowCommunityRegex(ownerIdStr, communityIdStr);
			
			BasicDBObject dbo = (BasicDBObject)DbManager.getSocial().getShare().findOne(query);
			if (dbo != null)
			{
				SharePojo share = SharePojo.fromDb(dbo, SharePojo.class);	

				List<ShareCommunityPojo> communities = share.getCommunities();

				// Check to see if the community is already in share.communities
				boolean removeCommunity = false;
				for (ShareCommunityPojo scp : communities)
				{
					if (scp.get_id().toString().equalsIgnoreCase(communityIdStr)) 
					{
						removeCommunity = true;
						communities.remove(scp);
						share.setModified(new Date());
						DbManager.getSocial().getShare().update(query, share.toDb());
						rp.setResponse(new ResponseObject("Share", true, "Community successfully removed from the share"));
						break;
					}
				}

				if (!removeCommunity)
				{
					rp.setResponse(new ResponseObject("Share", false, "Unable to remove community (does not exist in share)"));
				}
			}
			else
			{
				rp.setResponse(new ResponseObject("Share", false, "Unable to remove community from share."));
			}
		} 
		catch (Exception e) 
		{
			logger.error("Exception Message: " + e.getMessage(), e);
			rp.setResponse(new ResponseObject("Share", false, "Unable to remove community from share: " + e.getMessage()));
		}
		return rp;
	}
	

	//////////////////////////////////////////////////////////////////////////
	//////////////////////// Private Helper Functions ////////////////////////
	//////////////////////////////////////////////////////////////////////////
	
	/**
	 * getPerson
	 * @param idStr
	 * @return
	 */
	private static PersonPojo getPerson(ObjectId id)
	{
		//
		BasicDBObject query = new BasicDBObject();
		query.put("_id", id);
		
		PersonPojo person = null;
		try
		{
			DBObject dbo = DbManager.getSocial().getPerson().findOne(query);

			if (dbo != null) 
			{
				// Get GsonBuilder object with MongoDb de/serializers registered
				person = PersonPojo.fromDb(dbo, PersonPojo.class);
			}
		}
		catch (Exception e) {}
		return person;
	}
	
	
	/**
	 * getShareOwner
	 * @param ownerId
	 * @return
	 */
	private ShareOwnerPojo getOwner(PersonPojo owner)
	{
		// Set ShareOwner 
		ShareOwnerPojo shareOwner = new ShareOwnerPojo();
		shareOwner.set_id(owner.get_id());
		shareOwner.setDisplayName(owner.getDisplayName());
		shareOwner.setEmail(owner.getEmail());
		return shareOwner;
	}
	
	/**
	 * getPersonalCommunity
	 * @param owner
	 * @return
	 */
	private List<ShareCommunityPojo> getPersonalCommunity(PersonPojo owner)
	{
		ShareCommunityPojo cp = new ShareCommunityPojo();
		for (PersonCommunityPojo pc : owner.getCommunities())
		{
			if (pc.get_id().equals(owner.get_id()))
			{
				cp.set_id(pc.get_id());
				cp.setComment(null);
				cp.setName(pc.getName());
			}
		}

		// Add the owner's personal community
		List<ShareCommunityPojo> communities = new ArrayList<ShareCommunityPojo>();
		communities.add(cp);
		return communities;
	}
	
	
	/**
	 * getCommunity
	 * @param idStr
	 * @return
	 */
	private static CommunityPojo getCommunity(ObjectId id)
	{
		//
		BasicDBObject query = new BasicDBObject();
		query.put("_id", id);
		
		CommunityPojo community = null;
		try
		{
			DBObject dbo = DbManager.getSocial().getCommunity().findOne(query);

			if (dbo != null) 
			{
				// Get GsonBuilder object with MongoDb de/serializers registered
				community = CommunityPojo.fromDb(dbo, CommunityPojo.class);
			}
		}
		catch (Exception e) {}
		return community;
	}
	
	/**
	 * Finds the gridfile given by id and returns the bytes
	 * 
	 * @param id the object id of the gridfile to lookup (stored in sharepojo)
	 * @return bytes of file stored in gridfile
	 */	
	private byte[] getGridFile(ObjectId id)
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try
		{
			GridFSDBFile file = DbManager.getSocial().getShareBinary().find(id);						
			file.writeTo(out);
			byte[] toReturn = out.toByteArray();
			out.close();
			return toReturn;
		}
		catch (Exception ex){}		
		return null;
	}
	
	@SuppressWarnings("unused")
	private ByteArrayOutputStream getGridStream(ObjectId id)
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try
		{
			GridFSDBFile file = DbManager.getSocial().getShareBinary().find(id);	
			file.writeTo(out);				
		}
		catch (Exception ex){}		
		return out;
	}
	
	/**
	 * Saves bytes into a new gridfile
	 * 
	 * @param bytes 
	 * @return the id of the gridfile
	 */
	private ObjectId saveGridFile(byte[] bytes)
	{
		try
		{
			GridFSInputFile file = DbManager.getSocial().getShareBinary().createFile(bytes);
			file.save();
			return (ObjectId) file.getId();
		}
		catch (Exception ex){}
		return null;
	}
	
	/**
	 * Updates a gridfile with new data, if binaryId is null
	 * the old gridfile did not exist, just create a new one.
	 * 
	 * If it is not null, will remove and create a new entry.
	 * 
	 * @param binaryId
	 * @param bytes
	 * @return
	 */
	private ObjectId updateGridFile(ObjectId binaryId, byte[] bytes) 
	{
		try
		{
			//remove old file if exists
			if ( binaryId != null )
				DbManager.getSocial().getShareBinary().remove(binaryId);
			//create new file
			GridFSInputFile file = DbManager.getSocial().getShareBinary().createFile(bytes);
			file.save();
			return (ObjectId) file.getId();
		}
		catch (Exception ex){}
		return null;
	}

	static private void mustBeOwnerOrAdmin(String userIdStr, BasicDBObject query) {
		if (!RESTTools.adminLookup(userIdStr)) {
			query.put("owner._id", new ObjectId(userIdStr));			
		}
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
