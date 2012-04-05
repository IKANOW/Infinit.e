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
package com.ikanow.infinit.e.api.config.source;


import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Date;
import java.util.Map;

import org.apache.log4j.Logger;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;
import org.restlet.resource.ResourceException;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;

import com.ikanow.infinit.e.api.utils.RESTTools;
import com.ikanow.infinit.e.data_model.api.ResponsePojo;
import com.ikanow.infinit.e.data_model.api.ResponsePojo.ResponseObject;
import com.mongodb.util.JSON;

/**
 * @author cmorgan
 *
 */
public class SourceInterface extends Resource 
{
	private String sourceid = null;
	private String action = "";
	private String sourceurl = null;
	private String sourcetitle = null;
	private String sourcedesc = null;
	private String extracttype = null;
	private SourceHandler source = new SourceHandler();
	private String cookieLookup = null;
	private String sourcetags = null;
	private String mediatype = null;
	private String communityid = null;
	private String cookie = null;
	private boolean needCookie = true;
	private String json = null;
	private String urlStr = null;
	private int nNumDocsToReturn = 10; // (used for test source)
	private boolean bReturnFullText = false; // (used for test source)
	
	@SuppressWarnings("unused")
	private static final Logger logger = Logger.getLogger(SourceInterface.class);	
	
