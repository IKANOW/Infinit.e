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
package com.ikanow.infinit.e.harvest;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.infinit.e.harvest.HarvestController;
import com.ikanow.infinit.e.harvest.HarvesterTest;
import com.ikanow.infinit.e.script_visible.ScriptUtil;

public class HarvestControllerTest extends HarvesterTest {
	
	public HarvestControllerTest(){
		super();
		initializeCompiledScriptFactory=false;
		initializeDocuments=false;
	}
	
	private static final Logger logger = Logger.getLogger(HarvestControllerTest.class);
	private String dataSourcePath = null;
	private String[] sourceFilenames = {
			"source_assoc_entity_delete.json",
			//"source_basic_feed.json"
			//"source_follow_links_chain.json"
			//"source_iterateOver.json"// works only on server, no_extractor=salience
			//"source_legacy_sah_feature.json" 
			//"source_legacy_sah.json" // works only on server, no_extractor=salience
			//"source_legacy_textcleanser.json"
			//"source_splitter_ind_domain.json" //works only on server, not enough privileges for lookup table
			//"source_splitter_unstructured.json",
			//"source_tika_legacy.json",
			//"source_tika_pipeline.json",
			};
	
	/**
	 * @param dataSourcePath the dataSourcePath to set
	 */
	public void setDataSourcePath(String dataSourcePath) {
		this.dataSourcePath = dataSourcePath;
	}
	


	@Test
	public void testRun(){
		for (int i = 0; i < sourceFilenames.length; i++) {
			setDataSourcePath("/data/sources/"+sourceFilenames[i]);
			setup();
			harvestSource();
		}		
	}
	
	public void harvestSource(){
		try {			
			List<DocumentPojo> toAdd = new LinkedList<DocumentPojo>();
			List<DocumentPojo> toUpdate = new LinkedList<DocumentPojo>();
			List<DocumentPojo> toRemove = new LinkedList<DocumentPojo>();				
			HarvestController hc = new HarvestController();
			hc.harvestSource(source, toAdd, toUpdate, toRemove);
			for (DocumentPojo documentPojo : toAdd) {
				logger.debug("Document:");
				logger.debug(ScriptUtil.toJson(documentPojo));
			}
		} catch (Exception e) {
			logger.error("Caught exception for source "+dataSourcePath+":",e);
		}
	}




	/* (non-Javadoc)
	 * @see com.ikanow.infinit.e.harvest.enrichment.custom.HarvesterTest#getDataSourcePath()
	 */
	@Override
	protected String getDataSourcePath() {
		return dataSourcePath;
	}
	public static void main(String[] args) {
		HarvestControllerTest test = new HarvestControllerTest();
		if(args.length>0){
			test.setConfig(args[0]);
		}
		test.setup();
		test.testRun();
	}
	
	
}
