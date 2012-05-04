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
package com.ikanow.infinit.e.api.custom.mapreduce;

import java.util.Date;
import java.util.Map;

import org.restlet.Request;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import com.ikanow.infinit.e.api.utils.RESTTools;
import com.ikanow.infinit.e.data_model.api.ResponsePojo;
import com.ikanow.infinit.e.data_model.api.ResponsePojo.ResponseObject;

public class CustomInterface extends ServerResource 
{
	private String action = "";	
	private String cookieLookup = null;
	private String cookie = null;
	private boolean needCookie = true;
	private String urlStr = null;
	private String jobid = null;
	private String communityIds = null;
	private String jarURL = null;
	private String nextRunTime = null;
	private String freqSched = null;
	private String mapperClass = null;
	private String reducerClass = null;
	private String combinerClass = null;
	private String query = null;
	private String title = null;
	private String desc = null;
	private String inputColl = null;
	private String map = null;
	private String reduce = null;
	private String outputKey = null;
	private String outputValue = null;
	
	private CustomHandler customhandler = new CustomHandler();
	
	// Logging: not currently needed
	//private static final StringBuffer logMsg = new StringBuffer();
	//private static final Logger logger = Logger.getLogger(CustomInterface.class);
	
	@Override
	public void doInit() 
	{
		 Request request = this.getRequest();
		 Map<String,Object> attributes = request.getAttributes();
		 urlStr = request.getResourceRef().toString();
		 cookie = request.getCookies().getFirstValue("infinitecookie",true);	
		 
		 // Method.GET
		 if (request.getMethod() == Method.GET) 
		 {
			 if (urlStr.contains("/knowledge/mapreduce/getresults/") || urlStr.contains("/custom/mapreduce/getresults/"))
			 {
				 jobid = RESTTools.decodeRESTParam("jobid", attributes);				 
				 action = "getresults"; 
			 }
			 else if (urlStr.contains("/knowledge/mapreduce/schedulejob") || urlStr.contains("/custom/mapreduce/schedulejob"))
			 {
				 action = "schedule";
				 communityIds = RESTTools.decodeRESTParam("communityIds", attributes);
				 freqSched = RESTTools.decodeRESTParam("frequencyToRun", attributes);
				 nextRunTime = RESTTools.decodeRESTParam("timeToRun", attributes);
				 jarURL = RESTTools.decodeRESTParam("jarURL", attributes);
				 mapperClass = RESTTools.decodeRESTParam("mapperClass", attributes);
				 reducerClass = RESTTools.decodeRESTParam("reducerClass", attributes);
				 combinerClass = RESTTools.decodeRESTParam("combinerClass", attributes);
				 query = RESTTools.decodeRESTParam("query", attributes);
				 title = RESTTools.decodeRESTParam("jobtitle", attributes);
				 desc = RESTTools.decodeRESTParam("jobdesc", attributes);
				 inputColl = RESTTools.decodeRESTParam("inputcollection", attributes);
				 outputKey = RESTTools.decodeRESTParam("outputKey", attributes);
				 outputValue = RESTTools.decodeRESTParam("outputValue", attributes);
			 }
			 else if ( urlStr.contains("/custom/mapreduce/updatejob") )
			 {
				 action = "update";
				 jobid = RESTTools.decodeRESTParam("jobid", attributes);
				 communityIds = RESTTools.decodeRESTParam("communityIds", attributes);
				 freqSched = RESTTools.decodeRESTParam("frequencyToRun", attributes);
				 nextRunTime = RESTTools.decodeRESTParam("timeToRun", attributes);
				 jarURL = RESTTools.decodeRESTParam("jarURL", attributes);
				 mapperClass = RESTTools.decodeRESTParam("mapperClass", attributes);
				 reducerClass = RESTTools.decodeRESTParam("reducerClass", attributes);
				 combinerClass = RESTTools.decodeRESTParam("combinerClass", attributes);
				 query = RESTTools.decodeRESTParam("query", attributes);
				 title = RESTTools.decodeRESTParam("jobtitle", attributes);
				 desc = RESTTools.decodeRESTParam("jobdesc", attributes);
				 inputColl = RESTTools.decodeRESTParam("inputcollection", attributes);
				 outputKey = RESTTools.decodeRESTParam("outputKey", attributes);
				 outputValue = RESTTools.decodeRESTParam("outputValue", attributes);
			 }
			 else if ( urlStr.contains("/custom/mapreduce/removejob"))
			 {
				 jobid = RESTTools.decodeRESTParam("jobid", attributes);
				 action = "removejob";
			 }
			 else if (urlStr.contains("/knowledge/mapreduce/getjobs") || urlStr.contains("/custom/mapreduce/getjobs"))
			 {
				 action = "getjobs";
			 }
			 else if (urlStr.contains("/knowledge/mapreduce/") || urlStr.contains("/custom/mapreduce/"))
			 {
				 action = "mapreduce";
				 map = RESTTools.decodeRESTParam("map", attributes);
				 reduce = RESTTools.decodeRESTParam("reduce", attributes);
				 query = RESTTools.decodeRESTParam("query", attributes);
				 inputColl = RESTTools.decodeRESTParam("inputcollection", attributes);
			 }
		 }		 
	}
	
	
	/**
	 * acceptRepresentation
	 */
	@Post
	public Representation post(Representation entity) throws ResourceException 
	{
		if (Method.POST == getRequest().getMethod()) 
		{
			//do nothing?
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
	public Representation get( ) throws ResourceException 
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
				 if ( action.equals("getresults") )
				 {
					 rp = this.customhandler.getJobResults(cookieLookup, jobid); 
				 }
				 else if ( action.equals("schedule"))
				 {
					 rp = this.customhandler.scheduleJob(cookieLookup, title, desc, communityIds, jarURL, nextRunTime, freqSched, mapperClass, reducerClass, combinerClass, query, inputColl, outputKey, outputValue);
				 }
				 else if ( action.equals("update") )
				 {
					 rp = this.customhandler.updateJob(cookieLookup, jobid, title, desc, communityIds, jarURL, nextRunTime, freqSched, mapperClass, reducerClass, combinerClass, query, inputColl, outputKey, outputValue);
				 }
				 else if ( action.equals("getjobs"))
				 {
					 rp = this.customhandler.getAllJobs(cookieLookup);
				 }
				 else if ( action.equals("mapreduce"))
				 {
					 rp = this.customhandler.runMapReduce(cookieLookup, inputColl, map, reduce, query);
				 }
				 else if ( action.equals("removejob") )
				 {
					 rp = this.customhandler.removeJob(cookieLookup, jobid); 
				 }
			 }			 
		 }		 
		 else
		 {
			// Note: Currently there are no methods that can be called without a cookie
		 }
		 
		 Date endTime = new Date();
		 rp.getResponse().setTime(endTime.getTime() - startTime.getTime());
		 return new StringRepresentation(rp.toApi(), MediaType.APPLICATION_JSON);
	}		
}
