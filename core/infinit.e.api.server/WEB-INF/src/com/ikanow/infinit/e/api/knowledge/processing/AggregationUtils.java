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
package com.ikanow.infinit.e.api.knowledge.processing;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.elasticsearch.client.action.search.SearchRequestBuilder;
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.search.geo.GeoHashUtils;
import org.elasticsearch.search.facet.Facet;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.datehistogram.DateHistogramFacet;
import org.elasticsearch.search.facet.datehistogram.DateHistogramFacetBuilder;
import org.elasticsearch.search.facet.terms.TermsFacet;
import org.elasticsearch.search.facet.terms.TermsFacetBuilder;

import com.ikanow.infinit.e.api.knowledge.QueryHandler;
import com.ikanow.infinit.e.api.knowledge.aliases.AliasLookupTable;
import com.ikanow.infinit.e.data_model.api.ResponsePojo;
import com.ikanow.infinit.e.data_model.api.knowledge.AdvancedQueryPojo;
import com.ikanow.infinit.e.data_model.api.knowledge.GeoAggregationPojo;
import com.ikanow.infinit.e.data_model.api.knowledge.AdvancedQueryPojo.QueryOutputPojo.AggregationOutputPojo;
import com.ikanow.infinit.e.data_model.store.document.AssociationPojo;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.infinit.e.data_model.store.feature.entity.EntityFeaturePojo;
import com.ikanow.infinit.e.data_model.utils.GeoOntologyMapping;
import com.mongodb.BasicDBObject;

public class AggregationUtils {
	
	// Utilty class:
	
	public static class GeoContainer {
		public Set<GeoAggregationPojo> geotags;
		public long minCount = 0;
		public long maxCount = 0;
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	// OUTPUT PARSING - TOP LEVEL
	
	public static void loadAggregationResults(ResponsePojo rp, Map<String, Facet> facets, AggregationOutputPojo aggOutParams, 
												ScoringUtils scoreStats, AliasLookupTable aliasLookup,
												String[] entityTypeFilterStrings, String[] assocVerbFilterStrings)
	{
		for (Map.Entry<String, Facet> facet: facets.entrySet()) {
			
			// Geo
			
			if (facet.getKey().equals("geo")) {
				TermsFacet geoFacet = (TermsFacet)facet.getValue();
				Set<GeoAggregationPojo> geoCounts = new TreeSet<GeoAggregationPojo>();
				int nHighestCount = -1;
				int nLowestCount = Integer.MAX_VALUE;
				for (TermsFacet.Entry geo: geoFacet.entries()) {										
					String geohash = geo.term().substring(2);
					double[] loc =  GeoHashUtils.decode(geohash);
					GeoAggregationPojo geoObj = new GeoAggregationPojo(loc[0],loc[1]);
					geoObj.count = geo.count();
					geoObj.type = GeoOntologyMapping.decodeOntologyCode(geo.term().charAt(0));
					geoCounts.add(geoObj);
					// (note this aggregates geo points whose decoded lat/logns are the same, which can result in slightly fewer records than requested)
					// (note the aggregation writes the aggregated count into geoObj.count)
					
					if (geoObj.count > nHighestCount) { // (the counts can be modified by the add command above)
						nHighestCount = geo.count();
					}
					if (geoObj.count < nLowestCount) {
						nLowestCount = geo.count();
					}
				}
				rp.setGeo(geoCounts, nHighestCount, nLowestCount);
			}//(TESTED)
			
			if (facet.getKey().equals("time")) {
				DateHistogramFacet timeFacet = (DateHistogramFacet)facet.getValue();
				rp.setTimes(timeFacet.entries(), QueryHandler.getInterval(aggOutParams.timesInterval, 'm'));
			}//(TESTED)
			
			if (facet.getKey().equals("events")) {
				TermsFacet eventsFacet = (TermsFacet)facet.getValue();
				rp.setEvents(parseEventAggregationOutput("Event", eventsFacet, scoreStats, aliasLookup, entityTypeFilterStrings, assocVerbFilterStrings));
			}					
			if (facet.getKey().equals("facts")) {
				TermsFacet factsFacet = (TermsFacet)facet.getValue();
				rp.setFacts(parseEventAggregationOutput("Fact", factsFacet, scoreStats, aliasLookup, entityTypeFilterStrings, assocVerbFilterStrings));
			}					
			//TESTED x2
			
			if (facet.getKey().equals("sourceTags")) {
				TermsFacet tagsFacet = (TermsFacet)facet.getValue();
				rp.setSourceMetaTags(tagsFacet.entries());
			}
			if (facet.getKey().equals("sourceTypes")) {
				TermsFacet typesFacet = (TermsFacet)facet.getValue();
				rp.setSourceMetaTypes(typesFacet.entries());
			}
			if (facet.getKey().equals("sourceKeys")) {
				TermsFacet keysFacet = (TermsFacet)facet.getValue();
				rp.setSources(keysFacet.entries());
			}
			//TESTED x3
		}		
	}//TESTED
	
