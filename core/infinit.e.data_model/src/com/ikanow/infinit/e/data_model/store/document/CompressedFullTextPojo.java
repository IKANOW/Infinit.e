/*******************************************************************************
 * Copyright 2012 The Infinit.e Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.ikanow.infinit.e.data_model.store.document;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

import org.bson.types.ObjectId;

import com.ikanow.infinit.e.data_model.store.BaseDbPojo;
import com.mongodb.BasicDBObject;

public class CompressedFullTextPojo extends BaseDbPojo {

	private String url = null;
	final public static String url_ = "url";	
	private String sourceKey; // (index sourceKey:1,url:1)
	final public static String sourceKey_ = "sourceKey";
	private ObjectId communityId;
	final public static String communityId_ = "communityId";
	
	// Main content
	private byte[] gzip_content = null;	
	final public static String gzip_content_ = "gzip_content";
	private Integer gzip_len = null;
	final public static String gzip_len_ = "gzip_len";
	
	// Optional additional content:
	// Original raw content:
	private byte[] gzip_raw_content = null;
	final public static String gzip_raw_content_ = "gzip_raw_content";
	private Integer gzip_raw_len = null;
	final public static String gzip_raw_len_ = "gzip_raw_len";
	// The metadata object
	private byte[] gzip_md_content = null;
	final public static String gzip_md_content_ = "gzip_md_content";
	private Integer gzip_md_len = null;
	final public static String gzip_md_len_ = "gzip_md_len";
	
	public CompressedFullTextPojo() {}
		// (for deserialization)
	
	public CompressedFullTextPojo(String url_, String sourceKey_, ObjectId communityId_, String text_, String rawText_, DocumentPojo docMeta, int nMaxLen) {
		url = url_;
		sourceKey = sourceKey_;
		communityId = communityId_;
		try {
			ByteArrayOutputStream out = null;
			if (null != text_) {
				out = writeStream(text_, nMaxLen);
				gzip_content = out.toByteArray();
				gzip_len = gzip_content.length;
			}
			
			if ((null != rawText_) && (rawText_ != text_)) { // (ie don't bother if they're exactly the same)
				if ((null == gzip_content) || (gzip_len != gzip_content.length) || (!rawText_.equals(text_))) {
					//(if the lengths are the same, worth another check, since it's so expensive) 
					
					out = writeStream(rawText_, nMaxLen);				
					gzip_raw_content = out.toByteArray();
					gzip_raw_len = gzip_raw_content.length;
				}//TESTED
			}
			if (null != docMeta) {
				out = writeStream(docMeta.toDb().toString(), Integer.MAX_VALUE);				
				gzip_md_content = out.toByteArray();
				gzip_md_len = gzip_md_content.length;				
			}//TESTED
			
		} catch (IOException e) {
			//Do nothing (will check on compressedData before writing to monogdb)
		}
	}
	public BasicDBObject getUpdate() {
		BasicDBObject update = new BasicDBObject();
		update.append(url_, url);
		update.append(sourceKey_, sourceKey);
		update.append(communityId_, communityId);
		
		update.append(gzip_content_, gzip_content);
		update.append(gzip_len_, gzip_len);
		if (null != gzip_raw_content) {
			update.append(gzip_raw_content_, gzip_raw_content);
			update.append(gzip_raw_len_, gzip_raw_len);			
		}
		if (null != gzip_md_content) {
			update.append(gzip_md_content_, gzip_md_content);
			update.append(gzip_md_len_, gzip_md_len);			
		}
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
	private static ByteArrayOutputStream writeStream(String content, int nMaxLen) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		GZIPOutputStream gzip = new GZIPOutputStream(out);
		if (content.length() > nMaxLen) {
			gzip.write(content.getBytes(), 0, nMaxLen); // (first 100KB - or whatever's configured - only)
		}
		else {
			gzip.write(content.getBytes());
		}
		gzip.close();
		return out;
	}

	public void setGzip_raw_content(byte[] gzip_raw_content) {
		this.gzip_raw_content = gzip_raw_content;
	}

	public byte[] getGzip_raw_content() {
		return gzip_raw_content;
	}

	public void setGzip_raw_len(Integer gzip_raw_len) {
		this.gzip_raw_len = gzip_raw_len;
	}

	public Integer getGzip_raw_len() {
		return gzip_raw_len;
	}

	public void setGzip_md_content(byte[] gzip_md_content) {
		this.gzip_md_content = gzip_md_content;
	}

	public byte[] getGzip_md_content() {
		return gzip_md_content;
	}

	public void setGzip_md_len(Integer gzip_md_len) {
		this.gzip_md_len = gzip_md_len;
	}

	public Integer getGzip_md_len() {
		return gzip_md_len;
	}

	public void setSourceKey(String sourceKey) {
		this.sourceKey = sourceKey;
	}

	public String getSourceKey() {
		return sourceKey;
	}

	public void setCommunityId(ObjectId communityId) {
		this.communityId = communityId;
	}

	public ObjectId getCommunityId() {
		return communityId;
	}
}
