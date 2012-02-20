package com.ikanow.infinit.e.data_model.api.authentication;

import com.google.gson.GsonBuilder;
import com.ikanow.infinit.e.data_model.api.BasePojoApiMap;
import com.ikanow.infinit.e.data_model.store.social.authentication.AuthenticationPojo;

public class AuthenticationPojoApiMap implements BasePojoApiMap<AuthenticationPojo> {

	public GsonBuilder extendBuilder(GsonBuilder gp) {
		return gp;
	}
}
