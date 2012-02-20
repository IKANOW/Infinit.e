package com.ikanow.infinit.e.api.knowledge.processing;

import java.util.ArrayList;
import java.util.Map;

import org.elasticsearch.index.field.data.strings.StringDocFieldData;
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
					
			StringDocFieldData docfield = (StringDocFieldData)doc.get("locs");
			String[] locs = docfield.getValues();
			if ( locs.length != 0 )
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
					double currd = ((newlat - paramlat)*(newlat - paramlat)) + ((newlon - paramlon)*(newlon - paramlon)) - ontModifier;
					if ( currd < mind )
					{
						minlat = newlat;
						minlon = newlon;
						mind = currd;
						minModifier = ontModifier;
					}
				}
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
			long pubdate = doc.numeric("publishedDate").getLongValue();
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
	}
	
	/**
	 * Returns a distance modifier for the ontology type.
	 * We remove this distance from the distance calculations
	 * 
	 * @param ontCode The ontology_type we want a modifier for.
	 * @return a number of km to take off the distance calculations
	 */
	private double getOntologyDistanceModifier(char ontCode)
	{
		switch ( ontCode )
		{
			case 'p':	return 0;		//point
			case 'u':	return 10;		//city
			case 'a':	return 100;		//countrysubsidiary
			case 'c':	return 1000;	//country
			case 'C':	return 1000;	//continent
			case 'g':	return 1000;	//geographregion
		}		
		return 0;
	}
}
