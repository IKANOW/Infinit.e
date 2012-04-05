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
package com.ikanow.infinit.e.api.config.source;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import com.ikanow.infinit.e.api.social.community.CommunityHandler;
import com.ikanow.infinit.e.api.social.community.PersonHandler;
import com.ikanow.infinit.e.api.utils.PropertiesManager;
import com.ikanow.infinit.e.api.utils.RESTTools;
import com.ikanow.infinit.e.api.utils.SendMail;
import com.ikanow.infinit.e.data_model.InfiniteEnums.HarvestEnum;
import com.ikanow.infinit.e.data_model.api.ApiManager;
import com.ikanow.infinit.e.data_model.api.BasePojoApiMap;
import com.ikanow.infinit.e.data_model.api.ResponsePojo;
import com.ikanow.infinit.e.data_model.api.ResponsePojo.ResponseObject;
import com.ikanow.infinit.e.data_model.api.config.SourcePojoApiMap;
import com.ikanow.infinit.e.data_model.api.knowledge.DocumentPojoApiMap;
import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.MongoDbManager;
import com.ikanow.infinit.e.data_model.store.config.source.SourceHarvestStatusPojo;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.ikanow.infinit.e.data_model.store.document.DocCountPojo;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.infinit.e.data_model.store.social.community.CommunityMemberPojo;
import com.ikanow.infinit.e.data_model.store.social.community.CommunityPojo;
import com.ikanow.infinit.e.data_model.store.social.person.PersonPojo;
import com.ikanow.infinit.e.harvest.HarvestController;
import com.ikanow.infinit.e.processing.generic.store_and_index.StoreAndIndexManager;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

/**
 * This class is for all operations related to the retrieval, addition
 * or update of sources within the system
 * @author cmorgan
 */
public class SourceHandler 
{
	private static final Logger logger = Logger.getLogger(SourceHandler.class);
	
	// These 2 fns are set by isOwnerModeratorOrSysAdmin
	private boolean isOwnerOrModerator = false;
	private boolean isSysAdmin = false;
	
	/**
	 * getInfo
	 * Retrieve a source
	 * @param idStr
	 * @param userid
	 * @return
	 */
	public ResponsePojo getInfo(String idStr, String userIdStr) 
	{
		ResponsePojo rp = new ResponsePojo();
		
		try 
		{	
			// Return source specified by _id
			BasicDBObject query = new BasicDBObject();
			try {
				query.put(SourcePojo._id_, new ObjectId(idStr));				
			}
			catch (Exception e){ // Obvious feature, allow key to be specified as well as _id
				query.put(SourcePojo.key_, idStr);
			}
		
			// Only return those community IDs that the user is a member of unless the 
			// user is an administrator in which case you can return all community IDs
			Set<ObjectId> communityIdSet = new TreeSet<ObjectId>();
			boolean bAdmin = false;
			if (RESTTools.adminLookup(userIdStr))
			{
				bAdmin = true;
			}
			else
			{
				communityIdSet = RESTTools.getUserCommunities(userIdStr);
				query.put(SourcePojo.communityIds_, new BasicDBObject(MongoDbManager.in_, communityIdSet)); // (security)
			}
			SourcePojo source = SourcePojo.fromDb(DbManager.getIngest().getSource().findOne(query), SourcePojo.class);
			if (null == source) {
				rp.setResponse(new ResponseObject("Source Info",false,"error retrieving source info (or permissions error)"));				
			}
			else {
				if (bAdmin) {
					communityIdSet = source.getCommunityIds();
				}			
				rp.setData(source, new SourcePojoApiMap(communityIdSet));
				rp.setResponse(new ResponseObject("Source Info",true,"Successfully retrieved source info"));
			}
		} 
		catch (Exception e)
		{
			logger.error("Exception Message: " + e.getMessage(), e);
			rp.setResponse(new ResponseObject("Source Info",false,"error retrieving source info"));
		}
		return rp;
	}
	
	
	
