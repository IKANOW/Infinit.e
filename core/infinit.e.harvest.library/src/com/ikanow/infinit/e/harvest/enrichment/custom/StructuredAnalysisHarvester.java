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
package com.ikanow.infinit.e.harvest.enrichment.custom;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.script.*;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.ikanow.infinit.e.data_model.InfiniteEnums;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.ikanow.infinit.e.data_model.store.config.source.StructuredAnalysisConfigPojo;
import com.ikanow.infinit.e.data_model.store.config.source.StructuredAnalysisConfigPojo.GeoSpecPojo;
import com.ikanow.infinit.e.data_model.store.config.source.StructuredAnalysisConfigPojo.EntitySpecPojo;
import com.ikanow.infinit.e.data_model.store.config.source.StructuredAnalysisConfigPojo.AssociationSpecPojo;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.infinit.e.data_model.store.document.EntityPojo;
import com.ikanow.infinit.e.data_model.store.document.AssociationPojo;
import com.ikanow.infinit.e.data_model.store.document.GeoPojo;
import com.ikanow.infinit.e.data_model.store.feature.geo.GeoFeaturePojo;
import com.ikanow.infinit.e.data_model.utils.GeoOntologyMapping;
import com.ikanow.infinit.e.harvest.HarvestContext;
import com.ikanow.infinit.e.harvest.HarvestController;
import com.ikanow.infinit.e.harvest.utils.DateUtility;
import com.ikanow.infinit.e.harvest.utils.AssociationUtils;
import com.ikanow.infinit.e.harvest.utils.DimensionUtility;
import com.ikanow.infinit.e.harvest.utils.HarvestExceptionUtils;

/**
 * StructuredAnalysisHarvester
 * @author cvitter
 */
public class StructuredAnalysisHarvester 
{
	// Private class variables
	private static Logger logger;
	private Set<Integer> sourceTypesCanHarvest = new HashSet<Integer>();
	private JSONObject document = null;
	private JSONObject iterator = null;
	private String iteratorIndex = null;
	private static Pattern pattern = Pattern.compile("\\$([a-zA-Z._0-9]+)|\\$\\{([^}]+)\\}");
	private static HashMap<String, GeoPojo> geoMap = new HashMap<String, GeoPojo>();
	
	private HarvestContext _context;
	
	/**
	 * Default Constructor
	 */
	public StructuredAnalysisHarvester()
	{			
		sourceTypesCanHarvest.add(InfiniteEnums.STRUCTUREDANALYSIS);
		logger = Logger.getLogger(StructuredAnalysisHarvester.class);
	}
	
	// Allows the unstructured handler to take advantage of text created by this
	public void addUnstructuredHandler(UnstructuredAnalysisHarvester uap) {
		unstructuredHandler = uap;
	}
	private UnstructuredAnalysisHarvester unstructuredHandler = null;
	
	// 
	private ScriptEngineManager factory = null;
	private ScriptEngine engine = null;
	private Invocable inv = null;
	

