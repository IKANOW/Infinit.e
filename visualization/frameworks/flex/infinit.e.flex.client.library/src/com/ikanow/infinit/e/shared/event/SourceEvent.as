package com.ikanow.infinit.e.shared.event
{
	import com.ikanow.infinit.e.shared.event.base.InfiniteEvent;
	import com.ikanow.infinit.e.shared.model.vo.ui.DialogControl;
	import flash.events.Event;
	import mx.collections.ArrayCollection;
	
	public class SourceEvent extends InfiniteEvent
	{
		
		//======================================
		// public static properties 
		//======================================
		
		public static const GET_SOURCES_GOOD:String = "getSourcesGoodEvent";
		
		public static const RESET:String = "resetSourcesEvent";
		
		
		//======================================
		// public properties 
		//======================================
		
		public var communityIDs:String;
		
		//======================================
		// constructor 
		//======================================
		
		public function SourceEvent( type:String, bubbles:Boolean = true, cancelable:Boolean = false, dialogControl:DialogControl = null, communityIDs:String = "" )
		{
			super( type, bubbles, cancelable );
			this.communityIDs = communityIDs;
		}
		
		
		//======================================
		// public methods 
		//======================================
		
		override public function clone():Event
		{
			return new SourceEvent( type, bubbles, cancelable, dialogControl, communityIDs );
		}
	}
}
