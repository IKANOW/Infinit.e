package com.ikanow.infinit.e.widget.library.components.DocViewer
{
	import com.ikanow.infinit.e.widget.library.enums.FilterDataSetEnum;
	
	import flash.events.Event;
	
	import system.data.Map;
	import system.data.Set;
	
	public class FilterEvent extends Event
	{
		public var filterDataSetEnum:FilterDataSetEnum;
		public var docSet:Set;
		public var docToEntMap:Map;
		public var docFilterField:String;
		public var docToEntField:String;
		public var desc:String;
		
		public function FilterEvent(type:String, filterDataSetEnum:FilterDataSetEnum, docSet:Set, docToEntMap:Map, docFilterField:String, docToEntField:String, desc:String )
		{
			super(type,true);
			this.filterDataSetEnum = filterDataSetEnum;
			this.docSet = docSet;
			this.docToEntMap = docToEntMap;
			this.docFilterField = docFilterField;
			this.docToEntField = docToEntField;
			this.desc = desc;
		}
	}
}