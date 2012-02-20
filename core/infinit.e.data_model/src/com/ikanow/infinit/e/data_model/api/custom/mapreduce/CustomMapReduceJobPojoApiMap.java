package com.ikanow.infinit.e.data_model.api.custom.mapreduce;

import com.google.gson.GsonBuilder;
import com.ikanow.infinit.e.data_model.api.BasePojoApiMap;
import com.ikanow.infinit.e.data_model.store.custom.mapreduce.CustomMapReduceJobPojo;

public class CustomMapReduceJobPojoApiMap implements BasePojoApiMap<CustomMapReduceJobPojo> {

	public GsonBuilder extendBuilder(GsonBuilder gp) {
		return gp;
	}
}