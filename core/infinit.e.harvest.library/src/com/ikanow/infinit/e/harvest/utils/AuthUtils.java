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

import org.bson.types.ObjectId;

import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.social.authentication.AuthenticationPojo;
import com.mongodb.BasicDBObject;

public class AuthUtils {
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
}
