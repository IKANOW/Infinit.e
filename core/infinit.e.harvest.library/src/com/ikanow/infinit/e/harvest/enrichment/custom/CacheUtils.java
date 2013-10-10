package com.ikanow.infinit.e.harvest.enrichment.custom;

import java.util.Map;
import java.util.Set;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.bson.types.ObjectId;
import org.json.JSONException;
import org.json.JSONObject;

import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.MongoDbManager;
import com.ikanow.infinit.e.data_model.store.social.sharing.SharePojo;
import com.ikanow.infinit.e.harvest.HarvestContext;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

public class CacheUtils 
{
	/**
	 * Creates a default map of caches and then tries to grab a share for each id given and create a 
	 * cache with that name.
	 * 
	 * @param caches
	 * @param engine
	 * @param communityIds
	 * @param _context2
	 * @throws ScriptException
	 * @throws JSONException 
	 */	
	public static void addJSONCachesToEngine(Map<String, ObjectId> caches, ScriptEngine engine, Set<ObjectId> communityIds, HarvestContext _context2) throws ScriptException, JSONException 
	{
		if ( null != engine )
		{
			engine.eval("_cache = {}");
			//get json from shares
			for ( String cacheName : caches.keySet())
			{
				ObjectId shareId = caches.get(cacheName);
				String json = getShareFromDB(shareId, communityIds);
				JSONObject jsonObj = new JSONObject(json);
				engine.put("tmpcache", jsonObj);				
				if ( json != null )
				{					
					engine.eval("_cache['"+cacheName+"'] = eval('(' + tmpcache + ')');");
				}
				
			}
		}
	}//TESTED
	
	/**
	 * Trys to grab the json from a share using the communityIds as auth
	 * Returns the String json or null
	 * 
	 * @param shareId
	 * @param communityIds
	 * @return
	 */
	private static String getShareFromDB(ObjectId shareId, Set<ObjectId> communityIds) 
	{
		BasicDBObject query = new BasicDBObject(SharePojo._id_, shareId);
		query.put("communities._id",new BasicDBObject( MongoDbManager.in_,communityIds.toArray()));
		DBObject dbo = DbManager.getSocial().getShare().findOne(query);
		if ( dbo != null )
		{
			SharePojo share = SharePojo.fromDb(dbo,SharePojo.class);
			return share.getShare();
		}	
		return null;
	}
}
