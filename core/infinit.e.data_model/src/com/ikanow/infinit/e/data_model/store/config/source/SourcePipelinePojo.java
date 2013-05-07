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
	public List<StructuredAnalysisConfigPojo.AssociationSpecPojo> assocs;
	//TODO (INF-1922) need to add store/index to these guys?
	
	// 1.6] Finishing steps
	
	public SourcePojo.SourceSearchIndexFilter storeAndIndex;
	
	////////////////////////////////////////////////////////
	
	// 2] Sub-classes:
	
	// 2.2] Global operations
	
	public static class GlobalScriptPojo {
		public List<String> imports;
		public List<String> scripts;
		public String scriptlang;		
	}
	
	// 2.3] Text and Linked-Document extraction
	
	public static class AutomatedTextExtractionSpecPojo {
		public String criteria;
		public String engineName;
		public LinkedHashMap<String, String> engineConfig;
		
	}
	
	public static class ManualTextExtractionSpecPojo {
		public String fieldName;
		public String script;
		public String flags;
		public String replacement;
		public String scriptlang;
		//(note headers and footers are no longer supported - you can just do this manually anyway now)
	}
	
	// 2.4] Document-level field (including metadata extraction)
	
	public static class DocumentSpecPojo {
		public String title;
		public String description;
		public String publishedDate;
		public String fullText;
		public String displayUrl;
		public Boolean appendTagsToDocs; // if true (*NOT* default) source tags are appended to the document 
		public StructuredAnalysisConfigPojo.GeoSpecPojo docGeo;
	}
	
	public static class MetadataSpecPojo {
		public String fieldName;
		public String scriptlang;
		public String script;
		public String flags;
		public String replace;
		public Boolean store;
		public Boolean index;
	}
	
	// 2.5] Entities and Associations
	
	public static class AutomatedEntityExtractionSpecPojo {
		public String criteria;
		public String engineName;
		public LinkedHashMap<String, String> engineConfig;
		public String entityFilter;
		public String assocFilter;
	}
	
	// 2.6] Finishing steps
	
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
