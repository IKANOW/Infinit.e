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
	import com.ikanow.infinit.e.shared.model.constant.ServiceConstants;
	import com.ikanow.infinit.e.shared.service.base.InfiniteDelegate;
	import com.ikanow.infinit.e.shared.util.PasswordUtil;
	import com.ikanow.infinit.e.shared.util.ServiceUtil;
	import mx.rpc.AsyncToken;
	import mx.rpc.http.HTTPService;
	
	public class SessionServiceDelegate extends InfiniteDelegate implements ISessionServiceDelegate
	{
		
		//======================================
		// public properties 
		//======================================
		
		[Inject( "sessionService" )]
		public var service:HTTPService;
		
		//======================================
		// constructor 
		//======================================
		
		public function SessionServiceDelegate()
		{
		}
		
		
		//======================================
		// public methods 
		//======================================
		
		/**
		 * Get Cookie
		 * @param event
		 * @return AsyncToken
		 */
		public function getCookie( event:SessionEvent ):AsyncToken
		{
			var url:String = ServiceConstants.GET_COOKIE_URL;
			var params:Object = { action: ServiceConstants.GET_COOKIE_ACTION, dialogControl: event.dialogControl };
			
			return makeCall( service, url, params, default_resultHandler, default_faultHandler );
		}
		
		/**
		 * Keep Alive
		 * @param event
		 * @return AsyncToken
		 */
		public function keepAlive( event:SessionEvent ):AsyncToken
		{
			var url:String = ServiceConstants.KEEP_ALIVE_URL;
			var params:Object = { action: ServiceConstants.KEEP_ALIVE_ACTION, dialogControl: event.dialogControl };
			
			return makeCall( service, url, params, default_resultHandler, default_faultHandler );
		}
		
		/**
		 * Forgot Password
		 * 
		 */
		public function forgotPassword( event:SessionEvent ):AsyncToken
		{
			var url:String = ServiceConstants.FORGOT_PASSWORD_URL + ServiceUtil.urlEncode( event.username );
			var params:Object = { action: ServiceConstants.FORGOT_PASSWORD_ACTION, dialogControl: event.dialogControl };
			
			return makeCall( service, url, params, default_resultHandler, default_faultHandler );
		}
		
		/**
		 * Login
		 * @param event
		 * @return AsyncToken
		 */
		public function login( event:SessionEvent ):AsyncToken
		{
			var hashPassword:String = PasswordUtil.hashPassword( event.password );
			var url:String = ServiceConstants.LOGIN_URL + ServiceUtil.urlEncode( event.username ) + "/" + ServiceUtil.urlEncode( hashPassword );
			var params:Object = { action: ServiceConstants.LOGIN_ACTION, dialogControl: event.dialogControl, username: event.username, password: event.password };
			var token:AsyncToken = makeCall( service, url, params, default_resultHandler, default_faultHandler );
			
			return token;
		}
		
		/**
		 * Log Out
		 * @param event
		 * @return AsyncToken
		 */
		public function logout( event:SessionEvent ):AsyncToken
		{
			var url:String = ServiceConstants.LOGOUT_URL;
			var params:Object = { action: ServiceConstants.LOGOUT_ACTION, dialogControl: event.dialogControl };
			
			return makeCall( service, url, params, default_resultHandler, default_faultHandler );
		}
	}
}
