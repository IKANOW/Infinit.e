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
package com.ikanow.infinit.e.data_model.store;

import java.net.UnknownHostException;
import java.util.Date;

import com.ikanow.infinit.e.data_model.utils.PropertiesManager;
import com.mongodb.BasicDBObject;
import com.mongodb.CommandResult;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCollectionProxyFactory;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;
import com.mongodb.gridfs.GridFS;

// Encapsulates Mongo Databases and collections

public class MongoDbManager {

	// MongoDB commands:
	public static final String regex_ = "$regex"; 
	public static final String exists_ = "$exists"; 
	public static final String min_ = "$min"; 
	public static final String max_ = "$max"; 
	public static final String and_ = "$and"; 
	public static final String not_ = "$not"; 
	public static final String nor_ = "$nor"; 
	public static final String or_ = "$or"; 
	public static final String ne_ = "$ne"; 
	public static final String gt_ = "$gt"; 
	public static final String gte_ = "$gte"; 
	public static final String lt_ = "$lt"; 
	public static final String lte_ = "$lte"; 
	public static final String all_ = "$all"; 
	public static final String in_ = "$in";
	public static final String nin_ = "$nin"; 
	public static final String each_ = "$each"; 
	public static final String set_ = "$set"; 
	public static final String unset_ = "$unset"; 
	public static final String inc_ = "$inc"; 
	public static final String addToSet_ = "$addToSet"; 
	public static final String pull_ = "$pull"; 
	public static final String pullAll_ = "$pullAll"; 
	public static final String push_ = "$push"; 
	public static final String pushAll_ = "$pushAll"; 
	public static final String pop_ = "$pop"; 
	public static final String size_ = "$size";
	public static final String where_ = "$where";
	// Other keywords:
	public static final String sparse_ = "sparse";
	
	protected MongoDbManager() {}
	
	// 1. MongoDB connection management
	
	private static ThreadLocal<MongoDbConnection> _connections = new ThreadLocal<MongoDbConnection>() {
        @Override protected MongoDbConnection initialValue() {
            try {
				return new MongoDbConnection(new PropertiesManager());
			} catch (Exception e) {
				try {
					return new MongoDbConnection(); // (default to localhost:27017)
				} 
				catch (Exception e1) {
					return null;
				} 
			}
        }
	};
	
	private static DB initializeFastWriteDB(String dbName, Mongo savedMongo) {
		DB db = savedMongo.getDB(dbName);
		//(keep performance in line with 2.4)
		db.setWriteConcern(WriteConcern.UNACKNOWLEDGED);
		return db;
	}
	
	// Generic database access:
	
	public static DB getDB(String dbName) {
		return _connections.get().getMongo().getDB(dbName);
	}
	public static DBCollection getCollection(String dbName, String collectionName) {
		return DBCollectionProxyFactory.get(_connections.get().getMongo().getDB(dbName).getCollection(collectionName));
	}
	
	// 2. MongoDB database management

	// 2.1. Social DB
	
	private static ThreadLocal<SocialMongoDb> _social = new ThreadLocal<SocialMongoDb>() {
        @Override protected SocialMongoDb initialValue() {
        	return new SocialMongoDb(_connections.get().getMongo());
        }
	};
	public static SocialMongoDb getSocial() {
		return _social.get();
	}	
	
	// 2.2. Document database
	
	private static ThreadLocal<DocumentMongoDb> _doc = new ThreadLocal<DocumentMongoDb>() {
        @Override protected DocumentMongoDb initialValue() {
        	return new DocumentMongoDb(_connections.get().getMongo());
        }
	};
	public static DocumentMongoDb getDocument() {
		return _doc.get();
	}	
	
	// 2.3. Feature database
	
	private static ThreadLocal<FeatureMongoDb> _feature = new ThreadLocal<FeatureMongoDb>() {
        @Override protected FeatureMongoDb initialValue() {
        	return new FeatureMongoDb(_connections.get().getMongo());
        }
	};
	public static FeatureMongoDb getFeature() {
		return _feature.get();
	}	
	
	// 3.4. Config database
	
	private static ThreadLocal<IngestMongoDb> _ingest = new ThreadLocal<IngestMongoDb>() {
        @Override protected IngestMongoDb initialValue() {
        	return new IngestMongoDb(_connections.get().getMongo());
        }
	};
	public static IngestMongoDb getIngest() {
		return _ingest.get();
	}	
	
