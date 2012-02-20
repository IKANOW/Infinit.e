package com.ikanow.infinit.e.shared.model.vo
{
	import com.ikanow.infinit.e.shared.model.vo.ui.ISelectable;
	import flash.events.EventDispatcher;
	import mx.collections.ArrayCollection;
	
	[Bindable]
	public class Community extends EventDispatcher implements ISelectable
	{
		
		//======================================
		// public properties 
		//======================================
		
		public var _id:String;
		
		public var created:Date;
		
		public var modified:Date;
		
		public var name:String;
		
		public var description:String;
		
		public var isSystemCommunity:Boolean;
		
		[ArrayCollectionElementType( "String" )]
		public var tags:ArrayCollection;
		
		[ArrayCollectionElementType( "com.ikanow.infinit.e.shared.model.vo.CommunityAttribute" )]
		public var communityAttributes:ArrayCollection;
		
		[ArrayCollectionElementType( "com.ikanow.infinit.e.shared.model.vo.UserAttribute" )]
		public var userAttributes:ArrayCollection;
		
		public var ownerId:String;
		
		public var communityStatus:String;
		
		public var ownerDisplayName:String;
		
		public var numberOfMembers:int;
		
		[ArrayCollectionElementType( "com.ikanow.infinit.e.shared.model.vo.Member" )]
		public var members:ArrayCollection;
		
		[Transient]
		public var selected:Boolean;
		
		[Transient]
		public var isUserMember:Boolean;
		
		[Transient]
		public var isUserOwner:Boolean;
		
		[Transient]
		public var sortOrder:int;
	}
}
