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
package com.ikanow.infinit.e.processing.generic.store_and_index;


import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import org.elasticsearch.index.query.QueryBuilders;

import com.google.gson.reflect.TypeToken;
import com.ikanow.infinit.e.data_model.index.ElasticSearchManager;
import com.ikanow.infinit.e.data_model.index.IndexManager;
import com.ikanow.infinit.e.data_model.index.document.DocumentPojoIndexMap;
import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.MongoDbManager;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.ikanow.infinit.e.data_model.store.document.CompressedFullTextPojo;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.infinit.e.data_model.utils.PropertiesManager;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

/**
 * Class used to commit records to backend storage during harvest process
 * @author cmorgan
 *
 */
public class StoreAndIndexManager {

	// Initialize the Logger
	private static final Logger logger = Logger.getLogger(StoreAndIndexManager.class);	
	
	private static boolean _diagnosticMode = false;
	public static void setDiagnosticMode(boolean bMode) { _diagnosticMode = bMode; }
	
	private int nMaxContentLen_bytes = 100000; // (100KB default max)
	private boolean bStoreRawContent = false; // (store the raw as well as the processed data)
	private boolean bStoreMetadataAsContent = false; // (store the metadata in the content block)
	
	private String harvesterUUID = null;
	public String getUUID() { return harvesterUUID; }
	
	public StoreAndIndexManager() {
		com.ikanow.infinit.e.processing.generic.utils.PropertiesManager pm = 
			new com.ikanow.infinit.e.processing.generic.utils.PropertiesManager();
		
		int nMaxContent = pm.getMaxContentSize();
		if (nMaxContent > -1) {
			nMaxContentLen_bytes = nMaxContent;
		}		
		bStoreRawContent = pm.storeRawContent();
		bStoreMetadataAsContent = pm.storeMetadataAsContent();
		
		try {
			StringBuffer sb = new StringBuffer("?DEL?").append(java.net.InetAddress.getLocalHost().getHostName());
			harvesterUUID = sb.toString();
		} catch (UnknownHostException e) {
			harvesterUUID = "?DEL?UNKNOWN";
		}
	}
	
/////////////////////////////////////////////////////////////////////////////////////////////////	
/////////////////////////////////////////////////////////////////////////////////////////////////	
	
// Datastore addition		
	
	/**
	 * Add a list of doc documents to the data store
	 * @param feeds
	 */
	public void addToDatastore(List<DocumentPojo> docs, boolean bSaveContent, SourcePojo source) {
		try {
			// Create collection manager
			// Add to data store
			addToDatastore(DbManager.getDocument().getMetadata(), docs);
		} catch (Exception e) {
			// If an exception occurs log the error
			logger.error("Exception Message: " + e.getMessage(), e);
		}
		// (note: currently modifies docs, see DocumentIndexPojoMap, so beware if using after this point)
		if (bSaveContent) {
			saveContent(docs);
		}
		boolean index = true;
		if ((null != source) && (null != source.getSearchIndexFilter())) {
			if (null != source.getSearchIndexFilter().indexOnIngest) {
				index = source.getSearchIndexFilter().indexOnIngest;
			}
		}
		if (index) {
			this.addToSearch(docs);
		}
		
	}//TESTED
	
	/////////////////////////////////////////////////////////////////////////////////////////////////
	
	// Utilities
	
	/**
	 * Add a single doc document to the datastore
	 * @param col
	 * @param doc
	 */
	private void addToDatastore(DBCollection col, DocumentPojo doc) {
		if (!_diagnosticMode) {
			if (!docHasExternalContent(doc.getUrl(), doc.getSourceUrl())) {
				doc.makeFullTextNonTransient(); // (ie store full text in this case)
			}
			col.save(doc.toDb());
		}
		else {
			System.out.println("StoreAndIndexManager.addToDatastore: " + ((BasicDBObject)doc.toDb()).toString());
		}
	}//TESTED
	
