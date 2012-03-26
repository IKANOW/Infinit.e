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
	import flash.display.Bitmap;
	import flash.display.BitmapData;
	import flash.geom.Matrix;
	import mx.core.DragSource;
	import mx.core.IFlexDisplayObject;
	import mx.core.UIComponent;
	import mx.events.DragEvent;
	import mx.managers.DragManager;
	import spark.components.Image;
	import spark.components.List;
	
	public class InfDragImageList extends List
	{
		
		//======================================
		// constructor 
		//======================================
		
		public function InfDragImageList()
		{
			super();
		}
		
		
		//======================================
		// public methods 
		//======================================
		
		/**
		 *  Creates an instance of a class that is used to display the visuals
		 *  of the dragged items during a drag and drop operation.
		 *  The default <code>DragEvent.DRAG_START</code> handler passes the
		 *  instance to the <code>DragManager.doDrag()</code> method.
		 *
		 *  @return The IFlexDisplayObject representing the drag indicator.
		 *
		 *  @langversion 3.0
		 *  @playerversion Flash 10
		 *  @playerversion AIR 1.5
		 *  @productversion Flex 4
		 */
		override public function createDragIndicator():IFlexDisplayObject
		{
			var index:int = dataProvider.getItemIndex( selectedItem );
			var item:UIComponent = useVirtualLayout ? layout.target.getVirtualElementAt( index ) as UIComponent : layout.target.getElementAt( index ) as UIComponent;
			var dragIndicator:InfDragImageGroup = new InfDragImageGroup();
			dragIndicator.setStyle( "styleName", item.styleName );
			dragIndicator.height = item.height;
			dragIndicator.width = item.width;
			dragIndicator.move( item.x, item.y );
			var dragImage:Image = new Image();
			dragImage.height = item.height;
			dragImage.width = item.width;
			dragImage.source = new Bitmap( getUIComponentBitmapData( item ) );
			dragIndicator.contentGroup.addElement( dragImage );
			
			return dragIndicator;
		}
		
		//======================================
		// private methods 
		//======================================
		
		private function getUIComponentBitmapData( target:UIComponent ):BitmapData
		{
			var bd:BitmapData = new BitmapData( target.width, target.height );
			var m:Matrix = new Matrix();
			bd.draw( target, m );
			return bd;
		}
	}
}
