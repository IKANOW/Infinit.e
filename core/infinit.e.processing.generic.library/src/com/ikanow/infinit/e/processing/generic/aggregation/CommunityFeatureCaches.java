package com.ikanow.infinit.e.processing.generic.aggregation;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.WeakHashMap;

import org.bson.types.ObjectId;

import com.ikanow.infinit.e.data_model.store.feature.association.AssociationFeaturePojo;
import com.ikanow.infinit.e.data_model.store.feature.entity.EntityFeaturePojo;

// Here's the basic idea:
// (CUSTOM: set the entity cache in advance - set a transient flag on them so will ignore second time through)
// (CUSTOM: set incremental mode in GENERIC)
// (GENERIC: step over entities creating that _aggregatedEntities, ignoring those with transient flag)
// (GENERIC: where it matches just avoid doing findAndModify, just do update instead, unless cache miss ... in incremental don't even do update)
// (CUSTOM: at the end whizz over the delta doing a load of updates - only populated in incremental mode)

public class CommunityFeatureCaches {

	private static boolean _incrementalMode = false;
	public static void setIncrementalMode(boolean mode) {
		_incrementalMode = mode;
	}
	//TODO (INF-2825): Incremental mode not yet working:
	private static boolean _fullIncrementalModeDisabled = true;
	
	// For debugging in tomcat, clear the cache
	public static void clearCacheInDebugMode() {
		_cache.clear();
	}//TESTED (by hand)
	
	private static HashMap<ObjectId, CommunityFeatureCache> _cache = new HashMap<ObjectId, CommunityFeatureCache>();
	
	public static CommunityFeatureCache getCommunityFeatureCache(ObjectId communityId) {
		CommunityFeatureCache retVal = null;
		synchronized (CommunityFeatureCaches.class) {
			retVal = _cache.get(communityId);
			if (null == retVal) {
				retVal = new CommunityFeatureCache(communityId);
				_cache.put(communityId, retVal);
				return retVal;
			}
		}
		return retVal;
	}//TESTED (by hand) 
	
	public static class CommunityFeatureCache {
		
		ObjectId _communityId;
		// Change these into weak references..
		//Entities
		protected Map<String, EntityFeaturePojo> _entityCache; //WeakHashMap via synchronized
		protected Map<String, EntityFeaturePojo> _deltaEntityList; // will update these in the background
		//Associations:
		protected Map<String, AssociationFeaturePojo> _assocCache; //WeakHashMap via synchronized
		protected Map<String, AssociationFeaturePojo> _deltaAssocList; // will update these in the background
		
		public CommunityFeatureCache(ObjectId communityId) {
			_communityId = communityId;
			_entityCache = new WeakHashMap<String, EntityFeaturePojo>();
			_assocCache = new WeakHashMap<String, AssociationFeaturePojo>();
		}
		ObjectId getCommunityId() {
			return _communityId;
		}
		
		public synchronized int getEntityCacheSize() {
			return _entityCache.size();
		}
		public synchronized int getAssocCacheSize() {
			return _assocCache.size();
		}
		
		public synchronized EntityFeaturePojo getCachedEntityFeature(EntityFeaturePojo in) {
			EntityFeaturePojo retVal = _entityCache.get(in.getIndex());
			if (null == retVal) {
				// (gets created in a separate call, once created from the DB field)
				return null;
			}
			else { //(incremental mode: create a delta list for efficient updates later)
				if (CommunityFeatureCaches._incrementalMode && !_fullIncrementalModeDisabled) {
					if (null == _deltaEntityList) {
						_deltaEntityList = new TreeMap<String, EntityFeaturePojo>();
					}
					// Update all retVal's fields
					
					EntityFeaturePojo toUpdate = _deltaEntityList.get(in.getIndex());
					if (null == toUpdate) {
						_deltaEntityList.put(in.getIndex(), in);
					}
					else {
						copyAttributes(in, toUpdate, false);
					}
				}
				// (don't copy yet - need to know which fields are different so can optimize the query)

				//DEBUG
				//System.out.println("ENT " + retVal.getDoccount() + ": " + retVal.getIndex() + " ? " + in.getDoccount());
				
				return retVal;
			}
		}//TESTED (by hand)
		public synchronized AssociationFeaturePojo getCachedAssocFeature(AssociationFeaturePojo in) {
			AssociationFeaturePojo retVal = _assocCache.get(in.getIndex());
			if (null == retVal) {
				// (gets created in a separate call, once created from the DB field)
				return null;
			}
			else { //(incremental mode: create a delta list for efficient updates later)
				if (CommunityFeatureCaches._incrementalMode && !_fullIncrementalModeDisabled) {
					if (null == _deltaEntityList) {
						_deltaAssocList = new TreeMap<String, AssociationFeaturePojo>();
					}
					// Update all retVal's fields
					
					AssociationFeaturePojo toUpdate = _deltaAssocList.get(in.getIndex());
					if (null == toUpdate) {
						_deltaAssocList.put(in.getIndex(), in);
					}
					else {
						copyAttributes(in, toUpdate, false);
					}
				}
				// (don't copy yet - need to know which fields are different so can optimize the query)

				//DEBUG
				//System.out.println("ASSOC " + retVal.getDoccount() + ": " + retVal.getIndex() + " ? " + in.getDoccount());
				
				return retVal;
			}
		}//TESTED (by hand)

