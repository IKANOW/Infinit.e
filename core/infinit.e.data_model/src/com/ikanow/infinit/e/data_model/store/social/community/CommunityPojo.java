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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.map.HashedMap;
import org.bson.types.ObjectId;

import com.mongodb.BasicDBObject;
import com.google.gson.reflect.TypeToken;
import com.ikanow.infinit.e.data_model.store.BaseDbPojo;
import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.MongoDbManager;
import com.ikanow.infinit.e.data_model.store.document.DocCountPojo;
import com.ikanow.infinit.e.data_model.store.social.community.CommunityMemberPojo.MemberType;
import com.ikanow.infinit.e.data_model.store.social.person.*;;

/**
 * Class used to establish the community information in the environment
 * @author cvitter
 */
public class CommunityPojo extends BaseDbPojo
{
	

	// Standard static function for readability
	@SuppressWarnings("unchecked")
	static public TypeToken<List<CommunityPojo>> listType() { return new TypeToken<List<CommunityPojo>>(){}; }
	
	
	// (NOTE CAN'T CURRENTLY USE THIS AS QUERY POJO BECAUSE IT HAS DEFAULT PARAMS)
	/** 
	  * Private Class Variables
	  */
	private ObjectId _id = null;
	final public static String _id_ = "_id";
	public enum CommunityType { user, data };
	private CommunityType type = null; // (defaults to combined)
	private Date created = null;
	private Date modified = null;
	private String name = null;
	private String description = null;
	private Boolean isSystemCommunity = false;
	private ObjectId parentId = null;
	private List<ObjectId> parentTree = null;
	private String parentName = null;
	private Boolean isPersonalCommunity = false;
	private List<String> tags = null;
	private Map<String, CommunityAttributePojo> communityAttributes = null;
	private Map<String, CommunityUserAttributePojo> userAttributes = null;
	private ObjectId ownerId = null;
	private String communityStatus = "active"; //communityStatusEnum defaults to active
	private String ownerDisplayName = null;
	final public static String members_ = "members";
	private Set<CommunityMemberPojo> members = null;
	private Set<ObjectId> children = null;
	private DocCountPojo documentInfo = null; // (added by certain API calls, stored separately in the DB, under doc_metadata.doc_counts)

	public enum communityStatusEnum
	{
		ACTIVE {
		    public String toString() {
		        return "active";
		    }
		},
		DISABLED {
		    public String toString() {
		        return "disabled";
		    }
		},
		PENDING {
		    public String toString() {
		        return "pending";
		    }
		}
	}
	
	/** 
	  * Get the id
	  * 
	  */
	public ObjectId getId() {
		return _id;
	}
	
	/** 
	  * Set the id
	  * 
	  */
	public void setId(ObjectId _id) {
		this._id = _id;
	}
	
	public void setCommunityStatus(String communityStatus) {
		this.communityStatus = communityStatus;
	}
	
	public String getCommunityStatus() {
		return this.communityStatus;
	}
	
	/** 
	  * Get the created date
	  */
	public Date getCreated() {
		return created;
	}
	
	/** 
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
	  * Get the community name
	  */
	public String getName() {
		return name;
	}
	
	/** 
	  * Set the community name
	  */
	public void setName(String name) {
		this.name = name;
	}
	
	/** 
	  * Get the community description
	  */
	public String getDescription() {
		return description;
	}
	
	/** 
	  * Set the community description
	  */
	public void setDescription(String description) {
		this.description = description;
	}
	
	/**
	 * @param isSystemCommunity the isSystemCommunity to set
	 */
	public void setIsSystemCommunity(Boolean isSystemCommunity) {
		this.isSystemCommunity = isSystemCommunity;
	}

	/**
	 * @return the isSystemCommunity
	 */
	public Boolean getIsSystemCommunity() {
		return isSystemCommunity;
	}

	/** 
	  * Get the community parent id
	  */
	public ObjectId getParentId() {
		return parentId;
	}
	
	/** 
	  * Set the community parent id
	  */
	public void setParentId(ObjectId parentId) {
		this.parentId = parentId;
	}
	
