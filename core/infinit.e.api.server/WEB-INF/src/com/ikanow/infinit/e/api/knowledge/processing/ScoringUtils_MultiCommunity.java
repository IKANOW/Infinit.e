package com.ikanow.infinit.e.api.knowledge.processing;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.bson.types.ObjectId;

import com.ikanow.infinit.e.api.knowledge.processing.ScoringUtils.EntSigHolder;
import com.ikanow.infinit.e.api.knowledge.processing.ScoringUtils.TempDocBucket;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.infinit.e.data_model.store.document.EntityPojo;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;

class ScoringUtils_MultiCommunity {

	/////////////////////////////////////////////////////////////////////////////////////////////////////////////	
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////	

	//APPROXIMATE PAN-COMMUNITY SIGNIFICANCE CALCULATIONS

	// Classes:

	// Holds the list of all possible communities that could be returned (and whether they've been seen before)
	static class Community_GlobalInfo {
		Community_GlobalInfo(int nIndex_) { nIndex = nIndex_; bInDataset = false; nEntityCount = 0; }
		public int nIndex;
		public boolean bInDataset;		
		public long nEntityCount;
	}
	// Holds the relative doc counts between community pairs, used to estimate doc counts from unobserved communities
	// (for a specific entity, although obviously these stats are across all entities)
	static class Community_GlobalCorrelation {
		Community_GlobalCorrelation(int nId1, int nId2) {
			community1_id = nId1; community2_id = nId2;
		}
		private int community1_id;
		private int community2_id;
		private long community1_docCount = 0;
		private long community2_docCount = 0;
		private long overlapping_entities = 0;
		public int getCommunity1_id() { return community1_id; }
		public int getCommunity2_id() { return community2_id; }
		public long getCommunity1_docCount() { return community1_docCount; }
		public long getCommunity2_docCount() { return community2_docCount; }
		public long getOverlapping_entities() { return overlapping_entities; }
		public void updateCorrelation(int nCommIdA, int nCommIdB, long nDocCountA, long nDocCountB) {
			if ((0 != nDocCountA) && (0 != nDocCountB)) { // (new entity)
				overlapping_entities++;
			}
			if (community1_id == nCommIdA) {
				community1_docCount += nDocCountA;
				community2_docCount += nDocCountB;
			}
			else {
				community1_docCount += nDocCountB;
				community2_docCount +=nDocCountA;				
			}			
		}
	}
	// Just holds the matching vs total counts for each community per entity
	static class Community_LocalInfo {
		public long datasetCount;
		public long communityCount;
	}
	// Holds per entity extensions for multi community scoring
	static class Community_EntityExtensions {
		public int singleId; // (only kick complex code in if needed - intialized when an instance of this class is created)
		public Community_LocalInfo multiInfo[] = null; // (only used if needed)
		public Map<Integer, Community_LocalInfo> multiInfoMap = null; // (only used if *really* needed)
		public int observedCount = 1; // (ie must be 1 if this even exists)		
	}

	///////////////////////////////////////////////

	// State:

	private ObjectId[] _community_ids = null; // (taken from calling function)
	private Map<ObjectId, Community_GlobalInfo> _community_globalInfo = null; //new TreeMap<String, GlobalCommunityInfo>();
		// (created on demand)
	private Map<Integer, Community_GlobalInfo> _community_globalInfo_observed = null; //new TreeMap<String, GlobalCommunityInfo>();
		// (created on the fly)
	private Community_GlobalInfo[] _community_globalInfoTable = null; // (same as above, but with faster lookup given id)
	private int _community_nObserved = 0; 
	private Community_GlobalCorrelation _community_correlationTable[] = null; //= new GlobalCommunityCorrelation[(n*(n-1))/2];
	private ObjectId _community_cachedValue = null; // (last community observed from document)
	private int _community_nCachedId = -1;
	private int _community_nPreMultiId = -1; // (_community_nCachedId is -1 until we setup the metadata...
		// ...which we defer as long as possible in order to not to do it if we don't have to,
		// as a result, a bunch of EntSigHolder objects will have an incorrect community_singleId==-1)

	private static final int _community_nUpperLimitForArrays = 25;
	private static final int _community_nLowerLimitForMaps = 10;
	
	///////////////////////////////////////////////
	
	// External Utility
	
	ScoringUtils_MultiCommunity(String[] communityIds) {
		_community_ids = new ObjectId[communityIds.length];
		for (int i = 0; i < communityIds.length; ++i) {
			_community_ids[i] = new ObjectId(communityIds[i]); 
		}
	}		

	///////////////////////////////////////////////
	
	// Get converted community Ids
	
	ObjectId[] getCommunityIds() {
		return _community_ids;
	}
	///////////////////////////////////////////////

	// External Utility
	
	// Tells the delegating class whether the multi community code has been activated yet
	
