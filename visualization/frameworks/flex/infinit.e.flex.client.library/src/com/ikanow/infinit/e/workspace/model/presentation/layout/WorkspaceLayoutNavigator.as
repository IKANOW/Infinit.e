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
