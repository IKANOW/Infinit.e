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
import java.util.List;
import java.util.ArrayList;
import java.util.Set;

import org.bson.types.ObjectId;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.ikanow.infinit.e.data_model.api.BaseApiPojo;
import com.ikanow.infinit.e.data_model.api.BasePojoApiMap;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePipelinePojo;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.ikanow.infinit.e.data_model.store.config.source.SourceRssConfigPojo;
import com.ikanow.infinit.e.data_model.store.config.source.StructuredAnalysisConfigPojo;
import com.ikanow.infinit.e.data_model.store.config.source.UnstructuredAnalysisConfigPojo;

// When retrieving a source object, need to restrict visibility of communities to
// those allowed to know

public class SourcePojoApiMap implements BasePojoApiMap<SourcePojo> {

	// Construction:
	
	public SourcePojoApiMap(ObjectId userId, Set<ObjectId> allowedCommunityIds, Set<ObjectId> ownedOrModeratedCommunities) {
		_userId = userId;
		_allowedCommunityIds = allowedCommunityIds;
		_ownedOrModeratedCommunities = ownedOrModeratedCommunities;
	}
	// NOTE: auto set key can only be done for testing, since it doesn't guarantee uniqueness...
	public SourcePojoApiMap(Set<ObjectId> allowedCommunityIds) {
		_allowedCommunityIds = allowedCommunityIds;
	}
	private Set<ObjectId> _allowedCommunityIds = null;
	
	private ObjectId _userId = null; // (stays null for admin)
	private Set<ObjectId> _ownedOrModeratedCommunities = null;
	
	public GsonBuilder extendBuilder(GsonBuilder gb) {
		return gb.registerTypeAdapter(SourcePojo.class, new SourcePojoSerializer(_userId, _allowedCommunityIds, _ownedOrModeratedCommunities)).
					registerTypeAdapter(SourcePojo.class, new SourcePojoDeserializer(_allowedCommunityIds));		
	}
	
	// Custom serialization:
	
	protected static class SourcePojoSerializer implements JsonSerializer<SourcePojo> 
	{
		private Set<ObjectId> _allowedCommunityIds = null;
		private ObjectId _userId;
		private Set<ObjectId> _ownedOrModeratedCommunities = null;
		
