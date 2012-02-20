package com.ikanow.infinit.e.shared.model.vo.ui
{
	import flash.events.EventDispatcher;
	
	[Bindable]
	public class ExportFormat extends EventDispatcher
	{
		
		//======================================
		// public properties 
		//======================================
		
		public var label:String;
		
		public var format:String;
		
		//======================================
		// constructor 
		//======================================
		
		public function ExportFormat( label:String, format:String )
		{
			super();
			this.label = label;
			this.format = format;
		}
	}
}
