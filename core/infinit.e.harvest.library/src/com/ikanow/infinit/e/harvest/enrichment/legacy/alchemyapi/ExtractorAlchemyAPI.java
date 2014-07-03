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
import com.ikanow.infinit.e.data_model.store.document.GeoPojo;
import com.ikanow.infinit.e.harvest.extraction.text.legacy.TextExtractorTika;
import com.ikanow.infinit.e.data_model.utils.DimensionUtility;
import com.ikanow.infinit.e.harvest.utils.PropertiesManager;

public class ExtractorAlchemyAPI implements IEntityExtractor, ITextExtractor 
{
	@Override
	public String getName() { return "alchemyapi"; }
	
	private static final Logger logger = Logger.getLogger(ExtractorAlchemyAPI.class);
	private AlchemyAPI_JSON _alch = AlchemyAPI_JSON.GetInstanceFromProperties();
	private Map<EntityExtractorEnum, String> _capabilities = new HashMap<EntityExtractorEnum, String>();
	
	private static final int MAX_LENGTH = 145000;

	// Post processing to clean up people and geo entities
	protected AlchemyEntityPersonCleanser postProcPerson = null;
	protected AlchemyEntityGeoCleanser postProcGeo = null;
	private boolean _bConceptExtraction = false;

	protected TextExtractorTika _tikaExtractor = null;
	
	//_______________________________________________________________________
	//_____________________________INITIALIZATION________________
	//_______________________________________________________________________

	/**
	 * Construtor, adds capabilities of Alchemy to hashmap
	 */
	public ExtractorAlchemyAPI()
	{
		//insert capabilities of this extractor
		_capabilities.put(EntityExtractorEnum.Name, "AlchemyAPI");
		_capabilities.put(EntityExtractorEnum.Quality, "1");
		_capabilities.put(EntityExtractorEnum.URLTextExtraction, "true");
		_capabilities.put(EntityExtractorEnum.GeotagExtraction, "true");
		_capabilities.put(EntityExtractorEnum.SentimentExtraction, "true");
		_capabilities.put(EntityExtractorEnum.MaxInputBytes, Integer.toString(MAX_LENGTH));
		
		// configuration done when the first document is received for this source
	}
	
	// Configuration: override global configuration on a per source basis
	
	private boolean configured = false;
	
