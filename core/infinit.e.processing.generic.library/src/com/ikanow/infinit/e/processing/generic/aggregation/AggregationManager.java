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
package com.ikanow.infinit.e.processing.generic.aggregation;

import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.infinit.e.data_model.store.document.EntityPojo;
import com.ikanow.infinit.e.data_model.store.document.AssociationPojo;
import com.ikanow.infinit.e.data_model.store.feature.association.AssociationFeaturePojo;
import com.ikanow.infinit.e.data_model.store.feature.entity.EntityFeaturePojo;
import com.ikanow.infinit.e.processing.generic.utils.PropertiesManager;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MapReduceCommand.OutputType;
import com.mongodb.MapReduceOutput;

public class AggregationManager {

	private static final Logger logger = Logger.getLogger(AggregationManager.class);

	public void setIncrementalMode(boolean mode) {
		EntityAggregationUtils.setIncrementalMode(mode);
		AssociationAggregationUtils.setIncrementalMode(mode);
		CommunityFeatureCaches.setIncrementalMode(mode);
	}
	
	private static boolean _diagnosticMode = false;
	private static boolean _logInDiagnosticMode = true;
	public static void setDiagnosticMode(boolean bMode) {
		EntityAggregationUtils.setDiagnosticMode(bMode);
		AssociationAggregationUtils.setDiagnosticMode(bMode);
		_diagnosticMode = bMode;
	}
	public static void setLogInDiagnosticMode(boolean bLog) {
		EntityAggregationUtils.setLogInDiagnosticMode(bLog);
		AssociationAggregationUtils.setLogInDiagnosticMode(bLog);
		_logInDiagnosticMode = bLog;
	}
	
	public AggregationManager() {
		PropertiesManager props = new PropertiesManager();
		double dDutyCycle = props.getHarvestAggregationDutyCycle();
		if (dDutyCycle > 0.0) { // Do most of the aggregation in a separate thread
			_bBackgroundAggregationEnabled = true;
		}
	}
	private boolean _bBackgroundAggregationEnabled = false; 

	public void reset() {
		_aggregatedEntities = new TreeMap<String, Map<ObjectId, EntityFeaturePojo>>();
		_aggregatedEvents = new TreeMap<String, Map<ObjectId, AssociationFeaturePojo>>();
	}
	
	/////////////////////////////////////////////////////////////////////////////////////////	
	/////////////////////////////////////////////////////////////////////////////////////////
	
	private Map<String, Map<ObjectId, EntityFeaturePojo>> _aggregatedEntities = new TreeMap<String, Map<ObjectId, EntityFeaturePojo>>();
	private Map<String, Map<ObjectId, AssociationFeaturePojo>> _aggregatedEvents = new TreeMap<String, Map<ObjectId, AssociationFeaturePojo>>();
		// (Note that the aggregation manager is invoked per source so normally the inner map will just have one thing in it)
		// (making it a TreeMap should improve the working set size in MongoDB)
	
	/////////////////////////////////////////////////////////////////////////////////////////	
	/////////////////////////////////////////////////////////////////////////////////////////
	
