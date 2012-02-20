package com.ikanow.infinit.e.shared.model.vo
{
	import com.ikanow.infinit.e.shared.model.constant.types.QueryStringTypes;
	
	import flash.events.EventDispatcher;
	
	[Bindable]
	public class TypedQueryString extends EventDispatcher
	{
		
		//======================================
		// public properties 
		//======================================
		
		public var queryDate:Date = new Date();
		
		public var type:String = QueryStringTypes.QUERY;
		
		public var queryString:QueryString;
		
		public function get time():Number
		{
			return queryDate.time;
		}
	}
}
