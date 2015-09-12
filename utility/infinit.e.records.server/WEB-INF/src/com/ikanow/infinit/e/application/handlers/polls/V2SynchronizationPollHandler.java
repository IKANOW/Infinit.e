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

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

	private static ConcurrentHashMap<String, Map<String, String>> _v2_mapping = new ConcurrentHashMap<>();
	// comm id -> (v1 key -> v2 es index)* 
	
	public static Map<String, String> getV2BucketsInCommunity(final String comm_id_str) {
		return Collections.unmodifiableMap(_v2_mapping.getOrDefault(comm_id_str, Collections.emptyMap()));
	}
	
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
					Map<String, String> s = _v2_mapping.computeIfAbsent(comm_id_str, __ -> {
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
							final String index = getBaseIndexName(path);
							
							Map<String, String> s = _v2_mapping.computeIfAbsent(comm_id, __ -> new ConcurrentHashMap<>());
							s.put(_id, index);
							
							added++;
						}
					}
				}//(end loop over v2 buckets)
				_logger.info("Added buckets=" + added);
			}//(end if new buckets discovered)
			//TESTED (by hand)			
			
			int removed = 0;
			for (Map<String, String> v1s: _v2_mapping.values()) {
				HashSet<String> to_remove = new HashSet<>();
				for (String key: v1s.keySet()) {
					if (!all_keys.contains(key)) {
						to_remove.add(key);
						removed++;
					}
				}
				for (String s: to_remove) {
					v1s.remove(s);
				}
			}
			//TESTED
			if (removed > 0) {
				_logger.info("Removed buckets=" + removed);
			}			
			
			if (on_startup) {
				_logger.info("Imported v2 sources: " + _v2_mapping.toString());
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
	
	/** Returns the base index name (before any date strings, splits etc) have been appended
	 *  Generated by taking 1-3 directories from the path and then appening the end of a UUID
	 * @param bucket
	 * @return
	 */
	public static String getBaseIndexName(final String path) {
		
		String[] components = path.substring(1).split("[/]");
		if (1 == components.length) {
			return tidyUpIndexName(components[0]) + generateUuidSuffix(path);
		}
		else if (2 == components.length) {
			return tidyUpIndexName(components[0] + "_" + components[1]) + generateUuidSuffix(path);
		}
		else { // take the first and the last 2
			final int n = components.length;
			return tidyUpIndexName(components[0] + "_" + components[n-2] + "_" + components[n-1]) + generateUuidSuffix(path);
		}
	}
	// Utils for getBaseIndexName
	private static String tidyUpIndexName(final String in) {
		return in.toLowerCase().replaceAll("[^a-z0-9_-]", "_").replaceAll("__+", "_");
	}
	private static String generateUuidSuffix(final String in) {
		return "__" + java.util.UUID.nameUUIDFromBytes(in.getBytes()).toString().substring(24);
	}
	
}
