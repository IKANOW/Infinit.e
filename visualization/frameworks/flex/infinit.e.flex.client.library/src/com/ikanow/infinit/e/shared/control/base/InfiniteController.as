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
package com.ikanow.infinit.e.shared.control.base
{
	import com.ikanow.infinit.e.shared.event.base.IDialogControlEvent;
	import com.ikanow.infinit.e.shared.model.constant.DelegateConstants;
	import com.ikanow.infinit.e.shared.model.vo.ui.ServiceResult;
	
	import flash.events.IEventDispatcher;
	
	import mx.controls.Alert;
	import mx.logging.ILogger;
	import mx.logging.Log;
	import mx.resources.ResourceManager;
	import mx.rpc.AsyncToken;
	import mx.rpc.events.FaultEvent;
	
	import org.swizframework.utils.services.ServiceHelper;
	
	public class InfiniteController
	{
		
		//======================================
		// protected static properties 
		//======================================
		
		protected static const DIALOG_CONTROL:String = DelegateConstants.SERVICE_CALL_DIALOG_CONTROL;
		
		//======================================
		// private static properties 
		//======================================
		
		private static const logger:ILogger = Log.getLogger( "controller" );
		
		
		//======================================
		// public properties 
		//======================================
		
		[Dispatcher]
		public var dispatcher:IEventDispatcher;
		
		[Inject]
		public var serviceHelper:ServiceHelper;
		
		//======================================
		// constructor 
		//======================================
		
		public function InfiniteController()
		{
		}
		
		
		//======================================
		// protected methods 
		//======================================
		
		/**
		 * Default fault handler
		 * @param event
		 * @param args
		 */
		protected function defaultFaultHandler( event:FaultEvent, ... args ):void
		{
			var params:Array = new Array();
			if ( event != null && event.fault != null && event.fault.rootCause != null && event.fault.rootCause.text != null )
				params.push(event.fault.rootCause.text);
			Alert.show( ResourceManager.getInstance().getString( 'infinite', 'infiniteController.serverErrorMessage', params ), ResourceManager.getInstance().getString( 'infinite', 'infiniteController.serverError' ) );
		}
		
		/**
		 * Execute the service call
		 * @param serviceCallDescription
		 * @param call
		 * @param resultHandler
		 * @param faultHandler
		 * @param resultHandlerArgs
		 */
		protected function executeServiceCall( serviceCallDescription:String, event:IDialogControlEvent, call:AsyncToken, resultHandler:Function, faultHandler:Function = null, resultHandlerArgs:Array = null ):void
		{
			logger.info( "Executing " + serviceCallDescription + " service call" );
			event.dialogControl.description = serviceCallDescription;
			serviceHelper.executeServiceCall( call, resultHandler, faultHandler, resultHandlerArgs );
			call[ "serviceCallDescription" ] = serviceCallDescription;
		}
		
		protected function verifyServiceResponseSuccess( serviceCallDescription:String, result:ServiceResult ):Boolean
		{
			if ( result.response.responseSuccess )
			{
				return true;
			}
			else
			{
				var reason:String = result.response.message;
				if (reason.length > 1024) {
					reason = reason.substring(0, 1024) + '...';
				}
				Alert.show( ResourceManager.getInstance().getString( 'infinite', 'infiniteController.serviceCallUnsuccessfulMessage', [ serviceCallDescription ] ) + '   Reason: ' + reason, ResourceManager.getInstance().getString( 'infinite', 'infiniteController.serviceCallUnsuccessful' ) );
			}
			
			return false;
		}
	}
}

