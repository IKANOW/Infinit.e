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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
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
import com.ikanow.infinit.e.api.utils.SocialUtils;
import com.ikanow.infinit.e.data_model.Globals;
import com.ikanow.infinit.e.data_model.api.ResponsePojo;
import com.ikanow.infinit.e.data_model.api.ResponsePojo.ResponseObject;
import com.ikanow.infinit.e.data_model.api.knowledge.AdvancedQueryPojo;
import com.ikanow.infinit.e.data_model.api.knowledge.AdvancedQueryPojo.QueryTermPojo.SentimentModifierPojo;
import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.social.authentication.AuthenticationPojo;
import com.mongodb.BasicDBObject;

// The resource handlers for Advanced Queries in beta

// The REST call comes in one of the following formats:
//
// GET <preamble>/knowledge/query/<communityIdList>?json=<encoded JSON object>
// GET <preamble>/knowledge/query/<communityIdList>?<query-url-parameters>
// or...
// POST<preamble>/knowledge/query/<communityIdList>
// <JSON object>

//TODO moments interface

public class QueryInterface extends ServerResource 
{
	// Utility objects
	private QueryHandler _queryController = new QueryHandler();
	
	private static final Logger _logger = Logger.getLogger(QueryInterface.class);
	
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
		 Map<String,Object> attributes = request.getAttributes();
		 Map<String, String> queryOptions = this.getQuery().getValuesMap();
		 
