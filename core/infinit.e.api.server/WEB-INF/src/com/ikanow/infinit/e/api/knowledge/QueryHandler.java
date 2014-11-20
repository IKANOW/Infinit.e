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
package com.ikanow.infinit.e.api.knowledge;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.action.search.SearchRequestBuilder;
import org.elasticsearch.common.joda.time.Interval;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.BaseQueryBuilder;
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.CrossVersionQueryBuilders;
import org.elasticsearch.index.query.CustomFiltersScoreQueryBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.GeoDistanceFilterBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.sort.SortOrder;

import com.google.common.collect.ArrayListMultimap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ikanow.infinit.e.api.knowledge.aliases.AliasLookupTable;
import com.ikanow.infinit.e.api.knowledge.aliases.AliasManager;
import com.ikanow.infinit.e.api.knowledge.federated.FederatedQueryInMemoryCache;
import com.ikanow.infinit.e.api.knowledge.federated.SimpleFederatedQueryEngine;
import com.ikanow.infinit.e.api.knowledge.processing.AggregationUtils;
import com.ikanow.infinit.e.api.knowledge.processing.QueryDecayFactory;
import com.ikanow.infinit.e.api.knowledge.processing.ScoringUtils;
import com.ikanow.infinit.e.api.social.sharing.ShareHandler;
import com.ikanow.infinit.e.api.utils.PropertiesManager;
import com.ikanow.infinit.e.api.utils.SimpleBooleanParser;
import com.ikanow.infinit.e.api.utils.SocialUtils;
import com.ikanow.infinit.e.data_model.Globals;
import com.ikanow.infinit.e.data_model.api.BasePojoApiMap;
import com.ikanow.infinit.e.data_model.api.ResponsePojo;
import com.ikanow.infinit.e.data_model.api.ResponsePojo.ResponseObject;
import com.ikanow.infinit.e.data_model.api.knowledge.AdvancedQueryPojo;
import com.ikanow.infinit.e.data_model.api.knowledge.StatisticsPojo;
import com.ikanow.infinit.e.data_model.control.DocumentQueueControlPojo;
import com.ikanow.infinit.e.data_model.index.ElasticSearchManager;
import com.ikanow.infinit.e.data_model.index.document.DocumentPojoIndexMap;
import com.ikanow.infinit.e.data_model.interfaces.query.IQueryExtension;
import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.config.source.SourceFederatedQueryConfigPojo;
import com.ikanow.infinit.e.data_model.store.custom.mapreduce.CustomMapReduceJobPojo;
import com.ikanow.infinit.e.data_model.store.document.AssociationPojo;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.infinit.e.data_model.store.document.EntityPojo;
import com.ikanow.infinit.e.data_model.store.feature.entity.EntityFeaturePojo;
import com.ikanow.infinit.e.data_model.store.social.sharing.SharePojo;
import com.ikanow.infinit.e.data_model.utils.GeoOntologyMapping;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.MongoException;
import com.mongodb.ReadPreference;

//
// This code contains all the processing logic for the (beta)
// Advanced Queries
//

//(remove this during active development - want to just depress a deprecation warning but no way of doing this for both 0.19 and 1.0)
//@SuppressWarnings("deprecation")
@SuppressWarnings("all")
public class QueryHandler {

	private final StringBuffer _logMsg = new StringBuffer();	
	private static final Logger _logger = Logger.getLogger(QueryHandler.class);
	
	public QueryHandler() {}
	
	private static Semaphore _concurrentAccessLock = null;
	private boolean acquireConcurrentAccessLock() throws InterruptedException {
		if (null == _concurrentAccessLock) {
			_concurrentAccessLock = new Semaphore(2);
		}
		return _concurrentAccessLock.tryAcquire(10, TimeUnit.MINUTES);
	}
	private void releaseConcurrentAccessLock() {
		_concurrentAccessLock.release();
	}
	
	// Query cache (re-created per request, but there's some static things in here for performance):
	private AliasLookupTable _aliasLookup = null;
	
	private LinkedList<AdvancedQueryPojo.QueryTermPojo> _extraFullTextTerms = null;
		// (used to allow entity terms to add top level (full text) terms)	
	
	private static PropertiesManager _properties = null;
	private static com.ikanow.infinit.e.data_model.utils.PropertiesManager _dataModelProps = null;
	private static String _aggregationAccuracy = "full";  
	private static ArrayList<Class<IQueryExtension>> _queryExtensions = null;
	
	// Built in query extension: federated query engine
	private SimpleFederatedQueryEngine _builtInFederatedQueryEngine = null; // (note: _not_ static)
	private static HashMap<String, FederatedQueryInMemoryCache> _federatedQueryCache = new HashMap<String, FederatedQueryInMemoryCache>();
	static void setFederatedQueryCache(HashMap<String, FederatedQueryInMemoryCache> newCache) {
		_federatedQueryCache = newCache;
	}
	
	private AdvancedQueryPojo.QueryScorePojo _scoringParams;
		// (need this here so we can set the adjust param for complex queries)
	
	private static int _replicaSetDistributionRatio = -1;
	
////////////////////////////////////////////////////////////////////////
	
// 0] Top level processing
	
	public ResponsePojo doQuery(String userIdStr, AdvancedQueryPojo query, String communityIdStrList, StringBuffer errorString) throws UnknownHostException, MongoException, IOException, InstantiationException, IllegalAccessException {

		if (null == _properties) {
			_properties = new PropertiesManager();
			_aggregationAccuracy = _properties.getAggregationAccuracy();
			_dataModelProps = new com.ikanow.infinit.e.data_model.utils.PropertiesManager();
			_replicaSetDistributionRatio = 1 + _dataModelProps.getDocDbReadDistributionRatio();
			
			String[] queryExtensions = _properties.getQueryExtensions();
			if (null != queryExtensions) {
				_queryExtensions = new ArrayList<Class<IQueryExtension>>(queryExtensions.length);
				for (String s: queryExtensions) {
					try {
						Class<IQueryExtension> queryExtensionClass = (Class<IQueryExtension>) Class.forName(s);
						_queryExtensions.add(queryExtensionClass);
					}
					catch (Exception e) {
						_logger.error("Failed to load query extension: " + s, e);
					}
					catch (Error e) {
						_logger.error("Failed to load query extension: " + s, e);						
					}
				}//(end list over query extensions)
				if (_queryExtensions.isEmpty()) {
					_queryExtensions = null;
				}
			}//TESTED (see test.QueryExtensionsTestCode)
		}
		ObjectId queryId = null;
		_scoringParams = query.score;
		
		// (NOTE CAN'T ACCESS "query" UNTIL AFTER 0.1 BECAUSE THAT CAN CHANGE IT) 
		
		long nSysTime = (_nNow = System.currentTimeMillis());		
		
		ResponsePojo rp = new ResponsePojo();
		
		// communityIdList is CSV
		String[] communityIdStrs = SocialUtils.getCommunityIds(userIdStr, communityIdStrList);
		
		//(timing)
		long nQuerySetupTime = System.currentTimeMillis();
	
		ElasticSearchManager indexMgr = getIndexManager(communityIdStrs);
		SearchRequestBuilder searchSettings = indexMgr.getSearchOptions();

		StringBuffer querySummary = new StringBuffer();
		BaseQueryBuilder queryObj = null;
		InternalTempFilterInfo tempFilterInfo = null;
		try {
			queryObj = getBaseQuery(query, communityIdStrs, communityIdStrList, userIdStr, querySummary);
			if (null == queryObj) { // only occurs if has 1 element with ftext starting $cache:
				return getSavedQueryInstead(query.qt.get(0).ftext.substring(7), communityIdStrs, query); // (step over cache preamble)
			}
			tempFilterInfo = getBaseFilter(query, communityIdStrs);
		}
		catch (Exception e) {
			Globals.populateStackTrace(errorString, e);
			if (null != e.getCause()) {
				errorString.append("[CAUSE=").append(e.getCause().getMessage()).append("]");
				Globals.populateStackTrace(errorString, e.getCause());				
			}
			errorString.append(": " + e.getMessage());
			return null;
		}
		
		//DEBUG
		//querySummary.append(new Gson().toJson(query, AdvancedQueryPojo.class));
		
	// 0.4] Pre-Lucene Scoring
		
		// 0.4.1] General
		
		// Different options:
		//   a] Get the most recent N documents matching the query, score post-query
		//   b] Get the N highest (Lucene) scoring documents, incorporate significance post-query if desired
		// In both cases, N depends on whether significance calculation is taking place (and on the "skip" param)
		
		int nRecordsToOutput = query.output.docs.numReturn;
		int nRecordsToSkip = query.output.docs.skip;
		int nRecordsToGet = query.score.numAnalyze;

		final int nMAXRECORDSTOOUTPUT = 10000;
		final int nMAXRECORDSTOGET = 20000;
		
		// Some sanity checking on doc numbers:
		if (nRecordsToOutput > nMAXRECORDSTOOUTPUT) { // Upper limit...
			errorString.append(": Max # docs to return is 10000.");
			return null;
		}
		if (nRecordsToGet < nRecordsToOutput) {
			nRecordsToGet = nRecordsToOutput;
		}
		else if (nRecordsToGet > nMAXRECORDSTOGET) { // Upper limit...
			nRecordsToGet = nMAXRECORDSTOGET; // (we can do something sensible with this so carry on regardless)
		}
		
		boolean bUseSignificance = (query.score.sigWeight > 0.0);
		boolean bNeedExtraResultsForEnts = 
			((query.output.aggregation != null) && (query.output.aggregation.entsNumReturn != null) && (query.output.aggregation.entsNumReturn > 0))
				||
			(query.output.docs.enable && (query.output.docs.numReturn > 0) && (query.output.docs.ents) && (query.score.scoreEnts));
		
		if (bUseSignificance || bNeedExtraResultsForEnts) {
			
			// Some logic taken from the original "knowledge/search"
			while ( (nRecordsToSkip + nRecordsToOutput > nRecordsToGet) && (nRecordsToGet <= nMAXRECORDSTOGET) )
			{
				nRecordsToGet += nRecordsToGet;
			}
			if (nRecordsToGet > nMAXRECORDSTOGET) {
				errorString.append(": Can only skip through to 20000 documents.");				
				return null;
			}
			searchSettings.setSize(nRecordsToGet);
			
			//TESTED
		}
		else if (query.output.docs.enable) { // In this case we just need the minimum number of records
			// (whether searching by date or by relevance)
			searchSettings.setFrom(nRecordsToSkip);
			nRecordsToSkip = 0; // (so it isn't double counted in the processing module)
			nRecordsToGet = nRecordsToOutput;
			searchSettings.setSize(nRecordsToGet);
			//TESTED
		}
		else { // In thise case we're just outputting aggregations, and not even ones that come from the docs
			nRecordsToGet = 0; // (use this variable everywhere where we care about bring docs back either to output or for suitable aggregation)
			searchSettings.setSize(0);
		}
		
		// Sort on score if relevance is being used		
		
		if (nRecordsToGet > 0) {
			if (query.score.relWeight > 0.0) { // (b) above
				// Using score is default, nothing to do
			}
			else { // (a) above
				// Debug code, if rel weight negative then use date to check Lucene score is better...
				if (query.score.relWeight < 0.0) {
					query.score.relWeight = -query.score.relWeight;
				}
				// Set Lucene to order:
				searchSettings.addSort(DocumentPojo.publishedDate_, SortOrder.DESC);
			}//TOTEST
		}//(if docs aren't enabled, don't need to worry about sorting)
		
		// 0.4.2] Prox scoring (needs to happen after [0.3]

		// Add proximity scoring:
		boolean bLowAccuracyDecay = false;
		if ((nRecordsToGet > 0) || (null == _scoringParams.adjustAggregateSig) || _scoringParams.adjustAggregateSig) {
			// (ie if we're getting docs or applying scores to entities)
			
			if (!_aggregationAccuracy.equals("full")) {
				if (_aggregationAccuracy.equals("low") || _aggregationAccuracy.contains("decay:low")) {
					bLowAccuracyDecay = true;
				}
			}
			queryObj = addProximityBasedScoring(queryObj, searchSettings, query.score, tempFilterInfo.parentFilterObj, bLowAccuracyDecay);
			
			if (null == _scoringParams.adjustAggregateSig) { // auto-decide .. if ftext is set and is non-trivial
				if ((null != query.score.timeProx) || (null != query.score.geoProx)) {
						// (These are set to null above if badly formed)
					_scoringParams.adjustAggregateSig = true;					
				}
			}
			
		}// (else not worth the effort)	
		
		// 0.4.3] Source weightings (if any)
		
		queryObj = applyManualWeights(queryObj, query.score);
		
	// 0.5] Pre-lucene output options
		
		// only return the id field and score
		// (Both _id and score come back as default options, SearchHit:: getId and getScore, don't need anything else)

		// Facets
		
		// (These are needed for the case where we need to perform aggregations manually)
		Integer manualEntsNumReturn = null;
		Integer manualEventsNumReturn = null;
		Integer manualFactsNumReturn = null;
		Integer manualGeoNumReturn = null;
		
		//DEBUG
		//System.out.println(new Gson().toJson(query.output.aggregation));
		
		if ((null != query.output.aggregation) && (null != query.output.aggregation.raw)) { // Like query, specify raw aggregation (Facets)
			// Gross raw handling for facets
			if ((null != query.raw) && (null != query.raw.query)) {
				// Don't currently support raw query and raw facets because I can't work out how to apply
				// the override on group/source!
				errorString.append(": Not currently allowed raw query and raw facets");
				return null;
			}
			else { // Normal code
				searchSettings.setFacets(query.output.aggregation.raw.getBytes());
			}
		}
		else { // Apply various aggregation (=="facet") outputs to searchSettings
			boolean bSpecialCase = (null != query.raw) && (null != query.raw.query);
			
			if (!_aggregationAccuracy.equals("full")) {
				if (null != query.output.aggregation) {
					// Geo - optional
					if (_aggregationAccuracy.equals("low") || _aggregationAccuracy.contains("geo:low")) {
						manualGeoNumReturn = query.output.aggregation.geoNumReturn;
						query.output.aggregation.geoNumReturn = null;						
					}
					// Assoc - optional
					if (_aggregationAccuracy.equals("low") || _aggregationAccuracy.contains("assoc:low")) {
						manualEventsNumReturn = query.output.aggregation.eventsNumReturn;
						manualFactsNumReturn = query.output.aggregation.factsNumReturn;
						query.output.aggregation.eventsNumReturn = null;
						query.output.aggregation.factsNumReturn = null;
					}										
					// Ents - optional
					if (_aggregationAccuracy.equals("low") || _aggregationAccuracy.contains("ent:low")) {
						manualEntsNumReturn = query.output.aggregation.entsNumReturn;
						query.output.aggregation.entsNumReturn = null;
					}					
					// (allow time aggregation)
					// (allow source aggregation)
				}
			}
			AggregationUtils.parseOutputAggregation(query.output.aggregation, _aliasLookup, 
														tempFilterInfo.entityTypeFilterStrings, tempFilterInfo.assocVerbFilterStrings, 
															searchSettings, bSpecialCase?tempFilterInfo.parentFilterObj:null);

			// In partial accuracy case, restore aggregation
			if (null != manualEntsNumReturn) {
				query.output.aggregation.entsNumReturn = manualEntsNumReturn;
			}
			if (null != manualEventsNumReturn) {
				query.output.aggregation.eventsNumReturn = manualEventsNumReturn;
			}
			if (null != manualFactsNumReturn) {
				query.output.aggregation.factsNumReturn = manualFactsNumReturn;
			}
			if (null != manualGeoNumReturn) {
				query.output.aggregation.geoNumReturn = manualGeoNumReturn;
			}
			//TESTED
		}
		//TESTED x2			
		
		//(timing)
		nQuerySetupTime = System.currentTimeMillis() - nQuerySetupTime;
		
	// 0.6] Perform Lucene query
		
		// 0.6.1: query extensions: pre-query hook
		ArrayList<IQueryExtension> queryExtensions = null;
		if (null != _queryExtensions) {
			queryId = new ObjectId();
			queryExtensions = new ArrayList<IQueryExtension>(_queryExtensions.size());
			for (Class<IQueryExtension> queryExtensionClass: _queryExtensions) {
				// Don't catch any exceptions thrown here - let it bubble upwards
				IQueryExtension queryExtension = queryExtensionClass.newInstance();
				queryExtension.preQueryActivities(queryId, query, communityIdStrs);
				queryExtensions.add(queryExtension);
			}
		}//TESTED (see test.QueryExtensionsTestCode)
		
		// Built-in federated query engine ...
		if (null != _federatedQueryCache) {
			// 2 modes:
			// 1) If srcInclude is true(default) then check each source vs the table
			// 2) If srcInclude is false, or no sources specified, then check each community vs the table
			
			// 1: 
			if ((null != query.input) && (null != query.input.sources) && ((null == query.input.srcInclude) || query.input.srcInclude))
			{
				for (String srcKey: query.input.sources) {
					FederatedQueryInMemoryCache fedQueryCacheEl = _federatedQueryCache.get(srcKey);
					if (null != fedQueryCacheEl) {
						if (null == this._builtInFederatedQueryEngine) {
							_builtInFederatedQueryEngine = new SimpleFederatedQueryEngine();
						}
						_builtInFederatedQueryEngine.addEndpoint(fedQueryCacheEl.source);
					}
				}
			}//TESTED (//TESTED (http://localhost:8184/knowledge/document/query/53ab42a2e4b04bcfe2de4387?qt[0].entity=%22garyhart.com/externaldomain%22&output.docs.numReturn=10&input.sources=inf...federated.externaldomain.&input.srcInclude=true))
			
			// 2:
			else { //Get federated queries from communities
				HashSet<String> excludeSrcs = null;
				for (String commIdStr: communityIdStrs) {
					FederatedQueryInMemoryCache fedQueryCacheEl = _federatedQueryCache.get(commIdStr);
					if (null != fedQueryCacheEl) {
						
						if ((null != query.input) && (null != query.input.sources)) { // (there are exclude sources)
							if (null == excludeSrcs) {
								excludeSrcs = new HashSet<String>(query.input.sources);
							}
						}//TESTED (http://localhost:8184/knowledge/document/query/53ab42a2e4b04bcfe2de4387?qt[0].entity=%22garyhart.com/externaldomain%22&output.docs.numReturn=10&input.sources=inf...federated.externaldomain.&input.srcInclude=false)
						
						for (Map.Entry<String, SourceFederatedQueryConfigPojo> fedQueryKV: fedQueryCacheEl.sources.entrySet()) {
							if ((null == excludeSrcs) || !excludeSrcs.contains(fedQueryKV.getKey())) {
								if (null == this._builtInFederatedQueryEngine) {
									_builtInFederatedQueryEngine = new SimpleFederatedQueryEngine();
								}
								_builtInFederatedQueryEngine.addEndpoint(fedQueryKV.getValue());
							}
						}
					}
				}//TESTED (by hand)
			}
			if (null != _builtInFederatedQueryEngine) {
				_builtInFederatedQueryEngine.preQueryActivities(queryId, query, communityIdStrs);
			}
		}		
		
		// 0.6.2: the main query
		if ((null != query.explain) && query.explain) { // (for diagnostic - will return lucene explanation)
			searchSettings.setExplain(true);
		}
		
		SearchResponse queryResults = null;

		// (_source can now be enabled, so this is necessary to avoid returning it)
		searchSettings.addFields();
		if ((null != query.raw) && (null != query.raw.query)) 
		{
			// (Can bypass all other settings)				
			searchSettings.setQuery(query.raw.query);
			queryResults = indexMgr.doQuery(null, tempFilterInfo.parentFilterObj, searchSettings);
		}//TESTED '{ "raw": { "match_all": {} } }'
		else 
		{
			// Where I can, use the source filter as part of the query so that
			// facets will apply to query+filter, not just filter
			queryObj = QueryBuilders.boolQuery().must(queryObj).must(QueryBuilders.constantScoreQuery(tempFilterInfo.parentFilterObj).boost(0.0F));
			
			queryResults = indexMgr.doQuery(queryObj, null, searchSettings);
		}//TESTED '{}' etc
		
		long nLuceneTime = queryResults.getTookInMillis();

	// 0.7] Lucene scores	
		
		long nProcTime = 0;
		long nProcTime_tmp = System.currentTimeMillis();
		
		StatisticsPojo stats = new StatisticsPojo();			
		stats.found = queryResults.getHits().getTotalHits();
        stats.start = (long)nRecordsToSkip;
        
		if (nRecordsToGet > 0) {
			stats.setScore(queryResults.getHits(), (null != query.score.geoProx)||(null != query.score.timeProx), (null != query.explain) && query.explain);
		}

		//DEBUG
		//System.out.println(new Gson().toJson(queryResults));
		
		nProcTime += (System.currentTimeMillis() - nProcTime_tmp);
		
	// 0.8] Get data from Mongo + handle scoring

		//(timing)
		long nMongoTime = System.currentTimeMillis();
		List<BasicDBObject> docs = null;
		
		//(aggregation)
		LinkedList<BasicDBObject> lowAccuracyAggregatedEntities = null; // (always low accuracy)
		LinkedList<BasicDBObject> standaloneEvents = null;
		LinkedList<BasicDBObject> lowAccuracyAggregatedEvents = null;
		LinkedList<BasicDBObject> lowAccuracyAggregatedFacts = null;
		AggregationUtils.GeoContainer lowAccuracyAggregatedGeo = null;
		AggregationUtils.GeoContainer extraAliasAggregatedGeo = null;
		
		ScoringUtils scoreStats = null;
		if (null != stats.getIds()) {

			DBCursor docs0 = this.getDocIds(DbManager.getDocument().getMetadata(), stats.getIds(), nRecordsToGet, query.output, query.score);
			nMongoTime = System.currentTimeMillis() - nMongoTime;
							
			nProcTime_tmp = System.currentTimeMillis();
			
			// Entity aggregation (CURRENTLY ALWAYS LOW AGGREGATION):
			if ((null != query.output.aggregation) && (null != query.output.aggregation.entsNumReturn) && (query.output.aggregation.entsNumReturn > 0)) {					
				lowAccuracyAggregatedEntities = new LinkedList<BasicDBObject>();
			}
			
			// Standalone events:
			if ((query.output.docs != null) && (query.output.docs.eventsTimeline != null) && query.output.docs.eventsTimeline) {
				standaloneEvents = new LinkedList<BasicDBObject>();
			}				
			
			// Low accuracy aggregations:
			
			if ((null != manualEventsNumReturn) && (manualEventsNumReturn > 0)) {
				lowAccuracyAggregatedEvents = new LinkedList<BasicDBObject>();
			}
			if ((null != manualFactsNumReturn) && (manualFactsNumReturn > 0)) {
				lowAccuracyAggregatedFacts = new LinkedList<BasicDBObject>();				
			}
			
			if ((null != manualGeoNumReturn) && (manualGeoNumReturn > 0)) {
				lowAccuracyAggregatedGeo = new AggregationUtils.GeoContainer();								
			}
			else if ((null != query.output.aggregation) && (null != query.output.aggregation.geoNumReturn) && (query.output.aggregation.geoNumReturn > 0))
			{
				// (only if not using low accuracy aggregation ... otherwise it all gets dumped in lowAccuracyAggregatedGeo)
				extraAliasAggregatedGeo = new AggregationUtils.GeoContainer();				
			}
			
			scoreStats = new ScoringUtils();
			try {
				boolean lockAcquired = true;
				try {
					lockAcquired = this.acquireConcurrentAccessLock();
					
				} catch (InterruptedException e) {
					//(that's fine just carry on)
					lockAcquired = false;
				}
				if (!lockAcquired) {
					rp.setResponse(new ResponseObject("Query", false, "Query engine busy, please try again later."));
					return rp;
				}
				
				scoreStats.setAliasLookupTable(_aliasLookup);
				docs = scoreStats.calcTFIDFAndFilter(DbManager.getDocument().getMetadata(), 
															docs0, query.score, query.output, stats, bLowAccuracyDecay,
																nRecordsToSkip, nRecordsToOutput, 
																	communityIdStrs,
																	tempFilterInfo.entityTypeFilterStrings, tempFilterInfo.assocVerbFilterStrings,
																	standaloneEvents, 
																	lowAccuracyAggregatedEntities, 
																	lowAccuracyAggregatedGeo, extraAliasAggregatedGeo,
																					lowAccuracyAggregatedEvents, lowAccuracyAggregatedFacts);
			}
			finally {
				scoreStats.clearAsMuchMemoryAsPossible();
				this.releaseConcurrentAccessLock();
			}			
			nProcTime += (System.currentTimeMillis() - nProcTime_tmp);
		}
		else {
			nMongoTime = 0;
		}
		//TESTED (all queries)
		
	// 0.9] Output:

		rp.setResponse(new ResponseObject("Query", true, querySummary.toString()));
		
		// 0.9.1] Stats:
		stats.resetArrays();
		rp.setStats(stats); // (only actually uses the response pojo, but get rid of big fields anyway...)

		// 0.9.2] Facets:

		if (null != lowAccuracyAggregatedEntities) { // Entity aggregation
			rp.setEntities(lowAccuracyAggregatedEntities);				
		}
		if (null != standaloneEvents) {
			rp.setEventsTimeline(standaloneEvents);
		}
		if (null != lowAccuracyAggregatedGeo) {
			rp.setGeo(lowAccuracyAggregatedGeo.geotags, (int)lowAccuracyAggregatedGeo.maxCount, (int)lowAccuracyAggregatedGeo.minCount);
		}
		if (null != lowAccuracyAggregatedEvents) {
			rp.setEvents(lowAccuracyAggregatedEvents);
		}
		if (null != lowAccuracyAggregatedFacts) {
			rp.setFacts(lowAccuracyAggregatedFacts);
		}
		
		if ((null != query.output.aggregation) && (null != query.output.aggregation.raw)) {
			rp.setFacets(queryResults.getFacets().facetsAsMap());
		}
		else if ((null != queryResults.getFacets()) && (null != queryResults.getFacets().getFacets())) { // "Logical" aggregation

			if (0.0 == query.score.sigWeight) {
				scoreStats = null; // (don't calculate event/fact aggregated significance if it's not wanted)
			}
			AggregationUtils.loadAggregationResults(rp, queryResults.getFacets().getFacets(), query.output.aggregation, scoreStats, _aliasLookup, tempFilterInfo.entityTypeFilterStrings, tempFilterInfo.assocVerbFilterStrings, extraAliasAggregatedGeo);
			
		} // (end facets not overwritten)			
		
		scoreStats = null; // (now definitely never need scoreStats)
		
		// 0.9.3] Documents
		if  (query.output.docs.enable) {
			if ((null != docs) && (docs.size() > 0)) {
				rp.setData(docs, (BasePojoApiMap<BasicDBObject>)null);
			}
			else { // (ensure there's always an empty list)
				docs = new ArrayList<BasicDBObject>(0);
				rp.setData(docs, (BasePojoApiMap<BasicDBObject>)null);
			}
		}
		else { // (ensure there's always an empty list)
			docs = new ArrayList<BasicDBObject>(0);
			rp.setData(docs, (BasePojoApiMap<BasicDBObject>)null);
		}
		
		// 0.9.4] query extensions: post-query hook
		if (null != queryExtensions) {
			for (IQueryExtension queryExtension: queryExtensions) {
				// Don't catch any exceptions thrown here - let it bubble upwards
				queryExtension.postQueryActivities(queryId, docs, rp);
			}
		}//TESTED (see test.QueryExtensionsTestCode)
		
		// (Built-in version)
		if (null != _builtInFederatedQueryEngine) {
			_builtInFederatedQueryEngine.postQueryActivities(queryId, docs, rp);
		}
		
		// 0.9.5] Timing/logging
		
		long nTotalTime = System.currentTimeMillis() - nSysTime;
		rp.getResponse().setTime(nTotalTime);
		
		_logMsg.setLength(0);
		_logMsg.append("knowledge/query querylen=").append(querySummary.length());
		_logMsg.append(" query=").append(querySummary.toString());
		_logMsg.append(" userid=").append(userIdStr);
		_logMsg.append(" groups=").append(communityIdStrList);
		_logMsg.append(" found=").append(stats.found);
		_logMsg.append(" luceneTime=").append(nLuceneTime).append(" ms");
		_logMsg.append(" setupTime=").append(nQuerySetupTime).append(" ms");
		_logMsg.append(" procTime=").append(nProcTime).append(" ms");
		_logMsg.append(" mongoTime=").append(nMongoTime).append(" ms");
		_logMsg.append(" time=").append(nTotalTime).append(" ms");
		_logger.info(_logMsg.toString());

		//DEBUG
		//System.out.println(_logMsg.toString());
			
		// Exceptions percolate up to the resource and are handled there...
		return rp;
	}
		
////////////////////////////////////////////////////////////////////////
	
