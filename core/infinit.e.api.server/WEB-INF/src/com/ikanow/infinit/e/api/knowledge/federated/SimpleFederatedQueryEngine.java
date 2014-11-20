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
package com.ikanow.infinit.e.api.knowledge.federated;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import net.minidev.json.JSONArray;

import org.bson.types.ObjectId;

import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.ikanow.infinit.e.data_model.api.ResponsePojo;
import com.ikanow.infinit.e.data_model.api.knowledge.AdvancedQueryPojo;
import com.ikanow.infinit.e.data_model.interfaces.query.IQueryExtension;
import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.MongoDbManager;
import com.ikanow.infinit.e.data_model.store.MongoDbUtil;
import com.ikanow.infinit.e.data_model.store.config.source.SimpleFederatedCache;
import com.ikanow.infinit.e.data_model.store.config.source.SourceFederatedQueryConfigPojo;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.infinit.e.data_model.store.document.EntityPojo;
import com.ikanow.infinit.e.data_model.utils.IkanowSecurityManager;
import com.jayway.jsonpath.JsonPath;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.Response;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

public class SimpleFederatedQueryEngine implements IQueryExtension {

	//DEBUG
	static private boolean _DEBUG = false;
	//static private boolean _DEBUG = true;
	
	static Logger _logger = Logger.getLogger(SimpleFederatedQueryEngine.class);

	private boolean _cacheMode = true;
	private boolean _testMode = false;
	public void setTestMode(boolean testMode) {
		_testMode = testMode;
		_cacheMode = !_testMode; // (don't cache in some places if in test mode)
	}
	
	public static class FederatedRequest {
		// Endpoint version
		public SourceFederatedQueryConfigPojo.FederatedQueryEndpointUrl subRequest;
		public Future<Response> responseFuture;
		public AsyncHttpClient asyncClient;
		public String mergeKey;
		
		// Script import version
		public FederatedHarvest importThread;
		public AdvancedQueryPojo fullQuery;
		public Throwable errorMessage;
		public BasicDBObject scriptResult = null;
		
		// Joint
		public SourceFederatedQueryConfigPojo endpointInfo;
		public BasicDBObject cachedResult = null;
		public BasicDBObject cachedDoc = null;
		public BasicDBObject cachedDoc_expired = null;
		public String requestParameter;
		public String[] communityIdStrs;
		public String queryIndex;
	}
	
	public void addEndpoint(SourceFederatedQueryConfigPojo newEndpoint) {
		if (null == _endpoints) {
			_endpoints = new LinkedList<SourceFederatedQueryConfigPojo>();
		}
		_endpoints.add(newEndpoint);
	}
	
	LinkedList<FederatedRequest> _asyncRequestsPerQuery = null;
	LinkedList<SourceFederatedQueryConfigPojo> _endpoints = null;
	
	IkanowSecurityManager _scriptingSecurityManager = new IkanowSecurityManager();
	private ScriptEngine _pyEngine = null;
	
