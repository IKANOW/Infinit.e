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
package com.ikanow.infinit.e.api.gui;

import java.io.IOException;
import java.util.Map;

import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;

import org.restlet.resource.Resource;

import com.ikanow.infinit.e.api.utils.RESTTools;
import com.ikanow.infinit.e.data_model.api.ResponsePojo;
import com.ikanow.infinit.e.data_model.api.ResponsePojo.ResponseObject;

public class UIInterface extends Resource
{
	String action = "";
	String cookieLookup = "";
	String updateItem = "";
	String communityIdStrList = "";
	String query = "";
	UIHandler uc = new UIHandler();
	private String cookie = null;
	private boolean needCookie = true;
	
	public UIInterface(Context context, Request request, Response response)
	{
		 super(context, request, response);
		 String urlStr = request.getResourceRef().toString();
		 Map<String,Object> attributes = request.getAttributes();
		 cookie = request.getCookies().getFirstValue("infinitecookie",true);
		 
		 if ( urlStr.contains("/uisetup/get"))
		 {			
			 action = "uisetup"; 
		 }
		 else if ( urlStr.contains("/uisetup/update"))
		 {
			 // (these first 2 will be null for the POST version) 
			 updateItem = RESTTools.decodeRESTParam("modules", attributes);
			 query = RESTTools.decodeRESTParam("query", attributes);
			 
			 communityIdStrList = RESTTools.decodeRESTParam("communityids", attributes);
			 if (communityIdStrList.equalsIgnoreCase("null")) {
				 communityIdStrList = null;
			 }
			 action = "update";
		 }
		 else if ( urlStr.contains("/modules/get"))
		 {
			 action = "getmodules";
		 }
		 else if ( urlStr.contains("/modules/install"))
		 {
			 action = "installmodule";
		 }
		 else if ( urlStr.contains("/modules/delete"))
		 {
			 action = "deletemodule";
			 updateItem = RESTTools.decodeRESTParam("modules", attributes);
		 }
		 else if ( urlStr.contains("/modules/user/get"))
		 {
			 action = "getusermodules";
		 }
		 else if ( urlStr.contains("/modules/search"))
		 {
			 updateItem = RESTTools.decodeRESTParam("term", attributes);
			 action = "searchmodules";
		 }
		 else if ( urlStr.contains("/modules/user/set"))
		 {
			 updateItem = RESTTools.decodeRESTParam("modules", attributes);
			 action = "savemodules";
		 }
		 
		 this.setModifiable(true);
		 getVariants().add(new Variant(MediaType.APPLICATION_JSON));
	}
	
	/**
	 * Handles a POST
	 * 
	 * @param entity
	 * @return
	 * @throws ResourceException
	 */

	public void acceptRepresentation(Representation entity) throws ResourceException {

		if (Method.POST == getRequest().getMethod()) {
			try {
				query = entity.getText();
			}
			catch (IOException e) {
				e.printStackTrace();
				getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			}
		}		 
		Representation response = represent(null);
		this.getResponse().setEntity(response);
	}//TESTED
	
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
		 if ( needCookie )
		 {
			 cookieLookup = RESTTools.cookieLookup(cookie);
			 if ( cookieLookup == null )
			 {
				 rp = new ResponsePojo();
				 rp.setResponse(new ResponseObject("Cookie Lookup",false,"Cookie session expired or never existed, please login first"));
			 }
			 else
			 {
				 if ( action.equals("getmodules"))
				 {
					 rp = this.uc.getModules(cookieLookup);
				 }			 
				 if ( action.equals("installmodule"))
				 {
					 rp = this.uc.installModule(query, cookieLookup);
				 }			 
				 if ( action.equals("deletemodule"))
				 {
					 rp = this.uc.deleteModule(updateItem, cookieLookup);
				 }			 
				 else if ( action.equals("searchmodules"))
				 {
					 rp = this.uc.searchModules(cookieLookup, updateItem);
				 }
				 else if ( action.equals("uisetup") )
				 {
					 rp = this.uc.getLastSetup(cookieLookup);
				 }
				 else if ( action.equals("update"))
				 {					 
					 if ( RESTTools.validateCommunityIds(cookieLookup, communityIdStrList) )
					 {
						 rp = this.uc.updateLastSetup(cookieLookup, communityIdStrList, query, updateItem);
					 }
					 else
					 {
						 rp = new ResponsePojo();
						 rp.setResponse(new ResponseObject("Cookie Lookup",false,"Cookie session expired or never existed, please login first"));
					 }
						 
				 }
				 else if ( action.equals("getusermodules"))
				 {
					 rp = this.uc.getUserModules(cookieLookup);
				 }
				 else if ( action.equals("savemodules"))
				 {
					 rp = this.uc.saveModules(updateItem,cookieLookup);
				 }
			 }
		 }
		 else
		 {
			 //there are no actions that shouldn't require cookies
		 }
		 return new StringRepresentation(rp.toApi(),MediaType.APPLICATION_JSON);
	}
	
}
