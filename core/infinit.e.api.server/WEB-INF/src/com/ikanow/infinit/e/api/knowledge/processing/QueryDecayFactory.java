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
