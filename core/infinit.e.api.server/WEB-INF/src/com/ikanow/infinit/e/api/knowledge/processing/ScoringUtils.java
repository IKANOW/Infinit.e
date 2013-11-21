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
package com.ikanow.infinit.e.api.knowledge.processing;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.bson.types.ObjectId;

import com.ikanow.infinit.e.api.knowledge.QueryHandler;
import com.ikanow.infinit.e.api.knowledge.aliases.AliasLookupTable;
import com.ikanow.infinit.e.api.knowledge.processing.ScoringUtils_Associations.StandaloneEventHashAggregator;
import com.ikanow.infinit.e.api.knowledge.processing.ScoringUtils_MultiCommunity.Community_EntityExtensions;
import com.ikanow.infinit.e.data_model.api.knowledge.AdvancedQueryPojo;
import com.ikanow.infinit.e.data_model.api.knowledge.DocumentPojoApiMap;
import com.ikanow.infinit.e.data_model.api.knowledge.GeoAggregationPojo;
import com.ikanow.infinit.e.data_model.api.knowledge.StatisticsPojo;
import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.MongoDbManager;
import com.ikanow.infinit.e.data_model.store.document.AssociationPojo;
import com.ikanow.infinit.e.data_model.store.document.DocCountPojo;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.infinit.e.data_model.store.document.EntityPojo;
import com.ikanow.infinit.e.data_model.store.document.GeoPojo;
import com.ikanow.infinit.e.data_model.store.feature.entity.EntityFeaturePojo;
import com.ikanow.infinit.e.data_model.utils.GeoOntologyMapping;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

public class ScoringUtils 
{	
	private static final Logger logger = Logger.getLogger(ScoringUtils.class);
	
	private AliasLookupTable _s1_aliasLookup = null;
	public void setAliasLookupTable(AliasLookupTable aliasLookup) {
		_s1_aliasLookup = aliasLookup;
	}
	
/////////////////////////////////////////////////////////////////////////////////////////////////////////////	
// 
// OPTIMIZED FULL TFIDF CALCULATIONS	

// Classes required by the calculation
	
	static public class TempDocBucket implements Comparable<TempDocBucket> { // (only needs to be public because of test code)		
		public double docLength = 0; // (number of entities in document, taking frequency into account)
		public long nLeftToProcess = 0; // (state variable used to determine when a feed's score can be calc'd)
										// (after it's been used for that, I steal it to be used as pub-date/10-minutes)
		public BasicDBObject dbo; // (doc object from Mongo)
		public double totalScore = 0.0; // (combined sig/rel)
		public double aggSignificance = 0.0; // (sum of sigs of all entities)
		public double luceneScore = 0.0; // (score from Lucene)
		public double geoTemporalDecay = 1.0; // (decay based on time and location and query params)
		public boolean bPromoted = false;
		public int nLuceneIndex = -1; // (index in the sorted lucene reply)
		public double manualWeighting = 1.0; // (source-specific weighting)
		
		// Deduplication-specific code ... create a simple linked list 
		public int nTieBreaker; // ensures that elements will tend to get put in at the end of the list, which should improve performance 
		public String url = null;
		public TempDocBucket dupList = null; // (linked list starting at the "master" document)
		public int nEntsInDoc = 0; // (performance shortcut for comparing 2 potentially duplicate docs)
		
		// Store explain object (rarely needed) so won't incur map cost across all docs
		public Object explain;
		
		// Deduplication and ordering:
		@Override
		public int compareTo(TempDocBucket rhs) {
			boolean bCloseEnoughToCompare = false;
			
			double diff = this.totalScore - rhs.totalScore;
			
			if (-1 != nLuceneIndex) { // ie sorting by date
				if (this.nEntsInDoc == rhs.nEntsInDoc) { // (don't bother comparing unless they have the same number of entities_
					if (0 == this.nLeftToProcess) {
						try {
							this.nLeftToProcess = ((Date) dbo.get(DocumentPojo.publishedDate_)).getTime()/600000; // (down to 10 minutes==10*60*1000)
						}
						catch (Exception e) {
							this.nLeftToProcess = -1; // no date. don't try again
						}
					}
					if (0 == rhs.nLeftToProcess) {					
						try {
							rhs.nLeftToProcess = ((Date) rhs.dbo.get(DocumentPojo.publishedDate_)).getTime()/600000; // (down to 10 minutes==10*60*1000)
						}
						catch (Exception e) {
							rhs.nLeftToProcess = -1; // no date. don't try again
						}
					}
					if (rhs.nLeftToProcess == this.nLeftToProcess) { // This now contains the date in seconds...
						bCloseEnoughToCompare = true;						
					}
				}
			}
			else { // normal score based sorting:
				bCloseEnoughToCompare = (Math.abs(diff) <= 1.0) && (this.nEntsInDoc == rhs.nEntsInDoc);
			}
			//TESTED (both sort types - by date and by score)
			
			if (bCloseEnoughToCompare) {				
				
				// Get the url /(hash code since that will then get saved) and check that
				if (null == this.url) {
					this.url = dbo.getString(DocumentPojo.url_);
				}
				if (null == rhs.url) {
					rhs.url = rhs.dbo.getString(DocumentPojo.url_);
				}
				if (ScoringUtils_MultiCommunity.community_areDuplicates(this, rhs)) {
					
					this.dupList = rhs.dupList;
					rhs.dupList = this; // (add to very simple linked list)
					return 0;					
				}
				else if (0.0 == diff) {					
					return this.nTieBreaker - rhs.nTieBreaker;
				}
				else return Double.compare(this.totalScore, rhs.totalScore); 
			}
			else if (0.0 == diff) {					
				return this.nTieBreaker - rhs.nTieBreaker;
			}			
			else return Double.compare(this.totalScore, rhs.totalScore); 
		}//TESTED (see TestCode#1)
	};
	static class TempEntityInDocBucket {
		public double freq = 0.0; // (freq pf entity in document, double for MongoDB reasons)
		public BasicDBObject dbo; // (entity object from Mongo)
		public TempDocBucket doc; // (parent document)
	};
	static class EntSigHolder implements Comparable<EntSigHolder> {
		
		EntSigHolder(String index, long nTotalDocCount, ScoringUtils_MultiCommunity multiCommunityHandler) {
			this.index = index; // (used for aliasing only)
			this.nTotalDocCount = nTotalDocCount;
			if (null != multiCommunityHandler) {
				multiCommunityHandler.initializeEntity(this);
			}
		}
		public String index = null; // (only used for aliasing)
			// (ALSO USED FOR ALIASES)
		
		public long nDocCountInQuerySubset = 0; // total number of matching docs in retrieved data
		public double datasetSignificance = 0.0; // calculated weighted avg of doc significances (ie TF*standalone)		
		public double standaloneSignificance = 0.0; // the IDF term of the significance
		public double queryCoverage = 0.0; // the % of documents in the query subset in which the entity occurs
		public double avgFreqOverQuerySubset = 0.0; // the average freq over all documents (not just those in which the entity occurs)
			// (ALSO USED FOR ALIASES)  (ALL FIVE)
		
		//Totals - since don't have "ent" any more
		public long nTotalDocCount = 0; // document count in population
			// (ALSO USED FOR ALIASES)
		
		// To approximate avg significance:
		public double decayedDocCountInQuerySubset = 0.0; // sigma(doc-count-in-query-subset * geo-temporal decay)
			// (ALSO USED FOR ALIASES)
		
		// Some more attempts to avoid going through the DB cursor more than once
		List<TempEntityInDocBucket> entityInstances = new LinkedList<TempEntityInDocBucket>();
		
		// For entity aggregation:
				
		public BasicDBObject unusedDbo = null;
		public double maxDocSig = 0.0;		
			// (ALSO USED FOR ALIASES) (BOTH)
		public double maxFreq = 0.0;
		
		public long nTotalSentimentValues = 0;
		public double positiveSentiment = 0.0;
		public double negativeSentiment = 0.0;
			// (ALSO USED FOR ALIASES) (ALL THREE)
		
		@Override
		public int compareTo(EntSigHolder rhs) {
			return Double.compare(datasetSignificance, rhs.datasetSignificance);
		}
		
		// New code to handle significance approximation for pan-community queries
		// (see "additional functionality #1)
		Community_EntityExtensions community;
		
		// For aliasing:
		public EntSigHolder masterAliasSH = null;
		public EntityFeaturePojo aliasInfo = null;
			// (ALSO USED FOR ALIASES) (BOTH)
		
		// For low accuracy geo
		public BasicDBObject geotaggedEntity = null; // (store the entire ent object so we don't need to pay the deser cost unless it's promoted...)
		public BasicDBObject geotag = null; // (need both of these for onto type + geotag)
	};

// Top level state ("s0" for "stage 0") 

	// (Some processing controls)
	long _s0_nQuerySetDocCount;  // (however many were actually found in the Lucene indexes, NOTE not how many are retrieved from DB)
	int _s0_nQuerySubsetDocCount; // (eg 1000 docus, user limit - ie how many are retrieved from DB)
	boolean _s0_bNeedToCalcSig; // (whether this function needs to calc sig - eg if only being used for standalone events)
	double _s0_globalDocCount;
	
	double _s0_maxLuceneScoreInv; // (unused)
	double _s0_avgLuceneScoreInv; // (used for adjust aggregates' statistics)
	double _s0_avgLuceneScore;
	
	// (Some output controls)
	boolean _s0_sortingByDate = false;
	int _s0_nNumEntsReturn;
	boolean _s0_bNonGeoEnts;
	boolean _s0_bGeoEnts;
	boolean _s0_bEvents;
	boolean _s0_bFacts;
	boolean _s0_bSummaries;
	boolean _s0_bMetadata;

	// Type/Verb filtering:
	HashSet<String> _s0_entityTypeFilter = null;
	boolean _s0_bEntityTypeFilterPositive = true;
	HashSet<String> _s0_assocVerbFilter = null;
	boolean _s0_bAssocVerbFilterPositive = true;
	
	ScoringUtils_MultiCommunity _s0_multiCommunityHandler = null;
		// (handles approximating significance from multiple communities with various overlaps)
	StandaloneEventHashAggregator _s0_standaloneEventAggregator = null;
		// (handles event scoring)
	StandaloneEventHashAggregator _s0_lowAccuracyAssociationAggregator_events = null;
	StandaloneEventHashAggregator _s0_lowAccuracyAssociationAggregator_facts = null;
		// (workarounds for clusters where the Lucene indexes are too large to do faceting)
	
	// TF-params: original suggested values are (0.5, 1.5)
	private static final double TF_PARAM1 = 0.5;
	// I think this ranks docs with many entities up too high:
	// (FYI with (0.5,1.5): for freq==1, doc length==average, then tf term=0.333 (f==2=>0.5); doc length==av*2 => tf=0.222, (f==2=>0.364))
	//private static final double TF_PARAM2 = 1.5;
	// The following value has the property that there's a break-even point at ~3x the average number of entities
	private static final double TF_PARAM2 = 5.5;
	
