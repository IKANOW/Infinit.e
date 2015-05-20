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
package com.ikanow.infinit.e.processing.custom.output;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import org.bson.types.ObjectId;
import org.elasticsearch.action.admin.cluster.state.ClusterStateResponse;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.ImmutableSettings.Builder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilders;

import com.ikanow.infinit.e.data_model.index.ElasticSearchManager;
import com.ikanow.infinit.e.data_model.index.IndexManager;
import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.MongoDbManager;
import com.ikanow.infinit.e.data_model.store.custom.mapreduce.CustomMapReduceJobPojo;
import com.ikanow.infinit.e.data_model.utils.ThreadSafeSimpleDateFormat;
import com.ikanow.infinit.e.processing.custom.utils.InfiniteHadoopUtils;
import com.mongodb.BasicDBObject;
import com.mongodb.CommandResult;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

public class CustomOutputIndexingEngine {

	////////////////////////////////////////////////////////////////////////////////////
	
	// TOP LEVEL LOGIC

	//TODO: at some point for performance, it should probably do the indexing from within the custom job output format class
	// we can set up all the initialization from "prepareOutputCollection" (use the size of the previous iteration)
	// and then just sort out the alias swapping from here
	
	/**
	 * Tidy up the output collection _before_ it is written into
	 */
	public static void prepareOutputCollection(CustomMapReduceJobPojo job) {
		// (currently nothing to do here) 
	}
	
	public static void completeOutput(CustomMapReduceJobPojo job, Date ageOutTime, ObjectId newIdThreshold) {
		
		BasicDBObject outputSettings = new BasicDBObject();
		if (!isIndexed(job, outputSettings)) {
			return;
		}
		
		if ((null == job.appendResults) || !job.appendResults) {
			
			// delete/re-create index, fill with data, swap the aliases, delete the old index 
			
			ElasticSearchManager customIndex = createNewIndex(job, true);
			transferData(customIndex, job, newIdThreshold);
			swapAliases(customIndex, job.communityIds, false);
			// remove old index			
			String indexToRemove = new StringBuilder("custom_").append(job.outputCollection).toString();			
			if (IndexManager.pingIndex(indexToRemove)) { // delete if it exists...
				IndexManager.getIndex(indexToRemove).deleteMe();
			}

		}
		else { // append mode
			//TODO: note that if swapping from non-append to append mode, then might run into...
			// ...shards-too-small issue: will live with this for now (preferred option is to use aliases)
			
			// create index (and add aliases), fill with data, remove aged-out data
			ElasticSearchManager customIndex = getExistingIndex(job);
			if (null == customIndex) { // easy case, create index 
				customIndex = createNewIndex(job, false);
				swapAliases(customIndex, job.communityIds, false);
			}
			transferData(customIndex, job, newIdThreshold);
			deleteOldCustomRecords(customIndex, ageOutTime);
		}
	}//TODO TOTEST (append mode), TESTED (api mode) 
	
	// (Called when the job is completed)
	public static void deleteOutput(CustomMapReduceJobPojo job) {
		String indexToRemove = new StringBuilder("custom_").append(job.outputCollection).toString();			
		if (IndexManager.pingIndex(indexToRemove)) { // delete if it exists...
			IndexManager.getIndex(indexToRemove).deleteMe();
		}
		indexToRemove = new StringBuilder("custom_").append(job.outputCollectionTemp).toString();			
		if (IndexManager.pingIndex(indexToRemove)) { // delete if it exists...
			IndexManager.getIndex(indexToRemove).deleteMe();
		}		
	}//TESTED (by hand via API)
	
	////////////////////////////////////////////////////////////////////////////////////
	
	// UTILITY CODE
	
	private static boolean isIndexed(CustomMapReduceJobPojo job, BasicDBObject settings) {
		BasicDBObject postProcObject = (BasicDBObject) com.mongodb.util.JSON.parse(InfiniteHadoopUtils.getQueryOrProcessing(job.query, InfiniteHadoopUtils.QuerySpec.POSTPROC));
		if (null == postProcObject) {
			return false;
		}
		if (null != settings) {
			settings.putAll((DBObject)postProcObject);
		}		
		String indexMode = postProcObject.getString("indexMode", null);
		
		return indexMode != null;
	}//TESTED (by hand via API) 
	
