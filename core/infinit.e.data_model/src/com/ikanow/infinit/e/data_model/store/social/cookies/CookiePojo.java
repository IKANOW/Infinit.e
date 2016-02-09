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
package com.ikanow.infinit.e.data_model.store.social.cookies;

import java.util.Date;
import java.util.List;

import org.bson.types.ObjectId;

import com.google.gson.reflect.TypeToken;
import com.ikanow.infinit.e.data_model.store.BaseDbPojo;

public class CookiePojo extends BaseDbPojo
{
	// Standard static function for readability
	@SuppressWarnings("unchecked")
	static public TypeToken<List<CookiePojo>> listType() { return new TypeToken<List<CookiePojo>>(){}; }
	
	private ObjectId _id = null;
	private ObjectId profileId = null;
	private ObjectId cookieId = null;
	private Date startDate = null;
	private Date lastActivity = null;
	private String apiKey = null;
	
	public static String _id_ = "_id";
	public static String profileId_ = "profileId";
	public static String cookieId_ = "cookieId";
	public static String startDate_ = "startDate";
	public static String lastActivity_ = "lastActivity";
	public static String apiKey_ = "apiKey";
	
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
	public void setCookieId(ObjectId cookieId) {
		this.cookieId = cookieId;
	}
	public ObjectId getCookieId() {
		return cookieId;
	}
	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}
	public Date getStartDate() {
		return startDate;
	}
	public void setLastActivity(Date lastActivity) {
		this.lastActivity = lastActivity;
	}
	public Date getLastActivity() {
		if (null == lastActivity) {
			this.updateActivity();
		}
		return lastActivity;
	}
	public void updateActivity() {
		this.lastActivity = new Date();
	}
	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}
	public String getApiKey() {
		return apiKey;
	}
	
}
