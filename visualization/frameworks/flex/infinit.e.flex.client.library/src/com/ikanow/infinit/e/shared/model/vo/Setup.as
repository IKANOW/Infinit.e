package com.ikanow.infinit.e.shared.model.vo
{
	import flash.events.EventDispatcher;
	import mx.collections.ArrayCollection;
	
	[Bindable]
	public class Setup extends EventDispatcher
	{
		
		//======================================
		// public properties 
		//======================================
		
		public var communityIds:Array;
		
		public var profileId:String;
		
		[ArrayCollectionElementType( "com.ikanow.infinit.e.shared.model.vo.WidgetSummary" )]
		public var openModules:ArrayCollection;
		
		public var queryString:QueryString;
	}
}
