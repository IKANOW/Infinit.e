package com.ikanow.infinit.e.workspace.model.presentation.layout
{
	import com.ikanow.infinit.e.shared.model.constant.NavigationConstants;
	import com.ikanow.infinit.e.shared.model.presentation.base.Navigator;
	
	/**
	 * Workspace Layout Navigator
	 */
	public class WorkspaceLayoutNavigator extends Navigator
	{
		
		//======================================
		// public static properties 
		//======================================
		
		public static const CONTENT_ID:String = NavigationConstants.WORKSPACE_CONTENT_ID;
		
		public static const LAYOUT_ID:String = NavigationConstants.WORKSPACE_LAYOUT_ID;
		
		
		//======================================
		// public properties 
		//======================================
		
		/**
		 * Presentation Model
		 */
		[Bindable]
		[Inject]
		public var model:WorkspaceLayoutModel;
		
		//======================================
		// constructor 
		//======================================
		
		/**
		 * Constructor
		 */
		public function WorkspaceLayoutNavigator()
		{
			navigatorId = NavigationConstants.WORKSPACE_LAYOUT_ID;
			parentNavigatorId = NavigationConstants.WORKSPACES_BODY_WORKSPACE_ID;
		}
		
		
		//======================================
		// public methods 
		//======================================
		
		/**
		 * Close Widgets Drawer
		 */
		public function closeWidgetsDrawer():void
		{
			navigateById( NavigationConstants.WORKSPACES_BODY_DRAWER_CLOSED_ID );
		}
		
		/**
		 * Hide Layout View
		 */
		public function hideView():void
		{
			navigateById( CONTENT_ID );
		}
		
		/**
		 * Show Layout View
		 */
		public function showView():void
		{
			navigateById( LAYOUT_ID );
		}
	}
}