	public void doAggregation(List<DocumentPojo> docsToAdd, List<DocumentPojo> docsToDelete) {
		
		// 1a. Get aggregated entities and events from documents (addition)
		
		for (DocumentPojo doc: docsToAdd) 
		{					
			Set<String> intraDocStore = new HashSet<String>();
			//(to avoid double-counting things like "obama/quote/blah1" and "obama/quote/blah2")
			
			Set<String> deletedEntities = null; // (need this to ensure associations are also deleted)
			
			if (null != doc.getEntities())
			{
				Iterator<EntityPojo> entIt = doc.getEntities().iterator();
				while (entIt.hasNext())  {
					EntityPojo ent = entIt.next();
					if (ent.getAlreadyProcessedInternally()) { // (for incremental mode processing)
						continue;
					}//TESTED (by hand)
					
					// Some QA checking:
					if ((null == ent.getIndex()) || (null == ent.getDisambiguatedName()) || (null == ent.getType())
							|| (null == ent.getDimension()) ||  (ent.getIndex().length() > 250))
					{
						if (null != ent.getIndex()) {
							if (null == deletedEntities) {
								deletedEntities = new HashSet<String>();
							}
							deletedEntities.add(ent.getIndex());
						}//TESTED
						
						entIt.remove(); // malformed / Entities can't be >500B (ie 250 16b characters)
						continue;
					}//TESTED
					
					if (null == ent.getFrequency()) { // can recover
						ent.setFrequency(1L);
					}

					Map<ObjectId, EntityFeaturePojo> entityInCommunity = _aggregatedEntities.get(ent.getIndex());
					if (null == entityInCommunity) {
						entityInCommunity = new HashMap<ObjectId, EntityFeaturePojo>();
						_aggregatedEntities.put(ent.getIndex(), entityInCommunity);
						
					}
					ObjectId communityId = doc.getCommunityId();
					if (null != communityId) 
					{
						EntityFeaturePojo feature = entityInCommunity.get(communityId);
						if (null == feature) 
						{
							feature = new EntityFeaturePojo();
							feature.setCommunityId(communityId);
							feature.setDimension(ent.getDimension());
							feature.setDisambiguatedName(ent.getDisambiguatedName());
							feature.setType(ent.getType());
							feature.addAlias(ent.getDisambiguatedName());							
							entityInCommunity.put(feature.getCommunityId(), feature);
						}
						if ( feature.getGeotag() == null )  {
							feature.setGeotag(ent.getGeotag());
							feature.setOntology_type(ent.getOntology_type());
						}
						if (null != ent.getSemanticLinks()) {
							feature.addToSemanticLinks(ent.getSemanticLinks());
						}
						feature.addAlias(ent.getActual_name());
						feature.setDoccount(feature.getDoccount() + 1);
						feature.setTotalfreq(feature.getTotalfreq() + ent.getFrequency());
					}
				}
			}//TESTED
			if (null != doc.getAssociations()) {
				Iterator<AssociationPojo> evtIt = doc.getAssociations().iterator();
				while (evtIt.hasNext())  {	
					AssociationPojo evt = evtIt.next();
					if (evt.getAlreadyProcessedInternally()) { // (for incremental mode processing)
						continue;
					}//TESTED (by hand)

					if (null != deletedEntities) { // check we're not using these entities in our associations
						if (null != evt.getEntity1_index() && deletedEntities.contains(evt.getEntity1_index())) {
							evtIt.remove();
							continue;
						}//TESTED (cut and paste from tested code below)
						if (null != evt.getEntity2_index() && deletedEntities.contains(evt.getEntity2_index())) {
							evtIt.remove();
							continue;
						}//TESTED
						if (null != evt.getGeo_index() && deletedEntities.contains(evt.getGeo_index())) {
							evt.setGeo_index(null);
						}//TESTED (trivial)
						
					}//TESTED
					
					boolean bAlreadyCountedFreq = false;					
					if ((null == evt.getEntity1_index()) && (null == evt.getEntity2_index())) {//skip this event if there is no ent1/en2
						continue;
					}
					// Calc index (this is not remotely unique, of course, but good enough for now...):
					String sEventFeatureIndex = AssociationAggregationUtils.getEventFeatureIndex(evt);
					evt.setIndex(sEventFeatureIndex); //(temp save for applyAggregationToDocs below)
					
					// Use index:
					Map<ObjectId, AssociationFeaturePojo> eventInCommunity = _aggregatedEvents.get(sEventFeatureIndex);
					if (null == eventInCommunity) {
						eventInCommunity = new HashMap<ObjectId, AssociationFeaturePojo>();
						_aggregatedEvents.put(sEventFeatureIndex, eventInCommunity);
						intraDocStore.add(sEventFeatureIndex);
					}
					else if (intraDocStore.contains(sEventFeatureIndex)) { 
						bAlreadyCountedFreq = true;
					}
					else {
						intraDocStore.add(sEventFeatureIndex);
					}
					ObjectId communityId = doc.getCommunityId();
					if (null != communityId) {
						AssociationFeaturePojo feature = eventInCommunity.get(communityId);
						if (null == feature) {
							feature = new AssociationFeaturePojo();
							feature.setCommunityId(communityId);
							feature.setIndex(sEventFeatureIndex);
							feature.setEntity1_index(evt.getEntity1_index());
							feature.setEntity2_index(evt.getEntity2_index());
							feature.setVerb_category(evt.getVerb_category());
							feature.setAssociation_type(evt.getAssociation_type());
							feature.setGeo_index(evt.getGeo_index());
							eventInCommunity.put(feature.getCommunityId(), feature);
						}
						if (!bAlreadyCountedFreq) {
							feature.setDoccount(feature.getDoccount() + 1);
						}
						if (null != evt.getEntity1_index()) {
							feature.addEntity1(evt.getEntity1_index());
						}
						if (null != evt.getEntity2_index()) {
							feature.addEntity2(evt.getEntity2_index());
						}
						if (null != evt.getVerb()) {
							feature.addVerb(evt.getVerb());
						}
						if (null != evt.getEntity1()) { 
							// Restrict length of entity string, in case it's a quotation
							if (evt.getEntity1().length() > AssociationFeaturePojo.entity_MAXSIZE) {
								int i = AssociationFeaturePojo.entity_MAXSIZE;
								for (; i > AssociationFeaturePojo.entity_MAXSIZE - 10; --i) {
									char c = evt.getEntity1().charAt(i);
									if (c < 0x30) {
										break;
									}
								}
								feature.addEntity1(evt.getEntity1().substring(0, i+1));
							}
							else {
								feature.addEntity1(evt.getEntity1());
							}//TESTED (both clauses, 2.1.4.3a)
						}
						if (null != evt.getEntity2()) {
							// Restrict length of entity string, in case it's a quotation
							if (evt.getEntity2().length() > AssociationFeaturePojo.entity_MAXSIZE) {
								int i = AssociationFeaturePojo.entity_MAXSIZE;
								for (; i > AssociationFeaturePojo.entity_MAXSIZE - 10; --i) {
									char c = evt.getEntity2().charAt(i);
									if (c < 0x30) {
										break;
									}
								}
								feature.addEntity2(evt.getEntity2().substring(0, i+1));
							}
							else {
								feature.addEntity2(evt.getEntity2());
							}//TESTED (both clauses, 2.1.4.3a)
						}
					}
				}//(end loop over associations)				
			}//TESTED
		}
		// 1b. Get aggregated entities and events from documents (removal)
		
		// (This is slightly expensive as would need to get all the entities for all the documents to be deleted,
		//  and there currently isn't a big requirement ... for now, wrap this up instead in the weekly resyncs that occur
		//  and will revisit if a use-case appears)
		
	}//TESTED
	
