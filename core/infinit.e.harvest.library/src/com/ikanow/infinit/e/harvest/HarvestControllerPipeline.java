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
package com.ikanow.infinit.e.harvest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import javax.script.ScriptException;

import org.json.JSONException;

import com.ikanow.infinit.e.data_model.InfiniteEnums.ExtractorDailyLimitExceededException;
import com.ikanow.infinit.e.data_model.InfiniteEnums.ExtractorDocumentLevelException;
import com.ikanow.infinit.e.data_model.InfiniteEnums.ExtractorSourceLevelException;
import com.ikanow.infinit.e.data_model.InfiniteEnums.ExtractorSourceLevelMajorException;
import com.ikanow.infinit.e.data_model.InfiniteEnums.ExtractorSourceLevelTransientException;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePipelinePojo;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.ikanow.infinit.e.data_model.store.config.source.SourceRssConfigPojo;
import com.ikanow.infinit.e.data_model.store.config.source.SourceRssConfigPojo.ExtraUrlPojo;
import com.ikanow.infinit.e.data_model.store.config.source.SourceSearchFeedConfigPojo;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.infinit.e.harvest.enrichment.custom.StructuredAnalysisHarvester;
import com.ikanow.infinit.e.harvest.enrichment.custom.UnstructuredAnalysisHarvester;
import com.ikanow.infinit.e.harvest.extraction.document.rss.FeedHarvester_searchEngineSubsystem;
import com.ikanow.infinit.e.harvest.utils.DateUtility;
import com.ikanow.infinit.e.harvest.utils.HarvestExceptionUtils;
import com.ikanow.infinit.e.harvest.utils.PropertiesManager;

public class HarvestControllerPipeline {

	// Objects that persist across the sources

	protected HarvestController _hc;
	protected StructuredAnalysisHarvester _sah;
	protected UnstructuredAnalysisHarvester _uah;

	protected long nMaxTimeSpentInPipeline_ms = 600000; // (always gets overwritten)
	
	protected long nInterDocDelay_ms = 10000; // (the inter doc delay time is always set anyway)
	public void setInterDocDelayTime(long nBetweenFeedDocs_ms) {
		nInterDocDelay_ms = nBetweenFeedDocs_ms;
	}
	
	protected String _defaultTextExtractor = null;
	
	// Object initialization:
	
	private void intializeState()
	{
		_sah = null;
		_uah = null;
		_defaultTextExtractor = null;
		// (don't re-initialize nInterDocDelay_ms that is always set from the parent hc)
		// (don't re-initialize _props, that can persist)
		// (don't re-initialize doc state like _cachedRawFullText*, _firstTextExtractionInPipeline, _lastDocInPipeline)
		_timeOfLastSleep = -1L; // (not sure about this one, can see an argument for leaving it, but it's consistent with legacy this way)
		
		if (null == _props) { // turns out we always need this, for getMaxTimePerSource()
			_props = new PropertiesManager();	
			nMaxTimeSpentInPipeline_ms = _props.getMaxTimePerSource();
		}
	}//TESTED (by eye)
	
	/////////////////////////////////////////////////////////////////////////////////////////
	//
	// Sets up the input stream into the harvester (and performs various other initialization activities)
	// 

	private PropertiesManager _props = null;
	
