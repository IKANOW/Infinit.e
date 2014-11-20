package org.elasticsearch.action.admin.indices.settings;

import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.common.settings.Settings;

public class UpdateSettingsUtils {
	static public void updateSettings(IndicesAdminClient client, String indexName, Settings settings) {
		client.updateSettings(new UpdateSettingsRequest(indexName).settings(settings)).actionGet();		
	}
}