	// Some support for low accuracy geo:
	LinkedList<EntSigHolder>[] _s3_geoBuckets = null;
	boolean _s3_bLowAccuracyGeo = false;
	boolean _s3_bExtraAliasGeo = false;
	private static final int _s3_nGEO_BUCKETS = 100;
	private static final int _s3_nGEO_BUCKETS_1 = 99;
	private static final double _s3_dGEO_BUCKETS = 100.0;
	double _s2_maxGeoQueryCoverage = 0.0; 	              
	
// Top level logic
	
	@SuppressWarnings("unchecked")
	public List<BasicDBObject> calcTFIDFAndFilter(DBCollection docsDb, DBCursor docs, 
														AdvancedQueryPojo.QueryScorePojo scoreParams, 
														AdvancedQueryPojo.QueryOutputPojo outParams,
														StatisticsPojo scores, boolean bLowAccuracyDecay,
														long nStart, long nToClientLimit,
														String[] communityIds,
														String[] entityTypeFilterStrings, String[] assocVerbFilterStrings,
														LinkedList<BasicDBObject> standaloneEventsReturn,
														LinkedList<BasicDBObject> lowAccuracyAggregatedEnts,
														AggregationUtils.GeoContainer lowAccuracyAggregatedGeo,
														AggregationUtils.GeoContainer extraAliasAggregatedGeo,
														LinkedList<BasicDBObject> lowAccuracyAggregatedEvents,
														LinkedList<BasicDBObject> lowAccuracyAggregatedFacts)
	{
		_s0_multiCommunityHandler = new ScoringUtils_MultiCommunity(communityIds);
		
		_s0_avgLuceneScore = scores.avgScore;
		_s0_avgLuceneScoreInv = 1.0/(scores.avgScore + 0.01); // (+0.01 for safety in case avgScore is small)
		_s0_maxLuceneScoreInv = 1.0/(scores.maxScore + 0.01);		
		
// Utility classes

// Quick check - do I need to be here at all?		
		
		LinkedList<BasicDBObject> returnList = new LinkedList<BasicDBObject>();
		_s0_bNeedToCalcSig = (null != lowAccuracyAggregatedEnts) || 
						(null != lowAccuracyAggregatedEvents) || (null != lowAccuracyAggregatedFacts) ||
									(null != lowAccuracyAggregatedGeo) ||
										((nToClientLimit > 0) && outParams.docs.enable); 
		
		if (!_s0_bNeedToCalcSig && (null == standaloneEventsReturn)) {
			return returnList;
		}//TESTED
		else if (!_s0_bNeedToCalcSig) { // (ie and want standaloneEventsReturn)
			if (scoreParams.sigWeight > 0.0) { // (reverse the call, we want sig for the standalone events)
				_s0_bNeedToCalcSig = true;
				nToClientLimit = 0; // (ensure no docs get accidentally output)
			}
		}//TESTED
		
// Various configuration and state variables		
		
		// Entity aggregation code:
		_s0_nNumEntsReturn = 0;
		if (null != lowAccuracyAggregatedEnts) {
			_s0_nNumEntsReturn = outParams.aggregation.entsNumReturn;
		}
		_s1_entitiesInDataset = new HashMap<String, EntSigHolder>();
		_s1_noEntityBuckets = new ArrayList<TempDocBucket>();
		
		// (User output options)
		_s0_bNonGeoEnts = true;
		_s0_bGeoEnts = true;
		_s0_bEvents = true;
		_s0_bFacts = true;
		_s0_bSummaries = true;
		_s0_bMetadata = true;
		if (null != outParams.docs) {
			if ((null != outParams.docs.metadata) && !outParams.docs.metadata) {
				_s0_bMetadata = false;
			}
			if ((null != outParams.docs.ents) && !outParams.docs.ents) {
				_s0_bNonGeoEnts = false;
				_s0_bGeoEnts = false; // (but can be overridden below)
			}		
			if ((null != outParams.docs.geo) && !outParams.docs.geo) {
				_s0_bGeoEnts = false;			
			}		
			else if ((null != outParams.docs.geo) && outParams.docs.geo) {
				_s0_bGeoEnts = true;			
			}		
			if ((null != outParams.docs.events) && !outParams.docs.events) {
				_s0_bEvents = false;
			}
			if ((null != outParams.docs.facts) && !outParams.docs.facts) {
				_s0_bFacts = false;
			}
			if ((null != outParams.docs.summaries) && !outParams.docs.summaries) {
				_s0_bSummaries = false;
			}
		} //TESTED		
		
		if (null != entityTypeFilterStrings) {
			
			if ('-' == entityTypeFilterStrings[0].charAt(0)) {
				_s0_bEntityTypeFilterPositive = false;
			}
			//TESTED (in both entities and associations)
			_s0_entityTypeFilter = new HashSet<String>();
			for (String entityType: entityTypeFilterStrings) {
				if (!_s0_bEntityTypeFilterPositive && ('-' == entityType.charAt(0))) {
					entityType = entityType.substring(1);
				}
				_s0_entityTypeFilter.add(entityType.toLowerCase());
			}
		}
		if (_s0_bEvents || _s0_bFacts || _s0_bSummaries || (null != standaloneEventsReturn)) { // (ie most of the time!)
			if (null != assocVerbFilterStrings) {
				if ('-' == assocVerbFilterStrings[0].charAt(0)) {
					_s0_bAssocVerbFilterPositive = false;
				}
				//TESTED
				_s0_assocVerbFilter = new HashSet<String>();
				for (String assocVerb: assocVerbFilterStrings) {
					if (!_s0_bAssocVerbFilterPositive && ('-' == assocVerb.charAt(0))) {
						assocVerb = assocVerb.substring(1);
					}
					_s0_assocVerbFilter.add(assocVerb);
				}
			}
		}
		//TESTED
		
		if ((scoreParams.relWeight == 0.0) && (scoreParams.sigWeight == 0.0)) {
			_s0_sortingByDate = true;
		}
		
// First loop: just count and store		
		
		if ((null != standaloneEventsReturn) && (null != outParams.docs) 
				&& (null != outParams.docs.numEventsTimelineReturn) && (outParams.docs.numEventsTimelineReturn > 0))
		{
			_s0_standaloneEventAggregator = new StandaloneEventHashAggregator(standaloneEventsReturn, false, _s1_aliasLookup);
		}
		if ((null != lowAccuracyAggregatedEvents) && (null != outParams.aggregation) 
				&& (null != outParams.aggregation.eventsNumReturn) && (outParams.aggregation.eventsNumReturn > 0))
		{
			_s0_lowAccuracyAssociationAggregator_events = new StandaloneEventHashAggregator(lowAccuracyAggregatedEvents, true, _s1_aliasLookup);
		}
		if ((null != lowAccuracyAggregatedFacts) && (null != outParams.aggregation) 
				&& (null != outParams.aggregation.factsNumReturn) && (outParams.aggregation.factsNumReturn > 0))
		{
			_s0_lowAccuracyAssociationAggregator_facts = new StandaloneEventHashAggregator(lowAccuracyAggregatedFacts, true, _s1_aliasLookup);
		}
		if ((null != lowAccuracyAggregatedGeo) && (null != outParams.aggregation) 
				&& (null != outParams.aggregation.geoNumReturn) && (outParams.aggregation.geoNumReturn > 0))
		{
			// Initialize the buckets
			_s3_geoBuckets = (LinkedList<EntSigHolder>[])new LinkedList[_s3_nGEO_BUCKETS]; 
			_s3_bLowAccuracyGeo = true;
		}
		if ((null != extraAliasAggregatedGeo) && (null != outParams.aggregation) 
				&& (null != outParams.aggregation.geoNumReturn) && (outParams.aggregation.geoNumReturn > 0))
		{
			_s3_bExtraAliasGeo = true; 
			// (don't initialize _s3_geoBuckets until we have to)
		}
		if (bLowAccuracyDecay) {
			_s1_dManualGeoDecay_latLonInvdecay = QueryHandler.parseGeoDecay(scoreParams);
		}//TESTED
		
		_s0_nQuerySubsetDocCount = docs.size(); // eg (1000 docus, user limit)
		_s0_nQuerySetDocCount = scores.found;  // however many were actually found
		
		//lookup the totaldoc count
		_s0_globalDocCount = 0;
		long nGlobalDocCount = 0;
		try  {
			nGlobalDocCount = getDocCount(_s0_multiCommunityHandler.getCommunityIds());
		} 
		catch (Exception e) {
			// If an exception occurs log the error
			logger.error("Exception Message: " + e.getMessage(), e);
		}
		// (End doccount)

		if (_s0_nQuerySetDocCount > nGlobalDocCount) {
			nGlobalDocCount = _s0_nQuerySetDocCount;
				// (This can happen if the source doc counts get out of sync...
				// ... conversely if the index/db get out of sync, the other way round can be correct, but this way is safer)
		}
		_s0_globalDocCount = (double)nGlobalDocCount;
		stage1_initialCountingLoop(docs, scoreParams, scores, standaloneEventsReturn, communityIds.length);
		
		//Exit if not generating documents or entity aggregations:
		if (!_s0_bNeedToCalcSig) {
			return returnList;
		}//TESTED
		
// Histogram time:
		
		this.stage2_generateFreqHistogramCalcIDFs();
		
// Next stop: loop over the entities and calculate the IDF terms   

		this.stage3_calculateTFTerms(scoreParams, scores, nStart + nToClientLimit);
			// (get extra docs to handle deduplication)
		
// Finally, write all the information to the surviving 100 (or whatever) documents  		

		// Handle skipping past the end:
		if ((nStart + nToClientLimit) > _s3_pqDocs.size()) {
			nToClientLimit = _s3_pqDocs.size() - nStart;
			if (nToClientLimit < 0) {
				nToClientLimit = 0;
			}
		}
		this.stage4_prepareDocsForOutput(scoreParams, scores, nToClientLimit, returnList);
		
// And then same for entities		
		
		this.stage4_prepareEntsForOutput(lowAccuracyAggregatedEnts);
		
		//Association is mostly done on the fly, but a final tidy up:
		if (null != standaloneEventsReturn) {
			ScoringUtils_Associations.finalizeStandaloneEvents(standaloneEventsReturn, _s0_standaloneEventAggregator, outParams.docs.numEventsTimelineReturn);
		}		
		if (null != _s0_lowAccuracyAssociationAggregator_events) {
			ScoringUtils_Associations.finalizeStandaloneEvents(lowAccuracyAggregatedEvents, _s0_lowAccuracyAssociationAggregator_events, outParams.aggregation.eventsNumReturn);			
		}
		if (null != _s0_lowAccuracyAssociationAggregator_facts) {
			ScoringUtils_Associations.finalizeStandaloneEvents(lowAccuracyAggregatedFacts, _s0_lowAccuracyAssociationAggregator_facts, outParams.aggregation.factsNumReturn);			
		}
		// Geo is mostly done on the fly, but a final tidy up:
		if (null != lowAccuracyAggregatedGeo) {
			finalizeLowAccuracyGeoAggregation(lowAccuracyAggregatedGeo, outParams.aggregation.geoNumReturn);
				// (outParams.aggregation.geoNumReturn must exist if (null != lowAccuracyAggregatedGeo)) 
		}
		else if ((null != extraAliasAggregatedGeo) && (null != _s3_geoBuckets)) {
			finalizeLowAccuracyGeoAggregation(extraAliasAggregatedGeo, Long.MAX_VALUE);
				//(at most 1 per alias so size shouldn't be an issue)
		}
		return returnList;
	}

/////////////////////////////////////////////////////////////
	
