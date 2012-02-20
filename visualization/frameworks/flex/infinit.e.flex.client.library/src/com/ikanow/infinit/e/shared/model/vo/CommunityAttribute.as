package com.ikanow.infinit.e.shared.model.vo
{
	import flash.events.EventDispatcher;
	
	[Bindable]
	public class CommunityAttribute extends EventDispatcher
	{
		
		//======================================
		// public properties 
		//======================================
		
		public var isPublic:Type;
		
		public var usersCanSelfRegister:Type;
		
		public var registrationRequiresApproval:Type;
		
		public var usersCanCreateSubCommunities:Type;
	}
}
