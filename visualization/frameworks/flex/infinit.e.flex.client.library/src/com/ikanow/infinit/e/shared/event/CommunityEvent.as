package com.ikanow.infinit.e.shared.event
{
	import com.ikanow.infinit.e.shared.event.base.InfiniteEvent;
	import com.ikanow.infinit.e.shared.model.vo.Community;
	import com.ikanow.infinit.e.shared.model.vo.ui.DialogControl;
	import flash.events.Event;
	import mx.collections.ArrayCollection;
	
	public class CommunityEvent extends InfiniteEvent
	{
		
		//======================================
		// public static properties 
		//======================================
		
		public static const GET_COMMUNITIES_PUBLIC:String = "getCommunitiesPublicEvent";
		
		public static const SELECT_COMMUNITY:String = "selectCommunityEvent";
		
		public static const SELECT_ALL_COMMUNITIES:String = "selectAllCommunitiesEvent";
		
		public static const SELECT_NO_COMMUNITIES:String = "selectNoCommunitiesEvent";
		
		public static const JOIN_COMMUNITY:String = "joinCommunityEvent";
		
		public static const LEAVE_COMMUNITY:String = "leaveCommunityEvent";
		
		public static const RESET:String = "resetCommunitiesEvent";
		
		
		//======================================
		// public properties 
		//======================================
		
		public var communityID:String;
		
		public var community:Community;
		
		//======================================
		// constructor 
		//======================================
		
		public function CommunityEvent( type:String, bubbles:Boolean = true, cancelable:Boolean = false, dialogControl:DialogControl = null, communityID:String = "", community:Community = null )
		{
			super( type, bubbles, cancelable );
			this.communityID = communityID;
			this.community = community;
		}
		
		
		//======================================
		// public methods 
		//======================================
		
		override public function clone():Event
		{
			return new CommunityEvent( type, bubbles, cancelable, dialogControl, communityID, community );
		}
	}
}