	@Override
	public void preQueryActivities(ObjectId queryId, AdvancedQueryPojo query, String[] communityIdStrs) {
		
		_asyncRequestsPerQuery = null;
		
		// 1] Check whether this makes sense to query, get the (sole) entity if so
		String entityType = null;
		String entityValue = null;
		String entityIndex = null;
		String textToTest = null;
		if ((null != query.qt) && (query.qt.size() > 0) && (query.qt.size() < 4)) {

			String logic = query.logic;
			if (null != logic) {
				logic = logic.toLowerCase();
			}
			if ((null != logic) && (logic.contains("or") || logic.contains("not"))) {
				//DEBUG
				if (_DEBUG) _logger.debug("DEB: preQA1: Logic too complex: " + query.logic);
				if (_testMode) {
					throw new RuntimeException("Bad testQueryJson: Logic too complex: " + query.logic);
				}
				
				return; // logic too complex
			}//TESTED (1.3)
			for (AdvancedQueryPojo.QueryTermPojo qt: query.qt) {
				if ((null != qt.entity) || ((null != qt.entityType) && (null != qt.entityValue))) {
					if (null == entityType) { // we now have == 1 entity 
						if (null != qt.entityValue) {
							entityValue = qt.entityValue;
							entityType = qt.entityType;
							entityIndex = entityValue.toLowerCase() + "/" + entityType.toLowerCase();
						}//TESTED (1.5)
						else {
							entityIndex = qt.entity.toLowerCase();
							int index = qt.entity.lastIndexOf('/');
							if (index > 0) {
								entityValue = qt.entity.substring(0, index);
								entityType = qt.entity.substring(index + 1).toLowerCase();
							}
						}//TESTED (1.6)
					}
					else { // >1 entity, not supported
						//DEBUG
						if (_DEBUG) _logger.debug("DEB: preQA2a: >1 entity: " + qt.entity + " / " + entityType + " / " + query.toApi());
						if (_testMode) {
							throw new RuntimeException("Bad testQueryJson: >1 entity: " + qt.entity + " / " + entityType + " / " + query.toApi());
						}
						
						return;
					}//TESTED (1.4)
				}//TESTED
				else if ((null != qt.etext) && (qt.etext.equals("*"))) {
					//this is fine provided it's only ANDed together (eg above logic case)
				}
				else if (null != qt.etext) { // Only work if it matches one of the regexes
					if (null == entityType) {
						textToTest = qt.etext;
						entityType = "etext";
					}
					else { // >1 entity, not supported
						//DEBUG
						if (_DEBUG) _logger.debug("DEB: preQA2b: >1 entity: " + qt.entity + " / " + entityType + " / " + query.toApi());
						if (_testMode) {
							throw new RuntimeException("Bad testQueryJson: >1 entity: " + qt.entity + " / " + entityType + " / " + query.toApi());
						}
						
						return;
					}//TESTED (1.4)					
				}
				else if (null == qt.time) { // temporal 
					//DEBUG
					if (_DEBUG) _logger.debug("DEB: preQA3: non-entity/date " + query.toApi());
					if (_testMode) {
						throw new RuntimeException("Bad testQueryJson: non-entity/date " + query.toApi());
					}
					return;
				}//TESTED (1.1)
			}//(end loop over query terms)
			
		}//TESTED (1.*)
		if (null == entityType) { // Query too complex
			//DEBUG
			if (_DEBUG) _logger.debug("DEB: preQA4: query missing entity " + query.toApi());
			if (_testMode) {
				throw new RuntimeException("Bad testQueryJson: query missing entity " + query.toApi());
			}
			
			return;
		}//TESTED (1.2)
		entityType = entityType.toLowerCase();
		
		// 2] If so, query across all the end
		
		for (SourceFederatedQueryConfigPojo endpoint: _endpoints) {
			
			// Endpoint validation:
			if (null == endpoint.entityTypes) {
				if (_testMode) {
					throw new RuntimeException("No entity types specified");
				}
				else {
					continue;
				}
			}
			if (null != textToTest) { // This is text, see if you can convert to an entity
				entityValue = null; //(reset for different endpoints - used in the check to decide whether to continue)
				
				for (String entityTypeRegex: endpoint.entityTypes) {
					if (entityTypeRegex.startsWith("/")) {
						int regexIndex = entityTypeRegex.lastIndexOf('/'); // (guaranteed to be >= 0)
						try {
							Pattern regex = Pattern.compile(entityTypeRegex.substring(1, regexIndex));
							if (regex.matcher(textToTest).matches()) {
								entityType = entityTypeRegex.substring(1+regexIndex);
								if (entityType.length() > 0) {
									entityValue = textToTest;
									entityIndex = entityValue.toLowerCase() + "/" + entityType.toLowerCase();
								}
							}
						}
						catch (Exception e) { // if not in test mode, carry on
							if (_testMode) {
								throw new RuntimeException(e);
							}
						}
					}
				}//(end loop over entity regexes)
			}//TESTED 
			if (null == entityValue) { // None of the regexes matched
				if (_testMode) {
					throw new RuntimeException("Text specified, does not match any of the regexes: " + Arrays.toString(endpoint.entityTypes.toArray()) + " ... text = " + textToTest);
				}
				continue;
			}
			
			//DEBUG
			if (_DEBUG) _logger.debug("DEB: preQA5: ENDPOINT: " + Arrays.toString(endpoint.entityTypes.toArray()) + " / " + entityType);

			if ((null != endpoint.importScript) && !endpoint.importScript.isEmpty()) {
				if (null == endpoint.scriptlang) {
					endpoint.scriptlang = "python"; // python ==default
				}
				if (endpoint.scriptlang.equalsIgnoreCase("python")) {
					_pyEngine = new ScriptEngineManager().getEngineByName("python");
					if (null == _pyEngine) {
						_logger.error("Python not installed - copy jython 2.5+ into /opt/infinite-home/lib/unbundled");
						if (_testMode) {
							throw new RuntimeException("Python not installed - copy jython 2.5+ into /opt/infinite-home/lib/unbundled");
						}
					}//TESTED (by hand, importScript != null and scriptlang: "python", jython not on classpath)
				}
				else {
					_logger.error("Python is currently the only supported scriptlang");
					if (_testMode) {
						throw new RuntimeException("Python is currently the only supported scriptlang");
					}
				}//TESTED (by hand, importScript != null and scriptlang: "none")
			}//TESTED
			
			if ((null != endpoint.bypassSimpleQueryParsing) && endpoint.bypassSimpleQueryParsing) {
				throw new RuntimeException("Currently only simple query parsing is supported");				
			}
			if ((null != endpoint.entityTypes) && endpoint.entityTypes.contains(entityType)) {
				
				// Check if the *doc* (not *API response*) generated from this endpoint/entity has been cached, check expiry if so
				String cachedDocUrl = buildScriptUrl(endpoint.parentSource.getKey(), entityIndex);
				BasicDBObject cachedDoc = null;
				BasicDBObject cachedDoc_expired = null;
				if (_cacheMode && ((null == endpoint.cacheTime_days) || (endpoint.cacheTime_days >= 0))) {
					BasicDBObject cachedDocQuery = new BasicDBObject(DocumentPojo.url_, cachedDocUrl);
					cachedDocQuery.put(DocumentPojo.sourceKey_, endpoint.parentSource.getKey());
					cachedDoc = (BasicDBObject) DbManager.getDocument().getMetadata().findOne(cachedDocQuery);
					
					if ((null != cachedDoc) && checkDocCache_isExpired(cachedDoc, endpoint)) {
						cachedDoc_expired = cachedDoc;
						cachedDoc = null;
					}
				}//TESTED (by hand)
				
				if (null == _asyncRequestsPerQuery) {
					// If we've got this far create a list to store the async requests
					_asyncRequestsPerQuery = new LinkedList<FederatedRequest>();
				}

				if (null != cachedDoc) {
					// Common params:
					FederatedRequest requestOverview = new FederatedRequest();
					requestOverview.endpointInfo = endpoint;
					requestOverview.communityIdStrs = communityIdStrs;
					requestOverview.requestParameter = entityValue;
					requestOverview.queryIndex = entityIndex;
					requestOverview.mergeKey = endpoint.parentSource.getKey();
					
					if (_DEBUG) _logger.debug("DEB: preQA6z: Doc Cache: " + cachedDocUrl + " , " + cachedDoc);											
					
					requestOverview.cachedDoc = cachedDoc;
					_asyncRequestsPerQuery.add(requestOverview);
				}//TESTED (by hand)
				else if (null != endpoint.importScript) {
					BasicDBObject cachedVal = null;
					if (_cacheMode) { // (source key not static, plus not sure it's desirable, so for simplicity just don't cache requests in test mode) 
						cachedVal = this.getCache(cachedDocUrl, endpoint);
					}
					
					// Common params:
					FederatedRequest requestOverview = new FederatedRequest();
					requestOverview.endpointInfo = endpoint;
					requestOverview.communityIdStrs = communityIdStrs;
					requestOverview.requestParameter = entityValue;
					requestOverview.queryIndex = entityIndex;
					requestOverview.mergeKey = endpoint.parentSource.getKey();
					requestOverview.cachedDoc_expired = cachedDoc_expired;
					
					if (null == cachedVal) {
						requestOverview.importThread = new FederatedHarvest();
						requestOverview.importThread.queryEngine = this;
						requestOverview.importThread.request = requestOverview;
						requestOverview.importThread.start();
					}
					else {
						if (_DEBUG) _logger.debug("DEB: preQA6a: Cache: " + cachedDocUrl + " , " + cachedVal);						
						
						requestOverview.cachedResult = cachedVal;						
					}
					// Launch thread
					_asyncRequestsPerQuery.add(requestOverview);
				}//TESTED (by hand)
				else {
					AsyncHttpClient asyncHttpClient = null;
					try {
						for (SourceFederatedQueryConfigPojo.FederatedQueryEndpointUrl request: endpoint.requests) {
							asyncHttpClient = new AsyncHttpClient();
							BoundRequestBuilder asyncRequest = null;
							String postContent = null;
							if (null != request.httpFields) {
								postContent = request.httpFields.get("Content");
								if (null == postContent) 
									postContent = request.httpFields.get("content");
							}//TESTED (by hand, "http://httpbin.org/post", "httpFields": { "Content": "test" }
							
							if (null == postContent) {
								asyncRequest = asyncHttpClient.prepareGet(request.endPointUrl.replace("$1", entityValue));
							}
							else {
								asyncRequest = asyncHttpClient.preparePost(request.endPointUrl.replace("$1", entityValue));							
							}//TESTED (by hand, "http://httpbin.org/post", "httpFields": { "Content": "test" }
							
							if (null != request.urlParams) {
								for (Map.Entry<String, String> keyValue: request.urlParams.entrySet()) {
									asyncRequest = asyncRequest.addQueryParameter(keyValue.getKey(), keyValue.getValue().replace("$1", entityValue));
								}
							}//TESTED (1.5, 1.6, 3.*, 4.*)
							if (null != request.httpFields) {
								for (Map.Entry<String, String> keyValue: request.httpFields.entrySet()) {
									if (!keyValue.getKey().equalsIgnoreCase("content")) {
										asyncRequest = asyncRequest.addHeader(keyValue.getKey(), keyValue.getValue().replace("$1", entityValue));
									}
								}							
							}//TESTED (by hand, "http://httpbin.org/cookies", "httpFields": { "Cookie": "mycookie=test" }
							if (null != postContent) {
								asyncRequest = asyncRequest.setBody(postContent.replace("$1", entityValue));
							}//TESTED (by hand, "http://httpbin.org/post", "httpFields": { "Content": "$1" }
							
							// Common params:
							FederatedRequest requestOverview = new FederatedRequest();
							requestOverview.endpointInfo = endpoint;
							requestOverview.communityIdStrs = communityIdStrs;
							requestOverview.requestParameter = entityValue;
							requestOverview.asyncClient = asyncHttpClient;
							requestOverview.queryIndex = entityIndex;
							requestOverview.mergeKey = endpoint.parentSource.getKey();
							requestOverview.subRequest = request;
		
							// Now check out the cache:
							URI rawUri = asyncRequest.build().getRawURI();
							String url = rawUri.toString();
							if ((null == endpoint.parentSource.getOwnedByAdmin()) || !endpoint.parentSource.getOwnedByAdmin())
							{
								//TODO (INF-2798): Make this consistent with the how security is handled elsewhere 
								int port = rawUri.getPort();
								if ((80 != port) && (443 != port) && (-1 != port)) {
									_logger.error("Only admin can make requests on non-standard ports: " + url + ": " + port);
									if (_testMode) {
										asyncHttpClient.close();
										throw new RuntimeException("Only admin can make requests on non-standard ports: " + url + ": " + port);
									}									
								}
							}//TESTED (by hand)
							
							BasicDBObject cachedVal = this.getCache(url, endpoint);
							requestOverview.cachedResult = cachedVal;
							requestOverview.cachedDoc_expired = cachedDoc_expired;
							
							if (null == cachedVal) {
								requestOverview.responseFuture = asyncRequest.execute();						
							}					
							else {
								//DEBUG
								if (_DEBUG) _logger.debug("DEB: preQA6b: Cache: " + url + " , " + cachedVal);
								
								requestOverview.responseFuture = null;
								asyncHttpClient.close();
							}
							_asyncRequestsPerQuery.add(requestOverview);
						}//(end loop over multiple requests
					}
					catch (Exception e) {
						_logger.error("Unknown error creating federated query for " + endpoint.titlePrefix + ": " + e.getMessage());
						if (_testMode) {
							throw new RuntimeException("Unknown error creating federated query for " + endpoint.titlePrefix + ": " + e.getMessage(), e);
						}
					}
				}//(end cached doc vs script vs request mode for queries)
				
			}//(end if this request is for this entity type)
			else { // no entity matches - if in test mode then bomb out with useful error
				if (_testMode) {
					throw new RuntimeException("Specified entity: " + entityIndex + " not in set: " + Arrays.toString(endpoint.entityTypes.toArray()));
				}
			}
		}//(end loop over endpoints)
	}

