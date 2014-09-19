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
package com.ikanow.infinit.e.query.model.presentation.builder.editor
{
	import com.adobe.utils.StringUtil;
	import com.ikanow.infinit.e.shared.event.QueryEvent;
	import com.ikanow.infinit.e.shared.model.constant.Constants;
	import com.ikanow.infinit.e.shared.model.constant.QueryConstants;
	import com.ikanow.infinit.e.shared.model.constant.types.EditModeTypes;
	import com.ikanow.infinit.e.shared.model.constant.types.EntityTypes;
	import com.ikanow.infinit.e.shared.model.constant.types.QueryDimensionTypes;
	import com.ikanow.infinit.e.shared.model.constant.types.QuerySuggestionTypes;
	import com.ikanow.infinit.e.shared.model.constant.types.QueryTermTypes;
	import com.ikanow.infinit.e.shared.model.presentation.base.PresentationModel;
	import com.ikanow.infinit.e.shared.model.vo.QuerySuggestion;
	import com.ikanow.infinit.e.shared.model.vo.QueryTerm;
	import com.ikanow.infinit.e.shared.model.vo.QueryTermEvent;
	import com.ikanow.infinit.e.shared.model.vo.QueryTermGeoLocation;
	import com.ikanow.infinit.e.shared.model.vo.QueryTermTemporal;
	import com.ikanow.infinit.e.shared.util.CollectionUtil;
	import com.ikanow.infinit.e.shared.util.QueryUtil;
	
	import flash.events.Event;
	
	import flashx.textLayout.edit.EditingMode;
	
	import mx.collections.ArrayCollection;
	import mx.controls.Alert;
	import mx.resources.ResourceManager;
	
	/**
	 *  Query Term Editor Presentation Model
	 */
	public class QueryTermEditorModel extends PresentationModel
	{
		
		//======================================
		// public properties 
		//======================================
		
		[Bindable]
		[Inject]
		public var navigator:QueryTermEditorNavigator;
		
		[Bindable]
		/**
		 * The selected query term used for editing
		 */
		public var selectedQueryTerm:QueryTerm;
		
		[Bindable]
		/**
		 * The editor query term used for editing
		 */
		public var editorQueryTerm:QueryTerm;
		
		[Bindable]
		/**
		 * The title for the query term editor
		 */
		public var queryTermEditorTitle:String;
		
		[Bindable]
		/**
		 * The collection of query terms
		 */
		public var queryTerms:ArrayCollection;
		
		[Bindable]
		/**
		 * the collection of query suggestions
		 */
		public var suggestions:ArrayCollection;
		
		private var _showSuggestions:Boolean;
		
		[Bindable( "showSuggestionsChange" )]
		/**
		 * Show the suggestions list
		 */
		public function get showSuggestions():Boolean
		{
			return _showSuggestions;
		}
		
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
		
		
		//======================================
		// public methods 
		//======================================
		
		/**
		 * Clear the selected suggestion
		 */
		public function clearSelectedSuggestion():void
		{
			selectedSuggestion = null;
		}
		
		/**
		 * Clear suggestions
		 */
		public function clearSuggestionsList():void
		{
			hideSuggestionsList();
			setSuggestionsSelectedIndex( -1 );
			clearSelectedSuggestion();
			setSuggestions( null );
		}
		
		/**
		 * Get Query Event Suggestions
		 */
		public function getQueryEventSuggestions( keywordString:String, keywordString2:String ):void
		{
			var queryEvent:QueryEvent = new QueryEvent( QueryEvent.TRY_GET_QUERY_SUGGESTIONS );
			queryEvent.keywordString = keywordString;
			queryEvent.keywordString2 = keywordString2;
			queryEvent.searchType = QueryEvent.GET_EDITOR_QUERY_EVENT_SUGGESTIONS;
			dispatcher.dispatchEvent( queryEvent );
			
			currentKeywordString = keywordString;
		}
		
		/**
		 * Get Query Suggestions
		 */
		public function getQuerySuggestions( keywordString:String ):void
		{
			if ( StringUtil.trim( keywordString ) == Constants.BLANK )
				keywordString = Constants.WILDCARD;
			
			var queryEvent:QueryEvent = new QueryEvent( QueryEvent.TRY_GET_QUERY_SUGGESTIONS );
			queryEvent.keywordString = keywordString;
			queryEvent.searchType = QueryEvent.GET_EDITOR_QUERY_SUGGESTIONS;
			dispatcher.dispatchEvent( queryEvent );
			
			// Reset the term I was editing - this is invalid now
			this.setSelectedSuggestion( null );
			
			currentKeywordString = keywordString;
		}
		
		/**
		 * Hide the Suggestions
		 */
		public function hideSuggestionsList():void
		{
			_showSuggestions = false;
			
			dispatchEvent( new Event( "showSuggestionsChange" ) );
		}
		
		/**
		 * Run the advanced query
		 */
		public function runAdvancedQuery():void
		{
			dispatcher.dispatchEvent( new QueryEvent( QueryEvent.RUN_ADVANCED_QUERY ) );
		}
		
		/**
		 * Set the initial selected suggestion when the editor query term changes
		 */
		public function setInitialSelectedSuggestion():void
		{
			var selectedSuggestionNew:QuerySuggestion = new QuerySuggestion();
			
			switch ( editorQueryTerm.type.toLowerCase() )
			{
				case QueryTermTypes.EXACT_TEXT:
					selectedSuggestionNew.value = editorQueryTerm.etext;
					selectedSuggestionNew.type = QuerySuggestionTypes.EXACT_TEXT;
					selectedSuggestionNew.dimension = QueryDimensionTypes.EXACT_TEXT;
					currentKeywordString = editorQueryTerm.etext;
					break;
				case QueryTermTypes.FREE_TEXT:
					selectedSuggestionNew.value = editorQueryTerm.ftext;
					selectedSuggestionNew.type = QuerySuggestionTypes.FREE_TEXT;
					selectedSuggestionNew.dimension = QueryDimensionTypes.FREE_TEXT;
					currentKeywordString = editorQueryTerm.ftext;
					break;
				case QueryTermTypes.ENTITY:
					selectedSuggestionNew.value = editorQueryTerm.entityLabel;
					var strings:Array = editorQueryTerm.entity.split( "/", 2 );
					selectedSuggestionNew.type = strings[ 1 ];
					selectedSuggestionNew.dimension = EntityTypes.getEntityDimensionType( strings[ 1 ] );
					currentKeywordString = editorQueryTerm.entityLabel;
					break;
				default:
					selectedSuggestionNew = null;
					break;
			}
			
			setSelectedSuggestion( selectedSuggestionNew );
		}
		
		[Inject( "queryManager.queryTerms", bind = "true" )]
		/**
		 * Query Terms Collection
		 * @param value
		 */
		public function setQueryTerms( value:ArrayCollection ):void
		{
			queryTerms = value;
		}
		
		/**
		 * Set the selected query term
		 * @param value
		 */
		[Inject( "queryManager.selectedQueryTerm", bind = "true" )]
		public function setSelectedQueryTerm( value:QueryTerm ):void
		{
			selectedQueryTerm = value;
			
			if ( value )
			{
				var editorQueryTermNew:QueryTerm = new QueryTerm();
				
				editorQueryTermNew = value.clone();
				
				if ( editorQueryTermNew.editMode == EditModeTypes.UPDATE )
					editorQueryTermNew._id = value._id;
				
				if ( editorQueryTermNew.event == null )
					editorQueryTermNew.event = new QueryTermEvent();
				
				if ( editorQueryTermNew.geo == null )
					editorQueryTermNew.geo = new QueryTermGeoLocation();
				
				if ( editorQueryTermNew.time == null )
					editorQueryTermNew.time = new QueryTermTemporal();
				
				editorQueryTerm = editorQueryTermNew;
				
				setSuggestions( null );
				
				setInitialSelectedSuggestion();
			}
			else
			{
				editorQueryTerm = null;
				setSuggestions( null );
				clearSelectedSuggestion();
			}
		}
		
		/**
		 * Set the selected suggestion
		 */
		public function setSelectedSuggestion( value:QuerySuggestion ):void
		{
			selectedSuggestion = value;
			
			if ( value && suggestions )
			{
				setSuggestionsSelectedIndex( suggestions.getItemIndex( selectedSuggestion ) );
			}
			else
			{
				setSuggestionsSelectedIndex( -1 );
			}
		}
		
		/**
		 * set the query suggestions
		 * @param value
		 */
		[Inject( "queryManager.editorSuggestions", bind = "true" )]
		public function setSuggestions( value:ArrayCollection ):void
		{
			suggestions = value;
			
			if ( suggestions && currentKeywordString != Constants.WILDCARD )
			{
				var firstSuggestion:QuerySuggestion = suggestions.getItemAt( 0 ) as QuerySuggestion;
				
				if ( firstSuggestion.value == currentKeywordString )
				{
					setSelectedSuggestion( firstSuggestion );
				}
			}
			
			_showSuggestions = ( suggestions && suggestions.length > 0 );
			
			dispatchEvent( new Event( "showSuggestionsChange" ) );
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
		 * Show the Suggestions
		 */
		public function showSuggestionsList():void
		{
			_showSuggestions = true;
			
			dispatchEvent( new Event( "showSuggestionsChange" ) );
		}
		
		/**
		 * Toggle the selected query term expand alias setting
		 */
		public function toggleSelectedQueryTermRawTextOrLockDate():void
		{
			selectedQueryTerm.entityOpt.rawText = !selectedQueryTerm.entityOpt.rawText;
			editorQueryTerm.entityOpt.rawText = !editorQueryTerm.entityOpt.rawText;
			selectedQueryTerm.entityOpt.lockDate = !selectedQueryTerm.entityOpt.lockDate;
			editorQueryTerm.entityOpt.lockDate = !editorQueryTerm.entityOpt.lockDate;
			queryTerms.refresh();
		}
		
		/**
		 * Update the entity type query term
		 */
		public function updateEntityQueryType(whatUserTyped:String):void
		{
			if ( selectedSuggestion == null && whatUserTyped )
			{
				var selectedSuggestionNew:QuerySuggestion = new QuerySuggestion();
				selectedSuggestionNew.value = whatUserTyped;
				selectedSuggestionNew.type = QuerySuggestionTypes.EXACT_TEXT;
				selectedSuggestionNew.dimension = QueryDimensionTypes.EXACT_TEXT;
				selectedSuggestion = selectedSuggestionNew;
			}
			else if ( selectedSuggestion == null && suggestions )
				setSelectedSuggestion( suggestions.getItemAt( 0 ) as QuerySuggestion );
			
			var queryEvent:QueryEvent;
			var queryTerm:QueryTerm = QueryUtil.getQueryTermFromSuggestion( selectedSuggestion );
			queryTerm._id = editorQueryTerm._id;
			queryTerm.logicOperator = editorQueryTerm.logicOperator;
			queryTerm.entityOpt = editorQueryTerm.entityOpt;
			
			if ( editorQueryTerm.editMode == EditModeTypes.ADD )
			{
				queryEvent = new QueryEvent( QueryEvent.ADD_QUERY_TERM_TO_QUERY );
			}
			else
			{
				queryEvent = new QueryEvent( QueryEvent.UPDATE_QUERY_TERM );
				queryTerm.level = editorQueryTerm.level;
				queryTerm.logicIndex = editorQueryTerm.logicIndex;
			}
			
			queryEvent.queryTerm = queryTerm;
			dispatcher.dispatchEvent( queryEvent );
		}
		
		/**
		 * Update the event type query term
		 */
		public function updateEventQueryType( queryTermEvent:QueryTermEvent ):void
		{
			var queryEvent:QueryEvent;
			
			editorQueryTerm.setEvent( queryTermEvent );
			
			if ( editorQueryTerm.editMode == EditModeTypes.ADD )
				queryEvent = new QueryEvent( QueryEvent.ADD_QUERY_TERM_TO_QUERY );
			else
				queryEvent = new QueryEvent( QueryEvent.UPDATE_QUERY_TERM );
			
			queryEvent.queryTerm = editorQueryTerm;
			dispatcher.dispatchEvent( queryEvent );
		}
		
		/**
		 * Update the geo type query term
		 */
		public function updateGeoQueryType( lat:String, lng:String, radius:String ):void
		{
			var queryEvent:QueryEvent;
			
			editorQueryTerm.setGeo( new QueryTermGeoLocation() );
			editorQueryTerm.geo = new QueryTermGeoLocation();
			editorQueryTerm.geo.centerll = lat + Constants.COMMA + lng;
			editorQueryTerm.geo.dist = int( radius );
			
			if ( editorQueryTerm.editMode == EditModeTypes.ADD )
				queryEvent = new QueryEvent( QueryEvent.ADD_QUERY_TERM_TO_QUERY );
			else
				queryEvent = new QueryEvent( QueryEvent.UPDATE_QUERY_TERM );
			
			queryEvent.queryTerm = editorQueryTerm;
			dispatcher.dispatchEvent( queryEvent );
		}
		
		/**
		 * Update the temporal type query term
		 */
		public function updateTemporalQueryType( startDate:Date, endDate:Date ):void
		{
			var queryEvent:QueryEvent;
			
			editorQueryTerm.setTemporal( new QueryTermTemporal() );
			
			if ( startDate != null )
				editorQueryTerm.time.min = startDate;
			
			if ( endDate != null )
				editorQueryTerm.time.max = endDate;
			
			if ( editorQueryTerm.editMode == EditModeTypes.ADD )
				queryEvent = new QueryEvent( QueryEvent.ADD_QUERY_TERM_TO_QUERY );
			else
				queryEvent = new QueryEvent( QueryEvent.UPDATE_QUERY_TERM );
			
			queryEvent.queryTerm = editorQueryTerm;
			dispatcher.dispatchEvent( queryEvent );
		}
	}
}

