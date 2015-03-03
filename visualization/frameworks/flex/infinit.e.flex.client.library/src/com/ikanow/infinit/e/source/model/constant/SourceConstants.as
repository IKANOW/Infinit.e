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
package com.ikanow.infinit.e.source.model.constant
{
	import com.ikanow.infinit.e.shared.model.constant.types.QueryOperatorTypes;
	import com.ikanow.infinit.e.shared.model.vo.ui.ColumnSelector;
	
	import mx.collections.ArrayCollection;
	import mx.resources.ResourceManager;
	
	/**
	 * Source Constants
	 */
	public class SourceConstants
	{
		
		//======================================
		// public static properties 
		//======================================
		
		public static const FIELD_TITLE:String = "title";
		
		public static const FIELD_KEY:String = "key";
		
		public static const FIELD_TAG:String = "tag";
		
		public static const FIELD_TAGS_STRING:String = "tagsString";
		
		public static const FIELD_MEDIA_TYPE:String = "mediaType";
		
		public static const FIELD_COMMUNITY:String = "community";
		
		public static const FIELD_DESCRIPTION:String = "description";
		
		public static const FIELD_STATUS:String = "status";
		
		public static const DATA_FIELD:String = "dataField";
		
		
		//======================================
		// public static methods 
		//======================================
		
		public static function getSelectableColumns( selected:Boolean = true ):ArrayCollection
		{
			var selectableColumns:ArrayCollection = new ArrayCollection();
			
			selectableColumns.addItem( new ColumnSelector( FIELD_TITLE, FIELD_TITLE, ResourceManager.getInstance().getString( 'infinite', 'sources.sourceName' ), QueryOperatorTypes.AND, selected ) );
			selectableColumns.addItem( new ColumnSelector( FIELD_KEY, FIELD_KEY, ResourceManager.getInstance().getString( 'infinite', 'sources.sourceKey' ), QueryOperatorTypes.AND, selected ) );
			selectableColumns.addItem( new ColumnSelector( FIELD_TAG, FIELD_TAGS_STRING, ResourceManager.getInstance().getString( 'infinite', 'sources.tags' ), QueryOperatorTypes.AND, selected ) );
			selectableColumns.addItem( new ColumnSelector( FIELD_MEDIA_TYPE, FIELD_MEDIA_TYPE, ResourceManager.getInstance().getString( 'infinite', 'sources.type' ), QueryOperatorTypes.AND, selected ) );
			selectableColumns.addItem( new ColumnSelector( FIELD_COMMUNITY, FIELD_COMMUNITY, ResourceManager.getInstance().getString( 'infinite', 'sources.community' ), QueryOperatorTypes.AND, selected ) );
			
			return selectableColumns;
		}
	}
}

