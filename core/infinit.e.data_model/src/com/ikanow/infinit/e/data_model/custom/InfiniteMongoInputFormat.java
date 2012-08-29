package com.ikanow.infinit.e.data_model.custom;

import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.JobContext;

import com.mongodb.hadoop.MongoInputFormat;

public class InfiniteMongoInputFormat extends MongoInputFormat
{	
	@Override
	public List<InputSplit> getSplits(JobContext context) 
	{
		final Configuration hadoopConfiguration = context.getConfiguration();
		final InfiniteMongoConfig conf = new InfiniteMongoConfig( hadoopConfiguration );
		return InfiniteMongoSplitter.calculateSplits(conf);
	}
}
