/*******************************************************************************
 * Copyright 2012 The Infinit.e Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
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
	
	public static String lastCompletionTime_ = "lastCompletionTime";
	public static String results_ = "results";
}
