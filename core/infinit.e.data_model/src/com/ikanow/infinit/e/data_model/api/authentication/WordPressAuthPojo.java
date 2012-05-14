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

import com.ikanow.infinit.e.data_model.api.BaseApiPojo;

public class WordPressAuthPojo extends BaseApiPojo
{
	private String WPUserID = null; // (optional)
	@SuppressWarnings("unused")
	private String username = null; // (ignored, WordPressUserPojo.email[0] always used - retained for backwards compatibility)
	private String password = null; // (mandatory)
	private String accountType = null; // (optional, defaults to "user")
	private String created = null; // (optional)
	private String modified = null; // (optional)
	
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
}
