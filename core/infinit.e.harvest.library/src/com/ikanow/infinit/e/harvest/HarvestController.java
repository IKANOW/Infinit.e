/**
 * 
 */
package com.ikanow.infinit.e.harvest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;

import org.apache.log4j.Logger;
import org.bson.types.ObjectId;

import com.ikanow.infinit.e.data_model.InfiniteEnums;
import com.ikanow.infinit.e.data_model.InfiniteEnums.ExtractorDocumentLevelException;
import com.ikanow.infinit.e.data_model.InfiniteEnums.ExtractorSourceLevelException;
import com.ikanow.infinit.e.data_model.InfiniteEnums.ExtractorDailyLimitExceededException;
import com.ikanow.infinit.e.data_model.InfiniteEnums.ExtractorSourceLevelMajorException;
import com.ikanow.infinit.e.data_model.InfiniteEnums.ExtractorSourceLevelTransientException;
import com.ikanow.infinit.e.data_model.InfiniteEnums.HarvestEnum;
import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.config.source.SourceHarvestStatusPojo;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.infinit.e.data_model.store.document.EntityPojo;
import com.ikanow.infinit.e.data_model.utils.GeoOntologyMapping;
import com.ikanow.infinit.e.harvest.enrichment.custom.StructuredAnalysisHarvester;
import com.ikanow.infinit.e.harvest.enrichment.custom.UnstructuredAnalysisHarvester;
import com.ikanow.infinit.e.harvest.enrichment.legacy.EntityExtractorEnum;
import com.ikanow.infinit.e.harvest.enrichment.legacy.IEntityExtractor;
import com.ikanow.infinit.e.harvest.enrichment.legacy.alchemyapi.ExtractorAlchemyAPI;
import com.ikanow.infinit.e.harvest.enrichment.legacy.opencalais.ExtractorOpenCalais;
import com.ikanow.infinit.e.harvest.extraction.document.DuplicateManager;
import com.ikanow.infinit.e.harvest.extraction.document.DuplicateManager_Integrated;
import com.ikanow.infinit.e.harvest.extraction.document.DuplicateManager_Standalone;
import com.ikanow.infinit.e.harvest.extraction.document.HarvestStatus;
import com.ikanow.infinit.e.harvest.extraction.document.HarvestStatus_Integrated;
import com.ikanow.infinit.e.harvest.extraction.document.HarvestStatus_Standalone;
import com.ikanow.infinit.e.harvest.extraction.document.HarvesterInterface;
import com.ikanow.infinit.e.harvest.extraction.document.database.DatabaseHarvester;
import com.ikanow.infinit.e.harvest.extraction.document.file.FileHarvester;
import com.ikanow.infinit.e.harvest.extraction.document.rss.FeedHarvester;
import com.ikanow.infinit.e.harvest.extraction.text.boilerpipe.TextExtractorBoilerpipe;
import com.ikanow.infinit.e.harvest.extraction.text.legacy.ITextExtractor;
import com.ikanow.infinit.e.harvest.utils.HarvestExceptionUtils;
import com.ikanow.infinit.e.harvest.utils.PropertiesManager;
import com.mongodb.BasicDBObject;

/**
 * @author cmorgan
 *
 * Used to process all incoming sources in the system
 * @param <DimensionPojo>
 */
public class HarvestController implements HarvestContext
{
	private PropertiesManager pm = new PropertiesManager();
	private IEntityExtractor default_entity_extractor = null;
	private ITextExtractor default_text_extractor = null;
	private ArrayList<HarvesterInterface> harvesters = new ArrayList<HarvesterInterface>();
	private static Set<String> urlsThatError = new TreeSet<String>();
	private static final Logger logger = Logger.getLogger(HarvestController.class);
	
	private HashMap<String, IEntityExtractor> entity_extractor_mappings = null;
	private HashMap<String, ITextExtractor> text_extractor_mappings = null;
	
	private int _nMaxDocs = Integer.MAX_VALUE; 
	private DuplicateManager _duplicateManager = new DuplicateManager_Integrated();
	private HarvestStatus _harvestStatus = new HarvestStatus_Integrated(); // (can either be standalone or integrated, defaults to standalone)
	public DuplicateManager getDuplicateManager() { return _duplicateManager; }
	public HarvestStatus getHarvestStatus() { return _harvestStatus; }
	public void setStandaloneMode(int nMaxDocs) {
		if (nMaxDocs > 0) {
			_nMaxDocs = nMaxDocs;
		}
		_duplicateManager = new DuplicateManager_Standalone();
		_harvestStatus = new HarvestStatus_Standalone();
	}
	private long nBetweenFeedDocs_ms = 10000; // (default 10s)
	
