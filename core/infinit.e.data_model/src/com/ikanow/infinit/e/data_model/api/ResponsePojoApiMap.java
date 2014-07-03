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

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

// ResponsePojo is a slightly special case as it can contain many different objects
// some of which need special serialization, and some don't

public class ResponsePojoApiMap implements BasePojoApiMap<ResponsePojo> {

	// For serialization:
	public ResponsePojoApiMap(BasePojoApiMap<?> dataMap) {
		_dataMap = dataMap;
	}
	protected BasePojoApiMap<?> _dataMap = null;
	
	// For deserialization:
	public ResponsePojoApiMap() { } 
		// (for raw access to JSON)
	
	public ResponsePojoApiMap(Class<? extends BaseApiPojo> type) {
		
		if(type.equals(ResponsePojo.class)){
			// (for raw access to JSON) 
			// Special cases: leaves the "data" array as a JsonElement to be de-serialized by the user 
			_type1 = null;
		}else{
			//catching all but ResponsePojo
			_type1 = type;
		}
	}
	public <L extends Collection<? extends BaseApiPojo>> ResponsePojoApiMap(TypeToken<L> type) {
		_listType1 = type;
	}
	public <T> ResponsePojoApiMap(Class<T> type, BasePojoApiMap<T> deserMap) {
		_type2 = type;
		_deserMap = deserMap;
	}
	public <T, L extends Collection<T>> ResponsePojoApiMap(TypeToken<L> type, BasePojoApiMap<T> deserMap) {
		_listType2 = type;
		_deserMap = deserMap;
	}
	protected Class<? extends BaseApiPojo> _type1 = null;
	protected Class<?> _type2 = null;
	protected TypeToken<? extends Collection<? extends BaseApiPojo>> _listType1 = null;
	protected TypeToken<? extends Collection<?>> _listType2 = null;
	protected BasePojoApiMap<?> _deserMap = null;
	
	@Override
	public GsonBuilder extendBuilder(GsonBuilder gp) {
		// Serialization:
		if (null != _dataMap) {
			gp = _dataMap.extendBuilder(gp);
		}
		// Deserialization:
		else if ((null != _type1) || (null != _listType1)) {
			gp = gp.registerTypeAdapter(ResponsePojo.class, new ResponsePojoDeserializer(_type1, _listType1));
		}
		else if ((null != _type2) || (null != _listType2)) {
			gp = gp.registerTypeAdapter(ResponsePojo.class, new ResponsePojoDeserializer(_type2, _listType2, _deserMap));
		}
		else {
			gp = gp.registerTypeAdapter(ResponsePojo.class, new ResponsePojoDeserializer());
		}
		return gp;
	}

	protected static class ResponsePojoDeserializer implements JsonDeserializer<ResponsePojo> 
	{
		ResponsePojoDeserializer() { }
			// (for raw access to JSON)
		
		ResponsePojoDeserializer(Class<? extends BaseApiPojo> type, TypeToken<? extends Collection<? extends BaseApiPojo>> listType) {
			_type1 = type;
			_listType1 = listType;
		}
		ResponsePojoDeserializer(Class<?> type, TypeToken<? extends Collection<?>> listType, BasePojoApiMap<?> deserMap) {
			_type2 = type;
			_listType2 = listType;
			_deserMap = deserMap;
		}
		protected Class<? extends BaseApiPojo> _type1 = null;
		protected Class<?> _type2 = null;
		protected TypeToken<? extends Collection<? extends BaseApiPojo>> _listType1 = null;
		protected TypeToken<? extends Collection<?>> _listType2 = null;
		protected BasePojoApiMap<?> _deserMap = null;
		
