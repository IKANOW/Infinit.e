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
package com.ikanow.infinit.e.processing.generic.aggregation;

import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.bson.types.ObjectId;

import com.ikanow.infinit.e.data_model.index.ElasticSearchManager;
import com.ikanow.infinit.e.data_model.index.IndexManager;
import com.ikanow.infinit.e.data_model.index.feature.entity.EntityFeaturePojoIndexMap;
import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.MongoDbManager;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.infinit.e.data_model.store.document.EntityPojo;
import com.ikanow.infinit.e.data_model.store.document.GeoPojo;
import com.ikanow.infinit.e.data_model.store.feature.entity.EntityFeaturePojo;
import com.ikanow.infinit.e.processing.generic.GenericProcessingController;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

//TODO (INF-1660): here and for assocs, I think I should delete entities that have doccount==0

public class EntityAggregationUtils {

	private static final Logger logger = Logger.getLogger(EntityAggregationUtils.class);	
	
	private static boolean _diagnosticMode = false;
	public static void setDiagnosticMode(boolean bMode) { _diagnosticMode = bMode; }
	
	private static boolean _incrementalMode = false;
	public static void setIncrementalMode(boolean mode) {
		_incrementalMode = mode;
	}

	///////////////////////////////////////////////////////////////////////////////////////	
	///////////////////////////////////////////////////////////////////////////////////////	

	// CREATING/UPDATE FEATURES
	
	///////////////////////////////////////////////////////////////////////////////////////	
	///////////////////////////////////////////////////////////////////////////////////////	
	
