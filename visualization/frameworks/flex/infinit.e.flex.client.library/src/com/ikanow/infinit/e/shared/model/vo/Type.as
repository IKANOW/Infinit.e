package com.ikanow.infinit.e.shared.model.vo
{
	import flash.events.EventDispatcher;
	
	[Bindable]
	public class Type extends EventDispatcher
	{
		
		//======================================
		// public properties 
		//======================================
		
		public var type:String;
		
		public var value:String;
		
		public var defaultValue:String;
		
		public var allowOverride:Boolean;
	}
}