	private void configure(SourcePojo source)
	{
		if (configured) {
			return;
		}
		configured = true;
		
		// SOURCE OVERRIDE
		
		int nPostProc = -1;
		Boolean bSentimentEnabled = null;
		Boolean bConceptsEnabled = null;
		String apiKey = null;
		
		if ((null != source) && (null != source.getExtractorOptions())) {
			try {
				nPostProc = Integer.parseInt(source.getExtractorOptions().get("app.alchemyapi.postproc"));				
			}
			catch (Exception e){}
			
			try {
				apiKey = source.getExtractorOptions().get("app.alchemyapi-metadata.apiKeyOverride");
			}
			catch (Exception e){}
			try {
				String s = source.getExtractorOptions().get("app.alchemyapi.sentiment");
				if (null != s) bSentimentEnabled = Boolean.parseBoolean(s);
			}
			catch (Exception e){}
			try {
				String s = source.getExtractorOptions().get("app.alchemyapi.concepts");
				if (null != s) bConceptsEnabled = Boolean.parseBoolean(s);						
			}
			catch (Exception e){}
			
		}
		// DEFAULT CONFIGURATION
		
		PropertiesManager properties = new PropertiesManager();
		
		// 1] POST PROC
		
		if (-1 == nPostProc) { // (ie no per source config)
			try {
				nPostProc = properties.getAlchemyPostProcessingSetting();				
			}		
			catch (Exception e) {
				nPostProc = -1;
			} 		
		}
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

		// ACTUALLY DO CONFIG
		
		try {
			if (-1 != nPostProc) { // (ie some configuration enabled)
				if (0 != (1 & nPostProc)) {
					postProcPerson = new AlchemyEntityPersonCleanser();
					postProcPerson.initialize();
				} 
				if (0 != (2 & nPostProc)) {
					postProcGeo = new AlchemyEntityGeoCleanser();
					postProcGeo.initialize();
				} 							
			}
			else {
				postProcPerson = null; // (just don't do post processing)
				postProcGeo = null; // (just don't do post processing)			
			}
		}
		catch (Exception e) {
			postProcPerson = null; // (just don't do post processing)
			postProcGeo = null; // (just don't do post processing)						
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
	 * @throws ExtractorDailyLimitExceededException 
	 */
	@Override
	public void extractEntities(DocumentPojo partialDoc) throws ExtractorDocumentLevelException, ExtractorDailyLimitExceededException 
	{		
		if (null == partialDoc) {
			return;
		}
		configure(partialDoc.getTempSource());
		
		// Run through specified extractor need to pull these properties from config file
		if (null == partialDoc.getFullText()) {
			return;
		}
		if (partialDoc.getFullText().length() < 16) { // Else don't waste Extractor call/error logging			
			return;
		}
		
		String json_doc = null;
			
		try
		{
			String text = partialDoc.getFullText();
			if (text.length() > MAX_LENGTH) {
				text = text.substring(0, MAX_LENGTH);
			}
			json_doc = _alch.TextGetRankedNamedEntities(text);
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
		catch ( Exception e ) 
		{
			//Collect info and spit out to log
			String strError = "Exception Message (1): doc=" + partialDoc.getUrl() + " error=" +  e.getMessage();
			logger.error(strError, e);
			throw new InfiniteEnums.ExtractorDocumentLevelException(strError);
		}

		try {
			//Deserialize json into AlchemyPojo Object
			Gson gson = new Gson();
			AlchemyPojo sc = gson.fromJson(json_doc,AlchemyPojo.class);
			List<EntityPojo> ents = convertToEntityPoJo(sc);
			if (null != partialDoc.getEntities()) {
				partialDoc.getEntities().addAll(ents);
				partialDoc.setEntities(partialDoc.getEntities());
			}
			else {
				partialDoc.setEntities(ents);
			}
			
			// Alchemy post processsing:
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
		
		// Run through specified extractor need to pull these properties from config file
		String json_doc = null;
		try
		{
			json_doc = _alch.URLGetRankedNamedEntities(partialDoc.getUrl());
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
			throw new InfiniteEnums.ExtractorDocumentLevelException(strError);
		}	
		
		try
		{	
			//Deserialize json into AlchemyPojo Object			
			AlchemyPojo sc = new Gson().fromJson(json_doc,AlchemyPojo.class);			
			//pull fulltext
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
			//pull entities
			List<EntityPojo> ents = convertToEntityPoJo(sc);
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
		if (null != postProcPerson) {
			try {
				postProcPerson.cleansePeopleInDocu(doc);
			}
			catch (Exception e) {} // do nothing, just carry on
		}
		if (null != postProcGeo) {
			try {
				postProcGeo.cleanseGeoInDocu(doc);
			}
			catch (Exception e) {} // do nothing, just carry on
		}		
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
	 */
	@Override
	public void extractText(DocumentPojo partialDoc) throws ExtractorDocumentLevelException, ExtractorDailyLimitExceededException
	{
		if (null == partialDoc) {
			return;
		}
		configure(partialDoc.getTempSource());
		
		// Quick check: if it's PDF then send it to tika instead
		String tmpUrl = partialDoc.getUrl(); 
		int endIndex = tmpUrl.indexOf('?');
		if (endIndex > 0) {
			tmpUrl = tmpUrl.substring(0, endIndex);
		}
		endIndex = tmpUrl.indexOf('#');
		if (endIndex > 0) {
			tmpUrl = tmpUrl.substring(0, endIndex);
		}
		if (tmpUrl.endsWith(".pdf") || tmpUrl.endsWith(".doc") || tmpUrl.endsWith(".docx") || tmpUrl.endsWith(".xls") || tmpUrl.endsWith(".xlsx"))
		{ 
			//(eventually should detect error from AApi and send to tika on certain error types)
			if (null == _tikaExtractor) {
				_tikaExtractor = new TextExtractorTika();
			}
			_tikaExtractor.extractText(partialDoc);
			return;
		}
		//TESTED
		
		String json_doc = null;
		try
		{
			json_doc = _alch.URLGetText(partialDoc.getUrl());
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
			String strError = "Exception Message (5): doc=" + partialDoc.getUrl() + " error=" +  e.getMessage();
			logger.error(strError, e);
			throw new InfiniteEnums.ExtractorDocumentLevelException(strError);
		}
		
		try
		{			
			//Deserialize json into AlchemyPojo Object
			Gson gson = new Gson();
			AlchemyPojo sc = gson.fromJson(json_doc,AlchemyPojo.class);	
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
		}
		catch (Exception e)
		{
			//Collect info and spit out to log
			String strError = "Exception Message (6): doc=" + partialDoc.getUrl() + " error=" +  e.getMessage();
			logger.error(strError, e);
			throw new InfiniteEnums.ExtractorDocumentLevelException(strError);
		}	
		// Then get concepts:
		if (_bConceptExtraction) {
			doConcepts(partialDoc);
		}
	}
	
	//_______________________________________________________________________
	//______________________________UTILIY FUNCTIONS_______________________
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
			String strError = "Exception Message (7): doc=" + partialDoc.getUrl() + " error=" +  e.getMessage();
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
			String strError = "Exception Message (8): doc=" + partialDoc.getUrl() + " error=" +  e.getMessage();
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
	private List<EntityPojo> convertToEntityPoJo(AlchemyPojo sc)
	{
		
		//convert alchemy object into a list of entity pojos
		List<EntityPojo> ents = new ArrayList<EntityPojo>();
		if ( sc.entities != null)
		{
			for ( AlchemyEntityPojo ae : sc.entities)
			{
				EntityPojo ent = convertAlchemyEntToEntPojo(ae);
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
	
	private static EntityPojo convertAlchemyEntToEntPojo(AlchemyEntityPojo pojoToConvert)
	{
		try
		{
			EntityPojo ent = new EntityPojo();
			ent.setActual_name(pojoToConvert.text);
			ent.setType(pojoToConvert.type);
			ent.setRelevance(Double.parseDouble(pojoToConvert.relevance));
			ent.setFrequency(Long.parseLong(pojoToConvert.count));
			if (null != pojoToConvert.sentiment) {
				if (null != pojoToConvert.sentiment.score) {
					ent.setSentiment(Double.parseDouble(pojoToConvert.sentiment.score));
				}
				else { // neutral
					ent.setSentiment(0.0);
				}
			}
			// (else no sentiment present)
			
			if ( pojoToConvert.disambiguated != null )
			{
				ent.setSemanticLinks(new ArrayList<String>());
				ent.setDisambiguatedName(pojoToConvert.disambiguated.name);
				if ( pojoToConvert.disambiguated.geo != null )
				{
					GeoPojo geo = new GeoPojo();
					String[] geocords = pojoToConvert.disambiguated.geo.split(" ");
					geo.lat = Double.parseDouble(geocords[0]);
					geo.lon = Double.parseDouble(geocords[1]);
					ent.setGeotag(geo);
				}
				//Add link data if applicable
				if ( pojoToConvert.disambiguated.census != null)
					ent.getSemanticLinks().add(pojoToConvert.disambiguated.census);
				if ( pojoToConvert.disambiguated.ciaFactbook != null)
					ent.getSemanticLinks().add(pojoToConvert.disambiguated.ciaFactbook);
				if ( pojoToConvert.disambiguated.dbpedia != null)
					ent.getSemanticLinks().add(pojoToConvert.disambiguated.dbpedia);
				if ( pojoToConvert.disambiguated.freebase != null)
					ent.getSemanticLinks().add(pojoToConvert.disambiguated.freebase);
				if ( pojoToConvert.disambiguated.opencyc != null)
					ent.getSemanticLinks().add(pojoToConvert.disambiguated.opencyc);
				if ( pojoToConvert.disambiguated.umbel != null)
					ent.getSemanticLinks().add(pojoToConvert.disambiguated.umbel);
				if ( pojoToConvert.disambiguated.yago != null)
					ent.getSemanticLinks().add(pojoToConvert.disambiguated.yago);
				
				if ( ent.getSemanticLinks().size() == 0)
					ent.setSemanticLinks(null); //If no links got added, remove the list
			}
			else
			{
				//sets the disambig name to actual name if
				//there was no disambig name for this ent
				//that way all entities have a disambig name
				ent.setDisambiguatedName(ent.getActual_name());
			}
			//Calculate Dimension based on ent type
			try {
				ent.setDimension(DimensionUtility.getDimensionByType(ent.getType()));
			}
			catch (java.lang.IllegalArgumentException e) {
				ent.setDimension(EntityPojo.Dimension.What);									
			}
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
}
