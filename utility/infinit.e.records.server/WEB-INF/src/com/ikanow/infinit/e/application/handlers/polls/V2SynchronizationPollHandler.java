/*******************************************************************************
 * Copyright 2012 The Infinit.e Open Source Project sponsored by IKANOW.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.ikanow.infinit.e.application.handlers.polls;

import java.net.URLDecoder;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.log4j.Logger;
import org.bson.types.ObjectId;

import com.ikanow.infinit.e.data_model.store.MongoDbManager;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.mongodb.BasicDBList;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;

/**
 * @author Alex
 */
public class V2SynchronizationPollHandler implements PollHandler {
	private static final Logger _logger = Logger.getLogger(V2SynchronizationPollHandler.class);

	protected final static ConcurrentHashMap<String, Map<String, String>> _v2_bucket_cache = new ConcurrentHashMap<>();	
	
	private static ConcurrentHashMap<String, Map<String, BucketPathInfo>> _v2_mapping = new ConcurrentHashMap<>();
	// comm id -> (v1 key -> v2 es index)* 

	public static class BucketPathInfo {
		BucketPathInfo(String key, String path, String index, String comm_id) {
			this.key = key;
			this.path = path;
			this.index = index;
			this.comm_id = comm_id;
		}
		final String key;
		final String path;
		final String index;
		final String comm_id;
	}//TESTED
	
	// Alternative view of the same data
	private static ConcurrentNavigableMap<String, BucketPathInfo> _v2_path_or_key_mapping = new ConcurrentSkipListMap<>();
	// v1.key AND v2.path -> (v2 es index, community_ids)
	
	/**
	 * @param comm_id_str
	 * @param negative_set
	 * @param user_id
	 * @param show_objects
	 * @param show_tests
	 * @param show_logging
	 * @param cache_key
	 * @return key (path or source) -> index prefix (not including data suffix or any wildcards)
	 */
	public static Map<String, String> getV2BucketsInCommunity(final String comm_id_str, final Set<String> negative_set, final String user_id, boolean show_objects, boolean show_tests, boolean show_logging, final String cache_key) {
		final String final_cache_key = comm_id_str + ":" + user_id + ":" + cache_key; //(is called individually for each community)
		try {
			return _v2_bucket_cache.computeIfAbsent(final_cache_key, __ -> {
				return _v2_mapping.getOrDefault(comm_id_str, Collections.emptyMap()).entrySet().stream()
						.filter(kv -> (null == negative_set) || !negative_set.contains(kv.getKey()))
						.flatMap(kv -> {
							return Stream.concat(
									show_objects ? Stream.of(kv.getValue()) : Stream.empty()
									,
									show_tests ? Stream.of(getTestVersion(kv.getValue(), user_id)) : Stream.empty()
									);					
						})
						.collect(Collectors.toMap(info -> info.key, info -> info.index))
						;
			})
			;
		}
		catch (Throwable t) { return new HashMap<String, String>(); } // won't happen in practice
		//TESTED
	}

	/**
	 * @param bucket_paths
	 * @param comm_ids
	 * @param user_id
	 * @param show_objects
	 * @param show_tests
	 * @param show_logging
	 * @param cache_key
	 * @return key (path or source) -> index prefix (not including data suffix or any wildcards)
	 */
	public static Map<String, String> getV2BucketsInCommunity(final HashSet<String> bucket_paths, final HashSet<String> comm_ids, final String user_id, boolean show_objects, boolean show_tests, boolean show_logging, final String cache_key) {
		final String final_cache_key = user_id + ":" + cache_key;
		try {
			return _v2_bucket_cache.computeIfAbsent(final_cache_key, __ -> {
					return bucket_paths.stream()
							.map(s -> { try { return URLDecoder.decode(s, "UTF-8"); } catch (Throwable t) { return s; } })
							.collect(Collectors.partitioningBy(s -> s.startsWith("/") && s.matches(".*[*?].*")))
							.entrySet()
							.stream()
							.filter(kv -> !kv.getValue().isEmpty())
							.flatMap(kv -> {
								if (kv.getKey()) { // this is a wildcarded path, which is nice
									return kv.getValue().stream()
											.flatMap(s -> matchWildcardedPath(s))
											;
								}
								else { // this is a nice easy case, it's a source key (or a wildcard-less path, treat both the same...)
									return kv.getValue().stream()
											.map(ss -> (!ss.startsWith("/") && !ss.endsWith(";")) ? (ss + ";") : ss)
											.map(ss -> _v2_path_or_key_mapping.get(ss))
											.filter(ret -> null != ret)
											;
								}
							})//TESTED x2
							.filter(info -> comm_ids.contains(info.comm_id)) //(ie have permission)
							.flatMap(info -> {
								return Stream.concat(
										show_objects ? Stream.of(info) : Stream.empty()
										,
										show_tests ? Stream.of(getTestVersion(info, user_id)) : Stream.empty()
										);
							})//TESTED x3
							.collect(Collectors.toMap(info -> info.key, info -> info.index))
							;
			});
		}
		catch (Throwable t) { return new HashMap<String, String>(); } // won't happen in practice
		
	}//TESTED
	
