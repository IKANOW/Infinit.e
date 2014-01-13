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
	import com.ikanow.infinit.e.shared.control.base.InfiniteController;
	import com.ikanow.infinit.e.shared.event.SessionEvent;
	import com.ikanow.infinit.e.shared.model.manager.SessionManager;
	import com.ikanow.infinit.e.shared.model.vo.ui.ServiceResponse;
	import com.ikanow.infinit.e.shared.model.vo.ui.ServiceResult;
	import com.ikanow.infinit.e.shared.service.session.ISessionServiceDelegate;
	
	import mx.controls.Alert;
	import mx.rpc.events.FaultEvent;
	import mx.rpc.events.ResultEvent;
	
	/**
	 * Session Controller
	 */
	public class SessionController extends InfiniteController
	{
		
		//======================================
		// public properties 
		//======================================
		
		[Inject]
		public var sessionServiceDelegate:ISessionServiceDelegate;
		
		[Inject]
		public var sessionManager:SessionManager;
		
		
		//======================================
		// public methods 
		//======================================
		
		[EventHandler( event = "SessionEvent.DATA_LOADING" )]
		/**
		 * Data is loading
		 * @param event
		 */
		public function dataLoading( event:SessionEvent ):void
		{
			sessionManager.dataLoadinghandler( event.dialogControl );
		}
		
		[EventHandler( event = "SessionEvent.DATA_READY" )]
		/**
		 * Data is ready
		 * @param event
		 */
		public function dataReady( event:SessionEvent ):void
		{
			sessionManager.dataReadyhandler( event.dialogControl );
		}
		
		[EventHandler( event = "SessionEvent.GET_COOKIE" )]
		/**
		 * Get Cookie
		 * @param event
		 */
		public function getCookie( event:SessionEvent ):void
		{
			executeServiceCall( "SessionController.getCookie()", event, sessionServiceDelegate.getCookie( event ), getCookie_resultHandler, defaultFaultHandler );
		}
		
		/**
		 * Get Cookie Result Handler
		 * @param event
		 */
		public function getCookie_resultHandler( event:ResultEvent ):void
		{
			sessionManager.setGetCookieResponse( ServiceResult( event.result ).response as ServiceResponse );
		}
		
		[EventHandler( event = "SessionEvent.KEEP_ALIVE" )]
		/**
		 * Keep Alive
		 * @param event
		 */
		public function keepAlive( event:SessionEvent ):void
		{
			executeServiceCall( "SessionController.keepAlive()", event, sessionServiceDelegate.keepAlive( event ), keepAlive_resultHandler, defaultFaultHandler );
		}
		
		/**
		 * Keep Alive Result Handler
		 * @param event
		 */
		public function keepAlive_resultHandler( event:ResultEvent ):void
		{
			sessionManager.keepAlive_resultHandler( ServiceResult( event.result ).response as ServiceResponse );
		}
		
		[EventHandler( event = "SessionEvent.FORGOT_PASSWORD" )]
		/**
		 * Forgot Password
		 * @param event
		 */
		public function forgotPassword( event:SessionEvent ):void
		{
			executeServiceCall( "SessionController.forgotPassword()", event, sessionServiceDelegate.forgotPassword( event ), forgotPassword_resultHandler, defaultFaultHandler );
		}
		
		/**
		 * Forgot Password Result Handler
		 * @param event
		 */
		public function forgotPassword_resultHandler( event:ResultEvent ):void
		{			
			//we might not be using the infinit.e password system so we 
			//will write a generic message, and add our own text if it was us
			var response:String = "Forgot password sent successfully";
			try
			{
				var sr:ServiceResult = ServiceResult( event.result );
				if ( sr != null && sr.response != null && sr.response.responseSuccess && sr.response.message != null )
				{
					response += ": " + sr.response.message;			
				}
			}
			catch ( err:Error)
			{
				//do nothing, was probably not an infinit.e forget password call
			}
			Alert.show(response);
		}
		
		[EventHandler( event = "SessionEvent.LOGIN" )]
		/**
		 * Login
		 * @param event
		 */
		public function login( event:SessionEvent ):void
		{
			executeServiceCall( "SessionController.login()", event, sessionServiceDelegate.login( event ), login_resultHandler, defaultFaultHandler );
		}
		
		/**
		 * Login Result Handler
		 * @param event
		 */
		public function login_resultHandler( event:ResultEvent ):void
		{
			sessionManager.setLoginResponse( ServiceResult( event.result ).response as ServiceResponse );
		}
		
		[EventHandler( event = "SessionEvent.LOGOUT" )]
		/**
		 * Logout
		 * @param event
		 */
		public function logout( event:SessionEvent ):void
		{
			executeServiceCall( "SessionController.logout()", event, sessionServiceDelegate.logout( event ), logout_resultHandler, logout_faultHandler );
		}
		
		/**
		 * Logout Fault Handler
		 * @param event
		 */
		public function logout_faultHandler( event:FaultEvent ):void
		{
			sessionManager.logoutResponseHandler();
		}
		
		/**
		 * Logout Result Handler
		 * @param event
		 */
		public function logout_resultHandler( event:ResultEvent ):void
		{
			sessionManager.logoutResponseHandler();
		}
		
		[EventHandler( event = "SessionEvent.MOUSE_MOVE" )]
		/**
		 * Mouse Move Handler
		 * @param event
		 */
		public function mouseMoveHandler( event:SessionEvent ):void
		{
			sessionManager.mouseMoveHandler( event.mouseX, event.mouseY );
		}
	}
}
