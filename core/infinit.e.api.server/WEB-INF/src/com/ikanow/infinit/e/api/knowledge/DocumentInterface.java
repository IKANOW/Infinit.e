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
import org.restlet.resource.ServerResource;

import com.ikanow.infinit.e.api.knowledge.DocumentHandler.DocumentFileInterface;
import com.ikanow.infinit.e.api.social.sharing.ByteArrayOutputRepresentation;
import com.ikanow.infinit.e.api.utils.RESTTools;
import com.ikanow.infinit.e.data_model.api.ResponsePojo;
import com.ikanow.infinit.e.data_model.api.ResponsePojo.ResponseObject;

import java.util.Date;
import java.util.Map;

/**
 * @author cmorgan
 *
 */
public class DocumentInterface extends ServerResource 
{
	//private static final Logger logger = Logger.getLogger(FeedResource.class);
	
	private String docid = null;
	private String sourcekey = null;
	private String action = "";
	private String cookieLookup = null;
	private String cookie = null;
	private boolean needCookie = true;
	boolean bReturnFullText = false;
	boolean returnRawData = false;
	
	private DocumentHandler docHandler = new DocumentHandler();
	
	@Override
	public void doInit()	
	{
		Request request = this.getRequest();
		String urlStr = request.getResourceRef().toString();
		 cookie = request.getCookies().getFirstValue("infinitecookie",true);
		 
		 Map<String,Object> attributes = request.getAttributes();
		 if (urlStr.contains("/document/file/get/")) {
			 sourcekey = RESTTools.decodeRESTParam("sourcekey", attributes);
			 int nIndexOfRelativePath = urlStr.indexOf("/document/file/get/"); //19B 
			 nIndexOfRelativePath += 19 + sourcekey.length() + 1; // (the +1 for the trailing /)
			 docid = urlStr.substring(nIndexOfRelativePath);
			 int nEndOfUrl = docid.lastIndexOf('?');
			 if (nEndOfUrl > 0) {
				 docid = docid.substring(0, nEndOfUrl);
			 }
			 action = "file";
			 returnRawData = true;
			 //TESTED
		 }
		 else if ( urlStr.contains("/knowledge/feed/") || urlStr.contains("/knowledge/doc/") || urlStr.contains("/knowledge/document/"))
		 {	
			 docid = RESTTools.decodeRESTParam("docid", attributes);
			 if (null == docid) {
				 docid = RESTTools.decodeRESTParam("url", attributes);
				 sourcekey = RESTTools.decodeRESTParam("sourcekey", attributes);
			 }
			 Map<String, String> queryOptions = this.getQuery().getValuesMap();
			 String returnFullText = queryOptions.get("returnFullText");			 
			 if ((null != returnFullText) && ((returnFullText.equalsIgnoreCase("true")) || (returnFullText.equals("1")))) 
			 {
				 bReturnFullText = true;
			 }
			 String returnRawData = queryOptions.get("returnRawData");
			 if ((null != returnRawData) && ((returnRawData.equalsIgnoreCase("true")) || (returnRawData.equals("1")))) 
			 {
				 this.returnRawData = true;
			 }
			 action = "doc";
		 }
	}
	
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
				 if ( action.equals("doc"))
				 {
					 rp = this.docHandler.getInfo(cookieLookup, sourcekey, docid, bReturnFullText, returnRawData);
					 //return full text takes precedence over raw data
				 }
				 else if ( action.equals("file"))
				 {
					rp = this.docHandler.getFileContents(cookieLookup, sourcekey, docid); 
				 }
				 
				 if ( !bReturnFullText && returnRawData && rp.getResponse().isSuccess() )
				 {		
					 try
					 {
						 //return the bytes like we do in shares
						 DocumentFileInterface dfp = (DocumentFileInterface) rp.getData();
						 if (null != dfp) {
							 ByteArrayOutputRepresentation rep = new ByteArrayOutputRepresentation(MediaType.valueOf(dfp.mediaType));
							 rep.setOutputBytes(dfp.bytes);
							 return rep;
						 }
					 }
					 catch (Exception ex )
					 {
						 rp = new ResponsePojo(new ResponseObject("Doc Info", false, "error converting bytes to output"));
					 }	
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
