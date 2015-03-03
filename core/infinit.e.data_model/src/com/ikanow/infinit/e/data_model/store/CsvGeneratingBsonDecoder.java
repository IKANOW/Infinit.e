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
package com.ikanow.infinit.e.data_model.store;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.bson.BSONCallback;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCallback;
import com.mongodb.DBCollection;
import com.mongodb.DBDecoder;
import com.mongodb.DBDecoderFactory;
import com.mongodb.DBObject;
import com.mongodb.DefaultDBCallback;

public class CsvGeneratingBsonDecoder extends org.bson.BasicBSONDecoder implements DBDecoderFactory, DBDecoder {
	@Override
	public int decode(byte[] b, BSONCallback callback) {
		int size = super.decode(b, callback);
		_size += size;
		return size;
	}
	@Override
	public int decode(InputStream in, BSONCallback callback)
			throws IOException {
		int size = super.decode(in, callback);
		_size += size;
		return size;
	}
	public void resetSize() {
		_size = 0;
	}
	public long getSize() {
		return _size;
	}
	public Collection<String> getOrderedFields() {
		return _fieldPos.keySet();
	}
	public StringBuffer getCsv() {
		return _csv;
	}
	
	protected long _size = 0;
	protected LinkedHashMap<String, Integer> _fieldPos = new LinkedHashMap<String, Integer>();
	
	protected StringBuffer _csv = new StringBuffer();
	
	@Override
	public DBDecoder create() {
		return this;
	}
	protected BasicDBObject _emptyObject = new BasicDBObject();
	@Override
	public DBObject decode(byte[] b, DBCollection collection) {
        DBCallback cbk = new CsvGeneratingDBCallback(collection);
        cbk.reset();
        decode(b, cbk);
        appendRecord((DBObject)cbk.get());
        return _emptyObject;
	}
	
	@Override
	public DBObject decode(InputStream in, DBCollection collection)
			throws IOException {
        DBCallback cbk = new CsvGeneratingDBCallback(collection);
        cbk.reset();
        decode(in, cbk);
        appendRecord((DBObject)cbk.get());
        return _emptyObject;
	}
	
	protected void appendRecord(DBObject record) {
		for (String field: _fieldPos.keySet()) {
			Object o = record.get(field);
			if (null != o) {
				_csv.append(o);
			}
			_csv.append(',');
		}
		_csv.setLength(_csv.length() - 1); // (remove trailing ,)
		_csv.append('\n');
	}
	
	public class CsvGeneratingDBCallback extends DefaultDBCallback {

		protected StringBuffer _currFieldStack = new StringBuffer();
		protected String _currName = null;
		protected boolean _isArray = false;
		
		public CsvGeneratingDBCallback(DBCollection coll) {
			super(coll);
		}		
		
		@Override
		public void objectStart(boolean array, String name) {
			_isArray = array;

			super.objectStart(array, name);
			
			//DEBUG
			//System.out.println("new object! " + name + " ? " + array);
			
			_currFieldStack.append(name);
			_currFieldStack.append('.');
			_currName = name;
		}
		@Override
		public Object objectDone() {			
			Object o = super.objectDone();
			
			//DEBUG
			//System.out.println("(done) " + _currName);
			
			if (_isArray) {
				BasicBSONList list = (BasicBSONList)o;
				if (list.isEmpty()) { // (treat this like a missing entry)					
					cur().removeField(_currName);
				}
				else { // overwrite the array with the first val
					cur().put(_currName, list.iterator().next());
				}
				// (this then gets flattened by the parent)
				_currFieldStack.setLength(_currFieldStack.length() - _currName.length() - 1);				
			}
			else if (null != _currName) { // (else nothing to do, already flattened)
				// We know that o just consists of top level fields at this point (see removeField below), so
				// just copy those into the root
				BSONObject oo = (BSONObject)o;
				String currStack = _currFieldStack.toString();
				BasicBSONObject root = (BasicBSONObject) get();
				for (String field: oo.keySet()) {
					String compositeFieldName = currStack + field;
					root.put(compositeFieldName, oo.get(field));
					//(field normalization and column id is handled below at the end of the object)
				}
				_currFieldStack.setLength(_currFieldStack.length() - _currName.length() - 1);				
				cur().removeField(_currName);
			}
			else { // just ensure that the top-level fields are present 
				BasicBSONObject root = (BasicBSONObject) get();
				for (Map.Entry<String, Object> it: root.entrySet()) {
					String field = it.getKey();
					Object val = it.getValue();
					if (!_fieldPos.containsKey(field)) {
						_fieldPos.put(field, 1 + _fieldPos.size());
					}		
					if (val instanceof String) { // handle escaping
						String s = (String) val;
						if ('\n' == s.charAt(s.length() - 1)) {
							s = s.substring(0, s.length() - 1);
						}
						root.put(field, "\"" + s.replace("\"", "\\\"").replace("\n",  " ") + "\"");
					}
				}
			}
			_currName = curName();
			_isArray = false;
			return o;
		}
	}
	
	@Override
	public DBCallback getDBCallback(DBCollection collection) {
		return new CsvGeneratingDBCallback(collection);
	}
}
