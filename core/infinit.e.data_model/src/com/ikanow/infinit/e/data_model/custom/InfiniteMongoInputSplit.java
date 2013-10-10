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

import com.mongodb.Bytes;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoURI;
import com.mongodb.hadoop.input.MongoInputSplit;

public class InfiniteMongoInputSplit extends MongoInputSplit
{		
	public InfiniteMongoInputSplit(MongoURI inputURI, String inputKey,
			DBObject query, DBObject fields, DBObject sort, int limit, int skip,
			boolean noTimeout) {
		super(inputURI,inputKey,query,fields,sort,limit,skip,noTimeout);
	}
	
	public InfiniteMongoInputSplit(){super(); }

	@SuppressWarnings("deprecation")
	@Override
	protected DBCursor getCursor()
	{
		//added the limit and skip
		if ( _cursor == null ){
			_cursor = InfiniteMongoConfigUtil.getCollection( _mongoURI ).find( _querySpec, _fieldSpec ).sort( _sortSpec ).limit(_limit).skip(_skip);
		if (_notimeout) _cursor.setOptions( Bytes.QUERYOPTION_NOTIMEOUT );
			_cursor.slaveOk();
		}
		
		return _cursor;
	}	
}
