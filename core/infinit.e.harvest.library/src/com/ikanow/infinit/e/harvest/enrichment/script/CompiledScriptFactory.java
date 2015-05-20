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

import java.util.HashMap;
import java.util.Map;

import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.log4j.Logger;

import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.ikanow.infinit.e.data_model.utils.IkanowSecurityManager;
import com.ikanow.infinit.e.harvest.HarvestContext;
import com.ikanow.infinit.e.harvest.enrichment.custom.JavaScriptUtils;

public class CompiledScriptFactory {
	public static int GLOBAL = 0;
	protected static int COMBINED_PIPELINE = 1;
//	public static int UNSTRUCTURED_GLOBAL = 2;
	
	private static final Logger logger = Logger.getLogger(CompiledScriptFactory.class);
	private ScriptEngine engine = null;

	protected static String JAVASCRIPT = "javascript";
	public static int STANDALONE_TYPE=0;
	public static int SCRIPT_TYPE=1;
	public static int FUNC_TYPE=2;

	protected CompiledScriptAssembler compiledScriptAssembler = null;
	// map from hash(script) to function name
	private HashMap<Integer, String> pipelineGenericFunctions = new HashMap<Integer, String>();
	private HashMap<Integer, String> structuredAnalysisGenericFunctions = new HashMap<Integer, String>();
	private HashMap<Integer, String> unstructuredAnalysisGenericFunctions = new HashMap<Integer, String>();
	
	private IkanowSecurityManager securityManager;
	private ScriptContext scriptContext = null;
	private CompiledScript checkCacheScript = null;
	private HarvestContext context = null;
	
	/**
	 * @return the engine
	 */
	public ScriptEngine getEngine() {
		return engine;
	}



	public HashMap<Integer, String> getPipelineGenericFunctions() {
		return pipelineGenericFunctions;
	}


	public void setPipelineGenericFunctions(HashMap<Integer, String> pipelineGenericFunctions) {
		this.pipelineGenericFunctions = pipelineGenericFunctions;
	}


	public HashMap<Integer, String> getStructuredAnalysisGenericFunctions() {
		return structuredAnalysisGenericFunctions;
	}


	public void setStructuredAnalysisGenericFunctions(HashMap<Integer, String> structuredAnalysisGenericFunctions) {
		this.structuredAnalysisGenericFunctions = structuredAnalysisGenericFunctions;
	}


	public HashMap<Integer, String> getUnstructuredAnalysisGenericFunctions() {
		return unstructuredAnalysisGenericFunctions;
	}


	public void setUnstructuredAnalysisGenericFunctions(HashMap<Integer, String> unstructuredAnalysisGenericFunctions) {
		this.unstructuredAnalysisGenericFunctions = unstructuredAnalysisGenericFunctions;
	}
	
	
	public CompiledScriptFactory(SourcePojo source, HarvestContext context){		
		logger.debug("CompiledScriptFactoryContructor compiling source, id="+source.getId()+",key="+source.getKey());
		this.securityManager = context.getSecurityManager();
		this.context = context;
		ScriptEngineManager manager = new ScriptEngineManager();
		this.engine = manager.getEngineByName("JavaScript");	
		this.scriptContext = engine.getContext();
		this.compiledScriptAssembler =  new CompiledScriptAssembler(this, securityManager);
		this.compiledScriptAssembler.compileSourceScripts(source);
		// add a few scripts to be available
		addAndCompileScript(JavaScriptUtils.initOnUpdateScript,false);			
		addAndCompileScript(JavaScriptUtils.generateParsingScript(),false);		
		addAndCompileScript(JavaScriptUtils.s1FunctionScript,false);
		addAndCompileScript(JavaScriptUtils.cacheEmptyScript,false);
		addAndCompileScript(JavaScriptUtils.customEmptyScript,false);
		addAndCompileScript(JavaScriptUtils.putCacheFunction,false); 
		addAndCompileScript(JavaScriptUtils.putCustomFunction,false);
		addAndCompileScript(JavaScriptUtils.tmpCacheScript,false);
		addAndCompileScript(JavaScriptUtils.setDocumentAttributeScript,false);		
		addAndCompileScript(JavaScriptUtils.getIteratorOm2js(),false);			
		addAndCompileScript(JavaScriptUtils.dprint,false);
		addAndCompileScript(JavaScriptUtils.printState,false);
		
		this.checkCacheScript  = addAndCompileScript(JavaScriptUtils.getCheckDirtyDoc(),false);
		
		// TODO commment out
		//getScriptContext().setAttribute("_debug", true,ScriptContext.ENGINE_SCOPE);
	}
	

	protected HashMap<Integer,CompiledScriptEntry> compiledScriptMap = new HashMap<Integer,CompiledScriptEntry>();
	