	// (Top level logic - associations)
	
	public boolean calcAssocationSignificance(String ent1_index, String ent2_index, String geo_index, BasicDBObject assoc) {
		
		if ((null == _s1_entitiesInDataset) || _s1_entitiesInDataset.isEmpty()) {
			return false;
		}
		else {
			ScoringUtils_Associations.calcAssocationSignificance(ent1_index, ent2_index, geo_index, assoc, _s1_entitiesInDataset);			
		}
		return true;
	}
	
/////////////////////////////////////////////////////////////////////////////////////////////////////////////

// SUB-FUNCTIONS

/////////////////////////////////////////////////////////////
	
// 1] stage1_initialCountingLoop()
	// Loops over the data a first time and generates basic statistics required by the more complex
	// functionality that follow

	// Input:
	
	double _s1_dManualGeoDecay_latLonInvdecay[] = null;
		// (this is needed if internal Lucene geo decay is turned off for performance reasons)

	// Output:
	
	double _s1_sumFreqInQuerySubset = 0; // (the sum of all the frequencies in the received matching (sub-)dataset)
	HashMap<String, EntSigHolder> _s1_entitiesInDataset; // (map of entities to various stats)
	ArrayList<TempDocBucket> _s1_noEntityBuckets; // (docs with no entities)
	HashMap<String, EntSigHolder> _s1_aliasSummary = null; // (for aggregating entities by their alias)	
		
	// Logic:
	
	@SuppressWarnings("unchecked")
	private void stage1_initialCountingLoop(DBCursor docs, 
										AdvancedQueryPojo.QueryScorePojo scoreParams, 
										StatisticsPojo scores,
										LinkedList<BasicDBObject> standaloneEventsReturn, 
										int nCommunities)
	{		
		double s0_nQuerySubsetDocCountInv = 1.0/(double)_s0_nQuerySubsetDocCount;
		
		for ( DBObject f0 : docs)
		{
			BasicDBObject f = (BasicDBObject)f0;
			// Simple handling for standalone events
			if ((null != _s0_standaloneEventAggregator) && !_s0_bNeedToCalcSig) {
				//if _s0_bNeedToCalcSig then do this elsewhere
				ScoringUtils_Associations.addStandaloneEvents(f, 0.0, 0, _s0_standaloneEventAggregator, 
																_s0_bEntityTypeFilterPositive, _s0_bAssocVerbFilterPositive, _s0_entityTypeFilter, _s0_assocVerbFilter, 
																	_s0_bEvents, _s0_bSummaries, _s0_bFacts);
			}//TESTED
			
			if (!_s0_bNeedToCalcSig) {
				continue;
			}//TESTED
	
			if (nCommunities > 1) { // (could have pan-community entities)
				ObjectId communityId = (ObjectId) f.get(DocumentPojo.communityId_);
				if (null != communityId) { // (have big problems if so, but anyway!)
					_s0_multiCommunityHandler.community_getIdAndInitialize(communityId, _s1_entitiesInDataset);
						// (returns an int community id but also sets it into the cache, so just use that below)
				}
			}//TESTED		
			
			TempDocBucket docBucket = new TempDocBucket();
			docBucket.dbo = f;
			ObjectId id = (ObjectId) f.get(DocumentPojo._id_);
			
			// If we're going to weight relevance in, or we need the geo temporal decay:
			if ((0 != scoreParams.relWeight) || (null != scoreParams.timeProx) || (null != scoreParams.geoProx)) {
				StatisticsPojo.Score scoreObj = scores.getScore().get(id);
				if (null != scoreObj) {
					docBucket.explain = scoreObj.explain; // (will normally be null)
					docBucket.luceneScore = scoreObj.score;
					if ((null != scoreParams.timeProx) || (null != scoreParams.geoProx)) {
						if (scoreObj.decay >= 0.0) {
							docBucket.geoTemporalDecay = scoreObj.decay;
						}
						// (see also below for low accuracy geo scoring)
					}
				}
			}//TESTED
			else if (this._s0_sortingByDate) {
				StatisticsPojo.Score scoreObj = scores.getScore().get(id);
				if (null != scoreObj) {
					docBucket.nLuceneIndex = scoreObj.nIndex;			
				}				
			}			
			docBucket.manualWeighting = this.getManualScoreWeights(scoreParams, f);
			
			BasicDBList l = (BasicDBList)(f.get(DocumentPojo.entities_));
			if (null != l) {
	
				long nEntsInDoc = l.size();
				double dBestGeoScore = 0.0; // (for low accuracy geo only)
				for(Iterator<?> e0 = l.iterator(); e0.hasNext();){					
					BasicDBObject e = (BasicDBObject)e0.next();

					BasicDBObject tmpGeotag = null;
					if (_s3_bLowAccuracyGeo || (null != _s1_dManualGeoDecay_latLonInvdecay)) { 
						// low accuracy geo, need to look for geotag
						tmpGeotag = (BasicDBObject) e.get(EntityPojo.geotag_);
					}
										
					// Get attributes
					
					double freq = -1.0;
					long ntotaldoccount = -1;
					String entity_index;
					Double sentiment = null;
					try {
						sentiment = (Double) e.get(EntityPojo.sentiment_);
						ntotaldoccount = e.getLong(EntityPojo.doccount_);
						freq = e.getDouble(EntityPojo.frequency_);
						entity_index = e.getString(EntityPojo.index_);
						if (null == entity_index) {
							// Just bypass the entity 
							e.put(EntityPojo.significance_, 0.0);
							nEntsInDoc--;
							continue;
						}
					}
					catch (Exception ex) {
						try {
							String sfreq;
							if (ntotaldoccount < 0) {
								sfreq = e.getString(EntityPojo.doccount_) ;
								ntotaldoccount = Long.valueOf(sfreq);								
							}
							if (freq < -0.5) {
								sfreq = e.getString(EntityPojo.frequency_) ;
								freq = Long.valueOf(sfreq).doubleValue();
							}
							entity_index = e.getString(EntityPojo.index_);
							if (null == entity_index) {
								// Just bypass the entity 
								e.put(EntityPojo.significance_, 0.0);
								nEntsInDoc--;
								continue;
							}
						}
						catch (Exception e2) {						
							// Just bypass the entity 
							e.put(EntityPojo.significance_, 0.0);
							nEntsInDoc--;
							continue;						
						}
					}//TESTED
					
					// First loop through is just counting
					
					// Retrieve entity (create/initialzie if necessary)
					EntSigHolder shp = _s1_entitiesInDataset.get(entity_index);
					if (null == shp) {		
						if (ntotaldoccount > (long)_s0_globalDocCount) { // obviously can't have more entities-in-dos than docs... 
							ntotaldoccount = (long)_s0_globalDocCount;
						}
						shp = new EntSigHolder(entity_index, ntotaldoccount, _s0_multiCommunityHandler);						
						
						// Stage 1a alias handling: set up infrastructure, calculate doc overlap
						if (null != _s1_aliasLookup) {
							stage1_initAlias(shp);
						}
						if ((null != shp.aliasInfo) && (null == shp.masterAliasSH)) { // this is the discard alias
							nEntsInDoc--;
							continue;
						}//TESTED
						
						// Check if entity is in type filter list
						if (null != _s0_entityTypeFilter) {
							String entType = null;
							if (null != shp.aliasInfo) {
								entType = shp.aliasInfo.getType();
							}
							else {
								entType = e.getString(EntityPojo.type_);
							}
							if (_s0_bEntityTypeFilterPositive) {
								if ((null != entType) && !_s0_entityTypeFilter.contains(entType.toLowerCase())) {
									nEntsInDoc--;
									continue;
								}
							}
							else if ((null != entType) && _s0_entityTypeFilter.contains(entType.toLowerCase())) {
								//(negative filter)
								nEntsInDoc--;
								continue;
							}
							
						}//TESTED (end entity filter)
						
						// Geo:
						if (null != shp.aliasInfo) {
							if (null != shp.aliasInfo.getGeotag()) { //Geo, overwrite/create tmpGeotag
								if (_s3_bLowAccuracyGeo || _s3_bExtraAliasGeo || (null != _s1_dManualGeoDecay_latLonInvdecay)) { 
									// Always capture alias geo, even if not in low accuracy mode because we add it to the 
									// legitimate geo:
									if ((_s3_bLowAccuracyGeo || _s3_bExtraAliasGeo) && 
											(null == _s3_geoBuckets))
									{
										// Initialize the buckets if this is for aggregation not just decay
										_s3_geoBuckets = (LinkedList<EntSigHolder>[])new LinkedList[_s3_nGEO_BUCKETS]; 									
									}
									
									if (null == tmpGeotag) {
										tmpGeotag = new BasicDBObject();
									}
									tmpGeotag.put(GeoPojo.lat_, shp.aliasInfo.getGeotag().lat);
									tmpGeotag.put(GeoPojo.lon_, shp.aliasInfo.getGeotag().lon);
	
									if (null != shp.aliasInfo.getOntology_type()) {
										e.put(EntityPojo.ontology_type_, shp.aliasInfo.getOntology_type());
									}
								}
							}
						}//TESTED (end geo for aggregation or decay)
						
						_s1_entitiesInDataset.put(entity_index, shp);
						// end Stage 1a alias handling
					}//(end if is alias)
					
					// Stage 1b alias handling: calculate document counts (taking overlaps into account)
					if (null != shp.masterAliasSH) {
						// Counts:
						shp.masterAliasSH.nTotalDocCount++; 
							// docs including overlaps
						shp.masterAliasSH.avgFreqOverQuerySubset += freq;

						// Keep track of overlaps:
						if (f != shp.masterAliasSH.unusedDbo) {
							shp.masterAliasSH.unusedDbo = f;
								// (note this is only used in stage 1, alias.unusedDbo is re-used differently in stage 3/4)
							shp.masterAliasSH.nDocCountInQuerySubset++;
								// non-overlapping docs ie < shp.nDocCountInQuerySubset
						}
						
						// Sentiment:
						shp.masterAliasSH.positiveSentiment += shp.positiveSentiment;
						shp.masterAliasSH.negativeSentiment += shp.negativeSentiment;
						if (null != sentiment) {
							shp.masterAliasSH.nTotalSentimentValues++;
						}
						
					}//TESTED (end if is alias)
					// end Stage 1b
										
					// Pan-community logic (this needs to be before the entity object is updated)
					if (_s0_multiCommunityHandler.isActive()) {
						_s0_multiCommunityHandler.community_updateCorrelations(shp, ntotaldoccount, entity_index);
					}		
					else { // (Once we've started multi-community logic, this is no longer desirable)
						if ((ntotaldoccount > shp.nTotalDocCount) && (ntotaldoccount <= _s0_globalDocCount)) {
							shp.nTotalDocCount = ntotaldoccount;
						}						
						//(note there used to be some cases where we adjusted for dc/tf==0, but the 
						// underlying issue in the data model that caused this has been fixed, so it's 
						// now a pathological case that can be ignored)
					}//(TESTED)
					
					// Update counts:
					_s1_sumFreqInQuerySubset += freq;
					shp.avgFreqOverQuerySubset += freq;
					shp.nDocCountInQuerySubset++; 
					shp.decayedDocCountInQuerySubset += docBucket.geoTemporalDecay;
						// (note this doesn't handle low accuracy geo-decay ... we'll address that via a separate term)

					TempEntityInDocBucket entBucket = new TempEntityInDocBucket();
					entBucket.dbo = e;
					entBucket.freq = freq;
					entBucket.doc = docBucket;
					shp.entityInstances.add(entBucket);
					if (null != tmpGeotag) { // (only needed for low accuracy geo aggregation)
						
						if ((_s3_bLowAccuracyGeo || _s3_bExtraAliasGeo) && (null == shp.geotag)) { // (first time for shp only)
							shp.geotag = tmpGeotag;
							shp.geotaggedEntity = e; // (ie for onto type, which has been overwritten in the alias case...)
						}						
						if (null != _s1_dManualGeoDecay_latLonInvdecay) {
							// Emulate scripted Lucene calculations
							double minlat = tmpGeotag.getDouble(GeoPojo.lat_);
							double minlon = tmpGeotag.getDouble(GeoPojo.lon_);
							double paramlat = _s1_dManualGeoDecay_latLonInvdecay[0];
							double paramlon = _s1_dManualGeoDecay_latLonInvdecay[1];
							double gdecay = _s1_dManualGeoDecay_latLonInvdecay[2];
							char ontCode = GeoOntologyMapping.encodeOntologyCode(e.getString(EntityPojo.ontology_type_));
							double dDecay = QueryDecayScript.getGeoDecay(minlat, minlon, paramlat, paramlon, gdecay, ontCode);
							if (dDecay > dBestGeoScore) {
								dBestGeoScore = dDecay;
							}
						}//TESTED
					}//(end if entity has geo and need to process entity geo)
					
					if (freq > shp.maxFreq) {
						shp.maxFreq = freq;
					}
					// Sentiment:
					if ((null != sentiment) && (Math.abs(sentiment) <= 1.1)) { // (actually 1.0)
						shp.nTotalSentimentValues++;
						if (sentiment > 0.0) {
							shp.positiveSentiment += sentiment;
						}
						else {
							shp.negativeSentiment += sentiment;							
						}
					}
					else if (null != sentiment) { // corrupt sentiment for some reason?!
						e.put(EntityPojo.sentiment_, null);
					}
					docBucket.docLength += freq;
					
				} //(end loop over entities)
				
				docBucket.nLeftToProcess = nEntsInDoc;
				docBucket.nEntsInDoc = (int) nEntsInDoc;
				
				if (null != this._s1_dManualGeoDecay_latLonInvdecay) { // Low accuracy geo-calculations
					docBucket.geoTemporalDecay *= dBestGeoScore;
					docBucket.luceneScore *= dBestGeoScore;
					_s2_dAvgLowAccuracyGeoDecay += dBestGeoScore*s0_nQuerySubsetDocCountInv;
				}//TESTED				
				
			} // (end if feed has entities)
	
			// Handle documents with no entities - can still promote them
			if (0 == docBucket.nLeftToProcess) { // (use this rather than doc length in case all the entities had freq 0)
				_s1_noEntityBuckets.add(docBucket);				
			}
			
		} // (end loop over feeds)
		//TESTED
	}
	
/////////////////////////////////////////////////////////////	
	
// 2] stage2_generateFreqHistogramCalcIDFs()
	// Generates a histogram of entity frequencies that can be used to suppress the significance
	// of likely false positives
	// Then calculates the IDFs of each entity (including cross-community scoring adjustments if necessary)
	
