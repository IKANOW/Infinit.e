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
package com.ikanow.infinit.e.data_model.store.custom.mapreduce;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bson.types.ObjectId;

import com.google.gson.reflect.TypeToken;
import com.ikanow.infinit.e.data_model.store.BaseDbPojo;

public class CustomMapReduceJobPojo extends BaseDbPojo
{
	// Standard static function for readability
	@SuppressWarnings("unchecked")
	static public TypeToken<List<CustomMapReduceJobPojo>> listType() { return new TypeToken<List<CustomMapReduceJobPojo>>(){}; }
	
	public ObjectId _id = null;
	public static final String _id_ = "_id";
	public String jobtitle = null;
	public static final String jobtitle_ = "jobtitle";
	public String jobdesc = null;
	public static final String jobdesc_ = "jobdesc";
	public ObjectId submitterID = null;
	public static final String submitterID_ = "submitterID";
	public List<ObjectId> communityIds = null;
	public static final String communityIds_ = "communityIds";
	public String jarURL = null;
	public static final String jarURL_ = "jarURL";
	public String inputCollection = null;
	public static final String inputCollection_ = "inputCollection";
	private String outputDatabase = null; // (if null then put things in custommr)
	public static final String outputDatabase_ = "outputDatabase";
	public String outputCollection = null;
	public static final String outputCollection_ = "outputCollection";
	public Date lastCompletionTime = null;
	public static final String lastCompletionTime_ = "lastCompletionTime";
	public long nextRunTime = Long.MAX_VALUE;
	public static final String nextRunTime_ = "nextRunTime";
	public String jobidS = null;
	public static final String jobidS_ = "jobidS";
	public int jobidN = 0;
	public static final String jobidN_ = "jobidN";
	public SCHEDULE_FREQUENCY scheduleFreq = SCHEDULE_FREQUENCY.NONE;	
	public static final String scheduleFreq_ = "scheduleFreq";
	public Date firstSchedule = null;
	public static final String firstSchedule_ = "firstSchedule";
	public int timesRan = 0;
	public static final String timesRan_ = "timesRan";
	public int timesFailed = 0;
	public static final String timesFailed_ = "timesFailed";
	public String errorMessage = null;
	public static final String errorMessage_ = "errorMessage";
	public String tempConfigXMLLocation = null;
	public static final String tempConfigXMLLocation_ = "tempConfigXMLLocation";
	public String tempJarLocation = null;
	public static final String tempJarLocation_ = "tempJarLocation";
	public boolean isCustomTable = false;
	public static final String isCustomTable_ = "isCustomTable";
	public Date lastRunTime = null;
	public static final String lastRunTime_ = "lastRunTime";
	public String mapper;
	public static final String mapper_ = "mapper";
	public String reducer;
	public static final String reducer_ = "reducer";
	public String combiner;
	public static final String combiner_ = "combiner";
	public String query;
	public static final String query_ = "query";
	public String outputKey;
	public static final String outputKey_ = "outputKey";
	public String outputValue;
	public static final String outputValue_ = "outputValue";
	public float mapProgress;
	public static final String mapProgress_ = "mapProgress";
	public float reduceProgress;
	public static final String reduceProgress_ = "reduceProgress";
	public Boolean appendResults; // (defaults to null)
	public static final String appendResults_ = "appendResults";
	public Double appendAgeOutInDays;
	public static final String incrementalMode_ = "incrementalMode";
	public Boolean incrementalMode;
	public static final String appendAgeOutInDays_ = "appendAgeOutInDays";
	public Set<ObjectId> jobDependencies = new HashSet<ObjectId>(); //jobs this one depends on to run
	public static final String jobDependencies_ = "jobDependencies";
	public Set<ObjectId> waitingOn = new HashSet<ObjectId>(); //jobs until this one can run (temp list)
	public static final String waitingOn_ = "waitingOn";
	public boolean isUpdatingOutput = false;
	public static final String isUpdatingOutput_ = "isUpdatingOutput";
	public String outputCollectionTemp = null;
	public static final String outputCollectionTemp_ = "outputCollectionTemp";
	public String arguments = null; //user arguments that can be added and will be sent to the job
	public static final String arguments_ = "arguments";
	public Boolean exportToHdfs; // If (present AND true), the output is written to hdfs://user/tomcat
	public static final String exportToHdfs_ = "exportToHdfs";
	// (need to start moving these guys to private...)
	public List<String> tempErrors = null; // (get copied into the message string on completion/error)
	public static final String tempErrors_ = "tempErrors";
	
	public static final String selfMerge_ = "selfMerge";
	public Boolean selfMerge;
	
	public enum SCHEDULE_FREQUENCY
	{
		NONE,
		HOURLY,
		DAILY,
		WEEKLY,
		MONTHLY;
	}
	
	public enum INPUT_COLLECTIONS
	{	
		DOC_METADATA, DOC_CONTENT, FEATURE_ENTITIES, FEATURE_ASSOCS, FILESYSTEM, FEATURE_TEMPORAL;
	}

	// Getters and setters

	public void setOutputDatabase(String outputDatabase) {
		this.outputDatabase = outputDatabase;
	}

	public String getOutputDatabase() {
		if (null == outputDatabase) {
			return "custommr";
		}
		return outputDatabase;
	}
}


