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
package com.ikanow.infinit.e.workspace.view.layout
{
	import flash.geom.Rectangle;
	import mx.core.IVisualElement;
	import mx.core.UIComponent;
	import spark.layouts.supportClasses.LayoutBase;
	
	public class CustomTileLayout extends LayoutBase
	{
		
		//======================================
		// public properties 
		//======================================
		
		public var padding:int;
		
		public var gap:int;
		
		//======================================
		// private properties 
		//======================================
		
		private var itemRects:Array = [];
		
		
		//======================================
		// public methods 
		//======================================
		
		override public function updateDisplayList( unscaledWidth:Number, unscaledHeight:Number ):void
		{
			super.updateDisplayList( unscaledWidth, unscaledHeight );
			
			itemRects = [];
			
			var numElements:int = target.numElements;
			var p:int = padding;
			
			// calculate all potential x, y, width and height values
			var col1x:int = p;
			var col1y:int = p;
			var col2x:int = ( unscaledWidth / 2 ) + ( gap / 2 );
			var col2y:int = ( unscaledHeight / 2 ) + ( gap / 2 );
			
			var fullWidth:Number = unscaledWidth - ( p * 2 );
			var fullHeight:Number = unscaledHeight - ( p * 2 );
			var halfWidth:Number = ( unscaledWidth / 2 ) - ( gap / 2 ) - p;
			var halfHeight:Number = ( unscaledHeight / 2 ) - ( gap / 2 ) - p;
			
			// build array of Rectangles to use for positioning and sizing list items
			switch ( numElements )
			{
				case 1:
					itemRects.push( new Rectangle( col1x, col1y, fullWidth, fullHeight ) );
					break;
				
				case 2:
					itemRects.push( new Rectangle( col1x, col1y, halfWidth, fullHeight ) );
					itemRects.push( new Rectangle( col2x, col1y, halfWidth, fullHeight ) );
					break;
				
				case 3:
					itemRects.push( new Rectangle( col1x, col1y, halfWidth, fullHeight ) );
					itemRects.push( new Rectangle( col2x, col1y, halfWidth, halfHeight ) );
					itemRects.push( new Rectangle( col2x, col2y, halfWidth, halfHeight ) );
					break;
				
				case 4:
					itemRects.push( new Rectangle( col1x, col1y, halfWidth, halfHeight ) );
					itemRects.push( new Rectangle( col2x, col1y, halfWidth, halfHeight ) );
					itemRects.push( new Rectangle( col1x, col2y, halfWidth, halfHeight ) );
					itemRects.push( new Rectangle( col2x, col2y, halfWidth, halfHeight ) );
					break;
			}
			
			// apply Rectangle properties to list items
			for ( var i:int = 0; i < numElements; i++ )
			{
				var item:IWorkspaceVisualElement = useVirtualLayout ? target.getVirtualElementAt( i ) as IWorkspaceVisualElement : target.getElementAt( i ) as IWorkspaceVisualElement;
				var rect:Rectangle = itemRects[ item.positionIndex ];
				
				if ( rect )
				{
					item.setLayoutBoundsPosition( rect.x, rect.y );
					item.setLayoutBoundsSize( rect.width, rect.height );
				}
				
				UIComponent( item ).visible = true;
				UIComponent( item ).enabled = true;
			}
		}
	}
}
