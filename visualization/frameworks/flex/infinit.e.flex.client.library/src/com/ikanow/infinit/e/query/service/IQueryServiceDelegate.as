package com.ikanow.infinit.e.query.service
{
	import com.ikanow.infinit.e.shared.event.QueryEvent;
	import mx.rpc.AsyncToken;
	
	public interface IQueryServiceDelegate
	{
		/**
		 * Get Query Event Suggestions
		 * This will return event suggestions for a keyword string
		 * @param event
		 * @return AsyncToken
		 */
		function getQueryEventSuggestions( event:QueryEvent ):AsyncToken;
		
		/**
		 * Get Query Suggestions
		 * This will return suggestions for a keyword string
		 * @param event
		 * @return AsyncToken
		 */
		function getQuerySuggestions( event:QueryEvent ):AsyncToken;
		
		/**
		 * Query
		 * @param event
		 * @return AsyncToken
		 */
		function query( event:QueryEvent ):AsyncToken
	}
}

