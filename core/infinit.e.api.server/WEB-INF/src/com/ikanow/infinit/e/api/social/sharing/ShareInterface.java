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
package com.ikanow.infinit.e.api.social.sharing;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
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
import com.ikanow.infinit.e.data_model.store.social.sharing.SharePojo;
import com.mongodb.util.JSON;

public class ShareInterface extends ServerResource
{
	// 
	private ShareHandler shareController = new ShareHandler();

	//
	private String personId = null;
	private String shareId = null;
	private String communityId = null;
	private String searchby = null;
	private String id = null;
	private String skip = null;
	private String limit = null;
	private String type = null;
	private String title = null;
	private String description = null;
	private String documentId = null;
	private String comment = null;
	
	//
	private String action = "";
	private String cookieLookup = null;
	private String cookie = null;
	private String urlStr = null;
	private String json = null;
	SharePojo sharePojo = null;
	private byte[] binaryData = null;
	private boolean returnContent = true;
	
	
	/**
	 * ShareResource
	 * @param context
	 * @param request
	 * @param response
	 * @throws UnsupportedEncodingException 
	 */
	@Override
	public void doInit() 
	{
		Request request = this.getRequest();
		
		Map<String,Object> attributes = request.getAttributes();
		urlStr = request.getResourceRef().toString();

		//Every user must pass in their cookies	
		cookie = request.getCookies().getFirstValue("infinitecookie", true);
		
		// Method.POST
		if (request.getMethod() == Method.POST) 
		{
			if (RESTTools.decodeRESTParam("id", attributes) != null) id = RESTTools.decodeRESTParam("id", attributes);
			if (RESTTools.decodeRESTParam("type", attributes) != null) type = RESTTools.decodeRESTParam("type", attributes);
			if (RESTTools.decodeRESTParam("title", attributes) != null) title = RESTTools.decodeRESTParam("title", attributes);
			if (RESTTools.decodeRESTParam("description", attributes) != null) description = RESTTools.decodeRESTParam("description", attributes);
		}
		
		// Method.GET
		if (request.getMethod() == Method.GET) 
		{
			// Method.GET
			Map<String, String> queryOptions = this.getQuery().getValuesMap();
			
			// Query String Values
			if (queryOptions.get("id") != null) id = queryOptions.get("id");
			if (queryOptions.get("skip") != null) skip = queryOptions.get("skip");
			if (queryOptions.get("limit") != null) limit = queryOptions.get("limit");
			if (queryOptions.get("searchby") != null) searchby = queryOptions.get("searchby");
			if (queryOptions.get("json") != null) json = queryOptions.get("json");
			if (queryOptions.get("type") != null) type = queryOptions.get("type");
			if ((queryOptions.get("nocontent") != null) && (queryOptions.get("nocontent").equalsIgnoreCase("true"))) {
				returnContent = false;				
			}

			// Get Share by ID
			if ( urlStr.contains("/share/get/") )
			{
				shareId = RESTTools.decodeRESTParam("id", attributes);
				action = "getShare";			 
			}
			
			// Search Shares by Owner, Community, Type
			else if ( urlStr.contains("/share/search/") )
			{
				action = "searchShares";			 
			}

			// Save a JSON share object to the DB
			// /social/share/save/json/{id}/{type}/{title}/{description}/?json={...}
			else if ( urlStr.contains("/share/save/json/") || urlStr.contains("/share/add/json/") || 
					  urlStr.contains("/share/update/json/") )
			{
				if (RESTTools.decodeRESTParam("id", attributes) != null) id = RESTTools.decodeRESTParam("id", attributes);
				type = RESTTools.decodeRESTParam("type", attributes);
				title = RESTTools.decodeRESTParam("title", attributes);
				description = RESTTools.decodeRESTParam("description", attributes);
				// Use URLDecoder on the json string
				try 
				{
					json = URLDecoder.decode(json, "UTF-8");
					action = "saveJson";
				}
				catch (UnsupportedEncodingException e) 
				{
					//TODO can't throw exceptions
					//set to failed so it doesn't run
					//throw e;
					action = "failed";
				}
				
			}
			
			else if ( urlStr.contains("/share/add/binary/"))
			{
				action = "addBinaryGET";
			}
			else if ( urlStr.contains("/share/update/binary/"))
			{
				action = "updateBinaryGET";
			}
			
			
			// Add a Ref (Pointer to a record within a collection)
			else if ( urlStr.contains("/share/add/ref/") )
			{
				type = RESTTools.decodeRESTParam("type", attributes);
				documentId = RESTTools.decodeRESTParam("documentid", attributes);
				title = RESTTools.decodeRESTParam("title", attributes);
				description = RESTTools.decodeRESTParam("description", attributes);
				action = "addRef";
			}
			
			// Add a Ref (Pointer to a record within a collection)
			else if ( urlStr.contains("/share/update/ref/") )
			{
				id = RESTTools.decodeRESTParam("id", attributes);
				type = RESTTools.decodeRESTParam("type", attributes);
				documentId = RESTTools.decodeRESTParam("documentid", attributes);
				title = RESTTools.decodeRESTParam("title", attributes);
				description = RESTTools.decodeRESTParam("description", attributes);
				action = "updateRef";
			}

			// Share - Remove a community from a share
			else if ( urlStr.contains("/share/remove/community/") )
			{
				shareId = RESTTools.decodeRESTParam("shareid", attributes);
				communityId = RESTTools.decodeRESTParam("communityid", attributes);
				action = "removeCommunity";
			}

			// Remove share
			else if ( urlStr.contains("/share/remove/") )
			{
				shareId = RESTTools.decodeRESTParam("shareid", attributes);
				action = "removeShare";
			}

			// Share - Add a community so that members can view the share
			else if ( urlStr.contains("/share/add/community/") )
			{
				shareId = RESTTools.decodeRESTParam("shareid", attributes);
				communityId = RESTTools.decodeRESTParam("communityid", attributes);
				comment = RESTTools.decodeRESTParam("comment", attributes);
				action = "addCommunity";
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
				if ( urlStr.contains("/share/save/json/") || urlStr.contains("/share/add/json/") || 
					 urlStr.contains("/share/update/json/") )
				{
					json = entity.getText();
					action = "saveJson";
				}
//				else if ( urlStr.contains("/share/update/json/") )
//				{
//					json = entity.getText();
//					action = "updateJson";
//				}	
				else if ( urlStr.contains("/share/add/binary/"))
				{
					action = "addBinaryPOST";
					InputStream instream = entity.getStream();
					ByteArrayOutputStream buffer = new ByteArrayOutputStream();
					int nRead;
					byte[] data = new byte[16384];
					while ((nRead = instream.read(data,0,data.length)) != -1)
					{
						buffer.write(data,0,nRead);
					}
					buffer.flush();
					binaryData = buffer.toByteArray();
				}
				else if ( urlStr.contains("/share/update/binary/"))
				{
					action = "updateBinaryPOST";
					InputStream instream = entity.getStream();
					ByteArrayOutputStream buffer = new ByteArrayOutputStream();
					int nRead;
					byte[] data = new byte[16384];
					while ((nRead = instream.read(data,0,data.length)) != -1)
					{
						buffer.write(data,0,nRead);
					}
					buffer.flush();
					binaryData = buffer.toByteArray();
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
	 * 
	 */
	@Get
	public Representation get() 
	{
		 ResponsePojo rp = new ResponsePojo(); 
		 Date startTime = new Date();	
		 cookieLookup = RESTTools.cookieLookup(cookie);

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
			 if (cookieLookup == null )
			 {
				 // User is not logged in
				 rp.setResponse(new ResponseObject("Cookie Lookup",false,"Cookie session expired or" +
				 " never existed, please login first"));
			 }
			 else
			 {
				 // UserId which will serve as the OwnerId for transactions below that require it
				 personId = cookieLookup; 

				 if (action.equals("saveJson"))
				 {
					 rp = this.shareController.saveJson(personId, id, type, title, description, json);
				 }
				 else if (action.equals("addBinaryGET"))
				 {
					 rp = new ResponsePojo(new ResponseObject("addBinary",false,"Can only add binary in POST (do not use GET)"));
				 }
				 else if (action.equals("addBinaryPOST"))
				 {
					 rp = this.shareController.addBinary(personId,"binary",title,description,this.getRequest().getEntity().getMediaType().toString(),binaryData);
				 }
				 else if ( action.equals("updateBinaryGET"))
				 {
					 rp = new ResponsePojo(new ResponseObject("updateBinary",false,"Can only update binary in POST (do not use GET)"));
				 }
				 else if ( action.equals("updateBinaryPOST"))
				 {
					 rp = this.shareController.updateBinary(personId, id, "binary",title,description,this.getRequest().getEntity().getMediaType().toString(),binaryData);
				 }
				 else if (action.equals("addRef"))
				 {
					 rp = this.shareController.addRef(personId, type, documentId, title, description);
				 }
				 else if (action.equals("updateRef"))
				 {
					 rp = this.shareController.updateRef(personId, id, type, documentId, title, description);
				 }
				 else if (action.equals("removeShare"))
				 {
					 rp = this.shareController.removeShare(personId, shareId);
				 }
				 else if (action.equals("addCommunity"))
				 {
					 rp = this.shareController.addCommunity(personId, shareId, communityId, comment);
				 }
				 else if (action.equals("removeCommunity"))
				 {
					 rp = this.shareController.removeCommunity(personId, shareId, communityId);
				 }
				 else if (action.equals("getShare"))
				 {			
					 rp = this.shareController.getShare(personId, shareId, returnContent);	
					 SharePojo share = (SharePojo) rp.getData();
					 if (null != share) {
						 if ( share.getType().equals("binary") && returnContent )					 
						 {			
							 try
							 {							 
								 ByteArrayOutputRepresentation rep = new ByteArrayOutputRepresentation(MediaType.valueOf(share.getMediaType()));
								 rep.setOutputBytes(share.getBinaryData());
								 //rep.setSize(share.getBinaryData().length);
								 return rep;							 
							 }
							 catch (Exception ex )
							 {
								 rp = new ResponsePojo(new ResponseObject("get Share",false,"error converting bytes to output"));
							 }						 
						 }
					 }
					 //(else error)
				 }
				 else if (action.equals("searchShares"))
				 {
					 rp = this.shareController.searchShares(personId, searchby, id, type, skip, limit);
				 }	 
			 }
		 }
		 
		 Date endTime = new Date();
		 rp.getResponse().setTime(endTime.getTime() - startTime.getTime());
		 return new StringRepresentation(rp.toApi(), MediaType.APPLICATION_JSON);
	}
	
}
