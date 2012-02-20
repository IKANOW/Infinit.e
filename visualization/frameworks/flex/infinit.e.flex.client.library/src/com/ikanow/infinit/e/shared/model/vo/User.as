package com.ikanow.infinit.e.shared.model.vo
{
	import flash.events.EventDispatcher;
	import mx.collections.ArrayCollection;
	
	[Bindable]
	public class User extends EventDispatcher
	{
		
		//======================================
		// public properties 
		//======================================
		
		public var _id:String;
		
		public var created:Date;
		
		public var modified:Date;
		
		public var accountStatus:String;
		
		public var email:String;
		
		public var firstName:String;
		
		public var lastName:String;
		
		public var displayName:String;
		
		public var phone:String;
		
		public var title:String;
		
		public var organization:String;
		
		public var avatar:String;
		
		[ArrayCollectionElementType( "com.ikanow.infinit.e.shared.model.vo.Community" )]
		public var communities:ArrayCollection;
		
		public var WPUsedID:String;
		
		public var SubscriptionID:String;
		
		public var SubscriptionTypeID:String;
		
		public var SubscriptionStartDate:Date;
	}
}
