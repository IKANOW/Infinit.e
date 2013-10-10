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
package com.ikanow.infinit.e.data_model.store;

import java.text.ParseException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.bson.types.ObjectId;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.ikanow.infinit.e.data_model.utils.ThreadSafeSimpleDateFormat;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.hadoop.io.BSONWritable;

public class MongoDbUtil {

	@SuppressWarnings("unchecked")
	public static <T> T getProperty(DBObject dbo, String fieldInDotNotation) {
	    final String[] keys = fieldInDotNotation.split( "\\." );
	    DBObject current = dbo;
	    Object result = null;
	    for ( int i = 0; i < keys.length; i++ ) {
	        result = current.get( keys[i] );
	        if ( i + 1 < keys.length ) {
	            current = (DBObject) result;
	        }
	    }
	    return (T) result;		
	}//TESTED
	
    public static JsonElement encode(DBCursor cursor) {
        JsonArray result = new JsonArray();
    	while (cursor.hasNext()) {
    		DBObject dbo = cursor.next();
    		result.add(encode(dbo));
    	}
    	return result;
    }//TESTED
    public static JsonElement encode(List<DBObject> listOfObjects) {
        JsonArray result = new JsonArray();
    	for (DBObject dbo: listOfObjects) {    		
    		result.add(encode(dbo));
    	}
    	return result;
    }//TESTED
    public static JsonElement encode(BasicBSONList a) {
        JsonArray result = new JsonArray();
        for (int i = 0; i < a.size(); ++i) {
            Object o = a.get(i);
            if (o instanceof DBObject) {
                result.add(encode((DBObject)o));
            } 
            else if (o instanceof BasicBSONObject) {
                result.add(encode((BasicBSONObject)o));
            } 
            else if (o instanceof BasicBSONList) {
                result.add(encode((BasicBSONList)o));
            } 
            else if (o instanceof BasicDBList) {
                result.add(encode((BasicDBList)o));
            } 
            else { // Must be a primitive... 
            	if (o instanceof String) {
            		result.add(new JsonPrimitive((String)o));
            	}
            	else if (o instanceof Number) {
            		result.add(new JsonPrimitive((Number)o));
            	}
            	else if (o instanceof Boolean) {
            		result.add(new JsonPrimitive((Boolean)o));
            	}
            	// MongoDB special fields
            	else if (o instanceof ObjectId) {
            		JsonObject oid = new JsonObject();
            		oid.add("$oid", new JsonPrimitive(((ObjectId)o).toString()));
            		result.add(oid);
            	}
            	else if (o instanceof Date) {
            		JsonObject date = new JsonObject();
            		date.add("$date", new JsonPrimitive(_format.format((Date)o)));
            		result.add(date);            		
            	}
            	// Ignore BinaryData, should be serializing that anyway...            	
            }
        }
        return result;
    }//TESTED
    
    public static JsonElement encode(BSONObject o) {
        JsonObject result = new JsonObject();
        Iterator<?> i = o.keySet().iterator();
        while (i.hasNext()) {
            String k = (String)i.next();
            Object v = o.get(k);
            if (v instanceof BasicBSONList) {
                result.add(k, encode((BasicBSONList)v));
            } 
            else if (v instanceof BasicDBList) {
                result.add(k, encode((BasicDBList)v));
            } 
            else if (v instanceof DBObject) {
                result.add(k, encode((DBObject)v));
            } 
            else if (v instanceof BasicBSONObject) {
                result.add(k, encode((BasicBSONObject)v));
            } 
            else { // Must be a primitive... 
            	if (v instanceof String) {            		
            		result.add(k, new JsonPrimitive((String)v));
            	}
            	else if (v instanceof Number) {
            		result.add(k, new JsonPrimitive((Number)v));
            	}
            	else if (v instanceof Boolean) {
            		result.add(k, new JsonPrimitive((Boolean)v));
            	}
            	// MongoDB special fields
            	else if (v instanceof ObjectId) {
            		JsonObject oid = new JsonObject();
            		oid.add("$oid", new JsonPrimitive(((ObjectId)v).toString()));
            		result.add(k, oid);
            	}
            	else if (v instanceof Date) {
            		JsonObject date = new JsonObject();
            		date.add("$date", new JsonPrimitive(_format.format((Date)v)));
            		result.add(k, date); 
            	}
            	// Ignore BinaryData, should be serializing that anyway...            	
            }
        }
        return result;
    }//TESTED
    private static ThreadSafeSimpleDateFormat _format = new ThreadSafeSimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    public static Object encodeUnknown(JsonElement from) {
		if (from.isJsonArray()) { // Array
			return encodeArray(from.getAsJsonArray());
		}//TESTED
		else if (from.isJsonObject()) { // Object
			JsonObject obj = from.getAsJsonObject();
			// Check for OID/Date:
			if (1 == obj.entrySet().size()) {
				if (obj.has("$date")) {
					try {
						return _format.parse(obj.get("$date").getAsString());
					} catch (ParseException e) {
						return null;
					}
				}//TESTED
				else if (obj.has("$oid")) {
					return new ObjectId(obj.get("$oid").getAsString());
				}//TESTED    				
			}
			return encode(obj);
		}//TESTED
		else if (from.isJsonPrimitive()) { // Primitive
			JsonPrimitive val = from.getAsJsonPrimitive();
			if (val.isNumber()) {
				return val.getAsNumber();
			}//TESTED
			else if (val.isBoolean()) {
				return val.getAsBoolean();
			}//TESTED
			else if (val.isString()) {
				return val.getAsString();
			}//TESTED
		}//TESTED
    	return null;
    }//TESTED
    public static BasicDBList encodeArray(JsonArray a) {
    	BasicDBList dbl = new BasicDBList();
    	for (JsonElement el: a) {
    		dbl.add(encodeUnknown(el));
    	}
    	return dbl;    	
    }//TESTED
    public static BasicDBObject encode(JsonObject o) {
    	BasicDBObject dbo = new BasicDBObject();
    	for (Map.Entry<String, JsonElement> elKV: o.entrySet()) {
    		dbo.append(elKV.getKey(), encodeUnknown(elKV.getValue()));
    	}    	
    	return dbo;
    }//TESTED
	public static DBObject convert(BSONWritable dbo) {
		DBObject out = new BasicDBObject();
		for (Object entryIt: dbo.toMap().entrySet()) {
			@SuppressWarnings("unchecked")			
			Map.Entry<String, Object> entry = (Map.Entry<String, Object>)entryIt;
			out.put(entry.getKey(), entry.getValue());
		}
		return out;
	}//TESTED
	public static BSONWritable convert(BSONObject dbo) {
		BSONWritable out = new BSONWritable();
		for (Object entryIt: dbo.toMap().entrySet()) {
			@SuppressWarnings("unchecked")
			Map.Entry<String, Object> entry = (Map.Entry<String, Object>)entryIt;
			out.put(entry.getKey(), entry.getValue());
		}
		return out;
	}//TESTED
}
