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
}