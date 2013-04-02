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
package com.ikanow.infinit.e.data_model.test;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bson.types.ObjectId;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.common.settings.ImmutableSettings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import com.ikanow.infinit.e.data_model.Globals;
import com.ikanow.infinit.e.data_model.Globals.Identity;
import com.ikanow.infinit.e.data_model.store.BaseDbPojo;
import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.social.community.CommunityPojo;
import com.ikanow.infinit.e.data_model.store.social.sharing.SharePojo;
import com.ikanow.infinit.e.data_model.api.ApiManager;
import com.ikanow.infinit.e.data_model.api.BaseApiPojo;
import com.ikanow.infinit.e.data_model.api.BasePojoApiMap;
import com.ikanow.infinit.e.data_model.api.ResponsePojo;
import com.ikanow.infinit.e.data_model.api.config.SourcePojoApiMap;
import com.ikanow.infinit.e.data_model.api.knowledge.AdvancedQueryPojo;
import com.ikanow.infinit.e.data_model.api.knowledge.DocumentPojoApiMap;
import com.ikanow.infinit.e.data_model.api.social.sharing.SharePojoApiMap;
import com.ikanow.infinit.e.data_model.index.ElasticSearchManager;
import com.ikanow.infinit.e.data_model.index.IndexManager;
import com.ikanow.infinit.e.data_model.index.document.DocumentPojoIndexMap;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.infinit.e.data_model.store.document.EntityPojo;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.MongoException;

