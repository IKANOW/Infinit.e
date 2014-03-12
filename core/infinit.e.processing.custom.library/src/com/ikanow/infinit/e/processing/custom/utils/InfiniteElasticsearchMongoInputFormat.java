/*******************************************************************************
 * Copyright 2012 The Infinit.e Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.ikanow.infinit.e.processing.custom.utils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.JobContext;
import org.bson.types.ObjectId;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.action.search.SearchRequestBuilder;
import org.elasticsearch.search.SearchHit;

import com.ikanow.infinit.e.api.knowledge.QueryHandler;
import com.ikanow.infinit.e.data_model.api.knowledge.AdvancedQueryPojo;
import com.ikanow.infinit.e.data_model.custom.InfiniteMongoConfig;
import com.ikanow.infinit.e.data_model.custom.InfiniteMongoInputFormat;
import com.ikanow.infinit.e.data_model.custom.InfiniteMongoInputSplit;
import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.mongodb.BasicDBObject;

public class InfiniteElasticsearchMongoInputFormat extends InfiniteMongoInputFormat
{	
	@Override
	public List<InputSplit> getSplits(JobContext context) 
	{
		final Configuration hadoopConfiguration = context.getConfiguration();
		final InfiniteMongoConfig conf = new InfiniteMongoConfig( hadoopConfiguration );
		String queryStr = hadoopConfiguration.get("mongo.input.query");
		String userIdStr = hadoopConfiguration.get("infinit.e.userid");
		AdvancedQueryPojo query = AdvancedQueryPojo.fromApi(queryStr, AdvancedQueryPojo.class);
		return calculateSplits(query, userIdStr, conf);		
	}	
	
	static List<InputSplit> calculateSplits(AdvancedQueryPojo query, String userIdStr, InfiniteMongoConfig conf) {
		// Community IDs, get from QUERY?
		// user id str - get from configuration?
		
		QueryHandler qh = new QueryHandler();
		
		LinkedList<InputSplit> outList = new LinkedList<InputSplit>();
		
		String[] communityIdStrs = new String[query.communityIds.size()];
		int i = 0;
		for (ObjectId commId: query.communityIds) {
			communityIdStrs[i++] = commId.toString();
		}
		
		QueryHandler.QueryInfo queryInfo = qh.convertInfiniteQuery(query, communityIdStrs, userIdStr);
		if (null != queryInfo) {
			int numDocs = 0;
			int maxDocs = conf.getLimit();
			if (0 == maxDocs) {
				maxDocs = Integer.MAX_VALUE;
			}
			int docsPerScroll = 500; // (note this is multiplied by the number of primary shards, will normally be 5 per community)			
			if (maxDocs < docsPerScroll) {
				docsPerScroll = maxDocs; // (can't do any better than this because we don't know what the distribution across shards will be)
			}
			
			SearchRequestBuilder searchOptions = queryInfo.indexMgr.getSearchOptions();
			searchOptions.setSize(docsPerScroll); 
			searchOptions.setSearchType(SearchType.SCAN);
			searchOptions.setScroll("1m");
			
			SearchResponse rsp = queryInfo.indexMgr.doQuery(queryInfo.queryObj, searchOptions);
			String scrollId = rsp.getScrollId();
			
			final int SPLIT_SIZE = 4000;
			ArrayList<ObjectId> idList = null;
			for (;;) { // Until no more hits 
				
				rsp = queryInfo.indexMgr.doScrollingQuery(scrollId, "1m");
				SearchHit[] docs = rsp.getHits().getHits();
				scrollId = rsp.getScrollId(); // (for next time)
				
				if ((null == docs) || (0 == docs.length)) {
					break;
				}					
				//DEBUG
				//System.out.println("SCROLL! " + docs.length + " : " + docs[0].getId());
								
				for (SearchHit hit: docs)  {
					if (null == idList) {
						idList = new ArrayList<ObjectId>(SPLIT_SIZE);
					}
					String idStr = hit.getId();
					try {
						idList.add(new ObjectId(idStr));
					}
					catch (Exception e) {} // carry on...
					if (SPLIT_SIZE == idList.size()) {
						//DEBUG
						//System.out.println("SPLIT! " + idList.size() + " " + idList.get(0) + " , " + outList.size());
										
						BasicDBObject mongoQuery = new BasicDBObject(DocumentPojo._id_, 
														new BasicDBObject(DbManager.in_, idList));
						outList.add((InputSplit)new InfiniteMongoInputSplit(conf.getInputURI(), conf.getInputKey(), 
										mongoQuery, conf.getFields(), conf.getSort(), 0, 0, conf.isNoTimeout()));
						idList = null;
					}//TESTED
					numDocs++;
					if (numDocs >= maxDocs) {
						break;
					}//TESTED
				}
				if (numDocs >= maxDocs) {
					break;
				}//TESTED
			}//(end loop over hits)
			if (null != idList) {
				//DEBUG
				//System.out.println("SPLIT! " + idList.size() + " " + idList.get(0) + " , " + outList.size());
				
				BasicDBObject mongoQuery = new BasicDBObject(DocumentPojo._id_, 
											new BasicDBObject(DbManager.in_, idList));
				outList.add((InputSplit)new InfiniteMongoInputSplit(conf.getInputURI(), conf.getInputKey(), 
								mongoQuery, conf.getFields(), conf.getSort(), 0, 0, conf.isNoTimeout()));
			}//TESTED
		}
		return outList;
	}//TESTED
}
