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
package actionscript
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
		
		[ArrayCollectionElementType( "Type" )]
		public var userAttributes:ArrayCollection;
		
		[ArrayCollectionElementType( "Type" )]
		public var contacts:ArrayCollection;
		
		[ArrayCollectionElementType( "Link" )]
		public var links:ArrayCollection;
	}
}
