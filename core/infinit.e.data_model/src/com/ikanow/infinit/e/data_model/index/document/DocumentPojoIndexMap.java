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
package com.ikanow.infinit.e.data_model.index.document;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.elasticsearch.index.search.geo.GeoHashUtils;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.ikanow.infinit.e.data_model.index.BaseIndexPojo;
import com.ikanow.infinit.e.data_model.index.BasePojoIndexMap;
import com.ikanow.infinit.e.data_model.index.ElasticSearchPojos;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.infinit.e.data_model.store.document.EntityPojo;
import com.ikanow.infinit.e.data_model.store.document.AssociationPojo;
import com.ikanow.infinit.e.data_model.utils.GeoOntologyMapping;

public class DocumentPojoIndexMap implements BasePojoIndexMap<DocumentPojo> {
	////////////////////////////////////////////////////////////////////////////////

	// Some useful string constants:
	
	public final static String globalDocumentIndex_ = "document_index";
	public final static String globalDocumentIndexCollection_ = "docs_index";
	public final static String manyGeoDocumentIndex_ = "doc_manygeos";
	public final static String manyGeoDocumentIndexCollection_ = "docs_manygeos";
	public final static String dummyDocumentIndex_ = "doc_dummy";
	public final static String documentType_ = "document_index";
	
	////////////////////////////////////////////////////////////////////////////////

	// Due to multi-valued field memory handling within ES, need to put "large" arrays
	// of any non-nested objects into their own index: currently this is just geo
	// (can't nest geo due to geo decay function - anyway, it's a bit painful because
	//  of the ontological type issues)
	
	// This function fills "locs" in and returns true, if the document needs to be treated separately
	