	/////////////////////////////////////////////////////////////////////////////////////////
	
	// Have no recalculated all the entity/events statistics ... apply them to the current set of documents
	
	// (Really, for performance, this should happen as the list is being created, ie splitting createOrUpdateFeatures
	//  into a findOrCreateFeature and then an updateFeature - it's a bit messy though, so will live with this for the 
	//  moment)
	
	public void applyAggregationToDocs(List<DocumentPojo> docsToAdd) {
		
		for (DocumentPojo doc: docsToAdd) {
			
			if (null != doc.getEntities()) for (EntityPojo ent: doc.getEntities()) {
				if (ent.getAlreadyProcessedInternally()) {
					continue;
				}//TESTED (by hand)
				
				Map<ObjectId, EntityFeaturePojo> entityInCommunity = _aggregatedEntities.get(ent.getIndex());
				if ((null != entityInCommunity) && !entityInCommunity.isEmpty()) { // (should always be true)
					EntityFeaturePojo entityFeature = null;
					if (1 == entityInCommunity.size()) {
						entityFeature = entityInCommunity.values().iterator().next();				
					}
					else {						
						entityFeature = entityInCommunity.get(doc.getCommunityId()); 
					}
					ent.setDoccount(entityFeature.getDoccount());
					ent.setTotalfrequency(entityFeature.getTotalfreq());
					
					//CURRENTLY UNUSED, LEFT HERE IN CASE IT BECOMES NECESSARY AGAIN
					// (these will be null except in the complex federated query case)
					//ent.setDatasetSignificance(entityFeature.getDatasetSignficance());
					//ent.setSignificance(entityFeature.getDatasetSignficance());
					//ent.setQueryCoverage(entityFeature.getQueryCoverage());
				}					
			}// (End loop over entities)
			
			//TODO (INF-1276): The listed INF has a workaround for this, need to decide whether to implement
			// a full solution along the lines of the code below.
			//Don't currently support doc-count in events in documents (needed for event significance)
//			if (null != doc.getEvents()) for (EventPojo evt: doc.getEvents()) {
//				if (null != evt.getIndex()) { // (else not a very interesting event) 
//					
//					Map<ObjectId, EventFeaturePojo> eventInCommunity = _aggregatedEvents.get(evt.getIndex());
//					if ((null != eventInCommunity) && !eventInCommunity.isEmpty()) { // (should always be true)
//						EventFeaturePojo evtFeature = null;			
//						if (1 == eventInCommunity.size()) {
//							evtFeature = eventInCommunity.values().iterator().next();				
//						}
//						else {						
//							evtFeature = eventInCommunity.get(new ObjectId(doc.getCommunityId())); 
//						}			
//						// Set event doccount:
//						evt.setDoccount(evtFeature.getDoccount());
//					}
//					
//				} // (If an interesting event)
//			}// (End loop over events)
			
		}// (End loop over docs)
	} //TESTED
	
