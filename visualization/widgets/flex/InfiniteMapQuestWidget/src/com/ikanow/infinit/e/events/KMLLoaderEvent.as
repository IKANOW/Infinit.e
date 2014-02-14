package com.ikanow.infinit.e.events
{	
	import flash.events.Event;
	
	public class KMLLoaderEvent extends Event
	{	
		public static const LOADER_EVENT:String = "loader_event";
		public var name:String;
		public var url:String;
		public var community_id:String;
		
		public function KMLLoaderEvent(name:String, url:String, community_id:String, type:String, bubbles:Boolean = true, cancelable:Boolean = false)
		{
			super( type, bubbles, cancelable );
			this.name = name;
			this.url = url;
			this.community_id = community_id;
		}
		
		override public function clone():Event
		{
			return new KMLLoaderEvent( name, url, community_id, type, bubbles, cancelable);
		}
	}
}