	public static boolean hasManyGeos(DocumentPojo doc) {
		
		// Filter, if applied
		SourcePojo.SourceSearchIndexFilter filter = null;
		if (null != doc.getTempSource()) filter = doc.getTempSource().getSearchIndexFilter();
		
		doc.setLocs(new TreeSet<String>()); // (TreeSet not HashSet to use Comparable)
		
		if (null != doc.getDocGeo()) {
			
			boolean bAddDocGeoToFilter = true; // (support indexing filter)
			if ((null != filter) && (null != filter.fieldList)) {
				if (filter.fieldList.startsWith("-") && filter.fieldList.contains(DocumentPojo.docGeo_)) {
					bAddDocGeoToFilter = false;
				}
				else if (!filter.fieldList.contains(DocumentPojo.docGeo_)) {
					bAddDocGeoToFilter = false;					
				}
			}
			//TESTED: positively in, positively out, and not mentioned
			
			if (bAddDocGeoToFilter) {
				doc.getLocs().add(new StringBuffer("p#").append(GeoHashUtils.encode(doc.getDocGeo().lat, doc.getDocGeo().lon)).toString());
			}
		}

		// Write local geo - from entities

		if (null != doc.getEntities()) for (EntityPojo ent: doc.getEntities()) 
		{
			if (null != ent.getGeotag()) 
			{
				// Apply index filter, if it exists:
				if (null != filter) {
					Pattern whichRegex = null;
					String whichPattern = null;
					if (null != filter.entityGeoFilterRegex) {
						whichRegex = filter.entityGeoFilterRegex;
						whichPattern = filter.entityGeoFilter;
					}
					else {
						whichRegex = filter.entityFilterRegex;
						whichPattern = filter.entityFilter;								
					} // (end which regex to pick)
					if (null != whichRegex) {
						if (whichPattern.startsWith("-")) {
							if (whichRegex.matcher(ent.getIndex()).find()) {
								continue;
							}
						}
						else if (!whichRegex.matcher(ent.getIndex()).find()) {
							continue;
						}					
					} // (end if regex exists)
				}//TESTED (positive and negative geo and normal entities)
								
				if ((null != ent.getGeotag().lat) && (null != ent.getGeotag().lon)) 
				{
					if ((ent.getGeotag().lat >= -90.0) && (ent.getGeotag().lon >= -180.0) && (ent.getGeotag().lat <= 90.0) && (ent.getGeotag().lon <= 180.0)) 
					{
						// (sigh need to check ranges)							
						doc.getLocs().add(new StringBuffer().append(GeoOntologyMapping.encodeOntologyCode(ent.getOntology_type()))
													.append('#').append(GeoHashUtils.encode(ent.getGeotag().lat, ent.getGeotag().lon)).toString());	
								//^^^(convert onto type to character for efficiency (the es "script"/jar-let is going to get called *a lot*)) 							
					}	
					else { // Invalid geotag, need to null out since now search on this in the index
						ent.setGeotag(null);
					}
				}
			}
		}//TESTED

		// Write local geo - from events

		if (null != doc.getAssociations()) for (AssociationPojo evt: doc.getAssociations()) 
		{
			if (null != evt.getGeotag()) 
			{
				// Apply association index filter, if it exists
				if (null != filter) {
					Pattern whichRegex = null;
					String whichPattern = null;
					if (null != filter.assocGeoFilterRegex) {
						whichRegex = filter.assocGeoFilterRegex;
						whichPattern = filter.assocGeoFilter;
					}
					else {
						whichRegex = filter.assocFilterRegex;
						whichPattern = filter.assocFilter;								
					} // (end which regex to pick)
					if (null != whichRegex) {
						if (whichPattern.startsWith("-")) {
							if (whichRegex.matcher(AssociationPojoIndexMap.serialize(evt)).find()) {
								continue;
							}
						}
						else if (!whichRegex.matcher(AssociationPojoIndexMap.serialize(evt)).find()) {
							continue;
						}
						
					} // (end if regex exists)
				}//(end if filter exists)
				//TESTED: only by cut and paste from normal association filtering below
				
				if ( null != evt.getGeotag().lat && null != evt.getGeotag().lon)
				{ // (post sync - or entity didn't have a loc)
					if ((evt.getGeotag().lat >= -90.0) && (evt.getGeotag().lon >= -180.0) && (evt.getGeotag().lat <= 90.0) && (evt.getGeotag().lon <= 180.0)) 
					{
						// (sigh need to check ranges)
						doc.getLocs().add(new StringBuffer("p#").append(GeoHashUtils.encode(evt.getGeotag().lat, evt.getGeotag().lon)).toString());	
					}
					else { // Invalid geotag, need to null out since now search on this in the index
						evt.setGeotag(null);
					}
				}
			}//TESTED
		}
		
		return (doc.getLocs().size() >= 20);
	}
	
	////////////////////////////////////////////////////////////////////////////////
	
	// Transform the pojo ready for insertion into the index (has already been added to DB at this point)

	@Override
	public GsonBuilder extendBuilder(GsonBuilder gp) {
		return new EntityPojoIndexMap().extendBuilder(
				new AssociationPojoIndexMap().extendBuilder(
					gp.registerTypeAdapter(DocumentPojo.class, new DocumentPojoSerializer())));
	}
	protected static class DocumentPojoSerializer implements JsonSerializer<DocumentPojo> 
	{
		// Utility function for serialization
		
