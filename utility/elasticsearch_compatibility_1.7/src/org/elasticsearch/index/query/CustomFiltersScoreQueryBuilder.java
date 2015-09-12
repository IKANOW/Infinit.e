package org.elasticsearch.index.query;

import java.io.IOException;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.lucene.search.function.CombineFunction;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;

//The old customfiltersscorequerybuilder is deprecated to this bad boy
	//try to copy all the functionality of the old calls and point it to this new version
public class CustomFiltersScoreQueryBuilder extends BaseQueryBuilder implements BoostableQueryBuilder<CustomFiltersScoreQueryBuilder>
{
	private FunctionScoreQueryBuilder _delegate;
	
	public CustomFiltersScoreQueryBuilder(FunctionScoreQueryBuilder functionScoreQuery)
	{
		_delegate = functionScoreQuery;
	}
	
	public CustomFiltersScoreQueryBuilder add(FilterBuilder filterBuilder, float key) 
	{
		ScoreFunctionBuilder scoreFunctionBuilder = ScoreFunctionBuilders.factorFunction(key); 
		_delegate.add(filterBuilder, scoreFunctionBuilder);
		return this;		
	}

	public CustomFiltersScoreQueryBuilder add(FilterBuilder filter,
			ScoreFunctionBuilder scoreFunctionBuilder) {
		_delegate.add(filter, scoreFunctionBuilder);
		return this;
	}

	public CustomFiltersScoreQueryBuilder add(
			ScoreFunctionBuilder scoreFunctionBuilder) {
		_delegate.add(scoreFunctionBuilder);
		return this;
	}

	public CustomFiltersScoreQueryBuilder boost(float boost) {
		_delegate.boost(boost);
		return this;
	}

	public CustomFiltersScoreQueryBuilder boostMode(CombineFunction combineFunction) {
		_delegate.boostMode(combineFunction);
		return this;
	}

	public CustomFiltersScoreQueryBuilder boostMode(String boostMode) {
		_delegate.boostMode(boostMode);
		return this;
	}

	public BytesReference buildAsBytes() throws ElasticsearchException {
		return _delegate.buildAsBytes();
	}

	public BytesReference buildAsBytes(XContentType arg0)
			throws ElasticsearchException {
		return _delegate.buildAsBytes(arg0);
	}

	public int hashCode() {
		return _delegate.hashCode();
	}

	public boolean equals(Object obj) {
		return _delegate.equals(obj);
	}

	public CustomFiltersScoreQueryBuilder maxBoost(float maxBoost) {
		_delegate.maxBoost(maxBoost);
		return this;
	}

	public CustomFiltersScoreQueryBuilder scoreMode(String scoreMode) {
		_delegate.scoreMode(scoreMode);
		return this;
	}

	public String toString() {
		return _delegate.toString();
	}

	public XContentBuilder toXContent(XContentBuilder builder, Params params)
			throws IOException {
		return _delegate.toXContent(builder, params);
	}

	@Override
	protected void doXContent(XContentBuilder arg0, Params arg1)
			throws IOException {
		
		throw new IOException();
	}
}
