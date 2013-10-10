package org.elasticsearch.client.action.search;

import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.search.SearchResponse;

public class SearchScrollRequestBuilder {

	protected org.elasticsearch.action.search.SearchScrollRequestBuilder _delegate;
	
	public SearchScrollRequestBuilder(org.elasticsearch.action.search.SearchScrollRequestBuilder delegate) {
		_delegate = delegate;
	}
	public SearchScrollRequestBuilder setScroll(String scrollId) {
		_delegate.setScroll(scrollId);
		return this;
	}
	public ListenableActionFuture<SearchResponse> execute() {
		return _delegate.execute();
	}


}
