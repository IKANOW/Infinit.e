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

package com.ikanow.infinit.e.hadoop.processing;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.util.ToolRunner;
import org.bson.BSONObject;



import com.ikanow.infinit.e.data_model.store.MongoDbUtil;
//import com.ikanow.infinit.e.data_model.store.document.CompressedFullTextPojo;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
//import com.ikanow.infinit.e.data_model.store.feature.association.AssociationFeaturePojo;
//import com.ikanow.infinit.e.data_model.store.feature.entity.EntityFeaturePojo;
import com.ikanow.infinit.e.hadoop.configuration.InfiniteProcessingEngineConfig;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.hadoop.io.BSONWritable;
import com.mongodb.hadoop.util.MongoTool;

public class InfiniteProcessingEngine extends MongoTool {

	// You can use this logger to output to the Plugin manager GUI - run in debug mode and "info" messages will appear
	// (in standalone Hadoop mode, error messages will also appear)
	static Logger _logger = Logger.getLogger("com.ikanow.infinit.e.hadoop.processing.InfiniteProcessingEngine");
	static boolean _logMessages = true; // (set to false for improved performance)
	
	// MAPPER
	
	//TODO: pick the output key/value classes
	// (if they are different to those of the reducer then you must use $mapper_key_class and $mapper_value_class overrides in the query
	public static class InfiniteMapper extends Mapper<Object, BSONObject, Text, BSONWritable> {
		
		// State:
		InfiniteProcessingEngineConfig _config;
		
		// Folder if yout want it:
		InfiniteFolder _folder = null;
		
		@Override
		protected void setup(Context context) {
			String args = context.getConfiguration().get("arguments");
			if (null != args) {
				_config = InfiniteProcessingEngineConfig.fromApi(args, InfiniteProcessingEngineConfig.class);
			}
			else {
				_config = new InfiniteProcessingEngineConfig(); // (all defaults)
			}
			if (_logMessages) _logger.info("CONFIGURATION = " + _config.toApi());
			
			//TODO if using folder
			_folder = new  InfiniteFolder();
			
			synchronized (InfiniteProcessingEngine.class) {
				//TODO: anything that needs to be synchronized across multiple mappers/reducers in a JVM
			}
		}
		@Override
		public void map( Object key, BSONObject value, Context context ) throws IOException, InterruptedException
		{
			// 1] Get object
			
			//TODO: optionally serialize into one of the pojos, depending on input type
			// (this is a trade-off between performance and maintainability, you can
			//  also just access "value" directly)
			//DocumentPojo doc = DocumentPojo.fromDb( (BasicDBObject) value, DocumentPojo.class );
			//EntityFeaturePojo ent = EntityFeaturePojo.fromDb( (BasicDBObject) value, EntityFeaturePojo.class );
			//AssociationFeaturePojo assoc = AssociationFeaturePojo.fromDb( (BasicDBObject) value, AssociationFeaturePojo.class );
			//CompressedFullTextPojo fullText = CompressedFullTextPojo.fromDb( (BasicDBObject) value, CompressedFullTextPojo.class );
			
			// (or direct access as described above)
			BasicDBObject record = (BasicDBObject)value;
			String docUrl = record.getString(DocumentPojo.url_, null);
			String docTitle = record.getString(DocumentPojo.title_, null);
			BasicDBList tags = (BasicDBList) record.get(DocumentPojo.tags_);
			
			// 2] Processing and output/folding 
			
			//TODO: now do whatever processing you want to and emit (as many times as you want) as follows:
			// example, count tags
			if (null != tags) {
				for (Object tagObj: tags) {
					if (tagObj instanceof String) {
						BSONWritable outVal = new BSONWritable();
						outVal.put(DocumentPojo.url_, docUrl);						
						outVal.put(DocumentPojo.title_, docTitle);	
						if (_config.fold) { // fold, can be faster for simpler aggregations
							_folder.fold((String) tagObj, outVal, context);
						}
						else {
							// Write don't fold, default:
							context.write(new Text((String) tagObj), outVal);
						}						
						
						//DEBUG:
						if (_logMessages) _logger.info("MAPOUT " + tagObj + ": " + MongoDbUtil.convert(outVal).toString());						
					}
				}//(end loop over tags)
			}//(end if tags specificed)
			
		}
		@Override
		protected void cleanup(Context context) throws IOException, InterruptedException {
			//TODO: 1] emit folder cleanup if using 
			if (null != _folder) {
				_folder.cleanup(context);
			}
			
			// 2] Any user specific cleanup
			// (none)
		}
		
	}
	// FOLDER OPTIONS
	
	// A folder is a convenient utility for simple statistical operations
	// it lets you "reduce" on the fly within a mapper, so the minimal number of objects are
	// exported to the combiner/reducer
	
	public static class InfiniteFolder {
		
		protected HashMap<String, BSONWritable> _folderState = new HashMap<String, BSONWritable>();
		
