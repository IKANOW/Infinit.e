package org.apache.lucene.queryParser;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.util.Version;

public class CrossVersionQueryParser extends org.apache.lucene.queryParser.QueryParser {
	
	public CrossVersionQueryParser(Version v, String s, StandardAnalyzer analyzer) {
		super(v, s, analyzer);
	}
	
}
