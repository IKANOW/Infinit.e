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
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.ikanow.infinit.e.data_model.InfiniteEnums.ExtractorSourceLevelTransientException;
import com.ikanow.infinit.e.data_model.InfiniteEnums.ExtractorDocumentLevelException;
import com.ikanow.infinit.e.data_model.InfiniteEnums.HarvestEnum;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.ikanow.infinit.e.data_model.store.config.source.SourceRssConfigPojo;
import com.ikanow.infinit.e.data_model.store.config.source.SourceRssConfigPojo.ExtraUrlPojo;
import com.ikanow.infinit.e.data_model.store.config.source.SourceSearchFeedConfigPojo;
import com.ikanow.infinit.e.data_model.store.config.source.UnstructuredAnalysisConfigPojo;
import com.ikanow.infinit.e.data_model.store.config.source.UnstructuredAnalysisConfigPojo.Context;
import com.ikanow.infinit.e.data_model.store.config.source.UnstructuredAnalysisConfigPojo.metaField;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.infinit.e.harvest.HarvestContext;
import com.ikanow.infinit.e.harvest.HarvestController;
import com.ikanow.infinit.e.harvest.enrichment.custom.UnstructuredAnalysisHarvester;
import com.mongodb.BasicDBObject;

public class FeedHarvester_searchEngineSubsystem {

	@SuppressWarnings("unused")
	private static final Logger logger = Logger.getLogger(FeedHarvester_searchEngineSubsystem.class);
	
	private int maxDocsPerCycle = Integer.MAX_VALUE; // (should never exceed this, anyway...)
	
