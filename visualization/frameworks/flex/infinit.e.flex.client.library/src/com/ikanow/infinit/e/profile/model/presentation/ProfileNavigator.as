package com.ikanow.infinit.e.profile.model.presentation
{
	import com.ikanow.infinit.e.shared.model.constant.NavigationConstants;
	import com.ikanow.infinit.e.shared.model.presentation.base.Navigator;
	
	/**
	 * Profile Navigator
	 */
	public class ProfileNavigator extends Navigator
	{
		
		//======================================
		// public properties 
		//======================================
		
		/**
		 * Presentation Model
		 */
		[Bindable]
		[Inject]
		public var model:ProfileModel;
		
		//======================================
		// constructor 
		//======================================
		
		/**
		 * Constructor
		 */
		public function ProfileNavigator()
		{
			navigatorId = NavigationConstants.DASHBOARD_HEADER_PROFILE_ID;
			parentNavigatorId = NavigationConstants.DASHBOARD_HEADER_ID;
		}
	}
}
