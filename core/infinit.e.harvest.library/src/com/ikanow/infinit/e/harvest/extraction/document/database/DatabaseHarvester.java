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

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import com.ikanow.infinit.e.data_model.InfiniteEnums;
import com.ikanow.infinit.e.data_model.InfiniteEnums.CommitType;
import com.ikanow.infinit.e.data_model.InfiniteEnums.DatabaseType;
import com.ikanow.infinit.e.data_model.InfiniteEnums.HarvestEnum;
import com.ikanow.infinit.e.data_model.store.config.source.SourceDatabaseConfigPojo;
import com.ikanow.infinit.e.data_model.store.config.source.SourceHarvestStatusPojo;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.infinit.e.harvest.HarvestContext;
import com.ikanow.infinit.e.harvest.extraction.document.HarvesterInterface;
import com.ikanow.infinit.e.harvest.extraction.document.DuplicateManager;
import com.ikanow.infinit.e.harvest.utils.HarvestExceptionUtils;
import com.ikanow.infinit.e.harvest.utils.PropertiesManager;
import com.ikanow.infinit.e.harvest.utils.TextEncryption;

/**
 * DatabaseHarvester
 * Used to harvest data from RDBMs
 * @author cmorgan
 */
public class DatabaseHarvester implements HarvesterInterface 
{

	// Private class variables
	private RdbmsManager rdbms = null;
	private PropertiesManager properties = null;
	private String jdbcUrl = null;
	private List<DocumentPojo> docsToAdd = null;
	@SuppressWarnings("unused")
	private List<DocumentPojo> docsToUpdate = null;
	private List<DocumentPojo> docsToDelete = null;
	private Set<Integer> sourceTypesCanHarvest = new HashSet<Integer>();
	private int maxDocsPerCycle = Integer.MAX_VALUE;
	
	private HarvestContext _context;

	// Initialize the Logger
	private static final Logger logger = Logger.getLogger(DatabaseHarvester.class);
	
	/**
	 * Default Constructor
	 */
	public DatabaseHarvester()
	{			
		sourceTypesCanHarvest.add(InfiniteEnums.DATABASE);
		PropertiesManager pm = new PropertiesManager();
		maxDocsPerCycle = pm.getMaxDocsPerSource();
	}
	
	/**
	 * executeHarvest
	 * Execute the harvest against a single source object
	 * @param source
	 * @return List<DocumentPojo> 
	 */
	public void executeHarvest(HarvestContext context, SourcePojo source, List<DocumentPojo> toAdd, List<DocumentPojo> toUpdate, List<DocumentPojo> toRemove)
	{
		_context = context;
		docsToAdd = toAdd;
		docsToUpdate = toUpdate;
		docsToDelete = toRemove;

		try 
		{
			processDatabase(source);
		} 
		catch (Exception e)
		{
			_context.getHarvestStatus().update(source, new Date(), HarvestEnum.error, "Error when harvesting DB: " + e.getMessage(), false, false);
			logger.error("Exception Message: " + e.getMessage(), e);
		}
	}
	
