package org.elasticsearch.client.action.search;

import org.elasticsearch.client.Client;

public class SearchScrollRequestBuilder extends org.elasticsearch.action.search.SearchScrollRequestBuilder {

	public SearchScrollRequestBuilder(Client client) {
		super(client);
	}
	public SearchScrollRequestBuilder(Client client, String scrollId) {
		super(client, scrollId);
	}
	public SearchScrollRequestBuilder setScroll(String scrollId) {
		return (SearchScrollRequestBuilder) super.setScroll(scrollId);
	}


}
