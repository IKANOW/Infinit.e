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
package com.ikanow.infinit.e.processing.generic;

import java.util.List;

//import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.ImmutableSettings.Builder;

import com.google.gson.Gson;
import com.ikanow.infinit.e.data_model.InfiniteEnums;
import com.ikanow.infinit.e.data_model.index.ElasticSearchManager;
import com.ikanow.infinit.e.data_model.index.IndexManager;
import com.ikanow.infinit.e.data_model.index.document.DocumentPojoIndexMap;
import com.ikanow.infinit.e.data_model.index.feature.entity.EntityFeaturePojoIndexMap;
import com.ikanow.infinit.e.data_model.index.feature.event.AssociationFeaturePojoIndexMap;
import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.MongoDbManager;
import com.ikanow.infinit.e.data_model.store.config.source.SourceHarvestStatusPojo;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.ikanow.infinit.e.data_model.store.document.DocCountPojo;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.infinit.e.data_model.store.document.EntityPojo;
import com.ikanow.infinit.e.data_model.store.feature.association.AssociationFeaturePojo;
import com.ikanow.infinit.e.data_model.store.feature.entity.EntityFeaturePojo;
import com.ikanow.infinit.e.data_model.store.social.community.CommunityPojo;
import com.ikanow.infinit.e.processing.generic.aggregation.AggregationManager;
import com.ikanow.infinit.e.processing.generic.store_and_index.StoreAndIndexManager;
import com.ikanow.infinit.e.processing.generic.utils.PropertiesManager;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;

public class GenericProcessingController {

	//NOTE THIS FUNCTION SHOULD CONTAIN NO STATE SINCE IT CAN BE RUN ACROSS MULTIPLE THREADS
	
	//(Nothing currently to log)
	//private static final Logger logger = Logger.getLogger(GenericProcessingController.class);

	///////////////////////////////////////////////////////////////////////////////////////
	//
	// Set up the databases and indexes
	
	public void Initialize() {
		InitializeDatabase();
		InitializeIndex(false, false, false);
			// (Don't delete anything, obviously)
	}
	
	public void InitializeDatabase() {
		// Add indices:
		try 
		{
			PropertiesManager pm = new PropertiesManager();
			
			DbManager.getDocument().getContent().ensureIndex(new BasicDBObject(DocumentPojo.url_, 1)); // (annoyingly necessary)
			DbManager.getDocument().getMetadata().ensureIndex(new BasicDBObject(DocumentPojo.sourceUrl_, 1));
			
			// Compound index lets me access {url, sourceKey}, {url} efficiently ... but need sourceKey separately to do {sourceKey}
			BasicDBObject compIndex = new BasicDBObject(DocumentPojo.url_, 1);
			compIndex.put(DocumentPojo.sourceKey_, 1);
			DbManager.getDocument().getMetadata().ensureIndex(compIndex);
			DbManager.getDocument().getMetadata().ensureIndex(new BasicDBObject(DocumentPojo.sourceKey_, 1)); // (this is also needed standalone)
			DbManager.getDocument().getMetadata().ensureIndex(new BasicDBObject(DocumentPojo.title_, 1));
			DbManager.getDocument().getMetadata().ensureIndex(DocumentPojo.updateId_);
			if (!pm.getAggregationDisabled()) {
				compIndex = new BasicDBObject(EntityPojo.docQuery_index_, 1);
				compIndex.put(DocumentPojo.communityId_, 1);
				DbManager.getDocument().getMetadata().ensureIndex(compIndex);
			}
			compIndex = new BasicDBObject(DocCountPojo._id_, 1);
			compIndex.put(DocCountPojo.doccount_, 1);
			DbManager.getDocument().getCounts().ensureIndex(compIndex);
			DbManager.getFeature().getEntity().ensureIndex(new BasicDBObject(EntityFeaturePojo.disambiguated_name_, 1));
			DbManager.getFeature().getEntity().ensureIndex(new BasicDBObject(EntityFeaturePojo.index_, 1));
			DbManager.getFeature().getEntity().ensureIndex(new BasicDBObject(EntityFeaturePojo.alias_, 1));
			DbManager.getFeature().getAssociation().ensureIndex(new BasicDBObject(AssociationFeaturePojo.index_, 1));
			DbManager.getFeature().getGeo().ensureIndex(new BasicDBObject("country", 1));
			DbManager.getFeature().getGeo().ensureIndex(new BasicDBObject("search_field", 1));
			DbManager.getFeature().getGeo().ensureIndex(new BasicDBObject("geoindex", "2d"));
			DbManager.getIngest().getSource().ensureIndex(new BasicDBObject(SourcePojo.key_, 1));
			DbManager.getIngest().getSource().ensureIndex(new BasicDBObject(SourcePojo.communityIds_, 1));
			DbManager.getIngest().getSource().ensureIndex(new BasicDBObject(SourceHarvestStatusPojo.sourceQuery_harvested_, 1));
			DbManager.getIngest().getSource().ensureIndex(new BasicDBObject(SourceHarvestStatusPojo.sourceQuery_synced_, 1));
			// Compound index lets me access {type, communities._id}, {type} efficiently 
			DbManager.getSocial().getShare().ensureIndex(new BasicDBObject("type", 1), new BasicDBObject("communities._id", 1));
			DbManager.getCustom().getLookup().ensureIndex(new BasicDBObject("jobidS", 1));
			DbManager.getCustom().getLookup().ensureIndex(new BasicDBObject("jobtitle", 1));
			DbManager.getCustom().getLookup().ensureIndex(new BasicDBObject("waitingOn", 1));
		}
		catch (Exception e)  {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		}
	}//TESTED (not changed since by-eye test in Beta)
						
