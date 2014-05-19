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

import java.net.InetAddress;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
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
import com.ikanow.infinit.e.processing.generic.store_and_index.StoreAndIndexManager;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;

public class SourceUtils {

    private static Logger logger = Logger.getLogger(SourceUtils.class);
    
    private static final long _ONEDAY = 24L*3600L*1000L;
    
/////////////////////////////////////////////////////////////////////////////////////

// Utilities common to both harvester and synchronization
	
    /////////////////////////////////////////////////////////////////////////////////////
    
    public static boolean checkDbSyncLock() {
    	DBCursor dbc = DbManager.getFeature().getSyncLock().find();
    	if (0 == dbc.count()) {
    		return false; // working fine
    	}
    	Date now = new Date();
    	while (dbc.hasNext()) {
    		BasicDBObject sync_lock = (BasicDBObject) dbc.next();
    		Object lastSyncObj = sync_lock.get("last_sync");
    		if (null != lastSyncObj) {
    			try {
    				Date last_sync = (Date) lastSyncObj;
    				if (last_sync.getTime() + _ONEDAY > now.getTime()) {
    					
    					return true; // (ie sync object exists and is < 1 day old)
    				}
    			}
    			catch (Exception e) { 
    				// class cast, do nothing
    			}
    		}
    	} // (end "loop over" 1 object in sync_lock DB)
    	
    	return false;
    }
    //TESTED (active lock, no lock, old lock)
    
    /////////////////////////////////////////////////////////////////////////////////////
    
	// Get all sources to be harvested (max 500 per cycle, in order of harvesting so nothing should get lost)
	
