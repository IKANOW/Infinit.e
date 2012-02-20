package com.ikanow.infinit.e.shared.model.vo
{
	import com.ikanow.infinit.e.shared.model.vo.ui.ISelectable;
	import flash.events.EventDispatcher;
	
	[Bindable]
	public class WidgetSummary extends EventDispatcher implements ISelectable
	{
		
		//======================================
		// public properties 
		//======================================
		
		public var widgetDisplay:String;
		
		public var widgetTitle:String;
		
		public var widgetUrl:String;
		
		public var widgetImageUrl:String;
		
		public var widgetOptions:Object;
		
		public var widgetWidth:int;
		
		public var widgetHeight:int;
		
		public var widgetX:int;
		
		public var widgetY:int;
		
		[Transient]
		public var selected:Boolean;
	}
}
