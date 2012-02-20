package com.ikanow.infinit.e.api.knowledge.processing;

import java.util.ArrayList;
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
import com.ikanow.infinit.e.data_model.api.ResponsePojo;
import com.ikanow.infinit.e.data_model.api.knowledge.AdvancedQueryPojo;
import com.ikanow.infinit.e.data_model.api.knowledge.GeoAggregationPojo;
import com.ikanow.infinit.e.data_model.api.knowledge.AdvancedQueryPojo.QueryOutputPojo.AggregationOutputPojo;
import com.ikanow.infinit.e.data_model.store.document.AssociationPojo;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.infinit.e.data_model.utils.GeoOntologyMapping;
import com.mongodb.BasicDBObject;

public class AggregationUtils {
	
	////////////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	// OUTPUT PARSING - TOP LEVEL
	
	public static void loadAggregationResults(ResponsePojo rp, Map<String, Facet> facets, AggregationOutputPojo aggOutParams, ScoringUtils scoreStats)
	{
		for (Map.Entry<String, Facet> facet: facets.entrySet()) {
			
			// Geo
			
			if (facet.getKey().equals("geo")) {
				TermsFacet geoFacet = (TermsFacet)facet.getValue();
				Set<GeoAggregationPojo> geoCounts = new TreeSet<GeoAggregationPojo>();
				int nHighestCount = -1;
				int nLowestCount = Integer.MAX_VALUE;
				for (TermsFacet.Entry geo: geoFacet.entries()) {
					if (nHighestCount < 0) { // First time
						nHighestCount = geo.count();
					}
					if (geo.count() < nLowestCount) {
						nLowestCount = geo.count();
					}
										
					String geohash = geo.term().substring(2);
					double[] loc =  GeoHashUtils.decode(geohash);
					GeoAggregationPojo geoObj = new GeoAggregationPojo(loc[0],loc[1]);
					geoObj.count = geo.count();
					geoObj.type = GeoOntologyMapping.decodeOntologyCode(geo.term().charAt(0));
					geoCounts.add(geoObj); 
					// (There's a failure case here because geohashes can map to the same lat/long - at least they'll be colocated so the GUI can sort it out)
				}
				rp.setGeo(geoCounts, nHighestCount, nLowestCount);
			}//(TESTED)
			
			if (facet.getKey().equals("time")) {
				DateHistogramFacet timeFacet = (DateHistogramFacet)facet.getValue();
				rp.setTimes(timeFacet.entries(), QueryHandler.getInterval(aggOutParams.timesInterval, 'm'));
			}//(TESTED)
			
			if (facet.getKey().equals("events")) {
				TermsFacet eventsFacet = (TermsFacet)facet.getValue();
				rp.setEvents(parseEventAggregationOutput("Event", eventsFacet, scoreStats));
			}					
			if (facet.getKey().equals("facts")) {
				TermsFacet factsFacet = (TermsFacet)facet.getValue();
				rp.setFacts(parseEventAggregationOutput("Fact", factsFacet, scoreStats));
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
			SearchRequestBuilder searchSettings, BoolFilterBuilder parentFilterObj)
	{
		// 1.] Go through aggregation list
		
		// 1.1] Apply "simple specifications" if necessary
		
		 //TODO (INF-1230): return a set of facet name vs aggregation term?
		
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
		
		// Events
		
		if ((null != aggregation) && (null != aggregation.eventsNumReturn) && (aggregation.eventsNumReturn > 0)) 
		{
			
			TermsFacetBuilder fb = FacetBuilders.termsFacet("events").field(AssociationPojo.assoc_index_).size(aggregation.eventsNumReturn).nested(DocumentPojo.associations_);
			fb.regex("^Event\\|.*");		
			// Gross raw handling for facets
			if (null != parentFilterObj) {
				fb = fb.facetFilter(parentFilterObj);
			}
			searchSettings.addFacet(fb);					
		}
		if ((null != aggregation) && (null != aggregation.factsNumReturn) && (aggregation.factsNumReturn > 0)) {
			TermsFacetBuilder fb = FacetBuilders.termsFacet("facts").field(AssociationPojo.assoc_index_).size(aggregation.factsNumReturn).nested(DocumentPojo.associations_);
			fb.regex("^Fact\\|.*");		
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
	
	private static List<BasicDBObject> parseEventAggregationOutput(String sEventOrFact, TermsFacet facet, ScoringUtils scoreStats) {
		ArrayList<BasicDBObject> facetList = new ArrayList<BasicDBObject>(facet.entries().size());
		
		//TEST CASES:
//		String term1 = "mark kelly/person|family relation|gabrielle giffords/person|";
//		String term2 = "|family relation|gabrielle giffords/person|";
//		String term3 = "mark kelly/person||gabrielle giffords/person|";
//		String term4 = "mark kelly/person|family relation||";
//		String term5 = "mark kelly/person|family relation|gabrielle giffords/person|loca,tion/city";
//		List<String> terms = Arrays.asList(term1, term2, term3, term4, term5);

		int nFacetEl = 0;
		for (TermsFacet.Entry facetEl: facet.entries()) {
			String term = facetEl.getTerm().substring(sEventOrFact.length() + 1); // (step over "Fact|" or "Event|"
			//TEST CASES:
//			if (nFacetEl < terms.size()) {
//				term = terms.get(nFacetEl);
//			}
			
			// Parse the string
			Matcher m = eventIndexParser.matcher(term);
			if (m.matches()) {
				BasicDBObject json = new BasicDBObject();				
				json.put("assoc_type", sEventOrFact);
				String sEnt1_index = m.group(1);
				if (null != sEnt1_index) json.put("entity1_index", sEnt1_index.replaceAll("%7C", "|"));
				String sVerbCat = m.group(2);
				if (null != sVerbCat) json.put("verb_category", sVerbCat.replaceAll("%7C", "|"));
				String sEnt2_index = m.group(3);
				if (null != sEnt2_index) json.put("entity2_index", sEnt2_index.replaceAll("%7C", "|"));
				String sGeoIndex = m.group(4);
				if (null != sGeoIndex) json.put("geo_index", sGeoIndex.replaceAll("%7C", "|"));
				json.put("doccount", facetEl.getCount());				
				
				// Add significance if possible
				if ((null == scoreStats) || !scoreStats.calcAssocationSignificance(sEnt1_index, sEnt2_index, sGeoIndex, json)) {
					// These fields are optional:
					//json.put("entity1_sig", 0.0);
					//json.put("entity2_sig", 0.0);
					//json.put("geo_sig", 0.0);
					// Mandatory:
					json.put("assoc_sig", 0.0);
				}				
				facetList.add(json);
			}
			nFacetEl++;
		}
		return facetList;		
	}//TESTED (see cases above - difficult to make this test case standalone because of TermsFacet.Entry)
		
}
