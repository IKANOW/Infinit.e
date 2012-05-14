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
import java.util.List;

import com.google.gson.reflect.TypeToken;
import com.ikanow.infinit.e.data_model.InfiniteEnums.HarvestEnum;
import com.ikanow.infinit.e.data_model.store.BaseDbPojo;

public class SourceHarvestStatusPojo extends BaseDbPojo {
	// Standard static function for readability
	@SuppressWarnings("unchecked")
	static public TypeToken<List<SourceHarvestStatusPojo>> listType() { return new TypeToken<List<SourceHarvestStatusPojo>>(){}; }
 
	private Date harvested = null;
	final public static String harvested_ = "harvested";
	final public static String sourceQuery_harvested_ = "harvest.harvested";
	private Date synced = null;
	final public static String synced_ = "synced";
	final public static String sourceQuery_synced_ = "harvest.synced";
	private HarvestEnum harvest_status = null;
	final public static String harvest_status_ = "harvest_status";
	final public static String sourceQuery_harvest_status_ = "harvest.harvest_status";
	private String harvest_message = null;
	final public static String harvest_message_ = "harvest_message";
	final public static String sourceQuery_harvest_message_ = "harvest.harvest_message";
	private Long doccount = null;
	final public static String doccount_ = "doccount";
	final public static String sourceQuery_doccount_ = "harvest.doccount";
	
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
}
