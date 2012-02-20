package com.ikanow.infinit.e.source.service
{
	import com.ikanow.infinit.e.shared.event.SourceEvent;
	import mx.rpc.AsyncToken;
	
	public interface ISourceServiceDelegate
	{
		/**
		 * Get Sources Good
		 * Retrieves the sources that are good
		 * @param event
		 * @return AsyncToken
		 */
		function getSourcesGood( event:SourceEvent ):AsyncToken;
	}
}
