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
package com.ikanow.infinit.e.processing.generic.aggregation;

import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.document.EntityPojo.Dimension;
import com.ikanow.infinit.e.data_model.store.feature.entity.EntityFeaturePojo;
import com.ikanow.infinit.e.data_model.store.feature.geo.GeoFeaturePojo;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

public class EntityGeotagAggregationUtils 
{	
	private static final Logger logger = Logger.getLogger(EntityGeotagAggregationUtils.class);	
	
	/**
	 * Takes an entity feature and checks if it does not have a geotag but is a dimension: WHERE
	 * If these conditions are met then this attempts to find a geotag for the entity.
	 * 
	 * 1st try: looks up in feature.geo table on entity.getIndex()
	 * 2nd try:
	 *    A. Trys to split entity.getIndex into 3 terms and searches feature.geo on city,region,country
	 *    B. Trys to split entity.getIndex into 2 terms and searches feature.geo on region,country
	 * 
	 * Sets the features geotag and ontology_type if a result is found
	 * 
	 * @param ent_feature The entity feature we are trying to find a geotag for
	 * 	 
	 */
	public static void addEntityGeo(EntityFeaturePojo ent_feature)
	{
		try {
			if ( ent_feature.getGeotag() == null && (Dimension.Where == ent_feature.getDimension()) )
			{
				BasicDBObject hint = new BasicDBObject("search_field", 1);
				//attempt 1, try to match on index
				String firsttry = ent_feature.getIndex().substring(0, ent_feature.getIndex().lastIndexOf("/"));
				BasicDBObject query1 = new BasicDBObject("geoindex", new BasicDBObject("$exists", true));
				query1.append("search_field", firsttry);
				DBCursor dbc1 = DbManager.getFeature().getGeo().find(query1).hint(hint);
				DBObject dbo = null;
				if (dbc1.hasNext()) dbo = dbc1.next(); // (more efficient - I think! - version of dbc1.count() == 1)
				if ( (null != dbo) && !dbc1.hasNext() ) // (ie "at least 1" && "not more than 1")
				{
					//only 1 match so we can use this
					GeoFeaturePojo gfp = GeoFeaturePojo.fromDb(dbo,GeoFeaturePojo.class);
					ent_feature.setGeotag(gfp.getGeoindex());
					//we dont know what kind of point this is so we have to guess
					if ( gfp.getCity() != null) 			ent_feature.setOntology_type("city");
					else if ( gfp.getRegion() != null )		ent_feature.setOntology_type("countrysubsidiary");
					else if ( gfp.getCountry() != null )	ent_feature.setOntology_type("country");
					else									ent_feature.setOntology_type("point");
					
					return; //we are done so return
				}
				else
				{
					//on to step 2, we attempt to attack on 2 fronts
					//the geo term can be in the form of something,something,something
					//CASE 1: city,region,country e.g. blacksburg,virginia,united states
					//CASE 2: region,country e.g. new jersey,united states
					//NOTE: this fails if something has a comma in the name, but its the best we can hope for
					String[] secondtry = firsttry.split("\\s*,\\s*");				
					if ( secondtry.length > 2 ) //CASE 1
					{
						StringBuffer sb22 = new StringBuffer("^").append(Pattern.quote(secondtry[1])).append("$");
						Pattern searchterm22 = Pattern.compile(sb22.toString(), Pattern.CASE_INSENSITIVE);
						StringBuffer sb23 = new StringBuffer("^").append(Pattern.quote(secondtry[2])).append("$");
						Pattern searchterm23 = Pattern.compile(sb23.toString(), Pattern.CASE_INSENSITIVE);
						BasicDBObject query2 = new BasicDBObject("geoindex", new BasicDBObject("$exists", true));					
						query2.append("search_field", secondtry[0].toLowerCase());
						query2.append("region", searchterm22);
						query2.append("country", searchterm23);
						DBCursor dbc2 = DbManager.getFeature().getGeo().find(query2).hint(hint);
						DBObject dbo2 = null;
						if (dbc2.hasNext()) dbo2 = dbc2.next(); //(see dbc1)
						if ( (null != dbo2) && !dbc2.hasNext() ) // (ie "at least 1" && "not more than 1")
						{
							ent_feature.setGeotag(GeoFeaturePojo.fromDb(dbo2,GeoFeaturePojo.class).getGeoindex());
							ent_feature.setOntology_type("city"); //we searched for city,region,country
							
							return; //we are done so return
						}
					}
					else if ( secondtry.length > 1 ) //CASE 2
					{
						StringBuffer sb22 = new StringBuffer("^").append(Pattern.quote(secondtry[1])).append("$");
						Pattern searchterm22 = Pattern.compile(sb22.toString(), Pattern.CASE_INSENSITIVE);
						
						BasicDBObject query2 = new BasicDBObject("geoindex", new BasicDBObject("$exists", true));					
						query2.append("search_field", secondtry[0].toLowerCase());
						query2.append("country", searchterm22);
						DBCursor dbc2 = DbManager.getFeature().getGeo().find(query2).hint(hint);
						DBObject dbo2 = null;
						if (dbc2.hasNext()) dbo2 = dbc2.next(); //(see dbc1)
						if ( (null != dbo2) && !dbc2.hasNext() ) // (ie "at least 1" && "not more than 1")
						{
							ent_feature.setGeotag(GeoFeaturePojo.fromDb(dbo2,GeoFeaturePojo.class).getGeoindex());
							ent_feature.setOntology_type("countrysubsidiary"); //we searched for region, country
							
							return; //we are done so return
						}
					}
				}
			}
		}
		catch (Exception e) {
			// If an exception occurs log the error
			logger.error("Exception Message: " + e.getMessage(), e);			
		}
	}
}
