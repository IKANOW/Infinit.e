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
package com.ikanow.infinit.e.harvest.extraction.document.logstash;

import java.util.Date;
import java.util.List;

import org.bson.types.ObjectId;

import com.ikanow.infinit.e.data_model.InfiniteEnums;
import com.ikanow.infinit.e.data_model.InfiniteEnums.HarvestEnum;
import com.ikanow.infinit.e.data_model.index.ElasticSearchManager;
import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePipelinePojo;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.infinit.e.harvest.HarvestContext;
import com.ikanow.infinit.e.harvest.extraction.document.HarvesterInterface;
import com.ikanow.infinit.e.harvest.utils.AuthUtils;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;

public class LogstashHarvester implements HarvesterInterface {

	@Override
	public boolean canHarvestType(int sourceType) {
		return sourceType == InfiniteEnums.LOGSTASH;
	}

	@Override
	public void executeHarvest(HarvestContext context, SourcePojo source,
			List<DocumentPojo> toAdd, List<DocumentPojo> toUpdate,
			List<DocumentPojo> toRemove) {
		
		if (ElasticSearchManager.getVersion() < 100) {
			context.getHarvestStatus().update(source,new Date(),HarvestEnum.error, "This version of infinit.e (elasticsearch version < 1.0) does not support logstash, you will need to upgrade to v0.3 and ensure your elasticsearch instance is upgraded.", true, false);
			return;			
		}
		
		if (context.isStandalone()) { // test mode
			
			// Get the configuration
			
			String logStashConfig = null;
			SourcePipelinePojo logstashElement = null;
			for (SourcePipelinePojo pxPipe: source.getProcessingPipeline()) { /// (must be non null if here)
				if (null != pxPipe.logstash) {
					logStashConfig = pxPipe.logstash.config;
					logstashElement = pxPipe;
				}
				break;
			}
			if ((null == logStashConfig) || logStashConfig.isEmpty()) {
				context.getHarvestStatus().update(source,new Date(),HarvestEnum.error, "Logstash internal logic error, no configuration", true, false);
				return;				
			}

			// Admin check (passed down)
			
			boolean isAdmin = AuthUtils.isAdmin(source.getOwnerId());

			// Perform the request
			
			ObjectId requestId = new ObjectId();
			BasicDBObject logQ = new BasicDBObject("_id", requestId);
			boolean removeJobWhenDone = true;
			
			try {

				// (See records.service for the programmatic definition of this message)
				logstashElement.logstash.config = logStashConfig;
				BasicDBObject logStashDbo = (BasicDBObject) logstashElement.toDb();
				logStashDbo.put("_id", requestId);
				logStashDbo.put("maxDocs", context.getStandaloneMaxDocs());
				logStashDbo.put("sourceKey", source.getKey());
				logStashDbo.put("isAdmin", isAdmin);
	
				// Step 0: place request on Q
				DbManager.getIngest().getLogHarvesterQ().save(logStashDbo);
				
				// Step 1: has my request been serviced:
				boolean serviced = false;
				String error = null;
	
				
				final int WAIT_TIME_2_MINS = 120;
				for (int time = 0; time < WAIT_TIME_2_MINS; time += 5) { // (allow 2 minutes for servicing)
					//1. have i been removed from queue?
					//2. check size of logstash queue - is it decreasing
					try {
						Thread.sleep(5000); // check every 5s
						logStashDbo = (BasicDBObject) DbManager.getIngest().getLogHarvesterQ().findOne(logQ);
						if (null == logStashDbo) {
							removeJobWhenDone = false;
							serviced = true;
							break; // found!
						}//TESTED
						error = logStashDbo.getString("error", null);
						if (null != error) {
							break; // bad!
						}//TESTED
					}
					catch (Exception e) {}
				}
				if (!serviced) {
					DbManager.getIngest().getLogHarvesterQ().remove(logQ);
					removeJobWhenDone = false;
					
					if (null == error) {
						context.getHarvestStatus().update(source,new Date(),HarvestEnum.error, "Logstash service appears not to be running", true, false);
					}
					else {
						context.getHarvestStatus().update(source,new Date(),HarvestEnum.error, "Logstash service reports error: " + error, true, false);					
					}//TESTED
					return;
				}//TESTED
				
				// Step 2: get data from the queue
				final int WAIT_TIME_5_MINS = 300;
				for (int time = 0; time < WAIT_TIME_5_MINS; time += 5) { // (allow 5 minutes for processing)
	
					logStashDbo = (BasicDBObject) DbManager.getIngest().getLogHarvesterQ().findOne(logQ);
					if (null != logStashDbo) { // if it reappears then there's been an error so handle and exit
						DbManager.getIngest().getLogHarvesterQ().remove(logQ);
						removeJobWhenDone = false;
						
						long count = DbManager.getCollection("ingest", requestId.toString()).count();
						if (count > 0) {
							DBCursor dbc = DbManager.getCollection("ingest", requestId.toString()).find().limit(context.getStandaloneMaxDocs());
							for (Object o: dbc) {
								DocumentPojo doc = new DocumentPojo();
								doc.addToMetadata("logstash_record", o);
								toAdd.add(doc);
							}
							error = logStashDbo.getString("error", "no info");
							context.getHarvestStatus().update(source,new Date(),HarvestEnum.success, "Logstash service info: " + error, false, false);					
							break;							
						}//TESTED
						else { // Then it's an error:
							error = logStashDbo.getString("error", null);
							
							if (error == null) {
								if (0 == context.getStandaloneMaxDocs()) {
									context.getHarvestStatus().update(source,new Date(),HarvestEnum.success, "Logstash service info: success", false, false);					
									break;
								}
								else {
									error = "unknown error";
								}
							}//TESTED
							
							context.getHarvestStatus().update(source,new Date(),HarvestEnum.error, "Logstash service reports error: " + error, true, false);					
							return;							
						}//TESTED
						
					}//TESTED
					try {
						Thread.sleep(5000); // check every 5s
					}
					catch (Exception e) {}
					
				} // (end loop while waiting for docs)
			}
			finally { // just to be on the safe side...
				if (removeJobWhenDone) {
					DbManager.getIngest().getLogHarvesterQ().remove(logQ);
				}
				try {
					DbManager.getCollection("ingest", requestId.toString()).drop();
				}
				catch (Exception e) {} // that's fine it just doesn't exist
			}
		}
		else {
			context.getHarvestStatus().update(source,new Date(),HarvestEnum.error, "Tried to harvest logstash data internally", true, false);
			return;
		}
	}//TESTED

}
