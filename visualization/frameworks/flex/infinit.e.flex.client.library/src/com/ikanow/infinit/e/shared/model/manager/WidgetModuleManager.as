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
package com.ikanow.infinit.e.shared.model.manager
{
	import com.ikanow.infinit.e.shared.event.QueryEvent;
	import com.ikanow.infinit.e.shared.event.WidgetEvent;
	import com.ikanow.infinit.e.shared.model.constant.Constants;
	import com.ikanow.infinit.e.shared.model.constant.QueryConstants;
	import com.ikanow.infinit.e.shared.model.constant.WidgetConstants;
	import com.ikanow.infinit.e.shared.model.manager.base.InfiniteManager;
	import com.ikanow.infinit.e.shared.model.vo.QueryScoreOptions;
	import com.ikanow.infinit.e.shared.model.vo.QueryString;
	import com.ikanow.infinit.e.shared.model.vo.QueryStringRequest;
	import com.ikanow.infinit.e.shared.model.vo.User;
	import com.ikanow.infinit.e.shared.model.vo.Widget;
	import com.ikanow.infinit.e.shared.model.vo.WidgetSummary;
	import com.ikanow.infinit.e.shared.model.vo.ui.ServiceResult;
	import com.ikanow.infinit.e.shared.util.CollectionUtil;
	import com.ikanow.infinit.e.shared.util.PDFGenerator;
	import com.ikanow.infinit.e.widget.library.data.SelectedInstance;
	import com.ikanow.infinit.e.widget.library.data.SelectedItem;
	import com.ikanow.infinit.e.widget.library.data.WidgetContext;
	import com.ikanow.infinit.e.widget.library.framework.InfiniteMaster;
	import com.ikanow.infinit.e.widget.library.frameworkold.ModuleInterface;
	import com.ikanow.infinit.e.widget.library.frameworkold.QueryResults;
	import com.ikanow.infinit.e.widget.library.widget.IResultSet;
	import com.ikanow.infinit.e.widget.library.widget.IWidget;
	import flash.display.DisplayObject;
	import flash.utils.setTimeout;
	import mx.collections.ArrayCollection;
	import mx.resources.IResourceManager;
	import mx.resources.ResourceManager;
	import mx.rpc.http.mxml.HTTPService;
	
	/**
	 * Widget Manager
	 */
	public class WidgetModuleManager extends InfiniteManager implements InfiniteMaster
	{
		
		//======================================
		// public properties 
		//======================================
		
		public var context:WidgetContext;
		
		[Bindable]
		/**
		 * The results returned from a query
		 */
		public var queryResult:Object;
		
		[Bindable]
		/**
		 * The current query string that is being modified for a new query
		 */
		public var currentQueryStringRequest:QueryStringRequest;
		
		private var _widgets:Array = [];
		
		public function get widgets():Array
		{
			return _widgets;
		}
		
		[Inject]
		public var setupManager:SetupManager;
		
		[Inject( "queryManager.scoreOptions", bind = "true" )]
		/**
		 * The advanced scoring options
		 */
		public var scoreOptions:QueryScoreOptions;
		
		[Inject( "communityManager.selectedCommunities", bind = "true" )]
		/**
		 * The collection of selected communities
		 */
		public var selectedCommunities:ArrayCollection;
		
		[Inject( "userManager.currentUser", bind = "true" )]
		/**
		 * The current user
		 */
		public var currentUser:User;
		
		[Bindable]
		/**
		 * The query results are filtered
		 */
		public var filterActive:Boolean;
		
		[Bindable]
		/**
		 * The tooltip for the current filter
		 */
		public var filterToolTip:String = Constants.BLANK;
		
		//======================================
		// protected properties 
		//======================================
		
		protected var selectedItem:SelectedItem;
		
		protected var doneFilterContext:Boolean;
		
		protected var resourceManager:IResourceManager = ResourceManager.getInstance();
		
		//======================================
		// private properties 
		//======================================
		
		private var _widgetUrls:Array = [];
		
		//======================================
		// constructor 
		//======================================
		
		public function WidgetModuleManager()
		{
			super();
			
			context = new WidgetContext( this );
		}
		
		
		//======================================
		// public methods 
		//======================================
		
		public function applyQueryToAllWidgets( queryResults:IResultSet ):void
		{
			filterActive = false;
			
			context.onNewQuery_internal( queryResults, null );
			
			for each ( var widget:IWidget in widgets )
			{
				widget.onReceiveNewQuery();
			}
		}
		
		public function clearWidgetFilters():void
		{
			filterActive = false;
			filterToolTip = Constants.BLANK;
			selectedItem = null;
			
			applyQueryToAllWidgets( context.getQuery_AllResults() );
		}
		
		/**
		 * ExportPDF
		 * Used to export to pdf
		 */
		public function exportPDF():void
		{
			// If no query results, exit
			if ( context.getQuery_AllResults() == null )
				return;
			
			// Get the currently enabled communities
			var groups:String = CollectionUtil.getStringFromArrayCollectionField( selectedCommunities, QueryConstants.NAME );
			var communityids:String = CollectionUtil.getStringFromArrayCollectionField( selectedCommunities );
			
			// Create the pdf generator
			var printPDF:PDFGenerator = new PDFGenerator();
			
			// Query logic for page 1
			printPDF.generateQueryPage( currentUser.displayName, groups, context );
			
			var widget:DisplayObject;
			var widgetParent:Object;
			
			// Looking for the doc browser, for page 2
			for each ( widget in widgets )
			{
				widgetParent = widget.parent.parent.parent as Object;
				
				if ( widgetParent.title == WidgetConstants.DOC_BROWSER )
				{
					printPDF.widgetToPdf( widget, widgetParent[ WidgetConstants.TITLE ] );
					break;
				}
			}
			
			// All open widgets, except the doc browser
			for each ( widget in widgets )
			{
				widgetParent = widget.parent.parent.parent as Object;
				
				if ( widgetParent.title != WidgetConstants.DOC_BROWSER )
				{
					printPDF.widgetToPdf( widget, widgetParent[ WidgetConstants.TITLE ] );
				}
			}
			
			// Appendix
			printPDF.generateAppendix( communityids, context );
			
			// Save
			printPDF.saveToPdf();
		}
		
		/**
		 * function to follow bread crumb based on the action that was clicked
		 *
		 * @param action The action to be processed
		 */
		public function followBreadcrumb( action:String ):void
		{
		
		}
		
		public function getCurrentQuery():Object
		{
			return currentQueryStringRequest.clone() as Object;
		}
		
		/**
		 * Gets the widget options for a widget that is loading
		 * @param value
		 */
		public function getWidgetModuleOptions( widgetUrl:String ):Object
		{
			var widgetOptions:Object = null;
			
			for each ( var url:String in _widgetUrls )
			{
				if ( url == widgetUrl )
				{
					var index:int = _widgetUrls.indexOf( url );
					widgetOptions = IWidget( _widgets[ index ] ).onSaveWidgetOptions();
				}
			}
			
			return widgetOptions;
		}
		
		public function invokeQueryEngine( queryObject:Object, widgetHttpService:HTTPService = null ):Boolean
		{
			return false;
		}
		
		public function loadUISetup():void
		{
		}
		
		public function loadWidget( widget:IWidget, widgetUrl:String ):void
		{
			var index:int = _widgetUrls.indexOf( widgetUrl );
			
			if ( index == -1 )
			{
				_widgets.push( widget );
				_widgetUrls.push( widgetUrl );
				
				widget.onInit( context );
				
				// load the widget options
				widget.onLoadWidgetOptions( setupManager.getSetupWidgetOptions( widgetUrl ) );
				
				// load the query results
				if ( queryResult )
					widget.onReceiveNewQuery();
				
				// if there is a selected item pass it to the widget
				if ( selectedItem )
				{
					var mod:ModuleInterface = widget as ModuleInterface;
					
					if ( mod != null )
					{
						mod.receiveSelectedItem( selectedItem );
					}
					else
					{
						widget.onReceiveNewFilter();
					}
				}
			}
			
			setTimeout( sortWidgets, 400 );
		}
		
		public function parentFlagFilterEvent():void
		{
			doneFilterContext = true;
		}
		
		public function parentReceiveSelectedItem( selectedItem:SelectedItem ):void
		{
			this.selectedItem = selectedItem;
			
			if ( !doneFilterContext ) // filter in context object (likely came from legacy widget) 
			{
				context.filterLegacy( selectedItem );
			}
			
			doneFilterContext = false;
			
			// get a selected item from a module and then distribute it to all modules	
			for each ( var widget:IWidget in widgets )
			{
				var mod:ModuleInterface = widget as ModuleInterface;
				
				if ( mod != null )
				{
					mod.receiveSelectedItem( selectedItem );
				}
				else
				{
					widget.onReceiveNewFilter();
				}
			}
			
			filterActive = true;
			
			updateFilterToolTip();
		}
		
		/**
		 * Reset
		 * Used to reset on logout
		 */
		public function reset():void
		{
			_widgets = [];
			_widgetUrls = [];
			selectedItem = null;
		}
		
		[Inject( "queryManager.currentQueryStringRequest", bind = "true" )]
		/**
		 * set the current query string
		 * @param value
		 */
		public function setCurrentQueryString( value:QueryStringRequest ):void
		{
			currentQueryStringRequest = value;
		}
		
		[Inject( "queryManager.queryResult", bind = "true" )]
		/**
		 * set the result returned from a query
		 * @param value
		 */
		public function setQueryResult( value:Object ):void
		{
			queryResult = value;
			
			var queryResults:QueryResults = new QueryResults();
			
			if ( value )
				queryResults.populateQueryResults( value, null, context );
			
			// set the results in the context and the widget modules
			context.onNewQuery( queryResults, QueryConstants.QUERY_RESULTS, currentQueryStringRequest as Object );
			applyQueryToAllWidgets( context.getQuery_AllResults() );
		}
		
		public function unloadWidget( widget:Widget ):void
		{
			var index:int = _widgetUrls.indexOf( widget.url );
			
			if ( index > -1 )
			{
				_widgets.splice( index, 1 );
				_widgetUrls.splice( index, 1 );
			}
			
			setTimeout( sortWidgets, 400 );
		}
		
		public function updateCurrentQuery( newQuery:Object, modifiedElements:String ):void
		{
			var queryEvent:QueryEvent;
			
			// update the scoring options or add query terms
			if ( modifiedElements && modifiedElements == QueryConstants.SCORE )
			{
				var scoreOptionsNew:QueryScoreOptions = scoreOptions.clone();
				
				if ( newQuery[ QueryConstants.SCORE ] )
				{
					// time decay
					if ( newQuery[ QueryConstants.SCORE ][ QueryConstants.TIME_PROXIMITY ] )
					{
						scoreOptionsNew.timeProx.time = newQuery[ QueryConstants.SCORE ][ QueryConstants.TIME_PROXIMITY ][ QueryConstants.TIME ];
						scoreOptionsNew.timeProx.decay = newQuery[ QueryConstants.SCORE ][ QueryConstants.TIME_PROXIMITY ][ QueryConstants.DECAY ];
					}
					
					// gei decay
					if ( newQuery[ QueryConstants.SCORE ][ QueryConstants.GEO_PROXIMITY ] )
					{
						scoreOptionsNew.geoProx.ll = newQuery[ QueryConstants.SCORE ][ QueryConstants.GEO_PROXIMITY ][ QueryConstants.LAT_LONG ];
						scoreOptionsNew.geoProx.decay = newQuery[ QueryConstants.SCORE ][ QueryConstants.GEO_PROXIMITY ][ QueryConstants.DECAY ];
					}
					
					// update the scoring options
					queryEvent = new QueryEvent( QueryEvent.SAVE_QUERY_ADVANCED_SCORING_SETTINGS );
					queryEvent.scoreOptions = scoreOptionsNew;
					dispatcher.dispatchEvent( queryEvent );
				}
			}
			else
			{
				// add query terms
				queryEvent = new QueryEvent( QueryEvent.ADD_QUERY_TERMS_TO_QUERY );
				queryEvent.queryTerms = new ArrayCollection( newQuery.qt );
				dispatcher.dispatchEvent( queryEvent );
			}
		
		}
		
		//======================================
		// protected methods 
		//======================================
		
		/**
		 * Sort the widgets in the widget drawer
		 */
		protected function sortWidgets():void
		{
			dispatcher.dispatchEvent( new WidgetEvent( WidgetEvent.SORT_WIDGETS ) );
		}
		
		/**
		 * Update the filter tool tip with the current filter info
		 * includes a max of 3 entities
		 */
		protected function updateFilterToolTip():void
		{
			var words:ArrayCollection = new ArrayCollection();
			var wordsArray:Array;
			var items:ArrayCollection = selectedItem.getSelectedInstances();
			var text:String = resourceManager.getString( "infinite", "widgetModuleManager.tooltip.currentFilter" );
			var inst:SelectedInstance;
			var ents:ArrayCollection;
			var entcount:int = 0;
			var totalentcount:int = 0;
			var totaldoccount:int = 0;
			
			if ( items.length > 0 )
			{
				for ( var j:int = 0; j < items.length; j++ )
				{
					totaldoccount++;
					inst = items.getItemAt( j ) as SelectedInstance;
					ents = inst.getEntities();
					
					for ( var i:int = 0; i < ents.length; i++ )
					{
						totalentcount++;
						words.addItem( ents.getItemAt( i ) );
						entcount++;
					}
				}
			}
			
			text += Constants.LINE_BREAK + resourceManager.getString( "infinite", "widgetModuleManager.tooltip.documents" ) + Constants.COLON + Constants.SPACE + totaldoccount;
			text += Constants.LINE_BREAK + resourceManager.getString( "infinite", "widgetModuleManager.tooltip.entities" ) + Constants.COLON + Constants.SPACE + totalentcount;
			
			if ( selectedItem.getDescription() != null )
			{
				text += Constants.LINE_BREAK + selectedItem.getDescription();
			}
			else
			{
				if ( words.length > 0 )
				{
					wordsArray = words.toArray();
					text += Constants.LINE_BREAK + resourceManager.getString( "infinite", "widgetModuleManager.tooltip.terms" ) + Constants.COLON + Constants.SPACE;
					
					for ( var k:int = 0; k < wordsArray.length && k < 3; k++ )
					{
						text += wordsArray[ k ] + Constants.COMMA + Constants.SPACE;
					}
				}
				
				if ( text.charAt( text.length - 2 ) == Constants.COMMA ) //check 2 spaces back because we add on the ,_
					text = text.substr( 0, text.length - 2 );
				
				if ( words.length > 3 )
					text += Constants.SPACE + Constants.PLUS + Constants.SPACE + resourceManager.getString( "infinite", "widgetModuleManager.tooltip.more" );
			}
			
			filterToolTip = text;
		}
	}
}
