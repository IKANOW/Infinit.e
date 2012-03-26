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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.GZIPInputStream;

import org.bson.types.ObjectId;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.ImmutableSettings.Builder;

import com.google.gson.Gson;
import com.ikanow.infinit.e.data_model.index.ElasticSearchManager;
import com.ikanow.infinit.e.data_model.index.IndexManager;
import com.ikanow.infinit.e.data_model.index.document.DocumentPojoIndexMap;
import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.infinit.e.processing.generic.GenericProcessingController;
import com.ikanow.infinit.e.processing.generic.store_and_index.StoreAndIndexManager;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.MongoException;

public class MongoDocumentTxfer {

	//___________________________________________________________________________________________________
	
	// MAIN
	
	/**
	 * @param args: 0,1 is the location of the MongoDB host/port, 2/3 is the location of the ES index host/port
	 * @throws MongoException 
	 * @throws NumberFormatException 
	 * @throws IOException 
	 */
	public static void main(String sConfigPath, String sQuery, boolean bDelete, boolean bRebuildIndex, int nSkip, int nLimit) throws NumberFormatException, MongoException, IOException {
		
		// Command line processing
		com.ikanow.infinit.e.data_model.Globals.setIdentity(com.ikanow.infinit.e.data_model.Globals.Identity.IDENTITY_SERVICE);
		if (null != sConfigPath) {
			com.ikanow.infinit.e.data_model.Globals.overrideConfigLocation(sConfigPath);
		}
		boolean bRebuildIndexOnFly = false;
		if (bRebuildIndex && (null == sQuery)) { // (else will do them 1-by-1)
			new GenericProcessingController().InitializeIndex(true, false, false);
		}
		else {
			// Have seen odd transport timeouts on occasion: this should ensure they never happen
			new GenericProcessingController().InitializeIndex(false, false, false);
				// (don't delete anything, but do recalc)
			
			if (bRebuildIndex) {
				bRebuildIndexOnFly = true;
			}
		}
		MongoDocumentTxfer txferManager = new MongoDocumentTxfer(bRebuildIndexOnFly);
		
		BasicDBObject query = null;		
		if (null == sQuery) {
			query = new BasicDBObject();
		}
		else {
			query = (BasicDBObject) com.mongodb.util.JSON.parse(sQuery);
		}
		
		if (!bDelete) {
			txferManager.doTransfer(query, nSkip, nLimit);
		}
		else {
			txferManager.doDelete(query, nLimit);
		}
	}
	public MongoDocumentTxfer(boolean bRebuildIndexOnFly) {
		if (bRebuildIndexOnFly) {
			_deletedIndex = new TreeSet<String>();
		}
	}
	
	//___________________________________________________________________________________________________
	
	// PROCESSING LOOP (new interface)
	
	private Map<String, SourcePojo> _sourceCache = new HashMap<String, SourcePojo>();
	private TreeSet<String> _deletedIndex = null;
	