	/////////////////////////////////////////////////////////////////////////////////////////
	
	// This serves the dual function of filling in the database (creating a new entry if necessary)
	// And also copying relevant existing info (doc counts, sync information) into the objects in the 
	// _aggregatedXXX collections.
	
	public void createOrUpdateFeatureEntries() {
		
		// 2a. Create/update feature entries for entities
		
		EntityAggregationUtils.updateEntityFeatures(_aggregatedEntities);
		
		// 2b. Create/update feature entries for events
		
		AssociationAggregationUtils.updateEventFeatures(_aggregatedEvents);
		
	}//TESTED
	
	/////////////////////////////////////////////////////////////////////////////////////////
	
	// (This needs to happen last because it "corrupts" the entities and events)
	
	public void runScheduledSynchronization() {
		if (_bBackgroundAggregationEnabled) {
			//Used to also synchronize (the indexes of) first-time entities, but don't do that any more
			return;
		}
		
		// 3a. Check entity schedule for doc/feature updates and index synchronization
		
		long nStartTime, nEndTime;	
		nStartTime = System.currentTimeMillis();
		for (Map<ObjectId, EntityFeaturePojo> entCommunity: _aggregatedEntities.values()) {
			
			for (Map.Entry<ObjectId, EntityFeaturePojo> entFeature: entCommunity.entrySet()) {
				
				try {				
					boolean bSync = false;
					if (!_bBackgroundAggregationEnabled) { // Otherwise this occurs in BackgroundAggregationManager thread
						bSync = doScheduleCheck(Schedule.SYNC_INDEX, entFeature.getValue().getIndex(), entFeature.getValue().getDoccount(), 
												entFeature.getValue().getDbSyncDoccount(), entFeature.getValue().getDbSyncTime());
					}
					//Used to also synchronize (the indexes of) first-time entities, but don't do that any more
	
					if (bSync) {
						EntityAggregationUtils.synchronizeEntityFeature(entFeature.getValue(), entFeature.getKey());
					}
				}
				catch (Exception e) { } // Likely that es can't handle the characters in its _id (ie the index), just carry on
			}
		}
		nEndTime = System.currentTimeMillis();
		if ((nEndTime - nStartTime) > 60000) {
			logger.warn("Frequency update slow, time=" + (nEndTime - nStartTime)/1000 + " num_ents=" + _aggregatedEntities.size());
		}
		
		// 3b. Check event schedule for doc/feature updates and index synchronization		
		
		// (All association aggregation still happens inline)
		
		nStartTime = System.currentTimeMillis();					
		for (Map<ObjectId, AssociationFeaturePojo> evtCommunity: _aggregatedEvents.values()) {
			
			for (Map.Entry<ObjectId, AssociationFeaturePojo> evtFeature: evtCommunity.entrySet()) {				
				boolean bSync = false;
				if (!_bBackgroundAggregationEnabled) { // Otherwise this occurs in BackgroundAggregationManager thread
					bSync = doScheduleCheck(Schedule.SYNC_INDEX, evtFeature.getValue().getIndex(), evtFeature.getValue().getDoccount(), 
										evtFeature.getValue().getDb_sync_doccount(), evtFeature.getValue().getDb_sync_time());
				}
				//Used to also synchronize (the indexes of) first-time entities, but don't do that any more
				
				if (bSync) {
					AssociationAggregationUtils.synchronizeEventFeature(evtFeature.getValue(), evtFeature.getKey());
				}
			}
		}
		nEndTime = System.currentTimeMillis();
		if ((nEndTime - nStartTime) > 60000) {
			logger.warn("Frequency update slow, time=" + (nEndTime - nStartTime)/1000 + " num_evts=" + _aggregatedEvents.size());
		}
		
	}//TESTED 
	
