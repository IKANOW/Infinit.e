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

import org.apache.log4j.Logger;

/**
 * Class used to perform JPQL Connections and Queries for Database Crawling and Indexing
 * 
 * @author cmorgan
 *
 */
public class NoSqlOrmManager {
	
	// Private Variables
	private String url = null;
	private String user = null;
	private String password = null;
	private String query = null;
	private String errorMessage = null;

	// Initialize the Logger
	@SuppressWarnings("unused")
	private static final Logger logger = Logger.getLogger(NoSqlOrmManager.class);
	
	/**
	 * Get the jpa url
	 * @return
	 */
	public String getUrl() {
		return url;
	}
	/**
	 * Set the jpa url
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
	 * Get the error message 
	 * @return the error message string
	 */
	public String getErrorMessage() {
		return errorMessage;
	}
	
	/**
	 * Constructor for the RdbmsManager class
	 */
	public NoSqlOrmManager() {
		initialize(null,null,null,null);
	}
	
	/**
	 * Constructor for the RdbmsManager class
	 * @param className
	 * @param url
	 * @param query
	 */
	public NoSqlOrmManager(String className, String url, String query) {
		initialize(url, query, null, null);
	}
	
	/**
	 * Constructor for the RdbmsManager class
	 * @param className
	 * @param url
	 * @param user
	 * @param password
	 * @param query
	 */
	public NoSqlOrmManager(String url, String query, String user, String password ) {
		initialize(url, user, password, query);
	}
	
	// Initialize the class private variables
	private void initialize(String url, String user, String password, String query) {
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
		return errorMessage;
	}
	
	/**
	 * Execute the query
	 * @return a friendly error message illustrating the error that occured, null if no error occured
	 */
	public String executeQuery()
	{
		return errorMessage;
	}		
}
