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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeSet;

import com.ikanow.infinit.e.api.knowledge.processing.ScoringUtils.EntSigHolder;
import com.ikanow.infinit.e.data_model.store.document.AssociationPojo;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;

class ScoringUtils_Associations {

/////////////////////////////////////////////////////////////////////////////////////////////////////////////	

// ADDITIONAL FUNCTIONALITY #1: STANDALONE EVENTS	
	
// Add standalone events to the output (aggregate across all time for facts, on a per-day basis for events and summaries)
// Events are returned in the order of the documents in which they first occurred (this is arbitrary)
	
// Note that this code will *also* alter the events returned in the documents in the case where
// the first instance of the event is in a document that is being promoted. This obviously isn't ideal but:
// - Assuming 1000+ docs analyzed, 100 docs returned, this won't affect all events anyway
// - The event has "better" time_start/time_end added and a "doccount" field that can be ignored, so 
//   it's not the end of the world
// - The performance hit of duplicating all events could be high, and the code complexity of optimized duplication is high
// This should be revisited during V0 aggregation improvements
	
	static class StandaloneEventHashAggregator {
		@SuppressWarnings("unchecked")
		StandaloneEventHashAggregator(LinkedList<BasicDBObject> primaryList, boolean bSimulateAggregation) {
			store = new HashMap<StandaloneEventHashCode,BasicDBObject>();
			listBuckets = (LinkedList<BasicDBObject>[])new LinkedList[NUM_BUCKETS]; 
			tmpList = new LinkedList<BasicDBObject>();
			this.bSimulateAggregation = bSimulateAggregation;
		}
		HashMap<StandaloneEventHashCode,BasicDBObject> store;
		double dMaxSig = 0; // (max sig observed)
		boolean bCalcSig = true; // (default)
		int nPhase0Events = 0; // (count the events from promoted docs - allows some optimization later)
		int nPhase1Events = 0; // (count the events from once-promoted docs - allows some optimization later)
		boolean bSimulateAggregation = false; // (if true, generates pure aggregations of events/facts)
		
		// Very basic prioritization
		private static final int NUM_BUCKETS = 100;
		private static final int NUM_BUCKETS_1 = 99;
		private static final double DNUM_BUCKETS = 100.0;
		
		LinkedList<BasicDBObject>[] listBuckets = null; 
		LinkedList<BasicDBObject> tmpList = null; // (until they're ordered)
		
	}//TESTED
	
	static class StandaloneEventHashCode { // (used to aggregate standalone events) 
		StandaloneEventHashCode(boolean bAggregation_, BasicDBObject evt_, boolean bIsSummary_, boolean bIsFact_) { 
			evt = new BasicDBObject(evt_);
			if (bAggregation_) { // Remove loads of things
				evt.remove(AssociationPojo.entity1_);
				evt.remove(AssociationPojo.entity2_);				
				evt.remove(AssociationPojo.verb_);
				evt.remove(AssociationPojo.time_start_);
				evt.remove(AssociationPojo.time_end_);
				evt.remove(AssociationPojo.geo_sig_);
			}//TESTED
			else {
				if (!bIsSummary_) {
					evt.remove(AssociationPojo.entity1_);
					evt.remove(AssociationPojo.entity2_);
				}
				if (bIsFact_) {
					evt.remove(AssociationPojo.time_start_);
					evt.remove(AssociationPojo.time_end_);				
				}
			}
			nHashCode = evt.hashCode();
		}//TESTED (saw facts, events, and summaries aggregate correctly)
		
		@Override
		public int hashCode() { return nHashCode; }
		@Override
		public boolean equals(Object obj) {
	        if ( ! ( obj instanceof StandaloneEventHashCode ) )
	            return false;
			return evt.equals(((StandaloneEventHashCode)obj).evt);
		}
		private BasicDBObject evt = null;
		private int nHashCode = -1;
	};
	
	/////////////////////////////////////////////////////////////
	
	// Prepare "nMaxToReturn" of the ~ highest ranked events  
	