	/**
	 * Add a list of doc documents to the data store
	 * @param feeds
	 */
	private void addToDatastore(DBCollection col, List<DocumentPojo> docs) {
		// Store the knowledge in the feeds collection in the harvester db			
		for ( DocumentPojo f : docs) {
			
			// Set an _id before writing it to the datastore,
			// so the same _id gets written to the index
			// NOTE WE OVERWRITE ANY TRANSIENT IDS THAT MIGHT HAVE BEEN SET eg BY REMOVE CODE
			f.setId(new ObjectId());
			
			// Check geo-size: need to add to a different index if so, for memory usage reasons
			if (DocumentPojoIndexMap.hasManyGeos(f)) {
				f.setIndex(DocumentPojoIndexMap.manyGeoDocumentIndex_);
				// (note this check isn't stateless, it actually populates "locs" at the same time)
				// therefore...
			}
			Set<String> locs = f.getLocs();
			f.setLocs(null);
			
			addToDatastore(col, f);
			
			f.setLocs(locs);
		}
	}//TESTED

	//////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Save the fulltext of a pojo to mongo for using later
	 * 
	 * @param docs
	 */
	private void saveContent(List<DocumentPojo> docs)
	{
		try
		{
			DBCollection contentDb = DbManager.getDocument().getContent();
			
			for ( DocumentPojo doc : docs )
			{
				//TODO: (INF-1367) Currently simplistic logic for deciding whether to store external content 
				boolean bStoreContent = true;
				bStoreContent &= (0 != nMaxContentLen_bytes); // (otherwise it's turned off)
				bStoreContent &= this.bStoreMetadataAsContent || ((null != doc.getFullText()) && !doc.getFullText().isEmpty());				
				boolean bDocHasExternalContent = docHasExternalContent(doc.getUrl(), doc.getSourceUrl());
				
				if (bStoreContent && bDocHasExternalContent) {
					try
					{
						String rawText = this.bStoreRawContent ? doc.getRawFullText() : null; 
						DocumentPojo meta = bStoreMetadataAsContent ? doc : null; 
						CompressedFullTextPojo gzippedContent = new CompressedFullTextPojo(doc.getUrl(), doc.getSourceKey(), doc.getCommunityId(),
																							doc.getFullText(), rawText, meta, nMaxContentLen_bytes);
						
						if (null != gzippedContent.getUrl())  {
							// Be efficient and write field-by-field vs using JSON conversion
							BasicDBObject query = new BasicDBObject(CompressedFullTextPojo.url_, gzippedContent.getUrl());
							query.put(CompressedFullTextPojo.sourceKey_, gzippedContent.getSourceKey());
							BasicDBObject update = gzippedContent.getUpdate();
							if (!_diagnosticMode) {
								contentDb.update(query, update, true, false); // (ie upsert, supported because query includes shard key==url)
							}
							else {
								System.out.println("StoreAndIndexManager.savedContent, save content: " + gzippedContent.getUrl());
							}
						}
					}
					catch (Exception ex)
					{
						// Do nothing, just carry on
						ex.printStackTrace();
					}
				}//TESTED
			}
		}
		catch (Exception ex)
		{
			// This is a more serious error
			logger.error(ex.getMessage());
		}
	}//TESTED (not changed since by-eye testing in Beta)
	
	
/////////////////////////////////////////////////////////////////////////////////////////////////	
/////////////////////////////////////////////////////////////////////////////////////////////////	
	
// Datastore removal		

	/**
	 * This function removes documents "soft deleted" by this harvester
	 */
	
	public void removeSoftDeletedDocuments()
	{
		BasicDBObject query = new BasicDBObject(DocumentPojo.sourceKey_, harvesterUUID);
		
		if (_diagnosticMode) {
			System.out.println("Soft delete: " + DbManager.getDocument().getMetadata().count(query));			
		}
		else {
			DbManager.getDocument().getMetadata().remove(query);			
		}
	}//TESTED
	
	/**
	 * Remove a list of doc documents from the data store (you have their id)
	 * 
	 * CALLED FROM:	resizeDB()
	 */
	public void removeFromDatastore_byId(List<DocumentPojo> docs, boolean bDeleteContent) {
		try {
			// Add to data store
			removeFromDatastore_byId(DbManager.getDocument().getMetadata(), docs, bDeleteContent);
			
			// IMPORTANT: removeFromDataStore(), above, adds "url" and "index" to the doc
			// (not created since can't be used for update...)
			this.removeFromSearch(docs);
			
		} catch (Exception e) {
			// If an exception occurs log the error
			logger.error("Exception Message: " + e.getMessage(), e);
		}
	}//TESTED
	
