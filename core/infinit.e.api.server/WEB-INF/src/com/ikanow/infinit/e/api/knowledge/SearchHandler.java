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
package com.ikanow.infinit.e.api.knowledge;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.util.Version;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.document.Field;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.document.Document;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

import org.bson.types.ObjectId;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.action.search.SearchRequestBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.BaseQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.elasticsearch.search.sort.SortOrder;

import com.ikanow.infinit.e.api.knowledge.aliases.AliasLookupTable;
import com.ikanow.infinit.e.api.knowledge.aliases.AliasManager;
import com.ikanow.infinit.e.api.utils.RESTTools;
import com.ikanow.infinit.e.data_model.api.BasePojoApiMap;
import com.ikanow.infinit.e.data_model.api.ResponsePojo;
import com.ikanow.infinit.e.data_model.api.ResponsePojo.ResponseObject;
import com.ikanow.infinit.e.data_model.api.knowledge.DimensionListPojo;
import com.ikanow.infinit.e.data_model.api.knowledge.SearchSuggestPojo;
import com.ikanow.infinit.e.data_model.index.ElasticSearchManager;
import com.ikanow.infinit.e.data_model.index.feature.entity.EntityFeaturePojoIndexMap;
import com.ikanow.infinit.e.data_model.index.feature.event.AssociationFeaturePojoIndexMap;
import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.MongoDbManager;
import com.ikanow.infinit.e.data_model.store.document.EntityPojo;
import com.ikanow.infinit.e.data_model.store.feature.association.AssociationFeaturePojo;
import com.ikanow.infinit.e.data_model.store.feature.entity.EntityFeaturePojo;
import com.ikanow.infinit.e.harvest.utils.DimensionUtility;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;


/**
 * This class is for all operations related to the retrieval, addition
 * or update of people within the system
 * 
 * @author cmorgan
 *
 */
@SuppressWarnings("deprecation")
public class SearchHandler 
{
	private static final Logger logger = Logger.getLogger(SearchHandler.class);

	private final StringBuffer logMsg = new StringBuffer();	
	private static long lastSuggestLog = 0;
	private static long lastAliasLog = 0;

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	// SEARCH SUGGEST API call

	//TODO (INF-1660): here and for assoc, should enforce doc_count>0? (or should i remove from entity feature when freq hits 0??)
	// (or both?)

	private static final String entityIndex_ = EntityFeaturePojoIndexMap.indexCollectionName_ + "/" + EntityFeaturePojoIndexMap.indexName_;

