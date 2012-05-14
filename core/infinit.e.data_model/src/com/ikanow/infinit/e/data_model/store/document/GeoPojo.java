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
package com.ikanow.infinit.e.data_model.store.document;

public class GeoPojo 
{
	public Double lat = null;
	final public static String lat_ = "lat";
	public Double lon = null;
	final public static String lon_ = "lon";
	
	public GeoPojo()
	{
		
	}
	public GeoPojo(double lat, double lon)
	{
		this.lat = lat;
		this.lon = lon;
	}
	public GeoPojo deepCopy()
	{
		GeoPojo gp = new GeoPojo();
		gp.lat = lat;
		gp.lon = lon;
		return gp;
	}
	/**
	 * Checks to see if any ents are outside of lat (-90:90) or lon (-180:180) bounds
	 * removes any bad ents it finds so they will not be stored.
	 */
	public static GeoPojo cleanseBadGeotag(GeoPojo geo)
	{
		if ( geo != null )
		{
			if ( geo.lat > 90 || geo.lat < -90 || geo.lon > 180 || geo.lon < -180 )
			{
				return null;
			}
		}		
		return geo;
	}
}
