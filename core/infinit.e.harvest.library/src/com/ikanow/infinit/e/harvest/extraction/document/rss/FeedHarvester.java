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

import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

import com.ikanow.infinit.e.data_model.InfiniteEnums;
import com.ikanow.infinit.e.data_model.InfiniteEnums.HarvestEnum;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.ikanow.infinit.e.data_model.store.config.source.SourceRssConfigPojo;
import com.ikanow.infinit.e.data_model.store.config.source.SourceRssConfigPojo.ExtraUrlPojo;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.infinit.e.data_model.store.document.GeoPojo;
import com.ikanow.infinit.e.data_model.store.social.authentication.AuthenticationPojo;
import com.ikanow.infinit.e.harvest.HarvestContext;
import com.ikanow.infinit.e.harvest.extraction.document.HarvesterInterface;
import com.ikanow.infinit.e.harvest.extraction.document.DuplicateManager;
import com.ikanow.infinit.e.harvest.extraction.document.file.XmlToMetadataParser;
import com.ikanow.infinit.e.harvest.utils.DateUtility;
import com.ikanow.infinit.e.harvest.utils.PropertiesManager;
import com.ikanow.infinit.e.harvest.utils.TextEncryption;
import com.sun.syndication.feed.module.georss.GeoRSSModule;
import com.sun.syndication.feed.module.georss.GeoRSSUtils;
import com.sun.syndication.feed.module.georss.geometries.AbstractGeometry;
import com.sun.syndication.feed.module.georss.geometries.AbstractRing;
import com.sun.syndication.feed.module.georss.geometries.Envelope;
import com.sun.syndication.feed.module.georss.geometries.LineString;
import com.sun.syndication.feed.module.georss.geometries.LinearRing;
import com.sun.syndication.feed.module.georss.geometries.Polygon;
import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;
import com.sun.syndication.fetcher.FeedFetcher;
import com.sun.syndication.fetcher.impl.FeedFetcherCache;
import com.sun.syndication.fetcher.impl.HashMapFeedInfoCache;
import com.sun.syndication.fetcher.impl.HttpClientFeedFetcher;
import com.sun.syndication.fetcher.impl.HttpURLFeedFetcher;

public class FeedHarvester implements HarvesterInterface
{

	// List of Feeds
	private List<DocumentPojo> docsToAdd = null;
	private List<DocumentPojo> docsToUpdate = null;
	@SuppressWarnings("unused")
	private List<DocumentPojo> docsToRemove = null;
	private Set<Integer> sourceTypesCanHarvest = new HashSet<Integer>();

	private HarvestContext _context;

	// Initialize the Logger
	private static final Logger logger = Logger.getLogger(FeedHarvester.class);

	// Parameters
	PropertiesManager props = new PropertiesManager();
	
	/**
	 * Default Constructor, does nothing
	 */
	public FeedHarvester()
	{			
		sourceTypesCanHarvest.add(InfiniteEnums.FEEDS);
	}

	@Override
	protected void finalize() throws Throwable
	{

	}

	// State across a harvest
	private int nTmpHttpErrors = 0;
	private int nTmpDocsSubmitted = 0;

	public void executeHarvest(HarvestContext context, SourcePojo source, List<DocumentPojo> toAdd, List<DocumentPojo> toUpdate, List<DocumentPojo> toRemove) {
		_context = context;
		this.docsToAdd = toAdd;
		this.docsToUpdate = toUpdate;
		this.docsToRemove = toRemove;
		
		// Fill in any blank user agents at the top level, for simplicity
		if (null != props.getHarvestUserAgent()) {
			String userAgent = props.getHarvestUserAgent();			
			if (null == source.getRssConfig()) {
				source.setRssConfig(new SourceRssConfigPojo());
			}
			if (null == source.getRssConfig().getUserAgent()) { // ...But rss.userAgent doesn't:
				source.getRssConfig().setUserAgent(userAgent); // then override it
			}
			if (null != source.getRssConfig().getSearchConfig()) { // If rss.searchConfig exists...
				if (null == source.getRssConfig().getSearchConfig().getUserAgent()) { // but rss.searchConfig.userAgent doesn't:
					source.getRssConfig().getSearchConfig().setUserAgent(userAgent);
				}
			}
		} // (end default user agent logic)
		
		try 
		{
			logger.debug("Source: " + source.getUrl());

			//compile feeds from source
			processFeed(source);

			logger.debug("Doc List Size: " + this.docsToAdd.size());			
			logger.debug("Doc Update Size: " + this.docsToUpdate.size());			
		} 
		catch (Exception e)
		{
			// If an exception occurs log the error
			logger.error("Exception Message: " + e.getMessage(), e);
		}
	}

