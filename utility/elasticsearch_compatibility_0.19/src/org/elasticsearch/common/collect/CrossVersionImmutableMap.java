package org.elasticsearch.common.collect;

import org.elasticsearch.cluster.metadata.IndexTemplateMetaData;
import org.elasticsearch.cluster.metadata.MetaData;

public class CrossVersionImmutableMap<T> {
	protected ImmutableMap<String, T> _backingMap;
	
	public static CrossVersionImmutableMap<IndexTemplateMetaData> getTemplates(MetaData meta) {
		return new CrossVersionImmutableMap<IndexTemplateMetaData>(meta.getTemplates());
	}
	
	CrossVersionImmutableMap(ImmutableMap<String, T> backingMap) {
		_backingMap = backingMap;
	}
	public T get(String key) {
		return _backingMap.get(key);
	}
	public boolean containsKey(String key) {
		return _backingMap.containsKey(key);
	}
}
