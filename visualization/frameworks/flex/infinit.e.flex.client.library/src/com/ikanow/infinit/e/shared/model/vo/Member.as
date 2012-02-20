package com.ikanow.infinit.e.shared.model.vo
{
	import flash.events.EventDispatcher;
	import mx.collections.ArrayCollection;
	
	[Bindable]
	public class Member extends EventDispatcher
	{
		
		//======================================
		// public properties 
		//======================================
		
		public var _id:String;
		
		public var email:String;
		
		public var displayName:String;
		
		public var userType:String;
		
		public var userStatus:String;
		
		[ArrayCollectionElementType( "com.ikanow.infinit.e.shared.model.vo.Type" )]
		public var userAttributes:ArrayCollection;
		
		[ArrayCollectionElementType( "com.ikanow.infinit.e.shared.model.vo.Type" )]
		public var contacts:ArrayCollection;
		
		[ArrayCollectionElementType( "com.ikanow.infinit.e.shared.model.vo.Link" )]
		public var links:ArrayCollection;
	}
}
