package org.elasticsearch.client.action.delete;

import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.WriteConsistencyLevel;
import org.elasticsearch.action.delete.DeleteResponse;

public class DeleteRequestBuilder {

	protected org.elasticsearch.action.delete.DeleteRequestBuilder _delegate;
	
	public DeleteRequestBuilder(org.elasticsearch.action.delete.DeleteRequestBuilder delegate) {
		_delegate = delegate;
	}
	public DeleteRequestBuilder setRouting(String routing) {
		_delegate.setRouting(routing);
		return this;
	}
	public DeleteRequestBuilder setConsistencyLevel(WriteConsistencyLevel consistencyLevel) {
		_delegate.setConsistencyLevel(consistencyLevel);
		return this;
	}
	public ListenableActionFuture<DeleteResponse> execute() {
		return _delegate.execute();
	}
	public org.elasticsearch.action.delete.DeleteRequestBuilder getDelegate() {
		return _delegate;
	}
}