	/** 
	  * Get the community parent Tree
	  */
	public List<ObjectId> getParentTree() {
		return parentTree;
	}
	
	/** 
	  * Set the community parent Tree
	  */
	public void setParentTree(List<ObjectId> parentTree) {
		this.parentTree = parentTree;
	}
	
	/** 
	  * Get the community parent named
	  */
	public String getParentName() {
		return parentName;
	}
	
	/** 
	  * Set the community parent name
	  */
	public void setParentName(String parentName) {
		this.parentName = parentName;
	}
	
	/** 
	  * Set the is personal community true/false
	  */
	public void setIsPersonalCommunity(Boolean isPersonalCommunity) {
		this.isPersonalCommunity = isPersonalCommunity;
	}

	/** 
	  * Get the is personal community value
	  */
	public Boolean getIsPersonalCommunity() {
		return isPersonalCommunity;
	}

	/** 
	  * Set the list of community tags
	  */
	public void setTags(List<String> tags) {
		this.tags = tags;
	}

	/** 
	  * Get the list of community tags
	  */
	public List<String> getTags() {
		return tags;
	}
		
	/**
	 * @param communityAttributes the communityAttributes to set
	 */
	public void setCommunityAttributes(Map<String, CommunityAttributePojo> communityAttributes) {
		this.communityAttributes = communityAttributes;
	}

	/**
	 * @return the communityAttributes
	 */
	public Map<String, CommunityAttributePojo> getCommunityAttributes() {
		return communityAttributes;
	}

	/**
	 * @param communityUserAttribute the communityUserAttribute to set
	 */
	public void setCommunityUserAttribute(Map<String, CommunityUserAttributePojo> communityUserAttribute) {
		this.userAttributes = communityUserAttribute;
	}

	/**
	 * @return the communityUserAttribute
	 */
	public Map<String, CommunityUserAttributePojo> getCommunityUserAttribute() {
		return userAttributes;
	}

	/** 
	  * Get the community owner id
	  */
	public ObjectId getOwnerId() {
		return ownerId;
	}
	
	/** 
	  * Set the community owner id
	  */
	public void setOwnerId(ObjectId ownerId) {
		this.ownerId = ownerId;
	}
	
	/** 
	  * Get the community owner display name
	  */
	public String getOwnerDisplayName() {
		return ownerDisplayName;
	}
	
	/** 
	  * Set the community owner display name
	  */
	public void setOwnerDisplayName(String ownerDisplayName) {
		this.ownerDisplayName = ownerDisplayName;
	}

	/*
	 * Set the community members
	 */
	public void setMembers(Set<CommunityMemberPojo> members) {
		this.members = members;
	}
	
	/*
	 * Get the community members
	 */
	public Set<CommunityMemberPojo> getMembers() {
		return members;
	}
	
	/**
	 * Check a user is a member of this community
	 */
	public boolean isMember(ObjectId userID)
	{
		for ( CommunityMemberPojo cmp : members)
			if ( cmp.get_id().equals(userID) )
				return true;
		return false;
	}

	/**
	 * Check a user is the owner of this community
	 * 
	 * @param objectId
	 * @return
	 */
	public boolean isOwner(ObjectId userID) 
	{
		if ( isPersonalCommunity ) {
			return userID.equals(_id);
		}
		else {
			return userID.equals(ownerId);
		}
	}
	


