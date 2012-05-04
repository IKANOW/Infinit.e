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
package com.ikanow.infinit.e.data_model.store.config.source;

public class SourceSearchFeedConfigPojo {

	// JSON fields
	
	private String userAgent = null; // Optional, if present is used for the "User-Agent:" HTTP field
	
	private String script = null; // Mandatory, processes the search results and returns an array of the following JSON objects:
									// { "description: string, // (optionally)
									//   "publishedDate": string, // (optionally)
									//   "title": string,
									//	 "url": string }
									// These links are then harvested as if they were "rss.extraUrls"
	private String scriptlang = null; // Currently only "javascript" is supported
	
	private String pageChangeRegex = null; // If non-null, this regex should be used to match the pagination URL parameter (which will be replaced by pageChangeReplace)
											// Also, group 1 should be the start, to allow any offsets specified in the URL to be respected
	private String pageChangeReplace = null; // Mandatory if pageChangeRegex is non-null, must be a replace string where $1 is the page*numResultsPerPage
	private Integer numPages = 10; // Mandatory if pageChangeRegex is non-null - controls the number of pages deep the search will go
	private Integer numResultsPerPage = 1; // Mandatory if pageChangeRegex is non-null - controls the number of results per page 
	private Integer waitTimeBetweenPages_ms = null; // Optional, only used if pageChangeRegex is non-null - controls a wait between successive pages if set
	
	// Getters and setters
	
	public String getUserAgent() {
		return userAgent;
	}
	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}
	public String getScript() {
		return script;
	}
	public void setScript(String script) {
		this.script = script;
	}
	public String getPageChangeRegex() {
		return pageChangeRegex;
	}
	public void setPageChangeRegex(String pageChangeRegex) {
		this.pageChangeRegex = pageChangeRegex;
	}
	public Integer getNumPages() {
		return numPages;
	}
	public void setNumPages(Integer numPages) {
		this.numPages = numPages;
	}
	public void setPageChangeReplace(String pageChangeReplace) {
		this.pageChangeReplace = pageChangeReplace;
	}
	public String getPageChangeReplace() {
		return pageChangeReplace;
	}
	public void setNumResultsPerPage(Integer numResultsPerPage) {
		this.numResultsPerPage = numResultsPerPage;
	}
	public Integer getNumResultsPerPage() {
		return numResultsPerPage;
	}
	public void setScriptlang(String scriptlang) {
		this.scriptlang = scriptlang;
	}
	public String getScriptlang() {
		return scriptlang;
	}
	public void setWaitTimeBetweenPages_ms(Integer waitTimeBetweenPages_ms) {
		this.waitTimeBetweenPages_ms = waitTimeBetweenPages_ms;
	}
	public Integer getWaitTimeBetweenPages_ms() {
		return waitTimeBetweenPages_ms;
	}
	
}