	// Utility version of the above query call - just converts the advanced query pojo into an elasticsearch object that can
	// be queried

	public static class QueryInfo {
		public ElasticSearchManager indexMgr;
		public BaseQueryBuilder queryObj;
		public String querySummary;
	}
	
	public QueryInfo convertInfiniteQuery(AdvancedQueryPojo query, String[] communityIdStrs, String userIdStr) {
		// Fill in the blanks (a decent attempt has been made to fill out the blanks inside these options)
		if (null == query.input) {
			query.input = new AdvancedQueryPojo.QueryInputPojo();				
		}
		if (null == query.score) {
			query.score = new AdvancedQueryPojo.QueryScorePojo();				
		}
		if (null == query.output) {
			query.output = new AdvancedQueryPojo.QueryOutputPojo();
		}		
		if (null == query.output.docs) { // (Docs are sufficiently important we'll make sure they're always present)
			query.output.docs = new AdvancedQueryPojo.QueryOutputPojo.DocumentOutputPojo();
		}

		// Other intialization
		_nNow = System.currentTimeMillis();
		
		// Now onto the logic:
		QueryInfo queryInfo = new QueryInfo();
		
		StringBuffer sb = new StringBuffer(userIdStr);
		for (String sCommunityId: communityIdStrs) {
			sb.append(',').append(sCommunityId);
		}
		
		queryInfo.indexMgr = getIndexManager(communityIdStrs);
		StringBuffer info = new StringBuffer();
		queryInfo.queryObj = getBaseQuery(query, communityIdStrs, sb.toString(), userIdStr, info);
		queryInfo.querySummary = info.toString();
		InternalTempFilterInfo tempFilterInfo = getBaseFilter(query, communityIdStrs);
		queryInfo.queryObj = QueryBuilders.boolQuery().must(queryInfo.queryObj).
								must(QueryBuilders.constantScoreQuery(tempFilterInfo.parentFilterObj).boost(0.0F));
		
		return queryInfo;
	}//TOTEST
	
////////////////////////////////////////////////////////////////////////
	
//0b] QUERY BREAKDOWN	

////////////////////////////////////////////////////////////////////////
	
	// 0.b.1) indexes
	
	private ElasticSearchManager getIndexManager(String[] communityIdStrs)
	{
		// Create a multi-index to check against all relevant shards:
		StringBuffer sb = new StringBuffer(DocumentPojoIndexMap.globalDocumentIndexCollection_);
		sb.append(',').append(DocumentPojoIndexMap.manyGeoDocumentIndexCollection_);
		for (String sCommunityId: communityIdStrs) {
			sb.append(',').append("docs_").append(sCommunityId);
		}
		sb.append('/').append(DocumentPojoIndexMap.documentType_);
		ElasticSearchManager indexMgr = ElasticSearchManager.getIndex(sb.toString());
		return indexMgr;
	}//TESTED (cut and paste from original code)
	
////////////////////////////////////////////////////////////////////////
	
	// 0.b.1) filter
	
	private static class InternalTempFilterInfo {
		BoolFilterBuilder parentFilterObj;
		String[] entityTypeFilterStrings;
		String[] assocVerbFilterStrings;
	}
	
	private InternalTempFilterInfo getBaseFilter(AdvancedQueryPojo query, String communityIdStrs[])
	{
		BoolFilterBuilder parentFilterObj = 
				FilterBuilders.boolFilter().must(FilterBuilders.termsFilter(DocumentPojo.communityId_, communityIdStrs));
			
		BoolFilterBuilder sourceFilter = this.parseSourceManagement(query.input);
		
		if (null != sourceFilter) {
			parentFilterObj = parentFilterObj.must(sourceFilter);
		}//TESTED
		
	// 0.2] Output filtering	
		
		// Output filters: parse (also used by aggregation, scoring)
		
		String[] entityTypeFilterStrings = null;
		String[] assocVerbFilterStrings = null;
		if ((null != query.output) && (null != query.output.filter)) {
			if (null != query.output.filter.entityTypes) {
				entityTypeFilterStrings = query.output.filter.entityTypes;
				if (0 == entityTypeFilterStrings.length) {
					entityTypeFilterStrings = null;
				}
				else if ((1 == entityTypeFilterStrings.length) && (entityTypeFilterStrings[0].isEmpty())) {
					entityTypeFilterStrings = null;					
				}
				// (note this is important because it means we can always check entityTypeFilterStrings[0].getCharAt(0) for -ve filtering)
			}
			if (null != query.output.filter.assocVerbs) {
				assocVerbFilterStrings = query.output.filter.assocVerbs;				
				if (0 == assocVerbFilterStrings.length) {
					assocVerbFilterStrings = null;
				}
				else if ((1 == assocVerbFilterStrings.length) && (assocVerbFilterStrings[0].isEmpty())) {
					assocVerbFilterStrings = null;					
				}
				// (note this is important because it means we can always check assocVerbFilterStrings[0].getCharAt(0) for -ve filtering)
			}
		}
		
		// Now apply output filters to query
		
		BoolFilterBuilder outputFilter = this.parseOutputFiltering(entityTypeFilterStrings, assocVerbFilterStrings);
		if (null != outputFilter) {
			parentFilterObj = parentFilterObj.must(outputFilter);
		}
		//TESTED

		InternalTempFilterInfo out = new InternalTempFilterInfo();
		out.parentFilterObj = parentFilterObj;
		out.entityTypeFilterStrings = entityTypeFilterStrings;
		out.assocVerbFilterStrings = assocVerbFilterStrings;
		
		return out;
	}//TESTED (cut/paste from original code)
	
////////////////////////////////////////////////////////////////////////
	
	// 0.b.2] Query
	// (if it returns null then call getSavedQueryInstead instead)
	
	private BaseQueryBuilder getBaseQuery(AdvancedQueryPojo query, String communityIdStrs[], String communityIdStrList, String userIdStr, StringBuffer querySummary)
	{
		// Intialize alias if so required:
		if ((null == query.expandAlias) || query.expandAlias) {
			AliasManager aliasManager = AliasManager.getAliasManager();
			if (null != aliasManager) {
				_aliasLookup = aliasManager.getAliasLookupTable(null, communityIdStrs, null, userIdStr);
			}
		}
		// (end initialize index)
		
		BaseQueryBuilder queryObj = null;
		
	// 0.1] Input data (/filtering)

		if (null != query.input.name) { // This is actually a share id visible to this user
			query = getStoredQueryArtefact(query.input.name, query, userIdStr);
		}
		
	// 0.3] Query terms
		
		int nQueryElements = 0;
		
		if (null != query.qt) {
			nQueryElements = query.qt.size();
			
			if ((1 == nQueryElements) && (null != query.qt.get(0).ftext) && (query.qt.get(0).ftext.startsWith("$cache:"))) {
				return null;
			}
			if (nQueryElements > 0) { // NORMAL CASE
				
				this.handleEntityExpansion(DbManager.getFeature().getEntity(), query.qt, userIdStr, communityIdStrList);
				
				BaseQueryBuilder queryElements[] = new BaseQueryBuilder[nQueryElements];
				StringBuffer sQueryElements[] = new StringBuffer[nQueryElements];
				for (int i = 0; i < nQueryElements; ++i) {
					_extraFullTextTerms = null;	
					
					queryElements[i] = this.parseQueryTerm(query.qt.get(i), (sQueryElements[i] = new StringBuffer()));
					
					// Extra full text terms generated by aliasing:
					if (null != _extraFullTextTerms) {
						BoolQueryBuilder extraTerms = QueryBuilders.boolQuery().should(queryElements[i]);
						StringBuffer discard = new StringBuffer(); // (we already have added the info the query elements)
						for (AdvancedQueryPojo.QueryTermPojo qtExtra: _extraFullTextTerms) {
							extraTerms = extraTerms.should(this.parseQueryTerm(qtExtra, discard));
						}
						queryElements[i] = extraTerms; 
						_extraFullTextTerms = null; // (reset ready for next term...) 
					}//TESTED				
					
				}//end loop over query terms
				
				queryObj = this.parseLogic(query.logic, queryElements, sQueryElements, querySummary);
				
				if (null == queryObj) { //error parsing logic
					throw new RuntimeException("Error parsing logic");
				}
			}
			else { //(QT exists but doesn't have any elements)
				queryObj = QueryBuilders.matchAllQuery();
				querySummary.append('*');
			}
		}//TESTED
		else {
			queryObj = QueryBuilders.matchAllQuery();
			querySummary.append('*');				
		} //(QT not specified)
		
		return queryObj;
	}//TESTED (cut/paste from original code)
	
////////////////////////////////////////////////////////////////////////
	
//1] QUERY UTILITIES	

////////////////////////////////////////////////////////////////////////
	
// 1.0] Stored queries/datasets

