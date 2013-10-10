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
package com.ikanow.infinit.e.widget.model.presentation
{
	import com.ikanow.infinit.e.shared.model.constant.NavigationConstants;
	import com.ikanow.infinit.e.shared.model.constant.types.NavigationItemTypes;
	import com.ikanow.infinit.e.shared.model.presentation.base.Navigator;
	import com.ikanow.infinit.e.shared.util.StateUtil;
	import mx.collections.ArrayCollection;
	import mx.core.UIComponent;
	
	/**
	 * Widgets Navigator
	 */
	public class WidgetsNavigator extends Navigator
	{
		
		//======================================
		// public static properties 
		//======================================
		
		public static const WIDGET_LIST_ID:String = NavigationConstants.WIDGET_LIST_ID;
		
		public static const WIDGET_EDITOR_ID:String = NavigationConstants.WIDGET_EDITOR_ID;
		
		public static const WORKSPACES_BODY_DRAWER_CLOSED_ID:String = NavigationConstants.WORKSPACES_BODY_DRAWER_CLOSED_ID;
		
		public static const WORKSPACES_BODY_DRAWER_OPEN_ID:String = NavigationConstants.WORKSPACES_BODY_DRAWER_OPEN_ID;
		
		
		//======================================
		// public properties 
		//======================================
		
		/**
		 * Presentation Model
		 */
		[Bindable]
		[Inject]
		public var model:WidgetsModel;
		
		//======================================
		// constructor 
		//======================================
		
		/**
		 * Constructor
		 */
		public function WidgetsNavigator()
		{
			navigatorId = NavigationConstants.WORKSPACES_BODY_WIDGETS_ID;
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
			StateUtil.setStates( component, [ WIDGET_LIST_ID, WIDGET_EDITOR_ID ], state );
		}
		
		
		//======================================
		// public methods 
		//======================================
		
		/**
		 * Close Drawer
		 */
		public function closeDrawer():void
		{
			navigateById( WORKSPACES_BODY_DRAWER_CLOSED_ID );
		}
		
		/**
		 * Open Drawer
		 */
		public function openDrawer():void
		{
			navigateById( WORKSPACES_BODY_DRAWER_OPEN_ID );
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
			
			// list
			navStates.addItem( createNavigationItem( WIDGET_LIST_ID, NavigationItemTypes.STATE, WIDGET_LIST_ID ) );
			
			// editor
			navStates.addItem( createNavigationItem( WIDGET_EDITOR_ID, NavigationItemTypes.STATE, WIDGET_EDITOR_ID ) );
			
			// set states - default list
			setStates( navStates, WIDGET_LIST_ID );
		}
	}
}
