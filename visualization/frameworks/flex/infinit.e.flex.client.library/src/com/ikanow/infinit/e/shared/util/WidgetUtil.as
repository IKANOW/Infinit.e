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
