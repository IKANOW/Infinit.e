/*******************************************************************************
 * Copyright 2012 The Infinit.e Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.ikanow.infinit.e.data_model;

public class Globals 
{
	public enum  Identity { IDENTITY_NONE, IDENTITY_API, IDENTITY_SERVICE };
	
	private static String configLocation = "/opt/infinite-home/config";
	private static Identity identity = Identity.IDENTITY_NONE;  	
	
	public static Identity getIdentity() {
		return identity;
	}
	public static void setIdentity(Identity id) {
		identity = id;
	}
	public static void overrideConfigLocation(String newConfigLocation) {
		assert identity != Identity.IDENTITY_NONE;
		configLocation = newConfigLocation;
	}
	public static String getLogPropertiesLocation() {
		assert identity != Identity.IDENTITY_NONE;
		if (Identity.IDENTITY_API == identity) {
			return configLocation + "/log4j.api.properties";
		}
		else if (Identity.IDENTITY_SERVICE == identity) {
			return configLocation + "/log4j.service.properties";
		}
		return null; //(unreachable due to assert)
	}
	public static String getAppPropertiesLocation() {
		assert identity != Identity.IDENTITY_NONE;
		if (Identity.IDENTITY_API == identity) {
			return configLocation + "/infinite.api.properties";
		}
		else if (Identity.IDENTITY_SERVICE == identity) {
			return configLocation + "/infinite.service.properties";
		}
		return null; //(unreachable due to assert)
	}	
	
	public static String getConfigLocation() {
		assert identity != Identity.IDENTITY_NONE;
		return configLocation;
	}
	//___________________________________________________________________________________
	
	// Utility function - parse stack exception
	
	public static void populateStackTrace(StringBuffer sb, Throwable t) {
		int n = 0;
		String lastMethodName = null;
		String lastClassName = null;
		String lastFileName = null;
		StackTraceElement firstEl = null;
		StackTraceElement lastEl = null;
		for (StackTraceElement el: t.getStackTrace()) {
			if (el.getClassName().contains("com.ikanow.") && (n < 20)) {
				if ((lastEl != null) && (lastEl != firstEl)) { // last non-ikanow element before the ikanow bit
					sb.append("[").append(lastEl.getFileName()).append(":").append(lastEl.getLineNumber()).append(":").append(lastEl.getClassName()).append(":").append(lastEl.getMethodName()).append("]");
					n += 2;				
					firstEl = null;
					lastEl = null;
				}//TESTED
				
				if (el.getClassName().equals(lastClassName) && el.getMethodName().equalsIgnoreCase(lastMethodName)) { // (overrides)
					sb.append("[").append(el.getLineNumber()).append("]");
					// (don't increment n in this case)
				}//(c/p of other clauses)
				else if (el.getClassName().equals(lastClassName)) { // different methods in the same class
					sb.append("[").append(el.getLineNumber()).append(":").append(el.getMethodName()).append("]");
					n++; // (allow more of these)
				}//TESTED
				else if (el.getFileName().equals(lastFileName)) { // different methods in the same class					
					sb.append("[").append(el.getLineNumber()).append(":").append(el.getClassName()).append(":").append(el.getMethodName()).append("]");
					n += 2;
				}//(c/p of other clauses)
				else {
					sb.append("[").append(el.getFileName()).append(":").append(el.getLineNumber()).append(":").append(el.getClassName()).append(":").append(el.getMethodName()).append("]");
					n += 3;
				}//TESTED
				lastMethodName = el.getMethodName();
				lastClassName = el.getClassName();
				lastFileName = el.getFileName();
			}
			else if (0 == n) {
				firstEl = el;
				sb.append("[").append(el.getFileName()).append(":").append(el.getLineNumber()).append(":").append(el.getClassName()).append(":").append(el.getMethodName()).append("]");
				n += 3;
			}//TESTED
			else if (null != firstEl) {
				lastEl = el;
			}
		}		
	}//TESTED
}
