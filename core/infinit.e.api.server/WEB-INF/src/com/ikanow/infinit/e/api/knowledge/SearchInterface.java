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
package com.ikanow.infinit.e.api.knowledge;

import java.util.Map;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;
import org.restlet.resource.ResourceException;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;

import com.ikanow.infinit.e.api.utils.RESTTools;
import com.ikanow.infinit.e.data_model.api.ResponsePojo;
import com.ikanow.infinit.e.data_model.api.ResponsePojo.ResponseObject;

/**
 * @author cmorgan
 *
 */
public class SearchInterface extends Resource 
{
	private SearchHandler search = new SearchHandler();
	private String action = "";
	private String term = "";
	private String field = "";
	private String communityIdStrList = "";
	private String cookieLookup = null;
	private String cookie = null;
	private String ent1 = null;
	private String verb = null;
	private String ent2 = null;
	private boolean wantGeo = true;
	private boolean wantLinkdata = false;
	
	public SearchInterface(Context context, Request request, Response response) 
	{
		 super(context, request, response);
		 cookie = request.getCookies().getFirstValue("infinitecookie",true);		 
		 Map<String,Object> attributes = request.getAttributes();
		 String urlStr = request.getResourceRef().toString();
		 
		 if ( urlStr.contains("knowledge/searchSuggest") || urlStr.contains("feature/entitySuggest/"))
		 {
			 action = "suggest";
			 term = RESTTools.decodeRESTParam("term", attributes);
			 communityIdStrList = RESTTools.decodeRESTParam("communityids", attributes);
		 }
		 else if ( urlStr.contains("knowledge/searchEventSuggest") || urlStr.contains("feature/eventSuggest/") || urlStr.contains("feature/assocSuggest/"))
		 {
			 action ="suggestassoc";
			 ent1 = RESTTools.decodeRESTParam("ent1", attributes);
			 verb = RESTTools.decodeRESTParam("verb", attributes);
			 ent2 = RESTTools.decodeRESTParam("ent2", attributes);
			 field = RESTTools.decodeRESTParam("field", attributes);
			 communityIdStrList = RESTTools.decodeRESTParam("communityids", attributes);
		 }
		 else if ( urlStr.contains("knowledge/aliasSuggest/") || urlStr.contains("feature/aliasSuggest/"))
		 {
			 action = "alias";
			 communityIdStrList = RESTTools.decodeRESTParam("communityids", attributes);
			 field = RESTTools.decodeRESTParam("field", attributes);
			 term = RESTTools.decodeRESTParam("term", attributes);
		 }
		 else if ( urlStr.contains("feature/geoSuggest/"))
		 {
			 action = "suggestgeo";
			 term = RESTTools.decodeRESTParam("term", attributes);
		 }
		 
		 // All modifications of this resource
		 this.setModifiable(true);
		 
		 //turn off cookies for rss calls
		 Map<String, String> queryOptions = this.getQuery().getValuesMap();
		 String geo = queryOptions.get("geo");
		 if ((null != geo) && ( (geo.equals("0")) || (geo.equalsIgnoreCase("false")) )) {
			 wantGeo = false;			 
		 }
		 String linkdata = queryOptions.get("linkdata");
		 if ((null != linkdata) && ( (linkdata.equals("1")) || (linkdata.equalsIgnoreCase("true")) )) {
			 wantLinkdata = true;
		 }
		 getVariants().add(new Variant(MediaType.APPLICATION_JSON));
	}
	
	/**
	 * Represent the user object in the requested format.
	 * 
	 * @param variant
	 * @return
	 * @throws ResourceException
	 */
	public Representation represent(Variant variant) throws ResourceException 
	{
	
		 String data = "";
		 MediaType mediaType = MediaType.APPLICATION_JSON;
		 ResponsePojo rp = new ResponsePojo();

		 cookieLookup = RESTTools.cookieLookup(cookie);
		 if ( cookieLookup == null )
		 {
			 rp = new ResponsePojo();
			 rp.setResponse(new ResponseObject("Cookie Lookup",false,"Cookie session expired or never existed, please login first"));
		 }
		 else {
			 boolean validGroups = RESTTools.validateCommunityIds(cookieLookup, communityIdStrList); //every call needs communityid so check now
			 if ( validGroups == false )
			 {
				 rp = new ResponsePojo();
				 rp.setResponse(new ResponseObject("Verifying Communities",false,"Community Ids are not valid for this user"));
			 }
			 else
			 {
				 if ( action.equals("suggest"))
				 {
					 rp = this.search.getSuggestions(cookieLookup, term, communityIdStrList, wantGeo, wantLinkdata);
				 }
				 else if ( action.equals("suggestassoc"))
				 {
					 rp = this.search.getAssociationSuggestions(cookieLookup, ent1, verb, ent2, field, communityIdStrList);					 
				 }
				 else if ( action.equals("suggestgeo"))
				 {
					 rp = this.search.getSuggestionsGeo(cookieLookup, term, communityIdStrList);					 
				 }
				 else if ( action.equals("alias"))
				 {
					 rp = this.search.getAliasSuggestions(cookieLookup, term, field, communityIdStrList);
				 }
			 }
		 } // (end if login valid)
		 data = rp.toApi();

		 return new StringRepresentation(data, mediaType);
	}
}
