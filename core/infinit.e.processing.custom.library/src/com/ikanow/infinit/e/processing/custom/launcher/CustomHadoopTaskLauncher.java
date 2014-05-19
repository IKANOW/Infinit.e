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
package com.ikanow.infinit.e.processing.custom.launcher;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.OutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.bson.types.ObjectId;
import org.elasticsearch.common.joda.time.Interval;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.ikanow.infinit.e.api.knowledge.QueryHandler;
import com.ikanow.infinit.e.data_model.api.knowledge.AdvancedQueryPojo;
import com.ikanow.infinit.e.data_model.custom.InfiniteFileInputFormat;
import com.ikanow.infinit.e.data_model.custom.InfiniteMongoSplitter;
import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.MongoDbUtil;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.ikanow.infinit.e.data_model.store.custom.mapreduce.CustomMapReduceJobPojo;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.infinit.e.processing.custom.output.CustomOutputManager;
import com.ikanow.infinit.e.processing.custom.utils.HadoopUtils;
import com.ikanow.infinit.e.processing.custom.utils.InfiniteHadoopUtils;
import com.ikanow.infinit.e.processing.custom.utils.PropertiesManager;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

public class CustomHadoopTaskLauncher extends AppenderSkeleton {

	private static Logger _logger = Logger.getLogger(CustomHadoopTaskLauncher.class);
	private com.ikanow.infinit.e.data_model.utils.PropertiesManager prop_general = new com.ikanow.infinit.e.data_model.utils.PropertiesManager();
	private PropertiesManager props_custom = null;
	private boolean bLocalMode;
	private Integer nDebugLimit;
	private boolean bTestMode = false;
	
