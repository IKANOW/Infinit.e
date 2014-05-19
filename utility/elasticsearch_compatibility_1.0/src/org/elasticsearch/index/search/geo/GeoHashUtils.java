package org.elasticsearch.index.search.geo;

import org.elasticsearch.common.geo.GeoPoint;

public class GeoHashUtils {
	
	public static String encode(double lat, double lon) {
		return org.elasticsearch.common.geo.GeoHashUtils.encode(lat, lon);
	}
	
	public static double[] decode(String geohash) {
		GeoPoint x = org.elasticsearch.common.geo.GeoHashUtils.decode(geohash);
		double retVal[] = { x.getLat(), x.getLon() };
		return retVal;
	}
	
}
