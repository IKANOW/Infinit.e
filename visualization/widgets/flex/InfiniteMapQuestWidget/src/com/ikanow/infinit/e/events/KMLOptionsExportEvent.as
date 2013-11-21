package com.ikanow.infinit.e.events
{
	import com.ikanow.infinit.e.actionscript.KMLOptions;
	
	import flash.events.Event;

	public class KMLOptionsExportEvent extends Event
	{	
		public static const EXPORT_EVENT:String = "exportEvent";
		
		public var options:KMLOptions;
		
		public function KMLOptionsExportEvent(type:String, options:KMLOptions, bubbles:Boolean = true, cancelable:Boolean = false)
		{
			super( type, bubbles, cancelable );
			this.options = options;
		}
		
		override public function clone():Event
		{
			return new KMLOptionsExportEvent( type, options, bubbles, cancelable);
		}
	}
}