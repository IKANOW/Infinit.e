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
package com.ikanow.infinit.e.data_model.store.social.community;

/**
 * @author cvitter
 */
public class CommunityUserAttributePojo {
	
	private String type = null;
	private String defaultValue = null;
	private Boolean allowOverride = false;
	
	public CommunityUserAttributePojo()
	{
		
	}
	
	public CommunityUserAttributePojo(String _type, String _defaultValue, boolean _allowOverride)
	{
		type = _type;
		defaultValue = _defaultValue;
		allowOverride = _allowOverride;
	}
	
	/**
	 * @param type the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}
	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}
	
	/**
	 * @param defaultValue the defaultValue to set
	 */
	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}
	/**
	 * @return the defaultValue
	 */
	public String getDefaultValue() {
		return defaultValue;
	}
	
	/**
	 * @param allowOverride the allowOverride to set
	 */
	public void setAllowOverride(Boolean allowOverride) {
		this.allowOverride = allowOverride;
	}
	/**
	 * @return the allowOverride
	 */
	public Boolean getAllowOverride() {
		return allowOverride;
	}


}
