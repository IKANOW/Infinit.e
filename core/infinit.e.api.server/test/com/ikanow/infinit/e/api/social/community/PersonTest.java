package com.ikanow.infinit.e.api.social.community;

import static org.junit.Assert.*;

import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ikanow.infinit.e.data_model.api.ResponsePojo.ResponseObject;
import com.ikanow.infinit.e.data_model.driver.InfiniteDriver;
import com.ikanow.infinit.e.data_model.store.social.person.PersonPojo;

public class PersonTest {

	public static final String defaultTestPassword = "123456";
	private static String apiRootUrl = "http://localhost:8184/"; //TODO move this out somewhere else
	private static String apiKey = "12345";
	private static InfiniteDriver driver_local;
	private static long counter = 0;
	private static String test_user_name_prefix = "test123_";
	private static Logger logger = LogManager.getLogger(PersonTest.class);
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		driver_local = new InfiniteDriver(apiRootUrl, apiKey);
		deleteTestUsers(driver_local, new ResponseObject());
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {		
		deleteTestUsers(driver_local, new ResponseObject());
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}
	
	//TODO need to create an enum for accountType
	public static PersonPojo createTestUser(String accountType, InfiniteDriver driver, ResponseObject responseObject)
	{
		String user_email = test_user_name_prefix + counter+"@ikanow.com";
		PersonPojo pp = null;
		//register doesn't return the user so we have to look them up after
		driver.registerPerson(test_user_name_prefix + counter, test_user_name_prefix + counter, "555-555-5555", user_email, defaultTestPassword, accountType, responseObject);		
		counter++;
		if ( responseObject.isSuccess() )
		{
			pp = driver.getPerson(user_email, responseObject);
		}
		return pp;
	}
	
	public static void deleteTestUsers(InfiniteDriver driver, ResponseObject responseObject)
	{		
		//Delete test users with name test123_
		List<PersonPojo> persons = driver.listPerson(responseObject);
		for ( PersonPojo person : persons )
		{
			if ( person.getFirstName().startsWith(test_user_name_prefix) )
			{
				driver.deletePerson(person.get_id().toString(), responseObject);
				if ( !responseObject.isSuccess() )
					logger.error("Error cleaning up persons in PersonTest, could not delete person: " + person.getFirstName());
			}
		}
	}

	@Test
	public void testAccountType() {
		ResponseObject ro = new ResponseObject();
		PersonPojo person = createTestUser("user", driver_local, ro);
		assertNotNull(person);
		assertEquals("user", person.getAccountType());
		
		person = createTestUser("admin", driver_local, ro);
		assertNotNull(person);
		assertEquals("admin", person.getAccountType());
	}

}
