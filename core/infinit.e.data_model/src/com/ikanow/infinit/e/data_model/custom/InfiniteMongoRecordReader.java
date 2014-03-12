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
