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
package com.ikanow.infinit.e.widget.library.data
{
	import com.ikanow.infinit.e.widget.library.enums.EntityMatchTypeEnum;
	import com.ikanow.infinit.e.widget.library.enums.FilterDataSetEnum;
	import com.ikanow.infinit.e.widget.library.enums.IncludeEntitiesEnum;
	import com.ikanow.infinit.e.widget.library.frameworkold.QueryResults;
	import com.ikanow.infinit.e.widget.library.widget.IResultSet;
	import com.ikanow.infinit.e.widget.library.widget.IWidget;
	import com.ikanow.infinit.e.widget.library.widget.IWidgetContext;
	import com.ikanow.infinit.e.widget.library.utility.JSONDecoder;
	
	import mx.collections.ArrayCollection;
	import mx.rpc.events.FaultEvent;
	import mx.rpc.events.ResultEvent;
	import mx.rpc.http.mxml.HTTPService;

	
	import system.data.Map;
	import system.data.Set;
	import system.data.stacks.ArrayStack;
	
	public class WidgetContext_Local implements IWidgetContext
	{
		///////////////////////////////////////////////////////////////////////////////////////
		
		// State:
		
		// Contact with the framework
		private var _callingWidget:IWidget = null;
		private var _globalContext:WidgetContext = null;
		
		// Can override default result set
		//TODO ideally need some logic to ignore global context updates (but not local context updates)
		// when in override mode (will re-display data but it's a waste of CPU and may lose settings etc)
		// not immediately obvious how to do this however....
		private var _overrideContext:WidgetContext = null;
		
		// HTTP requests
		// Queue (for the moment we'll just have 1 HTTP service and Q)
		private var _serviceQueue:ArrayStack  = null;
		// HTTP service
		private var _httpService:HTTPService = null; 

		// Cached saved widget state		
		private var _widgetState = null;
		
		///////////////////////////////////////////////////////////////////////////////////////
		
		// Constructor
		
		public function WidgetContext_Local(widget:IWidget, widgetContext:WidgetContext)
		{
			_callingWidget = widget;
			_globalContext = widgetContext;
			_serviceQueue = new ArrayStack();
			_httpService = new HTTPService();
		}
		
		///////////////////////////////////////////////////////////////////////////////////////		
		
		// Local widget specific
		
		/**
		 * Performs a local query for the widget - see parameters for more details
		 * @param queryObject The query to make (see REST API for details) - often obtained by modifying getCurrentQuery or getSavedQuery
		 * @param callback The function to be called when successful (ignored if isLocalQuery==false), needs 2 params: an IResultSet (null if fails), and a string containing an error if IResultSet is null 
		 * @param isLocalQuery If false, make the call for the framework, ie as if a user had typed it at the dimension explorer. If true, the results are local to the widget.
		 * @param saveQueryName If non-null (default: null), saves the query so that other widgets can access it via getRecentQueryOptions 
		 * 
		 * @return if a problem occurred queuing the query (the query is asynchronous, so the return value
		 */
		public function doQuery(queryObject:Object, callback:Function, isLocalQuery:Boolean=true, saveQueryName:String=null):Boolean
		{
			var doQueryObject:Object = new Object();			
			doQueryObject["queryObject"] = queryObject;
			doQueryObject["callback"] = callback;
			doQueryObject["isLocalQuery"] = isLocalQuery;
			doQueryObject["saveQueryName"] = saveQueryName;
			_serviceQueue.push(doQueryObject);
			// If there's nothing ahead of me in the Q, initiate query
			if (1 == _serviceQueue.size()) {
				this.initiateQuery();
			}
			return true;
		}
		
		// Utility functions:
		
		// Queue the function (use the master to do this to take advantage of the URL, utility functions, etc etc)
		
		private function initiateQuery():void {
			var doQueryObject:Object = _serviceQueue.peek(); // (ie leave on the Q, needed on success/failure)
			if (null != doQueryObject) {
				if (doQueryObject.isLocalQuery) {
					_httpService.addEventListener(ResultEvent.RESULT, onLocalQuerySuccess);
					_httpService.addEventListener(FaultEvent.FAULT, onLocalQueryFailure);					
					if (!_globalContext.getFramework().invokeQueryEngine(doQueryObject.queryObject, _httpService)) {
						doQueryObject.callback(null, "InvokeError");
					}
				}
				else {
					// all done with this (note multiple global invocations will just overwrite each other)
					_serviceQueue.pop(); 
					_globalContext.getFramework().invokeQueryEngine(doQueryObject.queryObject);
				}
			}
		}
		
		// Success callback
		
		private function onLocalQuerySuccess(event:ResultEvent):void {
			var doQueryObject:Object = _serviceQueue.pop(); // (now take off the Q)
			var data:Object = JSONDecoder.decode(event.result as String);
			if(data.response.success.toString() == "true")
			{
				//TODO parse the results
				//var queryResults:QueryResults = new QueryResults();
				//queryResults.populateQueryResults(data, null, context);
				//TODO call the callback
				
				//TODO save the query
			}
			else { // Some failure
				doQueryObject.callback(null, data.response.message);				
			}
			// Next query:
			if (_serviceQueue.size() > 0) {
				this.initiateQuery();
			}			
		}
		
		// Failure callback
		
		private function onLocalQueryFailure(event:FaultEvent):void {
			var doQueryObject:Object = _serviceQueue.pop(); // (now take off the Q)
			doQueryObject.callback(null, "Query Fault: " + event.type + " (" + event.fault.faultCode + ")");
			// Next query:
			if (_serviceQueue.size() > 0) {
				this.initiateQuery();
			}
		}
		
		/**
		 * Applies the specified query to either the current or all widgets (ie calls all IWidget.onReceiveNewQuery with the specified query results)
		 * @param queryResults (optional) - the name of the saved query from which to retrieve the data, the history, a local query, if null/left out the last executed query
		 * @param localOnly - by default, saved query applied only to this widgetl if false applied to all widgets
		 * 
		 * @return whether the query was found or not
		 */		
		public function applySavedQuery_FromName(queryName:String = null, localOnly:Boolean=true):Boolean
		{
			// Get result set in query name:
			var queryResults:IResultSet = null;
			if (null != queryName) {
				queryResults = _globalContext.getSavedQueries().get(queryName);
				if (null == queryResults) {
					return false;
				} 
			}
			this.applySavedQuery_FromResultSet(queryResults, localOnly);
			return true;
		}
		
		/**
		 * Applies the specified query to *all* widgets (ie calls all IWidget.onReceiveNewQuery with the specified query results)
		 * @param queryResults (optional) - the query results, eg obtained from the chain of the current IResultSet, the history, a local query, if null/left out the last executed query 
		 * @param localOnly - by default, saved query applied only to this widgetl if false applied to all widgets
		 */		
		public function applySavedQuery_FromResultSet(queryResults:IResultSet = null, localOnly:Boolean=true):void
		{
			if (null == queryResults) {
				// Here need to revert back to the non-override query
				_overrideContext = null;
				
				// Then apply the global context to either this or all widgets
				if (localOnly) {
					_callingWidget.onReceiveNewQuery();
				}
				else {
					_globalContext.getFramework().applyQueryToAllWidgets(_globalContext.getQuery_AllResults());	
				}
			}
			else {
				if (localOnly) {
					// create override context
					_overrideContext = new WidgetContext(_globalContext.getFramework());
					_overrideContext.onNewQuery_internal(queryResults, null);
					_callingWidget.onReceiveNewQuery();
				}
				else {
					// First revert back to non-override context since we're about to join with the 
					// rest of the system
					_overrideContext = null;
					_globalContext.getFramework().applyQueryToAllWidgets(queryResults);				
				}
			}
		}
		
		///////////////////////////////////////////////////////////////////////////////////////		
		
		// Delegated to the global widget context (ie no local state required)
		
		public function getQuery_AllResults():IResultSet
		{
			if (null != _overrideContext) {
				return _overrideContext.getQuery_AllResults();
			}
			else {
				return _globalContext.getQuery_AllResults();
			}
		}
		
		public function getQuery_TopResults():IResultSet
		{
			if (null != _overrideContext) {
				return _overrideContext.getQuery_TopResults();
			}
			else {
				return _globalContext.getQuery_TopResults();
			}
		}
		
		public function getQuery_FilteredResults():IResultSet
		{
			if (null != _overrideContext) {
				return _overrideContext.getQuery_FilteredResults();
			}
			else {
				return _globalContext.getQuery_FilteredResults();
			}
		}
		
		public function getSavedQuery_AllResults(queryName:String):IResultSet
		{
			if (null != _overrideContext) {
				return _overrideContext.getSavedQuery_AllResults(queryName);
			}
			else {
				return _globalContext.getSavedQuery_AllResults(queryName);
			}
		}
		
		public function getRecentQueries(queriesAgo:int):ArrayCollection
		{
			if (null != _overrideContext) {
				return _overrideContext.getRecentQueries(queriesAgo);
			}
			else {
				return _globalContext.getRecentQueries(queriesAgo);
			}
		}
		
		public function filterByEntities(filterDataSet:FilterDataSetEnum, entDisNames:Set, matchType:EntityMatchTypeEnum, includeEntsType:IncludeEntitiesEnum, filterDescription:String=null):void
		{
			if (null != _overrideContext) {
				_overrideContext.filterByEntities(filterDataSet, entDisNames, matchType, includeEntsType, filterDescription);
			}
			else {
				_globalContext.filterByEntities(filterDataSet, entDisNames, matchType, includeEntsType, filterDescription);
			}
		}
		
		public function filterByDocField(filterDataSet:FilterDataSetEnum, values:Set, field:String="_id", filterDescription:String=null):void
		{
			if (null != _overrideContext) {
				_overrideContext.filterByDocField(filterDataSet, values, field, filterDescription);
			}
			else {
				_globalContext.filterByDocField(filterDataSet, values, field, filterDescription);
			}
		}
		
		public function filterByDocFieldAndEntities(filterDataSet:FilterDataSetEnum, docValues:Set, docToEntMap:Map, docFilterField:String="_id", docToEntityField:String="_id", filterDescription:String=null):void
		{
			if (null != _overrideContext) {
				_overrideContext.filterByDocFieldAndEntities(filterDataSet, docValues, docToEntMap, docFilterField, docToEntityField, filterDescription);
			}
			else {
				_globalContext.filterByDocFieldAndEntities(filterDataSet, docValues, docToEntMap, docFilterField, docToEntityField, filterDescription);
			}
		}
		
		public function getCurrentQuery():Object
		{
			if (null != _overrideContext) {
				return _overrideContext.getCurrentQuery();
			}
			else {
				return _globalContext.getCurrentQuery();
			}
		}
		
		public function getLastQuery():Object
		{
			if (null != _overrideContext) {
				return _overrideContext.getLastQuery();
			}
			else {
				return _globalContext.getLastQuery();
			}
		}
		
		public function getSavedQuery(queryName:String=null):Object
		{
			if (null != _overrideContext) {
				return _overrideContext.getSavedQuery(queryName);
			}
			else {
				return _globalContext.getSavedQuery(queryName);
			}
		}
		
		public function getSavedQueryNames():ArrayCollection
		{
			if (null != _overrideContext) {
				return _overrideContext.getSavedQueryNames();
			}
			else {
				return _globalContext.getSavedQueryNames();
			}
		}
		
		public function setCurrentQuery(modifiedQueryOptions:Object, modifiedElements:String=null):void
		{
			if (null != _overrideContext) {
				_overrideContext.setCurrentQuery(modifiedQueryOptions, modifiedElements);
			}
			else {
				_globalContext.setCurrentQuery(modifiedQueryOptions, modifiedElements);
			}
		}
		
	}
}
