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
package com.ikanow.infinit.e.api.utils;

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
	
// API configuration	
	
	/***
	 * SaaS deployment variables
	 */
	
	public Boolean isSaasDeployment() {
		return this.getProperty("app.saas").toLowerCase().equals("true");
	}
	public String getSaasTrustedDns() {
		return this.getProperty("app.saas.trusted.dns");
	}
	
	// API timeout (for raw API calls - the GUI has its own timeout)
	
	public Long getApiTimeoutSeconds() {
		String s = this.getProperty("access.timeout");
		if (null == s) {
			return null;
		}
		else {
			return Long.parseLong(s);
		}
	}	
	
	// Address from which automated admin emails are sent
	
	public String getAdminEmailAddress() {
		String s = this.getProperty("mail.from_address");
		if (null == s) {
			return "service@ikanow.com";
		}
		return s;
	}
	
	// ROOT URL to which to send links
	
	public String getUrlRoot() {		
		return this.getProperty("url.root");
	}
	
	// REMOTE ACCESS CONTROLS (defaults to everything)
	
	public String getRemoteAccessAllow() {
		return this.getProperty("remote.access.allow");
	}
	public String getRemoteAccessDeny() {
		return this.getProperty("remote.access.deny");
	}
	public boolean getApiLoggingEnabled() {
		try {
			return Boolean.parseBoolean(this.getProperty("ui.logging"));
		}
		catch (Exception e) {
			return false;
		}
	}
	public String getApiLoggingRegex() {
		return this.getProperty("ui.logging.api.regex");
	}
	//ALIASING
	public boolean getAliasingEnabled() {
		String s = this.getProperty("api.aliasing.enabled");
		if (null == s) {
			return true;
		}
		else return Boolean.parseBoolean(s);
	}
	
	// AGGREGATION ACCURACY-PERFORMANCE TRADE-OFFS: none (don't allow aggregations), low (calc aggregations manually), full (use facets)
	public String getAggregationAccuracy() {
		String s =  this.getProperty("api.aggregation.accuracy");
		if (null == s) {
			s = "full";
		}
		return s;
	}
	
}
