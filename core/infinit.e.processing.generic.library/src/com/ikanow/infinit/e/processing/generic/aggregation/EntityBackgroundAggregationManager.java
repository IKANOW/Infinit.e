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
package com.ikanow.infinit.e.processing.generic.aggregation;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.bson.types.ObjectId;

import com.ikanow.infinit.e.data_model.Globals;
import com.ikanow.infinit.e.data_model.Globals.Identity;
import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.MongoDbManager;
import com.ikanow.infinit.e.data_model.store.feature.entity.EntityFeaturePojo;
import com.ikanow.infinit.e.data_model.utils.MongoApplicationLock;
import com.ikanow.infinit.e.data_model.utils.MongoTransactionLock;
import com.ikanow.infinit.e.processing.generic.utils.PropertiesManager;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;

public class EntityBackgroundAggregationManager implements Runnable {

	private static final Logger logger = Logger.getLogger(EntityBackgroundAggregationManager.class);	
	private static MongoApplicationLock _appLock = null;
	
	private static EntityBackgroundAggregationManager _instance = null;
	private static Thread _instanceThread;
	private static boolean _bKillMe = false;
	private static final Object _lockObj = new Object();
	
	public static void startThread() {
		if (null == _instance) {
			_instance = new EntityBackgroundAggregationManager(); // (auto starts the thread)
			if (_instance.isEnabled()) {
				_instanceThread = new Thread(_instance);
				_instanceThread.start();
				
				logger.debug("Starting thread: " + _instanceThread.getName());
			}
			else {
				logger.debug("Background aggregation manually disabled");				
			}//TESTED
		}
	}//TESTED
	
