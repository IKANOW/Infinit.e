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
import java.util.LinkedList;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;
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

	private static final Logger _logger = Logger.getLogger(LogstashSourceDeletionPollHandler.class);
	
	public static String LOGSTASH_CONFIG = "/opt/logstash-infinite/logstash.conf.d/";
	public static String LOGSTASH_CONFIG_DISTRIBUTED = "/opt/logstash-infinite/dist.logstash.conf.d/";
	public static String LOGSTASH_WD = "/opt/logstash-infinite/logstash/";
	public static String LOGSTASH_RESTART_FILE = "/opt/logstash-infinite/RESTART_LOGSTASH";
	public static String LOGSTASH_CONFIG_EXTENSION = ".auto.conf";
	
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
					
					_logger.info("Removing unresponsive slave host=" + hostname);
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
		LinkedList<TestLogstashExtractorPojo> secondaryQueue = new LinkedList<TestLogstashExtractorPojo>();
		LinkedList<String> deleteAfterRestartQueue = new LinkedList<String>();
		boolean deletedSources = false;
		boolean deletedSinceDbs = false;
		while ( nextElement != null )
		{
			//DEBUG
			//System.out.println("HOST: " + slaveHostname + ": RECEIVED: " + nextElement.toString() + " FROM " + queueQuery);
			_logger.info("host=" + slaveHostname + " received=" + nextElement.toString() + " from=" + queueQuery);
			
			TestLogstashExtractorPojo testInfo = TestLogstashExtractorPojo.fromDb(nextElement, TestLogstashExtractorPojo.class);
			if (null == testInfo.sourceKey) {
				continue; // need a sourceKey parameter...
			}
			if (!isSlave) { // slaves don't need to delete anything from the index, only files
				secondaryQueue.add(testInfo);
			}//(end if master)
				
			try {
				// First off - need to remove the conf file and restart logstash if we're actually deleting this...
				boolean deletedSource = false;
				if ((null == testInfo.deleteDocsOnly) || !testInfo.deleteDocsOnly) { // (default = delete entire source)
					deletedSources = true;
					deletedSource = true;
					
					String fileToDelete = new StringBuffer(LOGSTASH_CONFIG).append(testInfo._id.toString()).append(LOGSTASH_CONFIG_EXTENSION).toString();
					
					boolean deleted = false;
					try {
						deleted = new File(fileToDelete).delete();
					}
					catch (Exception e) {}
					
					//DEBUG
					//System.out.println("DELETED CONF FILE" + fileToDelete + " ? " + deleted);
					_logger.info("delete conf_file=" + fileToDelete + " success=" + deleted);
				}//TESTED (docs-only + source deletion)
				
				// If _not_ deleting the source, then do delete the sincedb file
				// (else let it get cleaned up separately - minimizes race conditions where the source starts ingesting again)
				String fileToDelete = new StringBuffer(LOGSTASH_WD).append(".sincedb_").append(testInfo._id.toString()).toString();
				if (!deletedSource) {
					
					boolean deleted = false;
					try {
						deleted = new File(fileToDelete).delete();
						deletedSinceDbs |= deleted;
					}
					catch (Exception e) {}
					
					//DEBUG
					//System.out.println("DELETED SINCEDB" + fileToDelete + " ? " + deletedSinceDb);
					_logger.info("primary delete sincedb_file=" + fileToDelete + " success=" + deleted);
				}
				else {
					deleteAfterRestartQueue.add(fileToDelete);
				}//TESTED (primary + secondary deletes)
				
			}
			catch (Exception e) {
				//e.printStackTrace();				
			} // probably just doesn't exist				
			
			// Get next element and carry on
			nextElement = _logHarvesterQ.pop(queueQuery);
			
		}//TESTED (end first loop over elements to delete)
		
		if (deletedSources || deletedSinceDbs) { // this file actually existed - need to restart the logstash unfortunately
			_logger.info("Restarting logstash, and sleeping until logstash is restarted");
			try {
				new File(LOGSTASH_RESTART_FILE).createNewFile();	
				for (int i = 0; i < 12; ++i) {
					Thread.sleep(10L*1000L);
					if (!new File(LOGSTASH_RESTART_FILE).exists()) {
						Thread.sleep(5L*1000L); // (extra wait for it to shut down)
						break; // (early exit)
					}
				}
			}
			catch (Exception e) {}					
		}//TESTED (from doc deletion and from src deletion)
		
		for (String fileToDelete: deleteAfterRestartQueue) {
			boolean deleted = false;
			try {
				deleted = new File(fileToDelete).delete();
			}
			catch (Exception e) {}
			
			//DEBUG
			//System.out.println("DELETED SINCEDB" + fileToDelete + " ? " + deletedSinceDb);
			_logger.info("secondary delete sincedb_file=" + fileToDelete + " success=" + deleted);			
		}//TESTED (primary and secondary deletion)
		
		for (TestLogstashExtractorPojo testInfo: secondaryQueue) {
			
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
			
			_logger.info("Deleted key=" + testInfo.sourceKey + " from indexes=" + ArrayUtils.toString(indices.toArray()));
			
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
					_logger.info("distributing deletion message to host=" + slave.toString());
				}
			}//TESTED (by hand)
		}//(end loop over secondary queue, ie to actually delete the indexes)		
		
	}
	protected void deleteSourceKeyRecords(ElasticSearchManager indexMgr, String[] indices, String sourceKey) {
		indexMgr.getRawClient().prepareDeleteByQuery().setIndices(indices)
									.setQuery(QueryBuilders.termQuery(DocumentPojo.sourceKey_, sourceKey))
									.setConsistencyLevel(WriteConsistencyLevel.ONE)
									.execute().actionGet();
	}//TESTED
}
