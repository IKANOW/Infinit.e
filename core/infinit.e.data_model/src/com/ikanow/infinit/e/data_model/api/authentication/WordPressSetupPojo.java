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
