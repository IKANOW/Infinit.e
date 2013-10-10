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
	
	public class MaximizedTileLayout extends LayoutBase
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
			var fullWidth:Number = unscaledWidth - ( p * 2 );
			var fullHeight:Number = unscaledHeight - ( p * 2 );
			
			for ( var i:int = 0; i < numElements; i++ )
			{
				var item:IWorkspaceVisualElement = useVirtualLayout ? target.getVirtualElementAt( i ) as IWorkspaceVisualElement : target.getElementAt( i ) as IWorkspaceVisualElement;
				var rect:Rectangle = new Rectangle( p, p, fullWidth, fullHeight );
				
				if ( item.maximized )
				{
					UIComponent( item ).visible = true;
					UIComponent( item ).enabled = true;
				}
				else
				{
					UIComponent( item ).visible = false;
					UIComponent( item ).enabled = false;
				}
				
				// Previously, maximizing a widgte could result in unwanted datatips from other
				// widgets appearing - this appears to fix that issue, and confirmed that tabbing through
				// the other widgets in the list is not an issue either
				if ( item.maximized )
				{
					item.setLayoutBoundsPosition( rect.x, rect.y );
					item.setLayoutBoundsSize( rect.width, rect.height );
				}
			}
		}
	}
}
