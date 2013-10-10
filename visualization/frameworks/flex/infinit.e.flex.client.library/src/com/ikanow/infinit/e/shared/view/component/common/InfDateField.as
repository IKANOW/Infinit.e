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
	import mx.controls.DateField;
	import mx.core.mx_internal;
	
	public class InfDateField extends DateField
	{
		
		//======================================
		// constructor 
		//======================================
		
		public function InfDateField()
		{
			super();
		}
		
		
		//======================================
		// protected methods 
		//======================================
		
		/**
		 *  @private
		 *  Create subobjects in the component.
		 */
		override protected function createChildren():void
		{
			super.createChildren();
			
			mx_internal::downArrowButton.buttonMode = true;
		}
	}
}
