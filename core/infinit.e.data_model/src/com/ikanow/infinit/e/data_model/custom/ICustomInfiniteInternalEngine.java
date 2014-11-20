package com.ikanow.infinit.e.data_model.custom;

import java.util.Collection;

import org.apache.hadoop.conf.Configuration;
import org.bson.types.ObjectId;

public interface ICustomInfiniteInternalEngine {

	void preTaskActivities(ObjectId jobId, Collection<ObjectId> jobCommunityIds, Configuration jobConfig, boolean isDistributed);
	
	void postTaskActivities(ObjectId jobId, String jobName, Collection<ObjectId> jobCommunityIds, String userArgs, boolean isError, String errs);
}
