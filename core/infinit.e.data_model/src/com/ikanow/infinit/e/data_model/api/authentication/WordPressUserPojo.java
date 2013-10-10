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

import java.util.List;

import com.ikanow.infinit.e.data_model.api.BaseApiPojo;

public class WordPressUserPojo extends BaseApiPojo
{
	private String WPUserID = null; // (optional, defaults to email[0] if not specified, in which case this must be unique)
	private String created = null; // (optional)
	private String modified = null; // (optional)
	private String firstname = null; // (optional, one of firstname/lastname must be specified)
	private String lastname = null; // (optional, one of firstname/lastname must be specified)
	private String phone = null; // (optional)
	private String mobile = null; // (optional)
	private String SubscriptionID = null; // (optional)
	private String SubscriptionTypeID = null; // (optional)
	private String SubscriptionStartDate = null; // (optional)
	private String SubscriptionEndDate = null; // (optional)
	private List<String> email = null; //(at least one must be specified)
	
	public void setWPUserID(String wPUserID) {
		WPUserID = wPUserID;
	}
	public String getWPUserID() {
		return WPUserID;
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
	public void setFirstname(String firstname) {
		this.firstname = firstname;
	}
	public String getFirstname() {
		return firstname;
	}
	public void setLastname(String lastname) {
		this.lastname = lastname;
	}
	public String getLastname() {
		return lastname;
	}
	public void setPhone(String phone) {
		this.phone = phone;
	}
	public String getPhone() {
		return phone;
	}
	public void setMobile(String mobile) {
		this.mobile = mobile;
	}
	public String getMobile() {
		return mobile;
	}
	public void setSubscriptionID(String subscriptionID) {
		SubscriptionID = subscriptionID;
	}
	public String getSubscriptionID() {
		return SubscriptionID;
	}
	public void setSubscriptionTypeID(String subscriptionTypeID) {
		SubscriptionTypeID = subscriptionTypeID;
	}
	public String getSubscriptionTypeID() {
		return SubscriptionTypeID;
	}
	public void setSubscriptionStartDate(String subscriptionStartDate) {
		SubscriptionStartDate = subscriptionStartDate;
	}
	public String getSubscriptionStartDate() {
		return SubscriptionStartDate;
	}
	public void setSubscriptionEndDate(String subscriptionEndDate) {
		SubscriptionEndDate = subscriptionEndDate;
	}
	public String getSubscriptionEndDate() {
		return SubscriptionEndDate;
	}
	public void setEmail(List<String> email) {
		this.email = email;
	}
	public List<String> getEmail() {
		return email;
	}
}
