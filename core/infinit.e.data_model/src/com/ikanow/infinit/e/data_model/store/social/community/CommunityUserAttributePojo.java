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