	boolean isActive() {
		return (-1 != _community_nCachedId);
	}
	
	///////////////////////////////////////////////
	
	// External Utility
	
	// Initalize a new entity
	
	void initializeEntity(EntSigHolder ent) {
		ent.community = new Community_EntityExtensions();
		ent.community.singleId = _community_nCachedId;
		if (-1 != _community_nCachedId) {
			_community_globalInfoTable[ent.community.singleId].nEntityCount++;
		}
	}
	
	///////////////////////////////////////////////

	// Internal Utility

	final private int community_getCorrelationIndex(int nId1, int nId2) {
		// each value of nId1 "uses up" (N-nId1) elements
		// so the start of the nId2s for a given nId1 (nId2>nId1) is:
		// sigma(s=1,s<=nId1)[N - s] = N*nId1 - (nId1*(nId1+1))/2
		// and then to get the (nId1,nId2) position, you add (nId2-nId1-1)
		// and the equation below drops out:		
		if (nId1 < nId2) {
			return (nId1*(2*_community_ids.length - nId1 - 3))/2 + (nId2 - 1);
		}
		else {
			return (nId2*(2*_community_ids.length - nId2 - 3))/2 + (nId1 - 1);
		}
	}//TESTED

	///////////////////////////////////////////////

	// Top level check to determine the community Id and do some initialization where needed 

	int community_getIdAndInitialize(ObjectId communityId, Map<String, EntSigHolder> entitiesInDataset) {
		if (0 == _community_nObserved) { // First time through, still don't know if we need any of this horrible logic
			_community_nObserved = 1;
			_community_cachedValue = communityId;
				// (leave _community_nCachedId as -1 until we know we're going to need it... then we'll use
				//  _community_nCachedIdMap to update all the old local values as needed)
		}//TESTED
		else if (!_community_cachedValue.equals(communityId)) {
			// Not the same community as last time, is it a new community?

			Community_GlobalInfo global = null;
			if (null == this._community_globalInfo) { // Setup metadata if not already done so
				community_setupMetadata();
				global = _community_globalInfo.get(_community_cachedValue);				
				
				global.bInDataset = true;
				global.nEntityCount = entitiesInDataset.size();
					// (Update observed entities for original community) 
				
				_community_nPreMultiId = global.nIndex;
				_community_globalInfo_observed.put(global.nIndex, global); 
			}//TESTED
			global = _community_globalInfo.get(communityId);
				// this should always return an object, see setupCommunityMetadata()

			if (!global.bInDataset) {
				global.bInDataset = true;
				_community_globalInfo_observed.put(global.nIndex, global); // (preserves order)
				_community_nObserved++;
			}//TESTED
			_community_cachedValue = communityId;
			_community_nCachedId = global.nIndex;

		}//TESTED (both above clauses seen in true and false states)

		// (else nothing to do)

		return _community_nCachedId;
	}//TESTED

	///////////////////////////////////////////////

	// Initialization function - only need be called if >1 community observed

	void community_setupMetadata() {
		_community_globalInfo = new TreeMap<ObjectId, Community_GlobalInfo>();
		_community_globalInfo_observed = new TreeMap<Integer, Community_GlobalInfo>();
		int n = _community_ids.length;
		_community_correlationTable = new Community_GlobalCorrelation[1 + community_getCorrelationIndex(n - 2, n - 1)];
		_community_globalInfoTable = new Community_GlobalInfo[n]; 

		int nPosInTable = 0;
		for (int i = 0; i < _community_ids.length; ++i) {
			Community_GlobalInfo commInfo = new Community_GlobalInfo(i);
			_community_globalInfo.put(_community_ids[i], commInfo);
			_community_globalInfoTable[i] = commInfo;
			for (int j = i + 1; j < _community_ids.length; ++j) {
				if (0 == i) { // First time through, also setting up communities 
					Community_GlobalInfo subCommInfo = new Community_GlobalInfo(j);					
					_community_globalInfo.put(_community_ids[j], subCommInfo);
				}
				Community_GlobalCorrelation correlation = new Community_GlobalCorrelation(i, j);
				_community_correlationTable[nPosInTable] = correlation;
				nPosInTable++;
			}
		}
	}//TESTED

	/////////////////////////////////////////////////////////////////////////////////////////////////////

	// Per entity function

