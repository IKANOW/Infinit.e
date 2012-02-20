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
	
			CommandLine cliOpts = cliParser.parse(allOps, args);
			
			//Set up common parameters
	
			String configOverride = null;
			String query = null;
			boolean bDelete = false;
			boolean bRebuildIndex = false;
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
			if ((0 == args.length) || ((null == query)&&(0 == nLimit))) {
				System.out.println("Usage: MongoIndexerMain --doc|--assoc|--entity [--rebuild] [--query <query>] [--config <path>] [--delete] [--skip <start record>] [--limit <max records>]");
				if (args.length > 0) {
					System.out.println("(Note you must either specify a limit or a query - the query can be {} to get all records)");
				}
				System.exit(-1);
			}
					
			// Invoke appropriate manager to perform processing
			
			if (cliOpts.hasOption("doc")) {
				MongoDocumentTxfer.main(configOverride, query, bDelete, bRebuildIndex, nSkip, nLimit);
			}
			else if (cliOpts.hasOption("assoc")||cliOpts.hasOption("association")) {
				MongoAssociationFeatureTxfer.main(configOverride, query, bDelete, bRebuildIndex, nSkip, nLimit);			
			}
			else if (cliOpts.hasOption("entity")) {
				MongoEntityFeatureTxfer.main(configOverride, query, bDelete, bRebuildIndex, nSkip, nLimit);			
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
