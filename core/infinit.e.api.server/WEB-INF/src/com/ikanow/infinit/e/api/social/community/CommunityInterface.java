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
package com.ikanow.infinit.e.api.social.community;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Date;
import java.util.Map;

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
import com.ikanow.infinit.e.data_model.store.social.community.CommunityPojo;

/**
 * @author cvitter
 */
public class CommunityInterface extends ServerResource 
{
	private CommunityHandler community = new CommunityHandler();
	
	// 
	private String communityIdStr = null;
	private String personId = null;
	private String userType = null;
	private String userStatus = null;
	private String name = null;
	private String description = null;
	private String parentId = null;
	private String parentName = null;
	private String ownerId = null;
	private String ownerDisplayName = null;
	private String ownerEmail = null;
	private String tags = null;
	private String skipInvitation = null;
	private boolean showDocInfo = false;
	
	private String action = "";
	
	private String cookieLookup = null;
	private String cookie = null;
	private String requestId = null;
	private String resp = null;
	private String urlStr = null;
	private String json = null;
	String project_id = null;
	
	private CommunityPojo.CommunityType communityType = null; // or "user" or "data" for the new groups split
	
	@Override
	public void doInit()
	{
		Request request = this.getRequest();
		 
		Map<String,Object> attributes = request.getAttributes();
		Map<String, String> queryOptions = this.getQuery().getValuesMap();
		
		cookie = request.getCookies().getFirstValue("infinitecookie", true);	
		urlStr = request.getResourceRef().toString();
		
		project_id = queryOptions.get(ProjectUtils.query_param);
		
		if (urlStr.contains("/social/group/user/")) {
			communityType = CommunityPojo.CommunityType.user;
		}
		else if (urlStr.contains("/social/group/data/")) {
			communityType = CommunityPojo.CommunityType.data;			
		}
		//(else defaults to null, equivalent to data except it lists both types)
		
		// Method.POST
		if (request.getMethod() == Method.POST) 
		{
			communityIdStr = RESTTools.getUrlAttribute("communityid", attributes, queryOptions);
		}
		
		// Method.GET
		if (request.getMethod() == Method.GET) 
		{
			// Query String Values
			if (queryOptions.get("skipinvitation") != null) skipInvitation = queryOptions.get("skipinvitation");
			try {
				if (queryOptions.get("docinfo") != null) showDocInfo = Boolean.parseBoolean(queryOptions.get("docinfo"));
			}
			catch (Exception e) {} // do nothing, carry on
			
			if (urlStr.contains("/community/get/") || urlStr.matches(".*/group/(user|data)/get/.*"))
			{
				if (RESTTools.getUrlAttribute("communityid", attributes, queryOptions) != null) 
					communityIdStr = RESTTools.getUrlAttribute("communityid", attributes, queryOptions);
				action = "getCommunity";
			}
			
			else if (urlStr.contains("/community/getsystem")  || urlStr.matches(".*/group/(user|data)/getsystem.*") )
			{
				action = "getSystemCommunity";
			}
			
			else if (urlStr.contains("/community/getall")   || urlStr.matches(".*/group/(user|data)/getall.*"))
			{
				if (RESTTools.getUrlAttribute("communityid", attributes, queryOptions) != null) 
					communityIdStr = RESTTools.getUrlAttribute("communityid", attributes, queryOptions);
				action = "getAllCommunities";
			}
			
			else if (urlStr.contains("/community/getpublic") || urlStr.matches(".*/group/(user|data)/getpublic.*") )
			{
				action = "getPublicCommunities";
			}
			
			else if (urlStr.contains("/community/getprivate") || urlStr.matches(".*/group/(user|data)/getprivate.*") )
			{
				action = "getPrivateCommunities";
			}
			
			else if (urlStr.contains("/community/add/") || urlStr.matches(".*/group/(user|data)/add/.*") )
			{
				name = RESTTools.getUrlAttribute("name", attributes, queryOptions);
				description = RESTTools.getUrlAttribute("description", attributes, queryOptions);
				if (RESTTools.getUrlAttribute("tags", attributes, queryOptions) != null) tags = RESTTools.getUrlAttribute("tags", attributes, queryOptions);
				if (RESTTools.getUrlAttribute("parentid", attributes, queryOptions) != null) parentId = RESTTools.getUrlAttribute("parentid", attributes, queryOptions);
				action = "addCommunity";
			}
			
			else if (urlStr.contains("/community/addwithid/") || urlStr.matches(".*/group/(user|data)/addwithid/.*")  )
			{
				communityIdStr = RESTTools.getUrlAttribute("id", attributes, queryOptions);
				name = RESTTools.getUrlAttribute("name", attributes, queryOptions);
				description = RESTTools.getUrlAttribute("description", attributes, queryOptions);
				parentId = RESTTools.getUrlAttribute("parentid", attributes, queryOptions);
				parentName = RESTTools.getUrlAttribute("parentname", attributes, queryOptions);
				tags = RESTTools.getUrlAttribute("tags", attributes, queryOptions);
				ownerId = RESTTools.getUrlAttribute("ownerid", attributes, queryOptions);
				ownerDisplayName = RESTTools.getUrlAttribute("ownerdisplayname", attributes, queryOptions);
				ownerEmail = RESTTools.getUrlAttribute("owneremail", attributes, queryOptions);
				action = "addCommunityWithId";
			}
			
			else if ( urlStr.contains("/community/remove/") || urlStr.matches(".*/group/(user|data)/remove/.*") )
			{
				communityIdStr = RESTTools.getUrlAttribute("id", attributes, queryOptions);
				action = "removeCommunityById";
			}
			
			else if (urlStr.contains("/community/member/update/status") || urlStr.matches(".*/group/(user|data)/member/update/status.*")  )
			{

				communityIdStr = RESTTools.getUrlAttribute("communityid", attributes, queryOptions);
				personId = RESTTools.getUrlAttribute("personid", attributes, queryOptions);
				userStatus = RESTTools.getUrlAttribute("userstatus", attributes, queryOptions);
				action = "updateMemberStatus";
			}
			
			else if (urlStr.contains("/community/member/update/type") || urlStr.matches(".*/group/(user|data)/member/update/type.*")  )
			{
				communityIdStr = RESTTools.getUrlAttribute("communityid", attributes, queryOptions);
				personId = RESTTools.getUrlAttribute("personid", attributes, queryOptions);
				userType = RESTTools.getUrlAttribute("usertype", attributes, queryOptions);
				action = "updateMemberType";
			}
			
			else if ( urlStr.contains("/community/update/") || urlStr.matches(".*/group/(user|data)/update/.*") )
			{
				communityIdStr = RESTTools.getUrlAttribute("communityid", attributes, queryOptions);
				// Use URLDecoder on the json string
				try 
				{
					json = URLDecoder.decode(json, "UTF-8");
					action = "updateCommunity";
				}
				catch (UnsupportedEncodingException e) 
				{
					//cannot throw exceptions anymore so
					//set actino to failed so it doesn't run
					//throw e;
					action = "failed";
				}				
			}
			
			else if ( urlStr.contains("/community/member/join/") || urlStr.matches(".*/group/(user|data)/member/join/.*"))
			{
				communityIdStr = RESTTools.getUrlAttribute("communityid", attributes, queryOptions);
				//personId = RESTTools.getUrlAttribute("personid", attributes, queryOptions);
				action = "joinCommunity";
			}
			
			else if (urlStr.contains("/community/member/leave/") || urlStr.matches(".*/group/(user|data)/member/leave/.*"))
			{
				communityIdStr = RESTTools.getUrlAttribute("communityid", attributes, queryOptions);
				//personId = RESTTools.getUrlAttribute("personid", attributes, queryOptions);
				action = "leaveCommunity";
			}
			
			else if (urlStr.contains("/community/member/invite/") || urlStr.matches(".*/group/(user|data)/member/invite/.*"))
			{
				communityIdStr = RESTTools.getUrlAttribute("communityid", attributes, queryOptions);
				personId = RESTTools.getUrlAttribute("personid", attributes, queryOptions);
				action = "inviteCommunity";
			}
			
			else if ( urlStr.contains("/community/requestresponse/") || urlStr.matches(".*/group/(user|data)/requestresponse/.*"))
			{
				requestId = RESTTools.getUrlAttribute("requestid", attributes, queryOptions);
				resp = RESTTools.getUrlAttribute("response", attributes, queryOptions);
				action = "requestresponse";
			}
			
		}
	}
	
	
	
	
	/**
	 * Handles a POST
	 * acceptRepresentation
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
				json = entity.getText();
				if ( urlStr.contains("/community/update/") || urlStr.matches(".*/group/(user|data)/update/.*") )
				{
					action = "updateCommunity";
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
	 * Represent the community object in the requested format.	 * 
	 * @param variant
	 * @return
	 * @throws ResourceException
	 */
	@Get
	public Representation get( )  
	{
		 ResponsePojo rp = new ResponsePojo();
		 Date startTime = new Date();  
		 
		 cookieLookup = RESTTools.cookieLookup(cookie);
		 
		 //these functions do not require cookies
		 if (action.equals("requestresponse")) 
		 {
			 rp = this.community.requestResponse(requestId, resp, communityType);
		 }
		 else if ( cookieLookup == null ) //no cookie found
		 {
			 rp = new ResponsePojo();
			 rp.setResponse(new ResponseObject("Cookie Lookup",false,"Cookie session expired or never existed, please login first"));			 
		 }
		 else //requires cookies
		 {
			 if (action.equals("getCommunity"))
			 {
				 if ( SocialUtils.validateCommunityIds(cookieLookup, communityIdStr) || (communityIdStr.startsWith("*")) )
				 {
					 rp = this.community.getCommunity(cookieLookup, communityIdStr, showDocInfo, communityType);
				 }
				 else
				 {
					 rp = new ResponsePojo();
					 rp.setResponse(new ResponseObject("Verifying Communities",false,"Community Ids are not valid for this user"));
				 }
			 }
			 else if (action.equals("getSystemCommunity"))
			 {
				 rp = this.community.getSystemCommunity(communityType);
			 }
			 else if (action.equals("getAllCommunities"))
			 {
				 try
				 {					 
					 if ( project_id != null )
					 {
						 //then apply project filter
						 communityIdStr = ProjectUtils.getCommunityIdStr(ProjectUtils.authenticate(project_id, cookieLookup));							
					 }									
					 rp = this.community.getCommunities(cookieLookup, communityType, communityIdStr);
				 }
				 catch (Exception ex)
				 {
					 rp = new ResponsePojo();
					 rp.setResponse(new ResponseObject("Project Lookup", false, ex.getMessage()));						
				 }					 
			 }
			 else if (action.equals("getPublicCommunities"))
			 {
				 rp = this.community.getCommunities(cookieLookup, true, communityType);
			 }
			 else if (action.equals("getPrivateCommunities")) 
			 {
				 rp = this.community.getCommunities(cookieLookup, false, communityType);
			 }
			 else if (action.equals("updateMemberStatus"))
			 {				 
				 if (personId.contains(",")) {
					 rp = this.community.bulkUpdateOperation("updateMemberStatus", cookieLookup, personId, null, userStatus, communityIdStr, communityType, null);
				 }
				 else { // (old version for robustness)
					 rp = this.community.updateMemberStatus(cookieLookup, personId, communityIdStr, userStatus, communityType);
				 }
			 }
			 else if (action.equals("updateMemberType"))
			 {
				 if (personId.contains(",")) {
					 rp = this.community.bulkUpdateOperation("updateMemberType", cookieLookup, personId, userType, null, communityIdStr, communityType, null);
				 }
				 else { // (old version for robustness)
					 rp = this.community.updateMemberType(cookieLookup, personId, communityIdStr, userType, communityType);
				 }
			 }
			 else if (action.equals("addCommunity"))
			 {
				 rp = this.community.addCommunity(cookieLookup, name, description, parentId, tags, communityType);
			 }
			 else if (action.equals("addCommunityWithId"))
			 {
				 rp = this.community.addCommunity(cookieLookup, communityIdStr, name, description, parentId, 
						 parentName, tags, ownerId, ownerDisplayName, ownerEmail, communityType);
			 }
			 else if ( action.equals("removeCommunityById"))
			 {
				 rp = this.community.removeCommunity(cookieLookup, communityIdStr, communityType);
			 }
			 else if ( action.equals("updateCommunity"))
			 {
				 rp = this.community.updateCommunity(cookieLookup, communityIdStr, json, communityType);
			 }
			 else if (action.equals("joinCommunity"))
			 {
				 rp = this.community.joinCommunity(cookieLookup, communityIdStr, communityType);
			 }
			 else if ( action.equals("leaveCommunity"))
			 {
				 rp = this.community.leaveCommunity(cookieLookup, communityIdStr, communityType);
			 }
			 else if ( action.equals("inviteCommunity"))
			 {
				 if (personId.contains(",")) {
					 rp = this.community.bulkUpdateOperation("inviteCommunity", cookieLookup, personId, null, null, communityIdStr, communityType, skipInvitation);
				 }
				 else { // (old version for robustness)
					 rp = this.community.inviteCommunity(cookieLookup, personId, communityIdStr, skipInvitation, communityType);
				 }
			 }
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
