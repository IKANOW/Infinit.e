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
package com.ikanow.infinit.e.core.mapreduce;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobStatus;
import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.OutputFormat;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.ikanow.infinit.e.api.knowledge.QueryHandler;
import com.ikanow.infinit.e.data_model.Globals;
import com.ikanow.infinit.e.data_model.api.ApiManager;
import com.ikanow.infinit.e.data_model.api.ResponsePojo;
import com.ikanow.infinit.e.data_model.api.ResponsePojo.ResponseObject;
import com.ikanow.infinit.e.data_model.api.knowledge.AdvancedQueryPojo;
import com.ikanow.infinit.e.data_model.store.BaseDbPojo;
import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.MongoDbManager;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.ikanow.infinit.e.data_model.store.custom.mapreduce.CustomMapReduceJobPojo;
import com.ikanow.infinit.e.data_model.store.custom.mapreduce.CustomMapReduceJobPojo.SCHEDULE_FREQUENCY;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.infinit.e.data_model.store.social.person.PersonCommunityPojo;
import com.ikanow.infinit.e.data_model.store.social.person.PersonPojo;
import com.ikanow.infinit.e.data_model.store.social.sharing.SharePojo;
import com.ikanow.infinit.e.data_model.store.social.sharing.SharePojo.ShareCommunityPojo;
import com.ikanow.infinit.e.harvest.utils.HarvestExceptionUtils;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.gridfs.GridFSDBFile;


public class HadoopJobRunner
{
	private static Logger _logger = Logger.getLogger(HadoopJobRunner.class);
	private com.ikanow.infinit.e.processing.custom.utils.PropertiesManager prop_custom = new com.ikanow.infinit.e.processing.custom.utils.PropertiesManager();
	private com.ikanow.infinit.e.data_model.utils.PropertiesManager prop_general = new com.ikanow.infinit.e.data_model.utils.PropertiesManager();
	final long MS_IN_DAY = 86400000;
	final long SECONDS_60 = 60000;
	
	private boolean bHadoopEnabled = true;
	private boolean bLocalMode = false;
	
	public HadoopJobRunner()
	{
		bLocalMode = prop_custom.getHadoopLocalMode();
		try {
			@SuppressWarnings("unused")
			JobClient jc = new JobClient(getJobClientConnection(), new Configuration());
			if (bLocalMode) {
				System.out.println("Will run hadoop locally (infrastructure appears to exist).");				
			}
		}
		catch (Exception e) { // Hadoop doesn't work
			if (bLocalMode) {
				System.out.println("Will run hadoop locally (no infrastructure).");				
			}
			else {
				System.out.println("No hadoop infrastructure installed, will just look for saved queries.");
			}
			bHadoopEnabled = false;
		}		
	}
	
	public void runScheduledJobs(String jobOverride)
	{				
		//check mongo for jobs needing ran
		
		CustomMapReduceJobPojo job = null;
		if (null != jobOverride) {
			job = CustomMapReduceJobPojo.fromDb(
					MongoDbManager.getCustom().getLookup().findOne(new BasicDBObject(CustomMapReduceJobPojo.jobtitle_, jobOverride)),
					CustomMapReduceJobPojo.class);
			
			if (null != job) {
				job.lastRunTime = new Date();
				job.nextRunTime = job.lastRunTime.getTime();
				if (!bLocalMode) { 
					// Need to store the times or they just get lost between here and the job completion check  
					MongoDbManager.getCustom().getLookup().save(job.toDb());
						// (not that efficient, but this is essentially a DB call so whatever)
				}
				runJob(job);
			}
		}
		else {
			job = getJobsToRun();
			while ( job != null )
			{
				if ( dependenciesNotStartingSoon(job) )
				{
					//Run each job				
					runJob(job);
				}
				//try to get another available job
				job = getJobsToRun();
			}
		}
	}
	
	/**
	 * Checks if any dependent jobs are running or are about to, resets this job to 1 min
	 * in the future if any are.  (This prevents a user from manually starting job A, 
	 * then job B if job A had completed previously, thus job B will have no dependencies).
	 * 
	 * @param cmr
	 * @return
	 */
	private boolean dependenciesNotStartingSoon(CustomMapReduceJobPojo cmr )
	{
		boolean dependencyRunning = false;
		
		try
		{
			BasicDBObject query = new BasicDBObject(CustomMapReduceJobPojo._id_, 
														new BasicDBObject(MongoDbManager.in_, cmr.jobDependencies.toArray()));
			query.put(CustomMapReduceJobPojo.nextRunTime_, 
														new BasicDBObject( MongoDbManager.lt_, new Date().getTime()));
			if ( DbManager.getCustom().getLookup().find(query).size() > 0 )
			{
				dependencyRunning = true;
				//reset this job to 1min in future
				long MS_TO_RESCHEDULE_JOB = 1000*60*1; //ms*s*min
				BasicDBObject updates = new BasicDBObject(CustomMapReduceJobPojo.nextRunTime_, new Date().getTime() + MS_TO_RESCHEDULE_JOB);
				updates.put(CustomMapReduceJobPojo.jobidS_, null);	
				updates.put(CustomMapReduceJobPojo.errorMessage_, "Waiting on a job dependency to finish before starting.");
				DbManager.getCustom().getLookup().update(new BasicDBObject(CustomMapReduceJobPojo._id_, cmr._id),
															new BasicDBObject(MongoDbManager.set_, updates));
			}
		}
		catch (Exception ex)
		{
			_logger.info("job_error_checking_dependencies=" + HarvestExceptionUtils.createExceptionMessage(ex) );
		}
		
		return !dependencyRunning;
	}
	
	private List<ObjectId> getUserCommunities(ObjectId submitterId) {
		// Set up the query
		PersonPojo personQuery = new PersonPojo();
		personQuery.set_id(submitterId);
		
		BasicDBObject dbo = (BasicDBObject) DbManager.getSocial().getPerson().findOne(personQuery.toDb());
		PersonPojo person = PersonPojo.fromDb(dbo, PersonPojo.class);
		
		if (null == person) {
			throw new RuntimeException("User no longer exists?");
		}
		if ((null == person.getCommunities()) || person.getCommunities().isEmpty()) {
			throw new RuntimeException("Corrupt user, no community access?");
		}
		ArrayList<ObjectId> retVal = new ArrayList<ObjectId>(person.getCommunities().size());
		for (PersonCommunityPojo personInfo: person.getCommunities()) {
			retVal.add(personInfo.get_id());
		}
		return retVal;
	}
	
