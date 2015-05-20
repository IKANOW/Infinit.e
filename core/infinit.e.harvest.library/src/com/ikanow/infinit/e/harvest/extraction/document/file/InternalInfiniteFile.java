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
package com.ikanow.infinit.e.harvest.extraction.document.file;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

import net.sf.jazzlib.GridFSZipFile;
import net.sf.jazzlib.ZipEntry;

import org.bson.types.ObjectId;

import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.MongoDbManager;
import com.ikanow.infinit.e.data_model.store.custom.mapreduce.CustomMapReduceJobPojo;
import com.ikanow.infinit.e.data_model.store.social.sharing.SharePojo;
import com.ikanow.infinit.e.harvest.utils.AuthUtils;
import com.ikanow.utility.GridFSRandomAccessFile;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.gridfs.GridFSDBFile;

import jcifs.smb.NtlmPasswordAuthentication;

public class InternalInfiniteFile extends InfiniteFile {
	
	public static final String INFINITE_PREFIX = "inf://";
	public static final String INFINITE_SHARE_PREFIX = "inf://share/";
	public static final int INFINITE_SHARE_PREFIX_LEN = 12;
	public static final String INFINITE_CUSTOM_PREFIX = "inf://custom/";
	public static final int INFINITE_CUSTOM_PREFIX_LEN = 13;
	
	//////////////////////////////////////////////////////////////////

	// INTERFACE C'TORS
	
