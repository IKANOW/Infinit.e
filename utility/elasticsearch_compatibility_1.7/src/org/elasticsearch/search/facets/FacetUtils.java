package org.elasticsearch.search.facets;

import org.elasticsearch.search.facet.terms.TermsFacet;

public class FacetUtils {

	public static String getTerm(TermsFacet.Entry entry) {
		return entry.getTerm().toString();
	}
}
