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