	public void extractSource_preProcessingPipeline(SourcePojo source, HarvestController hc)
	{
		// Initialize some variables (since this pipeline can persist):
		
		intializeState();
		
		// Now run:
		
		_hc = hc;
		StringBuffer globalScript = null;
		for (SourcePipelinePojo pxPipe: source.getProcessingPipeline()) { /// (must be non null if here)
			
			// 1] Input source, copy into src pojo
			
			if (null != pxPipe.database) {
				source.setUrl(pxPipe.database.getUrl());
				source.setAuthentication(pxPipe.database.getAuthentication());
				source.setDatabaseConfig(pxPipe.database);
				source.setExtractType("Database");
			}//TESTED (NEED to build a working test file - basic_db_test needs a DB to test against)
			if (null != pxPipe.nosql) {
				//TODO (INF-1963): Not yet supported
			}
			else if (null != pxPipe.file) {
				source.setUrl(pxPipe.file.getUrl());
				source.setFileConfig(pxPipe.file);
				source.setExtractType("File");
			}//TESTED (basic_file)
			else if (null != pxPipe.feed) {
				//(unset URL here, it will use pxPipe.feed.extraUrls automatically)
				source.setUrl(null);
				source.setAuthentication(pxPipe.feed.getAuthentication());
				source.setRssConfig(pxPipe.feed);
				source.setExtractType("Feed");
			}//TESTED (basic_feed_test)
			else if (null != pxPipe.web) {
				//(unset URL here, it will use pxPipe.web.extraUrls automatically)
				source.setUrl(null);
				source.setAuthentication(pxPipe.web.getAuthentication());
				source.setRssConfig(pxPipe.web);
				source.setExtractType("Feed"); // (feed/web use the same base extractor, called "Feed" for legacy reasons)
			}//TESTED (basic_web_test*.json)
			
			// 2] Globals - copy across harvest control parameters, if any
			
			if ((null != pxPipe.globals) && (null != pxPipe.globals.scripts)) {
				globalScript = new StringBuffer();
				for (String script: pxPipe.globals.scripts) {
					if (globalScript.length() > 0) {
						globalScript.append("\n\n");
					}
					globalScript.append(script);
				}
			}//TESTED (web_links_global)
			
			if (null != pxPipe.harvest) {
				
				if (null != pxPipe.harvest.maxDocs_global) {
					source.setMaxDocs(pxPipe.harvest.maxDocs_global);
				}
				if (null != pxPipe.harvest.searchCycle_secs) {
					if (null == source.getSearchCycle_secs()) {
						// (don't override top level search cycle - that's for suspending/enabling the source)
						source.setSearchCycle_secs(pxPipe.harvest.searchCycle_secs);
					}
				}
				if (null != pxPipe.harvest.duplicateExistingUrls) {
					source.setDuplicateExistingUrls(pxPipe.harvest.duplicateExistingUrls);
				}
				if (null != pxPipe.harvest.distributionFactor) {
					source.setDistributionFactor(pxPipe.harvest.distributionFactor);
				}
				if (null != pxPipe.harvest.maxDocs_perCycle) { 
					//per cycle max docs (-> success_iteration if exceeded, ie next cycle will start again)
					source.setThrottleDocs(pxPipe.harvest.maxDocs_perCycle);		
						// (ugh apologies for inconsistent naming convention between legacy and pipeline!)
				}
				if (null != pxPipe.harvest.throttleDocs_perCycle) {
					//TODO (INF-2223): per cycle counts (-> success if exceeded), ie can limit overall ingest speed)
				}
				
			}//TESTED (storageSettings_test)
			
			// 3] Extraction - link extraction/document splitting, copy into feed pojo
			
			if (null != pxPipe.links) {
				applyGlobalsToDocumentSplitting(source, pxPipe.links, globalScript, true);
			}
			if (null != pxPipe.splitter) {
				applyGlobalsToDocumentSplitting(source, pxPipe.splitter, globalScript, false);
			}
			
			// (same for document splitter - they're the same at this point)
			
			// 3-6] Everything else - just check whether we're going to need UAH/SAH
			
			// Text Engine
			
			if (null != pxPipe.textEngine) {
				if (null == _defaultTextExtractor) {
					_defaultTextExtractor = _props.getDefaultTextExtractor();
				}//TESTED (text_raw_to_boilerpipe)
				if ((null != pxPipe.textEngine.engineName) && pxPipe.textEngine.engineName.equalsIgnoreCase("raw")) {
					requiresUnstructuredAnalysis();				
				}//TESTED (raw by itself)
			}
			
			// UAH
			
			if ((null != pxPipe.contentMetadata) || (null != pxPipe.text)) {
				requiresUnstructuredAnalysis();
			}//TESTED (see requires* function)
			
			// SAH
			
			if ((null != pxPipe.docMetadata) || (null != pxPipe.entities) || (null != pxPipe.associations))
			{
				requiresStructuredAnalysis();
			}//TESTED (see requires* function)

			if (null != pxPipe.searchIndex) { // Handles which fields need to be index
				source.setSearchIndexFilter(pxPipe.searchIndex);
			}//TESTED (storageSettings_test)				
			
			if (null != pxPipe.storageSettings) {
				requiresStructuredAnalysis();
				//TODO (INF-2223): handle the metadata-specific settings 
				// (maybe just create a final storage settings at the end if we haven't managed to correlate
				//  with a pre-existing one) 
			}//TESTED (storageSettings_test)				
			
			// 4] If any of the pipeline elements have criteria then need to turn sah on
			if ((null != pxPipe.criteria) && !pxPipe.criteria.isEmpty()) {
				requiresStructuredAnalysis();
			}//TESTED (basic_criteria_test)
			
		}//TESTED

		if (null != source.getSearchIndexFilter()) {
			//TODO (INF-2223): going to need to copy info from md/ents/assocs into here, some fiddly logic to worry about			
		}//
		
		// Initialize the script engines here:
		if (null != _sah) {
			_sah.intializeScriptEngine();
		}//TESTED (imports_and_lookup_test_uahSah)
		if (null != _uah) { // (will just use the SAH's engine if it exists)
			_uah.intializeScriptEngine(null, null);
				// (script engine params get configured in the processing pipeline proper, see below)
		}//TESTED (imports_and_lookup_test)
		
	}
	
	/////////////////////////////////////////////////////////////////////////////////////////
	//
	// Gets metadata using the extractors and appends to documents
	//
	
	// Per document member variables used to pass between main loop and utilities:
	private String _cachedRawFullText = null;
	private boolean _cachedRawFullText_available = true; // (set this latch if I never see the full text, eg non-raw text engine is called)
	private boolean _firstTextExtractionInPipeline = false;
	private boolean _lastDocInPipeline = false;
	
