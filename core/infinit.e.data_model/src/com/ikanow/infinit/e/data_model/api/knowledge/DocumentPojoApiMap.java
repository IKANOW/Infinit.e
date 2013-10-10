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
package com.ikanow.infinit.e.data_model.api.knowledge;

import java.lang.reflect.Type;
import java.util.Map.Entry;

import org.bson.types.ObjectId;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.ikanow.infinit.e.data_model.api.BaseApiPojo;
import com.ikanow.infinit.e.data_model.api.BasePojoApiMap;
import com.ikanow.infinit.e.data_model.store.MongoDbUtil;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;

public class DocumentPojoApiMap implements BasePojoApiMap<DocumentPojo> {

	public GsonBuilder extendBuilder(GsonBuilder gp) {
		return gp.registerTypeAdapter(DocumentPojo.class, new DocumentPojoSerializer())
					.registerTypeAdapter(DocumentPojo.class, new DocumentPojoDeserializer());
	}
	
	// Tidy up document pojo from DB format to API format (few slight differences, see DocumentPojo)
	
	protected static class DocumentPojoSerializer implements JsonSerializer<DocumentPojo> 
	{
		@Override
		public JsonElement serialize(DocumentPojo doc, Type typeOfT, JsonSerializationContext context)
		{
			// 1. On the document side: remove the internal index reference, other internal fields
			doc.setIndex(null);
			// (locs, months, timeRanges not stored in DB so no need to remove)
			
			// 1b. Also on the doc side: switch the update id and _id
			ObjectId updateId = doc.getUpdateId();
			if (null != updateId) {
				doc.setUpdateId(doc.getId()); // (store the old _id mostly for diagnosis)
				doc.setId(updateId); // this makes the _id field immutable across updates
			}
			
			// Everything else needs to be done on the JSON side:
			JsonElement je = BaseApiPojo.getDefaultBuilder().create().toJsonTree(doc, typeOfT);
			JsonObject jo = je.getAsJsonObject();
			JsonElement jetmp = null;
			// 2. Source title should be an array:
			jetmp = jo.get(DocumentPojo.source_);
			if (null != jetmp) {
				JsonArray ja = new JsonArray();
				ja.add(jetmp);
				jo.add(DocumentPojo.source_, ja);
			}
			// 3. Source keys should be an array:
			jetmp = jo.get(DocumentPojo.sourceKey_);
			// (also the <key>#<format> should be reduced back to <key>)
			if (null != jetmp) {
				String sourceKey = jetmp.getAsString();
				if (null != sourceKey) {
					int nCommunityIndex = 0;
					if (-1 != (nCommunityIndex = sourceKey.indexOf('#')))  {
						sourceKey = sourceKey.substring(0, nCommunityIndex);
						jetmp = new JsonPrimitive(sourceKey);
					}					
				}
				JsonArray ja = new JsonArray();
				ja.add(jetmp);
				jo.add(DocumentPojo.sourceKey_, ja);
			}
			// 4. Media types should be an array:
			jetmp = jo.get(DocumentPojo.mediaType_);
			if (null != jetmp) {
				JsonArray ja = new JsonArray();
				ja.add(jetmp);
				jo.add(DocumentPojo.mediaType_, ja);
			}
			// 5. Finally, CommunityId becomes an array
			jetmp = jo.get(DocumentPojo.communityId_);
			if (null != jetmp) {
				JsonArray ja = new JsonArray();
				ja.add(jetmp);
				jo.add(DocumentPojo.communityId_, ja);
			}
			return jo;
		}//TESTED (see DOC_API2 in TestCode)
	}
	
	// The same functionality as the above function but operates on the raw BasicDBObject
	// for performance
	
