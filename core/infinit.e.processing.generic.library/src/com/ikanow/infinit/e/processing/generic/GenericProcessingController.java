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

import java.util.HashMap;
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
import com.ikanow.infinit.e.data_model.store.custom.mapreduce.CustomMapReduceJobPojo;
import com.ikanow.infinit.e.data_model.store.document.CompressedFullTextPojo;
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
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import org.elasticsearch.action.admin.cluster.state.ClusterStateRequest;
import org.elasticsearch.action.admin.cluster.state.ClusterStateResponse;
import org.elasticsearch.cluster.metadata.AliasMetaData;
import org.elasticsearch.common.collect.ImmutableMap;

//DEBUG (alias corruption)
//import org.elasticsearch.action.admin.indices.status.IndexStatus;
//import org.elasticsearch.action.admin.indices.status.IndicesStatusRequest;
//import org.elasticsearch.action.admin.indices.status.IndicesStatusResponse;

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
			
			BasicDBObject compIndex = new BasicDBObject(CompressedFullTextPojo.sourceKey_, 2);
			compIndex.put(CompressedFullTextPojo.url_, 2);
			DbManager.getDocument().getContent().ensureIndex(compIndex); // (annoyingly necessary)
			dropIndexIfItExists(DbManager.getDocument().getContent(), CompressedFullTextPojo.url_, 1);
			dropIndexIfItExists(DbManager.getDocument().getContent(), CompressedFullTextPojo.sourceKey_, 2);
			DbManager.getDocument().getMetadata().ensureIndex(new BasicDBObject(DocumentPojo.sourceUrl_, 2), new BasicDBObject(MongoDbManager.sparse_, true));
			dropIndexIfItExists(DbManager.getDocument().getMetadata(), DocumentPojo.sourceUrl_, 1);
			
			// Compound index lets me access {url, sourceKey}, {url} efficiently ... but need sourceKey separately to do {sourceKey}
			compIndex = new BasicDBObject(DocumentPojo.url_, 1);
			compIndex.put(DocumentPojo.sourceKey_, 1);
			DbManager.getDocument().getMetadata().ensureIndex(compIndex);
			// Add {_id:-1} to "standalone" sourceKey, sort docs matching source key by "time" (sort of!) 
			compIndex = new BasicDBObject(DocumentPojo.sourceKey_, 1);
			compIndex.put(DocumentPojo._id_, -1);
			DbManager.getDocument().getMetadata().ensureIndex(compIndex);
			dropIndexIfItExists(DbManager.getDocument().getMetadata(), DocumentPojo.sourceKey_, 1);
			// Title simply not needed, that was a mistake from an early iteration:
			dropIndexIfItExists(DbManager.getDocument().getMetadata(), DocumentPojo.title_, 1);
			DbManager.getDocument().getMetadata().ensureIndex(new BasicDBObject(DocumentPojo.updateId_, 2), new BasicDBObject(MongoDbManager.sparse_, true));
			dropIndexIfItExists(DbManager.getDocument().getMetadata(), DocumentPojo.updateId_, 1);
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
			DbManager.getFeature().getEntity().ensureIndex(new BasicDBObject(EntityFeaturePojo.db_sync_prio_, 2), new BasicDBObject(MongoDbManager.sparse_, true));
			DbManager.getFeature().getAssociation().ensureIndex(new BasicDBObject(AssociationFeaturePojo.index_, 1));
			DbManager.getFeature().getAssociation().ensureIndex(new BasicDBObject(AssociationFeaturePojo.db_sync_prio_, 2), new BasicDBObject(MongoDbManager.sparse_, true));
			DbManager.getFeature().getGeo().ensureIndex(new BasicDBObject("country", 1));
			DbManager.getFeature().getGeo().ensureIndex(new BasicDBObject("search_field", 1));
			DbManager.getFeature().getGeo().ensureIndex(new BasicDBObject("geoindex", "2d"));
			DbManager.getIngest().getSource().ensureIndex(new BasicDBObject(SourcePojo.key_, 1));
			DbManager.getIngest().getSource().ensureIndex(new BasicDBObject(SourcePojo.communityIds_, 1));
			DbManager.getIngest().getSource().ensureIndex(new BasicDBObject(SourceHarvestStatusPojo.sourceQuery_harvested_, 1));
			DbManager.getIngest().getSource().ensureIndex(new BasicDBObject(SourceHarvestStatusPojo.sourceQuery_synced_, 1));
			// Compound index lets me access {type, communities._id}, {type} efficiently
			compIndex = new BasicDBObject("type", 1);
			compIndex.put("communities._id", 1);
			DbManager.getSocial().getShare().ensureIndex(compIndex);
			dropIndexIfItExists(DbManager.getSocial().getShare(), "type", 1);
			DbManager.getSocial().getCookies().ensureIndex(new BasicDBObject("apiKey", 2), new BasicDBObject(MongoDbManager.sparse_, true));
			dropIndexIfItExists(DbManager.getSocial().getCookies(), "apiKey", 1);
			DbManager.getCustom().getLookup().ensureIndex(new BasicDBObject(CustomMapReduceJobPojo.jobtitle_, 1));
			//TODO (): MOVE THESE TO SPARSE INDEXES AFTER YOU'VE UPDATED THE LOGIC (SWAP THE 1 AND 2)
			DbManager.getCustom().getLookup().ensureIndex(new BasicDBObject(CustomMapReduceJobPojo.jobidS_, 1), new BasicDBObject(MongoDbManager.sparse_, false));
			dropIndexIfItExists(DbManager.getCustom().getLookup(),CustomMapReduceJobPojo.jobidS_, 2);