	public void enrichSource_processingPipeline(SourcePojo source, List<DocumentPojo> toAdd, List<DocumentPojo> toUpdate, List<DocumentPojo> toRemove)
	{
		Iterator<SourcePipelinePojo> pxPipeIt = source.getProcessingPipeline().iterator(); // (must be non null if here)
		while (pxPipeIt.hasNext()) { 
			SourcePipelinePojo pxPipe = pxPipeIt.next();
				// (note can edit the iterator/list because this bit of the pipeline only runs once, not over each doc)
			
			// 1] Is this a starting point:
			
			if ((null != pxPipe.database) || (null != pxPipe.nosql) || (null != pxPipe.file) || (null != pxPipe.feed) || (null != pxPipe.web))
			{
				pxPipeIt.remove();
				continue; // Already handled these
			}
			
			// 2] Is this a global operation:
			
			if (null != pxPipe.globals) {
				pxPipeIt.remove();				
				handleGlobals(pxPipe, source);
				
			}//TESTED (UAH/imports_only, UAH/imports_and_scripts, SAH+UAH/imports_only, SAH+UAH/imports_and_scripts)
			
			if (null != pxPipe.harvest) {
				pxPipeIt.remove();
				// Already handled
				continue;
			}//TESTED (storageSettings_test)
			
			if (null != pxPipe.lookupTables) {
				pxPipeIt.remove();
				
				if (null != _sah) {
					_sah.loadLookupCaches(pxPipe.lookupTables, source.getCommunityIds());
						// (will also load them into the UAH if created)
				}
				else { // UAH specified but SAH not					
					_uah.loadLookupCaches(pxPipe.lookupTables, source.getCommunityIds());
				}
			}
			//TESTED (uah:import_and_lookup_test.json, sah+uah:import_and_lookup_test_uahSah.json)
			
			if (null != pxPipe.aliases) {
				pxPipeIt.remove();
				//TODO (INF-2219): Not currently supported
				continue;
			}
			
			if (null != pxPipe.links) { // (already handler for feed/web in globals section, otherwise ignored)
				pxPipeIt.remove();
				continue; // Going to handle this as part of feed/web processing 
			}//TESTED
			
			// 3] Post processing operation
			
			if (null != pxPipe.searchIndex) {
				pxPipeIt.remove();
				// Already handled
				continue;
			}//TESTED (storageSettings_test)
			
		}//(end globals loop over the data)
		
		if (null == toAdd) 
			return;
		
		// The remainder of these steps are per-document...
			
		ListIterator<DocumentPojo> docIt = toAdd.listIterator();
		_firstTextExtractionInPipeline = false;
	
		long pipelineStartTime = new Date().getTime();
		
		int error_on_feed_count = 0, feed_count = 0;
		LinkedList<DocumentPojo> splitterList = null;
		for (docIt.hasNext();;) {

			DocumentPojo doc = null;
			if (!docIt.hasNext()) {
				if ((null == splitterList) || (splitterList.isEmpty())) {
					break;
				} // all done!
				else { // add all splitterList elements to toAdd
					while (!splitterList.isEmpty()) {
						docIt.add(splitterList.removeLast());
						doc = docIt.previous();
					}
				}//TESTED (doc_splitte_test)
			}
			else { 
				doc = docIt.next();
			}//TESTED

			boolean processSpawnedDocOrNotSpawnedDoc = null == doc.getSpawnedFrom(); // (initially: only true if not spawned doc...)

			// (Do this at the top so don't get foxed by any continues in the code)
			long currTime = new Date().getTime();		
			
			if ( HarvestController.isHarvestKilled() // (harvest manually killed or because of global time)
					|| 
				((currTime - pipelineStartTime) > nMaxTimeSpentInPipeline_ms))
						// Don't let any source spend too long in one iteration...
			{ 
				source.setReachedMaxDocs(); // (move to success iteration)		

				// Remove the rest of the documents
				doc.setTempSource(null); // (can safely corrupt this doc since it's been removed)
				docIt.remove();
				while (docIt.hasNext()) {
					doc = docIt.next();
					doc.setTempSource(null); // (can safely corrupt this doc since it's been removed)
					docIt.remove();
				}				
				// Exit loop
				break;
			}//TESTED
			
			feed_count++;
			
			try {
				// For cases where we grab the full text for early processing and then want it back
				_cachedRawFullText = null;
				_cachedRawFullText_available = true; // (set this latch if I never see the full text, eg non-raw text engine is called)
				
				if (null != _uah) {
					_uah.resetForNewDoc();				
				}
				if (null != _sah) {
					_sah.resetForNewDoc();	
				}
				_lastDocInPipeline = !docIt.hasNext();
	
				//NOTE: inter-doc waiting needs to happen before the following processing elements:
				// pxPipe.textEngine: always
				// pxPipe.text: only if doc.fullText==null
				// pxPipe.contentMetadata: only if doc.fullText==null
				// pxPipe.featureEngine: only if doc.fullText==null
							
				for (SourcePipelinePojo pxPipe: source.getProcessingPipeline()) { /// (must be non null if here)
					//DEBUG
					//System.out.println("PX EL: " + pxPipe.display + ", " + processSpawnedDocOrNotSpawnedDoc + ", " + doc.getUrl() + ": " + toAdd.size());
					
					// Spawned documents only enter at their spot in the pipeline:
					if (!processSpawnedDocOrNotSpawnedDoc) {
						if (pxPipe == doc.getSpawnedFrom()) { // (intentionally ptr ==)
							processSpawnedDocOrNotSpawnedDoc = true; // (next pipeline element, start processing)
						}
						continue; // (skip past elements, including the spawnee)
						
					}//TESTED (doc_splitter_test);
					
					// Run criteria for this pipeline element:
					if ((null != pxPipe.criteria) && !pxPipe.criteria.isEmpty()) {
						if (!pxPipe.criteria.startsWith("$SCRIPT")) {
							pxPipe.criteria= "$SCRIPT(" + pxPipe.criteria + ")";
						}//TESTED (basic_criteria_test)
						
						if (!_sah.rejectDoc(pxPipe.criteria, doc, false)) {
							continue;
						}			
					}//TESTED (basic_criteria_test)
					
					//TODO (INF-2218): improve performance of doc serialization by only updating spec'd fields (note: need to change the js engine)
					// and by sharing engine state between the SAH and UAH
					
					// Save metadata state so we know if we need to re-serialize the document 				
					int nCurrMetaFields = 0;
					Object ptr = doc.getMetadata();
					// (Only needed for text engine or feature engine - otherwise the SAH cache is reset as needed)
					if ((null != pxPipe.featureEngine) || (null != pxPipe.textEngine)) {
						if ((null != _sah) && (null != ptr)) {
							nCurrMetaFields = doc.getMetadata().size();
						}					
					}//TESTED (metadata_doc_cache_reset)
					
					try {					
						// 3] Create new documents from existing ones
						
						if (null != pxPipe.splitter) {
							if (null == splitterList) {
								splitterList = new LinkedList<DocumentPojo>();
							}
							try {
								splitDocuments(doc, source, pxPipe, splitterList);
							}
							catch (Exception e) {} // do nothing, still want to keep doc unless otherwise specified below
							
							if ((null == pxPipe.splitter.getDeleteExisting()) || pxPipe.splitter.getDeleteExisting()) {
								// Don't keep original doc
								docIt.remove();
								doc.setTempSource(null); // (can safely corrupt this doc since it's been removed)
								break;								
							}//TESTED (test1,test2)
							
						}//TESTED (doc_splitter)

						// 4] Text and linked document extraction
						
						if (null != pxPipe.text) {
							// IN: doc (xpath/regex) or json(doc) (js)
							// OUT: doc.fullText, doc.title, doc.desc, (less common) doc.metadata.*
							// POST: reset
	
							updateInterDocDelayState(doc, false);
							
							String cachedFullText = _uah.doManualTextEnrichment(doc, pxPipe.text, source.getRssConfig());
							if (null != _sah) {
								_sah.resetDocumentCache();				
							}
							
							// Cache the full text if available
							if ((null == _cachedRawFullText) && _cachedRawFullText_available) {
								_cachedRawFullText = cachedFullText;
							}//(TESTED: cache available: text_raw_to_boilerpipe, no cache available: text_then_raw_then_content*)
						}
						//TESTED (fulltext_regexTests.json, basic_web_uahRawText.json, text_raw_to_boilerpipe)
						
						if (null != pxPipe.textEngine) { 
							// IN: doc
							// OUT: doc.* 
							// POST: reset sah ent cache (_should_ change only metadata and text (+ents/assocs) so don't need to reset sah doc cache) 
													
							if (!handleTextEngine(pxPipe, doc, source)) {
								error_on_feed_count++;
								
								if ((null == pxPipe.textEngine.exitOnError) || pxPipe.textEngine.exitOnError) {
									doc.setTempSource(null); // (can safely corrupt this doc since it's been removed)
									docIt.remove();
									break; // (no more processing)
								}//TESTED (engines_exit_on_error)
								
							}
						} //TESTED (basic_web_test_ocOptions.json, basic_web_test_textaaOptions.json)
						
						// 5] Document level fields
						
						if (null != pxPipe.docMetadata) {
							// IN: sah.doc
							// OUT: doc.*
							// POST: reset
							
							_sah.setDocumentMetadata(doc, pxPipe.docMetadata);
							_sah.resetDocumentCache();
						}
						//TESTED (fulltext_docMetaTest.json)
						
						if (null != pxPipe.contentMetadata) {
							// IN: doc (xpath/regex) or json(doc) (js)
							// OUT: doc.meta.*
							// POST: reset
							
							updateInterDocDelayState(doc, false);
							
							_uah.processMetadataChain(doc, pxPipe.contentMetadata, source.getRssConfig());
							if (null != _sah) {
								_sah.resetDocumentCache();				
							}
							
							// Cache the full text if available
							if ((null == _cachedRawFullText) && _cachedRawFullText_available) {
								_cachedRawFullText = doc.getFullText();
							}//(TESTED: ((cache available) text_content_then_raw_to_boilerpipe (not available) text_default_then_content_then_default_test.json)
						}
						//TESTED (fulltext_regexTests.json, basic_web_uahRawText.json)
						
						// 6] Entities and Associations
						
						if (null != pxPipe.entities) {
							// IN: sah.doc.*, sah.doc.metadadata.*, 
								//(recalculate from scratch then use: sah.entityMap, sah.geoMap)
							// OUT: doc.entities, sah.entityMap, sah.geoMap
							// POST: no need to reset anything, sah.entities never read 
							
							_sah.setEntities(doc, pxPipe.entities);
						}
						//TESTED (fulltext_ents_and_assocs.json)
						
						if (null != pxPipe.associations) {
							// IN: sah.doc.*, sah.doc.metadadata.*, doc.entities, sah.entityMap, sah.geoMap
							// OUT: doc.associations
							// POST: no need to reset anything, sah.associations never read	
							
							_sah.setAssociations(doc, pxPipe.associations);
						}
						//TESTED (fulltext_ents_and_assocs.json)
						
						if (null != pxPipe.featureEngine) {
							// IN: doc
							// OUT: doc.* 
							// POST: reset sah ent cache (_should_ change only metadata, ents and assocs so don't need to reset sah doc cache)  
							
							if (!handleFeatureEngine(pxPipe, doc, source)) {
								error_on_feed_count++;
								
								if ((null == pxPipe.featureEngine.exitOnError) || pxPipe.featureEngine.exitOnError) {
									doc.setTempSource(null); // (can safely corrupt this doc since it's been removed)
									docIt.remove();
									break; // (no more processing)
									
								}//TESTED (engines_exit_on_error_test)
							}
							
						} //TESTED (basic_web_test_ocOptions.json, basic_web_test_textaaOptions.json)

						// 7] Finishing steps:
						
						if (null != pxPipe.storageSettings) {							
							// IN: doc
							// OUT: doc.metadata.*
							// POST: reset if metadata settings present
							
							if (!handleStorageSettings(pxPipe, doc)) {
								// (this is a manual rejection not an error so we're good)
								doc.setTempSource(null); // (can safely corrupt this doc since it's been removed)
								docIt.remove();
								break; // (no more processing for this document)
							}							
							if ((null != pxPipe.storageSettings.exitPipeline) && pxPipe.storageSettings.exitPipeline) {
								break; // (no more processing for this document)
							}//TESTED (basic_criteria_test)
							
						}//TESTED (storageSettings_test; not update - need more infrastructure) 	
						
					}
					catch (Exception e) { // For now we'll just handle any exception by nuking either the doc or source
						// (in the future we could consider continuing on depending on which pipeline element had died
						//  or perhaps even have a common option "continue on error")
						throw e;
					}
					finally {}
					
					// Check metadata state so we know if we need to re-ingest the document
					// (Only needed for text engine or feature engine - otherwise the SAH cache is reset as needed)
					if ((null != pxPipe.featureEngine) || (null != pxPipe.textEngine)) {
						Object ptrAfter = doc.getMetadata();
						int nCurrMetaFieldsAfter = 0;
						if (null != _sah) {
							if (null != ptrAfter) {
								nCurrMetaFieldsAfter = doc.getMetadata().size();						
							}
							if ((ptr != ptrAfter) || (nCurrMetaFieldsAfter != nCurrMetaFields))
							{
								_sah.resetDocumentCache();
							}
						}
					}//TESTED (metadata_doc_cache_reset)
					
				}//end loop over per-document processing pipeline elements
			}
			catch (ExtractorSourceLevelException e) { // Delete all docs, log
				this.handleDocOrSourceError(source, doc, docIt, e, true);	
				break;
			} //TESTED (c/p file_textError)
			catch (ExtractorDailyLimitExceededException e) {
				this.handleDocOrSourceError(source, doc, docIt, e, true);	
				break;
			} //TESTED (c/p file_textError) 
			catch (ExtractorSourceLevelMajorException e) {
				this.handleDocOrSourceError(source, doc, docIt, e, true);	
				break;
			} //TESTED (file_textError) 
			catch (ExtractorSourceLevelTransientException e) {
				this.handleDocOrSourceError(source, doc, docIt, e, true);	
				break;
			} //TESTED (c/p file_textError)
			catch (Exception e) { // Misc doc error
				/**/
				e.printStackTrace();
				
				error_on_feed_count++;
				this.handleDocOrSourceError(source, doc, docIt, e, false);	
				// (don't break)
			} //TESTED (web_errors_test)
			finally {}

		}//end loop over documents		
		
		// Old legacy logic to check for a source that is full of errors:
		// c/p from HarvestController.extractTextAndEntities
		try {
			if ((error_on_feed_count == feed_count) && (feed_count > 5))
			{
				String errorMsg = new StringBuffer().append(feed_count).append(" docs, ").append(error_on_feed_count).append(", errors").toString(); 
				if (error_on_feed_count > 20) {
					throw new ExtractorSourceLevelMajorException(errorMsg);
				}
				else {
					throw new ExtractorSourceLevelException(errorMsg);
				}//TESTED (copy/paste from legacy HarvestController.extractTextAndEntities)
			}
		}//TESTED (web_errors_test)
		catch (ExtractorSourceLevelMajorException e) {
			this.handleDocOrSourceError(source, null, null, e, true);	
		} //TESTED (c/p web_errors_test)
		catch (ExtractorSourceLevelException e) { // Delete all docs, log
			this.handleDocOrSourceError(source, null, null, e, true);	
		} //TESTED (web_errors_test)
	}
	
