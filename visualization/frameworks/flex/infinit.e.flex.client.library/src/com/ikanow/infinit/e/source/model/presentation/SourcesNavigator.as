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
package com.ikanow.infinit.e.source.model.presentation
{
	import com.ikanow.infinit.e.shared.model.constant.NavigationConstants;
	import com.ikanow.infinit.e.shared.model.constant.types.NavigationItemTypes;
	import com.ikanow.infinit.e.shared.model.presentation.base.Navigator;
	import com.ikanow.infinit.e.shared.model.vo.ui.INavigationItem;
	import com.ikanow.infinit.e.shared.model.vo.ui.NavigationItem;
	import com.ikanow.infinit.e.shared.util.StateUtil;
	import mx.collections.ArrayCollection;
	import mx.core.UIComponent;
	import mx.resources.IResourceManager;
	import mx.resources.ResourceManager;
	
	/**
	 * Sources Navigator
	 */
	public class SourcesNavigator extends Navigator
	{
		
		//======================================
		// private static properties 
		//======================================
		
		private static const SOURCES_AVAILABLE_ID:String = NavigationConstants.SOURCES_AVAILABLE_ID;
		
		private static const SOURCES_SELECTED_ID:String = NavigationConstants.SOURCES_SELECTED_ID;
		
		private static const COMMUNITY_ID:String = NavigationConstants.SOURCES_COMMUNITY_ID;
		
		
		//======================================
		// public properties 
		//======================================
		
		/**
		 * Presentation Model
		 */
		[Bindable]
		[Inject]
		public var model:SourcesModel;
		
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
		public function SourcesNavigator()
		{
			navigatorId = NavigationConstants.WORKSPACES_SOURCES_ID;
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
			StateUtil.setStates( component, [ SOURCES_AVAILABLE_ID, SOURCES_SELECTED_ID ], state );
		}
		
		
		//======================================
		// public methods 
		//======================================
		
		/**
		 * Close Sources View
		 */
		public function closeSourcesView():void
		{
			navigateById( NavigationConstants.WORKSPACES_BODY_ID );
		}
		
		/**
		 * Navigate
		 */
		override public function navigate( navigationItem:* ):void
		{
			super.navigate( navigationItem );
			
			if ( navigationItem is INavigationItem )
			{
				switch ( navigationItem.type )
				{
					case NavigationItemTypes.STATE:
					{
						navigateStates( navigationItem );
						break;
					}
				}
			}
		}
		
		/**
		 * Show Community List View
		 */
		public function showCommunityListView():void
		{
			navigateById( NavigationConstants.COMMUNITY_LIST_ID );
		}
		
		/**
		 * Update the sources counts
		 */
		public function updateSourcesCounts():void
		{
			var navigationItem:INavigationItem;
			var selectedSourcesCount:int;
			var availableSourcesCount:int;
			
			if ( model )
			{
				if ( model.sources )
				{
					selectedSourcesCount = model.selectedSourcesCount;
					availableSourcesCount = model.sources.source.length;
				}
			}
			
			navigationItem = getItemById( SOURCES_SELECTED_ID );
			
			if ( navigationItem )
				navigationItem.label = resourceManager.getString( 'infinite', 'sources.active', [ selectedSourcesCount ] );
			
			navigationItem = getItemById( SOURCES_AVAILABLE_ID );
			
			if ( navigationItem )
				navigationItem.label = resourceManager.getString( 'infinite', 'sources.available', [ availableSourcesCount ] );
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
			var navigationItem:NavigationItem;
			
			// available
			navStates.addItem( createNavigationItem( SOURCES_AVAILABLE_ID, NavigationItemTypes.STATE, SOURCES_AVAILABLE_ID ) );
			
			// selected
			navStates.addItem( createNavigationItem( SOURCES_SELECTED_ID, NavigationItemTypes.STATE, SOURCES_SELECTED_ID ) );
			
			// set states - default available sources
			setStates( navStates, SOURCES_AVAILABLE_ID );
			
			// update sources counts
			updateSourcesCounts();
		}
		
		/**
		 * Handle state changes
		 */
		protected function navigateStates( navigationItem:NavigationItem ):void
		{
			switch ( navigationItem.id )
			{
				case SOURCES_SELECTED_ID:
				{
					model.showSelectedSources();
					break;
				}
				case SOURCES_AVAILABLE_ID:
				{
					model.showAvailableSources();
					break;
				}
			}
		}
	}
}

