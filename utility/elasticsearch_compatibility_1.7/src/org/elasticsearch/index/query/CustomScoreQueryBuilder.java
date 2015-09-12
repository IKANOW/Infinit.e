package org.elasticsearch.index.query;

import java.io.IOException;
import java.util.Map;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.lucene.search.function.CombineFunction;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;

public class CustomScoreQueryBuilder extends BaseQueryBuilder implements BoostableQueryBuilder<CustomScoreQueryBuilder>
{
	private FunctionScoreQueryBuilder _delegate;

	public CustomScoreQueryBuilder(FunctionScoreQueryBuilder functionScoreQuery)
	{
		_delegate = functionScoreQuery;
	}
	
	public CustomScoreQueryBuilder script(String script, String lang, Map<String, Object> params)
	{
		ScoreFunctionBuilder scoreFunctionBuilder = ScoreFunctionBuilders.scriptFunction(script, lang, params); 
		return new CustomScoreQueryBuilder(_delegate.add(scoreFunctionBuilder));		
	}
	
	public CustomScoreQueryBuilder add(FilterBuilder filter,
			ScoreFunctionBuilder scoreFunctionBuilder) {
		_delegate.add(filter, scoreFunctionBuilder);
		return this;
	}

	public CustomScoreQueryBuilder add(
			ScoreFunctionBuilder scoreFunctionBuilder) {
		_delegate.add(scoreFunctionBuilder);
		return this;
	}

	public CustomScoreQueryBuilder boost(float boost) {
		_delegate.boost(boost);
		return this;
	}

	public CustomScoreQueryBuilder boostMode(CombineFunction combineFunction) {
		_delegate.boostMode(combineFunction);
		return this;
	}

	public CustomScoreQueryBuilder boostMode(String boostMode) {
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

	public CustomScoreQueryBuilder maxBoost(float maxBoost) {
		_delegate.maxBoost(maxBoost);
		return this;
	}

	public CustomScoreQueryBuilder scoreMode(String scoreMode) {
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
