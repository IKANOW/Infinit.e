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

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.ImmutableSettings.Builder;

import com.google.gson.Gson;
import com.ikanow.infinit.e.data_model.index.ElasticSearchManager;
import com.ikanow.infinit.e.data_model.index.IndexManager;
import com.ikanow.infinit.e.data_model.index.feature.event.AssociationFeaturePojoIndexMap;
import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.MongoDbManager;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.ikanow.infinit.e.data_model.store.document.AssociationPojo;
import com.ikanow.infinit.e.data_model.store.feature.association.AssociationFeaturePojo;
import com.ikanow.infinit.e.processing.generic.GenericProcessingController;
import com.ikanow.infinit.e.processing.generic.aggregation.AssociationAggregationUtils;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.MongoException;

public class MongoAssociationFeatureTxfer 
{
//___________________________________________________________________________________________________
	
	// MAIN
	
	/**
	 * @param args: 0,1 is the location of the MongoDB host/port, 2/3 is the location of the ES index host/port
	 * @throws MongoException 
	 * @throws UnknownHostException 
	 * @throws NumberFormatException 
	 */
	public static void main(String sConfigPath, String sQuery, boolean bDelete, boolean bRebuildIndex, int nSkip, int nLimit) throws NumberFormatException, UnknownHostException, MongoException {
		
		MongoAssociationFeatureTxfer txferManager = new MongoAssociationFeatureTxfer();
		
		// Command line processing
		com.ikanow.infinit.e.data_model.Globals.setIdentity(com.ikanow.infinit.e.data_model.Globals.Identity.IDENTITY_SERVICE);
		if (null != sConfigPath) {
			com.ikanow.infinit.e.data_model.Globals.overrideConfigLocation(sConfigPath);
		}
		if (bRebuildIndex) {
			new GenericProcessingController().InitializeIndex(false, false, true);
		}
		
		BasicDBObject query = null;		
		if (null == sQuery) {
			query = new BasicDBObject();
		}
		else {
			query = (BasicDBObject) com.mongodb.util.JSON.parse(sQuery);
		}
		
		if (bDelete) {
			txferManager.doDelete(query, nLimit);
		}
		else {
			txferManager.doTransfer(query, nSkip, nLimit);						
		}
	}
	
	//___________________________________________________________________________________________________
	
	// PROCESSING LOOP (new interface)
	
	Map<String, SourcePojo> _sourceCache = new HashMap<String, SourcePojo>();
	
