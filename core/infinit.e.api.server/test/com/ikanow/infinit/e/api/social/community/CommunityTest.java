package com.ikanow.infinit.e.api.social.community;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ikanow.infinit.e.api.social.sharing.ShareV2ApiTest;
import com.ikanow.infinit.e.data_model.api.ResponsePojo.ResponseObject;
import com.ikanow.infinit.e.data_model.driver.InfiniteDriver;
import com.ikanow.infinit.e.data_model.store.social.community.CommunityAttributePojo;
import com.ikanow.infinit.e.data_model.store.social.community.CommunityMemberPojo;
import com.ikanow.infinit.e.data_model.store.social.community.CommunityPojo;
import com.ikanow.infinit.e.data_model.store.social.community.CommunityUserAttributePojo;
import com.ikanow.infinit.e.data_model.store.social.community.CommunityPojo.CommunityType;
import com.ikanow.infinit.e.data_model.store.social.person.PersonCommunityPojo;
import com.ikanow.infinit.e.data_model.store.social.person.PersonPojo;
import com.ikanow.infinit.e.data_model.store.social.sharing.SharePojo;

public class CommunityTest {

	private static String apiRootUrl = "http://localhost:8184/";
	private static String apiKeyAdmin = "abcdef";
	private static String apiKeyUser = "abcdefg";
	private static InfiniteDriver driver_local;
	private static long counter = 0;
	private static String test_comm_name_prefix = "test123_";
	private static Logger logger = LogManager.getLogger(CommunityTest.class);
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		driver_local = new InfiniteDriver(apiRootUrl, apiKeyAdmin);
		//cleanup any existing test data
		PersonTest.deleteTestUsers(driver_local, new ResponseObject());
		CommunityTest.removeTestCommunities(driver_local, new ResponseObject());	
		ShareV2ApiTest.removeTestShares(driver_local, new ResponseObject());
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		PersonTest.deleteTestUsers(driver_local, new ResponseObject());
		CommunityTest.removeTestCommunities(driver_local, new ResponseObject());	
		ShareV2ApiTest.removeTestShares(driver_local, new ResponseObject());
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}
	
	public static CommunityPojo createTestCommunity(CommunityType type, InfiniteDriver driver, ResponseObject responseObject)
	{
		return createTestCommunity(type, driver, responseObject, null, null);
	}
	
	public static CommunityPojo createTestCommunity(CommunityType type, InfiniteDriver driver, ResponseObject responseObject, Map<String, CommunityAttributePojo> community_attributes, Map<String, CommunityUserAttributePojo> user_attributes)
	{
		CommunityPojo cp = new CommunityPojo();
		cp.setName(test_comm_name_prefix + type + "_" + counter);
		cp.setDescription("asdf");		
		cp.setTags(Arrays.asList("test123"));
		cp.setParentId(null);
		cp.setCommunityAttributes(community_attributes);
		cp.setCommunityUserAttribute(user_attributes);
		cp.setType(type);
		//set no indexes (we shouldn't need them in test classes, use createShardTestCommunity instead if you do)
		if ( community_attributes == null )
			community_attributes = new HashMap<String, CommunityAttributePojo>();
		community_attributes.put(CommunityAttributePojo.NUM_SHARDS_ATTRIBUTE, new CommunityAttributePojo("Integer", "-1"));
		cp.setCommunityAttributes(community_attributes);
		cp = driver.createCommunity(cp, responseObject);
		counter++;
		return cp;
	}
	
	public static void removeTestCommunities(InfiniteDriver driver, ResponseObject responseObject)
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
	
	public static boolean containsMember(CommunityPojo community, String user_id) {
		for ( CommunityMemberPojo cmp : community.getMembers() )
		{
			if ( user_id.equals(cmp.get_id().toString()) )
			{
				return true;
			}
		}
		return false;
	}

	@Test
	public void testCreateLegacy() {
		CommunityPojo community = createTestCommunity(null, driver_local, new ResponseObject());
		assertNotNull(community);
		//SPECIAL CASE, LEGACY COMMUNITIES REPORT BACK AS DATA COMMS
		assertEquals(community.getType(), CommunityType.data);		
	}
	
	@Test
	public void testCreateUserGroup() {
		CommunityPojo community = createTestCommunity(CommunityType.user, driver_local, new ResponseObject());
		assertNotNull(community);
		//SPECIAL CASE, LEGACY COMMUNITIES REPORT BACK AS DATA COMMS
		assertEquals(community.getType(), CommunityType.user);		
	}
	
	@Test
	public void testCreateDataGroup() {
		CommunityPojo community = createTestCommunity(CommunityType.data, driver_local, new ResponseObject());
		assertNotNull(community);
		//SPECIAL CASE, LEGACY COMMUNITIES REPORT BACK AS DATA COMMS
		assertEquals(community.getType(), CommunityType.data);		
	}
	
	@Test
	public void testAddUserLegacy() {
		CommunityPojo community = createTestCommunity(null, driver_local, new ResponseObject());
		PersonPojo person = PersonTest.createTestUser("user", driver_local, new ResponseObject());
		ResponseObject ro = new ResponseObject();
		driver_local.addToCommunity(community.getId().toString(), person.get_id().toString(), ro);
		assertTrue(ro.isSuccess());
		ResponseObject ro1 = new ResponseObject();
		CommunityPojo totest = driver_local.getCommunity(community.getId().toString(), ro1);
		assertTrue(containsMember(totest, person.get_id().toString()));
	}
	
	

	@Test
	public void testAddUserUserGroup() {
		CommunityPojo community = createTestCommunity(CommunityType.user, driver_local, new ResponseObject());
		PersonPojo person = PersonTest.createTestUser("user", driver_local, new ResponseObject());
		ResponseObject ro = new ResponseObject();
		driver_local.addToCommunity(community.getId().toString(), person.get_id().toString(), ro);
		assertTrue(ro.isSuccess());
		ResponseObject ro1 = new ResponseObject();
		CommunityPojo totest = driver_local.getCommunity(community.getId().toString(), ro1);
		assertTrue(containsMember(totest, person.get_id().toString()));
	}
	
	@Test
	public void testAddUserDataGroup() {
		CommunityPojo community = createTestCommunity(CommunityType.data, driver_local, new ResponseObject());
		PersonPojo person = PersonTest.createTestUser("user", driver_local, new ResponseObject());
		ResponseObject ro = new ResponseObject();
		driver_local.addToCommunity(community.getId().toString(), person.get_id().toString(), ro);
		assertTrue(ro.isSuccess());
		ResponseObject ro1 = new ResponseObject();
		CommunityPojo totest = driver_local.getCommunity(community.getId().toString(), ro1);
		assertTrue(containsMember(totest, person.get_id().toString()));
	}
	
	@Test
	public void testRemoveUserLegacy() {
		CommunityPojo community = createTestCommunity(null, driver_local, new ResponseObject());
		PersonPojo person = PersonTest.createTestUser("user", driver_local, new ResponseObject());
		ResponseObject ro = new ResponseObject();
		driver_local.addToCommunity(community.getId().toString(), person.get_id().toString(), ro);
		assertTrue(ro.isSuccess());		
		ResponseObject ro2 = new ResponseObject();
		driver_local.removeUserFromCommunity(person.get_id().toString(), community.getId().toString(), ro2);
		ResponseObject ro3 = new ResponseObject();
		CommunityPojo totest = driver_local.getCommunity(community.getId().toString(), ro3);
		assertFalse(containsMember(totest, person.get_id().toString()));
	}
	
	@Test
	public void testRemoveUserUserGroup() {
		CommunityPojo community = createTestCommunity(CommunityType.user, driver_local, new ResponseObject());
		PersonPojo person = PersonTest.createTestUser("user", driver_local, new ResponseObject());
		ResponseObject ro = new ResponseObject();
		driver_local.addToCommunity(community.getId().toString(), person.get_id().toString(), ro);
		assertTrue(ro.isSuccess());		
		ResponseObject ro2 = new ResponseObject();
		driver_local.removeUserFromCommunity(person.get_id().toString(), community.getId().toString(), ro2);
		ResponseObject ro3 = new ResponseObject();
		CommunityPojo totest = driver_local.getCommunity(community.getId().toString(), ro3);
		assertFalse(containsMember(totest, person.get_id().toString()));
	}
	
	@Test
	public void testRemoveUserDataGroup() {
		CommunityPojo community = createTestCommunity(CommunityType.data, driver_local, new ResponseObject());
		PersonPojo person = PersonTest.createTestUser("user", driver_local, new ResponseObject());
		ResponseObject ro = new ResponseObject();
		driver_local.addToCommunity(community.getId().toString(), person.get_id().toString(), ro);
		assertTrue(ro.isSuccess());		
		ResponseObject ro2 = new ResponseObject();
		driver_local.removeUserFromCommunity(person.get_id().toString(), community.getId().toString(), ro2);
		ResponseObject ro3 = new ResponseObject();
		CommunityPojo totest = driver_local.getCommunity(community.getId().toString(), ro3);
		assertFalse(containsMember(totest, person.get_id().toString()));
	}
	
	@Test
	public void testAccessLegacy()
	{		
		//1. create comm
		CommunityPojo community_legacy = createTestCommunity(null, driver_local, new ResponseObject());
		PersonPojo person = PersonTest.createTestUser("user", driver_local, new ResponseObject());
		
		//2. put user in comm
		ResponseObject ro = new ResponseObject();
		driver_local.addToCommunity(community_legacy.getId().toString(), person.get_id().toString(), ro);
		assertTrue(ro.isSuccess());
		
		//3. create share in comm (as admin)
		ResponseObject ro2 = new ResponseObject();		
		SharePojo share = ShareV2ApiTest.createTestShare(driver_local, ro2, community_legacy.getId().toString());		
		assertTrue(ro2.isSuccess());
		
		//4. access comm as user 
		InfiniteDriver driver_user = new InfiniteDriver(apiRootUrl, person.getEmail(), PersonTest.defaultTestPassword);
		driver_user.login();
		ResponseObject ro3 = new ResponseObject();
		SharePojo share_result = driver_user.getShare(share.get_id().toString(), ro3);
		assertTrue(ro3.isSuccess());
		assertNotNull(share_result);
	}
	
	@Test
	public void testAccessUserGroup()
	{		
		//1. create comm
		CommunityPojo community_user = createTestCommunity(CommunityType.user, driver_local, new ResponseObject());
		PersonPojo person = PersonTest.createTestUser("user", driver_local, new ResponseObject());
		
		//2. put user in comm
		ResponseObject ro = new ResponseObject();
		driver_local.addToCommunity(community_user.getId().toString(), person.get_id().toString(), ro);
		assertTrue(ro.isSuccess());
		
		//3. create share in comm (as admin)
		ResponseObject ro2 = new ResponseObject();		
		SharePojo share = ShareV2ApiTest.createTestShare(driver_local, ro2, community_user.getId().toString());		
		assertTrue(ro2.isSuccess());
		
		//4. access comm as user 
		InfiniteDriver driver_user = new InfiniteDriver(apiRootUrl, person.getEmail(), PersonTest.defaultTestPassword);
		driver_user.login();
		ResponseObject ro3 = new ResponseObject();
		SharePojo share_result = driver_user.getShare(share.get_id().toString(), ro3);
		assertTrue(ro3.isSuccess());
		assertNotNull(share_result);
	}
	
	@Test
	public void testAccessDataGroup()
	{		
		//1. create comm
		CommunityPojo community_data = createTestCommunity(CommunityType.data, driver_local, new ResponseObject());
		PersonPojo person = PersonTest.createTestUser("user", driver_local, new ResponseObject());
		
		//2. put user in comm
		ResponseObject ro = new ResponseObject();
		driver_local.addToCommunity(community_data.getId().toString(), person.get_id().toString(), ro);
		assertTrue(ro.isSuccess());
		
		//3. create share in comm (as admin)
		ResponseObject ro2 = new ResponseObject();		
		SharePojo share = ShareV2ApiTest.createTestShare(driver_local, ro2, community_data.getId().toString());		
		assertTrue(ro2.isSuccess());
		
		//4. access comm as user 
		InfiniteDriver driver_user = new InfiniteDriver(apiRootUrl, person.getEmail(), PersonTest.defaultTestPassword);
		driver_user.login();
		ResponseObject ro3 = new ResponseObject();
		SharePojo share_result = driver_user.getShare(share.get_id().toString(), ro3);
		assertTrue(ro3.isSuccess());
		assertNotNull(share_result);
	}
	
	@Test
	public void testAccessDataGroupViaUserGroupBasicAccess()
	{
		//TODO make this actually do what i named it
		//TODO this is the thing i'm actually testing in CORE-36
		
		//create a user group/user
		CommunityPojo community_usergroup = createTestCommunity(CommunityType.user, driver_local, new ResponseObject());
		PersonPojo person = PersonTest.createTestUser("user", driver_local, new ResponseObject());
		
		//put user in user group
		ResponseObject ro = new ResponseObject();
		driver_local.addToCommunity(community_usergroup.getId().toString(), person.get_id().toString(), ro);
		assertTrue(ro.isSuccess());
		
		//create a datagroup
		CommunityPojo community_datagroup = createTestCommunity(CommunityType.data, driver_local, new ResponseObject());		
		
		//put usergroup in datagroup
		ResponseObject ro1 = new ResponseObject();
		driver_local.addToCommunity(community_datagroup.getId().toString(), community_usergroup.getId().toString(), ro1);
		assertTrue(ro1.isSuccess());
		
		//as admin add share to datagroup
		ResponseObject ro2 = new ResponseObject();
		SharePojo share = ShareV2ApiTest.createTestShare(driver_local, ro2, community_datagroup.getId().toString());		
		assertTrue(ro2.isSuccess());
		
		//as user, try to get share 
		InfiniteDriver driver_user = new InfiniteDriver(apiRootUrl, person.getEmail(), PersonTest.defaultTestPassword);
		driver_user.login();
		ResponseObject ro3 = new ResponseObject();
		SharePojo share_result = driver_user.getShare(share.get_id().toString(), ro3);
		assertTrue(ro3.isSuccess());
		assertNotNull(share_result);
	}
	
	@Test
	public void testAccessDataGroupViaUserGroupRemoveUsergroup()
	{
		//create a user group/user
		CommunityPojo community_usergroup = createTestCommunity(CommunityType.user, driver_local, new ResponseObject());
		PersonPojo person = PersonTest.createTestUser("user", driver_local, new ResponseObject());
		
		//put user in user group
		ResponseObject ro = new ResponseObject();
		driver_local.addToCommunity(community_usergroup.getId().toString(), person.get_id().toString(), ro);
		assertTrue(ro.isSuccess());
		
		//create a datagroup
		CommunityPojo community_datagroup = createTestCommunity(CommunityType.data, driver_local, new ResponseObject());		
		
		//put usergroup in datagroup
		ResponseObject ro1 = new ResponseObject();
		driver_local.addToCommunity(community_datagroup.getId().toString(), community_usergroup.getId().toString(), ro1);
		assertTrue(ro1.isSuccess());
		
		//as admin add share to datagroup
		ResponseObject ro2 = new ResponseObject();
		SharePojo share = ShareV2ApiTest.createTestShare(driver_local, ro2, community_datagroup.getId().toString());		
		assertTrue(ro2.isSuccess());
		
		//as user, try to get share 
		InfiniteDriver driver_user = new InfiniteDriver(apiRootUrl, person.getEmail(), PersonTest.defaultTestPassword);
		driver_user.login();
		ResponseObject ro3 = new ResponseObject();
		SharePojo share_result = driver_user.getShare(share.get_id().toString(), ro3);
		assertTrue(ro3.isSuccess());
		assertNotNull(share_result);
		
		//remove usergroup access by removing usergroup from datagroup and check we can't see share anymore
		driver_local.removeUserFromCommunity(community_usergroup.getId().toString(), community_datagroup.getId().toString(), ro); 
		share_result = driver_user.getShare(share.get_id().toString(), ro);
		assertFalse(ro.isSuccess());
		assertNull(share_result);
	}	
	
	@Test
	public void testAccessDataGroupViaUserGroupDeleteUsergroup()
	{		
		//create a user group/user
		CommunityPojo community_usergroup = createTestCommunity(CommunityType.user, driver_local, new ResponseObject());
		PersonPojo person = PersonTest.createTestUser("user", driver_local, new ResponseObject());
		
		//put user in user group
		ResponseObject ro = new ResponseObject();
		driver_local.addToCommunity(community_usergroup.getId().toString(), person.get_id().toString(), ro);
		assertTrue(ro.isSuccess());
		
		//create a datagroup
		CommunityPojo community_datagroup = createTestCommunity(CommunityType.data, driver_local, new ResponseObject());		
		
		//put usergroup in datagroup
		ResponseObject ro1 = new ResponseObject();
		driver_local.addToCommunity(community_datagroup.getId().toString(), community_usergroup.getId().toString(), ro1);
		assertTrue(ro1.isSuccess());
		
		//as admin add share to datagroup
		ResponseObject ro2 = new ResponseObject();
		SharePojo share = ShareV2ApiTest.createTestShare(driver_local, ro2, community_datagroup.getId().toString());		
		assertTrue(ro2.isSuccess());
		
		//as user, try to get share 
		InfiniteDriver driver_user = new InfiniteDriver(apiRootUrl, person.getEmail(), PersonTest.defaultTestPassword);
		driver_user.login();
		ResponseObject ro3 = new ResponseObject();
		SharePojo share_result = driver_user.getShare(share.get_id().toString(), ro3);
		assertTrue(ro3.isSuccess());
		assertNotNull(share_result);
		
		//delete usergroup access and check we can't see share anymore
		driver_local.removeShareFromCommunity(share.get_id().toString(), community_datagroup.getId().toString(), ro);
		share_result = driver_user.getShare(share.get_id().toString(), ro);
		assertFalse(ro.isSuccess());
		assertNull(share_result);
	}	
	
	@Test
	public void testAccessDataGroupViaUserGroupAddNewUserToUsergroup()
	{
		//create a user group/user
		CommunityPojo community_usergroup = createTestCommunity(CommunityType.user, driver_local, new ResponseObject());
		PersonPojo person = PersonTest.createTestUser("user", driver_local, new ResponseObject());
		
		//put user in user group
		ResponseObject ro = new ResponseObject();
		driver_local.addToCommunity(community_usergroup.getId().toString(), person.get_id().toString(), ro);
		assertTrue(ro.isSuccess());
		
		//create a datagroup
		CommunityPojo community_datagroup = createTestCommunity(CommunityType.data, driver_local, new ResponseObject());		
		
		//put usergroup in datagroup
		ResponseObject ro1 = new ResponseObject();
		driver_local.addToCommunity(community_datagroup.getId().toString(), community_usergroup.getId().toString(), ro1);
		assertTrue(ro1.isSuccess());
		
		//as admin add share to datagroup
		ResponseObject ro2 = new ResponseObject();
		SharePojo share = ShareV2ApiTest.createTestShare(driver_local, ro2, community_datagroup.getId().toString());		
		assertTrue(ro2.isSuccess());
		
		//as user, try to get share 
		InfiniteDriver driver_user = new InfiniteDriver(apiRootUrl, person.getEmail(), PersonTest.defaultTestPassword);
		driver_user.login();
		ResponseObject ro3 = new ResponseObject();
		SharePojo share_result = driver_user.getShare(share.get_id().toString(), ro3);
		assertTrue(ro3.isSuccess());
		assertNotNull(share_result);
				
		//create a new user and add them to usergroup, make sure they can now access share
		PersonPojo person2 = PersonTest.createTestUser("user", driver_local, new ResponseObject());
		driver_local.addToCommunity(community_usergroup.getId().toString(), person2.get_id().toString(), ro);
		assertTrue(ro.isSuccess());
		InfiniteDriver driver_user2 = new InfiniteDriver(apiRootUrl, person2.getEmail(), PersonTest.defaultTestPassword);
		driver_user2.login();
		share_result = driver_user.getShare(share.get_id().toString(), ro);
		assertTrue(ro.isSuccess());
		assertNotNull(share_result);
	}	
	
	@Test
	public void testAccessDataGroupViaUserGroupDeleteDatagroup()
	{		
		//create a user group/user
		CommunityPojo community_usergroup = createTestCommunity(CommunityType.user, driver_local, new ResponseObject());
		PersonPojo person = PersonTest.createTestUser("user", driver_local, new ResponseObject());
		
		//put user in user group
		ResponseObject ro = new ResponseObject();
		driver_local.addToCommunity(community_usergroup.getId().toString(), person.get_id().toString(), ro);
		assertTrue(ro.isSuccess());
		
		//create a datagroup
		CommunityPojo community_datagroup = createTestCommunity(CommunityType.data, driver_local, new ResponseObject());		
		
		//put usergroup in datagroup
		ResponseObject ro1 = new ResponseObject();
		driver_local.addToCommunity(community_datagroup.getId().toString(), community_usergroup.getId().toString(), ro1);
		assertTrue(ro1.isSuccess());
		
		//as admin add share to datagroup
		ResponseObject ro2 = new ResponseObject();
		SharePojo share = ShareV2ApiTest.createTestShare(driver_local, ro2, community_datagroup.getId().toString());		
		assertTrue(ro2.isSuccess());
		
		//as user, try to get share 
		InfiniteDriver driver_user = new InfiniteDriver(apiRootUrl, person.getEmail(), PersonTest.defaultTestPassword);
		driver_user.login();
		ResponseObject ro3 = new ResponseObject();
		SharePojo share_result = driver_user.getShare(share.get_id().toString(), ro3);
		assertTrue(ro3.isSuccess());
		assertNotNull(share_result);
		
		//delete datagroup, user should no longer have access to it
		driver_local.deleteCommunity(community_datagroup.getId().toString(), ro);
		//have to call twice to fully delete
		driver_local.deleteCommunity(community_datagroup.getId().toString(), ro);
		share_result = driver_user.getShare(share.get_id().toString(), ro);
		assertFalse(ro.isSuccess());
		assertNull(share_result);
		
	}	
	
	@Test
	public void testAddUserGroupToDataGroup()
	{
		CommunityPojo community_usergroup = createTestCommunity(CommunityType.user, driver_local, new ResponseObject());
		PersonPojo person = PersonTest.createTestUser("user", driver_local, new ResponseObject());
		
		//put user in user group
		ResponseObject ro = new ResponseObject();
		driver_local.addToCommunity(community_usergroup.getId().toString(), person.get_id().toString(), ro);
		assertTrue(ro.isSuccess());
		
		//create a datagroup
		CommunityPojo community_datagroup = createTestCommunity(CommunityType.data, driver_local, new ResponseObject());		
		
		//put usergroup in datagroup
		ResponseObject ro1 = new ResponseObject();
		driver_local.addToCommunity(community_datagroup.getId().toString(), community_usergroup.getId().toString(), ro1);
		assertTrue(ro1.isSuccess());
		
		//check user.communities contains datagroup and user.datagroup_reason contains usergroup
		ResponseObject ro2 = new ResponseObject();
		person = driver_local.getPerson(person.get_id().toString(), ro2);
		boolean containsDatagroup = false;
		for ( PersonCommunityPojo cmp : person.getCommunities() )
		{
			if ( cmp.get_id().equals(community_datagroup.getId().toString()) )
			{
				containsDatagroup = true;
				break;
			}
		}
		
		//user is in datagroup
		assertTrue(containsDatagroup);
		//reasons contains the usergroup
		assertTrue( person.getDatagroupReason().get(community_datagroup.getId().toString()).contains(community_usergroup.getId().toString()) );
	}
	
	@Test
	public void testDeleteUserGroupInDatagroup()
	{
		CommunityPojo community_usergroup = createTestCommunity(CommunityType.user, driver_local, new ResponseObject());
		PersonPojo person = PersonTest.createTestUser("user", driver_local, new ResponseObject());
		
		//put user in user group
		ResponseObject ro = new ResponseObject();
		driver_local.addToCommunity(community_usergroup.getId().toString(), person.get_id().toString(), ro);
		assertTrue(ro.isSuccess());
		
		//create a datagroup
		CommunityPojo community_datagroup = createTestCommunity(CommunityType.data, driver_local, new ResponseObject());		
		
		//put usergroup in datagroup
		ResponseObject ro1 = new ResponseObject();
		driver_local.addToCommunity(community_datagroup.getId().toString(), community_usergroup.getId().toString(), ro1);
		assertTrue(ro1.isSuccess());
		
		//check user.communities contains datagroup and user.datagroup_reason contains usergroup
		ResponseObject ro2 = new ResponseObject();
		person = driver_local.getPerson(person.get_id().toString(), ro2);
		boolean containsDatagroup = false;
		for ( PersonCommunityPojo cmp : person.getCommunities() )
		{
			if ( cmp.get_id().equals(community_datagroup.getId().toString()) )
			{
				containsDatagroup = true;
				break;
			}
		}
		
		//user is in datagroup
		assertTrue(containsDatagroup);
		//reasons contains the usergroup
		assertTrue( person.getDatagroupReason().get(community_datagroup.getId().toString()).contains(community_usergroup.getId().toString()) );
		
		//NOW remove the usergroup, make sure user doesn't have the datagroup and the datagroup reason is removed
		ResponseObject ro3 = new ResponseObject();
		assertTrue(driver_local.deleteCommunity(community_usergroup.getId().toString(), ro3));
		assertTrue(driver_local.deleteCommunity(community_usergroup.getId().toString(), ro3)); //have to call twice for perma delete
		
		ro2 = new ResponseObject();
		person = driver_local.getPerson(person.get_id().toString(), ro2);
		containsDatagroup = false;
		for ( PersonCommunityPojo cmp : person.getCommunities() )
		{
			if ( cmp.get_id().equals(community_datagroup.getId().toString()) )
			{
				containsDatagroup = true;
				break;
			}
		}
		
		//user should not be in datagroup
		assertFalse(containsDatagroup);
		Set<String> reason = person.getDatagroupReason().get(community_datagroup.getId().toString());
		//user should not have usergroup as a reason for having the datagroup (should be empty via this test)
		assertTrue( reason == null || !reason.contains(community_usergroup.getId().toString()));
	}
	
	@Test
	public void testDeleteDatagroup()
	{
		CommunityPojo community_usergroup = createTestCommunity(CommunityType.user, driver_local, new ResponseObject());
		PersonPojo person = PersonTest.createTestUser("user", driver_local, new ResponseObject());
		
		//put user in user group
		ResponseObject ro = new ResponseObject();
		driver_local.addToCommunity(community_usergroup.getId().toString(), person.get_id().toString(), ro);
		assertTrue(ro.isSuccess());
		
		//create a datagroup
		CommunityPojo community_datagroup = createTestCommunity(CommunityType.data, driver_local, new ResponseObject());		
		
		//put usergroup in datagroup
		ResponseObject ro1 = new ResponseObject();
		driver_local.addToCommunity(community_datagroup.getId().toString(), community_usergroup.getId().toString(), ro1);
		assertTrue(ro1.isSuccess());
		
		//check user.communities contains datagroup and user.datagroup_reason contains usergroup
		ResponseObject ro2 = new ResponseObject();
		person = driver_local.getPerson(person.get_id().toString(), ro2);
		boolean containsDatagroup = false;
		for ( PersonCommunityPojo cmp : person.getCommunities() )
		{
			if ( cmp.get_id().equals(community_datagroup.getId().toString()) )
			{
				containsDatagroup = true;
				break;
			}
		}
		
		//user is in datagroup
		assertTrue(containsDatagroup);
		//reasons contains the usergroup
		assertTrue( person.getDatagroupReason().get(community_datagroup.getId().toString()).contains(community_usergroup.getId().toString()) );
		
		//NOW remove the usergroup, make sure user doesn't have the datagroup and the datagroup reason is removed
		ResponseObject ro3 = new ResponseObject();
		assertTrue(driver_local.deleteCommunity(community_datagroup.getId().toString(), ro3));
		assertTrue(driver_local.deleteCommunity(community_datagroup.getId().toString(), ro3)); //have to call twice for perma delete
		
		ro2 = new ResponseObject();
		person = driver_local.getPerson(person.get_id().toString(), ro2);
		containsDatagroup = false;
		for ( PersonCommunityPojo cmp : person.getCommunities() )
		{
			if ( cmp.get_id().equals(community_datagroup.getId().toString()) )
			{
				containsDatagroup = true;
				break;
			}
		}
		
		//user should not be in datagroup
		assertFalse(containsDatagroup);
		Set<String> reason = person.getDatagroupReason().get(community_datagroup.getId().toString());
		//user should not have usergroup as a reason for having the datagroup (should be empty via this test)
		assertTrue( reason == null || !reason.contains(community_usergroup.getId().toString()));
	}
	
	@Test
	public void testRemoveUserGroupFromDataGroup()
	{
		CommunityPojo community_usergroup = createTestCommunity(CommunityType.user, driver_local, new ResponseObject());
		PersonPojo person = PersonTest.createTestUser("user", driver_local, new ResponseObject());
		
		//put user in user group
		ResponseObject ro = new ResponseObject();
		driver_local.addToCommunity(community_usergroup.getId().toString(), person.get_id().toString(), ro);
		assertTrue(ro.isSuccess());
		
		//create a datagroup
		CommunityPojo community_datagroup = createTestCommunity(CommunityType.data, driver_local, new ResponseObject());		
		
		//put usergroup in datagroup
		ResponseObject ro1 = new ResponseObject();
		driver_local.addToCommunity(community_datagroup.getId().toString(), community_usergroup.getId().toString(), ro1);
		assertTrue(ro1.isSuccess());
		
		//check user.communities contains datagroup and user.datagroup_reason contains usergroup
		ResponseObject ro2 = new ResponseObject();
		person = driver_local.getPerson(person.get_id().toString(), ro2);
		boolean containsDatagroup = false;
		for ( PersonCommunityPojo cmp : person.getCommunities() )
		{
			if ( cmp.get_id().equals(community_datagroup.getId().toString()) )
			{
				containsDatagroup = true;
				break;
			}
		}
		
		//user is in datagroup
		assertTrue(containsDatagroup);
		//reasons contains the usergroup
		assertTrue( person.getDatagroupReason().get(community_datagroup.getId().toString()).contains(community_usergroup.getId().toString()) );
		
		//NOW remove the usergroup, make sure user doesn't have the datagroup and the datagroup reason is removed
		ResponseObject ro3 = new ResponseObject();
		driver_local.removeUserFromCommunity(community_usergroup.getId().toString(), community_datagroup.getId().toString(), ro3);		
		
		ro2 = new ResponseObject();
		person = driver_local.getPerson(person.get_id().toString(), ro2);
		containsDatagroup = false;
		for ( PersonCommunityPojo cmp : person.getCommunities() )
		{
			if ( cmp.get_id().equals(community_datagroup.getId().toString()) )
			{
				containsDatagroup = true;
				break;
			}
		}
		
		//user should not be in datagroup
		assertFalse(containsDatagroup);
		Set<String> reason = person.getDatagroupReason().get(community_datagroup.getId().toString());
		//user should not have usergroup as a reason for having the datagroup (should be empty via this test)
		assertTrue( reason == null || !reason.contains(community_usergroup.getId().toString()));
	}
	
	@Test
	public void testUserInDatagroupAndUserGroupInDatagroup1()
	{
		CommunityPojo community_usergroup = createTestCommunity(CommunityType.user, driver_local, new ResponseObject());
		PersonPojo person = PersonTest.createTestUser("user", driver_local, new ResponseObject());
		
		//put user in user group
		ResponseObject ro = new ResponseObject();
		driver_local.addToCommunity(community_usergroup.getId().toString(), person.get_id().toString(), ro);
		assertTrue(ro.isSuccess());
		
		//create a datagroup
		CommunityPojo community_datagroup = createTestCommunity(CommunityType.data, driver_local, new ResponseObject());		
		
		//put usergroup in datagroup
		ResponseObject ro1 = new ResponseObject();
		driver_local.addToCommunity(community_datagroup.getId().toString(), community_usergroup.getId().toString(), ro1);
		assertTrue(ro1.isSuccess());
		
		//put user in datagroup (so they are now in there twice, once in usergroup and once as user
		ResponseObject ro5 = new ResponseObject();
		driver_local.addToCommunity(community_datagroup.getId().toString(), person.get_id().toString().toString(), ro5);
		assertTrue(ro5.isSuccess());
		
		//check user.communities contains datagroup and user.datagroup_reason contains usergroup
		ResponseObject ro2 = new ResponseObject();
		person = driver_local.getPerson(person.get_id().toString(), ro2);
		boolean containsDatagroup = false;
		for ( PersonCommunityPojo cmp : person.getCommunities() )
		{
			if ( cmp.get_id().equals(community_datagroup.getId().toString()) )
			{
				containsDatagroup = true;
				break;
			}
		}
		
		//user is in datagroup
		assertTrue(containsDatagroup);
		//reasons contains the usergroup
		assertTrue( person.getDatagroupReason().get(community_datagroup.getId().toString()).contains(community_usergroup.getId().toString()) );
		
		//remove usergroup and make sure user is still in datagroup (because of user)
		ResponseObject ro3 = new ResponseObject();
		driver_local.removeUserFromCommunity(community_usergroup.getId().toString(), community_datagroup.getId().toString(), ro3);
		
		//check user.communities contains datagroup and user.datagroup_reason contains usergroup
		ResponseObject ro4 = new ResponseObject();
		person = driver_local.getPerson(person.get_id().toString(), ro4);
		containsDatagroup = false;
		for ( PersonCommunityPojo cmp : person.getCommunities() )
		{
			if ( cmp.get_id().equals(community_datagroup.getId().toString()) )
			{
				containsDatagroup = true;
				break;
			}
		}
		
		//user is in datagroup
		assertTrue(containsDatagroup);
		//reasons contains the user
		assertTrue( person.getDatagroupReason().get(community_datagroup.getId().toString()).contains(person.get_id().toString()) );
	}
	
	@Test
	public void testUserInDatagroupAndUserGroupInDatagroup2()
	{
		CommunityPojo community_usergroup = createTestCommunity(CommunityType.user, driver_local, new ResponseObject());
		PersonPojo person = PersonTest.createTestUser("user", driver_local, new ResponseObject());
		
		//put user in user group
		ResponseObject ro = new ResponseObject();
		driver_local.addToCommunity(community_usergroup.getId().toString(), person.get_id().toString(), ro);
		assertTrue(ro.isSuccess());
		
		//create a datagroup
		CommunityPojo community_datagroup = createTestCommunity(CommunityType.data, driver_local, new ResponseObject());		
		
		//put usergroup in datagroup
		ResponseObject ro1 = new ResponseObject();
		driver_local.addToCommunity(community_datagroup.getId().toString(), community_usergroup.getId().toString(), ro1);
		assertTrue(ro1.isSuccess());
		
		//put user in datagroup (so they are now in there twice, once in usergroup and once as user
		ResponseObject ro5 = new ResponseObject();
		driver_local.addToCommunity(community_datagroup.getId().toString(), person.get_id().toString().toString(), ro5);
		assertTrue(ro5.isSuccess());
		
		//check user.communities contains datagroup and user.datagroup_reason contains usergroup
		ResponseObject ro2 = new ResponseObject();
		person = driver_local.getPerson(person.get_id().toString(), ro2);
		boolean containsDatagroup = false;
		for ( PersonCommunityPojo cmp : person.getCommunities() )
		{
			if ( cmp.get_id().equals(community_datagroup.getId().toString()) )
			{
				containsDatagroup = true;
				break;
			}
		}
		
		//user is in datagroup
		assertTrue(containsDatagroup);
		//reasons contains the usergroup
		assertTrue( person.getDatagroupReason().get(community_datagroup.getId().toString()).contains(community_usergroup.getId().toString()) );
		
		//TODO when you tell it to remove a user, it doesn't check if user is part of a datagroup that is a member
		
		//remove user and make sure user is still in datagroup (because of usergroup)
		ResponseObject ro3 = new ResponseObject();
		driver_local.removeUserFromCommunity(person.get_id().toString(), community_datagroup.getId().toString(), ro3);
		
		//check user.communities contains datagroup and user.datagroup_reason contains usergroup
		ResponseObject ro4 = new ResponseObject();
		person = driver_local.getPerson(person.get_id().toString(), ro4);
		containsDatagroup = false;
		for ( PersonCommunityPojo cmp : person.getCommunities() )
		{
			if ( cmp.get_id().equals(community_datagroup.getId().toString()) )
			{
				containsDatagroup = true;
				break;
			}
		}
		
		//user is in datagroup
		assertTrue(containsDatagroup);
		//reasons contains the user
		assertTrue( person.getDatagroupReason().get(community_datagroup.getId().toString()).contains(community_usergroup.getId().toString()) );
	}
	
	@Test
	public void testInviteUserToDatagroup()
	{
		//Test that datagroup_reason works when a user is invited to a group and ends up "pending"
		//then fully becomes a member				
		PersonPojo person = PersonTest.createTestUser("user", driver_local, new ResponseObject());
		
		//create a datagroup
		CommunityPojo community_datagroup = createTestCommunity(CommunityType.data, driver_local, new ResponseObject());
		//set it to not self register so invites will be sent
		Map<String, CommunityAttributePojo> attr = community_datagroup.getCommunityAttributes();
		attr.put("usersCanSelfRegister", new CommunityAttributePojo("Boolean", "false"));
		community_datagroup.setCommunityAttributes(attr);
		ResponseObject ro2 = new ResponseObject();		
		assertTrue( driver_local.updateCommunity(community_datagroup.getId().toString(), community_datagroup, ro2));
		ResponseObject ro3 = new ResponseObject();
		community_datagroup = driver_local.getCommunity(community_datagroup.getId().toString(), ro3);
		
		//invite user to datagroup
		ResponseObject ro1 = new ResponseObject();
		driver_local.addToCommunity(community_datagroup.getId().toString(), person.get_id().toString(), ro1);
		
		//accept invite (i think we can just try to join comm as user)
		InfiniteDriver driver_user = new InfiniteDriver(apiRootUrl, person.getEmail(), PersonTest.defaultTestPassword);
		driver_user.login();
		ResponseObject ro4 = new ResponseObject();
		driver_user.joinCommunity(community_datagroup.getId().toString(), ro4);
			
		//make sure user is in DG
		person = driver_local.getPerson(person.get_id().toString(), ro1);
		boolean containsDatagroup = false;
		for ( PersonCommunityPojo cmp : person.getCommunities() )
		{
			if ( cmp.get_id().equals(community_datagroup.getId().toString()) )
			{
				containsDatagroup = true;
				break;
			}
		}
		assertTrue(containsDatagroup);
	}
	
	@Test
	public void testInviteUserToUsergroupAlreadyInDatagroup()
	{
		//Test that datagroup_reason works when a user is invited to a group and ends up "pending"
		//then fully becomes a member		
		CommunityPojo community_usergroup = createTestCommunity(CommunityType.user, driver_local, new ResponseObject());
		//set it to not self register so invites will be sent
		Map<String, CommunityAttributePojo> attr = community_usergroup.getCommunityAttributes();
		attr.put("usersCanSelfRegister", new CommunityAttributePojo("Boolean", "false"));
		community_usergroup.setCommunityAttributes(attr);
		ResponseObject ro2 = new ResponseObject();		
		assertTrue( driver_local.updateCommunity(community_usergroup.getId().toString(), community_usergroup, ro2));
		ResponseObject ro3 = new ResponseObject();
		community_usergroup = driver_local.getCommunity(community_usergroup.getId().toString(), ro3);
		
		PersonPojo person = PersonTest.createTestUser("user", driver_local, new ResponseObject());
		
		//invite user to user group
		ResponseObject ro = new ResponseObject();
		driver_local.addToCommunity(community_usergroup.getId().toString(), person.get_id().toString(), ro);
		assertTrue(ro.isSuccess());
		
		//create a datagroup
		CommunityPojo community_datagroup = createTestCommunity(CommunityType.data, driver_local, new ResponseObject());
		
		
		//add usergroup to datagroup
		ResponseObject ro1 = new ResponseObject();
		driver_local.addToCommunity(community_datagroup.getId().toString(), community_usergroup.getId().toString(), ro1);
		
		//make sure user is not in datagroup
		boolean containsDatagroup = false;
		for ( PersonCommunityPojo cmp : person.getCommunities() )
		{
			if ( cmp.get_id().equals(community_datagroup.getId().toString()) )
			{
				containsDatagroup = true;
				break;
			}
		}
		assertFalse(containsDatagroup);
		
		//accept invite (i think we can just try to join comm as user)
		InfiniteDriver driver_user = new InfiniteDriver(apiRootUrl, person.getEmail(), PersonTest.defaultTestPassword);
		driver_user.login();
		ResponseObject ro4 = new ResponseObject();
		driver_user.joinCommunity(community_usergroup.getId().toString(), ro4);
			
		//make sure user is in DG now
		person = driver_local.getPerson(person.get_id().toString(), ro);
		containsDatagroup = false;
		for ( PersonCommunityPojo cmp : person.getCommunities() )
		{
			if ( cmp.get_id().equals(community_datagroup.getId().toString()) )
			{
				containsDatagroup = true;
				break;
			}
		}
		assertTrue(containsDatagroup);
	}
	
	@Test
	public void testGetAll()
	{
		ResponseObject ro = new ResponseObject();
		//test getting all communities, nothing specific we can test for except we find >1 
		//different communities
		List<CommunityPojo> communities = driver_local.getAllCommunity(ro, null);
		assertTrue(communities.size() > 2 );
		String commid1 = communities.get(0).getId().toString();
		String commid2 = communities.get(1).getId().toString();
		
		//test filtering to a single community
		List<CommunityPojo> comm_single = driver_local.getAllCommunity(ro, commid1);
		assertEquals(1, comm_single.size());
		
		//test filtering to 2 communities
		List<CommunityPojo> comm_double = driver_local.getAllCommunity(ro, commid1+","+commid2);
		assertEquals(2, comm_double.size());
	}
	
	@Test
	public void testGetAllUser()
	{
		InfiniteDriver driver_user = new InfiniteDriver(apiRootUrl, apiKeyUser);
		ResponseObject ro = new ResponseObject();
		//test getting all communities, nothing specific we can test for except we find >1 
		//different communities
		List<CommunityPojo> communities = driver_user.getAllCommunity(ro, null);
		assertTrue(communities.size() > 2 );
		String commid1 = communities.get(0).getId().toString();
		String commid2 = communities.get(1).getId().toString();
		
		//test filtering to a single community
		List<CommunityPojo> comm_single = driver_user.getAllCommunity(ro, commid1);
		assertEquals(1, comm_single.size());
		
		//test filtering to 2 communities
		List<CommunityPojo> comm_double = driver_user.getAllCommunity(ro, commid1+","+commid2);
		assertEquals(2, comm_double.size());
	}
	
	@Test
	public void testAddCommunityCustomAttributes()
	{
		Map<String, CommunityAttributePojo> community_attributes = new HashMap<String, CommunityAttributePojo>();
		community_attributes.put("test_attr1", new CommunityAttributePojo("Boolean", "false"));
		Map<String, CommunityUserAttributePojo> user_attributes = new HashMap<String, CommunityUserAttributePojo>();
		user_attributes.put("test_attr2", new CommunityUserAttributePojo("Integer", "1", false));
		CommunityPojo community_usergroup = createTestCommunity(CommunityType.user, driver_local, new ResponseObject(), community_attributes, user_attributes);
		assertNotNull(community_usergroup);
		assertEquals("false", community_usergroup.getCommunityAttributes().get("test_attr1").getValue());
		assertEquals("1", community_usergroup.getCommunityUserAttribute().get("test_attr2").getDefaultValue());
		
		//test the defaults still exist
		assertEquals("false", community_usergroup.getCommunityAttributes().get("isPublic").getValue());		
	}
	
	@Test
	public void testUpdateCommunityCustomAttributes()
	{
		CommunityPojo community_usergroup = createTestCommunity(CommunityType.user, driver_local, new ResponseObject());
		//test the defaults still exist
		assertEquals("false", community_usergroup.getCommunityAttributes().get("isPublic").getValue());
		
		//change teh attributes
		Map<String, CommunityAttributePojo> community_attributes = new HashMap<String, CommunityAttributePojo>();
		community_attributes.put("test_attr1", new CommunityAttributePojo("Boolean", "false"));
		Map<String, CommunityUserAttributePojo> user_attributes = new HashMap<String, CommunityUserAttributePojo>();
		user_attributes.put("test_attr2", new CommunityUserAttributePojo("Integer", "1", false));
		community_usergroup.setCommunityAttributes(community_attributes);
		community_usergroup.setCommunityUserAttribute(user_attributes);
		driver_local.updateCommunity(community_usergroup.getId().toString(), community_usergroup, new ResponseObject());
		community_usergroup = driver_local.getCommunity(community_usergroup.getId().toString(), new ResponseObject());
		
		//test the defaults still exist, and our new args
		assertEquals("false", community_usergroup.getCommunityAttributes().get("isPublic").getValue());
		assertEquals("false", community_usergroup.getCommunityAttributes().get("test_attr1").getValue());
		assertEquals("1", community_usergroup.getCommunityUserAttribute().get("test_attr2").getDefaultValue());
	}
}