	/**
	 * processDatabase
	 * Connect to the database referenced in the SourcePojo and execute sql query to fetch
	 * a result set, pass result set to add records to this.docs
	 * @param source
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 * @throws IOException
	 * @return void
	 */
	private void processDatabase(SourcePojo source) throws ClassNotFoundException, SQLException, IOException 
	{
		// Set up properties and RDBMS Manager
		properties = new PropertiesManager();
		rdbms = new RdbmsManager();

		// Get the type of database to access from the source object
		DatabaseType dt = source.getDatabaseConfig().getDatabaseType();	
		
		// Create the jdbcUrl connection string for the manager
		jdbcUrl = rdbms.getConnectionString(dt, dt.toString(), 
				source.getDatabaseConfig().getHostname(), source.getDatabaseConfig().getPort(), 
				source.getDatabaseConfig().getDatabaseName());
		rdbms.setUrl(jdbcUrl);
		
		// Set the class name to load for JDBC, i.e. com.mysql.jdbc.Driver for MySql
		rdbms.setClassName(properties.getJdbcClass(source.getDatabaseConfig().getDatabaseType()));

		// If authentication is required to access the database set the user/pass here
		if ( source.getAuthentication() != null ) 
		{
			String userName = source.getAuthentication().getUsername();
			String password = source.getAuthentication().getPassword();
			
			if (userName != null && userName.length() > 0) rdbms.setUser(userName);
			if (password != null && password.length() > 0) rdbms.setPassword(new TextEncryption().decrypt(password));
		}
		
		// Some horrible logic to handle a DB cycle being >max records
		int nNumRecordsToSkip = 0;
		SourceHarvestStatusPojo lastHarvestInfo = source.getHarvestStatus();
		if (null != lastHarvestInfo) { // either in delta query mode, or truncation mode of initial query
			if (null != lastHarvestInfo.getHarvest_message()) {
				if (lastHarvestInfo.getHarvest_message().startsWith("query:")) { // Initial query
					try {
						int nEndOfNum = lastHarvestInfo.getHarvest_message().indexOf('.', 6);
						if (nEndOfNum > 0) {
							nNumRecordsToSkip = Integer.parseInt(lastHarvestInfo.getHarvest_message().substring(6, nEndOfNum));
						}
					}
					catch (Exception e) {} // Do nothing, just default to 0
					lastHarvestInfo = null; // (go through initial query branch below)
				}
				else { // Delta query or error!
					if (lastHarvestInfo.getHarvest_message().startsWith("deltaquery:")) { // Delta query, phew
						try {
							int nEndOfNum = lastHarvestInfo.getHarvest_message().indexOf('.', 11);
							if (nEndOfNum > 0) {
								nNumRecordsToSkip = Integer.parseInt(lastHarvestInfo.getHarvest_message().substring(11, nEndOfNum));
							}
						}
						catch (Exception e) {} // Do nothing, just default to 0							
					}					
				}
			}
		}//TESTED ("query:" by eye, "deltaquery:" implicitly as cut and paste) //TOTEST with "." and with other fields added
		
		// If the query has been performed before run delta and delete queries
		if (lastHarvestInfo != null) 
		{
			String deltaQuery = null;
			String deleteQuery = null;
			String deltaDate = null;
			
			// Get the date last harvested from the harvest object and then convert the date
			// to the proper format for the database being queried
			if (source.getHarvestStatus().getHarvested() != null)
			{
				deltaDate = getDatabaseDateString(dt, source.getHarvestStatus().getHarvested());
			}
						
			// Delta Query - get new data that has appeared in our source since the harvest last run
			// Important Note: The query should be against a "last modified" field not publishedDate unless they are 
			// equal to ensure new records get added properly and not ignored by the harvester
			if ((source.getDatabaseConfig().getDeltaQuery() != null) && !source.getDatabaseConfig().getDeltaQuery().isEmpty())
			{
				deltaQuery = source.getDatabaseConfig().getDeltaQuery();
				// Replace '?' in the delta query with the date value retrieved via getHarvested()
				if ((source.getHarvestStatus().getHarvested() != null) && (deltaQuery.contains("?")))
				{
					deltaQuery = deltaQuery.replace("?", "'" + deltaDate + "'").toString();
				}
			}
			// Skip logic:
			if (deltaQuery.contains("!SKIP!")) {
				deltaQuery = deltaQuery.replace("!SKIP!", new Integer(nNumRecordsToSkip).toString());
			}
			else {
				rdbms.setRecordsToSkip(nNumRecordsToSkip);				
			}
			
			// Delete docs from the index if they have been deleted from the source
			// database. Designed to check a "deleted" table to get records that have been
			// deleted to identify the records to remove form the index.
			if ((source.getDatabaseConfig().getDeleteQuery() != null) && !source.getDatabaseConfig().getDeleteQuery().isEmpty()) 
			{
				deleteQuery = source.getDatabaseConfig().getDeleteQuery();
				// Replace '?' in the delete query with the date value retrieved via getHarvested()
				if ((source.getHarvestStatus().getHarvested() != null) && (deleteQuery.contains("?")))
				{
					deleteQuery = deleteQuery.replace("?", "'" + deltaDate + "'").toString();
				}
				rdbms.setQuery(deleteQuery, deltaDate);
				rdbms.executeQuery();
				deleteRecords(rdbms.getResultSet(), source);
			}
			
			// Set the rdbms query = deltaQuery
			if ((deltaQuery != null) && (deltaDate != null))
			{
				rdbms.setQuery(deltaQuery, deltaDate);
			}
			else
			{
				rdbms.setQuery(source.getDatabaseConfig().getQuery());
			}
		}
		else // Very first import (though might be in horrible initial query mode)
		{
			String query = source.getDatabaseConfig().getQuery();
			if (query.contains("!SKIP!")) {
				query = query.replace("!SKIP!", new Integer(nNumRecordsToSkip).toString());
			}
			else {
				rdbms.setRecordsToSkip(nNumRecordsToSkip);				
			}
			// Set rdbs query = default source.database.query value
			rdbms.setQuery(query);
		}
		
		// Execute the specified query
		String sErr = rdbms.executeQuery();
		
		if (null != sErr) {
			_context.getHarvestStatus().update(source, new Date(), HarvestEnum.error, "Error when harvesting DB: " + sErr, false, false);
		}
		else {
			if (this.docsToAdd.size() > 0) {
				System.out.println("First doc = " + docsToAdd.get(0).getId());
			}
			
			// Build the list of docs using the result set from the query
			boolean bTruncated = addRecords(rdbms.getResultSet(), rdbms.getMetaData(), source);
			
			// Update source document with harvest success message
			String truncationMode = null;
			if (bTruncated) {
				if (null == lastHarvestInfo) { // Was an initial import
					truncationMode = new StringBuffer("query:").append(nNumRecordsToSkip + this.docsToAdd.size()).append('.').toString();
				}
				else { // Was a delta query
					truncationMode = new StringBuffer("deltaquery:").append(nNumRecordsToSkip + this.docsToAdd.size()).append('.').toString();					
				}
			}//TESTED (by hand in truncation and non-truncation cases)
			_context.getHarvestStatus().update(source, new Date(), HarvestEnum.in_progress, truncationMode, false, false);
		}	    
		// Close the connection
		try {
			rdbms.closeConnection();
			rdbms = null;
		}
		catch (Exception e) {
			// Do nothing, we've already updated the harvest
		}
	}
	

