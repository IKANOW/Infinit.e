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
package com.ikanow.infinit.e.data_model.api;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.bson.types.ObjectId;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import com.ikanow.infinit.e.data_model.utils.ThreadSafeSimpleDateFormat;

public class BaseApiPojo {
	// Every Pojo should "override" this static function (stick the actual class in the 2x <>s) for readability
	static public <S> TypeToken<List<S>> listType() { return new TypeToken<List<S>>(){}; }
	
	// Override this function to perform custom serialization (see BasePojoApiMap)
	public GsonBuilder extendBuilder(GsonBuilder gp) {
		return extendBuilder_internal(gp);
	}
	// Allows API owner to enforce some custom serializations
	final public static GsonBuilder getDefaultBuilder() {
		GsonBuilder gb = 
			new GsonBuilder()
				.registerTypeAdapter(ObjectId.class, new ObjectIdSerializer())
				.registerTypeAdapter(ObjectId.class, new ObjectIdDeserializer())
				.registerTypeAdapter(Date.class, new DateDeserializer())
				.registerTypeAdapter(Date.class, new DateSerializer())			
				.registerTypeAdapter(JsonArray.class, new JsonArraySerializer())
				;
		return gb;
	}
	private static GsonBuilder extendBuilder_internal(GsonBuilder gp) {
		return gp;
	}
	//________________________________________________________________________________________________
	
	// Won't normally override these (but can)
	// DB format conversion
	
	// For BaseApiPojos
	// 2 versions, 1 where you need dynamic mapping (eg runtime-specific)
	// (in both cases for single objects, you can use the nicer class<X> or 
	//  the nastier Google TypeToken, the latter has 2 advantages:
	//  a. consisntency with list types, where you have to use the TypeToken
	//  b. the class<X> doesn't work where X is generic)
	//
	// Note - the code is a bit unpleastant in places....

	/////////////////////////////////////////////////////////////////////////////////////
	// To the API JSON From a single Object
	public static<S extends BaseApiPojo> String toApi(S s) {
		return toApi(s, null);
	}	
	// (Nicer version of the static "toApi")	
	public String toApi() {
		return toApi(this);
	}
	public static<S extends BaseApiPojo> String toApi(S s, BasePojoApiMap<S> dynamicMap) {
		GsonBuilder gb = s.extendBuilder(BaseApiPojo.getDefaultBuilder());
		if (null != dynamicMap) {
			gb = dynamicMap.extendBuilder(gb);
		}
		return gb.create().toJson(s);
	}
	/////////////////////////////////////////////////////////////////////////////////////
	// From the API JSON To a single Object
	public static<S extends BaseApiPojo> S fromApi(String s, Class<S> type) {
		return fromApi(s, type, null);
	}
	public static<S extends BaseApiPojo> S fromApi(JsonElement j, Class<S> type) {
		return fromApi(j, type, null);
	}
	public static<S extends BaseApiPojo> S fromApi(String s, TypeToken<S> type) {
		return fromApi(s, type, null);
	}
	public static<S extends BaseApiPojo> S fromApi(JsonElement j, TypeToken<S> type) {
		return fromApi(j, type, null);
	}
	public static<S extends BaseApiPojo> S fromApi(String s, Class<S> type, BasePojoApiMap<S> dynamicMap) {
		// Create a new instance of the class in order to override it
		GsonBuilder gb = null;
		try {
			gb = type.newInstance().extendBuilder(BaseApiPojo.getDefaultBuilder());
		} catch (Exception e) {
			return null;
		}		
		if (null != dynamicMap) {
			gb = dynamicMap.extendBuilder(gb);
		}
		return gb.create().fromJson(s.toString(), type);
	}
	public static<S extends BaseApiPojo> S fromApi(JsonElement j, Class<S> type, BasePojoApiMap<S> dynamicMap) {
		// Create a new instance of the class in order to override it
		GsonBuilder gb = null;
		try {
			gb = type.newInstance().extendBuilder(BaseApiPojo.getDefaultBuilder());
		} catch (Exception e) {
			return null;
		}		
		if (null != dynamicMap) {
			gb = dynamicMap.extendBuilder(gb);
		}
		return gb.create().fromJson(j, type);
	}
	@SuppressWarnings("unchecked")
	public static<S extends BaseApiPojo> S fromApi(String s, TypeToken<S> type, BasePojoApiMap<S> dynamicMap) {
		GsonBuilder gb = null;
		try {
			Class<S> clazz = (Class<S>)type.getType();
			gb = ((S)clazz.newInstance()).extendBuilder(BaseApiPojo.getDefaultBuilder());
		} catch (Exception e) {
			return null;
		}
		if (null != dynamicMap) {
			gb = dynamicMap.extendBuilder(gb);
		}
		return (S)gb.create().fromJson(s.toString(), type.getType());
	}
	@SuppressWarnings("unchecked")
	public static<S extends BaseApiPojo> S fromApi(JsonElement j, TypeToken<S> type, BasePojoApiMap<S> dynamicMap) {
		GsonBuilder gb = null;
		try {
			Class<S> clazz = (Class<S>)type.getType();
			gb = ((S)clazz.newInstance()).extendBuilder(BaseApiPojo.getDefaultBuilder());
		} catch (Exception e) {
			return null;
		}
		if (null != dynamicMap) {
			gb = dynamicMap.extendBuilder(gb);
		}
		return (S)gb.create().fromJson(j, type.getType());
	}
	/////////////////////////////////////////////////////////////////////////////////////
	// To the API JSON From a list of objects
	public static <S extends BaseApiPojo> String listToApi(Collection<S> list, TypeToken<? extends Collection<S>> listType) {
		return listToApi(list, listType, null);
	}
	public static <S extends BaseApiPojo> String listToApi(Collection<S> list, TypeToken<? extends Collection<S>> listType, BasePojoApiMap<S> dynamicMap) {
		GsonBuilder gb = null;
		try {
			if (!list.isEmpty()) {
				gb = list.iterator().next().extendBuilder(BaseApiPojo.getDefaultBuilder());
			}
		} catch (Exception e) {
			return null;
		}
		return gb.create().toJson(list, listType.getType());
	}
	/////////////////////////////////////////////////////////////////////////////////////
	// From the API JSON to a list of objects
	public static <S extends BaseApiPojo, L extends Collection<S>> L listFromApi(String json, TypeToken<? extends L> listType) {
		return listFromApi(json, listType, null);
	}
	public static <S extends BaseApiPojo, L extends Collection<S>> L listFromApi(JsonElement json, TypeToken<? extends L> listType) {
		return listFromApi(json, listType, null);
	}
	@SuppressWarnings("unchecked")
	public static <S extends BaseApiPojo, L extends Collection<S>> L listFromApi(String json, TypeToken<? extends L> listType, BasePojoApiMap<S> dynamicMap) {
		GsonBuilder gb = null;
		try {
			Class<S> clazz = (Class<S>)((ParameterizedType)listType.getType()).getActualTypeArguments()[0];
				// (know this works because of construction of listType)
			gb = (clazz.newInstance()).extendBuilder(BaseApiPojo.getDefaultBuilder());
		} catch (Exception e) {
			return null;
		}
		if (null != dynamicMap) {
			gb = dynamicMap.extendBuilder(gb);
		}
		return (L)gb.create().fromJson(json, listType.getType());
	}
	@SuppressWarnings("unchecked")
	public static <S extends BaseApiPojo, L extends Collection<S>> L listFromApi(JsonElement json, TypeToken<? extends L> listType, BasePojoApiMap<S> dynamicMap) {
		GsonBuilder gb = null;
		try {
			Class<S> clazz = (Class<S>)((ParameterizedType)listType.getType()).getActualTypeArguments()[0];
				// (know this works because of construction of listType)
			gb = (clazz.newInstance()).extendBuilder(BaseApiPojo.getDefaultBuilder());
		} catch (Exception e) {
			return null;
		}
		if (null != dynamicMap) {
			gb = dynamicMap.extendBuilder(gb);
		}
		return (L)gb.create().fromJson(json, listType.getType());
	}
	//___________________________________________________________________
	
