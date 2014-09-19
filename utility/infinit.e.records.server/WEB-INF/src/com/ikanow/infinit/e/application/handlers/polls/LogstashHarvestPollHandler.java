/*******************************************************************************
 * Copyright 2012 The Infinit.e Open Source Project sponsored by IKANOW.
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
package com.ikanow.infinit.e.application.handlers.polls;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.bson.types.ObjectId;

import com.ikanow.infinit.e.application.utils.LogstashConfigUtils;
import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.config.source.SourceHarvestStatusPojo;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePipelinePojo;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojoSubstitutionDbMap;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePipelinePojo.LogstashExtractorPojo;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.ikanow.infinit.e.data_model.utils.PropertiesManager;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;

public class LogstashHarvestPollHandler implements PollHandler {

	public static String LOGSTASH_DIRECTORY = "/opt/logstash-infinite/";
	public static String LOGSTASH_WD = "/opt/logstash-infinite/logstash/";
	public static String LOGSTASH_CONFIG = "/opt/logstash-infinite/logstash.conf.d/";
	public static String LOGSTASH_CONFIG_DISTRIBUTED = "/opt/logstash-infinite/dist.logstash.conf.d/";
	public static String LOGSTASH_TEST_OUTPUT_TEMPLATE_LIVE = "/opt/logstash-infinite/templates/transient-record-output-template.conf";
	public static String LOGSTASH_TEST_OUTPUT_TEMPLATE_STASHED = "/opt/logstash-infinite/templates/stashed-record-output-template.conf";
	public static String LOGSTASH_RESTART_FILE = "/opt/logstash-infinite/RESTART_LOGSTASH";
	public static String LOGSTASH_CONFIG_EXTENSION = ".auto.conf";
	
	private String _testOutputTemplate_stashed = null;
	private String _testOutputTemplate_live = null;
	private String _clusterName = null;
	
	private PropertiesManager _props = null;
	
	@Override
	public void performPoll() {

		boolean isSlave = false;
		
		if (null == LOGSTASH_CONFIG) { // (static memory not yet initialized)
			try {
				Thread.sleep(1000); // (extend the sleep time a bit)
			}
			catch (Exception e) {}
			return;			
		}
		
		File logstashDirectory = new File(LOGSTASH_CONFIG);
		String logstashDirName = LOGSTASH_CONFIG;
		if (!logstashDirectory.isDirectory() || !logstashDirectory.canRead() || !logstashDirectory.canWrite())
		{
			logstashDirectory = new File(LOGSTASH_CONFIG_DISTRIBUTED);
			logstashDirName = LOGSTASH_CONFIG_DISTRIBUTED;
			isSlave = true;
			if (!logstashDirectory.isDirectory() || !logstashDirectory.canRead() || !logstashDirectory.canWrite())
			{				
				try {
					Thread.sleep(10000); // (extend the sleep time a bit)
				}
				catch (Exception e) {}
				return;
			}
		}
		
		if (null == _props) {
			_props = new PropertiesManager();
			_clusterName = _props.getElasticCluster();
		}
		if (null == _testOutputTemplate_stashed) {
			try {
				File testOutputTemplate = new File(LOGSTASH_TEST_OUTPUT_TEMPLATE_STASHED);
				InputStream inStream = null;
				try {
					inStream = new FileInputStream(testOutputTemplate);
					_testOutputTemplate_stashed = IOUtils.toString(inStream);
				}
				catch (Exception e) {// abandon ship!
					return;
				} 
				finally {
					inStream.close();
				}
			}
			catch (Exception e) {// abandon ship!
				//DEBUG
				//e.printStackTrace();
				return;
			} 
		}//TESTED
		if (null == _testOutputTemplate_live) {
			try {
				File testOutputTemplate = new File(LOGSTASH_TEST_OUTPUT_TEMPLATE_LIVE);
				InputStream inStream = null;
				try {
					inStream = new FileInputStream(testOutputTemplate);
					_testOutputTemplate_live = IOUtils.toString(inStream);
				}
				catch (Exception e) {// abandon ship!
					return;
				} 
				finally {
					inStream.close();
				}
			}
			catch (Exception e) {// abandon ship!
				//DEBUG
				//e.printStackTrace();
				return;
			} 
		}//TESTED
		
		
		// 0] Race condition protection: if we're still waiting for the last restart to happen then don't do anything else
		
		if (new File(LOGSTASH_RESTART_FILE).exists()) {
			//DEBUG
			//System.out.println("Waiting for last restart to occur");
			
			try {
				Thread.sleep(10000); // (extend the sleep time a bit)
			}
			catch (Exception e) {}
			return;			
		}//TESTED
		
		// 1] Get the time of the most recent change
		
		File[] files = logstashDirectory.listFiles();
		long directoryTime = -1L;
		for (File toCheck: files) {
			if (toCheck.getName().endsWith(LOGSTASH_CONFIG_EXTENSION)) {
				directoryTime = toCheck.lastModified();
				break; // Get the time of the most recent change
			}
		}//TESTED
		
		//DEBUG
		//if (-1L != directoryTime) System.out.println("LAST CHANGE = " + new Date(directoryTime));
		
		// 2] Check vs the sources
		
		boolean modifiedConfiguration = false;
		for (;;) { // This is an inefficient but safe way of beating source publish races
		
			// Logstash type, not suspended
			BasicDBObject srcQuery = new BasicDBObject(SourcePojo.extractType_, "Logstash");
			// (need both suspended and active sources)
			srcQuery.put(SourcePojo.isApproved_, true);
			srcQuery.put(SourcePojo.harvestBadSource_, false);
			
			DBCursor dbc = DbManager.getIngest().getSource().find(srcQuery);			
			List<SourcePojo> srcList = SourcePojo.listFromDb(dbc, SourcePojo.listType(), new SourcePojoSubstitutionDbMap());
			long mostRecentlyChangedSource = 0L;
			for (SourcePojo src: srcList) {
				// Some input checking:
				if (ignoreSource(src, isSlave)) {
					continue;
				}				
				if ((null != src.getModified()) && (src.getModified().getTime() > directoryTime)) {

					//DEBUG
					//System.out.println("MODIFIED SRC=" + src.getModified() + ": " + src.getTitle());
					
					if (src.getModified().getTime() > mostRecentlyChangedSource) {
						
						boolean modified = !isSuspended(src);
						if (!modified) { // check if corresponding file exists
							if (new File(logstashDirName + src.getId() + LOGSTASH_CONFIG_EXTENSION).exists()) {
								modified = true;
								//DEBUG
								//System.out.println("ACTIVE->SUSPENDED SRC=" + src.getModified() + ": " + src.getTitle());
							}
							//DEBUG
							//else System.out.println("(...MODIFIED SUSPENDED SRC=" + src.getModified() + ": " + src.getTitle());
						}//TESTED
						if (modified) {
							mostRecentlyChangedSource = src.getModified().getTime();
								// (don't break want the latest file time to set the file times)
						}
					}
				}
			}//(end loop over sources)			
			
			// 3] Handle modified source(s)
			
			if (0 == mostRecentlyChangedSource) {
				break;
			}
			else {
				// Delete the directory
				cleanseDirectory(logstashDirectory);

				for (SourcePojo src: srcList) {
					// Some input checking:
					if (ignoreSource(src, isSlave)) {
						continue;
					}				
					if (!isSuspended(src)) {
						createConfigFileFromSource(src, mostRecentlyChangedSource, logstashDirName);
					}
				}//TESTED
				
				modifiedConfiguration = true;
				
				// and now .... will recheck with this new time
				directoryTime = mostRecentlyChangedSource;
			}
		}//(end loop over source check)
		
		if (modifiedConfiguration) {
			try {
				new File(LOGSTASH_RESTART_FILE).createNewFile();
			}//TESTED
			catch (Exception e) {
				//DEBUG
				//e.printStackTrace();
				
			} // (should never happen)
		}//TESTED
	}

	////////////////////////////////////////////////////////////
	
	// UTILITY
	
	private boolean ignoreSource(SourcePojo src, boolean isSlave) {
		// Some input checking:
		if (null == src.getProcessingPipeline() || src.getProcessingPipeline().isEmpty()) {
			return true;
		}
		LogstashExtractorPojo logstashExtractor = src.getProcessingPipeline().iterator().next().logstash;
		if (null == logstashExtractor) {
			return true;
		}
		// If a slave, only carry on if this is a distributed logstash extractor 
		if (isSlave && ((null == logstashExtractor.distributed) || !logstashExtractor.distributed))  {
			return true;
		}		
		return false;
	}
	
	private void cleanseDirectory(File logstashDirectory) {
		File[] files = logstashDirectory.listFiles();
		for (File toCheck: files) {
			if (toCheck.getName().endsWith(LOGSTASH_CONFIG_EXTENSION)) {
				toCheck.delete();
			}
		}		
	}//TESTED
	
	
	private void createConfigFileFromSource(SourcePojo src, long fileTime, String logstashDirName) {
		// Ignore anything malformed:
		if (null == src.getProcessingPipeline() || src.getProcessingPipeline().isEmpty()) {
			setSourceError(src.getId(), "Internal logic error: no processing pipeline");
			return;
		}
		SourcePipelinePojo px = src.getProcessingPipeline().iterator().next();
		if ((null == px.logstash) || (null == px.logstash.config)) {
			setSourceError(src.getId(), "Internal logic error: no logstash block");
			return;
		}

		// Validate/tranform the configuration:
		StringBuffer errMessage = new StringBuffer();
		String logstashConfig = LogstashConfigUtils.validateLogstashInput(src.getKey(), px.logstash.config, errMessage, true);
		if (null == logstashConfig) { // Validation error...
			setSourceError(src.getId(), errMessage.toString());
			return;				
		}//TESTED
		
		logstashConfig = logstashConfig.replace("_XXX_DOTSINCEDB_XXX_", LOGSTASH_WD + ".sincedb_" + src.getId().toString());
		// Replacement for #LOGSTASH{host} - currently only replacement supported (+ #IKANOW{} in main code)
		try {
			logstashConfig = logstashConfig.replace("#LOGSTASH{host}", java.net.InetAddress.getLocalHost().getHostName());
		}
		catch (Exception e) {
			logstashConfig = logstashConfig.replace("#LOGSTASH{host}", "localhost.localdomain");
			
		}

		String output = null;
		if ((null != px.logstash.streaming) && !px.logstash.streaming) {
			output = this._testOutputTemplate_stashed;
		}
		else {
			output = this._testOutputTemplate_live;			
		}
		
		logstashConfig = logstashConfig + output.replace("_XXX_CLUSTER_XXX_", _clusterName).
													replace("_XXX_SOURCEKEY_XXX_", src.getKey()).
														replace("_XXX_COMMUNITY_XXX_", src.getCommunityIds().iterator().next().toString());
			
		File outFile = new File(logstashDirName + src.getId() + LOGSTASH_CONFIG_EXTENSION);

		try {
			FileOutputStream outStream = new FileOutputStream(outFile);
			IOUtils.write(logstashConfig, outStream);
			outFile.setLastModified(fileTime);
			
			setSourceSuccess(src.getId());
			
		}//TESTED
		catch (IOException e) {
			// Error out:
			setSourceError(src.getId(), outFile.getName() + ": " + e.getMessage());			
		}
	}
	
	///////////////////////////////////////////////
	
	// LOWER LEVEL UTILS: Update source status utilities
	
	protected void setSourceError(ObjectId sourceId, String errMessage) {
		BasicDBObject query = new BasicDBObject(SourcePojo._id_, sourceId);
		BasicDBObject update1 = new BasicDBObject(SourcePojo.searchCycle_secs_, -1L);
		update1.put(SourceHarvestStatusPojo.sourceQuery_harvest_message_, errMessage);
		update1.put(SourceHarvestStatusPojo.sourceQuery_harvest_status_, "error");
		update1.put(SourceHarvestStatusPojo.sourceQuery_harvested_, new Date());
		BasicDBObject update = new BasicDBObject(DbManager.set_, update1);
		DbManager.getIngest().getSource().update(query,  update);
	}//TESTED
	
	protected void setSourceSuccess(ObjectId sourceId) {
		BasicDBObject query = new BasicDBObject(SourcePojo._id_, sourceId);
		BasicDBObject update1 = new BasicDBObject();
		update1.put(SourceHarvestStatusPojo.sourceQuery_harvest_message_, "Added to logstash");
		update1.put(SourceHarvestStatusPojo.sourceQuery_harvested_, new Date());
		update1.put(SourceHarvestStatusPojo.sourceQuery_harvest_status_, "success");
		BasicDBObject update = new BasicDBObject(DbManager.set_, update1);
		DbManager.getIngest().getSource().update(query,  update);		
	}//TESTED

	protected static boolean isSuspended(SourcePojo src) {
		return (null != src.getSearchCycle_secs()) && (src.getSearchCycle_secs() < 0);
	}//TESTED
}


