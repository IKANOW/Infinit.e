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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
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
import com.mongodb.BasicDBObject;

public class FeedHarvester_searchEngineSubsystem {

	private static final Logger logger = Logger.getLogger(FeedHarvester_searchEngineSubsystem.class);
	
	private int maxDocsPerCycle = 1000; // (should never exceed this, anyway...)
	
	public void generateFeedFromSearch(SourcePojo src, HarvestContext context) {

		if (context.isStandalone()) {
			maxDocsPerCycle = context.getStandaloneMaxDocs();
		}		
		
		String savedUrl = src.getUrl();
		SourceRssConfigPojo feedConfig = src.getRssConfig();		
		SourceSearchFeedConfigPojo searchConfig = feedConfig.getSearchConfig();
		String savedProxyOverride = feedConfig.getProxyOverride();
		if ((null == feedConfig) || (null == searchConfig)) {
			return;
		}
		
		UnstructuredAnalysisConfigPojo savedUAHconfig = src.getUnstructuredAnalysisConfig(); // (can be null)
		String savedUserAgent = feedConfig.getUserAgent();
		Integer savedWaitTimeOverride_ms = feedConfig.getWaitTimeOverride_ms();

		// Create a deduplication set to ensure URLs derived from the search pages don't duplicate the originals
		// (and also derived URLs)
		HashSet<String> dedupSet = new HashSet<String>();
		if (null != src.getRssConfig().getExtraUrls()) {
			Iterator<ExtraUrlPojo> itDedupUrls = src.getRssConfig().getExtraUrls().iterator();
			while (itDedupUrls.hasNext()) {
				String dedupUrl = itDedupUrls.next().url;
				dedupSet.add(dedupUrl);
			}
		}//TESTED
		
		Iterator<ExtraUrlPojo> itUrls = null;
		
		// Spider parameters used in conjunction with itUrls
		List<ExtraUrlPojo> iteratingList = null;
		List<ExtraUrlPojo> waitingList = null;
		int nIteratingDepth = 0;
		
		// (ie no URL specified, so using extra URLs as search URLs - and optionally as real URLs also)
		if ((null == savedUrl) && (null != src.getRssConfig().getExtraUrls()) && !src.getRssConfig().getExtraUrls().isEmpty()) {
			// Spider logic:
			iteratingList = src.getRssConfig().getExtraUrls();
			// (end spidering logic)
			
			itUrls = iteratingList.iterator();
			src.getRssConfig().setExtraUrls(new LinkedList<ExtraUrlPojo>());
				// (ie overwrite the original list)
		}//TESTED
		
		for (;;) { // The logic for this loop can vary...
			if (dedupSet.size() >= maxDocsPerCycle) {
				break;
			}
			String currTitle = null;
			String currFullText = null;
			String currDesc = null;
		
			if (null != itUrls) {
								
				ExtraUrlPojo urlPojo = itUrls.next(); 
				savedUrl = urlPojo.url;
				if (0 == nIteratingDepth) {
					if (null != urlPojo.title) { // Also harvest this
						src.getRssConfig().getExtraUrls().add(urlPojo);
					}
				}
				currTitle = urlPojo.title;
				currDesc = urlPojo.description;
				currFullText = urlPojo.fullText;
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
					if (dedupSet.size() >= maxDocsPerCycle) {
						break;
					}
					
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
					if (null == feedConfig.getSearchConfig().getScriptflags()) { // Set flags if necessary
						if (null == feedConfig.getSearchConfig().getExtraMeta()) {
							feedConfig.getSearchConfig().setScriptflags("dt");
						}
						else {
							feedConfig.getSearchConfig().setScriptflags("dtm");							
						}
					}
					if (null != feedConfig.getSearchConfig().getExtraMeta()) {
						dummyUAHconfig.CopyMeta(feedConfig.getSearchConfig().getExtraMeta());
					}
					dummyUAHconfig.AddMetaField("searchEngineSubsystem", Context.All, feedConfig.getSearchConfig().getScript(), "javascript", feedConfig.getSearchConfig().getScriptflags());
					src.setUnstructuredAnalysisConfig(dummyUAHconfig);
					if (null != searchConfig.getProxyOverride()) {
						feedConfig.setProxyOverride(searchConfig.getProxyOverride());
					}
					if (null != searchConfig.getUserAgent()) {
						feedConfig.setUserAgent(searchConfig.getUserAgent());
					}
					if (null != searchConfig.getWaitTimeBetweenPages_ms()) {
						// Web etiquette: don't hit the same site too often
						// (applies this value to sleeps inside UAH.executeHarvest)
						feedConfig.setWaitTimeOverride_ms(searchConfig.getWaitTimeBetweenPages_ms());
					}
					//TESTED (including RSS-level value being written back again and applied in SAH/UAH code)
					
					DocumentPojo searchDoc = new DocumentPojo();
					// Required terms:
					searchDoc.setUrl(url);
					searchDoc.setScore((double)nIteratingDepth); // (spidering param)
					// Handy terms
					if (null != src.getHarvestStatus()) {
						searchDoc.setModified(src.getHarvestStatus().getHarvested()); // the last time the source was harvested - can use to determine how far back to go
					}
					// If these exist (they won't normally), fill them:
					searchDoc.setFullText(currFullText);
					searchDoc.setDescription(currDesc);
					searchDoc.setTitle(currTitle);

					UnstructuredAnalysisHarvester dummyUAH = new UnstructuredAnalysisHarvester();
					boolean bMoreDocs = (nPage < nMaxPages - 1);
					dummyUAH.executeHarvest(context, src, searchDoc, false, bMoreDocs);
						// (the leading false means that we never sleep *before* the query, only after)
					
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
								
								if (!dedupSet.contains(linkUrl)) {
									dedupSet.add(linkUrl);
								
									String linkTitle = bsonObj.getString(DocumentPojo.title_);
									String linkDesc = bsonObj.getString(DocumentPojo.description_);
									String linkPubDate = bsonObj.getString(DocumentPojo.publishedDate_);
									String linkFullText = bsonObj.getString(DocumentPojo.fullText_);
									String spiderOut = bsonObj.getString("spiderOut");
									
									if (null != linkUrl) {
										SourceRssConfigPojo.ExtraUrlPojo link = new SourceRssConfigPojo.ExtraUrlPojo();
										link.url = linkUrl;
										link.title = linkTitle;
										link.description = linkDesc;
										link.publishedDate = linkPubDate;
										link.fullText = linkFullText;
										if ((null != itUrls) && (null != spiderOut) && spiderOut.equalsIgnoreCase("true")) { 
											// In this case, add it back to the original list for chained processing
											
											if (null == waitingList) {
												waitingList = new LinkedList<ExtraUrlPojo>();
											}
											waitingList.add(link);
												// (can't result in an infinite loop like this because we check 
											 	//  dedupSet.size() and only allow links not already in dedupSet)
											
										} //TESTED
										if (null != linkTitle) {
											
											if (null == feedConfig.getExtraUrls()) {
												feedConfig.setExtraUrls(new ArrayList<ExtraUrlPojo>(searchResults.length));
											}
											feedConfig.getExtraUrls().add(link);											
										}
										
									}
								}//(end if URL not already found)
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
				feedConfig.setWaitTimeOverride_ms(savedWaitTimeOverride_ms);
				feedConfig.setProxyOverride(savedProxyOverride);
			}			
			if (null == itUrls) {
				break;		
			}
			else if (!itUrls.hasNext()) {
				if (null != waitingList) {
					
					// Spider logic:
					if (null == searchConfig.getMaxDepth()) {
						searchConfig.setMaxDepth(2); // (default max depth is 2 hops, ie original document, link, link from link)
					}
					nIteratingDepth++;
					if (nIteratingDepth > searchConfig.getMaxDepth()) {
						break;
					}
					itUrls = waitingList.iterator();
					waitingList = null;
					// (end spider logic)
					
				} //TESTED
				else break;
				
			}//TESTED x2
			
		}//(end loop over candidate URLs)
			
	}//TESTED
}
