package com.ikanow.infinit.e.api.knowledge.processing;

import java.util.Map;

import org.elasticsearch.script.ExecutableScript;
import org.elasticsearch.script.NativeScriptFactory;

public class QueryDecayFactory implements NativeScriptFactory 
{
	private final static String LANGUAGE = "native";
	private final static String SCRIPT_NAME = "decayscript";
	
	@Override
	public ExecutableScript newScript(Map<String, Object> arg0) 
	{		
		return new QueryDecayScript(arg0);
	}
	
	public static String getLanguage()
	{
		return LANGUAGE;
	}
	
	public static String getScriptName()
	{
		return SCRIPT_NAME;
	}

}
