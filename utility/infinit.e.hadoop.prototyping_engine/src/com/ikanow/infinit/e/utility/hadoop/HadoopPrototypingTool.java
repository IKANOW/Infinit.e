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

//TODO (INF-1666) OSS headers

// This protoptype version is very slow, should run with the actual Rhino script,
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
			_engine.put("_map_input_value", JSON.serialize(value));
			try {
				((Invocable) _engine).invokeFunction("internal_mapper", (Object[])null);
			} catch (ScriptException e) {
				throw new RuntimeException("map: " + e.getMessage());
			} catch (NoSuchMethodException e) {
				throw new RuntimeException("map: " + e.getMessage());
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
