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
package com.ikanow.infinit.e.harvest.test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Date;

import jcifs.smb.NtlmPasswordAuthentication;

import com.ikanow.infinit.e.data_model.Globals;
import com.ikanow.infinit.e.harvest.extraction.document.file.InfiniteFile;

//TEST CASES COMMAND LINE ARGS:
// 1a] C:\\Users\\acp\workspace-ikanow-v0\\utility.dev.ikanow.config\core_config file://C:\\Users\\acp\\test delete|rename
// 1b] C:\\Users\\acp\workspace-ikanow-v0\\utility.dev.ikanow.config\core_config file://C:\\Users\\acp\\test delete|rename
//(^^^identical but set to read only)
// 2a] C:\\Users\\acp\workspace-ikanow-v0\\utility.dev.ikanow.config\core_config smb://modus:139/infinit.e_files/test delete|rename <auth>
//(^^^ read only section so tests should all fail)
// 2b] C:\\Users\\acp\workspace-ikanow-v0\\utility.dev.ikanow.config\core_config smb://modus:139/infinit.e_rw/test delete|rename <auth>
// 3] C:\\Users\\acp\workspace-ikanow-v0\\utility.dev.ikanow.config\core_config s3://test.ikanow.com/test/ delete|rename <auth>
// 3b] C:\\Users\\acp\workspace-ikanow-v0\\utility.dev.ikanow.config\core_config s3://test.ikanow.com/test/ delete|rename|list <auth>
//(^^^ hand made read only for the purpose of the test from S3 console)

public class InfiniteFileTest {
	
	public static void main(String[] args) throws URISyntaxException, IOException {
		// Configuration:
		
		System.out.println(Arrays.toString(args));
		Globals.setIdentity(com.ikanow.infinit.e.data_model.Globals.Identity.IDENTITY_SERVICE);
		Globals.overrideConfigLocation(args[0]);

		// <config> <url> <user> <password> <domain>
		String fileUrl = args[1];
		NtlmPasswordAuthentication auth = null;			
		
		String actions = args[2]; // delete|rename
		
		if (args.length > 3) {
			String user = args[3];
			String password = args[4];
			String domain = args[5];
			auth = new NtlmPasswordAuthentication(domain, user, password);
		}
		// Load the file from the config
		
		InfiniteFile file = null;
		if (null != auth) {
			file = InfiniteFile.create(fileUrl, auth);
		}
		else {
			file = InfiniteFile.create(fileUrl);
		}
		// Tests...
		boolean doActions = true;
		
		System.out.println("X.1]: " + new Date(file.getDate()));
		InfiniteFile[] files = file.listFiles();
		System.out.println("X.2]: " + new Date(file.getDate()));
		int i = 0;
		for (InfiniteFile subFile: files) {
			if (null == subFile) break;
			if (i >= 10) break;
			
			System.out.println("X.3] " + subFile.getUrlString() + " AND " + subFile.getName() + ": " + new Date(subFile.getDate()));
			
			if (!subFile.isDirectory()) ++i;
			else {
				if (actions.contains("list")) {
					for (InfiniteFile subsubFile: subFile.listFiles()) {
						if (null == subsubFile) break;
						System.out.println("X.3.1] subfile " + subsubFile.getUrlString());
					}
				}
				continue;
			}
			
			if (actions.contains("delete")) {
				try {
					if (1 == i) { // delete (X.4)
						System.out.println("CHECK DELETED: " + subFile.getUrlString());
						if (doActions) subFile.delete();
					}
					//ALSO BY HAND make files read only and retry 
				}
				catch (Exception e) {
					System.out.println("X.4] FAILED: " + e.getMessage());
				}
			}
			if (actions.contains("rename")) {
				try {
					if (2 == i) { // rename 1 (X.5.1)
						String newName = "$path/$name.RENAMED";
						System.out.println("CHECK NEW NAME: " + createNewName(subFile, newName));
						if (doActions) subFile.rename(createNewName(subFile, newName));
					}
				}
				catch (Exception e) {
					System.out.println("X.5.1] FAILED: " + e.getMessage());
				}
				try {
					if (3 == i) { // rename 2 (X.5.2)
						String newName = "$path/deleted/$name";
						System.out.println("CHECK NEW NAME: " + createNewName(subFile, newName));
						if (doActions) subFile.rename(createNewName(subFile, newName));
					}
				}
				catch (Exception e) {
					System.out.println("X.5.2] FAILED: " + e.getMessage());
				}
				try {
					if (4 == i) { // rename 3 (X.5.3)
						String newName = "$path/../test2/$name";					
						System.out.println("CHECK NEW NAME: " + createNewName(subFile, newName));
						if (doActions) subFile.rename(createNewName(subFile, newName));
					}
				}
				catch (Exception e) {
					System.out.println("X.5.3] FAILED: " + e.getMessage());
				}
				try {
					if (5 == i) { // fail to rename (X.5.4)
						String newName = "$path/noexisto/$name";
						try {
							if (doActions) subFile.rename(createNewName(subFile, newName));
							throw new RuntimeException("X.5.4.1] FAILED NO EXCEPTION");
						}
						catch (IOException e) {
							System.out.println("X.5.4.1] Passed: " + e.getMessage());
						}					
					}
				}
				catch (Exception e) {
					System.out.println("X.5.4] FAILED: " + e.getMessage());
				}
				
			}
		}
	}	
	private static String createNewName(InfiniteFile subFile, String replacement) throws MalformedURLException, UnsupportedEncodingException, URISyntaxException {
		String path = subFile.getUrlString(); // (currently the entire string)
		String name = subFile.getName();
		int startOfName = path.lastIndexOf(name);
		return replacement.replace("$name", name).replace("$path", path.substring(0, startOfName - 1));
	}
}