	////////////////////////////////////////////////////////////////////////////////
	
	// Sub-processing utilities for the more complex pipeline elements
	
	// pxPipe.globals
	
	private void handleGlobals(SourcePipelinePojo pxPipe, SourcePojo source)
	{
		if (null != _sah) {
			_sah.loadGlobalFunctions(pxPipe.globals.imports, pxPipe.globals.scripts, pxPipe.globals.scriptlang);
				// (will also load them into the UAH if created)
			
		}//TESTED (imports_and_lookup_test_uahSah, imports_and_script_test_sah, imports_and_2scripts_test_uahSah)
		else if (null != _uah) { 
			// handle case where the SAH is not created but the UAH is...
			
			//TODO (INF-2118): Note imports bypass security if prefaced with file:// ...
			if ((null == pxPipe.globals.scriptlang) || (pxPipe.globals.scriptlang.equalsIgnoreCase("javascript"))) {
				
				if (null != pxPipe.globals.scripts) {
					boolean bFirst = true;
					for (String script: pxPipe.globals.scripts) {
						List<String> imports = bFirst ? pxPipe.globals.imports : null;
						
						_uah.loadGlobalFunctions(imports, script);
						bFirst = false;
					}
				}//TESTED (imports_2scripts_test_uah)
				else {					
					_uah.loadGlobalFunctions(pxPipe.globals.imports, null); 
				}//TESTED (imports_and_lookup_test)
			}
		}		
	}//TESTED (UAH/imports_only, UAH/imports_and_scripts, SAH+UAH/imports_only, SAH+UAH/imports_and_scripts)
	
