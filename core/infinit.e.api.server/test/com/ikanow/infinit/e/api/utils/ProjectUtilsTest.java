package com.ikanow.infinit.e.api.utils;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.gson.GsonBuilder;
import com.ikanow.infinit.e.api.custom.mapreduce.CustomTest;
import com.ikanow.infinit.e.api.social.community.CommunityTest;
import com.ikanow.infinit.e.api.social.community.PersonTest;
import com.ikanow.infinit.e.api.social.sharing.ShareV2ApiTest;
import com.ikanow.infinit.e.data_model.api.BaseApiPojo;
import com.ikanow.infinit.e.data_model.api.ResponsePojo;
import com.ikanow.infinit.e.data_model.api.ResponsePojo.ResponseObject;
import com.ikanow.infinit.e.data_model.api.knowledge.AdvancedQueryPojo;
import com.ikanow.infinit.e.data_model.api.utils.ProjectPojoApiMap;
import com.ikanow.infinit.e.data_model.driver.InfiniteDriver;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.ikanow.infinit.e.data_model.store.custom.mapreduce.CustomMapReduceJobPojo;
import com.ikanow.infinit.e.data_model.store.social.community.CommunityPojo;
import com.ikanow.infinit.e.data_model.store.social.community.CommunityPojo.CommunityType;
import com.ikanow.infinit.e.data_model.store.social.sharing.SharePojo;
import com.ikanow.infinit.e.data_model.store.utils.ProjectPojo;
import com.ikanow.infinit.e.data_model.store.utils.ProjectPojo.ProjectGroups;
import com.ikanow.infinit.e.data_model.store.utils.ProjectPojo.ProjectSources;

public class ProjectUtilsTest {