		private static void synchronizeWithIndex(DocumentPojo doc) 
		{
			// Filter, if applied
			SourcePojo.SourceSearchIndexFilter filter = null;
			if (null != doc.getTempSource()) filter = doc.getTempSource().getSearchIndexFilter();
			
			doc.setIndex(null); // (this is the index to which we're about to store the feed, so obviously redundant by this point)
			doc.setDisplayUrl(null); // (this is just for the GUI)

			if (null == doc.getLocs()) { // (should always have been filled in by hasManyGeos)
				hasManyGeos(doc);
			}
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

			// If the entity list exists then filter it
			if ((null != filter) && (null != doc.getEntities()) && 
					((null != filter.entityFilter)||(null != filter.entityGeoFilter)))
			{
				Iterator<EntityPojo> it = doc.getEntities().iterator();
				while (it.hasNext()) {
					EntityPojo ent = it.next();
					Pattern whichRegex = null;
					String whichPattern = null;
					if (null != ent.getGeotag() && (null != filter.entityGeoFilterRegex)) {
						whichRegex = filter.entityGeoFilterRegex;
						whichPattern = filter.entityGeoFilter;
					}
					else {
						whichRegex = filter.entityFilterRegex;
						whichPattern = filter.entityFilter;								
					} // (end which regex to pick)
					if (null != whichRegex) {
						if (whichPattern.startsWith("-")) {
							if (whichRegex.matcher(ent.getIndex()).find()) {
								it.remove();
								continue;
							}
						}
						else if (!whichRegex.matcher(ent.getIndex()).find()) {
							it.remove();
							continue;
						}					
					} // (end if regex exists)
				}//TESTED positive and negative geo and normal entities
			} // (end if filter entities out)
			
			if (null != doc.getAssociations()) {
				Iterator<AssociationPojo> it = doc.getAssociations().iterator();
				while (it.hasNext()) {
					AssociationPojo evt = it.next();
					try 
					{
						// Apply association index filter, if it exists
						if (null != filter) {
							Pattern whichRegex = null;
							String whichPattern = null;
							if ((null != evt.getGeotag()) && (null != filter.assocGeoFilterRegex)) {
								whichRegex = filter.assocGeoFilterRegex;
								whichPattern = filter.assocGeoFilter;
							}
							else {
								whichRegex = filter.assocFilterRegex;
								whichPattern = filter.assocFilter;								
							} // (end which regex to pick)
							if (null != whichRegex) {
								if (whichPattern.startsWith("-")) {
									if (whichRegex.matcher(AssociationPojoIndexMap.serialize(evt)).find()) {
										it.remove();
										continue;
									}
								}
								else if (!whichRegex.matcher(AssociationPojoIndexMap.serialize(evt)).find()) {
									it.remove();
									continue;
								}
								
							} // (end if regex exists)
						}
						//TESTED: positive and negative associations, normal only (geo from cut and paste from entities)
						
						// Add event date ranges month by month (eg for histograms)
						if (null != evt.getTime_start()) {
							Date d1 = sdf.parse(evt.getTime_start());
							if (null == doc.getMonths()) {
								doc.setMonths(new TreeSet<Integer>());
							}
	
							if (null == evt.getTime_end()) {
								Calendar c = Calendar.getInstance();
								c.setTime(d1);
								doc.getMonths().add(c.get(Calendar.YEAR)*100 + c.get(Calendar.MONTH)+1);
	
							}
							else {
								Date d2 = sdf.parse(evt.getTime_end());
								Calendar c = Calendar.getInstance();
								c.setTime(d1);
	
								int nStartYr = c.get(Calendar.YEAR);
								int nStartMo = c.get(Calendar.MONTH)+1;
								c.setTime(d2);
								int nEndYr = c.get(Calendar.YEAR);
								int nEndMo = c.get(Calendar.MONTH)+1;
	
								if (nStartYr == nEndYr) {
									for (int i = nStartMo; i <= nEndMo; ++i) {
										doc.getMonths().add(nStartYr*100 + i);
									}
								}
								else if ((nStartYr + 1) == nEndYr) {
									for (int i = nStartMo; i <= 12; ++i) {
										doc.getMonths().add(nStartYr*100 + i);
									}
									for (int i = 1; i <= nEndMo; ++i) {
										doc.getMonths().add(nEndYr*100 + i);
									}							
								}
								// (else too long a period, ignore)
							}
						}
					}//TESTED (single date range, within yr, spanning yrs, > 1yr span) 
					catch (Exception e) {} // Parsing error, carry on... 
				} // (End loop over associations)
			}//TESTED

			// modified: Don't index 
			doc.setModified(null);

			// Write content into non-transient field:
			doc.makeFullTextNonTransient();

			// Remove #N/#NN distribution in source keys
			doc.setSourceKey(doc.getSourceKey());
			
			// If filter specified for metadata then remove unwanted fields
			if ((null != filter) && (null != filter.metadataFieldList) && (null != doc.getMetadata())) {
				String metaFields = filter.metadataFieldList;
				boolean bInclude = true;
				if (metaFields.startsWith("+")) {
					metaFields = metaFields.substring(1);
				}
				else if (metaFields.startsWith("-")) {
					metaFields = metaFields.substring(1);
					bInclude = false;
				}
				String[] metaFieldArray = metaFields.split("\\s*,\\s*");
				if (bInclude) {
					Set<String> metaFieldSet = new HashSet<String>();
					metaFieldSet.addAll(Arrays.asList(metaFieldArray));
					Iterator<Entry<String,  Object[]>> metaField = doc.getMetadata().entrySet().iterator();
					while (metaField.hasNext()) {
						Entry<String,  Object[]> metaFieldIt = metaField.next();
						if (!metaFieldSet.contains(metaFieldIt.getKey())) {
							metaField.remove();
						}
					}
				} 
				else { // exclude case, easier
					for (String metaField: metaFieldArray) {
						doc.getMetadata().remove(metaField);
					}
				} // (end include vs exclude)
				
				//TESTED: (copy/paste from Structured Analysis Handler) by eye: positive, negative, default metadata
			} // (end if filter specified)
			
		}//TESTED (see above clauses): see MongoDocumentTxfer test cases 
		
