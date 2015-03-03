package org.elasticsearch.search.facets;

public class CrossVersionFacetBuilders {

	public static CrossVersionFacetBuilder.TermsFacetBuilder termsFacet(String name) {
		return new CrossVersionFacetBuilder.TermsFacetBuilder(name);
	}
	public static CrossVersionFacetBuilder.DateHistogramFacetBuilder dateHistogramFacet(String name) {
		return new CrossVersionFacetBuilder.DateHistogramFacetBuilder(name);
	}
}
