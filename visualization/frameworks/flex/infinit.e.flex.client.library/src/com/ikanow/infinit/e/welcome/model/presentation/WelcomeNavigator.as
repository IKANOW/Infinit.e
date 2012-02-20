package com.ikanow.infinit.e.welcome.model.presentation
{
	import com.ikanow.infinit.e.shared.model.constant.NavigationConstants;
	import com.ikanow.infinit.e.shared.model.presentation.base.Navigator;
	
	/**
	 * Welcome Navigator
	 */
	public class WelcomeNavigator extends Navigator
	{
		
		//======================================
		// public properties 
		//======================================
		
		/**
		 * Presentation Model
		 */
		[Bindable]
		[Inject]
		public var model:WelcomeModel;
		
		//======================================
		// constructor 
		//======================================
		
		/**
		 * Constructor
		 */
		public function WelcomeNavigator()
		{
			navigatorId = NavigationConstants.WORKSPACES_WELCOME_ID;
			parentNavigatorId = NavigationConstants.WORKSPACES_ID;
		}
	}
}
