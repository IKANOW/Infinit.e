/**
 * 
 */
package com.ikanow.infinit.e.data_model.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import com.ikanow.infinit.e.data_model.Globals;


/**
 * @author cmorgan
 *
 */
public class PropertiesManager {
	
	/** 
	  * Class Constructor used to pull the properties object
	 * @throws IOException  
	  */
	public PropertiesManager() 
	{
		try 
		{
			//LINUX VERSION
			FileInputStream fis = new FileInputStream(Globals.getAppPropertiesLocation());
			properties = new Properties();
			properties.load(fis);
			fis.close();
		} 
		catch (Exception e) 
		{
			// If an exception occurs log the error
			properties = null;
		}		
	}
	/** 
	  * Private Class Variables
	  */
	private Properties properties = null;

	/**
	 * Get a property based only on the key
	 * @param key
	 * @return
	 */
	public String getProperty(String key) {
		return properties.getProperty(key);
	}
	
// Misc application metadata: (API and DB)	
	
	/** 
	  * Get application name
	  */
	public String getApplicationName() {
		return this.getProperty("app.appname");
	}
	/** 
	  * Get application copyright
	  */
	public String getApplicationCopyright() {
		return this.getProperty("app.copyright");
	}
	/** 
	  * Get application version
	  */
	public String getApplicationVersion() {
		return this.getProperty("app.version");
	}
	
// MongoDB configuration
	
	/** 
	  * Get database server
	  */
	public String getDatabaseServer() {
		return this.getProperty("db.server");
	}
	/** 
	  * Get database port
	  */
	public int getDatabasePort() {
		return Integer.parseInt(this.getProperty("db.port"));
	}
	public long getStorageCapacity() {
		return Long.parseLong(this.getProperty("db.capacity"));		
	}
		
	// Some configurable backwards compatiblity as we move into OSS...
	//  - BETA has h.feeds, h.gazateer etc
	//  - V0S1 has docs_meta.meta, docs_content.gzip_content, h.entity_features, h.event_features 
	//  - V0S2 will have correctly named fields
	public String getLegacyNamingVersion() {
		String s = this.getProperty("db.legacy_naming_version");
		if (null == s) {
			s = "V0"; // (default will change with different versions)
		}
		return s;
	}
	
// Index configuration
	
	/**
	 * Get elastic search url
	 */
	public String getElasticUrl() {
		return this.getProperty("elastic.url");
	}
	public String getElasticCluster() {
		return this.getProperty("elastic.cluster");
	}
	public int getElasticNodesPerReplica() {		
		String s = this.getProperty("elastic.nodes_per_replica");
		if (null == s) {
			return 5;			
		}
		else {
			return Integer.parseInt(s);
		}
	}
	public int getElasticMaxReplicas() {		
		String s = this.getProperty("elastic.max_replicas");
		if (null == s) {
			return 2;			
		}
		else {
			return Integer.parseInt(s);
		}
	}
	public String getElasticCachePolicy() {		
		String s = this.getProperty("elastic.cache_policy");
		if (null == s) {
			return "soft";			
		}
		else {
			return s; // current values: "soft", "resident"
		}
	}
}
