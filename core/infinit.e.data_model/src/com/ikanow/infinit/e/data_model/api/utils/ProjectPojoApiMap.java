package com.ikanow.infinit.e.data_model.api.utils;

import com.google.gson.GsonBuilder;
import com.ikanow.infinit.e.data_model.api.BasePojoApiMap;
import com.ikanow.infinit.e.data_model.store.utils.ProjectPojo;

public class ProjectPojoApiMap implements BasePojoApiMap<ProjectPojo> {

	@Override
	public GsonBuilder extendBuilder(GsonBuilder gp) {
		return gp;
	}

}
