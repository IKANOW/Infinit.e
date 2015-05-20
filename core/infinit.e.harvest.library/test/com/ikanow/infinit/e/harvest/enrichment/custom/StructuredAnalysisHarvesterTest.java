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
package com.ikanow.infinit.e.harvest.enrichment.custom;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ikanow.infinit.e.data_model.store.config.source.SourcePipelinePojo;
import com.ikanow.infinit.e.harvest.DefaultHarvestContext;
import com.ikanow.infinit.e.harvest.HarvesterTest;


public class StructuredAnalysisHarvesterTest extends HarvesterTest {
	private static final Logger logger = Logger.getLogger(StructuredAnalysisHarvesterTest.class);
	protected StructuredAnalysisHarvester harvester = null;

	@BeforeClass
	public void setup(){
		super.setup();
		for (SourcePipelinePojo pipelineElement : source.getProcessingPipeline()) {
			if(pipelineElement.storageSettings!=null){
				onUpdateScript = pipelineElement.storageSettings.onUpdateScript;
				break;
			}
		}

		try{
			 harvester = new StructuredAnalysisHarvester();
			 harvester.intializeScriptEngine(compiledScriptFactory);
			 harvester.setContext(new DefaultHarvestContext());
			 harvester.intializeDocIfNeeded(doc);

		} catch (Exception e) {
			logger.error("Caught exception:",e);
			Assert.fail();
		}
	}	

	@Test
	public void testDocumentUpdate(){
		try {			
			
			harvester.handleDocumentUpdates(onUpdateScript, doc);
		} catch (Exception e) {
			logger.error("Caught exception:",e);
		}
	}


	public static void main(String[] args) {
		StructuredAnalysisHarvesterTest test = new StructuredAnalysisHarvesterTest();
		if(args.length>0){
			test.setConfig(args[0]);
		}
		test.setNo ="2";
		test.setup();
		test.testDocumentUpdate();
	}
}
