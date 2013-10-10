package org.elasticsearch.index.engine;

import org.elasticsearch.index.shard.ShardId;

public class DocumentAlreadyExistsEngineException extends org.elasticsearch.index.engine.DocumentAlreadyExistsException {

	private static final long serialVersionUID = 6963742420200074438L;

	public DocumentAlreadyExistsEngineException(ShardId shardId, String type,String id) {
		super(shardId, type, id);
	}
	
}