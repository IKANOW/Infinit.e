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
package com.ikanow.infinit.e.api.knowledge.output;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import com.ikanow.infinit.e.api.utils.PropertiesManager;
import com.ikanow.infinit.e.data_model.api.ResponsePojo;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.mongodb.BasicDBObject;
import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedOutput;

public class RssOutput {

private static final Logger logger = Logger.getLogger(RssOutput.class);
	
	public String getDocs(ResponsePojo rp) {
		// Create the feed using Rome
		SyndFeed feed = new SyndFeedImpl(); // create the feed
		String feedType = "rss_2.0";
	
		// Setup a list of feeds
		@SuppressWarnings("unchecked")
		List<BasicDBObject> docs = (List<BasicDBObject>) rp.getData();
		
		PropertiesManager props = new PropertiesManager();
		
		// Set the title of the feed
		feed.setTitle( "Infinit.e Knowledge Discovery RSS Feed" );
		feed.setDescription( "Infinit.e Search Results RSS Feed" );
		feed.setLanguage( "en-us" );
		feed.setPublishedDate( new Date( System.currentTimeMillis() ) );
		feed.setFeedType( feedType ); // set the type of your feed
		String urlRoot = props.getUrlRoot();
		if (null != urlRoot) {
			feed.setLink(urlRoot.replace("/api/", ""));
		}
		else { // feed.link needs to be specified, otherwise RSS feed will fail:
			feed.setLink("http://www.ikanow.com/#SET_YOUR_URL_ROOT");
		}
		
		// Establish the list to contain the feeds
		List<SyndEntry> entries = new ArrayList<SyndEntry>();
		
		// loop through the result set
		for ( BasicDBObject fdbo : docs) 
		{
			SyndEntry entry = new SyndEntryImpl(); // create a feed entry

			String title = fdbo.getString(DocumentPojo.title_);
			if ( title != null ) {
				entry.setTitle( title );
			}
			Date pubDate = (Date) fdbo.get(DocumentPojo.publishedDate_);
			if ( pubDate != null )
				entry.setPublishedDate( pubDate );
			
			String url = fdbo.getString(DocumentPojo.displayUrl_);
			if (null == url) {
				url = fdbo.getString(DocumentPojo.url_);
				if ((null != url) && !url.startsWith("http")) {
					url = null;
				}
			}//TESTED
			else if (!url.startsWith("http:") && !url.startsWith("https:")) {
				if (null != urlRoot) {
					Object sourceKeyObj = fdbo.get(DocumentPojo.sourceKey_);
					String sourceKey = null;
					try {
						if (sourceKeyObj instanceof String) {
							sourceKey = (String) sourceKey;
						}//(should never happen)
						else if (sourceKeyObj instanceof Collection) {
							@SuppressWarnings("rawtypes")
							Collection sourceKeyCollection = ((Collection)sourceKeyObj);
							sourceKey = (String) sourceKeyCollection.iterator().next();
						}//TESTED
						else if (sourceKeyObj instanceof String[]) {
							sourceKey = ((String[])sourceKeyObj)[0];							
						}//(should never happen)
						
						if (url.startsWith("/")) {
							url = urlRoot + "knowledge/document/file/get/" + sourceKey + url;
						}
						else {
							url = urlRoot + "knowledge/document/file/get/" + sourceKey + "/" + url;						
						}
					}
					catch (Exception e) {} // carry on...
				}//TESTED
				else {
					url = null;
				}
			}//TESTED
			if ((null == url) && (null != urlRoot)) {
				url = urlRoot + "knowledge/document/get/" + fdbo.getObjectId(DocumentPojo._id_).toString() + "?returnRawData=false";
			}//TESTED
			if (null != url) {
				entry.setLink( url );
			}
			
			String description = fdbo.getString(DocumentPojo.description_);
			if ( description != null ) {
				// Create the content for the entry
				SyndContent content = new SyndContentImpl(); // create the content of your entry
				content.setType( "text/plain" );
				content.setValue( description );
				entry.setDescription( content );
			}
			entries.add( entry );
		}//(end loop over entries)
		
		feed.setEntries( entries ); // you can add multiple entries in your feed
		
		SyndFeedOutput output = new SyndFeedOutput();
		String rss = null;
        
		try {
			rss = output.outputString(feed);		
		} catch (FeedException e) {
            e.printStackTrace(  );
            logger.error("Line: [" + e.getStackTrace()[2].getLineNumber() + "] " + e.getMessage());
        }
		return rss;				
	}
}
