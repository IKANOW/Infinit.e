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
package com.ikanow.infinit.e.processing.custom.launcher;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.bson.types.ObjectId;

import com.ikanow.infinit.e.api.knowledge.QueryHandler;
import com.ikanow.infinit.e.data_model.api.ResponsePojo;
import com.ikanow.infinit.e.data_model.api.knowledge.AdvancedQueryPojo;
import com.ikanow.infinit.e.data_model.control.DocumentQueueControlPojo;
import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.infinit.e.data_model.store.social.sharing.SharePojo;
import com.mongodb.BasicDBObject;

public class CustomSavedQueryQueueLauncher {

	private static Logger _logger = Logger.getLogger(CustomSavedQueryQueueLauncher.class);
	
	@SuppressWarnings("unchecked")
	public static void executeQuery(DocumentQueueControlPojo savedQuery) {
		
		if (null == savedQuery._parentShare) {
			return;
		}
		
		AdvancedQueryPojo query = savedQuery.getQueryInfo().getQuery();

		// 1) append the a time as an extra query term (unless it's the first time)
		
		if (null != savedQuery.getLastDocIdInserted()) {
			long lastRun = savedQuery.getLastDocIdInserted().getTime();
			if (null != savedQuery.getQueryInfo().getLastRun()) {
				long altLastRun = savedQuery.getQueryInfo().getLastRun().getTime();
				if (altLastRun < lastRun) { // pick the longest duration
					lastRun = altLastRun;
				}
			}
			lastRun = ((new Date().getTime() - lastRun)/1000L + 3599L)/3600L; // (hours rounded up)
			
			if (lastRun < (14L*24L)) { // if it's more than 14 days then query over everything				
				
				if (null == query.qt) {
					query.qt = new ArrayList<AdvancedQueryPojo.QueryTermPojo>(1);
				}
				AdvancedQueryPojo.QueryTermPojo extraTermTime = new AdvancedQueryPojo.QueryTermPojo();
				extraTermTime.time = new AdvancedQueryPojo.QueryTermPojo.TimeTermPojo();
				extraTermTime.time.max = "now+1d"; // (ie now plus some margin)
				if (savedQuery.getQueryInfo().getFrequency() == DocumentQueueControlPojo.SavedQueryInfo.DocQueueFrequency.Hourly)
				{
					extraTermTime.time.min = "now-" + (lastRun + 1) + "h";
					//extraTermTime.time.min = "now-2h"; // (just add some margin)
				}
				else if (savedQuery.getQueryInfo().getFrequency() == DocumentQueueControlPojo.SavedQueryInfo.DocQueueFrequency.Daily)
				{
					extraTermTime.time.min = "now-" + (lastRun + 6) + "h";
					//extraTermTime.time.min = "now-30h"; // (just add some margin)				
				}
				else if (savedQuery.getQueryInfo().getFrequency() == DocumentQueueControlPojo.SavedQueryInfo.DocQueueFrequency.Weekly)
				{
					lastRun = (lastRun + 23L)/24L;
					extraTermTime.time.min = "now-" + (lastRun + 1) + "d";
					//extraTermTime.time.min = "now-8d"; // (just add some margin)							
				}
				query.qt.add(extraTermTime);
				
				if (null != query.logic) { // else terms ANDed together, ie what I want
					query.logic = "(" + query.logic + ") AND " + query.qt.size();
				}
			}
		}//TESTED (test3abc)
		
		// 2) other minor mods to the query engine (because there's lots we don't care about)
		
		if (null == query.output){
			query.output = new AdvancedQueryPojo.QueryOutputPojo();
			if (null == query.output.docs) {
				query.output.docs = new AdvancedQueryPojo.QueryOutputPojo.DocumentOutputPojo();
			}
		}
		if (null == query.score) {
			query.score = new AdvancedQueryPojo.QueryScorePojo();
		}
		if (null == query.input) {
			query.input = new AdvancedQueryPojo.QueryInputPojo();
		}
		query.output.aggregation = null; // (no aggregations)
		query.output.docs.ents = false;
		query.output.docs.events = false;
		query.output.docs.facts = false;
		query.output.docs.summaries = false;
		query.output.docs.eventsTimeline = false;
		query.output.docs.metadata = false;
		if (null == query.output.docs.numReturn) {
			query.output.docs.numReturn = 100; // (default)
		}		
		if (null == query.score.numAnalyze) {
			query.output.docs.numReturn = 1000; // (default)			
		}
		//TESTED (entire block)
		
		// 3) run saved query:
		
		QueryHandler queryHandler = new QueryHandler();
		
		StringBuffer errorString = new StringBuffer();
		StringBuffer communityIdStrList = new StringBuffer();
		for (ObjectId commId: savedQuery.getQueryInfo().getQuery().communityIds) {
			if (communityIdStrList.length() > 0) {
				communityIdStrList.append(',');
			}
			communityIdStrList.append(commId.toString());
		}//TESTED
		
		try {
			//DEBUG
			//System.out.println("COMMS="+communityIdStrList.toString() + ": QUERY=" + query.toApi());
			
			// (should have a version of this that just returns the IPs from the index engine)
			// (for now this will do)
			ResponsePojo rp = queryHandler.doQuery(savedQuery._parentShare.getOwner().get_id().toString(), 
					query, communityIdStrList.toString(), errorString);
			
			if (null == rp) {
				throw new RuntimeException(errorString.toString()); // (handled below)
			}
			
			// 4) Add the results to the original data
			
			SharePojo savedQueryShare = SharePojo.fromDb(DbManager.getSocial().getShare().findOne(
					new BasicDBObject(SharePojo._id_, savedQuery._parentShare.get_id())), SharePojo.class);
			
			if (null != savedQueryShare) {
				DocumentQueueControlPojo toModify = DocumentQueueControlPojo.fromApi(savedQueryShare.getShare(), DocumentQueueControlPojo.class);
				List<BasicDBObject> docs = (List<BasicDBObject>) rp.getData();
				if ((null != docs) && !docs.isEmpty()) {
					if (null == toModify.getQueueList()) {
						toModify.setQueueList(new ArrayList<ObjectId>(docs.size()));
					}
					ObjectId ignoreBeforeId = toModify.getLastDocIdInserted();
					ObjectId maxDocId = toModify.getLastDocIdInserted();

					//DEBUG
					//System.out.println("before, num docs=" + toModify.getQueueList().size() + " adding " + docs.size() + " from " + ignoreBeforeId);
					
					// Add new docs...

					for (BasicDBObject doc: docs) {
						ObjectId docId = doc.getObjectId(DocumentPojo._id_);
						if (null != docId) {
							if (null != ignoreBeforeId) {
								if (docId.compareTo(ignoreBeforeId) <= 0) { // ie docId <= ignoreBeforeId
									continue;
								}
							}//(end check if this doc has already been seen)							
							
							toModify.getQueueList().add(0, docId);
							if (null == maxDocId) {
								maxDocId = docId;
							}
							else if (maxDocId.compareTo(docId) < 0) { // ie maxDocId < docId
								maxDocId = docId;
							}
						}//TESTED (test5)
					}//(end loop over new docs)
					
					// Remove old docs...
					
					int maxDocs = query.output.docs.numReturn;
					if (null != toModify.getMaxDocs()) { // override
						maxDocs = toModify.getMaxDocs();
					}
					
					if (toModify.getQueueList().size() > maxDocs) {
						toModify.setQueueList(toModify.getQueueList().subList(0, maxDocs));
					}//TESTED (test2.2)

					//DEBUG
					//System.out.println("after, num docs=" + toModify.getQueueList().size() + " at " + maxDocId);
					
					// Update share info:
					toModify.setLastDocIdInserted(maxDocId);
					
					// We've modified the share so update it:
					savedQueryShare.setShare(toModify.toApi());
					savedQueryShare.setModified(new Date());
					DbManager.getSocial().getShare().save(savedQueryShare.toDb());
					
				}//(end found some docs)
				
			}//(end found share)
			
		} 
		catch (Exception e) {
			_logger.info("knowledge/query userid=" + savedQuery._parentShare.getOwner().get_id() + " groups=" + communityIdStrList + " error=" + e.getMessage());
		}
	}//TESTED
	
}
