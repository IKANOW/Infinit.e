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
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;

// Handles case of either local or SMB (or in the future other) file systems

public class InfiniteFile {

	// Constructors:
	
	public InfiniteFile(String url) throws MalformedURLException {
		if (url.startsWith("file://")) {
			_localFile = new File(url.substring(7)); // ie "file://", path is relative to ~tomcat I guess
		}
		else {
			_smbFile = new SmbFile(url);
		}
	}
	public InfiniteFile(String url, NtlmPasswordAuthentication auth) throws MalformedURLException {
		_smbFile = new SmbFile(url, auth);
	}
	public InfiniteFile(SmbFile smbFile) {
		_smbFile = smbFile;
	}
	public InfiniteFile(File localFile) {
		_localFile = localFile;
	}
	
	// Access input stream from file
	
	public InputStream getInputStream() throws SmbException, MalformedURLException, UnknownHostException, FileNotFoundException {
		if (null != _smbFile) {
			return new SmbFileInputStream(_smbFile);
		}
		else { // _localFile
			return new FileInputStream(_localFile);
		}
	}
	
	public InfiniteFile[] listFiles() throws SmbException {
		
		InfiniteFile[] fileList = null;
		if (null != _smbFile) {
			SmbFile[] smbFileList = _smbFile.listFiles(); 
			if (null != smbFileList) {
				fileList = new InfiniteFile[smbFileList.length];
				for (int i = 0; i < smbFileList.length; ++i) {
					fileList[i] = new InfiniteFile(smbFileList[i]);
				}
			}
		}
		else {
			File[] localFileList = _localFile.listFiles(); 
			if (null != localFileList) {
				fileList = new InfiniteFile[localFileList.length];
				for (int i = 0; i < localFileList.length; ++i) {
					fileList[i] = new InfiniteFile(localFileList[i]);
				}
			}
		}
		return fileList;
	}
	
	public boolean isDirectory() throws SmbException {
		if (null != _smbFile) {
			return _smbFile.isDirectory(); 
		}
		else {
			return _localFile.isDirectory();
		}
	}
	@SuppressWarnings("deprecation")
	public URL getURL() throws MalformedURLException {
		if (null != _smbFile) {
			return _smbFile.getURL(); 
		}
		else {
			return _localFile.toURL();
		}
	}
	public String getName() {
		if (null != _smbFile) {
			return _smbFile.getName(); 
		}
		else {
			return _localFile.getName();
		}
	}
	public long getDate() {
		if (null != _smbFile) {
			return _smbFile.getDate(); 
		}
		else {
			return _localFile.lastModified();
		}
	}
	
	// Internal state
	
	private SmbFile _smbFile;
	private File _localFile;
	
}