//			DbManager.getCustom().getLookup().ensureIndex(new BasicDBObject(CustomMapReduceJobPojo.jobidS_, 2), new BasicDBObject(MongoDbManager.sparse_, true));
//			dropIndexIfItExists(DbManager.getCustom().getLookup(),CustomMapReduceJobPojo.jobidS_, 1);
			DbManager.getCustom().getLookup().ensureIndex(new BasicDBObject(CustomMapReduceJobPojo.waitingOn_, 1), new BasicDBObject(MongoDbManager.sparse_, false));
			dropIndexIfItExists(DbManager.getCustom().getLookup(),CustomMapReduceJobPojo.waitingOn_, 2);
//			DbManager.getCustom().getLookup().ensureIndex(new BasicDBObject(CustomMapReduceJobPojo.waitingOn_, 2), new BasicDBObject(MongoDbManager.sparse_, true));
//			dropIndexIfItExists(DbManager.getCustom().getLookup(),CustomMapReduceJobPojo.waitingOn_, 1);
		}		
		catch (Exception e)  {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		}
	}//TESTED (not changed since by-eye test in Beta)
			
	private void dropIndexIfItExists(DBCollection coll, String indexName, int nIndexIndex)
	{
		StringBuffer indexNameStrBuff = new StringBuffer(indexName).append("_").append(nIndexIndex);
		String indexName2 = indexNameStrBuff.toString();
		List<DBObject> list = coll.getIndexInfo();
		for (DBObject dbo: list) {
			String name = (String) dbo.get("name");
			if (indexName2.equalsIgnoreCase(name)) {
				try { 
					coll.dropIndex(new BasicDBObject(indexName, nIndexIndex)); 
				} 
				catch (Exception e) {} 
			}
		}
	}//TESTED
	
	// (Note some of the code below is duplicated in MongoDocumentTxfer, so make sure you sync changes)
	public void InitializeIndex(boolean bDeleteDocs, boolean bDeleteEntityFeature, boolean bDeleteEventFeature) {
		InitializeIndex(bDeleteDocs, bDeleteEntityFeature, bDeleteEventFeature, false);
	}	
	public void InitializeIndex(boolean bDeleteDocs, boolean bDeleteEntityFeature, boolean bDeleteEventFeature, boolean bRebuildDocsIndex) {
		
		try { //create elasticsearch indexes
			
			PropertiesManager pm = new PropertiesManager();
			
			if (!pm.getAggregationDisabled()) {
				
				boolean languageNormalization = pm.getNormalizeEncoding();
				
				Builder localSettingsEvent = ImmutableSettings.settingsBuilder();
				localSettingsEvent.put("number_of_shards", 1).put("number_of_replicas", 0);
				localSettingsEvent.put("index.analysis.analyzer.suggestAnalyzer.tokenizer", "standard");
				if (languageNormalization) {
					localSettingsEvent.putArray("index.analysis.analyzer.suggestAnalyzer.filter", "icu_normalizer","icu_folding","standard","lowercase");
				}
				else {
					localSettingsEvent.putArray("index.analysis.analyzer.suggestAnalyzer.filter", "standard","lowercase");					
				}
	
				Builder localSettingsGaz = ImmutableSettings.settingsBuilder();
				localSettingsGaz.put("number_of_shards", 1).put("number_of_replicas", 0);
				localSettingsGaz.put("index.analysis.analyzer.suggestAnalyzer.tokenizer", "standard");
				if (languageNormalization) {
					localSettingsGaz.putArray("index.analysis.analyzer.suggestAnalyzer.filter", "icu_normalizer","icu_folding","standard","lowercase");
				}
				else {
					localSettingsGaz.putArray("index.analysis.analyzer.suggestAnalyzer.filter", "standard","lowercase");					
				}
			
				//event feature
				String eventGazMapping = new Gson().toJson(new AssociationFeaturePojoIndexMap.Mapping(), AssociationFeaturePojoIndexMap.Mapping.class);	
				ElasticSearchManager eventIndex = IndexManager.createIndex(AssociationFeaturePojoIndexMap.indexName_, null, false, null, eventGazMapping, localSettingsEvent);
				if (null == eventIndex) { // (if has been previously referenced in this process space)
					eventIndex = IndexManager.getIndex(AssociationFeaturePojoIndexMap.indexName_);
				}
				eventIndex.createAlias(AssociationFeaturePojoIndexMap.indexCollectionName_);
				if (bDeleteEventFeature) {
					eventIndex.deleteMe();
					eventIndex = IndexManager.createIndex(AssociationFeaturePojoIndexMap.indexName_, null, false, null, eventGazMapping, localSettingsEvent);
				}
				//entity feature
				String gazMapping = new Gson().toJson(new EntityFeaturePojoIndexMap.Mapping(), EntityFeaturePojoIndexMap.Mapping.class);	
				ElasticSearchManager entityIndex = IndexManager.createIndex(EntityFeaturePojoIndexMap.indexName_, null, false, null, gazMapping, localSettingsGaz);
				if (null == entityIndex) { // (if has been previously referenced in this process space)
					entityIndex = IndexManager.getIndex(EntityFeaturePojoIndexMap.indexName_);
				}
				entityIndex.createAlias(EntityFeaturePojoIndexMap.indexCollectionName_);
				if (bDeleteEntityFeature) {
					entityIndex.deleteMe();
					entityIndex = IndexManager.createIndex(EntityFeaturePojoIndexMap.indexName_, null, false, null, gazMapping, localSettingsGaz);
				}
			}
			
			//DOCS - much more complicated than anything else 

			boolean bPingMainIndexFailed = !ElasticSearchManager.pingIndex(DocumentPojoIndexMap.globalDocumentIndex_); 
				// (ie if main doc index doesn't exist then always rebuild all indexes)
			
			if (bPingMainIndexFailed) { // extra level of robustness... sleep for a minute then double check the index is really missing...
				try { Thread.sleep(60000); } catch (Exception e) {}
				bPingMainIndexFailed = !ElasticSearchManager.pingIndex(DocumentPojoIndexMap.globalDocumentIndex_);
			}
			bRebuildDocsIndex |= bPingMainIndexFailed;
			
			// check the main index has the "collection" alias - if not then rebuild everything
			
			if (!bPingMainIndexFailed && (null == _aliasInfo)) { 
				ElasticSearchManager docIndex = ElasticSearchManager.getIndex(DocumentPojoIndexMap.globalDocumentIndex_);
				ClusterStateResponse clusterState = docIndex.getRawClient().admin().cluster().state(new ClusterStateRequest()).actionGet();
				_aliasInfo = clusterState.getState().getMetaData().getAliases();
				if (!_aliasInfo.containsKey(DocumentPojoIndexMap.globalDocumentIndexCollection_)) {
					bRebuildDocsIndex = true;
				}
			} //TESTED
			
			createCommunityDocIndex(DocumentPojoIndexMap.globalDocumentIndex_, null, false, true, bDeleteDocs);
			createCommunityDocIndex(DocumentPojoIndexMap.manyGeoDocumentIndex_, null, false, false, bDeleteDocs);
			
			// Some hardwired dummy communities
			createCommunityDocIndex("4e3706c48d26852237078005", null, true, false, bDeleteDocs); // (admin)
			createCommunityDocIndex("4e3706c48d26852237079004", null, true, false, bDeleteDocs); // (test user)
				// (create dummy index used to keep personal group aliases)
						
			if (bRebuildDocsIndex || bDeleteDocs) {

				// OK, going to have different shards for different communities:
				// Get a list of all the communities:
				
				BasicDBObject query = new BasicDBObject();
				BasicDBObject fieldsToDrop = new BasicDBObject("members", 0);
				fieldsToDrop.put("communityAttributes", 0);
				fieldsToDrop.put("userAttributes", 0);
				DBCursor dbc = DbManager.getSocial().getCommunity().find(query, fieldsToDrop);
				
				List<DBObject> tmparray = dbc.toArray(); // (brings the entire thing into memory so don't get cursor timeouts)
				int i = 0;
				System.out.println("Initializing " + dbc.size() + " indexes:");
				for (int j = 0; j < 2; ++j) {
					for (DBObject dbotmp: tmparray) {
						if ((++i % 100) == 0) {
							System.out.println("Initialized " + i + " indexes.");
						}
						BasicDBObject dbo = (BasicDBObject) dbotmp;
						
						// OK, going to see if there are any sources with this group id, create a new index if so:
						// (Don't use CommunityPojo data model here for performance reasons....
						//  (Also, haven't gotten round to porting CommunityPojo field access to using static fields))
						ObjectId communityId = (ObjectId) dbo.get("_id");
						boolean bPersonalGroup = dbo.getBoolean("isPersonalCommunity", false);
						boolean bSystemGroup = dbo.getBoolean("isSystemCommunity", false);
						ObjectId parentCommunityId = (ObjectId) dbo.get("parentId");
						
						createCommunityDocIndex(communityId.toString(), parentCommunityId, bPersonalGroup, bSystemGroup, bDeleteDocs, j==0);
						
					}//end loop over communities
				}// end loop over communities - first time parents only
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
	
	public static void createCommunityDocIndex(String nameOrCommunityIdStr, ObjectId parentCommunityId,  boolean bPersonalGroup, boolean bSystemGroup, boolean bClearIndex)
	{
		createCommunityDocIndex(nameOrCommunityIdStr, parentCommunityId, bPersonalGroup, bSystemGroup, bClearIndex, false); 
	}
	
	public static void createCommunityDocIndex(String nameOrCommunityIdStr, ObjectId parentCommunityId, 
			boolean bPersonalGroup, boolean bSystemGroup, boolean bClearIndex, boolean bParentsOnly)
	{
		//create elasticsearch indexes
		PropertiesManager pm = new PropertiesManager();
		boolean languageNormalization = pm.getNormalizeEncoding();
		int nPreferredReplicas = pm.getMaxIndexReplicas();
		
		String docMapping = new Gson().toJson(new DocumentPojoIndexMap.Mapping(), DocumentPojoIndexMap.Mapping.class);
		
		String sGroupIndex = null; // for indexing, ie always a single index
		String sAliasIndex = null; // for querying, ie will point to doc_commid, doc_commid_1, etc
		try {
			sGroupIndex = new StringBuffer("doc_").append(new ObjectId(nameOrCommunityIdStr).toString()).toString();
			sAliasIndex = new StringBuffer("docs_").append(new ObjectId(nameOrCommunityIdStr).toString()).toString();
		}
		catch (Exception e) {
			sGroupIndex = nameOrCommunityIdStr;
			if (DocumentPojoIndexMap.globalDocumentIndex_.equals(nameOrCommunityIdStr)) { 
				sAliasIndex = DocumentPojoIndexMap.globalDocumentIndexCollection_;
			}
			else if (DocumentPojoIndexMap.manyGeoDocumentIndex_.equals(nameOrCommunityIdStr)) {
				sAliasIndex = DocumentPojoIndexMap.manyGeoDocumentIndexCollection_;				
			}
			else { // fallback
				sAliasIndex = nameOrCommunityIdStr.replaceAll("doc(?:ument)?_", "docs_");
			}
			//TESTED
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
				if (languageNormalization) {
					localSettingsGroupIndex.put("index.analysis.analyzer.default.tokenizer","standard");
					localSettingsGroupIndex.putArray("index.analysis.analyzer.default.filter", "icu_normalizer","icu_folding","standard","lowercase","stop");
				}//TESTED

				ElasticSearchManager docIndex = IndexManager.createIndex(sGroupIndex, DocumentPojoIndexMap.documentType_, false, null, docMapping, localSettingsGroupIndex);
				if (null == docIndex) { // index has already been referenced, hence createIndex returns null
					docIndex = IndexManager.getIndex(sGroupIndex);
				}
				if (bClearIndex) {
					docIndex.deleteMe();
					docIndex = IndexManager.createIndex(sGroupIndex, DocumentPojoIndexMap.documentType_, false, null, docMapping, localSettingsGroupIndex);
				}
				if (null != docIndex) {
					try {
						docIndex.pingIndex(); // (wait until it's created itself)
					}
					catch (Exception e) {} // (just make sure this doesn't die horribly)
				}
				else {
					docIndex = IndexManager.getIndex(sGroupIndex);
				}
				if (null != docIndex) { // should always be true
					docIndex.createAlias(sAliasIndex);
					docIndex.closeIndex();
				}
			}
			else if (!bParentsOnly) { // A sub-index of a parent 			
				String sParentGroupIndex = new StringBuffer("doc_").append(new ObjectId(parentCommunityIdStr).toString()).toString();
				ElasticSearchManager docIndex = IndexManager.getIndex(sParentGroupIndex);
				
				//DEBUG (alias corruption)
//				if (null == _aliasInfo) {
//					ClusterStateResponse clusterState = docIndex.getRawClient().admin().cluster().state(new ClusterStateRequest()).actionGet();
//					_aliasInfo = clusterState.getState().getMetaData().getAliases();
//				}
//				else {
//					if (_aliasInfo.containsKey(sGroupIndex)) { // has no aliases, we're not good
//						return;
//					}
//					else {
//						//DEBUG
//						System.out.println("Alias " + sGroupIndex + " has no aliases (but should)");						
//						ElasticSearchManager docIndex2 = IndexManager.getIndex(sGroupIndex);
//						docIndex2.deleteMe();
//					}
//				}
				
				docIndex.createAlias(sGroupIndex); // for indexing 
					// (this is going to be tricky when the functionality is fully implemented
					//  because it will need to handle the parent index splitting)
				docIndex.createAlias(sAliasIndex); // for queries
				docIndex.closeIndex();
				// (do nothing on delete - that will be handled at the parent index level)
			}
			//TESTED (parents, children, and personal + docs_ aliases)
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
			dummyGroupIndex.createAlias(sGroupIndex); // (at some point we should delete the sGroupIndex alias, but leave it in for bw compatibility for now)
			dummyGroupIndex.createAlias(sAliasIndex); // (never index dummy indices so only need query index)
			// (do nothing on delete since don't have any docs in here anyway)
		}
	}
	//TESTED (including new docs_ alias)

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
		
		String sGroupIndex = null; // for indexing, ie always a single index
		String sAliasIndex = null; // for querying, ie will point to doc_commid, doc_commid_1, etc
		try {
			sGroupIndex = new StringBuffer("doc_").append(new ObjectId(nameOrCommunityIdStr).toString()).toString();
			sAliasIndex = new StringBuffer("docs_").append(new ObjectId(nameOrCommunityIdStr).toString()).toString();
		}
		catch (Exception e) {
			sGroupIndex = nameOrCommunityIdStr;
			if (DocumentPojoIndexMap.globalDocumentIndex_.equals(nameOrCommunityIdStr)) { 
				sAliasIndex = DocumentPojoIndexMap.globalDocumentIndexCollection_;
			}
			else if (DocumentPojoIndexMap.manyGeoDocumentIndex_.equals(nameOrCommunityIdStr)) {
				sAliasIndex = DocumentPojoIndexMap.manyGeoDocumentIndexCollection_;				
			}
			else { // fallback
				sAliasIndex = nameOrCommunityIdStr.replaceAll("doc(?:ument)?_", "docs_");
			}
			//TESTED
		}		
		if (bPersonalGroup) {
			ElasticSearchManager dummyGroupIndex = IndexManager.getIndex(DocumentPojoIndexMap.dummyDocumentIndex_);
			dummyGroupIndex.removeAlias(sAliasIndex);
			dummyGroupIndex.removeAlias(sGroupIndex);
		}
		else if ((null != parentCommunityId) && (!parentCommunityId.equals(new ObjectId("4c927585d591d31d7b37097a")))) {
			// (system community is hardwired)
			
			String sParentGroupIndex = new StringBuffer("doc_").append(parentCommunityId.toString()).toString();
			ElasticSearchManager docIndex = IndexManager.getIndex(sParentGroupIndex);
			docIndex.removeAlias(sGroupIndex);
			docIndex.removeAlias(sAliasIndex);
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
	// Interface to handle scaleable indexes
	// Currently this is a dummy interface, but it will make it easy to split the indexes in the future

	private static HashMap<String, String> _docIndexMap = null;
	private static String _assocIndex = null;
	private static String _entityIndex = null;
	private static ImmutableMap<String, ImmutableMap<String, AliasMetaData>> _aliasInfo = null;
		
	//TODO (INF-1136): Test and integrate this (phase 1), then implement the index splitting code (phase 2)
	
	public static synchronized String getIndex(String communityIdOrIndexStr) {
		if (communityIdOrIndexStr == EntityFeaturePojoIndexMap.indexName_) { // pointer == intended
			if (null == _entityIndex) {
				_entityIndex = EntityFeaturePojoIndexMap.indexName_;
			}
			return _entityIndex;
		}
		else if (communityIdOrIndexStr == AssociationFeaturePojoIndexMap.indexName_) { // pointer == intended			
			if (null == _assocIndex) {
				_assocIndex = AssociationFeaturePojoIndexMap.indexName_;
			}
			return _assocIndex;
		}
		else { // Documents
			
			if (null == _docIndexMap) {
				_docIndexMap = new HashMap<String, String>();
			}
			String sAliasIndex;
			try {
				sAliasIndex = new StringBuffer("doc_").append(new ObjectId(communityIdOrIndexStr).toString()).toString();
			}			
			catch (Exception e) {
				if (DocumentPojoIndexMap.globalDocumentIndex_.equals(communityIdOrIndexStr)) { 
					communityIdOrIndexStr = sAliasIndex = DocumentPojoIndexMap.globalDocumentIndexCollection_;
				}
				else if (DocumentPojoIndexMap.manyGeoDocumentIndex_.equals(communityIdOrIndexStr)) {
					communityIdOrIndexStr = sAliasIndex = DocumentPojoIndexMap.manyGeoDocumentIndexCollection_;				
				}
				else { // fallback
					communityIdOrIndexStr = sAliasIndex = communityIdOrIndexStr.replaceAll("doc(?:ument)?_", "");
				}				
			}
			String sDocIndex = _docIndexMap.get(communityIdOrIndexStr);
			if (null == sDocIndex) {
				sDocIndex = sAliasIndex;
				_docIndexMap.put(communityIdOrIndexStr, sAliasIndex);
			}
			return sDocIndex;
		}
	}
	//TOTEST (lots of cases)
	
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
