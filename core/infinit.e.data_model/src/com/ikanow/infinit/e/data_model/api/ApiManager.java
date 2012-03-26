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
package com.ikanow.infinit.e.data_model.api;

import java.util.Collection;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;

public class ApiManager {

//________________________________________________________________________________________________
	
// For non-BaseApiPojo types (eg BaseDbTypes)
// Must include a map (but it can be null):
	
	public static<S> String mapToApi(S s, BasePojoApiMap<S> apiMap) {
		GsonBuilder gb = BaseApiPojo.getDefaultBuilder();
		if (null != apiMap) {
			gb = apiMap.extendBuilder(gb);
		}
		return gb.create().toJson(s);
	}
	public static<S> S mapFromApi(String s, Class<S> type, BasePojoApiMap<S> apiMap) {
		GsonBuilder gb = BaseApiPojo.getDefaultBuilder();
		if (null != apiMap) {
			gb = apiMap.extendBuilder(gb);
		}
		return gb.create().fromJson(s, type);
	}
	public static<S> S mapFromApi(JsonElement j, Class<S> type, BasePojoApiMap<S> apiMap) {
		GsonBuilder gb = BaseApiPojo.getDefaultBuilder();
		if (null != apiMap) {
			gb = apiMap.extendBuilder(gb);
		}
		return gb.create().fromJson(j, type);
	}
	@SuppressWarnings("unchecked")
	public static<S> S mapFromApi(String s, TypeToken<S> type, BasePojoApiMap<S> apiMap) {
		GsonBuilder gb = BaseApiPojo.getDefaultBuilder();
		if (null != apiMap) {
			gb = apiMap.extendBuilder(gb);
		}
		return (S)gb.create().fromJson(s, type.getType());
	}
	@SuppressWarnings("unchecked")
	public static<S> S mapFromApi(JsonElement j, TypeToken<S> type, BasePojoApiMap<S> apiMap) {
		GsonBuilder gb = BaseApiPojo.getDefaultBuilder();
		if (null != apiMap) {
			gb = apiMap.extendBuilder(gb);
		}
		return (S)gb.create().fromJson(j, type.getType());
	}
	public static <S> String mapListToApi(Collection<S> list, TypeToken<? extends Collection<S>> listType, BasePojoApiMap<S> apiMap) {
		GsonBuilder gb = BaseApiPojo.getDefaultBuilder();
		if (null != apiMap) {
			gb = apiMap.extendBuilder(gb);
		}
		return gb.create().toJson(list, listType.getType());
	}
	@SuppressWarnings("unchecked")
	public static <S, L extends Collection<S>> L mapListFromApi(String json,TypeToken<? extends L> listType, BasePojoApiMap<S> apiMap) {
		GsonBuilder gb = BaseApiPojo.getDefaultBuilder();
		if (null != apiMap) {
			gb = apiMap.extendBuilder(gb);
		}
		return (L)gb.create().fromJson(json, listType.getType());
	}	
	@SuppressWarnings("unchecked")
	public static <S, L extends Collection<S>> L mapListFromApi(JsonElement json,TypeToken<? extends L> listType, BasePojoApiMap<S> apiMap) {
		GsonBuilder gb = BaseApiPojo.getDefaultBuilder();
		if (null != apiMap) {
			gb = apiMap.extendBuilder(gb);
		}
		return (L)gb.create().fromJson(json, listType.getType());
	}	
}