	/**
	 * addSource
	 * Add a new source
	 * @param sourcetitle
	 * @param sourcedesc
	 * @param sourceurl
	 * @param extracttype
	 * @param sourcetags
	 * @param mediatype
	 * @param communityIdStr
	 * @param userIdStr
	 * @return
	 */
	public ResponsePojo addSource(String sourcetitle, String sourcedesc, String sourceurl, String extracttype,
			String sourcetags, String mediatype, String communityIdStr, String userIdStr) 
	{
		ResponsePojo rp = new ResponsePojo();
		try 
		{
			communityIdStr = allowCommunityRegex(userIdStr, communityIdStr);
			boolean isApproved = isOwnerModeratorOrSysAdmin(communityIdStr, userIdStr);
			
			//create source object
			SourcePojo newSource = new SourcePojo();
			newSource.setId(new ObjectId());
			newSource.setTitle(sourcetitle);
			newSource.setDescription(sourcedesc);
			newSource.setUrl(sourceurl); // (also sets key, which is then tidied up below)
			newSource.setExtractType(extracttype);
			newSource.setOwnerId(new ObjectId(userIdStr));
			newSource.setTags(new HashSet<String>(Arrays.asList(sourcetags.split(","))));
			newSource.setMediaType(mediatype);
			newSource.addToCommunityIds(new ObjectId(communityIdStr));
			newSource.setApproved(isApproved);
			newSource.setCreated(new Date());
			newSource.setModified(new Date());
			newSource.generateShah256Hash();
			
			newSource.setKey(validateSourceKey(newSource.getId(), newSource.getKey()));
			
			///////////////////////////////////////////////////////////////////////
			// Add the new source to the harvester.sources collection
			try
			{
				DbManager.getIngest().getSource().save(newSource.toDb());
				String awaitingApproval = (isApproved) ? "" : " Awaiting approval by the community owner or moderators.";
				
				if (isUniqueSource(newSource, Arrays.asList(new ObjectId(communityIdStr))))
				{				
					rp.setResponse(new ResponseObject("Source", true, "New source added successfully." + 
							awaitingApproval));
				}
				else {
					rp.setResponse(new ResponseObject("Source", true, "New source added successfully. Note functionally identical sources are also present within your communities, which may waste system resources." + 
							awaitingApproval));					
				}
				
				///////////////////////////////////////////////////////////////////////
				// If the user is not the owner or moderator we need to send the owner
				// and email asking them to approve or reject the source
				if (!isOwnerOrModerator && !isSysAdmin)
				{
					emailSourceApprovalRequest(newSource);
				}
			}
			catch (Exception e)
			{
				rp.setResponse(new ResponseObject("Source", false, "Unable to add new source. Error: " + e.getMessage()));
			}
		}
		catch (Exception e)
		{
			logger.error("Exception Message: " + e.getMessage(), e);
			rp.setResponse(new ResponseObject("Add Source", false, "Error adding source"));
		}
		return rp;
	}

	
	
