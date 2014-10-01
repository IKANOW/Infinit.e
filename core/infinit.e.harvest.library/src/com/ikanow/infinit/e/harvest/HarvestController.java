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
/**
 * 
 */
package com.ikanow.infinit.e.harvest;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
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
import com.ikanow.infinit.e.data_model.interfaces.harvest.EntityExtractorEnum;
import com.ikanow.infinit.e.data_model.interfaces.harvest.IEntityExtractor;
import com.ikanow.infinit.e.data_model.interfaces.harvest.ITextExtractor;
import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.MongoDbManager;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.ikanow.infinit.e.data_model.store.document.CompressedFullTextPojo;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.infinit.e.data_model.store.document.EntityPojo;
import com.ikanow.infinit.e.data_model.store.social.sharing.SharePojo;
import com.ikanow.infinit.e.data_model.store.social.sharing.SharePojo.ShareCommunityPojo;
import com.ikanow.infinit.e.data_model.utils.GeoOntologyMapping;
import com.ikanow.infinit.e.data_model.utils.IkanowSecurityManager;
import com.ikanow.infinit.e.data_model.utils.TrustManagerManipulator;
import com.ikanow.infinit.e.harvest.enrichment.custom.StructuredAnalysisHarvester;
import com.ikanow.infinit.e.harvest.enrichment.custom.UnstructuredAnalysisHarvester;
import com.ikanow.infinit.e.harvest.enrichment.legacy.TextRankExtractor;
import com.ikanow.infinit.e.harvest.enrichment.legacy.alchemyapi.ExtractorAlchemyAPI;
import com.ikanow.infinit.e.harvest.enrichment.legacy.alchemyapi.ExtractorAlchemyAPI_Metadata;
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
import com.ikanow.infinit.e.harvest.extraction.document.logstash.LogstashHarvester;
import com.ikanow.infinit.e.harvest.extraction.document.rss.FeedHarvester;
import com.ikanow.infinit.e.harvest.extraction.text.boilerpipe.TextExtractorBoilerpipe;
import com.ikanow.infinit.e.harvest.extraction.text.legacy.TextExtractorTika;
import com.ikanow.infinit.e.harvest.utils.AuthUtils;
import com.ikanow.infinit.e.harvest.utils.HarvestExceptionUtils;
import com.ikanow.infinit.e.harvest.utils.PropertiesManager;
import com.mongodb.BasicDBObject;
import com.mongodb.gridfs.GridFSDBFile;

/**
 * @author cmorgan
 *
 * Used to process all incoming sources in the system
 * @param <DimensionPojo>
 */
public class HarvestController implements HarvestContext
{
	private HarvestControllerPipeline procPipeline = null;
	private IkanowSecurityManager _securityManager = null;
	public IkanowSecurityManager getSecurityManager() { return _securityManager; }
	
	private PropertiesManager pm = new PropertiesManager();
	private IEntityExtractor default_entity_extractor = null;
	private ITextExtractor default_text_extractor = null;
	private ArrayList<HarvesterInterface> harvesters = new ArrayList<HarvesterInterface>();
	private static Set<String> urlsThatError = new TreeSet<String>();
	private static final Logger logger = Logger.getLogger(HarvestController.class);

	private HashMap<String, IEntityExtractor> entity_extractor_mappings = null;
	private HashMap<String, ITextExtractor> text_extractor_mappings = null;
	private HashSet<String> failedDynamicExtractors = null;
	private static HashMap<String, Class<?> > dynamicExtractorClassCache = null;

