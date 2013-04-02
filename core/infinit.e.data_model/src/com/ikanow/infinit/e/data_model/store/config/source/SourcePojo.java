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

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Base64;
import org.bson.types.ObjectId;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import com.ikanow.infinit.e.data_model.store.BaseDbPojo;
import com.ikanow.infinit.e.data_model.store.social.authentication.AuthenticationPojo;
import com.mongodb.BasicDBObject;

/**
 * Class used to establish the source information for a feed
 * this defines the data necessary to create a feed in the system
 * 
 * @author cmorgan
 *
 */

public class SourcePojo extends BaseDbPojo {
	// Standard static function for readability
	@SuppressWarnings("unchecked")
	static public TypeToken<List<SourcePojo>> listType() { return new TypeToken<List<SourcePojo>>(){}; }

	/** 
	  * Private Class Variables
	  */
	private ObjectId _id = null;
	final public static String _id_ = "_id";
	private Date created = null;
	final public static String created_ = "created";
	private Date modified = null;
	final public static String modified_ = "modified";
	private String url = null;
	final public static String url_ = "url";
	private String title = null;
	final public static String title_ = "title";
	private Boolean isPublic = null;
	final public static String isPublic_ = "isPublic";
	private ObjectId ownerId = null;
	final public static String ownerId_ = "ownerId";
	private String author = null;
	final public static String author_ = "author";
	
	private AuthenticationPojo authentication = null;
	final public static String authentication_ = "authentication";
	
	private String mediaType = null;
	final public static String mediaType_ = "mediaType";
	private String key = null;
	final public static String key_ = "key";
	private String description = null;
	final public static String description_ = "description";
	private Set<String> tags = null;
	final public static String tags_ = "tags";
	
	private Set<ObjectId> communityIds = null;
	final public static String communityIds_ = "communityIds";
	
	private SourceHarvestStatusPojo harvest = null;
	final public static String harvest_ = "harvest";
	private SourceDatabaseConfigPojo database = null;
	final public static String database_ = "database";
	private SourceFileConfigPojo file = null;
	final public static String file_ = "file";
	private SourceRssConfigPojo rss = null;
	final public static String rss_ = "rss";
	
	private boolean isApproved = false;
	final public static String isApproved_ = "isApproved";
	private boolean harvestBadSource = false;
	final public static String harvestBadSource_ = "harvestBadSource";
	
	private String extractType = null;
	final public static String extractType_ = "extractType";
	private String useExtractor = null;
	final public static String useExtractor_ = "useExtractor";
	private String useTextExtractor = null;
	final public static String useTextExtractor_ = "useTextExtractor";
	
	private StructuredAnalysisConfigPojo structuredAnalysis = null;
	final public static String structuredAnalysis_ = "structuredAnalysis";
	private UnstructuredAnalysisConfigPojo unstructuredAnalysis = null;
	final public static String unstructuredAnalysis_ = "unstructuredAnalysis";
	
	private String shah256Hash = null;	
	final public static String shah256Hash_ = "shah256Hash";

	private Integer searchCycle_secs = null; // Determines the time between searches, defaults as quickly as the harvest can cycle
	final public static String searchCycle_secs_ = "searchCycle_secs";
	private Integer maxDocs = null; // Limits the number of docs that can be stored for this source at any one time
	final public static String maxDocs_ = "maxDocs";
	private Boolean duplicateExistingUrls; // If false (defaults: true) will ignore docs harvested by other sources in the community
	final public static String duplicateExistingUrls_ = "duplicateExistingUrls";

	// LEGACY CODE, IGNORED IN PROCESSING-PIPELINE MODE
	private Boolean appendTagsToDocs = null; // if true (default) source tags are appended to the document
	final public static String appendTagsToDocs_ = "appendTagsToDocs";
	
	public static class SourceSearchIndexFilter {
		public String entityFilter = null; // (regex applied to entity indexes, starts with "+" or "-" to indicate inclusion/exclusion, defaults to include-only)
		public String assocFilter = null; // (regex applied to new-line separated association indexes, starts with "+" or "-" to indicate inclusion/exclusion, defaults to include-only)
		public String entityGeoFilter = null; // (regex applied to entity indexes if the entity has geo, starts with "+" or "-" to indicate inclusion/exclusion, defaults to include-only)
		public String assocGeoFilter = null; // (regex applied to new-line separated association indexes if the association has geo, starts with "+" or "-" to indicate inclusion/exclusion, defaults to include-only)
		public String fieldList = null; // (comma-separated list of doc fields, starts with "+" or "-" to indicate inclusion/exclusion, defaults to include-only)
		public String metadataFieldList = null; // (comma-separated list of doc fields, starts with "+" or "-" to indicate inclusion/exclusion, defaults to include-only)
		
