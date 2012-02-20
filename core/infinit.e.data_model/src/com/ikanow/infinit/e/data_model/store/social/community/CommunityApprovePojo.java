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
	
	
}
