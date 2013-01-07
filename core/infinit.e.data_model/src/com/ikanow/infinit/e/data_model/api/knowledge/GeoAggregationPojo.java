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
		// this is always the object being added
		// that is always the object in the container
		
        if (this.lon < that.lon) return -1;
        if (this.lon > that.lon) return +1;
        if (this.lat < that.lat) return -1;
        if (this.lat > that.lat) return +1;
        
        int nCompare = this.type.compareTo(that.type);
        if (0 != nCompare) {
        	return nCompare;
        }        
        // OK if we get to here, they are the same object
        // assuming we're adding then increment that.count:
        if ((null != this.count) && (null != that.count)) {
        	that.count += this.count;
        	this.count = that.count; // (so the object I attempted to add is also updated)
        }
    	return 0;
	}//TOTEST
}
