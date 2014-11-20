package com.ikanow.infinit.e.data_model.custom;

import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.apache.hadoop.io.ArrayWritable;
import org.apache.hadoop.io.BooleanWritable;
import org.apache.hadoop.io.ByteWritable;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.MapWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.VIntWritable;
import org.apache.hadoop.io.VLongWritable;
import org.apache.hadoop.io.Writable;
import org.bson.BSONObject;
import org.bson.types.ObjectId;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;

@SuppressWarnings("serial")
public class BasicDBObjectWrappingEsWritable extends BasicDBObject {

	public BasicDBObjectWrappingEsWritable(MapWritable backer) {
		_backer = backer;
	}
	
	protected MapWritable _backer;
	protected boolean _addedTerms = false;
	
	// Methods to leave alone
	//public boolean isPartialObject() {
	//public void markAsPartialObject() {
	
	@Override
	public Object copy() {
		return this.clone();
	}
	
	@Override
	public BasicDBObject append(String key, Object val) {
		return (BasicDBObject) this.put(key, val);
	}
	
	@Override
	public String toString() {
		this.values();
		return super.toString();
	}

	@Override
	public boolean containsField(String field) {
		return _backer.containsKey(field) || super.containsField(field);
	}

	@Override
	public boolean containsKey(String field) {
		return _backer.containsKey(field) || super.containsField(field);
	}
	
	@Override
	public boolean equals(Object arg0) {
		if (arg0 instanceof BasicDBObjectWrappingEsWritable) {
			return _backer.equals(((BasicDBObjectWrappingEsWritable)arg0)._backer) && super.equals(arg0);
		}
		else return false;
	}

	public static Object transform(Object x) {
		if (null == x) {
			return x;
		}
		else if (x instanceof NullWritable) {
			return null;
		}
		else if (x instanceof BooleanWritable) {
			return ((BooleanWritable)x).get();
		}
		else if (x instanceof Text) {
			return ((Text)x).toString();
		}
		else if (x instanceof ByteWritable) {
			return ((ByteWritable)x).get();
		}
		else if (x instanceof IntWritable) {
			return ((IntWritable)x).get();
		}
		else if (x instanceof VIntWritable) {
			return ((VIntWritable)x).get();
		}
		else if (x instanceof LongWritable) {
			return ((LongWritable)x).get();
		}
		else if (x instanceof VLongWritable) {
			return ((VLongWritable)x).get();
		}
		else if (x instanceof BytesWritable) {
			return ((BytesWritable)x).getBytes();
		}
		else if (x instanceof DoubleWritable) {
			return ((DoubleWritable)x).get();
		}
		else if (x instanceof FloatWritable) {
			return ((FloatWritable)x).get();
		}
		else if (x instanceof ArrayWritable) {
			Writable[] xx = ((ArrayWritable)x).get();
			BasicDBList dbl = new BasicDBList();
			for (Writable w: xx) {
				dbl.add(transform(w));
			}
			return dbl;
		}
		else if (x instanceof MapWritable) { // recurse! (ish)
			return new BasicDBObjectWrappingEsWritable((MapWritable)x);
		}		
		else return null;
	}
	
	@Override
	public Object get(String key) {
		if (_addedTerms) {
			Object o = super.get(key);
			if (null != o) {
				return o;
			}
		}
		Object x = _backer.get(key);
		return transform(x);
	}

	@Override
	public boolean getBoolean(String key) {
		if (_addedTerms) {
			Object x = super.get(key);
			if (null != x) return (Boolean)x;
		}
		Object o = _backer.get(key);
		if (null == o) {
			throw new ClassCastException();
		}
		else if (o instanceof BooleanWritable) {
			return ((BooleanWritable)o).get();
		}
		else {
			throw new ClassCastException();
		}
	}

	@Override
	public boolean getBoolean(String key, boolean def) {
		if (_addedTerms) {
			Object x = super.get(key);
			if (null != x) return (Boolean)x;
		}
		Object o = _backer.get(key);
		if (null == o) {
			return def;
		}
		else if (o instanceof BooleanWritable) {
			return ((BooleanWritable)o).get();
		}
		else {
			throw new ClassCastException();
		}
	}

	@Override
	public Date getDate(String field) {
		if (_addedTerms) {
			Object x = super.get(field);
			if (null != x) return (Date)x;
		}
		throw new ClassCastException();
	}

	@Override
	public Date getDate(String field, Date def) {
		if (_addedTerms) {
			Object x = super.get(field);
			if (null != x) return (Date)x;
		}
		return def;
	}

	@Override
	public double getDouble(String key) {
		if (_addedTerms) {
			Object x = super.get(key);
			if (null != x) return (Double)x;
		}
		Object o = _backer.get(key);
		if (null == o) {
			throw new ClassCastException();
		}
		else if (o instanceof DoubleWritable) {
			return ((DoubleWritable)o).get();
		}
		else {
			throw new ClassCastException();
		}
	}

