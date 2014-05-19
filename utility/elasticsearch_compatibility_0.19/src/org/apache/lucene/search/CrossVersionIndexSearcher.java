package org.apache.lucene.search;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;

public class CrossVersionIndexSearcher {

	protected IndexSearcher _delegate;

	public CrossVersionIndexSearcher(Directory dir) throws IOException {
		_delegate = new IndexSearcher(IndexReader.open(dir));
	}
	
	public IndexReader getIndexReader() {
		return _delegate.getIndexReader();
	}

	public TopDocs search(Query query, int n) throws IOException {
		return _delegate.search(query, n);
	}
	
}