	/**
	 * executeHarvest(SourcePojo source, List<DocumentPojo> feeds) extracts document GEO, Entities,
	 * and Associations based on the DocGeoSpec, EntitySpec, and AssociationSpec information contained
	 * within the source document's StructuredAnalysis sections
	 * @param source
	 * @param docs
	 * @return List<DocumentPojo>
	 * @throws ScriptException 
	 */
	// (Utility function for optimization)
	private void intializeDocIfNeeded(DocumentPojo f, Gson g) throws JSONException, ScriptException {
		if (null == document) {
			// Convert the DocumentPojo Object to a JSON document using GsonBuilder
			document = new JSONObject(g.toJson(f));
			// Add the document (JSONObject) to the engine
			if (null != engine) {
		        engine.put("document", document);
	        	engine.eval(JavaScriptUtils.initScript);
			}
		}
	}
	public List<DocumentPojo> executeHarvest(HarvestController contextController, SourcePojo source, List<DocumentPojo> docs)
	{
		_context = contextController;
		
		// Skip if the StructuredAnalysis object of the source is null 
		if (source.getStructuredAnalysisConfig() != null)
		{
			StructuredAnalysisConfigPojo s = source.getStructuredAnalysisConfig();			
			
			// Instantiate a new ScriptEngineManager and create an engine to execute  
			// the type of script specified in StructuredAnalysisPojo.scriptEngine
			if (s.getScriptEngine() != null)
			{
				factory = new ScriptEngineManager();
				if ((null == s.getScriptEngine()) || s.getScriptEngine().equalsIgnoreCase("javascript")) {
					s.setScriptEngine("JavaScript"); // (sigh case sensitive)
				}
				engine = factory.getEngineByName(s.getScriptEngine());
			}
			
			// Create a GsonBuilder that we will use to convert the feed to JSON	
			GsonBuilder gb = new GsonBuilder();
			Gson g = gb.create();
			
			// Iterate over each doc in docs, create entity and association pojo objects
			// to add to the feed using the source entity and association spec pojos
			Iterator<DocumentPojo> it = docs.iterator();
			int nDocs = 0;
			while (it.hasNext())
			{
				DocumentPojo f = it.next();
				nDocs++;
				try 
				{ 
					document = null;
						// (don't create this until needed, since it might need to be (re)serialized after a call
						//  to the UAH which would obviously be undesirable)
								        					
					// If the script engine has been instantiated pass the feed document and any scripts
					if (engine != null)
					{
						// Script code embedded in source
						String script = (s.getScript() != null) ? s.getScript(): null;
						
						// scriptFiles - can contain String[] of script files to import into the engine
						String[] scriptFiles = (s.getScriptFiles() != null) ? s.getScriptFiles(): null;
						
				        // Pass scripts into the engine
				        try 
				        {
				        	// Eval script passed in s.script
				        	if (script != null) engine.eval(script);
				        	
				        	// Retrieve and eval script files in s.scriptFiles
				        	if (scriptFiles != null)
				        	{
				        		for (String file : scriptFiles)
				        		{
				        			engine.eval(JavaScriptUtils.getJavaScriptFile(file));
				        		}
				        	}
						} 
				        catch (ScriptException e) 
						{
							this._context.getHarvestStatus().logMessage("ScriptException: " + e.getMessage(), true);						
							logger.error("ScriptException: " + e.getMessage(), e);
						}
				        
				        // Make the engine invocable so that we can call functions in the script
				        // using the inv.invokeFunction(function) method
				        inv = (Invocable) engine;
					}
					
			// 1. Document level fields
					
					// Extract Title if applicable
					boolean bTryTitleLater = false;
					try {
						if (s.getTitle() != null)
						{
							intializeDocIfNeeded(f, g);
							if (JavaScriptUtils.containsScript(s.getTitle()))
							{
								f.setTitle((String)getValueFromScript(s.getTitle(), null, null));
							}
							else
							{
								f.setTitle(getFormattedTextFromField(s.getTitle()));
							}
							if (null == f.getTitle()) {
								bTryTitleLater = true;
							}
						}
					}
					catch (Exception e) 
					{
						this._context.getHarvestStatus().logMessage("title: " + e.getMessage(), true);						
						logger.error("title: " + e.getMessage(), e);
					}

					// Extract Description if applicable
					boolean bTryDescriptionLater = false;
					try {
						if (s.getDescription() != null)
						{
							intializeDocIfNeeded(f, g);
							if (JavaScriptUtils.containsScript(s.getDescription()))
							{
								f.setDescription((String)getValueFromScript(s.getDescription(), null, null));
							}
							else
							{
								f.setDescription(getFormattedTextFromField(s.getDescription()));
							}
							if (null == f.getDescription()) {
								bTryDescriptionLater = true;
							}
						}
					}
					catch (Exception e) 
					{
						this._context.getHarvestStatus().logMessage("description: " + e.getMessage(), true);						
						logger.error("description: " + e.getMessage(), e);
					}
					

					// Extract fullText if applicable
					boolean bTryFullTextLater = false;
					try {
						if (s.getFullText() != null)
						{
							intializeDocIfNeeded(f, g);
							if (JavaScriptUtils.containsScript(s.getFullText()))
							{
								f.setFullText((String)getValueFromScript(s.getFullText(), null, null));
							}
							else
							{
								f.setFullText(getFormattedTextFromField(s.getFullText()));
							}
							if (null == f.getFullText()) {
								bTryFullTextLater = true;
							}
						}
					}
					catch (Exception e) 
					{
						this._context.getHarvestStatus().logMessage("fullText: " + e.getMessage(), true);						
						logger.error("fullText: " + e.getMessage(), e);
					}
					
					// Published date and URL are done after the UAH 
					// (since the UAH can't access them, and they might be populated via the UAH)
					
			// 2. UAH/extraction properties
					
					// Add fields to metadata that can be used to create entities and associations
					// (Either with the UAH, or with the entity extractor)
					try {
						boolean bMetadataChanged = false;
						if (null != this.unstructuredHandler) {
							bMetadataChanged = this.unstructuredHandler.executeHarvest(_context, source, f, (1 == nDocs), it.hasNext());
						}	
						if (contextController.isEntityExtractionRequired(source))
						{
							bMetadataChanged = true;
							
							// Text/Entity Extraction 
							List<DocumentPojo> toAdd = new ArrayList<DocumentPojo>(1);
							toAdd.add(f);
							try {
								contextController.extractTextAndEntities(toAdd, source);
							}
							catch (Exception e) {
								contextController.handleExtractError(e, source); //handle extractor error if need be				
							}
						}
						if (bMetadataChanged) {
							// Ugly, but need to re-create doc json because metadata has changed
							String sTmpFullText = f.getFullText();
							f.setFullText(null); // (no need to serialize this, can save some cycles)
							document = null;
							intializeDocIfNeeded(f, g);							
					        f.setFullText(sTmpFullText); //(restore)
						}
					}
					catch (Exception e) {
						this._context.getHarvestStatus().logMessage("SAH->UAH: " + e.getMessage(), true);						
						logger.error("SAH->UAH: " + e.getMessage(), e);
					}
						
					// Now create document since there's no risk of having to re-serialize
					intializeDocIfNeeded(f, g);
					
			// 3. final doc-level metadata fields:
					
					// If description was null before might need to get it from a UAH field
					if (bTryTitleLater) {
						try {
							if (s.getTitle() != null)
							{
								intializeDocIfNeeded(f, g);
								if (JavaScriptUtils.containsScript(s.getTitle()))
								{
									f.setTitle((String)getValueFromScript(s.getTitle(), null, null));
								}
								else
								{
									f.setTitle(getFormattedTextFromField(s.getTitle()));
								}
							}
						}
						catch (Exception e) 
						{
							this._context.getHarvestStatus().logMessage("title: " + e.getMessage(), true);						
							logger.error("title: " + e.getMessage(), e);
						}
					}
					
					// If description was null before might need to get it from a UAH field
					if (bTryDescriptionLater) {
						try {
							if (s.getDescription() != null)
							{
								intializeDocIfNeeded(f, g);
								if (JavaScriptUtils.containsScript(s.getDescription()))
								{
									f.setDescription((String)getValueFromScript(s.getDescription(), null, null));
								}
								else
								{
									f.setDescription(getFormattedTextFromField(s.getDescription()));
								}
							}
						}
						catch (Exception e) 
						{
							this._context.getHarvestStatus().logMessage("description2: " + e.getMessage(), true);						
							logger.error("description2: " + e.getMessage(), e);
						}						
					}
					
					// If fullText was null before might need to get it from a UAH field
					if (bTryFullTextLater) {
						try {
							if (s.getFullText() != null)
							{
								intializeDocIfNeeded(f, g);
								if (JavaScriptUtils.containsScript(s.getFullText()))
								{
									f.setFullText((String)getValueFromScript(s.getFullText(), null, null));
								}
								else
								{
									f.setFullText(getFormattedTextFromField(s.getFullText()));
								}
							}
						}
						catch (Exception e) 
						{
							this._context.getHarvestStatus().logMessage("fullText: " + e.getMessage(), true);						
							logger.error("fullText: " + e.getMessage(), e);
						}						
					}
					
					// Extract Published Date if applicable
					if (s.getPublishedDate() != null)
					{
						if (JavaScriptUtils.containsScript(s.getPublishedDate()))
						{
							try 
							{
								f.setPublishedDate(new Date(
										DateUtility.parseDate((String)getValueFromScript(s.getPublishedDate(), null, null))));
							}
							catch (Exception e) 
							{
								this._context.getHarvestStatus().logMessage("publishedDate: " + e.getMessage(), true);						
								logger.error("publishedDate: " + e.getMessage(), e);
							}
						}
						else
						{
							try 
							{ 
								f.setPublishedDate(new Date(
										DateUtility.parseDate((String)getFormattedTextFromField(s.getPublishedDate()))));
							} 
							catch (Exception e) 
							{
								this._context.getHarvestStatus().logMessage("publishedDate: " + e.getMessage(), true);						
								logger.error("publishedDate: " + e.getMessage(), e);
							}
						}
					}
					
					// Extract URL if applicable
					try {
						if (s.getUrl() != null)
						{
							if (JavaScriptUtils.containsScript(s.getUrl()))
							{
								f.setUrl((String)getValueFromScript(s.getUrl(), null, null));
							}
							else
							{
								f.setUrl(getFormattedTextFromField(s.getUrl()));
							}
						}
					}
					catch (Exception e) 
					{
						this._context.getHarvestStatus().logMessage("URL: " + e.getMessage(), true);						
						logger.error("URL: " + e.getMessage(), e);
					}
					
			// 4. Entity level fields		
					
					// Extract Document GEO if applicable
					
					if (s.getDocumentGeo() != null)
					{
						try
						{
							f.setDocGeo(getDocGeo(s.getDocumentGeo()));
						}
						catch (Exception e)
						{
							this._context.getHarvestStatus().logMessage("docGeo: " + e.getMessage(), true);						
							logger.error("docGeo: " + e.getMessage(), e);
						}
					}

					// Extract Entities
					if (s.getEntities() != null)
					{
						f.setEntities(getEntities(s.getEntities(), f, s));
					}

					// Extract Associations
					if (s.getAssociations() != null)
					{
						f.setAssociations(getAssociations(s.getAssociations(), f));
					}
				} 
				catch (Exception e) 
				{
					this._context.getHarvestStatus().logMessage("Unknown: " + e.getMessage(), true);						
					logger.error("Unknown: " + e.getMessage(), e);
				}
				finally
				{
					document = null;
				}
			}
		}		
		return docs;
	}
	
	
	/**
	 * getEntities(EntitySpecPojo e, DocumentPojo f)
	 * 
	 * @param e
	 * @param f
	 * @return List<EntityPojo>
	 * @throws JSONException 
	 */
	private List<EntityPojo> getEntities(List<EntitySpecPojo> esps, DocumentPojo f, StructuredAnalysisConfigPojo s) throws JSONException
	{
		// If the feed already has entities we want to add the new entities to the list of existing entities
		List<EntityPojo> entities = null;
		if (f.getEntities() != null) 
		{ 
			entities = f.getEntities(); 
		}
		// Otherwise we create a new arraylist to hold the new entities we are adding
		else 
		{ 
			entities = new ArrayList<EntityPojo>();
		}
		
		// Clear geoMap before we start extracting entities and associations for each feed
		if (!geoMap.isEmpty()) geoMap.clear();
	
		// Iterate over each EntitySpecPojo and try to create an entity, or entities, from the data
		JSONObject metadata = null;
		if (document.has("metadata")) {
			metadata = document.getJSONObject("metadata");
		}
		for (EntitySpecPojo esp : esps)
		{
			try {
				List<EntityPojo> tempEntities = getEntities(esp, f, metadata);
				for (EntityPojo e : tempEntities)
				{
					entities.add(e);
				}
			}
			catch (Exception e) {} // (carry on, prob just a missing field in this doc)
		}
		
		return entities;
	}
	
	
	
