package com.ikanow.infinit.e.model.presentation
{
	import com.ikanow.infinit.e.shared.model.constant.NavigationConstants;
	import com.ikanow.infinit.e.shared.model.constant.ServiceConstants;
	import com.ikanow.infinit.e.shared.model.constant.types.NavigationItemTypes;
	import com.ikanow.infinit.e.shared.model.presentation.base.Navigator;
	import com.ikanow.infinit.e.shared.util.StateUtil;
	import mx.collections.ArrayCollection;
	import mx.core.UIComponent;
	
	/**
	 * Main Navigator
	 */
	public class MainNavigator extends Navigator
	{
		
		//======================================
		// private static properties 
		//======================================
		
		private static const LOGIN_ID:String = NavigationConstants.MAIN_LOGIN_ID;
		
		private static const DASHBOARD_ID:String = NavigationConstants.MAIN_DASHBOARD_ID;
		
		
		//======================================
		// public properties 
		//======================================
		
		/**
		 * Presentation Model
		 */
		[Bindable]
		[Inject]
		public var model:MainModel;
		
		//======================================
		// constructor 
		//======================================
		
		/**
		 * Constructor
		 */
		public function MainNavigator()
		{
			navigatorId = NavigationConstants.MAIN_ID;
			parentNavigatorId = NavigationConstants.APPLICATION_ID;
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
			StateUtil.setStates( component, [ LOGIN_ID, DASHBOARD_ID ], state );
		}
		
		
		//======================================
		// public methods 
		//======================================
		
		/**
		 * Show Dashboard View
		 */
		public function showDashboardView():void
		{
			navigateById( DASHBOARD_ID );
		}
		
		/**
		 * Show Login View
		 */
		public function showLoginView():void
		{
			navigateById( LOGIN_ID );
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
			
			// login
			navStates.addItem( createNavigationItem( LOGIN_ID, NavigationItemTypes.STATE, LOGIN_ID ) );
			
			// dashboard
			navStates.addItem( createNavigationItem( DASHBOARD_ID, NavigationItemTypes.STATE, DASHBOARD_ID ) );
			
			// set states - default login
			setStates( navStates, LOGIN_ID );
		}
	}
}

