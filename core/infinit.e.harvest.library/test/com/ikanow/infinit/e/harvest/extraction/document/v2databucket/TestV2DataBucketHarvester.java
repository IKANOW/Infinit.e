package com.ikanow.infinit.e.harvest.extraction.document.v2databucket;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.log4j.PropertyConfigurator;
import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ikanow.infinit.e.data_model.Globals;
import com.ikanow.infinit.e.data_model.InfiniteEnums;
import com.ikanow.infinit.e.data_model.InfiniteEnums.HarvestEnum;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePipelinePojo;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.infinit.e.data_model.utils.IkanowSecurityManager;
import com.ikanow.infinit.e.harvest.HarvestContext;
import com.ikanow.infinit.e.harvest.extraction.document.DuplicateManager;
import com.ikanow.infinit.e.harvest.extraction.document.HarvestStatus;
import com.ikanow.infinit.e.harvest.extraction.document.HarvestStatus_Standalone;

public class TestV2DataBucketHarvester
{

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	public void setConfig(String config) {
		Globals.setIdentity(com.ikanow.infinit.e.data_model.Globals.Identity.IDENTITY_API);
		Globals.overrideConfigLocation(config);
		PropertyConfigurator.configure(Globals.getLogPropertiesLocation());
	}
	
	@Before
	public void before() {
		setConfig("C:\\Users\\Burch\\git\\ikanow_infinit.e_private\\utility_internal\\utility.dev.ikanow.config\\core_config");
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testCanHarvestType() {
		final V2DataBucketHarvester harvester = new V2DataBucketHarvester();
		assertTrue(harvester.canHarvestType(InfiniteEnums.V2DATABUCKET));
		assertFalse(harvester.canHarvestType(InfiniteEnums.LOGSTASH));
	}
	
	@Test
	public void testExecuteHarvest() {
		final V2DataBucketHarvester harvester = new V2DataBucketHarvester();	
		SourcePojo source = getTestSource();
		List<DocumentPojo> toAdd = new ArrayList<DocumentPojo>();
		HarvestContext test_context = new TestHarvestContext();
		harvester.executeHarvest(test_context, source, toAdd, null, null);
		//TODO check harvest Context
		toAdd.stream().forEach(doc -> {
			System.out.println(doc);
		});
	}
	
	private SourcePojo getTestSource() {
		SourcePojo source = new SourcePojo();
		List<SourcePipelinePojo> procPipe = new ArrayList<SourcePipelinePojo>();
		SourcePipelinePojo pipe = new SourcePipelinePojo();
		LinkedHashMap<String, Object> data_bucket = new LinkedHashMap<String, Object>();
		data_bucket.put("full_name", "/my/test/path");		
		LinkedHashMap<String, Object> data_schema = new LinkedHashMap<String, Object>();
		LinkedHashMap<String, Object> search_index_schema = new LinkedHashMap<String, Object>();
		search_index_schema.put("enabled", true);
		data_schema.put("search_index_schema", search_index_schema);
		data_bucket.put("data_schema", data_schema);
		pipe.data_bucket = data_bucket;
		
		procPipe.add(pipe);
		source.setProcessingPipeline(procPipe);
		source.setCreated(new Date());
		source.setModified(new Date());
		source.setOwnerId(new ObjectId());
		source.setKey("myfakekey");
		return source;
	}
	
	protected class TestHarvestContext implements HarvestContext {
		//private HarvestStatus harvest_status = new TestHarvestStatus();
		private HarvestStatus harvest_status = new HarvestStatus_Standalone();
		@Override
		public DuplicateManager getDuplicateManager() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public HarvestStatus getHarvestStatus() {
			return harvest_status;
		}

		@Override
		public boolean isStandalone() {
			return true;
		}

		@Override
		public int getStandaloneMaxDocs() {
			return 10;
		}

		@Override
		public IkanowSecurityManager getSecurityManager() {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
	
	protected class TestHarvestStatus implements HarvestStatus {

		@Override
		public void resetForNewSource() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void update(SourcePojo sourceToUpdate, Date harvestDate,
				HarvestEnum harvestStatus, String harvestMessage,
				boolean bTempDisable, boolean bPermDisable) {
			//TODO
			System.out.println("TODO NEED TO WRITE THIS: " + harvestMessage);
		}

		@Override
		public void logMessage(String message, boolean bAggregate) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public boolean moreToLog() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public String getMostCommonMessage() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public int getNumMessages() {
			// TODO Auto-generated method stub
			return 0;
		}
		
	}

}
