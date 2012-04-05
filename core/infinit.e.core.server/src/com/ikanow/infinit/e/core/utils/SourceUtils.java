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
package com.ikanow.infinit.e.core.utils;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.bson.types.ObjectId;

import com.google.gson.reflect.TypeToken;
import com.ikanow.infinit.e.data_model.InfiniteEnums;
import com.ikanow.infinit.e.data_model.InfiniteEnums.HarvestEnum;
import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.MongoDbManager;
import com.ikanow.infinit.e.data_model.store.config.source.SourceHarvestStatusPojo;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.ikanow.infinit.e.data_model.store.document.DocCountPojo;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;

public class SourceUtils {

    private static Logger logger = Logger.getLogger(SourceUtils.class);
    
/////////////////////////////////////////////////////////////////////////////////////

// Utilities common to both harvester and synchronization
	
    /////////////////////////////////////////////////////////////////////////////////////
    
	// Get all sources to be harvested (max 500 per cycle, in order of harvesting so nothing should get lost)
	
	public static LinkedList<SourcePojo> getSourcesToWorkOn(String sCommunityOverride, String sSourceId, boolean bSync, boolean bDistributed) {
		// Add harvest types to the DB
		com.ikanow.infinit.e.harvest.utils.PropertiesManager props = new com.ikanow.infinit.e.harvest.utils.PropertiesManager();

		String sTypes = props.getHarvesterTypes();
		String sType[] = sTypes.split("\\s*,\\s*");
		String sTypeCase[] = new String[sType.length*2];
		for (int i = 0; i < sType.length; i++) {
			String s = sType[i];
			sTypeCase[2*i] = s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
			sTypeCase[2*i + 1] = s.toLowerCase();
		}
		BasicDBObject harvestTypes = new BasicDBObject(MongoDbManager.in_, sTypeCase);
		LinkedList<SourcePojo> sources = null;
		try 
		{			
			BasicDBObject query = null;
			BasicDBObject adminUpdateQuery = new BasicDBObject();
			if (bDistributed) {
				Date now = new Date();
				query = generateNotInProgressClause(now, bSync);
					// (just don't waste time on things currently being harvested)

				// Also need to ignore any sources that have just been synced by a different node... 
				if (bSync) {
					Date recentlySynced = new Date(now.getTime() - 1800*1000); //(ie not synced within 1/2 hour)
					query.put(SourceHarvestStatusPojo.sourceQuery_synced_, new BasicDBObject(MongoDbManager.lt_, recentlySynced));
						// (will know synced exists because we set it below - the sort doesn't work without its being set for all records)
				}
			}
			else {
				query = new BasicDBObject();
			}
			query.put(SourcePojo.isApproved_, true);
			if (!bSync) {
				query.put(SourcePojo.harvestBadSource_,  new BasicDBObject(MongoDbManager.ne_, true)); // (ie true or doesn't exist)
					// (still sync bad sources)
			}
			query.put(SourcePojo.extractType_, harvestTypes);
			if (null != sCommunityOverride) {
				query.put(SourcePojo.communityIds_, new ObjectId(sCommunityOverride));
				adminUpdateQuery.put(SourcePojo.communityIds_, new ObjectId(sCommunityOverride));
			}
			else if (null != sSourceId) {
				try {
					query.put(SourcePojo._id_, new ObjectId(sSourceId));				
					adminUpdateQuery.put(SourcePojo._id_, new ObjectId(sSourceId));
				}
				catch (Exception e) { // Allow either _id or key to be used as the id...
					query.put(SourcePojo.key_, sSourceId);				
					adminUpdateQuery.put(SourcePojo.key_, sSourceId);
				}
			}
			BasicDBObject orderBy = new BasicDBObject(); 
			if (bSync) {
				orderBy.put(SourceHarvestStatusPojo.sourceQuery_synced_, 1);
			}
			else {
				orderBy.put(SourceHarvestStatusPojo.sourceQuery_harvested_, 1);		
			}
			BasicDBObject fields = new BasicDBObject(); 
			if (bDistributed) { 
				// Mainly just _id and extractType, we'll get these for debugging
				fields.put(SourcePojo._id_, 1);
				fields.put(SourcePojo.extractType_, 1);
				
				fields.put(SourcePojo.key_, 1);
				fields.put(SourceHarvestStatusPojo.sourceQuery_harvested_, 1);
				fields.put(SourceHarvestStatusPojo.sourceQuery_synced_, 1);
				fields.put(SourcePojo.searchCycle_secs_, 1);
			}
			// (first off, set the harvest/sync date for any sources that don't have it set,
			//  needed because sort doesn't return records without the sorting field) 
			Date yesterday = new Date(new Date().getTime() - 24*3600*1000);
			if (bSync) {
				adminUpdateQuery.put(SourceHarvestStatusPojo.sourceQuery_synced_, new BasicDBObject(MongoDbManager.exists_, false));
				DbManager.getIngest().getSource().update(adminUpdateQuery,
						new BasicDBObject(MongoDbManager.set_, new BasicDBObject(SourceHarvestStatusPojo.sourceQuery_synced_, yesterday)), false, true);
			}
			else {
				adminUpdateQuery.put(SourceHarvestStatusPojo.sourceQuery_harvested_, new BasicDBObject(MongoDbManager.exists_, false));
				DbManager.getIngest().getSource().update(adminUpdateQuery,
						new BasicDBObject(MongoDbManager.set_, new BasicDBObject(SourceHarvestStatusPojo.sourceQuery_harvested_, yesterday)), false, true);				
			}
			// (then perform query)
			DBCursor cur = DbManager.getIngest().getSource().find(query, fields).sort(orderBy).limit(1000);			
			sources = SourcePojo.listFromDb(cur, new TypeToken<LinkedList<SourcePojo>>(){});
		} 
		catch (Exception e) 
		{
			logger.error("Exception Message getting sources for sync: " + e.getMessage(), e);
		} 
		return sources;		
	}//TESTED (mostly unchanged from "tested" Beta version - few changes for distribution tested by eye)

