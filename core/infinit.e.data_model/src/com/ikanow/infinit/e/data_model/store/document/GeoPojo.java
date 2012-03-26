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
