package com.ikanow.infinit.e.events
{	
	import com.ikanow.infinit.e.renderers.KMLLayersDropDownItemRenderer;
	
	import flash.events.Event;
	
	public class DropDownClickEvent extends Event
	{	
		public static const CLICK_EVENT:String = "click_event";
		public var item:KMLLayersDropDownItemRenderer;
		
		public function DropDownClickEvent(item:KMLLayersDropDownItemRenderer, type:String, bubbles:Boolean = true, cancelable:Boolean = false)
		{
			super( type, bubbles, cancelable );
			this.item = item;
		}
		
		override public function clone():Event
		{
			return new DropDownClickEvent( item, type, bubbles, cancelable);
		}
	}
}