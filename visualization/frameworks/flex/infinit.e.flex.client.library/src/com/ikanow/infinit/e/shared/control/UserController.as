package com.ikanow.infinit.e.shared.control
{
	import com.ikanow.infinit.e.shared.control.base.InfiniteController;
	import com.ikanow.infinit.e.shared.event.UserEvent;
	import com.ikanow.infinit.e.shared.model.manager.UserManager;
	import com.ikanow.infinit.e.shared.model.vo.User;
	import com.ikanow.infinit.e.shared.model.vo.ui.ServiceResult;
	import com.ikanow.infinit.e.shared.service.user.IUserServiceDelegate;
	import mx.rpc.events.ResultEvent;
	
	/**
	 * User Controller
	 */
	public class UserController extends InfiniteController
	{
		
		//======================================
		// public properties 
		//======================================
		
		[Inject]
		public var userServiceDelegate:IUserServiceDelegate
		
		[Inject]
		public var userManager:UserManager;
		
		
		//======================================
		// public methods 
		//======================================
		
		[EventHandler( event = "UserEvent.RESET" )]
		/**
		 * Reset User
		 * @param event
		 */
		public function resetUser( event:UserEvent ):void
		{
			userManager.reset();
		}
		
		[EventHandler( event = "UserEvent.GET_USER" )]
		/**
		 * Get User
		 * @param event
		 */
		public function getUser( event:UserEvent ):void
		{
			executeServiceCall( "UserController.getUser()", event, userServiceDelegate.getUser( event ), getUser_resultHandler, defaultFaultHandler );
		}
		
		/**
		 * Get User Result Handler
		 * @param event
		 */
		public function getUser_resultHandler( event:ResultEvent ):void
		{
			if ( verifyServiceResponseSuccess( "getUser()", event.result as ServiceResult ) )
				userManager.setCurrentUser( ServiceResult( event.result ).data as User );
		}
	}
}
