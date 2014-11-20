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

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.io.MapWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.elasticsearch.hadoop.mr.EsInputFormat;

import com.mongodb.hadoop.util.MongoConfigUtil;

@SuppressWarnings("rawtypes")
public class InfiniteEsInputFormat extends InputFormat {
	
	//DEBUG
	public final static boolean LOCAL_DEBUG_MODE = false;
	
	private EsInputFormat _delegate;

	@Override
	public RecordReader createRecordReader(InputSplit arg0,
			TaskAttemptContext arg1) throws IOException, InterruptedException {
		
		if (null == _delegate) {
			_delegate = new EsInputFormat();
		}
		int limitOverride = MongoConfigUtil.getLimit(arg1.getConfiguration());
		if (limitOverride > 0) {
			InfiniteEsRecordReader.MAX_RECORDS = limitOverride;
		}
		return new InfiniteEsRecordReader(arg1, _delegate.createRecordReader(arg0, arg1));		
	}//TESTED (by hand)

	@Override
	public List getSplits(JobContext arg0) throws IOException,
			InterruptedException {
		
		LinkedList<InputSplit> fullList = new LinkedList<InputSplit>();

		String indexes[] = arg0.getConfiguration().get("es.resource").split("\\s*,,\\s*");
		for (String index: indexes) {
			_delegate = new EsInputFormat(); // create a new input format for each object
			
			arg0.getConfiguration().set("es.resource.read", index.replace(" ", "%20")); // (spaces in types cause problems)
						
			@SuppressWarnings("unchecked")
			List<InputSplit> list = _delegate.getSplits(arg0);
			if (LOCAL_DEBUG_MODE) enableInputSplitDebugMode(list);

			fullList.addAll(list);
		}		
		return fullList;
	}//TESTED (by hand)
	
	// (this is pretty involved .. 1) enable this, 2) enable the proxy settings in the custom hadoop launcher 
	// 3) use (eg) fiddler to create a proxy pointing the nodes to localhost:9200, 4) tunnel localhost:9200 
	// (in theory should just be able to do 3) but pointing to localhost:4x92, but fiddler wasn't co-operating for some reason)
	private static void enableInputSplitDebugMode(List<InputSplit> list) {
		for (InputSplit is: list) {
			try {
				Class<?> c = is.getClass();
				
				Field nodeIp = c.getDeclaredField("nodeIp");
				nodeIp.setAccessible(true);
				nodeIp.set(is, "127.0.0.1");
			}
			catch (Exception e) {
				//DEBUG
				//e.printStackTrace();
			}
		}				
	}//TESTED (by hand)
	
	public static class InfiniteEsRecordReader extends RecordReader {

		public static int MAX_RECORDS = Integer.MAX_VALUE;
		
		protected int recordsRead = 0;
		protected RecordReader _delegate;
		
		public InfiniteEsRecordReader(TaskAttemptContext arg1, RecordReader createRecordReader) {
			// This is needed for the debug mode to grab the logging:
			log.info(arg1.getJobName() + ": (New elasticsearch split)");
			
			_delegate = createRecordReader;
		}

		@Override
		public void close() throws IOException {
			_delegate.close();
			
		}

		@Override
		public Object getCurrentKey() throws IOException, InterruptedException {			
			Object o = _delegate.getCurrentKey();
			return o;
		}

		@Override
		public Object getCurrentValue() throws IOException,
				InterruptedException {
			// Add _id to object for compatibility with existing code
			MapWritable w = (MapWritable) _delegate.getCurrentValue();
			w.put(new Text("_id"), (Writable)_delegate.getCurrentKey());
			
			Object o = new BasicDBObjectWrappingEsWritable(w);
			return o;
		}

		@Override
		public float getProgress() throws IOException, InterruptedException {
			return _delegate.getProgress();
		}

		@Override
		public void initialize(InputSplit arg0, TaskAttemptContext arg1)
				throws IOException, InterruptedException {
			_delegate.initialize(arg0, arg1);			
		}

		@Override
		public boolean nextKeyValue() throws IOException, InterruptedException {
			return (recordsRead++ < MAX_RECORDS) && _delegate.nextKeyValue();
		}		
		private static final Log log = LogFactory.getLog( InfiniteEsRecordReader.class );
	}//TESTED (by hand)
	
}
