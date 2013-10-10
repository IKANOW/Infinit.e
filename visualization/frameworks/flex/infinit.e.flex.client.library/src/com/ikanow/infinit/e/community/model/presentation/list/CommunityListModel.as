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
	import com.ikanow.infinit.e.shared.event.CommunityEvent;
	import com.ikanow.infinit.e.shared.model.presentation.base.PresentationModel;
	import com.ikanow.infinit.e.shared.model.vo.Community;
	import com.ikanow.infinit.e.shared.model.vo.ui.DialogControl;
	import com.ikanow.infinit.e.shared.util.FilterUtil;
	import flash.utils.setTimeout;
	import mx.collections.ArrayCollection;
	import mx.events.CollectionEvent;
	
	/**
	 *  Community List Presentation Model
	 */
	public class CommunityListModel extends PresentationModel
	{
		
		//======================================
		// public properties 
		//======================================
		
		[Bindable]
		[Inject]
		public var navigator:CommunityListNavigator;
		
		[Bindable]
		/**
		 * The collection of communities
		 */
		public var communities:ArrayCollection;
		
		[Bindable]
		[Inject( "communityManager.selectedCommunity", bind = "true" )]
		/**
		 * The selected community
		 */
		public var selectedCommunity:Community;
		
		[Bindable]
		/**
		 * The search term
		 */
		public var searchTerm:String;
		
		
		//======================================
		// public methods 
		//======================================
		
		/**
		 * Clear communities search
		 */
		public function clearCommunitiesSearch():void
		{
			searchTerm = "";
			communities.refresh();
		}
		
		/**
		 * Search communities
		 */
		public function searchCommunities( searchTerm:String ):void
		{
			this.searchTerm = searchTerm;
			communities.filterFunction = communitiesFilterFunction;
			communities.refresh();
		}
		
		/**
		 * Select a community
		 * @param community
		 */
		public function selectCommunity( community:Community ):void
		{
			var communityEvent:CommunityEvent = new CommunityEvent( CommunityEvent.SELECT_COMMUNITY );
			communityEvent.community = community;
			communityEvent.dialogControl = DialogControl.create( false );
			dispatcher.dispatchEvent( communityEvent );
		}
		
		/**
		 * Select a community to join
		 * @param community
		 */
		public function selectCommunityToJoin( community:Community ):void
		{
			selectCommunity( community );
			setTimeout( navigator.showCommunityRequestView, 200 );
		}
		
		/**
		 * Public Communities Collection
		 * @param value
		 */
		[Inject( "communityManager.communities", bind = "true" )]
		public function setCommunities( value:ArrayCollection ):void
		{
			communities = value;
		}
		
		//======================================
		// protected methods 
		//======================================
		
		/**
		 * Communities filter function
		 * Used for searching the communities
		 * @param item
		 * @return
		 */
		protected function communitiesFilterFunction( item:Object ):Boolean
		{
			if ( searchTerm == null || searchTerm.length == 0 )
				return true;
			else
				return FilterUtil.checkAllSearchTerms( searchTerm, [ Community( item ).name ] );
		}
	}
}

