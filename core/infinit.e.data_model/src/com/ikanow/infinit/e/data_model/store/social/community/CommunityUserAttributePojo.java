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
