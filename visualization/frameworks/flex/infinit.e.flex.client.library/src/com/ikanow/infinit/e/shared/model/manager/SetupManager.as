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
	import com.ikanow.infinit.e.shared.event.SetupEvent;
	import com.ikanow.infinit.e.shared.model.constant.Constants;
	import com.ikanow.infinit.e.shared.model.constant.ServiceConstants;
	import com.ikanow.infinit.e.shared.model.constant.WidgetConstants;
	import com.ikanow.infinit.e.shared.model.manager.base.InfiniteManager;
	import com.ikanow.infinit.e.shared.model.vo.QueryStringRequest;
	import com.ikanow.infinit.e.shared.model.vo.Setup;
	import com.ikanow.infinit.e.shared.model.vo.Share;
	import com.ikanow.infinit.e.shared.model.vo.Widget;
	import com.ikanow.infinit.e.shared.model.vo.WidgetSummary;
	import com.ikanow.infinit.e.shared.model.vo.ui.DialogControl;
	import com.ikanow.infinit.e.shared.util.CollectionUtil;
	import com.ikanow.infinit.e.shared.util.JSONUtil;
	import com.ikanow.infinit.e.shared.util.QueryUtil;
	import com.ikanow.infinit.e.shared.util.ServiceUtil;
	import com.ikanow.infinit.e.widget.library.utility.JSONDecoder;
	import com.ikanow.infinit.e.widget.library.widget.IWidget;
	import flash.events.Event;
	import mx.collections.ArrayCollection;
	import mx.collections.ListCollectionView;
	import mx.collections.Sort;
	import mx.collections.SortField;
	import mx.resources.ResourceManager;
	
	/**
	 * Setup Manager
	 */
	public class SetupManager extends InfiniteManager
	{
		
		//======================================
		// public properties 
		//======================================
		
		[Bindable]
		/**
		 * The UI Setup
		 */
		public var setup:Setup;
		
		[Bindable]
		/**
		 * The user widgets
		 */
		public var userWidgets:ArrayCollection;
		
		private var _widgets:ArrayCollection;
		
		[Bindable( "widgetsChanged" )]
		/**
		 * The available widgets
		 */
		public function get widgets():ArrayCollection
		{
			return _widgets;
		}
		
		private var _selectedWidgets:ArrayCollection = new ArrayCollection();
		
		[Bindable( "selectedWidgetsChanged" )]
		/**
		 * The selected widgets
		 */
		public function get selectedWidgets():ArrayCollection
		{
			return _selectedWidgets;
		}
		
		[Inject( "communityManager.selectedCommunities", bind = "true" )]
		/**
		 * The collection of selected communities
		 */
		public var selectedCommunities:ArrayCollection;
		
		[Inject( "queryManager.lastQueryStringRequest", bind = "true" )]
		/**
		 * The last query string that was performed
		 */
		public var lastQueryStringRequest:QueryStringRequest;
		
		[Inject]
		public var widgetModuleManager:WidgetModuleManager;
		
		//======================================
		// protected properties 
		//======================================
		
		/**
		 * Flag that controls showing the debug or regular widgets based on the config setting
		 */
		protected var debugMode:Boolean = CONFIG::debug;
		
		
		//======================================
		// public methods 
		//======================================
		
		/**
		 * Gets the widget options for a widget that is loading
		 * @param value
		 */
		public function getSetupWidgetOptions( widgetUrl:String ):Object
		{
			var widgetOptions:Object = null;
			
			if ( setup && setup.openModules )
			{
				for each ( var widget:Widget in selectedWidgets )
				{
					if ( widget.url == widgetUrl )
					{
						if ( widgetModuleManager.widgetSaveMap.containsKey( widget.title ) )
						{
							return JSONDecoder.decode( ( widgetModuleManager.widgetSaveMap.get( widget.title ) as Share ).share );
						}
						
					}
				}
			}
			
			return widgetOptions;
		}
		
		/**
		 * Reset
		 * Used to reset on logout
		 */
		public function reset():void
		{
			setup = null;
			_widgets = new ArrayCollection();
			dispatchEvent( new Event( "widgetsChanged" ) );
			_selectedWidgets = new ArrayCollection();
			dispatchEvent( new Event( "selectedWidgetsChanged" ) );
		
		}
		
		/**
		 * Save the ui setup
		 * @param value
		 */
		public function saveSetup():void
		{
			var url:String = Constants.BLANK;
			
			// add the selected widgets
			if ( selectedWidgets && selectedWidgets.length > 0 )
			{
				// sort the widgets by the position index
				var lcv:ListCollectionView = new ListCollectionView( selectedWidgets );
				var sortOrderSortField:SortField = new SortField();
				sortOrderSortField.name = Constants.POSITION_INDEX_PROPERTY;
				sortOrderSortField.numeric = true;
				lcv.sort = new Sort();
				lcv.sort.fields = [ sortOrderSortField ];
				lcv.refresh();
				
				for each ( var widget:Widget in lcv )
				{
					if ( url != Constants.BLANK )
						url += ServiceConstants.SERVICE_RIGHT_BRACE_DELIMITER;
					
					// New way of encoding widget info: just pass the _id in and nothing else
					url += ServiceUtil.urlEncode( widget._id );
					url += Constants.STRING_ARRAY_DELIMITER;
					url += Constants.STRING_ARRAY_DELIMITER;
					url += Constants.STRING_ARRAY_DELIMITER;
					
					// Legacy code, may put back in at some point
					// Mainly removed because ".." can't be encoded and sent in a URL by flex for some odd reason
					//url += ServiceUtil.urlEncode( widget.title );
					//url += Constants.STRING_ARRAY_DELIMITER + ServiceUtil.urlEncode( widget.url );
					//url += Constants.STRING_ARRAY_DELIMITER + ServiceUtil.urlEncode( widget.description );
					//url += Constants.STRING_ARRAY_DELIMITER + ServiceUtil.urlEncode( widget.imageurl );						
					
					// the following properties are not needed for this version of the application ( widget - x, y, width, height )
					url += Constants.STRING_ARRAY_DELIMITER + "0"; // x
					url += Constants.STRING_ARRAY_DELIMITER + "0"; // y
					url += Constants.STRING_ARRAY_DELIMITER + "600"; // width
					url += Constants.STRING_ARRAY_DELIMITER + "600"; // height
					
					// widget options
					//BURCH OLD WIDGET OPTIONS, Removed to save to shares, just return an empty widget options now
					//var widgetOptions:Object = widgetModuleManager.getWidgetModuleOptions( widget.url );
					//url += Constants.STRING_ARRAY_DELIMITER + JSONUtil.encode( widgetOptions );
					url += Constants.STRING_ARRAY_DELIMITER + JSONUtil.encode( null );
					widgetModuleManager.saveWidgetModuleOptions( widget.title, widget.url, widget.author );
				}
			}
			else
			{
				url = Constants.NULL_STRING;
			}
			
			// add a separator between the widget and community params
			url += Constants.FORWARD_SLASH;
			
			// add the selected community ids
			url += CollectionUtil.getStringFromArrayCollectionField( selectedCommunities );
			
			// update the ui setup
			var setupEvent:SetupEvent = new SetupEvent( SetupEvent.UPDATE_SETUP );
			setupEvent.urlParams = url;
			
			if ( lastQueryStringRequest )
				setupEvent.queryString = QueryUtil.getQueryStringObject( lastQueryStringRequest );
			
			setupEvent.dialogControl = DialogControl.create( false, ResourceManager.getInstance().getString( 'infinite', 'setupService.updateSetup' ) );
			dispatcher.dispatchEvent( setupEvent );
			
			// save the user widgets
			saveUserWidgets();
		}
		
		public function saveWidgetOptions( shares:ArrayCollection ):void
		{
			widgetModuleManager.setWidgetOptions( shares );
		}
		
		/**
		 * Setup for the ui
		 * @param value
		 */
		public function setSetup( value:Setup ):void
		{
			setup = value;
		}
		
		/**
		 * Set User Widget
		 * @param value
		 */
		public function setUserWidget( widgetId:String, favorite:Boolean ):void
		{
			var widget:Widget = CollectionUtil.getItemById( widgets, widgetId ) as Widget;
			widget.favorite = favorite;
			
			saveUserWidgets();
		}
		
		/**
		 * User Widgets Collection
		 * @param value
		 */
		public function setUserWidgets( value:ArrayCollection ):void
		{
			userWidgets = createWidgetCollection( value );
			
			updateFavoriteWidgets();
		}
		
		/**
		 * Widgets Master Collection
		 * @param value
		 */
		public function setWidgets( value:ArrayCollection ):void
		{
			// remove "Dev:" prefix
			for each ( var widget:Widget in value )
			{
				widget.title = widget.title.replace( WidgetConstants.DEV_WIDGET_PREFIX, Constants.BLANK );
			}
			
			_widgets = createWidgetCollection( value );
			
			updateSelectedWidgets( widgets );
			
			updateFavoriteWidgets();
			
			dispatchEvent( new Event( "widgetsChanged" ) );
		}
		
		
		/**
		 * Refresh the widgets so that they get sorted in the widget list drawer
		 */
		public function sortWidgets():void
		{
			for each ( var widget:Widget in widgets )
			{
				widget.sortOrder = widget.favorite ? 0 : 1;
			}
			
			dispatchEvent( new Event( "widgetsChanged" ) );
		}
		
		//======================================
		// protected methods 
		//======================================
		
		/**
		 * Only include debug or regular widgets based on the debugMode property
		 * @param value
		 */
		protected function createWidgetCollection( collection:ArrayCollection ):ArrayCollection
		{
			var widgetsNew:ArrayCollection = new ArrayCollection();
			
			for each ( var widget:Widget in collection )
			{
				widget.url = widget.url.replace( ServiceConstants.SERVER_URL_SUBSTITUTE, ServiceConstants.SERVER_URL );
				widget.imageurl = widget.imageurl.replace( ServiceConstants.SERVER_URL_SUBSTITUTE, ServiceConstants.SERVER_URL );
				
				if ( widget.debug == debugMode )
					widgetsNew.addItem( widget );
			}
			
			return widgetsNew;
		}
		
		/**
		 * Save User Widget
		 * @param value
		 */
		protected function saveUserWidgets():void
		{
			var widgetIds:String = Constants.BLANK;
			
			if ( widgets && userWidgets )
			{
				for each ( var widget:Widget in widgets )
				{
					
					if ( widget.selected || widget.favorite )
					{
						if ( widgetIds != Constants.BLANK )
						{
							widgetIds += Constants.COMMA;
						}
						
						widgetIds += widget._id;
					}
				}
				
				// update the user widgets
				var setupEvent:SetupEvent = new SetupEvent( SetupEvent.SET_MODULES_USER );
				setupEvent.userModules = widgetIds;
				setupEvent.dialogControl = DialogControl.create( false, ResourceManager.getInstance().getString( 'infinite', 'setupService.setUserModules' ) );
				dispatcher.dispatchEvent( setupEvent );
			}
		}
		
		/**
		 * Set the favorite property for user widgets
		 * @param value
		 */
		protected function updateFavoriteWidgets():void
		{
			if ( widgets && userWidgets )
			{
				for each ( var userWidget:Widget in userWidgets )
				{
					for each ( var widget:Widget in widgets )
					{
						if ( widget._id == userWidget._id )
						{
							widget.favorite = true;
							widget.sortOrder = 0;
						}
					}
				}
				
				dispatchEvent( new Event( "widgetsChanged" ) );
			}
		}
		
		/**
		 * Set the selected property for widgets that are open
		 * @param value
		 */
		protected function updateSelectedWidgets( collection:ArrayCollection ):void
		{
			var positionIndex:int;
			var widget:Widget;
			
			if ( collection && setup && setup.openModules )
			{
				for each ( var widgetSummary:WidgetSummary in setup.openModules )
				{
					for each ( widget in collection )
					{
						// Normally, widgets will be passed in by _id, but can also be passed in by title etc etc
						var widgetMatch:Boolean = widgetSummary.widgetTitle == widget._id;
						
						if ( !widgetMatch && ( widgetSummary.widgetUrl != Constants.BLANK ) )
						{
							// In this case need to match on URL (so first transform "setup" URL)
							widgetSummary.widgetUrl = widgetSummary.widgetUrl.replace( ServiceConstants.SERVER_URL_SUBSTITUTE, ServiceConstants.SERVER_URL );
							widgetMatch = widgetSummary.widgetUrl == widget.url;
						}
						
						if ( widgetMatch && _selectedWidgets.length < 4 )
						{
							widget.selected = true;
							widget.favorite = true;
							widget.sortOrder = 0;
							widget.positionIndex = positionIndex;
							_selectedWidgets.addItem( widget );
							positionIndex++;
						}
					}
				}
			}
			
			for each ( widget in _selectedWidgets )
			{
				widget.parentCollectionCount = _selectedWidgets.length;
			}
			
			dispatchEvent( new Event( "selectedWidgetsChanged" ) );
		}
	}
}
