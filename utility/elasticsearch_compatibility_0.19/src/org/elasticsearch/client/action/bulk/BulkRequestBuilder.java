package org.elasticsearch.client.action.bulk;

import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.WriteConsistencyLevel;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.action.delete.DeleteRequestBuilder;
import org.elasticsearch.client.action.index.IndexRequestBuilder;

public class BulkRequestBuilder {

	protected org.elasticsearch.action.bulk.BulkRequestBuilder _delegate;
	public BulkRequestBuilder(org.elasticsearch.action.bulk.BulkRequestBuilder delegate) {
		_delegate = delegate;
	}
	public BulkRequestBuilder setConsistencyLevel(WriteConsistencyLevel consistencyLevel) {
		_delegate.setConsistencyLevel(consistencyLevel);
		return this;
	}
	public BulkRequestBuilder add(DeleteRequest request) {
		_delegate.add(request);
		return this;
	}
	public BulkRequestBuilder add(DeleteRequestBuilder request) {
		_delegate.add(request.getDelegate());
		return this;
	}
	public BulkRequestBuilder add(IndexRequest request) {
		_delegate.add(request);
		return this;
	}
	public BulkRequestBuilder add(IndexRequestBuilder request) {
		_delegate.add(request.getDelegate());
		return this;
	}
	public ListenableActionFuture<BulkResponse> execute() {
		return _delegate.execute();
	}
}
