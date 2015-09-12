package org.elasticsearch.common.collect;

import org.elasticsearch.cluster.metadata.AliasMetaData;
import org.elasticsearch.cluster.metadata.MetaData;

public class CrossVersionImmutableMapOfImmutableMaps<T> {
	ImmutableOpenMap<String, ImmutableOpenMap<String, T>> _backingMap;
	
	public static CrossVersionImmutableMapOfImmutableMaps<AliasMetaData> getAliases(MetaData meta) {
		return new CrossVersionImmutableMapOfImmutableMaps<AliasMetaData>(meta.getAliases());
	}
	
	protected CrossVersionImmutableMapOfImmutableMaps(ImmutableOpenMap<String, ImmutableOpenMap<String, T>> backingMap) {		
		_backingMap = backingMap;		
	}
	public CrossVersionImmutableMap<T> get(String key) {
		ImmutableOpenMap<String, T> x = _backingMap.get(key);
		if (null == x) {
			return null;
		}
		return new CrossVersionImmutableMap<T>(x);
	}
	public boolean containsKey(String key) {
		return _backingMap.containsKey(key);
	}
}