		public synchronized void updateCachedEntityFeatureStatistics(EntityFeaturePojo cachedEnt, EntityFeaturePojo newEnt) {
			copyAttributes(newEnt, cachedEnt, true);
		}
		public synchronized void updateCachedAssocFeatureStatistics(AssociationFeaturePojo cachedAssoc, AssociationFeaturePojo newAssoc) {
			copyAttributes(newAssoc, cachedAssoc, true);
		}
		
		public synchronized void addCachedEntityFeature(EntityFeaturePojo newEnt) {
			// Conceptually simple ... just created a new EntityFeaturePojo from the data store so cache it
			// Complication: another thread might have already cached it, in which case it just looks like a non-incremental add
			EntityFeaturePojo retVal = _entityCache.get(newEnt.getIndex());
			if (null == retVal) {
				_entityCache.put(newEnt.getIndex(), newEnt);
			}			
			else { // someone else beat me to it, no need to create a delta though, i've already updated the DB..
				copyAttributes(newEnt, retVal, false);				
			}
		}//TESTED (by hand)
		public synchronized void addCachedAssocFeature(AssociationFeaturePojo newAssoc) {
			// Conceptually simple ... just created a new EntityFeaturePojo from the data store so cache it
			// Complication: another thread might have already cached it, in which case it just looks like a non-incremental add
			AssociationFeaturePojo retVal = _assocCache.get(newAssoc.getIndex());
			if (null == retVal) {
				_assocCache.put(newAssoc.getIndex(), newAssoc);
			}			
			else { // someone else beat me to it, no need to create a delta though, i've already updated the DB..
				copyAttributes(newAssoc, retVal, false);				
			}
		}//TESTED (by hand)
		
		public synchronized Collection<EntityFeaturePojo> getAndClearEntityDeltaList() {
			if (null == _deltaEntityList) {
				return null;
			}
			Collection<EntityFeaturePojo> retVal = _deltaEntityList.values();
			_deltaEntityList = null;
			return retVal;
		}//TODO (INF-2825): TOTEST
		
		public synchronized Collection<AssociationFeaturePojo> getAndClearAssociationDeltaList() {
			if (null == _deltaAssocList) {
				return null;
			}
			Collection<AssociationFeaturePojo> retVal = _deltaAssocList.values();
			_deltaAssocList = null;
			return retVal;
		}//TODO (INF-2825): TOTEST
		
		////////////////////////////////////////////
		
		// POJO UTILS
		
		public static void copyAttributes(EntityFeaturePojo from, EntityFeaturePojo to, boolean copyBack) {
			// Update incremental statistics:
			if (null != from.getAlias()) to.addAllAlias(from.getAlias());
			to.setDoccount(to.getDoccount() + from.getDoccount());
			to.setTotalfreq(to.getTotalfreq() + from.getTotalfreq());
			if (null == to.getGeotag()) to.setGeotag(from.getGeotag());
			if (null != from.getSemanticLinks()) to.addToSemanticLinks(from.getSemanticLinks());	
			
			if (copyBack) { // In addition to update the statistics, we're copying back from the cache into the ent feature, that is 
				if (null != to.getAlias()) from.addAllAlias(to.getAlias());
				from.setDoccount(to.getDoccount());
				from.setTotalfreq(to.getTotalfreq());
				from.setDbSyncDoccount(to.getDbSyncDoccount());
				from.setDbSyncTime(to.getDbSyncTime());				
				if (null == from.getGeotag()) from.setGeotag(to.getGeotag());
				if (null != to.getSemanticLinks()) from.addToSemanticLinks(to.getSemanticLinks());
			}
			else { // purely updating the cache for the first time
				to.setDbSyncDoccount(from.getDbSyncDoccount());
				to.setDbSyncTime(from.getDbSyncTime());				
			}		
			
		}//TESTED (by hand)
		
		public static void copyAttributes(AssociationFeaturePojo from, AssociationFeaturePojo to, boolean copyBack) {
			// Update incremental statistics:
			if (null != from.getEntity1()) for (String ent1: from.getEntity1()) to.addEntity1(ent1);
			if (null != from.getEntity2()) for (String ent2: from.getEntity2()) to.addEntity2(ent2);
			if (null != from.getVerb()) for (String verb: from.getVerb()) to.addVerb(verb);
			to.setDoccount(to.getDoccount() + from.getDoccount());
			
			if (copyBack) { // (See ent version for explanation)
				if (null != to.getEntity1()) for (String ent1: to.getEntity1()) from.addEntity1(ent1);
				if (null != to.getEntity2()) for (String ent2: to.getEntity2()) from.addEntity2(ent2);
				if (null != to.getVerb()) for (String verb: to.getVerb()) from.addVerb(verb);
				from.setDb_sync_time(to.getDb_sync_time());
				from.setDb_sync_doccount(to.getDb_sync_doccount());				
				from.setDoccount(to.getDoccount());				
			}
			else { // purely updating the cache for the first time
				to.setDb_sync_time(from.getDb_sync_time());
				to.setDb_sync_doccount(from.getDb_sync_doccount());				
			}
		}//TOTEST				
	}	
}
 