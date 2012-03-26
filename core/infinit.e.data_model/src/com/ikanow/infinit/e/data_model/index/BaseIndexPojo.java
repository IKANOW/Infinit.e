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
package com.ikanow.infinit.e.data_model.index;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Date;

import org.bson.types.ObjectId;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

public class BaseIndexPojo {
	// Override this function to perform custom serialization (see BasePojoIndexMap)
	public GsonBuilder extendBuilder(GsonBuilder gp) {
		return extendBuilder_internal(gp);
	}
	// Allows Index owner to enforce some custom serializations
	final public static GsonBuilder getDefaultBuilder() {
		return new GsonBuilder()
			.registerTypeAdapter(ObjectId.class, new ObjectIdSerializer())
			.registerTypeAdapter(Date.class, new DateSerializer());
	}
	private static GsonBuilder extendBuilder_internal(GsonBuilder gp) {
		return gp;
	}
	//________________________________________________________________________________________________
	
	// Won't normally override these (but can)
	// DB format conversion
	
	// For BaseIndexPojos
	// 2 versions, 1 where you need dynamic mapping (eg runtime-specific)
	// (in both cases for single objects, you can use the nicer class<X> or 
	//  the nastier Google TypeToken, the latter has 2 advantages:
	//  a. consisntency with list types, where you have to use the TypeToken
	//  b. the class<X> doesn't work where X is generic)
	//
	// Note - the code is a bit unpleastant in places....

	/////////////////////////////////////////////////////////////////////////////////////
	// To the Index JSON From a single Object
	public static<S extends BaseIndexPojo> String toIndex(S s) {
		return toIndex(s, null);
	}	
	// (Nicer version of the static "toIndex")	
	public String toIndex() {
		return toIndex(this);
	}
	public static<S extends BaseIndexPojo> String toIndex(S s, BasePojoIndexMap<S> dynamicMap) {
		GsonBuilder gb = s.extendBuilder(BaseIndexPojo.getDefaultBuilder());
		if (null != dynamicMap) {
			gb = dynamicMap.extendBuilder(gb);
		}
		return gb.create().toJson(s);
	}
	/////////////////////////////////////////////////////////////////////////////////////
	// From the Index JSON To a single Object
	public static<S extends BaseIndexPojo> S fromIndex(String s, Class<S> type) {
		return fromIndex(s, type, null);
	}
	public static<S extends BaseIndexPojo> S fromIndex(String s, TypeToken<S> type) {
		return fromIndex(s, type, null);
	}
	public static<S extends BaseIndexPojo> S fromIndex(String s, Class<S> type, BasePojoIndexMap<S> dynamicMap) {
		// Create a new instance of the class in order to override it
		GsonBuilder gb = null;
		try {
			gb = type.newInstance().extendBuilder(BaseIndexPojo.getDefaultBuilder());
		} catch (Exception e) {
			return null;
		}		
		if (null != dynamicMap) {
			gb = dynamicMap.extendBuilder(gb);
		}
		return gb.create().fromJson(s.toString(), type);
	}
	@SuppressWarnings("unchecked")
	public static<S extends BaseIndexPojo> S fromIndex(String s, TypeToken<S> type, BasePojoIndexMap<S> dynamicMap) {
		GsonBuilder gb = null;
		try {
			Class<S> clazz = (Class<S>)type.getType();
			gb = ((S)clazz.newInstance()).extendBuilder(BaseIndexPojo.getDefaultBuilder());
		} catch (Exception e) {
			return null;
		}
		if (null != dynamicMap) {
			gb = dynamicMap.extendBuilder(gb);
		}
		return (S)gb.create().fromJson(s.toString(), type.getType());
	}
	/////////////////////////////////////////////////////////////////////////////////////
	// To the Index JSON From a list of objects
	public static <S extends BaseIndexPojo> String listToIndex(Collection<S> list, TypeToken<? extends Collection<S>> listType) {
		return listToIndex(list, listType, null);
	}
	public static <S extends BaseIndexPojo> String listToIndex(Collection<S> list, TypeToken<? extends Collection<S>> listType, BasePojoIndexMap<S> dynamicMap) {
		GsonBuilder gb = null;
		try {
			if (!list.isEmpty()) {
				gb = list.iterator().next().extendBuilder(BaseIndexPojo.getDefaultBuilder());
			}
		} catch (Exception e) {
			return null;
		}
		return (String) com.mongodb.util.JSON.parse(gb.create().toJson(list, listType.getType()));
	}
	/////////////////////////////////////////////////////////////////////////////////////
	// From the Index JSON to a list of objects
	public static <S extends BaseIndexPojo, L extends Collection<S>> L listFromIndex(String bson, TypeToken<? extends L> listType) {
		return listFromIndex(bson, listType, null);
	}
	@SuppressWarnings("unchecked")
	public static <S extends BaseIndexPojo, L extends Collection<S>> L listFromIndex(String bson, TypeToken<? extends L> listType, BasePojoIndexMap<S> dynamicMap) {
		GsonBuilder gb = null;
		try {
			Class<S> clazz = (Class<S>)((ParameterizedType)listType.getType()).getActualTypeArguments()[0];
				// (know this works because of construction of listType)
			gb = (clazz.newInstance()).extendBuilder(BaseIndexPojo.getDefaultBuilder());
		} catch (Exception e) {
			return null;
		}
		if (null != dynamicMap) {
			gb = dynamicMap.extendBuilder(gb);
		}
		return (L)gb.create().fromJson(bson, listType.getType());
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
	// 2. Dates
	protected static class DateSerializer implements JsonSerializer<Date> 
	{
		@Override
		public JsonElement serialize(Date date, Type typeOfT, JsonSerializationContext context)
		{
			return new JsonPrimitive(date.getTime());
		}
	}
}
