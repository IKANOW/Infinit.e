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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;
import org.bson.BSONObject;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.ikanow.infinit.e.data_model.store.config.source.SourceFileConfigPojo;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.mongodb.BasicDBObject;

//(taken from com.ikanow.infinit.e.harvest.extraction.document.file.JsonToMetadataParser)

public class InfiniteFileInputJsonParser implements InfiniteFileInputParser {

	private HashSet<String> objectIdentifiers = new HashSet<String>();
	private HashSet<String> recursiveObjectIdentifiers = new HashSet<String>();
	private HashSet<String> fieldsThatNeedToExist = new HashSet<String>();
	private String primaryKey = null;
	private String sourceName = null;
	private boolean bRecurse = false;
	
	private JsonParser parser = null;
	private JsonReader reader = null;
	
	private JsonArray _secondaryArray = null;
	private int _posInSecondaryArray = 0;
	
	///////////////////////////////////////////////////////////////
	
	// INTERFACE CODE
	
	@Override
	public InfiniteFileInputParser initialize(InputStream inStream,
			SourceFileConfigPojo fileConfig) throws IOException {

		this.primaryKey = fileConfig.XmlPrimaryKey;
		this.sourceName = fileConfig.XmlSourceName;
		if (null != fileConfig.XmlRootLevelValues) {
			for (String objectId: fileConfig.XmlRootLevelValues) {
				if (objectId.startsWith("*")) {
					this.bRecurse = true;
					this.recursiveObjectIdentifiers.add(objectId.substring(1).toLowerCase());
					throw new RuntimeException("JSON metadata parser: Don't currently support recursive parsing.");
					//TODO (INF-2469): Not currently supported, it gets a bit tricky?
				}//TESTED
				this.objectIdentifiers.add(objectId.toLowerCase());
			}
		}
		if (null != fileConfig.XmlIgnoreValues) {
			this.fieldsThatNeedToExist.addAll(fileConfig.XmlIgnoreValues);
		}
		reader = new JsonReader(new InputStreamReader(inStream, "UTF-8"));
		reader.setLenient(true);
		parser = new JsonParser();
		
		return this;
	}

	@Override
	public BSONObject getNextRecord() throws IOException {
		if (null != _secondaryArray) {
			for (; _posInSecondaryArray < _secondaryArray.size(); ) {
				JsonElement meta2 = _secondaryArray.get(_posInSecondaryArray);
				_posInSecondaryArray++;
				
				BasicDBObject currObj = convertJsonToDocument(meta2);
				if (null != currObj) {
					return currObj;
				}
			}
			_secondaryArray = null;
		}//TESTED
		return parseDocument();
	}

	@Override
	public void close() throws IOException {
		if (null != reader) reader.close();

	}

	@Override
	public String getCanonicalExtension() {
		return ".json";
	}

	///////////////////////////////////////////////////////////////
	
	// PROCESSING CODE
	
	private boolean _inTopLevelArray = false;
	private JsonToken tok = JsonToken.BEGIN_OBJECT;
	
	public BasicDBObject parseDocument() throws IOException {
		
		// Different cases:
		// {} 
		// ^^ many of these
		// [ {}, {}, {} ]
		// For each of these 2/3 cases, you might either want to grab the entire object, or a field
		// within the object
		
		try {
			while (true) { // (use exceptions to get outta here) 

				try {
					tok = reader.peek();
				}
				catch (Exception e) {
					// EOF or end of object, keep going and find out...
					tok = reader.peek();
				}
				//TESTED
								
				if (JsonToken.BEGIN_ARRAY == tok) {
					if (!_inTopLevelArray) {
						reader.beginArray();
						_inTopLevelArray = true;
					}
					if (objectIdentifiers.isEmpty()) {
						while (reader.hasNext()) {
							JsonElement meta = parser.parse(reader);						
							BasicDBObject currObj = convertJsonToDocument(meta);
							
							if (null != currObj) {
								return currObj;
							}//(else carry on...)
						}
					}//TESTED
					else {
						while (reader.hasNext()) {
							BasicDBObject currObj = getDocumentFromJson(false);
							if (null != currObj) {
								return currObj;
							}//(else carry on...)
						}
					}//TESTED
				}
				else if (JsonToken.BEGIN_OBJECT == tok) {
					if (objectIdentifiers.isEmpty()) {
						JsonElement meta = parser.parse(reader);						
						BasicDBObject currObj = convertJsonToDocument(meta);
		
						if (null != currObj) {
							return currObj;
						}//(else carry on...)
						
					}//TESTED (single and multiple doc case)
					else {
						BasicDBObject currObj = getDocumentFromJson(false);
						if (null != currObj) {
							return currObj;							
						}//(else carry on...)
						
					}//TESTED (single and multiple doc case)	
				}
				else if ((JsonToken.END_DOCUMENT == tok) || (JsonToken.END_ARRAY == tok) || (JsonToken.END_OBJECT == tok))  {
					return null;
				}
				else { // Must be recursing through the next level(s)
					BasicDBObject currObj = getDocumentFromJson(false);
					if (null != currObj) {
						return currObj;							
					}//(else carry on...)					
				}
			} // (end loop forever - exception out)
		}
		catch (Exception e) {} // This is our EOF
		
		return null;
	}
	
