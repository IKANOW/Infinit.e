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
package com.ikanow.infinit.e.model.presentation.dashboard.workspaces
{
	import com.ikanow.infinit.e.shared.model.constant.NavigationConstants;
	import com.ikanow.infinit.e.shared.model.constant.types.NavigationItemTypes;
	import com.ikanow.infinit.e.shared.model.presentation.base.Navigator;
	import com.ikanow.infinit.e.shared.util.StateUtil;
	import mx.collections.ArrayCollection;
	import mx.core.UIComponent;
	
	/**
	 * Workspaces Navigator
	 */
	public class WorkspacesNavigator extends Navigator
	{
		
		//======================================
		// public static properties 
		//======================================
		
		public static const BODY_ID:String = NavigationConstants.WORKSPACES_BODY_ID;
		
		public static const WELCOME_ID:String = NavigationConstants.WORKSPACES_WELCOME_ID;
		
		public static const QUERY_ID:String = NavigationConstants.WORKSPACES_QUERY_ID;
		
		public static const SOURCES_ID:String = NavigationConstants.WORKSPACES_SOURCES_ID;
		
		public static const HISTORY_ID:String = NavigationConstants.WORKSPACES_HISTORY_ID;
		
		
		//======================================
		// public properties 
		//======================================
		
		/**
		 * Presentation Model
		 */
		[Bindable]
		[Inject]
		public var model:WorkspacesModel;
		
		//======================================
		// constructor 
		//======================================
		
		/**
		 * Constructor
		 */
		public function WorkspacesNavigator()
		{
			navigatorId = NavigationConstants.WORKSPACES_ID;
			parentNavigatorId = NavigationConstants.MAIN_DASHBOARD_ID;
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
			StateUtil.setStates( component, [ BODY_ID, WELCOME_ID, QUERY_ID, SOURCES_ID, HISTORY_ID ], state );
		}
		
		
		//======================================
		// public methods 
		//======================================
		
		/**
		 * Override change state
		 * reset the Header button bar selected items
		 */
		override public function changeState( state:String ):Boolean
		{
			if ( state == BODY_ID )
			{
				resetToDefault( NavigationConstants.DASHBOARD_HEADER_ID, NavigationItemTypes.DIALOG );
			}
			
			return super.changeState( state );
		}
		
		/**
		 * Show Body View
		 */
		public function showBodyView():void
		{
			navigateById( BODY_ID );
		}
		
		/**
		 * Show History View
		 */
		public function showHistoryView():void
		{
			navigateById( HISTORY_ID );
		}
		
		/**
		 * Show Query View
		 */
		public function showQueryView():void
		{
			navigateById( QUERY_ID );
		}
		
		/**
		 * Show Sources View
		 */
		public function showSourcesView():void
		{
			navigateById( SOURCES_ID );
		}
		
		/**
		 * Show Welcome View
		 */
		public function showWelcomeView():void
		{
			navigateById( WELCOME_ID );
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
			
			// workspace
			navStates.addItem( createNavigationItem( BODY_ID, NavigationItemTypes.STATE, BODY_ID ) );
			
			// welcome
			navStates.addItem( createNavigationItem( WELCOME_ID, NavigationItemTypes.STATE, WELCOME_ID ) );
			
			// query
			navStates.addItem( createNavigationItem( QUERY_ID, NavigationItemTypes.STATE, QUERY_ID ) );
			
			// sources
			navStates.addItem( createNavigationItem( SOURCES_ID, NavigationItemTypes.STATE, SOURCES_ID ) );
			
			// history
			navStates.addItem( createNavigationItem( HISTORY_ID, NavigationItemTypes.STATE, HISTORY_ID ) );
			
			// set states - default body
			setStates( navStates, BODY_ID );
		}
	}
}
