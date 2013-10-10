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
package com.ikanow.infinit.e.processing.custom.scheduler;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.MongoDbManager;
import com.ikanow.infinit.e.data_model.store.custom.mapreduce.CustomMapReduceJobPojo;
import com.ikanow.infinit.e.data_model.store.custom.mapreduce.CustomMapReduceJobPojo.SCHEDULE_FREQUENCY;
import com.ikanow.infinit.e.processing.custom.utils.PropertiesManager;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

public class CustomScheduleManager {

	/**
	 * Check there are available slots for running
	 */	
	public static boolean availableSlots(PropertiesManager prop_custom)
	{
		int nMaxConcurrent = prop_custom.getHadoopMaxConcurrent();
		if (Integer.MAX_VALUE != nMaxConcurrent) {
			BasicDBObject maxQuery = new BasicDBObject(CustomMapReduceJobPojo.jobidS_, new BasicDBObject(DbManager.ne_, null));
			int nCurrRunningJobs = (int) DbManager.getCustom().getLookup().count(maxQuery);
			if (nCurrRunningJobs >= nMaxConcurrent) {
				return false;
			}
		}
		return true;
	}//TESTED
	
	/**
	 * Look for jobs that have not started yet but are scheduled for some point in the future
	 */
	public static CustomMapReduceJobPojo getJobsToRun(PropertiesManager prop_custom, boolean bLocalMode, boolean bHadoopEnabled)
	{
		try
		{
			// First off, check the number of running jobs - don't exceed the max
			// (seem to run into memory problems if this isn't limited?)
			if (!availableSlots(prop_custom)) {
				return null;
			}
			
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
	
	/**
	 * Look for running jobs, decide if they are complete
	 */
	public static CustomMapReduceJobPojo getJobsToMakeComplete(boolean bHadoopEnabled)
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
	 * Uses a map reduce jobs schedule frequency to determine when the next
	 * map reduce job should be ran.
	 * 
	 * @param scheduleFreq
	 * @param firstSchedule
	 * @param iterations
	 * @return
	 */
	public static long getNextRunTime(SCHEDULE_FREQUENCY scheduleFreq, Date firstSchedule, long nextRuntime, int iterations) 
	{
		if ((null == firstSchedule) || (0 == firstSchedule.getTime())) {
			firstSchedule = new Date(nextRuntime);
			iterations = 1; // recover...
		}//TESTED
		
		if ( scheduleFreq == null || SCHEDULE_FREQUENCY.NONE == scheduleFreq)
		{
			return Long.MAX_VALUE;
		}
		Calendar cal = new GregorianCalendar();
		cal.setTime(firstSchedule);
		
		if ( SCHEDULE_FREQUENCY.HOURLY == scheduleFreq)
		{
			cal.add(Calendar.HOUR, 1*iterations);
		}
		else if ( SCHEDULE_FREQUENCY.DAILY == scheduleFreq)
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
