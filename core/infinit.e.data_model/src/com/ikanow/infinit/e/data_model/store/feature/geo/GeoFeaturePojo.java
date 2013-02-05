/*******************************************************************************
 * Copyright 2012 The Infinit.e Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.ikanow.infinit.e.data_model.store.feature.geo;

import java.util.List;

import com.google.gson.reflect.TypeToken;
import com.ikanow.infinit.e.data_model.store.BaseDbPojo;
import com.ikanow.infinit.e.data_model.store.document.GeoPojo;

public class GeoFeaturePojo extends BaseDbPojo
{
	// Standard static function for readability
	@SuppressWarnings("unchecked")
	static public TypeToken<List<GeoFeaturePojo>> listType() { return new TypeToken<List<GeoFeaturePojo>>(){}; }

	private String search_field = null;
	private String country = null;
	private String country_code = null;
	private String city = null;
	private String region = null;
	private String region_code = null;
	private Integer population = null;
	private String latitude = null;
	private String longitude = null;
	
	private GeoPojo geoindex = null;
	
	private transient String ontology_type = null; // (Not stored in the DB, but assigned by geo-coding logic)
	
	//cburch switch geoindex to a geopojo, i dont think we need all this
//	public static class GeoIndex
//	{
//		private Double lat = null;
//		private Double lon = null;
//		/**
//		 * @param lat the lat to set
//		 */
//		public void setLat(Double lat) {
//			this.lat = lat;
//		}
//		/**
//		 * @return the lat
//		 */
//		public Double getLat() {
//			return lat;
//		}
//		/**
//		 * @param lon the lon to set
//		 */
//		public void setLon(Double lon) {
//			this.lon = lon;
//		}
//		/**
//		 * @return the lon
//		 */
//		public Double getLon() {
//			return lon;
//		}
//	}
	
	@Override
	public boolean equals(Object o) {
		GeoFeaturePojo rhs = (GeoFeaturePojo)o;
		if (null == rhs) { 
			return false;
		}
		// Otherwise.....
		// Search field
		if (null != search_field) {
			if (null == rhs.search_field) {
				return false;
			}
			else if (!search_field.equals(rhs.search_field)) {
				return false;
			}
		}
		else {
			if (null != rhs.search_field) {
				return false;
			}
		}
		// Country
		if (null != country) {
			if (null == rhs.country) {
				return false;
			}
			else if (!country.equals(rhs.country)) {
				return false;
			}
		}
		else {
			if (null != rhs.country) {
				return false;
			}
		}
		// Country code
		if (null != country_code) {
			if (null == rhs.country_code) {
				return false;
			}
			else if (!country_code.equals(rhs.country_code)) {
				return false;
			}
		}
		else {
			if (null != rhs.country_code) {
				return false;
			}
		}
		// City
		if (null != city) {
			if (null == rhs.city) {
				return false;
			}
			else if (!city.equals(rhs.city)) {
				return false;
			}
		}
		else {
			if (null != rhs.city) {
				return false;
			}
		}
		// Region
		if (null != region) {
			if (null == rhs.region) {
				return false;
			}
			else if (!region.equals(rhs.region)) {
				return false;
			}
		}
		else {
			if (null != rhs.region) {
				return false;
			}
		}
		// Region code
		if (null != region_code) {
			if (null == rhs.region_code) {
				return false;
			}
			else if (!region_code.equals(rhs.region_code)) {
				return false;
			}
		}
		else {
			if (null != rhs.region_code) {
				return false;
			}
		}
		// Population:
		if (null != population) {
			if (null == rhs.population) {
				return false;
			}
			else if (!population.equals(rhs.population)) {
				return false;
			}
		}
		else {
			if (null != rhs.population) {
				return false;
			}
		}
		// Latitude:
		if (null != latitude) {
			if (null == rhs.latitude) {
				return false;
			}
			else if (!latitude.equals(rhs.latitude)) {
				return false;
			}
		}
		else {
			if (null != rhs.latitude) {
				return false;
			}
		}
		// Longitude
		if (null != longitude) {
			if (null == rhs.longitude) {
				return false;
			}
			else if (!longitude.equals(rhs.longitude)) {
				return false;
			}
		}
		else {
			if (null != rhs.longitude) {
				return false;
			}
		}
		return true;
	}
	
	@Override
	public int hashCode() {
		int nHashCode = 0;
		if (null != search_field) {
			nHashCode += getSearch_field().hashCode();
		}
		if (null != country) {
			nHashCode += country.hashCode();
		}
		if (null != country_code) {
			nHashCode += country_code.hashCode();
		}
		if (null != city) {
			nHashCode += city.hashCode();
		}
		if (null != region) {
			nHashCode += region.hashCode();
		}
		if (null != region_code) {
			nHashCode += region_code.hashCode();
		}
		if (null != population) {
			nHashCode += population.hashCode();
		}
		if (null != latitude) {
			nHashCode += latitude.hashCode();
		}
		if (null != longitude) {
			nHashCode += longitude.hashCode();
		}
		return nHashCode;
	}
	
	
	/**
	 * @param search_field the search_field to set
	 */
	public void setSearch_field(String search_field) {
		this.search_field = search_field;
	}

	/**
	 * @return the search_field
	 */
	public String getSearch_field() {
		return search_field;
	}
	
	/**
	 * @return the country
	 */
	public String getCountry() {
		return country;
	}
	/**
	 * @param country the country to set
	 */
	public void setCountry(String country) {
		this.country = country;
	}
	/**
	 * @return the country_code
	 */
	public String getCountry_code() {
		return country_code;
	}
	/**
	 * @param country_code the country_code to set
	 */
	public void setCountry_code(String country_code) {
		this.country_code = country_code;
	}
	/**
	 * @return the city
	 */
	public String getCity() {
		return city;
	}
	/**
	 * @param city the city to set
	 */
	public void setCity(String city) {
		this.city = city;
	}
	/**
	 * @return the region
	 */
	public String getRegion() {
		return region;
	}
	/**
	 * @param region the region to set
	 */
	public void setRegion(String region) {
		this.region = region;
	}
	/**
	 * @return the region_code
	 */
	public String getRegion_code() {
		return region_code;
	}
	/**
	 * @param region_code the region_code to set
	 */
	public void setRegion_code(String region_code) {
		this.region_code = region_code;
	}
	/**
	 * @return the population
	 */
	public int getPopulation() {
		return population;
	}
	/**
	 * @param population the population to set
	 */
	public void setPopulation(int population) {
		this.population = population;
	}
	/**
	 * @return the latitude
	 */
	public String getLatitude() {
		return latitude;
	}
	/**
	 * @param latitude the latitude to set
	 */
	public void setLatitude(String latitude) {
		this.latitude = latitude;
	}
	/**
	 * @return the longitude
	 */
	public String getLongitude() {
		return longitude;
	}
	/**
	 * @param longitude the longitude to set
	 */
	public void setLongitude(String longitude) {
		this.longitude = longitude;
	}


	/**
	 * @param geoindex the geoindex to set
	 */
	public void setGeoindex(GeoPojo geoindex) {
		this.geoindex = geoindex;
	}


	/**
	 * @return the geoindex
	 */
	public GeoPojo getGeoindex() {
		return geoindex;
	}

	public String getOntology_type() {
		if (null == ontology_type) { // assign		
			if (null != city) {
				ontology_type = "city";
			}
			else if ((null != region) || (null != region_code)) {
				ontology_type = "countrysubsidiary";				
			}
			else if ((null != country) || (null != country_code)) {
				ontology_type = "country";
			}
			// else leave null, will default to "point"
		}		
		return ontology_type;
	}
}

