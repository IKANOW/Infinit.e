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
/**
 * 
 */
package com.ikanow.infinit.e.harvest.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import org.apache.log4j.Logger;

import com.ikanow.infinit.e.data_model.Globals;


/**
 * @author cmorgan
 *
 */
public class PropertiesManager {
	
	private static final Logger logger = Logger.getLogger(PropertiesManager.class);

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
			logger.error("Exception Message: " + e.getMessage(), e);
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
	
// General harvester configuration	
	
	public String getHarvesterTypes() {
		return this.getProperty("harvester.types");
	}
	public int getMaxSourcesPerHarvest() {
		String s = this.getProperty("harvest.maxsources_perharvest");
		if (null == s) {
			return 1000; // (default max limit) 
		}
		else {
			return Integer.parseInt(s);
		}				
	}
	public int getMaxDocsPerSource() {
		String s = this.getProperty("harvest.maxdocs_persource");
		if (null == s) {
			return Integer.MAX_VALUE; 
		}
		else {
			return Integer.parseInt(s);
		}		
	}
	public long getWebCrawlWaitTime() { // (Delay between feed docs) 
		String s = this.getProperty("harvest.feed.wait");
		if (null == s) { // Default 10s
			return 10000;
		}
		else {
			return Long.parseLong(s);
		}
	}
	public long getMaxTimePerFeed() { // (Limits the number of documents based on getWebCrawlWaitTime) 
		String s = this.getProperty("harvest.feed.maxtime");
		if (null == s) { // Default 10m
			return 600000;
		}
		else {
			return Long.parseLong(s);
		}
	}
	public String getHarvestUserAgent() { // Default user agent
		String s = this.getProperty("harvest.feed.useragent");
		if ((null == s) || s.isEmpty()) {
			return null;
		}
		return s;
	}
	
// DB class names for DbHarvester
	
	/**
	 * Get the jdbc class for the specified database type
	 * @param databaseType
	 * @return
	 */
	public String getJdbcClass(String databaseType) 
	{
		return this.getProperty("jdbc." + databaseType + ".class");
	}
	/**
	 * Get the jdbc url for the specified database type
	 * @param databaseType
	 * @return
	 */
	public String getJdbcUrl(String databaseType ) {
		return this.getProperty("jdbc." + databaseType + ".url");
	}
	
// Extractor specific configuration	
	
	public String getExtractorKey(String extractor) {
		return this.getProperty("extractor.key." + extractor.toLowerCase());
	}
	public String getDefaultEntityExtractor() {
		return this.getProperty("extractor.entity.default");		
	}
	public String getDefaultTextExtractor() {
		return this.getProperty("extractor.text.default");		
	}
	/***
	 * Alchemy specific configuration
	 */
	
	public int getAlchemyPostProcessingSetting() {
		String s = this.getProperty("app.alchemy.postproc");
		if (null == s) {
			return 0;
		}
		else {
			return Integer.parseInt(s.toLowerCase());
		}
	}
	
	public Boolean getExtractionCapabilityEnabled(String name, String function) {  
		String s = this.getProperty("app."+ name.toLowerCase() +"."+ function.toLowerCase());
		if (null == s) {
			return null;
		}
		return (s.equals("1") || s.equalsIgnoreCase("true"));
	}
	
	// Generic other extractors (can't configure, need UIMA framework for that)
	// Will be a list of classes that need to be on the JAR classpath
	
	public String getCustomTextExtractors() {
		return this.getProperty("extractor.text.custom");
	}
	public String getCustomEntityExtractors() {
		return this.getProperty("extractor.entity.custom");
	}
}


