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
