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
package com.ikanow.infinit.e.shared.util
{
	import mx.core.UIComponent;
	
	public class StateUtil
	{
		
		//======================================
		// constructor 
		//======================================
		
		/**
		 *
		 * Constructor
		 *
		 */
		public function StateUtil()
		{
		
		}
		
		
		//======================================
		// public static methods 
		//======================================
		
		/**
		 * Overrides the state names for a UIComponent
		 *
		 * @return
		 *
		 */
		public static function setStates( component:UIComponent, states:Array, currentState:String = "" ):void
		{
			if ( component.states.length != states.length )
			{
				throw new Error( "Incorrect Number of State Parameters" );
				return;
			}
			
			for ( var i:int = 0; i < states.length; ++i )
			{
				component.states[ i ].name = states[ i ];
			}
			
			component.currentState = currentState;
		}
	}
}
