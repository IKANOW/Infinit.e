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
package com.ikanow.infinit.e.harvest.enrichment.legacy.alchemyapi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.ikanow.infinit.e.data_model.InfiniteEnums;
import com.ikanow.infinit.e.data_model.InfiniteEnums.ExtractorDailyLimitExceededException;
import com.ikanow.infinit.e.data_model.InfiniteEnums.ExtractorDocumentLevelException;
import com.ikanow.infinit.e.data_model.InfiniteEnums.ExtractorSourceLevelException;
import com.ikanow.infinit.e.data_model.interfaces.harvest.EntityExtractorEnum;
import com.ikanow.infinit.e.data_model.interfaces.harvest.IEntityExtractor;
import com.ikanow.infinit.e.data_model.interfaces.harvest.ITextExtractor;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.infinit.e.data_model.store.document.EntityPojo;
import com.ikanow.infinit.e.harvest.utils.PropertiesManager;

public class ExtractorAlchemyAPI_Metadata implements IEntityExtractor, ITextExtractor 
{
	@Override
	public String getName() { return "alchemyapi-metadata"; }
	
	private static final Logger logger = Logger.getLogger(ExtractorAlchemyAPI_Metadata.class);
	private AlchemyAPI_JSON _alch = AlchemyAPI_JSON.GetInstanceFromProperties();
	private Map<EntityExtractorEnum, String> _capabilities = new HashMap<EntityExtractorEnum, String>();		
	private boolean _bConceptExtraction = false;
	
	private static final int MAX_LENGTH = 145000;	
	
	// Batch size
	private int _nBatchSize = 1;
	private int _nNumKeywords = 50; // (for batch size > 1, keywords/batch_size)
	private static class BatchInfo {
		DocumentPojo doc;
		String fullText;
	}
	BatchInfo[] _batchedDocuments = null;
	StringBuffer _batchText = null;
	int _nCurrBatchedDocs = 0;
	
	//_______________________________________________________________________
	//_____________________________INITIALIZATION________________
	//_______________________________________________________________________

	/**
	 * Constructor, adds capabilities of Alchemy to hashmap
	 */
	public ExtractorAlchemyAPI_Metadata()
	{
		//insert capabilities of this extractor
		_capabilities.put(EntityExtractorEnum.Name, "AlchemyAPI-metadata");
		_capabilities.put(EntityExtractorEnum.Quality, "1");
		_capabilities.put(EntityExtractorEnum.URLTextExtraction, "true");
		_capabilities.put(EntityExtractorEnum.GeotagExtraction, "true");
		_capabilities.put(EntityExtractorEnum.SentimentExtraction, "false");		
		_capabilities.put(EntityExtractorEnum.MaxInputBytes, Integer.toString(MAX_LENGTH));
	}
	
	// Configuration: override global configuration on a per source basis
	
	private boolean _bConfigured = false;
	
