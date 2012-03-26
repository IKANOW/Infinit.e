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
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import com.ikanow.infinit.e.data_model.api.ResponsePojo;
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
		
		// Set the title of the feed
		feed.setTitle( "Infinit.e Knowledge Discovery RSS Feed" );
		feed.setDescription( "Infinit.e Search Results RSS Feed" );
		feed.setLanguage( "en-us" );
		feed.setPublishedDate( new Date( System.currentTimeMillis() ) );
		feed.setFeedType( feedType ); // set the type of your feed
		feed.setLink("http://www.ikanow.com");
		
		// Establish the list to contain the feeds
		List<SyndEntry> entries = new ArrayList<SyndEntry>();
		
		// loop through the result set
		for ( BasicDBObject fdbo : docs) 
		{
			SyndEntry entry = new SyndEntryImpl(); // create a feed entry
			
			if ( fdbo.getString("title") != null ) {
				entry.setTitle( fdbo.getString("title") );

			Date pubDate = (Date) fdbo.get("publishedDate");
			if ( pubDate != null)
				entry.setPublishedDate( pubDate );
			
			
			if ( fdbo.getString("url") != null)
				entry.setLink( fdbo.getString("url") );
			
			if ( fdbo.getString("description") != null ) {
				// Create the content for the entry
				SyndContent content = new SyndContentImpl(); // create the content of your entry
				content.setType( "text/plain" );
				content.setValue( fdbo.getString("description") );
				entry.setDescription( content );
			}
				entries.add( entry );
			}
		}
		
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
