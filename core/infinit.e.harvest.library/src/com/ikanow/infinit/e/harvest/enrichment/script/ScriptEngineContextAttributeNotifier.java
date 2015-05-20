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

import javax.script.ScriptContext;

import org.apache.log4j.Logger;

import com.ikanow.infinit.e.data_model.utils.IChangeListener;

/**
 * 
 * @author jfreydank
 *
 */
public class ScriptEngineContextAttributeNotifier implements IChangeListener {
	private static final Logger logger = Logger.getLogger(ScriptEngineContextAttributeNotifier.class);

	private CompiledScriptFactory factory;

	private String contextAttributeName;
	private int engineScope = ScriptContext.ENGINE_SCOPE;

	/**
	 * @return the engineScope
	 */
	public int getEngineScope() {
		return engineScope;
	}

	/**
	 * @param engineScope the engineScope to set
	 */
	public void setEngineScope(int engineScope) {
		this.engineScope = engineScope;
	}

	private Object contextAttributeValue;


	/**
	 *  This class sets a context attribuet to a certain value onChange.
	 * @param factory
	 * @param contextAttributeName 
	 * @param contextAttributeValue
	 */
	public ScriptEngineContextAttributeNotifier(CompiledScriptFactory factory, String contextAttributeName,Object contextAttributeValue){
		this.factory = factory;
		this.contextAttributeName = contextAttributeName;
		this.contextAttributeValue = contextAttributeValue;
	}
	
	@Override
	public void onChange(String where, Object what) {
		logger.debug("ScriptEngineContextAttributeNotifier setting "+contextAttributeName+"="+contextAttributeValue);
		if(factory!=null){
			factory.getScriptContext().setAttribute(contextAttributeName,contextAttributeValue,engineScope);
		}else{
			logger.warn("ScriptEngineContextAttributeNotifier was initialized with CompiledScriptfactory value null.");
		}
	}


}
