/*******************************************************************************
 * Copyright 2012, The Infinit.e Open Source Project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.ikanow.infinit.e.harvest.utils;

import java.util.HashSet;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.bson.types.ObjectId;

import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.MongoDbManager;
import com.ikanow.infinit.e.data_model.store.social.authentication.AuthenticationPojo;
import com.ikanow.infinit.e.data_model.store.social.community.CommunityMemberPojo;
import com.ikanow.infinit.e.data_model.store.social.community.CommunityPojo;
import com.ikanow.infinit.e.data_model.store.social.community.CommunityPojo.CommunityType;
import com.ikanow.infinit.e.data_model.store.social.person.PersonCommunityPojo;
import com.ikanow.infinit.e.data_model.store.social.person.PersonPojo;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;

public class AuthUtils {
	private static final Logger logger = Logger.getLogger(AuthUtils.class);
	
	public static boolean isAdmin(ObjectId userId) {
		try {
			AuthenticationPojo authQuery = new AuthenticationPojo();
			authQuery.setProfileId(userId);
			BasicDBObject dbo = (BasicDBObject) DbManager.getSocial().getAuthentication().findOne(authQuery.toDb());
			if (null != dbo) {
				AuthenticationPojo ap = AuthenticationPojo.fromDb(dbo, AuthenticationPojo.class);
				if (null != ap.getAccountType()) {
					if (ap.getAccountType().equalsIgnoreCase("admin")) {
						return true;
					}
					else if (ap.getAccountType().equalsIgnoreCase("admin-enabled")) {
						return true; // (these are offline so always allow this also)
					}
				}//TESTED
			}
			return false;
		}
		catch (Exception e) {} // fail out and return false
		return false;
	}
	
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
	
	public static HashSet<ObjectId> getUserCommunities(String userIdStr, Pattern regex) {
		PersonPojo person = AuthUtils.getPerson(userIdStr);
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
}