	/**
	 * Remove a list of doc documents from the data store (you have their url) AND ALSO the search index
	 * 
	 * @param docs - child function needs url (optionally sourceUrl) set - child function requires sourceKey
	 * 					this function needs id and index both of which are set by the child stack
	 * 
	 * CALLED FROM: MongoDocumentTxfer.doDelete(...) <- SETS URL, SOURCE URL, SOURCE KEY, COMMUNITY ID
	 * 				processDocuments(...) [ always called after harvester, ie docs are fully populated ]
	 * 				pruneSource(source, ...) <- SETS URL, SOURCE URL, SOURCE KEY, INDEX
	 * 					updateHarvestStatus(...)
	 */
	public void removeFromDatastore_byURL(List<DocumentPojo> docs, boolean bDeleteContent) {
		
		// Remove from data store:
		
		try {
			removeFromDatastore_byURL(DbManager.getDocument().getMetadata(), docs, bDeleteContent);			
				// ^^^ adds "_id" and "created" to the doc and expands "sourceUrl" docs
			
		} catch (Exception e) {
			// If an exception occurs log the error
			logger.error("Exception Message: " + e.getMessage(), e);
		}
		
		// Remove from index:
		
		try {
			this.removeFromSearch(docs);
			
		} catch (Exception e) {
			// If an exception occurs log the error
			logger.error("Exception Message: " + e.getMessage(), e);
		}
	}//TESTED
	
	/**
	 * Remove a list of doc documents from the data store (you have a source key, so in most cases you can go much quicker)
	 */
	public void removeFromDatastore_bySourceKey(List<DocumentPojo> docs, String sourceKey, boolean bDeleteContent) {
		try {
			if (bDeleteContent) {
				// Worth quickly checking if all of these docs have no external content (eg XML), will be *much* faster...
				boolean bNoDocsHaveExternalContent = true;
				for (DocumentPojo doc: docs) {
					if (docHasExternalContent(doc.getUrl(), doc.getSourceUrl())) {
						bNoDocsHaveExternalContent = false;
						break;
					}
				}//TESTED			
				if (!bNoDocsHaveExternalContent) {
					// OK quick check ... are there any legacy docs left?
					DBObject quickCheck = DbManager.getDocument().getContent().findOne(new BasicDBObject(CompressedFullTextPojo.sourceKey_, null));
					if (null != quickCheck) {		
						
						// Still need to go slow in the DB unfortunately because of the content
						removeFromDatastore_byURL(DbManager.getDocument().getMetadata(), docs, bDeleteContent);
					}
					else {
						DbManager.getDocument().getContent().remove(new BasicDBObject(CompressedFullTextPojo.sourceKey_, sourceKey));
						bDeleteContent = false; // ie drop to clause below
					}
					//TESTED (all sourceKeys set => moves to more efficient delete)
				}
				else {
					bDeleteContent = false; // ie drop to clause below
				}
			}
			
			if (!bDeleteContent) { // databases and the like, can do a really quick delete

				BasicDBObject query = new BasicDBObject(DocumentPojo.sourceKey_, sourceKey);
				BasicDBObject softDelete = new BasicDBObject(DbManager.set_,
												new BasicDBObject(DocumentPojo.sourceKey_, harvesterUUID));
				DbManager.getDocument().getMetadata().update(query, softDelete, false, true);
			}
			
			// Quick delete for index though:
			ElasticSearchManager indexManager = IndexManager.getIndex(new StringBuffer("_all").append('/').append(DocumentPojoIndexMap.documentType_).toString());
			indexManager.doDeleteByQuery(QueryBuilders.termQuery(DocumentPojo.sourceKey_, sourceKey));
			
		} catch (Exception e) {
			// If an exception occurs log the error
			logger.error("Exception Message: " + e.getMessage(), e);
		}
	}//TESTED (all above clauses, including "no external content", "external content", and "external content but none found (eg XML)"
	
	/////////////////////////////////////////////////////////////////////////////////////////////////
	
	// Utility
	