	/**
	 * addMember
	 * Attempts to add a user to community
	 * @param pp
	 * @throws Exception 
	 */
	public void addMember(ObjectId personOrUserGroupId, PersonPojo user, CommunityPojo userGroup) throws Exception 
	{
		addMember(personOrUserGroupId, user, userGroup, false);		
	}
	
	
	/**
	 * addMember
	 * Attempts to add a user to community
	 * 
	 * If the person is already a member and isInvite is false, will set
	 * the person to active
	 * 
	 * @param user
	 * @param isInvite
	 * @throws Exception 
	 */
	public void addMember(ObjectId personOrUserGroupId, PersonPojo user, CommunityPojo userGroup, boolean isInvite) throws Exception 
	{
		CommunityMemberPojo old_cmp = null;
		for ( CommunityMemberPojo cmp : this.getMembers() )
		{
			if ( cmp.get_id().equals(personOrUserGroupId) )
			{
				//found the user
				old_cmp = cmp;			
				break;
			}
		}
		// If the person is not already a member of the community or pending
		if ( old_cmp == null || old_cmp.getUserStatus().equals("pending") )
		{			
			// Create the new member object
			CommunityMemberPojo cmp = new CommunityMemberPojo();
			cmp.set_id(personOrUserGroupId);
			if (null != user) {
				cmp.setEmail(user.getEmail());
				cmp.setDisplayName(user.getDisplayName());
				cmp.setType(MemberType.user);
			}
			else {
				cmp.setDisplayName(userGroup.getName());
				cmp.setType(MemberType.user_group);
			}
			cmp.setUserType("content_publisher");
			
			if (isInvite)
			{
				cmp.setUserStatus("pending");
			}
			else
			{
				cmp.setUserStatus("active");
				//if user gets set to active and this is datagroup
				if ( this.type == CommunityType.data )
				{
					//add dg to user/usergroup
					addDatagroupToUserOrUserGroup(this, user, userGroup);
				}
			}

			// Set the userAttributes based on default
			Set<CommunityMemberUserAttributePojo> cmua = new HashSet<CommunityMemberUserAttributePojo>();
			
			Map<String, CommunityUserAttributePojo> cua = this.getCommunityUserAttribute();

			Iterator<Map.Entry<String, CommunityUserAttributePojo>> it = cua.entrySet().iterator();
			while (it.hasNext())
			{
				CommunityMemberUserAttributePojo c = new CommunityMemberUserAttributePojo();
				Map.Entry<String, CommunityUserAttributePojo> pair = it.next();
				c.setType(pair.getKey().toString());
				CommunityUserAttributePojo v = (CommunityUserAttributePojo)pair.getValue();
				c.setValue(v.getDefaultValue());
				cmua.add(c);
			}
			cmp.setUserAttributes(cmua);

			// Get Person data to added to member record

			if (null != user) {
				if (user.getContacts() != null)
				{
					// Set contacts from person record
					Set<CommunityMemberContactPojo> contacts = new HashSet<CommunityMemberContactPojo>();
					Map<String, PersonContactPojo> pcp = user.getContacts();
					Iterator<Map.Entry<String, PersonContactPojo>> it2 = pcp.entrySet().iterator();
					while (it.hasNext())
					{
						CommunityMemberContactPojo c = new CommunityMemberContactPojo();
						Map.Entry<String, PersonContactPojo> pair = it2.next();
						c.setType(pair.getKey().toString());
						PersonContactPojo v = (PersonContactPojo)pair.getValue();
						c.setValue(v.getValue());
						contacts.add(c);
					}
					cmp.setContacts(contacts);
				}
	
				// Set links from person record
				if (user.getLinks() != null)
				{
					// Set contacts from person record
					Set<CommunityMemberLinkPojo> links = new HashSet<CommunityMemberLinkPojo>();
					Map<String, PersonLinkPojo> plp = user.getLinks();
					Iterator<Map.Entry<String, PersonLinkPojo>> it3 = plp.entrySet().iterator();
					while (it.hasNext())
					{
						CommunityMemberLinkPojo c = new CommunityMemberLinkPojo();
						Map.Entry<String, PersonLinkPojo> pair = it3.next();
						c.setTitle(pair.getKey().toString());
						PersonLinkPojo v = (PersonLinkPojo)pair.getValue();
						c.setUrl(v.getUrl());
						links.add(c);
					}
					cmp.setLinks(links);
				}
			}
			
			//remove the old entry if it exists
			if ( old_cmp != null )
			{
				members.remove(old_cmp);
			}
			//TESTED removes the actually entry
			members.add(cmp);				
		}		
	}
	
	

