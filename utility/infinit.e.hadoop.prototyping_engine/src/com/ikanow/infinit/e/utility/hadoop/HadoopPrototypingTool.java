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
package com.ikanow.infinit.e.utility.hadoop;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Scanner;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.ReduceContext;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.TaskInputOutputContext;
import org.apache.hadoop.util.ToolRunner;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import com.ikanow.infinit.e.data_model.custom.InfiniteMongoConfig;
import com.ikanow.infinit.e.data_model.store.MongoDbUtil;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.hadoop.io.BSONWritable;
import com.mongodb.hadoop.util.MongoTool;
import com.mongodb.util.JSON;

// This protoptype version is very slow, should run with the actual Rhino script engine,
// and there are a number of unnecessary memory copies for simplicity.
// We'll see if this is noticeably slow (it should really only be run on small datasets anyway)

public class HadoopPrototypingTool extends MongoTool {

	public static class JavascriptUtils {
		private ScriptEngineManager _factory = null;
		private ScriptEngine _engine = null;
		private boolean _memoryOptimized = false;
		private TaskInputOutputContext<?, ?, BSONWritable, BSONWritable> _context = null;
		private ReduceContext<BSONWritable, BSONWritable, BSONWritable, BSONWritable> _reduceContext = null;
		
		//////////////////////////////////////////////////////
		
		// (Java side) Interface
		
		protected boolean isInitialized() { return (_engine != null); }
		
		protected boolean isMemoryOptimized() { return _memoryOptimized; }
		
		// (JS side) Interface
		
		public Object clone(Boolean topLevel) { return topLevel ? new BSONWritable() : new BasicDBObject(); }
		
		// "Streaming" output (less efficient, more memory friendly)
		
		public void write(BSONWritable key, BSONWritable value) throws IOException, InterruptedException {
			_context.write(key, value);
		}//TESTED
		
		// "Streaming" input (less efficient, more memory friendly)
		
		public boolean hasNext() throws IOException, InterruptedException {
			boolean hasNext = _reduceContext.getValues().iterator().hasNext();
			return hasNext;
		}//TESTED
		
		public String next() throws IOException, InterruptedException {
			BSONWritable next = _reduceContext.getValues().iterator().next();
			if (null == next) {
				return null;
			}
			else {
				return JSON.serialize(MongoDbUtil.convert(next));
			}
		}//TESTED
		
		//////////////////////////////////////////////////////
		
		protected void setupJavascript(String script,
				TaskInputOutputContext<?, ?, BSONWritable, BSONWritable> context, // for output
				ReduceContext<BSONWritable, BSONWritable, BSONWritable, BSONWritable> reduceContext // for streaming input
				)
		{
			_context = context;
			_reduceContext = reduceContext;
			if (null == _engine) {
				_factory = new ScriptEngineManager();
				_engine = _factory.getEngineByName("JavaScript");	
				try {
					InputStream in1 = HadoopPrototypingTool.class.getResourceAsStream("javascript_utils_part1.js");
					_engine.eval(new Scanner(in1).useDelimiter("\\A").next());
					_engine.eval(script);
					InputStream in2 = HadoopPrototypingTool.class.getResourceAsStream("javascript_utils_part2.js");
					_engine.eval(new Scanner(in2).useDelimiter("\\A").next());
					
					// We've loaded the script so now we can check for memory-optimization mode
					try {
						Object memOptimizationMode = _engine.get("_memoryOptimization");
						if (null != memOptimizationMode) {
							if (memOptimizationMode instanceof Boolean) {
								_memoryOptimized = (Boolean)memOptimizationMode;
							}
							else {
								_memoryOptimized = Boolean.parseBoolean(memOptimizationMode.toString());
							}
						}//TESTED
						
						if (_memoryOptimized) { 
							// (do it this way because in the future might want to separate these 2 out)
							_engine.put("_inContext", this);
							_engine.put("_outContext", this);
							setupOutput();
						}
					}
					catch (Exception e) {} // we don't care about errors for this clause specifically
					
					// Now overwrite it to ensure it's always present
					_engine.put("_memoryOptimization", _memoryOptimized);
					
				} catch (ScriptException e) {
					throw new RuntimeException("setupJavascript: " + e.getMessage());
				}
				
			}
		}		
		
