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
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;

import com.ikanow.infinit.e.data_model.store.config.source.SourcePipelinePojo;
import com.ikanow.infinit.e.data_model.store.config.source.StructuredAnalysisConfigPojo.AssociationSpecPojo;
import com.ikanow.infinit.e.data_model.store.config.source.StructuredAnalysisConfigPojo.EntitySpecPojo;
import com.ikanow.infinit.e.data_model.store.document.AssociationPojo;


public class EntitiesHarvesterTest extends StructuredAnalysisHarvesterTest {
	private static final Logger logger = Logger.getLogger(EntitiesHarvesterTest.class);
	protected List<EntitySpecPojo> esps = null;
	protected List<AssociationSpecPojo> asps = null;

	@BeforeClass
	public void setup(){
		super.setup();
		esps = new ArrayList<EntitySpecPojo>();
		for (SourcePipelinePojo pipelineElement : source.getProcessingPipeline()) {
			if(pipelineElement.entities!=null){
				esps = pipelineElement.entities;					
			}
			else if(pipelineElement.associations!=null){
				asps = pipelineElement.associations;					
			}
			else if(pipelineElement.storageSettings!=null){
				onUpdateScript = pipelineElement.storageSettings.onUpdateScript;					
			}
		}
		EntitySpecPojo esp1 = new EntitySpecPojo();
		esp1.setDisambiguated_name("stringtest_dname");
		esp1.setIterateOver("stringtest");
		esp1.setType("default");
		esp1.setUseDocGeo(false);
		esps.add(esp1);

	}
	
	public void testEntitiesAndAssociations(){
		try {			
			harvester.setEntities(doc, esps);
			Assert.assertEquals(doc.getEntities().size(),12);
			logger.debug("entity.length="+doc.getEntities().size());
			harvester.setAssociations(doc, asps);
			logger.debug("association.length="+doc.getAssociations().size());
			Assert.assertEquals(doc.getAssociations().size(),7);
			for (AssociationPojo association : doc.getAssociations()) {
				logger.debug("A:"+association.getEntity1_index()+"_"+association.getEntity2_index());
			}
		} catch (Exception e) {
			logger.error("Caught exception:",e);
		}
	}


	public static void main(String[] args) {
		EntitiesHarvesterTest test = new EntitiesHarvesterTest();
		test.setNo ="2";
		test.setup();
		test.testEntitiesAndAssociations();
	}
}
