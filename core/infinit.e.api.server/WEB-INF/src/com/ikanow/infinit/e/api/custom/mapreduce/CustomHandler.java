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
package com.ikanow.infinit.e.api.custom.mapreduce;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.bson.types.ObjectId;

import com.google.common.collect.Lists;
import com.ikanow.infinit.e.api.utils.RESTTools;
import com.ikanow.infinit.e.data_model.api.ResponsePojo;
import com.ikanow.infinit.e.data_model.api.ResponsePojo.ResponseObject;
import com.ikanow.infinit.e.data_model.api.custom.mapreduce.CustomMapReduceJobPojoApiMap;
import com.ikanow.infinit.e.data_model.api.custom.mapreduce.CustomMapReduceResultPojo;
import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.custom.mapreduce.CustomMapReduceJobPojo;
import com.ikanow.infinit.e.data_model.store.custom.mapreduce.CustomMapReduceJobPojo.SCHEDULE_FREQUENCY;
import com.ikanow.infinit.e.data_model.store.custom.mapreduce.CustomMapReduceJobPojo.INPUT_COLLECTIONS;
import com.ikanow.infinit.e.data_model.store.social.community.CommunityPojo;
import com.ikanow.infinit.e.data_model.store.social.person.PersonCommunityPojo;
import com.ikanow.infinit.e.data_model.store.social.person.PersonPojo;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MapReduceOutput;

public class CustomHandler 
{
	private static final Logger logger = Logger.getLogger(CustomHandler.class);
	
	/**
	 * Returns the results of a map reduce job if it has completed
	 * 
	 * @param userid
	 * @param jobid
	 * @return
	 */
	public ResponsePojo getJobResults(String userid, String jobid ) 
	{
		ResponsePojo rp = new ResponsePojo();		
		
		List<Object> searchTerms = new ArrayList<Object>();
		try
		{
			ObjectId jid = new ObjectId(jobid);
			searchTerms.add(new BasicDBObject("_id",jid));
		}
		catch (Exception ex)
		{
			//oid failed, will only add title
		}
		searchTerms.add(new BasicDBObject("jobtitle",jobid));
				
		try 
		{
			//find admin entry);
			DBObject dbo = DbManager.getCustom().getLookup().findOne(new BasicDBObject("$or",searchTerms.toArray()));			
			if ( dbo != null )
			{				
				CustomMapReduceJobPojo cmr = CustomMapReduceJobPojo.fromDb(dbo, CustomMapReduceJobPojo.class);
				//make sure user is allowed to see results
				if ( RESTTools.adminLookup(userid) || isInCommunity(cmr.communityIds, userid) )
				{
					//get results collection if done and return
					if ( cmr.lastCompletionTime != null )
					{
						//return the results
						DBCursor resultCursor = DbManager.getCollection("custommr", cmr.outputCollection).find();
						rp.setResponse(new ResponseObject("Custom Map Reduce Job Results",true,"Map reduce job completed at: " + cmr.lastCompletionTime));
						CustomMapReduceResultPojo cmrr = new CustomMapReduceResultPojo();
						cmrr.lastCompletionTime = cmr.lastCompletionTime;
						cmrr.results = resultCursor.toArray();
						rp.setData(cmrr);
					}
					else
					{
						rp.setResponse(new ResponseObject("Custom Map Reduce Job Results",false,"Map reduce job has not completed yet"));
					}
				}
				else
				{
					rp.setResponse(new ResponseObject("Custom Map Reduce Job Results",false,"User is not a member of communities with read rights"));
				}
			}
			else
			{
				rp.setResponse(new ResponseObject("Custom Map Reduce Job Results",false,"Job does not exist"));
			}
		} 
		catch (Exception e)
		{
			// If an exception occurs log the error
			logger.error("Exception Message: " + e.getMessage(), e);
			rp.setResponse(new ResponseObject("Custom Map Reduce Job Results",false,"error retrieving job info"));
		}
		return rp;
	}
	