	/**
	 * Updates the feature entries for the list of entities
	 * that was just extracted including changing frequency,
	 * adding aliases etc
	 * 
	 * This method now has 3 steps:
	 * 1. Try to update alias
	 * 	1.a If fail, create new gaz
	 * 2. Update totalfreq and doccount
	 * 
	 * @param ents List of entities to update in the entity feature
	 */
	public static void updateEntityFeatures(Map<String, Map<ObjectId, EntityFeaturePojo>> entFeatures)
	{
		// Some diagnostic counters:
		int numCacheMisses = 0;
		int numCacheHits = 0;
		int numNewEntities = 0;
		long entityAggregationTime = new Date().getTime();
				
		DBCollection col = DbManager.getFeature().getEntity();
		
		// (This fn is normally run for a single community id)
		CommunityFeatureCaches.CommunityFeatureCache currCache = null;
		
		String savedSyncTime = null;
		for (Map<ObjectId, EntityFeaturePojo> entCommunity: entFeatures.values()) {
			
			Iterator<Map.Entry<ObjectId, EntityFeaturePojo>> it = entCommunity.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<ObjectId, EntityFeaturePojo> entFeatureKV = it.next();
				try {		
					EntityFeaturePojo entFeature = entFeatureKV.getValue();
	
					long nSavedDocCount = entFeature.getDoccount();
					long nSavedFreqCount = entFeature.getTotalfreq();
						// (these should be constant across all communities but keep it here
						//  so can assign it using entFeature, it's v cheap so no need to get once like for sync vars)
					
					// For each community, see if the entity feature already exists *for that community*					
					ObjectId communityID = entFeature.getCommunityId();
					if (null != communityID)
					{
						if ((null == currCache) || !currCache.getCommunityId().equals(entFeatureKV.getKey())) {
							currCache = CommunityFeatureCaches.getCommunityFeatureCache(entFeatureKV.getKey());							
							if (_diagnosticMode) {
								System.out.println("EntityAggregationUtils.updateEntityFeatures, Opened cache for community: " + entFeatureKV.getKey());
							}
						}//TESTED (by hand)
						
						// Is this in our cache? If so can short cut a bunch of the DB interaction:
						EntityFeaturePojo cachedEnt = currCache.getCachedEntityFeature(entFeature);
						if (null != cachedEnt) {							
							if (_incrementalMode) {
								if (_diagnosticMode) {
									System.out.println("EntityAggregationUtils.updateEntityFeatures, skip cached: " + cachedEnt.toDb());									
									//TODO (INF-2825): should be continue-ing here (after implementing incremental caching fully) so can use delta more efficiently...
								}
							}
							else if (_diagnosticMode) {
								System.out.println("EntityAggregationUtils.updateEntityFeatures, grabbed cached: " + cachedEnt.toDb());
							}								
							numCacheHits++;							
							
						}//TESTED (by hand)						
						else {
							numCacheMisses++;							
						}
						
						BasicDBObject query = new BasicDBObject(EntityFeaturePojo.index_, entFeature.getIndex());
						query.put(EntityFeaturePojo.communityId_, communityID);
						BasicDBObject updateOp = new BasicDBObject();
						// Add aliases:
						BasicDBObject updateOpA = new BasicDBObject();
						if (null != entFeature.getAlias()) {
							if ((null == cachedEnt) || (null == cachedEnt.getAlias()) || !cachedEnt.getAlias().containsAll(entFeature.getAlias())) {
								//(if the data we have is already cached, don't bother adding it again)
								BasicDBObject multiopE = new BasicDBObject(MongoDbManager.each_, entFeature.getAlias());
								updateOpA.put(EntityFeaturePojo.alias_, multiopE);
							}//TESTED (by hand)
						}
						// Add link data, if there is any:
						if ((null != entFeature.getSemanticLinks()) && !entFeature.getSemanticLinks().isEmpty()) {
							if ((null == cachedEnt) || (null == cachedEnt.getSemanticLinks()) || !cachedEnt.getSemanticLinks().containsAll(entFeature.getSemanticLinks())) {
								//(if the data we have is already cached, don't bother adding it again)
								BasicDBObject multiopF = new BasicDBObject(MongoDbManager.each_, entFeature.getSemanticLinks());							
								updateOpA.put(EntityFeaturePojo.linkdata_, multiopF);
							}//TESTED (by hand)
						}
						// OK - now we can copy across the fields into the cache:
						if (null != cachedEnt) {
							currCache.updateCachedEntityFeatureStatistics(cachedEnt, entFeature); //(entFeature is now fully up to date)
						}//TESTED (by hand)
						
						if (!updateOpA.isEmpty()) {
							updateOp.put(MongoDbManager.addToSet_, updateOpA);
						}
						// Update frequency:
						BasicDBObject updateOpB = new BasicDBObject();
						updateOpB.put(EntityFeaturePojo.totalfreq_, nSavedFreqCount);
						updateOpB.put(EntityFeaturePojo.doccount_, nSavedDocCount);
						updateOp.put(MongoDbManager.inc_, updateOpB);
	
						//try to use find/modify to see if something comes back and set doc freq/totalfreq
						BasicDBObject fields = new BasicDBObject(EntityFeaturePojo.totalfreq_,1);
						fields.put(EntityFeaturePojo.doccount_, 1);
						fields.put(EntityFeaturePojo.alias_, 1); 
						fields.put(EntityFeaturePojo.linkdata_, 1); 
							//(slightly annoying, since only want these 2 largish fields if updating freq but won't know
							// until after i've got this object)						
						fields.put(EntityFeaturePojo.db_sync_time_, 1);
						fields.put(EntityFeaturePojo.db_sync_doccount_, 1);
	
						DBObject dboUpdate = null;
						if (_diagnosticMode) {
							if (null == cachedEnt) {
								dboUpdate = col.findOne(query,fields);
							}
						}
						else {
							if (null != cachedEnt) {
								col.update(query, updateOp, false, false);
							}
							else { // Not cached - so have to grab the feature we're either getting or creating
								dboUpdate = col.findAndModify(query, fields, new BasicDBObject(), false, updateOp, false, true);
									// (can use findAndModify because specify index, ie the shard key)
									// (returns entity before the changes above, update the feature object below)
									// (also atomically creates the object if it doesn't exist so is "distributed-safe")
							}
						}
						if ((null != cachedEnt) || ((dboUpdate != null) && !dboUpdate.keySet().isEmpty())) // (feature already exists)
						{
							EntityFeaturePojo gp = cachedEnt;
							
							// (Update the entity feature to be correct so that it can be accurately synchronized with the index)
							if (null == gp) {
								gp = EntityFeaturePojo.fromDb(dboUpdate, EntityFeaturePojo.class);
								entFeature.setTotalfreq(gp.getTotalfreq() + nSavedFreqCount);
								entFeature.setDoccount(gp.getDoccount() + nSavedDocCount);
								entFeature.setDbSyncDoccount(gp.getDbSyncDoccount());
								entFeature.setDbSyncTime(gp.getDbSyncTime());
								if (null != gp.getAlias()) {
									entFeature.addAllAlias(gp.getAlias());
								}
								if (null != gp.getSemanticLinks()) {
									entFeature.addToSemanticLinks(gp.getSemanticLinks());
								}
							}//TESTED (cached case and non-cached case)
							// (in the cached case, entFeature has already been updated by updateCachedEntityFeatureStatistics)
							
							if (_diagnosticMode) {
								System.out.println("EntityAggregationUtils.updateEntityFeatures, found: " + ((BasicDBObject)gp.toDb()).toString());
								System.out.println("EntityAggregationUtils.updateEntityFeatures, ^^^ found from query: " + query.toString() + " / " + updateOp.toString());
							}
							// (In background aggregation mode we update db_sync_prio when checking the doc update schedule) 
						}
						else // (the object in memory is now an accurate representation of the database, minus some fields we'll now add)
						{
							numNewEntities++;
							
							// Synchronization settings for the newly created object
							if (null == savedSyncTime) {
								savedSyncTime = Long.toString(System.currentTimeMillis());
							}
							entFeature.setDbSyncDoccount(nSavedDocCount);
							entFeature.setDbSyncTime(savedSyncTime);
							
							// This is all "distributed safe" (apart from the db_syc_xxx and it doesn't matter if that is 
							// out of date, the update will just be slightly out-of-date at worst) since (otherwise) these fields are 
							// only set here, and the findAndModify is atomic
	
							// (Do in raw MongoDB for performance)
							BasicDBObject baseFields = new BasicDBObject();
							baseFields.put(EntityFeaturePojo.dimension_, entFeature.getDimension().toString());
							baseFields.put(EntityFeaturePojo.type_, entFeature.getType());
							baseFields.put(EntityFeaturePojo.disambiguated_name_, entFeature.getDisambiguatedName());
							baseFields.put(EntityFeaturePojo.db_sync_doccount_, entFeature.getDbSyncDoccount());
							baseFields.put(EntityFeaturePojo.db_sync_prio_, 1000.0);
							baseFields.put(EntityFeaturePojo.db_sync_time_, entFeature.getDbSyncTime());
							if ((null != entFeature.getSemanticLinks()) && !entFeature.getSemanticLinks().isEmpty()) {
								baseFields.put(EntityFeaturePojo.linkdata_, entFeature.getSemanticLinks());
							}
							
							//attempt to add geotag (makes necessary checks on util side)
							//also add ontology type if geotag is found
							EntityGeotagAggregationUtils.addEntityGeo(entFeature);
							if( entFeature.getGeotag() != null ) {
								BasicDBObject geo = new BasicDBObject(GeoPojo.lat_, entFeature.getGeotag().lat);
								geo.put(GeoPojo.lon_, entFeature.getGeotag().lon);
								baseFields.put(EntityFeaturePojo.geotag_, geo);
								
								if ( entFeature.getOntology_type() != null ) {
									baseFields.put(EntityFeaturePojo.ontology_type_, entFeature.getOntology_type());
								}
							}
							
							if (!_diagnosticMode) {
								// Store the object
								col.update(query, new BasicDBObject(MongoDbManager.set_, baseFields));
							}
							else {
								System.out.println("EntityAggregationUtils.updateEntityFeatures, not found: " + query.toString() + ": " + baseFields.toString());
							}
							
						}//(end first time this feature seen - globally)
						
						if (null == cachedEnt) { // First time we've seen this locally, so add to cache
							currCache.addCachedEntityFeature(entFeature);
							if (_diagnosticMode) {
								System.out.println("EntityAggregationUtils.updateEntityFeatures, added to cache: " + entFeature.toDb());
							}							
						}//TESTED (by hand)							
						
					}//(end if community id assigned)
				}
				catch (Exception e) {
					// Exception, remove from feature list
					it.remove();
					
					// If an exception occurs log the error
					logger.error("Exception Message: " + e.getMessage(), e);
				}
				
			}// (end loop over communities)
		}// (end loop over indexes)
		
		
		if ((numCacheHits > 0) || (numCacheMisses > 0)) { // ie some ents were grabbed
			int cacheSize = 0;
			if (null != currCache) {
				cacheSize = currCache.getEntityCacheSize();
			}
			StringBuffer logMsg = new StringBuffer() // (should append key, but don't have that...)
									.append(" ent_agg_time_ms=").append(new Date().getTime() - entityAggregationTime)
									.append(" total_ents=").append(entFeatures.size()).append(" new_ents=").append(numNewEntities)
									.append(" cache_misses=").append(numCacheMisses).append(" cache_hits=").append(numCacheHits).append(" cache_size=").append(cacheSize);
			
			logger.info(logMsg.toString());
		}
		
	}//TESTED (just by eye - made few changes during re-factoring)
	
	///////////////////////////////////////////////////////////////////////////////////////	
	///////////////////////////////////////////////////////////////////////////////////////	

	// DOCUMENT UPDATE
	
	///////////////////////////////////////////////////////////////////////////////////////	
	///////////////////////////////////////////////////////////////////////////////////////	
	
	// NOTE: IMPORTANT that only entFeature attributes used are index/totalfreq/doccount
	// (this is relied upon by BackgroundAggregationThread)
	
	public static void updateMatchingEntities(EntityFeaturePojo entFeature, ObjectId communityId)
	{
		String index = entFeature.getIndex();
		long totalFreq = entFeature.getTotalfreq();
		long docCount = entFeature.getDoccount();
		
		try
		{
			DBCollection docDb = DbManager.getDocument().getMetadata();
			
			BasicDBObject query1 = new BasicDBObject();
			query1.put(EntityPojo.docQuery_index_, index);
			query1.put(DocumentPojo.communityId_, communityId);
			
			BasicDBObject multiopB = new BasicDBObject();
			multiopB.put(EntityPojo.docUpdate_totalfrequency_, totalFreq);
			multiopB.put(EntityPojo.docUpdate_doccount_, docCount);			
			BasicDBObject multiopA = new BasicDBObject(MongoDbManager.set_, multiopB);

			if (_diagnosticMode) {
				System.out.println("EntityAggregationUtils.updateMatchingEntities: " + query1.toString() + " / " + multiopA.toString());
			}
			else {
				synchronized (GenericProcessingController.class) {
					// Because this op can be slow, and traverse a lot of disk, need to ensure that
					// we don't allow all the threads to hammer it at once (the updates all yield to each other
					// enough that the disk goes totally crazy)
					
					docDb.update(query1, multiopA, false, true);
					DbManager.getDocument().getLastError(DbManager.getDocument().getMetadata().getName());
						// (enforce consecutive accesses for this potentially very slow operation)
				}
				
				// Was originally checked updatedExisting but for INF-1406, it sometimes seemed to be 
				// checking the wrong command. I suspect the reason we had this code in here has gone away,
				// and it doesn't matter if this update occasionally fails anyway, it will just be out of date
				// so the check/retry has been removed.
			}
		}
		catch(Exception ex){
			logger.error(ex.getMessage(), ex);			
		}
	}//TESTED (by eye, mostly cut-and-paste from test Beta)
	
	///////////////////////////////////////////////////////////////////////////////////////	
	///////////////////////////////////////////////////////////////////////////////////////	

	// INDEX SYNCHRONIZATION
	
	///////////////////////////////////////////////////////////////////////////////////////	
	///////////////////////////////////////////////////////////////////////////////////////	

	// Synchronization: sync vs index, update sync counts for each community
	
	public static void synchronizeEntityFeature(EntityFeaturePojo entityFeature, ObjectId communityId) {
		DBCollection entityFeatureDb = DbManager.getFeature().getEntity();
		
		// NOTE: Important that feeds update occurs before synchronization, since the sync "corrupts" the entity
				
		if (_diagnosticMode || (null != entityFeature.getDbSyncTime()) || (null != entityFeature.getDb_sync_prio())) 
		{ 
			// Else this is a new feature so don't need to update the feature DB, only the index (if db_sync_prio null then have to update to avoid b/g aggergation loop)
			// (note that db_sync_prio will in practice not be set when this is a new feature because it will have same sync_doccount as doc_count)
			
			long nCurrTime = System.currentTimeMillis();
			//(query from top of the function, basically lookup on gaz_index)
			BasicDBObject update2 = new BasicDBObject();
			update2.put(EntityFeaturePojo.db_sync_time_, Long.toString(nCurrTime));
			update2.put(EntityFeaturePojo.db_sync_doccount_, entityFeature.getDoccount());
			BasicDBObject update = new BasicDBObject(MongoDbManager.set_, update2);
			BasicDBObject update3 = new BasicDBObject(EntityFeaturePojo.db_sync_prio_, 1);
			update.put(MongoDbManager.unset_, update3);
			BasicDBObject query = new BasicDBObject(EntityFeaturePojo.index_, entityFeature.getIndex());
			query.put(EntityFeaturePojo.communityId_, communityId);
	
			if (_diagnosticMode) {
				if ((null != entityFeature.getDbSyncTime()) || (null != entityFeature.getDb_sync_prio())) {
					System.out.println("EntityAggregationUtils.synchronizeEntityFeature, featureDB: " + query.toString() + " / " + update.toString());
				}
				else {
					System.out.println("(WOULD NOT RUN) EntityAggregationUtils.synchronizeEntityFeature, featureDB: " + query.toString() + " / " + update.toString());					
				}
			}
			else {
				entityFeatureDb.update(query, update, false, true);
			}
		}

		if (_diagnosticMode) {
			System.out.println("EntityAggregationUtils.synchronizeEntityFeature, synchronize: " + new StringBuffer(entityFeature.getIndex()).append(':').append(communityId).toString() + " = " + 
					IndexManager.mapToIndex(entityFeature, new EntityFeaturePojoIndexMap()));
		}
		else {
			ElasticSearchManager esm = IndexManager.getIndex(EntityFeaturePojoIndexMap.indexName_);				
			esm.addDocument(entityFeature, new EntityFeaturePojoIndexMap(), null, true);
				//(_id is set by the index map to index:communityId)
		}
	}//TESTED (by eye, mostly cut-and-paste from test Beta)
	
	///////////////////////////////////////////////////////////////////////////////////////
	
	// Set flag to synchronize entity features
	
	public static void markEntityFeatureForSync(EntityFeaturePojo entityFeature, ObjectId communityId) {
		DBCollection entityFeatureDb = DbManager.getFeature().getEntity();
		double dPrio = 100.0*(double)entityFeature.getDoccount()/(0.01 + (double)entityFeature.getDbSyncDoccount());
		entityFeature.setDb_sync_prio(dPrio);
		BasicDBObject query = new BasicDBObject(EntityFeaturePojo.index_, entityFeature.getIndex());
		query.put(EntityFeaturePojo.communityId_, communityId);
		BasicDBObject update = new BasicDBObject(MongoDbManager.set_, new BasicDBObject(EntityFeaturePojo.db_sync_prio_, dPrio));
		if (_diagnosticMode) {
			System.out.println("EntityAggregationUtils.markEntityFeatureForSynchronization, featureDB: " + query.toString() + " / " + update.toString());				
		}
		else {
			entityFeatureDb.update(query, update, false, true);
		}
	}//TESTED
}