	////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	// OUTPUT PARSING - UTILS:
	
	public static void parseOutputAggregation(AdvancedQueryPojo.QueryOutputPojo.AggregationOutputPojo aggregation,
			String[] entTypeFilterStrings, String[] assocVerbFilterStrings, SearchRequestBuilder searchSettings, BoolFilterBuilder parentFilterObj)
	{
		// 1.] Go through aggregation list
		
		// 1.1] Apply "simple specifications" if necessary
		
		// Geo
		
		if ((null != aggregation) && (null != aggregation.geoNumReturn) && (aggregation.geoNumReturn > 0)) {
			TermsFacetBuilder fb = FacetBuilders.termsFacet("geo").field(DocumentPojo.locs_).size(aggregation.geoNumReturn);
			// Gross raw handling for facets
			if (null != parentFilterObj) {
				fb = fb.facetFilter(parentFilterObj);
			}
			searchSettings.addFacet(fb);					
		}//(TESTED)
		
		// Temporal
		
		if ((null != aggregation) && (null != aggregation.timesInterval)) {
			if (aggregation.timesInterval.contains("m")) {
				aggregation.timesInterval = "month";
			}
			DateHistogramFacetBuilder fb = FacetBuilders.dateHistogramFacet("time").field(DocumentPojo.publishedDate_).interval(aggregation.timesInterval);
			// Gross raw handling for facets
			if (null != parentFilterObj) {
				fb = fb.facetFilter(parentFilterObj);
			}
			searchSettings.addFacet(fb);					
		}//(TESTED)

		// Entities - due to problems with significance, handled on a document by document basis, see Significance helper class
		
		// Associations (Events/Facts)
		
		// Association verb category filter	
		StringBuilder verbCatRegex = null;
		StringBuilder entTypeRegex = null;
		
		if (((null != aggregation) && (null != aggregation.eventsNumReturn) && (aggregation.eventsNumReturn > 0))
				||
			((null != aggregation) && (null != aggregation.factsNumReturn) && (aggregation.factsNumReturn > 0)))
		{
			if (null != entTypeFilterStrings) {
				boolean bNegative = false;
				if ('-' != entTypeFilterStrings[0].charAt(0)) { // positive filtering
					entTypeRegex = new StringBuilder("(?:");
				}
				else {
					bNegative = true;
					entTypeRegex = new StringBuilder("(?!");
						// (this is a lookahead but will be fine because of the .*/ in front of it)
				}
				for (String entType: entTypeFilterStrings) {
					if (bNegative && ('-' == entType.charAt(0))) {
						entType = entType.substring(1);
					}
					entType = entType.replace("|", "%7C");
					entTypeRegex.append(".*?/").append(Pattern.quote(entType.toLowerCase())).append('|');
						// (can't match greedily because of the 2nd instance of entity type)
				}
				entTypeRegex.setLength(entTypeRegex.length() - 1); // (remove trailing |)
				entTypeRegex.append(")");
				if (bNegative) {
					entTypeRegex.append("[^|]*"); // (now the actual verb, if a -ve lookahead)					
				}
				
			}//TESTED 
			
			if (null != assocVerbFilterStrings) {
				boolean bNegative = false;
				if ('-' != assocVerbFilterStrings[0].charAt(0)) { // positive filtering
					verbCatRegex = new StringBuilder("\\|(?:");
				}
				else {
					bNegative = true;
					verbCatRegex = new StringBuilder("\\|(?!");
						// (this is a lookahead but will be fine because of the "^[^|]*\\" in front of it)
					
					// eg say I have -VERB then subject|VERB|object will match because if the 
				}				
				for (String assocVerbFilterString: assocVerbFilterStrings) {
					if (bNegative && ('-' == assocVerbFilterString.charAt(0))) {						
						assocVerbFilterString = assocVerbFilterString.substring(1);
					}
					assocVerbFilterString = assocVerbFilterString.replace("|", "%7C");
					verbCatRegex.append(Pattern.quote(assocVerbFilterString)).append('|');
				}
				verbCatRegex.setLength(verbCatRegex.length() - 1); // (remove trailing |)
				verbCatRegex.append(")");
				if (bNegative) {
					verbCatRegex.append("[^|]*"); // (now the actual verb, if a -ve lookahead)
				}
			}//TESTED
		}
		//TESTED (all combinations of 1/2 people, 1/2 verbs)			
		
		if ((null != aggregation) && (null != aggregation.eventsNumReturn) && (aggregation.eventsNumReturn > 0)) 
		{			
			StringBuffer regex = new StringBuffer("^Event\\|");
			if (null != entTypeRegex) {
				regex.append(entTypeRegex);
			}
			else {
				regex.append("[^|]*");
			}
			if (null != verbCatRegex) {
				regex.append(verbCatRegex);
			}
			else if (null != entTypeRegex) {
				regex.append("\\|[^|]*");				
			}
			else {
				regex.append(".*");				
			}
			if (null != entTypeRegex) {
				regex.append("\\|").append(entTypeRegex);				
				regex.append(".*");
			}
			else {
				regex.append("\\|.*");
			}
			//DEBUG
			//System.out.println("REGEX==" + regex.toString());			
			//TESTED (all combinations of 1/2 people, 1/2 verbs)			
			
			TermsFacetBuilder fb = FacetBuilders.termsFacet("events").field(AssociationPojo.assoc_index_).size(aggregation.eventsNumReturn).nested(DocumentPojo.associations_);
			fb.regex(regex.toString());
			
			// Gross raw handling for facets
			if (null != parentFilterObj) {
				fb = fb.facetFilter(parentFilterObj);
			}
			searchSettings.addFacet(fb);					
		}
		if ((null != aggregation) && (null != aggregation.factsNumReturn) && (aggregation.factsNumReturn > 0))
		{
			StringBuffer regex = new StringBuffer("^Fact\\|");
			if (null != entTypeRegex) {
				regex.append(entTypeRegex);
			}
			else {
				regex.append("[^|]*");
			}
			if (null != verbCatRegex) {
				regex.append(verbCatRegex);
			}
			else if (null != entTypeRegex) {
				regex.append("\\|[^|]*");				
			}
			else {
				regex.append(".*");				
			}
			if (null != entTypeRegex) {
				regex.append("\\|").append(entTypeRegex);				
				regex.append(".*");
			}
			else {
				regex.append("\\|.*");
			}
			//DEBUG
			//System.out.println("REGEX==" + regex.toString());			
			//TESTED (all combinations of 1/2 people, 1/2 verbs)			
						
			TermsFacetBuilder fb = FacetBuilders.termsFacet("facts").field(AssociationPojo.assoc_index_).size(aggregation.factsNumReturn).nested(DocumentPojo.associations_);
			fb.regex(regex.toString());
			
			// Gross raw handling for facets
			if (null != parentFilterObj) {
				fb = fb.facetFilter(parentFilterObj);
			}
			searchSettings.addFacet(fb);					
		}		
		
		// Source management/monitoring
		
		if ((null != aggregation) && (null != aggregation.sourceMetadata) && (aggregation.sourceMetadata > 0)) {
			TermsFacetBuilder fb = FacetBuilders.termsFacet("sourceTags").field(DocumentPojo.tags_).size(aggregation.sourceMetadata).facetFilter(parentFilterObj);
			TermsFacetBuilder fb1 = FacetBuilders.termsFacet("sourceTypes").field(DocumentPojo.mediaType_).size(aggregation.sourceMetadata).facetFilter(parentFilterObj);
			// Gross raw handling for facets
			if (null != parentFilterObj) {
				fb = fb.facetFilter(parentFilterObj);
				fb1 = fb1.facetFilter(parentFilterObj);
			}
			searchSettings.addFacet(fb);					
			searchSettings.addFacet(fb1);					
		}
		
		if ((null != aggregation) && (null != aggregation.sources) && (aggregation.sources > 0)) {
			TermsFacetBuilder fb = FacetBuilders.termsFacet("sourceKeys").field(DocumentPojo.sourceKey_).size(aggregation.sources);
			// Gross raw handling for facets
			if (null != parentFilterObj) {
				fb = fb.facetFilter(parentFilterObj);
			}
			searchSettings.addFacet(fb);					
		}
		 
	} //TESTED
	