		@SuppressWarnings("unchecked")
		@Override
		public ResponsePojo deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
		{			
			JsonObject responseObj = json.getAsJsonObject();
			JsonElement data = responseObj.get("data");
			if (null != data) {
				responseObj.remove("data");
			}
			// Begin: all the query-only types
			
			JsonElement stats = responseObj.get("stats");
			JsonElement evtTimeline=null, facets=null, times=null, entities=null, events=null, 
				facts=null, summaries=null, sources=null, sourceMetaTags=null, sourceMetaTypes=null, moments=null, other=null;
			
			if (null != stats) { // It's a query, need to fix a bunch of other params
				evtTimeline = responseObj.get("eventsTimeline");
				responseObj.remove("eventsTimeline");
				facets = responseObj.get("facets");
				responseObj.remove("facets");
				times = responseObj.get("times");
				responseObj.remove("times");
				entities = responseObj.get("entities");
				responseObj.remove("entities");
				events = responseObj.get("events");
				responseObj.remove("events");
				facts = responseObj.get("facts");
				responseObj.remove("facts");
				summaries = responseObj.get("summaries");
				responseObj.remove("summaries");
				sources = responseObj.get("sources");
				responseObj.remove("sources");
				sourceMetaTags = responseObj.get("sourceMetatags");
				responseObj.remove("sourceMetatags");
				sourceMetaTypes = responseObj.get("sourceMetaTypes");
				responseObj.remove("sourceMetaTypes");
				moments = responseObj.get("moments");
				responseObj.remove("moments");
				other = responseObj.get("other");
				responseObj.remove("other");
			}
			
			// End: all the query-only types
			ResponsePojo rp = ResponsePojo.getDefaultBuilder().create().fromJson(json, ResponsePojo.class); 
			if (null != data) {
				try {
					if (null != _type1) { // Derived from BaseApiPojo
						rp.setData(_type1.newInstance().extendBuilder(BaseApiPojo.getDefaultBuilder()).create().fromJson(data, _type1));							
					}
					else if (null != _listType1) {
						GsonBuilder gb = null;
						Class<? extends BaseApiPojo> clazz = (Class<? extends BaseApiPojo>)((ParameterizedType)_listType1.getType()).getActualTypeArguments()[0];
							// (know this works because of construction of listType)
						gb = (clazz.newInstance()).extendBuilder(BaseApiPojo.getDefaultBuilder());
						
						rp.setData((Collection<? extends BaseApiPojo>)gb.create().fromJson(data, _listType1.getType()));
					}
					else if (null != _type2) { // Not derived from BaseApiPojo, use mapper
						if (null != _deserMap) {
							rp.setData(_deserMap.extendBuilder(BaseApiPojo.getDefaultBuilder()).create().fromJson(data, _type2), null);
						}
						else {
							rp.setData(BaseApiPojo.getDefaultBuilder().create().fromJson(data, _type1), null);						
						}
					}
					else if (null != _listType2) {
						if (null != _deserMap) {
							rp.setData(_deserMap.extendBuilder(BaseApiPojo.getDefaultBuilder()).create().fromJson(data, _listType2.getType()), null);
						}
						else {
							rp.setData(BaseApiPojo.getDefaultBuilder().create().fromJson(data, _listType2.getType()), null);						
						}
					}
					else { // Raw access
						rp.setData(data, null);						
					}
				} catch (InstantiationException e) {
					// No data for you!
				} catch (IllegalAccessException e) {
					// No data for you!
				}
			} // (if there's data)
			
			//Begin: all the other query-specific types
			rp.setEventsTimeline(evtTimeline);
			rp.setFacets(facets);
			rp.setTimes(times, rp.getTimeInterval()==null?0:rp.getTimeInterval());
			rp.setEntities(entities);
			rp.setEvents(events);
			rp.setFacts(facts);
			rp.setSummaries(summaries);
			rp.setSources(sources);
			rp.setSourceMetaTags(sourceMetaTags);
			rp.setSourceMetaTypes(sourceMetaTypes);
			rp.setMoments(moments, rp.getMomentInterval());
			rp.setOther(other);
			//End: all the other query-specific type			
			
			return rp;
		}//TESTED _type1 and _type2 and _listType1 and _listType2 (look for "rpRecreated" in TestCode for examples) 
	}
}
