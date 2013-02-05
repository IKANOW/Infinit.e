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

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.ikanow.infinit.e.data_model.store.document.EntityPojo;
import com.ikanow.infinit.e.data_model.store.feature.entity.EntityFeaturePojo;
import com.ikanow.infinit.e.data_model.store.social.sharing.SharePojo;
import com.ikanow.infinit.e.harvest.utils.DimensionUtility;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;

public class AliasLookupTable {

	private static final Logger logger = Logger.getLogger(AliasLookupTable.class);
	
	/////////////////////////////////
	
	// Builds the Alias Lookup from a set of sources
	
	public synchronized void buildOrUpdateAliasTable(List<SharePojo> aliasTables, String userIdStr) {
		_aliasTable.clear();
		_reverseAliasTable.clear();
		
		_lastModified = new Date();
		
		_nNumAliasShares = 0;
		SharePojo personalShare = null;
		for (SharePojo share: aliasTables) {
			// Look for personal shares, apply them last
			if ((null != share.getCommunities()) && !share.getCommunities().isEmpty()) {
				SharePojo.ShareCommunityPojo primaryShareComm = share.getCommunities().iterator().next();
				if (null != primaryShareComm.get_id()) {
					if (userIdStr.equalsIgnoreCase(primaryShareComm.get_id().toString())) {
						personalShare = share; // (save and carry on) 
						continue;
					}
				}				
			}//end look for personal community
			
			_nNumAliasShares++;
			populateAliasTableFromShare(share);
		}
		if (null != personalShare) { // Apply personal shares last
			_nNumAliasShares++;
			populateAliasTableFromShare(personalShare);			
		}
		
		
	} //TESTED
	
	// Utility:
	
	private void populateAliasTableFromShare(SharePojo share) {
		String json = share.getShare();
		if (null != json) {
			try {
				DBObject dbo = (DBObject) JSON.parse(json);
				if (null != dbo) {
					for (Object entryObj: dbo.toMap().entrySet()) {
						@SuppressWarnings("unchecked")
						Map.Entry<String, Object> entry = (Map.Entry<String, Object>)entryObj;
						
						String masterAlias = entry.getKey();
						EntityPojo masterAliasEntity = new EntityPojo();
						masterAliasEntity.setIndex(masterAlias);
						
						BasicDBObject entityFeatureObj = (BasicDBObject) entry.getValue();
						EntityFeaturePojo aliasInfo = null;
						try {
							aliasInfo = EntityFeaturePojo.fromDb(entityFeatureObj, EntityFeaturePojo.class);
						}
						catch (Exception e) {
							logger.debug("Failed to deserialize aliasInfo", e);
						}

						if ((null != aliasInfo) && (null != aliasInfo.getAlias()))
						{							
							aliasInfo.setIndex(masterAlias);
							if ((null == aliasInfo.getDimension()) && (null != aliasInfo.getType())) {
								aliasInfo.setDimension(DimensionUtility.getDimensionByType(aliasInfo.getType()));
							}//TESTED
							
							logger.debug("aliasTable entry: " + aliasInfo.getIndex() + " vs " + Arrays.toString(aliasInfo.getAlias().toArray()));
							
							// This is going to collide in an ugly fashion across multiple communities, 
							// we just have to live with that
							for (String aliasIndex: aliasInfo.getAlias()) {
								_aliasTable.put(aliasIndex, aliasInfo);
							}
							_aliasTable.put(aliasInfo.getIndex(), aliasInfo);
							Set<String> currAlias = _reverseAliasTable.get(aliasInfo.getIndex());
							if (null == currAlias) {
								_reverseAliasTable.put(aliasInfo.getIndex(), aliasInfo.getAlias());									
							}
							else { // Collision ... this we can handle a little-bit more elegantly
								currAlias.addAll(aliasInfo.getAlias());
							}									
						}
					}
				}
			}
			catch (Exception e) {
				logger.debug("General Aliasing Error: ", e);
				
			} // not Json, just carry on...
		}		
	}//TESTED
	
	/////////////////////////////////
	
	// Returns relevant info
	// (for now only care about disname and index)
	
	public synchronized EntityFeaturePojo doLookupFromIndex(String index) {
		return _aliasTable.get(index);
	}
	
	public synchronized Set<String> doInverseLookupFromIndex(String index) {
		return _reverseAliasTable.get(index);
	}
	
	/////////////////////////////////
	
	// Return last update time (null for never)
	
	public synchronized Date getLastModified() { 
		return _lastModified;
	}
	public synchronized int  getNumAliasShares() { 
		return _nNumAliasShares;
	}
	
	/////////////////////////////////
	
	// Update modified time (if not modified)
	
	public synchronized void setLastModified(Date lastChecked) {
		_lastModified = lastChecked;
	}
	
	/////////////////////////////////
	
	// Returns true if the table is actually empty
	
	public synchronized boolean isEmpty() {
		return _aliasTable.isEmpty();
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////
	
	// State:

	private int _nNumAliasShares = 0;
	private Date _lastModified = null;
	private HashMap<String, EntityFeaturePojo> _aliasTable = new HashMap<String, EntityFeaturePojo>();
	private HashMap<String, Set<String>> _reverseAliasTable = new HashMap<String, Set<String>>();
	
}
