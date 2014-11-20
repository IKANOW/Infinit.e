package com.ikanow.infinit.e.harvest.extraction.document.file;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;

public class JsonToMetadataParser {

	private HashSet<String> objectIdentifiers = new HashSet<String>();
	private HashSet<String> recursiveObjectIdentifiers = new HashSet<String>();
	private HashSet<String> fieldsThatNeedToExist = new HashSet<String>();
	private String primaryKey = null;
	private String sourceName = null;
	private boolean bRecurse = false;
	private int nMaxDocs = Integer.MAX_VALUE;
	private int nCurrDocs = 0;
	
	// Track approximate memory usage
	private ObjectLength _memUsage = new ObjectLength();			
	public long getMemUsage() {
		return _memUsage.memory*10; // 5x for overhead, 2x for string->byte
	}
	
	public JsonToMetadataParser(String sourceName, List<String> objectIdentifiers, String primaryKey, List<String> fieldsThatNeedToExist, int nMaxDocs)
	{
		if (nMaxDocs > 0) {
			this.nMaxDocs = nMaxDocs;
		}
		this.primaryKey = primaryKey;
		this.sourceName = sourceName;
		if (null != objectIdentifiers) {
			for (String objectId: objectIdentifiers) {
				if (objectId.startsWith("*")) {
					this.bRecurse = true;
					this.recursiveObjectIdentifiers.add(objectId.substring(1).toLowerCase());
				}
				this.objectIdentifiers.add(objectId.toLowerCase());
			}
		}
		if (null != fieldsThatNeedToExist) {
			this.fieldsThatNeedToExist.addAll(fieldsThatNeedToExist);
		}
	}
	
	/**
	 * Parses XML and returns a new feed with the resulting HashMap as Metadata
	 * @param reader XMLStreamReader using Stax to avoid out of memory errors
	 * @return List of Feeds with their Metadata set
	 * @throws IOException 
	 */
	public List<DocumentPojo> parseDocument(JsonReader reader) throws IOException {
		return parseDocument(reader, false);
	}//TESTED (used by FileHarvester in this form, UAH::meta (stream) with textOnly==true below)
	
	public List<DocumentPojo> parseDocument(JsonReader reader, boolean textOnly) throws IOException {
		List<DocumentPojo> docList = new ArrayList<DocumentPojo>();
		JsonParser parser = new JsonParser();
		nCurrDocs = 0;
		_memUsage.memory = 0;
		
		// Different cases:
		// {} 
		// ^^ many of these
		// [ {}, {}, {} ]
		// For each of these 2/3 cases, you might either want to grab the entire object, or a field
		// within the object
		
		try {
			while (true) { // (use exceptions to get outta here) 

				JsonToken tok = JsonToken.BEGIN_OBJECT;
				try {
					tok = reader.peek();
				}
				catch (Exception e) {
					// EOF or end of object, keep going and find out...
					tok = reader.peek();
				}
				//TESTED
				
				if (JsonToken.BEGIN_ARRAY == tok) {
					reader.beginArray();
					if (objectIdentifiers.isEmpty()) {
						while (reader.hasNext()) {
							DocumentPojo doc = convertJsonToDocument(reader, parser, textOnly);
							if (null != doc) {
								docList.add(doc);
								if (++nCurrDocs >= nMaxDocs) {
									return docList;
								}
							}
						}
					}//TESTED
					else {
						while (reader.hasNext()) {
							getDocumentsFromJson(reader, parser, docList, false, textOnly);
						}
					}//TESTED
				}
				else if (JsonToken.BEGIN_OBJECT == tok) {
					if (objectIdentifiers.isEmpty()) {
						DocumentPojo doc = convertJsonToDocument(reader, parser, textOnly);
						if (null != doc) {
							docList.add(doc);
							if (++nCurrDocs >= nMaxDocs) {
								return docList;
							}
						}
					}//TESTED (single and multiple doc case)
					else {
						getDocumentsFromJson(reader, parser, docList, false, textOnly);
					}//TESTED (single and multiple doc case)	
				}
				else if ((JsonToken.END_DOCUMENT == tok) || (JsonToken.END_ARRAY == tok) || (JsonToken.END_OBJECT == tok))  {
					return docList;
				}
				else { // No object
					throw new RuntimeException("");
				}
			} // (end loop forever - exception out)
		}
		catch (Exception e) {} // This is our EOF
		
		return docList;
	}
	
