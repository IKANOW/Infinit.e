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
package com.ikanow.infinit.e.shared.control
{
	import com.ikanow.infinit.e.shared.event.NavigationEvent;
	import com.ikanow.infinit.e.shared.model.manager.NavigationManager;
	import com.ikanow.infinit.e.shared.model.presentation.base.INavigator;
	import com.ikanow.infinit.e.shared.model.vo.ui.INavigationItem;
	
	/**
	 * Navigation Controller
	 */
	public class NavigationController
	{
		
		//======================================
		// public properties 
		//======================================
		
		[Inject]
		public var navigationManager:NavigationManager;
		
		
		//======================================
		// public methods 
		//======================================
		
		[EventHandler( event = "NavigationEvent.RESET" )]
		/**
		 * Reset Navigation
		 * @param event
		 */
		public function resetNavigation( event:NavigationEvent ):void
		{
			navigationManager.reset();
		}
		
		[EventHandler( event = "NavigationEvent.NAVIGATE", properties = "navigationItem" )]
		/**
		 * Navigate
		 * @param navigationItem
		 */
		public function navigate( navigationItem:INavigationItem ):void
		{
			navigationManager.navigate( navigationItem );
		}
		
		[EventHandler( event = "NavigationEvent.NAVIGATE_BY_ID", properties = "navigationItemId" )]
		/**
		 * Navigate by ID
		 * @param navigationItemId
		 */
		public function navigateById( navigationItemId:String ):void
		{
			navigationManager.navigateById( navigationItemId );
		}
		
		[EventHandler( event = "NavigationEvent.NAVIGATE_TO_FIRST_VIEW", properties = "navigatorId" )]
		/**
		 * Navigate to First View
		 * @param navigatorId
		 */
		public function navigateToFirstView( navigatorId:String ):void
		{
			navigationManager.navigateToFirstView( navigatorId );
		}
		
		[EventHandler( event = "NavigationEvent.REGISTER_NAVIGATOR", properties = "navigator" )]
		/**
		 * Register Navigator
		 * Add the navigator to the navigators dictionary
		 * @param navigator
		 */
		public function registerNavigator( navigator:INavigator ):void
		{
			navigationManager.registerNavigator( navigator );
		}
		
		[EventHandler( event = "NavigationEvent.RESET_TO_DEFAULT", properties = "navigatorId, itemType" )]
		/**
		 * Reset to Default
		 * Reset the to the default for a navigation item type
		 * @param navigator
		 */
		public function resetToDefault( navigatorId:String, itemType:String ):void
		{
			navigationManager.resetToDefault( navigatorId, itemType );
		}
	}
}
