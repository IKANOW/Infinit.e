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
import com.ikanow.infinit.e.data_model.store.MongoDbUtil;
import com.ikanow.infinit.e.data_model.utils.PropertiesManager;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;

public class LogstashConfigUtils {

	private static PropertiesManager _props = null;
	private static HashSet<String> _allowedInputs = new HashSet<String>();	
	private static HashSet<String> _allowedFilters = new HashSet<String>();	
	
	private static Pattern _validationRegexInputReplace = Pattern.compile("^([^a-z]*input[\\s\\n\\r]*\\{[\\s\\n\\r]*([a-z0-9_.-]+)[\\s\\n\\r]*\\{)", Pattern.CASE_INSENSITIVE);
	//private static Pattern _validationRegexOutput = Pattern.compile("[^a-z]output[\\s\\n\\r]*\\{", Pattern.CASE_INSENSITIVE); // (<-REPLACED BY JSONIFIED LOGIC, RETAINED UNLESS A NEED FOR IT COMES BACK)
	private static Pattern _validationRegexNoSourceKey = Pattern.compile("[^a-z0-9_]sourceKey[^a-z0-9_]", Pattern.CASE_INSENSITIVE);
	private static Pattern _validationRegexAppendFields = Pattern.compile("\\}[\\s\\n\\r]*\\}[^a-z{}\"']*(filter[\\s\\n\\r]*\\{)", Pattern.CASE_INSENSITIVE);

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
			errorMessage.append("Invalid input format, should be 'input { INPUT_TYPE { ... } }' (only one INPUT_TYPE) and also contain a filter, no \"s around them. (0)");
			return null;			
		}//TESTED (3_1d)
		else { // Check there's only one input type and (unless admin) it's one of the allowed types
			BasicDBObject inputDbo = (BasicDBObject)input;
			if (1 != inputDbo.size()) {
				errorMessage.append("Invalid input format, should be 'input { INPUT_TYPE { ... } }' (only one INPUT_TYPE) and also contain a filter, no \"s around them. (1)");
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
			errorMessage.append("Invalid input format, should be 'input { INPUT_TYPE { ... } }' (only one INPUT_TYPE) and also contain a filter, no \"s around them. (2)");
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
		m =  _validationRegexInputReplace.matcher(config);
		if (!m.find()) {
			errorMessage.append("Invalid input format, should be 'input { INPUT_TYPE { ... } }' (only one INPUT_TYPE) and also contain a filter, no \"s around them. (3)");
			return null;
		}//TESTED (see above)
		else { // If admin check on allowed types
			String inputType = m.group(2).toLowerCase();
			
			// If it's a file-based plugin then replace sincedb_path (check that it's not used during the JSON-ification):
			if (inputType.equalsIgnoreCase("file") || inputType.equalsIgnoreCase("s3")) {
				config = _validationRegexInputReplace.matcher(config).replaceFirst("$1\n      sincedb_path => \"_XXX_DOTSINCEDB_XXX_\"\n");
			}//TESTED

		}//TESTED
		
		m =  _validationRegexNoSourceKey.matcher(config);
			// (this won't help malicious changes to source key, but will let people know they're not supposed to)
		if (m.find()) {
			errorMessage.append("Not allowed to reference sourceKey - this is automatically appended by the logstash harvester");
			return null;
		}//TESTED		
		
		// OK now need to append the sourceKey at each stage of the pipeline to really really ensure that nobody sets sourceKey to be different 
		
		m = _validationRegexAppendFields.matcher(config);
		StringBuffer newConfig = new StringBuffer();
		if (m.find()) {
			m.appendReplacement(newConfig, "add_field => [ \"sourceKey\", \""+sourceKey+"\"] \n\n" + m.group() + " \n if [sourceKey] == \""+sourceKey+"\" { \n\n ");
		}
		else {
			errorMessage.append("Invalid input format, should be 'input { INPUT_TYPE { ... } }' (only one INPUT_TYPE) and also contain a filter, no \"s around them. (4)");
			return null;			
		}
		m.appendTail(newConfig);
		config = newConfig.toString();
		config = config.replaceAll("}[^}]*$", ""); // (remove the last })
		config += "\n\n mutate { update => [ \"sourceKey\", \""+sourceKey+"\"] } \n}\n}\n"; // double check the sourceKey hasn't been overwritten and close the if from above
		//TESTED (syntactically correct and does overwrite sourceKey everywhere - success_2_2)
		
		return config;
	}//TESTED
	
	/////////////////////////////////////////////////////////
	
	// The different cases here are:
	// 1) (?:[a-z0-9_]+)\\s*(=>)?\\s*([a-z0-9_]+)?\\s*\\{) 
	//		"input/filter {" - top level block
	//		"fieldname => type {" - (complex field, definitely allowed .. eg input.file.code)
	//		"fieldname => {" - (object field, not sure this is allowed)
	//		"fieldname {" - (object field, not sure this is allowed)
	// 2) (?:[a-z0-9_]+)\\s*=>\\s*[\\[a-z0-9])
	//		"fieldname => value" - sub-field or array of sub-fields
	// 3) (?:if\\s*[^{]+\\s*\\{)
	//		if statement
	// 4) }
	//		closing block
	public static Pattern _navigateLogstash = Pattern.compile("(?:([a-z0-9_]+)\\s*(=>)?\\s*([a-z0-9_]+)?\\s*\\{)|(?:([a-z0-9_]+)\\s*=>\\s*[\\[a-z0-9])|(?:if\\s*[^{]+\\s*\\{)|}", Pattern.CASE_INSENSITIVE);
	
	public static BasicDBObject parseLogstashConfig(String configFile, StringBuffer error) {
		
		BasicDBObject tree = new BasicDBObject();

		// Stage 0: remove escaped "s and 's (for the purpose of the validation):
		// (prevents tricksies with escaped "s and then #s)
		// (http://stackoverflow.com/questions/5082398/regex-to-replace-single-backslashes-excluding-those-followed-by-certain-chars)
		configFile = configFile.replaceAll("(?<!\\\\)(?:((\\\\\\\\)*)\\\\)[\"']", "X");
		//TESTED (by hand - using last 2 fields of success_2_1)
		
		// Stage 1: remove #s, and anything in quotes (for the purpose of the validation)
		configFile = configFile.replaceAll("(?m)(?:([\"'])(?:(?!\\1).)*\\1)", "VALUE").replaceAll("(?m)(?:#.*$)", "");
		//TESTED (2_1 - including with a # inside the ""s - Event_Date -> Event_#Date)
		//TESTED (2_2 - various combinations of "s nested inside 's) ... yes that is a negative lookahead up there - yikes!
		
		// Stage 2: get a nested list of objects
		int depth = 0;
		int ifdepth = -1;
		Stack<Integer> ifStack = new Stack<Integer>();
		BasicDBObject inputOrFilter = null;
		Matcher m = _navigateLogstash.matcher(configFile);
		// State:
		String currTopLevelBlockName = null;
		String currSecondLevelBlockName = null;
		BasicDBObject currSecondLevelBlock = null;
		while (m.find()) {
			boolean simpleField = false;

			//DEBUG
			//System.out.println("--DEPTH="+depth + " GROUP=" + m.group() + " IFS" + Arrays.toString(ifStack.toArray()));
			//System.out.println("STATES: " + currTopLevelBlockName + " AND " + currSecondLevelBlockName);
			
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
				if (null == typeName) { // it's an if statement or a string value
					typeName = m.group(4);
					if (null != typeName) {
						simpleField = true;
					}
				}		
				else if (typeName.equalsIgnoreCase("else")) { // It's an if statement..
					typeName = null;
				}
				if (null == typeName) { // if statement after all
					// Just keep track of ifs so we can ignore them
					ifStack.push(depth);
					ifdepth = depth;
					// (don't increment depth)
				}//TESTED (1_1bc, 2_1)
				else { // processing block
					String subTypeName = m.group(3);
					if (null != subTypeName) { // eg codec.multiline
						typeName = typeName + "." + subTypeName;
					}//TESTED (2_1, 2_3)
					
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
								
								// Store state:
								currTopLevelBlockName = topLevelType;
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
						
						// Some validation: can't include a type called "filter" anywhere
						if ((null != currTopLevelBlockName) && currTopLevelBlockName.equals("input")) {
							if (subElType.equals("filter") || subElType.endsWith(".filter")) {
								error.append("Not allowed sub-elements of input called 'filter' (1)");
								return null;
							}
						}//TESTED (1_5b)
						
						BasicDBList subElements = (BasicDBList) inputOrFilter.get(subElType);
						if (null == subElements) {
							subElements = new BasicDBList();
							inputOrFilter.put(subElType, subElements);
						}
						BasicDBObject newEl = new BasicDBObject();
						subElements.add(newEl);
						
						// Store state:
						currSecondLevelBlockName = subElType;
						currSecondLevelBlock = newEl;
					}//TESTED (*)
					else if (depth == 2) { // attributes of processing blocks
						// we'll just store the field names for these and do any simple validation that was too complicated for the regexes
						String subSubElType = typeName.toLowerCase();
						
						// Validation:
						if (null != currTopLevelBlockName) {
							// 1] sincedb path
							if (currTopLevelBlockName.equals("input") && (null != currSecondLevelBlockName)) {
								// (don't care what the second level block name is - no sincedb allowed)
								if (subSubElType.equalsIgnoreCase("sincedb_path")) {
									error.append("Not allowed sincedb_path in input.* block");
									return null;
								}//TESTED (1_5a)
								// 2] no sub-(-sub etc)-elements of input called filter
								if (subSubElType.equals("filter") || subSubElType.endsWith(".filter")) {
									error.append("Not allowed sub-elements of input called 'filter' (2)");
									return null;									
								}//TESTED (1_5c)
							}				
						}
						
						// Store in map:
						if (null != currSecondLevelBlock) {
							currSecondLevelBlock.put(subSubElType, new BasicDBObject());
						}
					}
					// (won't go any deeper than this)
					if (!simpleField) {
						depth++;
					}
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
		else if (!errors.toString().startsWith("{} Mismatch (})")) {
			System.out.println("**** FAIL " + testName + ": " + errors.toString());			
		}
		//b
		errors.setLength(0);
		testName = "error_1_1b";
		if (null != parseLogstashConfig(getTestFile(testName), errors)) {
			System.out.println("**** FAIL " + testName);
		}
		else if (!errors.toString().startsWith("{} Mismatch (})")) {
			System.out.println("**** FAIL " + testName + ": " + errors.toString());			
		}
		//c
		errors.setLength(0);
		testName = "error_1_1c";
		if (null != parseLogstashConfig(getTestFile(testName), errors)) {
			System.out.println("**** FAIL " + testName);
		}
		else if (!errors.toString().startsWith("{} Mismatch (})")) {
			System.out.println("**** FAIL " + testName + ": " + errors.toString());			
		}
		
		// 1.2) {} mismatch 2

		//a
		errors.setLength(0);
		testName = "error_1_2a";
		if (null != parseLogstashConfig(getTestFile(testName), errors)) {
			System.out.println("**** FAIL " + testName);
		}
		else if (!errors.toString().startsWith("{} Mismatch ({)")) {
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
		
		// 1.5) fields/sub-elements that are not permitted
		// a ... sincedb_path
		errors.setLength(0);
		testName = "error_1_5a";
		if (null != parseLogstashConfig(getTestFile(testName), errors)) {
			System.out.println("**** FAIL " + testName);
		}
		else if (!errors.toString().equals("Not allowed sincedb_path in input.* block")) {
			System.out.println("**** FAIL " + testName + ": " + errors.toString());			
		}
		// b ... filter as sub-path of input
		errors.setLength(0);
		testName = "error_1_5b";
		if (null != parseLogstashConfig(getTestFile(testName), errors)) {
			System.out.println("**** FAIL " + testName);
		}
		else if (!errors.toString().equals("Not allowed sub-elements of input called 'filter' (1)")) {
			System.out.println("**** FAIL " + testName + ": " + errors.toString());			
		}
		// c ... filter as sub-path of sub-element of input
		errors.setLength(0);
		testName = "error_1_5c";
		if (null != parseLogstashConfig(getTestFile(testName), errors)) {
			System.out.println("**** FAIL " + testName);
		}
		else if (!errors.toString().equals("Not allowed sub-elements of input called 'filter' (2)")) {
			System.out.println("**** FAIL " + testName + ": " + errors.toString());			
		}
		
		// 2) Valid formatted source
		BasicDBObject retVal;
		String output;
		String inputName; // (for re-using config files across text)
		//2.1)
		errors.setLength(0);
		testName = "success_2_1";
		if (null == (retVal = parseLogstashConfig(getTestFile(testName), errors))) {
			System.out.println("**** FAIL " + testName + ": " + errors.toString());			
		}
		else if (!retVal.toString().equals("{ \"input\" : { \"file\" : [ { \"path\" : { } , \"start_position\" : { } , \"type\" : { } , \"codec.multiline\" : { }}]} , \"filter\" : { \"csv\" : [ { \"columns\" : { }}] , \"drop\" : [ { }] , \"mutate\" : [ { \"convert\" : { }} , { \"add_fields\" : { }} , { \"rename\" : { }}] , \"date\" : [ { \"timezone\" : { } , \"match\" : { }}] , \"geoip\" : [ { \"source\" : { } , \"fields\" : { }}]}}")) {
			System.out.println("**** FAIL " + testName + ": " + retVal.toString());						
		}
		//System.out.println("(val="+retVal+")");
		
		// 2.2
		errors.setLength(0);
		testName = "success_2_2";
		if (null == (retVal = parseLogstashConfig(getTestFile(testName), errors))) {
			System.out.println("**** FAIL " + testName + ": " + errors.toString());			
		}
		if (null == MongoDbUtil.getProperty(retVal, "filter.geoip.fields")) {
			System.out.println("**** FAIL " + testName + ": " + retVal);						
		}
		//System.out.println(retVal);
		
		//2.3)	- check that the sincedb is added correctly, plus the sourceKey manipulation
		// (USE success_2_1 for this)
		errors.setLength(0);
		testName = "inputs_2_3";
		inputName = "success_2_3";		
		if (null == (output = validateLogstashInput(testName, getTestFile(inputName), errors, true))) {
			System.out.println("**** FAIL " + testName + ": errored: " + errors);			
		}
		else {
			String outputToTest = output.replaceAll("[\n\r]", "\\\\n").replaceAll("\\s+", " ");
			String testAgainst = "input {\n\n file {\n sincedb_path => \"_XXX_DOTSINCEDB_XXX_\"\n\n\n path => \"/root/odin-poc-data/proxy_logs/may_known_cnc.csv\"\n\n start_position => beginning\n\n type => \"proxy_logs\"\n\n codec => multiline {\n\n pattern => \"^%{YEAR}-%{MONTHNUM}-%{MONTHDAY}%{DATA:summary}\"\n\n negate => true\n\n what => \"previous\"\n\n } \n\n add_field => [ \"sourceKey\", \"inputs_2_3\"] \n\n}\n\n}\n\n\n\nfilter { \n if [sourceKey] == \"inputs_2_3\" { \n\n \n\n if [type] == \"proxy_logs\" {\n\n csv {\n\n columns => [\"Device_Name\",\"SimpleDate\",\"Event_#Date\",\"Source_IP\",\"Source_Port\",\"Destination_IP\",\"Destination_Port\",\"Protocol\",\"Vendor_Alert\",\"MSS_Action\",\"Logging_Device_IP\",\"Application\",\"Bytes_Received\",\"Bytes_Sent\",\"Dest._Country\",\"Message\",\"Message_Type\",\"MSS_Log_Source_IP\",\"MSS_Log_Source_Type\",\"MSS_Log_Source_UUID\",\"network_protocol_id\",\"OS_Type\",\"PIX_Main-Code\",\"PIX_Sub-Code\",\"Port\",\"Product_ID\",\"Product\",\"Rule\",\"Rule_Identifier\",\"Sensor_Name\",\"Class\",\"Translate_Destination_IP\",\"Translate_Destination_Port\",\"Translate_Source_IP\"]\n\n }\n\n if [Device_Name] == \"Device Name\" {\n\n drop {}\n\n }\n\n mutate {\n\n convert => [ \"Bytes_Received\", \"integer\" ]\n\n convert => [ \"Bytes_Sent\", \"integer\" ]\n\n }\n\n date {\n\n timezone => \"Europe/London\"\n\n match => [ \"Event_Date\" , \"yyyy-MM-dd'T'HH:mm:ss\" ]\n\n }\n\n geoip {\n\n source => \"Destination_IP\"\n\n fields => [\"timezone\",\"location\",\"latitude\",\"longitude\"]\n\n }\n\n }\n\n\n\n mutate { update => [ \"sourceKey\", \"inputs_2_3\"] } \n}\n}\n";
			testAgainst = testAgainst.replaceAll("[\n\r]", "\\\\n").replaceAll("\\s+", " ");
			if (!outputToTest.equals(testAgainst)) {
				System.out.println("**** FAIL " + testName + ": " + output);									
			}
		}

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