	/////////////////////////////////////////////////////////////////////////////////////////
	
	public void runScheduledDocumentUpdates() {
		
		// 3a. Check entity schedule for doc/feature updates and index synchronization
		
		for (Map<ObjectId, EntityFeaturePojo> entCommunity: _aggregatedEntities.values()) {
			for (EntityFeaturePojo entFeature: entCommunity.values()) {
				if (doScheduleCheck(Schedule.UPDATE_DOCS, entFeature.getIndex(), entFeature.getDoccount(), 
									entFeature.getDbSyncDoccount(), entFeature.getDbSyncTime()))
				{
					if (!_bBackgroundAggregationEnabled) { // Otherwise this occurs in BackgroundAggregationManager thread
						
						EntityAggregationUtils.updateMatchingEntities(entFeature, entFeature.getCommunityId());
					}
					else { // Background aggregation mode, mark the entity feature for the bg thread
						EntityAggregationUtils.markEntityFeatureForSync(entFeature, entFeature.getCommunityId());
					}//TESTED
				}
			}
		}
		
		// 3b. Check event schedule for doc/feature updates and index synchronization		

		// (All association aggregation still happens inline)
		
		for (Map<ObjectId, AssociationFeaturePojo> evtCommunity: _aggregatedEvents.values()) {
			for (AssociationFeaturePojo evtFeature: evtCommunity.values()) {
				
				if (doScheduleCheck(Schedule.UPDATE_DOCS, evtFeature.getIndex(), evtFeature.getDoccount(), 
										evtFeature.getDb_sync_doccount(), evtFeature.getDb_sync_time()))
				{
					if (!_bBackgroundAggregationEnabled) { // Otherwise this occurs in BackgroundAggregationManager thread
						
						AssociationAggregationUtils.updateMatchingEvents(evtFeature, evtFeature.getCommunityId());
							// (note, currently does nothing - not needed until we start trying to calc signficance for events)
					}
					else { // Background aggregation mode, mark the assoc feature for the bg thread
						AssociationAggregationUtils.markAssociationFeatureForSync(evtFeature, evtFeature.getCommunityId());
					}//TOEST					
				}
			}
		}
		
	}//TESTED 
	
	/////////////////////////////////////////////////////////////////////////////////////////	
	/////////////////////////////////////////////////////////////////////////////////////////
	
	// UTILITIES
	
	public enum Schedule { UPDATE_DOCS, SYNC_INDEX };
	
