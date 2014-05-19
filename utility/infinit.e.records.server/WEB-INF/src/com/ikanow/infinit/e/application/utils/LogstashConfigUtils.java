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

package com.ikanow.infinit.e.application.utils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;

import com.ikanow.infinit.e.data_model.Globals;
import com.ikanow.infinit.e.data_model.utils.PropertiesManager;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;

public class LogstashConfigUtils {

	private static PropertiesManager _props = null;
	private static HashSet<String> _allowedInputs = new HashSet<String>();	
	private static HashSet<String> _allowedFilters = new HashSet<String>();	
	
	private static Pattern _validationRegexInput = Pattern.compile  ("^[^a-z]*input[\\s\\n\\r]*\\{[\\s\\n\\r]*([a-z0-9_.-]+)[\\s\\n\\r]*\\{[^}]+\\}[\\s\\n\\r]*\\}[^a-z]*filter[\\s\\n\\r]*\\{", Pattern.CASE_INSENSITIVE);
	private static Pattern _validationRegexReplace = Pattern.compile("^([^a-z]*input[\\s\\n\\r]*\\{[\\s\\n\\r]*(?:[a-z0-9_.-]+)[\\s\\n\\r]*\\{)", Pattern.CASE_INSENSITIVE);
	//private static Pattern _validationRegexOutput = Pattern.compile("[^a-z]output[\\s\\n\\r]*\\{", Pattern.CASE_INSENSITIVE); // (<-REPLACED BY JSONIFIED LOGIC, RETAINED UNLESS A NEED FOR IT COMES BACK)
	private static Pattern _validationRegexNoSourceKey = Pattern.compile("[^a-z0-9_]sourceKey[^a-z0-9_]", Pattern.CASE_INSENSITIVE);
	private static Pattern _validationRegexAppendFields = Pattern.compile  ("^([^a-z]*input[\\s\\n\\r]*\\{[\\s\\n\\r]*[a-z0-9_.-]+[\\s\\n\\r]*\\{[^}]+)\\}[\\s\\n\\r]*\\}[^a-z]*(filter[\\s\\n\\r]*\\{)", Pattern.CASE_INSENSITIVE);

