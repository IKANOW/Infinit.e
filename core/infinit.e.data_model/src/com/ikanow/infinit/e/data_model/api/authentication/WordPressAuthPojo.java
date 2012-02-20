package com.ikanow.infinit.e.data_model.api.authentication;

import com.ikanow.infinit.e.data_model.api.BaseApiPojo;

public class WordPressAuthPojo extends BaseApiPojo
{
	private String WPUserID = null; // (optional)
	@SuppressWarnings("unused")
	private String username = null; // (ignored, WordPressUserPojo.email[0] always used - retained for backwards compatibility)
	private String password = null; // (mandatory)
	private String accountType = null; // (optional, defaults to "user")
	private String created = null; // (optional)
	private String modified = null; // (optional)
	
	public void setWPUserID(String wPUserID) {
		WPUserID = wPUserID;
	}
	public String getWPUserID() {
		return WPUserID;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getPassword() {
		return password;
	}
	public void setAccountType(String accountType) {
		this.accountType = accountType;
	}
	public String getAccountType() {
		return accountType;
	}
	public void setCreated(String created) {
		this.created = created;
	}
	public String getCreated() {
		return created;
	}
	public void setModified(String modified) {
		this.modified = modified;
	}
	public String getModified() {
		return modified;
	}
}
