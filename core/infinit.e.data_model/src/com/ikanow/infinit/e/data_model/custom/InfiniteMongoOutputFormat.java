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

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import com.mongodb.hadoop.MongoOutput;
import com.mongodb.hadoop.MongoOutputFormat;
import com.mongodb.hadoop.output.MongoRecordWriter;
import com.mongodb.hadoop.util.MongoConfigUtil;

import org.apache.hadoop.io.AbstractMapWritable;
import org.apache.hadoop.io.ArrayWritable;
import org.apache.hadoop.io.BooleanWritable;
import org.apache.hadoop.io.ByteWritable;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.UTF8;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapreduce.RecordWriter;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
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
	
	public static class InfiniteMongoRecordWriter<K, V> extends MongoRecordWriter<K, V> {

		public InfiniteMongoRecordWriter(DBCollection outCollection, TaskAttemptContext context, String... updateKeys_) {
			super(outCollection, context, updateKeys_, true);
			_collection = outCollection;
			_context = context;
			updateKeys = updateKeys_;
			multiUpdate = true; // (has to be true because we're definitely not sharding on it)
			//OR...
			//multiUpdate = false; // (we're sharding on keys in this case)
		}//TODO (INF-2126) TOTEST
		public InfiniteMongoRecordWriter(DBCollection outCollection, TaskAttemptContext context) {
			super(outCollection, context);
			_collection = outCollection;
			_context = context;
			updateKeys = null;
			multiUpdate = false;
		}

		@Override
		public void write( K key, V value ) throws IOException{
			final DBObject o = new BasicDBObject();

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
			}
			else{
				o.put( "value", toBSON( value ) );
			}

			try {
				if (updateKeys == null) {
					_collection.save( o );
				} else {
					// Form the query fields
					DBObject query = new BasicDBObject(updateKeys.length);
					for (String updateKey : updateKeys) {
						query.put(updateKey, o.get(updateKey));
						o.removeField(updateKey);
					}
					// If _id is null remove it, we don't want to override with null _id
					ObjectId newId = new ObjectId();
					o.put("_id", newId); // (this should be ignored in upsert)
					o.put("_updateId", newId); // (use this so we can track the last time a document was modified)
					DBObject set = new BasicDBObject().append("$set", o);
					_collection.update(query, set, true, multiUpdate);
					//TODO (INF-2126): not going to work unless I specify key as the shard key, which currently breaks custom files...
				}
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
		private final boolean multiUpdate;
	}	
}
