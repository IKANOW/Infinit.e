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

import com.ikanow.infinit.e.api.utils.ProjectUtils;
import com.ikanow.infinit.e.api.utils.RESTTools;
import com.ikanow.infinit.e.api.utils.SocialUtils;
import com.ikanow.infinit.e.data_model.api.ResponsePojo;
import com.ikanow.infinit.e.data_model.api.ResponsePojo.ResponseObject;
import com.mongodb.util.JSON;

/**
 * @author cmorgan
 *
 */
public class SourceInterface extends ServerResource 
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
	private boolean shouldSuspend = false;
	private int nNumDocsToReturn = 10; // (used for test source)
	private boolean bReturnFullText = false; // (used for test source)
	private boolean bRealDedup = false; // (used for test source)
	private boolean bStripped = false;
	String project_id = null; 	
	
	@SuppressWarnings("unused")
	private static final Logger logger = Logger.getLogger(SourceInterface.class);	
	
	@Override
	public void doInit() 
	{
		 Request request = this.getRequest();
		 Map<String,Object> attributes = request.getAttributes();
		 urlStr = request.getResourceRef().toString();
		 cookie = request.getCookies().getFirstValue("infinitecookie",true);
		 Map<String, String> queryOptions = this.getQuery().getValuesMap();
		 bStripped = (null != queryOptions.get("stripped")) && Boolean.parseBoolean((String) queryOptions.get("stripped"));
		 project_id = queryOptions.get(ProjectUtils.query_param);
		 
		 // communityid - all get/post methods need this value
		 if (RESTTools.getUrlAttribute("communityid", attributes, queryOptions) != null)
		 {
			 communityid = RESTTools.getUrlAttribute("communityid", attributes, queryOptions);
		 }
		 
		 // Method.GET
		 if (request.getMethod() == Method.GET) 
		 {
			 if ( urlStr.contains("source/add/") || urlStr.contains("knowledge/sources/add/"))
			 {
				 sourceurl = RESTTools.getUrlAttribute("sourceurl", attributes, queryOptions);
				 sourcetitle = RESTTools.getUrlAttribute("sourcetitle", attributes, queryOptions);
				 sourcedesc = RESTTools.getUrlAttribute("sourcedesc", attributes, queryOptions);
				 extracttype = RESTTools.getUrlAttribute("extracttype", attributes, queryOptions);
				 sourcetags = RESTTools.getUrlAttribute("sourcetags", attributes, queryOptions);
				 action = "add"; 
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
				 json = RESTTools.getUrlAttribute("json", attributes, queryOptions);
				 try 
				 {
					 json = URLDecoder.decode(json, "UTF-8");
					 action = "saveSource";
				 }
				 catch (UnsupportedEncodingException e) 
				 {
					 //throw e;
					 //TODO cannot throw errors so do something useful
					 //set to failed temporarily so it doesn't run
					 action = "failed";
				 }
				 
			 }			 
			 else if ( urlStr.contains("source/delete/docs") || urlStr.contains("sources/delete/docs") )
			 {
				 sourceid = RESTTools.getUrlAttribute("sourceid", attributes, queryOptions);
				 action = "deletedocs";
			 }
			 else if ( urlStr.contains("source/delete") || urlStr.contains("sources/delete") )
			 {
				 sourceid = RESTTools.getUrlAttribute("sourceid", attributes, queryOptions);
				 action = "delete";
			 }
			 else if ( urlStr.contains("source/get") || urlStr.contains("sources/get") )
			 {
				 sourceid = RESTTools.getUrlAttribute("sourceid", attributes, queryOptions);
				 action = "info";
			 }
			 else if ( urlStr.contains("source/suspend") )
			 {
				 sourceid = RESTTools.getUrlAttribute("sourceid", attributes, queryOptions);
				 shouldSuspend = RESTTools.getUrlAttribute("shouldSuspend", attributes, queryOptions).equalsIgnoreCase("true");	
				 action = "suspend";
			 }
		 }
	}
	
	
	/**
	 * acceptRepresentation
	 * @return 
	 */
	@Post
	public Representation post(Representation entity) throws ResourceException 
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
					 String testUpdates = queryOptions.get("testUpdates");
					 if ((null != testUpdates) && ((testUpdates.equalsIgnoreCase("true")) || (testUpdates.equals("1")))) {
						 bRealDedup = true;
					 }
					action = "testSource";
				}
			}
			catch (Exception e) 
			{
				getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			}
		}
		
		return get();
	}
	
	
	
	/**
	 * Represent the user object in the requested format.
	 * 
	 * @param variant
	 * @return
	 * @throws ResourceException
	 */
	@Get
	public Representation get() throws ResourceException 
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
								 SocialUtils.validateCommunityIds(cookieLookup, communityid);

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
							 rp = this.source.testSource(json, nNumDocsToReturn, bReturnFullText, bRealDedup, cookieLookup);
						 }
						 else if ( action.equals("add") )
						 {
							 rp = this.source.addSource(sourcetitle, sourcedesc, sourceurl, extracttype, 
									 sourcetags, mediatype, communityid, cookieLookup);
						 }
						 else if ( action.equals("info") )
						 {
							 rp = this.source.getInfo(sourceid, cookieLookup);
						 }
						 else if ( action.equals("good") )
						 {
							 try
							 {
								 if ( project_id != null )
								 {
									 //then apply project filter
									 communityid = ProjectUtils.getCommunityIdStr(ProjectUtils.authenticate(project_id, cookieLookup));							
								 }									
								 rp = this.source.getGoodSources(cookieLookup, communityid, bStripped);
							 }
							 catch (Exception ex)
							 {
								 rp = new ResponsePojo();
								 rp.setResponse(new ResponseObject("Project Lookup", false, ex.getMessage()));						
							 }							 
						 }
						 else if ( action.equals("bad"))
						 {
							 try
							 {
								 if ( project_id != null )
								 {
									 //then apply project filter
									 communityid = ProjectUtils.getCommunityIdStr(ProjectUtils.authenticate(project_id, cookieLookup));							
								 }									
								 rp = this.source.getBadSources(cookieLookup, communityid, bStripped);
							 }
							 catch (Exception ex)
							 {
								 rp = new ResponsePojo();
								 rp.setResponse(new ResponseObject("Project Lookup", false, ex.getMessage()));						
							 }							 
						 }
						 else if ( action.equals("pending"))
						 {
							 try
							 {
								 if ( project_id != null )
								 {
									 //then apply project filter
									 communityid = ProjectUtils.getCommunityIdStr(ProjectUtils.authenticate(project_id, cookieLookup));							
								 }									
								 rp = this.source.getPendingSources(cookieLookup, communityid, bStripped);
							 }
							 catch (Exception ex)
							 {
								 rp = new ResponsePojo();
								 rp.setResponse(new ResponseObject("Project Lookup", false, ex.getMessage()));						
							 }							 
						 }
						 else if ( action.equals("user"))
						 {
							 rp = this.source.getUserSources(cookieLookup, bStripped);
						 }
						 else if ( action.equals("delete") || action.equals("deletedocs"))
						 {
							 rp = this.source.deleteSource(sourceid, communityid, cookieLookup, action.equals("deletedocs"));
						 }
						 else if ( action.equals("suspend"))
						 {
							 rp = this.source.suspendSource(sourceid, communityid, cookieLookup, shouldSuspend);
						 }	
					 }
				 } // (end communities valid)
			 } // (End login succeeded)
		 }
		 
		 Date endTime = new Date();
		 rp.getResponse().setTime(endTime.getTime() - startTime.getTime());
		 if (!rp.getResponse().isSuccess()) {
			 if (rp.getResponse().getMessage().contains("ermission")) { // likely to be a permissions error
				 RESTTools.logRequest(this);
			 }
		 }//TOTEST (TODO-2194)
		 return new StringRepresentation(rp.toApi(), MediaType.APPLICATION_JSON);
	}		
}

