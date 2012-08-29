package com.ikanow.infinit.e.data_model.custom;

import org.apache.hadoop.conf.Configuration;

import com.mongodb.hadoop.util.MongoConfigUtil;

public class InfiniteMongoConfigUtil extends MongoConfigUtil
{    
    /**
     * Infinite added variables for splitting
     */
    public static final String MAX_SPLITS = "max.splits";
    public static final String MAX_DOCS_PER_SPLIT = "max.docs.per.split";
    
    public static int getMaxSplits( Configuration conf ){
    	return conf.getInt(MAX_SPLITS, 0);
    }
    
    public static void setMaxSplits(Configuration conf, int maxSplits)
    {
    	conf.setInt(MAX_SPLITS, maxSplits);
    }
    
    public static int getMaxDocsPerSplit( Configuration conf ){
    	return conf.getInt(MAX_DOCS_PER_SPLIT, 0);
    }
    
    public static void setMaxDocsPerSplit(Configuration conf, int maxDocsPerSplit)
    {
    	conf.setInt(MAX_DOCS_PER_SPLIT, maxDocsPerSplit);
    }
}