	void community_updateCorrelations(EntSigHolder ent, long nTotalDocCount, String sIndex) {		
		int newCommunityId = _community_nCachedId;
		
		boolean bMultiCommunityNotInitialized = (null == ent.community.multiInfo) && (null == ent.community.multiInfoMap); 

		if ((-1 == ent.community.singleId) && (newCommunityId == _community_nPreMultiId)) {
			//CASE 1: First time through in "multi mode", not a new community though

			ent.community.singleId = newCommunityId; // (can't be -1 because if we're here it's because we're in "multi mode")
			
			//DEBUG PRINT:
			//System.out.println("ScoringUtils.community_updateCorrelations#1: " + sIndex 
			//		+ " / updated deferred single community id" + newCommunityId);			
			return;
		}//TESTED (case1)
		else if (bMultiCommunityNotInitialized && (newCommunityId == ent.community.singleId)) {
			// CASE 2: STILL ONLY SEEN ONE COMMUNITY

			// (nothing to do, handled by existing "single community" code)
			return;
		}//TESTED (case2)
		else if (bMultiCommunityNotInitialized) {
			// CASE 3: MULTIPLE COMMUNITIES FOR THE FIRST TIME

			if (-1 == ent.community.singleId) {
				ent.community.singleId = _community_nPreMultiId;

				//DEBUG PRINT:
				//System.out.println("ScoringUtils.community_updateCorrelations#3a: " + sIndex
				//		+ " / updating deferred cache " + _community_nCachedIdMap);

			}//TESTED

			// Some ugly optimization logic (also elsewhere in this function):
			// - <=10 communities, just use "ent.multipleCommunityInfo"
			// - 10<.<=25 communities, use both array and map
			// - >25 communities, just use map
			if (_community_ids.length <= _community_nUpperLimitForArrays) {
				ent.community.multiInfo = new Community_LocalInfo[_community_ids.length];
			}
			if (_community_ids.length > _community_nLowerLimitForMaps) {
				ent.community.multiInfoMap = new TreeMap<Integer,Community_LocalInfo>();
			}
			// Information from previously-sole community:
			Community_LocalInfo infoForCommunity = new Community_LocalInfo();
			infoForCommunity.communityCount = ent.nTotalDocCount;
			infoForCommunity.datasetCount = ent.nDocCountInQuerySubset;
			if (null != ent.community.multiInfo) {
				ent.community.multiInfo[ent.community.singleId] = infoForCommunity;
			}
			if (null != ent.community.multiInfoMap) {
				ent.community.multiInfoMap.put(ent.community.singleId, infoForCommunity); 
			}

			// Information from new community:
			Community_LocalInfo infoForCommunity2 = new Community_LocalInfo();
			infoForCommunity2.communityCount = nTotalDocCount;
			infoForCommunity2.datasetCount = 1;
			if (null != ent.community.multiInfo) {
				ent.community.multiInfo[newCommunityId] = infoForCommunity2;
			}
			if (null != ent.community.multiInfoMap) {
				ent.community.multiInfoMap.put(newCommunityId, infoForCommunity2); 
			}
			//(update global and local entity and community counts)
			_community_globalInfoTable[newCommunityId].nEntityCount++;
			ent.community.observedCount++; 

			// Update global correlation (update both sides, unusually):

			Community_GlobalCorrelation correlation = 
				_community_correlationTable[community_getCorrelationIndex(newCommunityId, ent.community.singleId)];
			correlation.updateCorrelation(newCommunityId,  ent.community.singleId, nTotalDocCount, ent.nTotalDocCount);

			//DEBUG PRINT:
			//System.out.println("ScoringUtils.community_updateCorrelations#3c: "  + sIndex 
			//		+ " / " + newCommunityId + " prev " + ent.community.singleId);
			//System.out.println("ScoringUtils.community_updateCorrelations#3c, correlation: " + new com.google.gson.Gson().toJson(correlation));
			//System.out.println("ScoringUtils.community_updateCorrelations#3c, local old: " + new com.google.gson.Gson().toJson(infoForCommunity));
			//System.out.println("ScoringUtils.community_updateCorrelations#3c, local new: " + new com.google.gson.Gson().toJson(infoForCommunity2));

		}//TESTED(case3)
		else {
			// CASE 4: WORKING WITH MULTIPLE COMMUNITIES

			Community_LocalInfo infoForCommunity;
			if (null != ent.community.multiInfo) { // (obviously try array lookup first, much faster)
				infoForCommunity = ent.community.multiInfo[newCommunityId];
			}
			else { // then the info map must be non-null
				infoForCommunity = ent.community.multiInfoMap.get(newCommunityId);
			}
			if (null == infoForCommunity) { // New community, add to local list and update global communities
				infoForCommunity = new Community_LocalInfo();
				infoForCommunity.communityCount = nTotalDocCount;

				//DEBUG PRINT:
				//System.out.println("ScoringUtils.community_updateCorrelations#4a: " + sIndex
				//		+ " / " + newCommunityId);

				community_updateCorrelations_innerLoop(ent, nTotalDocCount, newCommunityId, false);

				// Update local info:

				if (null != ent.community.multiInfo) {
					ent.community.multiInfo[newCommunityId] = infoForCommunity;
				}
				if (null != ent.community.multiInfoMap) {
					ent.community.multiInfoMap.put(newCommunityId, infoForCommunity); 					
				}
				//(update global and local entity and community counts)
				_community_globalInfoTable[newCommunityId].nEntityCount++;
				ent.community.observedCount++;

				//DEBUG PRINT:
				//System.out.println("ScoringUtils.community_updateCorrelations#4a, local new: " + newCommunityId +": " 
				//		+ new com.google.gson.Gson().toJson(infoForCommunity)
				//		+ " / global=" + _community_globalInfoTable[newCommunityId] .nEntityCount + ", local_comms=" + ent.community.observedCount
				//		+ " (" + ent.community.mutliInfo  + "/" + ent.community.multiInfoMap + ")");

			}//(end if new community for this entity)
			else {				
				// Just check if the DC is up-to-date:
				if (nTotalDocCount > infoForCommunity.communityCount) {
					//DEBUG PRINT:
					//System.out.println("ScoringUtils.community_updateCorrelations#4b, update local old: " + sIndex + " , " + new com.google.gson.Gson().toJson(infoForCommunity)
					//		+ " (" + ent.community.multiInfo  + " to " + nTotalDocCount + ")");

					// (Update all the correlation tables if the difference is significant...) 
					long nDiff = nTotalDocCount - infoForCommunity.communityCount;  
					if (((nDiff*100)/nTotalDocCount) > 15) { // say >15%
						community_updateCorrelations_innerLoop(ent, nDiff, newCommunityId, true);
					}
				}
			}//TESTED, including >15% case (modified by hand)

			// In either case, update the dataset count
			infoForCommunity.datasetCount++;

		}//TESTED (case4)

	}//TESTED 