	@Override
	public void postQueryActivities(ObjectId queryId, List<BasicDBObject> docs, ResponsePojo response)
	{
		boolean grabbedScores = false;
		double aggregateSignif = 100.0;
		double queryRelevance = 100.0;
		double score = 100.0;
		
		if (null != _asyncRequestsPerQuery) {
			int added = 0;
			BasicDBObject doc = null;
			BasicDBList bsonArray = new BasicDBList();
			PeekingIterator<FederatedRequest> it = Iterators.peekingIterator(_asyncRequestsPerQuery.iterator());
			while (it.hasNext()) {
				
				FederatedRequest request = it.next();
				if (null == request.cachedDoc) { // no cached doc
					try {
						if (null == request.cachedResult) {	// no cached value				
							if (null != request.importThread) {
								// 1) wait for the thread to finish
								if (null == request.endpointInfo.queryTimeout_secs) {
									request.endpointInfo.queryTimeout_secs = 300;
								}
								for (int timer = 0; timer < request.endpointInfo.queryTimeout_secs; timer++) {
									try {
										request.importThread.join(1000L);
										if (!request.importThread.isAlive()) {
											break;
										}
									}//TESTED (by hand)
									catch (Exception e) {
										
									}
								}
								if (request.importThread.isAlive()) {
									request.errorMessage = new RuntimeException("Script timed out");
								}//TESTED (by hand)
								
								// 2) Get the results
								if (null != request.errorMessage) {
									if (_testMode) {
										throw new RuntimeException(request.errorMessage);
									}
								}
								else if (null == request.scriptResult) {								
									if (_testMode) {
										throw new RuntimeException("Script mode: no cached result found from: " + request.requestParameter);
									}
								}
								else {
									String url = buildScriptUrl(request.mergeKey, request.queryIndex);
									if (_cacheMode) { // (don't cache python federated queries in test mode)
										this.cacheApiResponse(url, request.scriptResult, request.endpointInfo);
									}
									bsonArray.add(request.scriptResult);								
								}
							} // end script mode
							else { // HTTP mode
								Response endpointResponse = request.responseFuture.get();
								request.asyncClient.close();
								request.asyncClient = null;
		
								String jsonStr = endpointResponse.getResponseBody();
								String url = endpointResponse.getUri().toURL().toString();
								
								Object bsonUnknownType = com.mongodb.util.JSON.parse(jsonStr);
								BasicDBObject bson = null;
								if (bsonUnknownType instanceof BasicDBObject) {
									bson = (BasicDBObject) bsonUnknownType;
								}
								else if (bsonUnknownType instanceof BasicDBList) {
									bson = new BasicDBObject("array", bsonUnknownType);
								}
								else if (bsonUnknownType instanceof String) {
									bson = new BasicDBObject("value", bsonUnknownType);									
								}
								
								//DEBUG
								if (_DEBUG) _logger.debug("DEB: postQA1: " + url + ": " + jsonStr);
								
								if (null != bson) {
									MongoDbUtil.enforceTypeNamingPolicy(bson, 0);
									this.cacheApiResponse(url, bson, request.endpointInfo);
									bsonArray.add(bson);
								}
							}//(end script vs request method)
						}//TESTED (3.1, 4.2)
						else { // (just used cached value)
							//DEBUG 
							if (_DEBUG) _logger.debug("DEB: postQA2: " + request.cachedResult.toString());
							
							bsonArray.add((BasicDBObject)request.cachedResult.get(SimpleFederatedCache.cachedJson_));
						}//TESTED (4.1, 4.3)
					} 
					catch (Exception e) {
						//DEBUG
						if (null == request.subRequest) {
							_logger.error("Error with script: " + e.getMessage());
							if (_testMode) {
								throw new RuntimeException("Error with script: " + e.getMessage(), e);
							}						
						}
						else {
							_logger.error("Error with " + request.subRequest.endPointUrl + ": " + e.getMessage());
							if (_testMode) {
								throw new RuntimeException("Error with " + request.subRequest.endPointUrl + ": " + e.getMessage(), e);
							}
						}
					}
					if (!it.hasNext() || (request.mergeKey != it.peek().mergeKey)) { // deliberate ptr arithmetic
						String url = buildScriptUrl(request.mergeKey, request.queryIndex);
						
						//DEBUG
						if (_DEBUG) _logger.debug("DEB: postQA3: " + url + ": " + bsonArray);					
						
						doc = createDocFromJson(bsonArray, url, request, request.endpointInfo);
					}
				} // (end if no cached doc)
				else { // cached doc, bypass lots of processing because no merging and doc already built
					doc = request.cachedDoc;
				}//TESTED (by hand)
					
				if (null != doc) {
					// Cache the document unless already cached (or caching disabled)
					if ((null == request.cachedDoc) && _cacheMode && 
							((null == request.endpointInfo.cacheTime_days) || (request.endpointInfo.cacheTime_days >= 0)))
					{
						if (null != request.cachedDoc_expired) {
							ObjectId updateId = request.cachedDoc_expired.getObjectId(DocumentPojo.updateId_);
							if (null != updateId) {
								doc.put(DocumentPojo.updateId_, updateId);
							}
							else {
								doc.put(DocumentPojo.updateId_, request.cachedDoc_expired.getObjectId(DocumentPojo._id_));
							}
							BasicDBObject docUpdate = new BasicDBObject(DocumentPojo.url_, doc.getString(DocumentPojo.url_));
							docUpdate.put(DocumentPojo.sourceKey_, doc.getString(DocumentPojo.sourceKey_));
							DbManager.getDocument().getMetadata().remove(docUpdate);
							
							//DEBUG
							if (_DEBUG) _logger.debug("DEB: postQA4a: re-cached ... " + docUpdate.toString() + ": " + doc.getObjectId(DocumentPojo.updateId_));
						}
						//DEBUG
						if (_DEBUG) _logger.debug("DEB: postQA4b: cached ... " + doc);					
						DbManager.getDocument().getMetadata().save(doc);

					}//TESTED (by hand, 3 cases: cached not expired, cached expired first time, cached expired multiple times)
					
					if (!grabbedScores) {
						if (!docs.isEmpty()) {
							BasicDBObject topDoc = docs.get(0);
							aggregateSignif = topDoc.getDouble(DocumentPojo.aggregateSignif_, aggregateSignif);
							queryRelevance = topDoc.getDouble(DocumentPojo.queryRelevance_, queryRelevance);
							score = topDoc.getDouble(DocumentPojo.score_, score);
							grabbedScores = true;
							
							// OK would also like to grab the original matching entity
							BasicDBList ents = (BasicDBList) topDoc.get(DocumentPojo.entities_);
							if (null != ents) {
								for (Object entObj: ents) {
									BasicDBObject ent = (BasicDBObject)entObj;
									String entIndex = ent.getString(EntityPojo.index_, "");
									if (entIndex.equals(request.queryIndex)) {
										ents = (BasicDBList) doc.get(DocumentPojo.entities_);
										if (null != ents) {
											ents.add(ent);
										}
										break;
									}
								}
							}//TESTED (by hand)
						}
					}
					doc.put(DocumentPojo.aggregateSignif_, aggregateSignif);
					doc.put(DocumentPojo.queryRelevance_, queryRelevance);
					doc.put(DocumentPojo.score_, score);

					// If we're returning to a query then we'll adjust the doc format (some of the atomic fields become arrays)
					if (!_testMode) {
						convertDocToQueryFormat(doc, request.communityIdStrs);							
					}//TESTED (by hand)
					
					docs.add(0, doc);
					added++;
					doc = null; //(reset)
					
					//(end if built a doc from the last request/set of requests)
				}//TESTED (3.1)		
				
			}//(end loop over federated requests)
			
			if (null != response.getStats()) {
				response.getStats().found += added;
			}//TESTED (by hand)			
		}
	}

