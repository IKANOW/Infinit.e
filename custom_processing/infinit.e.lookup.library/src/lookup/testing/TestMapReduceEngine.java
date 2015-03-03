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

package lookup.testing;


import com.ikanow.infinit.e.lookup.Lookup;
import com.mongodb.BasicDBObject;
import com.mongodb.hadoop.io.BSONWritable;
import com.mongodb.hadoop.util.MongoTool;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.log4j.Logger;
import org.bson.BSONObject;

import java.io.IOException;


public class TestMapReduceEngine extends MongoTool {

	static Logger _logger = Logger.getLogger("test");
	static boolean _logMessages = false; // (set to false for improved performance)
	
	////////////////////////////////////////////////////
	
	// MAP/REDUCE SPECIFIC LOGIC
	
	////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////
	
	// MAPPER
	
	public static class InfiniteMapper extends Mapper<Object, BSONObject, BSONWritable, BSONWritable> {

		// State:
		protected Lookup.LookupConfig _config;
		protected Lookup _lookup = null;


		@Override
		protected void setup(Context context) {
			String args = context.getConfiguration().get("arguments");
			if (null != args) {
				_config = Lookup.LookupConfig.fromApi(args, Lookup.LookupConfig.class);
			} else {
				_config = new Lookup.LookupConfig();
			}

			_lookup = new Lookup(_config);
		}

		@Override
		public void map(Object key, BSONObject value, Context context) throws IOException, InterruptedException {
			// 1] Get object			
			BasicDBObject record = (BasicDBObject) value;


			record = _lookup.join(record);
			if (record == null) {    // Join Condition was not satisfied
				return;
			}

			BSONWritable keyOut = new BSONWritable();
			keyOut.put("_id", key);
			context.write(keyOut, new BSONWritable(record));


		}

		@Override
		protected void cleanup(Context context) throws IOException, InterruptedException {
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////
	
	// COMBINER OPTIONS	
	
	// COMBINER: identical to reducer
	
	public static class InfiniteCustomCombiner extends InfiniteReducer 
	{
		@Override
		protected boolean inCombiner() { return true; }
	}		
		
	////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////
	
	// REDUCER/COMBINER

	public static class InfiniteReducer extends Reducer<BSONWritable, BSONWritable, BSONWritable, BSONWritable> {
		protected boolean inCombiner() {
			return false;
		}

		@Override
		protected void setup(Context context) {
			String args = context.getConfiguration().get("arguments");

		}

		public void reduce(BSONWritable key, Iterable<BSONWritable> values, Context context)
				throws IOException, InterruptedException {
			for (BSONWritable valueObj : values) {
				context.write(key, valueObj);
			}

		}
	}
		
}