	////////////////////////////////////
	
	// pxPipe.textEngine (false means nuke the document) 
	
	private boolean handleTextEngine(SourcePipelinePojo pxPipe, DocumentPojo doc, SourcePojo source) throws ExtractorDocumentLevelException, ExtractorSourceLevelException, ExtractorDailyLimitExceededException, ExtractorSourceLevelMajorException, ExtractorSourceLevelTransientException, IOException
	{
		// Set default extractor up:
		String extractor = _defaultTextExtractor;
		if ((null != pxPipe.textEngine.engineName) && !pxPipe.textEngine.engineName.equalsIgnoreCase("default")) {
			extractor = pxPipe.textEngine.engineName;
		}//TESTED (various)
		
		// Handle case of doc that doesn't have have a valid URL
		String savedUrl = null;
		if ((null != doc.getUrl()) && !doc.getUrl().startsWith("http") &&
				(null != doc.getDisplayUrl()) && doc.getDisplayUrl().startsWith("http"))
		{
			savedUrl = doc.getUrl();
			doc.setUrl(doc.getDisplayUrl());
			doc.setDisplayUrl(savedUrl);
		}
		try {
			source.setUseExtractor("None");
			if ((null != extractor) && extractor.equalsIgnoreCase("raw")) {
				// Raw text extraction, some special cases...
				
				if (null != _cachedRawFullText) { // if we cached full text then just reset it
					doc.setFullText(_cachedRawFullText);
				}
				else { //go get it (NOTE: null != source.getRssConfig() because of check at the top)
					doc.setFullText(null); // (reset)
					updateInterDocDelayState(doc, true);
					
					try {
						_uah.getRawTextFromUrlIfNeeded(doc, source.getRssConfig());
					}
					catch (Exception e) {
						return false;
					}						
					_cachedRawFullText = doc.getFullText();
					_cachedRawFullText_available = true;
				}
				return true; // no need to do anything else, we have what we need
			}//TESTED (cache: text_content_then_raw_to_boilerpipe, text_raw_to_boilerpipe; nocache: web_test_aaNoText.json)
			else { // (normal cases)
				source.setUseTextExtractor(extractor);
				// (null => use default)
				
				// Currently: special case for boilerpipe, can re-use earlier cached raw text 
				if ((null != extractor) && (null != _cachedRawFullText) && extractor.equalsIgnoreCase("boilerpipe")) {
					//TODO (INF-2223): longer term need to use text extractor capability instead (which means getting a global text extractor obj)
					doc.setFullText(_cachedRawFullText);
				}//TESTED (not-default: text_content_then_raw_to_boilerpipe, text_raw_to_boilerpipe, web_raw_to_default, default==boilerpipe)
				else { // All other cases...
					updateInterDocDelayState(doc, true);
				}//TESTED (web_raw_to_default, default==aapi)
			}//TESTED (see above)
			
			source.setExtractorOptions(pxPipe.textEngine.engineConfig);
			ArrayList<DocumentPojo> docWrapper = new ArrayList<DocumentPojo>(1);
			docWrapper.add(doc);
			_hc.extractTextAndEntities(docWrapper, source, false, true);
			if (docWrapper.isEmpty()) { // Then this document has errored and needs to be removed - note logging etc has already occurred
				return false;
			}//TESTED (featureEngine_batch_test, remove textEngine stage; c/p from featureEngine)
		}
		finally { // ensure doc isn't corrupted
			if (null != savedUrl) {
				doc.setDisplayUrl(doc.getUrl());
				doc.setUrl(savedUrl);
			}
		}
		
		// Reset SAH cache for any future manual ent/assoc extraction
		if (null != _sah) {
			_sah.resetEntityCache(); // (ensures is recalculated by next manual entity/association call)
		}//TESTED (entity_cache_reset_test)

		if ((null == _cachedRawFullText) && (null != doc.getFullText())) {
			// If we're here (ie not raw) and text has successfully been set, then we're never going to get the cached text
			_cachedRawFullText_available = false;
		}//TESTED (text_default_then_content_then_default_test.json)
		
		// (note: changes to doc fields will not be available to next docMetadata/entities/associations element)
		
		return true;
		
	}//TESTED (basic_web_test_ocOptions.json, basic_web_test_textaaOptions.json)
	
