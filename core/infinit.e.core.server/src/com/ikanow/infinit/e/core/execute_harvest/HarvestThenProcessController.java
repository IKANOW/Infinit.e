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
package com.ikanow.infinit.e.core.execute_harvest;

import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import com.ikanow.infinit.e.core.execute_harvest.utils.BlockingExecutor;
import com.ikanow.infinit.e.core.utils.PropertiesManager;
import com.ikanow.infinit.e.core.utils.SourceUtils;
import com.ikanow.infinit.e.data_model.InfiniteEnums.HarvestEnum;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.infinit.e.harvest.HarvestController;
import com.ikanow.infinit.e.processing.generic.GenericProcessingController;
import com.ikanow.infinit.e.processing.generic.aggregation.AggregationManager;
import com.ikanow.infinit.e.processing.generic.aggregation.EntityBackgroundAggregationManager;
import com.ikanow.infinit.e.processing.generic.aggregation.AssociationBackgroundAggregationManager;
import com.ikanow.infinit.e.processing.generic.store_and_index.StoreAndIndexManager;

public class HarvestThenProcessController {
    private static Logger _logger = Logger.getLogger(HarvestThenProcessController.class);
    private static boolean _bStopHarvest = false;
	private static boolean _bCurrentlySleepingBeforeExit = false;
	private static boolean _bReadyToTerminate = false;
	private static Thread _mainThread = null;
   
////////////////////////////////////////////////////////////////////////////////////////////
    
// Start harvesting
    