	/**
	 * Remove a list of doc documents from the data store + adds _id and index and created to the docs
	 * @param docs - needs url (optionally sourceUrl) set - child function requires sourceKey
	 * @param col
	 * @param feeds
	 * 
	 * CALLED FROM: removeFromDatastore_bySourceKey(List<doc>, sourceKey, bDeleteContent)  
	 * 					deleteSource(...) <- ENSURES DOC HAS URL, SOURCEURL, SOURCEKEY, INDEX
	 * 				removeFromDataStore_byURL(List<doc>, bDeleteContent) [REQUIRES index be set]
	 * 					MongoDocumentTxfer.doDelete(...)  <- SETS URL, SOURCE URL, SOURCE KEY, COMMUNITY ID
	 * 					processDocuments(...) [ always called after harvester, ie docs are fully populated ]
	 * 					pruneSource(source, ...) <- SETS URL, SOURCE URL, SOURCE KEY, INDEX
	 * 						updateHarvestStatus(...)
	 */
	private void removeFromDatastore_byURL(DBCollection col, List<DocumentPojo> docs, boolean bDeleteContent) {
		BasicDBObject fields = new BasicDBObject();
		fields.put(DocumentPojo.created_, 1); //TODO (INF-1367) explain why this is needed
		fields.put(DocumentPojo.index_, 1); // This is needed for the calling removeFromDataStore_byURL
		
		TreeMap<String,String> sourceUrlToKeyMap = null;
		// Store the knowledge in the feeds collection in the harvester db
		Iterator<DocumentPojo> docIt = docs.iterator();
		while (docIt.hasNext()) {
			DocumentPojo f = docIt.next();
			
			if ((null != f.getSourceUrl()) && (null == f.getUrl())) { // special case ... delete all these documents...
				if (null == sourceUrlToKeyMap) {
					sourceUrlToKeyMap = new TreeMap<String,String>();
				}
				sourceUrlToKeyMap.put(f.getSourceUrl(), f.getSourceKey());				
				docIt.remove(); // (will get expanded into actual documents below)
			}
			else {
				removeFromDatastore_byURL(col, f, fields, bDeleteContent);
			}
		}
		// Now we've deleted all the standard feeds, delete other sourceUrl feeds   
		if (null != sourceUrlToKeyMap) for (Map.Entry<String, String> entry: sourceUrlToKeyMap.entrySet()) {
			String srcUrl = entry.getKey();
			String srcKey = entry.getValue();
			try {
				DBCursor dbc = col.find(new BasicDBObject(DocumentPojo.sourceUrl_, srcUrl), new BasicDBObject(DocumentPojo.url_, 1));
				Iterator<DBObject> dbcIt = dbc.iterator();
				while (dbcIt.hasNext()) {
					BasicDBObject dbo = (BasicDBObject) dbcIt.next();
					DocumentPojo toDel = new DocumentPojo();
					toDel.setUrl(dbo.getString(DocumentPojo.url_));
					toDel.setId((ObjectId) dbo.get(DocumentPojo._id_));
					toDel.setSourceKey(srcKey);
					toDel.setSourceUrl(srcUrl);
					docs.add(toDel); 
						// (this "add" is for removing things from the index, and also to keep count)
					
					// Actually: Delete the doc:
					removeFromDatastore_byURL(col, toDel, fields, bDeleteContent);
					
					//NOTE: I think we could be significantly more efficient here by just doing an "update" to soft delete
					// the docs, then using the sourceUrl to remove-by-query from the index ... only downside would be
					// losing count of deleted docs, you can probably just do a count() efficiently since sourceUrl is indexed?
				}
			} catch (Exception e) {
				// If an exception occurs log the error
				logger.error("Exception Message: " + e.getMessage(), e);
			}
		}
	}//TESTED
	
	/**
	 * Remove a doc from the data store
	 * @param col
	 * @param doc
	 * @param fields - fields to retrieve
	 * 
	 * CALLED FROM:	removeFromDataStore_byId(List<doc>, bDeleteContent)
	 * 					resizeDB()
	 *  THIS FN SETS URL, INDEX, SOURCEKEY AS FIELDS 
	 */
	private void removeFromDatastore_byId(DBCollection col, List<DocumentPojo> docs, boolean bDeleteContent) {
		BasicDBObject fields = new BasicDBObject();
		fields.put(DocumentPojo.url_, 1);
		fields.put(DocumentPojo.index_, 1);
		fields.put(DocumentPojo.sourceKey_, 1); // (set these here for performance)
		
		// Store the knowledge in the feeds collection in the harvester db			
		for ( DocumentPojo f : docs) {
			removeFromDatastore_byId(col, f, fields, bDeleteContent);
		}
	}//TESTED
	
