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
package com.ikanow.infinit.e.data_model.api.knowledge;

public class GeoAggregationPojo implements Comparable<GeoAggregationPojo> {
	public double lat;
	public double lon;
	public Integer count = null;
	public String type = null;

	public GeoAggregationPojo()
	{
		
	}
	
	public GeoAggregationPojo(double d, double e) 
	{
		this.lat = d;
		this.lon = e;
	}

	@Override
	public int compareTo(GeoAggregationPojo that) { 
        if (this.lon < that.lon) return -1;
        if (this.lon > that.lon) return +1;
        if (this.lat < that.lat) return -1;
        if (this.lat > that.lat) return +1;
        // Count, default to 0
        int this_count = 0;
        int that_count = 0;
        if (null != this.count) {
        	this_count =  this.count;
        }
        if (null != that.count) {
        	that_count =  that.count;
        }
        if (this_count < that_count) return -1;
        if (this_count > that_count) return +1;        
        return 0;
	}
}
