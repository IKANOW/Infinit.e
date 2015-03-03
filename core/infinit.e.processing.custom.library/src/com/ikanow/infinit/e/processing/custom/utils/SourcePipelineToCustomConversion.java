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

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bson.types.ObjectId;

import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePipelinePojo;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePipelinePojo.CustomOutputTable.AppendMode;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePipelinePojo.CustomScheduler;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePipelinePojo.DocumentByDatastoreQuery.ContentMode;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePipelinePojo.FeatureByDatastoreQuery.FeatureName;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePipelinePojo.RecordByIndexQuery.StreamingMode;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.ikanow.infinit.e.data_model.store.custom.mapreduce.CustomMapReduceJobPojo;
import com.ikanow.infinit.e.data_model.store.custom.mapreduce.CustomMapReduceJobPojo.SCHEDULE_FREQUENCY;
import com.ikanow.infinit.e.data_model.utils.JsonPrettyPrinter;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;

public class SourcePipelineToCustomConversion {

	public static void convertSourcePipeline(SourcePojo in, List<CustomMapReduceJobPojo> out, boolean testNotCreateMode) {
		BasicDBObject query = new BasicDBObject();
		BasicDBObject queryOutput = null; // (holds complex object)
		
		// Not sure if this will be string or JSON object..
		StringBuffer args = null;
		BasicDBObject argsJson = null;
		
		boolean haveInput = false;
		SourcePipelinePojo scorecard = new SourcePipelinePojo();
		
		List<String> caches = new LinkedList<String>();
		
		// Create a generic-ish set of fields for the job
		CustomMapReduceJobPojo job = handleInitializeOrGetJob(in, testNotCreateMode);
		
		// Now modify the fields based on the processing pipeline
		if (null != in.getProcessingPipeline()) for (SourcePipelinePojo px: in.getProcessingPipeline()) {
			
			if (null != px.custom_datastoreQuery) {
				if (haveInput) throw new RuntimeException("Currently only support one input block");
				haveInput = true;
				
				job.isCustomTable = true;
				
				job.inputCollection = px.custom_datastoreQuery.customTable;
				query = handleCommonInFields(px.custom_datastoreQuery.query, px.custom_datastoreQuery.fields, px.custom_datastoreQuery.tmin, px.custom_datastoreQuery.tmax, null, null);
			}
			else if (null != px.custom_file) { // HDFS or Ikanow share
				if (haveInput) throw new RuntimeException("Currently only support one input block");
				haveInput = true;

				SourcePojo temp = new SourcePojo();
				temp.setFileConfig(px.custom_file);
				BasicDBObject fileObj = (BasicDBObject) temp.toDb().get(SourcePojo.file_);
				query = new BasicDBObject(SourcePojo.file_, fileObj);

				String url = fileObj.getString("url", "will_error_later");
				
				if (url.startsWith("inf://share/")) {
					job.inputCollection = "file.binary_shares";					
				}
				else {
					fileObj.put("url", url.replace("hdfs:///", "/").replace("hdfs:", "")); // (get rid of leading hdfs:)
					job.inputCollection = "filesystem";
				}				
			}
			else if (null != px.docs_datastoreQuery) {
				if (haveInput) throw new RuntimeException("Currently only support one input block");
				haveInput = true;
				
				if (ContentMode.content == px.docs_datastoreQuery.contentMode) {
					job.inputCollection = "doc_content.gzip_content";					
				}
				else if ((null == px.docs_datastoreQuery.contentMode) || (ContentMode.metadata == px.docs_datastoreQuery.contentMode)) {
					job.inputCollection = "doc_metadata.metadata";
				}
				else {
					throw new RuntimeException("Both content + metadata in the same job: not currently supported");
				}
				query = handleCommonInFields(px.docs_datastoreQuery.query, px.docs_datastoreQuery.fields, px.docs_datastoreQuery.tmin, px.docs_datastoreQuery.tmax, px.docs_datastoreQuery.srcTags, null);
			}
			else if (null != px.docs_documentQuery) {
				if (haveInput) throw new RuntimeException("Currently only support one input block");
				haveInput = true;
				
				job.inputCollection = "doc_metadata.metadata";
				
				query = handleDocumentQuery(px.docs_documentQuery.query, in, job);
			}
			else if (null != px.records_indexQuery) {
				if (haveInput) throw new RuntimeException("Currently only support one input block");
				haveInput = true;
				
				job.inputCollection = "records";
				query = handleCommonInFields(null, null, px.records_indexQuery.tmin, px.records_indexQuery.tmax, null, new BasicDBObject());
				if (null != px.records_indexQuery.query) {
					if (px.records_indexQuery.query.trim().startsWith("{")) {
						query.put("query", com.mongodb.util.JSON.parse(px.records_indexQuery.query));
					}
					else {
						query.put("query", px.records_indexQuery.query);
					}
				}
				if (null != px.records_indexQuery.filter) {
					if (px.records_indexQuery.filter.trim().startsWith("{")) {
						query.put("filter", com.mongodb.util.JSON.parse(px.records_indexQuery.filter));
					}
					else {
						query.put("filter", px.records_indexQuery.filter);
					}
				}
				if (null != px.records_indexQuery.types) {
					query.put("$types", px.records_indexQuery.types);
				}
				if (null != px.records_indexQuery.streamingMode) {
					if (StreamingMode.stashed == px.records_indexQuery.streamingMode) {
						query.put("$streaming", false);
					}
					else if (StreamingMode.streaming == px.records_indexQuery.streamingMode) {
						query.put("$streaming", true);						
					}
					//(else don't set $streaming, defaults to both)
				}
				// (else don't set $streaming, defaults to both)
			}
			else if (null != px.feature_datastoreQuery) {
				if (haveInput) throw new RuntimeException("Currently only support one input block");
				haveInput = true;				
		
				if (FeatureName.association == px.feature_datastoreQuery.featureName) {
					job.inputCollection = "feaure.association";
				}
				else if (FeatureName.entity == px.feature_datastoreQuery.featureName) {
					job.inputCollection = "feaure.entity";
				}
				else if (FeatureName.temporal == px.feature_datastoreQuery.featureName) {
					job.inputCollection = "feaure.temporal";
				}
				query = handleCommonInFields(px.feature_datastoreQuery.query, px.feature_datastoreQuery.fields, px.feature_datastoreQuery.tmin, px.feature_datastoreQuery.tmax, null, null);
			}
			else if (null != px.extraInputSettings) {		
				if (!haveInput) throw new RuntimeException("Job must start with an input block");
				
				handleGroupOverride(px.extraInputSettings.groupOverrideList, px.extraInputSettings.groupOverrideRegex, job, in);
				
				if (null != px.extraInputSettings.debugLimit) {
					query.put("$limit", px.extraInputSettings.debugLimit);
				}
				if (null != px.extraInputSettings.docsPerSplitOverride) {
					query.put("$docsPerSplit", px.extraInputSettings.docsPerSplitOverride);
				}
				if (null != px.extraInputSettings.numSplitsOverride) {
					query.put("$splits", px.extraInputSettings.numSplitsOverride);
				}
			}
			else if (null != px.scheduler) {
				if (null != scorecard.scheduler) throw new RuntimeException("Only support one scheduler");
				scorecard.scheduler = px.scheduler;

				boolean isDisabled = false;
				if (null == px.scheduler.frequency) {
					px.scheduler.frequency = CustomScheduler.FrequencyMode.disabled;
				}
				if (CustomScheduler.FrequencyMode.once_only == px.scheduler.frequency) {
					job.scheduleFreq = SCHEDULE_FREQUENCY.NONE;
				}
				else if (CustomScheduler.FrequencyMode.hourly == px.scheduler.frequency) {
					job.scheduleFreq = SCHEDULE_FREQUENCY.HOURLY;						
				}
				else if (CustomScheduler.FrequencyMode.daily == px.scheduler.frequency) {
					job.scheduleFreq = SCHEDULE_FREQUENCY.DAILY;												
				}
				else if (CustomScheduler.FrequencyMode.weekly == px.scheduler.frequency) {
					job.scheduleFreq = SCHEDULE_FREQUENCY.WEEKLY;																		
				}
				else if (CustomScheduler.FrequencyMode.monthly == px.scheduler.frequency) {
					job.scheduleFreq = SCHEDULE_FREQUENCY.MONTHLY;																		
				}
				else if (CustomScheduler.FrequencyMode.disabled == px.scheduler.frequency) {
					isDisabled = true;
					job.scheduleFreq = SCHEDULE_FREQUENCY.NONE;						
					job.nextRunTime = CustomApiUtils.DONT_RUN_TIME; 
				}
				else if (CustomScheduler.FrequencyMode.ondemand == px.scheduler.frequency) {
					isDisabled = true;
					job.nextRunTime = CustomApiUtils.DONT_RUN_TIME; //01-01-2099 in milliseconds! Will use this constant to mean "dont' run" - CustomHandler.DONT_RUN_TIME					
					
					//TODO (INF-2865): to implement
					throw new RuntimeException("'OnDemand' not yet supported");
				}
				
				if (!isDisabled) {
					if (null != scorecard.scheduler.runDate) {
						Date d = InfiniteHadoopUtils.dateStringFromObject(scorecard.scheduler.runDate, true);
						if (null != d) {
							// Special case: if once_only and runDate < now then update it
							if (CustomScheduler.FrequencyMode.once_only == px.scheduler.frequency) {
								long now = new Date().getTime();
								if (d.getTime() < now) {
									job.nextRunTime = now;
								}
								else {	
									job.nextRunTime = d.getTime();
								}
							}
							else {
								// (otherwise retain it so that it gets used to determine the next time)
								job.nextRunTime = d.getTime();								
							}
						}
					}
					else if (Long.MAX_VALUE == job.nextRunTime) { // (ie not set => field left at its default)
						job.nextRunTime = new Date().getTime();
					}
					if ((null == job.firstSchedule) || (CustomApiUtils.DONT_RUN_TIME == job.firstSchedule.getTime())) {
						// (ie if firstSchedule not set then set it)
						job.firstSchedule = new Date(job.nextRunTime);
					}					
				}//(else already set)
				
				if (null != scorecard.scheduler.autoDependency) {
					//(will eventually automatically automatically generate a dependency on any custom input tables)
					//TODO (INF-2865): to implement
					throw new RuntimeException("'Automatic dependencies' not yet supported");
				}

				if (null != scorecard.scheduler.dependencies) {
					try {
						job.jobDependencies = new HashSet<ObjectId>(scorecard.scheduler.dependencies.size());
						for (String depId: scorecard.scheduler.dependencies) {
							job.jobDependencies.add(new ObjectId(depId));
						}
					}
					catch (Exception e) {
						throw new RuntimeException("Custom Scheduler Dependencies: invalid Dependency in " + Arrays.toString(scorecard.scheduler.dependencies.toArray()));
					}
				}
				
				// First time through, can overwrite some of the fields: 
				if ((null == in.getHarvestStatus()) || (null == in.getHarvestStatus().getHarvest_status())) {
					job.timesRan = 0; // (if we're setting the initial override, then need to ensure that it's unset after running)
					job.timesFailed = 0;
					
					// Unset any tmin/tmax/srctags fields if set to " "s
					String tminOver = px.scheduler.tmin_initialOverride;
					String tmaxOver = px.scheduler.tmax_initialOverride;
					String srctagsOver = px.scheduler.srcTags_initialOverride;
					if (null != tminOver) {
						tminOver = tminOver.trim(); // (hence will be ignored)
						if (tminOver.isEmpty()) {
							query.remove("$tmin");
						}
					}
					if (null != tmaxOver) {
						tmaxOver = tmaxOver.trim();					
						if (tmaxOver.isEmpty()) {
							query.remove("$tmax");
						}
					}
					if (null != srctagsOver) {
						srctagsOver = srctagsOver.trim();							
						if (srctagsOver.isEmpty()) {
							query.remove("$srctags");
						}
					}//TESTED (custom_scheduler_test_2, custom_scheduler_test_1)
					
					if (null == px.scheduler.query_initialOverride) { // easy, just override fields from existing query
						query = handleCommonInFields(null, null, tminOver, tmaxOver, srctagsOver, query);						
					}//TESTED (custom_scheduler_test_1)
					else { // one extra complication ... if tmin/tmax/srctags _aren't_ overridden then use originals instead
						if (null == tminOver) tminOver = query.getString("$tmin");
						if (null == tmaxOver) tmaxOver = query.getString("$tmax");
						if (null == srctagsOver) srctagsOver = query.getString("$srctags");
						query = handleCommonInFields(px.scheduler.query_initialOverride, null, tminOver, tmaxOver, srctagsOver, null);
					}//TESTED (custom_scheduler_test_2 - some fields override (+ve or -ve), some pulled from original)
				}
				//TESTED (that first time through harvest|harvest.status==null, subsequently not)
			}
			else if (null != px.artefacts) {
				if (!haveInput) throw new RuntimeException("Job must start with an input block");
				
				if (null != px.artefacts.mainJar) {
					String jar = null;
					// A few options:
					// $infinite/.../<id> or <id> or a URL
					try {
						jar = new ObjectId(px.artefacts.mainJar).toString();
						jar = "$infinite/share/get/" + jar;
					}
					catch (Exception e) {} // fall through to...

					if (null == jar) {
						jar = px.artefacts.mainJar;
					}							
					job.jarURL = jar;
				}
				if (null != px.artefacts.extraJars) {
					for (String jarId: px.artefacts.extraJars) {
						caches.add(jarId);						
					}
				}
				if (null != px.artefacts.joinTables) {
					for (String shareId: px.artefacts.joinTables) {
						caches.add(shareId);						
					}
				}				
				if (null != px.artefacts.selfJoin) {
					job.selfMerge = px.artefacts.selfJoin;
				}
			}
			else if (null != px.mapper) {
				if (!haveInput) throw new RuntimeException("Job must start with an input block");
				
				if (null != scorecard.scriptingEngine) throw new RuntimeException("Can't have a scriptingEngine and mapper");
				if (null != scorecard.hadoopEngine) throw new RuntimeException("Can't have a hadoopEngine and mapper");
				if (null != scorecard.mapper) throw new RuntimeException("Currently only support one mapper");
				scorecard.mapper = px.mapper;
				
				job.mapper = px.mapper.mapperClass;
				
				if (null != px.mapper.mapperKeyClass) {
					query.put("$mapper_key_class", px.mapper.mapperKeyClass);
				}
				if (null != px.mapper.mapperValueClass) {
					query.put("$mapper_value_class", px.mapper.mapperValueClass);
				}
			}
			else if (null != px.combiner) {
				if (!haveInput) throw new RuntimeException("Job must start with an input block");
				
				if (null != scorecard.scriptingEngine) throw new RuntimeException("Can't have a scriptingEngine and combiner");
				if (null != scorecard.hadoopEngine) throw new RuntimeException("Can't have a hadoopEngine and combiner");
				if (null != scorecard.combiner) throw new RuntimeException("Currently only support one combiner");
				scorecard.combiner = px.combiner;
				
				job.combiner = px.combiner.combinerClass;
			}
			else if (null != px.reducer) {
				if (!haveInput) throw new RuntimeException("Job must start with an input block");
				
				if (null != scorecard.scriptingEngine) throw new RuntimeException("Can't have a scriptingEngine and reducer");
				if (null != scorecard.hadoopEngine) throw new RuntimeException("Can't have a hadoopEngine and reducer");
				if (null != scorecard.reducer) throw new RuntimeException("Currently only support one reducer");
				scorecard.reducer = px.reducer;
				
				job.reducer = px.reducer.reducerClass;
				
				if (null != px.reducer.numReducers) {
					query.put("$reducers", px.reducer.numReducers);
				}
				if (null != px.reducer.outputKeyClass) {
					job.outputKey = px.reducer.outputKeyClass;
				}
				if (null != px.reducer.outputValueClass) {
					job.outputValue = px.reducer.outputValueClass;
				}
			}
			else if (null != px.hadoopEngine) {
				if (!haveInput) throw new RuntimeException("Job must start with an input block");
				
				if (null != scorecard.scriptingEngine) throw new RuntimeException("Only one of: scriptingEngine, hadoopEngine");
				if (null != scorecard.hadoopEngine) throw new RuntimeException("Only support one hadoopEngine");
				if (null != scorecard.mapper) throw new RuntimeException("Can't have a hadoopEngine and mapper");
				if (null != scorecard.combiner) throw new RuntimeException("Can't have a hadoopEngine and combiner");
				if (null != scorecard.reducer) throw new RuntimeException("Can't have a hadoopEngine and reducer");
				scorecard.hadoopEngine = px.hadoopEngine;
				
				if (null != px.hadoopEngine.mainJar) {
					String jar = null;
					// A few options:
					// $infinite/.../<id> or <id> or a URL
					try {
						jar = new ObjectId(px.hadoopEngine.mainJar).toString();
						jar = "$infinite/share/get/" + jar;
					}
					catch (Exception e) {} // fall through to...

					if (null == jar) {
						jar = px.hadoopEngine.mainJar;
					}							
					job.jarURL = jar;
				}
				
				job.mapper = px.hadoopEngine.mapperClass;
				if (null != px.hadoopEngine.combinerClass) {
					job.combiner = px.hadoopEngine.combinerClass;
				}
				else {
					job.combiner = "none";
				}
				if (null != px.hadoopEngine.reducerClass) {
					job.reducer = px.hadoopEngine.reducerClass;
				}
				else {
					job.reducer  = "none";
				}
				job.outputKey = px.hadoopEngine.outputKeyClass;
				job.outputValue = px.hadoopEngine.outputValueClass;
				
				if (null != px.hadoopEngine.mapperKeyClass) {
					query.put("$mapper_key_class", px.hadoopEngine.mapperKeyClass);
				}
				if (null != px.hadoopEngine.mapperValueClass) {
					query.put("$mapper_value_class", px.hadoopEngine.mapperValueClass);
				}
				if (null != px.hadoopEngine.numReducers) {
					query.put("$reducers", px.hadoopEngine.numReducers);
				}
				
				if (null != px.hadoopEngine.configuration) { 
					if (px.hadoopEngine.configuration.trim().startsWith("{")) {
						argsJson = (BasicDBObject) com.mongodb.util.JSON.parse(px.hadoopEngine.configuration);
						if (null != px.hadoopEngine.configParams) for (Map.Entry<String, String> param: px.hadoopEngine.configParams.entrySet()) {
							argsJson.put(param.getKey(), param.getValue());
						}
					}
					else {
						args = new StringBuffer(px.hadoopEngine.configuration);
						if (null != px.hadoopEngine.configParams) {
							throw new RuntimeException("Can only specify hadoopEngine.configParams when hadoopEngine.configuration is in JSON format");
						}
					}
				}
				else {
					args = new StringBuffer(); // (ie just "")
				}
			}
			else if (null != px.scriptingEngine) {
				if (!haveInput) throw new RuntimeException("Job must start with an input block");
				
				if (null != scorecard.hadoopEngine) throw new RuntimeException("Only one of: scriptingEngine, hadoopEngine");
				if (null != scorecard.scriptingEngine) throw new RuntimeException("Only support one scriptingEngine");
				if (null != scorecard.mapper) throw new RuntimeException("Can't have a scriptingEngine and mapper");
				if (null != scorecard.combiner) throw new RuntimeException("Can't have a scriptingEngine and combiner");
				if (null != scorecard.reducer) throw new RuntimeException("Can't have a scriptingEngine and reducer");
				scorecard.scriptingEngine = px.scriptingEngine;

				//TODO (INF-2865): handle jython scripting engine (mainJar and also the classes below)
				job.jarURL = InfiniteHadoopUtils.BUILT_IN_JOB_PATH;
				
				args = new StringBuffer();
				
				if (null != px.scriptingEngine.numReducers) {
					query.put("$reducers", px.scriptingEngine.numReducers);
				}
				
				if (null != px.scriptingEngine.memoryOptimized) {
					args.append("_memoryOptimization = ").append(px.scriptingEngine.memoryOptimized).append(";\n\n");
				}
				if ((null != px.scriptingEngine.globalScript) && !px.scriptingEngine.globalScript.isEmpty()) {
					args.append(px.scriptingEngine.globalScript).append("\n\n");
				}
				
				job.mapper = "com.ikanow.infinit.e.utility.hadoop.HadoopPrototypingTool$JavascriptMapper";
				if ((null != px.scriptingEngine.mapScript) && !px.scriptingEngine.mapScript.isEmpty()) {
					args.append(px.scriptingEngine.mapScript).append("\n\n");					
				}
				if ((null != px.scriptingEngine.combineScript) && !px.scriptingEngine.combineScript.isEmpty()) {
					job.combiner = "com.ikanow.infinit.e.utility.hadoop.HadoopPrototypingTool$JavascriptCombiner";
					args.append(px.scriptingEngine.combineScript).append("\n\n");					
				}
				else {
					job.combiner = "#com.ikanow.infinit.e.utility.hadoop.HadoopPrototypingTool$JavascriptCombiner";
				}
				if ((null != px.scriptingEngine.reduceScript) && !px.scriptingEngine.reduceScript.isEmpty()) {
					job.reducer = "com.ikanow.infinit.e.utility.hadoop.HadoopPrototypingTool$JavascriptReducer";
					args.append(px.scriptingEngine.reduceScript).append("\n\n");					
				}
				else {
					job.reducer = "#com.ikanow.infinit.e.utility.hadoop.HadoopPrototypingTool$JavascriptReducer";
				}
				job.outputKey = "com.mongodb.hadoop.io.BSONWritable";
				job.outputValue = "com.mongodb.hadoop.io.BSONWritable";
			}
			else if (null != px.tableOutput) {
				if (!haveInput) throw new RuntimeException("Job must start with an input block");
				
				if (null != scorecard.tableOutput) throw new RuntimeException("Only support one tableOutput");
				scorecard.tableOutput = px.tableOutput;
				
				if (null != px.tableOutput.ageOut_days) {
					job.appendAgeOutInDays = px.tableOutput.ageOut_days;
				}
				if (null != px.tableOutput.globalObjectLimit) {
					if (null == queryOutput) {
						queryOutput = new BasicDBObject();
						query.put("$output", queryOutput);
					}
					queryOutput.put("limit", px.tableOutput.globalObjectLimit);
					queryOutput.put("limitAllData", true);
				}
				if (null != px.tableOutput.perCycleObjectLimit) {
					if (null != px.tableOutput.globalObjectLimit) {
						throw new RuntimeException("Currently can support only one of: globalObjectLimit, perCycleObjectLimit in tableOutput");
					}
					
					if (null == queryOutput) {
						queryOutput = new BasicDBObject();
						query.put("$output", queryOutput);
					}
					queryOutput.put("limit", px.tableOutput.globalObjectLimit);
					queryOutput.put("limitAllData", false);					
				}
				if (null != px.tableOutput.sortDirection) {
					if (null == queryOutput) {
						queryOutput = new BasicDBObject();
						query.put("$output", queryOutput);
					}
					queryOutput.put("sortDirection", px.tableOutput.sortDirection);
				}
				if (null != px.tableOutput.sortField) {
					if (null == queryOutput) {
						queryOutput = new BasicDBObject();
						query.put("$output", queryOutput);
					}
					queryOutput.put("sortField", px.tableOutput.sortField);					
				}
				if (null != px.tableOutput.appendMode) {
					if (AppendMode.append_merge == px.tableOutput.appendMode) {
						job.appendResults = true;
						job.incrementalMode = false;
					}
					else if (AppendMode.append_reduce == px.tableOutput.appendMode) {
						job.appendResults = true;
						job.incrementalMode = true;						
					}
					//(else leave alone)
				}
				if (null != px.tableOutput.dataStoreIndexes) {
					if (null == queryOutput) {
						queryOutput = new BasicDBObject();
						query.put("$output", queryOutput);
					}
					queryOutput.put("indexed", com.mongodb.util.JSON.parse(px.tableOutput.dataStoreIndexes));
				}
				if (!testNotCreateMode) {
					if (null != px.tableOutput.indexed) {
						if (px.tableOutput.indexed) {
							if (null == queryOutput) {
								queryOutput = new BasicDBObject();
								query.put("$output", queryOutput);
							}
							queryOutput.put("indexMode", "custom");
						}
					}
				}
				if (null != px.tableOutput.postFixName) {
					throw new RuntimeException("Can't currently specify a postFix for job names - job name == source key");
				}
			}
			//(don't allow any other output types in test mode?)
			
		}//(end loop over pipeline elements)
	
		completeJob(job, query, caches, (null != args) ? args.toString() : null, argsJson, scorecard);
		out.add(job);
	}

