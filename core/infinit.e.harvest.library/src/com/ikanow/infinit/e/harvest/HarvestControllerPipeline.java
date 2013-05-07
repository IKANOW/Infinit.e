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

	//TODO - need to copy fields over in order to calculate hash...
	
	public void extractSource_preProcessingPipeline(SourcePojo source, HarvestController hc)
	{
		_hc = hc;
		for (SourcePipelinePojo pxPipe: source.getProcessingPipeline()) { /// (must be non null if here)
			
			// 1] Input source, copy into src pojo
			
			//TODO: set the URL everywhere (do that in the API? do it also here?)
			if (null != pxPipe.database) {
				source.setAuthentication(pxPipe.database.getAuthentication());
				source.setDatabaseConfig(pxPipe.database);
				source.setExtractType("Database");
			}
			if (null != pxPipe.nosql) {
				source.setAuthentication(pxPipe.nosql.getAuthentication());
				source.setNoSql(pxPipe.nosql);
				source.setExtractType("Database");				
			}
			else if (null != pxPipe.file) {
				source.setFileConfig(pxPipe.file);
				source.setExtractType("File");
			}
			else if (null != pxPipe.feed) {
				source.setAuthentication(pxPipe.feed.getAuthentication());
				source.setRssConfig(pxPipe.feed);
				source.setExtractType("Feed");
			}
			else if (null != pxPipe.web) {
				source.setAuthentication(pxPipe.feed.getAuthentication());
				source.setRssConfig(pxPipe.feed);
				source.setExtractType("Feed");
			}
			
			// 2] Globals - nothing to do
			
			// 3] Extraction - link extraction, copy into feed pojo
			
			if ((null != pxPipe.links) && (null != source.getRssConfig())) {
				//TODO (INF-1922) Currently only supported for feed/web
				source.getRssConfig().setSearchConfig(pxPipe.links);
			}
			
			// 3-6] Everything else - just check whether we're going to need UAH/SAH
			
			// UAH
			
			if ((null != pxPipe.contentMetadata) || (null != pxPipe.text)) {
				requiresUnstructuredAnalysis();
			}
			
			// SAH
			
			if ((null != pxPipe.docMetadata) || (null != pxPipe.entities) || (null != pxPipe.assocs) 
					|| (null != pxPipe.storeAndIndex))
			{
				requiresStructuredAnalysis();
			}
			
		}//TOTEST
	}
	
	/////////////////////////////////////////////////////////////////////////////////////////
	//
	// Gets metadata using the extractors and appends to documents
	//
	
	public void enrichSource_processingPipeline(SourcePojo source, List<DocumentPojo> toAdd, List<DocumentPojo> toUpdate, List<DocumentPojo> toRemove)
	{
		// TODO (INF-1922): fill this in with code
		
		for (SourcePipelinePojo pxPipe: source.getProcessingPipeline()) { /// (must be non null if here)
			
			// 1] Is this a starting point:
			
			if ((null != pxPipe.database) || (null != pxPipe.nosql) || (null != pxPipe.file) || (null != pxPipe.feed) || (null != pxPipe.web)) {
				continue; // Already handled this
			}
			
			// 2] Is this a global operation:
			
			if (null != pxPipe.globals) {
				_sah.loadGlobalFunctions(pxPipe.globals.imports, pxPipe.globals.scripts, pxPipe.globals.scriptlang);
					// (will also load them into the UAH if created)
			}
			//TOTEST
			
			if (null != pxPipe.lookupTables) {
				_sah.loadLookupCaches(pxPipe.lookupTables, source.getCommunityIds());
					// (will also load them into the UAH if created)
			}
			//TOTEST
			
			if (null != pxPipe.aliases) {
				//TODO (INF-1922): Not currently supported
			}
			
			// The remainder of these steps are per-document...
			
			if (null == toAdd) 
				continue;
			
			Iterator<DocumentPojo> docIt = toAdd.iterator();
			while (docIt.hasNext()) {
				DocumentPojo doc = docIt.next();
				
				//TODO (INF-1922): Handle inter-doc etiquette...
				
				try {
					
					//TODO (INF-1922) need to handle major extraction errors and delete the doc (and potentially update the source)
					
					// 3] Text and linked document extraction
					
					if (null != pxPipe.links) {
						continue; // This has already been handled if pxPipe.feed or pxPipe.web
									// (not currently supported for the other input types)
					}
				
					if (null != pxPipe.text) {
						//TODO (INF-1922): Handle criteria
						_uah.doManualTextEnrichment(doc, pxPipe.text, source.getRssConfig());
					}
					//TOTEST
					
					if (null != pxPipe.textEngine) { 
						//TODO (INF-1922): Handle criteria
						// Remove existing text if there is any
						doc.setFullText(null);
						source.setUseExtractor("None");
						source.setUseTextExtractor(pxPipe.textEngine.engineName);
						source.setExtractorOptions(pxPipe.textEngine.engineConfig);
						ArrayList<DocumentPojo> docWrapper = new ArrayList<DocumentPojo>(1);
						docWrapper.add(doc);
						_hc.extractTextAndEntities(docWrapper, source, false);
					}
					//TOTEST
					
					// 4] Document level fields
					
					if (null != pxPipe.docMetadata) {
						_sah.setDocumentMetadata(doc, pxPipe.docMetadata);
					}
					//TOTEST
					
					if (null != pxPipe.contentMetadata) {
						_uah.processMetadataChain(doc, pxPipe.contentMetadata);
					}
					//TOTEST
					
					// 5] Entities and Associations
					
					if (null != pxPipe.entities) {
						_sah.setEntities(doc, pxPipe.entities);
					}
					//TOTEST
					
					if (null != pxPipe.assocs) {
						_sah.setAssociations(doc, pxPipe.assocs);
					}
					//TOTEST
					
					if (null != pxPipe.featureEngine) {
						//TODO (INF-1922): Handle criteria
						source.setUseTextExtractor("None");
						//TODO: what about combined text/entity extractors? 
						source.setUseExtractor(pxPipe.featureEngine.engineName);
						source.setExtractorOptions(pxPipe.featureEngine.engineConfig);
						ArrayList<DocumentPojo> docWrapper = new ArrayList<DocumentPojo>(1);
						docWrapper.add(doc);
						_hc.extractTextAndEntities(docWrapper, source, false);
						//TODO: handle batch completion
					}
					//TOTEST
					
					// 6] Is this a finishing step?
					
					if (null != pxPipe.storeAndIndex) {
						//TODO going to need to copy info from md/ents/assocs into here, some fiddly logic to worry about
						//TODO remove fields, specify which to index
					}					
					
					//TODO exceptions:
					
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
			}//end loop over documents			
			
		}//(end loop over pipeline)
	}
	
	////////////////////////////////////////////////////////////////////////////////
	
	// Utility functions
	
	//
	// Called from first loop through the pipeline - determines what needs to be done
	//
	
	protected void requiresStructuredAnalysis() {
		if (null != _sah) {
			_sah = new StructuredAnalysisHarvester();
			_sah.setContext(_hc);
			
			if (null != _uah) {
				_sah.addUnstructuredHandler(_uah);
			}
		}//(only only)
	}
	protected void requiresUnstructuredAnalysis() {
		if (null != _uah) {
			_uah = new UnstructuredAnalysisHarvester();
			_uah.setContext(_hc);
			
			if (null != _sah) {
				_sah.addUnstructuredHandler(_uah);
			}
		}//(only only)
	}
	
}
