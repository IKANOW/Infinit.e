/*******************************************************************************
 * Copyright 2012 The Infinit.e Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.ikanow.infinit.e.processing.custom.utils;

import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.regex.Pattern;

import org.bson.types.ObjectId;

import com.ikanow.infinit.e.api.utils.SocialUtils;
import com.ikanow.infinit.e.data_model.api.BasePojoApiMap;
import com.ikanow.infinit.e.data_model.api.ResponsePojo;
import com.ikanow.infinit.e.data_model.api.ResponsePojo.ResponseObject;
import com.ikanow.infinit.e.data_model.api.custom.mapreduce.CustomMapReduceResultPojo;
import com.ikanow.infinit.e.data_model.store.CsvGeneratingBsonDecoder;
import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.SizeReportingBasicBSONDecoder;
import com.ikanow.infinit.e.data_model.store.custom.mapreduce.CustomMapReduceJobPojo;
import com.ikanow.infinit.e.data_model.store.social.person.PersonCommunityPojo;
import com.ikanow.infinit.e.data_model.store.social.person.PersonPojo;
import com.ikanow.infinit.e.processing.custom.CustomProcessingController;
import com.ikanow.infinit.e.processing.custom.scheduler.CustomScheduleManager;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBDecoderFactory;

public class CustomApiUtils {

	public final static long DONT_RUN_TIME = 4070908800000L; // 01-01-2099 in Java time, used in the GUI to mean don't run. 	
	
	// UTILITY FUNCTION FOR SCHEDULE/UPDATE JOB
	
	public static void runJobAndWaitForCompletion(CustomMapReduceJobPojo job, boolean bQuickRun, boolean bTestMode, Integer debugLimit) {
		com.ikanow.infinit.e.processing.custom.utils.PropertiesManager customProps = new com.ikanow.infinit.e.processing.custom.utils.PropertiesManager();
		boolean bLocalMode = customProps.getHadoopLocalMode();
		if (!bLocalMode || bQuickRun || (null != debugLimit)) {			
			// (if local mode is running then initializing job is bad because it will wait until the job is complete
			//  ... except if quickRun is set then this is what we want anyway!)
			
			// Check there are available timeslots:
			if (CustomScheduleManager.availableSlots(customProps)) {
				job.lastRunTime = new Date();
				job.jobidS = "";
				if (!bTestMode) DbManager.getCustom().getLookup().save(job.toDb());							
				
				// Run the job
				CustomProcessingController pxController = null;
				if (null != debugLimit) {
					pxController = new CustomProcessingController(debugLimit);					
				}
				else {
					pxController = new CustomProcessingController();
				}
				pxController.initializeJob(job); // (sets job.jobid*)
				
				// In quick run mode, keep checking until the job is done (5s intervals)
				if (bQuickRun) {
					int nRuns = 0;
					while (!pxController.checkRunningJobs(job)) {
						try { Thread.sleep(5000); } catch (Exception e) {}
						if (++nRuns > 120) { // bail out after 10 minutes 
							break;
						}
					}
				}
			}
			else { // (no available timeslots - just save as is and let the px engine start it)
				if (!bTestMode) DbManager.getCustom().getLookup().save(job.toDb());											
			}
		}		
		else { // still need to save the job
			if (!bTestMode) DbManager.getCustom().getLookup().save(job.toDb());														
		}
		
	}//TESTED: (local mode on/off, quick mode on/off) //TESTED (local/quick, local/!quick)
	
	// (UTILITY FOR GETTING RESULTS)
	
	public static void getJobResults(ResponsePojo rp, CustomMapReduceJobPojo cmr, int limit, String fields, String findStr, String sortStr) {
		CustomApiUtils.getJobResults(rp, cmr, limit, fields, findStr, sortStr, false);
	}
	public static void getJobResults(ResponsePojo rp, CustomMapReduceJobPojo cmr, int limit, String fields, String findStr, String sortStr, boolean bCsv) {
		
		BasicDBObject queryDbo = null;
		if (null != findStr) {
			queryDbo = (BasicDBObject) com.mongodb.util.JSON.parse(findStr);
		}
		else {
			queryDbo = new BasicDBObject();	
		}//TOTEST
		
		BasicDBObject fieldsDbo = new BasicDBObject();
		if (null != fields) {
			fieldsDbo = (BasicDBObject) com.mongodb.util.JSON.parse("{" + fields + "}");
		}

		//return the results:
		
		// Need to handle sorting...
		BasicDBObject sort = null;
		if (null != sortStr) { //override
			sort = (BasicDBObject) com.mongodb.util.JSON.parse(sortStr);
		}
		else { //defaults
			String sortField = "_id";
			int sortDir = 1;
			BasicDBObject postProcObject = (BasicDBObject) com.mongodb.util.JSON.parse(InfiniteHadoopUtils.getQueryOrProcessing(cmr.query, InfiniteHadoopUtils.QuerySpec.POSTPROC));
			if ( postProcObject != null )
			{
				sortField = postProcObject.getString("sortField", "_id");
				sortDir = postProcObject.getInt("sortDirection", 1);
			}//TESTED (post proc and no post proc)
			sort = new BasicDBObject(sortField, sortDir);
		}//TOTEST
								
		// Case 1: DB
		rp.setResponse(new ResponseObject("Custom Map Reduce Job Results",true,"Map reduce job completed at: " + cmr.lastCompletionTime));
		if ((null == cmr.exportToHdfs) || !cmr.exportToHdfs) {
			DBCursor resultCursor = null;
			DBCollection coll = DbManager.getCollection(cmr.getOutputDatabase(), cmr.outputCollection);
			DBDecoderFactory defaultDecoder = coll.getDBDecoderFactory();
			CsvGeneratingBsonDecoder csvDecoder = null;
			SizeReportingBasicBSONDecoder sizeDecoder = null;
			CustomMapReduceResultPojo cmrr = new CustomMapReduceResultPojo();
			try {
				if (bCsv) {
					coll.setDBDecoderFactory((csvDecoder = new CsvGeneratingBsonDecoder()));
				}
				else {
					coll.setDBDecoderFactory((sizeDecoder = new SizeReportingBasicBSONDecoder()));					
				}
				if (limit > 0) {
					resultCursor = coll.find(queryDbo, fieldsDbo).sort(sort).limit(limit);
				}
				else {
					resultCursor = coll.find(queryDbo, fieldsDbo).sort(sort);
				}
				LinkedList<BasicDBObject> list = null;
				if (!bCsv) {
					list = new LinkedList<BasicDBObject>();
				}
				final int MAX_SIZE_CSV = 80*1024*1024; //(80MB)
				final int MAX_SIZE_JSON = 80*1024*1024; //(80MB)
				while (resultCursor.hasNext()) {
					BasicDBObject x = (BasicDBObject)resultCursor.next();
					if (!bCsv) {
						list.add(x);
					}
					if (null != csvDecoder) {
						if (csvDecoder.getCsv().length() > MAX_SIZE_CSV) {
							break;
						}
					}
					else if (null != sizeDecoder) {
						if (sizeDecoder.getSize() > MAX_SIZE_JSON) {
							break;
						}						
					}
				}
				cmrr.results = list;				
			}
			finally {
				coll.setDBDecoderFactory(defaultDecoder);
			}
			cmrr.lastCompletionTime = cmr.lastCompletionTime;
			if (null != csvDecoder) {
				StringBuffer header = new StringBuffer();
				for (String field: csvDecoder.getOrderedFields()) {
					if (0 != header.length()) { 
						header.append(',');
					}
					header.append('"');
					header.append(field.replace("\"", "\\\""));
					header.append("\"");
				}
				header.append('\n');
				header.append(csvDecoder.getCsv().toString());
				cmrr.results = header.toString();
			}
			rp.setData(cmrr);
		}//TESTED
		else { // Case 2: HDFS
			
			if ((null != cmr.outputKey) && (null != cmr.outputValue) && 
				cmr.outputKey.equalsIgnoreCase("org.apache.hadoop.io.text") && cmr.outputValue.equalsIgnoreCase("org.apache.hadoop.io.text"))
			{
				// special case, text file
				try {
					rp.setData(HadoopUtils.getBsonFromTextFiles(cmr, limit, fields), (BasePojoApiMap<BasicDBList>) null);
				}
				catch (Exception e) {
					rp.setResponse(new ResponseObject("Custom Map Reduce Job Results",false,"Files don't appear to be in text file format, did you run the job before changing the output to Text/Text?"));
				}
			}//TESTED
			else { // sequence file
				try {
					rp.setData(HadoopUtils.getBsonFromSequenceFile(cmr, limit, fields), (BasePojoApiMap<BasicDBList>) null);
				}
				catch (Exception e) {
					rp.setResponse(new ResponseObject("Custom Map Reduce Job Results",false,"Files don't appear to be in sequence file format, did you run the job with Text/Text?"));
				}
			}//TESTED
		}//TESTED		
	}
	
	// UTIL: Get the output database, based on the size of the collection
	
	public static String getJobDatabase(CustomMapReduceJobPojo cmr) {
		long nJobs = DbManager.getCustom().getLookup().count();
		long nDbNum = nJobs / 3000; // (3000 jobs per collection, max is 6000)
		if (nDbNum > 0) { // else defaults to custommr
			String dbName = cmr.getOutputDatabase() + Long.toString(nDbNum);
			return dbName;
		}		
		else {
			return cmr.getOutputDatabase();
		}
	}
	
	////////////////////////////////////////////////////////////////////
	
	// DUPLICATION OF CODE FROM API (SocialUtils):
	
	static public String[] getCommunityIds(String userIdStr, String communityIdStrList) {
		
		if (communityIdStrList.charAt(0) < 0x30) {
			Pattern communityRegex = null;
			if (communityIdStrList.length() > 1) {
				communityRegex = Pattern.compile(communityIdStrList.substring(1), Pattern.CASE_INSENSITIVE);
			}
			HashSet<ObjectId> allCommunities = getUserCommunities(userIdStr, communityRegex);
			String[] communityIds = new String[allCommunities.size()];
			int i = 0; 
			for (ObjectId oid: allCommunities) {
				communityIds[i] = oid.toString();
				++i;
			}
			return communityIds;
		}
		else {
			 String[] communityIdStrs = communityIdStrList.split("\\s*,\\s*");
			 return communityIdStrs;
		}
	} //TESTED
	
	static private HashSet<ObjectId> getUserCommunities(String userIdStr, Pattern regex) {
		PersonPojo person = SocialUtils.getPerson(userIdStr);
		HashSet<ObjectId> memberOf = new HashSet<ObjectId>();
		if (null != person) {
			if (null != person.getCommunities()) {
				for (PersonCommunityPojo community: person.getCommunities()) {
					if ((null == regex) || regex.matcher(community.getName()).find()) {
						memberOf.add(community.get_id());
					}
				}
			}
		}
		return memberOf;
	}//TESTED

	public static PersonPojo getPerson(String id)
	{
		PersonPojo person = null;		
		try
		{
			// Set up the query
			PersonPojo personQuery = new PersonPojo();
			personQuery.set_id(new ObjectId(id));
			
			BasicDBObject dbo = (BasicDBObject) DbManager.getSocial().getPerson().findOne(personQuery.toDb());
			person = PersonPojo.fromDb(dbo, PersonPojo.class);
		} 
		catch (Exception e)
		{
		}
		return person;
	}

}
