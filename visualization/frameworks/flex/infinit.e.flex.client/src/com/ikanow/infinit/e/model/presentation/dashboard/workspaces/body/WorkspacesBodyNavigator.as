package com.ikanow.infinit.e.model.presentation.dashboard.workspaces.body
{
	import com.ikanow.infinit.e.shared.model.constant.NavigationConstants;
	import com.ikanow.infinit.e.shared.model.constant.types.NavigationItemTypes;
	import com.ikanow.infinit.e.shared.model.presentation.base.Navigator;
	import com.ikanow.infinit.e.shared.util.StateUtil;
	import mx.collections.ArrayCollection;
	import mx.core.UIComponent;
	
	/**
	 * Workspaces Body Navigator
	 */
	public class WorkspacesBodyNavigator extends Navigator
	{
		
		//======================================
		// private static properties 
		//======================================
		
		private static const DRAWER_CLOSED_ID:String = NavigationConstants.WORKSPACES_BODY_DRAWER_CLOSED_ID;
		
		private static const DRAWER_OPEN_ID:String = NavigationConstants.WORKSPACES_BODY_DRAWER_OPEN_ID;
		
		
		//======================================
		// public properties 
		//======================================
		
		/**
		 * Presentation Model
		 */
		[Bindable]
		[Inject]
		public var model:WorkspacesBodyModel;
		
		//======================================
		// constructor 
		//======================================
		
		/**
		 * Constructor
		 */
		public function WorkspacesBodyNavigator()
		{
			navigatorId = NavigationConstants.WORKSPACES_BODY_ID;
			parentNavigatorId = NavigationConstants.WORKSPACES_ID;
		}
		
		
		//======================================
		// public static methods 
		//======================================
		
		/**
		 * Update View States
		 * Used to override the states of a view with the
		 * full state navigation item ids from the
		 * associated Navigator.
		 * @param component - the component to update states
		 * @param state - the current state to set after the update
		 */
		public static function updateViewStates( component:UIComponent, state:String = "" ):void
		{
			StateUtil.setStates( component, [ DRAWER_CLOSED_ID, DRAWER_OPEN_ID ], state );
		}
		
		
		//======================================
		// public methods 
		//======================================
		
		/**
		 * Close Drawer
		 */
		public function closeDrawer():void
		{
			navigateById( DRAWER_CLOSED_ID );
		}
		
		/**
		 * Open Drawer
		 */
		public function openDrawer():void
		{
			navigateById( DRAWER_OPEN_ID );
		}
		
		//======================================
		// protected methods 
		//======================================
		
		/**
		 * Create States
		 */
		override protected function createStates():void
		{
			var navStates:ArrayCollection = new ArrayCollection();
			
			// drawer closed
			navStates.addItem( createNavigationItem( DRAWER_CLOSED_ID, NavigationItemTypes.STATE, DRAWER_CLOSED_ID ) );
			
			// drawer open
			navStates.addItem( createNavigationItem( DRAWER_OPEN_ID, NavigationItemTypes.STATE, DRAWER_OPEN_ID ) );
			
			// set states - default drawer closed
			setStates( navStates, DRAWER_CLOSED_ID );
		}
	}
}
