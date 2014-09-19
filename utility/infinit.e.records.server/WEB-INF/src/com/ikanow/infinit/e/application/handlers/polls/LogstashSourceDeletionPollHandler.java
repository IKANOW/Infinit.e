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

import java.io.File;
import java.util.ArrayList;
import java.util.Date;

import org.elasticsearch.action.WriteConsistencyLevel;
import org.elasticsearch.action.admin.cluster.state.ClusterStateResponse;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.index.query.QueryBuilders;

import com.ikanow.infinit.e.application.data_model.TestLogstashExtractorPojo;
import com.ikanow.infinit.e.application.utils.MongoQueue;
import com.ikanow.infinit.e.data_model.index.ElasticSearchManager;
import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

public class LogstashSourceDeletionPollHandler implements PollHandler {

	public static String LOGSTASH_CONFIG = "/opt/logstash-infinite/logstash.conf.d/";
	public static String LOGSTASH_CONFIG_DISTRIBUTED = "/opt/logstash-infinite/dist.logstash.conf.d/";
	public static String LOGSTASH_WD = "/opt/logstash-infinite/logstash/";
	public static String LOGSTASH_RESTART_FILE = "/opt/logstash-infinite/RESTART_LOGSTASH";
	
	final static private String DUMMY_INDEX = "doc_dummy"; // (guaranteed to exist)
		