	// Saved queries (ie the entire dataset)
	
	private ResponsePojo getSavedQueryInstead(String storedQueryNameOrId, String[] communityIdStrs, AdvancedQueryPojo query) {
		ResponsePojo rp = null;
		ObjectId oid = null;
		BasicDBObject jobQuery = null;
		try {
			oid = new ObjectId(storedQueryNameOrId);
			jobQuery = new BasicDBObject(CustomMapReduceJobPojo._id_, oid);
		}
		catch (Exception e) {
			jobQuery = new BasicDBObject(CustomMapReduceJobPojo.jobtitle_, storedQueryNameOrId);			
		}
		CustomMapReduceJobPojo savedJob = CustomMapReduceJobPojo.fromDb(DbManager.getCustom().getLookup().findOne(jobQuery), CustomMapReduceJobPojo.class);
		
		if (null != savedJob) { // Is this even a saved job?
			if (null != savedJob.jarURL) {				
				savedJob = null;
			}
		}
		if (null != savedJob) { // Authorization
			boolean auth = false;
			String communityIdStrList = Arrays.toString(communityIdStrs);
			for (ObjectId commId: savedJob.communityIds) {
				
				if (communityIdStrList.contains(commId.toString())) {
					auth = true;
					break;
				}
			}
			if (!auth) {
				savedJob = null;
			}
			if (null == savedJob) {
				throw new RuntimeException("Can't find saved query, or is a custom job not a query, or authorization error");
			}
			// OK go get the results of the job
			DBCollection coll = DbManager.getCollection(savedJob.getOutputDatabase(), savedJob.outputCollection);
			BasicDBObject result = (BasicDBObject) coll.findOne(); // (at some point support multiple saved queries)
			if (null == result) {
				throw new RuntimeException("Saved query is empty");			
			}
			BasicDBObject apiResultToConvert = (BasicDBObject) result.get("value");
			if (null == apiResultToConvert) {
				throw new RuntimeException("Saved query has invalid format");			
			}
			rp = ResponsePojo.fromDb(apiResultToConvert);
		}
		else if (null != oid) { // Support new user/doc queues
			SharePojo share = SharePojo.fromDb(DbManager.getSocial().getShare().findOne(jobQuery), SharePojo.class);
			if ((null == share) || (null == share.getShare()) ||
					(!share.getType().equals(DocumentQueueControlPojo.UserQueue) && !share.getType().equals(DocumentQueueControlPojo.SavedQueryQueue))
					)
			{
				throw new RuntimeException("Can't find saved query, or is a custom job not a query, or authorization error");								
			}
			else { // share.share is a  DocumentQueueControlPojo
				DocumentQueueControlPojo queue = DocumentQueueControlPojo.fromApi(share.getShare(), DocumentQueueControlPojo.class);
				BasicDBObject docQuery1 = new BasicDBObject(DocumentPojo._id_, new BasicDBObject(DbManager.in_, queue.getQueueList()));
				BasicDBObject docQuery2 = new BasicDBObject(DocumentPojo.updateId_, new BasicDBObject(DbManager.in_, queue.getQueueList()));
				BasicDBObject docQuery = new BasicDBObject(DbManager.or_, Arrays.asList(docQuery1, docQuery2));
				DBCursor dbc = DbManager.getDocument().getMetadata().find(docQuery).limit(query.score.numAnalyze);
				ScoringUtils scoreStats = new ScoringUtils();
				List<BasicDBObject> docs = null;
				StatisticsPojo stats = new StatisticsPojo();
				stats.setSavedScores(query.output.docs.skip, dbc.count());
				try {
					boolean lockAcquired = true;
					try {
						lockAcquired = this.acquireConcurrentAccessLock();
						
					} catch (InterruptedException e) {
						//(that's fine just carry on)
						lockAcquired = false;
					}
					if (!lockAcquired) {
						rp.setResponse(new ResponseObject("Query", false, "Query engine busy, please try again later."));
						return rp;
					}
					scoreStats.setAliasLookupTable(_aliasLookup);
					docs = scoreStats.calcTFIDFAndFilter(DbManager.getDocument().getMetadata(), 
															dbc, query.score, query.output, stats, false,
															query.output.docs.skip, query.output.docs.numReturn, 
																		communityIdStrs,
																		null, null,
																		null, 
																		null, 
																		null, null,
																		null, null);
				}
				finally {
					scoreStats.clearAsMuchMemoryAsPossible();
					this.releaseConcurrentAccessLock();
				}
				rp = new ResponsePojo();
				rp.setResponse(new ResponseObject("Query", true, "Saved Query: " + share.getTitle()));
				rp.setStats(stats);
				if ((null != docs) && (docs.size() > 0)) {
					rp.setData(docs, (BasePojoApiMap<BasicDBObject>)null);
				}
				else { // (ensure there's always an empty list)
					docs = new ArrayList<BasicDBObject>(0);
					rp.setData(docs, (BasePojoApiMap<BasicDBObject>)null);
				}
			}//end if user or saved query queue
		}
		return rp;
		
	}//TESTED
	
	// Stored queries (ie just the query JSON)
	
	static AdvancedQueryPojo getStoredQueryArtefact(String shareIdStr, AdvancedQueryPojo query, String userIdStr) {
		
		ResponsePojo rp2 = new ShareHandler().getShare(userIdStr, shareIdStr, true);
		if ((null != rp2.getData() || !rp2.getResponse().isSuccess())) {
			SharePojo share = (SharePojo) rp2.getData();
			if (null != share) {
				if (share.getType().equalsIgnoreCase("dataset")) {
					query.input = new com.google.gson.Gson().fromJson(share.getShare(), AdvancedQueryPojo.QueryInputPojo.class);
				}
				else if (share.getType().equalsIgnoreCase("query")) {
					query = new com.google.gson.Gson().fromJson(share.getShare(), AdvancedQueryPojo.class);
				}
				else { // Unrecognized share
					throw new RuntimeException("Unexpected share type: " + share.getType());
				}
			}
			else {
				throw new RuntimeException("Invalid return from share: " + rp2.getData().toString());
			}
		}
		else {
			throw new RuntimeException(rp2.getResponse().getMessage());					
		}		
		return query;
	}
	
////////////////////////////////////////////////////////////////////////

// 1.1] Source management utility
	
	BoolFilterBuilder parseSourceManagement(AdvancedQueryPojo.QueryInputPojo input) {
		
		BoolFilterBuilder sourceFilter = null;
		
		if ((null != input.tags) || (null != input.typeAndTags) 
				|| (null != input.sources))
		{
			sourceFilter = FilterBuilders.boolFilter();
		}//TESTED
		
		if (null != input.tags) {
			sourceFilter = sourceFilter.should(FilterBuilders.termsFilter(DocumentPojo.tags_, input.tags.toArray()));
		}//TESTED '{ "input": { "tags": [ "healthcare", "cyber" ] } }'
		
		if (null != input.typeAndTags) {
			BoolFilterBuilder typeAndTagFilter = FilterBuilders.boolFilter();
			for (AdvancedQueryPojo.QueryInputPojo.TypeAndTagTermPojo tt: input.typeAndTags) {
				if (null != tt.tags) {
					typeAndTagFilter = typeAndTagFilter.should(
							FilterBuilders.boolFilter().must(FilterBuilders.termFilter(DocumentPojo.mediaType_, tt.type)).
														must(FilterBuilders.termsFilter(DocumentPojo.tags_, tt.tags.toArray())));
				}
				else {
					typeAndTagFilter = typeAndTagFilter.should(FilterBuilders.termFilter(DocumentPojo.mediaType_, tt.type));
				}
			}
			sourceFilter = sourceFilter.should(typeAndTagFilter);
		}//TESTED '{ "input": { "typeAndTags": [ { "type": "Social" }, { "type": "Video", "tags": [ "education", "MIT" ] } ] } }'
		
		if (null != input.sources) {
			if ((null == input.srcInclude) || input.srcInclude) {
				sourceFilter = sourceFilter.should(FilterBuilders.termsFilter(DocumentPojo.sourceKey_, input.sources.toArray()));						
			}
			else {
				sourceFilter = sourceFilter.mustNot(FilterBuilders.termsFilter(DocumentPojo.sourceKey_, input.sources.toArray()));						
			}
		}//TESTED '{ "input": { "srcInclude": false, "sources": [ "http.twitter.com.statuses.public_timeline.atom", "http.gdata.youtube.com.feeds.base.users.mit.uploads.alt=rss.v=2.orderby=published.client=ytapi-youtube-profile" ] } }'
		//(also "srcInclude" not set - checked got the complement of the result)
		
		return sourceFilter;
	}

////////////////////////////////////////////////////////////////////////
	
// 1.X2] Output filter parsing	

	private BoolFilterBuilder addNegativeSelectorToFilter(EntityFeaturePojo docDiscardAlias, BoolFilterBuilder outputFilter, int recursionLevel) {
		if ((null != docDiscardAlias.getAlias()) && !docDiscardAlias.getAlias().isEmpty()) {
			if (null == outputFilter) {
				outputFilter = FilterBuilders.boolFilter();				
			}
			outputFilter = outputFilter.mustNot(FilterBuilders.nestedFilter(DocumentPojo.entities_, 
												FilterBuilders.termsFilter(EntityPojo.docQuery_index_, docDiscardAlias.getAlias().toArray())));
			
			if (recursionLevel <= 1) { // (only go two deep for now)
				for (String aliasIndex: docDiscardAlias.getAlias()) {
					EntityFeaturePojo docDiscardSubAlias = _aliasLookup.getAliases(aliasIndex);
					if (null != docDiscardSubAlias) {
						outputFilter = addNegativeSelectorToFilter(docDiscardSubAlias, outputFilter, 1 + recursionLevel);
					}
				}//TESTED
			}
			
		}//TESTED (by hand)
		if (null != docDiscardAlias.getSemanticLinks()) { // (recall: we've abused this field for text queries)
			for (String textQuery: docDiscardAlias.getSemanticLinks()) {
				//(probably not a very efficient construct, but nothing about this is efficient, just functional, so we'll leave it for now)
				outputFilter = outputFilter.mustNot(FilterBuilders.queryFilter(
						CrossVersionQueryBuilders.matchPhraseQuery(DocumentPojo.fullText_, textQuery)));
				outputFilter = outputFilter.mustNot(FilterBuilders.queryFilter(
						CrossVersionQueryBuilders.matchPhraseQuery("_all", textQuery)));
			}
		}//TESTED (by hand)
		return outputFilter;
	}//TESTED
	
	BoolFilterBuilder parseOutputFiltering(String[] entityTypeFilterStrings, String[] assocVerbFilterStrings)
	{
		BoolFilterBuilder outputFilter = null;
		
		// First off: document discard aliases:
		
		if (null != _aliasLookup) { // Check out the document discard table...			
			EntityFeaturePojo docDiscardAlias = _aliasLookup.getAliases("DOCUMENT_DISCARD");
			if (null != docDiscardAlias) {				
				outputFilter = addNegativeSelectorToFilter(docDiscardAlias, outputFilter, 0);
			}						
		}//TESTED (by hand, nothing repeatable)
		
		// Other simple filter types:
		
		if (null != entityTypeFilterStrings) {
			if ('-' != entityTypeFilterStrings[0].charAt(0)) { // (negative entity type, don't add to filter)
				outputFilter = FilterBuilders.boolFilter();
								
				outputFilter.must(FilterBuilders.nestedFilter(DocumentPojo.entities_, 
						FilterBuilders.termsFilter(EntityPojo.docQuery_type_, entityTypeFilterStrings)));
			}
		}
		if (null != assocVerbFilterStrings) {
			if ('-' != assocVerbFilterStrings[0].charAt(0)) { // (negative verb, don't add to filter)
				if (null == outputFilter) {
					outputFilter = FilterBuilders.boolFilter();				
				}
				BoolFilterBuilder verbFilter = FilterBuilders.boolFilter();	
				StringBuffer sb = new StringBuffer();
				for (String assocVerb: assocVerbFilterStrings) {
					sb.setLength(0);
					sb.append('"').append(assocVerb).append('"');
					verbFilter.should(FilterBuilders.nestedFilter(DocumentPojo.associations_, 
							QueryBuilders.queryString(sb.toString()).field(AssociationPojo.docQuery_verb_category_)));
					//(closest to exact that we can manage, obv verb_cat should actually be not_analyzed)
				}
				outputFilter.must(verbFilter);
			}
		}		
		return outputFilter;
	}//TESTED
	
////////////////////////////////////////////////////////////////////////

// 1.2] Query term parsing

	// (Not needed any more, but kept here for illustrative purposes)
	//private static Pattern _luceneExactPattern = Pattern.compile("([\"+~*?:|&(){}\\[\\]\\^\\!\\-\\\\ ])");	
	private BaseQueryBuilder parseQueryTerm(AdvancedQueryPojo.QueryTermPojo qt, StringBuffer sQueryTerm) {
		
		BaseQueryBuilder term = null;
		BoolQueryBuilder boolTerm = null;
		
		sQueryTerm.setLength(0);
		sQueryTerm.append('(');

	// 1.1] Free text (Lucene)	
		
		boolean nonEmpty = false;
		
		if (null != qt.ftext) { // NOTE term building code below depends on this being 1st clause
			nonEmpty = true;
			if (qt.ftext.startsWith("$cache")) { // currently not supported
				throw new RuntimeException("Don't currently support nested cached queries - coming soon.");
			}
			
			sQueryTerm.append('(');
			if (null != qt.metadataField) {
				sQueryTerm.append(qt.metadataField).append(':');				
			}
			sQueryTerm.append(qt.ftext);			
			sQueryTerm.append(')');
			if (null != qt.metadataField) { // Metadata only
				term = QueryBuilders.queryString(qt.ftext).field(qt.metadataField);
			}
			else {
				term = QueryBuilders.queryString(qt.ftext).field("_all").field(DocumentPojo.fullText_);				
			}
			if ((null == _scoringParams) || (null == _scoringParams.adjustAggregateSig)) { // auto-decide .. if ftext is set and is non-trivial
				if (qt.ftext.contains(" ")) {
					_scoringParams.adjustAggregateSig = true;
				}
			}
		}//TESTED (logic0)
		
	// 1.2] Exact text	
		
		if (null != qt.etext) { // NOTE term building code below depends on this being 2nd clause
			nonEmpty = true;
			BaseQueryBuilder termQ = null;
			if (sQueryTerm.length() > 1) {
				sQueryTerm.append(" AND ");
			}
			if (qt.etext.equals("*")) { // Special case
				termQ = QueryBuilders.matchAllQuery();
			}
			else { // Normal query
				if (null != qt.metadataField) { // Metadata only
					termQ = CrossVersionQueryBuilders.matchPhraseQuery(qt.metadataField, qt.etext);
				}
				else { // Normal query
					termQ = QueryBuilders.boolQuery().
						should(CrossVersionQueryBuilders.matchPhraseQuery("_all", qt.etext)).
						should(CrossVersionQueryBuilders.matchPhraseQuery(DocumentPojo.fullText_, qt.etext));
				}
			}
			sQueryTerm.append('(');
			if (null != qt.metadataField) {
				sQueryTerm.append(qt.metadataField).append(':');				
			}
			sQueryTerm.append('"');
			sQueryTerm.append(qt.etext);			
			sQueryTerm.append("\")");
			if (null == term) {
				term = termQ;
			}
			else {
				term = (boolTerm = QueryBuilders.boolQuery().must(term).must(termQ));
			}
		}//TESTED (logic1)
		
		// Here's where it starts getting interesting:
	
	// 1.3] Entity 	
		
		if ((null != qt.entity) || (null != qt.entityValue) || ((null == qt.assoc) && (null != qt.sentiment))) { // (if no association specified then sentiment applies to entities)
			nonEmpty = true;
			if (sQueryTerm.length() > 1) {
				sQueryTerm.append(" AND ");
			}
			sQueryTerm.append('(');
			
			BaseQueryBuilder termQ = QueryBuilders.nestedQuery(DocumentPojo.entities_, this.parseEntityTerm(qt, sQueryTerm, EntityPojo.docQuery_index_)).scoreMode("max").boost((float)1.0);
			
			if (null == term) {
				term = termQ;
			}
			else if (null == boolTerm) {
				term = (boolTerm = QueryBuilders.boolQuery().must(term).must(termQ));
			}
			else {
				term = (boolTerm = boolTerm.must(termQ));
			}
			sQueryTerm.append(')');
			
		}//TESTED: logic2* TOTEST: alias expansion code (logic3)
				
	// 1.4] Dates
		
		if (null != qt.time) {
			nonEmpty = true;
			if (sQueryTerm.length() > 1) {
				sQueryTerm.append(" AND ");
			}
			sQueryTerm.append('(');			
			
			BaseQueryBuilder termQ = this.parseDateTerm(qt.time, sQueryTerm, (null != qt.entityOpt) && qt.entityOpt.lockDate);
			
			if (null == term) {
				term = termQ;
			}
			else if (null == boolTerm) {
				term = (boolTerm = QueryBuilders.boolQuery().must(term).must(termQ));
			}
			else {
				term = (boolTerm = boolTerm.must(termQ));
			}
			sQueryTerm.append(')');
			
		}//TESTED (logic5-10)

	// 1.5] Geo	
		
		if (null != qt.geo) 
		{
			nonEmpty = true;
			if (sQueryTerm.length() > 1) 
			{
				sQueryTerm.append(" AND ");
			}
			sQueryTerm.append('(');
			
			BaseQueryBuilder termQ = this.parseGeoTerm(qt.geo, sQueryTerm, GeoParseField.ALL);
			if (null != termQ) 
			{
				if (null == term) 
				{
					term = termQ;
				}
				else if (null == boolTerm) 
				{
					term = (boolTerm = QueryBuilders.boolQuery().must(term).must(termQ));
				}
				else 
				{
					term = (boolTerm = boolTerm.must(termQ));
				}				
			}
			
			sQueryTerm.append(')');					
		} // (end geo)
		
		if (null == qt.assoc) qt.assoc = qt.event;
			//(continue to support the old "event" name for another release)
		if (null != qt.assoc) {
			nonEmpty = true;
			if (sQueryTerm.length() > 1) {
				sQueryTerm.append(" AND ");
			}
			sQueryTerm.append('(');
			
			BaseQueryBuilder termQ = QueryBuilders.nestedQuery(DocumentPojo.associations_, this.parseAssociationTerm(qt.assoc, qt.sentiment, sQueryTerm));
			if (null != termQ) {
				if (null == term) {
					term = termQ;
				}
				else if (null == boolTerm) {
					term = (boolTerm = QueryBuilders.boolQuery().must(term).must(termQ));
				}
				else {
					term = (boolTerm = boolTerm.must(termQ));
				}				
			}
			
			sQueryTerm.append(')');								
		} // (end event)

		if (!nonEmpty) {
			throw new RuntimeException("One+ of your query terms is empty!");
		}//TOTEST
		
		sQueryTerm.append(')');					
		return term;
		
	}//TESTED (logic*) TOTEST event logic

