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

import java.util.HashSet;
import java.util.Iterator;

import org.apache.hadoop.conf.Configuration;
import org.bson.types.ObjectId;
import org.elasticsearch.cluster.metadata.IndexMetaData;

import com.ikanow.infinit.e.data_model.custom.InfiniteEsInputFormat;
import com.ikanow.infinit.e.data_model.index.ElasticSearchManager;
import com.ikanow.infinit.e.data_model.store.custom.mapreduce.CustomMapReduceJobPojo;
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
		
		//DEBUG
		//System.out.println("QUERY = " + advancedConfigurationDbo.toString());
		
		BasicDBObject newQuery = new BasicDBObject();
		Object queryObj = advancedConfigurationDbo.get("query");
		if (queryObj instanceof String) {
			config.set("es.query", queryObj.toString()); // URL version)			
		}
		else if (null != queryObj) {
			newQuery.put("query", queryObj);
			Object filterObj = advancedConfigurationDbo.get("filter");
			if (null != filterObj) newQuery.put("filter", filterObj); // (doesn't matter if it doesn't exist)
			Object fieldsObj = advancedConfigurationDbo.get("fields");
			if (null != fieldsObj) newQuery.put("fields", fieldsObj); // (doesn't matter if it doesn't exist)
			Object sizeObj = advancedConfigurationDbo.get("size");
			if (null != sizeObj) newQuery.put("size", sizeObj); // (doesn't matter if it doesn't exist)
			config.set("es.query", newQuery.toString());
		}
		//(else no query == match all)

		//COMMUNITIES
		
		StringBuffer overallIndexNames = new StringBuffer();
		for (ObjectId commId: job.communityIds) {
			StringBuffer indexNames = new StringBuffer();
			//TODO (INF-2641): need to handle
			//c) anyway to sub-query?! (look for communityIds term?!)
			//f) tmin/tmax => a) set time filter, b) restrict over indexes

			indexNames.append("recs_*").append(commId.toString());					
			
			HashSet<String> typesAdded = new HashSet<String>();
			if (null != types) {
				for (String s: types) typesAdded.add(s);
			}
			else {
				// (All this oddly written code is to minimize the number of es types that get exposed, because
				//  they are really badly behaved in terms of bw compatbility)

				ElasticSearchManager indexMgr = ElasticSearchManager.getIndex("doc_dummy"); // (index guaranteed to exist)
				Object[] indexMetaObj = indexMgr.getRawClient().admin().cluster().prepareState()
						.setIndices(indexNames.toString())
						.setRoutingTable(false).setNodes(false).setListenerThreaded(false).get().getState()
						.getMetaData().getIndices().values().toArray();
				
				if (null != indexMetaObj) for (Object oo: indexMetaObj) {
					IndexMetaData indexMeta = (IndexMetaData)oo;
					Iterator<String> typesIt = indexMeta.getMappings().keysIt();
					while (typesIt.hasNext()) {
						String type = typesIt.next();
						if (!type.equals("_default_")) {
							typesAdded.add(type);
						}
					}
				}
			}//(end if need to derive the types from the indexes) 					
			
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