	/////////////////////////////////////////////////////////////////////////////
	
	// Local utils
	
	private static void completeJob(CustomMapReduceJobPojo job, BasicDBObject query, List<String> caches, String config, BasicDBObject configJson, SourcePipelinePojo scorecard) {

		// Sort out whether the mapper output classes or the reducer classes are the actual output class:
		
		if (null == scorecard.reducer) { // mapper only
			if (null != scorecard.mapper) {
				if (null != scorecard.mapper.mapperKeyClass) {
					job.outputKey = scorecard.mapper.mapperKeyClass;
					query.remove("$mapper_key_class");
				}
				if (null != scorecard.mapper.mapperValueClass) {
					job.outputValue = scorecard.mapper.mapperValueClass;
					query.remove("$mapper_value_class");
				}
			}
		}
		
		// Copy across caches into the query:
		
		if ((null != caches) && !caches.isEmpty()) {
			query.put("$caches", caches);
		}
		
		// Copy across the query
		// Copy across the args
		
		job.query = JsonPrettyPrinter.jsonObjectToTextFormatted(query, 3);  
		if (null != configJson) { 
			job.arguments = JsonPrettyPrinter.jsonObjectToTextFormatted(configJson, 3);
		}
		else {
			job.arguments = config;
		}
		
		//DEBUG
		//System.out.println("??? JOB = " + JsonPrettyPrinter.jsonObjectToTextFormatted(job.toDb(), 3));
	}
	
