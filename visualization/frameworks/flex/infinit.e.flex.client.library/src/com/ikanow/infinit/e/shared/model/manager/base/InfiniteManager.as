package com.ikanow.infinit.e.shared.model.manager.base
{
	import flash.events.EventDispatcher;
	import flash.events.IEventDispatcher;
	
	public class InfiniteManager extends EventDispatcher
	{
		//======================================
		// public properties 
		//======================================
		
		[Dispatcher]
		public var dispatcher:IEventDispatcher;
		
		//======================================
		// constructor 
		//======================================
		
		public function InfiniteManager()
		{
			super();
		}
	}
}

