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

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;

import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ikanow.infinit.e.data_model.InfiniteEnums.HarvestEnum;
import com.ikanow.infinit.e.data_model.store.config.source.SourceHarvestStatusPojo;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;

public class HarvestStatus_Standalone implements HarvestStatus {

	@Override
	public void resetForNewSource() {
		if (null != _messages) {
			_messages.clear();
		}
		_numMessages = 0;
		_currMessage = null;		
	}//TESTED
	
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
		if (HarvestEnum.in_progress == harvestStatus) {
			// This is just for display purposes and never run post-processing so just declare success!
			harvestStatus = HarvestEnum.success;
		}//TESTED
		if ((null != harvestMessage) && !harvestMessage.isEmpty()) {
			this.logMessage(harvestMessage, false);
			if (HarvestEnum.error == harvestStatus) {
				_numMessages++;
			}
		}//TOTEST	
		
		SourceHarvestStatusPojo hp = new SourceHarvestStatusPojo();
		hp.setHarvested(harvestDate);
		hp.setHarvest_status(harvestStatus);
		sourceToUpdate.setHarvestStatus(hp);
		sourceToUpdate.getHarvestStatus().setRealHarvested(harvestDate);
		
		// Display message
		if (null == _currMessage) {	
			// (then also no harvest message else would have logged already)
			sourceToUpdate.getHarvestStatus().setHarvest_message("");				
		}//TESTED
		else { // New messages to display
			String date = new SimpleDateFormat("'['yyyy-MM-dd'T'HH:mm:ss']' ").format(new Date());
			if ((null != _messages) && !_messages.isEmpty()) {
				_currMessage.append('\n');	
				_currMessage.append(getLogMessages(true)); // (clears _messages)
			}
			sourceToUpdate.getHarvestStatus().setHarvest_message(date + _currMessage.toString());				
		}//TESTED
		//(end display message)
		
		if (bTempDisable) {
			sourceToUpdate.setHarvestBadSource(true);
		}
		if (bPermDisable) {
			sourceToUpdate.setApproved(false);
		}
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
		}//TESTED
		else { // Aggregate messages
			_numMessages++;
			if (null == _messages) {
				_messages = new HashMap<String, Integer>();
			}
			if (_messages.size() > 0) { //(unlike integrated, don't limit # messages in standalone)
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
		}//TESTED
	}
	/**
	 * moreToLog
	 * @return true if custom enrichment has generated more errors
	 */
	public boolean moreToLog() {
		return (null != _currMessage);
	}//TESTED	
	
	private StringBuffer _currMessage = null; // Current message (output at the end of the source processing)
 	private HashMap<String, Integer> _messages = null; // (list of messages to aggregate)
	private int _numMessages = 0;
 	public int getNumMessages() {
 		return _numMessages;
 	}
	
	/**
	 * getLogMessages
	 * Returns a list of up to 20 errors (eg encountered when parsing JavaScrip)t for 
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
			
			// Append up to the top 20 messages to our StringBuffer and return
			int messageCount = 1;
			for (String s : messages)
			{
				if (messageCount > 1) {
					messagesString.append('\n');
				}
				messagesString.append(s);
				messageCount++;
				if (messageCount > 20) break;
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
	}//TESTED (cut and paste from old code)

 	public String getMostCommonMessage() {
 		int max = -1;
 		String maxMsg = null;
 		if (null != _messages) {
			for (java.util.Map.Entry<String, Integer> entry : _messages.entrySet()) 
			{
				if (entry.getValue() > max) {
					max = entry.getValue();
					maxMsg = entry.getKey();
				}
			}
 		}
		if (null != maxMsg) {
			return new StringBuffer(" errmsg='").append(max).append(": ").append(maxMsg).append("'").toString();
		}
		else return "";
 	}//TESTED
}