	private void doTransfer(BasicDBObject query, int nSkip, int nLimit) throws IOException
	{		
		// Initialize the DB:
		
		DBCollection docsDB = DbManager.getDocument().getMetadata();
		DBCollection contentDB = DbManager.getDocument().getContent();
		DBCollection sourcesDB = DbManager.getIngest().getSource();

		ElasticSearchManager.setDefaultClusterName("infinite-aws");

// 1. Get the documents from the DB (combining data + metadata and refreshing source meta)
		
		//Debug:
		DBCursor dbc = null;
		dbc = docsDB.find(query).skip(nSkip).limit(nLimit);
		int nCount = dbc.count() - nSkip;
		if (nCount < 0) nCount = 0;
		System.out.println("Found " + nCount + " records to sync, process first " + (0==nLimit?nCount:nLimit));
		if (0 == nCount) { // Nothing to do...
			return;
		}
		
		byte[] storageArray = new byte[200000];
		
		int nSynced = 0;
		LinkedList<DocumentPojo> docsToTransfer = new LinkedList<DocumentPojo>(); 
		while (dbc.hasNext()) {
			BasicDBObject dbo = (BasicDBObject)dbc.next();
			DocumentPojo doc = DocumentPojo.fromDb(dbo, DocumentPojo.class);
			String sDocIndex = doc.getIndex();
			if (null == sDocIndex) {
				sDocIndex = "document_index";
			}
			if ((null != _deletedIndex) && !_deletedIndex.contains(sDocIndex)) {
				_deletedIndex.add(sDocIndex);
				rebuildIndex(sDocIndex);
				try { // (Just in case the index requires some time to sort itself out)
					Thread.sleep(1000);
				} catch (InterruptedException e) {}
			}
			
			//Debug:
			//System.out.println("Getting content..." + feed.getTitle() + " / " + feed.getUrl());
			
			// Get the content:
			BasicDBObject contentQ = new BasicDBObject("url", doc.getUrl());
			BasicDBObject dboContent = (BasicDBObject) contentDB.findOne(contentQ);
			if (null != dboContent) {
				byte[] compressedData = ((byte[])dboContent.get("gzip_content"));				
				ByteArrayInputStream in = new ByteArrayInputStream(compressedData);
				GZIPInputStream gzip = new GZIPInputStream(in);				
				int nRead = 0;
				StringBuffer output = new StringBuffer();
				while (nRead >= 0) {
					nRead = gzip.read(storageArray, 0, 200000);
					if (nRead > 0) {
						String s = new String(storageArray, 0, nRead, "UTF-8");
						output.append(s);
					}
				}
				doc.setFullText(output.toString());
			}				
			// Get tags, if necessary:
			// Always overwrite tags - one of the reasons we might choose to migrate
			//if (null == feed.getTags()) 
			{
				SourcePojo src = _sourceCache.get(doc.getSourceKey());
				if (null == src) {
					BasicDBObject srcDbo = (BasicDBObject) sourcesDB.findOne(new BasicDBObject("key", doc.getSourceKey()));
					if (null != srcDbo) {
						src = SourcePojo.fromDb(srcDbo, SourcePojo.class);
						
						_sourceCache.put(doc.getSourceKey(), src);
					}
				}
				if (null != src) {
					if (null != src.getTags()) {
						Set<String> tagsTidied = new TreeSet<String>();
						for (String s: src.getTags()) {
							String ss = s.trim().toLowerCase();
							tagsTidied.add(ss);
						}
						
						// May also want to write this back to the DB:
						if ((null == doc.getTags()) || (doc.getTags().size() != tagsTidied.size())) {
							docsDB.update(new BasicDBObject("_id", doc.getId()), 
									new BasicDBObject("$set", new BasicDBObject("tags", tagsTidied)));
						}
						doc.setTags(tagsTidied);
					}
				}
			}

// 2. Update the index with the new document				
			
			nSynced++;
			docsToTransfer.add(doc);
			if (0 == (nSynced % 10000)) {					
				StoreAndIndexManager manager = new StoreAndIndexManager();
				manager.addToSearch(docsToTransfer);
				docsToTransfer.clear();
				System.out.println("(Synced " + nSynced + " records)");
			}
			
		} // (End loop over docs)
						
		// Sync remaining docs
		
		if (!docsToTransfer.isEmpty()) {
			StoreAndIndexManager manager = new StoreAndIndexManager();
			manager.addToSearch(docsToTransfer);				
		}
	}
	//___________________________________________________________________________________________________
	
	// Utility function for the above, rebuilds an index
	// Code taken from generic processing controller, so need to keep in sync
	
	private void rebuildIndex(String indexName) {
		if (indexName.startsWith("doc_")) { // Else not eligible...
			String communityId = indexName.substring(4); // (Remove "doc_", just left with community ID)
			BasicDBObject dbo = (BasicDBObject) DbManager.getSocial().getCommunity().findOne(new BasicDBObject("_id", new ObjectId(communityId)));
			if (null != dbo) {
				boolean bSystemGroup = dbo.getBoolean("isSystemCommunity", false);
				boolean bPersonalGroup = dbo.getBoolean("isPersonalCommunity", false);
									
				int nShards = bPersonalGroup? 2 : 5 ; // (would like 1 for personal group, but that would then be treated as data local, probably undesirable)
				if (bSystemGroup) { // Biggest
					nShards = 10;
				}
				int nPreferredReplicas = 2;
				
				// Then create an index with this name:
				Builder localSettingsGroupIndex = ImmutableSettings.settingsBuilder();
				localSettingsGroupIndex.put("number_of_shards", nShards).put("number_of_replicas", nPreferredReplicas);	
				String docMapping = new Gson().toJson(new DocumentPojoIndexMap.Mapping(), DocumentPojoIndexMap.Mapping.class);
				
				ElasticSearchManager docIndex = IndexManager.createIndex(indexName, "document_index", false, null, docMapping, localSettingsGroupIndex);

				docIndex.deleteMe();
				docIndex = IndexManager.createIndex(indexName, "document_index", false, null, docMapping, localSettingsGroupIndex);
			}
		}
	}
	
