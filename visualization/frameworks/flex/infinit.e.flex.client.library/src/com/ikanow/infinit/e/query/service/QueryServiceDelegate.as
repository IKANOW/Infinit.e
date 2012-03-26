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
package com.ikanow.infinit.e.query.service
{
	import com.ikanow.infinit.e.shared.event.QueryEvent;
	import com.ikanow.infinit.e.shared.model.constant.Constants;
	import com.ikanow.infinit.e.shared.model.constant.ServiceConstants;
	import com.ikanow.infinit.e.shared.model.vo.QuerySuggestions;
	import com.ikanow.infinit.e.shared.model.vo.ui.DialogControl;
	import com.ikanow.infinit.e.shared.model.vo.ui.ServiceResult;
	import com.ikanow.infinit.e.shared.service.base.InfiniteDelegate;
	import com.ikanow.infinit.e.shared.util.JSONUtil;
	import com.ikanow.infinit.e.shared.util.ObjectTranslatorUtil;
	import com.ikanow.infinit.e.shared.util.QueryUtil;
	import com.ikanow.infinit.e.shared.util.ServiceUtil;
	import flash.net.Responder;
	import flash.utils.setTimeout;
	import mx.collections.ArrayCollection;
	import mx.rpc.AsyncToken;
	import mx.rpc.events.ResultEvent;
	import mx.rpc.http.HTTPService;
	
	public class QueryServiceDelegate extends InfiniteDelegate implements IQueryServiceDelegate
	{
		
		//======================================
		// public properties 
		//======================================
		
		[Inject( "queryService" )]
		public var service:HTTPService;
		
		[Inject( "querySuggestService" )]
		public var querySuggestService:HTTPService;
		
		//======================================
		// constructor 
		//======================================
		
		public function QueryServiceDelegate()
		{
		}
		
		
		//======================================
		// public methods 
		//======================================
		
		/**
		 * Get Query Event Suggestions
		 * @param event
		 * @return AsyncToken
		 */
		public function getQueryEventSuggestions( event:QueryEvent ):AsyncToken
		{
			var keywordString:String = event.keywordString;
			var url:String = ServiceConstants.GET_QUERY_ASSOC_SUGGESTIONS_URL + keywordString + Constants.FORWARD_SLASH + event.communityids;
			var params:Object = { action: ServiceConstants.GET_QUERY_ASSOC_SUGGESTIONS_ACTION, dialogControl: event.dialogControl, keywordString: event.keywordString, communityids: event.communityids };
			var token:AsyncToken = makeCall( querySuggestService, url, params, default_resultHandler, default_faultHandler );
			
			return token;
		}
		
		/**
		 * Get Query Suggestions
		 * @param event
		 * @return AsyncToken
		 */
		public function getQuerySuggestions( event:QueryEvent ):AsyncToken
		{
			var keywordString:String = ServiceUtil.replaceMultipleWhitespace( event.keywordString ).toLowerCase();
			var url:String = ServiceConstants.GET_QUERY_SUGGESTIONS_URL + ServiceUtil.urlEncode( keywordString ) + Constants.FORWARD_SLASH + event.communityids;
			var params:Object = { action: ServiceConstants.GET_QUERY_SUGGESTIONS_ACTION, dialogControl: event.dialogControl, keywordString: event.keywordString, communityids: event.communityids };
			var token:AsyncToken = makeCall( querySuggestService, url, params, default_resultHandler, default_faultHandler );
			
			return token;
		}
		
		/**
		 * Query
		 * @param event
		 * @return AsyncToken
		 */
		public function query( event:QueryEvent ):AsyncToken
		{
			var url:String = ServiceConstants.QUERY_URL + event.communityids;
			var params:Object = { action: ServiceConstants.QUERY_ACTION, dialogControl: event.dialogControl, communityids: event.communityids, queryString: event.queryString };
			var token:AsyncToken = makePostCall( service, url, params, query_resultHandler, default_faultHandler, event.queryString );
			
			return token;
		}
		
		//======================================
		// protected methods 
		//======================================
		
		/**
		 * The default result handler
		 * @param event
		 */
		protected function query_resultHandler( event:ResultEvent ):void
		{
			// Get the call token from the result event
			var callToken:AsyncToken = event.token;
			
			// dispatch the data ready event
			dispatchDataReadyEvent( callToken[ DIALOG_CONTROL ] as DialogControl );
			
			// translate the data and notify responder
			setTimeout( translateQueryResults, 50, event );
		}
		
		/**
		 * The default result handler
		 * @param event
		 */
		protected function translateQueryResults( event:ResultEvent ):void
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
			result.dataString = event.result as String;
			
			// Construct the result event to send to the external responders
			var resultEvent:ResultEvent = createResultEvent( result, delegateToken );
			
			// notify the external responders
			notifyResponders( resultEvent );
		}
		
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
				case ServiceConstants.QUERY_ACTION:
					result.data = serviceResult.data;
					result.rawData = serviceResult;
					break;
				case ServiceConstants.GET_QUERY_SUGGESTIONS_ACTION:
					result.data = ObjectTranslatorUtil.translateObject( serviceResult.data, new QuerySuggestions );
					break;
				case ServiceConstants.GET_QUERY_ASSOC_SUGGESTIONS_ACTION:
					var eventSuggestions:ArrayCollection = new ArrayCollection( ObjectTranslatorUtil.translateArrayObjects( serviceResult.data, String ) );
					result.data = QueryUtil.getQuerySuggestionsFromStrings( eventSuggestions );
					break;
			}
		}
	}
}
