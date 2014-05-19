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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;
//import org.apache.commons.lang.ArrayUtils;




import com.ikanow.infinit.e.application.data_model.TestLogstashExtractorPojo;
import com.ikanow.infinit.e.application.utils.LogstashConfigUtils;
import com.ikanow.infinit.e.application.utils.MongoQueue;
import com.ikanow.infinit.e.data_model.store.DbManager;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

public class LogstashTestRequestPollHandler implements PollHandler {

	public static String LOGSTASH_DIRECTORY = "/opt/logstash-infinite/";
	public static String LOGSTASH_WD = "/opt/logstash-infinite/logstash/";
	public static String LOGSTASH_BINARY = "/opt/logstash-infinite/logstash/bin/logstash";
	public static String LOGSTASH_TEST_OUTPUT_TEMPLATE = "/opt/logstash-infinite/templates/test-output-template.conf";

	private MongoQueue _logHarvesterQ = null;
	private String _testOutputTemplate = null;

	@Override
	public void performPoll() {

		if (null == LOGSTASH_DIRECTORY) { // (static memory not yet initialized)
			try {
				Thread.sleep(1000); // (extend the sleep time a bit)
			}
			catch (Exception e) {}
			return;			
		}
		
		// 1] Check - does logstash exist on this server:

		File logstashBinary = new File(LOGSTASH_BINARY);
		if (!logstashBinary.canExecute()) {
			try {
				Thread.sleep(10000); // (extend the sleep time a bit)
			}
			catch (Exception e) {}
			return;
		}		

		// 2] (Unlike harvester, _don't_ grab an application token, you can run this on as many servers as you want)

		// 3] Setup

		if (null == _logHarvesterQ) {
			_logHarvesterQ = new MongoQueue(DbManager.getIngest().getLogHarvesterQ().getDB().getName(), DbManager.getIngest().getLogHarvesterQ().getName());
		}
		if (null == _testOutputTemplate) {
			try {
				File testOutputTemplate = new File(LOGSTASH_TEST_OUTPUT_TEMPLATE);
				InputStream inStream = null;
				try {
					inStream = new FileInputStream(testOutputTemplate);
					_testOutputTemplate = IOUtils.toString(inStream);
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

		// 4] Check if any new requests have been made:

		BasicDBObject queueQuery = new BasicDBObject("logstash", new BasicDBObject(DbManager.exists_, true));
		DBObject nextElement = _logHarvesterQ.pop(queueQuery);
		while ( nextElement != null )
		{
			//DEBUG
			//System.out.println("FOUND: " + nextElement.toString());

			TestLogstashExtractorPojo testInfo = TestLogstashExtractorPojo.fromDb(nextElement, TestLogstashExtractorPojo.class);
			if ((null == testInfo.maxDocs) || (null == testInfo.logstash.config) || (null == testInfo.isAdmin) || (null == testInfo.sourceKey))
			{
				TestLogstashExtractorPojo testErr = new TestLogstashExtractorPojo();
				testErr._id = testInfo._id;
				testErr.error = "Internal Logic Error. Missing one of: maxDocs, isAdmin, sourceKey, logstash.config";
				_logHarvesterQ.push(testErr.toDb());

				return;
			}//TESTED
			
			// Validate/tranform the configuration:
			StringBuffer errMessage = new StringBuffer();
			String logstashConfig = LogstashConfigUtils.validateLogstashInput(testInfo.sourceKey, testInfo.logstash.config, errMessage, testInfo.isAdmin);
			if (null == logstashConfig) { // Validation error...
				TestLogstashExtractorPojo testErr = new TestLogstashExtractorPojo();
				testErr._id = testInfo._id;
				testErr.error = "Validation error: " + errMessage.toString();
				_logHarvesterQ.push(testErr.toDb());

				return;				
			}//TOTEST
			
			String outputConf = _testOutputTemplate.replace("_XXX_COLLECTION_XXX_", testInfo._id.toString()); //TESTED
			String sinceDbPath = LOGSTASH_WD + ".sincedb_" + testInfo._id.toString();
			String conf = logstashConfig.replace("_XXX_DOTSINCEDB_XXX_", sinceDbPath) + outputConf.replace("_XXX_SOURCEKEY_XXX_", testInfo.sourceKey);

			boolean allWorked = false;
			Process logstashProcess = null;
			try {
				// 1] Create the process

				ArrayList<String> args = new ArrayList<String>(4);
				args.addAll(Arrays.asList(LOGSTASH_BINARY, "-e", conf));
				if (0 == testInfo.maxDocs) {
					args.add("-t"); // test mode, must faster
				}
				ProcessBuilder logstashProcessBuilder = new ProcessBuilder(args);
				logstashProcessBuilder = logstashProcessBuilder.directory(new File(LOGSTASH_WD)).redirectErrorStream(true);
				logstashProcessBuilder.environment().put("JAVA_OPTS", "");

				//DEBUG
				//System.out.println("STARTING: " + ArrayUtils.toString(logstashProcessBuilder.command().toArray()));


				// 2] Kick off the process
				logstashProcess = logstashProcessBuilder.start();
				StringWriter outputAndError = new StringWriter();
				OutputCollector outAndErrorStream = new OutputCollector(logstashProcess.getInputStream(), new PrintWriter(outputAndError));
				outAndErrorStream.start();
				final int toWait_s = 240;

				boolean exited = false;

				// 3] Check the output collection for records
				
				int errorVal = 0;
				long priorCount = -1L;
				long priorPriorCount = -1L;
				int timeWhenFirstLinesAppeared = 0;
				final int maxWaitAfterLoggingStart_s = 30;
				for (int i = 0; i < toWait_s; i += 5) {
					try {
						Thread.sleep(5000); 
					}
					catch (Exception e) {}

					long count = DbManager.getCollection("ingest", testInfo._id.toString()).count();
					
					// 3.1] Do we have all the records (or is the number staying static)
					
					//DEBUG
					//System.out.println("FOUND: " + count + " VS " + priorCount + " , " + priorPriorCount);
					
					if (((count >= testInfo.maxDocs) && (count > 0)) || // ie max records (must be > 0)
							((priorPriorCount == count) && (count > 0))) // ie have started harvesting and it's stayed the same for 10 seconds
					{
						allWorked = true;
						break;
					}//TESTED					
					
					// 3.2] Has the process exited unexpectedly?
					
					if ((outAndErrorStream.getLines() > 0) && (0 == timeWhenFirstLinesAppeared)) {
						timeWhenFirstLinesAppeared = i;

						//DEBUG
						//System.out.println("LOG LINES! " + i);
					}//TESTED
					if ((0 == count) && ((outAndErrorStream.getLines() - 3) > 2*testInfo.maxDocs) && 
							((i - timeWhenFirstLinesAppeared) > maxWaitAfterLoggingStart_s))
					{
						// no records, lots of lines (subtract 3 in case maxDocs is really low - and assume 1 warning for input/filter/output,
						// 30 seconds since lines started appearing
						// likely to be an error, stop and start printing out:

						//DEBUG
						//System.out.println("LOG LINES! " + i + " NUM = " + outAndErrorStream.getLines());
						break;
					}//TESTED
					
					try {
						errorVal = logstashProcess.exitValue();
						exited = true;

						//DEBUG
						//System.out.println("GOT EXIT VALUE: " + errorVal);
						break;

					}//TESTED
					catch (Exception e) {} // that's OK we're just still going is all...
					
					priorPriorCount = priorCount;
					priorCount = count;					
					
				} //(end loop while waiting for job to complete)				
				
				// 4] If the process is still running then kill it
				
				if (!exited) {
					//DEBUG
					//System.out.println("EXITED WITHOUT FINISHING");

					logstashProcess.destroy();
				}//TESTED

				// 5] Things to do when the job is done: (worked or not)
				//    Send a message to the harvester
				
				outAndErrorStream.join(); // (if we're here then must have closed the process, wait for it to die)
				
				TestLogstashExtractorPojo testErr = new TestLogstashExtractorPojo();
				testErr._id = testInfo._id;
				if ((testInfo.maxDocs > 0) || (0 != errorVal)) {
					testErr.error = outputAndError.toString();
				}
				else { // maxDocs==0 (ie pre-publish test) AND no error returned
					testErr.error = null;
				}
				_logHarvesterQ.push(testErr.toDb());
				//TESTED				
			}
			catch (Exception e) {
				//DEBUG
				//e.printStackTrace();				

				TestLogstashExtractorPojo testErr = new TestLogstashExtractorPojo();
				testErr._id = testInfo._id;
				testErr.error = "Internal Logic Error: " + e.getMessage();
				_logHarvesterQ.push(testErr.toDb());

			}//TOTEST
			finally {
				// If we created a sincedb path then remove it:
				try {
					new File(sinceDbPath).delete();
				}
				catch (Exception e) {} // (don't care if it fails)
				
				if (!allWorked) { // (otherwise up to the harvester to remove these)
					try {
						DbManager.getCollection("ingest", testInfo._id.toString()).drop();
					}
					catch (Exception e) {} // doesn't matter if this errors
				}
				try {
					// Really really want to make sure the process isn't running
					if (null != logstashProcess) {
						logstashProcess.destroy();
					}
				}
				catch (Exception e) {}
				catch (Error ee) {}
			}//TESTED

			// (If we actually processed an element, then try again immediate)
			nextElement = _logHarvesterQ.pop(queueQuery);
		}
	}

	public static class OutputCollector extends Thread {
		private InputStream _in;
		private PrintWriter _out;
		private int _lines = 0;
		OutputCollector(InputStream in, PrintWriter out) {
			_in = in;
			_out = out;
		}		
		public int getLines() {
			return _lines;
		}
		
		@Override
		public void run() {
			BufferedReader br = null;
			try {
				br = new BufferedReader(new InputStreamReader(_in));
				String line = null;
				while (null != (line = br.readLine())) {
					_out.println(line);
					_lines++;

					//DEBUG
					//System.out.println(line);
				}
			}
			catch (Exception e) {
				//DEBUG
				//e.printStackTrace();
			} 
			finally {
				try {
					br.close();
				}
				catch (IOException e) {
					//DEBUG
					//e.printStackTrace();
				}
			}
		}
	}//TESTED
}
