package com.ikanow.infinit.e.data_model.index.feature.entity;

import java.lang.reflect.Type;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.ikanow.infinit.e.data_model.index.BasePojoIndexMap;
import com.ikanow.infinit.e.data_model.index.ElasticSearchPojos;
import com.ikanow.infinit.e.data_model.index.ElasticSearchPojos.FieldStringPojo;
import com.ikanow.infinit.e.data_model.store.feature.entity.EntityFeaturePojo;

public class EntityFeaturePojoIndexMap implements BasePojoIndexMap<EntityFeaturePojo> {

	// Misc access constants:
	final public static String indexName_ = "entity_index";
	
	@Override
	public GsonBuilder extendBuilder(GsonBuilder gp) {
		return gp.registerTypeAdapter(EntityFeaturePojo.class, new EventFeaturePojoSerializer());
	}

	protected static class EventFeaturePojoSerializer implements JsonSerializer<EntityFeaturePojo> 
	{
		@Override
		public JsonElement serialize(EntityFeaturePojo ent, Type typeOfT, JsonSerializationContext context)
		{
			String sCommunity = ent.getCommunityId().toString();
			synchronizeWithIndex(ent);			
			JsonElement jo = new GsonBuilder().create().toJsonTree(ent, typeOfT);
			jo.getAsJsonObject().add(EntityFeaturePojo.communityId_, new JsonPrimitive(sCommunity));
			jo.getAsJsonObject().add(EntityFeaturePojo._id_, new JsonPrimitive(new StringBuffer(ent.getIndex()).append(':').append(sCommunity).toString()));
				// (use an _id of index:community in elasticsearch)
			return jo;		
		}
		// Utility function for serialization
		private void synchronizeWithIndex(EntityFeaturePojo ent) {
			ent.set_id(null);
			ent.setCommunityId(null); // (set separately by json above)
			ent.setDbSyncDoccount(null);
			ent.setDbSyncTime(null);
			ent.setTotalfreq(null);
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
				ElasticSearchPojos.FieldStringPojo index = new ElasticSearchPojos.FieldStringPojo("yes", "not_analyzed", null).excludeFromAll();
				ElasticSearchPojos.FieldStringPojo disambiguated_name = new ElasticSearchPojos.FieldStringPojo("yes", "analyzed", null).setAnalyzer("suggestAnalyzer");
						
				ElasticSearchPojos.BiFieldPojo<FieldStringPojo> alias = new ElasticSearchPojos.BiFieldPojo<FieldStringPojo>(
							new ElasticSearchPojos.FieldStringPojo("no", "analyzed", null).setAnalyzer("suggestAnalyzer"),
							new ElasticSearchPojos.FieldStringPojo("no", "not_analyzed", null));
				final public static String alias_pri_ = "alias.pri";	
				
				ElasticSearchPojos.FieldStringPojo communityId = new ElasticSearchPojos.FieldStringPojo("yes", "not_analyzed", null);
				ElasticSearchPojos.FieldStringPojo type = new ElasticSearchPojos.FieldStringPojo("yes", "not_analyzed", null);
				ElasticSearchPojos.FieldStringPojo dimension = new ElasticSearchPojos.FieldStringPojo("yes", "not_analyzed", null);
				ElasticSearchPojos.FieldLongPojo doccount = new ElasticSearchPojos.FieldLongPojo("yes", null, null);				
				ElasticSearchPojos.FieldGeoPointPojo geotag = new ElasticSearchPojos.FieldGeoPointPojo("yes");	
				ElasticSearchPojos.FieldStringPojo ontology_type = new ElasticSearchPojos.FieldStringPojo("yes", "not_analyzed", null);
				ElasticSearchPojos.FieldStringPojo linkdata = new ElasticSearchPojos.FieldStringPojo("yes", "no", null).excludeFromAll();				
			}
			
			RootProperties properties = new RootProperties();
		} 
		
		RootObject entity_index = new RootObject();
	}
}
