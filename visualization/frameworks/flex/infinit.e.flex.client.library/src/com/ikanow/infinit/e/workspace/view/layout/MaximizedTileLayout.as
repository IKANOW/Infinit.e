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
				
				item.setLayoutBoundsPosition( rect.x, rect.y );
				item.setLayoutBoundsSize( rect.width, rect.height );
				
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
			}
		}
	}
}
