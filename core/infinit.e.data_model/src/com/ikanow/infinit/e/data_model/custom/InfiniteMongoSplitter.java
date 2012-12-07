package com.ikanow.infinit.e.data_model.custom;

import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.log4j.Logger;

import com.mongodb.DBCollection;
import com.mongodb.MongoURI;
import com.mongodb.hadoop.util.MongoSplitter;

public class InfiniteMongoSplitter
{
	private static Logger _logger = Logger.getLogger(InfiniteMongoSplitter.class);			
	
	/**
	 * Checks if the new params MAX_SPLITS and MAX_DOCS_PER_SPLIT are set
	 * in the config.  If they are it will use those to do splits via limit/skip
	 * otherwise it will call the previous chunking splitter in MongoSplitter.
	 * 
	 * @param conf
	 * @return
	 */
	public static List<InputSplit> calculateSplits( InfiniteMongoConfig conf) 
	{
		MongoURI uri = conf.getInputURI();
		DBCollection coll = InfiniteMongoConfigUtil.getCollection(uri);
		int count = coll.find(conf.getQuery()).count();
		//if maxdocssplit and maxsplits is set and there are less documents than splits*docspersplit then use the new splitter
		//otherwise use the old splitter
		if ( conf.getMaxDocsPerSplit() > 0 && conf.getMaxSplits() > 0 && ( count < (conf.getMaxSplits()*conf.getMaxDocsPerSplit()) ) )
		{
			_logger.debug("Calculating splits manually");
			int splits_needed = (count/conf.getMaxDocsPerSplit()) + 1;
			return calculateManualSplits(conf, splits_needed, conf.getMaxDocsPerSplit(), coll);
		}
		else
		{
			_logger.debug("Calculating splits via mongo-hadoop");
			return MongoSplitter.calculateSplits(conf);						
		}
	}
	
	/**
	 * Creates numSplits amount of splits with limit items in each split
	 * using limit and skip to determine the sets
	 * 
	 * @param conf
	 * @param numSplits
	 * @param count
	 * @param coll
	 * @return
	 */
	private static List<InputSplit> calculateManualSplits(InfiniteMongoConfig conf, int numSplits, int limit, DBCollection coll)
	{
		final List<InputSplit> splits = new ArrayList<InputSplit>(numSplits);
		_logger.debug("using a limit of " + limit + " for "+numSplits+" splits");
		for ( int i = 0; i < numSplits; i++ )
		{
			splits.add(new InfiniteMongoInputSplit(conf.getInputURI(), conf.getInputKey(), conf.getQuery(), conf.getFields(), conf.getSort(), limit, i*limit, conf.isNoTimeout()));
		}
		return splits;
	}		
}
