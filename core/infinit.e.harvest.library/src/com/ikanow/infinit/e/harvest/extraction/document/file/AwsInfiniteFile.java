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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.util.Date;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectMetadataRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;

public class AwsInfiniteFile extends InfiniteFile {

	// Constructors
	
	public AwsInfiniteFile(String url, NtlmPasswordAuthentication auth) throws MalformedURLException, SmbException {
		BasicAWSCredentials awsAuth = new BasicAWSCredentials(auth.getUsername(), auth.getPassword());
		AmazonS3Client client = new AmazonS3Client(awsAuth);
		_awsClient = (Object)client;
		
		getBucketAndObjectName(url, false);
	}//TESTED
	
	private void getBucketAndObjectName(String url, boolean newFile) {
		// 3 cases .. it can be a bucket (s3://X.Y.com[/]?) 
		// or a "directory" (these are handled slightly oddly in S3) .. eg s3://X.Y.com(/blah)+/
		// or an object s3://X.Y.com(/blah)+
				
		// In all cases let's get the bucket name first...
		url = url.substring(5); // ie step over s3://
		int index = url.indexOf('/');
		if (-1 != index) { // Else it's the entire bucket
			_awsBucketName = url.substring(0, index);
		}
		else {
			_awsBucketName = url;
		}//TESTED
		
		//two cases ... might be a bucket, or might be an object
		if (!url.endsWith("/")) { // Going to assume this is a file
			
			_awsObjectName = url.substring(_awsBucketName.length() + 1); // (+1 to step over the /)
			if (!newFile) {
				_awsFileMeta_lastDate = ((AmazonS3Client)_awsClient).getObjectMetadata(_awsBucketName, _awsObjectName).getLastModified();
					// (will fire off an exception if the file doesn't exist)
			}
		}//TESTED
		else if (-1 != index) { // This is a directory
			_awsObjectName = url.substring(_awsBucketName.length() + 1); // (+1 to step over the /)
		}//TESTED
		// (else already done, leave _awsObjectName as null)		
	}//TESTED
	
	public AwsInfiniteFile(String bucketName, String objectName, Date lastModified, Object client) {
		_awsBucketName = bucketName;
		_awsObjectName = objectName;
		_awsFileMeta_lastDate = lastModified;
		_awsClient = client;
	}//TESTED
	
	//////////////////////////////////////////////////////////////////
	
	// Accessors:
	
	@Override
	public InputStream getInputStream() throws SmbException, MalformedURLException, UnknownHostException, FileNotFoundException {
		S3Object s3Obj = ((AmazonS3Client)_awsClient).getObject(_awsBucketName, _awsObjectName);
		return s3Obj.getObjectContent();
	}
	
	@Override
	public InfiniteFile[] listFiles()  {
		InfiniteFile[] fileList = null;
		ObjectListing list = null;
		_overwriteTime = 0L;
		ListObjectsRequest listRequest = new ListObjectsRequest().withBucketName(_awsBucketName);
		if (null != _awsObjectName) {
			listRequest.withPrefix(_awsObjectName);
		}
		listRequest.withDelimiter("/");
		list = ((AmazonS3Client)_awsClient).listObjects(listRequest);
		fileList = new InfiniteFile[list.getObjectSummaries().size() + list.getCommonPrefixes().size()];
		//TESTED (3.2)
		int nAdded = 0;
		// Get the sub-directories
		for (String subDir: list.getCommonPrefixes()) {
			// Create directories:
			fileList[nAdded] = new AwsInfiniteFile(_awsBucketName, subDir, null, _awsClient);
			nAdded++;
		}//TESTED (3b.3)
		// Get the files:
		for (S3ObjectSummary s3Obj: list.getObjectSummaries()) {
			if (!s3Obj.getKey().endsWith("/")) {
				fileList[nAdded] = new AwsInfiniteFile(s3Obj.getBucketName(), s3Obj.getKey(), s3Obj.getLastModified(), _awsClient);
				long fileTime = fileList[nAdded].getDate();
				if (fileTime > _overwriteTime) {
					_overwriteTime = fileTime;
				}//TESTED (3.2)
				nAdded++;
			}
		}
		return fileList;
	}//TESTED (with and without prefixes)
	
	@Override
	public void delete() throws IOException {
		((AmazonS3Client)_awsClient).deleteObject(_awsBucketName, _awsObjectName);
	}//TESTED (3.4 and 3b.4)
	
	@Override
	public void rename(String newPathName) throws IOException {
		try {
			String oldBucket = _awsBucketName;
			String oldName = _awsObjectName;
			getBucketAndObjectName(newPathName, true); // (renames self)
			_awsObjectName = new URI("").resolve(_awsObjectName).getPath(); // (resolve relative paths)
			
			// Check parent directory exists:
			int index = _awsObjectName.lastIndexOf('/');
			if (index > 0) {
				String oldParentDir = _awsObjectName.substring(0, 1+index); // (don't include the "/" so will try to create)
				
				GetObjectMetadataRequest objMetaRequest = new GetObjectMetadataRequest(_awsBucketName, oldParentDir);
				((AmazonS3Client)_awsClient).getObjectMetadata(objMetaRequest);
			}		
			// Create actual file:
			
			((AmazonS3Client)_awsClient).copyObject(oldBucket, oldName, _awsBucketName, _awsObjectName);
			
			_awsBucketName = oldBucket;
			_awsObjectName = oldName; // (copy back again before deleting)
			delete(); // (original, only gets this far if the copyObject succeeds, else it exceptions out)
		}
		catch (AmazonS3Exception e) {
			throw new IOException(e.getMessage());
		} catch (URISyntaxException e) {
			throw new IOException(e.getMessage());
		}
	}//TESTED (3.4, 3.5)
	
	@Override
	public boolean isDirectory() throws SmbException {
		return (null == _awsFileMeta_lastDate);
	}	
	
	@Override
	public String getUrlString() throws MalformedURLException, URISyntaxException
	{
		return new StringBuffer("s3://").append(_awsBucketName).append('/').append(_awsObjectName).toString();
	}//TESTED
	@Override
	public String getUrlPath() throws MalformedURLException, URISyntaxException, UnsupportedEncodingException
	{
		return URLDecoder.decode(getURI().getPath(), "UTF-8");
	}//TESTED
	@Override
	public URI getURI() throws MalformedURLException, URISyntaxException {
		URI uri = new URI("s3", _awsBucketName, "/" + _awsObjectName, null);
		return uri;
	}//TESTED
	
	@Override
	public String getName() {
		return _awsObjectName.replaceAll(".*/", ""); // remove the leading path
	}//TESTED

	@Override
	public long getDate() {
		if (null != _overwriteTime) {
			return _overwriteTime;
		}//TESTED (3.2)
		else if (null == _awsFileMeta_lastDate) {
			return 0;
		}//TESTED (3.1)
		else {
			return (_overwriteTime = _awsFileMeta_lastDate.getTime());
		}//TEST (3.3)
	}//TESTED
	
	//////////////////////////////////////////////////////////////////
	
	// STATE
	
	// AWS stuff, which is more complicated
	private Object _awsClient;
	private Date _awsFileMeta_lastDate;
	private String _awsBucketName; // use these so we don't have to download the entire thing to look at metadata
	private String _awsObjectName; // 
}
