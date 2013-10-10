package org.elasticsearch.client.action.get;

import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.get.GetResponse;

public class GetRequestBuilder {

	protected org.elasticsearch.action.get.GetRequestBuilder _delegate;
	
	public GetRequestBuilder(org.elasticsearch.action.get.GetRequestBuilder delegate) {
		_delegate = delegate;
	}
	public GetRequestBuilder setFields(String... fields) {
		_delegate.setFields(fields);
		return this;
	}
	public ListenableActionFuture<GetResponse> execute() {
		return _delegate.execute();
	}
}
