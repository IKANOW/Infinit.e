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
package com.ikanow.infinit.e.data_model.store.social.community;

import java.util.Date;

import org.bson.types.ObjectId;

import com.ikanow.infinit.e.data_model.store.BaseDbPojo;

public class CommunityApprovePojo extends BaseDbPojo
{
	private ObjectId _id = null;
	private String requesterId = null;
	private String type = "invite";
	private String communityId = null;
	private String personId = null;
	private Date issueDate = null;
	private String sourceId = null;
	
	public void set_id(ObjectId _id) {
		this._id = _id;
	}
	public ObjectId get_id() {
		return _id;
	}
	public void setRequesterId(String requesterId) {
		this.requesterId = requesterId;
	}
	public String getRequesterId() {
		return requesterId;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getType() {
		return type;
	}
	public void setCommunityId(String communityId) {
		this.communityId = communityId;
	}
	public String getCommunityId() {
		return communityId;
	}
	public void setPersonId(String personId) {
		this.personId = personId;
	}
	public String getPersonId() {
		return personId;
	}
	public void setIssueDate(Date issueDate) {
		this.issueDate = issueDate;
	}
	public Date getIssueDate() {
		return issueDate;
	}
	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}
	public String getSourceId() {
		return sourceId;
	}
	
	
}
