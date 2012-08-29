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
