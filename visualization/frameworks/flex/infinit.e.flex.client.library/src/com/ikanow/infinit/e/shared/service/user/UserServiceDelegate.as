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
	import com.ikanow.infinit.e.shared.model.constant.ServiceConstants;
	import com.ikanow.infinit.e.shared.model.vo.User;
	import com.ikanow.infinit.e.shared.model.vo.ui.ServiceResult;
	import com.ikanow.infinit.e.shared.service.base.InfiniteDelegate;
	import com.ikanow.infinit.e.shared.util.ObjectTranslatorUtil;
	import mx.rpc.AsyncToken;
	import mx.rpc.http.HTTPService;
	
	public class UserServiceDelegate extends InfiniteDelegate implements IUserServiceDelegate
	{
		
		//======================================
		// public properties 
		//======================================
		
		[Inject( "userService" )]
		public var service:HTTPService;
		
		//======================================
		// constructor 
		//======================================
		
		public function UserServiceDelegate()
		{
		}
		
		
		//======================================
		// public methods 
		//======================================
		
		/**
		 * Get User
		 * @param event
		 * @return AsyncToken
		 */
		public function getUser( event:UserEvent ):AsyncToken
		{
			var url:String = ServiceConstants.GET_USER_URL;
			var params:Object = { action: ServiceConstants.GET_USER_ACTION, dialogControl: event.dialogControl, userName: event.userName };
			var token:AsyncToken = makeCall( service, url, params, default_resultHandler, default_faultHandler );
			
			return token;
		}
		
		/**
		 * Get User Activity
		 * @param event
		 * @return AsyncToken
		 */
		public function getUserActivity( event:UserEvent ):AsyncToken
		{
			return null;
		}
		
		/**
		 * Get User Profile
		 * @param event
		 * @return AsyncToken
		 */
		public function getUserProfile( event:UserEvent ):AsyncToken
		{
			return null;
		}
		
		/**
		 * Get User Sources
		 * @param event
		 * @return AsyncToken
		 */
		public function getUserSources( event:UserEvent ):AsyncToken
		{
			return null;
		}
		
		//======================================
		// protected methods 
		//======================================
		
		/**
		 * Called from translateResult method in super() if the result has data
		 * @param serviceResult - the raw object returned from the service
		 * @param result - the generated service result object
		 * @return Object
		 */
		override protected function translateServiceResultData( serviceResult:Object, result:ServiceResult ):void
		{
			var action:String = getResponseAction( serviceResult );
			
			switch ( action )
			{
				case ServiceConstants.GET_USER_ACTION:
					result.data = ObjectTranslatorUtil.translateObject( serviceResult.data, new User );
					break;
			}
		}
	}
}
