package com.ikanow.infinit.e.data_model.store.document;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

import com.ikanow.infinit.e.data_model.store.BaseDbPojo;
import com.mongodb.BasicDBObject;

public class CompressedFullTextPojo extends BaseDbPojo {

	private String url = null;
	final public static String url_ = "url";
	private byte[] gzip_content = null;	
	final public static String gzip_content_ = "gzip_content";
	private Integer gzip_len = null;
	final public static String gzip_len_ = "gzip_len";
	
	public CompressedFullTextPojo() {}
		// (for deserialization)
	
	public CompressedFullTextPojo(String url_, String text_) {
		url = url_;
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			GZIPOutputStream gzip = new GZIPOutputStream(out);
			if (text_.length() > 100000) {
				gzip.write(text_.getBytes(), 0, 100000); // (first 100KB only)
			}
			else {
				gzip.write(text_.getBytes());
			}
			gzip.close();
			gzip_content = out.toByteArray();
			gzip_len = gzip_content.length;
		} catch (IOException e) {
			//Do nothing (will check on compressedData before writing to monogdb)
		}
	}
	public BasicDBObject getUpdate() {
		BasicDBObject update = new BasicDBObject();
		update.append("url", url);
		update.append("gzip_content", gzip_content);
		update.append("gzip_len", gzip_len);
		return update;
		
	}
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public byte[] getGzip_content() {
		return gzip_content;
	}

	public void setGzip_content(byte[] gzip_content) {
		this.gzip_content = gzip_content;
	}

	public Integer getGzip_len() {
		return gzip_len;
	}

	public void setGzip_len(Integer gzip_len) {
		this.gzip_len = gzip_len;
	}
}
