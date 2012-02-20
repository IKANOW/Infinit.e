package com.ikanow.infinit.e.query.model.presentation.settings
{
	import com.ikanow.infinit.e.shared.model.constant.NavigationConstants;
	import com.ikanow.infinit.e.shared.model.presentation.base.Navigator;
	
	/**
	 * Query Settings Navigator
	 */
	public class QuerySettingsNavigator extends Navigator
	{
		
		//======================================
		// public properties 
		//======================================
		
		/**
		 * Presentation Model
		 */
		[Bindable]
		[Inject]
		public var model:QuerySettingsModel;
		
		//======================================
		// constructor 
		//======================================
		
		/**
		 * Constructor
		 */
		public function QuerySettingsNavigator()
		{
			navigatorId = NavigationConstants.QUERY_SETTINGS_ID;
			parentNavigatorId = NavigationConstants.WORKSPACES_QUERY_ID;
		}
	}
}
