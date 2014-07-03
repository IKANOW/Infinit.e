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
package com.ikanow.infinit.e.query.control
{
	import com.ikanow.infinit.e.query.model.manager.QueryManager;
	import com.ikanow.infinit.e.query.model.presentation.builder.QueryBuilderModel;
	import com.ikanow.infinit.e.query.service.IQueryServiceDelegate;
	import com.ikanow.infinit.e.shared.control.base.InfiniteController;
	import com.ikanow.infinit.e.shared.event.QueryEvent;
	import com.ikanow.infinit.e.shared.model.constant.QueryConstants;
	import com.ikanow.infinit.e.shared.model.vo.QueryString;
	import com.ikanow.infinit.e.shared.model.vo.QuerySuggestions;
	import com.ikanow.infinit.e.shared.model.vo.ui.ServiceResult;
	import com.ikanow.infinit.e.shared.model.vo.ui.ServiceStatistics;
	import com.ikanow.infinit.e.shared.util.JSONUtil;
	
	import flash.utils.setTimeout;
	
	import mx.collections.ArrayCollection;
	import mx.controls.Alert;
	import mx.resources.ResourceManager;
	import mx.rpc.events.FaultEvent;
	import mx.rpc.events.ResultEvent;
	
	//======================================
	// public methods 
	//======================================
	/**
	 * Query Controller
	 */
	public class QueryController extends InfiniteController
	{
		
		//======================================
		// public properties 
		//======================================
		
		[Inject]
		public var queryServiceDelegate:IQueryServiceDelegate
		
		[Inject]
		public var queryManager:QueryManager;
		
		[Inject]
		public var queryBuilderModel:QueryBuilderModel;
		
		
		//======================================
		// public methods 
		//======================================
		
		[EventHandler( event = "QueryEvent.ADD_QUERY_TERM_TO_QUERY" )]
		/**
		 * Add a query term to the query
		 * @param event
		 */
		public function addQueryTermToQuery( event:QueryEvent ):void
		{
			queryBuilderModel.addQueryTermToQuery( event.queryTerm );
		}
		
		[EventHandler( event = "QueryEvent.CANCEL_EDIT_ADVANCED_QUERY" )]
		/**
		 * Reverts the query advanced settings to the last query
		 * @param event
		 */
		public function cancelEditAdvancedQuery( event:QueryEvent ):void
		{
			queryManager.cancelEditAdvancedQuery();
		}
		
		[EventHandler( event = "QueryEvent.CANCEL_EDIT_QUERY_TERM" )]
		/**
		 * Clears the selected query term
		 * @param event
		 */
		public function cancelEditQueryTerm( event:QueryEvent ):void
		{
			queryManager.cancelEditQueryTerm();
		}
		
		[EventHandler( event = "QueryEvent.CLEAR_LAST_QUERY" )]
		/**
		 * Clear the last query
		 * @param event
		 */
		public function clearLastQuerySummary( event:QueryEvent ):void
		{
			queryManager.clearLastQuery();
			queryBuilderModel.clearLastQuery();
		}
		
		[EventHandler( event = "QueryEvent.EDIT_QUERY_TERM" )]
		/**
		 * Sets the selected query term for editing
		 * @param event
		 */
		public function editQueryTerm( event:QueryEvent ):void
		{
			queryManager.editQueryTerm( event.queryTerm );
		}
		
		[EventHandler( event = "QueryEvent.GET_EDITOR_QUERY_EVENT_SUGGESTIONS" )]
		/**
		 * Get the event suggestions for a query keyword string
		 * @param event
		 */
		public function getEditorQueryEventSuggestions( event:QueryEvent ):void
		{
			executeServiceCall( "QueryController.getEditorQueryEventSuggestions()", event, queryServiceDelegate.getQueryEventSuggestions( event ), getEditorQueryEventSuggestions_resultHandler, getEditorQueryEventSuggestions_faultHandler );
		}
		
		/**
		 * Get Editor Query Suggestions Fault Handler
		 * @param event
		 */
		public function getEditorQueryEventSuggestions_faultHandler( event:FaultEvent ):void
		{
			queryManager.clearGetQuerySuggestionsInProgress();
			
			super.defaultFaultHandler( event );
		}
		
		/**
		 * Get Editor Query Suggestions Result Handler
		 * @param event
		 */
		public function getEditorQueryEventSuggestions_resultHandler( event:ResultEvent ):void
		{
			if ( verifyServiceResponseSuccess( "getEditorQueryEventSuggestions()", event.result as ServiceResult ) )
				queryManager.setQuerySuggestions( ServiceResult( event.result ).data as QuerySuggestions, QueryEvent.GET_EDITOR_QUERY_EVENT_SUGGESTIONS );
		}
		
		[EventHandler( event = "QueryEvent.GET_EDITOR_QUERY_SUGGESTIONS" )]
		/**
		 * Get the suggestions for a query keyword string
		 * @param event
		 */
		public function getEditorQuerySuggestions( event:QueryEvent ):void
		{
			executeServiceCall( "QueryController.getEditorQuerySuggestions()", event, queryServiceDelegate.getQuerySuggestions( event ), getEditorQuerySuggestions_resultHandler, getEditorQuerySuggestions_faultHandler );
		}
		
		/**
		 * Get Editor Query Suggestions Fault Handler
		 * @param event
		 */
		public function getEditorQuerySuggestions_faultHandler( event:FaultEvent ):void
		{
			queryManager.clearGetQuerySuggestionsInProgress();
			
			super.defaultFaultHandler( event );
		}
		
		/**
		 * Get Editor Query Suggestions Result Handler
		 * @param event
		 */
		public function getEditorQuerySuggestions_resultHandler( event:ResultEvent ):void
		{
			if ( verifyServiceResponseSuccess( "getEditorQuerySuggestions()", event.result as ServiceResult ) )
				queryManager.setQuerySuggestions( ServiceResult( event.result ).data as QuerySuggestions, QueryEvent.GET_EDITOR_QUERY_SUGGESTIONS );
		}
		
		[EventHandler( event = "QueryEvent.GET_QUERY_SUGGESTIONS" )]
		/**
		 * Get the suggestions for a query keyword string
		 * @param event
		 */
		public function getQuerySuggestions( event:QueryEvent ):void
		{
			executeServiceCall( "QueryController.getQuerySuggestions()", event, queryServiceDelegate.getQuerySuggestions( event ), getQuerySuggestions_resultHandler, getQuerySuggestions_faultHandler );
		}
		
		/**
		 * Get Query Suggestions Fault Handler
		 * @param event
		 */
		public function getQuerySuggestions_faultHandler( event:FaultEvent ):void
		{
			queryManager.clearGetQuerySuggestionsInProgress();
			
			super.defaultFaultHandler( event );
		}
		
		/**
		 * Get Query Suggestions Result Handler
		 * @param event
		 */
		public function getQuerySuggestions_resultHandler( event:ResultEvent ):void
		{
			if ( verifyServiceResponseSuccess( "getQuerySuggestions()", event.result as ServiceResult ) )
				queryManager.setQuerySuggestions( ServiceResult( event.result ).data as QuerySuggestions, QueryEvent.GET_QUERY_SUGGESTIONS );
		}
		
		[EventHandler( event = "QueryEvent.QUERY" )]
		/**
		 * Execute a query
		 * @param event
		 */
		public function guery( event:QueryEvent ):void
		{
			if ( event.communityids == null || event.communityids == "" )
			{
				Alert.show( "Error: No communities selected, please open the Source Manager and choose a community to search in" );
			}
			else
			{
				executeServiceCall( "QueryController.guery()", event, queryServiceDelegate.query( event ), guery_resultHandler, guery_faultHandler );
			}
		}
		
		/**
		 * Fault handler for query
		 * @param event
		 **/
		public function guery_faultHandler( event:FaultEvent ):void
		{
			Alert.show( "Query Error: refreshing on the SourceManager window can fix this issue sometimes.\n\n" + ResourceManager.getInstance().getString( 'infinite', 'infiniteController.serverErrorMessage', [ event.fault.rootCause.text ] ), ResourceManager.getInstance().getString( 'infinite', 'infiniteController.serverError' ) );
		}
		
		/**
		 * Execute query Result Handler
		 * @param event
		 */
		public function guery_resultHandler( event:ResultEvent ):void
		{
			if ( verifyServiceResponseSuccess( "query()", event.result as ServiceResult ) )
			{
				queryManager.setQueryStatistics( ServiceResult( event.result ).stats );
				queryManager.setQueryResult( ServiceResult( event.result ).rawData, ServiceResult( event.result ).dataString );
			}
		}
		
		[EventHandler( event = "QueryEvent.RESET" )]
		/**
		 * Reset Query
		 * @param event
		 */
		public function resetQuery( event:QueryEvent ):void
		{
			queryManager.reset();
		}
		
		[EventHandler( event = "QueryEvent.RUN_ADVANCED_QUERY" )]
		/**
		 * Run the advanced query
		 * @param event
		 */
		public function runAdvancedQuery( event:QueryEvent ):void
		{
			queryManager.runAdvancedQuery();
		}
		
		[EventHandler( event = "QueryEvent.RUN_HISTORY_QUERY" )]
		/**
		 * Run a query from the history
		 * @param event
		 */
		public function runHistoryQuery( event:QueryEvent ):void
		{
			queryManager.runHistoryQuery( event.typedQueryString );
		}
		
		[EventHandler( event = "QueryEvent.RUN_SIMPLE_QUERY" )]
		/**
		 * Prepare the QUERY call for a simple query
		 * @param event
		 */
		public function runSimpleQuery( event:QueryEvent ):void
		{
			queryManager.runSimpleQuery( event.querySuggestion );
		}
		
		[EventHandler( event = "QueryEvent.SAVE_QUERY_ADVANCED_SETTINGS" )]
		/**
		 * Saves the query advanced settings
		 * @param event
		 */
		public function saveAdvancedSettings( event:QueryEvent ):void
		{
			queryManager.setAdvancedOptions( event.documentOptions, event.aggregationOptions, event.filterOptions, event.scoreOptions );
		}
		
		[EventHandler( event = "QueryEvent.TRY_GET_QUERY_SUGGESTIONS" )]
		/**
		 * Get the suggestions if there isn't a call pending
		 * @param event
		 */
		public function tryGetQuerySuggestions( event:QueryEvent ):void
		{
			queryManager.tryGetQuerySuggestions( event.searchType, event.keywordString, event.keywordString2 );
		}
		
		[EventHandler( event = "QueryEvent.UPDATE_QUERY_FROM_WIDGET_DRAGDROP" )]
		/**
		 * Updates the query logic string to be used in a query
		 * @param event
		 */
		public function updateQuery( event:QueryEvent ):void
		{
			queryManager.updateQueryFromWidgetDragDrop( event.widgetInfo );
		}
		
		[EventHandler( event = "QueryEvent.OVERWRITE_QUERY_NAVIGATE" )]
		/**
		 * Saves the query advanced scoring settings, navigates to a new page
		 * @param event
		 */
		public function overwriteQueryAndNavigate( event:QueryEvent ):void
		{
			var queryString:QueryString = event.queryString as QueryString;
			queryBuilderModel.setLastQueryString(queryString);
			queryManager.overwriteQueryAndNavigate( queryString );
		}
		[EventHandler( event = "QueryEvent.UPDATE_QUERY_NAVIGATE" )]
		/**
		 * Saves the query advanced scoring settings, navigates to a new page
		 * @param event
		 */
		public function updateQueryAndNavigate( event:QueryEvent ):void
		{
			var queryString:QueryString = event.queryString as QueryString;
			queryBuilderModel.addQueryTermsToQuery( queryString.qt, ( event.widgetInfo != null ) ); // if widget info is non null then group... 
			queryManager.updateQueryAndNavigate( queryString, event.searchType );
		}
		[EventHandler( event = "QueryEvent.UPDATE_QUERY_LOGIC" )]
		/**
		 * Updates the query logic string to be used in a query
		 * @param event
		 */
		public function updateQueryLogic( event:QueryEvent ):void
		{
			queryManager.setQueryLogic( event.queryLogic );
		}
		
		
		[EventHandler( event = "QueryEvent.UPDATE_QUERY_TERM" )]
		/**
		 * Update a query term
		 * @param event
		 */
		public function updateQueryTerm( event:QueryEvent ):void
		{
			queryBuilderModel.updateQueryTerm( event.queryTerm );
		}
		[EventHandler( event = "QueryEvent.UPDATE_QUERY_TERMS" )]
		/**
		 * Update the query terms to be used in the query
		 * @param event
		 */
		public function updateQueryTerms( event:QueryEvent ):void
		{
			queryManager.setQueryTerms( event.queryTerms );
		}
	}





}