	public CustomHadoopTaskLauncher(boolean bLocalMode_, Integer nDebugLimit_, PropertiesManager prop_custom_)
	{
		bLocalMode = bLocalMode_;
		nDebugLimit = nDebugLimit_;
		props_custom = prop_custom_;
		if (null != nDebugLimit) {
			bTestMode = true;			
		}
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public String runHadoopJob(CustomMapReduceJobPojo job, String tempJarLocation) throws IOException, SAXException, ParserConfigurationException
	{
		StringWriter xml = new StringWriter();
		String outputCollection = job.outputCollectionTemp;// (non-append mode) 
		if ( ( null != job.appendResults ) && job.appendResults) 
			outputCollection = job.outputCollection; // (append mode, write directly in....)
		else if ( null != job.incrementalMode )
			job.incrementalMode = false; // (not allowed to be in incremental mode and not update mode)
		
		createConfigXML(xml, job.jobtitle,job.inputCollection, InfiniteHadoopUtils.getQueryOrProcessing(job.query,InfiniteHadoopUtils.QuerySpec.INPUTFIELDS), job.isCustomTable, job.getOutputDatabase(), job._id.toString(), outputCollection, job.mapper, job.reducer, job.combiner, InfiniteHadoopUtils.getQueryOrProcessing(job.query,InfiniteHadoopUtils.QuerySpec.QUERY), job.communityIds, job.outputKey, job.outputValue,job.arguments, job.incrementalMode, job.submitterID);
		
		ClassLoader savedClassLoader = Thread.currentThread().getContextClassLoader();
				
		URLClassLoader child = new URLClassLoader (new URL[] { new File(tempJarLocation).toURI().toURL() }, savedClassLoader);			
		Thread.currentThread().setContextClassLoader(child);

		// Check version: for now, any infinit.e.data_model with an VersionTest class is acceptable
		boolean dataModelLoaded = true;
		try {
			URLClassLoader versionTest = new URLClassLoader (new URL[] { new File(tempJarLocation).toURI().toURL() }, null);
			try {
				Class.forName("com.ikanow.infinit.e.data_model.custom.InfiniteMongoInputFormat", true, versionTest);				
			}
			catch (ClassNotFoundException e2) {
				//(this is fine, will use the cached version)
				dataModelLoaded = false;
			}
			if (dataModelLoaded)
				Class.forName("com.ikanow.infinit.e.data_model.custom.InfiniteMongoVersionTest", true, versionTest);
		} 
		catch (ClassNotFoundException e1) {
			throw new RuntimeException("This JAR is compiled with too old a version of the data-model, please recompile with Jan 2014 (rc2) onwards");
		}		
		
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
			BasicDBObject advancedConfigurationDbo = null;
			try {
				advancedConfigurationDbo = (null != job.query) ? ((BasicDBObject) com.mongodb.util.JSON.parse(job.query)) : (new BasicDBObject());
			}
			catch (Exception e) {
				advancedConfigurationDbo = new BasicDBObject();
			}
			boolean esMode = advancedConfigurationDbo.containsField("qt") && !job.isCustomTable;
			if (esMode && !job.inputCollection.equals("doc_metadata.metadata")) {
				throw new RuntimeException("Infinit.e Queries are only supported on doc_metadata - use MongoDB queries instead.");
			}

			config.setBoolean("mapred.used.genericoptionsparser", true); // (just stops an annoying warning from appearing)
			if (bLocalMode) { // local job tracker and FS mode
				config.set("mapred.job.tracker", "local");
				config.set("fs.default.name", "local");		
			}
			else {
				if (bTestMode) { // run job tracker locally but FS mode remotely
					config.set("mapred.job.tracker", "local");					
				}
				else { // normal job tracker
					String trackerUrl = HadoopUtils.getXMLProperty(props_custom.getHadoopConfigPath() + "/hadoop/mapred-site.xml", "mapred.job.tracker");
					config.set("mapred.job.tracker", trackerUrl);
				}
				String fsUrl = HadoopUtils.getXMLProperty(props_custom.getHadoopConfigPath() + "/hadoop/core-site.xml", "fs.default.name");
				config.set("fs.default.name", fsUrl);				
			}
			if (!dataModelLoaded && !(bTestMode || bLocalMode)) { // If running distributed and no data model loaded then add ourselves
				Path jarToCache = InfiniteHadoopUtils.cacheLocalFile("/opt/infinite-home/lib/", "infinit.e.data_model.jar", config);
				DistributedCache.addFileToClassPath(jarToCache, config);
				jarToCache = InfiniteHadoopUtils.cacheLocalFile("/opt/infinite-home/lib/", "infinit.e.processing.custom.library.jar", config);
				DistributedCache.addFileToClassPath(jarToCache, config);
			}//TESTED
			
			// Debug scripts (only if they exist), and only in non local/test mode
			if (!bLocalMode && !bTestMode) {
				
				try {
					Path scriptToCache = InfiniteHadoopUtils.cacheLocalFile("/opt/infinite-home/scripts/", "custom_map_error_handler.sh", config);
					config.set("mapred.map.task.debug.script", "custom_map_error_handler.sh " + job.jobtitle);
					config.set("mapreduce.map.debug.script", "custom_map_error_handler.sh " + job.jobtitle);						
					DistributedCache.createSymlink(config);
					DistributedCache.addCacheFile(scriptToCache.toUri(), config);
				}
				catch (Exception e) {} // just carry on
				
				try {
					Path scriptToCache = InfiniteHadoopUtils.cacheLocalFile("/opt/infinite-home/scripts/", "custom_reduce_error_handler.sh", config);
					config.set("mapred.reduce.task.debug.script", "custom_reduce_error_handler.sh " + job.jobtitle);
					config.set("mapreduce.reduce.debug.script", "custom_reduce_error_handler.sh " + job.jobtitle);						
					DistributedCache.createSymlink(config);
					DistributedCache.addCacheFile(scriptToCache.toUri(), config);
				}
				catch (Exception e) {} // just carry on
				
			}//TOTEST
			
			// Manually specified caches
			List<URL> localJarCaches = 
					InfiniteHadoopUtils.handleCacheList(advancedConfigurationDbo.get("$caches"), job, config, props_custom);			
			
			Job hj = new Job( config );
			try {
				
				if (null != localJarCaches) {
					if (bLocalMode || bTestMode) {
						Method method = URLClassLoader.class.getDeclaredMethod("addURL", new Class[]{URL.class});
						method.setAccessible(true);
						method.invoke(child, localJarCaches.toArray());
						
					}//TOTEST (tested logically)
				}				
				Class<?> classToLoad = Class.forName (job.mapper, true, child);			
				hj.setJarByClass(classToLoad);
				
				if (job.inputCollection.equalsIgnoreCase("filesystem")) {
					String inputPath = null;
					try {
						inputPath = MongoDbUtil.getProperty(advancedConfigurationDbo, "file.url");
						if (!inputPath.endsWith("/")) {
							inputPath = inputPath + "/";
						}
					}
					catch (Exception e) {}
					if (null == inputPath) {
						throw new RuntimeException("Must specify 'file.url' if reading from filesystem.");
					}
					inputPath = InfiniteHadoopUtils.authenticateInputDirectory(job, inputPath);
					
					InfiniteFileInputFormat.addInputPath(hj, new Path(inputPath + "*/*")); // (that extra bit makes it recursive)
					InfiniteFileInputFormat.setMaxInputSplitSize(hj, 33554432); // (32MB)
					InfiniteFileInputFormat.setInfiniteInputPathFilter(hj, config);
					hj.setInputFormatClass((Class<? extends InputFormat>) Class.forName ("com.ikanow.infinit.e.data_model.custom.InfiniteFileInputFormat", true, child));
				}
				else {
					if (esMode) {
						hj.setInputFormatClass((Class<? extends InputFormat>) Class.forName ("com.ikanow.infinit.e.processing.custom.utils.InfiniteElasticsearchMongoInputFormat", true, child));						
					}
					else {
						hj.setInputFormatClass((Class<? extends InputFormat>) Class.forName ("com.ikanow.infinit.e.data_model.custom.InfiniteMongoInputFormat", true, child));
					}
				}
				if ((null != job.exportToHdfs) && job.exportToHdfs) {
					
					//TODO (INF-2469): Also, if the output key is BSON then also run as text (but output as JSON?)
					
					Path outPath = InfiniteHadoopUtils.ensureOutputDirectory(job, props_custom);
					
					if ((null != job.outputKey) && (null != job.outputValue) && job.outputKey.equalsIgnoreCase("org.apache.hadoop.io.text") && job.outputValue.equalsIgnoreCase("org.apache.hadoop.io.text"))
					{
						// (slight hack before I sort out the horrendous job class - if key/val both text and exporting to HDFS then output as Text)
						hj.setOutputFormatClass((Class<? extends OutputFormat>) Class.forName ("org.apache.hadoop.mapreduce.lib.output.TextOutputFormat", true, child));
						TextOutputFormat.setOutputPath(hj, outPath);
					}//TESTED
					else {
						hj.setOutputFormatClass((Class<? extends OutputFormat>) Class.forName ("org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat", true, child));					
						SequenceFileOutputFormat.setOutputPath(hj, outPath);
					}//TESTED
				}
				else { // normal case, stays in MongoDB
					hj.setOutputFormatClass((Class<? extends OutputFormat>) Class.forName ("com.ikanow.infinit.e.data_model.custom.InfiniteMongoOutputFormat", true, child));
				}
				hj.setMapperClass((Class<? extends Mapper>) Class.forName (job.mapper, true, child));
				String mapperOutputKeyOverride = advancedConfigurationDbo.getString("$mapper_key_class", null);
				if (null != mapperOutputKeyOverride) {
					hj.setMapOutputKeyClass(Class.forName(mapperOutputKeyOverride));
				}//TESTED 
				
				String mapperOutputValueOverride = advancedConfigurationDbo.getString("$mapper_value_class", null);
				if (null != mapperOutputValueOverride) {
					hj.setMapOutputValueClass(Class.forName(mapperOutputValueOverride));
				}//TESTED 
				
				if ((null != job.reducer) && !job.reducer.startsWith("#") && !job.reducer.equalsIgnoreCase("null") && !job.reducer.equalsIgnoreCase("none")) {
					hj.setReducerClass((Class<? extends Reducer>) Class.forName (job.reducer, true, child));
					// Variable reducers:
					if (null != job.query) {
						try { 
							hj.setNumReduceTasks(advancedConfigurationDbo.getInt("$reducers", 1));
						}catch (Exception e) {
							try {
								// (just check it's not a string that is a valid int)
								hj.setNumReduceTasks(Integer.parseInt(advancedConfigurationDbo.getString("$reducers", "1")));
							}
							catch (Exception e2) {}
						}
					}//TESTED
				}
				else {
					hj.setNumReduceTasks(0);
				}
				if ((null != job.combiner) && !job.combiner.startsWith("#") && !job.combiner.equalsIgnoreCase("null") && !job.combiner.equalsIgnoreCase("none")) {
					hj.setCombinerClass((Class<? extends Reducer>) Class.forName (job.combiner, true, child));
				}
				hj.setOutputKeyClass(Class.forName (job.outputKey, true, child));
				hj.setOutputValueClass(Class.forName (job.outputValue, true, child));
				
				hj.setJobName(job.jobtitle);
				currJobName = job.jobtitle;
			}
			catch (Error e) { // (messing about with class loaders = lots of chances for errors!)
				throw new RuntimeException(e.getMessage(), e);
			}
			if (bTestMode || bLocalMode) {				
				hj.submit();
				currThreadId = null;
				Logger.getRootLogger().addAppender(this);
				currLocalJobId = hj.getJobID().toString();
				currLocalJobErrs.setLength(0);
				while (!hj.isComplete()) {
					Thread.sleep(1000);
				}
				Logger.getRootLogger().removeAppender(this);
				if (hj.isSuccessful()) {
					if (this.currLocalJobErrs.length() > 0) {
						return "local_done: " + this.currLocalJobErrs.toString();						
					}
					else {
						return "local_done";
					}
				}
				else {
					return "Error: " + this.currLocalJobErrs.toString();
				}
			}
			else {
				hj.submit();
				String jobId = hj.getJobID().toString();
				return jobId;
			}			
		}
		catch (Exception e) {
			e.printStackTrace();
			Thread.currentThread().setContextClassLoader(savedClassLoader);
			return "Error: " + InfiniteHadoopUtils.createExceptionMessage(e);
		}
		finally {
			Thread.currentThread().setContextClassLoader(savedClassLoader);
		}
	}
	
