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
/**
 * <p>Infinit.e</p>
 *
 * <p>Copyright (c) 2011 IKANOW, llc.</p>
 * <p>http://www.ikanow.com</p>
 *
 * <p>NOTICE:  IKANOW permits you to use this this file in accordance with the terms of the license agreement
 * accompanying it.  For information about the licensing and copyright of this Plug-In please contact IKANOW, llc.
 * at support&#64;ikanow.com.</p>
 *
 * <p>http://www.ikanow.com/terms-conditions/</p>
 *
 */
package com.ikanow.infinit.e.widget.library.widget
{
	import mx.collections.ArrayCollection;
	
	/**
	 * Container for the query and filter results.
	 * Holds resulting document objects in an
	 * ArrayCollection
	 */
	public interface IResultSet
	{
		
		/**
		 * An array collection of IResultSet objects representing *either* a query and its sub-queried children
		 * *or* the top results from a query and its filtered children
		 * You can think of a query and its sub-queried children as a horizontal chain, with vertical chains dropping down from it,
		 *  the first of which is parent.getTopQueryResults()
		 * @return an array collection of IResultSets
		 */
		function getChain():ArrayCollection;
		//______________________________________________________________________________________
		//
		// METADATA
		
		/**
		 * A textual description of the contents of the result set
		 */
		function getDescription():String;
		/**
		 * If entity aggregations are available (either directly or derived from the documents), an array
		 * of entity objects (see the REST API for more details on the entity object) - null if none exist
		 * @param combine: top level only - if set will combine with any additional entities in the documents but not the aggregations
		 */
		function getEntities( combine:Boolean = false ):ArrayCollection;
		
		/**
		 * If "event" aggregations are available (either directly or derived from the documents), an array
		 * of "event" objects (see the REST API for more details on the "event" object) - null if none exist
		 * An "event" is a relationship between more than 1 entity of a transient nature (eg "person visits place")
		 * @param combine: top level only - if set will combine with any additional events in the documents but not the aggregations
		 */
		function getEvents( combine:Boolean = false ):ArrayCollection;
		
		//_________________
		
		// Object Aggregations
		
		/**
		 * If "event timeline" aggregations are available (either directly or derived from the documents), an array
		 * of "event" objects (see the REST API for more details on the "event" object) - null if none exist
		 * An "event" is a relationship between more than 1 entity of a transient nature (eg "person visits place")
		 * The "event timeline" aggregates events and summaries by day, and facts over the entire timerange.
		 * @param combine: top level only - if set will combine with any additional events in the documents but not the aggregations
		 */
		function getEventsTimeline( combine:Boolean = false ):ArrayCollection;
		/**
		 * If "fact" aggregations are available (either directly or derived from the documents), an array
		 * of "fact" objects (see the REST API for more details on the "fact" object) - null if none exist
		 * A "fact" is a relationship between more than 1 entity with some degree of persistence (eg "person works for company")
		 * @param combine: top level only - if set will combine with any additional facts in the documents but not the aggregations
		 */
		function getFacts( combine:Boolean = false ):ArrayCollection;
		
		/**
		 * If a filter has been applied to the original query, returns the corresponding IResultSet (null otherwise)
		 * @return The filtered IResultSet, if it exists
		 */
		function getFilteredQueryResults():IResultSet;
		
		//_________________
		
		// Counts
		
		/**
		 * If available (either directly from the query, or from the documents), an array collection of
		 * geotags (format: {lat:number, lon:number, count:number}) ordered by lat/long.
		 * Note that due to the granularity of the geohash used to aggregate lat/longs, distinct elements can have the
		 * same lat/long - since they will always be adjacent in the array, they can simply be summed.
		 */
		function getGeoCounts():ArrayCollection;
		/**
		 * For "geo-counts" (See getGeoCounts), the maximum count in the query (since unlike other counts, geo is ordered by lat/long)
		 */
		function getGeoMaxCount():int;
		/**
		 * For "geo-counts" (See getGeoCounts), the minimum count in the query (since unlike other counts, geo is ordered by lat/long)
		 */
		function getGeoMinCount():int;
		
		/**
		 * For "moments" (See getMoments), the interval over which documents are aggregated, in seconds.
		 */
		function getMomentInterval():Number;
		
		/**
		 * If available (either directly from the query, or from the documents), an array collection of
		 * "moments" (see REST API for more details on the moment object), every "getMomentInterval()" seconds apart
		 */
		function getMoments():ArrayCollection;
		
		/**
		 * If this IResultSet is the result of a filter, returns the parent IResultSet
		 * @return - the parent IResultSet
		 */
		function getParentQueryResults():IResultSet;
		
		/**
		 * The position of the current IResultSet object in the getChain() array collection
		 * @return the index of the current query in the filter chain (see getChain)
		 */
		function getPositionInChain():int;
		
		/**
		 * The query object used to generated the results set (null if this IResultSet generated from a filter)
		 * See the REST API documentation for more details on the query object format.
		 */
		function getQueryObject():Object;
		
		/**
		 * Returns the number of results matching the query
		 * @return number of results in this query
		 */
		function getQuerySetSize():Number;
		
		/**
		 * If available (either directly from the query, or from the documents), an array collection of
		 * {source_key:string, count:number}, ordered by count
		 * @param combine: top level only - if set will combine with any additional source keys from the documents but not the aggregations
		 */
		function getSourceKeyCounts( combine:Boolean = false ):ArrayCollection;
		/**
		 * If available (either directly from the query, or from the documents), an array collection of
		 * {source_tag:string, count:number}, ordered by count
		 * @param combine: top level only - if set will combine with any additional source tags in the documents but not the aggregations
		 */
		function getSourceTagCounts( combine:Boolean = false ):ArrayCollection;
		/**
		 * If available (either directly from the query, or from the documents), an array collection of
		 * {source_type:string, count:number}, ordered by count
		 * @param combine: top level only - if set will combine with any additional source types from the documents but not the aggregations
		 */
		function getSourceTypeCounts( combine:Boolean = false ):ArrayCollection;
		/**
		 * For "time counts" (See getTimeCounts), the interval over which documents are aggregated, in seconds.
		 */
		function getTimeCountInterval():Number;
		
		
		/**
		 * If available (either directly from the query, or from the documents), an array collection of
		 * {start_time:number, count:number}, every "getTimeCountInterval()" seconds apart and ordered by count
		 */
		function getTimeCounts():ArrayCollection;
		
		//______________________________________________________________________________________
		//
		// DATA ACCESS
		
		/**
		 * Returns the ArrayCollection holding the top document objects from the query (or filter) - null if none exist
		 * See the REST API documentation for more details on the document object and its various children (events, entities).
		 */
		function getTopDocuments():ArrayCollection;
		
		//______________________________________________________________________________________
		//
		// FILTERING
		
		/**
		 * Returns the IResultSet backed by the top N documents (null if no documents returned by the query)
		 * See the REST API documentation for more details on the document object and its various children (events, entities).
		 * @return The IResultSet, if documents have been returned for the query
		 */
		function getTopQueryResults():IResultSet;
		
		/**
		 * Developers can specify their own aggregations using the ElasticSearch "facet" interface - this object
		 * is a map (vs the string specified in the request) of ArrayCollections of term/counts, ordered by count.
		 */
		function getUserAggregations():Object;
	}
}
