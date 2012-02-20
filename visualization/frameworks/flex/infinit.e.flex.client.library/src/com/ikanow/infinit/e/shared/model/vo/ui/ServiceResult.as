package com.ikanow.infinit.e.shared.model.vo.ui
{
	import flash.events.EventDispatcher;
	
	[Bindable]
	public class ServiceResult extends EventDispatcher
	{
		
		//======================================
		// public properties 
		//======================================
		
		public var response:ServiceResponse;
		
		public var stats:ServiceStatistics;
		
		public var data:Object;
		
		public var params:Object;
		
		public var rawData:Object;
		
		public var dataString:String;
	}
}