	public InternalInfiniteFile(String url, NtlmPasswordAuthentication auth) throws MalformedURLException {
		try {
			ObjectId locationId = null;
			
			ObjectId ownerId = null;
			String communityIdsStr = null;

			if (url.startsWith(INFINITE_SHARE_PREFIX)) {
				_isShare = true;
				locationId = new ObjectId(url.substring(INFINITE_SHARE_PREFIX_LEN).replaceFirst("/.*$", "")); // remove trailing /s, can be used for information
				//TESTED (2.1, 2.2.1, 2.3)
				
				BasicDBObject query = new BasicDBObject(SharePojo._id_, locationId);
				_resultObj = (BasicDBObject) MongoDbManager.getSocial().getShare().findOne(query);
				if (null == _resultObj) {
					throw new MalformedURLException("Not found (or not authorized): " + url);					
				}//TESTED (7.1)
				String mediaType = (String) _resultObj.get(SharePojo.mediaType_);
				if ((null != mediaType) && (mediaType.equalsIgnoreCase("application/x-zip-compressed") || mediaType.equalsIgnoreCase("application/zip")))
				{
					_isDirectory = true;
					ObjectId fileId = _resultObj.getObjectId(SharePojo.binaryId_);
					
					GridFSRandomAccessFile file = new GridFSRandomAccessFile(MongoDbManager.getSocial().getShareBinary(), fileId);					
					_zipView = new GridFSZipFile(_resultObj.getString(SharePojo.title_), file);
				}//TESTED (3.1)
				else { // Single share
					if (_resultObj.containsField(SharePojo.documentLocation_)) {
						throw new MalformedURLException("Reference shares are not currently supported");
					}//TESTED (0.1)
					
					_isDirectory = false; // (this will get traversed as the initial "directory", which doesn't check isDirectory...
						//... and will return itself as a single file in the "directory")
				}//TESTED (1.1, 2.1, 3.1)
			}//TESTED 
			else if (url.startsWith(INFINITE_CUSTOM_PREFIX)) {
				_isCustom = true;
				_isDirectory = true;
				BasicDBObject query = null;
				String locationStr = url.substring(INFINITE_CUSTOM_PREFIX_LEN).replaceFirst("/.*$", "");
				StringBuffer sb = new StringBuffer(INFINITE_CUSTOM_PREFIX);
				try {
					locationId = new ObjectId(locationStr);
					query = new BasicDBObject(CustomMapReduceJobPojo._id_, locationId);
				}//TESTED (4.1)
				catch (Exception e) { // for custom jobs can also specify the job name
					query = new BasicDBObject(CustomMapReduceJobPojo.jobtitle_, locationStr);
				}//TESTED (5.1, 6.1)
				_resultObj = (BasicDBObject) MongoDbManager.getCustom().getLookup().findOne(query);
				if (null == _resultObj) {
					throw new MalformedURLException("Not found (or not authorized): " + url);					
				}//TESTED (7.2, 7.3)
				if (null != locationId) {
					sb.append(locationStr).append('/').append(_resultObj.getString(CustomMapReduceJobPojo.jobtitle_)).append('/');
				}//TESTED (5.1, 6.1)
				else {
					sb.append(_resultObj.getObjectId(CustomMapReduceJobPojo._id_).toString()).append('/').append(locationStr).append('/');					
				}//TESTED (4.1)			
				_originalUrl = sb.toString();
				_isCustomAppend = _resultObj.getBoolean(CustomMapReduceJobPojo.appendResults_, false);
				
				String outputDatabase = _resultObj.getString(CustomMapReduceJobPojo.outputDatabase_);
				String outputCollection = _resultObj.getString(CustomMapReduceJobPojo.outputCollection_);
				if (null == outputDatabase) {
					outputDatabase = "custommr";
				}
				DBCollection outColl = MongoDbManager.getCollection(outputDatabase, outputCollection);
				BasicDBObject sort = null;
				if (_isCustomAppend) { // Use time of _last_ record as file time
					sort = new BasicDBObject("_id", -1);
				}
				else { // Use time of _first_ record as file time
					sort = new BasicDBObject("_id", 1);
				}//TESTED
				DBCursor dbc = outColl.find().sort(sort).limit(1);
				List<DBObject> firstOrLastRecordList = dbc.toArray();
				if (!firstOrLastRecordList.isEmpty()) {
					_overwriteTime = ((ObjectId)firstOrLastRecordList.iterator().next().get("_id")).getDate().getTime();
				}
				else { // No records, use lastRunTime_ as backup
					_overwriteTime = _resultObj.getDate(CustomMapReduceJobPojo.lastRunTime_, new Date()).getTime();
				}//TOTEST
				
			}//TESTED
			else {
				throw new MalformedURLException("Not recognized: " + url);
			}//TESTED (7.4)
			communityIdsStr = auth.getDomain();
			ownerId = new ObjectId(auth.getUsername());
			
			// Now do some authentication:
			// Check communities first since that involves no external DB queries:
			boolean isAuthorized = false;
			if (_isShare) {
				BasicDBList communities = (BasicDBList) _resultObj.get(SharePojo.communities_);
				for (Object communityObj: communities) {
					BasicDBObject communityDbo = (BasicDBObject) communityObj;
					ObjectId commId = communityDbo.getObjectId("_id");
					if (communityIdsStr.contains(commId.toString())) {
						isAuthorized = true;
						break;
					}
				}
			}//TESTED (7.*)
			else { //_isCustom
				BasicDBList communities = (BasicDBList) _resultObj.get(CustomMapReduceJobPojo.communityIds_);				
				for (Object communityObj: communities) {
					ObjectId commId = (ObjectId) communityObj;
					if (communityIdsStr.equals(commId)) {
						isAuthorized = true;
						break;
					}
				}
			}//TESTED (7.*)
			if (!isAuthorized) { // Still OK ... only if user is an admin
				isAuthorized = AuthUtils.isAdmin(ownerId);
			}//TESTED (1,2,3,4,5,6)
			if (!isAuthorized) { // Permission fail
				throw new MalformedURLException("Not found (or not authorized): " + url);				
			}//TESTED (7.5)
		}
		catch (Exception e) {
			throw new MalformedURLException("Invalid authentication or location: " + e.getMessage());			
		}//(just passed exceptions on)
		// Save original URL
		if (_isShare) { // (custom handled above)
			if (!url.endsWith("/")) { 
				_originalUrl = url + "/";
			}
			else {
				_originalUrl = url;
			}
			
		}//(TESTED 1.3, 2.3, 3.3)
		
	}//TESTED
	
	
	//////////////////////////////////////////////////////////////////

	// INTERNAL C'TORS
	
	// Share/ZIP file
	
	protected InternalInfiniteFile(InternalInfiniteFile parent, String zipFilename) {
		_resultObj = parent._resultObj;
		_zipView = parent._zipView;
		_zipViewFilename = zipFilename;
		_zipEntry = _zipView.getEntry(_zipViewFilename);
		_isDirectory = false;
		_originalUrl = parent._originalUrl;
		_isShare = true;
	}//TESTED (3.2)
	
