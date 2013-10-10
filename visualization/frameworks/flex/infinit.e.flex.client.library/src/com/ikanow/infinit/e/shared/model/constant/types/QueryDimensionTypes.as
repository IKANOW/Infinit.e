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
	import mx.resources.ResourceManager;
	import assets.EmbeddedAssets;
	
	/**
	 * Query Dimension Type Constants
	 */
	public class QueryDimensionTypes
	{
		
		//======================================
		// public static properties 
		//======================================
		
		public static const EXACT_TEXT:String = "etext";
		
		public static const FREE_TEXT:String = "ftext";
		
		public static const WHO:String = "who";
		
		public static const WHAT:String = "what";
		
		public static const WHERE:String = "where";
		
		public static const WHEN:String = "when";
		
		
		//======================================
		// public static methods 
		//======================================
		
		/**
		 * function to get a dimension type icon
		 *
		 * @param type The type of the dimension
		 *
		 * @return The icon of the dimension
		 */
		public static function getIcon( dimensionType:String ):Class
		{
			switch ( dimensionType.toLowerCase() )
			{
				case EXACT_TEXT:
					return EmbeddedAssets.ENTITY_TEXT_EXACT;
					break;
				case FREE_TEXT:
					return EmbeddedAssets.ENTITY_TEXT_FREE;
					break;
				case WHO:
					return EmbeddedAssets.ENTITY_PERSON;
					break;
				case WHAT:
					return EmbeddedAssets.ENTITY_GENERIC;
					break;
				case WHERE:
					return EmbeddedAssets.ENTITY_GEO_LOCATION;
					break;
				case WHEN:
					return EmbeddedAssets.ENTITY_TEMPORAL;
					break;
				default:
					return EmbeddedAssets.ENTITY_GENERIC;
					break;
			}
		}
		
		/**
		 * function to get a dimension label
		 *
		 * @param type The type of the dimension
		 *
		 * @return The label of the dimension
		 */
		public static function getLabel( dimensionType:String ):String
		{
			switch ( dimensionType.toLowerCase() )
			{
				case EXACT_TEXT:
					return ResourceManager.getInstance().getString( 'infinite', 'dimension.exactText' );
					break;
				case FREE_TEXT:
					return ResourceManager.getInstance().getString( 'infinite', 'dimension.freeText' );
					break;
				case WHO:
					return ResourceManager.getInstance().getString( 'infinite', 'dimension.who' );
					break;
				case WHAT:
					return ResourceManager.getInstance().getString( 'infinite', 'dimension.what' );
					break;
				case WHERE:
					return ResourceManager.getInstance().getString( 'infinite', 'dimension.where' );
					break;
				case WHEN:
					return ResourceManager.getInstance().getString( 'infinite', 'dimension.when' );
					break;
				default:
					return ResourceManager.getInstance().getString( 'infinite', 'dimension.what' );
					break;
			}
		}
		
		/**
		 * function to get the dimension typ sort order
		 *
		 * @param type The type of the dimention
		 *
		 * @return The sort order of the dimention
		 */
		public static function getSortOrder( dimensionType:String ):int
		{
			switch ( dimensionType.toLowerCase() )
			{
				case EXACT_TEXT:
					return 0;
					break;
				case FREE_TEXT:
					return 1;
					break;
				case WHO:
					return 2;
					break;
				case WHAT:
					return 3;
					break;
				case WHERE:
					return 4;
					break;
				case WHEN:
					return 5;
					break;
				default:
					return 3;
					break;
			}
		}
	}
}

