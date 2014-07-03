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

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import javax.xml.bind.annotation.XmlRootElement;

import org.elasticsearch.common.text.Text;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import com.ikanow.infinit.e.data_model.api.knowledge.GeoAggregationPojo;
import com.ikanow.infinit.e.data_model.api.knowledge.StatisticsPojo;
import com.ikanow.infinit.e.data_model.index.ElasticSearchManager;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;


// Annotations example for Jackson (used for json writing to ensure root element is proper name)
//@@JsonTypeName( value = "response" )
// Annotations example (used for xml writing to ensure root element is proper name)

@XmlRootElement( name = "response" ) 
public class ResponsePojo extends BaseApiPojo
{
	// (some grovelling to avoid class def errors in earlier versions)
	static private Integer _esVersion = null;
	static private Class<?> _esTextClass = null;
	
	@Override
	public GsonBuilder extendBuilder(GsonBuilder gb) {
		if (null == _esVersion) {
			// (some grovelling to avoid class def errors in earlier versions)
			_esVersion = ElasticSearchManager.getVersion();
		}
		if (_esVersion >= 100) {
			if (null == _esTextClass) {
				try {
					_esTextClass = Class.forName("org.elasticsearch.common.text.Text");
				} 
				catch (ClassNotFoundException e) {
					return gb;
				}
			}
			gb = gb.registerTypeAdapter(_esTextClass, new TextSerializer());
		}
		return gb;
	}
	//////////////////////////////////////////
	
	// Mapping handler (the ResponsePojo is a slightly special case because it can contain 
	// either ApiPojos or DbPojos in its "generic" data field

	public String toApi() {
		try {
			return toApi(this, _mapper);
		}
		catch (Exception e) {
			data = null;
			if (null == response) {
				response = new ResponseObject();
			}
			//DEBUG
			//e.printStackTrace();
			response.message = "Error serializing response: " + e.getMessage();
			return toApi(this, null);
		}//TESTED
	}	
	public static String toApi(ResponsePojo s) {
		try {
			return toApi(s, s.getMapper());
		}
		catch (Exception e) {
			s.setData((String)null, null);
			s.getResponse().setMessage("Error serializing response: " + e.getMessage());
			return toApi(s, s.getMapper());
		}//TESTED
	}			
	public ResponsePojoApiMap getMapper() {
		return _mapper;
	}
	private transient ResponsePojoApiMap _mapper = null;
	
	//////////////////////////////////////////
	
	// Analogous code for the fromApi case
	// (dummy just needed to differentiate from BaseApiPojo calls, though is also more consistent)
	
	// For single objects derived from BaseApiPojo 

	public static ResponsePojo fromApi(String s, Class<ResponsePojo> dummy, Class<? extends BaseApiPojo> type)
	{
		return new ResponsePojoApiMap(type).extendBuilder(ResponsePojo.getDefaultBuilder()).create().fromJson(s, ResponsePojo.class);
	}
	
	// Special cases: leaves the "data" array as a JsonElement to be de-serialized by the user
	// A quick explanation of why this is, because it's quite important:
	// 1) ResponsePojo has data as type Object (ie because it resuses it for all sorts of different objects
	// (query: List<BasicDBObject>, some share stuff: String or List<String>, other cases: XxxPojo)
	// 2) so if I just used the default fromApi GSON would break while trying to deserialize anything into an object
	// 3) as a result I "override" fromApi() so that it just copies the raw JsonElement into data
	// 4) the client code can (/must!) then use the type specific deserializer on the data (or can just access it directly as a JsonElement)

	public static<S extends BaseApiPojo> S fromApi(String s, Class<S> type) {
		return new ResponsePojoApiMap(type).extendBuilder(ResponsePojo.getDefaultBuilder()).create().fromJson(s, type);
	}
	
	// For lists of objects derived from BaseApiPojo
	
	public static <S extends BaseApiPojo, L extends Collection<S>> ResponsePojo listFromApi(String s, Class<ResponsePojo> dummy, TypeToken<L> listType)
	{
		return new ResponsePojoApiMap(listType).extendBuilder(ResponsePojo.getDefaultBuilder()).create().fromJson(s, ResponsePojo.class);
	}
	
	// For other single objects (need to specify a mapper, thought it can be null)
	
	public static <T> ResponsePojo fromApi(String s, Class<ResponsePojo> dummy, Class<T> type, BasePojoApiMap<T> mapper)
	{
		ResponsePojo rp = new ResponsePojoApiMap(type, mapper).extendBuilder(ResponsePojo.getDefaultBuilder()).create().fromJson(s, ResponsePojo.class); 
		rp._mapper = new ResponsePojoApiMap(mapper); // (ensures toApi(fromApi(x))==x)
		return rp; 
	}
	