	/**
	 * getEntities
	 * @param esp
	 * @param f
	 * @return
	 */
	private List<EntityPojo> getEntities(EntitySpecPojo esp, DocumentPojo f, JSONObject currObj)
	{
		List<EntityPojo> entities = new ArrayList<EntityPojo>();
		
		// Does the entity contain a list of entities to iterate over - 
		if (esp.getIterateOver() != null)
		{
			try
			{
				String iterateOver = esp.getIterateOver();

				// Check to see if the arrayRoot specified exists in the current doc before proceeding
				JSONArray entityRecords;
				if ((entityRecords = currObj.getJSONArray(iterateOver)) != null)
				{					
					// Get the type of object contained in EntityRecords[0]
					String objType = entityRecords.get(0).getClass().toString();

					/*
					 *  EntityRecords is a simple String[] array of entities
					 */
					if (objType.equalsIgnoreCase("class java.lang.String"))
					{
						// Iterate over array elements and extract entities
						for (int i = 0; i < entityRecords.length(); ++i) 
						{							
							String field = entityRecords.getString(i);
							EntityPojo entity = getEntity(esp, field, String.valueOf(i), f);
							if (entity != null) entities.add(entity);	
						}
					}

					/*
					 *  EntityRecords is a JSONArray
					 */
					else if (objType.equalsIgnoreCase("class org.json.JSONObject"))
					{
						// Iterate over array elements and extract entities
						for (int i = 0; i < entityRecords.length(); ++i) 
						{
							// Get JSONObject containing entity fields and pass entityElement
							// into the script engine so scripts can access it
							if (engine != null) 
							{
								iterator = entityRecords.getJSONObject(i);
							}

							// Does the entity break out into multiple entities?
							if (esp.getEntities() != null)
							{
								// Iterate over the entities and call getEntities recursively
								for (EntitySpecPojo subEsp : esp.getEntities())
								{	
									List<EntityPojo> subEntities = getEntities(subEsp, f, iterator);
									for (EntityPojo e : subEntities)
									{
										entities.add(e);
									}
								}
							}
							else
							{
								EntityPojo entity = getEntity(esp, null, String.valueOf(i), f);
								if (entity != null) entities.add(entity);	
							}
						}
					}

					if (iterator != currObj) { // (ie at the top level)
						iterator = null;
					}
					
				}//(end if iterateOver)

			}
			catch (Exception e)
			{
				//e.printStackTrace();
				//System.out.println(e.getMessage());
				//logger.error("Exception: " + e.getMessage());
			}
		}
		
		// Single entity
		else
		{
			// Does the entity break out into multiple entities?
			if (esp.getEntities() != null)
			{
				// Iterate over the entities and call getEntities recursively
				for (EntitySpecPojo subEsp : esp.getEntities())
				{	
					List<EntityPojo> subEntities = getEntities(subEsp, f, currObj);
					for (EntityPojo e : subEntities)
					{
						entities.add(e);
					}
				}
			}
			else
			{
				EntityPojo entity = getEntity(esp, null, null, f);
				if (entity != null) entities.add(entity);	
			}
		}
		
		return entities;
	}
	
	
	
	/**
	 * getEntity
	 * @param esp
	 * @param field
	 * @param index
	 * @param f
	 * @return
	 */
	private EntityPojo getEntity(EntitySpecPojo esp, String field, String index, DocumentPojo f)
	{
		// If the EntitySpecPojo or DocumentPojo is null return null
		if ((esp == null) || (f == null)) return null;
		
		try
		{
			Boolean addEntity = true;
			EntityPojo e = new EntityPojo();
			
			// Entity.disambiguous_name
			String disambiguatedName = null;
			if (JavaScriptUtils.containsScript(esp.getDisambiguated_name()))
			{
				disambiguatedName = (String)getValueFromScript(esp.getDisambiguated_name(), field, index);
			}
			else
			{
				// Field - passed in via simple string array from getEntities
				if (field != null)
				{
					disambiguatedName = field + getFormattedTextFromField(esp.getDisambiguated_name());
				}
				else
				{
					disambiguatedName = getFormattedTextFromField(esp.getDisambiguated_name());
				}
			}
			
			// Only proceed if disambiguousName contains a meaningful value
			if (disambiguatedName != null && disambiguatedName.length() > 0)
			{
				e.setDisambiguatedName(disambiguatedName);
			}
			else
			{
				return null;
			}
			
			// Entity.frequency (count)
			String freq = "1";
			if (esp.getFrequency() != null)
			{
				if (JavaScriptUtils.containsScript(esp.getFrequency()))
				{
					freq = getValueFromScript(esp.getFrequency(), field, index).toString();
				}
				else
				{
					freq = getFormattedTextFromField(esp.getFrequency());
				}
				// Since we've specified freq, we're going to enforce it
				if (null == freq) { // failed to get it
					return null;
				}
			}

			// Try converting the freq string value to its numeric (double) representation
			Double frequency = (double) 0;
			try  
			{ 
				frequency = Double.parseDouble(freq); 
			} 
			catch (Exception e1) 
			{
				this._context.getHarvestStatus().logMessage(e1.getMessage(), true);
				return null;
			}
			
			// Only proceed if frequency > 0
			if (frequency > 0)
			{
				e.setFrequency(frequency.longValue()); // Cast to long from double
			}
			else
			{ 
				return null; 	
			}  
			
			// Entity.actual_name
			String actualName = null;
			if (esp.getActual_name() != null)
			{
				if (JavaScriptUtils.containsScript(esp.getActual_name()))
				{
					actualName = (String)getValueFromScript(esp.getActual_name(), field, index);
				}
				else
				{
					actualName = getFormattedTextFromField(esp.getActual_name());
				}
				// Since we've specified actual name, we're going to enforce it (unless otherwise specified)
				if (null == actualName) { // failed to get it
					if (null == esp.getCreationCriteriaScript()) {
						return null;
					}
				}
			}
			// If actualName == null set it equal to disambiguousName
			if (actualName == null) actualName = disambiguatedName;
			e.setActual_name(actualName);
			
			// Entity.type
			String type = null;
			if (esp.getType() != null)
			{
				if (JavaScriptUtils.containsScript(esp.getType()))
				{
					type = (String)getValueFromScript(esp.getType(), field, index);
				}
				else
				{
					type = getFormattedTextFromField(esp.getType());
				}
				// Since we've specified type, we're going to enforce it (unless otherwise specified)
				if (null == type) { // failed to get it
					if (null == esp.getCreationCriteriaScript()) {
						return null;
					}
				}
			}
			else
			{
				type = "Keyword";
			}
			e.setType(type);
			
			// Entity.index
			String entityIndex = disambiguatedName + "/" + type;
			e.setIndex(entityIndex.toLowerCase());
			
			// Entity.dimension
			String dimension = null;
			if (esp.getDimension() != null)
			{
				if (JavaScriptUtils.containsScript(esp.getDimension()))
				{
					dimension = (String)getValueFromScript(esp.getDimension(), field, index);
				}
				else
				{
					dimension = getFormattedTextFromField(esp.getDimension());
				}
				// Since we've specified dimension, we're going to enforce it (unless otherwise specified)
				if (null == dimension) { // failed to get it
					if (null == esp.getCreationCriteriaScript()) {
						return null;
					}
				}
			}
			if (null == dimension) {
				e.setDimension(DimensionUtility.getDimensionByType(type));
			}
			else {
				EntityPojo.Dimension enumDimension = EntityPojo.Dimension.valueOf(dimension);
				if (null == enumDimension) {
					return null; // (invalid dimension)
				}
				else {
					e.setDimension(enumDimension);
				}
			}
			
			// Entity.relevance
			String relevance = "0";
			if (esp.getRelevance() != null)
			{
				if (JavaScriptUtils.containsScript(esp.getRelevance()))
				{
					relevance = (String)getValueFromScript(esp.getRelevance(), field, index);
				}
				else
				{
					relevance = getFormattedTextFromField(esp.getRelevance());
				}
				// Since we've specified relevance, we're going to enforce it (unless otherwise specified)
				if (null == relevance) { // failed to get it
					if (null == esp.getCreationCriteriaScript()) {
						return null;
					}
				}
			}
			try {
				e.setRelevance(Double.parseDouble(relevance));
			}
			catch (Exception e1) {
				this._context.getHarvestStatus().logMessage(e1.getMessage(), true);
				return null;				
			}

			// Extract Entity GEO or set Entity Geo equal to DocGeo if specified via useDocGeo
			if (esp.getGeotag() != null)
			{	
				GeoPojo geo = getEntityGeo(esp.getGeotag(), null);
				if (null != geo) {
					e.setGeotag(geo);
				}
				// (Allow this field to be intrinsically optional)
			}
			else if (esp.getUseDocGeo() == true)
			{
				GeoPojo geo = getEntityGeo(null, f);
				if (null != geo) {
					e.setGeotag(geo);
				}
				// (Allow this field to be intrinsically optional)
			}

			// Add the gazateer_index and geotag to geomap to get used by associations with matching indexes
			if (e.getGeotag() != null)
			{
				geoMap.put(e.getIndex(), e.getGeotag());
			}

			// Entity.ontological_type (
			String ontology_type = null;
			if (esp.getOntology_type() != null)
			{
				if (JavaScriptUtils.containsScript(esp.getOntology_type()))
				{
					ontology_type = (String)getValueFromScript(esp.getOntology_type(), field, index);
				}
				else
				{
					ontology_type = getFormattedTextFromField(esp.getOntology_type());
				}
				// Allow this field to be intrinsically optional
			}
			// If ontological_type == null, go fetch it from the internal lookup
			if (ontology_type == null) {
				e.setOntology_type(GeoOntologyMapping.mapEntityToOntology(type));
			}
			else if ('p' == GeoOntologyMapping.encodeOntologyCode(ontology_type) && !ontology_type.equals("point")) {
				// In this case we don't recognize the ontology type so we'll overwrite it
				e.setOntology_type(GeoOntologyMapping.mapEntityToOntology(type));				
			}
			e.setOntology_type(ontology_type);			
			
			// Parse creation criteria script to determine if the entity should be added
			if (esp.getCreationCriteriaScript() != null && JavaScriptUtils.containsScript(esp.getCreationCriteriaScript()))
			{
				addEntity = validateEntityWithScript(esp.getCreationCriteriaScript(), e);	
			}
			
			// 
			if (addEntity) { return e; } else { return null; }
		}
		catch (Exception ex)
		{
			return null;
		}
	}
	
	
	