	public ResponsePojo getSuggestions(String userIdStr, String term, String communityIdStrList, boolean bIncludeGeo, boolean bIncludeLinkdata, boolean bWantNoAlias) 
	{
		long nSysTime = System.currentTimeMillis();		

		ResponsePojo rp = new ResponsePojo();

		ElasticSearchManager gazIndex = ElasticSearchManager.getIndex(entityIndex_);

		// Need to do a quick decomposition of the term to fit in with analyzed strings
		String escapedterm = null;
		StandardTokenizer st = new StandardTokenizer(Version.LUCENE_30, new StringReader(term));
		TermAttribute termAtt = st.addAttribute(TermAttribute.class);
		StringBuffer sb = new StringBuffer();
		try {
			while (st.incrementToken()) {
				if (sb.length() > 0) {
					sb.append(" +");
				}
				else {
					sb.append('+');						
				}
				sb.append(termAtt.term());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}			

		if (!term.endsWith(" ") || (0 == sb.length())) { // Could be in the middle of typing, stick a * on the end
			sb.append('*');
		}//TESTED			
		escapedterm = sb.toString();			

		// Create the search query

		SearchRequestBuilder searchOptions = gazIndex.getSearchOptions();
		BaseQueryBuilder queryObj1 = QueryBuilders.queryString(escapedterm).defaultField(EntityFeaturePojoIndexMap.Mapping.RootObject.RootProperties.alias_pri_);

		String[] communityIdStrs = RESTTools.getCommunityIds(userIdStr, communityIdStrList);
		BaseQueryBuilder queryObj2 = QueryBuilders.boolQuery().should(QueryBuilders.termsQuery(EntityFeaturePojo.communityId_, communityIdStrs));

		BaseQueryBuilder queryObj = QueryBuilders.boolQuery().must(queryObj1).must(queryObj2);

		searchOptions.addSort(EntityFeaturePojo.doccount_, SortOrder.DESC);
		searchOptions.addFields(EntityFeaturePojo.disambiguated_name_, EntityFeaturePojo.doccount_, 
				EntityFeaturePojo.type_, EntityFeaturePojo.dimension_);
		if (bIncludeGeo) {
			searchOptions.addFields(EntityFeaturePojo.geotag_);
			searchOptions.addFields(EntityFeaturePojo.ontology_type_);
		}
		if (bIncludeLinkdata) {
			searchOptions.addFields(EntityFeaturePojo.linkdata_);			
		}

		// Initial alias handling:

		AliasLookupTable aliasTable = null;
		HashMap<String, SearchSuggestPojo> aliasResults = null;
		if (!bWantNoAlias) {
			AliasManager aliasManager = AliasManager.getAliasManager();
			if (null != aliasManager) {
				aliasTable = aliasManager.getAliasLookupTable(communityIdStrList, communityIdStrs, null, userIdStr);
			}
		}
		//TESTED

		// Also create an internal Lucene index for aliases, in case any of them do not have actual entities representing them 
		List<EntityFeaturePojo> extraEntries = null;
		if (null != aliasTable) {
			extraEntries = checkAliasMasters(aliasTable, escapedterm);
		}
		// (end initial alias handling)

		int nDesiredSize = 20;
		if (null == aliasTable) {		
			searchOptions.setSize(nDesiredSize); // will forward all 20
		}
		else {
			searchOptions.addFields(EntityFeaturePojo.index_);
			searchOptions.setSize(3*nDesiredSize); // will forward top 20 after de-aliasing

			aliasResults = new HashMap<String, SearchSuggestPojo>();
			// (We use this to ensure we only include each entity once after aliasing)
		}
		//TESTED

		// Perform the search

		SearchResponse rsp = gazIndex.doQuery(queryObj, searchOptions);

		// Format the return values

		SearchHit[] docs = rsp.getHits().getHits();			
		DimensionListPojo dimlist = new DimensionListPojo();
		int nDocsAdded = 0;
		
		if (null != extraEntries) { // Put the alias masters at the top:
			System.out.println(Arrays.toString(extraEntries.toArray()));
			for (EntityFeaturePojo alias: extraEntries) {
				SearchSuggestPojo sp = new SearchSuggestPojo();
				if (null != alias.getDimension()) {
					sp.setDimension(alias.getDimension().toString());
				}
				else {
					sp.setDimension("What");
				}
				sp.setValue(alias.getDisambiguatedName());
				sp.setType(alias.getType());
				if (bIncludeGeo) { 
					sp.setGeotag(alias.getGeotag());
				}
				sp.setOntology_type(alias.getOntology_type());
				dimlist.addSearchSuggestPojo(sp); 
			}
		}//TESTED (inc geo)
		
		if (null != docs) 
		{
			for (SearchHit hit: docs) 
			{
				SearchHitField shf = hit.field(EntityFeaturePojo.disambiguated_name_);
				if (null == shf) { // robustness check, sometimes if the harvester goes wrong this field might be missing
					continue;
				}
				String disname = (String) shf.value();
				String type = (String) hit.field(EntityFeaturePojo.type_).value();
				String dimension = (String) hit.field(EntityFeaturePojo.dimension_).value();
				SearchSuggestPojo sp = new SearchSuggestPojo();				

				sp.setValue(disname);
				sp.setDimension(dimension);
				sp.setType(type);
				if (bIncludeGeo) 
				{
					SearchHitField loc = hit.field(EntityFeaturePojo.geotag_);
					if ( loc != null )
						sp.setLocFromES((String) loc.value());
					SearchHitField ont = hit.field(EntityFeaturePojo.ontology_type_);
					if ( ont != null )
						sp.setOntology_type((String)ont.value());
				}
				if (bIncludeLinkdata) {
					SearchHitField linkdata = hit.field(EntityFeaturePojo.linkdata_); 
					if ( linkdata != null )
						sp.setLinkdata(linkdata.values());
				}								

				// More alias handling
				String index = null;
				if (null != aliasTable) {
					index = (String) hit.field(EntityFeaturePojo.index_).value();
					EntityFeaturePojo alias = aliasTable.getAliasMaster(index);
					if (null != alias) { // Found!
						if (alias.getIndex().equalsIgnoreCase("discard")) { // Discard this entity
							continue;
						}
						else if ((null != alias.getDisambiguatedName()) && (null != alias.getType())) {
							// (these need to be present)

							//DEBUG (perf critical)
							//logger.debug("Alias! Replace " + index + " with " + alias.getIndex());

							index = alias.getIndex();
							disname = alias.getDisambiguatedName();
							type = alias.getType();
							if (null != alias.getDimension()) {
								dimension = alias.getDimension().toString();
							}
							else { // Guess from type
								dimension = DimensionUtility.getDimensionByType(type).toString();
							}
							// Reset values:
							sp.setValue(disname);
							sp.setDimension(dimension);
							sp.setType(type);
						}
					}
					SearchSuggestPojo existing = aliasResults.get(index);
					if (null != existing) {

						//DEBUG (perf critical)
						//logger.debug("Alias! Remove duplicate " + index);

						if ((null == existing.getGeotag()) && (null != sp.getGeotag())) {
							// (if they're both set then sigh just ignore on a first-come-first-served basis)
							existing.setGeotag(sp.getGeotag());
							existing.setOntology_type(sp.getOntology_type());
						}//TESTED
						if (null != sp.getLinkdata()) { // (here we can just combine the linkdata)
							if (null == existing.getLinkdata()) {
								existing.setLinkdata(sp.getLinkdata());
							}
							else {
								existing.getLinkdata().addAll(sp.getLinkdata());
							}
						}//TESTED
						continue; // (ie don't add this guy)
					}
					else { // add it
						aliasResults.put(index, sp);
					}
				}
				//TESTED
				// end more alias handing								

				dimlist.addSearchSuggestPojo(sp);
				// (only adds unique entries, ie handles multiple communities "ok" (only ok
				//  because it doesn't sum the doccounts across multiple communities, you'd probably
				//  want to use facets for that, but it doesn't seem worth it, especially since we're
				//  pretty short on field cache space)

				if (++nDocsAdded >= nDesiredSize) { // (can happen in the de-aliasing case)
					break;
				}//TESTED
			}			
		}
		rp.setData(dimlist);
		rp.setResponse(new ResponseObject("Suggestions",true,term));

		if (nSysTime > (lastSuggestLog + 5000)) {
			lastSuggestLog = nSysTime;
			logMsg.setLength(0);
			logMsg.append("knowledge/searchSuggest query=").append(escapedterm);
			logMsg.append(" groups=").append(communityIdStrList);
			logMsg.append(" found=").append(docs.length);
			logMsg.append(" time=").append(System.currentTimeMillis() - nSysTime).append(" ms");
			logger.info(logMsg.toString());
		}
		return rp;
	}	

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	// Geo suggestions code
	// (Haven't yet converted geo feature to string literals)

	public ResponsePojo getSuggestionsGeo(String userIdStr, String term, String communityIdStrList) 
	{			
		ResponsePojo rp = new ResponsePojo();
		ElasticSearchManager gazIndex = ElasticSearchManager.getIndex("geo_index");

		// Need to do a quick decomposition of the term to fit in with analyzed strings
		String escapedterm = null;
		StandardTokenizer st = new StandardTokenizer(Version.LUCENE_30, new StringReader(term));
		TermAttribute termAtt = st.addAttribute(TermAttribute.class);
		StringBuffer sb = new StringBuffer();
		try {
			while (st.incrementToken()) {
				if (sb.length() > 0) {
					sb.append(" +");
				}
				else {
					sb.append('+');						
				}
				sb.append(termAtt.term());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}			

		if (!term.endsWith(" ") || (0 == sb.length())) { // Could be in the middle of typing, stick a * on the end
			sb.append('*');
		}//TESTED			
		escapedterm = sb.toString();			

		// Create the search query

		SearchRequestBuilder searchOptions = gazIndex.getSearchOptions();
		BaseQueryBuilder queryObj1 = QueryBuilders.queryString(escapedterm).defaultField("search_field");	

		//TODO (INF-1279): support community specific geo (ie communityIds $exists:false or communityIds $in array)

		BaseQueryBuilder queryObj = QueryBuilders.boolQuery().must(queryObj1);

		searchOptions.addSort("population", SortOrder.DESC);
		searchOptions.addFields("search_field", "population");
		searchOptions.setSize(20);

		// Perform the search

		SearchResponse rsp = gazIndex.doQuery(queryObj, searchOptions);

		// Format the return values

		SearchHit[] docs = rsp.getHits().getHits();			
		Set<String> suggestions = new HashSet<String>();		
		for (SearchHit hit: docs) 
		{
			String suggestion = (String) hit.field("search_field").value();			
			suggestions.add(suggestion);
		}
		String[] suggestionArray = new String[suggestions.size()];
		rp.setData(Arrays.asList(suggestions.toArray(suggestionArray)), (BasePojoApiMap<String>)null);				
		rp.setResponse(new ResponseObject("Suggestions Geo", true, term));
		return rp;
	}

	// Event suggestions code

	private static final String assocIndex_ = AssociationFeaturePojoIndexMap.indexCollectionName_ + "/" + AssociationFeaturePojoIndexMap.indexName_;

	public ResponsePojo getAssociationSuggestions(String userIdStr, String ent1, String verb, String ent2, String field, String communityIdStrList, boolean bWantNoAlias) 
	{
		ResponsePojo rp = new ResponsePojo();
		try
		{
			// Community ids, needed in a couple of places
			String[] communityIdStrs = RESTTools.getCommunityIds(userIdStr, communityIdStrList);

			// Initial alias handling:
			AliasLookupTable aliasTable = null;
			// Initial alias handling:			
			if (!bWantNoAlias) {
				AliasManager aliasManager = AliasManager.getAliasManager();
				if (null != aliasManager) {
					aliasTable = aliasManager.getAliasLookupTable(communityIdStrList, communityIdStrs, null, userIdStr);
				}
			}//TESTED										

			ElasticSearchManager esm = ElasticSearchManager.getIndex(assocIndex_);
			SearchRequestBuilder searchOptions = esm.getSearchOptions();
			BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
			boolean bExtraQueryTerms = false;
			String term = "";
			if ( !ent1.equals("null") )
			{
				if ( field.equals(AssociationFeaturePojo.entity1_) )
					term = ent1;
				else {
					bExtraQueryTerms = true;
					EntityFeaturePojo alias = null;
					if (null != aliasTable) {
						alias = aliasTable.getAliasMaster(ent1);
					}
					if (null != alias) { // Found!
						boolQuery.must(QueryBuilders.termsQuery(AssociationFeaturePojo.entity1_index_, alias.getAlias().toArray()));
					}
					else {
						boolQuery.must(QueryBuilders.termQuery(AssociationFeaturePojo.entity1_index_, ent1));
					}//TESTED
				}
			}
			if ( !verb.equals("null") )
			{
				if ( field.equals(AssociationFeaturePojo.verb_) )
					term = verb;
				else
				{
					bExtraQueryTerms = true;
					boolQuery.must(QueryBuilders.queryString(new StringBuffer("+").append(verb.replaceAll("\\s+", " +")).toString()).
							defaultField(AssociationFeaturePojo.verb_));
				}
			}
			if ( !ent2.equals("null") )
			{
				if ( field.equals(AssociationFeaturePojo.entity2_) )
					term = ent2;
				else {
					bExtraQueryTerms = true;
					EntityFeaturePojo alias = null;
					if (null != aliasTable) {
						alias = aliasTable.getAliasMaster(ent2);
					}
					if (null != alias) { // Found!
						boolQuery.must(QueryBuilders.termsQuery(AssociationFeaturePojo.entity2_index_, alias.getAlias().toArray()));
					}
					else {
						boolQuery.must(QueryBuilders.termQuery(AssociationFeaturePojo.entity2_index_, ent2));
					}
				}//TESTED (cut and paste from entity1)
			}	

			String escapedterm = null;
			StandardTokenizer st = new StandardTokenizer(Version.LUCENE_30, new StringReader(term));
			TermAttribute termAtt = st.addAttribute(TermAttribute.class);
			StringBuffer sb = new StringBuffer();
			try {
				while (st.incrementToken()) {
					if (sb.length() > 0) {
						sb.append(" +");
					}
					else {
						sb.append('+');						
					}
					sb.append(termAtt.term());
				}
			} catch (IOException e) {
				e.printStackTrace();
			}			
			if (!term.endsWith(" ") || (0 == sb.length())) { // Could be in the middle of typing, stick a * on the end
				sb.append('*');
			}//TESTED			

			escapedterm = sb.toString();
			
			// Also create an internal Lucene index for aliases, in case any of them do not have actual entities representing them 
			List<EntityFeaturePojo> extraEntries = null;
			BoolQueryBuilder extraQueryTerms = null;
			if (field.startsWith("entity")) {
				String indexField = field.startsWith("entity1") ? "entity1_index" : "entity2_index";
				if (null != aliasTable) {
					extraEntries = checkAliasMasters(aliasTable, escapedterm);
				}
				if (null != extraEntries) {
					extraQueryTerms = QueryBuilders.boolQuery();
					int nExtraTerms = 0;
					Iterator<EntityFeaturePojo> aliasIt = extraEntries.iterator();
					while (aliasIt.hasNext()) {
						EntityFeaturePojo alias = aliasIt.next();						
						nExtraTerms += alias.getAlias().size();
						
						if (!bExtraQueryTerms && (nExtraTerms > 20)) { // If not filtering on event type we'll be more aggressive
							break;
						}//TESTED
						if (bExtraQueryTerms && (nExtraTerms > 60)) { // If the number of terms gets too large bail anyway
							break;
						}//TESTED
						
						extraQueryTerms.should(QueryBuilders.termsQuery(indexField, alias.getAlias().toArray()));
						aliasIt.remove();
						
					}//end loop over entities 
				}//if found new aliases
				
			}//(if this is an entity lookup) TESTED - including breaking out because of # of terms 
			
			// (end initial alias handling)
			
			if (null == extraQueryTerms) {
				boolQuery.must(QueryBuilders.queryString(escapedterm).defaultField(field));
			}
			else {//(in this case combine the escaped term with the aliases
				extraQueryTerms.should(QueryBuilders.queryString(escapedterm).defaultField(field));
				boolQuery.must(extraQueryTerms);
			}//TESTED
			boolQuery.must(QueryBuilders.termsQuery(AssociationFeaturePojo.communityId_, communityIdStrs));

			searchOptions.addSort(AssociationFeaturePojo.doccount_, SortOrder.DESC);

			// Work out which fields to return:
			//TODO (INF-1234) need to work out what to do with quotations and similar here (ie entityX without entityX_index) 
			String returnfield;
			boolean bReturningEntities = true;
			if ( field.equals(AssociationFeaturePojo.entity1_) ) {
				returnfield = AssociationFeaturePojo.entity1_index_;
				searchOptions.addFields( AssociationFeaturePojo.entity1_index_, AssociationFeaturePojo.doccount_);
			}
			else if ( field.equals(AssociationFeaturePojo.entity2_)) {
				returnfield = AssociationFeaturePojo.entity2_index_;
				searchOptions.addFields( AssociationFeaturePojo.entity2_index_, AssociationFeaturePojo.doccount_);
			}
			else {
				bReturningEntities = false;
				returnfield = AssociationFeaturePojo.verb_;
				searchOptions.addFields( AssociationFeaturePojo.verb_, AssociationFeaturePojo.verb_category_,  AssociationFeaturePojo.doccount_);
			}

			int nNumSuggestionsToReturn = 20;
			if (bReturningEntities && (null != aliasTable)) {
				searchOptions.setSize(3*nNumSuggestionsToReturn); // we're going to remove some duplicates so get more than we need
			}
			else { // normal case
				searchOptions.setSize(nNumSuggestionsToReturn);
			}

			SearchResponse rsp = esm.doQuery(boolQuery, searchOptions);
			SearchHit[] docs = rsp.getHits().getHits();

			//Currently this code takes the results and puts
			//them into a set so there are no duplicates
			//duplicates occur for example when you search for
			//obama you get obama/quotation/quote1 and obama/travel/spain
			//may want to work this differnt, or atleast sum up
			//frequency
			Set<String> suggestions = new HashSet<String>();

			for (SearchHit hit: docs) 
			{
				SearchHitField retField = hit.field(returnfield); // (this can be null in theory/by mistake)
				if (null != retField) {
					String suggestion = (String) retField.value();
					if (bReturningEntities && (null != aliasTable))
					{
						// More alias handling
						EntityFeaturePojo alias = aliasTable.getAliasMaster(suggestion);
						if (null != alias) { // Found!
							if (alias.getIndex().equalsIgnoreCase("discard")) { // Discard this entity
								continue;
							}
							else {
								// (these need to be present)
								suggestion = alias.getIndex();
							}
						}//TESTED
					}
					else { // (old code, still valid for verbs or no aliases) 
						if ( returnfield.equals(AssociationFeaturePojo.verb_) && hit.field(AssociationFeaturePojo.verb_category_) != null ) 
							//for some reason verb_cat can be null!?!?! i think this is broken (ent1 facebook inc/company verb *)
						{
							String verbcat = (String)hit.field(AssociationFeaturePojo.verb_category_).value();
							suggestion += " (" + verbcat + ")";
							suggestions.add(verbcat);
						}
					}
					suggestions.add(suggestion);

					if (suggestions.size() >= nNumSuggestionsToReturn) {
						break;
					}

				} // (end return string valid)
			}//end loop over suggestions

			// Add any aliases that I couldn't explicity convert to query terms
			if ((null != extraEntries) && (suggestions.size() < nNumSuggestionsToReturn)) {
				for (EntityFeaturePojo alias: extraEntries) {
					suggestions.add(alias.getIndex());
					if (suggestions.size() >= nNumSuggestionsToReturn) {
						break;
					}					
				}
			}//(end add any remaining entries)
			//TESTED			
			
			String[] suggestionArray = new String[suggestions.size()];
			rp.setData(Arrays.asList(suggestions.toArray(suggestionArray)), (BasePojoApiMap<String>)null);

			String searchTerm = "";
			if ( field.equals(AssociationFeaturePojo.entity1_))
				searchTerm = ent1;
			else if ( field.equals(AssociationFeaturePojo.verb_))
				searchTerm = verb;
			else
				searchTerm = ent2;

			rp.setResponse(new ResponseObject("Association Suggestions", true, searchTerm));
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			rp.setResponse(new ResponseObject("Association Suggestions",false,"Response returned unsuccessfully: " + ex.getMessage()));
		}
		return rp;
	}	

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	// Unused Alias code - returns aliases for a term
	// (The GUI code crashes or something, and anyway I'm not convinced we want to expose this to the user)

	public ResponsePojo getAliasSuggestions(String userIdStr, String term, String field, String communityIdStrList) 
	{
		long nSysTime = System.currentTimeMillis();		
		ResponsePojo rp = new ResponsePojo();

		// (keep user facing data model consistent, ie index(ex gazateer_index), actual_name/alias, disambiguated_name (ex disambiguous_name))
		if (field.equalsIgnoreCase(EntityPojo.actual_name_) || field.equalsIgnoreCase(EntityFeaturePojo.alias_)) {
			field = EntityFeaturePojo.alias_;
		}
		else if (field.equalsIgnoreCase("disambiguous_name") || field.equals(EntityPojo.disambiguated_name_) 
				|| field.equals(EntityFeaturePojo.disambiguated_name_)) {
			//^^ (for bw compatibility from GUI)
			field = EntityFeaturePojo.disambiguated_name_;
		}
		else if (field.equalsIgnoreCase("gazateer_index") || field.equalsIgnoreCase(EntityPojo.index_)) { // (for bw compatibility from GUI)
			field = EntityFeaturePojo.index_;
		}
		else if (!field.equalsIgnoreCase(EntityFeaturePojo.index_)) {
			rp.setResponse(new ResponseObject("aliasSuggest",false, "Field " + field + " not recognized"));
			return rp;
		}

		try
		{				 
			Collection<Set<String>> aliasSet = findAliases(null, field, Arrays.asList(term), userIdStr, communityIdStrList).values();
			Set<String> superSet = new HashSet<String>();
			for (Set<String> set : aliasSet )
			{
				superSet.addAll(set);
			}			 
			rp.setData(superSet, (BasePojoApiMap<String>)null);
			rp.setResponse(new ResponseObject("aliasSuggest",true,"Successfully returned aliases"));

			if (nSysTime > (lastAliasLog + 5000)) {
				lastAliasLog = nSysTime;
				logMsg.setLength(0);
				logMsg.append("knowledge/aliasSuggest query=").append(term);
				logMsg.append(" found=").append(superSet.size());
				logMsg.append(" time=").append(System.currentTimeMillis() - nSysTime).append(" ms");
				logger.info(logMsg.toString());
			}					
		}
		catch (Exception e)
		{
			// If an exception occurs log the error
			logger.error("Exception Message: " + e.getMessage(), e);
			rp.setResponse(new ResponseObject("aliasSuggest",false,"Error returning aliases"));
		} 
		return rp;
	}		

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	// Alias utility code - used by (unused) alias suggestions code above and also for alias expansion

	public static Map<String, Set<String>> findAliases(DBCollection entityFeatureDb, String field, Collection<String> terms, String userIdStr, String communityIdStrList)
	{
		Map<String, Set<String>> aliases = new HashMap<String, Set<String>>();		
		String[] communityIdStrs = RESTTools.getCommunityIds(userIdStr, communityIdStrList);
		try
		{
			if (null == entityFeatureDb) {
				entityFeatureDb = DbManager.getFeature().getEntity();
			}

			// Get all the aliases in one go, will sort them out later
			BasicDBObject query = new BasicDBObject();			
			query.put(field, new BasicDBObject(MongoDbManager.in_, terms));
			ObjectId[] communityIds = new ObjectId[communityIdStrs.length];
			int i = 0;
			for (String idStr: communityIdStrs) {
				communityIds[i] = new ObjectId(idStr);
				i++;
			}
			query.put(EntityFeaturePojo.communityId_, new BasicDBObject(MongoDbManager.in_, communityIds));

			List<EntityFeaturePojo> gpl = EntityFeaturePojo.listFromDb(entityFeatureDb.find(query), EntityFeaturePojo.listType());		

			for ( String s : terms )
			{				
				aliases.put(s, new HashSet<String>());
				for (EntityFeaturePojo gpit : gpl) 
				{
					if ((field.equals(EntityFeaturePojo.index_) && gpit.getIndex().equals(s)) // gazname
							||
							(field.equals(EntityFeaturePojo.disambiguated_name_) && gpit.getDisambiguatedName().equals(s)) // alias
							||
							(field.equals(EntityFeaturePojo.alias_) && gpit.getAlias().contains(s))) // alias
					{
						aliases.get(s).addAll(gpit.getAlias());						
					}
				}
			}
		}
		catch(Exception e)
		{
			logger.error("Exception Message: " + e.getMessage(), e);
		}
		return aliases;
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	// INTERNAL SEARCHING OF ALIAS MASTERS (USES LUCENE)
	
	private static Searcher _aliasSearcherCache = null;
	private static Date _searcherCacheLastCreated = null;
	private static EntityFeaturePojo[] indexToSearchCacheIndexes = null;

	private synchronized void createAliasSearchCache(AliasLookupTable aliasTable)
	{
		// Check if we need to update the Lucene store:
		if ((null != _searcherCacheLastCreated) && (null != aliasTable.getLastModified())) {
			if (_searcherCacheLastCreated.getTime() >= aliasTable.getLastModified().getTime()) {
				return;
			}
		}//TESTED

		RAMDirectory idx = new RAMDirectory();

		try {

			IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_30, new StandardAnalyzer(Version.LUCENE_30));
			IndexWriter writer = new IndexWriter(idx, config);
			int nAdded = 0;
			indexToSearchCacheIndexes = new EntityFeaturePojo[aliasTable.masters().size()];
			for (EntityFeaturePojo alias: aliasTable.masters()) {

				if ((null != alias.getIndex()) && (null != alias.getDisambiguatedName()) && (null != alias.getAlias()) 
						&& !alias.getIndex().equalsIgnoreCase("discard") && !alias.getAlias().contains(alias.getIndex()))
				{
					// (that last check just means there's no point in including the alias if it has itself as a sub-alias) 
					Document doc = new Document();
					doc.add(new Field("name", alias.getDisambiguatedName(), Field.Store.NO, Field.Index.ANALYZED));
					writer.addDocument(doc);
					indexToSearchCacheIndexes[nAdded] = alias;
					nAdded++;
					//System.out.println("CACHE ADD: " + alias.getDisambiguatedName() + ": " + nAdded + " - " + alias.getIndex());
				}
			}
			writer.close();
			
			if (nAdded > 0) {
				_aliasSearcherCache = new IndexSearcher(idx);
				if (null != _aliasSearcherCache) {
					_searcherCacheLastCreated = aliasTable.getLastModified();
				}
			}
			else {
				_aliasSearcherCache = null;
				_searcherCacheLastCreated = aliasTable.getLastModified();				 
			}
		}//TESTED
		catch (Exception e) {
			//Probably should never happen once set up correctly
			e.printStackTrace();
		}
	}//TESTED

	private ArrayList<EntityFeaturePojo> checkAliasMasters(AliasLookupTable aliasTable, String term) {
		createAliasSearchCache(aliasTable); // (only does anything if needed)
		ArrayList<EntityFeaturePojo> retVal = null;

		if (null != _aliasSearcherCache) {
			try {
				if (term.startsWith("*")) { // match all
					retVal = new ArrayList<EntityFeaturePojo>(indexToSearchCacheIndexes.length);
					for (EntityFeaturePojo ent: indexToSearchCacheIndexes) {
						if (null != ent) {
							retVal.add(ent);
						}
						else {
							break;
						}
					}
				}//TESTED (end special case, "*" wildcard)
				else {
					Query query = new QueryParser(Version.LUCENE_30, "name", new StandardAnalyzer(Version.LUCENE_30)).parse(term);
					TopDocs results = _aliasSearcherCache.search(query, aliasTable.masters().size());
					ScoreDoc[] hits = results.scoreDocs;
					if (hits.length > 0) {
						retVal = new ArrayList<EntityFeaturePojo>(hits.length);
						for (ScoreDoc hit: hits) {
							retVal.add(indexToSearchCacheIndexes[hit.doc]);
						}
					}
				}//TESTED (normal case, Lucene lookup)
			} 
			catch (Exception e) {
				//Probably should never happen once set up correctly
				e.printStackTrace();
			}			
		}
		return retVal;
	}//TESTED

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	// Some Lucene utlities:

	public static String luceneEncode(String rawQuery)
	{
		// + - && || ! ( ) { } [ ] ^ " ~ * ? : \
		/// add quotes to make it exact
		return '"' + rawQuery.replaceAll("([\"+~*?:|&(){}\\[\\]\\^\\!\\-\\\\])", "\\\\$1") + '"';
	}
	public static String luceneEncodeTerm(String rawQueryTerm)
	{
		// + - && || ! ( ) { } [ ] ^ " ~ * ? : \
		/// (no quotes)
		return rawQueryTerm.replaceAll("([\"+~*?:|&(){}\\[\\]\\^\\!\\-\\\\])", "\\\\$1");
	}
}

