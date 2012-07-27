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
/**
 * 
 */
package com.ikanow.infinit.e.harvest.extraction.document;

import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.bson.types.ObjectId;

import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.MongoDbManager;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

/**
 * @author cmorgan
 *
 */
public class DuplicateManager_Integrated implements DuplicateManager {

	private static final Logger logger = Logger.getLogger(DuplicateManager_Integrated.class);	
	
/////////////////////////////////////////////////////////////////////////////////////////////////////////
	
// DUPLICATION LOGIC
	
	// If a URL already exists...
	// 1] For the same source: the document is a duplicate
	// 2] For a different source, with the same "configuration hash": Set that source as the "duplicateFrom" field 
	//    in the DocumentPojo which in the HarvestController will result in the DocumentPojo's being cloned from that {url,source} pair  
	// 3] For a different source, with a different "configuration hash": Proceed as if URL didn't already exist
	// Some implementation specifics:
	// The duplicate manager will store knowledge gained about other sources (eg whether they fall into [2] or [3]) for a given source.
	
	private Set<String> _sameConfigurationSources = null;
	private Set<String> _differentConfigurationSources = null;
	private Set<String> _sameCommunitySources = null;
	
/////////////////////////////////////////////////////////////////////////////////////////////////////////
	
// INTERFACE
		
	// Resets source-specific state
	public void resetForNewSource() {
		_sameConfigurationSources = null;
		_differentConfigurationSources = null;
		_sameCommunitySources = null;
	}

	/**
	 * Tests to see if duplicates exist based on defined key
	 * 
	 * @param collection
	 * @param key
	 * @return boolean (true/false)
	 */
	public boolean isDuplicate_UrlTitleDescription(String url, String title, String description, SourcePojo source, List<String> duplicateSources) {
		BasicDBObject query = new BasicDBObject(DocumentPojo.url_, url);
		BasicDBObject orQuery1 = new BasicDBObject(DocumentPojo.title_, title);
		BasicDBObject orQuery2 = new BasicDBObject(DocumentPojo.description_, title);
		query.put(MongoDbManager.or_, Arrays.asList(orQuery1, orQuery2));		
		
		return duplicationLogic(query, source, duplicateSources);
	}
	
	/**
	 * Tests to see if duplicates exist based on defined key
	 * 
	 * @param collection
	 * @param key
	 * @return boolean (true/false)
	 */
	public boolean isDuplicate_UrlTitle(String url, String title, SourcePojo source, List<String> duplicateSources) {
		BasicDBObject query = new BasicDBObject();
		query.put(DocumentPojo.url_, url);
		query.put(DocumentPojo.title_, title);
		
		return duplicationLogic(query, source, duplicateSources);
	}
	/**
	 * Tests to see if duplicates exist based on defined key
	 * 
	 * @param collection
	 * @param key
	 * @return boolean (true/false)
	 */
	public boolean isDuplicate_Url(String url, SourcePojo source, List<String> duplicateSources) {
		BasicDBObject query = new BasicDBObject();
		query.put(DocumentPojo.url_, url);

		return duplicationLogic(query, source, duplicateSources);
	}	

	public ObjectId getLastDuplicateId() {
		return _duplicateId;
	}
	