	//CALEB NUMBERING SCHEME HERE
	private static ThreadLocal<CustomMongoDb> _custom = new ThreadLocal<CustomMongoDb>() {
        @Override protected CustomMongoDb initialValue() {
        	return new CustomMongoDb(_connections.get().getMongo());
        }
	};
	public static CustomMongoDb getCustom() {
		return _custom.get();
	}	
	
	// 3. Database-specific collection management
	
	// 3.1. Social collections
	
	public static class SocialMongoDb {		
		SocialMongoDb(Mongo mongo) { _savedMongo = mongo; }		
		private Mongo _savedMongo;
		
		private DBCollection _social_person;
		private DBCollection _social_community;
		private DBCollection _social_communityapprove;
		
		private DBCollection _social_authentication;
		private DBCollection _social_cookies;

		private DBCollection _social_share;
		private GridFS _social_share_binary;

		// Keeping these under social as intend to move them all to using share
		private DBCollection _social_gui_modules;
		private DBCollection _social_gui_favmodules;
		private DBCollection _social_gui_setup;		
		
		public DBCollection getPerson() {
			if (null == _social_person) {
				_social_person = DBCollectionProxyFactory.get(_savedMongo.getDB("social").getCollection("person"));
			}
			return _social_person;
		}
		public DBCollection getCommunity() {
			if (null == _social_community) {
				_social_community = DBCollectionProxyFactory.get(_savedMongo.getDB("social").getCollection("community"));
			}
			return _social_community;
		}
		public DBCollection getCommunityApprove() {
			if (null == _social_communityapprove) {
				_social_communityapprove = DBCollectionProxyFactory.get(_savedMongo.getDB("social").getCollection("communityapprove"));
			}
			return _social_communityapprove;
			
		}
		public DBCollection getAuthentication() {
			if (null == _social_authentication) {
				_social_authentication = DBCollectionProxyFactory.get(_savedMongo.getDB("security").getCollection("authentication"));
			}
			return _social_authentication;
		}
		public DBCollection getCookies() {
			if (null == _social_cookies) {
				_social_cookies = DBCollectionProxyFactory.get(_savedMongo.getDB("security").getCollection("cookies"));
			}
			return _social_cookies;
		}
		public DBCollection getShare() {
			if (null == _social_share) {
				_social_share = DBCollectionProxyFactory.get(_savedMongo.getDB("social").getCollection("share"));
			}
			return _social_share;
		}
		public GridFS getShareBinary() {
			if (null == _social_share_binary) {
				_social_share_binary = new GridFS(_savedMongo.getDB("file"), "binary_shares");					
			}
			return _social_share_binary;
		}
		public DBCollection getUIModules() {
			if (null == _social_gui_modules) {
				_social_gui_modules = DBCollectionProxyFactory.get(_savedMongo.getDB("gui").getCollection("modules"));					
			}
			return _social_gui_modules;
		}
		public DBCollection getUIFavoriteModules() {
			if (null == _social_gui_favmodules) {
				_social_gui_favmodules = DBCollectionProxyFactory.get(_savedMongo.getDB("gui").getCollection("favmodules"));					
			}
			return _social_gui_favmodules;
		}
		public DBCollection getUISetup() {
			if (null == _social_gui_setup) {
				_social_gui_setup = DBCollectionProxyFactory.get(_savedMongo.getDB("gui").getCollection("setup"));					
			}
			return _social_gui_setup;
		}
	}
	
	// 3.2. Document collections
	
	public static class DocumentMongoDb {
		DocumentMongoDb(Mongo mongo) { _savedMongo = mongo; }		
		private Mongo _savedMongo;
		
		private DBCollection _document_metadata;
		private DBCollection _document_content;
		private DBCollection _document_counts;
		
		public DBCollection getMetadata() {
			if (null == _document_metadata) {
				_document_metadata = DBCollectionProxyFactory.get(initializeFastWriteDB("doc_metadata", _savedMongo).getCollection("metadata"));					
			}
			return _document_metadata;
		}
		public DBCollection getContent() {
			if (null == _document_content) {
				_document_content = DBCollectionProxyFactory.get(initializeFastWriteDB("doc_content", _savedMongo).getCollection("gzip_content"));					
			}
			return _document_content;
		}
		public DBCollection getCounts() {
			if (null == _document_counts) {
				_document_counts = DBCollectionProxyFactory.get(initializeFastWriteDB("doc_metadata", _savedMongo).getCollection("doc_counts"));					
			}
			return _document_counts;			
		}
	}
	
