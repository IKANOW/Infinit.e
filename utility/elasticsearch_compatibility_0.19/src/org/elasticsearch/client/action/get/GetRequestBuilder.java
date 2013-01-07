package org.elasticsearch.client.action.get;

import org.elasticsearch.client.Client;

public class GetRequestBuilder extends org.elasticsearch.action.get.GetRequestBuilder {

	public GetRequestBuilder(Client client) {
		super(client);
	}
	public GetRequestBuilder(Client client, String index) {
		super(client, index);
	}
	public GetRequestBuilder setFields(String... fields) {
		return (GetRequestBuilder) super.setFields(fields);
	}
}
