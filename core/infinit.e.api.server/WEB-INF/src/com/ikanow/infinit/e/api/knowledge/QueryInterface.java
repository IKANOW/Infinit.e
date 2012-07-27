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

//TODO (INF-1516): Add new filters to GET interface

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.restlet.Request;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import com.google.gson.Gson;
import com.ikanow.infinit.e.api.authentication.PasswordEncryption;
import com.ikanow.infinit.e.api.knowledge.output.KmlOutput;
import com.ikanow.infinit.e.api.knowledge.output.RssOutput;
import com.ikanow.infinit.e.api.knowledge.output.XmlOutput;
import com.ikanow.infinit.e.api.utils.RESTTools;
import com.ikanow.infinit.e.data_model.api.ResponsePojo;
import com.ikanow.infinit.e.data_model.api.ResponsePojo.ResponseObject;
import com.ikanow.infinit.e.data_model.api.knowledge.AdvancedQueryPojo;
import com.ikanow.infinit.e.data_model.store.social.authentication.AuthenticationPojo;

// The resource handlers for Advanced Queries in beta

// The REST call comes in one of the following formats:
//
// GET <preamble>/knowledge/query/<communityIdList>?json=<encoded JSON object>
// GET <preamble>/knowledge/query/<communityIdList>?<query-url-parameters>
// or...
// POST<preamble>/knowledge/query/<communityIdList>
// <JSON object>

public class QueryInterface extends ServerResource 
{
	// Utility objects
	private QueryHandler _queryController = new QueryHandler();
	
	// Per-call transaction state
	String _cookie;
	String _ipAddress;
	String _communityIdStrList;
	String _queryJson = null;
	AdvancedQueryPojo _requestDetails = null;

	//___________________________________________________________________________________
	
	// Constructor/main processing 
	
	@Override
	public void doInit() 
	{
		 Request request = this.getRequest();
		
		// Some basic housekeeping
		 _cookie = request.getCookies().getFirstValue("infinitecookie",true);		 
		 _ipAddress =  request.getClientInfo().getAddress();

		 //Optional query object (else is a POST)
		 if (Method.POST == request.getMethod()) 
		 {
			 // (Parameters a bit different in POSTs)
			 
				 Map<String,Object> attributes = request.getAttributes();
				 _communityIdStrList = RESTTools.decodeRESTParam("communityids", attributes);
			 
			 // This is handled elsewhere, in acceptRepresentation, have faith.....
		 }
		 else 
		 {
			 // If we're in here, then we're in a query call, we don't support any others...
			 Map<String,Object> attributes = request.getAttributes();
			 _communityIdStrList = RESTTools.decodeRESTParam("communityids", attributes);
			 
			 Map<String, String> queryOptions = this.getQuery().getValuesMap();
			 _queryJson = queryOptions.get("json");
			 if (null == _queryJson) {
				 // Either a POST (see below) or the parameters are scattered across many URL parameters
				 parseUrlString(queryOptions);
				 	// ^^^ (creates and populates _requestDetails)
			 }
			 else {
				_requestDetails = QueryHandler.createQueryPojo(_queryJson);
			 }
		 }		 
	}//TESTED
	
	//___________________________________________________________________________________
	
	/**
	 * Handles a POST
	 * 
	 * @param entity
	 * @return
	 * @throws ResourceException
	 */

	@Post
	public Representation post(Representation entity) 
	{

		if (Method.POST == getRequest().getMethod()) 
		{
			try 
			{
				_queryJson = entity.getText();
				if ((null != _queryJson) && !_queryJson.isEmpty()) { // I suppose we see this for GETs?
					_requestDetails = QueryHandler.createQueryPojo(_queryJson);
				}
			}
			catch (IOException e) {
				getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			}
		}		 
		
		return get();
	}//TESTED
	
	//___________________________________________________________________________________
	
