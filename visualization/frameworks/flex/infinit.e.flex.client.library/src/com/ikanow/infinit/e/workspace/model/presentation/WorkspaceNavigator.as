package com.ikanow.infinit.e.workspace.model.presentation
{
	import com.ikanow.infinit.e.shared.model.constant.NavigationConstants;
	import com.ikanow.infinit.e.shared.model.constant.types.NavigationItemTypes;
	import com.ikanow.infinit.e.shared.model.presentation.base.Navigator;
	import com.ikanow.infinit.e.shared.util.StateUtil;
	import mx.collections.ArrayCollection;
	import mx.core.UIComponent;
	
	/**
	 * Workspace Navigator
	 */
	public class WorkspaceNavigator extends Navigator
	{
		
		//======================================
		// public static properties 
		//======================================
		
		public static const CONTENT_ID:String = NavigationConstants.WORKSPACE_CONTENT_ID;
		
		public static const LAYOUT_ID:String = NavigationConstants.WORKSPACE_LAYOUT_ID;
		
		public static const SETTINGS_ID:String = NavigationConstants.WORKSPACE_SETTINGS_ID;
		
		
		//======================================
		// public properties 
		//======================================
		
		/**
		 * Presentation Model
		 */
		[Bindable]
		[Inject]
		public var model:WorkspaceModel;
		
		//======================================
		// constructor 
		//======================================
		
		/**
		 * Constructor
		 */
		public function WorkspaceNavigator()
		{
			navigatorId = NavigationConstants.WORKSPACES_BODY_WORKSPACE_ID;
			parentNavigatorId = NavigationConstants.WORKSPACES_BODY_ID;
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
			StateUtil.setStates( component, [ CONTENT_ID, LAYOUT_ID, SETTINGS_ID ], state );
		}
		
		
		//======================================
		// public methods 
		//======================================
		
		/**
		 * Close Widget Drawer
		 */
		public function closeWidgetDrawer():void
		{
			navigateById( NavigationConstants.WORKSPACES_BODY_DRAWER_CLOSED_ID );
		}
		
		/**
		 * Show Content View
		 */
		public function showContentView():void
		{
			navigateById( CONTENT_ID );
		}
		
		/**
		 * Show Layouts View
		 */
		public function showLayoutView():void
		{
			navigateById( LAYOUT_ID );
		}
		
		/**
		 * Show Settings View
		 */
		public function showSettingsView():void
		{
			navigateById( SETTINGS_ID );
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
			
			// content
			navStates.addItem( createNavigationItem( CONTENT_ID, NavigationItemTypes.STATE, CONTENT_ID ) );
			
			// layouts
			navStates.addItem( createNavigationItem( LAYOUT_ID, NavigationItemTypes.STATE, LAYOUT_ID ) );
			
			// settings
			navStates.addItem( createNavigationItem( SETTINGS_ID, NavigationItemTypes.STATE, SETTINGS_ID ) );
			
			// set states - default content
			setStates( navStates, CONTENT_ID );
		}
	}
}
