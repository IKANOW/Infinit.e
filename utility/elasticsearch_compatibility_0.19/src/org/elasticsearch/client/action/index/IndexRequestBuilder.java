package org.elasticsearch.client.action.index;

import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.WriteConsistencyLevel;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;

public class IndexRequestBuilder {

	protected org.elasticsearch.action.index.IndexRequestBuilder _delegate;
	
	public IndexRequestBuilder(org.elasticsearch.action.index.IndexRequestBuilder delegate) {
		_delegate = delegate;
	}
	public IndexRequestBuilder setSource(String source) {
		_delegate.setSource(source);
		return this;
	}
	public IndexRequestBuilder setId(String id) {
		_delegate.setId(id);
		return this;
	}
	public IndexRequestBuilder setOpType(IndexRequest.OpType type) {
		_delegate.setOpType(type);
		return this;
	}
	public IndexRequestBuilder setParent(String id) {
		_delegate.setParent(id);
		return this;
	}
	public IndexRequestBuilder setConsistencyLevel(WriteConsistencyLevel consistencyLevel) {
		_delegate.setConsistencyLevel(consistencyLevel);
		return this;
	}
	public ListenableActionFuture<IndexResponse> execute() {
		return _delegate.execute();
	}
	public org.elasticsearch.action.index.IndexRequestBuilder getDelegate() {
		return _delegate;
	}
}
