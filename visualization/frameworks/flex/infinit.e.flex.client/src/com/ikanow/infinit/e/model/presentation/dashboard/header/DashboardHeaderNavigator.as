/*******************************************************************************
 * Copyright 2012, The Infinit.e Open Source Project.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.ikanow.infinit.e.model.presentation.dashboard.header
{
	import com.ikanow.infinit.e.shared.model.constant.NavigationConstants;
	import com.ikanow.infinit.e.shared.model.constant.types.NavigationItemTypes;
	import com.ikanow.infinit.e.shared.model.presentation.base.Navigator;
	import com.ikanow.infinit.e.shared.model.vo.ui.INavigationItem;
	import com.ikanow.infinit.e.shared.model.vo.ui.NavigationItem;
	import flash.events.MouseEvent;
	import flash.utils.setTimeout;
	import mx.collections.ArrayCollection;
	import mx.controls.Alert;
	import mx.resources.IResourceManager;
	import mx.resources.ResourceManager;
	import assets.EmbeddedAssets;
	
	/**
	 * Header Navigator
	 */
	public class DashboardHeaderNavigator extends Navigator
	{
		
		//======================================
		// public static properties 
		//======================================
		
		public static const PROFILE_INDEX:int = 0;
		
		public static const MANAGER_INDEX:int = 1;
		
		public static const LOGOUT_INDEX:int = 2;
		
		//======================================
		// private static properties 
		//======================================
		
		private static const QUERY_ID:String = NavigationConstants.DASHBOARD_HEADER_QUERY_ID;
		
		private static const SOURCES_ID:String = NavigationConstants.DASHBOARD_HEADER_SOURCES_ID;
		
		private static const HISTORY_ID:String = NavigationConstants.DASHBOARD_HEADER_HISTORY_ID;
		
		private static const PROFILE_ID:String = NavigationConstants.DASHBOARD_HEADER_PROFILE_ID;
		
		private static const MANAGER_ID:String = NavigationConstants.DASHBOARD_HEADER_MANAGER_ID;
		
		private static const LOGOUT_ID:String = NavigationConstants.DASHBOARD_HEADER_LOGOUT_ID;
		
		
		//======================================
		// public properties 
		//======================================
		
		/**
		 * Presentation Model
		 */
		[Bindable]
		[Inject]
		public var model:DashboardHeaderModel;
		
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
		public function DashboardHeaderNavigator()
		{
			navigatorId = NavigationConstants.DASHBOARD_HEADER_ID;
			parentNavigatorId = NavigationConstants.MAIN_DASHBOARD_ID;
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
		 * Hide Workspaces dialogs
		 */
		public function hideWorkspacesDialogs():void
		{
			navigateById( NavigationConstants.WORKSPACES_BODY_ID );
		}
		
		/**
		 * Navigate
		 */
		override public function navigate( navigationItem:* ):void
		{
			if ( navigationItem is INavigationItem )
			{
				switch ( navigationItem.type )
				{
					case NavigationItemTypes.DIALOG:
					{
						navigateDialogs( navigationItem );
						break;
					}
					case NavigationItemTypes.ACTION:
					{
						navigateActions( navigationItem );
						break;
					}
				}
			}
		}
		
		/**
		 * Reset Actions
		 */
		public function resetActions():void
		{
			resetItemsToDefault( NavigationItemTypes.ACTION );
		}
		
		override public function resetItemsToDefault( itemType:String ):void
		{
			super.resetItemsToDefault( itemType );
			model.reset();
		}
		
		/**
		 * Show History Dialog
		 */
		public function showHistoryDialog():void
		{
			navigateById( HISTORY_ID );
		}
		
		/**
		 * Show Login View
		 */
		public function showLoginView():void
		{
			navigateById( NavigationConstants.MAIN_LOGIN_ID );
			navigateById( NavigationConstants.LOGIN_FORM_ID );
		}
		
		/**
		 * Show Query Dialog
		 */
		public function showQueryDialog():void
		{
			navigateDialogs( getItemById( QUERY_ID ) as NavigationItem );
		}
		
		/**
		 * Show Sources Dialog
		 */
		public function showSourcesDialog():void
		{
			navigateById( SOURCES_ID );
		}
		
		/**
		 * Update Current User
		 */
		public function updateCurrentUser():void
		{
			resetItemsToDefault( NavigationItemTypes.ALL );
			createActions();
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
			
			// profile
			navigationItem = new NavigationItem();
			navigationItem.navigatorId = navigatorId;
			navigationItem.id = PROFILE_ID;
			navigationItem.type = NavigationItemTypes.ACTION;
			
			// current user name
			if ( model && model.currentUser )
			{
				navigationItem.label = model.currentUser.displayName;
			}
			
			navActions.addItem( navigationItem );
			
			// logout
			navigationItem = new NavigationItem();
			navigationItem.navigatorId = navigatorId;
			navigationItem.id = MANAGER_ID;
			navigationItem.type = NavigationItemTypes.ACTION;
			navigationItem.label = resourceManager.getString( 'infinite', 'header.launchManager' );
			navActions.addItem( navigationItem );
			
			// logout
			navigationItem = new NavigationItem();
			navigationItem.navigatorId = navigatorId;
			navigationItem.id = LOGOUT_ID;
			navigationItem.type = NavigationItemTypes.ACTION;
			navigationItem.label = resourceManager.getString( 'infinite', 'header.logOut' );
			navActions.addItem( navigationItem );
			
			// set actions
			setActions( navActions );
		}
		
		/**
		 * Create Dialogs
		 */
		override protected function createDialogs():void
		{
			var navDialogs:ArrayCollection = new ArrayCollection();
			var navigationItem:NavigationItem;
			
			// advanced search
			navigationItem = new NavigationItem();
			navigationItem.navigatorId = navigatorId;
			navigationItem.id = QUERY_ID;
			navigationItem.type = NavigationItemTypes.DIALOG;
			navigationItem.label = resourceManager.getString( 'infinite', 'header.advancedButton' );
			navigationItem.icon = EmbeddedAssets.ADVANCED_OFF;
			navigationItem.altIcon = EmbeddedAssets.ADVANCED_ON;
			navDialogs.addItem( navigationItem );
			
			// sources
			navigationItem = new NavigationItem();
			navigationItem.navigatorId = navigatorId;
			navigationItem.id = SOURCES_ID;
			navigationItem.type = NavigationItemTypes.DIALOG;
			navigationItem.label = resourceManager.getString( 'infinite', 'header.sourcesButton' );
			navigationItem.icon = EmbeddedAssets.SOURCES_OFF;
			navigationItem.altIcon = EmbeddedAssets.SOURCES_ON;
			navDialogs.addItem( navigationItem );
			
			// history
			navigationItem = new NavigationItem();
			navigationItem.navigatorId = navigatorId;
			navigationItem.id = HISTORY_ID;
			navigationItem.type = NavigationItemTypes.DIALOG;
			navigationItem.label = resourceManager.getString( 'infinite', 'header.historyButton' );
			navigationItem.icon = EmbeddedAssets.HISTORY_OFF;
			navigationItem.altIcon = EmbeddedAssets.HISTORY_ON;
			navDialogs.addItem( navigationItem );
			
			// set dialogs
			setDialogs( navDialogs );
		}
		
		/**
		 * Navigate Actions
		 */
		protected function navigateActions( navigationItem:NavigationItem ):void
		{
			switch ( navigationItem.id )
			{
				case PROFILE_ID:
				{
					resetItemsToDefault( NavigationItemTypes.ALL );
					super.navigate( navigationItem );
					break;
				}
				case MANAGER_ID:
				{
					// (do nothing, handled directly by the callback to avoid pop-up security issues)
					break;
				}
				case LOGOUT_ID:
				{
					model.logout();
					break;
				}
			}
		}
		
		/**
		 * Navigate Dialogs
		 */
		protected function navigateDialogs( navigationItem:NavigationItem ):void
		{
			switch ( navigationItem.id )
			{
				case QUERY_ID:
				{
					navigateById( NavigationConstants.WORKSPACES_QUERY_ID );
					break;
				}
				case SOURCES_ID:
				{
					navigateById( NavigationConstants.WORKSPACES_SOURCES_ID );
					break;
				}
				case HISTORY_ID:
				{
					navigateById( NavigationConstants.WORKSPACES_HISTORY_ID );
					break;
				}
			}
			
			resetItemsToDefault( NavigationItemTypes.ALL );
			
			super.navigate( navigationItem );
		}
	}
}
