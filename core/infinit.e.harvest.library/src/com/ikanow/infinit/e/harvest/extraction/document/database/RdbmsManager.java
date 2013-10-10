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
package com.ikanow.infinit.e.harvest.extraction.document.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import org.apache.log4j.Logger;

/**
 * Class used to perform JDBC Connections and Queries for Database Crawling and Indexing
 * 
 * @author cmorgan
 *
 */
public class RdbmsManager {
	
	// Private Variables
	private String className = null;
	private String url = null;
	private String user = null;
	private String password = null;
	private String query = null;
	private Connection con;
	private Statement stmt;
	private ResultSet resultSet;
	private ResultSetMetaData metaData;
	private String errorMessage = null;
	private int recordsToSkip = 0;
	private boolean oracleWorkaround = false;

	// Initialize the Logger
	private static final Logger logger = Logger.getLogger(RdbmsManager.class);
	
	public int getRecordsToSkip() { return recordsToSkip; }
	public void setRecordsToSkip(int recordsToSkip) {
		this.recordsToSkip = recordsToSkip;
	}
	public Connection getConnection() {
		return con;
	}
	
	/**
	 * Get the class name
	 * @return the class name
	 */
	public String getClassName() {
		return className;
	}
	/**
	 * Set the class name
	 * @param className
	 */
	public void setClassName(String className) {
		this.className = className;
	}
	
	/**
	 * Get the jdbc url
	 * @return
	 */
	public String getUrl() {
		return url;
	}
	/**
	 * Set the jdbc url
	 * @param url
	 */
	public void setUrl(String url) {
		this.url = url;
	}
	
	/**
	 * Get the user
	 * @return
	 */
	public String getUser() {
		return user;
	}
	/**
	 * Set the user
	 * @param user
	 */
	public void setUser(String user) {
		this.user = user;
	}
	
	/**
	 * Get the password
	 * @return
	 */
	public String getPassword() {
		return password;
	}
	/**
	 * Set the password
	 * @param password
	 */
	public void setPassword(String password) {
		this.password = password;
	}
	
	/**
	 * Get the query
	 * @return
	 */
	public String getQuery() {
		return query;
	}
	/**
	 * Set the query
	 * @param query
	 */
	public void setQuery(String query) {
		this.query = query;
	}
	/**
	 * Set the query
	 * @param query
	 * @param modifiedDate
	 */
	public void setQuery(String query, String modifiedDate) {
		this.query = query.replace("?", "'" + modifiedDate + "'");
	}
	
	/**
	 * Get the result set
	 * @return the result set object
	 */
	public ResultSet getResultSet() {
		return resultSet;
	}
	
	/**
	 * Get the result set metadata object
	 * @return the result metadata object
	 */
	public ResultSetMetaData getMetaData() {
		return metaData;
	}
	
	/**
	 * Get the error message 
	 * @return the error message string
	 */
	public String getErrorMessage() {
		return errorMessage;
	}
	
	/**
	 * Constructor for the RdbmsManager class
	 */
	public RdbmsManager() {
		initialize(null,null,null,null,null);
	}
	
	/**
	 * Constructor for the RdbmsManager class
	 * @param className
	 * @param url
	 * @param query
	 */
	public RdbmsManager(String className, String url, String query) {
		initialize(className, url, query, null, null);
	}
	
	/**
	 * Constructor for the RdbmsManager class
	 * @param className
	 * @param url
	 * @param user
	 * @param password
	 * @param query
	 */
	public RdbmsManager(String className, String url, String query, String user, String password ) {
		initialize(className, url, user, password, query);
	}
	
	// Initialize the class private variables
	private void initialize(String className, String url, String user, String password, String query) {
		if ( className != null ) { this.className = className; }
		if ( url != null ) { this.url = url; }
		if ( user != null ) { this.user = user; }
		if ( password != null ) { this.password = password; }
		if ( query != null ) { this.query = query; }
	}
	