	//TESTED: each of the above cases with the following GUI commands:
//	infiniteService.send('{"raw": { "match_all": {} } }');
//	infiniteService.send('{ "input": { "tags": [ "healthcare", "cyber" ] } }');
//	infiniteService.send('{ "input": { "typeAndTags": [ { "type": "Social" }, { "type": "Video", "tags": [ "education", "MIT" ] } ] } }');
//	infiniteService.send('{ "input": { "typeAndTags": [ { "type": "Social" }, { "type": "Video", "tags": [ "education", "MIT" ] } ] } }');
//	infiniteService.send('{ "input": { "sources": [ "http.twitter.com.statuses.public_timeline.atom", "http.gdata.youtube.com.feeds.base.users.mit.uploads.alt=rss.v=2.orderby=published.client=ytapi-youtube-profile" ] } }');
//	infiniteService.send('{ "input": { "srcInclude": false, "sources": [ "http.twitter.com.statuses.public_timeline.atom", "http.gdata.youtube.com.feeds.base.users.mit.uploads.alt=rss.v=2.orderby=published.client=ytapi-youtube-profile" ] } }');
//	infiniteService.send('{ "qt": [ { "etext":"barack obama" } ] }'); // (148 results)
//	infiniteService.send('{ "qt": [ { "ftext":"barack obama" } ] }'); // (790 results) 
//	infiniteService.send('{ "qt": [ { "ftext":"+barack +obama" } ] }'); // (151 results)
//	infiniteService.send('{ "qt": [ { "entity":"barack obama/person" } ] }'); // (132 results)
//	infiniteService.send('{ "qt": [ { "time": { "min": "20100301", "max": "20100310" } } ] }'); // (worked - by inspection of timeline)
//	infiniteService.send('{ "qt": [ { "geo": { "centerll": "(29.9569263889,15.7460923611)", "dist": "100mi" } } ] }'); //(259 results)
//	infiniteService.send('{ "qt": [ { "geo": { "minll": "(28.9569263889,14.7460923611)", "maxll": "(30.9569263889,16.7460923611)" } } ] }'); //(259 results)
	
	////////////////////////////////////////////////////////////////////////
	
	// 1.2.1] Entity term parsing
	
	BaseQueryBuilder parseEntityTerm(AdvancedQueryPojo.QueryTermPojo qt, StringBuffer sQueryTerm, String sFieldName)
	{
		BaseQueryBuilder termQ = null;
		
		// 1.3a] Entity decomposed	
		
		if (null != qt.entityValue) 
		{			
			qt.entityValue = qt.entityValue.toLowerCase();
			if (null == qt.entityType) { // Allow arbitrary types
				termQ = QueryBuilders.prefixQuery(sFieldName, qt.entityValue + "/");
				sQueryTerm.append(sFieldName).append(":\"").append(qt.entityValue).append("/*\"");
			}
			else { //Equivalent to above
				qt.entityType = qt.entityType.toLowerCase();
				qt.entity = qt.entityValue + "/" + qt.entityType;				
			}
			
		}//TESTED (use logic3f, logic3e)
		
	// 1.3b] Entity index	
		
		else if (null != qt.entity) 
		{
			qt.entity = qt.entity.toLowerCase();
			
			int nIndex1 = qt.entity.lastIndexOf(':');
			int nIndex2 = qt.entity.lastIndexOf('/');

			if (nIndex1 > nIndex2) {
				qt.entity = qt.entity.substring(0, nIndex1) + "/" + qt.entity.substring(nIndex1 + 1);
			}//TESTED logic2
			
		}//TESTED: logic2 
		
	// 1.3c] Logic	
		
		if ((null == termQ) && (null != qt.entity)) { // entities.index or fully-specified value,type
			
			sQueryTerm.append(sFieldName).append(":\"").append(qt.entity).append('"');
			
			// Just leave this fixed for entity expansion since we don't really support events anyway
			// we'll have to sort this out later
			BoolQueryBuilder termBoolQ = null;
			
			if ((null != qt.entityOpt) && qt.entityOpt.expandAlias) {
				// Alias expansion code
				// Easy bit:
				 termBoolQ = QueryBuilders.boolQuery().should(QueryBuilders.termQuery(sFieldName, qt.entity));
				// Interesting bit:
				if (null != _tmpAliasMap) {
					String[] terms = _tmpAliasMap.get(qt.entity).toArray(new String[0]);
					if ((null != terms) && (terms.length > 0)) {
						termQ = termBoolQ.should(QueryBuilders.termsQuery(EntityPojo.docQuery_actual_name_, terms));
						sQueryTerm.append(" OR actual_name:$aliases");
					}
				}
			}//TESTED logic3a,b,f - actual_name expansion
			
			boolean isKeyword = false;
			if (null != qt.entity) {
				isKeyword = qt.entity.endsWith("/keyword");
			}
			else if (null != qt.entityType) {
				isKeyword = qt.entityType.equalsIgnoreCase("Keyword");
			}
			
			if (((null != qt.entityOpt) && qt.entityOpt.rawText) || isKeyword)
			{
				if (null == this._extraFullTextTerms) {
					_extraFullTextTerms = new LinkedList<AdvancedQueryPojo.QueryTermPojo>();
				}
				String dName = qt.entityValue; 
				if (null == dName) { // Derive disambiguated name if not directly specified
					int nIndex2 = qt.entity.lastIndexOf('/');
					dName = qt.entity.substring(0, nIndex2);
				}//TESTED

				if (EntityPojo.docQuery_index_ == sFieldName) { // (note: can use pointers here)
					AdvancedQueryPojo.QueryTermPojo qtExtra = new AdvancedQueryPojo.QueryTermPojo();
					qtExtra.etext = dName;
					_extraFullTextTerms.add(qtExtra);
					sQueryTerm.append(" OR ((\"").append(dName).append('"').append("))");
				}
				else if (AssociationPojo.docQuery_geo_index_ != sFieldName) { // (geo has no non-indexed form)
					String nonIndexField = (AssociationPojo.docQuery_entity1_index_ == sFieldName) ? AssociationPojo.docQuery_entity1_ : AssociationPojo.docQuery_entity2_;
					if (null == termBoolQ) {
						termBoolQ = QueryBuilders.boolQuery();
					}
					termQ = termBoolQ = termBoolQ.should(CrossVersionQueryBuilders.matchPhraseQuery(nonIndexField, dName));
					sQueryTerm.append(" OR ").append(nonIndexField).append(":\"").append(dName).append('"');					
				}//TESTED
			}
			//TESTED (entity+association) - entity options, add dname as exact text query
			
			if (null != _aliasLookup) {
				EntityFeaturePojo masterAlias = _aliasLookup.getAliases(qt.entity);	
					// (need to do it this way round to get the semantic links)
				if (null != masterAlias) {
					if (null == termBoolQ) {
						termBoolQ = QueryBuilders.boolQuery();
					}
					sQueryTerm.append(" OR ").append(sFieldName).append(":$manual_aliases");
					termBoolQ = termBoolQ.should(QueryBuilders.termQuery(sFieldName, qt.entity));
					termQ = termBoolQ = termBoolQ.should(QueryBuilders.termsQuery(sFieldName, masterAlias.getAlias().toArray()));

					// If want to add manual aliases as full text also...
					if ((null != qt.entityOpt) && qt.entityOpt.rawText) {
						if (null == this._extraFullTextTerms) {
							_extraFullTextTerms = new LinkedList<AdvancedQueryPojo.QueryTermPojo>();
						}
						String nonIndexField = null;
						if (AssociationPojo.docQuery_geo_index_ != sFieldName) { // (geo has no non-indexed form)
							nonIndexField = (AssociationPojo.docQuery_entity1_index_ == sFieldName) ? AssociationPojo.docQuery_entity1_ : AssociationPojo.docQuery_entity2_;
						}
						
						// (slightly annoying because I have to derive the dnames for all of them) 
						for (String alias: masterAlias.getAlias()) {
							int nIndex2 = alias.lastIndexOf('/');
							String dName = alias.substring(0, nIndex2);
							
							if (EntityPojo.docQuery_index_ == sFieldName) { // (note: can use pointers here)
								AdvancedQueryPojo.QueryTermPojo qtExtra = new AdvancedQueryPojo.QueryTermPojo();
								qtExtra.etext = dName;
								_extraFullTextTerms.add(qtExtra);
							}
							else if (null != nonIndexField) {
								termQ = termBoolQ = termBoolQ.should(CrossVersionQueryBuilders.matchPhraseQuery(nonIndexField, dName));
							}
						}
						if (EntityPojo.docQuery_index_ == sFieldName) { // (note: can use pointers here)
							sQueryTerm.append(" OR (($manual_aliases").append("))");
						}
						else if (null != nonIndexField) {
							sQueryTerm.append(" OR ").append(nonIndexField).append(":$manual_aliases");												
						}
						
					}//TESTED (entity + association) - aliases #1
					
					// Recall: we're abusing linkdata to contain aliases:
					if ((null != masterAlias.getSemanticLinks()) && !masterAlias.getSemanticLinks().isEmpty()) {
						String nonIndexField = null;
						if (AssociationPojo.docQuery_geo_index_ != sFieldName) { // (geo has no non-indexed form)
							nonIndexField = (AssociationPojo.docQuery_entity1_index_ == sFieldName) ? AssociationPojo.docQuery_entity1_ : AssociationPojo.docQuery_entity2_;
						}
						
						if (null == this._extraFullTextTerms) {
							_extraFullTextTerms = new LinkedList<AdvancedQueryPojo.QueryTermPojo>();
						}
						for (String textAlias: masterAlias.getSemanticLinks()) {
							if (EntityPojo.docQuery_index_ == sFieldName) { // (note: can use pointers here)
								AdvancedQueryPojo.QueryTermPojo qtExtra = new AdvancedQueryPojo.QueryTermPojo();
								qtExtra.etext = textAlias;
								_extraFullTextTerms.add(qtExtra);
							}
							else if (null != nonIndexField) {							
								termQ = termBoolQ = termBoolQ.should(CrossVersionQueryBuilders.matchPhraseQuery(nonIndexField, textAlias));
							}
						}
						if (EntityPojo.docQuery_index_ == sFieldName) { // (note: can use pointers here)
							sQueryTerm.append(" OR (($manual_text_aliases").append("))");
						}
						else if (null != nonIndexField) {
							sQueryTerm.append(" OR ").append(nonIndexField).append(":$manual_text_aliases");												
						}
						
					}//TESTED (entity + association) - aliases #2
				}
			}//TESTED (by hand) - alias lookups
			
			if (null == termQ) {
				termQ = QueryBuilders.termQuery(sFieldName, qt.entity);				
			}
			
		} //TESTED logic3* - end if entity options (apart from sentiment, which is handled below)
						
		// Sentiment... only apply for entities (association sentiment is handled in parseAssociationTerm)
		
		if ((null != qt.sentiment) && (EntityPojo.docQuery_index_ == sFieldName)) { // (note: can use pointers here)
			RangeQueryBuilder sentimentQ = QueryBuilders.rangeQuery(EntityPojo.docQuery_sentiment_);
			if (null != qt.sentiment.min) {
				sentimentQ.from(qt.sentiment.min);
			}
			if (null != qt.sentiment.max) {
				sentimentQ.to(qt.sentiment.max);					
			}
			if (null == termQ) {
				termQ = sentimentQ;
				sQueryTerm.append("sentiment:[").append(qt.sentiment.min).append(',').append(qt.sentiment.max).append(']');
			}
			else {
				termQ = QueryBuilders.boolQuery().must(termQ).must(sentimentQ);
				sQueryTerm.append(" AND ").append("sentiment:[").append(qt.sentiment.min).append(',').append(qt.sentiment.max).append(']');
					// (don't mind the nulls in the range)
			}
		}//TESTED (combined sentiment and standalone sentiment)
		
		return termQ;
	}
	
	/////////////////////////////////////
	
	private Set<String> _tmpEntityExpansionList = null;
	private Map<String, Set<String>> _tmpAliasMap = null;
	
	// 1.2.1.2] Utility function for alias expansion
	
	void handleEntityExpansion(DBCollection entityFeatureDb, List<AdvancedQueryPojo.QueryTermPojo> qtList, String userIdStr, String communityIdList) {
		for (AdvancedQueryPojo.QueryTermPojo qt: qtList) {
			if ((null != qt.entityOpt) && qt.entityOpt.expandAlias) {
				String s = null;
				if (null != qt.entity) {
					int nIndex1 = qt.entity.lastIndexOf(':');
					int nIndex2 = qt.entity.lastIndexOf('/');
					if (nIndex1 > nIndex2) {
						s = qt.entity.substring(0, nIndex1) + "/" + qt.entity.substring(nIndex1 + 1);
					}//TESTED logic2 (cut and paste)
					else {
						s = qt.entity;
					}
				}
				else if ((null != qt.entityValue) && (null != qt.entityType)) {
					s = qt.entityValue + "/" + qt.entityType;
				}
				if (null != s) {
					if (null == _tmpEntityExpansionList) {
						_tmpEntityExpansionList = new TreeSet<String>();
					}
					_tmpEntityExpansionList.add(s);
				}
			}//(end if alias specified)
		} // (end loop over query terms)
		
		if (null != _tmpEntityExpansionList) {
			try {
				_tmpAliasMap = SearchHandler.findAliases(entityFeatureDb, EntityPojo.index_, _tmpEntityExpansionList, userIdStr, communityIdList);
				//(NOTE: this is intentionally _not_ EntityPojo.docQuery_index, that's just for referencing nested queries/filters in elasticsearch)
			}
			catch (Exception e) {
				// Just carry on without expansion
			}
		}
		
	} //TESTED (logic3 - cases: {entity, entityValue+entityType, entityValue, none of above})
	
	////////////////////////////////////////////////////////////////////////
	
	// 1.2.2] Date term parsing
	
	private long _nNow = 0;
	
	BaseQueryBuilder parseDateTerm(AdvancedQueryPojo.QueryTermPojo.TimeTermPojo time, StringBuffer sQueryTerm, boolean lockMaxToNow)
	{
		return parseDateTerm(time, sQueryTerm, DocumentPojo.publishedDate_, lockMaxToNow);
	}
	private BaseQueryBuilder parseDateTerm(AdvancedQueryPojo.QueryTermPojo.TimeTermPojo time, StringBuffer sQueryTerm, String sFieldName, boolean lockMaxToNow)
	{
		BaseQueryBuilder termQ = null;
		long nMinTime = 0L; 
		long nMaxTime = _nNow;
		Interval interval = parseMinMaxDates(time, nMinTime, nMaxTime, lockMaxToNow);
		nMinTime = interval.getStartMillis();
		nMaxTime = interval.getEndMillis();
		
		termQ = QueryBuilders.constantScoreQuery(
				FilterBuilders.numericRangeFilter(sFieldName).from(nMinTime).to(nMaxTime)).boost((float)1.0);

		sQueryTerm.append(sFieldName).append(":[").
			append(0==nMinTime?"0":new Date(nMinTime)).append(" TO ").append(new Date(nMaxTime)).append(']');		
		
		return termQ;
	}

	// Don't currently have a use for this guy - would be part of a combined time query?
//	private BaseQueryBuilder parseMonthTerm(AdvancedQueryPojo.QueryTermPojo.TimeTermPojo time, StringBuffer sQueryTerm, String sFieldName)
//	{
//		BaseQueryBuilder termQ = null;
//		long nMinTime = 0L; 
//		long nMaxTime = _nNow;
//		Interval interval = parseMinMaxDates(time, nMinTime, nMaxTime);
//		nMinTime = interval.getStartMillis();
//		nMaxTime = interval.getEndMillis();
//		
//		// Convert min/max dates to YYYYMM
//		Calendar c = Calendar.getInstance();
//		c.setTime(new Date(nMinTime));
//		nMinTime = (c.get(Calendar.YEAR)*100 + c.get(Calendar.MONTH)+1L);
//		c.setTime(new Date(nMaxTime));
//		nMaxTime = (c.get(Calendar.YEAR)*100 + c.get(Calendar.MONTH)+1L);
//		
//		termQ = QueryBuilders.constantScoreQuery(
//				FilterBuilders.numericRangeFilter(sFieldName).from(nMinTime).to(nMaxTime)).boost((float)1.0);
//
//		sQueryTerm.append("association.").append(sFieldName).append(":[").
//			append(nMinTime).append(" TO ").append(nMaxTime).append(']');		
//		
//		return termQ;
//	}
	
	// 1.2.2.1] Even lower level date parsing
	