		////////////////////////////////////////////////////////////////////////////////
		
		// Actual serialization:
		
		@Override
		public JsonElement serialize(DocumentPojo doc, Type typeOfT, JsonSerializationContext context)
		{
			synchronizeWithIndex(doc);
				// (does most of the transformations - the default index pojo does the rest)
			
			// Going to do tags manually to convert them to lower case:
			Set<String> tags = doc.getTags();
			doc.setTags(null);
			
			// GSON transformation:
			JsonElement je = new EntityPojoIndexMap().extendBuilder( 
								new AssociationPojoIndexMap().extendBuilder(
										BaseIndexPojo.getDefaultBuilder())).
								create().toJsonTree(doc, typeOfT);	

			// Write tags back in (as lower case):
			if (null != tags) {
				JsonArray ja = new JsonArray();
				for (String tag: tags) {
					ja.add(new JsonPrimitive(tag.toLowerCase()));
				}
				je.getAsJsonObject().add(DocumentPojo.tags_, ja);
			}//TESTED
			
			// Convert object names in metadata
			if ((null != doc.getMetadata()) && !doc.getMetadata().isEmpty()) {
				if (je.isJsonObject()) {
					JsonElement metadata = je.getAsJsonObject().get("metadata");
					if (null != metadata) {
						enforceTypeNamingPolicy(metadata, 0);
					}
				}
			}
			
			// Filter, if applied
			SourcePojo.SourceSearchIndexFilter filter = null;
			if (null != doc.getTempSource()) filter = doc.getTempSource().getSearchIndexFilter();
			
			// If filter specified for metadata then remove unwanted fields
			if ((null != filter) && (null != filter.fieldList)) {
				String docFields = filter.fieldList;
				boolean bInclude = true;
				if (docFields.startsWith("+")) {
					docFields = docFields.substring(1);
				}
				else if (docFields.startsWith("-")) {
					docFields = docFields.substring(1);
					bInclude = false;
				}
				String[] docFieldArray = docFields.split("\\s*,\\s*");
				if (bInclude) {
					Set<String> docFieldSet = new HashSet<String>();
					docFieldSet.addAll(Arrays.asList(docFieldArray));
					// If entity/association/metadata specified, then leave them in by default
					if ((null != filter.entityFilter) || (null != filter.entityGeoFilter)) {
						docFieldSet.add(DocumentPojo.entities_);
					}
					if ((null != filter.assocFilter) || (null != filter.assocGeoFilter)) {
						docFieldSet.add(DocumentPojo.associations_);
					}
					if (null != filter.metadataFieldList) {
						docFieldSet.add(DocumentPojo.metadata_);						
					}
					Iterator<Entry<String,  JsonElement>> docField = je.getAsJsonObject().entrySet().iterator();
					while (docField.hasNext()) {
						Entry<String,  JsonElement> docFieldIt = docField.next();
						String docFieldEl = docFieldIt.getKey();
						
						if (docFieldEl.equals(DocumentPojo.fullText_) || docFieldEl.equals(DocumentPojo.description_)
								|| docFieldEl.equals(DocumentPojo.tags_) || docFieldEl.equals(DocumentPojo.docGeo_)
								|| docFieldEl.equals(DocumentPojo.metadata_) || docFieldEl.equals(DocumentPojo.entities_)
								|| docFieldEl.equals(DocumentPojo.associations_))								
						{
							if (!docFieldSet.contains(docFieldIt.getKey())) {
								docField.remove();
							}
						} // (if this field is allowed to be delete)
					}
				} 
				else { // exclude case, easier
					for (String docField: docFieldArray) {
						if (docField.equals(DocumentPojo.fullText_) || docField.equals(DocumentPojo.description_)
								|| docField.equals(DocumentPojo.tags_) || docField.equals(DocumentPojo.docGeo_)
								|| docField.equals(DocumentPojo.metadata_) || docField.equals(DocumentPojo.entities_)
								|| docField.equals(DocumentPojo.associations_))								
						{
							if (je.getAsJsonObject().has(docField)) {
								je.getAsJsonObject().remove(docField);
							}
						} // (end if field that is allowed to be deleted)
					}
				} // (end include vs exclude)
				//TESTED: positive and negative filter, can't filter "obligatory fields"
			} // (end if filter specified)
			
			// Convert to record format:
			JsonObject jo = je.getAsJsonObject();
			jo.add("message", jo.get("title"));
			jo.add("type", jo.get("mediaType"));
			jo.add("@timestamp", jo.get("publishedDate"));
			
			return je;
		}		
	}