	//statistics variables
	private static AtomicInteger num_sources_harvested = new AtomicInteger(0);
	private static AtomicInteger num_docs_extracted = new AtomicInteger(0);
	private static AtomicInteger num_errors_source = new AtomicInteger(0);
	private static AtomicInteger num_error_url = new AtomicInteger(0);
	private static AtomicInteger num_ent_extracted = new AtomicInteger(0);
	private static AtomicInteger num_event_extracted = new AtomicInteger(0);
	
	private int nUrlErrorsThisSource = 0;
	
	/**
	 * Used to find out the sources harvest of information is successful
	 * @return
	 */
	public boolean isSuccessful() {
		return true;
	}

	// Handle clean shutdown of harvester
	private static boolean bIsKilled = false;
	public static void killHarvester() { bIsKilled = true; }
	public static boolean isHarvestKilled() { return bIsKilled; }

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	// TOP LEVEL LOGICAL
	
	// Utility objects for loading custom text and entity extractors across all threads just once
	@SuppressWarnings("rawtypes")
	private static HashMap<String, Class> customExtractors = null; 
	private static ClassLoader customExtractorClassLoader = HarvestController.class.getClassLoader();
	
	/**
	 *  Constructor for Harvest Controller class
	 *  
	 * @throws IOException 
	 */
	@SuppressWarnings("rawtypes")
	public HarvestController() throws IOException 
	{
		PropertiesManager props = new PropertiesManager();
		String sTypes = props.getHarvesterTypes();
		String sType[] = sTypes.split("\\s*,\\s*");
		
		// Add a harvester for each data type
		for (String s: sType) {
			if (s.equalsIgnoreCase("database")) {
				try {
					this.harvesters.add(new DatabaseHarvester());
				}
				catch (Exception e) {
					logger.error(s + " not supported: " + e.getMessage());
				}					
			}
			else if (s.equalsIgnoreCase("file")) {
				try {
					this.harvesters.add(new FileHarvester());
				}
				catch (Exception e) {
					logger.error(s + " not supported: " + e.getMessage());
				}
			} 
			else if (s.equalsIgnoreCase("feed")) {
				try {
					this.harvesters.add(new FeedHarvester());
				}
				catch (Exception e) {
					logger.error(s + " not supported: " + e.getMessage());
				}
			} 
		}
		
		// Load all the extractors, set up defaults
		entity_extractor_mappings = new HashMap<String, IEntityExtractor>();
		text_extractor_mappings = new HashMap<String, ITextExtractor>();
				
		// Load custom text/entity extractors
		synchronized (HarvestController.class) {
			if (null == customExtractors) {
				customExtractors = new HashMap<String, Class>();
				customExtractorClassLoader = HarvestController.class.getClassLoader();
			}
			// Text extractors:
			String customTextList = props.getCustomTextExtractors();
			if (null != customTextList) {
				String customTextArray[] = customTextList.split("\\s*,\\s*");
				for (String customText: customTextArray) {
					if (!customExtractors.containsKey(customText)) {
						// (else already have this extractor)
						try {
							Class customTextExtractor = customExtractorClassLoader.loadClass(customText);
							ITextExtractor obj = (ITextExtractor)customTextExtractor.newInstance(); 
							text_extractor_mappings.put(obj.getName().toLowerCase(), obj);
							customExtractors.put(customText, customTextExtractor);
						}
						catch (Exception e) {
							logger.error("ITextExtractor: Couldn't load " + customText +": " + e.getMessage(), e);
						}
					}				
					else { // Already loaded, put in again
						try {
							Class customTextExtractor = customExtractors.get(customText);	
							ITextExtractor obj = (ITextExtractor)customTextExtractor.newInstance(); 
							text_extractor_mappings.put(obj.getName().toLowerCase(), obj);						
						}
						catch (Exception e) {
							logger.error("ITextExtractor: Couldn't use already loaded " + customText +": " + e.getMessage(), e);
						}
					}
				}
			}//TESTED
			// Entity extractors 
			String customEntityList = props.getCustomEntityExtractors();
			if (null != customEntityList) {
				String customEntityArray[] = customEntityList.split("\\s*,\\s*");
				for (String customEntity: customEntityArray) {
					if (!customExtractors.containsKey(customEntity)) {
						// (else already have this extractor - but may have it for text, so some work to do)
						try {
							Class customEntityExtractor = customExtractorClassLoader.loadClass(customEntity);
							IEntityExtractor obj = (IEntityExtractor)customEntityExtractor.newInstance(); 
							entity_extractor_mappings.put(obj.getName().toLowerCase(), obj);
							customExtractors.put(customEntity, customEntityExtractor);
						}
						catch (Exception e) {
							logger.error("IEntityExtractor: Couldn't load " + customEntity +": " + e.getMessage(), e);
						}
					}
					else { // If this object exists and if it's a text extractor, then see if it's also an entity extractor
						try {
							Class customEntityExtractor = customExtractors.get(customEntity);						
							IEntityExtractor obj = (IEntityExtractor)customEntityExtractor.newInstance(); 
							entity_extractor_mappings.put(obj.getName(), obj);
						}
						catch (Exception e) { 
							logger.error("IEntityExtractor: Couldn't use already loaded " + customEntity +": " + e.getMessage(), e);						
						}
					}
				}
			}//TESTED
		}
		
		try {
			entity_extractor_mappings.put("opencalais", new ExtractorOpenCalais());
		}
		catch (Exception e) {
			logger.warn("Can't use OpenCalais as entity extractor: " + e.getMessage());			
		}
		
		try {
			ExtractorAlchemyAPI both = new ExtractorAlchemyAPI();
			entity_extractor_mappings.put("alchemyapi", both);
			text_extractor_mappings.put("alchemyapi", both);		
		}
		catch (Exception e) {
			logger.warn("Can't use AlchemyAPI as entity/text extractor: " + e.getMessage());			
		}
		try {
			text_extractor_mappings.put("boilerpipe", new TextExtractorBoilerpipe());
		}
		catch (Exception e) {
			logger.warn("Can't use Boilerpipe as text extractor: " + e.getMessage());			
		}
		
		if (null != pm.getDefaultEntityExtractor()) {
			default_entity_extractor = entity_extractor_mappings.get(pm.getDefaultEntityExtractor().toLowerCase());
		}
		else {
			try {
				default_entity_extractor = new ExtractorOpenCalais();
			}
			catch (Exception e) {
				logger.warn("Can't use OpenCalais as default entity extractor: " + e.getMessage());
			}
		}
		if (null != pm.getDefaultTextExtractor()) {
			default_text_extractor = text_extractor_mappings.get(pm.getDefaultTextExtractor().toLowerCase());
		}
		else {
			try {
				default_text_extractor = new ExtractorAlchemyAPI();			
			}
			catch (Exception e) {
				logger.warn("Can't use AlchemyAPI as default text extractor: " + e.getMessage());
			}
		}
		nBetweenFeedDocs_ms = props.getWebCrawlWaitTime();
	}
	
