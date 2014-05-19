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
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import org.elasticsearch.index.query.BaseQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import com.google.gson.reflect.TypeToken;
import com.ikanow.infinit.e.data_model.index.ElasticSearchManager;
import com.ikanow.infinit.e.data_model.index.IndexManager;
import com.ikanow.infinit.e.data_model.index.document.DocumentPojoIndexMap;
import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.MongoDbManager;
import com.ikanow.infinit.e.data_model.store.config.source.SourceHarvestStatusPojo;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.ikanow.infinit.e.data_model.store.document.CompressedFullTextPojo;
import com.ikanow.infinit.e.data_model.store.document.DocCountPojo;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.infinit.e.data_model.utils.PropertiesManager;
import com.mongodb.BasicDBObject;
import com.mongodb.CommandResult;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;

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
	
	public final static String DELETION_INDICATOR = "?DEL?";
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
			StringBuffer sb = new StringBuffer(DELETION_INDICATOR).append(java.net.InetAddress.getLocalHost().getHostName());
			harvesterUUID = sb.toString();
		} catch (UnknownHostException e) {
			harvesterUUID = DELETION_INDICATOR + "UNKNOWN";
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
			if (null == f.getLocs()) { // (can be set by update/deletion code also)
				if (DocumentPojoIndexMap.hasManyGeos(f)) {
					f.setIndex(DocumentPojoIndexMap.manyGeoDocumentIndex_);
					// (note this check isn't stateless, it actually populates "locs" at the same time)
					// therefore...
				}
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
		BasicDBObject query = new BasicDBObject(DocumentPojo.url_, harvesterUUID);
		
		if (_diagnosticMode) {
			System.out.println("Soft delete: " + DbManager.getDocument().getMetadata().count(query));			
		}
		else {
			DbManager.getDocument().getMetadata().remove(query);			
		}
	}//TESTED
	
	/**
	 * Low level utility to abstract soft deletion
	 * We're using URL because 1) we cant' use a shard key
	 * 2) it needs to be an indexed field
	 * 3) ideally one that is likely to be cached in memory
	 * 4) one that minimizes the chance of having to move the document when modifying the field
	 * (I also considered sourceUrl or an all new field, they _might_ be better because smaller, but conversely
	 *  would be less likely to be cached and most importantly there's the risk of 4)
	 */
	
	private BasicDBObject _softDeleter = null;
	
	private BasicDBObject getSoftDeleteUpdate()
	{
		if (null == _softDeleter) {
			BasicDBObject softDeleter = new BasicDBObject(DocumentPojo.url_, harvesterUUID);
			softDeleter.put(DocumentPojo.index_, DELETION_INDICATOR);
				// (used in CustomHadoopTaskLauncher.createConfigXML)
			_softDeleter = new BasicDBObject(DbManager.set_, softDeleter);
		}
		return _softDeleter;
	}//TESTED

	/**
	 * Remove a list of doc documents from the data store (you have their _id and sourceKey)
	 * 
	 * CALLED FROM:	resizeDB() <- FILLS IN _ID, SOURCEKEY, INDEX, URL, SOURCEURL
	 */
	public void removeFromDatastore_byId(List<DocumentPojo> docs) {
		try {
			// Remove from data store
			removeFromDatastore_byId(DbManager.getDocument().getMetadata(), docs);			
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
	 * CALLED FROM: MongoDocumentTxfer.doDelete(...) <- SETS URL, SOURCE URL, SOURCE KEY, COMMUNITY ID, INDEX, _ID
	 * 				processDocuments(...) [ always called after harvester: have sourceUrl, sourceKey, 
	 * 										DON'T have _id, BUT do have updateId and index (correct except in many geo cases)]
	 * 				pruneSource(source, ...) <- SETS URL, SOURCE URL, SOURCE KEY, INDEX, _ID
	 * 					updateHarvestStatus(...)
	 */
	public ObjectId removeFromDatastore_byURL(List<DocumentPojo> docs) {
		
		// Remove from data store:
		ObjectId nextId = null;
		try {
			nextId = removeFromDatastore_byURL(DbManager.getDocument().getMetadata(), docs);			
				// ^^^ adds "created" (if updateId set), "_id" and "index" to the doc and expands "sourceUrl" docs (adding "_id" and "index")
			
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
		
		return nextId;
	}//TESTED
	
	/**
	 * Remove a list of doc documents from the data store (you have a source key, so you can go much quicker)
	 * CALLED FROM:	deleteSource(...) 
	 * @returns the number of docs deleted
	 */
	public long removeFromDatastoreAndIndex_bySourceKey(String sourceKey, ObjectId lessThanId, boolean definitelyNoContent, String communityId) {
				
		try {			
			if (!definitelyNoContent) {
				DbManager.getDocument().getContent().remove(new BasicDBObject(CompressedFullTextPojo.sourceKey_, sourceKey));
					// (will just check index and pull out if the doc has no external content)
			}
			BasicDBObject query = new BasicDBObject(DocumentPojo.sourceKey_, sourceKey);
			if (null != lessThanId) { // Multiple threads running for this source
				// First check whether one of the other threads has already deleted the source:
				BasicDBObject oneFinalCheckQuery = new BasicDBObject(DocumentPojo.sourceKey_, sourceKey);
				BasicDBObject oneFinalCheckFields = new BasicDBObject(DocumentPojo.index_, 1);
				BasicDBObject firstDocToBeUpdated = (BasicDBObject) DbManager.getDocument().getMetadata().findOne(oneFinalCheckQuery, oneFinalCheckFields);
				if ((null == firstDocToBeUpdated) || firstDocToBeUpdated.getString(DocumentPojo.index_, "").equals(DELETION_INDICATOR))
				{
					//(ie grab the first doc in natural order and tell me if it's been soft-deleted yet, if so do nothing)
					return 0;
				}//TESTED
				
				// That check isn't perfect because of race conditions, so we'll still add the !="?DEL?" check to the 
				// update as well:				
				query.put(DocumentPojo._id_, new BasicDBObject(DbManager.lte_, lessThanId));
				query.put(DocumentPojo.index_, new BasicDBObject(DbManager.ne_, DELETION_INDICATOR));
			}//TESTED
			
			BasicDBObject softDeleter = getSoftDeleteUpdate();
			DbManager.getDocument().getMetadata().update(query, softDeleter, false, true);
			// (don't do getLastError just yet since it can block waiting for completion)
			
			// Quick delete for index though:
			StringBuffer sb = new StringBuffer(DocumentPojoIndexMap.manyGeoDocumentIndexCollection_).append(",docs_").append(communityId).append('/').append(DocumentPojoIndexMap.documentType_);
			ElasticSearchManager indexManager = IndexManager.getIndex(sb.toString());
			BaseQueryBuilder soloOrCombinedQuery = QueryBuilders.termQuery(DocumentPojo.sourceKey_, sourceKey);
			if (null != lessThanId) {
				//(_id isn't indexed - _uid is and == _type + "#" + _id)
				soloOrCombinedQuery = QueryBuilders.boolQuery().must(soloOrCombinedQuery).
										must(QueryBuilders.rangeQuery("_uid").lte("document_index#" + lessThanId.toString()));
				
			}//TESTED
			indexManager.doDeleteByQuery(soloOrCombinedQuery);						
			
			CommandResult result = DbManager.getDocument().getLastError("metadata");
			return result.getLong("n", 0);
			
		} catch (Exception e) {
			// If an exception occurs log the error
			logger.error("Exception Message: " + e.getMessage(), e);			
		}
		return 0;
	}//TESTED
	
	
	/**
	 * Remove a list of doc documents from the data store and index (you have a source URL, so you can go much quicker)
	 * 
	 * CALLED FROM: removeFromDataStore_byURL(List<doc>, bDeleteContent) [ALSO DELETES FROM INDEX AFTER ADDED FROM HERE]
	 * 					MongoDocumentTxfer.doDelete(...)  <- SETS URL, SOURCE URL, SOURCE KEY, COMMUNITY ID, INDEX, _ID
	 * 					processDocuments(...) [ always called after harvester: have sourceUrl, sourceKey, 
	 * 											DON'T have _id, BUT do have updateId and index (correct except in many geo cases)]
	 * @returns the number of docs deleted
	 */
	
	private ElasticSearchManager _cachedIndexManagerForSourceXxxDeletion = null;
	private ObjectId _cachedCommunityIdForSourceXxxDeletion = null;
	public long removeFromDatastoreAndIndex_bySourceUrl(String sourceUrl, String sourceKey, ObjectId communityId) {
				
		try {			
			// (never any content)
			BasicDBObject query = new BasicDBObject(DocumentPojo.sourceUrl_, sourceUrl);
			query.put(DocumentPojo.sourceKey_, sourceKey);
			BasicDBObject softDeleter = getSoftDeleteUpdate();
			DbManager.getDocument().getMetadata().update(query, softDeleter, false, true);
			CommandResult result = DbManager.getDocument().getLastError("metadata");
			
			// Quick delete for index though:
			if (!communityId.equals(_cachedCommunityIdForSourceXxxDeletion)) {
				StringBuffer sb = new StringBuffer(DocumentPojoIndexMap.manyGeoDocumentIndexCollection_).append(",docs_").append(communityId).append('/').append(DocumentPojoIndexMap.documentType_);
				_cachedIndexManagerForSourceXxxDeletion = IndexManager.getIndex(sb.toString());
				_cachedCommunityIdForSourceXxxDeletion = communityId;
			}//TESTED
			_cachedIndexManagerForSourceXxxDeletion.doDeleteByQuery(
					QueryBuilders.boolQuery()
						.must(QueryBuilders.termQuery(DocumentPojo.sourceUrl_, sourceUrl))
						.must(QueryBuilders.termQuery(DocumentPojo.sourceKey_, sourceKey))
					);
			
			return result.getLong("n", 0);
			
		} catch (Exception e) {
			// If an exception occurs log the error
			logger.error("Exception Message: " + e.getMessage(), e);			
		}
		return 0;
	}//TESTED
	
	
	/////////////////////////////////////////////////////////////////////////////////////////////////
	
	// Utility
	
	/**
	 * Remove a list of doc documents from the data store + adds _id and index doc fields to retrieve to support de-index
	 * (also adds created to docs with an updateId so the created remains ~the same)
	 * (Will in theory support arbitrary sourceUrl/sourceKey operators but in practice these will always be from a single source)
	 * @param docs - needs url (optionally sourceUrl) set - child function requires sourceKey
	 * @param col
	 * @param feeds
	 * 
	 * CALLED FROM: removeFromDataStore_byURL(List<doc>, bDeleteContent) [ALSO DELETES FROM INDEX AFTER ADDED FROM HERE]
	 * 					MongoDocumentTxfer.doDelete(...)  <- SETS URL, SOURCE URL, SOURCE KEY, COMMUNITY ID, INDEX, _ID
	 * 					processDocuments(...) [ always called after harvester: have sourceUrl, sourceKey, 
	 * 											DON'T have _id, BUT do have updateId and index (correct except in many geo cases)]
	 * 					pruneSource(source, ...) <- SETS URL, SOURCE URL, SOURCE KEY, INDEX, _ID
	 * 						updateHarvestStatus(...)
	 */
	private ObjectId removeFromDatastore_byURL(DBCollection col, List<DocumentPojo> docs) {
		ObjectId nextId = null;
		BasicDBObject fields = new BasicDBObject();
		fields.put(DocumentPojo.created_, 1); // (If we're getting the deleted doc fields, get this and have exact created time)
		fields.put(DocumentPojo.index_, 1); // This is needed for the removeFromSearch() called from parent removeFromDatastore_URL
				
		TreeMap<String,DocumentPojo> sourceUrlToKeyMap = null;
		HashSet<String> deletedSources = null;
		// Store the knowledge in the feeds collection in the harvester db
		Iterator<DocumentPojo> docIt = docs.iterator();
		while (docIt.hasNext()) {
			DocumentPojo f = docIt.next();
			nextId = f.getId(); // (only interested in the pruneSource case, in which case _id is set on input)
			
			if ((null != f.getSourceUrl()) && (null == f.getUrl())) { // special case ... delete all these documents...
				if ((null == deletedSources) || !deletedSources.contains(f.getSourceKey())) { // (don't bother deleting sourceURL if deleting source)
					if (null == sourceUrlToKeyMap) {
						sourceUrlToKeyMap = new TreeMap<String,DocumentPojo>();
					}
					sourceUrlToKeyMap.put(f.getSourceUrl(), f);				
				}//TESTED

				docIt.remove(); // (so don't miscount number of docs; processed below)
			}
			else if (null != f.getSourceKey() && (null == f.getSourceUrl()) && (null == f.getUrl())) {
				// Even more special case: delete entire sourceKey
				if (null == deletedSources) {
					deletedSources = new HashSet<String>();
				}
				if (!deletedSources.contains(f.getSourceKey())) {
					deletedSources.add(f.getSourceKey());
					long srcRemoved = removeFromDatastoreAndIndex_bySourceKey(f.getSourceKey(), f.getId(), true, f.getCommunityId().toString());
					if (srcRemoved > 0) {
						updateDocCountsOnTheFly(-srcRemoved, f.getSourceKey(), f.getCommunityId());						
					}
				}
				docIt.remove(); // (so don't miscount number of docs)
			}//TESTED
			else {
				removeFromDatastore_byURL(col, f, fields, 
						StoreAndIndexManager.docHasExternalContent(f.getUrl(), f.getSourceUrl()));
					// (adds "_id", "index")
			}
		}//TESTED

		// Now tidy up sourceUrls, do some caching across sourceKey/community for performance
		String sourceKey = null; // (if deleting sourceKey don't bother deleting any sourceUrls)
		long removed = 0; // (from special operations)
		String cachedSourceKey = null; // (will handle multiple source keys, although that can't currently happen in practice)
		ObjectId communityId = null;
		if (null != sourceUrlToKeyMap) for (Map.Entry<String, DocumentPojo> entry: sourceUrlToKeyMap.entrySet()) {
			String srcUrl = entry.getKey();
			DocumentPojo doc = entry.getValue();
			sourceKey = doc.getSourceKey();
			communityId = doc.getCommunityId();
			if (sourceKey != cachedSourceKey) { // ptr comparison by design
				if (removed > 0) {
					updateDocCountsOnTheFly(-removed, sourceKey, communityId);
					removed = 0;
				}//TESTED
				cachedSourceKey = sourceKey;
			}
			removed += removeFromDatastoreAndIndex_bySourceUrl(srcUrl, sourceKey, communityId);
		}//TESTED
		if ((removed > 0) && (null != sourceKey)) {
			updateDocCountsOnTheFly(-removed, sourceKey, communityId);
		}//TESTED
		return nextId;
	}//TESTED
	
	public void updateDocCountsOnTheFly(long docIncrement, String sourceKey, ObjectId communityId)
	{
		DbManager.getDocument().getCounts().update(new BasicDBObject(DocCountPojo._id_, communityId), 
				new BasicDBObject(DbManager.inc_, new BasicDBObject(DocCountPojo.doccount_, docIncrement)));
		DbManager.getIngest().getSource().update(new BasicDBObject(SourcePojo.key_, sourceKey),
				new BasicDBObject(MongoDbManager.inc_, 
						new BasicDBObject(SourceHarvestStatusPojo.sourceQuery_doccount_, docIncrement))
				);		
	}//TESTED
	
	/**
	 * Remove a doc from the data store
	 * @param col
	 * @param doc
	 * @param fields - fields to retrieve
	 * 
	 * CALLED FROM:	removeFromDataStore_byId(List<doc>, bDeleteContent)
	 * 					resizeDB() <- FILLS IN _ID, SOURCEKEY, INDEX, SOURCEURL
	 */
	private void removeFromDatastore_byId(DBCollection col, List<DocumentPojo> docs) {
		// Store the knowledge in the feeds collection in the harvester db			
		for ( DocumentPojo f : docs) {
			removeFromDatastore_byId(col, f);
		}
	}//TESTED
	
	/**
	 * Remove a doc from the data store
	 * @param col
	 * @param doc - assumes _id set
	 * @param fields - fields to retrieve (set in outside the doc loop for performance, url, index, sourceKey)
	 * 
	 * CALLED FROM:	removeFromDataStore_byId(col, List<doc>, bDeleteContent) 
	 * 					removeFromDataStore_byId(List<doc>, bDeleteContent) 
	 * 						resizeDB() <- _ID, SOURCEKEY, INDEX, SOURCEURL
	 */
	private void removeFromDatastore_byId(DBCollection col, DocumentPojo doc) {
		
		boolean bDeleteContent =  docHasExternalContent(doc.getUrl(), doc.getSourceUrl());
		
		if (bDeleteContent) {
			// Remove its content also:
			if (!_diagnosticMode) {
				BasicDBObject contentQuery = new BasicDBObject(DocumentPojo.url_, doc.getUrl());
				contentQuery.put(DocumentPojo.sourceKey_, doc.getSourceKey()); 
				DbManager.getDocument().getContent().remove(contentQuery);
			}
			else {
				System.out.println("StoreAndIndexManager.removeFromDatastore_byId, delete content: " + doc.getSourceKey() + "/" + doc.getUrl());
			}
		}
		
		// Update Mongodb with the data
		BasicDBObject query = new BasicDBObject();
		query.put(DocumentPojo.sourceKey_, doc.getSourceKey());
		query.put(DocumentPojo._id_, doc.getId());
		query.put(DocumentPojo.sourceKey_, doc.getSourceKey()); // (needed because on newer machines this is the shard key)
		
		if (!_diagnosticMode) {
			BasicDBObject softDelete = getSoftDeleteUpdate();
			col.update(query, softDelete);
				// (can do this on sharded collections because it uses sourceKey+_id, the shard key)
		}
		else { // (diagnostic mode)
			if (null != col.findOne(query)) {
				System.out.println("StoreAndIndexManager.removeFromDatastore_byId, delete: " + doc.toDb().toString());
			}
			else {
				System.out.println("StoreAndIndexManager.removeFromDatastore_byId, delete: DOC NOT FOUND");				
			}
		}
	}//TESTED (1.1)
	
	/**
	 * Remove a doc from the data store, ensures all the fields specified in "fields" are populated (ready for index deletion)
	 * @param col
	 * @param doc - needs  url, sourceKey set
	 * @param fields - fields to retrieve (index, created), set in calling function outside of loop for performance
	 * 
	 * CALLED FROM: removeFromDatastore_byURL(col, List<doc>, bDeleteContent) <- ADDS INDEX, CREATED TO FIELDS 
	 * 					removeFromDataStore_byURL(List<doc>, bDeleteContent) [ALSO DELETES FROM INDEX AFTER ADDED FROM HERE]
	 * 						MongoDocumentTxfer.doDelete(...)  <- SETS URL, SOURCE URL, SOURCE KEY, COMMUNITY ID, INDEX, _ID
	 * 						processDocuments(...) [ always called after harvester: have sourceUrl, sourceKey, 
	 * 												DON'T have _id, BUT do have updateId and index (correct except in many geo cases)]
	 * 						pruneSource(source, ...) <- SETS URL, SOURCE URL, SOURCE KEY, INDEX
	 * 							updateHarvestStatus(...)
	 */
	private void removeFromDatastore_byURL(DBCollection col, DocumentPojo doc, BasicDBObject fields, boolean bDeleteContent) {
		
		// 1] Create the query to soft delete the document
		
		BasicDBObject query = new BasicDBObject();
		query.put(DocumentPojo.url_, doc.getUrl());
		query.put(DocumentPojo.sourceKey_, doc.getSourceKey());

		// 2] Delete the content if needed
		
		if (bDeleteContent) {
			if (docHasExternalContent(doc.getUrl(), doc.getSourceUrl())) {
				if (!_diagnosticMode) {
					DbManager.getDocument().getContent().remove(query);
				}
				else {
					System.out.println("StoreAndIndexManager.removeFromDatastore_byUrl(2), delete content: " + doc.getSourceKey() + "/" + doc.getUrl());
				}
			}
		}
		//TESTED
		
		// 3] Work out which fields we have and which (if any we need to go and fetch):
		
		boolean needToFindAndModify = false;
		
		if (null == doc.getId()) { // This is called from processDocuments
			
			if (null != doc.getUpdateId()) { // update case...
				doc.setId(doc.getUpdateId()); // (note this is overwritten by addToDatastore later, in update case, so we're good)

				// (doc.index is populated but may not be correct because of the "many geos" workaround):
				if (DocumentPojoIndexMap.hasManyGeos(doc)) {
					doc.setIndex(DocumentPojoIndexMap.manyGeoDocumentIndex_);
					// (note this check isn't stateless, it actually populates "locs" at the same time
					//  this is handled in addToDatastore (update case), temp removed when adding to DB
				}//TESTED (2.1.2, diagnostic mode, doc2)
			}
			else { // Not an update case, we're going to have to grab the document after all, which is a bit slower
				needToFindAndModify = true;
			}
		}//TESTED (2.1.2, diagnostic mode, doc2)
		if (!needToFindAndModify) { // set created if we need to, since we're not grabbing it from the datastore
			if (null != doc.getUpdateId()) { // (this means we have an approx created if we don't need to go fetch the deleted doc)
				doc.setCreated(new Date(doc.getUpdateId().getTime()));
			}//TESTED (2.1.2, diagnostic mode, doc2)					
		}
		// (if we're here and index is not set, then it is intended to be null)
		
		// 4] Update the doc_metadata collection
		
		BasicDBObject softDelete = getSoftDeleteUpdate();
		BasicDBObject deadDoc = null; // (not normally needed)
		
		if (needToFindAndModify) { // less pleasant, need to go grab the doc
			deadDoc = (BasicDBObject) col.findOne(query, fields);				
		}//TESTED (2.1.2)
		
		if (!_diagnosticMode) {
			col.update(query, softDelete, false, true); // (needs to be multi- even though there's a single element for sharding reasons)			
		}//TESTED (2.1.2)
		
		// 5] Add fields if necessary
		
		if (null != deadDoc) {
			doc.setCreated((Date) deadDoc.get(DocumentPojo.created_));
				// (if getting this doc anyway then might as well get the created)
			doc.setId((ObjectId) deadDoc.get(DocumentPojo._id_));
			doc.setIndex((String) deadDoc.get(DocumentPojo.index_));
			
			if (_diagnosticMode) {
				System.out.println("StoreAndIndexManager.removeFromDatastore_byUrl(2): found " + deadDoc.toString());
			}
		}//TESTED (2.1.2)
		else if (_diagnosticMode) {
			if (!needToFindAndModify) {
				System.out.println("StoreAndIndexManager.removeFromDatastore_byUrl(2): straight deleted " + doc.toDb().toString());
			}
			else {
				System.out.println("StoreAndIndexManager.removeFromDatastore_byUrl(2): didn't find " + query.toString());
			}
		}//TESTED (2.1.2)
	}//TESSTED (2.1.2)

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
						((null != doc.getEntities())?("" + doc.getEntities().size()):"0") + " entities, " +
						((null != doc.getAssociations())?("" + doc.getAssociations().size()):"0") + " assocs, " +
						((null != doc.getLocs())?("" + doc.getLocs().size()):"0") + " locs"
								);
			}
		}// (end loop over docs)
		
		// Bulk add remaining docs		
		sendToIndex(indexManager, tmpDocs);
			
	}//TESTED (not change since by-eye testing in Beta)
	
	// Utility required by the above function
	
	private void sendToIndex(ElasticSearchManager indexManager, LinkedList<DocumentPojo> docsToAdd) {
		try {
			if (!docsToAdd.isEmpty()) {
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
			if ((null != doc.getIndex()) && doc.getIndex().equals("?DEL?"))  {
				continue; //(must have already been deleted, so can ignore)
			}
			if ((null == sIndex) || (null == doc.getIndex()) || !sIndex.equals(doc.getIndex())) { // Change index
				
				if (null != indexManager) { // ie not first time through, bulk delete what docs we have
					deleteFromIndex(indexManager, tmpDocs); // (clears tmpDocs)
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
				deleteFromIndex(indexManager, tmpDocs); // (clears tmpDocs)
				nTmpDocs = 0;
			}
			
			//delete from event search
			if (_diagnosticMode) {
				System.out.println("StoreAndIndexManager.removeFromSearch, remove: " + doc.getId() + " + " +
						((null != doc.getEntities())?("" + doc.getEntities().size()):"0") + " entities, " +
						((null != doc.getAssociations())?("" + doc.getAssociations().size()):"0") + " assocs, " +
						((null != doc.getLocs())?("" + doc.getLocs().size()):"0") + " locs"
						);
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
		return resizeDB(-1);
	}

	public boolean resizeDB(long capacityOverride)
	{
		//Do quick check to check if we are already under storage requirements
		if ( checkStorageCapacity(capacityOverride) ) {
			return false;
		}
		else
		{
			//if quick check fails, start removing docs to get under requirement
			try
			{
				long currDocsInDB = DbManager.getDocument().getMetadata().count();
				long storageCap = (capacityOverride == -1L) ? new PropertiesManager().getStorageCapacity() : capacityOverride;

				List<DocumentPojo> docsToRemove = getLeastActiveDocs((int) (currDocsInDB-storageCap));
					// (populates docsToRemove with _id and sourceKey - needed to support doc_metadata sharding)
				
				removeFromDatastore_byId(docsToRemove); // (remove content since don't know if it exists)
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
	private boolean checkStorageCapacity(long capacityOverride)
	{
		long currDocsInDB = 0;
		try {
			currDocsInDB = DbManager.getDocument().getMetadata().count();
		} catch (Exception e ) {
			// If an exception occurs log the error
			logger.error("Exception Message: " + e.getMessage(), e);
		}
		long storageCapacity = (-1L == capacityOverride) ? new PropertiesManager().getStorageCapacity() : capacityOverride;
		return (currDocsInDB <= storageCapacity); 
	}

	/**
	 * Returns a list of the least active documents
	 * List is of length numDocs
	 * 
	 * @param numDocs Number of documents to return that are least active
	 * @return a list of documents that are least active in DB (populates docsToRemove with _id and sourceKey - needed to support doc_metadata sharding)
)
	 */
	private List<DocumentPojo> getLeastActiveDocs(int numDocs)
	{
		List<DocumentPojo> olddocs = null;

		//TODO (INF-1301): WRITE AN ALGORITHM TO CALCULATE THIS BASED ON USAGE, just using time last accessed currently
		//give a weight to documents age and documents activity to calculate
		//least active (current incarnation doesn't work)
		try
		{
			BasicDBObject fields = new BasicDBObject(DocumentPojo._id_, 1);
			fields.put(DocumentPojo.sourceKey_, 1);
			fields.put(DocumentPojo.index_, 1);
			fields.put(DocumentPojo.sourceUrl_, 1);
			fields.put(DocumentPojo.url_, 1);
			DBCursor dbc = DbManager.getDocument().getMetadata().find(new BasicDBObject(), fields).
																	sort(new BasicDBObject(DocumentPojo._id_,1)).limit(numDocs);
			// (note, just retrieve _id and sourceKey fields: _id starts with timestamp so these are approximately oldest created)

			olddocs = DocumentPojo.listFromDb(dbc, DocumentPojo.listType());

		}
		catch (Exception e )
		{
			// If an exception occurs log the error
			logger.error("Exception Message: " + e.getMessage(), e);
		}
		return olddocs;
	}//TESTED (1.1)			

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
