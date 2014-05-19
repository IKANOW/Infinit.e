package org.elasticsearch.action.admin.indices.alias;

public class IndicesAliasUtils {

	public static void addAlias(IndicesAliasesRequest request, String alias, String index) {
		request.addAlias(index, alias);
	}
	public static void removeAlias(IndicesAliasesRequest request, String alias, String index) {
		request.removeAlias(index, alias);
	}
}