	// Set up the authentication credentials
	private AuthenticationCredentials authenticateFeed(AuthenticationPojo auth) {
		AuthenticationCredentials authenticationCredentials = null;
		// Added in the event that authentication is required for the feed
		if (auth != null) 
		{

			String decpword = new TextEncryption().decrypt(auth.getPassword());
			authenticationCredentials = new AuthenticationCredentials(auth.getUsername(), decpword);
		}

		return authenticationCredentials;
	}

	// Get the syndicated feed using rome
	private SyndFeed getFeed(SourcePojo source, String url) 
	{
		synchronized(FeedHarvester.class) { // (workaround for ROME concurrency issues: http://www.jdom.org/pipermail/jdom-interest/2008-December/016252.html)

			if (null == url) {
				url = source.getUrl();
			}
			for (int i = 0; i < 2; ++i) { // Will have 2 goes in case of failure
				// Check to see if the feed requires authentication
				if (source.getAuthentication() != null) //requires auth
				{
					try 
					{
						FeedFetcher feedFetcher = new HttpClientFeedFetcher(null, authenticateFeed(source.getAuthentication()));
						if ((null != source.getRssConfig()) && (null != source.getRssConfig().getUserAgent())) {
							feedFetcher.setUserAgent(source.getRssConfig().getUserAgent());
						}
						SyndFeed retVal = feedFetcher.retrieveFeed(new URL(this.cleanUrlStart(url)));
						if (null == retVal) {
							handleRssError(new RuntimeException("Unknown RSS error") , source);							
						}
						return retVal;
					} 
					catch (Exception e) {
						System.out.println(i + "  " + url);
						
						if (1 == i) { // else just try again
							handleRssError(e, source);
						}
					}
				}
				else //does not require auth
				{
					try 
					{
						FeedFetcherCache feedInfoCache = HashMapFeedInfoCache.getInstance();
						FeedFetcher feedFetcher = new HttpURLFeedFetcher(feedInfoCache);
						if ((null != source.getRssConfig()) && (null != source.getRssConfig().getUserAgent())) {
							feedFetcher.setUserAgent(source.getRssConfig().getUserAgent());
						}
						SyndFeed retVal = feedFetcher.retrieveFeed(new URL(this.cleanUrlStart(url)));
						if (null == retVal) {
							handleRssError(new RuntimeException("Unknown RSS error") , source);							
						}
						return retVal;
					} 
					catch (Exception e) 
					{
						if (1 == i) { // else just try again
							handleRssError(e, source);
						}
					}
				}
				
				// If still here, must have errored so sleep before trying again
				try { Thread.sleep(10000); } catch (InterruptedException e) {}
				
			} // (end get 2 goes)
		}
		return null;
	}

	// Utility for RSS extraction
	
