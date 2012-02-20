package com.ikanow.infinit.e.shared.model.vo
{
	import flash.events.EventDispatcher;
	
	[Bindable]
	public class QueryTermOptions extends EventDispatcher
	{
		
		//======================================
		// public properties 
		//======================================
		
		public var expandAlias:Boolean;
		
		
		//======================================
		// public methods 
		//======================================
		
		//public var expandOntology:Boolean;
		
		public function clone():QueryTermOptions
		{
			var clone:QueryTermOptions = new QueryTermOptions();
			
			clone.expandAlias = expandAlias;
			
			return clone;
		}
	}
}
