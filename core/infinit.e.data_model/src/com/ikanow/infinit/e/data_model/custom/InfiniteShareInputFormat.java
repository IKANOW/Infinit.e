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
package com.ikanow.infinit.e.data_model.custom;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import net.sf.jazzlib.GridFSZipFile;

import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.bson.BSONCallback;
import org.bson.BSONDecoder;
import org.bson.BSONEncoder;
import org.bson.BSONObject;
import org.bson.BasicBSONCallback;
import org.bson.BasicBSONDecoder;
import org.bson.BasicBSONEncoder;
import org.bson.types.ObjectId;

import com.ikanow.infinit.e.data_model.api.ApiManager;
import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.MongoDbManager;
import com.ikanow.infinit.e.data_model.store.config.source.SourceFileConfigPojo;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.ikanow.infinit.e.data_model.store.social.sharing.SharePojo;
import com.ikanow.utility.GridFSRandomAccessFile;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;

public class InfiniteShareInputFormat extends InputFormat<Object, BSONObject> {

	@Override
	public RecordReader<Object, BSONObject> createRecordReader(InputSplit inputSplit,
			TaskAttemptContext context) throws IOException, InterruptedException {
		InfiniteShareInputReader reader = new InfiniteShareInputReader();
		try {
			reader.initialize(inputSplit, context);
		} 
		catch (InterruptedException e) {
			throw new IOException(e);
		}
		return reader;
	}

	@Override
	public List<InputSplit> getSplits(JobContext job) throws IOException,
			InterruptedException {
				
		// Test mode needs to restrict the docs also, otherwise things can get out of hand...
		int debugLimit = job.getConfiguration().getInt("mongo.input.limit", Integer.MAX_VALUE);
		if (debugLimit <= 0) { // (just not set)
			debugLimit = Integer.MAX_VALUE;
		}		
		
		// Get the already authenticated list of share ids
		String oidStrs[] = job.getConfiguration().getStrings("mapred.input.dir");
		List<InputSplit> splits = new LinkedList<InputSplit>();
		int numSplits = 0;
		
		String sourceStr = job.getConfiguration().get("mongo.input.query");
		SourcePojo source = ApiManager.mapFromApi(sourceStr, SourcePojo.class, null);
		SourceFileConfigPojo fileConfig = source.getFileConfig();
		Pattern pathInclude = null;
		Pattern pathExclude = null;
		if (null != fileConfig.pathInclude) {
			pathInclude = Pattern.compile(fileConfig.pathInclude);
		}
		if (null != fileConfig.pathExclude) {
			pathExclude = Pattern.compile(fileConfig.pathExclude);
		}
		
		for (String oidStr: oidStrs) {
			try {
				BasicDBObject query = new BasicDBObject(SharePojo._id_, new ObjectId(oidStr));
				BasicDBObject fields = new BasicDBObject(SharePojo.binaryId_, 1);
				fields.put(SharePojo.title_, 1);
				SharePojo share = SharePojo.fromDb(DbManager.getSocial().getShare().findOne(query, fields), SharePojo.class);
				
				if ((null != share) && (null != share.getBinaryId())) {
					GridFSRandomAccessFile file = new GridFSRandomAccessFile(MongoDbManager.getSocial().getShareBinary(), share.getBinaryId());					
					GridFSZipFile zipView = new GridFSZipFile(share.getTitle(), file);					
					@SuppressWarnings("unchecked")
					Enumeration<net.sf.jazzlib.ZipEntry> entries = zipView.entries();
					while (entries.hasMoreElements()) {
						net.sf.jazzlib.ZipEntry zipInfo = entries.nextElement();
						
						if (zipInfo.isDirectory()) {
							continue;
						}							
						
						if (null != pathInclude) {
							if (!pathInclude.matcher(zipInfo.getName()).matches()) {
								continue;
							}
						}
						if (null != pathExclude) {
							if (pathExclude.matcher(zipInfo.getName()).matches()) {
								continue;
							}
						}						
						
						InfiniteShareInputSplit split = new InfiniteShareInputSplit(share.get_id(), share.getBinaryId(), zipInfo.getName(), zipInfo.getSize(), zipInfo.getTime());
						splits.add(split);
						if (++numSplits >= debugLimit) {
							break;
						}
						//DEBUG
						//System.out.println("ADD NEW SPLIT: " + share.get_id() + " , " + share.getBinaryId() + " , " + zipInfo.getName() + " , " + zipInfo.getSize());
					}
				}
			}
			catch (Exception e) {} // (this would be an internal logic error
		}
		return splits;
	}

	
	public static class InfiniteShareInputSplit extends InputSplit implements Writable {
		
		protected ObjectId _shareId;
		protected ObjectId _fileId;
		protected String _title;
		protected long _size;
		protected long _time;
		
		public ObjectId get_shareId() {
			return _shareId;
		}

		public ObjectId get_fileId() {
			return _fileId;
		}

		public String get_title() {
			return _title;
		}

		public long get_size() {
			return _size;
		}

		public long get_time() {
			return _time;
		}
		
		public void set_size(long _size) {
			this._size = _size;
		}

		public InfiniteShareInputSplit() {}
		
		public InfiniteShareInputSplit(ObjectId shareId, ObjectId fileId, String title, long size, long time) {
			_fileId = fileId;
			_shareId = shareId;
			_title = title;	
			_size = size;
			_time = time;
		}
		
	    /**
	     * Serialize the Split instance
	     */
	    public void write( final DataOutput out ) throws IOException{
	        BSONEncoder enc =  new BasicBSONEncoder();

	        BSONObject spec = BasicDBObjectBuilder.start().
	                                               add( "_fileId", _fileId ).
	                                               add( "_shareId", _shareId ).
	                                               add( "_size", _size ).
	                                               add( "_time", _time ).
	                                               add( "_title", _title ).get();

	        byte[] buf = enc.encode( spec );

	        out.write( buf );
	    }

	    public void readFields( DataInput in ) throws IOException{
	        BSONDecoder dec =  new BasicBSONDecoder();
	        BSONCallback cb = new BasicBSONCallback();
	        BSONObject spec;
	        // Read the BSON length from the start of the record
	        byte[] l = new byte[4];
	        try {
	            in.readFully( l );
	            int dataLen = org.bson.io.Bits.readInt( l );
	            byte[] data = new byte[dataLen + 4];
	            System.arraycopy( l, 0, data, 0, 4 );
	            in.readFully( data, 4, dataLen - 4 );
	            dec.decode( data, cb );
	            spec = (BSONObject) cb.get();
	        }
	        catch ( Exception e ) {
	            spec = new BasicDBObject();
	        }
	        _fileId = (ObjectId) spec.get("_fileId");
	        _shareId = (ObjectId) spec.get("_shareId");
	        _title = (String) spec.get("_title");
	        _size = (Long) spec.get("_size");
	        _time = (Long) spec.get("_time");
	    }

		@Override
		public long getLength() throws IOException, InterruptedException {
			return _size;
		}

		@Override
		public String[] getLocations() throws IOException, InterruptedException {
			// (No concept of rack locality here)
			return new String[0];
		}
	}
}
