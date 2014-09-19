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

import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketPermission;
import java.security.AllPermission;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Policy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.PropertyPermission;
import java.util.Scanner;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.ReduceContext;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.TaskInputOutputContext;
import org.apache.hadoop.util.ToolRunner;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import com.ikanow.infinit.e.data_model.custom.InfiniteMongoConfig;
import com.ikanow.infinit.e.data_model.custom.InfiniteMongoConfigUtil;
import com.ikanow.infinit.e.data_model.store.MongoDbUtil;
import com.ikanow.infinit.e.data_model.utils.IkanowSecurityManager;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.hadoop.io.BSONWritable;
import com.mongodb.hadoop.util.MongoTool;
import com.mongodb.util.JSON;

// This protoptype version is very slow, should run with the actual Rhino script engine,
// and there are a number of unnecessary memory copies for simplicity.
// We'll see if this is noticeably slow (it should really only be run on small datasets anyway)

public class HadoopPrototypingTool extends MongoTool {

    private static final Log log = LogFactory.getLog(HadoopPrototypingTool.class);
    
	public static class JavascriptUtils {
		private ScriptEngineManager _factory = null;
		private ScriptEngine _engine = null;
		private boolean _memoryOptimized = false;
		private TaskInputOutputContext<?, ?, BSONWritable, BSONWritable> _context = null;
		private ReduceContext<BSONWritable, BSONWritable, BSONWritable, BSONWritable> _reduceContext = null;
		
		private IkanowSecurityManager _secManager;		
		
		//////////////////////////////////////////////////////
		
		// (Java side) Interface
		
		protected boolean isInitialized() { return (_engine != null); }
		
		protected boolean isMemoryOptimized() { return _memoryOptimized; }
		
		// (JS side) Interface
		
		public Object clone(Boolean topLevel) { return topLevel ? new BSONWritable() : new BasicDBObject(); }
		
		// "Streaming" output (less efficient, more memory friendly)
		
		public void write(BSONWritable key, BSONWritable value) throws IOException, InterruptedException {
			try {
				_secManager.setSecureFlag(false);
				_context.write(key, value);
			}
			finally {
				_secManager.setSecureFlag(true);				
			}
		}//TESTED
		
		// "Streaming" input (less efficient, more memory friendly)
		
		public boolean hasNext() throws IOException, InterruptedException {
			try {
				_secManager.setSecureFlag(false);
				boolean hasNext = _reduceContext.getValues().iterator().hasNext();
				return hasNext;
			}
			finally {
				_secManager.setSecureFlag(true);				
			}
		}//TESTED
		
		public String next() throws IOException, InterruptedException {
			try {
				_secManager.setSecureFlag(false);
				BSONWritable next = _reduceContext.getValues().iterator().next();
				if (null == next) {
					return null;
				}
				else {
					return JSON.serialize(MongoDbUtil.convert(next));
				}
			}
			finally {
				_secManager.setSecureFlag(true);				
			}
		}//TESTED
		
		//////////////////////////////////////////////////////
		
		protected void setupJavascript(String script,
				TaskInputOutputContext<?, ?, BSONWritable, BSONWritable> context, // for output
				ReduceContext<BSONWritable, BSONWritable, BSONWritable, BSONWritable> reduceContext // for streaming input
				)
		{	
			Policy.setPolicy(new MinimalPolicy());
			_secManager = new IkanowSecurityManager(context.getConfiguration().getBoolean(InfiniteMongoConfigUtil.IS_ADMIN, false));
			
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
					
				} 
				catch (ScriptException e) {
					throw new RuntimeException("setupJavascript: " + e.getMessage(), e);
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
				_secManager.setSecureFlag(true);
				((Invocable) _engine).invokeFunction("internal_mapper", (Object[])null);
			}
			catch (Exception e) {
				Object internalError = _engine.get("_internalError");
				if ((internalError instanceof Boolean) && ((Boolean)internalError)) {
					log.warn("Internal error on doc: " + key);
				}
				else {
					throw new RuntimeException("map1: " + e.getMessage(), e);
				}
			}
			finally {
				_secManager.setSecureFlag(false);				
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
				_secManager.setSecureFlag(true);
				((Invocable) _engine).invokeFunction("internal_combiner", (Object[])null);
			} 
			catch (ScriptException e) {
				if ((null != e.getMessage()) && e.getMessage().contains("ReferenceError: \"combine\" is not defined")) {
					_secManager.setSecureFlag(false); // (set again in the reducer)
					reduce(key, values);
				}
				else {
					throw new RuntimeException("combine: " + e.getMessage(), e);
				}
			}
			catch (NoSuchMethodException e) {
				_secManager.setSecureFlag(false); // (set again in the reducer)
				reduce(key, values);
			}
			finally {
				_secManager.setSecureFlag(false);				
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
				_secManager.setSecureFlag(true);
				((Invocable) _engine).invokeFunction("internal_reducer", (Object[])null);
			}
			catch (ScriptException e) {
				throw new RuntimeException("reduce: " + e.getMessage(), e);
			}
			catch (NoSuchMethodException e) {
				throw new RuntimeException("reduce: " + e.getMessage(), e);
			}			
			finally {
				_secManager.setSecureFlag(false);				
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
					CacheUtils.addJSONCachesToEngine(caches, _javascript._engine, _javascript._secManager, (config.getLimit() > 0));
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
					CacheUtils.addJSONCachesToEngine(caches, _javascript._engine, _javascript._secManager, (config.getLimit() > 0));
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
					CacheUtils.addJSONCachesToEngine(caches, _javascript._engine, _javascript._secManager, (config.getLimit() > 0));
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
    
	//////////////////////////////////////////////////////

    // Specify security policy:
    
    public static class MinimalPolicy extends Policy {

        private static PermissionCollection perms;

        public MinimalPolicy() {
            super();
            if (perms == null) {
                perms = new MyPermissionCollection();
                addPermissions();
            }
        }

        @Override
        public PermissionCollection getPermissions(CodeSource codesource) {
            return perms;
        }

        private void addPermissions() {
            SocketPermission socketPermission = new SocketPermission("*:1024-", "connect, resolve");
            PropertyPermission propertyPermission = new PropertyPermission("*", "read, write");
            FilePermission filePermission = new FilePermission("<<ALL FILES>>", "read");
            AllPermission allPermission = new AllPermission();

            perms.add(socketPermission);
            perms.add(propertyPermission);
            perms.add(filePermission);
            perms.add(allPermission);
        }

    }
    public static class MyPermissionCollection extends PermissionCollection {

        private static final long serialVersionUID = 614300921365729272L;

        ArrayList<Permission> perms = new ArrayList<Permission>();

        public void add(Permission p) {
            perms.add(p);
        }

        public boolean implies(Permission p) {
            for (Iterator<Permission> i = perms.iterator(); i.hasNext();) {
                if (((Permission) i.next()).implies(p)) {
                    return true;
                }
            }
            return false;
        }

        public Enumeration<Permission> elements() {
            return Collections.enumeration(perms);
        }

        public boolean isReadOnly() {
            return false;
        }

    }
}
