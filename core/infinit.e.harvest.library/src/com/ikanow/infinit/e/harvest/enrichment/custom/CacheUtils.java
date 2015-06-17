package com.ikanow.infinit.e.harvest.enrichment.custom;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import javax.script.ScriptException;

import org.bson.types.ObjectId;
import org.json.JSONException;
import org.json.JSONObject;

import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.MongoDbManager;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.ikanow.infinit.e.data_model.store.custom.mapreduce.CustomMapReduceJobPojo;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.infinit.e.data_model.store.social.sharing.SharePojo;
import com.ikanow.infinit.e.harvest.HarvestContext;
import com.ikanow.infinit.e.harvest.enrichment.script.CompiledScriptFactory;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

//TODO for custom, map should contain weak references?

public class CacheUtils 
{
	/**
	 * Creates a default map of caches and then tries to grab a share for each id given and create a 
	 * cache with that name.
	 * 
	 * @param caches
	 * @param engine
	 * @param communityIds
	 * @param _context2
	 * @throws ScriptException
	 * @throws JSONException 
	 */	
	public static List<String> addJSONCachesToEngine(Map<String, ObjectId> caches, CompiledScriptFactory factory, Set<ObjectId> communityIds, ObjectId sourceOwnerId, HarvestContext _context2) throws ScriptException, JSONException 
	{
		boolean testMode = _context2.isStandalone();
		LinkedList<String> errors = new LinkedList<String>();
		
		if (factory != null)
		{
			factory.executeCompiledScript(JavaScriptUtils.cacheEmptyScript);
			factory.executeCompiledScript(JavaScriptUtils.customEmptyScript);
			//get json from shares
			for ( String cacheName : caches.keySet())
			{
				ObjectId shareId = caches.get(cacheName);
				String json = getShareFromDB(shareId, communityIds);
				
				if (null == json) { // not a share, maybe it's a custom job?
					try {
						createCustomCache(factory, cacheName, shareId.toString(), sourceOwnerId, testMode);
					}
					catch (Exception e) {
						try {
							createSourceCache(factory, cacheName, shareId.toString(), communityIds, testMode);
						}
						catch (Exception e2) {
							errors.add(e.getMessage());
							errors.add(e2.getMessage());
						}
					}
				}
				else { // is a share
					JSONObject jsonObj = new JSONObject(json);
					factory.executeCompiledScript(JavaScriptUtils.putCacheFunction,"tmpcache", jsonObj,"cacheName",cacheName);
				}		
			}
		}
		return errors;
		
	}//TESTED
	
	/**
	 * Trys to grab the json from a share using the communityIds as auth
	 * Returns the String json or null
	 * 
	 * @param shareId
	 * @param communityIds
	 * @return
	 */
	private static String getShareFromDB(ObjectId shareId, Set<ObjectId> communityIds) 
	{
		BasicDBObject query = new BasicDBObject(SharePojo._id_, shareId);
		query.put("communities._id",new BasicDBObject( MongoDbManager.in_,communityIds.toArray()));
		DBObject dbo = DbManager.getSocial().getShare().findOne(query);
		if ( dbo != null )
		{
			SharePojo share = SharePojo.fromDb(dbo,SharePojo.class);
			return share.getShare();
		}	
		return null;
	}
	
	/**
	 * Code for managing larger caches (represented as custom objects)
	 * @throws ScriptException 
	 */