	public static void mapToApi(BasicDBObject doc) {
		// 1. (doc_index field)
		doc.remove(DocumentPojo.index_);
		// 2. (source title)
		String tmp = doc.getString(DocumentPojo.source_);
		if (null != tmp) {
			BasicDBList array = new BasicDBList();
			array.add(tmp);
			doc.put(DocumentPojo.source_, array);
		}
		// 3. (source key)
		tmp = doc.getString(DocumentPojo.sourceKey_);
		if (null != tmp) {
			int nCommunityIndex = 0;
			if (-1 != (nCommunityIndex = tmp.indexOf('#')))  {
				tmp = tmp.substring(0, nCommunityIndex);
			}
			BasicDBList array = new BasicDBList();
			array.add(tmp);
			doc.put(DocumentPojo.sourceKey_, array);
		}
		// 4. (media type)
		tmp = doc.getString(DocumentPojo.mediaType_);
		if (null != tmp) {
			BasicDBList array = new BasicDBList();
			array.add(tmp);
			doc.put(DocumentPojo.mediaType_, array);
		}
		
	}//TESTED (see DOC_API1 in TestCode)
		
	protected static class DocumentPojoDeserializer implements JsonDeserializer<DocumentPojo> 
	{
		public DocumentPojo deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
		{
			// We have converted many of the fields into arrays, we need to transform them back to single values
			// (we will have to discard the 2nd+ fields in each array - this call is only used for testing so we can live
			//  with that)
			
			JsonElement tmp = json.getAsJsonObject().get(DocumentPojo.source_);			
			if ((null != tmp) && (tmp.isJsonArray())) {				
				JsonArray tmpArray = tmp.getAsJsonArray();		
				JsonElement singleVal = tmpArray.get(0);
				json.getAsJsonObject().add(DocumentPojo.source_, singleVal);
			}
			tmp = json.getAsJsonObject().get(DocumentPojo.sourceKey_);			
			if ((null != tmp) && (tmp.isJsonArray())) {				
				JsonArray tmpArray = tmp.getAsJsonArray();		
				JsonElement singleVal = tmpArray.get(0);
				json.getAsJsonObject().add(DocumentPojo.sourceKey_, singleVal);
			}
			tmp = json.getAsJsonObject().get(DocumentPojo.mediaType_);			
			if ((null != tmp) && (tmp.isJsonArray())) {				
				JsonArray tmpArray = tmp.getAsJsonArray();		
				JsonElement singleVal = tmpArray.get(0);
				json.getAsJsonObject().add(DocumentPojo.mediaType_, singleVal);
			}
			tmp = json.getAsJsonObject().get(DocumentPojo.communityId_);			
			if ((null != tmp) && (tmp.isJsonArray())) {				
				JsonArray tmpArray = tmp.getAsJsonArray();		
				JsonElement singleVal = tmpArray.get(0);
				json.getAsJsonObject().add(DocumentPojo.communityId_, singleVal);
			}
			// Finally sort out metadata...
			tmp = json.getAsJsonObject().get(DocumentPojo.metadata_);
			if (null != tmp) {
				json.getAsJsonObject().remove(DocumentPojo.metadata_);
			}
			
			DocumentPojo doc = BaseApiPojo.getDefaultBuilder().create().fromJson(json, DocumentPojo.class);
			
			// ...And add metadata back again...
			if (null != tmp) {
				JsonObject tmpMeta = tmp.getAsJsonObject();
				for (Entry<String, JsonElement> entry: tmpMeta.entrySet()) {
					if (entry.getValue().isJsonArray()) {
						doc.addToMetadata(entry.getKey(), MongoDbUtil.encodeArray(entry.getValue().getAsJsonArray()));
					}
					else {
						BasicDBList dbl = new BasicDBList();
						dbl.add(MongoDbUtil.encodeUnknown(entry.getValue()));
						doc.addToMetadata(entry.getKey(), dbl);
					}
				}//TOTEST				
			}
			
			// Finally handle updateId/_id swap
			ObjectId updateId = doc.getUpdateId();
			if (null != updateId) {
				doc.setUpdateId(doc.getId()); // (this is now the immutable _id)
				doc.setId(updateId); // this points to the _id in the DB
			}
			
			return doc;
		}//TESTED (by hand only, no formal record)		
	}
}