	/**
	 * Represent the user object in the requested format.
	 * 
	 * @param variant
	 * @return
	 * @throws ResourceException
	 */
	@Get
	public Representation get() 
	{
		StringBuffer errorString = new StringBuffer("Query error");
		String data = null;
		MediaType mediaType = MediaType.APPLICATION_JSON; // (or RSS, or XML)
		ResponsePojo rp = null;

		try {
			// First off, check the cookie is valid:
			String cookieLookup = null; 
			boolean bNeedCookie = !_requestDetails.output.format.equalsIgnoreCase("rss"); 
			// maybe don't need cookie for RSS?
			// Do a quick bit of further error checking here too:
			if (bNeedCookie) {
				if (!_requestDetails.output.format.equalsIgnoreCase("xml") &&
						!_requestDetails.output.format.equalsIgnoreCase("kml") &&
						!_requestDetails.output.format.equalsIgnoreCase("json"))
				{
					rp = new ResponsePojo();
					rp.setResponse(new ResponseObject("Output Format", false, "Unsupported output.format"));						
					data = rp.toApi();					
					return new StringRepresentation(data, mediaType);
				}
			}
			
			// Perform cookie lookup			
			cookieLookup = RESTTools.cookieLookup(_cookie);
			
			// Fail out otherwise perform query (not yet for RSS)
			
			if ((cookieLookup == null) && bNeedCookie)
			{
				rp = new ResponsePojo();
				rp.setResponse(new ResponseObject("Cookie Lookup", false, "Cookie session expired or never existed, please login first"));
				data = rp.toApi();
			}
			else 
			{
				// Next, assuming success with cookies process the query
				if (bNeedCookie) 
				{
					//check communities are valid before using
					if ( RESTTools.validateCommunityIds(cookieLookup, _communityIdStrList) )
						rp = _queryController.doQuery(cookieLookup, _requestDetails, _communityIdStrList, errorString);
					else {
						errorString.append(": Community Ids are not valid for this user");
					}
				}
				// (A bit badly written - for RSS don't expend energy yet)
				// for RSS make the assumption that the URL was generated by our system
				// and the user and community ids are correct
				// might want to revisit and force security on RSS (needs more thought)
				else 
				{
					rp = _queryController.doQuery(cookieLookup, _requestDetails, _communityIdStrList, errorString);
				}
				
				
				//if (bNeedCookie && (null == rp)) { // Error handling
				if (null == rp) { // Error handling including RSS
					rp = new ResponsePojo();
					rp.setResponse(new ResponseObject("Query Format", false, errorString.toString()));					
					data = rp.toApi();
				}
				
				else { // Valid response, output handle all output formats 

					// Output type
					// JSON
					//if (null != _requestDetails.output || _requestDetails.output.format.equalsIgnoreCase("json")) {
					// Modified based on logic (never able to get to xml or rss based on above logic)
					if (null == _requestDetails.output.format || _requestDetails.output.format.equalsIgnoreCase("json")) {
						data = rp.toApi();
					}
					else if (_requestDetails.output.format.equalsIgnoreCase("xml")) { // XML
						mediaType = MediaType.APPLICATION_XML;
						// Output type
						// Xml
						XmlOutput xml = new XmlOutput();
						data = xml.getFeeds(rp);
						
					}
					else if(_requestDetails.output.format.equalsIgnoreCase("kml")) {
						mediaType = MediaType.APPLICATION_XML;
						// Output type
						// Kml
						KmlOutput kml = new KmlOutput();
						data = kml.getDocs(rp);
					}
					else if (_requestDetails.output.format.equalsIgnoreCase("rss")) { // RSS
						
						// Authentication:
						if (null == cookieLookup) {
							Map<String, String> queryOptions = this.getQuery().getValuesMap();
							String sKey = queryOptions.get("key");
							String sKeyCmp = null;
							if (null != sKey) { // Key allowed to be 1 or 2 things: hash of query or password...
								sKeyCmp = PasswordEncryption.encrypt(this._queryJson); //encrypt
							}
							if ((null == sKeyCmp) || !sKeyCmp.equals(sKey)) {
								// User/password also allowed, TBD this will require SSL
								String user = queryOptions.get("user");
								String password = queryOptions.get("password");
								AuthenticationPojo authuser = null;
								if ((null != user) && (null != password)) {
									authuser = PasswordEncryption.validateUser(user,password, false);
								}
								if ( authuser == null )
								{
									// Don't have either authentication or key, bomb out...
									rp = new ResponsePojo();
									rp.setResponse(new ResponseObject("Cookie Lookup", false, "Cookie session expired or never existed, please login first or use valid key or user/pass"));
									data = rp.toApi();		
									mediaType = MediaType.APPLICATION_JSON;
									return new StringRepresentation(data, mediaType);
								}
							}
						} // end authentication:
						
						mediaType = MediaType.APPLICATION_XML;
						RssOutput rss = new RssOutput();
						
						// print out the rss since we know that the response is not null
						data = rss.getDocs(rp);							
					}
					else { // Not pleasant after all this just to return an error :(
						rp = new ResponsePojo();
						rp.setResponse(new ResponseObject("Output Format", false, "Unsupported output.format"));						
						data = rp.toApi();
					}
				}
			}//TOTEST
		}
		catch (Exception e) {
			e.printStackTrace();
			getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			rp = new ResponsePojo();
			rp.setResponse(new ResponseObject("Query", false, errorString.toString()));
			System.out.println(rp.toApi());
			data = rp.toApi();
		}
		
		// One last check to ensure data has value (ugly ugly ugly)
		if (data == null ) {
			rp = new ResponsePojo();
			rp.setResponse(new ResponseObject("Query", false, errorString.toString()));
			System.out.println(rp.toApi());
			data = rp.toApi();
		}
		return new StringRepresentation(data, mediaType);
	}		
	//___________________________________________________________________________________
	
	// Utility function - long and dull, I advise stopping reading around here...!
	
	// Probably would like to write a YAML handler for this (duh) but the qt[N] is non-standard I think so leave it like
	// this for the moment...
	