	/////////////////////////////////////////////////
	
	// UTILITY
	
	public BasicDBObject createDocFromJson(BasicDBList jsonList, String url, FederatedRequest request, SourceFederatedQueryConfigPojo endpointInfo) {
		BasicDBObject doc = null; // (don't create unless needed)
		BasicDBList ents = null;
		StringBuffer entVals = null;
		HashSet<String> entDedup = null;
		
		if (_testMode) { // In test mode, need to return the JSON even if no entities are specified 
			doc = new BasicDBObject();
		}
		if (null != endpointInfo.docConversionMap) {
			for (Map.Entry<String, String> docInfo: endpointInfo.docConversionMap.entrySet()) {
				for (Object jsonObj: jsonList) {
					BasicDBObject json = (BasicDBObject)jsonObj;
					try {
						String key = docInfo.getKey();
						// (allow user to not prepend array: if they don't want to)
						if ((1 == json.size()) && json.containsKey((Object)"array")) {
							if (!key.startsWith("array:") && 
									!key.startsWith(":array") && !key.startsWith("$:array") && 
									!key.startsWith("::") && !key.startsWith("$::")) 
							{
								if (key.startsWith(":")) { // jpath
									key = ":array" + key;
								}
								else if (key.startsWith("$:")) { // jpath
									key = "$:array" + key.substring(1);
								}
								else {
									key = "array:" + key;
								}									
							}								
						}//TESTED (by hand)
						if (key.startsWith(":")) { // jpath
							key = "$" + key;
						}
						// NOTE: *not* org.json.JSONArray
						JSONArray candidateEntities = null;
						if (key.startsWith("$")) {
							JSONArray candidateEntities_tmp = JsonPath.read(json.toString(), key.replace(':', '.'));
							if (null != candidateEntities_tmp) {
								candidateEntities = new JSONArray();
								for (Object o: candidateEntities_tmp) {
									if (o instanceof String) {
										candidateEntities.add(o);
									}
									else if (o instanceof JSONArray) {
										candidateEntities.addAll((JSONArray)o);
									}
								}//TESTED (displayUrl vs entities, 3.2)
							}							
							//DEBUG
							//System.out.println(candidateEntities);
							
						}//(TESTED (permutations above by hand))
						else {
							String s = (String) MongoDbUtil.getProperty(json, key.replace(':', '.'));
							if (null != s) {
								candidateEntities = new JSONArray();
								candidateEntities.add(s);
							}
						}//TESTED (3.1)											
						
						if (null != candidateEntities) for (int i = 0; i < candidateEntities.size(); ++i) {
							Object o = candidateEntities.get(i);
							if (!(o instanceof String)) {
								continue;
							}
							String s = o.toString();
							if (null == doc) {
								doc = new BasicDBObject();
								//(various fields added below)
							}
							if (docInfo.getValue().equalsIgnoreCase(DocumentPojo.displayUrl_)) {
								doc.put(DocumentPojo.displayUrl_, s);
							}//TESTED (3.1, 4.*)
							else { // Entities!
								if (null == ents) {
									ents = new BasicDBList();
								}
								String index = s.toLowerCase() + "/" + docInfo.getValue().toLowerCase();
								
								if (null == entDedup) {
									entDedup = new HashSet<String>();
								}
								else if (entDedup.contains(index)) { // Entity deduplication
									continue;
								}//TESTED (3.2)
								entDedup.add(index);
								
								if (null == entVals) {
									entVals = new StringBuffer(": ");
								}
								else {
									entVals.append(", ");
								}
								entVals.append(s);
								
								String dimension = null;
								if (null != endpointInfo.typeToDimensionMap) {
									try {
										dimension = EntityPojo.Dimension.valueOf(endpointInfo.typeToDimensionMap.get(docInfo.getValue())).toString();
									}
									catch (Exception e) {}
								}
								if (null == dimension) {
									dimension = EntityPojo.Dimension.What.toString();
								}//TESTED (by hand)
								
								// (alternative to "made up" values would be to go looking in the existing docs/ents?)
								// (we'll try to avoid that for now...)
								BasicDBObject ent = new BasicDBObject();
								ent.put(EntityPojo.disambiguated_name_, s);
								ent.put(EntityPojo.type_, docInfo.getValue());
								ent.put(EntityPojo.dimension_, dimension);
								ent.put(EntityPojo.relevance_, 1.0);
								ent.put(EntityPojo.doccount_, 1L); // (ie relative to this query)
								ent.put(EntityPojo.averageFreq_, 1.0);
								ent.put(EntityPojo.datasetSignificance_, 10.0); // (ie relative to this query)
								ent.put(EntityPojo.frequency_, 1.0);
								ent.put(EntityPojo.index_, index);
								ent.put(EntityPojo.queryCoverage_, 100.0); // (ie relative to this query)
								ent.put(EntityPojo.totalfrequency_, 1.0); // (ie relative to this query)
								ents.add(ent);
							}//TESTED (3.1, 4.*)
						}
					}
					catch (Exception e) {
						//(do nothing? null or the wrong type)
						//e.printStackTrace();
					}
				}//end loop over various JSON objects retrieved
			}//(End loop over doc conversion elements)
		}//TESTED (3.*, 4.*)
		
		if ((null == ents) && !_testMode) { // don't return unless there are any entities
			return null;
		}
		else if (null != doc) {
			// Insert mandatory fields:
			// (Note the query format is a little bit different, the following fields are converted to arrays:
			//  sourceKey, source, communityId, mediaType)
			doc.put(DocumentPojo._id_, new ObjectId());
			doc.put(DocumentPojo.url_, url);
			doc.put(DocumentPojo.created_, new Date());
			doc.put(DocumentPojo.modified_, new Date());
			doc.put(DocumentPojo.publishedDate_, new Date());
			doc.put(DocumentPojo.sourceKey_, endpointInfo.parentSource.getKey());
			doc.put(DocumentPojo.source_, endpointInfo.parentSource.getTitle());
			doc.put(DocumentPojo.communityId_, new ObjectId(request.communityIdStrs[0]));
			doc.put(DocumentPojo.mediaType_, endpointInfo.parentSource.getMediaType());
			doc.put(DocumentPojo.metadata_, new BasicDBObject("json", jsonList.toArray()));
			
			if ((null != entVals) && (entVals.length() > 165)) { // (arbitrary length)
				entVals.setLength(165);
				entVals.append("...");
			}
			doc.put(DocumentPojo.title_, new StringBuffer(endpointInfo.titlePrefix).append(": ").append(request.requestParameter).append(entVals).toString());
			doc.put(DocumentPojo.entities_, ents);
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			JsonParser jp = new JsonParser();
			JsonElement je = jp.parse(jsonList.toString());
			doc.put(DocumentPojo.description_, gson.toJson(je)); // (prettified JSON)				
		}//TESTED (3.*, 4.*)
		
		return doc;
	}
	