	public static Interval parseMinMaxDates(AdvancedQueryPojo.QueryTermPojo.TimeTermPojo time, long nMinTime, long nMaxTime, boolean lockMaxToNow) {
		
		if ((null != time.min) && (time.min.length() > 0)) {
			if (time.min.equals("now")) { 
				nMinTime = nMaxTime;
			}
			else if (time.min.startsWith("now")) { // now+N[hdmy] 
				// now+Xi or now-Xi
				long sgn = 1L;
				if ('-' == time.min.charAt(3)) { //now+ or now-
					sgn = -1L;
				}
				long offset = sgn*getInterval(time.min.substring(4), 'd'); // (default interval is day)
				nMinTime = nMaxTime + offset; // (maxtime is now)
			}
			else if (time.min.equals("midnight")) { 
				nMinTime = DateUtils.truncate(new Date(nMaxTime), Calendar.DAY_OF_MONTH).getTime();
			}
			else if (time.min.startsWith("midnight")) { // midnight+N[hdmy] 
				// midnight+Xi or midnight-Xi
				long sgn = 1L;
				if ('-' == time.min.charAt(8)) { //now+ or now-
					sgn = -1L;
				}
				long offset = sgn*getInterval(time.min.substring(9), 'd'); // (default interval is day)
				nMinTime = DateUtils.truncate(new Date(nMaxTime), Calendar.DAY_OF_MONTH).getTime() + offset; // (maxtime is now)
			}
			else {
				try {
					nMinTime = Long.parseLong(time.min); // (unix time format)
					if (nMinTime <= 99999999) { // More likely to be YYYYMMDD
						// OK try a bunch of common date parsing formats
						nMinTime = parseDate(time.min);							
					} // TESTED for nMaxTime
				}
				catch (NumberFormatException e) {
					// OK try a bunch of common date parsing formats
					nMinTime = parseDate(time.min);
				}
			}
		}
		if ((null != time.max) && (time.max.length() > 0)) {
			if (time.max.equals("midnight")) { 
				nMaxTime = DateUtils.truncate(new Date(nMaxTime), Calendar.DAY_OF_MONTH).getTime();
			}
			else if (time.max.startsWith("midnight")) { // midnight+N[hdmy] 
				// midnight+Xi or midnight-Xi
				long sgn = 1L;
				if ('-' == time.max.charAt(8)) { //now+ or now-
					sgn = -1L;
				}
				long offset = sgn*getInterval(time.min.substring(9), 'd'); // (default interval is day)
				nMaxTime = DateUtils.truncate(new Date(nMaxTime), Calendar.DAY_OF_MONTH).getTime() + offset; // (maxtime is now)
			}
			else if (!time.max.equals("now")) { // (What we have by default)
				if (time.max.startsWith("now")) { // now+N[hdmy] 
					// now+Xi or now-Xi
					long sgn = 1L;
					if ('-' == time.max.charAt(3)) { //now+ or now-
						sgn = -1L;
					}
					long offset = sgn*getInterval(time.max.substring(4), 'd'); // (default interval is day)
					nMaxTime = nMaxTime + offset; // (maxtime is now)
				}
				else {
					try {
						nMaxTime = Long.parseLong(time.max); // (unix time format)
						if (nMaxTime <= 99999999) { // More likely to be YYYYMMDD
							// OK try a bunch of common date parsing formats
							nMaxTime = parseDate(time.max);
							
							// max time, so should be 24h-1s ahead ...
							nMaxTime = nMaxTime - (nMaxTime % (24*3600*1000));
							nMaxTime += 24*3600*1000 - 1;										
							
						} //TESTED (logic5, logic10 for maxtime)
					}
					catch (NumberFormatException e) {
						// OK try a bunch of common date parsing formats
						nMaxTime = parseDate(time.max);
						
						// If day only is specified, should be the entire day...
						if (!time.max.contains(":")) {
							nMaxTime = nMaxTime - (nMaxTime % (24*3600*1000));
							nMaxTime += 24*3600*1000 - 1;							
						}
					}//TOTEST max time
				}
			}				
		} //TESTED (logic5)

		
		if (lockMaxToNow) {
			if ((null == time.max) || time.max.isEmpty()) {
				nMinTime = new Date().getTime();
				nMaxTime = Long.MAX_VALUE;
			}
			else { // set max to now
				long now = new Date().getTime();
				if ((null != time.min) && !time.min.isEmpty()) { // set min also
					nMinTime = nMinTime + (now - nMaxTime);
				}
				nMaxTime = now;
			}
		}//TESTED (by hand)
		if (nMinTime > nMaxTime) { //swap these round if not ordered correctly
			long tmp = nMinTime;
			nMinTime = nMaxTime;
			nMaxTime = tmp;
		}//TESTED (by hand)
		
		return new Interval(nMinTime, nMaxTime);		
	}
	
	////////////////////////////////////////////////////////////////////////
	
	// 1.2.2] Geo term parsing
	//(the fieldName is always locs normally, geotag for child events, events.geotag for subevents)
	
	/**
	 * Parses the GeoTermPojo arguments into a lucene query.  Currently there are multiple geo options
	 * 
	 * 1. Center lat/lng with radius (args centerll && dist)
	 * 2. Bouding box, top left, bottom right corners (args minll, maxll)
	 * NOT IMPLEMENTED 3. Geo name search (args name, OPTIONAL dist)  
	 * NOT IMPLEMENTED 4. Polygon search (args poly OPTIONAL dist)
	 * 
	 * OPTIONAL for all arg ontology_type (will apply a heuristic search only grabbing onts that level and below
	 * 
	 */
	BaseQueryBuilder parseGeoTerm(AdvancedQueryPojo.QueryTermPojo.GeoTermPojo geo, StringBuffer sQueryTerm, GeoParseField parseFields)
	{
		BoolQueryBuilder boolQ = QueryBuilders.boolQuery().minimumNumberShouldMatch(1);
		List<String> ont_terms = null;
		//Get ontology types
		if ( null != geo.ontology_type )
		{			
			//get all ontology terms we are looking for
			ont_terms = GeoOntologyMapping.getOntologyList(geo.ontology_type);	
		}
		else 
		{
			ont_terms = GeoOntologyMapping.getOntologyList(null);				
		}
		
		if ((null != geo.centerll) && (null != geo.dist)) 
		{
			double lat, lon;
			
			if ('(' == geo.centerll.charAt(0)) {
				geo.centerll = geo.centerll.substring(1, geo.centerll.length() - 1);
			}
			String[] latlon = geo.centerll.split("\\s*,\\s*");
			if (2 == latlon.length) 
			{
				lat = Double.parseDouble(latlon[0]);
				lon = Double.parseDouble(latlon[1]);
				
				char c = geo.dist.charAt(geo.dist.length() - 1);
				if ((c < 0x30) || (c > 0x39)) // not a digit, difference calculation is different
				{ 				
					//ENT
					//Add in ontology_type if necessary
					//in the end this results in query = CURR_GEO_QUERY AND (ONT_TYPE = [ont1 OR ont2 OR ont3])			
					if ( parseFields == GeoParseField.ALL || parseFields == GeoParseField.ENT )
					{						
						//use a 2nd variable so we dont have to keep casting termQ to BoolQuery
						BoolQueryBuilder subQ = QueryBuilders.boolQuery().must(QueryBuilders.constantScoreQuery(FilterBuilders.geoDistanceFilter(EntityPojo.docQuery_geotag_).distance(geo.dist).point(lat, lon)).boost(1.0F));
						subQ.must(QueryBuilders.termQuery(EntityPojo.docQuery_ontology_type_, ont_terms.toArray()));	
						boolQ.should(QueryBuilders.nestedQuery(DocumentPojo.entities_, subQ).scoreMode("max").boost((float)1.0));
					}
					
					//ASSOC AND DOCGEO (only added if ont is point or null)
					if ( parseFields == GeoParseField.ALL || parseFields == GeoParseField.ASSOC )
						boolQ.should(QueryBuilders.nestedQuery(DocumentPojo.associations_, FilterBuilders.geoDistanceFilter(AssociationPojo.docQuery_geotag_).distance(geo.dist).point(lat, lon)).scoreMode("max").boost((float)1.0));
					if ( parseFields == GeoParseField.ALL || parseFields == GeoParseField.DOC )
						boolQ.should(QueryBuilders.constantScoreQuery(FilterBuilders.geoDistanceFilter(DocumentPojo.docGeo_).distance(geo.dist).point(lat, lon)));					
				}
				else // (identical to the above except geo distance parsing is different)
				{					
					//ENT
					//Add in ontology_type if necessary
					//in the end this results in query = CURR_GEO_QUERY AND (ONT_TYPE = [ont1 OR ont2 OR ont3])	
					if ( parseFields == GeoParseField.ALL || parseFields == GeoParseField.ENT )
					{
						//use a 2nd variable so we dont have to keep casting termQ to BoolQuery
						BoolQueryBuilder subQ = QueryBuilders.boolQuery().must(QueryBuilders.constantScoreQuery(FilterBuilders.geoDistanceFilter(EntityPojo.docQuery_geotag_).distance(Double.parseDouble(geo.dist), DistanceUnit.KILOMETERS).point(lat, lon)).boost(1.0F));
						subQ.must(QueryBuilders.termsQuery(EntityPojo.docQuery_ontology_type_, ont_terms.toArray()));													
						boolQ.should(QueryBuilders.nestedQuery(DocumentPojo.entities_, subQ).scoreMode("max").boost((float)1.0));
					}
					//ASSOC AND DOCGEO (only added if ont is point or null)
					if ( parseFields == GeoParseField.ALL || parseFields == GeoParseField.ASSOC )
						boolQ.should(QueryBuilders.nestedQuery(DocumentPojo.associations_, FilterBuilders.geoDistanceFilter(AssociationPojo.docQuery_geotag_).distance(Double.parseDouble(geo.dist), DistanceUnit.KILOMETERS).point(lat, lon)).scoreMode("max").boost((float)1.0));
					if ( parseFields == GeoParseField.ALL || parseFields == GeoParseField.DOC )
						boolQ.should(QueryBuilders.constantScoreQuery(FilterBuilders.geoDistanceFilter(DocumentPojo.docGeo_).distance(Double.parseDouble(geo.dist), DistanceUnit.KILOMETERS).point(lat, lon)));
				}
				sQueryTerm.append("dist(*.geotag, (").append(geo.centerll).append(")) < ").append(geo.dist);				
			}				
		}//TESTED logic11,logic12
		else if ((null != geo.minll) && (null != geo.maxll)) 
		{
			double latmin, lonmin, latmax, lonmax;
			
			if ('(' == geo.minll.charAt(0)) {
				geo.minll = geo.minll.substring(1, geo.minll.length() - 1);
			}
			String[] latlon1 = geo.minll.split("\\s*,\\s*");
			if ('(' == geo.maxll.charAt(0)) {
				geo.maxll = geo.maxll.substring(1, geo.maxll.length() - 1);
			}
			String[] latlon2 = geo.maxll.split("\\s*,\\s*");
			
			if  ((2 == latlon1.length) && (2 == latlon2.length)) 
			{
				latmin = Double.parseDouble(latlon1[0]);
				lonmin = Double.parseDouble(latlon1[1]);
				latmax = Double.parseDouble(latlon2[0]);
				lonmax = Double.parseDouble(latlon2[1]);
				
				// top left = max,min 
				latmin = latmin < latmax ? latmin : latmax;
				latmax = latmin >= latmax ? latmin : latmax;
				lonmin = lonmin < lonmax ? lonmin : lonmax;
				lonmax = lonmin >= lonmax ? lonmin : lonmax;
									
				// If we've got this far, we've found all the different locations				
				if ( parseFields == GeoParseField.ALL || parseFields == GeoParseField.ENT )
				{
					//use a 2nd variable so we dont have to keep casting termQ to BoolQuery
					BoolQueryBuilder subQ = QueryBuilders.boolQuery().must(QueryBuilders.constantScoreQuery(FilterBuilders.geoBoundingBoxFilter(EntityPojo.docQuery_geotag_).topLeft(latmax,lonmin).bottomRight(latmin, lonmax)).boost(1.0F));
					subQ.must(QueryBuilders.termsQuery(EntityPojo.docQuery_ontology_type_, ont_terms.toArray()));													
					boolQ.should(QueryBuilders.nestedQuery(DocumentPojo.entities_, subQ).scoreMode("max").boost((float)1.0));
				}
				
				//ASSOC AND DOCGEO (only added if ont is point or null)			
				if ( parseFields == GeoParseField.ALL || parseFields == GeoParseField.ASSOC )
					boolQ.should(QueryBuilders.nestedQuery(DocumentPojo.associations_, FilterBuilders.geoBoundingBoxFilter(AssociationPojo.docQuery_geotag_).topLeft(latmax,lonmin).bottomRight(latmin, lonmax)).scoreMode("max").boost((float)1.0));
				if ( parseFields == GeoParseField.ALL || parseFields == GeoParseField.DOC )
					boolQ.should(QueryBuilders.constantScoreQuery(FilterBuilders.geoBoundingBoxFilter(DocumentPojo.docGeo_).topLeft(latmax,lonmin).bottomRight(latmin, lonmax)));
							
				sQueryTerm.append("*.geotag: [(").append(geo.minll).append("), (").append(geo.maxll).append(")]");	
			}	
		}//TESTED logic13,logic14
		else if ( (null != geo.name))
		{
			//TODO (INF-1239): NOT IMPLEMENTED YET
		}
		else if ( (null != geo.polys) )
		{
			//TODO (INF-1118): NOT IMPLEMENTED YET
		}			
				
		return boolQ;
	}
	
	////////////////////////////////////////////////////////////////////////
	
	// 1.2.2.1] Sub-Sub-Utility function to parse all the different date strings I can think of
	
	private static String[] _allowedDatesArray = null;
		// (odd, static initialization doesn't work; just initialize first time in utility fn)
	
	private static long parseDate(String sDate) {
		if (null == _allowedDatesArray) {
			_allowedDatesArray = new String[]
				{
					"yyyy'-'DDD", "yyyy'-'MM'-'dd", "yyyyMMdd", "dd MMM yyyy", "dd MMM yy", 
					"MM/dd/yy", "MM/dd/yyyy", "MM.dd.yy", "MM.dd.yyyy", 
					"yyyy'-'MM'-'dd hh:mm:ss", "dd MMM yy hh:mm:ss", "dd MMM yyyy hh:mm:ss",
					"MM/dd/yy hh:mm:ss", "MM/dd/yyyy hh:mm:ss", "MM.dd.yy hh:mm:ss", "MM.dd.yyyy hh:mm:ss",
					 DateFormatUtils.ISO_DATE_FORMAT.getPattern(),
					 DateFormatUtils.ISO_DATETIME_FORMAT.getPattern(),
					 DateFormatUtils.ISO_DATE_TIME_ZONE_FORMAT.getPattern(),
					 DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.getPattern(),
					 DateFormatUtils.SMTP_DATETIME_FORMAT.getPattern()
				};			
		}
		try {
			Date date = DateUtils.parseDate(sDate, _allowedDatesArray);			
			return date.getTime();
		}
		catch (Exception e) { // Error all the way out
			throw new RuntimeException(e);
		}		
	}//TESTED (logic5)
	
	////////////////////////////////////////////////////////////////////////
	
	// 1.2.3] Event term parsing - this one is pretty complex
	
	BaseQueryBuilder parseAssociationTerm(AdvancedQueryPojo.QueryTermPojo.AssociationTermPojo assoc, AdvancedQueryPojo.QueryTermPojo.SentimentModifierPojo sentiment, StringBuffer sQueryTerm )
	{
		boolean bFirstTerm = true;
		BoolQueryBuilder query = QueryBuilders.boolQuery();	
		sQueryTerm.append("association:(");
		
		@SuppressWarnings("unused")
		int nTerms = 0; // (might be used later)
		
		if (null != assoc.entity1) {
			bFirstTerm = false;
			sQueryTerm.append("(");			
			this.parseAssociationSubTerm(assoc.entity1, sQueryTerm, query, AssociationPojo.docQuery_entity1_, AssociationPojo.docQuery_entity1_index_);
			sQueryTerm.append(')');
			nTerms++;
		}//TESTED
		if (null != assoc.entity2) {
			if (!bFirstTerm) {
				sQueryTerm.append(" AND ");				
			}
			bFirstTerm = false;
			sQueryTerm.append("(");
			this.parseAssociationSubTerm(assoc.entity2, sQueryTerm, query, AssociationPojo.docQuery_entity2_, AssociationPojo.docQuery_entity2_index_);
			sQueryTerm.append(')');
			nTerms++;
		}//TESTED
		if (null != assoc.verb) {
			if (!bFirstTerm) {
				sQueryTerm.append(" AND ");				
			}
			bFirstTerm = false;
			sQueryTerm.append("(verb,verb_category:").append(assoc.verb).append(")");
			
			query.must(QueryBuilders.boolQuery().should(QueryBuilders.queryString(assoc.verb).field(AssociationPojo.docQuery_verb_)).
													should(QueryBuilders.queryString(assoc.verb).field(AssociationPojo.docQuery_verb_category_)));
			
			sQueryTerm.append(')');
			nTerms++;
		}//TESTED
		if (null != assoc.geo) 
		{
			if (!bFirstTerm) {
				sQueryTerm.append(" AND ");				
			}
			bFirstTerm = false;
			sQueryTerm.append("(");
			query.must(this.parseGeoTerm(assoc.geo, sQueryTerm, GeoParseField.ASSOC));
			sQueryTerm.append(')');
			nTerms++;
		}//TOTEST
		if (null != assoc.time) 
		{
			if (!bFirstTerm) {
				sQueryTerm.append(" AND ");				
			}
			bFirstTerm = false;
			sQueryTerm.append("(");
			
			// Top level: one of start OR end has to start inside the range (this is the first bit)
			// OR it must envelop the query 
			
			// OK this one is a bit tricky because an event has a start+end time ... I think both
			// have to be inside the time range (fortunately because that's the easy case!)
			// (Note time_start and time_end don't exist inside the document object)
			StringBuffer sbDummy = new StringBuffer();
			BoolQueryBuilder combo2 = QueryBuilders.boolQuery();
			combo2.should(this.parseDateTerm(assoc.time, sQueryTerm, AssociationPojo.docQuery_time_start_, false));
			sQueryTerm.append(") OR/CONTAINS (");
			combo2.should(this.parseDateTerm(assoc.time, sQueryTerm, AssociationPojo.docQuery_time_end_, false));
			// (complex bit, start must be < and end must be >)
			BoolQueryBuilder combo3 = QueryBuilders.boolQuery();
			AdvancedQueryPojo.QueryTermPojo.TimeTermPojo event1 = new AdvancedQueryPojo.QueryTermPojo.TimeTermPojo();
			AdvancedQueryPojo.QueryTermPojo.TimeTermPojo event2 = new AdvancedQueryPojo.QueryTermPojo.TimeTermPojo();
			sQueryTerm.append("))");
			event1.min = "10010101";
			event1.max = assoc.time.min; 
			event2.min = assoc.time.max; 
			event2.max = "99990101"; // (ie the end of time, sort of!)
			combo3.must(this.parseDateTerm(event1, sbDummy, AssociationPojo.docQuery_time_start_, false)); // ie start time earlier than query
			combo3.must(this.parseDateTerm(event2, sbDummy, AssociationPojo.docQuery_time_end_, false)); // AND end time later than query
			
			query.must(combo2.should(combo3));
			
			query.must(combo2); // (this one is very simple - either one of the times must be inside the range)
			nTerms++;
		}//TESTED
		if (null != assoc.type) {
			if (!bFirstTerm) {
				sQueryTerm.append(" AND ");				
			}
			bFirstTerm = false;
			sQueryTerm.append("(event_type:").append(assoc.type).append(")");
			query.must(QueryBuilders.termQuery(AssociationPojo.docQuery_assoc_type_, assoc.type));
			sQueryTerm.append(')');
			nTerms++;
		}//TOTEST
		if (null != sentiment) {
			if (!bFirstTerm) {
				sQueryTerm.append(" AND ");				
			}			
			bFirstTerm = false;
			RangeQueryBuilder sentimentQ = QueryBuilders.rangeQuery(AssociationPojo.docQuery_sentiment_);			
			if (null != sentiment.min) {
				sentimentQ.from(sentiment.min);
			}
			if (null != sentiment.max) {
				sentimentQ.to(sentiment.max);					
			}
			query.must(sentimentQ);
			sQueryTerm.append("sentiment:[").append(sentiment.min).append(',').append(sentiment.max).append(']');
		}//TOTEST (combined sentiment only)
		sQueryTerm.append(')');

		return query;
		
	} //TESTED/TOTEST (see above)
	
