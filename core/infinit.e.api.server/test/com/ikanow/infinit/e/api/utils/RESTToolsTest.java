package com.ikanow.infinit.e.api.utils;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ikanow.infinit.e.data_model.api.ResponsePojo.ResponseObject;
import com.ikanow.infinit.e.data_model.driver.InfiniteDriver;

public class RESTToolsTest {
	
	private static String apiRootUrl = "http://localhost:8184/";
	private static String apiKey = "12345";
	private static InfiniteDriver driver_local;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		driver_local = new InfiniteDriver(apiRootUrl, apiKey);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testCache() throws InterruptedException {
		//make some back to back api calls that should be checking cookie lookup
		//and verify we are getting cache hits
		ResponseObject ro = new ResponseObject();
		assertNotNull(driver_local.getPerson(null, ro));
		assertNotNull(driver_local.getPerson(null, ro));
		assertNotNull(driver_local.getPerson(null, ro));
		Thread.sleep(5000); //let cache expire
		assertNotNull(driver_local.getPerson(null, ro));
		//NOTE: you can only see the cache hits by putting in a
		//System.out.println(cache.getCacheStats()) in resttools	
		//and looking at the output, it should be:
		//CacheStats: hits: 2 misses: 2 inserts: 1 removals: 1
		//remember it doesn't reset unless you stop the api
	}

}