	// Convert some of the doc fields to 
	private static BasicDBObject convertDocToQueryFormat(BasicDBObject doc, String[] communityIdStrs) {
		doc.put(DocumentPojo.sourceKey_, Arrays.asList(doc.get(DocumentPojo.sourceKey_))); // (don't need to remove #N/#NN - fed queries can't have them)
		doc.put(DocumentPojo.source_, Arrays.asList(doc.get(DocumentPojo.source_)));
		doc.put(DocumentPojo.mediaType_, Arrays.asList(doc.get(DocumentPojo.mediaType_)));
		doc.put(DocumentPojo.communityId_, communityIdStrs);

		return doc;
	}//TESTED (by hand)
	
	private static String buildScriptUrl(String mergeKey, String entityIndex) {
		String url = new StringBuilder().append("inf://federated/").append(mergeKey).append("/").append(entityIndex).toString();
		return url;
	}//TESTED (by hand)

	/////////////////////////////////////////////////
	
	// CACHE UTILITIES
	
	public static boolean TEST_MODE_ONLY = false;	
	public static DBCollection getCacheCollection() {
		if (TEST_MODE_ONLY) {
			return MongoDbManager.getCollection("test", "fed_query_cache");
		}
		else {
			return MongoDbManager.getIngest().getFederatedCache();
		}
	}
	