	public static boolean isIndexed(CustomMapReduceJobPojo job) {
		return isIndexed(job, null);
	}//TESTED (by hand via API)
	
	public static ElasticSearchManager getExistingIndex(CustomMapReduceJobPojo job) {
		if (!isIndexed(job)) {
			return null;
		}
		boolean appendMode = ((null == job.appendResults) || !job.appendResults);
		String outputCollectionToUse = appendMode ? job.outputCollection : job.outputCollection;
		String indexToBuild = new StringBuilder("custom_").append(outputCollectionToUse).toString();
		
		if (!IndexManager.pingIndex(indexToBuild)) { 
			return null;
		}
		return IndexManager.getIndex(indexToBuild);
	}//TESTED (by hand - via changing the communities in updateJob)
	
	private static ElasticSearchManager createNewIndex(CustomMapReduceJobPojo job, boolean deleteExisting) {
		boolean appendMode = !((null == job.appendResults) || !job.appendResults);
		String outputCollectionToUse = appendMode ? job.outputCollection : job.outputCollectionTemp;
		String indexToBuild = new StringBuilder("custom_").append(outputCollectionToUse).toString();
		
		if (deleteExisting) {
			if (IndexManager.pingIndex(indexToBuild)) { // delete if it exists...
				IndexManager.getIndex(indexToBuild).deleteMe();
			}
		}
		
		// Recreate the index, size will depend on the size of the collection 
		
		DBCollection outputCollection = DbManager.getCollection(job.getOutputDatabase(), outputCollectionToUse);
		long collectionSize = 1L;
		CommandResult outputCollectionStats = outputCollection.getStats();
		if (null != outputCollectionStats) {
			collectionSize = outputCollectionStats.getLong("size", 1L);
		}
		// Let's say 1 shard/500MB
		final long SHARD_SIZE = 500L*1024L*1024L;
		int numShards = 2;
		if (appendMode) { // increase the size for the future shards=1 + 2*(initial size + 1)
			numShards = 1 + (int)((collectionSize)/SHARD_SIZE); // (round down here, since going to be >1 anyway)
			numShards++;
			numShards *= 2;
			numShards++; // (somewhat arbitrary equation, 1 shard -> 5 was pretty much the justification...)
		}
		else { // round up in this case
			numShards = 1 + (int)((collectionSize + SHARD_SIZE - 1)/SHARD_SIZE); // round up, ie min 2 (1 shard == distribute across all nodes, by INF convention)
		}
		
		Builder localSettingsEvent = ImmutableSettings.settingsBuilder();
		localSettingsEvent.put("number_of_shards", numShards).put("number_of_replicas", 0);
		//TODO: (no replicas for now ... in theory can always re-generate this .. though in practice need a post proc tool to make it easier)
		ElasticSearchManager customIndex = IndexManager.createIndex(indexToBuild, "custom", false, null, null, localSettingsEvent);
			// (don't need to set the mapping - that happens automatically because I've registered a template)
		
		return customIndex;		
	}//TESTED (basic functionality)
	
    private static ThreadSafeSimpleDateFormat _dateFormat = new ThreadSafeSimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
	
