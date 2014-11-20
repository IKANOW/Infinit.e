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
package com.ikanow.infinit.e.harvest.enrichment.custom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.lang.WordUtils;

import com.google.gson.reflect.TypeToken;
import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.feature.geo.GeoFeaturePojo;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;


/**
 * GeoReference
 * @author cvitter
 */
public class GeoReference 
{
	// Private class variables
	private static HashMap<GeoFeaturePojo, List<GeoFeaturePojo>> _cache = new HashMap<GeoFeaturePojo, List<GeoFeaturePojo>>();
	private static GeoFeaturePojo geoInfo = null;

	/**
	 * enrichGeoInfo
	 * @param geoInfo
	 * @param exactMatchOnly
	 * @param hasGeoindex
	 * @return
	 */
	public synchronized static List<GeoFeaturePojo> enrichGeoInfo(GeoFeaturePojo geoInfo, Boolean exactMatchOnly, Boolean hasGeoindex)
	{
		return enrichGeoInfo(geoInfo, exactMatchOnly, hasGeoindex, -1); 
	}
	
	/**
	 * enrichGeoInfo
	 * @param geoInfo
	 * @param exactMatchOnly
	 * @param hasGeoindex
	 * @param nMaxReturns
	 * @return
	 */
	public synchronized static List<GeoFeaturePojo> enrichGeoInfo(GeoFeaturePojo g, Boolean exactMatchOnly, Boolean hasGeoindex, int nMaxReturns)
	{
		geoInfo = g;
		
		BasicDBObject query = null;
		DBCursor result = null;
		try 
		{
			// Get cached values and return if the geoInfo object passed in matches a cached value
			List<GeoFeaturePojo> cachedVal = _cache.get(geoInfo);
			if (null != cachedVal)  { 
				return cachedVal;
			}
			
			// Establish the collection manager object use to connect to MongoDB
			// The connection will persist for lifetime of import
			DBCollection geoDb = DbManager.getFeature().getGeo();

			// If only search_field has been provided set exactMatchOnly = true
			if ((geoInfo.getSearch_field() != null) && (geoInfo.getCity() == null) && (geoInfo.getRegion() == null) && (geoInfo.getCountry() == null))
			{
				exactMatchOnly = true;
			}
			
			// Exact match
			if (exactMatchOnly)
			{
				query = getQuery(hasGeoindex, 1);
				//DEBUG
				//System.out.println(query);
				result = getGeoReference(geoDb, query, nMaxReturns);
			}
			// Loose match, broaden/modify search on each of up to 4 attempts
			else
			{
				for (int i = 1; i <= 4; i++)
				{
					query = getQuery(hasGeoindex, i);
					if (null != query) {
						result = getGeoReference(geoDb, query, nMaxReturns);
						if (result.hasNext()) { break; }
					}
				}
			}
			
			if (result.hasNext())
			{	
				List<GeoFeaturePojo> gpl = GeoFeaturePojo.listFromDb(result, GeoFeaturePojo.listType()); 
				_cache.put(geoInfo, gpl);
				return gpl;
			}
			//No value returned, cache a null value so we don't waste time searching for this value again
			else
			{
				_cache.put(geoInfo, null);
				return null;
			}
		} 
		catch (Exception e) 
		{
			return null;
		}
	}
	

	/**
	 * getQuery
	 * @param g
	 * @param hasGeoindex
	 * @param attempt
	 * @return
	 */
	//TODO (INF-1864): running this in non-strict mode can cripple the DB since search field might not
	//be set ... at least need to cache such queries (almost always the US every time!)....
	
