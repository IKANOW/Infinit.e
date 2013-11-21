/*******************************************************************************
 * Copyright 2012, The Infinit.e Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.ikanow.infinit.e.utility;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.bson.BSONObject;
import org.bson.types.ObjectId;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.ImmutableSettings.Builder;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ikanow.infinit.e.data_model.index.ElasticSearchManager;
import com.ikanow.infinit.e.data_model.index.IndexManager;
import com.ikanow.infinit.e.data_model.index.feature.entity.EntityFeaturePojoIndexMap;
import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.feature.entity.EntityFeaturePojo;
import com.ikanow.infinit.e.processing.generic.GenericProcessingController;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.MongoException;

public class MongoEntityFeatureTxfer 
{

	//___________________________________________________________________________________________________
	
	// MAIN
	
	/**
	 * @param args: 0 is config location, 1 is query, 2 is delete/split (optional)
	 * 				to run 3 options:
	 * 				Transfer: config query(opt)
	 * 				Delete: config query delete
	 * 				Split: config query split
	 * 
	 * @throws MongoException 
	 * @throws NumberFormatException 
	 * @throws IOException 
	 */
	public static void main(String sConfigPath, String sQuery, boolean bDelete, boolean bRebuildIndex, int nSkip, int nLimit, String chunksDescription) throws NumberFormatException, MongoException, IOException {
		
		MongoEntityFeatureTxfer txferManager = new MongoEntityFeatureTxfer();
		
		// Command line processing
		com.ikanow.infinit.e.data_model.Globals.setIdentity(com.ikanow.infinit.e.data_model.Globals.Identity.IDENTITY_SERVICE);
		if (null != sConfigPath) {
			com.ikanow.infinit.e.data_model.Globals.overrideConfigLocation(sConfigPath);
		}
		if (bRebuildIndex) {
			new GenericProcessingController().InitializeIndex(false, true, false);
		}
		
		BasicDBObject query = null;		
		if (null == sQuery) {
			query = new BasicDBObject();
		}
		else {
			query = (BasicDBObject) com.mongodb.util.JSON.parse(sQuery);
		}
		
		if (bDelete) {
			MongoEntityFeatureTxfer.doDelete(query, nLimit);
		}
		else {
			if (null == chunksDescription) {
				txferManager.doTransfer(query, nSkip, nLimit, null);
			}
			else {
				txferManager.doChunkedTransfer(query, nSkip, nLimit, chunksDescription);				
			}
		}
	}
	
	//___________________________________________________________________________________________________
	
	// Wrapper for doing transfer in chunks:
	
	private void doChunkedTransfer(BasicDBObject query, int nSkip, int nLimit, String chunksDescription) throws IOException
	{
		List<BasicDBObject> chunkList = MongoIndexerUtils.getChunks("feature.entity", chunksDescription);
		System.out.println("CHUNKS: Found " + chunkList.size() + " chunks");
		//DEBUG
		//System.out.println("Chunklist= " + chunkList);
		for (BasicDBObject chunk: chunkList) {
			BasicDBObject cleanQuery = new BasicDBObject();
			cleanQuery.putAll((BSONObject)query);
			String id = null;
			try {
				id = (String) chunk.remove("$id");
				System.out.println("CHUNK: " + id);
				doTransfer(cleanQuery, 0, 0, chunk);
			}
			catch (Exception e) {
				System.out.println("FAILED CHUNK: " + id + " ... " + e.getMessage());
			}
		}
	}//TESTED
	
	//___________________________________________________________________________________________________
	
	// PROCESSING LOOP (new interface)
	
	private void doTransfer(BasicDBObject query, int nSkip, int nLimit, BasicDBObject chunk)
	{		
		ElasticSearchManager elasticManager = null;
				
		// Initialize the DB:
		DBCollection entityFeatureDB = DbManager.getFeature().getEntity();
		
		// Initialize the ES (create the index if it doesn't already):
					
// 1. Set-up the entity feature index 

		String indexName = "entity_index";
		ElasticSearchManager.setDefaultClusterName("infinite-aws");
		
		// (delete the index)
		//elasticManager = ElasticSearchManager.getIndex(indexName);
		//elasticManager.deleteMe();
		
		// Create the index if necessary
		String sMapping = new Gson().toJson(new EntityFeaturePojoIndexMap.Mapping(), EntityFeaturePojoIndexMap.Mapping.class);
		Builder localSettings = ImmutableSettings.settingsBuilder();
		localSettings.put("number_of_shards", 1).put("number_of_replicas", 0);			
		localSettings.put("index.analysis.analyzer.suggestAnalyzer.tokenizer", "standard");
		localSettings.putArray("index.analysis.analyzer.suggestAnalyzer.filter", "standard", "lowercase");
		
		elasticManager = ElasticSearchManager.createIndex(indexName, null, false,	null, sMapping, localSettings);
		
		// Get the index (necessary if already created)
		if (null == elasticManager) 
		{
			elasticManager = ElasticSearchManager.getIndex(indexName);
		}
		
// Now query the DB:
		
		// Now apply chunk logic
		if (null != chunk) {
			if (null == query) {
				query = new BasicDBObject();			
			}
			for (String chunkField: chunk.keySet()) {				
				Object currQueryField = query.get(chunkField);
				if (null == currQueryField) { //easy...
					query.put(chunkField, chunk.get(chunkField));
				}
				else { // bit more complicated...
					if (currQueryField instanceof DBObject) { // both have modifiers - this is guaranteed for chunks
						((DBObject) currQueryField).putAll((DBObject) chunk.get(chunkField));
					}//TESTED
					else { // worst case, use $and
						query.put(DbManager.and_, Arrays.asList(
								new BasicDBObject(chunkField, query.get(chunkField)),
								new BasicDBObject(chunkField, chunk.get(chunkField))));
					}//TESTED
				}//TESTED (both cases)
			}
		}//TESTED
		
		DBCursor dbc = null;
		dbc = entityFeatureDB.find(query).skip(nSkip).limit(nLimit); 
		int nCount = dbc.count() - nSkip;
		if (nCount < 0) nCount = 0;
		System.out.println("Found " + nCount + " records to sync, process first " + (0==nLimit?nCount:nLimit));
		if (0 == nCount) { // Nothing to do...
			return;
		}
		
		List<EntityFeaturePojo> entities = new ArrayList<EntityFeaturePojo>();
		while ( dbc.hasNext() )
		{
			EntityFeaturePojo feature = EntityFeaturePojo.fromDb(dbc.next(),EntityFeaturePojo.class);
				
			if (null != feature.getAlias()) { // (some corrupt gazateer entry)

				// Handle groups (system group is: "4c927585d591d31d7b37097a")
				// if there is no community id, add system group (something is wrong if this happens?)
				if (null == feature.getCommunityId()) 
				{
					feature.setCommunityId(new ObjectId("4c927585d591d31d7b37097a"));						
				}
			}
			
			entities.add(feature);
			// Add the entities
			if ( entities.size() > 1000 )
			{
				elasticManager.bulkAddDocuments(
						IndexManager.mapListToIndex(entities, EntityFeaturePojo.listType(), new EntityFeaturePojoIndexMap()), 
						"_id", null, true);
					// (note EntityFeaturePojoIndexMap creates an "_id" field of the format index:community)
				
				entities = new ArrayList<EntityFeaturePojo>();
			}
		}
		//write whatevers left
		elasticManager.bulkAddDocuments(
				IndexManager.mapListToIndex(entities, EntityFeaturePojo.listType(), new EntityFeaturePojoIndexMap()), 
				"_id", null, true);
			// (note EntityFeaturePojoIndexMap creates an "_id" field of the format index:community)
		
	}
	
