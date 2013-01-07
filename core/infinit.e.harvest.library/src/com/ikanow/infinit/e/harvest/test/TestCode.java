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
package com.ikanow.infinit.e.harvest.test;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

import com.google.gson.GsonBuilder;
import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.config.source.SimpleTextCleanserPojo;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.ikanow.infinit.e.data_model.store.config.source.UnstructuredAnalysisConfigPojo;
import com.ikanow.infinit.e.data_model.store.config.source.UnstructuredAnalysisConfigPojo.Context;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.infinit.e.data_model.Globals;
import com.ikanow.infinit.e.data_model.Globals.Identity;
import com.ikanow.infinit.e.harvest.HarvestController;
import com.ikanow.infinit.e.harvest.utils.ProxyManager;
import com.mongodb.BasicDBObject;

@SuppressWarnings("unused")
public class TestCode {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		
		// Configuration:
		
		System.out.println(Arrays.toString(args));
		Globals.setIdentity(com.ikanow.infinit.e.data_model.Globals.Identity.IDENTITY_SERVICE);
		Globals.overrideConfigLocation(args[0]);

		// Check proxy:
		ProxyManager.getProxy(new URL("http://www.ikanow.com"), null);
		
		// TESTING
		
		HarvestController harvester = new HarvestController();
		//harvester.setStandaloneMode(0);
		harvester.setStandaloneMode(5);
		
		List<DocumentPojo> toAdd = new LinkedList<DocumentPojo>();
		List<DocumentPojo> toUpdate = new LinkedList<DocumentPojo>();
		List<DocumentPojo> toRemove = new LinkedList<DocumentPojo>();
		
		BasicDBObject query = null;
		SourcePojo feedSource = null;
		
		// 1. Get documents from a "feed" source
		
		// 1.1 OPENCALAIS		
//		toAdd.clear();
//		toUpdate.clear();
//		toRemove.clear();
//		query = new BasicDBObject("extractType", "Feed");
//		feedSource = SourcePojo.fromDb(DbManager.getConfig().getSource().findOne(query), SourcePojo.class);
//		feedSource.getHarvestConfig().setHarvested(null);
//		System.out.println("RSS1_SOURCE=" + feedSource.getUrl());
//		harvester.harvestSource(feedSource, toAdd, toUpdate, toRemove);
//		System.out.println("RSS1_STATUS=" + new GsonBuilder().setPrettyPrinting().create().toJson(feedSource.getHarvestConfig()));
//		System.out.println("RSS1_TOADD (" + toAdd.size() + "):");
//		if (toAdd.size() > 0) {
//			System.out.println("RSS1_EGDOC=" + new GsonBuilder().setPrettyPrinting().create().toJson(toAdd.get(0)));
//		}
//		System.out.println("RSS1_TOUPDATE (" + toUpdate.size() + ").");
//		System.out.println("RSS1_TOREMOVE (" + toRemove.size() + ").");
		
		// 1.2 ALCHEMYAPI
//		toAdd.clear();
//		toUpdate.clear();
//		toRemove.clear();
//		query = new BasicDBObject("extractType", "Feed");
//		query.put("useExtractor", "alchemyapi");
//		feedSource = SourcePojo.fromDb(DbManager.getConfig().getSource().findOne(query), SourcePojo.class);
//		feedSource.getHarvestConfig().setHarvested(null);
//		System.out.println("RSS2_SOURCE=" + feedSource.getUrl());
//		harvester.harvestSource(feedSource, toAdd, toUpdate, toRemove);
//		System.out.println("RSS2_STATUS=" + new GsonBuilder().setPrettyPrinting().create().toJson(feedSource.getHarvestConfig()));
//		System.out.println("RSS2_TOADD (" + toAdd.size() + "):");
//		if (toAdd.size() > 0) {
//			System.out.println("RSS2_EGDOC=" + new GsonBuilder().setPrettyPrinting().create().toJson(toAdd.get(0)));
//		}
//		System.out.println("RSS2_TOUPDATE (" + toUpdate.size() + ").");
//		System.out.println("RSS2_TOREMOVE (" + toRemove.size() + ").");
		