	public String runHadoopJob_commandLine(CustomMapReduceJobPojo job, String jar)
	{
		String jobid = null;
		try
		{				
			job.tempConfigXMLLocation = createConfigXML_commandLine(job.jobtitle,job.inputCollection,job._id.toString(),job.tempConfigXMLLocation, job.mapper, job.reducer, job.combiner, InfiniteHadoopUtils.getQueryOrProcessing(job.query,InfiniteHadoopUtils.QuerySpec.QUERY), job.communityIds, job.isCustomTable, job.getOutputDatabase(), job.outputKey, job.outputValue,job.outputCollectionTemp,job.arguments, job.incrementalMode, job.submitterID);
			Runtime rt = Runtime.getRuntime();
			String[] commands = new String[]{"hadoop","--config", props_custom.getHadoopConfigPath() + "/hadoop", "jar", jar, "-conf", job.tempConfigXMLLocation};			
			String command = "";
			for (String s : commands )
				command += s + " ";
			Process pr = rt.exec(command);
			
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
	        while (!bGotJobId && (line = br.readLine()) != null && (new Date().getTime() - startTime) < InfiniteHadoopUtils.SECONDS_60 ) 
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
			_logger.info("job_start_timeout_error_title=" + job.jobtitle + " job_start_timeout_error_id=" + job._id.toString() + " job_start_timeout_error_message=" + InfiniteHadoopUtils.createExceptionMessage(ex));
			jobid = "Error:\n" + ex.getMessage(); // (means this gets displayed)			
		}
		return jobid;
	}
	
