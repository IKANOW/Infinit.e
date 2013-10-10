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
	import assets.EmbeddedAssets;
	
	/**
	 * Query Term Type Constants
	 */
	public class QueryTermTypes
	{
		
		//======================================
		// public static properties 
		//======================================
		
		public static const EXACT_TEXT:String = "etext";
		
		public static const FREE_TEXT:String = "ftext";
		
		public static const ENTITY:String = "entity";
		
		public static const EVENT:String = "event";
		
		public static const TEMPORAL:String = "time";
		
		public static const GEO_LOCATION:String = "geo";
		
		public static const SENTIMENT:String = "sentiment";
		
		
		//======================================
		// public static methods 
		//======================================
		
		/**
		 * function to get an icon based on the query term type
		 *
		 * @param type The type of the query term
		 *
		 * @return The icon of the type
		 */
		public static function getIcon( type:String ):Class
		{
			type = type.toLowerCase();
			
			switch ( type )
			{
				case EXACT_TEXT:
					return EmbeddedAssets.ENTITY_TEXT_EXACT;
					break;
				case FREE_TEXT:
					return EmbeddedAssets.ENTITY_TEXT_FREE;
					break;
				case ENTITY:
					return EmbeddedAssets.ENTITY_GENERIC;
					break;
				case EVENT:
					return EmbeddedAssets.ENTITY_EVENT;
					break;
				case TEMPORAL:
					return EmbeddedAssets.ENTITY_TEMPORAL;
					break;
				case GEO_LOCATION:
					return EmbeddedAssets.ENTITY_GEO_LOCATION;
					break;
				default:
					return EmbeddedAssets.ENTITY_GENERIC;
					break;
			}
		}
	}
}

