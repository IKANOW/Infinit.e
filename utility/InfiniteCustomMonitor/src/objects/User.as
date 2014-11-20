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
		
		[ArrayCollectionElementType( "objects.Community" )]
		public var communities:ArrayCollection;
		
		public var WPUsedID:String;
		
		public var SubscriptionID:String;
		
		public var SubscriptionTypeID:String;
		
		public var SubscriptionStartDate:Date;
	}
}
