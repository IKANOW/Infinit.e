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
package com.ikanow.infinit.e.shared.view.component.dialog
{
	
	public class UserActionInputDialogMessage extends DialogMessage
	{
		
		//======================================
		// public properties 
		//======================================
		
		[ArrayElementType( "com.ikanow.infinit.e.shared.view.component.dialog.action.DialogInputUserAction" )]
		
		/**
		 * An array of DialogUserAction's related to a specific dialog.
		 */
		public var actions:Array;
		
		/**
		 * The layout of the input dialog
		 */
		public var layout:int;
		
		/**
		 * Maximum number of characters allowed.
		 */
		public var maxChars:int;
		
		/**
		 * Values to limit the input to.
		 */
		public var restrict:String;
		
		//======================================
		// constructor 
		//======================================
		
		public function UserActionInputDialogMessage()
		{
			super();
		}
	}
}
