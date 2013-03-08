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
import com.ikanow.infinit.e.data_model.store.social.sharing.SharePojo.ShareCommunityPojo;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

public class AliasManager {

	private static final Logger logger = Logger.getLogger(AliasManager.class);
	
	private static AliasManager _myself = null;
	private static boolean _bFirstTime = true;
	
	public static AliasManager getAliasManager() {

		synchronized (AliasManager.class) {
			if (_bFirstTime) {
				// Check if (beta) aliasing enabled
				PropertiesManager props = new PropertiesManager();
				if (props.getAliasingEnabled()) {
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
	
	public synchronized AliasLookupTable getAliasLookupTable(String communityListStr, String[] communityStrArray, List<ObjectId> communityList, String userIdStr) {
		
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
			if (sb.length() > 0) {
				sb.setLength(sb.length() - 1);
			}			
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
				query.put(ShareCommunityPojo.shareQuery_id_, new BasicDBObject(MongoDbManager.in_, communityList));								
				query.put(SharePojo.endorsed_, new BasicDBObject(MongoDbManager.in_, communityList));
				BasicDBObject fields = new BasicDBObject(SharePojo.modified_, 1);				
				DBCursor dbc = MongoDbManager.getSocial().getShare().find(query, fields);
				
				if (dbc.count() == table.getNumAliasShares()) { // easy answer is out!
					bRefresh = false;
					for (DBObject dbo: dbc) {
						Date date = (Date) dbo.get(SharePojo.modified_);
						if ((date != null) && (date.getTime() > tableLastMod.getTime())) {
							logger.debug("Alias: change in date for " + dbo.get(SharePojo._id_) + ": " + date);
							bRefresh = true;
							break;
						}
					}					
				}//TOTEST
				else {
					logger.debug("Alias: change in #shares, now: " + dbc.count() + " vs prev: " + table.getNumAliasShares());
				}
			}			
		}//TESTED 
		else {
			table = new AliasLookupTable();
			_aliasLookups.put(communityListStr, table);
		}
		
		// 4. Perform alias table refresh
		
		if (bRefresh) {
			
			BasicDBObject query = new BasicDBObject(SharePojo.type_, "infinite-entity-alias");
			query.put(ShareCommunityPojo.shareQuery_id_, new BasicDBObject(MongoDbManager.in_, communityList));
			query.put(SharePojo.endorsed_, new BasicDBObject(MongoDbManager.in_, communityList));
			
			List<SharePojo> aliasShares = SharePojo.listFromDb(
					MongoDbManager.getSocial().getShare().find(query), SharePojo.listType());
			
			if (null != aliasShares) {
				logger.debug("Refresh/build alias table " + communityListStr + ": " + aliasShares.size());
			}
			else {
				logger.debug("Clear alias table " + communityListStr);
				aliasShares = new ArrayList<SharePojo>();
			}
			table.buildOrUpdateAliasTable(aliasShares, userIdStr);

		}//TESTED
		
		return table.isEmpty() ? null : table;
		
	}//TOTEST (just the communityList == null case)
	
	////////////////////////////////////////////////////////////////////////////////////////////

	// State
	
	private HashMap<String, AliasLookupTable> _aliasLookups = new HashMap<String, AliasLookupTable>();
	
}
