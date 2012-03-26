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
package com.ikanow.infinit.e.workspace.model.presentation.content
{
	import com.ikanow.infinit.e.shared.model.constant.NavigationConstants;
	import com.ikanow.infinit.e.shared.model.constant.types.NavigationItemTypes;
	import com.ikanow.infinit.e.shared.model.presentation.base.Navigator;
	import com.ikanow.infinit.e.shared.util.StateUtil;
	import mx.collections.ArrayCollection;
	import mx.core.UIComponent;
	
	/**
	 * Workspace Area Content Navigator
	 */
	public class WorkspaceContentNavigator extends Navigator
	{
		
		//======================================
		// private static properties 
		//======================================
		
		private static const TILES_ID:String = NavigationConstants.WORKSPACE_CONTENT_TILES_ID;
		
		private static const MAXIMIZED_ID:String = NavigationConstants.WORKSPACE_CONTENT_MAXIMIZED_ID;
		
		
		//======================================
		// public properties 
		//======================================
		
		/**
		 * Presentation Model
		 */
		[Bindable]
		[Inject]
		public var model:WorkspaceContentModel;
		
		//======================================
		// constructor 
		//======================================
		
		/**
		 * Constructor
		 */
		public function WorkspaceContentNavigator()
		{
			navigatorId = NavigationConstants.WORKSPACE_CONTENT_ID;
			parentNavigatorId = NavigationConstants.WORKSPACES_BODY_WORKSPACE_ID;
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
			StateUtil.setStates( component, [ TILES_ID, MAXIMIZED_ID ], state );
		}
		
		
		//======================================
		// public methods 
		//======================================
		
		/**
		 * Show Maximized Layout
		 */
		public function showMaximizedLayout():void
		{
			navigateById( MAXIMIZED_ID );
		}
		
		/**
		 * Show Tiles Layout
		 */
		public function showTilesLayout():void
		{
			navigateById( TILES_ID );
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
			
			// tiles
			navStates.addItem( createNavigationItem( TILES_ID, NavigationItemTypes.STATE, TILES_ID ) );
			
			// maximized
			navStates.addItem( createNavigationItem( MAXIMIZED_ID, NavigationItemTypes.STATE, MAXIMIZED_ID ) );
			
			// set states - default tiles
			setStates( navStates, TILES_ID );
		}
	}
}
