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

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.elasticsearch.action.admin.cluster.state.ClusterStateResponse;
import org.elasticsearch.cluster.metadata.IndexMetaData;

import com.ikanow.infinit.e.data_model.index.ElasticSearchManager;
import com.ikanow.infinit.e.data_model.index.IndexManager;
import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.utils.MongoApplicationLock;
import com.ikanow.infinit.e.data_model.utils.ThreadSafeSimpleDateFormat;

public class LogstashIndexAgeOutPollHandler implements PollHandler {

	final static private int AGE_OUT_TIME_DAYS = 30;
	@SuppressWarnings("unused")
	final static private int AGE_OUT_TIME_MONTHS = 12;
	@SuppressWarnings("unused")
	final static private int AGE_OUT_TIME_YEARS = 3;
	
	final static private String DUMMY_INDEX = "doc_dummy"; // (guaranteed to exist)
	
	static private Pattern INDEX_TO_COMMUNITY_AND_DATE = Pattern.compile("recs_t_([0-9a-z]+)_(.*)", Pattern.CASE_INSENSITIVE);
	
	@Override
	public void performPoll() {
		
		if (null == DUMMY_INDEX) { // (static memory not yet initialized)
			try {
				Thread.sleep(1000); // (extend the sleep time a bit)
			}
			catch (Exception e) {}
			return;			
		}
		
		// 1] Application lock: only one node per cluster gets to do this  
		
		if (!MongoApplicationLock.getLock(DbManager.getIngest().getSource().getDB().getName()).acquire(100))  {
			return;
		}//TESTED
		
		// 2] Get all the elasticsearch indexes that can time out:		
		// (https://github.com/elasticsearch/elasticsearch/blob/master/src/main/java/org/elasticsearch/rest/action/admin/indices/alias/get/RestGetIndicesAliasesAction.java)
		
		ElasticSearchManager indexMgr = ElasticSearchManager.getIndex(DUMMY_INDEX);
		ClusterStateResponse retVal = indexMgr.getRawClient().admin().cluster().prepareState()
				.setIndices("recs_t_*")
				.setRoutingTable(false).setNodes(false).setListenerThreaded(false).get();

		long now = new Date().getTime();
		
		for (IndexMetaData indexMetadata: retVal.getState().getMetaData()) {
			String index = indexMetadata.index(); 

			//DEBUG
			//System.out.println("INDEX = " + index);
			
			Matcher m = INDEX_TO_COMMUNITY_AND_DATE.matcher(index);
			if (!m.matches() || (2 != m.groupCount())) {
				continue; // (just looks like one of our indexes)
			}
			
			// 3] Go get the community ... the record is in format "recs_t_<community>_<data_format>" 
			
			//TODO (INF-2533): check if the community has a default age-out period

			@SuppressWarnings("unused")
			String communityId = m.group(1);
			int ageOutTime = -1;
			
			//DEBUG
			//System.out.println("INDEX COMMUNITY = " + communityId);
			
			// 4] Now parse out the date, in one of the following formats:
			// - YYYY.MM.DD	... (default age-out: 30 days)
			//TODO (INF-2533): add these
			// - YYYY.MM	... (default age-out: 12 months) 
			// - YYYY		... (default age-out: 3 years)
			
			String dateStr = m.group(2);
			Date indexDate = null;
			long periodInMs = -1;
			ThreadSafeSimpleDateFormat dateFormatter = new ThreadSafeSimpleDateFormat("yyyy.MM.dd");
			try {
				indexDate = dateFormatter.parse(dateStr);
				if (-1 == ageOutTime) { // (ie not overridden)
					ageOutTime = AGE_OUT_TIME_DAYS; // (default)
				}
				periodInMs = 3600L*24L*1000L; // (24h)
			}//TESTED
			catch (Exception e) { // failed to parse date, just carry on
				indexDate = null;
			}
			//TODO (INF-2533): add other date formats here
			
			if (null == indexDate) {
				continue;
			}
			// If we're here we managed to parse one of the dates
			
			//DEBUG
			//System.out.println("INDEX DATE = " + indexDate);
			
			long then = indexDate.getTime();
			if ((now - then) > periodInMs*ageOutTime) {
				//DEBUG
				//System.out.println("DELETE INDEX: " + (now - then)/(periodInMs*ageOutTime));
				
				ElasticSearchManager recordsIndex = IndexManager.getIndex(index);
				recordsIndex.deleteMe();			
			}//TESTED
		}
	}//TESTED
}