		// 2. Get documents from a "database" source
//		toAdd.clear();
//		toUpdate.clear();
//		toRemove.clear();
//		query = new BasicDBObject("extractType", "Database");
//		feedSource = SourcePojo.fromDb(DbManager.getConfig().getSource().findOne(query), SourcePojo.class);
//		feedSource.getHarvestConfig().setHarvested(null);
//		feedSource.getDatabaseConfig().setDeltaQuery("SELECT * FROM IncidentReport LIMIT 10");
//		feedSource.getDatabaseConfig().setDeleteQuery("SELECT * FROM IncidentReport LIMIT 2");
//		System.out.println("DB1_SOURCE=" + feedSource.getUrl());
//		harvester.harvestSource(feedSource, toAdd, toUpdate, toRemove);
//		System.out.println("DB1_STATUS=" + new GsonBuilder().setPrettyPrinting().create().toJson(feedSource.getHarvestConfig()));
//		System.out.println("DB1_TOADD (" + toAdd.size() + "):");
//		if (toAdd.size() > 0) {
//			System.out.println("DB1_EGDOC=" + new GsonBuilder().setPrettyPrinting().create().toJson(toAdd.get(0)));
//		}
//		System.out.println("DB1_TOUPDATE (" + toUpdate.size() + ").");
//		System.out.println("DB1_TOREMOVE (" + toRemove.size() + ").");
//		if (toRemove.size() > 0) {
//			System.out.println("DB1_TOREMOVE=" + new GsonBuilder().setPrettyPrinting().create().toJson(toRemove.get(0)));
//		}
		
		// 3. Get documents from a "file" source (non-XML)
		
		// 3.1. Modus test dataset (also checks UAH code still called)
//		toAdd.clear();
//		toUpdate.clear();
//		toRemove.clear();
//		query = new BasicDBObject("useExtractor", "ModusOperandi");
//		feedSource = SourcePojo.fromDb(DbManager.getConfig().getSource().findOne(query), SourcePojo.class);
//		feedSource.getHarvestConfig().setHarvested(null);
//		System.out.println("FILE1_SOURCE=" + feedSource.getUrl());
//		harvester.harvestSource(feedSource, toAdd, toUpdate, toRemove);
//		System.out.println("FILE1_STATUS=" + new GsonBuilder().setPrettyPrinting().create().toJson(feedSource.getHarvestConfig()));
//		System.out.println("FILE1_TOADD (" + toAdd.size() + "):");
//		if (toAdd.size() > 0) {
//			System.out.println("FILE1_EGDOC=" + new GsonBuilder().setPrettyPrinting().create().toJson(toAdd.get(0)));
//		}
//		System.out.println("FILE1_TOUPDATE (" + toUpdate.size() + ").");
//		if (toUpdate.size() > 0) {
//			System.out.println("FILE1_TOUPDATE=" + new GsonBuilder().setPrettyPrinting().create().toJson(toUpdate.get(0)));
//		}
//		System.out.println("FILE1_TOREMOVE (" + toRemove.size() + ").");
//		if (toRemove.size() > 0) {
//			System.out.println("FILE1_TOREMOVE=" + new GsonBuilder().setPrettyPrinting().create().toJson(toRemove.get(0)));
//		}
				
		// 4. Get documents from a "file" source (XML)
		
		// 4.1. WITS dataset, also checks SAH code still called
//		toAdd.clear();
//		toUpdate.clear();
//		toRemove.clear();
//		query = new BasicDBObject("url", "smb://modus:139/wits/allfiles/");
//		feedSource = SourcePojo.fromDb(DbManager.getConfig().getSource().findOne(query), SourcePojo.class);
//		feedSource.getHarvestConfig().setHarvested(null);
//		System.out.println("FILE2_SOURCE=" + feedSource.getUrl());
//		harvester.harvestSource(feedSource, toAdd, toUpdate, toRemove);
//		System.out.println("FILE2_STATUS=" + new GsonBuilder().setPrettyPrinting().create().toJson(feedSource.getHarvestConfig()));
//		System.out.println("FILE2_TOADD (" + toAdd.size() + "):");
//		if (toAdd.size() > 0) {
//			System.out.println("FILE2_EGDOC=" + new GsonBuilder().setPrettyPrinting().create().toJson(toAdd.get(0)));
//		}
//		System.out.println("FILE2_TOUPDATE (" + toUpdate.size() + ").");
//		if (toUpdate.size() > 0) {
//			System.out.println("FILE2_TOUPDATE=" + new GsonBuilder().setPrettyPrinting().create().toJson(toUpdate.get(0)));
//		}
//		System.out.println("FILE2_TOREMOVE (" + toRemove.size() + ").");
//		if (toRemove.size() > 0) {
//			System.out.println("FILE2_TOREMOVE=" + new GsonBuilder().setPrettyPrinting().create().toJson(toRemove.get(0)));
//		}
		
