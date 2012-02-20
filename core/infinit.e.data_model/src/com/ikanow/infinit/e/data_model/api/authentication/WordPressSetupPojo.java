package com.ikanow.infinit.e.data_model.api.authentication;

import com.ikanow.infinit.e.data_model.api.BaseApiPojo;

public class WordPressSetupPojo  extends BaseApiPojo {
	private WordPressUserPojo user = null;
	private WordPressAuthPojo auth = null;
	/**
	 * @param user the user to set
	 */
	public void setUser(WordPressUserPojo user) {
		this.user = user;
	}
	/**
	 * @return the user
	 */
	public WordPressUserPojo getUser() {
		return user;
	}
	/**
	 * @param auth the auth to set
	 */
	public void setAuth(WordPressAuthPojo auth) {
		this.auth = auth;
	}
	/**
	 * @return the auth
	 */
	public WordPressAuthPojo getAuth() {
		return auth;
	}
}
