/*******************************************************************************
 * Copyright 2012 The Infinit.e Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.ikanow.infinit.e.data_model.store.config.source;

import java.util.LinkedHashMap;
import java.util.List;

import org.bson.types.ObjectId;

import com.google.gson.reflect.TypeToken;
import com.ikanow.infinit.e.data_model.store.BaseDbPojo;

public class SourcePipelinePojo extends BaseDbPojo {
	// Standard static function for readability
	@SuppressWarnings("unchecked")
	static public TypeToken<List<SourcePipelinePojo>> listType() { return new TypeToken<List<SourcePipelinePojo>>(){}; } 

	// 0] Common fields:
	public String display;
	
	// 1] Pipeline elements:
	
	// 1.1] Starting points: 
	
	public SourceDatabaseConfigPojo database;
	public SourceFileConfigPojo file;
	public SourceRssConfigPojo feed;
	public SourceRssConfigPojo web;
	public SourceNoSqlConfigPojo nosql = null; 

	// 1.2] Global operations
	
	public GlobalScriptPojo globals;
	public LinkedHashMap<String, ObjectId> lookupTables;
	public LinkedHashMap<String, ObjectId> aliases;
	public HarvestControlSettings harvest;
	
	// 1.3] Text and Linked-Document extraction
	
	public SourceSearchFeedConfigPojo links;
	public List<ManualTextExtractionSpecPojo> text;
	public AutomatedTextExtractionSpecPojo textEngine;
	
	// 1.4] Document-level field (including metadata extraction)
	
	public DocumentSpecPojo docMetadata;
	public List<MetadataSpecPojo> contentMetadata;
	
	// 1.5] Entities and Associations

	public AutomatedEntityExtractionSpecPojo featureEngine;
	public List<StructuredAnalysisConfigPojo.EntitySpecPojo> entities;
	public List<StructuredAnalysisConfigPojo.AssociationSpecPojo> associations;
	//TODO (INF-1922): ^^^need to add store/index to these guys
	
	// 1.6] Finishing steps
	
	public SourcePojo.SourceSearchIndexFilter searchIndex;
	public StorageSettingsPojo storageSettings;
	
	////////////////////////////////////////////////////////
	
	// 2] Sub-classes:
	
	// 2.2] Global operations
	
	public static class GlobalScriptPojo {
		public List<String> imports; // An optional list of URLs that get imported before the scripts below are run
		public List<String> scripts; // A list of (java)script code blocks that are evaluated in order (normally only need to specify one)
		public String scriptlang; // Currently only "javascript" is supported
	}
	
	public static class HarvestControlSettings {
		//TODO (INF-1922): support this in the code (Also have + enforce global max time setting)
		public Integer searchCycle_secs; // How often to run the harvester (copied to SourcePojo when published)
		public Boolean duplicateExistingUrls; // If false (defaults to true) then documents matching the URL of any existing document in the community is ignored (copied to SourcePojo when published)
		
		public Integer maxDocs_global; // If specified, limits the number of documents that can be harvested for a given source (state remains in SUCCESS_ITERATION until harvest complete - this is to limit time/resource consumption)
		public Integer throttleDocs_perCycle; // If specified, limits the number of documents that can be harvested for a given source (state moves to SUCCESS - ie this+searchCycle_secs limits document ingest rate)
		public Integer maxDocs_perCycle; // If specified, limits the number of documents that can be harvested for a given source (state moves to SUCCESS - ie this+searchCycle_secs limits document ingest rate)
		
		public Integer distributionFactor; // (EXPERIMENTAL) If specified, attempts to distribute the source across many threads
			//TODO (INF-1884): ^^^ add to GUI
	}
	
	// 2.3] Text and Linked-Document extraction
	
	public static class AutomatedTextExtractionSpecPojo {
		public String criteria; // A javascript expression that is passed the document as _doc - if returns false then this pipeline element is bypassed
		public String engineName; // The name of the text engine to use (can be fully qualified (eg "com.ikanow.infinit.e.harvest.boilerpipe"), or just the name (eg "boilerpipe") if the engine is registered in the Infinit.e system configuration)
		public LinkedHashMap<String, String> engineConfig; // The configuration object to be passed to the engine
		
	}
	
	public static class ManualTextExtractionSpecPojo {
		public String fieldName; // One of "fullText", "description", "title"
		public String script; // The script/xpath/javascript expression (see scriptlang below)
		public String flags; // Standard Java regex field (regex/xpath only), plus "H" to decode HTML
		public String replacement; // Replacement string for regex/xpath+regex matches, can include capturing groups as $1 etc
		public String scriptlang; // One of "javascript", "regex", "xpath"
		//(note headers and footers are no longer supported - you can just do this manually anyway now)
	}
	
	// 2.4] Document-level field (including metadata extraction)
	
	public static class DocumentSpecPojo {
		public String title; // The string expression or $SCRIPT(...) specifying the document title
		public String description; // The string expression or $SCRIPT(...) specifying the document description
		public String publishedDate; // The string expression or $SCRIPT(...) specifying the document publishedDate
		public String fullText; // The string expression or $SCRIPT(...) specifying the document fullText
		public String displayUrl; // The string expression or $SCRIPT(...) specifying the document displayUrl
		public Boolean appendTagsToDocs; // if true (*NOT* default) source tags are appended to the document 
		public StructuredAnalysisConfigPojo.GeoSpecPojo docGeo; // Specify a document level geo-tag
	}
	
	public static class MetadataSpecPojo {
		public String fieldName; // Any string, the key for generated array in "doc.metadata"
		public String scriptlang; // One of "javascript", "regex", "xpath"
		public String script; // The script that will generate the array in "doc.metadata" (under fieldName)
		public String flags; // Standard Java regex field (regex/xpath only), plus "H" to decode HTML, "D": will allow duplicate strings (by default they are de-duplicated), plus the following custom flags:
								// For javascript (defaults to "t" if none specified), "t" the script receives the doc fullText ("text"), "d" the script receives the entire doc (_doc), "m" the script receives the doc.metadata (_metadata)
								// For xpath: "o": if the XPath expression points to an HTML (/XML) object, then this object is converted to JSON and stored as an object in the corresponding metadata field array. (Can also be done via the deprecated "groupNum":-1)
		public String replace; // Replacement string for regex/xpath+regex matches, can include capturing groups as $1 etc
		public Boolean store; // Whether this field should be stored in the DB or discarded after the harvet processing
		public Boolean index; // Whether this field should be full-text indexed or just stored in the DB
	}
	
	// 2.5] Entities and Associations
	
	public static class AutomatedEntityExtractionSpecPojo {
		public String criteria; // A javascript expression that is passed the document as _doc - if returns false then this pipeline element is bypassed
		public String engineName; // The name of the text engine to use (can be fully qualified (eg "com.ikanow.infinit.e.harvest.boilerpipe"), or just the name (eg "boilerpipe") if the engine is registered in the Infinit.e system configuration)
		public LinkedHashMap<String, String> engineConfig; // The configuration object to be passed to the engine
		public String entityFilter; // (regex applied to entity indexes, starts with "+" or "-" to indicate inclusion/exclusion, defaults to include-only)
		public String assocFilter; // (regex applied to new-line separated association indexes, starts with "+" or "-" to indicate inclusion/exclusion, defaults to include-only) 
	}
	
	// 2.6] Finishing steps
	
	public static class StorageSettingsPojo {
		public String rejectDocCriteria; 	//OPTIONAL: If populated, runs a user script function and if return value is non-null doesn't create the object and logs the output.  *Not* wrapped in $SCRIPT().
		public String onUpdateScript; 		//OPTIONAL: Used to preserve existing metadata when documents are updated, and also to generate new metadata based on the differences between old and new documents. *Not* wrapped in $SCRIPT().
		public String metadataFieldStorage; //OPTIONAL: A comma-separated list of top-level metadata fields to either exclude (if "metadataFields" starts with '-'), or only include (starts with '+', default) - the fields are deleted at that point in the pipeline.
	}
	
	//(SourcePojo.SourceSearchIndexFilter)
	
	////////////////////////////////////////////////////////////////////
	
	// Utility function to return the URL to use to generate the source key
	
	public static String getUrl(List<SourcePipelinePojo> pxPipe) {
		if ((null != pxPipe) && !pxPipe.isEmpty()) {
			SourcePipelinePojo firstEl = pxPipe.iterator().next();
			if (null != firstEl.database) {
				return firstEl.database.getUrl();
			}
			else if (null != firstEl.file) {
				return firstEl.file.getUrl();				
			}
			else if ((null != firstEl.feed) || (null != firstEl.web)) {
				SourceRssConfigPojo feed = (null != firstEl.feed) ? firstEl.feed : firstEl.web;
				if (null != feed.getUrl()) {
					return feed.getUrl();
				}
				else if ((null != feed.getExtraUrls()) && !feed.getExtraUrls().isEmpty()) {
					return feed.getExtraUrls().iterator().next().url;
				}
				else return null; //(fail)
			}
		}
		return null;
	}//TOTEST	
}
