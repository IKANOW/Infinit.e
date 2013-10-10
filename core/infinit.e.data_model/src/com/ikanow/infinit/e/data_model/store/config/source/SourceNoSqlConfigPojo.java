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
package com.ikanow.infinit.e.data_model.store.config.source;

import java.util.LinkedHashMap;

import com.ikanow.infinit.e.data_model.store.social.authentication.AuthenticationPojo;

/**
 * Primary object used to specify the properties for connecting to a database
 * @author cmorgan
 */
public class SourceNoSqlConfigPojo 
{		
	//Pipeline fields:
	private String url = null; // (pipeline only)
	private AuthenticationPojo authentication = null; // (pipeline only)
	
	private String databaseType = null;			// Type of the database, mongodb/cassandra/neo4j/hbase
	private String hostname = null; 			// Name of the server where the database resides, fully qualified domain name
	private String port = null; 				// Port number on which the database resides 
	private String databaseName = null; 		// The name given to the database 
	private String query = null; 				// JPQL query to be performed to fully load the data
	private String deltaQuery = null; 			// JPQL query used to create new records 
	private String updateQuery = null; 			// JPQL query used to update existing records
	private String deleteQuery = null; 			// JPQL query used to update the index with recently deleted records from database environment
	private String primaryKey = null; 			// Primary key field, used for mapping db element to system
	private String primaryKeyValue = null; 		// Primary key value, only used when placing in the feed record
	private String title = null; 				// Record title field, used for field display purposes
	private String snippet = null; 				// Record snippet field, used for field display purposes
	private String publishedDate = null;		// Field to map to doc's publishedDate field
	private String modifiedDate = null;			// Field to map to doc's modified time, and is used for fine-grained updating, if present
	private Boolean preserveCase = null;		// If true, uses the column case in the metadata, else converts to lower case
	
	private String objectDescription = null;	// Currently, the list of (top-level only) fields to query ... later will be a JSON description of the object stored in the NoSQL DB
	
	// Kundera properties (dialect, nodes, port can also be set using the standard terms below - they will be overridden by this object map though)
	private LinkedHashMap<String, Object> kundera = null;	
	
	/**
	 * Get the type of the database
	 * @return
	 */
	public String getDatabaseType() {
		return databaseType;
	}
	/**
	 * Set the database type
	 * @param databaseType
	 */
	public void setDatabaseType(String databaseType) {
		this.databaseType = databaseType;
	}
	/**
	 * Get the hostname of the database
	 * @return
	 */
	public String getHostname() {
		return hostname;
	}
	/**
	 * Set the hostname for the database
	 * @param hostname
	 */
	public void setHostname(String hostname) {
		this.hostname = hostname;
	}
	/**
	 * Get the database port of operation
	 * @return
	 */
	public String getPort() {
		return port;
	}
	/**
	 * Set the database port of operation
	 * @param port
	 */
	public void setPort(String port) {
		this.port = port;
	}
	/**
	 * Get the database name
	 * @return
	 */
	public String getDatabaseName() {
		return databaseName;
	}
	/**
	 * Set the database name
	 * @param databaseName
	 */
	public void setDatabaseName(String databaseName) {
		this.databaseName = databaseName;
	}
	/**
	 * Get the query to use to populate the index.  This query is used for all full sync operations
	 * @return
	 */
	public String getQuery() {
		return query;
	}
	/**
	 * Set the query to use to populate the index.  This query is used for all full sync operations
	 * @param query
	 */
	public void setQuery(String query) {
		this.query = query;
	}
	/**
	 * Get the delta query used only for update operations and must query the same fields as the query 
	 * variable.  If the same fields are not specified data could be overwritten or removed.
	 * @return
	 */
	public String getDeltaQuery() {
		return deltaQuery;
	}
	/**
	 * Set the delta query, used only for update operations and must query the same fields as the 
	 * query variable.  If the same fields are not specified data could be overwritten or removed.
	 * @param deltaQuery
	 */
	public void setDeltaQuery(String deltaQuery) {
		this.deltaQuery = deltaQuery;
	}
	/**
	 * Get the delete query, used only for delete operations and must be a log type table
	 * also requires the use of the last_modified operator which is specified by the harvesting 
	 * or syncing processing 
	 * @return
	 */
	public String getDeleteQuery() {
		return deleteQuery;
	}
	/**
	* Set the delete query, used only for delete operations and must be a log type table
	* also requires the use of the last_modified operator which is specified by the harvesting 
	* or syncing processing
	* 
	* @param deleteQuery
	*/
	public void setDeleteQuery(String deleteQuery) {
		this.deleteQuery = deleteQuery;
	}
	/**
	 * Get the primary key field
	 * @return
	 */
	public String getPrimaryKey() {
		return primaryKey;
	}
	/**
	 * Set the primary key field
	 * @param primaryKey
	 */
	public void setPrimaryKey(String primaryKey) {
		this.primaryKey = primaryKey;
	}
	/**
	 * Get the primary key value
	 * @return
	 */
	public String getPrimaryKeyValue() {
		return primaryKeyValue;
	}
	/**
	 * Set the primary key value
	 * @param primaryKeyValue
	 */
	public void setPrimaryKeyValue(String primaryKeyValue) {
		this.primaryKeyValue = primaryKeyValue;
	}
	/**
	 * Get the title field or title data
	 * @return
	 */
	public String getTitle() {
		return title;
	}
	/**
	 * Set the title field or title data
	 * @param title
	 */
	public void setTitle(String title) {
		this.title = title;
	}
	/**
	 * Get the snippet field or snippet data
	 * @return
	 */
	public String getSnippet() {
		return snippet;
	}
	/**
	 * Set the snippet field or snippet data
	 * @param snippet
	 */
	public void setSnippet(String snippet) {
		this.snippet = snippet;
	}
	/**
	 * @param publishedDate the publishedDate to set
	 */
	public void setPublishedDate(String publishedDate) {
		this.publishedDate = publishedDate;
	}
	/**
	 * @return the publishedDate
	 */
	public String getPublishedDate() {
		return publishedDate;
	}
	public void setPreserveCase(boolean preserveCase) {
		this.preserveCase = preserveCase;
	}
	public boolean getPreserveCase() {
		return (preserveCase == null)?false:preserveCase;
	}
	public String getUpdateQuery() {
		return updateQuery;
	}
	public void setUpdateQuery(String updateQuery) {
		this.updateQuery = updateQuery;
	}
	public String getModifiedDate() {
		return modifiedDate;
	}
	public void setModifiedDate(String modifiedDate) {
		this.modifiedDate = modifiedDate;
	}
	public AuthenticationPojo getAuthentication() {
		return authentication;
	}
	public void setAuthentication(AuthenticationPojo authentication) {
		this.authentication = authentication;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public void setKundera(LinkedHashMap<String, Object> kundera) {
		this.kundera = kundera;
	}
	public LinkedHashMap<String, Object> getKundera() {
		return kundera;
	}
	public void setObjectDescription(String objectDescription) {
		this.objectDescription = objectDescription;
	}
	public String getObjectDescription() {
		return objectDescription;
	}
}