	/**
	 * saveSource
	 * Adds a new, or updates an existing, source where sourceString
	 * is a JSON representation or a SourcePojo
	 * @param sourceString
	 * @param ownerIdStr
	 * @param communityIdStr
	 * @return
	 */
	public ResponsePojo saveSource(String sourceString, String ownerIdStr, String communityIdStr)
	{
		ResponsePojo rp = new ResponsePojo();
	
		try {
			communityIdStr = allowCommunityRegex(ownerIdStr, communityIdStr);
			boolean isApproved = isOwnerModeratorOrSysAdmin(communityIdStr, ownerIdStr);
			
			///////////////////////////////////////////////////////////////////////
			// Try parsing the json into a SourcePojo object
			SourcePojo source = null;
			Set<ObjectId> communityIdSet = new TreeSet<ObjectId>();
			try
			{
				///////////////////////////////////////////////////////////////////////
				// Note: Remove any communityids already in the source and set groupdID to
				// the communityid param (supports multiple IDs in a comma separated list)
				communityIdSet.add(new ObjectId(communityIdStr));
				
				source = ApiManager.mapFromApi(sourceString, SourcePojo.class, new SourcePojoApiMap(communityIdSet, false));
					// (false => don't auto set key)
				if (null == source.getCommunityIds()) {
					source.setCommunityIds(new HashSet<ObjectId>());
				}
				for (ObjectId sid: communityIdSet) {
					source.getCommunityIds().add(sid);
				}
			}
			catch (Exception e)
			{
				rp.setResponse(new ResponseObject("Source", false, "Unable to serialize Source JSON. Error: " + e.getMessage()));
				return rp;
			}
			
			BasicDBObject query = null;
	
			///////////////////////////////////////////////////////////////////////
			// If source._id == null this should be a new source
			if ((source.getId() == null) && (source.getKey() == null))
			{
				///////////////////////////////////////////////////////////////////////
				// Note: Overwrite the following fields regardless of what was sent in
				source.setId(new ObjectId());
				source.setOwnerId(new ObjectId(ownerIdStr));
				source.setApproved(isApproved);
				source.setCreated(new Date());
				source.setModified(new Date());
				source.setUrl(source.getUrl()); // (sets the key, oops!)
	
				source.setKey(validateSourceKey(source.getId(), source.getKey()));
	
				source.generateShah256Hash();
					// Note: Create/update the source's Shah-256 hash 
			
				///////////////////////////////////////////////////////////////////////
				// Note: Check the SourcePojo to make sure the required fields are there
				// return an error message to the user if any are missing
				String missingFields = hasRequiredSourceFields(source);
				if (missingFields != null && missingFields.length() > 0)
				{
					rp.setResponse(new ResponseObject("Source", false, missingFields));
					return rp;
				}
				
				///////////////////////////////////////////////////////////////////////
				// Add the new source to the harvester.sources collection
				try
				{
					DbManager.getIngest().getSource().save(source.toDb());
					if (isUniqueSource(source, communityIdSet))
					{
						rp.setResponse(new ResponseObject("Source", true, "New source added successfully."));
					}
					else { // Still allow people to add identical sources, but warn them so they can delete it if they way
						rp.setResponse(new ResponseObject("Source", true, "New source added successfully. Note functionally identical sources are also present within your communities, which may waste system resources."));					
					}
					rp.setData(source, new SourcePojoApiMap(communityIdSet));
				}
				catch (Exception e)
				{
					rp.setResponse(new ResponseObject("Source", false, "Unable to add new source. Error: " + e.getMessage()));
				}
				
				///////////////////////////////////////////////////////////////////////
				// If the user is not the owner or moderator we need to send the owner
				// and email asking them to approve or reject the source
				try {
					if (!isApproved)
					{
						emailSourceApprovalRequest(source);
					}
				}
				catch (Exception e) { // Unable to ask for permission, remove sources and error out
					logger.error("Exception Message: " + e.getMessage(), e);
					DbManager.getIngest().getSource().remove(new BasicDBObject(SourcePojo._id_, source.getId()));
					rp.setData((String)null, (BasePojoApiMap<String>)null); // (unset)
					rp.setResponse(new ResponseObject("Source", false, "Unable to email authority for permission, maybe email infrastructure isn't added? Error: " + e.getMessage()));
				}
				
			}//TESTED (behavior when an identical source is added)
	
			///////////////////////////////////////////////////////////////////////
			// Existing source, update if possible
			else
			{
				///////////////////////////////////////////////////////////////////////
				// Attempt to retrieve existing share from harvester.sources collection
				query = new BasicDBObject();
				if (null != source.getId()) {
					query.put(SourcePojo._id_, source.getId());
				}
				else if (null != source.getKey()) {
					query.put(SourcePojo.key_, source.getKey());					
				}
				try 
				{
					BasicDBObject dbo = (BasicDBObject)DbManager.getIngest().getSource().findOne(query);
					// Source doesn't exist so it can't be updated
					if (dbo == null)
					{
						rp.setResponse(new ResponseObject("Source", false, "Unable to update source. The source ID is invalid."));
						return rp;
					}
					
					SourcePojo oldSource = SourcePojo.fromDb(dbo,SourcePojo.class);
					///////////////////////////////////////////////////////////////////////
					// Note: Only an Infinit.e administrator, source owner, community owner
					// or moderator can update/edit a source
					if (null == oldSource.getOwnerId()) { // (internal error, just correct)
						oldSource.setOwnerId(new ObjectId(ownerIdStr));
					}
					boolean isSourceOwner = oldSource.getOwnerId().toString().equalsIgnoreCase(ownerIdStr);
					if (!isApproved && !isSourceOwner)
					{
						rp.setResponse(new ResponseObject("Source", false, "User does not have permissions to "
								+ "edit sources shared by this community."));
						return rp;
					}
					
					String oldHash = source.getShah256Hash();
					
					///////////////////////////////////////////////////////////////////////
					// Note: The following fields in an existing source cannot be changed: Key
					// Make sure new source url and key match existing source values
					// (we allow URL to be changed, though obv the key won't be changed to reflect that)
					source.setKey(oldSource.getKey());
					// Overwrite/set other values in the new source from old source as appropriate
					source.setCreated(oldSource.getCreated());
					source.setModified(new Date());
					source.setOwnerId(oldSource.getOwnerId());
										
					///////////////////////////////////////////////////////////////////////
					// Check for missing fields:
					String missingFields = hasRequiredSourceFields(source);
					if (missingFields != null && missingFields.length() > 0)
					{
						rp.setResponse(new ResponseObject("Source", false, missingFields));
						return rp;
					}
					
					///////////////////////////////////////////////////////////////////////
					// Note: Create/update the source's Shah-256 hash
					source.generateShah256Hash();
					
					///////////////////////////////////////////////////////////////////////
					// Handle approval:
					if (isApproved || oldHash.equalsIgnoreCase(source.getShah256Hash())) {
						//(either i have permissions, or the source hasn't change)
						
						if (oldSource.isApproved()) { // Always approve - annoyingly no way of unsetting this
							source.setApproved(true);
						}
						else if (source.isApproved()) { // Want to re-approve
							if (!isApproved) // Don't have permission, so reset
							{						
								source.setApproved(oldSource.isApproved());
							}
						}					
					}
					else { // Need to re-approve						
						try {
							source.setApproved(false);
							emailSourceApprovalRequest(source);
						}
						catch (Exception e) { // Unable to ask for permission, remove sources and error out
							logger.error("Exception Message: " + e.getMessage(), e);
							DbManager.getIngest().getSource().remove(new BasicDBObject(SourcePojo._id_, source.getId()));
							rp.setData((String)null, (BasePojoApiMap<String>)null); // (unset)
							rp.setResponse(new ResponseObject("Source", false, "Unable to email authority for permission, maybe email infrastructure isn't added? Error: " + e.getMessage()));
						}
					}//TOTEST					
					
					// Source exists, update and prepare reply
					DbManager.getIngest().getSource().update(query, source.toDb());
					rp.setResponse(new ResponseObject("Source", true, "Source has been updated successfully."));
					rp.setData(source, new SourcePojoApiMap(communityIdSet));
				} 
				catch (Exception e) 
				{
					logger.error("Exception Message: " + e.getMessage(), e);
					rp.setResponse(new ResponseObject("Source", false, "Unable to update source: " + e.getMessage()));
				}
			}
		}
		catch (Exception e) {
			logger.error("Exception Message: " + e.getMessage(), e);
			rp.setResponse(new ResponseObject("Source", false, "Unable to add/update source: " + e.getMessage()));			
		}
		return rp;
	}
	
