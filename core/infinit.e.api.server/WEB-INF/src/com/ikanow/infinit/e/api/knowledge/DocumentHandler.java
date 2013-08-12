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
package com.ikanow.infinit.e.api.knowledge;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;

import org.apache.log4j.Logger;
import org.bson.types.ObjectId;

import com.ikanow.infinit.e.api.utils.MimeUtils;
import com.ikanow.infinit.e.api.utils.RESTTools;
import com.ikanow.infinit.e.data_model.api.ApiManager;
import com.ikanow.infinit.e.data_model.api.ResponsePojo;
import com.ikanow.infinit.e.data_model.api.ResponsePojo.ResponseObject;
import com.ikanow.infinit.e.data_model.api.knowledge.DocumentPojoApiMap;
import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.MongoDbManager;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.ikanow.infinit.e.data_model.store.document.CompressedFullTextPojo;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.infinit.e.harvest.extraction.document.file.FileHarvester;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;


/**
 * This class is for all operations related to the retrieval, addition
 * or update of people within the system
 * 
 * @author cmorgan
 *
 */
public class DocumentHandler 
{
	private static final Logger logger = Logger.getLogger(DocumentHandler.class);
	
	// Utility class used to pass binary/text info between doc handler and interface
	// (but it's changed into a doc object before being sent out the API)
	
	public static class DocumentFileInterface 
	{
		public byte[] bytes;
		public String mediaType;
	}
	
	/**
	 * Get information function that returns the user information in the form of a JSON String.
	 * 
	 * @param  key	the key definition of the user ( example email@email.com )
	 * @return      a JSON string representation of the person information on success
	 */
	public ResponsePojo getInfo(String userIdStr, String sourceKey, String idStrOrUrl, boolean bReturnFullText, boolean returnRawData) 
	{
		ResponsePojo rp = new ResponsePojo();
		
		try 
		{
			// Set up the query
			BasicDBObject query = new BasicDBObject();
			ObjectId id = null;
			if (null == sourceKey) {
				id = new ObjectId(idStrOrUrl);
				query.put(DocumentPojo._id_, id);				
			}
			else {
				query.put(DocumentPojo.sourceKey_, sourceKey);
				query.put(DocumentPojo.url_, idStrOrUrl);
			}
			query.put(DocumentPojo.communityId_, new BasicDBObject(MongoDbManager.in_, RESTTools.getUserCommunities(userIdStr)));
				// (use DBObject here because DocumentPojo is pretty big and this call could conceivably have perf implications)
			BasicDBObject dbo = (BasicDBObject) DbManager.getDocument().getMetadata().findOne(query);

			if ((null == dbo) || ((null != dbo.get(DocumentPojo.sourceKey_)) && 
									dbo.getString(DocumentPojo.sourceKey_).startsWith("?DEL?")))
			{
				if (null != id) { // this might be the update id...					
					query = new BasicDBObject(DocumentPojo.updateId_, id);
					dbo = (BasicDBObject) DbManager.getDocument().getMetadata().findOne(query);
				}
			}
			//TESTED (update case, normal case, and intermediate case where both update and original still exist)
			
			if (null == dbo) {
				rp.setResponse(new ResponseObject("Doc Info",true,"Document not found"));
				return rp;
			}
			DocumentPojo dp = DocumentPojo.fromDb(dbo, DocumentPojo.class);
			if (bReturnFullText) 
			{
				if (null == dp.getFullText()) { // (Some things like database records might have this stored already)
					byte[] storageArray = new byte[200000];
					DBCollection contentDB = DbManager.getDocument().getContent();
					BasicDBObject contentQ = new BasicDBObject(CompressedFullTextPojo.url_, dp.getUrl());
					contentQ.put(CompressedFullTextPojo.sourceKey_, new BasicDBObject(MongoDbManager.in_, Arrays.asList(null, dp.getSourceKey())));
					BasicDBObject fields = new BasicDBObject(CompressedFullTextPojo.gzip_content_, 1);
					BasicDBObject dboContent = (BasicDBObject) contentDB.findOne(contentQ, fields);
					if (null != dboContent) {
						byte[] compressedData = ((byte[])dboContent.get(CompressedFullTextPojo.gzip_content_));				
						ByteArrayInputStream in = new ByteArrayInputStream(compressedData);
						GZIPInputStream gzip = new GZIPInputStream(in);				
						int nRead = 0;
						StringBuffer output = new StringBuffer();
						while (nRead >= 0) {
							nRead = gzip.read(storageArray, 0, 200000);
							if (nRead > 0) {
								String s = new String(storageArray, 0, nRead, "UTF-8");
								output.append(s);
							}
						}
						dp.setFullText(output.toString());
						dp.makeFullTextNonTransient();
					}
				}				
			}
			else if (!returnRawData) {
				dp.setFullText(null); // (obviously will normally contain full text anyway)
			}
			else // if ( returnRawData )
			{
				//check if the harvest type is file, return the file instead
				//if file is db return the json
				//get source
				SourcePojo source = getSourceFromKey(dp.getSourceKey());
				if ( source.getExtractType().equals( "File" ))
				{
					//get file from harvester
					String fileURL = dp.getUrl();
					if ( dp.getSourceUrl() != null )
						fileURL = dp.getSourceUrl();
					byte[] bytes = FileHarvester.getFile(fileURL, source);
					if ( bytes == null )
					{
						// Try returning JSON instead
						String json = ApiManager.mapToApi(dp, new DocumentPojoApiMap());
						DocumentFileInterface dfp = new DocumentFileInterface();
						
						dfp.bytes = json.getBytes();
						dfp.mediaType = "application/json";
						
						rp.setResponse(new ResponseObject("Doc Info",true,"Document bytes returned successfully"));
						rp.setData(dfp, null);
						return rp;
					}
					else
					{						
						DocumentFileInterface dfp = new DocumentFileInterface();
						dfp.bytes = bytes;
						dfp.mediaType = getMediaType(fileURL);
						rp.setResponse(new ResponseObject("Doc Info",true,"Document bytes returned successfully"));
						rp.setData(dfp, null);
						return rp;
					}
				}
				else
				{				
					String json = ApiManager.mapToApi(dp, new DocumentPojoApiMap());
					DocumentFileInterface dfp = new DocumentFileInterface();
					
					dfp.bytes = json.getBytes();
					dfp.mediaType = "application/json";
					
					rp.setResponse(new ResponseObject("Doc Info",true,"Document bytes returned successfully"));
					rp.setData(dfp, null);
					return rp;
				}				
			}
			rp.setData(dp, new DocumentPojoApiMap());
			rp.setResponse(new ResponseObject("Doc Info",true,"Feed info returned successfully"));
		}//(end full text vs raw data)
		catch (Exception e)
		{
			// If an exception occurs log the error
			logger.error("Exception Message: " + e.getMessage(), e);
			rp.setResponse(new ResponseObject("Doc Info",false,"error returning feed: " + e.getMessage()));
		}
		// Return Json String representing the user
		return rp;
	}
	
