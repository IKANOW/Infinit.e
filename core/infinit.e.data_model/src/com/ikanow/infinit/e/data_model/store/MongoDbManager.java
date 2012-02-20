package com.ikanow.infinit.e.data_model.store;

import com.ikanow.infinit.e.data_model.utils.PropertiesManager;
import com.mongodb.CommandResult;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.Mongo;
import com.mongodb.gridfs.GridFS;

// Encapsulates Mongo Databases and collections

public class MongoDbManager {

	// MongoDB commands:
	public static final String regex_ = "$regex"; 
	public static final String exists_ = "exists"; 
	public static final String and_ = "$and"; 
	public static final String or_ = "$or"; 
	public static final String ne_ = "$ne"; 
	public static final String gt_ = "$gt"; 
	public static final String gte_ = "$gte"; 
	public static final String lt_ = "$lt"; 
	public static final String lte_ = "$lte"; 
	public static final String in_ = "$in"; 
	public static final String each_ = "$each"; 
	public static final String set_ = "$set"; 
	public static final String inc_ = "$inc"; 
	public static final String addToSet_ = "$addToSet"; 
	public static final String pull_ = "$pull"; 
	public static final String pullAll_ = "$pullAll"; 
	public static final String push_ = "$push"; 
	public static final String pushAll_ = "$pushAll"; 
	public static final String pop_ = "$pop"; 
	
	protected MongoDbManager() {}
	
	// 1. MongoDB connection management
	
	private static ThreadLocal<MongoDbConnection> _connections = new ThreadLocal<MongoDbConnection>() {
        @Override protected MongoDbConnection initialValue() {
            try {
				return new MongoDbConnection(new PropertiesManager());
			} catch (Exception e) {
				return null;
			}
        }
	};
	
	// Generic database access:
	