	/////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////
	
	// Global utils
	
	/////////////////////////////////////////////////////////////////////////////
	
	public static CustomMapReduceJobPojo handleInitializeOrGetJob(SourcePojo src, boolean testNotCreateMode) {
		boolean newElement = false;
		CustomMapReduceJobPojo job = null;
		if (!testNotCreateMode) {// Check to see if the job already exists, overwrite from DB if so (and then will mostly overwrite again)
			BasicDBObject query = new BasicDBObject(CustomMapReduceJobPojo.jobtitle_, src.getKey());
			job = CustomMapReduceJobPojo.fromDb(DbManager.getCustom().getLookup().findOne(query),CustomMapReduceJobPojo.class);
		}
		if (null == job) {
			job = new CustomMapReduceJobPojo();
			newElement = true;
		}
		
		if (newElement) job._id = new ObjectId();
		job.jobtitle = src.getKey();
		job.jobdesc = src.getDescription();
		job.submitterID = src.getOwnerId();
		job.communityIds =  new ArrayList<ObjectId>(src.getCommunityIds().size());
		job.communityIds.addAll(src.getCommunityIds());
		if (newElement) job.outputCollection = new StringBuffer(job._id.toString()).append("_1").toString();
		if (testNotCreateMode) 
			job.nextRunTime = new Date().getTime();
		else // (don't run yet, scheduled to run via harvest)
			job.nextRunTime = CustomApiUtils.DONT_RUN_TIME; 
		
		job.scheduleFreq = CustomMapReduceJobPojo.SCHEDULE_FREQUENCY.NONE;
		if (newElement) job.firstSchedule = new Date(job.nextRunTime);
		if (newElement) job.timesRan = 0;
		if (newElement) job.timesFailed = 0;
		if (newElement) job.lastRunTime = new Date();
		//job.query BELOW
		job.isCustomTable = false;
		job.appendResults = false;
		job.incrementalMode = false;
		job.appendAgeOutInDays = 0.0; // (mandatory)
		if (newElement) job.outputCollectionTemp = new StringBuffer(job._id.toString()).append("_2").toString();
		if (newElement) job.setOutputDatabase(CustomApiUtils.getJobDatabase(job));
		//job.arguments BELOW
		job.exportToHdfs = false;
		job.selfMerge = false;
		// (don't set lastCompletionTime, filled in first time it's completed)
		if (!testNotCreateMode) job.derivedFromSourceKey = src.getKey(); // (=> when it completes it will update the source) 
		
		return job;
	}
	