	private static boolean _staticInitializer = false;
	private static long _lastChecked = -1L;
	private BasicDBObject getCache(String url, SourceFederatedQueryConfigPojo endpoint) {
		if ((null != endpoint.cacheTime_days) && (endpoint.cacheTime_days <= 0)) { // cache disabled
			return null;
		}
		
		DBCollection endpointCacheCollection = getCacheCollection();
		
		if (!_staticInitializer) {
			_staticInitializer = true;
			endpointCacheCollection.ensureIndex(new BasicDBObject(SimpleFederatedCache.expiryDate_, 1));
		}
		BasicDBObject cacheObj = (BasicDBObject) endpointCacheCollection.findOne(new BasicDBObject(SimpleFederatedCache._id_, url));
		if (null == cacheObj) {
			return null;
		}
		// else found something, means there's stuff in the DB
		// so check it's not too big:
		Date now = new Date();					
		if ((-1 == _lastChecked) || (now.getTime() > (_lastChecked + (600L*1000L)))) { // (only check every 10 minutes) 
			
			if (endpointCacheCollection.count() > SimpleFederatedCache.QUERY_FEDERATION_CACHE_CLEANSE_SIZE) {
				_lastChecked = now.getTime();
				// Remove everything with expiry date older than now				
				endpointCacheCollection.remove(new BasicDBObject(SimpleFederatedCache.expiryDate_, new BasicDBObject(DbManager.lt_, new Date())));
			}
		}//TESTED (4.3)
		Date expiryDate = cacheObj.getDate(SimpleFederatedCache.expiryDate_, now);
		if (now.getTime() < expiryDate.getTime()) {
			return cacheObj;
		}
		else {
			return null;
		}//TESTED (4.2)
	}//TESTED (4.*)
	
	private static final int DEFAULT_CACHE_TIME_DAYS = 5;
	
	private void cacheApiResponse(String url, BasicDBObject toCacheJson, SourceFederatedQueryConfigPojo endpoint) {
		
		int cacheTime_days = DEFAULT_CACHE_TIME_DAYS;
		if (null != endpoint.cacheTime_days) {
			cacheTime_days = endpoint.cacheTime_days;
		}
		if (cacheTime_days <= 0) { // Disable _request_ cache (to disable all caching include doc caching use -1)
			return;
		}
		
		DBCollection endpointCacheCollection = getCacheCollection();
		
		BasicDBObject toCacheObj = new BasicDBObject(SimpleFederatedCache._id_, url);
		toCacheObj.put(SimpleFederatedCache.cachedJson_, toCacheJson);
		toCacheObj.put(SimpleFederatedCache.expiryDate_, new Date(new Date().getTime() + cacheTime_days*3600L*24L*1000L));
		endpointCacheCollection.save(toCacheObj);
	}//TESTED (3.1, 4.*)

	// Document level caching, although it effectively serves as a mostly redundant request cache,
	// It's actually used to allow users to save federated query documents in their buckets
	
	public static boolean checkDocCache_isExpired(BasicDBObject cachedDoc, SourceFederatedQueryConfigPojo endpoint) {
		if (null == endpoint.cacheTime_days)
			endpoint.cacheTime_days = DEFAULT_CACHE_TIME_DAYS;  
		
		Date now = new Date();
		long cacheThreshold = cachedDoc.getDate(DocumentPojo.created_, now).getTime() + endpoint.cacheTime_days*3600L*24L*1000L;
		
		if (cacheThreshold < now.getTime()) // (ie doc-creation-time + cache is earlier than now => time to decache)
		{
			if (_DEBUG) _logger.debug("DEB: preQA6zz: Cache expired: " + cachedDoc.getString(DocumentPojo.url_) + ": " + new Date(cacheThreshold) + " vs " + now);						
			
			return true;
		}
		else 
			return false;
	}//TESTED
	
	///////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////

	// (my own threading)
	