//___________________________________________________________________________________________________
	
	// DELETE DOCUMENTS FROM A QUERY
	
	static void doDelete(BasicDBObject query, int nLimit)
	{
		doDelete(query, nLimit, false);
	}
	static void doDelete(BasicDBObject query, int nLimit, boolean automatedRequest)
	{		
		try 
		{
			// Initialize the DB:	
			DBCollection entityFeatureDB = DbManager.getFeature().getEntity();
			ElasticSearchManager elasticManager = ElasticSearchManager.getIndex("entity_index");
			
			BasicDBObject fields = new BasicDBObject();
			fields.put(EntityFeaturePojo.index_, 1);
			fields.put(EntityFeaturePojo.communityId_, 1);
			
			DBCursor cur = entityFeatureDB.find(query, fields).limit(nLimit); 
				// (this internally works in batches of 1000)
			if (automatedRequest) {
				System.out.println("Found " + cur.count() + " records to delete from _id list");
			}
			else {
				System.out.println("Found " + cur.count() + " records to delete from " + query.toString());				
			}
			if (nLimit > 0) {
				System.out.println("(limited to " + nLimit + " records)");
			}
			int nArraySize = (cur.count() > 1000) ? 1000 : cur.count();
			ArrayList<EntityFeaturePojo> batchList = new ArrayList<EntityFeaturePojo>(nArraySize);			
			
			while (cur.hasNext())
			{
				EntityFeaturePojo gp = EntityFeaturePojo.fromDb(cur.next(),EntityFeaturePojo.class);
				batchList.add(gp);
				if (batchList.size() >= nArraySize) {
					internalDelete(batchList, elasticManager);
					batchList.clear();
				}
			}			
			if (!batchList.isEmpty()) {
				internalDelete(batchList, elasticManager);				
			}
			entityFeatureDB.remove(query);
			
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (MongoException e) {
			e.printStackTrace();
		}		
		finally 
		{
		}
		
	}//TESTED
	
	// Batch delete
	
	private static void internalDelete(List<EntityFeaturePojo> entitiesToDelete, ElasticSearchManager esMgr) {
		
		List<String> esids = new ArrayList<String>(entitiesToDelete.size());
		for (EntityFeaturePojo gp: entitiesToDelete) {
			esids.add(new StringBuffer(gp.getIndex()).append(':').append(gp.getCommunityId().toString()).toString());
		}		
		esMgr.bulkDeleteDocuments(esids);
	}//TESTED
	
	//___________________________________________________________________________________________________
	
	// TEST CODE
	
	@SuppressWarnings("unused")
	private void doUnitTestCode(String sMongoDbHost, String sMongoDbPort, String sElasticHost, String sElasticPort, 
			BasicDBObject query, int nLimit)
	{		
		Mongo mongoDB = null;
		ElasticSearchManager elasticManager = null;
		
		
		try {
			// Initialize the DB:
			
			mongoDB = new Mongo(sMongoDbHost, Integer.parseInt(sMongoDbPort));
			DBCollection gazDB = mongoDB.getDB("feature").getCollection("entity");
			
			// Initialize the ES (create the index if it doesn't already):
						
// 1. Set-up the entity feature index 

			String indexName = "entity_index";
			
			//TEST: delete the index:
//			elasticManager = ElasticSearchManager.getIndex(indexName, sElasticHost + ":" + sElasticPort);
//			elasticManager.deleteMe();

			//TEST: create the index
//			String sMapping = new Gson().toJson(new GazateerPojo.Mapping(), GazateerPojo.Mapping.class);
//			Builder localSettings = ImmutableSettings.settingsBuilder();
//			localSettings.put("number_of_shards", 1).put("number_of_replicas", 0);			 q
//			elasticManager = ElasticSearchManager.createIndex
//								(indexName, false, 
//										sElasticHost + ":" + sElasticPort, 
//										sMapping, localSettings);
			
			//TEST: delete the index:
//			elasticManager.deleteMe();
			
			//TEST: get the index:
//			elasticManager = ElasticSearchManager.getIndex(indexName, sElasticHost + ":" + sElasticPort);
			
// Now query the DB:
			
			DBCursor dbc = null;
			if (nLimit > 0) {
				dbc = gazDB.find(query).limit(nLimit); 
			}
			else { // Everything!
				dbc = gazDB.find(query); 			
			}
			
			Type listType = new TypeToken<ArrayList<EntityFeaturePojo>>() {}.getType();		
			List<EntityFeaturePojo> entities = new Gson().fromJson(dbc.toArray().toString(), listType);		
					
			//Debug:
			List<String> entIds = new LinkedList<String>();
			
			// Loop over array and invoke the cleansing function for each one

			for (EntityFeaturePojo ent: entities) {
				
				if (null != ent.getAlias()) { // (some corrupt gazateer entry)

					//Debug:
					//System.out.println("entity=" + ent.getGazateerIndex());
					//System.out.println("aliases=" + Arrays.toString(ent.getAlias().toArray()));
					
					// Insert into the elasticsearch index
					
					//Debug:
					//System.out.println(new Gson().toJson(ent, GazateerPojo.class));
					
					// Handle groups (system group is: "4c927585d591d31d7b37097a")
					if (null == ent.getCommunityId()) {
						ent.setCommunityId(new ObjectId("4c927585d591d31d7b37097a"));
					}
					
					//TEST: index documemt
//					ent.synchronizeWithIndex();
//					boolean b = elasticManager.addDocument(ent, ent.getGazateerIndex(), true);

					//TEST: remove document
					//b = elasticManager.removeDocument(ent.getGazateerIndex());
					
					//TEST: (part of get, bulk add/delete)
					entIds.add(ent.getIndex());
					
					// Debug:
//					if (!b) {
//						System.out.println("Didn't add " + ent.getGazateerIndex());						
//					}					
				}
				
			} // End loop over entities
			
			//TEST: bulk delete
			//elasticManager.bulkAddDocuments(entities, "index", null);
			//elasticManager.bulkDeleteDocuments(entIds);
			
			//TEST: get document
//			elasticManager.getRawClient().admin().indices().refresh(Requests.refreshRequest(indexName)).actionGet();
//			for (String id: entIds) {
//				Map<String, GetField> results = elasticManager.getDocument(id,"doccount", "disambiguated_name");
//				System.out.println(id + ": " + results.get("doccount").values().get(0) + " , " + results.get("disambiguated_name").values().get(0));
//			}
			
			//TEST: search
//			elasticManager.getRawClient().admin().indices().refresh(Requests.refreshRequest(indexName)).actionGet();
//			SearchRequestBuilder searchOptions = elasticManager.getSearchOptions();
//			XContentQueryBuilder queryObj = QueryBuilders.matchAllQuery();
//			searchOptions.addSort("doccount", SortOrder.DESC);
//			searchOptions.addFields("doccount", "type");
//			SearchResponse rsp = elasticManager.doQuery(queryObj, searchOptions);
//			SearchHit[] docs = rsp.getHits().getHits();
//			for (SearchHit hit: docs) {
//				String id = hit.getId();
//				Long doccount = (Long) hit.field("doccount").value();
//				String type = (String) hit.field("type").value();
//				System.out.println(id + ": " + doccount + ", " + type);
//			}			
			
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (MongoException e) {
			e.printStackTrace();
		}		
		finally {
			
			if (null != mongoDB) {
				mongoDB.close();
			}
			if (null != elasticManager) {
				//NB not sure when exactly to call this - probably can just not bother?
				//elasticManager.getRawClient().close();
			}
		} 
	}
	
}
