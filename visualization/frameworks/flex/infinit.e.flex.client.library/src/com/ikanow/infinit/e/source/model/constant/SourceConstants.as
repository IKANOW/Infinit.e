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
		
		public static const FIELD_TAG:String = "tag";
		
		public static const FIELD_TAGS_STRING:String = "tagsString";
		
		public static const FIELD_MEDIA_TYPE:String = "mediaType";
		
		public static const FIELD_COMMUNITY:String = "community";
		
		public static const FIELD_DESCRIPTION:String = "description";
		
		public static const DATA_FIELD:String = "dataField";
		
		
		//======================================
		// public static methods 
		//======================================
		
		public static function getSelectableColumns( selected:Boolean = true ):ArrayCollection
		{
			var selectableColumns:ArrayCollection = new ArrayCollection();
			
			selectableColumns.addItem( new ColumnSelector( FIELD_TITLE, FIELD_TITLE, ResourceManager.getInstance().getString( 'infinite', 'sources.sourceName' ), QueryOperatorTypes.AND, selected ) );
			selectableColumns.addItem( new ColumnSelector( FIELD_TAG, FIELD_TAGS_STRING, ResourceManager.getInstance().getString( 'infinite', 'sources.tags' ), QueryOperatorTypes.AND, selected ) );
			selectableColumns.addItem( new ColumnSelector( FIELD_MEDIA_TYPE, FIELD_MEDIA_TYPE, ResourceManager.getInstance().getString( 'infinite', 'sources.type' ), QueryOperatorTypes.AND, selected ) );
			selectableColumns.addItem( new ColumnSelector( FIELD_COMMUNITY, FIELD_COMMUNITY, ResourceManager.getInstance().getString( 'infinite', 'sources.community' ), QueryOperatorTypes.AND, selected ) );
			
			return selectableColumns;
		}
	}
}

