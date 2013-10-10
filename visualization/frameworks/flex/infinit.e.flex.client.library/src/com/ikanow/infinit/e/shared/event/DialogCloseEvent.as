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
	import flash.events.Event;
	
	public class DialogCloseEvent extends Event
	{
		
		//======================================
		// public static properties 
		//======================================
		
		public static const DIALOG_CLOSE:String = "dialogClose";
		
		
		//======================================
		// public properties 
		//======================================
		
		/**
		 * The label of the associated button that triggered the
		 * DialogCloseEvent.
		 */
		public var label:String;
		
		/**
		 * Additional arguments to pass from an input dialog.
		 */
		public var additionalArguments:Array;
		
		//======================================
		// constructor 
		//======================================
		
		/**
		 * Constructor.
		 */
		public function DialogCloseEvent( type:String, label:String )
		{
			super( type, false, false );
			this.label = label;
		}
		
		
		//======================================
		// public methods 
		//======================================
		
		/**
		 * @inheritDoc
		 */
		override public function clone():Event
		{
			return new DialogCloseEvent( type, label );
		}
	}
}
