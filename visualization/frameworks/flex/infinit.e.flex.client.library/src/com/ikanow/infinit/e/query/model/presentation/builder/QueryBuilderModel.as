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
package com.ikanow.infinit.e.query.model.presentation.builder
{
	import com.ikanow.infinit.e.query.view.builder.QueryTermSkinnableDataContainer;
	import com.ikanow.infinit.e.shared.event.QueryEvent;
	import com.ikanow.infinit.e.shared.model.constant.QueryConstants;
	import com.ikanow.infinit.e.shared.model.constant.types.EditModeTypes;
	import com.ikanow.infinit.e.shared.model.constant.types.QueryLogicTypes;
	import com.ikanow.infinit.e.shared.model.constant.types.QueryOperatorTypes;
	import com.ikanow.infinit.e.shared.model.constant.types.QueryTermTypes;
	import com.ikanow.infinit.e.shared.model.presentation.base.PresentationModel;
	import com.ikanow.infinit.e.shared.model.vo.QueryString;
	import com.ikanow.infinit.e.shared.model.vo.QueryStringRequest;
	import com.ikanow.infinit.e.shared.model.vo.QueryTerm;
	import com.ikanow.infinit.e.shared.model.vo.QueryTermEvent;
	import com.ikanow.infinit.e.shared.model.vo.QueryTermGeoLocation;
	import com.ikanow.infinit.e.shared.model.vo.QueryTermTemporal;
	import com.ikanow.infinit.e.shared.model.vo.Setup;
	import com.ikanow.infinit.e.shared.model.vo.ui.IQueryTerm;
	import com.ikanow.infinit.e.shared.model.vo.ui.IQueryTermGroup;
	import com.ikanow.infinit.e.shared.model.vo.ui.QueryTermGroup;
	import com.ikanow.infinit.e.shared.util.CollectionUtil;
	import com.ikanow.infinit.e.shared.util.ObjectTranslatorUtil;
	import com.ikanow.infinit.e.shared.util.QueryUtil;
	import flash.utils.setTimeout;
	import mx.collections.ArrayCollection;
	import mx.events.DragEvent;
	import mx.resources.ResourceManager;
	import spark.layouts.supportClasses.DropLocation;
	
	/**
	 *  Query Builder Presentation Model
	 */
	public class QueryBuilderModel extends PresentationModel
	{
		
		//======================================
		// public properties 
		//======================================
		
		[Bindable]
		[Inject]
		public var navigator:QueryBuilderNavigator;
		
		[Bindable]
		/**
		 * The query string used for the last query() service call
		 */
		public var lastQueryString:QueryString;
		
		[Bindable]
		/**
		 * The collection of query term groups
		 */
		public var queryTermGroups:ArrayCollection;
		
		[Bindable]
		/**
		 * The collection of query terms
		 */
		public var queryTerms:ArrayCollection;
		
		[Bindable]
		/**
		 * The string that represents the query logic
		 */
		public var queryLogic:String;
		
		[Bindable]
		/**
		 * Controls the visibility of the Query Term Editor popup
		 * @default
		 */
		public var showQueryTermEditor:Boolean;
		
		[Bindable]
		/**
		 * The selected query term used for editing
		 */
		public var selectedQueryTerm:QueryTerm;
		
		//======================================
		// protected properties 
		//======================================
		
		/**
		 * A property used to set the running logic index for the query terms
		 * in a query
		 * This is a work property
		 */
		protected var runningLogicIndex:int;
		
		/**
		 * A property used to parse the last query logic string
		 * and convert the query terms and logic operators into
		 * the queryTermGroups
		 * This is a work property
		 */
		protected var currentLogicArrayIndex:int;
		
		/**
		 * A property used to parse the last query logic string
		 * and convert the query terms and logic operators into
		 * the queryTermGroups
		 * This is a work property
		 */
		protected var logicArray:Array;
		
		/**
		 * The parent collection for a query term
		 * This is a work property
		 */
		protected var queryTermParentCollection:ArrayCollection;
		
		
		//======================================
		// public methods 
		//======================================
		
		/**
		 * Create a new query term for editing
		 */
		public function addQueryTerm():void
		{
			navigator.hideQueryTermEditor();
			
			clearEditingQueryTerms();
			
			var queryTermNew:QueryTerm = new QueryTerm();
			queryTermNew.etext = "";
			queryTermNew.logicOperator = QueryOperatorTypes.AND;
			queryTermNew.editing = true;
			queryTermNew.editMode = EditModeTypes.ADD;
			
			var queryEvent:QueryEvent = new QueryEvent( QueryEvent.EDIT_QUERY_TERM );
			queryEvent.queryTerm = queryTermNew;
			dispatcher.dispatchEvent( queryEvent );
		}
		
		/**
		 * Add a query term to the query
		 */
		public function addQueryTermToQuery( value:QueryTerm ):void
		{
			value.logicOperator = QueryOperatorTypes.AND;
			value.editMode = EditModeTypes.UPDATE;
			queryTermGroups.addItem( value );
			
			updateQueryTerms( queryTermGroups );
		}
		
		/**
		 * Add a query terms to the query
		 */
		public function addQueryTermsToQuery( value:ArrayCollection ):void
		{
			var queryTermAlreadyExists:Boolean;
			var queryTerm:QueryTerm;
			
			for each ( var queryTermObject:Object in value )
			{
				queryTerm = ObjectTranslatorUtil.translateObject( queryTermObject, new QueryTerm ) as QueryTerm;
				
				queryTermAlreadyExists = QueryUtil.doesQueryTermExistInCollection( queryTerm, queryTerms );
				
				if ( !queryTermAlreadyExists )
				{
					queryTerm.logicOperator = QueryOperatorTypes.AND;
					queryTerm.editMode = EditModeTypes.UPDATE;
					queryTermGroups.addItem( queryTerm );
				}
			}
			
			updateQueryTerms( queryTermGroups );
			
			navigator.showQueryBuilderView();
		}
		
		/**
		 * Reverts the advanced query builder to the last query
		 */
		public function cancelEditAdvancedQuery():void
		{
			dispatcher.dispatchEvent( new QueryEvent( QueryEvent.CANCEL_EDIT_ADVANCED_QUERY ) );
		}
		
		/**
		 * Cancels the editing of a query term
		 */
		public function cancelEditQueryTerm():void
		{
			navigator.hideQueryTermEditor();
			
			clearEditingQueryTerms();
			
			var queryEvent:QueryEvent = new QueryEvent( QueryEvent.CANCEL_EDIT_QUERY_TERM );
			dispatcher.dispatchEvent( queryEvent );
		}
		
		/**
		 * Clears the last query
		 */
		public function clearLastQuery():void
		{
			queryTermGroups = new ArrayCollection();
			
			updateQueryTerms( queryTermGroups );
		}
		
		/**
		 * Deletes a query term or query term group from the query
		 */
		public function deleteQueryTerm( term:* ):void
		{
			// remove the item
			QueryUtil.removeQueryTerm( queryTermGroups, term );
			
			// roll up orhan terms caused by removing the item
			for ( var i:int = 0; i < 10; i++ )
			{
				QueryUtil.normalizeQueryTermOrphans( queryTermGroups );
			}
			
			updateQueryTerms( queryTermGroups );
		}
		
		/**
		 * Handles dropping a query term to a new location
		 */
		public function dragDropHandler( event:DragEvent ):void
		{
			var dataGroup:QueryTermSkinnableDataContainer = event.target as QueryTermSkinnableDataContainer;
			var dropLocation:DropLocation = dataGroup.calculateDropLocation( event );
			var termToAdd:* = QueryTermSkinnableDataContainer( event.dragInitiator ).queryTerm.clone();
			var termToRemove:* = QueryTermSkinnableDataContainer( event.dragInitiator ).queryTerm;
			
			if ( dataGroup.queryTerm is QueryTermGroup )
			{
				// add the query term to the group
				dataGroup.queryTerm.children.addItemAt( termToAdd, dropLocation.dropIndex );
			}
			else if ( dataGroup.queryTerm is QueryTerm )
			{
				// replace the existing single query term with a group and add the existing and new query terms
				findQueryTermParentCollection( queryTermGroups, dataGroup.queryTerm );
				var queryTermGroup:QueryTermGroup = QueryUtil.convertQueryTermToGroup( dataGroup.queryTerm );
				queryTermGroup.children.addItemAt( termToAdd, dropLocation.dropIndex );
				queryTermParentCollection.setItemAt( queryTermGroup, queryTermParentCollection.getItemIndex( dataGroup.queryTerm ) );
			}
			else
			{
				// add query term to the top level group
				dataGroup.dataProvider.addItemAt( termToAdd, dropLocation.dropIndex );
			}
			
			// remove the moved item from it's original location
			deleteQueryTerm( termToRemove );
		}
		
		/**
		 * Edit a query term
		 */
		public function editQueryTerm( queryTerm:QueryTerm ):void
		{
			navigator.hideQueryTermEditor();
			
			clearEditingQueryTerms();
			
			var queryEvent:QueryEvent = new QueryEvent( QueryEvent.EDIT_QUERY_TERM );
			queryEvent.queryTerm = queryTerm;
			dispatcher.dispatchEvent( queryEvent );
		}
		
		/**
		 * Query manager last query string
		 * @param value
		 */
		[Inject( "queryManager.lastQueryString", bind = "true" )]
		public function setLastQueryString( value:QueryString ):void
		{
			lastQueryString = value;
			
			if ( value )
			{
				if ( value.qt && value.logic )
				{
					queryTerms = new ArrayCollection();
					
					for each ( var queryTerm:QueryTerm in value.qt )
					{
						queryTerms.addItem( queryTerm.clone() );
					}
					
					currentLogicArrayIndex = 0;
					logicArray = QueryUtil.getLogicStringArray( value.logic );
					queryTermGroups = buildQueryTermGroups();
				}
				else
				{
					queryTerms = new ArrayCollection();
					queryTermGroups = new ArrayCollection();
				}
				
				updateQueryTerms( queryTermGroups );
			}
			else
			{
				queryTerms = new ArrayCollection();
				queryTermGroups = new ArrayCollection();
			}
		}
		
		/**
		 * Query manager last query string
		 * @param value
		 */
		[Inject( "queryManager.selectedQueryTerm", bind = "true" )]
		public function setSelectedQueryTerm( value:QueryTerm ):void
		{
			selectedQueryTerm = value;
			
			if ( selectedQueryTerm )
			{
				switch ( selectedQueryTerm.type )
				{
					case QueryTermTypes.EXACT_TEXT:
						navigator.showQueryTermEditorEntityView();
						break;
					case QueryTermTypes.FREE_TEXT:
						navigator.showQueryTermEditorEntityView();
						break;
					case QueryTermTypes.ENTITY:
						navigator.showQueryTermEditorEntityView();
						break;
					case QueryTermTypes.EVENT:
						navigator.showQueryTermEditorEventView();
						break;
					case QueryTermTypes.TEMPORAL:
						navigator.showQueryTermEditorTemporalView();
						break;
					case QueryTermTypes.GEO_LOCATION:
						navigator.showQueryTermEditorGeoLocationView();
						break;
					default:
						navigator.showQueryTermEditorEntityView();
						break;
				}
				
				setTimeout( navigator.showQueryTermEditor, 100 );
			}
		}
		
		/**
		 * Updates a query term in the query
		 */
		public function updateQueryTerm( value:QueryTerm ):void
		{
			findQueryTermParentCollection( queryTermGroups, value );
			
			queryTermParentCollection.setItemAt( value, queryTermParentCollection.getItemIndex( selectedQueryTerm ) );
			
			updateQueryTerms( queryTermGroups );
		}
		
		/**
		 * Update the ui properties for the query terms nd query term groups
		 */
		public function updateQueryTerms( collection:ArrayCollection ):void
		{
			runningLogicIndex = 0;
			
			setQueryTermProperties( collection );
			
			queryTerms = new ArrayCollection();
			queryLogic = QueryConstants.BLANK;
			
			updateQueryLogic( collection );
			
			updateQueryManager();
		}
		
		//======================================
		// protected methods 
		//======================================
		
		/**
		 * Creates the query term groups recursively,
		 * from the format, operator and placeholder index values,
		 * in the query logic string
		 */
		protected function buildQueryTermGroups():ArrayCollection
		{
			var queryTermGroupsNew:ArrayCollection = new ArrayCollection();
			var queryTerm:QueryTerm;
			var queryTermGroup:QueryTermGroup;
			var operator:String = QueryOperatorTypes.AND;
			var logicType:String;
			
			for ( currentLogicArrayIndex; currentLogicArrayIndex < logicArray.length; currentLogicArrayIndex++ )
			{
				logicType = QueryLogicTypes.getType( logicArray[ currentLogicArrayIndex ] );
				
				switch ( logicType )
				{
					case QueryLogicTypes.GROUP_START:
						queryTermGroup = new QueryTermGroup();
						queryTermGroup.logicOperator = operator;
						currentLogicArrayIndex++;
						queryTermGroup.children = buildQueryTermGroups();
						queryTermGroupsNew.addItem( queryTermGroup );
						break;
					case QueryLogicTypes.OPERATOR:
						operator = logicArray[ currentLogicArrayIndex ].toString();
						break;
					case QueryLogicTypes.PLACE_HOLDER:
						queryTerm = queryTerms.getItemAt( int( logicArray[ currentLogicArrayIndex ] ) - 1 ) as QueryTerm;
						queryTerm.logicOperator = operator;
						queryTermGroupsNew.addItem( queryTerm );
						break;
					case QueryLogicTypes.GROUP_END:
						return queryTermGroupsNew;
				}
			}
			
			return queryTermGroupsNew;
		}
		
		/**
		 * Update the query terms and query logic string in the query manager
		 */
		protected function clearEditingQueryTerms():void
		{
			for each ( var queryTerm:QueryTerm in queryTerms )
			{
				queryTerm.editing = false;
			}
		}
		
		/**
		 * Find the parent collection for a query term
		 */
		protected function findQueryTermParentCollection( collection:ArrayCollection, queryTerm:QueryTerm ):void
		{
			if ( CollectionUtil.doesCollectionContainItem( collection, queryTerm ) )
			{
				queryTermParentCollection = collection;
			}
			else
			{
				for each ( var queryTermGroup:* in collection )
				{
					if ( queryTermGroup is QueryTermGroup )
					{
						findQueryTermParentCollection( queryTermGroup.children, queryTerm );
					}
				}
			}
		}
		
		/**
		 * Sets the level property for query terms and query term groups
		 * used for rendering different colors for each query level
		 * also sets the logic index for each query term
		 */
		protected function setQueryTermProperties( collection:ArrayCollection, parentLevel:int = 0 ):void
		{
			var level:int = parentLevel + 1;
			
			for each ( var term:* in collection )
			{
				term._id = QueryUtil.getRandomNumber();
				
				if ( term is QueryTermGroup )
				{
					term.level = level;
					setQueryTermProperties( term.children, level );
				}
				else
				{
					runningLogicIndex++;
					term.level = level - 1;
					term.logicIndex = runningLogicIndex;
				}
			}
		}
		
		/**
		 * Update the query terms and query logic string
		 */
		protected function updateQueryLogic( collection:ArrayCollection ):void
		{
			for each ( var term:* in collection )
			{
				// clean up the logic operators
				if ( collection.getItemIndex( term ) == 0 )
					term.logicOperator = QueryConstants.BLANK;
				else if ( term.logicOperator == QueryConstants.BLANK )
					term.logicOperator = QueryOperatorTypes.AND;
				
				// add the logic operator to the string
				if ( term.logicOperator != QueryConstants.BLANK )
					queryLogic += term.logicOperator;
				
				if ( term is QueryTermGroup )
				{
					// handle nested terms
					queryLogic += QueryConstants.SPACE + QueryConstants.PARENTHESIS_LEFT;
					updateQueryLogic( term.children );
					queryLogic += QueryConstants.PARENTHESIS_RIGHT + QueryConstants.SPACE;
				}
				else
				{
					// add the query term
					queryLogic += QueryConstants.SPACE + term.logicIndex + QueryConstants.SPACE;
					queryTerms.addItem( term );
				}
			}
		}
		
		/**
		 * Update the query terms and query logic string in the query manager
		 */
		protected function updateQueryManager():void
		{
			var queryEvent:QueryEvent;
			
			// update the query terms in the query manager
			queryEvent = new QueryEvent( QueryEvent.UPDATE_QUERY_TERMS );
			queryEvent.queryTerms = queryTerms;
			dispatcher.dispatchEvent( queryEvent );
			
			// update the query logic in the query manager
			queryEvent = new QueryEvent( QueryEvent.UPDATE_QUERY_LOGIC );
			queryEvent.queryLogic = queryLogic;
			dispatcher.dispatchEvent( queryEvent );
		}
	}
}

