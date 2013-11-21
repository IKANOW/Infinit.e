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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bson.types.ObjectId;

import com.ikanow.infinit.e.data_model.store.DbManager;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

public class MongoIndexerUtils {

	public static List<BasicDBObject> getChunks(String namespace, String description)
	{
		DBCollection configDB = DbManager.getCollection("config", "chunks");
		
		BasicDBObject query = new BasicDBObject("ns", namespace);		
		
		// Parse description:
		if (!description.equalsIgnoreCase("all")) {
			if (description.startsWith("+")) {				
				query.put("_id", new BasicDBObject(DbManager.gt_, description.substring(1)));
			}
			else { // assume it's a set of chunks
				String ids[] = description.split("\\s*,\\s*");
				query.put("_id", new BasicDBObject(DbManager.in_, ids));
			}
		}//TESTED all 3 cases
		//DEBUG
		//System.out.println("CHUNKQ=" + query.toString());
		
		// Get chunks and build queries
		DBCursor dbc = configDB.find(query);
		ArrayList<BasicDBObject> retList = new ArrayList<BasicDBObject>(dbc.count());
		while (dbc.hasNext()) {
			DBObject chunk = dbc.next();
			BasicDBObject derivedQuery = new BasicDBObject();
			BasicDBObject minObj = (BasicDBObject) chunk.get("min");
			BasicDBObject maxObj = (BasicDBObject) chunk.get("max");
			if (null != minObj) {
				addChunkModifier(derivedQuery, minObj, DbManager.gte_);
			}
			if (null != maxObj) {
				addChunkModifier(derivedQuery, maxObj, DbManager.lte_);
			}
			if (!derivedQuery.isEmpty()) {
				derivedQuery.put("$id", chunk.get("_id")); // (temp save the _id for printing in the main loop)
				retList.add(derivedQuery);
			}
		}		
		return retList;
	}//TESTED (different chunk types)

	private static void addChunkModifier(BasicDBObject derivedQuery, BasicDBObject minOrMax, String modifierType)
	{
		Iterator<String> indexFieldIt = minOrMax.keySet().iterator();
		while (indexFieldIt.hasNext()) {
			String indexField = indexFieldIt.next();
			if ((modifierType == DbManager.lte_) && (!indexFieldIt.hasNext())) { // last element, lte
				modifierType = DbManager.lt_;
			}
				
			Object fieldObj = minOrMax.get(indexField);
			if ((null != fieldObj) && // else it's a min/max so can just ignore
					(fieldObj instanceof String) || (fieldObj instanceof ObjectId) || (fieldObj instanceof Number))  
			{ 
				DBObject existingModifier = (DBObject) derivedQuery.get(indexField);
					// (must be object based on logic below)
				if (null == existingModifier) {
					derivedQuery.put(indexField, new BasicDBObject(modifierType, fieldObj));
				}
				else {
					existingModifier.put(modifierType, fieldObj);
				}
			}
		}
	}//TESTED (single object, compound index)
}
