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
package objects
{
	import flash.events.EventDispatcher;
	import mx.collections.ArrayCollection;
	import util.ISelectable;
	
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
		
		[ArrayCollectionElementType( "objects.CommunityAttribute" )]
		public var communityAttributes:ArrayCollection;
		
		[ArrayCollectionElementType( "objects.UserAttribute" )]
		public var userAttributes:ArrayCollection;
		
		public var ownerId:String;
		
		public var communityStatus:String;
		
		public var ownerDisplayName:String;
		
		public var numberOfMembers:int;
		
		[ArrayCollectionElementType( "objects.Member" )]
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
