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
package com.ikanow.infinit.e.shared.util
{
	import com.ikanow.infinit.e.shared.model.vo.Widget;
	import mx.collections.ArrayCollection;
	import mx.core.UIComponent;
	
	public class WidgetUtil
	{
		
		//======================================
		// constructor 
		//======================================
		
		/**
		 *
		 * Constructor
		 *
		 */
		public function WidgetUtil()
		{
		
		}
		
		
		//======================================
		// public static methods 
		//======================================
		
		/**
		 * Cleanup the widget positions after a widget is removed
		 */
		public static function clenupWidgetPositions( widgets:ArrayCollection, removedIndex:int ):void
		{
			for each ( var widget:Widget in widgets )
			{
				if ( widget.positionIndex > removedIndex )
				{
					widget.positionIndex--;
				}
			}
		}
		
		/**
		 * Moves a widget by updating the positionIndex property
		 */
		public static function moveWidget( widgets:ArrayCollection, targetWidget:Widget, newIndex:int ):void
		{
			var oldIndex:int = targetWidget.positionIndex;
			
			for each ( var widget:Widget in widgets )
			{
				if ( widget._id == targetWidget._id )
				{
					widget.positionIndex = newIndex;
				}
				else if ( widget.positionIndex > newIndex && widget.positionIndex < oldIndex )
				{
					widget.positionIndex++;
				}
				else if ( widget.positionIndex < newIndex && widget.positionIndex > oldIndex )
				{
					widget.positionIndex--;
				}
				else if ( widget.positionIndex == newIndex )
				{
					widget.positionIndex > oldIndex ? widget.positionIndex-- : widget.positionIndex++;
				}
			}
		}
	}
}
