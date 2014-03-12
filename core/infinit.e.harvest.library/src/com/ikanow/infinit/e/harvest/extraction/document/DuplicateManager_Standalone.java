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
/**
 * 
 */
package com.ikanow.infinit.e.harvest.extraction.document;

import java.util.Date;
import java.util.List;

import org.bson.types.ObjectId;

import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;

/**
 * @author cmorgan
 *
 */
public class DuplicateManager_Standalone implements DuplicateManager {
		
	// Resets source-specific state
	public void resetForNewSource() {}
	
	/**
	 * Tests to see if duplicates exist based on defined key
	 * 
	 * @param title - currently ignored
	 * @param description - currently ignored
	 * @param duplicateSources - list of sources containing a duplicate URL, filled in transiently by calls to this function
	 * @return boolean (true/false)
	 */
	public boolean isDuplicate_UrlTitleDescription(String url, String title, String description, SourcePojo source, List<String> duplicateSources) {
		return false;
	}
	
	/**
	 * Tests to see if duplicates exist based on defined key
	 * 
	 * @param duplicateSources - list of sources containing a duplicate URL, filled in transiently by calls to this function
	 * @return boolean (true/false)
	 */
	public boolean isDuplicate_Url(String url, SourcePojo source, List<String> duplicateSources) {
		//(removed debug/test code from here since this gets used quite a lot by the test api)
		return false;
	}	

	/**
	 * Tests to see if duplicates might exist based on defined key.
	 * If it is not a duplicate, true is returned. If it is a duplicate,
	 * the modified date is then checked to see if the file has been updated.
	 * True is returned if the file has been updated, false otherwise.
	 * 
	 * @return boolean (true/false)
	 */
	public boolean needsUpdated_SourceUrl(Date modifiedDate, String sourceUrl, SourcePojo source) {
		//(removed debug/test code from here since this gets used quite a lot by the test api and the deleted=1 is confusing)
		modifiedDate.setTime(0);
		return true;
	}	
	
	public boolean needsUpdated_Url(Date modifiedDate, String url, SourcePojo source) {
		return true;
	}		
	public Date getLastDuplicateModifiedTime() { // (returns an hour ago)
		return new Date(new Date().getTime() - 3600);
	}
	public ObjectId getLastDuplicateId() {
		return null;
	}

}
