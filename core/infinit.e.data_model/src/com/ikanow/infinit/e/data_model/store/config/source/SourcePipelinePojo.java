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

	//////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////
	
	// A] BASIC INPUT -> PIPELINE
	
	// A.0] Common fields:
	public String display; // (display only)
	public String criteria; // A javascript expression that is passed the document as _doc - if returns false then this pipeline element is bypassed 
	
	// A.1] Pipeline elements:
	
	// A.1.1] Starting points: 
	
	public SourceDatabaseConfigPojo database;
	public SourceFileConfigPojo file;
	public SourceRssConfigPojo feed;
	public SourceRssConfigPojo web;
	public SourceNoSqlConfigPojo nosql;
	public LogstashExtractorPojo logstash;
	public SourceFederatedQueryConfigPojo federatedQuery; // NOTE: this is currently alpha
	public DocumentPostProcessing postProcessing; // NOTE: this is currently alpha, and currently only supported in enterprise mode

	// A.1.2] Global operations
	
	public GlobalScriptPojo globals;
	public LinkedHashMap<String, ObjectId> lookupTables;
	public LinkedHashMap<String, ObjectId> aliases;
	public HarvestControlSettings harvest;
	
	// A.1.3] Secondary document extraction
	
	public SourceSearchFeedConfigPojo splitter;
	public SourceSearchFeedConfigPojo links;
	
	// A.1.4] Text and Linked-Document extraction
	
	public List<ManualTextExtractionSpecPojo> text;
	public AutomatedTextExtractionSpecPojo textEngine;
	
	// A.1.5] Document-level field (including metadata extraction)
	
	public DocumentSpecPojo docMetadata;
	public List<MetadataSpecPojo> contentMetadata;
	public List<DocumentJoinSpecPojo> joins;
	
	// A.1.6] Entities and Associations

	public AutomatedEntityExtractionSpecPojo featureEngine;
	public List<StructuredAnalysisConfigPojo.EntitySpecPojo> entities;
	public List<StructuredAnalysisConfigPojo.AssociationSpecPojo> associations;
	//TODO (INF-1922): ^^^need to add store/index to these guys
	
	// A.1.7] Finishing steps
	
	public SourcePojo.SourceSearchIndexFilter searchIndex;
	public StorageSettingsPojo storageSettings;
	
	////////////////////////////////////////////////////////
	
	// A.2] Sub-classes:
	
	// A.2.1] Extractors
	
	public static class DocumentPostProcessing {
		public enum PostProcessingQueryType { datastore, document }
		public PostProcessingQueryType queryType; //'datastore' - run a DB query against the document metadata fields, 'document' - run a standard Infinit.e query
		public String query; //(MANDATORY) Either a JSON object for a db or Infinit.e query, or paste a workspace link in to re-generate that workspace's (Infinit.e) query
		public enum ScheduleType { daily, harvest }
		public ScheduleType scheduleMode;
		public String dailySchedule;
		public List<String> groupOverrideList; // List of group ids that are added to the source community to determine the input
		public String groupOverrideRegex;//(OPTIONAL) Either a list of group ids, or a regex matching multiple group titles (START WITH '*') - these groups are added to the list
		
		// datastore query
		public String srcTags; //(OPTIONAL) A ,-separated list of tags: only documents from sources matching these tags are processed
		public String tmin; //(OPTIONAL) A date in either standard format, or in the format like "now-1d" (1 day ago), "midnight-5h" (midnight today minus 5 hours), etc: no documents before this time are processed
		public String tmax; //(OPTIONAL) A date in either standard format, or in the format like "now-1d" (1 day ago), "midnight-5h" (midnight today minus 5 hours), etc: no documents after this time are processed
		
		public Boolean rebuildAllCommunities; //(USE WITH CARE) If enabled will delete the index containing any matching documents and rebuild it (only containing matching documents, so normally used with no other query terms)
		public Boolean debugMode; //If enabled then no changes are actually made to the datastore or index (note: this is auto-enabled when testing, and auto-disabled when running operationally, though this parameter overrides those defaults)
	}
	
	public static class LogstashExtractorPojo {
		public String config; // The logstash-formatted configuration object
		public Boolean streaming; // if false (defaults to true), then source is "stashed" forever instead of being aged out
		public Boolean distributed; // if true (defaults to false), then source will be run on all available logstash nodes, not just master (normally used in conjunction with #LOGSTASH{hostname})
		public Boolean testDebugOutput; // if true (default: false) then will collect "debug" output during test (else "verbose")
		public Integer testInactivityTimeout_secs; // if specified (default 10s) then overrides the inactivity timeout during testing - the test will return if nothing happens for this period
	}
	
	// A.2.2] Global operations
	
	public static class GlobalScriptPojo {
		public List<String> imports; // An optional list of URLs that get imported before the scripts below are run
		public List<String> scripts; // A list of (java)script code blocks that are evaluated in order (normally only need to specify one)
		public String scriptlang; // Currently only "javascript" is supported
	}
	
	public static class HarvestControlSettings {
		public Integer searchCycle_secs; // How often to run the harvester (copied to SourcePojo when published)
		public Boolean duplicateExistingUrls; // If false (defaults to true) then documents matching the URL of any existing document in the community is ignored (copied to SourcePojo when published)
		
		public Integer timeToLive_days; // Sets a time to live for the documents harvested, after which they are deleted
		
		public Integer maxDocs_global; // If specified, limits the total number of documents that can be harvested for a given source - when new documents are harvested exceeding this limit, older documents are deleted to maintain the size
		public Integer maxDocs_perCycle; // If specified, limits the number of documents that can be harvested for a given source (state moves to SUCCESS_ITERATION ie the next harvest cycle, the harvester will pick up again, as above)
		public Integer throttleDocs_perCycle; // If specified, limits the number of documents that can be harvested for a given source (state moves to SUCCESS - ie this+searchCycle_secs limits document ingest rate, the harvester will wait for searchCycle_secs before starting again)
		
		public Integer distributionFactor; // (EXPERIMENTAL) If specified, attempts to distribute the source across many threads
	}
	
	// A.2.3] Secondary document extraction

	//TODO (INF-2479): add support for this in the GUI and pipeline engine (it's just a copy/paste of existing operations)
	public static class DocumentJoinSpecPojo {
		public enum DocumentJoinSpecType { custom, document, metadata, features_only };
		public DocumentJoinSpecType joinType; // if "document" takes entire document object, if "metadata" (DEFAULT) only the metadata object (ignored if a custom join)
		public String joinName; // (MANDATORY) the name of the join table to use
		public String iterateOver; // (MANDATORY) the key field
		public String accessorScript; // (OPTIONAL, by default use the object from iterateOver directly) the script ($SCRIPT or substitution as normal) that defines the key string used to get the join
		
		public String fieldName; // (MANDATORY, except if features_only) specifies field to copy the joins into, in the top level metadata field (ignored if features_only)
		public String fieldFilterList; // if starts with + or nothing then a list of fields to include (top-level only), if starts with -, a list of fields to exclude (supports nested via dot notation) 
		public String rootMetadataField; // for metadata/custom joins, the top level field to copy into the document metadata
		
		public Boolean importEntities; // (document only) if true, default false, the entities are imported and then discarded from the metadata
		public String entityTypeFilterList; // if starts with + or nothing then list of types to include, if starts with -, a list to exclude (not associations with excluded entities will also be excluded)
		public Boolean importAssociations; // (document only, ignored unless importEntities:true) if true, default false, the association are imported and then discarded from the metadata
		public String associationCategoryFilterList; // if starts with + or nothing then list of verb categories to include, if starts with -, a list to exclude		
	}
	
	// A.2.4] Text and Linked-Document extraction
	
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
	
	// A.2.5] Document-level field (including metadata extraction)
	
	public static class DocumentSpecPojo {
		public String title; // The string expression or $SCRIPT(...) specifying the document title
		public String description; // The string expression or $SCRIPT(...) specifying the document description
		public String publishedDate; // The string expression or $SCRIPT(...) specifying the document publishedDate
		public String fullText; // The string expression or $SCRIPT(...) specifying the document fullText
		public String displayUrl; // The string expression or $SCRIPT(...) specifying the document displayUrl
		public Boolean appendTagsToDocs; // if true (*NOT* default) source tags are appended to the document
		public String mediaType; // The string expression or $SCRIPT(...) specifying the document mediaType
		public String tags; // A ,-separated list of string expressions or $SCRIPT(...) - returning a ,-separated list, the result of each will be added to the tags
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
	
	// A.2.6] Entities and Associations
	
	public static class AutomatedEntityExtractionSpecPojo {
		public String engineName; // The name of the text engine to use (can be fully qualified (eg "com.ikanow.infinit.e.harvest.boilerpipe"), or just the name (eg "boilerpipe") if the engine is registered in the Infinit.e system configuration)
		public Boolean exitOnError; //OPTIONAL: if present and false, then on error tries to keep going (ie as if the pipeline element did not exist)
		public LinkedHashMap<String, String> engineConfig; // The configuration object to be passed to the engine
		public String entityFilter; // (regex applied to entity indexes, starts with "+" or "-" to indicate inclusion/exclusion, defaults to include-only)
		public String assocFilter; // (regex applied to new-line separated association indexes, starts with "+" or "-" to indicate inclusion/exclusion, defaults to include-only)
		transient public Pattern entityRegex;
		transient public Pattern assocRegex;
	}
	
	// A.2.7] Finishing steps
	
	public static class StorageSettingsPojo {
		public String rejectDocCriteria; 	//OPTIONAL: If populated, runs a user script function and if return value is non-null doesn't create the object and logs the output.  *Not* wrapped in $SCRIPT().
		public Boolean deleteExistingOnRejection; // OPTIONAL: if true, then if the doc is rejected and is updating an existing doc, then the existing doc is still deleted (default false)
		public String onUpdateScript; 		//OPTIONAL: Used to preserve existing metadata when documents are updated, and also to generate new metadata based on the differences between old and new documents. *Not* wrapped in $SCRIPT().
		public String metadataFieldStorage; //OPTIONAL: A comma-separated list of top-level metadata fields to either exclude (if "metadataFields" starts with '-'), or only include (starts with '+', default) - the fields are deleted at that point in the pipeline.
		public Boolean exitPipeline; 		//OPTIONAL: if present and true then the document exits the pipeline at this point, bypassing any further elements in the pipeline (usually used in conjunction with "criteria")
	}
	
	//(SourcePojo.SourceSearchIndexFilter)
	
	////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////
	
	// B] CUSTOM INPUT -> PIPELINE
	
	// B.0] Common Fields
	
	//("public String display;" from A)
	
	// B.1] Input
	public SourceFileConfigPojo custom_file; // Read data in from HDFS or from Infinit.e shares
	public DocumentByDatastoreQuery docs_datastoreQuery; // Reads documents, using a datastore (MongoDB) query
	public DocumentByDocumentQuery docs_documentQuery; // Reads documents, using an Infinit.e query (backed by elasticsearch)
	public RecordByIndexQuery records_indexQuery; // Reads records, using an index (elasticsearch) query
	public CustomTableByDatastoreQuery custom_datastoreQuery; // Reads custom objects, using a datastore (MongoDB) query
	public FeatureByDatastoreQuery feature_datastoreQuery; // Reads entity/association features, using a datastore (MongoDB) query
	
	// B.2] Additional artefacts
	
	public CustomAdvancedInput extraInputSettings; // Enables the job to be run across multiple data groups (prevoiusly called communities), plus some other debug settings
	public CustomArtefacts artefacts; // Specifies which "external" artefacts the job needs .. eg JARs containing the processing code (plus extra library JARs), join tables 
	public CustomScheduler scheduler; // MANDATORY element: determines the schedule in which the job is run
	
	// B.3] Processing
	
	public CustomProcessingMapper mapper; // (MANDATORY unless hadoopEngine/scriptingEngine is used) Specifies the mapper class to be executed in full classname eg com.blah.blah$MapperClass (must be in one of the JARs specified in the 'artefacts' element)
	public CustomProcessingCombiner combiner; // (OPTIONAL) Specifies the combiner class to be executed in full classname eg com.blah.blah$CombinerClass (must be in one of the JARs specified in the 'artefacts' element)
	public CustomProcessingReducer reducer; // (OPTIONAL) Specifies the reducer class to be executed in full classname eg com.blah.blah$ReducerClass (must be in one of the JARs specified in the 'artefacts' element)
		
	public HadoopEngine hadoopEngine; // An "all-in-one" specification of jobs to be run (mapper/combiner/reducer, plus configuration, plus JAR)
	public CustomScriptingEngine scriptingEngine; // An "all-in-one" specification of the built in scripting engine
	
	// B.4] Output
	
	public CustomOutputTable tableOutput; // Controls the output of custom jobs (if omitted then a minimal set of defaults are applied)
	
	// B.*] Objects:
	
	public static class DocumentByDatastoreQuery {
		public String tmin; // Maps Infinit.e-query style time strings (including "now", "now-1d" etc) onto an indexed field in the specified collection to support time-boxing the input
		public String tmax; // Maps Infinit.e-query style time strings (including "now", "now-1d" etc) onto an indexed field in the specified collection to support time-boxing the input
		public String query; // A string containing the JSON of a MongoDB query that can be used to select documents - it is recommended to only use indexed field, eg _id, url, sourceKey, entities.index - otherwise use DocumentByDocumentQuery
		public String fields; // A JSON object containing a standard MongoDB "projection", ie usual MongoDB specification of the fields to provide to the mapper, eg {"_id":0, "entities":1} (defaults to all if {}) 
		public String srcTags; // A MongoDB query, that is applied against the source tags (not the document tags) and converts to a list of sources (ie very efficient). (Note this backs out if too many sources - currently >5000 - are selected, so should be treated as a recommendation - ie mappers might still be called on non-matching sources)
		public enum ContentMode { metadata, content, both }; 
		public ContentMode contentMode; // Which aspect of documents to collection - metadata JSON, or a JSON object contain a gzip of the content. "both" NOT CURRENTLY SUPPORTED
	}
	public static class DocumentByDocumentQuery {
		public String query; // (can be the string of JSON Infinite.e query; or workspace link, eg a URL containing the param query=<URL-encoded-query-JSON); or a share)
	}
	public static class RecordByIndexQuery {
		public List<String> types; // List of elasticsearch types to filter on
		public enum StreamingMode { streaming, stashed, both };
		public StreamingMode streamingMode; // Which records table to take data from (stashed is normally demo or persistent data, streaming is normally live logs)
		public String tmin; // Maps Infinit.e-query style time strings (including "now", "now-1d" etc) onto an indexed field in the specified collection to support time-boxing the input
		public String tmax;	 // Maps Infinit.e-query style time strings (including "now", "now-1d" etc) onto an indexed field in the specified collection to support time-boxing the input
		public String query; // A string representing a JSON query in elasticsearch format (can also be in "URL format" eg q=<query string>)
		public String filter; // A string representing a JSON filter in elasticsearch format (currently not compatible with tmin/tmax)
	}
	public static class FeatureByDatastoreQuery {
		public enum FeatureName { entity, association, temporal };
		public FeatureName featureName; // Which features to query over - entities, associations, or the daily entity/sentiment aggregation 
		public String tmin; // Maps Infinit.e-query style time strings (including "now", "now-1d" etc) onto an indexed field in the specified collection to support time-boxing the input
		public String tmax; // Maps Infinit.e-query style time strings (including "now", "now-1d" etc) onto an indexed field in the specified collection to support time-boxing the input
		public String query; // A string containing the JSON of a MongoDB query that can be used to select documents - it is recommended to only use indexed field, eg _id or index
		public String fields; // A JSON object containing a standard MongoDB "projection", ie usual MongoDB specification of the fields to provide to the mapper, eg {"_id":0, "index":1} (defaults to all if {})
	}
	
	public static class CustomTableByDatastoreQuery {
		public String customTable; // The name or _id of the table to query
		public String tmin; // Maps Infinit.e-query style time strings (including "now", "now-1d" etc) onto an indexed field in the specified collection to support time-boxing the input
		public String tmax; // Maps Infinit.e-query style time strings (including "now", "now-1d" etc) onto an indexed field in the specified collection to support time-boxing the input
		public String query;// A string containing the JSON of a MongoDB query that can be used to select documents - it is recommended to only use indexed field, eg key or anything specified as an indexed field by the "tableOutput" element		
		public String fields; // A JSON object containing a standard MongoDB "projection", ie usual MongoDB specification of the fields to provide to the mapper, eg {"_id":0, "index":1} (defaults to all if {})
	}
	public static class CustomTableByIndexQuery {
		//TODO (INF-2865): support this  
	}
	
	public static class CustomAdvancedInput {
		public List<String> groupOverrideList; // (OPTIONAL - else defaults to current community) A list of community _ids that determine, together with the source's community _id, the sharing settings for the job  
		public String groupOverrideRegex; // (OPTIONAL - else defaults to current community), if starts with "*" then a regex applied across group titles to determine which groups share the job
		
		public Integer debugLimit; // (OPTIONAL) debug parameter that restricts the number of documents to either debugLimit, or the number of splits times debugLimit, depending on the input format
		
		public Integer numSplitsOverride; // (OPTIONAL) mostly a debug parameter for datastore queries, which forces the use of skip/limit (works well for small numbers of documents) instead of shard-based splitting
		public Integer docsPerSplitOverride; // (OPTIONAL) mostly a debug parameter for datastore queries, which forces the use of skip/limit (works well for small numbers of documents) instead of shard-based splitting
	}
	
	public static class CustomArtefacts {
		public String mainJar; // Either a share _id, or a URL (admin only), or a string of the format ($infinite/share/get/{_id}; legacy) that points the JAR to use for the mapper/combiner/reducer classes
		public List<String> extraJars; // A list in the same format as "mainJar", used (eg) so that multiple jobs can share a common set of resources in JAR format
		public List<String> joinTables; // A list of _ids pointing to either sources or custom jobs - these are authenticated at publish-time so can be safely used by hadoop JARs (and are automatically available via _custom when the scriptingEngine is used)
		public Boolean selfJoin; // (OPTIONAL - defaults to false) If true, then in addition to whatever documents come from the "Input" elements, then current contents of this job's output is also processed 
	}

	public static class CustomScheduler {
		public enum FrequencyMode { once_only, hourly, daily, weekly, monthly, disabled, ondemand }
		public FrequencyMode frequency; // The schedule type (ondemand NOT CURRENTLY SUPPORTED). To stop a running job, set it to "disabled".
		public List<String> dependencies; // Can be ids, jobnames, or #prefixes (LATTER NOT CURRENTLY SUPPORTED)
		public Boolean autoDependency; // (NOT CURRENTLY USED) (adds a dependency on the job immediately in front of you in the same pipeline) 
		public String runDate; // Either in a normal "js data format"; can also be in the usual "infinit.e" time format, eg "now", "now-1h" etc
		
		public String tmin_initialOverride; // copied over the tmin from the input block first time through (eg run on last week of data, then last hour)
		public String tmax_initialOverride; // copied over the tmin from the input block first time through
		public String query_initialOverride; // copied over the query from the input block first time through
		public String srcTags_initialOverride; // copied over the query from the input block first time through
	}

	public static class CustomProcessingMapper {
		public String mapperClass; // The java classpath to the jobs mapper, it should be in the form of package.file$class 
		
		public String mapperKeyClass; // (If no reducer is specified, then this is mandatory) Allows you to use different mapper output classes than the reducer (key class name, should be fully specified)
		public String mapperValueClass; // (If no reducer is specified, then this is mandatory) Allows you to use different mapper output classes than the reducer (value class name, should be fully specified)
	}

	public static class CustomProcessingCombiner {
		public String combinerClass; // The java classpath to the jobs combiner, it should be in the form of package.file$class (use the reducer if you have not written a combiner or submit null). If not present, then only the mapper (or combiner) is run, and records with duplicate keys will overwrite each other in an arbitrary order. 
	}

	public static class CustomProcessingReducer {
		public String reducerClass; // The java classpath to the jobs reducer, it should be in the form of package.file$class
		public Integer numReducers; // Specifies the number of reducers to use (OPTIONAL default: 1)
		
		public String outputKeyClass; // The classpath for the map reduce output format key usually org.apache.hadoop.io.Text or com.mongodb.hadoop.io.BSONWritable
		public String outputValueClass; // The classpath for the map reduce output format key usually com.mongodb.hadoop.io.BSONWritable
	}

	public static class CustomScriptingEngine {
		public String scriptLang; // Currently only "javascript" is supported
		public String globalScript; // Script that is available to map/combine/reduce scripts  
		public String mapScript; // (OPTIONAL) If specified, additional script code that is only applied in the mapper (a function "map(key, value)" must be specified either here or in the global)
		public String combineScript; // (OPTIONAL) If specified, additional script code that is only applied in the combiner (a function "combine(key, values)" must be specified either here or in the global if the combiner is to be run, which is optional)
		public String reduceScript; // (OPTIONAL) If specified, additional script code that is only applied in the combiner (a function "reduce(key, values)" must be specified either here or in the global if the reducer is to be run, which is optional)
		public Boolean memoryOptimized; // (OPTIONAL - defaults to true) If specified, a (very-slightly) slower but more memory efficient method is used (should be left as true except for legacy applications)
		public Integer numReducers; // Specifies the number of reducers to use (OPTIONAL default: 1)
	}
	
	public static class HadoopEngine {
		public String engineName; // A descriptive name for the engine
		public String mapperClass; // The java classpath to the jobs mapper, it should be in the form of package.file$class
		public String combinerClass; // The java classpath to the jobs combiner, it should be in the form of package.file$class (use the reducer if you have not written a combiner or submit null). If not present, then only the mapper (or combiner) is run, and records with duplicate keys will overwrite each other in an arbitrary order.
		public String reducerClass; // The java classpath to the jobs reducer, it should be in the form of package.file$class
		public Integer numReducers; // Specifies the number of reducers to use (OPTIONAL default: 1)
		public String mapperKeyClass; // Allows you to use different mapper output classes than the reducer (key class name, should be fully specified)
		public String mapperValueClass; // Allows you to use different mapper output classes than the reducer (value class name, should be fully specified)
		public String outputKeyClass; // The classpath for the map reduce output format key usually org.apache.hadoop.io.BSONWritable
		public String outputValueClass; // The classpath for the map reduce output format key usually org.apache.hadoop.io.BSONWritable
		public String configuration; // The configuration (can be JSON or any arbitrary string format depending on what the engine needs)
		public LinkedHashMap<String, String> configParams; // For JSON formats, enables seperate key/values to be inserted (currently needs to be string)
		public String mainJar; // Either a share _id, or a URL (admin only), or a string of the format ($infinite/share/get/{_id}; legacy) that points the JAR to use for the mapper/combiner/reducer classes
	}
	
	public static class CustomOutputTable {
		public String postFixName; // (CURRENTLY NOT SUPPORTED) is appended with a # to the source key to give the name of the custom output job/table 
		
		public String sortField; // Field to sort on (defaults to "_id"), supports dot notation
		public Integer sortDirection; // In conjunction with sortField, -1 or 1 for descending or ascending  
		public String dataStoreIndexes; // A JSON object or list of JSON objects defining simple or compound MongoDB indexes
		public Long perCycleObjectLimit; // (NOTE - Only one of this and globalObjectLimit can be specified - this only makes sense for append* jobs) Each time the job is run only this many objects from the last run (not globally) are added to the output (uses the sortField/sortDirection to decide what to throw away)
		public Long globalObjectLimit; // (NOTE - Only one of this and perCycleObjectLimit can be specified) Limits the total number of objects, uses the sortField/sortDirection to decide what to throw away 

		public enum AppendMode { replace, append_reduce, append_merge }
		public AppendMode appendMode; // Replace - the entire table gets rewritten each job, append_merge - new records are added to the data (where the key matches, the older data is overwritte), append_reduce - new records are added to the data (where the key matches older data an additional reduce step is run and the output replaces the old data) 
		public Double ageOut_days; // In either append mode, discards data older than this parameter (only applied each time the job is run)
		
		public Boolean indexed; // (OPTIONAL - defaults to true) If true, then the output table is mirrored to the elasticsearch index (and can be accessed via the records index) 
	}

	public static class CustomOutputRecords {
		//TODO (INF-2865): streaming .. type ... community  
	}
	
	public static class CustomOuputDocuments {
		//TODO (INF-2865): not sure about this one.. specify template id or then follow it with actual pipeline?
		public String sourceTemplateId; // (can point to share id, source id, or source key ... else just paste doc pipeline options in at the end of this..)
	}	
	
	// C] V2 INTEGRATION

	public LinkedHashMap<String, Object> data_bucket; // (free form object)
	public LinkedHashMap<String, Object> analytic_thread; // (free form object)
}