	// 1.2.3.2] Event term parsing utility
	void parseAssociationSubTerm(AdvancedQueryPojo.QueryTermPojo entity, StringBuffer sQueryTerm, BoolQueryBuilder combo, 
			String sFieldName, String sIndexName)
	{
		boolean bFirstTerm = true;
		// 3 cases: etext, ftext, or entity (in 2 subcases)...
		if ((null != entity.entity) && (!entity.entity.isEmpty())) { //1a
			combo.must(this.parseEntityTerm(entity, sQueryTerm, sIndexName));				
		}
		else if ((null != entity.entityValue) && (!entity.entityValue.isEmpty())) { //1b
			combo.must(this.parseEntityTerm(entity, sQueryTerm, sIndexName));
		}
		if ((null != entity.etext) && (!entity.etext.isEmpty())) { //2
			if (!bFirstTerm) {
				sQueryTerm.append(" AND ");				
			}
			bFirstTerm = false;
			sQueryTerm.append("(\""); 
			sQueryTerm.append(entity.etext);			
			sQueryTerm.append("\")");
			combo.must(CrossVersionQueryBuilders.matchPhraseQuery(sFieldName, entity.etext));
		}
		if ((null != entity.ftext) && (!entity.ftext.isEmpty())) { //3
			if (!bFirstTerm) {
				sQueryTerm.append(" AND ");				
			}
			bFirstTerm = false;
			sQueryTerm.append('(');
			sQueryTerm.append(entity.ftext);			
			sQueryTerm.append(')');
			combo.must(QueryBuilders.queryString(entity.ftext).field(sFieldName));
		}
	} //TESTED
	
////////////////////////////////////////////////////////////////////////

// 1.3] Utility to parse boolean logic

	private static Pattern _logicTidyUp = Pattern.compile("qt\\[\\d+\\]", Pattern.CASE_INSENSITIVE);
	
	private BoolQueryBuilder parseLogic(String logic, BaseQueryBuilder qt[], StringBuffer qtRead[], StringBuffer query)
	{
		BoolQueryBuilder bq = QueryBuilders.boolQuery();
		int nQueryElements = qt.length;
		
		if (null == logic) { // No logic specified, just and them all together
			for (int i = 0; i < nQueryElements; ++i) {
				if (null != qt[i]) {
					bq = bq.must(qt[i]);
					if (0 != i) {
						query.append(" and ");
					}
					query.append(qtRead[i]);
				}
			}			
			return bq;
			
		}//TESTED
		
		// Non-degenerate case, parse logic string (first convert qt[X] to X):
		
		SimpleBooleanParser.SimpleBooleanParserMTree tree = 
			SimpleBooleanParser.parseExpression(_logicTidyUp.matcher(logic).replaceAll("$1"));
		
		if (null == tree) {
			return null; 
		}
		else {
			parseLogicRecursive(tree, bq, qt, qtRead, query);
		}
		return bq;
	} //TESTED
	
	/////////////////////////////////////////
	
	// 1.3.1] Recursive utility for creating a binary object from the tree
	
	void parseLogicRecursive(SimpleBooleanParser.SimpleBooleanParserMTree node, BoolQueryBuilder levelUp, BaseQueryBuilder qt[], 
			StringBuffer qtRead[], StringBuffer query)
	{
		if (null == node.terms) {
			if ((node.bNegated) || (node.nTerm < 0)) {
				int nIndex = Math.abs(node.nTerm) - 1; // (turn into index)
				levelUp.mustNot(qt[nIndex]); 
				query.append("not ").append(qtRead[nIndex].toString());
			}
			else {
				int nIndex = node.nTerm - 1;
				levelUp.must(qt[nIndex]);
				query.append(qtRead[nIndex].toString());
			}
			return;
		}
		boolean bFirstPass = true;
		for (SimpleBooleanParser.SimpleBooleanParserMTree child: node.terms) {
			if (null == child.terms) {
				if (child.nTerm < 0) { // Term negated
					int nIndex = (-child.nTerm) - 1;
					if ('&' == node.op) {
						levelUp.mustNot(qt[nIndex]); // (turn into index)
						if (bFirstPass) {
							query.append("not ");
						}
						else {
							query.append(" and not ");
						}
						query.append(qtRead[nIndex].toString());
					}
					else {
						levelUp.should(QueryBuilders.boolQuery().mustNot(qt[nIndex]));
						if (bFirstPass) {
							query.append("not");
						}
						else {
							query.append(" or not ");
						}
						query.append(qtRead[nIndex].toString());
					}
				}
				else { // Term not negated
					int nIndex = child.nTerm - 1;
					if ('&' == node.op) {
						levelUp.must(qt[nIndex]);
						if (!bFirstPass) {
							query.append(" and ");
						}
						query.append(qtRead[nIndex].toString());
					}
					else {
						levelUp.should(qt[nIndex]);
						if (!bFirstPass) {
							query.append(" or ");
						}
						query.append(qtRead[nIndex].toString());
					}					
				}				
			} // actual term, not a node
			else { // (null != child.terms)
				// The term is a node, recurse!
				BoolQueryBuilder newLevel = QueryBuilders.boolQuery();
				if ('&' == node.op) {
					if (child.bNegated) {
						levelUp.mustNot(newLevel);
						if (!bFirstPass) {
							query.append(" and ");
						}
						query.append("not (");
					}
					else {
						levelUp.must(newLevel);
						if (!bFirstPass) {
							query.append(" and ");
						}
						query.append("(");
					}
				}
				else {
					if (child.bNegated) {
						levelUp.should(QueryBuilders.boolQuery().mustNot(newLevel));
						if (!bFirstPass) {
							query.append(" or ");
						}
						query.append("not (");
					}
					else {
						levelUp.should(newLevel);
						if (!bFirstPass) {
							query.append(" or ");
						}
						query.append("(");
					}
				}					
				parseLogicRecursive(child, newLevel, qt, qtRead, query);
				query.append(")");
				
			} // (end node is a term need to recurse)
			
			bFirstPass = false;
		
		} // end loop over child nodes
	}//TESTED
	
////////////////////////////////////////////////////////////////////////
	
// 2] Complex scoring	

	// 2.1] Proximity adjustments
	private static BaseQueryBuilder addProximityBasedScoring(BaseQueryBuilder currQuery, SearchRequestBuilder searchSettings, 
										AdvancedQueryPojo.QueryScorePojo scoreParams,
										BoolFilterBuilder parentFilterObj, boolean bLowAccuracyGeo)
	{
		Map<String, Object> params = new HashMap<String, Object>();
		Object[] paramDoublesScript = new Object[6];
		Object[] paramDoublesDecay = new Object[6];
		double[] geoDecay = parseGeoDecay(scoreParams); // (encapsulate this away since it can also be called by ScoringUtils)
		if ((null != geoDecay) && !bLowAccuracyGeo) {
			double dlat = geoDecay[0];
			double dlon = geoDecay[1];
			double dInvDist = geoDecay[2];
			paramDoublesScript[0] = dInvDist;
			paramDoublesScript[1] = dlat;
			paramDoublesScript[2] = dlon;
			paramDoublesDecay[0] = dInvDist;
			paramDoublesDecay[1] = dlat;
			paramDoublesDecay[2] = dlon;
		}
		else // geo prox not specified/malformed, or low accuracy
		{
			if (!bLowAccuracyGeo) {
				scoreParams.geoProx = null;
			}
			paramDoublesScript[0] = -1.0;
			paramDoublesScript[1] = -1.0;
			paramDoublesScript[2] = -1.0;
			paramDoublesDecay[0] = -1.0;
			paramDoublesDecay[1] = -1.0;
			paramDoublesDecay[2] = -1.0;
		}
		if ((null != geoDecay) && (null != parentFilterObj)) { // Regardless of high/low accuracy, add 0.5% filter
			
			GeoDistanceFilterBuilder geoFilter = FilterBuilders.geoDistanceFilter(EntityPojo.docQuery_geotag_).
													point(geoDecay[0], geoDecay[1]).distance(200.0/geoDecay[2], DistanceUnit.KILOMETERS);
			
			parentFilterObj.must(FilterBuilders.nestedFilter(DocumentPojo.entities_, geoFilter));
			
		}//TESTED
		
		//Time decay portion
		if ((null != scoreParams.timeProx) && (null != scoreParams.timeProx.time) && (null != scoreParams.timeProx.decay) &&  
				!scoreParams.timeProx.time.isEmpty() && !scoreParams.timeProx.decay.isEmpty()) 
		{
			long nDecayCenter = System.currentTimeMillis();
			if (!scoreParams.timeProx.time.equals("now")) 
			{
				nDecayCenter = parseDate(scoreParams.timeProx.time);
			}

			//Parse decay time:
			long nDecayTime = getInterval(scoreParams.timeProx.decay, 'w');
			double dInvDecay = 1.0/(double)nDecayTime;
			
			scoreParams.timeProx.nTime = nDecayCenter;
			scoreParams.timeProx.dInvDecay = dInvDecay;
			
			// Add 0.5% filter
			if (null != parentFilterObj) {
				
				long nMinTime = nDecayCenter - 200*nDecayTime;
				long nMaxTime = nDecayCenter + 200*nDecayTime;
				parentFilterObj.must(FilterBuilders.numericRangeFilter(DocumentPojo.publishedDate_).from(nMinTime).to(nMaxTime));				
				
			}//TESTED
			
			paramDoublesScript[3] = dInvDecay;
			paramDoublesScript[4] = nDecayCenter;		
			paramDoublesDecay[3] = dInvDecay;
			paramDoublesDecay[4] = nDecayCenter;
		}
		else
		{
			scoreParams.timeProx = null;
			paramDoublesScript[3] = -1.0;
			paramDoublesScript[4] = -1.0;
			paramDoublesDecay[3] = -1.0;
			paramDoublesDecay[4] = -1.0;
		}
		
		if ( scoreParams.timeProx == null && scoreParams.geoProx == null )
		{		
			//if there is no timeprox or geoprox, just run the query w/o script
			return currQuery;
		}
		else
		{			
			if (null != searchSettings) // just handles test cases where searchSettings==null
			{ 
				paramDoublesDecay[5] = true;
				Map<String,Object> scriptParams = new HashMap<String, Object>();	
				scriptParams.put("param", paramDoublesDecay);
				searchSettings.addScriptField("decay", QueryDecayFactory.getLanguage(), QueryDecayFactory.getScriptName(), scriptParams);
			}
			//if there is a decay, add the script to the query		
			paramDoublesScript[5] = false;
			params.put("param", paramDoublesScript);
			return CrossVersionQueryBuilders.customScoreQueryBuilderScript(currQuery,QueryDecayFactory.getScriptName(),QueryDecayFactory.getLanguage(),params);
		} 	
	}//TESTED
	
	// Utility to parse out geo for "software emulated case"
	// returns lat/lon/distance (or null if anything goes wrong)
	// Also called from ScoringUtils

	public static double[] parseGeoDecay(AdvancedQueryPojo.QueryScorePojo scoreParams) {
		//Geo decay portion
		if ((null != scoreParams.geoProx) && (null != scoreParams.geoProx.ll) && (null != scoreParams.geoProx.decay) &&  
				!scoreParams.geoProx.ll.equals(",") && !scoreParams.geoProx.ll.isEmpty() && !scoreParams.geoProx.decay.isEmpty()) 
		{			
			if ('(' == scoreParams.geoProx.ll.charAt(0)) 
			{
				scoreParams.geoProx.ll = scoreParams.geoProx.ll.substring(1, scoreParams.geoProx.ll.length() - 1);
			}
			String[] latlon = scoreParams.geoProx.ll.split("\\s*,\\s*");
			if (2 == latlon.length) 
			{
				double[] lat_lon_invdist = new double[3];
				lat_lon_invdist[0] = Double.parseDouble(latlon[0]);
				lat_lon_invdist[1] = Double.parseDouble(latlon[1]);
				double dDist = getDistance(scoreParams.geoProx.decay); // (Returns it in km)
				if (0.0 == dDist) dDist = 0.00001; // (robustness, whatever)
				lat_lon_invdist[2] = (1.0/dDist); 

				return lat_lon_invdist;
			}
		}
		return null;		
	}
	
	// Utility to get the ms count of an interval
	
	public static long getInterval(String interval, char defaultInterval) {
		
		if (interval.equals("month")) { // Special case
			return 30L*24L*3600L*1000L;
		}
		
		int nLastIndex = interval.length() - 1;
		long nDecayTime;
		char c = interval.charAt(nLastIndex);
		if (c >= 0x40) { // it's a digit, interpret:
			nDecayTime = Long.parseLong(interval.substring(0, nLastIndex));
		}
		else { // No digit use default
			c = defaultInterval;
			nDecayTime = Long.parseLong(interval);
		}
		if ('h' == c) {
			nDecayTime *= 3600L*1000L;
		}
		else if ('d' == c) {
			nDecayTime *= 24L*3600L*1000L;
		}
		else if ('w' == c) {
			nDecayTime *= 7L*24L*3600L*1000L;
		}
		else if ('m' == c) {
			nDecayTime *= 30L*24L*3600L*1000L;
		}
		else if ('y' == c) {
			nDecayTime *= 365L*24L*3600L*1000L;
		}
		return nDecayTime;
	}//TESTED
	
	private static double getDistance(String distance) { // [0-9]+{m,km,nm)
		double dDist = 0.0;
		
		int nCharIndex1 = distance.length() - 1;
		char c = distance.charAt(nCharIndex1);
		if (c == 'm') {
			c = distance.charAt(nCharIndex1 - 1);
			if (c == 'k') { // km
				dDist = Double.parseDouble(distance.substring(0, nCharIndex1 - 1));
			}
			else if (c == 'n') { // nm
				dDist = Double.parseDouble(distance.substring(0, nCharIndex1 - 1))*1.852;				
			}
			else { // m==mi
				dDist = Double.parseDouble(distance.substring(0, nCharIndex1))*1.150779;
			}
		}
		else if (c == 'i') { // mi
			dDist = Double.parseDouble(distance.substring(0, nCharIndex1 - 1))*1.150779;			
		}
		else { // Default to km
			dDist = Double.parseDouble(distance.substring(0, nCharIndex1 + 1));			
		}		
		return dDist;
	}//TESTED

	////////////////////////////////////////////////////////////////////////
	
	// 2.2] Manual weighting
	
	private BaseQueryBuilder applyManualWeights(BaseQueryBuilder queryObj, AdvancedQueryPojo.QueryScorePojo score)
	{
		if ((null != score.tagWeights) || (null != score.typeWeights) || (null != score.sourceWeights)) {
			CustomFiltersScoreQueryBuilder manualWeights = CrossVersionQueryBuilders.customFiltersScoreQuery(queryObj);
			manualWeights.scoreMode("avg"); // Only tags can match multiple filters, in which case we average them
			
			if (null != score.sourceWeights) {
				// Find all weightings with the same score:
				ArrayListMultimap<Float, String> invSourceWeights = ArrayListMultimap.create();
				for (Map.Entry<String, Double> sourceKeyEl: score.sourceWeights.entrySet()) {
					invSourceWeights.put((float)(double)sourceKeyEl.getValue(), sourceKeyEl.getKey());
				}
				for (Map.Entry<Float, Collection<String>> invSourceKeyEl: invSourceWeights.asMap().entrySet()) {
					manualWeights.add(FilterBuilders.termsFilter(DocumentPojo.sourceKey_, invSourceKeyEl.getValue().toArray()), invSourceKeyEl.getKey());
				}
			}//TESTED
			if (null != score.typeWeights) {
				// Find all weightings with the same score:
				ArrayListMultimap<Float, String> invTypeWeights = ArrayListMultimap.create();
				for (Map.Entry<String, Double> typeEl: score.typeWeights.entrySet()) {
					invTypeWeights.put((float)(double)typeEl.getValue(), typeEl.getKey());
				}
				for (Map.Entry<Float, Collection<String>> invTypeEl: invTypeWeights.asMap().entrySet()) {
					if (null == score.sourceWeights) { // Easy case
						manualWeights.add(FilterBuilders.termsFilter(DocumentPojo.mediaType_, invTypeEl.getValue().toArray()), invTypeEl.getKey());
					}
					else { // Need to filter out sources they are matched with higher prio
						BoolFilterBuilder typesNotSources = FilterBuilders.boolFilter();
						typesNotSources = typesNotSources.must(FilterBuilders.termsFilter(DocumentPojo.mediaType_, invTypeEl.getValue().toArray())).
															mustNot(FilterBuilders.termsFilter(DocumentPojo.sourceKey_, score.sourceWeights.keySet().toArray()));
						
						manualWeights.add(typesNotSources, invTypeEl.getKey());
					}
				}
			}//TESTED
			if (null != score.tagWeights) { 
				// Find all weightings with the same score:
				ArrayListMultimap<Float, String> invTagWeights = ArrayListMultimap.create();
				for (Map.Entry<String, Double> tagEl: score.tagWeights.entrySet()) {
					invTagWeights.put((float)(double)tagEl.getValue(), tagEl.getKey());
				}
				for (Map.Entry<Float, Collection<String>> invTagEl: invTagWeights.asMap().entrySet()) {
					if ((null == score.sourceWeights) && (null == score.typeWeights)) { // Easy case
						manualWeights.add(FilterBuilders.termsFilter(DocumentPojo.tags_, invTagEl.getValue().toArray()), invTagEl.getKey());
					}
					else { // need to exclude types or sources
						BoolFilterBuilder typesNotSources = FilterBuilders.boolFilter();
						BoolFilterBuilder tagsAndNothingElse = typesNotSources.must(FilterBuilders.termsFilter(DocumentPojo.tags_, invTagEl.getValue().toArray()));
						if (null != score.sourceWeights) {
							tagsAndNothingElse = tagsAndNothingElse.mustNot(FilterBuilders.termsFilter(DocumentPojo.sourceKey_, score.sourceWeights.keySet().toArray()));
						}
						if (null != score.typeWeights) {
							tagsAndNothingElse = tagsAndNothingElse.mustNot(FilterBuilders.termsFilter(DocumentPojo.mediaType_, score.typeWeights.keySet().toArray()));
						}
						manualWeights.add(tagsAndNothingElse, invTagEl.getKey());
					}
				}
			}//TESTED
			queryObj = manualWeights;
		}
		return queryObj;
	}//TESTED
	
////////////////////////////////////////////////////////////////////////
	
// 3] Output parsing
		
	// (Aggregation output parsing delegated to processing.AggregationUtils)
	
////////////////////////////////////////////////////////////////////////
		
// 4] Query management
	