	/**
	 * Adds datagroup and reason to usergroup users or user
	 * 
	 * @param communityPojo
	 * @param user
	 * @param userGroup
	 * @throws Exception 
	 */
	public static void addDatagroupToUserOrUserGroup(CommunityPojo dataGroup,
			PersonPojo user, CommunityPojo userGroup) throws Exception 
	{
		if ( dataGroup.type == CommunityType.data )
		{
			List<PersonPojo> usersToUpdate = new ArrayList<PersonPojo>();
			if ( user != null )
				usersToUpdate.add(user);
			if ( userGroup != null )
			{
				for ( CommunityMemberPojo member : userGroup.getMembers() )
				{
					PersonPojo person = PersonPojo.fromDb( DbManager.getSocial().getPerson().findOne(new BasicDBObject(PersonPojo._id_, member.get_id())), PersonPojo.class);
					if ( person != null )
					{
						usersToUpdate.add(person);
					}
				}			
			}
			
			
			for ( PersonPojo person : usersToUpdate )
			{
				//add datagroup to user.communities
				person.addCommunity(dataGroup);
				
				//add usergroup to user.datagroup_reason
				Map<String, Set<String>> datagroup_reason = person.getDatagroupReason();
				if ( datagroup_reason == null )
					datagroup_reason = new HashedMap<String, Set<String>>();
				
				if ( !datagroup_reason.containsKey(dataGroup.getId().toString()) )
					datagroup_reason.put(dataGroup.getId().toString(), new HashSet<String>());
				
				if ( user != null )
					datagroup_reason.get(dataGroup.getId().toString()).add(user.get_id().toString());
				if ( userGroup != null )
					datagroup_reason.get(dataGroup.getId().toString()).add(userGroup.getId().toString());
				
				
				person.setDatagroupReason(datagroup_reason);
				//TODO don't use save, use $set and such
				DbManager.getSocial().getPerson().update(new BasicDBObject(PersonPojo._id_, person.get_id()), person.toDb());
			}
		}
		else
		{
			throw new Exception("Community was not a datagroup, can't update user/usergroup");
		}
	}
	
	/**
	 * Removes the usergroup from user.reason for all it's members
	 * 
	 * @param dataGroup
	 * @throws Exception 
	 */
	public static void removeDatagroupFromUserOrUsergroup( PersonPojo userBeingRemoved, CommunityPojo communityBeingRemoved, CommunityPojo communityLeaving) throws Exception
	{
		if ( userBeingRemoved != null || communityBeingRemoved.type == CommunityType.user )
		{
			//if comm being removed is a user group, find all datagroups this usergroup is a member of
			//and remove them from all user.reason
			ObjectId reason = null;
			List<PersonPojo> usersToUpdate = new ArrayList<PersonPojo>();
			if ( userBeingRemoved != null )
			{
				reason = userBeingRemoved.get_id();
				usersToUpdate.add(userBeingRemoved);
			}
			else
			{
				reason = communityBeingRemoved.getId();
				for ( CommunityMemberPojo member : communityBeingRemoved.getMembers() )
				{
					PersonPojo person = PersonPojo.fromDb( DbManager.getSocial().getPerson().findOne(new BasicDBObject(PersonPojo._id_, member.get_id())), PersonPojo.class);
					if ( person != null )
					{
						usersToUpdate.add(person);
					}
				}
			}
			
			List<CommunityPojo> data_groups = null;
			//if we aren't given a community we are leaving, assume we are being deleted and removed from all comms
			if ( communityLeaving == null )
				data_groups = CommunityPojo.listFromDb( MongoDbManager.getSocial().getCommunity().find(new BasicDBObject(CommunityPojo.members_+ "." + CommunityMemberPojo._id_, reason)), CommunityPojo.listType());
			else //assume we are only leaving the given community
				data_groups = Arrays.asList(communityLeaving);
			
			for ( CommunityPojo data_group : data_groups)
				removeDatagroupReason(data_group, usersToUpdate, reason.toString());
		}
		else
		{
			//NOTE: this should never be called w/ communityLeaving because a datagroup can't be a member of any other community so it has
			//no community to leave, we assume it's being deleted
			
			//if comm being removed is a datagroup, find all users with this datagroup as a reason and remove it			
			List<PersonPojo> usersToUpdate = PersonPojo.listFromDb(MongoDbManager.getSocial().getPerson().find(new BasicDBObject(PersonPojo.communities_ + "." + PersonCommunityPojo._id_, communityBeingRemoved.getId())), PersonPojo.listType());
			removeDatagroupReason(communityBeingRemoved, usersToUpdate, null);
		}						
	}
	