		 //Optional query object (else is a POST)
		 if (Method.POST == request.getMethod()) 
		 {
			 // (Parameters a bit different in POSTs)
			 
				 
				 _communityIdStrList = RESTTools.getUrlAttribute("communityids", attributes, queryOptions);
			 
			 // This is handled elsewhere, in acceptRepresentation, have faith.....
		 }
		 else 
		 {
			 // If we're in here, then we're in a query call, we don't support any others...
			 _communityIdStrList = RESTTools.getUrlAttribute("communityids", attributes, queryOptions);
			 
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

		String cookieLookup = null; 
		try {
			// First off, check the cookie is valid:
			boolean bNotRss = !_requestDetails.output.format.equalsIgnoreCase("rss"); 
			// maybe don't need cookie for RSS?
			// Do a quick bit of further error checking here too:
			if (bNotRss) {
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
			
			// Perform cookie lookup (for RSS may allow us to skip other auth logic)			
			cookieLookup = RESTTools.cookieLookup(_cookie);
			
			if (!bNotRss) { // RSS case
				ObjectId userId = null;
				
				//Set the commids to whatever is given in the query to
				_communityIdStrList = "";
				for ( ObjectId comm : _requestDetails.communityIds )
				{
					_communityIdStrList += "," + comm.toString(); 
				}
				_communityIdStrList = _communityIdStrList.substring(1);
				// Authentication:
				if (null == cookieLookup) 
				{ // (else don't need to both)
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
						userId = authuser.getProfileId();
						cookieLookup = userId.toString();
						
					}
					//no other auth was used, try using the commid
					if ( null == cookieLookup )
					{
						userId = _requestDetails.communityIds.get(0);
						cookieLookup = userId.toString();
					}
					// Check user still exists, leave quietly if not
					try {
						BasicDBObject personQuery = new BasicDBObject("_id", userId);
						if (null == DbManager.getSocial().getPerson().findOne(personQuery)) {
							cookieLookup = null;
						}
					}
					catch (Exception e) { // unknown error, bail
						cookieLookup = null;						
					}
				}
				// end authentication for RSS
				// Also, since we're RSS, there's a bunch of output params that we know we don't need:
				
				// (output and output.docs are guaranteed to exist)
				_requestDetails.output.aggregation = null;
				_requestDetails.output.docs.ents = false;
				_requestDetails.output.docs.events = false;
				_requestDetails.output.docs.facts = false;
				_requestDetails.output.docs.summaries = false;
				_requestDetails.output.docs.eventsTimeline = false;
				_requestDetails.output.docs.metadata = false;
				//set cookielookup to first commid
				
			}
			
			// Fail out otherwise perform query
			
			if (cookieLookup == null) // wrong password, or rss-user doesn't exist
			{
				rp = new ResponsePojo();
				rp.setResponse(new ResponseObject("Cookie Lookup", false, "Cookie session expired or never existed, please login first"));
				data = rp.toApi();
			}
			else 
			{
				//check communities are valid before using
				if ( SocialUtils.validateCommunityIds(cookieLookup, _communityIdStrList) )
					rp = _queryController.doQuery(cookieLookup, _requestDetails, _communityIdStrList, errorString);
				else {
					errorString.append(": Community Ids are not valid for this user");
					RESTTools.logRequest(this);
				}
				
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
			}//TESTED
		}
		catch (Exception e) {
			// (LOGS TO CATALINA.OUT IF THE LOG MESSAGES AREN'T NECESSARY)
			e.printStackTrace();
			
			errorString.append(" userid=").append(cookieLookup).append(" groups=").append(_communityIdStrList);
			errorString.append( " error='").append(e.getMessage()).append("' stack=");
			Globals.populateStackTrace(errorString, e);
			if (null != e.getCause()) {
				errorString.append("[CAUSE=").append(e.getCause().getMessage()).append("]");
				Globals.populateStackTrace(errorString, e.getCause());				
			}
			String error = errorString.toString(); 
			_logger.error(error);
			
			//getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			rp = new ResponsePojo();
			rp.setResponse(new ResponseObject("Query", false, error));
			data = rp.toApi();
		}//TESTED
		
		// One last check to ensure data has value (ugly ugly ugly)
		if (data == null ) {
			rp = new ResponsePojo();
			rp.setResponse(new ResponseObject("Query", false, errorString.toString()));
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
		TreeMap<Integer, String> momentEnts = new TreeMap<Integer, String>();
		
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
					else if (attrName.equals("sentiment.min")) {
						if (null == qtIndex.sentiment) {
							qtIndex.sentiment = new SentimentModifierPojo();
						}
						qtIndex.sentiment.min = Double.parseDouble(value);
					}
					else if (attrName.equals("sentiment.max")) {
						if (null == qtIndex.sentiment) {
							qtIndex.sentiment = new SentimentModifierPojo();
						}
						qtIndex.sentiment.max = Double.parseDouble(value);
					}
					else if (attrName.equals("entityopt.expandalias")) {
						if (null == qtIndex.entityOpt) {
							qtIndex.entityOpt = new AdvancedQueryPojo.QueryTermPojo.EntityOptionPojo();
						}
						qtIndex.entityOpt.expandAlias = Boolean.parseBoolean(value);
					}
					else if (attrName.equals("entityopt.lockdate")) {
						if (null == qtIndex.entityOpt) {
							qtIndex.entityOpt = new AdvancedQueryPojo.QueryTermPojo.EntityOptionPojo();
						}
						qtIndex.entityOpt.lockDate = Boolean.parseBoolean(value);
					}
					else if (attrName.equals("entityopt.rawtext")) {
						if (null == qtIndex.entityOpt) {
							qtIndex.entityOpt = new AdvancedQueryPojo.QueryTermPojo.EntityOptionPojo();
						}
						qtIndex.entityOpt.rawText = Boolean.parseBoolean(value);
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
					else if (attrName.equals("metadatafield")) {
						qtIndex.metadataField = value;						
					}
					//association parsing code: 
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
					else if (attrName.equals("assoc.entity1.entityvalue")) {
						if (null == qtIndex.assoc) {
							qtIndex.assoc = new AdvancedQueryPojo.QueryTermPojo.AssociationTermPojo();
						}
						if (null == qtIndex.assoc.entity1) {
							qtIndex.assoc.entity1 = new AdvancedQueryPojo.QueryTermPojo();
						}
						qtIndex.assoc.entity1.entityValue = value;
					}
					else if (attrName.equals("assoc.entity1.entitytype")) {
						if (null == qtIndex.assoc) {
							qtIndex.assoc = new AdvancedQueryPojo.QueryTermPojo.AssociationTermPojo();
						}
						if (null == qtIndex.assoc.entity1) {
							qtIndex.assoc.entity1 = new AdvancedQueryPojo.QueryTermPojo();
						}
						qtIndex.assoc.entity1.entityType = value;
					}
					else if (attrName.equals("assoc.entity1.entityopt.rawtext")) { // (only supported entity opt)
						if (null == qtIndex.assoc) {
							qtIndex.assoc = new AdvancedQueryPojo.QueryTermPojo.AssociationTermPojo();
						}
						if (null == qtIndex.assoc.entity1) {
							qtIndex.assoc.entity1 = new AdvancedQueryPojo.QueryTermPojo();
						}
						if (null == qtIndex.assoc.entity1.entityOpt) {
							qtIndex.assoc.entity1.entityOpt = new AdvancedQueryPojo.QueryTermPojo.EntityOptionPojo();
						}
						qtIndex.assoc.entity1.entityOpt.rawText = Boolean.parseBoolean(value);
					}
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
					else if (attrName.equals("assoc.entity2.entityvalue")) {
						if (null == qtIndex.assoc) {
							qtIndex.assoc = new AdvancedQueryPojo.QueryTermPojo.AssociationTermPojo();
						}
						if (null == qtIndex.assoc.entity2) {
							qtIndex.assoc.entity2 = new AdvancedQueryPojo.QueryTermPojo();
						}
						qtIndex.assoc.entity2.entityValue = value;
					}
					else if (attrName.equals("assoc.entity2.entitytype")) {
						if (null == qtIndex.assoc) {
							qtIndex.assoc = new AdvancedQueryPojo.QueryTermPojo.AssociationTermPojo();
						}
						if (null == qtIndex.assoc.entity2) {
							qtIndex.assoc.entity2 = new AdvancedQueryPojo.QueryTermPojo();
						}
						qtIndex.assoc.entity2.entityType = value;
					}
					else if (attrName.equals("assoc.entity2.entityopt.rawtext")) { // (only supported entity opt)
						if (null == qtIndex.assoc) {
							qtIndex.assoc = new AdvancedQueryPojo.QueryTermPojo.AssociationTermPojo();
						}
						if (null == qtIndex.assoc.entity2) {
							qtIndex.assoc.entity2 = new AdvancedQueryPojo.QueryTermPojo();
						}
						if (null == qtIndex.assoc.entity2.entityOpt) {
							qtIndex.assoc.entity2.entityOpt = new AdvancedQueryPojo.QueryTermPojo.EntityOptionPojo();
						}
						qtIndex.assoc.entity2.entityOpt.rawText = Boolean.parseBoolean(value);
					}
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
			else if (attrName.equals("expandalias")) {
				_requestDetails.expandAlias = Boolean.parseBoolean(value);
			}
			else if (attrName.equals("explain")) {
				_requestDetails.explain = Boolean.parseBoolean(value);
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
			else if (attrName.equals("score.scoreents")) {
				if (null == _requestDetails.score) {
					_requestDetails.score = new AdvancedQueryPojo.QueryScorePojo();
				}
				_requestDetails.score.scoreEnts = Boolean.parseBoolean(value);								
			}
			else if (attrName.equals("score.adjustaggregatesig")) {
				if (null == _requestDetails.score) {
					_requestDetails.score = new AdvancedQueryPojo.QueryScorePojo();
				}
				_requestDetails.score.adjustAggregateSig = Boolean.parseBoolean(value);								
			}
			else if (attrName.startsWith("score.sourceweights.")) {
				if (null == _requestDetails.score) {
					_requestDetails.score = new AdvancedQueryPojo.QueryScorePojo();
				}
				if (null == _requestDetails.score.sourceWeights) {
					_requestDetails.score.sourceWeights = new HashMap<String, Double>();
				}
				String key = attrName.substring(20); // len("score.sourceweights"+".")
				_requestDetails.score.sourceWeights.put(key, Double.parseDouble(value));
			}
			else if (attrName.startsWith("score.typeweights.")) {
				if (null == _requestDetails.score) {
					_requestDetails.score = new AdvancedQueryPojo.QueryScorePojo();
				}
				if (null == _requestDetails.score.typeWeights) {
					_requestDetails.score.typeWeights = new HashMap<String, Double>();
				}
				String key = attrName.substring(18); // len("score.typeweights"+".")
				_requestDetails.score.typeWeights.put(key, Double.parseDouble(value));
			}
			else if (attrName.startsWith("score.tagweights.")) {
				if (null == _requestDetails.score) {
					_requestDetails.score = new AdvancedQueryPojo.QueryScorePojo();
				}
				if (null == _requestDetails.score.tagWeights) {
					_requestDetails.score.tagWeights = new HashMap<String, Double>();
				}
				String key = attrName.substring(17); // len("score.tagweights"+".")
				_requestDetails.score.tagWeights.put(key, Double.parseDouble(value));
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
			else if (attrName.equals("output.filter.assocverbs")) { // comma-separated list 
				if (null == _requestDetails.output) {
					_requestDetails.output = new AdvancedQueryPojo.QueryOutputPojo();
				}
				if (null == _requestDetails.output.filter) {
					_requestDetails.output.filter = new AdvancedQueryPojo.QueryOutputPojo.FilterOutputPojo();
				}
				_requestDetails.output.filter.assocVerbs = value.split("\\s*,\\s*");
			}
			else if (attrName.equals("output.filter.entitytypes")) { // comma-separated list 
				if (null == _requestDetails.output) {
					_requestDetails.output = new AdvancedQueryPojo.QueryOutputPojo();
				}
				if (null == _requestDetails.output.filter) {
					_requestDetails.output.filter = new AdvancedQueryPojo.QueryOutputPojo.FilterOutputPojo();
				}
				_requestDetails.output.filter.entityTypes = value.split("\\s*,\\s*");
			}
			// moments
			else if (attrName.equals("output.aggregation.moments.timesinterval")) {
				if (null == _requestDetails.output) {
					_requestDetails.output = new AdvancedQueryPojo.QueryOutputPojo();
				}
				if (null == _requestDetails.output.aggregation) {
					_requestDetails.output.aggregation = new AdvancedQueryPojo.QueryOutputPojo.AggregationOutputPojo();
				}
				if (null == _requestDetails.output.aggregation.moments) {
					_requestDetails.output.aggregation.moments = new AdvancedQueryPojo.QueryOutputPojo.AggregationOutputPojo.TemporalAggregationOutputPojo();
				}
				_requestDetails.output.aggregation.moments.timesInterval = value;
			}//TOTEST
			else if (attrName.equals("output.aggregation.moments.entitylist")) {
				if (null == _requestDetails.output) {
					_requestDetails.output = new AdvancedQueryPojo.QueryOutputPojo();
				}
				if (null == _requestDetails.output.aggregation) {
					_requestDetails.output.aggregation = new AdvancedQueryPojo.QueryOutputPojo.AggregationOutputPojo();
				}
				if (null == _requestDetails.output.aggregation.moments) {
					_requestDetails.output.aggregation.moments = new AdvancedQueryPojo.QueryOutputPojo.AggregationOutputPojo.TemporalAggregationOutputPojo();
				}
				_requestDetails.output.aggregation.moments.entityList = Arrays.asList(value.split("\\s*,\\s*"));
			}//TESTED
			else if (attrName.startsWith("output.aggregation.moments.entitylist[")) {
				if (null == _requestDetails.output) {
					_requestDetails.output = new AdvancedQueryPojo.QueryOutputPojo();
				}
				if (null == _requestDetails.output.aggregation) {
					_requestDetails.output.aggregation = new AdvancedQueryPojo.QueryOutputPojo.AggregationOutputPojo();
				}
				if (null == _requestDetails.output.aggregation.moments) {
					_requestDetails.output.aggregation.moments = new AdvancedQueryPojo.QueryOutputPojo.AggregationOutputPojo.TemporalAggregationOutputPojo();
				}
				int npos = attrName.indexOf(']');				
				if (npos >= 0) {
					String index = attrName.substring(38, npos);		
					int nIndex = Integer.parseInt(index);
					momentEnts.put(nIndex, value);
				}
			}//TESTED
			
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
		if (!momentEnts.isEmpty()) {
			_requestDetails.output.aggregation.moments.entityList = new ArrayList<String>(momentEnts.values());
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
		//TODO (INF-475): 1) now missing lots of test cases, 2) convert this to some JUnit thing
		
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
	}
	
} // (end class QueryResource)
