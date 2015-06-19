package com.ikanow.infinit.e.api.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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
	private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
	
	public InMemoryCache(long time_to_live_ms)
	{
		this.time_to_live_ms = time_to_live_ms;
		cache = new HashMap<String, InMemoryCacheItem<T>>();
		stats = new CacheStats();
		final Runnable cleanup_runnable = new Runnable() {			
			@Override
			public void run() {
				logger.info("Running cleanup thread");
				cleanExpiredEntries();
			}
		};
		executor.scheduleAtFixedRate(cleanup_runnable, 60, 60, TimeUnit.SECONDS);
	}
	
	/**
	 * Returns an entry in the cache if it exists and hasn't expired.
	 * Otherwise returns null.
	 * 
	 * @param key
	 * @return
	 */
	public T getEntry(String key)
	{
		InMemoryCacheItem<T> value = cache.get(key);
		if ( value != null )
		{
			if ( value.time_added + time_to_live_ms > System.currentTimeMillis() )
			{
				stats.hits++;
				logger.trace(getCacheStats());
				return value.item;
			}
			else
			{
				//timed out			
				stats.removals++;
				cache.remove(key);
			}
		}
		stats.misses++;
		logger.trace(getCacheStats());
		return null;
	}
	
	/**
	 * Adds an entry to the cache, will overwrite any existing
	 * entry with the same key.  Sets the insert time to now
	 * so it's TTL clock starts immediately
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	public T addEntry(String key, T value)
	{
		stats.inserts++;
		cache.put(key, new InMemoryCacheItem<T>(System.currentTimeMillis(), value));
		logger.trace(getCacheStats());
		return value;
	}
	
	/**
	 * Returns true if the entry is in the cache, remember that the entry
	 * could expire before you call cache.get().
	 * 
	 * @param key
	 * @return
	 */
	public boolean containsEntry(String key)
	{
		return getEntry(key) != null;			
	}
	
	/**
	 * Removes any entries that have expired.
	 * 
	 */
	private void cleanExpiredEntries()
	{	
		cache.entrySet().stream()
			.filter(entry -> entry.getValue().time_added + time_to_live_ms <= System.currentTimeMillis() )
			.forEach(entry -> {
				cache.remove(entry.getKey());
				stats.removals++;
				logger.trace(getCacheStats());
			});
	}
	
	/**
	 * Returns a debug object that shows how many cache hits, misses
	 * inserts, and removals have occured.
	 * 
	 * @return
	 */
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