	////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////
	
	// UTILS
	
	/**
	 * Create the xml file that will configure the mongo commands and
	 * write that to the server
	 * 
	 * @param input
	 * @param output
	 * @throws IOException 
	 */
	private String createConfigXML_commandLine( String title, String input, String output, String configLocation, String mapper, String reducer, String combiner, String query, List<ObjectId> communityIds, boolean isCustomTable, String outputDatabase, String outputKey, String outputValue, String tempOutputCollection, String arguments, Boolean incrementalMode, ObjectId userId) throws IOException
	{		
		
		if ( configLocation == null )
			configLocation = InfiniteHadoopUtils.assignNewConfigLocation(props_custom);
		
		File configFile = new File(configLocation);
		FileWriter fstream = new FileWriter(configFile);
		BufferedWriter out = new BufferedWriter(fstream);
		createConfigXML(out, title, input, InfiniteHadoopUtils.getQueryOrProcessing(query,InfiniteHadoopUtils.QuerySpec.INPUTFIELDS), isCustomTable, outputDatabase, output, tempOutputCollection, mapper, reducer, combiner, query, communityIds, outputKey, outputValue, arguments, incrementalMode, userId);
		fstream.close();
			
		return configLocation;
	}	
	
	private void createConfigXML( Writer out, String title, String input, String fields, boolean isCustomTable, String outputDatabase, String output, String tempOutputCollection, String mapper, String reducer, String combiner, String query, List<ObjectId> communityIds, String outputKey, String outputValue, String arguments, Boolean incrementalMode, ObjectId userId) throws IOException
	{
		String dbserver = prop_general.getDatabaseServer();
		output = outputDatabase + "." + tempOutputCollection;

		
		int nSplits = 8;
		int nDocsPerSplit = 12500;
		
		//add communities to query if this is not a custom table
		BasicDBObject oldQueryObj = null;
		BasicDBObject srcTags = null;
		// Start with the old query:
		if (query.startsWith("{")) {
			oldQueryObj = (BasicDBObject) com.mongodb.util.JSON.parse(query);
		}
		else {
			oldQueryObj = new BasicDBObject();
		}
		boolean elasticsearchQuery = oldQueryObj.containsField("qt") && !isCustomTable;
		int nLimit = 0;
		if (oldQueryObj.containsField("$limit")) {
			nLimit = oldQueryObj.getInt("$limit");
			oldQueryObj.remove("$limit");
		}
		if (oldQueryObj.containsField("$splits")) {
			nSplits = oldQueryObj.getInt("$splits");
			oldQueryObj.remove("$splits");
		}
		if (oldQueryObj.containsField("$srctags")) {
			srcTags = new BasicDBObject(SourcePojo.tags_, oldQueryObj.get("$srctags"));
			oldQueryObj.remove("$srctags");
		}
		if (bLocalMode) { // If in local mode, then set this to a large number so we always run inside our limit/split version
			// (since for some reason MongoInputFormat seems to fail on large collections)
			nSplits = InfiniteMongoSplitter.MAX_SPLITS; 
		}
		if (oldQueryObj.containsField("$docsPerSplit")) {
			nDocsPerSplit = oldQueryObj.getInt("$docsPerSplit");
			oldQueryObj.remove("$docsPerSplit");
		}
		oldQueryObj.remove("$fields");
		oldQueryObj.remove("$output");
		oldQueryObj.remove("$reducers");
		String mapperKeyClass = oldQueryObj.getString("$mapper_key_class", "");
		String mapperValueClass = oldQueryObj.getString("$mapper_value_class", "");
		oldQueryObj.remove("$mapper_key_class");
		oldQueryObj.remove("$mapper_value_class");
		String cacheList = null;
		Object cacheObj = oldQueryObj.get("$caches");
		if (null != cacheObj) {
			cacheList = cacheObj.toString(); // (either array of strings, or single string)
			if (!cacheList.startsWith("[")) {
				cacheList = "[" + cacheList + "]"; // ("must" now be valid array)
			}
			oldQueryObj.remove("$caches");
		}//TESTED
		
		if (null != nDebugLimit) { // (debug mode override)
			nLimit = nDebugLimit;
		}
		boolean tmpIncMode = ( null != incrementalMode) && incrementalMode; 
		
		Date fromOverride = null;
		Date toOverride = null;
		Object fromOverrideObj = oldQueryObj.remove("$tmin");
		Object toOverrideObj = oldQueryObj.remove("$tmax");
		if (null != fromOverrideObj) {
			fromOverride = dateStringFromObject(fromOverrideObj, true);
		}
		if (null != toOverrideObj) {
			toOverride = dateStringFromObject(toOverrideObj, false);
		}
		
		if ( !isCustomTable )
		{
			if (elasticsearchQuery) {
				oldQueryObj.put("communityIds", communityIds);
				//tmin/tmax not supported - already have that capability as part of the query
			}
			else {
				if (input.equals("feature.temporal")) {
					if ((null != fromOverride) || (null != toOverride)) {
						oldQueryObj.put("value.maxTime", createDateRange(fromOverride, toOverride, true));
					}//TESTED
					oldQueryObj.put("_id.c", new BasicDBObject(DbManager.in_, communityIds));
				}
				else {
					oldQueryObj.put(DocumentPojo.communityId_, new BasicDBObject(DbManager.in_, communityIds));
					if ((null != fromOverride) || (null != toOverride)) {
						oldQueryObj.put("_id", createDateRange(fromOverride, toOverride, false));
					}//TESTED			
					if (input.equals("doc_metadata.metadata")) {
						oldQueryObj.put(DocumentPojo.index_, new BasicDBObject(DbManager.ne_, "?DEL?")); // (ensures not soft-deleted)
					}
				}
			}
		}
		else
		{
			if ((null != fromOverride) || (null != toOverride)) {
				oldQueryObj.put("_id", createDateRange(fromOverride, toOverride, false));
			}//TESTED
			//get the custom table (and database)
			input = CustomOutputManager.getCustomDbAndCollection(input);
		}		
		query = oldQueryObj.toString();
		
		/**/
		System.out.println("QUERY=== " + query);
		
		if ( arguments == null )
			arguments = "";
		
		// Generic configuration
		out.write("<?xml version=\"1.0\"?>\n<configuration>");
		
		// Mongo specific configuration
		
		out.write(
				"\n\t<property><!-- name of job shown in jobtracker --><name>mongo.job.name</name><value>"+title+"</value></property>"+
				"\n\t<property><!-- run the job verbosely ? --><name>mongo.job.verbose</name><value>true</value></property>"+
				"\n\t<property><!-- Run the job in the foreground and wait for response, or background it? --><name>mongo.job.background</name><value>false</value></property>"+
				"\n\t<property><!-- If you are reading from mongo, the URI --><name>mongo.input.uri</name><value>mongodb://"+dbserver+"/"+input+"</value></property>"+  
				"\n\t<property><!-- If you are writing to mongo, the URI --><name>mongo.output.uri</name><value>mongodb://"+dbserver+"/"+output+"</value>  </property>"+  
				"\n\t<property><!-- The query, in JSON, to execute [OPTIONAL] --><name>mongo.input.query</name><value>" + StringEscapeUtils.escapeXml(query) + "</value></property>"+
				"\n\t<property><!-- The fields, in JSON, to read [OPTIONAL] --><name>mongo.input.fields</name><value>"+( (fields==null) ? ("") : fields )+"</value></property>"+
				"\n\t<property><!-- A JSON sort specification for read [OPTIONAL] --><name>mongo.input.sort</name><value></value></property>"+
				"\n\t<property><!-- The number of documents to limit to for read [OPTIONAL] --><name>mongo.input.limit</name><value>" + nLimit + "</value><!-- 0 == no limit --></property>"+
				"\n\t<property><!-- The number of documents to skip in read [OPTIONAL] --><!-- TODO - Are we running limit() or skip() first? --><name>mongo.input.skip</name><value>0</value> <!-- 0 == no skip --></property>"+
				"\n\t<property><!-- Class for the mapper --><name>mongo.job.mapper</name><value>"+ mapper+"</value></property>"+
				"\n\t<property><!-- Reducer class --><name>mongo.job.reducer</name><value>"+reducer+"</value></property>"+
				"\n\t<property><!-- InputFormat Class --><name>mongo.job.input.format</name><value>com.ikanow.infinit.e.data_model.custom.InfiniteMongoInputFormat</value></property>"+
				"\n\t<property><!-- OutputFormat Class --><name>mongo.job.output.format</name><value>com.ikanow.infinit.e.data_model.custom.InfiniteMongoOutputFormat</value></property>"+
				"\n\t<property><!-- Output key class for the output format --><name>mongo.job.output.key</name><value>"+outputKey+"</value></property>"+
				"\n\t<property><!-- Output value class for the output format --><name>mongo.job.output.value</name><value>"+outputValue+"</value></property>"+
				"\n\t<property><!-- Output key class for the mapper [optional] --><name>mongo.job.mapper.output.key</name><value>"+mapperKeyClass+"</value></property>"+
				"\n\t<property><!-- Output value class for the mapper [optional] --><name>mongo.job.mapper.output.value</name><value>"+mapperValueClass+"</value></property>"+
				"\n\t<property><!-- Class for the combiner [optional] --><name>mongo.job.combiner</name><value>"+combiner+"</value></property>"+
				"\n\t<property><!-- Partitioner class [optional] --><name>mongo.job.partitioner</name><value></value></property>"+
				"\n\t<property><!-- Sort Comparator class [optional] --><name>mongo.job.sort_comparator</name><value></value></property>"+
				"\n\t<property><!-- Split Size [optional] --><name>mongo.input.split_size</name><value>32</value></property>"
				);		
		
		// Infinit.e specific configuration
		
		out.write(
				"\n\t<property><!-- User Arguments [optional] --><name>infinit.e.userid</name><value>"+ StringEscapeUtils.escapeXml(userId.toString())+"</value></property>"+
				"\n\t<property><!-- User Arguments [optional] --><name>arguments</name><value>"+ StringEscapeUtils.escapeXml(arguments)+"</value></property>"+
				"\n\t<property><!-- Maximum number of splits [optional] --><name>max.splits</name><value>"+nSplits+"</value></property>"+
				"\n\t<property><!-- Maximum number of docs per split [optional] --><name>max.docs.per.split</name><value>"+nDocsPerSplit+"</value></property>"+
				"\n\t<property><!-- Infinit.e incremental mode [optional] --><name>update.incremental</name><value>"+tmpIncMode+"</value></property>"
			);		
		if (null != cacheList) {
			out.write(
					"\n\t<property><!-- Infinit.e cache list [optional] --><name>infinit.e.cache.list</name><value>"+cacheList+"</value></property>"
				);						
		}//TESTED
		if (null != srcTags) {
			out.write(
					"\n\t<property><!-- Infinit.e src tags filter [optional] --><name>infinit.e.source.tags.filter</name><value>"+srcTags.toString()+"</value></property>"
				);			
		}
		
		// Closing thoughts:
		out.write("\n</configuration>");
		
		out.flush();
		out.close();
	}	
	////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////
	
