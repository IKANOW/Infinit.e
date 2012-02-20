package com.ikanow.infinit.e.model.presentation.login
{
	import com.ikanow.infinit.e.shared.model.constant.NavigationConstants;
	import com.ikanow.infinit.e.shared.model.constant.types.NavigationItemTypes;
	import com.ikanow.infinit.e.shared.model.presentation.base.Navigator;
	import com.ikanow.infinit.e.shared.util.StateUtil;
	import flash.utils.setTimeout;
	import mx.collections.ArrayCollection;
	import mx.core.UIComponent;
	
	/**
	 * Login Navigator
	 */
	public class LoginNavigator extends Navigator
	{
		
		//======================================
		// private static properties 
		//======================================
		
		private static const COOKIE_ID:String = NavigationConstants.LOGIN_COOKIE_ID;
		
		private static const LOGIN_ID:String = NavigationConstants.LOGIN_FORM_ID;
		
		private static const LOADING_DATA_ID:String = NavigationConstants.LOGIN_LOADING_DATA_ID;
		
		private static const FORGOT_PASSWORD_ID:String = NavigationConstants.LOGIN_FORGOT_PASSWORD_ID;
		
		
		//======================================
		// public properties 
		//======================================
		
		/**
		 * Presentation Model
		 */
		[Bindable]
		[Inject]
		public var model:LoginModel;
		
		//======================================
		// constructor 
		//======================================
		
		/**
		 * Constructor
		 */
		public function LoginNavigator()
		{
			navigatorId = NavigationConstants.MAIN_LOGIN_ID;
			parentNavigatorId = NavigationConstants.MAIN_ID;
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
			StateUtil.setStates( component, [ COOKIE_ID, LOGIN_ID, LOADING_DATA_ID, FORGOT_PASSWORD_ID ], state );
		}
		
		
		//======================================
		// public methods 
		//======================================
		
		/**
		 * Show Dashboard View
		 */
		public function showDashboardView():void
		{
			navigateById( NavigationConstants.MAIN_DASHBOARD_ID );
			
			// clear the password
			model.clearPassword();
		}
		
		/**
		 * Show Forgot Passwor View
		 */
		public function showForgotPasswordView():void
		{
			navigateById( FORGOT_PASSWORD_ID );
		}
		
		/**
		 * Show Loading Data View
		 */
		public function showLoadingDataView():void
		{
			navigateById( LOADING_DATA_ID );
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
		
		override public function resetItemsToDefault(itemType:String):void
		{
			super.resetItemsToDefault( itemType );
			
			showLoginView();	
		}
		
		/**
		 * Create States
		 */
		override protected function createStates():void
		{
			var navStates:ArrayCollection = new ArrayCollection();
			
			// cookie
			navStates.addItem( createNavigationItem( COOKIE_ID, NavigationItemTypes.STATE, COOKIE_ID ) );
			
			// login
			navStates.addItem( createNavigationItem( LOGIN_ID, NavigationItemTypes.STATE, LOGIN_ID ) );
			
			// loading data
			navStates.addItem( createNavigationItem( LOADING_DATA_ID, NavigationItemTypes.STATE, LOADING_DATA_ID ) );
			
			// forgot password
			navStates.addItem( createNavigationItem( FORGOT_PASSWORD_ID, NavigationItemTypes.STATE, FORGOT_PASSWORD_ID ) );
			
			// set states - default cookie
			setStates( navStates, COOKIE_ID );
		}
	}
}