	private void configure(SourcePojo source)
	{
		if (_bConfigured) {
			return;
		}
		_bConfigured = true;
		
		// SOURCE OVERRIDE
		
		Boolean bSentimentEnabled = null;
		Boolean bConceptsEnabled = null;
		Boolean bStrict = null;
		Integer batchSize = null;
		Integer numKeywords = null;				
		String apiKey = null;
		
		if ((null != source) && (null != source.getExtractorOptions())) {
			try {
				apiKey = source.getExtractorOptions().get("app.alchemyapi-metadata.apiKeyOverride");
			}
			catch (Exception e){}
			try {
				String s = source.getExtractorOptions().get("app.alchemyapi-metadata.sentiment");
				if (null != s) bSentimentEnabled = Boolean.parseBoolean(s);				
			}
			catch (Exception e){}
			try {
				String s = source.getExtractorOptions().get("app.alchemyapi-metadata.concepts");
				if (null != s) bConceptsEnabled = Boolean.parseBoolean(s);						
			}
			catch (Exception e){}
			try {
				String s = source.getExtractorOptions().get("app.alchemyapi-metadata.strict");
				if (null != s) bStrict = Boolean.parseBoolean(s);						
			}
			catch (Exception e){}
			try {
				numKeywords = Integer.parseInt(source.getExtractorOptions().get("app.alchemyapi-metadata.numKeywords"));						
			}
			catch (Exception e){}
			try {
				batchSize = Integer.parseInt(source.getExtractorOptions().get("app.alchemyapi-metadata.batchSize"));						
			}
			catch (Exception e){}
		}
		
		// DEFAULT CONFIGURATION
		
		PropertiesManager properties = new PropertiesManager();
		
		// 2] SENTIMENT
		
		try {
			if (null == bSentimentEnabled) { // (ie not per source)
				bSentimentEnabled = properties.getExtractionCapabilityEnabled(getName(), "sentiment");			
			}
		}
		catch (Exception e) {}
		
		// 3] CONCEPTS
		
		try {
			if (null == bConceptsEnabled) { // (ie not per source)
				bConceptsEnabled = properties.getExtractionCapabilityEnabled(getName(), "concepts");			
			}
		}
		catch (Exception e) {}

		// 4] KEYWORD QUALITY
		
		try {
			if (null == bStrict) { // (ie not per source)
				bStrict = properties.getExtractionCapabilityEnabled(getName(), "strict");			
			}
		}
		catch (Exception e) {}
		
		
		// ACTUALLY DO CONFIG
		
		if (null != batchSize) {
			_nBatchSize = batchSize;
			if (_nBatchSize > 1) {
				_batchedDocuments = new BatchInfo[_nBatchSize];
				_batchText = new StringBuffer();
			}
		}
		
		if (null != numKeywords) {
			_alch.setNumKeywords(numKeywords);
			_nNumKeywords = numKeywords; // (only used for batch size > 1)
		}
		
		if (null != bStrict) {
			_alch.setStrict(bStrict);
		}
		
		if (null != bSentimentEnabled) { // (ie defaults to true)
			_alch.setSentimentEnabled(bSentimentEnabled);
		}
		
		if (null != bConceptsEnabled) { // (ie defaults to true)
			_bConceptExtraction = bConceptsEnabled;
		}			
		if (null != apiKey) {
			_alch.SetAPIKey(apiKey);
		}
	}
	
	//_______________________________________________________________________
	//_____________________________ENTITY EXTRACTOR FUNCTIONS________________
	//_______________________________________________________________________
	
