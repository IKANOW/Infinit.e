/**
 * 
 */
package com.ikanow.infinit.e.processing.custom.utils;

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
	 * Get API URL root
	 */
	public String getURLRoot() {
		return this.getProperty("url.root");
	}
	/**
	 * Get jobclient address
	 */
	public String getJobClientServer() {
		return this.getProperty("jobclient.server");
	}
	/**
	 * Get db address
	 */
	public String getDBServer() {
		return this.getProperty("db.server");
	}
	/**
	 * Get jobclient port
	 * 
	 * defaults to 8021
	 */
	public int getJobClientPort() {
		try
		{
			return Integer.parseInt(this.getProperty("jobclient.port"));
		}
		catch(Exception ex)
		{
			return 8021;
		}
	}
	
	public String getHadoopConfigPath()
	{
		try
		{
			return this.getProperty("hadoop.configpath");
		}
		catch (Exception ex)
		{
			return "/opt/hadoop-infinite";
		}
	}
	
}


