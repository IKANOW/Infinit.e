package org.elasticsearch.client.action.bulk;

import org.elasticsearch.action.WriteConsistencyLevel;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.action.delete.DeleteRequestBuilder;
import org.elasticsearch.client.action.index.IndexRequestBuilder;

public class BulkRequestBuilder extends org.elasticsearch.action.bulk.BulkRequestBuilder {

	public BulkRequestBuilder(Client client) {
		super(client);
	}
	public BulkRequestBuilder setConsistencyLevel(WriteConsistencyLevel consistencyLevel) {
		return (BulkRequestBuilder) super.setConsistencyLevel(consistencyLevel);
	}
	public BulkRequestBuilder add(DeleteRequest request) {
		return (BulkRequestBuilder) super.add(request);
	}
	public BulkRequestBuilder add(DeleteRequestBuilder request) {
		return (BulkRequestBuilder) super.add(request);
	}
	public BulkRequestBuilder add(IndexRequest request) {
		return (BulkRequestBuilder) super.add(request);
	}
	public BulkRequestBuilder add(IndexRequestBuilder request) {
		return (BulkRequestBuilder) super.add(request);
	}
	
}