	// REALLY LOW LEVEL UTILS
	
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

	private static Date dateStringFromObject(Object o, boolean minNotMax) {
		if (null == o) {
			return null;
		}
		else if (o instanceof Long) {
			return new Date((Long)o);
		}
		else if (o instanceof Integer) {
			return new Date((long)(int)(Integer)o);
		}
		else if (o instanceof Date) {
			return (Date)o;
		}
		else if (o instanceof DBObject) {
			o = ((DBObject) o).get("$date");
		}
		if (o instanceof String) {
			AdvancedQueryPojo.QueryTermPojo.TimeTermPojo time = new AdvancedQueryPojo.QueryTermPojo.TimeTermPojo();
			if (minNotMax) {
				time.min = (String) o;
				Interval i = QueryHandler.parseMinMaxDates(time, 0L, new Date().getTime());
				return i.getStart().toDate();
			}
			else {
				time.max = (String) o;
				Interval i = QueryHandler.parseMinMaxDates(time, 0L, new Date().getTime());
				return i.getEnd().toDate();				
			}
		}
		else {
			return null;
		}
	}//TESTED: relative string. string, $date obj, number - parse failure + success
	
	private static BasicDBObject createDateRange(Date min, Date max, boolean timeNotOid) {
		BasicDBObject toFrom = new BasicDBObject();
		if (null != min) {
			if (timeNotOid) {
				toFrom.put(DbManager.gte_, min.getTime());
			}
			else {
				toFrom.put(DbManager.gte_, new ObjectId(min));
				
			}
		}
		if (null != max) {
			if (timeNotOid) {
				toFrom.put(DbManager.lte_, max.getTime());
			}
			else {
				toFrom.put(DbManager.lte_, new ObjectId(max));				
			}
		}
		return toFrom;
	}//TESTED (min/max, _id/time)
	
