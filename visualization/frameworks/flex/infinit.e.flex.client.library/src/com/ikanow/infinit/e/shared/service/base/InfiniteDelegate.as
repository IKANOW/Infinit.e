package com.ikanow.infinit.e.shared.service.base
{
	import com.ikanow.infinit.e.shared.event.SessionEvent;
	import com.ikanow.infinit.e.shared.model.constant.Constants;
	import com.ikanow.infinit.e.shared.model.constant.DelegateConstants;
	import com.ikanow.infinit.e.shared.model.constant.ServiceConstants;
	import com.ikanow.infinit.e.shared.model.vo.ui.DialogControl;
	import com.ikanow.infinit.e.shared.model.vo.ui.ServiceResponse;
	import com.ikanow.infinit.e.shared.model.vo.ui.ServiceResult;
	import com.ikanow.infinit.e.shared.model.vo.ui.ServiceStatistics;
	import com.ikanow.infinit.e.shared.service.base.Delegate;
	import com.ikanow.infinit.e.shared.util.JSONUtil;
	import com.ikanow.infinit.e.shared.util.ObjectTranslatorUtil;
	import com.ikanow.infinit.e.shared.view.component.dialog.DialogManager;
	import mx.core.Application;
	import mx.rpc.AsyncToken;
	import mx.rpc.Responder;
	import mx.rpc.events.FaultEvent;
	import mx.rpc.events.ResultEvent;
	import mx.rpc.http.HTTPService;
	
	public class InfiniteDelegate extends Delegate
	{
		
		//======================================
		// protected static properties 
		//======================================
		
		protected static const ACTION:String = DelegateConstants.SERVICE_CALL_ACTION;
		
		protected static const DATA:String = DelegateConstants.SERVICE_CALL_DATA;
		
		protected static const DIALOG_CONTROL:String = DelegateConstants.SERVICE_CALL_DIALOG_CONTROL;
		
		protected static const ID:String = DelegateConstants.SERVICE_CALL_ID;
		
		protected static const PARAMS:String = DelegateConstants.SERVICE_CALL_PARAMS;
		
		protected static const RESPONSE:String = DelegateConstants.SERVICE_CALL_RESPONSE;
		
		protected static const STATS:String = DelegateConstants.SERVICE_CALL_STATS;
		
		//======================================
		// constructor 
		//======================================
		
		/**
		 * Constructor.
		 */
		public function InfiniteDelegate()
		{
			super();
		}
		
		
		//======================================
		// protected methods 
		//======================================
		
		/**
		 * The default fault handler
		 * @param event
		 */
		protected function default_faultHandler( event:FaultEvent ):void
		{
			// Get the call token from the fault event
			var callToken:AsyncToken = event.token;
			
			// Get the delegate token from the dictionary then delete it from the dict
			var delegateToken:AsyncToken = tokenLookup[ callToken ] as AsyncToken;
			delete tokenLookup[ callToken ];
			
			// notify the external responders
			notifyResponders( createFaultEvent( event.fault, delegateToken ) );
			
			// dispatch the data ready event
			var dialogControl:DialogControl = callToken[ DIALOG_CONTROL ] as DialogControl;
			dialogControl.message = ServiceConstants.SERVICE_FAULT;
			dispatchDataReadyEvent( dialogControl );
		}
		
		/**
		 * The default result handler
		 * @param event
		 */
		protected function default_resultHandler( event:ResultEvent ):void
		{
			// Get the call token from the result event
			var callToken:AsyncToken = event.token;
			
			// Get the delegate token from the dictionary then delete it from the dict
			var delegateToken:AsyncToken = tokenLookup[ callToken ] as AsyncToken;
			delete tokenLookup[ callToken ];
			
			// deserialize json string to Object
			var serviceResult:Object = JSONUtil.decode( event.result as String );
			
			// translate the json object to actionscript object
			var result:Object = translateServiceResult( serviceResult, callToken );
			
			// Construct the result event to send to the external responders
			var resultEvent:ResultEvent = createResultEvent( result, delegateToken );
			
			// notify the external responders
			notifyResponders( resultEvent );
			
			// dispatch the data ready event
			dispatchDataReadyEvent( callToken[ DIALOG_CONTROL ] as DialogControl );
		}
		
		/**
		 * Dispatches a data loading event
		 * @param message
		 */
		protected function dispatchDataLoadingEvent( dialogControl:DialogControl ):void
		{
			dialogControl.startTime = new Date();
			
			var sessionEvent:SessionEvent = new SessionEvent( SessionEvent.DATA_LOADING );
			sessionEvent.dialogControl = dialogControl;
			dispatcher.dispatchEvent( sessionEvent );
		}
		
		/**
		 * Dispatches a data ready event
		 * @param message
		 */
		protected function dispatchDataReadyEvent( dialogControl:DialogControl ):void
		{
			dialogControl.endTime = new Date();
			
			var sessionEvent:SessionEvent = new SessionEvent( SessionEvent.DATA_READY );
			sessionEvent.dialogControl = dialogControl;
			dispatcher.dispatchEvent( sessionEvent );
		}
		
		/**
		 * Returns a random number
		 * @return Number
		 */
		protected function getRandomNumber():Number
		{
			return Math.round( Math.random() * 1000 );
		}
		
		/**
		 * Returns the service call action string from the service result
		 * @return String
		 */
		protected function getResponseAction( serviceResult:Object ):String
		{
			return String( serviceResult.response.action );
		}
		
		/**
		 * Executes a service call
		 * This method is overriden in subclasses
		 * @param service
		 * @param url
		 * @param params
		 * @param resultFunction
		 * @param faultFunction
		 * @return AsyncToken
		 */
		protected function makeCall( service:HTTPService, url:String, params:Object, resultFunction:Function, faultFunction:Function ):AsyncToken
		{
			var delegateToken:AsyncToken = new AsyncToken();
			
			if ( service == null || url == null || params == null || resultFunction == null || faultFunction == null )
				return delegateToken;
			
			service.url = url;
			
			var id:Number = getRandomNumber();
			var dialogControl:DialogControl = params[ DIALOG_CONTROL ] as DialogControl;
			dialogControl._id = id;
			
			var callToken:AsyncToken = service.send();
			callToken[ ID ] = id;
			callToken[ ACTION ] = params[ ACTION ];
			callToken[ DIALOG_CONTROL ] = dialogControl;
			callToken[ PARAMS ] = params;
			callToken.addResponder( new Responder( resultFunction, faultFunction ) );
			tokenLookup[ callToken ] = delegateToken;
			
			dispatchDataLoadingEvent( dialogControl );
			
			return delegateToken;
		}
		
		/**
		 * Executes a POST service call
		 * This method is overriden in subclasses
		 * @param service
		 * @param url
		 * @param params
		 * @param resultFunction
		 * @param faultFunction
		 * @return AsyncToken
		 */
		protected function makePostCall( service:HTTPService, url:String, params:Object, resultFunction:Function, faultFunction:Function, sendParam:Object ):AsyncToken
		{
			var delegateToken:AsyncToken = new AsyncToken();
			service.method = ServiceConstants.SERVICE_METHOD_POST;
			var header:Object = new Object();
			header[ ServiceConstants.SERVICE_HEADER_ACCEPT ] = ServiceConstants.SERVICE_CONTENT_TYPE;
			service.contentType = ServiceConstants.SERVICE_CONTENT_TYPE;
			service.headers = header;
			
			if ( service == null || url == null || params == null || resultFunction == null || faultFunction == null )
				return delegateToken;
			
			service.url = url;
			
			var id:Number = getRandomNumber();
			var dialogControl:DialogControl = params[ DIALOG_CONTROL ] as DialogControl;
			dialogControl._id = id;
			
			trace( url + "   -   " + JSONUtil.encode( sendParam ) );
			
			var callToken:AsyncToken = service.send( JSONUtil.encode( sendParam ) );
			callToken[ ID ] = id;
			callToken[ ACTION ] = params[ ACTION ];
			callToken[ DIALOG_CONTROL ] = dialogControl;
			callToken[ PARAMS ] = params;
			callToken.addResponder( new Responder( resultFunction, faultFunction ) );
			tokenLookup[ callToken ] = delegateToken;
			
			dispatchDataLoadingEvent( dialogControl );
			
			return delegateToken;
		}
		
		/**
		 * Translates the service result: response, data and params
		 * @param serviceResult
		 * @param token
		 * @return Object
		 */
		protected function translateServiceResult( serviceResult:Object, token:AsyncToken ):ServiceResult
		{
			var action:String = getResponseAction( serviceResult );
			var result:ServiceResult = new ServiceResult();
			
			// response
			if ( serviceResult.hasOwnProperty( RESPONSE ) )
			{
				translateServiceResultResponse( serviceResult, result );
			}
			
			// statistics
			if ( serviceResult.hasOwnProperty( STATS ) )
			{
				translateServiceResultStatistics( serviceResult, result );
			}
			
			// data
			if ( serviceResult.hasOwnProperty( DATA ) )
			{
				translateServiceResultData( serviceResult, result );
			}
			
			// params
			if ( token.hasOwnProperty( PARAMS ) )
			{
				result.params = token[ PARAMS ];
			}
			
			return result;
		}
		
		/**
		 * Translates the service result data
		 * This method is overridden in the subclasses to handle specific calls
		 * @param serviceResult
		 */
		protected function translateServiceResultData( serviceResult:Object, result:ServiceResult ):void
		{
		
		}
		
		/**
		 * Translates the service result response
		 * @param serviceResult
		 */
		protected function translateServiceResultResponse( serviceResult:Object, result:ServiceResult ):void
		{
			result.response = ObjectTranslatorUtil.translateObject( serviceResult.response, new ServiceResponse ) as ServiceResponse;
		}
		
		/**
		 * Translates the service result statistics
		 * @param serviceResult
		 */
		protected function translateServiceResultStatistics( serviceResult:Object, result:ServiceResult ):void
		{
			result.stats = ObjectTranslatorUtil.translateObject( serviceResult.stats, new ServiceStatistics ) as ServiceStatistics;
		}
	}
}
