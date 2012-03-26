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
package com.ikanow.infinit.e.processing.generic.test;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;

import com.google.gson.GsonBuilder;
import com.ikanow.infinit.e.data_model.InfiniteEnums;
import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.ikanow.infinit.e.data_model.store.document.AssociationPojo;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.infinit.e.data_model.Globals;
import com.ikanow.infinit.e.data_model.Globals.Identity;
import com.ikanow.infinit.e.harvest.HarvestController;
import com.ikanow.infinit.e.processing.generic.GenericProcessingController;
import com.ikanow.infinit.e.processing.generic.aggregation.AggregationManager;
import com.ikanow.infinit.e.processing.generic.store_and_index.StoreAndIndexManager;
import com.mongodb.BasicDBObject;

@SuppressWarnings("unused")
public class TestCode {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {

		System.out.println(Arrays.toString(args));
		Globals.setIdentity(Identity.IDENTITY_SERVICE);
		Globals.overrideConfigLocation(args[0]);
		
		// "-1"] Test initialization
		new GenericProcessingController().Initialize();
		
		// 0] Preparation: use the harvest library to get various sets of files to test out...
		HarvestController hc = new HarvestController();
		hc.setStandaloneMode(5);
		LinkedList<DocumentPojo> toAdd_feed = new LinkedList<DocumentPojo>();
		LinkedList<DocumentPojo> toUpdate_feed = new LinkedList<DocumentPojo>();
		LinkedList<DocumentPojo> toDelete_feed = new LinkedList<DocumentPojo>();
		BasicDBObject query = new BasicDBObject("extractType", "Feed");
		// A useful source known to work during V0S1 testing:
		//BasicDBObject query = new BasicDBObject("key", "http.www.stjude.org.stjude.rss.medical_science_news_rss.xml");
		
		SourcePojo feedSource = SourcePojo.fromDb(DbManager.getIngest().getSource().findOne(query), SourcePojo.class);
		hc.harvestSource(feedSource, toAdd_feed, toUpdate_feed, toDelete_feed);
		System.out.println("############# Retrieved sample feed documents");
		
		// 1] Test the store and index manager by itself:
		
		StoreAndIndexManager.setDiagnosticMode(true);

//		// Test all public calls
//		StoreAndIndexManager storeManager = new StoreAndIndexManager();
//		storeManager.addToDatastore(toAdd_feed, true);
//		// (addToSearch called from addToDatastore so not duplicated here)
//		storeManager.removeFromDatastore_byURL(toAdd_feed, true);
//		storeManager.removeFromDatastore_byId(toAdd_feed, true);
//		// (removeFromSearch called from removeFromDatastore_byId so not duplicated here)
//				// Finally, test the index switching/bulk indexing logic standalone:
//		if (toAdd_feed.size() > 4) {
//			toAdd_feed.get(2).setIndex("document_index");
//			toAdd_feed.get(3).setIndex(null);
//			storeManager.addToSearch(toAdd_feed);
//		}
//		if (toAdd_feed.size() > 4) {
//			toAdd_feed.get(2).setIndex(toAdd_feed.get(0).getIndex());
//			toAdd_feed.get(3).setIndex(toAdd_feed.get(0).getIndex());
//			toAdd_feed.get(0).setIndex(null);
//			toAdd_feed.get(3).setIndex(null);
//			storeManager.removeFromSearch(toAdd_feed);
//		}		
//		if (toAdd_feed.size() < 5) {
//			System.out.println("************** Couldn't test StoreAndIndexManager indexing fully because <5 docs");			
//		}		
//		System.out.println("############# Tested StoreAndIndexManager calls");
			
		// 2] Now run through a full store/aggregation cycle
		
		// 2.0] Need to rerun the harvester because adding/removing from index changes to docs
//		toAdd_feed.clear(); toUpdate_feed.clear(); toDelete_feed.clear();
//		hc.harvestSource(feedSource, toAdd_feed, toUpdate_feed, toDelete_feed);
//		System.out.println("############# ReRetrieved sample feed documents");