	@Override
	public double getDouble(String key, double def) {
		if (_addedTerms) {
			Object x = super.get(key);
			if (null != x) return (Double)x;
		}
		Object o = _backer.get(key);
		if (null == o) {
			return def;
		}
		else if (o instanceof DoubleWritable) {
			return ((DoubleWritable)o).get();
		}
		else {
			throw new ClassCastException();
		}
	}

	@Override
	public int getInt(String key) {
		if (_addedTerms) {
			Object x = super.get(key);
			if (null != x) return (Integer)x;
		}
		Object o = _backer.get(key);
		if (null == o) {
			throw new ClassCastException();
		}
		else if (o instanceof IntWritable) {
			return ((IntWritable)o).get();
		}
		else {
			throw new ClassCastException();
		}
	}

	@Override
	public int getInt(String key, int def) {
		if (_addedTerms) {
			Object x = super.get(key);
			if (null != x) return (Integer)x;
		}
		Object o = _backer.get(key);
		if (null == o) {
			return def;
		}
		else if (o instanceof IntWritable) {
			return ((IntWritable)o).get();
		}
		else {
			throw new ClassCastException();
		}
	}

	@Override
	public long getLong(String key) {
		if (_addedTerms) {
			Object x = super.get(key);
			if (null != x) return (Long)x;
		}
		Object o = _backer.get(key);
		if (null == o) {
			throw new ClassCastException();
		}
		else if (o instanceof LongWritable) {
			return ((LongWritable)o).get();
		}
		else {
			throw new ClassCastException();
		}
	}

	@Override
	public long getLong(String key, long def) {
		if (_addedTerms) {
			Object x = super.get(key);
			if (null != x) return (Long)x;
		}
		Object o = _backer.get(key);
		if (null == o) {
			return def;
		}
		else if (o instanceof LongWritable) {
			return ((LongWritable)o).get();
		}
		else {
			throw new ClassCastException();
		}
	}

	@Override
	public ObjectId getObjectId(String field) {
		if (_addedTerms) {
			Object x = super.get(field);
			if (null != x) return (ObjectId)x;
		}
		throw new ClassCastException();
	}

	@Override
	public ObjectId getObjectId(String field, ObjectId def) {
		if (_addedTerms) {
			Object x = super.get(field);
			if (null != x) return (ObjectId)x;
		}
		return def;
	}

	@Override
	public String getString(String key) {
		if (_addedTerms) {
			Object x = super.get(key);
			if (null != x) return (String)x;
		}
		Object o = _backer.get(key);
		if (null == o) {
			throw new ClassCastException();
		}
		else if (o instanceof Text) {
			return ((Text)o).toString();
		}
		else {
			throw new ClassCastException();
		}
	}

	@Override
	public String getString(String key, String def) {
		if (_addedTerms) {
			Object x = super.get(key);
			if (null != x) return (String)x;
		}
		Object o = _backer.get(key);
		if (null == o) {
			return def;
		}
		else if (o instanceof Text) {
			return ((Text)o).toString();
		}
		else {
			throw new ClassCastException();
		}
	}

	@Override
	public Object put(String key, Object val) {
		_addedTerms = true;
		_backer.remove(key);
		return super.put(key, val);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void putAll(Map arg0) {
		_addedTerms = true;
		super.putAll(arg0);
	}

	@Override
	public void putAll(BSONObject arg0) {
		_addedTerms = true;
		super.putAll(arg0);
	}

	@Override
	public Object removeField(String key) {
		_backer.remove(key);
		return super.removeField(key);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Map toMap() {
		return this;
	}

	@Override
	public boolean containsValue(Object value) {
		return _backer.containsValue(value) || super.containsValue(value);
	}

	@Override
	public void clear() {
		_backer.clear();
		super.clear();
	}

	@Override
	protected boolean removeEldestEntry(
			java.util.Map.Entry<String, Object> eldest) {
		//(just do nothing to _backer)
		return super.removeEldestEntry(eldest);
	}

	@Override
	public int size() {
		return super.size() + _backer.size();
	}

	@Override
	public boolean isEmpty() {
		return super.isEmpty() && _backer.isEmpty();
	}

	@Override
	public Object remove(Object key) {
		Object o = _backer.remove(key);
		if (null == o) {
			return super.remove(key);
		}
		return transform(o);
	}

	@Override
	public Object clone() {
		BasicDBObjectWrappingEsWritable clone = (BasicDBObjectWrappingEsWritable)super.clone();
		clone._backer = new MapWritable(_backer);
		return clone;
	}

	@Override
	public Set<String> keySet() {
		Set<String> keySet = super.keySet();
		for (Writable w: _backer.keySet()) {
			keySet.add(w.toString());
		}
		return keySet;
	}

	@Override
	public Collection<Object> values() {
		_addedTerms = true;
		// This will be slow but (probably!) functional...
		for (Writable x: _backer.keySet()) {
			this.put(x.toString(), transform(_backer.get(x)));
		}
		_backer.clear();
		return super.values();
	}

	@Override
	public Set<java.util.Map.Entry<String, Object>> entrySet() {
		this.values();
		return super.entrySet();
	}

	@Override
	public int hashCode() {
		this.values();
		return super.hashCode();
	}
	
}
