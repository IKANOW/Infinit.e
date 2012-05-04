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
package com.ikanow.infinit.e.harvest.extraction.document.rss;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.ikanow.infinit.e.data_model.store.config.source.SourceRssConfigPojo;
import com.ikanow.infinit.e.data_model.store.config.source.SourceRssConfigPojo.ExtraUrlPojo;
import com.ikanow.infinit.e.data_model.store.config.source.SourceSearchFeedConfigPojo;
import com.ikanow.infinit.e.data_model.store.config.source.UnstructuredAnalysisConfigPojo;
import com.ikanow.infinit.e.data_model.store.config.source.UnstructuredAnalysisConfigPojo.Context;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.infinit.e.harvest.HarvestContext;
import com.ikanow.infinit.e.harvest.enrichment.custom.UnstructuredAnalysisHarvester;
import com.ikanow.infinit.e.harvest.utils.PropertiesManager;
import com.mongodb.BasicDBObject;

public class FeedHarvester_searchEngineSubsystem {

	private static final Logger logger = Logger.getLogger(FeedHarvester_searchEngineSubsystem.class);
	
	public void generateFeedFromSearch(SourcePojo src, HarvestContext context) {

		PropertiesManager props = new PropertiesManager();
		long nDefaultTimeBetweenPages_ms = props.getWebCrawlWaitTime(); 
		
		String savedUrl = src.getUrl();
		SourceRssConfigPojo feedConfig = src.getRssConfig();		
		SourceSearchFeedConfigPojo searchConfig = feedConfig.getSearchConfig();
		if ((null == feedConfig) || (null == searchConfig)) {
			return;
		}
		
		UnstructuredAnalysisConfigPojo savedUAHconfig = src.getUnstructuredAnalysisConfig(); // (can be null)
		String savedUserAgent = feedConfig.getUserAgent(); 

		Iterator<ExtraUrlPojo> itUrls = null;
		// (ie no URL specified, so using extra URLs as search URLs - and optionally as real URLs also)
		if ((null == savedUrl) && (null != src.getRssConfig().getExtraUrls()) && !src.getRssConfig().getExtraUrls().isEmpty()) {
			itUrls = src.getRssConfig().getExtraUrls().iterator();
			src.getRssConfig().setExtraUrls(new LinkedList<ExtraUrlPojo>());
		}//TESTED
		
		for (;;) { // The logic for this loop can vary...
		
			if (null != itUrls) {
				ExtraUrlPojo urlPojo = itUrls.next(); 
				savedUrl = urlPojo.url;
				if (null != urlPojo.title) { // Also harvest this
					src.getRssConfig().getExtraUrls().add(urlPojo);
				}
			}//TESTED
			
			try { // If we error out, we're probably going to abandon the entire search
				
			// We're going to loop over pages
			
			// Apply the regex to the URL for pagination, part 1
				
				int nResultOffset = 0;
				int nMaxPages = 1;
				Pattern pageChangeRegex = null;
				Matcher pageChangeRegexMatcher = null;
				if (null != feedConfig.getSearchConfig().getPageChangeRegex()) {
					pageChangeRegex = Pattern.compile(feedConfig.getSearchConfig().getPageChangeRegex(), Pattern.CASE_INSENSITIVE);
					pageChangeRegexMatcher = pageChangeRegex.matcher(savedUrl);
					nMaxPages = feedConfig.getSearchConfig().getNumPages();
					
					if (pageChangeRegexMatcher.find()) {
						String group = pageChangeRegexMatcher.group(1);
						if (null != group) {
							try {
								nResultOffset = Integer.parseInt(group);
							}
							catch (Exception e) {} // just carry on
						}
					}
					else { // URL doesn't match
						pageChangeRegexMatcher = null;					
					}//TESTED
					
				}//TESTED
	
				for (int nPage = 0; nPage < nMaxPages; ++nPage) {
					String url = savedUrl;	
					
			// Apply the regex to the URL for pagination, part 2
					
					if ((null != pageChangeRegex) && (null != feedConfig.getSearchConfig().getPageChangeReplace())) {
						int nResultStart = nPage*feedConfig.getSearchConfig().getNumResultsPerPage() + nResultOffset;
						String replace = feedConfig.getSearchConfig().getPageChangeReplace().replace("$1", Integer.toString(nResultStart));
	
						if (null == pageChangeRegexMatcher) {
							url += replace;
						}
						else {
							url = pageChangeRegexMatcher.replaceFirst(replace);
						}
					}//TESTED

					//DEBUG
					//System.out.println("URL=" + url);
					
			// Create a custom UAH object to fetch and parse the search results
			
					UnstructuredAnalysisConfigPojo dummyUAHconfig = new UnstructuredAnalysisConfigPojo();
					dummyUAHconfig.AddMetaField("searchEngineSubsystem", Context.First, feedConfig.getSearchConfig().getScript(), "javascript");
					src.setUnstructuredAnalysisConfig(dummyUAHconfig);
					if (null != searchConfig.getUserAgent()) {
						feedConfig.setUserAgent(searchConfig.getUserAgent());
					}
					
					DocumentPojo searchDoc = new DocumentPojo();
					searchDoc.setUrl(url);

					// Web etiquette: don't hit the same site too often
					if (nPage > 0) { // ie not first time 
						try {
							if (null != searchConfig.getWaitTimeBetweenPages_ms()) {
								Thread.sleep(searchConfig.getWaitTimeBetweenPages_ms());
							}
							else {
								Thread.sleep(nDefaultTimeBetweenPages_ms);							
							}
						}
						catch (Exception e) {} // Just carry on
					}
					
					UnstructuredAnalysisHarvester dummyUAH = new UnstructuredAnalysisHarvester();
					boolean bMoreDocs = (nPage < nMaxPages - 1);
					dummyUAH.executeHarvest(context, src, searchDoc, bMoreDocs);
					
					//DEBUG
					//System.out.println("NEW DOC MD: " + new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(searchDoc.getMetadata()));
					
			// Create extraUrl entries from the metadata
			
					Object[] searchResults = searchDoc.getMetaData().get("searchEngineSubsystem");
					if (null != searchResults) {
						for (Object searchResultObj: searchResults) {
							try {
								BasicDBObject bsonObj = (BasicDBObject)searchResultObj;
								
								// 3 fields: url, title, description(=optional)
								String linkUrl = bsonObj.getString(DocumentPojo.url_);
								String linkTitle = bsonObj.getString(DocumentPojo.title_);
								String linkDesc = bsonObj.getString(DocumentPojo.description_);
								String linkPubDate = bsonObj.getString(DocumentPojo.publishedDate_);
								String linkFullText = bsonObj.getString(DocumentPojo.fullText_);
								
								if ((null != linkUrl) && (null != linkTitle)) {
									SourceRssConfigPojo.ExtraUrlPojo link = new SourceRssConfigPojo.ExtraUrlPojo();
									link.url = linkUrl;
									link.title = linkTitle;
									link.description = linkDesc;
									link.publishedDate = linkPubDate;
									link.fullText = linkFullText;
									if (null == feedConfig.getExtraUrls()) {
										feedConfig.setExtraUrls(new ArrayList<ExtraUrlPojo>(searchResults.length));
									}
									feedConfig.getExtraUrls().add(link);
								}
							}
							catch (Exception e) {
								// (just carry on)
								//DEBUG
								//e.printStackTrace();
							}
						}
					}//TESTED

					//DEBUG
					//System.out.println("LINKS_SIZE=" + feedConfig.getExtraUrls().size());
					//System.out.println("LINKS=\n"+new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(feedConfig.getExtraUrls()));
				
				}// end loop over pages
				
			}
			catch (Exception e) {
				logger.error("Exception Message: " + e.getMessage(), e);
				context.getHarvestStatus().logMessage("generateFeedFromSearch: " + e.getMessage(), true);						
			}
			finally {
				// Fix any temp changes we made to the source
				src.setUnstructuredAnalysisConfig(savedUAHconfig);
				feedConfig.setUserAgent(savedUserAgent);
			}			
			if (null == itUrls) {
				break;		
			}
			else if (!itUrls.hasNext()) {
				break;
			}//TESTED x2
			
		}//(end loop over candidate URLs)
			
	}//TESTED
}
