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
package com.ikanow.infinit.e.workspace.view.content
{
	import com.ikanow.infinit.e.shared.view.component.common.InfDragImageList;
	
	/**
	 *  Dispatched when a maximize button is clicked
	 */
	[Event( name = "maximizeWidget", type = "mx.events.ItemClickEvent" )]
	/**
	 *  Dispatched when a minimize button is clicked
	 */
	[Event( name = "minimizeWidgets", type = "mx.events.ItemClickEvent" )]
	public class WorkspaceContentList extends InfDragImageList
	{
		
		//======================================
		// constructor 
		//======================================
		
		public function WorkspaceContentList()
		{
			super();
		}
	}
}