	private static BasicDBObject getQuery(Boolean hasGeoindex, int attempt)
	{
		BasicDBObject query = new BasicDBObject();

		// SearchField
		String searchField = (geoInfo.getSearch_field() != null) ? geoInfo.getSearch_field().toLowerCase() : null;
		
		// Cities are all lower case in the georeference collection, set toLowerCase here
		String city = (geoInfo.getCity() != null) ? geoInfo.getCity().toLowerCase() : null;
		
		// Use WordUtils.capitalize to set first char of region and country words to Upper Case
		String region = (geoInfo.getRegion() != null) ? WordUtils.capitalize(geoInfo.getRegion()) : null;
		String country = (geoInfo.getCountry() != null) ? WordUtils.capitalize(geoInfo.getCountry()) : null;
		String countryCode = geoInfo.getCountry_code();

		// If the only field sent was the search_field
		if ((searchField != null) && (city == null) && (region == null) && (country == null) && (countryCode == null))
		{
			query.put("search_field", searchField);
		}
		
		// Otherwise...
		else
		{
			switch (attempt)
			{
			case 1:
				// Set the searchField if it is null
				if (searchField == null && city != null) searchField = city.toLowerCase();
				if (searchField == null && region != null) searchField = region.toLowerCase();
				if (searchField == null && country != null) searchField = country.toLowerCase();
				
				// 
				if (searchField != null) query.put("search_field", searchField);
				if (city != null) query.put("city", city);
				if (region != null) query.put("region", region);
				if (country != null) query.put("country", country);
				if (null == searchField) { // only country code specified...
					query.put("city", new BasicDBObject(DbManager.exists_, false));
					query.put("region", new BasicDBObject(DbManager.exists_, false));
				}
				if (countryCode != null) query.put("country_code", countryCode);
				break;

			case 2:
				if (city != null)
				{
					query.put("search_field", city.toLowerCase());
					query.put("city", city);
				}
				else if (region != null)
				{
					query.put("search_field", region.toLowerCase());
					query.put("region", region);
				}
				else
				{
					query.put("search_field", country.toLowerCase());
				}

				if (country != null) query.put("country", country);
				if (countryCode != null) query.put("country_code", countryCode);
				break;

			case 3:
				if (searchField == null && region != null) searchField = region.toLowerCase();
				if (searchField == null && country != null) searchField = country.toLowerCase();
				
				if (searchField != null) query.put("search_field", searchField);
				if (region != null) query.put("region", region);
				if (country != null) query.put("country", country);
				if (countryCode != null) query.put("country_code", countryCode);
				break;
				
			default:
				if (country != null) query.put("search_field", country.toLowerCase());
				if (country != null) query.put("country", country);
				if (countryCode != null) query.put("country_code", countryCode);
				break;
			}
		}
		if (query.isEmpty()) {
			return null;
		}

		// Only return records with GeoIndex objects
		if (hasGeoindex)
		{
			BasicDBObject ne = new BasicDBObject();
			ne.append(DbManager.exists_, true);
			query.put("geoindex", ne);
		}

		return query;
	}	
	
	/**
	 * getGeoReference
	 * @param cm
	 * @param query
	 * @param nMaxReturns
	 * @return
	 */
	private static DBCursor getGeoReference(DBCollection geoDb, BasicDBObject query, int nMaxReturns)
	{
		if (nMaxReturns == -1)
		{
			return geoDb.find(query);
		}
		else
		{
			return geoDb.find(query).limit(nMaxReturns);
		}
	}

	
	public synchronized static void resetCache() {
		_cache.clear();		
	}
	
	public synchronized static void reset() {
		resetCache();
	}
	

	/**
	 * getNearestCity
	 * Get the city closest to the lat/lon pair passed in
	 * @param lat
	 * @param lon
	 * @return List<GeoReferencePojo>
	 */
	public static List<GeoFeaturePojo> getNearestCity(DBCollection geoDb, String lat, String lon)
	{
		return getNearestCities(geoDb, lat, lon, 1);
	}
	

	/**
	 * getNearestCities
	 * Get n-cities near a lat/lon pair, results returned ordered by distance from
	 * the lat/lon pair
	 * @param lat
	 * @param lon
	 * @param nMaxReturns
	 * @return List<GeoReferencePojo>
	 */
	public static List<GeoFeaturePojo> getNearestCities(DBCollection geoDb, String lat, String lon, int nMaxReturns)
	{
		try
		{
			// Create Double[] from lat, lon
			Double[] d = new Double[] { Double.parseDouble(lat) , Double.parseDouble(lon)};

			// Build query object to return the shell equivalent of:
			// db.georeference.find({geoindex : {$near : [lat.lon]}})
			BasicDBObject query = new BasicDBObject();
			BasicDBObject near = new BasicDBObject();
			near.append("$near", d);
			query.put("geoindex", near);
			
			// Perform query
			DBCursor result = geoDb.find(query).limit(nMaxReturns);
			
			// Convert results to List<GeoReferencePojo>
			List<GeoFeaturePojo> gpl = GeoFeaturePojo.listFromDb(result, new TypeToken<ArrayList<GeoFeaturePojo>>(){});			
			return gpl;
		}
		catch (Exception e)
		{
			return null;
		}
	}


}
