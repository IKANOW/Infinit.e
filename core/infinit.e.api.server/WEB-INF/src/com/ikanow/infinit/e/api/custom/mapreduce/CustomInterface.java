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
	private String outputKey = null;
	private String outputValue = null;
	private String appendResults = null;
	private String ageOutInDays = null;
	private String jobsToDependOn = null;
	private String json = null;
	private boolean bQuickRun = false;
	private boolean shouldRemoveJar = false;
	private int limit = 0;
	private CustomMapReduceJobPojo jsonPojo = new CustomMapReduceJobPojo();
	private Integer debugLimit = null;
	private String findStr = null;
	private String sortStr = null;
	
	private CustomHandler customhandler = null;
	
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
		 
		 String debugLimitString = queryOptions.get("debugLimit");
		 if ((null != debugLimitString))
		 {
			 try
			 {
				 debugLimit = Integer.parseInt(debugLimitString);
			 }
			 catch (Exception ex)
			 {
				 debugLimit = null;
			 }
		 }
		customhandler = new CustomHandler(debugLimit);
		 
		 String removeJar = queryOptions.get("removeJar");
		 if ((null != removeJar) && ( (removeJar.equals("1")) || (removeJar.equalsIgnoreCase("true")) )) 
		 {
			 shouldRemoveJar = true;			 
		 }
		 String limitString = queryOptions.get("limit");
		 if ((null != limitString))
		 {
			 try
			 {
				 limit = Integer.parseInt(limitString);
			 }
			 catch (Exception ex)
			 {
				 limit = 0;
			 }
		 }
		 String fields = queryOptions.get("fields");
		 if ((null != fields)) {
			 json = fields;
		 }
		 findStr = queryOptions.get("find");
		 sortStr = queryOptions.get("sort");
		 
		 String quickRun = queryOptions.get("quickRun");
		 if ((null != quickRun) && ( (quickRun.equals("1")) || (quickRun.equalsIgnoreCase("true")) )) 
		 {
			 bQuickRun = true;			 
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
			if (urlStr.contains("/knowledge/mapreduce/schedulejob") || urlStr.contains("/custom/mapreduce/schedulejob") || urlStr.contains("/custom/savedquery/schedulejob"))
			{
				action = "schedule";
			}	
			else if ( urlStr.contains("/custom/mapreduce/updatejob" ) || urlStr.contains("/custom/savedquery/updatejob"))
			{
				action = "update";
			}
			try 
			{
				json = entity.getText();
				if ( json != null )
				{
					//convert json to mrpojo
					jsonPojo = ApiManager.mapFromApi(json, CustomMapReduceJobPojo.class, new CustomMapReduceJobPojoApiMap(null));

					// Overwrite command line args (quick and easy way to get POST working)
					// communityIds and jobDependencies still have to be specified at the command line
					if ((null != jsonPojo.scheduleFreq) && (null == freqSched)) {
						//(this has a default value so need to prioritize URL params)
						freqSched = jsonPojo.scheduleFreq.toString();
					}
					if ((Long.MAX_VALUE != jsonPojo.nextRunTime) && (null == nextRunTime)) {
						nextRunTime = Long.toString(jsonPojo.nextRunTime);
					}
					if (null != jsonPojo.jarURL) {
						jarURL = jsonPojo.jarURL;
					}
					if (null != jsonPojo.mapper) {
						mapperClass = jsonPojo.mapper;
					}
					if (null != jsonPojo.combiner) {
						combinerClass = jsonPojo.combiner;
					}
					if (null != jsonPojo.reducer) {
						reducerClass = jsonPojo.reducer;
					}
					if (null != jsonPojo.query) {
						query = jsonPojo.query;
					}
					if (null != jsonPojo.jobtitle) {
						title = jsonPojo.jobtitle;
					}
					if (null != jsonPojo.jobdesc) {
						desc = jsonPojo.jobdesc;
					}
					if (null != jsonPojo.inputCollection) {
						inputColl = jsonPojo.inputCollection;
					}
					if (null != jsonPojo.outputKey) {
						outputKey = jsonPojo.outputKey;
					}
					if (null != jsonPojo.outputValue) {
						outputValue = jsonPojo.outputValue;
					}
					if (null != jsonPojo.appendResults) {
						appendResults = Boolean.toString(jsonPojo.appendResults);
					}
					if (null != jsonPojo.appendAgeOutInDays) {
						ageOutInDays = Double.toString(jsonPojo.appendAgeOutInDays);
					}
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
					 rp = this.customhandler.getJobResults(cookieLookup, jobid, limit, json, findStr, sortStr); 
				 }
				 else if ( action.equals("schedule"))
				 {
					 rp = this.customhandler.scheduleJob(cookieLookup, title, desc, communityIds, jarURL, nextRunTime, freqSched, mapperClass, reducerClass, combinerClass, query, inputColl, outputKey, outputValue,appendResults,ageOutInDays,jsonPojo.incrementalMode,jobsToDependOn,jsonPojo.arguments, jsonPojo.exportToHdfs, bQuickRun, jsonPojo.selfMerge);
				 }
				 else if ( action.equals("update") )
				 {
					 rp = this.customhandler.updateJob(cookieLookup, (jobid==null)?(title):(jobid), title, desc, communityIds, jarURL, nextRunTime, freqSched, mapperClass, reducerClass, combinerClass, query, inputColl, outputKey, outputValue,appendResults,ageOutInDays,jsonPojo.incrementalMode,jobsToDependOn,jsonPojo.arguments, jsonPojo.exportToHdfs, bQuickRun, jsonPojo.selfMerge);
				 }
				 else if ( action.equals("getjobs"))
				 {
					 rp = this.customhandler.getJobOrJobs(cookieLookup, jobid);
				 }
				 else if ( action.equals("removejob") )
				 {
					 rp = CustomHandler.removeJob(cookieLookup, jobid, shouldRemoveJar, false);
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
		 if (!rp.getResponse().isSuccess()) {
			 if (rp.getResponse().getMessage().contains("ermission")) { // likely to be a permissions error
				 RESTTools.logRequest(this);
			 }
		 }//TOTEST (TODO-2194)
		 return new StringRepresentation(rp.toApi(), MediaType.APPLICATION_JSON);
	}		
}
