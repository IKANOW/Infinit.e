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
package com.ikanow.infinit.e.processing.generic.synchronization;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.action.search.SearchRequestBuilder;
import org.elasticsearch.index.get.GetField;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;

import com.ikanow.infinit.e.data_model.index.ElasticSearchManager;
import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.MongoDbManager;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.ikanow.infinit.e.data_model.store.document.CompressedFullTextPojo;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.infinit.e.processing.generic.store_and_index.StoreAndIndexManager;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

public class SynchronizationManager {

	private static Logger logger = Logger.getLogger(SynchronizationManager.class);
	private static boolean bKillMeNow = false;
	public static void killMe() {
		bKillMeNow = true;
	}
	
	private List<SourcePojo> sources = null;
	public void setSources(List<SourcePojo> sources) {
		this.sources = sources;
	}
	
	/**
	 * Does the DB sync, pulls all mongo docs that occured from the
	 * cleanseStartTime and source and makes sure they are in the search db.
	 * 
	 * @param lastCleanse 1 hour before this harvester started
	 * @param sources list of sources we are syncing
	 * @return The number of errors fixed (docs deleted)
	 */
	// DON'T USE THIS UNTIL REWRITTEN - IT SHOULD TRANSFER DOCS ACROSS, NOT LEAVE THEM ALONE
	@Deprecated
	public int syncDB(long cleanseStartTime, Set<String> dbCache)
	{
		dbCache.clear();
		
		int fixcount = 0;
		DBCollection contentDb = DbManager.getDocument().getContent();
		DBCollection documentDb = DbManager.getDocument().getMetadata();
		StoreAndIndexManager storeManager = new StoreAndIndexManager();
		
		for ( SourcePojo sp : sources ) {
			// Don't combine the sources (apart from unusual multi-community case), because
			// that prevents you from using the compound sourceKey/_id index
			
			List<String> sourceKeyList = new ArrayList<String>();
			sourceKeyList.add(sp.getKey());
			if (sp.getCommunityIds().size() > 1) { // Special case, need to add the communities
				for (ObjectId communityId: sp.getCommunityIds()) {
					sourceKeyList.add(new StringBuffer(sp.getKey()).append('#').append(communityId.toString()).toString());
				}
			}//(end handling rare multi-community case)
			
			try 
			{	
				List<DocumentPojo> docs_to_remove = new ArrayList<DocumentPojo>();
				//FIRST DO ALL NEW FEEDS
				BasicDBObject query = new BasicDBObject();
				query.put(DocumentPojo._id_, new BasicDBObject(MongoDbManager.gt_, new ObjectId((int)(cleanseStartTime/1000), 0, 0))); // time aspect
				query.put(DocumentPojo.sourceKey_, new BasicDBObject(MongoDbManager.in_, sourceKeyList) ); //source aspect
				BasicDBObject queryFields = new BasicDBObject();
				queryFields.append(DocumentPojo.url_, 1);
				queryFields.append(DocumentPojo.index_, 1);
				queryFields.append(DocumentPojo.sourceKey_, 1);
				
				DBCursor cur = documentDb.find(query, queryFields).batchSize(100); 
				ElasticSearchManager esm = null;
				ElasticSearchManager esm_base = ElasticSearchManager.getIndex("document_index");
				String sIndex = null;

				while (cur.hasNext())
				{
					if (bKillMeNow) {
						return fixcount;
					}
									
					DocumentPojo doc = DocumentPojo.fromDb(cur.next(), DocumentPojo.class);
					if (null != doc.getId()) {
						dbCache.add(doc.getId().toString());
					}
					
					// Get index of doc to check in:
					String sNewIndex = doc.getIndex();
					if (null == sNewIndex) {
						sIndex = null;
						esm = esm_base;
					}
					else if ((null == sIndex) || (!sNewIndex.equals(sIndex))) {
						sIndex = sNewIndex;
						if (sNewIndex.equals("document_index")) {
							esm = esm_base;
						}
						else {
							esm = ElasticSearchManager.getIndex(sNewIndex + "/document_index");
						}
					}				
					
					//Compare mongo doc to search doc
					Map<String, GetField> results = esm.getDocument(doc.getId().toString(),DocumentPojo.url_);
					if ( null == results || results.isEmpty() )
					{
						//either too many entries (duplicates) or no entry
						//delete this doc from both
						logger.info("db sync removing doc: " + doc.getId() + "/" + doc.getSourceKey() + " not found in search (or duplicate)");						
						docs_to_remove.add(doc);					
						documentDb.remove(new BasicDBObject(DocumentPojo._id_, doc.getId()));
						BasicDBObject contentQ = new BasicDBObject(CompressedFullTextPojo.url_, doc.getUrl());
						contentQ.put(CompressedFullTextPojo.sourceKey_, new BasicDBObject(MongoDbManager.in_, Arrays.asList(null, doc.getSourceKey())));						
						contentDb.remove(contentQ);
						fixcount++;
					}
				} //end loop over new docs for this source
				storeManager.removeFromSearch(docs_to_remove);
				
				//NOW VERIFY ALL OLD FEEDS
				int iteration = 1;
				boolean removedAll = true;
				docs_to_remove.clear();
				while (removedAll)
				{
					int rows = iteration*iteration*10; //10x^2 exponentially check more docs
					int oldfixes = 0;
					BasicDBObject queryOLD = new BasicDBObject();	
					queryOLD.put(DocumentPojo.sourceKey_, new BasicDBObject(MongoDbManager.in_, sourceKeyList) ); //source aspect
					BasicDBObject sortOLD = new BasicDBObject(DocumentPojo._id_, 1);
					
					DBCursor curOLD = documentDb.find(queryOLD, queryFields).sort(sortOLD).limit(rows);
					while (curOLD.hasNext())
					{
						DocumentPojo doc = DocumentPojo.fromDb(curOLD.next(), DocumentPojo.class);				
						if (null != doc.getId()) {
							dbCache.add(doc.getId().toString());
						}
						
						// Get index of doc to check in:
						String sNewIndex = doc.getIndex();
						if (null == sNewIndex) {
							sIndex = null;
							esm = esm_base;
						}
						else if ((null == sIndex) || (!sNewIndex.equals(sIndex))) {
							sIndex = sNewIndex;
							if (sNewIndex.equals("document_index")) {
								esm = esm_base;
							}
							else {
								esm = ElasticSearchManager.getIndex(sNewIndex + "/document_index");
							}
						}
						
						//Compare mongo doc to search doc
						Map<String, GetField> results = esm.getDocument(doc.getId().toString(),DocumentPojo.url_);
						if ( null == results || results.isEmpty() )
						{
							//either too many entries (duplicates) or no entry
							//delete this doc from both
							logger.info("db sync removing doc: " + doc.getId() + "/" + doc.getSourceKey() + " not found in search (or duplicate)");						
							docs_to_remove.add(doc);						
							documentDb.remove(new BasicDBObject(DocumentPojo._id_, doc.getId()));
							contentDb.remove(new BasicDBObject(DocumentPojo.url_, doc.getUrl()));
							fixcount++;
							oldfixes++;
						}					
					}
					if ( oldfixes != rows )
						removedAll = false;
				}//(end loop over old docs for this source)
				storeManager.removeFromSearch(docs_to_remove);
			} 
			catch (Exception e) 
			{
				// If an exception occurs log the error
				logger.error("Exception Message: " + e.getMessage(), e);
			} 
		}		
		return fixcount;
	}//TESTED (unchanged from "tested" Beta version)
	
	
	/**
	 * Does the DB sync, pulls all solr docs that occured from the
	 * cleanseStartTime and source and makes sure they are in the mongo db.
	 * 
	 * @param lastCleanse 1 hour before this harvester started
	 * @param sources list of sources we are syncing
	 * @return The number of errors fixed (docs deleted)
	 */
	//TODO INF-2239 ... lol fail if syncDB isn't called then dbCache is empty and everything gets deleted...
	public int syncSearch(long cleanseStartTime, Set<String> dbCache)
	{
		int fixcount = 0;
		StoreAndIndexManager storeManager = new StoreAndIndexManager();
		
		// NO LONGER NEEDED, HAVE CACHE (EXCEPT IN ONE PLACE, THE "OLD DOCS" CHECK)
		DBCollection documentDb = DbManager.getDocument().getMetadata();
		BasicDBObject queryFields = new BasicDBObject(); // (ie just _id, basically only need to know if it exists)
		try 
		{	
			//get solr entries from last cleanse point	
			int source_index = 0;
			int source_count = sources.size();
			for ( SourcePojo sp : sources )
			{
				if (bKillMeNow) {
					return fixcount;
				}
				List<DocumentPojo> docs_to_remove = new ArrayList<DocumentPojo>();
				
				// Get all indexes this source might use:
				StringBuffer sb = new StringBuffer("document_index");
				for (ObjectId sCommunityId: sp.getCommunityIds()) {
					sb.append(",doc_").append(sCommunityId.toString());
				}
				sb.append("/document_index");
				
				ElasticSearchManager esm = ElasticSearchManager.getIndex(sb.toString());
								
				SearchRequestBuilder searchOptions = esm.getSearchOptions();
				BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
				boolQuery.must(QueryBuilders.rangeQuery(DocumentPojo.created_).from(cleanseStartTime));
				boolQuery.must(QueryBuilders.termQuery(DocumentPojo.sourceKey_, sp.getKey() ));
				searchOptions.setSize(200); // (note this is multiplied by the number of primary shards)
				searchOptions.setSearchType(SearchType.SCAN);
				searchOptions.setScroll("10m");
				SearchResponse rsp = esm.doQuery(boolQuery, searchOptions);
				String scrollId = rsp.getScrollId();
				int nSkip = 0;
				
				for (;;) // Until no more hits
				{
					rsp = esm.doScrollingQuery(scrollId, "10m");
					SearchHit[] docs = rsp.getHits().getHits();
					scrollId = rsp.getScrollId();
					
					if ((null == docs) || (0 == docs.length)) {
						break;
					}					
					if (docs.length > 100) { // just display large checks)
						logger.info("Checking ES docs for large source=" + sp.getKey() + " source: " + source_index + "/" + source_count + " from " + nSkip + " to " + (nSkip+docs.length) );
					}
					
					//Check all solr docs against mongodb
					
					for (SearchHit hit: docs) 
					{
						String idStr = hit.getId();
						boolean found = true; //(fail closed!)
						if (null == dbCache) {
							//OBSOLETED, USE DBCACHE INSTEAD (WHERE AVAILABLE):
							ObjectId id = new ObjectId(idStr);
							BasicDBObject query = new BasicDBObject(DocumentPojo._id_, id);
							query.put(DocumentPojo.sourceKey_, sp.getKey()); // (ensures uses only the right shard)
							DBObject dbo = documentDb.findOne(query, queryFields);
							found = (dbo != null);
						}//TESTED
						else {
							found = dbCache.contains(idStr);
						}//TESTED
						if (!found) 
						{				
							ObjectId id = new ObjectId(idStr);
							DocumentPojo doc = new DocumentPojo();
							doc.setId(id);
							doc.setIndex(hit.getIndex() + "/document_index");
							docs_to_remove.add(doc);
							logger.info("db sync removing doc: " + id + "/" + hit.getIndex() + "/" + source_index + " not found in mongo");
							fixcount++;
						} // end if not found
					} // end loop over docs to check
					
					nSkip += docs.length;
				}// until no more hits
				if (!docs_to_remove.isEmpty()) {
					storeManager.removeFromSearch(docs_to_remove);
					docs_to_remove.clear();
				}
				
				//CHECK OLD FEEDS 10 at atime
				int iteration = 1;
				boolean removedAll = true; 
				while (removedAll )
				{
					int rows = iteration*iteration*10;//exponential scaling 10x^2
					iteration++;
					int oldfixes = 0;
					
					//get old docs from es
					SearchRequestBuilder searchOptionsOLD = esm.getSearchOptions();
					BoolQueryBuilder boolQueryOLD = QueryBuilders.boolQuery();
					boolQueryOLD.must(QueryBuilders.rangeQuery(DocumentPojo.created_).from(cleanseStartTime));
					boolQueryOLD.must(QueryBuilders.termQuery(DocumentPojo.sourceKey_, sp.getKey()));
					searchOptionsOLD.addSort(DocumentPojo.created_, SortOrder.ASC);
					searchOptionsOLD.setSize(rows);
					SearchResponse rspOLD = esm.doQuery(boolQueryOLD, searchOptionsOLD);
					SearchHit[] docsOLD = rspOLD.getHits().getHits();
					
					//Check all solr docs against mongodb
					
					for (SearchHit hit: docsOLD) 				
					{
						String idStr = hit.getId();
						boolean found = true;
						if (null == dbCache) {
							//OBSOLETED, USE DBCACHE INSTEAD (WHERE AVAILABLE):
							ObjectId id = new ObjectId(idStr);
							BasicDBObject queryOLD = new BasicDBObject(DocumentPojo._id_, id);
							DBObject dbo = documentDb.findOne(queryOLD, queryFields);
							found = (dbo != null);
						}//TESTED
						else {
							 found = dbCache.contains(idStr);
						}//TESTED
						if (!found)
						{				
							// Also need to check the DB since dbCache is not guaranteed to be populated with the same
							// number of "final" docs
							ObjectId id = new ObjectId(idStr);
							if (rows > 10) { // (dbCache always loaded with the first 10 rows)
								BasicDBObject queryOLD = new BasicDBObject(DocumentPojo._id_, id);
								if (null != documentDb.findOne(queryOLD, queryFields)) { // it is actually present
									continue;
								}
							}
							DocumentPojo doc = new DocumentPojo();
							doc.setId(id);
							doc.setIndex(hit.getIndex() + "/document_index");
							docs_to_remove.add(doc);
							logger.info("db sync removing doc: " + idStr + "/" + source_index + " not found in mongo");
							oldfixes++;
							fixcount++;
						}
					}
					if (!docs_to_remove.isEmpty()) {
						storeManager.removeFromSearch(docs_to_remove);
					}
					
					if ( oldfixes != rows )
						removedAll = false;
				}
				source_index++;
			} // end loop over sources
		} 
		catch (Exception e) 
		{
			// If an exception occurs log the error
			logger.error("Exception Message: " + e.getMessage(), e);
		} 
		return fixcount;
	}//TESTED (unchanged from "tested" Beta version)
}
