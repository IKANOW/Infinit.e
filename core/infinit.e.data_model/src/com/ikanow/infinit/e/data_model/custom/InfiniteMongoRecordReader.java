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
package com.ikanow.infinit.e.data_model.custom;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.TaskAttemptContext;

import com.mongodb.hadoop.input.MongoInputSplit;
import com.mongodb.hadoop.input.MongoRecordReader;

// Just needed to insert a log message that lets me do local debugging...

public class InfiniteMongoRecordReader extends MongoRecordReader {

    public InfiniteMongoRecordReader( MongoInputSplit split ){
    	super(split);
    }
    
    @Override
    public void initialize( InputSplit split, TaskAttemptContext context ){
    	super.initialize(split, context);
    	
		String jobName = context.getConfiguration().get("mapred.job.name", "unknown");
		log.info(jobName + ": new split");		
    }
	private static final Log log = LogFactory.getLog( InfiniteMongoRecordReader.class );
}