	// Default MongoDB serialization rule:	
	// 1. Object Ids
	protected static class ObjectIdSerializer implements JsonSerializer<ObjectId> 
	{
		@Override
		public JsonElement serialize(ObjectId id, Type typeOfT, JsonSerializationContext context)
		{
			return new JsonPrimitive(id.toStringMongod());
		}
	}	
	protected static class ObjectIdDeserializer implements JsonDeserializer<ObjectId> 
	{
		@Override
		public ObjectId deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
		{
			if (json.isJsonPrimitive()) {
				return new ObjectId(json.getAsString());
			}
			else {
				try {
					return new ObjectId(json.getAsJsonObject().get("$oid").getAsString());
				}
				catch (Exception e) {
					return null;
				}
			}
		}
	}
	protected static class DateDeserializer implements JsonDeserializer<Date> 
	{
        private static ThreadSafeSimpleDateFormat _format = new ThreadSafeSimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        private static ThreadSafeSimpleDateFormat _format2 = new ThreadSafeSimpleDateFormat("MMM d, yyyy hh:mm:ss a");
		@Override
		public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
			throws JsonParseException
		{
			Date d = null;
			try {
				d = _format2.parse(json.getAsString());
			}
			catch (Exception e) {
				try {
					d = _format.parse(json.getAsString());
				}
				catch (Exception e2) {
					d = null;
				}
			}
			return d;
		}
	}
	// Just convert API calls to UTC
	protected static class DateSerializer implements JsonSerializer<Date> 
	{
		@Override
		public JsonElement serialize(Date date, Type typeOfT, JsonSerializationContext context)
		{
			ThreadSafeSimpleDateFormat tsdf = new ThreadSafeSimpleDateFormat("MMM d, yyyy hh:mm:ss a 'UTC'");
			return new JsonPrimitive(tsdf.format(date));
		}
	}
	protected static class JsonArraySerializer implements JsonSerializer<JsonArray> 
	{
		@Override
		public JsonElement serialize(JsonArray jsonArray, Type typeOfT, JsonSerializationContext context)
		{
			return jsonArray;
		}
	}
}