	private void handleRssError(Exception e, SourcePojo source) {
		// Error handling:
		// - If it's a 500 or 502 or 503 or 504 then just log and carry on
		// - Otherwise, if you get the same message twice in succession then error out
		boolean bSuspendSource = false;
		String sNewMessage = e.getMessage();
		if (null != sNewMessage) {
			if (sNewMessage.matches(".*50[0234].*")) {
				// Do nothing, this is just a temporary error
			}
			else if (null != source.getHarvestStatus()) {
				String sOldMessage = source.getHarvestStatus().getHarvest_message();
				if ((null != sOldMessage) && sOldMessage.equals(sNewMessage)) {
					bSuspendSource = true;
				}
			}
		}//TESTED
		if (null != source.getUrl()) {
			_context.getHarvestStatus().update(source, new Date(), HarvestEnum.error, sNewMessage, bSuspendSource, false);
		}//TESTED
		else {
			_context.getHarvestStatus().logMessage(sNewMessage, true);
		}//TESTED
		
		// If an exception occurs log the error
		logger.error("Exception Message: " + e.getMessage(), e);		
	}
	//TESTED (temp error ignore, allow 2 identical errors before failing)
	
	// Process the feed
	private void processFeed(SourcePojo source) throws Exception {
		// Process the feed
		LinkedList<SyndFeed> feeds = new LinkedList<SyndFeed>();
		boolean bExtraUrls = ( null == source.getUrl() );
		
		if ((null != source.getUrl()) && ((null == source.getRssConfig())||(null == source.getRssConfig().getSearchConfig()))) {
			// (if the second clause is false, the URL is a search query, will process differently, inside buildFeedList)
			
			SyndFeed feed = getFeed(source, null);
			if (null != feed) {
				feeds.add(feed);
			}
		}
		else if ((null != source.getRssConfig())&&(null != source.getRssConfig().getSearchConfig()))
		{
			FeedHarvester_searchEngineSubsystem searchEngineSubsystem = new FeedHarvester_searchEngineSubsystem();
			searchEngineSubsystem.generateFeedFromSearch(source, _context);
			bExtraUrls = true;
		}//TESTED
		
		if ((null != source.getRssConfig())&&(null != source.getRssConfig().getExtraUrls())&&(null == source.getRssConfig().getSearchConfig())) { 
			// Some of these might be RSS feeds, check if title==null
			for (ExtraUrlPojo url: source.getRssConfig().getExtraUrls()) {
				if ((null == url.title) && (null != url.url)) {
					SyndFeed feed = getFeed(source, url.url);
					if (null != feed) {
						feeds.add(feed);
					}					
				}
			}
		}//TESTED

		if ( !feeds.isEmpty() || bExtraUrls ) // (second case: also have extra URLs)
		{
			// Error handling, part 1:
			this.nTmpHttpErrors = 0;
			this.nTmpDocsSubmitted = 0;

			// Extract the feed and place into the pojo
			try {
				buildFeedList(feeds, source);
			}
			catch (Exception e) {
				// Propagate upwards:
				throw e;
			}
			
			// Error handling part 2:
			// clean up
			if ((nTmpHttpErrors == this.nTmpDocsSubmitted) && (this.nTmpDocsSubmitted > 5))
			{ 
				// any time when all a decent number of feeds are errors

				logger.error("Source generates only invalid feeds: " + " http_errs=" + nTmpHttpErrors + " source=" + source.getUrl());

				if (this.nTmpDocsSubmitted < 20) {
					//harvested unsucessfully, post in mongo
					_context.getHarvestStatus().update(source, new Date(), HarvestEnum.error, "Extraction errors: redirect_errs=" + 
							"http_errs=" + nTmpHttpErrors, true, false);				
				}
				else {
					//harvested unsucessfully, post in mongo *AND DISABLE*
					_context.getHarvestStatus().update(source, new Date(), HarvestEnum.error, "Extraction errors: redirect_errs=" + 
							"http_errs=" + nTmpHttpErrors, true, true);				
				}
			}	
			else {
				//harvested successfully, post in mongo
				_context.getHarvestStatus().update(source, new Date(), HarvestEnum.in_progress, "", false, false);				
			}

		}
	}