	public static void stopThreadAndWait() {
		if (null != _instanceThread) {
			synchronized (_lockObj) {
				_bKillMe = true;
				_lockObj.notify();
			}
			while (_instanceThread.isAlive()) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {}
			}
		}
	}//TESTED

	///////////////////////////////////////////////////////////////////////////
	
	private double _dutyCycle_ratio = 0.5;
	
	// C'tor:
	
	public EntityBackgroundAggregationManager() {
		PropertiesManager pm = new PropertiesManager();
		_dutyCycle_ratio = pm.getHarvestAggregationDutyCycle();
	}//TESTED
	
	// Have I been configured?
	public boolean isEnabled() {
		return _dutyCycle_ratio > 0.0;
	}//TESTED
	
	@Override
	public void run() {
		
		long nEntitiesUpdated = 0;
		long nDocsUpdated = 0;
		long nTimeSpentUpdating_ms = 0;

		// See below for derivation of this:
		double sleepCycle = (1.0 - _dutyCycle_ratio)/_dutyCycle_ratio; // (>0 or won't run)
		if (sleepCycle < 0) {
			sleepCycle = 0.0;
		}
		
		if (null == _appLock) { 
			_appLock = MongoApplicationLock.getLock(DbManager.getFeature().getEntity().getDB().getName(), true);
		}
		
		while (!_bKillMe) {
			
			if (!_appLock.acquire(100))  {
				try {
					Thread.sleep(10000);
				} catch (InterruptedException e) {}
				
				continue;
			}//TESTED
			
			long nStartTime_ms = new Date().getTime();
			long nEndTime_ms = nStartTime_ms;
			long nTimeToSleep_ms = 0;
			while (!_bKillMe) {
			
				// Get the offenders, ordered by prio:
				BasicDBObject onlyPrioritized = new BasicDBObject(EntityFeaturePojo.db_sync_prio_, new BasicDBObject(DbManager.gt_, 0));
				BasicDBObject orderBy = new  BasicDBObject(EntityFeaturePojo.db_sync_prio_, -1);
				DBCursor dbc = MongoDbManager.getFeature().getEntity().find(onlyPrioritized).sort(orderBy).limit(100);
					// (feature has a sparse index, so this automatically filters out non-"prio" entities)
	
				// Get the top offenders:
				
				List<EntityFeaturePojo> entitiesToSync = EntityFeaturePojo.listFromDb(dbc, EntityFeaturePojo.listType());
				
				logger.debug("Found " + entitiesToSync.size() + " entit(y|ies) to sync");
				
				//Loop over them in 20s blocks, calling the update/syncs
				
				if (entitiesToSync.isEmpty()) { // No entities to sync
	
					if (new Date().getTime() - nStartTime_ms > 20000) {
						logger.debug("(no entities, running for 20s: re-checking control)");
						break;
					}
					else {
						try {
							Thread.sleep(1000); // wait a second then look again
						} catch (InterruptedException e) {}
						continue;
					}//TESTED
				}
				for (EntityFeaturePojo entity: entitiesToSync) {					
					logger.debug("Entity=" + entity.getIndex() + " prio=" + entity.getDb_sync_prio());
					
					long nIndividualTime_ms = new Date().getTime(); // for stats

					// Update entities:
					// (duplicate a few entity fields so I can _first_ sync the entity, which corrupts it)
					EntityFeaturePojo docEnt = new EntityFeaturePojo();
					docEnt.setIndex(entity.getIndex());
					docEnt.setDoccount(entity.getDoccount());
					docEnt.setTotalfreq(entity.getTotalfreq());
					ObjectId communityId = entity.getCommunityId();
					EntityAggregationUtils.synchronizeEntityFeature(entity, communityId);
						// (this also removes db_sync_prio - and corrupts "entity", hence use "docEnt" below)
					EntityAggregationUtils.updateMatchingEntities(docEnt, communityId);
						// (some of the time I'll lose my lock in here - this is checked below)					
	
					nEndTime_ms = new Date().getTime();
					long nTimeTaken_ms = nEndTime_ms - nIndividualTime_ms;
										
					// Update stats:
					nTimeSpentUpdating_ms += nTimeTaken_ms;
					nEntitiesUpdated++;
					nDocsUpdated += entity.getDoccount();
					
					//After each entity, sleep for some user-specified %
					// OK I just spent time X @ duty cycle d, so to keep up my contract, I need for total time T
					// T*d = X, ie T=X/d, ie I now need to sleep for T-X=X-X/d=(1-d)X/d
					long nDutyCycle_ms = (long)(sleepCycle*nTimeTaken_ms);
					if (nDutyCycle_ms > 20) {
						nTimeToSleep_ms +=  nDutyCycle_ms - 20; // (allow 20ms for comms)
					}
					else {
						nTimeToSleep_ms +=  1; // (just so we always sleep occasionally)
					}
					
					// Some max time for sanity (20s):
					if (nTimeToSleep_ms > 20000) {
						nTimeToSleep_ms = 20000;
					}
					if (nTimeToSleep_ms > 0) {
						logger.debug("Need to sleep for: " + nTimeToSleep_ms + " after working for: " + nTimeTaken_ms);
					}
					if (nTimeToSleep_ms > 100) {
						logger.debug("Actually sleep for: " + nTimeToSleep_ms);
						synchronized (_lockObj) {
							if (_bKillMe) { // (check before sleeping)
								break;
							}				
							try {
								_lockObj.wait(nTimeToSleep_ms);
							} catch (InterruptedException e) {}
						}//TESTED (including exit while in long sleep, immediately wakes up)
						
						nEndTime_ms += nTimeToSleep_ms;
						nTimeToSleep_ms = 0;
					}
					
					// Check if 20s block is up:
					if (_bKillMe || (nEndTime_ms - nStartTime_ms > 20000)) {
						logger.debug("(Completed 20s block of entity updates #1)");
						break;
					}//TESTED
					
				} // end loop over entities to sync
				
				if (_bKillMe || (nEndTime_ms - nStartTime_ms > 20000)) {
					logger.debug("(Completed 20s block of entity updates #2)");
					break;
				}//TESTED
				
			} // end check entities every second in 20s blocks 			
			
			
			if (_bKillMe) {
				_appLock.release();
				break;
			}					
		} // end while harvester is running
		
		//Display some stats if anything has been updated?
		if (nEntitiesUpdated > 0) {
			logger.info("Background aggregation thread complete, ents=" + nEntitiesUpdated + " docs=" + nDocsUpdated + " dbtime=" + nTimeSpentUpdating_ms);
		}//TESTED		
		
	}//TESTED
	
	///////////////////////////////////////////////////////////////////////////
	
	// TEST CODE
	
	public static void main(String[] args) {
		System.out.println(Arrays.toString(args));
		Globals.setIdentity(Identity.IDENTITY_SERVICE);
		Globals.overrideConfigLocation(args[0]);
		PropertyConfigurator.configure(Globals.getLogPropertiesLocation());
		
		MongoTransactionLock.setHostnameSuffix(args[1]); // (so i can test on the same host)
		
		EntityBackgroundAggregationManager.startThread();
		EntityBackgroundAggregationManager.startThread(); // (just check it only launched once)
		
		logger.debug("TEST: main thread goes to sleep...");
		for (int i = 0; i < 20; i++) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {}
		}
		logger.debug("TEST: Request background thread stop...");
		EntityBackgroundAggregationManager.stopThreadAndWait();
		logger.debug("TEST: main thread exiting...");
		
		MongoApplicationLock.registerAppShutdown(); 
	}	
}
