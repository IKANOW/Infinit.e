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
import com.mongodb.DBObject;

public class LogstashSourceDeletionPollHandler implements PollHandler {

	public static String LOGSTASH_CONFIG = "/opt/logstash-infinite/logstash.conf.d/";
	public static String LOGSTASH_WD = "/opt/logstash-infinite/logstash/";
	
	final static private String DUMMY_INDEX = "doc_dummy"; // (guaranteed to exist)
		
	private MongoQueue _logHarvesterQ = null;
	@Override
	public void performPoll() {
		
		if (null == LOGSTASH_CONFIG) { // (static memory not yet initialized)
			try {
				Thread.sleep(1000); // (extend the sleep time a bit)
			}
			catch (Exception e) {}
			return;			
		}
		
		// Can only do this poll for logstash enabled nodes (because need to remove the .sincedb) in some cases
		File logstashDirectory = new File(LOGSTASH_CONFIG);
		if (!logstashDirectory.isDirectory() || !logstashDirectory.canRead() || !logstashDirectory.canWrite())
		{
			try {
				Thread.sleep(10000); // (extend the sleep time a bit)
			}
			catch (Exception e) {}
			return;
		}

		// Read delete elements from the Q...
		
		if (null == _logHarvesterQ) {
			_logHarvesterQ = new MongoQueue(DbManager.getIngest().getLogHarvesterQ().getDB().getName(), DbManager.getIngest().getLogHarvesterQ().getName());
		}
		BasicDBObject queueQuery = new BasicDBObject("deleteOnlyCommunityId", new BasicDBObject(DbManager.exists_, true));
		DBObject nextElement = _logHarvesterQ.pop(queueQuery);
		while ( nextElement != null )
		{
			TestLogstashExtractorPojo testInfo = TestLogstashExtractorPojo.fromDb(nextElement, TestLogstashExtractorPojo.class);
			if (null == testInfo.sourceKey) {
				continue; // need a sourceKey parameter...
			}
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
			try {
				String fileToDelete = new StringBuffer(LOGSTASH_WD).append(".sincedb_").append(testInfo._id.toString()).toString();
				
				@SuppressWarnings("unused")
				boolean deleted = new File(fileToDelete).delete();
				
				//DEBUG
				//System.out.println("DELETED " + fileToDelete + " ? " + deleted);
			}
			catch (Exception e) {} // probably just doesn't exist
			
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
