package com.ikanow.infinit.e.widget.library.components.DocViewer
{
	import flash.events.Event;
	
	public class GoogleSearchEvent extends Event
	{
		public static var GOOGLE_SEARCH:String = "googleSearch";
		
		public var search_terms:Array;
		
		public function GoogleSearchEvent(type:String, search_terms:Array, bubbles:Boolean=false, cancelable:Boolean=false)
		{
			this.search_terms = search_terms;
			super(type, bubbles, cancelable);
		}
	}
}