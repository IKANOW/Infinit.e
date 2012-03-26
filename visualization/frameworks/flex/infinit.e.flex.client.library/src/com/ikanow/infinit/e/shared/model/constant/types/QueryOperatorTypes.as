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
package com.ikanow.infinit.e.shared.model.constant.types
{
	import mx.collections.ArrayCollection;
	
	/**
	 * Query Operator Type Constants
	 */
	public class QueryOperatorTypes
	{
		
		//======================================
		// public static properties 
		//======================================
		
		public static const AND:String = "AND";
		
		public static const OR:String = "OR";
		
		public static const NOT:String = "NOT";
		
		public static const AND_NOT:String = "AND NOT";
		
		/**
		 * returns a collection fo the operator types
		 * @return The collection of operator types
		 */
		public static function get types():ArrayCollection
		{
			return new ArrayCollection( [ AND, OR, NOT ] );
		}
	}
}

