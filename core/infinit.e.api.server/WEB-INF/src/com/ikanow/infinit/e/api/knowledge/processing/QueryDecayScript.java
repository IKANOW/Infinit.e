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
package com.ikanow.infinit.e.api.knowledge.processing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.elasticsearch.index.field.ScriptFieldUtils;
import org.elasticsearch.index.search.geo.GeoHashUtils;
import org.elasticsearch.script.AbstractDoubleSearchScript;
import org.elasticsearch.search.lookup.DocLookup;

//To use this script you must export a jar of infinit.e.api.server and place it 
//on the ES servers at: /usr/share/java/elasticsearch/plugins/scoringscripts/
//then restart the server using /etc/init.d/infinite-index-engine restart
public class QueryDecayScript extends AbstractDoubleSearchScript
{	
	private Map<String,Object> params;
	private static final double AVG_EARTH_RADIUS = 6371.01;
	private static final double DEGREE_IN_RADIAN = 0.0174532925;
	private static final double APPROX_KM_TO_DEGREES_SQ = 0.00008098704;
		// 1minute(==1/60th of a degree)~=1.852km at the equator
	
	public QueryDecayScript(Map<String,Object> params)
	{
		this.params = params;
	}

	@Override
	public double runAsDouble() 
	{				
		DocLookup doc = doc();
		//to speed lookup, only a single param object is sent (an array of Objects)
		//so we can cheat and just get the first value in the map and then cast our objects as
		//necessary, they are also ordered via lookup (e.g. array[0],array[1], I was hoping to 
		//squeeze a few ms more performance doing that				
		@SuppressWarnings("unchecked")
		ArrayList<Object> paramObjects = (ArrayList<Object>)params.entrySet().iterator().next().getValue();		
		
		double gfactor = 1.0;
		double tfactor = 1.0;
					
		//Geodecay
		double gdecay = (Double)paramObjects.get(0);
		if ( gdecay > 0 )
		{
			double minlat = 0.0;
			double minlon = 0.0;
			double mind = 1000000.0;
			double newlat = 0.0;
			double newlon = 0.0;
			double minModifier = 0;
					
			Object docfield = doc.get("locs");
			List<String> locs = ScriptFieldUtils.getStrings(docfield);
			if ( locs.size() != 0 )
			{
				double paramlat = (Double) paramObjects.get(1);
				double paramlon = (Double) paramObjects.get(2);				
				
				for ( String loc : locs )
				{
					//these should be in the form x#geohash where x = character for ont type geohash = es geohash
					double ontModifier = getOntologyDistanceModifier(loc.charAt(0)); //we move certain ontologies closer					

					String geohash = loc.substring(2);
					double[] locll = GeoHashUtils.decode(geohash);
					newlat = locll[0];
					newlon = locll[1];
					// very approximate, just used for comparison!
					double currdsq = ((newlat - paramlat)*(newlat - paramlat)) + ((newlon - paramlon)*(newlon - paramlon)) 
										- ontModifier*ontModifier*APPROX_KM_TO_DEGREES_SQ;
					if ( currdsq < mind )
					{
						minlat = newlat;
						minlon = newlon;
						mind = currdsq;
						minModifier = ontModifier;
					}
				}
				// THIS CODE IS CUT-AND-PASTED BELOW INTO getGeoDecay
				// (LEAVE THIS HERE AS IT'S INNER LOOP AND THERE'S NOT MUCH CODE TO COPY)
				if ( mind < 1000000.0 )
				{
					minlat = minlat*DEGREE_IN_RADIAN;
					minlon = minlon*DEGREE_IN_RADIAN;
					newlat = paramlat*DEGREE_IN_RADIAN;
					newlon = paramlon*DEGREE_IN_RADIAN;					
					mind = (Math.acos(Math.sin(minlat)*Math.sin(newlat) + Math.cos(minlat)*Math.cos(newlat)*Math.cos(minlon-newlon))*AVG_EARTH_RADIUS) - minModifier;
					//the modifier may have made the distance < 0, set to 0 if it is
					if ( mind < 0 )
						mind = 0;
				}					
				gfactor = 1.0/(1.0 + gdecay*mind);
			}			
		}
		
		//Time decay
		double tdecay = (Double)paramObjects.get(3);
		if ( tdecay > 0 )
		{
			long now = (Long)paramObjects.get(4);					
			long pubdate = ScriptFieldUtils.getLong(doc.get("publishedDate"));
			tfactor = 1.0 / (1.0 + tdecay*Math.abs(now-pubdate));			
		}
		
		//If only for decay field (param decayfield=true) return geo*time, otherwise return geo*time*score		
		
		
		if ( (Boolean)paramObjects.get(5) )
		{
			return gfactor*tfactor;
		}
		else
		{
			return this.score()*gfactor*tfactor;
		}
	}//TESTED
	
	/**
	 * Performs the distance calculation
	 * This is provided as a public static function so that
	 * it can be "software emulated" within the API when the Lucene geo facet
	 * is disabled.
	 * 
	 * NOTE THIS CODE IS A COPY/PASTE FROM ABOVE, KEEP IT THE SAME IN BOTH PLACES
	 *  
	 * @param TODO TODO
	 * @return the geo decay only
	 */
	public static double getGeoDecay(double minlat, double minlon, double paramlat, double paramlon, double gdecay, char ontCode) 
	{
		//(Only bit that isn't copy paste)
		double minModifier = getOntologyDistanceModifier(ontCode);
		
		minlat = minlat*DEGREE_IN_RADIAN;
		minlon = minlon*DEGREE_IN_RADIAN;
		double newlat = paramlat*DEGREE_IN_RADIAN;
		double newlon = paramlon*DEGREE_IN_RADIAN;					
		double mind = (Math.acos(Math.sin(minlat)*Math.sin(newlat) + Math.cos(minlat)*Math.cos(newlat)*Math.cos(minlon-newlon))*AVG_EARTH_RADIUS) - minModifier;
		//the modifier may have made the distance < 0, set to 0 if it is
		if ( mind < 0 )
			mind = 0;
		
		return 1.0/(1.0 + gdecay*mind);
	}//TESTED
	
	/**
	 * Returns a distance modifier for the ontology type.
	 * We remove this distance from the distance calculations
	 * 
	 * @param ontCode The ontology_type we want a modifier for.
	 * @return a number of km to take off the distance calculations
	 */
	private static double getOntologyDistanceModifier(char ontCode)
	{
		switch ( ontCode )
		{
			case 'p':	return 0;		//point
			case 'u':	return 10;		//city
			case 'a':	return 100;		//countrysubsidiary
			case 'c':	return 500;		//country
			case 'C':	return 1000;	//continent
			case 'g':	return 1000;	//geographregion
		}		
		return 0;
	}
}
