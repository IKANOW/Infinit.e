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
	import com.ikanow.infinit.e.shared.model.constant.Constants;
	import com.ikanow.infinit.e.shared.model.vo.Source;
	import mx.collections.ISortField;
	import mx.utils.ObjectUtil;
	import spark.components.gridClasses.GridColumn;
	
	public class SourceUtil
	{
		
		//======================================
		// constructor 
		//======================================
		
		/**
		 *
		 * Constructor
		 *
		 */
		public function SourceUtil()
		{
		
		}
		
		
		//======================================
		// public static methods 
		//======================================
		
		/**
		 * Sort compare function for the community column
		 * @param itemA
		 * @param itemB
		 * @param column
		 * @return
		 */
		public static function sortCompareCommunity( itemA:Source, itemB:Source, column:GridColumn ):int
		{
			if ( !( itemA && itemB ) )
				return 0;
			
			if ( itemA.community && !itemB.community )
				return 1;
			
			if ( itemB.community && !itemA.community )
				return -1;
			
			var itemAString:String = itemA.community + Constants.SPACE + itemA.title + Constants.SPACE + itemA.tagsString + Constants.SPACE + itemA.mediaType;
			var itemBString:String = itemB.community + Constants.SPACE + itemB.title + Constants.SPACE + itemB.tagsString + Constants.SPACE + itemB.mediaType;
			
			return ObjectUtil.stringCompare( itemAString, itemBString, true );
		}
		
		/**
		 * Sort compare function for the media type column
		 * @param itemA
		 * @param itemB
		 * @param column
		 * @return
		 */
		public static function sortCompareMediaType( itemA:Source, itemB:Source, column:GridColumn ):int
		{
			if ( !( itemA && itemB ) )
				return 0;
			
			if ( itemA.mediaType && !itemB.mediaType )
				return 1;
			
			if ( itemB.mediaType && !itemA.mediaType )
				return -1;
			
			var itemAString:String = itemA.mediaType + Constants.SPACE + itemA.title + Constants.SPACE + itemA.tagsString + Constants.SPACE + itemA.community;
			var itemBString:String = itemB.mediaType + Constants.SPACE + itemB.title + Constants.SPACE + itemB.tagsString + Constants.SPACE + itemB.community;
			
			return ObjectUtil.stringCompare( itemAString, itemBString, true );
		}
		/**
		 * Sort compare function for the title column
		 * @param itemA
		 * @param itemB
		 * @param column
		 * @return
		 */
		public static function sortCompareStatus( itemA:Source, itemB:Source, column:GridColumn ):int
		{
			if ( !( itemA && itemB ) )
				return 0;
			
			if ( itemA.harvest && !itemB.harvest )
				return 1;
			
			if ( itemA.harvest.harvest_status && !itemB.harvest.harvest_status )
				return 1;
			
			if ( itemB.harvest && !itemA.harvest )
				return -1;
			
			if ( itemB.harvest.harvest_status && !itemA.harvest.harvest_status )
				return -1;
			
			var itemAString:String = itemA.harvest.harvest_status + Constants.SPACE + itemA.title + Constants.SPACE + itemA.tagsString + Constants.SPACE + itemA.mediaType + Constants.SPACE + itemA.community;
			var itemBString:String = itemB.harvest.harvest_status + Constants.SPACE + itemB.title + Constants.SPACE + itemB.tagsString + Constants.SPACE + itemB.mediaType + Constants.SPACE + itemB.community;
			
			return ObjectUtil.stringCompare( itemAString, itemBString, true );
		}
		
		/**
		 * Sort compare function for the title column
		 * @param itemA
		 * @param itemB
		 * @param column
		 * @return
		 */
		public static function sortCompareStatus_direct( itemA:Source, itemB:Source, fields:Array ):int
		{
			var item:ISortField = fields.pop() as ISortField;
			fields.push( item );
			
			if ( !item.descending )
				return sortCompareStatus( itemA, itemB, null );
			else
				return sortCompareStatus( itemB, itemA, null );
		}
		/**
		 * Sort compare function for the tags column
		 * @param itemA
		 * @param itemB
		 * @param column
		 * @return
		 */
		public static function sortCompareTags( itemA:Source, itemB:Source, column:GridColumn ):int
		{
			if ( !( itemA && itemB ) )
				return 0;
			
			if ( itemA.tagsString && !itemB.tagsString )
				return 1;
			
			if ( itemB.tagsString && !itemA.tagsString )
				return -1;
			
			var itemAString:String = itemA.tagsString + Constants.SPACE + itemA.title + Constants.SPACE + itemA.mediaType + Constants.SPACE + itemA.community;
			var itemBString:String = itemB.tagsString + Constants.SPACE + itemB.title + Constants.SPACE + itemB.mediaType + Constants.SPACE + itemB.community;
			
			return ObjectUtil.stringCompare( itemAString, itemBString, true );
		}
		
		/**
		 * Sort compare function for the title column
		 * @param itemA
		 * @param itemB
		 * @param column
		 * @return
		 */
		public static function sortCompareTitle( itemA:Source, itemB:Source, column:GridColumn ):int
		{
			if ( !( itemA && itemB ) )
				return 0;
			
			if ( itemA.title && !itemB.title )
				return 1;
			
			if ( itemB.title && !itemA.title )
				return -1;
			
			var itemAString:String = itemA.title + Constants.SPACE + itemA.tagsString + Constants.SPACE + itemA.mediaType + Constants.SPACE + itemA.community;
			var itemBString:String = itemB.title + Constants.SPACE + itemB.tagsString + Constants.SPACE + itemB.mediaType + Constants.SPACE + itemB.community;
			
			return ObjectUtil.stringCompare( itemAString, itemBString, true );
		}
	}
}
