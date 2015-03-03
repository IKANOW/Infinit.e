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
package com.ikanow.infinit.e.processing.custom.utils;

import java.text.ParseException;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.hadoop.conf.Configuration;
import org.bson.types.ObjectId;
import org.elasticsearch.cluster.metadata.IndexMetaData;

import com.ikanow.infinit.e.data_model.custom.InfiniteEsInputFormat;
import com.ikanow.infinit.e.data_model.index.ElasticSearchManager;
import com.ikanow.infinit.e.data_model.store.custom.mapreduce.CustomMapReduceJobPojo;
import com.ikanow.infinit.e.data_model.utils.ThreadSafeSimpleDateFormat;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;

public class InfiniteElasticsearchHadoopUtils {

	public static void handleElasticsearchInput(CustomMapReduceJobPojo job, Configuration config, BasicDBObject advancedConfigurationDbo) {
		// Pull out type list:
		Object o = advancedConfigurationDbo.remove("$types");
		String[] types = null;
		if (null != o) {
			if (o instanceof BasicDBList) {
				types = ((BasicDBList)o).toArray(new String[0]);
			}
			else if (o instanceof String) {
				types = ((String)o).split("\\s*,\\s*"); 
			}
		}//TESTED (by hand)				
		
		//QUERY:
		
		// Date override:
		Date fromOverride = null;
		Date toOverride = null;
		Object fromOverrideObj = advancedConfigurationDbo.remove("$tmin");
		Object toOverrideObj = advancedConfigurationDbo.remove("$tmax");
		if (null != fromOverrideObj) {
			fromOverride = InfiniteHadoopUtils.dateStringFromObject(fromOverrideObj, true);
		}
		if (null != toOverrideObj) {
			toOverride = InfiniteHadoopUtils.dateStringFromObject(toOverrideObj, false);
		}	
		Boolean streaming = null;
		Object streamingObj = advancedConfigurationDbo.remove("$streaming");
		if (streamingObj instanceof Boolean) {
			streaming = (Boolean) streamingObj;
		}
		
		//DEBUG
		//System.out.println("QUERY = " + advancedConfigurationDbo.toString());
		
		BasicDBObject newQuery = new BasicDBObject();
		Object queryObj = advancedConfigurationDbo.get("query");
		if (queryObj instanceof String) {
			config.set("es.query", queryObj.toString()); // URL version)			
			if ((null != fromOverride) || (null != toOverride)) {
				throw new RuntimeException("Can't specify $tmin/$tmax shortcut in conjunction with 'URL' query type"); 				
			}//TESTED
		}
		else if (null != queryObj) {
			newQuery.put("query", queryObj);
			Object filterObj = advancedConfigurationDbo.get("filter");
			if (null != filterObj) newQuery.put("filter", filterObj); // (doesn't matter if it doesn't exist)
			Object fieldsObj = advancedConfigurationDbo.get("fields");
			if (null != fieldsObj) newQuery.put("fields", fieldsObj); // (doesn't matter if it doesn't exist)
			Object sizeObj = advancedConfigurationDbo.get("size");
			if (null != sizeObj) newQuery.put("size", sizeObj); // (doesn't matter if it doesn't exist)
			
			if ((null != fromOverride) || (null != toOverride)) {
				if (null == filterObj) {
					BasicDBObject filterRangeParamsDbo = new BasicDBObject();
					if (null != fromOverride) {
						filterRangeParamsDbo.put("gte", fromOverride.getTime());
					}
					if (null != toOverride) {
						filterRangeParamsDbo.put("lte", toOverride.getTime());
					}
					BasicDBObject filterRangeDbo = new BasicDBObject("@timestamp", filterRangeParamsDbo);
					BasicDBObject filterDbo = new BasicDBObject("range", filterRangeDbo);
					newQuery.put("filter", filterDbo);
				}
				else { // combine filter
					throw new RuntimeException("Can't (currently) specify $tmin/$tmax shortcut in conjunction with filter"); 									
				}//TESTED				
			}
			
			config.set("es.query", newQuery.toString());
		}
		//(else no query == match all)

		//COMMUNITIES
		
		Pattern dateRegex = null;
		ThreadSafeSimpleDateFormat tssdf = null;
		if ((null != fromOverride) || (null != toOverride)) {
			dateRegex = Pattern.compile("[0-9]{4}[.][0-9]{2}[.][0-9]{2}");
			tssdf = new ThreadSafeSimpleDateFormat("yyyy.MM.dd");
		}//TESTED
		
		StringBuffer overallIndexNames = new StringBuffer();
		for (ObjectId commId: job.communityIds) {
			StringBuffer indexNames = new StringBuffer();
			//TODO (INF-2641): need to handle:
			//c) anyway to sub-query?! (look for communityIds term?!)

			if (null == streaming) {
				indexNames.append("recs_*").append(commId.toString()).append("*");
			}
			else if (streaming) {
				indexNames.append("recs_t_").append(commId.toString()).append("*");				
			}
			else {// !streaming
				indexNames.append("recs_").append(commId.toString());								
			}//TESTED
			
			StringBuffer decomposedIndexes = new StringBuffer();
			boolean needDecomposedIndexes = false;
			
			HashSet<String> typesAdded = new HashSet<String>();
			if ((null != types) && (null == fromOverride) && (null == toOverride)) { // (types manual, no date filtering - can be much simpler)
				for (String s: types) typesAdded.add(s);
			}
			else {
				// (All this oddly written code is to minimize the number of es types that get exposed, because
				//  they are really badly behaved in terms of bw compatbility)

				if (null != types) {
					for (String s: types) typesAdded.add(s);					
				}
				
				ElasticSearchManager indexMgr = ElasticSearchManager.getIndex("doc_dummy"); // (index guaranteed to exist)
				Object[] indexMetaObj = indexMgr.getRawClient().admin().cluster().prepareState()
						.setIndices(indexNames.toString())
						.setRoutingTable(false).setNodes(false).setListenerThreaded(false).get().getState()
						.getMetaData().getIndices().values().toArray();
				
				if (null != indexMetaObj) for (Object oo: indexMetaObj) {
					IndexMetaData indexMeta = (IndexMetaData)oo;					
					String indexName = indexMeta.getIndex();
					
					if ((null != fromOverride) || (null != toOverride)) {						
						//DEBUG
						//System.out.println("INDEX: " + indexName);						
						
						Matcher m = dateRegex.matcher(indexName);
						if (m.find()) {
							try {
								Date d = tssdf.parse(m.group());
								long endpoint = d.getTime() + 24L*3600L*1000L - 1;
								//DEBUG
								//System.out.println("***************** COMPARE: " + d + " FROM " + fromOverride + " TO " + toOverride + "..errr . " + m.group());
								
								if (null != fromOverride) {
									if (endpoint < fromOverride.getTime()) { // no overlap on the left
										needDecomposedIndexes = true;
										continue;
									}
								}//TESTED
								if (null != toOverride) {
									if (d.getTime() > toOverride.getTime()) { // no overlap on the right
										needDecomposedIndexes = true;
										continue;
									}
								}//TESTED
								
							} catch (ParseException e) {
								// just carry on, odd index name, it happens
								needDecomposedIndexes = true;
								continue;
							}							
						}						
					}//TESTED (end loop over time checking)
					
					if (null == types) {
						Iterator<String> typesIt = indexMeta.getMappings().keysIt();
						while (typesIt.hasNext()) {
							String type = typesIt.next();
							if (!type.equals("_default_")) {
								typesAdded.add(type);
							}
						}
					}
					if (0 != decomposedIndexes.length()) {
						decomposedIndexes.append(',');
					}
					decomposedIndexes.append(indexName);
					
				}//(end loop over indexes)
			}//(end if need to derive the types from the indexes) 					
			
			if (needDecomposedIndexes) { // (because we filtered some indexes out)
				indexNames = decomposedIndexes;
			}
			if (0 == indexNames.length()) {
				continue; // nothing to do here...
			}
			
			int numTypesAdded = 0;
			if (typesAdded.isEmpty()) { // there doesn't seem to be any types associated with this set of indexes
				continue; // (ie don't add)
			}
			else for (String type: typesAdded) {
				if (numTypesAdded > 0) {
					indexNames.append(",");
				}
				else {
					indexNames.append("/");
				}
				numTypesAdded++;
				indexNames.append(type);						
			}
			
			if (overallIndexNames.length() > 0) {
				overallIndexNames.append(",,"); 
			}
			overallIndexNames.append(indexNames);
			
		}//(end loop over community)
		//TESTED (by hand)
		
		if (0 == overallIndexNames.length()) {
			throw new RuntimeException("Communities contained no types, either all indexes empty, or index is corrupt"); 
		}//TESTED (by hand)

		//DEBUG
		//System.out.println("INDEXES = " + overallIndexNames.toString());
		
		config.set("es.resource", overallIndexNames.toString());
		config.set("es.index.read.missing.as.empty", "yes");
		
		//proxy if running in debug mode:
		if (InfiniteEsInputFormat.LOCAL_DEBUG_MODE)
		{
			config.set("es.net.proxy.http.host", "localhost");
			config.set("es.net.proxy.http.port", "8888");
		}//TESTED (by hand)				
		
	}
	
}