	protected CompiledScript addAndCompileScript(String scriptlet,int key,boolean executeCacheCheckScript){
		CompiledScript cs = null;
		if(scriptlet!=null){
			CompiledScriptEntry ce = compiledScriptMap.get(key); 
			if(ce==null){
				int scriptType = STANDALONE_TYPE;
				StringBuffer script = new StringBuffer();
				if (scriptlet.toLowerCase().startsWith("$script") )
				{
					script.append(JavaScriptUtils.createDollarScriptFunctionAndCall(scriptlet));
					scriptType = SCRIPT_TYPE;
				}
				else  if(scriptlet.toLowerCase().startsWith("$func"))
				{
					script.append(JavaScriptUtils.getScript(scriptlet));
					scriptType = FUNC_TYPE;
				}
				else{
					script.append(scriptlet);
				}
				try {
					cs = securityManager.compile(engine, script.toString());
					compiledScriptMap.put(key, new CompiledScriptEntry(cs,scriptType,executeCacheCheckScript));
					logger.debug("Added compiled script,mapsize="+compiledScriptMap.size()+",key="+key+" ,script:\n"+script);
					// debug
					//if (scriptlet.toLowerCase().startsWith("$script") )
					//{
						//executeCompiledScript(scriptlet);
					//}
					
				} catch (ScriptException e) {
					this.context.getHarvestStatus().logMessage("Error compiling script:\n"+scriptlet, true);						
					logger.error("Error compiling script:\n"+scriptlet);			
				}
			}else{
				cs= ce.getCompiledScript();
				logger.debug("addAndCompileScript script (key="+key+") already exists, skipping compilation\n"+scriptlet);
			}
		} // if !=null
		return cs;
	}

	protected CompiledScript addAndCompileScript(String scriptlet,boolean addCacheCheckScript){
		CompiledScript cs = null;
		if(scriptlet!=null){
			cs = addAndCompileScript(scriptlet,scriptlet.hashCode(),addCacheCheckScript);
		}
		return cs;
	}

	protected String lookupFunctionName(Map<Integer,String> genericFunctionNames,String script) {
		// just pass in hash so we don't keep the scripts
		String functionName = genericFunctionNames.get(script.hashCode());
		if (functionName != null) {
			return functionName;
		} else {
			logger.warn("generic function name not found for script:" + script + " ");
			return null;
		}
	}

	public ScriptContext getScriptContext() {
		return scriptContext;
	}

	public Object executeCompiledScript(String scriptlet,Object... attributes) throws ScriptException{
		Object retVal = null;		
		try{
			for (int i = 0; i < 2*(attributes.length/2); i+=2) {
				String attrName  = (String)attributes[i];
				Object attrValue  = attributes[i+1];
				if(attrName!=null){
					getScriptContext().setAttribute(attrName,attrValue,ScriptContext.ENGINE_SCOPE);
				}
			}
			//logger.debug("Factory document:"+getScriptContext().getAttribute("_doc")+",metadata"+getScriptContext().getAttribute("_metadata")+" for script:"+scriptlet);
			if(scriptlet!=null){
				int key = scriptlet.hashCode();
				CompiledScriptEntry ce = compiledScriptMap.get(key);				
				if(ce!=null){
					// execute cache check script with security off, which maps document into script engine and uses internal java packages
					if(ce.isExecuteCacheCheckScript()){
						securityManager.setSecureFlag(false);				
						checkCacheScript.eval(scriptContext);						
					}
					securityManager.setSecureFlag(true);			
					int cetype = ce.getScriptType();
					if((cetype==STANDALONE_TYPE) || (cetype==FUNC_TYPE)){
						retVal = ce.getCompiledScript().eval(scriptContext);
					}else if(cetype==SCRIPT_TYPE){
						retVal = ce.getCompiledScript().eval(scriptContext);
					}
				} // ce!=null
				else{
					// TODO maybe allow lazy compilation
					logger.error("Script was not compiled ,mapsize="+compiledScriptMap.size()+",key="+key+" ,script:"+scriptlet);
				}
			}
		} catch (Exception e) {
			logger.error("executeCompiledScript caught exception for script:\n"+scriptlet,e);
			throw new ScriptException(e);
		}finally {
			securityManager.setSecureFlag(false);				
		}
		return retVal;
	}
	

	public Object executeCompiledScript(int key) throws ScriptException{
		Object retVal = null;
		try{
			securityManager.setSecureFlag(true);				
			CompiledScriptEntry ce = compiledScriptMap.get(key);
			if(ce!=null){
				int cetype = ce.getScriptType();
				if((cetype==STANDALONE_TYPE) || (cetype==FUNC_TYPE)){
					retVal = ce.getCompiledScript().eval(scriptContext);
				}else if(cetype==SCRIPT_TYPE){
					// call invocable
					Invocable invocable = (Invocable)ce.getCompiledScript().getEngine();					
					retVal = invocable.invokeFunction(JavaScriptUtils.genericFunctionCall);
				}
			} // ce!=null
		} catch (Exception e) {
			logger.error("executeCompiledScript caught exception:",e);
		}finally {
			securityManager.setSecureFlag(false);				
		}
		return retVal;
	}

	/**
	 *  Internal class use to speedup check if script has a $SCRIPT, func or standalone functionality 
	 * @author jfreydank
	 *
	 */
	  class CompiledScriptEntry{
		private CompiledScript compiledScript;
		private int scriptType = 0;
		private boolean executeCacheCheckScript = false;

		public CompiledScriptEntry(CompiledScript compiledScript,int scriptType,boolean executeCacheCheckScript){
			this.compiledScript= compiledScript;			
			this.scriptType = scriptType;
			this.executeCacheCheckScript = executeCacheCheckScript;
		}
		
		public CompiledScript getCompiledScript() {
			return compiledScript;
		}
		
		public int getScriptType() {
			return scriptType;
		}

		/**
		 * @return the executeCacheCheckScript
		 */
		public boolean isExecuteCacheCheckScript() {
			return executeCacheCheckScript;
		}	
	}

}