	/**
	 * addRecords - 
	 * 
	 * @param rs
	 * @param md
	 * @param source
	 */
	private boolean addRecords(ResultSet rs, ResultSetMetaData md, SourcePojo source) 
	{	
		LinkedList<String> duplicateSources = new LinkedList<String>(); 		
	    try 
	    {	
	    	int nAdded = 0;
	    	while(rs.next() && (nAdded < this.maxDocsPerCycle)) 
	    	{	
				/**
				 * Check for existing doc by checking the doc.url field which will contain
				 * the following information for a database record:
				 * source.url + primary key fields. See example below
				 * jdbc:mysql://184.72.206.97:3306/washingtondc/987863
				 */
				try 
				{
					DuplicateManager qr = _context.getDuplicateManager();
			    	String primaryKey = rs.getString(source.getDatabaseConfig().getPrimaryKey());
			    	String docUrl = source.getUrl() + "/" + primaryKey;
			    	
					// Check to see if the record has already been added
					// If it has been added then we need to update it with the new information
					if (!qr.isDuplicate_Url(docUrl, source, duplicateSources)) 
					{
						nAdded++;
						DocumentPojo newDoc = createDoc(CommitType.insert, rs, md, source, docUrl);
						if (!duplicateSources.isEmpty()) {
							newDoc.setDuplicateFrom(duplicateSources.getFirst());
						}
						this.docsToAdd.add(newDoc);
					}
					else {
						//TODO (INF-1300): update, I guess need to check if the record has changed?
						// If not, do nothing; if so, 
						//this.docsToUpdate.add(newOrUpdatedDoc);
					}					
				} 
				catch (Exception e) 
				{
					logger.error("Error on record: Exception Message: " + e.getMessage(), e);
					_context.getHarvestStatus().logMessage(HarvestExceptionUtils.createExceptionMessage("Error on record: ", e).toString(), true);
				} 				
			  }//end loop over records
	    	
		    return rs.next(); // (ie true if there are more records...)
	    } 
	    catch (Exception e) 
	    {			
	    	logger.error("Exception Message: " + e.getMessage(), e);
			_context.getHarvestStatus().logMessage(HarvestExceptionUtils.createExceptionMessage("Error on addRecords: ", e).toString(), true);
		}
	    return false;
	}
	
	
	/**
	 * deleteRecords
	 * @param rs
	 * @param source
	 */
	private void deleteRecords(ResultSet rs, SourcePojo source) 
	{
	    try 
	    {
			while(rs.next()) 
			{
				// Set up the primary variables
		    	String primaryKey = rs.getString(source.getDatabaseConfig().getPrimaryKey());
		    	String docUrl = source.getUrl() + "/" + primaryKey;
		    	
		    	// Get our system id from the record
		    	DocumentPojo docToRemove = new DocumentPojo();
		    	docToRemove.setUrl(docUrl);
		    	docsToDelete.add(docToRemove);
			}
		} 
	    catch (SQLException e) 
	    {
			logger.error("Error on delete: Exception Message: " + e.getMessage(), e);
			_context.getHarvestStatus().logMessage(HarvestExceptionUtils.createExceptionMessage("Error on delete: ", e).toString(), true);
		}
	}
	

	
	/**
	 * processDoc
	 * @param commitType
	 * @param col
	 * @param rs
	 * @param md
	 * @param source
	 * @param docUrl
	 */
	private DocumentPojo createDoc(CommitType commitType, ResultSet rs, ResultSetMetaData md, 
			SourcePojo source, String docUrl)
	{
		// Set up Feed object to be used to contain information
		DocumentPojo doc = null;
	
		try 
		{	
			// Check to see if the commit type is a insert or update
			if (commitType == CommitType.insert) 
			{
				// create the doc pojo
				doc = new DocumentPojo();
				doc.setId(new ObjectId());
				doc.setUrl(docUrl);
				doc.setCreated(new Date());				
			}
			else 
			{	
				//TODO (INF-1300): support for updated docs (will want to save old creation time, zap everything else?)
			}
			
			doc.setModified(new Date());
			
			// Strip out html if it is present
			if (rs.getString(source.getDatabaseConfig().getTitle()) != null)
			{
				doc.setTitle(rs.getString(source.getDatabaseConfig().getTitle()).replaceAll("\\<.*?\\>", ""));
			}
			
			if (rs.getString(source.getDatabaseConfig().getSnippet()) != null)
			{
				doc.setDescription(rs.getString(source.getDatabaseConfig().getSnippet()).replaceAll("\\<.*?\\>", ""));
			}

			if (rs.getString(source.getDatabaseConfig().getPublishedDate()) != null)
			{
				Object d = null;
				try
				{
					Object o = rs.getDate(source.getDatabaseConfig().getPublishedDate());
					d = convertJdbcTypes(null, o);
				}
				catch (Exception e)
				{
					d = new Date();
				}
				doc.setPublishedDate((Date) d);
			}
			else
			{
				doc.setPublishedDate(new Date());
			}
			
			// Add database information for primary key information
			SourceDatabaseConfigPojo db = new SourceDatabaseConfigPojo();
			if (source.getDatabaseConfig().getPrimaryKey() != null)
			{
				db.setPrimaryKey(source.getDatabaseConfig().getPrimaryKey());
			}
			
			if (rs.getString(source.getDatabaseConfig().getPrimaryKey()) != null)
			{
				db.setPrimaryKeyValue(rs.getString(source.getDatabaseConfig().getPrimaryKey()));
			}
			
	        // Create a list of metadata to be added to the doc
			for ( int i = 1; i <= md.getColumnCount(); i++ ) 
			{
				String column = md.getColumnLabel(i);
				Object value = rs.getObject(i);
				
				// Convert value to standard Java type from JDBC type if needed
				value = convertJdbcTypes(column, value);
				
				if (  (column != null) && (value != null) ) 
				{
					if (!source.getDatabaseConfig().getPreserveCase()) {
						column = column.toLowerCase();
					}
					doc.addToMetadata(column, value);
				}
	        }			
		}
		catch (SQLException e) 
		{
			logger.error("Error on add: Exception Message: " + e.getMessage(), e);
			_context.getHarvestStatus().logMessage(HarvestExceptionUtils.createExceptionMessage("Error on add: ", e).toString(), true);
		}
		
		return doc;
	}
	

