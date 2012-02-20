package com.ikanow.infinit.e.shared.model.vo
{
	import flash.events.EventDispatcher;
	
	[Bindable]
	public class QueryTermEvent extends EventDispatcher
	{
		
		//======================================
		// public properties 
		//======================================
		
		public var entity1:QueryTerm;
		
		public var entity2:QueryTerm;
		
		public var verb:String;
		
		
		//======================================
		// public methods 
		//======================================
		
		public function clone():QueryTermEvent
		{
			var clone:QueryTermEvent = new QueryTermEvent();
			
			if ( entity1 )
				clone.entity1 = entity1.clone();
			
			if ( entity2 )
				clone.entity2 = entity2.clone();
			
			clone.verb = verb;
			
			return clone;
		}
	}
}
