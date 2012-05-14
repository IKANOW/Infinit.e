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
package com.ikanow.infinit.e.data_model.index;

import java.util.Collection;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import com.ikanow.infinit.e.data_model.index.BasePojoIndexMap;

public class IndexManager extends ElasticSearchManager {
	
	//____________________________________________________________________________________
	
	// Index abstraction layer
	
	// Replaces ElasticSeachManager
	
	protected IndexManager() {}
	
	//________________________________________________________________________________________________
	
	// Format conversion
	
	// For non-BaseIndexPojo types (eg BaseDbTypes)
	// Must include a map (but it can be null):
		
	public static<S> JsonElement mapToIndex(S s, BasePojoIndexMap<S> docMap) {
		GsonBuilder gb = BaseIndexPojo.getDefaultBuilder();
		if (null != docMap) {
			gb = docMap.extendBuilder(gb);
		}
		return gb.create().toJsonTree(s);
	}
	public static<S> S mapFromIndex(String s, Class<S> type, BasePojoIndexMap<S> docMap) {
		GsonBuilder gb = BaseIndexPojo.getDefaultBuilder();
		if (null != docMap) {
			gb = docMap.extendBuilder(gb);
		}
		return gb.create().fromJson(s, type);
	}
	@SuppressWarnings("unchecked")
	public static<S> S mapFromIndex(String s, TypeToken<S> type, BasePojoIndexMap<S> docMap) {
		GsonBuilder gb = BaseIndexPojo.getDefaultBuilder();
		if (null != docMap) {
			gb = docMap.extendBuilder(gb);
		}
		return (S)gb.create().fromJson(s, type.getType());
	}
	public static <S> JsonElement mapListToIndex(Collection<S> list, TypeToken<? extends Collection<S>> listType, BasePojoIndexMap<S> docMap) {
		GsonBuilder gb = BaseIndexPojo.getDefaultBuilder();
		if (null != docMap) {
			gb = docMap.extendBuilder(gb);
		}
		return gb.create().toJsonTree(list, listType.getType());
	}
	@SuppressWarnings("unchecked")
	public static <S, L extends Collection<S>> L mapListFromIndex(String json,TypeToken<? extends L> listType, BasePojoIndexMap<S> docMap) {
		GsonBuilder gb = BaseIndexPojo.getDefaultBuilder();
		if (null != docMap) {
			gb = docMap.extendBuilder(gb);
		}
		return (L)gb.create().fromJson(json, listType.getType());
	}		
}
