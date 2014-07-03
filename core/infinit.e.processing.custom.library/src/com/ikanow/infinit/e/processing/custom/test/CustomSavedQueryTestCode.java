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
package com.ikanow.infinit.e.processing.custom.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import org.bson.types.ObjectId;

import com.ikanow.infinit.e.data_model.Globals;
import com.ikanow.infinit.e.data_model.Globals.Identity;
import com.ikanow.infinit.e.data_model.api.knowledge.AdvancedQueryPojo;
import com.ikanow.infinit.e.data_model.control.DocumentQueueControlPojo;
import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.social.sharing.SharePojo;
import com.ikanow.infinit.e.data_model.store.social.sharing.SharePojo.ShareCommunityPojo;
import com.ikanow.infinit.e.data_model.store.social.sharing.SharePojo.ShareOwnerPojo;
import com.ikanow.infinit.e.processing.custom.CustomProcessingController;
import com.mongodb.BasicDBObject;

public class CustomSavedQueryTestCode {

	static ObjectId fixedShareId_1 = new ObjectId("53a489a93b7ba746c1996827");
	static ObjectId fixedShareId_2 = new ObjectId("53a489a93b7ba746c1996828");
	
	public static void main(String[] args) {
		// Initialize
		System.out.println(Arrays.toString(args));
		Globals.setIdentity(Identity.IDENTITY_SERVICE);
		Globals.overrideConfigLocation(args[0]);

		long testsToRun = 0x4;
		
		try {
			if (0 != (testsToRun & 0x1)) {
				// Basic functionality, hourly first time run
				doTest(1);
				new CustomProcessingController().runThroughSavedQueues();
			}			
			if (0 != (testsToRun & 0x2)) {
				// Basic functionality, multiple queries
				doTest(1);
				doTest(2); // already inserted docs)
				new CustomProcessingController().runThroughSavedQueues();
			}
			if (0 != (testsToRun & 0x4)) {
				// Check that extra time term gets added and check last time
				doTest(3);
				new CustomProcessingController().runThroughSavedQueues();
			}
			if (0 != (testsToRun & 0x8)) {
				// daily frequency, maxDocs
				doTest(4);
				new CustomProcessingController().runThroughSavedQueues();
			}
			if (0 != (testsToRun & 0x10)) {
				// weekly frequency, last doc inserted
				doTest(5);
				new CustomProcessingController().runThroughSavedQueues();
			}
			if (0 != (testsToRun & 0x20)) {
				// saved query
				doTest(6);
				new CustomProcessingController().runThroughSavedQueues();
			}
		}
		finally {
			DbManager.getSocial().getShare().remove(new BasicDBObject(SharePojo._id_, fixedShareId_1));
			DbManager.getSocial().getShare().remove(new BasicDBObject(SharePojo._id_, fixedShareId_2));
		}
	}
	public static void doTest(int testNum)
	{
		// Build a query
		AdvancedQueryPojo queryObj =  AdvancedQueryPojo.fromApi("{'qt':[], 'communityIds': [], 'score':{}, 'output':{'docs':{},'aggregations': { 'entsNumReturn':10 }}}", AdvancedQueryPojo.class);
		AdvancedQueryPojo.QueryTermPojo qt = new AdvancedQueryPojo.QueryTermPojo();
		if (6 == testNum) {
			qt.ftext="bbc";			
		}
		else {
			qt.etext="*";
		}
		queryObj.qt.add(qt);
		queryObj.communityIds.add(new ObjectId("4c927585d591d31d7b37097a"));
		queryObj.communityIds.add(new ObjectId("4c927585d591d31d7b37097b")); // random community to remove
		queryObj.communityIds.add(new ObjectId("4e3706c48d26852237078005"));
		queryObj.score.numAnalyze = 20;
		queryObj.output.docs.numReturn = 10;
		
		// Build a template doc control pojo
		DocumentQueueControlPojo query = new DocumentQueueControlPojo();
		query.setQueueType(DocumentQueueControlPojo.DocQueueType.SavedQueryQueue);
		query.setQueueName("test" + testNum);
		query.setQueryInfo(new DocumentQueueControlPojo.SavedQueryInfo());
		query.getQueryInfo().setQuery(queryObj);

		if (4 == testNum) { // daily
			query.getQueryInfo().setFrequency(DocumentQueueControlPojo.SavedQueryInfo.DocQueueFrequency.Daily);		
			query.getQueryInfo().setFrequencyOffset(18);
			query.setMaxDocs(5);
		}
		else if (5 == testNum) { // weekly
			query.getQueryInfo().setFrequency(DocumentQueueControlPojo.SavedQueryInfo.DocQueueFrequency.Weekly);		
			query.getQueryInfo().setFrequencyOffset(6);
			query.setLastDocIdInserted(new ObjectId("509c04a81fda81052a25d660"));
		}
		else { // hourly
			query.getQueryInfo().setFrequency(DocumentQueueControlPojo.SavedQueryInfo.DocQueueFrequency.Hourly);		
			query.getQueryInfo().setFrequencyOffset(1);
		}		
		if (3 == testNum) {
			query.setLastDocIdInserted(new ObjectId("509c04a81fda81052a25d660"));
			query.getQueryInfo().getQuery().logic = "1";
			//3a: hourly
			query.getQueryInfo().setLastRun(new Date(new Date().getTime() - 4000000L));
			//3b: daily
			//query.getQueryInfo().setLastRun(new Date(new Date().getTime() - 27*3600*1000L));
			//query.getQueryInfo().setFrequency(DocQueueFrequency.Daily);
			//query.getQueryInfo().setFrequencyOffset(null);
			//3c: weekly
			//query.getQueryInfo().setLastRun(new Date(new Date().getTime() - 8*24*3600*1000L));
			//query.getQueryInfo().setFrequency(DocQueueFrequency.Weekly);
			//query.getQueryInfo().setFrequencyOffset(null);
		}		
		if (6 == testNum) {
			query.getQueryInfo().setQueryId(fixedShareId_2);
			
			SharePojo queryShare = new SharePojo();
			queryShare.set_id(fixedShareId_2);
			queryShare.setType("infinite-saved-query");
			queryShare.setTitle("test query " + testNum);
			queryShare.setDescription("test query " + testNum);
			queryShare.setShare(query.getQueryInfo().getQuery().toApi());		
			addSocial(queryShare);
			DbManager.getSocial().getShare().save(queryShare.toDb());

			query.getQueryInfo().setQuery(null);
		}
		
		// Build a template share
		SharePojo share = new SharePojo();
		if (testNum == 2) {
			share.set_id(fixedShareId_2);		
			query.setQueueList(new ArrayList<ObjectId>(5));
			for (int i = 0; i < 5; ++i) {
				query.getQueueList().add(new ObjectId());
			}
		}
		else {
			share.set_id(fixedShareId_1);
		}
		share.setType(DocumentQueueControlPojo.SavedQueryQueue);
		share.setTitle("test" + testNum);
		share.setDescription("test1" + testNum);
		share.setShare(query.toApi());		
		addSocial(share);
		
		DbManager.getSocial().getShare().save(share.toDb());
	}
	private static void addSocial(SharePojo share) {
		ShareOwnerPojo person = new ShareOwnerPojo();
		person.set_id(new ObjectId("4e3706c48d26852237078005")); // (admin)
		person.setEmail("infinite_default@ikanow.com");
		person.setDisplayName("infinite_default@ikanow.com");
		share.setOwner(person);
		share.setCommunities(new ArrayList<ShareCommunityPojo>(2));
		ShareCommunityPojo community = new ShareCommunityPojo();
		community.set_id(new ObjectId("4c927585d591d31d7b37097a"));
		community.setName("System");
		share.getCommunities().add(community);
		community = new ShareCommunityPojo();
		community.set_id(new ObjectId("4e3706c48d26852237078005"));
		community.setName("Personal");		
		share.getCommunities().add(community);				
	}
}
