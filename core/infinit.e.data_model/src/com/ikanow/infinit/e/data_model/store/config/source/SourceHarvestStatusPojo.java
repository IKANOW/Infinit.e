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
package com.ikanow.infinit.e.data_model.store.config.source;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

import com.google.gson.reflect.TypeToken;
import com.ikanow.infinit.e.data_model.InfiniteEnums.HarvestEnum;
import com.ikanow.infinit.e.data_model.store.BaseDbPojo;

public class SourceHarvestStatusPojo extends BaseDbPojo {
	// Standard static function for readability
	@SuppressWarnings("unchecked")
	static public TypeToken<List<SourceHarvestStatusPojo>> listType() { return new TypeToken<List<SourceHarvestStatusPojo>>(){}; }
 
	private Date harvested = null; // (last harvested - but can be smoothed to avoid clumping, so more for internal usage)
	final public static String harvested_ = "harvested";
	final public static String sourceQuery_harvested_ = "harvest.harvested";
	private Date realHarvested = null; // (last harvested - can't be changed, so for display purposes)
	final public static String realHarvested_ = "realHarvested";
	final public static String sourceQuery_realHarvested_ = "harvest.realHarvested";
	private Date synced = null; // (time last syncronized)
	final public static String synced_ = "synced"; 
	final public static String sourceQuery_synced_ = "harvest.synced";
	private Date extracted = null; // (last harvest that actually pulled docs)
	final public static String extracted_ = "extracted"; 
	final public static String sourceQuery_extracted_ = "harvest.extracted";
	private HarvestEnum harvest_status = null;
	final public static String harvest_status_ = "harvest_status";
	final public static String sourceQuery_harvest_status_ = "harvest.harvest_status";
	private String harvest_message = null; // (this isn't just a display message, it is used for state in db and feed harvesters)
	final public static String harvest_message_ = "harvest_message";
	final public static String sourceQuery_harvest_message_ = "harvest.harvest_message";
	private Long doccount = null;
	final public static String doccount_ = "doccount";
	final public static String sourceQuery_doccount_ = "harvest.doccount";
	private String lastHarvestedBy = null; // (last hostname to modify this source)
	final public static String lastHarvestedBy_ = "lastHarvestedBy";
	final public static String sourceQuery_lastHarvestedBy_ = "harvest.lastHarvestedBy";
	// Distribution logic:
	private Integer distributionTokensFree = null;
	final public static String distributionTokensFree_ = "distributionTokensFree";
	final public static String sourceQuery_distributionTokensFree_ = "harvest.distributionTokensFree";
	private Integer distributionTokensComplete = null;
	final public static String distributionTokensComplete_ = "distributionTokensComplete";
	final public static String sourceQuery_distributionTokensComplete_ = "harvest.distributionTokensComplete";
	private Boolean distributionReachedLimit = null;
	final public static String distributionReachedLimit_ = "distributionReachedLimit";
	final public static String sourceQuery_distributionReachedLimit_ = "harvest.distributionReachedLimit";
	private LinkedHashMap<String, String> distributedStatus = null;
	final public static String distributedStatus_ = "distributedStatus";
	final public static String sourceQuery_distributedStatus_ = "harvest.distributedStatus";
	
	public Long getDoccount() {
		return doccount;
	}
	public void setDoccount(Long doccount) {
		this.doccount = doccount;
	}
	public Date getSynced() {
		return synced;
	}
	public void setSynced(Date synced) {
		this.synced = synced;
	}
	public void setHarvested(Date harvested) {
		this.harvested = harvested;
	}
	public Date getHarvested() {
		return harvested;
	}
	public void setHarvest_status(HarvestEnum harvest_status) {
		this.harvest_status = harvest_status;
	}
	public HarvestEnum getHarvest_status() {
		return harvest_status;
	}
	public void setHarvest_message(String harvest_message) {
		this.harvest_message = harvest_message;
	}
	public String getHarvest_message() {
		return harvest_message;
	}
	public void setLastHarvestedBy(String lastHarvestedBy) {
		this.lastHarvestedBy = lastHarvestedBy;
	}
	public String getLastHarvestedBy() {
		return lastHarvestedBy;
	}
	public void setDistributionTokensFree(Integer distributionTokensFree) {
		this.distributionTokensFree = distributionTokensFree;
	}
	public Integer getDistributionTokensFree() {
		return distributionTokensFree;
	}
	public void setDistributionTokensComplete(Integer distributionTokensComplete) {
		this.distributionTokensComplete = distributionTokensComplete;
	}
	public Integer getDistributionTokensComplete() {
		return distributionTokensComplete;
	}
	public void setDistributionReachedLimit(Boolean distributionReachedLimit) {
		this.distributionReachedLimit = distributionReachedLimit;
	}
	public Boolean getDistributionReachedLimit() {
		return distributionReachedLimit;
	}
	public void setDistributedStatus(LinkedHashMap<String, String> distributedStatus) {
		this.distributedStatus = distributedStatus;
	}
	public LinkedHashMap<String, String> getDistributedStatus() {
		return distributedStatus;
	}
	public Date getRealHarvested() {
		return realHarvested;
	}
	public void setRealHarvested(Date realHarvested) {
		this.realHarvested = realHarvested;
	}
	public Date getExtracted() {
		return extracted;
	}
	public void setExtracted(Date extracted) {
		this.extracted = extracted;
	}
}