	/**
	 * Takes a doc with some of the information stored in it
	 * such as title, desc, etc, and needs to parse the full
	 * text and add entities, events, and other metadata.
	 * 
	 * @param partialDoc The feedpojo before extraction with fulltext field to extract on
	 * @return The feedpojo after extraction with entities, events, and full metadata
	 * @throws ExtractorDocumentLevelException 
	 */
	@Override
	public void extractEntities(DocumentPojo partialDoc) throws ExtractorDocumentLevelException, ExtractorDailyLimitExceededException
	{		
		if (null != partialDoc) {
			configure(partialDoc.getTempSource());
			
			if (null == partialDoc.getFullText()) {
				return;
			}
		}
		if (_nBatchSize > 1) {
			
			if (null != partialDoc) {
				BatchInfo batchInfo = _batchedDocuments[_nCurrBatchedDocs];
				if (null == batchInfo) {
					_batchedDocuments[_nCurrBatchedDocs] = (batchInfo = new BatchInfo());
				}
				batchInfo.doc = partialDoc;
				batchInfo.fullText = partialDoc.getFullText();
				_batchText.append(batchInfo.fullText);
				if (!batchInfo.fullText.endsWith(".")) {
					_batchText.append('.');
				}
				_batchText.append('\n');
				_nCurrBatchedDocs++;
			}//TESTED
			
			if ((_nCurrBatchedDocs == _nBatchSize) || 
					((null == partialDoc) && (_nCurrBatchedDocs > 0)))
			{
				if (_nCurrBatchedDocs < _nBatchSize) {
					_batchedDocuments[_nCurrBatchedDocs] = null; // (null-terminated array)
				}
				
				DocumentPojo megaDoc = new DocumentPojo();
				megaDoc.setUrl(_batchedDocuments[0].doc.getUrl() + "|" + _batchedDocuments[_nCurrBatchedDocs-1].doc.getUrl());
				megaDoc.setFullText(_batchText.toString());
				
				_alch.setNumKeywords(_nNumKeywords*_nCurrBatchedDocs);
				int nSavedBatchSize = _nBatchSize;
				// Recurse, but only once since setting _nBatchSize to 1
				_nBatchSize = 1;
				try {
					this.extractEntities(megaDoc);
					
					// Apply megaDoc results to individual docs
					handleBatchProcessing(megaDoc, _batchedDocuments);
				}
				catch (Exception e) {					
					String strError = "Exception Message (0): doc=" + megaDoc.getUrl() + " error=" +  e.getMessage();
					logger.error(strError, e);
					throw new InfiniteEnums.ExtractorDocumentLevelException(strError);
				}
				finally {
					_alch.setNumKeywords(_nNumKeywords); // (<- probably not necessary)
					
					// Tidy up:
					_nBatchSize = nSavedBatchSize;
					_nCurrBatchedDocs = 0;
					_batchText.setLength(0);
				}			
				
			}//TESTED
			
			return; // (don't do anything until batch size complete)
		}
		if (null == partialDoc) {
			return;
		}
		
		// Run through specified extractor need to pull these properties from config file
		// (already checked if this is non-null)
		if (partialDoc.getFullText().length() < 16) { // (don't waste Extractor call/error logging)
			return;
		}

		String json_doc = null;
		try
		{
			String text = partialDoc.getFullText();
			if (text.length() > MAX_LENGTH) {
				text = text.substring(0, MAX_LENGTH);
			}
			json_doc = _alch.TextGetRankedKeywords(text);
			checkAlchemyErrors(json_doc, partialDoc.getUrl());
		}
		catch ( InfiniteEnums.ExtractorDocumentLevelException ex )
		{
			throw ex;
		}
		catch ( InfiniteEnums.ExtractorDailyLimitExceededException ex )
		{
			throw ex;
		}
		catch (Exception e)
		{
			//Collect info and spit out to log
			String strError = "Exception Message (1): doc=" + partialDoc.getUrl() + " error=" +  e.getMessage();
			logger.error(strError, e);
			throw new InfiniteEnums.ExtractorDocumentLevelException(strError);
		}
		
		try
		{			
			//Deserialize json into AlchemyPojo Object
			Gson gson = new Gson();
			AlchemyPojo sc = gson.fromJson(json_doc,AlchemyPojo.class);

			// Turn keywords into entities
			List<EntityPojo> ents = convertKeywordsToEntityPoJo(sc);
			if (null != partialDoc.getEntities()) {
				partialDoc.getEntities().addAll(ents);
				partialDoc.setEntities(partialDoc.getEntities());
			}
			else if (null != ents) {
				partialDoc.setEntities(ents);
			}
			// Alchemy post processsing (empty stub):
			this.postProcessEntities(partialDoc);
		}
		catch (Exception e)
		{
			//Collect info and spit out to log
			String strError = "Exception Message (2): doc=" + partialDoc.getUrl() + " error=" +  e.getMessage();
			logger.error(strError, e);
			throw new InfiniteEnums.ExtractorDocumentLevelException(strError);
		}	
		// Then get concepts:
		if (_bConceptExtraction) {
			doConcepts(partialDoc);
		}
	}

