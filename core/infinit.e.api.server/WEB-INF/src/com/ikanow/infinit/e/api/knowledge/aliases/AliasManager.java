package com.ikanow.infinit.e.api.knowledge.aliases;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.bson.types.ObjectId;

import com.ikanow.infinit.e.api.utils.PropertiesManager;
import com.ikanow.infinit.e.data_model.store.MongoDbManager;
import com.ikanow.infinit.e.data_model.store.social.sharing.SharePojo;
import com.ikanow.infinit.e.data_model.store.social.sharing.SharePojo.ShareOwnerPojo;
import com.mongodb.BasicDBObject;

public class AliasManager {

	private static final Logger logger = Logger.getLogger(AliasManager.class);
	
	private static AliasManager _myself = null;
	private static boolean _bFirstTime = true;
	
	public static AliasManager getAliasManager() {

		synchronized (AliasManager.class) {
			if (_bFirstTime) {
				// Check if (beta) aliasing enabled
				PropertiesManager props = new PropertiesManager();
				String s = props.getProperty("app.aliasing.beta");
				if ((null != s) && s.equalsIgnoreCase("true")) {
					if (null == _myself) {
						logger.debug("Initialized alias manager");
						
						return (_myself = new AliasManager());						
					}					
				}
				_bFirstTime = false;
			}
		}//TOTEST
		return _myself;
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////
	
	// Returns null if there's no active alias lookup table
	// (isn't null in the underlying structure)
	
	public synchronized AliasLookupTable getAliasLookupTable(String communityListStr, String[] communityStrArray, List<ObjectId> communityList) {
		
		// 1. Normalize input args
		if ((null == communityListStr) || (communityListStr.startsWith("*"))) {
			StringBuffer sb = new StringBuffer();
			if (null != communityStrArray) {
				for (String sid: communityStrArray) {
					sb.append(sid);
					sb.append(",");
				}
			}
			else { // communityList != null
				for (ObjectId oid: communityList) {
					sb.append(oid.toString());
					sb.append(",");
				}				
			}
			sb.setLength(sb.length() - 1);
			
			communityListStr = sb.toString();
		}//TESTED
		if (null == communityList) {
			if (null == communityStrArray) {
				communityStrArray = communityListStr.split("\\.");
			}
			communityList = new ArrayList<ObjectId>(communityStrArray.length);
			for (String s: communityStrArray) {
				try {
					communityList.add(new ObjectId(s));
				}
				catch (Exception e) {} // just carry on
			}
		}//TOTEST
		
		// 2. Get alias table
		
		AliasLookupTable table = _aliasLookups.get(communityListStr);
		Date now = new Date();
		
		// 3. Check alias table refresh
		
		boolean bRefresh = true;
		
		if (null != table) {
			// If it exists, check if it needs to be refreshed (check share modified times if >1 minute say)
			Date tableLastMod = table.getLastModified();
			if (null != tableLastMod) {
				if (now.getTime() - tableLastMod.getTime() <= 60000) { // less than a minute ago
					bRefresh = false;					
				}
			}
			if (bRefresh) { // Check shares to see if we really need to refresh
				
				logger.debug("Alias table exists, checking for refresh: " + communityListStr);
				
				BasicDBObject query = new BasicDBObject(SharePojo.type_, "infinite-entity-alias");
				query.put(ShareOwnerPojo.communities_id_, new BasicDBObject(MongoDbManager.in_, communityList));
				query.put(SharePojo.modified_, new BasicDBObject(MongoDbManager.gt_, tableLastMod));

				if (null == MongoDbManager.getSocial().getShare().findOne(query)) {
					bRefresh = false;
					table.setLastModified(now); // (ie don't check again for another minute)
				}//TESTED				
			}			
		}//TESTED 
		else {
			table = new AliasLookupTable();
			_aliasLookups.put(communityListStr, table);
		}
		
		// 4. Perform alias table refresh
		
		if (bRefresh) {
			
			BasicDBObject query = new BasicDBObject(SharePojo.type_, "infinite-entity-alias");
			query.put(ShareOwnerPojo.communities_id_, new BasicDBObject(MongoDbManager.in_, communityList));
			
			List<SharePojo> aliasShares = SharePojo.listFromDb(
					MongoDbManager.getSocial().getShare().find(query), SharePojo.listType());
			
			if (null != aliasShares) {
				logger.debug("Refresh/build alias table " + communityListStr + ": " + aliasShares.size());
				
				table.buildOrUpdateAliasTable(aliasShares);
			}

		}//TESTED
		
		return table.isEmpty() ? null : table;
		
	}//TOTEST (just the communityList == null case)
	
	////////////////////////////////////////////////////////////////////////////////////////////

	// State
	
	private HashMap<String, AliasLookupTable> _aliasLookups = new HashMap<String, AliasLookupTable>();
	
}