	////////////////////////////

	// Look into the JSON object and find the object with the specified name
	// (for now the "path" is ignored - maybe later we allow "x.y" terminology)
	
	private boolean _inSecondaryObject = false;
	
	private BasicDBObject getDocumentFromJson(boolean bRecursing) throws IOException {
		if (!_inSecondaryObject) {
			reader.beginObject();
			_inSecondaryObject = true;
		}
		
		while (reader.hasNext()) {
			String name = reader.nextName();
			
			boolean bMatch = false;
			if (bRecursing) {
				bMatch = recursiveObjectIdentifiers.contains(name.toLowerCase());
			}
			else {
				bMatch = objectIdentifiers.contains(name.toLowerCase());				
			}//TESTED
			
			if (bMatch) {
				JsonElement meta = parser.parse(reader);
				
				if (meta.isJsonObject()) { 
					
					BasicDBObject currObj = convertJsonToDocument(meta);
					if (null != currObj) {
						return currObj;
					}
				}//TESTED
				else if (meta.isJsonArray()) {
					_secondaryArray = meta.getAsJsonArray();
					_posInSecondaryArray = 0;
					for (JsonElement meta2: _secondaryArray) { 
						_posInSecondaryArray++;
						BasicDBObject currObj = convertJsonToDocument(meta2);
						if (null != currObj) {
							return currObj;
						}
					}
					_secondaryArray = null;
				}//TESTED
				
			}//TESTED
			else {
				if (bRecurse) { //TODO (INF-2469): Not currently supported, it gets a bit tricky? (need to convert to a stack)
				
					JsonToken tok = reader.peek();
					if (JsonToken.BEGIN_OBJECT == tok) {
						BasicDBObject currObj = getDocumentFromJson(true);
						if (null != currObj) {
							return currObj;
						}
					}//TESTED
					else if (JsonToken.BEGIN_ARRAY == tok) {
						reader.beginArray();					
						while (reader.hasNext()) {
							JsonToken tok2 = reader.peek();
							
							if (JsonToken.BEGIN_OBJECT == tok2) {
								BasicDBObject currObj = getDocumentFromJson(true);
								if (null != currObj) {
									return currObj;
								}
							}
							else {
								reader.skipValue();
							}//TESTED
							
						}//TESTED		
						reader.endArray();					
					}
					else {
						reader.skipValue();
					}//TESTED
				}
				else {
					reader.skipValue();
				}//TESTED
			}
			
		}//(end loop over reader)
		
		reader.endObject();
		_inSecondaryObject = false;
		
		return null;
		
	} //TESTED

	////////////////////////////////////////////////////////////////////////////////

	// Utility - Check the object is well formed
	
	private boolean checkIfMandatoryFieldsExist(JsonElement meta) {
		if ((null != this.fieldsThatNeedToExist) && !this.fieldsThatNeedToExist.isEmpty())
		{
			boolean fieldsExist = false;
		
			for (String field: this.fieldsThatNeedToExist) {
				String exists = getKey(meta, field, false);
				if (null != exists) {
					fieldsExist = true;
					break;
				}
			}			
			return fieldsExist;
		}
		return true;
	}//TESTED
	
	/////////////////////////////////
	
	// Utility - get the primary key (does handle recursion)
	
	private String getPrimaryKey(JsonElement meta) {
		return getKey(meta, primaryKey, true);
	}
	
	/////////////////////////////////
	
	// Utility - create document set
	
