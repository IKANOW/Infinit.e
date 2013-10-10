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
package com.ikanow.infinit.e.data_model.index.document;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.ikanow.infinit.e.data_model.index.BasePojoIndexMap;
import com.ikanow.infinit.e.data_model.index.ElasticSearchPojos;
import com.ikanow.infinit.e.data_model.store.document.AssociationPojo;

// For normal queries and faceting (happens *after* synchronizeForChildMapping)

public class AssociationPojoIndexMap implements BasePojoIndexMap<AssociationPojo> {

	@Override
	public GsonBuilder extendBuilder(GsonBuilder gp) {
		return gp.registerTypeAdapter(AssociationPojo.class, new EventPojoSerializer());
	}

	protected static class EventPojoSerializer implements JsonSerializer<AssociationPojo> 
	{
		@Override
		public JsonElement serialize(AssociationPojo evt, Type typeOfT, JsonSerializationContext context)
		{
			synchronizeWithIndex(evt);			
			JsonElement jo = new GsonBuilder().create().toJsonTree(evt, typeOfT);			
			return jo;		
		}
		// Utility function for serialization
		
		private static void synchronizeWithIndex(AssociationPojo evt) {			
			int nThings = 0;
			// Create an overall index
			StringBuffer sb = new StringBuffer(evt.getAssociation_type()).append('|');
			if (null != evt.getEntity1_index()) {
				sb.append(evt.getEntity1_index().replaceAll("\\|","%7C"));
				nThings++;
			}
			sb.append('|');
			if (null != evt.getVerb_category()) {
				sb.append(evt.getVerb_category().replaceAll("\\|","%7C"));
				//(don't count this towards link analysis usage)
			}
			sb.append('|');
			if (null != evt.getEntity2_index()) {
				sb.append(evt.getEntity2_index().replaceAll("\\|","%7C"));
				nThings++;
			}
			sb.append('|');
			if (null != evt.getGeo_index()) { // (don't count this)
				sb.append(evt.getGeo_index().replaceAll("\\|","%7C"));
				nThings++;
			}
			if (nThings > 1) {
				evt.setAssociation_index(sb.toString()); // (this gives the full event)			
			}

			// Get rid of dates that aren't ISO compliant:
			SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
			if (null != evt.getTime_start()) {
				try {
					parser.parse(evt.getTime_start());
				}
				catch (Exception e) {
					evt.setTime_start(null);					
				}
			}
			if (null != evt.getTime_end()) {
				try {
					parser.parse(evt.getTime_end());
				}
				catch (Exception e) {
					evt.setTime_end(null);					
				}
			}
			
		}//TESTED (see MongoDocumentTxfer)		
	}
	////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////
	
	public static class Mapping {
		
		public final String type = "nested";
		
		public static class RootProperties {
			public ElasticSearchPojos.FieldStringPojo assoc_index = new ElasticSearchPojos.FieldStringPojo("yes", "not_analyzed", null).excludeFromAll();
			public ElasticSearchPojos.FieldStringPojo entity1 = new ElasticSearchPojos.FieldStringPojo("no", "analyzed", null).excludeFromAll();
			public ElasticSearchPojos.FieldStringPojo entity1_index = new ElasticSearchPojos.FieldStringPojo("no", "not_analyzed", null).excludeFromAll();
			public ElasticSearchPojos.FieldStringPojo entity2 = new ElasticSearchPojos.FieldStringPojo("no", "analyzed", null).excludeFromAll();
			public ElasticSearchPojos.FieldStringPojo entity2_index = new ElasticSearchPojos.FieldStringPojo("no", "not_analyzed", null).excludeFromAll();
			public ElasticSearchPojos.FieldStringPojo verb = new ElasticSearchPojos.FieldStringPojo("no", "analyzed", null).excludeFromAll();
			public ElasticSearchPojos.FieldStringPojo verb_category = new ElasticSearchPojos.FieldStringPojo("no", "analyzed", null);
			public ElasticSearchPojos.FieldStringPojo geo_index = new ElasticSearchPojos.FieldStringPojo("no", "not_analyzed", null).excludeFromAll();
				 // ^^^(exclude this geo index from all since it appears in the entity anyway)
			public ElasticSearchPojos.FieldGeoPointPojo geotag = new ElasticSearchPojos.FieldGeoPointPojo("yes");
			public ElasticSearchPojos.FieldStringPojo assoc_type = new ElasticSearchPojos.FieldStringPojo("yes", "not_analyzed", null).excludeFromAll();
			ElasticSearchPojos.FieldDatePojo time_start = new ElasticSearchPojos.FieldDatePojo("yes", null, null, null).excludeFromAll(); 
			ElasticSearchPojos.FieldDatePojo time_end = new ElasticSearchPojos.FieldDatePojo("yes", null, null, null).excludeFromAll(); 
			public ElasticSearchPojos.FieldDoublePojo sentiment = new ElasticSearchPojos.FieldDoublePojo("yes", null, null);
		}
		RootProperties properties = new RootProperties();
	}
	
	////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////
	
	// Utility function (how it should have worked in the serializer probably)
	public static String serialize(AssociationPojo evt) {
		StringBuffer sb = new StringBuffer(evt.getAssociation_type()).append('\n');
		if (null != evt.getEntity1_index()) {
			sb.append(evt.getEntity1_index());
		}
		sb.append('\n');
		if (null != evt.getVerb_category()) {
			sb.append(evt.getVerb_category());
			//(don't count this towards link analysis usage)
		}
		sb.append('\n');
		if (null != evt.getEntity2_index()) {
			sb.append(evt.getEntity2_index());
		}
		sb.append('\n');
		if (null != evt.getGeo_index()) { // (don't count this)
			sb.append(evt.getGeo_index());
		}
		return sb.toString();
	}
	//TESTED (by eye)
	
}//TESTED (see MongoDocumentTxfer)