	/**
	 * Handles going through what to do with a source for harvesting
	 * The process currently is:
	 * 1. Extract from source
	 * 2. Enrich with metadata from toAdd (entity, fulltext, events, etc)
	 * 
	 * @param source The source to harvest
	 */
	public void harvestSource(SourcePojo source, List<DocumentPojo> toAdd, List<DocumentPojo> toUpdate, List<DocumentPojo> toRemove)
	{
		nUrlErrorsThisSource = 0;
		
		// Can override the default (feed) wait time from within the source (eg for sites that we know 
		// don't get upset about getting hammered)
		if (null != source.getRssConfig()) {
			if (null != source.getRssConfig().getWaitTimeOverride_ms()) {
				nBetweenFeedDocs_ms = source.getRssConfig().getWaitTimeOverride_ms();
			}
		}
		LinkedList<DocumentPojo> toDuplicate = new LinkedList<DocumentPojo>(); 
		
		// Reset any state that might have been generated from the previous source
		getDuplicateManager().resetForNewSource();
		
		//First up, Source Extraction (could spawn off some threads to do source extraction)
		// Updates will be treated as follows:
		// - extract etc etc (since they have changed)
		// [and then in generic processing
		// - remove them (including their child objects, eg events) ...
		//   ... - but retain "created" date (and in the future artefacts like comments)]
		extractSource(source, toAdd, toUpdate, toRemove, toDuplicate);
			// (^^^ this adds toUpdate to toAdd) 
		
		enrichSource(source, toAdd, toUpdate, toRemove);
		
		// (Now we've completed enrichment either normally or by cloning, add the dups back to the normal documents for generic processing)
		LinkedList<DocumentPojo> groupedDups = new LinkedList<DocumentPojo>(); // (ie clones)
		DocumentPojo masterDoc = null; // (just looking for simple pointer matching here)

		for (DocumentPojo dupDoc: toDuplicate) {
			if (null == dupDoc.getCloneFrom()) {
				toAdd.add(dupDoc);				
			}
			else if (null != dupDoc.getCloneFrom().getTempSource()) { //(Else doc was removed from toAdd list due to extraction errors) 
				if (null == masterDoc) { // First time through
					masterDoc = dupDoc.getCloneFrom();
				}
				else if (!masterDoc.getUrl().equals(dupDoc.getUrl())) { // New group!
					groupedDups = enrichDocByCloning(groupedDups);
					if (null != groupedDups) {
						toAdd.addAll(groupedDups);
						groupedDups.clear();
					}
					else {
						groupedDups = new LinkedList<DocumentPojo>();
					}
					masterDoc = dupDoc.getCloneFrom();
				}
				groupedDups.add(dupDoc);					
			}
		}//end loop over duplicates
		//TESTED, included case where the master doc errors during extraction (by good fortune!) 
		
		if (null != groupedDups) { // (Leftover group)
			groupedDups = enrichDocByCloning(groupedDups);
			if (null != groupedDups) {
				toAdd.addAll(groupedDups);
			}			
		}//TESTED (as above)
	}
	/**
	 * Figures out what source extractors to use and then fills the toAdd list
	 * with DocumentPojo objects from the extractors. 
	 * 
	 * @param flags The source extractors to use
	 * @param start source to start extracting at
	 * @param end source to stop extracting at
	 * @param toAdd A reference to the toAdd that should be filled with what the source extracts
	 */
	@SuppressWarnings("unchecked")
	private void extractSource(SourcePojo source, List<DocumentPojo> toAdd, List<DocumentPojo> toUpdate, List<DocumentPojo> toRemove, List<DocumentPojo> toDup)
	{
		//determine which source extractor to use
		for ( HarvesterInterface harvester : harvesters)
		{
			if ( harvester.canHarvestType(InfiniteEnums.castExtractType(source.getExtractType())) )
			{
				try {
					List<DocumentPojo> tmpToAdd = new LinkedList<DocumentPojo>();
					List<DocumentPojo> tmpToUpdate = new LinkedList<DocumentPojo>();
					List<DocumentPojo> tmpToRemove = new LinkedList<DocumentPojo>();
					harvester.executeHarvest(this, source, tmpToAdd, tmpToUpdate, tmpToRemove);
					
					int nDocs = 0;
					for (List<DocumentPojo> docList: Arrays.asList(tmpToAdd, tmpToUpdate)) {
						for (DocumentPojo doc : docList) {
							if (++nDocs > _nMaxDocs) {
								break;
							}
							// Handle cloning on "duplicate docs" from different sources
							boolean bDuplicated = false; 
							if (null != doc.getDuplicateFrom()) {
								DocumentPojo newDoc = enrichDocByDuplicating(doc);
									// (Note this is compatible with the cloning case whose logic is below:
									//  this document gets fully populated here then added to dup list (with dupFrom==null), with a set of slaves
									//  with dupFrom==sourceKey#comm. When the dup list is traversed (after bypassing enrichment), the slaves are
									//	then created from this master)
								if (null != newDoc) {
									doc = newDoc;
									bDuplicated = true;
								}
							}
							// Copy over material from source pojo:
							doc.setSource(source.getTitle());
							doc.setTempSource(source);
							doc.setMediaType(source.getMediaType());
							doc.setSourceType(source.getExtractType());
							doc.setTags(source.getTags());
							ObjectId sCommunityId = source.getCommunityIds().iterator().next(); // (multiple communities handled below) 
							String sIndex = new StringBuffer("doc_").append(sCommunityId.toString()).toString();
							doc.setCommunityId(sCommunityId);								
							doc.setIndex(sIndex);
							if (1 == source.getCommunityIds().size()) { // Normal case
								doc.setSourceKey(source.getKey());
							}
							else { // Many communities for a single source, not a pleasant case
								String sMasterDocSourceKey = null;
								for (ObjectId id: source.getCommunityIds()) {
									if (null == sMasterDocSourceKey) {
										// Will process this document as normal, just update its source key
										sMasterDocSourceKey = new StringBuffer(source.getKey()).append('#').append(id).toString(); 
										doc.setSourceKey(sMasterDocSourceKey);
									}
									else { // Will defer these until after the master doc has been added to the database
										DocumentPojo cloneDoc = new DocumentPojo();

										// Will need these fields
										cloneDoc.setIndex(new StringBuffer("doc_").append(id).toString());
										cloneDoc.setCommunityId(id); 
										cloneDoc.setSourceKey(source.getKey()); // (will be overwritten with the correct <key>#<id> composite later)
										cloneDoc.setSource(source.getTitle());
										cloneDoc.setUrl(doc.getUrl());
										cloneDoc.setTags(source.getTags());
										
										cloneDoc.setCloneFrom(doc);
										toDup.add(cloneDoc);
									}
								}//TESTED (both in clone and clone+duplicate)
							}							
							// Normally add to enrichment list (for duplicates, bypass this)
							if (bDuplicated) {
								toDup.add(doc); // (Already enriched by duplication process)
							}
							else {
								toAdd.add(doc);
							}
						}
					}//(end loop over docs to add/update)
					
					num_docs_extracted.addAndGet(tmpToAdd.size() > _nMaxDocs ? _nMaxDocs : tmpToAdd.size());
					toUpdate.addAll(tmpToUpdate);
					toRemove.addAll(tmpToRemove);
				}
				catch (Exception e) {
					
					e.printStackTrace();
					logger.error("Error extracting source=" + source.getKey() + ", type=" + source.getExtractType() + ", reason=" + e.getMessage());					
					_harvestStatus.update(source, new Date(), HarvestEnum.error, "Extraction error: " + e.getMessage(), false, false);					
				}
				break; //exit for loop, source is extracted
			}
		}
	}

