package com.ikanow.infinit.e.api.utils;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class InMemoryCacheTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
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
	public void testCache() {
		InMemoryCache<String> cache = new InMemoryCache<String>(1000);
		assertEquals(0, cache.getCacheStats().inserts);
		
		cache.addEntry("test1", "1");
		cache.addEntry("test2", "2");
		
		assertEquals("1",cache.getEntry("test1"));
		assertEquals(1, cache.getCacheStats().hits);
	}
	
	@Test
	public void testCacheTTL() {
		InMemoryCache<String> cache = new InMemoryCache<String>(0);
		assertEquals(0, cache.getCacheStats().inserts);
		
		cache.addEntry("test1", "1");
		cache.addEntry("test2", "2");
		
		assertEquals(null,cache.getEntry("test1"));
		assertEquals(0, cache.getCacheStats().hits);
	}

}
