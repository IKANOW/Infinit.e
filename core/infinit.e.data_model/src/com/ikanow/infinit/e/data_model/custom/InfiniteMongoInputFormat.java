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

import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.bson.BSONObject;
import org.bson.types.ObjectId;

import com.ikanow.infinit.e.data_model.store.MongoDbManager;
import com.mongodb.BasicDBObject;
import com.mongodb.hadoop.MongoInputFormat;
import com.mongodb.hadoop.input.MongoInputSplit;

public class InfiniteMongoInputFormat extends MongoInputFormat
{
	// Just needed to insert a log message that lets me do local debugging...
	@Override
    public RecordReader<Object, BSONObject> createRecordReader( InputSplit split, TaskAttemptContext context ){
        if ( !( split instanceof MongoInputSplit ) )
            throw new IllegalStateException( "Creation of a new RecordReader requires a MongoInputSplit instance." );

        final MongoInputSplit mis = (MongoInputSplit) split;

        return new InfiniteMongoRecordReader( mis );
    }	
	
	@Override
	public List<InputSplit> getSplits(JobContext context) 
	{
		final Configuration hadoopConfiguration = context.getConfiguration();
		final InfiniteMongoConfig conf = new InfiniteMongoConfig( hadoopConfiguration );
		List<InputSplit> splits = InfiniteMongoSplitter.calculateSplits(conf);
		
		if ( conf.getOtherCollections() != null)
		{
			String[] otherCollections = conf.getOtherCollections().split("\\s*[|]\\s*");
			for (String otherCollection: otherCollections) {
				final Configuration existingConfiguration = context.getConfiguration();
				existingConfiguration.set("mongo.input.uri", otherCollection);
				final InfiniteMongoConfig anotherConf = new InfiniteMongoConfig( existingConfiguration );
				splits.addAll( InfiniteMongoSplitter.calculateSplits(anotherConf) );
			}
		}
		if ( conf.getSelfMerge() != null )
		{
			//check if we need to grab existing records and add them to the splits
			final Configuration existingConfiguration = context.getConfiguration();
			existingConfiguration.set("mongo.input.uri", conf.getSelfMerge());
			BasicDBObject query = new BasicDBObject();
			//add on this query to only get items previous to now if no reducer is specified (otherwise
			//we will leak any items we map on the first run back in before this split runs)
			if ( context.getNumReduceTasks() == 0) 				
				query.put("_id", new BasicDBObject(MongoDbManager.lt_, new ObjectId()));
			existingConfiguration.set("mongo.input.query",query.toString());
			final InfiniteMongoConfig existingConf = new InfiniteMongoConfig( existingConfiguration );
			splits.addAll( InfiniteMongoSplitter.calculateSplits(existingConf) );
		}
		return splits;
	}
}
