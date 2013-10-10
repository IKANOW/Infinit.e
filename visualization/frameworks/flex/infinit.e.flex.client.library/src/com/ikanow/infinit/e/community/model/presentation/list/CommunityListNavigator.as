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
package com.ikanow.infinit.e.community.model.presentation.list
{
	import com.ikanow.infinit.e.shared.model.constant.NavigationConstants;
	import com.ikanow.infinit.e.shared.model.presentation.base.Navigator;
	
	/**
	 * Community List Navigator
	 */
	public class CommunityListNavigator extends Navigator
	{
		
		//======================================
		// public properties 
		//======================================
		
		/**
		 * Presentation Model
		 */
		[Bindable]
		[Inject]
		public var model:CommunityListModel;
		
		//======================================
		// constructor 
		//======================================
		
		/**
		 * Constructor
		 */
		public function CommunityListNavigator()
		{
			navigatorId = NavigationConstants.COMMUNITY_LIST_ID;
			parentNavigatorId = NavigationConstants.SOURCES_COMMUNITY_ID;
		}
		
		
		//======================================
		// public methods 
		//======================================
		
		/**
		 * Show Community Request View
		 */
		public function showCommunityRequestView():void
		{
			navigateById( NavigationConstants.COMMUNITY_REQUEST_JOIN_PROMPT_ID );
			navigateById( NavigationConstants.COMMUNITY_REQUEST_ID );
		}
	}
}
