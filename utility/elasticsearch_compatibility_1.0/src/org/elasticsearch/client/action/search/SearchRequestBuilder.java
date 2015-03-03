package org.elasticsearch.client.action.search;

import java.util.Map;

import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.facets.CrossVersionFacetBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortOrder;

public class SearchRequestBuilder {

	protected org.elasticsearch.action.search.SearchRequestBuilder _delegate;
	
	// 1.x:
	public SearchRequestBuilder addAggregation(AbstractAggregationBuilder agg) {
		_delegate.addAggregation(agg);
		return this;
	}
	
	// 0.19-
	
	public SearchRequestBuilder(org.elasticsearch.action.search.SearchRequestBuilder delegate) {
		_delegate = delegate;
	}
	
	public SearchRequestBuilder setIndices(String... arg0) {
		_delegate.setIndices(arg0);
		return this;
	}
	public SearchRequestBuilder setTypes(String... arg0) {
		_delegate.setTypes(arg0);
		return this;
	}
	
	public SearchRequestBuilder setSize(int size) {
		_delegate.setSize(size);
		return this;
	}
	public SearchRequestBuilder setFrom(int from) {
		_delegate.setFrom(from);
		return this;
	}
	
	public SearchRequestBuilder addScriptField(String name, String lang, String script, Map<String, Object> params) {
		_delegate.addScriptField(name, lang, script, params);
		return this;
	}
	
	public SearchRequestBuilder setExplain(boolean explain) {
		_delegate.setExplain(explain);
		return this;
	}
	
	public SearchRequestBuilder setFilter(FilterBuilder filter) {
		_delegate.setPostFilter(filter);
		return this;
		
	}
	public SearchRequestBuilder addSort(String field, SortOrder order) {
		_delegate.addSort(field, order);
		return this;
	}
	public SearchRequestBuilder addSort(SortBuilder sort) {
		_delegate.addSort(sort);
		return this;
	}
	public SearchRequestBuilder setFacets(byte[] facets) {
		_delegate.setFacets(facets);
		return this;
	}
	public SearchRequestBuilder addFacet(CrossVersionFacetBuilder facet) {
		_delegate.addFacet(facet.getVersionedInterface());
		return this;
	}
	public SearchRequestBuilder setQuery(QueryBuilder query) {
		_delegate.setQuery(query);
		return this;
	}
	public SearchRequestBuilder setQuery(String query) {
		_delegate.setQuery(query);
		return this;
	}
	public SearchRequestBuilder setSearchType(SearchType type) {
		_delegate.setSearchType(type);
		return this;
	}
	public SearchRequestBuilder setScroll(String keepAlive) {
		_delegate.setScroll(keepAlive);		
		return this;
	}
	public SearchRequestBuilder setScroll(Scroll scrollId) {
		_delegate.setScroll(scrollId);		
		return this;
	}
	public SearchRequestBuilder setScroll(TimeValue keepAlive) {
		_delegate.setScroll(keepAlive);		
		return this;
	}
	public SearchRequestBuilder addFields(String... fields) {
		_delegate.addFields(fields);	
		return this;
	}
	public SearchRequestBuilder addField(String field) {
		_delegate.addField(field);	
		return this;
	}
	public ListenableActionFuture<SearchResponse> execute() {
		return _delegate.execute();
	}
}
