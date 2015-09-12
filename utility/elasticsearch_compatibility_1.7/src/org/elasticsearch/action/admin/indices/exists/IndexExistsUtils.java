package org.elasticsearch.action.admin.indices.exists;

import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.client.IndicesAdminClient;

public class IndexExistsUtils {

	public static boolean exists(IndicesAdminClient client, String indexName) {
		return client.exists(new IndicesExistsRequest(indexName)).actionGet().isExists();
	}
}