	private void runJob(CustomMapReduceJobPojo job)
	{
		long time_start_setup = new Date().getTime();
		long time_setup = 0;
		try
		{
			shardOutputCollection(job);
			
			// This may be a saved query, if so handle that separately
			if (null == job.jarURL) {
				runSavedQuery(job);
				return;
			}
			
			List<ObjectId> communityIds = getUserCommunities(job.submitterID);
			job.tempJarLocation = downloadJarFile(job.jarURL, communityIds);		
			
			//OLD "COMMAND LINE: CODE
			//add job to hadoop
			//String jobid = runHadoopJob_commandLine(job, job.tempJarLocation);
			
			// Programmatic code:
			String jobid = runHadoopJob(job, job.tempJarLocation);
			
			if ( jobid.equals("local_done")) { // (run locally)
				setJobComplete(job, true, false, -1, -1, null);				
			}
			else if ( jobid != null && !jobid.startsWith("Error") )
			{
				time_setup = new Date().getTime() - time_start_setup;
				_logger.info("job_setup_title=" + job.jobtitle + " job_setup_id=" + job._id.toString() + " job_setup_time=" + time_setup + " job_setup_success=true job_hadoop_id=" + jobid);
				//write jobid back to lookup
				String[] jobParts = jobid.split("_");
				String jobS = jobParts[1];
				int jobN = Integer.parseInt( jobParts[2] );	
				updateJobPojo(job._id, jobS, jobN, job.tempConfigXMLLocation, job.tempJarLocation);
			}
			else
			{
				time_setup = new Date().getTime() - time_start_setup;
				_logger.info("job_setup_title=" + job.jobtitle + " job_setup_id=" + job._id.toString() + " job_setup_time=" + time_setup + " job_setup_success=false  job_setup_message=" + jobid);
				//job failed, send off the error message
				setJobComplete(job, true, true, -1, -1, jobid);
			}
		}
		catch(Exception ex)
		{			
			//job failed, send off the error message
			time_setup = new Date().getTime() - time_start_setup;
			_logger.info("job_setup_title=" + job.jobtitle + " job_setup_id=" + job._id.toString() + " job_setup_time=" + time_setup + " job_setup_success=false job_setup_message=" + HarvestExceptionUtils.createExceptionMessage(ex) );
			setJobComplete(job, true, true, -1, -1, ex.getMessage());
		}
	}
	
	/**
	 * Takes the query argument from a CustomMapReduceJobPojo
	 * and returns either the query or post processing part
	 * 
	 * @param query
	 * @param wantQuery
	 * @return
	 */
	private String getQueryOrProcessing(String query, boolean wantQuery)
	{
		if ( query.equals("") || query.equals("null") || query == null )
			query = "{}";
		DBObject dbo = (DBObject) com.mongodb.util.JSON.parse(query);
		try
		{
			BasicDBList dbl = (BasicDBList)dbo;
			//is a list
			if ( wantQuery )
			{
				return dbl.get(0).toString();
			}
			else
			{
				if ( dbl.size() > 1 )
					return dbl.get(1).toString();
				else
					return null;
			}
		}
		catch (Exception ex)
		{
			try
			{				
				//is just a an object
				if ( wantQuery )
				{
					return dbo.toString();
				}
				else 
				{
					return null;
				}
			}
			catch (Exception e)
			{
				if ( wantQuery )
					return "{}";
				else
					return null;
			}
		}
	}

	//
	// Instead of running a MR job, this will just execute the specified saved query
	//
	
	private void runSavedQuery(CustomMapReduceJobPojo savedQuery) {
				
		// Run saved query:
		
		QueryHandler queryHandler = new QueryHandler();
		
		// Create query object
		
		ResponsePojo rp = null;
		StringBuffer errorString = new StringBuffer("Saved query error");
		try 
		{
			String queryString = getQueryOrProcessing(savedQuery.query,true);			
			AdvancedQueryPojo query = QueryHandler.createQueryPojo(queryString);
			StringBuffer communityIdStrList = new StringBuffer();
			for (ObjectId commId: savedQuery.communityIds) 
			{
				if (communityIdStrList.length() > 0) 
				{
					communityIdStrList.append(',');
				}
				communityIdStrList.append(commId.toString());
			}
			rp = queryHandler.doQuery(savedQuery.submitterID.toString(), query, communityIdStrList.toString(), errorString);
		} 
		catch (Exception e) 
		{
			//DEBUG
			e.printStackTrace();
			errorString.append(": " + e.getMessage());
		}
		if ((null == rp) || (null == rp.getResponse())) { // (this is likely some sort of internal error)
			if (null == rp) 
			{
				rp = new ResponsePojo();
			}
			rp.setResponse(new ResponseObject("Query", false, "Unknown error"));
		}
		if (!rp.getResponse().isSuccess()) {
			setJobComplete(savedQuery, true, true, -1, -1, errorString.append('/').append(rp.getResponse().getMessage()).toString());
			return;
		}
	
		try {		
			// Write to the temp output collection:
		
			DBCollection dbTemp =  DbManager.getCollection(savedQuery.getOutputDatabase(), savedQuery.outputCollectionTemp);
			BasicDBObject outObj = new BasicDBObject();
			outObj.put("_id", new Date()); // (this gets renamed to "key")
			outObj.put("value", com.mongodb.util.JSON.parse(BaseDbPojo.getDefaultBuilder().create().toJson(rp)));
			dbTemp.save(outObj);
		}
		catch (Exception e) { // Any sort of error, just make sure we set the job to complete			
			setJobComplete(savedQuery, true, true, 1, 1, e.getMessage());							
			return;
		}
		// Update job status
		
		setJobComplete(savedQuery, true, false, 1, 1, ApiManager.mapToApi(rp.getStats(), null));							
	}
	
	/**
	 * Attempt to shard the output collection.  If the collection
	 * is already sharded it will just spit back an error which
	 * is fine.
	 * 
	 * @param outputCollection
	 */
	private void shardOutputCollection(CustomMapReduceJobPojo job) 
	{
		//enable sharding for the custommr db incase it hasn't been
		DbManager.getDB("admin").command(new BasicDBObject("enablesharding", job.getOutputDatabase()));
		//enable sharding for the output collection
		if ( job.outputCollection != null )
		{
			BasicDBObject command = new BasicDBObject("shardcollection", job.getOutputDatabase() + "." + job.outputCollection);
			command.append("key", new BasicDBObject("_id", 1));
			DbManager.getDB("admin").command(command);
		}
		//enable sharding on temp output collection
		if ( job.outputCollectionTemp != null )
		{
			BasicDBObject command1 = new BasicDBObject("shardcollection", job.getOutputDatabase() + "." + job.outputCollection);
			command1.append("key", new BasicDBObject("_id", 1));
			DbManager.getDB("admin").command(command1);
		}
	}