	/**
	 * Return true if a user is in community or is their own self community, false otherwise
	 * 1. CommunityID = userid
	 * 2. community.members contains userid
	 * 
	 * @param communityIds
	 * @return
	 */
	private boolean isInCommunity(List<ObjectId> communityIds, String userid)
	{
		try
		{			
			BasicDBObject inQuery = new BasicDBObject("$in", communityIds.toArray());
			BasicDBObject query = new BasicDBObject("_id",inQuery);
			DBCursor dbc = DbManager.getSocial().getCommunity().find(query);			
			while (dbc.hasNext())
			{
				DBObject dbo = dbc.next();				
				CommunityPojo cp = CommunityPojo.fromDb(dbo, CommunityPojo.class);
				if ( (cp.getId().toString()).equals(userid) )
					return true; //Means this is this users personal group
				else if ( cp.isMember(new ObjectId(userid)))
					return true; //means this user is a member of this group
			}
		}
		catch(Exception ex)
		{
			//error looking up communities
		}
		return false; //if we never find the user return false
	}
	
	/**
	 * Checks if a user is part of all communities, or an admin
	 * 
	 * @param communityIds
	 * @param userid
	 * @return
	 */
	private boolean isInAllCommunities(List<ObjectId> communityIds, String userid)
	{		
		//test each community for membership
		try
		{
			BasicDBObject inQuery = new BasicDBObject("$in", communityIds.toArray());
			BasicDBObject query = new BasicDBObject("_id",inQuery);
			DBCursor dbc = DbManager.getSocial().getCommunity().find(query);
			while (dbc.hasNext())
			{
				DBObject dbo = dbc.next();				
				CommunityPojo cp = CommunityPojo.fromDb(dbo, CommunityPojo.class);
				//if this is not your self community AND you are not a member THEN you are not in all these communities
				if ( !((cp.getId().toString()).equals(userid)) && !(cp.isMember(new ObjectId(userid))) )
					return false;
			}
		}
		catch(Exception ex)
		{
			//error looking up communities
			return false;
		}
		return true;
	}
	
	/**
	 * Schedules a map reduce job to be ran, whether it be repeated or not
	 * 
	 * @param userid
	 * @return
	 */
	public ResponsePojo scheduleJob(String userid, String title, String desc, String communityIds, String jarURL, String nextRunTime, String schedFreq, String mapperClass, String reducerClass, String combinerClass, String query, String inputColl, String outputKey, String outputValue)
	{
		ResponsePojo rp = new ResponsePojo();
		List<ObjectId> commids = new ArrayList<ObjectId>(); 
		for ( String s : communityIds.split(","))
			commids.add(new ObjectId(s));
		//first make sure user is allowed to submit on behalf of the commids given
		if ( RESTTools.adminLookup(userid) || isInAllCommunities(commids, userid) )
		{
			CustomMapReduceJobPojo cmr = new CustomMapReduceJobPojo();
			//make sure user can use the input collection
			String inputCollection = getStandardInputCollection(inputColl);			
			if ( inputCollection != null )
			{
				cmr.isCustomTable = false;
			}
			else
			{
				inputCollection = getCustomInputCollection(inputColl, commids);
				cmr.isCustomTable = true;
			}
			if ( inputCollection != null)
			{				
				try 
				{					
					cmr.communityIds = commids;
					cmr._id = new ObjectId();
					cmr.jobtitle = title;
					cmr.jobdesc = desc;
					cmr.inputCollection = inputCollection;//getInputCollection(inputColl);
					cmr.jarURL = jarURL;
					cmr.outputCollection = cmr._id.toString();
					cmr.submitterID = new ObjectId(userid);
					long nextRun = Long.parseLong(nextRunTime);
					cmr.firstSchedule = new Date(nextRun);
					cmr.nextRunTime = nextRun;
					cmr.scheduleFreq = SCHEDULE_FREQUENCY.valueOf(schedFreq);
					cmr.mapper = mapperClass;
					cmr.reducer = reducerClass;
					cmr.outputKey = outputKey;
					cmr.outputValue = outputValue;
					if ( !combinerClass.equals("null"))
						cmr.combiner = combinerClass;
					if ( !query.equals("null"))
						cmr.query = query;
					
					//make sure title hasn't been used before
					DBObject dbo = DbManager.getCustom().getLookup().findOne(new BasicDBObject("jobtitle",title));
					if ( dbo == null )
					{
						DbManager.getCustom().getLookup().insert(cmr.toDb());												
						rp.setResponse(new ResponseObject("Schedule MapReduce Job",true,"Job scheduled successfully, will run on: " + new Date(nextRun).toString()));
						rp.setData(cmr._id.toString(), null);
					}
					else
					{
						rp.setResponse(new ResponseObject("Schedule MapReduce Job",false,"A job already matches that title, please choose another title"));
					}
				} 
				catch (IllegalArgumentException e)
				{
					logger.error("Exception Message: " + e.getMessage(), e);
					rp.setResponse(new ResponseObject("Schedule MapReduce Job",false,"No enum matching scheduled frequency, try NONE, DAILY, WEEKLY, MONTHLY"));
				}
				catch (Exception e)
				{
					// If an exception occurs log the error
					logger.error("Exception Message: " + e.getMessage(), e);
					rp.setResponse(new ResponseObject("Schedule MapReduce Job",false,"error scheduling job"));
				}					
			}
			else
			{
				rp.setResponse(new ResponseObject("Schedule MapReduce Job",false,"You are not allowed to use the given input collection."));
			}
		}
		else
		{
			rp.setResponse(new ResponseObject("Schedule MapReduce Job",false,"You are not an admin or member of all the communities given."));
		}
		return rp;
	}
	