	// Custom/virtual directory
	
	protected InternalInfiniteFile(InternalInfiniteFile parent, ObjectId startId, ObjectId endId) {
		_resultObj = parent._resultObj;
		_virtualDirStartLimit = startId;
		_virtualDirEndLimit = endId;
		_isDirectory = true;
		_originalUrl = parent._originalUrl;
		_isCustom = true;
		if (null != endId) {
			_overwriteTime = endId.getDate().getTime();			
		}
		else if (null != startId) {
			_overwriteTime = startId.getDate().getTime();			
		}
		else _overwriteTime = parent._overwriteTime;
	}//TESTED (6.2.2) (custom _overwriteTime by hand)
	
	// Custom/file

	protected InternalInfiniteFile(InternalInfiniteFile parent, BasicDBObject document) {
		_resultObj = document;
		_isDirectory = false;
		_originalUrl = parent._originalUrl;
		_isCustom = true;
		
		Object id = _resultObj.get("_id");
		if ((null != id) && (id instanceof ObjectId)) {
			_overwriteTime = ((ObjectId)id).getDate().getTime();
		}
		else _overwriteTime = parent._overwriteTime;
		// (backup for odd/old custom jobs)

	}//TESTED (4.2) (custom _overwriteTime by hand)
	
	//////////////////////////////////////////////////////////////////

	// INTERFACE METHODS
	
	// INTERNAL SPECIFIC
	
	// For custom jobs - if this is an incremental job then process slightly differently
	
	public boolean isAppendingNotReplacing() {
		return _isCustomAppend;
	}
	
	// OVERRIDING
	
	@Override
	public InputStream getInputStream() throws IOException {
		if (!_isDirectory) {
			if (_isShare && (null == _zipView)) {
				String jsonShare = (String) _resultObj.get(SharePojo.share_);
				if (null != jsonShare) {
					return new ByteArrayInputStream(jsonShare.toString().getBytes());					
				}//TESTED (1.4)
				else { // must be binary
					GridFSDBFile file = DbManager.getSocial().getShareBinary().find(_resultObj.getObjectId(SharePojo.binaryId_));
					return file.getInputStream();
				}//TESTED (2.4)
			}
			else if (_isShare) { // then must be a zip file
				try {
					return _zipView.getInputStream(_zipEntry);
				} catch (IOException e) {
					throw new FileNotFoundException(e.getMessage());
				}
			}//TESTED (3.2.1)
			else if (_isCustom) {
				return new ByteArrayInputStream(_resultObj.toString().getBytes());
			}//TESTED (4.2.1)
		}
		return null;
	}

