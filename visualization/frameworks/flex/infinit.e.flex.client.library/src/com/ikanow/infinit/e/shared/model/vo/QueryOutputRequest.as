package com.ikanow.infinit.e.shared.model.vo
{
	import com.ikanow.infinit.e.shared.model.constant.QueryConstants;
	import flash.events.EventDispatcher;
	
	[Bindable]
	public class QueryOutputRequest extends EventDispatcher
	{
		
		//======================================
		// public properties 
		//======================================
		
		public var aggregation:Object;
		
		public var docs:QueryOutputDocumentOptions;
		
		public var format:String;
		
		//======================================
		// constructor 
		//======================================
		
		public function QueryOutputRequest()
		{
			this.format = QueryConstants.OUTPUT_FORMAT;
		}
	}
}