	/**
	 * convertJdbcTypes
	 * 
	 * @param o
	 * @return
	 */
	private static Object convertJdbcTypes(String columnName, Object o)
	{
		if (o instanceof java.sql.Timestamp)
		{
			java.sql.Timestamp t = (java.sql.Timestamp)o;
			Date d = new Date(t.getTime());
			o = d;
		}
		else if (o instanceof java.sql.Date)
		{
			java.sql.Date t = (java.sql.Date)o;
			Date d = new Date(t.getTime());
			o = d;
		}
		else if (o instanceof java.sql.Time)
		{
			java.sql.Time t = (java.sql.Time)o;
			Date d = new Date(t.getTime());
			o = d;
		}
		if ((null == columnName) || (columnName.indexOf("__") > 0)) { // (ie __suffix)
			return o;
		}
		else { // If type not explicity specified then treat as string
			return o.toString();
		}
	}
	
	
	/**
	 * getDatabaseDateString
	 * Convert the date (d) to a format suitable for the database
	 * being accessed using SimpeDateFormat.format(d)
	 * @param 	dt 			- InfiniteEnums.DatabaseType
	 * @param 	d 			- Date
	 * @return 			 	- String
	 */
	private static String getDatabaseDateString(DatabaseType dt, Date d)
	{
		// t.string () = yyyy-mm-dd hh:mm:ss
		java.sql.Timestamp t = new Timestamp(d.getTime());
		
		switch (dt)
		{
			case db2:
				return t.toString();
			case mssqlserver:
				return t.toString();
			case mysql:
				// yyyy-MM-dd HH:mm:ss
				return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(d);
			case oracle:
				// dd-MMM-yyyy HH:mm:ss
				return new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss").format(d);
			case sybase:
				return t.toString();
			default:
				return t.toString();
		}
	}
	


	@Override
	public boolean canHarvestType(int sourceType) 
	{
		return sourceTypesCanHarvest.contains(sourceType);
	}
}
