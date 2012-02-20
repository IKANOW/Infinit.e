package com.ikanow.infinit.e.shared.event
{
	import com.ikanow.infinit.e.shared.event.base.InfiniteEvent;
	import com.ikanow.infinit.e.shared.model.vo.ui.DialogControl;
	import flash.events.Event;
	
	public class UserEvent extends InfiniteEvent
	{
		
		//======================================
		// public static properties 
		//======================================
		
		public static const GET_USER:String = "getUserEvent";
		
		public static const GET_USER_ACTIVITY:String = "getUserActivityEvent";
		
		public static const GET_USER_COMMUNITIES:String = "getUserCommunitiesEvent";
		
		public static const GET_USER_SOURCES:String = "getUserSourcesEvent";
		
		public static const RESET:String = "resetUserEvent";
		
		
		//======================================
		// public properties 
		//======================================
		
		public var userId:String;
		
		public var userName:String;
		
		public var activityLimit:int;
		
		public var activitySkip:int;
		
		public var activityType:String;
		
		//======================================
		// constructor 
		//======================================
		
		public function UserEvent( type:String, bubbles:Boolean = true, cancelable:Boolean = false, dialogControl:DialogControl = null, userId:String = null, userName:String = null, activityType:String = null, activityLimit:int = 0, activitySkip:int = 0 )
		{
			super( type, bubbles, cancelable, dialogControl );
			this.userId = userId;
			this.userName = userName;
			this.activityType = activityType;
			this.activityLimit = activityLimit;
			this.activitySkip = activitySkip;
		}
		
		
		//======================================
		// public methods 
		//======================================
		
		override public function clone():Event
		{
			return new UserEvent( type, bubbles, cancelable, dialogControl, userId, userName, activityType, activityLimit, activitySkip );
		}
	}
}
