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
package com.ikanow.infinit.e.shared.service.session
{
	import com.ikanow.infinit.e.shared.event.SessionEvent;
	
	import mx.rpc.AsyncToken;
	
	public interface ISessionServiceDelegate
	{
		/**
		 * Get Cookie
		 * This will return a cookie that is active for 15m from last action.
		 * @param event
		 * @return AsyncToken
		 */
		function getCookie( event:SessionEvent ):AsyncToken;
		
		/**
		 * Keep Alive
		 * Updates this users cookie to extend session for another 15 minutes from now.
		 * Used to keep a session alive.  Will return false if a session has died or another user has logged in.
		 * @param event
		 * @return AsyncToken
		 */
		function keepAlive( event:SessionEvent ):AsyncToken;
		
		/**
		 * Forgot Password
		 * 
		 */
		function forgotPassword( event:SessionEvent ):AsyncToken;
		
		/**
		 * Log In
		 * Authenticates a session for the current user/environment.
		 * This will return a cookie that is active for 15m from last action.
		 * @param event
		 * @return AsyncToken
		 */
		function login( event:SessionEvent ):AsyncToken;
		
		/**
		 * Log Out
		 * Removes the current users authentication and cancels any other existing session.
		 * @param event
		 * @return AsyncToken
		 */
		function logout( event:SessionEvent ):AsyncToken;
	}
}