	/**
	 * Simliar to extractEntities except this case assumes that
	 * text extraction has not been done and therefore takes the
	 * url and extracts the full text and entities/events.
	 * 
	 * @param partialDoc The feedpojo before text extraction (empty fulltext field)
	 * @return The feedpojo after text extraction and entity/event extraction with fulltext, entities, events, etc
	 * @throws ExtractorDocumentLevelException 
	 */
	@Override
	public void extractEntitiesAndText(DocumentPojo partialDoc) throws ExtractorDocumentLevelException, ExtractorDailyLimitExceededException
	{
		if (null == partialDoc) {
			return;
		}
		configure(partialDoc.getTempSource());

		if (_nBatchSize > 1) {
			throw new ExtractorDocumentLevelException("Batching not supporting with combined text and entity extraction");
		}		
		
		// Run through specified extractor need to pull these properties from config file
		String json_doc = null;
			// (gets text also)
		try
		{
			json_doc = _alch.URLGetRankedKeywords(partialDoc.getUrl());
			checkAlchemyErrors(json_doc, partialDoc.getUrl());
		}
		catch ( InfiniteEnums.ExtractorDocumentLevelException ex )
		{
			throw ex;
		}
		catch ( InfiniteEnums.ExtractorDailyLimitExceededException ex )
		{
			throw ex;
		}
		catch (Exception e)
		{
			//Collect info and spit out to log
			String strError = "Exception Message (3): doc=" + partialDoc.getUrl() + " error=" +  e.getMessage();
			logger.error(strError, e);
			throw new InfiniteEnums.ExtractorDocumentLevelException();
		}	
		try
		{			
			//Deserialize json into AlchemyPojo Object			
			AlchemyPojo sc = new Gson().fromJson(json_doc,AlchemyPojo.class);			
			//pull fulltext
			if (null == sc.text){
				sc.text = "";
			}
			if (sc.text.length() < 32) { // Try and elongate full text if necessary
				StringBuilder sb = new StringBuilder(partialDoc.getTitle()).append(": ").append(partialDoc.getDescription()).append(". \n").append(sc.text);
				partialDoc.setFullText(sb.toString());
			}
			else {
				partialDoc.setFullText(sc.text);				
			}
			//pull keywords
			List<EntityPojo> ents = convertKeywordsToEntityPoJo(sc);
			if (null != partialDoc.getEntities()) {
				partialDoc.getEntities().addAll(ents);
				partialDoc.setEntities(partialDoc.getEntities());
			}
			else if (null != ents) {
				partialDoc.setEntities(ents);
			}
			// Alchemy post processsing:
			this.postProcessEntities(partialDoc);
		}
		catch (Exception e)
		{
			//Collect info and spit out to log
			String strError = "Exception Message (4): doc=" + partialDoc.getUrl() + " error=" +  e.getMessage();
			logger.error(strError, e);
			throw new InfiniteEnums.ExtractorDocumentLevelException(strError);
		}	
		// Then get concepts:
		if (_bConceptExtraction) {
			doConcepts(partialDoc);
		}
	}

	private void postProcessEntities(DocumentPojo doc) {
		//Nothing to do for now
	}
	
	/**
	 * Attempts to lookup if this extractor has a given capability,
	 * if it does returns value, otherwise null
	 * 
	 * @param capability Extractor capability we are looking for
	 * @return Value of capability, or null if capability not found
	 */
	@Override
	public String getCapability(EntityExtractorEnum capability) 
	{
		return _capabilities.get(capability);
	}	
	
	//_______________________________________________________________________
	//_____________________________TEXT EXTRACTOR FUNCTIONS________________
	//_______________________________________________________________________
	
	/**
	 * Takes a url and spits back the text of the
	 * site, usually cleans it up some too.
	 * 
	 * @param url Site we want the text extracted from
	 * @return The fulltext of the site
	 * @throws ExtractorDocumentLevelException 
	 * @throws ExtractorDailyLimitExceededException 
	 */
	@Override
	public void extractText(DocumentPojo partialDoc) throws ExtractorDocumentLevelException, ExtractorDailyLimitExceededException
	{
		if (null == partialDoc) {
			return;
		}
		configure(partialDoc.getTempSource());
		
		// In this case, extractText and extractTextAndEntities are doing the same thing
		// eg allows for keywords + entities (either from OC or from AA or from any other extractor)
		extractEntitiesAndText(partialDoc);
	}
	
	//_______________________________________________________________________

	//______________________________UTILITY FUNCTIONS_______________________
	//_______________________________________________________________________
	
	// Utility function for concept extraction
	