	@Override
	public InfiniteFile[] listFiles()  {
		return listFiles(null, Integer.MAX_VALUE);
	}
	@Override
	public InfiniteFile[] listFiles(Date optionalFilterDate, int maxDocsPerCycle)  {
		if (_isDirectory) {
			if (_isShare) { // must be a zip file
				ArrayList<InfiniteFile> zipFiles = new ArrayList<InfiniteFile>();
				@SuppressWarnings("unchecked")
				Enumeration<net.sf.jazzlib.ZipEntry> entries = _zipView.entries();
				while (entries.hasMoreElements()) {
					net.sf.jazzlib.ZipEntry zipInfo = entries.nextElement();
					InternalInfiniteFile newFile = new InternalInfiniteFile(this, zipInfo.getName());
					zipFiles.add(newFile);
				}
				return zipFiles.toArray(new InfiniteFile[zipFiles.size()]);
			}//TESTED (3.2)
			else if (_isCustom) { // create some virtual directories eg at most 10K per "virtual directory"
				String outputDatabase = _resultObj.getString(CustomMapReduceJobPojo.outputDatabase_);
				String outputCollection = _resultObj.getString(CustomMapReduceJobPojo.outputCollection_);
				if (null == outputDatabase) {
					outputDatabase = "custommr";
				}
				DBCollection outColl = null;
				DBCursor dbc = null;
				if ((null == _virtualDirStartLimit) && (null == _virtualDirEndLimit)) { // Actual directory
					
					DBCollection chunks = MongoDbManager.getCollection("config", "chunks");
					StringBuffer ns = new StringBuffer(outputDatabase).append(".").append(outputCollection);
					dbc = chunks.find(new BasicDBObject("ns", ns.toString()));
					int splits = dbc.count();

					if (splits < 2) { // Nothing to do (unsharded or 1 chunk)
						dbc.close();
						
						outColl = MongoDbManager.getCollection(outputDatabase, outputCollection);
						dbc = outColl.find();
					}//TESTED (4.2)
					else { // Create one virtual dir per split
						InfiniteFile[] virtualDirs = new InfiniteFile[splits];
						int added = 0;
						for (DBObject splitObj: dbc) {
							BasicDBObject minObj = (BasicDBObject) splitObj.get("min");
							BasicDBObject maxObj = (BasicDBObject) splitObj.get("max");
							ObjectId minId = null;
							try {
								minId = (ObjectId) minObj.get("_id");
							}
							catch (Exception e) {} // min key..
							ObjectId maxId = null;
							try {
								maxId = (ObjectId) maxObj.get("_id");
							}
							catch (Exception e) {} // max key..

							//Handle current case where custom jobs are all dumped in with the wrong _id type							
							if ((null != minId) || (null != maxId)) {
								if ((null != maxId) && (null != optionalFilterDate)) { // (also used on the files below)
									
									if (maxId.getDate().getTime() < optionalFilterDate.getTime()) {
										// (the "getTime()"s can overlap across chunks so we have to use minId
										//  and accept that we'll often deserialize 1+ extra chunk every harvest)
										continue;
									}
								}//TESTED (by hand)
								
								InternalInfiniteFile split = new InternalInfiniteFile(this, minId, maxId);
								virtualDirs[added] = split;
								added++;
							}//TESTED (5.2.2, 6.2.2) (chunk skipping by hand)
						}
						dbc.close();
						return virtualDirs;
					}//TESTED (5.2.2, 6.2.2)
				}//TESTED
				else { // Virtual directory
					BasicDBObject query = new BasicDBObject();
					if (null != _virtualDirStartLimit) {
						if (null != optionalFilterDate) {
							ObjectId altStartId = ObjectId.createFromLegacyFormat((int)(optionalFilterDate.getTime()/1000L), 0, 0); 
								//(zero out the inc/machine ids so this query is independent to calling service)
							
							if (altStartId.compareTo(_virtualDirStartLimit) > 0) { // (altStartId > _virtualDirStartLimit)
								query.put(MongoDbManager.gte_, altStartId);
							}
							else {
								query.put(MongoDbManager.gte_, _virtualDirStartLimit);								
							}
						}//TESTED (by hand)
						else { // normal case
							query.put(MongoDbManager.gte_, _virtualDirStartLimit);
						}
					}
					else if (null != optionalFilterDate) { // (first chunk so always overwrite with optionalFilter date if applicable)
						ObjectId altStartId = ObjectId.createFromLegacyFormat((int)(optionalFilterDate.getTime()/1000L), 0, 0); 
						query.put(MongoDbManager.gte_, altStartId);						
					}//TESTED (by hand)
					if (null != _virtualDirEndLimit) {
						query.put(MongoDbManager.lt_, _virtualDirEndLimit);						
					}		

					outColl = MongoDbManager.getCollection(outputDatabase, outputCollection);
					dbc = outColl.find(new BasicDBObject("_id", query)).limit(1 + maxDocsPerCycle);
				}//TESTED (6.2.2) (doc skipping by hand)
				
				if (null != outColl) { // has files, create the actual file objects
					//DEBUG
					//System.out.println("CHUNK: GOT " + dbc.count());
					
					int docCount = dbc.count();
					if (docCount > 1 + maxDocsPerCycle) {
						docCount = 1 + maxDocsPerCycle; // (we're limiting it here anyway)
					}
					InfiniteFile[] docs = new InfiniteFile[docCount];
					int added= 0;
					for (DBObject docObj: dbc) {
						// (if didn't use a query then apply internal filter date by hand)
						if ((null == _virtualDirStartLimit) && (null == _virtualDirEndLimit) && (null != optionalFilterDate)) {
							ObjectId docId = (ObjectId) docObj.get("_id");
							if (optionalFilterDate.getTime() > docId.getDate().getTime()) {
								continue;
							}
						}//TESTED
						
						if (added >= maxDocsPerCycle) { // (we've reached our limit so put the remaining docs in a new directory, will only be used if it has to)
							docs[added] = new InternalInfiniteFile(this, (ObjectId) docObj.get("_id"), _virtualDirEndLimit);
							break;
						}
						else {
							InternalInfiniteFile doc = new InternalInfiniteFile(this, (BasicDBObject) docObj);						
							docs[added] = doc;
						}//TESTED (both cases)
						added++;												
					}
					dbc.close();					
					return docs;
					
				}//TESTED (4.2)
			}
		}
		else { // can just return myself
			InfiniteFile[] retVal = new InfiniteFile[1];
			retVal[0] = this;
			return retVal;
		}//TESTED (1.2, 2.2)
		return null;
	}
	