	// Inputs
	
	double _s2_dAvgLowAccuracyGeoDecay = 0.0; // for low accuracy geo a further approximation...
	
	// Outputs
	
	double _s2_dApproxAverageDocumentSig; // Approximate calculated here for convenience, used later on
	int _s2_nMush1Index; // 33% significance frequency (very likely to be false positive)
	int _s2_nMush2Index; // 66% significance frequency (quite likely to be false positive)
	
	// Logic
	
	private void stage2_generateFreqHistogramCalcIDFs() {
		
		final int nMaxHistBins = 25;
		long nCountHistogram[] = new long[nMaxHistBins];
		
		// Prep histogram
		int nHistBins = 1 + (int)(_s0_nQuerySubsetDocCount/50); // (eg 21 bins for 1000 documents)
		if (nHistBins > nMaxHistBins) {
			nHistBins = nMaxHistBins;
		}
		//TESTED
		
		// (Sadly requires 1 spurious loop over the entities, shouldn't add too much extra)
		// Will take the opportunity to calculate the standalone entity significances here
		
		// OK looking at IDF equations below, the significance's maximum value is (entity appears only in query set)
		// log(doccount*nQuerySubsetDocCount/0.25) ... so we'll scale that to be 100%
		double dScaleFactor = 100.0/Math.log10((_s0_globalDocCount*_s0_nQuerySetDocCount+0.5)/0.25);
			// (note this isn't quite right anymore because of the adjustments performed below, but it does a reasonable
			//  job and the actual value is now very complicated...)
		double dHalfScaleFactor2 = 0.5*((0.5 + (double)_s0_nQuerySetDocCount)/(0.5 + _s0_globalDocCount)); 
		
		// Pre-calculated scalors to use in query coverage
		double halfQueryDocSubsetInv = 0.5/(0.5 + _s0_nQuerySubsetDocCount); // (case 2.1 below - needs multipled by the entity's query count)
		double halfGlobalDocCountInv = 0.5/(0.5 + _s0_globalDocCount); // (case 2.2 below - needs multipled by the entity's total count)
						
		_s2_dApproxAverageDocumentSig = 0.0; // (used to normalize vs the relevance)
		
		// Some TF-related numbers
		// (no longer needed since we calculate the average TF based on an average entity count, for performance reasons) 
		//double invAvgLength = ((double)_s0_nQuerySubsetDocCount/(_s1_sumFreqInQuerySubset + 0.01));

		// Pre-calculate a few dividors used in the loop below: 
		double s0_nQuerySubsetDocCountInv = 1.0/(double)_s0_nQuerySubsetDocCount;
		double s0_nQuerySetDocCountInv = 1.0/(double)_s0_nQuerySetDocCount;
		
		for (EntSigHolder shp: _s1_entitiesInDataset.values()) {
									
			double avgFreqPerEntity = shp.avgFreqOverQuerySubset/shp.nDocCountInQuerySubset;
				// (do this here because can overwrite shp.nDocCountInQuerySubset further below, losing direct link with shp.avgFreq)
			
			if (shp.nDocCountInQuerySubset < nHistBins) {							
				nCountHistogram[(int)shp.nDocCountInQuerySubset]++;
			}
			
			//(Robustness)
			if (shp.nTotalDocCount < shp.nDocCountInQuerySubset) {
				shp.nTotalDocCount = shp.nDocCountInQuerySubset;
			}				
			if (_s0_nQuerySubsetDocCount < shp.nDocCountInQuerySubset) {
				shp.nDocCountInQuerySubset = _s0_nQuerySubsetDocCount; 
			}
			
			// Transform from a ratio involving nQuery*Subset*DocCount to a ratio of nQuery*Set*DocCount
			double estEntityDocCountInQuery = (double)shp.nDocCountInQuerySubset; // (case 1 below)
			// Cases 
				// 1] if "shp.nTotalDocCount <= shp.nDocCountInQuerySubset" OR "shp.nTotalDocCount == shp.nDocCountInQuerySubset" 
				//    then know that all instances were in nQuery*Set*DocCount (else the available entities is the smaller of the 2 diffs, see below)
				// 2] Otherwise we don't know, maybe we can guess:
					// 2.1] If the subset-ratio is correct then it will be 
						// MIN[nQuerySetDocCount*(shp.nDocCountInQuerySubset/nQuerySubsetDocCount),nDocCountDiff] + shp.nDocCountInQuerySubset  
					// 2.2] If it's actually randomly distributed then it will be 
						// (nQuerySetDocCount/globalDocCount)*nDocCountDiff + shp.nDocCountInQuerySubset
					// So we'll average the 2 and call it a day
			if ((shp.nTotalDocCount > shp.nDocCountInQuerySubset) && (_s0_nQuerySetDocCount > _s0_nQuerySubsetDocCount)) {
				double docCountDiff = (double)(_s0_nQuerySetDocCount - _s0_nQuerySubsetDocCount);
				docCountDiff = Math.min(docCountDiff, (double)(shp.nTotalDocCount - shp.nDocCountInQuerySubset));
					// ie there are 2 differences:	the number of available entities in the total doc count
					//								the number of available documents in the un-queried dataset
				
				estEntityDocCountInQuery += halfQueryDocSubsetInv*shp.nDocCountInQuerySubset*docCountDiff;
				estEntityDocCountInQuery += halfGlobalDocCountInv*shp.nTotalDocCount*docCountDiff;
			}//TESTED
						
			// IDF component of entity
			
			double adjustEntTotalDocCount = shp.nTotalDocCount + _s0_multiCommunityHandler.community_estimateAnyMissingDocCounts(shp);
			
			shp.standaloneSignificance = dScaleFactor*Math.log10(
										((estEntityDocCountInQuery+0.5)/
											(_s0_nQuerySetDocCount - estEntityDocCountInQuery+0.5))
										/
										((adjustEntTotalDocCount - estEntityDocCountInQuery+0.5)/
											((_s0_globalDocCount - _s0_nQuerySetDocCount) - (adjustEntTotalDocCount - estEntityDocCountInQuery)+0.5))
									);
			
			if ((shp.standaloneSignificance <= 0.0) ||   
					(Double.isInfinite(shp.standaloneSignificance)) || Double.isNaN(shp.standaloneSignificance)) 
			{ 
				// Probably matches on the entire index or something like that, use a diff equation:
				// (basically ignore the denominator...)
				
				if ((2.0*_s0_nQuerySetDocCount) >= (_s0_globalDocCount)) { // (to within 33% ... after that we'll start to trust it)
					final double dBackupScalingFactor = 200.0/Math.log10(2);//200 vs 100 to counteract use of dHalfScaleFactor2
					
					// Use dHalfScaleFactor2 (see case 2.2)==0.5*((0.5 + (double)_s0_nQuerySetDocCount)/(0.5 + _s0_globalDocCount))
					// basically to suppress any non-matching records that (almost certainly) don't contain the entity
					
					shp.standaloneSignificance = dHalfScaleFactor2*dBackupScalingFactor*
									Math.log10((_s0_globalDocCount+shp.nDocCountInQuerySubset+0.5)/(_s0_globalDocCount+0.5));
						// (note if (shp.nDocCountInQuerySubset==_s0_nQuerySetDocCount) then this==100% because of defn of dBackupScalingFactor)
					
					if ((shp.standaloneSignificance < 0.0) || 
							(Double.isInfinite(shp.standaloneSignificance)) || Double.isNaN(shp.standaloneSignificance)) // (cleanup)
					{
						shp.standaloneSignificance = 0.0;
					}
				}
				else {
					shp.standaloneSignificance = 0.0;					
				}
								
			}//TESTED (vs entire dataset) 
			
			// Use an "estimated query coverage" (instead of the exact one over the subset)
			shp.queryCoverage = (100.0*(estEntityDocCountInQuery*s0_nQuerySetDocCountInv));
			shp.avgFreqOverQuerySubset *= s0_nQuerySubsetDocCountInv;

			if (null != shp.geotag) { // (only happens for low accuracy geo aggregation)
				if (shp.queryCoverage > _s2_maxGeoQueryCoverage) {
					_s2_maxGeoQueryCoverage = shp.queryCoverage;
				}
			}
			
			double dApproxAvgTfTerm = avgFreqPerEntity/
											(avgFreqPerEntity + TF_PARAM1 + TF_PARAM2);
				// (An approximation for the TF for this entity - assume on average that the entity occurs in docs 
				//  with an average doc length, to avoid an extra loop here or in S1 to calc "avg doc length for docs containing entity)
				// (We're summing this across all entities anyway, so it's not like it would be a particularly accurate number anyway...)
			
			if (_s2_dAvgLowAccuracyGeoDecay > 0.0) { // Take into account average low accuracy geo-decay across the entire dataset
				dApproxAvgTfTerm *= _s2_dAvgLowAccuracyGeoDecay;
			}			
			_s2_dApproxAverageDocumentSig += shp.decayedDocCountInQuerySubset*dApproxAvgTfTerm*shp.standaloneSignificance;
				// (ie an approximation to sum(TF-IDF) across docs

			// Stage 2 alias processing: calc pythag significance, store first/last values ready for S3
			if (null != shp.masterAliasSH) {
				if (null == shp.masterAliasSH.index) {
					shp.masterAliasSH.index = shp.index; // (used so I know I'm the first alias in the global list)
					shp.masterAliasSH.avgFreqOverQuerySubset *= s0_nQuerySubsetDocCountInv;
						// (can't do query coverage yet, we're still summing over the adjusted total doc counts)
					
					// pre-calculate and store an overlap scalor to apply to query coverage 
					shp.masterAliasSH.decayedDocCountInQuerySubset = (double)shp.masterAliasSH.nDocCountInQuerySubset/
																		(double)shp.masterAliasSH.nTotalDocCount;

				}//TESTED
				shp.masterAliasSH.queryCoverage += shp.queryCoverage*shp.masterAliasSH.decayedDocCountInQuerySubset;
					// (my not-very-good estimate sort-of-adjusted for overlap)
				
				shp.masterAliasSH.standaloneSignificance += shp.standaloneSignificance*shp.standaloneSignificance;
					// (combine using pythag, like I do elsewhere for an easy approximation)
				shp.masterAliasSH.masterAliasSH = shp; // (used so I know I'm the last alias in the global list)
				
			}//TESTED
			// end stage 2 alias processing
			
		}//(end stage 2 loop over entities)
		//TESTED (by eye for a 114 document query and a 646 document query) 

		_s2_dApproxAverageDocumentSig *= s0_nQuerySubsetDocCountInv;		
					
		// Intention is now to do some false positive reduction
		double peak = 0.0;
		_s2_nMush1Index = nHistBins; // 33% significance
		_s2_nMush2Index = nHistBins; // 66% significance
		
		double lastval = -1.0;
		for (int i = 1; i < nHistBins; ++i) {
			double val = (double)nCountHistogram[i]; 
			if (val > peak) {
				peak = val;
			}
			else {
				if (lastval >= 0.0) { // ie have got the 5% mark, now look for noise floor 
					if (val >= (lastval - 1.5)) { // noise floor!
						_s2_nMush2Index = i;	
						break; // (nothing left to do)
					}
					lastval = val;
				}
				else if (val < 0.05*peak) { //5%
					_s2_nMush1Index = i;
					lastval = val;
				}
			}
		} // (end loop over histobins)
		//TESTED				
				
	}

/////////////////////////////////////////////////////////////	
	
// 3] stage3_calculateTFTerms()
	// Calculate the entities' and documents' TF-IDF scores (already calculated IDF in stage2)
	
