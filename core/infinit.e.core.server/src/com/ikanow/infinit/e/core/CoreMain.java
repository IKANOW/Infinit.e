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
package com.ikanow.infinit.e.core;

import java.io.IOException;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.xml.DOMConfigurator;

import com.ikanow.infinit.e.core.execute_harvest.HarvestThenProcessController;
import com.ikanow.infinit.e.core.execute_synchronization.SynchronizationController;
import com.ikanow.infinit.e.core.utils.SourceUtils;
import com.ikanow.infinit.e.data_model.Globals;
import com.ikanow.infinit.e.data_model.index.ElasticSearchManager;
import com.ikanow.infinit.e.processing.custom.CustomProcessingController;

public class CoreMain {

	/**
	 * @param args
	 * @throws ParseException 
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws ParseException, IOException, InterruptedException {

		CommandLineParser cliParser = new BasicParser();
		Options allOps = new Options();
		// Common
		allOps.addOption("c", "config", true, "Configuration path");
		allOps.addOption("g", "community", true, "Only run on one community");
		// Harvest specific
		allOps.addOption("h", "harvest", false, "Run harvester");
		allOps.addOption("l", "local", false, "(for debug: use dummy index)");
		allOps.addOption("i", "source", true, "(for debug: use a single source)");
		allOps.addOption("r", "reset", false, "Reset bad sources");
		// Sync specific
		allOps.addOption("s", "sync", false, "Run synchronization");
		allOps.addOption("f", "from", true, "String unix time (secs) from when to sync");
		// Custom specifc
		allOps.addOption("p", "custom", false, "Run custom processing server");
		allOps.addOption("d", "dummy", true, "Use to keep temp unwanted options on the command line");
		allOps.addOption("j", "jobtitle", true, "(for debug: run a single job)");
		
		CommandLine cliOpts = cliParser.parse(allOps, args);
		
		Globals.setIdentity(com.ikanow.infinit.e.data_model.Globals.Identity.IDENTITY_SERVICE);
		
		if (cliOpts.hasOption("config")) {
			String configOverride = (String) cliOpts.getOptionValue("config");
			Globals.overrideConfigLocation(configOverride);
		}
    	//Set up logging
		java.io.File file = new java.io.File(com.ikanow.infinit.e.data_model.Globals.getLogPropertiesLocation() + ".xml");
		if (file.exists()) {
    		DOMConfigurator.configure(com.ikanow.infinit.e.data_model.Globals.getLogPropertiesLocation() + ".xml");
		}
		else {
    		PropertyConfigurator.configure(Globals.getLogPropertiesLocation());
		}
		
		if (cliOpts.hasOption("harvest")) {
			if (SourceUtils.checkDbSyncLock()) {
				Thread.sleep(10000); // (wait 10s and then try again)
				System.exit(0);
			}
			String communityOverride = null;
			String sourceDebug = null;
			if (cliOpts.hasOption("local")) {
				ElasticSearchManager.setLocalMode(true);				
			}
			if (cliOpts.hasOption("reset")) {
				SourceUtils.resetBadSources();
			}
			if (cliOpts.hasOption("community")) {
				communityOverride = (String) cliOpts.getOptionValue("community");
			}
			if (cliOpts.hasOption("source")) {
				sourceDebug = (String) cliOpts.getOptionValue("source");
			}
			new HarvestThenProcessController().startService(SourceUtils.getSourcesToWorkOn(communityOverride, sourceDebug, false, true));
		}//TESTED
		else if (cliOpts.hasOption("sync")) {
			if (SourceUtils.checkDbSyncLock()) {
				Thread.sleep(10000); // (wait 10s and then try again)
				System.exit(0);
			}
			// Sync command line options:
			long nTimeOfLastCleanse_secs = 0; // (default)
			if (cliOpts.hasOption("from")) {
				try {
					nTimeOfLastCleanse_secs = Long.parseLong((String) cliOpts.getOptionValue("from"));
				}
				catch (NumberFormatException e) {
					System.out.println("From date is incorrect");
					System.exit(-1);
				}
			}
			String communityOverride = null;
			String sourceDebug = null;
			if (cliOpts.hasOption("community")) {
				communityOverride = (String) cliOpts.getOptionValue("community");
			}
			else if (cliOpts.hasOption("source")) {
				sourceDebug = (String) cliOpts.getOptionValue("source");
			}
			SourceUtils.checkSourcesHaveHashes(communityOverride, sourceDebug);
				// (infrequently ie as part of sync, check all the sources have hashes, which the harvester depends on)
			
			new SynchronizationController().startService(nTimeOfLastCleanse_secs, SourceUtils.getSourcesToWorkOn(communityOverride, sourceDebug, true, true));
		}//TESTED
		else if (cliOpts.hasOption("custom")) 
		{
			String jobOverride = null;
			if (cliOpts.hasOption("jobtitle")) {
				jobOverride = (String) cliOpts.getOptionValue("jobtitle");
			}
			CustomProcessingController customPxController = new CustomProcessingController();
			customPxController.checkScheduledJobs(jobOverride);
			customPxController.checkRunningJobs();
		}
		else {
			//Test code for distribution:
//			boolean bSync = true;
//			LinkedList<SourcePojo> testSources = null;
//			LinkedList<SourcePojo> batchOfSources = null;
//			testSources = getSourcesToWorkOn(null, null, bSync, true);
//			System.out.println("Sources considered = " + testSources.size());
//			// Grab a batch of sources
//			batchOfSources = getDistributedSourceList(testSources, null, false);
//			System.out.println("Sources left = " + testSources.size());
//			System.out.println("Sources extracted = " + new com.google.gson.Gson().toJson(batchOfSources));
			
			System.out.println("com.ikanow.infinit.e.core.server [--config <config-dir>] [--harvest [<other options>]|--sync [<other options>]|--custom [<other options>]]");
			System.exit(-1);
		}		
	}
	
}
