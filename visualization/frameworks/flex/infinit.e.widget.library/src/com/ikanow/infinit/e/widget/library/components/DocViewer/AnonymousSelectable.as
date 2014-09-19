package com.ikanow.infinit.e.widget.library.components.DocViewer
{

	public class AnonymousSelectable implements ISelectable 
	{
		public var item:Object;
		private var selectedVar:Boolean = false;
		
		public function AnonymousSelectable(item:Object)
		{
			this.item = item;
			this.selected = false;
		}
		
		[Bindable]
		public function get selected():Boolean
		{
			return selectedVar;
		}
		
		public function set selected(value:Boolean):void
		{
			this.selectedVar = value;
		}
	}
}