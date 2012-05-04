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

import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.MongoDbManager;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

/**
 * @author cmorgan
 *
 */
public class DuplicateManager_Integrated implements DuplicateManager {

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
	
/////////////////////////////////////////////////////////////////////////////////////////////////////////
	
// INTERFACE
		
	// Resets source-specific state
	public void resetForNewSource() {
		_sameConfigurationSources = null;
		_differentConfigurationSources = null;
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
		
		int count = collection.find(query).limit(1).count();

		if ( count == 0 ) { //if there is no record, return true
			ret = true;
			modifiedDate.setTime(0);
		}
		else{
			query.put(DocumentPojo.modified_, new BasicDBObject(MongoDbManager.ne_, modifiedDate));
			ret = !(collection.find(query).limit(1).count() == 0);
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
			parentSourceKey = source.getKey(); // (so can saved modified time for later analysis)
		}//TESTED
		
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
	
	private LinkedList<String> getCandidateDuplicates(BasicDBObject query, String parentSourceKey) {
		_modifiedTimeOfActualDuplicate = null;
		LinkedList<String> returnVal = new LinkedList<String>(); 
		
		DBCollection collection = DbManager.getDocument().getMetadata();
		BasicDBObject fields = new BasicDBObject(DocumentPojo.sourceKey_, 1);
		fields.put(DocumentPojo._id_, 0);
		if (null != parentSourceKey) {
			fields.put(DocumentPojo.modified_, 1);
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
			}//TESTED
		}
		return returnVal;
	}//TESTED (created different types of duplicate, put print statements in, tested by hand)
	
	// Utility function to return one source containing a document for which this is a duplicate
	// Returns null if there isn't one
	// Updates _sameConfigurationSources, _differentConfigurationSources
		
	private String isFunctionalDuplicate(SourcePojo source, LinkedList<String> candidateSourceKeys) {
		// (Ensure everything's set up)
		if (null == _sameConfigurationSources) {
			_sameConfigurationSources = new TreeSet<String>();
			_differentConfigurationSources = new TreeSet<String>();				
		}
		if (null == source.getShah256Hash()) {
			source.generateShah256Hash();
		}
		// See if we've cached something:
		String returnVal = null;
		Iterator<String> it = candidateSourceKeys.iterator(); 
		while (it.hasNext()) {
			String sourceKey = it.next();
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
		if ((null == returnVal) && !candidateSourceKeys.isEmpty()) {
			
			// Need to query the DB for this source...			
			BasicDBObject query = new BasicDBObject(SourcePojo.shah256Hash_, source.getShah256Hash());
			query.put(SourcePojo.key_, new BasicDBObject(MongoDbManager.in_, candidateSourceKeys.toArray()));
			BasicDBObject fields = new BasicDBObject(SourcePojo._id_, 0);
			fields.put(SourcePojo.key_, 1);
			DBCursor dbc = DbManager.getIngest().getSource().find(query, fields);
			while (dbc.hasNext()) {
				BasicDBObject dbo = (BasicDBObject) dbc.next();
				String sSourceKey = dbo.getString(SourcePojo.key_);
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
		return returnVal;
		
	}//TESTED (created different types of duplicate, put print statements in, tested by hand)

	// Utility needed because of minor complexity of searching for either <sourceKey> or <sourceKey>#<community> 
	
	private static void addSourceKeyToQueries(BasicDBObject query, String sourceKey) {
		query.put(DocumentPojo.sourceKey_, new BasicDBObject(MongoDbManager.regex_, new StringBuffer().append('^').append(sourceKey).append("(#|$)").toString()));
	}//TESTED (by hand)
	
}
