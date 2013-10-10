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
package com.ikanow.infinit.e.shared.model.vo.ui
{
	import flash.events.EventDispatcher;
	import mx.collections.ArrayCollection;
	
	[Bindable]
	public class ColumnSelectorItem extends EventDispatcher implements ISelectable
	{
		
		//======================================
		// public properties 
		//======================================
		
		public var type:String;
		
		public var _id:String;
		
		public var description:String;
		
		public var count:int;
		
		public var selected:Boolean;
		
		//======================================
		// constructor 
		//======================================
		
		public function ColumnSelectorItem( type:String, _id:String, description:String, count:int = 0 )
		{
			super();
			this.type = type;
			this._id = _id;
			this.description = description;
			this.count = count;
		}
	}
}
