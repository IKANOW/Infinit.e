package com.ikanow.infinit.e.data_model.custom;

import org.apache.hadoop.conf.Configuration;

import com.mongodb.hadoop.MongoConfig;

public class InfiniteMongoConfig extends MongoConfig
{	
	public InfiniteMongoConfig(Configuration conf)
	{
		super(conf);		
	}
	
	public int getMaxSplits()
	{
		return InfiniteMongoConfigUtil.getMaxSplits( _conf );
	}

	public void setMaxSplits( int max_splits ){
	    InfiniteMongoConfigUtil.setMaxSplits( _conf, max_splits );
	}
	
	public int getMaxDocsPerSplit(){
	    return InfiniteMongoConfigUtil.getMaxDocsPerSplit( _conf );
	}
	
	public void setMaxDocsPerSplit( int max_docs_per_split ){
	    InfiniteMongoConfigUtil.setMaxDocsPerSplit( _conf, max_docs_per_split );
	}
}
