package com.ikanow.infinit.e.community.model.presentation.list
{
	import com.ikanow.infinit.e.shared.model.constant.NavigationConstants;
	import com.ikanow.infinit.e.shared.model.presentation.base.Navigator;
	
	/**
	 * Community List Navigator
	 */
	public class CommunityListNavigator extends Navigator
	{
		
		//======================================
		// public properties 
		//======================================
		
		/**
		 * Presentation Model
		 */
		[Bindable]
		[Inject]
		public var model:CommunityListModel;
		
		//======================================
		// constructor 
		//======================================
		
		/**
		 * Constructor
		 */
		public function CommunityListNavigator()
		{
			navigatorId = NavigationConstants.COMMUNITY_LIST_ID;
			parentNavigatorId = NavigationConstants.SOURCES_COMMUNITY_ID;
		}
		
		
		//======================================
		// public methods 
		//======================================
		
		/**
		 * Show Community Request View
		 */
		public function showCommunityRequestView():void
		{
			navigateById( NavigationConstants.COMMUNITY_REQUEST_JOIN_PROMPT_ID );
			navigateById( NavigationConstants.COMMUNITY_REQUEST_ID );
		}
	}
}