		public void fold(String key, BSONWritable value, @SuppressWarnings("rawtypes") org.apache.hadoop.mapreduce.Mapper.Context context) throws IOException, InterruptedException {
			
			// 1] Get current state, or create if not there
			
			BSONWritable currVal = _folderState.get(key);
			if (null == currVal) {
				currVal = new BSONWritable();
				_folderState.put(key, currVal);
			}
			
			// 2] Processing logic
						
			//TODO: write your logic in here, the key/val can be anything you want, since you control the map code also
			// Eg: count the docs
			Integer currDocs = (Integer) currVal.get("count");
			if (null == currDocs) {
				currDocs = 0;
			}
			currDocs++;
			currVal.put("count", currDocs);
					
			//DEBUG:
			if (_logMessages) _logger.info("FOLD_IN " + key + ": " + MongoDbUtil.convert(value) + "->" + MongoDbUtil.convert(currVal).toString());						
						
			// 3] Emit if state getting too large
			//TODO eg:
			if (_folderState.size() > 1000) {
				cleanup(context);
			}
			
			//TODO: if you are worried about the _folderState getting too large you can check its size here
			// and emit (hence passing "context" in)
		}
		
		@SuppressWarnings("unchecked")
		public void cleanup(@SuppressWarnings("rawtypes") org.apache.hadoop.mapreduce.Mapper.Context context) throws IOException, InterruptedException {
			//TODO: ensure the classes here are correct
			for (Map.Entry<String, BSONWritable> it: _folderState.entrySet()) {
				context.write(new Text(it.getKey()), it.getValue());
				
				//DEBUG:
				if (_logMessages) _logger.info("FOLD_OUT " + it.getKey() + ": " + MongoDbUtil.convert(it.getValue()).toString());						
			}
			_folderState.clear();
		}
	}	
	
	// COMBINER OPTIONS
	
	// There are 4 options:
	// 1) Use no combiner - configure this from the API
	// 2) Have a combiner that just passes data directly through - see NullCombiner
	// 3) Use the reducer as a combiner - configure this from the API
	// 4) Have a custom combiner - see InfiniteCustomCombiner
	
	//TODO need to ensure these key/value classes are correct (input and output)
	public static class InfiniteNullCombiner extends Reducer<Text, BSONWritable, Text, BSONWritable> 
	{
		public void reduce( Text key, Iterable<BSONWritable> values, Context context )
		throws IOException, InterruptedException
		{
			for (BSONWritable value: values) {
				context.write(key, value);
			}
		}
	}
	
	//TODO need to ensure these key/value classes are correct (input and output)
	public static class InfiniteCustomCombiner extends Reducer<Text, BSONWritable, Text, BSONWritable> 
	{
		public void reduce( Text key, Iterable<BSONWritable> values, Context context )
		throws IOException, InterruptedException
		{
			// Do whatever processing you want to and output the result, eg:
			//TODO job specific processing
			// eg count the docs for each tag
			int numDocs = 0;
			for (BSONWritable value: values) {
				
				if (!value.containsField("count")) { // (if directly from the mapper)
					//DEBUG
					if (_logMessages && (0 == numDocs)) _logger.info("COMBINE_IN_FROM_MAPPER, eg " + key + ": " + MongoDbUtil.convert(value));
					numDocs++;
				}
				else {
					//DEBUG
					if (_logMessages) _logger.info("COMBINE_IN_FROM_FOLDER " + key + ": " + MongoDbUtil.convert(value));
					
					Integer docs = (Integer) value.get("count"); // (if from the folder)
					numDocs += docs;
				}
			}
			BSONWritable outVal = new BSONWritable();
			outVal.put("count", numDocs);
			context.write(key, outVal);

			//DEBUG
			if (_logMessages) _logger.info("COMBINE_OUT " + key + ": " + MongoDbUtil.convert(outVal));
		}
	}
		
	// REDUCER

	//TODO need to ensure these key/value classes are correct (input and output)
	public static class InfiniteReducer extends Reducer<Text, BSONWritable, Text, BSONWritable> 
	{
		public void reduce( Text key, Iterable<BSONWritable> values, Context context )
		throws IOException, InterruptedException
		{
			// Do whatever processing you want to and output the result, eg:
			//TODO job specific processing
			// eg count the docs for each tag
			int numDocs = 0;
			for (BSONWritable value: values) {				
				if (!value.containsField("count")) { // (if directly from the mapper OR from the null combiner)
					//DEBUG
					if (_logMessages && (0 == numDocs)) _logger.info("REDUCE_IN_FROM_MAPPER, eg " + key + ": " + MongoDbUtil.convert(value));
					
					numDocs++;
				}
				else {
					//DEBUG
					if (_logMessages) _logger.info("REDUCE_IN_FROM_COMBO " + key + ": " + MongoDbUtil.convert(value));
					
					Integer docs = (Integer) value.get("count"); // (if from the custom combiner OR directly from the folder)
					numDocs += docs;
				}
			}
			BSONWritable outVal = new BSONWritable();
			outVal.put("count", numDocs);
			context.write(key, outVal);

			//DEBUG
			if (_logMessages) _logger.info("REDUCE_OUT " + key + ": " + MongoDbUtil.convert(outVal));
		}
	}	
	
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		final int exitCode = ToolRunner.run( new InfiniteProcessingEngine(), args );
		System.exit( exitCode );
	}

}
