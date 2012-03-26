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
package com.ikanow.infinit.e.shared.event
{
	import com.ikanow.infinit.e.shared.model.constant.WidgetConstants;
	import com.ikanow.infinit.e.shared.model.vo.Widget;
	import flash.events.Event;
	
	public class WorkspaceEvent extends Event
	{
		
		//======================================
		// public static properties 
		//======================================
		
		public static const ADD_WIDGET:String = "addWidgetEvent";
		
		public static const MOVE_WIDGET:String = "moveWidgetEvent";
		
		public static const REMOVE_WIDGET:String = "removeWidgetEvent";
		
		public static const RESET:String = "resetWorkspacesEvent";
		
		public static const MAXIMIZE_WIDGET:String = "maximizeWidgetEvent";
		
		public static const MINIMIZE_WIDGETS:String = "minimizesWidgetEvent";
		
		
		//======================================
		// public properties 
		//======================================
		
		public var widget:Widget;
		
		public var index:int;
		
		//======================================
		// constructor 
		//======================================
		
		public function WorkspaceEvent( type:String, widget:Widget = null, index:int = WidgetConstants.USE_NEXT_AVAILABLE_INDEX )
		{
			super( type, true, false );
			
			this.widget = widget;
			this.index = index;
		}
		
		
		//======================================
		// public methods 
		//======================================
		
		override public function clone():Event
		{
			return new WorkspaceEvent( type, widget, index );
		}
	}
}
