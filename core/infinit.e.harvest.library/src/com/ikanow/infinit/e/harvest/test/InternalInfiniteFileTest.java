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
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Date;
import java.util.Scanner;

import org.bson.types.ObjectId;

import jcifs.smb.NtlmPasswordAuthentication;

import com.ikanow.infinit.e.data_model.Globals;
import com.ikanow.infinit.e.harvest.extraction.document.file.InfiniteFile;
import com.ikanow.infinit.e.harvest.extraction.document.file.InternalInfiniteFile;

public class InternalInfiniteFileTest {

	/**
	 * @param args
	 * @throws URISyntaxException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws URISyntaxException, IOException {
		// Configuration:
		
		System.out.println(Arrays.toString(args));
		Globals.setIdentity(com.ikanow.infinit.e.data_model.Globals.Identity.IDENTITY_SERVICE);
		Globals.overrideConfigLocation(args[0]);

		NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication("515dc962abe2e9427ad9af5a", "506dc16dfbf042893dd6b8f2", null);
			//(invalid community, admin user)
		
		boolean doTest0 = false;
		boolean doTest1 = true;
		boolean doTest2 = true;
		boolean doTest3 = true;
		boolean doTest4 = true;
		boolean doTest5 = false; // (needs to point to inf-demo not inf-dev)
		boolean doTest6 = false;// (needs to point to inf-demo not inf-dev)
		boolean doTest7 = true;
		
		//////////////////////////////////////////////////////
		// 
		// FILE TYPE 0: REFERENCE SHARE (NOT SUPPORTED)
		if (doTest0) 
		{
			System.out.println("***********************************************************");
			try {
				//0.1] construct
				ObjectId fileId = new ObjectId("51eebba0e4b0dcb70c9ff2af"); // (a reference share)
				String url = "inf://share/" + fileId.toString();
				InternalInfiniteFile test0 = new InternalInfiniteFile(url, auth);
				
				throw new RuntimeException(test0.getName() + " incorrectly supported");
			}
			catch (MalformedURLException e) {
				// Correct
				System.out.println("0.1: passed: " + e.getMessage());
			}
		}
		
		//////////////////////////////////////////////////////
		// 
		// FILE TYPE 1: JSON SHARE
		if (doTest1) 
		{
			System.out.println("***********************************************************");
			ObjectId fileId = new ObjectId("519f9a17e4b0ce42e187db82"); // (a json share, type: infinite-entity-alias)
			
			//1.1] construct
			String url = "inf://share/" + fileId.toString();
			InternalInfiniteFile test1 = new InternalInfiniteFile(url, auth);
			
			System.out.println("1.1: " + test1.toString());
			
			//1.2] list files
			InfiniteFile[] test1_2_files = test1.listFiles();
			System.out.println("1.2: LISTED="+test1_2_files.length);
			for (InfiniteFile test1_2_file: test1_2_files) {
				// 1.2.1] child file metadata
				System.out.println("1.2: test1_2_file=" + new Date(test1_2_file.getDate()) + " | " + test1_2_file.getName() + 
						" | " + test1_2_file.getUrlPath() + " | " + test1_2_file.getUrlString() + " | " + test1_2_file.getURI().toString());
			}		
			
			//1.3] metadata
			System.out.println("1.3: test1_file=" + new Date(test1.getDate()) + " | " + test1.getName() + 
					" | " + test1.getUrlPath() + " | " + test1.getUrlString() + " | " + test1.getURI().toString());
			
			//1.4] contents
			System.out.println("1.4: test1_file_contents=");
			System.out.println("\t"+getContentsSample(test1.getInputStream()));
		}
		
		//////////////////////////////////////////////////////
		// 
		// FILE TYPE 2: BINARY SHARE
		if (doTest2) 
		{
			System.out.println("***********************************************************");
			ObjectId fileId = new ObjectId("51ad28a440b4a4f0f757824c"); // (a "binary" share, type: csv)
			
			//2.1] construct
			String url = "inf://share/" + fileId.toString() + "/more_info/";
			InternalInfiniteFile test2 = new InternalInfiniteFile(url, auth);
			
			System.out.println("2.1: " + test2.toString());
			
			//2.2] list files
			InfiniteFile[] test2_2_files = test2.listFiles();
			System.out.println("2.2: LISTED="+test2_2_files.length);
			for (InfiniteFile test2_2_file: test2_2_files) {
				// 2.2.1] child file metadata
				System.out.println("2.2: test2_2_file=" + new Date(test2_2_file.getDate()) + " | " + test2_2_file.getName() + 
						" | " + test2_2_file.getUrlPath() + " | " + test2_2_file.getUrlString() + " | " + test2_2_file.getURI().toString());
			}		
			
			//2.3] metadata
			System.out.println("2.3: test2_file=" + new Date(test2.getDate()) + " | " + test2.getName() + 
					" | " + test2.getUrlPath() + " | " + test2.getUrlString() + " | " + test2.getURI().toString());
			
			//2.4] contents
			System.out.println("2.4: test2_file_contents=");
			System.out.println("\t"+getContentsSample(test2.getInputStream()));
		}
		
		//////////////////////////////////////////////////////
		// 
		// FILE TYPE 3: ZIP SHARE
		if (doTest3) 
		{
			System.out.println("***********************************************************");
			ObjectId fileId = new ObjectId("51f2c058c265e96a761a1972"); // (a "binary" share, type: zip)
			
			//3.1] construct
			String url = "inf://share/" + fileId.toString();
			InternalInfiniteFile test3 = new InternalInfiniteFile(url, auth);
			
			System.out.println("3.1: " + test3.toString());
			
			//3.2] list files
			InfiniteFile[] test3_2_files = test3.listFiles();
			System.out.println("3.2: LISTED="+test3_2_files.length);
			int i = 0;
			for (InfiniteFile test3_2_file: test3_2_files) {
				if (++i > 10) break;
				// 3.3.1] child file metadata
				System.out.println("3.2: test3_2_file=" + new Date(test3_2_file.getDate()) + " | " + test3_2_file.getName() + 
						" | " + test3_2_file.getUrlPath() + " | " + test3_2_file.getUrlString() + " | " + test3_2_file.getURI().toString());
				//3.3.2] child file contents
				System.out.println("3.2: test3_2_file_contents=");
				System.out.println("\t"+getContentsSample(test3_2_file.getInputStream()));
			}		
			
			//3.3] metadata
			System.out.println("3.3: test3_file=" + new Date(test3.getDate()) + " | " + test3.getName() + 
					" | " + test3.getUrlPath() + " | " + test3.getUrlString() + " | " + test3.getURI().toString());
			//3.4] contents
			try {
				System.out.println("3.4: test3_file_contents=");
				System.out.println("\t"+getContentsSample(test3.getInputStream()));
				throw new RuntimeException("Contents of directory should have failed");
			}
			catch (NullPointerException e) {
				System.out.println("3.4: passed: " + e.getMessage());
			}
		}
		
		//////////////////////////////////////////////////////
		// 
		// FILE TYPE 4: SMALL CUSTOM TABLE 
		if (doTest4)
		{
			System.out.println("***********************************************************");
			ObjectId fileId = new ObjectId("51cb5ccce4b0b640c1444270");
			
			//4.1] construct
			String url = "inf://custom/" + fileId.toString();
			InternalInfiniteFile test4 = new InternalInfiniteFile(url, auth);
			
			//4.2] list files
			InfiniteFile[] test4_2_files = test4.listFiles();
			System.out.println("4.2: LISTED="+test4_2_files.length);
			for (InfiniteFile test4_2_file: test4_2_files) {
				// 4.2.1] child file metadata
				System.out.println("4.2: test4_2_file=" + new Date(test4_2_file.getDate()) + " | " + test4_2_file.getName() + 
						" | " + test4_2_file.getUrlPath() + " | " + test4_2_file.getUrlString() + " | " + test4_2_file.getURI().toString());
				//4.2.2] child file contents
				System.out.println("4.2: test4_2_file_contents=");
				System.out.println("\t"+getContentsSample(test4_2_file.getInputStream()));
			}		
			
			//4.3] metadata
			System.out.println("4.3: test4_file=" + new Date(test4.getDate()) + " | " + test4.getName() + 
					" | " + test4.getUrlPath() + " | " + test4.getUrlString() + " | " + test4.getURI().toString());
			
			//4.4] contents
			try {
				System.out.println("4.4: test4_file_contents=");
				System.out.println("\t"+getContentsSample(test4.getInputStream()));
				throw new RuntimeException("Contents of directory should have failed");
			}
			catch (NullPointerException e) {
				System.out.println("4.4: passed: " + e.getMessage());
			}
		}
		
		//////////////////////////////////////////////////////
		// 
		// FILE TYPE 5: LARGE CUSTOM TABLE (INF-DEMO NOT INF-DEV - FAILS BECAUSE SHARDS ARE WRONG)
		if (doTest5)
		{
			System.out.println("***********************************************************");
			String jobName = "LinkedInRecommendationEngine_P1";
			
			//5.1] construct
			String url = "inf://custom/" + jobName.toString();
			InternalInfiniteFile test5 = new InternalInfiniteFile(url, auth);
			
			//5.2] list files
			InfiniteFile[] test5_2_files = test5.listFiles();
			int i = 0;
			System.out.println("5.2: LISTED="+test5_2_files.length);
			for (InfiniteFile test5_2_file: test5_2_files) {
				if (++i > 10) break;
				// 5.2.1] child file metadata
				System.out.println("5.2: test5_2_file=" + new Date(test5_2_file.getDate()) + " | " + test5_2_file.getName() + 
						" | " + test5_2_file.getUrlPath() + " | " + test5_2_file.getUrlString() + " | " + test5_2_file.getURI().toString());
				//5.2.2] child file contents - should fail
				System.out.println("5.2: test5_2_file_contents=");
				System.out.println("\t"+getContentsSample(test5_2_file.getInputStream()));
			}		
			
			//5.3] metadata
			System.out.println("5.3: test5_file=" + new Date(test5.getDate()) + " | " + test5.getName() + 
					" | " + test5.getUrlPath() + " | " + test5.getUrlString() + " | " + test5.getURI().toString());
			
			//5.5] contents
			try {
				System.out.println("5.4: test5_file_contents=");
				System.out.println("\t"+getContentsSample(test5.getInputStream()));
				throw new RuntimeException("Contents of directory should have failed");
			}
			catch (NullPointerException e) {
				System.out.println("5.4: passed: " + e.getMessage());
			}
		}
		
		//////////////////////////////////////////////////////
		// 
		// FILE TYPE 6: LARGE CUSTOM TABLE (INF-DEMO NOT INF-DEV - ACTUALLY WORKS)
		if (doTest6)
		{
			System.out.println("***********************************************************");
			String jobName = "LinkedInRecommendationEngine_P2";
			
			//6.1] construct
			String url = "inf://custom/" + jobName.toString();
			InternalInfiniteFile test6 = new InternalInfiniteFile(url, auth);
			
			//6.2] list files
			InfiniteFile[] test6_2_files = test6.listFiles();
			System.out.println("6.2: LISTED="+test6_2_files.length);
			for (InfiniteFile test6_2_file: test6_2_files) {
				if (null == test6_2_file) break;
				// 6.2.1] child file metadata
				System.out.println("6.2: test6_2_file=" + new Date(test6_2_file.getDate()) + " | " + test6_2_file.getName() + 
						" | " + test6_2_file.getUrlPath() + " | " + test6_2_file.getUrlString() + " | " + test6_2_file.getURI().toString());
				//6.2.2] child file contents - should fail
				try {
					System.out.println("6.2: test6_2_file_contents=");
					System.out.println("\t"+getContentsSample(test6_2_file.getInputStream()));
					throw new RuntimeException("Contents of directory should have failed");
				}
				catch (NullPointerException e) {
					System.out.println("6.2.1: passed: " + e.getMessage());
				}
				//6.2.3] list contents of sub-directories
				InfiniteFile[] test6_2_3_files = test6_2_file.listFiles();
				int i = 0;
				System.out.println("6.2.3: LISTED="+test6_2_3_files.length);
				for (InfiniteFile test6_2_3_file: test6_2_3_files) {
					if (++i > 10) break;
					
					System.out.println("6.2.3.1: test6_2_3_file=" + new Date(test6_2_3_file.getDate()) + " | " + test6_2_3_file.getName() + 
							" | " + test6_2_3_file.getUrlPath() + " | " + test6_2_3_file.getUrlString() + " | " + test6_2_3_file.getURI().toString());
					System.out.println("6.2.3.2: test6_2_3_file_contents=");
					System.out.println("\t"+getContentsSample(test6_2_3_file.getInputStream()));					
				}				
			}		
			
			//6.3] metadata
			System.out.println("6.3: test6_file=" + new Date(test6.getDate()) + " | " + test6.getName() + 
					" | " + test6.getUrlPath() + " | " + test6.getUrlString() + " | " + test6.getURI().toString());
			
			//6.6] contents
			try {
				System.out.println("6.4: test6_file_contents=");
				System.out.println("\t"+getContentsSample(test6.getInputStream()));
				throw new RuntimeException("Contents of directory should have failed");
			}
			catch (NullPointerException e) {
				System.out.println("6.4: passed: " + e.getMessage());
			}
		}
		
		//////////////////////////////////////////////////////
		// 
		// TEST 7: MISCELLANEOUS INITIALIZATION ERRORS
		
		if (doTest7)
		{
			// 7.1] share not found
			try {
				ObjectId fileId = new ObjectId("41eebba0e4b0dcb70c9ff2af"); // (a reference share)
				String url = "inf://share/" + fileId.toString();
				@SuppressWarnings("unused")
				InternalInfiniteFile test7 = new InternalInfiniteFile(url, auth);								
			}
			catch (MalformedURLException e) {
				System.out.println("7.1 passed: " + e.getMessage());
			}
			// 7.2] custom not found (_id)
			try {
				ObjectId fileId = new ObjectId("41eebba0e4b0dcb70c9ff2af"); // (a reference share)
				String url = "inf://custom/" + fileId.toString();
				@SuppressWarnings("unused")
				InternalInfiniteFile test7 = new InternalInfiniteFile(url, auth);								
			}
			catch (MalformedURLException e) {
				System.out.println("7.2 passed: " + e.getMessage());
			}
			// 7.3] custom not found (jobtitle)
			try {
				String url = "inf://custom/blahblah";
				@SuppressWarnings("unused")
				InternalInfiniteFile test7 = new InternalInfiniteFile(url, auth);								
			}
			catch (MalformedURLException e) {
				System.out.println("7.3 passed: " + e.getMessage());
			}
			// 7.4] not share or custom
			try {
				String url = "inf://cusXXXtom/blahblah";
				@SuppressWarnings("unused")
				InternalInfiniteFile test7 = new InternalInfiniteFile(url, auth);								
			}
			catch (MalformedURLException e) {
				System.out.println("7.4 passed: " + e.getMessage());
			}
		}
		// 7.5] Authorization failed:
		try {
			auth = new NtlmPasswordAuthentication("515dc962abe2e9427ad9af5a", "406dc16dfbf042893dd6b8f2", null);
			ObjectId fileId = new ObjectId("519f9a17e4b0ce42e187db82"); // (a json share, type: infinite-entity-alias)
			String url = "inf://share/" + fileId.toString();
			@SuppressWarnings("unused")
			InternalInfiniteFile test7 = new InternalInfiniteFile(url, auth);
		}
		catch (MalformedURLException e) {
			System.out.println("7.5 passed: " + e.getMessage());
		}
		// 7.6] Authorization by community, share
		{
			auth = new NtlmPasswordAuthentication("5150b079e4b08171961a2992", "406dc16dfbf042893dd6b8f2", null);
				//(invalid user, correct community)
			ObjectId fileId = new ObjectId("519f9a17e4b0ce42e187db82"); // (a json share, type: infinite-entity-alias)
			String url = "inf://share/" + fileId.toString();
			InternalInfiniteFile test7 = new InternalInfiniteFile(url, auth);
			System.out.println("7.6 passed: " + test7.getUrlString());
		}
		// 7.7] Authorization by community, custom
		{
			auth = new NtlmPasswordAuthentication("5154cd50e4b0e8a9798d3d03", "406dc16dfbf042893dd6b8f2", null);
				//(invalid user, correct community)
			ObjectId fileId = new ObjectId("51cb5ccce4b0b640c1444270"); // (custom object)
			String url = "inf://custom/" + fileId.toString();
			InternalInfiniteFile test7 = new InternalInfiniteFile(url, auth);
			System.out.println("7.7 passed: " + test7.getUrlString());
		}
		// 7.8] Can't rename or delete (custom)
		{
			auth = new NtlmPasswordAuthentication("5154cd50e4b0e8a9798d3d03", "406dc16dfbf042893dd6b8f2", null);
			//(invalid user, correct community)
			ObjectId fileId = new ObjectId("51cb5ccce4b0b640c1444270"); // (custom object)
			String url = "inf://custom/" + fileId.toString();
			InternalInfiniteFile test7 = new InternalInfiniteFile(url, auth);
			try {
				test7.delete();
				throw new RuntimeException("FAIL 7.8: delete should throw an exception");
			}
			catch (IOException e) {
				System.out.println("7.8 passed: " + e.getMessage());				
			}
			try {
				test7.rename(test7.getUrlString());
				throw new RuntimeException("FAIL 7.8: rename should throw an exception");
			}
			catch (IOException e) {
				System.out.println("7.8 passed: " + e.getMessage());				
			}
		}
		// 7.9] Can't rename or delete (share)
		{
			auth = new NtlmPasswordAuthentication("5150b079e4b08171961a2992", "406dc16dfbf042893dd6b8f2", null);
				//(invalid user, correct community)
			ObjectId fileId = new ObjectId("519f9a17e4b0ce42e187db82"); // (a json share, type: infinite-entity-alias)
			String url = "inf://share/" + fileId.toString();
			InternalInfiniteFile test7 = new InternalInfiniteFile(url, auth);
			try {
				test7.delete();
				throw new RuntimeException("FAIL 7.9: delete should throw an exception");
			}
			catch (IOException e) {
				System.out.println("7.9 passed: " + e.getMessage());				
			}
			try {
				test7.rename(test7.getUrlString());
				throw new RuntimeException("FAIL 7.9: rename should throw an exception");
			}
			catch (IOException e) {
				System.out.println("7.9 passed: " + e.getMessage());				
			}
		}
	}

	//(just returns first line)
	private static String getContentsSample(InputStream in) {
		Scanner s = new Scanner(in, "UTF-8");
		return (s.useDelimiter("\n").next());		
		//return (s.useDelimiter("\\A").next());		
	}
	
}
