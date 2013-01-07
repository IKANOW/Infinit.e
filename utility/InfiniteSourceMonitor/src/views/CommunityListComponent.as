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
package views
{
	import views.InfItemClickList;
	
	/**
	 *  Dispatched when a join community button is clicked
	 */
	[Event( name = "joinCommunity", type = "mx.events.ItemClickEvent" )]
	/**
	 *  Dispatched when a leave community button is clicked
	 */
	[Event( name = "leaveCommunity", type = "mx.events.ItemClickEvent" )]
	public class CommunityListComponent extends InfItemClickList
	{
		
		//======================================
		// constructor 
		//======================================
		
		public function CommunityListComponent()
		{
			super();
			
			this.focusEnabled = false;
		}
	}
}