public class TestCode {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws MongoException 
	 * @throws UnknownHostException 
	 */
	public static void main(String[] args) throws UnknownHostException, MongoException, IOException {

		System.out.println(Arrays.toString(args));
		Globals.setIdentity(Identity.IDENTITY_SERVICE);
		Globals.overrideConfigLocation(args[0]);
		
/////////////////////////////////////////////////////////////////////////////
		
// API testing:
		
		// API: Variable community source pojo...
		ResponsePojo rp1 = new ResponsePojo();
		SourcePojo sp = new SourcePojo();
		sp.setUrl("http://test");
		sp.setKey(sp.generateSourceKey());
		sp.addToCommunityIds(new ObjectId("a0000000000000000000000a"));
		sp.addToCommunityIds(new ObjectId("c0000000000000000000000c"));
		//CHECK THIS DOESN'T COMPILE
		//rp1.setData(sp); // (Not allowed SourcePojo isn't a BaseApiPojo)

		////////////////////////////////////////////////
		//CANONICAL EXAMPLE:
		Set<ObjectId> communities = new HashSet<ObjectId>();
		communities.add(new ObjectId("a0000000000000000000000a"));
		rp1.setData(sp, new SourcePojoApiMap(null, communities, communities));
		String sRPSingleObject = rp1.toApi();
		System.out.println("RPa=" + sRPSingleObject); // ("chris" removed, toApi handles RepsonsePojo specially)
		////////////////////////////////////////////////
		System.out.println("RPb=" + ResponsePojo.toApi(rp1, rp1.getMapper())); // ("chris" removed because of mapper)
		System.out.println("RPc=" + ResponsePojo.toApi(rp1)); // ("chris" removed, toApi handles RepsonsePojo specially)
		
		//API: Get an non-API object
		String sJson = "{ 'url':'http://test2', 'isApproved': false, 'harvestBadSource': true, 'created': 'Feb 14, 2013 9:24:34 PM' } ";
		//sp = BaseApiPojo.mapFromApi(sJson, SourcePojo.class, null);
		// Equivalent to:
		SourcePojo sp2 = ApiManager.mapFromApi(sJson, SourcePojo.class, new SourcePojoApiMap(null, new HashSet<ObjectId>(), new HashSet<ObjectId>()));
		System.out.println("RPd="+new Gson().toJson(sp2)); // "alex" and "chris" both removed
		
		//API: add a list to the response Pojo
		List<SourcePojo> list = Arrays.asList(sp, sp2);
		//CHECK THIS DOESN'T COMPILE
		//rp1.setData(list); // (Not allowed SourcePojo isn't a BaseApiPojo)

		sp2.addToCommunityIds(new ObjectId("a0000000000000000000000a")); // (alex will be allowed again)
		rp1.setData(list, new SourcePojoApiMap(null, communities, communities));
		String sRPList = rp1.toApi(); 
		sp2.setCommunityIds(null);
		
		//API:  And get as a list
		String listJson =  BaseApiPojo.getDefaultBuilder().create().toJson(rp1.getData());
		System.out.println("RP=" + listJson); // include "alex" and "chris" - no mapping applied
		
		////////////////////////////////////////////////
		//CANONICAL EXAMPLE:
		list = ApiManager.mapListFromApi(listJson, SourcePojo.listType(), null);
		////////////////////////////////////////////////
		System.out.println("SPL=" + BaseApiPojo.getDefaultBuilder().create().toJson(list)); // both "alex" and "chris", no mapping "from API"
		Set<SourcePojo> set = ApiManager.mapListFromApi(BaseApiPojo.getDefaultBuilder().create().toJson(rp1.getData()), new TypeToken<Set<SourcePojo>>(){}, null);
		System.out.println("SPS=" + BaseApiPojo.getDefaultBuilder().create().toJson(set)); // both "alex" and "chris", no mapping "from API"
		
		// API: finally transform to a JSON list (applies mapping)	
		try {
			System.out.println("SPJ=" + ApiManager.mapListToApi(set, new TypeToken<Set<SourcePojo>>(){}, new SourcePojoApiMap(null, null, communities)));
				// should fail because one of the communities does not have
			System.out.println("**********FAILED SHOULD HAVE THROWN SECURITY EXCEPTION");
		}
		catch (RuntimeException e) {
			// Add "communities" to object with missing val
			for (SourcePojo spSet: set) {
				if (null == spSet.getCommunityIds()) {
					spSet.setCommunityIds(communities);
				}
			}			
			// Try again:
			System.out.println("SPJ=" + ApiManager.mapListToApi(set, new TypeToken<Set<SourcePojo>>(){}, new SourcePojoApiMap(null, communities, communities)));
				// (just has "alex")			
		}
		
		// And now in the other direction, ie deserializing....
		ResponsePojo rpRecreated = ResponsePojo.fromApi(sRPSingleObject, ResponsePojo.class, SourcePojo.class, new SourcePojoApiMap(null, communities, communities));
		System.out.println("RECREATED RP_SRC=" + rpRecreated.toApi());
		System.out.println("RECREATED SRC(RP_SRC)=" + ((BasicDBObject)((SourcePojo)rpRecreated.getData()).toDb()).toString());
		rpRecreated = ResponsePojo.listFromApi(sRPList, ResponsePojo.class, SourcePojo.listType(), new SourcePojoApiMap(null, communities, communities));
		System.out.println("RECREATED RP_LSRC=" + rpRecreated.toApi());
		
		rpRecreated = ResponsePojo.fromApi(sRPSingleObject, ResponsePojo.class);
		System.out.println("RECREATED RAW(RP_SRC)=" + ((JsonElement)rpRecreated.getData()));
		sp = ApiManager.mapFromApi((JsonElement)rpRecreated.getData(), SourcePojo.class, new SourcePojoApiMap(null, communities, communities));
		System.out.println("RECREATED SRC(RAW(RP_SRC))=" + ((JsonElement)rpRecreated.getData()));
		
		// Real-life source pojo testing:
		// No longer needed - if commented in will fail because communities not assigned in mapTo/FromApi
//		BasicDBObject srcQuery = new BasicDBObject("useExtractor", "ModusOperandi");
//		SourcePojo fileSource = SourcePojo.fromDb(DbManager.getConfig().getSource().findOne(srcQuery), SourcePojo.class);
//		System.out.println("MODUS=" + ApiManager.mapToApi(fileSource, new SourcePojoApiMap(new HashSet<ObjectId>())));
//		ResponsePojo testRP = new ResponsePojo();
//		testRP.setData(fileSource, new SourcePojoApiMap(new HashSet<ObjectId>()));
//		System.out.println("MODUS2=" + testRP.toApi());		
//		srcQuery = new BasicDBObject("useExtractor", "none");
//		fileSource = SourcePojo.fromDb(DbManager.getConfig().getSource().findOne(srcQuery), SourcePojo.class);
//		System.out.println("SAH=" + ApiManager.mapToApi(fileSource, new SourcePojoApiMap(new HashSet<ObjectId>())));
//		testRP.setData(fileSource, new SourcePojoApiMap(new HashSet<ObjectId>()));
//		System.out.println("SAH2=" + testRP.toApi());
		
		//API: Get an API object
		////////////////////////////////////////////////
		//CANONICAL EXAMPLE:
		String sQueryJson = "{ 'logic': 'alex' }";
		AdvancedQueryPojo aqp = AdvancedQueryPojo.fromApi(sQueryJson, AdvancedQueryPojo.class);
		//AdvancedQueryPojo aqp = AdvancedQueryPojo.fromApi(sQueryJson, new TypeToken<AdvancedQueryPojo>(){});
			//^^^ (equivalent, needed for types that are generics) 
		////////////////////////////////////////////////
		System.out.println("AQP="+new Gson().toJson(aqp));
		
		// Testing ResponsePojo code on serialization
		rp1.setData(aqp);
		sRPSingleObject = rp1.toApi();
		rpRecreated = ResponsePojo.fromApi(sRPSingleObject, ResponsePojo.class, AdvancedQueryPojo.class);
		System.out.println("RECREATED RP_AQP=" + rpRecreated.toApi());
		System.out.println("RECREATED AQP(RP_AQP)=" + ((AdvancedQueryPojo)(rpRecreated.getData())).toApi());
		
		//API: Get a list of objects		
		sQueryJson = "{ 'logic': 'chris' }";
		AdvancedQueryPojo aqp2 = AdvancedQueryPojo.fromApi(sQueryJson, AdvancedQueryPojo.class);
		List<AdvancedQueryPojo> list2 = Arrays.asList(aqp, aqp2);
		rp1.setData(list2);
		sRPList = rp1.toApi();
		System.out.println("RP=" + sRPList);
		// This won't compile because didn't write a listType() for AQP (since it's never actually used in this way)
		//rpRecreated = ResponsePojo.listFromApi(sRPList, ResponsePojo.class, AdvancedQueryPojo.listType());
		rpRecreated = ResponsePojo.listFromApi(sRPList, ResponsePojo.class, new TypeToken<List<AdvancedQueryPojo>>(){});
		System.out.println("RECREATED RP_AQP=" + rpRecreated.toApi());
		
		////////////////////////////////////////////////
		//CANONICAL EXAMPLE:
		sQueryJson = " [ { 'logic': 'alex' } , { 'logic': 'chris' } ] ";
		list2 = AdvancedQueryPojo.listFromApi(sQueryJson, new TypeToken<List<AdvancedQueryPojo>>(){});
		////////////////////////////////////////////////
		System.out.println("APL=" + new Gson().toJson(list2));
		
		//API: BasicDBList (like feeds in the full system)
		BasicDBList dbl = new BasicDBList();
		BasicDBObject db01 = new BasicDBObject("index", 1);
		BasicDBObject db02 = new BasicDBObject("index", 2);
		BasicDBObject db03 = new BasicDBObject("index", 3);
		dbl.addAll(Arrays.asList(db01, db02, db03));
		ResponsePojo rp2 = new ResponsePojo(null, dbl, (BasePojoApiMap<BasicDBList>)null);
		System.out.println("DBO=" + rp2.toApi());
		
		//API: test the V0 DocumentPojo, which has a few differences (including a static version for raw modification)
		BasicDBObject docApiDbo = (BasicDBObject) DbManager.getDocument().getMetadata().findOne();
		// (remove a few things to tidy up display)
		docApiDbo.remove("entities");
		docApiDbo.remove("associations");
		docApiDbo.remove("metadata");
		// (remove things as a test)
//		docApiDbo.remove("sourceKey");
//		docApiDbo.remove("source");
//		docApiDbo.remove("mediaType");
		// (sourceKey in <key>#<community> format)
//		docApiDbo.put("sourceKey", docApiDbo.getString("sourceKey")+"#doc_api_test");
		// (display results of API mappings)
		DocumentPojo docApi = DocumentPojo.fromDb(docApiDbo, DocumentPojo.class);
		ResponsePojo rp3 = new ResponsePojo(null, docApi, new DocumentPojoApiMap());

		System.out.println("TIME_DOC_API1_CREATED="+docApi.getCreated());
		System.out.println("DOC_API1=" + rp3.toApi());
		DocumentPojoApiMap.mapToApi(docApiDbo);
		System.out.println("DOC_API2=" + BaseApiPojo.getDefaultBuilder().setPrettyPrinting().create().toJson(docApiDbo));
		DocumentPojo docFromApi = ApiManager.mapFromApi(ApiManager.mapToApi(docApi, null), DocumentPojo.class, null);
		System.out.println("TIME_DOC_API1_CREATED_INV="+docFromApi.getCreated());
		
/////////////////////////////////////////////////////////////////////////////
		
// DB testing:
		System.out.println("Open Community DB collection");
		//OLD:
		//CollectionManager cm = new CollectionManager();
		//DBCollection communityDb = cm.getCommunities();
		//NEW:
		DBCollection communityDb = DbManager.getSocial().getCommunity();
		
		//DB: read/write community object
		////////////////////////////////////////////////
		//CANONICAL EXAMPLE:
		CommunityPojo cp = CommunityPojo.fromDb(communityDb.findOne(), CommunityPojo.class);
		System.out.println("CP1=" + cp.toDb()); // (converts DBObject to string ie BSON->JSON - should have { $oid } and { $date } objectid/date formats)
		////////////////////////////////////////////////
		System.out.println("CP2=" + new Gson().toJson(cp)); // (will have complex object id format and string dates)
		//DB: read/write list of community objects
		////////////////////////////////////////////////
		//CANONICAL EXAMPLE:
		List<CommunityPojo> cpl = CommunityPojo.listFromDb(communityDb.find().limit(3), CommunityPojo.listType());
		System.out.println("CPL1=" + CommunityPojo.listToDb(cpl, CommunityPojo.listType()));
		////////////////////////////////////////////////
		System.out.println("CPL2=" + BaseDbPojo.getDefaultBuilder().create().toJson(cpl)); // (will have complex object id format and string dates)

		//Expect to see another delay here with the old method, new method should roll on...
		System.out.println("Open Document DB collection");
		//OLD:
		//CollectionManager cm2 = new CollectionManager();
		//DBCollection documentDb = cm2.getFeeds();
		//NEW:
		DBCollection documentDb = DbManager.getDocument().getMetadata();
		
		//DB: Read/write feed with metadata
		BasicDBObject query = new BasicDBObject("metadata", new BasicDBObject("$exists", true)); // (complex query so can't represent using pojos)
		query.put("entities", new BasicDBObject("$size", 3));
		////////////////////////////////////////////////
		//CANONICAL EXAMPLE:
		DocumentPojo doc = DocumentPojo.fromDb(documentDb.findOne(query), DocumentPojo.class);
		System.out.println("DOC1="+doc.toDb());
		BasicDBList dblTest = (BasicDBList) doc.toDb().get("entities");
		BasicDBObject dboTest = (BasicDBObject) dblTest.get(0);
		if (!dboTest.get("doccount").getClass().toString().equals("class java.lang.Long")) {
			throw new RuntimeException(dboTest.get("doccount").getClass().toString() + " SHOULD BE LONG");
		}
		////////////////////////////////////////////////
		System.out.println("DOC2="+new Gson().toJson(doc));
		doc = DocumentPojo.fromDb(documentDb.findOne(query), new TypeToken<DocumentPojo>(){}); // (alternative to the prettier DocumentPojo.class, needed for container classes)
		System.out.println("DOC3="+doc.toDb());
		//DB: list example for doc
		Set<DocumentPojo> docset = DocumentPojo.listFromDb(documentDb.find(query).limit(3), new TypeToken<Set<DocumentPojo>>(){});
		System.out.println("DOCSET="+DocumentPojo.listToDb(docset, new TypeToken<Set<DocumentPojo>>(){}));
		
		// Shares - demonstrate mapping of _ids across to the API (and that binary data is discarded):
		List<SharePojo> shares = SharePojo.listFromDb(DbManager.getSocial().getShare().find().limit(10), SharePojo.listType()); 
		System.out.println("SHARE="+ApiManager.mapListToApi(shares, SharePojo.listType(), new SharePojoApiMap(null)));
		
// Index testing:
		
		////////////////////////////////////////////////
		//CANONICAL EXAMPLE:
		DocumentPojoIndexMap docMap = new DocumentPojoIndexMap();
		System.out.println("DOC_INDEX=" + IndexManager.mapToIndex(doc, docMap));
		
		////////////////////////////////////////////////
		// Check use of enums in Entity pojo works
		EntityPojo testEnt = new EntityPojo(); 
		testEnt.setDimension(EntityPojo.Dimension.Where);
		System.out.println("ENT1=" + new GsonBuilder().setPrettyPrinting().create().toJson(testEnt));		
		System.out.println("DIM=" + testEnt.getDimension());
		BasicDBObject testEntDb = new BasicDBObject("dimension", "Who");
		testEnt = new Gson().fromJson(testEntDb.toString(), EntityPojo.class);
		System.out.println("ENT2=" + new GsonBuilder().setPrettyPrinting().create().toJson(testEnt));
		try {
			testEntDb = new BasicDBObject("dimension", "what");
			testEnt = BaseDbPojo.getDefaultBuilder().create().fromJson(testEntDb.toString(), EntityPojo.class);
			System.out.println("***FAIL=" + BaseDbPojo.getDefaultBuilder().setPrettyPrinting().create().toJson(testEnt));
		}
		catch (Exception e) {
			System.out.println("ENT3: Correctly failed with illegal dimension type");
		}
		
		////////////////////////////////////////////////
		// Metadata transformations based on type:
		
		String metadataObjStr = "{ 'test__long': 3, 'test_long': '3', 'error__long': { 'field1': 'no'}, "+
			"'test_arrayObj': [ { 'field1': 'test' } ], 'test_nestedArrayObj': [ [ { 'field1': 'test' } ]  ], "+
			"'test_array': [ 'val' ], 'test_nestedArray': [ [ 'val' ] ], "+
			"'test_obj': { 'field1': 'string' }, 'test_nestedObj': { 'field1': 'string', 'field2': { 'field3': 'string' }},"+
			"'test_null1': {}, test_null2: null"+
			"}";
		
		BasicDBObject metadataObj = (BasicDBObject) com.mongodb.util.JSON.parse(metadataObjStr);
		
		doc.addToMetadata("TestMeta", metadataObj);
		System.out.println("DOC_META=" + docMap.extendBuilder(BaseApiPojo.getDefaultBuilder()).setPrettyPrinting().create().toJson(doc));
		
// Changes to new ElasticSearch construct (particularly for bulk add)
		
		ElasticSearchManager indexManager = IndexManager.createIndex("test", null, false, null, null, ImmutableSettings.settingsBuilder());
		
		BulkResponse result = null;
		// All docs
		result = indexManager.bulkAddDocuments(IndexManager.mapListToIndex(docset, new TypeToken<Set<DocumentPojo>>(){}, 
																	new DocumentPojoIndexMap()), "_id", null, true);
		if (result.hasFailures()) {
			System.out.print("****** FAILED: ");
			System.out.println(result.buildFailureMessage());
		}
		
		//Delete index (once testing complete)
		indexManager.deleteMe();
		
		
				
	}
}
