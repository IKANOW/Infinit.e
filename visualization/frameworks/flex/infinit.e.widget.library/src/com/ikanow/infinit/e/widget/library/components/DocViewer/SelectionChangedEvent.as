package com.ikanow.infinit.e.widget.library.components.DocViewer
{
	import flash.events.Event;
	
	public class SelectionChangedEvent extends Event
	{
		public var previousSelections:Vector.<int>;
		public var currentSelections:Vector.<int>;
		public var indexChanged:int;
		public var indexAdded:Boolean;
		
		public function SelectionChangedEvent(type:String, previousSelections:Vector.<int>, currentSelections:Vector.<int>, indexChanged:int, indexAdded:Boolean )
		{
			super(type);
			this.previousSelections = previousSelections;
			this.currentSelections = currentSelections;
			this.indexChanged = indexChanged;
			this.indexAdded = indexAdded;
		}
	}
}