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

//import java.util.Date;
import java.util.List;
import java.util.Set;

import org.bson.types.ObjectId;

import com.ikanow.infinit.e.data_model.store.BaseDbPojo;

/**
 * Class used to establish the community member information in the environment
 * @author cvitter
 */
public class CommunityMemberPojo extends BaseDbPojo {

	/** 
	  * Private Class Variables
	  */
	final public static String _id_ = "_id";
	private ObjectId _id = null;
	public enum MemberType { user, user_group };
	private MemberType type = null; // (defaults to user)
	private String email = null;
	private String displayName = null;
	private String userType = null; // memberUserType: "Member", "Content_Publisher", "Moderator", "Owner"
	private String userStatus = null; // memberUserStatus
	private List<String> languages = null;
	private Set<CommunityMemberUserAttributePojo> userAttributes = null;
	private Set<CommunityMemberContactPojo> contacts = null;
	private Set<CommunityMemberLinkPojo> links = null;
			
	/**
	 * @param _id the _id to set
	 */
	public void set_id(ObjectId _id) {
		this._id = _id;
	}
	/**
	 * @return the _id
	 */
	public ObjectId get_id() {
		return _id;
	}
	
	/**
	 * @param email the email to set
	 */
	public void setEmail(String email) {
		this.email = email;
	}
	/**
	 * @return the email
	 */
	public String getEmail() {
		return email;
	}
	
	/**
	 * @param displayName the displayName to set
	 */
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	/**
	 * @return the displayName
	 */
	public String getDisplayName() {
		return displayName;
	}
	
	/**
	 * @param userType the userType to set
	 */
	public void setUserType(String userType) {
		this.userType = userType;
	}
	/**
	 * @return the userType
	 */
	public String getUserType() {
		return userType;
	}
	
	/**
	 * @param userStatus the userStatus to set
	 */
	public void setUserStatus(String userStatus) {
		this.userStatus = userStatus;
	}
	/**
	 * @return the userStatus
	 */
	public String getUserStatus() {
		return userStatus;
	}
	
	
	
	/**
	 * @param languages the languages to set
	 */
	public void setLanguages(List<String> languages) {
		this.languages = languages;
	}
	/**
	 * @return the languages
	 */
	public List<String> getLanguages() {
		return languages;
	}

	/**
	 * @param userAttributes the userAttributes to set
	 */
	public void setUserAttributes(Set<CommunityMemberUserAttributePojo> userAttributes) {
		this.userAttributes = userAttributes;
	}
	/**
	 * @return the userAttributes
	 */
	public Set<CommunityMemberUserAttributePojo> getUserAttributes() {
		return userAttributes;
	}
	
	/**
	 * @param contacts the contacts to set
	 */
	public void setContacts(Set<CommunityMemberContactPojo> contacts) {
		this.contacts = contacts;
	}
	/**
	 * @return the contacts
	 */
	public Set<CommunityMemberContactPojo> getContacts() {
		return contacts;
	}
	
	/**
	 * @param links the links to set
	 */
	public void setLinks(Set<CommunityMemberLinkPojo> links) {
		this.links = links;
	}
	/**
	 * @return the links
	 */
	public Set<CommunityMemberLinkPojo> getLinks() {
		return links;
	}
	public MemberType getType() {
		return type == null ? MemberType.user : type;
	}
	public void setType(MemberType type) {
		this.type = type;
	}
	
}