	private DocumentPojo convertJsonToDocument(JsonReader reader, JsonParser parser, boolean textOnly) {
		
		JsonElement meta = parser.parse(reader);
		
		// Check if all required fields exist:
		if (!textOnly && !checkIfMandatoryFieldsExist(meta)) {
			return null;
		}
		//TESTED
		
		// Primary key and create doc
		DocumentPojo doc = new DocumentPojo();
		if ((null != primaryKey) && (null != sourceName)) {
			String primaryKey = getPrimaryKey(meta);
			if (null != primaryKey) {
				doc.setUrl(sourceName + primaryKey);
			}
		}
		if (textOnly) {
			doc.setFullText(meta.toString());
		}
		else {
			doc.setFullText("");
			if (meta.isJsonArray()) {
				ArrayList<Object> metaArray = new ArrayList<Object>(meta.getAsJsonArray().size());
				for (JsonElement je: meta.getAsJsonArray()) {
					if (je.isJsonObject()) {
						metaArray.add(convertJsonObjectToLinkedHashMap(je.getAsJsonObject(), _memUsage));
					}
				}
				doc.addToMetadata("json", metaArray.toArray());
			}
			else if (meta.isJsonObject()) {
				doc.addToMetadata("json", convertJsonObjectToLinkedHashMap(meta.getAsJsonObject(), _memUsage));
			}			
		}
		return doc;
		
	} //TESTED
	
	////////////////////////////

	// Look into the JSON object and find the object with the specified name
	// (for now the "path" is ignored - maybe later we allow "x.y" terminology)
	
