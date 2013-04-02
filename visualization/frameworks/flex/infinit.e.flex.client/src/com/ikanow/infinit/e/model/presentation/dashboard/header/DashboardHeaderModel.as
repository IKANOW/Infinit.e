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
package com.ikanow.infinit.e.model.presentation.dashboard.header
{
	import com.adobe.utils.StringUtil;
	import com.ikanow.infinit.e.shared.event.QueryEvent;
	import com.ikanow.infinit.e.shared.event.SessionEvent;
	import com.ikanow.infinit.e.shared.model.constant.Constants;
	import com.ikanow.infinit.e.shared.model.constant.QueryConstants;
	import com.ikanow.infinit.e.shared.model.constant.ServiceConstants;
	import com.ikanow.infinit.e.shared.model.constant.types.NavigationItemTypes;
	import com.ikanow.infinit.e.shared.model.constant.types.QueryDimensionTypes;
	import com.ikanow.infinit.e.shared.model.constant.types.QuerySuggestionTypes;
	import com.ikanow.infinit.e.shared.model.presentation.base.PresentationModel;
	import com.ikanow.infinit.e.shared.model.vo.QuerySuggestion;
	import com.ikanow.infinit.e.shared.model.vo.User;
	import com.ikanow.infinit.e.shared.model.vo.ui.DialogControl;
	import com.ikanow.infinit.e.shared.util.CollectionUtil;
	import com.ikanow.infinit.e.shared.util.ServiceUtil;
	import flash.events.Event;
	import flash.net.URLRequest;
	import flash.net.navigateToURL;
	import flash.utils.setTimeout;
	import mx.collections.ArrayCollection;
	import mx.controls.AdvancedDataGrid;
	import mx.controls.Alert;
	import mx.olap.QueryError;
	import mx.resources.ResourceManager;
	
	/**
	 *  Dashboard Header Presentation Model
	 */
	public class DashboardHeaderModel extends PresentationModel
	{
		
		//======================================
		// public properties 
		//======================================
		
		[Bindable]
		[Inject]
		public var navigator:DashboardHeaderNavigator;
		
		[Bindable]
		/**
		 * The current user
		 */
		public var currentUser:User;
		
		[Bindable]
		/**
		 * The summary for the last query summary
		 */
		public var lastQuerySummary:String;
		
		[Bindable]
		/**
		 * The summary for the last query summary
		 */
		public var lastQuerySettingsSummary:String;
		
		[Bindable]
		/**
		 * the collection of query suggestions
		 */
		public var suggestions:ArrayCollection;
		
		[Bindable]
		/**
		 * Show the suggestions list
		 */
		public var showSuggestions:Boolean;
		
		[Bindable]
		/**
		 * The selected query suggestion - to be used for the simple search
		 */
		public var selectedSuggestion:QuerySuggestion;
		
		private var _suggestionsSelectedIndex:int = -1;
		
		[Bindable( "suggestionsSelectedIndexChange" )]
		/**
		 * The selected index for the suggestions collection
		 */
		public function get suggestionsSelectedIndex():int
		{
			return _suggestionsSelectedIndex;
		}
		
		//======================================
		// protected properties 
		//======================================
		
		/**
		 * The last keyword that was entered
		 */
		protected var currentKeywordString:String = Constants.BLANK;
		
		/**
		 * Indicates if a search is in progress
		 */
		protected var searchInProgress:Boolean = false;
		
		
		//======================================
		// public methods 
		//======================================
		
		/**
		 * Clear the last query
		 */
		public function clearLastQuery():void
		{
			dispatcher.dispatchEvent( new QueryEvent( QueryEvent.CLEAR_LAST_QUERY ) );
		}
		
		/**
		 * Clear the selected suggestion
		 */
		public function clearSelectedSuggestion():void
		{
			selectedSuggestion = null;
		}
		
		/**
		 * Get Query Suggestions
		 */
		public function getQuerySuggestions( keywordString:String ):void
		{
			searchInProgress = false;
			
			if ( StringUtil.trim( keywordString ) == Constants.BLANK )
				keywordString = Constants.WILDCARD;
			
			var queryEvent:QueryEvent = new QueryEvent( QueryEvent.TRY_GET_QUERY_SUGGESTIONS );
			queryEvent.keywordString = keywordString;
			queryEvent.searchType = QueryEvent.GET_QUERY_SUGGESTIONS;
			dispatcher.dispatchEvent( queryEvent );
			
			currentKeywordString = keywordString;
		}
		
		/**
		 * Hide Suggestions
		 */
		public function hideSuggestionsList():void
		{
			showSuggestions = false;
		}
		
		/**
		 * LaunchManager
		 */
		public function launchManager():void
		{
			var urlRequest:URLRequest = new URLRequest( ServiceUtil.getManagerUrl() );
			navigateToURL( urlRequest, ServiceConstants.BLANK_URL );
		}
		
		/**
		 * Log Out
		 */
		public function logout():void
		{
			var sessionEvent:SessionEvent = new SessionEvent( SessionEvent.LOGOUT );
			sessionEvent.dialogControl = DialogControl.create( false, ResourceManager.getInstance().getString( 'infinite', 'sessionService.logout' ) );
			dispatcher.dispatchEvent( sessionEvent );
		}
		
		/**
		 * Reset
		 * Used to reset on logout
		 */
		public function reset():void
		{
			showSuggestions = false;
			suggestions = null;
		}
		
		/**
		 * Current User
		 * @param value
		 */
		[Inject( "userManager.currentUser", bind = "true" )]
		public function setCurrentUser( value:User ):void
		{
			currentUser = value;
			
			// update the user name in the navagator action button
			if ( value )
				navigator.updateCurrentUser();
		}
		
		/**
		 * Last Query Summary
		 * @param value
		 */
		[Inject( "queryManager.lastQuerySettingsSummary", bind = "true" )]
		public function setLastQuerySettingsSummary( value:String ):void
		{
			lastQuerySettingsSummary = value;
			
			// clear the selected suggestion
			clearSelectedSuggestion();
			
			searchInProgress = false;
			showSuggestions = false;
		}
		
		/**
		 * Last Query Summary
		 * @param value
		 */
		[Inject( "queryManager.lastQuerySummary", bind = "true" )]
		public function setLastQuerySummary( value:String ):void
		{
			lastQuerySummary = value;
			
			// clear the selected suggestion
			clearSelectedSuggestion();
			
			searchInProgress = false;
			showSuggestions = false;
		}
		
		/**
		 * Set the selected suggestion
		 */
		public function setSelectedSuggestion( value:QuerySuggestion ):void
		{
			selectedSuggestion = value;
		}
		
		
		/**
		 * set the query suggestions
		 * @param value
		 */
		[Inject( "queryManager.suggestions", bind = "true" )]
		public function setSuggestions( value:ArrayCollection ):void
		{
			if ( searchInProgress || lastQuerySummary )
				return;
			
			suggestions = value;
			
			if ( suggestions && currentKeywordString != Constants.WILDCARD )
			{
				var firstSuggestion:QuerySuggestion = suggestions.getItemAt( 0 ) as QuerySuggestion;
				
				if ( firstSuggestion.value == currentKeywordString )
				{
					setSelectedSuggestion( firstSuggestion );
				}
			}
			
			showSuggestions = ( suggestions && suggestions.length > 0 );
		}
		
		/**
		 * Set the suggestions selected index
		 */
		public function setSuggestionsSelectedIndex( value:int ):void
		{
			_suggestionsSelectedIndex = value;
			
			dispatchEvent( new Event( "suggestionsSelectedIndexChange" ) );
		}
		
		/**
		 * Show Suggestions
		 */
		public function showSuggestionsList():void
		{
			showSuggestions = true;
		}
		
		/**
		 * Submit Query
		 */
		public function submitQuery():void
		{
			// set or create the selected suggestion
			if ( !selectedSuggestion )
			{
				if ( suggestions && !selectedSuggestion )
				{
					setSelectedSuggestion( suggestions.getItemAt( 0 ) as QuerySuggestion );
				}
				else
				{
					selectedSuggestion = new QuerySuggestion();
					selectedSuggestion.dimension = QueryDimensionTypes.EXACT_TEXT;
					selectedSuggestion.value = currentKeywordString;
				}
			}
			
			searchInProgress = true;
			
			// run the last advanced query or a simple query
			if ( lastQuerySummary )
			{
				dispatcher.dispatchEvent( new QueryEvent( QueryEvent.RUN_ADVANCED_QUERY ) );
			}
			else
			{
				// catch uo to the last keyword string; in case the user typed very fast
				if ( selectedSuggestion.type == QuerySuggestionTypes.EXACT_TEXT && selectedSuggestion.value != currentKeywordString )
				{
					selectedSuggestion.value = currentKeywordString;
				}
				
				var queryEvent:QueryEvent = new QueryEvent( QueryEvent.RUN_SIMPLE_QUERY );
				queryEvent.querySuggestion = selectedSuggestion;
				dispatcher.dispatchEvent( queryEvent );
			}
		}
	}
}

