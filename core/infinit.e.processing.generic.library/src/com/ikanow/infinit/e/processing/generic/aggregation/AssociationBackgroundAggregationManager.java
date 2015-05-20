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

import java.net.InetAddress;
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
import com.ikanow.infinit.e.data_model.store.feature.association.AssociationFeaturePojo;
import com.ikanow.infinit.e.processing.generic.utils.PropertiesManager;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.WriteConcern;

//TODO: (XXX): can't currently port this over to use MongoApplicationLock because currently only one per database

public class AssociationBackgroundAggregationManager implements Runnable {

	private static final Logger logger = Logger.getLogger(AssociationBackgroundAggregationManager.class);
	
	private static AssociationBackgroundAggregationManager _instance = null;
	private static Thread _instanceThread;
	private static boolean _bKillMe = false;
	private static final Object _lockObj = new Object();
	
	public static void startThread() {
		if (null == _instance) {
			_instance = new AssociationBackgroundAggregationManager(); // (auto starts the thread)
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
	
	public AssociationBackgroundAggregationManager() {
		PropertiesManager pm = new PropertiesManager();
		_dutyCycle_ratio = pm.getHarvestAggregationDutyCycle();
	}//TESTED
	
	// Have I been configured?
	public boolean isEnabled() {
		return _dutyCycle_ratio > 0.0;
	}//TESTED
	
	@Override
	public void run() {
		
		long nAssocsUpdated = 0;
		long nDocsUpdated = 0;
		long nTimeSpentUpdating_ms = 0;

		// See below for derivation of this:
		double sleepCycle = (1.0 - _dutyCycle_ratio)/_dutyCycle_ratio; // (>0 or won't run)
		if (sleepCycle < 0) {
			sleepCycle = 0.0;
		}
		
		boolean bControl = false;
		while (!_bKillMe) {
			bControl = getToken();
			if (_bKillMe) {
				break;
			}
			if (!bControl) { // Shouldn't ever happen, just sleep and then try again
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
				BasicDBObject onlyPrioritized = new BasicDBObject(AssociationFeaturePojo.db_sync_prio_, new BasicDBObject(DbManager.gt_, 0));
				BasicDBObject orderBy = new  BasicDBObject(AssociationFeaturePojo.db_sync_prio_, -1);
				DBCursor dbc = MongoDbManager.getFeature().getAssociation().find(onlyPrioritized).sort(orderBy).limit(100);
					// (feature has a sparse index, so this automatically filters out non-"prio" associations)
	
				// Get the top offenders:
				
				List<AssociationFeaturePojo> assocToSync = AssociationFeaturePojo.listFromDb(dbc, AssociationFeaturePojo.listType());
				
				logger.debug("Found " + assocToSync.size() + " assoc(s) to sync");
				
				//Loop over them in 20s blocks, calling the update/syncs
				
				if (assocToSync.isEmpty()) { // No assocs to sync
	
					if (new Date().getTime() - nStartTime_ms > 20000) {
						logger.debug("(no assocs, running for 20s: re-checking control)");
						break;
					}
					else {
						try {
							Thread.sleep(1000); // wait a second then look again
						} catch (InterruptedException e) {}
						continue;
					}//TESTED
				}
				for (AssociationFeaturePojo assoc: assocToSync) {					
					logger.debug("Association=" + assoc.getIndex() + " prio=" + assoc.getDb_sync_prio());
					
					long nIndividualTime_ms = new Date().getTime(); // for stats

					// Update assocs:
					ObjectId communityId = assoc.getCommunityId();
					AssociationAggregationUtils.synchronizeEventFeature(assoc, communityId);
						// (this also removes db_sync_prio - and corrupts "assoc")
	
					nEndTime_ms = new Date().getTime();
					long nTimeTaken_ms = nEndTime_ms - nIndividualTime_ms;
										
					// Update stats:
					nTimeSpentUpdating_ms += nTimeTaken_ms;
					nAssocsUpdated++;
					nDocsUpdated += assoc.getDoccount();
					
					//After each assoc, sleep for some user-specified %
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
						logger.debug("(Completed 20s block of assoc updates #1)");
						break;
					}//TESTED
					
				} // end loop over assocs to sync
				
				if (_bKillMe || (nEndTime_ms - nStartTime_ms > 20000)) {
					logger.debug("(Completed 20s block of assoc updates #2)");
					break;
				}//TESTED
				
			} // end check assocs every second in 20s blocks 			
		} // end while harvester is running
		
		if (bControl) { // relinquish control for politeness
			MongoDbManager.getFeature().getAggregationLock().remove(new BasicDBObject(), WriteConcern.ACKNOWLEDGED);
			logger.debug("Giving up control before exiting");

		}//TESTED
		
		//Display some stats if anything has been updated?
		if (nAssocsUpdated > 0) {
			logger.info("Background aggregation thread complete, assoc=" + nAssocsUpdated + " docs=" + nDocsUpdated + " dbtime=" + nTimeSpentUpdating_ms + " stoleToken=" + _nGrabbedControl + " lostToken=" + _nLostControl);
		}//TESTED		
		
	}//TESTED
	
	///////////////////////////////////////////////////////////////////////////
	
	// Synchronization utility between multiple harvesters

	private int _nGrabbedControl = 0;
	private int _nLostControl = 0;
	
	private boolean getToken() {

		boolean bHaveControl = false;
		
		final String id_ = "_id";
		final String hostname_ = "hostname";
		final String oneUp_ = "1up";
		
		String savedHostname = "";
		String savedOneUp = "";
		
		while (!_bKillMe) {
			// Get IP address:
			BasicDBObject query = new BasicDBObject(id_, _assocLockId);
			BasicDBObject lockObj = (BasicDBObject) MongoDbManager.getFeature().getAggregationLock().findOne(query);
			if (null == lockObj) {
				lockObj = new BasicDBObject(id_, _assocLockId);
				lockObj.put(hostname_, getHostname());
				lockObj.put(oneUp_, Long.toString(1000000L*(new Date().getTime() % 10000))); 
					// (ie a randomish start number)
				
				MongoDbManager.getFeature().getAggregationLock().insert(lockObj);
					// (will fail if another harvester gets there first)
				
				logger.debug("Creating a new aggregation lock object: " + lockObj.toString());
				
				lockObj = (BasicDBObject) MongoDbManager.getFeature().getAggregationLock().findOne();
			}//TESTED
			
			// So by here lockObj is always non-null
			
			// Do I have control?
			String hostname = lockObj.getString(hostname_);
			String oneUp = lockObj.getString(oneUp_);
			
			bHaveControl = getHostname().equals(hostname);
			
			if (!bHaveControl) { // Don't currently have control				
				if (savedHostname.equals(hostname) && savedOneUp.equals(oneUp)) { // Now I have control...
					logger.debug("I am taking control from: " + hostname + ", " + oneUp);				
					bHaveControl = true;
					_nGrabbedControl++;
				}
				else if (getHostname().equals(savedHostname)) { // I had control of this last time I checked
					logger.debug("Lost control to: " + hostname);				
					_nLostControl++;
				}
			}
			else {
				logger.debug("I have control already: " + hostname);				
			}//TESTED
			
			if (bHaveControl) {
				savedHostname = hostname;
				long nOneUp = Long.parseLong(oneUp);
				lockObj.put(hostname_, getHostname());
				lockObj.put(oneUp_, Long.toString(nOneUp + 1));
				MongoDbManager.getFeature().getAggregationLock().save(lockObj, WriteConcern.ACKNOWLEDGED);
				return true;
			}//TESTED
			else { // Save info and sleep for 60s 
				savedHostname = hostname;
				savedOneUp = oneUp;
				logger.debug("Saving state and sleeping: " + savedHostname + ", " + savedOneUp);
				
				for (int i = 0; (i < 6) && !_bKillMe; ++i) {
					try {
						Thread.sleep(10000);
					} catch (InterruptedException e) {}
				}
			}//TESTED
			
		} // end loop forever 
		
		return bHaveControl;
	}//TESTED
	
	///////////////////////////////////////////////////////////////////////////
	
	// Utility to get harvest name for display purposes
	
	private static ObjectId _assocLockId = new ObjectId("4f976e98d4eefff2ed6963dd");
	
	private static String _hostname = null;
	private static String getHostname() {
		// (just get the hostname once)
		if (null == _hostname) {
			try {
				_hostname = InetAddress.getLocalHost().getHostName();
			} catch (Exception e) {
				_hostname = "UNKNOWN";
			}
			if (null != _hostnameSuffix) {
				_hostname = _hostname + _hostnameSuffix;
			}
		}		
		return _hostname;
	}//TESTED	
	
	// For testing purposes:
	public static void setHostnameSuffix(String hostnameSuffix) {
		_hostnameSuffix = hostnameSuffix;
	}
	private static String _hostnameSuffix = null;
	
	///////////////////////////////////////////////////////////////////////////
	
	// TEST CODE
	
	public static void main(String[] args) {
		System.out.println(Arrays.toString(args));
		Globals.setIdentity(Identity.IDENTITY_SERVICE);
		Globals.overrideConfigLocation(args[0]);
		PropertyConfigurator.configure(Globals.getLogPropertiesLocation());
		
		AssociationBackgroundAggregationManager.setHostnameSuffix(args[1]); // (so i can test on the same host)
		
		AssociationBackgroundAggregationManager.startThread();
		AssociationBackgroundAggregationManager.startThread(); // (just check it only launched once)
		
		logger.debug("TEST: main thread goes to sleep...");
		for (int i = 0; i < 20; i++) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {}
		}
		logger.debug("TEST: Request background thread stop...");
		AssociationBackgroundAggregationManager.stopThreadAndWait();
		logger.debug("TEST: main thread exiting...");
	}	
}
