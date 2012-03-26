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
package com.ikanow.infinit.e.data_model.index.document;

import java.lang.reflect.Type;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.ikanow.infinit.e.data_model.index.BasePojoIndexMap;
import com.ikanow.infinit.e.data_model.index.ElasticSearchPojos;
import com.ikanow.infinit.e.data_model.store.document.EntityPojo;

public class EntityPojoIndexMap implements BasePojoIndexMap<EntityPojo> {
	
	@Override
	public GsonBuilder extendBuilder(GsonBuilder gp) {
		return gp.registerTypeAdapter(EntityPojo.class, new EntityPojoSerializer());
	}

	protected static class EntityPojoSerializer implements JsonSerializer<EntityPojo> 
	{
		@Override
		public JsonElement serialize(EntityPojo ent, Type typeOfT, JsonSerializationContext context)
		{
			synchronizeWithIndex(ent);			
			JsonElement jo = new GsonBuilder().create().toJsonTree(ent, typeOfT);			
			return jo;		
		}
		// Utility function for serialization
		
		private static void synchronizeWithIndex(EntityPojo ent) {
			ent.setEntid(null);
			ent.setTotalfrequency(null);
			ent.setDoccount(null);
			ent.setSemanticLinks(null);
			ent.setRelevance(null);		
			if ( ent.getOntology_type() == null )
				ent.setOntology_type("point");
		}		
	}
	////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////
	
	public static class Mapping {
	
		public final String type = "nested";
		
		public static class RootProperties {
			// Names
			public ElasticSearchPojos.FieldStringPojo disambiguated_name = new ElasticSearchPojos.FieldStringPojo("no", "analyzed", null);
			public ElasticSearchPojos.FieldStringPojo actual_name = new ElasticSearchPojos.FieldStringPojo("no", "analyzed", null).excludeFromAll();
			public ElasticSearchPojos.FieldStringPojo index = new ElasticSearchPojos.FieldStringPojo("no", "not_analyzed", null).excludeFromAll();
			public ElasticSearchPojos.FieldStringPojo type = new ElasticSearchPojos.FieldStringPojo("no", "not_analyzed", null).excludeFromAll();
			public ElasticSearchPojos.FieldStringPojo dimension = new ElasticSearchPojos.FieldStringPojo("no", "not_analyzed", null).excludeFromAll();
			public ElasticSearchPojos.FieldStringPojo ontology_type = new ElasticSearchPojos.FieldStringPojo("no", "analyzed", null).excludeFromAll();
			public ElasticSearchPojos.FieldGeoPointPojo geotag = new ElasticSearchPojos.FieldGeoPointPojo("yes");
			// Stats
			public ElasticSearchPojos.FieldLongPojo frequency = new ElasticSearchPojos.FieldLongPojo("yes", "no", null);
			public ElasticSearchPojos.FieldDoublePojo sentiment = new ElasticSearchPojos.FieldDoublePojo("yes", null, null);
				// (include in the index since we want to calculate averages over it)
			
		}
		RootProperties properties = new RootProperties();
	}
}
