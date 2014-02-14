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

import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bson.BasicBSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.Bytes;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoURI;
import com.mongodb.hadoop.input.MongoInputSplit;

public class InfiniteMongoInputSplit extends MongoInputSplit
{		
	protected boolean _createCursor = false;
		
	public InfiniteMongoInputSplit(MongoInputSplit rhs, DBObject queryOverwrite, boolean noTimeout) {
		this(rhs.getMongoURI(), rhs.getKeyField(), rhs.getQuerySpec(), rhs.getFieldSpec(), rhs.getSortSpec(), rhs.getLimit(), rhs.getSkip(), noTimeout);
		getQuerySpec().put("$query", queryOverwrite);
	}
	
	public InfiniteMongoInputSplit(MongoInputSplit rhs, boolean noTimeout) {
		this(rhs.getMongoURI(), rhs.getKeyField(), rhs.getQuerySpec(), rhs.getFieldSpec(), rhs.getSortSpec(), rhs.getLimit(), rhs.getSkip(), noTimeout);
	}
	
	public InfiniteMongoInputSplit(MongoURI inputURI, String inputKey,
			DBObject query, DBObject fields, DBObject sort, int limit, int skip,
			boolean noTimeout) {
		super(inputURI,inputKey,query,fields,sort,limit,skip,noTimeout);
	}
	
	public InfiniteMongoInputSplit(){super(); _createCursor = true;} // (only create cursor when called from mapper)

	@SuppressWarnings("deprecation")
	@Override
	protected DBCursor getCursor()
	{
		//added the limit and skip
		if (_createCursor && ( _cursor == null )){

			DBObject query = null;
			BasicBSONObject queryObj =(BasicBSONObject) _querySpec.get("$query");
			BasicBSONObject minObj = (BasicBSONObject) _querySpec.get("$min");
			BasicBSONObject maxObj = (BasicBSONObject) _querySpec.get("$max");
			if (null == queryObj) {
				if ((null != minObj) || (null != maxObj)) { // one of min/max specified
					query = new BasicDBObject();
				}
				else { // no $query, $max or $min => this is the query
					query = _querySpec;
				}
			}
			else {
				query = new BasicDBObject(queryObj);
			}
			_cursor = InfiniteMongoConfigUtil.getCollection( _mongoURI ).find( query, _fieldSpec ).sort( _sortSpec ).limit(_limit).skip(_skip);
			
			if (null != minObj) {
				
				Iterator<Map.Entry<String, Object>> it = minObj.entrySet().iterator();
				while (it.hasNext()) { // remove upper/lower limit objects because not sure about new mongo syntax 
					Map.Entry<String, Object> keyVal = it.next();
					if (keyVal.getValue() instanceof org.bson.types.MinKey) {
						it.remove();
					}
				}
				if (!minObj.isEmpty()) {
					_cursor = _cursor.addSpecial("$min", new BasicDBObject(minObj));
				}
			}
			if (null != maxObj) {
				Iterator<Map.Entry<String, Object>> it = maxObj.entrySet().iterator();
				while (it.hasNext()) { // remove upper/lower limit objects because not sure about new mongo syntax 
					Map.Entry<String, Object> keyVal = it.next();
					if (keyVal.getValue() instanceof org.bson.types.MaxKey) {
						it.remove();
					}
				}
				if (!maxObj.isEmpty()) {
					_cursor = _cursor.addSpecial("$max", new BasicDBObject(maxObj));
				}
			}
			
	        log.info( "Created InfiniteMongoInputSplit cursor: min=" + minObj + ", max=" + maxObj + ", query=" + query );
			
			
			if (_notimeout) _cursor.setOptions( Bytes.QUERYOPTION_NOTIMEOUT );
			_cursor.slaveOk();
		}

		return _cursor;
	}	
    private static final Log log = LogFactory.getLog( InfiniteMongoInputSplit.class );
}