	// Build the feed list
	@SuppressWarnings("unchecked")
	private void buildFeedList(LinkedList<SyndFeed> syndFeeds, SourcePojo source) 
	{
		// If there's a max number of sources to get per harvest, configure that here:
		long nWaitTime_ms = props.getWebCrawlWaitTime();
		long nMaxTime_ms = props.getMaxTimePerSource(); // (can't override this, too easy to break the system...)
		
		int nMaxDocsPerSource = props.getMaxDocsPerSource();
		if (_context.isStandalone()) {
			nMaxDocsPerSource = _context.getStandaloneMaxDocs();
		}		
		long nNow = new Date().getTime();
		if (null != source.getRssConfig()) {
			if (null != source.getRssConfig().getWaitTimeOverride_ms()) {
				nWaitTime_ms = source.getRssConfig().getWaitTimeOverride_ms();
			}
		}
		long nMaxDocs = Long.MAX_VALUE;
		if (nWaitTime_ms > 0) {
			nMaxDocs = 1 + nMaxTime_ms/nWaitTime_ms;
		}
		if (nMaxDocs > nMaxDocsPerSource) { // (another limit, take the smaller of the 2)
			nMaxDocs = nMaxDocsPerSource;
		}
		// Can override system settings if less:
		if ((null != source.getThrottleDocs()) && (source.getThrottleDocs() < nMaxDocs)) {
			nMaxDocs = source.getThrottleDocs();
		}
		// (end per feed configuration)
		
		
		// Add extra docs
		List<SyndEntry> tmpList = null;
		boolean bCreatedAggregateList = false;
		int nRealSyndEntries = 0;
		
		for (SyndFeed feed: syndFeeds) {
			if (0 == nRealSyndEntries) {
				tmpList = feed.getEntries();
			}
			else if (!bCreatedAggregateList) {
				bCreatedAggregateList = true;
				tmpList = new LinkedList<SyndEntry>(tmpList);
				tmpList.addAll(feed.getEntries());
			}
			else {
				tmpList.addAll(feed.getEntries());
			}
			nRealSyndEntries += feed.getEntries().size();
		}
		if (null == tmpList) {
			tmpList = new LinkedList<SyndEntry>();			
		}
		//TESTED
		
		if ((null != source.getRssConfig()) && (null != source.getRssConfig().getExtraUrls())) {
			for (ExtraUrlPojo extraUrl: source.getRssConfig().getExtraUrls()) {
				if (null == extraUrl.title) {
					continue; // (this is an RSS feed not a URL)
				}//TESTED
				SyndEntryImpl synd = new SyndEntryImpl();
				synd.setLink(extraUrl.url);
				if (null != extraUrl.description) {
					SyndContentImpl description = new SyndContentImpl();
					description.setValue(extraUrl.description);
					synd.setDescription(description);
				}
				synd.setTitle(extraUrl.title);
				if (null != extraUrl.publishedDate) {
					try {
						synd.setPublishedDate(new Date(DateUtility.parseDate(extraUrl.publishedDate)));						
					}
					catch (Exception e) {} // do nothign will use now as pub date
				}				
				tmpList.add((SyndEntry) synd);

				if (null != extraUrl.fullText) {
					SyndFeedImpl fullTextContainer = new SyndFeedImpl();
					fullTextContainer.setDescription(extraUrl.fullText);
					synd.setSource(fullTextContainer);
				}
			}
		}
		
		// Then begin looping over entries

		LinkedList<String> duplicateSources = new LinkedList<String>(); 		
		try {
			Map<String, List<SyndEntry>> urlDups = new HashMap<String, List<SyndEntry>>();
			int nSyndEntries = 0;
			for ( Object synd : tmpList )
			{				
				nSyndEntries++; // (keep count so we know we're accessing our own fake SyndEntryImpls)
				final SyndEntry entry = (SyndEntry)synd;
	
				if ( null != entry.getLink() ) //if url returns null, skip this entry
				{
					String url = entry.getLink();
					if ((nSyndEntries <= nRealSyndEntries) || (null == entry.getSource())) { // (else URL can be what it wants)
						url = this.cleanUrlStart(entry.getLink());
					}

					// Intra-source distribution logic:
					if ((null != source.getDistributionTokens()) && (null != source.getDistributionFactor())) {
						int split = Math.abs(url.hashCode()) % source.getDistributionFactor();
						if (!source.getDistributionTokens().contains(split)) {
							continue;
						}
					}//TESTED (copy and paste from FileHarvester)
					
					if (null != source.getRssConfig()) { // Some RSS specific logic
						// If an include is specified, must match
						Matcher includeMatcher = source.getRssConfig().getIncludeMatcher(url);
						if (null != includeMatcher) {
							if (!includeMatcher.find()) {
								continue;
							}
						}
						// If an exclude is specified, must not match
						Matcher excludeMatcher = source.getRssConfig().getExcludeMatcher(url);
						if (null != excludeMatcher) {
							if (excludeMatcher.find()) {
								continue;
							}
						}
					}

					// Some error checking:
					// sometimes the URL seems to have some characters in front of the HTTP - remove these
					this.nTmpDocsSubmitted++;
					if (null == url) {
						this.nTmpHttpErrors++;
						continue;
					}

					// Also save the title and description:
					String title = "";
					if (null != entry.getTitle()) {
						title = entry.getTitle();
					}
					String desc = "";
					if (null != entry.getDescription()) {
						desc = entry.getDescription().getValue();					
					}				
					boolean duplicate = false;

					// Look for duplicates within the current set of sources
					List<SyndEntry> possDups = null;
					if (null == (possDups = urlDups.get(url))) { // (new URL)
						possDups = new LinkedList<SyndEntry>();
						possDups.add(entry);
						urlDups.put(url, possDups);
					}
					else { // (old URL, check if this is a duplicate...)
						@SuppressWarnings("unused")
						int nCount = 0; // (for handy debugging)
						
						for (SyndEntry possDup : possDups) {
							if (possDup.getTitle().equals(title) || 
									((null != possDup.getDescription()) && possDup.getDescription().getValue().equals(desc)) ||
									((null != possDup.getDescription()) && (null == entry.getDescription())))
							{
								// If *either* the title or the description matches as well as the URL...
								duplicate = true;
								break;
							}
							nCount++;
						}
						
						if (!duplicate) {
							possDups.add(entry);						
						}
						else { // DUPLICATE: ensure we have minimal set of data to cover all cases: 
							boolean bTitleMatch = false;
							boolean bDescMatch = false;
							for (SyndEntry possDup : possDups) {
								if (!bTitleMatch && possDup.getTitle().equals(title)) { // (don't bother if already have a title match)
									bTitleMatch = true;
								}
								else if (!bDescMatch) { // (don't yet have a desc match(
									if (null != entry.getDescription()) {
										if (null != possDup.getDescription()) { // (neither desc is null)
											if (possDup.getDescription().getValue().equals(desc)) {
												bDescMatch = true;
											}
										}
									}
									else { // curr desc is null
										if (null == possDup.getDescription()) { // dup desc is null
											bDescMatch = true;
										}

									} // (end various title match/desc match/both have no desc cases
								} // (end if no desc match)
								if (bTitleMatch && bDescMatch) {
									break; // (no way can fire)
								}
							} // (end loop over dups)

							if (!bTitleMatch || !bDescMatch) {
								possDups.add(entry);													
							}

						} // (end is duplicate, nasty logic to add minimal set to dup list to cover all titles, descs)
					}
					if (duplicate) {
						continue;
					}

					try {					
						DuplicateManager qr = _context.getDuplicateManager();
						if (null != entry.getDescription()) {
							duplicate = qr.isDuplicate_UrlTitleDescription(url, title.replaceAll("\\<.*?\\>", "").trim(), desc.replaceAll("\\<.*?\\>", "").trim(), source, duplicateSources);
						}
						else {
							duplicate = qr.isDuplicate_UrlTitleDescription(url, title.replaceAll("\\<.*?\\>", "").trim(), null, source, duplicateSources);						
							//^^^(this is different to isDuplicate_UrlTitle because it enforces that the description be null, vs just checking the title)
						}
						if (duplicate && (null != source.getRssConfig()) && (null != source.getRssConfig().getUpdateCycle_secs())) { 
							// Check modified times...
							Date dupModDate = qr.getLastDuplicateModifiedTime();
							ObjectId dupId = qr.getLastDuplicateId();
							
							if ((null != dupModDate) && (null != dupId)) {
								if (dupModDate.getTime() + source.getRssConfig().getUpdateCycle_secs()*1000 < nNow) {
									
									DocumentPojo doc = buildDocument(url, entry, source, duplicateSources);
									if ((nSyndEntries > nRealSyndEntries) && (null != entry.getSource())) {
										// (Use dummy TitleEx to create a "fake" full text block)
										doc.setFullText(entry.getSource().getDescription());
									}
									doc.setUpdateId(dupId); // (set _id to document I'm going to overwrite) 
									this.docsToUpdate.add(doc);
									
									if ((this.docsToAdd.size() + this.docsToUpdate.size()) >= nMaxDocs) {
										source.setReachedMaxDocs();
										break; // (that's enough documents)
									}
								}
							}
						}//TESTED (duplicates we update instead of ignoring)
						
						if (!duplicate) {
							DocumentPojo doc = buildDocument(url, entry, source, duplicateSources);
							if ((nSyndEntries > nRealSyndEntries) && (null != entry.getSource())) {
								// (Use dummy TitleEx to create a "fake" full text block)
								doc.setFullText(entry.getSource().getDescription());
							}
							this.docsToAdd.add(doc);

							if ((this.docsToAdd.size() + this.docsToUpdate.size()) >= nMaxDocs) {
								source.setReachedMaxDocs();
								break; // (that's enough documents)
							}
						}
						if (this.nTmpDocsSubmitted > 20) { // (some arbitrary "significant" number)
							if (nTmpHttpErrors == this.nTmpDocsSubmitted) {
								break;
							}
						}
					} 
					catch (Exception e) {
						// If an exception occurs log the error
						logger.error("Exception Message: " + e.getMessage(), e);
					} 
				}
			} // (end loop over feeds in a syndicate)
		}
		catch (Exception e) {
			// If an exception occurs log the error
			logger.error("Exception Message: " + e.getMessage(), e);
		} 
	}

