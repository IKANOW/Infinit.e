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
package com.ikanow.infinit.e.processing.custom.launcher;

import org.bson.types.ObjectId;

import com.ikanow.infinit.e.api.knowledge.QueryHandler;
import com.ikanow.infinit.e.data_model.api.ResponsePojo;
import com.ikanow.infinit.e.data_model.api.ResponsePojo.ResponseObject;
import com.ikanow.infinit.e.data_model.api.knowledge.AdvancedQueryPojo;
import com.ikanow.infinit.e.data_model.store.custom.mapreduce.CustomMapReduceJobPojo;
import com.ikanow.infinit.e.processing.custom.utils.InfiniteHadoopUtils;

public class CustomSavedQueryTaskLauncher {

	public ResponsePojo runSavedQuery(CustomMapReduceJobPojo savedQuery) {
		
		// Run saved query:
		
		QueryHandler queryHandler = new QueryHandler();
		
		// Create query object
		
		ResponsePojo rp = null;
		StringBuffer errorString = new StringBuffer("Saved query error");
		try 
		{
			String queryString = InfiniteHadoopUtils.getQueryOrProcessing(savedQuery.query, InfiniteHadoopUtils.QuerySpec.QUERY);			
			AdvancedQueryPojo query = QueryHandler.createQueryPojo(queryString);
			StringBuffer communityIdStrList = new StringBuffer();
			for (ObjectId commId: savedQuery.communityIds) 
			{
				if (communityIdStrList.length() > 0) 
				{
					communityIdStrList.append(',');
				}
				communityIdStrList.append(commId.toString());
			}
			rp = queryHandler.doQuery(savedQuery.submitterID.toString(), query, communityIdStrList.toString(), errorString);
		} 
		catch (Exception e) 
		{
			//DEBUG
			e.printStackTrace();
			errorString.append(": " + e.getMessage());
		}
		if ((null == rp) || (null == rp.getResponse())) { // (this is likely some sort of internal error)
			if (null == rp) 
			{
				rp = new ResponsePojo();
			}
			rp.setResponse(new ResponseObject("Query", false, "Unknown error"));
		}
		if (!rp.getResponse().isSuccess()) {
			rp.getResponse().setMessage(errorString.append('/').append(rp.getResponse().getMessage()).toString());
			return rp;
		}
	
		return rp;
	}
}
