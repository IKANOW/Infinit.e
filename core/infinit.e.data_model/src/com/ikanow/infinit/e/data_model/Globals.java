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
