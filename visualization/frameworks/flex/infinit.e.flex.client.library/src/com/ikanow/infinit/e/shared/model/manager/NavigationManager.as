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
package com.ikanow.infinit.e.shared.model.manager
{
	import com.ikanow.infinit.e.shared.model.constant.types.NavigationItemTypes;
	import com.ikanow.infinit.e.shared.model.presentation.base.INavigator;
	import com.ikanow.infinit.e.shared.model.vo.ui.INavigationItem;
	import flash.events.Event;
	import flash.events.EventDispatcher;
	import flash.utils.Dictionary;
	import mx.collections.ArrayCollection;
	
	/**
	 * Navigation Manager
	 */
	public class NavigationManager
	{
		
		//======================================
		// protected properties 
		//======================================
		
		protected var navigators:Dictionary = new Dictionary( true );
		
		protected var navigator:INavigator;
		
		protected var navigationItem:INavigationItem;
		
		protected var actionChanged:Boolean;
		
		protected var dialogChanged:Boolean;
		
		protected var stateChanged:Boolean;
		
		protected var viewChanged:Boolean;
		
		
		//======================================
		// public methods 
		//======================================
		
		/**
		 * Navigate
		 * @param navigationItem
		 */
		public function navigate( navigationItem:INavigationItem ):void
		{
			var navigator:INavigator = INavigator( navigators[ navigationItem.navigatorId ] );
			
			switch ( navigationItem.type )
			{
				case NavigationItemTypes.STATE:
					stateChanged = navigator.changeState( navigationItem.state );
					break;
				case NavigationItemTypes.VIEW:
					viewChanged = navigator.changeView( navigationItem );
					break;
				case NavigationItemTypes.DIALOG:
					dialogChanged = navigator.changeDialog( navigationItem );
					break;
				case NavigationItemTypes.ACTION:
					actionChanged = navigator.changeAction( navigationItem );
					break;
				default:
					break;
			}
		}
		
		/**
		 * Navigate by ID
		 * @param navigationItemId
		 */
		public function navigateById( navigationItemId:String ):void
		{
			for each ( var navigator:INavigator in navigators )
			{
				for each ( var navigationItem:INavigationItem in navigator.navigationItems )
				{
					if ( navigationItem.id == navigationItemId )
					{
						navigate( navigationItem );
						break;
					}
				}
			}
		}
		
		/**
		 * Navigate to First View
		 * @param navigatorId
		 */
		public function navigateToFirstView( navigatorId:String ):void
		{
			navigator = getNavigatorById( navigatorId );
			navigationItem = navigator.views.getItemAt( 0 ) as INavigationItem;
			navigateById( navigationItem.id );
		}
		
		/**
		 * Register Navigator
		 * Add the navigator to the navigators dictionary
		 * @param navigator
		 */
		public function registerNavigator( navigator:INavigator ):void
		{
			// add the navigator if it doesn't exist
			if ( navigators[ navigator.navigatorId ] == null )
			{
				// add navigator
				navigators[ navigator.navigatorId ] = navigator;
				
				// mark navigator registered
				navigator.registered = true;
			}
		}
		
		/**
		 * Reset
		 * Used to reset on logout
		 */
		public function reset():void
		{
			for each ( var navigator:INavigator in navigators )
			{
				navigator.resetItemsToDefault( NavigationItemTypes.ALL );
			}
		}
		
		/**
		 * Reset To Default
		 * Used to reset a navigator collection to the default item
		 * @param navigatorId
		 * @param itemType
		 */
		public function resetToDefault( navigatorId:String, itemType:String ):void
		{
			navigator = getNavigatorById( navigatorId );
			
			if ( navigator )
				navigator.resetItemsToDefault( itemType );
		}
		
		//======================================
		// protected methods 
		//======================================
		
		/**
		 * Returns a navigator by ID
		 * @param navigatorId
		 * @return
		 */
		protected function getNavigatorById( navigatorId:String ):INavigator
		{
			return navigators[ navigatorId ];
		}
	}
}
