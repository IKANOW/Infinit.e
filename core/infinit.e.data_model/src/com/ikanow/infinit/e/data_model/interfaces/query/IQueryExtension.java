package com.ikanow.infinit.e.data_model.interfaces.query;

import java.util.List;

import org.bson.types.ObjectId;

import com.ikanow.infinit.e.data_model.api.ResponsePojo;
import com.ikanow.infinit.e.data_model.api.knowledge.AdvancedQueryPojo;
import com.mongodb.BasicDBObject;

public interface IQueryExtension {

	/*
	 * preQueryActivities - hook called before any internal transactions are performed
	 */
	void preQueryActivities(ObjectId queryId, AdvancedQueryPojo query, String[] communityIdsStrs);
	
	/*
	 * preQueryActivities - hook called immediately before the data is returned from the API
	 */
	void postQueryActivities(ObjectId queryId, List<BasicDBObject> docs, ResponsePojo response);
	
}
