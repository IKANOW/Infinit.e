package org.elasticsearch.client.action.deletebyquery;

import org.elasticsearch.action.WriteConsistencyLevel;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilder;

public class DeleteByQueryRequestBuilder extends org.elasticsearch.action.deletebyquery.DeleteByQueryRequestBuilder {

	public DeleteByQueryRequestBuilder(Client client) {
		super(client);
	}
	public DeleteByQueryRequestBuilder setConsistencyLevel(WriteConsistencyLevel consistencyLevel) {
		return (DeleteByQueryRequestBuilder) super.setConsistencyLevel(consistencyLevel);
	}
	public DeleteByQueryRequestBuilder setTypes(String... arg0) {
		return (DeleteByQueryRequestBuilder) super.setTypes(arg0);
	}
	public DeleteByQueryRequestBuilder setQuery(String query) {
		return (DeleteByQueryRequestBuilder) super.setQuery(query);
	}
	public DeleteByQueryRequestBuilder setRouting(String routing) {
		return (DeleteByQueryRequestBuilder) super.setRouting(routing);
	}
	public DeleteByQueryRequestBuilder setRouting(String... routing) {
		return (DeleteByQueryRequestBuilder) super.setRouting(routing);
	}
	public DeleteByQueryRequestBuilder setQuery(QueryBuilder query) {
		return (DeleteByQueryRequestBuilder) super.setQuery(query);
	}

}