	public static class FederatedHarvest extends Thread {
		public FederatedRequest request;
		public SimpleFederatedQueryEngine queryEngine;
		public void run() {
			String scriptResult = null;
			LinkedList<String> debugLog = queryEngine._testMode ? new LinkedList<String>() : null;
			try {
				scriptResult = queryEngine.performImportPythonScript(request.endpointInfo.importScript, request.requestParameter, request.fullQuery, request.endpointInfo.parentSource.getOwnedByAdmin(), debugLog);
			}
			catch (Exception e) {
				request.errorMessage = e;
				return;
			}			
			Object parsedScriptResult = null;
			try {
				parsedScriptResult = com.mongodb.util.JSON.parse(scriptResult.toString());
			}
			catch (Exception e) {
				request.errorMessage = new RuntimeException("Error deserializing " + scriptResult + ": " + e.getMessage());
				return;
			}			
			if (parsedScriptResult instanceof BasicDBObject) {
				request.scriptResult = (BasicDBObject) parsedScriptResult;
			}
			else if (parsedScriptResult instanceof BasicDBList) {
				request.scriptResult = new BasicDBObject("array", parsedScriptResult);
			}
			else if (parsedScriptResult instanceof String) {
				request.scriptResult = new BasicDBObject("value", parsedScriptResult);
			}
			else {
				request.errorMessage = new RuntimeException("Error deserializing " + scriptResult + ": " + parsedScriptResult);
				return;				
			}
			try {
				MongoDbUtil.enforceTypeNamingPolicy(request.scriptResult, 0);
				if ((null != debugLog) && !debugLog.isEmpty()) {
					request.scriptResult.put("$logs", debugLog);
				}
			}
			catch (Exception ee) {
				request.errorMessage = new RuntimeException("Error deserializing " + scriptResult + ": " + ee.getMessage());
				return;
			}			
		}
	}//TESTED (by hand)
	
	///////////////////////////////////////////////////////////////////////////////

