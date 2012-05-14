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
package com.ikanow.infinit.e.data_model.api.gui;

import java.lang.reflect.Type;
import java.util.Set;

import org.bson.types.ObjectId;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.ikanow.infinit.e.data_model.api.BaseApiPojo;
import com.ikanow.infinit.e.data_model.api.BasePojoApiMap;
import com.ikanow.infinit.e.data_model.store.social.gui.UIModulePojo;

public class UIModulePojoApiMap implements BasePojoApiMap<UIModulePojo> {

	public UIModulePojoApiMap(Set<ObjectId> allowedCommunityIds) {
		_allowedCommunityIds = allowedCommunityIds;
	}
	private Set<ObjectId> _allowedCommunityIds = null;
	
	public GsonBuilder extendBuilder(GsonBuilder gp) {
		return gp.registerTypeAdapter(UIModulePojo.class, new UIModulePojoSerializer(_allowedCommunityIds));
	}
	protected static class UIModulePojoSerializer implements JsonSerializer<UIModulePojo> 
	{
		private Set<ObjectId> _allowedCommunityIds = null;
		UIModulePojoSerializer(Set<ObjectId> allowedCommunityIds) {
			_allowedCommunityIds = allowedCommunityIds;
		}
		@Override
		public JsonElement serialize(UIModulePojo module, Type typeOfT, JsonSerializationContext context)
		{
			Set<ObjectId> tmp = module.getCommunityIds();
			if (null != tmp) {
				module.setCommunityIds(null);
				for (ObjectId communityID: tmp) {
					if ((null == _allowedCommunityIds) || _allowedCommunityIds.contains(communityID)) {
							// (if allowed community ids == null, some sort of admin condition)
						module.addToCommunityIds(communityID);
					}
				}
			}
			JsonElement json = BaseApiPojo.getDefaultBuilder().create().toJsonTree(module);
			if (null != tmp) { // (this just ensures that the pojo isn't modified by the mapping)
				module.setCommunityIds(tmp);
			}
			return json;
		}		
	}
}//TESTED
