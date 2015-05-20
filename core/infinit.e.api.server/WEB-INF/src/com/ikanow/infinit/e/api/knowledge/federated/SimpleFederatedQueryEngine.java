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

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
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
import com.ikanow.infinit.e.api.knowledge.QueryHandler.ISimpleFederatedQueryEngine;
import com.ikanow.infinit.e.api.knowledge.processing.ScoringUtils;
import com.ikanow.infinit.e.data_model.api.ResponsePojo;
import com.ikanow.infinit.e.data_model.api.knowledge.AdvancedQueryPojo;
import com.ikanow.infinit.e.data_model.interfaces.query.IQueryExtension;
import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.MongoDbManager;
import com.ikanow.infinit.e.data_model.store.MongoDbUtil;
import com.ikanow.infinit.e.data_model.store.config.source.SimpleFederatedCache;
import com.ikanow.infinit.e.data_model.store.config.source.SourceFederatedQueryConfigPojo;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.ikanow.infinit.e.data_model.store.document.AssociationPojo;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.infinit.e.data_model.store.document.EntityPojo;
import com.ikanow.infinit.e.data_model.utils.IkanowSecurityManager;
import com.ikanow.infinit.e.harvest.HarvestController;
import com.ikanow.infinit.e.harvest.HarvestControllerPipeline;
import com.ikanow.infinit.e.harvest.extraction.text.externalscript.TextExtractorExternalScript;
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

public class SimpleFederatedQueryEngine implements IQueryExtension, ISimpleFederatedQueryEngine {

	//DEBUG
	static private boolean _DEBUG = false;
	//static private boolean _DEBUG = true;
	
	static Logger _logger = Logger.getLogger(SimpleFederatedQueryEngine.class);

	private ScoringUtils _scoreStats = null;
	public void registerScoringEngine(ScoringUtils scoreStats) {
		_scoreStats = scoreStats;
	}
	
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
		public List<DocumentPojo> complexSourceProcResults = null;
		
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
		