	/**
	 * Removes the datagroup as a reason from all the given users.
	 * 
	 * @param datagroup
	 * @param usersToUpdate
	 */
	public static void removeDatagroupReason(CommunityPojo datagroup , List<PersonPojo> usersToUpdate, String reason_id)
	{
		if ( datagroup.type == CommunityType.data )
		{
			for ( PersonPojo person : usersToUpdate )
			{
				//remove usergroup/user from reason
				Map<String, Set<String>> datagroup_reason = person.getDatagroupReason();
				if ( datagroup_reason == null )
					datagroup_reason = new HashMap<String, Set<String>>();
				Set<String> reasons = datagroup_reason.get(datagroup.getId().toString());
				if ( reasons != null )
				{
					if ( reason_id != null )
					{
						//if we were given a reason, only remove that
						reasons.remove(reason_id);
					}
					else
					{
						//if reason_id is null, clear all entries (usually for when a datagroup is deleted)
						reasons.clear();
					}
					
					//if reason is now empty, remove from communities
					if ( reasons.isEmpty() )
					{
						person.removeCommunity(datagroup);
						datagroup_reason.remove(datagroup.getId());
					}
					
					person.setDatagroupReason(datagroup_reason);
					//TODO don't use save, use $set and such
					DbManager.getSocial().getPerson().update(new BasicDBObject(PersonPojo._id_, person.get_id()), person.toDb());
				}
			}
		}
	}

	/**
	 * removeMember
	 * Attempts to remove a user from this community
	 * @param personId
	 */
	public void removeMember(ObjectId personId) 
	{
		for ( CommunityMemberPojo cmp : members)
		{
			if ( cmp.get_id().equals(personId) )
			{
				//found member, remove
				members.remove(cmp);
				break;
			}
		}		
	}
	
	
	/**
	 * getOwner
	 * @return
	 */
	public CommunityMemberPojo getOwner()
	{
		for ( CommunityMemberPojo cmp : members)
		{
			if ( cmp.get_id().equals(ownerId) )
			{
				//found owner, return
				return cmp;
			}
		}	
		return null; //hopefully we find an owner
	}
	
	
	/**
	 * updateMemberStatus
	 * @param personId
	 * @param userStatus
	 * @return
	 */
	public boolean updateMemberStatus(String personId, String userStatus)
	{
		ObjectId userID = new ObjectId(personId);
		//verified user, update status
		for ( CommunityMemberPojo cmp : members)
		{							
			if ( cmp.get_id().equals(userID) )
			{
				//if user is now active, add a member
				cmp.setUserStatus(userStatus);
				return true;
			}
		}
		return false;
	}
	
	
	/**
	 * createCommunityApprove
	 * @param personId
	 * @param communityId
	 * @param requestType
	 * @param requesterId
	 * @return
	 */
	public CommunityApprovePojo createCommunityApprove(ObjectId randomId, String personId, String communityId,
			String requestType, String requesterId) 
	{
		CommunityApprovePojo cap = new CommunityApprovePojo();
		cap.set_id(randomId);
		cap.setCommunityId(communityId);
		cap.setIssueDate(new Date());
		cap.setPersonId(personId);
		cap.setRequesterId(requesterId);
		cap.setType(requestType);		
		return cap;
	}

	public void setChildren(Set<ObjectId> children) {
		this.children = children;
	}

	public Set<ObjectId> getChildren() {
		return children;
	}

	public DocCountPojo getDocumentInfo() {
		return documentInfo;
	}

	public void setDocumentInfo(DocCountPojo documentInfo) {
		this.documentInfo = documentInfo;
	}

	public CommunityType getType() {
		return type == null ? CommunityType.data : type;
	}

	public void setType(CommunityType type) {
		this.type = type;
	}
}
