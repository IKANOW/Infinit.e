package com.ikanow.infinit.e.data_model.api.knowledge;

import java.lang.reflect.Type;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.ikanow.infinit.e.data_model.api.BaseApiPojo;
import com.ikanow.infinit.e.data_model.api.BasePojoApiMap;

public class SearchSuggestPojoApiMap implements BasePojoApiMap<SearchSuggestPojo>
{

	public GsonBuilder extendBuilder(GsonBuilder gp) {
		return gp.registerTypeAdapter(SearchSuggestPojo.class, new SearchSuggestPojoSerializer())
					.registerTypeAdapter(SearchSuggestPojo.class, new SearchSuggestPojoSerializer());
	}
		
	protected static class SearchSuggestPojoSerializer implements JsonSerializer<SearchSuggestPojo> 
	{
		@Override
		public JsonElement serialize(SearchSuggestPojo doc, Type typeOfT, JsonSerializationContext context)
		{
			JsonElement je = BaseApiPojo.getDefaultBuilder().create().toJsonTree(doc, typeOfT);
			return je;
		}
		
	}

}
