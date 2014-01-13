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
	import com.ikanow.infinit.e.shared.event.base.InfiniteEvent;
	import com.ikanow.infinit.e.shared.model.vo.ui.DialogControl;
	import flash.events.Event;
	
	public class SessionEvent extends InfiniteEvent
	{
		
		//======================================
		// public static properties 
		//======================================
		
		public static const GET_COOKIE:String = "getCookieEvent";
		
		public static const FORGOT_PASSWORD:String = "forgotPassword";
		
		public static const LOGIN:String = "loginEvent";
		
		public static const LOGOUT:String = "logoutEvent";
		
		public static const DATA_LOADING:String = "dataLoadingEvent";
		
		public static const DATA_READY:String = "dataReadyEvent";
		
		public static const KEEP_ALIVE:String = "keepAliveEvent";
		
		public static const MOUSE_MOVE:String = "mouseMoveEvent";
		
		
		//======================================
		// public properties 
		//======================================
		
		public var password:String;
		
		public var username:String;
		
		public var mouseX:Number;
		
		public var mouseY:Number;
		
		//======================================
		// constructor 
		//======================================
		
		public function SessionEvent( type:String, bubbles:Boolean = true, cancelable:Boolean = false, dialogControl:DialogControl = null, username:String = null, password:String = null, mouseX:Number = 0, mouseY:Number = 0 )
		{
			super( type, bubbles, cancelable, dialogControl );
			this.username = username;
			this.password = password;
			this.mouseX = mouseX;
			this.mouseY = mouseY;
		}
		
		
		//======================================
		// public methods 
		//======================================
		
		override public function clone():Event
		{
			return new SessionEvent( type, bubbles, cancelable, dialogControl, username, password, mouseX, mouseY );
		}
	}
}
