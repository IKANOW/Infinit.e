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
package com.ikanow.infinit.e.query.model.presentation
{
	import com.ikanow.infinit.e.shared.model.constant.NavigationConstants;
	import com.ikanow.infinit.e.shared.model.constant.types.NavigationItemTypes;
	import com.ikanow.infinit.e.shared.model.presentation.base.Navigator;
	import com.ikanow.infinit.e.shared.util.StateUtil;
	import mx.collections.ArrayCollection;
	import mx.core.UIComponent;
	import mx.resources.ResourceManager;
	
	/**
	 * Query Navigator
	 */
	public class QueryNavigator extends Navigator
	{
		
		//======================================
		// private static properties 
		//======================================
		
		private static const BUILDER_ID:String = NavigationConstants.QUERY_BUILDER_ID;
		
		private static const SETTINGS_ID:String = NavigationConstants.QUERY_SETTINGS_ID;
		
		
		//======================================
		// public properties 
		//======================================
		
		/**
		 * Presentation Model
		 */
		[Bindable]
		[Inject]
		public var model:QueryModel;
		
		//======================================
		// constructor 
		//======================================
		
		/**
		 * Constructor
		 */
		public function QueryNavigator()
		{
			navigatorId = NavigationConstants.WORKSPACES_QUERY_ID;
			parentNavigatorId = NavigationConstants.WORKSPACES_ID;
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
			StateUtil.setStates( component, [ BUILDER_ID, SETTINGS_ID ], state );
		}
		
		
		//======================================
		// public methods 
		//======================================
		
		/**
		 * Close Query View
		 */
		public function closeQueryView():void
		{
			navigateById( NavigationConstants.WORKSPACES_BODY_ID );
		}
		
		/**
		 * Show Builder View
		 */
		public function showBuilderView():void
		{
			navigateById( BUILDER_ID );
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
			
			// builder
			navStates.addItem( createNavigationItem( BUILDER_ID, NavigationItemTypes.STATE, BUILDER_ID, ResourceManager.getInstance().getString( 'infinite', 'query.queryBuilder' ) ) );
			
			// settings
			navStates.addItem( createNavigationItem( SETTINGS_ID, NavigationItemTypes.STATE, SETTINGS_ID, ResourceManager.getInstance().getString( 'infinite', 'query.querySettings' ) ) );
			
			// set states - default query builder
			setStates( navStates, BUILDER_ID );
		}
	}
}

