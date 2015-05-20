package com.ikanow.infinit.e.harvest;

import javax.script.ScriptContext;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.junit.Assert;
import org.junit.BeforeClass;

import com.ikanow.infinit.e.data_model.Globals;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.ikanow.infinit.e.data_model.store.document.ChangeAwareDocumentWrapper;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.infinit.e.data_model.utils.IkanowSecurityManager;
import com.ikanow.infinit.e.harvest.enrichment.custom.JavaScriptUtils;
import com.ikanow.infinit.e.harvest.enrichment.script.CompiledScriptFactory;
import com.ikanow.infinit.e.harvest.enrichment.script.ScriptCallbackNotifier;
import com.ikanow.infinit.e.harvest.enrichment.script.ScriptEngineContextAttributeNotifier;
import com.mongodb.BasicDBObject;
import com.mongodb.util.JSON;


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
public class HarvesterTest {
	private static final Logger logger = Logger.getLogger(HarvesterTest.class);

	protected SourcePojo source = null;
	protected ChangeAwareDocumentWrapper doc = null;
	protected CompiledScriptFactory compiledScriptFactory = null;
	protected String setNo ="1"; // 1 is govtrack, 2 is twitter using iterateOver
	protected String onUpdateScript = null;
	protected String config = null;
	protected boolean initializeCompiledScriptFactory = true;
	protected boolean initializeDocuments = true;
	protected boolean initializeSources = true;
	@BeforeClass
	public void setup(){
	 	 
		try {
			loadDataSource(getDataSourcePath());

			String documentJson1 = IOUtils.toString(HarvesterTest.class.getResourceAsStream(getDocumentPath()));			
			DocumentPojo delegate = DocumentPojo.fromDb((BasicDBObject)JSON.parse(documentJson1), DocumentPojo.class);		
			delegate.getMetadata().put("stringtest", new Object[]{"s1","s2","s3"});
			if(initializeCompiledScriptFactory){
			HarvestContext context = new DefaultHarvestContext(){

				@Override
				public IkanowSecurityManager getSecurityManager() {
					// TODO Auto-generated method stub
					return new IkanowSecurityManager(true);
				}
				
			};
			
			compiledScriptFactory = new CompiledScriptFactory(source,context);
        	// COMPILED_SCRIPT initialization 
            compiledScriptFactory.executeCompiledScript(CompiledScriptFactory.GLOBAL);
			compiledScriptFactory.getScriptContext().setAttribute("_debug", true,ScriptContext.ENGINE_SCOPE);
			}
			if(initializeDocuments){
				doc = new ChangeAwareDocumentWrapper(delegate);
				ScriptCallbackNotifier attributeChangeListener = new ScriptCallbackNotifier(compiledScriptFactory,JavaScriptUtils.setDocumentAttributeScript,JavaScriptUtils.docAttributeName,JavaScriptUtils.docAttributeValue);
				doc.setAttributeChangeListener(attributeChangeListener);
				ScriptEngineContextAttributeNotifier dirtyChangeListener = new ScriptEngineContextAttributeNotifier(compiledScriptFactory,"_dirtyDoc",true);
				doc.setDirtyChangeListener(dirtyChangeListener);
			}

		} catch (Exception e) {
			logger.error("Caught exception:",e);
			Assert.fail();
		}		
	 			
	}
	
	public void setConfig(String config) {
		this.config = config;
		Globals.setIdentity(com.ikanow.infinit.e.data_model.Globals.Identity.IDENTITY_SERVICE);
		Globals.overrideConfigLocation(config);
		PropertyConfigurator.configure(Globals.getLogPropertiesLocation());
	}


	protected String getDataSourcePath(){
		return "/data/source"+setNo+".json";
	}

	protected String getDocumentPath(){
		return "/data/document"+setNo+".json";
	}
	
	protected void loadDataSource(String dataSourcePath) {
		try {
			if(dataSourcePath!=null && initializeSources){			
				String sourceJson1 = IOUtils.toString(HarvesterTest.class.getResourceAsStream(dataSourcePath));
				source = DocumentPojo.fromDb((BasicDBObject) JSON.parse(sourceJson1), SourcePojo.class);
				Assert.assertNotNull(source);
			}
		} catch (Exception e) {
			logger.error("Caught exception:", e);
			Assert.fail();
		}
	}
}
