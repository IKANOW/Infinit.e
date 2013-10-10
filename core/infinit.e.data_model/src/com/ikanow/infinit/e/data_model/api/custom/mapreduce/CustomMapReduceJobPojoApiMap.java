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
package com.ikanow.infinit.e.data_model.api.custom.mapreduce;

import com.ikanow.infinit.e.data_model.api.BasePojoApiMap;
import com.ikanow.infinit.e.data_model.store.custom.mapreduce.CustomMapReduceJobPojo;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
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

// When retrieving a source object, need to restrict visibility of communities to
// those allowed to know

public class CustomMapReduceJobPojoApiMap implements BasePojoApiMap<CustomMapReduceJobPojo> 
{
	// NOTE: auto set key can only be done for testing, since it doesn't guarantee uniqueness...
	public CustomMapReduceJobPojoApiMap(Set<ObjectId> allowedCommunityIds) 
	{
		_allowedCommunityIds = allowedCommunityIds;
	}
	private Set<ObjectId> _allowedCommunityIds = null;
	
	public GsonBuilder extendBuilder(GsonBuilder gb) {
		return gb.registerTypeAdapter(CustomMapReduceJobPojo.class, new CustomMapReduceJobPojoSerializer( _allowedCommunityIds)).
					registerTypeAdapter(CustomMapReduceJobPojo.class, new CustomMapReduceJobPojoDeserializer(_allowedCommunityIds));		
	}
	
	// Custom serialization:
	
	protected static class CustomMapReduceJobPojoSerializer implements JsonSerializer<CustomMapReduceJobPojo> 
	{
		private Set<ObjectId> _allowedCommunityIds = null;
		
		CustomMapReduceJobPojoSerializer(Set<ObjectId> allowedCommunityIds) 
		{
			_allowedCommunityIds = allowedCommunityIds;
		}
		
		@Override
		public JsonElement serialize(CustomMapReduceJobPojo job, Type typeOfT, JsonSerializationContext context)
		{			
			// check below vs community list			
			List<ObjectId> tmp = job.communityIds;
			if (null != tmp) 
			{
				job.communityIds = new ArrayList<ObjectId>();
				for (ObjectId communityID : tmp) 
				{
					if (_allowedCommunityIds.contains(communityID)) 
					{
						job.communityIds.add(communityID);						
					}
				}
			}
			
			// Somehow a security error has occurred
			if (job.communityIds.size() == 0) 
			{ 
				//Exception out and hope for the best!
				throw new RuntimeException("Insufficient access permissions on this object");
			}
			JsonElement json = BaseApiPojo.getDefaultBuilder().create().toJsonTree(job);
			
			// Ensure the POJO isn't modified by the mapping
			if (null != tmp) 
			{
				job.communityIds = tmp;
			}
			
			return json;
		}		
	}
	
	protected static class CustomMapReduceJobPojoDeserializer implements JsonDeserializer<CustomMapReduceJobPojo> 
	{
		private Set<ObjectId> _allowedCommunityIds = null;
		CustomMapReduceJobPojoDeserializer(Set<ObjectId> allowedCommunityIds) 
		{
			_allowedCommunityIds = allowedCommunityIds;
		}
		
		@Override
		public CustomMapReduceJobPojo deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
		{
			CustomMapReduceJobPojo job = BaseApiPojo.getDefaultBuilder().create().fromJson(json, CustomMapReduceJobPojo.class);
			if (null != _allowedCommunityIds) 
			{
				job.communityIds = new ArrayList<ObjectId>();				
				for (ObjectId communityId: _allowedCommunityIds) 
				{
					job.communityIds.add(communityId);					
				}
			}
			return job;
		}
	}
}
