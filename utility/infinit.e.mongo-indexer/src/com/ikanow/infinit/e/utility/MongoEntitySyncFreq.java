package com.ikanow.infinit.e.utility;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.bson.types.ObjectId;

import com.ikanow.infinit.e.data_model.store.MongoDbManager;
import com.ikanow.infinit.e.data_model.store.feature.entity.EntityFeaturePojo;
import com.mongodb.BasicDBObject;
import com.mongodb.Bytes;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.WriteConcern;

public class MongoEntitySyncFreq {

	public static class EntitySyncFreqKey {
		String index;
		ObjectId communityId;
		@Override
		public int hashCode() {
			if ((index == null)||(communityId == null)) return 0;
			return index.hashCode() + communityId.hashCode();
		}
		@Override
		public boolean equals(Object rhsObj) {
			EntitySyncFreqKey rhs = (EntitySyncFreqKey)rhsObj;
			return index.equals(rhs.index) && communityId.equals(rhs.communityId);
		}
	}
	public static class EntitySyncFreqVal {
		long doccount;
		long totalfreq;
		boolean bSeen = false;
	}
	
	// Very specific function to perform part of "shard friendly" synchronization
	
	public static void syncFreq(String indexDatabase, String indexCollection, String sConfigPath) {
		
		// Command line processing
		com.ikanow.infinit.e.data_model.Globals.setIdentity(com.ikanow.infinit.e.data_model.Globals.Identity.IDENTITY_SERVICE);
		if (null != sConfigPath) {
			com.ikanow.infinit.e.data_model.Globals.overrideConfigLocation(sConfigPath);
		}
		
		MongoDbManager.getFeature().getEntity().setWriteConcern(WriteConcern.NORMAL);
			// (optimize performance, don't really care if we miss the odd write)
		
		DBCollection indexCollectionObj = MongoDbManager.getCollection(indexDatabase, indexCollection);
		BasicDBObject sort = new BasicDBObject("_id", 1); // (should be == $natural by construction)
		DBCursor iterator = indexCollectionObj.find().batchSize(1000).sort(sort).addOption(Bytes.QUERYOPTION_NOTIMEOUT);
		int nIts = 0;
		int nObjs = 0;
		int nObjThresh = 100000;
		String first = null;
		String last = null;
		HashMap<EntitySyncFreqKey, EntitySyncFreqVal> lookup = new HashMap<EntitySyncFreqKey, EntitySyncFreqVal>();
		for (Object itObj: iterator) {
			BasicDBObject it = (BasicDBObject)itObj;
			
			BasicDBObject key = (BasicDBObject) it.get("_id");
			BasicDBObject value = (BasicDBObject) it.get("value");
			if ((null == key) || (null == value)) {
				continue; // random error
			}
			String index = key.getString("index");
			ObjectId commId = key.getObjectId("comm");
			if ((null == index) || (null == commId)) {
				continue; // random error
			}
			Long doccount = value.getLong("dc");
			Long totalfreq = value.getLong("tf");
			if ((null == doccount) || (null == totalfreq)) {
				continue; // random error
			}
			
			if (null == first) {
				first = index;
			}

			// Lookahead - is the *next* element the same, don't do the lookup if not: 
			boolean bSameIndex = (last != null) && last.equals(index);			
			
			if ((nObjs >= nObjThresh) && !bSameIndex) {
				//(can only do this across changes in index since we're ignoring the communityId in the query we construct)
				nIts++;
				updateEntityFeatures(lookup, first, last, nIts, nObjs, false);
				lookup = new HashMap<EntitySyncFreqKey, EntitySyncFreqVal>();
				nObjs = 0;
				first = index;
			}//TESTED (including same index logic)
			
			last = index;
			
			EntitySyncFreqKey myKey = new EntitySyncFreqKey();
			myKey.index = index;
			myKey.communityId = commId;
			EntitySyncFreqVal myVal = new EntitySyncFreqVal();
			myVal.doccount = doccount;
			myVal.totalfreq = totalfreq;
			
			lookup.put(myKey, myVal);
			nObjs++;
			
		}//(end loop over objects to sync)
		
		updateEntityFeatures(lookup, first, last, 1 + nIts, nObjs, true); // -1 for last iteration
			// (call this even if nObjs==0 so it removes anything after last..)
		
		iterator.close();

	}//TESTED
	