	/**
	 * Downloads jar file from web using URL call.  Typically
	 * the jar files we be kept in our /share store so we will
	 * be calling our own api.
	 * 
	 * @param jarURL
	 * @return
	 * @throws Exception 
	 */
	private String downloadJarFile(String jarURL, List<ObjectId> communityIds) throws Exception
	{		
		String shareStringOLD = "$infinite/share/get/";
		String shareStringNEW = "$infinite/social/share/get/";
		String tempFileName = assignNewJarLocation();
		OutputStream out = new BufferedOutputStream(new FileOutputStream(tempFileName));
		if ( jarURL.startsWith(shareStringOLD) || jarURL.startsWith(shareStringNEW))
		{
			//jar is local use id to grab jar (skips authentication)
			String shareid = null;
			if ( jarURL.startsWith(shareStringOLD) )
			{
				shareid = jarURL.substring(shareStringOLD.length());
			}
			else
			{
				shareid = jarURL.substring(shareStringNEW.length());
			}
			BasicDBObject query = new BasicDBObject(SharePojo._id_, new ObjectId(shareid));
			query.put(ShareCommunityPojo.shareQuery_id_, new BasicDBObject(MongoDbManager.in_, communityIds));

			SharePojo share = SharePojo.fromDb(DbManager.getSocial().getShare().findOne(query),SharePojo.class);
			if (null == share) {
				throw new RuntimeException("Can't find JAR file or insufficient permissions");
			}
			if ( share.getBinaryId() != null )
			{			
				GridFSDBFile file = DbManager.getSocial().getShareBinary().find(share.getBinaryId());						
				file.writeTo(out);				
			}
			else
			{
				out.write(share.getBinaryData());
			}
		}
		else
		{
			if (jarURL.startsWith("$infinite")) {
				jarURL = jarURL.replace("$infinite", "http://localhost:8080");
			}
			else if (jarURL.startsWith("file://")) {
				// Can't access the file system, except for this one nominated file:
				if (!jarURL.equals("file:///opt/infinite-home/lib/plugins/infinit.e.hadoop.prototyping_engine.jar")) {
					throw new RuntimeException("Can't find JAR file or insufficient permissions");
				}
			}
			
			//download jar from external site
			URL url = new URL(jarURL);
			
			URLConnection ucon = url.openConnection();
			InputStream in = ucon.getInputStream();
			byte[] buf = new byte[1024];
			int byteRead = 0;
			while ((byteRead = in.read(buf)) != -1 )
			{
				out.write(buf,0,byteRead);				
			}
			in.close();
		}
		out.close();
		return tempFileName;
	}