	// 
	// Gets metadata using the extractors and appends to documents
	//
	
	private void enrichSource(SourcePojo source, List<DocumentPojo> toAdd, List<DocumentPojo> toUpdate, List<DocumentPojo> toRemove)
	{
		StructuredAnalysisHarvester sah = null;
		UnstructuredAnalysisHarvester usah = null;
		
		// Create metadata from the text using regex (also calculate header/footer information if desired)
		if (source.getUnstructuredAnalysisConfig() != null)
		{
			usah = new UnstructuredAnalysisHarvester();
			
			// If performing structured analysis also then need to mux them
			// since the UAH will run on the body/description potentially created by the SAH
			// and the SAH will take the metadata generated by UAH to create entities and events
			if (source.getStructuredAnalysisConfig() != null) {
				sah = new StructuredAnalysisHarvester();
				sah.addUnstructuredHandler(usah);
			}
			else {
				toAdd = usah.executeHarvest(this, source, toAdd);
			}
		}
				
		// For sources that generate structured data, we can turn that into entities and events
		// and fill in document fields from the metadata (that can be used by entity extraction)
		if (source.getStructuredAnalysisConfig() != null)
		{
			if (null == sah) {
				sah = new StructuredAnalysisHarvester();
			}
			toAdd = sah.executeHarvest(this, source, toAdd);
				// (if usah exists then this runs usah)
			
			// Get doc entity and event count
			getFeedEntityAndEventCount(toAdd);
		}
		
		// Perform text and entity extraction
		if ((null == source.useExtractor()) || (!source.useExtractor().equalsIgnoreCase("none")))
		{
			// Text/Entity Extraction (can spawn off some threads)
			try {
				extractTextAndEntities(toAdd, source);
			}
			catch (Exception e) {
				handleExtractError(e, source); //handle extractor error if need be				
			}
		}
		
		//Attempt to map entity types to set of ontology types
		//eventually the plan is to allow extractors to set the ontology_type of
		//entities to anything found in the opencyc ontology
		mapEntityToOntology(toAdd);
		
		// Log the number of feeds extracted for the current source
		if ((toAdd.size() > 0) || (toUpdate.size() > 0) || (toRemove.size() > 0) || (nUrlErrorsThisSource > 0)) {
			StringBuffer sLog = new StringBuffer("source=").append((null==source.getUrl()?source.getKey():source.getUrl())).
									append(" extracted=").append(toAdd.size()).append(" updated=").append(toUpdate.size()).
									append(" deleted=").append(toRemove.size()).append(" urlerrors=").append(nUrlErrorsThisSource);
			
			logger.info(sLog.toString());
			getHarvestStatus().logMessage(sLog.toString(), false);
		}//TOTEST
		
		// May need to update status again:
		if (getHarvestStatus().moreToLog()) {
			SourceHarvestStatusPojo prevStatus = source.getHarvestStatus();
			if (null != prevStatus) { // (just for robustness, should never happen)
				getHarvestStatus().update(source, new Date(), 
						source.getHarvestStatus().getHarvest_status(), prevStatus.getHarvest_message(), false, false);
					// (if we've got this far, the source can't have been so bad we were going to disable it...)
			}
		}
		
		num_sources_harvested.incrementAndGet();
	}
		
