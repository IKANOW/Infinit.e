package org.apache.lucene.index;

import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Version;

public class CrossVersionIndexWriter {

	protected IndexWriter _delegate;
	
	public CrossVersionIndexWriter(Directory dir, Version version, Analyzer analyzer) throws IOException {
		//IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_30, new StandardAnalyzer(Version.LUCENE_30));		
		IndexWriterConfig config = new IndexWriterConfig(version, analyzer);		
		_delegate = new IndexWriter(dir, config);
	}
	
	public void addSingleAnalyzedUnstoredFieldDocument(String fieldName, String fieldVal) throws IOException {
		Document doc = new Document();
		doc.add(new Field(fieldName, fieldVal, Field.Store.NO, Field.Index.ANALYZED));
		_delegate.addDocument(doc);
	}
	
	public void close() throws IOException {
		_delegate.close();
	}
}