	////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////
	
	// LOGGING INTERFACE
	
	private String currLocalJobId = null;
	private String currSanityCheck = null;
	private String currThreadId = null;
	private String currJobName = null;
	private StringBuffer currLocalJobErrs = new StringBuffer();
	
	@Override
	public void close() {
	}

	@Override
	public boolean requiresLayout() {
		return false;
	}

	@Override
	protected void append(LoggingEvent arg0) {
		
		// Get current thread id (need to check these even if we have them so we don't log them multiple times)
		if (arg0.getLoggerName().equals("com.ikanow.infinit.e.data_model.custom.InfiniteFileInputReader")) {
			if ((null != currJobName) && arg0.getRenderedMessage().startsWith(currJobName + ":")) {
				if (null == currThreadId) { // this is one of the first message that is printed out so get the thread...
					currThreadId = arg0.getThreadName();
					currSanityCheck = "Task:attempt_" + currLocalJobId.substring(4) + "_";
				}
				return; // (don't log this)
			}
		}//TESTED
		else if (arg0.getLoggerName().equals("com.ikanow.infinit.e.data_model.custom.InfiniteMongoRecordReader")) {
			if ((null != currJobName) && arg0.getRenderedMessage().startsWith(currJobName + ":")) {
				if (null == currThreadId) { // this is one of the first message that is printed out so get the thread...
					currThreadId = arg0.getThreadName();
					currSanityCheck = "Task:attempt_" + currLocalJobId.substring(4) + "_";
				}
				return; // (don't log this)
			}
		}//TESTED

		if ((null != currThreadId) && arg0.getLoggerName().equals("org.apache.hadoop.mapred.Task")) {
			if (arg0.getRenderedMessage().startsWith(currSanityCheck)) {
				// This is to check we didn't accidentally get someone else's messages
				if (!currThreadId.equals(arg0.getThreadName())) {
					_logger.error("Drop all logging: thread mismatch for " + currLocalJobId);
					currLocalJobErrs.setLength(0);
					currThreadId = "ZXCVB";
				}
			}
		}//TESTED
		else if (arg0.getLoggerName().equals("org.apache.hadoop.mapred.LocalJobRunner")) {
			if (arg0.getMessage().toString().equals(currLocalJobId)) {
				
				String[] exceptionInfo = arg0.getThrowableStrRep();
				if (null != exceptionInfo) {
					currLocalJobErrs.append("Uncaught Exception in local job.\n");
					for (String errLine: exceptionInfo) {
						if (errLine.startsWith("	at org.apache.hadoop")) {
							break;
						}
						currLocalJobErrs.append(errLine).append("\n");
					}
				}
			}
		}//TESTED (uncaught exception)
		else if (!arg0.getLoggerName().startsWith("org.apache.hadoop") && !arg0.getLoggerName().startsWith("com.mongodb.hadoop.")) {
			if (arg0.getThreadName().equals(currThreadId)) {
				
				if ((arg0.getLevel() == Level.ERROR)|| bTestMode) {
					currLocalJobErrs.append('[').append(arg0.getLevel()).append("] ").
										append(arg0.getLoggerName()).append(":").append(arg0.getLocationInformation().getLineNumber()).append(" ").
										append(arg0.getMessage()).append("\n");
					String[] exceptionInfo = arg0.getThrowableStrRep();
					if (null != exceptionInfo) {
						for (String errLine: exceptionInfo) {
							if (errLine.startsWith("	at org.apache.hadoop")) {
								break;
							}
							currLocalJobErrs.append(errLine).append("\n");
						}
					}//(end if exception information present)
				}//(end if error or in test mode)
				
			}//(end if this is my thread)
		}//TESTED
	}

}
