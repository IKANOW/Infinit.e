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
import java.util.zip.GZIPInputStream;

import org.apache.log4j.Logger;
import org.bson.types.ObjectId;

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
	public ResponsePojo getInfo(String userIdStr, String idStr, boolean bReturnFullText, boolean returnRawData) 
	{
		ResponsePojo rp = new ResponsePojo();
		
		try 
		{
			// Set up the query
			BasicDBObject query = new BasicDBObject();
			query.put(DocumentPojo._id_, new ObjectId(idStr));
				// (use DBObject here because DocumentPojo is pretty big and this call could conceivably have perf implications)
			query.put(DocumentPojo.communityId_, new BasicDBObject(MongoDbManager.in_, RESTTools.getUserCommunities(userIdStr)));
			
			DocumentPojo dp = DocumentPojo.fromDb(DbManager.getDocument().getMetadata().findOne(query), DocumentPojo.class);
			if (null == dp) 
			{
				throw new RuntimeException("Document not found");
			}
			if (bReturnFullText) 
			{
				byte[] storageArray = new byte[200000];
				DBCollection contentDB = DbManager.getDocument().getContent();
				BasicDBObject contentQ = new BasicDBObject(CompressedFullTextPojo.url_, dp.getUrl());
				BasicDBObject dboContent = (BasicDBObject) contentDB.findOne(contentQ);
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
			else if ( returnRawData )
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
						//fail
						rp.setResponse(new ResponseObject("Doc Info",false,"Could not find document"));
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
		}
		catch (Exception e)
		{
			// If an exception occurs log the error
			logger.error("Exception Message: " + e.getMessage(), e);
			rp.setResponse(new ResponseObject("Doc Info",false,"error returning feed"));
		}
		// Return Json String representing the user
		return rp;
	}
	
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
		String mediaType = "text/plain";
		int mid = url.lastIndexOf(".");
		String extension = url.substring(mid+1, url.length());
		if ( extension.equals("pdf"))
			return "application/pdf";
		else if ( extension.equals("xml"))
			return "text/xml";
		return mediaType;
	}
}

