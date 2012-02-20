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
