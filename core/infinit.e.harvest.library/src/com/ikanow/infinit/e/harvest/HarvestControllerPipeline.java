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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.ScriptException;

import org.json.JSONException;

import com.google.common.collect.HashMultimap;
import com.ikanow.infinit.e.data_model.Globals;
import com.ikanow.infinit.e.data_model.InfiniteEnums.ExtractorDailyLimitExceededException;
import com.ikanow.infinit.e.data_model.InfiniteEnums.ExtractorDocumentLevelException;
import com.ikanow.infinit.e.data_model.InfiniteEnums.ExtractorSourceLevelException;
import com.ikanow.infinit.e.data_model.InfiniteEnums.ExtractorSourceLevelMajorException;
import com.ikanow.infinit.e.data_model.InfiniteEnums.ExtractorSourceLevelTransientException;
import com.ikanow.infinit.e.data_model.store.MongoDbUtil;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePipelinePojo;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePipelinePojo.DocumentJoinSpecPojo;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePipelinePojo.MetadataSpecPojo;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo.SourceSearchIndexFilter;
import com.ikanow.infinit.e.data_model.store.config.source.SourceRssConfigPojo;
import com.ikanow.infinit.e.data_model.store.config.source.SourceRssConfigPojo.ExtraUrlPojo;
import com.ikanow.infinit.e.data_model.store.config.source.SourceSearchFeedConfigPojo;
import com.ikanow.infinit.e.data_model.store.config.source.StructuredAnalysisConfigPojo.EntitySpecPojo;
import com.ikanow.infinit.e.data_model.store.document.AssociationPojo;
import com.ikanow.infinit.e.data_model.store.document.ChangeAwareDocumentWrapper;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.infinit.e.data_model.store.document.EntityPojo;
import com.ikanow.infinit.e.harvest.enrichment.custom.JavaScriptUtils;
import com.ikanow.infinit.e.harvest.enrichment.custom.StructuredAnalysisHarvester;
import com.ikanow.infinit.e.harvest.enrichment.custom.UnstructuredAnalysisHarvester;
import com.ikanow.infinit.e.harvest.enrichment.script.CompiledScriptFactory;
import com.ikanow.infinit.e.harvest.enrichment.script.ScriptCallbackNotifier;
import com.ikanow.infinit.e.harvest.enrichment.script.ScriptEngineContextAttributeNotifier;
import com.ikanow.infinit.e.harvest.extraction.document.rss.FeedHarvester_searchEngineSubsystem;
import com.ikanow.infinit.e.harvest.utils.DateUtility;
import com.ikanow.infinit.e.harvest.utils.HarvestExceptionUtils;
import com.ikanow.infinit.e.harvest.utils.PropertiesManager;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/* Note the lifecycle of the pipeline controller is once per thread:
new once per source:
a) initializeState->clearState
b) extractSource_preProcessingPipeline
c) enrichSource_processingPipeline
d) clearState
the reason for the d) is that it frees a boat load of memory up
 */
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
	private CompiledScriptFactory compiledScriptFactory = null;
	
	public CompiledScriptFactory getCompiledScriptFactory() {
		return compiledScriptFactory;
	}

	public void setCompiledScriptFactory(CompiledScriptFactory compiledScriptFactory) {
		this.compiledScriptFactory = compiledScriptFactory;
	}
	protected String _defaultTextExtractor = null;
	
	// Distributed processing bypasses the pipeline and uses the custom processing
	
	protected boolean _bypassHarvestPipeline = false;
	
	// Object initialization:
	
	public void clearState() {
		// (just ensure the memory associated with this can be removed)
		_sah = null;
		_uah = null;	
		_bypassHarvestPipeline = false;
	}
	
	private void intializeState(SourcePojo source)
	{
		clearState();
		
		_defaultTextExtractor = null;
		// (don't re-initialize nInterDocDelay_ms that is always set from the parent hc)
		// (don't re-initialize _props, that can persist)
		// (don't re-initialize doc state like _cachedRawFullText*, _firstTextExtractionInPipeline, _lastDocInPipeline)
		_timeOfLastSleep = -1L; // (not sure about this one, can see an argument for leaving it, but it's consistent with legacy this way)
		
		if (null == _props) { // turns out we always need this, for getMaxTimePerSource()
			_props = new PropertiesManager();	
			nMaxTimeSpentInPipeline_ms = _props.getMaxTimePerSource();
		}
		_hc.initializeCompiledScripts(source);
		setCompiledScriptFactory(_hc.getCompiledScriptFactory());
	}//TESTED (by eye)
	
	/////////////////////////////////////////////////////////////////////////////////////////
	//
	// Sets up the input stream into the harvester (and performs various other initialization activities)
	// 

	private PropertiesManager _props = null;
	
	// branch mapping state
	private Pattern BRANCH_MAP_GET = Pattern.compile("\\$PATH\\(\\s*([^)\\s]+)\\s*\\)", Pattern.CASE_INSENSITIVE);
	private Pattern BRANCH_MAP_SET = Pattern.compile("\\$SETPATH\\(\\s*([^,\\s]+)\\s*,\\s*([^)\\s]+)\\s*\\)", Pattern.CASE_INSENSITIVE);
	private HashMultimap<String, String> _branchMappings = null;
	
	public void extractSource_preProcessingPipeline(SourcePojo source, HarvestController hc)
	{
		// Initialize some variables (since this pipeline can persist):
		
		_hc = hc;
		intializeState(source);
		StringBuffer unindexedFieldList = null;
		
		// Now run:
		
		StringBuffer globalScript = null;
		for (SourcePipelinePojo pxPipe: source.getProcessingPipeline()) { /// (must be non null if here)
			
			// 1] Input source, copy into src pojo
			//(note "extractType" should be normally filled in anyway from SourceHandler.savedSource->SourcePojo.fillInSourcePipelineFields)
			// ^except in test mode)
			
			if (null != pxPipe.criteria) {
				Matcher m = BRANCH_MAP_SET.matcher(pxPipe.criteria);
				if (m.find()) {
					if (null == _branchMappings) {
						_branchMappings = HashMultimap.create();
					}
					Matcher m2 = BRANCH_MAP_GET.matcher(pxPipe.criteria);
					while (m2.find()) {
						_branchMappings.put(m.group(1), m2.group(1));
					}
				}
			}
			//TESTED (complex_criteria_test)

			if (null != pxPipe.database) {
				source.setUrl(pxPipe.database.getUrl());
				source.setAuthentication(pxPipe.database.getAuthentication());
				source.setDatabaseConfig(pxPipe.database);
				source.setExtractType("Database"); 
			}//TESTED (NEED to build a working test file - basic_db_test needs a DB to test against)
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
			else if (null != pxPipe.logstash) {
				source.setExtractType("Logstash");				
			}
			else if (null != pxPipe.federatedQuery) {
				source.setExtractType("Federated");
			}
			// DISTRIBUTED HANDLING:
			else if (null != pxPipe.postProcessing) {
				source.setExtractType("Post_processing");
				_bypassHarvestPipeline = true;
				return;
			}
			else if ((null != pxPipe.docs_datastoreQuery) || (null != pxPipe.docs_documentQuery) ||
					(null != pxPipe.custom_file) || (null != pxPipe.custom_datastoreQuery) ||
					(null != pxPipe.records_indexQuery) || (null != pxPipe.feature_datastoreQuery))
			{
				source.setExtractType("Custom");
				_bypassHarvestPipeline = true;
				return;
			}				
			//(END DISTRIBUTED HANDLING)
			
			
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
				if (null != pxPipe.harvest.timeToLive_days) {
					source.setTimeToLive_days(pxPipe.harvest.timeToLive_days);
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
					//(note "distributionFactor" should be filled in anyway from SourceHandler.savedSource->SourcePojo.fillInSourcePipelineFields)
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
			
			if (null != pxPipe.lookupTables) {
				requiresStructuredAnalysis();
				requiresUnstructuredAnalysis();				
			}
			
			// 3] Extraction - link extraction/document splitting, copy into feed pojo
			
			if (null != pxPipe.links) {
				applyGlobalsToDocumentSplitting(source, pxPipe.links, globalScript, true);
			}
			if (null != pxPipe.splitter) {
				requiresUnstructuredAnalysis();				
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
				
				// Storage settings:
				if (null != pxPipe.contentMetadata) {
					for (SourcePipelinePojo.MetadataSpecPojo meta: pxPipe.contentMetadata) {
						// If don't want to index but _do_ want to store
						if (((null != meta.index) && !meta.index) 
								&& ((null == meta.store) || meta.store))
						{
							if (null == unindexedFieldList) {
								unindexedFieldList = new StringBuffer();
							}
							if (unindexedFieldList.length() > 0) {
								unindexedFieldList.append(',');
							}
							unindexedFieldList.append(meta.fieldName);
						}
					}//TESTED (storageSettings_advanced.json - note DiscardTest1 not included because !meta.store)
				}				
			}//TESTED (see requires* function)
			
			// Joins: always require UAH, usually require SAH (so just grab it)
			if (null != pxPipe.joins) {
				requiresUnstructuredAnalysis();
				requiresStructuredAnalysis();
			}
			
			// SAH
			
			if ((null != pxPipe.docMetadata) || (null != pxPipe.entities) || (null != pxPipe.associations))
			{
				requiresStructuredAnalysis();
			}//TESTED (see requires* function)

			if (null != pxPipe.searchIndex) { // Handles which fields need to be indexed
				source.setSearchIndexFilter(pxPipe.searchIndex);
				//TODO (INF-2223): going to need to copy info from ents/assocs into here, some fiddly logic to worry about (see below for metad)			
			}//TESTED (storageSettings_test)				
			
			if (null != pxPipe.storageSettings) {
				requiresStructuredAnalysis();
			}//TESTED (storageSettings_test)				
			
			// 4] If any of the pipeline elements have criteria then need to turn sah on
			if ((null != pxPipe.criteria) && !pxPipe.criteria.isEmpty()) {
				requiresStructuredAnalysis();
			}//TESTED (basic_criteria_test)
			
		}//TESTED

		if (null != unindexedFieldList) {
			if (null == source.getSearchIndexFilter()) {
				source.setSearchIndexFilter(new SourceSearchIndexFilter());
			}
			if (null == source.getSearchIndexFilter().metadataFieldList) {
				source.getSearchIndexFilter().metadataFieldList = "-" +  unindexedFieldList;
			}//TESTED (storageSettings_advanced.json)
			else if (source.getSearchIndexFilter().metadataFieldList.startsWith("-")) { // add our list to the -ve selections
				if (source.getSearchIndexFilter().metadataFieldList.trim().length() > 1) { // ie not just "-"
					source.getSearchIndexFilter().metadataFieldList = source.getSearchIndexFilter().metadataFieldList + "," +  unindexedFieldList;					
				}//TESTED (storageSettings_advanced_2.json)
				else {
					source.getSearchIndexFilter().metadataFieldList = "-" +  unindexedFieldList;					
				}//TESTED (storageSettings_advanced_2.json, adjusted by hand)
			}
			// (else ignore specific settings since positive selection is being used)
			
			//DEBUG print used for testing
			//System.out.println("INDEX SETTINGS = " + source.getSearchIndexFilter().metadataFieldList);
			
		}//TESTED (storageSettings_advanced.json storageSettings_advanced_2.json)				
		
		// Initialize the script engines here:
		if (null != _sah) {
			_sah.intializeScriptEngine(compiledScriptFactory);			
		}//TESTED (imports_and_lookup_test_uahSah)
		if (null != _uah) { // (will just use the SAH's engine if it exists)
			_uah.intializeScriptEngine(null, null,compiledScriptFactory);
				// (script engine params get configured in the processing pipeline proper, see below)
		}//TESTED (imports_and_lookup_test)
		
	}
	
	/////////////////////////////////////////////////////////////////////////////////////////
	//
	// Gets metadata using the extractors and appends to documents
	// *** IMPORTANT NOTE: ONLY USE SOURCE FOR ITS PIPELINE - FOR PER-DOC SRC USE DocumentPojo.getTempSource
	//
	
	// Per document member variables used to pass between main loop and utilities:
	private String _cachedRawFullText = null;
	private boolean _cachedRawFullText_available = true; // (set this latch if I never see the full text, eg non-raw text engine is called)
	private boolean _firstTextExtractionInPipeline = false;
	private boolean _lastDocInPipeline = false;
	
	public void enrichSource_processingPipeline(SourcePojo source, List<DocumentPojo> toAdd, List<DocumentPojo> toUpdate, List<DocumentPojo> toRemove)
	{
		if (_bypassHarvestPipeline) {
			return;
		}		
		boolean multiSearchIndexCheck = false; // (just to add a handy warning while testing)
		
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
					_sah.loadLookupCaches(pxPipe.lookupTables, source.getCommunityIds(), source.getOwnerId());
						// (will also load them into the UAH if created)
				}
				else { // UAH specified but SAH not					
					_uah.loadLookupCaches(pxPipe.lookupTables, source.getCommunityIds(), source.getOwnerId());
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
				// Add some warnings:
				if (null != pxPipe.criteria) { // This is always global, but just use
					if (_hc.isStandalone()) { // log a message
						_hc.getHarvestStatus().logMessage("Warning: all searchIndex elements are global, criteria field ignored", true);
					}
				}//TESTED (search_index_warnings_test)
				if (multiSearchIndexCheck) {
					if (_hc.isStandalone()) { // log a message
						_hc.getHarvestStatus().logMessage("Warning: only one searchIndex element is supported (the last one specified)", true);
					}					
				}//TESTED (search_index_warnings_test)
				multiSearchIndexCheck = true;
				
				pxPipeIt.remove();
				// Otherwise already handled
				continue;
			}//TESTED (storageSettings_test)
			
		}//(end globals loop over the data)
		
		if (null == toAdd) 
			return;
		
		// The remainder of these steps are per-document...
			
		ListIterator<DocumentPojo> docIt = toAdd.listIterator();
		_firstTextExtractionInPipeline = false;
	
		long pipelineStartTime = new Date().getTime();

		HashSet<String> unstoredFields = new HashSet<String>();
		
		int error_on_feed_count = 0, feed_count = 0;
		LinkedList<DocumentPojo> splitterList = null;
		for (docIt.hasNext();;) {			
			DocumentPojo doc = null;
			HashSet<String> currentBranches = null;
			unstoredFields.clear();
			
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
				// NOTE: this will leave "deleted" docs (eg by sourceKey or sourceUrl) ... these will
				// then be re-imported later on - not ideal of course, but there's no way round that
				// without re-architecting use of _id and updateId.
				
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
					
				// create document wrapper
				ChangeAwareDocumentWrapper caDoc = new ChangeAwareDocumentWrapper(doc);
				ScriptCallbackNotifier attributeChangeListener = new ScriptCallbackNotifier(compiledScriptFactory,JavaScriptUtils.setDocumentAttributeScript,JavaScriptUtils.docAttributeName,JavaScriptUtils.docAttributeValue);
				caDoc.setAttributeChangeListener(attributeChangeListener);
				ScriptEngineContextAttributeNotifier dirtyChangeListener = new ScriptEngineContextAttributeNotifier(compiledScriptFactory,"_dirtyDoc",true);
				caDoc.setDirtyChangeListener(dirtyChangeListener);

				for (SourcePipelinePojo pxPipe: source.getProcessingPipeline()) { /// (must be non null if here)
					//DEBUG
					//System.out.println("PX EL: " + pxPipe.display + ", " + processSpawnedDocOrNotSpawnedDoc + ", " + doc.getUrl() + ": " + toAdd.size());
					
					// Spawned documents only enter at their spot in the pipeline:
					if (!processSpawnedDocOrNotSpawnedDoc) {
						if (pxPipe == caDoc.getSpawnedFrom()) { // (intentionally ptr ==)
							processSpawnedDocOrNotSpawnedDoc = true; // (next pipeline element, start processing)
						}
						continue; // (skip past elements, including the spawnee)
						
					}//TESTED (doc_splitter_test);
					
					// Run criteria for this pipeline element:
					if ((null != pxPipe.criteria) && !pxPipe.criteria.isEmpty()) {
						// Check branches (read)
						boolean moddedCriteria = false;
						String newCriteria = pxPipe.criteria;
						
						Matcher m1 = this.BRANCH_MAP_GET.matcher(newCriteria);
						boolean modCriteria = false;
						
						boolean branchMismatch = false;
						while (m1.find()) {
							modCriteria = true;
							if ((null == currentBranches) || !currentBranches.contains(m1.group(1))) {
								branchMismatch = true;
								break;
							}
						}
						if (branchMismatch) {
							continue;
						}
						if (modCriteria) {
							newCriteria = m1.replaceAll("");
							moddedCriteria = true;
						}
						//TESTED (complex_criteria_test)
												
						// Check branches (write)
						String branchYes = null;
						String branchNo = null;
						Matcher m2 = BRANCH_MAP_SET.matcher(newCriteria);
						modCriteria = false;
						if (m2.find()) {
							modCriteria = true;
							branchYes = m2.group(1);
							branchNo = m2.group(2);
						}						
						if (modCriteria) {
							newCriteria = m2.replaceAll("");
							moddedCriteria = true;
						}
						//TESTED (complex_criteria_test)
						
						if (!moddedCriteria || !newCriteria.isEmpty()) {
							if (!newCriteria.startsWith("$SCRIPT")) {
								newCriteria= "$SCRIPT(" + newCriteria + ")";
							}//TESTED (basic_criteria_test)
							
							if ((null != branchYes) && (null == currentBranches)) {
								currentBranches = new HashSet<String>();
							}
							
							if (!_sah.rejectDoc(newCriteria, caDoc, false)) {
								if (null != branchNo) {
									currentBranches.add(branchNo);								
									Set<String> parentBranches = this._branchMappings.get(branchNo);
									if (null != parentBranches) {
										currentBranches.addAll(parentBranches);
									}
								}
								continue;
							}
							else {
								if (null != branchYes) {
									currentBranches.add(branchYes);								
									Set<String> parentBranches = this._branchMappings.get(branchYes);
									if (null != parentBranches) {
										currentBranches.addAll(parentBranches);
									}
								}
							}
							//TESTED (complex_criteria_test)
						}
					}//TESTED (basic_criteria_test)
					
					try {					
						// 3] Create new documents from existing ones
						
						if (null != pxPipe.splitter) {
							if (null == splitterList) {
								splitterList = new LinkedList<DocumentPojo>();
							}
							try {
								// DO not remove until unless you want to debug for 2 days
								//ScriptUtil.printEngineState(compiledScriptFactory);
								splitDocuments(caDoc, source, pxPipe, splitterList);
							}
							catch (Exception e) {} // do nothing, still want to keep doc unless otherwise specified below
							
							if ((null == pxPipe.splitter.getDeleteExisting()) || pxPipe.splitter.getDeleteExisting()) {
								// Don't keep original doc
								docIt.remove();
								caDoc.setTempSource(null); // (can safely corrupt this doc since it's been removed)
								break;								
							}//TESTED (test1,test2)
							
						}//TESTED (doc_splitter)

						// 4] Text and linked document extraction
						
						if (null != pxPipe.text) {
							// IN: doc (xpath/regex) or json(doc) (js)
							// OUT: doc.fullText, doc.title, doc.desc, (less common) doc.metadata.*
							// POST: reset
	
							updateInterDocDelayState(caDoc, false);
							
							String cachedFullText = _uah.doManualTextEnrichment(caDoc, pxPipe.text, source.getRssConfig());
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
													
							if (!handleTextEngine(pxPipe, caDoc, source)) {
								error_on_feed_count++;
								
								if ((null == pxPipe.textEngine.exitOnError) || pxPipe.textEngine.exitOnError) {
									caDoc.setTempSource(null); // (can safely corrupt this doc since it's been removed)
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
							
							_sah.setDocumentMetadata(caDoc, pxPipe.docMetadata);
							_sah.resetDocumentCache();
						}
						//TESTED (fulltext_docMetaTest.json)
						
						if (null != pxPipe.contentMetadata) {
							// IN: doc (xpath/regex) or json(doc) (js)
							// OUT: doc.meta.*
							// POST: reset
							
							updateInterDocDelayState(caDoc, false);
							
							// DO not remove until unless you want to debug for 2 days
							//ScriptUtil.printEngineState(compiledScriptFactory);
							_uah.processMetadataChain(caDoc, pxPipe.contentMetadata, source.getRssConfig(), unstoredFields);
							if (null != _sah) {
								_sah.resetDocumentCache();				
							}
							
							// Cache the full text if available
							if ((null == _cachedRawFullText) && _cachedRawFullText_available) {
								_cachedRawFullText = caDoc.getFullText();
							}//(TESTED: ((cache available) text_content_then_raw_to_boilerpipe (not available) text_default_then_content_then_default_test.json)
						}
						//TESTED (fulltext_regexTests.json, basic_web_uahRawText.json)
						
						if (null != pxPipe.joins) {
							handleJoins(caDoc, pxPipe.joins);
						}
						
						// 6] Entities and Associations
						
						if (null != pxPipe.entities) {
							// IN: sah.doc.*, sah.doc.metadadata.*, 
								//(recalculate from scratch then use: sah.entityMap, sah.geoMap)
							// OUT: doc.entities, sah.entityMap, sah.geoMap
							// POST: no need to reset anything, sah.entities never read 
							
							_sah.setEntities(caDoc, pxPipe.entities);
						}
						//TESTED (fulltext_ents_and_assocs.json)
						
						if (null != pxPipe.associations) {
							// IN: sah.doc.*, sah.doc.metadadata.*, doc.entities, sah.entityMap, sah.geoMap
							// OUT: doc.associations
							// POST: no need to reset anything, sah.associations never read	
							
							_sah.setAssociations(caDoc, pxPipe.associations);
						}
						//TESTED (fulltext_ents_and_assocs.json)
						
						if (null != pxPipe.featureEngine) {
							// IN: doc
							// OUT: doc.* 
							// POST: reset sah ent cache (_should_ change only metadata, ents and assocs so don't need to reset sah doc cache)  
							
							if (!handleFeatureEngine(pxPipe, caDoc, source)) {
								error_on_feed_count++;
								
								if ((null == pxPipe.featureEngine.exitOnError) || pxPipe.featureEngine.exitOnError) {
									caDoc.setTempSource(null); // (can safely corrupt this doc since it's been removed)
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
							
							if (!handleStorageSettings(pxPipe, caDoc)) {
								// (this is a manual rejection not an error so we're good)
								caDoc.setTempSource(null); // (can safely corrupt this doc since it's been removed)
								
								if ((null != pxPipe.storageSettings.deleteExistingOnRejection) && pxPipe.storageSettings.deleteExistingOnRejection) {
									// Use another field to indicate that the doc has not only been rejected, it's going to delete the original also...
									caDoc.setExplain(pxPipe.storageSettings); // otherwise will always be null
								}//TESTED (under harvest_post_processor testing)								
								
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
/*					if ((null != pxPipe.featureEngine) || (null != pxPipe.textEngine)) {
						Object ptrAfter = caDoc.getMetadata();
						int nCurrMetaFieldsAfter = 0;
						if (null != _sah) {
							if (null != ptrAfter) {
								nCurrMetaFieldsAfter = caDoc.getMetadata().size();						
							}
							if ((ptr != ptrAfter) || (nCurrMetaFieldsAfter != nCurrMetaFields))
							{
								_sah.resetDocumentCache();
							}
						}
					}//TESTED (metadata_doc_cache_reset) */
					
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
				//e.printStackTrace();
				
				error_on_feed_count++;
				this.handleDocOrSourceError(source, doc, docIt, e, false);	
				// (don't break)
			} //TESTED (web_errors_test)
			finally {}

			if (!unstoredFields.isEmpty()) {
				if (null != doc.getMetadata()) {
					for (String fieldToDelete: unstoredFields) {
						doc.getMetadata().remove(fieldToDelete);
					}
				}
			} //TESTED (storageSettings_advanced.json)
			
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
						if (e instanceof SecurityException) { // (worthy of further logging)
							_hc.getHarvestStatus().logMessage(e.getMessage(), true);
						}//TESTED
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

		// Apply entity filter if present
		HashSet<String> removedEntities = null;
		if ((null != pxPipe.featureEngine.entityFilter) && !pxPipe.featureEngine.entityFilter.isEmpty()) {
			if ((null != doc.getEntities()) && !doc.getEntities().isEmpty()) {
				boolean assocsToRemove = (null != doc.getAssociations()) && !doc.getAssociations().isEmpty();
				boolean includeOnly = true;
				if (pxPipe.featureEngine.entityFilter.startsWith("-")) {
					includeOnly = false;
				}//TOTEST (hand tested, script TBD)
				if (null == pxPipe.featureEngine.entityRegex) {
					String toRegex = pxPipe.featureEngine.entityFilter;
					if (pxPipe.featureEngine.entityFilter.startsWith("-") || pxPipe.featureEngine.entityFilter.startsWith("+")) {
						toRegex = pxPipe.featureEngine.entityFilter.substring(1);
					}
					pxPipe.featureEngine.entityRegex = Pattern.compile(toRegex, Pattern.CASE_INSENSITIVE);
				}//TOTEST (hand tested, script TBD)
				Iterator<EntityPojo> it = doc.getEntities().iterator();
				while (it.hasNext()) {
					String index = it.next().getIndex();
					boolean remove = false;
					if (pxPipe.featureEngine.entityRegex.matcher(index).find()) { // found
						remove = !includeOnly;
					}//TOTEST (hand tested, script TBD)
					else { // not found
						remove = includeOnly;
					}//TOTEST (hand tested, script TBD)
					if (remove) { // exclude
						it.remove();
						if (assocsToRemove) {
							if (null == removedEntities) {
								removedEntities = new HashSet<String>();
							}
							removedEntities.add(index);
						}
					}//TOTEST (hand tested, script TBD)
				}//(end loop over entities)
			}//(end if has entities)
		}//(end if has entity filter)
		
		if ((null != pxPipe.featureEngine.assocFilter) || (null != removedEntities)) {
			if ((null != doc.getAssociations()) && !doc.getAssociations().isEmpty()) {
				boolean includeOnly = true;
				if ((null != pxPipe.featureEngine.assocFilter) && !pxPipe.featureEngine.assocFilter.isEmpty()) {
					if (pxPipe.featureEngine.assocFilter.startsWith("-")) {
						includeOnly = false;
					}//TOTEST
					if (null == pxPipe.featureEngine.assocRegex) {
						String toRegex = pxPipe.featureEngine.assocFilter;
						if (pxPipe.featureEngine.assocFilter.startsWith("-") || pxPipe.featureEngine.assocFilter.startsWith("+")) {
							toRegex = pxPipe.featureEngine.assocFilter.substring(1);
						}
						pxPipe.featureEngine.assocRegex = Pattern.compile(toRegex, Pattern.CASE_INSENSITIVE);
					}//TOTEST
				}
				Iterator<AssociationPojo> it = doc.getAssociations().iterator();
				while (it.hasNext()) {
					AssociationPojo assoc = it.next();
					boolean removed = false;
					boolean matched = (null == pxPipe.featureEngine.assocRegex); // (ie always match if no regex spec'd)
					for (String index: Arrays.asList(assoc.getEntity1_index(), assoc.getEntity2_index(), assoc.getGeo_index())) {
						if (null != index) {
							if ((null != removedEntities) && removedEntities.contains(index)) {
								it.remove();
								removed = true;
								break;
							}//TOTEST (hand tested, script TBD)
							if (null != pxPipe.featureEngine.assocRegex) {
								boolean remove = false;
								if (pxPipe.featureEngine.assocRegex.matcher(index).find()) { // found
									matched = true;
									remove = !includeOnly;
								}//TOTEST (hand tested, script TBD)
								if (remove) { // exclude
									it.remove();
									removed = true;
									break;
								}//TOTEST (hand tested, script TBD)						
							}
						}//(end if index present)
					}//(end loop over indexes)
					if (removed) {
						continue;
					}
					// Verb cat:
					if ((null != pxPipe.featureEngine.assocRegex) && (null != assoc.getVerb_category())) {
						boolean remove = false;
						if (pxPipe.featureEngine.assocRegex.matcher(assoc.getVerb_category()).find()) { // found
							matched = true;
							remove = !includeOnly;
						}//TOTEST (hand tested, script TBD)
						if (remove) { // exclude
							it.remove();
							continue;
						}//TOTEST (hand tested, script TBD)						
					}
					// Verb
					if ((null != pxPipe.featureEngine.assocRegex) && (null != assoc.getVerb())) {
						boolean remove = false;
						if (pxPipe.featureEngine.assocRegex.matcher(assoc.getVerb()).find()) { // found
							matched = true;
							remove = !includeOnly;
						}//TOTEST (hand tested, script TBD)
						if (remove) { // exclude
							it.remove();
							continue;
						}//TOTEST (hand tested, script TBD)
					}
					if (includeOnly && !matched) {
						it.remove();
					}//TESTED (hand tested, script TBD)
					
				}//(end loop over associations)
			}//(end if associations present)
		}//(end if association filter present, or entity filter removed entries so need to sync)
		
		// Handle batch completion (needs to happen before docWrapper.isEmpty()==error check)
		if (_lastDocInPipeline) {
			//TODO (INF-2223): Entity/Assoc filter not applied in batch case (of course lots of batch things don't really work in pipelines...)
			_hc.extractTextAndEntities(null, source, true, true); 
		}//TESTED (featureEngine_batch_test)
		
		if (docWrapper.isEmpty()) { // Then this document has errored and needs to be removed
			return false;
		}//TESTED (featureEngine_batch_test, remove textEngine stage)
		
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
			_sah.intializeScriptEngine(compiledScriptFactory);
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
			_uah.intializeScriptEngine(null, null, compiledScriptFactory);

			
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
			
			HashMap<String, Object> jsonLookup = new HashMap<String, Object>();
			if ((null != splitter.splitter.getScriptlang()) && splitter.splitter.getScriptlang().startsWith("automatic"))
			{
				// (automatic or automatic_json or automatic_xml)
				
				String[] args = splitter.splitter.getScript().split("\\s*,\\s*");
				Object[] objList = null;		
				
				String field = args[0];
				if (field.startsWith(DocumentPojo.fullText_)) { // fullText, or fullText.[x] where [x] is the root value
					
					DocumentPojo dummyDoc = new DocumentPojo();
					dummyDoc.setFullText(doc.getFullText());							
					MetadataSpecPojo dummyContent = new MetadataSpecPojo();
					dummyContent.fieldName = "extract";
					dummyContent.scriptlang = "stream";
					dummyContent.flags = "o";
					
					if (field.equals(DocumentPojo.fullText_)) { // fullText
						dummyContent.script = "";
					}
					else {
						dummyContent.script = field.substring(1 + DocumentPojo.fullText_.length()); //+1 for the "."
					}
					_uah.processMetadataChain(dummyDoc, Arrays.asList(dummyContent), source.getRssConfig(), null);
					
					BasicDBObject dummyDocDbo = (BasicDBObject) dummyDoc.toDb();
					dummyDocDbo = (BasicDBObject) dummyDocDbo.get(DocumentPojo.metadata_);
					if (null != dummyDocDbo) {
						objList = ((Collection<?>)(dummyDocDbo.get("extract"))).toArray(); // (returns a list of strings)
					}
				}//TESTED (doc_splitter_test_auto_json, json: test3, xml: test4)
				else if (field.startsWith(DocumentPojo.metadata_)) { // field starts with "metadata."
					objList = doc.getMetadata().get(field.substring(1 + DocumentPojo.metadata_.length())); //+1 for the "."					
				}//TESTED (doc_splitter_test_auto_json, test1)
				else { // direct reference to metadata field
					objList = doc.getMetadata().get(field);
				}//TESTED (doc_splitter_test_auto_json, test2)
				
				if ((null != objList) && (objList.length > 0)) {
					source.getRssConfig().setExtraUrls(new ArrayList<ExtraUrlPojo>(objList.length));
					int num = 0;
					for (Object o: objList) {
						num++;
						ExtraUrlPojo url = new ExtraUrlPojo();
						if ((1 == args.length) || !(o instanceof DBObject)) { // generate default URL
							url.url = doc.getUrl() + "#" + num;							
						}//TESTED (doc_splitter_test_auto_json, test1)
						else if (2 == args.length) { // url specified in the format <fieldname-in-dot-notation>
							url.url = MongoDbUtil.getProperty((DBObject)o, args[1]);
						}//TESTED (doc_splitter_test_auto_json, test2)
						else { // url specified in format <message-format-with-{1}-{2}-etc>,<fieldname-in-dot-notation-for-1>,..
							ArrayList<Object> cmdArgs = new ArrayList<Object>(args.length - 1); //-2 + 1 (+1 - see below)
							cmdArgs.add("[INDEX_FROM_1_NOT_0]");
							for (int j = 2; j < args.length; ++j) {
								cmdArgs.add(MongoDbUtil.getProperty((DBObject)o, args[j]));
							}
							url.url = MessageFormat.format(args[1], cmdArgs.toArray());
						}//TESTED (doc_splitter_test_auto_json, test3, test4)
						
						if (null == url.url) { // (if we can't extract a URL then bail out)
							continue;
						}
						
						url.title = new StringBuffer(doc.getTitle()).append(" (").append(num).append(")").toString();
						url.fullText = o.toString();
						source.getRssConfig().getExtraUrls().add(url);	
						if (splitter.splitter.getScriptlang().startsWith("automatic_")) { // automatic_json or automatic_xml
							jsonLookup.put(url.url, o);
						}
					}
				}//TESTED (doc_splitter_test_auto_json)
			}
			else { // normal case - run the 'follow web links' code to get the docs
				source.getRssConfig().setSearchConfig(splitter.splitter);
	
				FeedHarvester_searchEngineSubsystem subsys = new FeedHarvester_searchEngineSubsystem(source, _hc);

				// DO not remove until unless you want to debug for 2 days
				//ScriptUtil.printEngineState(compiledScriptFactory);
				subsys.generateFeedFromSearch(source, _hc, doc);				
			}			
			if (null != source.getRssConfig().getExtraUrls()) {
				for (ExtraUrlPojo newDocInfo: source.getRssConfig().getExtraUrls()) {
					if (null == doc.getSourceUrl()) { // (if sourceUrl != null, bypass it's because it's been generated by a file so is being deleted anyway)
						//(note: this null check above is relied upon by the federated query engine, so don't go randomly changing it!) 
						
						if (_hc.getDuplicateManager().isDuplicate_Url(newDocInfo.url, source, null)) {
							//TODO: should handle updateCycle_secs?
							continue;
						}
					}					
					DocumentPojo newDoc = new DocumentPojo();
					newDoc.setCreated(doc.getCreated());
					newDoc.setModified(doc.getModified());
					newDoc.setUrl(newDocInfo.url);
					newDoc.setTitle(newDocInfo.title);
					newDoc.setDescription(newDocInfo.description);
					newDoc.setFullText(newDocInfo.fullText);
					
					// For JSON, also create the metadata)
					if (null != splitter.splitter.getScriptlang()) {
						if (splitter.splitter.getScriptlang().equals("automatic_json")) {
							newDoc.addToMetadata("json", jsonLookup.get(newDoc.getUrl()));
						}
						else if (splitter.splitter.getScriptlang().equals("automatic_xml")) {
							Object obj = jsonLookup.get(newDoc.getUrl());
							if (obj instanceof DBObject) {
								DBObject dbo = (DBObject) obj;
								for (String key: dbo.keySet()) {
									Object objArray = dbo.get(key);
									if (objArray instanceof Object[]) {
										newDoc.addToMetadata(key, (Object[])objArray);
									}	
									else if (objArray instanceof Collection<?>) {
										newDoc.addToMetadata(key, ((Collection<?>)objArray).toArray());
									}
								}
							}//(test4)
						}
					}//TESTED (doc_splitter_test_auto_json, test1:json, test4:xml)
					
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
					newDoc.setTempSource(source);
					newDoc.setSource(doc.getSource());
					newDoc.setMediaType(doc.getMediaType());
					newDoc.setSourceKey(doc.getSourceKey());
					newDoc.setSourceUrl(doc.getSourceUrl()); // (otherwise won't be able to delete child docs that come from a file)
					newDoc.setCommunityId(doc.getCommunityId());
					newDoc.setDocGeo(doc.getDocGeo());
					newDoc.setIndex(doc.getIndex());
					
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
	
	private void handleJoins(DocumentPojo doc, List<DocumentJoinSpecPojo> joins) {
		// OK we're going to hack this up via existing function calls, even though it's not very efficient...
		try {
			StructuredAnalysisHarvester joinSah = new StructuredAnalysisHarvester();
			List<EntitySpecPojo> ents = new ArrayList<EntitySpecPojo>(joins.size());

			for (DocumentJoinSpecPojo join: joins) {
				// First off, we need to go get the list of keys to use in the look-up:						
				EntitySpecPojo entitySpecPojo = new EntitySpecPojo();
				entitySpecPojo.setIterateOver(join.iterateOver);
				if (null != join.accessorScript) { 
					//TODO (INF-2479): doesn't have access to globals?
					entitySpecPojo.setDisambiguated_name(join.accessorScript);
				}
				else { // default, hope this works...
					entitySpecPojo.setDisambiguated_name("$value");				
				}
				entitySpecPojo.setType(join.toString());
				entitySpecPojo.setDimension("What");
				ents.add(entitySpecPojo);
			}
			DocumentPojo dummyDoc = new DocumentPojo();
			dummyDoc.setMetadata(doc.getMetadata());
			joinSah.setEntities(dummyDoc, ents);
		}
		catch (Exception e) {
			_hc.getHarvestStatus().logMessage(Globals.populateStackTrace(new StringBuffer("handleJoins: "), e).toString(), true);			
		}
	}//TODO: in progress
}