	private String performImportPythonScript(String importScript, String entityValue, AdvancedQueryPojo query, Boolean isSrcAdmin, LinkedList<String> debugLog) {
		String modCode = importScript;
		try {					
			// Create a return value
			String fnName = "var" + new ObjectId();
			
			// Pull all the imports, evaluate them outside the security manager
			Pattern importRegex = Pattern.compile("^\\s*(?:import|from)\\s[^\n]+", Pattern.MULTILINE);
			Matcher m = importRegex.matcher(importScript);
			StringBuffer sbImports = new StringBuffer();
			while (m.find()) {
				sbImports.append(m.group()).append('\n');
			}						
			if (null != query) {
				_pyEngine.put("_query", query.toApi()); // full query				
			}
			if (null != entityValue) {
				_pyEngine.put("_entityValue", entityValue); // allow either entityValue				
			}
			_pyEngine.eval(sbImports.toString());

			// Logging function
			if (null != debugLog) {
				String logName = "log" + new ObjectId();
				_pyEngine.put(logName, debugLog);
				_pyEngine.eval("def ikanow_log(logmsg):\n   " + logName + ".add(logmsg)\n\n");
			}
			else {
				_pyEngine.eval("def ikanow_log(logmsg):\n   pass\n\n");				
			}
			
			// Enable SSL everywhere (https://wiki.python.org/jython/NewSocketModule#SSLSupport)
			//http://tech.pedersen-live.com/2010/10/trusting-all-certificates-in-jython/
			//didn't work: http://jython.xhaus.com/installing-an-all-trusting-security-provider-on-java-and-jython/
			_pyEngine.eval(IOUtils.toString(SimpleFederatedQueryEngine.class.getResourceAsStream("JythonTrustManager.py")));
			
			// Now run the script
			modCode = m.replaceAll("");
			modCode = modCode.replaceAll("\n([^\n]+)$", "\n" + fnName + " = " + "$1");
			if ((null == isSrcAdmin) || !isSrcAdmin) {
				_scriptingSecurityManager.eval(_pyEngine, modCode);
			}
			else {
				//TODO (INF-2798): Make this consistent with the how security is handled elsewhere 
				_pyEngine.eval(modCode);
			}

			// Get return value
			Object result = _pyEngine.get(fnName.toString());
			
			//DEBUG
			if (_DEBUG) _logger.debug("DEB: T1: Return val from script: " + result);			
			
			if (null == result) {
				throw new RuntimeException("Null return from script - final line needs to evaluate expression to return, eg 'response.read()' or 'varname'");
			}//TESTED (by hand)
			
			return result.toString();
			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		catch (Error ee) {
			throw new RuntimeException(ee);
		}		
	}//TESTED
	
	///////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////
	
	// TEST UTILITIES
	
	// Check 1.x test results
	
	public void test_CheckIfQueryLaunched(String testName, boolean shouldWork) {
		
		FederatedRequest request = null;
		if (null != this._asyncRequestsPerQuery) {
			if (!this._asyncRequestsPerQuery.isEmpty()) {
				request = this._asyncRequestsPerQuery.peek();
				if (this._asyncRequestsPerQuery.size() > 1) {
					System.out.println("*** " + testName + ":  + too many async requests");
					System.exit(-1);
				}
			}
		}
		
		if (shouldWork) {
			if (null == request) {
				System.out.println("*** " + testName + ": Expected request, got none");				
				System.exit(-1);
			}
		}
		else {
			if (null != request) {
				if (null != request.responseFuture) {
					System.out.println("*** " + testName + ": incorrectly added " + request.requestParameter);
					System.exit(-1);
				}
				else if ((null == request.cachedResult) && (null == request.cachedDoc)) {
					System.out.println("*** " + testName + ": no request, not cached " + request.cachedResult);
					System.exit(-1);					
				}
			}
		}
	}//TESTED (by hand - who tests the tester?!)

	public void test_CheckIfDocAdded(String testName, List<BasicDBObject> docs) {
		
		// 1) Did a doc get added?
		
		if (2 != docs.size()) {
			System.out.println("*** " + testName + ": didn't add doc? " + docs.size());			
			System.exit(-1);
		}
		
		// 2) Was it the right doc?
		
		BasicDBObject doc = docs.get(0);
		doc.remove(DocumentPojo._id_);
		doc.remove(DocumentPojo.created_);
		doc.remove(DocumentPojo.modified_);
		doc.remove(DocumentPojo.publishedDate_);

		String docToCheck = 
			"{ \"displayUrl\" : \"http://test3_1\" , \"url\" : \"inf://federated/fakeendpoint.123/test3_1/testentityin\" , \"sourceKey\" : [ \"fakeendpoint.123\"] , \"source\" : [ \"fakeendpoint\"] , \"communityId\" : [ \"4c927585d591d31d7b37097a\"] , \"mediaType\" : [ \"Report\"] , \"metadata\" : { \"json\" : [ { \"test\" : { \"field\" : [ \"test3_1\" , \"test3_1\"] , \"field2\" : \"http://test3_1\"}}]} , \"title\" : \"fake endpoint: : test3_1: test3_1\" , \"entities\" : [ { \"disambiguated_name\" : \"test3_1\" , \"type\" : \"TestEntityOut\" , \"dimension\" : \"What\" , \"relevance\" : 1.0 , \"doccount\" : 1 , \"averageFreq\" : 1.0 , \"datasetSignificance\" : 10.0 , \"frequency\" : 1.0 , \"index\" : \"test3_1/testentityout\" , \"queryCoverage\" : 100.0 , \"totalfrequency\" : 1.0}] , \"description\" : \"[\\n  {\\n    \\\"test\\\": {\\n      \\\"field\\\": [\\n        \\\"test3_1\\\",\\n        \\\"test3_1\\\"\\n      ],\\n      \\\"field2\\\": \\\"http://test3_1\\\"\\n    }\\n  }\\n]\" , \"aggregateSignif\" : 115.0 , \"queryRelevance\" : 105.0 , \"score\" : 110.0}";
		
		if (!docToCheck.equals(doc.toString())) {
			System.out.println("*** " + testName + ": document incorrect:\n" + docToCheck + "\nVS\n" + doc.toString());
			System.exit(-1);
		}
		
		// 3) Did the doc get cached?
		
		DBCollection endpointCacheCollection = getCacheCollection();
		
		String hardwiredCacheId = "http://localhost:8186/test/test3_1/?param1=test3_1";
		BasicDBObject cachedVal = (BasicDBObject) endpointCacheCollection.findOne(new BasicDBObject(SimpleFederatedCache._id_, hardwiredCacheId));
		
		if (null == cachedVal) {
			System.out.println("*** " + testName + ": no cache for: " + doc.get(DocumentPojo.url_) + " / " + hardwiredCacheId);
			System.exit(-1);
		}
		else {
			Date expiryDate = cachedVal.getDate(SimpleFederatedCache.expiryDate_, null);
			if ((null == expiryDate) || ((new Date().getTime() - expiryDate.getTime()) > 10*1000L)) {
				System.out.println("*** " + testName + ": expiry date for: " + doc.get(DocumentPojo.url_) + ": " + expiryDate + " vs now= " + new Date());				
				System.exit(-1);
			}
			BasicDBObject cachedJson = (BasicDBObject) cachedVal.get(SimpleFederatedCache.cachedJson_);
			Object docVal = null;
			try {
				Object[] docVals = (Object[] ) ((BasicDBObject)doc.get(DocumentPojo.metadata_)).get("json");
				docVal = docVals[0];
			}
			catch (Exception e) {
				@SuppressWarnings("rawtypes")
				Collection docVals = (Collection) ((BasicDBObject)doc.get(DocumentPojo.metadata_)).get("json");
				docVal = docVals.iterator().next();
				
			}
			if ((null == cachedJson) || !cachedJson.equals(docVal)) {
				System.out.println("*** " + testName + ": cache: " + doc.get(DocumentPojo.url_) + ": cached val incorrect:\n" + docVal + "\nVS\n" + cachedJson);
				System.exit(-1);				
			}
		}
	}//TESTED (by hand)
	
	///////////////////////////////////////////////////////////////////////////////
	
	// (general test utils)
	
	// Reset state ready for next test
	
	public void test_queryClear(boolean clearCacheAlso) {
		if (null != this._asyncRequestsPerQuery) {
			for (FederatedRequest req: this._asyncRequestsPerQuery) {
				try {
					req.responseFuture.get();
					if (null != req.asyncClient) {
						req.asyncClient.close();
					}
				}
				catch (Exception e) {}
			}
			this._asyncRequestsPerQuery.clear();
		}
		if (clearCacheAlso) {
			DBCollection endpointCacheCollection = getCacheCollection();
			endpointCacheCollection.remove(new BasicDBObject());
		}
	}//TESTED (by hand - who tests the tester?!)
	
	public void test_cacheExpire() {
		DBCollection endpointCacheCollection = getCacheCollection();

		DBCursor dbc = endpointCacheCollection.find();
		for (DBObject cacheEntryObj: dbc) {
			BasicDBObject cacheEntry = (BasicDBObject) cacheEntryObj;
			cacheEntry.put(SimpleFederatedCache.expiryDate_, new Date(new Date().getTime() - 3600L*1000L)); // (ie expired an hour ago)
			endpointCacheCollection.save(cacheEntry);
		}
	}
	public void test_cacheClear(boolean clearApiCache, boolean clearDocCache, String key) {
		// (DO NOT COMMENT THIS OUT)
		SimpleFederatedQueryEngine.TEST_MODE_ONLY = true;		
		
		// Clear doc_metadata table before running
		if (clearDocCache) {
			MongoDbManager.getDocument().getMetadata().remove(new BasicDBObject(DocumentPojo.sourceKey_, key));
		}
		if (clearApiCache) {
			// (note: this only deletes the test collection because of TEST_MODE_ONLY code)
			SimpleFederatedQueryEngine.getCacheCollection().remove(new BasicDBObject()); // (clear test collection for testing)		
		}
	}
	public void test_cacheFill(String testName, boolean fill, boolean shouldBeFull) {
		DBCollection endpointCacheCollection = getCacheCollection();
		if (fill) {
			for (long i = 0; i < (1 + SimpleFederatedCache.QUERY_FEDERATION_CACHE_CLEANSE_SIZE); ++i) {
				SimpleFederatedCache fakeCacheElement = new SimpleFederatedCache();
				fakeCacheElement.expiryDate = new Date(new Date().getTime() - 3600L*1000L); // (ie expired an hour ago)
				fakeCacheElement._id = testName + "_" + i;
				fakeCacheElement.cachedJson = new BasicDBObject(); 
				endpointCacheCollection.save(fakeCacheElement.toDb());
			}
			_lastChecked = new Date(new Date().getTime() - 602L*1000L).getTime();
		}
		long count = endpointCacheCollection.count();
		if (shouldBeFull) {
			if (count < SimpleFederatedCache.QUERY_FEDERATION_CACHE_CLEANSE_SIZE) {
				System.out.println("*** " + testName + ": cache should just contain many elements, not: " + count);
				System.exit(-1);												
			}
		}
		else {
			if (1 != count) {
				System.out.println("*** " + testName + ": cache should just contain one element, not: " + count);
				System.exit(-1);								
			}
		}
	}
}
