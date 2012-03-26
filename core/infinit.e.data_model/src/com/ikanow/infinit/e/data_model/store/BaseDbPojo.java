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
	package com.ikanow.infinit.e.data_model.store;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.SimpleTimeZone;

import org.bson.types.ObjectId;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import com.ikanow.infinit.e.data_model.utils.ThreadSafeSimpleDateFormat;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

public class BaseDbPojo {
	// Every Pojo should "override" this static function (stick the actual class in the 2x <>s) for readability
	static public <S> TypeToken<List<S>> listType() { return new TypeToken<List<S>>(){}; }
	
	// Override this function to perform custom serialization (see BasePojoApiMap)
	public GsonBuilder extendBuilder(GsonBuilder gp) {
		return extendBuilder_internal(gp);
	}
	// Allows API owner to enforce some custom serializations
	final public static GsonBuilder getDefaultBuilder() {
		return new GsonBuilder()
			.registerTypeAdapter(ObjectId.class, new ObjectIdDeserializer())
			.registerTypeAdapter(Date.class, new DateDeserializer())			
			.registerTypeAdapter(ObjectId.class, new ObjectIdSerializer())
			.registerTypeAdapter(Date.class, new DateSerializer());
	}
	private static GsonBuilder extendBuilder_internal(GsonBuilder gp) {
		return gp;
	}
	//________________________________________________________________________________________________
	
	// Won't normally override these (but can)
	// DB format conversion
	
	// For BaseDbPojos
	// 2 versions, 1 where you need dynamic mapping (eg runtime-specific)
	// (in both cases for single objects, you can use the nicer class<X> or 
	//  the nastier Google TypeToken, the latter has 2 advantages:
	//  a. consisntency with list types, where you have to use the TypeToken
	//  b. the class<X> doesn't work where X is generic)
	//
	// Note - the code is a bit unpleasant in places....