	static void finalizeStandaloneEvents(LinkedList<BasicDBObject> standaloneEventList, StandaloneEventHashAggregator standaloneEventAggregator, int nMaxToReturn)
	{
		double dMaxSig = (standaloneEventAggregator.dMaxSig + 0.01); // (+0.01 ensures no div by zero and that dBucket<1.0)
		int nHighPrioAssocs = (standaloneEventAggregator.nPhase0Events + standaloneEventAggregator.nPhase1Events/4);
			// (all the docs being promoted and some of the docs that didn't quite make it)
		
		int nCurrAssoc = 0;
		int nFromHighPrioAddedToBucket = 0;
		for (BasicDBObject assoc: standaloneEventAggregator.tmpList) {

			try {
				double dAssocSig = assoc.getDouble(AssociationPojo.assoc_sig_);
				assoc.put(AssociationPojo.assoc_sig_, Math.sqrt(dAssocSig));
				
				double dBucket = dAssocSig/dMaxSig;
				int nBucket = StandaloneEventHashAggregator.NUM_BUCKETS_1 - 
								(int)(StandaloneEventHashAggregator.DNUM_BUCKETS*dBucket) 
									% StandaloneEventHashAggregator.NUM_BUCKETS; // (do crazy stuff if dBucket >= 1.0)
				
				LinkedList<BasicDBObject> bucketList = standaloneEventAggregator.listBuckets[nBucket];
				if (null == bucketList) {
					bucketList = new LinkedList<BasicDBObject>();
					standaloneEventAggregator.listBuckets[nBucket] = bucketList;
				}
				bucketList.add(assoc);
			}
			catch (Exception e) {
				// Just ignore that event
			}
			if (nCurrAssoc < nHighPrioAssocs) {
				nFromHighPrioAddedToBucket++;				
			}
			nCurrAssoc++;
						
			// Some exit criteria:
			
			if (nFromHighPrioAddedToBucket >= nMaxToReturn) { // Got enough events...
				if (nCurrAssoc >= nHighPrioAssocs) {  // And stepped through the high prio ones
					break;
				}
			}
			
		} // (end loop over all collected "events")
		
		// Now add the required number of elements to the output list:
		
		int nAddedToReturnList = 0;
		for (LinkedList<BasicDBObject> bucket: standaloneEventAggregator.listBuckets) {
			
			if (null != bucket) {
				for (BasicDBObject dbo: bucket) {
					if (standaloneEventAggregator.bSimulateAggregation) {
						dbo = new BasicDBObject(dbo);
						dbo.remove(AssociationPojo.entity1_);
						dbo.remove(AssociationPojo.entity2_);				
						dbo.remove(AssociationPojo.verb_);
						dbo.remove(AssociationPojo.time_start_);
						dbo.remove(AssociationPojo.time_end_);
						dbo.remove(AssociationPojo.geo_sig_);
					} //TESTED

					standaloneEventList.add(dbo);					
					nAddedToReturnList++;
					if (nAddedToReturnList >= nMaxToReturn) {
						return;
					}
				}
			}
		}
		
	}//TESTED
	
	/////////////////////////////////////////////////////////////
	
	//3. Integrate with API/GUI (also have max assoc sig for aggregated facts/events)
	// Phase 0 == promoted docs
	// Phase 1 == un-promoted docs
	// Phase 2 == never-promoted docs
	
