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

/**
 * 
 */

import org.restlet.Request;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import com.ikanow.infinit.e.api.utils.RESTTools;
import com.ikanow.infinit.e.data_model.api.ResponsePojo;
import com.ikanow.infinit.e.data_model.api.ResponsePojo.ResponseObject;

import java.util.Date;
import java.util.Map;

/**
 * @author cmorgan
 *
 */
public class FeatureInterface extends ServerResource 
{
	//private static final Logger logger = Logger.getLogger(FeedResource.class);
	
	private String entity = null;
	private String updateItem = null;
	private String action = "";
	private String cookieLookup = null;
	private String cookie = null;
	private boolean needCookie = true;
	
	private FeatureHandler featureHandler = new FeatureHandler();
	
	@Override
	public void doInit() 
	{
		Request request = this.getRequest();
		 String urlStr = request.getResourceRef().toString();
		 cookie = request.getCookies().getFirstValue("infinitecookie",true);
		 
		 Map<String,Object> attributes = getRequest().getAttributes();
		 if ( urlStr.contains("/add/") )
		 {		
			 entity = RESTTools.decodeRESTParam("entity", attributes);
			 updateItem = RESTTools.decodeRESTParam("alias", attributes);
			 action = "add";
		 }
		 else if (urlStr.contains("/approve/"))
		 {
			 updateItem = RESTTools.decodeRESTParam("aliasid",attributes);
			 action = "approve";
		 }
		 else if (urlStr.contains("/decline/"))
		 {
			 updateItem = RESTTools.decodeRESTParam("aliasid",attributes);
			 action = "decline"; 
		 }
		 else if (urlStr.contains("/all"))
		 {
			 action = "all";
		 }
		 else if (urlStr.contains("/feature/entity/"))
		 {
			 updateItem = RESTTools.decodeRESTParam("gazid",attributes);
			 action = "feature";			 
		 }
	}
	
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
		 ResponsePojo rp = new ResponsePojo(); 
		 Date startTime = new Date();	
		 
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
				 if ( action.equals("add"))
				 {
					 rp = this.featureHandler.suggestAlias(entity,updateItem, cookieLookup);
				 }
				 else if ( action.equals("approve"))
				 {
					 rp = this.featureHandler.approveAlias(updateItem);
				 }
				 else if ( action.equals("decline"))
				 {
					 rp = this.featureHandler.declineAlias(updateItem);
				 }
				 else if ( action.equals("all"))
				 {
					 rp = this.featureHandler.allAlias(cookieLookup);
				 }
				 else if ( action.equals("feature"))
				 {
					 rp = this.featureHandler.getEntityFeature(updateItem);
				 }
			 }
		 }
		 else
		 {
			 //no methods that dont need cookies
		 }
		 
		 
		 Date endTime = new Date();
		 rp.getResponse().setTime(endTime.getTime() - startTime.getTime());
		 return new StringRepresentation(rp.toApi(), MediaType.APPLICATION_JSON);
	}
}