	/////////////////////////////////////////////////////////////////////////////////////
	// To the API JSON From a single Object
	public static<S extends BaseDbPojo> DBObject toDb(S s) {
		return toDb(s, null);
	}	
	// (Nicer version of the static "toApi")	
	public DBObject toDb() {
		return toDb(this);
	}
	public static<S extends BaseDbPojo> DBObject toDb(S s, BasePojoDbMap<S> dynamicMap) {
		GsonBuilder gb = s.extendBuilder(BaseDbPojo.getDefaultBuilder());
		if (null != dynamicMap) {
			gb = dynamicMap.extendBuilder(gb);
		}
		return (DBObject) com.mongodb.util.JSON.parse(gb.create().toJson(s));
	}
	/////////////////////////////////////////////////////////////////////////////////////
	// From the API JSON To a single Object
	public static<S extends BaseDbPojo> S fromDb(DBObject s, Class<S> type) {
		return fromDb(s, type, null);
	}
	public static<S extends BaseDbPojo> S fromDb(DBObject s, TypeToken<S> type) {
		return fromDb(s, type, null);
	}
	public static<S extends BaseDbPojo> S fromDb(DBObject s, Class<S> type, BasePojoDbMap<S> dynamicMap) {
		if (null == s) return null;
		
		// Create a new instance of the class in order to override it
		GsonBuilder gb = null;
		try {
			gb = type.newInstance().extendBuilder(BaseDbPojo.getDefaultBuilder());
		} catch (Exception e) {
			return null;
		}		
		if (null != dynamicMap) {
			gb = dynamicMap.extendBuilder(gb);
		}
		return gb.create().fromJson(MongoDbUtil.encode(s), type);
	}
	@SuppressWarnings("unchecked")
	public static<S extends BaseDbPojo> S fromDb(DBObject s, TypeToken<S> type, BasePojoDbMap<S> dynamicMap) {
		if (null == s) return null;
		
		GsonBuilder gb = null;
		try {
			Class<S> clazz = (Class<S>)type.getType();
			gb = ((S)clazz.newInstance()).extendBuilder(BaseDbPojo.getDefaultBuilder());
		} catch (Exception e) {
			return null;
		}
		if (null != dynamicMap) {
			gb = dynamicMap.extendBuilder(gb);
		}
		return (S)gb.create().fromJson(MongoDbUtil.encode(s), type.getType());
	}
	/////////////////////////////////////////////////////////////////////////////////////
	// To the API JSON From a list of objects
	public static <S extends BaseDbPojo> DBObject listToDb(Collection<S> list, TypeToken<? extends Collection<S>> listType) {
		return listToDb(list, listType, null);
	}
	public static <S extends BaseDbPojo> DBObject listToDb(Collection<S> list, TypeToken<? extends Collection<S>> listType, BasePojoDbMap<S> dynamicMap) {
		GsonBuilder gb = null;
		try {
			if (!list.isEmpty()) {
				gb = list.iterator().next().extendBuilder(BaseDbPojo.getDefaultBuilder());
			}
		} catch (Exception e) {
			return null;
		}
		return (DBObject) com.mongodb.util.JSON.parse(gb.create().toJson(list, listType.getType()));
	}
	/////////////////////////////////////////////////////////////////////////////////////
	// From the API JSON to a list of objects
	public static <S extends BaseDbPojo, L extends Collection<S>> L listFromDb(DBCursor bson, TypeToken<? extends L> listType) {
		return listFromDb(bson, listType, null);
	}
	@SuppressWarnings("unchecked")
	public static <S extends BaseDbPojo, L extends Collection<S>> L listFromDb(DBCursor bson, TypeToken<? extends L> listType, BasePojoDbMap<S> dynamicMap) {
		GsonBuilder gb = null;
		try {
			Class<S> clazz = (Class<S>)((ParameterizedType)listType.getType()).getActualTypeArguments()[0];
				// (know this works because of construction of listType)
			gb = (clazz.newInstance()).extendBuilder(BaseDbPojo.getDefaultBuilder());
		} catch (Exception e) {
			return null;
		}
		if (null != dynamicMap) {
			gb = dynamicMap.extendBuilder(gb);
		}
		return (L)gb.create().fromJson(MongoDbUtil.encode(bson), listType.getType());
	}
	
	//___________________________________________________________________
	
	// Default MongoDB serialization/deserialization rules:
	
	// 1. Object Ids
	protected static class ObjectIdSerializer implements JsonSerializer<ObjectId> 
	{
		@Override
		public JsonElement serialize(ObjectId id, Type typeOfT, JsonSerializationContext context)
		{
			JsonObject jo = new JsonObject();
			jo.addProperty("$oid", id.toStringMongod());
			return jo;
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
	// Dates:
	protected static class DateDeserializer implements JsonDeserializer<Date> 
	{
        private static ThreadSafeSimpleDateFormat _format = new ThreadSafeSimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        private static ThreadSafeSimpleDateFormat _format2 = new ThreadSafeSimpleDateFormat("MMM d, yyyy hh:mm:ss a");
		@Override
		public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
			throws JsonParseException
		{
			Date d = null;
			if (json.isJsonPrimitive()) {
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
			}
			else {			
				try {
					d = _format.parse(json.getAsJsonObject().get("$date").getAsString());
				}
				catch (Exception e)	{
					d = null;
				}
			}
			return d;
		}
	}
	protected static class DateSerializer implements JsonSerializer<Date> 
	{
        private static ThreadSafeSimpleDateFormat _format = null;
		@Override
		public JsonElement serialize(Date date, Type typeOfT, JsonSerializationContext context)
		{
			Date d = (Date)date;
			if (null == _format) {
				_format = new ThreadSafeSimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
				_format.setCalendar(new GregorianCalendar(new SimpleTimeZone(0, "UTC")));
			}
			JsonObject jo = new JsonObject();
			jo.addProperty("$date", _format.format(d));
			return jo;
		}
	}	
}
