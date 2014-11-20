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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;

import org.apache.commons.io.FileUtils;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;

// Handles case of either local or SMB (or in the future other) file systems

public class InfiniteFile {

	// Constructors:
	
	public static InfiniteFile create(String url) throws IOException {
		return new InfiniteFile(url);			
	}
	public static InfiniteFile create(String url, NtlmPasswordAuthentication auth) throws IOException {
		try {
			if (url.startsWith("s3://")) {
				return new AwsInfiniteFile(url, auth);
			}
			else if (url.startsWith(InternalInfiniteFile.INFINITE_PREFIX)) {
				return new InternalInfiniteFile(url, auth);
			}
			else {
				return new InfiniteFile(url, auth);
			}
		}
		catch (SmbException e) {
			throw new IOException(e.getMessage(), e);
		}
		
	}
	
	////////////////////////////
	
	protected InfiniteFile() {}
	
	protected InfiniteFile(String url) throws IOException {
		try {
			if (url.startsWith("file://")) {
				_localFile = new File(url.substring(7)); // ie "file://", path is relative to ~tomcat I guess
			}
			else if (url.startsWith("file:")) { // (apparently the jcifs doesn't need the "//" bit in file)
				_localFile = new File(url.substring(5)); // ie "file:", path is relative to ~tomcat I guess
			}
			else {
				_smbFile = new SmbFile(url);
				if (!_smbFile.exists()) {
					throw new MalformedURLException(url + " NOT FOUND");
				}
			}
		}
		catch (SmbException e) {
			throw new IOException(e.getMessage(), e);
		}
	}
	protected InfiniteFile(String url, NtlmPasswordAuthentication auth) throws IOException {
		try {
			_smbFile = new SmbFile(url, auth);
			_auth = auth;
			if (!_smbFile.exists()) {
				throw new MalformedURLException(url + " NOT FOUND");
			}
		}
		catch (SmbException e) {
			throw new IOException(e.getMessage(), e);
		}
	}
	private InfiniteFile(SmbFile smbFile, NtlmPasswordAuthentication auth) {	
		_smbFile = smbFile;
		_auth = auth;
	}
	private InfiniteFile(File localFile) {
		_localFile = localFile;
	}
	
	// Access input stream from file
	
	public InputStream getInputStream() throws IOException {
		try {
			if (null != _smbFile) {
				return new SmbFileInputStream(_smbFile);
			}
			else if (null != _localFile) {
				return new FileInputStream(_localFile);
			}
		}
		catch (SmbException e) {
			throw new IOException(e.getMessage(), e);
		}
		return null;
	}
	
	public InfiniteFile[] listFiles() throws IOException {
		try {
			return listFiles(null, Integer.MAX_VALUE);
		}
		catch (SmbException e) {
			throw new IOException(e.getMessage(), e);
		}
	}
	public InfiniteFile[] listFiles(Date optionalFilterDate, int maxFiles) throws IOException {
		// (filterDate does nothing for file types)
		
		try {
			_overwriteTime = 0L;
			InfiniteFile[] fileList = null;
			if (null != _smbFile) {
				SmbFile[] smbFileList = _smbFile.listFiles(); 
				if (null != smbFileList) {
					fileList = new InfiniteFile[smbFileList.length];
					for (int i = 0; i < smbFileList.length; ++i) {
						fileList[i] = new InfiniteFile(smbFileList[i], _auth);
						long fileTime = fileList[i].getDate();
						if (fileTime > _overwriteTime) {
							_overwriteTime = fileTime;
						}//TESTED (2*.1, 2*.2)
					}
				}
			}
			else {
				File[] localFileList = _localFile.listFiles(); 
				if (null != localFileList) {
					fileList = new InfiniteFile[localFileList.length];
					for (int i = 0; i < localFileList.length; ++i) {
						fileList[i] = new InfiniteFile(localFileList[i]);
						long fileTime = fileList[i].getDate();
						if (fileTime > _overwriteTime) {
							_overwriteTime = fileTime;
						}//TESTED (1.1, 1.2)
					}
				}
			}
			return fileList;
		}
		catch (SmbException e) {
			throw new IOException(e.getMessage(), e);
		}
	}
	
