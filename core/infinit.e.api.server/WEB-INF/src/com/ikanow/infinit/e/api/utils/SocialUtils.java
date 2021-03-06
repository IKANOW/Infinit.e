package com.ikanow.infinit.e.api.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.bson.types.ObjectId;

import com.ikanow.infinit.e.data_model.index.ElasticSearchManager;
import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.MongoDbManager;
import com.ikanow.infinit.e.data_model.store.social.community.CommunityMemberPojo;
import com.ikanow.infinit.e.data_model.store.social.community.CommunityPojo;
import com.ikanow.infinit.e.data_model.store.social.community.CommunityPojo.CommunityType;
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
					//TODO also need to check if a usergroup is moderator/contentpub
					HashSet<ObjectId> userGroupIds = getUserCommunities(personIdStr);
					//check if user is owner/mod/contentpub
					isOwnerOrModerator = isModerator(personIdStr, community, bAllowContentPublisher);
					//if not, check usergroups until we get one
					if ( !isOwnerOrModerator )
						isOwnerOrModerator = userGroupIds.stream().map(ObjectId::toString).anyMatch(id -> isModerator(id, community, bAllowContentPublisher));
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
	
	
	private static Map<String, CommunityIndexStatus> communityIdsIndexStatus = new HashMap<String, CommunityIndexStatus>();
	static class CommunityIndexStatus
	{
		public boolean hasIndex;
		public long timeChecked;
		public CommunityIndexStatus(boolean hasIndex, long timeChecked) {
			this.hasIndex = hasIndex;
			this.timeChecked = timeChecked;
		}
		
	}
	/**
	 * Utility funciton to return community id's entered in the following formats:
	 * "*" for all user communities
	 * "*<regex>" to apply a regex to the community names
	 * "<id1>,<id2>,etc"
	 * 
	 * @param userIdStr
	 * @param communityIdStrList
	 * @param commsWithIndexOnly
	 * @return
	 */
	public static String[] getCommunityIds(String userIdStr, String communityIdStrList, boolean returnCommIdsWithIndexesOnly) {
		Set<String> communitiesToReturn;
		
		if (communityIdStrList.charAt(0) < 0x30) {
			communitiesToReturn = new HashSet<String>();
			Pattern communityRegex = null;
			if (communityIdStrList.length() > 1) {
				communityRegex = Pattern.compile(communityIdStrList.substring(1), Pattern.CASE_INSENSITIVE);
			}
			HashSet<ObjectId> allCommunities = getUserCommunities(userIdStr, communityRegex);
			//String[] communityIds = new String[allCommunities.size()];
			//int i = 0; 
			for (ObjectId oid: allCommunities) {
				communitiesToReturn.add(oid.toString());
				//communityIds[i] = oid.toString();
				//++i;
			}
			//communityIds;
		}
		else {
			 communitiesToReturn = new HashSet<String>(Arrays.asList( communityIdStrList.split("\\s*,\\s*") ));
			 
		}
		if ( returnCommIdsWithIndexesOnly )
		{
			//TODO fix this so if a comm changes from non-index to index, this will still work
			//NOTE: can't multiple things be looking at this at once, so we don't really want to
			//mess with the stream, maybe we should fix this later
			
			//filter out any comms that don't have indexes
			communitiesToReturn = communitiesToReturn.stream()
				.filter( commId -> {
					if ( !communityIdsIndexStatus.containsKey(commId) )
					{
						communityIdsIndexStatus.put(commId, new CommunityIndexStatus( getCommunityIndexStatus(commId), System.currentTimeMillis()));
					}
					return communityIdsIndexStatus.get(commId).hasIndex;
				})
				.collect(Collectors.toSet());
				
		}
		return communitiesToReturn.toArray(new String[communitiesToReturn.size()]);
	}
	
	// Utility function to get communities entered in the following formats:
	// "*" for all user communities
	// "*<regex>" to apply a regex to the community names
	// "<id1>,<id2>,etc"				
	static public String[] getCommunityIds(String userIdStr, String communityIdStrList) {
		return getCommunityIds(userIdStr, communityIdStrList, false);			
	} //TESTED
	
	private static Boolean getCommunityIndexStatus(String commId) {
		//we can do this 2 ways, 
		//1. get commid and check the num_shards attribute, if something went wrong during creation, it might still not have an index
		//2. we can try to get the index from ES, will be correct 100% of the time, but checking for non-existant indexes takes forever for some reason		
		return ElasticSearchManager.pingIndex("doc_" + commId + "*");
	}
		
		// Get a list of communities from a user and an optional regex
		
		static public HashSet<ObjectId> getUserCommunities(String userIdStr) {
			return getUserCommunities(userIdStr, null);
		}
		
		static public HashSet<ObjectId> getUserCommunities(String userIdStr, Pattern regex) {
			PersonPojo person = SocialUtils.getPerson(userIdStr);
			HashSet<ObjectId> memberOf = new HashSet<ObjectId>();
			HashSet<ObjectId> userGroupIds = new HashSet<ObjectId>();
			//STEP 1: collect all user communities matching regex
			if (null != person) {
				if (null != person.getCommunities()) {
					for (PersonCommunityPojo community: person.getCommunities()) {
						if (matchesCommunityRegex(regex, community.getName())) {
							memberOf.add(community.get_id());
						}
						//bulk up the usergroups for step2
						//TODO also need to add in any communities that a usergroup we are member of is a member of e.g. userIdStr -> usergroupA -> datagroupB
						if ( community.getType() == CommunityType.user) {		
							userGroupIds.add(community.get_id());													
						} 
					}
				}
			}
			//STEP 2 create a mega query to look at community.members._id for any of the usergroups we bulked up
			BasicDBObject in_query = new BasicDBObject(MongoDbManager.in_, userGroupIds);
			BasicDBObject query = new BasicDBObject(CommunityPojo.members_ + "." + CommunityMemberPojo._id_, in_query);
			DBCursor dbc = DbManager.getSocial().getCommunity().find(query);
			while ( dbc.hasNext() ) {
				CommunityPojo cp = CommunityPojo.fromDb(dbc.next(), CommunityPojo.class);
				//add any matches into memberOf
				if ( matchesCommunityRegex(regex, cp.getName()) ) {
					memberOf.add(cp.getId());
				}
			}
			
			return memberOf;
		}//TESTED
		
		private static boolean matchesCommunityRegex(Pattern regex, String community_name) {
			return (null == regex) || regex.matcher(community_name).find();
		}
		
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
			
			//calling get commids code and verifying we match
			HashSet<ObjectId> user_comms = getUserCommunities(userIdStr);
			for ( String comm : communities ) {
				ObjectId commid = new ObjectId(comm);
				if ( !user_comms.contains(commid) )
					return false;
			}
			return true; //made it thru the gauntlet, return successful
		}	
}
