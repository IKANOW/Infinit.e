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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLConnection;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobStatus;
import org.bson.types.ObjectId;

import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.custom.mapreduce.CustomMapReduceJobPojo;
import com.ikanow.infinit.e.data_model.store.custom.mapreduce.CustomMapReduceJobPojo.SCHEDULE_FREQUENCY;
import com.ikanow.infinit.e.data_model.store.social.sharing.SharePojo;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
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
		List<CustomMapReduceJobPojo> jobs = getJobsToRun();
		//Run each job		
		for ( CustomMapReduceJobPojo job : jobs )
		{
			System.out.println("Running job: " + job.jarURL);
			runJob(job);
		}
	}
	
	private void runJob(CustomMapReduceJobPojo job)
	{
		try
		{
			//get the jar file
			String tempJarURL = downloadJarFile(job.jarURL);		
			//add job to hadoop
			runHadoopJob(job, tempJarURL);
			//get job id and write to admin object?
			JobStatus jobid = getJobID();
			updateJobPojo(job._id, jobid.getJobID().getJtIdentifier(),jobid.getJobID().getId(), job.tempConfigXMLLocation, tempJarURL);
		}
		catch(Exception ex)
		{
			System.out.println("Failed to run scheduled job\nException: ");
			ex.printStackTrace();
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
	private String downloadJarFile(String jarURL) throws Exception
	{		
		String shareString = "$infinite/share/get/";
		String tempFileName = assignNewJarLocation();
		System.out.println("Downloading jar: " + jarURL + " and saving at: " + tempFileName);
		OutputStream out = new BufferedOutputStream(new FileOutputStream(tempFileName));
		if ( jarURL.startsWith(shareString))
		{
			//jar is local use id to grab jar (skips authentication)
			String shareid = jarURL.substring(shareString.length());
			System.out.println("Getting shareid: " + shareid);
			SharePojo share = SharePojo.fromDb(DbManager.getSocial().getShare().findOne(new BasicDBObject("_id",new ObjectId(shareid))),SharePojo.class);
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
	
	private JobStatus getJobID()
	{
		System.out.println("Getting jobid");
		try
		{
			JobClient jc = new JobClient(new InetSocketAddress(prop_custom.getJobClientServer(), prop_custom.getJobClientPort()), new Configuration());			
			JobStatus[] jobs = jc.getAllJobs();
			JobStatus lastJob = jobs[0];
			for ( JobStatus j : jobs )
			{
				if ( j.getStartTime() > lastJob.getStartTime() )
					lastJob = j;								
			}
			System.out.println("Last job started at: " + new Date(lastJob.getStartTime()));
			return lastJob;// jc.getAllJobs()[jc.getAllJobs().length-1];
		}
		catch (Exception ex)
		{			
			ex.printStackTrace();
		}
		return null;
	}
	
	private void runHadoopJob(CustomMapReduceJobPojo job, String jar)
	{
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
			pr.waitFor(); //wait for it to exec so we can get the jobid
		}
		catch (Exception ex)
		{
			//had an error running command
			//probably log error to the job so we stop trying to run it
			System.out.println("bombs away");
			ex.printStackTrace();
		}
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
				commQuery += "ObjectId(\""+oid.toString()+"\"), ";
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
					"\n\t<property><!-- Run the job in the foreground and wait for response, or background it? --><name>mongo.job.background</name><value>true</value></property>"+
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
		String dirname = prop_custom.getHadoopConfigPath() + "/config/xmlFiles/";
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
	 * 
	 * @return a list of jobs that need ran
	 */
	private List<CustomMapReduceJobPojo> getJobsToRun()
	{
		List<CustomMapReduceJobPojo> jobs = new ArrayList<CustomMapReduceJobPojo>();
		try
		{
			BasicDBObject query = new BasicDBObject();
			query.append("jobidS", null);
			query.append("nextRunTime", new BasicDBObject("$lt", new Date().getTime()));
			System.out.println("Finding any jobs that need ran after: " + new Date().getTime());
			DBCursor dbc = DbManager.getCustom().getLookup().find(query);
			if ( dbc != null )
			{				
				jobs = CustomMapReduceJobPojo.listFromDb(dbc, CustomMapReduceJobPojo.listType());
			}
		}
		catch(Exception ex)
		{
			//oh noes!
			ex.printStackTrace();
		}
		
		return jobs;
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
			JobClient jc = new JobClient(new InetSocketAddress(prop_custom.getJobClientServer(), prop_custom.getJobClientPort()), new Configuration());			
			DBCursor dbc = DbManager.getCustom().getLookup().find(new BasicDBObject("jobidS", new BasicDBObject("$ne", null)));
			while (dbc.hasNext())
			{
				CustomMapReduceJobPojo cmr = CustomMapReduceJobPojo.fromDb(dbc.next(), CustomMapReduceJobPojo.class);
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
			removeTempFile(cmr.tempConfigXMLLocation);
			removeTempFile(cmr.tempJarLocation);
			BasicDBObject update = new BasicDBObject();
			update.append("$set",updates);
			update.append("$inc",new BasicDBObject("timesRan", 1));
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
		f.delete();
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
}
