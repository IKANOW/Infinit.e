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
package com.ikanow.infinit.e.harvest.enrichment.legacy.opencalais;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ikanow.infinit.e.data_model.interfaces.harvest.EntityExtractorEnum;
import com.ikanow.infinit.e.data_model.interfaces.harvest.IEntityExtractor;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.infinit.e.data_model.store.document.EntityPojo;
import com.ikanow.infinit.e.data_model.store.document.AssociationPojo;
import com.ikanow.infinit.e.data_model.store.document.GeoPojo;
import com.ikanow.infinit.e.data_model.Globals;
import com.ikanow.infinit.e.data_model.Globals.Identity;
import com.ikanow.infinit.e.data_model.InfiniteEnums;
import com.ikanow.infinit.e.data_model.InfiniteEnums.ExtractorDailyLimitExceededException;
import com.ikanow.infinit.e.data_model.InfiniteEnums.ExtractorDocumentLevelException;
import com.ikanow.infinit.e.data_model.utils.DimensionUtility;
import com.ikanow.infinit.e.harvest.utils.PropertiesManager;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;

public class ExtractorOpenCalais implements IEntityExtractor 
{	
	@Override
	public String getName() { return "opencalais"; }
		
	private Map<EntityExtractorEnum, String> _capabilities = new HashMap<EntityExtractorEnum, String>();
	private static final String CALAIS_URL_LEGACY = "http://api.opencalais.com/tag/rs/enrich";
	private static final String CALAIS_URL_CURRENT = "https://api.thomsonreuters.com/permid/calais";
	private String CALAIS_LICENSE = null;
    private HttpClient client;
    private Map<String,EntityPojo> entityNameMap = new HashMap<String, EntityPojo>();
    private Map<String, EventSchemaPojo> eventSchemas;
    private Map<String, String> factOrEvent = new HashMap<String, String>();
    
    
    private static final Logger logger = Logger.getLogger(ExtractorOpenCalais.class);
    private static AtomicLong numInstances = new AtomicLong(0);
    private static ShutdownHook shutdownHook = null;
    private static AtomicLong num_extraction_collisions = new AtomicLong(0);
    private static AtomicLong num_extraction_requests = new AtomicLong(0);

	private static final int MAX_LENGTH = 99000;	
    
    private boolean bAddRawEventsToMetadata = false;
    
	//_______________________________________________________________________
	//_____________________________INITIALIZATION________________
	//_______________________________________________________________________

	public ExtractorOpenCalais()
	{		
		PropertiesManager props = new PropertiesManager();
		CALAIS_LICENSE = props.getExtractorKey("OpenCalais");
		
		client = new HttpClient();
		eventSchemas = loadEventSchemas();
		//insert capabilities of this extractor
		_capabilities.put(EntityExtractorEnum.Name, "OpenCalais");
		_capabilities.put(EntityExtractorEnum.Quality, "1");
		_capabilities.put(EntityExtractorEnum.GeotagExtraction, "true");
		_capabilities.put(EntityExtractorEnum.MaxInputBytes, Integer.toString(MAX_LENGTH));
		
		if (Identity.IDENTITY_SERVICE == Globals.getIdentity()) { // (ie not for API)
			if ( 1 == numInstances.incrementAndGet() ) // (first time only...)
			{
				shutdownHook = new ShutdownHook();
				Runtime.getRuntime().addShutdownHook(shutdownHook);
			}
		}
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
		
		Boolean bWriteMetadata = null;
		String apiKey = null;
		
		if ((null != source) && (null != source.getExtractorOptions())) {
			try {
				String s = source.getExtractorOptions().get("app.opencalais.store_raw_events");
				if (null != s) bWriteMetadata = Boolean.parseBoolean(s);
			}
			catch (Exception e){}
			try {
				apiKey = source.getExtractorOptions().get("app.opencalais.apiKeyOverride");
			}
			catch (Exception e){}
		}		
		
		// DEFAULT CONFIGURATION
		
		PropertiesManager properties = new PropertiesManager();
		
		try {
			if (null == bWriteMetadata) { // (ie not per source)
				bWriteMetadata = properties.getExtractionCapabilityEnabled(getName(), "store_raw_events");			
			}
		}
		catch (Exception e) {}

		// ACTUALLY DO CONFIGURATION
		
		if (null != bWriteMetadata) {
			bAddRawEventsToMetadata = bWriteMetadata;
		}
		if (null != apiKey) {
			this.CALAIS_LICENSE = apiKey;
		}
	}	
	//_______________________________________________________________________
	//_____________________________ENTITY EXTRACTOR FUNCTIONS________________
	//_______________________________________________________________________
	