	// (Note some of the code below is duplicated in MongoDocumentTxfer, so make sure you sync changes)
	
	public void InitializeIndex(boolean bDeleteDocs, boolean bDeleteEntityFeature, boolean bDeleteEventFeature) {
		
		try { //create elasticsearch indexes
			
			PropertiesManager pm = new PropertiesManager();
			
			if (!pm.getAggregationDisabled()) {
				
				Builder localSettingsEvent = ImmutableSettings.settingsBuilder();
				localSettingsEvent.put("number_of_shards", 1).put("number_of_replicas", 0);
				localSettingsEvent.put("index.analysis.analyzer.suggestAnalyzer.tokenizer", "standard");
				localSettingsEvent.putArray("index.analysis.analyzer.suggestAnalyzer.filter", "standard", "lowercase");
	
				localSettingsEvent.put("index.analysis.analyzer.suggestAnalyzer.tokenizer", "standard");
				localSettingsEvent.putArray("index.analysis.analyzer.suggestAnalyzer.filter", "standard", "lowercase");
				
				Builder localSettingsGaz = ImmutableSettings.settingsBuilder();
				localSettingsGaz.put("number_of_shards", 1).put("number_of_replicas", 0);
				localSettingsGaz.put("index.analysis.analyzer.suggestAnalyzer.tokenizer", "standard");
				localSettingsGaz.putArray("index.analysis.analyzer.suggestAnalyzer.filter", "standard", "lowercase");
			
				//event feature
				String eventGazMapping = new Gson().toJson(new AssociationFeaturePojoIndexMap.Mapping(), AssociationFeaturePojoIndexMap.Mapping.class);	
				ElasticSearchManager eventIndex = IndexManager.createIndex(AssociationFeaturePojoIndexMap.indexName_, null, false, null, eventGazMapping, localSettingsEvent);
				if (bDeleteEventFeature) {
					eventIndex.deleteMe();
					eventIndex = IndexManager.createIndex(AssociationFeaturePojoIndexMap.indexName_, null, false, null, eventGazMapping, localSettingsEvent);
				}
				//entity feature
				String gazMapping = new Gson().toJson(new EntityFeaturePojoIndexMap.Mapping(), EntityFeaturePojoIndexMap.Mapping.class);	
				ElasticSearchManager entityIndex = IndexManager.createIndex(EntityFeaturePojoIndexMap.indexName_, null, false, null, gazMapping, localSettingsGaz);
				if (bDeleteEntityFeature) {
					entityIndex.deleteMe();
					entityIndex = IndexManager.createIndex(EntityFeaturePojoIndexMap.indexName_, null, false, null, gazMapping, localSettingsGaz);
				}
			}
			
			//DOCS - much more complicated than anything else 
			
			boolean bRebuildDocsIndex = !ElasticSearchManager.pingIndex(DocumentPojoIndexMap.globalDocumentIndex_);			
			createCommunityDocIndex(DocumentPojoIndexMap.globalDocumentIndex_, null, false, true, bDeleteDocs);
			createCommunityDocIndex(DocumentPojoIndexMap.manyGeoDocumentIndex_, null, false, false, bDeleteDocs);
			
			// Some hardwired dummy communities
			createCommunityDocIndex("4e3706c48d26852237078005", null, true, false, bDeleteDocs); // (admin)
			createCommunityDocIndex("4e3706c48d26852237079004", null, true, false, bDeleteDocs); // (test user)
				// (create dummy index used to keep personal group aliases)
						
			// OK, going to have different shards for different communities:
			// Get a list of all the communities:
			
			DBCursor dbc = DbManager.getSocial().getCommunity().find();
			
			if (bRebuildDocsIndex || bDeleteDocs) {

				while (dbc.hasNext()) {
					BasicDBObject dbo = (BasicDBObject) dbc.next();
					// OK, going to see if there are any sources with this group id, create a new index if so:
					// (Don't use CommunityPojo data model here for performance reasons....
					//  (Also, haven't gotten round to porting CommunityPojo field access to using static fields))
					ObjectId communityId = (ObjectId) dbo.get("_id");
					boolean bPersonalGroup = dbo.getBoolean("isPersonalCommunity", false);
					boolean bSystemGroup = dbo.getBoolean("isSystemCommunity", false);
					ObjectId parentCommunityId = (ObjectId) dbo.get("parentId");
					createCommunityDocIndex(communityId.toString(), parentCommunityId, bPersonalGroup, bSystemGroup, bDeleteDocs);
					
				}//end loop over sources
			} // (end if need to do big loop over all sources)
		}
		catch (Exception e) 
		{
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		}
	}//TESTED (not changed since by-eye test in Beta - retested after moving code into createCommunityDocIndex below)
	
