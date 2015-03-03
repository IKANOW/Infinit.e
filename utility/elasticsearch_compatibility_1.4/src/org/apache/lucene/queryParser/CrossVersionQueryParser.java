package org.apache.lucene.queryParser;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.Version;

public class CrossVersionQueryParser {
	
	org.apache.lucene.queryparser.classic.QueryParser _delegate;
	
	public CrossVersionQueryParser(Version v, String s, StandardAnalyzer analyzer) {
		_delegate = new org.apache.lucene.queryparser.classic.QueryParser(v, s, analyzer);
	}
	public Query parse(String term) throws ParseException {
		return _delegate.parse(term);
	}
}
