package com.ikanow.infinit.e.source.control
{
	import com.ikanow.infinit.e.shared.control.base.InfiniteController;
	import com.ikanow.infinit.e.shared.event.SourceEvent;
	import com.ikanow.infinit.e.shared.model.vo.ui.ServiceResult;
	import com.ikanow.infinit.e.source.model.manager.SourceManager;
	import com.ikanow.infinit.e.source.service.ISourceServiceDelegate;
	import mx.collections.ArrayCollection;
	import mx.rpc.events.ResultEvent;
	
	/**
	 * Source Controller
	 */
	public class SourceController extends InfiniteController
	{
		
		//======================================
		// public properties 
		//======================================
		
		[Inject]
		public var sourceServiceDelegate:ISourceServiceDelegate
		
		[Inject]
		public var sourceManager:SourceManager;
		
		
		//======================================
		// public methods 
		//======================================
		
		[EventHandler( event = "SourceEvent.GET_SOURCES_GOOD" )]
		/**
		 * Get Sources Good
		 * @param event
		 */
		public function getSourcesGood( event:SourceEvent ):void
		{
			executeServiceCall( "SourceController.getSourcesGood()", event, sourceServiceDelegate.getSourcesGood( event ), getSourcesGood_resultHandler, defaultFaultHandler );
		}
		
		/**
		 * Get Sources Good Result Handler
		 * @param event
		 */
		public function getSourcesGood_resultHandler( event:ResultEvent ):void
		{
			if ( verifyServiceResponseSuccess( "getSourcesGood()", event.result as ServiceResult ) )
				sourceManager.setSources( ServiceResult( event.result ).data as ArrayCollection );
		}
		
		[EventHandler( event = "SourceEvent.RESET" )]
		/**
		 * Reset Sources
		 * @param event
		 */
		public function resetSources( event:SourceEvent ):void
		{
			sourceManager.reset();
		}
	}
}