	///////////////////////////////////////////////////////////////////////////////////////
	
	// Utility code for creating community indexes
	
	public static void createCommunityDocIndex(String nameOrCommunityIdStr, ObjectId parentCommunityId, boolean bPersonalGroup, boolean bSystemGroup, boolean bClearIndex)
	{
		//create elasticsearch indexes
		PropertiesManager pm = new PropertiesManager();
		int nPreferredReplicas = pm.getMaxIndexReplicas();
		String docMapping = new Gson().toJson(new DocumentPojoIndexMap.Mapping(), DocumentPojoIndexMap.Mapping.class);
		
		Builder localSettingsDoc = ImmutableSettings.settingsBuilder();
		localSettingsDoc.put("number_of_shards", 10).put("number_of_replicas", nPreferredReplicas);	
		
		String sGroupIndex = null;
		try {
			sGroupIndex = new StringBuffer("doc_").append(new ObjectId(nameOrCommunityIdStr).toString()).toString();
		}
		catch (Exception e) {
			sGroupIndex = nameOrCommunityIdStr;
		}		
		
		if (!bPersonalGroup) {
			
			String parentCommunityIdStr = null;
			if (null != parentCommunityId) {
				parentCommunityIdStr = parentCommunityId.toString();
			}
			
			if ((null == parentCommunityIdStr) || (parentCommunityIdStr.equals("4c927585d591d31d7b37097a"))) {
				// (system community is hardwired - children of this community are ignored)
			
				int nShards = bSystemGroup? 10 : 5 ; // (system group is largest)
				
				// Remove the alias, in case it exists:
				// Then create an index with this name:
				Builder localSettingsGroupIndex = ImmutableSettings.settingsBuilder();
				localSettingsGroupIndex.put("number_of_shards", nShards).put("number_of_replicas", nPreferredReplicas);	
				
				ElasticSearchManager docIndex = IndexManager.createIndex(sGroupIndex, DocumentPojoIndexMap.documentType_, false, null, docMapping, localSettingsGroupIndex);
				if (bClearIndex) {
					docIndex.deleteMe();
					docIndex = IndexManager.createIndex(sGroupIndex, DocumentPojoIndexMap.documentType_, false, null, docMapping, localSettingsGroupIndex);
				}
				if (null != docIndex) {
					try {
						docIndex.pingIndex(); // (wait until it's created itself)
					}
					catch (Exception e) {} // (just make sure this doesn't die horribly)
					
					docIndex.closeIndex();
				}
			}
			else {
				String sParentGroupIndex = new StringBuffer("doc_").append(new ObjectId(parentCommunityIdStr).toString()).toString();
				ElasticSearchManager docIndex = IndexManager.getIndex(sParentGroupIndex);
				docIndex.createAlias(sGroupIndex);
				docIndex.closeIndex();
				// (do nothing on delete - that will be handled at the parent index level)
			}
			//TESTED (parents, children, and personal)
		}
		else {
			// Just create the dummy index, no different to getting it in practice
			Builder localSettingsGroupIndex = ImmutableSettings.settingsBuilder();
			localSettingsGroupIndex.put("number_of_shards", 1).put("number_of_replicas", 0); // (ie guaranteed to be local to each ES node)	
			ElasticSearchManager dummyGroupIndex = IndexManager.createIndex(DocumentPojoIndexMap.dummyDocumentIndex_, DocumentPojoIndexMap.documentType_, false, null, docMapping, localSettingsGroupIndex);
			if (null == dummyGroupIndex) {
				dummyGroupIndex = IndexManager.getIndex(DocumentPojoIndexMap.dummyDocumentIndex_);
			}			
			
			// Just create an alias, so that queries work arbitrarily:
			dummyGroupIndex.createAlias(sGroupIndex);			
			// (do nothing on delete since don't have any docs in here anyway)
		}
	}
	//TESTED

