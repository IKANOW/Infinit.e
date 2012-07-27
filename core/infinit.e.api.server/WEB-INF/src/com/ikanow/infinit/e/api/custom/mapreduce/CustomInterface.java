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
import com.ikanow.infinit.e.data_model.api.ApiManager;
import com.ikanow.infinit.e.data_model.api.ResponsePojo;
import com.ikanow.infinit.e.data_model.api.ResponsePojo.ResponseObject;
import com.ikanow.infinit.e.data_model.api.custom.mapreduce.CustomMapReduceJobPojoApiMap;
import com.ikanow.infinit.e.data_model.store.custom.mapreduce.CustomMapReduceJobPojo;

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
	private String appendResults = null;
	private String ageOutInDays = null;
	private String jobsToDependOn = null;
	private String json = null;
	private boolean shouldRemoveJar = false;
	private CustomMapReduceJobPojo jsonPojo = new CustomMapReduceJobPojo();
	
	private CustomHandler customhandler = new CustomHandler();
	
	// Logging: not currently needed
	//private static final StringBuffer logMsg = new StringBuffer();
	//private static final Logger logger = Logger.getLogger(CustomInterface.class);
	
	@Override
	public void doInit() 
	{
		 Request request = this.getRequest();
		 Map<String,Object> attributes = request.getAttributes();
		 Map<String, String> queryOptions = this.getQuery().getValuesMap();
		 urlStr = request.getResourceRef().toString();
		 cookie = request.getCookies().getFirstValue("infinitecookie",true);	
		 
		 String removeJar = queryOptions.get("removeJar");
		 if ((null != removeJar) && ( (removeJar.equals("1")) || (removeJar.equalsIgnoreCase("true")) )) 
		 {
			 shouldRemoveJar = true;			 
		 }
		 
		 //Method.POST
		 if (request.getMethod() == Method.POST) 
		 {
			 // action determined in next function in chain (post)
			 
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
			 appendResults = RESTTools.decodeRESTParam("appendResults", attributes);
			 ageOutInDays = RESTTools.decodeRESTParam("ageOutInDays", attributes);
			 jobsToDependOn = RESTTools.decodeRESTParam("jobsToDependOn", attributes);
		 }
		 // Method.GET
		 if (request.getMethod() == Method.GET) 
		 {
			 if (urlStr.contains("/knowledge/mapreduce/getresults/") || urlStr.contains("/custom/mapreduce/getresults/") || urlStr.contains("/custom/savedquery/getresults/"))
			 {
				 jobid = RESTTools.decodeRESTParam("jobid", attributes);				 
				 action = "getresults"; 
			 }
			 else if (urlStr.contains("/knowledge/mapreduce/schedulejob") || urlStr.contains("/custom/mapreduce/schedulejob") || urlStr.contains("/custom/savedquery/schedulejob"))
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
				 appendResults = RESTTools.decodeRESTParam("appendResults", attributes);
				 ageOutInDays = RESTTools.decodeRESTParam("ageOutInDays", attributes);
				 jobsToDependOn = RESTTools.decodeRESTParam("jobsToDependOn", attributes);				 				 		
			 }
			 else if ( urlStr.contains("/custom/mapreduce/updatejob") || urlStr.contains("/custom/savedquery/updatejob"))
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
				 appendResults = RESTTools.decodeRESTParam("appendResults", attributes);
				 ageOutInDays = RESTTools.decodeRESTParam("ageOutInDays", attributes);
				 jobsToDependOn = RESTTools.decodeRESTParam("jobsToDependOn", attributes);				 
			 }
			 else if ( urlStr.contains("/custom/mapreduce/removejob") || urlStr.contains("/custom/savedquery/removejob"))
			 {
				 jobid = RESTTools.decodeRESTParam("jobid", attributes);
				 action = "removejob";
			 }
			 else if (urlStr.contains("/knowledge/mapreduce/getjobs") || urlStr.contains("/custom/mapreduce/getjobs") || urlStr.contains("/custom/savedquery/getjobs"))
			 {
				 jobid = RESTTools.decodeRESTParam("jobid", attributes);				 
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
			try 
			{
				json = entity.getText();
				try 
				 {
					 if ( json != null )
					 {
						 json = URLDecoder.decode(json, "UTF-8");
						 //convert json to mrpojo
						 jsonPojo = ApiManager.mapFromApi(json, CustomMapReduceJobPojo.class, new CustomMapReduceJobPojoApiMap(null));						 
					 }
				 }
				 catch (UnsupportedEncodingException e) 
				 {
						action = "failed";
				 }				
				if (urlStr.contains("/knowledge/mapreduce/schedulejob") || urlStr.contains("/custom/mapreduce/schedulejob") || urlStr.contains("/custom/savedquery/schedulejob"))
				{
					action = "schedule";
				}	
				else if ( urlStr.contains("/custom/mapreduce/updatejob" ) || urlStr.contains("/custom/savedquery/updatejob"))
				{
					action = "update";
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
					 rp = this.customhandler.scheduleJob(cookieLookup, title, desc, communityIds, jarURL, nextRunTime, freqSched, mapperClass, reducerClass, combinerClass, query, inputColl, outputKey, outputValue,appendResults,ageOutInDays,jobsToDependOn,jsonPojo.arguments);
				 }
				 else if ( action.equals("update") )
				 {
					 rp = this.customhandler.updateJob(cookieLookup, jobid, title, desc, communityIds, jarURL, nextRunTime, freqSched, mapperClass, reducerClass, combinerClass, query, inputColl, outputKey, outputValue,appendResults,ageOutInDays,jobsToDependOn,jsonPojo.arguments);
				 }
				 else if ( action.equals("getjobs"))
				 {
					 rp = this.customhandler.getJobOrJobs(cookieLookup, jobid);
				 }
				 else if ( action.equals("mapreduce"))
				 {
					 rp = this.customhandler.runMapReduce(cookieLookup, inputColl, map, reduce, query);
				 }
				 else if ( action.equals("removejob") )
				 {
					 rp = this.customhandler.removeJob(cookieLookup, jobid, shouldRemoveJar); 
				 }
				 else if (action.equals("failed")) {
					 rp.setResponse(new ResponseObject("Schedule/Update MapReduce Job", false, "Failed to parse POSTed content"));
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
