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
import com.ikanow.infinit.e.data_model.interfaces.harvest.EntityExtractorEnum;
import com.ikanow.infinit.e.data_model.interfaces.harvest.IEntityExtractor;
import com.ikanow.infinit.e.data_model.interfaces.harvest.ITextExtractor;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.infinit.e.data_model.store.document.EntityPojo;
import com.ikanow.infinit.e.data_model.store.document.GeoPojo;
import com.ikanow.infinit.e.harvest.utils.DimensionUtility;

public class ExtractorAlchemyAPI_Metadata implements IEntityExtractor, ITextExtractor 
{
	@Override
	public String getName() { return "alchemyapi-metadata"; }
	
	private static final Logger logger = Logger.getLogger(ExtractorAlchemyAPI_Metadata.class);
	private AlchemyAPI_JSON _alch = AlchemyAPI_JSON.GetInstanceFromProperties();
	private Map<EntityExtractorEnum, String> _capabilities = new HashMap<EntityExtractorEnum, String>();	

	/**
	 * Construtor, adds capabilities of Alchemy to hashmap
	 */
	public ExtractorAlchemyAPI_Metadata()
	{
		//insert capabilities of this extractor
		_capabilities.put(EntityExtractorEnum.Name, "AlchemyAPI-metadata");
		_capabilities.put(EntityExtractorEnum.Quality, "1");
		_capabilities.put(EntityExtractorEnum.URLTextExtraction, "true");
		_capabilities.put(EntityExtractorEnum.GeotagExtraction, "true");
		_capabilities.put(EntityExtractorEnum.SentimentExtraction, "false");
		
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
	public void extractEntities(DocumentPojo partialDoc) throws ExtractorDocumentLevelException 
	{		
		// Run through specified extractor need to pull these properties from config file
		try
		{
			if (partialDoc.getFullText().length() < 16) { // (don't waste Extractor call/error logging)
				return;
			}

			String json_doc = _alch.TextGetRankedKeywords(partialDoc.getFullText());
			try
			{
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
			
			// Then get concepts:
			//TODO (INF-997): commented out until we work out what to do with all this 
//			json_doc = _alch.TextGetRankedConcepts(partialDoc.getFullText());
//			try
//			{
//				checkAlchemyErrors(json_doc, partialDoc.getUrl());
//			}
//			catch ( InfiniteEnums.ExtractorDocumentLevelException ex )
//			{
//				throw ex;
//			}
//			catch ( InfiniteEnums.ExtractorDailyLimitExceededException ex )
//			{
//				throw ex;
//			}
//			// Turn concepts into metadata:
//			sc = gson.fromJson(json_doc,AlchemyPojo.class);
//			if (null != sc.concepts) {
//				partialDoc.addToMetadata("AlchemyAPI_concepts", sc.concepts.toArray(new AlchemyConceptPojo[sc.concepts.size()]));
//			}
			
			// Alchemy post processsing (empty stub):
			this.postProcessEntities(partialDoc);
		}
		catch (Exception e)
		{
			//Collect info and spit out to log
			logger.error("Exception Message: doc=" + partialDoc.getUrl() + " error=" +  e.getMessage(), e);
			throw new InfiniteEnums.ExtractorDocumentLevelException();
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
	public void extractEntitiesAndText(DocumentPojo partialDoc) throws ExtractorDocumentLevelException 
	{
		// Run through specified extractor need to pull these properties from config file
		try
		{			
			String json_doc = _alch.URLGetRankedKeywords(partialDoc.getUrl());
				// (gets text also)
			try
			{
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
			//TODO (INF-997): replicate whatever topic extraction is done for extractEntities  
			
			// Alchemy post processsing:
			this.postProcessEntities(partialDoc);
		}
		catch (Exception e)
		{
			//Collect info and spit out to log
			logger.error("Exception Message: doc=" + partialDoc.getUrl() + " error=" +  e.getMessage(), e);
			throw new InfiniteEnums.ExtractorDocumentLevelException();
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
	 */
	@Override
	public void extractText(DocumentPojo doc) throws ExtractorDocumentLevelException
	{
		// In this case, extractText and extractTextAndEntities are doing the same thing
		// eg allows for keywords + entities (either from OC or from AA or from any other extractor)
		extractEntitiesAndText(doc);
	}
	
	//_______________________________________________________________________
	//______________________________UTILIY FUNCTIONS_______________________
	//_______________________________________________________________________
	
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
	 */
	private void checkAlchemyErrors(String json_doc, String feed_url) throws ExtractorDailyLimitExceededException, ExtractorDocumentLevelException 
	{
		if ( json_doc.contains("daily-transaction-limit-exceeded") )
		{
			logger.error("AlchemyAPI daily limit exceeded");
			throw new InfiniteEnums.ExtractorDailyLimitExceededException();			
		}
		else if ( json_doc.contains("cannot-retrieve:http-redirect") )
		{
			logger.error("AlchemyAPI redirect error on url=" + feed_url);
			throw new InfiniteEnums.ExtractorDocumentLevelException();
		}
		else if ( json_doc.contains("cannot-retrieve:http-error:4") )
		{
			logger.error("AlchemyAPI cannot retrieve error on url=" + feed_url);
			throw new InfiniteEnums.ExtractorDocumentLevelException();			
		}
	}
	
	// Utility function to convert an Alchemy entity to an Infinite entity
	
	public static EntityPojo convertAlchemyEntToEntPojo(AlchemyEntityPojo pojoToConvert)
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
			ent.setDimension(DimensionUtility.getDimensionByType(ent.getType()));
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

	// Utility function to convert an Alchemy entity to an Infinite entity
	
	public static EntityPojo convertAlchemyKeywordToEntPojo(AlchemyKeywordPojo pojoToConvert)
	{
		try
		{
			EntityPojo ent = new EntityPojo();
			ent.setActual_name(pojoToConvert.text);
			ent.setType("Keyword");
			ent.setRelevance(Double.parseDouble(pojoToConvert.relevance));
			double dPseudoFreq = (Math.exp(ent.getRelevance()) - 1.0)*(20.0/(Math.E - 1.0)); // (ie between 0 and 20, assume relevance is log scale)
			ent.setFrequency(1L + (long)(dPseudoFreq)); //(ie it's a pseudo-frequency)
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
}
