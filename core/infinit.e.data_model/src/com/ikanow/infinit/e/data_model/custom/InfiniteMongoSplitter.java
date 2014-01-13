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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.log4j.Logger;

import com.ikanow.infinit.e.data_model.store.MongoDbManager;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.MongoURI;
import com.mongodb.hadoop.input.MongoInputSplit;
import com.mongodb.hadoop.util.MongoSplitter;

public class InfiniteMongoSplitter
{
	private static Logger _logger = Logger.getLogger(InfiniteMongoSplitter.class);			
	
	/**
	 * Checks if the new params MAX_SPLITS and MAX_DOCS_PER_SPLIT are set
	 * in the config.  If they are it will use those to do splits via limit/skip
	 * otherwise it will call the previous chunking splitter in MongoSplitter.
	 * 
	 * @param conf
	 * @return
	 */
	public static List<InputSplit> calculateSplits( InfiniteMongoConfig conf) 
	{
		MongoURI uri = conf.getInputURI();
		DBCollection coll = InfiniteMongoConfigUtil.getCollection(uri);
		int nMaxCount = 1 + conf.getMaxDocsPerSplit()*conf.getMaxSplits();
		int count = 0;
		if (nMaxCount <= 1) { 
			nMaxCount = 0;
		}
		else {
			count = coll.find(conf.getQuery()).limit(nMaxCount).count();
		}
		//if maxdocssplit and maxsplits is set and there are less documents than splits*docspersplit then use the new splitter
		//otherwise use the old splitter
		if (conf.getLimit() > 0) {
			return calculateManualSplits(conf, 1, conf.getLimit(), coll);			
		}
		else if ( conf.getMaxDocsPerSplit() > 0 && conf.getMaxSplits() > 0 && ( count < nMaxCount ) )
		{
			_logger.debug("Calculating splits manually");
			int splits_needed = (count/conf.getMaxDocsPerSplit()) + 1;
			return calculateManualSplits(conf, splits_needed, conf.getMaxDocsPerSplit(), coll);
		}
		else
		{
			List<InputSplit> splits = MongoSplitter.calculateSplits(conf);
			int initialSplitSize  = splits.size();
			
			// We have the MongoDB-calculated splits, now calculate their intersection vs the query
			@SuppressWarnings("rawtypes")
			Map<String, TreeSet<Comparable>> orderedArraySet = new HashMap<String, TreeSet<Comparable>>();
			BasicDBObject originalQuery = (BasicDBObject) conf.getQuery();
			
			Iterator<InputSplit> splitIt = splits.iterator();
			while (splitIt.hasNext()) {
				try {
					MongoInputSplit mongoSplit = (MongoInputSplit)splitIt.next();
					BasicDBObject min = (BasicDBObject) mongoSplit.getQuerySpec().get("$min");
					BasicDBObject max = (BasicDBObject) mongoSplit.getQuerySpec().get("$max");
					
					//DEBUG
					//_logger.info("+----------------- NEW SPLIT ----------------: " + min + " /" + max);
					//System.out.println("+----------------- NEW SPLIT ----------------: " + min + " /" + max);
					
					if (null != min) { // How does the min fit in with the general query
						try {
							if (compareFields(-1, originalQuery, min, max, orderedArraySet) < 0) {
								splitIt.remove();
								continue;
							}
						}
						catch (Exception e) {} // do nothing probably just some comparable issue
					}//TESTED
					
					if (null != max) { // How does the min fit in with the general query
						try {
							if (compareFields(1, originalQuery, max, min, orderedArraySet) > 0) {
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
				}
				catch (Exception e) {} // do nothing must be some other type of input split
			}//TESTED
			
			//DEBUG
			//System.out.println("Calculating splits via mongo-hadoop: " + initialSplitSize + " reduced to " + splits.size());
			
			_logger.info("Calculating splits via mongo-hadoop: " + initialSplitSize + " reduced to " + splits.size());
			return splits;
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
	private static List<InputSplit> calculateManualSplits(InfiniteMongoConfig conf, int numSplits, int limit, DBCollection coll)
	{
		final List<InputSplit> splits = new ArrayList<InputSplit>(numSplits);
		_logger.debug("using a limit of " + limit + " for "+numSplits+" splits");
		for ( int i = 0; i < numSplits; i++ )
		{
			splits.add(new InfiniteMongoInputSplit(conf.getInputURI(), conf.getInputKey(), conf.getQuery(), conf.getFields(), conf.getSort(), limit, i*limit, conf.isNoTimeout()));
		}
		return splits;
	}		

	///////////////////////////////////////////////////////////
	
	// UTILITY CODE
	
	// Comparison code to calculate if there is a non-zero intersection between the query and the chunk
	// Note that (eg) if you have [key:A, _id:B] as your min (/max)
	// then _id>B only applies if key==A ... if key>A then the entire _id space is allowed
		
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static int compareFields(int direction, BasicDBObject query, BasicDBObject minOrMax, BasicDBObject maxOrMin, Map<String, TreeSet<Comparable>> orderedArraySet)
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
						result = compareComplexObject(field, direction, (BasicDBObject) queryOfThisField, comparableMinOrMaxElement, orderedArraySet);
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
			catch (Exception e) {} // do nothing probably some odd comparable issue
		}
		return -direction; // (ie pass by default)
	}//TESTED
	
	// returns direction to pass without checking further fields, 0 to pass but check further fields, -direction to fail immediately
	// in practice won't ever return 0 (because it's not trivial to work out exact equality with complex operators)
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static int compareComplexObject(String parentField, int direction, BasicDBObject complexQueryElement, Comparable minOrMaxElement, Map<String, TreeSet<Comparable>> orderedArraySet)
	{
		for (String field: complexQueryElement.keySet()) {
			//DEBUG
			//System.out.println("2] Compare operator: " + field + ", vs " + minOrMaxElement);
			
			if (field.equals(MongoDbManager.in_)) {
				
				//TODO (INF-2132): (would be good to modify the query in these $in cases to make it faster)
				TreeSet<Comparable> orderedArray = orderedArraySet.get(parentField);
				if (null == orderedArray) {
					// First time for this field, order the $in for easy comparison
					orderedArray = new TreeSet<Comparable>();
					BasicDBList queryList = (BasicDBList)complexQueryElement.get(MongoDbManager.in_);
					for (Object o: queryList) {
						Comparable c = (Comparable)o;
						orderedArray.add(c);
					}					
					//DEBUG
					//System.out.println("2.1] Created orderered array for: " + parentField + ", size= " + orderedArray.size());
					
					if (!orderedArray.isEmpty()) {
						System.out.println("2.1.1] Head: " + orderedArray.iterator().next());					
						System.out.println("2.1.2] Tail: " + orderedArray.descendingIterator().next());					
					}
					orderedArraySet.put(parentField, orderedArray);
				}//TESTED
				
				if (-1 == direction) { // comparing vs min					
					//DEBUG
					//System.out.println("2.2] tailSet: " + orderedArray.tailSet(minOrMaxElement, true).size());
					
					if (orderedArray.tailSet(minOrMaxElement, true).isEmpty()) { // (elements >= minElement)
						return direction; // will always fail
					}
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
}
