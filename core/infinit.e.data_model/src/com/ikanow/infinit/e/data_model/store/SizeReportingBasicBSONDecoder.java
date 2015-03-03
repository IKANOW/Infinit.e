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

import org.bson.BSONCallback;

import com.mongodb.DBCallback;
import com.mongodb.DBCollection;
import com.mongodb.DBDecoder;
import com.mongodb.DBDecoderFactory;
import com.mongodb.DBObject;
import com.mongodb.DefaultDBCallback;

public class SizeReportingBasicBSONDecoder extends org.bson.BasicBSONDecoder implements DBDecoderFactory, DBDecoder {
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
	protected long _size = 0;
	@Override
	public DBDecoder create() {
		return this;
	}
	@Override
	public DBObject decode(byte[] b, DBCollection collection) {
        DBCallback cbk = getDBCallback(collection);
        cbk.reset();
        decode(b, cbk);
        return (DBObject) cbk.get();
	}
	
	@Override
	public DBObject decode(InputStream in, DBCollection collection)
			throws IOException {
        DBCallback cbk = getDBCallback(collection);
        cbk.reset();
        decode(in, cbk);
        return (DBObject) cbk.get();
	}
	@Override
	public DBCallback getDBCallback(DBCollection collection) {
		return new DefaultDBCallback(collection);
	}
}
