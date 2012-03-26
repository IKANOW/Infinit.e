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

import java.io.StringWriter;
import java.util.List;

import org.apache.log4j.Logger;

import com.ikanow.infinit.e.data_model.api.ResponsePojo;
import com.mongodb.BasicDBObject;

import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.Folder;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.Point;

public class KmlOutput {
	@SuppressWarnings("unused")
	private static final Logger logger = Logger.getLogger(KmlOutput.class);
	
	/**
	 * Public function used to return ResponsePojo object as KML representation
	 * @param rp
	 * @return
	 */
	//TODO (INF-1298): Complete this code (see InfiniteMapWidget for examples, though this may want to be different, ie handle documents and aggregations?)
	@SuppressWarnings("unused")
	public String getDocs(ResponsePojo rp) {

		// Setup a list of feeds
		@SuppressWarnings("unchecked")
		List<BasicDBObject> docs = (List<BasicDBObject>) rp.getData();
		
		// Setup the Kml object used to generate the kml document
		Kml kml = new Kml();
		
		// Create the document
		Document document = kml.createAndSetDocument()
			.withName("Infinit.e KML Interface")	
			.withDescription("Infinit.e search KML representation");
		
		// Create the folder to contain the placemarks (allows us to have multiple folders
		Folder placemarksFolder = document.createAndAddFolder()
			.withName("Documents")
			.withDescription("Placemarks for the document locations in the query");
		
		// loop through the result set
		for ( BasicDBObject fdbo : docs) 
		{
			// start out by checking to see if the title is not null
			if ( fdbo.getString("title") != null ) {
				// add logic to check for entities or event
				// Add in loop to create all the placemark points
				
				String description = "";
				if ( fdbo.getString("description") != null ) 
					description = fdbo.getString("description");
									
				Point placemark = placemarksFolder.createAndAddPlacemark()
				   .withName(fdbo.getString("title")).withOpen(Boolean.TRUE)
				   .withDescription(description)
				   .createAndSetPoint().addToCoordinates(-0.126236, 51.500152);
			}
		}
		
		
		// Create a string writer to contain the kml string
		StringWriter writer = new StringWriter();
		// marshal the string writer to get a string out to the kml object
		kml.marshal(writer);
		
		// return the kml to the client
		return writer.toString();

	}
}