	private DBCursor getDocIds(DBCollection docDb, ObjectId[] ids, int nFromServerLimit, AdvancedQueryPojo.QueryOutputPojo output, AdvancedQueryPojo.QueryScorePojo score)	
	{
		DBCursor docdCursor = null;
		try {
		
			BasicDBObject query = new BasicDBObject();
			query.put("_id", new BasicDBObject("$in", ids));
			BasicDBObject fields = new BasicDBObject(DocumentPojo.fullText_, 0); // (used to discard community ids -plus legacy versions-, now need it)
			if (!output.docs.metadata) {
				fields.put(DocumentPojo.metadata_, 0);
			}
			boolean bNotAggEnts = ((output.aggregation == null) || (output.aggregation.entsNumReturn == null) || (output.aggregation.entsNumReturn == 0));
			if (bNotAggEnts && (null != score) && (null != score.sigWeight) && (score.sigWeight > 0.0)) {
				bNotAggEnts = false; // (special case, use agg entities to score docs)
			}
			if (!output.docs.ents && bNotAggEnts) {
				fields.put(DocumentPojo.entities_, 0);				
			}
			boolean bNotAggEvents = ((output.aggregation == null) || (output.aggregation.eventsNumReturn == null) || (output.aggregation.eventsNumReturn == 0));
			boolean bNotAggFacts = ((output.aggregation == null) || (output.aggregation.factsNumReturn == null) || (output.aggregation.factsNumReturn == 0));
			boolean bNoStandaloneEvents = (null == output.docs.eventsTimeline) || (null == output.docs.numEventsTimelineReturn) || (output.docs.numEventsTimelineReturn == 0);
			if (!output.docs.events && !output.docs.facts && !output.docs.summaries && bNoStandaloneEvents && bNotAggEvents && bNotAggFacts) {
				fields.put(DocumentPojo.associations_, 0);
			}
			//TESTED
			
			//cm = new CollectionManager();
			boolean bPrimary = true;
			
			if (_replicaSetDistributionRatio > 0) {
				if (0 != (new Date().getTime() % _replicaSetDistributionRatio)) {
					bPrimary = false;
				}
			}			
			if (bPrimary) { // Get from the primary
				docdCursor = docDb.find(query, fields).batchSize(nFromServerLimit);				
			}
			else { // Try and get from the secondary if possible
				docdCursor = docDb.find(query, fields).batchSize(nFromServerLimit).setReadPreference(ReadPreference.secondaryPreferred());
			}
			
		} catch (Exception e) {
			// If an exception occurs log the error
			_logger.error("Address Exception Message: " + e.getMessage(), e);
		}
		return docdCursor;
	}		
	
	//___________________________________________________________________________________

	// Utility function: create a populated query object (by defaults if necessary)
	
	public static AdvancedQueryPojo createQueryPojo(String queryJson) {
		GsonBuilder gb = AdvancedQueryPojo.getDefaultBuilder();
		gb.registerTypeAdapter(AdvancedQueryPojo.QueryRawPojo.class, new AdvancedQueryPojo.QueryRawPojo.Deserializer());
		AdvancedQueryPojo query = gb.create().fromJson(queryJson, AdvancedQueryPojo.class);
		// Fill in the blanks (a decent attempt has been made to fill out the blanks inside these options)
		if (null == query.input) {
			query.input = new AdvancedQueryPojo.QueryInputPojo();				
		}
		if (null == query.score) {
			query.score = new AdvancedQueryPojo.QueryScorePojo();				
		}
		if (null == query.output) {
			query.output = new AdvancedQueryPojo.QueryOutputPojo();
		}		
		if (null == query.output.docs) { // (Docs are sufficiently important we'll make sure they're always present)
			query.output.docs = new AdvancedQueryPojo.QueryOutputPojo.DocumentOutputPojo();
		}
		return query;
	}//TESTED
	
////////////////////////////////////////////////////////////////////////
	
// 5] Unit testing code

	//static private final QueryHandler _test = new QueryHandler(true); 
	
	@SuppressWarnings("unused")
	private QueryHandler(boolean bTest) {		
		_scoringParams = new AdvancedQueryPojo.QueryScorePojo();
		this.testParsingCode();
	}
	