		// 5. Test communities with multiple sources
//		toAdd.clear();
//		toUpdate.clear();
//		toRemove.clear();
//		query = new BasicDBObject("extractType", "Feed");
//		// A useful source known to work during V0S1 testing:
//		//query = new BasicDBObject("key", "http.www.stjude.org.stjude.rss.medical_science_news_rss.xml");
//		feedSource = SourcePojo.fromDb(DbManager.getConfig().getSource().findOne(query), SourcePojo.class);
//		feedSource.addToCommunityIDs("test_dup1a");
//		feedSource.addToCommunityIDs("test_dup1b");
//		System.out.println("DUP1 feedSource=" + feedSource.getKey() + " communities=" + new com.google.gson.Gson().toJson(feedSource.getCommunityIDs()));
//		harvester.harvestSource(feedSource, toAdd, toUpdate, toRemove);
//
//		// Check for duplicate sources...
//		System.out.println("DUP1");
//		System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(toAdd));
//		for (DocumentPojo showContent: toAdd) {
//			System.out.println("DUP1 text for " + showContent.getUrl() + ":" + showContent.getFullText().substring(0, 64));			
//		}
		
		// 6. Test duplication across sources
		// Need a "non-standalone" harvester so it will actually test the duplication
		// The idea here will be to run the normal harvester once on a source and then rerun
//		toAdd.clear();
//		toUpdate.clear();
//		toRemove.clear();
//		query = new BasicDBObject("key", "http.www.stjude.org.stjude.rss.medical_science_news_rss.xml"); // ie run the harvester against this source before testing
//		feedSource = SourcePojo.fromDb(DbManager.getConfig().getSource().findOne(query), SourcePojo.class);
//		feedSource.setCommunityIDs(new TreeSet<String>());
//		feedSource.addToCommunityIDs("test_dup2a");
//		feedSource.addToCommunityIDs("test_dup2b");
//		feedSource.setKey("DUP2_TEST_"+feedSource.getKey());
//		new HarvestController().harvestSource(feedSource, toAdd, toUpdate, toRemove);
//		System.out.println("DUP2");
//		System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(toAdd));
//		for (DocumentPojo showContent: toAdd) {
//			System.out.println("DUP2 text for " + showContent.getUrl() + ":" + showContent.getFullText().substring(0, 64));			
//		}
		
		
		// 7. The UAH now allows arbitrary scripts to be run vs the content ... to generate metadata - the
		//    SAH then can run arbitrary scripts to run vs the metadata to generate entities and associations (phew!)
		query = new BasicDBObject("extractType", "Feed");
		// A useful source known to work during V0S1 testing:
		//query = new BasicDBObject("key", "http.www.stjude.org.stjude.rss.medical_science_news_rss.xml");
		feedSource = SourcePojo.fromDb(DbManager.getIngest().getSource().findOne(query), SourcePojo.class);
		// Add markup to feed source:
		UnstructuredAnalysisConfigPojo uah = new UnstructuredAnalysisConfigPojo();
		uah.setSimpleTextCleanser(new LinkedList<SimpleTextCleanserPojo>());
		SimpleTextCleanserPojo textCleanse1 = new SimpleTextCleanserPojo();
		textCleanse1.setField("description");
		textCleanse1.setScript("[aeiou]");
		textCleanse1.setReplacement("XXX");
		uah.getSimpleTextCleanser().add(textCleanse1);
		SimpleTextCleanserPojo textCleanse2 = new SimpleTextCleanserPojo();
		textCleanse2.setField("title");
		textCleanse2.setScript("[aeiou]");
		textCleanse2.setReplacement("YYY");
		uah.getSimpleTextCleanser().add(textCleanse2);
		SimpleTextCleanserPojo textCleanse3 = new SimpleTextCleanserPojo();
		textCleanse3.setField("fulltext");
		textCleanse3.setScript("[aeiou]");
		textCleanse3.setReplacement("ATCPSQZ");
		uah.getSimpleTextCleanser().add(textCleanse3);
		uah.AddMetaField("TEST1", Context.All, "var a = ['alex']; a;", "javascript");
		uah.AddMetaField("TEST2", Context.All, "var a = { 'test': 'alex' }; a;", "javascript");
		uah.AddMetaField("TEST3", Context.All, "var a = [ { 'test': 'alex' }, 'chris' ]; a;", "javascript");
		uah.AddMetaField("TEST4", Context.All, "var a = [ { 'test': { 's1': 'alex', 's2':['chris','craig'] } }, [ 'chris', 'alex' ] ]; a;", "javascript");
		uah.AddMetaField("TEST5", Context.All, "var a = [ { 'test': { 's1': 'alex', 's2':['chris','craig'] } }, [ 'chris', 'alex' ] ]; null;", "javascript");
		uah.AddMetaField("TEST6", Context.All, "if (-1 == text.indexOf('ATCPSQZ')) true; else false; ", "javascript");
		feedSource.setUnstructuredAnalysisConfig(uah);
		// Run harvester:
		toAdd.clear();
		toUpdate.clear();
		toRemove.clear();
		harvester.harvestSource(feedSource, toAdd, toUpdate, toRemove);
		// Check results:
		if (toAdd.size() > 0) {
			DocumentPojo doc = toAdd.get(0);
			// Check text cleansing:
			if (!doc.getDescription().contains("XXX")) {
				System.out.println("UAH: ******** FAIL: title not subbed: " + doc.getTitle());				
			}
			if (!doc.getTitle().contains("YYY")) {
				System.out.println("UAH: ******** FAIL: title not subbed: " + doc.getTitle());				
			}
			Object[] fullTextSubTest = doc.getMetadata().get("TEST6");
			if ((null != fullTextSubTest) && (1 == fullTextSubTest.length)) {
				Boolean bFullTextSubTest = (Boolean)fullTextSubTest[0];
				if ((null == bFullTextSubTest) || (!bFullTextSubTest)) {
					System.out.println("UAH: ******** FAIL: full text not subbed (or scripts not working) 1");									
				}
			}
			else {
				System.out.println("UAH: ******** FAIL: full text not subbed (or scripts not working) 2");				
			}
			// Check fields
			String test1 = new com.google.gson.Gson().toJson(doc.getMetadata().get("TEST1"));
			System.out.println("UAH TEST1: " + test1);
			if (!test1.equals("[\"alex\"]")) System.out.println("UAH: ******** FAIL: TEST1");
			String test2 = new com.google.gson.Gson().toJson(doc.getMetadata().get("TEST2"));
			System.out.println("UAH TEST2: " + new com.google.gson.Gson().toJson(doc.getMetadata().get("TEST2")));
			if (!test2.equals("[{\"test\":\"alex\"}]")) System.out.println("UAH: ******** FAIL: TEST2");
			String test3 = new com.google.gson.Gson().toJson(doc.getMetadata().get("TEST3"));
			System.out.println("UAH TEST3: " + new com.google.gson.Gson().toJson(doc.getMetadata().get("TEST3")));
			if (!test3.equals("[{\"test\":\"alex\"},\"chris\"]")) System.out.println("UAH: ******** FAIL: TEST3");
			String test4 = new com.google.gson.Gson().toJson(doc.getMetadata().get("TEST4"));
			System.out.println("UAH TEST4: " + new com.google.gson.Gson().toJson(doc.getMetadata().get("TEST4")));
			if (!test4.equals("[{\"test\":{\"s2\":[\"chris\",\"craig\"],\"s1\":\"alex\"}},[\"chris\",\"alex\"]]")) System.out.println("UAH: ******** FAIL: TEST4");
			if (null != doc.getMetadata().get("TEST5")) {
				System.out.println("UAH: ******** FAIL: TEST5 should not be present");								
			}
			//(test6 tested above)
		}
		else {
			System.out.println("UAH: ******** FAIL: no documents to check");
		}
		System.out.println("UAH: (all tests completed)");
	}
}
