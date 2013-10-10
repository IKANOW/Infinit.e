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
package com.ikanow.infinit.e.data_model.index.feature.event;

import java.lang.reflect.Type;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.ikanow.infinit.e.data_model.index.BasePojoIndexMap;
import com.ikanow.infinit.e.data_model.index.ElasticSearchPojos;
import com.ikanow.infinit.e.data_model.store.feature.association.AssociationFeaturePojo;

public class AssociationFeaturePojoIndexMap implements BasePojoIndexMap<AssociationFeaturePojo> {

	// Misc access constants:
	final public static String indexName_ = "association_index";
	final public static String indexCollectionName_ = "assocs_index";
	
	@Override
	public GsonBuilder extendBuilder(GsonBuilder gp) {
		return gp.registerTypeAdapter(AssociationFeaturePojo.class, new EventFeaturePojoSerializer());
	}
	
	/////////////////////////////////////////////////////////////////////////////////////

	// Index synchronization
	
	protected static class EventFeaturePojoSerializer implements JsonSerializer<AssociationFeaturePojo> 
	{
		@Override
		public JsonElement serialize(AssociationFeaturePojo evt, Type typeOfT, JsonSerializationContext context)
		{
			String sIndex = evt.getIndex();
			String sCommunity = evt.getCommunityId().toString();
			synchronizeWithIndex(evt);			
			JsonElement jo = new GsonBuilder().create().toJsonTree(evt, typeOfT);			
			jo.getAsJsonObject().add("communityId", new JsonPrimitive(sCommunity));
			jo.getAsJsonObject().add("_id", new JsonPrimitive(new StringBuffer(sIndex).append(':').append(sCommunity).toString()));
				// (use an _id of index:community in elasticsearch)
			
			return jo;		
		}
		private static void synchronizeWithIndex(AssociationFeaturePojo evt) {
			evt.setCommunityId(null);
			evt.setIndex(null); // (both these are set post serialization, above)
			
			evt.setDb_sync_doccount(null);
			evt.setDb_sync_time(null);
			evt.setDb_sync_prio(null);

			//TODO (INF-1234) Eventually want to index this information, but it's currently not clear what to do with
			// it inside the assocSuggest (which is hte only place it's currently used)
			if (null == evt.getEntity1_index()) {
				evt.setEntity1(null);
			}
			if (null == evt.getEntity2_index()) {
				evt.setEntity2(null);
			}
		}	
	}
	
	/////////////////////////////////////////////////////////////////////////////////////
	
	// Elastic Search mapping
	
	public static class Mapping
	{
		public static class RootObject 
		{
			ElasticSearchPojos.SourcePojo _source = new ElasticSearchPojos.SourcePojo(false, null, null);
			ElasticSearchPojos.AllPojo _all = new ElasticSearchPojos.AllPojo(true);
			
			public static class RootProperties 
			{
				ElasticSearchPojos.FieldStringPojo entity1 = new ElasticSearchPojos.FieldStringPojo("yes", "analyzed", null).setAnalyzer("suggestAnalyzer");
				ElasticSearchPojos.FieldStringPojo entity1_index = new ElasticSearchPojos.FieldStringPojo("yes", "not_analyzed", null);
				ElasticSearchPojos.FieldStringPojo verb = new ElasticSearchPojos.FieldStringPojo("yes", "analyzed", null).setAnalyzer("suggestAnalyzer");
				ElasticSearchPojos.FieldStringPojo verb_category = new ElasticSearchPojos.FieldStringPojo("yes", "not_analyzed", null);
				ElasticSearchPojos.FieldStringPojo entity2 = new ElasticSearchPojos.FieldStringPojo("yes", "analyzed", null).setAnalyzer("suggestAnalyzer");
				ElasticSearchPojos.FieldStringPojo entity2_index = new ElasticSearchPojos.FieldStringPojo("yes", "not_analyzed", null);
				ElasticSearchPojos.FieldStringPojo geo_index = new ElasticSearchPojos.FieldStringPojo("yes", "not_analyzed", null);
				ElasticSearchPojos.FieldStringPojo assoc_type = new ElasticSearchPojos.FieldStringPojo("yes", "not_analyzed", null);
				ElasticSearchPojos.FieldLongPojo totalfreq = new ElasticSearchPojos.FieldLongPojo("yes", null, null);
				ElasticSearchPojos.FieldLongPojo doccount = new ElasticSearchPojos.FieldLongPojo("yes", null, null);
				ElasticSearchPojos.FieldStringPojo communityId = new ElasticSearchPojos.FieldStringPojo("yes", "not_analyzed", null);
			}
			
			RootProperties properties = new RootProperties();
		} 
		
		RootObject event_index = new RootObject();
	}
}