    /////////////////////////////////////////////////////////////////////////////////////
    
	// Share sources to be harvested across all running harvesters
	
	public static LinkedList<SourcePojo> getDistributedSourceList(LinkedList<SourcePojo> uncheckedSources, String sSourceType, boolean bSync)
	{
		Date now = new Date();
		LinkedList<SourcePojo> nextSetToProcess = new LinkedList<SourcePojo>();
		
		PropertiesManager pm = new PropertiesManager();
		int nBatchSize = pm.getDistributionBatchSize(bSync);
		
		// The logic for getting the next set of sources is:
		// 2] Get the oldest 20 sources that are:
		// 2.1] In progress and >a day old (assume the harvester/sync running them crashed)
		// 2.2] Not in progress and have either never been harvested or synced, or in age of how long ago

		for (int nNumSourcesGot = 0; (nNumSourcesGot < nBatchSize) && (!uncheckedSources.isEmpty()); ) {
			
			BasicDBObject query = generateNotInProgressClause(now, bSync);
			SourcePojo candidate = uncheckedSources.pop(); 
			if ((null != sSourceType) && !candidate.getExtractType().equalsIgnoreCase(sSourceType)) {
				continue;
			}
			if (null != candidate.getSearchCycle_secs()) {
				if ((null != candidate.getHarvestStatus()) && (null != candidate.getHarvestStatus().getHarvested())) {
					//(ie the source has been harvested, and there is a non-default search cycle setting)
					if ((candidate.getHarvestStatus().getHarvested().getTime() + 1000L*candidate.getSearchCycle_secs())
							> now.getTime())
					{
						continue; // (too soon since the last harvest...)
					}
				}
			}//TOTEST
			
			query.put(SourcePojo._id_, candidate.getId());
			BasicDBObject modifyClause = new BasicDBObject();
			modifyClause.put(SourceHarvestStatusPojo.sourceQuery_harvest_status_, HarvestEnum.in_progress.toString());
			if (bSync) {
				modifyClause.put(SourceHarvestStatusPojo.sourceQuery_synced_, now);				
			}
			else {
				modifyClause.put(SourceHarvestStatusPojo.sourceQuery_harvested_, now);
			}
			BasicDBObject modify = new BasicDBObject(MongoDbManager.set_, modifyClause);
			
			try {
				BasicDBObject dbo = (BasicDBObject) DbManager.getIngest().getSource().findAndModify(query, modify);
				if (null != dbo) {
					nextSetToProcess.add(SourcePojo.fromDb(dbo, SourcePojo.class));
					nNumSourcesGot++;
				}
			}
			catch (Exception e) {

				// Unset the in-progress clause for robustness
				modifyClause = new BasicDBObject();
				modifyClause.put(SourceHarvestStatusPojo.sourceQuery_harvest_status_, HarvestEnum.error.toString());
				modify = new BasicDBObject(MongoDbManager.set_, modifyClause);
				DbManager.getIngest().getSource().update(query, modify);
				
				// This source has failed somehow, just carry on
				logger.error("Source " + candidate.getKey() + " has errored during distribution " + e.getMessage());
				e.printStackTrace();
			}
			
		} // (end loop over unchecked sources until we have >20)

		return nextSetToProcess;
	} //TESTED

