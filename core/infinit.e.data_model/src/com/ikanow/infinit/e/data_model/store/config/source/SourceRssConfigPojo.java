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
		public String description;
	}
	private List<ExtraUrlPojo> extraUrls; 
	
// Functions:
	
	public void createIncludeExcludeRegexes() {
		if (null != regexInclude) {
			regexIncludePattern = Pattern.compile(regexInclude, Pattern.CASE_INSENSITIVE);
		}
		if (null != regexExclude) {
			regexExcludePattern = Pattern.compile(regexExclude, Pattern.CASE_INSENSITIVE);
		}
	}
	public Matcher getIncludeMatcher(String sUrl) {
		if (null == regexIncludePattern) {
			return null;
		}
		return regexIncludePattern.matcher(sUrl);
	}
	public Matcher getExcludeMatcher(String sUrl) {
		if (null == regexExcludePattern) {
			return null;
		}
		return regexExcludePattern.matcher(sUrl);
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
	}
	public void setRegexExclude(String regexExclude) {
		this.regexExclude = regexExclude;
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
}