	private String getStandardInputCollection(String inputColl)
	{
		try
		{
			INPUT_COLLECTIONS input = INPUT_COLLECTIONS.valueOf(inputColl);
			if ( input == INPUT_COLLECTIONS.IRS_WORKFORCE )
				return "irs.workforce";
			else if ( input == INPUT_COLLECTIONS.DOC_METADATA )
				return "doc_metadata.metadata";
		}
		catch (Exception ex)
		{
			//was not one of the standard collections
		}
		return null;
	}

	private String getCustomInputCollection(String inputColl, List<ObjectId> communityIds) 
	{
		String output = null;		
		//if not one of the standard collections, see if its in custommr.customlookup
		List<Object> searchTerms = new ArrayList<Object>();
		try
		{
			ObjectId jid = new ObjectId(inputColl);
			searchTerms.add(new BasicDBObject("_id",jid));
		}
		catch (Exception e)
		{
			//oid failed, will only add title
		}
		searchTerms.add(new BasicDBObject("jobtitle",inputColl));
				
		try 
		{
			//find admin entry
			BasicDBObject query = new BasicDBObject("communityIds",new BasicDBObject("$in",communityIds.toArray()));
			query.append("$or",searchTerms.toArray());
			DBObject dbo = DbManager.getCustom().getLookup().findOne(query);
			if ( dbo != null )
			{
				CustomMapReduceJobPojo cmr = CustomMapReduceJobPojo.fromDb(dbo, CustomMapReduceJobPojo.class);
				output = "custommr." + cmr.outputCollection;
			}
		}
		catch (Exception exc)
		{
			//no tables were found leave output null
		}
				
		return output;
	}

	/**
	 * Returns all jobs that you have access to by checking for matching communities
	 * 
	 * @param userid
	 * @return
	 */
	public ResponsePojo getAllJobs(String userid) 
	{
		ResponsePojo rp = new ResponsePojo();		
		try 
		{
			DBObject dbo = DbManager.getSocial().getPerson().findOne(new BasicDBObject("_id",new ObjectId(userid)));
			if (dbo != null )
			{
				PersonPojo pp = PersonPojo.fromDb(dbo, PersonPojo.class);
				List<ObjectId> communities = new ArrayList<ObjectId>();
				for ( PersonCommunityPojo pcp : pp.getCommunities())
					communities.add(pcp.get_id());
				BasicDBObject commquery = new BasicDBObject("communityIds", new BasicDBObject("$in",communities.toArray()));
				DBCursor dbc = DbManager.getCustom().getLookup().find(commquery);				
				rp.setResponse(new ResponseObject("Custom Map Reduce Get Jobs",true,"succesfully returned jobs"));
				rp.setData(CustomMapReduceJobPojo.listFromDb(dbc, CustomMapReduceJobPojo.listType()), new CustomMapReduceJobPojoApiMap());
			}
			else
			{
				rp.setResponse(new ResponseObject("Custom Map Reduce Get Jobs",false,"error retrieving users communities"));
			}			
		} 
		catch (Exception e)
		{
			// If an exception occurs log the error
			logger.error("Exception Message: " + e.getMessage(), e);
			rp.setResponse(new ResponseObject("Custom Map Reduce Get Jobs",false,"error retrieving jobs"));
		}
		return rp;
	}

