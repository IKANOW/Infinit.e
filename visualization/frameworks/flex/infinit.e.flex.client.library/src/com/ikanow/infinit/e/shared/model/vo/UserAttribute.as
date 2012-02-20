package com.ikanow.infinit.e.shared.model.vo
{
	import flash.events.EventDispatcher;
	
	[Bindable]
	public class UserAttribute extends EventDispatcher
	{
		
		//======================================
		// public properties 
		//======================================
		
		public var publishLoginToActivityFeed:Type;
		
		public var publishCommentsToActivityFeed:Type;
		
		public var publishSharingToActivityFeed:Type;
		
		public var publishQueriesToActivityFeed:Type;
		
		public var publishCommentsPublicly:Type;
	}
}