	///////////////////////////
	
	// (this utility function is needed for the legacy case where empty communities were
	//  treated as aliases of the dummy community ... first time I encounter a community, I need
	//  to recreate it...)
	
	public static void recreateCommunityDocIndex_unknownFields(ObjectId communityId, boolean bDeleteFirst) {
		CommunityPojo cp = CommunityPojo.fromDb(MongoDbManager.getSocial().getCommunity().findOne(new BasicDBObject("_id", communityId)), CommunityPojo.class);
		if (null != cp) {
			deleteCommunityDocIndex(communityId.toString(), cp.getParentId(), true);
				// (in the legacy world this would have been treated as a "personal" ie equivalently to a dummy community ...
				//  this does nothing if it's already a real community)
			
			if (bDeleteFirst) {
				deleteCommunityDocIndex(communityId.toString(), cp.getParentId(), cp.getIsPersonalCommunity());
			}			
			createCommunityDocIndex(communityId.toString(), cp.getParentId(), cp.getIsPersonalCommunity(), cp.getIsSystemCommunity(), false);
		}
	}
	//TESTED
	
	///////////////////////////
		
	public static void deleteCommunityDocIndex(String nameOrCommunityIdStr, ObjectId parentCommunityId, boolean bPersonalGroup) {
		
		String sGroupIndex = null;
		try {
			sGroupIndex = new StringBuffer("doc_").append(new ObjectId(nameOrCommunityIdStr).toString()).toString();
		}
		catch (Exception e) {
			sGroupIndex = nameOrCommunityIdStr;
		}		
		if (bPersonalGroup) {
			ElasticSearchManager dummyGroupIndex = IndexManager.getIndex(DocumentPojoIndexMap.dummyDocumentIndex_);
			dummyGroupIndex.removeAlias(sGroupIndex);
		}
		else if (null != parentCommunityId) {
			String sParentGroupIndex = new StringBuffer("doc_").append(parentCommunityId.toString()).toString();
			ElasticSearchManager docIndex = IndexManager.getIndex(sParentGroupIndex);
			docIndex.removeAlias(sGroupIndex);
			docIndex.closeIndex();
		}
		else {
			ElasticSearchManager docIndex = IndexManager.getIndex(sGroupIndex);
			docIndex.deleteMe();			
		}
		//TESTED (parent, children, and personal)
	}
	//TESTED (personal and system)
		