	/**
	 * Remove a doc from the data store
	 * @param col
	 * @param doc - assumes _id set
	 * @param fields - fields to retrieve (set in outside the doc loop for performance, url, index, sourceKey)
	 * 
	 * CALLED FROM:	removeFromDataStore_byId(col, List<doc>, bDeleteContent) <- SETS URL,INDEX,SOURCEKEY AS FIELDS
	 * 					removeFromDataStore_byId(List<doc>, bDeleteContent)
	 * 						resizeDB()
	 */
	private void removeFromDatastore_byId(DBCollection col, DocumentPojo doc, BasicDBObject fields, boolean bDeleteContent) {
		// Update Mongodb with the data
		BasicDBObject query = new BasicDBObject();
		query.put(DocumentPojo._id_, doc.getId());
		BasicDBObject deadDoc = null;
		if (!_diagnosticMode) {
			BasicDBObject softDelete = new BasicDBObject(DbManager.set_,
										new BasicDBObject(DocumentPojo.sourceKey_, harvesterUUID));
			deadDoc = (BasicDBObject) col.findAndModify(query, fields, null, false, softDelete, false, false);
				// (can do this on sharded collections because it uses _id, the shard key)
		}
		else { // (diagnostic mode)
			deadDoc = (BasicDBObject) col.findOne(query, fields);			
			if (null != deadDoc) {
				System.out.println("StoreAndIndexManager.removeFromDatastore_byId, delete: " + deadDoc.toString());
			}
			else {
				System.out.println("StoreAndIndexManager.removeFromDatastore_byId, delete: DOC NOT FOUND");				
			}
		}
		if (null != deadDoc) {
			doc.setUrl(deadDoc.getString(DocumentPojo.url_));
			doc.setIndex(deadDoc.getString(DocumentPojo.index_));
			
			if ((null != doc.getUrl()) && bDeleteContent) { // Use URL to determine whether to delete content
				bDeleteContent =  docHasExternalContent(doc.getUrl(), null);
			}
		}
		if (bDeleteContent) {
			doc.setSourceKey(deadDoc.getString(DocumentPojo.sourceKey_));
			
			// Remove its content also:
			if (!_diagnosticMode) {
				BasicDBObject contentQuery = new BasicDBObject(DocumentPojo.url_, doc.getUrl());
				query.put(MongoDbManager.in_, Arrays.asList(null, doc.getSourceKey())); // (handles legacy case)
				DbManager.getDocument().getContent().remove(contentQuery);
			}
			else {
				System.out.println("StoreAndIndexManager.removeFromDatastore_byId, delete content: " + doc.getUrl() + "/" + doc.getSourceKey());
			}
		}
	}//TESTED
	
