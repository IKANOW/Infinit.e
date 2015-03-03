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
package com.ikanow.infinit.e.data_model.store.config.source;

import java.util.Date;
import java.util.List;

import com.google.gson.reflect.TypeToken;
import com.ikanow.infinit.e.data_model.store.BaseDbPojo;
import com.mongodb.BasicDBObject;

// (Won't ever actually ser/deser this, just for reference)
public class SimpleFederatedCache extends BaseDbPojo {
	// Standard static function for readability
	@SuppressWarnings("unchecked")
	static public TypeToken<List<SimpleFederatedCache>> listType() { return new TypeToken<List<SimpleFederatedCache>>(){}; }

	public static long QUERY_FEDERATION_CACHE_CLEANSE_SIZE = 100000L;

	public String _id; // (the URL)
	public Date expiryDate;
	public BasicDBObject cachedJson;
	public Date created;

	public static final String _id_ = "_id";
	public static final String expiryDate_ = "expiryDate";
	public static final String cachedJson_ = "cachedJson";
	public static final String created_ = "created";
	
	// cachedJson has the following special fields:
	public static final String __infinite__value_ = "__infinite__value";
	public static final String array_ = "array";
	public static final String value_ = "value";
}
