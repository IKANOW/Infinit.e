package com.ikanow.infinit.e.harvest.extraction.document.distributed;

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.bson.types.ObjectId;

import com.google.gson.GsonBuilder;
import com.ikanow.infinit.e.data_model.InfiniteEnums;
import com.ikanow.infinit.e.data_model.InfiniteEnums.HarvestEnum;
import com.ikanow.infinit.e.data_model.api.ApiManager;
import com.ikanow.infinit.e.data_model.api.BasePojoApiMap;
import com.ikanow.infinit.e.data_model.api.ResponsePojo;
import com.ikanow.infinit.e.data_model.api.custom.mapreduce.CustomMapReduceResultPojo;
import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.config.source.SourceHarvestStatusPojo;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePipelinePojo;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePipelinePojo.DocumentPostProcessing.ScheduleType;
import com.ikanow.infinit.e.data_model.store.custom.mapreduce.CustomMapReduceJobPojo;
import com.ikanow.infinit.e.data_model.store.custom.mapreduce.CustomMapReduceJobPojo.SCHEDULE_FREQUENCY;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.infinit.e.data_model.store.social.sharing.SharePojo;
import com.ikanow.infinit.e.data_model.utils.JsonPrettyPrinter;
import com.ikanow.infinit.e.harvest.HarvestContext;
import com.ikanow.infinit.e.harvest.extraction.document.HarvesterInterface;
import com.ikanow.infinit.e.processing.custom.CustomProcessingController;
import com.ikanow.infinit.e.processing.custom.utils.CustomApiUtils;
import com.ikanow.infinit.e.processing.custom.utils.SourcePipelineToCustomConversion;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;

public class DistributedHarvester implements HarvesterInterface {

