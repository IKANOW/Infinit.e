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
package com.ikanow.infinit.e.core.execute_synchronization;

import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.ikanow.infinit.e.core.utils.SourceUtils;
import com.ikanow.infinit.e.data_model.InfiniteEnums.HarvestEnum;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.ikanow.infinit.e.processing.generic.aggregation.AggregationManager;
import com.ikanow.infinit.e.processing.generic.store_and_index.StoreAndIndexManager;
import com.ikanow.infinit.e.processing.generic.synchronization.SynchronizationManager;

public class SynchronizationController {
    private static Logger logger = Logger.getLogger(SynchronizationController.class);
    private static boolean stopSync = false;
    private static boolean _bReadyToTerminate = false;
        
////////////////////////////////////////////////////////////////////////////////////////////
    
// Start synchronization
    
	/**
	 * Used to start the sync service
	 */
	public void startService(long time_of_last_cleanse_secs, LinkedList<SourcePojo> sources) 
	{
		// Let the client know the server is starting
        System.out.println("[SERVER] Sync server is coming online"); 
                
        // Add the shutdown hook
        ShutdownHook shutdownHook = new ShutdownHook();
        Runtime.getRuntime().addShutdownHook(shutdownHook);

        // Perform processing
        
		//SET LAST SYNC TIME to 2 hours before
		long time_of_this_cleanse = 0;		
		if ( 0 != time_of_last_cleanse_secs ) {
			time_of_this_cleanse = (time_of_last_cleanse_secs*1000L - 2*3600000L); // 3600,000 ms==1hr			
		}
		else { // Let's do 0 == two days ago, set it to 1 if you really want all time....
			time_of_this_cleanse = new Date().getTime() - 48L*3600000L;
		}
		if (time_of_this_cleanse < 0) {
			time_of_this_cleanse = 0;
		}
		logger.info("Starting sync process at: " + new Date().toString());
		logger.info("Syncing: " + sources.size() + " sources from time: " + new Date(time_of_this_cleanse));
		
		int fixes_db = 0;
		int fixes_search = 0;
        while (!sources.isEmpty()) {
        	List<SourcePojo> sourceBatch = SourceUtils.getDistributedSourceList(sources, null, true);
        	try {        		
		        SynchronizationManager syncManager = new SynchronizationManager();
		        syncManager.setSources(sourceBatch);
		        
		        Set<String> dbCache = new HashSet<String>();
		        
		        logger.debug("Syncing: " + sourceBatch.size());
		        
				fixes_db += syncManager.syncDB(time_of_this_cleanse, dbCache);
				
		        logger.debug("Syncing DB: " + fixes_db);
				
				fixes_search += syncManager.syncSearch(time_of_this_cleanse, dbCache);
				
		        logger.debug("Syncing Index: " + fixes_search);		        
        	}
        	catch (Exception e) {
        		// Do nothing, the purpose of this try/catch is to ensure that the updateSyncStatus below always gets called
        	}
        	for (SourcePojo source: sourceBatch) {
        		if ((null != source.getHarvestStatus()) && 
        				(null != source.getHarvestStatus().getHarvest_status()) &&
        					(HarvestEnum.in_progress != source.getHarvestStatus().getHarvest_status()))
        		{
        			SourceUtils.updateSyncStatus(source, source.getHarvestStatus().getHarvest_status());
        		}
        		else {
        			SourceUtils.updateSyncStatus(source, HarvestEnum.success);        			
        		}
        	}
        	
        }//end loop over all sources
        StoreAndIndexManager dataStore = new StoreAndIndexManager();
        AggregationManager.updateEntitiesFromDeletedDocuments(dataStore.getUUID());
        dataStore.removeSoftDeletedDocuments();
        AggregationManager.updateDocEntitiesFromDeletedDocuments(dataStore.getUUID());

		logger.info("DB fixes: " + fixes_db);
		logger.info("Search fixes: " + fixes_search);
        
		logger.info("Completed sync process at: " + new Date().toString());
		
		logger.info("Sync server is going offline");
		stopSync = true;
		_bReadyToTerminate = true; // (if we were terminated manually tell the shutdown hook it can stop)
		System.exit(0);
    }//TESTED
	
////////////////////////////////////////////////////////////////////////////////////////////	
	
// External restart
	
	class ShutdownHook extends Thread 
	{
	    public void run() 
	    {
	    	if ( !stopSync )
	    	{
	    		logger.error("Clean shutdown attempt");
	    		SynchronizationManager.killMe();
	    		
	    		// Wait at most 10 minutes
	    		for (int i = 0; i < 600; ++i) {
	    			try {
						Thread.sleep(1000);
	    				if (_bReadyToTerminate) {
	    					break;
	    				}
					} catch (InterruptedException e) {}
	    		}
	    		if (!_bReadyToTerminate) {
	    			logger.error("Unclean shutdown #1");
	    		}
	    	}
	    }
	}//TESTED (by hand/eye)
}
