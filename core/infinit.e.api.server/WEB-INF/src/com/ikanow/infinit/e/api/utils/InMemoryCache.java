package com.ikanow.infinit.e.api.utils;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * A generic cache that let's you specify a time to live for
 * cache items, doesn't bother doing any active cleanup of expired items
 * though, need to add that capability in
 * 
 * @author Burch
 *
 */
public class InMemoryCache<T> {
	private long time_to_live_ms = 0;
	private Map<String, InMemoryCacheItem<T>> cache;
	private CacheStats stats;
	private static Logger logger = LogManager.getLogger(InMemoryCache.class);
	
	public InMemoryCache(long time_to_live_ms)
	{
		this.time_to_live_ms = time_to_live_ms;
		cache = new HashMap<String, InMemoryCacheItem<T>>();
		stats = new CacheStats();
	}
	
	public T getEntry(String key)
	{
		InMemoryCacheItem<T> value = cache.get(key);
		if ( value != null )
		{
			if ( value.time_added + time_to_live_ms > System.currentTimeMillis() )
			{
				logger.info("Key: " + key + " found, returning");
				stats.hits++;
				return value.item;
			}
			else
			{
				//timed out			
				logger.info("Key: " + key + " timed out.");
				stats.removals++;
				cache.remove(key);
			}
		}
		stats.misses++;
		return null;
	}
	
	public T addEntry(String key, T value)
	{
		logger.info("Key: " + key + " added");
		stats.inserts++;
		cache.put(key, new InMemoryCacheItem<T>(System.currentTimeMillis(), value));
		return value;
	}
	
	public boolean containsEntry(String key)
	{
		return getEntry(key) != null;			
	}
	
	public CacheStats getCacheStats()
	{
		return stats;
	}
	
	public void resetCacheStats()
	{
		stats = new CacheStats();
	}
	
	public class InMemoryCacheItem<S>
	{
		public long time_added;
		public S item;
		
		public InMemoryCacheItem(long time_added, S item)
		{
			this.time_added = time_added;
			this.item = item;
		}
	}
	
	public class CacheStats
	{
		public long hits = 0;
		public long misses = 0;
		public long inserts = 0;
		public long removals = 0;
		
		@Override
		public String toString()
		{
			return "CacheStats: hits: " + hits + " misses: " + misses + " inserts: " + inserts + " removals: " + removals;
		}
	}
}