	private static boolean doScheduleCheck(Schedule type,
									String featureIndex, long nDocCount, 
									long nDbSyncDocCount, String dbSyncTime) 
	{
		
		long nCurrTime = System.currentTimeMillis();
		boolean bUpdateDocs = false;
				
		if (0 == nDbSyncDocCount) { // (never sync'd with DB since new code added)
			bUpdateDocs = true;
		}
		else if (nDbSyncDocCount != nDocCount) { // (otherwise, already sync'd)
			// Always re-sync on a >10% change
			if ((0 == nDbSyncDocCount) || (((100*nDocCount)/nDbSyncDocCount) >= 110)) {
				bUpdateDocs = true;						
			}
			else if (((100*nDocCount)/nDbSyncDocCount) > 101){ 
				// Smaller change, just update periodically, but never both if not more than 1%)
				
				long nDbSyncTime = (dbSyncTime != null) ? Long.parseLong(dbSyncTime) : 0;
				
				int nHoursBetweenUpdate;
				if (nDocCount < 100) { // (update less common entities more frequently)
					nHoursBetweenUpdate = 4;
				}
				else {							
					nHoursBetweenUpdate = 12;
				}
				long nHoursDiff = 3600L*1000L*nHoursBetweenUpdate;
				if ((nCurrTime - nDbSyncTime) > nHoursDiff) {
					// Spread out the entities across hours between update
					int nHashCode = featureIndex.hashCode() % nHoursBetweenUpdate;
					int nHour = (int)(nCurrTime/(3600L*1000L) % nHoursBetweenUpdate);
					if (nHashCode == nHour) {
						bUpdateDocs = true;																					
					}
				}
				else {
				}
			}		
		}
		if (_diagnosticMode) {
			if (_logInDiagnosticMode) System.out.println("AggregationManager.doScheduleCheck("+type.name()+"): "+featureIndex + 
					" dc="+nDocCount + " lastdc="+nDbSyncDocCount + " last="+dbSyncTime + ": " + "index="+((null == dbSyncTime)||bUpdateDocs) + " docs="+bUpdateDocs);
		}
		// Note currently always sync both or neither:
		if (Schedule.UPDATE_DOCS == type) {
			return bUpdateDocs;
		}
		else { // Schedule.SYNC_INDEX == type
			// Only difference, always update new features
			return (null == dbSyncTime) || bUpdateDocs;
		}
		
	} //TESTED (by eye - cut and paste from working BETA code)
	
	///////////////////////////////////////////////////////////////////////////////////////	
	///////////////////////////////////////////////////////////////////////////////////////	

	// HANDLING DELETED DOCUMENTS
	
	///////////////////////////////////////////////////////////////////////////////////////	
	///////////////////////////////////////////////////////////////////////////////////////	

	public static boolean updateEntitiesFromDeletedDocuments(String uuid) 
	{		
		try {
			BasicDBObject mrQuery = new BasicDBObject(DocumentPojo.url_, uuid);
			BasicDBObject mrFields = new BasicDBObject(DocumentPojo.url_, 1); // (ie will just hit the index, not the datastore)
			if (null == DbManager.getDocument().getMetadata().findOne(mrQuery, mrFields)) {
				return false;
			}//TESTED
			
			PropertiesManager props = new PropertiesManager();
			if (props.getAggregationDisabled()) { // (no need to do this)
				return true;
			}		
			// Load string resource
			
			InputStream in = EntityAggregationUtils.class.getResourceAsStream("AggregationUtils_scriptlets.xml");
			
			// Get XML elements for the script
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(in);
			NodeList nList = doc.getElementsByTagName("script");
			
			Map<String, String> scriptMap = new HashMap<String, String>();
			
			for (int temp = 0; temp < nList.getLength(); temp++) {			 
				Node nNode = nList.item(temp);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {				   
					Element eElement = (Element) nNode;	
					String name = getTagValue("name", eElement);
					String value = getTagValue("value", eElement);
					if ((null != name) && (null != value)) {
						scriptMap.put(name, value);
					}
				}
			}	
			String mapScript = scriptMap.get("common_mapper");
			String reduceScript = scriptMap.get("common_reducer");
			
			// Perform the map reduce (count deleted entities and associations)
	
			String outCollection = new StringBuilder(uuid).append("_AggregationUtils").toString();
				
			@SuppressWarnings("unused")
			MapReduceOutput res = 
				DbManager.getDocument().getMetadata().mapReduce(mapScript, reduceScript, outCollection, OutputType.REPLACE, mrQuery);

		}
		catch (Exception e) { // (These should never be runtime failures, all I/O is vs files embedded at compile-time)
			//DEBUG
			//e.printStackTrace();
			return true;
		}
		return true;
	}	
	//TESTED (including scriptlets in AggregationUtils_scriptlets)
	
