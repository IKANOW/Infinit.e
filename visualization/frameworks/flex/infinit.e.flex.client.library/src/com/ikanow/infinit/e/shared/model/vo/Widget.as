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
package com.ikanow.infinit.e.shared.model.vo
{
	import com.ikanow.infinit.e.shared.model.vo.ui.ISelectable;
	import flash.events.EventDispatcher;
	import mx.collections.ArrayCollection;
	
	[Bindable]
	public class Widget extends EventDispatcher implements ISelectable
	{
		
		//======================================
		// public properties 
		//======================================
		
		public var _id:String;
		
		public var swf:String;
		
		public var url:String;
		
		public var title:String;
		
		public var description:String;
		
		public var created:Date;
		
		public var modified:Date;
		
		public var version:String;
		
		public var author:String;
		
		public var imageurl:String;
		
		public var approved:Boolean;
		
		[ArrayCollectionElementType( "String" )]
		public var searchterms:ArrayCollection;
		
		public var debug:Boolean;
		
		[Transient]
		public var selected:Boolean;
		
		[Transient]
		public var favorite:Boolean;
		
		[Transient]
		public var sortOrder:int = 1;
		
		[Transient]
		public var positionIndex:int;
		
		[Transient]
		public var parentCollectionCount:int;
		
		[Transient]
		public var maximized:Boolean;
		
		[Transient]
		public var isBeingDragged:Boolean = false;
		
		[Transient]
		public var isBeingReplaced:Boolean = false;
		
		
		//======================================
		// public methods 
		//======================================
		
		public function clone():Widget
		{
			var clone:Widget = new Widget();
			
			clone._id = _id;
			clone.swf = swf;
			clone.url = url;
			clone.title = title;
			clone.description = description;
			clone.created = created;
			clone.modified = modified;
			clone.version = version;
			clone.author = author;
			clone.imageurl = imageurl;
			clone.approved = approved;
			clone.searchterms = searchterms;
			clone.debug = debug;
			clone.selected = selected;
			clone.favorite = favorite;
			clone.sortOrder = sortOrder;
			clone.positionIndex = positionIndex;
			clone.maximized = maximized;
			clone.isBeingDragged = isBeingDragged;
			clone.isBeingReplaced = isBeingReplaced;
			
			return clone;
		}
	}
}
