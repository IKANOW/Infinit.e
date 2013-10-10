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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bson.types.ObjectId;

import com.google.gson.reflect.TypeToken;
import com.ikanow.infinit.e.data_model.store.BaseDbPojo;

public class UISetupPojo extends BaseDbPojo
{
	// Standard static function for readability
	@SuppressWarnings("unchecked")
	static public TypeToken<List<UISetupPojo>> listType() { return new TypeToken<List<UISetupPojo>>(){}; }
	
	private ObjectId profileId;
	private Set<ObjectId> communityIds;
	private String queryString = null;
	private List<WidgetPojo> openModules = null;
	
	public Set<ObjectId> getCommunityIds() {
		return communityIds;
	}

	public void setCommunityIds(String communityIdStrList) {
		if (null != communityIdStrList) {
			String[] communityIdStrs = communityIdStrList.split("\\s*,\\s*");
			if (null == communityIds) {
				communityIds = new HashSet<ObjectId>();
			}
			else {
				communityIds.clear();
			}
			for (String communityIdStr: communityIdStrs) {
				try {
					communityIds.add(new ObjectId(communityIdStr));
				}
				catch (Exception e) {} // Just ignore that community
			}
		}
	}
	
	public void setCommunityIds(Set<ObjectId> communityIds) {
		this.communityIds = communityIds;
	}

	public void setProfileID(ObjectId profileID) {
		this.profileId = profileID;
	}

	public ObjectId getProfileID() {
		return profileId;
	}
	public void addWidget(WidgetPojo widget)
	{
		if (null == openModules) {
			openModules = new ArrayList<WidgetPojo>();
		}
		openModules.add(widget);
	}
	public List<WidgetPojo> getWidgets()
	{
		return openModules;
	}
	public void addWidgets(List<WidgetPojo> modules)
	{
		openModules = modules;
	}

	public void setQueryString(String queryString) {
		if ( queryString.equals("null"))
			this.queryString = null;
		else
			this.queryString = queryString;
	}

	public String getQueryString() {
		return queryString;
	}
	
}