	private void doTransfer(BasicDBObject query, int nSkip, int nLimit)
	{		
		ElasticSearchManager elasticManager = null;		
		
		// Initialize the DB:
		DBCollection eventFeatureDB = DbManager.getFeature().getAssociation();
		
		// Initialize the ES (create the index if it doesn't already):
					
// 1. Set-up the entity feature index 

		ElasticSearchManager.setDefaultClusterName("infinite-aws");
		
		// (delete the index)
		//elasticManager = ElasticSearchManager.getIndex("association_index");
		//elasticManager.deleteMe();
		
		// Create the index if necessary
		String sMapping = new Gson().toJson(new AssociationFeaturePojoIndexMap.Mapping(), AssociationFeaturePojoIndexMap.Mapping.class);
		Builder localSettings = ImmutableSettings.settingsBuilder();
		localSettings.put("number_of_shards", 1).put("number_of_replicas", 0);			
		localSettings.put("index.analysis.analyzer.suggestAnalyzer.tokenizer", "standard");
		localSettings.putArray("index.analysis.analyzer.suggestAnalyzer.filter", "standard", "lowercase");
		
		elasticManager = ElasticSearchManager.createIndex("association_index", null, false, null, sMapping, localSettings);
		
		// Get the index (necessary if already created)
		if (null == elasticManager) 
		{
			elasticManager = ElasticSearchManager.getIndex("association_index");
		}
		
// Now query the DB:
		
		DBCursor dbc = null;
		dbc = eventFeatureDB.find(query).skip(nSkip).limit(nLimit); 
		int nCount = dbc.count() - nSkip;
		if (nCount < 0) nCount = 0;
		System.out.println("Found " + nCount + " records to sync, process first " + (0==nLimit?nCount:nLimit));
		if (0 == nCount) { // Nothing to do...
			return;
		}
		
		List<AssociationFeaturePojo> events = new LinkedList<AssociationFeaturePojo>();
		
		// Loop over array and invoke the cleansing function for each one
		while ( dbc.hasNext() )
		{
			BasicDBObject dbo = (BasicDBObject) dbc.next();
			AssociationFeaturePojo evt = AssociationFeaturePojo.fromDb(dbo,AssociationFeaturePojo.class);
			
			// If this table has just been rebuilt from the document then the indexes are all wrong ...
			// recalculate and save
			if ('#' == evt.getIndex().charAt(0)) {
				AssociationPojo singleEvt = new AssociationPojo();
				singleEvt.setEntity1_index(evt.getEntity1_index());
				singleEvt.setEntity2_index(evt.getEntity2_index());
				singleEvt.setVerb_category(evt.getVerb_category());
				singleEvt.setGeo_index(evt.getGeo_index());
				evt.setIndex(AssociationAggregationUtils.getEventFeatureIndex(singleEvt));
				eventFeatureDB.update(new BasicDBObject("_id", dbo.get("_id")), 
											new BasicDBObject(MongoDbManager.set_, 
													new BasicDBObject(AssociationFeaturePojo.index_, evt.getIndex())), false, true);
					// (has to be a multi-update even though it's unique because it's sharded on index)
			}
			
			// Handle groups (system group is: "4c927585d591d31d7b37097a")
			if (null == evt.getCommunityId()) 
			{
				evt.setCommunityId(new ObjectId("4c927585d591d31d7b37097a"));
			}
			// Bulk add prep
			events.add(evt);
					
			if ( events.size() > 1000 )
			{
				elasticManager.bulkAddDocuments(IndexManager.mapListToIndex(events, AssociationFeaturePojo.listType(), new AssociationFeaturePojoIndexMap()), "_id", null,true);
				events.clear();
			}
		}
		 // End loop over entities
		
		//write whatevers left
		elasticManager.bulkAddDocuments(IndexManager.mapListToIndex(events, AssociationFeaturePojo.listType(), new AssociationFeaturePojoIndexMap()), "_id", null,true);
			
	}
	//___________________________________________________________________________________________________
	
	// DELETE DOCUMENTS FROM A QUERY
	
	private void doDelete(BasicDBObject query, int nLimit)
	{		
		try 
		{
			// Initialize the DB:	
			DBCollection eventFeatureDB = DbManager.getFeature().getAssociation();
			
			DBCursor cur = eventFeatureDB.find(query).limit(nLimit); 
				// (this internally works in batches of 1000; just get _id)
			System.out.println("Found " + cur.count() + " records to delete");
			if (nLimit > 0) {
				System.out.println("(limited to " + nLimit + " records)");
			}		
			
			ArrayList<AssociationFeaturePojo> events = new ArrayList<AssociationFeaturePojo>();
			LinkedList<String> eventIds = new LinkedList<String>(); 
			while (cur.hasNext())
			{
				AssociationFeaturePojo event = AssociationFeaturePojo.fromDb(cur.next(),AssociationFeaturePojo.class);	
				events.add(event);
				eventIds.add(new StringBuffer(event.getIndex()).append(":").append(event.getCommunityId()).toString());
				eventFeatureDB.remove(new BasicDBObject("index", event.getIndex()));
			}
			ElasticSearchManager elasticManager = ElasticSearchManager.getIndex("association_index");
			elasticManager.bulkDeleteDocuments(eventIds);
			
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (MongoException e) {
			e.printStackTrace();
		}		
	}	
}
