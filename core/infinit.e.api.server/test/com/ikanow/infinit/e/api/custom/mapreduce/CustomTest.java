package com.ikanow.infinit.e.api.custom.mapreduce;

import static org.junit.Assert.*;

import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ikanow.infinit.e.api.social.community.CommunityTest;
import com.ikanow.infinit.e.api.social.community.PersonTest;
import com.ikanow.infinit.e.api.social.sharing.ShareV2ApiTest;
import com.ikanow.infinit.e.api.utils.ProjectUtilsTest;
import com.ikanow.infinit.e.data_model.api.ResponsePojo.ResponseObject;
import com.ikanow.infinit.e.data_model.driver.InfiniteDriver;
import com.ikanow.infinit.e.data_model.store.custom.mapreduce.CustomMapReduceJobPojo;
import com.ikanow.infinit.e.data_model.store.custom.mapreduce.CustomMapReduceJobPojo.SCHEDULE_FREQUENCY;

public class CustomTest {

	private static String apiRootUrl = "http://localhost:8184/";
	private static String apiKey = "12345";
	private static InfiniteDriver driver_local;
	private static long counter = 0;
	private static String test_custom_name_prefix = "test123_custom123_";
	private static Logger logger = LogManager.getLogger(CustomTest.class);
	
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
		ProjectUtilsTest.removeTestProjects(driver_local, new ResponseObject());
		PersonTest.deleteTestUsers(driver_local, new ResponseObject());
		CommunityTest.removeTestCommunities(driver_local, new ResponseObject());			
		ShareV2ApiTest.removeTestShares(driver_local, new ResponseObject());
		CustomTest.removeTestCustoms(driver_local, new ResponseObject());
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}		
	
	public static CustomMapReduceJobPojo createTestCustom(InfiniteDriver driver, ResponseObject responseObject, List<ObjectId> communityIds)
	{
		CustomMapReduceJobPojo custom = new CustomMapReduceJobPojo();
		custom.jobtitle = test_custom_name_prefix + counter;
		counter++;
		custom.jobdesc = "test";
		custom.communityIds = communityIds;
		custom.jarURL = "aaa";
		custom.nextRunTime = 0;
		custom.scheduleFreq = SCHEDULE_FREQUENCY.NONE;
		custom.mapper = "aaa";
		custom.query = "aaa";
		custom.inputCollection = "DOC_METADATA";
		custom.outputKey = "aaa";
		custom.outputValue = "aaa";
		
		custom._id = driver.createCustomPluginTask(custom, null, communityIds, responseObject);
		return custom;				
	}
	
	public static void removeTestCustoms(InfiniteDriver driver, ResponseObject responseObject)
	{
		List<CustomMapReduceJobPojo> customs = driver.getCustomTaskOrQuery(null, responseObject);
		if ( customs != null )
		{
			for (CustomMapReduceJobPojo custom : customs )
			{
				if ( custom.jobtitle.startsWith(test_custom_name_prefix))
				{				
					driver.deleteCustomTaskOrQuery(custom._id, responseObject);
					if ( !responseObject.isSuccess() )
						logger.error("Error removing project test share: " + custom._id + " : " + custom.jobtitle);
				}
			}
		}
	}

	@Test
	public void test() {
		fail("Not yet implemented");
	}

}
