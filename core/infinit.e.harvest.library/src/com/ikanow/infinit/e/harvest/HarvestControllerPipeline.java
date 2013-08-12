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
import java.util.Iterator;
import java.util.List;

import javax.script.ScriptException;

import org.json.JSONException;

import com.ikanow.infinit.e.data_model.InfiniteEnums.ExtractorDailyLimitExceededException;
import com.ikanow.infinit.e.data_model.InfiniteEnums.ExtractorDocumentLevelException;
import com.ikanow.infinit.e.data_model.InfiniteEnums.ExtractorSourceLevelException;
import com.ikanow.infinit.e.data_model.InfiniteEnums.ExtractorSourceLevelMajorException;
import com.ikanow.infinit.e.data_model.InfiniteEnums.ExtractorSourceLevelTransientException;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePipelinePojo;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.infinit.e.harvest.enrichment.custom.StructuredAnalysisHarvester;
import com.ikanow.infinit.e.harvest.enrichment.custom.UnstructuredAnalysisHarvester;

public class HarvestControllerPipeline {

	// Objects that persist across the sources

	protected HarvestController _hc;
	protected StructuredAnalysisHarvester _sah;
	protected UnstructuredAnalysisHarvester _uah;

	/////////////////////////////////////////////////////////////////////////////////////////
	//
	// Sets up the input stream into the harvester (and performs various other initialization activities)
	// 

	//TODO (INF-1922) - need to copy fields over in order to calculate hash...(or alternatively
	// just have different hash calcs for px pipeline - I think this is preferable...)
	
	public void extractSource_preProcessingPipeline(SourcePojo source, HarvestController hc)
	{
		_hc = hc;
		for (SourcePipelinePojo pxPipe: source.getProcessingPipeline()) { /// (must be non null if here)
			
			// 1] Input source, copy into src pojo
			
			//TODO (INF-1922) : set the URL everywhere (do that in the API? do it also here?)
			if (null != pxPipe.database) {
				source.setUrl(pxPipe.database.getUrl());
				source.setAuthentication(pxPipe.database.getAuthentication());
				source.setDatabaseConfig(pxPipe.database);
				source.setExtractType("Database");
			}//TOTEST
			if (null != pxPipe.nosql) {
				source.setUrl(pxPipe.nosql.getUrl());
				source.setAuthentication(pxPipe.nosql.getAuthentication());
				source.setNoSql(pxPipe.nosql);
				source.setExtractType("NoSQL");				
			}//TOTEST
			else if (null != pxPipe.file) {
				source.setUrl(pxPipe.file.getUrl());
				source.setFileConfig(pxPipe.file);
				source.setExtractType("File");
			}//TOTEST
			else if (null != pxPipe.feed) {
				//(unset URL here, it will use pxPipe.feed.extraUrls automatically)
				source.setUrl(null);
				source.setAuthentication(pxPipe.feed.getAuthentication());
				source.setRssConfig(pxPipe.feed);
				source.setExtractType("Feed");
			}//TOTEST
			else if (null != pxPipe.web) {
				//(unset URL here, it will use pxPipe.web.extraUrls automatically)
				source.setUrl(null);
				source.setAuthentication(pxPipe.web.getAuthentication());
				source.setRssConfig(pxPipe.web);
				source.setExtractType("Feed"); // (feed/web use the same base extractor, called "Feed" for legacy reasons)
			}//TESTED (basic_web_test*.json)
			
			// 2] Globals - copy across harvest control parameters, if any
			
			if (null != pxPipe.harvest) {
				
				if (null != pxPipe.harvest.maxDocs_global) {
					source.setMaxDocs(pxPipe.harvest.maxDocs_global);
				}
				if (null != pxPipe.harvest.searchCycle_secs) {
					source.setSearchCycle_secs(pxPipe.harvest.searchCycle_secs);
				}
				if (null != pxPipe.harvest.duplicateExistingUrls) {
					source.setDuplicateExistingUrls(pxPipe.harvest.duplicateExistingUrls);
				}
				//TODO (INF-1922): per cycle counts (also add this for legacy source formats, since it's just as easy to)
				
			}//TOTEST
			
			// 3] Extraction - link extraction, copy into feed pojo
			
			if ((null != pxPipe.links) && (null != source.getRssConfig())) {
				//(Currently only supported for feed/web)
				//TODO (INF-1922) How will links handle globals? (just need to make sure it gets the same script engine right? or re-run the globals etc if not...)
				source.getRssConfig().setSearchConfig(pxPipe.links);
			}
			
			// 3-6] Everything else - just check whether we're going to need UAH/SAH
			
			// UAH
			
			if ((null != pxPipe.contentMetadata) || (null != pxPipe.text)) {
				requiresUnstructuredAnalysis();
			}//TESTED
			
			// SAH
			
			if ((null != pxPipe.docMetadata) || (null != pxPipe.entities) || (null != pxPipe.associations) 
					|| (null != pxPipe.searchIndex))
			{
				requiresStructuredAnalysis();
			}//TESTED
			
		}//TOTEST
		
		// Initialize the script engines here:
		if (null != _sah) {
			_sah.intializeScriptEngine();
		}//TOTEST
		if (null != _uah) { // (will just use the SAH's engine if it exists)
			_uah.intializeScriptEngine(null, null);
				// (script engine params get configured in the processing pipeline proper, see below)
		}//TOTEST
		
	}
	
