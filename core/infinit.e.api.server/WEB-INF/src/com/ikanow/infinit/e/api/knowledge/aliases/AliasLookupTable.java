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
	
	public synchronized void buildOrUpdateAliasTable(List<SharePojo> aliasTables) {
		_aliasTable.clear();
		_reverseAliasTable.clear();
		
		_lastModified = new Date();
		
		for (SharePojo share: aliasTables) {
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
								
								for (String aliasIndex: aliasInfo.getAlias()) {
									_aliasTable.put(aliasIndex, aliasInfo);
								}
								_aliasTable.put(aliasInfo.getIndex(), aliasInfo);
								_reverseAliasTable.put(aliasInfo.getIndex(), aliasInfo.getAlias());
							}
						}
					}
				}
				catch (Exception e) {
					logger.debug("General Aliasing Error: ", e);
					
				} // not Json, just carry on...
			}
		}
	} //TESTED
	
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
	
	private Date _lastModified = null;
	private HashMap<String, EntityFeaturePojo> _aliasTable = new HashMap<String, EntityFeaturePojo>();
	private HashMap<String, Set<String>> _reverseAliasTable = new HashMap<String, Set<String>>();
	
}