	/**
	 * getAssociations
	 * @param esps
	 * @param f
	 * @return
	 * @throws JSONException 
	 */
	private List<AssociationPojo> getAssociations(List<AssociationSpecPojo> esps, DocumentPojo f) throws JSONException
	{
		// If the feed already has associations we want to add the new associations to the list of existing associations
		List<AssociationPojo> associations = null;
		if (f.getAssociations() != null) 
		{ 
			associations = f.getAssociations(); 
		}
		// Otherwise we create a new arraylist to hold the new associations we are adding
		else 
		{ 
			associations = new ArrayList<AssociationPojo>();
		}
	
		// Iterate over each AssociationSpecPojo and try to create an entity, or entities, from the data
		JSONObject metadata = null;
		if (document.has("metadata")) {
			metadata = document.getJSONObject("metadata");
		}
		for (AssociationSpecPojo esp : esps)
		{
			try {
				List<AssociationPojo> tempAssocs = getAssociations(esp, f, metadata);
				if (null != tempAssocs) {
					for (AssociationPojo e : tempAssocs)
					{
						associations.add(e);
					}
				}
			}
			catch (Exception e) {} // (prob just a missing field)
		}		
		return associations;
	}
	
	
	
	/**
	 * getAssociations(List<AssociationSpecPojo> esps, DocumentPojo f)
	 * @param esps
	 * @param f
	 * @return List<AssociationPojo>
	 */
	private List<AssociationPojo> getAssociations(AssociationSpecPojo esp, DocumentPojo f, JSONObject currObj)
	{
		try
		{
			List<AssociationPojo> associations = null;
			if (f.getAssociations() == null) 
			{ 
				associations = new ArrayList<AssociationPojo>(); 
			}
			else 
			{ 
				associations = f.getAssociations(); 
			}

			//
			if (esp.getIterateOver() != null)
			{
				String iterateOver = esp.getIterateOver();

				// START - Multiplicative/Additive Association Creation
				// entity1/entity2/geo_index/time_start/time_end or entity1,entity2,geo_index,time_start,time_end
				if (iterateOver.split("/").length > 1 || iterateOver.split(",").length > 1)
				{
					ArrayList<String[]> assocsToCreate =  new ArrayList<String[]> ();

					// Multiplicative - entity1/entity2/geo_index/time_start/time_end
					if (iterateOver.split("/").length > 1)
					{
						assocsToCreate = getMultiplicativeAssociations(esp, iterateOver, f);	
					}

					// WARNING: This code has not been tested! It should work but...
					// Additive - entity1,entity2,geo_index,time_start,time_end
					else if (iterateOver.split(",").length > 1)
					{
						assocsToCreate = getAdditiveAssociations(esp, iterateOver, f);
					}
					
					// Iterate over each association String[] returned and (try to) create a new AssociationSpecPojo
					if (assocsToCreate != null)
					{
						for (String[] assocToCreate : assocsToCreate)
						{
							AssociationSpecPojo newAssoc = new AssociationSpecPojo();
							// Entity1
							if (assocToCreate[0] !=null) { newAssoc.setEntity1(assocToCreate[0]); }
							else { newAssoc.setEntity1(esp.getEntity1()); }
							
							// Entity2
							if (assocToCreate[1] !=null) { newAssoc.setEntity2(assocToCreate[1]); }
							else { newAssoc.setEntity2(esp.getEntity2()); }
							
							// Geo_index
							if (assocToCreate[2] !=null) { newAssoc.setGeo_index(assocToCreate[2]); }
							else { newAssoc.setGeo_index(esp.getGeo_index()); }
							
							// Time_start
							if (assocToCreate[3] !=null) { newAssoc.setTime_start(assocToCreate[3]); }
							else { newAssoc.setTime_start(esp.getTime_start()); }
							
							// Time_end
							if (assocToCreate[4] !=null) { newAssoc.setTime_end(assocToCreate[4]); }
							else { newAssoc.setTime_end(esp.getTime_end()); }
							
							// Misc. Fields to copy from the original pojo
							newAssoc.setCreationCriteriaScript(esp.getCreationCriteriaScript());
							newAssoc.setVerb(esp.getVerb());
							newAssoc.setVerb_category(esp.getVerb_category());
							newAssoc.setAssoc_type(esp.getAssoc_type());
							newAssoc.setGeotag(esp.getGeotag());

							// Create an association from the AssociationSpecPojo and document
							AssociationPojo association = getAssociation(newAssoc, null, null, f);
							if (association != null) associations.add(association);
						}
					}
				}
				// END - Multiplicative/Additive Association Creation

				//
				else if (null != currObj) // Single field iterateOver
				{
					try
					{
						// Check to see if the arrayRoot specified exists in the current doc before proceeding
						if (currObj.has(iterateOver))
						{
							// Get array of association records from the specified root element
							JSONArray assocRecords = currObj.getJSONArray(iterateOver);

							// Get the type of object contained in assocRecords[0]
							if (assocRecords.length() > 0) {
								String objType = assocRecords.get(0).getClass().toString();
	
								// EntityRecords is a simple String[] array of associations
								if (objType.equalsIgnoreCase("class java.lang.String"))
								{
									// Iterate over array elements and extract associations
									for (int i = 0; i < assocRecords.length(); ++i) 
									{
										String field = assocRecords.getString(i);
										AssociationPojo association = getAssociation(esp, field, Long.valueOf(i), f);
										if (association != null) associations.add(association);	
									}
								}
	
								// EntityRecords is a JSONArray
								else if (objType.equalsIgnoreCase("class org.json.JSONObject"))
								{
									// Iterate over array elements and extract associations
									for (int i = 0; i < assocRecords.length(); ++i) 
									{
										// Get JSONObject containing association fields and pass assocElement
										// into the script engine so scripts can access it
										if (engine != null) 
										{
											iterator = assocRecords.getJSONObject(i);
										}
	
										// Does the association break out into multiple associations?
										if (esp.getAssociations() != null)
										{
											// Iterate over the associations and call getAssociations recursively
											for (AssociationSpecPojo subEsp : esp.getAssociations())
											{	
												List<AssociationPojo> subAssocs = getAssociations(subEsp, f, iterator);
												for (AssociationPojo e : subAssocs)
												{
													associations.add(e);
												}
											}
										}
										else
										{
											AssociationPojo association = getAssociation(esp, null, null, f);
											if (association != null) associations.add(association);	
										}
									}//(else if is json object)
								}//(end if >0 array elements)
							}

							if (iterator != currObj) { // top level
								iterator = null;
							}
						}
					}
					catch (Exception e)
					{
						//System.out.println(e.getMessage());
						logger.error("Exception: " + e.getMessage(), e);
					}
				}
			}

			// 
			else // No iterate over at all
			{
				AssociationPojo association = getAssociation(esp, null, null, f);
				if (association != null) associations.add(association);
			}			

			return associations;
		}
		catch (Exception e)
		{
			logger.error("Exception: " + e.getMessage());
			return null;
		}
	}
	
	
	
