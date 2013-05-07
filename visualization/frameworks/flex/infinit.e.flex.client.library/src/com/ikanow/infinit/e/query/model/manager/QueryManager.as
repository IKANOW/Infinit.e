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
package com.ikanow.infinit.e.query.model.manager
{
	import com.adobe.utils.StringUtil;
	import com.ikanow.infinit.e.shared.event.NavigationEvent;
	import com.ikanow.infinit.e.shared.event.QueryEvent;
	import com.ikanow.infinit.e.shared.event.SetupEvent;
	import com.ikanow.infinit.e.shared.model.constant.Constants;
	import com.ikanow.infinit.e.shared.model.constant.NavigationConstants;
	import com.ikanow.infinit.e.shared.model.constant.QueryConstants;
	import com.ikanow.infinit.e.shared.model.constant.types.EntityTypes;
	import com.ikanow.infinit.e.shared.model.constant.types.QueryDimensionTypes;
	import com.ikanow.infinit.e.shared.model.constant.types.QueryOperatorTypes;
	import com.ikanow.infinit.e.shared.model.constant.types.QueryStringTypes;
	import com.ikanow.infinit.e.shared.model.constant.types.QueryTermTypes;
	import com.ikanow.infinit.e.shared.model.manager.base.InfiniteManager;
	import com.ikanow.infinit.e.shared.model.vo.QueryObject;
	import com.ikanow.infinit.e.shared.model.vo.QueryOutputAggregationOptions;
	import com.ikanow.infinit.e.shared.model.vo.QueryOutputDocumentOptions;
	import com.ikanow.infinit.e.shared.model.vo.QueryOutputFilterOptions;
	import com.ikanow.infinit.e.shared.model.vo.QueryScoreOptions;
	import com.ikanow.infinit.e.shared.model.vo.QueryScoreOptionsRequest;
	import com.ikanow.infinit.e.shared.model.vo.QueryString;
	import com.ikanow.infinit.e.shared.model.vo.QueryStringRequest;
	import com.ikanow.infinit.e.shared.model.vo.QuerySuggestion;
	import com.ikanow.infinit.e.shared.model.vo.QuerySuggestions;
	import com.ikanow.infinit.e.shared.model.vo.QueryTerm;
	import com.ikanow.infinit.e.shared.model.vo.Setup;
	import com.ikanow.infinit.e.shared.model.vo.TypedQueryString;
	import com.ikanow.infinit.e.shared.model.vo.User;
	import com.ikanow.infinit.e.shared.model.vo.ui.DialogControl;
	import com.ikanow.infinit.e.shared.model.vo.ui.ServiceResult;
	import com.ikanow.infinit.e.shared.model.vo.ui.ServiceStatistics;
	import com.ikanow.infinit.e.shared.util.CollectionUtil;
	import com.ikanow.infinit.e.shared.util.JSONUtil;
	import com.ikanow.infinit.e.shared.util.ObjectTranslatorUtil;
	import com.ikanow.infinit.e.shared.util.QueryUtil;
	import com.ikanow.infinit.e.shared.util.ServiceUtil;
	import com.ikanow.infinit.e.widget.library.data.WidgetDragObject;
	import flash.utils.setTimeout;
	import mx.collections.ArrayCollection;
	import mx.collections.SortField;
	import mx.controls.Alert;
	import mx.resources.ResourceManager;
	import mx.utils.ObjectUtil;
	
	/**
	 * Query Manager
	 */
	public class QueryManager extends InfiniteManager
	{
		
		//======================================
		// public properties 
		//======================================
		
		[Inject( "communityManager.selectedCommunities", bind = "true" )]
		/**
		 * The collection of selected communities
		 */
		public var selectedCommunities:ArrayCollection;
		
		[Inject( "sourceManager.sources", bind = "true" )]
		/**
		 * The current sources
		 */
		public var sources:ArrayCollection;
		
		[Bindable]
		/**
		 * The collection or recent queries
		 */
		public var recentQueries:ArrayCollection;
		
		/**
		 * The last event keyword string that was used for the getQuerySuggestions() call
		 */
		public var lastEventSuggestionKeywordString:String = Constants.BLANK;
		
		/**
		 * The last keyword string that was used for the getQuerySuggestions() call
		 */
		public var lastSuggestionKeywordString:String = Constants.BLANK;
		
		/**
		 * The next keyword string to be used for the getQuerySuggestions() call
		 * we only want to make one call for suggestions at a time
		 */
		public var nextSuggestionKeywordString:String = Constants.BLANK;
		
		/**
		 * indicates if a call to getQuerySuggestions is in progress
		 */
		public var getQuerySuggestionsInProgress:Boolean;
		
		[Bindable]
		/**
		 * The query suggestions from the server
		 */
		public var querySuggestions:QuerySuggestions;
		
		[Bindable]
		/**
		 * The parsed collection of suggetions for the simple query
		 */
		public var suggestions:ArrayCollection;
		
		[Bindable]
		/**
		 * The parsed collection of suggetions for the advanced query
		 */
		public var editorSuggestions:ArrayCollection;
		
		[Bindable]
		/**
		 * The results returned from a query
		 */
		public var queryResult:Object;
		
		[Bindable]
		/**
		 * The string results returned from a query
		 */
		public var queryResultString:String;
		
		[Bindable]
		/**
		 * The current query string that is being modified for a new query
		 */
		public var currentQueryStringRequest:QueryStringRequest;
		
		[Bindable]
		/**
		 * The query string request used for the last query() service call
		 */
		public var lastQueryStringRequest:QueryStringRequest;
		
		[Bindable]
		/**
		 * The raw query string used for the last query() service call
		 */
		public var lastQueryString:QueryString;
		
		[Bindable]
		/**
		 * The summary for the last query summary
		 */
		public var lastQuerySummary:String;
		
		[Bindable]
		/**
		 * The summary for the last query summary (settings component)
		 */
		public var lastQuerySettingsSummary:String;
		
		[Bindable]
		/**
		 * The statistics returned from a query
		 */
		public var queryStatistics:ServiceStatistics;
		
		[Bindable]
		/**
		 * Query Output Aggregation Options
		 */
		public var aggregationOptions:QueryOutputAggregationOptions = new QueryOutputAggregationOptions();
		
		[Bindable]
		/**
		 * Query Ouput Document Options
		 */
		public var documentOptions:QueryOutputDocumentOptions = new QueryOutputDocumentOptions();
		
		[Bindable]
		/**
		 * Query Ouput Filter Options
		 */
		public var filterOptions:QueryOutputFilterOptions = new QueryOutputFilterOptions();
		
		[Bindable]
		/**
		 * Query Scoring Options
		 */
		public var scoreOptions:QueryScoreOptions = new QueryScoreOptions();
		
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
		 * The selected query term used for editing
		 */
		public var selectedQueryTerm:QueryTerm;
		
		
		/**
		 * Variable for first time query runs
		 */
		public var refreshing:Boolean = false;
		
		//======================================
		// private properties 
		//======================================
		
		/**
		 * The current user
		 */
		private var currentUser:User;
		
		/**
		 * The setup
		 */
		private var setup:Setup;
		
		
		//======================================
		// public methods 
		//======================================
		
		/**
		 * Revert the advanced query to the last query
		 * @param value
		 */
		public function cancelEditAdvancedQuery():void
		{
			// set the last query string
			var queryString:QueryStringRequest = new QueryStringRequest( scoreOptions, documentOptions, QueryUtil.getAggregationOptionsObject( aggregationOptions ), filterOptions );
			queryString.qt = lastQueryString.qt.source;
			queryString.logic = lastQueryString.logic;
			queryString.qtOptions = lastQueryString.qtOptions;
			queryString.input = lastQueryString.input;
			
			// set the last query string
			lastQueryString = queryString.getOptions();
		}
		
		/**
		 * Clears the selected query term
		 * @param value
		 */
		public function cancelEditQueryTerm():void
		{
			selectedQueryTerm = null;
		}
		
		/**
		 * in case of a service fault, clear the in progress flag
		 */
		public function clearGetQuerySuggestionsInProgress():void
		{
			getQuerySuggestionsInProgress = false;
		}
		
		/**
		 * Clears the last query
		 * @param value
		 */
		public function clearLastQuery():void
		{
			lastQuerySummary = null;
			lastQuerySettingsSummary = null;
		}
		
		public function createAdvancedQuery():Object
		{
			var queryString:QueryStringRequest = new QueryStringRequest( scoreOptions, documentOptions, QueryUtil.getAggregationOptionsObject( aggregationOptions ), filterOptions );
			
			// set to wildcard if no query terms
			if ( queryTerms.length == 0 )
			{
				var queryTerm:QueryTerm = new QueryTerm();
				queryTerm.etext = Constants.WILDCARD;
				queryLogic = QueryConstants.DEFAULT_QUERY_LOGIC;
				queryTerms.addItem( queryTerm );
			}
			
			// add the query terms
			queryString.qt = queryTerms.source;
			
			// add the query logic 
			queryString.logic = queryLogic;
			
			var communtityIds:String = CollectionUtil.getStringFromArrayCollectionField( selectedCommunities );
			
			queryString.communityIds = communtityIds.split( Constants.STRING_ARRAY_DELIMITER );
			
			// set the query term options
			if ( setup && setup.queryString && setup.queryString.qtOptions )
				queryString.qtOptions = setup.queryString.qtOptions;
			else
				queryString.qtOptions = null;
			
			var sourcesAll:ArrayCollection = new ArrayCollection( sources.source );
			var sourcesCurrent:ArrayCollection = CollectionUtil.getSelectedItems( sourcesAll, true );
			var sourcesAvailable:ArrayCollection = CollectionUtil.getSelectedItems( sourcesAll, false );
			
			// update the sources input if the user has not selected all of the sources
			if ( sourcesCurrent.length > 0 && sourcesAvailable.length > 0 )
			{
				// use the collection that has the least amount of sources and mark srcInclude as true or false depending
				var useCurrentSources:Boolean = sourcesCurrent.length < sourcesAvailable.length;
				var sourcesCollection:ArrayCollection = useCurrentSources ? sourcesCurrent : sourcesAvailable;
				
				queryString.input = new Object();
				queryString.input[ QueryConstants.SRC_INCLUDE ] = useCurrentSources;
				queryString.input[ QueryConstants.SOURCES ] = CollectionUtil.getArrayFromString( CollectionUtil.getStringFromArrayCollectionField( sourcesCollection, QueryConstants.SOURCE_KEY ) );
			}
			
			return QueryUtil.getQueryStringObject( queryString );
		}
		
		/**
		 * Sets the selected query term for editing
		 * @param value
		 */
		public function editQueryTerm( value:QueryTerm ):void
		{
			selectedQueryTerm = value;
		}
		
		/**
		 * Get Query Suggestions
		 */
		public function getQuerySuggestions( keywordString:String, searchType:String ):void
		{
			getQuerySuggestionsInProgress = true;
			
			var queryEvent:QueryEvent = new QueryEvent( searchType );
			queryEvent.keywordString = keywordString;
			queryEvent.communityids = CollectionUtil.getStringFromArrayCollectionField( selectedCommunities );
			queryEvent.dialogControl = DialogControl.create( false, ResourceManager.getInstance().getString( 'infinite', 'queryService.getQuerySuggestions' ) );
			dispatcher.dispatchEvent( queryEvent );
			
			lastSuggestionKeywordString = keywordString;
		}
		public function loadAdvancedQuery( queryString:QueryStringRequest ):void
		{
			runQuery( queryString, true );
		}
		
		/**
		 * Reset
		 * Used to reset on logout
		 */
		public function reset():void
		{
			queryResult = null;
			selectedQueryTerm = null;
			queryTerms = new ArrayCollection();
			queryLogic = "";
			currentQueryStringRequest = null;
			lastEventSuggestionKeywordString = "";
			lastQueryString = null;
			lastQueryStringRequest = null;
			lastQuerySummary = null;
			lastQuerySettingsSummary = null;
			lastSuggestionKeywordString = "";
			documentOptions = null
			aggregationOptions = null;
			filterOptions = null;
			scoreOptions = null;
			setQueryStatistics( null );
			aggregationOptions = new QueryOutputAggregationOptions();
			documentOptions = new QueryOutputDocumentOptions();
			filterOptions = new QueryOutputFilterOptions();
			scoreOptions = new QueryScoreOptions();
			initRecentQueries();
		}
		
		/**
		 * run the advanced query
		 * @param value
		 */
		public function runAdvancedQuery():void
		{
			// create a new query string
			var queryString:QueryStringRequest = new QueryStringRequest( scoreOptions, documentOptions, QueryUtil.getAggregationOptionsObject( aggregationOptions ), filterOptions );
			
			// set to wildcard if no query terms
			if ( queryTerms.length == 0 )
			{
				var queryTerm:QueryTerm = new QueryTerm();
				queryTerm.etext = Constants.WILDCARD;
				queryLogic = QueryConstants.DEFAULT_QUERY_LOGIC;
				queryTerms.addItem( queryTerm );
			}
			
			// add the query terms
			queryString.qt = queryTerms.source;
			
			// add the query logic 
			queryString.logic = queryLogic;
			
			// run the query
			runQuery( queryString, true );
		}
		
		/**
		 * Run a query from a history item
		 */
		public function runHistoryQuery( typedQueryString:TypedQueryString ):void
		{
			// set advanced options
			aggregationOptions = typedQueryString.queryString.output.aggregation;
			documentOptions = typedQueryString.queryString.output.docs;
			filterOptions = typedQueryString.queryString.output.filter;
			scoreOptions = typedQueryString.queryString.score;
			
			// create a query string request
			var queryStringNew:QueryStringRequest = new QueryStringRequest( scoreOptions, documentOptions, QueryUtil.getAggregationOptionsObject( aggregationOptions ), filterOptions );
			queryStringNew.qt = typedQueryString.queryString.qt.source;
			queryStringNew.logic = typedQueryString.queryString.logic;
			queryStringNew.qtOptions = typedQueryString.queryString.qtOptions;
			queryStringNew.communityIds = QueryUtil.getUserCommunityIdsArrayFromArray( typedQueryString.queryString.communityIds.source, currentUser.communities );
			queryStringNew.input = null;
			
			// set the sources
			if ( typedQueryString.queryString.input && typedQueryString.queryString.input.sources )
			{
				queryStringNew.input = new Object();
				queryStringNew.input[ QueryConstants.SRC_INCLUDE ] = typedQueryString.queryString.input.srcInclude;
				queryStringNew.input[ QueryConstants.SOURCES ] = typedQueryString.queryString.input.sources.source;
			}
			
			// set the current query string
			currentQueryStringRequest = queryStringNew.clone();
			
			// set the last query string request
			lastQueryStringRequest = queryStringNew.clone();
			
			// set the last query string
			lastQueryString = typedQueryString.queryString.clone();
			
			// set last query summary
			lastQuerySummary = QueryUtil.getQueryStringSummary( lastQueryString.qt, lastQueryString.logic );
			lastQuerySettingsSummary = QueryUtil.getQueryStringSettingsSummary( lastQueryString );
			
			// run the query
			var queryEvent:QueryEvent = new QueryEvent( QueryEvent.QUERY );
			queryEvent.queryString = QueryUtil.getQueryStringObject( queryStringNew );
			queryEvent.communityids = QueryUtil.getUserCommunityIdsArrayStringFromArray( queryStringNew.communityIds, currentUser.communities );
			queryEvent.dialogControl = DialogControl.create( true, ResourceManager.getInstance().getString( 'infinite', 'queryService.searching' ) );
			dispatcher.dispatchEvent( queryEvent );
			
			// clear the query statistics
			setQueryStatistics( new ServiceStatistics() );
			
			// save the ui setup
			var setupEvent:SetupEvent = new SetupEvent( SetupEvent.SAVE_SETUP );
			setupEvent.dialogControl = DialogControl.create( false, ResourceManager.getInstance().getString( 'infinite', 'setupService.saveSetup' ) );
			dispatcher.dispatchEvent( setupEvent );
			
			// add the last query string to the recent queries collection
			recentQueries.addItem( QueryUtil.getTypedQueryString( lastQueryString, typedQueryString.type ) );
			recentQueries.refresh();
		}
		
		/**
		 * create the query string for a simple search
		 * and dispatch the QUERY event
		 * @param value
		 */
		public function runSimpleQuery( querySuggestion:QuerySuggestion ):void
		{
			// create a new query string
			var queryString:QueryStringRequest = new QueryStringRequest( scoreOptions, documentOptions, QueryUtil.getAggregationOptionsObject( aggregationOptions ), filterOptions );
			
			// set the query term
			queryString.qt = [ QueryUtil.getQueryTermFromSuggestion( querySuggestion ) ];
			
			// run the query
			runQuery( queryString, true );
		}
		
		public function saveAdvancedQuery():TypedQueryString
		{
			// create a new query string
			var queryString:QueryStringRequest = new QueryStringRequest( scoreOptions, documentOptions, QueryUtil.getAggregationOptionsObject( aggregationOptions ), filterOptions );
			
			// set to wildcard if no query terms
			if ( queryTerms.length == 0 )
			{
				var queryTerm:QueryTerm = new QueryTerm();
				queryTerm.etext = Constants.WILDCARD;
				queryLogic = QueryConstants.DEFAULT_QUERY_LOGIC;
				queryTerms.addItem( queryTerm );
			}
			
			// add the query terms
			queryString.qt = queryTerms.source;
			
			// add the query logic 
			queryString.logic = queryLogic;
			
			// set the community ids
			var communtityIds:String = CollectionUtil.getStringFromArrayCollectionField( selectedCommunities );
			queryString.communityIds = communtityIds.split( Constants.STRING_ARRAY_DELIMITER );
			
			// set the query term options
			if ( setup && setup.queryString && setup.queryString.qtOptions )
				queryString.qtOptions = setup.queryString.qtOptions;
			else
				queryString.qtOptions = null;
			
			var sourcesAll:ArrayCollection = new ArrayCollection( sources.source );
			var sourcesCurrent:ArrayCollection = CollectionUtil.getSelectedItems( sourcesAll, true );
			var sourcesAvailable:ArrayCollection = CollectionUtil.getSelectedItems( sourcesAll, false );
			
			// update the sources input if the user has not selected all of the sources
			if ( sourcesCurrent.length > 0 && sourcesAvailable.length > 0 )
			{
				// use the collection that has the least amount of sources and mark srcInclude as true or false depending
				var useCurrentSources:Boolean = sourcesCurrent.length < sourcesAvailable.length;
				var sourcesCollection:ArrayCollection = useCurrentSources ? sourcesCurrent : sourcesAvailable;
				
				queryString.input = new Object();
				queryString.input[ QueryConstants.SRC_INCLUDE ] = useCurrentSources;
				queryString.input[ QueryConstants.SOURCES ] = CollectionUtil.getArrayFromString( CollectionUtil.getStringFromArrayCollectionField( sourcesCollection, QueryConstants.SOURCE_KEY ) );
			}
			
			// set the last query string (handle score differently - we have already got the code in place to clone this programmatically)
			var tempQueryString:QueryString = queryString.getOptions();
			
			// add the last query string to the recent queries collection
			return QueryUtil.getTypedQueryString( tempQueryString, QueryStringTypes.QUERY );
		}
		
		
		
		/**
		 * Update the advanced query options and run a query to save
		 * @param scoreOptions
		 * @param aggregationOptions
		 * @param documentOptions
		 */
		public function setAdvancedOptions( documentOptions:QueryOutputDocumentOptions, aggregationOptions:QueryOutputAggregationOptions, filterOptions:QueryOutputFilterOptions, scoreOptions:QueryScoreOptions ):void
		{
			this.documentOptions = documentOptions;
			this.aggregationOptions = aggregationOptions;
			this.filterOptions = filterOptions;
			this.scoreOptions = scoreOptions;
			
			var queryString:QueryStringRequest = new QueryStringRequest( scoreOptions, documentOptions, QueryUtil.getAggregationOptionsObject( aggregationOptions ), filterOptions );
			queryString.qt = lastQueryStringRequest.qt;
			queryString.logic = lastQueryStringRequest.logic;
			
			// run the query to save the options
			runQuery( queryString, true, QueryStringTypes.SETTINGS );
		}
		
		/**
		 * Person Get response from server
		 * @param value
		 */
		[Inject( "userManager.currentUser", bind = "true" )]
		public function setCurrentUser( value:User ):void
		{
			if ( !value )
				return;
			
			currentUser = value;
			
			if ( setup && !refreshing )
			{
				initQueryStrings();
			}
			refreshing = false;
		}
		
		/**
		 * set the query logic string to be used for the query
		 * @param value
		 */
		public function setQueryLogic( value:String ):void
		{
			queryLogic = value;
			
			currentQueryStringRequest.logic = value;
		}
		
		/**
		 * set the result returned from a query
		 * @param value
		 */
		public function setQueryResult( value:Object, dataString:String ):void
		{
			queryResult = QueryUtil.getInititalizedQueryResultsObject( value );
			queryResultString = dataString;
		}
		
		/**
		 * set the statistics returned from a query
		 * @param value
		 */
		public function setQueryStatistics( value:ServiceStatistics ):void
		{
			queryStatistics = value;
		}
		
		/**
		 * set the suggestions for a query
		 * @param value
		 */
		public function setQuerySuggestions( value:QuerySuggestions, searchType:String ):void
		{
			querySuggestions = value;
			
			// set the appropriate collection
			switch ( searchType )
			{
				case QueryEvent.GET_QUERY_SUGGESTIONS:
					suggestions = getSuggestions( querySuggestions.dimensions );
					break;
				case QueryEvent.GET_EDITOR_QUERY_SUGGESTIONS:
					editorSuggestions = getSuggestions( querySuggestions.dimensions );
					break;
				case QueryEvent.GET_EDITOR_QUERY_EVENT_SUGGESTIONS:
					editorSuggestions = getEventSuggestions( querySuggestions.dimensions );
					break;
			}
			
			// gst the next set of suggestions or quit
			if ( nextSuggestionKeywordString != Constants.BLANK )
			{
				getQuerySuggestions( nextSuggestionKeywordString, searchType );
				nextSuggestionKeywordString = Constants.BLANK;
			}
			else
			{
				getQuerySuggestionsInProgress = false;
			}
		}
		
		/**
		 * set the squery terms to be used for the query
		 * @param value
		 */
		public function setQueryTerms( value:ArrayCollection ):void
		{
			queryTerms = value;
			
			var qtNew:Array = [];
			
			for each ( var queryTerm:QueryTerm in value )
			{
				qtNew.push( queryTerm );
			}
			
			// initialize the current query string if null
			if ( !currentQueryStringRequest )
				currentQueryStringRequest = new QueryStringRequest( scoreOptions, documentOptions, QueryUtil.getAggregationOptionsObject( aggregationOptions ), filterOptions );
			
			currentQueryStringRequest.qt = qtNew;
		}
		
		[Inject( "setupManager.setup", bind = "true" )]
		/**
		 * The setup
		 */
		public function setSetup( value:Setup ):void
		{
			if ( !value )
				return;
			
			setup = value;
			
			if ( currentUser && !refreshing )
			{
				initQueryStrings();
			}
			refreshing = false;
		}
		
		/**
		 * check to see if there is already a call in progress
		 * If so, overwirite the nextSuggestionKeywordString.
		 * else, get the suggestions.
		 * @param value
		 */
		public function tryGetQuerySuggestions( searchType:String, keywordString:String, keywordString2:String ):void
		{
			lastEventSuggestionKeywordString = keywordString2;
			
			// reset if the keyword is blank
			if ( keywordString.length == 0 )
			{
				suggestions = null;
				nextSuggestionKeywordString = Constants.BLANK;
				lastSuggestionKeywordString = Constants.BLANK;
				return;
			}
			
			// if a call is in progress queue the next call, else make the call
			if ( getQuerySuggestionsInProgress )
				nextSuggestionKeywordString = keywordString;
			else
				getQuerySuggestions( keywordString, searchType );
		}
		
		/**
		 * Updates the query object, pops up the changed settings page
		 */
		
		public function updateQueryAndNavigate( queryString:QueryString, newFocus:String ):void
		{
			var newQueryString:QueryStringRequest = updateQuery( queryString );
			
			// These have already been set (or are about to be set):
			newQueryString.qt = currentQueryStringRequest.qt;
			newQueryString.logic = currentQueryStringRequest.logic;
			
			// This can't be changed
			newQueryString.communityIds = currentQueryStringRequest.communityIds;
			
			// set the current query string
			currentQueryStringRequest = newQueryString.clone();
			
			// show the new view
			if ( null != newFocus )
			{
				var navigationEvent:NavigationEvent = null;
				
				// (special case for query builder)
				if ( NavigationConstants.QUERY_BUILDER_ID == newFocus )
				{
					navigationEvent = new NavigationEvent( NavigationEvent.NAVIGATE_BY_ID );
					navigationEvent.navigationItemId = NavigationConstants.QUERY_BUILDER_ID;
					dispatcher.dispatchEvent( navigationEvent );
					
					navigationEvent = new NavigationEvent( NavigationEvent.NAVIGATE_BY_ID );
					navigationEvent.navigationItemId = NavigationConstants.WORKSPACES_QUERY_ID;
					dispatcher.dispatchEvent( navigationEvent );
				}
				else // (source manager or query settings)
				{
					navigationEvent = new NavigationEvent( NavigationEvent.NAVIGATE_BY_ID );
					navigationEvent.navigationItemId = newFocus;
					dispatcher.dispatchEvent( navigationEvent );
				}
			}
		}
		
		/**
		 * set the query logic string to be used for the query
		 * @param value
		 */
		public function updateQueryFromWidgetDragDrop( widgetInfo:WidgetDragObject ):void
		{
			// DOCS
			
			if ( ( null != widgetInfo.documents ) && ( widgetInfo.documents.length > 0 ) )
				for each ( var doc:Object in widgetInfo.documents )
				{
					var newQuery:Object = QueryUtil.getQueryStringObject( currentQueryStringRequest );
					var qts:ArrayCollection = new ArrayCollection();
					
					var entities:ArrayCollection = new ArrayCollection( doc[ 'entities' ] );
					
					for each ( var ent:Object in entities )
					{
						var qt:Object = new Object();
						qt[ 'entity' ] = ent[ 'index' ];
						qts.addItem( qt );
					}
					newQuery[ 'qt' ] = qts;
					var queryString:QueryString = ObjectTranslatorUtil.translateObject( newQuery, new QueryString, null, false, true ) as QueryString;
					
					// Call update and navigate once per doc
					var queryEvent:QueryEvent = new QueryEvent( QueryEvent.UPDATE_QUERY_NAVIGATE );
					queryEvent.widgetInfo = widgetInfo;
					queryEvent.searchType = null;
					queryEvent.queryString = queryString;
					dispatcher.dispatchEvent( queryEvent );
				}
			
			//ENTS
			
			if ( ( null != widgetInfo.entities ) && ( widgetInfo.entities.length > 0 ) )
			{
				newQuery = QueryUtil.getQueryStringObject( currentQueryStringRequest );
				
				qts = new ArrayCollection();
				
				for each ( ent in widgetInfo.entities )
				{
					qt = new Object();
					qt[ 'entity' ] = ent[ 'index' ];
					qts.addItem( qt );
				}
				newQuery[ 'qt' ] = qts;
				
				queryString = ObjectTranslatorUtil.translateObject( newQuery, new QueryString, null, false, true ) as QueryString;
				
				// Call update and navigate once per doc
				queryEvent = new QueryEvent( QueryEvent.UPDATE_QUERY_NAVIGATE );
				queryEvent.widgetInfo = widgetInfo;
				queryEvent.searchType = null;
				queryEvent.queryString = queryString;
				dispatcher.dispatchEvent( queryEvent );
			}
			
			// ASSOCS
			
			if ( ( null != widgetInfo.associations ) && ( widgetInfo.associations.length > 0 ) )
			{
				newQuery = QueryUtil.getQueryStringObject( currentQueryStringRequest );
				
				qts = new ArrayCollection();
				
				for each ( var assoc:Object in widgetInfo.associations )
				{
					qt = new Object();
					var qtAssoc:Object = new Object();
					var qtAssocEnt1:Object = new Object();
					var qtAssocEnt2:Object = new Object();
					qtAssocEnt1[ 'entity' ] = assoc[ 'entity1_index' ];
					qtAssocEnt2[ 'entity' ] = assoc[ 'entity2_index' ];
					qtAssoc[ 'entity1' ] = qtAssocEnt1;
					qtAssoc[ 'entity2' ] = qtAssocEnt2;
					qt[ 'event' ] = qtAssoc; // (should be assoc, this isn't event forwwards compatible!)
					qts.addItem( qt );
				}
				newQuery[ 'qt' ] = qts;
				queryString = ObjectTranslatorUtil.translateObject( newQuery, new QueryString, null, false, true ) as QueryString;
				
				// Call update and navigate once per doc
				queryEvent = new QueryEvent( QueryEvent.UPDATE_QUERY_NAVIGATE );
				queryEvent.widgetInfo = widgetInfo;
				queryEvent.searchType = null;
				queryEvent.queryString = queryString;
				dispatcher.dispatchEvent( queryEvent );
			}
			
			// Once we're done, then navigate
			var navigationEvent:NavigationEvent = null;
			navigationEvent = new NavigationEvent( NavigationEvent.NAVIGATE_BY_ID );
			navigationEvent.navigationItemId = NavigationConstants.QUERY_BUILDER_ID;
			dispatcher.dispatchEvent( navigationEvent );
			navigationEvent = new NavigationEvent( NavigationEvent.NAVIGATE_BY_ID );
			navigationEvent.navigationItemId = NavigationConstants.WORKSPACES_QUERY_ID;
			dispatcher.dispatchEvent( navigationEvent );
		}
		
		//======================================
		// protected methods 
		//======================================
		
		/**
		 * Create the event suggestions collection from the results of the getQuerySuggetions() call
		 */
		protected function getEventSuggestions( collection:ArrayCollection ):ArrayCollection
		{
			var suggestionsNew:ArrayCollection = new ArrayCollection();
			var lastSearchTerm:String = ServiceUtil.replaceMultipleWhitespace( StringUtil.trim( lastSuggestionKeywordString ) ).toLowerCase();
			
			// add exact text suggestion
			var etext:QuerySuggestion = new QuerySuggestion();
			etext.value = lastEventSuggestionKeywordString;
			etext.type = EntityTypes.EXACT_TEXT;
			etext.dimension = QueryDimensionTypes.EXACT_TEXT;
			suggestionsNew.addItem( etext );
			
			// add entity suggestions
			suggestionsNew.addAll( collection );
			
			// set the search term for formatting
			for each ( var querySuggestion:QuerySuggestion in suggestionsNew )
			{
				querySuggestion.searchTerm = lastSearchTerm;
			}
			
			return suggestionsNew;
		}
		
		/**
		 * Create the suggestions collection from the results of the getQuerySuggetions() call
		 */
		protected function getSuggestions( collection:ArrayCollection ):ArrayCollection
		{
			var suggestionsNew:ArrayCollection = new ArrayCollection();
			var lastSearchTermWithCase:String = ServiceUtil.replaceMultipleWhitespace( StringUtil.trim( lastSuggestionKeywordString ) );
			var lastSearchTerm:String = lastSearchTermWithCase.toLowerCase();
			
			// add exact text suggestion
			var etext:QuerySuggestion = new QuerySuggestion();
			etext.value = lastSearchTerm;
			etext.type = EntityTypes.EXACT_TEXT;
			etext.dimension = QueryDimensionTypes.EXACT_TEXT;
			suggestionsNew.addItem( etext );
			
			// add free text suggestion
			var ftext:QuerySuggestion = new QuerySuggestion();
			ftext.value = lastSearchTermWithCase;
			ftext.type = EntityTypes.FREE_TEXT;
			ftext.dimension = QueryDimensionTypes.FREE_TEXT;
			suggestionsNew.addItem( ftext );
			
			// add entity suggestions
			suggestionsNew.addAll( collection );
			
			// sort the collection by dimension
			var sortOrderSortField:SortField = new SortField();
			sortOrderSortField.name = Constants.DEFAULT_SORT_ORDER_PROPERTY;
			sortOrderSortField.numeric = true;
			CollectionUtil.applySort( suggestionsNew, [ sortOrderSortField ] );
			
			var dimensionIndex:int = -1;
			
			// show the heading for the first item of each dimension
			for each ( var querySuggestion:QuerySuggestion in suggestionsNew )
			{
				// format the display value
				querySuggestion.searchTerm = lastSearchTerm;
				
				if ( querySuggestion.sortOrder != dimensionIndex )
				{
					querySuggestion.showHeading = true;
				}
				
				dimensionIndex = querySuggestion.sortOrder;
			}
			
			return suggestionsNew;
		}
		
		
		/**
		 * Initialize the query strings and run the first query
		 */
		protected function initQueryStrings():void
		{
			if ( !currentUser && !setup )
				return;
			
			var communityIds:Array = QueryUtil.getUserCommunityIdsArrayFromArray( setup.communityIds, currentUser.communities );
			
			setup.communityIds = communityIds;
			
			initRecentQueries();
			
			if ( !setup.queryString )
				return;
			
			// create a query string request (also updates aggregationOptions, documentOptions, filterOptions, scoreOptions)
			var queryString:QueryStringRequest = this.updateQuery( setup.queryString );
			
			//if ( communityIds.length > 0 && communityIds[ 0 ] != "" )
			//{
			// set the community ids
			queryString.communityIds = setup.communityIds;
			
			// set the current query string
			currentQueryStringRequest = queryString.clone();
			
			// set the last query string request
			lastQueryStringRequest = queryString.clone();
			
			// set the last query string
			lastQueryString = setup.queryString.clone();
			
			// set last query summary
			lastQuerySummary = QueryUtil.getQueryStringSummary( lastQueryString.qt, lastQueryString.logic );
			lastQuerySettingsSummary = QueryUtil.getQueryStringSettingsSummary( lastQueryString );
			
			// run the first query
			runFirstQuery( queryString );
			
			// add the last query string to the recent queries collection
			recentQueries.addItem( QueryUtil.getTypedQueryString( lastQueryString ) );
			recentQueries.refresh();
			//}
		}
		
		/**
		 * Initialize the recent queries colloction and apply an ascending sort by dateTime
		 */
		protected function initRecentQueries():void
		{
			// initialize the recent queries colloction and apply an ascending sort by dateTime
			recentQueries = new ArrayCollection();
			var sortOrderSortField:SortField = new SortField();
			sortOrderSortField.name = Constants.TIME_SORT_ORDER_PROPERTY;
			sortOrderSortField.numeric = true;
			sortOrderSortField.descending = true;
			CollectionUtil.applySort( recentQueries, [ sortOrderSortField ] );
		}
		
		/**
		 * run the first query
		 * @param value
		 */
		protected function runFirstQuery( queryString:QueryStringRequest ):void
		{
			var queryEvent:QueryEvent = new QueryEvent( QueryEvent.QUERY );
			queryEvent.queryString = QueryUtil.getQueryStringObject( queryString );
			queryEvent.communityids = QueryUtil.getUserCommunityIdsArrayStringFromArray( setup.communityIds, currentUser.communities );
			queryEvent.dialogControl = DialogControl.create( true, ResourceManager.getInstance().getString( 'infinite', 'queryService.searching' ) );
			dispatcher.dispatchEvent( queryEvent );
			
			// clear the query statistics
			setQueryStatistics( new ServiceStatistics() );
			
			// set last query summary
			lastQuerySummary = QueryUtil.getQueryStringSummary( new ArrayCollection( queryString.qt ), queryString.logic );
			lastQuerySettingsSummary = QueryUtil.getQueryStringSettingsSummary( queryString.getOptions() );
		}
		
		/**
		 * run a query
		 * @param value
		 */
		protected function runQuery( queryString:QueryStringRequest, advanced:Boolean, queryStringType:String = QueryStringTypes.QUERY ):void
		{
			// set the community ids
			var communtityIds:String = CollectionUtil.getStringFromArrayCollectionField( selectedCommunities );
			
			/*if ( communtityIds == "" || communtityIds == null )
			{
				Alert.show( "Error: No communities selected, please open the Source Manager and choose a community to search in" );
			}
			else
			{*/
			queryString.communityIds = communtityIds.split( Constants.STRING_ARRAY_DELIMITER );
			
			// set the query term options
			if ( setup && setup.queryString && setup.queryString.qtOptions )
				queryString.qtOptions = setup.queryString.qtOptions;
			else
				queryString.qtOptions = null;
			
			var sourcesAll:ArrayCollection = new ArrayCollection( sources.source );
			var sourcesCurrent:ArrayCollection = CollectionUtil.getSelectedItems( sourcesAll, true );
			var sourcesAvailable:ArrayCollection = CollectionUtil.getSelectedItems( sourcesAll, false );
			
			// update the sources input if the user has not selected all of the sources
			if ( sourcesCurrent.length > 0 && sourcesAvailable.length > 0 )
			{
				// use the collection that has the least amount of sources and mark srcInclude as true or false depending
				var useCurrentSources:Boolean = sourcesCurrent.length < sourcesAvailable.length;
				var sourcesCollection:ArrayCollection = useCurrentSources ? sourcesCurrent : sourcesAvailable;
				
				queryString.input = new Object();
				queryString.input[ QueryConstants.SRC_INCLUDE ] = useCurrentSources;
				queryString.input[ QueryConstants.SOURCES ] = CollectionUtil.getArrayFromString( CollectionUtil.getStringFromArrayCollectionField( sourcesCollection, QueryConstants.SOURCE_KEY ) );
			}
			
			// run the query
			var queryEvent:QueryEvent = new QueryEvent( QueryEvent.QUERY );
			queryEvent.queryString = QueryUtil.getQueryStringObject( queryString );
			queryEvent.communityids = CollectionUtil.getStringFromArrayCollectionField( selectedCommunities );
			queryEvent.dialogControl = DialogControl.create( true, ResourceManager.getInstance().getString( 'infinite', 'queryService.searching' ) );
			dispatcher.dispatchEvent( queryEvent );
			
			// clear the query statistics
			setQueryStatistics( new ServiceStatistics() );
			
			// set the current query string
			currentQueryStringRequest = queryString.clone();
			
			// set the last query string request
			lastQueryStringRequest = queryString.clone();
			
			// set the last query string
			lastQueryString = queryString.getOptions();
			
			// set last query summary
			lastQuerySummary = QueryUtil.getQueryStringSummary( lastQueryString.qt, lastQueryString.logic );
			lastQuerySettingsSummary = QueryUtil.getQueryStringSettingsSummary( lastQueryString );
			
			// save the ui setup
			var setupEvent:SetupEvent = new SetupEvent( SetupEvent.SAVE_SETUP );
			setupEvent.dialogControl = DialogControl.create( false, ResourceManager.getInstance().getString( 'infinite', 'setupService.saveSetup' ) );
			dispatcher.dispatchEvent( setupEvent );
			
			// if the setup was null, inti the recent queries collection
			if ( !recentQueries )
				initRecentQueries();
			
			// add the last query string to the recent queries collection
			recentQueries.addItem( QueryUtil.getTypedQueryString( lastQueryString, queryStringType ) );
			recentQueries.refresh();
			//}
		}
		
		/**
		 * Update the query object (utility function)
		 */
		protected function updateQuery( queryString:QueryString ):QueryStringRequest
		{
			// set advanced options
			aggregationOptions = queryString.output.aggregation;
			documentOptions = queryString.output.docs;
			filterOptions = queryString.output.filter;
			scoreOptions = queryString.score;
			
			// create a query string request
			var newQueryString:QueryStringRequest = new QueryStringRequest( scoreOptions, documentOptions, QueryUtil.getAggregationOptionsObject( aggregationOptions ), filterOptions );
			newQueryString.qt = queryString.qt.source;
			newQueryString.logic = queryString.logic.replace( QueryOperatorTypes.AND_NOT, QueryOperatorTypes.NOT );
			newQueryString.qtOptions = queryString.qtOptions;
			newQueryString.input = null;
			
			// set the sources
			if ( queryString.input && queryString.input.sources )
			{
				newQueryString.input = new Object();
				newQueryString.input[ QueryConstants.SRC_INCLUDE ] = queryString.input.srcInclude;
				newQueryString.input[ QueryConstants.SOURCES ] = queryString.input.sources.source;
			}
			return newQueryString;
		};
	}
}
