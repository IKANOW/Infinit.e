package com.ikanow.infinit.e.api.social.community;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.elasticsearch.common.settings.Settings;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ikanow.infinit.e.data_model.api.ResponsePojo.ResponseObject;
import com.ikanow.infinit.e.data_model.driver.InfiniteDriver;
import com.ikanow.infinit.e.data_model.index.ElasticSearchManager;
import com.ikanow.infinit.e.data_model.index.IndexManager;
import com.ikanow.infinit.e.data_model.store.social.community.CommunityAttributePojo;
import com.ikanow.infinit.e.data_model.store.social.community.CommunityPojo;
import com.ikanow.infinit.e.data_model.store.social.community.CommunityUserAttributePojo;
import com.ikanow.infinit.e.data_model.store.social.community.CommunityPojo.CommunityType;

public class CommunityShardTest {

	private static String apiRootUrl = "http://localhost:8184/";
	private static String apiKeyAdmin = "abcdef";
	private static InfiniteDriver driver_local;
	private static long counter = 0;
	private static String test_comm_name_prefix = "test123_";
	private static Logger logger = LogManager.getLogger(CommunityShardTest.class);
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		driver_local = new InfiniteDriver(apiRootUrl, apiKeyAdmin);
		//cleanup any existing test data
		CommunityShardTest.removeTestShardCommunities(driver_local, new ResponseObject());
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		CommunityShardTest.removeTestShardCommunities(driver_local, new ResponseObject());		
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}
	
	public static CommunityPojo createTestShardCommunity(CommunityType type, InfiniteDriver driver, ResponseObject responseObject, int num_shards)
	{
		Map<String, CommunityAttributePojo> community_attributes = new HashMap<String, CommunityAttributePojo>();
		community_attributes.put(CommunityAttributePojo.NUM_SHARDS_ATTRIBUTE, new CommunityAttributePojo("Integer", num_shards + ""));
		return createTestShardCommunity(type, driver, responseObject, community_attributes, null);
	}
	
	public static CommunityPojo createTestShardCommunity(CommunityType type, InfiniteDriver driver, ResponseObject responseObject, Map<String, CommunityAttributePojo> community_attributes, Map<String, CommunityUserAttributePojo> user_attributes)
	{
		CommunityPojo cp = new CommunityPojo();
		cp.setName(test_comm_name_prefix + type + "_" + counter);
		cp.setDescription("asdf");		
		cp.setTags(Arrays.asList("test123"));
		cp.setParentId(null);
		cp.setCommunityAttributes(community_attributes);
		cp.setCommunityUserAttribute(user_attributes);
		
		cp = driver.createCommunity(cp, responseObject);
		counter++;
		return cp;
	}
	
	public static void removeTestShardCommunities(InfiniteDriver driver, ResponseObject responseObject)
	{
		//delete all communities with name test123_
		ResponseObject ro = new ResponseObject();
		List<CommunityPojo> communities = driver.getAllCommunity(ro);
		for ( CommunityPojo community : communities )
		{
			if ( community.getName().startsWith(test_comm_name_prefix) )
			{
				driver.deleteCommunity(community.getId().toString(), responseObject);
				if ( !responseObject.isSuccess() )
					logger.error("Error cleaning up comms in CommunityTest, could not delete comm: " + community.getName() + " because: " + responseObject.getMessage());
				
				//have to delete twice, first time goes to pending
				driver.deleteCommunity(community.getId().toString(), responseObject);
				if ( !responseObject.isSuccess() )
					logger.error("Error cleaning up comms in CommunityTest, could not fully delete comm: " + community.getName() + " because: " + responseObject.getMessage());
			}
		}
	}
	
	/**
	 * Looks up the number of shards being used for the given community_id
	 * or returns -1 if an index does not exist.
	 * 
	 * @param community_id
	 * @return
	 */
	public int getNumberOfShards(String community_id)
	{
		ElasticSearchManager.setClusterName("infinite-dev");
		//NOTE: this ping request takes a long time (60s) for a non-existent index
		if ( IndexManager.pingIndex("doc_" + community_id, "localhost:4093") )
		{
			ElasticSearchManager esm = IndexManager.getIndex("doc_" + community_id, "localhost:4093" );
			Settings settings = esm.getIndexStats();
			if ( settings != null )
			{
				try
				{
					int num_shards = Integer.parseInt(settings.get("index.number_of_shards"));
					return num_shards;
				}
				catch (Exception ex)
				{
					//error parsing
					logger.error(ex);
				}
			}
		}
		return -1;
	}

	@Test
	public void testCreateDefault() {
		//Should create a community with a 5 shard index (the default)
		ResponseObject ro = new ResponseObject();
		CommunityPojo community = CommunityShardTest.createTestShardCommunity(CommunityType.data, driver_local, ro, 0);
		
		//test it has a 5 shard index somehow
		assertEquals(5, getNumberOfShards(community.getId().toString()));
	}
	
	@Test
	public void testCreateNoIndex() {
		//Should create a community without an index
		ResponseObject ro = new ResponseObject();
		CommunityPojo community = CommunityShardTest.createTestShardCommunity(CommunityType.data, driver_local, ro, -1);
		
		// test it has no index somehow
		assertEquals(-1, getNumberOfShards(community.getId().toString()));
	}
	
	@Test
	public void testCreateNonDefault() {
		//Should create a community with a 1 shard index (not the default)
		ResponseObject ro = new ResponseObject();
		CommunityPojo community = CommunityShardTest.createTestShardCommunity(CommunityType.data, driver_local, ro, 1);
		
		//test it has a 1 shard index somehow
		assertEquals(1, getNumberOfShards(community.getId().toString()));
	}
	
	@Test
	public void testUpdateNonShardToShard()
	{					
		//create a comm with no index, update to have num_shards set to 1
		ResponseObject ro = new ResponseObject();
		CommunityPojo community = CommunityShardTest.createTestShardCommunity(CommunityType.data, driver_local, ro, -1);
		assertEquals(-1, getNumberOfShards(community.getId().toString()));
		
		//update comm object
		Map<String, CommunityAttributePojo> community_attributes = new HashMap<String, CommunityAttributePojo>();
		community_attributes.put(CommunityAttributePojo.NUM_SHARDS_ATTRIBUTE, new CommunityAttributePojo("Integer", "1"));
		community.setCommunityAttributes(community_attributes);
		assertTrue(driver_local.updateCommunity(community.getId().toString(), community, ro));
		community = driver_local.getCommunity(community.getId().toString(), ro);
		assertEquals(1, getNumberOfShards(community.getId().toString()));		
	}
	
	@Test
	public void testUpdateShardToNonShard()
	{
		//create a comm with an index, update to have num_shards set to -1
		ResponseObject ro = new ResponseObject();
		CommunityPojo community = CommunityShardTest.createTestShardCommunity(CommunityType.data, driver_local, ro, 1);
		assertEquals(1, getNumberOfShards(community.getId().toString()));
		
		//update comm object to have no index, should fail
		Map<String, CommunityAttributePojo> community_attributes = new HashMap<String, CommunityAttributePojo>();
		community_attributes.put(CommunityAttributePojo.NUM_SHARDS_ATTRIBUTE, new CommunityAttributePojo("Integer", "-1"));
		community.setCommunityAttributes(community_attributes);
		assertFalse(driver_local.updateCommunity(community.getId().toString(), community, ro));
		//make sure the comm still has 1 shard
		community = driver_local.getCommunity(community.getId().toString(), ro);
		assertEquals(1, getNumberOfShards(community.getId().toString()));
	}
	
	@Test
	public void testCreateRidiculousNumShard()
	{
		ResponseObject ro = new ResponseObject();
		CommunityPojo community = CommunityShardTest.createTestShardCommunity(CommunityType.data, driver_local, ro, 100);
		assertNull(community);
	}

}
