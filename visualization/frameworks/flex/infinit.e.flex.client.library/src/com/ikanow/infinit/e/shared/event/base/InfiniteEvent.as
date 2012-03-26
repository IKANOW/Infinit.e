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
package com.ikanow.infinit.e.shared.event.base
{
	import com.ikanow.infinit.e.shared.model.vo.ui.DialogControl;
	import flash.events.Event;
	
	public class InfiniteEvent extends Event implements IDialogControlEvent
	{
		
		//======================================
		// public properties 
		//======================================
		
		private var _dialogControl:DialogControl = new DialogControl();
		
		public function get dialogControl():DialogControl
		{
			return _dialogControl;
		}
		
		public function set dialogControl( value:DialogControl ):void
		{
			_dialogControl = value;
		}
		
		//======================================
		// constructor 
		//======================================
		
		public function InfiniteEvent( type:String, bubbles:Boolean = true, cancelable:Boolean = false, dialogControl:DialogControl = null )
		{
			super( type, bubbles, cancelable );
			
			if ( dialogControl )
				this.dialogControl = dialogControl;
		}
		
		
		//======================================
		// public methods 
		//======================================
		
		override public function clone():Event
		{
			return new InfiniteEvent( type, bubbles, cancelable, dialogControl );
		}
	}
}
