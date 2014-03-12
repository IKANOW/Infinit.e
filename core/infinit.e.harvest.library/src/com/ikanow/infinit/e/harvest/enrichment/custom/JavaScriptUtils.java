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
package com.ikanow.infinit.e.harvest.enrichment.custom;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import com.mongodb.BasicDBList;
//import com.mongodb.BasicDBObject; // (used directly in the javascript now)

/**
 * JavaScriptUtils
 * @author cvitter
 */
public class JavaScriptUtils 
{
	// initScript - used to pass document in to the script engine
	public static String initScript = "var _doc = eval('(' + document + ')'); \n";
	
	// initScript - used to pass document in to the script engine
	public static String initOnUpdateScript = "var _old_doc = eval('(' + old_document + ')'); \n";
	
	// scripts to pass entity and event json objects into javascript methods
	public static String iteratorDocScript = "var _iterator = eval('(' + _iterator + ')'); \n";
	
	// scripts to pass entity and event json objects into javascript methods
	public static String iteratorMetaScript = "var _metadata = eval('(' + _metadata + ')'); \n";
	
	// genericFunctionCall - all functions passed in via $SCRIPT get the following name
	public static String genericFunctionCall = "getValue";
	
	/**
	 * getScript
	 * Extracts JavaScript code from $SCRIPT() and wraps in getVal function
	 * @param script - $SCRIPT()
	 * @return String - function getVal()
	 */
	public static String getScript(String script)
	{
		// The start and stop index use to substring the script
		int start = script.indexOf("(");
		int end = script.lastIndexOf(")");
		
		try {
			if (script.toLowerCase().startsWith("$script"))
			{
				// Remove $SCRIPT() wrapper and then wrap script in 'function getValue() { xxxxxx }'
				return "function " + genericFunctionCall + "() { " + script.substring(start + 1, end) + " }";
			}
			else
			{
				// Simply remove $FUNC() wrapper
				return script.substring(start + 1, end);
			}
		}
		catch (Exception e) {
			throw new RuntimeException("Malformed script: " + script);
		}
	}
	
	
	/**
	 * containsScript
	 * Determines whether or not value passed in contains script or a function
	 * call via $SCRIPT or $FUNC
	 * @param s
	 * @return
	 */
	public static Boolean containsScript(String s)
	{
		if (s.toLowerCase().startsWith("$script") || s.toLowerCase().startsWith("$func"))
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	
	/**
	 * getJavaScriptFile
	 * Retrieve a JavaScript file located at the fileUrl
	 * @param fileUrl - http://somewhere.com/javascript.js
	 * @return
	 */
	//TODO (INF-1922): Note this bypasses security if prefaced with file:// ... 
	public static String getJavaScriptFile(String fileUrl)
	{
		StringBuffer javaScript = new StringBuffer();
		try
		{
			// Create java.net.URL from fileUrl if possible
			URL url = new URL(fileUrl);

			// Read the contents of the url into a BufferedReader
			BufferedReader r = new BufferedReader(new InputStreamReader(url.openStream()));

			// Read the contents of r line by line and append to our StringBuffer
			String s;
			while (null != (s = r.readLine())) {
				javaScript.append(s);
			}

			// Close our reader
			r.close();
		}
		catch (Exception e)
		{			
		}
		return javaScript.toString();
	}//TESTED

	// Generate a script to convert the native JS objects into something
	// we can parse (NativeObject and NativeArrays can't be handled at the "user level" annoyingly)

	public static String generateParsingScript() {
		StringBuffer sbSub1 = new StringBuffer();
		sbSub1.append("function s1(el) {").append('\n');
		sbSub1.append("if (el == null) {}").append('\n');
		sbSub1.append("else if (el instanceof Array) {").append('\n');
		sbSub1.append("s2(el, 1);").append('\n');
		sbSub1.append("}").append('\n');
		sbSub1.append("else if (typeof el == 'object') {").append('\n');
		sbSub1.append("outList.add(s3(el));").append('\n');
		sbSub1.append("}").append('\n');
		sbSub1.append("else {").append('\n');
		sbSub1.append("outList.add(el.toString());").append('\n');
		sbSub1.append("}").append('\n');
		sbSub1.append("}").append('\n');

		StringBuffer sbSub2 = new StringBuffer();
		sbSub2.append("function s2(el, master_list) {").append('\n');
		sbSub2.append(
				"var list = (1 == master_list)?outList:listFactory.clone();")
				.append('\n');
		sbSub2.append("for (var i = 0; i < el.length; ++i) {").append('\n');
		sbSub2.append("var subel = el[i];").append('\n');
		sbSub2.append("if (subel == null) {}").append('\n');
		sbSub2.append("else if (subel instanceof Array) {").append('\n');
		sbSub2.append("list.add(s2(subel, 0));").append('\n');
		sbSub2.append("}").append('\n');
		sbSub2.append("else if (typeof subel == 'object') {").append('\n');
		sbSub2.append("list.add(s3(subel));").append('\n');
		sbSub2.append("}").append('\n');
		sbSub2.append("else {").append('\n');
		sbSub2.append("list.add(subel.toString());").append('\n');
		sbSub2.append("}").append('\n');
		sbSub2.append("}").append('\n');
		sbSub2.append("return list; }").append('\n');

		StringBuffer sbSub3 = new StringBuffer();
		sbSub3.append("function s3(el) {").append('\n');
		sbSub3.append("el.constructor.toString();").append("\n"); // Will crash out if is too complex
		// Replaced with 2 lines following, so I can create objects with smaller initial capacity
		//sbSub3.append("var currObj = objFactory.clone();").append('\n');
		sbSub3.append("var len = 0; for (var prop in el) { len++; }").append('\n');
		sbSub3.append("var currObj = new com.mongodb.BasicDBObject(Math.ceil(1.3*len));").append('\n');
		sbSub3.append("for (var prop in el) {").append('\n');
		sbSub3.append("var subel = el[prop];").append('\n');
		sbSub3.append("if (subel == null) {}").append('\n');
		sbSub3.append("else if (subel instanceof Array) {").append('\n');
		sbSub3.append("currObj.put(prop, s2(subel, 0));").append('\n');
		sbSub3.append("}").append('\n');
		sbSub3.append("else if (typeof subel == 'object') {").append('\n');
		sbSub3.append("currObj.put(prop, s3(subel));").append('\n');
		sbSub3.append("}").append('\n');
		sbSub3.append("else {").append('\n');
		sbSub3.append("currObj.put(prop, subel.toString());").append('\n');
		sbSub3.append("}").append('\n');
		sbSub3.append("}").append('\n');
		sbSub3.append("return currObj; }").append('\n');

		StringBuffer sbMain = new StringBuffer();
		sbMain.append(sbSub1);
		sbMain.append(sbSub2);
		sbMain.append(sbSub3);

		return sbMain.toString();
	}// TESTED (including null values, converts to string)

	// Convert a native JS complex object into a JSON-like map
	
	public static BasicDBList parseNativeJsObject(Object returnVal, ScriptEngine engine) throws ScriptException
	{		
		try {
			engine.put("output", returnVal);

			// Use BasicDBObject directly so I can reduce memory usage by setting the initial capacity depending on the size of the JSON array
//			BasicDBObject objFactory = new BasicDBObject();
//			engine.put("objFactory", objFactory);
			BasicDBList listFactory = new BasicDBList();
			engine.put("listFactory", listFactory);
			BasicDBList outList = new BasicDBList();
			engine.put("outList", outList);
	
			engine.eval("s1(output);");

			return outList;
		}
		catch (Exception e) {
			throw new RuntimeException("1 Cannot parse return non-JSON object: " + 
										returnVal.getClass().toString() + ":" + returnVal.toString() + 
										"; if embedding JAVA, considering using eg \"X = '' + X\" to convert back to native JS strings.");
		}
	}
	
}