		protected void setupOutput() {
			JavascriptUtils objFactory = this; // (clone creates BSONWritables)
			BasicBSONList listFactory = new BasicBSONList();
			_engine.put("objFactory", objFactory);
			_engine.put("listFactory", listFactory);
		}
		
		protected BasicBSONList generateOutput()
		{			
			setupOutput();
			BasicBSONList outList = new BasicBSONList();
			_engine.put("outList", outList);			
			
			try {
				_engine.eval("s1(_emit_list); ");
			} catch (ScriptException e) {
				throw new RuntimeException("generateOutput: " + e.getMessage());
			}
			return outList;			
		}
		
		//////////////////////////////////////////////////////
		
		protected void map(Object key, BSONObject value)
		{
			_engine.put("_map_input_key", key.toString());
			String valueStr = value.toString(); // (these BSON objects are actually DBObjects, hence have a sensible toString())
			_engine.put("_map_input_value", valueStr);
			try {
				((Invocable) _engine).invokeFunction("internal_mapper", (Object[])null);
			} catch (Exception e) {
				//e.printStackTrace();
				
				//TODO (INF-1891): running on system/sentiment/enron community, get a bunch of "unterminated string literal"
				// fails on: 4db7887ade327f612ca33ce3, 4fe6d7c2e4b0ec981064a0d5, 4fe8648de4b0d96833fa757d, 4fea0e9de4b08cca3fd181d5, 4fea2afce4b0f44772f0f4bb, etc
				// (compare this with SAH js input and fix, then start throwing exception again)
				//Just carry on, this entry has failed for some reason...
				//throw new RuntimeException("map1: " + e.getMessage() + ": " + inkey);
			}
		}
		protected void combine(BSONWritable key, Iterable<BSONWritable> values)
		{
			_engine.put("_combine_input_key", JSON.serialize(new BasicBSONObject(key.toMap())));
			if (!this.isMemoryOptimized()) {
				
				ArrayList<BasicBSONObject> tmpValues = new ArrayList<BasicBSONObject>();
				for (BSONWritable val: values) {
					tmpValues.add(new BasicBSONObject(val.toMap()));
				}
				_engine.put("_combine_input_values", JSON.serialize(tmpValues));
			}
			try {
				((Invocable) _engine).invokeFunction("internal_combiner", (Object[])null);
			} catch (ScriptException e) {
				if ((null != e.getMessage()) && e.getMessage().contains("ReferenceError: \"combine\" is not defined")) {
					reduce(key, values);
				}
				else {
					throw new RuntimeException("map: " + e.getMessage());
				}
			} catch (NoSuchMethodException e) {
				reduce(key, values);
			}
		}
		protected void reduce(BSONWritable key, Iterable<BSONWritable> values)
		{
			_engine.put("_reduce_input_key", JSON.serialize(new BasicBSONObject(key.toMap())));
			if (!this.isMemoryOptimized()) {
				ArrayList<BasicBSONObject> tmpValues = new ArrayList<BasicBSONObject>();
				for (BSONWritable val: values) {
					tmpValues.add(new BasicBSONObject(val.toMap()));
				}
				_engine.put("_reduce_input_values", JSON.serialize(tmpValues));
			}
			try {
				((Invocable) _engine).invokeFunction("internal_reducer", (Object[])null);
			} catch (ScriptException e) {
				throw new RuntimeException("map: " + e.getMessage());
			} catch (NoSuchMethodException e) {
				throw new RuntimeException("map: " + e.getMessage());
			}			
		}
	}
	
	public static class JavascriptMapper extends Mapper<Object, BSONObject, BSONWritable, BSONWritable> {
		private JavascriptUtils _javascript = new JavascriptUtils();

		@Override
		public void setup(Context context) {
			_javascript.setupJavascript(context.getConfiguration().get("arguments"), context, null);
			_javascript._engine.put("_query", context.getConfiguration().get("mongo.input.query"));
			
			// Set up cache if one is specified
			InfiniteMongoConfig config = new InfiniteMongoConfig(context.getConfiguration());
			BasicDBList caches = config.getCacheList();
			if ((null != caches) && !caches.isEmpty()) {				
				try {
					CacheUtils.addJSONCachesToEngine(caches, _javascript._engine, null, (config.getLimit() > 0));
				} catch (Exception e) {
					throw new RuntimeException("Error setting up caches: " + caches);
				}
			}
		}
		