	/**
	 * getMultiplicativeAssociations
	 * @param iterateOver
	 * @param f
	 * @return
	 */
	private ArrayList<String[]> getMultiplicativeAssociations(AssociationSpecPojo esp, String iterateOver, DocumentPojo f)
	{
		// Split iterateOver into a list of fields
		String[] entityFields = iterateOver.split("/");
		
		// ArrayList to store association entities in and extract the entities (disambiguous names) from feed.entities
		HashMap<String, ArrayList<String>> entityLists = extractEntityLists(esp, entityFields, f); 
		
		// Calculate the total number of associations to create from the EntitySpecPojo
		Hashtable<String, Integer> assocCounts = getTotalNumberOfAssociations(entityLists, entityFields);
		int totalNumberOfAssociations = (Integer) assocCounts.get("totalNumberOfAssociations");
		
		if (totalNumberOfAssociations > 0)
		{
			ArrayList<String[]> assocsToCreate = new ArrayList<String[]> ();

			int entity1Number = 1;
			int entity2Number = 1;
			int geoIndexNumber = 1;
			int timeStartNumber = 1;
			int timeEndNumber = 1;
			
			for (int i = 0; i < totalNumberOfAssociations; i++)
			{
				try
				{
					//
					String[] assocToCreate = new String[5];

					// Entity1
					if (entityLists.get("entity1") != null && entityLists.get("entity1").get(entity1Number - 1) != null)
					{
						assocToCreate[0] = entityLists.get("entity1").get(entity1Number - 1);
						if (((Integer) assocCounts.get("entity1Count") > 1) && (i % (Integer) assocCounts.get("entity1Repeat") == 0)) entity1Number++;
					}
					
					// Entity2
					if (entityLists.get("entity2") != null && entityLists.get("entity2").get(entity2Number - 1) != null)
					{
						assocToCreate[1] = entityLists.get("entity2").get(entity2Number - 1);
						if (((Integer) assocCounts.get("entity2Count") > 1) && (i % (Integer) assocCounts.get("entity2Repeat") == 0)) entity2Number++;
					}
					
					// Geo_Index
					if (entityLists.get("geo_index") != null && entityLists.get("geo_index").get(geoIndexNumber - 1) != null)
					{
						assocToCreate[2] = entityLists.get("geo_index").get(geoIndexNumber - 1);
						if (((Integer) assocCounts.get("geoIndexCount") > 1) && (i % (Integer) assocCounts.get("geoIndexCount") == 0)) geoIndexNumber++;
					}
					
					// Time_Start
					if (entityLists.get("time_start") != null && entityLists.get("time_start").get(timeStartNumber - 1) != null)
					{
						assocToCreate[3] = entityLists.get("time_start").get(timeStartNumber - 1);
						if (((Integer) assocCounts.get("timeStartCount") > 1) && (i % (Integer) assocCounts.get("timeStartCount") == 0)) timeStartNumber++;
					}
					
					// Time_End
					if (entityLists.get("time_end") != null && entityLists.get("time_end").get(timeEndNumber - 1) != null)
					{
						assocToCreate[4] = entityLists.get("time_end").get(timeEndNumber - 1);
						if (((Integer) assocCounts.get("timeEndCount") > 1) && (i % (Integer) assocCounts.get("timeEndCount") == 0)) timeEndNumber++;
					}

					assocsToCreate.add(assocToCreate);
				}
				catch (Exception e)
				{
					//System.out.println(e.getMessage());
					//logger.error("Exception: " + e.getMessage());
				}
			}
			return assocsToCreate;
		}
		else
		{
			return null;
		}
	}
	
	
	
	/**
	 * extractEntityLists
	 * @param esp
	 * @param entityFields
	 * @param f
	 * @return
	 */
	private HashMap<String, ArrayList<String>> extractEntityLists(AssociationSpecPojo esp, String[] entityFields, DocumentPojo f)
	{
		// ArrayList to store association entities in
		HashMap<String, ArrayList<String>> entityLists = new HashMap<String, ArrayList<String>>(); 
		
		// Get the list of entities from the feed
		List<EntityPojo> entities = f.getEntities();
		
		//
		for (String field : entityFields)
		{
			//
			String typeValue = getFieldValueFromAssociationSpecPojo(esp, field);
			
			// Get the disambiguous_name for any entity that matches the type field
			ArrayList<String> dNames = new ArrayList<String>();
			if (typeValue != null)
			{
				for (EntityPojo e : entities)
				{
					if (e.getType().equalsIgnoreCase(typeValue))
					{
						dNames.add(e.getDisambiguatedName());
					}
				}
				if (dNames.size() > 0) entityLists.put(field, dNames);
			}
		}
		
		return entityLists;
	}
	
	
	
	
	/**
	 * getFieldValueFromAssociationSpecPojo
	 * @param esp
	 * @param field
	 * @return
	 */
	private String getFieldValueFromAssociationSpecPojo(AssociationSpecPojo esp, String field)
	{
		if (field.equalsIgnoreCase("entity1"))
		{
			return esp.getEntity1();
		}
		else if (field.equalsIgnoreCase("entity2"))
		{
			return esp.getEntity2();
		}
		else if (field.equalsIgnoreCase("geo_index"))
		{
			return esp.getGeo_index();
		}
		else if (field.equalsIgnoreCase("time_start"))
		{
			return esp.getTime_start();
		}
		else if (field.equalsIgnoreCase("time_end"))
		{
			return esp.getTime_end();
		}
		else
		{
			return null;
		}
	}
	
	
	
	
	/**
	 * getTotalNumberOfAssociations
	 * @param entityLists
	 * @return
	 */
	private Hashtable<String, Integer> getTotalNumberOfAssociations(HashMap<String, ArrayList<String>> entityLists, String[] entityFields)
	{
		// Create Hashtable to hold count values referenced by name: i.e. totalNumberOfAssociations
		Hashtable<String, Integer> retVal = new Hashtable<String, Integer>();
		
		//
		int entity1_count = 0;
		int entity2_count = 0;
		int geo_index_count = 0;
		int time_start_count = 0;
		int time_end_count = 0;
		
		// Count up the total number of associations that need to be created 
		// Total Number of Associations = entity1 * entity2 * geo_index * time_start * time_end
		// Note: Only calculates based on the fields passed in the entityFields String[] and
		// the number of matching values in entityLists. If one of those values is 0 then the
		// total number of associations = 0
		int totalAssocs = 1;
		for (String field : entityFields)
		{
			if (field.equalsIgnoreCase("entity1")) 
			{
				entity1_count = (entityLists.get("entity1") != null) ? entityLists.get("entity1").size() : 0;
				totalAssocs = totalAssocs * entity1_count;
			}
			if (field.equalsIgnoreCase("entity2"))
			{
				entity2_count = (entityLists.get("entity2") != null) ? entityLists.get("entity2").size() : 0;
				totalAssocs = totalAssocs * entity2_count;
			}
			if (field.equalsIgnoreCase("geo_index"))
			{
				geo_index_count = (entityLists.get("geo_index") != null) ? entityLists.get("geo_index").size() : 0;
				totalAssocs = totalAssocs * geo_index_count;
			}
			if (field.equalsIgnoreCase("time_start"))
			{
				time_start_count = (entityLists.get("time_start") != null) ? entityLists.get("time_start").size() : 0;
				totalAssocs = totalAssocs * time_start_count;
			}
			if (field.equalsIgnoreCase("time_end"))
			{
				time_end_count = (entityLists.get("time_end") != null) ? entityLists.get("time_end").size() : 0;
				totalAssocs = totalAssocs * time_end_count;
			}
		}

		// Add total number of associations to the HashTable and return if the val == 0
		retVal.put("totalNumberOfAssociations", totalAssocs);
		if (totalAssocs == 0) return retVal;

		// Entity1
		int nCount = entity1_count;
		retVal.put("entity1Count", entity1_count);
		Double repeat = (double) (totalAssocs / entity1_count);
		retVal.put("entity1Repeat", repeat.intValue());
		
		// Entity2
		nCount *= entity2_count;
		retVal.put("entity2Count", entity2_count);
		if (nCount != 0) { repeat = (double) (totalAssocs / entity1_count / entity2_count); } 
		else { repeat = (double) 1; }
		retVal.put("entity2Repeat", repeat.intValue());
		
		// Geo_Index
		nCount *= geo_index_count;
		retVal.put("geoIndexCount", time_start_count);
		if (nCount != 0) { repeat = (double) (totalAssocs / entity1_count / entity2_count / geo_index_count); } 
		else { repeat = (double) 1; }
		retVal.put("geoIndexRepeat", repeat.intValue());
		
		// Time_Start
		nCount *= time_start_count;
		retVal.put("timeStartCount", time_start_count);
		if (nCount != 0) { repeat = (double) (totalAssocs / entity1_count / entity2_count / geo_index_count / time_start_count); } 
		else { repeat = (double) 1; }
		retVal.put("timeStartRepeat", repeat.intValue());
		
		// Time_End
		nCount *= time_end_count;
		retVal.put("timeEndCount", time_end_count);
		if (nCount != 0) { repeat = (double) (totalAssocs / entity1_count / entity2_count / geo_index_count / time_start_count / time_end_count); } 
		else { repeat = (double) 1; }
		retVal.put("timeEndRepeat", repeat.intValue());
		
		return retVal;
	}
	
	
	
	
	/**
	 * getAdditiveAssociations
	 * @param iterateOver
	 * @param f
	 * @return
	 */
	private ArrayList<String[]> getAdditiveAssociations(AssociationSpecPojo esp, String iterateOver, DocumentPojo f)
	{
		// Split iterateOver into a list of entities on ','
		String[] entityFields = iterateOver.split(",");
		
		// ArrayList to store association entities in and extract the entities (disambiguous names) from doc.entities
		HashMap<String, ArrayList<String>> entityLists = extractEntityLists(esp, entityFields, f); 
		
		int itemCount = 0;
		if (entityLists.size() > 0)
		{
			itemCount = (entityLists.get(entityFields[0]) != null) ? entityLists.get(entityFields[0]).size() : 0;
			
			// Get an ArrayList<String> from entity1, entity2, geo_index, time_start and time_end fields as appropriate
			ArrayList<String> entity1 = (entityLists.get("entity1") != null) ? entityLists.get("entity1") : null;
			ArrayList<String> entity2 = (entityLists.get("entity2") != null) ? entityLists.get("entity2") : null;
			ArrayList<String> geo_index = (entityLists.get("geo_index") != null) ? entityLists.get("geo_index") : null;
			ArrayList<String> time_start = (entityLists.get("time_start") != null) ? entityLists.get("time_start") : null;
			ArrayList<String> time_end = (entityLists.get("time_end") != null) ? entityLists.get("time_end") : null;
			
			ArrayList<String[]> assocsToCreate = new ArrayList<String[]>();
			for (int i = 0; i < itemCount; i++)
			{
				String[] assocToCreate = new String[5];
				if (entity1 != null && entity1.get(i) != null) assocToCreate[0] = entity1.get(i);
				if (entity2 != null && entity2.get(i) != null) assocToCreate[1] = entity2.get(i);
				if (geo_index != null && geo_index.get(i) != null) assocToCreate[2] = geo_index.get(i);
				if (time_start != null && time_start.get(i) != null) assocToCreate[3] = time_start.get(i);
				if (time_end != null && time_end.get(i) != null) assocToCreate[4] = time_end.get(i);
				
				// Only add assocToCreate to associationsToCreate if each field passed via entityFields has a value 
				boolean addAssocToCreate = true;
				for (String s : entityFields)
				{
					if (s.equalsIgnoreCase("entity1") && assocToCreate[0] == null) { addAssocToCreate = false; break; }
					if (s.equalsIgnoreCase("entity2") && assocToCreate[1] == null) { addAssocToCreate = false; break; }
					if (s.equalsIgnoreCase("geo_index") && assocToCreate[2] == null) { addAssocToCreate = false; break; }
					if (s.equalsIgnoreCase("time_start") && assocToCreate[3] == null) { addAssocToCreate = false; break; }
					if (s.equalsIgnoreCase("time_end") && assocToCreate[4] == null) { addAssocToCreate = false; break; }
				}
				
				if (addAssocToCreate) assocsToCreate.add(assocToCreate);
			}
			return assocsToCreate;
		}
		else
		{
			return null;
		}
	}
	
	

