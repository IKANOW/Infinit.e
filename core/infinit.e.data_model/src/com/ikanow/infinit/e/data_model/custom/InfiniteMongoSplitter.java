/*******************************************************************************
 * Copyright 2012 The Infinit.e Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.ikanow.infinit.e.data_model.custom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;

import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.log4j.Logger;
import org.bson.BasicBSONObject;
import org.bson.types.ObjectId;

import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.MongoDbManager;
import com.ikanow.infinit.e.data_model.store.config.source.SourceHarvestStatusPojo;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.infinit.e.data_model.store.document.EntityPojo;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoURI;
import com.mongodb.hadoop.input.MongoInputSplit;
import com.mongodb.hadoop.util.MongoSplitter;

public class InfiniteMongoSplitter
{
	public static final int MAX_SPLITS = 10000000;
	
	private static Logger _logger = Logger.getLogger(InfiniteMongoSplitter.class);			
	
	/**
	 * Checks if the new params MAX_SPLITS and MAX_DOCS_PER_SPLIT are set
	 * in the config.  If they are it will use those to do splits via limit/skip
	 * otherwise it will call the previous chunking splitter in MongoSplitter.
	 * 
	 * @param conf
	 * @return
	 */
	
	public static List<InputSplit> calculateSplits(InfiniteMongoConfig conf)
	{
		// First off: What is our sharding scheme?
		
		boolean shardingPolicyNew = false;
		try {
			BasicDBObject shardQuery = new BasicDBObject("_id", "doc_metadata.metadata");
			BasicDBObject shardInfo = (BasicDBObject) DbManager.getCollection("config", "collections").findOne(shardQuery);
			if (null != shardInfo) {
				BasicDBObject shardInfoKey = (BasicDBObject) shardInfo.get("key");
				if (null != shardInfoKey) {
					shardingPolicyNew = (shardInfoKey.size() > 1);
				}
			}
		}//TESTED (new and old)
		catch (Exception e) {} // stick with the old sharding, it's probably going to die soon after though, honestly
		
		// conf.getQuery returns a new copy of the query, so get once and use everywhere...
		BasicDBObject confQuery = (BasicDBObject) conf.getQuery();
		
		BasicDBObject srcTagsQuery = (BasicDBObject) conf.getSourceTags();
		
		String collection = conf.getInputURI().getCollection();
		if (!collection.equals(DbManager.getDocument().getContent().getName()) && !collection.equals(DbManager.getDocument().getMetadata().getName()))
		{
			// Case 1: feature table or custom table
			// Just run legacy code
			return calculateSplits_phase2(conf, confQuery, false, false, null);			
		}
		else { // complex cases...
			boolean simpleOtherIndex = false;
			// Check whether a simple query has been performed on a different indexed field			
			if (null == srcTagsQuery) { // (if srcTags specified, then going to want to use sourceKey as the index)
				for (String s: Arrays.asList(EntityPojo.docQuery_index_, DocumentPojo.url_)) {
					Object selector = confQuery.get(s);
					if (selector instanceof String) {
						simpleOtherIndex = true;
						break;
					}
					else if (selector instanceof DBObject) {
						DBObject selectorDbo = (DBObject)selector;
						if (selectorDbo.containsField(DbManager.in_)) {
							simpleOtherIndex = true;
							break;
						}
					}
				}//TESTED (both types, plus check complex indexes don't work)			
				// ALLOWED: {"entities.index": { "$in": [ "xxx", "yyy"] }, {"entities.index": "xxx" }, ditto for "url"
				// NOT ALLOWED: { "entities.index": { "$ne": "xxx" } }
			}
			//TESTED check ignored if eg entity_index specified
			
			if (simpleOtherIndex) {
				// Case 2: we have a simple query on an indexed field 
				// Just run legacy code
				
				return calculateSplits_phase2(conf, confQuery, false, shardingPolicyNew, null);					
			}//TESTED
			else if (conf.getLimit() > 0) { // debug
				//Case 3: Ensure we have small sets of sources to search over
				BasicDBList collectionOfSplits = splitPrecalculations_oldShardSchemeOrDebug(confQuery, srcTagsQuery, conf.getMaxDocsPerSplit());
				final List<InputSplit> splits = new ArrayList<InputSplit>();
				
				boolean queryNonTrivial = isQueryNonTrivial(confQuery);
				if (!queryNonTrivial) {
					//Case 3a: query is trivial, so can just create splits directly from the split pre-calcs
					int toProcess = conf.getLimit();
					Iterator<Object> itSplit = collectionOfSplits.iterator();
					while ((toProcess > 0) && (itSplit.hasNext())) {
						BasicDBObject split = (BasicDBObject) itSplit.next();

						int docCount = (int)split.getLong(SourceHarvestStatusPojo.doccount_, 0L);
						int toGet = (docCount > toProcess) ? toProcess : docCount;
						BasicDBObject modQuery = convertQuery(confQuery, split.get(DocumentPojo.sourceKey_));
						if (null != modQuery) {
							splits.add(new InfiniteMongoInputSplit(conf.getInputURI(), conf.getInputKey(), modQuery, conf.getFields(), conf.getSort(), toGet, 0, conf.isNoTimeout()));
							toProcess -= docCount;
						}
					}//TESTED
				}
				else {
					// Case 3b: annoying, some extra query terms, gonna need to do it the hard way...
					int toProcess = conf.getLimit();
					Iterator<Object> itSplit = collectionOfSplits.iterator();
					DBCollection coll = InfiniteMongoConfigUtil.getCollection(conf.getInputURI());
					while ((toProcess > 0) && (itSplit.hasNext())) {
						BasicDBObject split = (BasicDBObject) itSplit.next();
						
						BasicDBObject modQuery = convertQuery(confQuery, split.get(DocumentPojo.sourceKey_));
						if (null != modQuery) {
							int docsCounted = (int) coll.getCount(modQuery, null, toProcess, 0);
							int toGet = (docsCounted > toProcess) ? toProcess : docsCounted;
							if (docsCounted > 0) {
								splits.add(new InfiniteMongoInputSplit(conf.getInputURI(), conf.getInputKey(), modQuery, conf.getFields(), conf.getSort(), toGet, 0, conf.isNoTimeout()));
								toProcess -= docsCounted;
							}
						}//TESTED
					}
				}//TESTED
				
				return splits;
			}
			else { // More complex cases:
				
				if (shardingPolicyNew) {
					// Case 4a: NEW SHARDING SCHEME
					
					// Always fetch the new sources, eg convert communityId to sourceKeys
					try {					
						splitPrecalculations_newShardScheme(confQuery, srcTagsQuery); // (modifies confQuery if returns true)				
						boolean queryNonTrivial = isQueryNonTrivial(confQuery);
						
						return calculateSplits_phase2(conf, confQuery, !queryNonTrivial, shardingPolicyNew, null);

							// (ie trivial query => always use chunks, bypass skip/limit test)
					}//TESTED (trivial + non-trivial)
					catch (Exception e) { // Didn't match any sources, no problem
						return new ArrayList<InputSplit>();
					}//TESTED
					
				}//TESTED
				else {

					BasicDBList collectionOfSplits = splitPrecalculations_oldShardSchemeOrDebug(confQuery, srcTagsQuery, conf.getMaxDocsPerSplit());
					
					if (null == collectionOfSplits) {
						// Case 4b: OLD SHARDING SCHEME can't get a partition by source keys, just back off to old code
						return calculateSplits_phase2(conf, confQuery, false, shardingPolicyNew, null);						
					}//TESTED (old code)
					else {
						conf.setMaxDocsPerSplit(2*conf.getMaxDocsPerSplit());
							// (because we stop creating splits when the exceed the size)
						
						// Case 4c: OLD SHARDING SCHEME, have a source key partition
						int nMaxCount = 1 + conf.getMaxDocsPerSplit()*conf.getMaxSplits();
						boolean queryNonTrivial = isQueryNonTrivial(confQuery);
						final List<InputSplit> splits = new ArrayList<InputSplit>();
						
						BasicDBObject savedQuery = confQuery;
						
						Iterator<Object> itSplit = collectionOfSplits.iterator();
						BasicDBList bigSplit = null;
						while (itSplit.hasNext()) {
							BasicDBObject split = (BasicDBObject) itSplit.next();
							int docCount = (int)split.getLong(SourceHarvestStatusPojo.doccount_, 0L);
							if (docCount < nMaxCount) { // small split, will use skip/limit
								BasicDBObject modQuery = convertQuery(savedQuery, split.get(DocumentPojo.sourceKey_));
								if (null != modQuery) {

									final int SPLIT_THRESHOLD = 3;
									// A few cases:
									if ((docCount < (SPLIT_THRESHOLD*conf.getMaxDocsPerSplit())) || !queryNonTrivial) {
										splits.addAll(calculateSplits_phase2(conf, modQuery, false, shardingPolicyNew, (Integer)docCount));
									}//TESTED (based on limit, based on query)
									else {
										// My guess at the point at which you might as well as do the full query in the hope you're going
										// to save some (empty) splits
										splits.addAll(calculateSplits_phase2(conf, modQuery, false, shardingPolicyNew, null));
									}//TESTED
								}//TESTED
							}
							else { // large split, combine all these guys into an array of source keys
								if (null == bigSplit) {
									bigSplit = new BasicDBList();
								}
								bigSplit.add(split.get(DocumentPojo.sourceKey_));
									// (guaranteed to be a single element)
							}
						}//(end loop over collections)
						
						if (null != bigSplit) {
							
							// If we have a big left over community then create a set of splits for that - always chunks if query trivial
							if (1 == bigSplit.size()) {
								confQuery.put(DocumentPojo.sourceKey_, bigSplit.iterator().next());								
							}
							else {
								confQuery.put(DocumentPojo.sourceKey_, new BasicDBObject(DbManager.in_, bigSplit));
							}
							splits.addAll(calculateSplits_phase2(conf, confQuery, !queryNonTrivial, shardingPolicyNew, null));
						}//TESTED: singleton+trivial (sandy), array+trivial (sentiment/enron), array+non-trivial (sentiment/enron, docGeo), singleton+non-trivial (sandy, docGeo)

						return splits;
						
					}//TESTED: end if Cases 4a, 4b, 4c
					
				}//(end if old vs new sharding policy)
				
			}//(non-debug case)
		}//(content or metadata table are most complex)
	}

	@SuppressWarnings("unchecked")
	public static List<InputSplit> calculateSplits_phase2(InfiniteMongoConfig conf, BasicDBObject confQuery, boolean alwaysUseChunks, boolean newShardScheme, Integer splitDocCount) 
	{
		alwaysUseChunks &= (conf.getMaxSplits() != MAX_SPLITS);
			// (in standalone mode, never use chunks)
		
		MongoURI uri = conf.getInputURI();
		DBCollection coll = InfiniteMongoConfigUtil.getCollection(uri);
		if (conf.getLimit() > 0) {
			return calculateManualSplits(conf, confQuery, 1, conf.getLimit(), coll);			
		}
		else
		{
			if (!alwaysUseChunks) {
				int nMaxCount = 1 + conf.getMaxDocsPerSplit()*conf.getMaxSplits();
				int count = 0;
				if (null == splitDocCount) {
					if (nMaxCount <= 1) { 
						nMaxCount = 0;
					}
					else {
						//DEBUG
						//System.out.println(coll.find(confQuery).limit(1).explain());
						
						count = (int) coll.getCount(confQuery, null, nMaxCount, 0);
						if (0 == count) {
							return new ArrayList<InputSplit>();
						}
					}//TESTED
				}
				else {
					count = splitDocCount;
				}
				
				//if maxdocssplit and maxsplits is set and there are less documents than splits*docspersplit then use the new splitter
				//otherwise use the old splitter
				if ( conf.getMaxDocsPerSplit() > 0 && conf.getMaxSplits() > 0 && ( count < nMaxCount ) )
				{
					_logger.debug("Calculating splits manually");
					int splits_needed = (count/conf.getMaxDocsPerSplit()) + 1;
					
					return calculateManualSplits(conf, confQuery, splits_needed, conf.getMaxDocsPerSplit(), coll);
				}//TESTED
			}					
			if (newShardScheme && !confQuery.containsField(DocumentPojo.sourceKey_)) {
				// OK if we're going to do the sharded version then we will want to calculate
				splitPrecalculations_newShardScheme(confQuery, null); // (modifies confQuery if returns true)				
			}//TESTED: checked did nothing when had sourceKey, added sourceKey when necessary (eg entities.index case)
			
			if (!newShardScheme) { // unlike new sharding scheme, in this case the query is fixed, so overwrite now:
				conf.setQuery(confQuery);
			}
			
			List<InputSplit> splits = MongoSplitter.calculateSplits(conf);
				// (unless manually set, like above, runs with the _original_ query)
			int initialSplitSize  = splits.size();
			
			// We have the MongoDB-calculated splits, now calculate their intersection vs the query
			@SuppressWarnings("rawtypes")
			Map<String, TreeSet<Comparable>> orderedArraySet = new HashMap<String, TreeSet<Comparable>>();
			@SuppressWarnings("rawtypes")
			Map<String, NavigableSet<Comparable>> orderedArraySet_afterMin = new HashMap<String, NavigableSet<Comparable>>();
			BasicDBObject originalQuery = confQuery;
			
			
			ArrayList<InputSplit> newsplits = new ArrayList<InputSplit>(splits.size()); 
			Iterator<InputSplit> splitIt = splits.iterator();
			while (splitIt.hasNext()) {
				try {
					orderedArraySet_afterMin.clear();
					
					MongoInputSplit mongoSplit = (MongoInputSplit)splitIt.next();
					BasicDBObject min = (BasicDBObject) mongoSplit.getQuerySpec().get("$min");
					BasicDBObject max = (BasicDBObject) mongoSplit.getQuerySpec().get("$max");
					
					//DEBUG
					//_logger.info("+----------------- NEW SPLIT ----------------: " + min + " /" + max);
					//System.out.println("+----------------- NEW SPLIT ----------------: " + min + " /" + max);
					
					if (null != min) { // How does the min fit in with the general query
						try {
							if (compareFields(-1, originalQuery, min, max, orderedArraySet, orderedArraySet_afterMin) < 0) {
								splitIt.remove();
								continue;
							}
						}
						catch (Exception e) {} // do nothing probably just some comparable issue
					}//TESTED
					
					if (null != max) { // How does the min fit in with the general query
						try {
							if (compareFields(1, originalQuery, max, min, orderedArraySet, orderedArraySet_afterMin) > 0) {
								splitIt.remove();
								continue;
							}
						}
						catch (Exception e) {} // do nothing probably just some comparable issue
					}//TESTED
					
					//DEBUG
					//_logger.info("(retained split)");
					//System.out.println("(retained split)");
					
					// (don't worry about edge cases, won't happen very often and will just result in a spurious empty mapper)
					
					////////////////////////////////
					
					// Now some infinit.e specific processing...
					
					if (newShardScheme) {
						@SuppressWarnings("rawtypes")
						TreeSet<Comparable> sourceKeyOrderedArray = orderedArraySet.get(DocumentPojo.sourceKey_);
						if ((null != sourceKeyOrderedArray) && !sourceKeyOrderedArray.isEmpty()) {
							@SuppressWarnings("rawtypes")
							Comparable minSourceKey = null;
							Object minSourceKeyObj = (null == min) ? null : min.get(DocumentPojo.sourceKey_);
							if (minSourceKeyObj instanceof String) {
								minSourceKey = (String)minSourceKeyObj;
							}
							if (null == minSourceKey) {
								minSourceKey = sourceKeyOrderedArray.first();
							}//TESTED
							@SuppressWarnings("rawtypes")
							Comparable maxSourceKey = null;
							Object maxSourceKeyObj = (null == max) ? null : max.get(DocumentPojo.sourceKey_);
							if (maxSourceKeyObj instanceof String) {
								maxSourceKey = (String)maxSourceKeyObj;
							}
							if (null == maxSourceKey) {
								maxSourceKey = sourceKeyOrderedArray.last();
							}//TESTED
							
							DBObject splitQuery = mongoSplit.getQuerySpec();
							BasicDBObject splitQueryQuery = new BasicDBObject((BasicBSONObject) splitQuery.get("$query"));							
							if (0 == minSourceKey.compareTo(maxSourceKey)) { // single matching sourceKEy
								splitQueryQuery.put(DocumentPojo.sourceKey_, maxSourceKey);
							}//TESTED (array of sources, only one matches)
							else { // multiple matching source keys
								splitQueryQuery.put(DocumentPojo.sourceKey_, 
										new BasicDBObject(DbManager.in_, sourceKeyOrderedArray.subSet(minSourceKey, true, maxSourceKey, true)));
							}//TESTED (array of sources, multiple match)					
							newsplits.add(new InfiniteMongoInputSplit(mongoSplit, splitQueryQuery, conf.isNoTimeout()));															
						}
						else { // original query is of sufficient simplicity
							newsplits.add(new InfiniteMongoInputSplit(mongoSplit, originalQuery, conf.isNoTimeout()));							
						}//TESTED (no change to existing source)
						
					}//TESTED
					else { // old sharding scheme, remove min/max and replace with normal _id based query where possible
						
						DBObject splitQuery = mongoSplit.getQuerySpec();
						// Step 1: create a query range for _id:
						BasicDBObject idRange = null;
						Object idMin = (min == null) ? null : min.get(DocumentPojo._id_);
						Object idMax = (max == null) ? null : max.get(DocumentPojo._id_);
						if (!(idMin instanceof ObjectId))
							idMin = null;
						if (!(idMax instanceof ObjectId))
							idMax = null;
						
						if ((null != idMin) || (null != idMax)) {
							idRange = new BasicDBObject();
							if (null != idMin) {
								idRange.put(DbManager.gte_, idMin);
							}
							if (null != idMax) {
								idRange.put(DbManager.lt_, idMax);
							}
						}//TESTED						
						
						// Step 2: merge with whatever we have at the moment:
						if (null != idRange) {
							BasicDBObject splitQueryQuery = new BasicDBObject((BasicBSONObject) splitQuery.get("$query"));	
							Object idQueryElement = splitQueryQuery.get(DocumentPojo._id_);
							boolean convertedAwayFromMinMax = false;
							if (null == idQueryElement) { // nice and easy, add _id range
								splitQueryQuery.put(DocumentPojo._id_, idRange);
								convertedAwayFromMinMax = true;
							}//TESTED
							else if (! splitQueryQuery.containsField(DbManager.and_)) { // OK we're going to just going to make life easy
								splitQueryQuery.remove(DocumentPojo._id_);
								splitQueryQuery.put(DbManager.and_, Arrays.asList(
										new BasicDBObject(DocumentPojo._id_, idQueryElement),
										new BasicDBObject(DocumentPojo._id_, idRange)));
								convertedAwayFromMinMax = true;							
							}//TESTED
							// (else stick with min/max)
							
							if (convertedAwayFromMinMax) { // can construct an _id query
								splitQuery.removeField("$min");
								splitQuery.removeField("$max");
							}//TESTED
							splitQuery.put("$query", splitQueryQuery);
						}
						newsplits.add(new InfiniteMongoInputSplit(mongoSplit, conf.isNoTimeout()));
					}//TESTED			
				}
				catch (Exception e) {
					//DEBUG
					//e.printStackTrace();
				} // do nothing must be some other type of input split
			}//TESTED
			
			//DEBUG
			//System.out.println("Calculating splits via mongo-hadoop: " + initialSplitSize + " reduced to " + splits.size());

			_logger.info("Calculating (converted) splits via mongo-hadoop: " + initialSplitSize + " reduced to " + newsplits.size());
			return newsplits;
		}
	}//TESTED
	
	/**
	 * Creates numSplits amount of splits with limit items in each split
	 * using limit and skip to determine the sets
	 * 
	 * @param conf
	 * @param numSplits
	 * @param count
	 * @param coll
	 * @return
	 */
	private static List<InputSplit> calculateManualSplits(InfiniteMongoConfig conf, BasicDBObject confQuery, int numSplits, int limit, DBCollection coll)
	{
		final List<InputSplit> splits = new ArrayList<InputSplit>(numSplits);
		_logger.debug("using a limit of " + limit + " for "+numSplits+" splits");
		for ( int i = 0; i < numSplits; i++ )
		{
			splits.add(new InfiniteMongoInputSplit(conf.getInputURI(), conf.getInputKey(), confQuery, conf.getFields(), conf.getSort(), limit, i*limit, conf.isNoTimeout()));
		}
		return splits;
	}		

	///////////////////////////////////////////////////////////
	
	// UTILITY CODE
	
	// Comparison code to calculate if there is a non-zero intersection between the query and the chunk
	// Note that (eg) if you have [key:A, _id:B] as your min (/max)
	// then _id>B only applies if key==A ... if key>A then the entire _id space is allowed
		
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static int compareFields(int direction, BasicDBObject query, BasicDBObject minOrMax, BasicDBObject maxOrMin,
										Map<String, TreeSet<Comparable>> orderedArraySet, Map<String, NavigableSet<Comparable>> orderedArraySet_afterMin)
	{
		for (String field: minOrMax.keySet()) {
			//DEBUG
			//System.out.println("1] Compare: " + field + ": " + direction);
			
			try {
				Object queryOfThisField = query.get(field);
				Object minField = minOrMax.get(field);
				if ((null != queryOfThisField) && (minField instanceof Comparable)){
					int result = 0;
					Comparable comparableMinOrMaxElement = (Comparable)minField;
					if (queryOfThisField instanceof BasicDBObject) {
						result = compareComplexObject(field, direction, (BasicDBObject) queryOfThisField, comparableMinOrMaxElement, orderedArraySet, orderedArraySet_afterMin);
					}//TESTED
					else { // -1 if comparableQueryElement < comparableMinOrMaxElement 
						Comparable comparableQueryElement = (Comparable)queryOfThisField;
						result = comparableQueryElement.compareTo(comparableMinOrMaxElement);
						//DEBUG
						//System.out.println("3] Vals: " + comparableQueryElement + " vs " + comparableMinOrMaxElement + " = " + result);
					}//TESTED		
					if (result != 0) { // if we ever get a strict inequality then stop checking fields..
						if ((result == direction) || !minOrMax.equals(maxOrMin)) {
							// (fail)                 (pass but min/max keys different so not point checking any more)
							return result;  
						}//TESTED
					}
					// else equality, pass but keep checking fields 
				}
			}
			catch (Exception e) {
				//DEBUG
				//e.printStackTrace();
			} // do nothing probably some odd comparable issue
		}
		return -direction; // (ie pass by default)
	}//TESTED
	
	// returns direction to pass without checking further fields, 0 to pass but check further fields, -direction to fail immediately
	// in practice won't ever return 0 (because it's not trivial to work out exact equality with complex operators)
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static int compareComplexObject(String parentField, int direction, BasicDBObject complexQueryElement, Comparable minOrMaxElement, 
												Map<String, TreeSet<Comparable>> orderedArraySet, Map<String, NavigableSet<Comparable>> orderedArraySet_afterMin)
	{
		for (String field: complexQueryElement.keySet()) {
			//DEBUG
			//System.out.println("2] Compare operator: " + field + ", vs " + minOrMaxElement);
			
			if (field.equals(MongoDbManager.in_)) {
				
				NavigableSet<Comparable> orderedArray = null;
				if (1 == direction) { // try orderedArraySet_afterMin first...
					orderedArray = orderedArraySet_afterMin.get(parentField);
					//DEBUG
					//System.out.println("2.0] Found orderered sub-array for: " + parentField + ", size= " + orderedArray.size());
				}//TESTED
				if (null == orderedArray) { // (min, or max but min didn't set a sub-array)
					orderedArray = orderedArraySet.get(parentField);
					if (null == orderedArray) {
						// First time for this field, order the $in for easy comparison
						orderedArray = new TreeSet<Comparable>();
						Collection queryList = (Collection)complexQueryElement.get(MongoDbManager.in_);
						for (Object o: queryList) {
							Comparable c = (Comparable)o;
							orderedArray.add(c);
						}		
						//DEBUG
						//System.out.println("2.1] Created orderered array for: " + parentField + ", size= " + orderedArray.size());
						
						//DEBUG:
//						if (!orderedArray.isEmpty()) {
//							System.out.println("2.1.1] Head: " + orderedArray.iterator().next());					
//							System.out.println("2.1.2] Tail: " + orderedArray.descendingIterator().next());					
//						}
						
						orderedArraySet.put(parentField, (TreeSet<Comparable>)orderedArray); 
							// (know this cast is valid by construction)
					}//TESTED
				}				
				if (-1 == direction) { // comparing vs min
					//DEBUG
					//System.out.println("2.2] tailSet: " + orderedArray.tailSet(minOrMaxElement, true).size());
					NavigableSet<Comparable> minElements = orderedArray.tailSet(minOrMaxElement, true);
					if (minElements.isEmpty()) { // (elements >= minElement)
						return direction; // will always fail
					}
					else {
						orderedArraySet_afterMin.put(parentField, minElements);
					}//TESTED
				}//TESTED
				else if (1 == direction) { // comparing vs max
					//DEBUG
					//System.out.println("2.2] headSet: " + orderedArray.headSet(minOrMaxElement, true).size());
					
					if (orderedArray.headSet(minOrMaxElement, true).isEmpty()) { // (elements <= maxElement)
						return direction; // will always fail
					}					
				}//TESTED
			}
			else if (field.equals(MongoDbManager.gt_) || field.equals(MongoDbManager.gte_)) { // (don't worry about the boundaries, just results in spurious empty chunks)
				if (1 == direction) { // can't do anything about $gt vs min
					Comparable comparableQueryElement = (Comparable)complexQueryElement.get(field);
					//DEBUG
					//System.out.println("2.3.1] GT Vals: " + comparableQueryElement + " vs " + minOrMaxElement + " = " + comparableQueryElement.compareTo(minOrMaxElement));
					
					if (comparableQueryElement.compareTo(minOrMaxElement) > 0) // ie query _lower_ limit > chunk max 
						return direction; // ie fail
				}
			}//TESTED
			else if (field.equals(MongoDbManager.lt_) || field.equals(MongoDbManager.lte_)) { // (don't worry about the boundaries, just results in spurious empty chunks)
				if (-1 == direction) { // can't do anything about $lt vs max
					Comparable comparableQueryElement = (Comparable)complexQueryElement.get(field);
					//DEBUG
					//System.out.println("2.3.2] LT Vals: " + comparableQueryElement + " vs " + minOrMaxElement + " = " + comparableQueryElement.compareTo(minOrMaxElement));
					
					if (comparableQueryElement.compareTo(minOrMaxElement) < 0) // ie query upper limit < chunk min
						return direction; // ie fail
				}
			}//TESTED
		}
		return -direction; // (ie pass by default, don't check other fields unless they have the same min/max)
	}//TESTED (tested $in, $gte?, $lte?, $gte?/$lte? combinations)
	
	//TEST INFO:
	// shardKey = { sourceKey:1, _id: 1 }
	// FIRST TESTED  AGAINST $in 114 different keys starting with jdbc*
	// THEN query={"sourceKey": "jdbc.oracle.thin.@ec2-54-205-223-166.compute-1.amazonaws.com.152.1438"} ... left 226 chunks, hand checked
	// THEN query={"sourceKey": "jdbc.oracle.thin.@ec2-54-205-223-166.compute-1.amazonaws.com.152.1438"}, _id: { $oid: "52702a06e4b0b912ee0615f1" } ... left 1 chunk, hand checked
	// THEN query={"sourceKey": "jdbc.oracle.thin.@ec2-54-205-223-166.compute-1.amazonaws.com.152.1438"}, _id: {"$gte": {"$oid": "52702a06e4b0b912ee0615f0"}, "$lt": {"$oid":  "52753c1fe4b019e585827285"} } ...  left 3 chunks, hand checked
	// THEN query={_id: {"$gte": {"$oid": "52702a06e4b0b912ee0615f0"}, "$lt": {"$oid":  "52753c1fe4b019e585827285"} } ...  left 89 chunks, hand checked a few

	////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////

	// Util - returns a list of shards   
	
	@SuppressWarnings("unchecked")
	public static boolean splitPrecalculations_newShardScheme(BasicDBObject query, BasicDBObject srcTagsQuery) {
		// Get the communityIds from the query
		Collection<ObjectId> communityIds = null;
		try {
			BasicDBObject communityIdsIn = (BasicDBObject)query.get(DocumentPojo.communityId_);
			communityIds = (Collection<ObjectId>) communityIdsIn.get(DbManager.in_);
			if (null == communityIds) {
				return false;
			}
		}
		catch (Exception e) {
			//DEBUG
			//e.printStackTrace();
			
			return false; // back out
		}
		
		BasicDBObject keyQuery = new BasicDBObject(SourcePojo.communityIds_, new BasicDBObject(DbManager.in_, communityIds));
		BasicDBObject keyFields = new BasicDBObject(SourcePojo.key_, 1);
		keyFields.put(SourceHarvestStatusPojo.sourceQuery_doccount_, 1);

		// Get and remove the sourceKey information, incorporate into source query,
		// so it's nice and simple by the time it gets to the actual query
		Object sourceKeyQueryTerm = query.get(DocumentPojo.sourceKey_);
		
		if (null != srcTagsQuery) { // Simpler case: src tags specified, so going to get a list of all the sources regardless 
			if (null != sourceKeyQueryTerm) {
				keyQuery.put(SourcePojo.key_, sourceKeyQueryTerm);
			}
			keyQuery.put(SourcePojo.tags_, srcTagsQuery.get(SourcePojo.tags_));
		}//TESTED (including $all to test that "$srctags":{"$all": ["tagtest","db"]} matches on tags: ["tagtest","db", "tagtest2" ]
		else if (null != sourceKeyQueryTerm) {
			boolean sourceKeyQueryComplex = false;
			
			if (sourceKeyQueryTerm instanceof BasicDBObject) {
				BasicDBObject sourceKeyQueryTermDbo = (BasicDBObject) sourceKeyQueryTerm;
				if (sourceKeyQueryTermDbo.size() <= 2) { // every term must be lt/lte/gt/gte
					for (String sourceKeyQueryTermEl: sourceKeyQueryTermDbo.keySet()) {
						if (!sourceKeyQueryTermEl.equals(DbManager.in_) &&
								!sourceKeyQueryTermEl.equals(DbManager.lt_) && !sourceKeyQueryTermEl.equals(DbManager.lte_) &&
								!sourceKeyQueryTermEl.equals(DbManager.gt_) && !sourceKeyQueryTermEl.equals(DbManager.gte_))
						{
							sourceKeyQueryComplex = true;
							break;
						}//TESTED (eg ne)
						else if (sourceKeyQueryTermEl.equals(DbManager.in_) && (1 != sourceKeyQueryTermDbo.size())) {
							sourceKeyQueryComplex = true;
							break;							
						}//TESTED ((lt,in))
					}
				}//TESTED: (in, (gte,lt), ne)
				else {
					sourceKeyQueryComplex = true;					
				}//TESTED ({ "sourceKey": { "$in": ["test"], "$gt": "alex", "$lte":"test" } })
			}
			else if (sourceKeyQueryTerm instanceof java.util.regex.Pattern) { // probably a
				sourceKeyQueryComplex = true;					
			}
			//TESTED ($regex)
			
			if (sourceKeyQueryComplex) {
				keyQuery.put(SourcePojo.key_, sourceKeyQueryTerm); // ie we'll simplify it below
			}
			else {
				return false; // already have a perfectly good source key specification
			}
		}//TESTED (See combinations above)
				
		DBCursor dbc = MongoDbManager.getIngest().getSource().find(keyQuery, keyFields).sort(keyFields);
		int count = dbc.count();
		
		if (count > 5000) {
			// (too many source keys to process, just going to leave well alone... note will mean $srctags will fail open)
			return false;
		}
		else {
			ArrayList<String> sources = new ArrayList<String>(count);
			while (dbc.hasNext()) {
				BasicDBObject dbo = (BasicDBObject)dbc.next();
				String sourceKey = (String) dbo.get(SourcePojo.key_);
				sources.add(sourceKey);
			}
			if (sources.isEmpty()) {
				throw new RuntimeException(); // will just return no splits at all, no problem
			}//TESTED
			if (1 == sources.size()) {
				query.put(DocumentPojo.sourceKey_, sources.get(0));
			}//TESTED
			else {
				query.put(DocumentPojo.sourceKey_, new BasicDBObject(DbManager.in_, sources));
			}//TESTED
			
			return true;
		}		
	}//TESTED (See combinations above)
	
	// Util for creating a useful object containing source info (old sharding, _id - or new sharding but debug mode) 
	
	@SuppressWarnings("unchecked")
	public static BasicDBList splitPrecalculations_oldShardSchemeOrDebug(BasicDBObject query, BasicDBObject srcTagsQuery, int maxCountPerTask) {
		// Get the communityIds from the query
		Collection<ObjectId> communityIds = null;
		try {
			BasicDBObject communityIdsIn = (BasicDBObject)query.get(DocumentPojo.communityId_);
			communityIds = (Collection<ObjectId>) communityIdsIn.get(DbManager.in_);
			if (null == communityIds) {
				return null;
			}
		}
		catch (Exception e) {
			return null; // back out
		}
		
		BasicDBObject keyQuery = new BasicDBObject(SourcePojo.communityIds_, new BasicDBObject(DbManager.in_, communityIds));
		BasicDBObject keyFields = new BasicDBObject(SourcePojo.key_, 1);
		keyFields.put(SourceHarvestStatusPojo.sourceQuery_doccount_, 1);

		// Get and remove the sourceKey information, incorporate into source query:
		Object sourceKeyQueryTerm = query.get(DocumentPojo.sourceKey_);
		if (null != sourceKeyQueryTerm) {
			keyQuery.put(SourcePojo.key_, sourceKeyQueryTerm);
		}//TESTED
		if (null != srcTagsQuery) {
			keyQuery.put(SourcePojo.tags_, srcTagsQuery.get(SourcePojo.tags_));
		}//TESTED
		
		DBCursor dbc = MongoDbManager.getIngest().getSource().find(keyQuery, keyFields).sort(keyFields);
		if (dbc.count() > 5000) {
			// (too many source keys to process, just going to leave well alone... note this means $srctags will fail open)
			return null;
		}
		else {
			//TreeMap<String, Long> sourceKeys = new TreeMap<String, Long>();
			// Build collections of objects of format { sourceKey: string or [], totalDocs }
			BasicDBList sourceKeyListCollection = new BasicDBList();
			BasicDBList sourceKeyList = null;
			int runningDocs = 0;
			int runningSources = 0;
			while (dbc.hasNext()) {
				BasicDBObject dbo = (BasicDBObject)dbc.next();
				String sourceKey = (String) dbo.get(SourcePojo.key_);
				if (null != sourceKey) {
					long docCount = 0L;
					try {
						BasicDBObject harvestStatus = (BasicDBObject) dbo.get(SourcePojo.harvest_);
						if (null != harvestStatus) {
							docCount = harvestStatus.getLong(SourceHarvestStatusPojo.doccount_, 0L);
						}
					}
					catch (Exception e) {}
					
					//DEBUG
					//System.out.println("SOURCE=" + sourceKey + " DOC_COUNT=" + docCount + " RUNNING=" + runningDocs +"," + runningSources + ": " + sourceKeyList);
					
					if (docCount > maxCountPerTask) { // source is large enough by itself
						// Create collection
						BasicDBObject collection = new BasicDBObject();
						collection.put(DocumentPojo.sourceKey_, sourceKey);
						collection.put(SourceHarvestStatusPojo.doccount_, docCount);
						sourceKeyListCollection.add(collection);
						// (leaving running* alone, can keep building that)
					}//TESTED (by eye, system community of demo cluster)
					else if ((runningDocs + docCount) > maxCountPerTask) { // have now got a large enough collection of sources 
						if (null == sourceKeyList) {
							sourceKeyList = new BasicDBList();
						}
						sourceKeyList.add(sourceKey);
						// Create collection
						BasicDBObject collection = new BasicDBObject();
						collection.put(DocumentPojo.sourceKey_, sourceKeyList);
						collection.put(SourceHarvestStatusPojo.doccount_, runningDocs + docCount);
						sourceKeyListCollection.add(collection);			
						sourceKeyList = null;
						runningDocs = 0;
						runningSources = 0;
					}//TESTED (by eye, system community of demo cluster)
					else if (runningSources >= 15) { // have a limit on the number of sources per query, to keep the queries manageable
						sourceKeyList.add(sourceKey);
						// Create collection
						BasicDBObject collection = new BasicDBObject();
						collection.put(DocumentPojo.sourceKey_, sourceKeyList);
						collection.put(SourceHarvestStatusPojo.doccount_, runningDocs + docCount);
						sourceKeyListCollection.add(collection);			
						sourceKeyList = null;
						runningDocs = 0;
						runningSources = 0;						
					}//TESTED (by eye, system community of demo cluster)
					else { // (keep) build(ing) list
						if (null == sourceKeyList) {
							sourceKeyList = new BasicDBList();
						}
						sourceKeyList.add(sourceKey);
						runningDocs += docCount;
						runningSources++;						
					}//TESTED (by eye, system community of demo cluster)
				} //(end if has source key)
			}//(end loop over cursor)

			// Finish off:
			if (null != sourceKeyList) {				
				// Create collection
				BasicDBObject collection = new BasicDBObject();
				collection.put(DocumentPojo.sourceKey_, sourceKeyList);
				collection.put(SourceHarvestStatusPojo.doccount_, runningDocs);
				sourceKeyListCollection.add(collection);			
			}//TESTED (by eye, system community of demo cluster)
			
			if (sourceKeyListCollection.isEmpty()) { // query returns empty
				throw new RuntimeException("Communities contain no sources");
			}
			return sourceKeyListCollection;

		} // (end if too many source keys across the communities)
	}//TESTED

	// Utility - has user specified fields other than community Id, index, or sourceKey
	
	private static boolean isQueryNonTrivial(BasicDBObject query) {
		if ((query.size() > 3) || ((query.size() > 2) && !query.containsField(DocumentPojo.sourceKey_))) {
			return true;
		}
		return false;
	}//TESTED
	
	// Utility - create new query with overwritten sourceKey
	
	private static BasicDBObject convertQuery(BasicDBObject originalQuery, Object sourceObj) {
		BasicDBObject modQuery = null;
		if (null != sourceObj) {
			if (sourceObj instanceof Collection) {
				modQuery = new BasicDBObject(originalQuery.toMap());
				@SuppressWarnings("rawtypes")
				Collection sources = (Collection)sourceObj;
				modQuery.put(DocumentPojo.sourceKey_, new BasicDBObject(DbManager.in_, sources));
			}//TESTED
			else if (sourceObj instanceof String) {
				modQuery = new BasicDBObject(originalQuery.toMap());
				String source = (String)sourceObj;
				modQuery.put(DocumentPojo.sourceKey_, source);							
			}//TESTED
		}
		return modQuery;
	}//TESTED	
}