	public void generateFeedFromSearch(SourcePojo src, HarvestContext context, DocumentPojo docToSplit) throws Exception {

		if (context.isStandalone()) {
			maxDocsPerCycle = context.getStandaloneMaxDocs();
		}		
		// otherwise get everything and worry about max docs in the main feed harvester
		// (probably slightly less efficient than checking duplicates here, but much simpler, can 
		//  always change it later)
		
		String savedUrl = src.getUrl();
		SourceRssConfigPojo feedConfig = src.getRssConfig();		
		SourceSearchFeedConfigPojo searchConfig = feedConfig.getSearchConfig();
		String savedProxyOverride = feedConfig.getProxyOverride();
		if ((null == feedConfig) || (null == searchConfig)) {
			return;
		}
		String savedTextExtractor = src.useTextExtractor();
		String savedFeatureExtractor = src.useExtractor();
		LinkedHashMap<String, String> savedExtractorOptions = src.getExtractorOptions();
		if ((null != searchConfig.getAuthExtractor()) && searchConfig.getAuthExtractor().equals("none")) {
			searchConfig.setAuthExtractor(null);
		}
		LinkedHashMap<String, Object[]> authenticationMeta = new LinkedHashMap<String, Object[]>();

		// Now allowed to stop paginating on duplicate in success_iteration/error cases
		if ((null == src.getHarvestStatus()) || (HarvestEnum.success != src.getHarvestStatus().getHarvest_status())) {
			searchConfig.setStopPaginatingOnDuplicate(false);
		}//TESTED		
		
		UnstructuredAnalysisConfigPojo savedUAHconfig = src.getUnstructuredAnalysisConfig(); // (can be null)
		String savedUserAgent = feedConfig.getUserAgent();
		LinkedHashMap<String, String> savedHttpFields = feedConfig.getHttpFields();
		Integer savedWaitTimeOverride_ms = feedConfig.getWaitTimeOverride_ms();

		// Create a deduplication set to ensure URLs derived from the search pages don't duplicate the originals
		// (and also derived URLs)
		HashSet<String> dedupSet = new HashSet<String>();
		if (null != src.getRssConfig().getExtraUrls()) {
			Iterator<ExtraUrlPojo> itDedupUrls = src.getRssConfig().getExtraUrls().iterator();
			while (itDedupUrls.hasNext()) {
				ExtraUrlPojo itUrl = itDedupUrls.next();
				if (null != itUrl.title) {
					String dedupUrl = itUrl.url;
					dedupSet.add(dedupUrl);
					if (maxDocsPerCycle != Integer.MAX_VALUE) {
						maxDocsPerCycle++; // (ensure we get as far as adding these)
					}
				}
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
						if (maxDocsPerCycle != Integer.MAX_VALUE) {
							maxDocsPerCycle--; // (now added, can remove)
						}
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
	
				// Page limit check (see also nLinksFound/nCurrDedupSetSize inside loop)
				int nMinLinksToExitLoop = 10; // (use to check one iteration past the point at which nothing happens)
				
				// If checking vs duplicates then have a flag to exit (note: only applies to the current URL)
				boolean stopPaginating = false;
				boolean stopLinkFollowing = false; 
					// (if set to stop paginating but only link following occurs, assume this is treated like pagination, eg nextUrl sort of thing)
				
				for (int nPage = 0; nPage < nMaxPages; ++nPage) {										
					if ((dedupSet.size() >= maxDocsPerCycle) || stopPaginating) {
						if (dedupSet.size() >= maxDocsPerCycle) {
							src.setReachedMaxDocs();
						}
						break;
					}
					// Will use this to check if we reached a page limit (eg some sites will just repeat the same page over and over again)
					int nLinksFound = 0;
					int nCurrDedupSetSize = dedupSet.size();
					
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
						// Legacy -> Pipeline port
						for (metaField extraMeta: dummyUAHconfig.getMeta()) {
							if (null == extraMeta.context) { // mandatory in legacy, discarded in pipeline!
								extraMeta.context = Context.First;
							}
						}
					}
					dummyUAHconfig.setScript(feedConfig.getSearchConfig().getGlobals());
					dummyUAHconfig.AddMetaField("searchEngineSubsystem", Context.All, feedConfig.getSearchConfig().getScript(), "javascript", feedConfig.getSearchConfig().getScriptflags());
					src.setUnstructuredAnalysisConfig(dummyUAHconfig);
					if (null != searchConfig.getProxyOverride()) {
						feedConfig.setProxyOverride(searchConfig.getProxyOverride());
					}
					if (null != searchConfig.getUserAgent()) {
						feedConfig.setUserAgent(searchConfig.getUserAgent());
					}
					if (null != searchConfig.getHttpFields()) {
						feedConfig.setHttpFields(searchConfig.getHttpFields());
					}
					if (null != searchConfig.getWaitTimeBetweenPages_ms()) {
						// Web etiquette: don't hit the same site too often
						// (applies this value to sleeps inside UAH.executeHarvest)
						feedConfig.setWaitTimeOverride_ms(searchConfig.getWaitTimeBetweenPages_ms());
					}
					//TESTED (including RSS-level value being written back again and applied in SAH/UAH code)
					
					DocumentPojo searchDoc = docToSplit;
					Object[] savedMeta = null;
					if (null == searchDoc) {
						searchDoc = new DocumentPojo();
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
					}//TOTEST
					else if (null != searchDoc.getMetadata()){ 
						savedMeta = searchDoc.getMetadata().remove("searchEngineSubsystem");
							// (this is normally null)
					}//TOTEST
					UnstructuredAnalysisHarvester dummyUAH = new UnstructuredAnalysisHarvester();
					boolean bMoreDocs = (nPage < nMaxPages - 1);
					Object[] searchResults = null;
					try {
						if (null != searchConfig.getAuthExtractor()) {
							src.setUseTextExtractor(searchConfig.getAuthExtractor());
							src.setExtractorOptions(searchConfig.getAuthExtractorOptions());
							
							LinkedHashMap<String, Object[]> savedAuthMeta = searchDoc.getMetadata();
							try {
								searchDoc.setMetadata(authenticationMeta);
								HarvestController hc = (HarvestController)context;
								ArrayList<DocumentPojo> docWrapper = new ArrayList<DocumentPojo>(1);
								searchDoc.setTempSource(src);
								docWrapper.add(searchDoc);
								hc.extractTextAndEntities(docWrapper, src, false, true);
								authenticationMeta = searchDoc.getMetadata();
								
								if (null != authenticationMeta) {
									if (null == feedConfig.getHttpFields()) {
										feedConfig.setHttpFields(new LinkedHashMap<String, String>());
									}
									for (Map.Entry<String, Object[]> kv: authenticationMeta.entrySet()) {
										if (1 == kv.getValue().length) {
											if (kv.getValue()[0] instanceof String) {
												feedConfig.getHttpFields().put(kv.getKey(), kv.getValue()[0].toString());
											}
										}
									}
								}
							}
							catch (Throwable t) {
								//(do nothing)
							}
							finally {
								searchDoc.setMetadata(savedAuthMeta);
								
								src.setUseTextExtractor(savedTextExtractor); // (will be null in pipeline cases - can cause odd results in non-pipeline cases, but is consistent with older behavior, which seems safest)
								src.setExtractorOptions(savedExtractorOptions);
							}
						}//TESTED (if applying extractor options)
						dummyUAH.executeHarvest(context, src, searchDoc, false, bMoreDocs);
							// (the leading false means that we never sleep *before* the query, only after)
						searchResults = searchDoc.getMetaData().get("searchEngineSubsystem");
					}
					finally {
						if (null != savedMeta) { // (this is really obscure but handle the case where someone has created this meta field already) 
							searchDoc.getMetadata().put("searchEngineSubsystem", savedMeta);							
						}
						else if ((null != searchDoc) && (null != searchDoc.getMetadata())) {
							searchDoc.getMetadata().remove("searchEngineSubsystem");
						}
					}//TOTEST
					
					//DEBUG
					//System.out.println("NEW DOC MD: " + new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(searchDoc.getMetadata()));
					
			// Create extraUrl entries from the metadata
			
					if ((null != searchResults) && (searchResults.length > 0)) {
						for (Object searchResultObj: searchResults) {
							try {
								BasicDBObject bsonObj = (BasicDBObject)searchResultObj;
								
								// 3 fields: url, title, description(=optional)
								String linkUrl = bsonObj.getString(DocumentPojo.url_);
								
								nLinksFound++;
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
										if (!stopLinkFollowing && (null != itUrls) && (null != spiderOut) && spiderOut.equalsIgnoreCase("true")) { 
											// In this case, add it back to the original list for chained processing
						
											if (null == waitingList) {
												waitingList = new LinkedList<ExtraUrlPojo>();
											}
											waitingList.add(link);
												// (can't result in an infinite loop like this because we check 
											 	//  dedupSet.size() and only allow links not already in dedupSet)
											
										} //TESTED

										if (null != linkTitle) {
											
											boolean isDuplicate = false;
											if (!stopPaginating && searchConfig.getStopPaginatingOnDuplicate()) {
												// Quick duplicate check (full one gets done later)
												isDuplicate = context.getDuplicateManager().isDuplicate_Url(linkUrl, src, null);
											}//TESTED											
											if (!isDuplicate) {
												if (null == feedConfig.getExtraUrls()) {
													feedConfig.setExtraUrls(new ArrayList<ExtraUrlPojo>(searchResults.length));
												}
												feedConfig.getExtraUrls().add(link);
											}
											else {
												stopPaginating = true;
												if (null == feedConfig.getSearchConfig().getPageChangeRegex()) {
													stopLinkFollowing = true;
												}//TESTED											
											}//TESTED
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
					else if (0 == nPage) { //returned no links, log an error if this is page 1 and one has been saved
						Object[] onError = searchDoc.getMetaData().get("_ONERROR_");
						if ((null != onError) && (onError.length > 0) && (onError[0] instanceof String) && !(((String)(onError[0]))).isEmpty()) {
							throw new ExtractorSourceLevelTransientException("generateFeedFromSearch: _ONERROR_: " + onError[0]);					
						}
					}//TESTED

					if (context.isStandalone()) { // debug mode, will display some additional logging
						Object[] onDebug = searchDoc.getMetaData().get("_ONDEBUG_");
						if ((null != onDebug) && (onDebug.length > 0)) {
							for (Object debug: onDebug) {
								if (debug instanceof String) {
									context.getHarvestStatus().logMessage("_ONDEBUG_: " + (String)debug, true);															
								}
								else {
									context.getHarvestStatus().logMessage("_ONDEBUG_: " + new com.google.gson.Gson().toJson(debug), true);
								}
							}
						}
					}//TESTED
					
					// PAGINGATION BREAK LOGIC:
					// 1: All the links are duplicates of links already in the DB
					// 2: No new links from last page
					
					// LOGIC CASE 1: (All the links are duplicates of links already in the DB)
					 
					//(already handled above)
					
					// LOGIC CASE 2: (No new links from last page)
					
					//DEBUG
					//System.out.println("LINKS_SIZE=" + feedConfig.getExtraUrls().size());
					//System.out.println("LINKS=\n"+new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(feedConfig.getExtraUrls()));
							
					if (dedupSet.size() == nCurrDedupSetSize) { // All links were duplicate
						//DEBUG
						//System.out.println("FOUND " + nLinksFound + " vs " + nMinLinksToExitLoop + " duplicate URLs (" + nCurrDedupSetSize + ")");
						if (nLinksFound >= nMinLinksToExitLoop) { // (at least 10 found so insta-quit)
							break;							
						}
						else { // (fewer than 10 found - includ
							nMinLinksToExitLoop = 0; // (also handles the no links found case)
						}
					}//TESTED
					else {
						nMinLinksToExitLoop = 10; // (reset)
					}//TESTED
					
				}// end loop over pages
				
			}
			catch (Exception e) {
				//DEBUG
				//e.printStackTrace();
				
				if ((null == dedupSet) || dedupSet.isEmpty()) {
					throw new ExtractorSourceLevelTransientException("generateFeedFromSearch: " + e.getMessage());					
				}
				else {
					throw new ExtractorDocumentLevelException("generateFeedFromSearch: " + e.getMessage());
				}
				// (don't log since these errors will appear in the log under the source, ie more usefully)
			}//TESTED
			finally {
				// Fix any temp changes we made to the source
				src.setUnstructuredAnalysisConfig(savedUAHconfig);
				feedConfig.setUserAgent(savedUserAgent);
				feedConfig.setHttpFields(savedHttpFields);
				feedConfig.setWaitTimeOverride_ms(savedWaitTimeOverride_ms);
				feedConfig.setProxyOverride(savedProxyOverride);

				src.setUseTextExtractor(savedTextExtractor);
				src.setUseExtractor(savedFeatureExtractor);
				src.setExtractorOptions(savedExtractorOptions);				
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
