/*******************************************************************************
 * Copyright 2012 The Infinit.e Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.ikanow.infinit.e.data_model.utils;

import java.net.InetAddress;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.bson.types.ObjectId;

import com.ikanow.infinit.e.data_model.store.MongoDbManager;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;

//
// Use this to protect from race conditions between threads/processes/nodes during multiple short
// transactions (eg in tomcat handles)
//
// If you want to ensure that only one node is performing an action (eg polling a DB) 
// then use MongoApplicationLock instead (see examples)
//

public class MongoTransactionLock {

	///////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////
	
	// INTERFACE
	
	public static synchronized MongoTransactionLock getLock(String database)
	{
		if (null == _lockMap) {
			_lockMap = new HashMap<String, MongoTransactionLock>();			
		}
		MongoTransactionLock lock = _lockMap.get(database);
		if (null == lock) {
			lock = new MongoTransactionLock(database);
			_lockMap.put(database, lock);
		}
		return lock;
	}//TESTED
	
	///////////////////////////////////////////////////////////////////////////
	
	public boolean acquire(long nTimeout_ms)
	{
		return acquire(nTimeout_ms, false);
	}
	
	public boolean acquire(long nTimeout_ms, boolean bTryToAcquireAfterTimeoutIfRemote)
	{
		try {
			long nNow = new Date().getTime();
			boolean bAcquire = _localLock.tryAcquire(1, nTimeout_ms, TimeUnit.MILLISECONDS);
			
			if (bAcquire) {			
				while (!getToken()) {
					Thread.sleep(50);
					if ((new Date().getTime() - nNow) > nTimeout_ms) {
						bAcquire = false;
						if (bTryToAcquireAfterTimeoutIfRemote) {
							bAcquire = updateToken(true);
						}
						break;
					}//TESTED
					
				} // (end while don't have remote control)
				
				if (!bAcquire) { // Got local mutex but not remote one so bail on local)
					_localLock.release();
				}
				
			} // (bAcquire if got local mutex) 
			return bAcquire; 
		}
		catch (InterruptedException e) {
			return false;
		}
	}//TESTED
	
	///////////////////////////////////////////////////////////////////////////

	public void release() {		
		_localLock.release();
		removeToken();
	}//TESTED
	
	///////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////

	// TOP LEVEL STATE
	
	public static final String LOCKNAME = "appserver_translock";
	
	protected static HashMap<String, MongoTransactionLock> _lockMap = null; 
	
	protected String _database = null;
	protected String _lockname = null;
		
	protected MongoTransactionLock() {}
	protected MongoTransactionLock(String database) {
		_database = database;
		_lockname = LOCKNAME;
		_localLock = new Semaphore(1);
	}
	protected Semaphore _localLock;
	
	// Caches an instance of the collection object for each thread accessing this object
	protected ThreadLocal<DBCollection> _collections  = new ThreadLocal<DBCollection>() {
        @Override protected DBCollection initialValue() {
            try {
				return MongoDbManager.getCollection(_database, _lockname);
			} catch (Exception e) {
				return null;
			}
        }		
	};	
	
	///////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////
	
	// LOW LEVEL / SHARED STATE
	
	// Synchronization utility between multiple machines

	private String _savedHostname = "";
	private String _savedOneUp = "";
	private boolean _bHaveControl = false;
	private long _nLastCheck = 0;
	
	private final String id_ = "_id";
	private final String hostname_ = "hostname";
	private final String oneUp_ = "1up";
	
	protected synchronized boolean getToken() {
		DBCollection cachedCollection = _collections.get();
		
		boolean bDefinitelyHaveControl = false;
		String hostname = null;
		String oneUp = null;
		
		// 1] Get Lock Object (create one an assert control if it doesn't exist)
		
		BasicDBObject lockObj = (BasicDBObject) cachedCollection.findOne();
		if (null == lockObj) { // Currently the DB is unlocked
			hostname = getHostname();
			oneUp = Long.toString(1000000L*(new Date().getTime() % 10000));
				// (ie a randomish start number)
			
			lockObj = new BasicDBObject(id_, _lockId);
			lockObj.put(hostname_, hostname);
			lockObj.put(oneUp_, oneUp); 
			
			//logger.debug("Creating a new aggregation lock object: " + lockObj.toString());
			
			try {
				cachedCollection.insert(lockObj, WriteConcern.SAFE);
					// (will fail if another harvester gets there first)
				bDefinitelyHaveControl = true;
			}
			catch (Exception e) { // Someone else has created it in the meantime
				lockObj = (BasicDBObject) cachedCollection.findOne();				
			}
			
		}//TESTED
		
		// (So by here lockObj is always non-null)
		
		// 2] Do I have control?
		
		if (bDefinitelyHaveControl) {
			_bHaveControl = true;
			_nLastCheck = 0;
		}
		else {
			hostname = lockObj.getString(hostname_);
			oneUp = lockObj.getString(oneUp_);			
			_bHaveControl = getHostname().equals(hostname);
		}
		// 3] If not, has the lock object been static for >= 1 minute
		
		if (!_bHaveControl) { // Don't currently have control
			long nNow = new Date().getTime();
			if (0 == _nLastCheck) {
				_nLastCheck = nNow;				
			}
			
			if ((nNow - _nLastCheck) > 60000) { // re-check every minute
				if (_savedHostname.equals(hostname) && _savedOneUp.equals(oneUp)) { // Now I have control...
					//logger.debug("I am taking control from: " + hostname + ", " + oneUp);
					
					if (updateToken(true)) { // Try to grab control:						
						_bHaveControl = true;	
					}
					else { // (else someone else snagged control just carry on)
						_nLastCheck = 0; // (reset clock again anyway)
					}
					

				}//(if lock has remained static)
			}//(end if >=1 minutes has passed)
			
		}//(end if have don't have control)

		// 4] Update saved state 
		
		_savedHostname = hostname;
		_savedOneUp = oneUp;
		
		return _bHaveControl;
	}//TESTED

	///////////////////////////////////////////////////////////////////////////
	
	protected synchronized void removeToken() {
		if (_bHaveControl) {			
			DBCollection cachedCollection = _collections.get();
			
			BasicDBObject queryObj = new BasicDBObject();
			queryObj.put(hostname_, getHostname());
				// (ie will only remove a lock I hold)
			cachedCollection.remove(queryObj);
			_bHaveControl = false;
		}		
	}//TESTED
	
	///////////////////////////////////////////////////////////////////////////
	
	protected synchronized boolean updateToken(boolean bForce) {
		if (_bHaveControl || bForce) {
			DBCollection cachedCollection = _collections.get();
			BasicDBObject lockObj = new BasicDBObject();

			long nOneUp = Long.parseLong(_savedOneUp);
			lockObj.put(hostname_, getHostname());
			String newOneUp = Long.toString(nOneUp + 1);
			lockObj.put(oneUp_, newOneUp);
			BasicDBObject queryObj = new BasicDBObject();
			queryObj.put(hostname_, _savedHostname);
			queryObj.put(oneUp_, _savedOneUp);
			WriteResult wr = cachedCollection.update(queryObj, new BasicDBObject(MongoDbManager.set_, lockObj), false, true);
				// (need the true in case the db is sharded)
			
			if (wr.getN() > 0) {
				_savedOneUp = newOneUp;
				_bHaveControl = true;
				_nLastCheck = 0;
				return true;
			}
			else {
				return false;
			}
		}
		return false;
	}//TESTED
	
	///////////////////////////////////////////////////////////////////////////
	
	// Utility to get harvest name for display purposes
	
	private static ObjectId _lockId = new ObjectId("4f976e98d4eefff2ed6963dc");
	
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
	
}