	public ResponsePojo getFileContents(String userIdStr, String sourceKey, String relativePath) {
		ResponsePojo rp = new ResponsePojo();
		
		try  {
			BasicDBObject query = new BasicDBObject(SourcePojo.key_, sourceKey);
			query.put(SourcePojo.communityIds_, new BasicDBObject(MongoDbManager.in_, RESTTools.getUserCommunities(userIdStr)));
			BasicDBObject fields = new BasicDBObject(SourcePojo.url_, 1);
			fields.put(SourcePojo.extractType_, 1);
			fields.put(SourcePojo.file_, 1);
			fields.put(SourcePojo.isApproved_, 1);
			SourcePojo source = SourcePojo.fromDb(DbManager.getIngest().getSource().findOne(query, fields), SourcePojo.class);

			// TEST for security shenanigans
			String baseRelativePath = new File(".").getCanonicalPath();
			String actualRelativePath = new File(relativePath).getCanonicalPath();
			if (!actualRelativePath.startsWith(baseRelativePath)) {
				throw new RuntimeException("Access denied: " + relativePath);
			}			
			//(end security shenanigans)
			
			if (null == source) {
				throw new RuntimeException("Document source not found: " + sourceKey);
			}
			if ((null != source.getExtractType()) && !source.getExtractType().equals("File")) {
				throw new RuntimeException("Document source not a file: " + sourceKey + ", " + source.getExtractType());				
			}
			if (!source.isApproved()) {
				throw new RuntimeException("Document source not approved, access denied: " + sourceKey);				
			}
			String fileURL = source.getUrl() + relativePath;
			byte[] bytes = FileHarvester.getFile(fileURL, source);
			if ( bytes == null )
			{
				//fail
				rp.setResponse(new ResponseObject("Doc Info",false,"Could not find document: " + relativePath));
				return rp;
			}
			else
			{						
				DocumentFileInterface dfp = new DocumentFileInterface();
				dfp.bytes = bytes;
				dfp.mediaType = getMediaType(fileURL);
				rp.setResponse(new ResponseObject("Doc Info",true,"Document bytes returned successfully"));
				rp.setData(dfp, null);
				return rp;
			}			
		}
		catch (Exception e)
		{
			// If an exception occurs log the error
			logger.error("Exception Message: " + e.getMessage(), e);
			rp.setResponse(new ResponseObject("Doc Info",false,"error returning feed: " + e.getMessage()));
		}
		// Return Json String representing the user
		return rp;
	}//TESTED
	
	private SourcePojo getSourceFromKey(String sourceKey)
	{
		SourcePojo source = null;
		try
		{
			BasicDBObject query = new BasicDBObject();
			query.put(SourcePojo.key_, sourceKey);
			source = SourcePojo.fromDb(DbManager.getIngest().getSource().findOne(query), SourcePojo.class);
		}
		catch (Exception e)
		{

		}
		return source;
	}
	
	private String getMediaType(String url)
	{		
		String mediaType = null;

		int end = url.lastIndexOf("?");
		if (end >= 0) {
			url = url.substring(0, end);
		}
		int mid = url.lastIndexOf(".");
		String extension = url.substring(mid+1, url.length());
		mediaType = MimeUtils.lookupMimeType(extension);
		if (null == mediaType) {
			mediaType = "text/plain";
		}
		return mediaType;
	}//TESTED
}