	///////////////////////////////////////////////

	// Utility function for community_updateCorrelations()

	private void community_updateCorrelations_innerLoop(EntSigHolder ent, long nTotalDocCount, int newCommunityId, boolean bUpdateNewOnly)
	{
		// Update global correlations:
		// (Note slightly unpleasant due to performance-related use of both array and map)
		// (Also note in !bUpdateNewOnly haven't inserted new object yet so can happily loop over collection without worrying about that)
		if (null != ent.community.multiInfoMap) { // Loop over the map, faster than the array
			for (Map.Entry<Integer,Community_LocalInfo> existingCommunityEntry: ent.community.multiInfoMap.entrySet()) {
				int nExistingCommunityId = existingCommunityEntry.getKey();
				if (bUpdateNewOnly && (newCommunityId == nExistingCommunityId)) {
					continue;
				}
				long nExistingTotalDocCount = existingCommunityEntry.getValue().communityCount;
				Community_GlobalCorrelation correlation = 
					_community_correlationTable[community_getCorrelationIndex(newCommunityId, nExistingCommunityId)];

				if (bUpdateNewOnly) {
					nExistingTotalDocCount = 0;
				}
				correlation.updateCorrelation(newCommunityId,  nExistingCommunityId, nTotalDocCount, nExistingTotalDocCount);
				//DEBUG PRINT:
				//System.out.println("ScoringUtils.community_updateCorrelations#4xa, correlation: " + new com.google.gson.Gson().toJson(correlation));
			}//TESTED

			//(~ cut and pasted below)			
		}
		else { // Loop over the array, faster than the map

			//(~ cut and paste from above)
			for (int i = 0; i < ent.community.multiInfo.length; ++i) {
				if (bUpdateNewOnly && (newCommunityId == i)) {
					continue;
				}
				Community_LocalInfo existingCommunity = ent.community.multiInfo[i];
				if (null != existingCommunity) { // (can be null, means we haven't seen it yet)							
					long nExistingTotalDocCount = existingCommunity.communityCount;

					Community_GlobalCorrelation correlation = 
						_community_correlationTable[community_getCorrelationIndex(newCommunityId, i)];

					if (bUpdateNewOnly) {
						nExistingTotalDocCount = 0;
					}
					correlation.updateCorrelation(newCommunityId,  i, nTotalDocCount, nExistingTotalDocCount);
					//DEBUG PRINT:
					//System.out.println("ScoringUtils.community_updateCorrelations#4xb, correlation: " + new com.google.gson.Gson().toJson(correlation));
				}						
			}//TESTED
		}				
	}//TESTED

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	// Use the correlation coefficients calculated during stage1_/community_ to estimate any missing doc counts - top level logic

