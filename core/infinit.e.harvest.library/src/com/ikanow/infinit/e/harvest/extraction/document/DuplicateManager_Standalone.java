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
	 * @param collection
	 * @param key
	 * @return boolean (true/false)
	 */
	public boolean isDuplicate_UrlTitleDescription(String url, String title, String description, SourcePojo source, List<String> duplicateSources) {
		return false;
	}
	
	/**
	 * Tests to see if duplicates exist based on defined key
	 * 
	 * @param collection
	 * @param key
	 * @return boolean (true/false)
	 */
	public boolean isDuplicate_UrlTitle(String url, String title, SourcePojo source, List<String> duplicateSources) {
		return false;
	}
	/**
	 * Tests to see if duplicates exist based on defined key
	 * 
	 * @param collection
	 * @param key
	 * @return boolean (true/false)
	 */
	private int _nCount2 = 0;
	final private int _nEvery2 = 10; 
	public boolean isDuplicate_Url(String url, SourcePojo source, List<String> duplicateSources) {
		//(test code)
		return (++_nCount2 % _nEvery2) == 0;
	}	

	/**
	 * Tests to see if duplicates might exist based on defined key.
	 * If it is not a duplicate, true is returned. If it is a duplicate,
	 * the modified date is then checked to see if the file has been updated.
	 * True is returned if the file has been updated, false otherwise.
	 * 
	 * @param collection
	 * @param modifiedDate
	 * @param url
	 * @param title
	 * @return boolean (true/false)
	 */
	private int _nCount1 = 0;
	final private int _nEvery1 = 4; 
	public boolean needsUpdated_SourceUrl(Date modifiedDate, String sourceUrl, String sourceKey) {
		if ((++_nCount1 % _nEvery1) == 0) {
			modifiedDate.setTime(0);
		}
		return true;
	}	
	
	public boolean needsUpdated_Url(Date modifiedDate, String url, String sourceKey) {
		return true;
	}		
}