	public static Stream<BucketPathInfo> matchWildcardedPath(final String path) {
		final String longest_subpath = path.replaceFirst("[*?].*", "");
		
		final ConcurrentNavigableMap<String, BucketPathInfo> tail_map = _v2_path_or_key_mapping.tailMap(longest_subpath, true);
		final LinkedList<BucketPathInfo> mutable_matching = new LinkedList<>();
		
		PathMatcher matcher = null;
		
		for (Map.Entry<String,BucketPathInfo> kv: tail_map.entrySet()) {		
			if (!kv.getKey().startsWith(longest_subpath)) break; //(all done)
			
			// We're hierarchically below this entry, is it match vs the glob though?
			
			if (null == matcher) {
				matcher = FileSystems.getDefault().getPathMatcher("glob:" + path);
			}
			final java.nio.file.Path p = FileSystems.getDefault().getPath(kv.getKey());
			if (matcher.matches(p)) {
				mutable_matching.add(kv.getValue());
			}
		}		
		return mutable_matching.stream();
	}//TESTED
	
	
	private ObjectId _last_checked_id = new ObjectId(new Date());
	private Date _last_checked_time = new Date(0L);
	private long _last_sources = 0L;
	
	private static long FULL_CHECK_MS = 5L*60L*1000L;
	
	protected boolean on_startup = true;
	
	@SuppressWarnings("deprecation")
	@Override
	public void performPoll() {
		
		try {
			final DBCollection sources = MongoDbManager.getIngest().getSource();
			
			// Every 10s, check if there are any new elements in the source DB
			if (null != _last_checked_id) {
				final DBObject initial_query = QueryBuilder.start().put(SourcePojo._id_).greaterThan(_last_checked_id).get();
				final DBObject initial_sort = QueryBuilder.start().put(SourcePojo._id_).is(-1).get();
				final DBObject initial_fields = QueryBuilder.start().put(SourcePojo._id_).is(1).get();
				final DBCursor initial_dbc = sources.find(initial_query, initial_fields).sort(initial_sort).limit(1);
				
				if (initial_dbc.hasNext()) { // new _id!					
					_last_checked_id = (ObjectId) initial_dbc.next().get("_id");
					_last_checked_time = new Date(0L);
					_last_sources = sources.count();							
					_logger.info("Detected new source at: " + new Date(_last_checked_id.getTime()));					
				}
				else {
					long curr_sources = sources.count();
					if (_last_sources != curr_sources) {
						_last_checked_time = new Date(0L);
						_logger.info("Detected num_sources change: was=" + _last_sources + " now=" + curr_sources); 					
						_last_sources = curr_sources;			
					}
				}//TESTED (by hand)
			}
			final Date now = new Date();
			if ((now.getTime() - _last_checked_time.getTime()) > FULL_CHECK_MS) {
				_last_checked_time = now;
			}
			else {
				return;
			}
			_v2_bucket_cache.clear(); // (clears the cache because something has changed)
			//TESTED (by hand)
			
			// If so, or alternatively every 5 minutes, recheck everything
			
			final DBObject query = QueryBuilder.start()
										.put(SourcePojo.key_).greaterThanEquals("aleph...bucket.").lessThan("aleph...bucket/")
										.put(SourcePojo.extractType_).is("V2DataBucket")
									.get();
			final DBObject fields = QueryBuilder.start()
										.put(SourcePojo.communityIds_).is(1)
										.put(SourcePojo.key_).is(1)
									.get();
			
			final DBCursor dbc = sources.find(query, fields);
			
			final HashMap<String, String> v2_query_builder = new HashMap<>();
			final HashSet<String> comm_id_set = new HashSet<>(); // (just for info)
			final HashSet<String> all_keys = new HashSet<>(); //(handle buckets to remove)
			
			for (DBObject dbo: dbc) {
				final BasicDBList comm_ids = (BasicDBList) dbo.get(SourcePojo.communityIds_);
				final String key = (String) dbo.get(SourcePojo.key_) + ";";
				all_keys.add(key);
				// Find any changed sources:
				if (null != comm_ids) for (Object comm_id_obj: comm_ids) {
					String comm_id_str = comm_id_obj.toString();
					Map<String, BucketPathInfo> s = _v2_mapping.computeIfAbsent(comm_id_str, __ -> {
						comm_id_set.add(comm_id_str);
						return new ConcurrentHashMap<>();
					});
					if (!s.containsKey(key)) {
						v2_query_builder.put(key, comm_id_str);							
					}
					//(else nothing to do)
				}//(end loop over commid/sources)
			}//(end loop over v2 sources)
			//TESTED (by hand)
			
			if (!v2_query_builder.isEmpty()) {
				_logger.info("Looking for buckets=" + v2_query_builder.size() + " across communities=" + comm_id_set.size());
				
				final DBCollection v2_buckets = MongoDbManager.getCollection("aleph2_data_import", "bucket");
				final DBObject v2_query = QueryBuilder.start()
											.put("_id").in(v2_query_builder.keySet())
										.get();
				
				final DBObject v2_fields  = QueryBuilder.start()
												.put("_id").is(1)
												.put("full_name").is(1)
											.get();
				
				final DBCursor v2_dbc = v2_buckets.find(v2_query, v2_fields);

				int added = 0;
				for (DBObject v2_dbo: v2_dbc) {					
					// Update the hash map:
					final String _id = (String) v2_dbo.get("_id"); // == v1 key + ";"
					final String path = (String) v2_dbo.get("full_name");
					if ((null != _id) && (null != path)) {
						String comm_id = v2_query_builder.get(_id);
						
						if (null != comm_id) {
							final String index = "r__" + getBaseIndexName(path);
							
							Map<String, BucketPathInfo> s = _v2_mapping.computeIfAbsent(comm_id, __ -> new ConcurrentHashMap<>());
							final BucketPathInfo path_info = new BucketPathInfo(_id, path, index, comm_id);
							s.put(_id, path_info);
							_v2_path_or_key_mapping.put(_id, path_info);
							_v2_path_or_key_mapping.put(path, path_info);
							
							added++;
						}
					}
				}//(end loop over v2 buckets)
				_logger.info("Added buckets=" + added);
			}//(end if new buckets discovered)
			//TESTED (by hand)			
			
			int removed = 0;
			for (Map<String, BucketPathInfo> v1s: _v2_mapping.values()) {
				HashSet<String> to_remove = new HashSet<>();
				for (String key: v1s.keySet()) {
					if (!all_keys.contains(key)) {
						to_remove.add(key);
						removed++;
					}
				}
				for (String s: to_remove) {
					v1s.remove(s);
					final BucketPathInfo path_info_to_remove = _v2_path_or_key_mapping.remove(s);
					if (null != path_info_to_remove) {
						_v2_path_or_key_mapping.remove(path_info_to_remove.path);
					}
				}
			}
			//TESTED
			if (removed > 0) {
				_logger.info("Removed buckets=" + removed);
			}			
			
			if (on_startup) {
				_logger.info("Imported v2 sources: " + _v2_mapping.toString());
				_logger.info("Imported v2 paths: " + _v2_path_or_key_mapping.values().stream().map(info -> info.path).collect(Collectors.joining(";")));
				on_startup = false;
			}
		}
		catch (Throwable t) {
			//t.printStackTrace();
			
			// (probably some problem with the DB, wait a minute then try again)
			try {Thread.sleep(60000L); } catch (Exception e) {}
		}
	}

