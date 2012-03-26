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
