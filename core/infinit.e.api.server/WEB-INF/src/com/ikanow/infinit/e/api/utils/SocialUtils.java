package com.ikanow.infinit.e.api.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.bson.types.ObjectId;

import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.MongoDbManager;
import com.ikanow.infinit.e.data_model.store.social.community.CommunityMemberPojo;
import com.ikanow.infinit.e.data_model.store.social.community.CommunityPojo;
import com.ikanow.infinit.e.data_model.store.social.person.PersonCommunityPojo;
import com.ikanow.infinit.e.data_model.store.social.person.PersonPojo;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;

public class SocialUtils 
{
	private static final Logger logger = Logger.getLogger(SocialUtils.class);
	
	/**
	 * isOwnerOrModerator
	 * Returns true if personid is the owner or a moderator of the community
	 * @param communityIdStr
	 * @param personIdStr
	 * @return
	 */
	public static boolean isDataAllowed(String communityIdStr) {
		BasicDBObject query = new BasicDBObject("communityType", CommunityPojo.CommunityType.user.toString());
		BasicDBObject fields = new BasicDBObject();
		// ie can't be user group
		return null == DbManager.getSocial().getCommunity().findOne(query, fields);
	}
	public static boolean isOwnerOrModeratorOrContentPublisher(String communityIdStr, String personIdStr) 
	{
		return isOwnerOrModerator(communityIdStr, personIdStr, true);
	}
	
	public static boolean isOwnerOrModerator(String communityIdStr, String personIdStr) 
	{
		return isOwnerOrModerator(communityIdStr, personIdStr, false);
	}
	
	private static boolean isOwnerOrModerator(String communityIdStr, String personIdStr, boolean bAllowContentPublisher) 
	{		
		CommunityPojo community = null;
		try
		{
			BasicDBObject query = new BasicDBObject("_id", new ObjectId(communityIdStr));
			BasicDBObject dbo = (BasicDBObject)DbManager.getSocial().getCommunity().findOne(query);
			
			if ((null != dbo) && !dbo.isEmpty())
			{
				community = CommunityPojo.fromDb(dbo, CommunityPojo.class);				
			}
		} 
		catch (Exception e)
		{
			logger.error("Exception Message: " + e.getMessage(), e);
		}
		return isOwnerOrModerator(community, personIdStr, bAllowContentPublisher);
	}
	
	private static boolean isOwnerOrModerator(CommunityPojo community, String personIdStr, boolean bAllowContentPublisher)
	{
		boolean isOwnerOrModerator = false;		
		try
		{			
			if (community != null)
			{				
				if (community.getIsPersonalCommunity() && community.getId().toString().equals(personIdStr))
				{
					isOwnerOrModerator = true;					
				}	
				else if (community.getIsPersonalCommunity()) // won't have ownerId so just fail immediately
				{
					isOwnerOrModerator = false;										
				}
				else if (community.getOwnerId().toString().equalsIgnoreCase(personIdStr))
				{
					isOwnerOrModerator = true;
				}
				else
				{
					isOwnerOrModerator = isModerator(personIdStr, community, bAllowContentPublisher);
				}
			}
		} 
		catch (Exception e)
		{
			logger.error("Exception Message: " + e.getMessage(), e);
		}
		return isOwnerOrModerator;
	}
	
	public static boolean isModerator(String personIdStr, CommunityPojo community) 
	{
		return isModerator(personIdStr, community, false);
	}
	
	public static boolean isModerator(String personIdStr, CommunityPojo community, boolean bAllowContentPublisher) 
	{
		Set<CommunityMemberPojo> members = community.getMembers();
		for (CommunityMemberPojo c : members)
		{
			if (c.get_id().toString().equalsIgnoreCase(personIdStr) && c.getUserType().equalsIgnoreCase("moderator"))
				return true;
			else if (bAllowContentPublisher && c.get_id().toString().equalsIgnoreCase(personIdStr) && c.getUserType().equalsIgnoreCase("content_publisher"))
				return true;
		}		
		return false;
	}
	
	
	/**
	 * Recursive helper function for createParentTreeForCommunity.  
	 * 
	 * NOTE: This function will always update intermediate nodes but 
	 * will only update the community passed in if updateCommunity == true.
	 * 
	 * @param community
	 * @param updateCommunity
	 * @return
	 */
	public static CommunityPojo createParentTreeRecursion( CommunityPojo community, boolean updateCommunity )
	{
		//if we have a parentId but no parentTree, we go get our parent
		//otherwise we are already done and can come back down (parentTree is already set or 
		//we have no parent so would return an empty list anyways)
		if ( community.getParentId() != null && community.getParentTree() == null )
		{
			//get next node and move up a level
			CommunityPojo parent_community = CommunityPojo.fromDb(MongoDbManager.getSocial().getCommunity().findOne(new BasicDBObject("_id", community.getParentId())), CommunityPojo.class);
			parent_community = createParentTreeRecursion(parent_community, true);
			//now we have the parent_comm, add parentId to beginning of parentTree and update this comm
			List<ObjectId> parentTree;
			if ( parent_community.getParentTree() != null )
				parentTree = new ArrayList<ObjectId>(parent_community.getParentTree());
			else
				parentTree = new ArrayList<ObjectId>();
			parentTree.add(0, parent_community.getId());
			community.setParentTree(parentTree);
			if ( updateCommunity )
			{
				BasicDBObject update = new BasicDBObject("$set", new BasicDBObject("parentTree", parentTree));		
				MongoDbManager.getSocial().getCommunity().update(new BasicDBObject("_id", community.getId()), update);
			}			
		}
		return community;
	}
	
