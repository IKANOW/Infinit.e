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
package com.ikanow.infinit.e.data_model.index.feature.geo;

import java.lang.reflect.Type;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.ikanow.infinit.e.data_model.index.BasePojoIndexMap;
import com.ikanow.infinit.e.data_model.index.ElasticSearchPojos;
import com.ikanow.infinit.e.data_model.store.feature.geo.GeoFeaturePojo;

public class GeoFeaturePojoIndexMap implements BasePojoIndexMap<GeoFeaturePojo> {

	@Override
	public GsonBuilder extendBuilder(GsonBuilder gp) {
		return gp.registerTypeAdapter(GeoFeaturePojo.class, new GeoFeaturePojoSerializer());
	}

	protected static class GeoFeaturePojoSerializer implements JsonSerializer<GeoFeaturePojo> 
	{
		@Override
		public JsonElement serialize(GeoFeaturePojo ent, Type typeOfT, JsonSerializationContext context)
		{
			synchronizeWithIndex(ent);			
			JsonElement jo = new GsonBuilder().create().toJsonTree(ent, typeOfT);			
			return jo;		
		}
		// Utility function for serialization
		private void synchronizeWithIndex(GeoFeaturePojo ent) 
		{	
			//removed everything but search_field and population	
			ent.setCity(null);
			ent.setCountry(null);
			ent.setCountry_code(null);
			ent.setGeoindex(null);
			ent.setLatitude(null);
			ent.setLongitude(null);			
			ent.setRegion(null);
			ent.setRegion_code(null);	
		}			
	}
	
	// Schema for ElasticSearch pojos: 	
	// (Nested objects each have their own "properties" objects)
	public static class Mapping 
	{
		public static class RootObject 
		{
			ElasticSearchPojos.SourcePojo _source = new ElasticSearchPojos.SourcePojo(false, null, null);
			ElasticSearchPojos.AllPojo _all = new ElasticSearchPojos.AllPojo(false);
			
			public static class RootProperties 
			{													
				ElasticSearchPojos.FieldIntegerPojo population = new ElasticSearchPojos.FieldIntegerPojo("yes", null, null);	
				ElasticSearchPojos.FieldStringPojo search_field = new ElasticSearchPojos.FieldStringPojo("no", "not_analyzed", null);  
			}			
			RootProperties properties = new RootProperties();
		} 		
		RootObject geo_index = new RootObject();
	}
}
