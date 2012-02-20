package com.ikanow.infinit.e.community.service
{
	import com.ikanow.infinit.e.shared.event.CommunityEvent;
	
	import mx.rpc.AsyncToken;
	
	public interface ICommunityServiceDelegate
	{
		/**
		 * Get Communities Public
		 * Retrieves the public communities
		 * @param event
		 * @return AsyncToken
		 */
		function getCommunitiesPublic( event:CommunityEvent ):AsyncToken;
		
		/**
		 * Join Community
		 * @param event
		 * @return AsyncToken
		 */
		function joinCommunity( event:CommunityEvent ):AsyncToken;
		
		/**
		 * Leave Community
		 * @param event
		 * @return AsyncToken
		 */
		function leaveCommunity( event:CommunityEvent ):AsyncToken;
	}
}