	static void addStandaloneEvents(BasicDBObject doc, double dDocSig, int nPhase,
										StandaloneEventHashAggregator standaloneEventAggregator,
										boolean bEntTypeFilterPositive, boolean bAssocVerbFilterPositive,
										HashSet<String> entTypeFilter, HashSet<String> assocVerbFilter,
										boolean bEvents, boolean bSummaries, boolean bFacts)
	{				
		if (standaloneEventAggregator.bSimulateAggregation) {
			bSummaries = false;
		}
		String sDocIsoPubDate = null;
	
		BasicDBList lev = (BasicDBList)(doc.get(DocumentPojo.associations_));
		if (null != lev) 
		{
			for(Iterator<?> e0 = lev.iterator(); e0.hasNext();)
			{
				BasicDBObject e = (BasicDBObject)e0.next();
				
				String sEvType = e.getString(AssociationPojo.assoc_type_);
				boolean bIsFact = false;
				boolean bIsSummary = false;
				boolean bKeep = true;
				if (null == sEvType) {
					bKeep = false;
				}
				else if (sEvType.equalsIgnoreCase("event")) {
					if (!bEvents) bKeep = false;
				}
				else if (sEvType.equalsIgnoreCase("fact")) {
					if (!bFacts) bKeep = false;
					bIsFact = true;
				}
				else if (sEvType.equalsIgnoreCase("summary")) {
					if (!bSummaries) bKeep = false;
					bIsSummary = true;
				}//TESTED x4
				
				// Verb filter
				if (null != assocVerbFilter) {
					if (bAssocVerbFilterPositive) {
						if (!assocVerbFilter.contains(e.getString(AssociationPojo.verb_category_))) {
							bKeep = false;
						}						
					}
					else if (assocVerbFilter.contains(e.getString(AssociationPojo.verb_category_))) {
						bKeep = false;
					}
				}//TESTED

				if ((null != entTypeFilter) && bKeep) {
					String entIndex = e.getString(AssociationPojo.entity1_index_);
					if (null != entIndex) {
						String entType = null; 
						int nIndex = entIndex.lastIndexOf('/');
						if (nIndex >= 0) {
							entType = entIndex.substring(nIndex + 1);
						}
						if (bEntTypeFilterPositive) {
							if ((null != entType) && (!entTypeFilter.contains(entType))) {
								e0.remove();
								bKeep = false;
							}
						}
						else if ((null != entType) && (entTypeFilter.contains(entType))) {
							e0.remove();
							bKeep = false;
						}
						//TESTED
						
					}//(end if ent1_index exists)
					if (bKeep) { // same for ent index 2
						entIndex = e.getString(AssociationPojo.entity2_index_);
						if (null != entIndex) {
							String entType = null; 
							int nIndex = entIndex.lastIndexOf('/');
							if (nIndex >= 0) {
								entType = entIndex.substring(nIndex + 1);
							}
							if (bEntTypeFilterPositive) {
								if ((null != entType) && (!entTypeFilter.contains(entType))) {
									e0.remove();
									bKeep = false;
								}
							}
							else if ((null != entType) && (entTypeFilter.contains(entType))) {
								e0.remove();
								bKeep = false;
							}
						}//(end if ent2_index exists)
					}
				}//(end entity filter logic for associations)
				//TESTED
				
				if (bKeep) 
				{
					String time_start = null;
					String time_end = null; // (normally not needed)
					
					if (!standaloneEventAggregator.bSimulateAggregation) { //else times are discarded						
						// Add time from document
						time_start = e.getString(AssociationPojo.time_start_);
	
						if (null == time_start) 
						{
							if (null == sDocIsoPubDate) {
								// Convert docu pub date to ISO (day granularity):
								Date pubDate = (Date) doc.get(DocumentPojo.publishedDate_);
								
								if (null != pubDate) {
									SimpleDateFormat f2 = new SimpleDateFormat("yyyy-MM-dd");
									time_start = f2.format(pubDate);
								}
							}
							else {
								time_start = sDocIsoPubDate; // (so it doesn't get added again below)
							}
						}//TESTED					
						else 
						{ // Remove hourly granularity for consistency						
							time_start = time_start.replaceAll("T.*$", "");
							time_end = e.getString(AssociationPojo.time_end_);
							
							if (null != time_end) {
								time_end = time_end.replaceAll("T.*$", "");
							}
						}//TESTED (with debug code, eg time_start = "1997-07-16T19:20:30+01:00")
						if (null != time_start) 
						{ // Ensure it has day granularity, to help with aggregation
							e.put(AssociationPojo.time_start_, time_start);
							if (null != time_end) {
								e.put(AssociationPojo.time_end_, time_end);							
							}
						}//TESTED
					}//(end if normal standalone mode, not aggregation simulation)
					
					StandaloneEventHashCode evtHolder = new StandaloneEventHashCode(standaloneEventAggregator.bSimulateAggregation, e, bIsSummary, bIsFact);
					BasicDBObject oldEvt = standaloneEventAggregator.store.get(evtHolder);

					if (null == oldEvt) {
						// Doc count (see below)
						e.put(AssociationPojo.doccount_, 1);
						double dAssocSig = dDocSig*dDocSig;
						
						// Weight down summaries slightly (80%), and summaries with missing entities a lot (50%)  
						if (bIsSummary) {
							String sEntity2 = (String) e.get(AssociationPojo.entity2_);
							if (null == sEntity2) {
								dAssocSig *= 0.50;							
							}
							else {
								dAssocSig *= 0.80;								
							}
						}
						
						// Running significance count:
						e.put(AssociationPojo.assoc_sig_, dAssocSig); // (use sum-squared to score up events that occur frequently)
						if (dAssocSig > standaloneEventAggregator.dMaxSig) {
							standaloneEventAggregator.dMaxSig = dAssocSig;
						}
						
						standaloneEventAggregator.store.put(evtHolder, e);
						
						// Add to list in some sort of very basic order...
						if (2 == nPhase) { // Put at the back, it's probably really low sig
							standaloneEventAggregator.tmpList.add(e);
						}
						else if (1 == nPhase) { // Put at the front until Phase 0 comes along
							standaloneEventAggregator.tmpList.addFirst(e);							
							standaloneEventAggregator.nPhase1Events++;
						}
						else { // phases 0 and 1 get the higher orderings
							standaloneEventAggregator.tmpList.addFirst(e);
							standaloneEventAggregator.nPhase0Events++;
						}
					}
					else { // Update doc count
						long nDocCount = oldEvt.getInt(AssociationPojo.doccount_, 1) + 1;
						oldEvt.put(AssociationPojo.doccount_, nDocCount);
						// Running significance count:
						double dAssocSig = oldEvt.getDouble(AssociationPojo.doccount_) + dDocSig*dDocSig;
						oldEvt.put(AssociationPojo.assoc_sig_, dAssocSig);
						if (dAssocSig/nDocCount > standaloneEventAggregator.dMaxSig) {
							standaloneEventAggregator.dMaxSig = dAssocSig;							
						}
						
						if (bIsFact && !standaloneEventAggregator.bSimulateAggregation)
						{
							// For facts, also update the time range:
							String old_time_start = oldEvt.getString(AssociationPojo.time_start_);
							String old_time_end = oldEvt.getString(AssociationPojo.time_end_);
							// Just keep this really simple and inefficient:
							TreeSet<String> timeOrder = new TreeSet<String>();
							if (null != old_time_start) {
								timeOrder.add(old_time_start);
							}
							if (null != old_time_end) {
								timeOrder.add(old_time_end);
							}
							if (null != time_start) {
								timeOrder.add(time_start);
							}
							if (null != time_end) {
								timeOrder.add(time_end);
							}
							if (timeOrder.size() > 1) {
								Iterator<String> itStart = timeOrder.iterator();
								oldEvt.put(AssociationPojo.time_start_, itStart.next());
								Iterator<String> itEnd = timeOrder.descendingIterator();
								oldEvt.put(AssociationPojo.time_end_, itEnd.next());
							}
							
						}// end if is fact - treat times different
					}					
					//TESTED
					
				} // (end if keeping this event)
			} // (end loop over events)	
		} // (end if this doc has events)
		
	} //TESTED 

/////////////////////////////////////////////////////////////////////////////////////////////////////////////	

// ADDITIONAL FUNCTIONALITY #2: SIMPLE ASSOCIATION SCORING
	