	/**
	 * getCommunities
	 * Accepts a String[] of community IDs and returns an ArrayList of CommunityPojos
	 * @param ids
	 * @return
	 */
	public static ArrayList<CommunityPojo> getCommunities(Collection<ObjectId> ids) 
	{	
		BasicDBObject in = new BasicDBObject();
		in.put("$in", ids);
		BasicDBObject query = new BasicDBObject();
		query.put("_id", in);
		
		ArrayList<CommunityPojo> communities = new ArrayList<CommunityPojo>();
		
		try
		{
			DBCursor dbc = DbManager.getSocial().getCommunity().find(query);
			communities.addAll(CommunityPojo.listFromDb(dbc, CommunityPojo.listType()));
		} 
		catch (Exception e)
		{
			logger.error("Exception Message: " + e.getMessage(), e);
		}
		return communities;
	}
	
	/**
	 * PersonPojo (UTILITY FOR SOURCE HANDLER AND RESTE TOOLS)
	 * @param id
	 * @return
	 */
	public static PersonPojo getPerson(String id)
	{
		PersonPojo person = null;
		
		try
		{
			// Set up the query
			PersonPojo personQuery = new PersonPojo();
			personQuery.set_id(new ObjectId(id));
			
			BasicDBObject dbo = (BasicDBObject) DbManager.getSocial().getPerson().findOne(personQuery.toDb());
			person = PersonPojo.fromDb(dbo, PersonPojo.class);
		} 
		catch (Exception e)
		{
			logger.error("Exception Message: " + e.getMessage(), e);
		}
		return person;
	}
	
	// Utility function to get communities entered in the following formats:
		// "*" for all user communities
		// "*<regex>" to apply a regex to the community names
		// "<id1>,<id2>,etc"
		
		static public String[] getCommunityIds(String userIdStr, String communityIdStrList) {
			
			if (communityIdStrList.charAt(0) < 0x30) {
				Pattern communityRegex = null;
				if (communityIdStrList.length() > 1) {
					communityRegex = Pattern.compile(communityIdStrList.substring(1), Pattern.CASE_INSENSITIVE);
				}
				HashSet<ObjectId> allCommunities = getUserCommunities(userIdStr, communityRegex);
				String[] communityIds = new String[allCommunities.size()];
				int i = 0; 
				for (ObjectId oid: allCommunities) {
					communityIds[i] = oid.toString();
					++i;
				}
				return communityIds;
			}
			else {
				 String[] communityIdStrs = communityIdStrList.split("\\s*,\\s*");
				 return communityIdStrs;
			}
		} //TESTED
		
		// Get a list of communities from a user and an optional regex
		
		static public HashSet<ObjectId> getUserCommunities(String userIdStr) {
			return getUserCommunities(userIdStr, null);
		}
		
		static public HashSet<ObjectId> getUserCommunities(String userIdStr, Pattern regex) {
			PersonPojo person = SocialUtils.getPerson(userIdStr);
			HashSet<ObjectId> memberOf = new HashSet<ObjectId>();
			if (null != person) {
				if (null != person.getCommunities()) {
					for (PersonCommunityPojo community: person.getCommunities()) {
						if ((null == regex) || regex.matcher(community.getName()).find()) {
							memberOf.add(community.get_id());
						}
					}
				}
			}
			return memberOf;
		}//TESTED
				
		/**
		 * validateCommunityIds
		 * @param userIdStr
		 * @param communityIdStrList
		 * @return
		 */
		public static boolean validateCommunityIds(String userIdStr, String communityIdStrList) 
		{
			// Trivial case:
			if ( (null == communityIdStrList) || (communityIdStrList.isEmpty()) || (communityIdStrList.charAt(0) < 0x30) ) 
			{
				// (<0x30 => is a regex, don't need to validate since check vs person pojos anyway) 
				return true;
			}
			if (null == userIdStr) {
				return false;
			}
			if (RESTTools.adminLookup(userIdStr)) {
				return true;
			}
			
			String[] communities =  communityIdStrList.split(",");

			HashSet<ObjectId> communityObjects = new HashSet<ObjectId>();
			for (int i = 0; i < communities.length; i++)
			{
				communityObjects.add(new ObjectId(communities[i]));
			}
			
			// Get object Id for owner test
			ObjectId userId = null;
			try {
				userId = new ObjectId(userIdStr);
			}
			catch (Exception e) {
				userId = new ObjectId("0"); // (dummy user id)
			}			
			try
			{
				//check in mongo a user is part of these groups		
				BasicDBObject query = new BasicDBObject("_id",new BasicDBObject("$in",communityObjects) ); 
				List<CommunityPojo> retCommunities = CommunityPojo.listFromDb(DbManager.getSocial().getCommunity().find(query), CommunityPojo.listType());
				if ( retCommunities.size() == communityObjects.size() ) //make sure it found a group for all the id's
				{
					for (CommunityPojo cp: retCommunities)
					{
						//check to make sure user is a member or is his personal community (communityid and userid will be the same)
						if ( !cp.getId().equals(new ObjectId(userIdStr))) //this is NOT a personal group so check we are a member
						{
							if (!userId.equals(cp.getOwnerId())) { // (if you're owner you can always have it)
								if ( !cp.isMember(new ObjectId(userIdStr)) ) { //if user is not a member of this group, return false
									return false;
								}
							}
						}
					}
				}
				else
				{
					//error wrong number of groups returned meaning incorrect community ids were sent (groups dont exist)
					return false;				
				}
			}
			catch (Exception ex)
			{
				return false;
			}		
			return true; //made it thru the gauntlet, return successful
		}	
}
