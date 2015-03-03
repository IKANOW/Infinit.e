/*******************************************************************************
 * Copyright 2012, The Infinit.e Open Source Project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.ikanow.infinit.e.data_model.custom;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
//These can be commented in temporarily to test J6 code in J8 branch
//import java.util.Spliterator;
//import java.util.function.Consumer;

import com.ikanow.infinit.e.data_model.store.MongoDbUtil;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import com.mongodb.WriteConcern;
import com.mongodb.hadoop.MongoOutput;
import com.mongodb.hadoop.MongoOutputFormat;
import com.mongodb.hadoop.output.MongoRecordWriter;
import com.mongodb.hadoop.util.MongoConfigUtil;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.AbstractMapWritable;
import org.apache.hadoop.io.ArrayWritable;
import org.apache.hadoop.io.BooleanWritable;
import org.apache.hadoop.io.ByteWritable;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.DataInputBuffer;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.RawComparator;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.UTF8;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapred.RawKeyValueIterator;
import org.apache.hadoop.mapreduce.Counter;
import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.JobID;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.OutputCommitter;
import org.apache.hadoop.mapreduce.OutputFormat;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.RecordWriter;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.StatusReporter;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.TaskAttemptID;
import org.apache.hadoop.security.Credentials;
import org.apache.hadoop.util.Progress;
import org.bson.BSONObject;
import org.bson.types.ObjectId;

@SuppressWarnings("deprecation")
public class InfiniteMongoOutputFormat<K,V> extends MongoOutputFormat<K, V> {

	@Override
	public RecordWriter<K, V> getRecordWriter(final TaskAttemptContext context){
		if (InfiniteMongoConfigUtil.getUpdateModeIncremental(context.getConfiguration())) {
			return new InfiniteMongoRecordWriter<K, V>(MongoConfigUtil.getOutputCollection( context.getConfiguration() ), context, "key");			
		}
		else {
			return new InfiniteMongoRecordWriter<K, V>(MongoConfigUtil.getOutputCollection( context.getConfiguration() ), context);
		}
	}

	//TODO (INF-1159): because the existing code has all sort of private!=protected vars have to copy/paste lots of boilerplate code
	// need to pick up the latest code, which is much nicer
	// (though have now added an extra reduce stage)
	
	public static class InfiniteMongoRecordWriter<K, V> extends MongoRecordWriter<K, V> {

		protected boolean _updatingReduceNotPossible = false;
		protected  UpdatingReducer<K, V, K, V> _updatingReducer;
		
		public InfiniteMongoRecordWriter(DBCollection outCollection, TaskAttemptContext context, String... updateKeys_) {
			super(outCollection, context, updateKeys_, true);
			_collection = outCollection;
			_context = context;
			updateKeys = updateKeys_;
		}
		public InfiniteMongoRecordWriter(DBCollection outCollection, TaskAttemptContext context) {
			super(outCollection, context);
			_collection = outCollection;
			_context = context;
			updateKeys = null;
		}
		
		@Override
		public void write( K key, V value ) throws IOException{
			final DBObject o = new BasicDBObject();
			boolean outputIsBsonObject = false;

			if ( key instanceof MongoOutput ){
				( (MongoOutput) key ).appendAsKey( o );
			}
			else if ( key instanceof BSONObject ){
				o.put( "key", key );
			}
			else{
				o.put( "key", toBSON( key ) );
			}

			if ( value instanceof MongoOutput ){
				( (MongoOutput) value ).appendAsValue( o );
			}
			else if ( value instanceof BSONObject ){
				o.putAll( (BSONObject) value );
				outputIsBsonObject = true;
			}
			else{
				o.put( "value", toBSON( value ) );
			}

			try {
				if (updateKeys == null) {
					_collection.save( o, WriteConcern.UNACKNOWLEDGED );
				}
				else if ((null != _updatingReducer) && _updatingReducer.isReducing()) {
					ObjectId reduceId = _updatingReducer.getReduceId();
					
					if (null != _updatingReducer.getDeletionQuery()) { // (else there wasn't an existing record)
						_collection.remove(_updatingReducer.getDeletionQuery(), WriteConcern.UNACKNOWLEDGED);
					}
					if (null != reduceId) {
						o.put("_updateId", _updatingReducer.getReduceId()); // (so we know when this was last created)						
					}
					o.removeField("_id"); // (so will get re-created on save)
					_collection.save( o, WriteConcern.UNACKNOWLEDGED);
				}//TESTED
				else {
					// if we've found the object then we need to do a reduce on it:
					if (null == _updatingReducer && !_updatingReduceNotPossible) {
						try {
							_updatingReducer = new UpdatingReducer<K, V, K, V>(_context, this);
						}
						catch (Exception e) {
							// (this will handle the inval != outval issue)
							_updatingReduceNotPossible = true;
							//DEBUG
							//e.printStackTrace();
						}
					}//TOTEST (see that _updatingReduceNotPossible is set correctly, )
					
					// Is there already a value with this key?
					DBObject query = new BasicDBObject(updateKeys.length);
					for (String updateKey : updateKeys) {
						query.put(updateKey, o.get(updateKey));
					}
					if (null == _updatingReducer) { // fall back to old style
						BasicDBObject fields = new BasicDBObject("_id", 1);
						fields.put("_updateId", 1);
						DBCursor dbc = _collection.find(query, fields);
						if (!dbc.hasNext()) { // new _id, just save it as normal
							_collection.save( o, WriteConcern.UNACKNOWLEDGED );
						}
						else { // need to update an existing object...					
							DBObject currObj = dbc.next();
							ObjectId id = (ObjectId) currObj.get("_updateId");
							if (null == id) {
								id = (ObjectId) currObj.get("_id");
							}
							o.put("_updateId", id);
							_collection.remove(query, WriteConcern.UNACKNOWLEDGED);
							_collection.save( o, WriteConcern.UNACKNOWLEDGED );							
						}						
					}//TESTED (both cases above, second case with ==1 and >1 objects, not that it really matters)
					else { // complex case, the user re-reduces
						DBCursor dbc = _collection.find(query);
						int queryCount = dbc.count();
						if (0 == queryCount) {
							query = null;
						}
						boolean optimizeQuery = (queryCount == 1); // (ie if only object to remove can find its _id and use that instead)
						
						_updatingReducer.updatingReduce(key, dbc.iterator(), value, outputIsBsonObject, query, optimizeQuery);
							// (will call write but with getReduceId() set so won't recurse - see logic above)
							// (if >1 object save the query since will need to delete objects using query)
						
					}//TESTED
				}//(end if full reduce updates are supported) 
			}
			catch ( final MongoException e ) {
				throw new IOException( "can't write to mongo", e );
			}
		}
	    Object toBSON( Object x ){
	        if ( x == null )
	            return null;
	        if ( x instanceof Text || x instanceof UTF8 )
	            return x.toString();
	        if ( x instanceof Writable ){
	            if ( x instanceof AbstractMapWritable )
	                throw new IllegalArgumentException(
	                        "ERROR: MapWritables are not presently supported for MongoDB Serialization." );
	            if ( x instanceof ArrayWritable ){ 
	                Writable[] o = ( (ArrayWritable) x ).get();
	                Object[] a = new Object[o.length];
	                for ( int i = 0; i < o.length; i++ )
	                    a[i] = (Writable) toBSON( o[i] );
	            }
	            if ( x instanceof BooleanWritable )
	                return ( (BooleanWritable) x ).get();
	            if ( x instanceof BytesWritable )
	                return ( (BytesWritable) x ).getBytes();
	            if ( x instanceof ByteWritable )
	                return ( (ByteWritable) x ).get();
	            if ( x instanceof DoubleWritable )
	                return ( (DoubleWritable) x ).get();
	            if ( x instanceof FloatWritable )
	                return ( (FloatWritable) x ).get();
	            if ( x instanceof LongWritable )
	                return ( (LongWritable) x ).get();
	            if ( x instanceof IntWritable )
	                return ( (IntWritable) x ).get();

	        }
	        throw new RuntimeException( "can't convert: " + x.getClass().getName() + " to BSON" );
	    }		
		final DBCollection _collection;
		final TaskAttemptContext _context;
		private final String[] updateKeys;
		
		@Override
		public void close(TaskAttemptContext context) {
			if (null != _updatingReducer) {
				_updatingReducer.updatingCleanup();
			}
		}		
	}	

	///////////////////////////////////////////////////////////////////////////////////////////////
	
	// Proxy class to support reduce->update
	// (Seems to need to be a Reducer to enable me to use Context simply - doesn't do any harm, so whatever)
	
	public static class UpdatingReducer<InKey, InVal, K, V> extends org.apache.hadoop.mapreduce.Reducer<InKey, InVal, K, V> {

		// Interface with writer:
		protected boolean _isReducing;
		public boolean isReducing() {
			return _isReducing; // (set while inside reduce function to tell the writer not to recurse)
		}
		protected DBObject _keyQuery; // (needed if multiple objects are being reduced)
		protected DBObject _backupKeyQuery; // (needed if multiple objects are being reduced)
		public DBObject getDeletionQuery() {
			return (null != _keyQuery) ? _keyQuery : _backupKeyQuery;
		}
		public ObjectId getReduceId() {
			return _updatingReducerContext.getReduceId();
		}
		
		private UpdatingReducerContext _updatingReducerContext;
		private org.apache.hadoop.mapreduce.Reducer<InKey, InVal, K, V> _actualReducer;
		InfiniteMongoRecordWriter<K, V> _writer;
		
		private Method _reduceFunction;
		private Method _setupFunction;
		private Method _cleanupFunction;		
		
		private boolean setup = false;
		
		@SuppressWarnings("unchecked")
		public UpdatingReducer(TaskAttemptContext context, InfiniteMongoRecordWriter<K, V> writer) throws IOException, InterruptedException, InstantiationException, IllegalAccessException, ClassNotFoundException, IllegalArgumentException, InvocationTargetException {
			
			_writer = writer;
			
			_updatingReducerContext = new UpdatingReducerContext(context, context.getConfiguration(), context.getTaskAttemptID(), 
					null, //input - don't need this since calling reduce myself
					null, null, // inputKeyCount (optional), inputValueCounter (optional)
					writer,  // output
					null, // output committer - don't need this since calling reduce myself
					null, // status reporter - will have to live without this... 
					null, // raw comparator - don't need this since calling reduce myself 
					(Class<InKey>)context.getMapOutputKeyClass(), (Class<InVal>)context.getMapOutputValueClass());			
					
			_actualReducer = (org.apache.hadoop.mapreduce.Reducer<InKey, InVal, K, V>) context.getReducerClass().newInstance();
			
			// Can't access setup/reduce/cleanup so use reflection trickery:
			_setupFunction = getMethod(context.getReducerClass(), "setup", Context.class);
			_setupFunction.setAccessible(true);
			_cleanupFunction = getMethod(context.getReducerClass(), "cleanup", Context.class);
			_cleanupFunction.setAccessible(true);
			_reduceFunction = getMethod(context.getReducerClass(), "reduce", Object.class, Iterable.class, Context.class);
			_reduceFunction.setAccessible(true);						
		}//TESTED
		
		public void updatingReduce(InKey key, Iterator<DBObject> reduceIterator, InVal newVal, boolean outputIsBsonObject, DBObject keyQuery, boolean optimizeKeyQuery) {
			
			// Setup first time through:
			if (!setup) {
				setup = true;
				try {
					_setupFunction.invoke(_actualReducer, _updatingReducerContext);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}//TESTED
			try {
				_isReducing = true;
				_keyQuery = null;
				_backupKeyQuery = keyQuery;
				if (!optimizeKeyQuery) { // (will try to find an _id version that is faster)
					_keyQuery= keyQuery;
				}
				_updatingReducerContext.setupUpdatingReduce(key, reduceIterator, newVal, outputIsBsonObject); // (set up iterator)
				_reduceFunction.invoke(_actualReducer, key, _updatingReducerContext, _updatingReducerContext);
			} 
			catch (Exception e) {
				throw new RuntimeException(e);
			}
			finally {
				_isReducing = false;
			}
		}//TESTED
		
		protected void updatingCleanup() {
			try {
				_cleanupFunction.invoke(_actualReducer, _updatingReducerContext);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}//TESTED
		
		////////////////////////////////////////////
		
		// Also have a proxy for the context just to avoid some null pointers
		
		public class UpdatingReducerContext extends org.apache.hadoop.mapreduce.Reducer<InKey, InVal, K, V>.Context implements Iterable<InVal>, Iterator<InVal> {

			protected Iterator<DBObject> _reduceIterator;
			protected boolean _outputIsBsonObject;
			protected ObjectId _reduceId;
			public ObjectId getReduceId() {
				return _reduceId;
			}
			
			protected InKey _currInKey;			
			protected InVal _currInVal;			
			protected InVal _newInVal;
			
			// Reduce.Context override
			
			public void setupUpdatingReduce(InKey inKey, Iterator<DBObject> reduceIterator, InVal newVal, boolean outputIsBsonObject) {
				_reduceId = null;
				_currInKey = inKey;
				_reduceIterator = reduceIterator;
				_newInVal = newVal;
				_outputIsBsonObject = outputIsBsonObject;
				_currInVal = null;
			}
			
			@Override
			public InKey getCurrentKey() {
				return _currInKey;
			}

			@Override
			public InVal getCurrentValue() {	
				return _currInVal;
			}

			@Override
			public Iterable<InVal> getValues() throws IOException {
				return this;
			}
			
			@Override
			public boolean nextKey() throws IOException, InterruptedException {
				return false;
			}

			@Override
			public boolean nextKeyValue() throws IOException, InterruptedException {
				return false;
			}

			// Iterable override:
			
			@Override
			public Iterator<InVal> iterator() {
				return this;
			}
			
			// Iterator override:
			
			@Override
			public boolean hasNext() {
				return _reduceIterator.hasNext() || (_newInVal != null);
			}

			@SuppressWarnings("unchecked")
			@Override
			public InVal next() {
				if (_reduceIterator.hasNext()) {
					DBObject retObj = _reduceIterator.next();
					ObjectId newId = (ObjectId) retObj.get("_updateId");
					if (null == newId) {
						newId = (ObjectId) retObj.get("_id");
						if (null == _keyQuery) { // (if we can grab the _id here anyway then do so)
							_keyQuery = new BasicDBObject("_id", newId);
						}
					}
					if (null == _keyQuery) { // (if there's only a single id then fill it in here)
						_keyQuery = new BasicDBObject("_id", (ObjectId) retObj.get("_id"));
					}//TESTED
					if (null == _reduceId) { // (just grab the first _reduceId)
						_reduceId = newId;
					}
					else if (_reduceId.compareTo(newId) > 0){ // ie _reduceId > newId, ie newId is earlier
						_reduceId = newId;						
					}//TESTED
					
					if (_outputIsBsonObject) {
						// basically 2 options:
						// - it's a bson writable...
						_currInVal = (InVal)MongoDbUtil.convert(retObj);
					}//TESTED
					else {
						// - ...it's an atomic value as "value" inside dbo
						_currInVal = (InVal)retObj.get("value");
					}//TOTEST
				}
				else if (null != _newInVal) {
					_currInVal = _newInVal;
					_newInVal = null;
				}
				else {
					_currInVal = null;
				}
				return _currInVal;
			}

			@Override
			public void remove() {
				//(nothing to do)
			}

			// Misc other overrides, will hopefully not be called:
			
			@Override
			public Counter getCounter(Enum<?> counterName) {
				return null;
			}

			@Override
			public Counter getCounter(String groupName, String counterName) {
				return null;
			}

			@Override
			public OutputCommitter getOutputCommitter() {
				return null;
			}

			protected TaskAttemptContext _delegateContext;

			public boolean equals(Object obj) {
				return _delegateContext.equals(obj);
			}

			public Class<? extends Reducer<?, ?, ?, ?>> getCombinerClass()
					throws ClassNotFoundException {
				return _delegateContext.getCombinerClass();
			}

			public Configuration getConfiguration() {
				return _delegateContext.getConfiguration();
			}

			public Credentials getCredentials() {
				return _delegateContext.getCredentials();
			}

			public RawComparator<?> getGroupingComparator() {
				return _delegateContext.getGroupingComparator();
			}

			public Class<? extends InputFormat<?, ?>> getInputFormatClass()
					throws ClassNotFoundException {
				return _delegateContext.getInputFormatClass();
			}

			public String getJar() {
				return _delegateContext.getJar();
			}

			public JobID getJobID() {
				return _delegateContext.getJobID();
			}

			public String getJobName() {
				return _delegateContext.getJobName();
			}

			public Class<?> getMapOutputKeyClass() {
				return _delegateContext.getMapOutputKeyClass();
			}

			public Class<?> getMapOutputValueClass() {
				return _delegateContext.getMapOutputValueClass();
			}

			public Class<? extends Mapper<?, ?, ?, ?>> getMapperClass()
					throws ClassNotFoundException {
				return _delegateContext.getMapperClass();
			}

			public int getNumReduceTasks() {
				return _delegateContext.getNumReduceTasks();
			}

			public Class<? extends OutputFormat<?, ?>> getOutputFormatClass()
					throws ClassNotFoundException {
				return _delegateContext.getOutputFormatClass();
			}

			public Class<?> getOutputKeyClass() {
				return _delegateContext.getOutputKeyClass();
			}

			public Class<?> getOutputValueClass() {
				return _delegateContext.getOutputValueClass();
			}

			public Class<? extends Partitioner<?, ?>> getPartitionerClass()
					throws ClassNotFoundException {
				return _delegateContext.getPartitionerClass();
			}

			public Class<? extends Reducer<?, ?, ?, ?>> getReducerClass()
					throws ClassNotFoundException {
				return _delegateContext.getReducerClass();
			}

			public RawComparator<?> getSortComparator() {
				return _delegateContext.getSortComparator();
			}

			public String getStatus() {
				return _delegateContext.getStatus();
			}

			public TaskAttemptID getTaskAttemptID() {
				return _delegateContext.getTaskAttemptID();
			}

			public Path getWorkingDirectory() throws IOException {
				return _delegateContext.getWorkingDirectory();
			}

			public int hashCode() {
				return _delegateContext.hashCode();
			}

			public void progress() {
				_delegateContext.progress();
			}

			public void setStatus(String msg)  {
				try {
					_delegateContext.setStatus(msg);
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
			}

			public String toString() {
				return _delegateContext.toString();
			}

			public boolean userClassesTakesPrecedence() {
				return _delegateContext.userClassesTakesPrecedence();
			}

			public UpdatingReducerContext(TaskAttemptContext context, Configuration conf,
					TaskAttemptID taskid, RawKeyValueIterator input,
					Counter inputKeyCounter, Counter inputValueCounter,
					RecordWriter<K, V> output, OutputCommitter committer,
					StatusReporter reporter, RawComparator<InKey> comparator,
					Class<InKey> keyClass, Class<InVal> valueClass)
					throws IOException, InterruptedException {
				super(conf, taskid, input == null ? new UpdatingReducerDummyIterator() : input, inputKeyCounter, inputValueCounter, output,
						committer, reporter, comparator, keyClass, valueClass);
				_delegateContext = context;
			}

			// These can be commented in temporarily to test J6 code in J8 branch
			//BEGIN
//			public void forEachRemaining(Consumer<? super InVal> arg0) {
//			}
//
//			public void forEach(Consumer<? super InVal> arg0) {
//			}
//			public Spliterator<InVal> spliterator() {
//				return null;
//			}
			//END
		}
		public static class UpdatingReducerDummyIterator implements RawKeyValueIterator { // just needed in the c'tor
			public void close() throws IOException {}
			public DataInputBuffer getKey() throws IOException { return null; }
			public Progress getProgress() { return null; }
			public DataInputBuffer getValue() throws IOException {return null; }
			public boolean next() throws IOException {return false; }			
		}
		
		////////////////////////////////////////////
		
		// Low level utility
		
		@SuppressWarnings({ "rawtypes", "unchecked" })
		private static Method getMethod(Class clazz, String methodName, Class... parameters) {
			Method m = null;
			while ((null == m) && (Object.class != clazz)) {
				try {
					m = clazz.getDeclaredMethod(methodName, parameters);
				}
				catch (Exception e) {
					clazz = clazz.getSuperclass(); // (up one level)
				} 
			}
			if (null == m) {
				throw new RuntimeException("Can't find " + methodName + " in " + clazz.toString());
			}
			return m;
		}//(TESTED c/p from TESTED branch)		
		
	}//(end updating reducer)
}