	// 3.1] Utility to parse individual aggregation (facet) element
	
	private static Pattern eventIndexParser = Pattern.compile("([^|]+/[^/|]+)?\\|([^|]+)?\\|([^|]+/[^|/]+)?\\|(.+)?");
	
	private static List<BasicDBObject> parseEventAggregationOutput(String sEventOrFact, TermsFacet facet, 
																	ScoringUtils scoreStats, AliasLookupTable aliasLookup,
																	String[] entityTypeFilterStrings, String[] assocVerbFilterStrings)
	{
		ArrayList<BasicDBObject> facetList = new ArrayList<BasicDBObject>(facet.entries().size());
		
		// (These 2 might be needed if we alias and there are filter strings specified)
		HashSet<String> entTypeFilter = null;		
		//TEST CASES:
//		String term1 = "mark kelly/person|family relation|gabrielle giffords/person|";
//		String term2 = "|family relation|gabrielle giffords/person|";
//		String term3 = "mark kelly/person||gabrielle giffords/person|";
//		String term4 = "mark kelly/person|family relation||";
//		String term5 = "mark kelly/person|family relation|gabrielle giffords/person|loca,tion/city";
//		List<String> terms = Arrays.asList(term1, term2, term3, term4, term5);

		int nFacetEl = 0;
		for (TermsFacet.Entry facetEl: facet.entries()) {
			//DEBUG
			//System.out.println("TERM= " + facetEl.getTerm());
			
			String term = facetEl.getTerm().substring(sEventOrFact.length() + 1); // (step over "Fact|" or "Event|"
			//TEST CASES:
//			if (nFacetEl < terms.size()) {
//				term = terms.get(nFacetEl);
//			}
			
			// Parse the string
			Matcher m = eventIndexParser.matcher(term);
			if (m.matches()) {
				BasicDBObject json = new BasicDBObject();				
				json.put(AssociationPojo.assoc_type_, sEventOrFact);
				String sEnt1_index = m.group(1);
				if (null != sEnt1_index) {
					sEnt1_index = sEnt1_index.replaceAll("%7C", "|");
				}
				String sVerbCat = m.group(2);
				if (null != sVerbCat) json.put(AssociationPojo.verb_category_, sVerbCat.replaceAll("%7C", "|"));
				String sEnt2_index = m.group(3);
				if (null != sEnt2_index) {
					sEnt2_index = sEnt2_index.replaceAll("%7C", "|");
				}
				String sGeoIndex = m.group(4);
				if (null != sGeoIndex) {
					sGeoIndex = sGeoIndex.replaceAll("%7C", "|");
				}
				json.put(AssociationPojo.doccount_, facetEl.getCount());				
				
				// Add significance if possible
				if ((null == scoreStats) || !scoreStats.calcAssocationSignificance(sEnt1_index, sEnt2_index, sGeoIndex, json)) {
					// These fields are optional:
					//json.put("entity1_sig", 0.0);
					//json.put("entity2_sig", 0.0);
					//json.put("geo_sig", 0.0);
					// Mandatory:
					json.put(AssociationPojo.assoc_sig_, 0.0);
				}				
				
				boolean bTransformedByAlias = false; // when true need to re-check vs entity type filter
				
				// Now write the last few values (adjusted for aliases if necessary) into the JSON object
				if (null != sEnt1_index) {
					if (null != aliasLookup) {
						EntityFeaturePojo alias = aliasLookup.doLookupFromIndex(sEnt1_index);
						if (null != alias) {
							sEnt1_index = alias.getIndex();
							if (sEnt1_index.equalsIgnoreCase("discard")) {
								continue;
							}//TESTED
							bTransformedByAlias = true;
						}
					}					
					json.put(AssociationPojo.entity1_index_, sEnt1_index);
				}
				if (null != sEnt2_index) {
					if (null != aliasLookup) {
						EntityFeaturePojo alias = aliasLookup.doLookupFromIndex(sEnt2_index);
						if (null != alias) {
							sEnt2_index = alias.getIndex();
							if (sEnt2_index.equalsIgnoreCase("discard")) {
								continue;
							}//TESTED (cut and paste of ent index1)
							bTransformedByAlias = true;
						}						
					}					
					json.put(AssociationPojo.entity2_index_, sEnt2_index);					
				}
				if (null != sGeoIndex) {
					if (null != aliasLookup) {
						EntityFeaturePojo alias = aliasLookup.doLookupFromIndex(sGeoIndex);
						if (null != alias) {
							sGeoIndex = alias.getIndex();
							if (sGeoIndex.equalsIgnoreCase("discard")) {
								if ((sEnt1_index != null) && (sEnt2_index != null)) {
									sGeoIndex = null; // event/fact is still valid even without the geo									
								}//TESTED
								else continue; // event/fact now meaningless
							}
							bTransformedByAlias = true;
						}						
					}					
					json.put(AssociationPojo.geo_index_, sGeoIndex);										
				}				
				//TESTED
				
				//Whenever aliases are applied, need to re-check whether is this now a filter item
				//ideally have a single code block for doing this in scoringutils_association.
				if (bTransformedByAlias) {
					if ((null == entTypeFilter) && (null != entityTypeFilterStrings)) {
						entTypeFilter = new HashSet<String>();
					}
					// (only create the map once, and only if needed)
					
					boolean bKeep = recheckFiltersAfterTransform(json, aliasLookup, entityTypeFilterStrings, entTypeFilter);
					
					if (!bKeep) {
						continue; // ie just bypass the facetList.add and the nFacetEl
					}
				}//TESTED
				
				facetList.add(json);
			}
			nFacetEl++;
		}
		return facetList;		
	}//TESTED (see cases above - difficult to make this test case standalone because of TermsFacet.Entry)
		
	//////////////////////////////////
	
	// Utility:
	
	private static boolean recheckFiltersAfterTransform(BasicDBObject json, AliasLookupTable aliasLookup,
			String[] entityTypeFilterStrings, HashSet<String> entTypeFilter)
	{
		// (approximate copy paste from ScoringUtils to initialize these objects:)
		boolean bEntTypeFilterPositive = true; // (will recreate this every time since it's so cheap and passsing by ref is such a pain in Java)
		
		if (null != entityTypeFilterStrings) {
			
			if ('-' == entityTypeFilterStrings[0].charAt(0)) {
				bEntTypeFilterPositive = false;
			}
			if (entTypeFilter.isEmpty()) {// (first time through per call only)
				for (String entityType: entityTypeFilterStrings) {
					if (!bEntTypeFilterPositive && ('-' == entityType.charAt(0))) {
						entityType = entityType.substring(1);
					}
					entTypeFilter.add(entityType.toLowerCase());
				}
			}
		}
		// (Only need to re-filter on entities)
		return ScoringUtils_Associations.filterAndAliasAssociation(json, null, false, bEntTypeFilterPositive, true, entTypeFilter, null);		
	}//TESTED
	
}