		@Override
		public void map( Object key, BSONObject value, Context context ) throws IOException, InterruptedException
		{
			_javascript.map(key, value);
			
			if (!_javascript.isMemoryOptimized()) {
				BasicBSONList out = _javascript.generateOutput();
				for (Object bson: out) {
					BSONWritable bsonObj = (BSONWritable) bson;
					BSONWritable outkey = (BSONWritable) bsonObj.get("key");
					BSONWritable outval = (BSONWritable) bsonObj.get("val");
					if (null == outkey) {
						throw new IOException("Map: Can't output a null key from " + value.get("_id"));					
					}
					if (null == outval) {					
						throw new IOException("Map: Can't output a null value, key: " + MongoDbUtil.convert(outkey) + " from " + value.get("_id"));
					}
					context.write(outkey, outval);
				}
			}
		}
	}
	
	public static class JavascriptCombiner extends Reducer<BSONWritable, BSONWritable, BSONWritable, BSONWritable> 
	{
		private JavascriptUtils _javascript = new JavascriptUtils();
		
		@Override
		public void setup(Context context) {
			_javascript.setupJavascript(context.getConfiguration().get("arguments"), context, context);
			_javascript._engine.put("_query", context.getConfiguration().get("mongo.input.query"));

			// Set up cache if one is specified
			InfiniteMongoConfig config = new InfiniteMongoConfig(context.getConfiguration());
			BasicDBList caches = config.getCacheList();
			if ((null != caches) && !caches.isEmpty()) {				
				try {
					CacheUtils.addJSONCachesToEngine(caches, _javascript._engine, null, (config.getLimit() > 0));
				} catch (Exception e) {
					throw new RuntimeException("Error setting up caches: " + caches);
				}
			}
		}
		
		public void reduce( BSONWritable key, Iterable<BSONWritable> values, Context context )
		throws IOException, InterruptedException
		{
			_javascript.combine(key, values);

			if (!_javascript.isMemoryOptimized()) {
				BasicBSONList out = _javascript.generateOutput();
				for (Object bson: out) {
					BSONWritable bsonObj = (BSONWritable) bson;
					BSONWritable outkey = (BSONWritable) bsonObj.get("key");
					BSONWritable outval = (BSONWritable) bsonObj.get("val");
					if (null == outkey) {
						throw new IOException("Combine: Can't output a null key from " + key);					
					}
					if (null == outval) {					
						throw new IOException("Combine: Can't output a null value, key: " + MongoDbUtil.convert(outkey) + " from " + key);
					}
					context.write(outkey, outval);
				}
			}
		}
	}

	public static class JavascriptReducer extends Reducer<BSONWritable, BSONWritable, BSONWritable, BSONWritable> 
	{
		private JavascriptUtils _javascript = new JavascriptUtils();
		
		@Override
		public void setup(Context context) {
			_javascript.setupJavascript(context.getConfiguration().get("arguments"), context, context);
			_javascript._engine.put("_query", context.getConfiguration().get("mongo.input.query"));

			// Set up cache if one is specified
			InfiniteMongoConfig config = new InfiniteMongoConfig(context.getConfiguration());
			BasicDBList caches = config.getCacheList();
			if ((null != caches) && !caches.isEmpty()) {				
				try {
					CacheUtils.addJSONCachesToEngine(caches, _javascript._engine, null, (config.getLimit() > 0));
				} catch (Exception e) {
					throw new RuntimeException("Error setting up caches: " + caches);
				}
			}
		}
		
		public void reduce( BSONWritable key, Iterable<BSONWritable> values, Context context )
		throws IOException, InterruptedException
		{
			_javascript.reduce(key, values);
			
			if (!_javascript.isMemoryOptimized()) {
				BasicBSONList out = _javascript.generateOutput();
				for (Object bson: out) {
					BSONWritable bsonObj = (BSONWritable) bson;
					BSONWritable outkey = (BSONWritable) bsonObj.get("key");
					BSONWritable outval = (BSONWritable) bsonObj.get("val");
					if (null == outkey) {
						throw new IOException("Reduce: Can't output a null key from " + key);					
					}
					if (null == outval) {					
						throw new IOException("Reduce: Can't output a null value, key: " + MongoDbUtil.convert(outkey) + " from " + key);
					}
					context.write(outkey, outval);
				}
			}
		}
	}
    public static void main( String[] args ) throws Exception{
        final int exitCode = ToolRunner.run( new HadoopPrototypingTool(), args );
        System.exit( exitCode );
    }

}
