/*******************************************************************************
 * Copyright 2012 The Infinit.e Open Source Project sponsored by IKANOW.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.ikanow.infinit.e.application.data_model;

import java.util.List;

import org.bson.types.ObjectId;

import com.google.gson.reflect.TypeToken;
import com.ikanow.infinit.e.data_model.store.BaseDbPojo;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePipelinePojo;

public class TestLogstashExtractorPojo extends BaseDbPojo { // copy of SourcePipelinePojo + SourcePipelinePojo.LogstashExtractorPojo with couple of extra fields
	// Standard static function for readability
	@SuppressWarnings("unchecked")
	static public TypeToken<List<TestLogstashExtractorPojo>> listType() { return new TypeToken<List<TestLogstashExtractorPojo>>(){}; } 

	public ObjectId _id; // (input/output - test/delete)
	
	public ObjectId deleteOnlyCommunityId; // (input - delete only)
	public Boolean deleteDocsOnly; // (input - delete only)
	
	public SourcePipelinePojo.LogstashExtractorPojo logstash; // (input only - not delete)
	public Boolean isAdmin; // (input only - not delete)
	public String sourceKey; // (input only - test/delete)
	public Long maxDocs; // (input only - not delete)
	public String forSlave; // (input only - delete)
	public Boolean distributed; // (input only - delete)
		
	public String error; // (output only - test only)	
}