	/**
	 * Remove a doc from the data store
	 * @param col
	 * @param doc - needs  url, sourceKey set
	 * @param fields - fields to retrieve (index, created), set in calling function outside of loop for performance
	 * 
	 * CALLED FROM: removeFromDatastore_byURL(col, List<doc>, bDeleteContent)
	 * 					removeFromDatastore_bySourceKey(List<doc>, sourceKey, bDeleteContent)
	 * 						deleteSource(...) <- ENSURES DOC HAS URL, SOURCEURL, SOURCEKEY, INDEX
	 * 					removeFromDataStore_byURL(List<doc>, bDeleteContent) <- SETS INDEX, CREATED (ALSO SRC KEY FOR SRC URL CASES)
	 * 						MongoDocumentTxfer.doDelete(...)  <- SETS URL, SOURCE URL, SOURCE KEY, COMMUNITY ID
	 * 						processDocuments(...) [ always called after harvester, ie docs are fully populated ]
	 * 						pruneSource(source, ...) <- SETS URL, SOURCE URL, SOURCE KEY, INDEX
	 * 							updateHarvestStatus(...)
	 */
	private void removeFromDatastore_byURL(DBCollection col, DocumentPojo doc, BasicDBObject fields, boolean bDeleteContent) {
		// Update Mongodb with the data
		BasicDBObject query = new BasicDBObject();
		query.put(DocumentPojo.url_, doc.getUrl());
		query.put(DocumentPojo.sourceKey_, new BasicDBObject(MongoDbManager.in_, Arrays.asList(null, doc.getSourceKey())));

		// (Remove its content also:)
		if (bDeleteContent) {
			if (docHasExternalContent(doc.getUrl(), null)) {
				if (!_diagnosticMode) {
					DbManager.getDocument().getContent().remove(query);
				}
				else {
					System.out.println("StoreAndIndexManager.removeFromDatastore_byUrl(2), delete content: " + doc.getUrl());
				}
			}
		}
		//TESTED
		
		// Now append source key (not present in content DB):
		BasicDBObject deadDoc = null;
		if (!_diagnosticMode) {
			BasicDBObject softDelete = new BasicDBObject(DbManager.set_,
										new BasicDBObject(DocumentPojo.sourceKey_, harvesterUUID));
			
			col.update(query, softDelete, false, true); // (needs to be multi- even though there's a single element for sharding reasons)
			
			// Cases in which we don't need to go get extra information:
			// (index and _id needed for normal removal from ES, created needed for update cases) 
			// 1) if doc.getId==null then this comes from the update code in the main harvester, so need created
			// 2) obv if doc.index==null then we need to fill this in
			
			if ((null == doc.getIndex()) || (null == doc.getId())) {
				// null==doc.getId() means this is an updateId case
				// (these docs are dummy documents, they already have the _id for deleting from index, created not needed)
				deadDoc = (BasicDBObject) col.findOne(new BasicDBObject(DocumentPojo.url_, doc.getUrl()), fields);
			}
		}
		else {
			deadDoc = (BasicDBObject) col.findOne(query, fields);
		}
		if (null != deadDoc) {
			doc.setCreated((Date) deadDoc.get(DocumentPojo.created_));
			doc.setId((ObjectId) deadDoc.get(DocumentPojo._id_));
			doc.setIndex((String) deadDoc.get(DocumentPojo.index_));
			
			if (_diagnosticMode) {
				System.out.println("StoreAndIndexManager.removeFromDatastore_byUrl(2): found " + deadDoc.toString());
			}
		}
		else if (_diagnosticMode) {
			System.out.println("StoreAndIndexManager.removeFromDatastore_byUrl(2): didn't find " + query.toString());
		}
	}//TESTED
	
/////////////////////////////////////////////////////////////////////////////////////////////////	
/////////////////////////////////////////////////////////////////////////////////////////////////	

// Synchronize database with index	
	
	/**
	 * Add a list of feeds to the full text index
	 * @param docs
	 */
	public void addToSearch(List<DocumentPojo> docs) 
	{		
		String sSavedIndex = null;
		ElasticSearchManager indexManager = null;
		LinkedList<DocumentPojo> tmpDocs = new LinkedList<DocumentPojo>();
		int nTmpDocs = 0;
		for ( DocumentPojo doc : docs )
		{			
			String sThisDocIndex = doc.getIndex();
			
			if ((null == sSavedIndex) || (null == sThisDocIndex) || !sSavedIndex.equals(sThisDocIndex)) { // Change index
				
				if (null != indexManager) { // ie not first time through, bulk add what docs we have
					sendToIndex(indexManager, tmpDocs);
						// (ie with the *old* index manager)
					nTmpDocs = 0;
				}
				sSavedIndex = sThisDocIndex;
				if ((null == sSavedIndex) || (sSavedIndex.equals(DocumentPojoIndexMap.globalDocumentIndex_))) {
					indexManager = IndexManager.getIndex(DocumentPojoIndexMap.globalDocumentIndex_);
				}
				else {
					indexManager = IndexManager.getIndex(new StringBuffer(sSavedIndex).append('/').
																append(DocumentPojoIndexMap.documentType_).toString());					
				}
			}//TESTED		
			
			tmpDocs.add(doc);
			nTmpDocs++;
			
			if (nTmpDocs > 5000) { // some sensible upper limit
				sendToIndex(indexManager, tmpDocs);
				nTmpDocs = 0;
			}
			
			if (_diagnosticMode) {
				System.out.println("StoreAndIndexManager.addToSearch, add: " + doc.getId() + " + " +
						((null != doc.getAssociations())?("" + doc.getAssociations().size()):"0") + " events");
			}
		}// (end loop over docs)
		
		// Bulk add remaining docs		
		sendToIndex(indexManager, tmpDocs);
			
	}//TESTED (not change since by-eye testing in Beta)
	
