package com.ikanow.infinit.e.data_model.api.social.person;

import com.google.gson.GsonBuilder;
import com.ikanow.infinit.e.data_model.api.BasePojoApiMap;
import com.ikanow.infinit.e.data_model.store.social.person.PersonPojo;

public class PersonPojoApiMap implements BasePojoApiMap<PersonPojo> {

	public GsonBuilder extendBuilder(GsonBuilder gp) {
		return gp;
	}
}
