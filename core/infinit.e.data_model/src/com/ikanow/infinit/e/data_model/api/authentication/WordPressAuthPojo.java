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
package com.ikanow.infinit.e.data_model.api.authentication;

import com.ikanow.infinit.e.data_model.InfiniteEnums.AccountStatus;
import com.ikanow.infinit.e.data_model.api.BaseApiPojo;

public class WordPressAuthPojo extends BaseApiPojo
{
	private String WPUserID = null; // (optional)
	private String username = null; // (usually ignored, WordPressUserPojo.email[0] always used - retained for backwards compatibility and special case below)
									// (Special case: for admins to change their username, must specify this as the old username, WordPressUserPojo as the new one)
	private String password = null; // (mandatory)
	private String accountType = null; // (optional, defaults to "user")
	private String created = null; // (optional)
	private String modified = null; // (optional)
	
	private AccountStatus accountStatus = null;	//(optional, defaults to "active")
	
	private String apiKey = null; // (optional, allows API key access if enabled)
	
	public void setWPUserID(String wPUserID) {
		WPUserID = wPUserID;
	}
	public String getWPUserID() {
		return WPUserID;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getPassword() {
		return password;
	}
	public void setAccountType(String accountType) {
		this.accountType = accountType;
	}
	public String getAccountType() {
		return accountType;
	}
	public AccountStatus getAccountStatus() {
		return accountStatus;
	}
	public void setAccountStatus(AccountStatus accountStatus) {
		this.accountStatus = accountStatus;
	}
	public void setCreated(String created) {
		this.created = created;
	}
	public String getCreated() {
		return created;
	}
	public void setModified(String modified) {
		this.modified = modified;
	}
	public String getModified() {
		return modified;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getUsername() {
		return username;
	}
	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}
	public String getApiKey() {
		return apiKey;
	}
}