	/**
	 * Takes a feed with some of the information stored in it
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
		if (null == partialDoc) {
			return;
		}
		configure(partialDoc.getTempSource());
		
		num_extraction_requests.incrementAndGet();
		try 
		{
			if (null == partialDoc.getFullText()) {
				return;
			}
			if (partialDoc.getFullText().length() < 32) { // Else don't waste Extractor call/error logging
				return;
			}	
			
			PostMethod method = createPostMethod(partialDoc.getFullText());
			int responseCode = client.executeMethod(method);
			
			if ( responseCode == HttpStatus.SC_FORBIDDEN) //INF-1101 forbidden gets thrown when too many concurrent requests occur, try 14 more times
			{
				int count = 1;
				while ( count < 15 && responseCode == HttpStatus.SC_FORBIDDEN )
				{
					try {
						Thread.sleep(1800);
					}
					catch (Exception e) {} // carry on...

					responseCode = client.executeMethod(method); //attempt call again
					count++;
				}
				num_extraction_collisions.addAndGet(count);
			}
			
			if ( responseCode == HttpStatus.SC_OK)
			{
				byte[] responseBytes = method.getResponseBody();
				String response = new String(responseBytes, "UTF-8");
				List<EntityPojo> entities = new ArrayList<EntityPojo>();				
				List<AssociationPojo> events = new ArrayList<AssociationPojo>();
				ObjectMapper mapper = new ObjectMapper();
				JsonNode root = mapper.readValue(response, JsonNode.class);
				Iterator<JsonNode> iter = root.getElements();
				Iterator<String> iterNames = root.getFieldNames();
				List<JsonNode> eventNodes = new ArrayList<JsonNode>();
				BasicDBList rawEventObjects = null;
				while ( iter.hasNext() )
				{
					String currNodeName = iterNames.next();
					JsonNode currNode = iter.next();
					if (!currNodeName.equals("doc")) //we can assume these are the entities/topics
					{
						String typeGroup = currNode.get("_typeGroup").getTextValue();
						//check typegroup to see if it is an entity
						if ( typeGroup.equals("entities") )
						{
							try
							{
								EntityPojo ep = new EntityPojo();
								//get what fields we can					
								ep.setType(currNode.get("_type").getTextValue());
								try {
									ep.setDimension(DimensionUtility.getDimensionByType(ep.getType()));
								}
								catch (java.lang.IllegalArgumentException e) {
									ep.setDimension(EntityPojo.Dimension.What);									
								}
								String name = "";
								JsonNode nameNode = null;
								try
								{
									nameNode = currNode.get("name");
									name = nameNode.getTextValue();
								}
								catch (Exception ex )
								{									
									logger.debug("Error parsing name node: " + currNode.toString());
									continue;
								}
								ep.setActual_name(name);
								ep.setRelevance(Double.parseDouble(currNode.get("relevance").getValueAsText()));
								ep.setFrequency((long)currNode.get("instances").size());
								//attempt to get resolutions if they exist
								JsonNode resolutionNode = currNode.get("resolutions");
								if ( null != resolutionNode )
								{
									//resolution nodes are arrays
									JsonNode resolutionFirst = resolutionNode.get(0);
									ep.setSemanticLinks(new ArrayList<String>());
									if ( resolutionFirst.get("id") != null ) 
										ep.getSemanticLinks().add(resolutionFirst.get("id").getTextValue()); //this is a link to an alchemy page
									ep.setDisambiguatedName(resolutionFirst.get("name").getTextValue());
									//check if we need to create a geo object
									if ( null != resolutionFirst.get("latitude") )
									{
										GeoPojo gp = new GeoPojo();
										String lat = resolutionFirst.get("latitude").getValueAsText();
										String lon = resolutionFirst.get("longitude").getValueAsText();
										gp.lat = Double.parseDouble(lat);
										gp.lon = Double.parseDouble(lon);
										ep.setGeotag(gp);
									}	
								}
								else {
									ep.setDisambiguatedName(name); // use actual name)									
								}
								entityNameMap.put(currNodeName.toLowerCase(), ep);
								entities.add(ep);
							}
							catch (Exception ex)
							{
								logger.error("Error creating event pojo from OpenCalaisNode: " + ex.getMessage(), ex);
							}
						}
						else if ( typeGroup.equals("relations") )
						{							
							eventNodes.add(currNode);						
						}
					}					
				}
				//handle events
				if (bAddRawEventsToMetadata) {
					// For now just re-process these into DB objects since we know that works...
					rawEventObjects = new BasicDBList();
				}
				for ( JsonNode eventNode : eventNodes )
				{					
					AssociationPojo event = parseEvent(eventNode);
					//remove useless events (an event is useless if it only has a verb (guessing currently)
					if ( null != event )
					{
						event = removeUselessEvents(event);
						if ( null != event )
						{
							events.add(event);
						}
					}
					if (bAddRawEventsToMetadata) {
						BasicDBObject eventDbo = (BasicDBObject) com.mongodb.util.JSON.parse(eventNode.toString());
						if (null != eventDbo) {
							BasicDBObject transformObj = new BasicDBObject();
							for (Map.Entry<String, Object> entries: eventDbo.entrySet()) {
								if (entries.getValue() instanceof String) {
									String val = (String) entries.getValue();
									EntityPojo transformVal = findMappedEntityName(val);
									if (null != transformVal) {
										transformObj.put(entries.getKey(), transformVal.getIndex());										
										transformObj.put(entries.getKey() + "__hash", val);										
									}
									else {
										transformObj.put(entries.getKey(), val);										
									}
								}
								else {
									transformObj.put(entries.getKey(), entries.getValue());
								}
							}
							
							// (add to another list, which will get written to metadata)
							rawEventObjects.add(transformObj);
						}
					}
				}
				if (bAddRawEventsToMetadata) {
					partialDoc.addToMetadata("OpenCalaisEvents", rawEventObjects.toArray());
				}
				if (null != partialDoc.getEntities()) {
					partialDoc.getEntities().addAll(entities);
					partialDoc.setEntities(partialDoc.getEntities());
				}
				else if (null != entities) {
					partialDoc.setEntities(entities);
				}
				if (null != partialDoc.getAssociations()) {
					partialDoc.getAssociations().addAll(events);
					partialDoc.setAssociations(partialDoc.getAssociations());
				}
				else if (null != events) {
					partialDoc.setAssociations(events);
				}
			}
			else // Error back from OC, presumably the input doc is malformed/too long
			{
				throw new InfiniteEnums.ExtractorDocumentLevelException("OpenCalais HTTP error code: " + Integer.toString(responseCode));
			}
		} 
		catch (Exception e)
		{		
			//DEBUG
			//e.printStackTrace();
			logger.debug("OpenCalais", e);
			//there was an error, so we return null instead
			throw new InfiniteEnums.ExtractorDocumentLevelException(e.getMessage());
		}
	}

	/**
	 * Removes useless events by returning null so they
	 * do not get saved
	 * 
	 * Current strategy, if only a verb exists, remove this event
	 * 
	 * @param event The eventpojo to check if its useless
	 * @return Null if event is useless, otherwise the event
	 */
	private AssociationPojo removeUselessEvents(AssociationPojo event) 
	{
		if ( 	event.getVerb() != null && 
				event.getEntity1() == null &&
				event.getEntity2() == null &&
				event.getTime_start() == null &&
				event.getGeo_index() == null )
			return null;
		return event;
	}