	private void getDocumentsFromJson(JsonReader reader, JsonParser parser, List<DocumentPojo> docList, boolean bRecursing, boolean textOnly) throws IOException {		
		reader.beginObject();
		
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
				
				if (meta.isJsonObject()) { // (basically duplicates logic from convertJsonToDocument)
					if (textOnly || checkIfMandatoryFieldsExist(meta)) {
						DocumentPojo doc = new DocumentPojo();
						if ((null != primaryKey) && (null != sourceName)) {
							String primaryKey = getPrimaryKey(meta);
							if (null != primaryKey) {
								doc.setUrl(sourceName + primaryKey);
							}
						}
						if (textOnly) {
							doc.setFullText(meta.toString());
						}
						else {
							doc.addToMetadata("json", convertJsonObjectToLinkedHashMap(meta.getAsJsonObject(), _memUsage));
						}
						docList.add(doc);
						if (++nCurrDocs >= nMaxDocs) {
							while (reader.hasNext()) {
								reader.skipValue();
							}
							reader.endObject();
							return;
						}
					}
				}//TESTED
				else if (meta.isJsonArray()) {
					for (JsonElement meta2: meta.getAsJsonArray()) {
						if (textOnly || checkIfMandatoryFieldsExist(meta2)) {
							DocumentPojo doc = new DocumentPojo();
							if ((null != primaryKey) && (null != sourceName)) {
								String primaryKey = getPrimaryKey(meta2);
								if (null != primaryKey) {
									doc.setUrl(sourceName + primaryKey);
								}
							}
							if (textOnly) {
								doc.setFullText(meta2.toString());
							}
							else {
								doc.addToMetadata("json", convertJsonObjectToLinkedHashMap(meta2.getAsJsonObject(), _memUsage));
							}
							docList.add(doc);						
							if (++nCurrDocs >= nMaxDocs) {
								while (reader.hasNext()) {
									reader.skipValue();
								}
								reader.endObject();
								return;
							}
						}
					}
				}//TESTED
				
			}//TESTED
			else {
				if (bRecurse) {
				
					JsonToken tok = reader.peek();
					if (JsonToken.BEGIN_OBJECT == tok) {
						getDocumentsFromJson(reader, parser, docList, true, textOnly);
					}//TESTED
					else if (JsonToken.BEGIN_ARRAY == tok) {
						reader.beginArray();					
						while (reader.hasNext()) {
							JsonToken tok2 = reader.peek();
							
							if (JsonToken.BEGIN_OBJECT == tok2) {
								getDocumentsFromJson(reader, parser, docList, true, textOnly);							
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
		
	} //TESTED

	/////////////////////////////////

	// Check the object is well formed
	
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
	//TOTEST
	
	/////////////////////////////////
	
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
				}//TOTEST
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
	//TOTEST

	/////////////////////////////////

	// Utility - conversion

	public static class ObjectLength {
		public long memory = 0;
	}
	
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
	static public LinkedHashMap<String,Object> convertJsonObjectToLinkedHashMap(JsonObject json)
	{
		return convertJsonObjectToLinkedHashMap(json, false);
	}
	static public LinkedHashMap<String,Object> convertJsonObjectToLinkedHashMap(JsonObject json, ObjectLength size)
	{
		return convertJsonObjectToLinkedHashMap(json, false, size);
	}
	static public LinkedHashMap<String,Object> convertJsonObjectToLinkedHashMap(JsonObject json, boolean bHtmlUnescape)
	{
		return convertJsonObjectToLinkedHashMap(json, bHtmlUnescape, null);
	}
	static public LinkedHashMap<String,Object> convertJsonObjectToLinkedHashMap(JsonObject json, boolean bHtmlUnescape, ObjectLength size)
	{
		int length = json.entrySet().size();
		LinkedHashMap<String,Object> list = new LinkedHashMap<String,Object>(capacity(length));
		for (Map.Entry<String, JsonElement> jsonKeyEl: json.entrySet())
		{
			JsonElement jsonEl = jsonKeyEl.getValue();
			if (null != size) {
				size.memory += jsonKeyEl.getKey().length();
			}
			if (jsonEl.isJsonArray()) {
				list.put(jsonKeyEl.getKey(), handleJsonArray(jsonEl.getAsJsonArray(), bHtmlUnescape, size));
			}
			else if (jsonEl.isJsonObject()) {
				list.put(jsonKeyEl.getKey(), convertJsonObjectToLinkedHashMap(jsonEl.getAsJsonObject(), bHtmlUnescape, size));				
			}
			else if (jsonEl.isJsonPrimitive()) {
				String val;
				if (bHtmlUnescape) {
					val = StringEscapeUtils.unescapeHtml(jsonEl.getAsString());
				}
				else {
					val = jsonEl.getAsString();
				}
				if (null != size) {
					size.memory += val.length();
				}
				list.put(jsonKeyEl.getKey(), val);
			}
		}
		if (list.size() > 0)
		{
			return list;
		}
		return null;
	}
	//TESTED
	
	static private Object[] handleJsonArray(JsonArray jarray, boolean bHtmlUnescape, ObjectLength size)
	{
		Object o[] = new Object[jarray.size()];
		for (int i = 0; i < jarray.size(); i++)
		{
			JsonElement jsonEl = jarray.get(i);
			if (jsonEl.isJsonObject()) {
				o[i] = convertJsonObjectToLinkedHashMap(jsonEl.getAsJsonObject(), bHtmlUnescape, size);
			}
			else if (jsonEl.isJsonArray()) {
				o[i] = handleJsonArray(jsonEl.getAsJsonArray(), bHtmlUnescape, size);				
			}
			else if (jsonEl.isJsonPrimitive()) {
				if (bHtmlUnescape) {
					o[i] = StringEscapeUtils.unescapeHtml(jsonEl.getAsString());					
				}
				else {
					o[i] = jsonEl.getAsString();
				}
				if (null != size) {
					size.memory += ((String)o[i]).length();
				}
			}
		}
		return o;
	}
	//TESTED
}