	/**
	 * Takes a list of toAdd and extracts each ones full text and entities/events/sentiment (metadata)
	 * 
	 * @param toAdd The list of toAdd without metadata to extract on
	 * @return Any errors that occured while extracting, null if no error
	 * @throws ExtractorSourceLevelTransientException 
	 */
	private void extractTextAndEntities(List<DocumentPojo> toAdd, SourcePojo source)
		throws ExtractorDocumentLevelException, ExtractorSourceLevelException, 
				ExtractorDailyLimitExceededException, ExtractorSourceLevelMajorException, ExtractorSourceLevelTransientException
	{
		try {
			int error_on_feed_count = 0, feed_count = 0;
			
		// EXTRACTOR SELECTION LOGIC
			
			IEntityExtractor currentEntityExtractor = null;
			if (null != source.useExtractor()) {
				currentEntityExtractor = entity_extractor_mappings.get(source.useExtractor().toLowerCase());
			}
			if (currentEntityExtractor == null) // none specified or didn't find it (<-latter is error)
			{
				if (null != source.useExtractor()) { // ie specified one but it doesn't exist....
					StringBuffer errMsg = new StringBuffer("Skipping source=").append(source.getKey()).append(" no_extractor=").append(source.useExtractor());
					logger.warn(errMsg.toString());
					
					// No point trying this for the rest of the day
					throw new ExtractorSourceLevelException(errMsg.toString());					
				}
				else { // Didn't specify one, just use default:
					currentEntityExtractor = default_entity_extractor;
				}
			}//TESTED					
			
			// A teeny bit of complex logic:
			// toAdd by default use a text extractor
			// DB/Files by default don't (but can override)
	
			ITextExtractor currentTextExtractor = null;
			boolean bUseRawContentWhereAvailable = false; // (only applies for feeds)
			if (null != source.useTextExtractor()) {
				currentTextExtractor = text_extractor_mappings.get(source.useTextExtractor().toLowerCase());
			}
			if (null == currentTextExtractor) { // none specified or didn't find it (<-latter is error)
				if (null != source.useTextExtractor()) {					
					if ((null == source.getStructuredAnalysisConfig()) && (null == source.getUnstructuredAnalysisConfig())) {
						//(UAH and SAH get raw access to the data if they need it, so can carry on)
					
						StringBuffer errMsg = new StringBuffer("Skipping source=").append(source.getKey()).append(" no_txt_extractor=").append(source.useTextExtractor());
						logger.warn(errMsg.toString());
						
						// No point trying this for the rest of the day
						throw new ExtractorSourceLevelException(errMsg.toString());
					}
					else {
						bUseRawContentWhereAvailable = true; // (only checked for feeds)						
					}//TESTED
				}
				else if (source.getExtractType().equalsIgnoreCase("feed")) // (DB/files just use their existing fullText) 
				{
					if (null != currentEntityExtractor) {
						String selfExtraction = currentEntityExtractor.getCapability(EntityExtractorEnum.URLTextExtraction); 
						// Leave as null unless have no built-in capability
						if ((null == selfExtraction) || !selfExtraction.equals("true"))
						{
							currentTextExtractor = default_text_extractor;
						}
					}
					else {
						currentTextExtractor = default_text_extractor;						
					}
				}//TESTED		
			}
			
		// EXTRACTION
			Iterator<DocumentPojo> i = toAdd.iterator(); //iterator created so that elements in the toAdd list can be 
													// removed within the loop
			while ( i.hasNext() )
			{
				long nTime_ms = System.currentTimeMillis();
				DocumentPojo doc = i.next();
				doc.setTempSource(source); // (so the harvesters have access to doc.getTempSource)
				boolean bExtractedText = false;
			
				// If I've been stopped then just remove all remaining documents
				// (pick them up next time through)
				if (bIsKilled) {
					i.remove();
					continue;
				}
				
				if ( !urlsThatError.contains(doc.getUrl()) ) //only attempt if url is okay
				{				
					feed_count++;
					
					try {
						if (null != currentTextExtractor)
						{					
							bExtractedText = true;
							currentTextExtractor.extractText(doc);
							if (null != currentEntityExtractor) {
								currentEntityExtractor.extractEntities(doc);
							}
	
						}//TESTED
						else //db/filesys should already have full text extracted (unless otherwise specified)
						{
							if (source.getExtractType().equalsIgnoreCase("feed")) { // Need full text so get from current
								if ((null == doc.getFullText()) || !bUseRawContentWhereAvailable) {
									bExtractedText = true;
									if (null != currentEntityExtractor) {
										currentEntityExtractor.extractEntitiesAndText(doc);
									}
								}//TESTED (AlchemyAPI case)
								else { // Feed for which we've already extracted data
									if (null != currentEntityExtractor) {
										currentEntityExtractor.extractEntities(doc);
									}
								}//TESTED
							}
							else { // DB/File => use full text
								if (null != currentEntityExtractor) {
									currentEntityExtractor.extractEntities(doc);
								}
							}//TESTED
						}
						
						//statistics counting
						if ( doc.getEntities() != null )
							num_ent_extracted.addAndGet(doc.getEntities().size());
						if ( doc.getAssociations() != null )
							num_event_extracted.addAndGet(doc.getAssociations().size());
						
					}
					catch (ExtractorDailyLimitExceededException e) {
						
						//extractor can't do anything else today, return
						i.remove();
						doc.setTempSource(null); // (can safely corrupt this doc since it's been removed)
						
						throw e; // (ie stop processing this source)
					}//TESTED
					catch (Exception e) { // Anything except daily limit exceeded, expect it to be ExtractorDocumentLevelException
						
						// This can come from (sort-of/increasingly) "user" code so provide a bit more information
						StringBuffer errMessage = HarvestExceptionUtils.createExceptionMessage(e);
						_harvestStatus.logMessage(errMessage.toString(), true);
						
						num_error_url.incrementAndGet();
						nUrlErrorsThisSource++;
						
						error_on_feed_count++;
						i.remove();
						doc.setTempSource(null); // (can safely corrupt this doc since it's been removed)
						urlsThatError.add(doc.getUrl());						
					}
					//TESTED
				}
				if ((null != source.getExtractType()) && (source.getExtractType().equalsIgnoreCase("feed"))) {
					if (i.hasNext() && bExtractedText) {
						nTime_ms = nBetweenFeedDocs_ms - (System.currentTimeMillis() - nTime_ms); // (ie delay time - processing time)
						if (nTime_ms > 0) {
							try { Thread.sleep(nTime_ms); } catch (Exception e) {}; 
								// (wait 10s between web-site accesses for politeness)
						}
					}
				}//(TESTED)
				
			} // end loop over documents	
			//check if all toAdd were erroring, or more than 20 (arbitrary number)
			if ((error_on_feed_count == feed_count) && (feed_count > 5))
			{
				String errorMsg = new StringBuffer().append(feed_count).append(" docs, ").append(error_on_feed_count).append(", errors").toString(); 
				if (error_on_feed_count > 20) {
					throw new ExtractorSourceLevelMajorException(errorMsg);
				}
				else {
					throw new ExtractorSourceLevelException(errorMsg);
				}//TESTED
			}
		}
		catch (ExtractorDailyLimitExceededException e) {
			// Percolate upwards!
			throw e;
		}
		catch (ExtractorSourceLevelException e) {
			// Percolate upwards!
			throw e;
		}
		catch (ExtractorSourceLevelMajorException e) {
			// Percolate upwards!
			throw e;
		}
		catch (Exception e) { // Misc internal error
			
			StringBuffer errMsg = new StringBuffer("Skipping source=").append(source.getKey()).append(" error=").append(e.getMessage());
			logger.error(errMsg.toString());
			throw new ExtractorSourceLevelTransientException(errMsg.toString());
		}//TESTED
		
	}//TOTEST (exception handling extensions)
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	// UTILITY FUNCTIONS
	
