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
package com.ikanow.infinit.e.processing.custom;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobID;
import org.apache.hadoop.mapred.JobStatus;
import org.apache.hadoop.mapred.RunningJob;
import org.apache.log4j.Logger;
import org.bson.types.ObjectId;

import com.ikanow.infinit.e.data_model.InfiniteEnums.HarvestEnum;
import com.ikanow.infinit.e.data_model.api.ApiManager;
import com.ikanow.infinit.e.data_model.api.ResponsePojo;
import com.ikanow.infinit.e.data_model.control.DocumentQueueControlPojo;
import com.ikanow.infinit.e.data_model.store.BaseDbPojo;
import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.MongoDbManager;
import com.ikanow.infinit.e.data_model.store.config.source.SourceHarvestStatusPojo;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.ikanow.infinit.e.data_model.store.custom.mapreduce.CustomMapReduceJobPojo;
import com.ikanow.infinit.e.processing.custom.launcher.CustomHadoopTaskLauncher;
import com.ikanow.infinit.e.processing.custom.launcher.CustomSavedQueryQueueLauncher;
import com.ikanow.infinit.e.processing.custom.launcher.CustomSavedQueryTaskLauncher;
import com.ikanow.infinit.e.processing.custom.output.CustomOutputManager;
import com.ikanow.infinit.e.processing.custom.scheduler.CustomScheduleManager;
import com.ikanow.infinit.e.processing.custom.status.CustomStatusManager;
import com.ikanow.infinit.e.processing.custom.utils.InfiniteHadoopUtils;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;

public class CustomProcessingController {

	private static Logger _logger = Logger.getLogger(CustomProcessingController.class);
	private com.ikanow.infinit.e.processing.custom.utils.PropertiesManager prop_custom = null;
	
	private CustomStatusManager _statusManager;
	private boolean _bLocalMode = false;
	private boolean _bHadoopEnabled = true; 
	private Integer _nDebugLimit = null;
	
	public CustomProcessingController() {
		this(null);
	}
	public CustomProcessingController(Integer debugLimit) {
		prop_custom = new com.ikanow.infinit.e.processing.custom.utils.PropertiesManager();
		_statusManager = new CustomStatusManager(prop_custom);
		_nDebugLimit = debugLimit;
		
		_bLocalMode = prop_custom.getHadoopLocalMode();
		try {
			Configuration config = new Configuration();
			String hadoopConfigPath = prop_custom.getHadoopConfigPath() + "/hadoop/";
			config.addResource(new Path(hadoopConfigPath + "core-site.xml"));
			config.addResource(new Path(hadoopConfigPath + "mapred-site.xml"));
			config.addResource(new Path(hadoopConfigPath + "hadoop-site.xml"));
			if (new File(hadoopConfigPath + "yarn-site.xml").exists()) {
				config.addResource(new Path(hadoopConfigPath + "yarn-site.xml"));
				config.set("fs.hdfs.impl", "org.apache.hadoop.hdfs.DistributedFileSystem");									
				config.set("fs.file.impl", "org.apache.hadoop.fs.LocalFileSystem");									
				config.set("fs.AbstractFileSystem.hdfs.impl", "org.apache.hadoop.fs.Hdfs"); // (workaround for HDP issue)
			}
			@SuppressWarnings("unused")
			JobClient jc = new JobClient(InfiniteHadoopUtils.getJobClientConnection(config), config);
			if (_bLocalMode) {
				System.out.println("Will run hadoop locally (infrastructure appears to exist).");				
			}
		}
		catch (Error e) {
			if (_bLocalMode) {
				System.out.println("Will run hadoop locally (no infrastructure).");				
			}
			else {
				System.out.println("No hadoop infrastructure installed, will just look for saved queries.");
			}
			_bHadoopEnabled = false;			
		}
		catch (Exception e) { // Hadoop doesn't work
			if (_bLocalMode) {
				System.out.println("Will run hadoop locally (no infrastructure).");				
			}
			else {
				System.out.println("No hadoop infrastructure installed, will just look for saved queries.");
			}
			_bHadoopEnabled = false;
		}	
	}
	
	//
	// Initialize a specific job
	//
	