	/////////////////////////////////////////////////////////////////////////////
	
	private static Pattern _queryJsonExtractor = Pattern.compile("[?&](?:json|query)=([^&]+)");
	
	public static BasicDBObject handleDocumentQuery(String queryStr, SourcePojo src, CustomMapReduceJobPojo job)
	{
		BasicDBObject query = null;
		if (null != queryStr) {
			queryStr = queryStr.trim();
			if (0 == queryStr.length()) {
				query  = new BasicDBObject();
			}
			else {
				// 2 options: 
				// 1) a JSON object
				// 2) a link that contains an embedded URL-encoded string
				if (queryStr.startsWith("{")) {
					try { // 1
						query = (BasicDBObject) com.mongodb.util.JSON.parse(queryStr);
					}
					catch (Exception e) { 
						throw new RuntimeException("Decode object query for post processing: " + queryStr);								
					}
				}
				else {
					Matcher m = _queryJsonExtractor.matcher(queryStr);
					if (m.find()) {
						try {
							query = (BasicDBObject) com.mongodb.util.JSON.parse(URLDecoder.decode(m.group(1), "UTF-8"));
							// Try to get community ids out of the JSON object
							BasicDBList commIds = (BasicDBList) query.remove("communityIds");
							if (null != commIds) {
								StringBuffer sb = new StringBuffer("*");
								for (Object o: commIds) {
									if (1 != sb.length()) {
										sb.append('|');
									}
									sb.append(o);
								}
								String[] commIdStrs = CustomApiUtils.getCommunityIds(job.submitterID.toString(), sb.toString());
								
								for (String commIdStr: commIdStrs) {
									ObjectId oid = new ObjectId(commIdStr);
									if (!src.getCommunityIds().contains(oid)) { // (ie don't add ids already in the source community)
										job.communityIds.add(oid);
									}
								}
							}//TESTED (postproc_workspace_test)
						}
						catch (Exception e) {
							throw new RuntimeException("Decode string query for post processing: " + e.getMessage(), e);
						}
					}
					else {
						throw new RuntimeException("Decode string query for post processing: " + queryStr);								
					}
				}////TESTED (postproc_workspace_test)
			}
		}	
		else { // (just an empty query)
			query = new BasicDBObject();
		}
		return query;
	}//TESTED (postproc_*_test)
	