	/**
	 * Decides what to do with a source when an error is returned from the
	 * extractor process.
	 * 
	 * @param error The error that was returned from extractor
	 * @param source The source that the extractor was working on
	 */
	private void handleExtractError(Exception error, SourcePojo source)
	{
		if ( null != error)
		{
			if ( error instanceof ExtractorSourceLevelException)
			{
				num_errors_source.incrementAndGet();
				//We flag the source in mongo and temp disable
				_harvestStatus.update(source, new Date(), HarvestEnum.error, "Source Level extraction error: " + error.getMessage(), true, false);
			}//TESTED
			else if ( error instanceof ExtractorSourceLevelMajorException)
			{
				num_errors_source.incrementAndGet();
				//We flag the source in mongo and perma disable
				_harvestStatus.update(source, new Date(), HarvestEnum.error, "Major source level Extraction error: " + error.getMessage(), true, true);
			}//TESTED
			else if ( error instanceof ExtractorSourceLevelTransientException)
			{
				num_errors_source.incrementAndGet();
				//We flag the source in mongo
				_harvestStatus.update(source, new Date(), HarvestEnum.error, "Transient source level extraction error: " + error.getMessage(), false, false);				
			}//TESTED
			else if ( error instanceof ExtractorDailyLimitExceededException)
			{
				try 
				{				
					//Does using thread this way sleep current thread (if we are threading
					//the harvest process, may need to change this if it does global thread)
					Thread.sleep(60000); // (sleep for 10 minutes just to reduce the load on extractor a bit - increased val to reflect threading)
				}
				catch (Exception e) {}
			}//TESTED
		}
	}//TESTED (just that the instanceofs work)
	
