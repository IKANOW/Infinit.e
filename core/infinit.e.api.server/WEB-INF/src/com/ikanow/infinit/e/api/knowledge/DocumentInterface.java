package com.ikanow.infinit.e.api.knowledge;

/**
 * 
 */

import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;
import org.restlet.resource.ResourceException;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;

import com.ikanow.infinit.e.api.utils.RESTTools;
import com.ikanow.infinit.e.data_model.api.ResponsePojo;
import com.ikanow.infinit.e.data_model.api.ResponsePojo.ResponseObject;

import java.util.Date;
import java.util.Map;

/**
 * @author cmorgan
 *
 */
public class DocumentInterface extends Resource 
{
	//private static final Logger logger = Logger.getLogger(FeedResource.class);
	
	private String docid = null;
	private String action = "";
	private String cookieLookup = null;
	private String cookie = null;
	private boolean needCookie = true;
	boolean bReturnFullText = false;
	
	private DocumentHandler docHandler = new DocumentHandler();
	
	public DocumentInterface(Context context, Request request, Response response) 
	{
		 super(context, request, response);
		 String urlStr = request.getResourceRef().toString();
		 cookie = request.getCookies().getFirstValue("infinitecookie",true);
		 
		 Map<String,Object> attributes = getRequest().getAttributes();
		 if ( urlStr.contains("/knowledge/feed/") || urlStr.contains("/knowledge/doc/") || urlStr.contains("/knowledge/document/"))
		 {	
			 docid = RESTTools.decodeRESTParam("feedid", attributes);
			 Map<String, String> queryOptions = this.getQuery().getValuesMap();
			 String returnFullText = queryOptions.get("returnFullText");
			 if ((null != returnFullText) && ((returnFullText.equalsIgnoreCase("true")) || (returnFullText.equals("1")))) {
				 bReturnFullText = true;
			 }
			 action = "doc";
		 }
		 // All modifications of this resource
		 this.setModifiable(true);
		 
		 //this.user = findUser(userid);
		 //getVariants().add(new Variant(MediaType.TEXT_PLAIN));
		 getVariants().add(new Variant(MediaType.APPLICATION_JSON));
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
					 rp = this.docHandler.getInfo(cookieLookup, docid, bReturnFullText);
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
