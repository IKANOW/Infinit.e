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
	import com.ikanow.infinit.e.widget.library.enums.EntityMatchTypeEnum;
	import com.ikanow.infinit.e.widget.library.enums.FilterDataSetEnum;
	import com.ikanow.infinit.e.widget.library.enums.IncludeEntitiesEnum;
	import com.ikanow.infinit.e.widget.library.framework.WidgetSaveObject;
	
	import mx.collections.ArrayCollection;
	
	import system.data.Map;
	import system.data.Set;

	/**
	 * Data object for widgets.  Used to get results of queries
	 * and filter events so widgets can display new datasets.
	 */
	public interface IWidgetContext
	{
		//______________________________________________________________________________________
		//
		// DATA ACCESS
		
		/**
		 * Returns aggregations from all documents from a query (the documents themselves are not all returned,
		 * see getTopQueryResults)
		 * @return all results of a query as an IResultSet.
		 */
		function getQuery_AllResults():IResultSet;
		/**
		 * Returns the top documents from a query.
		 * Any aggregations are derived from these top documents
		 * Equivalent to getAllQueryResults().getFilteredQueryResults()
		 * 
		 * @return the top results of a query as an IResultSet.
		 */
		function getQuery_TopResults():IResultSet;
		/**
		 * If a filter has been applied to the top query results, returns those documents (and derived aggregations)
		 * Null otherwise
		 * 
		 * @return the filtered top results of a query as an IResultSet.
		 */
		function getQuery_FilteredResults():IResultSet;
		/**
		 * If a query has been saved by any of: the framework, this widget, or other widgets, returns that.
		 * Null otherwise
		 * @param queryName The name of the saved query
		 * 
		 * @return the results of a saved query.
		 */
		function getSavedQuery_AllResults(queryName:String):IResultSet;
		
		/**
		 * Returns recent queries. Although the entire IResultSet is returned, this can be used
		 * for just query request analysis/modification via IResultSet.getQueryRequest(), or the
		 * human readable descriptions can be accessed via IResultSet.getDescription() etc.
		 * @param queriesAgo: how many executed queries ago (ie 0==this query, 1==last query, etc)
		 * 
		 * @return an array collection of recent queries (including results, request and description - see IResultSet).
		 */
		function getRecentQueries(queriesAgo:int):ArrayCollection;
		
		//______________________________________________________________________________________
		//
		// FILTERING
		
		/**
		 * Sends a filter event that will be sent to all widgets (including this one)
		 * This filter event takes a set of entity names and filters documents on those names
		 * using the specified params
		 * 
		 * This call will result in a widgets onReceiveNewFilter() function to be called.
		 * 
		 * @param filterDataSet The dataset that needs filtered, either the global query
		 * data set of the filtered dataset is being further filtered
		 * @param entDisNames A set of entity names to filter on
		 * @param matchType How the data should be filtered: ALL means that a document
		 * must contain every entity in entDisNames while ANY means that a document
		 * must contain atleast 1 entity in entDisNames
		 * @param includeEntsType Determines if all the entities of a matching document
		 * should be added to the filter dataset or just the entities that were matched
		 * on: INCLUDE_ALL_ENTITIES means take all a docs entities while INCLUDE_SELECTED_ENTITIES
		 * only takes matching entities (i.e. will be a subset of entDisNames)
		 * @param filterDescription A string that will be displayed on the tooltip of the breadcrumb
		 * for this filter.  If left null a default one will be made for this filter.
		 */
		function filterByEntities(filterDataSet:FilterDataSetEnum, entDisNames:Set, matchType:EntityMatchTypeEnum, includeEntsType:IncludeEntitiesEnum, filterDescription:String=null):void;
		/**
		 * Sends a filter event that will be sent to all widgets (including this one)
		 * This filter event takes a field and a list of items to filter on.
		 * 
		 * This call will result in a widgets onReceiveNewFilter() function to be called.
		 * 
		 * @param filterDataSet The dataset that needs filtered, either the global query
		 * data set of the filtered dataset is being further filtered
		 * @param values The set of string values to search a documents field for e.g. if
		 * field is _id then values would be a list of document _id numbers to filter on
		 * @param field The field to filter a document on, defaults to _id.  This can be any
		 * field that a document contains, for example title, source, etc
		 * @param filterDescription A string that will be displayed on the tooltip of the breadcrumb
		 * for this filter.  If left null a default one will be made for this filter.
		 */ 
		function filterByDocField(filterDataSet:FilterDataSetEnum, values:Set, field:String = "_id", filterDescription:String=null):void;
		/**
		 * Sends a filter event that will be sent to all widgets (including this one)
		 * This filter event takes a field and a list of items to filter on.
		 * 
		 * This call will result in a widgets onReceiveNewFilter() function to be called.
		 * 
		 * @param filterDataSet The dataset that needs filtered, either the global query
		 * data set of the filtered dataset is being further filtered
		 * @param docValues The list of values to filter a document on, for example _id numbers
		 * @param docToEntMap The map containing the doc to entity mapping i.e. doc _id's to entity
		 * objects
		 * @param docFilterField The field to filter docValues on, defaults to _id, see filterByDocField
		 * @param docToEntityField The field that docToEntMap is mapping on, defaults to _id
		 * @param filterDescription A string that will be displayed on the tooltip of the breadcrumb
		 * for this filter.  If left null a default one will be made for this filter.
		 */
		function filterByDocFieldAndEntities(filterDataSet:FilterDataSetEnum, docValues:Set, docToEntMap:Map, docFilterField:String = "_id", docToEntityField:String = "_id", filterDescription:String=null):void;
		
		//______________________________________________________________________________________
		//
		// FRAMEWORK INTERFACE (QUERIES)

		// Query Options - Allows advanced users to do things like set the geo/time decay from widgets
		
		/**
		 * Gets the current query options (can then eg change and set it over the current query, or run it locally).
		 * For advanced developer use.
		 * @return the current query options (see REST API for details on the JSON format)
		 */
		function getCurrentQuery():Object;
		/**
		 * Gets the most recent query to be run (can then eg change and set it over the current query, or run it locally).
		 * For advanced developer use.
		 * @return the current query options (see REST API for details on the JSON format)
		 */
		function getLastQuery():Object;
		/**
		 * Gets the current query options (can then eg change and set it over the current query, or run it locally).
		 * For advanced developer use.
		 * @param queryName (optional) - the name of the query from which to retrieve the options (the last query by default/if null)
		 * @return the current query options (see REST API for details on the JSON format)
		 */
		function getSavedQuery(queryName:String = null):Object;
		
		/**
		 * Gets a list of all the saved queries
		 * For advanced developer use.
		 * @return an array collection of query names, use getSavedQuery to get the actual option
		 */
		function getSavedQueryNames():ArrayCollection;
		
		/**
		 * Sets the current query options (normally obtained from getCurrentQuery or getSavedQuery).
		 * For advanced developer use.
		 * @return the current query options (see REST API for details on the JSON format)
		 */
		function setCurrentQuery(modifiedQueryOptions:Object, modifiedElements:String = null):void;
		
		// Performing and applying queries
		
		/**
		 * Performs a local query for the widget - see parameters for more details
		 * @param queryObject The query to make (see REST API for details) - often obtained by modifying getCurrentQuery or getSavedQuery
		 * @param callback The function to be called when successful (ignored if isLocalQuery==false), needs 2 params: an IResultSet (null if fails), and a string containing an error if IResultSet is null 
		 * @param isLocalQuery If false, make the call for the framework, ie as if a user had typed it at the dimension explorer. If true, the results are local to the widget.
		 * @param saveQueryName If non-null (default: null), saves the query so that other widgets can access it via getRecentQueryOptions 
		 * 
		 * @return if a problem occurred queuing the query (the query is asynchronous, so the return value
		 */
		function doQuery(queryObject:Object, callback:Function, isLocalQuery:Boolean = true, saveQueryName:String = null):Boolean;
		
		/**
		 * Applies the specified query to either the current or all widgets (ie calls all IWidget.onReceiveNewQuery with the specified query results)
		 * @param queryResults (optional) - the name of the saved query from which to retrieve the data, the history, a local query, if null/left out the last executed query
		 * @param localOnly - by default, saved query applied only to this widgetl if false applied to all widgets
		 * 
		 * @return whether the query was found or not
		 */		
		function applySavedQuery_FromName(queryName:String = null, localOnly:Boolean = true):Boolean;
						 
		/**
		 * Applies the specified query to *all* widgets (ie calls all IWidget.onReceiveNewQuery with the specified query results)
		 * @param queryResults (optional) - the query results, eg obtained from the chain of the current IResultSet, the history, a local query, if null/left out the last executed query 
		 * @param localOnly - by default, saved query applied only to this widgetl if false applied to all widgets
		 */		
		function applySavedQuery_FromResultSet(queryResults:IResultSet = null, localOnly:Boolean = true):void;
		
		//______________________________________________________________________________________
		//
		// FRAMEWORK INTERFACE (SAVED SETTINGS)
		
		/**
		 * Allows widgets writers to tell the framework to save their settings immediately
		 * 
		 * @param id The widget save object
		 */
		function saveWidgetSettingsNow( widgetOptions:WidgetSaveObject ):void;
		
		//______________________________________________________________________________________
		//
		// FRAMEWORK INTERFACE (SOURCES AND COMMUNITIES)
		
		/**
		 * Returns an anonymous object in the Infinit.e "community" JSON format
		 * 
		 * @param id The id of the community
		 * @returns an Object representation of the community JSON
		 */
		function getCommunityById(id:String):Object;
		
		/**
		 * Returns an anonymous object in the Infinit.e "source" JSON format
		 * 
		 * @param id Either the id or the key of the source
		 * @returns an Object representation of the source JSON
		 */
		function getSourceByIdOrKey(idOrKey:String):Object;
	}
}