	private void updateJobPojo(ObjectId _id, String jobids, int jobidn, String xmlLocation, String jarLocation)
	{
		try
		{			
			BasicDBObject set = new BasicDBObject();
			set.append(CustomMapReduceJobPojo.jobidS_, jobids);
			set.append(CustomMapReduceJobPojo.jobidN_, jobidn);
			set.append(CustomMapReduceJobPojo.tempConfigXMLLocation_, xmlLocation);
			set.append(CustomMapReduceJobPojo.tempJarLocation_,jarLocation);
			set.append(CustomMapReduceJobPojo.errorMessage_, null);
			BasicDBObject updateObject = new BasicDBObject(MongoDbManager.set_,set);
			DbManager.getCustom().getLookup().update(new BasicDBObject(CustomMapReduceJobPojo._id_, _id), updateObject);		
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private String runHadoopJob(CustomMapReduceJobPojo job, String tempJarLocation) throws IOException, SAXException, ParserConfigurationException
	{
		StringWriter xml = new StringWriter();
		createConfigXML(xml, job.jobtitle,job.inputCollection, job.isCustomTable, job.getOutputDatabase(), job._id.toString(), job.outputCollectionTemp, job.mapper, job.reducer, job.combiner, getQueryOrProcessing(job.query,true), job.communityIds, job.outputKey, job.outputValue,job.arguments);
		
		ClassLoader savedClassLoader = Thread.currentThread().getContextClassLoader();

		URLClassLoader child = new URLClassLoader (new URL[] { new File(tempJarLocation).toURI().toURL() }, savedClassLoader);			
		Thread.currentThread().setContextClassLoader(child);
		
		// Now load the XML into a configuration object: 
		Configuration config = new Configuration();
		
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(new ByteArrayInputStream(xml.toString().getBytes()));
			NodeList nList = doc.getElementsByTagName("property");
			
			for (int temp = 0; temp < nList.getLength(); temp++) {			 
				Node nNode = nList.item(temp);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {				   
					Element eElement = (Element) nNode;	
					String name = getTagValue("name", eElement);
					String value = getTagValue("value", eElement);
					if ((null != name) && (null != value)) {
						config.set(name, value);
					}
				}
			}
		}
		catch (Exception e) {
			throw new IOException(e.getMessage());
		}
		
		// Now run the JAR file
		try {

			config.setBoolean("mapred.used.genericoptionsparser", true); // (just stops an annoying warning from appearing)
			if (bLocalMode) {
				config.set("mapred.job.tracker", "local");
				config.set("fs.default.name", "local");							
			}
			else {
				String trackerUrl = getXMLProperty(prop_custom.getHadoopConfigPath() + "/hadoop/mapred-site.xml", "mapred.job.tracker");
				String fsUrl = getXMLProperty(prop_custom.getHadoopConfigPath() + "/hadoop/core-site.xml", "fs.default.name");
				config.set("mapred.job.tracker", trackerUrl);
				config.set("fs.default.name", fsUrl);				
			}
						
			Job hj = new Job( config );
			
			Class<?> classToLoad = Class.forName (job.mapper, true, child);			
			hj.setJarByClass(classToLoad);
			hj.setInputFormatClass((Class<? extends InputFormat>) Class.forName ("com.ikanow.infinit.e.data_model.custom.InfiniteMongoInputFormat", true, child));
			hj.setOutputFormatClass((Class<? extends OutputFormat>) Class.forName ("com.mongodb.hadoop.MongoOutputFormat", true, child));
			hj.setMapperClass((Class<? extends Mapper>) Class.forName (job.mapper, true, child));
			hj.setReducerClass((Class<? extends Reducer>) Class.forName (job.reducer, true, child));
			if (null != job.combiner) {
				hj.setCombinerClass((Class<? extends Reducer>) Class.forName (job.combiner, true, child));
			}
			hj.setOutputKeyClass(Class.forName (job.outputKey, true, child));
			hj.setOutputValueClass(Class.forName (job.outputValue, true, child));
			
			hj.setJobName(job.jobtitle);

			if (bLocalMode) {
				hj.waitForCompletion(false);
				return "local_done";
			}
			else {
				hj.submit();
				String jobId = hj.getJobID().toString();
				return jobId;
			}			
		}
		catch (Exception e) {
			
			Thread.currentThread().setContextClassLoader(savedClassLoader);
			return "Error: " + HarvestExceptionUtils.createExceptionMessage(e);
		}
		finally {
			Thread.currentThread().setContextClassLoader(savedClassLoader);
		}
	}
	
	@SuppressWarnings("unused")
	private String runHadoopJob_commandLine(CustomMapReduceJobPojo job, String jar)
	{
		String jobid = null;
		try
		{				
			job.tempConfigXMLLocation = createConfigXML_commandLine(job.jobtitle,job.inputCollection,job._id.toString(),job.tempConfigXMLLocation, job.mapper, job.reducer, job.combiner, getQueryOrProcessing(job.query,true), job.communityIds, job.isCustomTable, job.getOutputDatabase(), job.outputKey, job.outputValue,job.outputCollectionTemp,job.arguments);
			Runtime rt = Runtime.getRuntime();
			String[] commands = new String[]{"hadoop","--config", prop_custom.getHadoopConfigPath() + "/hadoop", "jar", jar, "-conf", job.tempConfigXMLLocation};			
			String command = "";
			for (String s : commands )
				command += s + " ";
			Process pr = rt.exec(commands);
			
			//Once we start running the command attach to stderr to
			//receive the output to parse out the jobid
			InputStream in = pr.getErrorStream();			
			InputStreamReader is = new InputStreamReader(in);
			BufferedReader br = new BufferedReader(is);
			StringBuilder output = new StringBuilder();
	        String line = null;

	        long startTime = new Date().getTime();
	        boolean bGotJobId = false;	        
	        //while we haven't found the id, there are still lines to read, and it hasn't been more than 60 seconds
	        while (!bGotJobId && (line = br.readLine()) != null && (new Date().getTime() - startTime) < SECONDS_60 ) 
	        {	        	
	        	output.append(line);
	        	int getJobIdIndex = -1;  
	        	String searchstring = "INFO mapred.JobClient: Running job: ";
	        	if ((getJobIdIndex = line.indexOf(searchstring)) >= 0) 
	        	{
	        		// Get JobId and trim() it (obviously trivial)
	        		jobid = line.substring(getJobIdIndex + searchstring.length()).trim();
	        		bGotJobId = true;
	        	}
	        }    
	        
	        //60 seconds passed and we never found the id
	        if ( !bGotJobId )
	        {
	        	_logger.info("job_start_timeout_error_title=" + job.jobtitle + " job_start_timeout_error_id=" + job._id.toString() + " job_start_timeout_error_message=" + output.toString());
	        	//if we never found the id mark it as errored out
	        	return "Error:\n" + output.toString();
	        }
		}
		catch (Exception ex)
		{
			//had an error running command
			//probably log error to the job so we stop trying to run it
			_logger.info("job_start_timeout_error_title=" + job.jobtitle + " job_start_timeout_error_id=" + job._id.toString() + " job_start_timeout_error_message=" + HarvestExceptionUtils.createExceptionMessage(ex));
			jobid = "Error:\n" + ex.getMessage(); // (means this gets displayed)			
		}
		return jobid;
	}
	
	/**
	 * Create the xml file that will configure the mongo commands and
	 * write that to the server
	 * 
	 * @param input
	 * @param output
	 * @throws IOException 
	 */
	private String createConfigXML_commandLine( String title, String input, String output, String configLocation, String mapper, String reducer, String combiner, String query, List<ObjectId> communityIds, boolean isCustomTable, String outputDatabase, String outputKey, String outputValue, String tempOutputCollection, String arguments) throws IOException
	{		
		
		if ( configLocation == null )
			configLocation = assignNewConfigLocation();
		
		File configFile = new File(configLocation);
		FileWriter fstream = new FileWriter(configFile);
		BufferedWriter out = new BufferedWriter(fstream);
		createConfigXML(out, title, input, isCustomTable, outputDatabase, output, tempOutputCollection, mapper, reducer, combiner, query, communityIds, outputKey, outputValue, arguments);
		fstream.close();
			
		return configLocation;
	}	
	
	private void createConfigXML( Writer out, String title, String input, boolean isCustomTable, String outputDatabase, String output, String tempOutputCollection, String mapper, String reducer, String combiner, String query, List<ObjectId> communityIds, String outputKey, String outputValue, String arguments) throws IOException
	{
		String dbserver = prop_general.getDatabaseServer();
		output = outputDatabase + "." + tempOutputCollection;

		//add communities to query if this is not a custom table
		if ( !isCustomTable )
		{
			// Start with the old query:
			BasicDBObject oldQueryObj = null;
			if (query.startsWith("{")) {
				oldQueryObj = (BasicDBObject) com.mongodb.util.JSON.parse(query);
			}
			else {
				oldQueryObj = new BasicDBObject();
			}
			
			// Community Ids aren't indexed in the metadata collection, but source keys are, so we need to transform to that
			BasicDBObject keyQuery = new BasicDBObject(SourcePojo.communityIds_, new BasicDBObject(DbManager.in_, communityIds));
			if (oldQueryObj.containsField(DocumentPojo.sourceKey_)) {
				// Source Key specified by user, stick communityIds check in for security
				oldQueryObj.put(DocumentPojo.communityId_, new BasicDBObject(DbManager.in_, communityIds));
			}
			else { // Source key not specified by user, transform communities->sourcekeys
				BasicDBObject keyFields = new BasicDBObject(SourcePojo.key_, 1);
				DBCursor dbc = MongoDbManager.getIngest().getSource().find(keyQuery, keyFields);
				if (dbc.count() > 500) {
					// (too many source keys let's keep the query size sensible...)
					oldQueryObj.put(DocumentPojo.communityId_, new BasicDBObject(DbManager.in_, communityIds));					
				}
				else {
					HashSet<String> sourceKeys = new HashSet<String>();
					while (dbc.hasNext()) {
						DBObject dbo = dbc.next();
						String sourceKey = (String) dbo.get(SourcePojo.key_);
						if (null != sourceKey) {
							sourceKeys.add(sourceKey);
						}
					}
					if (sourceKeys.isEmpty()) { // query returns empty
						throw new RuntimeException("Communities contain no sources");
					}
					BasicDBObject newQueryClauseObj = new BasicDBObject(DbManager.in_, sourceKeys);
					// Now combine the queries...
					oldQueryObj.put(DocumentPojo.sourceKey_, newQueryClauseObj);

				} // (end if too many source keys across the communities)
			}//(end if need to break source keys down into communities)
			query = oldQueryObj.toString();
		}
		else
		{
			//get the custom table (and database)
			input = getCustomDbAndCollection(input);
		}		
		if ( arguments == null )
			arguments = "";
		
		out.write("<?xml version=\"1.0\"?>\n<configuration>"+
				"\n\t<property><!-- name of job shown in jobtracker --><name>mongo.job.name</name><value>"+title+"</value></property>"+
				"\n\t<property><!-- run the job verbosely ? --><name>mongo.job.verbose</name><value>true</value></property>"+
				"\n\t<property><!-- Run the job in the foreground and wait for response, or background it? --><name>mongo.job.background</name><value>false</value></property>"+
				"\n\t<property><!-- If you are reading from mongo, the URI --><name>mongo.input.uri</name><value>mongodb://"+dbserver+"/"+input+"</value></property>"+  
				"\n\t<property><!-- If you are writing to mongo, the URI --><name>mongo.output.uri</name><value>mongodb://"+dbserver+"/"+output+"</value>  </property>"+  
				"\n\t<property><!-- The query, in JSON, to execute [OPTIONAL] --><name>mongo.input.query</name><value>" + query + "</value></property>"+
				"\n\t<property><!-- The fields, in JSON, to read [OPTIONAL] --><name>mongo.input.fields</name><value></value></property>"+
				"\n\t<property><!-- A JSON sort specification for read [OPTIONAL] --><name>mongo.input.sort</name><value></value></property>"+
				"\n\t<property><!-- The number of documents to limit to for read [OPTIONAL] --><name>mongo.input.limit</name><value>0</value><!-- 0 == no limit --></property>"+
				"\n\t<property><!-- The number of documents to skip in read [OPTIONAL] --><!-- TODO - Are we running limit() or skip() first? --><name>mongo.input.skip</name><value>0</value> <!-- 0 == no skip --></property>"+
				"\n\t<property><!-- Class for the mapper --><name>mongo.job.mapper</name><value>"+ mapper+"</value></property>"+
				"\n\t<property><!-- Reducer class --><name>mongo.job.reducer</name><value>"+reducer+"</value></property>"+
				"\n\t<property><!-- InputFormat Class --><name>mongo.job.input.format</name><value>com.ikanow.infinit.e.data_model.custom.InfiniteMongoInputFormat</value></property>"+
				"\n\t<property><!-- OutputFormat Class --><name>mongo.job.output.format</name><value>com.mongodb.hadoop.MongoOutputFormat</value></property>"+
				"\n\t<property><!-- Output key class for the output format --><name>mongo.job.output.key</name><value>"+outputKey+"</value></property>"+
				"\n\t<property><!-- Output value class for the output format --><name>mongo.job.output.value</name><value>"+outputValue+"</value></property>"+
				"\n\t<property><!-- Output key class for the mapper [optional] --><name>mongo.job.mapper.output.key</name><value></value></property>"+
				"\n\t<property><!-- Output value class for the mapper [optional] --><name>mongo.job.mapper.output.value</name><value></value></property>"+
				"\n\t<property><!-- Class for the combiner [optional] --><name>mongo.job.combiner</name><value>"+combiner+"</value></property>"+
				"\n\t<property><!-- Partitioner class [optional] --><name>mongo.job.partitioner</name><value></value></property>"+
				"\n\t<property><!-- Sort Comparator class [optional] --><name>mongo.job.sort_comparator</name><value></value></property>"+
				"\n\t<property><!-- Split Size [optional] --><name>mongo.input.split_size</name><value>32</value></property>"+
				"\n\t<property><!-- User Arguments [optional] --><name>arguments</name><value>"+ StringEscapeUtils.escapeXml(arguments)+"</value></property>"+
				"\n\t<property><!-- Maximum number of splits [optional] --><name>max.splits</name><value>8</value></property>"+
				"\n\t<property><!-- Maximum number of docs per split [optional] --><name>max.docs.per.split</name><value>12500</value></property>"+				
				"\n</configuration>");		
		out.flush();
		out.close();
	}
	
	/**
	 * Returns the current output collection for a certain jobid
	 * This is usually used when a custom input collection is set for a job because
	 * the output collection of another job can change regularly.
	 * 
	 * @param jobid
	 * @return
	 */
	private String getCustomDbAndCollection(String jobid)
	{
		DBObject dbo = DbManager.getCustom().getLookup().findOne(new BasicDBObject(CustomMapReduceJobPojo._id_, new ObjectId(jobid)));
		if ( dbo != null )
		{
			CustomMapReduceJobPojo cmr = CustomMapReduceJobPojo.fromDb(dbo, CustomMapReduceJobPojo.class);
			return cmr.getOutputDatabase() + "." + cmr.outputCollection;
		}
		return null;
	}
	
	/**
	 * Returns a new xml file name following the format
	 * tempConfigXXXX.xml where XXXX is the next incrementing
	 * number in the directory.
	 * 
	 * @return a unique filename for the config file.
	 */
	private String assignNewConfigLocation() 
	{		
		String dirname = prop_custom.getHadoopConfigPath() + "/xmlFiles/";
		File dir = new File(dirname);
		if ( !dir.exists() )
			dir.mkdir();
		String prefix = "tempConfig";
		String suffix = ".xml";
		
		String lastFile = "tempConfig000000.xml";
		String[] filenames = dir.list();
		if ( filenames.length > 0 )
			 lastFile = filenames[filenames.length-1];
		String increment = lastFile.replaceFirst(prefix, "");
		increment = increment.replaceFirst(suffix, "");
		//add 1 to increment, and add leading 0's to keep in order
		String nextNumber = (Integer.parseInt(increment) + 1) + "";
		String zeros = "000000" + nextNumber;
		String newincrement = zeros.substring(zeros.length()-6);
				
		return dirname + prefix + newincrement + suffix;
	}
	
	/**
	 * Returns a new jar file name following the format
	 * tempJarXXXX.jar where XXXX is the next incrementing
	 * number in the directory.
	 * 
	 * @return a unique filename for the jar file.
	 */
	private String assignNewJarLocation() 
	{		
		String dirname = prop_custom.getHadoopConfigPath() + "/jars/";
		File dir = new File(dirname);
		if ( !dir.exists() )
			dir.mkdir();
		String prefix = "tempJar";
		String suffix = ".jar";
		
		String lastFile = "tempJar000000.jar";
		String[] filenames = dir.list();
		if ( filenames.length > 0 )
			 lastFile = filenames[filenames.length-1];
		String increment = lastFile.replaceFirst(prefix, "");
		increment = increment.replaceFirst(suffix, "");
		//add 1 to increment, and add leading 0's to keep in order
		String nextNumber = (Integer.parseInt(increment) + 1) + "";
		String zeros = "000000" + nextNumber;
		String newincrement = zeros.substring(zeros.length()-6);
				
		return dirname + prefix + newincrement + suffix;
	}
	
	/**
	 * Queries mongo to see if any jobs need to be ran now (if their nextRunTime is
	 * less than current time).
	 * 5/23/2012 Burch - Updated to only return 1 job atomically, sets that jobs jobidS to
	 * a blank so other core servers won't attempt to run it.
	 * 
	 * @return a list of jobs that need ran
	 */
	private CustomMapReduceJobPojo getJobsToRun()
	{
		try
		{
			// First off, check the number of running jobs - don't exceed the max
			// (see to run into memory problems if this isn't limited?)
			int nMaxConcurrent = prop_custom.getHadoopMaxConcurrent();
			if (Integer.MAX_VALUE != nMaxConcurrent) {
				BasicDBObject maxQuery = new BasicDBObject(CustomMapReduceJobPojo.jobidS_, new BasicDBObject(DbManager.ne_, null));
				int nCurrRunningJobs = (int) DbManager.getCustom().getLookup().count(maxQuery);
				if (nCurrRunningJobs >= nMaxConcurrent) {
					return null;
				}
			}
			//TESTED
			
			BasicDBObject query = new BasicDBObject();
			query.append(CustomMapReduceJobPojo.jobidS_, null);
			query.append(CustomMapReduceJobPojo.waitingOn_, new BasicDBObject(MongoDbManager.size_, 0)); 
			query.append(CustomMapReduceJobPojo.nextRunTime_, new BasicDBObject(MongoDbManager.lt_, new Date().getTime()));
			if (!bHadoopEnabled && !bLocalMode) {
				// Can only get shared queries:
				query.append("jarURL", null);
			}
			BasicDBObject updates = new BasicDBObject(CustomMapReduceJobPojo.jobidS_, "");
			updates.append("lastRunTime", new Date());
			BasicDBObject update = new BasicDBObject(MongoDbManager.set_, updates);
			DBObject dbo = DbManager.getCustom().getLookup().findAndModify(query,null,null,false,update,true,false);

			if ( dbo != null )
			{		
				return CustomMapReduceJobPojo.fromDb(dbo, CustomMapReduceJobPojo.class);
			}
		}
		catch(Exception ex)
		{
			//oh noes!
			ex.printStackTrace();
		}
		
		return null;
	}
	
	private CustomMapReduceJobPojo getJobsToMakeComplete()
	{
		try
		{						
			BasicDBObject query = new BasicDBObject();
			BasicDBObject nors[] = new BasicDBObject[3];
			nors[0] = new BasicDBObject(CustomMapReduceJobPojo.jobidS_, null);
			nors[1] = new BasicDBObject(CustomMapReduceJobPojo.jobidS_, "CHECKING_COMPLETION");
			nors[2] = new BasicDBObject(CustomMapReduceJobPojo.jobidS_, "");
			query.put(MongoDbManager.nor_, Arrays.asList(nors));					
			BasicDBObject updates = new BasicDBObject(CustomMapReduceJobPojo.jobidS_, "CHECKING_COMPLETION");			
			BasicDBObject update = new BasicDBObject(MongoDbManager.set_, updates);
			if (!bHadoopEnabled) {
				// Can only get shared queries:
				query.append(CustomMapReduceJobPojo.jarURL_, null);
			}
			DBObject dbo = DbManager.getCustom().getLookup().findAndModify(query, update);

			if ( dbo != null )
			{		
				return CustomMapReduceJobPojo.fromDb(dbo, CustomMapReduceJobPojo.class);
			}
		}
		catch(Exception ex)
		{
			//oh noes!
			ex.printStackTrace();
		}
		
		return null;
	}

	/**
	 * Checks any running/queued jobs and updates their status if they've completed
	 */
	public void updateJobStatus() 
	{
		Map<ObjectId, String> incompleteJobsMap = new HashMap<ObjectId, String>();
		//get mongo entries that have jobids?
		try
		{
			JobClient jc = null;
			
			CustomMapReduceJobPojo cmr = getJobsToMakeComplete();
			while (cmr != null)
			{		
				boolean markedComplete = false;
				//make sure its an actual ID, we now set jobidS to "" when running the job
				if ( !cmr.jobidS.equals("") )
				{
					if (null == jc) 
					{
						try 
						{
							jc = new JobClient(getJobClientConnection(), new Configuration());
						}
						catch (Exception e) 
						{ 
							// Better delete this, no idea what's going on....						
							_logger.info("job_update_status_error_title=" + cmr.jobtitle + " job_update_status_error_id=" + cmr._id.toString() + " job_update_status_error_message=Skipping job: " + cmr.jobidS + cmr.jobidN + ", this node does not run mapreduce");							
							setJobComplete(cmr,true,true, -1,-1, "Failed to launch job, unknown error (check configuration in  /opt/hadoop-infinite/mapreduce/hadoop/, jobtracker may be localhost?).");							
							cmr = getJobsToMakeComplete();
							continue;
						}
					}
					
					//check if job is done, and update if it is					
					JobStatus[] jobs = jc.getAllJobs();
					boolean bFound = false;
					for ( JobStatus j : jobs )
					{
						if ( j.getJobID().getJtIdentifier().equals(cmr.jobidS) && j.getJobID().getId() == cmr.jobidN )
						{
							bFound = true;
							boolean error = false;
							markedComplete = j.isJobComplete();
							String errorMessage = null;
							if ( JobStatus.FAILED == j.getRunState() )
							{
								markedComplete = true;
								error = true;
								errorMessage = "Job failed while running, check for errors in the mapper/reducer or that your key/value classes are set up correctly?";
							}
							setJobComplete(cmr, markedComplete, error, j.mapProgress(),j.reduceProgress(), errorMessage);
							break; // (from mini loop over hadoop jobs, not main loop over infinite tasks)
						}
					}					
					if (!bFound) { // Possible error
						//check if its been longer than 5min and mark job as complete (it failed to launch)
						Date currDate = new Date();
						Date lastDate = cmr.lastRunTime;
						//if its been more than 5 min (5m*60s*1000ms)					
						if ( currDate.getTime() - lastDate.getTime() > 300000 )
						{
							markedComplete = true;						
							setJobComplete(cmr,true,true, -1,-1, "Failed to launch job, unknown error #2.");
						}
					}
				}
				else // this job hasn't been started yet:
				{
					//check if its been longer than 5min and mark job as complete (it failed to launch)
					Date currDate = new Date();
					Date lastDate = cmr.lastRunTime;
					//if its been more than 5 min (5m*60s*1000ms)					
					if ( currDate.getTime() - lastDate.getTime() > 300000 )
					{
						markedComplete = true;						
						setJobComplete(cmr,true,true, -1,-1, "Failed to launch job, unknown error #1.");
					}
				}
				//job was not done, need to set flag back
				if ( !markedComplete )
				{
					incompleteJobsMap.put(cmr._id, cmr.jobidS);
				}
				cmr = getJobsToMakeComplete();
			}	
		}
		catch (Exception ex)
		{
			_logger.info("job_error_checking_status_message=" + HarvestExceptionUtils.createExceptionMessage(ex) );			
		}	
		catch (Error err) {
			// Really really want to get to the next line of code, and clear the status...
		}
				
		//set all incomplete jobs back
		for (ObjectId id : incompleteJobsMap.keySet())
		{		
			BasicDBObject update = new BasicDBObject(CustomMapReduceJobPojo.jobidS_, incompleteJobsMap.get(id));
			DbManager.getCustom().getLookup().update(new BasicDBObject(CustomMapReduceJobPojo._id_, id), 
														new BasicDBObject(MongoDbManager.set_, update));
		}		
	}

	/**
	 * Sets the custom mr pojo to be complete for the
	 * current job.  Currently this is done by removing the
	 * jobid and updating the next runtime, increments the
	 * amount of timeRan counter as well so we can calculate nextRunTime
	 * 
	 * Also set lastCompletion time to now (best we can approx)
	 * 
	 * @param cmr
	 */
	private void setJobComplete(CustomMapReduceJobPojo cmr, boolean isComplete, boolean isError, float mapProgress, float reduceProgress, String errorMessage) 
	{		
		try
		{			
			BasicDBObject updates = new BasicDBObject();
			BasicDBObject update = new BasicDBObject();
			if ( isComplete )
			{				
				updates.append(CustomMapReduceJobPojo.jobidS_, null);
				updates.append(CustomMapReduceJobPojo.jobidN_,0);
				try 
				{
					long nextRunTime = getNextRunTime(cmr.scheduleFreq, cmr.firstSchedule, cmr.nextRunTime, cmr.timesRan+1);
					//if next run time reschedules to run before now, keep rescheduling until its later
					//the server could have been turned off for days and would try to rerun all jobs once a day
					while ( nextRunTime < new Date().getTime() )
					{
						Date firstSchedule = new Date(nextRunTime);
						cmr.firstSchedule = firstSchedule;
						updates.append(CustomMapReduceJobPojo.firstSchedule_, firstSchedule);
						nextRunTime = getNextRunTime(cmr.scheduleFreq, cmr.firstSchedule, cmr.nextRunTime, cmr.timesRan+1);
					}
					updates.append(CustomMapReduceJobPojo.nextRunTime_,nextRunTime);
				}
				catch (Exception e) {} // just carry on, we'll live...
				
				updates.append(CustomMapReduceJobPojo.lastCompletionTime_, new Date());
				updates.append(CustomMapReduceJobPojo.tempConfigXMLLocation_,null);
				updates.append(CustomMapReduceJobPojo.tempJarLocation_,null);
				try 
				{
					removeTempFile(cmr.tempConfigXMLLocation);
					removeTempFile(cmr.tempJarLocation);					
				}
				catch (Exception e) 
				{
					_logger.info("job_error_removing_tempfiles=" + HarvestExceptionUtils.createExceptionMessage(e));
				} 
				
				BasicDBObject incs = new BasicDBObject("timesRan", 1);				
				//copy depencies to waitingOn
				updates.append(CustomMapReduceJobPojo.waitingOn_, cmr.jobDependencies);
				if ( !isError )
				{
					updates.append(CustomMapReduceJobPojo.errorMessage_, errorMessage); // (will often be null)
					moveTempOutput(cmr);
					//if job was successfully, mark off dependencies
					removeJobFromChildren(cmr._id);					
				}
				else
				{
					//failed, just append error message										
					updates.append(CustomMapReduceJobPojo.errorMessage_, errorMessage);
					incs.append(CustomMapReduceJobPojo.timesFailed_,1);					
				}
				update.append(MongoDbManager.inc_, incs);
				long runtime = new Date().getTime() - cmr.lastRunTime.getTime();
				long timeFromSchedule = cmr.lastRunTime.getTime() - cmr.nextRunTime;
				
				if (null != cmr.jobidS) 
				{
					_logger.info("job_completion_title=" + cmr.jobtitle + " job_completion_id="+cmr._id.toString() + " job_completion_time=" + runtime + " job_schedule_delta=" + timeFromSchedule + " job_completion_success=" + !isError + " job_hadoop_id=" + cmr.jobidS + "_" + cmr.jobidN);
				}
				else 
				{
					_logger.info("job_completion_title=" + cmr.jobtitle + " job_completion_id="+cmr._id.toString() + " job_completion_time=" + runtime + " job_schedule_delta=" + timeFromSchedule + " job_completion_success=" + !isError);					
				}
			}
			updates.append(CustomMapReduceJobPojo.mapProgress_, mapProgress);
			updates.append(CustomMapReduceJobPojo.reduceProgress_, reduceProgress);			
			update.append(MongoDbManager.set_,updates);				
			DbManager.getCustom().getLookup().update(new BasicDBObject(CustomMapReduceJobPojo._id_,cmr._id),update);					
		}
		catch (Exception ex)
		{
			_logger.info("job_error_updating_status_title=" + cmr.jobtitle + " job_error_updating_status_id=" + cmr._id.toString() + " job_error_updating_status_message="+HarvestExceptionUtils.createExceptionMessage(ex));
		}		
	}
	
	/**
	 * Removes the jobID from the waitingOn field of any of the children
	 * 
	 * @param jobID
	 * @param children
	 */
	private void removeJobFromChildren(ObjectId jobID)
	{
		BasicDBObject query = new BasicDBObject(CustomMapReduceJobPojo.waitingOn_, jobID);
		DbManager.getCustom().getLookup().update(query, new BasicDBObject(MongoDbManager.pull_, query), false, true);
	}	
	
	/**
	 * Moves the output of a job from output_tmp to output and deletes
	 * the tmp collection.
	 * 
	 * @param cmr
	 */
	private void moveTempOutput(CustomMapReduceJobPojo cmr)
	{
		/**
		 * Atomic plan:
		 * If not append, move customlookup pointer to tmp collection, drop old collection.
		 * If append, set sync flag (find/mod), move results from tmp to old, unset sync flag.
		 * 
		 */	
		//step1 build out any of the post proc arguments
		DBObject postProcObject = null;
		boolean limitAllData = true;
		boolean hasSort = false;
		int limit = 0;
		BasicDBObject sort = new BasicDBObject();
		try
		{
			postProcObject = (DBObject) com.mongodb.util.JSON.parse(getQueryOrProcessing(cmr.query, false));
			if ( postProcObject != null )
			{
				if ( postProcObject.containsField("limitAllData") )
				{
					limitAllData = (Boolean) postProcObject.get("limitAllData");
				}
				if ( postProcObject.containsField("limit") )
				{
					limit = (Integer) postProcObject.get("limit");
					if ( postProcObject.containsField("sortField"))
					{
						String sfield = (String) postProcObject.get("sortField");
						int sortDir = 1;
						if ( postProcObject.containsField("sortDirection"))
						{
							sortDir = (Integer)postProcObject.get("sortDirection");
						}					
						sort.put(sfield, sortDir);
						hasSort = true;
					}
					else if ( limit > 0 )
					{
						//set a default sort because the user posted a limit
						sort.put("_id", -1);
						hasSort = true;
					}
				}
			}
		}
		catch (Exception ex)
		{
			_logger.info("job_error_post_proc_title=" + cmr.jobtitle + " job_error_post_proc_id=" + cmr._id.toString() + " job_error_post_proc_message="+HarvestExceptionUtils.createExceptionMessage(ex));
		}
		
		
		//step 2a if not appending results then work on temp collection and swap to main
		if ( (null == cmr.appendResults) || !cmr.appendResults ) //format temp then change lookup pointer to temp collection
		{
			//transform all the results into necessary format:			
			DBCursor dbc_tmp = DbManager.getCollection(cmr.getOutputDatabase(), cmr.outputCollectionTemp).find(new BasicDBObject("key", null)).sort(sort).limit(limit);
			while (dbc_tmp.hasNext())
			{
				DBObject dbo = dbc_tmp.next();				
				Object key = dbo.get("_id");
				dbo.put("key", key);
				dbo.removeField("_id");								
				DbManager.getCollection(cmr.getOutputDatabase(), cmr.outputCollectionTemp).insert(dbo);
			}					
			DbManager.getCollection(cmr.getOutputDatabase(), cmr.outputCollectionTemp).remove(new BasicDBObject("key", null));
			
			//swap the output collections
			BasicDBObject notappendupdates = new BasicDBObject(CustomMapReduceJobPojo.outputCollection_, cmr.outputCollectionTemp);
			notappendupdates.append(CustomMapReduceJobPojo.outputCollectionTemp_, cmr.outputCollection);
			DbManager.getCustom().getLookup().findAndModify(new BasicDBObject(CustomMapReduceJobPojo._id_, cmr._id), 
																new BasicDBObject(MongoDbManager.set_, notappendupdates));						
			String temp = cmr.outputCollectionTemp;
			cmr.outputCollectionTemp = cmr.outputCollection;
			cmr.outputCollection = temp;
		}
		else //step 2b if appending results then drop modified results in output collection
		{
			DbManager.getCustom().getLookup().findAndModify(new BasicDBObject(CustomMapReduceJobPojo._id_, cmr._id), 
															new BasicDBObject(MongoDbManager.set_, new BasicDBObject("isUpdatingOutput", true)));
			//remove any aged out results
			if ( (null != cmr.appendAgeOutInDays) && cmr.appendAgeOutInDays > 0 )
			{
				//remove any results that have aged out
				long ageOutMS = (long) (cmr.appendAgeOutInDays*MS_IN_DAY);
				Date lastAgeOut = new Date(((new Date()).getTime() - ageOutMS));						
				DbManager.getCollection(cmr.getOutputDatabase(), cmr.outputCollection).remove(new BasicDBObject("_id",
														new BasicDBObject(MongoDbManager.lt_,new ObjectId(lastAgeOut))));
			}
			DBCursor dbc_tmp;
			if ( !limitAllData )
			{				
				//sort and limit the temp data set because we only want to process it
				dbc_tmp = DbManager.getCollection(cmr.getOutputDatabase(), cmr.outputCollectionTemp).find(new BasicDBObject("key", null)).sort(sort).limit(limit);
				limit = 0; //reset limit so we get everything in a few steps (we only want to limit the new data)
			}
			else
			{
				dbc_tmp = DbManager.getCollection(cmr.getOutputDatabase(), cmr.outputCollectionTemp).find(new BasicDBObject("key", null));
			}
			
			DBCollection dbc = DbManager.getCollection(cmr.getOutputDatabase(), cmr.outputCollection);
			//transform temp results and dump into output collection
			while ( dbc_tmp.hasNext() )
			{
				DBObject dbo = dbc_tmp.next();
				//transform the dbo to format {_id:ObjectId, key:(prev_id), value:value}
				Object key = dbo.get("_id");
				dbo.put("key", key);
				dbo.removeField("_id");
				//_id field should be automatically set to objectid when inserting now
				dbc.insert(dbo);
			}			
			//if there is a sort, we need to apply it to all the data now
			if ( hasSort )
			{
				ObjectId OID = new ObjectId();
				BasicDBObject query = new BasicDBObject("_id", new BasicDBObject(MongoDbManager.lt_, OID));
				//find everything inserted before now and sort/limit the data
				DBCursor dbc_sort = dbc.find(query).sort(sort).limit(limit);
				while ( dbc_sort.hasNext() )
				{
					//reinsert the data into db (it should be in sorted order naturally now)
					DBObject dbo = dbc_sort.next();
					dbo.removeField("_id");
					dbc.insert(dbo);
				}
				//remove everything inserted before we reorganized everything (should leave only the new results in natural order)
				dbc.remove(query);
			}
			DbManager.getCustom().getLookup().findAndModify(new BasicDBObject(CustomMapReduceJobPojo._id_, cmr._id), 
										new BasicDBObject(MongoDbManager.set_, new BasicDBObject("isUpdatingOutput", false)));
		}
		//step3 clean up temp output collection so we can use it again
		DbManager.getCollection(cmr.getOutputDatabase(), cmr.outputCollectionTemp).remove(new BasicDBObject());
		
	}		
	
	/**
	 * Uses a map reduce jobs schedule frequency to determine when the next
	 * map reduce job should be ran.
	 * 
	 * @param scheduleFreq
	 * @param firstSchedule
	 * @param iterations
	 * @return
	 */
	private long getNextRunTime(SCHEDULE_FREQUENCY scheduleFreq, Date firstSchedule, long nextRuntime, int iterations) 
	{
		if (null == firstSchedule) {
			firstSchedule = new Date(nextRuntime);
			iterations = 1; // recover...
		}
		
		if ( scheduleFreq == null || SCHEDULE_FREQUENCY.NONE == scheduleFreq)
		{
			return Long.MAX_VALUE;
		}
		Calendar cal = new GregorianCalendar();
		cal.setTime(firstSchedule);
		
		if ( SCHEDULE_FREQUENCY.DAILY == scheduleFreq)
		{
			cal.add(Calendar.HOUR, 24*iterations);
		}
		else if ( SCHEDULE_FREQUENCY.WEEKLY == scheduleFreq)
		{
			cal.add(Calendar.DATE, 7*iterations);
		}
		else if ( SCHEDULE_FREQUENCY.MONTHLY == scheduleFreq)
		{
			cal.add(Calendar.MONTH, 1*iterations);
		}
		return cal.getTimeInMillis();
	}
	
	/**
	 * Calls the XML Parser to grab the job client address and opens a connection to
	 * the server.  The parameters must be in the hadoopconfig/mapred-site.xml file
	 * under the property "mapred.job.tracker"
	 * 
	 * @return Connection to the job client
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 */
	private InetSocketAddress getJobClientConnection() throws SAXException, IOException, ParserConfigurationException
	{
		String jobclientAddress = getXMLProperty(prop_custom.getHadoopConfigPath() + "/hadoop/mapred-site.xml", "mapred.job.tracker");
		String[] parts = jobclientAddress.split(":");
		String hostname = parts[0];
		int port = Integer.parseInt(parts[1]);		
		return new InetSocketAddress(hostname, port);
	}
	
//////////////////////////////////////////////////////////////////////////////////////////////////

	// Utilities

	private static String getTagValue(String sTag, Element eElement) {
		NodeList nlList = eElement.getElementsByTagName(sTag).item(0).getChildNodes();		 
		Node nValue = (Node) nlList.item(0);
		if (null != nValue) {
			return nValue.getNodeValue();
		}
		else {
			return null;
		}
	}
	/**
	 * Removes the config file that is not being used anymore.
	 * 
	 * @param file
	 */
	private void removeTempFile(String file)
	{
		if ( file != null )
		{
			File f = new File(file);
			f.delete();
		}
	}
	/**
	 * Parses a given xml file and returns the requested value of propertyName.
	 * The XML is expected to be in a format: <configuration><property><name>some.prop.name</name><value>some.value</value></property></configuration>
	 * 
	 * @param xmlFileLocation
	 * @param propertyName
	 * @return
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 */
	private String getXMLProperty(String xmlFileLocation, String propertyName) throws SAXException, IOException, ParserConfigurationException
	{
		File configFile = new File(xmlFileLocation);
		
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        Document doc = docBuilder.parse(configFile);        
        doc.getDocumentElement().normalize();
        
        NodeList listOfProps = doc.getElementsByTagName("property");
        
        for ( int i = 0; i < listOfProps.getLength(); i++ )
        {
        	Node prop = listOfProps.item(i);
        	if ( prop.getNodeType() == Node.ELEMENT_NODE)
        	{
	        	Element propElement = (Element)prop;	        	
	        	NodeList name = propElement.getElementsByTagName("name").item(0).getChildNodes();
	        	Node nameValue = (Node) name.item(0);
	        	String nameString = nameValue.getNodeValue().trim();
	        	
	        	//found the correct property
	        	if ( nameString.equals(propertyName) )
	        	{
	        		//return the value
	        		NodeList value = propElement.getElementsByTagName("value").item(0).getChildNodes();
		        	Node valueValue = (Node) value.item(0);
		        	String valueString = valueValue.getNodeValue().trim();		        	
		        	return valueString;		        	
	        	}
        	}
        }
        return null;
	}
	
/////////////////////////////////////////////////////////////////////////////
	
	// Test code
	
	public static void main(String[] args) {
		
		Globals.setIdentity(com.ikanow.infinit.e.data_model.Globals.Identity.IDENTITY_SERVICE);
		Globals.overrideConfigLocation(args[0]);

		// Write temp test code here
	}	
}
