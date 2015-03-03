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
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import org.bson.types.ObjectId;

import com.google.gson.GsonBuilder;
import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.config.source.SimpleTextCleanserPojo;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.ikanow.infinit.e.data_model.store.config.source.UnstructuredAnalysisConfigPojo;
import com.ikanow.infinit.e.data_model.store.config.source.UnstructuredAnalysisConfigPojo.Context;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.infinit.e.data_model.Globals;
import com.ikanow.infinit.e.data_model.Globals.Identity;
import com.ikanow.infinit.e.data_model.InfiniteEnums.ExtractorDailyLimitExceededException;
import com.ikanow.infinit.e.data_model.InfiniteEnums.ExtractorDocumentLevelException;
import com.ikanow.infinit.e.harvest.HarvestController;
import com.ikanow.infinit.e.harvest.extraction.text.externalscript.TextExtractorExternalScript;
import com.ikanow.infinit.e.harvest.extraction.text.externalscript.TextExtractorExternalScript.Options;
import com.ikanow.infinit.e.harvest.utils.ProxyManager;
import com.mongodb.BasicDBObject;

@SuppressWarnings("unused")
public class ExternalScriptTest {

	/**
	 * @param args
	 * @throws ExtractorDailyLimitExceededException
	 * @throws  ExtractorDocumentLevelException
	 */
	//Used for Testing
			public static void main(String argv[]) throws ExtractorDailyLimitExceededException, ExtractorDocumentLevelException 
			{

				if (argv.length > 0 && argv[0] != null)
				{
					Globals.setIdentity(com.ikanow.infinit.e.data_model.Globals.Identity.IDENTITY_API);
					SourcePojo testSource = new SourcePojo();

					Set<ObjectId> commIds = new HashSet<ObjectId>();
					commIds.add(new ObjectId("4c927585d591d31d7b37097a"));
					testSource.setCommunityIds(commIds);
					LinkedHashMap<String, String> extractorOptions = new LinkedHashMap<String, String>();

					DocumentPojo testDoc = new DocumentPojo();
					testDoc.setUrl("temp/url");
					testDoc.setTempSource(testSource);
					TextExtractorExternalScript test = new TextExtractorExternalScript();
					test.setAdminOverrdide(true);

					//Debug set to true for testing and script+args read in as args
					extractorOptions.put(Options.DEBUG, "true");
					extractorOptions.put(Options.STDERR, "true");
					extractorOptions.put(Options.ERRTOFULLTEXT, "true");
					//This value is too large, should be set to max value of 600000 (if not admin)
					//extractorOptions.put(Options.TIMEOUT, "600001");
					extractorOptions.put(Options.SCRIPT, argv[0]);

					//add arguments
					int arg_count = 1;
					while (arg_count < argv.length)
					{
						if (null != argv[(arg_count)])
						{
							extractorOptions.put("arg"+arg_count, argv[(arg_count)]);
							arg_count++;
						}
						else
							break;
					}


					testSource.setExtractorOptions(extractorOptions);

					long startTime = System.currentTimeMillis();
					test.extractText(testDoc);
					//Print Results
					System.out.println("\n========================== RESULTS ==========================");
					System.out.println(testDoc.toDb());
					System.out.println("FULL TEXT: " + testDoc.getFullText());
					System.out.println("****Script Completed in " + TimeUnit.MILLISECONDS.toSeconds(( System.currentTimeMillis() - startTime)) + " seconds****");
				}
				else
				{
					System.out.println("Missing Args");
				}
			} //tested
}