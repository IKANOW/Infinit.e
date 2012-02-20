package com.ikanow.infinit.e.shared.model.vo
{
	import com.ikanow.infinit.e.shared.model.vo.ui.ISelectable;
	import flash.events.EventDispatcher;
	import mx.collections.ArrayCollection;
	
	[Bindable]
	public class Source extends EventDispatcher implements ISelectable
	{
		
		//======================================
		// public properties 
		//======================================
		
		public var _id:String;
		
		public var key:String;
		
		public var created:Date;
		
		public var modified:Date;
		
		public var url:String;
		
		public var title:String;
		
		public var description:String;
		
		public var mediaType:String;
		
		public var extractType:String;
		
		// (Beta- uses "groupID", V0+ uses "communityId" will support both for an interim period)
		[ArrayCollectionElementType( "String" )]
		public var groupID:ArrayCollection;		
		[ArrayCollectionElementType( "String" )]
		public var communityIds:ArrayCollection;
		
		public var isPublic:Boolean;
		
		[ArrayCollectionElementType( "String" )]
		public var tags:ArrayCollection;
		
		public var harvest:Harvest;
		
		public var isApproved:Boolean;
		
		[Transient]
		public var community:String;
		
		[Transient]
		public var selected:Boolean;
		
		[Transient]
		public function get tagsString():String
		{
			var string:String = "";
			
			if ( tags )
			{
				for each ( var tag:String in tags )
				{
					tag = tag.toLowerCase();
					
					string += tag.toString();
					string += ", ";
				}
			}
			else
			{
				return string;
			}
			
			return string.substr( 0, string.length - 2 );
		}
	}
}