	///////////////////////////////////////////////////////////////////////////////////////
	//
	// Enrich and store documents
	// (and remove any documents)
	
	public void processDocuments(int harvestType, List<DocumentPojo> toAdd, List<DocumentPojo> toUpdate_subsetOfAdd, List<DocumentPojo> toDelete)
	{
		PropertiesManager props = new PropertiesManager();
		
		// Note: toAdd = toAdd(old) + toUpdate
		// Need to treat updates as follows:
		// - Delete (inc children, eg events) but get fields to keep (currently _id, created; in the future comments etc)

		// Delete toUpdate and toAdd (also overwriting "created" for updated docs, well all actually...)
		toDelete.addAll(toUpdate_subsetOfAdd);
		StoreAndIndexManager storageManager = new StoreAndIndexManager();
		storageManager.removeFromDatastore_byURL(toDelete, (harvestType != InfiniteEnums.DATABASE));
			// (note: expands toDelete if any sourceUrl "docs" are present, see FileHarvester)

		// (Storing docs messes up the doc/event/entity objects, so don't do that just yet...)
		
		// Aggregation:
		// 1+2. Create aggregate entities/events ("features") and write them to the DB
		// (then can store feeds - doesn't matter that the event/entities have been modified by the aggregation)
		// 3. (Scheduled for efficiency) Update all documents' frequencies based on new entities and events
		// 4. (Scheduled for efficiency) Synchronize with index [after this, queries can find them - so (2) must have happened]
			// (Syncronization currently "corrupts" the entities so needs to be run last)

		AggregationManager perSourceAggregation = null;
		
		if (!props.getAggregationDisabled()) {
			perSourceAggregation = new AggregationManager();
		}
		
		// 1+2]
		if (null != perSourceAggregation) {
			perSourceAggregation.doAggregation(toAdd, toDelete);
			perSourceAggregation.createOrUpdateFeatureEntries();
		}
		
		// Save feeds to feeds collection in MongoDb
		// (second field determines if content gets saved)
		if (null != perSourceAggregation) {
			perSourceAggregation.applyAggregationToDocs(toAdd);
				// (First save aggregated statistics back to the docs' entity/event instances)
		}
		storeFeeds(toAdd, (harvestType != InfiniteEnums.DATABASE));

		// Then finish aggregation:
		
		if (null != perSourceAggregation) {
			// 3]  
			perSourceAggregation.runScheduledDocumentUpdates();
			
			// 4] This needs to happen last because it "corrupts" the entities and events
			perSourceAggregation.runScheduledSynchronization();
		}
		
	}//TESTED (by eye - logic is v simple)
	
	//TOTEST
	
	///////////////////////////////////////////////////////////////////////////////////////
	//
	// STORAGE AND INDEXING
	//
	//////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Writes the feeds to the DB and index
	 * 
	 * @param feeds list of feeds to be added to db
	 */
	private void storeFeeds(List<DocumentPojo> docs, boolean bSaveContent)
	{
		if ( null != docs && docs.size() > 0 )
		{
			StoreAndIndexManager store = new StoreAndIndexManager();
			store.addToDatastore(docs, bSaveContent);
		}
	}//TESTED (by eye)
	
	// See StoreAndIndexManager
	
	///////////////////////////////////////////////////////////////////////////////////////
	//
	// AGGREGATION
	//
	//////////////////////////////////////////////////////////////////////////////////////

	// See AggregationManager
	
}
