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
package com.ikanow.infinit.e.shared.model.presentation.base
{
	import com.ikanow.infinit.e.shared.event.NavigationEvent;
	import com.ikanow.infinit.e.shared.model.constant.types.NavigationItemTypes;
	import com.ikanow.infinit.e.shared.model.vo.ui.INavigationItem;
	import com.ikanow.infinit.e.shared.model.vo.ui.NavigationItem;
	import flash.events.Event;
	import flash.events.EventDispatcher;
	import flash.events.IEventDispatcher;
	import mx.collections.ArrayCollection;
	
	/**
	 * Navigator
	 * Base class for navigators
	 */
	public class Navigator extends EventDispatcher implements INavigator
	{
		
		//======================================
		// private static properties 
		//======================================
		
		private static const NO_DEFAULT:String = "";
		
		
		//======================================
		// public properties 
		//======================================
		
		// registered ------------------------------------------------------------
		private var _registered:Boolean;
		
		/**
		 *
		 * @return
		 */
		public function get registered():Boolean
		{
			return _registered;
		}
		
		/**
		 * Registered flag
		 * This is set to true after the navigator has been registered
		 * with the navigation controller
		 * @param value
		 */
		public function set registered( value:Boolean ):void
		{
			_registered = value;
		}
		
		// navigator id ------------------------------------------------------------
		private var _navigatorId:String = "";
		
		/**
		 *
		 * @return
		 */
		public function get navigatorId():String
		{
			return _navigatorId;
		}
		
		/**
		 * Navigator ID
		 * Used to associate the navigationItems to this navigator
		 * when a NavigationEvent.NAVIGATE event is dispatched
		 * @param value
		 */
		public function set navigatorId( value:String ):void
		{
			_navigatorId = value;
		}
		
		// parent navigator id ------------------------------------------------------------
		private var _parentNavigatorId:String = "";
		
		/**
		 *
		 * @return
		 */
		public function get parentNavigatorId():String
		{
			return _parentNavigatorId;
		}
		
		/**
		 * Parent Navigator ID
		 * The id of the parent Navigator
		 * Used for deep linking
		 * @param value
		 */
		public function set parentNavigatorId( value:String ):void
		{
			_parentNavigatorId = value;
		}
		
		// default state --------------------------------------------------------------
		private var _defaultState:String = NO_DEFAULT;
		
		/**
		 *
		 * @return
		 */
		public function get defaultState():String
		{
			return _defaultState;
		}
		
		/**
		 * Default State
		 * Used by the navigateToDefaultState method
		 * @param value
		 */
		public function set defaultState( value:String ):void
		{
			_defaultState = value;
		}
		
		// default view id ------------------------------------------------------------
		private var _defaultView:String = "";
		
		/**
		 *
		 * @return
		 */
		public function get defaultView():String
		{
			return _defaultView;
		}
		
		/**
		 * Default View
		 * Used by the navigateToDefaultView method
		 * @param value
		 */
		public function set defaultView( value:String ):void
		{
			_defaultView = value;
		}
		
		// navigation items --------------------------------------------
		private var _navigationItems:ArrayCollection = new ArrayCollection();
		
		[Bindable( event = "navigationItemsChange" )]
		/**
		 *
		 * @return
		 */
		public function get navigationItems():ArrayCollection
		{
			return _navigationItems;
		}
		
		/**
		 * Navigation Items
		 * A collection of all of the Navigation Items
		 * Used by navigateById
		 * @param value
		 */
		public function set navigationItems( value:ArrayCollection ):void
		{
			if ( _navigationItems != value )
			{
				_navigationItems = value;
				dispatchEvent( new Event( "navigationItemsChange" ) );
			}
		}
		
		// states ---------------------------------------------------------
		private var _states:ArrayCollection = new ArrayCollection();
		
		[Bindable( event = "statesChange" )]
		/**
		 *
		 * @return
		 */
		public function get states():ArrayCollection
		{
			return _states;
		}
		
		/**
		 * The collection of States
		 * @param value
		 */
		public function set states( value:ArrayCollection ):void
		{
			if ( _states != value )
			{
				_states = value;
				dispatchEvent( new Event( "statesChange" ) );
			}
		}
		
		// views ----------------------------------------------------------
		private var _views:ArrayCollection = new ArrayCollection();
		
		[Bindable( event = "viewsChange" )]
		/**
		 *
		 * @return
		 */
		public function get views():ArrayCollection
		{
			return _views;
		}
		
		/**
		 * The collection of Views
		 * @param value
		 */
		public function set views( value:ArrayCollection ):void
		{
			if ( _views != value )
			{
				_views = value;
				dispatchEvent( new Event( "statesChange" ) );
			}
		}
		
		// dialogs ----------------------------------------------------------
		private var _dialogs:ArrayCollection = new ArrayCollection();
		
		[Bindable( event = "dialogsChange" )]
		/**
		 *
		 * @return
		 */
		public function get dialogs():ArrayCollection
		{
			return _dialogs;
		}
		
		/**
		 * The collection of Dialogs
		 * @param value
		 */
		public function set dialogs( value:ArrayCollection ):void
		{
			if ( _dialogs != value )
			{
				_dialogs = value;
				dispatchEvent( new Event( "dialogsChange" ) );
			}
		}
		
		// actions ----------------------------------------------------------
		private var _actions:ArrayCollection = new ArrayCollection();
		
		[Bindable( event = "actionsChange" )]
		/**
		 *
		 * @return
		 */
		public function get actions():ArrayCollection
		{
			return _actions;
		}
		
		/**
		 * The collection of Actions
		 * @param value
		 */
		public function set actions( value:ArrayCollection ):void
		{
			if ( _actions != value )
			{
				_actions = value;
				dispatchEvent( new Event( "actionsChange" ) );
			}
		}
		
		// selected index -------------------------------------------------
		private var _selectedIndex:int = -1;
		
		[Bindable( event = "selectedIndexChange" )]
		/**
		 *
		 * @return
		 */
		public function get selectedIndex():int
		{
			return _selectedIndex;
		}
		
		/**
		 * The Selected View Index
		 * @param value
		 */
		public function set selectedIndex( value:int ):void
		{
			if ( _selectedIndex != value )
			{
				_selectedIndex = value;
				dispatchEvent( new Event( "selectedIndexChange" ) );
			}
		}
		
		// current state ----------------------------------------------
		private var _currentState:String = "";
		
		[Bindable( event = "currentStateChange" )]
		/**
		 *
		 * @return
		 */
		public function get currentState():String
		{
			return _currentState;
		}
		
		/**
		 * The Current State
		 * @param value
		 */
		public function set currentState( value:String ):void
		{
			if ( _currentState != value )
			{
				// set previous state
				previousState = _currentState;
				
				_currentState = value;
				dispatchEvent( new Event( "currentStateChange" ) );
			}
		}
		
		// current State index -------------------------------------------
		private var _currentStateIndex:int = -1;
		
		[Bindable( event = "currentStateIndexChange" )]
		/**
		 *
		 * @return
		 */
		public function get currentStateIndex():int
		{
			return _currentStateIndex;
		}
		
		/**
		 * The Selected Index for the Current State
		 * @param value
		 */
		public function set currentStateIndex( value:int ):void
		{
			if ( _currentStateIndex != value )
			{
				_currentStateIndex = value;
				dispatchEvent( new Event( "currentStateIndexChange" ) );
			}
		}
		
		// previous state -------------------------------------------------
		private var _previousState:String = "";
		
		[Bindable( event = "previousStateChange" )]
		/**
		 *
		 * @return
		 */
		public function get previousState():String
		{
			return _previousState;
		}
		
		/**
		 * The Previous State
		 * @param value
		 */
		public function set previousState( value:String ):void
		{
			if ( _previousState != value )
			{
				_previousState = value;
				dispatchEvent( new Event( "previousStateChange" ) );
			}
		}
		
		// selected dialog index -------------------------------------------------
		private var _selectedDialogIndex:int = -1;
		
		[Bindable( event = "selectedDialogIndexChange" )]
		/**
		 *
		 * @return
		 */
		public function get selectedDialogIndex():int
		{
			return _selectedDialogIndex;
		}
		
		/**
		 * The Selected Dialog Index
		 * @param value
		 */
		public function set selectedDialogIndex( value:int ):void
		{
			_selectedDialogIndex = value;
			dispatchEvent( new Event( "selectedDialogIndexChange" ) );
		}
		
		// selected action index -------------------------------------------------
		private var _selectedActionIndex:int = -1;
		
		[Bindable( event = "selectedActionIndexChange" )]
		/**
		 *
		 * @return
		 */
		public function get selectedActionIndex():int
		{
			return _selectedActionIndex;
		}
		
		/**
		 * The Selected Action Index
		 * @param value
		 */
		public function set selectedActionIndex( value:int ):void
		{
			_selectedActionIndex = value;
			dispatchEvent( new Event( "selectedActionIndexChange" ) );
		}
		
		// dispatcher ----------------------------------------------------
		private var _dispatcher:IEventDispatcher;
		
		[Dispatcher]
		/**
		 *
		 * @return
		 */
		public function get dispatcher():IEventDispatcher
		{
			return _dispatcher;
		}
		
		/**
		 * Dispatcher
		 * @param value
		 */
		public function set dispatcher( value:IEventDispatcher ):void
		{
			if ( value != null )
			{
				_dispatcher = value;
				
				// register the navigator with the navigation controller
				register();
			}
		}
		
		
		//======================================
		// public methods 
		//======================================
		
		// change action ------------------------------------------------
		/**
		 * Change Action
		 * Used by the Navigation Manager to change the selected action index
		 * @param navigationItem
		 * @return
		 */
		public function changeAction( navigationItem:INavigationItem ):Boolean
		{
			var newIndex:int = getItemIndex( getItemById( navigationItem.id ) );
			
			if ( selectedActionIndex != newIndex )
			{
				selectedActionIndex = newIndex;
				return true;
			}
			
			return false;
		}
		
		// change dialog ------------------------------------------------
		/**
		 * Change View
		 * Used by the Navigation Manager to change the selected dialog index
		 * @param navigationItem
		 * @return
		 */
		public function changeDialog( navigationItem:INavigationItem ):Boolean
		{
			var newIndex:int = getItemIndex( getItemById( navigationItem.id ) );
			
			if ( selectedDialogIndex != newIndex )
			{
				selectedDialogIndex = newIndex;
				return true;
			}
			
			return false;
		}
		
		// change state -----------------------------------------------
		/**
		 * Change State
		 * Used by the Navigation Manager to change the current state
		 */
		public function changeState( state:String ):Boolean
		{
			var newIndex:int = state == NO_DEFAULT ? -1 : getItemIndex( getItemById( state ) );
			
			if ( currentStateIndex != newIndex )
			{
				currentStateIndex = newIndex;
			}
			
			if ( currentState != state )
			{
				currentState = state;
				return true;
			}
			
			return false;
		}
		
		// change view ------------------------------------------------
		/**
		 * Change View
		 * Used by the Navigation Manager to change the selected index
		 * @param navigationItem
		 * @return
		 */
		public function changeView( navigationItem:INavigationItem ):Boolean
		{
			var newIndex:int = getItemIndex( getItemById( navigationItem.id ) );
			
			if ( selectedIndex != newIndex )
			{
				selectedIndex = newIndex;
				return true;
			}
			
			return false;
		}
		
		// get Item by id ----------------------------------------------
		/**
		 * Get Item By Id
		 * Returns the navigation item for a navigation item id
		 * @param navigationItemId
		 * @return
		 */
		public function getItemById( navigationItemId:String ):INavigationItem
		{
			for each ( var navigationItem:INavigationItem in navigationItems )
			{
				if ( navigationItem.id == navigationItemId )
				{
					return navigationItem;
				}
			}
			
			return null;
		}
		
		// get Item by state ----------------------------------------------
		/**
		 * Get Item By State
		 * Returns the navigation item for a state
		 * @param state
		 * @return
		 */
		public function getItemByState( state:String ):INavigationItem
		{
			for each ( var navigationItem:INavigationItem in navigationItems )
			{
				if ( navigationItem.state == state )
				{
					return navigationItem;
				}
			}
			
			return null;
		}
		
		// get item index ---------------------------------------------
		/**
		 * Get Item Index
		 * Returns the index for a navigation item
		 * @param navigationItem
		 * @return
		 */
		public function getItemIndex( navigationItem:INavigationItem ):int
		{
			var index:int = -1;
			
			if ( navigationItem )
			{
				switch ( navigationItem.type )
				{
					case NavigationItemTypes.STATE:
						index = states.getItemIndex( navigationItem );
						break;
					case NavigationItemTypes.VIEW:
						index = views.getItemIndex( navigationItem );
						break;
					case NavigationItemTypes.DIALOG:
						index = dialogs.getItemIndex( navigationItem );
						break;
					case NavigationItemTypes.ACTION:
						index = actions.getItemIndex( navigationItem );
						break;
					default:
						break;
				}
			}
			
			return index;
		}
		
		// initialize  ------------------------------------------------
		/**
		 * Initialize
		 * Used to reset and create the navigation items
		 */
		public function initialize():void
		{
			reset();
			
			createStates();
			createViews();
			createDialogs();
			createActions();
		}
		
		// navigate ---------------------------------------------------
		/**
		 * Used to navigate to a navigation item
		 * @param navigationItem
		 */
		public function navigate( navigationItem:* ):void
		{
			if ( navigationItem is INavigationItem )
			{
				var navigationEvent:NavigationEvent = new NavigationEvent( NavigationEvent.NAVIGATE );
				navigationEvent.navigationItem = navigationItem;
				dispatcher.dispatchEvent( navigationEvent );
			}
		}
		
		// navigate by id -------------------------------------------
		/**
		 * Used to Navigate by a navigationItemId
		 * @param navigationItemId
		 */
		public function navigateById( navigationItemId:String ):void
		{
			var navigationEvent:NavigationEvent = new NavigationEvent( NavigationEvent.NAVIGATE_BY_ID );
			navigationEvent.navigationItemId = navigationItemId;
			dispatcher.dispatchEvent( navigationEvent );
		}
		
		// navigate to default state ---------------------------------------------
		/**
		 * Navigate to the default state
		 */
		public function navigateToDefaultState():void
		{
			changeState( defaultState );
			
			var defaultStateIndex:int = defaultState == NO_DEFAULT ? -1 : getItemIndex( getItemById( defaultState ) );
			
			if ( defaultStateIndex != -1 )
			{
				currentStateIndex = defaultStateIndex;
			}
			else if ( views.length > 0 && defaultState != NO_DEFAULT )
			{
				currentStateIndex = 0;
			}
			else
			{
				currentStateIndex = -1;
			}
		}
		
		// navigate to default view ---------------------------------------------
		/**
		 * Navigate to the default view
		 */
		public function navigateToDefaultView():void
		{
			var defaultViewIndex:int = getItemIndex( getItemById( defaultView ) );
			
			if ( defaultViewIndex != -1 )
			{
				selectedIndex = defaultViewIndex;
			}
			else if ( views.length > 0 && defaultView != NO_DEFAULT )
			{
				selectedIndex = 0;
			}
			else
			{
				selectedIndex = -1;
			}
		}
		
		// navigate to first view -----------------------------------
		/**
		 * Navigate to the first enabled view of a navigator
		 * @param navigatorId
		 */
		public function navigateToFirstView( navigatorId:String ):void
		{
			var navigationEvent:NavigationEvent = new NavigationEvent( NavigationEvent.NAVIGATE_TO_FIRST_VIEW );
			navigationEvent.navigatorId = navigatorId;
			dispatcher.dispatchEvent( navigationEvent );
		}
		
		// register navigator  -------------------------------------------------
		[EventHandler( event = "mx.events.FlexEvent.APPLICATION_COMPLETE" )]
		/**
		 * Register Navigator
		 * Used to register a navigator with the Navigation Manager
		 */
		public function register():void
		{
			if ( !registered )
			{
				initialize();
				
				var navigationEvent:NavigationEvent = new NavigationEvent( NavigationEvent.REGISTER_NAVIGATOR );
				navigationEvent.navigator = this;
				dispatcher.dispatchEvent( navigationEvent );
			}
		}
		
		// clear navigation items -------------------------------------
		/**
		 * Used to clear the navigation items
		 */
		public function reset():void
		{
			navigationItems = new ArrayCollection();
			states = new ArrayCollection();
			views = new ArrayCollection();
			dialogs = new ArrayCollection();
			actions = new ArrayCollection();
		}
		
		// reset selected index ------------------------------
		/**
		 * Used by the navigation manager to reset to the default for an item type
		 */
		public function resetItemsToDefault( itemType:String ):void
		{
			switch ( itemType )
			{
				case NavigationItemTypes.ALL:
				{
					navigateToDefaultState();
					navigateToDefaultView();
					selectedDialogIndex = -1;
					selectedActionIndex = -1;
					break;
				}
				case NavigationItemTypes.STATE:
				{
					navigateToDefaultState();
					break;
				}
				case NavigationItemTypes.VIEW:
				{
					navigateToDefaultView();
					break;
				}
				case NavigationItemTypes.DIALOG:
				{
					selectedDialogIndex = -1;
					break;
				}
				case NavigationItemTypes.ACTION:
				{
					selectedActionIndex = -1;
					break;
				}
			}
		}
		
		// reset selected item -----------------------------------
		/**
		 * Dispatch an event to reset to the default for one or all collections
		 * @param navigatorId
		 * @param itemType
		 */
		public function resetToDefault( navigatorId:String, itemType:String ):void
		{
			var navigationEvent:NavigationEvent = new NavigationEvent( NavigationEvent.RESET_TO_DEFAULT );
			navigationEvent.navigatorId = navigatorId;
			navigationEvent.itemType = itemType;
			dispatcher.dispatchEvent( navigationEvent );
		}
		
		//======================================
		// protected methods 
		//======================================
		
		// create actions -------------------------------------------------
		/**
		 * Create the ACTION navigation items
		 * This method is overridden in subclasses to
		 * create specific ACTION navigation items
		 */
		protected function createActions():void
		{
		}
		
		// create dialogs -------------------------------------------------
		/**
		 * Create the DIALOG navigation items
		 * This method is overridden in subclasses to
		 * create specific DIALOG navigation items
		 */
		protected function createDialogs():void
		{
		}
		
		// create navigation item --------------------------------------
		/**
		 * Used to create a new navigation item
		 * @param id
		 * @param type
		 * @param state
		 * @param label
		 * @return
		 */
		protected function createNavigationItem( id:String, type:String, state:String = "", label:String = "", icon:Class = null ):INavigationItem
		{
			var navigationItem:INavigationItem = new NavigationItem();
			
			navigationItem.navigatorId = navigatorId;
			navigationItem.id = id;
			navigationItem.type = type;
			navigationItem.state = state;
			navigationItem.label = label;
			navigationItem.icon = icon;
			
			return navigationItem;
		}
		
		// create states -------------------------------------------------
		/**
		 * Create the STATE navigation items
		 * This method is overridden in subclasses to
		 * create specific STATE navigation items
		 */
		protected function createStates():void
		{
		}
		
		// create views -------------------------------------------------
		/**
		 * Create the VIEW navigation items
		 * This method is overridden in subclasses to
		 * create specific VIEW navigation items
		 */
		protected function createViews():void
		{
		}
		
		// remove navigation items ---------------------------------------
		/**
		 * Remove Navigation Items
		 * @param collection
		 */
		protected function removeNavigationItems( collection:ArrayCollection ):void
		{
			for each ( var navigationItem:INavigationItem in collection )
			{
				navigationItems.removeItemAt( navigationItems.getItemIndex( navigationItem ) );
			}
		}
		
		// set actions ----------------------------------------------
		/**
		 * Set Actions
		 * used to initialialize the actions collection
		 * Optional parameter for the defaultDialog
		 * @param value
		 */
		protected function setActions( value:ArrayCollection ):void
		{
			// remove the dialog navigation items
			removeNavigationItems( actions );
			
			// initialize
			selectedDialogIndex = -1;
			
			// set actions and add to navigation items
			actions = value;
			navigationItems.addAll( actions );
		}
		
		// set dialogs ----------------------------------------------
		/**
		 * Set Dialogs
		 * used to initialialize the dialogs collection
		 * Optional parameter for the defaultDialog
		 * @param value
		 */
		protected function setDialogs( value:ArrayCollection ):void
		{
			// remove the dialog navigation items
			removeNavigationItems( dialogs );
			
			// initialize
			selectedDialogIndex = -1;
			
			// set dialogs and add to navigation items
			dialogs = value;
			navigationItems.addAll( dialogs );
		}
		
		// set states --------------------------------------------------
		/**
		 * Set States
		 * Used to initialize the states collection
		 * Optional parameter for the defaultState
		 * @param value
		 * @param defaultState
		 */
		protected function setStates( value:ArrayCollection, defaultState:String = "" ):void
		{
			// remove the state navigation items
			removeNavigationItems( states );
			
			// set states and add to navigation items
			states = value;
			navigationItems.addAll( states );
			
			// navigate to the default state
			this.defaultState = defaultState;
			navigateToDefaultState();
		}
		
		// set views ----------------------------------------------
		/**
		 * Set Views
		 * used to initialialize the views collection
		 * Optional parameter for the defaultView
		 * @param value
		 * @param defaultView
		 */
		protected function setViews( value:ArrayCollection, defaultView:String = NO_DEFAULT ):void
		{
			// remove the view navigation items
			removeNavigationItems( views );
			
			// initialize
			selectedIndex = -1;
			
			// set views and add to navigation items
			views = value;
			navigationItems.addAll( views );
			
			// navigate to the default view
			this.defaultView = defaultView;
			navigateToDefaultView();
		}
	}
}