	public static DB getDB(String dbName) {
		return _connections.get().getMongo().getDB(dbName);
	}
	public static DBCollection getCollection(String dbName, String collectionName) {
		return _connections.get().getMongo().getDB(dbName).getCollection(collectionName);
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
		
		public CommandResult getLastError(String sLogicalCollectionName) {
			// (In this case, version doesn't -currently- matter)
			String sVersion = new PropertiesManager().getLegacyNamingVersion();
			if (sLogicalCollectionName.equalsIgnoreCase("authentication")) {			
				return _savedMongo.getDB("security").getLastError();
			}
			else if (sLogicalCollectionName.equalsIgnoreCase("cookies")) {			
				return _savedMongo.getDB("security").getLastError();
			}
			else if (sLogicalCollectionName.equalsIgnoreCase("binary")) {			
				return _savedMongo.getDB("file").getLastError();
			}
			else if (sLogicalCollectionName.equalsIgnoreCase("uimodules")) {			
				if (sVersion.equalsIgnoreCase("BETA")) {
					return _savedMongo.getDB("module").getLastError();
				}
				else {
					return _savedMongo.getDB("gui").getLastError();
				}
			}
			else if (sLogicalCollectionName.equalsIgnoreCase("uifavoritemodules")) {			
				if (sVersion.equalsIgnoreCase("BETA")) {			
					return _savedMongo.getDB("module").getLastError();
				}
				else {
					return _savedMongo.getDB("gui").getLastError();
				}
			}
			else if (sLogicalCollectionName.equalsIgnoreCase("uisetup")) {			
				if (sVersion.equalsIgnoreCase("BETA")) {			
					return _savedMongo.getDB("admin").getLastError();
				}
				else {
					return _savedMongo.getDB("gui").getLastError();					
				}
			}
			else {
				return _savedMongo.getDB("social").getLastError();
			}
		}

		public DBCollection getPerson() {
			if (null == _social_person) {
				_social_person = _savedMongo.getDB("social").getCollection("person");
			}
			return _social_person;
		}
		public DBCollection getCommunity() {
			if (null == _social_community) {
				_social_community = _savedMongo.getDB("social").getCollection("community");
			}
			return _social_community;
		}
		public DBCollection getCommunityApprove() {
			if (null == _social_communityapprove) {
				_social_communityapprove = _savedMongo.getDB("social").getCollection("communityapprove");
			}
			return _social_communityapprove;
			
		}
		public DBCollection getAuthentication() {
			if (null == _social_authentication) {
				_social_authentication = _savedMongo.getDB("security").getCollection("authentication");
			}
			return _social_authentication;
		}
		public DBCollection getCookies() {
			if (null == _social_cookies) {
				_social_cookies = _savedMongo.getDB("security").getCollection("cookies");
			}
			return _social_cookies;
		}
		public DBCollection getShare() {
			if (null == _social_share) {
				_social_share = _savedMongo.getDB("social").getCollection("share");
			}
			return _social_share;
		}
		public GridFS getShareBinary() {
			if (null == _social_share_binary) {
				String sVersion = new PropertiesManager().getLegacyNamingVersion();
				if (sVersion.equalsIgnoreCase("BETA")) {			
					_social_share_binary = new GridFS(_savedMongo.getDB("file"), "jars");
				}
				else {
					_social_share_binary = new GridFS(_savedMongo.getDB("file"), "binary_shares");					
				}
			}
			return _social_share_binary;
		}
		public DBCollection getUIModules() {
			if (null == _social_gui_modules) {
				String sVersion = new PropertiesManager().getLegacyNamingVersion();
				if (sVersion.equalsIgnoreCase("BETA")) {			
					_social_gui_modules = _savedMongo.getDB("module").getCollection("modules");
				}
				else {
					_social_gui_modules = _savedMongo.getDB("gui").getCollection("modules");					
				}
			}
			return _social_gui_modules;
		}
		public DBCollection getUIFavoriteModules() {
			if (null == _social_gui_favmodules) {
				String sVersion = new PropertiesManager().getLegacyNamingVersion();
				if (sVersion.equalsIgnoreCase("BETA")) {			
					_social_gui_favmodules = _savedMongo.getDB("module").getCollection("usermodules");
				}
				else {
					_social_gui_favmodules = _savedMongo.getDB("gui").getCollection("favmodules");					
				}
			}
			return _social_gui_favmodules;
		}
		public DBCollection getUISetup() {
			if (null == _social_gui_setup) {
				String sVersion = new PropertiesManager().getLegacyNamingVersion();
				if (sVersion.equalsIgnoreCase("BETA")) {			
					_social_gui_setup = _savedMongo.getDB("admin").getCollection("uisetup");
				}
				else {
					_social_gui_setup = _savedMongo.getDB("gui").getCollection("setup");					
				}
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
		
		public CommandResult getLastError(String sLogicalCollectionName) {
			// (In this case, logical collection name doesn't matter)
			String sVersion = new PropertiesManager().getLegacyNamingVersion();
			if (sVersion.equalsIgnoreCase("BETA")) {			
				return _savedMongo.getDB("harvester").getLastError();
			}
			else if (sLogicalCollectionName.equalsIgnoreCase("metadata")){
				return _savedMongo.getDB("doc_metadata").getLastError();				
			}
			else if (sLogicalCollectionName.equalsIgnoreCase("content")){
				return _savedMongo.getDB("doc_content").getLastError();				
			}
			else {
				return null;
			}
		}
		public DBCollection getMetadata() {
			if (null == _document_metadata) {
				String sVersion = new PropertiesManager().getLegacyNamingVersion();
				if (sVersion.equalsIgnoreCase("BETA")) {
					_document_metadata = _savedMongo.getDB("harvester").getCollection("feeds");
				}
				else {
					_document_metadata = _savedMongo.getDB("doc_metadata").getCollection("metadata");					
				}
			}
			return _document_metadata;
		}
		public DBCollection getContent() {
			if (null == _document_content) {
				String sVersion = new PropertiesManager().getLegacyNamingVersion();
				if (sVersion.equalsIgnoreCase("BETA")) {
					_document_content = _savedMongo.getDB("harvester").getCollection("gzip_content");
				}
				else {
					_document_content = _savedMongo.getDB("doc_content").getCollection("gzip_content");					
				}
			}
			return _document_content;
		}
		public DBCollection getCounts() {
			if (null == _document_counts) {
				_document_counts = _savedMongo.getDB("doc_metadata").getCollection("doc_counts");					
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
		
		public CommandResult getLastError(String sLogicalCollectionName) {
			// (In this case, logical collection name doesn't matter)
			String sVersion = new PropertiesManager().getLegacyNamingVersion();
			if (sVersion.equalsIgnoreCase("BETA")) {			
				return _savedMongo.getDB("harvester").getLastError();
			}
			else {
				return _savedMongo.getDB("feature").getLastError();				
			}
		}
		
		public DBCollection getEntity() {
			if (null == _feature_entity) {
				String sVersion = new PropertiesManager().getLegacyNamingVersion();
				if (sVersion.equalsIgnoreCase("BETA")) {
					_feature_entity = _savedMongo.getDB("harvester").getCollection("gazateer");
				}
				else {
					_feature_entity = _savedMongo.getDB("feature").getCollection("entity");					
				}
			}
			return _feature_entity;
		}
		public DBCollection getAssociation() {
			if (null == _feature_assoc) {
				String sVersion = new PropertiesManager().getLegacyNamingVersion();
				if (sVersion.equalsIgnoreCase("BETA")) {
					_feature_assoc = _savedMongo.getDB("harvester").getCollection("event_gazateer");
				}
				else {
					_feature_assoc = _savedMongo.getDB("feature").getCollection("association");					
				}
			}
			return _feature_assoc;
		}
		public DBCollection getGeo() {
			if (null == _feature_geo) {
				String sVersion = new PropertiesManager().getLegacyNamingVersion();
				if (sVersion.equalsIgnoreCase("BETA")) {
					_feature_geo = _savedMongo.getDB("harvester").getCollection("georeference");
				}
				else {
					_feature_geo = _savedMongo.getDB("feature").getCollection("geo");					
				}
			}
			return _feature_geo;
		}
	}
	
	// 3.4. Config collections
	
	public static class IngestMongoDb {	
		IngestMongoDb(Mongo mongo) { _savedMongo = mongo; }		
		private Mongo _savedMongo;

		private DBCollection _ingest_source;
		
		public CommandResult getLastError(String sLogicalCollectionName) {
			// (In this case, logical collection name doesn't matter)
			String sVersion = new PropertiesManager().getLegacyNamingVersion();
			if (sVersion.equalsIgnoreCase("BETA")) {			
				return _savedMongo.getDB("harvester").getLastError();
			}
			else if (sVersion.equalsIgnoreCase("V0S1")) {
				return _savedMongo.getDB("config").getLastError();								
			}
			else {
				return _savedMongo.getDB("ingest").getLastError();				
			}
		}
		
		public DBCollection getSource() {
			if (null == _ingest_source) {
				String sVersion = new PropertiesManager().getLegacyNamingVersion();
				if (sVersion.equalsIgnoreCase("BETA")) {
					_ingest_source = _savedMongo.getDB("harvester").getCollection("sources");
				}
				else if (sVersion.equalsIgnoreCase("V0S1")) {
					_ingest_source = _savedMongo.getDB("config").getCollection("source");										
				}
				else {
					_ingest_source = _savedMongo.getDB("ingest").getCollection("source");										
				}
			}
			return _ingest_source;
		}
	}
	
	//CALEBS SWEET CUSTOM CLASS
	public static class CustomMongoDb 
	{	
		CustomMongoDb(Mongo mongo) { _savedMongo = mongo; }		
		private Mongo _savedMongo;

		private DBCollection _config_customlookup;
		
		public CommandResult getLastError(String sLogicalCollectionName) {
			// (In this case, logical collection name doesn't matter)
			String sVersion = new PropertiesManager().getLegacyNamingVersion();
			if (sVersion.equalsIgnoreCase("BETA")) {			
				return _savedMongo.getDB("custommr").getLastError();
			}
			else {
				return _savedMongo.getDB("custommr").getLastError();				
			}
		}
		
		public DBCollection getLookup() {
			if (null == _config_customlookup) {
				String sVersion = new PropertiesManager().getLegacyNamingVersion();
				if (sVersion.equalsIgnoreCase("BETA")) {
					_config_customlookup = _savedMongo.getDB("custommr").getCollection("customlookup");
				}
				else {
					_config_customlookup = _savedMongo.getDB("custommr").getCollection("customlookup");										
				}
			}
			return _config_customlookup;
		}
	}
	
	
}