		// temp:
		public transient Pattern entityFilterRegex;
		public transient Pattern assocFilterRegex;
		public transient Pattern entityGeoFilterRegex;
		public transient Pattern assocGeoFilterRegex;
	}
	private SourceSearchIndexFilter searchIndexFilter = null; // Optional, allows the source builder to configure which fields are searchable
	final public static String searchIndexFilter_ = "searchIndexFilter";
	
	private LinkedHashMap<String, String> extractorOptions = null; // Optional, overrides the per-extractor configuration options, where permissible
	final public static String extractorOptions_ = "extractorOptions";
	
	// TODO (INF-1922) source pipeline
	private List<SourcePipelinePojo> processingPipeline;
	final public static String processingPipeline_ = "processingPipeline";
	
	// Gets and sets
	
	public AuthenticationPojo getAuthentication() {
		return authentication;
	}
	public void setAuthentication(AuthenticationPojo authentication) {
		this.authentication = authentication;
	}
	public SourceFileConfigPojo getFileConfig() {
		return file;
	}
	public void setFileConfig(SourceFileConfigPojo file) {
		this.file = file;
	}
	public SourceRssConfigPojo getRssConfig() {
		return rss;
	}
	public void setRssConfig(SourceRssConfigPojo rss) {
		this.rss = rss;
	}
	public SourceDatabaseConfigPojo getDatabaseConfig() {
		return database;
	}
	public void setDatabaseConfig(SourceDatabaseConfigPojo database) {
		this.database = database;
	}
	public ObjectId getId() {
		return _id;
	}
	public void setId(ObjectId id) {
		this._id = id;
	}
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	public Date getCreated() {
		return created;
	}
	public void setCreated(Date created) {
		this.created = created;
	}
	public Date getModified() {
		return modified;
	}
	public void setModified(Date modified) {
		this.modified = modified;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getMediaType() {
		return mediaType;
	}
	public void setMediaType(String mediaType) {
		this.mediaType = mediaType;
	}
	public String getExtractType() {
		return extractType;
	}
	public void setExtractType(String extractType) {
		this.extractType = extractType;
	}
	public Boolean getIsPublic() {
		return isPublic;
	}
	public boolean isPublic() {
		return (isPublic == null)?false:isPublic; // (ie defaults to false)
	}
	public void setIsPublic(Boolean isPublic) {
		this.isPublic = isPublic;		
	}
	public void setPublic(boolean isPublic) {
		this.isPublic = isPublic;
	}
	public String getAuthor() {
		return author;
	}
	public void setAuthor(String author) {
		this.author = author;
	}
	/** 
	  * Get the tags
	  */
	public Set<String> getTags() {
		return tags;
	}
	/** 
	  * Set the tags
	  */
	public void setTags(Set<String> tags) {
		this.tags = tags;
	}
	
	/**
	 * @param ownerID the ownerID to set
	 */
	public void setOwnerId(ObjectId ownerID) {
		this.ownerId = ownerID;
	}
	/**
	 * @return the ownerID
	 */
	public ObjectId getOwnerId() {
		return ownerId;
	}
	public SourcePojo() {
		
	}
	public void setHarvestStatus(SourceHarvestStatusPojo harvest) {
		this.harvest = harvest;
	}
	public SourceHarvestStatusPojo getHarvestStatus() {
		return harvest;
	}
	public void setApproved(boolean isApproved) {
		this.isApproved = isApproved;
	}
	public boolean isApproved() {
		return isApproved;
	}
	public void addToCommunityIds(ObjectId communityID) {
		if (null == this.communityIds) {
			this.communityIds = new HashSet<ObjectId>();
		}
		this.communityIds.add(communityID);
	}
	public void removeFromCommunityIds(ObjectId communityID) {
		if (null != this.communityIds) {
			this.communityIds.remove(communityID);
		}
	}
	public Set<ObjectId> getCommunityIds() {
		return communityIds;
	}
	public void setCommunityIds(Set<ObjectId> ids) {
		communityIds = ids;
	}
	public void setHarvestBadSource(boolean harvestBadSource) {
		this.harvestBadSource = harvestBadSource;
	}
	public boolean isHarvestBadSource() {
		return harvestBadSource;
	}

	/**
	 * @param useExtractor the useExtractor to set
	 */
	public void setUseExtractor(String useExtractor) {
		this.useExtractor = useExtractor;
	}

	/**
	 * @return the useExtractor
	 */
	public String useExtractor() {
		return useExtractor;
	}

	/**
	 * @param useTextExtractor the useTextExtractor to set
	 */
	public void setUseTextExtractor(String useTextExtractor) {
		this.useTextExtractor = useTextExtractor;
	}

	/**
	 * @return the useTextExtractor
	 */
	public String useTextExtractor() {
		return useTextExtractor;
	}

	/**
	 * @param structedAnalysis the structedAnalysis to set
	 */
	public void setStructuredAnalysisConfig(StructuredAnalysisConfigPojo structuredAnalysis) {
		this.structuredAnalysis = structuredAnalysis;
	}

	/**
	 * @return the structedAnalysis
	 */
	public StructuredAnalysisConfigPojo getStructuredAnalysisConfig() {
		return structuredAnalysis;
	}
	
	/**
	 * @param structuredAnalysis the structuredAnalysis to set
	 */
	public void setUnstructuredAnalysisConfig(UnstructuredAnalysisConfigPojo unstructuredAnalysis) {
		this.unstructuredAnalysis = unstructuredAnalysis;
	}

	/**
	 * @return the unstructuredAnalysis
	 */
	public UnstructuredAnalysisConfigPojo getUnstructuredAnalysisConfig() {
		return unstructuredAnalysis;
	}
	/**
	 * setShah256Hash - calls generateShah256Hash
	 */
	public void generateShah256Hash()
	{
		try 
		{
			generateShah256Hash_internal();
		} 
		catch (Exception e) 
		{
			
		}
	}

	/**
	 * getShah256Hash - calls generateShah256Hash if shah256Hash is null
	 * @return
	 */
	public String getShah256Hash() 
	{
		if (null != shah256Hash )
		{
			return shah256Hash;
		}
		else
		{
			try 
			{
				generateShah256Hash_internal();
				return shah256Hash;
			} 
			catch (Exception e) 
			{
				return null;
			}
		}
	}
	// Utility:
	
	/**
	 * generateSourceKey
	 * Strips out http://, smb:// /, :, etc. from the URL field to generate
	 * Example: http://www.ikanow.com/rss -> www.ikanow.com.rss
	 */
	public String generateSourceKey()
	{
		String s = "";
		if (null != this.url) {
			s = this.url.toLowerCase();			
		}
		else if ((null != this.rss) && (null != this.rss.getExtraUrls()) && (this.rss.getExtraUrls().size() > 0))
		{
			s = this.rss.getExtraUrls().get(0).url; 
		}
		int nIndex = s.indexOf('?');
		final int nMaxLen = 64; // (+24 for the object id, + random other stuff, keeps it in the <100 range)
		if (nIndex >= 0) {
			if (nIndex > nMaxLen) {
				nIndex = nMaxLen; // (ie max length)
			}
			StringBuffer sb = new StringBuffer(s.substring(0, nIndex));
			sb.append(".").append(s.length() - nIndex).append('.').append(Math.abs(s.hashCode()) % 100);
			s = sb.toString();
		}
		else if (s.length() > nMaxLen) {
			s = s.substring(0, nMaxLen);
		}
		//TESTED (urls with and without ?)
		
		s = s.replaceAll("http://|https://|smb://|ftp://|ftps://|file://|[/:+?&(),]", ".");
		if (s.startsWith(".")) s = s.substring(1);
		return s;
	}
	/**
	 * generateShah256Hash
	 * Combines the required and optional fields of a SourcePojo into a string that is
	 * then hashed using SHAH-256 and saved to the SourePojo.shah256Hash field;
	 * this value is used to determine source uniqueness
	 * @throws NoSuchAlgorithmException
	 * @throws UnsupportedEncodingException
	 */
	private void generateShah256Hash_internal() throws NoSuchAlgorithmException, UnsupportedEncodingException 
	{	
		// Create StringBuffer with fields to use to establish source *processing* uniqueness
		StringBuffer sb = new StringBuffer();

		// (Note what I mean by "source processing uniqueness" is that, *for a specific doc URL* 2 sources would process it identically)
		
		// So fields like key,URL,media type,tags,etc aren't included in the hash
		
		// Required Fields
		sb.append(this.extractType);
				
		// Optional fields
		if (this.extractType != null) sb.append(this.extractType);
		if (this.useExtractor != null) sb.append(this.useExtractor);
		if (this.useTextExtractor != null) sb.append(this.useTextExtractor);
		
		// Generate a hash of all the objects using the ORM layer
		SourcePojo newSrc = new SourcePojo();
		newSrc.setId(null); // (in case this is auto set by the c'tor)
		newSrc.setAuthentication(this.authentication);
		newSrc.setDatabaseConfig(this.database);
		newSrc.setFileConfig(this.file);
		newSrc.setRssConfig(this.rss);
		newSrc.setStructuredAnalysisConfig(this.structuredAnalysis);
		newSrc.setUnstructuredAnalysisConfig(this.unstructuredAnalysis);
		sb.append(((BasicDBObject)newSrc.toDb()).toString());
		
		// Create MessageDigest and set shah256Hash value
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		md.update(sb.toString().getBytes("UTF-8"));		
		shah256Hash = Base64.encodeBase64String(md.digest());	
	}
	public Integer getSearchCycle_secs() {
		return searchCycle_secs;
	}
	public void setSearchCycle_secs(Integer searchCycle_secs) {
		this.searchCycle_secs = searchCycle_secs;
	}
	public void setMaxDocs(Integer maxDocs) {
		this.maxDocs = maxDocs;
	}
	public Integer getMaxDocs() {
		return maxDocs;
	}
	public void setReachedMaxDocs() {
		this.reachedMaxDocs = true;
	}
	public boolean reachedMaxDocs() {
		return reachedMaxDocs;
	}
	public void setDuplicateExistingUrls(Boolean duplicateExistingUrls) {
		this.duplicateExistingUrls = duplicateExistingUrls;
	}
	public boolean getDuplicateExistingUrls() { // (defaults to true)
		return duplicateExistingUrls == null ? true : duplicateExistingUrls;
	}
	public SourceSearchIndexFilter getSearchIndexFilter() {
		initSearchIndexFilter(searchIndexFilter);
		return searchIndexFilter;
	}
	public void setSearchIndexFilter(SourceSearchIndexFilter searchIndexFilter) {
		this.searchIndexFilter = searchIndexFilter;
	}
	///////////////////////////////////////////////////////////////////////////////////
	
	// Transient state (implementation details)
	
	transient private boolean reachedMaxDocs = false;
	// (if set to true, means that the next search cycle won't be applied - otherwise if you only search once per day
	//  and only process 5K docs/search, it can take a while to build up large repositories)

	// Build some regexes:
	
	public static void initSearchIndexFilter(SourceSearchIndexFilter searchIndexFilter) {
		if (null != searchIndexFilter) { // Initialize regex
			if ((null != searchIndexFilter.assocFilter) && (null == searchIndexFilter.assocFilterRegex)) {
				if (searchIndexFilter.assocFilter.startsWith("+") || searchIndexFilter.assocFilter.startsWith("-")) {
					searchIndexFilter.assocFilterRegex = Pattern.compile(searchIndexFilter.assocFilter.substring(1), Pattern.CASE_INSENSITIVE|Pattern.DOTALL|Pattern.MULTILINE);
				}
				else {
					searchIndexFilter.assocFilterRegex = Pattern.compile(searchIndexFilter.assocFilter, Pattern.CASE_INSENSITIVE|Pattern.DOTALL|Pattern.MULTILINE);					
				}
			}
			if ((null != searchIndexFilter.assocGeoFilter) && (null == searchIndexFilter.assocGeoFilterRegex)) {
				if (searchIndexFilter.assocGeoFilter.startsWith("+") || searchIndexFilter.assocGeoFilter.startsWith("-")) {
					searchIndexFilter.assocGeoFilterRegex = Pattern.compile(searchIndexFilter.assocGeoFilter.substring(1), Pattern.CASE_INSENSITIVE|Pattern.DOTALL|Pattern.MULTILINE);
				}
				else {
					searchIndexFilter.assocGeoFilterRegex = Pattern.compile(searchIndexFilter.assocGeoFilter, Pattern.CASE_INSENSITIVE|Pattern.DOTALL|Pattern.MULTILINE);					
				}
			}
			if ((null != searchIndexFilter.entityFilter) && (null == searchIndexFilter.entityFilterRegex)) {
				if (searchIndexFilter.entityFilter.startsWith("+") || searchIndexFilter.entityFilter.startsWith("-")) {
					searchIndexFilter.entityFilterRegex = Pattern.compile(searchIndexFilter.entityFilter.substring(1), Pattern.CASE_INSENSITIVE);
				}
				else {
					searchIndexFilter.entityFilterRegex = Pattern.compile(searchIndexFilter.entityFilter, Pattern.CASE_INSENSITIVE);					
				}
			}
			if ((null != searchIndexFilter.entityGeoFilter) && (null == searchIndexFilter.entityGeoFilterRegex)) {
				if (searchIndexFilter.entityGeoFilter.startsWith("+") || searchIndexFilter.entityGeoFilter.startsWith("-")) {
					searchIndexFilter.entityGeoFilterRegex = Pattern.compile(searchIndexFilter.entityGeoFilter.substring(1), Pattern.CASE_INSENSITIVE);
				}
				else {
					searchIndexFilter.entityGeoFilterRegex = Pattern.compile(searchIndexFilter.entityGeoFilter, Pattern.CASE_INSENSITIVE);					
				}
			}
		} // (end if search filter specified)
	}//(end initialize search filter)
	public void setExtractorOptions(LinkedHashMap<String, String> extractorOptions) {
		this.extractorOptions = extractorOptions;
	}
	public LinkedHashMap<String, String> getExtractorOptions() {
		return extractorOptions;
	}
	//TESTED

	///////////////////////////////////////////////////////////////////
	
	// Serialization/deserialization utils:
	// (Ugh needed because extractorOptions keys can contain "."s)
	
	public GsonBuilder extendBuilder(GsonBuilder gp) {
		return gp.registerTypeAdapter(SourcePojo.class, new SourcePojoDeserializer()).
				registerTypeAdapter(SourcePojo.class, new SourcePojoSerializer());
	}
	
	public void setProcessingPipeline(List<SourcePipelinePojo> processingPipeline) {
		this.processingPipeline = processingPipeline;
	}
	public List<SourcePipelinePojo> getProcessingPipeline() {
		return processingPipeline;
	}

	public void setAppendTagsToDocs(Boolean appendTagsToDocs) {
		this.appendTagsToDocs = appendTagsToDocs;
	}
	public Boolean getAppendTagsToDocs() {
		return appendTagsToDocs;
	}

	protected static class SourcePojoDeserializer implements JsonDeserializer<SourcePojo> 
	{
		@Override
		public SourcePojo deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
		{
			SourcePojo src = BaseDbPojo.getDefaultBuilder().create().fromJson(json, SourcePojo.class);  
			if (null != src.extractorOptions) {
				LinkedHashMap<String, String> transformed = new LinkedHashMap<String, String>();
				for (Map.Entry<String, String> entry: src.extractorOptions.entrySet()) {
					transformed.put(entry.getKey().replace("%2e", "."), entry.getValue());
				}
				src.extractorOptions = transformed;
			}
			return src;
		}//TESTED (with and without extractor options)
	}
	protected static class SourcePojoSerializer implements JsonSerializer<SourcePojo> 
	{
		@Override
		public JsonElement serialize(SourcePojo src, Type typeOfT, JsonSerializationContext context) throws JsonParseException
		{
			if (null != src.extractorOptions) {
				LinkedHashMap<String, String> transformed = new LinkedHashMap<String, String>();
				for (Map.Entry<String, String> entry: src.extractorOptions.entrySet()) {
					transformed.put(entry.getKey().replace(".", "%2e"), entry.getValue());
				}
				src.extractorOptions = transformed;
			}
			// GSON transformation:
			JsonElement je = SourcePojo.getDefaultBuilder().create().toJsonTree(src, typeOfT);
			
			return je;
		}//TESTED (with and without extractor options)
	}	
}