	//delete and rename will just call the InfiniteFile versions, which will exception out
	
	@Override
	public boolean isDirectory() throws IOException {
		return _isDirectory;
	}
	
	@Override
	public String getUrlString() throws MalformedURLException, URISyntaxException
	{
		return _originalUrl + getName();
	}//TESTED (1.2, 1.3, 2.2, 2.3. 3.2.1, 3.3, 4.2.1, 4.3)
	
	@Override
	public String getUrlPath() throws MalformedURLException, URISyntaxException
	{
		if (null != this._virtualDirStartLimit) {
			return getUrlString().substring(5) + this._virtualDirStartLimit.toString();
		}
		else {
			return getUrlString().substring(5);
		}
	}//TESTED (1.2, 1.3, 2.2, 2.3. 3.2.1, 3.3, 4.2.1, 4.3)
	
	@Override
	public URI getURI() throws MalformedURLException, URISyntaxException {

		return new URI("inf", "", getUrlString().substring(5), null, null);
			//(this odd construct is needed to handle spaces in paths)
	}//TESTED (1.2, 1.3, 2.2, 2.3. 3.2.1, 3.3, 4.2.1, 4.3)

	@Override
	public String getName() {
		if (null != _zipEntry) {
			return _zipViewFilename;
		}//TESTED (3.2.1)
		else if (_isShare) { // (this is both a dir and a file)
			return _resultObj.getString(SharePojo.title_);
		}//TESTED (1.2, 1.3, 2.2, 2.3, 3.3)
		else { // _isCustom			
			if (_isDirectory) { // top level or virtual directory - returns no name 
				return "";									
			}//TESTED (4.3)
			else { // just make it _id, it's the user's responsibility to assign a primary key if you need to keep this unique
				ObjectId updateId = _resultObj.getObjectId("_updateId", null);
				if (null == updateId) {
					return _resultObj.getObjectId("_id").toString();
				}
				else { // I am a modified old object so retain my name for dedup...
					return updateId.toString();
				}//TOTEST
			}//TESTED (4.2.1)
		}
	}//TESTED
	
	@Override
	public long getDate() {
		if (null != _overwriteTime) {
			return _overwriteTime;
		}
		if (_isShare) {
			return (_overwriteTime = _resultObj.getDate(SharePojo.modified_, new Date()).getTime());			
		}
		//Custom will always have _overwriteTime, so this is just to avoid compiler error
		return 0L;
	}//TESTED (1.2, 1.3, 2.2, 2.3, 3.2, 3.3, 4.2, 4.3)

	//////////////////////////////////////////////////////////////////
	
	// STATE
	
	protected BasicDBObject _resultObj = null; // (can be the parent object or a child object)
	
	protected boolean _isDirectory = false;

	protected String _originalUrl = null;
	protected boolean _isShare = false;
	
	protected boolean _isCustom = false;
	protected boolean _isCustomAppend = false;
	
	
	// Custom state:
	protected ObjectId _virtualDirStartLimit = null;
	protected ObjectId _virtualDirEndLimit = null;
	
	// Share stuff:
	protected GridFSZipFile _zipView = null; // (always the parent zip)
	protected ZipEntry _zipEntry = null;
	protected String _zipViewFilename = null; // (just for display)	
}