	private DocumentPojo buildDocument(String cleansedUrl, SyndEntry entry, SourcePojo source, LinkedList<String> duplicateSources) {

		// create the feed pojo
		DocumentPojo doc = new DocumentPojo();

		doc.setUrl(cleansedUrl);
		doc.setCreated(new Date());
		doc.setModified(new Date());

		// Strip out html if it is present
		if ( entry.getTitle() != null )
			doc.setTitle(entry.getTitle().replaceAll("\\<.*?\\>", "").trim());
		if ( entry.getDescription() != null )
			doc.setDescription(entry.getDescription().getValue().replaceAll("\\<.*?\\>", "").trim());
		if ( entry.getPublishedDate() != null ) {
			doc.setPublishedDate(entry.getPublishedDate());
		}
		else {
			doc.setPublishedDate(new Date());
		}

		// Clone from an existing source if we can:
		if (!duplicateSources.isEmpty() && (null == doc.getUpdateId())) { // (can't duplicate updating document)
			doc.setDuplicateFrom(duplicateSources.getFirst());
		}
		
		//GeoRSS
		GeoRSSModule geoRSSModule = GeoRSSUtils.getGeoRSS(entry); //currently does not handle <georss:circle>
		if (null != geoRSSModule)
		{
			if (null != geoRSSModule.getPosition())
			{
				double lat = geoRSSModule.getPosition().getLatitude();
				double lon = geoRSSModule.getPosition().getLongitude();
				GeoPojo gp = new GeoPojo();
				gp.lat = lat;
				gp.lon = lon;
				doc.setDocGeo(gp);
			}
			if (null != geoRSSModule.getGeometry())
			{
				AbstractGeometry ag = geoRSSModule.getGeometry();
				if(ag.getClass().equals(new LineString().getClass()))
				{ //<georss:line>
					LineString ls = ((LineString)geoRSSModule.getGeometry());
					
					double latAvg = 0.0;
					double lonAvg = 0.0;
					int length = ls.getPositionList().size();
					for (int i = 0; i < length; i ++)
					{
						latAvg += ls.getPositionList().getLatitude(i);
						lonAvg += ls.getPositionList().getLongitude(i);
					}
					latAvg = latAvg/length;
					lonAvg = lonAvg/length;
					GeoPojo gp = new GeoPojo();
					gp.lat = latAvg;
					gp.lon = lonAvg;
					doc.setDocGeo(gp);
				}
				else if (ag.getClass().equals(new Polygon().getClass())) //<georss:polygon>
				{
					Polygon poly = ((Polygon)geoRSSModule.getGeometry());
					AbstractRing ar = poly.getExterior();
					LinearRing lr = (LinearRing)ar;

					double latAvg = 0.0;
					double lonAvg = 0.0;
					int length = lr.getPositionList().size();
					for (int i = 0; i < length; i ++)
					{
						latAvg += lr.getPositionList().getLatitude(i);
						lonAvg += lr.getPositionList().getLongitude(i);
					}
					latAvg = latAvg/length;
					lonAvg = lonAvg/length;
					GeoPojo gp = new GeoPojo();
					gp.lat = latAvg;
					gp.lon = lonAvg;
					doc.setDocGeo(gp);
				}
				else if(ag.getClass().equals(new Envelope().getClass()))
				{ //<georss:box>
					Envelope env = ((Envelope)geoRSSModule.getGeometry());
					
					double latAvg = (env.getMaxLatitude()+env.getMinLatitude())/2;
					double lonAvg = (env.getMaxLongitude()+env.getMinLongitude())/2;

					GeoPojo gp = new GeoPojo();
					gp.lat = latAvg;
					gp.lon = lonAvg;
					doc.setDocGeo(gp);
				}
			}
		}// end if GeoRSS
		
		// Arbitrary other metadata:

		if (null != entry.getForeignMarkup()) {
			JSONObject rssMetadata = new JSONObject();
			
			@SuppressWarnings("unchecked")
			List<Element> fms = (List<Element>) entry.getForeignMarkup();
			for (Element fm : fms) {
				try {
					JSONObject subObj = XML.toJSONObject(new XMLOutputter().outputString(fm));
					if (1 == subObj.length()) {
						for (String name: JSONObject.getNames(subObj)) {
							rssMetadata.put(name, subObj.get(name));							
						}
					}
					else { // (this will never happen in practice?)
						rssMetadata.put(fm.getName(), subObj);
					}
				} 
				catch (JSONException e) {} // (do nothing just carry on)
			}
			if (!fms.isEmpty()) {
				doc.addToMetadata("_FEED_METADATA_", XmlToMetadataParser.convertJsonObjectToLinkedHashMap(rssMetadata));
			}
		}//TESTED (longs converted to string, eg edgar:assistantDirector from "http.www.sec.gov.archives.edgar.usgaap.rss.xml")

		return doc;
	}

	// 
	// Utility function to trim whitespace (or anything - eg URL encoded +) from the start of URLs
	//
	private String cleanUrlStart(String url) 
	{
		url = url.replaceFirst("feed://", "http://");
		if (!url.startsWith("http://") && !url.startsWith("https://")) {
			int nIndex = url.indexOf("http://");
			if (-1 == nIndex) {
				nIndex = url.indexOf("https://");
				if (-1 == nIndex) {
					return "http://" + url.trim();
				}
			}
			url = url.substring(nIndex);
		}
		return url;		
	}

	@Override
	public boolean canHarvestType(int sourceType) {
		return sourceTypesCanHarvest.contains(sourceType);
	}
}
