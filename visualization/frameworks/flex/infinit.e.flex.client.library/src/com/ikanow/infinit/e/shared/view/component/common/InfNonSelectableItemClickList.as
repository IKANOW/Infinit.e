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
package com.ikanow.infinit.e.shared.view.component.common
{
	import flash.events.MouseEvent;
	import mx.core.mx_internal;
	import spark.components.List;
	
	use namespace mx_internal;
	
	/**
	 *  Dispatched when an item renderer is clicked
	 */
	[Event( name = "itemClick", type = "mx.events.ItemClickEvent" )]
	public class InfNonSelectableItemClickList extends List
	{
		
		//======================================
		// constructor 
		//======================================
		
		public function InfNonSelectableItemClickList()
		{
			super();
		}
		
		
		//======================================
		// internal methods 
		//======================================
		
		/**
		 * Override the setSelectedIndex() mx_internal method to not select an item
		 */
		mx_internal override function setSelectedIndex( value:int, dispatchChangeEvent:Boolean = false, changeCaret:Boolean = true ):void
		{
			// do nothing
		}
		
		/**
		 * Override the setSelectedIndex() mx_internal method to not select items
		 */
		mx_internal override function setSelectedIndices( value:Vector.<int>, dispatchChangeEvent:Boolean = false, changeCaret:Boolean = true ):void
		{
			// do nothing
		}
	}
}