	//TODO (INF-1566): TOTEST still: tags, MongoDocumentTxfer::rebuildIndex, AssociationPojoIndexMap (print out string plz)
	
	////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////

	// ElasticSearch mapping:	
	
	public static class Mapping {
			
		public static class RootObject 
		{
			ElasticSearchPojos.SourcePojo _source = new ElasticSearchPojos.SourcePojo(true, 
					"_id", "message", "@timestamp", "url", "sourceKey", "sourceUrl", "displayUrl", "docGeo", "type", "tags", "record.*");
	
			ElasticSearchPojos.AllPojo _all = new ElasticSearchPojos.AllPojo(true);
	
			public static class RootProperties 
			{
				ElasticSearchPojos.FieldStringPojo _id = new ElasticSearchPojos.FieldStringPojo("yes", "not_analyzed", null).excludeFromAll();

				// Basic metadata
				ElasticSearchPojos.FieldStringPojo title = new ElasticSearchPojos.FieldStringPojo("yes", "analyzed", null);
				ElasticSearchPojos.FieldStringPojo url = new ElasticSearchPojos.FieldStringPojo("yes", "not_analyzed", null).excludeFromAll();
				// (note this field needs to remain stored in order for sync to work)
	
				// Dates
				ElasticSearchPojos.FieldDatePojo publishedDate = new ElasticSearchPojos.FieldDatePojo("yes", null, null, null).excludeFromAll(); 
				ElasticSearchPojos.FieldDatePojo created = new ElasticSearchPojos.FieldDatePojo("yes", null, null, null).excludeFromAll(); 
	
