package com.ikanow.infinit.e.data_model.index.document;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;

import org.elasticsearch.index.search.geo.GeoHashUtils;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.ikanow.infinit.e.data_model.index.BaseIndexPojo;
import com.ikanow.infinit.e.data_model.index.BasePojoIndexMap;
import com.ikanow.infinit.e.data_model.index.ElasticSearchPojos;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.infinit.e.data_model.store.document.EntityPojo;
import com.ikanow.infinit.e.data_model.store.document.AssociationPojo;
import com.ikanow.infinit.e.data_model.utils.GeoOntologyMapping;

public class DocumentPojoIndexMap implements BasePojoIndexMap<DocumentPojo> {
	////////////////////////////////////////////////////////////////////////////////

	// Some useful string constants:
	
	public final static String globalDocumentIndex_ = "document_index";
	public final static String documentType_ = "document_index";
	public final static String manyGeoDocumentIndex_ = "doc_manygeos";
	public final static String dummyDocumentIndex_ = "doc_dummy";
	
	////////////////////////////////////////////////////////////////////////////////

	// Due to multi-valued field memory handling within ES, need to put "large" arrays
	// of any non-nested objects into their own index: currently this is just geo
	// (can't nest geo due to geo decay function - anyway, it's a bit painful because
	//  of the ontological type issues)
	
	// This function fills "locs" in and returns true, if the document needs to be treated separately
	
	public static boolean hasManyGeos(DocumentPojo doc) {
		
		doc.setLocs(new TreeSet<String>()); // (TreeSet not HashSet to use Comparable)
		
		if (null != doc.getDocGeo()) {
			doc.getLocs().add(new StringBuffer("p#").append(GeoHashUtils.encode(doc.getDocGeo().lat, doc.getDocGeo().lon)).toString());	
		}

		// Write local geo - from entities

		if (null != doc.getEntities()) for (EntityPojo ent: doc.getEntities()) 
		{
			if (null != ent.getGeotag()) 
			{
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
		@Override
		public JsonElement serialize(DocumentPojo doc, Type typeOfT, JsonSerializationContext context)
		{
			synchronizeWithIndex(doc);
				// (does most of the transformations - the default index pojo does the rest)
			
			// GSON transformation:
			JsonElement jo = new EntityPojoIndexMap().extendBuilder( 
								new AssociationPojoIndexMap().extendBuilder(
										BaseIndexPojo.getDefaultBuilder())).
								create().toJsonTree(doc, typeOfT);	

			// Convert object names in metadata
			if ((null != doc.getMetadata()) && !doc.getMetadata().isEmpty()) {
				if (jo.isJsonObject()) {
					JsonElement metadata = jo.getAsJsonObject().get("metadata");
					if (null != metadata) {
						enforceTypeNamingPolicy(metadata, 0);
					}
				}
			}
			
			return jo;
		}		
		// Utility function for serialization
		
		private static void synchronizeWithIndex(DocumentPojo doc) 
		{
			doc.setIndex(null); // (this is the index to which we're about to store the feed, so obviously redundant by this point)

			if (null == doc.getLocs()) { // (should always have been filled in by hasMayGeos)
				hasManyGeos(doc);
			}
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

			if (null != doc.getAssociations()) for (AssociationPojo evt: doc.getAssociations()) 
			{
				try 
				{
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

			}//TESTED

			// modified: Don't index 
			doc.setModified(null);

			// Write content into non-transient field:
			doc.makeFullTextNonTransient();

			// Multi-community docs have source keys represented by <key>#<community>
			// Store just the key in the index
			int nMultiCommunityDoc = doc.getSourceKey().lastIndexOf('#');
			if (nMultiCommunityDoc >= 0) {
				doc.setSourceKey(doc.getSourceKey().substring(0, nMultiCommunityDoc));
			}
			
		}//TESTED (see above clauses): see MongoDocumentTxfer test cases 
		
	}

	////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////

	// ElasticSearch mapping:	
	
	public static class Mapping {
			
		public static class RootObject 
		{
			ElasticSearchPojos.SourcePojo _source = new ElasticSearchPojos.SourcePojo(false, null, null);
	
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
			}
			public RootProperties properties = new RootProperties();
	
			// Default templates for metadata:
			public ElasticSearchPojos.DynamicTemplateList dynamic_templates[] = ElasticSearchPojos.DynamicTemplateList.generateDefaultTemplates(); 
		}
		public RootObject document_index = new RootObject();
	}

	//////////////////////////////////////////////////////////////////////////////////////////
	
	// Utility function for parsing native GSON to rename object fieldnames by appending "__obj"
	
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
				Entry<String, JsonElement> el = it.next();
				if (null == el.getValue()) {
					it.remove(); // nice easy case, just get rid of it
				}
				else if (enforceTypeNamingPolicy(el.getValue(), nDepth + 1)) { // rename!
					if (el.getKey().indexOf("__") < 0) { // unless it's an es type
						it.remove();
						if (null == newName) {
							newName = new StringBuffer();
						}
						else {
							newName.setLength(0);
						}
						if (null == toFixList) {
							toFixList = new HashMap<String, JsonElement>();
						}
						toFixList.put(newName.append(el.getKey()).append("__obj").toString(), el.getValue());
					}
				}
			}		
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
