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
package com.ikanow.infinit.e.api.config.source;

import org.apache.log4j.Logger;
import org.bson.types.ObjectId;

import com.ikanow.infinit.e.data_model.Globals;
import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.ikanow.infinit.e.data_model.store.document.DocCountPojo;
import com.ikanow.infinit.e.data_model.utils.MongoQueue;
import com.ikanow.infinit.e.processing.generic.aggregation.AggregationManager;
import com.ikanow.infinit.e.processing.generic.store_and_index.StoreAndIndexManager;
import com.mongodb.BasicDBObject;

public class SourceDeletionHandlerBackgroundThread implements Runnable {

	private static final Logger _logger = Logger.getLogger(SourceDeletionHandlerBackgroundThread.class);
	private MongoQueue _sourceDeletionQ = null;
	
	/////////////////////////////////////////////////////////////////////////////////////
	
	// TOP LEVEL LOGIC
	
	protected void performPoll()
	{
		SourcePojo deleteMessage = null;
		try {
			if (null == _sourceDeletionQ) {
				_sourceDeletionQ = new MongoQueue(DbManager.getIngest().getSourceDeletionQ().getDB().getName(), DbManager.getIngest().getSourceDeletionQ().getName());
			}
			deleteMessage = SourcePojo.fromDb(_sourceDeletionQ.pop(), SourcePojo.class);
			if (null == deleteMessage) {
				return;
			}		
			ObjectId communityId = deleteMessage.getCommunityIds().iterator().next();
			_logger.info("New source to delete key=" + deleteMessage.getKey() + " commId=" + communityId);
	
			
			StoreAndIndexManager dataStore = new StoreAndIndexManager("_api");
			long nDocsDeleted = dataStore.removeFromDatastoreAndIndex_bySourceKey(deleteMessage.getKey(), null, false, communityId.toString());
			
			DbManager.getDocument().getCounts().update(new BasicDBObject(DocCountPojo._id_, communityId), new BasicDBObject(DbManager.inc_, new BasicDBObject(DocCountPojo.doccount_, -nDocsDeleted)));
	
			_logger.info("Documents soft deleted key=" + deleteMessage.getKey() + " num=" + nDocsDeleted);		

			if (nDocsDeleted > 0) {
				// Do all this last:
				// (Not so critical if we time out here, the next harvest cycle should finish it; though would like to be able to offload this
				//  also if we are doing it from the API, then need a different getUUID so we don't collide with our own harvester...) 
				if (AggregationManager.updateEntitiesFromDeletedDocuments(dataStore.getUUID())) {
					dataStore.removeSoftDeletedDocuments();
					AggregationManager.updateDocEntitiesFromDeletedDocuments(dataStore.getUUID());
					_logger.info("Completed source deletion tidyup key=" + deleteMessage.getKey());
				}//TESTED			
			}//TESTED
		}
		catch (Throwable t) {
			StringBuffer sb = Globals.populateStackTrace(new StringBuffer(), t);
			if (null != deleteMessage) {		
				_logger.error("Error deleting source key=" + deleteMessage.getKey() + " err=" + sb.toString());
			}
			else {
				_logger.error("Error deleting source key=unknown err=" + sb.toString());				
			}
		}//TESTED
	}//TESTED
	
	/////////////////////////////////////////////////////////////////////////////////////
	
	// BACKGROUND INFRASTRUCTURE
	
	protected long _nPollingPeriod_ms = 500;
	protected Thread _pollThread;
	
	public SourceDeletionHandlerBackgroundThread() {
	}
	public void startThread() {
		_pollThread = new Thread(this);
		_pollThread.start();
	}

	@Override
	public void run() {
		for (;;) {
			performPoll();
						
			try {
				Thread.sleep(_nPollingPeriod_ms);
			} 
			catch (InterruptedException e)  {}
		}
	}
}