	public void initializeJob(CustomMapReduceJobPojo job) {
		long time_start_setup = new Date().getTime();
		long time_setup = 0;
		try
		{
			CustomOutputManager.prepareOutputCollection(job);

			// This may be a saved query, if so handle that separately
			if (null == job.jarURL) {
				ResponsePojo rp = new CustomSavedQueryTaskLauncher().runSavedQuery(job);
				if (!rp.getResponse().isSuccess()) {
					_statusManager.setJobComplete(job, true, true, -1, -1, rp.getResponse().getMessage());
				}
				else { // Success, write to output
					try {		
						// Write to the temp output collection:
					
						String outCollection = job.outputCollectionTemp;
						if ((job.appendResults != true) && job.appendResults)
							outCollection = job.outputCollection;
						//TESTED
						
						DBCollection dbTemp =  DbManager.getCollection(job.getOutputDatabase(), outCollection);
						BasicDBObject outObj = new BasicDBObject();
						outObj.put("key", new Date()); 
						outObj.put("value", com.mongodb.util.JSON.parse(BaseDbPojo.getDefaultBuilder().create().toJson(rp)));
						dbTemp.save(outObj);
						
						_statusManager.setJobComplete(job, true, false, 1, 1, ApiManager.mapToApi(rp.getStats(), null));					
						job.jobidS = null;
					}
					catch (Exception e) { // Any sort of error, just make sure we set the job to complete			
						_statusManager.setJobComplete(job, true, true, 1, 1, e.getMessage());
						job.jobidS = null;
					}
				}
			}
			else {

				List<ObjectId> communityIds = InfiniteHadoopUtils.getUserCommunities(job.submitterID);
				job.tempJarLocation = InfiniteHadoopUtils.downloadJarFile(job.jarURL, communityIds, prop_custom, job.submitterID);		
								
				// Programmatic code:
				String jobid = new CustomHadoopTaskLauncher(_bLocalMode, _nDebugLimit, prop_custom).runHadoopJob(job, job.tempJarLocation);
				//OLD "COMMAND LINE: CODE
				//add job to hadoop
				//String jobid = new CustomHadoopTaskLauncher().runHadoopJob_commandLine(job, job.tempJarLocation);
				
				if ( jobid.startsWith("local_done")) { // (run locally)
					String statusMessage = null;
					if (jobid.length() > 12) {
						statusMessage = jobid.substring(12);
					}					
					_statusManager.setJobComplete(job, true, false, -1, -1, statusMessage);				
					job.jobidS = null;
				}
				else if ( jobid != null && !jobid.startsWith("Error") )
				{
					time_setup = new Date().getTime() - time_start_setup;
					_logger.info("job_setup_title=" + job.jobtitle + " job_setup_id=" + job._id.toString() + " job_setup_time=" + time_setup + " job_setup_success=true job_hadoop_id=" + jobid);
					//write jobid back to lookup
					String[] jobParts = jobid.split("_");
					String jobS = jobParts[1];
					int jobN = Integer.parseInt( jobParts[2] );	
					job.jobidS = jobS;
					job.jobidN = jobN;
					_statusManager.updateJobPojo(job._id, jobS, jobN, job.tempConfigXMLLocation, job.tempJarLocation, job);
				}
				else
				{
					time_setup = new Date().getTime() - time_start_setup;
					_logger.info("job_setup_title=" + job.jobtitle + " job_setup_id=" + job._id.toString() + " job_setup_time=" + time_setup + " job_setup_success=false  job_setup_message=" + jobid);
					//job failed, send off the error message
					_statusManager.setJobComplete(job, true, true, -1, -1, jobid);
					job.jobidS = null;
				}
			}
		}
		catch(Exception ex)
		{			
			//job failed, send off the error message
			time_setup = new Date().getTime() - time_start_setup;
			_logger.info("job_setup_title=" + job.jobtitle + " job_setup_id=" + job._id.toString() + " job_setup_time=" + time_setup + " job_setup_success=false job_setup_message=" + InfiniteHadoopUtils.createExceptionMessage(ex) );
			_statusManager.setJobComplete(job, true, true, -1, -1, ex.getMessage());
			job.jobidS = null;
		}
	}
	
	//
	// Look at scheduled but inactive jobs, decide which ones to start
	//
		
	public void checkScheduledJobs() {
		checkScheduledJobs(null);
	}
	public void checkScheduledJobs(String jobOverride) {
		//check mongo for jobs needing ran
		
		CustomMapReduceJobPojo job = null;
		if (null != jobOverride) {
			job = CustomMapReduceJobPojo.fromDb(
					MongoDbManager.getCustom().getLookup().findOne(new BasicDBObject(CustomMapReduceJobPojo.jobtitle_, jobOverride)),
					CustomMapReduceJobPojo.class);
			
			if (null != job) {
				job.lastRunTime = new Date();
				job.nextRunTime = job.lastRunTime.getTime();
				if (!_bLocalMode) { 
					// Need to store the times or they just get lost between here and the job completion check  
					MongoDbManager.getCustom().getLookup().save(job.toDb());
						// (not that efficient, but this is essentially a DB call so whatever)
				}
				initializeJob(job);
			}
		}
		else {
			job = CustomScheduleManager.getJobsToRun(prop_custom, _bLocalMode, _bHadoopEnabled);
			while ( job != null )
			{
				if ( _statusManager.dependenciesNotStartingSoon(job) )
				{
					//Run each job				
					initializeJob(job);
				}
				//try to get another available job
				job = CustomScheduleManager.getJobsToRun(prop_custom, _bLocalMode, _bHadoopEnabled);
			}			
		}
	}
	