	double community_estimateAnyMissingDocCounts(EntSigHolder ent) {
		
		double estimatedDocCount = 0;

		// Check first if we can immediately return:
		if ((null == _community_globalInfo_observed) || (ent.community.observedCount == _community_globalInfo_observed.size())) {
			
			//DEBUG PRINT:
			//System.out.println("community_estimateAnyMissingDocCounts: Escaping unneeded calcs: " + ent.entityInstances.get(0).dbo.getString("index") + 
			//		" / " + ent.community.observedCount + " , " + new com.google.gson.Gson().toJson(_community_globalInfo_observed));
			
			return 0;
		}//TESTED
		
		// If not, need to find the missing communities and estimate their total doc counts:
		
		if (null == ent.community.multiInfoMap) { // This is just a really easy case - small number of communities - so let's knock it off first
			
			Iterator<Map.Entry<Integer, Community_GlobalInfo>> globalItQuick = _community_globalInfo_observed.entrySet().iterator();
			while (globalItQuick.hasNext()) {
				Map.Entry<Integer, Community_GlobalInfo> globalMeta = globalItQuick.next();
				int nEntityIt = globalMeta.getKey();
				
				boolean bMissingEntity = false;
				if (null != ent.community.multiInfo) {
					Community_LocalInfo local =  ent.community.multiInfo[nEntityIt];
					bMissingEntity = (null == local) || (0 == local.datasetCount);
				}
				else { // Special case, entity only ever saw 1 community
					bMissingEntity = !((-1 == ent.community.singleId) ? (nEntityIt == _community_nPreMultiId) : (nEntityIt == ent.community.singleId));
				}
				if (bMissingEntity) { // Missing
					estimatedDocCount += community_estimateMissingDocCount(ent, nEntityIt); 

					//DEBUG PRINT:
					//System.out.println("community_estimateAnyMissingDocCounts: Update1a: " + ent.entityInstances.get(0).dbo.getString("index") + ": " 
					//		+ nEntityIt + " , " + ent.community.observedCount + " = " + estimatedDocCount);
				}
			}
			return estimatedDocCount;
		}//TESTED (both singleId case and (null != ent.community.multiInfo) case) 
		
		// If we've got here then there are enough communities that we need to loop over only observed ones
		
		Iterator<Map.Entry<Integer, Community_GlobalInfo>> globalIt = _community_globalInfo_observed.entrySet().iterator();
		Iterator<Map.Entry<Integer, Community_LocalInfo>> entityIt = ent.community.multiInfoMap.entrySet().iterator();
		
		while (entityIt.hasNext()) {
			Map.Entry<Integer, Community_LocalInfo> entityInfo = entityIt.next(); 
			
			ObjectId entityCommunityId = _community_ids[entityInfo.getKey()]; 
			Map.Entry<Integer, Community_GlobalInfo> globalInfo = globalIt.next(); // (if entityIt hasNext then so must globalIt)
			
			while (true) {
				ObjectId globalCommunityId = _community_ids[globalInfo.getKey()];
				
				if (globalCommunityId.equals(entityCommunityId)) { // Lock step
					break;
				}
				else {
					estimatedDocCount += community_estimateMissingDocCount(ent, globalInfo.getKey());
					
					//DEBUG PRINT:
					//System.out.println("community_estimateAnyMissingDocCounts: Update2: " + ent.entityInstances.get(0).dbo.getString("index") + ": " 
					//		+ globalInfo.getKey() + " , " + ent.community.observedCount + " = " + estimatedDocCount);
					
					if (globalIt.hasNext()) {
						globalInfo = globalIt.next();
					}
					else {
						break;
					}
				}
			}// (end "lockstep" loop over all observed global communities)
			
		}//(end "lockstep" loop over communities observed in entity)
		//TESTED
		
		while (globalIt.hasNext()) {
			Map.Entry<Integer, Community_GlobalInfo> globalInfo = globalIt.next();
			estimatedDocCount += community_estimateMissingDocCount(ent, globalInfo.getKey());
			//DEBUG PRINT:
			//System.out.println("community_estimateAnyMissingDocCounts: " + ent.entityInstances.get(0).dbo.getString("index")
			//		+ ": add missing counts from " + globalInfo.getKey() + ": " + estimatedDocCount);
		}
		//TESTED		
		
		return estimatedDocCount;
	}//TESTED
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	// Use the correlation coefficients calculated during stage1_/community_ to estimate any missing doc counts - actual calculation

