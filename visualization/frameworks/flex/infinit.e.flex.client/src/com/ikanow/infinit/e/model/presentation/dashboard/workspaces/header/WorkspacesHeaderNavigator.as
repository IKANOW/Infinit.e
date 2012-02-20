package com.ikanow.infinit.e.model.presentation.dashboard.workspaces.header
{
	import com.ikanow.infinit.e.shared.model.constant.NavigationConstants;
	import com.ikanow.infinit.e.shared.model.constant.types.NavigationItemTypes;
	import com.ikanow.infinit.e.shared.model.presentation.base.Navigator;
	import com.ikanow.infinit.e.shared.model.vo.ui.INavigationItem;
	import com.ikanow.infinit.e.shared.model.vo.ui.NavigationItem;
	import mx.collections.ArrayCollection;
	import mx.resources.IResourceManager;
	import mx.resources.ResourceManager;
	
	/**
	 * Workspaces Header Navigator
	 */
	public class WorkspacesHeaderNavigator extends Navigator
	{
		
		//======================================
		// private static properties 
		//======================================
		
		private static const WORKSPACE_SETTIINGS:String = NavigationConstants.WORKSPACES_HEADER_WORKSPACE_SETTIINGS;
		
		private static const EXPORT_PDF_ID:String = NavigationConstants.WORKSPACES_HEADER_EXPORT_PDF_ID;
		
		private static const EXPORT_JSON_ID:String = NavigationConstants.WORKSPACES_HEADER_EXPORT_JSON_ID;
		
		private static const EXPORT_RSS_ID:String = NavigationConstants.WORKSPACES_HEADER_EXPORT_RSS_ID;
		
		
		//======================================
		// public properties 
		//======================================
		
		/**
		 * Presentation Model
		 */
		[Bindable]
		[Inject]
		public var model:WorkspacesHeaderModel;
		
		//======================================
		// private properties 
		//======================================
		
		private var resourceManager:IResourceManager = ResourceManager.getInstance();
		
		//======================================
		// constructor 
		//======================================
		
		/**
		 * Constructor
		 */
		public function WorkspacesHeaderNavigator()
		{
			navigatorId = NavigationConstants.WORKSPACES_HEADER_ID;
			parentNavigatorId = NavigationConstants.WORKSPACES_ID;
		}
		
		
		//======================================
		// public methods 
		//======================================
		
		/**
		 * Navigate
		 */
		override public function navigate( navigationItem:* ):void
		{
			if ( navigationItem is INavigationItem )
			{
				switch ( navigationItem.type )
				{
					case NavigationItemTypes.ACTION:
					{
						navigateActions( navigationItem );
						break;
					}
				}
			}
		}
		
		public function showWorkspaceSettings():void
		{
			navigateById( NavigationConstants.WORKSPACE_SETTINGS_ID );
		}
		
		//======================================
		// protected methods 
		//======================================
		
		/**
		 * Create Actions
		 */
		override protected function createActions():void
		{
			var navActions:ArrayCollection = new ArrayCollection();
			var navigationItem:NavigationItem;
			
			// workspace settings
			navigationItem = new NavigationItem();
			navigationItem.navigatorId = navigatorId;
			navigationItem.id = WORKSPACE_SETTIINGS;
			navigationItem.type = NavigationItemTypes.ACTION;
			navigationItem.label = resourceManager.getString( 'infinite', 'workspacesHeader.workspaceSettings' );
			navActions.addItem( navigationItem );
			
			// export pdf
			navigationItem = new NavigationItem();
			navigationItem.navigatorId = navigatorId;
			navigationItem.id = EXPORT_PDF_ID;
			navigationItem.type = NavigationItemTypes.ACTION;
			navigationItem.label = resourceManager.getString( 'infinite', 'workspacesHeader.export' );
			navActions.addItem( navigationItem );
			
			// export json
			navigationItem = new NavigationItem();
			navigationItem.navigatorId = navigatorId;
			navigationItem.id = EXPORT_JSON_ID;
			navigationItem.type = NavigationItemTypes.ACTION;
			navigationItem.label = resourceManager.getString( 'infinite', 'workspacesHeader.json' );
			navActions.addItem( navigationItem );
			
			// export rss
			navigationItem = new NavigationItem();
			navigationItem.navigatorId = navigatorId;
			navigationItem.id = EXPORT_RSS_ID;
			navigationItem.type = NavigationItemTypes.ACTION;
			navigationItem.label = resourceManager.getString( 'infinite', 'workspacesHeader.rss' );
			navActions.addItem( navigationItem );
			
			// set actions
			setActions( navActions );
		}
		
		/**
		 * Navigate Actions
		 */
		protected function navigateActions( navigationItem:NavigationItem ):void
		{
			switch ( navigationItem.id )
			{
				case WORKSPACE_SETTIINGS:
				{
					showWorkspaceSettings();
					break;
				}
				case EXPORT_PDF_ID:
				{
					model.exportPDF();
					break;
				}
				case EXPORT_JSON_ID:
				{
					model.exportJSON();
					break;
				}
				case EXPORT_RSS_ID:
				{
					model.exportRSS();
					break;
				}
			}
		}
	}
}
