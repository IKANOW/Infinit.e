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

import com.ikanow.infinit.e.api.utils.RESTTools;
import com.ikanow.infinit.e.data_model.api.ResponsePojo;
import com.ikanow.infinit.e.data_model.api.ResponsePojo.ResponseObject;

/**
 * @author cvitter
 */
public class CommunityInterface extends ServerResource 
{
	private CommunityHandler community = new CommunityHandler();
	
	// 
	private String communityId = null;
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
	
	private String action = "";
	
	private String cookieLookup = null;
	private String cookie = null;
	private String requestId = null;
	private String resp = null;
	private String urlStr = null;
	private String json = null;
	
	@Override
	public void doInit()
	{
		Request request = this.getRequest();
		 
		Map<String,Object> attributes = request.getAttributes();
		
		cookie = request.getCookies().getFirstValue("infinitecookie", true);	
		urlStr = request.getResourceRef().toString();
		
		// Method.POST
		if (request.getMethod() == Method.POST) 
		{
			communityId = RESTTools.decodeRESTParam("communityid", attributes);
		}
		
		// Method.GET
		if (request.getMethod() == Method.GET) 
		{
			if (urlStr.contains("/community/get/") )
			{
				if (RESTTools.decodeRESTParam("communityid", attributes) != null) 
					communityId = RESTTools.decodeRESTParam("communityid", attributes);
				action = "getCommunity";
			}
			
			else if (urlStr.contains("/community/getsystem") )
			{
				action = "getSystemCommunity";
			}
			
			else if (urlStr.contains("/community/getall") )
			{
				action = "getAllCommunities";
			}
			
			else if (urlStr.contains("/community/getpublic") )
			{
				action = "getPublicCommunities";
			}
			
			else if (urlStr.contains("/community/getprivate") )
			{
				action = "getPrivateCommunities";
			}
			
			else if (urlStr.contains("/community/add/") )
			{
				name = RESTTools.decodeRESTParam("name", attributes);
				description = RESTTools.decodeRESTParam("description", attributes);
				if (RESTTools.decodeRESTParam("tags", attributes) != null) tags = RESTTools.decodeRESTParam("tags", attributes);
				if (RESTTools.decodeRESTParam("parentid", attributes) != null) parentId = RESTTools.decodeRESTParam("parentid", attributes);
				action = "addCommunity";
			}
			
			else if (urlStr.contains("/community/addwithid/") )
			{
				communityId = RESTTools.decodeRESTParam("id", attributes);
				name = RESTTools.decodeRESTParam("name", attributes);
				description = RESTTools.decodeRESTParam("description", attributes);
				parentId = RESTTools.decodeRESTParam("parentid", attributes);
				parentName = RESTTools.decodeRESTParam("parentname", attributes);
				tags = RESTTools.decodeRESTParam("tags", attributes);
				ownerId = RESTTools.decodeRESTParam("ownerid", attributes);
				ownerDisplayName = RESTTools.decodeRESTParam("ownerdisplayname", attributes);
				ownerEmail = RESTTools.decodeRESTParam("owneremail", attributes);
				action = "addCommunityWithId";
			}
			
			else if ( urlStr.contains("/community/remove/"))
			{
				communityId = RESTTools.decodeRESTParam("id", attributes);
				action = "removeCommunityById";
			}
			
			else if (urlStr.contains("/community/member/update/status") )
			{

				communityId = RESTTools.decodeRESTParam("communityid", attributes);
				personId = RESTTools.decodeRESTParam("personid", attributes);
				userStatus = RESTTools.decodeRESTParam("userstatus", attributes);
				action = "updateMemberStatus";
			}
			
			else if (urlStr.contains("/community/member/update/type") )
			{
				communityId = RESTTools.decodeRESTParam("communityid", attributes);
				personId = RESTTools.decodeRESTParam("personid", attributes);
				userType = RESTTools.decodeRESTParam("usertype", attributes);
				action = "updateMemberType";
			}
			
			else if ( urlStr.contains("/community/update/"))
			{
				communityId = RESTTools.decodeRESTParam("communityid", attributes);
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
			
			else if ( urlStr.contains("/community/member/join/"))
			{
				communityId = RESTTools.decodeRESTParam("communityid", attributes);
				//personId = RESTTools.decodeRESTParam("personid", attributes);
				action = "joinCommunity";
			}
			
			else if (urlStr.contains("/community/member/leave/"))
			{
				communityId = RESTTools.decodeRESTParam("communityid", attributes);
				//personId = RESTTools.decodeRESTParam("personid", attributes);
				action = "leaveCommunity";
			}
			
			else if (urlStr.contains("/community/member/invite/"))
			{
				communityId = RESTTools.decodeRESTParam("communityid", attributes);
				personId = RESTTools.decodeRESTParam("personid", attributes);
				action = "inviteCommunity";
			}
			
			else if ( urlStr.contains("/community/requestresponse/"))
			{
				requestId = RESTTools.decodeRESTParam("requestid", attributes);
				resp = RESTTools.decodeRESTParam("response", attributes);
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
				if ( urlStr.contains("/community/update/") )
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
			 rp = this.community.requestResponse(requestId, resp);
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
				 if ( RESTTools.validateCommunityIds(cookieLookup, communityId) || (communityId.startsWith("*")) )
				 {
					 rp = this.community.getCommunity(cookieLookup, communityId);
				 }
				 else
				 {
					 rp = new ResponsePojo();
					 rp.setResponse(new ResponseObject("Verifying Communities",false,"Community Ids are not valid for this user"));
				 }
			 }
			 else if (action.equals("getSystemCommunity"))
			 {
				 rp = this.community.getSystemCommunity();
			 }
			 else if (action.equals("getAllCommunities"))
			 {
				 rp = this.community.getCommunities(cookieLookup);
			 }
			 else if (action.equals("getPublicCommunities"))
			 {
				 rp = this.community.getCommunities(cookieLookup, true);
			 }
			 else if (action.equals("getPrivateCommunities")) 
			 {
				 rp = this.community.getCommunities(cookieLookup, false);
			 }
			 else if (action.equals("updateMemberStatus"))
			 {
				 rp = this.community.updateMemberStatus(cookieLookup, personId, communityId, userStatus);
			 }
			 else if (action.equals("updateMemberType"))
			 {
				 rp = this.community.updateMemberType(cookieLookup, personId, communityId, userType);
			 }
			 else if (action.equals("addCommunity"))
			 {
				 rp = this.community.addCommunity(cookieLookup, name, description, parentId, tags);
			 }
			 else if (action.equals("addCommunityWithId"))
			 {
				 rp = this.community.addCommunity(cookieLookup, communityId, name, description, parentId, 
						 parentName, tags, ownerId, ownerDisplayName, ownerEmail);
			 }
			 else if ( action.equals("removeCommunityById"))
			 {
				 rp = this.community.removeCommunity(cookieLookup, communityId);
			 }
			 else if ( action.equals("updateCommunity"))
			 {
				 rp = this.community.updateCommunity(cookieLookup, communityId, json);
			 }
			 else if (action.equals("joinCommunity"))
			 {
				 rp = this.community.joinCommunity(cookieLookup, communityId);
			 }
			 else if ( action.equals("leaveCommunity"))
			 {
				 rp = this.community.leaveCommunity(cookieLookup, communityId);
			 }
			 else if ( action.equals("inviteCommunity"))
			 {
				 rp = this.community.inviteCommunity(cookieLookup, personId, communityId);
			 }
		 }
		 
		 Date endTime = new Date();
		 rp.getResponse().setTime(endTime.getTime() - startTime.getTime());
		 return new StringRepresentation(rp.toApi(), MediaType.APPLICATION_JSON);
	}
}
