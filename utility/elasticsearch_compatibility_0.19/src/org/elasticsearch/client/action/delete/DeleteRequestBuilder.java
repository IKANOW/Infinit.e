package org.elasticsearch.client.action.delete;

import org.elasticsearch.action.WriteConsistencyLevel;
import org.elasticsearch.client.Client;

public class DeleteRequestBuilder extends org.elasticsearch.action.delete.DeleteRequestBuilder {

	public DeleteRequestBuilder(Client client) {
		super(client);
	}
	public DeleteRequestBuilder(Client client, String s) {
		super(client, s);
	}
	public DeleteRequestBuilder setRouting(String routing) {
		return (DeleteRequestBuilder) super.setRouting(routing);
	}
	public DeleteRequestBuilder setConsistencyLevel(WriteConsistencyLevel consistencyLevel) {
		return (DeleteRequestBuilder) super.setConsistencyLevel(consistencyLevel);
	}
}
