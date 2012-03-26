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
package com.ikanow.infinit.e.shared.model.manager
{
	import com.ikanow.infinit.e.shared.model.manager.base.InfiniteManager;
	import com.ikanow.infinit.e.shared.model.vo.User;
	import flash.events.Event;
	import flash.events.EventDispatcher;
	
	/**
	 * User Manager
	 */
	public class UserManager extends InfiniteManager
	{
		
		//======================================
		// public properties 
		//======================================
		
		[Bindable]
		public var currentUser:User;
		
		
		//======================================
		// public methods 
		//======================================
		
		/**
		 * Reset
		 * Used to reset on logout
		 */
		public function reset():void
		{
			currentUser = null;
		}
		
		/**
		 * Login response from server
		 * @param value
		 */
		public function setCurrentUser( value:User ):void
		{
			currentUser = value;
		}
	}
}