	public boolean killRunningJob(CustomMapReduceJobPojo jobToKillInfo) {		
		try  {
			// Do this regardless of anyrthing else:
			if (null != jobToKillInfo.derivedFromSourceKey) { // Update the derived source, if one existse 
				BasicDBObject query = new BasicDBObject(SourcePojo.key_, jobToKillInfo.derivedFromSourceKey);
				BasicDBObject setUpdate = new BasicDBObject(SourceHarvestStatusPojo.sourceQuery_harvest_status_, HarvestEnum.error.toString());
				setUpdate.put(SourceHarvestStatusPojo.sourceQuery_harvest_message_, "Manually stopped");
				BasicDBObject srcUpdate = new BasicDBObject(DbManager.set_, setUpdate);
				DbManager.getIngest().getSource().update(query, srcUpdate, false, false);					
			}//TESTED (actually a bit pointless usually because is then overwritten by the source publish)
			
			Configuration conf = new Configuration();
			String hadoopConfigPath = prop_custom.getHadoopConfigPath() + "/hadoop/";
			conf.addResource(new Path(hadoopConfigPath + "core-site.xml"));
			conf.addResource(new Path(hadoopConfigPath + "mapred-site.xml"));
			conf.addResource(new Path(hadoopConfigPath + "hadoop-site.xml"));
			if (new File(hadoopConfigPath + "yarn-site.xml").exists()) {
				conf.addResource(new Path(hadoopConfigPath + "yarn-site.xml"));
				conf.set("fs.AbstractFileSystem.hdfs.impl", "org.apache.hadoop.fs.Hdfs"); // (workaround for HDP issue)
			}
			JobClient jc = new JobClient(InfiniteHadoopUtils.getJobClientConnection(conf), conf);
			jc.setConf(conf); // (doesn't seem to be set by the above call)

			RunningJob jobToKill = jc.getJob(new JobID(jobToKillInfo.jobidS, jobToKillInfo.jobidN));
			if (null == jobToKill) {
				_logger.error("Couldn't find this job: " + jobToKillInfo.jobidS + "_" + jobToKillInfo.jobidN +  " / " + new JobID(jobToKillInfo.jobidS, jobToKillInfo.jobidN).toString());
				
				// Update the custom pojo though:
				_statusManager.setJobComplete(jobToKillInfo, true, true, (float)0.0, (float)0.0, "Manually stopped");
				
				return false;
			}
			jobToKill.killJob();
			
			int nRuns = 0;
			while (!checkRunningJobs(jobToKillInfo)) {
				try { Thread.sleep(5000); } catch (Exception e) {}
				if (++nRuns > 24) { // bail out after 2 minutes 
					_logger.error("Killed job: " + jobToKillInfo.jobidS + "_" + jobToKillInfo.jobidN +  ", but job failed to stop within time allowed");
					return false;
				}
			}		
			return true;
		}
		catch (Exception e) {
			_logger.error("Failed to kill job: " + jobToKillInfo.jobidS + "_" + jobToKillInfo.jobidN +  " / " + e.getMessage(), e);
			return false;
		} 
	}//TESTED (by hand)
	
	//
	// Look at active jobs, decide which ones to finish
	// returns true if jobOverride is true if specified, if there are no running jobs otherwuse
	//
		
