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

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobStatus;
import org.apache.log4j.Logger;
import org.bson.types.ObjectId;

import com.ikanow.infinit.e.data_model.api.ApiManager;
import com.ikanow.infinit.e.data_model.api.ResponsePojo;
import com.ikanow.infinit.e.data_model.store.BaseDbPojo;
import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.MongoDbManager;
import com.ikanow.infinit.e.data_model.store.custom.mapreduce.CustomMapReduceJobPojo;
import com.ikanow.infinit.e.processing.custom.launcher.CustomHadoopTaskLauncher;
import com.ikanow.infinit.e.processing.custom.launcher.CustomSavedQueryTaskLauncher;
import com.ikanow.infinit.e.processing.custom.output.CustomOutputManager;
import com.ikanow.infinit.e.processing.custom.scheduler.CustomScheduleManager;
import com.ikanow.infinit.e.processing.custom.status.CustomStatusManager;
import com.ikanow.infinit.e.processing.custom.utils.AuthUtils;
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
		this(null, null);
	}
	public CustomProcessingController(Boolean bLocalMode, Integer debugLimit) {
		prop_custom = new com.ikanow.infinit.e.processing.custom.utils.PropertiesManager();
		_statusManager = new CustomStatusManager(prop_custom);
		_nDebugLimit = debugLimit;
		
		_bLocalMode = prop_custom.getHadoopLocalMode();
		if (null != bLocalMode) {
			_bLocalMode = bLocalMode;
		}
		try {
			@SuppressWarnings("unused")
			JobClient jc = new JobClient(InfiniteHadoopUtils.getJobClientConnection(prop_custom), new Configuration());
			if (_bLocalMode) {
				System.out.println("Will run hadoop locally (infrastructure appears to exist).");				
			}
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
			CustomOutputManager.shardOutputCollection(job);

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

				if (prop_custom.getHarvestSecurity()) {
					if (!AuthUtils.isAdmin(job.submitterID)) {
						throw new RuntimeException("Permissions error: in secure mode, only admins can launch Hadoop");
					}
				}//TODO (INF-2118): TOTEST
				
				List<ObjectId> communityIds = InfiniteHadoopUtils.getUserCommunities(job.submitterID);
				job.tempJarLocation = InfiniteHadoopUtils.downloadJarFile(job.jarURL, communityIds, prop_custom);		
								
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
					_statusManager.updateJobPojo(job._id, jobS, jobN, job.tempConfigXMLLocation, job.tempJarLocation);
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
			
			CustomMapReduceJobPojo cmr = jobOverride;
			if (null == cmr)
				cmr = CustomScheduleManager.getJobsToMakeComplete(_bHadoopEnabled);
			else if (null == cmr.jobidS)
				return true;
			
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
							jc = new JobClient(InfiniteHadoopUtils.getJobClientConnection(prop_custom), new Configuration());
						}
						catch (Exception e) 
						{ 
							// Better delete this, no idea what's going on....						
							_logger.info("job_update_status_error_title=" + cmr.jobtitle + " job_update_status_error_id=" + cmr._id.toString() + " job_update_status_error_message=Skipping job: " + cmr.jobidS + cmr.jobidN + ", this node does not run mapreduce");							
							_statusManager.setJobComplete(cmr,true,true, -1,-1, "Failed to launch job, unknown error (check configuration in  /opt/hadoop-infinite/mapreduce/hadoop/, jobtracker may be localhost?).");							
							cmr = CustomScheduleManager.getJobsToMakeComplete(_bHadoopEnabled);
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
				//job was not done, need to set flag back
				if ( !markedComplete )
				{
					incompleteJobsMap.put(cmr._id, cmr.jobidS);
				}
				if (null == jobOverride)
					cmr = CustomScheduleManager.getJobsToMakeComplete(_bHadoopEnabled);
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
}
