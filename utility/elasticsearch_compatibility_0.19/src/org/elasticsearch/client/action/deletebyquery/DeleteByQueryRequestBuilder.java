package org.elasticsearch.client.action.deletebyquery;

import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.WriteConsistencyLevel;
import org.elasticsearch.action.deletebyquery.DeleteByQueryResponse;
import org.elasticsearch.index.query.QueryBuilder;

public class DeleteByQueryRequestBuilder  {

	protected org.elasticsearch.action.deletebyquery.DeleteByQueryRequestBuilder _delegate;
	
	public DeleteByQueryRequestBuilder(org.elasticsearch.action.deletebyquery.DeleteByQueryRequestBuilder delegate) {
		_delegate = delegate;
	}
	public DeleteByQueryRequestBuilder setConsistencyLevel(WriteConsistencyLevel consistencyLevel) {
		_delegate.setConsistencyLevel(consistencyLevel);
		return this;
	}
	public DeleteByQueryRequestBuilder setTypes(String... arg0) {
		_delegate.setTypes(arg0);
		return this;
	}
	public DeleteByQueryRequestBuilder setQuery(String query) {
		_delegate.setQuery(query);
		return this;
	}
	public DeleteByQueryRequestBuilder setRouting(String routing) {
		_delegate.setRouting(routing);
		return this;
	}
	public DeleteByQueryRequestBuilder setRouting(String... routing) {
		_delegate.setRouting(routing);
		return this;
	}
	public DeleteByQueryRequestBuilder setQuery(QueryBuilder query) {
		_delegate.setQuery(query);
		return this;
	}
	public ListenableActionFuture<DeleteByQueryResponse> execute() {
		return _delegate.execute();
	}

}
