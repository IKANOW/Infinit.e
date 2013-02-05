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
	import com.ikanow.infinit.e.shared.model.constant.NavigationConstants;
	import com.ikanow.infinit.e.shared.model.constant.QueryConstants;
	import com.ikanow.infinit.e.shared.model.constant.ServiceConstants;
	import com.ikanow.infinit.e.shared.model.constant.WidgetConstants;
	import com.ikanow.infinit.e.shared.model.manager.base.InfiniteManager;
	import com.ikanow.infinit.e.shared.model.vo.Community;
	import com.ikanow.infinit.e.shared.model.vo.Member;
	import com.ikanow.infinit.e.shared.model.vo.QueryScoreOptions;
	import com.ikanow.infinit.e.shared.model.vo.QueryString;
	import com.ikanow.infinit.e.shared.model.vo.QueryStringRequest;
	import com.ikanow.infinit.e.shared.model.vo.Share;
	import com.ikanow.infinit.e.shared.model.vo.User;
	import com.ikanow.infinit.e.shared.model.vo.Widget;
	import com.ikanow.infinit.e.shared.model.vo.WidgetSummary;
	import com.ikanow.infinit.e.shared.model.vo.ui.ServiceResponse;
	import com.ikanow.infinit.e.shared.model.vo.ui.ServiceResult;
	import com.ikanow.infinit.e.shared.util.CollectionUtil;
	import com.ikanow.infinit.e.shared.util.JSONUtil;
	import com.ikanow.infinit.e.shared.util.ObjectTranslatorUtil;
	import com.ikanow.infinit.e.shared.util.PDFGenerator;
	import com.ikanow.infinit.e.shared.util.QueryUtil;
	import com.ikanow.infinit.e.source.view.Sources;
	import com.ikanow.infinit.e.widget.library.data.SelectedInstance;
	import com.ikanow.infinit.e.widget.library.data.SelectedItem;
	import com.ikanow.infinit.e.widget.library.data.WidgetContext;
	import com.ikanow.infinit.e.widget.library.framework.InfiniteMaster;
	import com.ikanow.infinit.e.widget.library.framework.WidgetSaveObject;
	import com.ikanow.infinit.e.widget.library.frameworkold.ModuleInterface;
	import com.ikanow.infinit.e.widget.library.frameworkold.QueryResults;
	import com.ikanow.infinit.e.widget.library.utility.JSONDecoder;
	import com.ikanow.infinit.e.widget.library.utility.JSONEncoder;
	import com.ikanow.infinit.e.widget.library.utility.URLEncoder;
	import com.ikanow.infinit.e.widget.library.widget.IResultSet;
	import com.ikanow.infinit.e.widget.library.widget.IWidget;
	import flash.display.DisplayObject;
	import flash.utils.setTimeout;
	import mx.collections.ArrayCollection;
	import mx.controls.Alert;
	import mx.resources.IResourceManager;
	import mx.resources.ResourceManager;
	import mx.rpc.events.ResultEvent;
	import mx.rpc.http.mxml.HTTPService;
	import system.data.Map;
	import system.data.maps.HashMap;
	
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
		
		public var widgetSaveMap:Map = null;
		
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
		 * The list of active communities has changed so we can now update what save options we give to the widgets
		 */
		public function communitiesUpdated():void
		{
			//send load messages to any widget that has already been loaded
			for ( var i:int = 0; i < _widgetUrls.length; i++ )
			{
				var widgetUrl:String = _widgetUrls[ i ];
				var widget:IWidget = _widgets[ i ] as IWidget;
				widget.onLoadWidgetOptions( setupManager.getSetupWidgetOptions( widgetUrl ) );
			}
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
			return QueryUtil.getQueryStringObject( currentQueryStringRequest );
			//return currentQueryStringRequest.clone() as Object;
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
		
		
		public function saveWidgetModuleOptions( widgetTitle:String, widgetURL:String, widgetAuthor:String ):void
		{
			for each ( var url:String in _widgetUrls )
			{
				if ( url == widgetURL )
				{
					var index:int = _widgetUrls.indexOf( url );
					var widgetOptions:Object = IWidget( _widgets[ index ] ).onSaveWidgetOptions();
					
					if ( widgetOptions != null )
					{
						//save widget options to share, possibly overwriting old widget options
						saveWidgetOptionsToShare( widgetTitle, widgetOptions )
					}
					break;
				}
			}
		}
		
		public function saveWidgetOptionsToShare( savetitle:String, saveobject:Object ):void
		{
			//only save if the widgetSaveMap has been initialized, this means that the
			//load uisetup has come in
			if ( widgetSaveMap != null )
			{
				var desc:String = "save options for the widget";
				var id:String = "null";
				var save:WidgetSaveObject = null;
				
				if ( widgetSaveMap.containsKey( savetitle ) )
				{
					save = widgetSaveMap.get( savetitle ) as WidgetSaveObject;
					id = save.shareid;
				}
				else
				{
					save = new WidgetSaveObject();
				}
				
				//put the share object back in the map
				//or add a new entry if necessary
				var share:Share = new Share();
				share._id = id;
				share.title = savetitle;
				share.owner = new Member();
				share.owner._id = currentUser._id;
				var json:String = JSONUtil.encode( saveobject );
				share.share = json;
				save.userSave = saveobject;
				widgetSaveMap.put( savetitle, save );
				
				var httpService:HTTPService = new HTTPService();
				httpService.url = ServiceConstants.GET_SOCIAL_SHARE_ADD_JSON_URL + id + "/" + "widgetsave/" + URLEncoder.encode( savetitle ) + "/" + URLEncoder.encode( desc );
				httpService.method = "POST";
				httpService.contentType = "application/json";
				httpService.addEventListener( ResultEvent.RESULT, widgetSaveResultHandler );
				httpService.send( json );
			}
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
		
		public function setWidgetOptions( shares:ArrayCollection ):void
		{
			//only try to save options if the user is already set
			if ( currentUser != null )
			{
				//initialize the save map
				widgetSaveMap = new HashMap();
				
				if ( shares != null && shares.length > 0 )
				{
					for each ( var share:Share in shares )
					{
						//check all the communities on a share, if it is in your personal community
						//then it is a user share object, otherwise its a communities share object
						var isUser:Boolean = false;
						
						for each ( var community:Community in share.communities )
						{
							if ( community._id == currentUser._id )
							{
								isUser = true;
								break;
							}
						}
						
						if ( !widgetSaveMap.containsKey( share.title ) )
						{
							//initialize save entry if it hasn't been already
							widgetSaveMap.put( share.title, new WidgetSaveObject() );
						}
						var widgetSave:WidgetSaveObject = widgetSaveMap.get( share.title );
						
						if ( isUser ) //is users personal share
						{
							//set the users share object, if for some reason
							//a user has 2 saves, this will overwrite any
							//previous one, they should only have 1
							widgetSave.userSave = JSONDecoder.decode( share.share );
							widgetSave.shareid = share._id;
						}
						else //is community share
						{
							//add an entry for every community, if a community
							//has 2 shares for a widget this will overwrite any
							//previous one, they should only have 1 but there is
							//nothing stopping an owner from submitting 2
							for each ( var comm:Community in share.communities )
							{
								widgetSave.communitySave.put( comm._id, JSONDecoder.decode( share.share ) );
							}
						}
					}
					
					//send load messages to any widget that has already been loaded
					for ( var i:int = 0; i < _widgetUrls.length; i++ )
					{
						var widgetUrl:String = _widgetUrls[ i ];
						var widget:IWidget = _widgets[ i ] as IWidget;
						widget.onLoadWidgetOptions( setupManager.getSetupWidgetOptions( widgetUrl ) );
					}
				}
			}
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
			var queryEvent:QueryEvent = new QueryEvent( QueryEvent.UPDATE_QUERY_NAVIGATE );
			
			var queryString:QueryString = ObjectTranslatorUtil.translateObject( newQuery, new QueryString, null, false, true ) as QueryString;
			queryEvent.queryString = queryString;
			
			// Default event: update query, pop up nothing
			
			if ( modifiedElements )
			{
				if ( modifiedElements.indexOf( QueryConstants.INPUT_OPTIONS ) >= 0 )
				{
					// Update-query-and-pop-up-source-manager						
					queryEvent.searchType = NavigationConstants.WORKSPACES_SOURCES_ID;
				}//TESTED
				else if ( ( modifiedElements.indexOf( QueryConstants.SCORING_OPTIONS ) >= 0 )
					|| ( modifiedElements.indexOf( QueryConstants.OUTPUT_OPTIONS ) >= 0 ) )
				{
					// Update-query-and-pop-up-query-settings	
					queryEvent.searchType = NavigationConstants.WORKSPACE_SETTINGS_ID;
				}//TESTED
				else if ( modifiedElements.indexOf( QueryConstants.QUERY_TERMS ) >= 0 )
				{
					// Update-query-and-pop-up-advanced-query-terms
					queryEvent.searchType = NavigationConstants.QUERY_BUILDER_ID;
				}//TESTED
			}
			dispatcher.dispatchEvent( queryEvent );
		
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
		
		//======================================
		// private methods 
		//======================================
		
		/**
		 * Write back new id to share handler if necessa
		 **/
		private function widgetSaveResultHandler( event:ResultEvent ):void
		{
			var json:Object = JSONDecoder.decode( event.result as String );
			var response:ServiceResponse = ObjectTranslatorUtil.translateObject( json.response, new ServiceResponse ) as ServiceResponse;
			
			if ( response.responseSuccess )
			{
				var share:Share = Share( ObjectTranslatorUtil.translateObject( json.data, new Share ) );
				
				//next put the share into the map
				var save:WidgetSaveObject = widgetSaveMap.get( share.title );
				save.shareid = share._id;
			}
		}
	}
}