	// Utility required by the above function
	
	private void sendToIndex(ElasticSearchManager indexManager, LinkedList<DocumentPojo> docsToAdd) {
		try {
			if (!docsToAdd.isEmpty()) {
				//TODO if index list is completely empty then don't actually add...
				
				
				if (!_diagnosticMode) {
					indexManager.bulkAddDocuments(IndexManager.mapListToIndex(docsToAdd, new TypeToken<LinkedList<DocumentPojo>>(){}, 
							new DocumentPojoIndexMap()), DocumentPojo._id_, null, true);
				}
				else {
					System.out.println("StoreAndIndexManager.addToSearch: index " + docsToAdd.size() + " documents to " + indexManager.getIndexName());
				}							
				docsToAdd.clear();				
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			logger.error("Exception Message saving document to ES: " + ex.getMessage(), ex);
		}
	}//TESTED
	
	/**
	 * 
	 * @param docs (just need the id and the index and any events)
	 */

	public void removeFromSearch(List<DocumentPojo> docs) 
	{
		String sIndex = null;
		ElasticSearchManager indexManager = null;
		LinkedList<String> tmpDocs = new LinkedList<String>();
		int nTmpDocs = 0;
		for ( DocumentPojo doc : docs )
		{	
			if (null == doc.getId()) { // Normally this will be sourceUrls, eg files pointing to many docs 
				continue; // (ie can just ignore)
			}
			if ((null == sIndex) || (null == doc.getIndex()) || !sIndex.equals(doc.getIndex())) { // Change index
				
				if (null != indexManager) { // ie not first time through, bulk delete what docs we have
					deleteFromIndex(indexManager, tmpDocs);
						// (ie with the *old* index manager)
					nTmpDocs = 0;
				}
				sIndex = doc.getIndex();
				if ((null == sIndex) || (sIndex.equals(DocumentPojoIndexMap.globalDocumentIndex_))) {
					indexManager = IndexManager.getIndex(DocumentPojoIndexMap.globalDocumentIndex_);
				}
				else {
					indexManager = IndexManager.getIndex(new StringBuffer(sIndex).append('/').append(DocumentPojoIndexMap.documentType_).toString());					
				}
			}//TESTED		
			
			tmpDocs.add(doc.getId().toString());
			nTmpDocs++;
			
			if (nTmpDocs > 5000) { // some sensible upper limit
				deleteFromIndex(indexManager, tmpDocs);
				nTmpDocs = 0;
			}
			
			//delete from event search
			if (_diagnosticMode) {
				System.out.println("StoreAndIndexManager.removeFromSearch, remove: " + doc.getId() + " + " +
						((null != doc.getAssociations())?("" + doc.getAssociations().size()):"0") + " events");
			}			
		} // (end loop over docs)
		
		// Bulk remove remaining docs		
		deleteFromIndex(indexManager, tmpDocs);
		
	}//TESTED (not change since by-eye testing in Beta)
	
	/////////////////////////////////////////////////////////////////////////////////////////////////
	
	// Utility required by the above function
	
	private void deleteFromIndex(ElasticSearchManager indexManager, LinkedList<String> docsToDelete) {
		try {
			if (!docsToDelete.isEmpty()) {
				if (!_diagnosticMode) {
					indexManager.bulkDeleteDocuments(docsToDelete);
				}
				else {
					System.out.println("StoreAndIndexManager.removeFromSearch: index " + docsToDelete.size() + " documents from " + indexManager.getIndexName());
				}							
				docsToDelete.clear();				
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			logger.error("Exception Message deleting document from ES: " + ex.getMessage(), ex);
		}
	}//TESTED
	
/////////////////////////////////////////////////////////////////////////////////////////////////	
/////////////////////////////////////////////////////////////////////////////////////////////////	
	
// Handle resizing the DB if it gets too large	

	// Utility function for diagnostic prints etc

	public long getDatabaseSize() {
		return DbManager.getDocument().getMetadata().count();
	}

	/**
	 * This function checks if DB storage requirements are met,
	 * if not it will start removing docs based on least used/oldest
	 * 
	 * @return true once DB is within bounds, false if an error occurs
	 */

	public boolean resizeDB()
	{
		//Do quick check to check if we are already under storage requirements
		if ( checkStorageCapacity() ) {
			return false;
		}
		else
		{
			//if quick check fails, start removing docs to get under requirement
			try
			{
				long currDocsInDB = DbManager.getDocument().getMetadata().count();
				long storageCap = new PropertiesManager().getStorageCapacity();

				List<DocumentPojo> docsToRemove = getLeastActiveDocs((int) (currDocsInDB-storageCap));			
				removeFromDatastore_byId(docsToRemove, true); // (remove content since don't know if it exists)
				//(^ this also removes from index)

				return true;
			}
			catch (Exception e)
			{
				// If an exception occurs log the error
				logger.error("Exception Message: " + e.getMessage(), e);
				return true;
			}
		}
	}//TESTED 

	/////////////////////////////////////////////////////////////////////////////////////////////////
	
	// Utility
	
	/**
	 * This method checks if doc count is
	 * below threshhold set in properties
	 * @return true is below threshhold, false if not
	 */
	private boolean checkStorageCapacity()
	{
		long currDocsInDB = 0;
		try {
			currDocsInDB = DbManager.getDocument().getMetadata().count();
		} catch (Exception e ) {
			// If an exception occurs log the error
			logger.error("Exception Message: " + e.getMessage(), e);
		}
		long storageCapacity = new PropertiesManager().getStorageCapacity();
		return (currDocsInDB < storageCapacity); 
	}

	/**
	 * Returns a list of the least active documents
	 * List is of length numDocs
	 * 
	 * @param numDocs Number of documents to return that are least active
	 * @return a list of documents that are least active in DB
	 */
	private List<DocumentPojo> getLeastActiveDocs(int numDocs)
	{
		List<DocumentPojo> olddocs = null;

		//TODO (INF-1301): WRITE AN ALGORITHM TO CALCULATE THIS BASED ON USAGE, just using time last accessed currently
		//give a weight to documents age and documents activity to calculate
		//least active (current incarnation doesn't work)
		try
		{
			DBCursor dbc = DbManager.getDocument().getMetadata().find(new BasicDBObject(), new BasicDBObject()).sort(
					new BasicDBObject(DocumentPojo._id_,1)).limit(numDocs);
			// (note, just retrieve _id fields: _id starts with timestamp so these are approximately oldest created)

			olddocs = DocumentPojo.listFromDb(dbc, DocumentPojo.listType());

		}
		catch (Exception e )
		{
			// If an exception occurs log the error
			logger.error("Exception Message: " + e.getMessage(), e);
		}
		return olddocs;
	}//TOTEST (functionality not currently used)			

////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////	

// Utility

		// Utility function to decide if we need to add/remove content via the external table 
		// (ie JDBC and XML have their content as part of their metadata, eg fields
		//  others like HTTP and Files can have large amounts of content that we don't want to store in the DB object)

		// Called from: (noted here because this needs to be tidied up at some point)
		// StoreAndIndexManager.addToDatastore
		// MongoDocumentTxfer.doTransfer
		// SourceUtils.pruneSource
		// StoreAndIndexManager.removeFromDataStore_by(Id|SourceKey|Url)
		// StoreAndIndexManager.saveContent
	
		static public boolean docHasExternalContent(String url, String srcUrl) {
			//TODO: INF-1367: there's an issue with this .. suppose it's some enormous JSON file
			// and we excise a bunch of JSON files from the metadata (after using them for processing)
			// seems like we should have an optional keepExternalContent that defaults to the return value 
			// of this function, but you can override from the SAH or whatever
			
			if (null != srcUrl) { // must be either JSON or XML or *sv
				return false;
			}
			else if (null == url) { // no idea, pathological case?!
				return true;
			}
			else if (url.startsWith("jdbc:")) { // DB entry
				return false;
			}
			else if (url.startsWith("inf://custom/")) { // custom entry
				return false;
			}
			else if ((url.startsWith("smb://") || url.startsWith("file:") || url.startsWith("s3://") || url.startsWith("inf://")) && 
									(url.endsWith(".xml") || url.endsWith(".json") || url.endsWith("sv")))
				// JSON/XML/*sv but 1 doc/file 				
			{
				return false;
			}
			return true;
		}	

}
