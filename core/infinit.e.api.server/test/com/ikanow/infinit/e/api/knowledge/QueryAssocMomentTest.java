package com.ikanow.infinit.e.api.knowledge;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.ikanow.infinit.e.api.social.community.CommunityTest;
import com.ikanow.infinit.e.data_model.api.ResponsePojo;
import com.ikanow.infinit.e.data_model.api.ResponsePojo.ResponseObject;
import com.ikanow.infinit.e.data_model.api.knowledge.AdvancedQueryPojo;
import com.ikanow.infinit.e.data_model.api.knowledge.AdvancedQueryPojo.QueryOutputPojo;
import com.ikanow.infinit.e.data_model.api.knowledge.AdvancedQueryPojo.QueryOutputPojo.AggregationOutputPojo;
import com.ikanow.infinit.e.data_model.api.knowledge.AdvancedQueryPojo.QueryOutputPojo.AggregationOutputPojo.TemporalAggregationOutputPojo;
import com.ikanow.infinit.e.data_model.api.knowledge.AdvancedQueryPojo.QueryTermPojo;
import com.ikanow.infinit.e.data_model.driver.InfiniteDriver;
import com.ikanow.infinit.e.data_model.store.social.community.CommunityPojo;
import com.ikanow.infinit.e.data_model.store.social.community.CommunityPojo.CommunityType;

public class QueryAssocMomentTest {

	public static final String defaultTestPassword = "123456";
	private static String apiRootUrl = "http://localhost:8184/"; //TODO move this out somewhere else
	private static String apiKey = "12345";
	private static InfiniteDriver driver_local;
	private static final String commid1 = "55242829eeafb47e63747935";
	private static final String commid2 = "55242816eeafb47e63747934";
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		driver_local = new InfiniteDriver(apiRootUrl, apiKey);
		CommunityTest.removeTestCommunities(driver_local, new ResponseObject());	
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		CommunityTest.removeTestCommunities(driver_local, new ResponseObject());	
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}
	
