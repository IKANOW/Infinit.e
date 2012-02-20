package com.ikanow.infinit.e.api.knowledge;

import java.io.ByteArrayInputStream;
import java.util.zip.GZIPInputStream;

import org.apache.log4j.Logger;
import org.bson.types.ObjectId;

import com.ikanow.infinit.e.api.utils.RESTTools;
import com.ikanow.infinit.e.data_model.api.ResponsePojo;
import com.ikanow.infinit.e.data_model.api.ResponsePojo.ResponseObject;
import com.ikanow.infinit.e.data_model.api.knowledge.DocumentPojoApiMap;
import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.MongoDbManager;
import com.ikanow.infinit.e.data_model.store.document.CompressedFullTextPojo;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
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
	/**
	 * Get information function that returns the user information in the form of a JSON String.
	 * 
	 * @param  key	the key definition of the user ( example email@email.com )
	 * @return      a JSON string representation of the person information on success
	 */
	public ResponsePojo getInfo(String userIdStr, String idStr, boolean bReturnFullText) 
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
			if (null == dp) {
				throw new RuntimeException("Document not found");
			}
			if (bReturnFullText) {
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
			rp.setData(dp, new DocumentPojoApiMap());
			rp.setResponse(new ResponseObject("Doc Info",true,"Feed info returned successfully"));
		}
		catch (Exception e)
		{
			// If an exception occurs log the error
			logger.error("Exception Message: " + e.getMessage(), e);
			rp.setResponse(new ResponseObject("Doc Info",false,"error returning feed"));
			//json = new Gson().toJson(new StatusPojo("Error",e.getMessage()));
		}
		// Return Json String representing the user
		return rp;
	}
}

