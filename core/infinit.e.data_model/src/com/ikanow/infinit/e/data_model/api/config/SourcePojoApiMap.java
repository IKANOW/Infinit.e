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
package com.ikanow.infinit.e.data_model.api.config;

import java.lang.reflect.Type;
import java.util.Set;

import org.bson.types.ObjectId;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.ikanow.infinit.e.data_model.api.BaseApiPojo;
import com.ikanow.infinit.e.data_model.api.BasePojoApiMap;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;

// When retrieving a source object, need to restrict visibility of communities to
// those allowed to know

public class SourcePojoApiMap implements BasePojoApiMap<SourcePojo> {

	// Construction:
	
	public SourcePojoApiMap(Set<ObjectId> allowedCommunityIds) {
		_allowedCommunityIds = allowedCommunityIds;
	}
	public SourcePojoApiMap(Set<ObjectId> allowedCommunityIds, boolean bAutoSetKey) {
		_allowedCommunityIds = allowedCommunityIds;
		_bAutoSetKey = bAutoSetKey;
	}
	private Set<ObjectId> _allowedCommunityIds = null;
	private boolean _bAutoSetKey = true;
	
	public GsonBuilder extendBuilder(GsonBuilder gb) {
		return gb.registerTypeAdapter(SourcePojo.class, new SourcePojoSerializer(_allowedCommunityIds)).
					registerTypeAdapter(SourcePojo.class, new SourcePojoDeserializer(_allowedCommunityIds, _bAutoSetKey));		
	}
	
	// Custom serialization:
	
	protected static class SourcePojoSerializer implements JsonSerializer<SourcePojo> 
	{
		private Set<ObjectId> _allowedCommunityIds = null;
		SourcePojoSerializer(Set<ObjectId> allowedCommunityIds) {
			_allowedCommunityIds = allowedCommunityIds;
		}
		@Override
		public JsonElement serialize(SourcePojo source, Type typeOfT, JsonSerializationContext context)
		{
			Set<ObjectId> tmp = source.getCommunityIds();
			if (null != tmp) {
				source.setCommunityIds(null);
				for (ObjectId communityID: tmp) {
					if (_allowedCommunityIds.contains(communityID)) {
						source.addToCommunityIds(communityID);
					}
				}
			}
			if (null == source.getCommunityIds()) { // Somehow a security error has occurred
				//Exception out and hope for the best!
				throw new RuntimeException("Insufficient access permissions on this object");
			}
			JsonElement json = BaseApiPojo.getDefaultBuilder().create().toJsonTree(source);
			if (null != tmp) { // (this just ensures that the pojo isn't modified by the mapping)
				source.setCommunityIds(tmp);
			}
			return json;
		}		
	}
	protected static class SourcePojoDeserializer implements JsonDeserializer<SourcePojo> 
	{
		private Set<ObjectId> _allowedCommunityIds = null;
		private boolean _bAutoSetKey = true;
		SourcePojoDeserializer(Set<ObjectId> allowedCommunityIds, boolean bAutoSetKey) {
			_allowedCommunityIds = allowedCommunityIds;
			_bAutoSetKey = bAutoSetKey;
		}
		@Override
		public SourcePojo deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
		{
			SourcePojo source = BaseApiPojo.getDefaultBuilder().create().fromJson(json, SourcePojo.class);
			if (null != _allowedCommunityIds) {
				source.setCommunityIds(null);
				for (ObjectId communityId: _allowedCommunityIds) {
					source.addToCommunityIds(communityId);
				}
			}
			if (null == source.getKey()) {
				if (_bAutoSetKey) {
					source.setUrl(source.getUrl()); // (recalculate source key)
				}
			}
			return source;
		}
	}
}
