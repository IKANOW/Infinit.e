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
