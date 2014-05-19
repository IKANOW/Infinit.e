package org.elasticsearch.client.action.delete;

import org.elasticsearch.action.delete.DeleteResponse;

public class DeleteResponseUtils {
	public static boolean isFound(DeleteResponse dr) {
		return !dr.isNotFound();
	}
}
