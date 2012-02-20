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
