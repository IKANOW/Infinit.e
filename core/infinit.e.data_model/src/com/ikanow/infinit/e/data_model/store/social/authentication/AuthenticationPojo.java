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
package com.ikanow.infinit.e.data_model.store.social.authentication;

import java.util.Date;
import java.util.List;

import org.bson.types.ObjectId;

import com.google.gson.reflect.TypeToken;
import com.ikanow.infinit.e.data_model.InfiniteEnums.AccountStatus;
import com.ikanow.infinit.e.data_model.store.BaseDbPojo;

/**
 * Used to contain all information related to the account authentication
 * 
 * @author cmorgan
 *
 */
public class AuthenticationPojo extends BaseDbPojo  {
	// Standard static function for readability
	@SuppressWarnings("unchecked")
	static public TypeToken<List<AuthenticationPojo>> listType() { return new TypeToken<List<AuthenticationPojo>>(){}; }
	
	
	private ObjectId _id = null;
	private ObjectId profileId = null;
	private String username = null;
	private String password = null;
	private String accountType = null; // "admin", "admin-enabled", anything else
	private Date lastSudo = null; // (time when last request admin rights)
	private AccountStatus accountStatus = null;
	//private List<RolesPojo> roles = null;
	private Date created = null;
	private Date modified = null;
	private String WPUserID = null;
	private String apiKey = null;
	
	/** 
	  * Get the id
	  */
	public ObjectId getId() {
		return _id;
	}
	/** 
	  * Set the id
	  */
	public void setId(ObjectId id) {
		this._id = id;
	}
	/** 
	  * Get the profile id which maps to the person
	  */
	public ObjectId getProfileId() {
		return profileId;
	}
	/** 
	  * Set the profile id which maps to the person
	  */
	public void setProfileId(ObjectId profileId) {
		this.profileId = profileId;
	}
	/**
	 * Get the account holders username
	 */
	public String getUsername() {
		return username;
	}
	/**
	 * Set the account holders username
	 */
	public void setUsername(String username) {
		this.username = username;
	}
	/**
	 * The the account holders encrypted password
	 */
	public String getPassword() {
		return password;
	}
	/**
	 * Set the account holders encrypted password
	 */
	public void setPassword(String password) {
		this.password = password;
	}
	/**
	 * Get the account type
	 */
	public String getAccountType() {
		return accountType;
	}
	/** 
	  * Set the account type
	  */
	public void setAccountType(String accountType) {
		this.accountType = accountType;
	}
	/** 
	  * Get the created date
	  */
	public Date getCreated() {
		return created;
	}/** 
	  * Set the created date
	  */
	public void setCreated(Date created) {
		this.created = created;
	}
	/** 
	  * Get the modified date
	  */
	public Date getModified() {
		return modified;
	}
	/** 
	  * Set modified date
	  */
	public void setModified(Date modified) {
		this.modified = modified;
	}

	/**
	 * Class constructor with no arguments
	 */
	public AuthenticationPojo() {
	   	 // No args constructor
	}
	/**
	 * Class constructor
	 * 
	 * @param  username	the username of user
	 * @param  password the encrypted password of the user
	 */
	public AuthenticationPojo(String username, String password) {
		this.username = username;
		this.password = password;
	}
	public void setAccountStatus(AccountStatus accountStatus) {
		this.accountStatus = accountStatus;
	}
	public AccountStatus getAccountStatus() {
		return accountStatus;
	}
	public void setWPUserID(String wPUserID) {
		WPUserID = wPUserID;
	}
	public String getWPUserID() {
		return WPUserID;
	}
	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}
	public String getApiKey() {
		return apiKey;
	}
	public void setLastSudo(Date lastSudo) {
		this.lastSudo = lastSudo;
	}
	public Date getLastSudo() {
		return lastSudo;
	}
	
}
