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
	public String jobtitle = null;
	public String jobdesc = null;
	public ObjectId submitterID = null;
	public List<ObjectId> communityIds = null;
	public String jarURL = null;
	public String inputCollection = null;
	public String outputCollection = null;
	public Date lastCompletionTime = null;
	public long nextRunTime = Long.MAX_VALUE;
	public String jobidS = null;
	public int jobidN = 0;
	public SCHEDULE_FREQUENCY scheduleFreq = SCHEDULE_FREQUENCY.NONE;	
	public Date firstSchedule = null;
	public int timesRan = 0;
	public int timesFailed = 0;
	public String errorMessage = null;
	public String tempConfigXMLLocation = null;
	public String tempJarLocation = null;
	public boolean isCustomTable = false;
	public Date lastRunTime = null;
	public String mapper;
	public String reducer;
	public String combiner = "";
	public String query = "";
	public String outputKey;
	public String outputValue;
	public float mapProgress;
	public float reduceProgress;
	public Boolean appendResults; // (defaults to null)
	public Double appendAgeOutInDays;
	public Set<ObjectId> jobDependencies = new HashSet<ObjectId>(); //jobs this one depends on to run
	public Set<ObjectId> waitingOn = new HashSet<ObjectId>(); //jobs until this one can run (temp list)
	public boolean isUpdatingOutput = false;
	public String outputCollectionTemp = null;
	public String arguments = null; //user arguments that can be added and will be sent to the job
	
	
	public enum SCHEDULE_FREQUENCY
	{
		NONE,
		DAILY,
		WEEKLY,
		MONTHLY;
	}
	
	public enum INPUT_COLLECTIONS
	{	
		DOC_METADATA;
	}
}


