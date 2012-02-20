package com.ikanow.infinit.e.shared.model.vo
{
	import flash.events.EventDispatcher;
	
	import mx.collections.ArrayCollection;
	
	[Bindable]
	public class QueryInput extends EventDispatcher
	{
		
		//======================================
		// public properties 
		//======================================
		
		public var srcInclude:Boolean;
		
		[ArrayCollectionElementType( "String" )]
		public var sources:ArrayCollection;
	}
}
