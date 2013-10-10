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
package com.ikanow.infinit.e.data_model.api.social.sharing;

import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.bson.types.ObjectId;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.ikanow.infinit.e.data_model.api.BaseApiPojo;
import com.ikanow.infinit.e.data_model.api.BasePojoApiMap;
import com.ikanow.infinit.e.data_model.store.social.sharing.SharePojo;
import com.ikanow.infinit.e.data_model.store.social.sharing.SharePojo.ShareCommunityPojo;

public class SharePojoApiMap implements BasePojoApiMap<SharePojo> {

	public SharePojoApiMap(Set<ObjectId> allowedCommunityIds) {
		_allowedCommunityIds = allowedCommunityIds;
	}
	private Set<ObjectId> _allowedCommunityIds = null;
	
	public GsonBuilder extendBuilder(GsonBuilder gp) {
		return gp.registerTypeAdapter(SharePojo.class, new SharePojoSerializer(_allowedCommunityIds));
	}
	protected static class SharePojoSerializer implements JsonSerializer<SharePojo> 
	{
		private Set<ObjectId> _allowedCommunityIds = null;
		SharePojoSerializer(Set<ObjectId> allowedCommunityIds) {
			_allowedCommunityIds = allowedCommunityIds;
		}
		@Override
		public JsonElement serialize(SharePojo share, Type typeOfT, JsonSerializationContext context)
		{
			List<ShareCommunityPojo> communities = share.getCommunities();
			if ((null != communities) && (null != _allowedCommunityIds)) {
				Iterator<ShareCommunityPojo> commIt = communities.iterator();
				while (commIt.hasNext()) {
					if (!_allowedCommunityIds.contains(commIt.next().get_id())) {
						commIt.remove();
							// (ok we're "corrupting" the share pojo here to make life easy)
					}
				}
				if (communities.isEmpty()) { // Somehow a security error has occurred
					//Exception out and hope for the best!
					throw new RuntimeException("Insufficient access permissions on this object");					
				}
			}//TESTED
			Set<ObjectId> communityIds = share.getEndorsed();
			if ((null != communityIds) && (null != _allowedCommunityIds)) {
				Iterator<ObjectId> commIt = communityIds.iterator();
				while (commIt.hasNext()) {
					if (!_allowedCommunityIds.contains(commIt.next())) {
						commIt.remove();
							// (ok we're "corrupting" the share pojo here to make life easy)
					}
				}
			}//TESTED
			JsonElement json = BaseApiPojo.getDefaultBuilder().create().toJsonTree(share);
			return json;
		}		
	}
}//TESTED