	/**
	 * getAssociation
	 * @param esp
	 * @param field
	 * @param count
	 * @param f
	 * @return
	 */
	private AssociationPojo getAssociation(AssociationSpecPojo esp, String field, Long count, DocumentPojo f) 
	{
		String index = (count != null) ? count.toString() : null;
		try
		{
			Boolean addAssoc = true;
			AssociationPojo e = new AssociationPojo();
			
			boolean bDontResolveToIndices = false; // (can always override to summary)
			if (null != esp.getAssoc_type() && (esp.getAssoc_type().equalsIgnoreCase("summary"))) {
				bDontResolveToIndices = true;
			}

			// Assoc.entity1
			if (esp.getEntity1() != null)
			{
				if (JavaScriptUtils.containsScript(esp.getEntity1()))
				{
					e.setEntity1((String)getValueFromScript(esp.getEntity1(), field, index));
				}
				else
				{
					e.setEntity1(getFormattedTextFromField(esp.getEntity1()));
				}
				if ((null == e.getEntity1()) && (null == esp.getCreationCriteriaScript())) {
					// Specified this, so going to insist on it
					return null;
				}
				
				// Association.entity1_index
				if (esp.getEntity1_index() != null)
				{
					if (JavaScriptUtils.containsScript(esp.getEntity1_index()))
					{
						String s = (String)getValueFromScript(esp.getEntity1_index(), field, index);
						if (null != s) e.setEntity1_index(s.toLowerCase());
					}
					else
					{
						String s = getFormattedTextFromField(esp.getEntity1_index());
						if (null != s) e.setEntity1_index(s.toLowerCase());
					}
					if ((null == e.getEntity1_index()) && (null == esp.getCreationCriteriaScript())) {
						// Specified this, so going to insist on it
						return null;
					}
				}
				else if (!bDontResolveToIndices)
				{
					// Try using the entity.gazateer_index
					for (EntityPojo entity : f.getEntities())
					{
						if (entity.getDisambiguatedName().equalsIgnoreCase(e.getEntity1()))
						{
							e.setEntity1_index(entity.getIndex());
							break;
						}
					}
				}
			}
			
			// Association.entity2
			if (esp.getEntity2() != null)
			{
				if (JavaScriptUtils.containsScript(esp.getEntity2()))
				{
					e.setEntity2((String)getValueFromScript(esp.getEntity2(), field, index));
				}
				else
				{
					e.setEntity2(getFormattedTextFromField(esp.getEntity2()));
				}
				if ((null == e.getEntity2()) && (null == esp.getCreationCriteriaScript())) {
					// Specified this, so going to insist on it
					return null;
				}
				
				// Association.entity2_index
				if (esp.getEntity2_index() != null)
				{
					if (JavaScriptUtils.containsScript(esp.getEntity2_index()))
					{
						String s = (String)getValueFromScript(esp.getEntity2_index(), field, index);
						if (null != s) e.setEntity2_index(s.toLowerCase());
					}
					else
					{
						String s = getFormattedTextFromField(esp.getEntity2_index());
						if (null != s) e.setEntity2_index(s.toLowerCase());
					}
					if ((null == e.getEntity2_index()) && (null == esp.getCreationCriteriaScript())) {
						// Specified this, so going to insist on it
						return null;
					}
				}
				else if (!bDontResolveToIndices)
				{
					// Try using the entity.index
					for (EntityPojo entity : f.getEntities())
					{
						if (entity.getDisambiguatedName().equalsIgnoreCase(e.getEntity2()))
						{
							e.setEntity2_index(entity.getIndex());
							break;
						}
					}
				}
			}
			
			// If both entity1_index and entity2_index are null don't add the association
			if (e.getEntity1_index() == null && e.getEntity2_index() == null) return null;
			
			// Association.verb
			if (esp.getVerb() != null)
			{
				if (JavaScriptUtils.containsScript(esp.getVerb()))
				{
					e.setVerb((String)getValueFromScript(esp.getVerb(), field, index));
				}
				else
				{
					e.setVerb(getFormattedTextFromField(esp.getVerb()));
				}
				if ((null == e.getVerb()) && (null == esp.getCreationCriteriaScript())) {
					// Specified this, so going to insist on it
					return null;
				}
			}
			
			// Association.verb_category
			if (esp.getVerb_category() != null)
			{
				if (JavaScriptUtils.containsScript(esp.getVerb_category()))
				{
					String s = (String)getValueFromScript(esp.getVerb_category(), field, index);
					if (null != s) e.setVerb_category(s.toLowerCase());
				}
				else
				{
					String s = getFormattedTextFromField(esp.getVerb_category());
					if (null != s) e.setVerb_category(s.toLowerCase());
				}
				if ((null == e.getVerb_category()) && (null == esp.getCreationCriteriaScript())) {
					// Specified this, so going to insist on it
					return null;
				}
			}
			
			// Entity.start_time
			if (esp.getTime_start() != null)
			{
				String startTimeString = null;
				if (JavaScriptUtils.containsScript(esp.getTime_start()))
				{
					startTimeString = (String)getValueFromScript(esp.getTime_start(), field, index);
				}
				else
				{
					startTimeString = getFormattedTextFromField(esp.getTime_start());
				}
				if (null != startTimeString) {
					e.setTime_start(DateUtility.getIsoDateString(startTimeString));
				}
				// Allow this to be intrinsically optional
			}
			
			// Entity.end_time
			if (esp.getTime_end() != null)
			{		
				String endTimeString = null;
				if (JavaScriptUtils.containsScript(esp.getTime_end()))
				{
					endTimeString = (String)getValueFromScript(esp.getTime_end(), field, index);
				}
				else
				{
					endTimeString = getFormattedTextFromField(esp.getTime_end());
				}
				if (null != endTimeString) {
					e.setTime_end(DateUtility.getIsoDateString(endTimeString));
				}
				// Allow this to be intrinsically optional
			}
			
			
			// Entity.geo_index
			// Note: esp.getGeo_index() is not the index field but the entity field from which to get the gazateer_index value
			if (esp.getGeo_index() != null)
			{
				String entity1 = null;
				if (JavaScriptUtils.containsScript(esp.getGeo_index()))
				{
					entity1 = (String)getValueFromScript(esp.getGeo_index(), field, index);
				}
				else
				{
					entity1 = getFormattedTextFromField(esp.getGeo_index());
				}

				// Get entity.gazateer_index/geotag
				if (entity1 != null)
				{
					for (EntityPojo entity : f.getEntities())
					{
						if (entity.getDisambiguatedName().equalsIgnoreCase(entity1))
						{
							e.setGeo_index(entity.getIndex());
							e.setGeotag(entity.getGeotag());
						}
					}
				}
				// Allow this to be intrinsically optional
			}
			
			//
			if (e.getGeotag() == null)
			{
				// Extract association geoTag if it exists in the association
				if (esp.getGeotag() != null)
				{	
					e.setGeotag(getEntityGeo(esp.getGeotag(), null));
				}
				// Otherwise search geoMap on gazateer_index (entity1_index, entity2_index) for a geoTag
				else
				{
					if (e.getEntity1_index() != null || e.getEntity2_index() != null)
					{
						if (geoMap.get(e.getEntity1_index()) != null) 
						{
							e.setGeotag(geoMap.get(e.getEntity1_index()));
							e.setGeo_index(e.getEntity1_index());
						}
						else if (geoMap.get(e.getEntity2_index()) != null) 
						{
							e.setGeotag(geoMap.get(e.getEntity2_index()));
							e.setGeo_index(e.getEntity2_index());
						}
					}
				}
				// Allow this to be intrinsically optional
			}


			// Calculate association type
			if (bDontResolveToIndices) {
				e.setAssociation_type("Summary");				
			}
			else {				
				e.setAssociation_type(AssociationUtils.getAssocType(e));
				if (null != esp.getAssoc_type()) {
					if (!e.getAssociation_type().equals("Summary")) {
						// Allowed to switch event<->fact
						if (esp.getAssoc_type().equalsIgnoreCase("fact")) {
							e.setAssociation_type("Fact");											
						}
						else if (esp.getAssoc_type().equalsIgnoreCase("event")) {
							e.setAssociation_type("Event");																		
						}
					}
				}
			}
			
			// If the AssociationSpecPojo has a creation criteria script check the association for validity
			if (esp.getCreationCriteriaScript() != null && JavaScriptUtils.containsScript(esp.getCreationCriteriaScript()))
			{
				addAssoc = validateAssociationWithScript(esp.getCreationCriteriaScript(), e);	
			}
			
			if (addAssoc) { return e; } else { return null; }
		}
		catch (Exception e)
		{
			// This can happen as part of normal logic flow
			//logger.error("Exception: " + e.getMessage());
			return null;
		}
	}
		

