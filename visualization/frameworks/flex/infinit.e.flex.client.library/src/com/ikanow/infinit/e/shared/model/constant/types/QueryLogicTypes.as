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
	import com.ikanow.infinit.e.shared.model.constant.QueryConstants;
	import mx.logging.Log;
	import assets.EmbeddedAssets;
	
	/**
	 * Query Logic Type Constants
	 */
	public class QueryLogicTypes
	{
		
		//======================================
		// public static properties 
		//======================================
		
		public static const GROUP_START:String = "groupStart";
		
		public static const GROUP_END:String = "groupEnd";
		
		public static const OPERATOR:String = "operator";
		
		public static const PLACE_HOLDER:String = "placeHolder";
		
		public static const BLANK_OR_SPACE:String = "blankOrSpace";
		
		
		//======================================
		// public static methods 
		//======================================
		
		/**
		 * function to get the query logic type
		 *
		 * @param item
		 *
		 * @return The type of logic
		 */
		public static function getType( item:String ):String
		{
			switch ( item )
			{
				case QueryConstants.BLANK:
					return BLANK_OR_SPACE;
					break;
				case QueryConstants.SPACE:
					return BLANK_OR_SPACE;
					break;
				case QueryConstants.PARENTHESIS_LEFT:
					return GROUP_START;
					break;
				case QueryConstants.PARENTHESIS_RIGHT:
					return GROUP_END;
					break;
				case QueryOperatorTypes.AND:
					return OPERATOR;
					break;
				case QueryOperatorTypes.OR:
					return OPERATOR;
					break;
				case QueryOperatorTypes.NOT:
					return OPERATOR;
					break;
				default:
					return PLACE_HOLDER;
					break;
			}
		}
	}
}

