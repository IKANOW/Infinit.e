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
package com.ikanow.infinit.e.processing.custom.status;

import java.util.Date;

import org.apache.log4j.Logger;
import org.bson.types.ObjectId;

import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.MongoDbManager;
import com.ikanow.infinit.e.data_model.store.custom.mapreduce.CustomMapReduceJobPojo;
import com.ikanow.infinit.e.processing.custom.output.CustomOutputManager;
import com.ikanow.infinit.e.processing.custom.scheduler.CustomScheduleManager;
import com.ikanow.infinit.e.processing.custom.utils.InfiniteHadoopUtils;
import com.ikanow.infinit.e.processing.custom.utils.PropertiesManager;
import com.mongodb.BasicDBObject;

public class CustomStatusManager {

	private static Logger _logger = Logger.getLogger(CustomStatusManager.class);
	private PropertiesManager prop_custom = null;
	
	public CustomStatusManager(PropertiesManager prop_custom_) {
		prop_custom = prop_custom_;
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
	public void setJobComplete(CustomMapReduceJobPojo cmr, boolean isComplete, boolean isError, float mapProgress, float reduceProgress, String errorMessage) 
	{		
		// (Note, inc_ and unset_ are added in one place each, so can't use them without ensuring you combine existing uses)  
		
		BasicDBObject updates = new BasicDBObject();
		BasicDBObject update = new BasicDBObject();
		try
		{			
			long nNew = 0;
			long nTotal = 0;
			if ( isComplete )
			{		
				long runtime = new Date().getTime() - cmr.lastRunTime.getTime();
				long timeFromSchedule = cmr.lastRunTime.getTime() - cmr.nextRunTime;
				
				updates.append(CustomMapReduceJobPojo.jobidS_, null);
				updates.append(CustomMapReduceJobPojo.jobidN_,0);
				try 
				{
					long nextRunTime = CustomScheduleManager.getNextRunTime(cmr.scheduleFreq, cmr.firstSchedule, cmr.nextRunTime, cmr.timesRan+1);
					//if next run time reschedules to run before now, keep rescheduling until its later
					//the server could have been turned off for days and would try to rerun all jobs once a day
					while ( nextRunTime < new Date().getTime() )
					{
						Date firstSchedule = new Date(nextRunTime);
						cmr.firstSchedule = firstSchedule;
						updates.append(CustomMapReduceJobPojo.firstSchedule_, firstSchedule);
						nextRunTime = CustomScheduleManager.getNextRunTime(cmr.scheduleFreq, cmr.firstSchedule, cmr.nextRunTime, cmr.timesRan+1);
					}
					updates.append(CustomMapReduceJobPojo.nextRunTime_,nextRunTime);
				}
				catch (Exception e) {} // just carry on, we'll live...
				
				updates.append(CustomMapReduceJobPojo.lastCompletionTime_, new Date());
				updates.append(CustomMapReduceJobPojo.tempConfigXMLLocation_,null);
				updates.append(CustomMapReduceJobPojo.tempJarLocation_,null);
				try 
				{
					InfiniteHadoopUtils.removeTempFile(cmr.tempConfigXMLLocation);
					InfiniteHadoopUtils.removeTempFile(cmr.tempJarLocation);					
				}
				catch (Exception e) 
				{
					_logger.info("job_error_removing_tempfiles=" + InfiniteHadoopUtils.createExceptionMessage(e));
				} 
				
				BasicDBObject incs = new BasicDBObject(CustomMapReduceJobPojo.timesRan_, 1);				
				//copy depencies to waitingOn
				updates.append(CustomMapReduceJobPojo.waitingOn_, cmr.jobDependencies);
				if ( !isError )
				{
					// Counts and move and output
					nNew = DbManager.getCollection(cmr.getOutputDatabase(), cmr.outputCollectionTemp).count();					
					
					//TODO (INF-1159): this shouldn't really be here but it makes life much easier for now (really should be part of the m/r OutputFormat...) 
					CustomOutputManager.completeOutput(cmr, prop_custom);
					
					//if job was successfully, mark off dependencies
					removeJobFromChildren(cmr._id);
					
					// More counts:
					nTotal = DbManager.getCollection(cmr.getOutputDatabase(), cmr.outputCollection).count();										

					// Status:
					String completionStatus = "Schedule Delta: " + timeFromSchedule + "ms\nCompletion Time: " + runtime + "ms\nNew Records: " + nNew + "\nTotal Records: " + nTotal;
					if (null == errorMessage) { // (I think will always be the case?)
						errorMessage = completionStatus;
					}
					else {
						errorMessage += "\n" + completionStatus;
					}					
					if ((null != cmr.tempErrors) && !cmr.tempErrors.isEmpty()) { // Individual errors reported from map/combine/reduce
						StringBuffer sb = new StringBuffer(errorMessage).append("\n\nLog Messages:\n\n");
						for (String err: cmr.tempErrors) {
							sb.append(err).append("\n");
						}
						errorMessage = sb.toString();
						update.put(MongoDbManager.unset_, new BasicDBObject(CustomMapReduceJobPojo.tempErrors_, 1));
					}
					updates.append(CustomMapReduceJobPojo.errorMessage_, errorMessage); // (will often be null)					
				}
				else
				{
					if ((null != cmr.tempErrors) && !cmr.tempErrors.isEmpty()) { // Individual errors reported from map/combine/reduce
						StringBuffer sb = new StringBuffer(errorMessage).append("\n\nLog Messages:\n\n");
						for (String err: cmr.tempErrors) {
							sb.append(err).append("\n");
						}
						errorMessage = sb.toString();
						update.put(MongoDbManager.unset_, new BasicDBObject(CustomMapReduceJobPojo.tempErrors_, 1));
					}
					//failed, just append error message										
					updates.append(CustomMapReduceJobPojo.errorMessage_, errorMessage);
					incs.append(CustomMapReduceJobPojo.timesFailed_,1);					
				}
				update.append(MongoDbManager.inc_, incs);
				
				if (null != cmr.jobidS) 
				{
					_logger.info("job_completion_title=" + cmr.jobtitle + " job_completion_id="+cmr._id.toString() + " job_completion_time=" + runtime + " job_schedule_delta=" + timeFromSchedule + " job_completion_success=" + !isError + " job_hadoop_id=" + cmr.jobidS + "_" + cmr.jobidN + " job_new_records=" + nNew + " job_total_records=" + nTotal);
				}
				else 
				{
					_logger.info("job_completion_title=" + cmr.jobtitle + " job_completion_id="+cmr._id.toString() + " job_completion_time=" + runtime + " job_schedule_delta=" + timeFromSchedule + " job_completion_success=" + !isError + " job_new_records=" + nNew + " job_total_records=" + nTotal);					
				}
			}
			updates.append(CustomMapReduceJobPojo.mapProgress_, mapProgress);
			updates.append(CustomMapReduceJobPojo.reduceProgress_, reduceProgress);			
		}
		catch (Exception ex)
		{
			//ex.printStackTrace();
			_logger.info("job_error_updating_status_title=" + cmr.jobtitle + " job_error_updating_status_id=" + cmr._id.toString() + " job_error_updating_status_message=" + InfiniteHadoopUtils.createExceptionMessage(ex));
		}		
		finally { // It's really bad if this doesn't happen, so do it here so that it always gets called
			if (!updates.isEmpty()) {
				update.append(MongoDbManager.set_,updates);
					// (if isComplete, should always include resetting jobidS and jobidN)
				DbManager.getCustom().getLookup().update(new BasicDBObject(CustomMapReduceJobPojo._id_,cmr._id),update);
			}
		}
	}
	/**
	 * Updates the status of the current, active, job
	 */
	public void updateJobPojo(ObjectId _id, String jobids, int jobidn, String xmlLocation, String jarLocation)
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
	 * Checks if any dependent jobs are running or are about to, resets this job to 1 min
	 * in the future if any are.  (This prevents a user from manually starting job A, 
	 * then job B if job A had completed previously, thus job B will have no dependencies).
	 * 
	 * @param cmr
	 * @return
	 */
	public boolean dependenciesNotStartingSoon(CustomMapReduceJobPojo cmr )
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
			_logger.info("job_error_checking_dependencies=" + InfiniteHadoopUtils.createExceptionMessage(ex) );
		}
		
		return !dependencyRunning;
	}
	
}
