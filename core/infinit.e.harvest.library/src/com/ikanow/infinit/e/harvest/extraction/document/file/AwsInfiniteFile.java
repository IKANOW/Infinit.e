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
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Date;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
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
		
		// 3 cases .. it can be a bucket (s3://X.Y.com[/]?) 
		// or a "directory" (these are handled slightly oddly in S3) .. eg s3://X.Y.com(/blah)+/
		// or an object s3://X.Y.com(/blah)+
		
		// In all cases let's get the bucket name first...
		url = url.substring(5); // ie step over s3://
		int nIndex = url.indexOf('/');
		if (-1 != nIndex) { // Else it's the entire bucket
			_awsBucketName = url.substring(0, nIndex);
		}
		else {
			_awsBucketName = url;
		}//TESTED
		
		//two cases ... might be a bucket, or might be an object
		if (!url.endsWith("/")) { // Going to assume this is a file
			
			_awsObjectName = url.substring(_awsBucketName.length() + 1); // (+1 to step over the /)
			_awsFileMeta_lastDate = client.getObjectMetadata(_awsBucketName, _awsObjectName).getLastModified();
				// (will fire off an exception if the file doesn't exist)
		}//TESTED
		else if (-1 != nIndex) { // This is a directory
			_awsObjectName = url.substring(_awsBucketName.length() + 1); // (+1 to step over the /)
		}//TESTED
		// (else already done, leave _awsObjectName as null)		
	}
	
	public AwsInfiniteFile(String bucketName, String objectName, Date lastModified, Object client) {
		_awsBucketName = bucketName;
		_awsObjectName = objectName;
		_awsFileMeta_lastDate = lastModified;
		_awsClient = client;
	}//TESTED
	
	//////////////////////////////////////////////////////////////////
	
	// Accessors:
	
	public InputStream getInputStream() throws SmbException, MalformedURLException, UnknownHostException, FileNotFoundException {
		S3Object s3Obj = ((AmazonS3Client)_awsClient).getObject(_awsBucketName, _awsObjectName);
		return s3Obj.getObjectContent();
	}
	
	public InfiniteFile[] listFiles()  {
		InfiniteFile[] fileList = null;
		ObjectListing list = null;
		if (null == _awsObjectName) {
			list = ((AmazonS3Client)_awsClient).listObjects(_awsBucketName);
		}
		else {				
			list = ((AmazonS3Client)_awsClient).listObjects(_awsBucketName, _awsObjectName);
		}
		fileList = new InfiniteFile[list.getObjectSummaries().size()];
		int nAdded = 0;
		for (S3ObjectSummary s3Obj: list.getObjectSummaries()) {
			if (!s3Obj.getKey().endsWith("/")) {
				fileList[nAdded] = new AwsInfiniteFile(s3Obj.getBucketName(), s3Obj.getKey(), s3Obj.getLastModified(), _awsClient);
				nAdded++;
			}
		}
		return fileList;
	}//TESTED (with and without prefixes)
	
	public boolean isDirectory() throws SmbException {
		return (null == _awsFileMeta_lastDate);
	}	
	
	public URI getURL() throws MalformedURLException, URISyntaxException {
		URI url = new URI(new StringBuffer("s3://").append(_awsBucketName).append('/').append(_awsObjectName).toString());
		return url;
	}//TESTED
	
	public String getName() {
		return _awsObjectName.replaceAll(".*/", ""); // remove the leading path
	}//TESTED

	public long getDate() {
		return _awsFileMeta_lastDate.getTime();
	}//TESTED
	
	//////////////////////////////////////////////////////////////////
	
	// STATE
	
	// AWS stuff, which is more complicated
	private Object _awsClient;
	private Date _awsFileMeta_lastDate;
	private String _awsBucketName; // use these so we don't have to download the entire thing to look at metadata
	private String _awsObjectName; // 
}