	/**
	 * getValueFromScript
	 * @param script
	 * @param value
	 * @param index
	 * @return
	 */
	private Object getValueFromScript(String script, String value, String index) 
	{
		Object retVal = null;
		try
		{
			// Create script object from entity or association JSON
			if (iterator != null)
			{
				engine.put("_iterator", iterator);
	        	engine.eval(JavaScriptUtils.iteratorDocScript);
			}
			
			// Pass value into script as _value so it is accessible
			if (value != null) { engine.put("_value", value); }
			
			//
			if (index != null) { engine.put("_index", iteratorIndex); }
			
			// $SCRIPT - string contains javacript to pass into the engine
			// via .eval and then invoke to get a return value of type Object
			if (script.toLowerCase().startsWith("$script"))
			{
				engine.eval(JavaScriptUtils.getScript(script));
				retVal = inv.invokeFunction(JavaScriptUtils.genericFunctionCall);
			}
			// $FUNC - string contains the name of a function to call (i.e. getSometing(); )
			else if (script.toLowerCase().startsWith("$func"))
			{
				retVal = engine.eval(JavaScriptUtils.getScript(script));
			}
		}
		catch (Exception e)
		{
			//e.printStackTrace();
			_context.getHarvestStatus().logMessage(HarvestExceptionUtils.createExceptionMessage(e).toString(), true);
		}
		return retVal;
	}
	


	/**
	 * getDocGeo(DocGeoSpecPojo d)
	 * Convert the contents of a DocGeoSpecPojo to a GeoJSONPojo, i.e. return
	 * latitude and longitude for a feed
	 * @param d DocGeoSpecPojo
	 * @return GeoJSONPojo
	 */
	private GeoPojo getDocGeo(GeoSpecPojo d)
	{
		GeoPojo docGeo = new GeoPojo();
		String latValue = null;
		String lonValue = null;
		
		try
		{

			// The DocSpecGeoPojo already has lat and lon so we just need to retrieve the values
			if ((d.getLat() != null) && (d.getLon() != null))
			{
				if (JavaScriptUtils.containsScript(d.getLat()))
				{
					latValue = (String)getValueFromScript(d.getLat(), null, null);
				}
				else
				{
					latValue = getStringFromJsonField(d.getLat());
				}

				if (JavaScriptUtils.containsScript(d.getLat()))
				{
					lonValue = (String)getValueFromScript(d.getLon(), null, null);
				}
				else
				{
					lonValue = getStringFromJsonField(d.getLon());
				}
			}

			// Try and retrieve lat and lon using city, state, country values
			else
			{
				String city, region, country, countryCode = null;
				
				// Create a GeoReferencePojo from the DocSpecGeo object
				GeoFeaturePojo g = new GeoFeaturePojo();

				if (d.getCity() != null)
				{
					if (JavaScriptUtils.containsScript(d.getCity()))
					{
						city = (String)getValueFromScript(d.getCity(), null, null);
					}
					else
					{
						city = d.getCity();
					}

					g.setCity(city);
					g.setSearch_field(city);
				}

				if (d.getStateProvince() != null)
				{
					if (JavaScriptUtils.containsScript(d.getStateProvince()))
					{
						region = (String)getValueFromScript(d.getStateProvince(), null, null);
					}
					else
					{
						region = d.getStateProvince();
					}

					g.setRegion(region);
					if (g.getSearch_field() == null) g.setSearch_field(region);
				}

				if (d.getCountry() != null)
				{
					if (JavaScriptUtils.containsScript(d.getCountry()))
					{
						country = (String)getValueFromScript(d.getCountry(), null, null);
					}
					else
					{
						country = d.getCountry();
					}

					g.setCountry(country);
					if (g.getSearch_field() == null) g.setSearch_field(country);
				}

				if (d.getCountryCode() != null)
				{
					if (JavaScriptUtils.containsScript(d.getCountryCode()))
					{
						countryCode = (String)getValueFromScript(d.getCountryCode(), null, null);
					}
					else
					{
						countryCode = d.getCountryCode();
					}

					g.setCountry_code(countryCode);
					if (g.getSearch_field() == null) g.setSearch_field(countryCode);
				}

				// Send the GeoReferencePojo to enrichGeoInfo to attempt to get lat and lon values
				List<GeoFeaturePojo> gList = GeoReference.enrichGeoInfo(g, false, true, 1);
				latValue = gList.get(0).getGeoindex().lat.toString();
				lonValue = gList.get(0).getGeoindex().lon.toString();
			}

			// Set lat and long in DocGeo if possible
			docGeo.lat = Double.parseDouble(latValue);
			docGeo.lon = Double.parseDouble(lonValue);
			
			if (docGeo.lat == 0 && docGeo.lon == 0) docGeo = null; // Don't save 0,0 vals
		}
		catch (Exception e)
		{
			docGeo = null;
		}
		return docGeo;
	}
	
	
	

