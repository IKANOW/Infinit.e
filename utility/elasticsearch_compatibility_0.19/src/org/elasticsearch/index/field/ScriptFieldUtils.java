package org.elasticsearch.index.field;

import java.util.Arrays;
import java.util.List;

import org.elasticsearch.index.field.data.strings.StringDocFieldData;
import org.elasticsearch.index.field.data.longs.LongDocFieldData;

public class ScriptFieldUtils {

	public static List<String> getStrings(Object o) {
		return Arrays.asList(((StringDocFieldData)o).getValues());
	}
	public static long getLong(Object o) {
		return ((LongDocFieldData)o).getValue();
	}
	
}