	/////////////////////////////////////////////////////////////////////////////
	
	public static void handleGroupOverride(List<String> groupOverrideList, String groupOverrideRegex, CustomMapReduceJobPojo job, SourcePojo src)
	{
		if ((null != groupOverrideList) && !groupOverrideList.isEmpty())
		{
			StringBuffer sb = new StringBuffer('*');
			for (Object o: groupOverrideList) {
				if (0 != sb.length()) {
					sb.append('|');
				}
				sb.append(o);
			}
			String[] commIdStrs = CustomApiUtils.getCommunityIds(job.submitterID.toString(), sb.toString());
			for (String commIdStr: commIdStrs) {
				ObjectId oid = new ObjectId(commIdStr);
				if (!src.getCommunityIds().contains(oid)) { // (ie don't add ids already in the source community)
					job.communityIds.add(oid);
				}
			}
		}//TESTED (c/p) from "URL query" code above
		if ((null != groupOverrideRegex) && !groupOverrideRegex.isEmpty()) {
			String groupOverride = groupOverrideRegex;
			if (!groupOverride.startsWith("*")) {
				groupOverride = '*' + groupOverride;
			}
			String[] commIdStrs = CustomApiUtils.getCommunityIds(job.submitterID.toString(), groupOverride);
			for (String commIdStr: commIdStrs) {
				ObjectId oid = new ObjectId(commIdStr);
				if (!src.getCommunityIds().contains(oid)) { // (ie don't add ids already in the source community)
					job.communityIds.add(oid);
				}
			}
		}//TESTED (postproc_datastore_test)		
	}//TESTED (postproc_*_test)
	