	/**
	 * Close the connections to the database
	 * @return
	 */
	public String closeConnection() {
		String errorMessage = null;
		// Close the result set
		try {
			if ( this.resultSet != null ) {
				this.resultSet.close();
			}
		} catch (SQLException e) {
			// If an exception occurs log the error
			logger.error("SQL Exception Message: " + e.getMessage(), e);
			// Set the error message
			return "Cannot close the result set : " + this.className;
		}
		// Close the statement
		try {
			if (this.stmt != null ) {
				this.stmt.close();	
			}
		} catch (SQLException e) {
			// If an exception occurs log the error
			logger.error("SQL Exception Message: " + e.getMessage(), e);
			// Set the error message
			return "Cannot close the statement : " + this.className;
		}
		// Close the connection
		try {
			if ( this.con != null  ) {
				this.con.close();	
			}
		}
		catch (SQLException e) {
			// If an exception occurs log the error
			logger.error("SQL Exception Message: " + e.getMessage(), e);
			// Set the error message
			return "Cannot close the connection : " + this.className;
		}
		return errorMessage;
	}
	
	/**
	 * Execute the query
	 * @return a friendly error message illustrating the error that occured, null if no error occured
	 */
	public String executeQuery()
	{
		String errorMessage = null;
		try
		{
			// Setup the class
			Class.forName(this.className); 
			
			// Check to see if the connection requires a username and password
			if (this.user != null && this.user.length() > 0) 
			{
				// Account for connections with empty string from password
				if (this.password == null) this.password = "";
				con = DriverManager.getConnection(this.url, this.user, this.password);
			}
			else 
			{
				con = DriverManager.getConnection(this.url);
			}
		}
		catch (ClassNotFoundException e) 
		{
			logger.error("Class Not Found Exception Message: " + e.getMessage(), e);
			return "Cannot find class : " + this.className;
		}
		catch (SQLException e) 
		{
			logger.error("SQL Exception Message: " + e.getMessage(), e);
			return "Cannot establish connection to the database : " + this.url;
		}
		
		if ( con != null ) 
		{
			// Get the result set and metadata from the query
		    try
		    {
		    	stmt = con.createStatement();
		 		this.resultSet = stmt.executeQuery(this.query);
		 		if (oracleWorkaround) {
			 		//avoid using scrollable methods due to jdbc limits - wtw
			 		for(int i = 0; i < recordsToSkip; i++)
			 			this.resultSet.next();		 			
		 		}
		 		else { // otherwise use JDBC
		 			this.resultSet.relative(recordsToSkip);
		 		}
			    this.metaData = this.resultSet.getMetaData();
			}
		    catch (SQLException e) 
		    {
				// If an exception occurs log the error
				logger.error("SQL Exception Message: " + e.getMessage(), e);
				// set the error message
				return "Cannot execute the query : " + this.query;
			}
		}
		return errorMessage;
	}


	/**
	 * getConnectionString
	 * @param databaseType
	 * @param driver
	 * @param hostname
	 * @param port
	 * @param databaseName
	 * @return
	 */
	public String getConnectionString(String databaseType, String hostname, String port, String databaseName) {
		// DB2 - jdbc:db2://server:port/database
		// MySQL - jdbc:mysql://host:port/database
		// Oracle - jdbc:oracle:thin:@//host:port/service
		// SQL Server - jdbc:jtds:sqlserver://host:port/database
		// Sybase - jdbc:jtds:sybase://host:port/database
		
		if (databaseType.equalsIgnoreCase("oracle") || databaseType.equalsIgnoreCase("oracle:thin")) {
			return "jdbc:" + databaseType + ":@//" + hostname + ":" + port + "/" + databaseName;			
		}
		if (databaseType.equalsIgnoreCase("oracle:thin:sid")) {
			databaseType = "oracle:thin";
			oracleWorkaround = true;
			return "jdbc:" + databaseType + ":@" + hostname + ":" + port + ":" + databaseName;			
		}
		else {
			if ((hostname.length() > 0) && (port.length() > 0)) {
				return "jdbc:" + databaseType + "://" + hostname + ":" + port + "/" + databaseName;
			}
			else {
				return "jdbc:" + databaseType + ":" + databaseName;					
			}			
		}
	}
		
}
