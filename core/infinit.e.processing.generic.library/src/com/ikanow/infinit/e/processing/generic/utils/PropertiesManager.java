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
package com.ikanow.infinit.e.processing.generic.utils;

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
	/** 
	  * Get max content length to save
	  */
	public int getMaxContentSize() {
		String s = this.getProperty("store.maxcontent");
		if (s == null) {
			return -1;
		}
		try {
			return Integer.parseInt(s);
		}
		catch (Exception e) {
			return -1;
		}
	}
	
	/** 
	  * Additional content:
	  */
	public boolean storeRawContent() {
		String s = this.getProperty("store.rawcontent");
		try {
			return Boolean.parseBoolean(s);
		}
		catch (Exception e) {
			return false;
		}		
	}

	public boolean storeMetadataAsContent() {
		String s = this.getProperty("store.metadata_as_content");
		try {
			return Boolean.parseBoolean(s);
		}
		catch (Exception e) {
			return false;
		}		
	}
	
	
	// Get preferred (max) index replicas
	
	public int getMaxIndexReplicas() {
		String s = this.getProperty("elastic.max_replicas");
		if (s == null) {
			return 1; // (default)
		}
		try {
			return Integer.parseInt(s);
		}
		catch (Exception e) {
			return 1; // (default)
		}		
	}

	// Check if entity/association aggregation is disabled
	
	public boolean getAggregationDisabled() {
		String s = this.getProperty("harvest.disable_aggregation");
		if (null == s) {
			return false;
		}
		else if (s.equals("1") || s.equalsIgnoreCase("true")) {
			return true;
		}
		return false;
	}

	// If aggregation enabled, normally will run in a background thread with a 50% duty cycle
	// set this to <0 to run synchronously in harvest (legacy code), or 0<.<=1 otherwise, (0 just doesn't aggreagate on-the-fly)
	
	public double getHarvestAggregationDutyCycle() {
		String s = this.getProperty("harvest.aggregation.duty_cycle");
		if (null == s) { // Once this has had a chance to be proven we'll enable it by default
			return 0.5;
		}
		return Double.parseDouble(s);
	}
	
	/**
	 * Checks if icu_normalization should be active for community ES index
	 * 
	 * @return false if turned off, true otherwise
	 */
	public boolean getNormalizeEncoding() 
	{
		String s = this.getProperty("store.normalize_encoding");
		if (null == s) 
		{
			return true;
		}
		else if (s.equals("1") || s.equalsIgnoreCase("true")) 
		{
			return true;
		}
		return false;
	}
	
}