	/////////////////////////////////////////////////////////////////////////////
	
	public static BasicDBObject handleCommonInFields(String queryStr, String fields, String tmin, String tmax, String srcTags, BasicDBObject query)
	{
		if (null == query) {
			query = new BasicDBObject();
		}
		if (null != queryStr) {
			query = (BasicDBObject) com.mongodb.util.JSON.parse(queryStr); 
		}
		if ((null != srcTags) && !srcTags.isEmpty()) {
			query.put("$srctags", srcTags);
		}// 
		if ((null != tmin) && !tmin.isEmpty()) {
			query.put("$tmin", tmin);
		}//
		if ((null != tmax) && !tmax.isEmpty()) {
			query.put("$tmax", tmax);
		}// 
		
		// fields ... can be just a list of fields, or can be a MongoDB object
		if ((null != fields) && !fields.isEmpty()) {
			fields = fields.trim();
			if (fields.startsWith("{")) {
				query.put("$fields", com.mongodb.util.JSON.parse(fields));
			}
			else if (fields.startsWith("-")) {
				BasicDBObject fieldsDbo = new BasicDBObject();
				String[] fieldsArray = fields.substring(1).split("\\s*,\\s*");
				for (String fieldStr: fieldsArray) fieldsDbo.put(fieldStr, 0);				
			}
			else {
				BasicDBObject fieldsDbo = new BasicDBObject();
				String[] fieldsArray = fields.split("\\s*,\\s*");
				for (String fieldStr: fieldsArray) fieldsDbo.put(fieldStr, 1);				
			}
		}
		
		return query;
	}//TESTED (postproc_*_test)
	
	/////////////////////////////////////////////////////////////////////////////
	
	//handleCommonOutFields?
}
