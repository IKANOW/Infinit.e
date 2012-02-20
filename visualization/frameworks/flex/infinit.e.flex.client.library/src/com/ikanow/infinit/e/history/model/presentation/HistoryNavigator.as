package com.ikanow.infinit.e.history.model.presentation
{
	import com.ikanow.infinit.e.shared.model.constant.NavigationConstants;
	import com.ikanow.infinit.e.shared.model.presentation.base.Navigator;
	
	/**
	 * History Navigator
	 */
	public class HistoryNavigator extends Navigator
	{
		
		//======================================
		// public properties 
		//======================================
		
		/**
		 * Presentation Model
		 */
		[Bindable]
		[Inject]
		public var model:HistoryModel;
		
		//======================================
		// constructor 
		//======================================
		
		/**
		 * Constructor
		 */
		public function HistoryNavigator()
		{
			navigatorId = NavigationConstants.WORKSPACES_HISTORY_ID;
			parentNavigatorId = NavigationConstants.WORKSPACES_ID;
		}
		
		
		//======================================
		// public methods 
		//======================================
		
		/**
		 * Close History View
		 */
		public function closeHistoryView():void
		{
			navigateById( NavigationConstants.WORKSPACES_BODY_ID );
		}
	}
}