	@Override
	public boolean canHarvestType(int sourceType) {
		return sourceType == InfiniteEnums.DISTRIBUTED || sourceType == InfiniteEnums.POSTPROC || sourceType == InfiniteEnums.CUSTOM;
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////
	
	@Override
	public void executeHarvest(HarvestContext context, SourcePojo source,
			List<DocumentPojo> toAdd, List<DocumentPojo> toUpdate,
			List<DocumentPojo> toRemove) {
		
		int srcType = InfiniteEnums.castExtractType(source.getExtractType());
		if (context.isStandalone()) { // test mode
			if (InfiniteEnums.POSTPROC == srcType) {
				testPostProcessingModule(context, source, toAdd);
			}
			else if (InfiniteEnums.CUSTOM == srcType) {
				testCustomJobs(context, source, toAdd);
			}
			else {
				throw new RuntimeException("Not yet supported: " + source.getExtractType());
			}
			//TODO (INF-2865): other cases ... (for HDFS, run in debug mode)
		}
		else { // normal mode
			if (InfiniteEnums.POSTPROC == srcType) {
				// 2 cases:
				// - daily schedule => ignore
				// - runs with harvester => 
				Boolean onDailySchedule = null;
				if ((null != source.getProcessingPipeline()) && !source.getProcessingPipeline().isEmpty()) {
					SourcePipelinePojo pxPipe = source.getProcessingPipeline().iterator().next();
					if (null != pxPipe.postProcessing) {
						onDailySchedule = ScheduleType.daily == pxPipe.postProcessing.scheduleMode;
					}
				}//TESTED (both cases)
				if (null == onDailySchedule) {
					throw new RuntimeException("No processing pipeline specified");
				}
				
				if (onDailySchedule) {
					if ((null != source.getHarvestStatus()) && 
							(HarvestEnum.in_progress != source.getHarvestStatus().getHarvest_status()) && 
							(HarvestEnum.success_iteration != source.getHarvestStatus().getHarvest_status()))
					{
						source.setReachedMaxDocs();
					}
					// (else not doing anything)
				}//TESTED (by hand)
				else {
					source.setReachedMaxDocs(); // (ensures goes to success_iteration, ie consistent with other multi-stage sources)
					
					if ((null != source.getHarvestStatus()) && 
							(HarvestEnum.in_progress != source.getHarvestStatus().getHarvest_status()) && 
							(HarvestEnum.success_iteration != source.getHarvestStatus().getHarvest_status()))
					{
						launchCustomJobs(context, source);
					}
					//TODO (INF-2865): else maybe grab the last N messages sorted by _id and log them and dump them into source (does that auto log them?)
				}//TESTED (by hand)
			}
			else if (InfiniteEnums.CUSTOM == srcType) {
				// (Should never reach here - if we do for some reason then just keep the source type in success_iteration)
				source.setReachedMaxDocs();
			}
			else {
				throw new RuntimeException("Not yet supported: " + source.getExtractType());
			}
			//TODO (INF-2865): other cases ... (for HDFS potentially check the timestamps .. and if the dir is empty ... and then bombs away)
		}
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////////////////////

	private void launchCustomJobs(HarvestContext context, SourcePojo src) {
		//TODO (INF-2865): at some point this will need to use the source key to get all related jobs
		
		BasicDBObject query = new BasicDBObject(CustomMapReduceJobPojo.jobtitle_, src.getKey());
		CustomMapReduceJobPojo job = CustomMapReduceJobPojo.fromDb(DbManager.getCustom().getLookup().findOne(query),CustomMapReduceJobPojo.class);
		if (null == job) {
			throw new RuntimeException("Custom job for " + src.getKey() + " not found - try re-publishing?");
		}
		
		job.nextRunTime = new Date().getTime(); // ie run now
		if (null == job.derivedFromSourceKey) { // (should always be set, but re-set if necessary, will get saved in the runJob call below)
			job.derivedFromSourceKey = src.getKey();
		}
		String savedError = job.errorMessage; // (hacky way of seeing if the job submit failed - errorMessage will have been rewritten)
		CustomApiUtils.runJobAndWaitForCompletion(job, false, false, null);
		
		//DEBUG
		//System.out.println("??? JOB = " + JsonPrettyPrinter.jsonObjectToTextFormatted(job.toDb(), 3));
		
		if (savedError != job.errorMessage) {
			if (null == src.getHarvestStatus()) { // (shouldn't ever be the case)
				src.setHarvestStatus(new SourceHarvestStatusPojo());
			}
			src.getHarvestStatus().setHarvest_status(HarvestEnum.error);
			context.getHarvestStatus().logMessage(job.errorMessage, false);
			src.getHarvestStatus().setHarvest_message(job.errorMessage); // (on the off-chance it doesn't get overwritten)
			
		}//TODO (INF-2865): TOTEST (needs non-local hadoop, ie to be deployed)
		
		// If set to suspend itself then do that now
		if ((null != src.getSearchCycle_secs()) && (0 == src.getSearchCycle_secs())) {
			
			BasicDBObject updateQuery = new BasicDBObject(SourcePojo._id_, src.getId());
			BasicDBObject set = new BasicDBObject(DbManager.set_, new BasicDBObject(SourcePojo.searchCycle_secs_, -1));
			DbManager.getIngest().getSource().update(updateQuery, set, false, false);
		}
		
	}//TESTED (by hand - apart from one case that needs deployment)
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public void decomposeCustomSourceIntoMultipleJobs(SourcePojo src, boolean updateMode) {
		
		//TODO (INF-2685): CUSTOM, should suspend/resume based on disabled/not disabled
		
		int srcType = InfiniteEnums.castExtractType(src.getExtractType());
		
		if (InfiniteEnums.POSTPROC == srcType) { // Register the post processing job
			createOrTestPostProcessingJob(src, false);
			// (auto works out if this is an update or not, so don't need to pass updateMode on) 
		}
		else if (InfiniteEnums.DISTRIBUTED == srcType) {
			//TODO (INF-2865): 
			throw new RuntimeException("HDFS: Not Yet Supported");
		}
		else if (InfiniteEnums.CUSTOM == srcType) {
			// TODO (INF-2865): in update mode, get all the jobs with the job prefix, delete any ones that no longer exist
			// (need to be pretty sure we're not going to throw an exception though)
			
			LinkedList<CustomMapReduceJobPojo> jobs = new LinkedList<CustomMapReduceJobPojo>();
			SourcePipelineToCustomConversion.convertSourcePipeline(src, jobs, false);
			
			long now = new Date().getTime();
			for (CustomMapReduceJobPojo job: jobs) {
				
				if (job.nextRunTime <= now) {
					CustomApiUtils.runJobAndWaitForCompletion(job, false, false, null);
					//(this saves the job)
					if ((null != job.jobidS) && !job.jobidS.isEmpty()) { // job started
						if (null == src.getHarvestStatus()) {
							src.setHarvestStatus(new SourceHarvestStatusPojo());
						}
						src.getHarvestStatus().setHarvest_status(HarvestEnum.success_iteration);
							// (otherwise the saveSource call will overwrite this)
					}
				}
				else if ((CustomApiUtils.DONT_RUN_TIME == job.nextRunTime) && (null != job.jobidS) && !job.jobidS.isEmpty()) {
					// currently running, now it's been disabled - so attempt to kill the running job					
					new CustomProcessingController().killRunningJob(job);
					if (null == src.getHarvestStatus()) {
						src.setHarvestStatus(new SourceHarvestStatusPojo());
					}
					src.getHarvestStatus().setHarvest_status(HarvestEnum.error);
					src.getHarvestStatus().setHarvest_message("Manually stopped");
						// (otherwise the saveSource call will overwrite this)
				}
				else {
					DbManager.getCustom().getLookup().save(job.toDb());
				}
			}
			// (source status will be updated once the job starts, using the derived job)
		}
		else { 
			// nothing to do, this isn't a distributed source...
		}
		
	}//TESTED - post processing test (see other cases)
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////////////////////

	@SuppressWarnings("unchecked")
	private void testCustomJobs(HarvestContext context, SourcePojo source, List<DocumentPojo> toAdd) {
		
		LinkedList<CustomMapReduceJobPojo> jobs = new LinkedList<CustomMapReduceJobPojo>();
		SourcePipelineToCustomConversion.convertSourcePipeline(source, jobs, true);
		
		if (null == source.getHarvestStatus()) {
			source.setHarvestStatus(new SourceHarvestStatusPojo());
		}
		source.getHarvestStatus().setHarvest_status(HarvestEnum.success);			
		
		for (CustomMapReduceJobPojo job: jobs) {
			
			int currTimesFailed = job.timesFailed;
			CustomApiUtils.runJobAndWaitForCompletion(job, true, true, context.getStandaloneMaxDocs());
			
			ResponsePojo rp = new ResponsePojo();  
			if (null != job.errorMessage) {
				context.getHarvestStatus().logMessage(job.jobtitle + ": " + job.errorMessage, false);
			}//TESTED (by hand, cp/ from testPostProcessing)
			
			if (job.timesFailed > currTimesFailed) {
				source.getHarvestStatus().setHarvest_status(HarvestEnum.error);
				break;
			}//TESTED (by hand)
			
			CustomApiUtils.getJobResults(rp, job, Integer.MAX_VALUE, null, null, null);
			// we know by construction that rp.getData() is a BasicDBList
			Object results = rp.getData();
			Collection<? extends Object> outputResults;
			if (results instanceof CustomMapReduceResultPojo) {
				outputResults = (Collection<? extends Object>)((CustomMapReduceResultPojo)results).results;
			}
			else if (results instanceof Collection<?> ) {
				outputResults = (Collection<? extends Object>)results;
			}
			else {
				throw new RuntimeException("Unexpected results object from custom test: " + results);					
			}
			
			for (Object val: outputResults) {
				BasicDBObject kv = (BasicDBObject) val;
				
				DocumentPojo doc = new DocumentPojo();
				doc.addToMetadata("record", kv); // like logstash - this is converted by testSource into raw objects
				toAdd.add(doc);
				
			}//(end loop over job results)			
		}//(end loop over jobs)
	}//TESTED (by hand)	
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////	
	
	@SuppressWarnings("unchecked")
	private void testPostProcessingModule(HarvestContext context, SourcePojo source, List<DocumentPojo> toAdd) {
		CustomMapReduceJobPojo job = createOrTestPostProcessingJob(source, true);
		
		int currTimesFailed = job.timesFailed;
		CustomApiUtils.runJobAndWaitForCompletion(job, true, true, context.getStandaloneMaxDocs());
		
		ResponsePojo rp = new ResponsePojo();  
		CustomApiUtils.getJobResults(rp, job, Integer.MAX_VALUE, null, null, null);
		// we know by construction that rp.getData() is a BasicDBList
		Object results = rp.getData();
		Collection<? extends Object> outputResults;
		if (results instanceof CustomMapReduceResultPojo) {
			outputResults = (Collection<? extends Object>)((CustomMapReduceResultPojo)results).results;
		}
		else if (results instanceof Collection<?> ) {
			outputResults = (Collection<? extends Object>)results;
		}
		else {
			throw new RuntimeException("Unexpected results object from post processing: " + results);					
		}
		StringBuffer statusStr = new StringBuffer();
		
		if (null != job.errorMessage) {
			context.getHarvestStatus().logMessage(job.errorMessage, false);
		}//TESTED (by hand)
		
		for (Object val: outputResults) {
			BasicDBObject kv = (BasicDBObject) val;
			// Documentation: https://ikanow.jira.com/wiki/display/INFAPI/Harvest+post+processor
			String key = kv.getString("key");
			if (null != key) {
				if (key.equalsIgnoreCase("modifieddocument")) {
					DocumentPojo doc = DocumentPojo.fromDb(kv, DocumentPojo.class);
					doc.setExplain("MODIFIED");
					toAdd.add(doc);							
				}//TESTED (by hand)
				else if (key.equalsIgnoreCase("deleteddocument")) {
					DocumentPojo doc = DocumentPojo.fromDb(kv, DocumentPojo.class);
					doc.setExplain("DELETED");
					toAdd.add(doc);							
				}//TESTED (c/p from above)
				else if (key.equalsIgnoreCase("runProcessingLoop")) { // (take "message" field)
					context.getHarvestStatus().logMessage(kv.getString("message", ""), false);
				}//TESTED (by hand)
				else if (key.equalsIgnoreCase("completeMapper")) { // (take "message" field)
					context.getHarvestStatus().logMessage(kv.getString("message", ""), false);
				}//TESTED (by hand)
				else if (key.equalsIgnoreCase("WARNING")) { // (take "message" field)
					context.getHarvestStatus().logMessage(kv.getString("error", ""), false);
				}//TESTED (added '{"criteria": "test","searchIndex": {}}' to test)
			}
		}//TESTED (most cases, close enough)
		
		// Make double sure that 
		DbManager.getCollection(job.getOutputDatabase(), job.outputCollection).drop();
		DbManager.getCollection(job.getOutputDatabase(), job.outputCollectionTemp).drop();
		DbManager.getCustom().getLookup().remove(new BasicDBObject("_id", job._id));
		
		if (null == source.getHarvestStatus()) {
			source.setHarvestStatus(new SourceHarvestStatusPojo());
		}
		if (job.timesFailed > currTimesFailed) {
			source.getHarvestStatus().setHarvest_status(HarvestEnum.error);
		}//TESTED (by hand)
		else {
			source.getHarvestStatus().setHarvest_status(HarvestEnum.success);			
		}
		source.getHarvestStatus().setHarvest_message(statusStr.toString()); 		
	}//TESTED (by hand)
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private static Boolean _distributedHarvestModuleExists = null;
	private static final ObjectId _distributedHarvestModuleId = new ObjectId("53843a21fcb505b3da966199");
	
	static public class PipelinePrettyPrinter implements BasePojoApiMap<Object> {
		@Override
		public GsonBuilder extendBuilder(GsonBuilder gp) {
			return gp.setPrettyPrinting();
		}
		
	}
	
	private CustomMapReduceJobPojo createOrTestPostProcessingJob(SourcePojo src, boolean testNotCreateMode) {

		// Quick check that it's actually installed,  first time only - permissions check will occur as part of the normal processing
		if (null == _distributedHarvestModuleExists) {
			_distributedHarvestModuleExists = (null != DbManager.getSocial().getShare().findOne(new BasicDBObject(SharePojo._id_, _distributedHarvestModuleId), new BasicDBObject()));
		}
		else if (!_distributedHarvestModuleExists) {
			throw new RuntimeException("Distributed Harvest Module is not installed (currently deployed via infinit.e-enterprise RPM or manually by request");
		}
		
		CustomMapReduceJobPojo job = SourcePipelineToCustomConversion.handleInitializeOrGetJob(src, testNotCreateMode);
		
		// Post-processing specific configuration
		job.jarURL = "$infinite/share/get/" + _distributedHarvestModuleId.toString();
		job.inputCollection = "doc_metadata.metadata";
		job.mapper = "com.ikanow.infinit.e.hadoop.processing.InfiniteProcessingEngine$InfiniteMapper";
		job.combiner = "none";
		job.reducer = "none";
		job.outputKey = "org.apache.hadoop.io.Text";
		job.outputValue = "com.mongodb.hadoop.io.BSONWritable";
		
		Iterator<SourcePipelinePojo> it = src.getProcessingPipeline().iterator();
		BasicDBObject query = null;
		BasicDBObject config = new BasicDBObject();
		if (it.hasNext()) {
			// Query: from the first element only
			
			SourcePipelinePojo px = it.next();
			if (null != px.postProcessing) {
				if (null != px.postProcessing.query) {
					query = SourcePipelineToCustomConversion.handleDocumentQuery(px.postProcessing.query, src, job);
				}
				else {
					query = new BasicDBObject();
				}
				SourcePipelineToCustomConversion.handleGroupOverride(px.postProcessing.groupOverrideList, px.postProcessing.groupOverrideRegex, job, src);				
				SourcePipelineToCustomConversion.handleCommonInFields(null, null, px.postProcessing.tmin, px.postProcessing.tmax, px.postProcessing.srcTags, query);
				
				// Configuration: from the "extractor" object
				if (null != px.postProcessing.debugMode) {
					config.put("debugMode", px.postProcessing.debugMode);					
				}//TESTED (postproc_*_test)
				else if (testNotCreateMode) {
					config.put("debugMode", true);
				}//TESTED (by hand) 
				else {
					config.put("debugMode", false);					
				}
				if (null != px.postProcessing.rebuildAllCommunities) {
					config.put("rebuildAllCommunities", px.postProcessing.rebuildAllCommunities);
				}//TESTED (postproc_*_test)

				// User can set daily schedule, instead of just allowing it to harvest
				if (!testNotCreateMode) {
					if ((null == src.getSearchCycle_secs()) || (src.getSearchCycle_secs() >= 0)) {
						if (ScheduleType.daily == px.postProcessing.scheduleMode) {
							job.scheduleFreq = SCHEDULE_FREQUENCY.DAILY;
							if (null != px.postProcessing.dailySchedule) {
								try {
									job.nextRunTime = getTimeFromTimeString(px.postProcessing.dailySchedule).getTime();
								}
								catch (Exception e) {
									throw new RuntimeException("Error parsing daily schedule time (hh:mm:ss): " + px.postProcessing.dailySchedule);
								}
							}
							else { // (just use now
								job.nextRunTime = new Date().getTime();
							}
							src.setSearchCycle_secs(360); // (check every 10 minutes)
						}//TESTED (both suspended and resume cases)
					}
					else { // src is disabled
						if (ScheduleType.daily == px.postProcessing.scheduleMode) {
							job.scheduleFreq = SCHEDULE_FREQUENCY.NONE; 
							job.nextRunTime = CustomApiUtils.DONT_RUN_TIME;
						}
					}
				}
				else if ((ScheduleType.daily == px.postProcessing.scheduleMode) && (null != px.postProcessing.dailySchedule))
				{ 
					// just try parsing the date so the user knows if it's wrong
					try {
						getTimeFromTimeString(px.postProcessing.dailySchedule).getTime();
					}
					catch (Exception e) {
						throw new RuntimeException("Error testing parsing of daily schedule time (hh:mm:ss): " + px.postProcessing.dailySchedule);
					}					
				}//TESTED (by hand)
				
			}//(end if post processing object exists)
		}//(get first element in pipeline)
		if (null == query) {
			throw new RuntimeException("No processing pipeline specified (or doesn't start with post processing element");
		}
		// For the rest of the configuration object: just copy the entire pipeline into		
		BasicDBObject srcDbo = (BasicDBObject) src.toDb();
		BasicDBList procPipeline = (BasicDBList) srcDbo.get(SourcePojo.processingPipeline_);
		procPipeline.remove(0); // (remove the first element)
		config.put(SourcePojo.processingPipeline_, procPipeline);		

		StringBuffer caches = new StringBuffer();
		while (it.hasNext()) {
			// Hunt through the pipeline looking for things that need to get specified in the cache
			SourcePipelinePojo px = it.next();
			if (null != px.lookupTables) {
				for (ObjectId cacheVal: px.lookupTables.values()) {
					caches.append(cacheVal).append(',');
				}
			}
			else if (null != px.featureEngine) {
				try {
					ObjectId cacheVal = new ObjectId(px.featureEngine.engineName);
					caches.append(cacheVal).append(',');
				}
				catch (Exception e) {} // not a dynamic extractor, fine carry on
			}
			else if (null != px.textEngine) {
				try {
					ObjectId cacheVal = new ObjectId(px.textEngine.engineName);
					caches.append(cacheVal).append(',');
				}
				catch (Exception e) {} // not a dynamic extractor, fine carry on
			}
		}//TESTED (postproc_datastore_test)
		if (caches.length() > 0) {
			caches.setLength(caches.length() - 1); // (remove trailing ,)
			query.put("$caches", caches.toString());
		}//TESTED (postproc_datastore_test)
		
		job.query = JsonPrettyPrinter.jsonObjectToTextFormatted(query, 3);
		job.arguments = ApiManager.mapToApi(config, new PipelinePrettyPrinter());
			// (JsonPrettyPrinter maps OID->{$oid:id}, ApiManager maps it to id)
		
		//DEBUG
		//System.out.println("??? JOB = " + JsonPrettyPrinter.jsonObjectToTextFormatted(job.toDb(), 3));
		
		if (!testNotCreateMode) { // save
			DbManager.getCustom().getLookup().save(job.toDb());
		}
		
		return job;
	}//TESTED (postproc_*_test)
	
	@SuppressWarnings("deprecation")
	private static Date getTimeFromTimeString(String timeString)
	{
	    String[] splitStrings = timeString.split(":");

	    Date timeDate = new Date();
	    timeDate.setHours(Integer.parseInt(splitStrings[0]));
	    timeDate.setMinutes(Integer.parseInt(splitStrings[1]));
	    timeDate.setSeconds(Integer.parseInt(splitStrings[2]));

	    return timeDate;
	}
}
