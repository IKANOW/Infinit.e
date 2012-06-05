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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.util.Version;
import org.bson.types.ObjectId;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.action.search.SearchRequestBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.BaseQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.elasticsearch.search.sort.SortOrder;

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
	
	public ResponsePojo getSuggestions(String userIdStr, String term, String communityIdStrList, boolean bIncludeGeo, boolean bIncludeLinkdata) 
	{
		long nSysTime = System.currentTimeMillis();		
		
		ResponsePojo rp = new ResponsePojo();

		ElasticSearchManager gazIndex = ElasticSearchManager.getIndex(EntityFeaturePojoIndexMap.indexName_);
		
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
		searchOptions.setSize(20);
		
		// Perform the search

		SearchResponse rsp = gazIndex.doQuery(queryObj, searchOptions);

		// Format the return values
		
		SearchHit[] docs = rsp.getHits().getHits();			
		DimensionListPojo dimlist = new DimensionListPojo();
		if (null != docs) 
		{
			for (SearchHit hit: docs) 
			{
				SearchHitField shf = hit.field(EntityFeaturePojo.disambiguated_name_);
				if (null == shf) { // robustness check, sometimes if the harvester goes wrong this field might be missing
					continue;
				}
				
				SearchSuggestPojo sp = new SearchSuggestPojo();
				sp.setValue((String) shf.value());
				sp.setDimension((String) hit.field(EntityFeaturePojo.dimension_).value());
				sp.setType((String) hit.field(EntityFeaturePojo.type_).value());
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
				dimlist.addSearchSuggestPojo(sp);
					// (only adds unique entries, ie handles multiple communities "ok" (only ok
					//  because it doesn't sum the doccounts across multiple communities, you'd probably
					//  want to use facets for that, but it doesn't seem worth it, especially since we're
					//  pretty short on field cache space)				
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
	
	public ResponsePojo getAssociationSuggestions(String userIdStr, String ent1, String verb, String ent2, String field, String communityIdStrList) 
	{
		ResponsePojo rp = new ResponsePojo();
		try
		{			
			ElasticSearchManager esm = ElasticSearchManager.getIndex(AssociationFeaturePojoIndexMap.indexName_);
			SearchRequestBuilder searchOptions = esm.getSearchOptions();
			BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
			String term = "";
			if ( !ent1.equals("null") )
			{
				if ( field.equals(AssociationFeaturePojo.entity1_) )
					term = ent1;
				else
					boolQuery.must(QueryBuilders.termQuery(AssociationFeaturePojo.entity1_index_, ent1));
			}
			if ( !verb.equals("null") )
			{
				if ( field.equals(AssociationFeaturePojo.verb_) )
					term = verb;
				else
				{
					boolQuery.must(QueryBuilders.queryString(new StringBuffer("+").append(verb.replaceAll("\\s+", " +")).toString()).
									defaultField(AssociationFeaturePojo.verb_));
				}
			}
			if ( !ent2.equals("null") )
			{
				if ( field.equals(AssociationFeaturePojo.entity2_) )
					term = ent2;
				else
					boolQuery.must(QueryBuilders.termQuery(AssociationFeaturePojo.entity2_index_, ent2));
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
			boolQuery.must(QueryBuilders.queryString(escapedterm).defaultField(field));
			String[] communityIdStrs = RESTTools.getCommunityIds(userIdStr, communityIdStrList);
			boolQuery.must(QueryBuilders.termsQuery(AssociationFeaturePojo.communityId_, communityIdStrs));
			
			searchOptions.addSort(AssociationFeaturePojo.doccount_, SortOrder.DESC);

			// Work out which fields to return:
			//TODO (INF-1234) need to work out what to do with quotations and similar here (ie entityX without entityX_index) 
			String returnfield;
			if ( field.equals(AssociationFeaturePojo.entity1_) ) {
				returnfield = AssociationFeaturePojo.entity1_index_;
				searchOptions.addFields( AssociationFeaturePojo.entity1_index_, AssociationFeaturePojo.doccount_);
			}
			else if ( field.equals(AssociationFeaturePojo.entity2_)) {
				returnfield = AssociationFeaturePojo.entity2_index_;
				searchOptions.addFields( AssociationFeaturePojo.entity2_index_, AssociationFeaturePojo.doccount_);
			}
			else {
				returnfield = AssociationFeaturePojo.verb_;
				searchOptions.addFields( AssociationFeaturePojo.verb_, AssociationFeaturePojo.verb_category_,  AssociationFeaturePojo.doccount_);
			}
			searchOptions.setSize(20);
			
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
					if ( returnfield.equals(AssociationFeaturePojo.verb_) && hit.field(AssociationFeaturePojo.verb_category_) != null ) 
							//for some reason verb_cat can be null!?!?! i think this is broken (ent1 facebook inc/company verb *)
					{
						String verbcat = (String)hit.field(AssociationFeaturePojo.verb_category_).value();
						suggestion += " (" + verbcat + ")";
						suggestions.add(verbcat);
					}
					suggestions.add(suggestion);
				}
			}
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