	////////////////////////////////////
	
	// pxPipe.featureEngine (false means nuke the document) 
		
	private boolean handleFeatureEngine(SourcePipelinePojo pxPipe, DocumentPojo doc, SourcePojo source) throws ExtractorDocumentLevelException, ExtractorSourceLevelException, ExtractorDailyLimitExceededException, ExtractorSourceLevelMajorException, ExtractorSourceLevelTransientException
	{
		updateInterDocDelayState(doc, false);
		
		source.setUseTextExtractor("None");
		if ((null != pxPipe.featureEngine.engineName) && !pxPipe.featureEngine.engineName.equalsIgnoreCase("default")) {
			source.setUseExtractor(pxPipe.featureEngine.engineName);
		}
		else {
			source.setUseExtractor(null);
		}//TESTED				
		source.setExtractorOptions(pxPipe.featureEngine.engineConfig);
		ArrayList<DocumentPojo> docWrapper = new ArrayList<DocumentPojo>(1);
		docWrapper.add(doc);
		_hc.extractTextAndEntities(docWrapper, source, false, true);
			// (Note if no textEngine is used and this engine supports it then also does text extraction - TESTED)

		// Handle batch completion (needs to happen before docWrapper.isEmpty()==error check)
		if (_lastDocInPipeline) {
			_hc.extractTextAndEntities(null, source, true, true); 
		}//TESTED (featureEngine_batch_test)
		
		if (docWrapper.isEmpty()) { // Then this document has errored and needs to be removed
			return false;
		}//TESTED (featureEngine_batch_test, remove textEngine stage)
		
		//TODO (INF-2223) entity and assoc filter
		
		// Reset SAH cache for any future manual ent/assoc extraction
		if (null != _sah) {
			_sah.resetEntityCache(); // (ensures is recalculated by next manual entity/association call)
		}//TESTED (entity_cache_reset_test; c/p from textEngine)
		
		if ((null == _cachedRawFullText) && (null != doc.getFullText())) {
			// If we're here (ie not raw) and text has successfully been set, then we're never going to get the cached text
			_cachedRawFullText_available = false;
		}//TESTED (web_test_aaNoText)						
		
		// (note: changes to doc fields will not be available to next docMetadata/entities/associations element)

		return true;
		
	}//TESTED (basic_web_test_ocOptions.json, basic_web_test_textaaOptions.json)
	