	/**
	 * deleteSource
	 * @param sourceIdStr
	 * @return
	 */
	public ResponsePojo deleteSource(String sourceIdStr, String communityIdStr, String personIdStr, boolean bDocsOnly) {
		ResponsePojo rp = new ResponsePojo();	
		try {
			communityIdStr = allowCommunityRegex(personIdStr, communityIdStr);
			boolean isApproved = isOwnerModeratorOrSysAdmin(communityIdStr, personIdStr);
			ObjectId communityId = new ObjectId(communityIdStr);
			// (Can't use pojos for queries because sources currently have default fields)
			BasicDBObject queryDbo = new BasicDBObject(SourcePojo.communityIds_, communityId);
			BasicDBObject queryFields = new BasicDBObject(SourcePojo.key_, 1);
			try {
				queryDbo.put(SourcePojo._id_, new ObjectId(sourceIdStr));
			}
			catch (Exception e) { // (allow either _id or key)
				queryDbo.put(SourcePojo.key_, sourceIdStr);					
			}
			if (!isApproved) {
				queryDbo.put(SourcePojo.ownerId_, new ObjectId(personIdStr));
			}
			if (!bDocsOnly) {
				BasicDBObject turnOff = new BasicDBObject(MongoDbManager.set_, 
											new BasicDBObject(SourcePojo.isApproved_, false));
				DbManager.getIngest().getSource().update(queryDbo, turnOff);
			}
			BasicDBObject srcDbo = (BasicDBObject) DbManager.getIngest().getSource().findOne(queryDbo, queryFields);
			if (null == srcDbo) {
				rp.setResponse(new ResponseObject("Delete Source", false, "Error finding source or permissions error."));			
				return rp;
			}
			// OK if we've got to here we're approved and the source exists, start deleting stuff
			SourcePojo source = SourcePojo.fromDb(srcDbo, SourcePojo.class);
			int nDocsDeleted = 0;
			if (null != source.getKey()) { // or may delete everything!
				BasicDBObject docQuery = new BasicDBObject(DocumentPojo.sourceKey_, source.getKey());
				BasicDBObject docFields = new BasicDBObject();
				docFields.append(DocumentPojo.url_, 1);
				docFields.append(DocumentPojo.index_, 1);
				docFields.append(DocumentPojo.sourceKey_, 1);
				
				for (;;) {
					DBCursor dbc = DbManager.getDocument().getMetadata().find(docQuery, docFields).limit(10000); // (ie batches of 10K)
					if (0 == nDocsDeleted) {
						nDocsDeleted = dbc.count();
					}
					if (0 == dbc.size()) {
						break;
					}
					List<DocumentPojo> docs = DocumentPojo.listFromDb(dbc, DocumentPojo.listType());
					new StoreAndIndexManager().removeFromDatastore_bySourceKey(docs, source.getKey(), true);
						// (wastes multiple calls to index, but not too wasteful, keeps interface "clean")
				}
				DbManager.getDocument().getCounts().update(new BasicDBObject(DocCountPojo._id_, new ObjectId(communityIdStr)), 
						new BasicDBObject(DbManager.inc_, new BasicDBObject(DocCountPojo.doccount_, -nDocsDeleted)));
			}			
			if (!bDocsOnly) { // Also deleting the entire source
				DbManager.getIngest().getSource().remove(queryDbo);
				rp.setResponse(new ResponseObject("Delete Source", true, "Deleted source and all documents: " + nDocsDeleted));			
			}
			else {
				rp.setResponse(new ResponseObject("Delete Source", true, "Deleted source documents: " + nDocsDeleted));						
			}
		}
		catch (Exception e) {
			rp.setResponse(new ResponseObject("Delete Source", false, 
			"Error approving source. You must be a the owner, the community owner or a moderator to delete the source"));			
		}
		return rp;
	}//TESTED
	
	/**
	 * approveSource
	 * @param sourceIdStr
	 * @param communityIdStrList
	 * @return
	 */
	public ResponsePojo approveSource(String sourceIdStr, String communityIdStr, String personIdStr) 
	{
		ResponsePojo rp = new ResponsePojo();	

		//////////////////////////////////////////////////////////////////////////////////////
		// Note: user must be an admin, owner or moderator of one or more groups to approve a source
		communityIdStr = allowCommunityRegex(personIdStr, communityIdStr);
		boolean isApproved = isOwnerModeratorOrSysAdmin(communityIdStr, personIdStr);

		if (isApproved)
		{
			try 
			{
				Set<ObjectId> communityIdSet = new TreeSet<ObjectId>();
				ObjectId communityId = new ObjectId(communityIdStr);
				communityIdSet.add(communityId);

				BasicDBObject query = new BasicDBObject();
				try {
					query.put(SourcePojo._id_, new ObjectId(sourceIdStr));
				}
				catch (Exception e) { // (allow either _id or key)
					query.put(SourcePojo.key_, sourceIdStr);					
				}
				query.put(SourcePojo.communityIds_, communityId);

				DBObject dbo = (BasicDBObject)DbManager.getIngest().getSource().findOne(query);
				SourcePojo sp = SourcePojo.fromDb(dbo,SourcePojo.class);
				sp.setApproved(true);

				DbManager.getIngest().getSource().update(query, (DBObject) sp.toDb());
				rp.setData(sp, new SourcePojoApiMap(communityIdSet));
				rp.setResponse(new ResponseObject("Approve Source",true,"Source approved successfully"));
				
				// Send email notification to the person who submitted the source
				emailSourceApproval(sp, personIdStr, "Approved");
			} 
			catch (Exception e)
			{
				logger.error("Exception Message: " + e.getMessage(), e);
				rp.setResponse(new ResponseObject("Approve Source",false,"Error approving source"));
			}
		}
		else
		{
			rp.setResponse(new ResponseObject("Approve Source", false, 
			"Error approving source. You must be a community owner or moderator to approve a source"));
		}

		return rp;
	}
	
