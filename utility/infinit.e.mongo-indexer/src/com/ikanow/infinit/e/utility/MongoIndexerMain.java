/*******************************************************************************
 * Copyright 2012, The Infinit.e Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.ikanow.infinit.e.utility;

import java.io.IOException;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.mongodb.MongoException;

public class MongoIndexerMain {

	/**
	 * This is just a placeholder to invoke MongoDocumentTxfer, MongoEventFeatureTxfer, or MongoEntityFeatureTxfer
	 * @param args
	 * @throws ParseException 
	 * @throws MongoException 
	 * @throws NumberFormatException 
	 * @throws IOException 
	 */
	
	public static void main(String[] args) {

		try {
			CommandLineParser cliParser = new BasicParser();
			Options allOps = new Options();
			// Fork
			allOps.addOption("d", "doc", false, "Document transfer");
			allOps.addOption("n", "entity", false, "Entity feature transfer");
			allOps.addOption("a", "assoc", false, "Association feature transfer");
			allOps.addOption("A", "associations", false, "Association feature transfer");
			// Common
			allOps.addOption("q", "query", true, "MongoDB query to select records to transfer");
			allOps.addOption("D", "delete", false, "Delete the records in both MongoDB and Elasticsearch (instead of transferring them)");
			allOps.addOption("c", "config", true, "Override the default config path");
			allOps.addOption("l", "limit", true, "Caps the number of records to act upon");
			allOps.addOption("s", "skip", true, "The record at which to start (not in delete mode)");
			allOps.addOption("r", "rebuild", false, "Rebuild the index before transferring");
			allOps.addOption("v", "verify", false, "Verifies the document indexes all exist | resync the entity frequencies (INTERNAL ONLY)");
			allOps.addOption("f", "features", false, "Updates features present in the queried documents (--doc only; INTERNAL ONLY)");
			allOps.addOption("C", "chunks", true, "Loop over chunks, '--chunks all' for all chunks, '--chunks A,B,..' for specific chunks, '--chunks +A' for all chunks after A (currently -doc only)");
	
			CommandLine cliOpts = cliParser.parse(allOps, args);
			
			//Set up common parameters
	
			String configOverride = null;
			String query = null;
			String chunksDescription = null;
			boolean bDelete = false;
			boolean bRebuildIndex = false;
			boolean bVerifyIndex = false;
			boolean bUpdateFeatures = false;
			int nLimit = 0;
			int nSkip = 0;
			if (cliOpts.hasOption("config")) {
				configOverride = (String) cliOpts.getOptionValue("config");
			}
			if (cliOpts.hasOption("query")) {
				query = (String) cliOpts.getOptionValue("query");
			}
			if (cliOpts.hasOption("delete")) {
				bDelete = true;
			}
			if (cliOpts.hasOption("limit")) {
				try {
					nLimit = Integer.parseInt((String) cliOpts.getOptionValue("limit"));
				}
				catch (Exception e) {
					System.out.println("Error parsing --limit: " + (String) cliOpts.getOptionValue("limit"));
					query = null; // exit in if clause below
				}
			}
			if (cliOpts.hasOption("skip")) {
				try {
					nSkip = Integer.parseInt((String) cliOpts.getOptionValue("skip"));
				}
				catch (Exception e) {
					System.out.println("Error parsing --skip: " + (String) cliOpts.getOptionValue("skip"));
					query = null; // exit in if clause below
				}
			}
			if (cliOpts.hasOption("rebuild")) {
				bRebuildIndex = true;
			}
			if (cliOpts.hasOption("features")) {
				bUpdateFeatures = true;
			}
			if (cliOpts.hasOption("verify")) {
				if (cliOpts.hasOption("doc") && !bRebuildIndex) {
					bVerifyIndex = true; // (doc only)
				}
				else if (cliOpts.hasOption("entity")) {
					bVerifyIndex = true; // (ents only)					
				}
			}
			if (cliOpts.hasOption("chunks")) {
				if (bDelete) {
					System.out.println("Can't specify --chunks in conjunction with --delete");
					query = null; // exit in if clause below
				}
				else if ((nSkip != 0) || (nLimit != 0)) {
					System.out.println("Can't specify --chunks in conjunction with --skip or --limit");					
					query = null; // exit in if clause below
				}
				chunksDescription = (String) cliOpts.getOptionValue("chunks");
			}
			if ((0 == args.length) || ((null == query)&&(0 == nLimit)&&!bVerifyIndex&&(null==chunksDescription))) {
				System.out.println("Usage: MongoIndexerMain --doc|--assoc|--entity [--rebuild] [--verify] [--query <query>] [--chunks all|<chunklist>|+<chunk>] [--config <path>] [--delete] [--skip <start record>] [--limit <max records>]");
				if (args.length > 0) {
					System.out.println("(Note you must either specify a limit or a query or chunks - the query can be {} to get all records)");
				}
				System.exit(-1);
			}
					
			// Invoke appropriate manager to perform processing
			
			if (cliOpts.hasOption("doc")) {
				MongoDocumentTxfer.main(configOverride, query, bDelete, bRebuildIndex, bVerifyIndex, bUpdateFeatures, nSkip, nLimit, chunksDescription);
			}
			else if (cliOpts.hasOption("assoc")||cliOpts.hasOption("association")) {
				MongoAssociationFeatureTxfer.main(configOverride, query, bDelete, bRebuildIndex, nSkip, nLimit, chunksDescription);			
			}
			else if (cliOpts.hasOption("entity")) {
				if (bVerifyIndex) {
					String[] dbDotColl = query.split("\\.");
					MongoEntitySyncFreq.syncFreq(dbDotColl[0], dbDotColl[1], configOverride);
				}
				else {
					MongoEntityFeatureTxfer.main(configOverride, query, bDelete, bRebuildIndex, nSkip, nLimit, chunksDescription);
				}
			}
			else {
				System.out.println("Usage: MongoIndexerMain --doc|--assoc|--entity [--rebuild] [--query <query>] [--config <path>] [--delete] [--skip <start record>] [--limit <max records>]");
				System.exit(-1);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
}