	////////////////////////////////////
	
	// pxPipe.storageSettings (false means remove the doc, no error though - this is because of a reject)
	
	private boolean handleStorageSettings(SourcePipelinePojo pxPipe, DocumentPojo doc) throws JSONException, ScriptException
	{
		// Document rejection:
		if (null != pxPipe.storageSettings.rejectDocCriteria) {
			if (!pxPipe.storageSettings.rejectDocCriteria.startsWith("$SCRIPT")) {
				pxPipe.storageSettings.rejectDocCriteria = "$SCRIPT(" + pxPipe.storageSettings.rejectDocCriteria + ")";
			}//TESTED (storageSettings_test)
			
			if (_sah.rejectDoc(pxPipe.storageSettings.rejectDocCriteria, doc)) {
				return false;
			}			
		}//TESTED (storageSettings_test)
		if (null != pxPipe.storageSettings.onUpdateScript) {
			_sah.handleDocumentUpdates(pxPipe.storageSettings.onUpdateScript, doc);
		}//TOTEST (but low risk) (INF-1922)
		
		if (null != pxPipe.storageSettings.metadataFieldStorage) {
			_sah.removeUnwantedMetadataFields(pxPipe.storageSettings.metadataFieldStorage, doc);
			_sah.resetDocumentCache();
		}//TESTED (storageSettings_test)

		return true;
		
	}//TOTEST TESTED (metadataFieldStorage, rejectDocCriteria) can't test onUpdateScript until integrated with harvester
	
	////////////////////////////////////////////////////////////////////////////////
	
	// Utility functions
	
	//
	// Called from first loop through the pipeline - determines what needs to be done
	//
	
	protected void requiresStructuredAnalysis() {
		if (null == _sah) {
			_sah = new StructuredAnalysisHarvester();
			_sah.setContext(_hc);
			
			if (null != _uah) {
				_sah.addUnstructuredHandler(_uah);
			}
		}//(only only)
	}//TESTED (fullText_docMetaTests.json, import_lookups_test_uahSah)
	
	protected void requiresUnstructuredAnalysis() {
		if (null == _uah) {
			_uah = new UnstructuredAnalysisHarvester();
			_uah.setContext(_hc);
			
			if (null != _sah) {
				_sah.addUnstructuredHandler(_uah);
			}
		}//(only only)
	}//TESTED (fulltext_regexTests.json, import_lookups_test_uahSah)
	
	////////////////////////////////////////////////////////////////////////////////
	
	// Handle sleeps between docs to avoid annoying web sites
	// only does anything if there's no full text
	
	private long _timeOfLastSleep = -1L;	
	
	private void updateInterDocDelayState(DocumentPojo doc, boolean forceDelay)
	{
		// Inter-doc-etiquette
		if ((null == doc.getFullText()) || forceDelay) {
			if (_firstTextExtractionInPipeline) {
				
				if (-1 != _timeOfLastSleep) {
					nInterDocDelay_ms -= (new Date().getTime() - _timeOfLastSleep);
					if (nInterDocDelay_ms <= 0) {
						nInterDocDelay_ms = 1;
					}
				}//TESTED (web_error_test)
				
				//DEBUG
				//System.out.println(">>>>>>>>>>>>>>Getting delay " + nInterDocDelay_ms + " " + _timeOfLastSleep + " VS " + new Date().getTime());
				
				try {
					Thread.sleep(nInterDocDelay_ms);
				}
				catch (InterruptedException e) {}
				
				_timeOfLastSleep = new Date().getTime();
				
				return; // (just use this so can debug print at the bottom)
			}
			else {
				_firstTextExtractionInPipeline = true;
			}
		}
		//DEBUG
		//System.out.println(">>>>Tried but *not* getting delay " + (null == doc.getFullText()) + "&&" + forceDelay + "&&" + delayNeeded);
		
	}//TESTED (first time/not first time: test_*_raw_*_test, forceDelay: test_raw_*, delay: web_error_test)

