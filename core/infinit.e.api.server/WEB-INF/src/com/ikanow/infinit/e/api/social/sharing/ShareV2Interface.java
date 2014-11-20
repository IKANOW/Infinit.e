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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;
import org.mozilla.universalchardet.UniversalDetector;
import org.restlet.Request;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.Put;
import org.restlet.resource.ServerResource;

import com.ikanow.infinit.e.api.utils.RESTTools;
import com.ikanow.infinit.e.data_model.api.ApiManager;
import com.ikanow.infinit.e.data_model.api.BasePojoApiMap;
import com.ikanow.infinit.e.data_model.api.ResponsePojo;
import com.ikanow.infinit.e.data_model.api.ResponsePojo.ResponseObject;
import com.ikanow.infinit.e.data_model.api.social.sharing.SharePojoApiMap;
import com.ikanow.infinit.e.data_model.store.social.sharing.SharePojo;
import com.ikanow.infinit.e.data_model.store.social.sharing.SharePojo.ShareCommunityPojo;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;

public class ShareV2Interface extends ServerResource
{
	// 
	private ShareHandler shareController = new ShareHandler();
	private static String ACTION = "SHARE V2";
	private String cookieLookup = null;
	private String cookie = null;
	SharePojo sharePojo = null;
	private String id = null;
	private boolean returnContent = true;
	private boolean jsonOnly = false; 
	private String searchids = null;
	private String skip = null;
	private String limit = null;
	private String type = null; //overloaded for searching and adding/updating
	private String data_type = null;
	private String title = null;
	private String description = null;
	private String searchby = null;
	private String communityIds = null;
	private boolean searchParent = false;	 
	private boolean ignoreAdmin = false;	
	private boolean readWrite = false;
	
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
		Map<String, String> queryOptions = this.getQuery().getValuesMap();
		if (RESTTools.getUrlAttribute("id", attributes, queryOptions) != null) id = RESTTools.getUrlAttribute("id", attributes, queryOptions);
				
		
		if (queryOptions.get("id") != null) searchids = queryOptions.get("id");
		if (queryOptions.get("skip") != null) skip = queryOptions.get("skip");
		if (queryOptions.get("limit") != null) limit = queryOptions.get("limit");
		if (queryOptions.get("searchby") != null) searchby = queryOptions.get("searchby");
		if (queryOptions.get("type") != null) type = queryOptions.get("type");
		if (queryOptions.get("data_type") != null) data_type = queryOptions.get("data_type");
		if (queryOptions.get("title") != null) title = queryOptions.get("title");
		if (queryOptions.get("description") != null) description = queryOptions.get("description");
		if (queryOptions.get("communityIds") != null) communityIds = queryOptions.get("communityIds");
		
		if ((queryOptions.get("ignoreAdmin") != null) && (queryOptions.get("ignoreAdmin").equalsIgnoreCase("true"))) {
			ignoreAdmin = true;				
		}
		if ((queryOptions.get("nocontent") != null) && (queryOptions.get("nocontent").equalsIgnoreCase("true"))) {
			returnContent = false;				
		}
		if ((queryOptions.get("nometa") != null) && (queryOptions.get("nometa").equalsIgnoreCase("true"))) {
			jsonOnly = true;
		}
		if ((queryOptions.get("searchParent") != null) && (queryOptions.get("searchParent").equalsIgnoreCase("true"))) {
			searchParent = true;
		}
		if ((queryOptions.get("readWrite") != null) && (queryOptions.get("readWrite").equalsIgnoreCase("true"))) {
			readWrite = true;
		}	
		