	/**
	 * Used to start the sync service
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	public void startService(LinkedList<SourcePojo> sources) throws IOException, InterruptedException 
	{
		// Let the client know the server is starting
        System.out.println("[SERVER] Harvest server is coming online"); 
                
		// Intialize/update generic process controller (do this here so that it blocks before threading fun starts) 
		new GenericProcessingController().Initialize();
		
		//Start the background aggregation thread (will do nothing if disabled)
		EntityBackgroundAggregationManager.startThread();
		AssociationBackgroundAggregationManager.startThread();
		
		_mainThread = Thread.currentThread();
		
        // Add the shutdown hook
        ShutdownHook shutdownHook = new ShutdownHook();
        Runtime.getRuntime().addShutdownHook(shutdownHook);

        Date startDate = new Date();
		_logger.info("Starting harvest process at: " + startDate);
		
        // Perform processing
        
        PropertiesManager threadConfig = new PropertiesManager();
        String sThreadConfig = threadConfig.getHarvestThreadConfig();
        
        try {         	
        	// All source types in a single thread
        	
        	int nThreads = Integer.parseInt(sThreadConfig);
        	SourceTypeHarvesterRunnable allTypes = new SourceTypeHarvesterRunnable(sources, nThreads); 
    		_logger.info("(Launching " + nThreads + " threads for all source types)");	        	
        	allTypes.run();        	
        }
        catch (NumberFormatException e) { 
        	
        	// The thread config must be comma-separated list of type:threads
        	
        	// (step over each type and launch that number of threads for that type)
        	
        	String[] sConfigBlocks = sThreadConfig.split("\\s*,\\s*");
        	ExecutorService exec = Executors.newFixedThreadPool(sConfigBlocks.length);
        	for (String sConfigBlock: sConfigBlocks) {
        		String[] sTypeOrNumThreads = sConfigBlock.split("\\s*:\\s*");
        		if (2 == sTypeOrNumThreads.length) {
        			try {
        				int nThreads = Integer.parseInt(sTypeOrNumThreads[1]);
        				SourceTypeHarvesterRunnable typeRunner = new SourceTypeHarvesterRunnable(sources, nThreads, sTypeOrNumThreads[0]);
        	    		_logger.info("(Launching " + nThreads + " threads for " + sTypeOrNumThreads[0] + " source types)");	    
        	    		exec.submit(typeRunner);
        			}
        			catch (NumberFormatException e2) {
            			_logger.error("Error in harvester thread configuration: " + sThreadConfig);        			        				
        			}
        		}
        		else {
        			_logger.error("Error in harvester thread configuration: " + sThreadConfig);        			
        		}
        	}//(end loop over different file types)
			exec.shutdown();
			while (!exec.isTerminated()) {
				try {
					Thread.sleep(1000);
				} 
				catch (InterruptedException e3) { }
			}			        
        }
        com.ikanow.infinit.e.processing.generic.utils.PropertiesManager aggProps = new com.ikanow.infinit.e.processing.generic.utils.PropertiesManager();
        boolean bAggDisabled = aggProps.getAggregationDisabled();
        StoreAndIndexManager dataStore = new StoreAndIndexManager();
        boolean bResizedDB = dataStore.resizeDB();
        if (!bAggDisabled) {
        	AggregationManager.updateEntitiesFromDeletedDocuments(dataStore.getUUID());
        }
        dataStore.removeSoftDeletedDocuments();
        if (!bAggDisabled) {
        	AggregationManager.updateDocEntitiesFromDeletedDocuments(dataStore.getUUID());
        }
        if (bResizedDB) {
        	_logger.info("(resized DB, now " + dataStore.getDatabaseSize() + " documents)");
        }
        
		HarvestController.logHarvesterStats();
		_logger.info("Completed harvest process at: " + new Date().toString());
		
        Date endDate = new Date();
        // Not allowed to cycle harvester runs too quickly
		// Sleep for some period:
		long nDiff = endDate.getTime() - startDate.getTime();
		long nToSleep = threadConfig.getMinimumHarvestTimeMs() - nDiff;
		if ((nToSleep > 0) && !_bCurrentlySleepingBeforeExit) {
			try {
				_bCurrentlySleepingBeforeExit = true; // (don't really care there's a minor race condition here) 
				Thread.sleep(nToSleep);
			} catch (InterruptedException e) {
				// Do nothing, probably got a signal
			}
		}//TESTED (cut and paste from tested Beta code)
        
		// Stop background aggregation
		EntityBackgroundAggregationManager.stopThreadAndWait();
		AssociationBackgroundAggregationManager.stopThreadAndWait();
		
		_logger.info("Harvest server is going offline");
		_bStopHarvest = true;
		_bReadyToTerminate = true; // (if we were terminated manually tell the shutdown hook it can stop)
		System.exit(0);
		
    }//TESTED
	
////////////////////////////////////////////////////////////////////////////////////////////
	
// Multi-threading

	////////////////////////////////////////////////////////////////////////////////////////////
	
	// 1] Different sources get different threads
	
	static private class SourceTypeHarvesterRunnable implements Runnable {
		
		private int _nThreads = 0;
		private LinkedList<SourcePojo> _sources;
		private String _sSourceType = null;
		
		public SourceTypeHarvesterRunnable(LinkedList<SourcePojo> sources, int nThreads) {
			_sources = new LinkedList<SourcePojo>(sources);
			_nThreads = nThreads;
		}
		public SourceTypeHarvesterRunnable(LinkedList<SourcePojo> sources, int nThreads, String sSourceType) {
			_sources = new LinkedList<SourcePojo>(sources);
			_sSourceType = sSourceType;
			_nThreads = nThreads;
		}
		
		public void run() {
	        ExecutorService exec = Executors.newFixedThreadPool(_nThreads);
	        BlockingExecutor bex = new BlockingExecutor(exec, 2*_nThreads);
	        	// (allow a few sources to get queued up)
	        
	        if (null != _sSourceType) {
	    		_logger.info("(Starting harvest thread for " + _sSourceType + ")");	        	
	        }	        
	        while (!_sources.isEmpty()) {
	        	LinkedList<SourcePojo> sourceBatch = SourceUtils.getDistributedSourceList(_sources, _sSourceType, false);
	        	while (!sourceBatch.isEmpty()) {
	        		SourcePojo source = sourceBatch.pop();
					SourceHarvesterRunnable sourceRunner = new SourceHarvesterRunnable(source);
					
					boolean bSubmittedTask = false;
					for (int i = 0; (i < 5) && !bSubmittedTask; ++i) {
						try {
							bex.submitTask(sourceRunner, true);
							bSubmittedTask = true;
						} 
						catch (Exception e) { 
							try {
					    		_logger.info("(Thread failure for " + _sSourceType + ", can probably recover)");	        	
								Thread.sleep(1000); // wait a second
								
							} catch (InterruptedException e1) { } 
						}
					}
					
					if (_bStopHarvest) { // Just need to update the status of all remaining source
			    		_logger.info("(Shutdown, cleaning up " + sourceBatch.size() + " queued sources for " + _sSourceType + ")");	        	
			        	for (SourcePojo sourceToDelete: sourceBatch) {
			        		if ((null != source.getHarvestStatus()) && 
			        				(null != source.getHarvestStatus().getHarvest_status()) &&
			        					(HarvestEnum.in_progress != source.getHarvestStatus().getHarvest_status()))
			        		{
			        			// (If I can revert to old status)
			        			SourceUtils.updateHarvestStatus(sourceToDelete, source.getHarvestStatus().getHarvest_status(), null, 0);
			        		}
			        		else {
			        			SourceUtils.updateHarvestStatus(sourceToDelete, HarvestEnum.success, null, 0);        			
			        		}
			        	}
						break;
					}//TESTED
					
				} // (end loop over this batch)
				if (_bStopHarvest) { // Just need to update the status of all remaining source
					break;
				}//TESTED
				
	        } // (end loop over entire DB)
			exec.shutdown();
			while (!exec.isTerminated()) {
				try {
					Thread.sleep(1000);
				} 
				catch (InterruptedException e) { }
			}			
		}
	}//TESTED
	
	////////////////////////////////////////////////////////////////////////////////////////////	
	
	// 2] The actual running of the harvester
	
	static private class SourceHarvesterRunnable implements Runnable {
		
		// Per-thread accessors for HC and GPCoh it		
		private static ThreadLocal<HarvestController> _harvesterController;
		private static ThreadLocal<GenericProcessingController> _genericController;
		
		static {
		
			_harvesterController = new ThreadLocal<HarvestController>() {
		        @Override protected HarvestController initialValue() {
		            try {
		            	HarvestController hc = new HarvestController();
						return hc;
					} catch (Exception e) {
						return null;
					}
		        }
			};
			_genericController = new ThreadLocal<GenericProcessingController>() {
		        @Override protected GenericProcessingController initialValue() {
		            try {
						return new GenericProcessingController();
					} catch (Exception e) {
						return null;
					}
		        }
			};

		}
		
		// Per thread processing 
				
		SourceHarvesterRunnable(SourcePojo source) {
			_sourceToProcess = source;
		}
		private SourcePojo _sourceToProcess = null;
		
		public void run() {
			
			try {
				if (null == _harvesterController.get()) { // Some sort of internal bug? No idea...
					_harvesterController.set(new HarvestController());
				}
				if (null == _genericController.get()) { // (ditto, not seen this but better safe than sorry)
					_genericController.set(new GenericProcessingController());
				}
				
				List<DocumentPojo> toAdd = new LinkedList<DocumentPojo>();
				List<DocumentPojo> toUpdate = new LinkedList<DocumentPojo>();
				List<DocumentPojo> toRemove = new LinkedList<DocumentPojo>();				
				
				_harvesterController.get().harvestSource(_sourceToProcess, toAdd, toUpdate, toRemove);
					// (toAdd includes toUpdate)
				
				if (HarvestEnum.error != _sourceToProcess.getHarvestStatus().getHarvest_status()) {
					_genericController.get().processDocuments(SourceUtils.getHarvestType(_sourceToProcess), toAdd, toUpdate, toRemove, _sourceToProcess);
						// (toRemove includes toUpdate)
					
					SourceUtils.updateHarvestStatus(_sourceToProcess, HarvestEnum.success, toAdd, toRemove.size());
						// (note also releases the "in_progress" lock)
						// (note also prunes sources based on "maxDocs")
						// (also handles the intra-source distribution logic)
				}
				// (if we've declared error, then "in_progress" lock already released so nothing to do)
			}
			catch (Error e) { // Don't like to catch these, but otherwise we leak away sources
				SourceUtils.updateHarvestStatus(_sourceToProcess, HarvestEnum.error, null, 0);					
				_logger.error("Source error on " + _sourceToProcess.getKey() + ": " + e.getMessage());
				e.printStackTrace();				
			}
			catch (Exception e) { // Limit any problems to a single source
				SourceUtils.updateHarvestStatus(_sourceToProcess, HarvestEnum.error, null, 0);					
				_logger.error("Source error on " + _sourceToProcess.getKey() + ": " + e.getMessage());
				e.printStackTrace();
			}			
		}
	}//TESTED
	
////////////////////////////////////////////////////////////////////////////////////////////	
	
// External restart
	
	class ShutdownHook extends Thread 
	{
	    public void run() 
	    {
	    	if (!_bStopHarvest) {
	    		
	    		boolean bLocalSleep = _bCurrentlySleepingBeforeExit;
	    		_bCurrentlySleepingBeforeExit = true; // (so won't sleep now when it gets to the end)
	    		
	    		_logger.error("Clean shutdown attempt");
	    		_bStopHarvest = true;
	    		HarvestController.killHarvester();
	    		
	    		if (bLocalSleep) {
	    			_mainThread.interrupt();
	    			// (Don't mind the minor race condition that's here, worst case have to wait a few more minutes)
	    		}
	    		
	    		// Wait at most 10 minutes
	    		for (int i = 0; i < 600; ++i) {
	    			try {
	    				if (_bReadyToTerminate) {
	    					break;
	    				}
						Thread.sleep(1000);
					} catch (InterruptedException e) {}
	    		}
	    		if (!_bReadyToTerminate) {
	    			_logger.error("Unclean shutdown #1");
	    		}
	    	}
	    }
	}//TESTED
}
