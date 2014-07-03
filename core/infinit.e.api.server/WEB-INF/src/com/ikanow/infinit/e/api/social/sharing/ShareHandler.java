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
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.bson.types.ObjectId;

import com.ikanow.infinit.e.api.utils.SocialUtils;
import com.ikanow.infinit.e.api.utils.MimeUtils;
import com.ikanow.infinit.e.api.utils.RESTTools;
import com.ikanow.infinit.e.data_model.api.ResponsePojo;
import com.ikanow.infinit.e.data_model.api.ResponsePojo.ResponseObject;
import com.ikanow.infinit.e.data_model.api.social.sharing.SharePojoApiMap;
import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.MongoDbManager;
import com.ikanow.infinit.e.data_model.store.custom.mapreduce.CustomMapReduceJobPojo;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.infinit.e.data_model.store.social.community.CommunityPojo;
import com.ikanow.infinit.e.data_model.store.social.person.PersonCommunityPojo;
import com.ikanow.infinit.e.data_model.store.social.person.PersonPojo;
import com.ikanow.infinit.e.data_model.store.social.sharing.SharePojo;
import com.ikanow.infinit.e.data_model.store.social.sharing.SharePojo.DocumentLocationPojo;
import com.ikanow.infinit.e.data_model.store.social.sharing.SharePojo.ShareCommunityPojo;
import com.ikanow.infinit.e.data_model.store.social.sharing.SharePojo.ShareOwnerPojo;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;

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
			memberOf = SocialUtils.getUserCommunities(userIdStr);
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
	
				if (returnContent) {					
					if (null != share.getBinaryId()) {
						share.setBinaryData(getGridFile(share.getBinaryId()));						
					}//TESTED
					else if (null != share.getDocumentLocation()) {
						try {
							if ((null != share.getType()) && share.getType().equalsIgnoreCase("binary")) {
								File x = new File(share.getDocumentLocation().getCollection());
								share.setBinaryData(FileUtils.readFileToByteArray(x));
							}//TESTED
							else { // text
								share.setShare(getReferenceString(share));								
							}//TESTED
						}
						catch (Exception e) {
							rp.setResponse(new ResponseObject("Get Share", false, "Unable to get share reference: " + e.getMessage()));						
							return rp;
						}//TESTED
					}	
					// (else share.share already set)
				}
				else { // don't return content
					share.setShare(null);					
				}//TESTED
				
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
	
	//TODO (): be able to specify not returning content? 
	
	public ResponsePojo searchShares(String personIdStr, String searchby, String idStrList, String sharetypes, String skip, String limit, boolean ignoreAdmin, boolean returnContent, boolean searchParent)
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
		
		HashSet<ObjectId> memberOf = SocialUtils.getUserCommunities(personIdStr);
			// (need this to sanitize share communities even if searching explicitly by community) 
		
		boolean bAdmin = false;
		if (!ignoreAdmin) {
			bAdmin = RESTTools.adminLookup(personIdStr);
		}
		
		// Community search, supports one or more communities._id values
		if ((searchby != null) && (searchby.equalsIgnoreCase("community") || searchby.equalsIgnoreCase("communities")))
		{
			query = getCommunityQueryObject(query, idStrList, personIdStr, ignoreAdmin, bAdmin, memberOf, searchParent);					
			//TESTED regex and list versions (single and multiple), no allowed commmunity versions
		}
		else if ((searchby != null) && searchby.equalsIgnoreCase("person"))
		{
			if ((ignoreAdmin || !bAdmin) || (null == idStrList)) { // not admin or no ids spec'd
				
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
			
			if (ignoreAdmin || !bAdmin) {			
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
		if (!returnContent) {
			removeFields.put("share", false);
		}
		
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
				
				Iterator<SharePojo> shareIt = shares.iterator();
				while (shareIt.hasNext()) {
					SharePojo share = shareIt.next();
					if (null != share.getDocumentLocation()) {
						try {
							if ((null == share.getType()) || !share.getType().equalsIgnoreCase("binary")) {
								// (ignore binary references)
								share.setShare(this.getReferenceString(share));
							}//TESTED
						}
						catch (Exception e) { // couldn't access data, just remove data from list
							share.setShare("{}");
						}
					}
				}//TESTED
				
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
	
	private BasicDBObject getCommunityQueryObject(BasicDBObject query, String idStrList, String personIdStr, boolean ignoreAdmin, boolean bAdmin, HashSet<ObjectId> memberOf, boolean searchParent)
	{		
		if (idStrList != null && idStrList.length() > 0)
		{
			idStrList = allowCommunityRegex(personIdStr, idStrList, true); // (allows regex, plus multiple communities)
			// List of communities to search on
			String[] idStrArray = idStrList.split(",");
			List<ObjectId> communityIds = new ArrayList<ObjectId>();
			for (String idStr : idStrArray)
			{
				try
				{
					ObjectId id = new ObjectId(idStr);
					if ( isMemberOwnerAdminOfCommunity(id, ignoreAdmin, bAdmin, memberOf) )
					{
						communityIds.add(id);
					}
				}
				catch(Exception ex) {}
			}
			if (communityIds.size() > 0) 
			{
				if ( searchParent )
				{
					//get the comm objects for these communities, so we can grab all their parents and add them to the query					
					BasicDBObject communities = new BasicDBObject();
					communities.append("$in", getParentIds(communityIds, personIdStr, ignoreAdmin, bAdmin, memberOf));
					query.put("communities._id", communities);
				}
				else
				{
					//otherwise just use the given commids
					BasicDBObject communities = new BasicDBObject();
					communities.append("$in", communityIds);
					query.put("communities._id", communities);
				}
			}
			else 
			{ // (some error but we'll let this go if the share has no security)
				query.put("communities", new BasicDBObject("$exists", false));						
			}
		}
		else 
		{ // (some error but we'll let this go if the share has no security)
			query.put("communities", new BasicDBObject("$exists", false));
		}	
		return query;
	}
	
	/**
	 * Takes a list of communityIds and finds any parentsIds.
	 * The parent id's will either be:
	 * 1. in comm.parentTree, just append
	 * 2. in comm.parentId, create parentTree and update comm, append
	 * 
	 * @param child_shares
	 * @return
	 */
	private List<ObjectId> getParentIds(List<ObjectId> children, String personIdStr, boolean ignoreAdmin, boolean bAdmin, HashSet<ObjectId> memberOf)
	{
		Set<ObjectId> communityIds = new HashSet<ObjectId>();
		List<CommunityPojo> child_communities = CommunityPojo.listFromDb( MongoDbManager.getSocial().getCommunity().find(new BasicDBObject("_id", new BasicDBObject(MongoDbManager.in_, children))), CommunityPojo.listType());
		for ( CommunityPojo child_comm : child_communities )
		{
			//this community has a parent
			if ( child_comm.getParentId() != null )
			{
				if ( child_comm.getParentTree() == null )
				{
					child_comm = SocialUtils.createParentTreeRecursion(child_comm, true);					
				}
				
			}
			//parentTree should be populated now, add these comms to the set
			communityIds.add(child_comm.getId());
			if ( child_comm.getParentTree() != null )
			{
				communityIds.addAll(getAllowedParentComms(child_comm.getParentTree(), personIdStr, ignoreAdmin, bAdmin, memberOf));
			}
			
		}
		return new ArrayList<ObjectId>(communityIds);
	}
	
	/**
	 * Returns only the communities that a user is allowed to see.
	 * 
	 * @param parentTree
	 * @param personIdStr
	 * @return
	 */
	private List<ObjectId> getAllowedParentComms(List<ObjectId> parentTree, String personIdStr, boolean ignoreAdmin, boolean bAdmin, HashSet<ObjectId> memberOf)
	{
		List<ObjectId> allowedParents = new ArrayList<ObjectId>();
		for ( ObjectId parent_id : parentTree )
		{
			if ( isMemberOwnerAdminOfCommunity(parent_id, ignoreAdmin, bAdmin, memberOf) )
			{
				allowedParents.add(parent_id);
			}
		}		
		return allowedParents;
	}
	
	/**
	 * Return true if user is a (member of the community) or (admin is true and ignoreadmin is false)
	 * 
	 * @param communityId
	 * @param ignoreAdmin
	 * @param bAdmin
	 * @param memberOf
	 * @return
	 */
	private boolean isMemberOwnerAdminOfCommunity(ObjectId communityId, boolean ignoreAdmin, boolean bAdmin, HashSet<ObjectId> memberOf)
	{
		try 
		{
			if (ignoreAdmin || !bAdmin) 
			{									
				if ((null != memberOf) && (memberOf.contains(communityId))) 
				{
					return true;
				}
			}
			else
			{ // admin, allowed it
				return true; 							
			}
		}
		catch (Exception e) {}
		return false;
	}
	
	//TODO (???): have ability to enforce uniqueness on title/type
	//TODO (???): for updates, have the ability to fail if document has changed in meantime...
	
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
			HashSet<ObjectId> endorsedSet = new HashSet<ObjectId>();
			share.setEndorsed(endorsedSet); // (you're always endorsed within your personal community)
			endorsedSet.add(new ObjectId(ownerIdStr));				
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
				boolean bAdminOrModOfAllCommunities = RESTTools.adminLookup(ownerIdStr);
				if (!share.getOwner().get_id().equals(ownerId)) { // Then I have to be admin (except for one special case)
					if (!bAdminOrModOfAllCommunities) {
						// Special case: I am also community admin/moderator of every community to which this share belongs
						bAdminOrModOfAllCommunities = true;
						for (ShareCommunityPojo comm: share.getCommunities()) {
							if (!SocialUtils.isOwnerOrModerator(comm.get_id().toString(), ownerIdStr)) {
								bAdminOrModOfAllCommunities = false;
							}
						}//TESTED
						
						if (!bAdminOrModOfAllCommunities) {						
							rp.setResponse(new ResponseObject("Update Share",false,"Unable to update share: you are not owner or admin"));
							return rp;
						}
					}					
				}//end if not owner
				
				// Check: am I trying to update a reference or json?
				if (null == share.getBinaryId()) {
					rp.setResponse(new ResponseObject("Update Share",false,"Unable to update share: this is not a binary share"));
					return rp;					
				}

				if (!bAdminOrModOfAllCommunities) { // quick check whether I'm admin on-request - if so can endorse
					bAdminOrModOfAllCommunities = RESTTools.adminLookup(ownerIdStr, false);
				}//TESTED
				
				// Remove endorsements unless I'm admin (if I'm not admin I must be owner...)
				if (!bAdminOrModOfAllCommunities) { // Now need to check if I'm admin/mod/content publisher for each community..
					if (null == share.getEndorsed()) { // fill this with all allowed communities
						share.setEndorsed(new HashSet<ObjectId>());
						share.getEndorsed().add(share.getOwner().get_id()); // (will be added later)
						for (ShareCommunityPojo comm: share.getCommunities()) {
							if (SocialUtils.isOwnerOrModeratorOrContentPublisher(comm.get_id().toString(), ownerIdStr)) {
								share.getEndorsed().add(comm.get_id());
							}
						}
					}//TESTED
					else {
						for (ShareCommunityPojo comm: share.getCommunities()) {
							// (leave it as is except remove anything that I can't endorse)
							if (!SocialUtils.isOwnerOrModeratorOrContentPublisher(comm.get_id().toString(), ownerIdStr)) {
								share.getEndorsed().remove(comm.get_id());
							}					
						}
					}//TESTED	
				}//TESTED
				else {
					if (null == share.getEndorsed()) { // fill this with all communities
						share.setEndorsed(new HashSet<ObjectId>());
						share.getEndorsed().add(share.getOwner().get_id());
						for (ShareCommunityPojo comm: share.getCommunities()) {
							share.getEndorsed().add(comm.get_id());							
						}
					}
					//(else just leave with the same set of endorsements as before)
				}//TESTED
				
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
	 * saveJson (handles both add and update)
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
				// 1a) if I'm the owner then GOTO_UPDATE
				// 1b) if I'm the admin/mod 
				// (NEW) if I am a content publisher for all communities for which this share is read/write
				
				share = SharePojo.fromDb(dbo, SharePojo.class);
				// Check ... am I the owner? 
				ObjectId ownerId = new ObjectId(ownerIdStr);
				boolean bAdminOrModOfAllCommunities = RESTTools.adminLookup(ownerIdStr);
				
				if (!share.getOwner().get_id().equals(ownerId)) { // Then I have to be admin (except for one special case)
					if (!bAdminOrModOfAllCommunities) {
						// Special case #1: I am also community admin/moderator of every community to which this share belongs
						bAdminOrModOfAllCommunities = true;
						for (ShareCommunityPojo comm: share.getCommunities()) {
							if (!SocialUtils.isOwnerOrModerator(comm.get_id().toString(), ownerIdStr)) {
								bAdminOrModOfAllCommunities = false;
							}
						}//TESTED
						
						if (!bAdminOrModOfAllCommunities) {
							// Special case #2: I am a admin/mod/content publisher of *any* community that is read/write
							boolean readWriteCase = false;
							if (null != share.getReadWrite()) {
								// I need to be content publisher across all shares
								for (ObjectId readWriteCommId: share.getReadWrite()) {
									if (SocialUtils.isOwnerOrModeratorOrContentPublisher(readWriteCommId.toString(), ownerIdStr)) {
										readWriteCase = true;
										break;
									}									
								}
							}//TESTED
							
							if (!readWriteCase) {
								rp.setResponse(new ResponseObject("Update Share",false,"Unable to update share: you are not owner or admin"));
								return rp;
							}
						}
					}					
				}//end if not owner
				
				// Check: am I trying to update a reference or binary?
				if ((null != share.getDocumentLocation()) || (null != share.getBinaryId())) {
					rp.setResponse(new ResponseObject("Update Share",false,"Unable to update share: this is not a JSON share"));
					return rp;					
				}
				
				if (!bAdminOrModOfAllCommunities) { // quick check whether I'm admin on-request - if so can endorse
					bAdminOrModOfAllCommunities = RESTTools.adminLookup(ownerIdStr, false);
				}//TESTED
								
				// Remove endorsements unless I'm admin (if I'm not admin I must be owner...)
				if (!bAdminOrModOfAllCommunities) { // Now need to check if I'm admin/mod/content publisher for each community..
					if (null == share.getEndorsed()) { // fill this with all allowed communities
						share.setEndorsed(new HashSet<ObjectId>());
						share.getEndorsed().add(share.getOwner().get_id()); // (will be added later)
						for (ShareCommunityPojo comm: share.getCommunities()) {
							if (SocialUtils.isOwnerOrModeratorOrContentPublisher(comm.get_id().toString(), ownerIdStr)) {
								share.getEndorsed().add(comm.get_id());
							}
						}
					}//TESTED
					else {
						for (ShareCommunityPojo comm: share.getCommunities()) {
							// (leave it as is except remove anything that I can't endorse)
							if (!SocialUtils.isOwnerOrModeratorOrContentPublisher(comm.get_id().toString(), ownerIdStr)) {
								share.getEndorsed().remove(comm.get_id());
							}					
						}
					}//TESTED	
				}//TESTED
				else {
					if (null == share.getEndorsed()) { // fill this with all communities
						share.setEndorsed(new HashSet<ObjectId>());
						share.getEndorsed().add(share.getOwner().get_id());
						for (ShareCommunityPojo comm: share.getCommunities()) {
							share.getEndorsed().add(comm.get_id());							
						}
					}
					//(else just leave with the same set of endorsements as before)
				}//TESTED
				
				share.setModified(new Date());
				share.setType(type);
				share.setTitle(title);
				share.setDescription(description);
				share.setShare(json);
				
				// Save the document to the share collection
				DbManager.getSocial().getShare().update(query, share.toDb());
				rp.setData(share, new SharePojoApiMap(null));
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
				
				HashSet<ObjectId> endorsedSet = new HashSet<ObjectId>();
				share.setEndorsed(endorsedSet); // (you're always endorsed within your own community)
				endorsedSet.add(new ObjectId(ownerIdStr));				
				
				// Get ShareOwnerPojo object and personal community
				PersonPojo owner = getPerson(new ObjectId(ownerIdStr));
				share.setOwner(getOwner(owner));
				share.setCommunities(getPersonalCommunity(owner));

				// Serialize the ID and Dates in the object to MongoDB format
				// Save the document to the share collection
				DbManager.getSocial().getShare().save(share.toDb());
				rp.setData(share, new SharePojoApiMap(null));
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
	public ResponsePojo addRef(String ownerIdStr, String type, String location, String idStr, String title, String description)
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
			setRefLocation(location, documentLocation);
			share.setDocumentLocation(documentLocation);
			if (null == documentLocation.getDatabase()) { // (local file)
				// Check, need to be admin:
				if (!RESTTools.adminLookup(ownerIdStr, false)) {
					rp.setResponse(new ResponseObject("Share", false, "Permission denied: you need to be admin to create a local file ref"));
					return rp;
				}				
				if ((null != type) && (type.equalsIgnoreCase("binary") || type.startsWith("binary:"))) {
					String[] binaryType = type.split(":", 2);
					if (binaryType.length > 1) {
						share.setMediaType(binaryType[1]);
						share.setType("binary");
					}
					else {
						share.setMediaType(MimeUtils.getMimeType(FilenameUtils.getExtension(idStr)));
					}
				}
				documentLocation.setCollection(idStr); // collection==file, database==id==null
			}//TESTED
			else {
				documentLocation.set_id(new ObjectId(idStr));				
			}
			
			// Get ShareOwnerPojo object
			PersonPojo owner = getPerson(new ObjectId(ownerIdStr));
			share.setOwner(getOwner(owner));
			
			// Endorsements:
			HashSet<ObjectId> endorsedSet = new HashSet<ObjectId>();
			share.setEndorsed(endorsedSet); // (you're always endorsed within your own community)
			endorsedSet.add(new ObjectId(ownerIdStr));				

			// Set Personal Community
			share.setCommunities(getPersonalCommunity(owner));

			// Serialize the ID and Dates in the object to MongoDB format
			// Save the document to the share collection
			DbManager.getSocial().getShare().save(share.toDb());
			rp.setResponse(new ResponseObject("Share", true, "New share added successfully. ID in data field."));
			rp.setData(share.get_id().toString(), null);
		}
		catch (Exception e)
		{
			//logger.error("Exception Message: " + e.getMessage(), e);
			rp.setResponse(new ResponseObject("Share", false, "Unable to add new share: " + e.getMessage()));
		}
		return rp;
	}
	
	
	public ResponsePojo updateRef(String ownerIdStr, String shareIdStr, String type, String location, String idStr, String title, String description)
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
				boolean bAdminOrModOfAllCommunities = RESTTools.adminLookup(ownerIdStr);
				if (!share.getOwner().get_id().equals(ownerId)) { // Then I have to be admin (except for one special case)
					if (!bAdminOrModOfAllCommunities) {
						// Special case: I am also community admin/moderator of every community to which this share belongs
						bAdminOrModOfAllCommunities = true;
						for (ShareCommunityPojo comm: share.getCommunities()) {
							if (!SocialUtils.isOwnerOrModerator(comm.get_id().toString(), ownerIdStr)) {
								bAdminOrModOfAllCommunities = false;
							}
						}//TESTED
						
						if (!bAdminOrModOfAllCommunities) {						
							rp.setResponse(new ResponseObject("Update Share",false,"Unable to update share: you are not owner or admin"));
							return rp;
						}
					}					
				}//end if not owner
				
				// Check: am I trying to update a reference or json?
				if (null == share.getDocumentLocation()) {
					rp.setResponse(new ResponseObject("Update Share",false,"Unable to update share: this is not a reference share"));
					return rp;					
				}
				
				if (!bAdminOrModOfAllCommunities) { // quick check whether I'm admin on-request - if so can endorse
					bAdminOrModOfAllCommunities = RESTTools.adminLookup(ownerIdStr, false);
				}//TESTED
								
				// Remove endorsements unless I'm admin (if I'm not admin I must be owner...)
				if (!bAdminOrModOfAllCommunities) { // Now need to check if I'm admin/mod/content publisher for each community..
					if (null == share.getEndorsed()) { // fill this with all allowed communities
						share.setEndorsed(new HashSet<ObjectId>());
						share.getEndorsed().add(share.getOwner().get_id()); // (will be added later)
						for (ShareCommunityPojo comm: share.getCommunities()) {
							if (SocialUtils.isOwnerOrModeratorOrContentPublisher(comm.get_id().toString(), ownerIdStr)) {
								share.getEndorsed().add(comm.get_id());
							}
						}
					}//TESTED
					else {
						for (ShareCommunityPojo comm: share.getCommunities()) {
							// (leave it as is except remove anything that I can't endorse)
							if (!SocialUtils.isOwnerOrModeratorOrContentPublisher(comm.get_id().toString(), ownerIdStr)) {
								share.getEndorsed().remove(comm.get_id());
							}					
						}
					}//TESTED	
				}//TESTED
				else {
					if (null == share.getEndorsed()) { // fill this with all communities
						share.setEndorsed(new HashSet<ObjectId>());
						share.getEndorsed().add(share.getOwner().get_id());
						for (ShareCommunityPojo comm: share.getCommunities()) {
							share.getEndorsed().add(comm.get_id());							
						}
					}
					//(else just leave with the same set of endorsements as before)
				}//TESTED
				
				share.setType(type);
				share.setTitle(title);
				share.setDescription(description);
				share.setModified(new Date());

				// Create DocumentLocationPojo and add to the share
				DocumentLocationPojo documentLocation = new DocumentLocationPojo();
				setRefLocation(location, documentLocation);
				share.setDocumentLocation(documentLocation);
				if (null == documentLocation.getDatabase()) { // (local file)
					// Check, need to be admin:
					if (!RESTTools.adminLookup(ownerIdStr, false)) {
						rp.setResponse(new ResponseObject("Share", false, "Permission denied: you need to be admin to update a local file ref"));
						return rp;
					}				
					if ((null != type) && (type.equalsIgnoreCase("binary") || type.startsWith("binary:"))) {
						String[] binaryType = type.split(":", 2);
						if (binaryType.length > 1) {
							share.setMediaType(binaryType[1]);
							share.setType("binary");
						}
						else {
							share.setMediaType(MimeUtils.getMimeType(FilenameUtils.getExtension(idStr)));
						}
					}
					documentLocation.setCollection(idStr); // collection==file, database==id==null
				}//TESTED
				else {
					documentLocation.set_id(new ObjectId(idStr));				
				}

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
			//logger.error("Exception Message: " + e.getMessage(), e);
			rp.setResponse(new ResponseObject("Share", false, "Unable to update share: " + e.getMessage()));
		}
		return rp;
	}
	

	
	/**
	 * endorseShare
	 * Endorse a share for use within a community (needs to be admin, community owner or moderator)
	 * Can be used by applications
	 * @param shareIdStr
	 * @return
	 */
	public ResponsePojo endorseShare(String personIdStr, String communityIdStr, String shareIdStr, boolean isEndorsed)
	{
		ResponsePojo rp = new ResponsePojo();
		try
		{
			communityIdStr = allowCommunityRegex(personIdStr, communityIdStr);
			ObjectId communityId = new ObjectId(communityIdStr);

			// Do I have permission to do any endorsing?
			// I can be:
			// Admin (or admin on request, regardless of enabled state)
			// Community owner
			// Community moderator
			boolean bAdmin = RESTTools.adminLookup(personIdStr, false);
			if (!bAdmin) {
				if (!SocialUtils.isOwnerOrModeratorOrContentPublisher(communityIdStr, personIdStr))  {	
					rp.setResponse(new ResponseObject("Share", false, "Unable to endorse share: insufficient permissions"));
					return rp;
				}
			}//TESTED
			
			// Now check if the share is even in our community...
			BasicDBObject query = new BasicDBObject(SharePojo._id_, new ObjectId(shareIdStr));
			query.put(ShareCommunityPojo.shareQuery_id_, communityId);
			BasicDBObject fields = new BasicDBObject(ShareOwnerPojo.shareQuery_id_, 1);
			fields.put(SharePojo.endorsed_, 1);
			BasicDBObject shareObj = (BasicDBObject) DbManager.getSocial().getShare().findOne(query, fields);
			SharePojo shareToEndorse = SharePojo.fromDb(shareObj, SharePojo.class);
			if (null == shareToEndorse) {
				rp.setResponse(new ResponseObject("Share", false, "Failed to locate share in community: " + shareIdStr + " vs " + communityIdStr));				
				return rp;
			}//TESTED
			// If we've got this far we're good to go
			BasicDBObject update = null;
			if ((null == shareToEndorse.getEndorsed()) && (null != shareToEndorse.getOwner())) {
				//Legacy case: create the owner's personal community in there
				update = new BasicDBObject(DbManager.addToSet_, new BasicDBObject(SharePojo.endorsed_, shareToEndorse.getOwner().get_id()));
				DbManager.getSocial().getShare().update(query, update, false, true);
			}//TESTED
			if (isEndorsed) {
				update = new BasicDBObject(DbManager.addToSet_, new BasicDBObject(SharePojo.endorsed_, communityId));
			}//TESTED
			else {
				update = new BasicDBObject(DbManager.pull_, new BasicDBObject(SharePojo.endorsed_, communityId));				
			}//TESTED
			DbManager.getSocial().getShare().update(query, update, false, true);
			rp.setResponse(new ResponseObject("Share", true, "Share endorsed successfully."));
		}
		catch (Exception e)
		{
			logger.error("Exception Message: " + e.getMessage(), e);
			rp.setResponse(new ResponseObject("Share", false, "Unable to endorse share: " + e.getMessage()));
		}
		return rp;
	}//TESTED
	
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
	public ResponsePojo addCommunity(String ownerIdStr, String shareIdStr, String communityIdStr, String comment, boolean readWrite)
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
				
				// Read write:
				if (null == share.getReadWrite()) {
					share.setReadWrite(new HashSet<ObjectId>());
				}
				ObjectId communityId = new ObjectId(communityIdStr);
				boolean changedReadWriteAccess = false;
				if (readWrite) { // set read-write up
					changedReadWriteAccess = share.getReadWrite().add(communityId);
				}				
				else {
					changedReadWriteAccess = share.getReadWrite().remove(communityId);
				}				

				// Check to see if the community is already in share.communities
				List<ShareCommunityPojo> communities = share.getCommunities();
				if (null == communities) {
					communities = new ArrayList<ShareCommunityPojo>();
				}
				Boolean addCommunity = true;
				for (ShareCommunityPojo scp : communities)
				{
					if (scp.get_id().toString().equalsIgnoreCase(communityIdStr)) addCommunity = false;
				}
				
				// Add new community to communities (or change its read/write permissions)
				if (addCommunity || changedReadWriteAccess)
				{					
					if (addCommunity) {
						ShareCommunityPojo cp = new ShareCommunityPojo();
						cp.set_id(new ObjectId(communityIdStr));
						cp.setName(getCommunity(new ObjectId(communityIdStr)).getName());
						cp.setComment(comment);
						communities.add(cp);
	
						// Endorse if applicable...
						if (null == share.getEndorsed()) { // legacy case
							share.setEndorsed(new HashSet<ObjectId>());
							share.getEndorsed().add(share.getOwner().get_id()); // user's personal community always endorsed
						}//TESTED
						boolean bAdmin = RESTTools.adminLookup(ownerIdStr, false); // (can be admin-on-request and not enabled, the bar for endorsing is pretty low)
						if (bAdmin || SocialUtils.isOwnerOrModeratorOrContentPublisher(communityIdStr, ownerIdStr))  {
							share.getEndorsed().add(cp.get_id());
						}
						//TESTED - adding as admin/community owner, not adding if not
					}					
					share.setModified(new Date());

					DbManager.getSocial().getShare().update(query, share.toDb());
					rp.setResponse(new ResponseObject("Share", true, "Community successfully added to the share"));
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
						//Also remove endorsements...
						if (null != share.getEndorsed()) {
							share.getEndorsed().remove(scp.get_id());
						}//TESTED						
						
						// Also remove readWrite...
						if (null != share.getReadWrite()) {
							share.getReadWrite().remove(scp.get_id());
						}
						
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
			//create new file
			GridFSInputFile file = DbManager.getSocial().getShareBinary().createFile(bytes);
			file.save();
			
			//remove old file if exists (this way if file throws an exception we don't lose the old file)
			if ( binaryId != null )
				DbManager.getSocial().getShareBinary().remove(binaryId);
			
			return (ObjectId) file.getId();
		}
		catch (Exception ex){}
		return binaryId;
	}

	static private void mustBeOwnerOrAdmin(String userIdStr, BasicDBObject query) {
		if (!RESTTools.adminLookup(userIdStr)) {
			query.put("owner._id", new ObjectId(userIdStr));			
		}
	}
	
	// Utility: make life easier in terms of adding/update/inviting/leaving from the command line
	private static String allowCommunityRegex(String userIdStr, String communityIdStr) {
		return allowCommunityRegex(userIdStr, communityIdStr, false);
	}
	private static String allowCommunityRegex(String userIdStr, String communityIdStr, boolean bAllowMulti) {
		if (communityIdStr.startsWith("*")) {
			String[] communityIdStrs = SocialUtils.getCommunityIds(userIdStr, communityIdStr);	
			if (1 == communityIdStrs.length) {
				communityIdStr = communityIdStrs[0]; 
			}
			else if ((bAllowMulti) && (communityIdStrs.length > 1)) {
				StringBuffer sb = new StringBuffer();
				for (String str: communityIdStrs) {
					if (sb.length() > 0) {
						sb.append(',');
					}
					sb.append(str);
				}
				return sb.toString();
			}
			else {
				throw new RuntimeException("Invalid community pattern");
			}
		}	
		return communityIdStr;
	}
	
	//////////////////////////////////////////////////
	
	// Ref utils
	
	private void setRefLocation(String type, DocumentLocationPojo documentLocation) {
		if (type.equalsIgnoreCase("doc_metadata.metadata")) {
			documentLocation.setDatabase("doc_metadata");
			documentLocation.setCollection("metadata");
		}
		else if (type.equalsIgnoreCase("local.file")) {
			documentLocation.setDatabase(null);
			documentLocation.setCollection(null);			
		}
		else if (type.equalsIgnoreCase("custommr.customlookup")) {
			documentLocation.setDatabase("custommr");
			documentLocation.setCollection("customlookup");				
		}
		else if (type.equalsIgnoreCase("feature.entity")) {
			documentLocation.setDatabase("feature");
			documentLocation.setCollection("entity");								
		}
		else if (type.equalsIgnoreCase("feature.association")) {
			documentLocation.setDatabase("feature");
			documentLocation.setCollection("association");
		}	
		else{
			throw new RuntimeException("Invalid share reference: " + type);
		}
	}//TESTED (all 4 types)
	
	private String getReferenceString(SharePojo share) {
		// FILE:
		if (null == share.getDocumentLocation().get_id()) { // local file based reference
			FileInputStream fin = null;
			Scanner s = null;
			try {
				File f = new File(share.getDocumentLocation().getCollection()) ;
				fin = new FileInputStream(f);
				s = new Scanner(fin, "UTF-8");
				return (s.useDelimiter("\n").next());
			}
			catch (Exception e) {
				return null;
			}
			finally {
				try {
					if (null != fin) fin.close();
					if (null != s) s.close();
				}
				catch (Exception e) {} // (probably just never opened)					
			}
		}		
		// DB:
		// Carry on, this is a database object
		HashSet<String> shareIdStrs = new HashSet<String>();
		for (ShareCommunityPojo commIds: share.getCommunities()) {
			shareIdStrs.add(commIds.get_id().toString());
		}
		String retVal = null;
		BasicDBObject query = new BasicDBObject(DocumentPojo._id_, share.getDocumentLocation().get_id()); // (same for all artifacts)
		String dbName = share.getDocumentLocation().getDatabase();
		String collectionName = share.getDocumentLocation().getCollection();
		BasicDBObject returnVal = (BasicDBObject) MongoDbManager.getCollection(dbName, collectionName).findOne(query);
		try {
			BasicDBList communities = null;
			boolean bCustomJob = dbName.equals("custommr"); // (a bit different)
			boolean bFoundOverlap = false;
			if (!bCustomJob) {
				ObjectId communityId = (ObjectId) returnVal.get(DocumentPojo.communityId_); // (same for other artifacts)
				bFoundOverlap = shareIdStrs.contains(communityId.toString());
			}
			else {
				communities = (BasicDBList) returnVal.get("communityIds"); // (shared across multiple json types)
				for (Object commIdObj: communities) {
					ObjectId commId = (ObjectId)commIdObj;
					if (shareIdStrs.contains(commId.toString())) {
						bFoundOverlap = true;
						break;
					}
				}
			}
			if (!bFoundOverlap) {
				throw new RuntimeException(""); // (turned into the common message below)
			}
			if (!bCustomJob) { // everything but custom jobs
				Date modifiedTime = returnVal.getDate(DocumentPojo.modified_); // (same for other artifacts)
				if (null != modifiedTime) {
					share.setModified(modifiedTime);
				}
				retVal = returnVal.toString();
			}
			else { // custom jobs
				String database = returnVal.getString(CustomMapReduceJobPojo.outputDatabase_);
				if (null == database) {
					database = dbName;
				}
				Date modifiedTime = returnVal.getDate(CustomMapReduceJobPojo.lastCompletionTime_);
				if (null != modifiedTime) {
					share.setModified(modifiedTime);
				}
				String collection = returnVal.getString(CustomMapReduceJobPojo.outputCollection_);
				BasicDBObject returnVal2 = (BasicDBObject) MongoDbManager.getCollection(database, collection).findOne();
				retVal = returnVal2.toString();
			}
		}
		catch (Exception e) {
			throw new RuntimeException("Document not found or permission issue (no overlapping communities)");
		}
		return retVal;
	}//TESTED (normal + custom)
	
}