	private static void transferData(ElasticSearchManager customIndex, CustomMapReduceJobPojo job, ObjectId newIdThreshold) {
		//(lots of scope for performance improvements here, but the intention is to move all this into the job itself, so it's probably moot)
		
		boolean appendMode = !((null == job.appendResults) || !job.appendResults);
		String outputCollectionToUse = appendMode ? job.outputCollection : job.outputCollectionTemp;
		DBCollection outputCollection = DbManager.getCollection(job.getOutputDatabase(), outputCollectionToUse);
		
		final int BATCH_SIZE = 1000;
		BasicDBObject query = new BasicDBObject();
		if (null != newIdThreshold) {
			query.put("_id", new BasicDBObject(MongoDbManager.gt_, newIdThreshold));
		}
		DBCursor dbc = outputCollection.find(query).batchSize(BATCH_SIZE);
		Iterator<DBObject> it = dbc.iterator();
		ArrayList<DBObject> toEsList = new ArrayList<DBObject>(BATCH_SIZE);
		String jobtitle = new StringBuilder("custom:").append(job.jobtitle).toString();
		while (it.hasNext()) {
			DBObject dbo = it.next();
			// Object transforms:
			// 1) _id needs to be a string
			ObjectId _id = (ObjectId) dbo.get("_id");
			dbo.put("_id", _id.toString());
			// 2) set timestamp if not set by user:
			Object ts = dbo.get("@timestamp");
			if (null == ts) {
				dbo.put("@timestamp", _dateFormat.format(_id.getDate()));
			}
			// 3) always overwrite sourceKey with job title
			dbo.put("sourceKey", jobtitle);
			Object message = dbo.get("message");
			// 4) add a message field if one doesn't already exist...
			if (null == message) {
				dbo.put("message", dbo.get("key").toString());
			}
			// 5) Support for incremental mode
			Object updateId = dbo.get("_updateId");
			if (null != updateId) {
				dbo.put("_id", updateId.toString());
			}
			toEsList.add(dbo);
			
			if (toEsList.size() >= BATCH_SIZE) {
				customIndex.bulkAddDocuments(toEsList, "_id", null, true);
				toEsList.clear();
			}
		}
		if (toEsList.size() > 0) {
			customIndex.bulkAddDocuments(toEsList, "_id", null, true);
		}
	}//TESTED (by hand - non-append mode: timestamp, _id, key)
	//TODO: totest .. updateId, message ... append mode
	
	public static void swapAliases(ElasticSearchManager customIndex, List<ObjectId> newCommIds, boolean deleteOldAliases) {
		// Add new aliases as sets of communities (at most 10 per alias)
		// (NOTE: the API will enforce that the user belong to all communities)
		TreeSet<ObjectId> newTreeSet = new TreeSet<ObjectId>();
		HashSet<String> aliasSet = new HashSet<String>();
		if (null != newCommIds) {
			newTreeSet.addAll(newCommIds);
			int added = 0;
			StringBuffer sb = new StringBuffer("customs");
			for (ObjectId commId: newCommIds) {
				sb.append("_").append(commId.toString());
				added++;
				if (added >= 10) {
					customIndex.createAlias(sb.toString());
					aliasSet.add(sb.toString());					
					added = 0;
					sb = new StringBuffer("customs");
				}//TESTED
			}
			if (added > 0) {
				customIndex.createAlias(sb.toString());
				aliasSet.add(sb.toString());				
			}//TESTED
		}
		
		if (deleteOldAliases) {
			// Remove all old aliases (unless we already have them)
			ClusterStateResponse retVal = customIndex.getRawClient().admin().cluster().prepareState()
					.setIndices(customIndex.getIndexName())
					.setRoutingTable(false).setNodes(false).setListenerThreaded(false).get();
	
			for (IndexMetaData indexMetadata: retVal.getState().getMetaData()) {
				Iterator<String> aliasIt = indexMetadata.aliases().keysIt();
				while (aliasIt.hasNext()) {
					String alias = aliasIt.next();
					if (!alias.equals(customIndex.getIndexName()) && !aliasSet.contains(alias)) {
						customIndex.removeAlias(alias);					
					}
				}
			}//TESTED
		}
	}//TESTED
	
	@SuppressWarnings("deprecation")
	private static void deleteOldCustomRecords(ElasticSearchManager customIndex, Date deleteThreshold) {
		if (null == deleteThreshold) {
			return;
		}
		customIndex.doDeleteByQuery(QueryBuilders.constantScoreQuery(FilterBuilders.numericRangeFilter("@timestamp").to(deleteThreshold.getTime())));
	}
}