		//Every user must pass in their cookies	
		cookie = request.getCookies().getFirstValue("infinitecookie", true);
		cookieLookup = RESTTools.cookieLookup(cookie);			
	}
	
	//RETRIEVE
	@Get
	public Representation get() 
	{		
		Date startTime = new Date();
		ResponsePojo rp = new ResponsePojo();
		if ( cookieLookup != null )
		{
			if ( id != null )
			{		
				//GET A SPECIFIC SHARE
				rp = shareController.getShare(cookieLookup, id, returnContent);	
				SharePojo share = (SharePojo) rp.getData();
				if (null != share) 
				{
					boolean bBinary = share.getType().equals("binary");
					if ( bBinary && returnContent )					 
					{			
						try
						{							 
							ByteArrayOutputRepresentation rep = new ByteArrayOutputRepresentation(MediaType.valueOf(share.getMediaType()));
							rep.setOutputBytes(share.getBinaryData());
							return rep;							 
						}
						catch (Exception ex )
						{
							rp = new ResponsePojo(new ResponseObject(ACTION,false,"error converting bytes to output: " + ex.getMessage()));
						}						 
					}
					else if (!bBinary && jsonOnly) 
					{
						try 
						{
							BasicDBObject dbo = (BasicDBObject) com.mongodb.util.JSON.parse(share.getShare());
							rp.setData(dbo, null);
						}
						catch (Exception e) 
						{ // Try a list instead						
							BasicDBList dbo = (BasicDBList) com.mongodb.util.JSON.parse(share.getShare());
							rp.setData(dbo, (BasePojoApiMap<BasicDBList>)null);								 
						}
					}
				}
			}
			else
			{
				//SEARCH
				rp = this.shareController.searchShares(cookieLookup, searchby, searchids, type, skip, limit, ignoreAdmin, returnContent, searchParent);
			}
		}
		else
		{
			rp = new ResponsePojo(new ResponseObject("Cookie Lookup",false,"Cookie session expired or never existed, please login first"));
		}
		
		return returnRepresentation(rp, startTime);
	}

	//CREATE
	@Post
	public Representation post(Representation entity)   
	{
		ResponsePojo rp = new ResponsePojo(); 
		Date startTime = new Date();
		
		if ( cookieLookup != null )
		{
			SharePojo share = null;
			try
			{
				share = parseEntity(entity);	
				share.set_id(null); //is create function, id can't exist
			}
			catch (Exception ex)
			{
				rp.setResponse(new ResponseObject(ACTION, false, ex.getMessage()));
				return returnRepresentation(rp, startTime);
			}
			
			String message = validateSharePojo(share, true);
			if ( message != null )
			{
				//failed to validate
				rp.setResponse(new ResponseObject(ACTION, false, message));
			}
			else
			{
				rp = this.shareController.createOrUpdateShare(cookieLookup, share, readWrite, returnContent);
			}	
		}
		else
		{
			rp = new ResponsePojo(new ResponseObject("Cookie Lookup",false,"Cookie session expired or never existed, please login first"));
		}
		
		return returnRepresentation(rp, startTime);
	}

	//UPDATE
	@Put
	public Representation put(Representation entity)
	{
		ResponsePojo rp = new ResponsePojo(); 
		Date startTime = new Date();
		if ( cookieLookup != null )
		{
			SharePojo share = null;
			try
			{
				share = parseEntity(entity);
			}
			catch (Exception ex)
			{
				rp.setResponse(new ResponseObject(ACTION, false, ex.getMessage()));
				return returnRepresentation(rp, startTime);
			}
			
			String message = validateSharePojo(share, false);
			if ( message != null )
			{
				//failed to validate
				rp.setResponse(new ResponseObject(ACTION, false, message));
			}
			else
			{
				rp = this.shareController.createOrUpdateShare(cookieLookup, share, readWrite, returnContent);
			}		
		}
		else
		{
			rp = new ResponsePojo(new ResponseObject("Cookie Lookup",false,"Cookie session expired or never existed, please login first"));
		}
		
		return returnRepresentation(rp, startTime);
	}
	
	//DELETE
	@Delete
	public Representation delete(Representation entity)
	{
		ResponsePojo rp = new ResponsePojo(); 
		Date startTime = new Date();
		
		if ( cookieLookup != null )
		{
			if ( id == null )
			{
				//check if its in the body
				SharePojo share = null;
				try
				{
					share = parseEntity(entity);
					if ( share != null && share.get_id() != null )
						id = share.get_id().toString();
					
				}
				catch (Exception ex)
				{
					//should just fail, this is fine
				}			
			}
			
			if ( id != null )
			{
				rp = this.shareController.removeShare(cookieLookup, id);
			}
			else
			{
				rp.setResponse(new ResponseObject(ACTION, false, "shareid is required and was not supplied"));
			}
		}
		else
		{
			rp = new ResponsePojo(new ResponseObject("Cookie Lookup",false,"Cookie session expired or never existed, please login first"));
		}
		
		return returnRepresentation(rp, startTime);
	}
	
	private SharePojo parseEntity(Representation entity) throws Exception
	{
		SharePojo share = null;
		
		//try to figure out what entity is (because for binary it can be a stream
		if ( entity == null )
		{
			//is empty, hopefully they passed args as url query params
			share = new SharePojo();			
		}
		else if ( entity.getMediaType().equals(MediaType.APPLICATION_JSON))
		{
			if ( data_type != null && data_type.toLowerCase().equals("json"))
			{
				//assume its only the data
				share = new SharePojo();
				share.setShare(entity.getText());
			}
			else
			{
				//is just a regular json share, parse it
				share = ApiManager.mapFromApi(entity.getText(), SharePojo.class, new SharePojoApiMap(null));
			}			
		}
		else
		{
			//is binary, pull in the bytes
			share = new SharePojo();
			InputStream instream = entity.getStream();
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			int nRead;
			byte[] data = new byte[16384];
			while ((nRead = instream.read(data,0,data.length)) != -1)
			{
				buffer.write(data,0,nRead);
			}
			buffer.flush();
			share.setBinaryData( buffer.toByteArray() );
			share.setMediaType(entity.getMediaType().toString());
		}
		
		if ( share.getShare() != null )
			checkForBadCharacters(share.getShare());
		
		//url params always overwrite passed in json
		if ( type != null )
			share.setType(type);
		if ( title != null )
			share.setTitle(title);
		if ( description != null )
			share.setDescription(description);
		if ( id != null )
			share.set_id(new ObjectId(id));
		if ( communityIds != null )
		{
			String[] splits = communityIds.split("\\s*,\\s*");
			List<ShareCommunityPojo> comms = new ArrayList<SharePojo.ShareCommunityPojo>();
			for ( String s : splits )
			{
				ShareCommunityPojo scp = new ShareCommunityPojo();
				scp.set_id(new ObjectId(s));
				comms.add(scp);
			}
			share.setCommunities(comms);
		}
		
		return share;
	}

	private String validateSharePojo(SharePojo share, boolean isNew) 
	{
		if ( isNew )
		{
			//validate these fields only for new shares
			if ( share.getType() == null )
				return "type field is required";
			if ( share.getTitle() == null )
				return "title field is required";
			if ( share.getDescription() == null )
				return "description field is required";
			if ( share.getCommunities() == null || share.getCommunities().isEmpty() )
				return "at least 1 community is required";
		}
		else
		{
			//validate these fields only for existing shares
			if ( share.get_id() ==  null )
				return "_id field was null";
		}
		//validate these fields for both
		if ( share.getCommunities() != null && share.getCommunities().isEmpty() )
			return "at least 1 community is required";
		
		//remove these fields for both
		
		return null;
	}
	
	private Representation returnRepresentation(ResponsePojo rp, Date startTime) 
	{
		Date endTime = new Date();
		rp.getResponse().setTime(endTime.getTime() - startTime.getTime());
		if (!rp.getResponse().isSuccess()) 
		{
			if (rp.getResponse().getMessage().contains("ermission")) 
			{ 
				// likely to be a permissions error
				RESTTools.logRequest(this);
			}
		}
		return new StringRepresentation(rp.toApi(), MediaType.APPLICATION_JSON);
	}
	
	/**
	 * Attempts to test if json is UTF-8, throws an exception
	 * if it detects a different charset
	 * 
	 * @param json
	 * @throws UnsupportedEncodingException 
	 * @throws Exception
	 */
	private void checkForBadCharacters(String json) throws Exception
	{
		byte[] bytes = json.getBytes();		
		UniversalDetector ud = new UniversalDetector(null);
		ud.handleData(bytes, 0, bytes.length);
		ud.dataEnd();
		String encoding = ud.getDetectedCharset();			
		if ( encoding != null )
		{
			//do an extra check for charcode 65533 if encoding is utf-8		
			if ( encoding.equals("UTF-8"))
			{
				for (int i = 0; i < json.length(); i ++)
				{
					if ( 65533 == Character.codePointAt(new char[]{json.charAt(i)}, 0) )
					{
						throw new Exception("Found illegal character at index: " + i);
					}
				}
			}
			else 	
			{
				//if encoding is not utf8 or null, fail	
				throw new Exception("Illegal encoding found: " + encoding);
			}
		}
	}	
}
