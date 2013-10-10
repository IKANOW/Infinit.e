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
package com.ikanow.infinit.e.shared.control
{
	import com.ikanow.infinit.e.query.model.manager.QueryManager;
	import com.ikanow.infinit.e.shared.control.base.InfiniteController;
	import com.ikanow.infinit.e.shared.event.SetupEvent;
	import com.ikanow.infinit.e.shared.event.UserEvent;
	import com.ikanow.infinit.e.shared.model.manager.UserManager;
	import com.ikanow.infinit.e.shared.model.vo.User;
	import com.ikanow.infinit.e.shared.model.vo.ui.DialogControl;
	import com.ikanow.infinit.e.shared.model.vo.ui.ServiceResult;
	import com.ikanow.infinit.e.shared.service.user.IUserServiceDelegate;
	import flash.utils.setTimeout;
	import mx.resources.ResourceManager;
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
		
		[Inject]
		public var queryManager:QueryManager;
		
		
		//======================================
		// public methods 
		//======================================
		
		[EventHandler( event = "UserEvent.REFRESH" )]
		/**
		 * Get User
		 * @param event
		 */
		public function getRefresh( event:UserEvent ):void
		{
			queryManager.refreshing = true;
			getUser( event );
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
			{
				userManager.setCurrentUser( ServiceResult( event.result ).data as User );
				//now that current user is set we can ask for the widgetsave
				setTimeout( getWidgetOptions, 500 );
			}
		}
		
		[EventHandler( event = "UserEvent.RESET" )]
		/**
		 * Reset User
		 * @param event
		 */
		public function resetUser( event:UserEvent ):void
		{
			userManager.reset();
		}
		
		//======================================
		// protected methods 
		//======================================
		
		/**
		 * Gets a users stored widget save options
		 *
		 */
		protected function getWidgetOptions():void
		{
			var setupEvent:SetupEvent = new SetupEvent( SetupEvent.GET_WIDGET_OPTIONS );
			setupEvent.dialogControl = DialogControl.create( false, ResourceManager.getInstance().getString( 'infinite', 'setupService.getWidgetOptions' ) );
			dispatcher.dispatchEvent( setupEvent );
		}
	}
}