	// 3.3. Feature collections
	
	public static class FeatureMongoDb {	
		FeatureMongoDb(Mongo mongo) { _savedMongo = mongo; }		
		private Mongo _savedMongo;
	
		private DBCollection _feature_entity;
		private DBCollection _feature_assoc;
		private DBCollection _feature_geo;
		private DBCollection _feature_sync_lock;
		private DBCollection _feature_agg_lock;
		
		public DBCollection getEntity() {
			if (null == _feature_entity) {
				_feature_entity = DBCollectionProxyFactory.get(initializeFastWriteDB("feature", _savedMongo).getCollection("entity"));					
			}
			return _feature_entity;
		}
		public DBCollection getAssociation() {
			if (null == _feature_assoc) {
				_feature_assoc = DBCollectionProxyFactory.get(initializeFastWriteDB("feature", _savedMongo).getCollection("association"));					
			}
			return _feature_assoc;
		}
		public DBCollection getGeo() {
			if (null == _feature_geo) {
				_feature_geo = DBCollectionProxyFactory.get(initializeFastWriteDB("feature", _savedMongo).getCollection("geo"));					
			}
			return _feature_geo;
		}
		public DBCollection getSyncLock() { // (Used to synchronize batch operations performed via script - manually ack writes to/from this)
			if (null == _feature_sync_lock) {
				_feature_sync_lock = DBCollectionProxyFactory.get(initializeFastWriteDB("feature", _savedMongo).getCollection("sync_lock"));					
			}
			return _feature_sync_lock;			
		}
		public DBCollection getAggregationLock() { // (Used to lock aggregation activities to one harvester - manually ack writes to/from this)
			if (null == _feature_agg_lock) {
				_feature_agg_lock = DBCollectionProxyFactory.get(initializeFastWriteDB("feature", _savedMongo).getCollection("agg_lock"));					
			}
			return _feature_agg_lock;			
		}
	}
	
	// 3.4. Config collections
	
	public static class IngestMongoDb {	
		IngestMongoDb(Mongo mongo) { _savedMongo = mongo; }		
		private Mongo _savedMongo;

		private DBCollection _ingest_source;
		private DBCollection _ingest_source_deletion_q;
		private DBCollection _ingest_log_harvester_q;
		private DBCollection _ingest_log_harvester_slaves;
		private DBCollection _ingest_federated_cache;
		
		public DBCollection getSource() {
			if (null == _ingest_source) {
				_ingest_source = DBCollectionProxyFactory.get(_savedMongo.getDB("ingest").getCollection("source"));										
			}
			return _ingest_source;
		}
		public DBCollection getLogHarvesterQ() {
			if (null == _ingest_log_harvester_q) {
				_ingest_log_harvester_q = DBCollectionProxyFactory.get(_savedMongo.getDB("ingest").getCollection("log_harvester_q"));										
			}
			return _ingest_log_harvester_q;
		}
		public DBCollection getSourceDeletionQ() {
			if (null == _ingest_source_deletion_q) {
				_ingest_source_deletion_q = DBCollectionProxyFactory.get(_savedMongo.getDB("ingest").getCollection("source_deletion_q"));										
			}
			return _ingest_source_deletion_q;
		}
		public DBCollection getLogHarvesterSlaves() {
			if (null == _ingest_log_harvester_slaves) {
				_ingest_log_harvester_slaves = DBCollectionProxyFactory.get(_savedMongo.getDB("ingest").getCollection("log_harvester_slaves"));										
			}
			return _ingest_log_harvester_slaves;
		}
		public DBCollection getFederatedCache() {
			if (null == _ingest_federated_cache) {
				_ingest_federated_cache = DBCollectionProxyFactory.get(_savedMongo.getDB("ingest").getCollection("federated_cache"));										
			}
			return _ingest_federated_cache;
		}
	}

	// 3.5 Custom processing collection
	
	public static class CustomMongoDb 
	{	
		CustomMongoDb(Mongo mongo) { _savedMongo = mongo; }		
		private Mongo _savedMongo;

		private DBCollection _config_customlookup;
		private DBCollection _config_customSavedQueryCache;
		
