package org.elasticsearch.client.action.search;

import java.util.Map;

import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.facet.AbstractFacetBuilder;
import org.elasticsearch.search.sort.SortOrder;

public class SearchRequestBuilder extends org.elasticsearch.action.search.SearchRequestBuilder {

	public SearchRequestBuilder(Client client) {
		super(client);
	}
	
	public SearchRequestBuilder setIndices(String... arg0) {
		return (SearchRequestBuilder) super.setIndices(arg0);
	}
	public SearchRequestBuilder setTypes(String... arg0) {
		return (SearchRequestBuilder) super.setTypes(arg0);
	}
	
	public SearchRequestBuilder setSize(int size) {
		return (SearchRequestBuilder) super.setSize(size);
	}
	public SearchRequestBuilder setFrom(int from) {
		return (SearchRequestBuilder) super.setFrom(from);
	}
	
	public SearchRequestBuilder addScriptField(String name, String lang, String script, Map<String, Object> params) {
		return (SearchRequestBuilder) super.addScriptField(name, lang, script, params);
	}
	
	public SearchRequestBuilder addSort(String field, SortOrder order) {
		return (SearchRequestBuilder) super.addSort(field, order);
	}
	public SearchRequestBuilder setFacets(byte[] facets) {
		return (SearchRequestBuilder) super.setFacets(facets);
	}
	public SearchRequestBuilder addFacet(AbstractFacetBuilder facet) {
		return (SearchRequestBuilder) super.addFacet(facet);		
	}
	public SearchRequestBuilder setQuery(QueryBuilder query) {
		return (SearchRequestBuilder) super.setQuery(query);
	}
	public SearchRequestBuilder setQuery(String query) {
		return (SearchRequestBuilder) super.setQuery(query);
	}
	public SearchRequestBuilder setSearchType(SearchType type) {
		return (SearchRequestBuilder) super.setSearchType(type);
	}
	public SearchRequestBuilder setScroll(String keepAlive) {
		return (SearchRequestBuilder) super.setScroll(keepAlive);		
	}
	public SearchRequestBuilder setScroll(Scroll scrollId) {
		return (SearchRequestBuilder) super.setScroll(scrollId);		
	}
	public SearchRequestBuilder setScroll(TimeValue keepAlive) {
		return (SearchRequestBuilder) super.setScroll(keepAlive);		
	}
}