	private void parseUrlString(Map<String,String> attr) {
		
		_requestDetails = new AdvancedQueryPojo();
		
		TreeMap<Integer, AdvancedQueryPojo.QueryTermPojo> qt = 
			new TreeMap<Integer, AdvancedQueryPojo.QueryTermPojo>();
		TreeMap<Integer, AdvancedQueryPojo.QueryInputPojo.TypeAndTagTermPojo> tt = 
			new TreeMap<Integer, AdvancedQueryPojo.QueryInputPojo.TypeAndTagTermPojo>();
		
		// And off we go through the different options...
		
		for (Entry<String, String> entry: attr.entrySet()) {		
			String attrName = entry.getKey().toLowerCase();
			
			String value = RESTTools.decodeURL((String)entry.getValue());
			
			if ('"' == value.charAt(0)) { // Handle quotes:
				value = value.substring(1, value.length() - 1);
			}			
			else if ('\'' == value.charAt(0)) { // Handle quotes:
				value = value.substring(1, value.length() - 1);
			}			
// Query			
			if (attrName.startsWith("qt[")) {
				int npos = attrName.indexOf(']');				
				if (npos >= 0) {
					String index = attrName.substring(3, npos);		
					int nIndex = Integer.parseInt(index);
					
					// Put in an ordered map that will get pasted into the query map
					AdvancedQueryPojo.QueryTermPojo qtIndex = qt.get(nIndex);
					if (null == qtIndex) {
						qtIndex = new AdvancedQueryPojo.QueryTermPojo();
						qt.put(nIndex, qtIndex);
					}
					attrName = attrName.substring(npos + 2); // +'.'

					// Parameters
					if (attrName.equals("ftext")) {
						qtIndex.ftext = value;
					}
					else if (attrName.equals("etext")) {
						qtIndex.etext = value;
					}
					else if (attrName.equals("entity")) {
						qtIndex.entity = value;						
					}
					else if (attrName.equals("entityvalue")) {
						qtIndex.entityValue = value;						
					}
					else if (attrName.equals("entitytype")) {
						qtIndex.entityType = value;						
					}
					else if (attrName.equals("entityopt.expandalias")) {
						if (null == qtIndex.entityOpt) {
							qtIndex.entityOpt = new AdvancedQueryPojo.QueryTermPojo.EntityOptionPojo();
						}
						qtIndex.entityOpt.expandAlias = Boolean.parseBoolean(value);
					}
					else if (attrName.equals("entityopt.expandontology")) {
						if (null == qtIndex.entityOpt) {
							qtIndex.entityOpt = new AdvancedQueryPojo.QueryTermPojo.EntityOptionPojo();
						}
						qtIndex.entityOpt.expandOntology = Boolean.parseBoolean(value);						
					}
					else if (attrName.equals("time.min")) {
						if (null == qtIndex.time) {
							qtIndex.time = new AdvancedQueryPojo.QueryTermPojo.TimeTermPojo();
						}
						qtIndex.time.min = value;
					}
					else if (attrName.equals("time.max")) {
						if (null == qtIndex.time) {
							qtIndex.time = new AdvancedQueryPojo.QueryTermPojo.TimeTermPojo();
						}
						qtIndex.time.max = value;						
					}
					else if (attrName.equals("geo.centerll")) {
						if (null == qtIndex.geo) {
							qtIndex.geo = new AdvancedQueryPojo.QueryTermPojo.GeoTermPojo();
						}						
						qtIndex.geo.centerll = value;						
					}
					else if (attrName.equals("geo.dist")) {
						if (null == qtIndex.geo) {
							qtIndex.geo = new AdvancedQueryPojo.QueryTermPojo.GeoTermPojo();
						}						
						qtIndex.geo.dist = value;												
					}
					else if (attrName.equals("geo.minll")) {
						if (null == qtIndex.geo) {
							qtIndex.geo = new AdvancedQueryPojo.QueryTermPojo.GeoTermPojo();
						}						
						qtIndex.geo.minll = value;						
					}
					else if (attrName.equals("geo.maxll")) {
						if (null == qtIndex.geo) {
							qtIndex.geo = new AdvancedQueryPojo.QueryTermPojo.GeoTermPojo();
						}						
						qtIndex.geo.maxll = value;						
					}
					else if (attrName.equals("geo.name")) {
						if (null == qtIndex.geo) {
							qtIndex.geo = new AdvancedQueryPojo.QueryTermPojo.GeoTermPojo();
						}						
						qtIndex.geo.name = value;						
					}
					else if (attrName.equals("geo.polys")) {
						if (null == qtIndex.geo) {
							qtIndex.geo = new AdvancedQueryPojo.QueryTermPojo.GeoTermPojo();
						}						
						qtIndex.geo.polys = value;						
					}
					else if (attrName.equals("geo.onttype")) {
						if (null == qtIndex.geo) {
							qtIndex.geo = new AdvancedQueryPojo.QueryTermPojo.GeoTermPojo();
						}						
						qtIndex.geo.ontology_type = value;						
					}
					else if (attrName.equals("metadataField")) {
						qtIndex.metadataField = value;						
					}
					//association parsing code:
					else if (attrName.equals("event.entity1.etext")) {
						if (null == qtIndex.event) {
							qtIndex.event = new AdvancedQueryPojo.QueryTermPojo.AssociationTermPojo();
						}
						if (null == qtIndex.event.entity1) {
							qtIndex.event.entity1 = new AdvancedQueryPojo.QueryTermPojo();
						}
						qtIndex.event.entity1.etext = value;
					}
					else if (attrName.equals("event.entity1.ftext")) {
						if (null == qtIndex.event) {
							qtIndex.event = new AdvancedQueryPojo.QueryTermPojo.AssociationTermPojo();
						}
						if (null == qtIndex.event.entity1) {
							qtIndex.event.entity1 = new AdvancedQueryPojo.QueryTermPojo();
						}
						qtIndex.event.entity1.ftext = value;
					}
					else if (attrName.equals("event.entity1.entity")) {
						if (null == qtIndex.event) {
							qtIndex.event = new AdvancedQueryPojo.QueryTermPojo.AssociationTermPojo();
						}
						if (null == qtIndex.event.entity1) {
							qtIndex.event.entity1 = new AdvancedQueryPojo.QueryTermPojo();
						}
						qtIndex.event.entity1.entity = value;
					}
					else if (attrName.equals("event.entity1.entityValue")) {
						if (null == qtIndex.event) {
							qtIndex.event = new AdvancedQueryPojo.QueryTermPojo.AssociationTermPojo();
						}
						if (null == qtIndex.event.entity1) {
							qtIndex.event.entity1 = new AdvancedQueryPojo.QueryTermPojo();
						}
						qtIndex.event.entity1.entityValue = value;
					}
					else if (attrName.equals("event.entity1.entityType")) {
						if (null == qtIndex.event) {
							qtIndex.event = new AdvancedQueryPojo.QueryTermPojo.AssociationTermPojo();
						}
						if (null == qtIndex.event.entity1) {
							qtIndex.event.entity1 = new AdvancedQueryPojo.QueryTermPojo();
						}
						qtIndex.event.entity1.entityType = value;
					}
					//(No entity opts supported for the moment)
					else if (attrName.equals("event.entity2.etext")) {
						if (null == qtIndex.event) {
							qtIndex.event = new AdvancedQueryPojo.QueryTermPojo.AssociationTermPojo();
						}
						if (null == qtIndex.event.entity2) {
							qtIndex.event.entity2 = new AdvancedQueryPojo.QueryTermPojo();
						}
						qtIndex.event.entity2.etext = value;
					}
					else if (attrName.equals("event.entity2.ftext")) {
						if (null == qtIndex.event) {
							qtIndex.event = new AdvancedQueryPojo.QueryTermPojo.AssociationTermPojo();
						}
						if (null == qtIndex.event.entity2) {
							qtIndex.event.entity2 = new AdvancedQueryPojo.QueryTermPojo();
						}
						qtIndex.event.entity2.ftext = value;
					}
					else if (attrName.equals("event.entity2.entity")) {
						if (null == qtIndex.event) {
							qtIndex.event = new AdvancedQueryPojo.QueryTermPojo.AssociationTermPojo();
						}
						if (null == qtIndex.event.entity2) {
							qtIndex.event.entity2 = new AdvancedQueryPojo.QueryTermPojo();
						}
						qtIndex.event.entity2.entity = value;
					}
					else if (attrName.equals("event.entity2.entityValue")) {
						if (null == qtIndex.event) {
							qtIndex.event = new AdvancedQueryPojo.QueryTermPojo.AssociationTermPojo();
						}
						if (null == qtIndex.event.entity2) {
							qtIndex.event.entity2 = new AdvancedQueryPojo.QueryTermPojo();
						}
						qtIndex.event.entity2.entityValue = value;
					}
					else if (attrName.equals("event.entity2.entityType")) {
						if (null == qtIndex.event) {
							qtIndex.event = new AdvancedQueryPojo.QueryTermPojo.AssociationTermPojo();
						}
						if (null == qtIndex.event.entity2) {
							qtIndex.event.entity2 = new AdvancedQueryPojo.QueryTermPojo();
						}
						qtIndex.event.entity2.entityType = value;
					}
					//(No entity opts supported for the moment)
					else if (attrName.equals("event.verb")) {
						if (null == qtIndex.event) {
							qtIndex.event = new AdvancedQueryPojo.QueryTermPojo.AssociationTermPojo();
						}
						qtIndex.event.verb = value;
					}
					else if (attrName.equals("event.type")) {
						if (null == qtIndex.event) {
							qtIndex.event = new AdvancedQueryPojo.QueryTermPojo.AssociationTermPojo();
						}
						qtIndex.event.type = value;
					}
					else if (attrName.equals("event.time.min")) {
						if (null == qtIndex.event) {
							qtIndex.event = new AdvancedQueryPojo.QueryTermPojo.AssociationTermPojo();
						}
						if (null == qtIndex.event.time) {
							qtIndex.event.time = new AdvancedQueryPojo.QueryTermPojo.TimeTermPojo();
						}
						qtIndex.event.time.min = value;
					}
					else if (attrName.equals("event.time.max")) {
						if (null == qtIndex.event) {
							qtIndex.event = new AdvancedQueryPojo.QueryTermPojo.AssociationTermPojo();
						}
						if (null == qtIndex.event.time) {
							qtIndex.event.time = new AdvancedQueryPojo.QueryTermPojo.TimeTermPojo();
						}
						qtIndex.event.time.max = value;						
					}
					else if (attrName.equals("event.geo.centerll")) {
						if (null == qtIndex.event) {
							qtIndex.event = new AdvancedQueryPojo.QueryTermPojo.AssociationTermPojo();
						}
						if (null == qtIndex.event.geo) {
							qtIndex.event.geo = new AdvancedQueryPojo.QueryTermPojo.GeoTermPojo();
						}						
						qtIndex.event.geo.centerll = value;						
					}
					else if (attrName.equals("event.geo.dist")) {
						if (null == qtIndex.event) {
							qtIndex.event = new AdvancedQueryPojo.QueryTermPojo.AssociationTermPojo();
						}
						if (null == qtIndex.event.geo) {
							qtIndex.event.geo = new AdvancedQueryPojo.QueryTermPojo.GeoTermPojo();
						}						
						qtIndex.event.geo.dist = value;												
					}
					else if (attrName.equals("event.geo.minll")) {
						if (null == qtIndex.event) {
							qtIndex.event = new AdvancedQueryPojo.QueryTermPojo.AssociationTermPojo();
						}
						if (null == qtIndex.event.geo) {
							qtIndex.event.geo = new AdvancedQueryPojo.QueryTermPojo.GeoTermPojo();
						}						
						qtIndex.event.geo.minll = value;						
					}
					else if (attrName.equals("event.geo.maxll")) {
						if (null == qtIndex.event) {
							qtIndex.event = new AdvancedQueryPojo.QueryTermPojo.AssociationTermPojo();
						}
						if (null == qtIndex.event.geo) {
							qtIndex.event.geo = new AdvancedQueryPojo.QueryTermPojo.GeoTermPojo();
						}						
						qtIndex.event.geo.maxll = value;						
					}
					//^^^(event have been renamed associations, see below - want to retire the "event" name for V1)
					else if (attrName.equals("assoc.entity1.etext")) {
						if (null == qtIndex.assoc) {
							qtIndex.assoc = new AdvancedQueryPojo.QueryTermPojo.AssociationTermPojo();
						}
						if (null == qtIndex.assoc.entity1) {
							qtIndex.assoc.entity1 = new AdvancedQueryPojo.QueryTermPojo();
						}
						qtIndex.assoc.entity1.etext = value;
					}
					else if (attrName.equals("assoc.entity1.ftext")) {
						if (null == qtIndex.assoc) {
							qtIndex.assoc = new AdvancedQueryPojo.QueryTermPojo.AssociationTermPojo();
						}
						if (null == qtIndex.assoc.entity1) {
							qtIndex.assoc.entity1 = new AdvancedQueryPojo.QueryTermPojo();
						}
						qtIndex.assoc.entity1.ftext = value;
					}
					else if (attrName.equals("assoc.entity1.entity")) {
						if (null == qtIndex.assoc) {
							qtIndex.assoc = new AdvancedQueryPojo.QueryTermPojo.AssociationTermPojo();
						}
						if (null == qtIndex.assoc.entity1) {
							qtIndex.assoc.entity1 = new AdvancedQueryPojo.QueryTermPojo();
						}
						qtIndex.assoc.entity1.entity = value;
					}
					else if (attrName.equals("assoc.entity1.entityValue")) {
						if (null == qtIndex.assoc) {
							qtIndex.assoc = new AdvancedQueryPojo.QueryTermPojo.AssociationTermPojo();
						}
						if (null == qtIndex.assoc.entity1) {
							qtIndex.assoc.entity1 = new AdvancedQueryPojo.QueryTermPojo();
						}
						qtIndex.assoc.entity1.entityValue = value;
					}
					else if (attrName.equals("assoc.entity1.entityType")) {
						if (null == qtIndex.assoc) {
							qtIndex.assoc = new AdvancedQueryPojo.QueryTermPojo.AssociationTermPojo();
						}
						if (null == qtIndex.assoc.entity1) {
							qtIndex.assoc.entity1 = new AdvancedQueryPojo.QueryTermPojo();
						}
						qtIndex.assoc.entity1.entityType = value;
					}
					//(No entity opts supported for the moment)
					else if (attrName.equals("assoc.entity2.etext")) {
						if (null == qtIndex.assoc) {
							qtIndex.assoc = new AdvancedQueryPojo.QueryTermPojo.AssociationTermPojo();
						}
						if (null == qtIndex.assoc.entity2) {
							qtIndex.assoc.entity2 = new AdvancedQueryPojo.QueryTermPojo();
						}
						qtIndex.assoc.entity2.etext = value;
					}
					else if (attrName.equals("assoc.entity2.ftext")) {
						if (null == qtIndex.assoc) {
							qtIndex.assoc = new AdvancedQueryPojo.QueryTermPojo.AssociationTermPojo();
						}
						if (null == qtIndex.assoc.entity2) {
							qtIndex.assoc.entity2 = new AdvancedQueryPojo.QueryTermPojo();
						}
						qtIndex.assoc.entity2.ftext = value;
					}
					else if (attrName.equals("assoc.entity2.entity")) {
						if (null == qtIndex.assoc) {
							qtIndex.assoc = new AdvancedQueryPojo.QueryTermPojo.AssociationTermPojo();
						}
						if (null == qtIndex.assoc.entity2) {
							qtIndex.assoc.entity2 = new AdvancedQueryPojo.QueryTermPojo();
						}
						qtIndex.assoc.entity2.entity = value;
					}
					else if (attrName.equals("assoc.entity2.entityValue")) {
						if (null == qtIndex.assoc) {
							qtIndex.assoc = new AdvancedQueryPojo.QueryTermPojo.AssociationTermPojo();
						}
						if (null == qtIndex.assoc.entity2) {
							qtIndex.assoc.entity2 = new AdvancedQueryPojo.QueryTermPojo();
						}
						qtIndex.assoc.entity2.entityValue = value;
					}
					else if (attrName.equals("assoc.entity2.entityType")) {
						if (null == qtIndex.assoc) {
							qtIndex.assoc = new AdvancedQueryPojo.QueryTermPojo.AssociationTermPojo();
						}
						if (null == qtIndex.assoc.entity2) {
							qtIndex.assoc.entity2 = new AdvancedQueryPojo.QueryTermPojo();
						}
						qtIndex.assoc.entity2.entityType = value;
					}
					//(No entity opts supported for the moment)
					else if (attrName.equals("assoc.verb")) {
						if (null == qtIndex.assoc) {
							qtIndex.assoc = new AdvancedQueryPojo.QueryTermPojo.AssociationTermPojo();
						}
						qtIndex.assoc.verb = value;
					}
					else if (attrName.equals("assoc.type")) {
						if (null == qtIndex.assoc) {
							qtIndex.assoc = new AdvancedQueryPojo.QueryTermPojo.AssociationTermPojo();
						}
						qtIndex.assoc.type = value;
					}
					else if (attrName.equals("assoc.time.min")) {
						if (null == qtIndex.assoc) {
							qtIndex.assoc = new AdvancedQueryPojo.QueryTermPojo.AssociationTermPojo();
						}
						if (null == qtIndex.assoc.time) {
							qtIndex.assoc.time = new AdvancedQueryPojo.QueryTermPojo.TimeTermPojo();
						}
						qtIndex.assoc.time.min = value;
					}
					else if (attrName.equals("assoc.time.max")) {
						if (null == qtIndex.assoc) {
							qtIndex.assoc = new AdvancedQueryPojo.QueryTermPojo.AssociationTermPojo();
						}
						if (null == qtIndex.assoc.time) {
							qtIndex.assoc.time = new AdvancedQueryPojo.QueryTermPojo.TimeTermPojo();
						}
						qtIndex.assoc.time.max = value;						
					}
					else if (attrName.equals("assoc.geo.centerll")) {
						if (null == qtIndex.assoc) {
							qtIndex.assoc = new AdvancedQueryPojo.QueryTermPojo.AssociationTermPojo();
						}
						if (null == qtIndex.assoc.geo) {
							qtIndex.assoc.geo = new AdvancedQueryPojo.QueryTermPojo.GeoTermPojo();
						}						
						qtIndex.assoc.geo.centerll = value;						
					}
					else if (attrName.equals("assoc.geo.dist")) {
						if (null == qtIndex.assoc) {
							qtIndex.assoc = new AdvancedQueryPojo.QueryTermPojo.AssociationTermPojo();
						}
						if (null == qtIndex.assoc.geo) {
							qtIndex.assoc.geo = new AdvancedQueryPojo.QueryTermPojo.GeoTermPojo();
						}						
						qtIndex.assoc.geo.dist = value;												
					}
					else if (attrName.equals("assoc.geo.minll")) {
						if (null == qtIndex.assoc) {
							qtIndex.assoc = new AdvancedQueryPojo.QueryTermPojo.AssociationTermPojo();
						}
						if (null == qtIndex.assoc.geo) {
							qtIndex.assoc.geo = new AdvancedQueryPojo.QueryTermPojo.GeoTermPojo();
						}						
						qtIndex.assoc.geo.minll = value;						
					}
					else if (attrName.equals("assoc.geo.maxll")) {
						if (null == qtIndex.assoc) {
							qtIndex.assoc = new AdvancedQueryPojo.QueryTermPojo.AssociationTermPojo();
						}
						if (null == qtIndex.assoc.geo) {
							qtIndex.assoc.geo = new AdvancedQueryPojo.QueryTermPojo.GeoTermPojo();
						}						
						qtIndex.assoc.geo.maxll = value;						
					}
					
				}
			}
			else if (attrName.equals("logic")) {
				_requestDetails.logic = value;
			}
			else if (attrName.equals("raw")) {
				_requestDetails.raw = new AdvancedQueryPojo.QueryRawPojo(value);
			}
// Input			
			else if (attrName.equals("input.name")) {
				if (null == _requestDetails.input) {
					_requestDetails.input = new AdvancedQueryPojo.QueryInputPojo();
				}
				_requestDetails.input.name = value;
			}
			else if (attrName.equals("input.tags")) { // comma-separated list 
				if (null == _requestDetails.input) {
					_requestDetails.input = new AdvancedQueryPojo.QueryInputPojo();
				}
				_requestDetails.input.tags = Arrays.asList(value.toLowerCase().split("\\s*,\\s*"));
			}
			else if (attrName.startsWith("input.typeandtags[")) { 
				if (null == _requestDetails.input) {
					_requestDetails.input = new AdvancedQueryPojo.QueryInputPojo();
				}
				int npos = attrName.indexOf(']');				
				if (npos >= 0) {
					String index = attrName.substring(18, npos);		
					int nIndex = Integer.parseInt(index);
					
					// Put in an ordered map that will get pasted into the query map
					AdvancedQueryPojo.QueryInputPojo.TypeAndTagTermPojo ttIndex = tt.get(nIndex);
					if (null == ttIndex) {
						ttIndex = new AdvancedQueryPojo.QueryInputPojo.TypeAndTagTermPojo();
						tt.put(nIndex, ttIndex);
					}
					attrName = attrName.substring(npos + 2); // +'.'

					// Parameters
					if (attrName.equals("type")) {
						ttIndex.type = value;
					}
					if (attrName.equals("tags")) { // comma-separated list
						ttIndex.tags = Arrays.asList(value.toLowerCase().split("\\s*,\\s*"));;
					}
				}
			}
			else if (attrName.equals("input.sources")) { // comma-separated list
				if (null == _requestDetails.input) {
					_requestDetails.input = new AdvancedQueryPojo.QueryInputPojo();
				}
				_requestDetails.input.sources = Arrays.asList(value.split("\\s*,\\s*"));
			}
			else if (attrName.equals("input.srcinclude")) { 
				if (null == _requestDetails.input) {
					_requestDetails.input = new AdvancedQueryPojo.QueryInputPojo();
				}
				_requestDetails.input.srcInclude = Boolean.parseBoolean(value);				
			}
// Score			
			else if (attrName.equals("score.numanalyze")) {
				if (null == _requestDetails.score) {
					_requestDetails.score = new AdvancedQueryPojo.QueryScorePojo();
				}
				_requestDetails.score.numAnalyze = Integer.parseInt(value);
			}
			else if (attrName.equals("score.sigweight")) {
				if (null == _requestDetails.score) {
					_requestDetails.score = new AdvancedQueryPojo.QueryScorePojo();
				}
				_requestDetails.score.sigWeight = Double.parseDouble(value);				
			}
			else if (attrName.equals("score.relweight")) {
				if (null == _requestDetails.score) {
					_requestDetails.score = new AdvancedQueryPojo.QueryScorePojo();
				}
				_requestDetails.score.relWeight = Double.parseDouble(value);								
			}
			else if (attrName.equals("score.timeprox.time")) {
				if (null == _requestDetails.score) {
					_requestDetails.score = new AdvancedQueryPojo.QueryScorePojo();
				}
				if (null == _requestDetails.score.timeProx) {
					_requestDetails.score.timeProx = new AdvancedQueryPojo.QueryScorePojo.TimeProxTermPojo();
				}
				_requestDetails.score.timeProx.time = value;
			}
			else if (attrName.equals("score.timeprox.decay")) {
				if (null == _requestDetails.score) {
					_requestDetails.score = new AdvancedQueryPojo.QueryScorePojo();
				}
				if (null == _requestDetails.score.timeProx) {
					_requestDetails.score.timeProx = new AdvancedQueryPojo.QueryScorePojo.TimeProxTermPojo();
				}
				_requestDetails.score.timeProx.decay = value;				
			}
			else if (attrName.equals("score.geoprox.ll")) {
				if (null == _requestDetails.score) {
					_requestDetails.score = new AdvancedQueryPojo.QueryScorePojo();
				}
				if (null == _requestDetails.score.geoProx) {
					_requestDetails.score.geoProx = new AdvancedQueryPojo.QueryScorePojo.GeoProxTermPojo();
				}
				_requestDetails.score.geoProx.ll = value;								
			}
			else if (attrName.equals("score.geoprox.decay")) {
				if (null == _requestDetails.score) {
					_requestDetails.score = new AdvancedQueryPojo.QueryScorePojo();
				}
				if (null == _requestDetails.score.geoProx) {
					_requestDetails.score.geoProx = new AdvancedQueryPojo.QueryScorePojo.GeoProxTermPojo();
				}
				_requestDetails.score.geoProx.decay = value;												
			}
// Output			
			else if (attrName.equals("output.format")) {
				if (null == _requestDetails.output) {
					_requestDetails.output = new AdvancedQueryPojo.QueryOutputPojo();
				}
				_requestDetails.output.format = value;
			}
			//TODO (INF-475): Test cases for all these
			else if (attrName.equals("output.docs.enable")) {
				if (null == _requestDetails.output) {
					_requestDetails.output = new AdvancedQueryPojo.QueryOutputPojo();
				}
				if (null == _requestDetails.output.docs) {
					_requestDetails.output.docs = new AdvancedQueryPojo.QueryOutputPojo.DocumentOutputPojo();
				}
				_requestDetails.output.docs.enable = Boolean.parseBoolean(value);				
			}
			else if (attrName.equals("output.docs.eventstimeline")) {
				if (null == _requestDetails.output) {
					_requestDetails.output = new AdvancedQueryPojo.QueryOutputPojo();
				}
				if (null == _requestDetails.output.docs) {
					_requestDetails.output.docs = new AdvancedQueryPojo.QueryOutputPojo.DocumentOutputPojo();
				}
				_requestDetails.output.docs.eventsTimeline = Boolean.parseBoolean(value);				
			}
			else if (attrName.equals("output.docs.numreturn")) {
				if (null == _requestDetails.output) {
					_requestDetails.output = new AdvancedQueryPojo.QueryOutputPojo();
				}
				if (null == _requestDetails.output.docs) {
					_requestDetails.output.docs = new AdvancedQueryPojo.QueryOutputPojo.DocumentOutputPojo();
				}
				_requestDetails.output.docs.numReturn = Integer.parseInt(value);
			}
			else if (attrName.equals("output.docs.numeventstimelinereturn")) {
				if (null == _requestDetails.output) {
					_requestDetails.output = new AdvancedQueryPojo.QueryOutputPojo();
				}
				if (null == _requestDetails.output.docs) {
					_requestDetails.output.docs = new AdvancedQueryPojo.QueryOutputPojo.DocumentOutputPojo();
				}
				_requestDetails.output.docs.numEventsTimelineReturn = Integer.parseInt(value);
			}
			else if (attrName.equals("output.docs.skip")) {
				if (null == _requestDetails.output) {
					_requestDetails.output = new AdvancedQueryPojo.QueryOutputPojo();
				}
				if (null == _requestDetails.output.docs) {
					_requestDetails.output.docs = new AdvancedQueryPojo.QueryOutputPojo.DocumentOutputPojo();
				}
				_requestDetails.output.docs.skip = Integer.parseInt(value);			
			}
			else if (attrName.equals("output.docs.ents")) {
				if (null == _requestDetails.output) {
					_requestDetails.output = new AdvancedQueryPojo.QueryOutputPojo();
				}
				if (null == _requestDetails.output.docs) {
					_requestDetails.output.docs = new AdvancedQueryPojo.QueryOutputPojo.DocumentOutputPojo();
				}
				_requestDetails.output.docs.ents = Boolean.parseBoolean(value);				
			}
			else if (attrName.equals("output.docs.geo")) {
				if (null == _requestDetails.output) {
					_requestDetails.output = new AdvancedQueryPojo.QueryOutputPojo();
				}
				if (null == _requestDetails.output.docs) {
					_requestDetails.output.docs = new AdvancedQueryPojo.QueryOutputPojo.DocumentOutputPojo();
				}
				_requestDetails.output.docs.geo = Boolean.parseBoolean(value);				
			}
			else if (attrName.equals("output.docs.events")) {
				if (null == _requestDetails.output) {
					_requestDetails.output = new AdvancedQueryPojo.QueryOutputPojo();
				}
				if (null == _requestDetails.output.docs) {
					_requestDetails.output.docs = new AdvancedQueryPojo.QueryOutputPojo.DocumentOutputPojo();
				}
				_requestDetails.output.docs.events = Boolean.parseBoolean(value);				
			}
			else if (attrName.equals("output.docs.facts")) {
				if (null == _requestDetails.output) {
					_requestDetails.output = new AdvancedQueryPojo.QueryOutputPojo();
					_requestDetails.output.docs = new AdvancedQueryPojo.QueryOutputPojo.DocumentOutputPojo();
				}
				if (null == _requestDetails.output.docs) {
					_requestDetails.output.docs = new AdvancedQueryPojo.QueryOutputPojo.DocumentOutputPojo();
				}
				_requestDetails.output.docs.facts = Boolean.parseBoolean(value);				
			}
			else if (attrName.equals("output.docs.summaries")) {
				if (null == _requestDetails.output) {
					_requestDetails.output = new AdvancedQueryPojo.QueryOutputPojo();
					_requestDetails.output.docs = new AdvancedQueryPojo.QueryOutputPojo.DocumentOutputPojo();
				}
				if (null == _requestDetails.output.docs) {
					_requestDetails.output.docs = new AdvancedQueryPojo.QueryOutputPojo.DocumentOutputPojo();
				}
				_requestDetails.output.docs.summaries = Boolean.parseBoolean(value);				
			}
			else if (attrName.equals("output.docs.metadata")) {
				if (null == _requestDetails.output) {
					_requestDetails.output = new AdvancedQueryPojo.QueryOutputPojo();
					_requestDetails.output.docs = new AdvancedQueryPojo.QueryOutputPojo.DocumentOutputPojo();
				}
				if (null == _requestDetails.output.docs) {
					_requestDetails.output.docs = new AdvancedQueryPojo.QueryOutputPojo.DocumentOutputPojo();
				}
				_requestDetails.output.docs.metadata = Boolean.parseBoolean(value);				
			}
			else if (attrName.equals("output.aggregation.geonumreturn")) {
				if (null == _requestDetails.output) {
					_requestDetails.output = new AdvancedQueryPojo.QueryOutputPojo();
				}
				if (null == _requestDetails.output.aggregation) {
					_requestDetails.output.aggregation = new AdvancedQueryPojo.QueryOutputPojo.AggregationOutputPojo();
				}
				_requestDetails.output.aggregation.geoNumReturn = Integer.parseInt(value);				
			}
			else if (attrName.equals("output.aggregation.timesinterval")) {
				if (null == _requestDetails.output) {
					_requestDetails.output = new AdvancedQueryPojo.QueryOutputPojo();
				}
				if (null == _requestDetails.output.aggregation) {
					_requestDetails.output.aggregation = new AdvancedQueryPojo.QueryOutputPojo.AggregationOutputPojo();
				}
				_requestDetails.output.aggregation.timesInterval = value;				
			}
			else if (attrName.equals("output.aggregation.entsnumreturn")) {
				if (null == _requestDetails.output) {
					_requestDetails.output = new AdvancedQueryPojo.QueryOutputPojo();
				}
				if (null == _requestDetails.output.aggregation) {
					_requestDetails.output.aggregation = new AdvancedQueryPojo.QueryOutputPojo.AggregationOutputPojo();
				}
				_requestDetails.output.aggregation.entsNumReturn = Integer.parseInt(value);				
			}
			else if (attrName.equals("output.aggregation.eventsnumreturn")) {
				if (null == _requestDetails.output) {
					_requestDetails.output = new AdvancedQueryPojo.QueryOutputPojo();
				}
				if (null == _requestDetails.output.aggregation) {
					_requestDetails.output.aggregation = new AdvancedQueryPojo.QueryOutputPojo.AggregationOutputPojo();
				}
				_requestDetails.output.aggregation.eventsNumReturn = Integer.parseInt(value);				
			}
			else if (attrName.equals("output.aggregation.factsnumreturn")) {
				if (null == _requestDetails.output) {
					_requestDetails.output = new AdvancedQueryPojo.QueryOutputPojo();
				}
				if (null == _requestDetails.output.aggregation) {
					_requestDetails.output.aggregation = new AdvancedQueryPojo.QueryOutputPojo.AggregationOutputPojo();
				}
				_requestDetails.output.aggregation.factsNumReturn = Integer.parseInt(value);				
			}
			else if (attrName.equals("output.aggregation.raw")) {
				if (null == _requestDetails.output) {
					_requestDetails.output = new AdvancedQueryPojo.QueryOutputPojo();
				}
				if (null == _requestDetails.output.aggregation) {
					_requestDetails.output.aggregation = new AdvancedQueryPojo.QueryOutputPojo.AggregationOutputPojo();
				}
				_requestDetails.output.aggregation.raw = value;				
			}
			else if (attrName.equals("output.aggregation.sources")) {
				if (null == _requestDetails.output) {
					_requestDetails.output = new AdvancedQueryPojo.QueryOutputPojo();
				}
				if (null == _requestDetails.output.aggregation) {
					_requestDetails.output.aggregation = new AdvancedQueryPojo.QueryOutputPojo.AggregationOutputPojo();
				}
				_requestDetails.output.aggregation.sources = Integer.parseInt(value);				
			}
			else if (attrName.equals("output.aggregation.sourcemetadata")) {
				if (null == _requestDetails.output) {
					_requestDetails.output = new AdvancedQueryPojo.QueryOutputPojo();
				}
				if (null == _requestDetails.output.aggregation) {
					_requestDetails.output.aggregation = new AdvancedQueryPojo.QueryOutputPojo.AggregationOutputPojo();
				}
				_requestDetails.output.aggregation.sourceMetadata = Integer.parseInt(value);				
			}
			else if (attrName.equals("output.filter.assocVerbs")) { // comma-separated list 
				if (null == _requestDetails.output) {
					_requestDetails.output = new AdvancedQueryPojo.QueryOutputPojo();
				}
				if (null == _requestDetails.output.filter) {
					_requestDetails.output.filter = new AdvancedQueryPojo.QueryOutputPojo.FilterOutputPojo();
				}
				_requestDetails.output.filter.assocVerbs = value.toLowerCase().split("\\s*,\\s*");
			}
			else if (attrName.equals("output.filter.entityTypes")) { // comma-separated list 
				if (null == _requestDetails.output) {
					_requestDetails.output = new AdvancedQueryPojo.QueryOutputPojo();
				}
				if (null == _requestDetails.output.filter) {
					_requestDetails.output.filter = new AdvancedQueryPojo.QueryOutputPojo.FilterOutputPojo();
				}
				_requestDetails.output.filter.entityTypes = value.toLowerCase().split("\\s*,\\s*");
			}
			
// Generic event aggregation: not implemented (INF-1230)
// Moments: not implemented (INF-955)
			
		} // (end loop over all the attributes)
		
		// Then write the query terms into a list 

		_requestDetails.qt = new LinkedList<AdvancedQueryPojo.QueryTermPojo>();
		for (AdvancedQueryPojo.QueryTermPojo qtIndex: qt.values()) {
			_requestDetails.qt.add(qtIndex);
		}
		if ((null != _requestDetails.input) && (!tt.isEmpty())) {
			_requestDetails.input.typeAndTags = new LinkedList<AdvancedQueryPojo.QueryInputPojo.TypeAndTagTermPojo>();
			for (AdvancedQueryPojo.QueryInputPojo.TypeAndTagTermPojo ttIndex: tt.values()) {
				_requestDetails.input.typeAndTags.add(ttIndex);
			}
		}
		// Fill in the blanks (a decent attempt has been made to fill out the blanks inside these options)
		if (null == _requestDetails.input) {
			_requestDetails.input = new AdvancedQueryPojo.QueryInputPojo();				
		}
		if (null == _requestDetails.score) {
			_requestDetails.score = new AdvancedQueryPojo.QueryScorePojo();				
		}
		if (null == _requestDetails.output) {
			_requestDetails.output = new AdvancedQueryPojo.QueryOutputPojo();
		}		
		if (null == _requestDetails.output.docs) { // (Docs are sufficiently important we'll make sure they're always present)
			_requestDetails.output.docs = new AdvancedQueryPojo.QueryOutputPojo.DocumentOutputPojo();
		}
		
	} //TESTED (see test fn below) 
	
