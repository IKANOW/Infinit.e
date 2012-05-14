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
package com.ikanow.infinit.e.data_model.store.social.gui;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bson.types.ObjectId;

import com.google.gson.reflect.TypeToken;
import com.ikanow.infinit.e.data_model.store.BaseDbPojo;

public class FavoriteUIModulePojo extends BaseDbPojo
{
	// Standard static function for readability
	@SuppressWarnings("unchecked")
	static public TypeToken<List<FavoriteUIModulePojo>> listType() { return new TypeToken<List<FavoriteUIModulePojo>>(){}; }
	
	private ObjectId _id = null;
	private ObjectId profileId = null;
	private List<ObjectId> quickModuleIds = null;
	private Date timestamp = null;
	
	public void set_id(ObjectId _id) {
		this._id = _id;
	}
	public ObjectId get_id() {
		return _id;
	}
	public void setProfileId(ObjectId profileId) {
		this.profileId = profileId;
	}
	public ObjectId getProfileId() {
		return profileId;
	}
	public void addQuickModules(String moduleIdStr)
	{
		if (null == this.quickModuleIds) {
			quickModuleIds = new ArrayList<ObjectId>();
		}
		this.quickModuleIds.add(new ObjectId(moduleIdStr));
	}
	public void clearQuickModules()
	{
		if (null == this.quickModuleIds) {
			quickModuleIds = new ArrayList<ObjectId>();
		}
		this.quickModuleIds.clear();
	}
	public void setQuickModules(List<ObjectId> quickModules) {
		this.quickModuleIds = quickModules;
	}
	public void setQuickModules(String[] moduleIds) {
		if (null == this.quickModuleIds) {
			quickModuleIds = new ArrayList<ObjectId>(moduleIds.length);
		}
		else {
			quickModuleIds.clear();
		}
		for (String s: moduleIds) {
			this.quickModuleIds.add(new ObjectId(s));
		}
	}
	public List<ObjectId> getQuickModules() {
		return quickModuleIds;
	}
	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}
	public Date getTimestamp() {
		return timestamp;
	}
}
