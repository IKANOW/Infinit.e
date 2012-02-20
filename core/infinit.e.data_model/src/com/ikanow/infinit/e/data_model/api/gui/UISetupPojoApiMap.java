package com.ikanow.infinit.e.data_model.api.gui;

import com.google.gson.GsonBuilder;
import com.ikanow.infinit.e.data_model.api.BasePojoApiMap;
import com.ikanow.infinit.e.data_model.store.social.gui.UISetupPojo;

public class UISetupPojoApiMap implements BasePojoApiMap<UISetupPojo> {

	public GsonBuilder extendBuilder(GsonBuilder gp) {
		return gp;
	}
}
