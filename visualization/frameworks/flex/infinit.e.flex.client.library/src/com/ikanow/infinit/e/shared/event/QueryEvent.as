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
package com.ikanow.infinit.e.shared.event
{
	import com.ikanow.infinit.e.shared.event.base.InfiniteEvent;
	import com.ikanow.infinit.e.shared.model.constant.Constants;
	import com.ikanow.infinit.e.shared.model.vo.QueryOutputAggregationOptions;
	import com.ikanow.infinit.e.shared.model.vo.QueryOutputDocumentOptions;
	import com.ikanow.infinit.e.shared.model.vo.QueryOutputFilterOptions;
	import com.ikanow.infinit.e.shared.model.vo.QueryScoreOptions;
	import com.ikanow.infinit.e.shared.model.vo.QueryStringRequest;
	import com.ikanow.infinit.e.shared.model.vo.QuerySuggestion;
	import com.ikanow.infinit.e.shared.model.vo.QueryTerm;
	import com.ikanow.infinit.e.shared.model.vo.TypedQueryString;
	import com.ikanow.infinit.e.shared.model.vo.ui.DialogControl;
	import com.ikanow.infinit.e.widget.library.data.WidgetDragObject;
	import flash.events.Event;
	import mx.collections.ArrayCollection;
	import mx.events.DragEvent;
	
	public class QueryEvent extends InfiniteEvent
	{
		
		//======================================
		// public static properties 
		//======================================
		
		public static const QUERY:String = "queryEvent";
		
		public static const RUN_SIMPLE_QUERY:String = "runSimpleQueryEvent";
		
		public static const RUN_ADVANCED_QUERY:String = "runAdvancedQueryEvent";
		
		public static const RUN_HISTORY_QUERY:String = "runHistoryQueryEvent";
		
		public static const GET_QUERY_SUGGESTIONS:String = "getQuerySuggestionsEvent";
		
		public static const GET_EDITOR_QUERY_SUGGESTIONS:String = "getEditorQuerySuggestionsEvent";
		
		public static const GET_EDITOR_QUERY_EVENT_SUGGESTIONS:String = "getEditorQueryEventSuggestionsEvent";
		
		public static const TRY_GET_QUERY_SUGGESTIONS:String = "tryGetQuerySuggestionsEvent";
		
		public static const TRY_GET_QUERY_EVENT_SUGGESTIONS:String = "tryGetQueryEventSuggestionsEvent";
		
		public static const SAVE_QUERY_ENTITY_SETTINGS:String = "saveQueryEntitySettingsEvent";
		
		public static const SAVE_QUERY_ADVANCED_SETTINGS:String = "saveQueryAdvancedSettingsEvent";
		
		public static const CANCEL_EDIT_ADVANCED_QUERY:String = "cancelEditAdvancedQueryEvent";
		
		public static const CLEAR_LAST_QUERY:String = "clearLastQueryEvent";
		
		public static const QUERY_TERM_DRAG_DROP:String = "queryTermDragDrop";
		
		public static const DELETE_QUERY_TERM:String = "deleteQueryTerm";
		
		public static const UPDATE_QUERY_TERMS:String = "updateQueryTermsEvent";
		
		public static const UPDATE_QUERY_LOGIC:String = "updateQueryLogicEvent";
		
		public static const UPDATE_QUERY_NAVIGATE:String = "updateQueryAndNavigate";
		
		public static const OVERWRITE_QUERY_NAVIGATE:String = "overwriteQueryAndNavigate";
		
		public static const EDIT_QUERY_TERM:String = "editQueryTermEvent";
		
		public static const CANCEL_EDIT_QUERY_TERM:String = "cancelEditQueryTermEvent";
		
		public static const ADD_QUERY_TERM_TO_QUERY:String = "addQueryTermToQueryEvent";
		
		public static const UPDATE_QUERY_TERM:String = "updateQueryTermEvent";
		
		public static const RESET:String = "resetQueryEvent";
		
		public static const UPDATE_QUERY_FROM_WIDGET_DRAGDROP:String = "updateQueryFromWidgetDragDrop";
		
		
		//======================================
		// public properties 
		//======================================
		
		public var keywordString:String;
		
		public var keywordString2:String = Constants.BLANK;
		
		public var communityids:String;
		
		public var querySuggestion:QuerySuggestion;
		
		public var queryString:Object;
		
		public var typedQueryString:TypedQueryString;
		
		public var documentOptions:QueryOutputDocumentOptions;
		
		public var aggregationOptions:QueryOutputAggregationOptions;
		
		public var filterOptions:QueryOutputFilterOptions;
		
		public var scoreOptions:QueryScoreOptions;
		
		public var dragEvent:DragEvent;
		
		public var queryTerm:QueryTerm;
		
		public var queryTermObject:*;
		
		public var queryTerms:ArrayCollection;
		
		public var queryLogic:String;
		
		public var searchType:String;
		
		public var widgetInfo:WidgetDragObject;
		
		//======================================
		// constructor 
		//======================================
		
		public function QueryEvent( type:String, bubbles:Boolean = true, cancelable:Boolean = false, dialogControl:DialogControl = null, keywordString:String = null, keywordString2:String = null, communityids:String = null, querySuggestion:QuerySuggestion = null, queryString:Object = null, typedQueryString:TypedQueryString = null, documentOptions:QueryOutputDocumentOptions = null, aggregationOptions:QueryOutputAggregationOptions = null, filterOptions:QueryOutputFilterOptions = null, scoreOptions:QueryScoreOptions = null, dragEvent:DragEvent = null, queryTerm:QueryTerm = null, queryTermObject:* = null, queryTerms:ArrayCollection = null, queryLogic:String = null, searchType:String = null, widgetInfo:WidgetDragObject = null )
		{
			super( type, bubbles, cancelable, dialogControl );
			this.keywordString = keywordString;
			this.keywordString2 = keywordString2;
			this.communityids = communityids;
			this.querySuggestion = querySuggestion;
			this.queryString = queryString;
			this.typedQueryString = typedQueryString;
			this.documentOptions = documentOptions;
			this.aggregationOptions = aggregationOptions;
			this.filterOptions = filterOptions;
			this.scoreOptions = scoreOptions;
			this.dragEvent = dragEvent;
			this.queryTerm = queryTerm;
			this.queryTermObject = queryTermObject;
			this.queryTerms = queryTerms;
			this.queryLogic = queryLogic;
			this.searchType = searchType;
			this.widgetInfo = widgetInfo;
		}
		
		
		//======================================
		// public methods 
		//======================================
		
		override public function clone():Event
		{
			return new QueryEvent( type, bubbles, cancelable, dialogControl, keywordString, keywordString2, communityids, querySuggestion, queryString, typedQueryString, documentOptions, aggregationOptions, filterOptions, scoreOptions, dragEvent, queryTerm, queryTermObject, queryTerms, queryLogic, searchType );
		}
	}
}