		// 2.1] Logic:
		
		AggregationManager.setDiagnosticMode(true);
		GenericProcessingController pxControl_feed = new GenericProcessingController(); 
		// No need to do this, thoroughly tested during beta (and a bit slow)
		//pxControl.Initialize();
		
		// Comment in the desired test...
		
		// 2.1.1] Only adding:
		// (+Check that the feeds' entities statistics have been updated:)
//		pxControl_feed.processDocuments(InfiniteEnums.FEEDS, toAdd_feed, toUpdate_feed, toDelete_feed); // (add, update, delete)
//		System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(toAdd_feed));
		// 2.1.2] Updating:
//		toUpdate_feed.add(toAdd_feed.get(0));
//		pxControl_feed.processDocuments(InfiniteEnums.FEEDS, toAdd_feed, toUpdate_feed, toDelete_feed); // (add, update, delete)
		// 2.1.3] Deleting some docs, adding others
//		toDelete_feed.add(toAdd_feed.pop());
//		pxControl_feed.processDocuments(InfiniteEnums.FEEDS, toAdd_feed, toUpdate_feed, toDelete_feed); // (add, update, delete)
		// 2.1.4] More logic:
		DocumentPojo doc1 = null;
		for (DocumentPojo doc: toAdd_feed) { 
			if ((null != doc.getEntities()) && (doc.getEntities().size() > 2)) {
				if ((null != doc.getAssociations()) && (doc.getAssociations().size() > 2)) {
					doc1 = doc;
					break;
				}				
			}
		}
		if (null == doc1) {
			System.out.println("!!!!!!!!!!!!!!!!! FAILED TO FIND DOC FOR AGGREGATION LOGIC TEST");
		}
		// 2.1.4.1] Only adding, 1 new entity, 1 old entity with new alias, 1 new event, 1 old event with new verb
//		doc1.getEntities().get(0).setDisambiguatedName("TestingTesting123");
//		doc1.getEntities().get(0).setIndex("testingtesting123/person");
//		doc1.getEntities().get(1).setActual_name("TestingTesting123"); 
//		doc1.getAssociations().get(0).setEntity1_index("testingtesting123/person");
//		doc1.getAssociations().get(1).setEntity1("testingtesting123");
//		pxControl_feed.processDocuments(InfiniteEnums.FEEDS, toAdd_feed, toUpdate_feed, toDelete_feed); // (add, update, delete)
		// 2.1.4.2] Only adding, 1 existing entity but with a new community
//		doc1.getCommunityIds().add("alextest");
//		toAdd_feed.clear();
//		toAdd_feed.add(doc1);
//		pxControl_feed.processDocuments(InfiniteEnums.FEEDS, toAdd_feed, toUpdate_feed, toDelete_feed); // (add, update, delete)
		// 2.1.4.3a] Check that large association truncation code works
//		StringBuffer sb = new StringBuffer(); 
//		for (int i = 0; i < 60; ++i) sb.append("word").append(i).append(' ');
//		doc1.getAssociations().get(0).setEntity1(sb.toString());
//		doc1.getAssociations().get(1).setEntity2(sb.toString());
		// 2.1.4.3b] Check that large number of association fields works
		AssociationPojo assoc3 = doc1.getAssociations().get(1);
//		for (int i = 0; i < 524; ++i) {
//			AssociationPojo newAssoc = AssociationPojo.fromDb(assoc3.toDb(), AssociationPojo.class); //(just a dumb clone)
//			newAssoc.setEntity1("test1_" + i);
//			newAssoc.setEntity2("test2_" + i);
//			doc1.getAssociations().add(newAssoc);
//		}
//		toAdd_feed.clear();
//		toAdd_feed.add(doc1);
//		pxControl_feed.processDocuments(InfiniteEnums.FEEDS, toAdd_feed, toUpdate_feed, toDelete_feed); // (add, update, delete)
		System.out.println("############# Tested GenericProcessingController calls, feed");
	}

}
