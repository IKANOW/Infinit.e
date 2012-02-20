package com.ikanow.infinit.e.shared.view.component.common
{
	
	/**
	 *  Dispatched when the user chooses to select all check boxes
	 */
	[Event( name = "selectAll", type = "flash.events.Event" )]
	/**
	 *  Dispatched when the user chooses to unselect all check boxes
	 */
	[Event( name = "selectNone", type = "flash.events.Event" )]
	/**
	 *  Dispatched when the user clicks a check box
	 */
	[Event( name = "selectItem", type = "mx.events.ItemClickEvent" )]
	/**
	 * A data gris that handles a check box column; used for selection
	 */
	public class InfSelectableDataGrid extends InfDataGrid
	{
		
		//======================================
		// constructor 
		//======================================
		
		public function InfSelectableDataGrid()
		{
			super();
		}
	}
}