		public DBCollection getLookup() {
			if (null == _config_customlookup) {
				_config_customlookup = DBCollectionProxyFactory.get(_savedMongo.getDB("custommr").getCollection("customlookup"));										
			}
			return _config_customlookup;
		}
		public DBCollection getSavedQueryCache() {
			if (null == _config_customSavedQueryCache) {
				_config_customSavedQueryCache = DBCollectionProxyFactory.get(_savedMongo.getDB("custommr").getCollection("saved_query_cache"));										
			}
			return _config_customSavedQueryCache;
		}
	}
	
	// TEST CODE TO UNDERSTAND PERFORMANCE
	@SuppressWarnings("deprecation")
	public static void main(String[] args) throws UnknownHostException {
		MongoClient mc = new MongoClient(args[0]);
		long tnow = 0;		
		DB db = mc.getDB("test");
		DBCollection test = db.getCollection("test123");
		BasicDBObject outObj = new BasicDBObject();
		int ITS = 1000;
		test.drop();
		
		boolean checkPerformance = false;
		boolean checkFunctionality = false;
		boolean checkErrors = false;
		
		// 1] Performance
		
		if (checkPerformance) {
		
			// ack'd
			db.setWriteConcern(WriteConcern.ACKNOWLEDGED);
			test.drop();
			tnow = new Date().getTime();
			for (int i = 0; i < ITS; ++i) {
				outObj.remove("_id");
				outObj.put("val", i);
				test.save(outObj);
			}
			tnow = new Date().getTime() - tnow;
			System.out.println("1: Ack'd: " + tnow);
			
			// un ack'd
			db.setWriteConcern(WriteConcern.UNACKNOWLEDGED);
			test.drop();
			tnow = new Date().getTime();
			outObj = new BasicDBObject();
			for (int i = 0; i < ITS; ++i) {
				outObj.remove("_id");
				outObj.put("val", i);
				test.save(outObj);
			}
			tnow = new Date().getTime() - tnow;
			System.out.println("2: unAck'd: " + tnow);
			
			// un ack'd but call getLastError
			db.setWriteConcern(WriteConcern.UNACKNOWLEDGED);
			test.drop();
			tnow = new Date().getTime();
			outObj = new BasicDBObject();
			for (int i = 0; i < ITS; ++i) {
				outObj.remove("_id");
				outObj.put("val", i);
				test.save(outObj);
				db.getLastError();
			}
			tnow = new Date().getTime() - tnow;
			test.drop();
			System.out.println("3: unAck'd but GLEd: " + tnow);
			
			// ack'd override
			db.setWriteConcern(WriteConcern.UNACKNOWLEDGED);
			test.drop();
			tnow = new Date().getTime();
			outObj = new BasicDBObject();
			for (int i = 0; i < ITS; ++i) {
				outObj.remove("_id");
				outObj.put("val", i);
				test.save(outObj, WriteConcern.ACKNOWLEDGED);
				db.getLastError();
			}
			tnow = new Date().getTime() - tnow;
			System.out.println("4: unAck'd but ACKd: " + tnow);
			
			// Performance Results:
			// 2.6) (unack'd 100ms ... ack'd 27000)
			// 2.4) (same)
		}
		
		// 2] Functionality
		
		if (checkFunctionality) {
		
			// Unack:
			db.setWriteConcern(WriteConcern.UNACKNOWLEDGED);
			WriteResult wr = test.update(new BasicDBObject(), new BasicDBObject(DbManager.set_, new BasicDBObject("val2", "x")), false, true);
			CommandResult cr = db.getLastError();
			System.out.println("UNACK: wr: " + wr);
			System.out.println("UNACK: cr: " + cr);
			
			// bonus, check that we get N==0 when insert dup object
			WriteResult wr2 = test.insert(outObj);
			System.out.println("ACK wr2 = " + wr2.getN()  + " all = " +wr2);
			CommandResult cr2 = db.getLastError();
			System.out.println("ACK cr2 = " + cr2);
			
			// Ack1:
			db.setWriteConcern(WriteConcern.ACKNOWLEDGED);
			wr = test.update(new BasicDBObject(), new BasicDBObject(DbManager.set_, new BasicDBObject("val3", "x")), false, true);
			cr = db.getLastError();
			System.out.println("ACK1: wr: " + wr);
			System.out.println("ACK1: cr: " + cr);
			
			// Ack2:
			db.setWriteConcern(WriteConcern.UNACKNOWLEDGED);
			wr = test.update(new BasicDBObject(), new BasicDBObject(DbManager.set_, new BasicDBObject("val4", "x")), false, true, WriteConcern.ACKNOWLEDGED);
			cr = db.getLastError();
			System.out.println("ACK2: wr: " + wr);
			System.out.println("ACK2: cr: " + cr);
	
			// bonus, check that we get N==0 when insert dup object
			wr2 = test.insert(outObj);
			System.out.println("ACK wr2 = " + wr2.getN()  + " all = " +wr2);
			
			// Functionality results:
			// 2.6: unack wr == N/A, otherwise both have "n", "ok"
			// 2.4: unack wr == N/A all other wrs + crs identical 
		}
		
		if (checkErrors) {

			//set up sharding
			DbManager.getDB("admin").command(new BasicDBObject("enablesharding", "test"));			
			// Ack:
			try {
				test.drop();
				test.createIndex(new BasicDBObject("key", 1));
				BasicDBObject command1 = new BasicDBObject("shardcollection", "test.test123");
				command1.append("key", new BasicDBObject("key", 1));
				DbManager.getDB("admin").command(command1);
				
				db.setWriteConcern(WriteConcern.ACKNOWLEDGED);
				outObj = new BasicDBObject("key", "test");
				test.save(outObj);
				WriteResult wr = test.update(new BasicDBObject(), new BasicDBObject(DbManager.set_, new BasicDBObject("key", "test2")));
				System.out.println("ACK wr = " + wr);
			}
			catch (Exception e) {
				System.out.println("ACK err = " + e.toString());
			}
			
			// UnAck:
			try {
				test.drop();
				test.createIndex(new BasicDBObject("key", 1));
				BasicDBObject command1 = new BasicDBObject("shardcollection", "test.test123");
				command1.append("key", new BasicDBObject("key", 1));
				DbManager.getDB("admin").command(command1);
				
				db.setWriteConcern(WriteConcern.UNACKNOWLEDGED);
				outObj = new BasicDBObject("key", "test");
				test.save(outObj);
				WriteResult wr = test.update(new BasicDBObject(), new BasicDBObject(DbManager.set_, new BasicDBObject("key", "test2")), false, false, WriteConcern.ACKNOWLEDGED);
				System.out.println("ACK override wr = " + wr);
			}
			catch (Exception e) {
				System.out.println("ACK override  err = " + e.toString());
			}

			// UnAck:
			try {
				test.drop();
				test.createIndex(new BasicDBObject("key", 1));
				BasicDBObject command1 = new BasicDBObject("shardcollection", "test.test123");
				command1.append("key", new BasicDBObject("key", 1));
				DbManager.getDB("admin").command(command1);
				
				db.setWriteConcern(WriteConcern.UNACKNOWLEDGED);
				outObj = new BasicDBObject("key", "test");
				test.save(outObj);
				WriteResult wr = test.update(new BasicDBObject(), new BasicDBObject(DbManager.set_, new BasicDBObject("key", "test2")));
				System.out.println("UNACK wr = " + wr);
			}
			catch (Exception e) {
				System.out.println("UNACK err = " + e.toString());
			}
			
			// UnAck + GLE:
			try {
				test.drop();
				test.createIndex(new BasicDBObject("key", 1));
				BasicDBObject command1 = new BasicDBObject("shardcollection", "test.test123");
				command1.append("key", new BasicDBObject("key", 1));
				DbManager.getDB("admin").command(command1);
				
				db.setWriteConcern(WriteConcern.UNACKNOWLEDGED);
				outObj = new BasicDBObject("key", "test");
				test.save(outObj);
				WriteResult wr = test.update(new BasicDBObject(), new BasicDBObject(DbManager.set_, new BasicDBObject("key", "test2")));
				CommandResult cr = db.getLastError();
				System.out.println("UNACK GLE wr = " + wr);
				System.out.println("UNACK GLE cr = " + cr);
			}
			catch (Exception e) {
				System.out.println("UNACK GLE err = " + e.toString());
			}
		
			// Error handling:
			
			// 2.6:
			// Ack - exception
			// Ack override - exception
			// UnAck - no error given
			// UnAck + GLE  - gle error
			
			// 2.4:
			// Ack - exception
			// Ack override - exception
			// UnAck - no error given
			// UnAck + GLE  - gle error
			
		}
	}
}
