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
package com.ikanow.infinit.e.shared.service.user
{
	import com.ikanow.infinit.e.shared.event.UserEvent;
	import mx.rpc.AsyncToken;
	
	public interface IUserServiceDelegate
	{
		/**
		 * Get User
		 * Retrieves a user
		 * @param event
		 * @return AsyncToken
		 */
		function getUser( event:UserEvent ):AsyncToken;
		
		/**
		 * Get User Activity
		 * Returns currently logged in users recent activity based on what type of activity you search for.
		 * Will only bring back limit amount of results and will start at skip value.
		 * @param event
		 * @return AsyncToken
		 */
		function getUserActivity( event:UserEvent ):AsyncToken;
		
		/**
		 * Get User Profile
		 * Returns a users profile information
		 * @param event
		 * @return AsyncToken
		 */
		function getUserProfile( event:UserEvent ):AsyncToken;
		
		/**
		 * Get User Sources
		 * Returns the currently logged in user's sources they are a member of.
		 * @param event
		 * @return AsyncToken
		 */
		function getUserSources( event:UserEvent ):AsyncToken;
	}
}
