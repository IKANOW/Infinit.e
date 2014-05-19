/*******************************************************************************
 * Copyright 2012 The Infinit.e Open Source Project sponsored by IKANOW.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.ikanow.infinit.e.application.utils;

import com.ikanow.infinit.e.data_model.store.MongoDbManager;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

public class MongoQueue
{
	private String collectionName;
	private String dbName;
	
	public MongoQueue(String dbName, String collectionName)
	{
		this.dbName = dbName;
		this.collectionName = collectionName;
	}
	
	public DBObject pop(BasicDBObject query)
	{
		return MongoDbManager.getCollection(dbName, collectionName).findAndModify(query, null, new BasicDBObject("_id",1), true, null, false, false);				
	}
	public DBObject pop()
	{
		return pop(new BasicDBObject());
	}
	
	public void push(DBObject element)
	{
		MongoDbManager.getCollection(dbName, collectionName).insert(element);
	}
}
