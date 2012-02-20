package com.ikanow.infinit.e.data_model.api.social.community;

import com.google.gson.GsonBuilder;
import com.ikanow.infinit.e.data_model.api.BasePojoApiMap;
import com.ikanow.infinit.e.data_model.store.social.community.CommunityPojo;

public class CommunityPojoApiMap implements BasePojoApiMap<CommunityPojo> {

	public GsonBuilder extendBuilder(GsonBuilder gp) {
		return gp;
	}
}
