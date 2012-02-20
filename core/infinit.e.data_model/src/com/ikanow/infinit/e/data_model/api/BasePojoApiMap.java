package com.ikanow.infinit.e.data_model.api;

import com.google.gson.GsonBuilder;

public interface BasePojoApiMap<S> {
	
	// Use this to customize serialization for the class and any known subclasses
	// (child-classes need to be added in the custom de/serializer by calling their extendBuilders manually)
	// (it is those subclasses' responsibility to handle their own child-classes etc etc)
	public GsonBuilder extendBuilder(GsonBuilder gp);
}