	//___________________________________________________________________________________________________
	
	// DELETE DOCUMENTS FROM A QUERY
	
	private void doDelete(BasicDBObject query, int nLimit)
	{		
		try {
			// Get the documents to delete
			BasicDBObject queryFields = new BasicDBObject("metadata", 0); 			
			DBCursor cur = DbManager.getDocument().getMetadata().find(query, queryFields); // (this internally works in batches of 1000)
			
			System.out.println("Found " + cur.count() + " records to delete");
			
			List<DocumentPojo> docs = DocumentPojo.listFromDb(cur, DocumentPojo.listType());

			// Keep track of number of docs per community getting deleted
			Map<ObjectId, Integer> communityMap = new HashMap<ObjectId, Integer>();
			Map<String, Integer> sourceKeyMap = new HashMap<String, Integer>();
			for (DocumentPojo doc: docs) {
				if (null != doc.getSourceKey()) { // (can only happen by error, still)
					ObjectId community = doc.getCommunityId();
					 Integer count = communityMap.get(community);
					 communityMap.put(community, (count == null ? 1 : count + 1));
					 int nSpecialFormat = doc.getSourceKey().indexOf('#');
					 String sourceKey = doc.getSourceKey();
					 if (nSpecialFormat > 0) {
						 sourceKey = sourceKey.substring(0, nSpecialFormat);
					 }
					 Integer count2 = sourceKeyMap.get(sourceKey);
					 sourceKeyMap.put(sourceKey, (count2 == null ? 1 : count2 + 1));
				}
			}
			
			new StoreAndIndexManager().removeFromDatastore_byURL(docs, true);

			// Actually update the DB counts:
			for (Map.Entry<ObjectId, Integer> communityInfo: communityMap.entrySet()) {
				System.out.println("Removed " + communityInfo.getValue() + " records from community " + communityInfo.getKey());
				DbManager.getDocument().getCounts().update(new BasicDBObject("_id", communityInfo.getKey()), 
						new BasicDBObject("$inc", new BasicDBObject("doccount", -communityInfo.getValue())));
			}
			for (Map.Entry<String, Integer> sourceInfo: sourceKeyMap.entrySet()) {
				System.out.println("Removed " + sourceInfo.getValue() + " records from source " + sourceInfo.getKey());
				DbManager.getIngest().getSource().update(new BasicDBObject("key", sourceInfo.getKey()), 
						new BasicDBObject("$inc", new BasicDBObject("harvest.doccount", -sourceInfo.getValue())));				
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	//___________________________________________________________________________________________________
	//___________________________________________________________________________________________________
	//___________________________________________________________________________________________________
	//___________________________________________________________________________________________________
	
	// UNIT/FUNCTIONAL/COVERAGE TEST CODE
	
	@SuppressWarnings("unused")
	private void doUnitTest(String sMongoDbHost, String sMongoDbPort, String sElasticHost, String sElasticPort, 
			BasicDBObject query, int nLimit)
	{		
		ElasticSearchManager elasticManager = null;
			
		try {
			// Initialize the DB:
			
			DBCollection feedsDB = DbManager.getDocument().getMetadata();
			DBCollection contentDB = DbManager.getDocument().getContent();
			DBCollection sourcesDB = DbManager.getIngest().getSource();

			String indexName = "document_index";
			
			// Test/debug recreate the index
			if (true) {
				
				// (delete the index)
				System.out.println("Deleting index...");
				elasticManager = ElasticSearchManager.getIndex(indexName, sElasticHost + ":" + sElasticPort);
				elasticManager.deleteMe();
				//(also deletes the child index - same index, different type)

				// Create the index if necessary
				String sMapping = new Gson().toJson(new DocumentPojoIndexMap.Mapping(), DocumentPojoIndexMap.Mapping.class);

				Builder localSettings = ImmutableSettings.settingsBuilder();
				localSettings.put("number_of_shards", 10).put("number_of_replicas", 2);			
				
				System.out.println("Creating index..." + sMapping);
				elasticManager = ElasticSearchManager.createIndex
									(indexName, null, false, 
											sElasticHost + ":" + sElasticPort, 
											sMapping, localSettings);
				
			}			
			// Get the index (necessary if already created)
			if (null == elasticManager) {
				elasticManager = ElasticSearchManager.getIndex(indexName, sElasticHost + ":" + sElasticPort);
			}
			
			// Get the feeds from the DB:
			
			//Debug:
//			System.out.println("Querying DB...");
			
			DBCursor dbc = feedsDB.find(query).limit(nLimit);
			
			byte[] storageArray = new byte[200000];
			
			while (dbc.hasNext()) {
				BasicDBObject dbo = (BasicDBObject)dbc.next();
				DocumentPojo doc = DocumentPojo.fromDb(dbo, DocumentPojo.class);
				
				//Debug:
				System.out.println("Getting content..." + doc.getTitle() + " / " + doc.getUrl());
				
				// Get the content:
				BasicDBObject contentQ = new BasicDBObject("url", doc.getUrl());
				BasicDBObject dboContent = (BasicDBObject) contentDB.findOne(contentQ);
				if (null != dboContent) {
					byte[] compressedData = ((byte[])dboContent.get("gzip_content"));				
					ByteArrayInputStream in = new ByteArrayInputStream(compressedData);
					GZIPInputStream gzip = new GZIPInputStream(in);				
					int nRead = gzip.read(storageArray, 0, 200000);
					String s = new String(storageArray, 0, nRead, "UTF-8");
					doc.setFullText(s);
				}				
				// Get tag:
				SourcePojo src = _sourceCache.get(doc.getSourceKey());
				if (null == src) {
					BasicDBObject srcDbo = (BasicDBObject) sourcesDB.findOne(new BasicDBObject("key", doc.getSourceKey()));
					if (null != srcDbo) {
						src = new Gson().fromJson(srcDbo.toString(), SourcePojo.class);
						
						_sourceCache.put(doc.getSourceKey(), src);
					}
				}
				if (null != src) {
					Set<String> tagsTidied = new TreeSet<String>();
					for (String s: src.getTags()) {
						String ss = s.trim().toLowerCase();
						tagsTidied.add(ss);
					}
					doc.setTags(tagsTidied);
				}
				
				//TEST: set dynamic field
				// Lots of testing of dynamic dates:
//				feed.addToMetadata("my_dateISO", Date.parse(feed.getCreated().toGMTString()));
//				String s1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(feed.getCreated());				
//				feed.addToMetadata("another_dateISO", s1);
//				String s1_5 = new SimpleDateFormat().format(feed.getCreated());
//				feed.addToMetadata("another_dateTimeJava", s1_5);
//				String s2 = new SimpleDateFormat("yyyyMMdd").format(feed.getCreated());				
//				feed.addToMetadata("another_dateYYYYMMDD", s2);
//				String s3 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z").format(feed.getCreated());
//				feed.addToMetadata("another_dateRFC822", s3);
//				feed.addToMetadata("another_dateGMT", feed.getCreated().toGMTString());
//				// Testing of the string field types
//				feed.addToMetadata("my_comment", "Testing this ABCDEFG");				
//				feed.addToMetadata("my_term", "Testing this UVWXYZ");
//				feed.addToMetadata("my_text", "Testing this 123456");				
//				// Test an array of longs:
//				Long tl[] = new Long[4]; tl[0] = 0L; tl[1] = 1L; tl[2] = 2L; tl[3] = 3L;
//				feed.addToMetadata("md_long", tl);

				//TEST: some dummy event timestamp adding code (not seeing much/any in the data)
//				if (null != feed.getEvents()) {
//					int i = 0;
//					for (EventPojo evt: feed.getEvents()) {
//						//1: Add single date
//						if (0 == i) {
//							evt.time_start = "2011-01-01";
//						}
//						//2: Add short span
//						if (1 == i) {
//							evt.time_start = "2010-04-06";
//							evt.time_end = "2010-08-09";
//						}
//						//3: Add cross-yr span
//						if (2 == i) {
//							evt.time_start = "2012-06-05";
//							evt.time_end = "2013-09-05";
//						}
//						//4: Add too long span
//						if (3 == i) {
//							evt.time_start = "2012-04-06";
//							evt.time_end = "2014-04-09";
//						}
//						i++;
//					}
//				}
						
				// For event adding, see data_model.test.TestCode
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			//nothing to do
		}
	}
}
