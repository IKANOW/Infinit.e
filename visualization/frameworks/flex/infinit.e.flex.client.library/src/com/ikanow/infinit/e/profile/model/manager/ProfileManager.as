package com.ikanow.infinit.e.profile.model.manager
{
	import com.ikanow.infinit.e.shared.model.manager.base.InfiniteManager;
	import com.ikanow.infinit.e.shared.model.vo.User;
	
	/**
	 * Profile Manager
	 */
	public class ProfileManager extends InfiniteManager
	{
		
		//======================================
		// public properties 
		//======================================
		
		[Bindable]
		/**
		 * The current user
		 */
		public var currentUser:User;
		
		
		//======================================
		// public methods 
		//======================================
		
		/**
		 * Person Get response from server
		 * @param value
		 */
		[Inject( "userManager.currentUser", bind = "true" )]
		public function setCurrentUser( value:User ):void
		{
			currentUser = value;
		}
	}
}