	// For lists of objects not derived from BaseApiPojo
	
	public static <T, L extends Collection<T>> ResponsePojo listFromApi(String s, Class<ResponsePojo> dummy, TypeToken<L> listType, BasePojoApiMap<T> mapper)
	{
		ResponsePojo rp = new ResponsePojoApiMap(listType, mapper).extendBuilder(ResponsePojo.getDefaultBuilder()).create().fromJson(s, ResponsePojo.class);
		rp._mapper = new ResponsePojoApiMap(mapper); // (ensures toApi(fromApi(x))==x)
		return rp; 
	}
	
	//////////////////////////////////////////
	
	// General API response
	
	static public class ResponseObject {
		private String action = null;
		private boolean success = false;
		private String message = null;
		private long time = 0;
		
		public ResponseObject()
		{			
		}
		public ResponseObject(String _action, boolean _success, String _message)
		{
			action = _action;
			success = _success;
			message = _message;
		}
		
		public void setTime(long _time){
			this.time = _time;
		}
		public long getTime(){
			return time;
		}
		public void setAction(String action) {
			this.action = action;
		}
		public String getAction() {
			return action;
		}
		public void setSuccess(boolean success) {
			this.success = success;
		}
		public boolean isSuccess() {
			return success;
		}
		public void setMessage(String message) {
			this.message = message;
		}
		public String getMessage() {
			return message;
		}
	}
	// Status
	private ResponseObject response = null;	// (misc)
	
	// Documents or Misc return data
	private Object data = null; // (documents - list of HashMap objects)

	//////////////////////////////////////////
	
	// Query API response specific
	
	// Query Summary
	private StatisticsPojo stats = null; // (stats - beta+ no longer contains scoring info - just average score and number found (>= number returned))
	
	// Another view of documents:
	private Object eventsTimeline = null; // Standalone events - list of HashMap objects
	
	// Arbitrary facets
	private Object facets = null; // (for raw access to facets)
	
	// Temporal aggregation
	private Object times = null; // Time ranges over the entire query - list of (term,count) pairs
	private Long timeInterval = null; // (The interval range, in ms)
	
	// Geo aggreation
	private Set<GeoAggregationPojo> geo = null; // Geo heatmap - list of (lat/long,count) triples
	@SuppressWarnings("unused")
	private Integer maxGeoCount = null; // (The highest count in the geo - like time, useful shortcut)
	
	// Metadata aggregation
	private Object entities = null; // Just the entities
	private Object events = null; // Just the events (well qualified events, except facts, see below)
	private Object facts = null; // Just the facts (well qualified events that are generic relations)
	private Object summaries = null; // Just the summaries (badly qualified events)
	
	// Source aggregation
	private Object sources = null; // List of source titles
	private Object sourceMetaTags = null; // List of source tags 
	private Object sourceMetaTypes = null; // List of source types 
	
	// Moments
	private Object moments = null; // Momentum (documents aggregated over time into summaries of docs/entities/geospatial/events)
	private Long momentInterval = null; // (The moment interval)

	private Object other = null; // (in case we thing of other sutff!)
	
	//////////////////////////////////////////
	
	// Constructors
	
	public ResponsePojo()
	{
		
	}
	public ResponsePojo(ResponseObject _response)
	{
		response = _response;
	}
	public <T extends BaseApiPojo>ResponsePojo(ResponseObject response, T data)
	{
		this.setData(data);
		this.response = response;
	}
	public <T extends BaseApiPojo>ResponsePojo(ResponseObject response, Collection<T> data)
	{
		this.setData(data);
		this.response = response;
	}
	public <T> ResponsePojo(ResponseObject response, T data, BasePojoApiMap<T> apiMap)
	{
		this.setData(data, apiMap);
		this.response = response;
	}
	public <T> ResponsePojo(ResponseObject response, Collection<T> data, BasePojoApiMap<T> apiMap)
	{
		this.setData(data, apiMap);
		this.response = response;
	}
	
	//////////////////////////////////////////
	
	// Getters and setters
	
	public void setStats(StatisticsPojo stats) {
		this.stats = stats;
	}
	public StatisticsPojo getStats() {
		return stats;
	}
	