	//___________________________________________________________________________________
	
	// TEST CODE

//	private static final QueryResource test = new QueryResource(true); 
	
	//Because there is no default constructor this breaks the api
	//possibly could get around by creating an empty constructor
	//did not test that - caleb 4/24/2012
	/*private QueryInterface(boolean bTest) {
		this.testUrlParsing();
	}*/
	
	@SuppressWarnings("unused")
	private void testUrlParsing()
	{
		String testString = 
			"qt[0].ftext=\"ftext0\"&qt[0].etext=etext0"+
			"&qt[1].entityValue=entity1&qt[1].entityType=type1"+
			"&qt[2].entity=entity2:type2&qt[2].entityOpt.expandAlias=true&qt[2].entityOpt.expandOntology=false&qt[3].time.min=0&qt[3].time.max=now"+
			"&qt[4].geo.centerll=(0,0)&qt[4].geo.dist=100m&qt[4].geo.minll=(1,1)&qt[4].geo.maxll=2,2"
			+
			"&logic=(qt[0] AND qt[1] AND qt[2] AND qt[3]) OR qt[4]"
			+
			"&raw={\"match_all\":{}}"			
			+
			"&input.name=name&input.tags=tags1,tags2,tags3"+
			"&input.typeAndTags[0].type=type0&input.typeAndTags[0].tags=type0.tags1,type0.tags2&input.typeAndTags[1].type=type2"+
			"&input.sources=sources1,sources2,sources3,sources4,&input.srcInclude=0"
			+
			"&score.numAnalyze=500&score.sigWeight=0.5&score.relWeight=0.4"+
			"&score.timeProx.time=10 Mar 2011&score.timeProx.decay=1w&score.geoProx.ll=0,0&score.geoProx.dist=10nm"
			+
			"&output.format=xml&output.numReturn=200&output.skip=100&output.docs=false"+
			"&output.ents=true&output.stats=true"+
			"&output.facets.enable=true&output.facets.minFreq=25&output.facets.maxCount=7"
			;

		Map<String, String> testMap = new TreeMap<String, String>();
		
		String pairs[] = testString.split("\\s*&\\s*");
		for (String pair: pairs) {
			String keyValue[] = pair.split("\\s*=\\s*");
			testMap.put(keyValue[0], keyValue[1]);
		}
		
		parseUrlString(testMap);
		
		String testResults = new Gson().toJson(_requestDetails, AdvancedQueryPojo.class);

		String refResults =
			"{\"qt\":[{\"ftext\":\"ftext0\",\"etext\":\"etext0\"},{\"entityValue\":\"entity1\",\"entityType\":\"type1\"},{\"entity\":\"entity2:type2\",\"entityOpt\":{\"expandAlias\":true,\"expandOntology\":false}},{\"time\":{\"min\":\"0\",\"max\":\"now\"}},{\"geo\":{\"centerll\":\"(0,0)\",\"dist\":\"100m\",\"minll\":\"(1,1)\",\"maxll\":\"2,2\"}}],"+
			"\"logic\":\"(qt[0] AND qt[1] AND qt[2] AND qt[3]) OR qt[4]\","+
			"\"raw\":{\"query\":\"{\\\"match_all\\\":{}}\"},"+
			"\"input\":{\"name\":\"name\",\"tags\":[\"tags1\",\"tags2\",\"tags3\"],\"typeAndTags\":[{\"type\":\"type0\",\"tags\":[\"type0.tags1\",\"type0.tags2\"]},{\"type\":\"type2\"}],\"sources\":[\"sources1\",\"sources2\",\"sources3\",\"sources4\"],\"srcInclude\":false},"+
			"\"score\":{\"numAnalyze\":500,\"sigWeight\":0.5,\"relWeight\":0.4,\"timeProx\":{\"time\":\"10 Mar 2011\",\"decay\":\"1w\"},\"geoProx\":{\"ll\":\"0,0\"}},"+			
			"\"output\":{\"format\":\"xml\",\"numReturn\":200,\"skip\":100,\"docs\":false,\"ents\":true,\"stats\":true,\"facets\":{\"enable\":true,\"minFreq\":25,\"maxCount\":7}}}";			
		
		if (testResults.equals(refResults)) {
			System.out.println("Great success!");
		}
		else {
			System.out.println("Epic fail!");			
			System.out.println(testResults);			
			System.out.println(refResults);			
		}
		//TODO (INF-475): convert this to some JUnit thing
	}
	
} // (end class QueryResource)