	private void doConcepts(DocumentPojo partialDoc) throws ExtractorDocumentLevelException, ExtractorDailyLimitExceededException {
		if ((null != partialDoc.getMetadata()) && partialDoc.getMetadata().containsKey("AlchemyAPI_concepts")) {
			return;
		}
		
		String json_doc = null;
		try
		{
			String text = partialDoc.getFullText();
			if (text.length() > MAX_LENGTH) {
				text = text.substring(0, MAX_LENGTH);
			}
			json_doc = _alch.TextGetRankedConcepts(text);
			checkAlchemyErrors(json_doc, partialDoc.getUrl());
		}
		catch ( InfiniteEnums.ExtractorDocumentLevelException ex )
		{
			throw ex;
		}
		catch ( InfiniteEnums.ExtractorDailyLimitExceededException ex )
		{
			throw ex;
		}
		catch (Exception e) {
			//Collect info and spit out to log
			String strError = "Exception Message (5): doc=" + partialDoc.getUrl() + " error=" +  e.getMessage();
			logger.error(strError, e);
			throw new InfiniteEnums.ExtractorDocumentLevelException(strError);			
		}
		try {
			// Turn concepts into metadata:
			Gson gson = new Gson();
			AlchemyPojo sc = gson.fromJson(json_doc,AlchemyPojo.class);
			if (null != sc.concepts) {
				partialDoc.addToMetadata("AlchemyAPI_concepts", sc.concepts.toArray(new AlchemyConceptPojo[sc.concepts.size()]));
			}
		}		
		catch (Exception e)
		{
			//Collect info and spit out to log
			String strError = "Exception Message (6): doc=" + partialDoc.getUrl() + " error=" +  e.getMessage();
			logger.error(strError, e);
			throw new InfiniteEnums.ExtractorDocumentLevelException(strError);
		}	
	}

	/**
	 * Converts the json return from alchemy into a list
	 * of entitypojo objects.
	 * 
	 * @param json The json text that alchemy creates for a document
	 * @return A list of EntityPojo's that have been extracted from the document.
	 */
	private List<EntityPojo> convertKeywordsToEntityPoJo(AlchemyPojo sc)
	{
		
		//convert alchemy object into a list of entity pojos
		List<EntityPojo> ents = new ArrayList<EntityPojo>();
		if ( sc.keywords != null)
		{
			for ( AlchemyKeywordPojo ae : sc.keywords)
			{
				EntityPojo ent = convertAlchemyKeywordToEntPojo(ae);
				if ( ent != null )
					ents.add(ent);
			}
		}
		return ents;	
	}
	
	/**
	 * Checks the json returned from alchemy so we can handle
	 * any exceptions
	 * 
	 * @param json_doc
	 * @return
	 * @throws ExtractorDailyLimitExceededException 
	 * @throws ExtractorDocumentLevelException 
	 * @throws ExtractorSourceLevelException 
	 */
	private void checkAlchemyErrors(String json_doc, String feed_url) throws ExtractorDailyLimitExceededException, ExtractorDocumentLevelException, ExtractorSourceLevelException 
	{		
		if ( json_doc.contains("daily-transaction-limit-exceeded") )
		{
			logger.error("AlchemyAPI daily limit exceeded");
			throw new InfiniteEnums.ExtractorDailyLimitExceededException();			
		}
		else if ( json_doc.contains("cannot-retrieve:http-redirect") )
		{
			String strError = "AlchemyAPI redirect error on url=" + feed_url;
			logger.error(strError);
			throw new InfiniteEnums.ExtractorDocumentLevelException(strError);
		}
		else if ( json_doc.contains("cannot-retrieve:http-error:4") )
		{
			String strError = "AlchemyAPI cannot retrieve error on url=" + feed_url;
			logger.error(strError);
			throw new InfiniteEnums.ExtractorDocumentLevelException(strError);			
		}
		else if ( json_doc.contains("invalid-api-key") )
		{
			logger.error("AlchemyAPI invalid API key");
			throw new InfiniteEnums.ExtractorSourceLevelException("AlchemyAPI invalid API key");						
		}
	}
	
	// Utility function to convert an Alchemy entity to an Infinite entity
	