	private MongoQueue _logHarvesterQ = null;
	@Override
	public void performPoll() {
		
		boolean isSlave = false;
		
		if (null == LOGSTASH_CONFIG) { // (static memory not yet initialized)
			try {
				Thread.sleep(1000); // (extend the sleep time a bit)
			}
			catch (Exception e) {}
			return;			
		}
		
		File logstashDirectory = new File(LOGSTASH_CONFIG);
		String slaveHostname = null;
		if (!logstashDirectory.isDirectory() || !logstashDirectory.canRead() || !logstashDirectory.canWrite())
		{
			logstashDirectory = new File(LOGSTASH_CONFIG_DISTRIBUTED);
			isSlave = true;
			if (!logstashDirectory.isDirectory() || !logstashDirectory.canRead() || !logstashDirectory.canWrite())
			{				
				try {
					Thread.sleep(10000); // (extend the sleep time a bit)
				}
				catch (Exception e) {}
				return;
			}
			try {
				slaveHostname = java.net.InetAddress.getLocalHost().getHostName();
			}
			catch (Exception e) { // too complex if we don't have a hostname, just return
				return;
			}
		}
		
		// Deletion of distributed sources requires some co-ordination, we'll do it in master
		
		if (isSlave) { // register my existence
			BasicDBObject existence = new BasicDBObject("_id", slaveHostname);
			existence.put("ping", new Date());
			DbManager.getIngest().getLogHarvesterSlaves().save(existence);
		}//TESTED (by hand)
		else { // MASTER: clear out old slaves
			// (if it hasn't pinged for more than 30 minutes)
			long now = new Date().getTime();
			BasicDBObject deadSlaveQuery = new BasicDBObject("ping", new BasicDBObject(DbManager.lt_, new Date(now - 1000L*1800L)));
			boolean found = false;
			DBCursor dbc = DbManager.getIngest().getLogHarvesterSlaves().find(deadSlaveQuery);
			while (dbc.hasNext()) {
				BasicDBObject deadSlave = (BasicDBObject) dbc.next();
				found = true;
				String hostname = deadSlave.getString("_id");
				if (null != hostname) {
					DbManager.getIngest().getLogHarvesterQ().remove(new BasicDBObject("forSlave", hostname));					
				}
			}
			if (found) {
				DbManager.getIngest().getLogHarvesterSlaves().remove(deadSlaveQuery);
			}			
		}//TESTED (by hand)

		// Read delete elements from the Q...
		
		if (null == _logHarvesterQ) {
			_logHarvesterQ = new MongoQueue(DbManager.getIngest().getLogHarvesterQ().getDB().getName(), DbManager.getIngest().getLogHarvesterQ().getName());
		}
		BasicDBObject queueQuery = new BasicDBObject("deleteOnlyCommunityId", new BasicDBObject(DbManager.exists_, true));
		if (!isSlave) { // only get master messages
			queueQuery.put("forSlave", new BasicDBObject(DbManager.exists_, false));
		}
		else { // only get messages intended for me
			queueQuery.put("forSlave", slaveHostname);
		}
		DBObject nextElement = _logHarvesterQ.pop(queueQuery);
		while ( nextElement != null )
		{
			//DEBUG
			//System.out.println("HOST: " + slaveHostname + ": RECEIVED: " + nextElement.toString() + " FROM " + queueQuery);			
			
			TestLogstashExtractorPojo testInfo = TestLogstashExtractorPojo.fromDb(nextElement, TestLogstashExtractorPojo.class);
			if (null == testInfo.sourceKey) {
				continue; // need a sourceKey parameter...
			}
			if (!isSlave) { // slaves don't need to delete anything
				String commIdStr = testInfo.deleteOnlyCommunityId.toString();
				
				// Get all the indexes that might need to be cleansed:
				ElasticSearchManager indexMgr = ElasticSearchManager.getIndex(DUMMY_INDEX);
				
				// Stashed index
				
				ArrayList<String> indices = new ArrayList<String>();
				
				String stashedIndex = "recs_" + commIdStr;
				ClusterStateResponse retVal = indexMgr.getRawClient().admin().cluster().prepareState()
						.setIndices(stashedIndex)
						.setRoutingTable(false).setNodes(false).setListenerThreaded(false).get();
				
				if (!retVal.getState().getMetaData().getIndices().isEmpty()) {
					indices.add(stashedIndex);
				} // (else doesn't exist...)
				
				// Live indexes:
				
				String indexPattern = new StringBuffer("recs_t_").append(commIdStr).append("*").toString();
				retVal = indexMgr.getRawClient().admin().cluster().prepareState()
						.setIndices(indexPattern)
						.setRoutingTable(false).setNodes(false).setListenerThreaded(false).get();

				for (IndexMetaData indexMetadata: retVal.getState().getMetaData()) {
					//DEBUG
					//System.out.println("INDEX=" + indexMetadata.index());
					indices.add(indexMetadata.index());						
				}
				deleteSourceKeyRecords(indexMgr, indices.toArray(new String[0]), testInfo.sourceKey);
				
				// Now I've deleted, go and distribute the deletion messages to the slaves
				if ((null != testInfo.distributed) && testInfo.distributed) {
					// Copy into the slaves' queue
					DBCursor dbc = DbManager.getIngest().getLogHarvesterSlaves().find();
					while (dbc.hasNext()) {						
						BasicDBObject slave = (BasicDBObject) dbc.next();
						testInfo.forSlave = slave.getString("_id");
						_logHarvesterQ.push(testInfo.toDb());
						testInfo.forSlave = null;

						//DEBUG
						//System.out.println("DISTRIBUTING DELETION MESSAGE TO " + slave.toString());
					}
				}//TESTED (by hand)
			}//(end if is master)
			try {
				String fileToDelete = new StringBuffer(LOGSTASH_WD).append(".sincedb_").append(testInfo._id.toString()).toString();
				
				boolean deleted = new File(fileToDelete).delete();
				
				if (deleted) { // this file actually existed - need to restart the logstash unfortunately
					new File(LOGSTASH_RESTART_FILE).createNewFile();					
				}

				//DEBUG
				//System.out.println("DELETED " + fileToDelete + " ? " + deleted);
			}
			catch (Exception e) {
				//e.printStackTrace();				
			} // probably just doesn't exist
			
			// Get next element and carry on
			nextElement = _logHarvesterQ.pop(queueQuery);
			
		}//TESTED
	}
	protected void deleteSourceKeyRecords(ElasticSearchManager indexMgr, String[] indices, String sourceKey) {
		indexMgr.getRawClient().prepareDeleteByQuery().setIndices(indices)
									.setQuery(QueryBuilders.termQuery(DocumentPojo.sourceKey_, sourceKey))
									.setConsistencyLevel(WriteConsistencyLevel.ONE)
									.execute().actionGet();
	}//TESTED
}