	// Output
	
	// For these 2: lower order (ie significance) puts you at the front of the Q
	java.util.TreeSet<TempDocBucket> _s3_pqDocs; // (doc queue for output - use a TreeSet + custom separator to do deduplication at the same time)
	java.util.PriorityQueue<EntSigHolder> _s3_pqEnt; // (entity queue for output, dedup not an issue for entities)

	double _s3_dLuceneScalingFactor; // How to weight relevance (using scoreParams config)
	double _s3_dSigScalingFactor; // How to weight significance (using scoreParams config)
	double _s3_dScoreScalingFactor; // How to weight total score (using scoreParams config)
	
	// Logic
	
	private void stage3_calculateTFTerms(AdvancedQueryPojo.QueryScorePojo scoreParams, 
									StatisticsPojo scores, long nToClientLimit)
	{
		// First off: we have an approximate average significance, we're going to create a scaling factor for 
		// relevance to fit in with the input parameters
		// Average doc score will be 100
		
		_s3_pqDocs = new java.util.TreeSet<TempDocBucket>();
		_s3_pqEnt = null;
		if (_s0_nNumEntsReturn > 0) {
			_s3_pqEnt = new java.util.PriorityQueue<EntSigHolder>();
		}

		// Calculate scaling factors:
		_s3_dSigScalingFactor = 1.0;			
		if (scoreParams.sigWeight != 0.0) {
			double d = (scoreParams.relWeight/scoreParams.sigWeight);
			_s3_dLuceneScalingFactor = (d*_s2_dApproxAverageDocumentSig)/
										(_s0_avgLuceneScore + 0.01); // (eg scale1*avQuery == (r/s)*avAggSig)			
			_s3_dScoreScalingFactor = 100.0/
									((1.0 + d)*_s2_dApproxAverageDocumentSig); // ie scale2*(scale1*avQuery + avAggSig)==100.0
			
			// Special case: all significances are 0:
			if (_s2_dApproxAverageDocumentSig == 0.0) { // just ignore significance
				_s3_dScoreScalingFactor = 100.0/_s0_avgLuceneScore;
				_s3_dLuceneScalingFactor = 1.0;
				_s3_dSigScalingFactor = 0.0;
			}
		}
		else { // Ignore significance
			_s3_dLuceneScalingFactor = 1.0;
			_s3_dSigScalingFactor = 0.0;
			_s3_dScoreScalingFactor = 100.0/_s0_avgLuceneScore;
		}
		//TESTED
		
		// (See wiki thoughts on not basing this on the query sub-set (eg 1000 is totally arbitrary) ... I like this current way)
		// Take set A == 1000 docs (ent hits = dc_in_sset), set B = #hits (ent hits = unknown), set C = total space (ent hits = dc)
		// If dc==dc_in_sset then *know* that ent hits in set B = dc, so you can divide by size of B
		// Where dc>dc_in_sset, you don't know how those remaining hits are partitioned between B and C
		// Use min(|B|*(dc_in_sset/|A|),dc) as one extreme, (dc-dc_in_sset)*|B|/|C| as other
		
		double invAvgLength = ((double)_s0_nQuerySubsetDocCount/(_s1_sumFreqInQuerySubset + 0.01));
		int n1Down = 0; // (ensures where scores are equal documents are added last, should make a small difference to performance)
		
		for (EntSigHolder shp: _s1_entitiesInDataset.values()) {
			//(NOTE: important that we loop over this in the same order as we looped over it in stage 2)

			// Stage 3a alias processing:
			if (null != shp.masterAliasSH) {
				if (shp.index == shp.masterAliasSH.index) { // First instance of this alias set...
					shp.masterAliasSH.standaloneSignificance = Math.sqrt(shp.masterAliasSH.standaloneSignificance);
					// OK now all the stats are up-to-date
				}
			}//TESTED
			// end Stage 3a alias processing:
			
			//(IDF component calculated above)
			
			// Now calculate the term frequencies
			
			for (TempEntityInDocBucket entBucket : shp.entityInstances) {
				
				double tf_term = (entBucket.freq / 
						(entBucket.freq+TF_PARAM1 + TF_PARAM2*((entBucket.doc.docLength+0.01)*invAvgLength)));
				
				if (shp.nDocCountInQuerySubset <= _s2_nMush1Index) {
					tf_term *= 0.33;
				}
				else if (shp.nDocCountInQuerySubset <= _s2_nMush2Index) {
					tf_term *= 0.66;							
				}
				double tf_idf_sig = tf_term*shp.standaloneSignificance*entBucket.doc.manualWeighting;
				//TESTED

				// Insert significance, unfortunately need to do this spuriously for low prio cases
				// (this could probably be accelerated by recalculating from the IDF and freq only for the top N docs, but empirically doesn't seem worth it)
				if (Double.isNaN(tf_idf_sig)) {
					entBucket.dbo.put(EntityPojo.significance_, 0.0);					
				}
				else {
					entBucket.dbo.put(EntityPojo.significance_, tf_idf_sig);
				}
				if (tf_idf_sig > shp.maxDocSig) {
					shp.maxDocSig = tf_idf_sig;
				}
				
				entBucket.doc.aggSignificance += tf_idf_sig;
				
				// Now we're done incorporating the significance into the document, we're going
				// to adjust the standalone significance for the relevance of the document
				// (if enabled - either manually or if the query contains OR statements)
				if ((null != scoreParams.adjustAggregateSig) && scoreParams.adjustAggregateSig) {
					tf_idf_sig *= entBucket.doc.luceneScore*_s0_avgLuceneScoreInv;
				}
				//TESTED (doc scores stay the same, entity scores adjust)
				
				shp.datasetSignificance += tf_idf_sig/(double)shp.nDocCountInQuerySubset;
				
				// Stage 3b alias processing: update dataset significance
				if (null != shp.masterAliasSH) {
					
					double alias_tf_idf_sig = tf_term*shp.masterAliasSH.standaloneSignificance*entBucket.doc.manualWeighting;												
						// (standaloneSig's calculation was finished at the start of this loop)
					
					// (adjust for relevance as above)
					if ((null != scoreParams.adjustAggregateSig) && scoreParams.adjustAggregateSig) {
						alias_tf_idf_sig *= entBucket.doc.luceneScore*_s0_avgLuceneScoreInv;
					}		
					//TESTED
					
					if (alias_tf_idf_sig > shp.masterAliasSH.maxDocSig) {
						shp.masterAliasSH.maxDocSig = alias_tf_idf_sig;
					}					
					shp.masterAliasSH.datasetSignificance += alias_tf_idf_sig/(double)shp.masterAliasSH.nDocCountInQuerySubset;
						// (don't use the nEntsInContainingDocs because here we do care about the overlap)
					
				}//TESTED
				// end Stage 3b alias processing
				
				entBucket.doc.nLeftToProcess--;
				if (0 == entBucket.doc.nLeftToProcess) {
					
					// Final calculation for Infinite significance
					entBucket.doc.aggSignificance *= entBucket.doc.geoTemporalDecay*_s3_dSigScalingFactor;
					
					entBucket.doc.luceneScore *= _s3_dLuceneScalingFactor; // (lucene already geo-temporally) scaled
						// (don't up lucene score this is done inside Lucene)
					
					double d = _s3_dScoreScalingFactor*(entBucket.doc.luceneScore + entBucket.doc.aggSignificance);
					if (Double.isNaN(d)) {
						d = 0.0;
					}
					if (_s0_sortingByDate) {
						entBucket.doc.totalScore  = (double)-entBucket.doc.nLuceneIndex;
					}
					else {
						entBucket.doc.totalScore = d;
					}
					entBucket.doc.nTieBreaker = n1Down--;

					// Completed calculating this feed's score					
					// Insert into "top 100" list:
					
					if (_s3_pqDocs.size() < nToClientLimit) {
						
						//DEBUG
						//System.out.println(_s3_pqDocs.size() + ", ADD URL=" + entBucket.doc.dbo.getString(DocumentPojo.url_));
						
						_s3_pqDocs.add(entBucket.doc);						
						entBucket.doc.bPromoted = true;
					}
					else if ((_s3_pqDocs.size() >= nToClientLimit) && (nToClientLimit > 0)) {
						TempDocBucket qsf = _s3_pqDocs.first();
						if (entBucket.doc.totalScore > qsf.totalScore) {
							qsf.bPromoted = false;
							entBucket.doc.bPromoted = true;
							_s3_pqDocs.add(entBucket.doc);							
							if (_s3_pqDocs.size() > nToClientLimit) { // (size might stay the same if this is a duplicate)								
								
								Iterator<TempDocBucket> it = _s3_pqDocs.iterator(); // (now can remove this the object via...)
								TempDocBucket tdb = it.next(); 
								it.remove(); // (ie remove the first object)

								// Phase "1": middle ranking (used to be good, not so much any more)
								if (null != _s0_standaloneEventAggregator) {
									ScoringUtils_Associations.addStandaloneEvents(tdb.dbo, tdb.aggSignificance, 1, _s0_standaloneEventAggregator, 
																					_s0_bEntityTypeFilterPositive, _s0_bAssocVerbFilterPositive, _s0_entityTypeFilter, 
																						_s0_assocVerbFilter, _s0_bEvents, _s0_bSummaries, _s0_bFacts);
								}//TESTED											
								if (null != _s0_lowAccuracyAssociationAggregator_events) {
									ScoringUtils_Associations.addStandaloneEvents(tdb.dbo, tdb.aggSignificance, 1, _s0_lowAccuracyAssociationAggregator_events, 
																					_s0_bEntityTypeFilterPositive, _s0_bAssocVerbFilterPositive, _s0_entityTypeFilter, 
																						_s0_assocVerbFilter, true, false, false);
								}//TESTED								
								if (null != _s0_lowAccuracyAssociationAggregator_facts) {
									ScoringUtils_Associations.addStandaloneEvents(tdb.dbo, tdb.aggSignificance, 1, _s0_lowAccuracyAssociationAggregator_facts, 
																					_s0_bEntityTypeFilterPositive, _s0_bAssocVerbFilterPositive, _s0_entityTypeFilter, 
																						_s0_assocVerbFilter, false, false, true);
								}//TESTED
								
							}//TESTED

						}
						else { // Not promoting
							shp.unusedDbo = entBucket.dbo; // (might save me the trouble of cloning a few times...)													

							// Phase "2": never any good!
							if (null != _s0_standaloneEventAggregator) {
								ScoringUtils_Associations.addStandaloneEvents(entBucket.doc.dbo, entBucket.doc.aggSignificance, 2, _s0_standaloneEventAggregator, 
																				_s0_bEntityTypeFilterPositive, _s0_bAssocVerbFilterPositive, _s0_entityTypeFilter, _s0_assocVerbFilter, 
																					_s0_bEvents, _s0_bSummaries, _s0_bFacts);
							}//TESTED
							if (null != _s0_lowAccuracyAssociationAggregator_events) {
								ScoringUtils_Associations.addStandaloneEvents(entBucket.doc.dbo, entBucket.doc.aggSignificance, 2, _s0_lowAccuracyAssociationAggregator_events, 
																				_s0_bEntityTypeFilterPositive, _s0_bAssocVerbFilterPositive, _s0_entityTypeFilter, 
																					_s0_assocVerbFilter, true, false, false);
							}//TESTED								
							if (null != _s0_lowAccuracyAssociationAggregator_facts) {
								ScoringUtils_Associations.addStandaloneEvents(entBucket.doc.dbo, entBucket.doc.aggSignificance, 2, _s0_lowAccuracyAssociationAggregator_facts, 
																				_s0_bEntityTypeFilterPositive, _s0_bAssocVerbFilterPositive, _s0_entityTypeFilter, 
																					_s0_assocVerbFilter, false, false, true);
							}//TESTED
						}
					}
					else { // Not promoting any documents...
						shp.unusedDbo = entBucket.dbo; // (might save me the trouble of cloning a few times...)
						
						// Phase "2": never any good!
						if (null != _s0_standaloneEventAggregator) {
							ScoringUtils_Associations.addStandaloneEvents(entBucket.doc.dbo, entBucket.doc.aggSignificance, 2, _s0_standaloneEventAggregator, 
																			_s0_bEntityTypeFilterPositive, _s0_bAssocVerbFilterPositive, _s0_entityTypeFilter, _s0_assocVerbFilter, 
																				_s0_bEvents, _s0_bSummaries, _s0_bFacts);
						}//TESTED
						if (null != _s0_lowAccuracyAssociationAggregator_events) {
							ScoringUtils_Associations.addStandaloneEvents(entBucket.doc.dbo, entBucket.doc.aggSignificance, 2, _s0_lowAccuracyAssociationAggregator_events, 
																			_s0_bEntityTypeFilterPositive, _s0_bAssocVerbFilterPositive, _s0_entityTypeFilter, 
																				_s0_assocVerbFilter, true, false, false);
						}//TESTED								
						if (null != _s0_lowAccuracyAssociationAggregator_facts) {
							ScoringUtils_Associations.addStandaloneEvents(entBucket.doc.dbo, entBucket.doc.aggSignificance, 2, _s0_lowAccuracyAssociationAggregator_facts, 
																			_s0_bEntityTypeFilterPositive, _s0_bAssocVerbFilterPositive, _s0_entityTypeFilter, 
																				_s0_assocVerbFilter, false, false, true);
						}//TESTED
					}
				}//TESTED
			
			} // (end loop over entity occurrences in feeds) 			
			//TESTED
			
			// Handle geo:
			if (null != shp.geotag) {
				loadLowAccuracyGeoBuckets(shp);
			}
			
			// Insert entities into the output priority queue
			// NOTE LOCAL SHP CANNOT BE USED AFTER THE FOLLOWING CLAUSE
			// (LOCAL==the object itself isn't changed, so the code above is fine, but the pointer is modified)
			
			if (_s0_nNumEntsReturn > 0) {
				
				// Stage 3c alias processing:
				if ((null != shp.masterAliasSH) && (shp.masterAliasSH.masterAliasSH != shp)) {					
					continue; // (only promote the last of the aliased entities)
				}//TESTED
				else if (null != shp.masterAliasSH) { // (use aggregated aliased version if present)
					
					shp.masterAliasSH.unusedDbo = shp.unusedDbo;
						// (overwriting this, which is fine since it's not used after stage 1)
					shp.masterAliasSH.index = shp.index; // (just so I know what the index of this entity is) 
					// (overwriting this, which is fine since it's not used after the first ent of the alias group in this stage)
					
					shp.masterAliasSH.entityInstances = shp.entityInstances;
						// (the only 2 fields that are needed but weren't present)
					shp = shp.masterAliasSH;					
				}//TESTED
				// end stage 3c of alias processing
				
				if (_s3_pqEnt.size() < _s0_nNumEntsReturn) {						
					_s3_pqEnt.add(shp);						
				}
				if ((_s3_pqEnt.size() >= _s0_nNumEntsReturn) && (_s0_nNumEntsReturn > 0)) {
					
					EntSigHolder qsf = _s3_pqEnt.element();
					if (shp.datasetSignificance > qsf.datasetSignificance) {
						_s3_pqEnt.remove();
						_s3_pqEnt.add(shp);
					}
				}				
			}//TESTED
			
			// (NOTE LOCAL SHP CANNOT BE USED FROM HERE - IE NO MORE CODE IN THIS LOOP!)	
			
		} // (end loop over entities)

		// Handle docus with no entities:
		
		if (nToClientLimit > 0) {
			for (TempDocBucket doc: _s1_noEntityBuckets) {
				doc.luceneScore *= _s3_dLuceneScalingFactor;
				double d = _s3_dScoreScalingFactor*doc.luceneScore;
				if (Double.isNaN(d)) {
					d = 0.0;
				}
				if (_s0_sortingByDate) {
					doc.totalScore = (double)-doc.nLuceneIndex;
				}
				else {
					doc.totalScore = d;					
				}
				doc.nTieBreaker = n1Down--;
				if (_s3_pqDocs.size() < nToClientLimit) {						
					_s3_pqDocs.add(doc);						
				}
				if (_s3_pqDocs.size() >= nToClientLimit) {
					TempDocBucket qsf = _s3_pqDocs.first();
					if (doc.totalScore > qsf.totalScore) {
						_s3_pqDocs.add(doc);
						if (_s3_pqDocs.size() > nToClientLimit) { // (could be a duplicate)
							Iterator<TempDocBucket> it = _s3_pqDocs.iterator(); // (now can remove this the object via...)
							it.next(); it.remove(); // (ie remove the first object)
						}
					}//(TESTED)
				}
			} // (end loop over feeds with no entities)
		} // (obv don't bother if we're not returning documents anyway...)		
	}
	
/////////////////////////////////////////////////////////////	
	
// 4a] stage4_prepareDocsForOutput()
	// Using the priority queues calculated in step [3] generate the lists of documents and entities to return 
	