	/////////////////////////////////////////////////////////////////////////////////////////
	//
	// Gets metadata using the extractors and appends to documents
	//
	
	public void enrichSource_processingPipeline(SourcePojo source, List<DocumentPojo> toAdd, List<DocumentPojo> toUpdate, List<DocumentPojo> toRemove)
	{
		//TODO (INF-1922): TOTEST - global pipeline removal elements
		
		Iterator<SourcePipelinePojo> pxPipeIt = source.getProcessingPipeline().iterator(); // (must be non null if here)
		while (pxPipeIt.hasNext()) { 
			SourcePipelinePojo pxPipe = pxPipeIt.next();
				// (note can edit the iterator/list because this bit of the pipeline only runs once, not over each doc)
			
			// 1] Is this a starting point:
			
			if ((null != pxPipe.database) || (null != pxPipe.nosql) || (null != pxPipe.file) || (null != pxPipe.feed) || (null != pxPipe.web) || (null != pxPipe.links))
			{
				pxPipeIt.remove();
				continue; // Already handled these
			}
			
			// 2] Is this a global operation:
			
			if (null != pxPipe.globals) {
				pxPipeIt.remove();
				
				if (null != _sah) {
					_sah.loadGlobalFunctions(pxPipe.globals.imports, pxPipe.globals.scripts, pxPipe.globals.scriptlang);
						// (will also load them into the UAH if created)
				}
				else { // handle case where the SAH is not created but the UAH is...
					
					if ((null == pxPipe.globals.scriptlang) || (pxPipe.globals.scriptlang.equalsIgnoreCase("javascript"))) {
						if (null != pxPipe.globals.scripts) {
							boolean bFirst = true;
							for (String script: pxPipe.globals.scripts) {
								List<String> imports = bFirst ? pxPipe.globals.imports : null;								
								_uah.loadGlobalFunctions(imports, script);
								bFirst = false;
							}
						}
						else {					
							_uah.loadGlobalFunctions(pxPipe.globals.imports, null); 
						}
						//TOTEST (imports only, 1 script, multi scripts, scripts+imports)  
					}
				}//(end SAH+UAH? vs UAH only)
			}
			//TOTEST (all 3 cases sah,uah,sah+uah)
			
			if (null != pxPipe.harvest) {
				pxPipeIt.remove();
				// Already handled
				continue;
			}
			
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
			//TOTEST (all 3 cases sah,uah,sah+uah)
			
			if (null != pxPipe.aliases) {
				pxPipeIt.remove();
				//TODO (INF-1922): Not currently supported
				continue;
			}
		}//(end globals loop over the data)
		
		/**/
		System.out.println("PER DOC PIPELINE ELEMENTS " + source.getProcessingPipeline().size());
		
		if (null == toAdd) 
			return;
		
		// The remainder of these steps are per-document...
			
		Iterator<DocumentPojo> docIt = toAdd.iterator();
		while (docIt.hasNext()) {
			if (null != _uah) {
				_uah.resetForNewDoc();				
			}
			DocumentPojo doc = docIt.next();
			//TODO (INF-1922): Handle inter-doc etiquette...
			
			for (SourcePipelinePojo pxPipe: source.getProcessingPipeline()) { /// (must be non null if here)
				// Save metadata state so we know if we need to re-ingest the document 				
				Object ptr = doc.getMetadata();
				int nCurrMetaFields = 0;
				if ((null != _sah) && (null != ptr)) {
					nCurrMetaFields = doc.getMetadata().size();
				}//TOTEST
				
				try {					
					//TODO (INF-1922) need to handle major extraction errors and delete the doc (and potentially update the source)
					// (including handling no text engine and extractor doesn't support text)
					
					// 3] Text and linked document extraction
					
					if (null != pxPipe.links) {
						continue; // This has already been handled if pxPipe.feed or pxPipe.web
									// (not currently supported for the other input types)
					}
				
					if (null != pxPipe.text) {
						//TODO (INF-1922): here and elsewhere need to decide when to re-serialize the doc...
						_uah.doManualTextEnrichment(doc, pxPipe.text, source.getRssConfig());
					}
					//TESTED (fulltext_regexTests.json, basic_web_uahRawText.json)
					
					if (null != pxPipe.textEngine) { 
						//TODO (INF-1922): Handle criteria
						//TODO (INF-1922): Tika might be a special case? Check out the logic in the UAH
						source.setUseExtractor("None");
						if ((null != pxPipe.textEngine.engineName) && !pxPipe.textEngine.engineName.equalsIgnoreCase("default")) {
							source.setUseTextExtractor(pxPipe.textEngine.engineName);
						}
						if ((null != pxPipe.textEngine.engineName) && !pxPipe.textEngine.engineName.equalsIgnoreCase("raw")) {
							if ((_uah != null) && (null != source.getRssConfig())) {
								_uah.getRawTextFromUrlIfNeeded(doc, source.getRssConfig());
							}
						}//TESTED
						else {
							source.setUseTextExtractor(null);
						}
						
						source.setExtractorOptions(pxPipe.textEngine.engineConfig);
						ArrayList<DocumentPojo> docWrapper = new ArrayList<DocumentPojo>(1);
						docWrapper.add(doc);
						_hc.extractTextAndEntities(docWrapper, source, false);
						
						//TODO: ALWAYS REFRESH ANYTHING THAT DEPENDS ON TEXT HERE (in UAH)
						//TODO: also need to check if the metadata has been modified, reset SAH document if so...
					}
					//TESTED (basic_web_test_ocOptions.json, basic_web_test_textaaOptions.json)
					
					// 4] Document level fields
					
					if (null != pxPipe.docMetadata) {
						_sah.setDocumentMetadata(doc, pxPipe.docMetadata);
						// Always reset the doc information here since the SAH code is welcome to
						// use doc information
						//TODO (INF-1922): would be nice to do something more efficient for this
						_sah.resetDocumentCache();
					}
					//TESTED (fulltext_docMetaTest.json)
					
					if (null != pxPipe.contentMetadata) {
						//TODO (INF-1922): here and elsewhere need to decide when to re-serialize the doc in the UAH...
						_uah.processMetadataChain(doc, pxPipe.contentMetadata, source.getRssConfig());
					}
					//TESTED (fulltext_regexTests.json, basic_web_uahRawText.json)
					
					// 5] Entities and Associations
					
					if (null != pxPipe.entities) {
						_sah.setEntities(doc, pxPipe.entities);
					}
					//TESTED (fulltext_ents_and_assocs.json)
					
					if (null != pxPipe.associations) {
						_sah.setAssociations(doc, pxPipe.associations);
					}
					//TESTED (fulltext_ents_and_assocs.json)
					
					if (null != pxPipe.featureEngine) {
						//TODO (INF-1922): Handle criteria
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
						_hc.extractTextAndEntities(docWrapper, source, false);
						// (Note if no textEngine is used and this engine supports it then also does text extraction - TESTED)
						
						// (Note - don't reset SAH doc cache here, SAH should just operate on doc metadata + content metadata
						//  apart from caches which are maintained locally not in the script engine)
						//TODO (INF-1922): ^^hmm what about if the extraction modifies the metadata ...perhaps we should check on that...
						
						//TODO (INF-1922): handle batch completion (see end of HarvestController.enrichSource)
					}
					//TESTED (basic_web_test_ocOptions.json, basic_web_test_textaaOptions.json)
					
					// 6] Is this a finishing step?
					
					if (null != pxPipe.searchIndex) {
						//TODO (INF-1922): going to need to copy info from md/ents/assocs into here, some fiddly logic to worry about
						//TODO remove fields, specify which to index
					}					
					
					//TODO (INF-1922): handle/log exceptions:
					
				} catch (ExtractorDocumentLevelException e) {
				} catch (ExtractorSourceLevelException e) {
				} catch (ExtractorDailyLimitExceededException e) {
				} catch (ExtractorSourceLevelMajorException e) {
				} catch (ExtractorSourceLevelTransientException e) {
				} catch (JSONException e) {
				} catch (ScriptException e) {
				} catch (IOException e) {
				}
				finally {}
				
				// Check metadata state so we know if we need to re-ingest the document 				
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
				}//TOTEST
				
			}//end loop over per-document processing pipeline elements
			
		}//end loop over documents			
			
	}
	
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
	}//TOTEST (combined sah/uah) (fullText_docMetaTests.json)
	
	protected void requiresUnstructuredAnalysis() {
		if (null == _uah) {
			_uah = new UnstructuredAnalysisHarvester();
			_uah.setContext(_hc);
			
			if (null != _sah) {
				_sah.addUnstructuredHandler(_uah);
			}
		}//(only only)
	}//TOTEST (combined sah/uah) (fulltext_regexTests.json)
	
}