	public ResponsePojo runMapReduce(String userid, String collection, String map, String reduce, String query)
	{
		ResponsePojo rp = new ResponsePojo();
		try
		{
			//get user communities
			DBObject dboperson = DbManager.getSocial().getPerson().findOne(new BasicDBObject("_id", new ObjectId(userid)));
			if ( dboperson != null )
			{
				PersonPojo pp = PersonPojo.fromDb(dboperson, PersonPojo.class);
				List<ObjectId> communityIds = new ArrayList<ObjectId>();
				for ( PersonCommunityPojo pcp : pp.getCommunities() )
					communityIds.add( pcp.get_id() );
				
				DBObject dbQuery = (DBObject) com.mongodb.util.JSON.parse(query);
				
				String inputCol = getStandardInputCollection(collection);
				if ( inputCol == null ) //was not a	standard collection, try custom
					inputCol = getCustomInputCollection(collection,communityIds);
				else //was a standard, add community ids to query
					dbQuery.put("communityId", new BasicDBObject("$in",communityIds.toArray()));
				
				if (inputCol != null )
				{
					String[] collString = inputCol.split("\\.");		
					String db = collString[0];
					String coll = collString[1];
					MapReduceOutput mro = DbManager.getCollection(db, coll).mapReduce(map, reduce, "{out: { inline : 1}}", dbQuery);
					Iterable<DBObject> results = mro.results();
					List<DBObject> resultList = Lists.newArrayList(results);
					rp.setResponse(new ResponseObject("Custom Map Reduce Run",true,"Map Reduce ran successfully"));
					rp.setData(resultList.toString(),null);
				}
				else
				{
					rp.setResponse(new ResponseObject("Custom Map Reduce Run",false,"you dont have access to the input collection"));
				}
			}
			else
			{
				rp.setResponse(new ResponseObject("Custom Map Reduce Run",false,"user did not exist"));
			}						
		}
		catch (Exception ex)
		{
			rp.setResponse(new ResponseObject("Custom Map Reduce Run",false,"error running map reduce"));
		}
		return rp;
	}
	
	/*
	public ResponsePojo runAggregation(String userid, String key, String cond, String initial, String reduce, String collection) 
	{
		//FINALize is in the new java driver it looks like, not our current version
		//String finalize = null; 
		ResponsePojo rp = new ResponsePojo();	
		try
		{
			CollectionManager cm = new CollectionManager();
			//get user communities
			DBObject dboperson = cm.getPerson().findOne(new BasicDBObject("_id", new ObjectId(userid)));
			if ( dboperson != null )
			{
				GsonBuilder gb = GsonTypeAdapter.getGsonBuilder(GsonTypeAdapter.GsonAdapterType.DESERIALIZER);
				Gson gson = gb.create();
				PersonPojo pp = gson.fromJson(dboperson.toString(), PersonPojo.class);
				List<ObjectId> communityIds = new ArrayList<ObjectId>();
				for ( PersonCommunityPojo pcp : pp.getCommunities() )
					communityIds.add( pcp.get_id() );
				//parse statements
				DBObject dbKey = (DBObject) com.mongodb.util.JSON.parse(key);
				DBObject dbCond = (DBObject) com.mongodb.util.JSON.parse(cond);
				DBObject dbInitial = (DBObject) com.mongodb.util.JSON.parse(initial);
				//add in communities to condition statement
				dbCond.put("communityId", new BasicDBObject("$in",communityIds.toArray()));			
				//run command
				String inputCol = getInputCollection(collection);
				DBObject dbo = cm.getCustomCollection(inputCol).group(dbKey, dbCond, dbInitial, reduce);
				if ( dbo != null )
				{
					rp.setResponse(new ResponseObject("Custom Map Reduce Run Aggregation",true,"Aggregation ran successfully"));
					rp.setData(dbo.toString());
				}
				else //need to test, dont know if results will ever be empty if it ran
				{
					rp.setResponse(new ResponseObject("Custom Map Reduce Run Aggregation",false,"aggregation returned no results"));
				}
			}
			else
			{
				rp.setResponse(new ResponseObject("Custom Map Reduce Run Aggregation",false,"user did not exist"));
			}			
		}
		catch (Exception ex)
		{
			rp.setResponse(new ResponseObject("Custom Map Reduce Run Aggregation",false,"error running aggregation, could not parse db objects maybe?"));
		}
		
		return rp;
	}
	*/
}

