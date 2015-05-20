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

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.mahout.math.Arrays;
import org.bson.types.ObjectId;

import com.ikanow.infinit.e.api.knowledge.QueryHandler;
import com.ikanow.infinit.e.data_model.api.ResponsePojo;
import com.ikanow.infinit.e.data_model.api.knowledge.AdvancedQueryPojo;
import com.ikanow.infinit.e.data_model.control.DocumentQueueControlPojo;
import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.infinit.e.data_model.store.social.sharing.SharePojo;
import com.ikanow.infinit.e.data_model.utils.SendMail;
import com.ikanow.infinit.e.processing.custom.utils.PropertiesManager;
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
			long lastRun = savedQuery.getLastDocIdInserted().getDate().getTime();
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
					
					// Some alerting preamble
					StringBuffer alertText = null;
					StringBuffer alertTitle = null;
					String rootUrl = new PropertiesManager().getURLRoot().replace("/api/", "");
					int maxDocsToAdd = 10; // (default)
					boolean alert = false;
					if ((null != toModify.getQueryInfo().getAlert()) && (null != toModify.getQueryInfo().getAlert().getEmailAddresses())
							&& !toModify.getQueryInfo().getAlert().getEmailAddresses().isEmpty())
					{
						alert = true;
						alertText = new StringBuffer();
						if (null != toModify.getQueryInfo().getAlert().getMaxDocsToInclude()) {
							maxDocsToAdd = toModify.getQueryInfo().getAlert().getMaxDocsToInclude();
							if (maxDocsToAdd < 0) {
								maxDocsToAdd = Integer.MAX_VALUE;
							}
						}												
						createAlertPreamble(alertText, toModify.getQueryInfo().getQuery(), savedQuery._parentShare.get_id(), rootUrl);
					}//TESTED
					
					// Add new docs...

					int numDocsAdded = 0;
					for (BasicDBObject doc: docs) {
						ObjectId docId = doc.getObjectId(DocumentPojo._id_);
						if (null != docId) {
							if (null != ignoreBeforeId) {
								if (docId.compareTo(ignoreBeforeId) <= 0) { // ie docId <= ignoreBeforeId
									continue;
								}
							}//(end check if this doc has already been seen)							
							
							toModify.getQueueList().add(0, docId);

							//Alerting
							if (alert) {
								// (this fn checks if the max number of docs have been added):
								createAlertDocSummary(alertText, numDocsAdded, maxDocsToAdd, doc, rootUrl);
								numDocsAdded++;
							}
							
							if (null == maxDocId) {
								maxDocId = docId;
							}
							else if (maxDocId.compareTo(docId) < 0) { // ie maxDocId < docId
								maxDocId = docId;
							}
						}//TESTED (test5)
					}//(end loop over new docs)
					
					// More alerting
					if (alert && (numDocsAdded > 0)) {
						alertTitle = new StringBuffer("IKANOW: Queue \"").append(toModify.getQueueName()).append("\" has ").append(numDocsAdded).append(" new");
						if (numDocsAdded == 1) {
							alertTitle.append(" document.");
						}
						else {
							alertTitle.append(" documents.");							
						}
						// (terminate the doc list)
						if (maxDocsToAdd > 0) {
							alertText.append("</ol>");
							alertText.append("\n");
						}
						
						String to = (Arrays.toString(toModify.getQueryInfo().getAlert().getEmailAddresses().toArray()).replaceAll("[\\[\\]]", "")).replace(',', ';');
						try {
							new SendMail(null, to, alertTitle.toString(), alertText.toString()).send("text/html");
						}
						catch (Exception e) {
							//DEBUG
							//e.printStackTrace();
						}
					}//TESTED
					
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

	//////////////////////////////////////////////////////////////////////
	
	// ALERT EMAIL UTILS
	
	public static void createAlertPreamble(StringBuffer alertEmailText, AdvancedQueryPojo query, ObjectId queueId, String rootUrl) 
	{
		alertEmailText.append("<p>");
		alertEmailText.append("Links for viewing the documents in the GUI:");
		alertEmailText.append("</p>");
		alertEmailText.append("\n");
		
		alertEmailText.append("<ul>");
		alertEmailText.append("\n");
		alertEmailText.append("<li/>");
		alertEmailText.append("<a href=\"").append(rootUrl);
		try {
			alertEmailText.append("?query=");
			StringBuffer guiQuery = new StringBuffer("{\"qt\":[{\"ftext\":\"$cache:").append(queueId).append("\"}]}");
			alertEmailText.append(URLEncoder.encode(guiQuery.toString(), "UTF-8"));
			alertEmailText.append("&communityIds=").append(Arrays.toString(query.communityIds.toArray()).replaceAll("[\\[\\]]", ""));				
		}
		catch (Exception e) {} // (just carry on)
		alertEmailText.append("\">");
		alertEmailText.append("The current queue");
		alertEmailText.append("</a>");
		alertEmailText.append("\n");
		alertEmailText.append("<li/>");
		alertEmailText.append("<a href=\"").append(rootUrl);
		try {
			alertEmailText.append("?query=");
			alertEmailText.append(URLEncoder.encode(query.toApi(), "UTF-8").replace("+", "%20"));
			alertEmailText.append("&communityIds=").append(Arrays.toString(query.communityIds.toArray()).replaceAll("[\\[\\]]", ""));				
		}
		catch (Exception e) {} // (just carry on)
		alertEmailText.append("\">");
		alertEmailText.append("Results from the saved query");
		alertEmailText.append("</a>");
		alertEmailText.append("\n");		
		alertEmailText.append("</ul>");
		alertEmailText.append("\n");
		
	}//TESTED
	
	public static void createAlertDocSummary(StringBuffer alertEmailText, int docNum, int numDocSummaries, BasicDBObject doc, String rootUrl) 
	{
		if (docNum < numDocSummaries) {
			// Preamble on the first doc
			if (0 == docNum) {
				alertEmailText.append("<p>");
				alertEmailText.append("Top ").append(numDocSummaries);
				if (0 == numDocSummaries) {
					alertEmailText.append(" document:");
				}
				else {
					alertEmailText.append(" documents:");				
				}
				alertEmailText.append("</p>");
				alertEmailText.append("\n");
				alertEmailText.append("<ol>");
				alertEmailText.append("\n");
			}		
			// Docs:			
			StringBuffer guiQuery = new StringBuffer("{\"qt\":[{\"ftext\":\"_id:").append(doc.getObjectId(DocumentPojo._id_)).append("\"}]}");
			String url = doc.getString(DocumentPojo.displayUrl_, doc.getString(DocumentPojo.url_));
			String title = doc.getString(DocumentPojo.title_, url);
			alertEmailText.append("<li/>");
			alertEmailText.append(title);
			alertEmailText.append(" [");
			alertEmailText.append(doc.getDate(DocumentPojo.publishedDate_, doc.getDate(DocumentPojo.created_)));
			alertEmailText.append("]");
			alertEmailText.append(" (");
			alertEmailText.append("<a href=\"").append(rootUrl);
			try {
				alertEmailText.append("?query=");
				alertEmailText.append(URLEncoder.encode(guiQuery.toString(), "UTF-8"));
				alertEmailText.append("&communityIds=").append(doc.getObjectId(DocumentPojo.communityId_, new ObjectId("4c927585d591d31d7b37097a")));				
			}
			catch (Exception e) {} // (just carry on)
			alertEmailText.append("\">");
			alertEmailText.append("GUI</a>)");
			if ((null != url) && (url.startsWith("http"))) {
				alertEmailText.append(" (");				
				alertEmailText.append("<a href=\"").append(url).append("\">");
				alertEmailText.append("External Link</a>)");
			}
			alertEmailText.append("\n");
		}
	}//TESTED
}