	private static String apiRootUrl = "http://localhost:8184/";
	private static String apiKey = "12345";
	private static InfiniteDriver driver_local;
	private static long counter = 0;
	private static String test_proj_name_prefix = "test123_proj123_";
	final private static String test_proj_type = "infinite_project_config";
	private static Logger logger = LogManager.getLogger(ProjectUtilsTest.class);
	final private static String test_datagroup_id = "551171cce4b02920e25c9a06";
	final private static String source_key_1 = "www.cnn.com.2015.03.23.us.online.threat.isis.us.troops.in.";
	final private static String source_key_2 = "www.cnn.com.2015.03.22.us.arizona.cottonwood.walmart.poli.";
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		driver_local = new InfiniteDriver(apiRootUrl, apiKey);
		//cleanup any existing test data
		ProjectUtilsTest.removeTestProjects(driver_local, new ResponseObject());
		PersonTest.deleteTestUsers(driver_local, new ResponseObject());
		CommunityTest.removeTestCommunities(driver_local, new ResponseObject());			
		ShareV2ApiTest.removeTestShares(driver_local, new ResponseObject());
		CustomTest.removeTestCustoms(driver_local, new ResponseObject());
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		driver_local.clearProjectFilter();
		ProjectUtilsTest.removeTestProjects(driver_local, new ResponseObject());
		PersonTest.deleteTestUsers(driver_local, new ResponseObject());
		CommunityTest.removeTestCommunities(driver_local, new ResponseObject());			
		ShareV2ApiTest.removeTestShares(driver_local, new ResponseObject());
		CustomTest.removeTestCustoms(driver_local, new ResponseObject());
	}

	@Before
	public void setUp() throws Exception {
		//make sure the project filter isn't set before each test
		driver_local.clearProjectFilter();
	}

	@After
	public void tearDown() throws Exception {
	}
	
	public static ProjectPojo getTestProjectWithSources()
	{
		//TODO will need to change this to work everywhere or something?
		//sets up a default project using the test datagroup (dev cluster only)
		List<String> datagroups = new ArrayList<String>();
		datagroups.add(test_datagroup_id);
		List<String> keys = new ArrayList<String>();
		keys.add(source_key_1);
		keys.add(source_key_2);
		return getTestProject(datagroups, keys, test_datagroup_id);
	}
	
	public static ProjectPojo getTestProjectEmpty(InfiniteDriver driver)
	{
		CommunityPojo test_comm = CommunityTest.createTestCommunity(CommunityType.data, driver, new ResponseObject());
		List<String> datagroups = new ArrayList<String>();
		datagroups.add(test_comm.getId().toString());
		List<String> keys = new ArrayList<String>();
		return getTestProject(datagroups, keys, test_comm.getId().toString());
	}
	
	public static ProjectPojo getTestProject(List<String> datagroups, List<String> keys, String communityId)
	{
		ProjectPojo project = new ProjectPojo();
		ProjectSources sources = project.new ProjectSources();
		sources.setDatagroups(datagroups);
		sources.setKeys(keys);
		project.setSources(sources);
		ProjectGroups data_groups = project.new ProjectGroups();
		data_groups.setIds(Arrays.asList(communityId));
		project.setDataGroups(data_groups);
		/*ProjectGroups user_groups = project.new ProjectGroups();
		user_groups.setIds(new ArrayList<String>());
		project.setUserGroups(user_groups);*/
		
		//create a datagroup for the project to use
		//TODO set the num shards to 0 so we don't create an index
		CommunityPojo data_group = CommunityTest.createTestCommunity(CommunityType.data, driver_local, new ResponseObject());
		project.setProjectDataGroupId(data_group.getId().toString());
		return project;
	}
	
	public static SharePojo createTestProject(InfiniteDriver driver, ResponseObject responseObject, ProjectPojo project)
	{
		String communityId = project.getProjectDataGroupId();
		GsonBuilder gb = project.extendBuilder(BaseApiPojo.getDefaultBuilder());		
		gb = (new ProjectPojoApiMap()).extendBuilder(gb);
		String json = gb.create().toJson(project);
		
		SharePojo share = driver.addShareJSON(test_proj_name_prefix+counter, "asdf", test_proj_type, json, responseObject);
		if ( communityId != null && share != null)
		{
			driver.addShareToCommunity(share.get_id().toString(), "asd", communityId, responseObject);
		}
		return share;
	}
	
	public static void removeTestProjects(InfiniteDriver driver, ResponseObject responseObject)
	{
		List<SharePojo> shares = driver.searchShares(null, null, test_proj_type, responseObject);
		if ( shares != null )
		{
			for (SharePojo share : shares )
			{
				if ( share.getTitle().startsWith(test_proj_name_prefix))
				{				
					driver.removeShare(share.get_id().toString(), responseObject);
					if ( !responseObject.isSuccess() )
						logger.error("Error removing project test share: " + share.get_id() + " : " + share.getTitle());
				}
			}
		}
	}

	@Test
	public void testApplyProjectToQueryBasic() {
		//create project
		ResponseObject ro = new ResponseObject();
		ProjectPojo project = getTestProjectWithSources();		
		SharePojo share_project = createTestProject(driver_local, ro, project);
		System.out.println(share_project.get_id().toString());
		//add user to project datagroup
		//PersonPojo user = PersonTest.createTestUser("member", driver_local, ro);
		//driver_local.addToCommunity(project.getProjectDataGroupId(), user.get_id().toString(), ro);
		
		driver_local.setProjectFilter(share_project.get_id().toString());
		
		//make query and ensure it uses the datagroup/2 source keys (both test files only have 1 doc)
		AdvancedQueryPojo query = new AdvancedQueryPojo();
		ResponsePojo response = driver_local.sendQuery(query, new ObjectId(), ro);	//id doesn't matter, will get replaced	
		assertTrue(response.getStats().found == 2L );
		
		//remove one of the keys
		List<String> source_keys = project.getSources().getKeys();
		source_keys.remove(0);
		project.getSources().setKeys(source_keys);
		GsonBuilder gb = project.extendBuilder(BaseApiPojo.getDefaultBuilder());		
		gb = (new ProjectPojoApiMap()).extendBuilder(gb);
		String json = gb.create().toJson(project);
		driver_local.updateShareJSON(share_project.get_id().toString(), share_project.getTitle(), share_project.getDescription(), share_project.getType(), 
				json, ro);
		
		//wait for cache to timeout
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			fail("Thread sleep interrupted");
		}
		
		//make query and ensure it uses only the remaining source key
		query = new AdvancedQueryPojo();
		response = driver_local.sendQuery(query, share_project.get_id(), ro);				
		assertEquals( new Long(1), response.getStats().found );		
	}
	
	@Test
	public void testApplyProjectToQueryPlusAlias() {
		fail("Not yet implemented");
	}
	
	@Test
	public void testApplyProjectToGetSources() {
		//create project
		ResponseObject ro = new ResponseObject();
		ProjectPojo project = getTestProjectWithSources();		
		SharePojo share_project = createTestProject(driver_local, ro, project);
		System.out.println(share_project.get_id().toString());
		//add user to project usergroup
		//PersonPojo user = PersonTest.createTestUser("member", driver_local, ro);
		//driver_local.addToCommunity(project.getProjectDataGroupId(), user.get_id().toString(), ro);
		
		driver_local.setProjectFilter(share_project.get_id().toString());
		
		//call get sources with project_id and expect to get back only the 2 sources in the datagroup
		List<SourcePojo> sources = driver_local.getSourceAll("*", ro);
		assertTrue(sources.size() > 0 );
		ObjectId datagroup_oid = new ObjectId(test_datagroup_id);
		for ( SourcePojo source : sources )
		{
			assertTrue(source.getCommunityIds().contains(datagroup_oid));
		}				
	}
	
	@Test
	public void testApplyProjectToGetGroups() {
		//create project
		ResponseObject ro = new ResponseObject();
		ProjectPojo project = getTestProjectEmpty(driver_local);		
		SharePojo share_project = createTestProject(driver_local, ro, project);
		System.out.println(share_project.get_id().toString());
		driver_local.setProjectFilter(share_project.get_id().toString());
		
		//call get communities with project_id and expect to only get back the projects communities
		List<CommunityPojo> communities = driver_local.getAllCommunity(ro);
		assertTrue(communities.size() > 0);
		
		//check we only received comms in project
		List<String> all_ids = new ArrayList<String>();
		all_ids.addAll(project.getDataGroups().getIds());
		all_ids.add(project.getProjectDataGroupId());
		
		assertEquals(all_ids.size(), communities.size());
		for ( CommunityPojo community : communities )
		{
			String communityId = community.getId().toString();
			assertTrue(all_ids.contains(communityId));
		}
	}
	
	@Test
	public void testApplyProjectToGetCustomPlugins() {		
		//create project
		ResponseObject ro = new ResponseObject();
		ProjectPojo project = getTestProjectEmpty(driver_local);		
		SharePojo share_project = createTestProject(driver_local, ro, project);
		System.out.println(share_project.get_id().toString());
		//add user to project usergroup
		//PersonPojo user = PersonTest.createTestUser("member", driver_local, ro);
		//driver_local.addToCommunity(project.getProjectDataGroupId(), user.get_id().toString(), ro);
		
		driver_local.setProjectFilter(share_project.get_id().toString());
		
		//add a job
		CustomMapReduceJobPojo custom = CustomTest.createTestCustom(driver_local, ro, Arrays.asList(new ObjectId(project.getDataGroups().getIds().get(0))));		
		
		//call get custom plugins with project id and expect to only get back the projects custom jobs
		List<CustomMapReduceJobPojo> jobs = driver_local.getCustomTaskOrQuery(null, ro);
		assertEquals(1, jobs.size());
		assertEquals(custom._id, jobs.get(0)._id);
	}
	
	@Test
	public void testApplyProjectToSearchShares() {		
		System.out.println("Starting search shares");
		//create project
		ResponseObject ro = new ResponseObject();
		ProjectPojo project = getTestProjectEmpty(driver_local);		
		SharePojo share_project = createTestProject(driver_local, ro, project);
		System.out.println(share_project.get_id().toString());
		//add user to project usergroup
		//PersonPojo user = PersonTest.createTestUser("member", driver_local, ro);
		//driver_local.addToCommunity(project.getProjectDataGroupId(), user.get_id().toString(), ro);
		
		//add a share to the project
		SharePojo test_share = ShareV2ApiTest.createTestShare(driver_local, ro, project.getDataGroups().getIds().get(0));
		
		//add a share to a comm not in the project
		CommunityPojo community_nonproject = CommunityTest.createTestCommunity(CommunityType.data, driver_local, ro);
		SharePojo test_share_nonproject = ShareV2ApiTest.createTestShare(driver_local, ro, community_nonproject.getId().toString());
		
		//test getting both back
		List<SharePojo> shares = driver_local.searchShares(null, null, test_share.getType(), ro);
		int matched = 0;
		for ( SharePojo share : shares )
		{
			if ( share.get_id().equals(test_share.get_id()) ||
				share.get_id().equals(test_share_nonproject.get_id()) ){
				matched++;
			}
		}
		assertEquals(2, matched);
		
		//test setting project_id and only getting 1 back
		driver_local.setProjectFilter(share_project.get_id().toString());
		shares = driver_local.searchShares(null, null, test_share.getType(), ro);
		matched = 0;
		for ( SharePojo share : shares )
		{
			if ( share.get_id().equals(test_share.get_id()) ||
				share.get_id().equals(test_share_nonproject.get_id()) ){
				matched++;
			}
		}
		assertEquals(1, matched);
		System.out.println("done search shares");
		
		//TODO think about what the outcome of this is suppose to be?
		//do we only want results to be from communities in project.data_groups?
		//we'll have to filter after making the query because a user could specify a searchby: something so we can't overwrite it.
		//also have to consider the searchParent argument, what is the expected outcome
	}
}
