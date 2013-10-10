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

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;

import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ikanow.infinit.e.data_model.InfiniteEnums.HarvestEnum;
import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.MongoDbManager;
import com.ikanow.infinit.e.data_model.store.config.source.SourceHarvestStatusPojo;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;

public class HarvestStatus_Integrated implements HarvestStatus {

	/**
	 * updateHarvestStatus
	 * Currently I am using the key to find the old source to update
	 * should switch sourcepojo to use correct id field and search on that.
	 * 
	 * @param sourceToUpdate
	 * @param harvestDate
	 * @param harvestStatus
	 * @param harvestMessage
	 * @param bTempDisable
	 * @param bPermDisable
	 */
	public void update(SourcePojo sourceToUpdate, Date harvestDate, HarvestEnum harvestStatus, 
			String harvestMessage, boolean bTempDisable, boolean bPermDisable)
	{
		// Handle successful harvests where the max docs were reached, so don't want to respect the searchCycle
		if ((harvestStatus == HarvestEnum.success) && (sourceToUpdate.reachedMaxDocs())) {
			harvestStatus = HarvestEnum.success_iteration;
		}
		
		if (null == sourceToUpdate.getHarvestStatus()) {
			sourceToUpdate.setHarvestStatus(new SourceHarvestStatusPojo());
		}
		
		BasicDBObject update = new BasicDBObject();
			// (annoyingly need to do this in raw format because otherwise overwrite any existing fields eg synced,doccount)
		if ((null == sourceToUpdate.getDistributionFactor()) || (HarvestEnum.in_progress != harvestStatus)) {
			update.put(SourceHarvestStatusPojo.sourceQuery_harvest_status_, harvestStatus.toString());
		}
		update.put(SourceHarvestStatusPojo.sourceQuery_harvested_, harvestDate);
		sourceToUpdate.getHarvestStatus().setHarvest_status(harvestStatus);
		sourceToUpdate.getHarvestStatus().setHarvested(harvestDate);

		// Optional fields:
		// Display message
		if (null == _currMessage) {	
			if ((null == harvestMessage) || harvestMessage.isEmpty()) {
				sourceToUpdate.getHarvestStatus().setHarvest_message("");				
			}
			else {
				sourceToUpdate.getHarvestStatus().setHarvest_message(harvestMessage);
			}
		}//TESTED
		else { // New messages to display
			if ((null != harvestMessage) && !harvestMessage.isEmpty()) {
				_currMessage.insert(0, harvestMessage);
				_currMessage.insert(harvestMessage.length(), '\n');
			}
			if ((null != _messages) && !_messages.isEmpty()) {
				_currMessage.append('\n');	
				_currMessage.append(getLogMessages(true));
			}
			sourceToUpdate.getHarvestStatus().setHarvest_message(_currMessage.toString());				
			_currMessage.setLength(0);
		}//TESTED
		//(end display message)
		
		if (sourceToUpdate.getHarvestStatus().getHarvest_message().length() > 0) {
			// (only overwrite the previous message if there's actually something to say...)
			update.put(SourceHarvestStatusPojo.sourceQuery_harvest_message_, sourceToUpdate.getHarvestStatus().getHarvest_message());
			
			if ((null != sourceToUpdate.getDistributionTokens()) && !sourceToUpdate.getDistributionTokens().isEmpty()) {
				for (Integer token: sourceToUpdate.getDistributionTokens()) {
					update.put(SourceHarvestStatusPojo.sourceQuery_distributedStatus_ + "." + token.toString(), sourceToUpdate.getHarvestStatus().getHarvest_message());
				}
			}//TESTED
		}
		if (bTempDisable) {
			sourceToUpdate.setHarvestBadSource(true);
			update.put(SourcePojo.harvestBadSource_, true);			
		}
		if (bPermDisable) {
			if ((null == sourceToUpdate.getSearchCycle_secs()) || (0 == sourceToUpdate.getSearchCycle_secs())) {
				sourceToUpdate.setSearchCycle_secs(-1);
			}
			else if (sourceToUpdate.getSearchCycle_secs() > 0) { //(else it's already negative, ie run manually)
				sourceToUpdate.setSearchCycle_secs(-sourceToUpdate.getSearchCycle_secs());				
			}
			update.put(SourcePojo.searchCycle_secs_, sourceToUpdate.getSearchCycle_secs());
		}
		DBCollection sourceDb = DbManager.getIngest().getSource();
		BasicDBObject query = new BasicDBObject(SourcePojo._id_, sourceToUpdate.getId());
		sourceDb.update(query, new BasicDBObject(MongoDbManager.set_, update));
	}
	/**
	 * logMessage
	 * Logs temporary messages
	 * should switch sourcepojo to use correct id field and search on that.
	 * 
	 * @param message The message to log
	 * @param bAggregate If true, duplicate error messages are aggregated
	 */
	public void logMessage(String message, boolean bAggregate) {
		if (null == message) return;
		
		if (null == _currMessage) {
			_currMessage = new StringBuffer();
		}
		if (!bAggregate) {
			if (_currMessage.length() > 0) {
				_currMessage.append('\n');
			}
			_currMessage.append(message);
		}
		else { // Aggregate messages
			if (null == _messages) {
				_messages = new HashMap<String, Integer>();
			}
			if (_messages.size() > 0) {
				Integer count = (Integer) _messages.get(message);
				
				if (count != null && count > 0) {
					_messages.put(message, count + 1);
				}
				else {
					_messages.put(message, 1);
				}
			}
			else {
				_messages.put(message, 1);
			}
		}
	}//TESTED
	/**
	 * moreToLog
	 * @return true if custom enrichment has generated more errors
	 */
	public boolean moreToLog() {
		return (null != _currMessage);
	}//TOTEST
	
	
	private StringBuffer _currMessage = null; // Current message (output at the end of the source processing)
 	private HashMap<String, Integer> _messages = null; // (list of messages to aggregate)
	
	/**
	 * getLogMessages
	 * Returns a list of up to 5 errors (eg encountered when parsing JavaScript) for 
	 * a source, sorted by frequency in ascending order
	 * @return
	 */
	private StringBuffer getLogMessages(boolean bReset)
	{
		if ((null != _messages) && (_messages.size() > 0))
		{
			StringBuffer messagesString = new StringBuffer();
		
			// Create multimap to store errors in, reverse the order of key (error message) and
			// value (count) to sort on error count
			Multimap<Integer, String> mm = TreeMultimap.create();
			for (java.util.Map.Entry<String, Integer> entry : _messages.entrySet()) 
			{
				StringBuffer msg = new StringBuffer(entry.getKey()).append(" (Occurences: ").append(entry.getValue()).append(')');
				mm.put(-entry.getValue(), msg.toString());
			}
			
			// Write the error messages to a Collection<String>
			Collection<String> messages = mm.values();
			
			// Append up to the top five messages to our StringBuffer and return
			int messageCount = 1;
			for (String s : messages)
			{
				if (messageCount > 1) messagesString.append('\n');
				messagesString.append(s);
				messageCount++;
				if (messageCount > 5) break;
			}
			if (bReset) {
				_messages.clear();
			}
			return messagesString;
		}
		else
		{
			return null;
		}
	}//TESTED
}
