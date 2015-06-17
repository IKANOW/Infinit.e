/*******************************************************************************
 * Copyright 2015, The Infinit.e Open Source Project.
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
package com.ikanow.infinit.e.harvest.enrichment.script;

import org.apache.log4j.Logger;

import com.ikanow.infinit.e.data_model.utils.IChangeListener;

public class ScriptCallbackNotifier implements IChangeListener {
	private static final Logger logger = Logger.getLogger(ScriptCallbackNotifier.class);

	private CompiledScriptFactory factory;

	private String script;

	private String whereParamName;

	private String whatParamName;

	public ScriptCallbackNotifier(CompiledScriptFactory factory, String script, String whereParamName,String whatParamName){
		this.factory = factory;
		this.script = script;
		this.whereParamName = whereParamName;
		this.whatParamName = whatParamName;
	}
	
	@Override
	public void onChange(String where, Object what) {
		//logger.debug("onChange:"+where+":"+what);
		try {
			String whereValue = setterToAttribute(where);
			if(whereParamName!=null && whereValue!=null && whatParamName!=null){
				factory.executeCompiledScript(script,whereParamName,whereValue, whatParamName,what);
			}else if(whereParamName==null && whatParamName!=null){
				factory.executeCompiledScript(script, whatParamName,what);				
			}else if(whereParamName==null && whatParamName==null){
				factory.executeCompiledScript(script);				
			}
		} catch (Exception e) {
			logger.error("ScriptCallbackChangeListener caught an exception",e);
		}
	}

	/** 
	 * Where comes in as setXyz function name. 
	 * returns xyz - converted attribute name from setter. 
	 */
	private static String setterToAttribute(String where) {
		String whereAttribute= null;
		if(where!=null && where.startsWith("set")){
			return where.substring(3,4).toLowerCase()+where.substring(4);
		}
		return whereAttribute;
	}

}
