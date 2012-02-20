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
public interface DuplicateManager {
	
	// Resets source-specific state
	void resetForNewSource();
	
	/**
	 * Tests to see if duplicates exist based on defined key
	 * 
	 * @param collection
	 * @param key
	 * @return boolean (true/false)
	 */
	boolean isDuplicate_UrlTitleDescription(String url, String title, String description, SourcePojo source, List<String> duplicateSources);
	
	/**
	 * Tests to see if duplicates exist based on defined key
	 * 
	 * @param collection
	 * @param key
	 * @return boolean (true/false)
	 */
	boolean isDuplicate_UrlTitle(String url, String title, SourcePojo source, List<String> duplicateSources);
	/**
	 * Tests to see if duplicates exist based on defined key
	 * 
	 * @param collection
	 * @param key
	 * @return boolean (true/false)
	 */
	boolean isDuplicate_Url(String url, SourcePojo source, List<String> duplicateSources);

	/**
	 * Tests to see if duplicates exist based on defined key.
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
	boolean needsUpdated_SourceUrl(Date modifiedDate, String sourceUrl, String sourceKey);
	
	boolean needsUpdated_Url(Date modifiedDate, String url, String sourceKey);	
}