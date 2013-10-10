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
package com.ikanow.infinit.e.shared.view.component.dialog.action
{
	
	public class DialogInputUserAction extends DialogUserAction
	{
		
		//======================================
		// public properties 
		//======================================
		
		/**
		 * Flags whether or not the input is required for the selected action.
		 */
		public var inputRequired:Boolean;
		
		//======================================
		// constructor 
		//======================================
		
		public function DialogInputUserAction( name:String = null, callback:Function = null, scope:* = null, inputRequired:Boolean = false, args:Array = null )
		{
			super( name, callback, scope, args );
			this.inputRequired = inputRequired;
		}
	}
}