	private int _nMaxDocs = Integer.MAX_VALUE; 
	private DuplicateManager _duplicateManager = new DuplicateManager_Integrated();
	private HarvestStatus _harvestStatus = new HarvestStatus_Integrated(); // (can either be standalone or integrated, defaults to standalone)
	public DuplicateManager getDuplicateManager() { return _duplicateManager; }
	public HarvestStatus getHarvestStatus() { return _harvestStatus; }
	boolean _bIsStandalone = false;
	public boolean isStandalone() { return _bIsStandalone; }
	public void setStandaloneMode(int nMaxDocs) {
		setStandaloneMode(nMaxDocs, false); // (by default don't dedup, however you may want to test updates)
	}
	public void setStandaloneMode(int nMaxDocs, boolean bRealDedup) {
		_bIsStandalone = true;
		urlsThatError.clear(); // (for api testing, obviously don't want to stop trying if we get an error)
		if (nMaxDocs >= 0) {
			_nMaxDocs = nMaxDocs;
		}
		if (!bRealDedup) {
			_duplicateManager = new DuplicateManager_Standalone();
		}
		_harvestStatus = new HarvestStatus_Standalone();

		if (null != dynamicExtractorClassCache) { // (standalone so don't cache extractors)
			dynamicExtractorClassCache.clear();
		}		
	}
	public int getStandaloneMaxDocs() {
		return _nMaxDocs;
	}
	private long nBetweenFeedDocs_ms = 10000; // (default 10s)

	//statistics variables
	private static AtomicInteger num_sources_harvested = new AtomicInteger(0);
	private static AtomicInteger num_docs_extracted = new AtomicInteger(0);
	private static AtomicInteger num_errors_source = new AtomicInteger(0);
	private static AtomicInteger num_error_url = new AtomicInteger(0);
	private static AtomicInteger num_error_px = new AtomicInteger(0);
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
	public HarvestController() throws IOException { this(false); }
	
	private static boolean _initializedSSL = false;
	
