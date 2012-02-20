package com.ikanow.infinit.e.source.service
{
	import com.ikanow.infinit.e.shared.event.SourceEvent;
	import com.ikanow.infinit.e.shared.model.constant.ServiceConstants;
	import com.ikanow.infinit.e.shared.model.vo.Source;
	import com.ikanow.infinit.e.shared.model.vo.ui.ServiceResult;
	import com.ikanow.infinit.e.shared.service.base.InfiniteDelegate;
	import com.ikanow.infinit.e.shared.util.ObjectTranslatorUtil;
	import mx.collections.ArrayCollection;
	import mx.rpc.AsyncToken;
	import mx.rpc.http.HTTPService;
	
	public class SourceServiceDelegate extends InfiniteDelegate implements ISourceServiceDelegate
	{
		
		//======================================
		// public properties 
		//======================================
		
		[Inject( "sourceService" )]
		public var service:HTTPService;
		
		//======================================
		// constructor 
		//======================================
		
		public function SourceServiceDelegate()
		{
		}
		
		
		//======================================
		// public methods 
		//======================================
		
		/**
		 * Get Sources Good
		 * Retrieves the sources that are good
		 * @param event
		 * @return AsyncToken
		 */
		public function getSourcesGood( event:SourceEvent ):AsyncToken
		{
			var url:String = ServiceConstants.GET_SOURCES_GOOD_URL + event.communityIDs;
			var params:Object = { action: ServiceConstants.GET_SOURCES_GOOD_ACTION, dialogControl: event.dialogControl };
			var token:AsyncToken = makeCall( service, url, params, default_resultHandler, default_faultHandler );
			
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
				case ServiceConstants.GET_SOURCES_GOOD_ACTION:
					result.data = new ArrayCollection( ObjectTranslatorUtil.translateArrayObjects( serviceResult.data, Source ) );
					break;
			}
		}
	}
}
