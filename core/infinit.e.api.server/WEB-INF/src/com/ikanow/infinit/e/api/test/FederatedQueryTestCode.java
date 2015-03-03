/*******************************************************************************
 * Copyright (c) 2013, Ikanow LLC and/or its affiliates.  All rights reserved
 * 
 * This file is released subject to the terms of a commercial license agreement with Ikanow.
 * This file may be utilized only under the terms of that commercial license agreement.
 * The terms of any other license, commercial or open source, do not apply to this file.
 ******************************************************************************/
package com.ikanow.infinit.e.api.test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;

import org.apache.log4j.Logger;
import org.bson.types.ObjectId;

import com.ikanow.infinit.e.api.knowledge.federated.SimpleFederatedQueryEngine;
import com.ikanow.infinit.e.data_model.Globals;
import com.ikanow.infinit.e.data_model.api.BasePojoApiMap;
import com.ikanow.infinit.e.data_model.api.ResponsePojo;
import com.ikanow.infinit.e.data_model.api.knowledge.AdvancedQueryPojo;
import com.ikanow.infinit.e.data_model.store.config.source.SimpleFederatedCache;
import com.ikanow.infinit.e.data_model.store.config.source.SourceFederatedQueryConfigPojo;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.mongodb.BasicDBObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class FederatedQueryTestCode {

	static Logger _logger = Logger.getLogger(SimpleFederatedQueryEngine.class);
	
	///////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////
	
	// TEST CODE
	// Summary of test code:
	// 1.1] Query ignored because has any term other than entity/date
	// 1.2] Query ignored because date only
	// 1.3] Query ignored because logic too complex
	// 1.3a) OR
	// 1.3b) NOT
	// 1.4] Query ignored because multiple entities 
	// 1.5] Single entity - not ignored (entity type and value)
	// 1.6] Single entity and date - not ignored (entity index)
	// 1.6a) single date
	// 1.6b) multiple dates
	// 1.6c) different orders
	// 2.1] Query ignored because no related entity types
	// 3.1] Test non-cached query
	// 4.1] Test cached query
	// 4.2] Test expired then cached query
	// 4.3] Test cached query - bulk remove	
	
	// Things to be added to the testing:
	// - JsonPath expressions (think I wrote some test code somewhere for this already?)
	// - Difference between test mode and non-test mode
	// - 2 different levels of caching
	// - Python handling
	// - Admin vs non-admin security checks
	// - Timeouts
	// - TODO: Entity deduplication
	
	public static void main(String[] args) throws Exception {

		//TODO: need to add (essentially identical) test cases for:
		// - python script
		// - "external" script
		// - full source pipeline functionality 
		
		// SETUP:
		
		System.out.println(Arrays.toString(args));
		Globals.setIdentity(com.ikanow.infinit.e.data_model.Globals.Identity.IDENTITY_API);
		Globals.overrideConfigLocation(args[0]);		
				
		// dummy HTTP server
		TestServer testServer = new TestServer();

		// Fake endpoint (points to dummy server)
		
		SourceFederatedQueryConfigPojo fakeEndpoint = new SourceFederatedQueryConfigPojo();
		fakeEndpoint.cacheTime_days = 2;
		HashMap<String, String> docConversionMap = new HashMap<String, String>(); 
		docConversionMap.put("test:field","TestEntityOut");
		docConversionMap.put("test:field2","displayUrl");
		fakeEndpoint.docConversionMap = docConversionMap; // string,string
		fakeEndpoint.requests = new ArrayList<SourceFederatedQueryConfigPojo.FederatedQueryEndpointUrl>(1);
		SourceFederatedQueryConfigPojo.FederatedQueryEndpointUrl endpoint = new SourceFederatedQueryConfigPojo.FederatedQueryEndpointUrl();
		endpoint.endPointUrl = "http://localhost:8186/test/$1/";
		LinkedHashMap<String, String> paramMap = new LinkedHashMap<String, String>(); 
		paramMap.put("param1", "$1");
		endpoint.urlParams = paramMap;
		fakeEndpoint.requests.add(endpoint);
		
		HashSet<String> entityTypes = new HashSet<String>();
		entityTypes.add("testentityin");
		fakeEndpoint.entityTypes = entityTypes; // set<string>
		fakeEndpoint.parentSource = new SourcePojo();
		fakeEndpoint.parentSource.setKey("fakeendpoint.123");
		fakeEndpoint.parentSource.setOwnedByAdmin(true);
		fakeEndpoint.parentSource.setTitle("fakeendpoint");
		fakeEndpoint.parentSource.setMediaType("Report");
		fakeEndpoint.titlePrefix = "fake endpoint: ";

		SimpleFederatedQueryEngine queryEngine = new SimpleFederatedQueryEngine();
		
		SimpleFederatedQueryEngine.TEST_MODE_ONLY = true;		
		SimpleFederatedCache.QUERY_FEDERATION_CACHE_CLEANSE_SIZE = 10; // (for 4.2 - override max size after which will intermittently cleanse cache)
		
		// CLEAR (TEST) CACHES:
		queryEngine.test_cacheClear(true, true, fakeEndpoint.parentSource.getKey());
		
		//TEST CASES:
		try {
			
			//(use this above the appropriate failing test to get debug info:)
			//queryEngine.setTestMode(true);			
			
			queryEngine.addEndpoint(fakeEndpoint);
			String[] communityIdStrs = new String[1];
			communityIdStrs[0] = "4c927585d591d31d7b37097a";
			AdvancedQueryPojo query = new AdvancedQueryPojo();
			AdvancedQueryPojo.QueryTermPojo qtText, qtDate, qtEntVal, qtEntIndex;		
			
			// 1.1] Query ignored because has any term other than entity/date
			query.qt = new ArrayList<AdvancedQueryPojo.QueryTermPojo>(1);
			qtText = new AdvancedQueryPojo.QueryTermPojo();
			qtText.etext="fail";
			query.qt.add(qtText);
			query.logic = null;
			queryEngine.preQueryActivities(new ObjectId(), query, communityIdStrs);
			queryEngine.test_CheckIfQueryLaunched("1.1a", false);
			queryEngine.test_queryClear(true);
			//(proper Test-Driven code would require all the types, but we know we're actually checking for anything except entity/date)
			
			// 1.2] Query ignored because date only
			query.qt = new ArrayList<AdvancedQueryPojo.QueryTermPojo>(1);
			qtDate = new AdvancedQueryPojo.QueryTermPojo();
			qtDate.time = new AdvancedQueryPojo.QueryTermPojo.TimeTermPojo();
			qtDate.time.max="now";
			qtDate.time.min="now-3d";
			query.qt.add(qtDate);
			query.logic = null;
			queryEngine.preQueryActivities(new ObjectId(), query, communityIdStrs);
			queryEngine.test_CheckIfQueryLaunched("1.2a", false);
			queryEngine.test_queryClear(true);		
			
			// 1.3] Query ignored because logic too complex
			// 1.3a) OR
			query.qt = new ArrayList<AdvancedQueryPojo.QueryTermPojo>(2);
			qtEntVal = new AdvancedQueryPojo.QueryTermPojo();
			qtEntVal.entityValue = "test1_3a";
			qtEntVal.entityType = "TestEntityIn";
			query.qt.add(qtEntVal);
			query.qt.add(qtDate);
			query.logic = "1 OR 2";
			queryEngine.preQueryActivities(new ObjectId(), query, communityIdStrs);
			queryEngine.test_CheckIfQueryLaunched("1.3a", false);
			queryEngine.test_queryClear(true);
			// 1.3b) NOT
			query.qt = new ArrayList<AdvancedQueryPojo.QueryTermPojo>(2);
			qtEntVal = new AdvancedQueryPojo.QueryTermPojo();
			qtEntVal.entityValue = "test1_3b";
			qtEntVal.entityType = "TestEntityIn";
			query.qt.add(qtEntVal);
			query.qt.add(qtDate);
			query.logic = "NOT (1 AND 2)";
			queryEngine.preQueryActivities(new ObjectId(), query, communityIdStrs);
			queryEngine.test_CheckIfQueryLaunched("1.3b", false);
			queryEngine.test_queryClear(true);
			
			// 1.4] Query ignored because multiple entities 
			query.qt = new ArrayList<AdvancedQueryPojo.QueryTermPojo>(2);
			qtEntVal = new AdvancedQueryPojo.QueryTermPojo();
			qtEntVal.entityValue = "test1_4_1";
			qtEntVal.entityType = "TestEntityIn";
			qtEntIndex = new AdvancedQueryPojo.QueryTermPojo();
			qtEntIndex.entity = "test1_4_1/testentityin";
			query.qt.add(qtEntVal);
			query.qt.add(qtEntIndex);
			query.logic = null;
			queryEngine.preQueryActivities(new ObjectId(), query, communityIdStrs);
			queryEngine.test_CheckIfQueryLaunched("1.4a", false);
			queryEngine.test_queryClear(true);
			
			// 1.5] Single entity - not ignored (entity type and value)		
			query.qt = new ArrayList<AdvancedQueryPojo.QueryTermPojo>(1);
			qtEntVal = new AdvancedQueryPojo.QueryTermPojo();
			qtEntVal.entityValue = "test1_5a";
			qtEntVal.entityType = "TestEntityIn";
			query.qt.add(qtEntVal);
			query.logic = null;
			queryEngine.preQueryActivities(new ObjectId(), query, communityIdStrs);
			queryEngine.test_CheckIfQueryLaunched("1.5a", true);
			queryEngine.test_queryClear(true);
			
			// 1.6] Single entity and date - not ignored (entity index)
			// 1.6a) single date
			query.qt = new ArrayList<AdvancedQueryPojo.QueryTermPojo>(2);
			qtEntIndex = new AdvancedQueryPojo.QueryTermPojo();
			qtEntIndex.entity= "test1_6a/testentityin";
			query.qt.add(qtDate);
			query.qt.add(qtEntIndex);
			query.logic = "1 AND 2";
			queryEngine.preQueryActivities(new ObjectId(), query, communityIdStrs);
			queryEngine.test_CheckIfQueryLaunched("1.6a", true);
			queryEngine.test_queryClear(true);
			// 1.6b) multiple dates
			query.qt = new ArrayList<AdvancedQueryPojo.QueryTermPojo>(3);
			qtEntIndex = new AdvancedQueryPojo.QueryTermPojo();
			qtEntIndex.entity= "test1_6b/testentityin";
			query.qt.add(qtDate);
			query.qt.add(qtEntIndex);
			query.qt.add(qtDate);
			query.logic = "1 AND 2 AND 3";
			queryEngine.preQueryActivities(new ObjectId(), query, communityIdStrs);
			queryEngine.test_CheckIfQueryLaunched("1.6b", true);
			queryEngine.test_queryClear(true);
			// 1.6c) different orders
			query.qt = new ArrayList<AdvancedQueryPojo.QueryTermPojo>(2);
			qtEntVal = new AdvancedQueryPojo.QueryTermPojo();
			qtEntVal.entityValue = "test1_6c";
			qtEntVal.entityType = "TestEntityIn";
			query.qt.add(qtEntVal);
			query.qt.add(qtDate);
			query.logic = null;
			queryEngine.preQueryActivities(new ObjectId(), query, communityIdStrs);
			queryEngine.test_CheckIfQueryLaunched("1.6c", true);
			queryEngine.test_queryClear(true);
			
			// 2.1] Query ignored because no related entity types
			query.qt = new ArrayList<AdvancedQueryPojo.QueryTermPojo>(1);
			qtEntIndex = new AdvancedQueryPojo.QueryTermPojo();
			qtEntIndex.entity = "test2_1a/NOT_testentityin";
			query.qt.add(qtEntIndex);
			query.logic = null;
			queryEngine.preQueryActivities(new ObjectId(), query, communityIdStrs);
			queryEngine.test_CheckIfQueryLaunched("2.1a", false);
			queryEngine.test_queryClear(true);
			
			// 3.1] Test non-cached query (and that query result is added to the cache)
			query.qt = new ArrayList<AdvancedQueryPojo.QueryTermPojo>(1);
			qtEntVal = new AdvancedQueryPojo.QueryTermPojo();
			qtEntVal.entityValue = "test3_1";
			qtEntVal.entityType = "TestEntityIn";
			query.qt.add(qtEntVal);
			query.logic = null;
			ObjectId queryId = new ObjectId();
			queryEngine.preQueryActivities(queryId, query, communityIdStrs);
			//TODO: this deletes stuff (make it so it won't - should fallback that function to the clear code)
			//queryEngine.test_CheckIfQueryLaunched("3.1", true);
			ResponsePojo rp = new ResponsePojo();
			ArrayList<BasicDBObject> docs = new ArrayList<BasicDBObject>(1);
			BasicDBObject doc = new BasicDBObject();
			doc.put(DocumentPojo.aggregateSignif_, 115);
			doc.put(DocumentPojo.queryRelevance_, 105);
			doc.put(DocumentPojo.score_, 110);
			docs.add(doc);
			rp.setData(docs, (BasePojoApiMap<BasicDBObject>)null);
			queryEngine.postQueryActivities(queryId, docs, rp);
			queryEngine.test_CheckIfDocAdded("3.1", docs);
			// (don't clear from cache, next doc should return without making a request)
			
			// 3.2] Like 3.1 but with JsonPath
			// (clear cache here to ensure we don't just used the cached doc)
			queryEngine.test_cacheClear(true, true, fakeEndpoint.parentSource.getKey());
			
			query.qt = new ArrayList<AdvancedQueryPojo.QueryTermPojo>(1);
			qtEntVal = new AdvancedQueryPojo.QueryTermPojo();
			qtEntVal.entityValue = "test3_1";
			qtEntVal.entityType = "TestEntityIn";
			docConversionMap.remove("test:field");
			docConversionMap.remove("test:field2");
			docConversionMap.put("::field2","displayUrl");
			docConversionMap.put("::field", "TestEntityOut");
			query.qt.add(qtEntVal);
			query.logic = null;
			queryId = new ObjectId();
			queryEngine.preQueryActivities(queryId, query, communityIdStrs);
			//TODO: this deletes stuff (make it so it won't - should fallback that function to the clear code)
			//queryEngine.test_CheckIfQueryLaunched("3.1", true);
			rp = new ResponsePojo();
			docs = new ArrayList<BasicDBObject>(1);
			doc = new BasicDBObject();
			doc.put(DocumentPojo.aggregateSignif_, 115);
			doc.put(DocumentPojo.queryRelevance_, 105);
			doc.put(DocumentPojo.score_, 110);
			docs.add(doc);
			rp.setData(docs, (BasePojoApiMap<BasicDBObject>)null);
			queryEngine.postQueryActivities(queryId, docs, rp);
			queryEngine.test_CheckIfDocAdded("3.1", docs);
			// (don't clear from cache, next doc should return without making a request)
			
			// 4.1] Test cached query
			docs.remove(0);
			queryId = new ObjectId();
			queryEngine.preQueryActivities(queryId, query, communityIdStrs);
			queryEngine.test_CheckIfQueryLaunched("4.1", false); // (ie check no query added - because it was in the cache)
			queryEngine.postQueryActivities(queryId, docs, rp);
			queryEngine.test_CheckIfDocAdded("4.1", docs);
			// (don't clear from cache, next doc should check cache but remove expired value)
			
			// 4.2] Test expired then cached query
			// (currently need to clear the doc cache to make this work - see tests that need to be added properly, listed above)
			queryEngine.test_cacheClear(false, true, fakeEndpoint.parentSource.getKey());
			queryEngine.test_cacheExpire();
			docs.remove(0);
			queryId = new ObjectId();
			queryEngine.preQueryActivities(queryId, query, communityIdStrs);
			queryEngine.test_CheckIfQueryLaunched("4.2", true); // (ie check no query added - because it was in the cache)
			queryEngine.postQueryActivities(queryId, docs, rp);
			queryEngine.test_CheckIfDocAdded("4.2", docs);
			// (don't clear from cache, next doc should return without making a request)
			
			// 4.3] Test cached query - bulk remove
			// (currently need to clear the doc cache to make this work - see tests that need to be added properly, listed above)
			queryEngine.test_cacheClear(false, true, fakeEndpoint.parentSource.getKey());
			docs.remove(0);
			queryId = new ObjectId();
			queryEngine.test_cacheFill("4.3a", true, true);
			queryEngine.preQueryActivities(queryId, query, communityIdStrs);
			queryEngine.test_CheckIfQueryLaunched("4.3", false); // (ie check no query added - because it was in the cache)
			queryEngine.postQueryActivities(queryId, docs, rp);
			queryEngine.test_CheckIfDocAdded("4.3", docs);
			queryEngine.test_cacheFill("4.3b", false, false);
			queryEngine.test_queryClear(true);		
		}
		finally {
			System.out.println("All tests run, quitting in 5s.");
			Thread.sleep(5000);		
			testServer.stop();
		}
	}
	
	///////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////
	
	// Example web server for local testing
	// 
	
	public static class TestServer {

		HttpServer server;
		
	    public TestServer() throws Exception {
	        server = HttpServer.create(new InetSocketAddress(8186), 0);
	        server.createContext("/test", new MyHandler());
	        server.setExecutor(null); // creates a default executor
	        server.start();
	        Thread.sleep(2000); // (give the server a chance to start)
	    }

	    public void stop() {
	    	server.stop(0);
	    }
	    
	    static class MyHandler implements HttpHandler {
	    	private static HashMap<String, String> parseParamsFromGet(String params) {
	    		HashMap<String, String> paramMap = new HashMap<String, String>();
	    		if (null != params) {
	    			String[] params1 = params.split("[&]");
	    			for (String param: params1) {
	    				String[] params2 = param.split("[=]", 2);
	    				String val = "";
	    				if (params2.length > 1) {
	    					val = params2[1];
	    				}
	    				paramMap.put(params2[0], val);
	    			}
	    		}
	    		return paramMap;
	    	}//TESTED (by hand - who tests the tester?!)
	    	
	        public void handle(HttpExchange t) throws IOException {
	        	String response = null;
	        	int responseCode = 200;
	        	try {
	        		//DEBUG
	        		_logger.debug("DEB: " + t.getRequestURI().toString() + " / " + t.getRequestURI().getRawQuery());	        		

	        		HashMap<String, String> params = parseParamsFromGet(t.getRequestURI().getRawQuery());
		        	String param1 = params.get("param1");
		            response = "{ 'test': {'field': ['REPLACEME', 'REPLACEME'], 'field2': 'http://REPLACEME'} }";
		            response = response.replace("REPLACEME", param1);

		            String url = t.getRequestURI().toString();
		            if (!url.contains(param1)) { // ensure param and URL sub code works
		            	responseCode = 404;
		            }
	        	}
	        	catch (Exception e) {
	        		//DEBUG
	        		//e.printStackTrace();
	        		
	        		responseCode = 500;
	        		response = "{ 'err': '" + response.replace("'", "\'") + "'}";	        		
	        	}
	            t.sendResponseHeaders(responseCode, response.length());
	            OutputStream os = t.getResponseBody();
	            os.write(response.getBytes());
	            os.close();
	        }//TESTED (by hand - who tests the tester?!)
	    }
	}
}
