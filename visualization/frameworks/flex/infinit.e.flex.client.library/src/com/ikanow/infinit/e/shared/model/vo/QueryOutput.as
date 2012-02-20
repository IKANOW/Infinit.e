package com.ikanow.infinit.e.shared.model.vo
{
	import com.ikanow.infinit.e.shared.model.constant.QueryConstants;
	import flash.events.EventDispatcher;
	
	[Bindable]
	public class QueryOutput extends EventDispatcher
	{
		
		//======================================
		// public properties 
		//======================================
		
		public var aggregation:QueryOutputAggregationOptions;
		
		public var docs:QueryOutputDocumentOptions;
		
		public var format:String;
		
		//======================================
		// constructor 
		//======================================
		
		public function QueryOutput()
		{
			this.format = QueryConstants.OUTPUT_FORMAT;
		}
	}
}