				// Source management
				ElasticSearchPojos.FieldStringPojo source = new ElasticSearchPojos.FieldStringPojo("yes", "analyzed", null).excludeFromAll();
				ElasticSearchPojos.FieldStringPojo sourceKey = new ElasticSearchPojos.FieldStringPojo("yes", "not_analyzed", null).excludeFromAll();
				ElasticSearchPojos.FieldStringPojo mediaType = new ElasticSearchPojos.FieldStringPojo("yes", "not_analyzed", null).excludeFromAll();
				ElasticSearchPojos.FieldStringPojo tags = new ElasticSearchPojos.FieldStringPojo("no", "not_analyzed", null).excludeFromAll();
				ElasticSearchPojos.FieldStringPojo sourceUrl = new ElasticSearchPojos.FieldStringPojo("yes", "not_analyzed", null).excludeFromAll();
	
				// Social/source
				ElasticSearchPojos.FieldStringPojo communityId = new ElasticSearchPojos.FieldStringPojo("yes", "not_analyzed", null).excludeFromAll();
	
				// Content
				ElasticSearchPojos.FieldStringPojo description = new ElasticSearchPojos.FieldStringPojo("no", "analyzed", null);
				ElasticSearchPojos.FieldStringPojo fullText = new ElasticSearchPojos.FieldStringPojo("no", "analyzed", null).excludeFromAll();
	
				// Enriched content
				EntityPojoIndexMap.Mapping entities = new EntityPojoIndexMap.Mapping();
				ElasticSearchPojos.FieldStringPojo locs = new ElasticSearchPojos.FieldStringPojo("yes", "not_analyzed", null).excludeFromAll(); 
				ElasticSearchPojos.FieldGeoPointPojo docGeo = new ElasticSearchPojos.FieldGeoPointPojo("yes"); 
				ElasticSearchPojos.FieldGeoPointPojo timeRanges = new ElasticSearchPojos.FieldGeoPointPojo("yes"); // (currently unused)
				ElasticSearchPojos.FieldIntegerPojo months = new ElasticSearchPojos.FieldIntegerPojo("yes", null, null).excludeFromAll(); 
				AssociationPojoIndexMap.Mapping associations = new AssociationPojoIndexMap.Mapping();
	
				// User content enrichment
				// No default mapping required, see below for dynamic template mapping
				
				// "Record" metadata
				// _id
				// title<->message
				ElasticSearchPojos.FieldStringPojo message = new ElasticSearchPojos.FieldStringPojo("no", "no", null).excludeFromAll();
				// type<->mediaType
				ElasticSearchPojos.FieldStringPojo type = new ElasticSearchPojos.FieldStringPojo("no", "no", null).excludeFromAll();
				// publishedDate<->@timestamp
				ElasticSearchPojos.FieldDatePojo __AMP__timestamp = new ElasticSearchPojos.FieldDatePojo("yes", null, null, null).excludeFromAll();
				// record: copied fields from metadata, will use dynamic template mapping like metadata
			}
			public RootProperties properties = new RootProperties();
	
			// Default templates for metadata:
			public ElasticSearchPojos.DynamicTemplateList dynamic_templates[] = ElasticSearchPojos.DynamicTemplateList.generateDefaultTemplates(generateRecordDynamicMapping(), generateMetadataDynamicMapping());
			
