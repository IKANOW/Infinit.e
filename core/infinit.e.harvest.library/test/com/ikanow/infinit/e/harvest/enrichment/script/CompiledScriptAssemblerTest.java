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

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.script.ScriptContext;
import javax.script.ScriptException;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.junit.Assert;
import org.junit.Test;

import com.ikanow.infinit.e.data_model.Globals;
import com.ikanow.infinit.e.harvest.HarvesterTest;
import com.ikanow.infinit.e.harvest.enrichment.custom.JavaScriptUtils;
import com.ikanow.infinit.e.script_visible.ScriptUtil;

public class CompiledScriptAssemblerTest extends HarvesterTest {
	private static final Logger logger = Logger.getLogger(CompiledScriptAssemblerTest.class);


	public void setConfig(String config) {
		this.config = config;
		Globals.setIdentity(com.ikanow.infinit.e.data_model.Globals.Identity.IDENTITY_SERVICE);
		Globals.overrideConfigLocation(config);
		PropertyConfigurator.configure(Globals.getLogPropertiesLocation());
	}

	@Test
	public void testCompiledScriptFactory(){
		
		try {
			compiledScriptFactory.executeCompiledScript(JavaScriptUtils.getCheckDirtyDoc(),"_docPojo",doc);
		} catch (ScriptException e) {
			logger.error("testCompiledFactory caught an exception:",e);
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void testExtractFields(){		
		try {
			Map<String,Object> fields = ScriptUtil.extractFields(source);
			for (Iterator<Entry<String, Object>> it = fields.entrySet().iterator(); it.hasNext();) {
				Entry<String, Object> e = (Entry<String, Object>) it.next();
				logger.debug(e);				
			}
		} catch (Exception e) {
			logger.error("TestExtractFields Caught an exception:",e);
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testSetText(){		
		try {
			String scriptlet = "print('TEXT:'+text); var json = eval('('+text+')'); json; ";
			compiledScriptFactory.addAndCompileScript(scriptlet, true);
			String text = "{	\"meta\": {	\"limit\": 100,	\"offset\": 0,	\"total_count\": 490	}	}";
			compiledScriptFactory.getScriptContext().setAttribute("text", text, ScriptContext.ENGINE_SCOPE);
			compiledScriptFactory.getScriptContext().setAttribute("_docPojo",doc, ScriptContext.ENGINE_SCOPE);
			Object retVal = compiledScriptFactory.executeCompiledScript(scriptlet);
			Assert.assertNotNull(retVal);
			
		} catch (Exception e) {
			logger.error("TestExtractFields Caught an exception:",e);
			Assert.fail(e.getMessage());
		}
	}

	public static void main(String[] args) {
		if(args.length>1){
			CompiledScriptAssemblerTest test = new CompiledScriptAssemblerTest();	
			test.setup();
			test.setConfig(args[0]);
//			test.testExtractFields();
//			test.testCompiledScriptFactory();
			test.testSetText();
		}
	}


}
