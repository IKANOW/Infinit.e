/*******************************************************************************
 * Copyright 2012, The Infinit.e Open Source Project
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
package com.ikanow.infinit.e.utility;

import java.io.IOException;

import org.bson.types.ObjectId;

import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.custom.mapreduce.CustomMapReduceJobPojo;
import com.ikanow.infinit.e.processing.custom.output.CustomOutputIndexingEngine;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoException;

public class MongoCustomTxfer {

	public static void main(String sConfigPath, String sQuery, boolean bDelete) throws NumberFormatException, MongoException, IOException {
		
		// Command line processing
		com.ikanow.infinit.e.data_model.Globals.setIdentity(com.ikanow.infinit.e.data_model.Globals.Identity.IDENTITY_SERVICE);
		if (null != sConfigPath) {
			com.ikanow.infinit.e.data_model.Globals.overrideConfigLocation(sConfigPath);
		}
		
		// Step 1: get the custom map reduce pojo
		
		final BasicDBObject query = new BasicDBObject();
		try {
			query.put("_id", new ObjectId(sQuery));
		}
		catch (Exception e) {
			query.put("jobtitle", sQuery);
		}
		
		final CustomMapReduceJobPojo cmr = CustomMapReduceJobPojo.fromDb(DbManager.getCustom().getLookup().findOne(query), CustomMapReduceJobPojo.class);
		
		if (null == cmr) {
			System.out.println("Failed to locate custom job name/id =" + sQuery);
		}
		
		if (bDelete) {
			CustomOutputIndexingEngine.deleteOutput(cmr);			
		}
		else {
			CustomOutputIndexingEngine.completeOutput(cmr, null, null);
		}
		
	}
}
