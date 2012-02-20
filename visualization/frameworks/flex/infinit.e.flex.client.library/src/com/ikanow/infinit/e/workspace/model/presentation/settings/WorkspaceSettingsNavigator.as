package com.ikanow.infinit.e.workspace.model.presentation.settings
{
	import com.ikanow.infinit.e.shared.model.constant.NavigationConstants;
	import com.ikanow.infinit.e.shared.model.presentation.base.Navigator;
	
	/**
	 * Workspace Settings Navigator
	 */
	public class WorkspaceSettingsNavigator extends Navigator
	{
		
		//======================================
		// public properties 
		//======================================
		
		/**
		 * Presentation Model
		 */
		[Bindable]
		[Inject]
		public var model:WorkspaceSettingsModel;
		
		//======================================
		// constructor 
		//======================================
		
		public function showContentView():void
		{
			navigateById( NavigationConstants.WORKSPACE_CONTENT_ID );
		}
		
		/**
		 * Constructor
		 */
		public function WorkspaceSettingsNavigator()
		{
			navigatorId = NavigationConstants.WORKSPACE_SETTINGS_ID;
			parentNavigatorId = NavigationConstants.WORKSPACES_BODY_WORKSPACE_ID;
		}
	}
}
