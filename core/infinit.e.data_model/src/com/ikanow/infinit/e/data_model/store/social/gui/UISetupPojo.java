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