	/**
	 * denySource
	 * @param sourceIdStr
	 * @param communityIdStrList
	 * @return
	 */
	public ResponsePojo denySource(String sourceIdStr, String communityIdStr, String personIdStr) 
	{
		ResponsePojo rp = new ResponsePojo();	

		//////////////////////////////////////////////////////////////////////////////////////
		// Note: user must be owner or moderator of one or more groups to deny a source
		communityIdStr = allowCommunityRegex(personIdStr, communityIdStr);
		boolean isApproved = isOwnerModeratorOrSysAdmin(communityIdStr, personIdStr);

		if (isApproved)
		{
			try 
			{
				Set<ObjectId> communityIdSet = new TreeSet<ObjectId>();
				ObjectId communityId = new ObjectId(communityIdStr);
				communityIdSet.add(communityId);
				
				// Set up the query
				BasicDBObject query = new BasicDBObject();
				try {
					query.put(SourcePojo._id_, new ObjectId(sourceIdStr));
				}
				catch (Exception e) { // (allow either _id or key)
					query.put(SourcePojo.key_, sourceIdStr);					
				}
				query.put(SourcePojo.communityIds_, communityId);

				// Get the source - what we do with it depends on whether it's ever been active or not
				DBObject dbo = (BasicDBObject)DbManager.getIngest().getSource().findOne(query);
				SourcePojo sp = SourcePojo.fromDb(dbo,SourcePojo.class);
				
				// Case 1: is currently active, set to inactive
				
				if (sp.isApproved()) {
					sp.setApproved(false);
					DbManager.getIngest().getSource().update(query, (DBObject) sp.toDb());
					rp.setResponse(new ResponseObject("Decline Source",true,"Source set to unapproved, use config/source/delete to remove it"));
				}
				
				// Case 2: is currently inactive, has been active
				
				else if (null != sp.getHarvestStatus()) {
					rp.setResponse(new ResponseObject("Decline Source",false,"Source has been active, use config/source/delete to remove it"));
				}
				
				// Case 3: 

				else {
					DbManager.getIngest().getSource().remove(query);			
					rp.setResponse(new ResponseObject("Deny Source",true,"Source removed successfully"));					
					// Send email notification to the person who submitted the source
					emailSourceApproval(getSource(sourceIdStr), personIdStr, "Denied");
				}
								
			} 
			catch (Exception e)
			{
				// If an exception occurs log the error
				logger.error("Exception Message: " + e.getMessage(), e);
				rp.setResponse(new ResponseObject("Deny Source",false,"error removing source"));
			}
		}
		else
		{
			rp.setResponse(new ResponseObject("Deny Source", false, 
			"Error denying source. You must be a community owner or moderator to deny a source"));
		}
		return rp;
	}
	
	
	/**
	 * getGoodSources
	 * Get a list of approved sources for a list of one or more
	 * community IDs passed via the communityid parameter
	 * @param communityIdStrList
	 * @return
	 */
	public ResponsePojo getGoodSources(String userIdStr, String communityIdStrList) 
	{
		ResponsePojo rp = new ResponsePojo();		
		try 
		{
			String[] communityIdStrs = RESTTools.getCommunityIds(userIdStr, communityIdStrList);
			Set<ObjectId> communityIdSet = new TreeSet<ObjectId>();
			for (String s: communityIdStrs) communityIdSet.add(new ObjectId(s));
			
			// Set up the query
			BasicDBObject query = new BasicDBObject();
			// (allow failed harvest sources because they might have previously had good data)
			query.put(SourcePojo.isApproved_, true);
			query.put(SourcePojo.communityIds_, new BasicDBObject(MongoDbManager.in_, communityIdSet));
			
			DBCursor dbc = DbManager.getIngest().getSource().find(query);
			
			// Remove communityids we don't want the user to see:
			rp.setData(SourcePojo.listFromDb(dbc, SourcePojo.listType()), new SourcePojoApiMap(communityIdSet));			
			rp.setResponse(new ResponseObject("Good Sources",true,"successfully returned good sources"));
		} 
		catch (Exception e)
		{
			// If an exception occurs log the error
			logger.error("Exception Message: " + e.getMessage(), e);
			rp.setResponse(new ResponseObject("Good Sources",false,"error returning good sources"));			
		}
		// Return Json String representing the user
		return rp;
	}
	
	
	/**
	 * getBadSources
	 * Get a list of sources with harvester errors for a list of one or more
	 * community IDs passed via the communityid parameter
	 * @param communityIdStrList
	 * @return
	 */
	public ResponsePojo getBadSources(String userIdStr, String communityIdStrList) 
	{
		ResponsePojo rp = new ResponsePojo();
		try 
		{
			String[] communityIdStrs = RESTTools.getCommunityIds(userIdStr, communityIdStrList);
			Set<ObjectId> communityIdSet = new TreeSet<ObjectId>();
			for (String s: Arrays.asList(communityIdStrs)) communityIdSet.add(new ObjectId(s));
			
			// Set up the query
			BasicDBObject query = new BasicDBObject();
			query.put(SourceHarvestStatusPojo.sourceQuery_harvest_status_, HarvestEnum.error.toString());
			query.put(SourcePojo.communityIds_, new BasicDBObject(MongoDbManager.in_, communityIdSet));
			DBCursor dbc = DbManager.getIngest().getSource().find(query);			

			// Remove communityids we don't want the user to see:
			rp.setData(SourcePojo.listFromDb(dbc, SourcePojo.listType()), new SourcePojoApiMap(communityIdSet));			
			rp.setResponse(new ResponseObject("Bad Sources",true,"Successfully returned bad sources"));
		} 
		catch (Exception e)
		{
			// If an exception occurs log the error
			logger.error("Exception Message: " + e.getMessage(), e);
			rp.setResponse(new ResponseObject("Bad Sources",false,"error returning bad sources"));
		}
		// Return Json String representing the user
		return rp;
	}
	
	
	/**
	 * getPendingSources
	 * Get a list of sources pending approval for a list of one or more
	 * community IDs passed via the communityid parameter
	 * @param communityIdStrList
	 * @return
	 */
	public ResponsePojo getPendingSources(String userIdStr, String communityIdStrList) 
	{
		ResponsePojo rp = new ResponsePojo();		
		try 
		{
			String[] communityIdStrs = RESTTools.getCommunityIds(userIdStr, communityIdStrList);
			Set<ObjectId> communityIdSet = new TreeSet<ObjectId>();
			for (String s: Arrays.asList(communityIdStrs)) communityIdSet.add(new ObjectId(s));
			
			// Set up the query
			BasicDBObject query = new BasicDBObject();
			query.put(SourcePojo.isApproved_, false);
			query.put(SourcePojo.communityIds_, new BasicDBObject(MongoDbManager.in_, communityIdSet));
			DBCursor dbc = DbManager.getIngest().getSource().find(query);
			
			// Remove communityids we don't want the user to see:
			rp.setData(SourcePojo.listFromDb(dbc, SourcePojo.listType()), new SourcePojoApiMap(communityIdSet));			
			rp.setResponse(new ResponseObject("Pending Sources",true,"successfully returned pending sources"));
		} 
		catch (Exception e)
		{
			// If an exception occurs log the error
			logger.error("Exception Message: " + e.getMessage(), e);
			rp.setResponse(new ResponseObject("Pending Sources",false,"error returning pending sources"));			
		}
		return rp;
	}
	
	
	/**
	 * getUserSources
	 * @param userIdStr
	 * @param userId
	 * @return
	 */
	public ResponsePojo getUserSources(String userIdStr) 
	{
		ResponsePojo rp = new ResponsePojo();
		try 
		{	
			HashSet<ObjectId> userCommunities = RESTTools.getUserCommunities(userIdStr);
			
			DBCursor dbc = null;
			BasicDBObject query = new BasicDBObject(); 
			query.put(SourcePojo.communityIds_, new BasicDBObject("$in", userCommunities));
			
			// Get all sources for admins
			if (RESTTools.adminLookup(userIdStr))
			{
				dbc = DbManager.getIngest().getSource().find(query);
			}
			// Get only sources the user owns
			else
			{
				query.put(SourcePojo.ownerId_, new ObjectId(userIdStr));
				dbc = DbManager.getIngest().getSource().find(query);
			}
			
			rp.setData(SourcePojo.listFromDb(dbc, SourcePojo.listType()), new SourcePojoApiMap( userCommunities));
			rp.setResponse(new ResponseObject("User's Sources", true, "successfully returned user's sources"));
		} 
		catch (Exception e)
		{
			// If an exception occurs log the error
			logger.error("Exception Message: " + e.getMessage(), e);
			rp.setResponse(new ResponseObject("User's Sources", false, "error returning user's sources"));			
		}
		return rp;
	}


	
	/**
	 * testSource
	 * @param sourceJson
	 * @param nNumDocsToReturn
	 * @param bReturnFullText
	 * @param userIdStr
	 * @return
	 */
	public ResponsePojo testSource(String sourceJson, int nNumDocsToReturn, boolean bReturnFullText, String userIdStr) 
	{
		ResponsePojo rp = new ResponsePojo();		
		try 
		{
			SourcePojo source = ApiManager.mapFromApi(sourceJson, SourcePojo.class, new SourcePojoApiMap(null, true));
				// (true => auto calculate source key == default)

			// This is the only field that you don't normally need to specify in save but will cause 
			// problems if it's not populated in test.
			if (null == source.getCommunityIds()) {
				source.setCommunityIds(new TreeSet<ObjectId>());
			}
			if (source.getCommunityIds().isEmpty()) {
				source.addToCommunityIds(new ObjectId(userIdStr)); // (ie user's personal community, always has same _id - not that it matters)
			}
			
			HarvestController harvester = new HarvestController();
			harvester.setStandaloneMode(nNumDocsToReturn);
			List<DocumentPojo> toAdd = new LinkedList<DocumentPojo>();
			List<DocumentPojo> toUpdate = new LinkedList<DocumentPojo>();
			List<DocumentPojo> toRemove = new LinkedList<DocumentPojo>();
			harvester.harvestSource(source, toAdd, toUpdate, toRemove);
			
			String message = null;
			if ((null != source.getHarvestStatus()) && (null != source.getHarvestStatus().getHarvest_message())) {
				message = source.getHarvestStatus().getHarvest_message();
			}
			else {
				message = "";
			}
			if ((null != source.getHarvestStatus()) && (HarvestEnum.error == source.getHarvestStatus().getHarvest_status())) {
				rp.setResponse(new ResponseObject("Test Source",false,"source error: " + message));			
				rp.setData(toAdd, new DocumentPojoApiMap());				
			}
			else {
				if ((null == message) || message.isEmpty()) {
					message = "no messages from harvester";
				}
				rp.setResponse(new ResponseObject("Test Source",true,"successfully returned " + toAdd.size() + " docs: " + message));			
				try {
					if (bReturnFullText) {
						for (DocumentPojo doc: toAdd) {
							doc.makeFullTextNonTransient();
						}
					}
					rp.setData(toAdd, new DocumentPojoApiMap());
					//Test deserialization:
					rp.toApi();
				}
				catch (Exception e) {
					e.printStackTrace();
					rp.setData(new BasicDBObject("error_message", "Error deserializing documents. This is likely because the Unstructured Analysis Handler is not returning a valid object, eg doesn't end 'var obj = <something>; obj;'"), null);
				}
			}
		}		
		catch (Exception e)
		{
			// If an exception occurs log the error
			logger.error("Exception Message: " + e.getMessage(), e);
			rp.setResponse(new ResponseObject("Test Source",false,"error testing source"));			
		}
		return rp;
	}
	
	
	