	public Date getLastDuplicateModifiedTime() {
		return _modifiedTimeOfActualDuplicate;
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	// File handling specific logic
	
	/**
	 * Tests to see if duplicates might exist.
	 * If it is not a duplicate, true is returned. If it is a duplicate,
	 * the modified date is then checked to see if the file has been updated.
	 * True is returned if the file has been updated, false otherwise.
	 * 
	 * @param collection
	 * @param modifiedDate
	 * @param url
	 * @param title
	 * @return boolean (true/false)
	 */
	public boolean needsUpdated_SourceUrl(Date modifiedDate, String sourceUrl, String sourceKey) {
		DBCollection collection = DbManager.getDocument().getMetadata();
		boolean ret = true;
		BasicDBObject query = new BasicDBObject();
		query.put(DocumentPojo.sourceUrl_, sourceUrl);
		addSourceKeyToQueries(query, sourceKey);
		BasicDBObject fields = new BasicDBObject(DocumentPojo.modified_, 1); 
		
		DBCursor dbc = collection.find(query, fields).limit(1);
			// (this should be very fast since sourceUrl is indexed ... order doesn't matter as all docs should have the same modified)

		if ( dbc.count() == 0 ) { //if there is no record, return true
			ret = true;
			modifiedDate.setTime(0);
		}
		else{ // (all docs should have same modified, though this is ~ time ordered anyway)
			
			BasicDBObject dbo = (BasicDBObject) dbc.iterator().next();
			Date oldModified = (Date) dbo.get(DocumentPojo.modified_);
			
			ret = (modifiedDate.getTime() != oldModified.getTime()); // ie if different -> true -> update docs from sourceUrl
			
			if (ret) { //TODO (INF-1520): temporary log for debugging purposes...
				logger.info("File update: " + sourceUrl + ": new=" + modifiedDate.getTime() + ", old=" + oldModified.getTime());
				//TODO (INF-1520): temp fix attempt?!
//				long time1 = modifiedDate.getTime()/10000; // (they only need to be within 10s of each other
//				long time2 =  oldModified.getTime()/10000;
//				if (time1 == time2) {
//					logger.info("(Temp workaround, vetoed file update)");
//					ret = false;
//				}
			}
		}
		return ret;
	}	
	public boolean needsUpdated_Url(Date modifiedDate, String url, String sourceKey) {
		DBCollection collection = DbManager.getDocument().getMetadata();
		boolean ret = true;
		BasicDBObject query = new BasicDBObject();
		query.put(DocumentPojo.url_, url);
		addSourceKeyToQueries(query, sourceKey);
		
		int count = collection.find(query).limit(1).count();

		if ( count == 0 ) { //if there is no record, return true
			ret = true;
		}
		else{
			query.put(DocumentPojo.modified_, new BasicDBObject(MongoDbManager.ne_, modifiedDate));
			ret = !(collection.find(query).limit(1).count() == 0);
		}
		return ret;
	}		
		
/////////////////////////////////////////////////////////////////////////////////////////////////////////
	
// UTILITY
	
	// Top level utility function to handle duplicate logic
	
	boolean duplicationLogic(BasicDBObject query, SourcePojo source, List<String> duplicateSources) {
		duplicateSources.clear();
		String parentSourceKey = null;
		if ((null != source.getRssConfig()) && (null != source.getRssConfig().getUpdateCycle_secs())) {
			//RSS and there's a means of updating
			parentSourceKey = source.getKey(); // (so can saved modified time and update id)
		}
		else if (null != source.getFileConfig()) {
			// File and we're processing XML, normally >1 /file (else just waste some CPU cycles anyway)
			parentSourceKey = source.getKey(); // (as above)			
		}//TESTEDx2
		// TODO (INF-1300): (Leave databases alone until update functionality is implemented, then check if is enabled)
		
		LinkedList<String> possibleDups = getCandidateDuplicates(query, parentSourceKey);
		if (!possibleDups.isEmpty()) {
			String definiteDup = isFunctionalDuplicate(source, possibleDups);
			if (null == definiteDup) {
				return false;
			}//TESTED
			else if (definiteDup.equalsIgnoreCase(source.getKey())) {
				return true;
			}//TESTED
			else {
				duplicateSources.add(definiteDup);
				return false;
			}//TESTED
		}
		else {
			return false; // Definitely not a duplicate
		}//TESTED
	}//TESTED (created different types of duplicate, put print statements in, tested by hand)
	
	// Utility function to take DB query and return key information from matching documents
	
	private Date _modifiedTimeOfActualDuplicate = null; // (if we have a pure 1-1 duplicate, store its modified time)
	private ObjectId _duplicateId = null; //  (if we have a pure 1-1 duplicate, store its _id)
	
	private LinkedList<String> getCandidateDuplicates(BasicDBObject query, String parentSourceKey) {
		_modifiedTimeOfActualDuplicate = null;
		_duplicateId = null;
		LinkedList<String> returnVal = new LinkedList<String>(); 
		
		DBCollection collection = DbManager.getDocument().getMetadata();
		BasicDBObject fields = new BasicDBObject(DocumentPojo.sourceKey_, 1);
		if (null != parentSourceKey) {
			fields.put(DocumentPojo.modified_, 1);
			fields.put(DocumentPojo.updateId_, 1);
		}//TESTED
		DBCursor dbc = collection.find(query, fields);
		
		while (dbc.hasNext()) {
			DBObject dbo = dbc.next();
			String sourceKey = (String) dbo.get(DocumentPojo.sourceKey_);
			if (null != sourceKey) {
				int nCompositeSourceKey = sourceKey.indexOf('#'); // (handle <key>#<id> case)
				if (-1 != nCompositeSourceKey) {
					sourceKey = sourceKey.substring(0, nCompositeSourceKey);
				}//TESTED
				returnVal.add(sourceKey);
			}			
			if ((null != parentSourceKey) && (parentSourceKey.equalsIgnoreCase(sourceKey))) {
				_modifiedTimeOfActualDuplicate = (Date) dbo.get(DocumentPojo.modified_);
				_duplicateId = (ObjectId) dbo.get(DocumentPojo.updateId_);
				if (null == _duplicateId) { // first time, use the _id
					_duplicateId = (ObjectId) dbo.get(DocumentPojo._id_);
				}
			}//TESTED
		}
		return returnVal;
	}//TESTED (created different types of duplicate, put print statements in, tested by hand)
	
	// Utility function to return one source containing a document for which this is a duplicate
	// Returns null if there isn't one
	// Updates _sameConfigurationSources, _differentConfigurationSources, _sameCommunitySources
		
	private String isFunctionalDuplicate(SourcePojo source, LinkedList<String> candidateSourceKeys) {
		// (Ensure everything's set up)
		if (null == _sameConfigurationSources) {
			_sameConfigurationSources = new TreeSet<String>();
			_differentConfigurationSources = new TreeSet<String>();				
			_sameCommunitySources = new TreeSet<String>();
		}
		if (null == source.getShah256Hash()) {
			source.generateShah256Hash();
		}
		
		// See if we've cached something:
		String returnVal = null;
		Iterator<String> it = candidateSourceKeys.iterator(); 
		while (it.hasNext()) {
			String sourceKey = it.next();
			
			if (!source.getDuplicateExistingUrls()) {
				// Check _sameCommunitySources: ignore+carry on if sourceKey isn't in here, else 
				// return sourceKey, which will treat as a non-update duplicate (non update because 
				// the update params only set if it was an update duplicate)
				if (_sameCommunitySources.contains(sourceKey)) {
					return source.getKey(); // (ie return fake source key that will cause above logic to occur)
				}
			}//TESTED
			
			if (sourceKey.equalsIgnoreCase(source.getKey())) {
				return sourceKey; // (the calling function will then treat it as a duplicate)
			}
			else if (_sameConfigurationSources.contains(sourceKey)) {
				returnVal = sourceKey; // (overwrite prev value, doesn't matter since this property is obv transitive)
			}
			else if (_differentConfigurationSources.contains(sourceKey)) {
				it.remove(); // (don't need to check this source out)
			}
		}//TESTED
		boolean bMatchedInCommunity = false; // (duplication logic below)
		if ((null == returnVal) && !candidateSourceKeys.isEmpty()) {
			
			// Need to query the DB for this source...			
			BasicDBObject query = new BasicDBObject(SourcePojo.shah256Hash_, source.getShah256Hash());
			query.put(SourcePojo.key_, new BasicDBObject(MongoDbManager.in_, candidateSourceKeys.toArray()));
			BasicDBObject fields = new BasicDBObject(SourcePojo._id_, 0);
			fields.put(SourcePojo.key_, 1);
			if (!source.getDuplicateExistingUrls()) {
				fields.put(SourcePojo.communityIds_, 1);				
			}
			DBCursor dbc = DbManager.getIngest().getSource().find(query, fields);
			while (dbc.hasNext()) {
				BasicDBObject dbo = (BasicDBObject) dbc.next();
				String sSourceKey = dbo.getString(SourcePojo.key_);
				
				// DON'T DEDUP LOGIC:
				if (!source.getDuplicateExistingUrls()) { 
					BasicDBList communities = (BasicDBList) dbo.get(SourcePojo.communityIds_);
					for (Object communityIdObj: communities) {
						ObjectId communityId = (ObjectId) communityIdObj;
						if (source.getCommunityIds().contains(communityId)) { // Not allowed to duplicate off this
							_sameCommunitySources.add(sSourceKey);
							bMatchedInCommunity = true;
						}
					}
				}//(end "don't duplicate existing URLs logic")
				//TESTED (same community and different communities)
				
				if (null != sSourceKey) {
					_sameConfigurationSources.add(sSourceKey);
					returnVal = sSourceKey; // (overwrite prev value, doesn't matter since this property is obv transitive)
				}
			}
			// Loop over config sources again to work out which keys can now be placed in the "_differentConfigurationSources" cache
			for (String sourceKey: candidateSourceKeys) {
				if (!_sameConfigurationSources.contains(sourceKey)) {
					_differentConfigurationSources.add(sourceKey);
				}
			}			
		}//TESTED
		if (bMatchedInCommunity) {
			return source.getKey(); // (ie return fake source key that will cause above logic to occur)
		}
		else {
			return returnVal;
		}
		
	}//TESTED (created different types of duplicate, put print statements in, tested by hand)

	// Utility needed because of minor complexity of searching for either <sourceKey> or <sourceKey>#<community> 
	
	private static void addSourceKeyToQueries(BasicDBObject query, String sourceKey) {
		query.put(DocumentPojo.sourceKey_, new BasicDBObject(MongoDbManager.regex_, new StringBuffer().append('^').append(sourceKey).append("(#|$)").toString()));
	}//TESTED (by hand)
	
}
