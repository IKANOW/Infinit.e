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

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SourceRssConfigPojo {

	private String feedType = null;
	
	private Integer waitTimeOverride_ms = null; // If specified, overrides the system wait time between consecutive
	
	private String regexInclude = null;
	private String regexExclude = null;
	
	private transient Pattern regexIncludePattern = null;
	private transient Pattern regexExcludePattern = null;	
	
	// Intended to be a temporary way of handling non-RSS feeds for a given source
	public static class ExtraUrlPojo {
		public String url;
		public String title;
		public String description; // (optional)
		public String publishedDate; // (optional)
		public String fullText; // (optional - for cases where you need to build many docs from one page, also debugging) 
	}
	private List<ExtraUrlPojo> extraUrls; 
	
	private String userAgent = null; // (if present, used to override the userAgent)
	
	// Using a set of search results to generate the feed:
	
	private SourceSearchFeedConfigPojo searchConfig = null;
	
	private Integer updateCycle_secs; // Optional, if present will re-extract duplicate URLs
	
// Functions:
	
	public Matcher getIncludeMatcher(String sUrl) {
		if ((null == regexIncludePattern) && (null != regexInclude)) {
			regexIncludePattern = Pattern.compile(regexInclude, Pattern.CASE_INSENSITIVE);
		}
		if (null != regexIncludePattern) {
			return regexIncludePattern.matcher(sUrl);
		}
		else return null;
	}
	public Matcher getExcludeMatcher(String sUrl) {
		if ((null == regexExcludePattern) && (null != regexExclude)) {
			regexExcludePattern = Pattern.compile(regexExclude, Pattern.CASE_INSENSITIVE);
		}
		if (null != regexExcludePattern) {
			return regexExcludePattern.matcher(sUrl);
		}
		else return null;
	}
	
	// Get set:
	
	public String getRegexInclude() {
		return regexInclude;
	}
	public String getRegexExclude() {
		return regexExclude;
	}
	public void setRegexInclude(String regexInclude) {
		this.regexInclude = regexInclude;
		regexIncludePattern = null;
	}
	public void setRegexExclude(String regexExclude) {
		this.regexExclude = regexExclude;
		regexExcludePattern = null;
	}

	public String getFeedType() {
		return feedType;
	}
	public void setFeedType(String feedType) {
		this.feedType = feedType;
	}
	public void setExtraUrls(List<ExtraUrlPojo> extraUrls) {
		this.extraUrls = extraUrls;
	}
	public List<ExtraUrlPojo> getExtraUrls() {
		return extraUrls;
	}
	public Integer getWaitTimeOverride_ms() {
		return waitTimeOverride_ms;
	}
	public void setWaitTimeOverride_ms(Integer waitTimeOverride_ms) {
		this.waitTimeOverride_ms = waitTimeOverride_ms;
	}
	public SourceSearchFeedConfigPojo getSearchConfig() {
		return searchConfig;
	}
	public void setSearchConfig(SourceSearchFeedConfigPojo searchConfig) {
		this.searchConfig = searchConfig;
	}
	public String getUserAgent() {
		return userAgent;
	}
	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}
	public void setUpdateCycle_secs(Integer updateCycle_secs) {
		this.updateCycle_secs = updateCycle_secs;
	}
	public Integer getUpdateCycle_secs() {
		return updateCycle_secs;
	}
}