	private void stage4_prepareDocsForOutput(AdvancedQueryPojo.QueryScorePojo scoreParams, 
										StatisticsPojo scores,
										long nToClientLimit, 
										LinkedList<BasicDBObject> returnList)
	{
		// Get the documents
		long nDocs = 0;
		double dBestScore = 0.0;
		double dAvgScore = 0.0;
		
		double dSigFactor = 100.0/(_s3_dSigScalingFactor*_s2_dApproxAverageDocumentSig);
		double dRelFactor = 100.0/(_s3_dLuceneScalingFactor*_s0_avgLuceneScore);
		
		// Start at the bottom of the list, so don't need to worry about skipping documents, just count out from the bottom
		// The call to stage3_calculateTFTerms with nStart+nToClientLimit handles the rest
		
		Iterator<TempDocBucket> pqIt = _s3_pqDocs.iterator();
		while (pqIt.hasNext() && (nDocs < nToClientLimit))
		{
			TempDocBucket qsf = pqIt.next();
			nDocs++;
			if (!_s0_sortingByDate) {
				dBestScore = qsf.totalScore;
			}
			dAvgScore += dBestScore;
			
			BasicDBObject f = qsf.dbo;
			
			// Phase "0" - these are the highest prio events
			boolean bNeedToFilterAndAliasAssoc_event = true;
			boolean bNeedToFilterAndAliasAssoc_fact = true;
			boolean bNeedToFilterAndAliasAssoc_summary = true;
			if (null != _s0_standaloneEventAggregator) {
				ScoringUtils_Associations.addStandaloneEvents(qsf.dbo, qsf.aggSignificance, 0, _s0_standaloneEventAggregator, 
																_s0_bEntityTypeFilterPositive, _s0_bAssocVerbFilterPositive, _s0_entityTypeFilter, _s0_assocVerbFilter, 
																	_s0_bEvents, _s0_bSummaries, _s0_bFacts);
				bNeedToFilterAndAliasAssoc_event = false;
				bNeedToFilterAndAliasAssoc_fact = false;
				bNeedToFilterAndAliasAssoc_summary = false;
			}//TESTED
			if (null != _s0_lowAccuracyAssociationAggregator_events) {
				ScoringUtils_Associations.addStandaloneEvents(qsf.dbo, qsf.aggSignificance, 0, _s0_lowAccuracyAssociationAggregator_events, 
																_s0_bEntityTypeFilterPositive, _s0_bAssocVerbFilterPositive, _s0_entityTypeFilter, 
																	_s0_assocVerbFilter, true, false, false);
				bNeedToFilterAndAliasAssoc_event = false;
			}//TESTED								
			if (null != _s0_lowAccuracyAssociationAggregator_facts) {
				ScoringUtils_Associations.addStandaloneEvents(qsf.dbo, qsf.aggSignificance, 0, _s0_lowAccuracyAssociationAggregator_facts, 
																_s0_bEntityTypeFilterPositive, _s0_bAssocVerbFilterPositive, _s0_entityTypeFilter, 
																	_s0_assocVerbFilter, false, false, true);
				bNeedToFilterAndAliasAssoc_fact = false;
			}//TESTED
			
			try {
				DocumentPojoApiMap.mapToApi(f);
				// Handle deduplication/multi-community code:
				if (null != qsf.dupList) {
					try {
						ScoringUtils_MultiCommunity.community_combineDuplicateDocs(qsf);
					}
					catch (Exception e) {
						// Do nothing, just carry on with minimal damage!
					}
				}

				// Scoring:
				double d = qsf.aggSignificance*dSigFactor;
				if (Double.isNaN(d)) {
					f.put(DocumentPojo.aggregateSignif_, 0.0);				
				}
				else {
					f.put(DocumentPojo.aggregateSignif_, d);								
				}				
				d = qsf.luceneScore*dRelFactor;
				if (Double.isNaN(d)) {
					f.put(DocumentPojo.queryRelevance_, 0.0);				
				}
				else {
					f.put(DocumentPojo.queryRelevance_, d);								
				}
				if (!_s0_sortingByDate) {
					f.put(DocumentPojo.score_, qsf.totalScore);
				}
	
				BasicDBList l = (BasicDBList)(f.get(DocumentPojo.entities_));

				// Handle update ids vs normal ids:
				ObjectId updateId = (ObjectId) f.get(DocumentPojo.updateId_);
				if (null != updateId) { // swap the 2...
					f.put(DocumentPojo.updateId_, f.get(DocumentPojo._id_));
					f.put(DocumentPojo._id_, updateId);
				}
				
				// Check if entities enabled				
				if ((null != l) && (!_s0_bGeoEnts && !_s0_bNonGeoEnts)) {
					f.removeField(DocumentPojo.entities_);
					l = null;
				}//TESTED
				
				// Check if events etc enabled
				if ((!_s0_bEvents && !_s0_bFacts && !_s0_bSummaries)) {
					f.removeField(DocumentPojo.associations_);										
				}//TESTED				
				else if (!_s0_bEvents || !_s0_bFacts || !_s0_bSummaries || (null != _s0_assocVerbFilter)) {					
					
					// Keep only specified event_types
					BasicDBList lev = (BasicDBList)(f.get(DocumentPojo.associations_));
					if (null != lev) {
						for(Iterator<?> e0 = lev.iterator(); e0.hasNext();){
							BasicDBObject e = (BasicDBObject)e0.next();
							
							// Type filter
							boolean bNeedToFilterAndAliasAssoc = true;
							String sEvType = e.getString(AssociationPojo.assoc_type_);
							boolean bKeep = true;
							if (null == sEvType) {
								bKeep = false;
							}
							else if (sEvType.equalsIgnoreCase("event")) {
								if (!_s0_bEvents) bKeep = false;
								bNeedToFilterAndAliasAssoc = bNeedToFilterAndAliasAssoc_event;
							}
							else if (sEvType.equalsIgnoreCase("fact")) {
								if (!_s0_bFacts) bKeep = false;
								bNeedToFilterAndAliasAssoc = bNeedToFilterAndAliasAssoc_fact;
							}
							else if (sEvType.equalsIgnoreCase("summary")) {
								if (!_s0_bSummaries) bKeep = false;
								bNeedToFilterAndAliasAssoc = bNeedToFilterAndAliasAssoc_summary;
							}
							if (!bKeep) {
								e0.remove();
							}
							else { // Type matches, now for some more complex logic....
								
								if (bNeedToFilterAndAliasAssoc) { // (otherwise done already)

									bKeep = ScoringUtils_Associations.filterAndAliasAssociation(
																			e, _s1_aliasLookup, true,
																			_s0_bEntityTypeFilterPositive, _s0_bAssocVerbFilterPositive,
																			_s0_entityTypeFilter, _s0_assocVerbFilter);
									if (!bKeep) {
										e0.remove();
									}
									
								}//TESTED
								
							}//(end output filter logic)

						} // (end loop over events)	
					} // (end if this doc has events)
					
				} //TESTED				
				
				// Check if metadata is enabled
				if (!_s0_bMetadata) {
					f.removeField(DocumentPojo.metadata_);
				} //TESTED
				
				if (null != l) {
					
					for(Iterator<?> e0 = l.iterator(); e0.hasNext();){
						BasicDBObject e = (BasicDBObject)e0.next();
						
						if (!_s0_bNonGeoEnts) { // then must only be getting geo (else wouldn't be in this loop)
							if (null == e.get(EntityPojo.geotag_)) {
								e0.remove();
								continue;
							}
						}
						
						String entity_index = e.getString(EntityPojo.index_);
						if (null == entity_index) continue;
	
						EntSigHolder shp = (EntSigHolder)_s1_entitiesInDataset.get(entity_index);
						
						if (null != shp) {
							// Stage 4x: alias processing, just overwrite 
							// (note don't delete "duplicate entities", hard-to-be-globally-consistent
							//  and will potentially throw data away which might be undesirable)
							if (null != shp.masterAliasSH) {
								shp = shp.masterAliasSH; // (already has all the aggregated values used below)
								if (!entity_index.equals(shp.aliasInfo.getIndex())) {
									e.put(EntityPojo.index_, shp.aliasInfo.getIndex());
									e.put(EntityPojo.disambiguated_name_, shp.aliasInfo.getDisambiguatedName());
									e.put(EntityPojo.type_, shp.aliasInfo.getType());
									e.put(EntityPojo.dimension_, shp.aliasInfo.getDimension());			
									
									if (null != shp.aliasInfo.getGeotag()) {
										BasicDBObject aliasedGeoTag = new BasicDBObject();
										aliasedGeoTag.put(GeoPojo.lat_, shp.aliasInfo.getGeotag().lat);
										aliasedGeoTag.put(GeoPojo.lon_, shp.aliasInfo.getGeotag().lon);
										e.put(EntityPojo.geotag_, aliasedGeoTag);
										if (null != shp.aliasInfo.getOntology_type()) {
											e.put(EntityPojo.ontology_type_, shp.aliasInfo.getOntology_type());
										}
									}//TESTED
								}
							}//TESTED
							// end Stage 4x of alias processing						
						
							double dataSig = shp.datasetSignificance;
							if (Double.isNaN(dataSig)) {
								e.put(EntityPojo.datasetSignificance_, 0.0);								
							}
							else {
								e.put(EntityPojo.datasetSignificance_, dataSig);
							}
							e.put(EntityPojo.queryCoverage_, shp.queryCoverage);
							e.put(EntityPojo.averageFreq_, shp.avgFreqOverQuerySubset);
							if (shp.nTotalSentimentValues > 0) {
								e.put(EntityPojo.positiveSentiment_, shp.positiveSentiment);
								e.put(EntityPojo.negativeSentiment_, shp.negativeSentiment);
								e.put(EntityPojo.sentimentCount_, shp.nTotalSentimentValues);
							}
						}
						else { // (most likely to occur if the entity is discarded (alias/filter) or is corrupt in some way)
							e0.remove();
							continue;
						}
		
					} //(end loop over entities)
				} // (end if feed has entities)
				//TESTED
					
				// Explain if enabled
				if (null != qsf.explain) {
					f.put(DocumentPojo.explain_, qsf.explain);
				}
				
				// Add to the end of the list (so will come back from API call in natural order, highest first)
				returnList.addFirst(f);
					// (add elements to the front of the list so that the top of the list is ordered by priority)
			}
			catch(Exception e){
				// Probably a JSON error, just carry on
				String title = f.getString(DocumentPojo.title_);
				logger.error(title + ": " + e.getMessage());
			}
	
		} // (end loop over feeds)
		//TESTED

		// Update the scores:
		scores.maxScore = (float) dBestScore;
		if (nDocs > 0) {
			scores.avgScore = (float)dAvgScore/nDocs;
		}
	}

/////////////////////////////////////////////////	
	
// 4b] stage4_prepareEntsForOutput()
	// Using the priority queues calculated in step [3] generate the lists of documents and entities to return 
	