	public static synchronized void createCustomCache(CompiledScriptFactory factory, String jobAlias, String jobNameOrShareId, ObjectId submitterId, boolean testMode) throws ScriptException {		
		if (null == _customCache) {
			_customCache = new HashMap<String, CustomCacheInJavascript>();
		}
		
		// 1) Check authentication
		
		String jobName = null;
		ObjectId jobId = null;
		CustomMapReduceJobPojo customJob = null;
		BasicDBObject query = new BasicDBObject(); // unlike shares and sources - we only use the source submitter's id here
		
		try {
			jobId = new ObjectId(jobNameOrShareId);
			query.put(CustomMapReduceJobPojo._id_, jobId);
			customJob = CustomMapReduceJobPojo.fromDb(
					MongoDbManager.getCustom().getLookup().findOne(query), 
						CustomMapReduceJobPojo.class);
			
			//DEBUG
			//System.out.println("?? " + customJob + " ... " + query);
		}
		catch (Exception e) {
			// it's a job name
			jobName = jobNameOrShareId;
			query.put(CustomMapReduceJobPojo.jobtitle_, jobName);
			customJob = CustomMapReduceJobPojo.fromDb(
					MongoDbManager.getCustom().getLookup().findOne(query), 
						CustomMapReduceJobPojo.class);				
		} 
		if (null == customJob) {
			throw new RuntimeException("Authentication failure or no matching custom job: " + query);
		}
		// OK we found the job, need to check against the submitter's id though
		if (!checkOwnerPermissions(submitterId, customJob.communityIds)) {
			throw new RuntimeException("Source owner does not have sufficient privileges to access this lookup table: " + submitterId + " : " + customJob.communityIds);
		}
		
		jobName = customJob.jobtitle;
		jobId = customJob._id;
		
		DBCollection cacheCollection = MongoDbManager.getCollection(customJob.getOutputDatabase(), customJob.outputCollection);
		
		// 2) Do we already have this cache?
		
		CustomCacheInJavascript cacheElement = _customCache.get(jobNameOrShareId);
		
		if (testMode) {
			_customCache.remove(jobNameOrShareId);
			cacheElement = null;				
		}//TESTED
		
		if (null != cacheElement) {
			// Cache has changed:
			if (cacheElement.getCollection().equals(customJob.outputCollection)) {
				_customCache.remove(jobNameOrShareId);
				cacheElement = null;
			}
		}//TOTEST
		if (null == cacheElement) {
			// 3) Check key is indexed
			cacheCollection.createIndex(new BasicDBObject("key", 1));
			
			// 4) Check key is Text or single value BSON (and what is that value?)
			String keyField = null;
			if (null == customJob.outputKey) {
				throw new RuntimeException("Invalid key: " + customJob.outputKey);
			}
			if (customJob.outputKey.equalsIgnoreCase("com.mongodb.hadoop.io.BSONWritable")) {
				// Just going to use the first element in the key
				BasicDBObject record = (BasicDBObject) cacheCollection.findOne();
				if (null != record) {
					BasicDBObject key = (BasicDBObject) record.get("key");
					if (1 == key.size()) {
						keyField = key.entrySet().iterator().next().getKey();
					}
					else {
						throw new RuntimeException("Invalid key size, too complex, eg: " + key);					
					}//TOTEST
				}
			}//TESTED (apart from too complex case TOTEST)
			else if (!customJob.outputKey.equalsIgnoreCase("org.apache.hadoop.io.Text")) {
				throw new RuntimeException("Invalid key: " + customJob.outputKey);					
			}//TOTEST
			cacheElement = new CustomCacheInJavascript(cacheCollection, keyField);
		}		

		// 5) Add object to js
		
		if (null != cacheElement) {
			_customCache.put(jobNameOrShareId, cacheElement);
			factory.executeCompiledScript(JavaScriptUtils.putCustomFunction,"cachewrapper",  new CustomCacheInJavascriptWrapper(cacheElement, factory),"jobAlias",jobAlias);				
		}//TESTED
	}
	/**
	 * Code for managing larger caches (represented as communities)
	 * @throws ScriptException 
	 */
	public static synchronized void createSourceCache(CompiledScriptFactory factory, String jobAlias, String sourceKeyOrId, Set<ObjectId> communityIds, boolean testMode) throws ScriptException
	{
		if (null == _customCache) {
			_customCache = new HashMap<String, CustomCacheInJavascript>();
		}
		
		// 1) Check authentication
		
		ObjectId sourceId = null;
		SourcePojo source = null;
		BasicDBObject query = new BasicDBObject(SourcePojo.communityIds_, 
													new BasicDBObject(DbManager.in_, communityIds));
		try {
			sourceId = new ObjectId(sourceKeyOrId);
			query.put(SourcePojo._id_, sourceId);
			source = SourcePojo.fromDb(
					MongoDbManager.getIngest().getSource().findOne(query), 
					SourcePojo.class);
		}
		catch (Exception e) {
			// it's a job name
			String sourceKey = sourceKeyOrId;
			query.put(SourcePojo.key_, sourceKey);
			source = SourcePojo.fromDb(
					MongoDbManager.getIngest().getSource().findOne(query), 
					SourcePojo.class);
		} 
		if (null == source) {
			throw new RuntimeException("Authentication failure or no matching source job: " + query);
		}
		sourceId = source.getId();
		
		DBCollection cacheCollection = DbManager.getDocument().getMetadata();
		
		// 2) Do we already have this cache?
		
		CustomCacheInJavascript cacheElement = _customCache.get(sourceKeyOrId);

		if (testMode) {
			_customCache.remove(sourceKeyOrId);
			cacheElement = null;				
		}//TESTED
		
		if (null == cacheElement) {				
			cacheElement = new CustomCacheInJavascript(cacheCollection, DocumentPojo.url_);
			cacheElement.setBaseQuery(new BasicDBObject(DocumentPojo.sourceKey_, source.getDistributedKeyQueryTerm()));
		}//TESTED (distributed and non-distributed cases)

		// 3) Add object to js
		
		if (null != cacheElement) {
			_customCache.put(sourceKeyOrId, cacheElement);
			factory.executeCompiledScript(JavaScriptUtils.putCustomFunction,"cachewrapper",  new CustomCacheInJavascriptWrapper(cacheElement, factory),"jobAlias",jobAlias);				
			
		}//TESTED
	}//TESTED (except where noted)


