package com.ikanow.infinit.e.data_model.api;

import java.util.Collection;
import java.util.Set;

import javax.xml.bind.annotation.XmlRootElement;

import com.google.gson.reflect.TypeToken;
import com.ikanow.infinit.e.data_model.api.knowledge.GeoAggregationPojo;
import com.ikanow.infinit.e.data_model.api.knowledge.StatisticsPojo;


// Annotations example for Jackson (used for json writing to ensure root element is proper name)
//@@JsonTypeName( value = "response" )
// Annotations example (used for xml writing to ensure root element is proper name)

@XmlRootElement( name = "response" ) 
public class ResponsePojo extends BaseApiPojo
{
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
	
	// Special cases: leaves the "data" array as a JsonElement to be de-serialized by the user 

	public static ResponsePojo fromApi(String s, Class<ResponsePojo> dummy)
	{
		return new ResponsePojoApiMap().extendBuilder(ResponsePojo.getDefaultBuilder()).create().fromJson(s, ResponsePojo.class);
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
	private String momentInterval = null; // (The moment interval)

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
	public String getMomentInterval() {
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
	public void setMoments(Object moments, String interval) {
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
}
