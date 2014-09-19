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
package com.ikanow.infinit.e.data_model.control;

import java.util.Date;
import java.util.List;

import org.bson.types.ObjectId;

import com.google.gson.reflect.TypeToken;
import com.ikanow.infinit.e.data_model.api.BaseApiPojo;
import com.ikanow.infinit.e.data_model.api.knowledge.AdvancedQueryPojo;
import com.ikanow.infinit.e.data_model.store.social.sharing.SharePojo;

public class DocumentQueueControlPojo extends BaseApiPojo {

	// Standard static function for readability
	@SuppressWarnings("unchecked")
	static public TypeToken<List<DocumentQueueControlPojo>> listType() { return new TypeToken<List<DocumentQueueControlPojo>>(){}; }
	
	private String queueName; // (same as the share title)

	// User queues: filled/emptied manually
	// Query queues: filled automatically based on a scheduled query 
	public enum DocQueueType { UserQueue, SavedQueryQueue };	
	public static final String UserQueue = "infinite-user-queue";
	public static final String SavedQueryQueue = "infinite-query-queue";
	
	private DocQueueType queueType = null;
	private Integer maxDocs = null; // (defaults to score.numAnalyze, or 1000 if not specified)
	
	// For saved query queues, need the query
	
	public static class SavedQueryAlertInfo {
		private List<String> emailAddresses; // List of email addresses to which to send alerts 
		private Integer maxDocsToInclude; // (defaults to 10, -1 means no limit) The max number of new docs to include in the alert email
		// (later include more controls - eg descriptions, entity information etc)
		
		// ACCESSORS:
		final static public String emailAddresses_ = "emailAddresses";
		final static public String savedQuery_emailAddresses_ = "queryInfo.alert.lastRun";
		public List<String> getEmailAddresses() { return emailAddresses; }
		public void setEmailAddresses(List<String> emailAddresses) { this.emailAddresses = emailAddresses; }
		public Integer getMaxDocsToInclude() { return maxDocsToInclude; }
		public void setMaxDocsToInclude(Integer maxDocsToInclude) { this.maxDocsToInclude = maxDocsToInclude; }
	}
	public static class SavedQueryInfo {
		//Either:
		private ObjectId queryId = null;  // (tied to infinite-saved-query share)
		
		//Or:
		private AdvancedQueryPojo query = null; // (fixed query)
		
		public enum DocQueueFrequency { Near_Real_Time, Hourly, Daily, Weekly };
		private DocQueueFrequency frequency = null;
		private Integer frequencyOffset = null; // (only in daily+ frequencies)
		
		private Date lastRun = null; // (data query was last run)

		private SavedQueryAlertInfo alert = null; // (alerting metadata)
		
		// ACCESSORS:
		
		final static public String queryId_ = "queryId";
		final static public String savedQuery_queryId_ = "queryInfo.queryId";
		final static public String query_ = "query";
		final static public String savedQuery_query_ = "queryInfo.query";
		final static public String querySummary_ = "querySummary";
		final static public String savedQuery_querySummary_ = "queryInfo.querySummary";
		final static public String frequency_ = "frequency";
		final static public String savedQuery_frequency_ = "queryInfo.frequency";
		final static public String frequencyOffset_ = "frequencyOffset";
		final static public String savedQuery_frequencyOffset_ = "queryInfo.frequencyOffset";
		final static public String lastRun_ = "lastRun";
		final static public String savedQuery_lastRun_ = "queryInfo.lastRun";
		final static public String alert_ = "alert";
		final static public String savedQuery_alert_ = "queryInfo.alert";
		
		public AdvancedQueryPojo getQuery() { return query; }
		public void setQuery(AdvancedQueryPojo query) { this.query = query; }
		public Integer getFrequencyOffset() { return frequencyOffset; }
		public void setFrequencyOffset(Integer freqOffset) { this.frequencyOffset = freqOffset; }
		public Date getLastRun() { return lastRun; }
		public void setLastRun(Date lastRun) { this.lastRun = lastRun; }
		public ObjectId getQueryId() { return queryId; }
		public void setQueryId(ObjectId queryId) { this.queryId = queryId; }
		public DocQueueFrequency getFrequency() { return frequency; }
		public void setFrequency(DocQueueFrequency frequency) { this.frequency = frequency; }
		public SavedQueryAlertInfo getAlert() { return alert; }
		public void setAlert(SavedQueryAlertInfo alert) { this.alert = alert; }
	}
	SavedQueryInfo queryInfo = null;
	
	// For user queries (and in some cases saved queue queries)
	
	private ObjectId lastDocIdInserted;
	private List<ObjectId> queueList = null;
	
	// ACCESSORS:
	
	final static public String queueName_ = "queueName";
	final static public String queueType_ = "queueType";
	final static public String queryInfo_ = "queryInfo";
	final static public String lastDocIdInserted_ = "lastDocIdInserted";
	final static public String queueList_ = "queueList";
	final static public String maxDocs_ = "maxDocs";
	
	public String getQueueName() {
		return queueName;
	}
	public void setQueueName(String queueName) {
		this.queueName = queueName;
	}
	public DocQueueType getQueueType() {
		return queueType;
	}
	public void setQueueType(DocQueueType queueType) {
		this.queueType = queueType;
	}
	public SavedQueryInfo getQueryInfo() {
		return queryInfo;
	}
	public void setQueryInfo(SavedQueryInfo queryInfo) {
		this.queryInfo = queryInfo;
	}
	public ObjectId getLastDocIdInserted() {
		return lastDocIdInserted;
	}
	public void setLastDocIdInserted(ObjectId lastDocIdInserted) {
		this.lastDocIdInserted = lastDocIdInserted;
	}
	public List<ObjectId> getQueueList() {
		return queueList;
	}
	public void setQueueList(List<ObjectId> queueList) {
		this.queueList = queueList;
	}
	public Integer getMaxDocs() {
		return maxDocs;
	}
	public void setMaxDocs(Integer maxDocs) {
		this.maxDocs = maxDocs;
	}
	////////////////////
	
	transient public SharePojo _parentShare = null; // (convenience)
}
