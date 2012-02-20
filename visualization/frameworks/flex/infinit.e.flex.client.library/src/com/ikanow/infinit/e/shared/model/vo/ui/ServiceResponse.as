package com.ikanow.infinit.e.shared.model.vo.ui
{
	import flash.events.EventDispatcher;
	
	[Bindable]
	public class ServiceResponse extends EventDispatcher
	{
		
		//======================================
		// public properties 
		//======================================
		
		public var action:String;
		
		public var success:String;
		
		public var message:String;
		
		public var time:Number;
		
		[Transient]
		public function get responseSuccess():Boolean
		{
			return success == "true" ? true : false;
		}
	}
}
