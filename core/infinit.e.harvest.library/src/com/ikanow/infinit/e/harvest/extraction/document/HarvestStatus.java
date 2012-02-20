package com.ikanow.infinit.e.harvest.extraction.document;

import java.util.Date;

import com.ikanow.infinit.e.data_model.InfiniteEnums.HarvestEnum;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;

public interface HarvestStatus {

	/**
	 * updateHarvestStatus
	 * 
	 * @param sourceToUpdate
	 * @param harvestDate
	 * @param harvestStatus
	 * @param harvestMessage
	 * @param bTempDisable
	 * @param bPermDisable
	 */
	void update(SourcePojo sourceToUpdate, Date harvestDate, HarvestEnum harvestStatus, 
					String harvestMessage, boolean bTempDisable, boolean bPermDisable);
	

	/**
	 * logMessage
	 * Logs temporary messages
	 * should switch sourcepojo to use correct id field and search on that.
	 * 
	 * @param message The message to log
	 * @param bAggregate If true, duplicate error messages are aggregated
	 */
	void logMessage(String message, boolean bAggregate);

	/**
	 * moreToLog
	 * @return true if custom enrichment has generated more errors
	 */
	boolean moreToLog();
}
