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
