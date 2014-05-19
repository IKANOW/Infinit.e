package org.elasticsearch.search.facets;

import java.io.IOException;

import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.ToXContent.Params;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.search.facet.terms.TermsFacet.ComparatorType;

public abstract class CrossVersionFacetBuilder {

	public abstract org.elasticsearch.search.facet.AbstractFacetBuilder getVersionedInterface();

	////////////////////////////////////////////////////////////////////////////////////////
	
	// DATE HISTOGRAM
	
	public static class DateHistogramFacetBuilder extends CrossVersionFacetBuilder {
		
		private org.elasticsearch.search.facet.datehistogram.DateHistogramFacetBuilder _delegate;
		
		protected DateHistogramFacetBuilder() {}
		public DateHistogramFacetBuilder(String name) {
			_delegate = new org.elasticsearch.search.facet.datehistogram.DateHistogramFacetBuilder(name);
		}
		
 		public DateHistogramFacetBuilder comparator(
				org.elasticsearch.search.facet.datehistogram.DateHistogramFacet.ComparatorType comparatorType) {
			_delegate.comparator(comparatorType);
			return this;
		}

		public boolean equals(Object obj) {
			return _delegate.equals(obj);
		}

		public DateHistogramFacetBuilder facetFilter(
				FilterBuilder filter) {
			_delegate.facetFilter(filter);
			return this;
		}

		public DateHistogramFacetBuilder factor(
				float factor) {
			_delegate.factor(factor);
			return this;
		}

		public DateHistogramFacetBuilder field(
				String field) {
			_delegate.field(field);
			return this;
		}

		public DateHistogramFacetBuilder global(
				boolean global) {
			_delegate.global(global);
			return this;
		}

		public int hashCode() {
			return _delegate.hashCode();
		}

		public DateHistogramFacetBuilder interval(
				String interval) {
			_delegate.interval(interval);
			return this;
		}

		public DateHistogramFacetBuilder keyField(
				String keyField) {
			_delegate.keyField(keyField);
			return this;
		}

		public DateHistogramFacetBuilder lang(
				String lang) {
			_delegate.lang(lang);
			return this;
		}

		public DateHistogramFacetBuilder nested(
				String nested) {
			_delegate.nested(nested);
			return this;
		}

		public DateHistogramFacetBuilder param(
				String name, Object value) {
			_delegate.param(name, value);
			return this;
		}

		public DateHistogramFacetBuilder postOffset(
				TimeValue postOffset) {
			_delegate.postOffset(postOffset);
			return this;
		}

		public DateHistogramFacetBuilder postZone(
				String postZone) {
			_delegate.postZone(postZone);
			return this;
		}

		public DateHistogramFacetBuilder preOffset(
				TimeValue preOffset) {
			_delegate.preOffset(preOffset);
			return this;
		}

		public DateHistogramFacetBuilder preZone(
				String preZone) {
			_delegate.preZone(preZone);
			return this;
		}

		public DateHistogramFacetBuilder preZoneAdjustLargeInterval(
				boolean preZoneAdjustLargeInterval) {
			_delegate
					.preZoneAdjustLargeInterval(preZoneAdjustLargeInterval);
			return this;
		}

		public String toString() {
			return _delegate.toString();
		}

		public XContentBuilder toXContent(XContentBuilder builder, Params params)
				throws IOException {
			return _delegate.toXContent(builder, params);
		}

		public DateHistogramFacetBuilder valueField(
				String valueField) {
			_delegate.valueField(valueField);
			return this;
		}

		public DateHistogramFacetBuilder valueScript(
				String valueScript) {
			_delegate.valueScript(valueScript);
			return this;
		}

		@Override
		public org.elasticsearch.search.facet.AbstractFacetBuilder getVersionedInterface() {
			// (don't call except within CrossVersion compability code)
			return _delegate;
		}
		
	}
	
	////////////////////////////////////////////////////////////////////////////////////////
	
	// TERMS
	
	public static class TermsFacetBuilder extends CrossVersionFacetBuilder {

		private org.elasticsearch.search.facet.terms.TermsFacetBuilder _delegate;
		
		@Override
		public org.elasticsearch.search.facet.AbstractFacetBuilder getVersionedInterface() {
			// (don't call except within CrossVersion compability code)
			return _delegate;
		}
		protected TermsFacetBuilder() {}
		public TermsFacetBuilder(String name) {
			_delegate = new org.elasticsearch.search.facet.terms.TermsFacetBuilder(name);
		}		
		
		public TermsFacetBuilder allTerms(
				boolean allTerms) {
			_delegate.allTerms(allTerms);
			return this;
		}

		public boolean equals(Object obj) {
			return _delegate.equals(obj);
		}

		public TermsFacetBuilder exclude(
				Object... exclude) {
			_delegate.exclude(exclude);
			return this;
		}

		public TermsFacetBuilder executionHint(
				String executionHint) {
			_delegate.executionHint(executionHint);
			return this;
		}

		public TermsFacetBuilder facetFilter(
				FilterBuilder filter) {
			_delegate.facetFilter(filter);
			return this;
		}

		public TermsFacetBuilder field(
				String field) {
			_delegate.field(field);
			return this;
		}

		public TermsFacetBuilder fields(
				String... fields) {
			_delegate.fields(fields);
			return this;
		}

		public TermsFacetBuilder global(
				boolean global) {
			_delegate.global(global);
			return this;
		}

		public int hashCode() {
			return _delegate.hashCode();
		}

		public TermsFacetBuilder lang(
				String lang) {
			_delegate.lang(lang);
			return this;
		}

		public TermsFacetBuilder nested(
				String nested) {
			_delegate.nested(nested);
			return this;
		}

		public TermsFacetBuilder order(
				ComparatorType comparatorType) {
			_delegate.order(comparatorType);
			return this;
		}

		public TermsFacetBuilder param(
				String name, Object value) {
			_delegate.param(name, value);
			return this;
		}

		public TermsFacetBuilder regex(
				String regex, int flags) {
			_delegate.regex(regex, flags);
			return this;
		}

		public TermsFacetBuilder regex(
				String regex) {
			_delegate.regex(regex);
			return this;
		}

		public TermsFacetBuilder script(
				String script) {
			_delegate.script(script);
			return this;
		}

		public TermsFacetBuilder scriptField(
				String scriptField) {
			_delegate.scriptField(scriptField);
			return this;
		}

		public TermsFacetBuilder shardSize(
				int shardSize) {
			//(do nothing)
			return this;
		}

		public TermsFacetBuilder size(
				int size) {
			_delegate.size(size);
			return this;
		}

		public String toString() {
			return _delegate.toString();
		}

		public XContentBuilder toXContent(XContentBuilder arg0, Params arg1)
				throws IOException {
			return _delegate.toXContent(arg0, arg1);
		}

	}
}
