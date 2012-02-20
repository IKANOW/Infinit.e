package com.ikanow.infinit.e.shared.model.vo.ui
{
	import flash.events.EventDispatcher;
	
	[Bindable]
	public class ServiceStatistics extends EventDispatcher
	{
		
		//======================================
		// public properties 
		//======================================
		
		public var found:int;
		
		public var start:int;
		
		public var maxScore:Number;
		
		public var avgScore:Number;
		
		[Transient]
		public var reset:Boolean = true;
	}
}
