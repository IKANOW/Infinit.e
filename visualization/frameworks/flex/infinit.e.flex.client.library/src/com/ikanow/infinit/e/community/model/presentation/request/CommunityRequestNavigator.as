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
package com.ikanow.infinit.e.community.model.presentation.request
{
	import com.ikanow.infinit.e.shared.model.constant.NavigationConstants;
	import com.ikanow.infinit.e.shared.model.constant.types.NavigationItemTypes;
	import com.ikanow.infinit.e.shared.model.presentation.base.Navigator;
	import com.ikanow.infinit.e.shared.util.StateUtil;
	import mx.collections.ArrayCollection;
	import mx.core.UIComponent;
	
	/**
	 * Community Request Navigator
	 */
	public class CommunityRequestNavigator extends Navigator
	{
		
		//======================================
		// public static properties 
		//======================================
		
		public static const REQUEST_JOIN_PROMPT_ID:String = NavigationConstants.COMMUNITY_REQUEST_JOIN_PROMPT_ID;
		
		public static const REQUESTING_JOIN_ID:String = NavigationConstants.COMMUNITY_REQUEST_REQUESTING_JOIN_ID;
		
		public static const REQUEST_JOIN_APPROVED_ID:String = NavigationConstants.COMMUNITY_REQUEST_JOIN_APPROVED_ID;
		
		public static const REQUEST_JOIN_PENDING_ID:String = NavigationConstants.COMMUNITY_REQUEST_JOIN_PENDING_ID;
		
		
		//======================================
		// public properties 
		//======================================
		
		/**
		 * Presentation Model
		 */
		[Bindable]
		[Inject]
		public var model:CommunityRequestModel;
		
		//======================================
		// constructor 
		//======================================
		
		/**
		 * Constructor
		 */
		public function CommunityRequestNavigator()
		{
			navigatorId = NavigationConstants.COMMUNITY_REQUEST_ID;
			parentNavigatorId = NavigationConstants.SOURCES_COMMUNITY_ID;
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
			StateUtil.setStates( component, [ REQUEST_JOIN_PROMPT_ID, REQUESTING_JOIN_ID, REQUEST_JOIN_APPROVED_ID, REQUEST_JOIN_PENDING_ID ], state );
		}
		
		
		//======================================
		// public methods 
		//======================================
		
		/**
		 * Show Community List View
		 */
		public function showCommunityListView():void
		{
			navigateById( NavigationConstants.COMMUNITY_LIST_ID );
			showRequestJoinPrompt();
		}
		
		/**
		 * Show Request Join Approved
		 */
		public function showRequestJoinApproved():void
		{
			navigateById( REQUEST_JOIN_APPROVED_ID );
		}
		
		/**
		 * Show Request Join Pending
		 */
		public function showRequestJoinPending():void
		{
			navigateById( REQUEST_JOIN_PENDING_ID );
		}
		
		/**
		 * Show Request Join Prompt
		 */
		public function showRequestJoinPrompt():void
		{
			navigateById( REQUEST_JOIN_PROMPT_ID );
		}
		
		/**
		 * Show Requesting Join
		 */
		public function showRequestingJoin():void
		{
			navigateById( REQUESTING_JOIN_ID );
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
			
			// request join
			navStates.addItem( createNavigationItem( REQUEST_JOIN_PROMPT_ID, NavigationItemTypes.STATE, REQUEST_JOIN_PROMPT_ID ) );
			
			// requesting join
			navStates.addItem( createNavigationItem( REQUESTING_JOIN_ID, NavigationItemTypes.STATE, REQUESTING_JOIN_ID ) );
			
			// request join approved
			navStates.addItem( createNavigationItem( REQUEST_JOIN_APPROVED_ID, NavigationItemTypes.STATE, REQUEST_JOIN_APPROVED_ID ) );
			
			// request join pending
			navStates.addItem( createNavigationItem( REQUEST_JOIN_PENDING_ID, NavigationItemTypes.STATE, REQUEST_JOIN_PENDING_ID ) );
			
			// set states - default request join prompt
			setStates( navStates, REQUEST_JOIN_PROMPT_ID );
		}
	}
}
