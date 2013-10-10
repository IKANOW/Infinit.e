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
package com.ikanow.infinit.e.community.control
{
	import com.ikanow.infinit.e.community.model.manager.CommunityManager;
	import com.ikanow.infinit.e.community.service.ICommunityServiceDelegate;
	import com.ikanow.infinit.e.shared.control.base.InfiniteController;
	import com.ikanow.infinit.e.shared.event.CommunityEvent;
	import com.ikanow.infinit.e.shared.model.vo.CommunityApproval;
	import com.ikanow.infinit.e.shared.model.vo.ui.ServiceResponse;
	import com.ikanow.infinit.e.shared.model.vo.ui.ServiceResult;
	import mx.collections.ArrayCollection;
	import mx.rpc.events.ResultEvent;
	
	/**
	 * Community Controller
	 */
	public class CommunityController extends InfiniteController
	{
		
		//======================================
		// public properties 
		//======================================
		
		[Inject]
		public var communityServiceDelegate:ICommunityServiceDelegate
		
		[Inject]
		public var communityManager:CommunityManager;
		
		
		//======================================
		// public methods 
		//======================================
		
		[EventHandler( event = "CommunityEvent.GET_COMMUNITIES_ALL" )]
		/**
		 * Get Communities Public
		 * @param event
		 */
		public function getCommunitiesAll( event:CommunityEvent ):void
		{
			executeServiceCall( "CommunityController.getCommunitiesAll()", event, communityServiceDelegate.getCommunitiesAll( event ), getCommunitiesAll_resultHandler, defaultFaultHandler );
		}
		
		/**
		 * Get Communities Public Result Handler
		 * @param event
		 */
		public function getCommunitiesAll_resultHandler( event:ResultEvent ):void
		{
			if ( verifyServiceResponseSuccess( "getCommunitiesAll()", event.result as ServiceResult ) )
				communityManager.setCommunities( ServiceResult( event.result ).data as ArrayCollection );
		}
		
		[EventHandler( event = "CommunityEvent.GET_COMMUNITIES_PUBLIC" )]
		/**
		 * Get Communities Public
		 * @param event
		 */
		public function getCommunitiesPublic( event:CommunityEvent ):void
		{
			executeServiceCall( "CommunityController.getCommunitiesPublic()", event, communityServiceDelegate.getCommunitiesPublic( event ), getCommunitiesPublic_resultHandler, defaultFaultHandler );
		}
		
		/**
		 * Get Communities Public Result Handler
		 * @param event
		 */
		public function getCommunitiesPublic_resultHandler( event:ResultEvent ):void
		{
			if ( verifyServiceResponseSuccess( "getCommunitiesPublic()", event.result as ServiceResult ) )
				communityManager.setCommunities( ServiceResult( event.result ).data as ArrayCollection );
		}
		
		[EventHandler( event = "CommunityEvent.REFRESH" )]
		/**
		 * Get Communities Public
		 * @param event
		 */
		public function getCommunitiesRefresh( event:CommunityEvent ):void
		{
			communityManager.refreshing = true;
			//getCommunitiesPublic( event );
			getCommunitiesAll(event);
		}
		
		[EventHandler( event = "CommunityEvent.JOIN_COMMUNITY" )]
		/**
		 * Join Community
		 * @param event
		 */
		public function joinCommunity( event:CommunityEvent ):void
		{
			executeServiceCall( "CommunityController.joinCommunity()", event, communityServiceDelegate.joinCommunity( event ), joinCommunity_resultHandler, defaultFaultHandler );
		}
		
		/**
		 * Join Result Handler
		 * @param event
		 */
		public function joinCommunity_resultHandler( event:ResultEvent ):void
		{
			if ( verifyServiceResponseSuccess( "joinCommunity()", event.result as ServiceResult ) )
				communityManager.joinCommunity_resultHandler( ServiceResult( event.result ).data as CommunityApproval );
		}
		
		/**
		 * Leave Result Handler
		 * @param event
		 */
		public function leaveCommunity_resultHandler( event:ResultEvent ):void
		{
			if ( verifyServiceResponseSuccess( "leaveCommunity()", event.result as ServiceResult ) )
				communityManager.leaveCommunity_resultHandler();
		}
		
		[EventHandler( event = "CommunityEvent.LEAVE_COMMUNITY" )]
		/**
		 * Leave Community
		 * @param event
		 */
		public function leaveComunity( event:CommunityEvent ):void
		{
			executeServiceCall( "CommunityController.leaveCommunity()", event, communityServiceDelegate.leaveCommunity( event ), leaveCommunity_resultHandler, defaultFaultHandler );
		}
		
		[EventHandler( event = "CommunityEvent.RESET" )]
		/**
		 * Reset Communities
		 * @param event
		 */
		public function resetCommunities( event:CommunityEvent ):void
		{
			communityManager.reset();
		}
		
		[EventHandler( event = "CommunityEvent.SELECT_ALL_COMMUNITIES" )]
		/**
		 * Select All Communities
		 * @param event
		 */
		public function selectAllCommunities( event:CommunityEvent ):void
		{
			communityManager.selectAllCommunities();
		}
		
		[EventHandler( event = "CommunityEvent.SELECT_COMMUNITY" )]
		/**
		 * Select Community
		 * @param event
		 */
		public function selectCommunity( event:CommunityEvent ):void
		{
			communityManager.setSelectedCommunity( event.community );
		}
		
		[EventHandler( event = "CommunityEvent.SELECT_NO_COMMUNITIES" )]
		/**
		 * Select No Communities
		 * @param event
		 */
		public function selectNoCommunities( event:CommunityEvent ):void
		{
			communityManager.selectNoCommunities();
		}
	}
}