    /////////////////////////////////////////////////////////////////////////////////////
    
	// Sub-utility function used by both the above functions
	
	private static BasicDBObject generateNotInProgressClause(Date date, boolean bSync) {
		
		//24hrs ago
		Date oldDate = new Date(date.getTime() - 24*3600*1000);
		
		// This query says: if the query isn't in progress [1] (or the harvest object doesn't exist [3,4]) ... or if it is but nothing's happened in 24 hours [2]
		
		BasicDBObject subclause1 = new BasicDBObject(SourceHarvestStatusPojo.sourceQuery_harvest_status_, 
														new BasicDBObject(MongoDbManager.ne_, HarvestEnum.in_progress.toString()));
		BasicDBObject subclause2 = new BasicDBObject();
		if (bSync) {
			subclause2.put(SourceHarvestStatusPojo.sourceQuery_synced_, new BasicDBObject(MongoDbManager.lt_, oldDate));
		}
		else {
			subclause2.put(SourceHarvestStatusPojo.sourceQuery_harvested_, new BasicDBObject(MongoDbManager.lt_, oldDate));
		}
		BasicDBObject subclause3 = new BasicDBObject(SourcePojo.harvest_, new BasicDBObject(MongoDbManager.exists_, false));
		BasicDBObject subclause4 = new BasicDBObject(SourceHarvestStatusPojo.sourceQuery_harvest_status_, 
														new BasicDBObject(MongoDbManager.exists_, false));
		
		BasicDBObject clause = new BasicDBObject(MongoDbManager.or_, Arrays.asList(subclause1, subclause2, subclause3, subclause4));
		return clause;
	}//TESTED
	
	public static void checkSourcesHaveHashes(String sCommunityOverride, String sSourceDebug) {
		
		BasicDBObject query = new BasicDBObject(SourcePojo.shah256Hash_, new BasicDBObject(MongoDbManager.exists_, false));
		if (null != sCommunityOverride) {
			query.put(SourcePojo.communityIds_, new ObjectId(sCommunityOverride));
		}
		if (null != sSourceDebug) {
			try {
				query.put(SourcePojo._id_, new ObjectId(sSourceDebug));
			}
			catch (Exception e) { // Allow key also
				query.put(SourcePojo.key_, sSourceDebug);				
			}
		}
		DBCursor dbc = DbManager.getIngest().getSource().find(query);

		int nSrcFixCount = 0;
		while (dbc.hasNext()) {
			SourcePojo src = SourcePojo.fromDb(dbc.next(), SourcePojo.class);
			nSrcFixCount++;
			src.generateShah256Hash();
			DbManager.getIngest().getSource().update(new BasicDBObject(SourcePojo._id_, src.getId()), 
					new BasicDBObject(MongoDbManager.set_, new BasicDBObject(SourcePojo.shah256Hash_, src.getShah256Hash())));
		}
		if (nSrcFixCount > 0) {
			logger.info("Core.Server: Fixed " + nSrcFixCount + " missing source hash(es)");
		}		
	}//TESTED (by hand/eye)
	
////////////////////////////////////////////////////////////////////////////////////////////
	
	// Synchronization specific utilities
			
		// Updates "in_progress" to either "success" or "error"
		
		public static void updateSyncStatus(SourcePojo source, HarvestEnum harvestStatus) {
			BasicDBObject query = new BasicDBObject(SourcePojo._id_, source.getId());
			BasicDBObject update = new BasicDBObject(MongoDbManager.set_, new BasicDBObject(SourceHarvestStatusPojo.sourceQuery_harvest_status_, harvestStatus.toString()));
			DbManager.getIngest().getSource().update(query, update);
		}
		
////////////////////////////////////////////////////////////////////////////////////////////
	
// Harvest specific source utilities
		
