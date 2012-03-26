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
package com.ikanow.infinit.e.community.model.presentation
{
	import com.ikanow.infinit.e.shared.event.CommunityEvent;
	import com.ikanow.infinit.e.shared.model.presentation.base.PresentationModel;
	import com.ikanow.infinit.e.shared.model.vo.ui.DialogControl;
	
	/**
	 *  Community Presentation Model
	 */
	public class CommunitiesModel extends PresentationModel
	{
		
		//======================================
		// public properties 
		//======================================
		
		[Bindable]
		[Inject]
		public var navigator:CommunitiesNavigator;
		
		
		//======================================
		// public methods 
		//======================================
		
		/**
		 * Select all communities
		 */
		public function selectAllCommunities():void
		{
			var communityEvent:CommunityEvent = new CommunityEvent( CommunityEvent.SELECT_ALL_COMMUNITIES );
			communityEvent.dialogControl = DialogControl.create( false );
			dispatcher.dispatchEvent( communityEvent );
		}
		
		/**
		 * Select no communities
		 */
		public function selectNoCommunities():void
		{
			var communityEvent:CommunityEvent = new CommunityEvent( CommunityEvent.SELECT_NO_COMMUNITIES );
			communityEvent.dialogControl = DialogControl.create( false );
			dispatcher.dispatchEvent( communityEvent );
		}
	}
}