	public void setResponse(ResponseObject response) {
		this.response = response;
	}
	public ResponseObject getResponse() {
		return response;
	}
	public <T extends BaseApiPojo> void setData(T data) {
		this.data = data;
	}
	public <T extends BaseApiPojo> void setData(Collection<T> data) {
		this.data = data;
	}
	public <T> void setData(T data, BasePojoApiMap<T> apiMap) {
		if (null != apiMap) {
			_mapper = new ResponsePojoApiMap(apiMap);
		}
		this.data = data;
	}
	public <T> void setData(Collection<T> data, BasePojoApiMap<T> apiMap) {
		if (null != apiMap) {
			_mapper = new ResponsePojoApiMap(apiMap);
		}
		this.data = data;
	}
	public Object getData() {
		return data;
	}
	public void setEventsTimeline(Object events) {
		this.eventsTimeline = events;
	}
	public Object getEventsTimeline() {
		return eventsTimeline;
	}
	public void setFacets(Object facets) {
		this.facets = facets;
	}
	public Object getFacets() {
		return facets;
	}
	
	public Object getTimes() {
		return times;
	}
	public Long getTimeInterval() {
		return this.timeInterval;
	}
	public Object getGeo() {
		return geo;
	}
	public Object getEntities() {
		return entities;
	}
	public Object getEvents() {
		return events;
	}
	public Object getFacts() {
		return facts;
	}
	public Object getSummaries() {
		return summaries;
	}
	public Object getMoments() {
		return moments;
	}
	public Long getMomentInterval() {
		return momentInterval;
	}
	public void setTimes(Object times, long interval) {
		this.times = times;
		this.timeInterval = interval;
	}
	public void setGeo(Set<GeoAggregationPojo> geo, int nMaxCount, int nMinCount) {
		this.geo = geo;
		this.maxGeoCount = nMaxCount;
	}
	public void setEntities(Object entities) {
		this.entities = entities;
	}
	public void setEvents(Object events) {
		this.events = events;
	}
	public void setFacts(Object facts) {
		this.facts = facts;
	}
	public void setSummaries(Object summaries) {
		this.summaries = summaries;
	}
	public void setMoments(Object moments, Long interval) {
		this.moments = moments;
		this.momentInterval = interval;
	}
	public Object getSources() {
		return sources;
	}
	public Object getSourceMetaTags() {
		return sourceMetaTags;
	}
	public Object getSourceMetaTypes() {
		return sourceMetaTypes;
	}
	public void setSources(Object sources) {
		this.sources = sources;
	}
	public void setSourceMetaTags(Object sourceMetaTags) {
		this.sourceMetaTags = sourceMetaTags;
	}
	public void setSourceMetaTypes(Object sourceMetaTypes) {
		this.sourceMetaTypes = sourceMetaTypes;
	}
	public Object getOther() {
		return other;
	}
	public void setOther(Object other) {
		this.other = other;
	}
	
	//////////////////////// FROM DB CODE
	
	public static ResponsePojo fromDb(BasicDBObject bson) {
		BasicDBObject bson2 = new BasicDBObject();
		bson2.put("stats", bson.get("stats"));
		bson2.put("response", bson.get("response"));
		ResponsePojo rp = ResponsePojo.fromApi(bson2.toString(), ResponsePojo.class);
		
		// Now all the elements!
		Object evtTimeline=null, facets=null, times=null, entities=null, events=null, 
				facts=null, summaries=null, sources=null, sourceMetaTags=null, sourceMetaTypes=null, moments=null, other=null;
			
		evtTimeline = bson.get("eventsTimeline");
		facets = bson.get("facets");
		times = bson.get("times");
		entities = bson.get("entities");
		events = bson.get("events");
		facts = bson.get("facts");
		summaries = bson.get("summaries");
		sources = bson.get("sources");
		sourceMetaTags = bson.get("sourceMetatags");
		sourceMetaTypes = bson.get("sourceMetaTypes");
		moments = bson.get("moments");
		other = bson.get("other");
		
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
		
		// The main data object is discarded in the original fromApi() call, so put it back now
		Object docData = bson.get("data");
		if (null != docData) {
			rp.setData((BasicDBList)docData, (BasePojoApiMap<BasicDBList>)null);			
		}
		else { // (ensure there's always an empty list)
			rp.setData(new ArrayList<BasicDBObject>(0), (BasePojoApiMap<BasicDBObject>)null);
		}		
		return rp;
	}
	
	///////////////////////////////////////////////
	
	// Serializer to handle facets which use Strings (0.19-) or Text (1.0)
	// (Don't do this in the BaseApiPojo since that can be called from Hadoop which
	//  doesn't know internally about elasticsearch - yet!)
	
	// Just convert elasticsearch text objects to strings
	
	protected static class TextSerializer implements JsonSerializer<Text> 
	{
		@Override
		public JsonElement serialize(Text text, Type typeOfT, JsonSerializationContext context)
		{
			return new JsonPrimitive(text.toString());
		}
	}
}
