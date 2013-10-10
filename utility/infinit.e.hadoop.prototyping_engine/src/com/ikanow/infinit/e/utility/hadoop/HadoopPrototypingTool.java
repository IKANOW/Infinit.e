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
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.util.ToolRunner;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.bson.types.ObjectId;

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

		//////////////////////////////////////////////////////
		
		protected boolean isInitialized() { return (_engine != null); }
		
		public BSONWritable clone() { return new BSONWritable(); }
		
		//////////////////////////////////////////////////////
		
		protected void setupJavascript(String script)
		{
			if (null == _engine) {
				_factory = new ScriptEngineManager();
				_engine = _factory.getEngineByName("JavaScript");	
				try {
					InputStream in1 = HadoopPrototypingTool.class.getResourceAsStream("javascript_utils_part1.js");
					_engine.eval(new Scanner(in1).useDelimiter("\\A").next());
					_engine.eval(script);
					InputStream in2 = HadoopPrototypingTool.class.getResourceAsStream("javascript_utils_part2.js");
					_engine.eval(new Scanner(in2).useDelimiter("\\A").next());
				} catch (ScriptException e) {
					throw new RuntimeException("setupJavascript: " + e.getMessage());
				}
				
			}
		}		
		
		protected BasicBSONList generateOutput()
		{
			JavascriptUtils objFactory = this; // (clone creates BSONWritables)
			BasicBSONList listFactory = new BasicBSONList();
			BasicBSONList outList = new BasicBSONList();
			_engine.put("objFactory", objFactory);
			_engine.put("listFactory", listFactory);
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
			ObjectId inkey = (ObjectId)key;
			_engine.put("_map_input_key", inkey.toString());
			String valueStr = value.toString(); // (these BSON objects are actually DBObjects, hence have a sensible toString())
			_engine.put("_map_input_value", valueStr);
			try {
				((Invocable) _engine).invokeFunction("internal_mapper", (Object[])null);
			} catch (Exception e) {
				//TODO (INF-1891): running on system/sentiment/enron community, get a bunch of "unterminated string literal"
				// fails on: 4db7887ade327f612ca33ce3, 4fe6d7c2e4b0ec981064a0d5, 4fe8648de4b0d96833fa757d, 4fea0e9de4b08cca3fd181d5, 4fea2afce4b0f44772f0f4bb, etc
				// (compare this with SAH js input and fix, then start throwing exception again)
				//Just carry on, this entry has failed for some reason...
				//throw new RuntimeException("map1: " + e.getMessage() + ": " + inkey);
			}
		}
		protected void combine(BSONWritable key, Iterable<BSONWritable> values)
		{
			ArrayList<BasicBSONObject> tmpValues = new ArrayList<BasicBSONObject>();
			for (BSONWritable val: values) {
				tmpValues.add(new BasicBSONObject(val.toMap()));
			}
			_engine.put("_combine_input_key", JSON.serialize(new BasicBSONObject(key.toMap())));
			_engine.put("_combine_input_values", JSON.serialize(tmpValues));
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
			ArrayList<BasicBSONObject> tmpValues = new ArrayList<BasicBSONObject>();
			for (BSONWritable val: values) {
				tmpValues.add(new BasicBSONObject(val.toMap()));
			}
			_engine.put("_reduce_input_key", JSON.serialize(new BasicBSONObject(key.toMap())));
			_engine.put("_reduce_input_values", JSON.serialize(tmpValues));
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
		
		public void map( Object key, BSONObject value, Context context ) throws IOException, InterruptedException
		{
			if (!_javascript.isInitialized()) {
				_javascript.setupJavascript(context.getConfiguration().get("arguments"));
			}
			_javascript.map(key, value);
			
			BasicBSONList out = _javascript.generateOutput();
			for (Object bson: out) {
				BSONWritable bsonObj = (BSONWritable) bson;
				BSONWritable outkey = (BSONWritable) bsonObj.get("key");
				BSONWritable outval = (BSONWritable) bsonObj.get("val");
				context.write(outkey, outval);
			}
		}
	}
	
	public static class JavascriptCombiner extends Reducer<BSONWritable, BSONWritable, BSONWritable, BSONWritable> 
	{
		private JavascriptUtils _javascript = new JavascriptUtils();
		
		public void reduce( BSONWritable key, Iterable<BSONWritable> values, Context context )
		throws IOException, InterruptedException
		{
			if (!_javascript.isInitialized()) {
				_javascript.setupJavascript(context.getConfiguration().get("arguments"));
			}
			_javascript.combine(key, values);

			BasicBSONList out = _javascript.generateOutput();
			for (Object bson: out) {
				BSONWritable bsonObj = (BSONWritable) bson;
				BSONWritable outkey = (BSONWritable) bsonObj.get("key");
				BSONWritable outval = (BSONWritable) bsonObj.get("val");
				context.write(outkey, outval);
			}
		}
	}

	public static class JavascriptReducer extends Reducer<BSONWritable, BSONWritable, BSONWritable, BSONWritable> 
	{
		private JavascriptUtils _javascript = new JavascriptUtils();
		
		public void reduce( BSONWritable key, Iterable<BSONWritable> values, Context context )
		throws IOException, InterruptedException
		{
			if (!_javascript.isInitialized()) {
				_javascript.setupJavascript(context.getConfiguration().get("arguments"));
			}
			_javascript.reduce(key, values);

			BasicBSONList out = _javascript.generateOutput();
			for (Object bson: out) {
				BSONWritable bsonObj = (BSONWritable) bson;
				BSONWritable outkey = (BSONWritable) bsonObj.get("key");
				BSONWritable outval = (BSONWritable) bsonObj.get("val");
				context.write(outkey, outval);
			}
		}
	}
    public static void main( String[] args ) throws Exception{
        final int exitCode = ToolRunner.run( new HadoopPrototypingTool(), args );
        System.exit( exitCode );
    }

}