	public void delete() throws IOException {
		if (null != _smbFile) {
			try {
				_smbFile.delete();
			} catch (SmbException e) {
				throw new IOException(e.getMessage());
			}
		}//TESTED (2a.4, 2b.4 - success and fail)
		else if (null != _localFile){
			if (!_localFile.delete()) {
				throw new IOException("Access permission error on delete");
			}
		}//TESTED (1a.4, 1b.4 - success and fail)
		else {
			throw new IOException("Operation (delete) not supported");			
		}//TESTED (InternalInfiniteFile, 7.8)
	}//TESTED
	
	public void rename(String newPathName) throws IOException {
		if (null != _smbFile) {
			try {
				if (null != _auth) {
					_smbFile.renameTo(new SmbFile(newPathName, _auth));
				}
				else {
					_smbFile.renameTo(new SmbFile(newPathName));
				}
			} catch (SmbException e) {
				throw new IOException(e.getMessage());
			}
		}//TESTED (2a.5.*, 2b.5.* - various success and fail)
		else if (null != _localFile) {
			InfiniteFile toRename = InfiniteFile.create(newPathName);
			File dest = toRename._localFile;
			if (!dest.getParentFile().exists()) {
				throw new IOException("Rename failed: parent directory doesn't exist");							
			}
			FileUtils.moveFile(_localFile, dest);
		}//TESTED (1a.5.*, 1b.5.* - various success and fail)
		else {
			throw new IOException("Operation (rename) not supported");			
		}//TESTED (InternalInfiniteFile, 7.9)
	}//TESTED
	
	public boolean isDirectory() throws IOException {
		try {
			if (null != _smbFile) {
				return _smbFile.isDirectory(); 
			}
			else {
				return _localFile.isDirectory();
			}
		}
		catch (SmbException e) {
			throw new IOException(e.getMessage(), e);
		}
	}
	@SuppressWarnings("deprecation")
	public String getUrlString() throws MalformedURLException, URISyntaxException
	{
		if (null != _smbFile) {
			return _smbFile.toURL().toString(); // (confirmed spaces in paths works here)
		}		
		else {
			return _localFile.toURL().toString(); // (confirmed spaces in paths works here)
		}
	}//TESTED
	@SuppressWarnings("deprecation")
	public String getUrlPath() throws MalformedURLException, URISyntaxException, UnsupportedEncodingException
	{
		if (null != _smbFile) {
			return _smbFile.toURL().getPath(); // (confirmed spaces in paths works here)
		}		
		else {
			return _localFile.toURL().getPath(); // (confirmed spaces in paths works here)
		}
	}//TESTED
	public URI getURI() throws MalformedURLException, URISyntaxException { // (note this doesn't work nicely with spaces)
		if (null != _smbFile) {
			URL url = _smbFile.getURL(); 
			return new URI(url.getProtocol(), url.getHost(), url.getPath(), url.getQuery(), null);
				// (this odd construct is needed to handle spaces in paths)
		}
		else {
			return _localFile.toURI(); // (confirmed spaces in paths works here)
		}
	}//TESTED
	public String getName() {
		if (null != _smbFile) {
			return _smbFile.getName(); 
		}
		else {
			return _localFile.getName();
		}
	}
	public long getDate() {
		if (null != _overwriteTime) {
			return _overwriteTime;
		}
		else if (null != _smbFile) {
			return (_overwriteTime = _smbFile.getDate()); 
		}
		else {
			return (_overwriteTime = _localFile.lastModified());
		}
	}
	
	// Internal state
		
	// SMB/local
	private SmbFile _smbFile;
	private File _localFile;
	private NtlmPasswordAuthentication _auth;
	protected Long _overwriteTime; 
		// (also caches date values)
}
