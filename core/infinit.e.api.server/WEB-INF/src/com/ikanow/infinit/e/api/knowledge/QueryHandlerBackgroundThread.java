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
package com.ikanow.infinit.e.api.knowledge;

import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.bson.types.ObjectId;

import com.ikanow.infinit.e.api.knowledge.federated.FederatedQueryInMemoryCache;
import com.ikanow.infinit.e.api.utils.RESTTools;
import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.config.source.SourceFederatedQueryConfigPojo;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.mongodb.BasicDBObject;

public class QueryHandlerBackgroundThread implements Runnable {

	private static final Logger _logger = Logger.getLogger(QueryHandlerBackgroundThread.class);

	//private static boolean _DEBUG = true;
	private static boolean _DEBUG = false;
	
	/////////////////////////////////////////////////////////////////////////////////////
	
	// TOP LEVEL LOGIC
	
	protected void performPoll()
	{
		performPoll_federatedQueries();
	}
	
	/////////////////////////////////////////////////////////////////////////////////////
	
	// FEDERATED QUERY LOGIC
	
	protected HashMap<String, FederatedQueryInMemoryCache>[] _federatedQueryCache_pingPong;
	protected int _pingPong = 0;	

	@SuppressWarnings("unchecked")
	protected void performPoll_federatedQueries()
	{
		if (null == _federatedQueryCache_pingPong) {
			_federatedQueryCache_pingPong = (HashMap<String, FederatedQueryInMemoryCache>[])new HashMap[2];
			_federatedQueryCache_pingPong[0] = new HashMap<String, FederatedQueryInMemoryCache>();
		}
		
		// Fetch the latest set of federated sources:
		BasicDBObject fedQueryQuery = new BasicDBObject(SourcePojo.federatedQueryCommunityIds_, new BasicDBObject(DbManager.ne_, null));
		List<SourcePojo> federatedQueries = SourcePojo.listFromDb(DbManager.getIngest().getSource().find(fedQueryQuery), SourcePojo.listType());

		if (_DEBUG) _logger.debug("Awoke, found " + federatedQueries.size() + " federated query objects");
		
		int newPingPong = _pingPong == 0 ? 1 : 0; // (ie swap between 0 and 1)
		_federatedQueryCache_pingPong[newPingPong] = new HashMap<String, FederatedQueryInMemoryCache>();
		HashMap<String, FederatedQueryInMemoryCache> thisCache = _federatedQueryCache_pingPong[newPingPong];
		for (SourcePojo fedQuerySrc: federatedQueries) {
			// Check admin rights:
			if (null != fedQuerySrc.getOwnerId()) {
				fedQuerySrc.setOwnedByAdmin(RESTTools.adminLookup(fedQuerySrc.getOwnerId().toString(), false));
			}
			
			SourceFederatedQueryConfigPojo fedQuery = null;
			// Get the federated query:
			if ((null != fedQuerySrc.getProcessingPipeline()) && !fedQuerySrc.getProcessingPipeline().isEmpty()) {
				fedQuery = fedQuerySrc.getProcessingPipeline().iterator().next().federatedQuery;
			}			
			if (null == fedQuery) {
				continue;
			}
			
			fedQuery.parentSource = fedQuerySrc;
			
			FederatedQueryInMemoryCache newCacheEl = new FederatedQueryInMemoryCache();
			newCacheEl.sourceKey = fedQuerySrc.getKey();
			newCacheEl.source = fedQuery;
			newCacheEl.lastUpdated = fedQuerySrc.getModified();
			thisCache.put(newCacheEl.sourceKey, newCacheEl);
			
			// Also cache under the community
			ObjectId commId = fedQuerySrc.getCommunityIds().iterator().next();
			String commIdStr = commId.toString();
			FederatedQueryInMemoryCache newCommCacheEl = thisCache.get(commIdStr);
			if (null == newCommCacheEl) {
				newCommCacheEl = new FederatedQueryInMemoryCache();
				newCommCacheEl.communityId = commId;
				newCommCacheEl.sources = new HashMap<String, SourceFederatedQueryConfigPojo>();
				newCommCacheEl.lastUpdated = fedQuerySrc.getModified(); 
				thisCache.put(commIdStr, newCommCacheEl);
				
				if (_DEBUG) _logger.debug("Added community " + commIdStr + " , now # fed queries cache elements = " + thisCache.size());
			}//TESTED (by hand)
			newCommCacheEl.sources.put(newCacheEl.sourceKey, fedQuery);	
			if (newCommCacheEl.lastUpdated.before(fedQuerySrc.getModified())) {
				newCommCacheEl.lastUpdated = fedQuerySrc.getModified(); 
			}//TESTED (by hand)
			
			if (_DEBUG) _logger.debug("Added source " + newCacheEl.sourceKey + " , now # fed queries cache elements = " + thisCache.size() + ", # in this community = " + newCommCacheEl.sources.size());
			
		}//(end loop over federated queries)
		
		QueryHandler.setFederatedQueryCache(thisCache);
		_pingPong = newPingPong;
	}
	
	/////////////////////////////////////////////////////////////////////////////////////
	
	// BACKGROUND INFRASTRUCTURE
	
	protected long _nPollingPeriod_ms = 15000;
	protected Thread _pollThread;
	
	public QueryHandlerBackgroundThread() {
	}
	public void startThread() {
		_pollThread = new Thread(this);
		_pollThread.start();
	}

	@Override
	public void run() {
				
		for (;;) 
		{
			try
			{
				performPoll();
			}
			catch (Throwable thrown)
			{
				_logger.error("Error during poll", thrown);
			}
						
			try 
			{
				Thread.sleep(_nPollingPeriod_ms);
			} 
			catch (InterruptedException e) 
			{
			}
		}
	}
	
}
