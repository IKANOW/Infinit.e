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
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLConnection;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobStatus;
import org.bson.types.ObjectId;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.MongoDbManager;
import com.ikanow.infinit.e.data_model.store.custom.mapreduce.CustomMapReduceJobPojo;
import com.ikanow.infinit.e.data_model.store.custom.mapreduce.CustomMapReduceJobPojo.SCHEDULE_FREQUENCY;
import com.ikanow.infinit.e.data_model.store.social.sharing.SharePojo;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.gridfs.GridFSDBFile;


public class HadoopJobRunner
{
	private com.ikanow.infinit.e.processing.custom.utils.PropertiesManager prop_custom = new com.ikanow.infinit.e.processing.custom.utils.PropertiesManager();
	private com.ikanow.infinit.e.data_model.utils.PropertiesManager prop_general = new com.ikanow.infinit.e.data_model.utils.PropertiesManager();
	
	public HadoopJobRunner()
	{
		
	}
	
	public void runScheduledJobs()
	{				
		//check mongo for jobs needing ran
		CustomMapReduceJobPojo job = getJobsToRun();
		while ( job != null )
		{
			//Run each job				
			System.out.println("Running job: " + job.jarURL);
			runJob(job);
			//try to get another available job
			job = getJobsToRun();
		}
	}
	
	private void runJob(CustomMapReduceJobPojo job)
	{
		try
		{
			shardOutputCollection(job.outputCollection);
			//get the jar file
			String tempJarURL = downloadJarFile(job.jarURL, job.communityIds);		
			//add job to hadoop
			String jobid = runHadoopJob(job, tempJarURL);
			//write jobid back to lookup
			String[] jobParts = jobid.split("_");
			String jobS = jobParts[1];
			int jobN = Integer.parseInt( jobParts[2] );	
			updateJobPojo(job._id, jobS, jobN, job.tempConfigXMLLocation, tempJarURL);
		}
		catch(Exception ex)
		{
			System.out.println("Failed to run scheduled job\nException: ");
			ex.printStackTrace();
		}
	}
	
	/**
	 * Attempt to shard the output collection.  If the collection
	 * is already sharded it will just spit back an error which
	 * is fine.
	 * 
	 * @param outputCollection
	 */
	private void shardOutputCollection(String outputCollection) 
	{
		System.out.println("Sharding output collection on _id");
		//enable sharding for the custommr db incase it hasn't been
		DbManager.getDB("admin").command(new BasicDBObject("enablesharding", "custommr"));
		//enable sharding for the output collection
		BasicDBObject command = new BasicDBObject("shardcollection", "custommr." + outputCollection);
		command.append("key", new BasicDBObject("_id", 1));
		DbManager.getDB("admin").command(command);		
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
		System.out.println("Downloading jar: " + jarURL + " and saving at: " + tempFileName);
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
			System.out.println("Getting shareid: " + shareid);
			BasicDBObject query = new BasicDBObject("_id",new ObjectId(shareid));
			query.put("communities._id", new BasicDBObject("$in", communityIds));

			SharePojo share = SharePojo.fromDb(DbManager.getSocial().getShare().findOne(query),SharePojo.class);
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
		System.out.println("Done downloading");
		return tempFileName;
	}

