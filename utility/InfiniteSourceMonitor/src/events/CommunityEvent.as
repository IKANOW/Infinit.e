/*******************************************************************************
 * Copyright 2012, The Infinit.e Open Source Project.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package events
{
	import flash.events.Event;
	import mx.collections.ArrayCollection;
	import events.InfiniteEvent;
	import objects.Community;
	import views.DialogControl;
	
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
		
		public static const JOIN_COMMUNITY_SELECTED:String = "joinCommunitySelectedEvent";
		
		public static const JOIN_COMMUNITY_CANCELED:String = "joinCommunityCanceledEvent";
		
		public static const COMMUNITY_JOINED:String = "communityJoined";
		
		public static const LEAVE_COMMUNITY:String = "leaveCommunityEvent";
		
		public static const RESET:String = "resetCommunitiesEvent";
		
		
		//======================================
		// public properties 
		//======================================
		
		public var communityID:String;
		
		public var community:Community;
		
		public var selectedCommunities:ArrayCollection;
		
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