	double community_estimateMissingDocCount(EntSigHolder ent, int nMissingCommunityId) {

		double estimatedDocCount = 0;
		
		if (null != ent.community.multiInfoMap) { // Loop over the map, faster than the array
			
			//DEBUG PRINT:
			//System.out.println("community_estimateMissingDocCount 1: loop over ent.community.multiInfoMap");
			
			for (Map.Entry<Integer,Community_LocalInfo> existingCommunityEntry: ent.community.multiInfoMap.entrySet()) {
				int nExistingCommunityId = existingCommunityEntry.getKey();
				Community_LocalInfo existingCommunity = existingCommunityEntry.getValue();
					// (note can't be == to nMissingCommunityId otherwise it wouldn't be missing!)

				Community_GlobalCorrelation correlation = 
					_community_correlationTable[community_getCorrelationIndex(nMissingCommunityId, nExistingCommunityId)];

				estimatedDocCount += community_estimateMissingDocCount_innerLoop(nMissingCommunityId, nExistingCommunityId, existingCommunity.communityCount, correlation);

			}// (end loop over community against which I can correlate)
			
			//(~ cut and pasted below)			
		}//TESTED
		else if (null != ent.community.multiInfo) { // Loop over the array, faster than the map
			//(~ cut and paste from above)
			
			//DEBUG PRINT:
			//System.out.println("community_estimateMissingDocCount 2: loop over ent.community.multiInfo");
			
			for (int i = 0; i < ent.community.multiInfo.length; ++i) {
				Community_LocalInfo existingCommunity = ent.community.multiInfo[i];
				
				if (null != existingCommunity) {
					Community_GlobalCorrelation correlation = 
						_community_correlationTable[community_getCorrelationIndex(nMissingCommunityId, i)];
	
					estimatedDocCount += community_estimateMissingDocCount_innerLoop(nMissingCommunityId, i, existingCommunity.communityCount, correlation);
				}
			}
		}//TESTED (end performance-oriented choice over which collection to loop over)
		else {
			// In this case, the entity has never been intiialized, ie only has 1 entity to compare against: 

			//DEBUG PRINT:
			//System.out.println("community_estimateMissingDocCount 3: no loop needed, single id: " + this._community_nPreMultiId + "/" + ent.community.singleId);
			
			int nExistingCommunity = (-1 == ent.community.singleId) ?  this._community_nPreMultiId : ent.community.singleId;
			
			Community_GlobalCorrelation correlation = 
				_community_correlationTable[community_getCorrelationIndex(nMissingCommunityId, nExistingCommunity)];
			
			estimatedDocCount += community_estimateMissingDocCount_innerLoop(nMissingCommunityId, nExistingCommunity, ent.nTotalDocCount, correlation);
		}//(TESTED)
		
		return estimatedDocCount;		
	}//TESTED (all 3 cases above)

	///////////////////////////////////////////////

	// Utility function for community_estimateMissingDocCount()

	
	private static Double[] frequencyAdjustment = {  0.218, 0.357, 0.433, 0.477, 0.497, 0.497, 0.477, 0.433, 0.357, 0.218  };
		// (for high query overlaps, the "significance" (statistical) of an entity not overlapping is high, so suppress somewhat the calculated frequency)
		// (for low query overlaps, suppress the calculated frequency to take into account prob that entity simply doesn't occur in both communities)
			// (so in "conclusion" pre-calculate table of sqrt((1 - t)t) for 10 buckets, 0-9,10-19,etc)
	
	private double community_estimateMissingDocCount_innerLoop(int nMissingCommunityId, int nExistingCommunityId,
																long nExistingCommunityCount, Community_GlobalCorrelation correlation) {
		long nMissingCommunityOverlapCount;
		long nExistingCommunityOverlapCount;
		if (correlation.getOverlapping_entities() > 0) {
			
			long nMissingEntityCount = this._community_globalInfoTable[nMissingCommunityId].nEntityCount;
			long nExistingEntityCount = this._community_globalInfoTable[nExistingCommunityId].nEntityCount;
			double freqAdjustment = 0.109; // (a default just in case nXxxEntityCount == correlation, == 0.5*frequencyAdjustment[0])
			if (nMissingEntityCount < nExistingEntityCount) { // (use the smaller of the 2)
				if (nMissingEntityCount > correlation.getOverlapping_entities()) { // (just in case!)
					freqAdjustment = frequencyAdjustment[(int) ((correlation.getOverlapping_entities()*10)/nMissingEntityCount)];
				}
			}
			else {
				if (nExistingEntityCount > correlation.getOverlapping_entities()) { // (just in case!)
					freqAdjustment = frequencyAdjustment[(int) ((correlation.getOverlapping_entities()*10)/nExistingEntityCount)];
				}
			}
			if (nMissingCommunityId == correlation.getCommunity1_id()) {
				nMissingCommunityOverlapCount = correlation.getCommunity1_docCount();
				nExistingCommunityOverlapCount = correlation.getCommunity2_docCount();						
			}
			else {
				nMissingCommunityOverlapCount = correlation.getCommunity2_docCount();
				nExistingCommunityOverlapCount = correlation.getCommunity1_docCount();						
			}
			if (nExistingCommunityOverlapCount > 0) {

				//DEBUG PRINT:
				//System.out.println("community_estimateMissingDocCount_innerLoop ("+nMissingCommunityId+","+nExistingCommunityId+"): " + 
				//		freqAdjustment +" * " + 
				//		(nExistingCommunityCount*nMissingCommunityOverlapCount)/nExistingCommunityOverlapCount+ " << "
				//		+ nMissingCommunityOverlapCount +" , " + nExistingCommunityOverlapCount + " ; " + nExistingCommunityCount
				//		+ " / " + nMissingEntityCount + " , " + nExistingEntityCount + " ; " + correlation.getOverlapping_entities());				
				
				return (freqAdjustment*(nExistingCommunityCount*nMissingCommunityOverlapCount)/nExistingCommunityOverlapCount);
			}
		}
		return 0;
	}//TESTED
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	// Deduplication logic
	
