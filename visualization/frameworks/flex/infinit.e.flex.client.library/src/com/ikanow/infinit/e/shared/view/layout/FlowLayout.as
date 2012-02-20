package com.ikanow.infinit.e.shared.view.layout
{
	import com.ikanow.infinit.e.query.view.builder.QueryTermSkinnableDataContainer;
	import flash.geom.Rectangle;
	import mx.core.ILayoutElement;
	import mx.core.IVisualElement;
	import spark.components.supportClasses.GroupBase;
	import spark.layouts.supportClasses.DropLocation;
	import spark.layouts.supportClasses.LayoutBase;
	
	public class FlowLayout extends LayoutBase
	{
		
		//======================================
		// public properties 
		//======================================
		
		private var _border:Number = 10;
		
		public function set border( val:Number ):void
		{
			_border = val;
			var layoutTarget:GroupBase = target;
			
			if ( layoutTarget )
			{
				layoutTarget.invalidateDisplayList();
			}
		}
		
		private var _gap:Number = 10;
		
		public function set gap( val:Number ):void
		{
			_gap = val;
			var layoutTarget:GroupBase = target;
			
			if ( layoutTarget )
			{
				layoutTarget.invalidateDisplayList();
			}
		}
		
		private var _rowHeight:Number = 50;
		
		public function set rowHeight( val:Number ):void
		{
			if ( _rowHeight != val )
			{
				_rowHeight = val;
				var layoutTarget:GroupBase = target;
				
				if ( layoutTarget )
				{
					measure();
					layoutTarget.invalidateDisplayList();
				}
			}
		}
		
		private var _rowCount:Number = 1;
		
		public function set rowCount( val:Number ):void
		{
			if ( _rowCount != val )
			{
				_rowCount = val;
				var layoutTarget:GroupBase = target;
				
				if ( layoutTarget )
				{
					measure();
					layoutTarget.invalidateSize();
				}
			}
		}
		
		[Bindable]
		public var addButtonX:int;
		
		[Bindable]
		public var addButtonY:int;
		
		
		//======================================
		// public methods 
		//======================================
		
		override public function measure():void
		{
			var layoutTarget:GroupBase = target;
			layoutTarget.measuredHeight = ( _rowCount * ( _rowHeight + _gap ) ) + _border;
			layoutTarget.measuredWidth = Math.ceil( layoutTarget.measuredWidth );
		}
		
		override public function updateDisplayList( containerWidth:Number, containerHeight:Number ):void
		{
			var x:Number = _border;
			var y:Number = _border;
			var elementY:Number = _border;
			var maxWidth:Number = 0;
			var maxHeight:Number = 0;
			
			//loop through all the elements
			var layoutTarget:GroupBase = target;
			var count:int = layoutTarget.numElements;
			
			var rowCountNew:int = 1;
			
			for ( var i:int = 0; i < count; i++ )
			{
				var element:ILayoutElement = useVirtualLayout ?
					layoutTarget.getVirtualElementAt( i ) :
					layoutTarget.getElementAt( i );
				
				//resize the element to its preferred size by passing in NaN
				element.setLayoutBoundsSize( NaN, NaN );
				
				//get element's size, but AFTER it has been resized to its preferred size.
				var elementWidth:Number = element.getLayoutBoundsWidth();
				var elementHeight:Number = element.getLayoutBoundsHeight();
				
				//does the element fit on this line, or should we move to the next line?
				if ( x + elementWidth > containerWidth )
				{
					//start from the left side
					x = _border;
					
					// move to the next line, and add the gap, but not if it's the first line
					if ( _rowHeight != 0 )
						y += _rowHeight + _gap;
					else
						y += elementHeight + _gap;
					
					if ( rowCountNew > 0 )
					{
						y += _gap;
					}
					
					rowCountNew++;
				}
				
				//position the element
				if ( _rowHeight != 0 )
				{
					elementY = y + ( _rowHeight / 2 ) - ( elementHeight / 2 );
					element.setLayoutBoundsPosition( x, elementY );
				}
				else
				{
					element.setLayoutBoundsPosition( x, y );
				}
				
				//update max dimensions (needed for scrolling)
				maxWidth = Math.max( maxWidth, x + elementWidth );
				maxHeight = Math.max( maxHeight, y + elementHeight );
				
				//update the current pos, and add the gap
				x += elementWidth + _gap;
			}
			
			// position the add button
			if ( count > 0 )
			{
				element = layoutTarget.getElementAt( count - 1 );
				addButtonX = element.getLayoutBoundsX() + element.getLayoutBoundsWidth() + _gap;
				addButtonY = element.getLayoutBoundsY() + ( element.getLayoutBoundsHeight() / 2 ) - 9;
				
				if ( addButtonX > containerWidth - 30 )
				{
					addButtonX = _border + 4;
					addButtonY = addButtonY + _rowHeight + _gap + 9;
				}
			}
			else
			{
				addButtonX = _border + 4;
				addButtonY = 26;
			}
			
			//set final content size (needed for scrolling)
			layoutTarget.setContentSize( maxWidth + _border, ( _rowHeight * rowCountNew ) + _border );
			
			rowCount = rowCountNew;
		}
		
		//======================================
		// protected methods 
		//======================================
		
		/**
		 *  @private
		 */
		override protected function calculateDropIndex( x:Number, y:Number ):int
		{
			// Iterate over the visible elements
			var layoutTarget:GroupBase = target;
			var count:int = layoutTarget.numElements;
			
			// If there are no items, insert at index 0
			if ( count == 0 )
				return 0;
			
			// Go through the visible elements
			var minDistance:Number = Number.MAX_VALUE;
			var bestIndex:int = -1;
			var start:int = 0;
			var end:int = count - 1;
			
			for ( var i:int = start; i <= end; i++ )
			{
				var elementBounds:Rectangle = this.getElementBounds( i );
				
				if ( !elementBounds )
					continue;
				
				if ( elementBounds.left <= x && x <= elementBounds.right && elementBounds.top <= y && y <= elementBounds.bottom )
				{
					var centerX:Number = elementBounds.x + elementBounds.width / 2;
					return ( x < centerX ) ? i : i + 1;
				}
				
				var curDistance:Number = Math.min( Math.abs( x - elementBounds.left ), Math.abs( x - elementBounds.right ) );
				
				if ( curDistance < minDistance )
				{
					minDistance = curDistance;
					bestIndex = ( x < elementBounds.left ) ? i : i + 1;
				}
			}
			
			// If there are no visible elements, either pick to drop at the beginning or at the end
			if ( bestIndex == -1 )
				bestIndex = getElementBounds( 0 ).x < x ? count : 0;
			
			return bestIndex;
		}
		
		/**
		 *  @private
		 */
		override protected function calculateDropIndicatorBounds( dropLocation:DropLocation ):Rectangle
		{
			var dropIndex:int = dropLocation.dropIndex;
			var count:int = target.numElements;
			
			var emptySpace:Number = ( 0 < _border ) ? _border : 0;
			var emptySpaceLeft:Number = 10;
			
			if ( target.numElements > 0 )
			{
				emptySpaceLeft = ( dropIndex < count ) ? getElementBounds( dropIndex ).left - emptySpace :
					getElementBounds( dropIndex - 1 ).right + _border - emptySpace;
			}
			
			// Calculate the size of the bounds, take minium and maximum into account
			var width:Number = emptySpace;
			var height:Number = _rowHeight;
			
			if ( dropIndicator is IVisualElement )
			{
				var element:IVisualElement = IVisualElement( dropIndicator );
				width = Math.max( Math.min( width, element.getMaxBoundsWidth( false ) ), element.getMinBoundsWidth( false ) );
			}
			
			var x:Number = emptySpaceLeft + Math.round( ( emptySpace - width ) / 2 );
			x = Math.max( -Math.ceil( width / 2 ), Math.min( target.contentWidth - Math.ceil( width / 2 ), x ) );
			
			var row:int = getRow( getElementBounds( dropIndex > ( count - 1 ) ? count - 1 : dropIndex ).top );
			var y:Number = row * ( _rowHeight + _gap + _gap ) + _border - 1;
			
			if ( row == 0 )
				y = _border;
			
			return new Rectangle( x, y, width, _rowHeight );
		}
		
		/**
		 *  @private
		 */
		protected function getRow( y:int ):int
		{
			var row:int;
			// Iterate over the visible elements
			var layoutTarget:GroupBase = target;
			var count:int = layoutTarget.numElements;
			
			// If there are no items, return 0
			if ( count == 0 )
				return 0;
			
			var currentRowY:int = 0;
			var nextRowY:int = _border + _rowHeight + _gap;
			
			for ( var i:int = 0; i <= _rowCount; i++ )
			{
				if ( y > currentRowY && y < nextRowY )
				{
					return row;
				}
				
				row++;
				currentRowY += _rowHeight + _gap;
				nextRowY += _rowHeight + _gap;;
			}
			
			return row;
		}
	}
}
