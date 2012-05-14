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
package com.ikanow.infinit.e.data_model.store.social.person;

import java.util.Date;
import java.util.List;
import java.util.Map;
import org.bson.types.ObjectId;

import com.google.gson.reflect.TypeToken;
import com.ikanow.infinit.e.data_model.store.BaseDbPojo;
import com.ikanow.infinit.e.data_model.store.social.community.*;

/**
 * PersonPojo
 * @author cvitter
 */
public class PersonPojo extends BaseDbPojo
{
	// Standard static function for readability
	@SuppressWarnings("unchecked")
	static public TypeToken<List<PersonPojo>> listType() { return new TypeToken<List<PersonPojo>>(){}; }
	
	// Private class fields
	private ObjectId _id = null;
	private Date created = null;
	private Date modified = null;
	private String accountStatus = null;
	private String email = null;
	private String firstName = null;
	private String lastName = null;
	private String displayName = null;
	private String organization = null;
	private String title = null;
	private String location = null;
	private List<String> languages = null;
	private String biography = null;
	private String avatar = null;
	private String phone = null;
	private Map<String, PersonContactPojo> contacts = null;
	private Map<String, PersonLinkPojo> links = null;
	private List<PersonCommunityPojo> communities = null;
	private List<String> tags = null;
	
	//Wordpress fields
	private String WPUserID = null;
	private String SubscriptionID = null;
	private String SubscriptionTypeID = null;
	private String SubscriptionStartDate = null;
	private String SubscriptionEndDate = null;
	
	// Public getters and setters
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
	 * @param created the created to set
	 */
	public void setCreated(Date created) {
		this.created = created;
	}
	/**
	 * @return the created
	 */
	public Date getCreated() {
		return created;
	}
	/**
	 * @param modified the modified to set
	 */
	public void setModified(Date modified) {
		this.modified = modified;
	}
	/**
	 * @return the modified
	 */
	public Date getModified() {
		return modified;
	}
	/**
	 * @param accountStatus the accountStatus to set
	 */
	public void setAccountStatus(String accountStatus) {
		this.accountStatus = accountStatus;
	}
	/**
	 * @return the accountStatus
	 */
	public String getAccountStatus() {
		return accountStatus;
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
	 * @param firstname the firstname to set
	 */
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}
	/**
	 * @return the firstname
	 */
	public String getFirstName() {
		return firstName;
	}
	/**
	 * @param lastname the lastname to set
	 */
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
	/**
	 * @return the lastname
	 */
	public String getLastName() {
		return lastName;
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
	 * @param organization the organization to set
	 */
	public void setOrganization(String organization) {
		this.organization = organization;
	}
	/**
	 * @return the organization
	 */
	public String getOrganization() {
		return organization;
	}
	/**
	 * @param title the title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}
	/**
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}
	/**
	 * @param location the location to set
	 */
	public void setLocation(String location) {
		this.location = location;
	}
	/**
	 * @return the location
	 */
	public String getLocation() {
		return location;
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
	 * @param biography the biography to set
	 */
	public void setBiography(String biography) {
		this.biography = biography;
	}
	/**
	 * @return the biography
	 */
	public String getBiography() {
		return biography;
	}
	/**
	 * @param avatar the avatar to set
	 */
	public void setAvatar(String avatar) {
		this.avatar = avatar;
	}
	/**
	 * @return the avatar
	 */
	public String getAvatar() {
		return avatar;
	}
	/**
	 * @param phone the phone to set
	 */
	public void setPhone(String phone) {
		this.phone = phone;
	}
	/**
	 * @return the phone
	 */
	public String getPhone() {
		return phone;
	}
	/**
	 * @param contacts the contacts to set
	 */
	public void setContacts(Map<String, PersonContactPojo> contacts) {
		this.contacts = contacts;
	}
	/**
	 * @return the contacts
	 */
	public Map<String, PersonContactPojo> getContacts() {
		return contacts;
	}
	/**
	 * @param links the links to set
	 */
	public void setLinks(Map<String, PersonLinkPojo> links) {
		this.links = links;
	}
	/**
	 * @return the links
	 */
	public Map<String, PersonLinkPojo> getLinks() {
		return links;
	}
	/**
	 * @param communities the communities to set
	 */
	public void setCommunities(List<PersonCommunityPojo> communities) {
		this.communities = communities;
	}
	/**
	 * @return the communities
	 */
	public List<PersonCommunityPojo> getCommunities() {
		return communities;
	}
	
	public boolean isInCommunity(String communityId)
	{
		for ( PersonCommunityPojo pcp : communities)
		{
			if ( pcp.get_id().toString().equals(communityId) )
				return true;
		}
		return false;
	}
	
	/**
	 * adds a community to the user's list of communities
	 * @param cp
	 */
	public void addCommunity(CommunityPojo cp) 
	{
		if ( !isInCommunity(cp.getId().toString()))
		{
			PersonCommunityPojo pcp = new PersonCommunityPojo();
			pcp.set_id(cp.getId());
			pcp.setName(cp.getName());						
			communities.add(pcp);
		}		
	}
	
	/**
	 * removes a community from this users lsit of communities
	 * 
	 * @param cp
	 */
	public void removeCommunity(CommunityPojo cp) 
	{
		for ( PersonCommunityPojo pcp : communities)
		{
			if ( pcp.get_id().equals(cp.getId()) )
			{
				communities.remove(pcp);
				break;
			}
		}		
	}
	/**
	 * @param tags the tags to set
	 */
	public void setTags(List<String> tags) {
		this.tags = tags;
	}
	/**
	 * @return the tags
	 */
	public List<String> getTags() {
		return tags;
	}
	
	//////////////////////////////////////////////////WORDPRESS FIELDS/////////////////////////////////
	public void setWPUserID(String wPUserID) {
		WPUserID = wPUserID;
	}
	public String getWPUserID() {
		return WPUserID;
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
	////////////////////////////////////////////////// END WORDPRESS FIELDS/////////////////////////////////
	
}
