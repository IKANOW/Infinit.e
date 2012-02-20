package com.ikanow.infinit.e.shared.event
{
	import com.ikanow.infinit.e.shared.model.presentation.base.INavigator;
	import com.ikanow.infinit.e.shared.model.vo.ui.INavigationItem;
	import flash.events.Event;
	
	/**
	 *  Navigation Event
	 */
	public class NavigationEvent extends Event
	{
		
		//======================================
		// public static properties 
		//======================================
		
		public static const NAVIGATE:String = "navigateEvent";
		
		public static const NAVIGATE_BY_ID:String = "navigateByIdEvent";
		
		public static const NAVIGATE_TO_FIRST_VIEW:String = "navigateToFirstViewEvent";
		
		public static const NAVIGATE_TO_STATE:String = "navigateToStateEvent";
		
		public static const NAVIGATE_TO_VIEW:String = "navigateToViewEvent";
		
		public static const REGISTER_NAVIGATOR:String = "registerNavigatorEvent";
		
		public static const RESET_TO_DEFAULT:String = "resetToDefaultEvent";
		
		public static const RESET:String = "resetNavigationEvent";
		
		
		//======================================
		// public properties 
		//======================================
		
		public var label:String = "";
		
		public var navigationItem:INavigationItem;
		
		public var navigationItemId:String;
		
		public var navigator:INavigator;
		
		public var navigatorId:String;
		
		public var parent:String = "";
		
		public var state:String = "";
		
		public var view:int = -1;
		
		public var itemType:String = "";
		
		//======================================
		// constructor 
		//======================================
		
		/**
		 * NavigationEvent
		 * @param type
		 * @param bubbles
		 * @param cancelable
		 * @param navigator
		 * @param navigationItem
		 * @param navigatorId
		 * @param navigationItemId
		 * @param parent
		 * @param view
		 * @param state
		 * @param label
		 * @param itemType
		 */
		public function NavigationEvent( type:String, bubbles:Boolean = true, cancelable:Boolean = false, navigator:INavigator = null, navigationItem:INavigationItem = null, navigatorId:String = "", navigationItemId:String = "", parent:String = "", view:int = -1, state:String = "", label:String = "", itemType:String = "" )
		{
			super( type, bubbles, cancelable );
			
			this.navigator = navigator;
			this.navigationItem = navigationItem;
			this.navigatorId = navigatorId;
			this.navigationItemId = navigationItemId;
			this.parent = parent;
			this.view = view;
			this.state = state;
			this.label = label;
			this.itemType = itemType;
		}
		
		
		//======================================
		// public methods 
		//======================================
		
		/**
		 * Clone event
		 */
		override public function clone():Event
		{
			return new NavigationEvent( type, bubbles, cancelable, navigator, navigationItem, navigatorId, navigationItemId, parent, view, state, label, itemType );
		}
	}
}