	private void stage4_prepareEntsForOutput(LinkedList<BasicDBObject> entityReturn)
	{
		if (_s0_nNumEntsReturn > 0) { // (else entities not enabled)
			
			for (EntSigHolder qsf = _s3_pqEnt.poll(); null != qsf; qsf = _s3_pqEnt.poll()) // (start with lowest ranking)
			{				
				BasicDBObject ent = qsf.unusedDbo;
				if (null == ent) {
					int nTries = 0;
					for (TempEntityInDocBucket tefb: qsf.entityInstances) {
						// (Try to find an entity that wasn't promoted ie can now be re-used
						//  if we can't find one quite quickly then bail out and we'll pay the cost of cloning it)
						if (!tefb.doc.bPromoted) {
							ent = tefb.dbo;
							break;
						}
						else if (++nTries > 10) {
							break;
						}
					}
					if (null == ent) {
						ent = qsf.entityInstances.get(0).dbo;
					}
				}//TESTED
				try {

					if (null != qsf.aliasInfo) {
						if (!qsf.index.equals(qsf.aliasInfo.getIndex())) {
							ent.put(EntityPojo.index_, qsf.aliasInfo.getIndex());
							ent.put(EntityPojo.disambiguated_name_, qsf.aliasInfo.getDisambiguatedName());
							ent.put(EntityPojo.type_, qsf.aliasInfo.getType());
							ent.put(EntityPojo.dimension_, qsf.aliasInfo.getDimension());			
							if (null != qsf.aliasInfo.getGeotag()) {
								BasicDBObject aliasedGeoTag = new BasicDBObject();
								aliasedGeoTag.put(GeoPojo.lat_, qsf.aliasInfo.getGeotag().lat);
								aliasedGeoTag.put(GeoPojo.lon_, qsf.aliasInfo.getGeotag().lon);
								ent.put(EntityPojo.geotag_, aliasedGeoTag);
								if (null != qsf.aliasInfo.getOntology_type()) {
									ent.put(EntityPojo.ontology_type_, qsf.aliasInfo.getOntology_type());
								}
							}//TESTED
						}
					}//TESTED
					
					if (null == ent.get(EntityPojo.datasetSignificance_)) { // Not getting promoted so need to add fields...						
						if (Double.isNaN(qsf.datasetSignificance)) {
							ent.put("datasetSignificance", 0.0);								
						}
						else {
							ent.put(EntityPojo.datasetSignificance_, qsf.datasetSignificance);
						}
						ent.put(EntityPojo.queryCoverage_, qsf.queryCoverage);
						ent.put(EntityPojo.averageFreq_, qsf.avgFreqOverQuerySubset);
						if (qsf.nTotalSentimentValues > 0) {
							ent.put(EntityPojo.positiveSentiment_, qsf.positiveSentiment);
							ent.put(EntityPojo.negativeSentiment_, qsf.negativeSentiment);
							ent.put(EntityPojo.sentimentCount_, qsf.nTotalSentimentValues);
						}
					}					
					else { // (... but can just use it without cloning)
						BasicDBObject ent2 = new BasicDBObject(); 
						for (Map.Entry<String, Object> kv: ent.entrySet()) {
							ent2.append(kv.getKey(), kv.getValue());
						}
						ent = ent2;
					}
					ent.removeField(EntityPojo.relevance_);
					if (Double.isNaN(qsf.maxDocSig)) {
						ent.put(EntityPojo.significance_, 0.0);
					}
					else {
						ent.put(EntityPojo.significance_, qsf.maxDocSig);
					}
					ent.put(EntityPojo.frequency_, (long)qsf.maxFreq);
					entityReturn.addFirst(ent);
				}
				catch(Exception e){
					// Probably a JSON error, just carry on
					String title = ent.getString(EntityPojo.index_);
					logger.error(title + ": " + e.getMessage());
				} //TESTED
			}
		}//TESTED				
	}
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	// Utility
	