	public void checkRunningJobs() {		
		checkRunningJobs(null); 
	}
	public boolean checkRunningJobs(CustomMapReduceJobPojo jobOverride) {
		Map<ObjectId, String> incompleteJobsMap = new HashMap<ObjectId, String>();
		//get mongo entries that have jobids?
		try
		{
			JobClient jc = null;
			
			// Build a configuration:
			Configuration config = new Configuration();
			String hadoopConfigPath = prop_custom.getHadoopConfigPath() + "/hadoop/";
			config.addResource(new Path(hadoopConfigPath + "core-site.xml"));
			config.addResource(new Path(hadoopConfigPath + "mapred-site.xml"));
			config.addResource(new Path(hadoopConfigPath + "hadoop-site.xml"));
			if (new File(hadoopConfigPath + "yarn-site.xml").exists()) {
				config.addResource(new Path(hadoopConfigPath + "yarn-site.xml"));
				config.set("fs.AbstractFileSystem.hdfs.impl", "org.apache.hadoop.fs.Hdfs"); // (workaround for HDP issue)
			}
			
			CustomMapReduceJobPojo cmr = jobOverride;
			if (null == cmr)
				cmr = CustomScheduleManager.getJobsToMakeComplete(_bHadoopEnabled, incompleteJobsMap);
			else if (null == cmr.jobidS)
				return true;
			else 
				incompleteJobsMap.put(cmr._id, cmr.jobtitle);
			
			while (cmr != null)
			{		
				boolean markedComplete = false;
				//make sure its an actual ID, we now set jobidS to "" when running the job
				if ( !cmr.jobidS.equals("") ) // non null by construction
				{
					if (null == jc) 
					{
						try 
						{							
							jc = new JobClient(InfiniteHadoopUtils.getJobClientConnection(config), config);
						}
						catch (Exception e) 
						{ 
							// Better delete this, no idea what's going on....						
							_logger.info("job_update_status_error_title=" + cmr.jobtitle + " job_update_status_error_id=" + cmr._id.toString() + " job_update_status_error_message=Skipping job: " + cmr.jobidS + cmr.jobidN + ", this node does not run mapreduce");							
							_statusManager.setJobComplete(cmr,true,true, -1,-1, "Failed to launch job, unknown error (check configuration in  /opt/hadoop-infinite/mapreduce/hadoop/, jobtracker may be localhost?).");
							incompleteJobsMap.remove(cmr._id);
							cmr = CustomScheduleManager.getJobsToMakeComplete(_bHadoopEnabled, incompleteJobsMap);
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
								errorMessage = "Job failed while running, check for errors in the mapper/reducer or that your key/value classes are set up correctly? " + j.getFailureInfo();
							}
							_statusManager.setJobComplete(cmr, markedComplete, error, j.mapProgress(),j.reduceProgress(), errorMessage);
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
							_statusManager.setJobComplete(cmr,true,true, -1,-1, "Failed to launch job, unknown error #2.");
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
						_statusManager.setJobComplete(cmr,true,true, -1,-1, "Failed to launch job, unknown error #1.");
					}
				}
				
				//job was done, remove flag
				if ( markedComplete )
				{
					incompleteJobsMap.remove(cmr._id);
				}
				if (null == jobOverride)
					cmr = CustomScheduleManager.getJobsToMakeComplete(_bHadoopEnabled, incompleteJobsMap);
				else
					cmr = null;
			}	
		}
		catch (Exception ex)
		{
			_logger.info("job_error_checking_status_message=" + InfiniteHadoopUtils.createExceptionMessage(ex) );			
		}	
		catch (Error err) {
			// Really really want to get to the next line of code, and clear the status...
			_logger.info("job_error_checking_status_message=" + InfiniteHadoopUtils.createExceptionMessage(err) );			
		}
				
		if (null == jobOverride)
		{
			//set all incomplete jobs' status back
			for (ObjectId id : incompleteJobsMap.keySet())
			{		
				BasicDBObject update = new BasicDBObject(CustomMapReduceJobPojo.jobidS_, incompleteJobsMap.get(id));
				DbManager.getCustom().getLookup().update(new BasicDBObject(CustomMapReduceJobPojo._id_, id), 
															new BasicDBObject(MongoDbManager.set_, update));
			}					
		}
		return incompleteJobsMap.isEmpty();
	}
	public void runThroughSavedQueues()
	{
		CustomScheduleManager.createOrUpdatedSavedQueryCache();

		// Spend at most 5 minutes per iteration
		long startTime = new Date().getTime();
		long iterationTime = startTime;
		
		DocumentQueueControlPojo savedQuery = CustomScheduleManager.getSavedQueryToRun();
		while (null != savedQuery) {
			CustomSavedQueryQueueLauncher.executeQuery(savedQuery);
			
			long timeTaken = new Date().getTime();

			//DEBUG
			//System.out.println("Query took: " + (timeTaken - iterationTime)/1000L + " total="+(timeTaken - startTime)/1000L);			
						
			if ((timeTaken - startTime) > 5L*60000L) {
				break;
			}
			savedQuery = CustomScheduleManager.getSavedQueryToRun();
			
			if (null != savedQuery) {
				try {
					Thread.sleep(timeTaken - iterationTime); // (at most 50% duty cycle)
				}
				catch (Exception e) {}
				
				iterationTime = new Date().getTime();
			}//TESTED (test2)
		}
	}//TESTED (CustomSavedQueryTestCode:*)
}