	/**
	 * getFeedEntityAndEventCount
	 * Iterate over a list of DocumentPojos and count the number of entities and events
	 * @param toAdd
	 */
	private void getFeedEntityAndEventCount(List<DocumentPojo> toAdd)
	{
		for (DocumentPojo doc : toAdd)
		{
			if ( doc.getEntities() != null )
				num_ent_extracted.addAndGet(doc.getEntities().size());
			if ( doc.getAssociations() != null )
				num_event_extracted.addAndGet(doc.getAssociations().size());
		}
	}
	/**
	 * Prints out some quick info about how the harvester performed
	 */
	public static void logHarvesterStats()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("num_of_sources_harvested=" + num_sources_harvested.get());
		sb.append(" num_of_docs_extracted=" + num_docs_extracted.get());
		sb.append(" num_of_entities_extracted=" + num_ent_extracted.get());
		sb.append(" num_of_events_extracted=" + num_event_extracted.get());
		sb.append(" num_of_source_errors=" + num_errors_source.get());
		sb.append(" num_of_url_errors=" + num_error_url.get());
		logger.info(sb.toString());
	}
	
	// Utility to handle the various multiple community problems:
	// - Different sources, name URL ("duplicates") ... get the doc from the DB (it's there by definition)
	// - Same source, multiple communities ("clones") ... get the doc from the first community processed
	
	private static DocumentPojo enrichDocByDuplicating(DocumentPojo docToReplace) {
		DocumentPojo newDoc = null;
		BasicDBObject dbo = getDocumentMetadataFromWhichToDuplicate(docToReplace);
		if (null != dbo) {
			String sContent = getDocumentContentFromWhichToDuplicate(docToReplace);
			if (null != sContent) {
				newDoc = duplicateDocument(docToReplace, dbo, sContent, false);
					// (Note this erases the "duplicateFrom" field - this is important because it distinguishes "clones" and "duplicates")
			}
		}
		return newDoc;		
	}//TESTED
		
	private static LinkedList<DocumentPojo> enrichDocByCloning(List<DocumentPojo> docsToReplace) {
		DocumentPojo newDoc = null;
		BasicDBObject dbo = null;
		String sContent = null;
		LinkedList<DocumentPojo> newDocs = new LinkedList<DocumentPojo>(); 
		for (DocumentPojo docToReplace: docsToReplace) {
		
			if (null == dbo) { // First time through...
				sContent = docToReplace.getCloneFrom().getFullText();
				docToReplace.getCloneFrom().setFullText(null);
				dbo = (BasicDBObject) docToReplace.getCloneFrom().toDb();
				docToReplace.getCloneFrom().setFullText(sContent);
			}
			newDoc = duplicateDocument(docToReplace, dbo, sContent, true);
			newDocs.add(newDoc);
		}
		return newDocs;
		
	}//TESTED

	// Sub-utility
	
	private static BasicDBObject getDocumentMetadataFromWhichToDuplicate(DocumentPojo docToReplace) {
		BasicDBObject query = new BasicDBObject("url", docToReplace.getUrl());
		query.put("sourceKey", new BasicDBObject("$regex", new StringBuffer().append('^').append(docToReplace.getDuplicateFrom()).append("(#|$)").toString()));
			//(slight complication because searching for either <sourceKey> or <sourceKey>#<community>)
		BasicDBObject dbo = (BasicDBObject) DbManager.getDocument().getMetadata().findOne(query);
		
		return dbo;
	}//TESTED
	
	private static String getDocumentContentFromWhichToDuplicate(DocumentPojo docToReplace) {
		try {
			// Get the full text:
			byte[] storageArray = new byte[200000];
			BasicDBObject contentQ = new BasicDBObject("url", docToReplace.getUrl());
			BasicDBObject dboContent = (BasicDBObject) DbManager.getDocument().getContent().findOne(contentQ);
			if (null != dboContent) {
				byte[] compressedData = ((byte[])dboContent.get("gzip_content"));				
				ByteArrayInputStream in = new ByteArrayInputStream(compressedData);
				GZIPInputStream gzip = new GZIPInputStream(in);				
				int nRead = 0;
				StringBuffer output = new StringBuffer();
				while (nRead >= 0) {
					nRead = gzip.read(storageArray, 0, 200000);
					if (nRead > 0) {
						String s = new String(storageArray, 0, nRead, "UTF-8");
						output.append(s);
					}
				}
				return output.toString();
			}		
			else { // Will just need to-reprocess this document
				return null;
			}
		}
		catch (Exception e) {
			// Do nothing, just carry on
			e.printStackTrace();
		}		
		return null;
	}//TESTED
	
	private static DocumentPojo duplicateDocument(DocumentPojo docToReplace, BasicDBObject dbo, String content, boolean bClone) {
		DocumentPojo newDoc = DocumentPojo.fromDb(dbo, DocumentPojo.class);
		newDoc.setFullText(content);
		newDoc.setId(new ObjectId()); // (ie ensure it's unique)
		
		if (bClone) { // Cloned docs have special source key formats (and also need to update their community)
			ObjectId docCommunity = docToReplace.getCommunityId();
			newDoc.setSourceKey(new StringBuffer(docToReplace.getSourceKey()).append('#').append(docCommunity).toString());
			newDoc.setCommunityId(docCommunity);
			newDoc.setIndex(new StringBuffer("doc_").append(docCommunity).toString());			
		}		
		else { // For cloned documents, published etc can be taken from the master document, ie newDoc is already accurate
			// Copy over timing details from new document (set by the harvesters) 
			newDoc.setPublishedDate(docToReplace.getPublishedDate());
			newDoc.setCreated(docToReplace.getCreated());
			newDoc.setModified(docToReplace.getModified());			
		}
		return newDoc;
	}//TESTED
	
	static public void mapEntityToOntology(List<DocumentPojo> docs)
	{		
		if ( docs != null )
		{
			for ( DocumentPojo doc : docs )
			{
				if ( doc.getEntities() != null )
				{
					for ( EntityPojo entity : doc.getEntities() )
					{
						if ( entity.getGeotag() != null )
						{
							entity.setOntology_type(GeoOntologyMapping.mapEntityToOntology(entity.getType()));							
						}
					}
				}
			}
		}
	}
}
