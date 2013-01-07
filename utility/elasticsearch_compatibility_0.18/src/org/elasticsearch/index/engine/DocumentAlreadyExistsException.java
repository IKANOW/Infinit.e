package org.elasticsearch.index.engine;

import org.elasticsearch.index.shard.ShardId;

public class DocumentAlreadyExistsException extends org.elasticsearch.index.engine.DocumentAlreadyExistsEngineException {

	private static final long serialVersionUID = 6963742420200074438L;

	public DocumentAlreadyExistsException(ShardId shardId, String type,String id) {
		super(shardId, type, id);
	}
	
}