	public static LinkedList<SourcePojo> getSourcesToWorkOn(String sCommunityOverride, String sSourceId, boolean bSync, boolean bDistributed) {
		// Add harvest types to the DB
		com.ikanow.infinit.e.harvest.utils.PropertiesManager props = new com.ikanow.infinit.e.harvest.utils.PropertiesManager();
		int nMaxSources = 1000;
		
		if (!bSync) {
			nMaxSources = props.getMaxSourcesPerHarvest(); // (normally 0 == no limit)
		}

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
				query = generateNotInProgressClause(now);
					// (just don't waste time on things currently being harvested)
				
				// Also need to ignore any sources that have just been synced by a different node... 
				if (bSync) {
					Date recentlySynced = new Date(now.getTime() - 1800*1000); //(ie not synced within 1/2 hour)
					query.put(SourceHarvestStatusPojo.sourceQuery_synced_, new BasicDBObject(MongoDbManager.lt_, recentlySynced));
						// (will know synced exists because we set it below - the sort doesn't work without its being set for all records)
				}
				else if (null == sSourceId) { // for harvest, try to take into account the effect of search cycles
					// (if manually setting the source then ignore this obviously...)
					addSearchCycleClause(query, now);
				}
			}
			else {
				query = new BasicDBObject();
			}
			if (null == sSourceId) {
				query.put(SourcePojo.isApproved_, true);
			}
			if (!bSync && (null == sSourceId)) {
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
			//(note although there's a complex query preceding this, it should be using the above index 
			// anyway so there should be some benefit to this)
			
			BasicDBObject fields = new BasicDBObject(); 
			if (bDistributed) { 
				// Mainly just _id and extractType, we'll get these for debugging
				fields.put(SourcePojo._id_, 1);
				fields.put(SourcePojo.extractType_, 1);
				
				fields.put(SourcePojo.key_, 1);
				fields.put(SourceHarvestStatusPojo.sourceQuery_harvested_, 1);
				fields.put(SourceHarvestStatusPojo.sourceQuery_synced_, 1);
				fields.put(SourceHarvestStatusPojo.sourceQuery_harvest_status_, 1);
				if (null != sSourceId) {
					//put a random field in just so we know it's a source override:
					fields.put(SourcePojo.ownerId_, 1);
					//(plus don't add searchCycle, we're just going to ignore it anyway)
				}//TESTED
				else {
					fields.put(SourcePojo.searchCycle_secs_, 1);					
				}//TESTED
				
				// (need these for distributed logic)
				fields.put(SourcePojo.distributionFactor_, 1);
				fields.put(SourceHarvestStatusPojo.sourceQuery_distributionTokensFree_, 1);
			}
			// (first off, set the harvest/sync date for any sources that don't have it set,
			//  needed because sort doesn't return records without the sorting field) 
			Date yesteryear = new Date(new Date().getTime() - 365L*_ONEDAY);
				// (NOTE this time being >=1 yr is depended upon by applications, so you don't get to change it. Ever)
			if (bSync) {
				adminUpdateQuery.put(SourceHarvestStatusPojo.sourceQuery_synced_, new BasicDBObject(MongoDbManager.exists_, false));
				DbManager.getIngest().getSource().update(adminUpdateQuery,
						new BasicDBObject(MongoDbManager.set_, new BasicDBObject(SourceHarvestStatusPojo.sourceQuery_synced_, yesteryear)), false, true);
			}
			else {
				adminUpdateQuery.put(SourceHarvestStatusPojo.sourceQuery_harvested_, new BasicDBObject(MongoDbManager.exists_, false));
				DbManager.getIngest().getSource().update(adminUpdateQuery,
						new BasicDBObject(MongoDbManager.set_, new BasicDBObject(SourceHarvestStatusPojo.sourceQuery_harvested_, yesteryear)), false, true);				
			}
			// (then perform query)
			DBCursor cur = DbManager.getIngest().getSource().find(query, fields).sort(orderBy).limit(nMaxSources);
			
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
		
		// Some additional distributed logic
		LinkedList<SourcePojo> putMeBackAtTheStart_distributed = null;
		
		PropertiesManager pm = new PropertiesManager();
		int nBatchSize = pm.getDistributionBatchSize(bSync);
		Long defaultSearchCycle_ms = pm.getMinimumHarvestTimePerSourceMs();
		
		// The logic for getting the next set of sources is:
		// 2] Get the oldest 20 sources that are:
		// 2.1] In progress and >a day old (assume the harvester/sync running them crashed)
		// 2.2] Not in progress and have either never been harvested or synced, or in age of how long ago

		for (int nNumSourcesGot = 0; (nNumSourcesGot < nBatchSize) && (!uncheckedSources.isEmpty()); ) {
			
			BasicDBObject query = generateNotInProgressClause(now);
			SourcePojo candidate = null;
			synchronized (SourceUtils.class) { // (can be called across multiple threads)
				candidate = uncheckedSources.pop();
			}		

			//DEBUG
			//System.out.println(" CANDIDATE=" + candidate.getKey() + " ..." + candidate.getId());			
			
			if ((null != sSourceType) && !candidate.getExtractType().equalsIgnoreCase(sSourceType)) {
				continue;
			}
			HarvestEnum candidateStatus = null;
			if (null != candidate.getHarvestStatus()) {
				candidateStatus = candidate.getHarvestStatus().getHarvest_status();
			}
			if (bSync && (null == candidateStatus)) { // Don't sync unharvested sources, obviously!
				continue;
			}
			//(DISTRIBUTON LOGIC)
			
			// Checking whether to respect the searchCycle_secs for distributed sources is a bit more complex
			boolean isDistributed = (null != candidate.getDistributionFactor());			
			boolean distributedInProcess = isDistributed &&  
				candidate.reachedMaxDocs() ||  // (<- only set inside a process)
					((null != candidate.getHarvestStatus()) && // (robustness) 
							(null != candidate.getHarvestStatus().getDistributionTokensFree()) && // (else starting out)
								(candidate.getDistributionFactor() != candidate.getHarvestStatus().getDistributionTokensFree()));
									// (else this is the start)
			//(TESTED - local and distributed)
			//(END DISTRIBUTON LOGIC)
			
			if (((HarvestEnum.success_iteration != candidateStatus) && !distributedInProcess) 
					|| 
					((null != candidate.getSearchCycle_secs()) && (candidate.getSearchCycle_secs() < 0)))
			{
				// (ie EITHER we're not iteration OR we're disabled)
				//(^^^ don't respect iteration status if source manually disabled)
				
				if ((null != candidate.getSearchCycle_secs()) || (null != defaultSearchCycle_ms)) {
					if (null == candidate.getSearchCycle_secs()) {
						candidate.setSearchCycle_secs((int)(defaultSearchCycle_ms/1000));
					}
					if (candidate.getSearchCycle_secs() < 0) {
						continue; // negative search cycle => disabled
					}
					if ((null != candidate.getHarvestStatus()) && (null != candidate.getHarvestStatus().getHarvested())) {
						//(ie the source has been harvested, and there is a non-default search cycle setting)
						
						if ((candidate.getHarvestStatus().getHarvested().getTime() + 1000L*candidate.getSearchCycle_secs())
								> now.getTime())
						{
							if ((HarvestEnum.in_progress != candidateStatus) && (null != candidateStatus) && (null == candidate.getOwnerId()))
							{
								//(^^ last test, if it's in_progress then it died recently (or hasn't started) so go ahead and harvest anyway)
								// (also hacky use of getOwnerId just to see if this is a source override source or not)
								continue; // (too soon since the last harvest...)
							}//TESTED (including hacky use of ownerId)
						}
					}
				}//TESTED
			}
			//TESTED: manually disabled (ignore), not success_iteration (ignore if outside cycle), success_iteration (always process)
			
			query.put(SourcePojo._id_, candidate.getId());
			BasicDBObject modifyClause = new BasicDBObject();
			modifyClause.put(SourceHarvestStatusPojo.sourceQuery_harvest_status_, HarvestEnum.in_progress.toString());
			if (bSync) {
				modifyClause.put(SourceHarvestStatusPojo.sourceQuery_synced_, now);				
			}
			else {
				modifyClause.put(SourceHarvestStatusPojo.sourceQuery_harvested_, now);
			}
			modifyClause.put(SourceHarvestStatusPojo.sourceQuery_lastHarvestedBy_, getHostname());
			BasicDBObject modify = new BasicDBObject(MongoDbManager.set_, modifyClause);
			
			try {
				BasicDBObject dbo = (BasicDBObject) DbManager.getIngest().getSource().findAndModify(query, modify);
				if (null != dbo) {
					SourcePojo fullSource = SourcePojo.fromDb(dbo, SourcePojo.class);
					nextSetToProcess.add(fullSource);
					nNumSourcesGot++;
					
					////////////////////////////////////////////////////////////////////////
					//
					// DISTRIBUTION LOGIC:
					// If distributionFactor set then grab one token and set state back to 
					// success_iteration, to allow other threads/processes to grab me
					if ((null != fullSource.getDistributionFactor()) && !bSync)
					{
						// Get the current distribution token
						int distributionToken = 0;						
						boolean bReset = false;
						if ((null == fullSource.getHarvestStatus()) || (null == fullSource.getHarvestStatus().getDistributionTokensFree())) {
							distributionToken = fullSource.getDistributionFactor();
							// (also set up some parameters so don't need to worry about null checks later)
							if (null == fullSource.getHarvestStatus()) {
								fullSource.setHarvestStatus(new SourceHarvestStatusPojo());
							}
							fullSource.getHarvestStatus().setDistributionTokensFree(distributionToken);
							fullSource.getHarvestStatus().setDistributionTokensComplete(0);
						}
						else {
							distributionToken = fullSource.getHarvestStatus().getDistributionTokensFree();
							
							//Check last harvested time to ensure this isn't an old state (reset if so)
							if ((distributionToken != fullSource.getDistributionFactor()) ||
									(0 != fullSource.getHarvestStatus().getDistributionTokensComplete()))
							{
								if (null != fullSource.getHarvestStatus().getRealHarvested()) { // harvested is useless here because it's already been updated
									if ((new Date().getTime() - fullSource.getHarvestStatus().getRealHarvested().getTime()) >
											_ONEDAY) // (ie older than a day)
									{
										distributionToken = fullSource.getDistributionFactor(); // ie start again
									}
								}
							}//TESTED
						}//(end check for any existing state)					

						if (distributionToken == fullSource.getDistributionFactor()) {
							bReset = true; // (first time through, might as well go ahead and reset to ensure all the vars are present)
						}

						// If in error then just want to grab all remaining tokens and reset the status
						if (HarvestEnum.error == fullSource.getHarvestStatus().getHarvest_status()) { // currently an error
							if (distributionToken != fullSource.getDistributionFactor()) { // In the middle, ie just errored
								fullSource.setDistributionTokens(new HashSet<Integer>());
								while (distributionToken > 0) {
									distributionToken--;
									fullSource.getDistributionTokens().add(distributionToken);									
								}
								BasicDBObject dummy = new BasicDBObject();
								bReset = updateHarvestDistributionState_tokenComplete(fullSource, HarvestEnum.error, dummy, dummy);
									// (then finish off completion down below)								
							}
						}//TESTED (error mode, 2 cases: complete and incomplete)
						
						//DEBUG
						//System.out.println(" DIST_SOURCE=" + fullSource.getKey() + "/" + fullSource.getDistributionFactor() + ": " + distributionToken + ", " + bReset);
						
						//(note we'll see this even if searchCycle is set because the "source" var (which still has the old
						// state) is stuck back at the start of uncheckedList, so each harvester will see the source >1 time)
						
						if (0 != distributionToken) { // (else no available tokens for this cycle)
							distributionToken--;
							
							fullSource.setDistributionTokens(new HashSet<Integer>());
							fullSource.getDistributionTokens().add(distributionToken);
							
							// Remove one of the available tokens (they don't get reset until the source is complete)
							updateHarvestDistributionState_newToken(fullSource.getId(), distributionToken, HarvestEnum.success_iteration, bReset);

							// After this loop is complete, put back at the start of the unchecked list
							// so another thread can pick up more tokens:
							if (null == putMeBackAtTheStart_distributed) {
								putMeBackAtTheStart_distributed = new LinkedList<SourcePojo>();
							}
							putMeBackAtTheStart_distributed.add(candidate);
							
							// Before adding back to list, set a transient field to ensure it bypasses any search cycle checks
							// (for in process logic where we won't see the update status from the DB)
							candidate.setReachedMaxDocs();
							
							// Reset full source's status so we know if we started in success/error/success_iteration
							if (null == candidateStatus) {
								candidateStatus = HarvestEnum.success;
							}
							fullSource.getHarvestStatus().setHarvest_status(candidateStatus);							
							
						} // (end if available tokens)
						else { // (don't process, just set back to original status)
							HarvestEnum harvestStatus = HarvestEnum.success;
							if (null != fullSource.getHarvestStatus()) {
								if (null != fullSource.getHarvestStatus().getHarvest_status()) {
									harvestStatus = fullSource.getHarvestStatus().getHarvest_status();
								}
							}
							if (bReset) { // resetting back to 10 
								distributionToken = fullSource.getDistributionFactor();
							}
							updateHarvestDistributionState_newToken(fullSource.getId(), distributionToken, harvestStatus, bReset);
								// (bReset can be true in the error case handled above)

							nextSetToProcess.removeLast();
							nNumSourcesGot--;							
						}//TESTED						
						
					}//TESTED
					else if (bSync) {
						// Not allowed to sync "distributed in progress"
						if ((null != fullSource.getHarvestStatus()) || (null != fullSource.getHarvestStatus().getDistributionTokensFree())) {
							if (null == fullSource.getHarvestStatus().getHarvest_status()) { // (shouldn't ever happen)
								fullSource.getHarvestStatus().setHarvest_status(HarvestEnum.success_iteration);
							}
							if (fullSource.getHarvestStatus().getDistributionTokensFree() != fullSource.getDistributionFactor()) {
								updateHarvestDistributionState_newToken(fullSource.getId(), fullSource.getHarvestStatus().getDistributionTokensFree(), fullSource.getHarvestStatus().getHarvest_status(), false);
								nextSetToProcess.removeLast();
								nNumSourcesGot--;							
							}
						}
					}//TESTED
					//
					//(end DISTRIBUTION LOGIC)
					////////////////////////////////////////////////////////////////////////
					
				}//(end found source - note could have been gazumped by a different thread in the meantime, and that's fine)
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

		// Little bit more distribution logic:
		if (null != putMeBackAtTheStart_distributed) {
			synchronized (SourceUtils.class) { // (can be called across multiple threads)
				for (SourcePojo distSource: putMeBackAtTheStart_distributed) {
					uncheckedSources.addFirst(distSource);
				}
			}			
		}//TESTED
		
		return nextSetToProcess;
	} //TESTED

    /////////////////////////////////////////////////////////////////////////////////////
    
	// Sub-utility function used by both the above functions
	
	private static BasicDBObject generateNotInProgressClause(Date date) {
		
		//24hrs ago
		Date oldDate = new Date(date.getTime() - _ONEDAY);
		
		// This query says: if the query isn't in progress [1] (or the harvest object doesn't exist [3,4]) ... or if it is but nothing's happened in 24 hours [2]
		
		BasicDBObject subclause1 = new BasicDBObject(SourceHarvestStatusPojo.sourceQuery_harvest_status_, 
														new BasicDBObject(MongoDbManager.ne_, HarvestEnum.in_progress.toString()));
		BasicDBObject subclause2 = new BasicDBObject();
		subclause2.put(SourceHarvestStatusPojo.sourceQuery_harvested_, new BasicDBObject(MongoDbManager.lt_, oldDate));
			// (always check for harvested, don't care if synced isn't happening regularly)
		BasicDBObject subclause3 = new BasicDBObject(SourcePojo.harvest_, new BasicDBObject(MongoDbManager.exists_, false));
		BasicDBObject subclause4 = new BasicDBObject(SourceHarvestStatusPojo.sourceQuery_harvest_status_, 
														new BasicDBObject(MongoDbManager.exists_, false));
		
		BasicDBObject clause = new BasicDBObject(MongoDbManager.or_, Arrays.asList(subclause1, subclause2, subclause3, subclause4));
		return clause;
	}//TESTED
	
	//(NOTE: IF RUN IN CONJUNCTION WITH "ABUSIVE" MAP/REDUCE WILL CAUSE DB HANG)
	private static void addSearchCycleClause(BasicDBObject currQuery, Date now) {
		BasicDBObject subclause1 = new BasicDBObject(SourcePojo.searchCycle_secs_, new BasicDBObject(MongoDbManager.exists_, false));
		StringBuffer js = new StringBuffer();
		js.append("(null == this.harvest) || ('success_iteration'== this.harvest.harvest_status) || (null == this.harvest.harvested) || (null == this.searchCycle_secs) || ((this.searchCycle_secs >= 0) && ((this.harvest.harvested.getTime() + 1000*this.searchCycle_secs) <= ");
		js.append(now.getTime());
		js.append("))");
		BasicDBObject subclause2 = new BasicDBObject(MongoDbManager.where_, js.toString());
		currQuery.append(MongoDbManager.or_, Arrays.asList(subclause1, subclause2));
	}//TESTED (by hand/eye)
	
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

	public static void updateHarvestStatus(SourcePojo source, HarvestEnum harvestStatus, List<DocumentPojo> added, long nDocsDeleted, String extraMessage) {
		// Handle successful harvests where the max docs were reached, so don't want to respect the searchCycle
		if ((harvestStatus == HarvestEnum.success) && (source.reachedMaxDocs())) {
			harvestStatus = HarvestEnum.success_iteration;
		}
		// Always update status object in order to release the "in_progress" lock
		// (make really really sure we don't exception out before doing this!)

		BasicDBObject query = new BasicDBObject(SourcePojo._id_, source.getId());
		BasicDBObject setClause = new BasicDBObject(SourceHarvestStatusPojo.sourceQuery_harvest_status_, harvestStatus.toString());
		if ((null != added) && !added.isEmpty()) {
			setClause.put(SourceHarvestStatusPojo.sourceQuery_extracted_, new Date());
		}
		if (null != extraMessage) {
			if ((null == source.getHarvestStatus()) || (null == source.getHarvestStatus().getHarvest_message())) {
				setClause.put(SourceHarvestStatusPojo.sourceQuery_harvest_message_, extraMessage);
			}
			else {
				source.getHarvestStatus().setHarvest_message(source.getHarvestStatus().getHarvest_message() + "\n" + extraMessage);
				setClause.put(SourceHarvestStatusPojo.sourceQuery_harvest_message_, source.getHarvestStatus().getHarvest_message());
			}
		}
		BasicDBObject update = new BasicDBObject(MongoDbManager.set_, setClause);

		int docsAdded = 0;
		if (null != added) {
			docsAdded = added.size();
		}
		BasicDBObject incClause = new BasicDBObject(SourceHarvestStatusPojo.sourceQuery_doccount_, docsAdded - nDocsDeleted);
		update.put(MongoDbManager.inc_, incClause);
		
		if (null != source.getDistributionTokens()) { // Distribution logic (specified and also enabled - eg ignore Feed/DB)
			updateHarvestDistributionState_tokenComplete(source, harvestStatus, incClause, setClause);
		}
		if (setClause.isEmpty()) { // (ie got removed by the distribution logic above)
			update.remove(MongoDbManager.set_);
		}//TESTED
		
		long nTotalDocsAfterInsert = 0;
		BasicDBObject fieldsToReturn = new BasicDBObject(SourceHarvestStatusPojo.sourceQuery_doccount_, 1);
		BasicDBObject updatedSource = 
			(BasicDBObject) DbManager.getIngest().getSource().findAndModify(query, fieldsToReturn, null, false, update, true, false);
		BasicDBObject harvestStatusObj = (BasicDBObject) updatedSource.get(SourcePojo.harvest_);
		if (null != harvestStatusObj) {
			Long docCount = harvestStatusObj.getLong(SourceHarvestStatusPojo.doccount_);
			if (null != docCount) {
				nTotalDocsAfterInsert = docCount;
			}
		}
		//TESTED
		
		// Prune documents if necessary
		if ((null != source.getMaxDocs()) && (nTotalDocsAfterInsert > source.getMaxDocs())) {
			long nToPrune = (nTotalDocsAfterInsert - source.getMaxDocs());
			SourceUtils.pruneSource(source, (int) nToPrune);
			nDocsDeleted += nToPrune;
			
			// And update to reflect that it now has max docs...
			BasicDBObject update2_1 = new BasicDBObject(SourceHarvestStatusPojo.sourceQuery_doccount_, source.getMaxDocs());
			BasicDBObject update2 = new BasicDBObject(DbManager.set_, update2_1);
			DbManager.getIngest().getSource().update(query, update2);
		}					
		//TESTED

		// (OK now the only thing we really had to do is complete, add some handy metadata)

		// Also update the document count table in doc_metadata:
		if (docsAdded > 0) {
			if (1 == source.getCommunityIds().size()) { // (simple/usual case, just 1 community)
				query = new BasicDBObject(DocCountPojo._id_, source.getCommunityIds().iterator().next());
				update = new BasicDBObject(MongoDbManager.inc_, new BasicDBObject(DocCountPojo.doccount_, docsAdded - nDocsDeleted));	
				if ((docsAdded != 0) || (nDocsDeleted != 0)) {
					update.put(DbManager.set_, new BasicDBObject(DocCountPojo.extracted_, new Date()));
				}
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
					if ((communityInfo.getValue() != 0) || (nDocsDeleted_byCommunity != 0)) {
						update.put(DbManager.set_, new BasicDBObject(DocCountPojo.extracted_, new Date()));
					}
					DbManager.getDocument().getCounts().update(query, update, true, false);
					// (true for upsert, false for multi add)
				}
			}//(never called in practice - tested up until 5/2/2014)
		}
	}//TESTED (actually, except for multi community sources, which can't happen at the moment anyway)

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

	/////////////////////////////////////////////////////////////////////////////////////

	// Prune sources with max doc settings

	private static void pruneSource(SourcePojo source, int nToPrune)
	{
		int nDocsDeleted = 0;
	
		// (code taken mostly from SourceHandler.deleteSource)
		if (null != source.getKey()) { // or may delete everything!
			BasicDBObject docQuery = new BasicDBObject(DocumentPojo.sourceKey_, source.getKey());
			docQuery.put(DocumentPojo.index_, new BasicDBObject(DbManager.ne_, "?DEL?")); // (robustness)
			BasicDBObject sortField = new BasicDBObject(DocumentPojo._id_, 1);
			BasicDBObject docFields = new BasicDBObject();
			docFields.append(DocumentPojo.url_, 1);
			docFields.append(DocumentPojo.sourceUrl_, 1);
			docFields.append(DocumentPojo.index_, 1);
			docFields.append(DocumentPojo.sourceKey_, 1);
			
			StoreAndIndexManager dataStore = new StoreAndIndexManager();
			ObjectId nextId = null;
			while (nToPrune > 0) {
				int nToDelete = nToPrune;
				if (nToDelete > 10000) {
					nToDelete = 10000;
				}
				if (null != nextId) {
					docQuery.put(DocumentPojo._id_, new BasicDBObject(DbManager.gt_, nextId));
				}//TESTED (by hand)
				
				DBCursor dbc = DbManager.getDocument().getMetadata().find(docQuery, docFields).sort(sortField).limit(nToDelete); 
					// (ie batches of 10K, ascending ordered by _id)
				
				nToPrune -= nToDelete;
				if (0 == nDocsDeleted) {
					nDocsDeleted = dbc.count();
				}
				if (0 == dbc.size()) {
					break;
				}
				List<DocumentPojo> docs = DocumentPojo.listFromDb(dbc, DocumentPojo.listType());
				
				nextId = dataStore.removeFromDatastore_byURL(docs);
			}
		}
		// No need to do anything related to soft deletion, this is all handled when the harvest ends 
	}//TESTED

	//////////////////////////////////////////////////////
	
	// Utility to get harvest name for display purposes
	
	private static String _harvestHostname = null;
	private static String getHostname() {
		// (just get the hostname once)
		if (null == _harvestHostname) {
			try {
				_harvestHostname = InetAddress.getLocalHost().getHostName();
			} catch (Exception e) {
				_harvestHostname = "UNKNOWN";
			}
		}		
		return _harvestHostname;
	}//TESTED


	////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////

	// DISTRIBUTION UTILITIES
	
	//
	// Update the distibution state BEFORE the source is processed
	// (note can set in here because currently the status is in_process so no other threads can touch it)
	//	
	
	private static void updateHarvestDistributionState_newToken(ObjectId sourceId, int distributionTokensFree, HarvestEnum harvestStatus, boolean bResetOldState) {
		BasicDBObject query = new BasicDBObject(SourcePojo._id_, sourceId);
		BasicDBObject setClause = new BasicDBObject(SourceHarvestStatusPojo.sourceQuery_distributionTokensFree_, distributionTokensFree);
		if (bResetOldState) {
			setClause.put(SourceHarvestStatusPojo.sourceQuery_distributionTokensComplete_, 0);
			setClause.put(SourceHarvestStatusPojo.sourceQuery_distributionReachedLimit_, false); 
		}//TESTED
		setClause.put(SourceHarvestStatusPojo.sourceQuery_harvest_status_, harvestStatus.toString());
		BasicDBObject update = new BasicDBObject(MongoDbManager.set_, setClause);
		MongoDbManager.getIngest().getSource().update(query, update, false, false);
		
		//DEBUG
		//System.out.println(" NEW_TOKEN=" + query.toString() + " / " + update.toString());

	}//TESTED
	
	//
	// Update the distibution state AFTER the source is processed
	// (note can set here if source is complete because that means no other thread can have control)
	// returns true if harvest is complete
	//	
	// NOTE this isn't called if an error occurs during the ingest cycle (which is where almost all the errors are called)
	// as a result, the source will linger with incomplete/unavailable tokens until it's seen by the getDistributedSourceList
	// again - normally this will be quick because the sources keep getting put back on the uncheckedList
	//
	
	private static boolean updateHarvestDistributionState_tokenComplete(SourcePojo source, HarvestEnum harvestStatus, BasicDBObject incClause, BasicDBObject setClause) {
		
		// Update tokens complete, and retrieve modified version 
		int nTokensToBeCleared = source.getDistributionTokens().size(); 
		BasicDBObject query = new BasicDBObject(SourcePojo._id_, source.getId());
		BasicDBObject modify = new BasicDBObject(MongoDbManager.inc_, 
				new BasicDBObject(SourceHarvestStatusPojo.sourceQuery_distributionTokensComplete_, nTokensToBeCleared));
		BasicDBObject fields = new BasicDBObject(SourceHarvestStatusPojo.sourceQuery_distributionTokensComplete_, 1);
		fields.put(SourceHarvestStatusPojo.sourceQuery_harvest_status_, 1);		
		fields.put(SourceHarvestStatusPojo.sourceQuery_distributionReachedLimit_, 1);		
		BasicDBObject partial = (BasicDBObject) MongoDbManager.getIngest().getSource().findAndModify(query, fields, null, false, modify, true, false);
			//(return new version - ensures previous increments have been taken into account)
		
		// Two cases: source complete (all tokens obtained), source incomplete:
		
		if (null != partial) { // (else yikes!)
			BasicDBObject partialStatus = (BasicDBObject) partial.get(SourcePojo.harvest_);
			if (null != partialStatus) { // (else yikes!)
				int nTokensComplete = partialStatus.getInt(SourceHarvestStatusPojo.distributionTokensComplete_, 0); 
					// (note after increment)
				
				// COMPLETE: reset parameters, status -> error (if anything has errored), success (all done), success_iteration (more to do)
				
				if (nTokensComplete == source.getDistributionFactor()) {
					setClause.put(SourceHarvestStatusPojo.sourceQuery_distributionTokensComplete_, 0);
					setClause.put(SourceHarvestStatusPojo.sourceQuery_distributionTokensFree_, source.getDistributionFactor());
					setClause.put(SourceHarvestStatusPojo.sourceQuery_distributionReachedLimit_, false); // (resetting this)
					// This source is now complete
					String status = partialStatus.getString(SourceHarvestStatusPojo.harvest_status_, null);
					Boolean reachedLimit = partialStatus.getBoolean(SourceHarvestStatusPojo.distributionReachedLimit_, false) || source.reachedMaxDocs();

					if ((null != status) 
							&& ((status.equalsIgnoreCase(HarvestEnum.error.toString()) || (HarvestEnum.error == harvestStatus))))
					{
						setClause.put(SourceHarvestStatusPojo.sourceQuery_harvest_status_, HarvestEnum.error.toString());
					}//TESTED (current and previous state == error)
					else if (reachedLimit || (HarvestEnum.success_iteration == harvestStatus)) {
						
						setClause.put(SourceHarvestStatusPojo.sourceQuery_harvest_status_, HarvestEnum.success_iteration.toString());							
					}//TESTED (from previous or current state)
						
					// (else leave with default of success)
					
					//DEBUG
					//System.out.println(Thread.currentThread().getName() + " COMPLETE_SRC COMPLETE_TOKEN=" + source.getKey() + " / " + setClause.toString() + " / " + incClause.toString() + " / " + nTokensComplete);
					
					return true;
					
				}//TESTED
				else { // Not complete
					
					// If we're here then we're only allowed to update the status to error
					if (HarvestEnum.error != harvestStatus) {
						setClause.remove(SourceHarvestStatusPojo.sourceQuery_harvest_status_);
					}//TESTED
					if (source.reachedMaxDocs()) {
						setClause.put(SourceHarvestStatusPojo.sourceQuery_distributionReachedLimit_, true);
					}//TESTED

					//DEBUG
					//System.out.println(Thread.currentThread().getName() + " COMPLETE_TOKEN=" + source.getKey() + " / " + setClause.toString() + " / " + incClause.toString() + " / " + nTokensComplete);
					
					return false;
					
				}//(end is complete or not)
				//TESTED (reached max limit)
				
			}//(end found partial source status, else catastrophic failure)
		}//(end found partial source, else catastrophic failure)
				
		return false;
		
	}//TESTED
	
}
