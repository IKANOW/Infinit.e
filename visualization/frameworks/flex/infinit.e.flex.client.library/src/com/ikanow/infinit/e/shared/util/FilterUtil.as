package com.ikanow.infinit.e.shared.util
{
	import com.ikanow.infinit.e.shared.model.constant.Constants;
	import flash.utils.Dictionary;
	import spark.components.gridClasses.GridColumn;
	
	public class FilterUtil
	{
		
		//======================================
		// constructor 
		//======================================
		
		/**
		 *
		 * Constructor
		 *
		 */
		public function FilterUtil()
		{
		
		}
		
		
		//======================================
		// public static methods 
		//======================================
		
		/**
		 * Build an array of values to search from data grid columns
		 * @param item
		 */
		public static function buildSearchValuesArrayForGridColumns( item:*, columns:Array, selectedCollumns:Array = null ):Array
		{
			if ( item == null )
				return [];
			
			var result:Array = [];
			var column:GridColumn;
			
			// get label strings in all columns for the current item
			for each ( column in columns )
			{
				result.push( column.itemToLabel( item ) );
			}
			
			return result;
		}
		
		/**
		 * Check the search term
		 * An "and" search for all of the search keywords
		 * @param searchTerm
		 * @param values
		 * @return
		 */
		public static function checkAllSearchTerms( searchTerm:String, values:Array ):Boolean
		{
			var keyWords:Array = searchTerm.split( Constants.SPACE );
			var keyWordDictionary:Dictionary = new Dictionary( true );
			var allKeyWordsFound:Boolean = true;
			
			for each ( var value:String in values )
			{
				for each ( var keyWord:String in keyWords )
				{
					if ( keyWord != null && keyWordDictionary[ keyWord ] == null )
						keyWordDictionary[ keyWord ] = false;
					
					if ( value != null && keyWord != null && keyWord != Constants.BLANK && value.toLowerCase().indexOf( keyWord.toLowerCase() ) != -1 )
					{
						keyWordDictionary[ keyWord ] = true;
					}
					
					if ( keyWord != null && keyWord == Constants.BLANK )
						keyWordDictionary[ keyWord ] = true;
				}
			}
			
			for each ( var keyWordItem:Object in keyWordDictionary )
			{
				if ( keyWordItem == false )
					allKeyWordsFound = false;
			}
			
			return allKeyWordsFound;
		}
		
		/**
		 * Check the search term
		 * An "or" search for all of the search keywords
		 * @param searchTerm
		 * @param values
		 * @return
		 */
		public static function checkAnySearchTerms( searchTerm:String, values:Array ):Boolean
		{
			var keyWords:Array = searchTerm.split( Constants.SPACE );
			var keyWordDictionary:Dictionary = new Dictionary( true );
			var anyKeyWordsFound:Boolean = false;
			
			for each ( var value:String in values )
			{
				for each ( var keyWord:String in keyWords )
				{
					if ( keyWord != null && keyWordDictionary[ keyWord ] == null )
						keyWordDictionary[ keyWord ] = false;
					
					if ( value != null && keyWord != null && keyWord != Constants.BLANK && value.toLowerCase().indexOf( keyWord.toLowerCase() ) != -1 )
					{
						keyWordDictionary[ keyWord ] = true;
					}
				}
			}
			
			for each ( var keyWordItem:Object in keyWordDictionary )
			{
				if ( keyWordItem == true )
					anyKeyWordsFound = true;
			}
			
			return anyKeyWordsFound;
		}
	}
}