	//////////////////////////////////////////////////////////////////////////
	//////////////////////// Helper Functions ////////////////////////////////
	//////////////////////////////////////////////////////////////////////////
	
	

	/**
	 * isOwnerModeratorOrSysAdmin
	 * If the user is a system administrator, community owner, or 
	 * moderator they can add a source and isApproved will be true, otherwise 
	 * it will == false and the owner/moderator will need to approve it
	 * @param communityIdStr
	 * @param ownerIdStr
	 * @return
	 */
	private boolean isOwnerModeratorOrSysAdmin(String communityIdStr, String ownerIdStr)
	{
		isOwnerOrModerator = CommunityHandler.isOwnerOrModerator(communityIdStr, ownerIdStr);
		if (!isOwnerOrModerator) {
			isSysAdmin = RESTTools.adminLookup(ownerIdStr);
		}
		boolean isApproved = (isOwnerOrModerator || isSysAdmin) ? true : false;
		return isApproved;
	}	
	
	
	/**
	 * validateSourceKey
	 * Checks source key passed in for uniqueness, if the key is not
	 * unique it adds a number to the end and increments the number
	 * until it is unique
	 * @param id
	 * @param key
	 * @return
	 */
	private String validateSourceKey(ObjectId id, String key)
	{
		///////////////////////////////////////////////////////////////////////
		// If the source.key value is not unique we need to make it
		// unique, this is being done in an arbitrary way by tacking on a 
		// number to the end of the key value: keyvalue.n
		int counter = 1;
		while (!hasUniqueSourceKey(id, key))
		{
			key = key + "." + counter;
			counter++;
		}
		return key;
	}
	
	
	
	
	/**
	 * emailSourceApprovalRequest
	 * @param source
	 * @return
	 */
	private static boolean emailSourceApprovalRequest(SourcePojo source)
	{
		// Get Information for Person requesting the new source
		PersonPojo p = PersonHandler.getPerson(source.getOwnerId().toString());
		
		// Get the root URL to prepend to the approve/reject link below
		PropertiesManager propManager = new PropertiesManager();
		String rootUrl = propManager.getUrlRoot();
		
		// Subject Line
		String subject = "Approve/Reject New Source: " + source.getTitle();
		
		// Get array of community IDs and get corresponding CommunityPojo objects
		ArrayList<CommunityPojo> communities = CommunityHandler.getCommunities(source.getCommunityIds());
		
		// Iterate over the communities and send an email to each set of owner/moderators requesting
		// that the approve or reject the source
		for (CommunityPojo c : communities)
		{
			// Message Body
			String body = "<p>" + p.getDisplayName() + " has requested that the following source be " +
				"added to the " + c.getName() + " community:</p>" + 
				"<p>" +
				"Title: " + source.getTitle() + "<br/>" + 
				"Description: " + source.getDescription() + "<br/>" + 
				"URL: " + source.getUrl() + "<br/>" + 
				"</p>" +
				"<p>Please click on the Approve or Reject links below to complete the approval process: </p>" +
				"<li><a href=\"" + rootUrl + "config/source/approve/" + source.getId().toString() + "/" +
					c.getId().toString() + "/\">Approve new Source</a></li>" +
				"<li><a href=\"" + rootUrl + "config/source/decline/" + source.getId().toString() + "/" +
					c.getId().toString() + "/\">Reject new Source</a></li>";
			
			// Email address or addresses to send to
			// Extract email addresses for owners and moderators from list of community members
			StringBuffer sendTo = new StringBuffer();
			Set<CommunityMemberPojo> members = c.getMembers();
			for (CommunityMemberPojo member : members)
			{
				if (member.getUserType().equalsIgnoreCase("owner") || member.getUserType().equalsIgnoreCase("moderator"))
				{
					if (sendTo.length() > 0) sendTo.append(";");
					sendTo.append(member.getEmail());
				}
			}
			if (0 == sendTo.length()) { 
				throw new RuntimeException("community " + c.getName() + " / " + c.getId() + " has no owner/moderator");
			}
			
			// Send
			new SendMail(new PropertiesManager().getAdminEmailAddress(), sendTo.toString(), subject, body).send("text/html");	
		}
		return true;
	}
	
	
	/**
	 * emailSourceApproval
	 * @param source
	 * @return
	 */
	private static boolean emailSourceApproval(SourcePojo source, String approverIdStr, String decision)
	{
		// Get Information for Person requesting the new source
		PersonPojo submitter = PersonHandler.getPerson(source.getOwnerId().toString());
		
		// Get Information for Person making approval decision
		PersonPojo approver = PersonHandler.getPerson(approverIdStr);
	
		// Subject Line
		String subject = "Request to add new Source " + source.getTitle() + " was " + decision;

		// Message Body
		String body = "<p>Your request to add the following source:</p>" + 
		"<p>" +
		"Title: " + source.getTitle() + "<br/>" + 
		"Description: " + source.getDescription() + "<br/>" + 
		"URL: " + source.getUrl() + "<br/>" + 
		"</p>" +
		"<p>Was <b>" + decision + "</b> by " + approver.getDisplayName() + "</p>";

		// Send
		new SendMail(new PropertiesManager().getAdminEmailAddress(), submitter.getEmail(), subject, body).send("text/html");	
		return true;
	}
	
	
	/**
	 * hasRequiredSourceFields
	 * @param s
	 * @return
	 */
	private String hasRequiredSourceFields(SourcePojo s)
	{
		ArrayList<String> fields = new ArrayList<String>();
		if (s.getUrl() == null) {
			if ((null == s.getRssConfig()) || (null == s.getRssConfig().getExtraUrls()) || s.getRssConfig().getExtraUrls().isEmpty()) {
				fields.add("URL");				
			}
		}
		if (s.getTitle() == null) fields.add("Title");
		if (s.getMediaType() == null) fields.add("Media Type");
		if (s.getExtractType() == null) fields.add("Extract Type");
		
		if (fields.size() > 0)
		{
			StringBuffer sb = new StringBuffer();
			sb.append("Unable to add source. The following required field/s are missing: ");
			int count = 0;
			for (String field : fields)
			{
				sb.append(field);
				if (count < (fields.size() - 1)) sb.append(", ");
				count++;
			}
			sb.append(".");
			
			return sb.toString();		
		}
		return null;
	}

	
	