	/**
	 * getEntityGeo
	 * Get GeoPojo object for entities and associations
	 * @param gsp
	 * @return
	 */
	private GeoPojo getEntityGeo(GeoSpecPojo gsp, DocumentPojo f)
	{
		try
		{
			GeoPojo g = null;
			Double dLat = (double) 0;
			Double dLon = (double) 0;
			
			if (gsp != null)
			{
				String latValue = null;
				String lonValue = null;
				// The GeoSpecPojo already has lat and lon so we just need to retrieve the values
				if ((gsp.getLat() != null) && (gsp.getLon() != null)) {
					if (JavaScriptUtils.containsScript(gsp.getLat()))
					{
						latValue = (String)getValueFromScript(gsp.getLat(), null, null);
					}
					else
					{
						latValue = getFormattedTextFromField(gsp.getLat());
					}
	
					if (JavaScriptUtils.containsScript(gsp.getLon()))
					{
						lonValue = (String)getValueFromScript(gsp.getLon(), null, null);
					}
					else
					{
						lonValue = getFormattedTextFromField(gsp.getLon());
					}
					
					if (latValue != null && lonValue != null)
					{
						dLat = Double.parseDouble(latValue);
						dLon = Double.parseDouble(lonValue);
					}
				}
				
				else
				{
					String city, region, country, countryCode = null;
					
					// Create a GeoReferencePojo from the GeoSpec object
					GeoFeaturePojo gfp = new GeoFeaturePojo();

					if (gsp.getCity() != null)
					{
						if (JavaScriptUtils.containsScript(gsp.getCity()))
						{
							city = (String)getValueFromScript(gsp.getCity(), null, null);
						}
						else
						{
							city = gsp.getCity();
						}

						gfp.setCity(city);
						gfp.setSearch_field(city);
					}

					if (gsp.getStateProvince() != null)
					{
						if (JavaScriptUtils.containsScript(gsp.getStateProvince()))
						{
							region = (String)getValueFromScript(gsp.getStateProvince(), null, null);
						}
						else
						{
							region = gsp.getStateProvince();
						}

						gfp.setRegion(region);
						if (gfp.getSearch_field() == null) gfp.setSearch_field(region);
					}

					if (gsp.getCountry() != null)
					{
						if (JavaScriptUtils.containsScript(gsp.getCountry()))
						{
							country = (String)getValueFromScript(gsp.getCountry(), null, null);
						}
						else
						{
							country = gsp.getCountry();
						}

						gfp.setCountry(country);
						if (gfp.getSearch_field() == null) gfp.setSearch_field(country);
					}

					if (gsp.getCountryCode() != null)
					{
						if (JavaScriptUtils.containsScript(gsp.getCountryCode()))
						{
							countryCode = (String)getValueFromScript(gsp.getCountryCode(), null, null);
						}
						else
						{
							countryCode = gsp.getCountryCode();
						}

						gfp.setCountry_code(countryCode);
						if (gfp.getSearch_field() == null) gfp.setSearch_field(countryCode);
					}

					// Send the GeoReferencePojo to enrichGeoInfo to attempt to get lat and lon values
					List<GeoFeaturePojo> gList = GeoReference.enrichGeoInfo(gfp, false, true, 1);
					latValue = gList.get(0).getGeoindex().lat.toString();
					lonValue = gList.get(0).getGeoindex().lon.toString();
					
					// Set lat and long in DocGeo if possible
					dLat = Double.parseDouble(latValue);
					dLon = Double.parseDouble(lonValue);
				}
			}
			else if (f.getDocGeo() != null)
			{
				dLat = f.getDocGeo().lat;
				dLon = f.getDocGeo().lon;

			}

			if (dLat != 0 && dLon !=0)
			{
				g = new GeoPojo();
				g.lat = dLat;
				g.lon = dLon;
			}

			return g;
		}
		catch (Exception e)
		{
			return null;
		}
	}
	
	
	
	
	/**
	 * validateEntityWithScript
	 * @param script
	 * @param entity
	 * @return
	 */
	private Boolean validateEntityWithScript(String script, EntityPojo entity)
	{
		Boolean retVal = true;
		try
		{
			// Convert entity object to a JSONObject
			GsonBuilder gb = new GsonBuilder();
			Gson g = gb.create();
			JSONObject entityObj = new JSONObject(g.toJson(entity));
			retVal = executeEntityAssociationValidation(script, entityObj);
		}
		catch (Exception e) {}
		return retVal;
	}
	
	
	
	
	/**
	 * validateAssociationWithScript
	 * @param script
	 * @param entity
	 * @return
	 */
	private Boolean validateAssociationWithScript(String script, AssociationPojo association)
	{
		Boolean retVal = true;
		try
		{
			// Convert association object to a JSONObject
			GsonBuilder gb = new GsonBuilder();
			Gson g = gb.create();
			JSONObject assocObj = new JSONObject(g.toJson(association));
			retVal = executeEntityAssociationValidation(script, assocObj);
		}
		catch (Exception e) {}
		return retVal;
	}
	
	
	
	
	/**
	 * executeEntityAssociationValidation
	 * @param s
	 * @param j
	 * @return
	 */
	private Boolean executeEntityAssociationValidation(String s, JSONObject j)
	{
		Boolean retVal = true;
		try
		{
			// Put the entity or association JSON into our engine and parse it into an object
			engine.put("_iterator", j);
			engine.eval(JavaScriptUtils.iteratorDocScript);
			// Run our script that checks whether or not the entity/association should be added
			retVal = (Boolean) getValueFromScript(s, null, null);
		}
		catch (Exception e) 
		{
			_context.getHarvestStatus().logMessage(HarvestExceptionUtils.createExceptionMessage(e).toString(), true);
		}
		return retVal;
	}
	
	
	
	
	/**
	 * getFormattedTextFromField
	 * Accepts a string value that can contain a combination of literal text and
	 * names of fields in the JSON document that need to be retrieved into the 
	 * literal text, i.e.:
	 * 'On $metadata.reportdatetime MPD reported that a $metadata.offense occurred.'
	 * @param v - origString
	 * @return String
	 */
	private String getFormattedTextFromField(String origString)
	{
		// Don't bother running the rest of the code if there are no replacements to make (i.e. does not have $)
		if (!origString.contains("$")) return origString;
		
		StringBuffer sb = new StringBuffer();
		Matcher m = pattern.matcher(origString);
		int ncurrpos = 0;
		
		// Iterate over each match found within the string and concatenate values together:
		// string literal value + JSON field (matched pattern) retrieved
		while (m.find()) 
		{
		   int nnewpos = m.start();
		   sb.append(origString.substring(ncurrpos, nnewpos));
		   ncurrpos = m.end();
		   
		   // Retrieve the field information matched with the RegEx
		   String match = (m.group(1) != null) ? m.group(1): m.group(2);
		   
		   // Retrieve the data from the JSON field and append
		   String sreplace = getStringFromJsonField(match); 
		   if (null == sreplace) {
			   return null;
		   }
		   sb.append( sreplace );
		}
		sb.append(origString.substring(ncurrpos));
		return sb.toString();
	}
	
	
	
	
	/**
	 * getStringFromJsonField
	 * Takes string in the form of: node1.nodeN.fieldName and returns
	 * the value contained in the JSON for that field as an String
	 * Note: supports leading $s in the field name, $s get stripped
	 * out in getValueFromJsonField
	 * @param fieldLocation
	 * @return Object
	 */
	private String getStringFromJsonField(String fieldLocation) 
	{
		try
		{
			return (String)getValueFromJsonField(fieldLocation);
		}
		catch (Exception e)
		{
			return null;
		}
	}
	
	
	
	
	/**
	 * getValueFromJsonField
	 * Takes string in the form of: node1.node2.fieldName and returns
	 * the value contained in the JSON for that field as an Object
	 * Note: supports leading $s in the field name
	 * @param fieldLocation
	 * @return
	 */
	private Object getValueFromJsonField(String fieldLocation) 
	{
		try
		{
			// Strip out $ chars if present and then split on '.' 
			// to get the JSON node hierarchy and field name
			String[] field = fieldLocation.replace("$", "").split("\\.");

			StringBuffer node = new StringBuffer();
			// JSON node = all strings in field[] (except for the last string in the array)
			// concatenated together with the '.' char
			if (field.length > 1)
			{
				for ( int i = 0; i < field.length - 1; i++ ) 
				{
					if (node.length() > 0) node.append(".");
					node.append(field[i]);
				}
			}

			// The field name is the final value in the array
			String fieldName = field[field.length - 1];
			return getValueFromJson(node.toString(), fieldName);
		}
		catch (Exception e)
		{
			// This can happen as part of normal logic flow
			//logger.error("getValueFromJsonField Exception: " + e.getMessage());
			return null;
		}
	}
	
	
	
	
	/**
	 * getValueFromJson(String node, String field)
	 * Attempts to retrieve a value from the node/field and return
	 * and object containing the value to be converted by calling method
	 * @param node
	 * @param field
	 * @return Object o
	 */
	private Object getValueFromJson(String node, String field)
	{
		JSONObject json = (iterator != null) ? iterator : document;		
		Object o = null;
		try
		{
			if (node.length() > 1)
			{
				// (removed the [] case, you'll need to do that with scripts unless you want [0] for every field)
				
				// Mostly standard case $metadata(.object).+field
				if (node.indexOf('.') > -1) {
					String node_fields[] = node.split("\\.");
					JSONObject jo = json; 
					for (String f: node_fields) {
						Object testJo = jo.get(f); 
						if (testJo instanceof JSONArray) {
							jo = ((JSONArray)testJo).getJSONObject(0);
						}
						else {
							jo = (JSONObject)testJo;
						}
					}
					Object testJo = jo.get(field); 
					if (testJo instanceof JSONArray) {
						o = ((JSONArray)testJo).getString(0);
					}
					else {
						o = testJo;
					}
				}
				// Standard case - $metadata.field
				else
				{
					JSONObject jo = json.getJSONObject(node);
					if (jo.get(field) instanceof JSONArray)
					{
						o = jo.getJSONArray(field).getString(0);
					}
					else
					{
						o = jo.getString(field);
					}
				}
			}
			else
			{
				if (json.get(field) instanceof JSONArray)
				{
					o = json.getJSONArray(field).getString(0);
				}
				else
				{
					o = json.getString(field);
				}
			}
		}
		catch (Exception e) 
		{
			// This can happen as part of normal logic flow
			//logger.error("getValueFromJson Exception: " + e.getMessage());
			return null;
		}
		return o;
	}
}