	static boolean community_areDuplicates(TempDocBucket lhs, TempDocBucket rhs) {
		if (lhs.url.equals(rhs.url)) {
			// These are probably duplicates, we'll do some more tests...
			BasicDBList lhsEnts = (BasicDBList) lhs.dbo.get(DocumentPojo.entities_);
			BasicDBList rhsEnts = (BasicDBList) rhs.dbo.get(DocumentPojo.entities_);
			
			if (!((null != lhsEnts) ^ (null != rhsEnts))) // ie both null or neither null
			{
				if (null != lhsEnts) {
					if (lhsEnts.size() != rhsEnts.size()) {
						return false;
					}//TESTED
					if (lhsEnts.size() > 0) {
						BasicDBObject lhsFirstEnt = (BasicDBObject) lhsEnts.get(0);
						BasicDBObject rhsFirstEnt = (BasicDBObject) rhsEnts.get(0);
						String lhsIndex = lhsFirstEnt.getString(EntityPojo.index_);
						String rhsIndex = rhsFirstEnt.getString(EntityPojo.index_);
						if ((null != lhsIndex) && (null != rhsIndex)) {
							if (!lhsIndex.equals(rhsIndex)) {
								return false;
							}
						}
					}//(end "random" entity test)
					//TESTED
				}
			} // (end entity count test)
			
			// Finally we'll just count the events:
			BasicDBList lhsEvents = (BasicDBList) lhs.dbo.get(DocumentPojo.associations_);
			BasicDBList rhsEvents = (BasicDBList) rhs.dbo.get(DocumentPojo.associations_);
			if (!((null != lhsEvents) ^ (null != rhsEvents))) // ie both null or neither null
			{
				if (null != lhsEvents) {
					if (lhsEvents.size() != rhsEvents.size()) {
						return false;
					}//TESTED					
				}
			}//(end event count test)
						
			return true;
		}
		return false;		
	}//TESTED
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	// Combine duplicate documents
	
	static void community_combineDuplicateDocs(TempDocBucket masterDoc) {
		//(source key)
		BasicDBList masterSourceKeyList = (BasicDBList) masterDoc.dbo.get(DocumentPojo.sourceKey_);
		String masterSourceKey = (masterSourceKeyList.size() > 0) ? (String)masterSourceKeyList.get(0) : null;
		HashSet<String> sourceKeySet = null;
		//(community Id)
		BasicDBList masterCommunityList = (BasicDBList) masterDoc.dbo.get(DocumentPojo.communityId_);
		ObjectId masterCommunityId = (masterCommunityList.size() > 0) ? (ObjectId)masterCommunityList.get(0) : null;
		HashSet<ObjectId> communityIdSet = null;
		//(source title)
		BasicDBList masterSourceList = null;
		String masterSource = null;
		HashSet<String> sourceSet = null;
		//(mediaType)
		BasicDBList masterTypeList = null;
		String masterType = null;
		HashSet<String> typeSet = null;
		//(tags)
		BasicDBList masterTagList = null;
		String masterTag = null;
		HashSet<String> tagSet = null;
		
		for (TempDocBucket slave = masterDoc.dupList; null != slave; slave = slave.dupList) {
			String sourceKey = slave.dbo.getString(DocumentPojo.sourceKey_);
			int nCommunityPos = sourceKey.indexOf('#');
			if ((nCommunityPos > 0) && (null != sourceKey)) {
				sourceKey = sourceKey.substring(0, nCommunityPos);
			}//TESTED
			
			// Always combine communities:
			ObjectId communityIdObj = (ObjectId) slave.dbo.get(DocumentPojo.communityId_);
			communityIdSet = combineElements(communityIdSet, TypeErasureWorkaround.Type.OBJECTID, masterCommunityList, masterCommunityId, slave, null, communityIdObj);
			
			if ((null != masterSourceKey) && (null != sourceKey) && sourceKey.equals(masterSourceKey)) {
				continue; 
			}//TESTED
			
			// else fall through to...
			
			// Combine source keys:
			sourceKeySet = combineElements(sourceKeySet, TypeErasureWorkaround.Type.STRING, masterSourceKeyList, masterSourceKey, slave, null, sourceKey);
			
			// Combine source titles:
			masterSourceList = (BasicDBList) masterDoc.dbo.get(DocumentPojo.source_);
			masterSource = (masterSourceList.size() > 0) ? (String)masterSourceList.get(0) : null;
			String slaveSource =  slave.dbo.getString(DocumentPojo.source_);
			sourceSet = combineElements(sourceSet, TypeErasureWorkaround.Type.STRING, masterSourceList, masterSource, slave, null, slaveSource);
			
			// Combine media types:
			masterTypeList = (BasicDBList) masterDoc.dbo.get(DocumentPojo.mediaType_);
			masterType = (masterTypeList.size() > 0) ? (String)masterTypeList.get(0) : null;
			String slaveType =  slave.dbo.getString(DocumentPojo.mediaType_);
			typeSet = combineElements(typeSet, TypeErasureWorkaround.Type.STRING, masterTypeList, masterType, slave, null, slaveType);
			
			// Combine tags:
			masterTagList = (BasicDBList) masterDoc.dbo.get(DocumentPojo.tags_);
			masterTag = (masterTagList.size() > 0) ? (String)masterTagList.get(0) : null;
			BasicDBList slaveTagList =  (BasicDBList) slave.dbo.get(DocumentPojo.tags_);
			tagSet = combineElements(tagSet, TypeErasureWorkaround.Type.STRING, masterTagList, masterTag, slave, slaveTagList, null);
			
		} // (end loop over slaves)
		
		// Now use any sets created to fill in
		
		if (null != communityIdSet) {
			masterCommunityList.clear();
			for (ObjectId id: communityIdSet) {		
				masterCommunityList.add(id);				
			}
		}//TESTED
		
		if (null != sourceKeySet) {
			masterSourceKeyList.clear();
			for (String key: sourceKeySet) {		
				masterSourceKeyList.add(key);				
			}
		}//TESTED
		
		if (null != sourceSet) {
			masterSourceList.clear();
			for (String src: sourceSet) {		
				masterSourceList.add(src);				
			}
		}//TESTED
		
		if (null != typeSet) {
			masterTypeList.clear();
			for (String type: typeSet) {		
				masterTypeList.add(type);				
			}
		}//TESTED
		
		if (null != tagSet) {
			masterTagList.clear();
			for (String tag: tagSet) {		
				masterTagList.add(tag);				
			}
		}//TESTED
		
	}//TESTED
	
