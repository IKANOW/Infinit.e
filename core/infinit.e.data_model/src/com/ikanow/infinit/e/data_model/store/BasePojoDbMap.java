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
package com.ikanow.infinit.e.data_model.store;

import com.google.gson.GsonBuilder;

public interface BasePojoDbMap<S> {
	
	// Use this to customize serialization for the class and any known subclasses
	// (child-classes need to be added in the custom de/serializer by calling their extendBuilders manually)
	// (it is those subclasses' responsibility to handle their own child-classes etc etc)
	public GsonBuilder extendBuilder(GsonBuilder gp);
}
