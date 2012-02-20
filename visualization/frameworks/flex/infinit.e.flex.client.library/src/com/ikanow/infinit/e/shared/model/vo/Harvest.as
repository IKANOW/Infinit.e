package com.ikanow.infinit.e.shared.model.vo
{
	import flash.events.EventDispatcher;
	
	[Bindable]
	public class Harvest extends EventDispatcher
	{
		
		//======================================
		// public properties 
		//======================================
		
		public var harvested:Date;
		
		public var harvest_status:String;
		
		public var harvest_message:String;
	}
}