	private BasicDBObject convertJsonToDocument(JsonElement meta) {
		
		// Check if all required fields exist:
		if (!checkIfMandatoryFieldsExist(meta)) {
			return null;
		}
		//TESTED
		
		// Primary key and create doc
		BasicDBObject currObj = new BasicDBObject();
		if ((null != primaryKey) && (null != sourceName)) {
			String primaryKey = getPrimaryKey(meta);
			if (null != primaryKey) {
				currObj.put(DocumentPojo.url_, sourceName + primaryKey);
			}
		}
		
		if (meta.isJsonObject()) {
			currObj.put(DocumentPojo.metadata_, new BasicDBObject("json", Arrays.asList(convertJsonObjectToBson(meta.getAsJsonObject()))));
		}
		return currObj;
		
	} //TESTED
	
	// Utility - get an arbitrary key (does handle recursion)
	
	private String getKey(JsonElement meta, String key, boolean bPrimitiveOnly) {
		try {
			String[] components = key.split("\\.");	
			JsonObject metaObj = meta.getAsJsonObject();
			for (String comp: components) {				
				meta = metaObj.get(comp);
				
				if (null == meta) {
					return null;
				}//TESTED
				else if (meta.isJsonObject()) {
					metaObj = meta.getAsJsonObject();
				}//TESTED
				else if (meta.isJsonPrimitive()) {
					return meta.getAsString();
				}//TESTED
				else if (bPrimitiveOnly) { // (meta isn't allowed to be an array, then you'd have too many primary keys!)
					return null;
				}//TOTEST (? - see JsonToMetadataParser)
				else { // Check with first instance
					JsonArray array = meta.getAsJsonArray();
					meta = array.get(0);
					if (meta.isJsonObject()) {
						metaObj = meta.getAsJsonObject();
					}
				}//TESTED				
			}
			if (!bPrimitiveOnly) { // allow objects, we just care if the field exists...
				if (null != metaObj) {
					return "[Object]";
				}
			}//TESTED
		}
		catch (Exception e) {} // no primary key
		
		return null;
	}
	//(TEST status unknown - see JsonToMetadataParser)

	/////////////////////////////////

	// Utility - conversion

	/**
	 * Converts a JsonObject to a LinkedHashMap.
	 * @param json  JSONObject to convert
	 */
	static private int capacity(int expectedSize) {
	    if (expectedSize < 3) {
	        return expectedSize + 1;
	    }
        return expectedSize + expectedSize / 3;
	}
	static public BasicDBObject convertJsonObjectToBson(JsonObject json)
	{
		return convertJsonObjectToBson(json, false);
	}
	static public BasicDBObject convertJsonObjectToBson(JsonObject json, boolean bHtmlUnescape)
	{
		int length = json.entrySet().size();
		BasicDBObject list = new BasicDBObject(capacity(length));
		for (Map.Entry<String, JsonElement> jsonKeyEl: json.entrySet())
		{
			JsonElement jsonEl = jsonKeyEl.getValue();
			if (jsonEl.isJsonArray()) {
				list.put(jsonKeyEl.getKey(), handleJsonArray(jsonEl.getAsJsonArray(), bHtmlUnescape));
			}
			else if (jsonEl.isJsonObject()) {
				list.put(jsonKeyEl.getKey(), convertJsonObjectToBson(jsonEl.getAsJsonObject(), bHtmlUnescape));				
			}
			else if (jsonEl.isJsonPrimitive()) {
				if (bHtmlUnescape) {
					list.put(jsonKeyEl.getKey(), StringEscapeUtils.unescapeHtml(jsonEl.getAsString()));					
				}
				else {
					list.put(jsonKeyEl.getKey(), jsonEl.getAsString());
				}
			}
		}
		if (list.size() > 0)
		{
			return list;
		}
		return null;
	}
	//TESTED
	
	static private Object[] handleJsonArray(JsonArray jarray, boolean bHtmlUnescape)
	{
		Object o[] = new Object[jarray.size()];
		for (int i = 0; i < jarray.size(); i++)
		{
			JsonElement jsonEl = jarray.get(i);
			if (jsonEl.isJsonObject()) {
				o[i] = convertJsonObjectToBson(jsonEl.getAsJsonObject(), bHtmlUnescape);
			}
			if (jsonEl.isJsonArray()) {
				o[i] = handleJsonArray(jsonEl.getAsJsonArray(), bHtmlUnescape);
			}
			else if (jsonEl.isJsonPrimitive()) {
				if (bHtmlUnescape) {
					o[i] = StringEscapeUtils.unescapeHtml(jsonEl.getAsString());					
				}
				else {
					o[i] = jsonEl.getAsString();
				}
			}
		}
		return o;
	}
	//TESTED
}