	@SuppressWarnings("rawtypes")
	public HarvestController(boolean overrideTypeSettings) throws IOException 
	{
		if (!_initializedSSL) {
			_initializedSSL = true;
			try {
				// Ensure we don't have any self-signed cert debacles:
				TrustManagerManipulator.allowAllSSL();		
			}
			finally {}
		}
		
		PropertiesManager props = new PropertiesManager();
		String sTypes = props.getHarvesterTypes();
		if (overrideTypeSettings) { // (override API settings in test mode)
			sTypes = "Feed,File,Database,Logstash";
		}
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
				catch(NoClassDefFoundError e) {
					logger.error(s + " not supported: " + e.getMessage());
				}				
			}
			else if (s.equalsIgnoreCase("logstash")) {
				try {
					this.harvesters.add(new LogstashHarvester());
				}
				catch (Exception e) {
					logger.error(s + " not supported: " + e.getMessage());
				}
				catch(NoClassDefFoundError e) {
					logger.error(s + " not supported: " + e.getMessage());
				}								
			}
			else if (s.equalsIgnoreCase("file")) {

				// According to http://www.ryanchapin.com/fv-b-4-648/java-lang-OutOfMemoryError--unable-to-create-new-native-thread-Exception-When-Using-SmbFileInputStream.html
				// this is needed to avoid java.lang.OutOfMemoryError (intermittent - for me at least, it's happened for exactly 1 source, but consistently when it does)
				System.setProperty("jcifs.resolveOrder", "DNS");
				System.setProperty("jcifs.smb.client.dfs.disabled", "true");

				try {
					this.harvesters.add(new FileHarvester());
				}
				catch (Exception e) {
					logger.error(s + " not supported: " + e.getMessage());
				}
				catch(NoClassDefFoundError e) {
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
				catch(NoClassDefFoundError e) {
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
						catch(NoClassDefFoundError e) {
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
						catch(NoClassDefFoundError e) {
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
						catch(NoClassDefFoundError e) {
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
						catch(NoClassDefFoundError e) {
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
			entity_extractor_mappings.put("textrank", new TextRankExtractor());
		}
		catch (Exception e) {
			logger.warn("Can't use textrank as entity extractor: " + e.getMessage());			
		}

		try {
			ExtractorAlchemyAPI both = new ExtractorAlchemyAPI();
			entity_extractor_mappings.put("alchemyapi", both);
			text_extractor_mappings.put("alchemyapi", both);	
			ExtractorAlchemyAPI_Metadata both_metadata = new ExtractorAlchemyAPI_Metadata();
			entity_extractor_mappings.put("alchemyapi-metadata", both_metadata);
			text_extractor_mappings.put("alchemyapi-metadata", both_metadata);				
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
		try {
			text_extractor_mappings.put("tika", new TextExtractorTika());
		}
		catch (Exception e) {
			logger.warn("Can't use Tika as text extractor: " + e.getMessage());			
		}

		if (null != pm.getDefaultEntityExtractor()) {
			default_entity_extractor = entity_extractor_mappings.get(pm.getDefaultEntityExtractor().toLowerCase());
		}
		else {
			default_entity_extractor = null;
		}
		if (null != pm.getDefaultTextExtractor()) {
			default_text_extractor = text_extractor_mappings.get(pm.getDefaultTextExtractor().toLowerCase());
		}
		else {
			try {
				default_text_extractor = new TextExtractorBoilerpipe();			
			}
			catch (Exception e) {
				logger.warn("Can't use BoilerPlate as default text extractor: " + e.getMessage());
			}
		}
		nBetweenFeedDocs_ms = props.getWebCrawlWaitTime();
		
		// Set up security manager - basically always needed so might as well create here
		
		_securityManager = new IkanowSecurityManager();								
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

		// New Harvest Pipeline logic
		if (null != source.getProcessingPipeline()) {
			if (null == procPipeline) {
				procPipeline = new HarvestControllerPipeline();
			}
			procPipeline.extractSource_preProcessingPipeline(source, this);
			//(just copy the config into the legacy source fields since the 
			// actual processing is the same in both cases)
		}//TESTED

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
		getHarvestStatus().resetForNewSource();

		//First up, Source Extraction (could spawn off some threads to do source extraction)
		// Updates will be treated as follows:
		// - extract etc etc (since they have changed)
		// [and then in generic processing
		// - remove them (including their child objects, eg events) ...
		//   ... - but retain "created" date (and in the future artefacts like comments)]
		extractSource(source, toAdd, toUpdate, toRemove, toDuplicate);
		// (^^^ this adds toUpdate to toAdd) 

		if (null != source.getProcessingPipeline()) {
			procPipeline.setInterDocDelayTime(nBetweenFeedDocs_ms);
			try {
				procPipeline.enrichSource_processingPipeline(source, toAdd, toUpdate, toRemove);
			}
			finally { // (ensure can clear memory)
				procPipeline.clearState();
			}
		}
		else { // Old logic (more complex, less functional)
			enrichSource(source, toAdd, toUpdate, toRemove);
		}
		completeEnrichmentProcess(source, toAdd, toUpdate, toRemove);

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
		boolean normalCase = true;
		normalCase = (1 == source.getCommunityIds().size()) || // (normal case..)
						((2 == source.getCommunityIds().size()) && source.getCommunityIds().contains(source.getOwnerId()));
							// (test case..)
		
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
							if (null != doc.getDuplicateFrom() && (null == doc.getUpdateId())) {
								DocumentPojo newDoc = enrichDocByDuplicating(doc);
								// (Note this is compatible with the cloning case whose logic is below:
								//  this document gets fully populated here then added to dup list (with dupFrom==null), with a set of slaves
								//  with dupFrom==sourceKey. When the dup list is traversed (after bypassing enrichment), the slaves are
								//	then created from this master)
								if (null != newDoc) {
									doc = newDoc;
									bDuplicated = true;
								}
							}
							else { // if the update id is non-null then ignore the above logic
								doc.setDuplicateFrom(null);
							}
							// Copy over material from source pojo:
							doc.setSource(source.getTitle());
							doc.setTempSource(source);
							doc.setMediaType(source.getMediaType());
							if ((null == source.getAppendTagsToDocs()) || source.getAppendTagsToDocs()) {
								doc.setTags(source.getTags());
							}
							ObjectId sCommunityId = source.getCommunityIds().iterator().next(); // (multiple communities handled below) 
							String sIndex = new StringBuffer("doc_").append(sCommunityId.toString()).toString();
							doc.setCommunityId(sCommunityId);								
							doc.setIndex(sIndex);
							if (normalCase) { // Normal case (or test case)
								doc.setSourceKey(source.getKey());
							}
							else { // Many communities for a single source, not a pleasant case
								String sMasterDocSourceKey = null;
								for (ObjectId id: source.getCommunityIds()) {
									if (null == sMasterDocSourceKey) {
										sMasterDocSourceKey = (source.getKey());
										doc.setSourceKey(sMasterDocSourceKey);
									}
									else { // Will defer these until after the master doc has been added to the database
										DocumentPojo cloneDoc = new DocumentPojo();

										// Will need these fields
										cloneDoc.setIndex(new StringBuffer("doc_").append(id).toString());
										cloneDoc.setCommunityId(id); 
										cloneDoc.setSourceKey(source.getKey()); 
										cloneDoc.setSource(source.getTitle());
										cloneDoc.setUrl(doc.getUrl());
										if ((null == source.getAppendTagsToDocs()) || source.getAppendTagsToDocs()) {
											cloneDoc.setTags(source.getTags());
										}

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

					//DEBUG
					//e.printStackTrace();
					logger.error("Error extracting source=" + source.getKey() + ", type=" + source.getExtractType() + ", reason=" + e.getMessage());					
					_harvestStatus.update(source, new Date(), HarvestEnum.error, "Extraction error: " + e.getMessage(), false, false);					
				}
				break; //exit for loop, source is extracted
			}
		}
	}

	// 
	// (LEGACY) Gets metadata using the extractors and appends to documents
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
		}

		// Perform text and entity extraction
		if (source.getStructuredAnalysisConfig() == null) // (Else is performed during SAH above)
		{
			if (isEntityExtractionRequired(source))
			{
				// Text/Entity Extraction
				try {
					extractTextAndEntities(toAdd, source, false, false);
				}
				catch (Exception e) {
					handleExtractError(e, source); //handle extractor error if need be				
				}
			}
		} // (end if no SAH)

		// Finish processing:
		// Complete batches
		if (isEntityExtractionRequired(source))
		{
			try {
				extractTextAndEntities(null, source, true, false);
			}
			catch (Exception e) {}
		}		
	}

	private void completeEnrichmentProcess(SourcePojo source, List<DocumentPojo> toAdd, List<DocumentPojo> toUpdate, List<DocumentPojo> toRemove)
	{
		// Map ontologies:

		completeDocumentBuilding(toAdd, toUpdate);

		int pxErrors = getHarvestStatus().getNumMessages();		
		num_error_px.addAndGet(pxErrors);
		
		// Log the number of feeds extracted for the current source
		if ((toAdd.size() > 0) || (toUpdate.size() > 0) || (toRemove.size() > 0) || (nUrlErrorsThisSource > 0) || (pxErrors > 0)) {
			StringBuffer sLog = new StringBuffer("source=").append((null==source.getUrl()?source.getKey():source.getUrl())).append(" ");
				// (only need this for the log, not the source harvest message)

			if ((null != source.getHarvestStatus()) && (null != source.getHarvestStatus().getHarvest_message() && !source.getHarvestStatus().getHarvest_message().isEmpty()))
			{
				String message = source.getHarvestStatus().getHarvest_message().replace("\n",  " ");
				if (message.length() > 512) {
					sLog.append("extracterr='").append(message.substring(0, 512)).append("...' ");					
				}
				else {
					sLog.append("extracterr='").append(message).append("' ");
				}
			}//TESTED
			
			StringBuffer sLog2 = new StringBuffer();

			// Extraction stats:
			sLog2.append("extracted=").append(toAdd.size()).append(" updated=").append(toUpdate.size()).
					append(" deleted=").append(toRemove.size()).append(" urlerrors=").append(nUrlErrorsThisSource).append(" pxerrors=").append(pxErrors);
			
			getHarvestStatus().logMessage(sLog2.toString(), false); 
			sLog.append(sLog2);
			
			// Other error info for the log only: 
			String mostCommonMessage = getHarvestStatus().getMostCommonMessage();
			if (null != mostCommonMessage) {
				if (mostCommonMessage.length() > 256) {
					mostCommonMessage = mostCommonMessage.substring(0, 253) + "...'";
				}
				sLog.append(mostCommonMessage); // (don't need this in the harvest status since we already have all of them)
			}
			logger.info(sLog.toString());
		}//TESTED

		// May need to update status again (eg any extractor errors or successes - in the harvesters or immediately above):
		if (getHarvestStatus().moreToLog()) {
			getHarvestStatus().update(source, new Date(), source.getHarvestStatus().getHarvest_status(), "", false, false);
		}
		// (note: the harvest status is updated 3 times:
		//  1) inside the source-type harvester (which: 1.1) resets the message 1.2) wipes the messages, but sets prevStatus.getHarvest_message() above)
		//  2) above (the update call, which occurs if logMessage() has been called at any point)
		//  3) after store/index manager, which normally just sets the status unless any errors occurred during indexing

		num_sources_harvested.incrementAndGet();		
	}

	// Quick utility to return if entity extraction has been specified by the user

	public boolean isEntityExtractionRequired(SourcePojo source) {
		return (((null == source.useExtractor()) && (null != default_entity_extractor)) 
				|| ((null != source.useExtractor()) && !source.useExtractor().equalsIgnoreCase("none")))
				||
				(((null == source.useTextExtractor()) && (null != default_text_extractor)) 
						|| ((null != source.useTextExtractor()) && !source.useTextExtractor().equalsIgnoreCase("none")))
						;		
	}

	/**
	 * Takes a list of toAdd and extracts each ones full text and entities/events/sentiment (metadata)
	 * 
	 * @param toAdd The list of toAdd without metadata to extract on
	 * @return Any errors that occured while extracting, null if no error
	 * @throws ExtractorSourceLevelTransientException 
	 */
	public void extractTextAndEntities(List<DocumentPojo> toAdd, SourcePojo source, boolean bFinalizeBatchOnly, boolean calledFromPipeline)
	throws ExtractorDocumentLevelException, ExtractorSourceLevelException, 
	ExtractorDailyLimitExceededException, ExtractorSourceLevelMajorException, ExtractorSourceLevelTransientException
	{
		IEntityExtractor currentEntityExtractor = null;
		try {
			int error_on_feed_count = 0, feed_count = 0;

			// EXTRACTOR SELECTION LOGIC

			if (null != source.useExtractor()) {
				currentEntityExtractor = entity_extractor_mappings.get(source.useExtractor().toLowerCase());
				if (null == currentEntityExtractor) { // (second chance)
					currentEntityExtractor = (IEntityExtractor) lookForDynamicExtractor(source, false);
				}
			}
			if (currentEntityExtractor == null) // none specified or didn't find it (<-latter is error)
			{
				if ((null != source.useExtractor()) && !source.useExtractor().equalsIgnoreCase("none")) {					

					// ie specified one but it doesn't exist....
					StringBuffer errMsg = new StringBuffer("Skipping source=").append(source.getKey()).append(" no_extractor=").append(source.useExtractor());
					logger.warn(errMsg.toString());

					// No point trying this for the rest of the day
					throw new ExtractorSourceLevelException(errMsg.toString());					
				}
				else if (null == source.useExtractor()) { // Didn't specify one, just use default:
					currentEntityExtractor = default_entity_extractor;
				}
			}//TESTED					

			if (bFinalizeBatchOnly) {
				try {
					currentEntityExtractor.extractEntities(null);
				}
				catch (Exception e) {} // do nothing, eg handle entity extractors that don't handle things well
				return;
			}

			// A teeny bit of complex logic:
			// toAdd by default use a text extractor
			// DB/Files by default don't (but can override)

			ITextExtractor currentTextExtractor = null;
			boolean bUseRawContentWhereAvailable = false; // (only applies for feeds)
			if (null != source.useTextExtractor()) {
				currentTextExtractor = text_extractor_mappings.get(source.useTextExtractor().toLowerCase());
				if (null == currentTextExtractor) { // (second chance)
					currentTextExtractor = (ITextExtractor) lookForDynamicExtractor(source, true);
				}
			}
			if (null == currentTextExtractor) { // none specified or didn't find it (<-latter is error)				
				if (null != source.useTextExtractor()) {										

					if ((null == source.getStructuredAnalysisConfig()) && (null == source.getUnstructuredAnalysisConfig())
							&& (null == source.getProcessingPipeline()))
					{
						//(UAH and SAH get raw access to the data if they need it, so can carry on - ditto processing pipeline)

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
				boolean bExtractedText = false;

				// If I've been stopped then just remove all remaining documents
				// (pick them up next time through)
				if (bIsKilled) {
					i.remove();
					if (!calledFromPipeline) {
						doc.setTempSource(null); // (can safely corrupt this doc since it's been removed)
					}
					continue;
				}

				if ( calledFromPipeline || !urlsThatError.contains(doc.getUrl()) ) //only attempt if url is okay
				{				
					feed_count++;

					try {
						// (Check for truncation)
						if ((null != currentEntityExtractor) && (null != doc.getFullText())) {
							try {
								String s = currentEntityExtractor.getCapability(EntityExtractorEnum.MaxInputBytes);
								if (null != s) {
									int maxLength = Integer.parseInt(s);
									if (doc.getFullText().length() > maxLength) { //just warn, it's up to the extractor to sort it out
										getHarvestStatus().logMessage("Warning: truncating document to max length: " + s, false);
									}
								}
							}
							catch (Exception e) {} // max length not reported just carry on
						}
						
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
						if (!calledFromPipeline) {
							doc.setTempSource(null); // (can safely corrupt this doc since it's been removed)
						}

						// Source error, ignore all other documents
						while (i.hasNext()) {
							doc = i.next();
							if (!calledFromPipeline) {
								doc.setTempSource(null); // (can safely corrupt this doc since it's been removed)
							}
							i.remove();
						}
						//TESTED

						throw e; // (ie stop processing this source)
					}//TESTED
					catch (Exception e) { // Anything except daily limit exceeded, expect it to be ExtractorDocumentLevelException

						//TODO (INF-1922): put this in a separate function and call that from pipeline on failure...
						// (not sure what to do about error_on_feed_count though, need to maintain a separate one of those in pipeline?)
						
						// This can come from (sort-of/increasingly) "user" code so provide a bit more information
						StringBuffer errMessage = HarvestExceptionUtils.createExceptionMessage(e);
						_harvestStatus.logMessage(errMessage.toString(), true);
						num_error_url.incrementAndGet();
						nUrlErrorsThisSource++;
						
						if (!calledFromPipeline) {
							urlsThatError.add(doc.getUrl());
						}

						error_on_feed_count++;
						i.remove();
						if (!calledFromPipeline) {
							doc.setTempSource(null); // (can safely corrupt this doc since it's been removed)
						}
					}
					//TESTED
				}
				// (note this is only ever called in legacy mode - it's handled in the HarvestControllerPipeline)
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
			//NOTE: this is duplicated in HarvestControllerPipeline for non-legacy cases
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
			logger.error(errMsg.toString(), e);
			throw new ExtractorSourceLevelTransientException(errMsg.toString());
		}//TESTED

	}//TESTED

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	// UTILITY FUNCTIONS

	/**
	 * Decides what to do with a source when an error is returned from the
	 * extractor process.
	 * 
	 * @param error The error that was returned from extractor
	 * @param source The source that the extractor was working on
	 */
	public void handleExtractError(Exception error, SourcePojo source)
	{
		if ( null != error)
		{
			if ( error instanceof ExtractorDocumentLevelException)
			{
				num_error_url.incrementAndGet();
				nUrlErrorsThisSource++;
			}
			else if ( error instanceof ExtractorSourceLevelException)
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
				//We flag the source in mongo and temp disable
				_harvestStatus.update(source, new Date(), HarvestEnum.success, "Extractor daily limit error.", true, false);				
			}//TESTED
		}
	}//TESTED (just that the instanceofs work)

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
		sb.append(" num_of_px_errors=" + num_error_px.get());
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
		query.put("sourceKey", docToReplace.getDuplicateFrom());
		BasicDBObject dbo = (BasicDBObject) DbManager.getDocument().getMetadata().findOne(query);

		return dbo;
	}//TESTED

	private static String getDocumentContentFromWhichToDuplicate(DocumentPojo docToReplace) {
		try {
			// Get the full text:
			byte[] storageArray = new byte[200000];
			BasicDBObject contentQ = new BasicDBObject("url", docToReplace.getUrl());
			contentQ.put(CompressedFullTextPojo.sourceKey_, new BasicDBObject(MongoDbManager.in_, Arrays.asList(null, docToReplace.getSourceKey())));
			BasicDBObject fields = new BasicDBObject(CompressedFullTextPojo.gzip_content_, 1);
			BasicDBObject dboContent = (BasicDBObject) DbManager.getDocument().getContent().findOne(contentQ, fields);
			if (null != dboContent) {
				byte[] compressedData = ((byte[])dboContent.get(CompressedFullTextPojo.gzip_content_));				
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
		newDoc.setId(null); // (ie ensure it's unique)

		if (bClone) { // Cloned docs have special source key formats (and also need to update their community)
			ObjectId docCommunity = docToReplace.getCommunityId();
			newDoc.setSourceKey(docToReplace.getSourceKey());
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

	//
	// Any documents that have got this far are going to get processed
	//

	// Processing:
	//Attempt to map entity types to set of ontology types
	//eventually the plan is to allow extractors to set the ontology_type of
	//entities to anything found in the opencyc ontology	

	static public void completeDocumentBuilding(List<DocumentPojo> docs, List<DocumentPojo> updateDocs)
	{		
		// Handle documents to be added
		// Currently, just set ontology type
		if ( docs != null )
		{
			for ( DocumentPojo doc : docs )
			{
				if ( doc.getEntities() != null )
				{
					num_ent_extracted.addAndGet(doc.getEntities().size());
					for ( EntityPojo entity : doc.getEntities() )
					{
						if ( entity.getGeotag() != null )
						{
							if (null == entity.getOntology_type()) {
								entity.setOntology_type(GeoOntologyMapping.mapEntityToOntology(entity.getType()));
							}
						}
					}
				}
				if ( doc.getAssociations() != null ) 
				{
					num_event_extracted.addAndGet(doc.getAssociations().size());
				}
			}
		}
		// Remove any docs from update list that didn't get updated
		if ( updateDocs != null )
		{
			Iterator<DocumentPojo> it = updateDocs.iterator();
			while (it.hasNext()) {
				DocumentPojo d = it.next();
				if (null == d.getTempSource()) { //this doc got deleted
					it.remove();
				}
			}
		}
	}

	///////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////

	// Dynamic extraction utilities

	private synchronized Object lookForDynamicExtractor(SourcePojo source, boolean bTextExtractor)
	{
		String extractorName = bTextExtractor ? source.useTextExtractor() : source.useExtractor();
		if (null == extractorName) {
			return null;
		}
		Object outClassInstance = null;
		
		if (null != failedDynamicExtractors) { // (cache for failed shares)
			if (failedDynamicExtractors.contains(extractorName)) {
				return null;
			}
		}
		ClassLoader savedClassLoader = null;
		try {
			ObjectId extractorId = null;
			if (extractorName.startsWith("/")) { // allow /<id>/free text..
				extractorName = extractorName.substring(1).replaceFirst("/.*", "");
			}//TESTED
			try {
				extractorId = new ObjectId(extractorName);
			}
			catch (Exception e) { // not a dynamic share that's fine, just exit no harm done
				return null;
			} 
			// If we're here then it was a share

			BasicDBObject query = new BasicDBObject("_id", extractorId);
			SharePojo extractorInfo = SharePojo.fromDb(MongoDbManager.getSocial().getShare().findOne(query), SharePojo.class);
			if ((null != extractorInfo) && (null != extractorInfo.getBinaryId())) {
				// Check share owned by an admin:
				if (!AuthUtils.isAdmin(extractorInfo.getOwner().get_id())) {
					throw new RuntimeException("Extractor share owner must be admin");
				}//TESTED
				// Check >0 source communities are in the share communities
				int nMatches = 0;
				for (ShareCommunityPojo commObj: extractorInfo.getCommunities()) {
					if (source.getCommunityIds().contains(commObj.get_id())) {
						nMatches++;
						break;
					}
				}
				if (0 == nMatches) {
					throw new RuntimeException("Extractor not shared across source communities");					
				}//TESTED
				
				savedClassLoader = Thread.currentThread().getContextClassLoader();
				
				//HashMap<String, Class<?> > dynamicExtractorClassCache = null;
				if (null == dynamicExtractorClassCache) {
					dynamicExtractorClassCache = new HashMap<String, Class<?> >();
				}

				URL[] cachedJarFile = { new File(maintainJarFileCache(extractorInfo)).toURI().toURL() };				
				
				Class<?> classToLoad = dynamicExtractorClassCache.get(extractorInfo.getTitle());				
				if (null == classToLoad) {				
					URLClassLoader child = new URLClassLoader(cachedJarFile, savedClassLoader);
					
					Thread.currentThread().setContextClassLoader(child);
					classToLoad = Class.forName(extractorInfo.getTitle(), true, child);
					dynamicExtractorClassCache.put(extractorInfo.getTitle(), classToLoad);
				}

				if (bTextExtractor) {
					ITextExtractor txtExtractor = (ITextExtractor )classToLoad.newInstance();
					text_extractor_mappings.put(source.useTextExtractor(), txtExtractor);
					outClassInstance = txtExtractor;
				}
				else {
					IEntityExtractor entExtractor = (IEntityExtractor)classToLoad.newInstance();
					entity_extractor_mappings.put(source.useExtractor(), entExtractor);					
					outClassInstance = entExtractor;
				}
			}
		}
		catch (Exception e) {
			getHarvestStatus().logMessage("custom extractor error: " + e.getMessage(), false);
			if (null == failedDynamicExtractors) {
				failedDynamicExtractors = new HashSet<String>();
				failedDynamicExtractors.add(extractorName);
			}
			//e.printStackTrace();
		} // General fail just carry on 
		catch (Error err) {
			getHarvestStatus().logMessage("custom extractor error: " + err.getMessage(), false);
			if (null == failedDynamicExtractors) {
				failedDynamicExtractors = new HashSet<String>();
				failedDynamicExtractors.add(extractorName);
			}
			//err.printStackTrace();
			
		} // General fail just carry on
		finally {
			if (null != savedClassLoader) {
				Thread.currentThread().setContextClassLoader(savedClassLoader);				
			}
		}
		return outClassInstance;
	}//TOTEST

	/**
	 * Finds the gridfile given by id and returns the bytes
	 * 
	 * @param id the object id of the gridfile to lookup (stored in sharepojo)
	 * @return bytes of file stored in gridfile
	 */	
//	private static byte[] getGridFile(ObjectId id)
//	{
//		ByteArrayOutputStream out = new ByteArrayOutputStream();
//		try
//		{
//			GridFSDBFile file = DbManager.getSocial().getShareBinary().find(id);						
//			file.writeTo(out);
//			byte[] toReturn = out.toByteArray();
//			out.close();
//			return toReturn;
//		}
//		catch (Exception ex){}		
//		return null;
//	}

	/**
	 * Downloads jar file from web using URL call.  Typically
	 * the jar files we be kept in our /share store so we will
	 * be calling our own api.
	 * 
	 * @param jarURL
	 * @return
	 * @throws Exception 
	 */
	public static String maintainJarFileCache(SharePojo share) throws Exception
	{		
		String tempFileName = System.getProperty("java.io.tmpdir") + "/" + share.get_id() + ".cache.jar";
		File tempFile = new File(tempFileName);

		// Compare dates (if it exists) to see if we need to update the cache) 
		
		if (!tempFile.exists() || (tempFile.lastModified() < share.getModified().getTime())) {
			OutputStream out = new BufferedOutputStream(new FileOutputStream(tempFileName));
			if ( share.getBinaryId() != null )
			{			
				GridFSDBFile file = DbManager.getSocial().getShareBinary().find(share.getBinaryId());						
				file.writeTo(out);				
			}
			else
			{
				out.write(share.getBinaryData());
			}
		}//TESTED
		
		return tempFileName;
	}
}
