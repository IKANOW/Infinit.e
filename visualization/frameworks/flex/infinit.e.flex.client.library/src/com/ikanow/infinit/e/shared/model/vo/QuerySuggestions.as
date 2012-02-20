package com.ikanow.infinit.e.shared.model.vo
{
	import flash.events.EventDispatcher;
	import mx.collections.ArrayCollection;
	
	[Bindable]
	public class QuerySuggestions extends EventDispatcher
	{
		
		//======================================
		// public properties 
		//======================================
		
		[ArrayCollectionElementType( "String" )]
		public var currentSuggestions:ArrayCollection;
		
		[ArrayCollectionElementType( "com.ikanow.infinit.e.shared.model.vo.QuerySuggestion" )]
		public var dimensions:ArrayCollection;
	}
}
