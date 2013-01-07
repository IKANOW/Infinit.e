package org.elasticsearch.client.action.index;

import org.elasticsearch.action.WriteConsistencyLevel;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.Client;

public class IndexRequestBuilder extends org.elasticsearch.action.index.IndexRequestBuilder {

	public IndexRequestBuilder(Client client) {
		super(client);
	}
	public IndexRequestBuilder(Client client, String index) {
		super(client, index);
	}
	public IndexRequestBuilder setSource(String source) {
		return (IndexRequestBuilder) super.setSource(source);
	}
	public IndexRequestBuilder setId(String id) {
		return (IndexRequestBuilder) super.setId(id);
	}
	public IndexRequestBuilder setOpType(IndexRequest.OpType type) {
		return (IndexRequestBuilder) super.setOpType(type);
	}
	public IndexRequestBuilder setParent(String id) {
		return (IndexRequestBuilder) super.setParent(id);
	}
	public IndexRequestBuilder setConsistencyLevel(WriteConsistencyLevel consistencyLevel) {
		return (IndexRequestBuilder) super.setConsistencyLevel(consistencyLevel);
	}
}