	public static void updateDocEntitiesFromDeletedDocuments(String uuid) 
	{
		String outCollection = new StringBuilder(uuid).append("_AggregationUtils").toString();
		try {			
			PropertiesManager props = new PropertiesManager();
			if (props.getAggregationDisabled()) { // (no need to do this)
				return;
			}
			DBCollection outColl = DbManager.getCollection("doc_metadata", outCollection);
			
			DBCursor dbc = outColl.find();
			for (DBObject dbo: dbc) {
				BasicDBObject entityEl = (BasicDBObject) dbo;
				BasicDBObject entityVal = (BasicDBObject) entityEl.get("value");
				
				long nDocDecrement = entityVal.getLong("dc");
				long nFreqDecrement = entityVal.getLong("f");
				long nCurrFreq = entityVal.getLong("tf");
				long nCurrDocCount = entityVal.getLong("tdc");
				
				// (These are by construction the lowest values so this will provide some defence against going -ve)
				if (nDocDecrement > nCurrDocCount) {
					nDocDecrement = nCurrDocCount;
				}
				if (nFreqDecrement > nCurrFreq) {
					nFreqDecrement = nCurrFreq;
				}
				
				BasicDBObject entityId = (BasicDBObject) entityEl.get("_id");
				ObjectId commId = null;
				Object commObj = entityId.get("comm");
				if (commObj instanceof ObjectId) {
					commId = entityId.getObjectId("comm");
				}
				String index = (String) entityId.get("index");
				if ((null == index) || (null == commId)) {
					continue; // random error
				}
				
				BasicDBObject updateQuery = new BasicDBObject(EntityFeaturePojo.index_, index);
				updateQuery.put(EntityFeaturePojo.communityId_, commId);
				BasicDBObject entityUpdate1 = new BasicDBObject(EntityFeaturePojo.doccount_, -nDocDecrement);
				entityUpdate1.put(EntityFeaturePojo.totalfreq_, -nFreqDecrement);
				BasicDBObject entityUpdate = new BasicDBObject(DbManager.inc_, entityUpdate1);
				
				if (_diagnosticMode) {
					if (_logInDiagnosticMode) System.out.println("UPDATE FEATURE DATABASE: " + updateQuery.toString() + "/" + entityUpdate.toString());
				}
				else {
					DbManager.getFeature().getEntity().update(updateQuery, entityUpdate);
						// (can be a single query because the query is on index, the shard)
				}
				//TESTED
				
				if ((nDocDecrement < nCurrDocCount) && (nDocDecrement*10 > nCurrDocCount)) {
					// ie there are some documents left
					// and the doc count has shifted by more than 10%
					BasicDBObject updateQuery2 = new BasicDBObject(EntityPojo.docQuery_index_, index);
					updateQuery2.put(DocumentPojo.communityId_, commId);
					BasicDBObject entityUpdate2_1 = new BasicDBObject(EntityPojo.docUpdate_doccount_, nCurrDocCount - nDocDecrement);
					entityUpdate2_1.put(EntityPojo.docUpdate_totalfrequency_, nCurrFreq - nFreqDecrement);
					BasicDBObject entityUpdate2 = new BasicDBObject(DbManager.set_, entityUpdate2_1);
	
					if (_diagnosticMode) {
						if (_logInDiagnosticMode) System.out.println("UPDATE DOC DATABASE: " + updateQuery2.toString() + "/" + entityUpdate2.toString());
					}
					else {
						DbManager.getDocument().getMetadata().update(updateQuery2, entityUpdate2, false, true);						
					}
				}
			}//TESTED (including when to update logic above)
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		// Tidy up
		DbManager.getDB("doc_metadata").getCollection(outCollection).drop();
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////

	// Utility

	private static String getTagValue(String sTag, Element eElement) {
		NodeList nlList = eElement.getElementsByTagName(sTag).item(0).getChildNodes();
		int listLength = nlList.getLength();		
		
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < listLength; ++i) {
			Node nValue = (Node) nlList.item(i);
			if (null != nValue) {
				sb.append(nValue.getNodeValue());
			}			
		}
		return sb.toString();
	}	
}
