package com.ikanow.infinit.e.history.model.manager
{
	import com.ikanow.infinit.e.shared.model.manager.base.InfiniteManager;
	import mx.collections.ArrayCollection;
	
	/**
	 * History Manager
	 */
	public class HistoryManager extends InfiniteManager
	{
		
		//======================================
		// public properties 
		//======================================
		
		[Bindable]
		/**
		 * The collection of recent queries
		 */
		public var recentQueries:ArrayCollection;
		
		
		//======================================
		// public methods 
		//======================================
		
		[Inject( "queryManager.recentQueries", bind = "true" )]
		/**
		 * set the collection of recent queries
		 */
		public function setRecentQueries( value:ArrayCollection ):void
		{
			recentQueries = value;
		}
	}
}
