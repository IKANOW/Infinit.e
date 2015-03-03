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
package com.ikanow.infinit.e.data_model.api.config;

import java.util.List;

import org.bson.types.ObjectId;

import com.google.gson.GsonBuilder;
import com.ikanow.infinit.e.data_model.api.BasePojoApiMap;
import com.ikanow.infinit.e.data_model.store.config.source.SourceFederatedQueryConfigPojo;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojoSubstitutionDbMap.SourcePojoSubstitutionDeserializer;

public class SourcePojoSubstitutionApiMap implements BasePojoApiMap<SourcePojo> {

	protected ObjectId _callingUserId;
	protected SourcePojoSubstitutionDeserializer _errorHandler;
	
	public SourcePojoSubstitutionApiMap(ObjectId callingUserId) {
		_callingUserId = callingUserId;
	}
	public List<String> getErrorMessages() {
		return _errorHandler.getErrMessages();
	}
	
	@Override
	public GsonBuilder extendBuilder(GsonBuilder gp) {
		return new SourceFederatedQueryConfigPojo().extendBuilder(gp.registerTypeAdapter(String.class, (_errorHandler = new SourcePojoSubstitutionDeserializer(_callingUserId))));
		//(note this bypasses the custom source pojo deserialization - but it isn't needed because extractor options are already in "." notation)
	}
}
