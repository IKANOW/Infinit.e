/*******************************************************************************
 * Copyright 2012, The Infinit.e Open Source Project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.ikanow.infinit.e.harvest.extraction.document.v2databucket;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.bson.types.ObjectId;

import com.ikanow.infinit.e.data_model.InfiniteEnums;
import com.ikanow.infinit.e.data_model.InfiniteEnums.HarvestEnum;
import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.infinit.e.harvest.HarvestContext;
import com.ikanow.infinit.e.harvest.extraction.document.HarvesterInterface;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;

public class V2DataBucketHarvester implements HarvesterInterface {
	
	@Override
	public boolean canHarvestType(int sourceType) {
		return sourceType == InfiniteEnums.V2DATABUCKET;
	}

	@Override
	public void executeHarvest(HarvestContext context, SourcePojo source,
			List<DocumentPojo> toAdd, List<DocumentPojo> toUpdate,
			List<DocumentPojo> toRemove) {		
		
		if (context.isStandalone()) { // test mode		
			String requestId = new ObjectId().toString();
			BasicDBObject v2Q = new BasicDBObject("_id", requestId);
			
			@SuppressWarnings("unchecked")
			Optional<Map<String, Object>> test_options = 
					Optional.ofNullable(source.getProcessingPipeline()).orElse(Collections.emptyList())
											.stream().map(m -> m.data_bucket).filter(m -> null != m).findFirst()
						.map(m -> m.get("test_params"))
						.filter(m -> m instanceof Map) // (also checks for null)
						.map(m -> (Map<String, Object>) m)
					;
			long max_startup_time = 120; //amount of time we wait for test to startup
			long max_run_time = 300; //amount of time we wait for test to run
			try {
				BasicDBObject test_params = new BasicDBObject();
				// Mandatory:
				test_params.put("requested_num_objects", test_options.map(m -> m.get("requested_num_objects")).orElse(new Long(context.getStandaloneMaxDocs())));
				max_run_time = Math.round(test_options.map(m-> (Double)m.get("max_run_time_secs")).orElse(300.0));
				max_startup_time = Math.round(test_options.map(m-> (Double)m.get("max_startup_time_secs")).orElse(120.0));
				test_params.put("max_startup_time_secs", max_startup_time); //5 minutes
				test_params.put("max_run_time_secs", max_run_time); //5 minutes
				max_run_time = (long) (max_run_time * 1.25); //increase how long we wait by 25%
				// Optional:
				test_options.map(m -> m.get("overwrite_existing_data")).ifPresent(b -> test_params.put("overwrite_existing_data", b));
				test_options.map(m -> m.get("max_storage_time_secs")).ifPresent(n -> test_params.put("max_storage_time_secs", n));
				
				BasicDBObject v2Dbo = new BasicDBObject();
				v2Dbo.put("_id", requestId);
				v2Dbo.put("source", source.toDb());
				v2Dbo.put("test_params", test_params);
				v2Dbo.put("status", "submitted");
	
				// Step 0: place request on Q
				DbManager.getIngest().getV2DataBucketHarvesterQ().save(v2Dbo);
				
				// Step 1: has my request been serviced:
				boolean serviced = false;
				String error = null;
				//long end_time = System.currentTimeMillis() + 480 * 1000; //now+8min
				long end_time = System.currentTimeMillis() + max_startup_time * 1000; //now+2min
				//wait 2 min for startup time
				while ( System.currentTimeMillis() < end_time ) {
					try
					{
						Thread.sleep(5000);
						v2Dbo = (BasicDBObject) DbManager.getIngest().getV2DataBucketHarvesterQ().findOne(v2Q);
						if ( v2Dbo.containsField("status")) {
							if (  v2Dbo.getString("status").equals("error")) {
								error = "error when trying to start up test job: " + v2Dbo.getString("message", "no error message returned");
								break;
							}
							else if ( v2Dbo.containsField("started_processing_on") &&
									v2Dbo.get("started_processing_on") != null ) {
									//started_processing_on is set, job is done setting up
								serviced = true;
								break;
								}
						}
					} catch (InterruptedException e){}					
				}	
						
				//if I wasn't serviced, remove the q entry and output the error (if one was returned)
				if (!serviced) {					
					if (null == error) {
						context.getHarvestStatus().update(source,new Date(),HarvestEnum.error, "V2DataBucket service timed out (is it running, took too long to startup?): " + v2Dbo.getString("message", "no error message returned"), true, false);
					}
					else {
						context.getHarvestStatus().update(source,new Date(),HarvestEnum.error, "V2DataBucket service reports error: " + error, true, false);					
					}
					return;
				}			
				
				// Step 2: get data from the queue
				boolean finished = false;
				end_time = System.currentTimeMillis() + max_run_time * 1000; //now+2min
				while ( System.currentTimeMillis() < end_time ) {					
					v2Dbo = (BasicDBObject) DbManager.getIngest().getV2DataBucketHarvesterQ().findOne(v2Q);
					//wait for entry to be completed/errored
					if ( null == v2Dbo || v2Dbo.getString("status").equals("error")) {
						finished = true;
						error = "Had an error while harvesting: " + v2Dbo.getString("message", "no error message returned");						
						context.getHarvestStatus().update(source,new Date(),HarvestEnum.error, "V2DataBucket service reports error: " + error, true, false);
						break;
					} else if ( v2Dbo.getString("status").equals("completed")) {
						finished = true;
						//done copy over results
						String output_coll = v2Dbo.getString("result");
						long count = DbManager.getCollection("ingest", output_coll).count();
						if (count > 0) {
							DBCursor dbc = DbManager.getCollection("ingest", output_coll).find().limit(context.getStandaloneMaxDocs());
							for (Object o: dbc) {
								DocumentPojo doc = new DocumentPojo();
								doc.addToMetadata("record", o);
								toAdd.add(doc);
							}
						}
						error = v2Dbo.getString("message", "no info");
						context.getHarvestStatus().update(source,new Date(),HarvestEnum.success, "V2 service info: " + error, false, false);
						// Tidy up:
						try {
							DbManager.getCollection("ingest", requestId).drop();
						}
						catch (Exception e) {} // that's fine it just doesn't exist
						try {
							DbManager.getCollection("ingest", output_coll).drop();
						}
						catch (Exception e) {} // that's fine it just doesn't exist
							
						break;
					}
					try {
						Thread.sleep(5000); // check every 5s
					} catch (Exception e) {}
					
				} // (end loop while waiting for docs)
				
				//return any messages if we timed out while waiting for test to finish
				if ( !finished ) {
					context.getHarvestStatus().update(source,new Date(),HarvestEnum.error, "V2DataBucket service timed out before job finished (taking too long): " + 
							v2Dbo.getString("message", "no error message returned"), true, false);
				}
			}
			finally { // just to be on the safe side...
				//always try to remove the job, no harm
				DbManager.getIngest().getV2DataBucketHarvesterQ().remove(v2Q);
			}
		}
		else {
			context.getHarvestStatus().update(source,new Date(),HarvestEnum.error, "Tried to harvest v2 data bucket internally", true, false);
			return;
		}
	}//TESTED

}
