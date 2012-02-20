package com.ikanow.infinit.e.data_model.api.custom.mapreduce;

import java.util.Date;
import java.util.List;

import com.google.gson.reflect.TypeToken;
import com.ikanow.infinit.e.data_model.api.BaseApiPojo;

public class CustomMapReduceResultPojo  extends BaseApiPojo
{
	// Standard static function for readability
	@SuppressWarnings("unchecked")
	static public TypeToken<List<CustomMapReduceResultPojo>> listType() { return new TypeToken<List<CustomMapReduceResultPojo>>(){}; }
	
	public Date lastCompletionTime = null;
	public Object results = null;
}
