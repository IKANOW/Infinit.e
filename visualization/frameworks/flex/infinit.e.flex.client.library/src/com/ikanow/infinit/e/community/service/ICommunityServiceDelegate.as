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
package com.ikanow.infinit.e.community.service
{
	import com.ikanow.infinit.e.shared.event.CommunityEvent;
	
	import mx.rpc.AsyncToken;
	
	public interface ICommunityServiceDelegate
	{
		/**
		 * Get Communities All
		 * Retrieves the All communities
		 * @param event
		 * @return AsyncToken
		 */
		function getCommunitiesAll( event:CommunityEvent ):AsyncToken;
		
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