	///////////////////////////////////////////////
	
	// Utilities
	
	private static class TypeErasureWorkaround {
		public enum Type { OBJECTID, STRING }
		public static HashSet<?> newSet(Type type) {
			if (Type.OBJECTID == type) {
				return new HashSet<ObjectId>();			
			}
			else {
				return new HashSet<String>();
			}
		}
	}//TESTED
	
	///////////////////////////////////////////////

	// Combine the elements from the different documents into arrays (preserving uniqueness)
	
	@SuppressWarnings("unchecked")
	private static <T> HashSet<T> combineElements(HashSet<T> combinorSet, TypeErasureWorkaround.Type type,  
														BasicDBList masterEls, T masterEl, 
														TempDocBucket slave, BasicDBList slaveEls, T slaveEl)
	{
		int nSlaveEls = 0;
		if (null == slaveEl) {
			slaveEl = ((nSlaveEls = slaveEls.size()) > 0) ? (T)slaveEls.get(0) : null;
		}
		if ((null == combinorSet) && (null == slave.dupList) && (masterEls.size() < 2)) { // most of the time, only combining 2 objects 
			//DEBUG PRINT:
			//System.out.println("community_combineDuplicateDocs.combineElements#1: " + slaveEl.toString() + " vs " +  masterEl.toString());
			
			if (null != slaveEl) {
				if ((masterEl == null) || !slaveEl.equals(masterEl)) {
					masterEls.add(slaveEl);
				}
				if (nSlaveEls > 1) { // (pretty inelegant way of doing this never mind, it's only in 2 places)
					for (int i = 1; i < nSlaveEls; ++i) {
						slaveEl = (T) slaveEls.get(i);
						if ((masterEl == null) || !slaveEl.equals(masterEl)) {
							masterEls.add(slaveEl);
						}
					}
				}//TESTED
			}
		}//TESTED
		else { // Slower version, needed because >1 duplicate so it gets a bit messy...
			if (null == combinorSet) {
				combinorSet = (HashSet<T>) TypeErasureWorkaround.newSet(type);
				combinorSet.add(masterEl);
				int nMasterEls = masterEls.size();
				for (int i = 1; i < nMasterEls; ++i) {
					combinorSet.add((T) masterEls.get(i));					
				}				
			}//TESTED
			combinorSet.add(slaveEl);
			if (nSlaveEls > 1) { // (pretty inelegant way of doing this never mind, it's only in 2 places)
				for (int i = 1; i < nSlaveEls; ++i) {
					combinorSet.add((T) slaveEls.get(i));
				}
			}//TESTED
			//DEBUG PRINT:
			//System.out.println("community_combineDuplicateDocs.combineElements#2: " + slaveEl.toString() + " vs " +  masterEl.toString());
		}//TESTED		
		return combinorSet;
	}//TESTED
}
