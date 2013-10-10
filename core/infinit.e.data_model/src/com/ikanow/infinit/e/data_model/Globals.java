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
}