	/**
	 * hasUniqueSourceKey
	 * Checks to ensure that a sourcekey is unique across all sources in the
	 * harvester.sources collection
	 * @param key
	 * @return
	 */
	public static boolean hasUniqueSourceKey(ObjectId sourceId, String key)
	{
		boolean isUnique = true;
		BasicDBObject query = new BasicDBObject();
		query.put(SourcePojo._id_, new BasicDBObject(MongoDbManager.ne_,sourceId));
		query.put(SourcePojo.key_, key);
		try
		{
			DBObject dbo = DbManager.getIngest().getSource().findOne(query);
			if (dbo != null)
			{
				isUnique = false;
			}
		}
		catch (Exception e)
		{
			logger.error("Exception Message: " + e.getMessage(), e);
		}
		return isUnique;
	}
	
	
	/**
	 * isUniqueSource
	 * Determine whether or not a source is unique based on its ID and Shah-256 Hash
	 * @param sourceid
	 * @param shah256Hash
	 * @return
	 */
	private static boolean isUniqueSource(SourcePojo source, Collection<ObjectId> communityIdList)
	{
		try
		{
			BasicDBObject query = new BasicDBObject();
			query.put(SourcePojo.url_, source.getUrl());
			query.put(SourcePojo._id_, new BasicDBObject(MongoDbManager.ne_, source.getId()));
			query.put(SourcePojo.communityIds_, new BasicDBObject(MongoDbManager.in_,communityIdList));
			query.put(SourcePojo.shah256Hash_, source.getShah256Hash());
			source = SourcePojo.fromDb(DbManager.getIngest().getSource().findOne(query), SourcePojo.class);
		}
		catch (Exception e)
		{
			source = null;
		}

		if (source == null)
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
	
	/**
	 * getSource
	 * @param sourceIdStr
	 * @return
	 */
	private static SourcePojo getSource(String sourceIdStr)
	{
		SourcePojo source = null;
		try
		{
			BasicDBObject query = new BasicDBObject();
			query.put(SourcePojo._id_, new ObjectId(sourceIdStr));
			source = SourcePojo.fromDb(DbManager.getIngest().getSource().findOne(query), SourcePojo.class);
		}
		catch (Exception e)
		{

		}
		return source;
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

