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
import java.util.regex.Pattern;

import org.bson.types.ObjectId;

import com.google.gson.reflect.TypeToken;
import com.ikanow.infinit.e.data_model.store.BaseDbPojo;

public class SourcePipelinePojo extends BaseDbPojo {
	// Standard static function for readability
	@SuppressWarnings("unchecked")
	static public TypeToken<List<SourcePipelinePojo>> listType() { return new TypeToken<List<SourcePipelinePojo>>(){}; } 

	// 0] Common fields:
	public String display; // (display only)
	public String criteria; // A javascript expression that is passed the document as _doc - if returns false then this pipeline element is bypassed 
	
	// 1] Pipeline elements:
	
	// 1.1] Starting points: 
	
	public SourceDatabaseConfigPojo database;
	public SourceFileConfigPojo file;
	public SourceRssConfigPojo feed;
	public SourceRssConfigPojo web;
	public SourceNoSqlConfigPojo nosql = null;
	public LogstashExtractorPojo logstash;

	// 1.2] Global operations
	
	public GlobalScriptPojo globals;
	public LinkedHashMap<String, ObjectId> lookupTables;
	public LinkedHashMap<String, ObjectId> aliases;
	public HarvestControlSettings harvest;
	
	// 1.3] Secondary document extraction
	
	public SourceSearchFeedConfigPojo splitter;
	public SourceSearchFeedConfigPojo links;
	public List<DocumentJoinSpecPojo> joins;
	
	// 1.4] Text and Linked-Document extraction
	
	public List<ManualTextExtractionSpecPojo> text;
	public AutomatedTextExtractionSpecPojo textEngine;
	
	// 1.5] Document-level field (including metadata extraction)
	
	public DocumentSpecPojo docMetadata;
	public List<MetadataSpecPojo> contentMetadata;
	
	// 1.6] Entities and Associations

	public AutomatedEntityExtractionSpecPojo featureEngine;
	public List<StructuredAnalysisConfigPojo.EntitySpecPojo> entities;
	public List<StructuredAnalysisConfigPojo.AssociationSpecPojo> associations;
	//TODO (INF-1922): ^^^need to add store/index to these guys
	
	// 1.7] Finishing steps
	
	public SourcePojo.SourceSearchIndexFilter searchIndex;
	public StorageSettingsPojo storageSettings;
	
	////////////////////////////////////////////////////////
	
	// 2] Sub-classes:
	
	// 2.1] Extractors
	
	public static class LogstashExtractorPojo {
		public String config; // The logstash-formatted configuration object
		public Boolean streaming; // if false (defaults to true), then source is "stashed" forever instead of being aged out
		public Boolean testDebugOutput; // if true (default: false) then will collect "debug" output during test (else "verbose")
		public Integer testInactivityTimeout_secs; // if specified (default 10s) then overrides the inactivity timeout during testing - the test will return if nothing happens for this period
	}
	
	// 2.2] Global operations
	
	public static class GlobalScriptPojo {
		public List<String> imports; // An optional list of URLs that get imported before the scripts below are run
		public List<String> scripts; // A list of (java)script code blocks that are evaluated in order (normally only need to specify one)
		public String scriptlang; // Currently only "javascript" is supported
	}
	
	public static class HarvestControlSettings {
		public Integer searchCycle_secs; // How often to run the harvester (copied to SourcePojo when published)
		public Boolean duplicateExistingUrls; // If false (defaults to true) then documents matching the URL of any existing document in the community is ignored (copied to SourcePojo when published)
		
		public Integer maxDocs_global; // If specified, limits the total number of documents that can be harvested for a given source - when new documents are harvested exceeding this limit, older documents are deleted to maintain the size
		public Integer maxDocs_perCycle; // If specified, limits the number of documents that can be harvested for a given source (state moves to SUCCESS_ITERATION ie the next harvest cycle, the harvester will pick up again, as above)
		public Integer throttleDocs_perCycle; // If specified, limits the number of documents that can be harvested for a given source (state moves to SUCCESS - ie this+searchCycle_secs limits document ingest rate, the harvester will wait for searchCycle_secs before starting again)
		
		public Integer distributionFactor; // (EXPERIMENTAL) If specified, attempts to distribute the source across many threads
	}
	
	// 2.3] Secondary document extraction