		// Updates "in_progress" to either "success" or "error", increments the doccount (per source and per community)
		
		public static void updateHarvestStatus(SourcePojo source, HarvestEnum harvestStatus, List<DocumentPojo> added, long nDocsDeleted) {
			
			// Always update status object in order to release the "in_progress" lock
			// (make really really sure we don't exception out before doing this!)
			
			BasicDBObject query = new BasicDBObject(SourcePojo._id_, source.getId());
			BasicDBObject update = new BasicDBObject(MongoDbManager.set_, new BasicDBObject(SourceHarvestStatusPojo.sourceQuery_harvest_status_, harvestStatus.toString()));

			int nDocsAdded = 0;
			if (null != added) {
				nDocsAdded = added.size();
			}
			update.put(MongoDbManager.inc_, new BasicDBObject(SourceHarvestStatusPojo.sourceQuery_doccount_, nDocsAdded - nDocsDeleted));
			DbManager.getIngest().getSource().update(query, update);

			// (OK now the only thing we really had to do is complete, add some handy metadata)
			
			// Also update the document count table in doc_metadata:
			if (nDocsAdded > 0) {
				if (1 == source.getCommunityIds().size()) { // (simple/usual case, just 1 community)
					query = new BasicDBObject(DocCountPojo._id_, source.getCommunityIds().iterator().next());
					update = new BasicDBObject(MongoDbManager.inc_, new BasicDBObject(DocCountPojo.doccount_, nDocsAdded - nDocsDeleted));		
					DbManager.getDocument().getCounts().update(query, update, true, false);
				}
				else if (!source.getCommunityIds().isEmpty()) { // Complex case since docs can belong to diff communities (but they're usually somewhat grouped)
					Map<ObjectId, Integer> communityMap = new HashMap<ObjectId, Integer>();
					for (DocumentPojo doc: added) {
						ObjectId communityId = doc.getCommunityId();
						Integer count = communityMap.get(communityId);
						communityMap.put(communityId, (count == null ? 1 : count + 1));
					}//end loop over added documents (updating the separate community counts)
					long nDocsDeleted_byCommunity = nDocsDeleted/source.getCommunityIds().size();
						// (can't do better than assume a uniform distribution - the whole thing gets recalculated weekly anyway...)
					
					for (Map.Entry<ObjectId, Integer> communityInfo: communityMap.entrySet()) {
						query = new BasicDBObject(DocCountPojo._id_, communityInfo.getKey());
						update = new BasicDBObject(MongoDbManager.inc_, new BasicDBObject(DocCountPojo.doccount_, communityInfo.getValue() - nDocsDeleted_byCommunity));
						DbManager.getDocument().getCounts().update(query, update, true, false);
							// (true for upsert, false for multi add)
					}
				}
			}
		}//TOTEST (just the document count increments)
		
		////////////////////////////////////////////////////////////////////////////////////////////
		
		// Maps string type in source pojo to enum
		
		public static int getHarvestType(SourcePojo source) {
			if (source.getExtractType().equalsIgnoreCase("database")) {
				return InfiniteEnums.DATABASE;
			}
			else if (source.getExtractType().equalsIgnoreCase("file")) {
				return InfiniteEnums.FILES;
			}
			else {
				return InfiniteEnums.FEEDS;
			}
		}//TESTED
		
		////////////////////////////////////////////////////////////////////////////////////////////
		
		/**
		 * Changes all sources badSource flag to false so it will be attempted again on
		 * the next harvest cycle.
		 * 
		 * NOTE: If mutliple harvesters are called with reset flag they will all
		 * set the bad source flag back to true for all sources
		 * 
		 */
		public static void resetBadSources() 
		{
			try 
			{			
				BasicDBObject query = new BasicDBObject();
				query.put(MongoDbManager.set_,new BasicDBObject(SourcePojo.harvestBadSource_, false));
				DbManager.getIngest().getSource().update(new BasicDBObject(), query, false, true);			
			} 
			catch (Exception e) 
			{
				logger.error("Exception Message reseting feeds badsource flag: " + e.getMessage(), e);
			} 
		}//TESTED (unchanged from working Beta version)
		
}
