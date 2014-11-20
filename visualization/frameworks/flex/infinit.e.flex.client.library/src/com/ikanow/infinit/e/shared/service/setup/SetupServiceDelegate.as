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
package com.ikanow.infinit.e.shared.service.setup
{
	import com.ikanow.infinit.e.shared.event.SetupEvent;
	import com.ikanow.infinit.e.shared.model.constant.ServiceConstants;
	import com.ikanow.infinit.e.shared.model.vo.Setup;
	import com.ikanow.infinit.e.shared.model.vo.Share;
	import com.ikanow.infinit.e.shared.model.vo.Widget;
	import com.ikanow.infinit.e.shared.model.vo.ui.ServiceResult;
	import com.ikanow.infinit.e.shared.service.base.InfiniteDelegate;
	import com.ikanow.infinit.e.shared.util.ObjectTranslatorUtil;
	import com.ikanow.infinit.e.shared.util.PasswordUtil;
	import com.ikanow.infinit.e.shared.util.ServiceUtil;
	import mx.collections.ArrayCollection;
	import mx.rpc.AsyncToken;
	import mx.rpc.http.HTTPService;
	
	public class SetupServiceDelegate extends InfiniteDelegate implements ISetupServiceDelegate
	{
		
		//======================================
		// public properties 
		//======================================
		
		[Inject( "setupService" )]
		public var service:HTTPService;
		
		//======================================
		// constructor 
		//======================================
		
		public function SetupServiceDelegate()
		{
		}
		
		
		//======================================
		// public methods 
		//======================================
		
		/**
		 * Get Modules All
		 * @param event
		 * @return AsyncToken
		 */
		public function getModulesAll( event:SetupEvent ):AsyncToken
		{
			var url:String = ServiceConstants.GET_MODULES_ALL_URL;
			var params:Object = { action: ServiceConstants.GET_MODULES_ALL_ACTION, dialogControl: event.dialogControl };
			var token:AsyncToken = makeCall( service, url, params, default_resultHandler, default_faultHandler );
			
			return token;
		}
		
		/**
		 * Get Modules User
		 * @param event
		 * @return AsyncToken
		 */
		public function getModulesUser( event:SetupEvent ):AsyncToken
		{
			var url:String = ServiceConstants.GET_MODULES_USER_URL;
			var params:Object = { action: ServiceConstants.GET_MODULES_USER_ACTION, dialogControl: event.dialogControl };
			var token:AsyncToken = makeCall( service, url, params, default_resultHandler, default_faultHandler );
			
			return token;
		}
		
		/**
		 * Get Setup
		 * @param event
		 * @return AsyncToken
		 */
		public function getSetup( event:SetupEvent ):AsyncToken
		{
			var url:String = ServiceConstants.GET_SETUP_URL;
			var params:Object = { action: ServiceConstants.GET_SETUP_ACTION, dialogControl: event.dialogControl };
			var token:AsyncToken = makeCall( service, url, params, default_resultHandler, default_faultHandler );
			
			return token;
		}
		
		public function getWidgetOptions( event:SetupEvent ):AsyncToken
		{
			var url:String = ServiceConstants.GET_WIDGET_OPTIONS_URL;
			var params:Object = { action: ServiceConstants.GET_WIDGET_OPTIONS_ACTION, dialogControl: event.dialogControl };
			var token:AsyncToken = makeCall( service, url, params, default_resultHandler, default_faultHandler );
			
			return token;
		}
		
		/**
		 * Set Modules User
		 * @param event
		 * @return AsyncToken
		 */
		public function setModulesUser( event:SetupEvent ):AsyncToken
		{
			var url:String = ServiceConstants.SET_MODULES_USER_URL + "$moduleids?moduleids=" + ServiceUtil.getStringOrNullString( event.userModules );
			var params:Object = { action: ServiceConstants.SET_MODULES_USER_ACTION, dialogControl: event.dialogControl };
			var token:AsyncToken = makeCall( service, url, params, default_resultHandler, default_faultHandler );
			
			return token;
		}
		
		/**
		 * Update Setup
		 * @param event
		 * @return AsyncToken
		 */
		public function updateSetup( event:SetupEvent ):AsyncToken
		{
			var url:String = ServiceConstants.UPDATE_SETUP_URL + event.urlParams;
			var params:Object = { action: ServiceConstants.UPDATE_SETUP_ACTION, dialogControl: event.dialogControl };
			var token:AsyncToken = makePostCall( service, url, params, default_resultHandler, default_faultHandler, event.queryString );
			
			return token;
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
				case ServiceConstants.GET_SETUP_ACTION:
					result.data = ObjectTranslatorUtil.translateObject( serviceResult.data, new Setup );
					break;
				case ServiceConstants.GET_MODULES_ALL_ACTION:
					result.data = new ArrayCollection( ObjectTranslatorUtil.translateArrayObjects( serviceResult.data, Widget ) );
					break;
				case ServiceConstants.GET_MODULES_USER_ACTION:
					result.data = new ArrayCollection( ObjectTranslatorUtil.translateArrayObjects( serviceResult.data, Widget ) );
					break;
				case ServiceConstants.GET_WIDGET_OPTIONS_ACTION:
					result.data = new ArrayCollection( ObjectTranslatorUtil.translateArrayObjects( serviceResult.data, Share ) );
					break;
			}
		}
	}
}