	private static EntityPojo convertAlchemyKeywordToEntPojo(AlchemyKeywordPojo pojoToConvert)
	{
		try
		{
			EntityPojo ent = new EntityPojo();
			ent.setActual_name(pojoToConvert.text);
			ent.setType("Keyword");
			ent.setRelevance(Double.parseDouble(pojoToConvert.relevance));
			ent.setFrequency(1L); 
			if (null != pojoToConvert.sentiment) {
				if (null != pojoToConvert.sentiment.score) {
					ent.setSentiment(Double.parseDouble(pojoToConvert.sentiment.score));
				}
				else { // neutral
					ent.setSentiment(0.0);
				}
			}
			// (else no sentiment present)
			
			ent.setDisambiguatedName(pojoToConvert.text);
			ent.setActual_name(pojoToConvert.text);
			
			ent.setDimension(EntityPojo.Dimension.What);
			return ent;
		}
		catch (Exception ex)
		{
			logger.error("Line: [" + ex.getStackTrace()[2].getLineNumber() + "] " + ex.getMessage());
			ex.printStackTrace();
			//******************BUGGER***********
			//LOG ERROR TO A LOG
		}
		return null;
	}

	//_______________________________________________________________________

	//______________________________BATCH PROCESSING_________________________
	//_______________________________________________________________________

	private static final int nMaxKeywordsPerSearch = 50; 
	
	private void handleBatchProcessing(DocumentPojo megaDoc, BatchInfo[] batchInfo)
	{
		int nCurrKeywords = 0;
		HashMap<String, EntityPojo> currKeywordMap = new HashMap<String, EntityPojo>();
		StringBuffer keywordRegexBuff = new StringBuffer();
		
		for (EntityPojo ent: megaDoc.getEntities()) {
			
			String disName = ent.getDisambiguatedName().toLowerCase();
			if (!currKeywordMap.containsKey(disName)) {
				keywordRegexBuff.append(Pattern.quote(ent.getDisambiguatedName())).append('|');
				currKeywordMap.put(disName, ent);
				nCurrKeywords++;
				
				if (nMaxKeywordsPerSearch == nCurrKeywords) {
					keywordRegexBuff.setLength(keywordRegexBuff.length() - 1);
					
					handleBatchProcessing_keywordSet(batchInfo, keywordRegexBuff.toString(), currKeywordMap);
					
					currKeywordMap.clear();
					keywordRegexBuff.setLength(0);
					nCurrKeywords = 0;
				}
			}// (else entity already seen - won't normally happen I think)
		}//end loop over entities
		if (nCurrKeywords > 0) {
			
			keywordRegexBuff.setLength(keywordRegexBuff.length() - 1);			
			handleBatchProcessing_keywordSet(batchInfo, keywordRegexBuff.toString(), currKeywordMap);
		}
	}//TESTED
	
	private void handleBatchProcessing_keywordSet(BatchInfo[] batchInfo, String currKeywordRegexStr, HashMap<String, EntityPojo> currKeywordMap)
	{
		Pattern currKeywordRegex = Pattern.compile(currKeywordRegexStr, Pattern.CASE_INSENSITIVE);
		
		long nDoc = 0;
		for (BatchInfo batchedDoc: batchInfo) {
			nDoc--;
			
			if (null == batchedDoc) { // null-terminated array
				break;
			}			
			Matcher m = currKeywordRegex.matcher(batchedDoc.fullText);

			while (m.find()) {
				
				String name = m.group().toLowerCase();
				EntityPojo ent = currKeywordMap.get(name);
				
				if ((null != ent) && (nDoc != ent.getDoccount())) { // (see below) 
					if (null == batchedDoc.doc.getEntities()) {
						batchedDoc.doc.setEntities(new ArrayList<EntityPojo>());
					}
					batchedDoc.doc.getEntities().add(ent);
					ent.setDoccount(nDoc);
						// use this as an efficient check to only add each entity once per doc
						// doccount gets overwritten by the generic processing module so fine to abuse this
				}
				// (else probably an internal logic error ie shouldn't happen)
				
			} // end loop over matches
		}//end loop over matched docs
	}//TESTED
}
