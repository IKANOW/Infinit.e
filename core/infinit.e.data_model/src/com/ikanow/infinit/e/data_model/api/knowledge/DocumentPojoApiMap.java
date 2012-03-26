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
package com.ikanow.infinit.e.data_model.api.knowledge;

import java.lang.reflect.Type;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.ikanow.infinit.e.data_model.api.BaseApiPojo;
import com.ikanow.infinit.e.data_model.api.BasePojoApiMap;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;

public class DocumentPojoApiMap implements BasePojoApiMap<DocumentPojo> {

	public GsonBuilder extendBuilder(GsonBuilder gp) {
		return gp.registerTypeAdapter(DocumentPojo.class, new DocumentPojoSerializer());
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
			
			// Everything else needs to be done on the JSON side:
			JsonElement je = BaseApiPojo.getDefaultBuilder().create().toJsonTree(doc, typeOfT);
			JsonObject jo = je.getAsJsonObject();
			JsonElement jetmp = null;
			// 2. Source title should be an array:
			jetmp = jo.get("source");
			if (null != jetmp) {
				JsonArray ja = new JsonArray();
				ja.add(jetmp);
				jo.add("source", ja);
			}
			// 3. Source keys should be an array:
			jetmp = jo.get("sourceKey");
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
				jo.add("sourceKey", ja);
			}
			// 4. Media types should be an array:
			jetmp = jo.get("mediaType");
			if (null != jetmp) {
				JsonArray ja = new JsonArray();
				ja.add(jetmp);
				jo.add("mediaType", ja);
			}
			// 5. Finally, CommunityId becomes an array
			jetmp = jo.get("communityId");
			if (null != jetmp) {
				JsonArray ja = new JsonArray();
				ja.add(jetmp);
				jo.add("communityId", ja);
			}
			return jo;
		}//TESTED (see DOC_API2 in TestCode)
	}
	
	// The same functionality as the above function but operates on the raw BasicDBObject
	// for performance
	
	public static void mapToApi(BasicDBObject doc) {
		// 1. (doc_index field)
		doc.remove("index");
		// 2. (source title)
		String tmp = doc.getString("source");
		if (null != tmp) {
			BasicDBList array = new BasicDBList();
			array.add(tmp);
			doc.put("source", array);
		}
		// 3. (source key)
		tmp = doc.getString("sourceKey");
		if (null != tmp) {
			int nCommunityIndex = 0;
			if (-1 != (nCommunityIndex = tmp.indexOf('#')))  {
				tmp = tmp.substring(0, nCommunityIndex);
			}
			BasicDBList array = new BasicDBList();
			array.add(tmp);
			doc.put("sourceKey", array);
		}
		// 4. (media type)
		tmp = doc.getString("mediaType");
		if (null != tmp) {
			BasicDBList array = new BasicDBList();
			array.add(tmp);
			doc.put("mediaType", array);
		}
		
	}//TESTED (see DOC_API1 in TestCode)
}
