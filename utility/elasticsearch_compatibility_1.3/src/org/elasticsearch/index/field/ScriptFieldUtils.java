package org.elasticsearch.index.field;

import java.util.List;

import org.elasticsearch.index.fielddata.ScriptDocValues;

public class ScriptFieldUtils {

	public static List<String> getStrings(Object o) {
		return ((ScriptDocValues.Strings)o).getValues();
	}
	public static long getLong(Object o) {
		return ((ScriptDocValues.Longs)o).getValue();
	}
	
}
