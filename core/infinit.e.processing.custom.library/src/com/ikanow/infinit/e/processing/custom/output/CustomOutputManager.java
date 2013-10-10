/*******************************************************************************
 * Copyright 2012, The Infinit.e Open Source Project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.ikanow.infinit.e.processing.custom.output;

import java.io.IOException;
import java.util.Date;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import org.xml.sax.SAXException;

import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.MongoDbManager;
import com.ikanow.infinit.e.data_model.store.MongoDbUtil;
import com.ikanow.infinit.e.data_model.store.custom.mapreduce.CustomMapReduceJobPojo;
import com.ikanow.infinit.e.processing.custom.utils.InfiniteHadoopUtils;
import com.ikanow.infinit.e.processing.custom.utils.PropertiesManager;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

public class CustomOutputManager {

	private static Logger _logger = Logger.getLogger(CustomOutputManager.class);
	
	/**
	 * Attempt to shard the output collection.  If the collection
	 * is already sharded it will just spit back an error which
	 * is fine.
	 * 
	 * @param outputCollection
	 */
	public static void shardOutputCollection(CustomMapReduceJobPojo job) 
	{
		//enable sharding for the custommr db incase it hasn't been
		DbManager.getDB("admin").command(new BasicDBObject("enablesharding", job.getOutputDatabase()));
		//enable sharding for the output collection (does nothing if already sharded)
		if ( job.outputCollection != null )
		{
			BasicDBObject command = new BasicDBObject("shardcollection", job.getOutputDatabase() + "." + job.outputCollection);
			command.append("key", new BasicDBObject("_id", 1));
			DbManager.getDB("admin").command(command);
			
			if ((null != job.incrementalMode) && job.incrementalMode) {
				DBCollection target = DbManager.getCollection(job.getOutputDatabase(), job.outputCollection);
				target.ensureIndex(new BasicDBObject("key", 1));
				target.ensureIndex(new BasicDBObject("_updateId", 1));
			}//TODO (INF-2126): TOTEST
				
		}
		//enable sharding on temp output collection (only needed in non-append mode)
		if ((null == job.appendResults) || !job.appendResults) { // then we're going to write into temp
			
			// Need to do this in case we're swapping from append to !append
			try {
				DbManager.getCollection(job.getOutputDatabase(), job.outputCollectionTemp).drop();
			}
			catch (Exception e) {} // That's fine, it probably just doesn't exist yet...
			if ( job.outputCollectionTemp != null )
			{
				BasicDBObject command1 = new BasicDBObject("shardcollection", job.getOutputDatabase() + "." + job.outputCollectionTemp);
				command1.append("key", new BasicDBObject("_id", 1));
				DbManager.getDB("admin").command(command1);
			}
		}
	}
	/**
	 * Moves the output of a job from output_tmp to output and deletes
	 * the tmp collection.
	 * 
	 * @param cmr
	 * @throws IOException 
	 * @throws ParserConfigurationException 
	 * @throws SAXException 
	 */
	public static void moveTempOutput(CustomMapReduceJobPojo cmr, PropertiesManager props_custom) throws IOException, SAXException, ParserConfigurationException
	{
		// If we are an export job then move files:
		InfiniteHadoopUtils.bringTempOutputToFront(cmr, props_custom);
			// (the rest of this will just do nothing) 
		
		/**
		 * Atomic plan:
		 * If not append, move customlookup pointer to tmp collection, drop old collection.
		 * If append, set sync flag (find/mod), move results from tmp to old, unset sync flag.
		 * 
		 */	
		//step1 build out any of the post proc arguments
		BasicDBObject postProcObject = null;
		boolean limitAllData = true;
		boolean hasSort = false;
		int limit = 0;
		BasicDBObject sort = new BasicDBObject();
		int sortDir = 1;
		String sortField = null; 
		try
		{
			///////////////////////////////////////////////////////////////////////
			// POST PROCESSING CONFIGURATION:
			postProcObject = (BasicDBObject) com.mongodb.util.JSON.parse(InfiniteHadoopUtils.getQueryOrProcessing(cmr.query, InfiniteHadoopUtils.QuerySpec.POSTPROC));
			if ( postProcObject != null )
			{
				limitAllData = postProcObject.getBoolean("limitAllData", false);
				limit = postProcObject.getInt("limit", 0);
				sortField = (String) postProcObject.get("sortField");
				sortDir = postProcObject.getInt("sortDirection", 1);
				if ((limit > 0) && (null == sortField)) {
					sortField = "_id";					
				}
			}//TESTED
		}
		catch (Exception ex)
		{
			_logger.info("job_error_post_proc_title=" + cmr.jobtitle + " job_error_post_proc_id=" + cmr._id.toString() + " job_error_post_proc_message="+InfiniteHadoopUtils.createExceptionMessage(ex));
		}
		
		///////////////////////////////////////////////////////////////////////
		// ACTUAL POST PROCESSING:
		
		//step 2a if not appending results then work on temp collection and swap to main
		if ( (null == cmr.appendResults) || !cmr.appendResults ) //format temp then change lookup pointer to temp collection
		{
			if (limit > 0) {
				BasicDBObject query = new BasicDBObject();
				limitCollection(DbManager.getCollection(cmr.getOutputDatabase(), cmr.outputCollectionTemp), query, sortField, sortDir, limit);
			}//TESTED
			else if (hasSort) {
				DbManager.getCollection(cmr.getOutputDatabase(), cmr.outputCollectionTemp).ensureIndex(sort);				
			}
			
			//swap the output collections
			BasicDBObject notappendupdates = new BasicDBObject(CustomMapReduceJobPojo.outputCollection_, cmr.outputCollectionTemp);
			notappendupdates.append(CustomMapReduceJobPojo.outputCollectionTemp_, cmr.outputCollection);
			DbManager.getCustom().getLookup().findAndModify(new BasicDBObject(CustomMapReduceJobPojo._id_, cmr._id), 
																new BasicDBObject(MongoDbManager.set_, notappendupdates));						
			String temp = cmr.outputCollectionTemp;
			cmr.outputCollectionTemp = cmr.outputCollection;
			cmr.outputCollection = temp;

			//step3a clean up temp output collection so we can use it again (non-append only...)
			// (drop it, removing chunks)
			try {
				DbManager.getCollection(cmr.getOutputDatabase(), cmr.outputCollectionTemp).drop();
			}
			catch (Exception e) {} // That's fine, it probably just doesn't exist yet...
		}
		else //step 2b if appending results then drop modified results in output collection
		{
			DbManager.getCustom().getLookup().findAndModify(new BasicDBObject(CustomMapReduceJobPojo._id_, cmr._id), 
															new BasicDBObject(MongoDbManager.set_, new BasicDBObject("isUpdatingOutput", true)));
			try {
				//remove any aged out results
				if ( (null != cmr.appendAgeOutInDays) && cmr.appendAgeOutInDays > 0 )
				{
					//remove any results that have aged out
					long ageOutMS = (long) (cmr.appendAgeOutInDays*InfiniteHadoopUtils.MS_IN_DAY);
					Date lastAgeOut = new Date(((new Date()).getTime() - ageOutMS));
					BasicDBObject queryTerm = new BasicDBObject(MongoDbManager.lt_,new ObjectId(lastAgeOut));
					
					if ((null == cmr.incrementalMode) || !cmr.incrementalMode) {
						DbManager.getCollection(cmr.getOutputDatabase(), cmr.outputCollection).remove(new BasicDBObject("_id", queryTerm));
					}
					else { // if in incremental mode then use _updateId instead
						DbManager.getCollection(cmr.getOutputDatabase(), cmr.outputCollection).remove(new BasicDBObject("_updateId", queryTerm));
					}//TODO (INF-2126) TOTEST
				}
				
				if (limit > 0) {
					BasicDBObject query = new BasicDBObject();
					if (!limitAllData) {
						if (null != cmr.lastCompletionTime) {
							ObjectId olderRecords = new ObjectId(cmr.lastCompletionTime);
							query.put("_id", new BasicDBObject(MongoDbManager.gt_, olderRecords));
						}
					}
					limitCollection(DbManager.getCollection(cmr.getOutputDatabase(), cmr.outputCollection), query, sortField, sortDir, limit);
				}
				else if (hasSort) {
					DbManager.getCollection(cmr.getOutputDatabase(), cmr.outputCollection).ensureIndex(sort);				
				}
			}
			finally {
				DbManager.getCustom().getLookup().findAndModify(new BasicDBObject(CustomMapReduceJobPojo._id_, cmr._id), 
											new BasicDBObject(MongoDbManager.set_, new BasicDBObject("isUpdatingOutput", false)));
			}
		}
	}	
	
	/**
	 * Returns the current output collection for a certain jobid
	 * This is usually used when a custom input collection is set for a job because
	 * the output collection of another job can change regularly.
	 * 
	 * @param jobid
	 * @return
	 */
	public static String getCustomDbAndCollection(String jobid)
	{
		DBObject dbo = DbManager.getCustom().getLookup().findOne(new BasicDBObject(CustomMapReduceJobPojo._id_, new ObjectId(jobid)));
		if ( dbo != null )
		{
			CustomMapReduceJobPojo cmr = CustomMapReduceJobPojo.fromDb(dbo, CustomMapReduceJobPojo.class);
			return cmr.getOutputDatabase() + "." + cmr.outputCollection;
		}
		return null;
	}
	
	/**
	 * @param collToLimit - outputCollection or outputCollectionTemp depending on mode
	 * @param query - empty or only considers new 
	 * @param sortField - the field to sort on
	 * @param sortDir - the direction
	 * @param limit - the limit (either global or just on new docs)
	 */
	private static void limitCollection(DBCollection collToLimit, BasicDBObject query, String sortField, int sortDir, int limit) {
		BasicDBObject sort = new BasicDBObject(sortField, sortDir); BasicDBObject inverseSort = new BasicDBObject(sortField, -sortDir);

		collToLimit.ensureIndex(sort);
		BasicDBObject fields = new BasicDBObject(sortField, 1);
		DBCursor cursor = null;
		long current = collToLimit.count();
		// Efficiently find the worst object to keep:
		if ((current - limit) > (limit - 2)) {
			cursor = collToLimit.find(query, fields).sort(sort).skip(limit - 1).limit(1); // ie last object
		}//TESTED
		else if (current >= limit) {
			cursor = collToLimit.find(query, fields).sort(inverseSort).skip((int)((current - limit))).limit(1); 
				// ie first bad object + 1
		}//TESTED

		String dir = sortDir > 0 ? MongoDbManager.gt_ : MongoDbManager.lt_;
		if ((cursor != null) && cursor.hasNext()) {
			BasicDBObject worstToKeep = (BasicDBObject) cursor.next();			
			query.put(sortField, new BasicDBObject(dir, MongoDbUtil.getProperty(worstToKeep, sortField)));
			collToLimit.remove(query);
		}
		
	}//TESTED
	
}
