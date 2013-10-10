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
