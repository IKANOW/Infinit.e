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

import java.util.Date;
import java.util.HashMap;

//
// Use this to ensure that only one node is performing an action (eg polling a DB)
// Multiple processes/threads on this node all get the mutex, only other nodes are blocked
//
// If you want to protect from race conditions between threads/processes/nodes during multiple short
// transactions (eg in tomcat handles) then use MongoTransactionLock instead (see examples)
//

public class MongoApplicationLock extends MongoTransactionLock implements Runnable  {

	///////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////
	
	// INTERFACE
	
	public static synchronized MongoApplicationLock getLock(String database) {
		return getLock(database, false);
	}//TESTED
	
	public static synchronized MongoApplicationLock getLock(String database, boolean bReleasable)
	{
		if (null == _lockMap) {
			_lockMap = new HashMap<String, MongoApplicationLock>();	
			
			// Very first time through .. create a shutdown hook
			Runtime.getRuntime().addShutdownHook(new Thread() {
			    @Override
			    public void run() {
			    	_bAppClosingDown = true;
			    }

			});			
		}
		MongoApplicationLock lock = _lockMap.get(database);
		if (null == lock) {
			lock = new MongoApplicationLock(database);
			_lockMap.put(database, lock);
		}
		lock._bReleasable = bReleasable;
		return lock;
	}//TESTED
	
	///////////////////////////////////////////
	
	// Interface
	
	@Override
	public boolean acquire(long nTimeout_ms, boolean bTryToAcquireAfterTimeoutIfRemote) {
		synchronized (synch_object) {
			throw new RuntimeException("Option not supported");
		}
	}
	
	@Override
	public boolean acquire(long nTimeout_ms) {
		synchronized (synch_object) {
			if (null == _thread) {
				_thread = new Thread(this);
				_thread.start();
					
				//logger.debug("Starting thread: " + _instanceThread.getName());
			}
			long nThen = new Date().getTime();
			while (!getToken()) {
				try {
					Thread.sleep(100);
				} catch (Exception e) {}
				
				long nNow = new Date().getTime();
				if ((nNow - nThen) > nTimeout_ms) {
					return false;
				}
			}		
			return true;
		}		
	}//TESTED
	
	@Override
	public void release() {
		synchronized (synch_object) {
			if (!_bReleasable) {
				return;
			}
			if (null != _thread) {
				_bKillMe = true;
	
				while (_thread.isAlive()) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {}
				}
				removeToken();
				_bKillMe = false; // (in case we want to acquire it again)
			}
		}
	}//TOTEST
	
	///////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////

	// TOP LEVEL STATE
	
	public static final String LOCKNAME = "appserver_applock";
	
	protected static HashMap<String, MongoApplicationLock> _lockMap = null;
	
	protected Thread _thread = null;
	
	protected MongoApplicationLock(String database) {
		super(); // (does nothing, don't call super(database) because it also sets an unused semaphore)
		_database = database;
		_lockname = LOCKNAME;
	}
	private boolean _bKillMe = false;
	private boolean _bReleasable = false;
	private static boolean _bAppClosingDown = false;
	public static void registerAppShutdown() {
		_bAppClosingDown = true;	
	}
	private Object synch_object = new Object(); // (can't use synchronized in the method definitions because of race conditions vs its super class MongoTransactionLock)
	
	///////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////

	// LOW LEVEL STATE
	
	@Override
	public void run() { // This thread just keeps control once it's been obtained
		
		while (!_bKillMe && !_bAppClosingDown) {
			updateToken(false);
			if (_bKillMe || _bAppClosingDown) {
				break;
			}
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {}
			
			continue;
		}		
	}//TESTED
}
