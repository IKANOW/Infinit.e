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
package com.ikanow.infinit.e.data_model;

public class Globals 
{
	public enum  Identity { IDENTITY_NONE, IDENTITY_API, IDENTITY_SERVICE };
	
	private static String configLocation = "/opt/infinite-home/config";
	private static Identity identity = Identity.IDENTITY_NONE;  	
	
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