//	@Test
//	public void test()
//	{
//		CommunityPojo cp = new CommunityPojo();
//		cp.setName("DatagroupAssocMoment1");
//		cp.setDescription("test");		
//		cp.setTags(Arrays.asList("test"));
//		cp.setParentId(null);
//		cp.setCommunityAttributes(null);
//		cp.setCommunityUserAttribute(null);
//		cp.setType(CommunityType.data);		
//		
//		Map<String, CommunityAttributePojo> community_attributes = new HashMap<String, CommunityAttributePojo>();
//		community_attributes.put(CommunityAttributePojo.NUM_SHARDS_ATTRIBUTE, new CommunityAttributePojo("Integer", "1"));
//		cp.setCommunityAttributes(community_attributes);
//		
//		cp = driver_local.createCommunity(cp, new ResponseObject());
//		System.out.println(cp.getId().toString());
//		
//		
//	}
	
	@Test
	public void testQuerySingleDocValueComm()
	{
		ResponseObject ro = new ResponseObject();
		CommunityPojo comm1 = driver_local.getCommunity(commid1, ro);
		
		//query, check that assoc moments exist
		AdvancedQueryPojo query = new AdvancedQueryPojo();
		QueryTermPojo qt = new QueryTermPojo();
		qt.etext = "*";
		query.qt = new ArrayList<AdvancedQueryPojo.QueryTermPojo>();
		query.qt.add(qt);
		query.output = new QueryOutputPojo();
		query.output.aggregation = new AggregationOutputPojo();
		query.output.aggregation.moments = new TemporalAggregationOutputPojo();
		query.output.aggregation.moments.associationsNumReturn = 1;
		query.output.aggregation.moments.timesInterval = "1y";
		List<ObjectId> commids = new ArrayList<ObjectId>();
		commids.add(comm1.getId());
		ResponsePojo results = driver_local.sendQuery(query, commids, ro);
		assertTrue(ro.isSuccess());
		
		JsonObject moments = (JsonObject) results.getMoments();
		assertNotNull(moments);
		assertNotNull(moments.get("assocs"));
		assertTrue(((JsonArray)moments.get("assocs")).size() > 0 );
//		for ( JsonElement assoc : (JsonArray)moments.get("assocs"))
//		{
//			System.out.println(assoc.toString());
//		}
	}
	
	@Test
	public void testQueryMultipleDocValueComm()
	{
		ResponseObject ro = new ResponseObject();
		CommunityPojo comm1 = driver_local.getCommunity(commid1, ro);
		CommunityPojo comm2 = driver_local.getCommunity(commid2, ro);
		
		//query, check that assoc moments exist
		AdvancedQueryPojo query = new AdvancedQueryPojo();
		QueryTermPojo qt = new QueryTermPojo();
		qt.etext = "*";
		query.qt = new ArrayList<AdvancedQueryPojo.QueryTermPojo>();
		query.qt.add(qt);
		query.output = new QueryOutputPojo();
		query.output.aggregation = new AggregationOutputPojo();
		query.output.aggregation.moments = new TemporalAggregationOutputPojo();
		query.output.aggregation.moments.associationsNumReturn = 1;
		query.output.aggregation.moments.timesInterval = "1y";
		List<ObjectId> commids = new ArrayList<ObjectId>();
		commids.add(comm1.getId());
		commids.add(comm2.getId());
		ResponsePojo results = driver_local.sendQuery(query, commids, ro);
		assertTrue(ro.isSuccess());
		
		JsonObject moments = (JsonObject) results.getMoments();
		assertNotNull(moments);
		assertNotNull(moments.get("assocs"));
		assertTrue(((JsonArray)moments.get("assocs")).size() > 0 );
	}
	
	@Test
	public void testQuerySingleNonComm()
	{
		ResponseObject ro = new ResponseObject();
		CommunityPojo comm1 = CommunityTest.createTestCommunity(CommunityType.data, driver_local, ro);
		System.out.println(comm1.getId().toString());		
		
		//query, check that assoc moments exist
		AdvancedQueryPojo query = new AdvancedQueryPojo();
		QueryTermPojo qt = new QueryTermPojo();
		qt.etext = "*";
		query.qt = new ArrayList<AdvancedQueryPojo.QueryTermPojo>();
		query.qt.add(qt);
		query.output = new QueryOutputPojo();
		query.output.aggregation = new AggregationOutputPojo();
		query.output.aggregation.moments = new TemporalAggregationOutputPojo();
		query.output.aggregation.moments.associationsNumReturn = 1;
		query.output.aggregation.moments.timesInterval = "1y";
		List<ObjectId> commids = new ArrayList<ObjectId>();
		commids.add(comm1.getId());
		ResponsePojo results = driver_local.sendQuery(query, commids, ro);
		assertFalse(results.getResponse().isSuccess());						
	}
	
	@Test
	public void testQueryMultipleSplitComm()
	{
		ResponseObject ro = new ResponseObject();
		CommunityPojo comm1 = driver_local.getCommunity(commid1, ro);
		CommunityPojo comm2 = CommunityTest.createTestCommunity(CommunityType.data, driver_local, ro);
		System.out.println(comm1.getId().toString());
		System.out.println(comm2.getId().toString());
		
		//query, check that assoc moments exist
		AdvancedQueryPojo query = new AdvancedQueryPojo();
		QueryTermPojo qt = new QueryTermPojo();
		qt.etext = "*";
		query.qt = new ArrayList<AdvancedQueryPojo.QueryTermPojo>();
		query.qt.add(qt);
		query.output = new QueryOutputPojo();
		query.output.aggregation = new AggregationOutputPojo();
		query.output.aggregation.moments = new TemporalAggregationOutputPojo();
		query.output.aggregation.moments.associationsNumReturn = 1;
		query.output.aggregation.moments.timesInterval = "1y";
		List<ObjectId> commids = new ArrayList<ObjectId>();
		commids.add(comm1.getId());
		commids.add(comm2.getId());
		ResponsePojo results = driver_local.sendQuery(query, commids, ro);
		assertTrue(ro.isSuccess());
				
		JsonObject moments = (JsonObject) results.getMoments();
		assertNotNull(moments);
		assertNotNull(moments.get("assocs"));
		assertTrue(((JsonArray)moments.get("assocs")).size() > 0 );
	}
	
	@Test
	public void testQueryStar()
	{		
		ResponseObject ro = new ResponseObject();
		
		//query, check that assoc moments exist
		AdvancedQueryPojo query = new AdvancedQueryPojo();
		QueryTermPojo qt = new QueryTermPojo();
		qt.etext = "*";
		query.qt = new ArrayList<AdvancedQueryPojo.QueryTermPojo>();
		query.qt.add(qt);
		query.output = new QueryOutputPojo();
		query.output.aggregation = new AggregationOutputPojo();
		query.output.aggregation.moments = new TemporalAggregationOutputPojo();
		query.output.aggregation.moments.associationsNumReturn = 1;
		query.output.aggregation.moments.timesInterval = "1y";		
		ResponsePojo results = driver_local.sendQuery(query, "*", ro);
		assertTrue(ro.isSuccess());
		
		//until all indexes are converted to doc values, assoc moments will not return
		assertNull(results.getMoments());
	}

}