	//TODO (INF-2479): add support for this in the GUI and pipeline engine (it's just a copy/paste of existing operations)
	public static class DocumentJoinSpecPojo {
		public String iterateOver; // allows multiple joins to be created in the case where the key is an array)
		public String fieldName; // specifies the (and the field to copy the joins into, in the top level metadata field)
		public String accessorScript; // the script ($SCRIPT or substitution as normal) that defines the key string used to get the join 
		public enum DocumentJoinSpecType { document, metadata };
		public DocumentJoinSpecType joinType; // if "document" takes entire document object, if "metadata" (DEFAULT) only the metadata object (ignored if a custom join)
		public String fieldFilterList; // if starts with + or nothing then a list of fields to include (top-level only), if starts with -, a list of fields to exclude (supports nested via dot notation) 
		public String rootMetadataField; // for metadata/custom joins, the top level field to copy into the document metadata 
		public Boolean importEntities; // (document only) if true, default false, the entities are imported and then discarded from the metadata
		public String entityTypeFilterList; // if starts with + or nothing then list of types to include, if starts with -, a list to exclude (not associations with excluded entities will also be excluded)
		public Boolean importAssociations; // (document only, ignored unless importEntities:true) if true, default false, the association are imported and then discarded from the metadata
		public String associationCategoryFilterList; // if starts with + or nothing then list of verb categories to include, if starts with -, a list to exclude		
	}
	
	// 2.4] Text and Linked-Document extraction
	
	public static class AutomatedTextExtractionSpecPojo {
		public String engineName; // The name of the text engine to use (can be fully qualified (eg "com.ikanow.infinit.e.harvest.boilerpipe"), or just the name (eg "boilerpipe") if the engine is registered in the Infinit.e system configuration)
		public Boolean exitOnError; //OPTIONAL: if present and false, then on error tries to keep going (ie as if the pipeline element did not exist)
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
	
	// 2.5] Document-level field (including metadata extraction)
	
	public static class DocumentSpecPojo {
		public String title; // The string expression or $SCRIPT(...) specifying the document title
		public String description; // The string expression or $SCRIPT(...) specifying the document description
		public String publishedDate; // The string expression or $SCRIPT(...) specifying the document publishedDate
		public String fullText; // The string expression or $SCRIPT(...) specifying the document fullText
		public String displayUrl; // The string expression or $SCRIPT(...) specifying the document displayUrl
		public Boolean appendTagsToDocs; // if true (*NOT* default) source tags are appended to the document 
		public StructuredAnalysisConfigPojo.GeoSpecPojo geotag; // Specify a document level geo-tag
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
	
	// 2.6] Entities and Associations
	
	public static class AutomatedEntityExtractionSpecPojo {
		public String engineName; // The name of the text engine to use (can be fully qualified (eg "com.ikanow.infinit.e.harvest.boilerpipe"), or just the name (eg "boilerpipe") if the engine is registered in the Infinit.e system configuration)
		public Boolean exitOnError; //OPTIONAL: if present and false, then on error tries to keep going (ie as if the pipeline element did not exist)
		public LinkedHashMap<String, String> engineConfig; // The configuration object to be passed to the engine
		public String entityFilter; // (regex applied to entity indexes, starts with "+" or "-" to indicate inclusion/exclusion, defaults to include-only)
		public String assocFilter; // (regex applied to new-line separated association indexes, starts with "+" or "-" to indicate inclusion/exclusion, defaults to include-only)
		transient public Pattern entityRegex;
		transient public Pattern assocRegex;
	}
	
	// 2.7] Finishing steps
	
	public static class StorageSettingsPojo {
		public String rejectDocCriteria; 	//OPTIONAL: If populated, runs a user script function and if return value is non-null doesn't create the object and logs the output.  *Not* wrapped in $SCRIPT().
		public String onUpdateScript; 		//OPTIONAL: Used to preserve existing metadata when documents are updated, and also to generate new metadata based on the differences between old and new documents. *Not* wrapped in $SCRIPT().
		public String metadataFieldStorage; //OPTIONAL: A comma-separated list of top-level metadata fields to either exclude (if "metadataFields" starts with '-'), or only include (starts with '+', default) - the fields are deleted at that point in the pipeline.
		public Boolean exitPipeline; 		//OPTIONAL: if present and true then the document exits the pipeline at this point, bypassing any further elements in the pipeline (usually used in conjunction with "criteria")
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
