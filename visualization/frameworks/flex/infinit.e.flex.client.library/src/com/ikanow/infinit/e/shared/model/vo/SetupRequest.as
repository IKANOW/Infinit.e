package com.ikanow.infinit.e.shared.model.vo
{
	import flash.events.EventDispatcher;
	
	[Bindable]
	public class SetupRequest extends EventDispatcher
	{
		
		//======================================
		// public properties 
		//======================================
		
		public var communityIds:String;
		
		public var openModules:String;
		
		public var queryString:QueryStringRequest;
	}
}