			private static ElasticSearchPojos.DynamicTemplateList generateRecordDynamicMapping() {
				ElasticSearchPojos.DynamicTemplateList.FieldGenericTemplate.Mapping rawMapping = 
						new ElasticSearchPojos.DynamicTemplateList.FieldGenericTemplate.Mapping("string", "yes", "not_analyzed", null, null, null, null, (Integer)256);
				TreeMap<String, ElasticSearchPojos.DynamicTemplateList.FieldGenericTemplate.Mapping> fields = new TreeMap<String, ElasticSearchPojos.DynamicTemplateList.FieldGenericTemplate.Mapping> ();
				fields.put("raw", rawMapping);
				
				return 	new ElasticSearchPojos.DynamicTemplateList("template_record_mapping" ,
						new ElasticSearchPojos.DynamicTemplateList.FieldGenericTemplate(null, "record.*", "string",
						new ElasticSearchPojos.DynamicTemplateList.FieldGenericTemplate.Mapping("string", "yes", "analyzed", null, null, null, null, null, fields))); 
			}
			private static ElasticSearchPojos.DynamicTemplateList generateMetadataDynamicMapping() {
				ElasticSearchPojos.DynamicTemplateList.FieldGenericTemplate.Mapping rawMapping = 
						new ElasticSearchPojos.DynamicTemplateList.FieldGenericTemplate.Mapping("string", "yes", "not_analyzed", null, null, null, null, (Integer)256);
				TreeMap<String, ElasticSearchPojos.DynamicTemplateList.FieldGenericTemplate.Mapping> fields = new TreeMap<String, ElasticSearchPojos.DynamicTemplateList.FieldGenericTemplate.Mapping> ();
				fields.put("raw", rawMapping);
				
				return 	new ElasticSearchPojos.DynamicTemplateList("template_metadata_mapping" ,
						new ElasticSearchPojos.DynamicTemplateList.FieldGenericTemplate(null, "metadata.*", "string",
						new ElasticSearchPojos.DynamicTemplateList.FieldGenericTemplate.Mapping("string", "yes", "analyzed", null, null, null, null, null, fields))); 
			}
			
			// Turn number/date detection off for metadata:
			public Boolean date_detection = false;
			public Boolean numeric_detection = false;
		}
		public RootObject document_index = new RootObject();
	}

	//////////////////////////////////////////////////////////////////////////////////////////
	
	// Utility function for parsing native GSON to rename object fieldnames by appending "__obj"
	// and encoding "."s and "%"s (needed for the DB, duplicated here sigh)
	
	private static boolean enforceTypeNamingPolicy(JsonElement je, int nDepth) {
		
		if (je.isJsonPrimitive()) {
			return false; // Done
		}
		else if (je.isJsonArray()) {
			JsonArray ja = je.getAsJsonArray();
			if (0 == ja.size()) {
				return false; // No idea, carry on
			}
			JsonElement jaje = ja.get(0);
			return enforceTypeNamingPolicy(jaje, nDepth + 1); // keep going until you find primitive/object
		}
		else if (je.isJsonObject()) {
			JsonObject jo = je.getAsJsonObject();
			// Nested variables:
			Iterator<Entry<String, JsonElement>> it = jo.entrySet().iterator();
			StringBuffer newName = null;
			Map<String, JsonElement> toFixList = null;
			while (it.hasNext()) {
				boolean bFix = false;
				Entry<String, JsonElement> el = it.next();
				String currKey = el.getKey();
				
				if ((currKey.indexOf('.') >= 0) || (currKey.indexOf('%') >= 0)) {
					it.remove();
					currKey = currKey.replace("%", "%25").replace(".", "%2e");
					bFix = true;
				}				
				if (null == el.getValue()) {
					if (!bFix) it.remove(); // nice easy case, just get rid of it (if bFix, it's already removed)
					bFix = false;
				}
				else if (enforceTypeNamingPolicy(el.getValue(), nDepth + 1)) { // rename!
					if (currKey.indexOf("__") < 0) { // unless it's an es type
						if (!bFix) it.remove();  // (if bFix, it's already removed)
						if (null == newName) {
							newName = new StringBuffer();
						}
						else {
							newName.setLength(0);
						}
						currKey = newName.append(currKey).append("__obj").toString();
						bFix = true;
					}	
				} // (end check if need to rename)
				if (bFix) {
					if (null == toFixList) {
						toFixList = new HashMap<String, JsonElement>();
					}
					toFixList.put(currKey, el.getValue());					
				}
			} // (end loop over params)	
			if (null != toFixList) {
				for (Entry<String, JsonElement> el: toFixList.entrySet()) {
					jo.add(el.getKey(), el.getValue());
				}
			}
			return true; // (in any case, I get renamed by calling parent)
		}
		return false;
	}
	//TESTED (see DOC_META in test/TestCode)
	
}//TESTED (by hand/eye, see MongoDocumentTxfer)
