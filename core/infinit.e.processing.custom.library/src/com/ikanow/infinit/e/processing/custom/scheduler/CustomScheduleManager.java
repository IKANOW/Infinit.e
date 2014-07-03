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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Set;

import org.bson.types.ObjectId;

import com.ikanow.infinit.e.data_model.api.knowledge.AdvancedQueryPojo;
import com.ikanow.infinit.e.data_model.control.DocumentQueueControlPojo;
import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.MongoDbManager;
import com.ikanow.infinit.e.data_model.store.custom.mapreduce.CustomMapReduceJobPojo;
import com.ikanow.infinit.e.data_model.store.custom.mapreduce.CustomMapReduceJobPojo.SCHEDULE_FREQUENCY;
import com.ikanow.infinit.e.data_model.store.social.sharing.SharePojo;
import com.ikanow.infinit.e.data_model.store.social.sharing.SharePojo.ShareCommunityPojo;
import com.ikanow.infinit.e.data_model.utils.MongoApplicationLock;
import com.ikanow.infinit.e.processing.custom.utils.AuthUtils;
import com.ikanow.infinit.e.processing.custom.utils.PropertiesManager;
import com.mongodb.BasicDBObject;
import com.mongodb.CommandResult;
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
	
	////////////////////////////////////////////////////////////////////
	
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

	////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////
	
	// Saved Query Handling

	// Somewhat confusingly there are now 2 different types of saved query:
	// The original one, handled by CustomSavedQueryTaskLauncher
	// - this "re-uses" the CustomMapReducePojo object
	// - stores the entire query result in one object, including aggregations (can easily >16MB and break)
	// - you can store snapshots in append mode
	// - never really found a use
	// The new one, handled via DocumentQueueControlPojos embedded in shares
	// - writes the _ids into the shares, the existing API query is required to get them out
	
	private static MongoApplicationLock _appLock = null;
	
	public static void doneWithSavedQueryCache()
	{
		_appLock.release();
	}
	
	public static void createOrUpdatedSavedQueryCache()
	{
		if (null == _appLock) { 
			_appLock = MongoApplicationLock.getLock(DbManager.getCustom().getSavedQueryCache().getDB().getName());
		}
		// the built-in applock acquisition code requires more persistent threads so we use the alternate mechanism that
		// just wipes out anything that hasn't been used in the last 2 minutes and then uses the existing contention handling to
		// allow one thread to grab it
		_appLock.clearStaleLocksOnTime(120);
		if (_appLock.acquire(100)) {
			BasicDBObject query = new BasicDBObject(SharePojo.type_, DocumentQueueControlPojo.SavedQueryQueue);
			List<SharePojo> savedQueries = SharePojo.listFromDb(DbManager.getSocial().getShare().find(query), SharePojo.listType());
			if (null != savedQueries) {
				for (SharePojo savedQueryShare: savedQueries) {
					if (null != savedQueryShare.getShare()) {
						DocumentQueueControlPojo savedQuery = DocumentQueueControlPojo.fromApi(savedQueryShare.getShare(), DocumentQueueControlPojo.class);
						
						// Is this query well formed?
						if ((null != savedQuery.getQueryInfo()) && 
								((null != savedQuery.getQueryInfo().getQuery()) || (null != savedQuery.getQueryInfo().getQueryId())))
						{
							Date now = new Date();
							long freqOffset;
							// Check if it's time to run the query
							if (savedQuery.getQueryInfo().getFrequency() == DocumentQueueControlPojo.SavedQueryInfo.DocQueueFrequency.Hourly)
							{
								freqOffset = 3600L*1000L;
								//(nothing to do here, just run whenever)
							}//TESTED (test3)
							else if (savedQuery.getQueryInfo().getFrequency() == DocumentQueueControlPojo.SavedQueryInfo.DocQueueFrequency.Daily)
							{
								if (null != savedQuery.getQueryInfo().getFrequencyOffset()) { // hour of day
									freqOffset = 12L*3600L*1000L; // (already check vs hour of day so be more relaxed) 
									Calendar calendar = GregorianCalendar.getInstance();
									calendar.setTime(now);

									//DEBUG
									//System.out.println("DAILY: " + calendar.get(Calendar.HOUR_OF_DAY) + " VS " + savedQuery.getQueryInfo().getFrequencyOffset());	
									
									if (calendar.get(Calendar.HOUR_OF_DAY) != savedQuery.getQueryInfo().getFrequencyOffset()) {
										continue;
									}//TESTED (test4)
								}
								else {
									freqOffset = 24L*3600L*1000L; // (just run every 24 hours) 									
								}
							}//TESTED (test4)
							else if (savedQuery.getQueryInfo().getFrequency() == DocumentQueueControlPojo.SavedQueryInfo.DocQueueFrequency.Weekly)
							{
								if (null != savedQuery.getQueryInfo().getFrequencyOffset()) { // day of week
									freqOffset = 3L*24L*3600L*1000L; // (already check vs day of week so be more relaxed) 
									Calendar calendar = GregorianCalendar.getInstance();
									calendar.setTime(now);

									//DEBUG
									//System.out.println("WEEKLY: " + calendar.get(Calendar.DAY_OF_WEEK) + " VS " + savedQuery.getQueryInfo().getFrequencyOffset());
									
									if (calendar.get(Calendar.DAY_OF_WEEK) != savedQuery.getQueryInfo().getFrequencyOffset()) {
										continue;
									}
								}
								else {
									freqOffset = 7L*24L*3600L*1000L;	//(just run every 7 days)
								}
							}//TESTED (test5)
							else continue; // (no -supported- frequency, don't run)
							
							long nowTime = now.getTime();

							/**/
							//DEBUG
							System.out.println("Comparing: " + savedQuery.getQueryInfo().getLastRun() + " VS " + now + " @ " + freqOffset/1000L);
							
							if ((null == savedQuery.getQueryInfo().getLastRun()) ||
									((nowTime - savedQuery.getQueryInfo().getLastRun().getTime()) > freqOffset))
							{
								//(does nothing if the share already exists)
								DbManager.getCustom().getSavedQueryCache().insert(savedQueryShare.toDb());
								CommandResult cr = DbManager.getCustom().getSavedQueryCache().getDB().getLastError();
								
								if (null == cr.get("err")) { // if we've actually done something, update the main share table also
									savedQuery.getQueryInfo().setLastRun(now);
									savedQueryShare.setShare(savedQuery.toApi());
									// (this will overwrite the existing version)
									DbManager.getSocial().getShare().save(savedQueryShare.toDb());								
								}//TESTED (by hand with prints)
								
							}//TESTED (test3-5)
							
						} // (end saved query actually has a query)
					}
				}//(end loop over saved queries)
			}
		}//(end acquired app lock)
	}//TESTED

	public static DocumentQueueControlPojo getSavedQueryToRun()
	{
		DocumentQueueControlPojo toReturn = null;
		try {
			SharePojo savedQueryShare = SharePojo.fromDb(DbManager.getCustom().getSavedQueryCache().findAndRemove(new BasicDBObject()), SharePojo.class);
			if (null == savedQueryShare) { // nothing to process
				return null;
			}//TESTED (test1)
			toReturn = DocumentQueueControlPojo.fromApi(savedQueryShare.getShare(), DocumentQueueControlPojo.class);
			
			// Get the user communities and append the query if possible
			Set<ObjectId> userAccess = AuthUtils.getCommunities(savedQueryShare.getOwner().get_id());
			if (userAccess.isEmpty()) {
				return toReturn; // (_parentShare is null so will be discarded)
			}
			if (null != toReturn.getQueryInfo().getQueryId()) {
				BasicDBObject queryQuery = new BasicDBObject(SharePojo._id_, toReturn.getQueryInfo().getQueryId());
				queryQuery.put(ShareCommunityPojo.shareQuery_id_, new BasicDBObject(DbManager.in_, userAccess));
				SharePojo shareContainingQuery = SharePojo.fromDb(DbManager.getSocial().getShare().findOne(queryQuery), SharePojo.class);

				if (null == shareContainingQuery) {
					return toReturn; // (_parentShare is null so will be discarded)
				}
				toReturn.getQueryInfo().setQuery(AdvancedQueryPojo.fromApi(shareContainingQuery.getShare(), AdvancedQueryPojo.class));
			}//TESTED (test6)
			
			if (null != toReturn.getQueryInfo().getQuery()) {
				// Check the communityIds...
				if (null != toReturn.getQueryInfo().getQuery().communityIds) {
					ArrayList<ObjectId> revisedCommunityList = new ArrayList<ObjectId>(toReturn.getQueryInfo().getQuery().communityIds.size());
					for (ObjectId commId: toReturn.getQueryInfo().getQuery().communityIds) {
						if (userAccess.contains(commId)) {
							revisedCommunityList.add(commId);
						}
					}//(end loop over unchecked communities)
					toReturn.getQueryInfo().getQuery().communityIds = revisedCommunityList;
				}
			}//TESTED (test1)
			toReturn._parentShare = savedQueryShare; // (if this is null then the subsequent processing will ignore this)
		}
		catch (Exception e) { // (this is some internal horror so log)
			e.printStackTrace();
		}
		return toReturn;
	}//TESTED (test1,test6)
	
}
