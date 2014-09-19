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
package com.ikanow.infinit.e.data_model.store.config.source;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.ikanow.infinit.e.data_model.store.BaseDbPojo;

public class SourceFederatedQueryConfigPojo extends BaseDbPojo {
	// Standard static function for readability
	@SuppressWarnings("unchecked")
	static public TypeToken<List<SourceFederatedQueryConfigPojo>> listType() { return new TypeToken<List<SourceFederatedQueryConfigPojo>>(){}; }

	////////////////////////////////////////////////////////////////////////////////
	
	// FEDERATED REQUEST CONFIGURATION
	
	// Caching:
	public Integer cacheTime_days; //(defaults to 5)
	
	// Set of entity types (lower case) for which to apply this endpoint
	public HashSet<String> entityTypes;

	public Integer queryTimeout_secs; // (timeout - currently only works for script) If not specified then 5 minutes (300s)	
	
	//Input is defined by one of:

	public Boolean bypassSimpleQueryParsing; // (defaults to false, ignored unless importScript==null) 
												// Always pass full query object into importScript as _query (vs using default logic to pull out query param)
	public String importScript; // Script to run (requests field ignore if this is specified), substitution var is _entityValue	
	public String scriptlang; // Language (currently only "python" or "none" is supported)
	
	// or...
	
	public static class FederatedQueryEndpointUrl {
		// Input params ("$1" is used to substitute in any of these params)
		public String endPointUrl;
		public LinkedHashMap<String, String> urlParams;
		public LinkedHashMap<String, String> httpFields; // (use content to POST vs GET)
	}
	public List<FederatedQueryEndpointUrl> requests; // A list of requests
	
	////////////////////////////////////////////////////////////////////////////////

	// FEDERATED RESPONSE CONVERSRION - WILL BE REPLACED WITH STANDARD SOURCE PROCESSING
	
	// Actual doc title is in the format: <this title>: <lookup key>: <result strings>
	public String titlePrefix;
	
	// Map to get fields -  key is nested JSON path expression (.s replaced with :s), value is displayUrl/<case sensitive entity type>
	public HashMap<String, String> docConversionMap;
	// You can populate this script, which is passed _json (output of engine) and _doc (after the conversion map) and you can override fields if you want
	// (CURRENTLY UNUSED)
	//public String simpleJsDocConversionScript;

	// In advance of the ontological share type, this lets users map from the entity type (case sensitive) to dimension (case sensitive)
	// (defaults to what if not present)
	public HashMap<String, String> typeToDimensionMap;
	
	////////////////////////////////////////////////////////////////////////////////

	// TESTING AND UTILITY:
	
	public String testQueryJson; // The JSON of an AdvancedQueryPojo that is passed to the federated query object in a config/source/test call 
	
	transient public SourcePojo  parentSource; // (just used for expediency in internal processing)
	
	////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////
	
	// CUSTOM DESERIALIAZATION: CONVERT entityTypes TO LOWER CASE
	
	public GsonBuilder extendBuilder(GsonBuilder gp) {
		return gp.registerTypeAdapter(SourceFederatedQueryConfigPojo.class, new PojoDeserializer());
	}
	
	protected static class PojoDeserializer implements JsonDeserializer<SourceFederatedQueryConfigPojo> 
	{
		@Override
		public SourceFederatedQueryConfigPojo deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
		{
			JsonElement entTypesCheck = json.getAsJsonObject().remove("entityTypes");
			JsonArray entTypes = null;
			if (null != entTypesCheck) {
				entTypes = entTypesCheck.getAsJsonArray();
			}
			SourceFederatedQueryConfigPojo endpoint = BaseDbPojo.getDefaultBuilder().create().fromJson(json, SourceFederatedQueryConfigPojo.class);
			if (null != entTypes) {
				endpoint.entityTypes = new HashSet<String>(entTypes.size());
				for (JsonElement val: entTypes) {
					String entType = val.getAsString().toLowerCase();
					endpoint.entityTypes.add(entType);
					if (entType.startsWith("/")) { // regex, always add the type as well as the regex
						int regexIndex = entType.lastIndexOf('/'); // (will always match because of startsWith above)
						entType = entType.substring(1+regexIndex);
						if (entType.length() > 0) {
							endpoint.entityTypes.add(entType);							
						}
					}//TESTED					
				}
			}
			return endpoint;
		}
	}
	
}
