package org.elasticsearch.common.collect;

import org.elasticsearch.cluster.metadata.AliasMetaData;
import org.elasticsearch.cluster.metadata.MetaData;

public class CrossVersionImmutableMapOfImmutableMaps<T> {
	ImmutableMap<String, ImmutableMap<String, T>> _backingMap;
	public CrossVersionImmutableMapOfImmutableMaps(ImmutableMap<String, ImmutableMap<String, T>> backingMap) {
		_backingMap = backingMap;		
	}
	
	public static CrossVersionImmutableMapOfImmutableMaps<AliasMetaData> getAliases(MetaData meta) {
		return new CrossVersionImmutableMapOfImmutableMaps<AliasMetaData>(meta.getAliases());
	}
	
	CrossVersionImmutableMap<T> get(String key) {
		ImmutableMap<String, T> x = _backingMap.get(key);
		if (null == x) {
			return null;
		}
		return new CrossVersionImmutableMap<T>(x);
	}
	public boolean containsKey(String key) {
		return _backingMap.containsKey(key);
	}
}