	private void updateJobPojo(ObjectId _id, String jobids, int jobidn, String xmlLocation, String jarLocation)
	{
		System.out.println("Updating job pojo");
		try
		{			
			BasicDBObject set = new BasicDBObject();
			set.append("jobidS", jobids);
			set.append("jobidN", jobidn);
			set.append("tempConfigXMLLocation", xmlLocation);
			set.append("tempJarLocation",jarLocation);
			BasicDBObject updateObject = new BasicDBObject("$set",set);
			DbManager.getCustom().getLookup().update(new BasicDBObject("_id", _id), updateObject);		
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	private String runHadoopJob(CustomMapReduceJobPojo job, String jar)
	{
		String jobid = null;
		job.tempConfigXMLLocation = createConfigXML(job.jobtitle,job.inputCollection,job.outputCollection,job.tempConfigXMLLocation, job.mapper, job.reducer, job.combiner, job.query, job.communityIds, job.isCustomTable, job.outputKey, job.outputValue);
		try
		{				
			System.out.println("Setting up Hadoop job");
			Runtime rt = Runtime.getRuntime();
			String[] commands = new String[]{"hadoop","--config", prop_custom.getHadoopConfigPath() + "/hadoop", "jar", jar, "-conf", job.tempConfigXMLLocation};			
			String command = "";
			for (String s : commands )
				command += s + " ";
			System.out.println("Running command: " + command);
			Process pr = rt.exec(commands);
			
			//Once we start running the command attach to stderr to
			//receive the output to parse out the jobid
			InputStream in = pr.getErrorStream();			
			InputStreamReader is = new InputStreamReader(in);
			BufferedReader br = new BufferedReader(is);
	        String line = null;

	        boolean bGotJobId = false;
	        
	        while (!bGotJobId && (line = br.readLine()) != null) 
	        {
	        	int getJobIdIndex = -1;  
	        	String searchstring = "INFO mapred.JobClient: Running job: ";
	        	if ((getJobIdIndex = line.indexOf(searchstring)) >= 0) 
	        	{
	        		// Get JobId and trim() it (obviously trivial)
	        		jobid = line.substring(getJobIdIndex + searchstring.length()).trim();
	        		bGotJobId = true;
	        	}
	        }       
		}
		catch (Exception ex)
		{
			//had an error running command
			//probably log error to the job so we stop trying to run it
			System.out.println("bombs away");
			ex.printStackTrace();
		}
		return jobid;
	}
	
	/**
	 * Create the xml file that will configure the mongo commands and
	 * write that to the server
	 * 
	 * @param input
	 * @param output
	 */
	private String createConfigXML(String title, String input, String output, String configLocation, String mapper, String reducer, String combiner, String query, List<ObjectId> communityIds, boolean isCustomTable, String outputKey, String outputValue)
	{		
		//outputValue = "org.apache.hadoop.io.DoubleWritable";
		//outputKey = "org.apache.hadoop.io.Text";
		output = "custommr." + output;

		//add communities to query if this is not a custom table
		if ( !isCustomTable )
		{
			if ( query.equals("") )
			{
				query = "{";
			}
			else
			{
				query = query.substring(0, query.lastIndexOf("}")) + ", ";
			}
			String commQuery = "\"communityId\": {\"$in\": [";		
			for ( ObjectId oid : communityIds)
				commQuery += "{ \"$oid\":\""+oid.toString()+"\"}, ";
			commQuery = commQuery.substring(0, commQuery.length()-2);
			commQuery += "]}";
			query += commQuery + "}";
		}
		
		System.out.println("Creating XML config file using\n" +
				"jobname: " + title + "\n" +
				"mapper: " + mapper + "\n" +
				"reducer: " + reducer + "\n" +
				"combiner: " + combiner + "\n" +
				"query: " + query + "\n" +
				"outputKey: " + outputKey + "\n" +
				"outputValue: " + outputValue);
		
		try
		{
			if ( configLocation == null )
				configLocation = assignNewConfigLocation();
			File configFile = new File(configLocation);
			FileWriter fstream = new FileWriter(configFile);
			BufferedWriter out = new BufferedWriter(fstream);
			String dbserver = prop_general.getDatabaseServer();
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
					"\n\t<property><!-- InputFormat Class --><name>mongo.job.input.format</name><value>com.mongodb.hadoop.MongoInputFormat</value></property>"+
					"\n\t<property><!-- OutputFormat Class --><name>mongo.job.output.format</name><value>com.mongodb.hadoop.MongoOutputFormat</value></property>"+
					"\n\t<property><!-- Output key class for the output format --><name>mongo.job.output.key</name><value>"+outputKey+"</value></property>"+
					"\n\t<property><!-- Output value class for the output format --><name>mongo.job.output.value</name><value>"+outputValue+"</value></property>"+
					"\n\t<property><!-- Output key class for the mapper [optional] --><name>mongo.job.mapper.output.key</name><value></value></property>"+
					"\n\t<property><!-- Output value class for the mapper [optional] --><name>mongo.job.mapper.output.value</name><value></value></property>"+
					"\n\t<property><!-- Class for the combiner [optional] --><name>mongo.job.combiner</name><value>"+combiner+"</value></property>"+
					"\n\t<property><!-- Partitioner class [optional] --><name>mongo.job.partitioner</name><value></value></property>"+
					"\n\t<property><!-- Sort Comparator class [optional] --><name>mongo.job.sort_comparator</name><value></value></property>"+
					"\n\t<property><!-- Split Size [optional] --><name>mongo.input.split_size</name><value>32</value></property>"+
					"\n</configuration>");
			out.flush();
			out.close();
			fstream.close();
		}
		catch (Exception ex)
		{
			//error writing xml file
			System.out.println("Error creating temporarily xml file");
		}		
		return configLocation;
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
			BasicDBObject query = new BasicDBObject();
			query.append("jobidS", null);
			query.append("nextRunTime", new BasicDBObject("$lt", new Date().getTime()));
			System.out.println("Finding any jobs that need ran after: " + new Date().getTime());
			BasicDBObject updates = new BasicDBObject("jobidS", "");
			updates.append("lastRunTime", new Date());
			BasicDBObject update = new BasicDBObject("$set", updates);
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
		System.out.println("Finding any hadoop jobs and checking status");
		//get mongo entries that have jobids?
		try
		{
			JobClient jc = null;			
			DBCursor dbc = DbManager.getCustom().getLookup().find(new BasicDBObject("jobidS", new BasicDBObject("$ne", null)));
			while (dbc.hasNext())
			{
				CustomMapReduceJobPojo cmr = CustomMapReduceJobPojo.fromDb(dbc.next(), CustomMapReduceJobPojo.class);
				//make sure its an actual ID, we now set jobidS to "" when running the job
				if ( !cmr.jobidS.equals("") )
				{
					if (null == jc) {
						jc = new JobClient(getJobClientConnection(), new Configuration());						
					}					
					System.out.println("Checking if job: " + cmr.jobidS + cmr.jobidN + " is complete");
					//check if job is done, and update if it is	
					JobStatus[] jobs = jc.getAllJobs();
					for ( JobStatus j : jobs )
					{
						if ( j.getJobID().getJtIdentifier().equals(cmr.jobidS) && j.getJobID().getId() == cmr.jobidN )
						{
							if ( j.isJobComplete() )
								setJobComplete(cmr);
						}
					}
				}
				else
				{
					//check if its been longer than 5min and mark job as complete (it failed to launch)
					Date currDate = new Date();
					Date lastDate = cmr.lastRunTime;
					//if its been more than 5 min (5m*60s*1000ms)					
					if ( currDate.getTime() - lastDate.getTime() > 300000 )
					{
						setJobComplete(cmr);
					}
				}
			}	
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}	
		System.out.println("Done updating job status");
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
	private void setJobComplete(CustomMapReduceJobPojo cmr) 
	{		
		try
		{
			BasicDBObject updates = new BasicDBObject();
			updates.append("jobidS", null);
			updates.append("jobidN",0);
			long nextRunTime = getNextRunTime(cmr.scheduleFreq,cmr.firstSchedule, cmr.timesRan+1);
			updates.append("nextRunTime",nextRunTime);
			updates.append("lastCompletionTime", new Date());
			updates.append("tempConfigXMLLocation",null);
			updates.append("tempJarLocation",null);
			if (null != cmr.tempConfigXMLLocation) {
				removeTempFile(cmr.tempConfigXMLLocation);
			}
			if (null != cmr.tempJarLocation) {
				removeTempFile(cmr.tempJarLocation);
			}
			BasicDBObject update = new BasicDBObject();
			update.append(MongoDbManager.set_,updates);
			update.append(MongoDbManager.inc_,new BasicDBObject("timesRan", 1));
			DbManager.getCustom().getLookup().update(new BasicDBObject("_id",cmr._id),update);			
			System.out.println("Setting job : " + cmr._id.toString() + " to complete, run again on: " + new Date(nextRunTime).toString());
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}		
	}
	
	/**
	 * Removes the config file that is not being used anymore.
	 * 
	 * @param file
	 */
	private void removeTempFile(String file)
	{
		File f = new File(file);
		if (null != f) {
			f.delete();
		}
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
	private long getNextRunTime(SCHEDULE_FREQUENCY scheduleFreq, Date firstSchedule, int iterations) 
	{
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
}