	private void testParsingCode() {

		// (these are used for logic testing below)
		List<BaseQueryBuilder> qtTerms = new LinkedList<BaseQueryBuilder>(); 
		List<StringBuffer> qtReadTerms = new LinkedList<StringBuffer>(); 		
		
	// Various query terms

		AdvancedQueryPojo.QueryTermPojo qt0 = new AdvancedQueryPojo.QueryTermPojo();
		qt0.ftext = "ftext +test";
		AdvancedQueryPojo.QueryTermPojo qt1 = new AdvancedQueryPojo.QueryTermPojo();
		qt1.ftext = "ftext"; qt1.etext = "etext +test";
		AdvancedQueryPojo.QueryTermPojo qt2 = new AdvancedQueryPojo.QueryTermPojo();
		qt2.etext = "etext test"; qt2.entity = "entity:type";
		
		StringBuffer result = new StringBuffer();
		BaseQueryBuilder resJson = null;
		
		// "logic0":
		resJson = this.parseQueryTerm(qt0, result);
		qtTerms.add(resJson); qtReadTerms.add(new StringBuffer(result.toString()));
		String logic0a = new Gson().toJson(resJson);
		String logic0b = result.toString();
		String answer0a = "{\"queryString\":\"ftext +test\",\"fuzzyMinSim\":-1.0,\"boost\":-1.0,\"fuzzyPrefixLength\":-1,\"phraseSlop\":-1,\"tieBreaker\":-1.0}";
		String answer0b = "((ftext +test))";
		if (!logic0a.equals(answer0a) || !logic0b.equals(answer0b))
		{
			System.out.println("Fail 0"); System.out.println(logic0a); System.out.println(answer0a); System.out.println(logic0b); System.out.println(answer0b);  
		}
		
		// "logic1":
		resJson = this.parseQueryTerm(qt1, result);
		qtTerms.add(resJson); qtReadTerms.add(new StringBuffer(result.toString()));
		String logic1a = new Gson().toJson(resJson);
		String logic1b = result.toString();
		String answer1a = "{\"clauses\":[{\"queryBuilder\":{\"queryString\":\"ftext\",\"fuzzyMinSim\":-1.0,\"boost\":-1.0,\"fuzzyPrefixLength\":-1,\"phraseSlop\":-1,\"tieBreaker\":-1.0},\"occur\":\"MUST\"},{\"queryBuilder\":{\"queryString\":\"etext\\\\ \\\\+test\",\"fuzzyMinSim\":-1.0,\"boost\":-1.0,\"fuzzyPrefixLength\":-1,\"phraseSlop\":-1,\"tieBreaker\":-1.0},\"occur\":\"MUST\"}],\"boost\":-1.0,\"minimumNumberShouldMatch\":-1}";
		String answer1b = "((ftext) AND (etext\\ \\+test))";
		if (!logic1a.equals(answer1a) || !logic1b.equals(answer1b))
		{
			System.out.println("Fail 1"); System.out.println(logic1a); System.out.println(answer1a); System.out.println(logic1b); System.out.println(answer1b);  
		}
		
		// "logic2":
		resJson = this.parseQueryTerm(qt2, result);
		qtTerms.add(resJson); qtReadTerms.add(new StringBuffer(result.toString()));
		String logic2a = new Gson().toJson(resJson);
		String logic2b = result.toString();
		String answer2a = "{\"clauses\":[{\"queryBuilder\":{\"queryString\":\"etext\\\\ test\",\"fuzzyMinSim\":-1.0,\"boost\":-1.0,\"fuzzyPrefixLength\":-1,\"phraseSlop\":-1,\"tieBreaker\":-1.0},\"occur\":\"MUST\"},{\"queryBuilder\":{\"name\":\"entities.index\",\"value\":\"entity/type\",\"boost\":-1.0},\"occur\":\"MUST\"}],\"boost\":-1.0,\"minimumNumberShouldMatch\":-1}";
		String answer2b = "((etext\\ test) AND (entities.index:\"entity/type\"))";
		if (!logic2a.equals(answer2a) || !logic2b.equals(answer2b))
		{
			System.out.println("Fail 2"); System.out.println(logic2a); System.out.println(answer2a); System.out.println(logic2b); System.out.println(answer2b);  
		}	
		// (entityValue/entityType tested by logic3 below) 
		
	// Alias expansion (leave this commented out since results depend on current DB - ie check by eye)
		
		AdvancedQueryPojo.QueryTermPojo qt3a = new AdvancedQueryPojo.QueryTermPojo();
		qt3a.ftext = "ftext"; qt3a.etext = "etext"; qt3a.entity = "barack obama/person"; 
		qt3a.entityOpt = new AdvancedQueryPojo.QueryTermPojo.EntityOptionPojo(); 
		qt3a.entityOpt.expandAlias = true;
		AdvancedQueryPojo.QueryTermPojo qt3b = new AdvancedQueryPojo.QueryTermPojo();
		qt3b.entity = "new york city,new york,united states:city"; 
		qt3b.entityOpt = new AdvancedQueryPojo.QueryTermPojo.EntityOptionPojo(); 
		qt3b.entityOpt.expandAlias = true;
		AdvancedQueryPojo.QueryTermPojo qt3c = new AdvancedQueryPojo.QueryTermPojo();
		qt3c.entity = "entity3/type3"; 
		qt3c.entityOpt = new AdvancedQueryPojo.QueryTermPojo.EntityOptionPojo(); 
		qt3c.entityOpt.expandAlias = false;
		AdvancedQueryPojo.QueryTermPojo qt3d = new AdvancedQueryPojo.QueryTermPojo();
		qt3d.entity = "entity4/type4"; 
		qt3d.entityOpt = null; 
		AdvancedQueryPojo.QueryTermPojo qt3e = new AdvancedQueryPojo.QueryTermPojo();
		qt3e.etext = "etext"; qt3e.entityValue = "facebook inc"; // no entity type, ie shouldn't request anything  
		qt3e.entityOpt = new AdvancedQueryPojo.QueryTermPojo.EntityOptionPojo(); 
		qt3e.entityOpt.expandAlias = true;
		AdvancedQueryPojo.QueryTermPojo qt3f = new AdvancedQueryPojo.QueryTermPojo();
		qt3f.entityValue = "facebook inc"; qt3f.entityType = "company";  
		qt3f.entityOpt = new AdvancedQueryPojo.QueryTermPojo.EntityOptionPojo(); 
		qt3f.entityOpt.expandAlias = true;
		AdvancedQueryPojo.QueryTermPojo qt3g = new AdvancedQueryPojo.QueryTermPojo();
		qt3g.ftext = "No entity so should ignore the entityOpt parameters"; 
		qt3g.entityOpt = new AdvancedQueryPojo.QueryTermPojo.EntityOptionPojo(); 
		qt3g.entityOpt.expandAlias = true;
		List<AdvancedQueryPojo.QueryTermPojo> qtList = Arrays.asList(qt3a, qt3b, qt3c, qt3d, qt3e, qt3f, qt3g);
		this.handleEntityExpansion(null, qtList, null, "4c927585d591d31d7b37097a");
		
		String sAnswer_3_1 = "[barack obama/person, facebook inc/company, new york city,new york,united states/city]";
		String sResults_3_1 = Arrays.toString(_tmpEntityExpansionList.toArray());
		if (!sAnswer_3_1.equals(sResults_3_1)) {
			System.out.println("Fail 3.1"); System.out.println(sAnswer_3_1); System.out.println(sResults_3_1);
		}
		String [] sResults_3_2 = _tmpAliasMap.get("barack obama/person").toArray(new String[0]);
		if (null != sResults_3_2) {
			//DEBUG
			//System.out.println(Arrays.toString(sResults_3_2));
			resJson = this.parseQueryTerm(qt3a, result);
			String logic3a_1 = new Gson().toJson(resJson);
			String logic3a_2 = result.toString();
			//DEBUG
			//System.out.println(logic3a_1); System.out.println(logic3a_2);
			if (!logic3a_2.contains("$aliases")) {
				System.out.println("Fail 3.2a"); System.out.println(logic3a_1); System.out.println(logic3a_2);				
			}
		}
		else {
			System.out.println("Fail 3.2a"); 
		}
		sResults_3_2 = _tmpAliasMap.get("facebook inc/company").toArray(new String[0]);
		if (null != sResults_3_2) {
			//DEBUG
			//System.out.println(Arrays.toString(sResults_3_2));
			resJson = this.parseQueryTerm(qt3b, result);
			String logic3b_1 = new Gson().toJson(resJson);
			String logic3b_2 = result.toString();
			//DEBUG
			//System.out.println(logic3b_1); System.out.println(logic3b_2);
			if (!logic3b_2.contains("$aliases")) {
				System.out.println("Fail 3.2b"); System.out.println(logic3b_1); System.out.println(logic3b_2);				
			}
		}
		else {
			System.out.println("Fail 3.2b"); 
		}		
		sResults_3_2 = _tmpAliasMap.get("new york city,new york,united states/city").toArray(new String[0]);
		if (null != sResults_3_2) {
			//DEBUG
			//System.out.println(Arrays.toString(sResults_3_2));
			resJson = this.parseQueryTerm(qt3f, result);
			String logic3f_1 = new Gson().toJson(resJson);
			String logic3f_2 = result.toString();
			//DEBUG
			//System.out.println(logic3f_1); System.out.println(logic3f_2);
			if (!logic3f_2.contains("$aliases")) {
				System.out.println("Fail 3.2f"); System.out.println(logic3f_1); System.out.println(logic3f_2);				
			}
		}
		else {
			System.out.println("Fail 3.2f"); 
		}
		// Just check we don't normally get aliases:
		resJson = this.parseQueryTerm(qt3e, result);
		String logic3e_1 = new Gson().toJson(resJson);
		String logic3e_2 = result.toString();
		//DEBUG
		//System.out.println(logic3e_1); System.out.println(logic3e_2);
		if (logic3e_2.contains("$aliases")) {
			System.out.println("Fail 3.ef"); System.out.println(logic3e_1); System.out.println(logic3e_2);				
		}
		
	//Date debugging:
		_nNow = 1284666757165L; //Thu, 16 Sep 2010 19:52:37 GMT
		
		// Lots of nasty time cases, sigh
		AdvancedQueryPojo.QueryTermPojo qt5 = new AdvancedQueryPojo.QueryTermPojo();
		qt5.entity = "entity/type"; qt5.time = new AdvancedQueryPojo.QueryTermPojo.TimeTermPojo();
		AdvancedQueryPojo.QueryTermPojo qt6 = new AdvancedQueryPojo.QueryTermPojo();
		qt6.time = new AdvancedQueryPojo.QueryTermPojo.TimeTermPojo();
		qt6.time.min = "1284666757164"; qt6.time.max = "now";
		AdvancedQueryPojo.QueryTermPojo qt7 = new AdvancedQueryPojo.QueryTermPojo();
		qt7.time = new AdvancedQueryPojo.QueryTermPojo.TimeTermPojo();
		qt7.time.max = "1284666757164";
		AdvancedQueryPojo.QueryTermPojo qt8 = new AdvancedQueryPojo.QueryTermPojo();
		qt8.time = new AdvancedQueryPojo.QueryTermPojo.TimeTermPojo();
		qt8.time.min = "02/10/2000"; qt8.time.max = "02.10.2000";
		AdvancedQueryPojo.QueryTermPojo qt9 = new AdvancedQueryPojo.QueryTermPojo();
		qt9.time = new AdvancedQueryPojo.QueryTermPojo.TimeTermPojo();
		qt9.time.min = "10 Feb 2000"; qt9.time.max = "10 Feb 2000 00:00:00";
		AdvancedQueryPojo.QueryTermPojo qt9b = new AdvancedQueryPojo.QueryTermPojo();
		qt9b.time = new AdvancedQueryPojo.QueryTermPojo.TimeTermPojo();
		qt9b.time.min = "10 Feb 2000"; qt9b.time.max = "10 Feb 2000";
		AdvancedQueryPojo.QueryTermPojo qt10 = new AdvancedQueryPojo.QueryTermPojo();
		qt10.time = new AdvancedQueryPojo.QueryTermPojo.TimeTermPojo();
		qt10.time.max = "20000210";
	
		// "logic5":
		resJson = this.parseQueryTerm(qt5, result);
		String logic5a = new Gson().toJson(resJson);
		String logic5b = result.toString();
		String answer5a = "{\"clauses\":[{\"queryBuilder\":{\"name\":\"entities.index\",\"value\":\"entity/type\",\"boost\":-1.0},\"occur\":\"MUST\"},{\"queryBuilder\":{\"filterBuilder\":{\"name\":\"publishedDate\",\"from\":0,\"to\":1284666757165,\"includeLower\":true,\"includeUpper\":true},\"boost\":1.0},\"occur\":\"MUST\"}],\"boost\":-1.0,\"minimumNumberShouldMatch\":-1}";
		String answer5b = "((entities.index:\"entity/type\") AND (publishedDate:[0 TO Thu Sep 16 15:52:37 EDT 2010]))";
		if (!logic5a.equals(answer5a) || !logic5b.equals(answer5b))
		{
			System.out.println("Fail 5"); System.out.println(logic5a); System.out.println(answer5a); System.out.println(logic5b); System.out.println(answer5b);  
		}
		
		// "logic6":
		resJson = this.parseQueryTerm(qt6, result);
		String logic6a = new Gson().toJson(resJson);
		String logic6b = result.toString();
		String answer6a = "{\"filterBuilder\":{\"name\":\"publishedDate\",\"from\":1284666757164,\"to\":1284666757165,\"includeLower\":true,\"includeUpper\":true},\"boost\":1.0}";
		String answer6b = "((publishedDate:[Thu Sep 16 15:52:37 EDT 2010 TO Thu Sep 16 15:52:37 EDT 2010]))";
		if (!logic6a.equals(answer6a) || !logic6b.equals(answer6b))
		{
			System.out.println("Fail 6"); System.out.println(logic6a); System.out.println(answer6a); System.out.println(logic6b); System.out.println(answer6b);  
		}
		
		// "logic7"
		resJson = this.parseQueryTerm(qt7, result);
		qtTerms.add(resJson); qtReadTerms.add(new StringBuffer(result.toString()));
		String logic7a = new Gson().toJson(resJson);
		String logic7b = result.toString();
		String answer7a = "{\"filterBuilder\":{\"name\":\"publishedDate\",\"from\":0,\"to\":1284666757164,\"includeLower\":true,\"includeUpper\":true},\"boost\":1.0}";
		String answer7b = "((publishedDate:[0 TO Thu Sep 16 15:52:37 EDT 2010]))";
		if (!logic7a.equals(answer7a) || !logic7b.equals(answer7b))
		{
			System.out.println("Fail 7"); System.out.println(logic7a); System.out.println(answer7a); System.out.println(logic7b); System.out.println(answer7b);  
		}
		
		// "logic8"
		resJson = this.parseQueryTerm(qt8, result);
		String logic8a = new Gson().toJson(resJson);
		String logic8b = result.toString();
		String answer8a = "{\"filterBuilder\":{\"name\":\"publishedDate\",\"from\":950158800000,\"to\":950227199999,\"includeLower\":true,\"includeUpper\":true},\"boost\":1.0}";
		String answer8b = "((publishedDate:[Thu Feb 10 00:00:00 EST 2000 TO Thu Feb 10 18:59:59 EST 2000]))";
		if (!logic8a.equals(answer8a) || !logic8b.equals(answer8b))
		{
			System.out.println("Fail 8"); System.out.println(logic8a); System.out.println(answer8a); System.out.println(logic8b); System.out.println(answer8b);  
		}
		
		// "logic9" (different to 8 because hour specified)
		resJson = this.parseQueryTerm(qt9, result);
		String logic9a = new Gson().toJson(resJson);
		String logic9b = result.toString();
		String answer9a = "{\"filterBuilder\":{\"name\":\"publishedDate\",\"from\":950158800000,\"to\":950158800000,\"includeLower\":true,\"includeUpper\":true},\"boost\":1.0}";
		String answer9b = "((publishedDate:[Thu Feb 10 00:00:00 EST 2000 TO Thu Feb 10 00:00:00 EST 2000]))";
		if (!logic9a.equals(answer9a) || !logic9b.equals(answer9b))
		{
			System.out.println("Fail 9"); System.out.println(logic9a); System.out.println(answer9a); System.out.println(logic9b); System.out.println(answer9b);  
		}
		
		// "logic9b" (answer identical to 8...)
		resJson = this.parseQueryTerm(qt9b, result);
		String logic9ba = new Gson().toJson(resJson);
		String logic9bb = result.toString();
		String answer9ba = "{\"filterBuilder\":{\"name\":\"publishedDate\",\"from\":950158800000,\"to\":950227199999,\"includeLower\":true,\"includeUpper\":true},\"boost\":1.0}";
		String answer9bb = "((publishedDate:[Thu Feb 10 00:00:00 EST 2000 TO Thu Feb 10 18:59:59 EST 2000]))";
		if (!logic9ba.equals(answer9ba) || !logic9bb.equals(answer9bb))
		{
			System.out.println("Fail 9b"); System.out.println(logic9ba); System.out.println(answer9ba); System.out.println(logic9bb); System.out.println(answer9bb);  
		}
		
		// "logic10" 
		resJson = this.parseQueryTerm(qt10, result);
		String logic10a = new Gson().toJson(resJson);
		String logic10b = result.toString();
		String answer10a = "{\"filterBuilder\":{\"name\":\"publishedDate\",\"from\":0,\"to\":950227199999,\"includeLower\":true,\"includeUpper\":true},\"boost\":1.0}";
		String answer10b = "((publishedDate:[0 TO Thu Feb 10 18:59:59 EST 2000]))";
		if (!logic10a.equals(answer10a) || !logic10b.equals(answer10b))
		{
			System.out.println("Fail 10"); System.out.println(logic10a); System.out.println(answer10a); System.out.println(logic10b); System.out.println(answer10b);  
		}
		
	// GEO test cases:
		AdvancedQueryPojo.QueryTermPojo qt11 = new AdvancedQueryPojo.QueryTermPojo();
		qt11.geo = new AdvancedQueryPojo.QueryTermPojo.GeoTermPojo();
		qt11.geo.centerll = "40.12,-71.34";
		qt11.geo.dist = "100km";
		AdvancedQueryPojo.QueryTermPojo qt12 = new AdvancedQueryPojo.QueryTermPojo();
		qt12.geo = new AdvancedQueryPojo.QueryTermPojo.GeoTermPojo();
		qt12.geo.centerll = "(4.1,-171.34)";
		qt12.geo.dist = "100";
		
		AdvancedQueryPojo.QueryTermPojo qt13 = new AdvancedQueryPojo.QueryTermPojo();
		qt13.geo = new AdvancedQueryPojo.QueryTermPojo.GeoTermPojo();
		qt13.geo.minll = "(4.1,-171.34)";
		qt13.geo.maxll = "40.12,-71.34";		
		
		AdvancedQueryPojo.QueryTermPojo qt14 = new AdvancedQueryPojo.QueryTermPojo();
		qt14.geo = new AdvancedQueryPojo.QueryTermPojo.GeoTermPojo();
		qt14.geo.minll = "4.1,-171.34";
		qt14.geo.maxll = "(40.12,-71.34)";		
		
		// "logic11"
		
		resJson = this.parseQueryTerm(qt11, result);
		qtTerms.add(resJson); qtReadTerms.add(new StringBuffer(result.toString()));
		String logic11a = new Gson().toJson(resJson);
		String logic11b = result.toString();
		String answer11a = "{\"filterBuilder\":{\"name\":\"locs\",\"distance\":\"100km\",\"lat\":40.12,\"lon\":-71.34},\"boost\":1.0}";
		String answer11b = "((dist(*.geotag, (40.12,-71.34)) < 100km))";
		if (!logic11a.equals(answer11a) || !logic11b.equals(answer11b))
		{
			System.out.println("Fail 11"); System.out.println(logic11a); System.out.println(answer11a); System.out.println(logic11b); System.out.println(answer11b);  
		}
		
		// "logic12"
		
		resJson = this.parseQueryTerm(qt12, result);
		String logic12a = new Gson().toJson(resJson);
		String logic12b = result.toString();
		String answer12a = "{\"filterBuilder\":{\"name\":\"locs\",\"distance\":\"100.0km\",\"lat\":4.1,\"lon\":-171.34},\"boost\":1.0}";
		String answer12b = "((dist(*.geotag, (4.1,-171.34)) < 100))";
		if (!logic12a.equals(answer12a) || !logic12b.equals(answer12b))
		{
			System.out.println("Fail 12"); System.out.println(logic12a); System.out.println(answer12a); System.out.println(logic12b); System.out.println(answer12b);  
		}
		
		// "logic13"
		
		resJson = this.parseQueryTerm(qt13, result);
		String logic13a = new Gson().toJson(resJson);
		String logic13b = result.toString();
		String answer13a = "{\"filterBuilder\":{\"name\":\"locs\",\"topLeft\":{\"lat\":40.12,\"lon\":-171.34},\"bottomRight\":{\"lat\":4.1,\"lon\":-71.34}},\"boost\":1.0}";
		String answer13b = "((*.geotag: [(4.1,-171.34), (40.12,-71.34)]))";
		if (!logic13a.equals(answer13a) || !logic13b.equals(answer13b))
		{
			System.out.println("Fail 13"); System.out.println(logic13a); System.out.println(answer13a); System.out.println(logic13b); System.out.println(answer13b);  
		}
		
		// "logic14"
		
		resJson = this.parseQueryTerm(qt14, result);
		String logic14a = new Gson().toJson(resJson);
		String logic14b = result.toString();
		String answer14a = "{\"filterBuilder\":{\"name\":\"locs\",\"topLeft\":{\"lat\":40.12,\"lon\":-171.34},\"bottomRight\":{\"lat\":4.1,\"lon\":-71.34}},\"boost\":1.0}";
		String answer14b = "((*.geotag: [(4.1,-171.34), (40.12,-71.34)]))";
		if (!logic14a.equals(answer14a) || !logic14b.equals(answer14b))
		{
			System.out.println("Fail 14"); System.out.println(logic14a); System.out.println(answer14a); System.out.println(logic14b); System.out.println(answer14b);  
		}

	// Logic test code	
		
		// (saved 5 terms in the qtTerms and qtReadTerms: 0,1,2,7,11)
		
		String parser1 = "1 and 2 AND 3";
		SimpleBooleanParser.SimpleBooleanParserMTree tree = SimpleBooleanParser.parseExpression(parser1);	
		String parserres = SimpleBooleanParser.traverse(tree, false);
		String parserans = "$0: & (3 2 1 ) ";
		if (!parserans.equals(parserres)) {
			System.out.println("Fail p1"); System.out.println(parser1); System.out.println(parserres);
		}
		BoolQueryBuilder bq = QueryBuilders.boolQuery(); result.setLength(0);
		this.parseLogicRecursive(tree, bq, qtTerms.toArray(new BaseQueryBuilder[6]), qtReadTerms.toArray(new StringBuffer[6]), result);
		String parseransQ = "((etext\\ test) AND (entities.index:\"entity/type\")) and ((ftext) AND (etext\\ \\+test)) and ((ftext +test))";
		if (!parseransQ.equals(result.toString())) {
			System.out.println("Fail p1"); System.out.println(parseransQ); System.out.println(result.toString());			
		}
		
		String parser2 = "1 or 2 and 3 or 4";
		tree = SimpleBooleanParser.parseExpression(parser2);
		parserres = SimpleBooleanParser.traverse(tree, false);
		parserans = "$0: | ($1 1 ) $1: | (4 $2 ) $2: & (3 2 ) ";
		if (!parserans.equals(parserres)) {
			System.out.println("Fail p2"); System.out.println(parser2); System.out.println(parserres);
		}
		bq = QueryBuilders.boolQuery(); result.setLength(0);
		this.parseLogicRecursive(tree, bq, qtTerms.toArray(new BaseQueryBuilder[6]), qtReadTerms.toArray(new StringBuffer[6]), result);
		parseransQ = "(((publishedDate:[0 TO Thu Sep 16 15:52:37 EDT 2010])) or (((etext\\ test) AND (entities.index:\"entity/type\")) and ((ftext) AND (etext\\ \\+test)))) or ((ftext +test))";
		if (!parseransQ.equals(result.toString())) {
			System.out.println("Fail p2"); System.out.println(parseransQ); System.out.println(result.toString());			
		}
		
		String parser3 = "(1 or 2) and 3 or 4";
		tree = SimpleBooleanParser.parseExpression(parser3);
		parserres = SimpleBooleanParser.traverse(tree, false);
		parserans = "$0: | (4 $1 ) $1: & (3 $2 ) $2: | (2 1 ) ";
		if (!parserans.equals(parserres)) {
			System.out.println("Fail p3"); System.out.println(parser3); System.out.println(parserres);
		}
		bq = QueryBuilders.boolQuery(); result.setLength(0);
		this.parseLogicRecursive(tree, bq, qtTerms.toArray(new BaseQueryBuilder[6]), qtReadTerms.toArray(new StringBuffer[6]), result);
		parseransQ = "((publishedDate:[0 TO Thu Sep 16 15:52:37 EDT 2010])) or (((etext\\ test) AND (entities.index:\"entity/type\")) and (((ftext) AND (etext\\ \\+test)) or ((ftext +test))))";	
		if (!parseransQ.equals(result.toString())) {
			System.out.println("Fail p3"); System.out.println(parseransQ); System.out.println(result.toString());			
		}
		
		String parser4 = "1 or 2 and (3 or 4)";
		tree = SimpleBooleanParser.parseExpression(parser4);
		parserres = SimpleBooleanParser.traverse(tree, false);
		parserans = "$0: | ($1 1 ) $1: & ($2 2 ) $2: | (4 3 ) ";
		if (!parserans.equals(parserres)) {
			System.out.println("Fail p4"); System.out.println(parser4); System.out.println(parserres);
		}
		bq = QueryBuilders.boolQuery(); result.setLength(0);
		this.parseLogicRecursive(tree, bq, qtTerms.toArray(new BaseQueryBuilder[6]), qtReadTerms.toArray(new StringBuffer[6]), result);
		parseransQ = "((((publishedDate:[0 TO Thu Sep 16 15:52:37 EDT 2010])) or ((etext\\ test) AND (entities.index:\"entity/type\"))) and ((ftext) AND (etext\\ \\+test))) or ((ftext +test))";
		if (!parseransQ.equals(result.toString())) {
			System.out.println("Fail p4"); System.out.println(parseransQ); System.out.println(result.toString());			
		}
		
		String parser5 = "1 or not 2 and not (3 or 4)";
		tree = SimpleBooleanParser.parseExpression(parser5);
		parserres = SimpleBooleanParser.traverse(tree, false);
		parserans = "$0: | ($1 1 ) $1: & ($2 -2 ) $2: -| (4 3 ) ";
		if (!parserans.equals(parserres)) {
			System.out.println("Fail p5"); System.out.println(parser5); System.out.println(parserres);
		}
		bq = QueryBuilders.boolQuery(); result.setLength(0);
		this.parseLogicRecursive(tree, bq, qtTerms.toArray(new BaseQueryBuilder[6]), qtReadTerms.toArray(new StringBuffer[6]), result);
		parseransQ = "(not (((publishedDate:[0 TO Thu Sep 16 15:52:37 EDT 2010])) or ((etext\\ test) AND (entities.index:\"entity/type\"))) and not ((ftext) AND (etext\\ \\+test))) or ((ftext +test))";
		if (!parseransQ.equals(result.toString())) {
			System.out.println("Fail p5"); System.out.println(parseransQ); System.out.println(result.toString());			
		}
		
		String parser6 = "not (1 or (2 and (3 or 4) and 5))";
		tree = SimpleBooleanParser.parseExpression(parser6);
		parserres = SimpleBooleanParser.traverse(tree, false);
		parserans = "$0: & ($1 ) $1: -| ($2 1 ) $2: & (5 $3 2 ) $3: | (4 3 ) ";
		if (!parserans.equals(parserres)) {
			System.out.println("Fail p6"); System.out.println(parser6); System.out.println(parserres);
		}
		bq = QueryBuilders.boolQuery(); result.setLength(0);
		this.parseLogicRecursive(tree, bq, qtTerms.toArray(new BaseQueryBuilder[6]), qtReadTerms.toArray(new StringBuffer[6]), result);
		parseransQ = "not ((((dist(*.geotag, (40.12,-71.34)) < 100km)) and (((publishedDate:[0 TO Thu Sep 16 15:52:37 EDT 2010])) or ((etext\\ test) AND (entities.index:\"entity/type\"))) and ((ftext) AND (etext\\ \\+test))) or ((ftext +test)))";
		if (!parseransQ.equals(result.toString())) {
			System.out.println("Fail p6"); System.out.println(parseransQ); System.out.println(result.toString());			
		}
		
		String parser7 = "not (1 or (2 and (3 or 4) or 5))";
		tree = SimpleBooleanParser.parseExpression(parser7);
		parserres = SimpleBooleanParser.traverse(tree, false);
		parserans = "$0: & ($1 ) $1: -| ($2 1 ) $2: | (5 $3 ) $3: & ($4 2 ) $4: | (4 3 ) ";
		if (!parserans.equals(parserres)) {
			System.out.println("Fail p7"); System.out.println(parser7); System.out.println(parserres);
		}
		bq = QueryBuilders.boolQuery(); result.setLength(0);
		this.parseLogicRecursive(tree, bq, qtTerms.toArray(new BaseQueryBuilder[6]), qtReadTerms.toArray(new StringBuffer[6]), result);
		parseransQ = "not ((((dist(*.geotag, (40.12,-71.34)) < 100km)) or ((((publishedDate:[0 TO Thu Sep 16 15:52:37 EDT 2010])) or ((etext\\ test) AND (entities.index:\"entity/type\"))) and ((ftext) AND (etext\\ \\+test)))) or ((ftext +test)))";
		if (!parseransQ.equals(result.toString())) {
			System.out.println("Fail p7"); System.out.println(parseransQ); System.out.println(result.toString());			
		}
		
		// Pure parsing tests:
		
		String parser8 = "( 1 OR 2 ) OR ( 3 OR 4 ) AND 5";
		tree = SimpleBooleanParser.parseExpression(parser8);
		parserres = SimpleBooleanParser.traverse(tree, false);
		parserans = "$0: | ($1 $2 ) $2: | (2 1 ) $1: & (5 $3 ) $3: | (4 3 ) ";
		if (!parserans.equals(parserres)) {
			System.out.println("Fail p8"); System.out.println(parser8); System.out.println(parserres);
		}
		
		String parser9 = "(( 1 OR 2 ) OR ( 3 OR 4 )) AND 5";
		tree = SimpleBooleanParser.parseExpression(parser9);
		parserres = SimpleBooleanParser.traverse(tree, false);
		parserans = "$0: & (5 $1 ) $1: | ($2 $3 ) $3: | (2 1 ) $2: | (4 3 ) ";
		if (!parserans.equals(parserres)) {
			System.out.println("Fail p9"); System.out.println(parser9); System.out.println(parserres);
		}
		
// Some proximity test code
		
		// First off, check out the distance code
		Double d1 = getDistance("1000");
		if (d1 != 1000.0) {
			System.out.println("1000 vs " + d1);
		}
		d1 = getDistance("10000m");
		if (d1 != 10000*1.150779) {
			System.out.println("1000m vs " + d1);
		}
		d1 = getDistance("1000mi");
		if (d1 != 1000*1.150779) {
			System.out.println("1000mi vs " + d1);
		}
		d1 = getDistance("1000km");
		if (d1 != 1000.0) {
			System.out.println("1000km vs " + d1);
		}
		d1 = getDistance("1000nm");
		if (d1 != 1000.0*1.852) {
			System.out.println("1000nm vs " + d1);
		}
		
		// Then interval test code
		Long l1 = getInterval("month", 'x');
		if (2592000000L != l1) {
			System.out.println("month vs " + l1);
		}
		l1 = getInterval("1", 'd'); // (day)
		if (86400000L != l1) {
			System.out.println("1d vs " + l1);
		}
		l1 = getInterval("10", 'm'); // (month)
		if (25920000000L != l1) {
			System.out.println("10m vs " + l1);
		}
		l1 = getInterval("1", 'y'); // (year)
		if (31536000000L != l1) {
			System.out.println("1y vs " + l1);
		}
		
		// OK this is the difficult bit:
		AdvancedQueryPojo.QueryScorePojo scoreParams = new AdvancedQueryPojo.QueryScorePojo();		
		// Can't unit test this properly, so just rely on the "TEST CODE"
		//NO PROXIMITY SCORING
		addProximityBasedScoring(QueryBuilders.matchAllQuery(), null, scoreParams, null, false);
		
		// Geo only:
		scoreParams.geoProx = new AdvancedQueryPojo.QueryScorePojo.GeoProxTermPojo();
		scoreParams.geoProx.ll = "10.0,20.0";
		scoreParams.geoProx.decay = "100km";
		addProximityBasedScoring(QueryBuilders.matchAllQuery(), null, scoreParams, null, false);
		
		// Geo+time:
		scoreParams.geoProx.ll = "(10.0,20.0)"; // (double check this version works)
		scoreParams.geoProx.decay = "1000nm";
		scoreParams.timeProx = new AdvancedQueryPojo.QueryScorePojo.TimeProxTermPojo();
		scoreParams.timeProx.decay = "month";
		scoreParams.timeProx.time = "2000-01-01";
		addProximityBasedScoring(QueryBuilders.matchAllQuery(), null, scoreParams, null, false);
		
		// Time only:
		scoreParams.geoProx = null;
		scoreParams.timeProx.decay = "1m";
		addProximityBasedScoring(QueryBuilders.matchAllQuery(), null, scoreParams, null, false);		
	}
	
	public enum GeoParseField
	{
		ALL,ASSOC,DOC,ENT;
	}
	
}


