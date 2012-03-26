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
package com.ikanow.infinit.e.widget.library.data
{	
	import com.ikanow.infinit.e.widget.library.widget.IResultSet;
	import com.ikanow.infinit.e.widget.library.data.ResultSet;
	import com.ikanow.infinit.e.widget.library.enums.EntityMatchTypeEnum;
	import com.ikanow.infinit.e.widget.library.enums.FilterDataSetEnum;
	import com.ikanow.infinit.e.widget.library.enums.IncludeEntitiesEnum;
	import com.ikanow.infinit.e.widget.library.framework.InfiniteMaster;
	
	import mx.collections.ArrayCollection;
	import mx.controls.Alert;
	import mx.utils.ObjectUtil;
	
	import system.data.Iterator;
	import system.data.Map;
	import system.data.Set;
	import system.data.maps.HashMap;
	import com.ikanow.infinit.e.widget.library.widget.IResultSet;
	import com.ikanow.infinit.e.widget.library.widget.IWidgetContext;
	import com.ikanow.infinit.e.widget.library.frameworkold.QueryResults;
	
	/**
	 * @private
	 * Extends the WidgetContext class to let setting the filter
	 * and query results.  This class also does the grunt work of filtering.
	 */ 
	public class WidgetContext implements IWidgetContext
	{
		//______________________________________________________________________________________
		//
		// STATE

		private var _lastQueryResults:IResultSet = null;
		private var _lastQuery:Object = null; // (cache of _lastQueryResults.getQueryObject)
		private var _savedQueryResultsMap:HashMap = new HashMap(); // any saved queries
		public function getSavedQueries():HashMap { return _savedQueryResultsMap; }
		
		private var _currFilterResults:IResultSet = null;
		
		private var _nRecentQueriesToKeep:int = 10; // keep last 10 queries
		private var _recentQueryResults:ArrayCollection = new ArrayCollection(); // last few queries (results)
		public function getRecentQueryResultsList():ArrayCollection { return _recentQueryResults; }
				
		private var infinite_parent:InfiniteMaster = null;
		public function getFramework():InfiniteMaster { return infinite_parent; }
		
		//______________________________________________________________________________________
		//
		// CONSTRUCTOR
		
		/**
		 * Constructor takes the infinite_master parent so it can
		 * send filters up for legacy widget types.
		 * 
		 * @param _infinite_parent The instance of the parent page InfiniteMaster
		 */
		public function WidgetContext( _infinite_parent:InfiniteMaster = null )
		{
			infinite_parent = _infinite_parent;
		}
		
		//______________________________________________________________________________________
		//
		// DATA ACCESS
		
		/**
		 * Returns aggregations from all documents from a query (the documents themselves are not all returned,
		 * see getTopQueryResults)
		 * @return all results of a query as an IResultSet.
		 */
		public function getQuery_AllResults():IResultSet {
			return _lastQueryResults;
		}
		/**
		 * Returns the top documents from a query.
		 * Any aggregations are derived from these top documents
		 * Equivalent to getAllQueryResults().getFilteredQueryResults()
		 * 
		 * @return the top results of a query as an IResultSet.
		 */
		public function getQuery_TopResults():IResultSet {
			return _lastQueryResults.getTopQueryResults();
		}
		/**
		 * If a filter has been applied to the top query results, returns those documents (and derived aggregations)
		 * Null otherwise
		 * 
		 * @return the filtered top results of a query as an IResultSet.
		 */
		public function getQuery_FilteredResults():IResultSet {
			return _currFilterResults;
		}
		/**
		 * If a query has been saved by any of: the framework, this widget, or other widgets, returns that.
		 * Null otherwise
		 * @param queryName The name of the saved query
		 * 
		 * @return the results of a saved query.
		 */
		public function getSavedQuery_AllResults(queryName:String):IResultSet {
			return _savedQueryResultsMap.get(queryName);
		}
		
		/**
		 * Returns recent queries. Although the entire IResultSet is returned, this can be used
		 * for just query request analysis/modification via IResultSet.getQueryRequest(), or the
		 * human readable descriptions can be accessed via IResultSet.getDescription() etc.
		 * 
		 * @return an array collection of recent queries (including results, request and description - see IResultSet).
		 */
		public function getRecentQueries(queriesAgo:int):ArrayCollection {			
			return _recentQueryResults;
		}
		
		//______________________________________________________________________________________
		//
		// FILTERING
				
		public function filterByEntities(filterDataSet:FilterDataSetEnum, entDisNames:Set, matchType:EntityMatchTypeEnum, includeEntsType:IncludeEntitiesEnum, filterDescription:String=null):void 
		{
			var docSet:ArrayCollection = getDataSetToApplyFilterTo(filterDataSet); 
			var filteredDocs:ArrayCollection = new ArrayCollection();
			var filterBoolString:String;
			for each(var doc:Object in docSet )
			{
				if ( matchType == EntityMatchTypeEnum.ANY && hasMatch(entDisNames, new ArrayCollection(doc.entities))) //send doc if atleast 1 ent matches
				{
					filterBoolString = "OR";
					if ( includeEntsType == IncludeEntitiesEnum.INCLUDE_SELECTED_ENTITIES ) //include only ents in entDisName, create new doc object
					{						
						var newDocObj:Object = ObjectUtil.copy(doc);
						newDocObj.entities = getMatchingEnts(doc.entities,entDisNames);
						newDocObj.associations = getMatchingAssocs(doc.associations,entDisNames);
						filteredDocs.addItem(newDocObj);
					}
					else //include all ents (just send normal doc object)
					{
						filteredDocs.addItem(doc);
					}
				}
				else if ( hasMatch_AND(entDisNames, new ArrayCollection(doc.entities) ) ) //send docs if they contain all ents
				{
					filterBoolString = "AND";
					if ( includeEntsType == IncludeEntitiesEnum.INCLUDE_SELECTED_ENTITIES ) //include only ents in entDisName, create new doc object
					{
						var newDocObj1:Object = ObjectUtil.copy(doc);
						newDocObj1.entities = getMatchingEnts(doc.entities,entDisNames);
						newDocObj1.associations = getMatchingAssocs(doc.associations,entDisNames);
						filteredDocs.addItem(newDocObj1);
					}
					else //include all ents (just send normal doc object)
					{
						filteredDocs.addItem(doc);
					}
				}
			}	
			//do work to create filtering message
			var filterEntities:String = "";
			var i:int = 0;
			var iter:Iterator = entDisNames.iterator();
			while ( iter.hasNext() )
			{
				var entName:String = iter.next();
				if ( i == 0 )
					filterEntities += entName;	
				else
					filterEntities += " " + filterBoolString + " " + entName;
				i++;
			}
			if ( null == filterDescription ) {
				filterDescription = "Entity Filter: Documents containing: " + filterEntities;
			}
			_currFilterResults = ResultSet.createFilteredDocumentSet(_currFilterResults, filteredDocs, filterDescription);
			
			this.sendFilter();//sends call off to infinite to let all modules know filter is done
		}
		
		public function filterByDocField(filterDataSet:FilterDataSetEnum, values:Set, field:String = "_id", filterDescription:String=null):void
		{
			var docSet:ArrayCollection = getDataSetToApplyFilterTo(filterDataSet); 			
			var filteredDocs:ArrayCollection = new ArrayCollection();
			for each(var doc:Object in docSet )
			{
				if(values.contains(doc[field]))
				{
					filteredDocs.addItem(doc);
				}
			}
			//do work to create filtering message
			var desc:String = "";
			var i:int = 0;
			var it:Iterator = values.iterator();
			while (it.hasNext() )					
			{
				if ( i == 0 )
				{
					desc += it.next();
				}
				else
				{
					desc += " OR " + it.next();
				}
				i++;
			}	
			if ( null == filterDescription )
				filterDescription = ("Document Field Filter: Field: " + field + " Values: " + desc);
			
			_currFilterResults = ResultSet.createFilteredDocumentSet(_currFilterResults, filteredDocs, filterDescription);
			this.sendFilter();//sends call off to infinite to let all modules know filter is done
		}	
				
		public function filterByDocFieldAndEntities(filterDataSet:FilterDataSetEnum, docValues:Set, docToEntMap:Map, docFilterField:String = "_id", docToEntityField:String = "_id", filterDescription:String=null):void
		{
			var docSet:ArrayCollection = getDataSetToApplyFilterTo(filterDataSet);
			var filteredDocs:ArrayCollection = new ArrayCollection();
			for each(var doc:Object in docSet )
			{
				if(docValues.contains(doc[docFilterField]))
				{
					var docCopy:Object = ObjectUtil.copy(doc);					
					docCopy.entities = new Array();
					//now only add specific ents to these docs
					var entSet:Set = docToEntMap.get(doc[docToEntityField]);
					for each (var ent:Object in doc.entities )
					{
						//look in map for doc[docToEntField] i.e. doc[_id] and get that set
						//check if set contains this entity, if so add it						
						if ( entSet != null && entSet.contains(ent.index) ) 
						{
							//add ent to docCopy
							docCopy.entities.push(ent);
						}
					}
					filteredDocs.addItem(docCopy);
				}
			}
			if ( null == filterDescription )
				filterDescription = ("Document Field and Entity Filter");	
			_currFilterResults = ResultSet.createFilteredDocumentSet(_currFilterResults, filteredDocs, filterDescription);
			this.sendFilter();//sends call off to infinite to let all modules know filter is done
		}
		
		private function sendFilter():void
		{					
			if ( infinite_parent != null )
			{
				infinite_parent.parentFlagFilterEvent();
				infinite_parent.parentReceiveSelectedItem(createLegacyFilter());
			}
		}
		
		private function createLegacyFilter():SelectedItem
		{
			var si:SelectedItem = new SelectedItem();
			var docs:ArrayCollection = _currFilterResults.getTopDocuments();
			for each ( var doc:Object in docs )
			{
				var ent_names:ArrayCollection = new ArrayCollection();
				if ( doc.entities != null )
				{
					for each (var ent:Object in doc.entities) {
						ent_names.addItem(ent.index);						
					} 
				}
				si.addSelectedInstance(new SelectedInstance(doc._id,ent_names));
			}
			si.setDescription(_currFilterResults.getDescription());			
			return si;
		}
		
		private function getDataSetToApplyFilterTo(filterDataSet:FilterDataSetEnum):ArrayCollection
		{
			//TODO: probably more complicated than this since this would also imply a change in _currFilterResults
			// (ie back up to the top level)
			if ( filterDataSet == FilterDataSetEnum.FILTER_GLOBAL_DATA )
				return _lastQueryResults.getTopDocuments();
			else
				return _currFilterResults.getTopDocuments();
		}
		
		public function filterLegacy(selectedItem:SelectedItem):void
		{			
			var docSet:ArrayCollection = _lastQueryResults.getTopDocuments(); // (legacy filters always operate on the global dataset)
			var filteredDocs:ArrayCollection = new ArrayCollection();
			//loop through all instances
			for each ( var inst:SelectedInstance in selectedItem.getSelectedInstances())
			{
				//cast to selected instance and find in original data
				for each(var doc:Object in docSet)
				{
					if ( doc._id.toString() == inst.getfeedID() )
					{
						var newFeedObj:Object = ObjectUtil.copy(doc);
						//newFeedObj._id = doc._id;
						var oldEnts:Array = doc.entities;
						newFeedObj.entities = new Array();
						//loop through this instances entities and only add matches to the new feed obj
						for each ( var entName:Object in inst.getEntities())
						{
							for each ( var ent:Object in oldEnts )
							{
								if ( entName.toString() == ent.index )
								{
									newFeedObj.entities.push(ent);
									break;
								}
							}									 
						}
						//add matches to currData
						filteredDocs.addItem(newFeedObj);
						break;
					}
				}						
			}	
			_currFilterResults = ResultSet.createFilteredDocumentSet(_currFilterResults, filteredDocs, selectedItem.getDescription());
			this.sendFilter();//sends call off to infinite to let all modules know filter is done
		}
		
		// (utility function)
		private function hasMatch(entNames:Set, feedEnts:ArrayCollection):Boolean
		{
			if (null != feedEnts) 
			{
				for each ( var ent:Object in feedEnts)
				{
					if (entNames.contains(ent.index)) 
					{
						return true;
					}
				}				
			}
			return false;
		}
		
		// (utility function)
		private function hasMatch_AND(entNames:Set, feedEnts:ArrayCollection):Boolean
		{
			var nfound:int = 0;
			if (null != feedEnts) {
				for each ( var ent:Object in feedEnts)
				{
					if (entNames.contains(ent.index)) {
						nfound++;
					}
					if (entNames.size() == nfound) {
						break;
					}
				}				
			}
			if (entNames.size() == nfound) {
				return true;
			}
			else {
				return false;
			}
		}
		
		private function getMatchingEnts(totalEnts:Array, entNamesToMatch:Set):Array
		{
			var matchingEnts:Array = new Array();
			//loop through this instances entities and only add matches to the new doc obj
			for each ( var entity:Object in totalEnts )
			{
				if ( entNamesToMatch.contains(entity.index) )
				{
					matchingEnts.push(entity);
					break;
				}
			}									 
			return matchingEnts;
		}
		
		private function getMatchingAssocs(totalAssocs:Array, entNamesToMatch:Set):Array
		{
			var matchingAssocs:Array = new Array();
			//loop through this instances entities and only add matches to the new doc obj
			for each ( var assoc:Object in totalAssocs )
			{
				var entity1_index:String = assoc.entity1_index;
				var entity2_index:String = assoc.entity2_index;
				var geo_index:String = assoc.geo_index;
				
				if ((null != entity1_index) && (entNamesToMatch.contains(entity1_index))) {
					matchingAssocs.push(assoc);
					break;
				}
				if ((null != entity2_index) && (entNamesToMatch.contains(entity2_index))) {
					matchingAssocs.push(assoc);
					break;
				}
				if ((null != geo_index) && (entNamesToMatch.contains(geo_index))) {
					matchingAssocs.push(assoc);
					break;
				}
			}									 
			return matchingAssocs;
		}
		
		//______________________________________________________________________________________
		//
		// FRAMEWORK INTERFACE (QUERIES)
		
		// Query Options - Allows advanced users to do things like set the geo/time decay from widgets
		
		/**
		 * Gets the current query options (can then change and set them).
		 * For advanced developer use.
		 * @return the current query options (see REST API for details on the JSON format)
		 */
		public function getCurrentQuery():Object 
		{
			if ( infinite_parent != null )
			{
				return infinite_parent.getCurrentQuery();
			}
			else
			{
				return null;
			}
		}
		/**
		 * Gets the most recent query to be run (can then eg change and set it over the current query, or run it locally).
		 * For advanced developer use.
		 * @return the current query options (see REST API for details on the JSON format)
		 */
		public function getLastQuery():Object {
			return _lastQuery;
		}
		/**
		 * Gets the current query options (can then change and set them).
		 * For advanced developer use.
		 * @param queryName (optional) - the name of the query from which to retrieve the options (the last query by default/if null)
		 * @return the current query options (see REST API for details on the JSON format)
		 */
		public function getSavedQuery(queryName:String = null):Object {
			var savedQuery:IResultSet = _savedQueryResultsMap.get(queryName) as IResultSet;
			if (null != savedQuery) {
				return savedQuery.getQueryObject();
			}
			else {
				return null;
			}			
		}
		
		/**
		 * Gets a list of all the saved queries
		 * For advanced developer use.
		 * @param regexFilter (optional) - An optional filter to apply to the saved queries
		 * @return an array collection of query names, use getSavedQuery to get the actual option
		 */
		public function getSavedQueryNames():ArrayCollection {
			return new ArrayCollection(_savedQueryResultsMap.getKeys());
		}
		
		/**
		 * Sets the current query options (normally obtained from getCurrentQuery or getSavedQuery).
		 * For advanced developer use.
		 * @param modifiedQueryOptions The new query object
		 * @param modifiedElements A comma-separated list of elements that have changed (eg "score", "qt")
		 * if non-null pops up the advanced options dialog
		 * @return the current query options (see REST API for details on the JSON format)
		 */
		public function setCurrentQuery(modifiedQueryOptions:Object, modifiedElements:String = null):void {
			infinite_parent.updateCurrentQuery(modifiedQueryOptions, modifiedElements);
		}

		//////////////////////////////////////////////////////////////////////////////////
		
		// Performing and applying queries
		
		// These are all performed by widget-specific contexts (WidgetContext_Local)
		
		public function doQuery(queryObject:Object, callback:Function, isLocalQuery:Boolean = true, saveQueryName:String = null):Boolean {
			// Not applicable
			return false;
		}
		
		public function applySavedQuery_FromName(queryName:String = null, localOnly:Boolean = true):Boolean {
			// Not applicable
			return false;
		}
		
		public function applySavedQuery_FromResultSet(queryResults:IResultSet = null, localOnly:Boolean = true):void {
			// Not applicable
		}
		
		//______________________________________________________________________________________
		//
		// THE FRAMEWORK'S INTERFACE WITH US (NOT IWIDGETCONTEXT)
		// (This would be a separate IWidgetContext_Framework if it were necessary)
						
		// New query
		
		public function onNewQuery(queryResults:QueryResults, queryDesc:String, queryObj:Object, queryName:String = null):void {
			var lastQueryResults:IResultSet = ResultSet.createBaseQuery(queryResults, queryObj, queryDesc);
			this.onNewQuery_internal(lastQueryResults, queryName);
		}
		
		public function onNewQuery_internal(queryResults:IResultSet, queryName:String = null):void {
			_lastQueryResults = queryResults;
			_lastQuery = _lastQueryResults.getQueryObject();
			_currFilterResults = _lastQueryResults.getTopQueryResults();
			
			if (null != queryName) {
				_savedQueryResultsMap.put(queryName, _lastQueryResults);
			}
			_recentQueryResults.addItem(_lastQueryResults);
			while (_recentQueryResults.length > this._nRecentQueriesToKeep) {
				_recentQueryResults.removeItemAt(_recentQueryResults.length - 1);
			}			
		}
		
		// Filter reset
		
		public function changeFilterPosition(relativePos:int, absolutePos:int = -1):void 
		{
			if (-1 != absolutePos) 
			{
				if (0 == absolutePos) 
				{
					absolutePos = 1; // Can't start at the query base
				}
				_currFilterResults = _currFilterResults.getChain().getItemAt(absolutePos) as IResultSet;
			}
			else {
				var nCurrPos:int = _currFilterResults.getPositionInChain();
				nCurrPos += relativePos;
				if (0 == nCurrPos) {
					nCurrPos = 1; // Can't start at the query base
				}
				_currFilterResults = _currFilterResults.getChain().getItemAt(nCurrPos) as IResultSet;
			}
		}		
		
		//______________________________________________________________________________________
		//
		// MISC UTILITY
		
	}
}