		SourcePojoSerializer(ObjectId userId, Set<ObjectId> allowedCommunityIds, Set<ObjectId> ownedOrModeratedCommunities) {
			_userId = userId;
			_allowedCommunityIds = allowedCommunityIds;
			_ownedOrModeratedCommunities = ownedOrModeratedCommunities;
		}
		@Override
		public JsonElement serialize(SourcePojo source, Type typeOfT, JsonSerializationContext context)
		{
			boolean bIsPublic = false;
			if (source.isPublic() || (null == _userId) || _userId.equals(source.getOwnerId())) {
				// (null if admin)
				bIsPublic = true;
			}
			// (else check below vs community list)
			
			Set<ObjectId> tmp = source.getCommunityIds();
			if (null != tmp) {
				source.setCommunityIds(null);
				for (ObjectId communityID: tmp) {
					if (_allowedCommunityIds.contains(communityID)) {
						source.addToCommunityIds(communityID);
						if (!bIsPublic) {
							if (_ownedOrModeratedCommunities.contains(communityID)) {
								bIsPublic = true;
							}
						}
					}
				}
			}
			// (saved fields to rewrite later if not public, do up here for visibility)
			String url = source.getUrl();
			SourceRssConfigPojo rss = source.getRssConfig();
			List<SourcePipelinePojo> pxPipe = source.getProcessingPipeline(); 
			StructuredAnalysisConfigPojo sah = source.getStructuredAnalysisConfig();
			UnstructuredAnalysisConfigPojo uah = source.getUnstructuredAnalysisConfig();
			
			if (!bIsPublic) { // Cleanse URLs, remove processing pipeline information
				source.setPartiallyPublished(true); //TESTED
				
				// Copy URL info from px pipeline into the main source
				if ((null != source.getProcessingPipeline()) && !source.getProcessingPipeline().isEmpty()) {					
					SourcePipelinePojo firstEl = source.getProcessingPipeline().iterator().next();
					if (null != firstEl.web) {
						source.setRssConfig(firstEl.web);
					}
					else if (null != firstEl.feed) {
						source.setRssConfig(firstEl.feed);
					}
					else if (null != firstEl.database) {
						source.setUrl(firstEl.database.getUrl());
					}
					else if (null != firstEl.file) {
						source.setUrl(firstEl.file.getUrl());
					}
					source.setProcessingPipeline(new ArrayList<SourcePipelinePojo>()); // (delete px pipeline)
				}//(end if non-empty px pipeline)
				//TESTED
				
				int nIndex = -1;
				if ((null != url) && ((nIndex = url.indexOf('?')) >= 0)) {
					source.setUrl(url.substring(0, 1 + nIndex));
				}
				if (null != rss) {
					rss.setHttpFields(null); // (remove cookie information)
				}
				if ((null != rss) && (null != rss.getExtraUrls())) {
					SourceRssConfigPojo newRss = new SourceRssConfigPojo();
					ArrayList<SourceRssConfigPojo.ExtraUrlPojo> newList = new ArrayList<SourceRssConfigPojo.ExtraUrlPojo>(rss.getExtraUrls().size());
					for (SourceRssConfigPojo.ExtraUrlPojo urlObj: rss.getExtraUrls()) {
						SourceRssConfigPojo.ExtraUrlPojo newUrlObj = new SourceRssConfigPojo.ExtraUrlPojo();
						if ((null != urlObj.url) && ((nIndex = urlObj.url.indexOf('?')) >= 0)) {
							newUrlObj.url = urlObj.url.substring(0, 1 + nIndex);
						}
						else {
							newUrlObj.url = urlObj.url;
						}
						newUrlObj.title = urlObj.title; 
						newUrlObj.description = urlObj.description;
						newUrlObj.publishedDate = urlObj.publishedDate;
						newUrlObj.fullText = urlObj.fullText;
						newList.add(newUrlObj);
					}
					newRss.setExtraUrls(newList);
					source.setRssConfig(newRss);
				}
				else if (null != rss) {
					source.setRssConfig(null);					
				}
				if (null != source.getStructuredAnalysisConfig()) {
					source.setStructuredAnalysisConfig(new StructuredAnalysisConfigPojo());
				}//TESTED 
				if (null != source.getUnstructuredAnalysisConfig()) {
					source.setUnstructuredAnalysisConfig(new UnstructuredAnalysisConfigPojo());
				}//TESTED 				
			}
			//TESTED (extraUrls with and without ?s, RSS/no extraURLs, URL)	
			
			if (null == source.getCommunityIds()) { // Somehow a security error has occurred
				//Exception out and hope for the best!
				throw new RuntimeException("Insufficient access permissions on this object");
			}
			JsonElement json = BaseApiPojo.getDefaultBuilder().create().toJsonTree(source);
			if (!bIsPublic) { // Remove a load of potentially sensitive information
				JsonObject jsonObj = json.getAsJsonObject();
				//(url cleansed above)
				jsonObj.remove(SourcePojo.database_);
				jsonObj.remove(SourcePojo.file_);
				//(rss cleansed above)
				jsonObj.remove(SourcePojo.authentication_);
				
				// Ensure the POJO isn't modified by the mapping (part 1)
				source.setUrl(url);
				source.setRssConfig(rss);
				source.setProcessingPipeline(pxPipe);
				source.setUnstructuredAnalysisConfig(uah);
				source.setStructuredAnalysisConfig(sah);
				source.setPartiallyPublished(null);
			}			
			// Ensure the POJO isn't modified by the mapping (part 2)
			if (null != tmp) {
				source.setCommunityIds(tmp);
			}
			return json;
		}		
	}//TESTED (private: yes, public sources: yes, owned sources: yes, non-owner/own community: yes)  
	
	protected static class SourcePojoDeserializer implements JsonDeserializer<SourcePojo> 
	{
		private Set<ObjectId> _allowedCommunityIds = null;
		SourcePojoDeserializer(Set<ObjectId> allowedCommunityIds) {
			_allowedCommunityIds = allowedCommunityIds;
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
			return source;
		}
	}
}
