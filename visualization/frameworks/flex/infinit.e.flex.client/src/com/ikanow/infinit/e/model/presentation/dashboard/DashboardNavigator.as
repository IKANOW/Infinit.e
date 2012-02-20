package com.ikanow.infinit.e.model.presentation.dashboard
{
	import com.ikanow.infinit.e.shared.model.constant.NavigationConstants;
	import com.ikanow.infinit.e.shared.model.presentation.base.Navigator;
	
	/**
	 * Dashboard Navigator
	 */
	public class DashboardNavigator extends Navigator
	{
		
		//======================================
		// public properties 
		//======================================
		
		/**
		 * Presentation Model
		 */
		[Bindable]
		[Inject]
		public var model:DashboardModel;
		
		//======================================
		// constructor 
		//======================================
		
		/**
		 * Constructor
		 */
		public function DashboardNavigator()
		{
			navigatorId = NavigationConstants.MAIN_DASHBOARD_ID;
			parentNavigatorId = NavigationConstants.MAIN_ID;
		}
	}
}
