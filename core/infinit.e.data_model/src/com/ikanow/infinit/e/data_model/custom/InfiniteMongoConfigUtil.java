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

import org.apache.hadoop.conf.Configuration;

import com.mongodb.BasicDBList;
import com.mongodb.DBObject;
import com.mongodb.hadoop.util.MongoConfigUtil;

public class InfiniteMongoConfigUtil extends MongoConfigUtil
{    
    /**
     * Infinite added variables for splitting
     */
    public static final String MAX_SPLITS = "max.splits";
    public static final String MAX_DOCS_PER_SPLIT = "max.docs.per.split";
    public static final String UPDATE_MODE = "update.incremental";
    public static final String SOURCE_TAGS = "infinit.e.source.tags.filter";
    public static final String CACHE_LIST = "infinit.e.cache.list";
    public static final String SELF_MERGE = "infinit.e.selfMerge";
    
    public static int getMaxSplits( Configuration conf ){
    	return conf.getInt(MAX_SPLITS, 0);
    }
    
    public static void setMaxSplits(Configuration conf, int maxSplits)
    {
    	conf.setInt(MAX_SPLITS, maxSplits);
    }
    
    public static boolean getUpdateModeIncremental( Configuration conf ){
    	return conf.getBoolean(UPDATE_MODE, false);
    }//TODO (INF-2126) TOTEST
    
    public static int getMaxDocsPerSplit( Configuration conf ){
    	return conf.getInt(MAX_DOCS_PER_SPLIT, 0);
    }
    
    public static void setMaxDocsPerSplit(Configuration conf, int maxDocsPerSplit)
    {
    	conf.setInt(MAX_DOCS_PER_SPLIT, maxDocsPerSplit);
    }

    public static DBObject getSourceTags(Configuration conf)
    {
    	return (DBObject) com.mongodb.util.JSON.parse(conf.get(SOURCE_TAGS, null));
    }
    public static void setSourceTags(Configuration conf, DBObject tags)
    {
    	conf.setStrings(SOURCE_TAGS, tags.toString());
    }
    
    public static BasicDBList getCacheList(Configuration conf)
    {
    	return (BasicDBList) com.mongodb.util.JSON.parse(conf.get(CACHE_LIST, "[]"));
    }
    public static void setCacheList(Configuration conf, BasicDBList cacheList)
    {
    	conf.setStrings(CACHE_LIST, cacheList.toString());
    }
    
    public static String getSelfMerge(Configuration conf)
    {
    	return conf.get(SELF_MERGE);
    }
    public static void setSelfMerge(Configuration conf, String selfMerge)
    {
    	conf.set(SELF_MERGE, selfMerge);
    }
    
}
