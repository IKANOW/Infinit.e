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
package com.ikanow.infinit.e.api.knowledge.federated;

import java.util.Date;
import java.util.HashMap;

import org.bson.types.ObjectId;

import com.ikanow.infinit.e.data_model.store.config.source.SourceFederatedQueryConfigPojo;

public class FederatedQueryInMemoryCache {

	public ObjectId communityId; // if cached under community id
	public String sourceKey; // if cached under source key
	public Date lastUpdated;
	
	public HashMap<String, SourceFederatedQueryConfigPojo> sources; // vs source key, if for a community
	public SourceFederatedQueryConfigPojo source; // if for a source key
}
