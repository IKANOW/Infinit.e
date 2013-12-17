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
			else if (description.startsWith("@")) { // it's a list of replica sets
				String replicas[] = description.substring(1).split("\\s*,\\s*");
				query.put("shard", new BasicDBObject(DbManager.in_, replicas));
			}
			else { // assume it's a set of chunks (allow for hacky replacement because "s don't seem to get loaded from windows?)
				String ids[] = description.replace("^QUOTE^", "\"").split("\\s*,\\s*");
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
				BasicDBObject modifier = addChunkModifier(minObj);
				if (null != modifier)
					derivedQuery.put(DbManager.min_, modifier);
			}
			if (null != maxObj) {
				BasicDBObject modifier = addChunkModifier(maxObj);
				if (null != modifier)
					derivedQuery.put(DbManager.max_, modifier);
			}
			if (!derivedQuery.isEmpty()) {
				derivedQuery.put("$id", chunk.get("_id")); // (temp save the _id for printing in the main loop)
				retList.add(derivedQuery);
			}
		}		
		return retList;
	}//TESTED (_id, index, {sourceKey:1,_id:1}

	private static BasicDBObject addChunkModifier(BasicDBObject minOrMax)
	{
		BasicDBObject modifier = new BasicDBObject();
		Iterator<String> indexFieldIt = minOrMax.keySet().iterator();
		while (indexFieldIt.hasNext()) {
			String indexField = indexFieldIt.next();
				
			Object fieldObj = minOrMax.get(indexField);
			if ((null != fieldObj) && // else it's a min/max so can just ignore
					(fieldObj instanceof String) || (fieldObj instanceof ObjectId) || (fieldObj instanceof Number))  
			{ 
				modifier.put(indexField, fieldObj);
			}
		}
		if (modifier.isEmpty())
			return null;
		
		return modifier;
	}//TESTED (single/compound indexes)
}
