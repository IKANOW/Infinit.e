/*******************************************************************************
 * Copyright 2012, The Infinit.e Open Source Project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
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