	// Authentication utility
	
	private static boolean checkOwnerPermissions(ObjectId submitterId, Collection<ObjectId> customCommunities) {
		BasicDBObject query = new BasicDBObject("_id", submitterId);
		query.put("communities._id", new BasicDBObject(DbManager.all_, customCommunities));
		BasicDBObject fields = new BasicDBObject();
		boolean found = (null != DbManager.getSocial().getPerson().findOne(query, fields));
		
		return found;
	}//TESTED (by hand)
	
	/**
	 * Utility code for larger caches
	 */
	
	// first map is jobName/objectid, second map is the key -> object 
	// (the object will be a native JS object, ie opaque on the Java side)
	private static HashMap<String, CustomCacheInJavascript> _customCache = null;
	
	private static final Integer _NOT_FOUND_PLACEHOLDER = 0;
	
	public static class CustomCacheInJavascript {
		private WeakHashMap<String, Object> _cacheElement; 
		private DBCollection _cacheCollection;
		private String _keyField;
		private BasicDBObject _baseQuery = null;
		
		String getCollection() {
			return _cacheCollection.getName();
		}
		void setBaseQuery(BasicDBObject baseQuery) {
			_baseQuery = baseQuery;
		}
		CustomCacheInJavascript(DBCollection cacheCollection, String keyField) {
			_cacheElement = new WeakHashMap<String, Object>();
			_cacheCollection = cacheCollection;
			_keyField = keyField;
		}
		public synchronized Object get(String key, CompiledScriptFactory factory) {
			Object returnVal = _cacheElement.get(key);
			
			if (returnVal instanceof Integer) {
				return null;
			}//TESTED
			else if (null != returnVal) {
				// (used to directly return the cached NativeObject but that was intermittently failing - see below)
				factory.executeCompiledScript(JavaScriptUtils.tmpCacheScript,"tmpcache", returnVal);

				return factory.getScriptContext().getAttribute("tmpcache");
			}
			else { // not present lookup
				BasicDBObject dbo = null;
					BasicDBObject query  = null;
					if (null != _baseQuery) {
						query = (BasicDBObject) _baseQuery.clone();
					}
					else {
						query = new BasicDBObject();
					}
					if (null == _keyField) { // string lookup
						query.put("key", key);
						dbo = (BasicDBObject) _cacheCollection.findOne(query);
					}//TESTED
					else {
						query.put("key", new BasicDBObject(_keyField, key));
						dbo = (BasicDBObject) _cacheCollection.findOne(query);					
					}//TESTED

					//DEBUG:
					//System.out.println("LOOKING: " + query + " IN " + _cacheCollection.getFullName() + " / FOUND: " + dbo);
					
					if (null == dbo) {
						_cacheElement.put(key, _NOT_FOUND_PLACEHOLDER);
						return null;
					}//TESTED
					else { // ok need to create the JSON object and then retrieve it
						Object cacheVal = null;
						String strToSave = null;
						try {
							// (initially saved the cache object but that appeared to lose
							//  the array type (ie appearing as { 0: , 1: } instead of []) so
							//  for safety will save the string and re-eval each call)
							strToSave = dbo.toString();
							factory.executeCompiledScript(JavaScriptUtils.tmpCacheScript,"tmpcache", strToSave);
							cacheVal = factory.getScriptContext().getAttribute("tmpcache");
						}
						catch (Exception e) {}
						_cacheElement.put(key, strToSave);
						return cacheVal;
					}//TESTED
				//DEBUG
				//catch (Exception e) { e.printStackTrace(); return null; }
				
			}
		}
		public synchronized void put(String key, Object value) {
			_cacheElement.put(key, value);
		}
	}
	public static class CustomCacheInJavascriptWrapper {
		CustomCacheInJavascript _cache;
		CompiledScriptFactory _factory;
		CustomCacheInJavascriptWrapper(CustomCacheInJavascript cache,CompiledScriptFactory factory) {
			_cache = cache;
			_factory = factory;
		}
		public Object get(String key) {
			return _cache.get(key, _factory);
		}
	}
}