	////////////////////////////////////////////////////////////////////////////////

	// Handle major errors
	
	private void handleDocOrSourceError(SourcePojo source, DocumentPojo doc, Iterator<DocumentPojo> docIt, Exception e, boolean isMajor) {
		
		// Removing the errored doc
		if (null != docIt) {
			docIt.remove();
			doc.setTempSource(null); // (can safely corrupt this doc since it's been removed)
			
			//extractor can't do anything else today, return
			if (isMajor) {
				// Source error, ignore all other documents
				while (docIt.hasNext()) {
					doc = docIt.next();
					doc.setTempSource(null); // (can safely corrupt this doc since it's been removed)
					docIt.remove();
				}
			}
			// Log doc case
			if (!isMajor) {
				StringBuffer errMessage = HarvestExceptionUtils.createExceptionMessage(e);
				_hc.getHarvestStatus().logMessage(errMessage.toString(), true);
			}
		}
		//TESTED		
		
		// Log/update sources/counts
		_hc.handleExtractError(e, source);
		
	}//TESTED minor+major(null): web_errors_test, major(docs): file_textError
	
	/////////////////////////
	
	// Document splitting logic
	
	private void applyGlobalsToDocumentSplitting(SourcePojo source, SourceSearchFeedConfigPojo links, StringBuffer globalScript, boolean webLinks)
	{
		if (null != globalScript) {
			if ((null != links.getGlobals()) && !links.getGlobals().isEmpty()) {
				links.setGlobals(globalScript.toString() + "\n" + links.getGlobals());
			}
			else {
				links.setGlobals(globalScript.toString());
			}
		}//TESTED
		
		if (webLinks) {
			if (null == source.getRssConfig()) { // (Just set some fields to be used by other)
				source.setRssConfig(new SourceRssConfigPojo());
				
				// Also copy across HTTP control fields:
				source.getRssConfig().setHttpFields(links.getHttpFields());
				source.getRssConfig().setProxyOverride(links.getProxyOverride());
				source.getRssConfig().setUserAgent(links.getUserAgent());
				
				source.getRssConfig().setWaitTimeOverride_ms(links.getWaitTimeBetweenPages_ms());					
	
			}//TESTED
			source.getRssConfig().setSearchConfig(links);
		}
	}//TESTED (non-feed)
	
	private void splitDocuments(DocumentPojo doc, SourcePojo source, SourcePipelinePojo splitter, List<DocumentPojo> docs)
	{
		try {
			if (null == source.getRssConfig()) {
				source.setRssConfig(new SourceRssConfigPojo());
			}
			if (null != source.getRssConfig().getExtraUrls()) { // refreshed ready for new document
				source.getRssConfig().setExtraUrls(null);
			}
			source.getRssConfig().setSearchConfig(splitter.splitter);

			FeedHarvester_searchEngineSubsystem subsys = new FeedHarvester_searchEngineSubsystem();
			subsys.generateFeedFromSearch(source, _hc, doc);
			
			if (null != source.getRssConfig().getExtraUrls()) {
				for (ExtraUrlPojo newDocInfo: source.getRssConfig().getExtraUrls()) {
					DocumentPojo newDoc = new DocumentPojo();
					newDoc.setCreated(new Date());
					newDoc.setModified(newDoc.getCreated());
					newDoc.setUrl(newDocInfo.url);
					newDoc.setTitle(newDocInfo.title);
					newDoc.setDescription(newDocInfo.description);
					newDoc.setFullText(newDocInfo.fullText);
					// Published date is a bit more complex
					if (null != newDocInfo.publishedDate) {
						try {
							newDoc.setPublishedDate(new Date(DateUtility.parseDate(newDocInfo.publishedDate)));						
						}
						catch (Exception e) {}
					}//TESTED (test3,test4)
					if (null == newDoc.getPublishedDate()) {
						newDoc.setPublishedDate(doc.getPublishedDate());
					}//TESTED (test1)
					if (null == newDoc.getPublishedDate()) {
						newDoc.setPublishedDate(doc.getCreated());
					}//TESTED (test2)
					newDoc.setSource(doc.getSource());
					newDoc.setSourceKey(doc.getSourceKey());
					newDoc.setCommunityId(doc.getCommunityId());
					newDoc.setDocGeo(doc.getDocGeo());
					
					newDoc.setSpawnedFrom(splitter);
					docs.add(newDoc);
				}//end loop over URLs
			}//TESTED
		}
		catch (Exception e) {
			StringBuffer errMessage = HarvestExceptionUtils.createExceptionMessage(e);
			_hc.getHarvestStatus().logMessage(errMessage.toString(), true);
		}//TESTED (test4)
		
	}//TESTED (doc_splitter_test, see above for details)
	
}