	private BasicDBObject _s0_docCountFields = null; 
	private BasicDBObject _s0_docCountHint = null; 
	
	private long getDocCount(ObjectId[] communityIds) {
		long nDocCount = 0;
		try {
			BasicDBObject query = new BasicDBObject(DocCountPojo._id_, new BasicDBObject(MongoDbManager.in_, communityIds));
			if (null == _s0_docCountFields) {
				_s0_docCountFields = new BasicDBObject(DocCountPojo._id_, 0); 			
				_s0_docCountFields.put(DocCountPojo.doccount_, 1);
				_s0_docCountHint = new BasicDBObject(DocCountPojo._id_, 1); 			
				_s0_docCountHint.put(DocCountPojo.doccount_, 1);
			}
			DBCursor dbc = DbManager.getDocument().getCounts().find(query, _s0_docCountFields).hint(_s0_docCountHint);
			while (dbc.hasNext()) {
				BasicDBObject dbo = (BasicDBObject) dbc.next();
				Iterator<?> it = dbo.values().iterator();
				if (it.hasNext()) {
					nDocCount += (double)((Long)it.next()).longValue(); // (from _s0_docCountFields, doccount is only return variable)
				}
			}
			if (0 == nDocCount) { // (Probably shouldn't happen if a harvest has occurred, just don't bomb out
				nDocCount = _s0_nQuerySetDocCount;
			}
		}
		catch (Exception e) { // Doc count might not be setup correctly?
			nDocCount = _s0_nQuerySetDocCount;			
		}
		return nDocCount;
	}//TESTED

	// The overall plan is:
	// S1: identify alias (write helper function based on the code above), calculate overlapping doc count
	// S2: calc pythag significance, store first/last values ready for S3
	// S3: first time through, do sqrt bit of pythag, last time through add to PQ
	// S4: overwrite the entity values with aliased entities where necessary
	
	private void stage1_initAlias(EntSigHolder shp) {
		EntityFeaturePojo alias = _s1_aliasLookup.getAliasMaster(shp.index);
		if (null != alias) { // overwrite index
			if (alias.getIndex().equalsIgnoreCase("discard")) {
				shp.aliasInfo = alias;
				shp.masterAliasSH = null;
				return;
			}			
			EntSigHolder masterAliasSH = null;
			if (null == _s1_aliasSummary) {
				_s1_aliasSummary = new HashMap<String, EntSigHolder>();
			}
			else {
				masterAliasSH = _s1_aliasSummary.get(alias.getIndex());
			}
			if (null == masterAliasSH) {
				masterAliasSH = new EntSigHolder(null, 0, null); //(use ESH as handy collection of req'd vars)
				_s1_aliasSummary.put(alias.getIndex(), masterAliasSH);							
			}			
			shp.masterAliasSH = masterAliasSH;
			shp.aliasInfo = alias;
			shp.masterAliasSH.aliasInfo = alias; // (no harm storing this in 2 places)
		}
	}//TESTED

	private double getManualScoreWeights(AdvancedQueryPojo.QueryScorePojo scoreParams, BasicDBObject doc)
	{
		// Highest prio: source key weight
		if (null != scoreParams.sourceWeights) {			
			String sourceKey = doc.getString(DocumentPojo.sourceKey_);
			Double dWeight = scoreParams.sourceWeights.get(sourceKey);
			
			if (null != dWeight) {
				return dWeight;
			}
		}
		// Middle prio: type
		if (null != scoreParams.typeWeights) {
			String mediaType = doc.getString(DocumentPojo.mediaType_);
			Double dWeight = scoreParams.typeWeights.get(mediaType);
			
			if (null != dWeight) {
				return dWeight;
			}			
		}
		// Lowest prio: average of tags
		if (null != scoreParams.tagWeights) {
			double dScore = 0.0;
			int nComps = 0;
			BasicDBList tags = (BasicDBList) doc.get(DocumentPojo.tags_);
			if (null != tags) {
				for (Object tagObj: tags) {
					String tag = (String)tagObj;
					Double dWeight = scoreParams.tagWeights.get(tag);
					if (null != dWeight) {
						nComps++;
						dScore += dWeight;
					}
				}
				if (nComps > 0) {
					return dScore/nComps;
				}
			}
		}
		return 1.0;
	}//TESTED (all 3 cases)	

	////////////////////////////////////////////////////////////////////////////
	
	// Low accuracy geo aggregation utils:
	
	// Code copied from ScoringUtils_Association
	
	private void loadLowAccuracyGeoBuckets(EntSigHolder shp) {
		double dBucket = shp.queryCoverage/(this._s2_maxGeoQueryCoverage + 0.01); // (ensure <1)
		if (dBucket > 1.0) dBucket = 1.0;
		
		int nBucket = _s3_nGEO_BUCKETS_1 - ((int)(_s3_dGEO_BUCKETS*dBucket) % _s3_nGEO_BUCKETS); 
		
		LinkedList<EntSigHolder> bucketList = _s3_geoBuckets[nBucket];
		if (null == bucketList) {
			bucketList = new LinkedList<EntSigHolder>();
			_s3_geoBuckets[nBucket] = bucketList;
		}
		bucketList.add(shp);		
	}//TESTED
	
	private void finalizeLowAccuracyGeoAggregation(AggregationUtils.GeoContainer geoContainer, long nMaxToReturn) {
		
		geoContainer.geotags = new TreeSet<GeoAggregationPojo>();
		
		for (LinkedList<EntSigHolder> bucket: _s3_geoBuckets) {
			
			if (null != bucket) {
				for (EntSigHolder shp: bucket) {
					// Estimated count:
					
					try {
						if (null != shp.geotag) { // will always be the case...
							GeoAggregationPojo geo = new GeoAggregationPojo();
							
							geo.lat = shp.geotag.getDouble(GeoPojo.lat_);
							geo.lon = shp.geotag.getDouble(GeoPojo.lon_);
							geo.type = shp.geotaggedEntity.getString(EntityPojo.ontology_type_); 
							if (null == geo.type) {
								geo.type = "point";
							}
							geo.count = (int)(0.01*shp.queryCoverage*_s0_nQuerySetDocCount);
								// (query coverage is a %)
							
							geoContainer.geotags.add(geo);
								// (can change geo.count, where aggregation has happened)
							
							if (geo.count > geoContainer.maxCount) {
								geoContainer.maxCount = geo.count;
							}
							if (geo.count < geoContainer.minCount) {
								geoContainer.minCount = geo.count;								
							}							
							if (geoContainer.geotags.size() >= nMaxToReturn) {
								return;
							}
						}
					}
					catch (Exception e) {} // geotag invalid just carry on
				}
			}
		}		
	}//TESTED
	
}