	// Grab the standalone significances of the entities and also calc their pythag distance
	// (weight geo down since it's a secondary value)
	
	final private static double ASSOC_SCALE = 0.707106781; //(1/sqrt(2)
	
	static void calcAssocationSignificance(String ent1_index, String ent2_index, String geo_index, BasicDBObject assoc, HashMap<String, EntSigHolder> entitySet)
	{
		double dPythag = 0.0;
		if (null != ent1_index) {
			EntSigHolder ent = entitySet.get(ent1_index);
			if (null != ent) {
				if (null != ent.masterAliasSH) { // (for the 3 indexes, use the aliased version if it exists)
					ent = ent.masterAliasSH;
				}
				assoc.put(AssociationPojo.entity1_sig_, ent.datasetSignificance);
				dPythag += ent.datasetSignificance*ent.datasetSignificance;
			}
		}
		if (null != ent2_index) {
			EntSigHolder ent = entitySet.get(ent2_index);
			if (null != ent) {
				if (null != ent.masterAliasSH) {
					ent = ent.masterAliasSH;
				}
				assoc.put(AssociationPojo.entity2_sig_, ent.datasetSignificance);
				dPythag += ent.datasetSignificance*ent.datasetSignificance;
			}
		}
		if (null != geo_index) {			
			EntSigHolder ent = entitySet.get(geo_index);
			if (null != ent) {
				if (null != ent.masterAliasSH) {
					ent = ent.masterAliasSH;
				}
				assoc.put(AssociationPojo.geo_sig_, ent.datasetSignificance);
				dPythag += 0.25*ent.datasetSignificance*ent.datasetSignificance;
			}
		}
		if (0.0 != dPythag) {
			dPythag = Math.sqrt(dPythag);
		}
		assoc.put(AssociationPojo.assoc_sig_, ASSOC_SCALE*dPythag);
	}//TESTED
	
}
