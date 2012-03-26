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
	public class ColumnSelector extends EventDispatcher implements ISelectable
	{
		
		//======================================
		// public properties 
		//======================================
		
		public var _id:String;
		
		public var dataField:String;
		
		public var description:String;
		
		public var selected:Boolean;
		
		public var dataProvider:ArrayCollection = new ArrayCollection();
		
		public var dataProviderEnabled:Boolean = true;
		
		public var filterType:String;
		
		//======================================
		// constructor 
		//======================================
		
		public function ColumnSelector( _id:String, dataField:String, description:String, filterType:String, selected:Boolean )
		{
			super();
			this._id = _id;
			this.dataField = dataField;
			this.description = description;
			this.filterType = filterType;
			this.selected = selected;
		}
	}
}