	@Override
	public void extractEntitiesAndText(DocumentPojo partialDoc)
			throws ExtractorDailyLimitExceededException,
			ExtractorDocumentLevelException 
	{
		throw new RuntimeException("You must have a textEngine or text object in front of this featureEngine.");
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
	//_____________________________UTILITY FUNCTIONS_________________________
	//_______________________________________________________________________
	
	private PostMethod createPostMethod(String text) throws UnsupportedEncodingException {
		if ( CALAIS_LICENSE.trim().length() >= 32 ) {
			return createPostMethodCurrent(text);
		} else {
			return createPostMethodLegacy(text);
		}
		
    }
	
	//Documentation States this legacy api will die on August 31, 2015
	private PostMethod createPostMethodLegacy(String text) throws UnsupportedEncodingException {
		if (text.length() > MAX_LENGTH) {
			text = text.substring(0, MAX_LENGTH);
		}		
        PostMethod method = new PostMethod(CALAIS_URL_LEGACY);

        // Set mandatory parameters
        method.setRequestHeader("x-calais-licenseID", CALAIS_LICENSE.trim());
        
        // Set input content type
        method.setRequestHeader("Content-Type", "text/raw; charset=UTF-8");

		// Set response/output format
        method.setRequestHeader("Accept", "application/json");

        method.setRequestHeader("enableMetadataType","GenericRelations");
        // Enable Social Tags processing
        method.setRequestEntity(new StringRequestEntity(text,"text/plain","UTF-8"));
        return method;
	}
	
	private PostMethod createPostMethodCurrent(String text) throws UnsupportedEncodingException {
		if (text.length() > MAX_LENGTH) {
			text = text.substring(0, MAX_LENGTH);
		}		
        PostMethod method = new PostMethod(CALAIS_URL_CURRENT);

        // Set mandatory parameters
        method.setRequestHeader("x-ag-access-token", CALAIS_LICENSE.trim());
        
        // Set input content type
        method.setRequestHeader("Content-Type", "text/raw; charset=UTF-8");

		// Set response/output format
        method.setRequestHeader("outputFormat", "application/json");

        method.setRequestHeader("enableMetadataType","GenericRelations");
        // Enable Social Tags processing
        method.setRequestEntity(new StringRequestEntity(text,"text/plain","UTF-8"));
        return method;
	}
	
	/**
	 * Checks if the entity is in our map and returns
	 * its value if so, otherwise just returns this entity.
	 * 
	 * This is used for when OpenCalais references an entity in the form of
	 * http://s.opencalais.com/hash so we can get back an actual name like Obama
	 * 
	 * @param entity The entity that could potentially be a hash
	 * @return The unhashed entity, just a string name
	 */
	private EntityPojo findMappedEntityName(String entity)
	{
		if ( entityNameMap.containsKey(entity) )
			return entityNameMap.get(entity);
		else
		{
			//Here we create a fake pojo to return so it will just use
			//the text given (could return null and do a check but
			//requires a lot of extra code
			/*EntityPojo fakeEP = new EntityPojo();
			fakeEP.disambiguous_name = entity;
			fakeEP.actual_name = entity;
			return fakeEP;*/
			return null;
		}
	}
	
	/**
	 * Parses the entity type into the correct noun verb noun columns
	 * 
	 * 
	 * @param nodename
	 * @param current_node
	 * @return
	 */
	public AssociationPojo parseEvent(JsonNode current_node)
	{
		AssociationPojo ep = null;
		//handle the different types on entities
		String entity_type = current_node.get("_type").getTextValue().toLowerCase();
		String curr_ent;
		//find eventschema for this type if one exists
		EventSchemaPojo esp = eventSchemas.get(entity_type);
		if ( esp != null )
		{
			ep = new AssociationPojo();
			//entity 1
			for ( String ent1column : getColumnNames(esp.entity1column)) {
				if ( null != ent1column && null != current_node.get(ent1column) ) 
				{
					JsonNode ent1node = current_node.get(ent1column);
					if ( ent1node.isArray() )
					{
						Iterator<JsonNode> entiter = ent1node.getElements();
						curr_ent = entiter.next().getTextValue().toLowerCase();
						EntityPojo matchEnt1 = findMappedEntityName(curr_ent); 
						if ( null != matchEnt1)
						{
							ep.setEntity1(matchEnt1.getActual_name());
							ep.setEntity1_index(createEntityIndex(matchEnt1));
							if ( ep.getGeotag() == null && matchEnt1.getGeotag() != null) //try to set geotag if it already hasn't been
								ep.setGeotag(matchEnt1.getGeotag().deepCopy());
						}
						else
							ep.setEntity1(curr_ent);					
						
						if ( entiter.hasNext())
						{
							curr_ent = entiter.next().getTextValue().toLowerCase();
							EntityPojo matchEnt12 = findMappedEntityName(curr_ent); 
							if ( null != matchEnt12 )
							{
								ep.setEntity2(matchEnt12.getActual_name());
								ep.setEntity2_index(createEntityIndex(matchEnt12));
								if ( ep.getGeotag() == null && matchEnt12.getGeotag() != null) //try to set geotag if it already hasn't been
									ep.setGeotag(matchEnt12.getGeotag().deepCopy());
							}
							else
								ep.setEntity2(curr_ent);						
						}
					}
					else
					{
						curr_ent = current_node.get(ent1column).getTextValue().toLowerCase();
						EntityPojo matchEnt1Only = findMappedEntityName(curr_ent);
						if ( null != matchEnt1Only )
						{
							ep.setEntity1(matchEnt1Only.getActual_name());
							ep.setEntity1_index(createEntityIndex(matchEnt1Only)); 
							if ( ep.getGeotag() == null && matchEnt1Only.getGeotag() != null ) //try to set geotag if it already hasn't been
								ep.setGeotag(matchEnt1Only.getGeotag().deepCopy());
						}
						else
							ep.setEntity1(curr_ent);					
					}
					break;
				}	
			}
			//entity 2	
			for ( String entity2column : getColumnNames(esp.entity2column)) {
				if ( null != entity2column && null != current_node.get(entity2column)  ) 
				{
					JsonNode ent2node = current_node.get(entity2column);
					if ( ent2node.isTextual() )
					{
						curr_ent = current_node.get(entity2column).getTextValue().toLowerCase();
						EntityPojo matchEnt2 = findMappedEntityName(curr_ent);
						if ( null != matchEnt2 )
						{
							ep.setEntity2(matchEnt2.getActual_name());
							ep.setEntity2_index(createEntityIndex(matchEnt2));
							if ( ep.getGeotag() == null && matchEnt2.getGeotag() != null ) //try to set geotag if it already hasn't been
								ep.setGeotag(matchEnt2.getGeotag().deepCopy());
						}
						else
							ep.setEntity2(curr_ent);
					}
					break;
				}
				
			}
			
			//verb and verb category (if there is a verb cat, assign that and then get column value)
			if ( null != esp.verbcategory )
			{
				ep.setVerb_category(esp.verbcategory);
				
				if ( null != esp.verbcolumn && null != current_node.get(esp.verbcolumn) )
				{
					JsonNode verbnode = current_node.get(esp.verbcolumn);
					if ( verbnode.isTextual() )
					{
						ep.setVerb(current_node.get(esp.verbcolumn).getTextValue().toLowerCase());
						EntityPojo verbent = findMappedEntityName(ep.getVerb());
						if ( verbent != null )
							ep.setVerb(verbent.getActual_name());
					}
				}
			}
			else if ( null != esp.verbcolumn && null != current_node.get(esp.verbcolumn) )
			{
				ep.setVerb(current_node.get(esp.verbcolumn).getTextValue().toLowerCase());
			}
			//location
			if ( null != esp.locationcolumn && null != current_node.get(esp.locationcolumn) ) 
			{
				curr_ent = current_node.get(esp.locationcolumn).getTextValue().toLowerCase();
				EntityPojo geoEnt = findMappedEntityName(curr_ent); 
				if ( geoEnt != null && geoEnt.getGeotag() != null )
				{
					ep.setGeo_index(createEntityIndex(geoEnt));				
					ep.setGeotag(geoEnt.getGeotag().deepCopy()); //location always over-rides geotag location
				}
			}			
			//time
			if ( null != esp.timecolumnstart && null != current_node.get(esp.timecolumnstart) ) 
			{
				curr_ent = current_node.get(esp.timecolumnstart).getTextValue().toLowerCase();				
				if ( null != curr_ent )
				{
					ep.setTime_start(standardizeTime(curr_ent));
					//System.out.println(current_node);
					//add some time parsing to get ranges if possible	
					if ( null != esp.timecolumnend && null != current_node.get(esp.timecolumnend) )
					{
						curr_ent = current_node.get(esp.timecolumnend).getTextValue().toLowerCase();
						String[] times = new String[2];
						times[0] = ep.getTime_start();
						times[1] = curr_ent;
						parseEndDate(times);
						ep.setTime_start(times[0]);
						ep.setTime_end(times[1]);
					}
				}
			}
			//remove geotag if it does not have loc
			if ( ep.getGeotag() != null && ep.getGeotag().lon == null)
				ep.setGeotag(null);
			ep.setAssociation_type(getEventType(ep));
		}
		else
		{
			// It's OK just to use the log for this, at some point could consider passing in HarvestContext
			// so could use the per source logger
			logger.info("OpenCalais extractor does not have an event_schema for: " + entity_type);
		}
		return ep;
	}
	
	/**
	 * Returns back a list of strings from the comma delim param columnNames
	 * 
	 * @param columnNames
	 * @return
	 */
	private List<String> getColumnNames(String columnNames) {
		if ( columnNames != null )
			return Arrays.asList(columnNames.split("\\s*,\\s*"));
		return new ArrayList<String>();
	}
	
	/**
	 * Modifies both the time start and time end variables to create time ranges
	 * when possible.
	 * 
	 * Takes a 2 String array [ timestart, timeend] so that it can be passed by
	 * refence and therefore both items  can be modified
	 * 
	 * @param times 2 String array consisting of index 0 = timestart and index 1 = timeend
	 */
	private void parseEndDate(String[] times)
	{
		String time_start = times[0];
		String time_end = times[1];
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");
		int num_time_end = 0;
		try
		{
			num_time_end = Integer.parseInt(time_end);
		}
		catch (Exception ex)
		{
			num_time_end = 0;
		}
		
		try
		{
			if ( num_time_end != 0 && time_end.length() == 4 ) //CASE 1: 2004 (just a year)
			{
				//just a year, span from jan 1 to dec 31
				Calendar cal = Calendar.getInstance();
				cal.set(num_time_end, 0, 1);
				Date datestart = cal.getTime();
				cal.set(num_time_end, 11,31);
				Date dateend = cal.getTime();
				time_start = sdf.format(datestart);
				time_end = sdf.format(dateend);
			}		
			else if ( time_end.substring(0,2).toLowerCase().equals("in") ) //CASE 2: in 2004 (in year) OR in May (in month)
			{
				try
				{
					//pull out year and span from jan1 to dec 31
					num_time_end = Integer.parseInt(time_end.substring(3,7));
					Calendar cal = Calendar.getInstance();
					cal.set(num_time_end, 0, 1);
					Date datestart = cal.getTime();
					cal.set(num_time_end, 11,31);
					Date dateend = cal.getTime();
					time_start = sdf.format(datestart);
					time_end = sdf.format(dateend);
				}
				catch(Exception ex)
				{
					//was not a year, try a month
					String monthString = time_end.substring(3);
					int monthint = parseMonth(monthString);
					if ( monthint > -1 )
					{
						Calendar cal = Calendar.getInstance();
						cal.set(num_time_end, monthint, 1);
						Date datestart = cal.getTime();
						cal.set(num_time_end, monthint,cal.getActualMaximum(Calendar.DATE));
						Date dateend = cal.getTime();
						time_start = sdf.format(datestart);
						time_end = sdf.format(dateend);
					}
					else
					{
						time_end = null;
					}					
				}
			}
			else if ( time_end.substring(0,4).toLowerCase().equals("last") ) //CASE 3: last june
			{
				String monthString = time_end.substring(5);
				int monthint = parseMonth(monthString);
				if ( monthint > -1 )
				{
					Calendar cal = Calendar.getInstance();
					num_time_end = cal.get(Calendar.YEAR)-1;
					cal.set(num_time_end, monthint, 1);
					Date datestart = cal.getTime();
					cal.set(num_time_end, monthint,cal.getActualMaximum(Calendar.DATE));
					Date dateend = cal.getTime();
					time_start = sdf.format(datestart);
					time_end = sdf.format(dateend);
				}
				else
				{
					time_end = null;
				}		
			}
			else if ( time_end.split(" ").length == 2 ) //CASE 4: June 2004 (month and year)
			{
				String[] parts = time_end.split(" ");
				//try to get month
				int monthint = parseMonth(parts[0]);
				if ( monthint > -1 )
				{
					try
					{
						num_time_end = Integer.parseInt(parts[1]);
						Calendar cal = Calendar.getInstance();
						cal.set(num_time_end, monthint, 1);
						Date datestart = cal.getTime();
						cal.set(num_time_end, monthint,cal.getActualMaximum(Calendar.DATE));
						Date dateend = cal.getTime();
						time_start = sdf.format(datestart);
						time_end = sdf.format(dateend);
					}
					catch (Exception ex)
					{
						num_time_end = 0;
					}
				}
				else
				{
					time_end = null;
				}
			}			
			else //didn't fall into one of our cases, we either dont need to parse or dont know how so null out
			{
				time_end = null;
			}
		}
		catch (Exception ex)
		{
			//we had some sort of error, null out the end date, and leave start date whatever open calais extracted
			time_end = null;
		}
		
		
		//System.out.println(time_start + " to " + time_end);
		times[0] = time_start;
		times[1] = time_end;
	}
	
	/**
	 * Returns an integer for the month given from 0(january) to 11(december)
	 * returns -1 if no match is found
	 * 
	 * @param month string full name of month, e.g. January,may,JULY
	 * @return 0-11 for jan-dec or -1 on error
	 */
	private int parseMonth(String month)
	{
		month = month.toLowerCase();
		if ( month.equals("january") )
			return 0;
		else if ( month.equals("february"))
			return 1;
		else if ( month.equals("march"))
			return 2;
		else if ( month.equals("april"))
			return 3;
		else if ( month.equals("may"))
			return 4;
		else if ( month.equals("june"))
			return 5;
		else if ( month.equals("july"))
			return 6;
		else if ( month.equals("august"))
			return 7;
		else if ( month.equals("september"))
			return 8;
		else if ( month.equals("october"))
			return 9;
		else if ( month.equals("november"))
			return 10;
		else if ( month.equals("december"))
			return 11;
		else return -1;
	}
	
	/**
	 * OpenCalais dates are in the format (yyyy-mm-dd)
	 * convert to yyyy-mm-dd?
	 * 
	 * @param date
	 * @return
	 */
	private String standardizeTime(String date)
	{
		//attempt 1 try to convert yyyy-mm-dd
		try
		{
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			Date parsedDate = sdf.parse(date);
			SimpleDateFormat sdfEnd = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
			return sdfEnd.format(parsedDate);
			
		}
		catch (Exception ex)
		{
			//error converting opencalais date
			//logger.info("Could not extract correct dateformat for: " + date);			
		}
		//attempt 2 try to convert yyyy
		try
		{
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
			Date parsedDate = sdf.parse(date);
			SimpleDateFormat sdfEnd = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
			return sdfEnd.format(parsedDate);
			
		}
		catch (Exception ex)
		{
			//error converting opencalais date
			//logger.info("Could not extract correct dateformat for: " + date);
			
		}
		return new StringBuffer("(").append(date).append(")").toString(); //just return what they gave us if all fails
	}
	
	/**
	 * Return the type of event based on following criteria,
	 * event can be either Event, Fact, or Summary
	 * 
	 *  Event: Must contain atleast 2 disambigous entities
	 *  Fact: Generic Relation
	 *  Summary: Anything else
	 * 
	 * @param evt
	 * @return
	 */
	private String getEventType(AssociationPojo evt)
	{
		//count disambig ents
		int disambig_count = 0;
		if ( evt.getEntity1_index() != null ) disambig_count++;
		if ( evt.getEntity2_index() != null ) disambig_count++;
		if ( evt.getGeo_index() != null ) disambig_count++;
		
		String sEventOrFact = factOrEvent.get(evt.getVerb_category());
		if (null == sEventOrFact) { // (defaults to event)
			sEventOrFact = "Event";
		}
		if ( disambig_count > 1 )
			return sEventOrFact;
		else
			return "Summary";
	}
	
	/**
	 * Creates the entity gazateer entry if one exists
	 * for the current entity.  We have to do this because
	 * the entity has not yet been added to the gaz and therefore will not have
	 * one otherwise
	 * 
	 * @param ent
	 * @return
	 */
	private String createEntityIndex(EntityPojo ent)
	{
		if ( ent.getType() != null )
			return new StringBuffer(ent.getDisambiguatedName().toLowerCase()).append('/').append(ent.getType().toLowerCase()).toString();
		else
			return ent.getDisambiguatedName();
	}
	
	/**
	 * Read in xml file and save schema examples
	 * 
	 * @return A list of schemas that we can turn into events from open calais
	 */
	private Map<String,EventSchemaPojo> loadEventSchemas()
	{
		Map<String, EventSchemaPojo> schemas = new HashMap<String,EventSchemaPojo>();		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try
		{
			File file = new File(Globals.getConfigLocation()+"/event_schema.xml");
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(file);
			doc.getDocumentElement().normalize();
			NodeList nodelist = doc.getElementsByTagName("event");
			for ( int i = 0; i < nodelist.getLength(); i++ )
			{
				EventSchemaPojo esp = new EventSchemaPojo();
				Node node = nodelist.item(i);
				NodeList children = node.getChildNodes();
				for ( int j = 0; j < children.getLength(); j++)
				{
					Node child = children.item(j);
					String name = child.getNodeName();
					// (note getNodeValue can be null, so can only be referenced in one of the if blocks below)
					
					if ( name.equals("eventtype"))
						esp.eventtype = child.getChildNodes().item(0).getNodeValue();
					else if ( name.equals("entity1column"))
						esp.entity1column = child.getChildNodes().item(0).getNodeValue();
					else if ( name.equals("verbcolumn"))
						esp.verbcolumn = child.getChildNodes().item(0).getNodeValue();
					else if ( name.equals("verbcategory"))
						esp.verbcategory = child.getChildNodes().item(0).getNodeValue();
					else if ( name.equals("entity2column"))
						esp.entity2column = child.getChildNodes().item(0).getNodeValue();
					else if ( name.equals("locationcolumn"))
						esp.locationcolumn = child.getChildNodes().item(0).getNodeValue();
					else if ( name.equals("timecolumnstart"))
						esp.timecolumnstart = child.getChildNodes().item(0).getNodeValue();
					else if ( name.equals("timecolumnend"))
						esp.timecolumnend = child.getChildNodes().item(0).getNodeValue();
					else if ( name.equals("metatype")) {
						factOrEvent.put(esp.verbcategory, child.getChildNodes().item(0).getNodeValue());
					}						
				}
				schemas.put(esp.eventtype, esp);
			}
		}
		catch (Exception ex)
		{
			logger.error(ex.getMessage());
			ex.printStackTrace();
		}
		return schemas;
	}
	
	class ShutdownHook extends Thread 
	{
	    public void run() 
	    {
	    	if ((null != num_extraction_requests) && (null != num_extraction_collisions)) {
		    	if ((num_extraction_requests.get() > 0) || (num_extraction_collisions.get() > 0)){
			    	StringBuilder sb = new StringBuilder();
			    	sb.append("OpenCalais runtime report: ");
					sb.append("num_of_extraction_requests=" + num_extraction_requests.get());
					sb.append(" num_of_extraction_collisions=" + num_extraction_collisions.get());
					logger.info(sb.toString());
		    	}
	    	}
	    	// (did see a null ptr exception here, not clear how it happens - ie ^^^ for robustness)
	    }
	}
}