	/////////////////////////////////////////////////////////////////////
	
	// INDEX NAMES
	
	public static BucketPathInfo getTestVersion(final BucketPathInfo info, String user_id) {
		final String new_path = "/aleph2_testing/" + user_id + info.path;
		return new BucketPathInfo("test:" + info.key, 
				new_path,
				"r__" + getBaseIndexName(new_path),
				info.comm_id
				);
	}//TESTED
	
	private static final int MAX_COLL_COMP_LEN = 16;
	
	/** Returns the base index name (before any date strings, splits etc) have been appended
	 *  Generated by taking 1-3 directories from the path and then appening the end of a UUID
	 * @param bucket
	 * @return
	 */
	public static String getBaseIndexName(final String path) {
		
		final String[] components = Optional.of(path)
				.map(p -> p.startsWith("/") ? p.substring(1) : p)
				.get()
				.split("[/]");

		if (1 == components.length) {
			return tidyUpIndexName(safeTruncate(components[0], MAX_COLL_COMP_LEN))
					+ "__" + generateUuidSuffix(path);
		}
		else if (2 == components.length) {
			return tidyUpIndexName(safeTruncate(components[0], MAX_COLL_COMP_LEN) 
					+ "_" + safeTruncate(components[1], MAX_COLL_COMP_LEN))
					+ "__" + generateUuidSuffix(path);
		}
		else { // take the first and the last 2
			final int n = components.length;
			return tidyUpIndexName(safeTruncate(components[0], MAX_COLL_COMP_LEN)
					+ "_" + safeTruncate(components[n-2], MAX_COLL_COMP_LEN) 
					+ "_" + safeTruncate(components[n-1], MAX_COLL_COMP_LEN))
					+ "__" + generateUuidSuffix(path);
		}
	}
	// Utils for getBaseIndexName
	private static String safeTruncate(final String in, final int max_len) {
		return in.length() < max_len ? in : in.substring(0, max_len);
	}
	private static String tidyUpIndexName(final String in) {
		return Optional.of(in.toLowerCase().replaceAll("[^a-z0-9_]", "_").replaceAll("__+", "_"))
				.map(s -> s.endsWith("_") ? s.substring(0, s.length() - 1) : s)
				.get()
				;
	}
	private static String generateUuidSuffix(final String in) {
		return java.util.UUID.nameUUIDFromBytes(in.getBytes()).toString().substring(24);
	}

}