	//TODO (INF-2533): some of the not allowed types should be allowed but only with certain param settings (eg client not server - or even more sophisticated stuff)
	//eg would be good to allow elasticsearch but override indices

	
	public static String validateLogstashInput(String sourceKey, String config, StringBuffer errorMessage, boolean isAdmin) {
		
		if (null == _props) {
			_props = new PropertiesManager();
			String allowedInputs = _props.getProperty("harvest.logstash.allowed_inputs");

			if ((null == allowedInputs) || (allowedInputs.isEmpty())) {
				allowedInputs = "collectd,drupal_dblog,gelf,gemfire,imap,irc,lumberjack,s3,snmptrap,sqs,syslog,twitter,udp,xmpp,zenoss";
				// currently *not* allowed by default: elasticsearch,eventlog,exec,file,ganglia,generator,graphite,heroku,jmx,log4j,pipe,puppet_facter,rabbitmq,redit,relp,sqlite,stdin,stomp,tcp,unix,varnishlog,websocket,wmi,zeromq
			}
			_allowedInputs.addAll(Arrays.asList(allowedInputs.toLowerCase().split("\\s*,\\s*")));
			
			String allowedFilters = _props.getProperty("harvest.logstash.allowed_filters");
			if ((null == allowedFilters) || (allowedFilters.isEmpty())) {
				allowedFilters = "advisor,alter,anonymize,checksum,cidr,cipher,clone,collate,csv,date,dns,drop,elapsed,extractnumbers,fingerprint,geoip,gelfify,grep,grok,grokdiscovery,l18n,json,json_encode,kv,metaevent,metrics,multiline,mutate,noop,prune,punct,railsparallelrequest,range,sleep,split,sumnumbers,syslog_pri,throttle,translate,unique,urldecode,useragent,uuid,wms,wmts,xml";
				// currently *not* allowed by default: elasticsearch,ruby,zeromq
			}
			_allowedFilters.addAll(Arrays.asList(allowedFilters.toLowerCase().split("\\s*,\\s*")));
		}//TESTED (3_2a)
		
		// Configuration validation, phase 1
		
		errorMessage.append("Validation error:");
		BasicDBObject jsonifiedConfig = parseLogstashConfig(config, errorMessage);
		if (null == jsonifiedConfig) {
			return null;
		}
		errorMessage.setLength(0);
		
		// Configuration validation, phase 2 - very basic checks on the structure of the object

		Object input =  jsonifiedConfig.get("input");
		if ((null == input) || !(input instanceof BasicDBObject)) { // Does input exist?
			errorMessage.append("Invalid input format, should be 'input { INPUT_TYPE { ... } }' (only one INPUT_TYPE) and also contain a filter, no \"s around them.");
			return null;			
		}//TESTED (3_1d)
		else { // Check there's only one input type and (unless admin) it's one of the allowed types
			BasicDBObject inputDbo = (BasicDBObject)input;
			if (1 != inputDbo.size()) {
				errorMessage.append("Invalid input format, should be 'input { INPUT_TYPE { ... } }' (only one INPUT_TYPE) and also contain a filter, no \"s around them.");
				return null;
			}//TESTED
			if (!isAdmin) {
				for (String key: inputDbo.keySet()) {
					if (!_allowedInputs.contains(key.toLowerCase())) {
						errorMessage.append("Security error, non-admin not allowed input type " + key + ", allowed options: " + _allowedInputs.toString());
						return null;
					}//TESTED
				}
			}//TESTED (3_1abc)
		}
		Object filter =  jsonifiedConfig.get("filter");
		if ((null == filter) || !(filter instanceof BasicDBObject)) { // Does filter exist?
			errorMessage.append("Invalid input format, should be 'input { INPUT_TYPE { ... } }' (only one INPUT_TYPE) and also contain a filter, no \"s around them.");
			return null;			
		}//TESTED (3_2d)
		else { // Check there's only one input type and (unless admin) it's one of the allowed types
			if (!isAdmin) {
				BasicDBObject filterDbo = (BasicDBObject)filter;
				for (String key: filterDbo.keySet()) {
					if (!_allowedFilters.contains(key.toLowerCase())) {
						errorMessage.append("Security error, non-admin not allowed filter type " + key + ", allowed options: " + _allowedFilters.toString());
						return null;
					}//TESTED
				}
			}//TESTED (3_2abc)
		}		
		
		// Configuration validation, phase 3
		
		Matcher m =  null;
		m =  _validationRegexInput.matcher(config);
		if (!m.find()) {
			errorMessage.append("Invalid input format, should be 'input { INPUT_TYPE { ... } }' (only one INPUT_TYPE) and also contain a filter, no \"s around them.");
			return null;
		}//TESTED (see above)
		else { // If admin check on allowed types
			String inputType = m.group(1).toLowerCase();
			
			// If it's a file-based plugin then ensure sincedb is not used:
			if (inputType.equalsIgnoreCase("file") || inputType.equalsIgnoreCase("s3")) {
				if (m.group().contains("sincedb_path")) {
					errorMessage.append("Not allowed sincedb_path in file input block");
					return null;
				}
				config = _validationRegexReplace.matcher(config).replaceFirst("$1\n      sincedb_path => \"_XXX_DOTSINCEDB_XXX_\"\n");
			}//TESTED

		}//TESTED
		
		m =  _validationRegexNoSourceKey.matcher(config);
			// (this won't help malicious changes to source key, but will let people know they're not supposed to)
		if (m.find()) {
			errorMessage.append("Not allowed to reference sourceKey - this is automatically appended by the logstash harvester");
			return null;
		}//TESTED		
		
		// OK now need to append the sourceKey at each stage of the pipeline to really really ensure that nobody sets sourceKey to be different 
		
		config = _validationRegexAppendFields.matcher(config).replaceFirst("$1 add_field => [ \"sourceKey\", \""+sourceKey+"\"] \n\n } \n\n } \n\n  $2 \n if [sourceKey] == \""+sourceKey+"\" { \n\n ");
		config = config.replaceAll("}[^}]*$", ""); // (remove the last })
		config += "\n\n mutate { update => [ \"sourceKey\", \""+sourceKey+"\"] } \n}\n}\n"; // double check the sourceKey hasn't been overwritten and close the if from above
		//TESTED (syntactically correct and does overwrite sourceKey everywhere)
		
		return config;
	}//TESTED
	