	public SourceInterface(Context context, Request request, Response response) throws UnsupportedEncodingException 
	{
		 super(context, request, response);
		 Map<String,Object> attributes = request.getAttributes();
		 urlStr = request.getResourceRef().toString();
		 cookie = request.getCookies().getFirstValue("infinitecookie",true);	
		 
		 // communityid - all get/post methods need this value
		 if (RESTTools.decodeRESTParam("communityid", attributes) != null)
		 {
			 communityid = RESTTools.decodeRESTParam("communityid", attributes);
		 }
		 
		 // Method.GET
		 if (request.getMethod() == Method.GET) 
		 {
			 if ( urlStr.contains("source/add/") || urlStr.contains("knowledge/sources/add/"))
			 {
				 sourceurl = RESTTools.decodeRESTParam("sourceurl", attributes);
				 sourcetitle = RESTTools.decodeRESTParam("sourcetitle", attributes);
				 sourcedesc = RESTTools.decodeRESTParam("sourcedesc", attributes);
				 extracttype = RESTTools.decodeRESTParam("extracttype", attributes);
				 sourcetags = RESTTools.decodeRESTParam("sourcetags", attributes);
				 action = "add"; 
			 }
			 
			 else if (urlStr.contains("source/approve/") || urlStr.contains("knowledge/sources/approve/"))
			 {
				 sourceid = RESTTools.decodeRESTParam("sourceid",attributes);
				 action = "approve";
			 }
			 
			 else if ( urlStr.contains("source/decline/") || urlStr.contains("knowledge/sources/decline/"))
			 {
				 sourceid = RESTTools.decodeRESTParam("sourceid",attributes);
				 action = "deny";
			 }
			 
			 else if ( urlStr.contains("source/good") || urlStr.contains("knowledge/sources/good/"))
			 {
				 action = "good";			
			 }
			 
			 else if ( urlStr.contains("source/bad") || urlStr.contains("knowledge/sources/bad/"))
			 {
				 action = "bad";
			 }
			 
			 else if ( urlStr.contains("source/pending") || urlStr.contains("knowledge/sources/pending/"))
			 {
				 action = "pending";
			 }
			 
			 else if ( urlStr.contains("source/user"))
			 {
				 action = "user";
			 }
			 
			 else if ( urlStr.contains("source/save/") || urlStr.contains("knowledge/sources/save/"))
			 {
				 // Use URLDecoder on the json string
				 json = RESTTools.decodeRESTParam("json", attributes);
				 try 
				 {
					 json = URLDecoder.decode(json, "UTF-8");
				 }
				 catch (UnsupportedEncodingException e) 
				 {
					 throw e;
				 }
				 action = "saveSource";
			 }			 
			 else if ( urlStr.contains("source/delete/docs") || urlStr.contains("sources/delete/docs") )
			 {
				 sourceid = RESTTools.decodeRESTParam("sourceid",attributes);
				 action = "deletedocs";
			 }
			 else if ( urlStr.contains("source/delete") || urlStr.contains("sources/delete") )
			 {
				 sourceid = RESTTools.decodeRESTParam("sourceid",attributes);
				 action = "delete";
			 }
			 else if ( urlStr.contains("source/get") || urlStr.contains("sources/get") )
			 {
				 sourceid = RESTTools.decodeRESTParam("sourceid",attributes);
				 action = "info";
			 }
		 }
		 
		// All modifications of this resource
		this.setModifiable(true);
		getVariants().add(new Variant(MediaType.APPLICATION_JSON));
	}
	
	
	/**
	 * acceptRepresentation
	 */
	public void acceptRepresentation(Representation entity) throws ResourceException 
	{
		if (Method.POST == getRequest().getMethod()) 
		{
			try 
			{
				json = entity.getText();
				
				if ( urlStr.contains("source/save/") )
				{
					action = "saveSource";
				}	
				else if ( urlStr.contains("source/test") )
				{
					 Map<String, String> queryOptions = this.getQuery().getValuesMap();
					 String numReturn = queryOptions.get("numReturn");
					 if (null != numReturn) {
						 try {
							 nNumDocsToReturn = Integer.parseInt(numReturn);
						 }
						 catch (Exception e) {} // Just revert to default
					 }
					 String returnFullText = queryOptions.get("returnFullText");
					 if ((null != returnFullText) && ((returnFullText.equalsIgnoreCase("true")) || (returnFullText.equals("1")))) {
						 bReturnFullText = true;
					 }
					action = "testSource";
				}
			}
			catch (Exception e) 
			{
				getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			}
		}
		
		Representation response = represent(null);
		this.getResponse().setEntity(response);
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
		 ResponsePojo rp = new ResponsePojo(); 
		 Date startTime = new Date();
		 
		 // If JSON is != null, check that it is valid JSON
		 boolean isValidJson = true;
		 if (json != null)
		 {
			 try
			 {
				 JSON.parse(json);
			 }
			 catch (Exception e)
			 {
				 rp.setResponse(new ResponseObject("Parsing JSON",false,"The value passed via the json parameter could not be" +
				 " parsed as valid JSON."));
				 isValidJson = false;
			 }
		 }
		 
		 if (isValidJson)
		 {
			 if ( needCookie )
			 {
				 cookieLookup = RESTTools.cookieLookup(cookie);
				 
				 if ( cookieLookup == null )
				 {
					 rp = new ResponsePojo();
					 rp.setResponse(new ResponseObject("Cookie Lookup",false,"Cookie session expired or never existed, please login first"));
				 }
				 else {
					 // Every call needs communityid so check now
					 
					 boolean validCommunities = ((communityid == null) || communityid.startsWith("*")) ?
							 true : // (in this case, we apply the regex to user communities, so don't need to validate)
							 RESTTools.validateCommunityIds(cookieLookup, communityid);

					 if ( validCommunities == false )
					 {
						 rp = new ResponsePojo();
						 rp.setResponse(new ResponseObject("Verifying Communities",false,"Community IDs are not valid for this user"));
					 }
					 else
					 {
						 if ( action.equals("saveSource") )
						 {
							 rp = this.source.saveSource(json, cookieLookup, communityid); 
						 }
						 else if ( action.equals("testSource") )
						 {
							 rp = this.source.testSource(json, nNumDocsToReturn, bReturnFullText, cookieLookup);
						 }
						 else if ( action.equals("add") )
						 {
							 rp = this.source.addSource(sourcetitle, sourcedesc, sourceurl, extracttype, 
									 sourcetags, mediatype, communityid, cookieLookup);
						 }
						 else if (action.equals("approve"))
						 {
							 rp = this.source.approveSource(sourceid, communityid, cookieLookup);
						 }
						 else if ( action.equals("deny"))
						 {
							 rp = this.source.denySource(sourceid, communityid, cookieLookup);
						 }
						 else if ( action.equals("info") )
						 {
							 rp = this.source.getInfo(sourceid, cookieLookup);
						 }
						 else if ( action.equals("good") )
						 {
							 rp = this.source.getGoodSources(cookieLookup, communityid);
						 }
						 else if ( action.equals("bad"))
						 {
							 rp = this.source.getBadSources(cookieLookup, communityid);
						 }
						 else if ( action.equals("pending"))
						 {
							 rp = this.source.getPendingSources(cookieLookup, communityid);
						 }
						 else if ( action.equals("user"))
						 {
							 rp = this.source.getUserSources(cookieLookup);
						 }
						 else if ( action.equals("delete") || action.equals("deletedocs"))
						 {
							 rp = this.source.deleteSource(sourceid, communityid, cookieLookup, action.equals("deletedocs"));
						 }
					 }
				 } // (end communities valid)
			 } // (End login succeeded)
		 }
		 
		 Date endTime = new Date();
		 rp.getResponse().setTime(endTime.getTime() - startTime.getTime());
		 return new StringRepresentation(rp.toApi(), MediaType.APPLICATION_JSON);
	}		
}