	private static void updateEntityFeatures(HashMap<EntitySyncFreqKey, EntitySyncFreqVal> lookup, String first, String last, int nIt, int nObjs, boolean bLast) {
		System.out.println(new Date().toString() + ": iteration=" + nIt + ", numObjs=" +  nObjs);
		DBCollection entityFeatureColl = MongoDbManager.getFeature().getEntity();

		if (1 == nIt) { // Remove anything prior to the very first index encountered
			BasicDBObject removeOldInitialObjects = new BasicDBObject(EntityFeaturePojo.index_, new BasicDBObject(MongoDbManager.lt_, first));
			MongoEntityFeatureTxfer.doDelete(removeOldInitialObjects, 0);
			entityFeatureColl.remove(removeOldInitialObjects);
		}//TESTED
		
		if (bLast) { // Remove anything after this spot
			BasicDBObject removeOldInitialObjects = new BasicDBObject(EntityFeaturePojo.index_, new BasicDBObject(MongoDbManager.gt_, last));
			MongoEntityFeatureTxfer.doDelete(removeOldInitialObjects, 0);
			if (0 == nObjs) {
				return; // nothing more to do...
			}
		}//TESTED
		
		int nUpdated = 0;
		int nRemoved = 0;
		BasicDBObject querySub = new BasicDBObject();
		querySub.put(MongoDbManager.gte_, first);
		querySub.put(MongoDbManager.lte_, last);
		BasicDBObject query = new BasicDBObject(EntityFeaturePojo.index_, querySub);
		BasicDBObject fields = new BasicDBObject(); 
		fields.put(EntityFeaturePojo._id_, 1);
		fields.put(EntityFeaturePojo.index_, 1);
		fields.put(EntityFeaturePojo.communityId_, 1);
		fields.put(EntityFeaturePojo.doccount_, 1);
		DBCursor dbc = entityFeatureColl.find(query, fields).batchSize(1000);

		//DEBUG
		//System.out.println("LOCATE QUERY = " + query);

		ArrayList<ObjectId> batchOfIdsToDelete = new ArrayList<ObjectId>(1000);
		
		EntitySyncFreqKey lookupKey = new EntitySyncFreqKey();
		for (Object itObj: dbc) {
			BasicDBObject it = (BasicDBObject) itObj;
			lookupKey.index = it.getString(EntityFeaturePojo.index_);
			lookupKey.communityId = it.getObjectId(EntityFeaturePojo.communityId_);
			EntitySyncFreqVal val = lookup.get(lookupKey);
			
			if ((null != val) && !val.bSeen) { // else can't find the entity, it's probably deaded - or have already seen it
				val.bSeen = true;
				
				Long nCurrDocCount = it.getLong(EntityFeaturePojo.doccount_);
				if (val.doccount != nCurrDocCount) { // i don't really care about totalfreq
					double dPrio = 1000.0;
					if ((null != nCurrDocCount) && (0 != nCurrDocCount)) {
						dPrio = (double)val.doccount/(double)nCurrDocCount;
						if ((dPrio > 0.) && (dPrio < 1.0)) { // handle case where it's now lower
							dPrio = 1.0/dPrio;
						}
						dPrio *= 100.0; // for some reason it's on this scale...
					}
					BasicDBObject updateQuery = new BasicDBObject(EntityFeaturePojo._id_, it.get(EntityFeaturePojo._id_));
					BasicDBObject updateSub = new BasicDBObject();
					updateSub.put(EntityFeaturePojo.doccount_, val.doccount);
					updateSub.put(EntityFeaturePojo.totalfreq_, val.totalfreq);
					updateSub.put(EntityFeaturePojo.db_sync_prio_, dPrio);
					BasicDBObject update = new BasicDBObject(MongoDbManager.set_, updateSub);
					//DEBUG
					//System.out.println("UPDATE: " + lookupKey.index + "/" + lookupKey.communityId + ": " + updateQuery + " / " + update.toString() + " (" + nUpdated);
					entityFeatureColl.update(updateQuery, update);
					nUpdated++;
				}//TESTED
				else {
					//DEBUG
					//System.out.println("IGNORE: " + lookupKey.index + "/" + lookupKey.communityId + ": " + val.doccount);					
				}
			}//TESTED
			else if ((null != val) && val.bSeen) { // Handle this differently, there's only 1 index entry so leave that alone
				BasicDBObject updateQuery = new BasicDBObject(EntityFeaturePojo._id_, it.get(EntityFeaturePojo._id_));
				entityFeatureColl.remove(updateQuery);
					// (assume this will happen relatively infrequently, so just called remove on this 1 entry)

				//DEBUG
				//System.out.println("REMOVEDUP: " + lookupKey.index + "/" + lookupKey.communityId + ": " + val + " (" + nRemoved);					

			}//TESTED
			else { // We'll delete this bad boy (either doesn't exist any more or it's a duplicate)
				
				batchOfIdsToDelete.add((ObjectId) it.get(EntityFeaturePojo._id_));
				if (batchOfIdsToDelete.size() >= 1000) {
					BasicDBObject toDel = new BasicDBObject(MongoDbManager.in_, batchOfIdsToDelete.toArray());
					MongoEntityFeatureTxfer.doDelete(new BasicDBObject(EntityFeaturePojo._id_, toDel), 0);
					batchOfIdsToDelete.clear();
				}
				nRemoved++;
				//DEBUG
				//System.out.println("REMOVE: " + lookupKey.index + "/" + lookupKey.communityId + ": " + val + " (" + nRemoved);					
			}//TESTED
		}
		if (!batchOfIdsToDelete.isEmpty()) {
			BasicDBObject toDel = new BasicDBObject(MongoDbManager.in_, batchOfIdsToDelete.toArray());
			MongoEntityFeatureTxfer.doDelete(new BasicDBObject(EntityFeaturePojo._id_, toDel), 0);
		}//TESTED
		
		System.out.println(new Date().toString() + ": end_iteration=" + nIt + ", updated=" +  nUpdated + ", removed=" + nRemoved);
	}//TESTED
}