	/////////////////////////////////////////////////////////
	
	public static Pattern _navigateLogstash = Pattern.compile("(?:([a-z0-9_]+)\\s*(=>)?\\s*\\{)|(?:if\\s*[^{]+\\s*\\{)|}", Pattern.CASE_INSENSITIVE);
	
	public static BasicDBObject parseLogstashConfig(String configFile, StringBuffer error) {
		
		BasicDBObject tree = new BasicDBObject();
		
		// Stage 1: remove #s, and anything in quotes
		configFile = configFile.replaceAll("(?m)(#.*$)|\"[^\"]*\"", "");
		
		// Stage 2: get a nested list of objects
		int depth = 0;
		int ifdepth = -1;
		Stack<Integer> ifStack = new Stack<Integer>();
		BasicDBObject inputOrFilter = null;
		Matcher m = _navigateLogstash.matcher(configFile);
		while (m.find()) {
			
			//DEBUG
			//System.out.println("--DEPTH="+depth + " GROUP=" + m.group() + " IFS" + Arrays.toString(ifStack.toArray()));
			
			if (m.group().equals("}")) {
				
				if (ifdepth == depth) { // closing an if statement
					ifStack.pop();
					if (ifStack.isEmpty()) {
						ifdepth = -1;
					}
					else {
						ifdepth = ifStack.peek();
					}
				}//TESTED (1_1bc, 2_1)
				else { // closing a processing block
					
					depth--;
					if (depth < 0) { // {} Mismatch
						error.append("{} Mismatch (})");
						return null;
					}//TESTED (1_1abc)
				}
			}
			else { // new attribute!
				String typeName = m.group(1);
				if (null == typeName) { // it's an if statement
					// Just keep track of ifs so we can ignore them
					ifStack.push(depth);
					ifdepth = depth;
					// (don't increment depth)
				}//TESTED (1_1bc, 2_1)
				else { // processing block
					
					if (depth == 0) { // has to be one of input/output/filter)
						String topLevelType = typeName.toLowerCase();
						if (topLevelType.equalsIgnoreCase("input") || topLevelType.equalsIgnoreCase("filter")) {
							if (tree.containsField(topLevelType)) {
								error.append("Multiple input or filter blocks: " + topLevelType);
								return null;
							}//TESTED (1_3ab)
							else {
								inputOrFilter = new BasicDBObject();
								tree.put(topLevelType, inputOrFilter);
							}//TESTED (*)
						}
						else {
							if (topLevelType.equalsIgnoreCase("output")) {
								error.append("Not allowed output blocks - these are appended automatically by the logstash harvester");
							}
							else {
								error.append("Unrecognized processing block: " + topLevelType);
							}
							return null;
						}//TESTED (1_4a)
					}
					else if (depth == 1) { // processing blocks
						String subElType = typeName.toLowerCase();
						BasicDBList subElements = (BasicDBList) inputOrFilter.get(subElType);
						if (null == subElements) {
							subElements = new BasicDBList();
							inputOrFilter.put(subElType, subElements);
						}
						subElements.add(new BasicDBObject());
					}//TESTED (*)
					else if (depth == 2) { // attributes of processing blocks
						//currently won't do anything with these - in the future could allow entries that had certain fields
					}
					// (won't go any deeper than this)
					depth++;
				}
				
			}
		}
		if (0 != depth) {
			error.append("{} Mismatch ({)");
			return null;
		}//TESTED (1_2a)
		
		return tree;
	}//TESTED (1.1-3,2.1)

	/////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////

	// TEST CODE
	
