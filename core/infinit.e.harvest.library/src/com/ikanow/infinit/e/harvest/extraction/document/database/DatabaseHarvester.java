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
import java.lang.reflect.Array;
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
import com.ikanow.infinit.e.data_model.InfiniteEnums.HarvestEnum;
import com.ikanow.infinit.e.data_model.store.config.source.SourceHarvestStatusPojo;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.infinit.e.harvest.HarvestContext;
import com.ikanow.infinit.e.harvest.extraction.document.HarvesterInterface;
import com.ikanow.infinit.e.harvest.extraction.document.DuplicateManager;
import com.ikanow.infinit.e.harvest.utils.HarvestExceptionUtils;
import com.ikanow.infinit.e.harvest.utils.PropertiesManager;
import com.ikanow.infinit.e.harvest.utils.TextEncryption;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;

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

		if (_context.isStandalone()) {
			maxDocsPerCycle = _context.getStandaloneMaxDocs();
		}
		// Can override system settings if less:
		if ((null != source.getThrottleDocs()) && (source.getThrottleDocs() < maxDocsPerCycle)) {
			maxDocsPerCycle = source.getThrottleDocs();
		}
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
		String dt = source.getDatabaseConfig().getDatabaseType();	
		
		// Create the jdbcUrl connection string for the manager
		jdbcUrl = rdbms.getConnectionString(dt,  
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
		Integer distributionToken = null;
		SourceHarvestStatusPojo lastHarvestInfo = source.getHarvestStatus();
		if (null != lastHarvestInfo) { // either in delta query mode, or truncation mode of initial query
			String harvestMessage = null;
			if ((null == source.getDistributionTokens()) || source.getDistributionTokens().isEmpty()) {
				harvestMessage = lastHarvestInfo.getHarvest_message();
			}
			else { // (currently only support one slice per harvester)
				distributionToken = source.getDistributionTokens().iterator().next();
				if (null != lastHarvestInfo.getDistributedStatus()) {
					harvestMessage = lastHarvestInfo.getDistributedStatus().get(distributionToken.toString());
				}
			}//TODO (INF-2120): TOTEST
			
			if (null != harvestMessage) {
				if (harvestMessage.startsWith("query:")) { // Initial query
					try {
						int nEndOfNum = harvestMessage.indexOf('.', 6);
						if (nEndOfNum > 0) {
							nNumRecordsToSkip = Integer.parseInt(harvestMessage.substring(6, nEndOfNum));
						}
					}
					catch (Exception e) {} // Do nothing, just default to 0
					lastHarvestInfo = null; // (go through initial query branch below)
				}
				else { // Delta query or error!
					if (harvestMessage.startsWith("deltaquery:")) { // Delta query, phew
						try {
							int nEndOfNum = harvestMessage.indexOf('.', 11);
							if (nEndOfNum > 0) {
								nNumRecordsToSkip = Integer.parseInt(harvestMessage.substring(11, nEndOfNum));
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
			
			// Distribution logic, currently manual:
			if ((null != distributionToken) && deltaQuery.contains("!TOKEN!")) {
				deltaQuery = deltaQuery.replace("!TOKEN!", distributionToken.toString());				
			}//TODO (INF-2120): TOTEST		
			if ((null != source.getDistributionFactor()) && deltaQuery.contains("!NTOKENS!")) {
				deltaQuery = deltaQuery.replace("!NTOKENS!", source.getDistributionFactor().toString());				
			}//TODO (INF-2120): TOTEST		
			
			// Delete docs from the index if they have been deleted from the source
			// database. Designed to check a "deleted" table to get records that have been
			// deleted to identify the records to remove form the index.
			if ((source.getDatabaseConfig().getDeleteQuery() != null) && !source.getDatabaseConfig().getDeleteQuery().isEmpty()) 
			{
				deleteQuery = source.getDatabaseConfig().getDeleteQuery();
				
				// Distribution logic, currently manual:
				if ((null != distributionToken) && deleteQuery.contains("!TOKEN!")) {
					deleteQuery = deltaQuery.replace("!TOKEN!", distributionToken.toString());				
				}//TODO (INF-2120): TOTEST		
				if ((null != source.getDistributionFactor()) && deleteQuery.contains("!NTOKENS!")) {
					deleteQuery = deleteQuery.replace("!NTOKENS!", source.getDistributionFactor().toString());				
				}//TODO (INF-2120): TOTEST		
				
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
			
			// Distribution logic, currently manual:
			if ((null != distributionToken) && query.contains("!TOKEN!")) {
				query = query.replace("!TOKEN!", distributionToken.toString());				
			}//TODO (INF-2120): TOTEST					
			if ((null != source.getDistributionFactor()) && query.contains("!NTOKENS!")) {
				query = query.replace("!NTOKENS!", source.getDistributionFactor().toString());				
			}//TODO (INF-2120): TOTEST		
			
			// Set rdbs query = default source.database.query value
			rdbms.setQuery(query);
		}
		
		// Execute the specified query
		String sErr = rdbms.executeQuery();
		
		if (null != sErr) {
			_context.getHarvestStatus().update(source, new Date(), HarvestEnum.error, "Error when harvesting DB: " + sErr, false, false);
		}
		else {
			// Build the list of docs using the result set from the query
			boolean bTruncated = addRecords(rdbms.getResultSet(), rdbms.getMetaData(), source);
			
			// Update source document with harvest success message
			String truncationMode = null;
			if (bTruncated) {
				source.setReachedMaxDocs();
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
				 * jdbc:mysql://<IP ADDRESS>:3306/washingtondc/987863
				 */
				try 
				{
					DuplicateManager qr = _context.getDuplicateManager();
					
			    	String primaryKey = null;
			    	if (null != source.getDatabaseConfig().getPrimaryKey()) {
			    		primaryKey = rs.getString(source.getDatabaseConfig().getPrimaryKey());
			    	}
			    	if (null == primaryKey) { // Just pick something unique, to avoid URL collisions
			    		primaryKey = new ObjectId().toString();
			    	}
			    	String docUrl = source.getDatabaseConfig().getPrimaryKeyValue();
			    	if (null == docUrl) {
			    		docUrl = source.getUrl() + "/" + primaryKey;
			    	}
			    	else {
			    		docUrl = docUrl + primaryKey;
			    	}
			    	
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
						//newOrUpdatedDoc.setId(qr.getLastDuplicateId()); // (set _id to doc we're going to replace)
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
				doc.setUrl(docUrl);
				doc.setCreated(new Date());				
			}
			else 
			{	
				//TODO (INF-1300): support for updated docs (will want to save old creation time, zap everything else?)
			}
			
			doc.setModified(new Date());
			
			// Strip out html if it is present
			if (null != source.getDatabaseConfig().getTitle()) {
				if (rs.getString(source.getDatabaseConfig().getTitle()) != null)
				{
					doc.setTitle(rs.getString(source.getDatabaseConfig().getTitle()).replaceAll("\\<.*?\\>", ""));
				}
			}
			
			if (null != source.getDatabaseConfig().getSnippet()) {
				if (rs.getString(source.getDatabaseConfig().getSnippet()) != null)
				{
					doc.setDescription(rs.getString(source.getDatabaseConfig().getSnippet()).replaceAll("\\<.*?\\>", ""));
				}
			}

			if (null != source.getDatabaseConfig().getPublishedDate()) {
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
			}
			else
			{
				doc.setPublishedDate(new Date());
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
		if (null == o) {
			return null;
		}
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
		else if (o instanceof java.sql.Array) {
			try {
				o = getComplexArray(columnName, (java.sql.Array)o);
			}
			catch (Exception e) { // Fail out gracefully
				return null;
			}
			columnName = null; // (don't convert to string)
		}//TOTEST
		else if (o instanceof java.sql.Struct) {
			try {
				o = getComplexObject((java.sql.Struct)o);
			}
			catch (Exception e) { // Fail out gracefully
				return null;
			}
			columnName = null; // (don't convert to string)
		}//TESTED
		else if ((o instanceof java.math.BigDecimal) || (o instanceof java.math.BigInteger)) {
			o = ((Number)o).longValue();
		}
		else if (!(o instanceof Number) && !(o instanceof Boolean) && !(o instanceof String) ) {
			columnName = null; // (don't convert to string)
			// not sure, try JSON (nuke horrendous oracle fr?
			try {
				o = new com.google.gson.GsonBuilder().setExclusionStrategies(new OracleLogic()).create().toJsonTree(o);
			}
			catch (Exception e) { // If JSON doesn't work, abandon ship
				return null;
			}
		}//TESTED (this is pretty much a worst case, the above code worked on oracle objects though gave a horrendous object in return)
			
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
	private static String getDatabaseDateString(String dt, Date d)
	{
		if (dt.equalsIgnoreCase("mysql")) {
			// yyyy-MM-dd HH:mm:ss
			return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(d);			
		}
		else if (dt.equalsIgnoreCase("oracle")) {
			// dd-MMM-yyyy HH:mm:ss
			return new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss").format(d);			
		}
		else {
			java.sql.Timestamp t = new Timestamp(d.getTime());
			return t.toString();			
		}
	}

	@Override
	public boolean canHarvestType(int sourceType) 
	{
		return sourceTypesCanHarvest.contains(sourceType);
	}
	
	///////////////////////////////////////////////////////////////////////
	//
	// Utils: create objects out of more complex SQL arrays
	
	public static BasicDBList getComplexArray(String columnName, java.sql.Array a) throws IllegalArgumentException, SQLException {
		BasicDBList bsonArray = new BasicDBList();

		Object array = a.getArray();
		int length = Array.getLength(array);
		for (int i = 0; i < length; ++i) {
			Object o = Array.get(array, i);
			bsonArray.add(convertJdbcTypes(columnName, o));
		}
		a.free();
		
		return bsonArray;
	}//TOTEST	
	public static BasicDBObject getComplexObject(java.sql.Struct s) throws SQLException {
		//JsonObject jsonObj = new JsonObject();
		BasicDBObject bsonObj = new BasicDBObject();
		Integer elNo = 0;
		Object[] els = s.getAttributes();
		for (Object el: els) {
			bsonObj.put(elNo.toString(), convertJdbcTypes("", el)); // the "" ensures that primitives will always be treated as strings
				// (which is necessary to avoid elasticsearch-side conflicts)
			elNo++;
		}
		
		return bsonObj;
	}//TESTED
	public static class OracleLogic implements com.google.gson.ExclusionStrategy {
		public boolean shouldSkipClass(Class<?> arg0) {
			String name = arg0.getName();
			System.out.println(name);
			return name.startsWith("oracle.net") || name.startsWith("oracle.jdbc");
		}
		public boolean shouldSkipField(com.google.gson.FieldAttributes f) {
			return false;
		}
	}//TESTED (obviously not comprehensive, just removed the oracle objects that broke it)
	
}