		_scoreStats = null;
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
				else if (endpoint.scriptlang.equalsIgnoreCase("external")) {
					//nothing to do here, just carry on, will handle the external bit later on
				}
				else {
					_logger.error("Python/External is currently the only supported scriptlang");
					if (_testMode) {
						throw new RuntimeException("Python is currently the only supported scriptlang");
					}
				}//TESTED (by hand, importScript != null and scriptlang: "none")
			}//TESTED
			
			if ((null != endpoint.bypassSimpleQueryParsing) && endpoint.bypassSimpleQueryParsing) {
				throw new RuntimeException("Currently only simple query parsing is supported");				
			}
			if ((null != endpoint.entityTypes) && endpoint.entityTypes.contains(entityType)) {
				
				// If not using the full source pipeline processing capability (ie always generating 0/1
				BasicDBObject cachedDoc = null;
				String cachedDocUrl = buildScriptUrl(endpoint.parentSource.getKey(), entityIndex);
				BasicDBObject cachedDoc_expired = null;
				if (!isComplexSource(endpoint.parentSource)) {
					// Check if the *doc* (not *API response*) generated from this endpoint/entity has been cached, check expiry if so
					if (_cacheMode && ((null == endpoint.cacheTime_days) || (endpoint.cacheTime_days >= 0))) {
						
						if (_DEBUG) _logger.debug("DEB: preQA6ya: Search Doc Cache: " + cachedDocUrl + " , " + endpoint.cacheTime_days);						
						
						BasicDBObject cachedDocQuery = new BasicDBObject(DocumentPojo.url_, cachedDocUrl);
						cachedDocQuery.put(DocumentPojo.sourceKey_, endpoint.parentSource.getKey());
						cachedDoc = (BasicDBObject) DbManager.getDocument().getMetadata().findOne(cachedDocQuery);
						if (null != cachedDoc) {
							// (quick check if we have a complex source in here)
							String sourceUrl = cachedDoc.getString(DocumentPojo.sourceUrl_);
							if (null != sourceUrl) { // switching from complex to simple source - delete the cached docs
								
								if (_DEBUG) _logger.debug("DEB: preQA6yb: Clear Search Doc Cache: " + cachedDocUrl + " , " + sourceUrl);						
								
								cachedDocQuery.remove(DocumentPojo.url_);
								cachedDocQuery.put(DocumentPojo.sourceUrl_, sourceUrl);
								DbManager.getDocument().getMetadata().remove(cachedDocQuery);
								cachedDoc = null;
							}//TESTED (by hand)
							else if (checkDocCache_isExpired(cachedDoc, endpoint)) {
								cachedDoc_expired = cachedDoc;
								cachedDoc = null;
							}
						}
					}//TESTED (by hand)
				}
				
				if (null == _asyncRequestsPerQuery) {
					// If we've got this far create a list to store the async requests
					_asyncRequestsPerQuery = new LinkedList<FederatedRequest>();
				}

				if (null != cachedDoc) { // (simple sources only, by construction)
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
					
					if (null != cachedVal) {
						if (checkIfNeedToClearCache(cachedVal, endpoint.parentSource)) {
							if (_DEBUG) _logger.debug("DEB: preQA6aa: Clear cache: " + cachedDocUrl + " , " + cachedVal);						
							cachedVal = null;
						}
					}
					requestOverview.cachedResult = cachedVal; // will often be null						
					
					if ((null == cachedVal) || isComplexSource(endpoint.parentSource)) {
						if (null != cachedVal) {
							if (_DEBUG) _logger.debug("DEB: preQA6ab: Complex Src Cache: " + cachedDocUrl + " , " + cachedVal);													
						}						
						if (endpoint.scriptlang.equalsIgnoreCase("external")) {
							requestOverview.importThread = new FederatedScriptHarvest();
						}
						else {
							requestOverview.importThread = new FederatedJythonHarvest();
						}
						requestOverview.importThread.queryEngine = this;
						requestOverview.importThread.request = requestOverview;
						requestOverview.importThread.start();
					}
					else {
						if (_DEBUG) _logger.debug("DEB: preQA6a: Cache: " + cachedDocUrl + " , " + cachedVal);						
					}
					// Launch thread
					_asyncRequestsPerQuery.add(requestOverview);
				}//TESTED (by hand)
				else {
					
					if (isComplexSource(endpoint.parentSource)) {
						
						//DEBUG
						if (_DEBUG) _logger.debug("DEB: preQA6ba: Build complex source, num requests = " + endpoint.requests.size());								
						
						FederatedRequest requestOverview = new FederatedRequest();
						requestOverview.endpointInfo = endpoint;
						requestOverview.communityIdStrs = communityIdStrs;
						requestOverview.requestParameter = entityValue;
						requestOverview.queryIndex = entityIndex;
						requestOverview.mergeKey = endpoint.parentSource.getKey();
						requestOverview.cachedDoc_expired = cachedDoc_expired;	
						
						requestOverview.importThread = new FederatedSimpleHarvest();
						requestOverview.importThread.queryEngine = this;
						requestOverview.importThread.request = requestOverview;
						requestOverview.importThread.start();						
						
						// Launch thread
						_asyncRequestsPerQuery.add(requestOverview);						
					}
					else { // simple source					
						try {
							for (SourceFederatedQueryConfigPojo.FederatedQueryEndpointUrl request: endpoint.requests) {																								
								FederatedRequest requestOverview = createSimpleHttpEndpoint_includingCache(
																	entityValue, entityIndex, communityIdStrs, 
																		endpoint, request, cachedDoc_expired);
								
								//DEBUG
								if (_DEBUG) _logger.debug("DEB: preQA6bb: Build request: " + request.endPointUrl);								
								
								_asyncRequestsPerQuery.add(requestOverview);
							}//(end loop over multiple requests
						}
						catch (Exception e) {
							_logger.error("Unknown error creating federated query for " + endpoint.titlePrefix + ": " + e.getMessage());
							if (_testMode) {
								throw new RuntimeException("Unknown error creating federated query for " + endpoint.titlePrefix + ": " + e.getMessage(), e);
							}
						}
					}//(end if simple not complex)
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
			BasicDBList bsonArray = new BasicDBList();
			PeekingIterator<FederatedRequest> it = Iterators.peekingIterator(_asyncRequestsPerQuery.iterator());
			while (it.hasNext()) {
				// loop state:
				BasicDBObject[] docOrDocs = new BasicDBObject[1];
				docOrDocs[0] = null;
				
				FederatedRequest request = it.next();
				boolean isComplexSource = isComplexSource(request.endpointInfo.parentSource);
				if (null == request.cachedDoc) { // no cached doc, simple source processing (OR ANY COMPLEX CASE BY CONSTRUCTION)
					try {
						if ((null == request.cachedResult) || isComplexSource) {	// no cached api response, or complex			
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
										//(carry on)
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
								else if (isComplexSource) {
									//DEBUG 
									if (_DEBUG) _logger.debug("DEB: postQA0: " + request.complexSourceProcResults.size());

									handleComplexDocCaching(request, _cacheMode, _scoreStats);									
									
									// Get a list of docs
									docOrDocs = ((BasicDBList)DocumentPojo.listToDb(request.complexSourceProcResults, DocumentPojo.listType())).toArray(new BasicDBObject[0]);

									// (_API_ caching is exactly the same between cache and non-cache cases)
									// (note that if null != complexSourceProcResults then follows that null != scriptResult)
									String url = buildScriptUrl(request.mergeKey, request.queryIndex);
									
									if (!(request.importThread instanceof FederatedSimpleHarvest) && _cacheMode) { // (don't cache python federated queries in test mode)
										// (simple harvest caching is done separately)
										this.cacheApiResponse(url, request.scriptResult, request.endpointInfo);
									}									
								}//TESTED (by hand - single and multiple doc mode)					
								else if (null == request.scriptResult) {								
									if (_testMode) {
										throw new RuntimeException("Script mode: no cached result found from: " + request.requestParameter);
									}
								}
								else {
									// (_API_ caching is exactly the same between cache and non-cache cases)
									String url = buildScriptUrl(request.mergeKey, request.queryIndex);
									if (_cacheMode) { // (don't cache python federated queries in test mode)
										this.cacheApiResponse(url, request.scriptResult, request.endpointInfo);
									}
									bsonArray.add(request.scriptResult);								
								}
							} // end script mode
							else { // HTTP mode (also: must be simple source builder)
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
									bson = new BasicDBObject(SimpleFederatedCache.array_, bsonUnknownType);
								}
								else if (bsonUnknownType instanceof String) {
									bson = new BasicDBObject(SimpleFederatedCache.value_, bsonUnknownType);									
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
										
					if (null == docOrDocs[0]) {
						// (this next bit of logic can only occur in simple source cases by construction, phew)
						if (!it.hasNext() || (request.mergeKey != it.peek().mergeKey)) { // deliberate ptr arithmetic
							String url = buildScriptUrl(request.mergeKey, request.queryIndex);
							
							//DEBUG
							if (_DEBUG) _logger.debug("DEB: postQA3: " + url + ": " + bsonArray);					
							
							docOrDocs[0] = createDocFromJson(bsonArray, url, request, request.endpointInfo);
						}
					}
				} // (end if no cached doc)
				else { // cached doc, bypass lots of processing because no merging and doc already built (simple source processing)
					docOrDocs[0] = request.cachedDoc;
				}//TESTED (by hand)
					
				if (null != docOrDocs[0]) for (BasicDBObject doc: docOrDocs) {
					
					// Cache the document unless already cached (or caching disabled)
					if ((null == request.cachedDoc) && _cacheMode && !isComplexSource &&
							((null == request.endpointInfo.cacheTime_days) || (request.endpointInfo.cacheTime_days >= 0)))
					{
						simpleDocCache(request, doc);
					}//TESTED (by hand, 3 cases: cached not expired, cached expired first time, cached expired multiple times)
					
					if (!grabbedScores) {
						if (!docs.isEmpty()) {
							BasicDBObject topDoc = docs.get(0);
							aggregateSignif = topDoc.getDouble(DocumentPojo.aggregateSignif_, aggregateSignif);
							queryRelevance = topDoc.getDouble(DocumentPojo.queryRelevance_, queryRelevance);
							score = topDoc.getDouble(DocumentPojo.score_, score);
							grabbedScores = true;
							
							// OK would also like to grab the original matching entity, if it exists
							if (!isComplexSource) {
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
					}
					doc.put(DocumentPojo.aggregateSignif_, aggregateSignif);
					doc.put(DocumentPojo.queryRelevance_, queryRelevance);
					doc.put(DocumentPojo.score_, score);

					// Swap id and updateId, everything's been cached now:
					// Handle update ids vs normal ids:
					ObjectId updateId = (ObjectId) doc.get(DocumentPojo.updateId_);
					if (null != updateId) { // swap the 2...
						doc.put(DocumentPojo.updateId_, doc.get(DocumentPojo._id_));
						doc.put(DocumentPojo._id_, updateId);
					}//TESTED (by hand)				
					
					// If we're returning to a query then we'll adjust the doc format (some of the atomic fields become arrays)
					if (!_testMode) {
						convertDocToQueryFormat(doc, request.communityIdStrs);							
					}//TESTED (by hand)
					
					docs.add(0, doc);
					added++;
					//(doc auto reset at top of loop)
					
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
	
	public FederatedRequest createSimpleHttpEndpoint_includingCache(
			String entityValue, String entityIndex, String[] communityIdStrs,
			SourceFederatedQueryConfigPojo endpoint,
			SourceFederatedQueryConfigPojo.FederatedQueryEndpointUrl request,
			BasicDBObject cachedDoc_expired) throws IOException
	{
		AsyncHttpClient asyncHttpClient = new AsyncHttpClient();
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
		if (null != cachedVal) {
			if (checkIfNeedToClearCache(cachedVal, endpoint.parentSource)) {
				//DEBUG
				if (_DEBUG) _logger.debug("DEB: pre:CSHEb: Clear cache: " + url + " , " + cachedVal);
				
				cachedVal = null;
			}
		}		
		requestOverview.cachedResult = cachedVal;
		requestOverview.cachedDoc_expired = cachedDoc_expired;
		
		if (null == cachedVal) {
			requestOverview.responseFuture = asyncRequest.execute();						
		}					
		else {
			//DEBUG
			if (_DEBUG) _logger.debug("DEB: pre:CSHEb: Cache: " + url + " , " + cachedVal);
			
			requestOverview.responseFuture = null;
			asyncHttpClient.close();
		}
		return requestOverview;
	}
	
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
								ent.put(EntityPojo.significance_, 10.0); // (ie relative to this query)
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
			endpointCacheCollection.createIndex(new BasicDBObject(SimpleFederatedCache.expiryDate_, 1));
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
		toCacheObj.put(SimpleFederatedCache.created_, new Date());
		endpointCacheCollection.save(toCacheObj);
	}//TESTED (3.1, 4.*)

	// Document level caching, although it effectively serves as a mostly redundant request cache,
	// It's actually used to allow users to save federated query documents in their buckets	
	
	public static void simpleDocCache(FederatedRequest request, BasicDBObject doc) {
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
		else if (null == request.cachedDoc) { // if no currently cached doc, simply save what we have
			//DEBUG
			if (_DEBUG) _logger.debug("DEB: postQA4b: cached ... " + doc);					
			DbManager.getDocument().getMetadata().save(doc);
		}		
		// (else already have a valid cached doc so nothing to do)
	}
	
	public static boolean checkDocCache_isExpired(BasicDBObject cachedDoc, SourceFederatedQueryConfigPojo endpoint) {
		if (null == endpoint.cacheTime_days)
			endpoint.cacheTime_days = DEFAULT_CACHE_TIME_DAYS;  
		
		Date now = new Date();
		long cacheThreshold = cachedDoc.getDate(DocumentPojo.created_, now).getTime() + endpoint.cacheTime_days*3600L*24L*1000L;
		
		if (cacheThreshold < now.getTime()) // (ie doc-creation-time + cache is earlier than now => time to decache)
		{
			//DEBUG
			if (_DEBUG) _logger.debug("DEB: preQA6zz: Cache expired: " + cachedDoc.getString(DocumentPojo.url_) + ": " + new Date(cacheThreshold) + " vs " + now);						
			
			return true;
		}
		else 
			return false;
	}//TESTED
	
	private static BasicDBObject getCachedApiResponse(String scriptResult) {
		Object parsedScriptResult;
		BasicDBObject outResult;
		try {
			parsedScriptResult = com.mongodb.util.JSON.parse(scriptResult);
		}
		catch (Exception e) {
			throw new RuntimeException("Error deserializing " + scriptResult + ": " + e.getMessage(), e);			
		}
		if (parsedScriptResult instanceof BasicDBObject) {
			outResult = (BasicDBObject) parsedScriptResult;
		}
		else if (parsedScriptResult instanceof BasicDBList) {
			outResult = new BasicDBObject(SimpleFederatedCache.array_, parsedScriptResult);
		}
		else if (parsedScriptResult instanceof String) {
			outResult = new BasicDBObject(SimpleFederatedCache.value_, parsedScriptResult);
		}
		else {
			throw new RuntimeException("Error deserializing " + scriptResult + ": " + parsedScriptResult);
		}
		try {
			MongoDbUtil.enforceTypeNamingPolicy(outResult, 0);
		}
		catch (Exception ee) {
			throw new RuntimeException("Error deserializing " + scriptResult + ": " + ee.getMessage(), ee);
		}			
		return outResult;
	}//TESTED (c/p from tested code though I deleted the test cases)
	
	private static boolean isComplexSource(SourcePojo src) {
		return (null != src.getProcessingPipeline()) && (src.getProcessingPipeline().size() > 1);		
	}

	private static boolean checkIfNeedToClearCache(BasicDBObject cachedVal, SourcePojo src)
	{
		BasicDBObject cachedJson = (BasicDBObject) cachedVal.get(SimpleFederatedCache.cachedJson_);
		if (null == cachedJson) {
			return true; // (corrupt cache)
		}
		boolean isComplexSrc = isComplexSource(src);
		if (isComplexSrc) { // check API response
			Date createdDate = (Date) cachedVal.get(SimpleFederatedCache.created_);
			if (null == createdDate) {
				return true; // (needs date for doc caching code so just remove from cache and re-create)
			}
		}
		if ((1 == cachedJson.size()) && (cachedJson.get(SimpleFederatedCache.__infinite__value_) instanceof String)) {
			// ie if complex source, return false - no need to clear cache
			return !isComplexSrc; 
		}
		else { // opposite
			return isComplexSrc; 			
		}
	}//TESTED (by hand - all 4 combos)
		
	///////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////

	// (my own threading)

	public abstract class FederatedHarvest extends Thread {
		public FederatedRequest request;
		public SimpleFederatedQueryEngine queryEngine;		
	}	
	public class FederatedJythonHarvest extends FederatedHarvest {
		
		public void run() {
			String scriptResult = null;
			LinkedList<String> debugLog = queryEngine._testMode ? new LinkedList<String>() : null;
			try {
				if (null != request.cachedResult) { // we have a cached value but are in complex mode so can't immediately 
					BasicDBObject cachedJson = (BasicDBObject) request.cachedResult.get(SimpleFederatedCache.cachedJson_);
					scriptResult = (String) cachedJson.get(SimpleFederatedCache.__infinite__value_);
				}
				else {				
					scriptResult = queryEngine.performImportPythonScript(request.endpointInfo.importScript, request.requestParameter, request.fullQuery, request.endpointInfo.parentSource.getOwnedByAdmin(), debugLog);
				}
			}
			catch (Exception e) {
				request.errorMessage = e;
				return;
			}
			try {
				if (isComplexSource(request.endpointInfo.parentSource)) {
					DocumentPojo doc = new DocumentPojo();
					
					// Fields that are specific to the federated query type:
					doc.setFullText(scriptResult);
					doc.setUrl(buildScriptUrl(request.mergeKey, request.queryIndex));
	
					// (always cache complex source pipeline results like this)
					request.scriptResult = new BasicDBObject(SimpleFederatedCache.__infinite__value_, scriptResult); 
					if ((null != debugLog) && !debugLog.isEmpty()) {
						request.scriptResult.put("$logs", debugLog);
					}						
					handleComplexDocProcessing(doc, request, _cacheMode);				
				}
				else { // simple case:
					// In this simple case, the output has to be JSON
					request.scriptResult = getCachedApiResponse(scriptResult);
					if ((null != debugLog) && !debugLog.isEmpty()) {
						request.scriptResult.put("$logs", debugLog);
					}
				}//(END simple vs complex post processing case)
			}
			catch (Exception e) {
				request.errorMessage = e;
				return;
			}			
		}
	}//TESTED (by hand)
	
	public class FederatedSimpleHarvest extends FederatedHarvest {
		public void run() {

			// 1) Kick off all the requests asynchronously
			
			BasicDBList bsonArray = new BasicDBList();
			StringBuffer sb = new StringBuffer();
			try {
				LinkedList<FederatedRequest> asyncRequestsPerQuery = new LinkedList<FederatedRequest>();
				
				for (SourceFederatedQueryConfigPojo.FederatedQueryEndpointUrl httpRequest: request.endpointInfo.requests) {																								
					FederatedRequest requestOverview = createSimpleHttpEndpoint_includingCache(
														request.requestParameter, request.queryIndex, request.communityIdStrs, 
														request.endpointInfo, httpRequest, null);
					
					if (_DEBUG) _logger.debug("DEB: FederatedSimpleHarvest: Build request: " + httpRequest.endPointUrl);								
					
					asyncRequestsPerQuery.add(requestOverview);
					
				}//(end loop over multiple requests
				//TESTED (by hand)
			
				// 2) Combine the results (waiting asyncronously for the results) - see the mergeKey code equivalent for simple queries
				
				while (!asyncRequestsPerQuery.isEmpty()) {
					Iterator<FederatedRequest> it = asyncRequestsPerQuery.iterator();
					while (it.hasNext()) {
						FederatedRequest requestOverview = it.next();
						if (null != requestOverview.cachedResult) { // cached result, note must be in format cachedJson.__infinite__value (else will have been discarded)
						
							BasicDBObject jsonCache = (BasicDBObject) requestOverview.cachedResult.get(SimpleFederatedCache.cachedJson_);
							if (null != jsonCache) {
								String s = (String) jsonCache.get(SimpleFederatedCache.__infinite__value_);
								if (null != s) {
									//DEBUG
									if (_DEBUG) _logger.debug("DEB: FederatedSimpleHarvest: found cached element: " + requestOverview.cachedResult);
									
									bsonArray.add(s);
									sb.append(s).append("\n\n");
									
									it.remove();
									continue;
								}
							}
						}//TESTED (by hand)
						
						//IF HERE THEN CACHE DIDN'T EXIST OR FAILED (see continue above)
						if (requestOverview.responseFuture.isDone()) {
							it.remove();
							
							Response endpointResponse = requestOverview.responseFuture.get();
							requestOverview.asyncClient.close();
							requestOverview.asyncClient = null;
							String jsonStr = endpointResponse.getResponseBody();
							String url = endpointResponse.getUri().toURL().toString();
							
							//DEBUG
							if (_DEBUG) _logger.debug("DEB: FederatedSimpleHarvest: found new element: " + url + " = " + jsonStr);													
							
							BasicDBObject bson = new BasicDBObject(SimpleFederatedCache.__infinite__value_, jsonStr);							
							cacheApiResponse(url, bson, request.endpointInfo);
							
							bsonArray.add(jsonStr);
							sb.append(jsonStr).append("\n\n");
						}//TESTED (by hand)
					}
					if (!asyncRequestsPerQuery.isEmpty()) { // wait 100ms to stop thrashing
						try { Thread.sleep(100); } catch (Exception e) {}
					}
				} //TESTED (by hand)
			}
			catch (Exception e) {
				request.errorMessage = e;
				return;
			}
			
			// 3) Run the document processing
			
			DocumentPojo doc = new DocumentPojo();
			
			// Fields that are specific to the federated query type:
			doc.setFullText(sb.toString());
			if (bsonArray.size() > 1) {
				doc.addToMetadata("__FEDERATED_REPLIES__", bsonArray.toArray());
			}
			doc.setUrl(buildScriptUrl(request.mergeKey, request.queryIndex));

			// (always cache complex source pipeline results like this)
			handleComplexDocProcessing(doc, request, _cacheMode);				
		}
	}
	
	public class FederatedScriptHarvest extends FederatedHarvest {
		public void run() {
			String scriptResult = null;
			try {
				if (null != request.cachedResult) { // we have a cached value but are in complex mode so can't immediately 
					BasicDBObject cachedJson = (BasicDBObject) request.cachedResult.get(SimpleFederatedCache.cachedJson_);
					scriptResult = (String) cachedJson.get(SimpleFederatedCache.__infinite__value_);
				}
				else { // Use the TextExtractorExternalScript function:
					
					TextExtractorExternalScript extractor = new TextExtractorExternalScript();
					LinkedHashMap<String, String> dummyExtractorOptions = new LinkedHashMap<String, String>();
					String[] args = request.endpointInfo.importScript.split("\\s+");
					int i = 0;
					for (String arg: args) {
						arg = arg.replace("$1", request.requestParameter);
						if (0 == i) {
							dummyExtractorOptions.put("script", arg);
						}
						else {
							dummyExtractorOptions.put("arg" + i, arg);
						}
						i++;
					}
					if (null != request.endpointInfo.queryTimeout_secs) {
						dummyExtractorOptions.put("timeout", Long.toString(request.endpointInfo.queryTimeout_secs.longValue()*1000L));
							//(convert to ms then to string)
					}
					SourcePojo dummySrc = new SourcePojo();
					dummySrc.setOwnerId(request.endpointInfo.parentSource.getOwnerId());
					dummySrc.setExtractorOptions(dummyExtractorOptions);
					dummySrc.setCommunityIds(request.endpointInfo.parentSource.getCommunityIds());
					DocumentPojo dummyDoc = new DocumentPojo();
					dummyDoc.setTempSource(dummySrc);
					extractor.extractText(dummyDoc);
					scriptResult = dummyDoc.getFullText();
					if (null == scriptResult) {
						request.errorMessage = new RuntimeException("Unknown problem, script didn't return text: " + dummyExtractorOptions.toString());
						return;						
					}
				}
			}
			catch (Exception e) {
				request.errorMessage = e;
				return;
			}			
			try {
				if (isComplexSource(request.endpointInfo.parentSource)) {
					DocumentPojo doc = new DocumentPojo();
					
					// Fields that are specific to the federated query type:
					doc.setFullText(scriptResult);
					doc.setUrl(buildScriptUrl(request.mergeKey, request.queryIndex));
	
					// (always cache complex source pipeline results like this)
					request.scriptResult = new BasicDBObject(SimpleFederatedCache.__infinite__value_, scriptResult); 
					handleComplexDocProcessing(doc, request, _cacheMode);				
				}
				else { // simple case:
					// In this simple case, the output has to be JSON
					request.scriptResult = getCachedApiResponse(scriptResult);
				}//(END simple vs complex post processing case)
			}
			catch (Exception e) {
				request.errorMessage = e;
				return;
			}			
		}
	}
	
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

	public static void handleComplexDocProcessing(DocumentPojo doc, FederatedRequest request, boolean cacheMode) {
		List<DocumentPojo> docWrapper = null;
		if (null != request.cachedResult) { // API result was cached, so docs might be
			Date createdDate = request.cachedResult.getDate(SimpleFederatedCache.created_);
			if ((null != request.endpointInfo.parentSource.getModified()) && (request.endpointInfo.parentSource.getModified().getTime() > createdDate.getTime())) {
				
				//DEBUG
				if (_DEBUG) _logger.debug("DEB: HCDP0: cache out of date, src=" + request.endpointInfo.parentSource.getModified() + " cache=" + createdDate);					
								
				request.cachedResult = null; // (clear cache)
			}
			if (null != request.cachedResult) { // see if we can get the docs from the DB instead of from the harvester  
				BasicDBObject query = new BasicDBObject(DocumentPojo.sourceKey_, request.endpointInfo.parentSource.getKey());
				query.put(DocumentPojo.sourceUrl_, doc.getUrl());
				DBCursor dbc = DbManager.getDocument().getMetadata().find(query);
				if (dbc.hasNext()) {
					docWrapper = DocumentPojo.listFromDb(dbc, DocumentPojo.listType());
					if (null == docWrapper) { // (shouldn't ever happen)
						request.cachedResult = null;
					}
					else {
						//DEBUG
						if (_DEBUG) _logger.debug("DEB: HCDP2: cache, found docs=" + docWrapper.size());						
					}
				}
				else { // last chance to clear cache
					//DEBUG
					if (_DEBUG) _logger.debug("DEB: HCDP3: empty cache from " + query);
					
					request.cachedResult = null;
				}
			}
		}//TESTED (by hand)
		
		// Need to set this 1) to avoid the harvest controller from deduplicating and 2) so i can retrieve those docs
		if (null == docWrapper) { // Go ahead and harvest these docs
			doc.setSourceUrl(doc.getUrl());
			
			doc.setId(new ObjectId());
			doc.setSourceKey(request.endpointInfo.parentSource.getKey());
			doc.setSource(request.endpointInfo.parentSource.getTitle());
			doc.setMediaType(request.endpointInfo.parentSource.getMediaType());
			doc.setTitle(request.endpointInfo.titlePrefix);
			Date d = new Date();
			doc.setCreated(d);
			doc.setModified(d);
			docWrapper = new ArrayList<DocumentPojo>(1);
			ArrayList<DocumentPojo> dummyDocs = new ArrayList<DocumentPojo>(0);
			docWrapper.add(doc);
			
			try {
				HarvestController hc = new HarvestController();
				HarvestControllerPipeline hcp = new HarvestControllerPipeline();
				hcp.extractSource_preProcessingPipeline(request.endpointInfo.parentSource, hc);
				hcp.enrichSource_processingPipeline(request.endpointInfo.parentSource, docWrapper, dummyDocs, dummyDocs);
				
				//DEBUG
				if (_DEBUG) _logger.debug("DEB: HCDP4: created " + docWrapper.size() + " doc(s)");					
			} 
			catch (IOException e) {
				throw new RuntimeException("Complex source processing", e);
			}
		}
		request.complexSourceProcResults = docWrapper;
	}
	
	public static void handleComplexDocCaching(FederatedRequest request, boolean cacheMode, ScoringUtils scoreStats) {

		List<DocumentPojo> docWrapper = request.complexSourceProcResults;

		//In non-test mode .. Go through the list of docs and work out what the deal is with caching, ie remove docs + add update ids
		//Also go through and set default scores for any entities that haven't been scored based on existing docs
		BasicDBObject query = new BasicDBObject(DocumentPojo.sourceKey_, request.endpointInfo.parentSource.getKey());
		BasicDBObject fields = new BasicDBObject(DocumentPojo.updateId_, 1); // (ie _id and updateId only)
		String srcUrl = null;
		for (DocumentPojo outDoc: docWrapper) {
			if (null == srcUrl) {
				srcUrl = outDoc.getSourceUrl();
			}
			// Always make the text non-transient, so gets stored
			outDoc.makeFullTextNonTransient();
			if (null == outDoc.getId()) {
				outDoc.setId(new ObjectId());
			}
			
			if (cacheMode && (null == request.cachedResult)) { // (if result not previously cached)
				// Step 1: deduplication
				query.put(DocumentPojo.url_, outDoc.getUrl());
				BasicDBObject outVal = (BasicDBObject) DbManager.getDocument().getMetadata().findOne(query, fields);
				if (null != outVal) {
					//DEBUG
					if (_DEBUG) _logger.debug("DEB: HCDC1: update cache from : " + outVal + " for " + outDoc.getUrl());			

					// Use updateId if it exists, otherwise _id
					ObjectId updateId = outVal.getObjectId(DocumentPojo.updateId_);
					if (null == updateId) {
						updateId = outVal.getObjectId(DocumentPojo._id_);
					}
					outDoc.setUpdateId(updateId);
				}
			}//TESTED (by hand - single and multiple docs mode)
			
			// Step 2: add fake scores to all the entities that didn't get scores from the aggregation manager
			if (null != outDoc.getEntities()) for (EntityPojo ent: outDoc.getEntities()) {
				boolean fakeStats = true;
				if (null != scoreStats) {
					if (scoreStats.fillInEntityStatistics(ent)) {
						fakeStats = false;
					}
				}
				if (fakeStats) {
					ent.setDoccount(1L);
					ent.setTotalfrequency(1L);
					ent.setDatasetSignificance(10.0);
					ent.setSignificance(10.0);
					ent.setQueryCoverage(100.0);
				}
				//DEBUG
				if (_DEBUG) _logger.debug("DEB: HCDC2: entity: " + ent.getIndex() + " , sig=" + ent.getDatasetSignificance());			
			}//TESTED
			
			if (null != outDoc.getAssociations()) for (AssociationPojo assoc: outDoc.getAssociations()) {
				assoc.setAssoc_sig(10.0);
				assoc.setDoccount(1L);
			}
		}//TESTED (by hand - overlapping and non-overlapping case)
		
		if (cacheMode && (null == request.cachedResult)) { // (if result not previously cached)
			//Remove old docs now we have new ones
			DbManager.getDocument().getMetadata().remove(query); // remove everything with this specific URL (ie simple source)
			query.remove(DocumentPojo.url_);
			query.put(DocumentPojo.sourceUrl_, srcUrl);
			DbManager.getDocument().getMetadata().remove(query); // remove everything with this specific _source_ URL (ie docs generated from this URL)
			
			// Now cache all the existing docs:
			
			@SuppressWarnings("unchecked")
			ArrayList<Object> tmpDocList = (ArrayList<Object>) DocumentPojo.listToDb(docWrapper, DocumentPojo.listType());
			
			DbManager.getDocument().getMetadata().insert(tmpDocList.toArray(new BasicDBObject[0]));		
			
			//DEBUG
			if (_DEBUG) _logger.debug("DEB: HCDC3: remove/insert cache: " + query.toString());			
		}//TESTED (by hand - single and multiple docs)
		
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
			"{ \"displayUrl\" : \"http://test3_1\" , \"url\" : \"inf://federated/fakeendpoint.123/test3_1/testentityin\" , \"sourceKey\" : [ \"fakeendpoint.123\"] , \"source\" : [ \"fakeendpoint\"] , \"communityId\" : [ \"4c927585d591d31d7b37097a\"] , \"mediaType\" : [ \"Report\"] , \"metadata\" : { \"json\" : [ { \"test\" : { \"field\" : [ \"test3_1\" , \"test3_1\"] , \"field2\" : \"http://test3_1\"}}]} , \"title\" : \"fake endpoint: : test3_1: test3_1\" , \"entities\" : [ { \"disambiguated_name\" : \"test3_1\" , \"type\" : \"TestEntityOut\" , \"dimension\" : \"What\" , \"relevance\" : 1.0 , \"doccount\" : 1 , \"averageFreq\" : 1.0 , \"datasetSignificance\" : 10.0 , \"significance\" : 10.0 , \"frequency\" : 1.0 , \"index\" : \"test3_1/testentityout\" , \"queryCoverage\" : 100.0 , \"totalfrequency\" : 1.0}] , \"description\" : \"[\\n  {\\n    \\\"test\\\": {\\n      \\\"field\\\": [\\n        \\\"test3_1\\\",\\n        \\\"test3_1\\\"\\n      ],\\n      \\\"field2\\\": \\\"http://test3_1\\\"\\n    }\\n  }\\n]\" , \"aggregateSignif\" : 115.0 , \"queryRelevance\" : 105.0 , \"score\" : 110.0}";
		
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