	public static void main(String[] args) throws IOException {

		System.out.println(Arrays.toString(args));
		Globals.setIdentity(com.ikanow.infinit.e.data_model.Globals.Identity.IDENTITY_API);
		Globals.overrideConfigLocation(args[0]);
		
		
		// 1) Errored sources - things that break the formatting
		StringBuffer errors = new StringBuffer();
		String testName;
		// 1.1) {} mismatch 1
		//a
		errors.setLength(0);		
		testName = "error_1_1a";
		if (null != parseLogstashConfig(getTestFile(testName), errors)) {
			System.out.println("**** FAIL " + testName);
		}
		else if (!errors.toString().equals("{} Mismatch (})")) {
			System.out.println("**** FAIL " + testName + ": " + errors.toString());			
		}
		//b
		errors.setLength(0);
		testName = "error_1_1b";
		if (null != parseLogstashConfig(getTestFile(testName), errors)) {
			System.out.println("**** FAIL " + testName);
		}
		else if (!errors.toString().equals("{} Mismatch (})")) {
			System.out.println("**** FAIL " + testName + ": " + errors.toString());			
		}
		//c
		errors.setLength(0);
		testName = "error_1_1c";
		if (null != parseLogstashConfig(getTestFile(testName), errors)) {
			System.out.println("**** FAIL " + testName);
		}
		else if (!errors.toString().equals("{} Mismatch (})")) {
			System.out.println("**** FAIL " + testName + ": " + errors.toString());			
		}
		
		// 1.2) {} mismatch 2

		//a
		errors.setLength(0);
		testName = "error_1_2a";
		if (null != parseLogstashConfig(getTestFile(testName), errors)) {
			System.out.println("**** FAIL " + testName);
		}
		else if (!errors.toString().equals("{} Mismatch ({)")) {
			System.out.println("**** FAIL " + testName + ": " + errors.toString());			
		}

		
		// 1.3) multiple input/filter blocks
		// 1.3a) input
		errors.setLength(0);
		testName = "error_1_3a";
		if (null != parseLogstashConfig(getTestFile(testName), errors)) {
			System.out.println("**** FAIL " + testName);
		}
		else if (!errors.toString().equals("Multiple input or filter blocks: input")) {
			System.out.println("**** FAIL " + testName + ": " + errors.toString());			
		}
		// 1.3b) filter
		errors.setLength(0);
		testName = "error_1_3b";
		if (null != parseLogstashConfig(getTestFile(testName), errors)) {
			System.out.println("**** FAIL " + testName);
		}
		else if (!errors.toString().equals("Multiple input or filter blocks: filter")) {
			System.out.println("**** FAIL " + testName + ": " + errors.toString());			
		}
		
		// 1.4) unrecognized blocks
		// a output - special case
		errors.setLength(0);
		testName = "error_1_4a";
		if (null != parseLogstashConfig(getTestFile(testName), errors)) {
			System.out.println("**** FAIL " + testName);
		}
		else if (!errors.toString().equals("Not allowed output blocks - these are appended automatically by the logstash harvester")) {
			System.out.println("**** FAIL " + testName + ": " + errors.toString());			
		}
		// b
		errors.setLength(0);
		testName = "error_1_4b";
		if (null != parseLogstashConfig(getTestFile(testName), errors)) {
			System.out.println("**** FAIL " + testName);
		}
		else if (!errors.toString().equals("Unrecognized processing block: something_random")) {
			System.out.println("**** FAIL " + testName + ": " + errors.toString());			
		}
		
		// 2) Valid formatted source
		BasicDBObject retVal;
		String output;
		String inputName; // (for re-using config files across text)
		//c
		errors.setLength(0);
		testName = "success_2_1";
		if (null == (retVal = parseLogstashConfig(getTestFile(testName), errors))) {
			System.out.println("**** FAIL " + testName + ": " + errors.toString());			
		}
		else if (!retVal.toString().equals("{ \"input\" : { \"file\" : [ { }]} , \"filter\" : { \"csv\" : [ { }] , \"drop\" : [ { }] , \"mutate\" : [ { }] , \"date\" : [ { }] , \"geoip\" : [ { }]}}")) {
			System.out.println("**** FAIL " + testName + ": " + retVal.toString());						
		}
		//System.out.println("(val="+retVal+")");
		
		// 3) Valid formatted source, access to restricted types
		
		// 3.1) input 
		// a) restricted - admin
		// (USE success_2_1 for this)
		errors.setLength(0);
		testName = "inputs_3_1a";
		inputName = "success_2_1";		
		if (null != (output = validateLogstashInput(testName, getTestFile(inputName), errors, false))) {
			System.out.println("**** FAIL " + testName + ": Should have errored: " + output);			
		}
		else if (!errors.toString().startsWith("Security error, non-admin not allowed input type file, allowed options: ")) {
			System.out.println("**** FAIL " + testName + ": " + errors.toString());				
		}
		
		// b) restricted - non admin
		// (USE success_2_1 for this)
		errors.setLength(0);
		testName = "inputs_3_1b";
		inputName = "success_2_1";		
		if (null == (output = validateLogstashInput(testName, getTestFile(inputName), errors, true))) {
			System.out.println("**** FAIL " + testName + ": " + errors.toString());				
		}
		
		// c) unrestricted - non admin
		errors.setLength(0);
		testName = "inputs_3_1c";
		inputName = "inputs_3_1c";		
		if (null == (output = validateLogstashInput(testName, getTestFile(inputName), errors, true))) {
			System.out.println("**** FAIL " + testName + ": " + errors.toString());				
		}
		//System.out.println("(val="+output+")");
		
		// d) no input at all
		errors.setLength(0);
		testName = "inputs_3_1d";
		inputName = "inputs_3_1d";		
		if (null != (output = validateLogstashInput(testName, getTestFile(inputName), errors, false))) {
			System.out.println("**** FAIL " + testName + ": Should have errored: " + output);			
		}
		else if (!errors.toString().startsWith("Invalid input format, should be 'input { INPUT_TYPE { ... } }' (only one INPUT_TYPE) and also contain a filter, no \"s around them.")) {
			System.out.println("**** FAIL " + testName + ": " + errors.toString());				
		}
		
		// 3.2) filter
		// a) restricted - admin
		errors.setLength(0);
		testName = "filters_3_2a";
		inputName = "filters_3_2a";		
		if (null != (output = validateLogstashInput(testName, getTestFile(inputName), errors, false))) {
			System.out.println("**** FAIL " + testName + ": Should have errored: " + output);			
		}
		else if (!errors.toString().startsWith("Security error, non-admin not allowed filter type elasticsearch, allowed options: ")) {
			System.out.println("**** FAIL " + testName + ": " + errors.toString());				
		}
		//System.out.println("(err="+errors.toString()+")");
		
		// b) restricted - non admin
		// (USE filters_3_2a for this)
		errors.setLength(0);
		testName = "filters_3_2a";
		inputName = "filters_3_2a";		
		if (null == (output = validateLogstashInput(testName, getTestFile(inputName), errors, true))) {
			System.out.println("**** FAIL " + testName + ": " + errors.toString());				
		}
		//System.out.println("(val="+output+")");
		
		// c) unrestricted - non admin
		// (implicitly tested via 3.1bc)
		
		// d) no filter at all
		errors.setLength(0);
		testName = "filters_3_2d";
		inputName = "filters_3_2d";		
		if (null != (output = validateLogstashInput(testName, getTestFile(inputName), errors, false))) {
			System.out.println("**** FAIL " + testName + ": Should have errored: " + output);			
		}
		else if (!errors.toString().startsWith("Invalid input format, should be 'input { INPUT_TYPE { ... } }' (only one INPUT_TYPE) and also contain a filter, no \"s around them.")) {
			System.out.println("**** FAIL " + testName + ": " + errors.toString());				
		}
		
	}
	private static String getTestFile(String name) throws IOException {
		File testFile = new File("./test_files/logstashtest_" + name + ".txt");
		return FileUtils.readFileToString(testFile);
	}
}
